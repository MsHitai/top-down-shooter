package com.trush.game.bot;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.enemy.Enemy;
import com.trush.game.input.Gamepad;
import com.trush.game.input.Gamepad.Button;
import com.trush.game.launcher.Dev;
import com.trush.game.objects.Bullet;
import com.trush.game.objects.Chest;
import com.trush.game.objects.Item;
import com.trush.game.objects.Pickup;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import com.trush.game.world.Room;
import com.trush.game.world.StageMap;
import com.trush.game.world.StoryDirector;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A balancing bot (dev builds, -Dfluffy.bot=true): plays story runs through the virtual {@link Gamepad} -
 * dodges by guessing where every enemy bullet will be over the next half second, rolls when a hit can't be
 * walked away from, blanks when it's about to die, shoots the nearest enemy it can see, then picks things up,
 * opens chests and walks on to the next room it hasn't seen, the boss last. Every room and run goes into a
 * JSON-lines log (-Dfluffy.botLog=path, default bot-log.jsonl) for balancing.
 */
public class Bot {

    public static final boolean ON = Dev.flag("bot");
    /** One bot for the whole session: runs come and go, the log and the run counter stay. */
    public static final Bot INSTANCE = ON ? new Bot() : null;
    /** World steps per rendered frame (each one a fixed 1/60 s): the game runs this many times faster. */
    public static final int STEPS = Math.max(1, Dev.number("botSpeed", 3));
    /** How many runs before the game quits on its own (0: never). */
    private static final int RUNS = Dev.number("botRuns", 0);
    /** Floors a run plays before it counts as done (0: the whole story) - e.g. one floor to try a gun on. */
    private static final int FLOORS = Dev.number("botFloors", 0);
    private int floorsCleared;
    /** Game seconds on one floor before the bot gives up (stuck somewhere): the run counts as "stuck". */
    private static final float FLOOR_LIMIT = Dev.number("botFloorLimit", 600);

    private static final int DIRS = 16;
    private static final float[] TIMES = {0.06f, 0.13f, 0.22f, 0.33f, 0.48f};
    private static final float PROBE = 0.15f;
    private static final float CELL = 0.25f;

    private final PrintWriter log;
    private final boolean[] buttons = new boolean[Button.values().length];
    private final Vector2 stick = new Vector2();
    private final Vector2 aimDir = new Vector2(1, 0);
    private final Vector2 goal = new Vector2();
    private final Vector2 tmp = new Vector2();
    private final Vector2 lastPos = new Vector2();
    private boolean hasGoal;
    private int goalDoor = -1;

    private int run;
    private GameWorld world;
    private float floorTime;
    private float runTime;
    private float stuckTime;
    private float wanderTime;
    private final Vector2 wanderDir = new Vector2();
    private float chargeTime;
    private Object target;
    private float targetTime;
    private final Set<Object> given = new HashSet<>();
    private final Map<String, Integer> deathsBy = new HashMap<>();

    // the room being fought in
    private Room fightRoom;
    private float fightTime;
    private int fightHalves;
    private int fightBlanks;
    private int fightRolls;
    private final Map<String, Float> fightGunTime = new HashMap<>();
    private String fightBoss;
    private final Set<String> fightKinds = new HashSet<>();
    private int lastHealth;
    private int lastBlanks;
    private boolean wasRolling;
    private int runHalves;
    private int runBlanks;

    private Bot() {
        String path = Dev.text("botLog");
        PrintWriter w;
        try {
            w = new PrintWriter(new FileWriter(path != null ? path : "bot-log.jsonl", true), true);
        } catch (IOException e) {
            throw new IllegalStateException("can't open the bot log", e);
        }
        log = w;
    }

    /** A new floor's world (a new run, or the next floor of this one). */
    public void begin(GameWorld w, boolean newRun) {
        world = w;
        floorTime = 0;
        fightRoom = null;
        given.clear();
        target = null;
        hasGoal = false;
        if (newRun) {
            run++;
            runTime = 0;
            runHalves = 0;
            runBlanks = 0;
            floorsCleared = 0;
        }
        lastHealth = w.getPlayer().getHealth();
        lastBlanks = w.getPlayer().getBlanks();
        System.out.println("[bot] run " + run + " floor " + floor() + " " + stageName());
    }

    /** A floor is beaten: true if the run has played all the floors it was asked to (it ends as "cleared"). */
    public boolean floorCleared() {
        floorsCleared++;
        return FLOORS > 0 && floorsCleared >= FLOORS;
    }

    /** True once the bot has been on this floor too long: GameScreen ends the run. */
    public boolean gaveUp() {
        return floorTime > FLOOR_LIMIT;
    }

    /** Has played all the runs it was asked to (-Dfluffy.botRuns). */
    public boolean done() {
        return RUNS > 0 && run >= RUNS;
    }

    /** A run is over: "death", "victory", "cleared" (its floors done) or "stuck". */
    public void runEnded(String result) {
        if (world == null) return;
        Player p = world.getPlayer();
        String where = fightBoss != null && fightRoom != null ? "boss:" + fightBoss
                : fightRoom != null ? "room:" + fightRoom.type : "walking";
        if (result.equals("death")) {
            endFight(true);
            deathsBy.merge(floor() + " " + where, 1, Integer::sum);
        }
        if (result.equals("stuck")) {
            Room r = world.getRoom();
            int seen = 0;
            int all = 0;
            for (Room m : world.getMap().rooms) {
                all++;
                if (m.visited) seen++;
            }
            where = "stuck in " + r.type + " locked=" + r.isLocked() + " enemies=" + world.getEnemies().size + " exit=" + r.exit
                    + " seen=" + seen + "/" + all + " feet=" + Math.round(p.feetX() * 10) / 10f + "," + Math.round(p.feetY() * 10) / 10f
                    + " goal=" + (hasGoal ? goal : "none") + " target=" + (target == null ? "none" : target.getClass().getSimpleName() + ":" + target)
                    + " prompt=" + world.getPrompt();
        }
        StringBuilder guns = new StringBuilder();
        for (Weapon g : p.getWeapons()) {
            if (guns.length() > 0) guns.append(',');
            guns.append(g.getName());
        }
        line("{\"event\":\"run\",\"run\":" + run + ",\"hero\":\"" + p.getHero() + "\",\"hard\":" + world.isHard()
                + ",\"result\":\"" + result + "\",\"floor\":" + floor() + ",\"stage\":\"" + stageName()
                + "\",\"where\":\"" + where + "\",\"time\":" + Math.round(runTime) + ",\"halvesLost\":" + runHalves
                + ",\"blanksUsed\":" + runBlanks + ",\"score\":" + world.getScore() + ",\"maxHealth\":" + p.getMaxHealth()
                + ",\"coins\":" + p.getCoins() + ",\"guns\":\"" + guns + "\",\"items\":\"" + p.getPassives() + "\"}");
        System.out.println("[bot] run " + run + " " + result + " on floor " + floor() + " (" + where + ") deaths so far " + deathsBy);
        world = null;
    }

    private int floor() {
        return world != null && world.getDirector() instanceof StoryDirector ? ((StoryDirector) world.getDirector()).getFloor() : 0;
    }

    private String stageName() {
        return world != null && world.getDirector() instanceof StoryDirector ? String.valueOf(((StoryDirector) world.getDirector()).getStage()) : "ENDLESS";
    }

    private void line(String json) {
        log.println(json);
    }

    // ------------------------------------------------------------------ one world step

    /** Decides this step's input and hands it to the pad (and the aim to the world). */
    public void think(GameWorld w, Gamepad pad, float delta) {
        world = w;
        floorTime += delta;
        runTime += delta;
        Player p = w.getPlayer();
        java.util.Arrays.fill(buttons, false);
        stick.setZero();
        track(p, delta);
        if (!p.isDead() && !p.isFalling()) {
            Enemy aimAt = pickTarget(p);
            boolean fight = w.getDirector().inFight(w) || w.getEnemies().size > 0;
            chooseGun(p);
            if (fight) {
                hasGoal = false;
                if (aimAt != null) {
                    // a little out of reach with no clear shot: walk round to where there is one
                    if (!w.hasLineOfSight(p.centerX(), p.hitboxY(), aimAt.pos.x, aimAt.pos.y, GameWorld.SIGHT_THICKNESS)
                            || Vector2.dst(p.centerX(), p.hitboxY(), aimAt.pos.x, aimAt.pos.y) > 3.3f) {
                        shotSpotAge += delta;
                        if (shotSpotAge > 0.5f || shotSpotFor != aimAt) {
                            shotSpotAge = 0;
                            shotSpotFor = aimAt;
                            if (!findShotSpot(p, aimAt, shotSpot)) shotSpot.set(aimAt.pos);
                        }
                        setGoalTo(p, shotSpot.x, shotSpot.y);
                    }
                }
            } else {
                explore(p);
            }
            move(p, aimAt, fight);
            shoot(p, aimAt, delta);
            if (fight) useItem(p, aimAt);
            unstick(p, delta, fight);
        }
        // the aim: GameScreen's pad aim turns the right stick into a spot in front of the hero
        pad.drive(stick, aimDir, buttons);
    }

    /** Hits taken, blanks, rolls, rooms fought: for the log. */
    private void track(Player p, float delta) {
        Room room = world.getRoom();
        boolean inFight = world.getDirector().inFight(world);
        if (inFight && fightRoom != room) {
            endFight(false);
            fightRoom = room;
            fightTime = 0;
            fightHalves = 0;
            fightBlanks = 0;
            fightRolls = 0;
            fightBoss = null;
            fightKinds.clear();
            fightGunTime.clear();
        }
        if (fightRoom != null) {
            fightTime += delta;
            fightGunTime.merge(p.currentWeapon().getName(), delta, Float::sum);
            for (Enemy e : world.getEnemies()) {
                fightKinds.add(e.dexId());
                if (e.boss && e.bossName() != null && fightBoss == null) fightBoss = e.bossName();
            }
        }
        int h = p.getHealth();
        if (h < lastHealth && fightRoom == null) { // a hit outside a fight: what was going on?
            Enemy near = null;
            for (Enemy e : world.getEnemies()) {
                if (near == null || e.pos.dst2(p.centerX(), p.hitboxY()) < near.pos.dst2(p.centerX(), p.hitboxY())) near = e;
            }
            line("{\"event\":\"hurt_outside\",\"run\":" + run + ",\"floor\":" + floor() + ",\"stage\":\"" + stageName()
                    + "\",\"room\":\"" + room.type + "\",\"locked\":" + room.isLocked() + ",\"enemies\":" + world.getEnemies().size
                    + ",\"dying\":" + world.getDying().size + ",\"nearest\":\"" + (near == null ? "" : near.dexId())
                    + "\",\"bullets\":" + world.getEnemyBullets().size + ",\"halves\":" + (lastHealth - h) + "}");
        }
        if (h < lastHealth) {
            fightHalves += lastHealth - h;
            runHalves += lastHealth - h;
        }
        lastHealth = h;
        if (p.getBlanks() < lastBlanks) {
            fightBlanks++;
            runBlanks++;
        }
        lastBlanks = p.getBlanks();
        if (p.isRolling() && !wasRolling) fightRolls++;
        wasRolling = p.isRolling();
        if (fightRoom != null && !inFight && !world.bossDying() && world.getEnemies().size == 0) {
            endFight(false);
        }
    }

    private void endFight(boolean died) {
        if (fightRoom == null) return;
        line("{\"event\":\"room\",\"run\":" + run + ",\"floor\":" + floor() + ",\"stage\":\"" + stageName()
                + "\",\"type\":\"" + fightRoom.type + "\",\"depth\":" + fightRoom.depth
                + ",\"small\":" + fightRoom.arena.isSmall() + ",\"boss\":" + (fightBoss == null ? "null" : "\"" + fightBoss + "\"")
                + ",\"time\":" + Math.round(fightTime * 10) / 10f + ",\"halvesLost\":" + fightHalves + ",\"blanks\":" + fightBlanks
                + ",\"rolls\":" + fightRolls + ",\"died\":" + died + ",\"health\":" + world.getPlayer().getHealth()
                + ",\"maxHealth\":" + world.getPlayer().getMaxHealth() + ",\"kinds\":\"" + String.join(",", fightKinds) + "\"" + loadout() + "}");
        fightRoom = null;
        fightBoss = null;
    }

    /** The gun held longest in the fight, and everything carried. */
    private String loadout() {
        Player p = world.getPlayer();
        String main = p.currentWeapon().getName();
        float most = -1f;
        for (Map.Entry<String, Float> e : fightGunTime.entrySet()) {
            if (e.getValue() > most) {
                most = e.getValue();
                main = e.getKey();
            }
        }
        StringBuilder items = new StringBuilder();
        for (Object i : p.getPassives()) {
            if (items.length() > 0) items.append(',');
            items.append(i);
        }
        if (p.getActive() != null) items.append(items.length() > 0 ? "," : "").append(p.getActive());
        return ",\"gun\":\"" + main + "\",\"guns\":" + p.getWeapons().size + ",\"items\":\"" + items + "\"";
    }

    // ------------------------------------------------------------------ fighting

    private Enemy pickTarget(Player p) {
        Enemy best = null;
        float bestScore = Float.MAX_VALUE;
        for (Enemy e : world.getEnemies()) {
            if (!e.alive || e.isUntouchable()) continue;
            float d = Vector2.dst(p.centerX(), p.hitboxY(), e.pos.x, e.pos.y);
            if (!world.hasLineOfSight(p.muzzleX(), p.muzzleY(), e.pos.x, e.pos.y, 0.04f)) d += 4f;
            if (e.boss) d -= 1f;
            if (d < bestScore) {
                bestScore = d;
                best = e;
            }
        }
        return best;
    }

    /** The best gun that still has ammo (higher tier first), the starter when nothing else is left. */
    private void chooseGun(Player p) {
        Array<Weapon> guns = p.getWeapons();
        int best = p.getWeaponIndex();
        Weapon now = p.currentWeapon();
        if (!now.isEmpty() && (now.isReloading() || now.isCharging())) return;
        float bestScore = score(now);
        for (int i = 0; i < guns.size; i++) {
            float s = score(guns.get(i));
            if (s > bestScore + 0.5f) {
                bestScore = s;
                best = i;
            }
        }
        if (best != p.getWeaponIndex()) p.selectWeapon(best);
    }

    private static float score(Weapon w) {
        if (w.isEmpty()) return -10f;
        float s = w.getTier().ordinal() * 2f + 2f; // any chest gun beats the starter
        if (w.isInfinite()) s = Math.min(s, 1f) + (w.isFlufful() ? 6f : 0f);
        if (w.getMelee() > 0) s -= 3f; // the frying pan needs you up close
        return s;
    }

    private void shoot(Player p, Enemy e, float delta) {
        Weapon w = p.currentWeapon();
        if (e == null) {
            if (w.wantsReload() || (w.getMag() < w.getMagSize() && !w.isInfinite())) buttons[Button.X.ordinal()] = !w.isReloading();
            if (stick.len2() > 0.01f) aimDir.set(stick).nor();
            chargeTime = 0;
            return;
        }
        float ex = e.pos.x;
        float ey = e.pos.y;
        aimDir.set(ex - p.centerX(), ey - p.hitboxY()).nor();
        boolean clear = world.hasLineOfSight(p.muzzleX(), p.muzzleY(), ex, ey, 0.04f)
                && world.onScreen(ex, ey, -0.3f);
        if (!clear && w.getMelee() <= 0) {
            chargeTime = 0;
            return;
        }
        if (w.chargesShots()) { // the egg lobber / slingshot: hold, then let go
            chargeTime += delta;
            buttons[Button.RB.ordinal()] = chargeTime < 0.55f;
            if (chargeTime > 0.6f) chargeTime = 0;
        } else {
            buttons[Button.RB.ordinal()] = w.getMelee() <= 0
                    || Vector2.dst(p.centerX(), p.hitboxY(), ex, ey) < w.getMelee() + e.radius + 0.3f;
        }
    }

    /**
     * Scores 16 directions and standing still against where the enemy bullets will be, the enemies' bodies,
     * the walls and where it wants to go; walks the best one, rolls when even that gets hit, blanks when the
     * hit would hurt too much.
     */
    private void move(Player p, Enemy e, boolean fight) {
        float px = p.centerX();
        float py = p.hitboxY();
        float speed = p.moveSpeed();
        Array<Bullet> bullets = world.getEnemyBullets();
        Vector2 want = goalDir(p);
        float bestDanger = Float.MAX_VALUE;
        int bestDir = -1;
        float stayDanger = 0f;
        float bestHitAt = 99f;
        for (int d = -1; d < DIRS; d++) {
            float dx = d < 0 ? 0 : MathUtils.cosDeg(d * 360f / DIRS);
            float dy = d < 0 ? 0 : MathUtils.sinDeg(d * 360f / DIRS);
            float danger = 0f;
            float hitAt = 99f;
            float tFree = TIMES[TIMES.length - 1];
            for (float t : TIMES) {
                float fx = p.feetX() + dx * speed * t;
                float fy = p.feetY() + dy * speed * t;
                if (d >= 0 && !free(fx, fy)) { // a wall: it stops there
                    tFree = Math.min(tFree, t);
                }
            }
            for (float t : TIMES) {
                float tt = Math.min(t, tFree);
                float x = px + dx * speed * tt;
                float y = py + dy * speed * tt;
                float near = 0f;
                for (int i = 0; i < bullets.size; i++) {
                    Bullet b = bullets.get(i);
                    if (!b.alive) continue;
                    float bx = b.pos.x + b.vel.x * t;
                    float by = b.pos.y + b.vel.y * t;
                    float gap = Vector2.dst(bx, by, x, y) - b.radius * b.hitScale - Player.HITBOX_RADIUS;
                    if (gap < 0.02f) {
                        hitAt = Math.min(hitAt, t);
                        near += 12f;
                    } else if (gap < 0.3f) {
                        near += (0.3f - gap) * (0.3f - gap) * 40f;
                    }
                }
                for (Enemy en : world.getEnemies()) {
                    if (!en.alive || !en.hurtsToTouch) continue;
                    float gap = Vector2.dst(en.pos.x, en.pos.y, x, y) - en.radius - 0.2f;
                    if (gap < 0.4f) near += (0.4f - gap) * 10f;
                }
                danger += near / (1f + t * 4f); // soon counts more than later
            }
            // keep off the walls and corners: cornered is how you die in a bullet hell
            float ex = px + dx * speed * 0.3f;
            float ey = py + dy * speed * 0.3f;
            Arena a = Arena.now;
            float wall = Math.min(Math.min(ex - a.left, a.right - ex), Math.min(ey - a.bottom, a.top - ey));
            if (fight && wall < 0.8f) danger += (0.8f - wall) * 2.5f;
            // a fight: keep a good distance from the target
            if (fight && e != null) {
                float dist = Vector2.dst(ex, ey, e.pos.x, e.pos.y);
                float good = e.boss ? 3f : 2.4f;
                danger += Math.abs(dist - good) * 0.25f;
            }
            if (want != null && d >= 0) danger -= (dx * want.x + dy * want.y) * (fight ? 0.8f : 3f);
            if (want != null && d < 0) danger += fight ? 0f : 1.5f;
            if (wanderTime > 0 && d >= 0) danger -= (dx * wanderDir.x + dy * wanderDir.y) * 3f;
            if (d >= 0 && tFree <= TIMES[1] && onPit(p.feetX() + dx * 0.2f, p.feetY() + dy * 0.2f)) danger += 50f;
            if (d < 0) stayDanger = danger;
            if (danger < bestDanger) {
                bestDanger = danger;
                bestDir = d;
                bestHitAt = hitAt;
            }
        }
        if (bestDir >= 0) {
            stick.set(MathUtils.cosDeg(bestDir * 360f / DIRS), MathUtils.sinDeg(bestDir * 360f / DIRS));
        }
        hitSoon = bestHitAt;
        if (bestHitAt <= 0.22f && !p.isInvulnerable()) {
            boolean dying = p.getHealth() <= 2;
            if (p.getBlanks() > 0 && (dying || (bestHitAt <= 0.06f && countNear(px, py, 1.2f) > 14))) {
                buttons[Button.RS.ordinal()] = true; // a blank: every bullet gone
            } else if (p.getRollCooldown() <= 0 && !p.isRolling()) {
                if (stick.isZero()) stick.set(aimDir).rotateDeg(90);
                buttons[Button.LB.ordinal()] = true; // through it, invulnerable
            }
        }
    }

    private boolean itemHeld;
    /** Set by move(): a bullet will hit this soon even on the best path (rolls and blanks look at it too). */
    private float hitSoon = 99f;

    /**
     * The active item, the way a player would use it: heals when low, the plushie / bubble wand when a hit is
     * coming, the grenade thrown at the target, the rest (robot, globe, cardboard hero, sugar rush) when a fight's on.
     */
    private void useItem(Player p, Enemy e) {
        Item item = p.getActive();
        boolean press = false;
        if (item != null && p.getActiveCooldown() <= 0) {
            switch (item) {
                case MED_KIT:
                case FLUFFY_CANDY:
                    press = p.getHealth() <= 2 && p.getHealth() < p.getMaxHealth();
                    break;
                case PACKED_LUNCH: // eaten by itself on a fatal hit
                    break;
                case STAR_PLUSHIE:
                case BUBBLE_WAND:
                    press = hitSoon <= 0.22f && !p.isInvulnerable();
                    break;
                case GRENADE: // hold to throw further, let go when it would land on the target
                    if (e != null && world.hasLineOfSight(p.centerX(), p.hitboxY(), e.pos.x, e.pos.y, 0.05f)) {
                        float d = Vector2.dst(p.centerX(), p.hitboxY(), e.pos.x, e.pos.y);
                        buttons[Button.RT.ordinal()] = p.grenadeDistance() < d - 0.2f && d < 4.5f;
                    }
                    return;
                default:
                    press = e != null && (e.boss || world.getEnemies().size >= 2);
                    break;
            }
        }
        buttons[Button.RT.ordinal()] = press && !itemHeld; // a tap
        itemHeld = buttons[Button.RT.ordinal()];
    }

    private int countNear(float x, float y, float r) {
        int n = 0;
        for (Bullet b : world.getEnemyBullets()) {
            if (b.alive && b.pos.dst2(x, y) < r * r) n++;
        }
        return n;
    }

    private boolean onPit(float fx, float fy) {
        for (com.badlogic.gdx.math.Rectangle r : world.getRoom().pits) {
            if (r.contains(fx, fy)) return true;
        }
        return false;
    }

    private boolean free(float fx, float fy) {
        return world.pointIsFree(fx, fy + 0.05f, PROBE);
    }

    private void unstick(Player p, float delta, boolean fight) {
        wanderTime -= delta;
        if (p.isRolling() || stick.isZero() || (fight && !hasGoal)) {
            stuckTime = 0;
            lastPos.set(p.feetX(), p.feetY());
            return;
        }
        if (lastPos.dst(p.feetX(), p.feetY()) > 0.25f) {
            lastPos.set(p.feetX(), p.feetY());
            stuckTime = 0;
        } else {
            stuckTime += delta;
            if (stuckTime > 1.5f) { // walking into something: try a different way for a moment
                wanderDir.setToRandomDirection();
                wanderTime = 0.6f;
                stuckTime = 0;
                if (MathUtils.randomBoolean(0.3f)) buttons[Button.A.ordinal()] = true; // a table in the way: flip it
            }
        }
    }

    // ------------------------------------------------------------------ getting about

    /** Out of a fight: pickups, chests, the exit, the next room. */
    private void explore(Player p) {
        Room room = world.getRoom();
        targetTime += 1f / 60f;
        if (targetTime > 6f && target != null) { // can't get it (a heart at full health, behind a wall...)
            given.add(target);
            target = null;
        }
        Pickup pick = nearestPickup(p, room);
        if (pick != null) {
            aimTarget(pick);
            setGoalTo(p, pick.pos.x, pick.pos.y - 0.1f);
            return;
        }
        for (Chest c : room.chests) {
            if (c.opened || given.contains(c) || (c.locked && p.getKeys() == 0)) continue;
            aimTarget(c);
            float gx = c.cx;
            float gy = c.solid.y - 0.25f;
            setGoalTo(p, gx, gy);
            if (Vector2.dst(p.feetX(), p.feetY(), gx, gy) < 0.35f) {
                buttons[Button.A.ordinal()] = MathUtils.randomBoolean(0.3f);
            }
            return;
        }
        if (room.exit && ((StoryDirector) world.getDirector()).getStage() != null && room.type == Room.Type.BOSS) {
            aimTarget(room);
            setGoalTo(p, room.exitX, room.exitY - 0.05f);
            if (Vector2.dst(p.feetX(), p.feetY(), room.exitX, room.exitY) < 0.35f) {
                buttons[Button.A.ordinal()] = MathUtils.randomBoolean(0.3f);
            }
            return;
        }
        if (room.type == Room.Type.BOSS && !room.cleared) { // the boss wakes when you walk in
            aimTarget(room);
            setGoalTo(p, Arena.now.centerX, Arena.now.centerY - 1f);
            return;
        }
        int dir = nextDoor(room);
        if (dir < 0) {
            hasGoal = false;
            return;
        }
        Arena a = Arena.now;
        float gx = dir == Room.W ? a.left - 0.6f : dir == Room.E ? a.right + 0.6f : a.centerX;
        float gy = dir == Room.N ? a.top + 0.8f : dir == Room.S ? a.bottom - 0.6f : (a.doorY0 + a.doorY1) / 2f - 0.15f;
        aimTarget(dir);
        goalDoor = dir;
        setGoalTo(p, gx, gy);
    }

    private void aimTarget(Object t) {
        if (t != target) {
            target = t;
            targetTime = 0;
        }
    }

    private Pickup nearestPickup(Player p, Room room) {
        Pickup best = null;
        float bestD = Float.MAX_VALUE;
        for (Pickup k : room.pickups) {
            if (!k.alive || given.contains(k) || k.waitForLeave) continue;
            if (k.type == Pickup.Type.COIN) continue; // they fly to you
            if ((k.type == Pickup.Type.HEART || k.type == Pickup.Type.HALF_HEART) && p.getHealth() >= p.getMaxHealth()) continue;
            if (k.type == Pickup.Type.ITEM && p.getActive() != null && k.item != null && k.item.active) continue; // keep the one it has
            float d = k.pos.dst2(p.centerX(), p.feetY());
            if (d < bestD) {
                bestD = d;
                best = k;
            }
        }
        return best;
    }

    /**
     * The door to walk out of: towards the nearest room not yet seen (the boss room last). Rooms already
     * seen on the way are skipped by teleporting, like a player with the map would.
     */
    private int nextDoor(Room room) {
        StageMap map = world.getMap();
        if (map == null) return -1;
        Map<Room, Room> parent = new HashMap<>();
        Map<Room, Integer> firstDir = new HashMap<>();
        ArrayDeque<Room> queue = new ArrayDeque<>();
        queue.add(room);
        parent.put(room, null);
        Room found = null;
        Room boss = null;
        while (!queue.isEmpty()) {
            Room r = queue.poll();
            if (r != room && !r.visited) {
                if (r.type == Room.Type.BOSS) {
                    if (boss == null) boss = r;
                } else {
                    found = r;
                    break;
                }
                continue; // don't look past rooms not yet seen
            }
            for (int d = 0; d < 4; d++) {
                if (!r.doors[d]) continue;
                Room n = map.neighbor(r, d);
                if (n == null || parent.containsKey(n)) continue;
                parent.put(n, r);
                firstDir.put(n, r == room ? d : firstDir.get(r));
                queue.add(n);
            }
        }
        if (found == null) found = boss;
        if (found == null) {
            // everything seen and no way on: back to the boss room (its exit)
            if (map.boss != null && map.boss != room && world.canTeleportTo(map.boss)) world.teleportTo(map.boss);
            return -1;
        }
        Room before = parent.get(found);
        if (before != room && before != null && world.canTeleportTo(before)) {
            world.teleportTo(before); // skip the rooms in between
            hasGoal = false;
            return -1;
        }
        Integer d = firstDir.get(found);
        return d == null ? -1 : d;
    }

    private final Vector2 shotSpot = new Vector2();
    private float shotSpotAge = 99f;
    private Enemy shotSpotFor;

    /** The nearest free spot (by walking) with a clear shot at the enemy, not right on top of it. */
    private boolean findShotSpot(Player p, Enemy e, Vector2 out) {
        Arena a = Arena.now;
        int cols = Math.max(1, (int) ((a.right - a.left) / CELL));
        int rows = Math.max(1, (int) ((a.top - a.bottom) / CELL));
        int n = cols * rows;
        boolean[] seen = new boolean[n];
        int[] queue = new int[n];
        int head = 0;
        int tail = 0;
        int start = cell(p.feetX(), p.feetY(), cols, rows);
        queue[tail++] = start;
        seen[start] = true;
        while (head < tail) {
            int c = queue[head++];
            float x = cx(c, cols);
            float y = cy(c, cols);
            float d = Vector2.dst(x, y + 0.3f, e.pos.x, e.pos.y);
            if (d > 1f && d < 4.5f && world.hasLineOfSight(x, y + 0.35f, e.pos.x, e.pos.y, 0.05f)) {
                out.set(x, y);
                return true;
            }
            int col = c % cols;
            int row = c / cols;
            for (int k = 0; k < 4; k++) {
                int nc = col + (k == 0 ? 1 : k == 1 ? -1 : 0);
                int nr = row + (k == 2 ? 1 : k == 3 ? -1 : 0);
                if (nc < 0 || nr < 0 || nc >= cols || nr >= rows) continue;
                int m = nr * cols + nc;
                if (seen[m] || !free(cx(m, cols), cy(m, cols))) continue;
                seen[m] = true;
                queue[tail++] = m;
            }
        }
        return false;
    }

    private final Vector2 path = new Vector2();
    private float pathAge = 99f;
    private final Vector2 pathGoal = new Vector2();

    private void setGoalTo(Player p, float x, float y) {
        goal.set(x, y);
        hasGoal = true;
    }

    /** Which way to walk for the goal: the next corner of a grid path round the furniture. */
    private Vector2 goalDir(Player p) {
        if (!hasGoal) return null;
        float fx = p.feetX();
        float fy = p.feetY();
        pathAge += 1f / 60f;
        if (pathAge > 0.25f || pathGoal.dst2(goal) > 0.04f) {
            pathAge = 0;
            pathGoal.set(goal);
            if (!findPath(fx, fy, goal.x, goal.y, path)) path.set(goal);
        }
        tmp.set(path).sub(fx, fy);
        if (tmp.len2() < 0.0004f) tmp.set(goal).sub(fx, fy);
        return tmp.len2() < 0.0004f ? null : tmp.nor();
    }

    /** Grid search over free floor from the feet to the cell nearest the goal; writes the furthest straight-line waypoint. */
    private boolean findPath(float fx, float fy, float gx, float gy, Vector2 out) {
        Arena a = Arena.now;
        int cols = Math.max(1, (int) ((a.right - a.left) / CELL));
        int rows = Math.max(1, (int) ((a.top - a.bottom) / CELL));
        int n = cols * rows;
        int[] parent = new int[n];
        java.util.Arrays.fill(parent, -2);
        boolean[] ok = new boolean[n];
        for (int c = 0; c < n; c++) ok[c] = free(cx(c, cols), cy(c, cols));
        int start = cell(fx, fy, cols, rows);
        float cgx = MathUtils.clamp(gx, a.left + 0.2f, a.right - 0.2f);
        float cgy = MathUtils.clamp(gy, a.bottom + 0.2f, a.top - 0.2f);
        int goalCell = cell(cgx, cgy, cols, rows);
        int[] queue = new int[n];
        int head = 0;
        int tail = 0;
        queue[tail++] = start;
        parent[start] = -1;
        int best = start;
        float bestD = Float.MAX_VALUE;
        while (head < tail) {
            int c = queue[head++];
            float d = Vector2.dst2(cx(c, cols), cy(c, cols), cgx, cgy);
            if (d < bestD) {
                bestD = d;
                best = c;
            }
            if (c == goalCell) break;
            int col = c % cols;
            int row = c / cols;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    if (dx == 0 && dy == 0) continue;
                    int nc = col + dx;
                    int nr = row + dy;
                    if (nc < 0 || nr < 0 || nc >= cols || nr >= rows) continue;
                    int k = nr * cols + nc;
                    if (!ok[k] || parent[k] != -2) continue;
                    if (dx != 0 && dy != 0 && (!ok[row * cols + nc] || !ok[nr * cols + col])) continue;
                    parent[k] = c;
                    queue[tail++] = k;
                }
            }
        }
        if (best == start) {
            out.set(gx, gy);
            return true;
        }
        if (best == goalCell || bestD < 0.1f) {
            // the goal itself (a door, just past the wall) once it's in a straight line
            if (straight(fx, fy, cgx, cgy)) {
                out.set(gx, gy);
                return true;
            }
        }
        int step = best;
        for (int c = best; c != start && c >= 0; c = parent[c]) {
            step = c;
            if (straight(fx, fy, cx(c, cols), cy(c, cols))) break;
        }
        out.set(cx(step, cols), cy(step, cols));
        return true;
    }

    /** Can the feet go from (x1, y1) to (x2, y2) in a straight line: no walls, furniture or pits on the way. */
    private boolean straight(float x1, float y1, float x2, float y2) {
        if (!world.walkable(x1, y1 + 0.05f, x2, y2 + 0.05f, PROBE * 0.8f)) return false;
        float len = Vector2.dst(x1, y1, x2, y2);
        for (float d = 0.1f; d < len; d += 0.1f) {
            float k = d / len;
            if (!Arena.now.contains(x1 + (x2 - x1) * k, y1 + (y2 - y1) * k, 0f)) return true; // out through a door
            if (!free(x1 + (x2 - x1) * k, y1 + (y2 - y1) * k)) return false;
        }
        return true;
    }

    private static float cx(int c, int cols) {
        return Arena.now.left + (c % cols + 0.5f) * CELL;
    }

    private static float cy(int c, int cols) {
        return Arena.now.bottom + (c / cols + 0.5f) * CELL;
    }

    private static int cell(float x, float y, int cols, int rows) {
        int col = MathUtils.clamp((int) ((x - Arena.now.left) / CELL), 0, cols - 1);
        int row = MathUtils.clamp((int) ((y - Arena.now.bottom) / CELL), 0, rows - 1);
        return row * cols + col;
    }
}
