package com.trush.game.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ObjectFloatMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.trush.game.audio.Sfx;
import com.trush.game.effects.Particles;
import com.trush.game.enemy.Enemy;
import com.trush.game.enemy.FluffyShark;
import com.trush.game.enemy.LampMimic;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.RoomRenderer;
import com.trush.game.gfx.Sprites;
import com.trush.game.input.Gamepad;
import com.trush.game.launcher.Dev;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Bird;
import com.trush.game.objects.Bullet;
import com.trush.game.objects.Chest;
import com.trush.game.objects.Coins;
import com.trush.game.objects.Decoy;
import com.trush.game.objects.FluffulVoice;
import com.trush.game.objects.Grenade;
import com.trush.game.objects.Item;
import com.trush.game.objects.LittleCloud;
import com.trush.game.objects.Cat;
import com.trush.game.objects.Pickup;
import com.trush.game.objects.Prop;
import com.trush.game.objects.GasCloud;
import com.trush.game.objects.Puddle;
import com.trush.game.objects.SeaMine;
import com.trush.game.objects.Shop;
import com.trush.game.objects.Tier;
import com.trush.game.objects.Turret;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Hero;
import com.trush.game.player.Player;
import com.trush.game.save.Unlocks;
import lombok.Getter;

import java.util.Arrays;

/**
 * Owns every entity in the current room and runs the simulation: movement and collisions
 * against walls/furniture, bullets, pickups, effects and moving between rooms.
 * A {@link Director} decides what spawns (story stage or endless waves).
 */
@Getter
public class GameWorld {

    private static final Color ENEMY_BULLET = Color.valueOf("ff5e3a");
    private static final Color SKY = Color.valueOf("7ec8f0");
    private static final Color STORM_SKY = Color.valueOf("56607a");
    private static final Color CLOUD_PUFF = Color.valueOf("f4f2fc");
    private static final Color CORAL_BITS = Color.valueOf("ff8a6a");
    private static final Color KELP_BITS = Color.valueOf("4a9a4a");
    private static final Color DUMPSTER_BITS = Color.valueOf("3a6a4a");
    private static final Color SOOT = Color.valueOf("3a3434");
    private static final Color RUST = Color.valueOf("9a4a2a");
    private static final Color TRASH_BAG = Color.valueOf("2a2a30");
    private static final Color SEA_TINT = Color.valueOf("1a6aa8");
    private static final Color SEA_FOAM = Color.valueOf("d8f4ff");
    private static final Color GOLD = Color.valueOf("e8c040");
    private static final Color SPARK = Color.valueOf("fff3b0");
    private static final Color BLOOD = Color.valueOf("e43b44");
    private static final Color BRASS = Color.valueOf("d9b24c");
    private static final Color SMOKE = Color.valueOf("b8b8b8");
    private static final Color SHIELD = Color.valueOf("8fd8ff");
    private static final Color CANDY = Color.valueOf("ff9ed8");
    private static final Color FEATHER = Color.valueOf("fbf6ff");
    private static final Color TURRET_SHOT = Color.valueOf("ff8a6a");
    private static final Color QUILL = Color.valueOf("e8d8b0");
    private static final Color VOLT = Color.valueOf("ffe94a");
    private static final Color SNOW = Color.valueOf("dff4ff");
    private static final Color LAMB_GOLD = Color.valueOf("ffe9a8");
    /** The Grenade recharges, like ETG's actives. */
    private static final float GRENADE_COOLDOWN = 7f;
    private static final Color BLANK_BLUE = Color.valueOf("a8d8ff");
    private static final Color WOOD = Color.valueOf("a0642f");
    private static final Color STONE = Color.valueOf("8d8a86");
    private static final Color DUST = Color.valueOf("c8b49a");
    private static final Color LAMP_GLASS = Color.valueOf("ffd166");
    private static final Color LAMP_LIGHT = Color.valueOf("ffd890");

    private final ETGFanfic game;
    /** (Swapped for the sea's while the player is down there, see {@link #diveIntoSea}.) */
    private Director director;
    /** Null in endless mode. */
    private StageMap map;
    /** The dev flag -Dfluffy.floor=6 means the secret sea (it hangs below floor 5, the clouds). */
    public static final int SEA_FLOOR = 6;
    /** While in the secret sea: the floor above, the room the player dived from, and where they stood. */
    private StoryDirector above;
    private StageMap aboveMap;
    private Room diveRoom;
    private final Vector2 diveSpot = new Vector2();
    /** The fall in progress is into the sea hole (with the Flippers on, that's the way down). */
    private boolean fallingIntoSea;
    private Room room;
    private final Player player;
    private final Array<Enemy> enemies = new Array<>();
    private final Array<Enemy> pendingEnemies = new Array<>();
    /** Killed enemies playing their death animation (Enemy.updateDeath); a boss's holds the room's reward. */
    private final Array<Enemy> dying = new Array<>();
    /** Where the last boss fell: the camera stays on it a moment after its death show (bossDeathSpot). */
    private final Vector2 bossFallSpot = new Vector2();
    private float bossFallZoom = 1f;
    private float bossFallHold;
    /** How long the camera lingers on a beaten boss once its death show is over (its banner is up). */
    private static final float BOSS_FALL_HOLD = 2.2f;
    /** The skyscraper's elevator: a ride between two storeys (the world waits while it runs). */
    public static final float ELEVATOR_TIME = 1.9f;
    private float elevatorTimer;
    private int elevatorFrom;
    private int elevatorTo;
    /** Real seconds of slow motion left (a boss's killing blow). */
    private float slowMo;
    private final Array<Bullet> playerBullets = new Array<>(false, 128);
    private final Array<Bullet> enemyBullets = new Array<>(false, 512);
    private final Array<Grenade> grenades = new Array<>();
    /** The Toy Robot's turrets, the Cardboard Hero and the Little Cloud (see updateGadgets). */
    private final Array<Turret> turrets = new Array<>();
    private Decoy decoy;
    private final LittleCloud littleCloud = new LittleCloud();
    /** Granny's cat (Hero.GRANNY); {@code catTarget} is who it's leaping at. */
    private final Cat cat = new Cat();
    private Enemy catTarget;
    /** The Snow Globe: enemies and their bullets run slow while this counts down. */
    private float globeTimer;
    private static final float GLOBE_TIME = 5f;
    private static final float GLOBE_BULLET_SLOW = 0.4f;
    private static final float GLOBE_COOLDOWN = 20f;
    private static final float ROBOT_COOLDOWN = 16f;
    private static final float DECOY_COOLDOWN = 15f;
    /** Frying Pan swings drawn for a moment: x, y and the angle swung at; with their ages. */
    private final Array<Vector3> swings = new Array<>();
    private final FloatArray swingTimes = new FloatArray();
    private static final float SWING_TIME = 0.15f;
    /** The Polaroid's last flash: where from and which way (x, y, angle), and how long ago (negative: none). */
    private final Vector3 flashShot = new Vector3();
    private float flashTime = -1f;
    private static final float FLASH_TIME = 0.35f;
    private static final float FLASH_RANGE = 3.4f;
    private static final float FLASH_ARC = 34f;
    /** The clouds: birds flying across the room during a fight (see updateBirds). */
    private final Array<Bird> birds = new Array<>();
    private float birdTimer;
    /** Seconds since the world started (for wobbly effects). */
    private float clock;
    /** Blast flashes: x, y and radius. */
    private final Array<Vector3> explosions = new Array<>();
    /** Laser beams fired this moment (drawn for a few frames). */
    private final Array<Beam> beams = new Array<>();
    private final FloatArray explosionTimes = new FloatArray();
    private final Particles particles = new Particles();
    private final RoomRenderer roomRenderer = new RoomRenderer();
    private final Vector2 aim = new Vector2();

    private float trauma;
    private float blankRing = -1f;
    /** After a blank, enemies are dazed this long: no new attacks, a moment to get away (like ETG). */
    private float blankCalm;
    private static final float BLANK_CALM = 1.1f;
    private final Vector2 blankCenter = new Vector2();
    private int score;
    private int kills;
    private float difficulty = 1f;
    /** 1 -> 0 fade after walking through a door. */
    private float fade;

    private String toast;
    private float toastTimer;
    /** What the camera shows this frame (set by GameScreen), so enemies don't attack from off screen. */
    private final Rectangle view = new Rectangle(0, 0, Arena.VIEW_W, Arena.VIEW_H);
    /** An ammo box already dropped in this room (or endless wave): one is plenty. */
    private boolean ammoDropped;
    /** The current boss fight is still without a scratch (a flawless kill drops a heart container). */
    private boolean bossFlawless;
    /** Shot Baa-llo one time too many: his prices are up by half for the rest of the run (saved between floors). */
    private boolean shopGrudge;
    /** Endless: the bell on the travelling stall's counter was rung - on with the next wave. */
    private boolean bellRung;
    /** Coins are picked up from a little further than other pickups (they're small and they scatter). */
    private static final float COIN_REACH = 0.38f;
    /** How long the coins lie still after a fight before they fly to the player (long enough to see the pile). */
    private static final float COIN_RUSH_DELAY = 0.5f;
    /** Time since the last fight ended (0 during a fight): once the room is calm, coins come to you, like ETG's casings. */
    private float calm;
    /** Fluff Field: regular enemies are pushed back out to this far from the hero's belly, at this speed. */
    private static final float FIELD_RADIUS = 1.1f;
    private static final float FIELD_PUSH = 3.5f;
    /** Static Fuzz: enemies this close get zapped, each at most once per {@link #ZAP_COOLDOWN}. */
    private static final float FUZZ_RADIUS = 1.25f; // (past the Fluff Field's edge: the two together zap what's held back)
    private static final float FUZZ_DAMAGE = 3f;
    private static final float ZAP_COOLDOWN = 0.5f;
    private final ObjectFloatMap<Enemy> zapTimers = new ObjectFloatMap<>();
    /** Fire and acid on the floor of this room (dynamite fires, flares, acid globs); see Puddle. */
    private final Array<Puddle> puddles = new Array<>();
    private static final int MAX_PUDDLES = 30;
    /** Smoke and stink hanging in the air of this room; see GasCloud. */
    private final Array<GasCloud> gas = new Array<>();
    private static final int MAX_GAS = 18;
    private boolean inSmoke;
    private boolean inStink;
    /** How long a dynamite's fire burns on the floor. */
    public static final float FIRE_LIFE = 4f;
    /** Ember Rounds / Frost Rounds: the share of bullets that set on fire / freeze, and for how long. */
    private static final float ROUNDS_CHANCE = 0.2f;
    private static final float EMBER_BURN = 3f;
    private static final float FROST_FREEZE = 1.3f;
    private static final Color EMBER = Color.valueOf("ff7a2a");
    private static final Color FROST = Color.valueOf("bfeaff");
    private static final Color ACID_GREEN = Color.valueOf("8ae83a");
    /** Pom-Pom Moons: three pom-poms circling the hero; they stop enemy bullets and bump into enemies. */
    private static final int MOONS = 3;
    private static final float MOON_ORBIT = 0.45f;
    private static final float MOON_RADIUS = 0.075f;
    private static final float MOON_SPEED = 210f;
    private static final float MOON_DAMAGE = 4f;
    private static final float MOON_HIT_COOLDOWN = 0.3f;
    private static final Color[] MOON_COLORS = {Color.valueOf("ff8ac8"), Color.valueOf("8ad8ff"), Color.valueOf("ffe08a")};
    private float moonAngle;
    private final float[] moonCooldown = new float[MOONS];
    /** Flippers: a roll ends in up to this many bubbles, each after an enemy within reach - weak, and not every roll. */
    private static final int ROLL_BUBBLES = 3;
    private static final float BUBBLE_REACH = 3f;
    private static final float BUBBLE_DAMAGE = 3f;
    private static final float BUBBLE_COOLDOWN = 1.2f;
    private static final Color BUBBLE_BLUE = Color.valueOf("9ae8ff");
    private float bubbleCooldown;
    /** The shop stand the player stood at last frame (Baa-llo hints when they step up to the flippers). */
    private Shop.Slot lookedAt;
    /** 0-1: the Fluff Field glows up while it's holding something back. */
    private float fieldGlow;
    private static final Color FIELD_PINK = Color.valueOf("ff8ac8");
    private static final Color ZAP_BLUE = Color.valueOf("8ec8ff");
    private static final Color WOOL = Color.valueOf("fbfaf5");
    private static final Color PRICE = Color.valueOf("ffe08a");
    private static final Color TOO_DEAR = Color.valueOf("ff7a6a");
    private static final Color SALE = Color.valueOf("7aff8a");

    /** The boss room's exit hatch (placed when it opens); walking onto it and pressing interact ends the floor. */
    private float exitX() {
        return room.exitX;
    }

    private float exitY() {
        return room.exitY;
    }
    /** The boss room's way on (a gate, a bus stop...): what it looks like and the spot you use it from. */
    private Stage.Exit exitKind() {
        return room.stage.exit;
    }
    /** How close the feet must be to a chest to open it. */
    private static final float CHEST_REACH = 0.45f;
    /** Set when the player goes down the exit hatch. */
    private boolean exitTaken;
    /** What the interact button would do right now ("OPEN", "DESCEND"...), or null; shown above the target. */
    private String prompt;
    private final Vector2 promptPos = new Vector2();

    private final Rectangle tmpRect = new Rectangle();
    private final Rectangle bodyRect = new Rectangle();
    private final Rectangle holeRect = new Rectangle();
    /** Where the player stood last time they were well clear of every pit: they climb out there after a fall. */
    private final Vector2 lastSafe = new Vector2();
    private final Vector2 tmpVec = new Vector2();
    private final Vector2 spawnPoint = new Vector2();
    private final Array<Object> drawList = new Array<>();

    /** Half-width of the line used for "can this enemy shoot the player" checks (about a bullet). */
    public static final float SIGHT_THICKNESS = 0.07f;
    // enemy navigation: a 0.25-unit grid over the floor, rebuilt lazily once per frame
    private static final float NAV_CELL = 0.25f;
    /** Nav grid size for the current room (2 nav cells per layout cell); the arrays fit the biggest room. */
    private int navCols = 26;
    private int navRows = 16;
    private static final int NAV_MAX = 64 * 40;
    private static final float NAV_CLEARANCE = 0.17f;
    private final boolean[] navFree = new boolean[NAV_MAX];
    private final int[] navParent = new int[NAV_MAX];
    private final int[] navQueue = new int[NAV_MAX];
    private boolean navDirty = true;
    private final Rectangle navRect = new Rectangle();

    /** Story mode: a stage map of rooms. */
    public GameWorld(ETGFanfic game, StoryDirector story) {
        this(game, story, story.getMap(), story.startRoom());
    }

    /** Endless mode: one closed arena room with a random layout. */
    public GameWorld(ETGFanfic game, WaveDirector waves) {
        this(game, waves, null, arenaRoom());
    }

    private static Room arenaRoom() {
        Room r = new Room(0, 0, Room.Type.ARENA);
        r.arena = new Arena(Arena.MEDIUM_COLS, Arena.MEDIUM_ROWS);
        r.stage = Stage.forFloor(MathUtils.random(1, Stage.lastFloor())); // (not the secret sea)
        r.theme = r.stage.combatThemes[MathUtils.random(r.stage.combatThemes.length - 1)];
        // dev shortcut: -Dfluffy.layout=N picks RoomTemplates.COMBAT[N] for the endless arena
        int layout = Dev.number("layout", -1);
        r.template = layout >= 0 && layout < RoomTemplates.COMBAT.length
                ? RoomTemplates.COMBAT[layout] : RoomTemplates.randomCombat();
        r.rebuildWalls();
        return r;
    }

    private GameWorld(ETGFanfic game, Director director, StageMap map, Room first) {
        this.game = game;
        this.director = director;
        this.map = map;
        this.player = new Player(game.sprites, Hero.current);
        player.addCoins(Dev.number("coins", 0)); // dev builds: -Dfluffy.coins=500 to go shopping
        enterRoom(first, -1);
        // dev builds: -Dfluffy.floor=6 (or -Dfluffy.sea=true) starts in the secret sea, below floor 5
        if (director instanceof StoryDirector && (Dev.flag("sea") || Dev.number("floor", 1) == SEA_FLOOR)) {
            diveSpot.set(lastSafe);
            diveIntoSea();
            if (Dev.flag("bossRoom")) { // with -Dfluffy.bossRoom=true: straight to the Fluffy Shark's lair
                enterRoom(this.map.boss, -1); // (this.map: the sea's - the parameter is still the clouds')
            }
        }
    }

    public Sfx sfx() {
        return game.sfx;
    }

    public void setView(float centerX, float centerY, float zoom) {
        float w = Arena.VIEW_W * zoom;
        float h = Arena.VIEW_H * zoom;
        view.set(centerX - w / 2f, centerY - h / 2f, w, h);
    }

    /** True if (x, y) is on screen, at least {@code margin} inside its edges. */
    public boolean onScreen(float x, float y, float margin) {
        return x > view.x + margin && x < view.x + view.width - margin
                && y > view.y + margin && y < view.y + view.height - margin;
    }

    /** The storey the elevator shows right now (counts from where you got in to where you're going). */
    public int elevatorFloor() {
        float k = MathUtils.clamp(1f - (elevatorTimer - 0.35f) / (ELEVATOR_TIME - 0.7f), 0f, 1f);
        return Math.round(elevatorFrom + (elevatorTo - elevatorFrom) * Interpolation.smooth.apply(k));
    }

    /** A beaten boss is still going through its death show (the room's reward waits for it). */
    public boolean bossDying() {
        for (int i = 0; i < dying.size; i++) {
            if (dying.get(i).boss) return true;
        }
        return false;
    }

    /**
     * Where the camera should look while a beaten boss goes down: on it during its death show, then on
     * the spot where it fell for a moment while its banner is up; null otherwise. {@link #bossDeathZoom}
     * is the zoom to go with it.
     */
    public Vector2 bossDeathSpot() {
        for (int i = 0; i < dying.size; i++) {
            Enemy e = dying.get(i);
            if (e.boss) {
                bossFallZoom = e.viewZoom();
                return bossFallSpot.set(e.pos);
            }
        }
        return bossFallHold > 0 ? bossFallSpot : null;
    }

    public float bossDeathZoom() {
        return bossFallZoom;
    }

    /** Furniture endless mode restocks the arena with (from every stage). */
    private static final Prop.Kind[] RESTOCK = {Prop.Kind.BARREL, Prop.Kind.CRATE, Prop.Kind.HAY, Prop.Kind.PILLAR,
            Prop.Kind.BENCH, Prop.Kind.LAMP, Prop.Kind.CAR, Prop.Kind.TABLE};

    /**
     * Endless, after a boss wave: new furniture from any stage takes the place of what was shot to
     * pieces - never more than the arena started with, away from the player and anything lying around.
     */
    public void restockProps() {
        Arena a = Arena.now;
        int missing = room.startingProps - room.props.size;
        for (int i = 0; i < missing; i++) {
            for (int tries = 0; tries < 40; tries++) {
                Prop.Kind kind = RESTOCK[MathUtils.random(RESTOCK.length - 1)];
                if (room.stage.isSea()) { // the flooded arena gets sea things
                    kind = Prop.seaKindFor(kind);
                }
                int col = MathUtils.random(1, a.cols - 3);
                int row = MathUtils.random(1, a.rows - 3);
                Prop p = new Prop(kind, a.left + Arena.CELL * col + (kind.wide() ? Arena.CELL : Arena.CELL / 2f),
                        a.bottom + Arena.CELL * row + Arena.CELL / 2f);
                navRect.set(p.solid.x - 0.35f, p.solid.y - 0.35f, p.solid.width + 0.7f, p.solid.height + 0.7f);
                if (findSolid(navRect) != null || Vector2.dst(p.cx, p.cy, player.feetX(), player.feetY()) < 1.6f
                        || restockBlocked(navRect)) {
                    continue;
                }
                room.props.add(p);
                particles.burst(p.cx, p.cy, 12, 0.3f, 1.2f, 0.5f, 0.05f, DUST);
                break;
            }
        }
        navDirty = true;
    }

    /** Something lies there that new furniture mustn't land on: a pickup, a chest, a pit. */
    private boolean restockBlocked(Rectangle r) {
        for (Pickup p : room.pickups) {
            if (r.contains(p.pos)) return true;
        }
        for (Chest c : room.chests) {
            if (r.overlaps(c.solid)) return true;
        }
        for (Rectangle pit : room.pits) {
            if (r.overlaps(pit)) return true;
        }
        return false;
    }

    /** Adds furniture to the current room during play (e.g. a boss's statue). */
    public void addProp(Prop p) {
        room.props.add(p);
        navDirty = true;
        if (p.solid.overlaps(player.feetRect(tmpRect))) { // never trap the player inside it
            player.setFeet(player.feetX(), p.solid.y - 0.16f);
        }
    }

    /** The boss currently fighting (not still spawning), or null. */
    public Enemy activeBoss() {
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.boss && e.alive && !e.isSpawning()) return e;
        }
        return null;
    }

    public Gamepad pad() {
        return game.pad;
    }

    public Particles particles() {
        return particles;
    }

    public Array<Pickup> getPickups() {
        return room.pickups;
    }

    public void setDifficulty(float difficulty) {
        this.difficulty = difficulty;
    }

    /** The Flufful's chatter (the talking gun); GameScreen draws its speech bubble. */
    private final FluffulVoice flufful = new FluffulVoice();
    private boolean fluffulMet;
    private Weapon lastHeld;
    private boolean heldWasReloading;

    private boolean fluffulHeld() {
        return player.currentWeapon().isFlufful() && !player.isDead();
    }

    /** Tells the Flufful something happened; it only talks while in the player's hands. */
    public void fluffulSays(FluffulVoice.Event e) {
        if (fluffulHeld()) {
            flufful.say(e);
        }
    }

    /** Picking the Flufful up / switching to it or away, and reloading it, get a line. */
    private void updateFlufful(float delta) {
        Weapon held = player.currentWeapon();
        if (held != lastHeld) {
            if (held.isFlufful()) {
                flufful.say(fluffulMet ? FluffulVoice.Event.SWITCH_TO : FluffulVoice.Event.MEET);
                fluffulMet = true;
            } else if (lastHeld != null && lastHeld.isFlufful()) {
                flufful.say(FluffulVoice.Event.SWITCH_AWAY);
            }
            lastHeld = held;
        }
        if (held.isFlufful() && held.isReloading() && !heldWasReloading) {
            flufful.say(FluffulVoice.Event.RELOAD);
        }
        heldWasReloading = held.isReloading();
        flufful.update(delta, fluffulHeld(), director.inFight(this));
    }

    /** Regular enemies spawn with this much of their health (later floors and waves: tougher). */
    private float enemyHealth = 1f;

    public void setEnemyHealth(float scale) {
        enemyHealth = scale;
    }

    /** Carries the score over from the previous floor. */
    public void restoreStats(int score, int kills) {
        this.score = score;
        this.kills = kills;
    }

    public void addScore(int amount) {
        score += amount;
    }

    public void shake(float amount) {
        trauma = Math.min(1f, trauma + amount);
    }

    public void toast(String text) {
        toast = text;
        toastTimer = 2.2f;
    }

    // ------------------------------------------------------------ rooms

    /** @param fromDir direction the player walked to get here (Room.N...), or -1 for the first room. */
    private void enterRoom(Room next, int fromDir) {
        room = next;
        ammoDropped = false;
        Arena.now = next.arena; // gameplay code (enemies, bullets...) works in the current room's geometry
        room.build();
        room.rebuildWalls();
        enemies.clear();
        pendingEnemies.clear();
        dying.clear();
        bossFallHold = 0f;
        turrets.clear(); // gadgets stay behind in the room you left
        decoy = null;
        littleCloud.reset();
        cat.reset();
        catTarget = null;
        swings.clear();
        swingTimes.clear();
        playerBullets.clear();
        enemyBullets.clear();
        particles.clear();
        grenades.clear();
        birds.clear();
        birdTimer = MathUtils.random(4f, 6f);
        explosions.clear();
        explosionTimes.clear();
        beams.clear();
        zapTimers.clear();
        puddles.clear();
        for (Vector3 pool : room.acidPools) { // the Slums' old acid, there all along
            Puddle old = new Puddle(Puddle.Kind.ACID, pool.x, pool.y, pool.z, 10f, false);
            old.old = true;
            puddles.add(old);
        }
        gas.clear();
        blankRing = -1f;
        navDirty = true;

        float fx;
        float fy;
        switch (fromDir) {
            case Room.N: // came in through the bottom door
                fx = Arena.now.centerX;
                fy = Arena.now.bottom + 0.12f;
                break;
            case Room.S:
                fx = Arena.now.centerX;
                fy = Arena.now.top - 0.45f;
                break;
            case Room.E:
                fx = Arena.now.left + 0.3f;
                fy = (Arena.now.doorY0 + Arena.now.doorY1) / 2f - 0.15f;
                break;
            case Room.W:
                fx = Arena.now.right - 0.3f;
                fy = (Arena.now.doorY0 + Arena.now.doorY1) / 2f - 0.15f;
                break;
            default:
                fx = Arena.now.centerX;
                fy = Arena.now.bottom + 0.5f;
                break;
        }
        player.setFeet(fx, fy);
        lastSafe.set(fx, fy);
        if (fromDir >= 0) {
            fade = 1f;
        }
        director.onRoomEntered(room, this);
        if (room.shop != null) {
            room.shop.greet(shopGrudge);
        }
    }

    private void checkRoomTransition() {
        if (map == null || player.isDead()) {
            return;
        }
        float x = player.feetX();
        float y = player.feetY() + 0.07f;
        int dir = -1;
        if (y > Arena.now.top + 0.3f) dir = Room.N;
        else if (y < Arena.now.bottom - 0.05f) dir = Room.S;
        else if (x < 0.12f) dir = Room.W;
        else if (x > Arena.now.worldW - 0.12f) dir = Room.E;
        if (dir >= 0 && room.doorOpen(dir)) {
            Room next = map.neighbor(room, dir);
            if (next != null) {
                if (room.stage.isTower() && next.floorNumber != room.floorNumber) {
                    // the skyscraper: up (or down) by elevator, counting the storeys
                    elevatorFrom = room.floorNumber;
                    elevatorTo = next.floorNumber;
                    elevatorTimer = ELEVATOR_TIME;
                    sfx().play("door", 0.8f);
                }
                enterRoom(next, dir);
            }
        }
    }

    /** True if the player can teleport (from the map) to {@code target} right now. */
    public boolean canTeleportTo(Room target) {
        return map != null && target != null && target != room && target.visited && !room.isLocked()
                && !player.isDead() && !player.isFalling();
    }

    /** Teleports the player to an already visited room (from the map; not in the middle of a fight). */
    public boolean teleportTo(Room target) {
        if (!canTeleportTo(target)) {
            return false;
        }
        enterRoom(target, -1);
        fade = 1f;
        particles.burst(player.centerX(), player.hitboxY(), 16, 0.3f, 1.4f, 0.5f, 0.05f, BLANK_BLUE);
        sfx().play("spawn", 1f);
        return true;
    }

    /** True when the player's feet are at least {@code margin} away from every wall. */
    public boolean playerInsideRoom(float margin) {
        return Arena.now.contains(player.feetX(), player.feetY() + 0.07f, margin);
    }

    // ------------------------------------------------------------ collision

    /** First movement-blocking rectangle (wall or prop) overlapping {@code r}, or null. */
    public Rectangle findSolid(Rectangle r) {
        return findSolid(r, false);
    }

    /** @param overTables a dodge roll: standing tables don't block (you roll across them, like ETG). */
    public Rectangle findSolid(Rectangle r, boolean overTables) {
        for (Rectangle w : room.walls) {
            if (w.overlaps(r)) return w;
        }
        for (Prop p : room.props) {
            if (p.alive && !(overTables && p.canFlip()) && p.solid.overlaps(r)) return p.solid;
        }
        for (Chest c : room.chests) {
            if (c.solid.overlaps(r)) return c.solid;
        }
        return null;
    }

    private static boolean circleOverlaps(Rectangle r, float x, float y, float radius) {
        float cx = MathUtils.clamp(x, r.x, r.x + r.width);
        float cy = MathUtils.clamp(y, r.y, r.y + r.height);
        float dx = x - cx;
        float dy = y - cy;
        return dx * dx + dy * dy < radius * radius;
    }

    /** Pushes a circle out of walls and props (used for enemies). */
    public void pushOutOfSolids(Vector2 p, float radius) {
        for (int pass = 0; pass < 2; pass++) {
            for (Prop prop : room.props) {
                if (prop.alive) pushOut(prop.solid, p, radius);
            }
            for (Chest c : room.chests) {
                pushOut(c.solid, p, radius);
            }
            for (Rectangle w : room.walls) {
                pushOut(w, p, radius);
            }
        }
    }

    private static void pushOut(Rectangle r, Vector2 p, float radius) {
        float cx = MathUtils.clamp(p.x, r.x, r.x + r.width);
        float cy = MathUtils.clamp(p.y, r.y, r.y + r.height);
        float dx = p.x - cx;
        float dy = p.y - cy;
        float d2 = dx * dx + dy * dy;
        if (d2 >= radius * radius) {
            return;
        }
        if (d2 > 1e-6f) {
            float d = (float) Math.sqrt(d2);
            p.x += dx / d * (radius - d);
            p.y += dy / d * (radius - d);
        } else { // centre inside the rect: push out along the shortest axis
            float left = p.x - r.x;
            float right = r.x + r.width - p.x;
            float down = p.y - r.y;
            float up = r.y + r.height - p.y;
            float m = Math.min(Math.min(left, right), Math.min(down, up));
            if (m == left) p.x = r.x - radius;
            else if (m == right) p.x = r.x + r.width + radius;
            else if (m == down) p.y = r.y - radius;
            else p.y = r.y + r.height + radius;
        }
    }

    /** True if a circle at (x, y) is inside the room and clear of furniture and chests. */
    public boolean pointIsFree(float x, float y, float radius) {
        if (!Arena.now.contains(x, y, radius)) return false;
        for (Prop p : room.props) {
            if (p.alive && circleOverlaps(p.solid, x, y, radius)) return false;
        }
        for (Chest c : room.chests) {
            if (circleOverlaps(c.solid, x, y, radius)) return false;
        }
        for (Rectangle pit : room.pits) {
            if (circleOverlaps(pit, x, y, radius)) return false;
        }
        return true;
    }

    /** A random free spot for an enemy, preferably at least {@code minPlayerDist} from the player. */
    public Vector2 findSpawnPoint(float minPlayerDist) {
        float bestScore = -1;
        spawnPoint.set(Arena.now.centerX, Arena.now.top - 0.6f);
        for (int i = 0; i < 60; i++) {
            float x = MathUtils.random(Arena.now.left + 0.4f, Arena.now.right - 0.4f);
            float y = MathUtils.random(Arena.now.bottom + 0.4f, Arena.now.top - 0.4f);
            if (!pointIsFree(x, y, 0.3f)) continue;
            float d = Vector2.dst(x, y, player.centerX(), player.hitboxY());
            float nearest = 9;
            for (Enemy e : enemies) nearest = Math.min(nearest, e.pos.dst(x, y));
            for (Enemy e : pendingEnemies) nearest = Math.min(nearest, e.pos.dst(x, y));
            if (d > minPlayerDist && nearest > 0.6f) {
                return spawnPoint.set(x, y);
            }
            float s = Math.min(d, 3.5f) + Math.min(nearest, 1f);
            if (s > bestScore) {
                bestScore = s;
                spawnPoint.set(x, y);
            }
        }
        return spawnPoint;
    }

    // ------------------------------------------------------------ pits (open hatches in the town)

    /** True if (x, y) is inside a pit's hole shrunk by {@code margin} (negative = grown). */
    public boolean inPit(float x, float y, float margin) {
        for (Rectangle pit : room.pits) {
            Room.hole(pit, holeRect);
            if (x > holeRect.x + margin && x < holeRect.x + holeRect.width - margin
                    && y > holeRect.y + margin && y < holeRect.y + holeRect.height - margin) {
                return true;
            }
        }
        return false;
    }

    private void updatePlayerPits() {
        if (player.isDead() || player.isFalling() || room.pits.isEmpty()) {
            return;
        }
        float fx = player.feetX();
        float fy = player.feetY() + 0.07f;
        if (!player.isRolling() && inPit(fx, fy, 0.05f)) { // rolling over a pit is fine, stopping on it isn't
            fallingIntoSea = room.seaPit != null && room.seaPit.contains(fx, fy);
            player.startFall();
            sfx().play("dodge", 1f);
            particles.burst(fx, fy, 8, 0.2f, 0.8f, 0.4f, 0.04f, DUST);
        } else if (!player.isRolling() && !inPit(fx, fy, -0.35f)) {
            lastSafe.set(fx, fy);
        }
    }

    /** Called when the fall animation is over: climb back out at the last safe spot, half a heart lighter. */
    public void onPlayerFell() {
        if (fallingIntoSea && player.hasItem(Item.FLIPPERS)) { // the secret: down to the sea, no harm done
            fallingIntoSea = false;
            diveSpot.set(lastSafe);
            diveIntoSea();
            return;
        }
        player.setFeet(lastSafe.x, lastSafe.y);
        player.clearInvulnerability();
        damagePlayer();
        if (fallingIntoSea) {
            fallingIntoSea = false;
            toast("THE SEA IS FAR TOO DEEP TO SWIM IN...");
        }
    }

    /**
     * Through the hole in the clouds, flippers on: a splash, and the secret sea below this floor. The floor
     * above waits (its map, the room the player dived from) until the waterspout brings them back up.
     */
    private void diveIntoSea() {
        Unlocks.findSea(game.prefs); // endless gets its sea waves from now on
        above = (StoryDirector) director;
        aboveMap = map;
        diveRoom = room;
        StoryDirector sea = above.diveIntoSea();
        director = sea;
        map = sea.getMap();
        enterRoom(sea.getMap().start, -1);
        player.setFeet(Arena.now.centerX, Arena.now.centerY - 0.6f);
        lastSafe.set(player.feetX(), player.feetY());
        player.clearInvulnerability();
        fade = 1f;
        splash(player.centerX(), player.feetY());
    }

    /**
     * The waterspout after the sea's boss: back up through the hole, into the room the player dived from
     * - its fight (if they dove out of one) counts as won, so they can catch their breath, shop, open a chest.
     */
    private void surface() {
        director = above;
        map = aboveMap;
        above = null;
        aboveMap = null;
        enterRoom(diveRoom, -1);
        player.setFeet(diveSpot.x, diveSpot.y);
        lastSafe.set(diveSpot);
        ((StoryDirector) director).surfaced(this);
        player.ensureBlanks(2); // like starting a floor: at least two blanks (and the Spare Pocket's extra)
        if (player.hasItem(Item.SPARE_POCKET)) {
            player.addBlank();
        }
        if (room.seaPit != null) { // the hole clouds over behind you: the sea is one trip per run
            room.pits.removeValue(room.seaPit, true);
            particles.burst(room.seaPit.x + 0.5f, room.seaPit.y + 0.5f, 20, 0.4f, 1.4f, 0.8f, 0.07f, CLOUD_PUFF);
            room.seaPit = null;
            navDirty = true;
        }
        fade = 1f;
        splash(player.centerX(), player.feetY());
    }

    /** Endless: the arena's own look, kept while the sea waves flood it. */
    private Stage dryStage;
    private String dryTheme;
    /** The arena's own furniture, by the sea furniture standing in for it while flooded. */
    private final ObjectMap<Prop, Prop> dryProps = new ObjectMap<>();

    /**
     * Endless, waves 26-30: the arena floods - it takes on the sea's look (and its underwater tint) - and
     * after the shark's wave the water goes out again. The furniture stays as it was.
     */
    public void floodArena(boolean flooded) {
        if (flooded == room.stage.isSea()) {
            return;
        }
        if (flooded) {
            dryStage = room.stage;
            dryTheme = room.theme;
            room.stage = Stage.SEA;
            room.theme = Stage.SEA.combatThemes[MathUtils.random(Stage.SEA.combatThemes.length - 1)];
            toast("THE ARENA FLOODS!");
        } else {
            room.stage = dryStage != null ? dryStage : Stage.VILLAGE;
            room.theme = dryTheme != null ? dryTheme : Stage.VILLAGE.combatThemes[0];
            toast("THE TIDE GOES OUT...");
        }
        swapFurniture(flooded);
        if (flooded) { // fires go out under water
            for (int i = puddles.size - 1; i >= 0; i--) {
                if (puddles.get(i).kind == Puddle.Kind.FIRE) puddles.removeIndex(i);
            }
            player.douse();
            for (Enemy e : enemies) e.douse();
        }
        roomRenderer.forget();
        fade = 1f;
        splash(player.centerX(), player.feetY());
    }

    /**
     * Flooding: every piece of furniture still standing turns into its sea version (a table into coral, a
     * pillar into a rock spire, a barrel into seaweed, a crate into a giant clam), in the same spot. Draining:
     * back again - what was shot to pieces under water stays gone, and sea furniture that was never
     * anything else (restocked under water) becomes something from the arena's own stage.
     */
    private void swapFurniture(boolean flooded) {
        for (int i = 0; i < room.props.size; i++) {
            Prop p = room.props.get(i);
            if (!p.alive || p.isShopFurniture() || p.isSeaFurniture() == flooded) continue;
            Prop swap;
            if (flooded) {
                swap = new Prop(Prop.seaKindFor(p.kind), p.cx, p.cy);
                dryProps.put(swap, p);
            } else {
                Prop dry = dryProps.get(p);
                if (dry == null) {
                    Prop.Kind kind = room.stage.propFor(p.kind == Prop.Kind.CORAL ? 'H' : p.kind == Prop.Kind.ROCK ? 'P'
                            : p.kind == Prop.Kind.CLAM ? 'C' : 'B');
                    dry = new Prop(kind != null ? kind : Prop.Kind.BARREL, p.cx, p.cy);
                } else if (dry.joinedAbove != null) {
                    dry.unjoin();
                }
                swap = dry;
            }
            room.props.set(i, swap);
        }
        if (!flooded) {
            dryProps.clear();
        }
        for (Prop p : room.props) {
            if (p.joinedAbove != null) p.unjoin();
        }
        room.joinStacks();
        navDirty = true;
    }

    /** True while the player is down in the secret sea. */
    public boolean inTheSea() {
        return above != null;
    }

    private void splash(float x, float y) {
        particles.burst(x, y, 24, 0.5f, 2.2f, 0.7f, 0.05f, SEA_FOAM);
        particles.burst(x, y, 12, 0.3f, 1.2f, 0.9f, 0.07f, SEA_TINT);
        sfx().play("pop", 1f);
        shake(0.25f);
    }

    /**
     * Walking enemies keep off the pits' edges - unless a bullet is shoving them (knockback), then they
     * can't stop and may go over. A fall counts as a kill. Bosses and flyers don't care.
     */
    private void updateEnemyPits() {
        if (room.pits.isEmpty()) {
            return;
        }
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.boss || e.flying || e.isSpawning() || !e.alive) continue;
            if (inPit(e.pos.x, e.pos.y, 0.08f)) {
                e.fellInPit = true;
                e.hit(e.hp + 1f, tmpVec.set(0, -1), 0f, this);
                particles.burst(e.pos.x, e.pos.y, 10, 0.3f, 1.2f, 0.4f, 0.05f, DUST);
                sfx().play("body_fall", 0.7f);
                continue;
            }
            if (e.knockSpeed() < 0.6f) {
                for (Rectangle pit : room.pits) {
                    pushOut(Room.hole(pit, holeRect), e.pos, e.radius * 0.5f);
                }
            }
        }
    }

    // ------------------------------------------------------------ enemy sight and navigation

    private boolean segmentHits(Rectangle r, float x1, float y1, float x2, float y2, float thickness) {
        navRect.set(r.x - thickness, r.y - thickness, r.width + 2 * thickness, r.height + 2 * thickness);
        return Intersector.intersectSegmentRectangle(x1, y1, x2, y2, navRect);
    }

    /** True if a bullet could fly from (x1, y1) to (x2, y2): no wall or bullet-blocking prop in between. */
    public boolean hasLineOfSight(float x1, float y1, float x2, float y2, float thickness) {
        for (Rectangle w : room.walls) {
            if (segmentHits(w, x1, y1, x2, y2, thickness)) return false;
        }
        for (Prop p : room.props) {
            if (p.alive && p.blocksBullets() && segmentHits(p.bulletRect(game.sprites, y2 > y1), x1, y1, x2, y2, thickness)) return false;
        }
        return true;
    }

    /**
     * How far a beam (laser) from (x, y) going at {@code angle} travels before a wall or a
     * bullet-blocking prop stops it, up to {@code max}.
     */
    public float rayLength(float x, float y, float angle, float max) {
        float dx = MathUtils.cosDeg(angle);
        float dy = MathUtils.sinDeg(angle);
        for (float d = 0; d < max; d += 0.05f) {
            float px = x + dx * d;
            float py = y + dy * d;
            if (!Arena.now.contains(px, py, 0f)) return d;
            for (Prop p : room.props) {
                if (p.alive && p.blocksBullets() && p.bulletRect(game.sprites, dy > 0).contains(px, py)) return d;
            }
        }
        return max;
    }

    /** True if something of the given radius could walk in a straight line from (x1, y1) to (x2, y2). */
    public boolean walkable(float x1, float y1, float x2, float y2, float radius) {
        for (Rectangle w : room.walls) {
            if (segmentHits(w, x1, y1, x2, y2, radius)) return false;
        }
        for (Prop p : room.props) {
            if (p.alive && segmentHits(p.solid, x1, y1, x2, y2, radius)) return false;
        }
        for (Chest c : room.chests) {
            if (segmentHits(c.solid, x1, y1, x2, y2, radius)) return false;
        }
        return true;
    }

    /**
     * For big enemies that don't fit the nav grid (the boss): the nearest of a few random free spots that
     * can be walked to in a straight line (anywhere, if {@code flying}) and has a clear shot at the player, at least {@code minDist}
     * away from them. Returns false if none of the candidates works.
     */
    public boolean findOpenShot(Vector2 from, float radius, float minDist, boolean flying, Vector2 out) {
        float px = player.centerX();
        float py = player.hitboxY();
        float best = Float.MAX_VALUE;
        for (int i = 0; i < 48; i++) {
            float x = MathUtils.random(Arena.now.left + radius, Arena.now.right - radius);
            float y = MathUtils.random(Arena.now.bottom + radius, Arena.now.top - radius);
            float d = from.dst2(x, y);
            if (d >= best || Vector2.dst(x, y, px, py) < minDist || (!flying && !pointIsFree(x, y, radius))) continue;
            if (hasLineOfSight(x, y, px, py, SIGHT_THICKNESS) && (flying || walkable(from.x, from.y, x, y, radius * 0.8f))) {
                best = d;
                out.set(x, y);
            }
        }
        return best < Float.MAX_VALUE;
    }

    private void rebuildNav() {
        if (!navDirty) {
            return;
        }
        navDirty = false;
        navCols = Math.min(64, Arena.now.cols * 2);
        navRows = Math.min(40, Arena.now.rows * 2);
        for (int c = 0; c < navCols * navRows; c++) {
            navFree[c] = pointIsFree(navX(c), navY(c), NAV_CLEARANCE);
        }
    }

    private float navX(int cell) {
        return Arena.now.left + (cell % navCols + 0.5f) * NAV_CELL;
    }

    private float navY(int cell) {
        return Arena.now.bottom + (cell / navCols + 0.5f) * NAV_CELL;
    }

    private int navCell(float x, float y) {
        int col = MathUtils.clamp((int) ((x - Arena.now.left) / NAV_CELL), 0, navCols - 1);
        int row = MathUtils.clamp((int) ((y - Arena.now.bottom) / NAV_CELL), 0, navRows - 1);
        return row * navCols + col;
    }

    /**
     * For an enemy behind cover: breadth-first search over free floor from {@code from} to the nearest
     * spot that has a clear shot at the player and is at least {@code minDist} away from them.
     * Writes the next point to walk to (the furthest point along the path reachable in a straight line)
     * into {@code out}. Returns false if no such spot is reachable.
     */
    public boolean findSightPoint(Vector2 from, float radius, float minDist, Vector2 out) {
        rebuildNav();
        int start = navCell(from.x, from.y);
        Arrays.fill(navParent, -2); // -2 = not visited yet
        navParent[start] = -1;
        int head = 0;
        int tail = 0;
        navQueue[tail++] = start;
        float px = player.centerX();
        float py = player.hitboxY();
        int goal = -1;
        while (head < tail) {
            int c = navQueue[head++];
            float cx = navX(c);
            float cy = navY(c);
            if (c != start && Vector2.dst(cx, cy, px, py) >= minDist
                    && hasLineOfSight(cx, cy, px, py, SIGHT_THICKNESS)) {
                goal = c;
                break;
            }
            int col = c % navCols;
            int row = c / navCols;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int nc = col + dx;
                    int nr = row + dy;
                    if ((dx == 0 && dy == 0) || nc < 0 || nr < 0 || nc >= navCols || nr >= navRows) continue;
                    int n = nr * navCols + nc;
                    if (!navFree[n] || navParent[n] != -2) continue;
                    // no cutting corners diagonally
                    if (dx != 0 && dy != 0 && (!navFree[row * navCols + nc] || !navFree[nr * navCols + col])) continue;
                    navParent[n] = c;
                    navQueue[tail++] = n;
                }
            }
        }
        if (goal < 0) {
            return false;
        }
        // walk back from the goal; the first cell we can reach directly is the furthest shortcut
        int step = goal;
        for (int c = goal; c != start; c = navParent[c]) {
            step = c;
            if (walkable(from.x, from.y, navX(c), navY(c), radius * 0.8f)) {
                break;
            }
        }
        out.set(navX(step), navY(step));
        return true;
    }

    // ------------------------------------------------------------ spawning

    public void addPlayerBullet(Bullet b) {
        // the muzzle is ahead of the body: an enemy pressed up against the player sits between hand and
        // muzzle, so the first hit check runs from the hand - point-blank shotgun blasts land every pellet
        b.sweepFrom(player.gunHandX(), player.gunHandY());
        if (player.hasItem(Item.FROST_ROUNDS) && MathUtils.random() < ROUNDS_CHANCE) {
            b.freeze = FROST_FREEZE;
            b.color.set(FROST);
        } else if (player.hasItem(Item.EMBER_ROUNDS) && canBurn() && MathUtils.random() < ROUNDS_CHANCE) {
            b.burn = Math.max(b.burn, EMBER_BURN);
            b.color.set(EMBER);
        }
        if (b.shape == Bullet.Shape.SHARK && underwater()) { // the Chomper's sharks are at home here
            b.vel.scl(CHOMPER_SWIM);
            b.homing *= 1.6f;
        }
        playerBullets.add(b);
    }

    /** How much faster the Chomper's sharks swim under water. */
    private static final float CHOMPER_SWIM = 1.4f;

    /** Boss bullets look this much bigger and fly this much slower - easier to read, like ETG's. */
    private static final float BOSS_BULLET_SIZE = 1.3f;
    private static final float BOSS_BULLET_SPEED = 0.85f;
    /**
     * Gives the bullets a boss fired during its turn (enemy bullets from index {@code from} on) the boss
     * look: bigger and slower, with the hitbox kept as it was so gaps in patterns don't shrink.
     */
    public void styleBossBullets(int from) {
        for (int i = from; i < enemyBullets.size; i++) {
            Bullet b = enemyBullets.get(i);
            b.radius *= BOSS_BULLET_SIZE;
            b.hitScale = 0.8f / BOSS_BULLET_SIZE;
            b.vel.scl(BOSS_BULLET_SPEED);
            b.accel *= BOSS_BULLET_SPEED;
            b.maxSpeed *= BOSS_BULLET_SPEED;
        }
    }

    public Bullet spawnEnemyBullet(float x, float y, float angle, float speed) {
        Bullet b = new Bullet(false, x, y, angle, speed, 0.065f, 1f, ENEMY_BULLET);
        b.life = 10f;
        enemyBullets.add(b);
        return b;
    }

    public void spawnEnemy(Enemy e) {
        if (e.boss) {
            bossFlawless = true; // like ETG's Master Round: beat it without a scratch for an extra heart
        }
        if (!e.boss) { // bosses get tougher through their own tier instead
            e.hp *= enemyHealth;
            e.maxHp = e.hp;
        }
        Arena.now.clamp(e.pos, e.radius);
        pushOutOfSolids(e.pos, e.radius);
        pendingEnemies.add(e);
        sfx().play("spawn", 0.5f);
    }

    /** True if a pickup at (x, y) would be in the open: not under any furniture's sprite. */
    private boolean pickupSpotFree(float x, float y) {
        if (!Arena.now.contains(x, y, 0.2f)) return false;
        for (Prop p : room.props) {
            if (p.alive && circleOverlaps(p.spriteRect(game.sprites, tmpRect), x, y, 0.2f)) return false;
        }
        for (Chest c : room.chests) {
            if (circleOverlaps(c.spriteRect(game.sprites, tmpRect), x, y, 0.2f)) return false;
        }
        for (Rectangle pit : room.pits) {
            if (circleOverlaps(pit, x, y, 0.2f)) return false;
        }
        return true;
    }

    /** Drops a pickup, nudged to the nearest spot where furniture doesn't hide it. */
    public void spawnPickup(Pickup p) {
        nudgeIntoTheOpen(p);
        room.pickups.add(p);
    }

    /** Moves a pickup to the nearest spot in the room where furniture doesn't hide it. */
    private void nudgeIntoTheOpen(Pickup p) {
        Arena.now.clamp(p.pos, 0.25f);
        if (!pickupSpotFree(p.pos.x, p.pos.y)) {
            outer:
            for (float r = 0.25f; r < 3f; r += 0.25f) {
                for (int a = 0; a < 360; a += 30) {
                    float x = p.pos.x + MathUtils.cosDeg(a) * r;
                    float y = p.pos.y + MathUtils.sinDeg(a) * r;
                    if (pickupSpotFree(x, y)) {
                        p.pos.set(x, y);
                        break outer;
                    }
                }
            }
        }
    }

    /**
     * Spills {@code total} coins around (x, y) - big ones for most of a large amount - each nudged out from
     * under the furniture.
     */
    public void dropCoins(float x, float y, int total) {
        int big = Coins.bigCoins(total);
        int small = total - big * Coins.BIG;
        int n = big + small;
        for (int i = 0; i < n; i++) {
            float a = i * 137.5f + MathUtils.random(-20f, 20f); // a golden-angle spiral: a neat little pile
            float r = n == 1 ? 0f : 0.12f + 0.42f * (float) Math.sqrt((i + 0.5f) / n);
            spawnPickup(Pickup.coin(x + MathUtils.cosDeg(a) * r, y + MathUtils.sinDeg(a) * r * 0.8f,
                    i < big ? Coins.BIG : 1));
        }
    }

    // ------------------------------------------------------------ update

    public void update(float delta) {
        if (elevatorTimer > 0) { // riding the elevator: nothing happens until the doors open
            elevatorTimer -= delta;
            if (elevatorTimer <= 0) {
                sfx().play("pickup", 0.6f); // ding
            }
            return;
        }
        if (slowMo > 0) { // the boss's killing blow: everything slows right down for a moment
            slowMo -= delta;
            delta *= 0.3f;
        }
        navDirty = true; // props get flipped and destroyed
        clock += delta;
        fade = Math.max(0f, fade - delta * 4f);
        player.update(delta, this);
        updateFlufful(delta);
        updatePlayerPits();
        checkRoomTransition();
        director.update(delta, this);

        if (globeTimer > 0) { // the Snow Globe keeps everything that comes in chilled too
            globeTimer -= delta;
            for (Enemy e : enemies) {
                e.chill(globeTimer);
            }
        }
        for (Enemy e : enemies) {
            e.update(delta, this);
        }
        enemies.addAll(pendingEnemies);
        pendingEnemies.clear();
        separateEnemies();
        updateAuras(delta);
        updateGadgets(delta);
        updateMines(delta);
        updatePuddles(delta);
        updateMoons(delta);
        for (Enemy e : enemies) {
            if (!e.isSpawning() && !e.flying) {
                pushOutOfSolids(e.pos, e.radius * 0.8f);
            }
        }
        updateEnemyPits();

        for (Prop p : room.props) {
            p.update(delta);
            if (p.mimic && p.alive && !player.isDead()
                    && Vector2.dst(p.cx, p.cy, player.centerX(), player.feetY()) < MIMIC_WAKE_DIST) {
                wakeMimic(p);
            }
        }
        for (Chest c : room.chests) {
            c.update(delta);
        }
        if (room.shop != null) {
            room.shop.update(delta);
            for (Shop.Slot s : room.shop.slots) {
                s.pickup.update(delta);
            }
        }
        updatePrompt();

        updatePlayerBullets(delta);
        updateEnemyBullets(delta);
        updateGrenades(delta);
        updateBirds(delta);
        for (int i = explosions.size - 1; i >= 0; i--) {
            explosionTimes.incr(i, delta);
            if (explosionTimes.get(i) > 0.3f) {
                explosions.removeIndex(i);
                explosionTimes.removeIndex(i);
            }
        }
        for (int i = beams.size - 1; i >= 0; i--) {
            if ((beams.get(i).ttl -= delta) <= 0) {
                beams.removeIndex(i);
            }
        }
        for (int i = swings.size - 1; i >= 0; i--) {
            swingTimes.incr(i, delta);
            if (swingTimes.get(i) > SWING_TIME) {
                swings.removeIndex(i);
                swingTimes.removeIndex(i);
            }
        }
        if (flashTime >= 0 && (flashTime += delta) > FLASH_TIME) {
            flashTime = -1f;
        }
        checkContactDamage();

        for (int i = enemies.size - 1; i >= 0; i--) {
            if (!enemies.get(i).alive) {
                dying.add(enemies.removeIndex(i));
            }
        }
        for (int i = dying.size - 1; i >= 0; i--) {
            Enemy e = dying.get(i);
            if (!e.updateDeath(delta, this)) {
                dying.removeIndex(i);
                if (e.boss) {
                    bossFallSpot.set(e.pos);
                    bossFallZoom = e.viewZoom();
                    bossFallHold = BOSS_FALL_HOLD;
                }
            }
        }
        if (!bossDying()) {
            bossFallHold = Math.max(0f, bossFallHold - delta);
        }
        for (int i = room.props.size - 1; i >= 0; i--) {
            if (!room.props.get(i).alive) {
                Prop gone = room.props.removeIndex(i);
                for (int j = 0; j < room.props.size; j++) {
                    if (room.props.get(j).joinedAbove == gone) room.props.get(j).unjoin();
                }
            }
        }

        boolean wasFighting = calm == 0f;
        calm = director.inFight(this) || bossDying() ? 0f : calm + delta;
        if (wasFighting && calm > 0f && player.refitHelmet()) { // the fight's over: the helmet goes back on
            toast("HELMET BACK ON");
        }
        if (wasFighting && calm > 0f) {
            catPresent();
        }
        // like ETG: once the fight is over, every coin in the room flies to you, faster and faster
        float coinRush = player.isDead() || calm < COIN_RUSH_DELAY ? 0f : Math.min(14f, 4f + 12f * (calm - COIN_RUSH_DELAY));
        Array<Pickup> pickups = room.pickups;
        for (int i = pickups.size - 1; i >= 0; i--) {
            Pickup p = pickups.get(i);
            p.update(delta);
            float dist = Vector2.dst(p.pos.x, p.pos.y, player.centerX(), player.hitboxY());
            if (p.waitForLeave && dist > 0.6f) {
                p.waitForLeave = false;
            }
            if (p.type == Pickup.Type.COIN && coinRush > 0 && dist > 0.05f) { // (over furniture: they fly)
                float step = Math.min(dist, coinRush * delta);
                p.pos.add((player.centerX() - p.pos.x) / dist * step, (player.hitboxY() - p.pos.y) / dist * step);
            }
            if (!player.isDead() && !p.waitForLeave && dist < (p.type == Pickup.Type.COIN ? COIN_REACH : 0.32f)) {
                collect(p);
            }
            if (!p.alive) {
                pickups.removeIndex(i);
            }
        }

        particles.update(delta);
        trauma = Math.max(0f, trauma - delta * 1.8f);
        blankCalm = Math.max(0f, blankCalm - delta);
        if (blankRing >= 0) {
            blankRing += delta;
            if (blankRing > 0.5f) {
                blankRing = -1f;
            }
        }
        toastTimer -= delta;
    }

    private void separateEnemies() {
        for (int i = 0; i < enemies.size; i++) {
            Enemy a = enemies.get(i);
            if (a.isSpawning()) continue;
            for (int j = i + 1; j < enemies.size; j++) {
                Enemy b = enemies.get(j);
                if (b.isSpawning()) continue;
                float dx = b.pos.x - a.pos.x;
                float dy = b.pos.y - a.pos.y;
                float min = a.radius + b.radius;
                float d2 = dx * dx + dy * dy;
                if (d2 < min * min && d2 > 0.0001f) {
                    float d = (float) Math.sqrt(d2);
                    float push = (min - d) / 2f;
                    float wa = b.boss ? 2f : (a.boss ? 0f : 1f);
                    float wb = a.boss ? 2f : (b.boss ? 0f : 1f);
                    a.pos.sub(dx / d * push * wa, dy / d * push * wa);
                    b.pos.add(dx / d * push * wb, dy / d * push * wb);
                }
            }
        }
    }

    /** Returns true if the bullet hit a wall or a bullet-blocking prop (and damages the prop). */
    private boolean bulletHitsScenery(Bullet b) {
        if (b.pos.x < -0.2f || b.pos.x > Arena.now.worldW + 0.2f || b.pos.y < -0.2f || b.pos.y > Arena.now.worldH + 0.2f) {
            return true;
        }
        for (Rectangle w : room.walls) {
            if (w.contains(b.pos)) {
                return true;
            }
        }
        for (Prop p : room.props) {
            if (p.alive && p.blocksBullets()
                    && circleOverlaps(p.bulletRect(game.sprites, b.vel.y > 0), b.pos.x, b.pos.y, b.radius * 0.7f)) {
                if (p.mimic && b.fromPlayer) {
                    wakeMimic(p);
                    return true;
                }
                float dmg = b.fromPlayer ? b.damage * 0.3f : 1f;
                if (p.damage(dmg)) {
                    breakProp(p);
                } else {
                    particles.burst(b.pos.x, b.pos.y, 3, 0.4f, 1.2f, 0.25f, 0.03f, debrisColor(p.kind));
                }
                return true;
            }
        }
        return false;
    }

    /**
     * The player rolling across standing tables: each table they're on takes a knock, once per roll
     * ({@code alreadyHit} remembers which). Returns true while the feet are still on a table.
     */
    public boolean rollOverTables(Rectangle feet, Array<Prop> alreadyHit) {
        boolean onTable = false;
        for (Prop p : room.props) {
            if (!p.canFlip() || !p.solid.overlaps(feet)) continue;
            onTable = true;
            if (alreadyHit.contains(p, true)) continue;
            alreadyHit.add(p);
            if (p.damage(4f)) { // a standing table breaks after three rolls
                breakProp(p);
            } else {
                particles.burst(p.cx, p.cy, 6, 0.3f, 1f, 0.3f, 0.04f, WOOD);
                sfx().play("hit", 0.6f);
            }
        }
        return onTable;
    }

    private static final float MIMIC_WAKE_DIST = 0.95f;

    /** Story mode, town and city: rarely one street lamp in a combat room is a LampMimic in disguise. */
    public void hideMimicLamp() {
        Array<Prop> lamps = new Array<>();
        for (Prop p : room.props) {
            if (p.alive && p.kind == Prop.Kind.LAMP) lamps.add(p);
        }
        if (lamps.size > 0) {
            lamps.random().mimic = true;
        }
    }

    /** The disguise is off: the lamp turns into a LampMimic right where it stood. */
    private void wakeMimic(Prop p) {
        p.alive = false;
        p.mimic = false;
        LampMimic m = new LampMimic(p.cx, p.solid.y);
        Arena.now.clamp(m.pos, m.radius);
        pendingEnemies.add(m); // not spawnEnemy: no spawn circle, it was already here
        particles.burst(p.cx, p.solid.y, 14, 0.3f, 1.4f, 0.6f, 0.07f, DUST);
        particles.burst(p.cx, p.solid.y + 0.4f, 10, 0.4f, 1.8f, 0.5f, 0.05f, STONE);
        sfx().play("break", 1f);
        sfx().play("cackle", 0.6f);
        shake(0.4f);
        toast("THAT LAMP WAS A MIMIC!");
    }

    /** What flies off a prop when it's hit: stone chips, wood splinters, wisps of cloud, gold. */
    private static Color debrisColor(Prop.Kind kind) {
        switch (kind) {
            case PILLAR:
            case LAMP:
            case COLUMN:
            case ROCK:
                return STONE;
            case CORAL:
            case CLAM:
                return CORAL_BITS;
            case SEAWEED:
                return KELP_BITS;
            case DUMPSTER:
                return DUMPSTER_BITS;
            case WRECK:
                return SOOT;
            case DRUM:
                return RUST;
            case TRASH:
                return TRASH_BAG;
            case CLOUD:
                return CLOUD_PUFF;
            case URN:
                return GOLD;
            default:
                return WOOD;
        }
    }

    private void breakProp(Prop p) {
        float x = p.solid.x + p.solid.width / 2f;
        float y = p.solid.y + p.solid.height / 2f;
        if (p.kind == Prop.Kind.LAMP) { // metal bits from the post, glass from the lantern up top
            particles.burst(x, y + 0.5f, 16, 0.6f, 2.2f, 0.6f, 0.05f, STONE);
            particles.burst(x, y + 1.0f, 12, 0.6f, 2.4f, 0.5f, 0.04f, LAMP_GLASS);
        } else {
            particles.burst(x, y, 18, 0.6f, 2.2f, 0.6f, 0.05f, p.kind == Prop.Kind.HAY ? BRASS : debrisColor(p.kind));
        }
        particles.burst(x, y, 8, 0.2f, 0.8f, 0.6f, 0.07f, DUST);
        sfx().play("break", 0.9f);
        shake(0.15f);
    }

    private void updatePlayerBullets(float delta) {
        for (int i = playerBullets.size - 1; i >= 0; i--) {
            Bullet b = playerBullets.get(i);
            if (b.homing > 0) {
                steerToNearestEnemy(b, delta);
            }
            if (b.boomerang) {
                updateBoomerang(b);
            }
            if (b.vortex > 0) {
                updateVortex(b, delta);
            }
            b.update(delta);
            if (b.rocket) {
                particles.spawn(b.pos.x - b.vel.x * 0.02f, b.pos.y - b.vel.y * 0.02f, MathUtils.random(-0.2f, 0.2f),
                        MathUtils.random(-0.2f, 0.2f), 0.4f, 0.06f, SMOKE, 2f);
            }
            hitEnemiesAlongPath(b);
            if (b.alive) {
                hitShopkeeper(b);
            }
            if (b.alive) {
                hitMine(b);
            }
            if (b.alive && bulletHitsScenery(b)) {
                if (b.bounces > 0) {
                    bounce(b);
                } else {
                    b.alive = false;
                    particles.burst(b.pos.x, b.pos.y, 3, 0.3f, 1f, 0.2f, 0.03f, SMOKE);
                }
            }
            if (!b.alive) {
                if (b.blastRadius > 0 && !b.impactOnly) { // rockets blow up wherever they stop
                    explode(b.prev.x, b.prev.y, b.blastRadius, b.blastDamage);
                } else if (b.blastRadius > 0 && b.life > 0) { // a Boom-buss bomb hit something (else it fizzles)
                    smallBlast(b.prev.x, b.prev.y, b.blastRadius, b.blastDamage);
                }
                if (b.shape == Bullet.Shape.SHARK && b.life > 0) {
                    chomp(b);
                }
                if (b.splits > 0) {
                    burstIntoFeathers(b);
                }
                if (b.puddle != null) { // a flare's flames, a glob's pool of acid
                    spillPuddle(b.puddle, b.prev.x, b.prev.y, b.puddleRadius, b.puddle == Puddle.Kind.FIRE ? 3f : 5f, true);
                }
                playerBullets.removeIndex(i);
            }
        }
    }

    /** True if a bullet of radius {@code r} at (x, y) would be stopped by a wall, the room edge or cover. */
    private boolean sceneryAt(float x, float y, float r, boolean northbound) {
        if (x < 0 || x > Arena.now.worldW || y < 0 || y > Arena.now.worldH) {
            return true;
        }
        for (Rectangle w : room.walls) {
            if (w.contains(x, y)) return true;
        }
        for (Prop p : room.props) {
            if (p.alive && p.blocksBullets() && circleOverlaps(p.bulletRect(game.sprites, northbound), x, y, r)) {
                return true;
            }
        }
        return false;
    }

    /** A rubber ball hit a wall or cover: back to where it was, flipping whichever way it ran into it. */
    private void bounce(Bullet b) {
        boolean north = b.vel.y > 0;
        boolean xBlocked = sceneryAt(b.pos.x, b.prev.y, b.radius * 0.7f, north);
        boolean yBlocked = sceneryAt(b.prev.x, b.pos.y, b.radius * 0.7f, north);
        if (xBlocked == yBlocked) { // a corner (or no telling): straight back
            b.vel.scl(-1f);
        } else if (xBlocked) {
            b.vel.x = -b.vel.x;
        } else {
            b.vel.y = -b.vel.y;
        }
        b.pos.set(b.prev);
        b.bounces--;
        b.alreadyHit.clear();
        particles.burst(b.pos.x, b.pos.y, 3, 0.2f, 0.8f, 0.15f, 0.03f, b.color);
        sfx().play("pop", 0.25f);
    }

    /** True if cover (a pillar, barrel, flipped table...) stands between (x, y) and the player's hitbox. */
    private boolean coverBetween(float x, float y, float px, float py) {
        for (Prop p : room.props) {
            if (p.alive && p.blocksBullets() && segmentHits(p.bulletRect(game.sprites, py > y), x, y, px, py, 0f)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Hits enemies along the bullet's path this frame (prev -> pos), not just where it ended up: fast
     * bullets can't skip over small enemies and point-blank shots land. A normal bullet stops at the
     * first enemy on its path; a piercing one hits every enemy it passes (each only once).
     */
    private void hitEnemiesAlongPath(Bullet b) {
        while (b.alive) {
            Enemy first = null;
            float firstDist = Float.MAX_VALUE;
            for (int i = 0; i < enemies.size; i++) {
                Enemy e = enemies.get(i);
                if (!e.alive || e.isUntouchable() || b.alreadyHit.contains(e, true)) continue;
                float r = e.radius + b.radius;
                if (Intersector.distanceSegmentPoint(b.prev.x, b.prev.y, b.pos.x, b.pos.y, e.pos.x, e.pos.y) < r) {
                    float d = b.prev.dst2(e.pos);
                    if (d < firstDist) {
                        firstDist = d;
                        first = e;
                    }
                }
            }
            if (first == null) {
                return;
            }
            damageEnemy(first, b.damage, b.vel, b.knockback);
            if (b.chill > 0) {
                first.chill(b.chill);
                particles.burst(first.pos.x, first.pos.y, 6, 0.4f, 1.2f, 0.4f, 0.04f, SNOW);
            }
            if (b.freeze > 0) {
                first.freeze(b.freeze);
                particles.burst(first.pos.x, first.pos.y, 8, 0.4f, 1.4f, 0.4f, 0.04f, FROST);
            }
            if (b.burn > 0 && canBurn()) {
                first.ignite(b.burn);
                particles.burst(first.pos.x, first.pos.y, 6, 0.3f, 1.2f, 0.35f, 0.04f, EMBER);
            }
            if (b.acid > 0) {
                first.poison(b.acid);
            }
            if (b.chain > 0) {
                chainSpark(first, b.chain, b.damage * 0.8f);
            }
            // sparks on the side of the enemy the bullet came from
            tmpVec.set(b.prev).sub(first.pos).limit(first.radius).add(first.pos);
            particles.burst(tmpVec.x, tmpVec.y, 4, 0.8f, 2f, 0.18f, 0.03f, SPARK);
            if (b.shape != Bullet.Shape.SHARK) { // a shark makes its own noise (chomp)
                sfx().play("hit", 0.6f);
            }
            b.alreadyHit.add(first); // (a pillow's feathers don't hit it all over again)
            if (!b.piercing) {
                b.alive = false;
            }
        }
    }

    /** The player's damage to an enemy; what actually lands charges a popped Bubble Shield back up. */
    private void damageEnemy(Enemy e, float damage, Vector2 vel, float knockback) {
        float dealt = e.alive && !e.isSpawning() ? Math.min(damage, Math.max(0f, e.hp)) : 0f;
        e.hit(damage, vel, knockback, this);
        if (player.chargeShield(dealt)) {
            sfx().play("pop", 0.7f);
            particles.burst(player.centerX(), player.hitboxY(), 12, 0.3f, 1.2f, 0.4f, 0.04f, SHIELD);
            toast("BUBBLE SHIELD READY");
        }
    }

    /**
     * A boomerang (the banana) slows down on its way out; once nearly stopped it turns round, speeds up
     * and flies back to the player's hand - able to hit every enemy again - until the player catches it.
     */
    private void updateBoomerang(Bullet b) {
        if (!b.returning && b.vel.len() <= b.minSpeed + 0.05f) {
            b.returning = true;
            b.alreadyHit.clear(); // the way back hits again
            b.accel = 12f;
            b.maxSpeed = 8f;
        }
        if (b.returning) {
            float hx = player.gunHandX();
            float hy = player.gunHandY();
            float speed = Math.max(b.vel.len(), 0.5f);
            b.vel.set(hx - b.pos.x, hy - b.pos.y).nor().scl(speed);
            if (Vector2.dst2(b.pos.x, b.pos.y, hx, hy) < 0.3f * 0.3f) {
                b.alive = false; // caught
            }
        }
    }

    /** Homing bullets (bees) turn toward the nearest enemy within reach. */
    private void steerToNearestEnemy(Bullet b, float delta) {
        Enemy best = null;
        float bestD2 = 4f * 4f;
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (!e.alive || e.isUntouchable() || b.alreadyHit.contains(e, true)) continue;
            float d2 = e.pos.dst2(b.pos);
            if (d2 < bestD2) {
                bestD2 = d2;
                best = e;
            }
        }
        if (best == null) {
            return;
        }
        float want = MathUtils.atan2Deg360(best.pos.y - b.pos.y, best.pos.x - b.pos.x);
        float diff = ((want - b.vel.angleDeg() + 540f) % 360f) - 180f;
        float turn = b.homing * delta;
        b.vel.rotateDeg(MathUtils.clamp(diff, -turn, turn));
    }

    private void updateEnemyBullets(float delta) {
        float px = player.centerX();
        float py = player.hitboxY();
        // behind cover the hitbox can't be reached, but the head still shows above it: a bullet that touches
        // the visible body breaks on it like on the cover - only if the cover really is between that
        // bullet and the player (standing beside a post doesn't make you bulletproof from every side)
        boolean alive = !player.isDead();
        Rectangle body = bodyRect.set(px - 0.17f, player.feetY() + 0.05f, 0.34f, Player.HEIGHT - 0.08f);
        for (int i = enemyBullets.size - 1; i >= 0; i--) {
            Bullet b = enemyBullets.get(i);
            if (!b.alive) {
                continue; // cleared by a blank / hit earlier this frame, swept below
            }
            b.update(globeTimer > 0 ? delta * GLOBE_BULLET_SLOW : delta);
            if (decoy != null && Vector2.dst2(b.pos.x, b.pos.y, decoy.x, decoy.centerY())
                    < (Decoy.RADIUS + b.radius) * (Decoy.RADIUS + b.radius)) { // it takes the bullet for you
                b.alive = false;
                decoy.hit();
                particles.burst(b.pos.x, b.pos.y, 3, 0.3f, 0.8f, 0.25f, 0.03f, DUST);
                continue;
            }
            if (bulletHitsScenery(b) || (alive && body.contains(b.pos) && coverBetween(b.pos.x, b.pos.y, px, py))) {
                b.alive = false;
                particles.burst(b.pos.x, b.pos.y, 2, 0.2f, 0.6f, 0.2f, 0.03f, b.color);
            } else if (!player.isDead() && !player.isInvulnerable()) {
                float r = b.radius * b.hitScale + Player.HITBOX_RADIUS;
                if (Vector2.dst2(b.pos.x, b.pos.y, px, py) < r * r) {
                    b.alive = false;
                    damagePlayer();
                }
            }
            if (!b.alive && b.puddle != null) { // an acid glob splats where it lands (or on whatever it hit)
                spillPuddle(b.puddle, b.prev.x, b.prev.y, b.puddleRadius, 4.5f, false);
                b.puddle = null;
            }
        }
        removeDeadEnemyBullets();
    }

    private void removeDeadEnemyBullets() {
        for (int i = enemyBullets.size - 1; i >= 0; i--) {
            if (!enemyBullets.get(i).alive) {
                enemyBullets.removeIndex(i);
            }
        }
    }

    private void checkContactDamage() {
        if (player.isDead()) {
            return;
        }
        if (player.isRolling()) { // rolling over rats squishes them
            float fx = player.feetX();
            float fy = player.feetY() + 0.1f;
            for (Enemy e : enemies) {
                if (!e.squishable || e.isSpawning() || !e.alive) continue;
                float r = e.radius + 0.16f;
                if (Vector2.dst2(e.pos.x, e.pos.y, fx, fy) < r * r) {
                    e.hit(e.hp + 1f, tmpVec.set(e.pos).sub(fx, fy), 0f, this);
                    particles.burst(e.pos.x, e.pos.y, 10, 0.4f, 1.6f, 0.35f, 0.05f, BLOOD);
                    sfx().play("hit", 0.8f);
                }
            }
        }
        if (player.isInvulnerable()) {
            return;
        }
        for (Enemy e : enemies) {
            if (e.isUntouchable() || !e.alive || e.isFrozen() || !e.hurtsToTouch) continue; // (a block of ice doesn't bite)
            float r = e.radius * 0.8f + Player.HITBOX_RADIUS;
            if (Vector2.dst2(e.pos.x, e.pos.y, player.centerX(), player.hitboxY()) < r * r) {
                damagePlayer();
                return;
            }
        }
    }

    /** Damage from something that isn't a bullet or a body (e.g. a laser beam). Respects i-frames. */
    public void hurtPlayer() {
        if (!player.isDead() && !player.isInvulnerable()) {
            damagePlayer();
        }
    }

    private void damagePlayer() {
        if (!player.isDead() && !player.isInvulnerable() && player.consumeHelmet()) { // Bike Helmet: knocked off instead
            sfx().play("hit", 1f);
            shake(0.4f);
            particles.burst(player.centerX(), player.feetY() + Player.HEIGHT, 10, 0.4f, 1.6f, 0.4f, 0.05f, Color.RED);
            clearEnemyBullets(player.centerX(), player.hitboxY(), 1.1f);
            toast("HELMET KNOCKED OFF!");
            return;
        }
        if (!player.isDead() && !player.isInvulnerable() && player.consumeShield()) { // Bubble Shield
            sfx().play("pop", 1f);
            particles.burst(player.centerX(), player.hitboxY(), 16, 0.4f, 1.8f, 0.4f, 0.05f, SHIELD);
            clearEnemyBullets(player.centerX(), player.hitboxY(), 1.1f);
            toast("BUBBLE POPPED!");
            return;
        }
        if (!player.hurt(this)) {
            return;
        }
        bossFlawless = false;
        shake(0.7f);
        particles.burst(player.centerX(), player.hitboxY(), 14, 0.8f, 2.5f, 0.4f, 0.05f, BLOOD);
        if (player.takeLambSave()) { // the Guardian Lamb: a second chance, with a burst of golden fluff
            sfx().play("blank", 1f);
            sfx().play("baa", 0.9f);
            clearEnemyBullets(0, 0, -1f);
            blankCalm = BLANK_CALM;
            blankRing = 0f;
            blankCenter.set(player.centerX(), player.hitboxY());
            particles.burst(player.centerX(), player.hitboxY(), 30, 0.5f, 2.2f, 0.9f, 0.06f, LAMB_GOLD);
            particles.burst(player.centerX(), player.hitboxY(), 20, 0.3f, 1.4f, 1.2f, 0.07f, Color.WHITE);
            toast("THE GUARDIAN LAMB SAVED YOU!");
            return;
        }
        if (player.isDead()) {
            sfx().play("death", 1f);
            clearEnemyBullets(0, 0, -1f);
        } else {
            sfx().play("hurt", 1f);
            // small free "blank" around the player so a hit doesn't chain into another one
            clearEnemyBullets(player.centerX(), player.hitboxY(), 1.1f);
            if (player.hasItem(Item.PRICKLY_SWEATER)) {
                sprayQuills();
            }
        }
    }

    /** Marks enemy bullets within {@code radius} (negative = all) as dead. Safe to call mid-iteration. */
    public void clearEnemyBullets(float x, float y, float radius) {
        for (Bullet b : enemyBullets) {
            if (b.alive && (radius < 0 || b.pos.dst2(x, y) < radius * radius)) {
                particles.spawn(b.pos.x, b.pos.y, 0, 0.3f, 0.3f, b.radius * 1.4f, SMOKE, 2f);
                b.alive = false;
            }
        }
    }

    public void useBlank() {
        if (player.isDead() || blankRing >= 0 || !player.consumeBlank()) {
            return;
        }
        float x = player.centerX();
        float y = player.hitboxY();
        clearEnemyBullets(x, y, -1f);
        removeDeadEnemyBullets();
        for (GasCloud g : gas) {
            g.blowAway(x, y);
        }
        for (Enemy e : enemies) {
            if (e.boss || e.isSpawning()) continue;
            Vector2 push = new Vector2(e.pos).sub(x, y).nor().scl(2.5f);
            e.hit(0, push, 1.2f, this);
        }
        blankRing = 0f;
        blankCalm = BLANK_CALM;
        blankCenter.set(x, y);
        shake(0.5f);
        sfx().play("blank", 1f);
    }

    // ------------------------------------------------------------ interacting: chests, exit, tables

    /** Closest unopened chest within reach of the player's feet, or null. */
    private Chest chestInReach() {
        float fx = player.feetX();
        float fy = player.feetY() + 0.07f;
        Chest best = null;
        float bestDist = CHEST_REACH;
        for (Chest c : room.chests) {
            if (c.opened) continue;
            float cx = MathUtils.clamp(fx, c.solid.x, c.solid.x + c.solid.width);
            float cy = MathUtils.clamp(fy, c.solid.y, c.solid.y + c.solid.height);
            float d = Vector2.dst(fx, fy, cx, cy);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        return best;
    }

    private boolean onExit() {
        return room.exit && Math.abs(player.feetX() - exitX()) < exitKind().w / 2f + 0.1f
                && Math.abs(player.feetY() + 0.07f - exitY()) < exitKind().h / 2f + 0.1f;
    }

    private void updatePrompt() {
        prompt = null;
        Shop.Slot wasAt = lookedAt;
        lookedAt = null;
        if (player.isDead()) {
            return;
        }
        Chest c = chestInReach();
        if (c != null) {
            if (!c.locked) prompt = "OPEN";
            else prompt = player.getKeys() > 0 ? "OPEN  (-1 KEY)" : "LOCKED - NEEDS A KEY";
            Rectangle r = c.spriteRect(game.sprites, tmpRect);
            promptPos.set(c.cx, r.y + r.height + 0.22f);
        } else if (standInReach() != null) {
            Shop.Slot s = standInReach();
            if (s.special && wasAt != s) { // stepping up to the flippers: Baa-llo drops a hint
                room.shop.onFlippersLooked();
            }
            lookedAt = s;
            String title = s.title().toUpperCase();
            prompt = player.getCoins() >= s.price ? "BUY " + title + " - " + s.price + " COINS"
                    : title + " - NEED " + s.price + " COINS";
            promptPos.set(s.stand.cx, s.stand.solid.y - 0.42f);
        } else if (atTheBell()) {
            prompt = "RING THE BELL - NEXT WAVE";
            promptPos.set(room.shop.counter.cx, room.shop.counter.solid.y - 0.3f);
        } else if (onExit()) {
            prompt = exitKind().prompt;
            promptPos.set(exitX(), exitY() + exitKind().h / 2f + 0.3f);
        }
    }

    /** The shop stand (with something still on it) closest to the player's feet, within reach - or null. */
    private Shop.Slot standInReach() {
        if (room.shop == null) {
            return null;
        }
        float fx = player.feetX();
        float fy = player.feetY() + 0.07f;
        Shop.Slot best = null;
        float bestDist = CHEST_REACH;
        for (Shop.Slot s : room.shop.slots) {
            if (s.sold) continue;
            Rectangle r = s.stand.solid;
            float d = Vector2.dst(fx, fy, MathUtils.clamp(fx, r.x, r.x + r.width), MathUtils.clamp(fy, r.y, r.y + r.height));
            if (d < bestDist) {
                bestDist = d;
                best = s;
            }
        }
        return best;
    }

    /** Endless: standing at the travelling stall's counter, where the bell is. */
    private boolean atTheBell() {
        if (room.shop == null || !room.shop.travelling) {
            return false;
        }
        Rectangle r = room.shop.counter.solid;
        float fx = player.feetX();
        float fy = player.feetY() + 0.07f;
        return Vector2.dst(fx, fy, MathUtils.clamp(fx, r.x, r.x + r.width), MathUtils.clamp(fy, r.y, r.y + r.height)) < 0.5f;
    }

    /** E key / A button: open a chest, go down the exit, or flip a table - whichever is in reach. */
    public void interact() {
        Chest c = chestInReach();
        Shop.Slot ware = c == null ? standInReach() : null;
        if (c != null) {
            openChest(c);
        } else if (ware != null) {
            buy(ware);
        } else if (atTheBell()) {
            bellRung = true;
            sfx().play("kaching", 0.6f);
        } else if (onExit() && room.stage.isSea()) { // the waterspout: back up to the clouds
            surface();
        } else if (onExit()) {
            exitTaken = true;
            sfx().play("door", 1f);
        } else {
            tryFlipTable();
        }
    }

    private void openChest(Chest c) {
        c.bump();
        if (c.locked && !player.consumeKey()) {
            sfx().play("empty", 1f);
            toast("YOU NEED A KEY");
            return;
        }
        c.opened = true;
        sfx().play("chest_open", 1f);
        shake(0.12f);
        particles.burst(c.cx, c.cy + 0.15f, 14, 0.5f, 1.6f, 0.5f, 0.04f, Color.GOLD);
        // loot pops out in front of the chest
        float y = c.solid.y - 0.35f;
        if (c.gun != null) {
            spawnPickup(new Pickup(Pickup.Type.GUN, c.cx, y, c.gun));
            return;
        }
        if (c.item != null) {
            spawnPickup(new Pickup(c.item, c.cx, y));
            return;
        }
        dropCoins(c.cx, y - 0.25f, Coins.forChest(c.tier, MathUtils.random(0, 2)));
        int n = c.tier == Tier.D ? MathUtils.random(1, 2) : c.tier == Tier.A ? 3 : 2;
        // sometimes one of the items is a heart container - better chests, better odds
        float containerChance = c.tier == Tier.A ? 0.25f : c.tier == Tier.B ? 0.15f : c.tier == Tier.C ? 0.06f : 0.03f;
        boolean container = MathUtils.random() < containerChance;
        for (int i = 0; i < n; i++) {
            Pickup.Type type = container && i == 0 ? Pickup.Type.HEART_CONTAINER : randomLoot();
            spawnPickup(new Pickup(type, c.cx + (i - (n - 1) / 2f) * 0.38f, y));
        }
    }

    /** A consumable for chests without a gun: something useful right now if possible. */
    private Pickup.Type randomLoot() {
        Array<Pickup.Type> options = new Array<>();
        options.add(Pickup.Type.BLANK);
        if (player.getHealth() < player.getMaxHealth()) {
            options.add(Pickup.Type.HEART);
            options.add(Pickup.Type.HEART);
        }
        if (hasLimitedGunNeedingAmmo()) {
            options.add(Pickup.Type.AMMO);
            options.add(Pickup.Type.AMMO);
        }
        return MathUtils.random() < 0.05f ? Pickup.Type.KEY : options.random();
    }

    /** A free spot for a chest: away from doors, furniture and the player. */
    public Vector2 findChestSpot() {
        for (int i = 0; i < 80; i++) {
            float x = MathUtils.random(Arena.now.left + 0.6f, Arena.now.right - 0.6f);
            float y = MathUtils.random(Arena.now.bottom + 0.6f, Arena.now.top - 0.8f);
            navRect.set(x - 0.45f, y - 0.3f, 0.9f, 0.6f);
            if (pointIsFree(x, y, 0.5f) && !room.blocksADoor(navRect)
                    && Vector2.dst(x, y, player.feetX(), player.feetY()) > 0.9f) {
                return spawnPoint.set(x, y);
            }
        }
        return spawnPoint.set(Arena.now.centerX, Arena.now.centerY);
    }

    /** (x, y) if a chest fits there, otherwise a free spot elsewhere in the room. */
    public Vector2 chestSpotNear(float x, float y) {
        navRect.set(x - 0.45f, y - 0.3f, 0.9f, 0.6f);
        return pointIsFree(x, y, 0.5f) && !room.blocksADoor(navRect) ? spawnPoint.set(x, y) : findChestSpot();
    }

    /** Puts a chest into the current room, stepping the player out of the way if it lands on them. */
    public void placeChest(Chest c) {
        room.chests.add(c);
        if (c.solid.overlaps(player.feetRect(tmpRect))) {
            player.setFeet(player.feetX(), c.solid.y - 0.16f);
        }
        particles.burst(c.cx, c.cy, 12, 0.3f, 1.2f, 0.5f, 0.05f, DUST);
    }

    /** Opens the hatch to the next floor (boss room, after the boss). */
    public void openExit() {
        // a little above the middle of the room - or the nearest clear spot if something stands there
        // (like the Pigeon King's statue)
        Arena a = Arena.now;
        float[][] spots = {{0, 0.75f}, {0, 1.7f}, {0, -1.6f}, {-2.2f, 0.75f}, {2.2f, 0.75f}, {-2.2f, -1f}, {2.2f, -1f}};
        room.exitX = a.centerX;
        room.exitY = a.centerY + 0.75f;
        for (float[] s : spots) {
            float x = a.centerX + s[0];
            float y = a.centerY + s[1];
            navRect.set(x - exitKind().w / 2f - 0.2f, y - exitKind().h / 2f - 0.2f, exitKind().w + 0.4f, exitKind().h + 0.4f);
            if (a.contains(x, y, 0.6f) && findSolid(navRect) == null) {
                room.exitX = x;
                room.exitY = y;
                break;
            }
        }
        room.exit = true;
        particles.burst(exitX(), exitY(), 20, 0.3f, 1.2f, 0.6f, 0.06f, DUST);
        sfx().play("door", 1f);
    }

    /** E key / A button: flips the closest standing table next to the player, away from the player. */
    public boolean tryFlipTable() {
        float fx = player.feetX();
        float fy = player.feetY() + 0.08f;
        Prop best = null;
        float bestDist = 0.5f;
        for (Prop p : room.props) {
            if (!p.canFlip()) continue;
            float cx = MathUtils.clamp(fx, p.solid.x, p.solid.x + p.solid.width);
            float cy = MathUtils.clamp(fy, p.solid.y, p.solid.y + p.solid.height);
            float d = Vector2.dst(fx, fy, cx, cy);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        if (best == null) {
            return false;
        }
        float dx = best.cx - fx;
        float dy = best.cy - fy;
        int dir;
        if (Math.abs(dx) > Math.abs(dy) * 1.6f) {
            dir = dx > 0 ? Room.E : Room.W;
        } else {
            dir = dy > 0 ? Room.N : Room.S;
        }
        best.flip(dir);
        // don't leave the player stuck inside the new footprint
        if (best.solid.overlaps(player.feetRect(tmpRect))) {
            Vector2 away = tmpVec.set(Room.dx(dir), Room.dy(dir)).scl(-0.25f);
            player.nudge(away.x, away.y, this);
        }
        particles.burst(best.cx, best.cy, 10, 0.4f, 1.4f, 0.4f, 0.05f, DUST);
        sfx().play("flip", 1f);
        shake(0.15f);
        return true;
    }

    public void onEnemyKilled(Enemy e) {
        kills++;
        score += e.score;
        int n = e.boss ? 30 : 16; // (a boss saves its big blast for the end of its death animation)
        particles.burst(e.pos.x, e.pos.y, n, 0.5f, e.boss ? 3f : 2.5f, 0.6f, e.boss ? 0.08f : 0.05f, BRASS);
        particles.burst(e.pos.x, e.pos.y, n / 2, 0.3f, 1.2f, 0.7f, 0.07f, SMOKE);
        sfx().play("enemy_die", e.boss ? 1f : 0.7f);
        shake(e.boss ? 0.6f : 0.15f);
        if (e.boss) {
            slowMo = 0.8f;
        }

        if (fluffulHeld() && player.currentWeapon().addKill()) { // the kill counts for the Flufful in hand
            flufful.say(FluffulVoice.Event.LEVEL_UP); // (kept for after the fight)
            toast(player.currentWeapon().displayName().toUpperCase() + " - FLUFFIER!");
            sfx().play("heal", 0.8f);
        }
        if (e.boss) {
            fluffulSays(FluffulVoice.Event.BOSS_DOWN);
        }
        if (e.boss && bossFlawless) { // not a single hit taken during the whole fight
            spawnPickup(new Pickup(Pickup.Type.HEART_CONTAINER, e.pos.x, e.pos.y));
            toast("NOT A SCRATCH! +1 MAX HEART");
            bossFlawless = false;
        }
        // the Fluffy Shark beaten (the shark that sits on the Flufful): the talking gun is unlocked for good
        if (e instanceof FluffyShark && Unlocks.unlockFlufful(game.prefs)) {
            toast("THE SHARK IS TAMED! FLUFFUL UNLOCKED!");
            flufful.say(FluffulVoice.Event.UNLOCK);
        }
        if (e.boss) { // like ETG: the boss's bullets and minions die with it
            clearEnemyBullets(0, 0, -1f);
            // index loop: we're usually called from inside a for-each over enemies, and libGDX's
            // Array iterator can't be nested
            for (int i = 0; i < enemies.size; i++) {
                Enemy other = enemies.get(i);
                if (other != e && other.alive) {
                    other.alive = false; // swept after the update; no score for these
                    particles.burst(other.pos.x, other.pos.y, 12, 0.4f, 2f, 0.5f, 0.05f, SMOKE);
                }
            }
            pendingEnemies.clear();
        }

        if (!e.boss && activeBoss() == null) { // (no coins for a boss's minions: no farming them)
            float luck = player.hasItem(Item.LUCKY_CLOVER) ? 1.25f : 1f;
            dropCoins(e.pos.x, e.pos.y, Coins.forEnemy(Math.round(e.score * luck), MathUtils.random()));
        }
        if (!e.boss) {
            // the Lucky Clover makes drops half as likely again
            float roll = MathUtils.random() / (player.hasItem(Item.LUCKY_CLOVER) ? 1.5f : 1f);
            // enemies rarely drop anything, and only what you need (keys come from rooms and chests);
            // ammo as rarely as half hearts, and at most one box per room
            if (roll < 0.03f && player.getHealth() < player.getMaxHealth()) {
                spawnPickup(new Pickup(Pickup.Type.HALF_HEART, e.pos.x, e.pos.y));
            } else if (roll < 0.06f && takeAmmoDrop()) {
                spawnPickup(new Pickup(Pickup.Type.AMMO, e.pos.x, e.pos.y));
            }
        }
    }

    /**
     * True (and remembered) if an ammo box may drop now: a gun needs ammo and none has dropped in this
     * room yet (endless: this wave) - so a room never showers you with ammo.
     */
    public boolean takeAmmoDrop() {
        if (ammoDropped || !hasLimitedGunNeedingAmmo()) {
            return false;
        }
        ammoDropped = true;
        return true;
    }

    /** Endless mode: a new wave may drop an ammo box again. */
    public void allowAmmoDrop() {
        ammoDropped = false;
    }

    public boolean hasLimitedGunNeedingAmmo() {
        for (Weapon w : player.getWeapons()) {
            if (!w.isFullAmmo()) return true;
        }
        return false;
    }

    private void collect(Pickup p) {
        switch (p.type) {
            case COIN:
                player.addCoins(p.value);
                p.alive = false;
                sfx().play("coin", p.value > 1 ? 0.9f : 0.6f);
                particles.burst(p.pos.x, p.pos.y, p.value > 1 ? 8 : 4, 0.3f, 1.2f, 0.3f, 0.03f, GOLD);
                return;
            case HEART:
                if (!player.heal(2)) {
                    return; // leave it on the floor for later
                }
                toast("+1 HEART");
                break;
            case HEART_CONTAINER:
                if (!player.addHeartContainer()) {
                    return; // at the cap and already full: leave it for later
                }
                toast(player.getMaxHealth() >= Player.MAX_HEALTH_CAP ? "MAX HEARTS!"
                        : player.getMaxHealth() > Player.RED_HEARTS ? "+1 GOLDEN HEART!" : "+1 MAX HEART!");
                sfx().play("wave_clear", 0.7f);
                break;
            case HALF_HEART:
                if (!player.heal(1)) {
                    return;
                }
                toast("+1/2 HEART");
                break;
            case AMMO:
                if (!hasLimitedGunNeedingAmmo()) {
                    return;
                }
                for (Weapon w : player.getWeapons()) {
                    w.addAmmo(0.6f);
                }
                toast("AMMO REFILLED");
                break;
            case BLANK:
                player.addBlank();
                toast("+1 BLANK");
                break;
            case KEY:
                player.addKey();
                toast("+1 KEY");
                break;
            case GUN:
                Weapon.Upgrade up = player.addWeapon(p.weapon);
                if (up == null) {
                    toast("GOT " + p.weapon.getName().toUpperCase() + "!");
                } else { // a second copy: the one in hand gets better instead
                    toast(player.currentWeapon().displayName().toUpperCase() + " - " + up.text + "!");
                }
                break;
            case ITEM:
                if (p.item.active) {
                    Item old = player.takeActive(p.item);
                    if (old != null) { // one active item at a time: the old one stays here
                        Pickup dropped = new Pickup(old, p.pos.x, p.pos.y);
                        dropped.waitForLeave = true;
                        room.pickups.add(dropped);
                    }
                } else {
                    player.addPassive(p.item);
                }
                toast(p.item.title.toUpperCase() + " - " + p.item.description.toUpperCase());
                break;
        }
        p.alive = false;
        sfx().play("pickup", 1f);
        particles.burst(p.pos.x, p.pos.y, 10, 0.5f, 1.5f, 0.4f, 0.04f, Color.GOLD);
    }

    // ------------------------------------------------------------ the Fluff Field and the Static Fuzz

    /**
     * The hero's auras. Fluff Field (C): regular enemies that come within {@link #FIELD_RADIUS} are pushed
     * back out - melee enemies and dynamite can't reach you - but bullets aren't stopped and bosses don't
     * care. Static Fuzz (B): enemies within {@link #FUZZ_RADIUS} (bosses too) get a little zap now and then.
     */
    private void updateAuras(float delta) {
        fieldGlow = Math.max(0f, fieldGlow - delta * 3f);
        bubbleCooldown = Math.max(0f, bubbleCooldown - delta);
        if (enemies.size == 0) {
            zapTimers.clear(); // (endless never changes rooms: forget the dead)
        }
        for (ObjectFloatMap.Entry<Enemy> z : zapTimers) {
            zapTimers.put(z.key, z.value - delta);
        }
        boolean field = player.hasItem(Item.FLUFF_FIELD);
        boolean fuzz = player.hasItem(Item.STATIC_FUZZ);
        if (!field && !fuzz || player.isDead()) {
            return;
        }
        float px = player.centerX();
        float py = player.hitboxY();
        if (fuzz && MathUtils.random() < 5f * delta) { // it crackles now and then
            float a = MathUtils.random(360f);
            particles.burst(px + MathUtils.cosDeg(a) * 0.3f, py + MathUtils.sinDeg(a) * 0.3f, 1, 0.2f, 0.6f, 0.15f, 0.02f, ZAP_BLUE);
        }
        for (int i = 0; i < enemies.size; i++) { // (index loop: damage below may end up nesting iterators)
            Enemy e = enemies.get(i);
            if (!e.alive || e.isSpawning() || e.isUntouchable()) continue;
            float dx = e.pos.x - px;
            float dy = e.pos.y - py;
            float d = Math.max(0.01f, (float) Math.sqrt(dx * dx + dy * dy));
            if (field && !e.boss && d < FIELD_RADIUS + e.radius) {
                e.shove(dx / d, dy / d, FIELD_PUSH);
                if (fieldGlow < 0.5f) {
                    sfx().play("pop", 0.25f);
                }
                fieldGlow = 1f;
            }
            if (fuzz && d < FUZZ_RADIUS + e.radius && zapTimers.get(e, 0f) <= 0f) {
                zapTimers.put(e, ZAP_COOLDOWN);
                tmpVec.set(dx, dy);
                damageEnemy(e, FUZZ_DAMAGE * player.damageScale(), tmpVec, 0f);
                Beam b = new Beam(); // a crackle of static from the hero to it
                b.x = px;
                b.y = py;
                b.angle = MathUtils.atan2Deg360(dy, dx);
                b.length = Math.max(0f, d - e.radius * 0.5f);
                b.width = 0.012f;
                b.ttl = 0.08f;
                b.color.set(ZAP_BLUE);
                beams.add(b);
                particles.burst(e.pos.x, e.pos.y, 6, 0.6f, 1.8f, 0.2f, 0.03f, ZAP_BLUE);
                sfx().play("zap", 0.45f);
            }
        }
    }

    /**
     * The player's dodge roll just ended. Flippers (the city shop's): a few small bubbles float off after the
     * nearest enemies within {@link #BUBBLE_REACH} - one each, round again if there are fewer - then a cooldown.
     */
    public void onRollEnd() {
        if (!player.hasItem(Item.FLIPPERS) || bubbleCooldown > 0 || player.isDead()) {
            return;
        }
        float px = player.centerX();
        float py = player.hitboxY();
        Array<Enemy> near = new Array<>();
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.alive && !e.isSpawning() && !e.isUntouchable() && e.pos.dst(px, py) < BUBBLE_REACH + e.radius) {
                near.add(e);
            }
        }
        if (near.size == 0) {
            return;
        }
        near.sort((a, b) -> Float.compare(a.pos.dst2(px, py), b.pos.dst2(px, py)));
        for (int i = 0; i < ROLL_BUBBLES; i++) {
            Enemy e = near.get(i % near.size);
            float angle = MathUtils.atan2Deg360(e.pos.y - py, e.pos.x - px) + MathUtils.random(-25f, 25f);
            Bullet b = new Bullet(true, px, py, angle, MathUtils.random(4f, 5f), 0.07f,
                    BUBBLE_DAMAGE * player.damageScale(), BUBBLE_BLUE);
            b.bubble = true;
            b.homing = 300f;
            b.knockback = 0.3f;
            b.life = 1.1f;
            playerBullets.add(b);
        }
        bubbleCooldown = BUBBLE_COOLDOWN;
        sfx().play("pop", 0.5f);
    }

    // ------------------------------------------------------------ fire and acid

    /** Nothing burns under water (the secret sea, a flooded endless arena). */
    public boolean canBurn() {
        return !underwater();
    }

    /** The secret sea, or a flooded endless arena. */
    public boolean underwater() {
        return room.stage.isSea();
    }

    /**
     * Spills fire or acid on the floor at (x, y). A fire under water only hisses out in a puff of steam.
     * {@code friendly}: the player's own (it only hurts enemies).
     */
    public void spillPuddle(Puddle.Kind kind, float x, float y, float radius, float life, boolean friendly) {
        if (kind == Puddle.Kind.FIRE && !canBurn()) {
            particles.burst(x, y, 6, 0.2f, 0.8f, 0.6f, 0.06f, SNOW);
            return;
        }
        if (puddles.size >= MAX_PUDDLES) { // the oldest spill goes (never the room's own old acid)
            for (int i = 0; i < puddles.size; i++) {
                if (!puddles.get(i).old) {
                    puddles.removeIndex(i);
                    break;
                }
            }
        }
        puddles.add(new Puddle(kind, x, y, radius, life, friendly));
        if (kind == Puddle.Kind.ACID) {
            particles.burst(x, y, 6, 0.2f, 1.2f, 0.3f, 0.04f, ACID_GREEN);
            sfx().play("pop", 0.35f);
        }
    }

    /**
     * Lets out a cloud of smoke or stink at (x, y), drifting off at (vx, vy) and slowing down. When the room
     * is full of it, the oldest puff goes first.
     */
    public GasCloud releaseGas(GasCloud.Kind kind, float x, float y, float vx, float vy, float radius, float life) {
        if (gas.size >= MAX_GAS) {
            gas.removeIndex(0);
        }
        GasCloud g = new GasCloud(kind, x, y, vx, vy, radius, life);
        gas.add(g);
        return g;
    }

    public Array<GasCloud> getGas() {
        return gas;
    }

    /** Moves the clouds in the air and notes which kind the hero's hitbox is in (inSmoke, inStink). */
    private void updateGas(float delta) {
        inSmoke = false;
        inStink = false;
        for (int i = gas.size - 1; i >= 0; i--) {
            GasCloud g = gas.get(i);
            g.update(delta);
            if (!g.alive()) {
                gas.removeIndex(i);
            } else if (g.touches(player.centerX(), player.hitboxY(), Player.HITBOX_RADIUS)) {
                if (g.kind == GasCloud.Kind.SMOKE) inSmoke = true;
                else inStink = true;
            }
        }
    }

    public Array<Puddle> getPuddles() {
        return puddles;
    }

    /**
     * Fire and acid on the floor, and what they do: the hero catches fire stepping into flames and acid
     * bites after a moment (a dodge roll passes over both - see Player.updateHazards); walking enemies catch
     * fire too, and acid eats at them. Burning and acid then wear enemies down a few times a second.
     */
    private void updatePuddles(float delta) {
        boolean inFire = false;
        boolean inAcid = false;
        float fx = player.feetX();
        float fy = player.feetY() + 0.05f;
        for (int i = puddles.size - 1; i >= 0; i--) {
            Puddle pd = puddles.get(i);
            pd.update(delta);
            if (!pd.alive()) {
                puddles.removeIndex(i);
                continue;
            }
            if (!pd.friendly && pd.covers(fx, fy)) {
                if (pd.kind == Puddle.Kind.FIRE) inFire = true;
                else inAcid = true;
            }
            for (int j = 0; j < enemies.size; j++) {
                Enemy e = enemies.get(j);
                if (!e.alive || e.flying || e.isUntouchable() || !pd.covers(e.pos.x, e.pos.y - e.radius * 0.5f)) continue;
                if (pd.kind == Puddle.Kind.FIRE) e.ignite(1.5f);
                else e.poison(1.5f);
            }
        }
        updateGas(delta);
        if (!canBurn()) {
            player.douse();
            if (inSmoke) { // nothing can catch fire under water: hot smoke just hurts
                hurtPlayer();
            }
        }
        // smoke is hot: it sets the hero alight like flames (the flame warning); stink fills the acid drop
        if (player.updateHazards(delta, inFire || (inSmoke && canBurn()), inAcid, inStink)) {
            hurtPlayer();
        }
        if (player.takeRolledOutFire()) {
            particles.burst(player.centerX(), player.hitboxY(), 8, 0.3f, 1f, 0.5f, 0.05f, SMOKE);
        }
        if (player.isBurning() && MathUtils.random() < 12f * delta) {
            particles.spawn(player.centerX() + MathUtils.random(-0.15f, 0.15f), player.hitboxY() + 0.2f, 0f, 0.6f, 0.5f,
                    0.04f, SMOKE, 2f);
        }
        boolean wet = !canBurn();
        for (int i = 0; i < enemies.size; i++) { // (index loop: a kill may spawn things mid-loop)
            Enemy e = enemies.get(i);
            if (wet) e.douse();
            if (!e.alive || e.isSpawning()) continue;
            float dmg = e.takeAilmentDamage(delta);
            if (dmg > 0) {
                damageEnemy(e, dmg * player.damageScale(), tmpVec.setZero(), 0f);
            }
        }
    }

    /** Pom-Pom Moons: where moon {@code i} is now. */
    private float moonX(int i) {
        return player.centerX() + MathUtils.cosDeg(moonAngle + i * 360f / MOONS) * MOON_ORBIT;
    }

    private float moonY(int i) {
        return player.hitboxY() + MathUtils.sinDeg(moonAngle + i * 360f / MOONS) * MOON_ORBIT * 0.8f;
    }

    /** The pom-poms circle the hero, soak up enemy bullets that touch them and bump enemies they pass through. */
    private void updateMoons(float delta) {
        if (!player.hasItem(Item.POM_POM_MOONS) || player.isDead()) {
            return;
        }
        moonAngle = (moonAngle + MOON_SPEED * delta) % 360f;
        for (int i = 0; i < MOONS; i++) {
            moonCooldown[i] = Math.max(0f, moonCooldown[i] - delta);
            float mx = moonX(i);
            float my = moonY(i);
            for (int j = 0; j < enemyBullets.size; j++) {
                Bullet b = enemyBullets.get(j);
                float r = MOON_RADIUS + b.radius * b.hitScale;
                if (b.alive && Vector2.dst2(b.pos.x, b.pos.y, mx, my) < r * r) {
                    b.alive = false; // (swept with the other dead bullets)
                    particles.burst(b.pos.x, b.pos.y, 3, 0.2f, 0.8f, 0.2f, 0.03f, MOON_COLORS[i]);
                }
            }
            if (moonCooldown[i] > 0) continue;
            for (int j = 0; j < enemies.size; j++) {
                Enemy e = enemies.get(j);
                float r = MOON_RADIUS + e.radius;
                if (e.alive && !e.isUntouchable() && Vector2.dst2(e.pos.x, e.pos.y, mx, my) < r * r) {
                    damageEnemy(e, MOON_DAMAGE * player.damageScale(), tmpVec.set(e.pos.x - mx, e.pos.y - my), 0.3f);
                    moonCooldown[i] = MOON_HIT_COOLDOWN;
                    sfx().play("pop", 0.3f);
                    break;
                }
            }
        }
    }

    private void drawMoons(SpriteBatch batch) {
        if (!player.hasItem(Item.POM_POM_MOONS) || player.isDead()) {
            return;
        }
        for (int i = 0; i < MOONS; i++) {
            float mx = moonX(i);
            float my = moonY(i);
            game.draw.circle(batch, mx, my, MOON_RADIUS * 1.25f, Draw.OUTLINE_COLOR, 0.8f);
            game.draw.circle(batch, mx, my, MOON_RADIUS, MOON_COLORS[i]);
            game.draw.circle(batch, mx - MOON_RADIUS * 0.3f, my + MOON_RADIUS * 0.3f, MOON_RADIUS * 0.35f, Color.WHITE, 0.8f);
        }
    }

    // ------------------------------------------------------------ the sea: mines, and the shark's stun

    /**
     * Sea mines: the player walking up (or a walking enemy) lights one; it blinks and blows up, hurting
     * everyone in reach - and lighting any other mine the blast touches.
     */
    private void updateMines(float delta) {
        if (room.mines.isEmpty()) {
            return;
        }
        for (int i = 0; i < room.mines.size; i++) {
            SeaMine m = room.mines.get(i);
            if (!m.isLit() && !player.isDead()
                    && Vector2.dst(m.x, m.ballY() - 0.2f, player.feetX(), player.feetY()) < SeaMine.TRIGGER) {
                lightMine(m, SeaMine.FUSE);
            }
            for (int j = 0; j < enemies.size && !m.isLit(); j++) {
                Enemy e = enemies.get(j);
                if (e.alive && !e.flying && !e.boss && !e.isSpawning()
                        && Vector2.dst(m.x, m.ballY() - 0.2f, e.pos.x, e.feetY()) < SeaMine.ENEMY_TRIGGER) {
                    lightMine(m, SeaMine.FUSE);
                }
            }
            if (m.update(delta)) {
                enemyBlast(m.x, m.ballY(), SeaMine.BLAST);
                particles.burst(m.x, m.ballY(), 14, 0.4f, 1.8f, 0.8f, 0.06f, SEA_FOAM);
                sfx().play("boom", 0.8f);
                shake(0.35f);
                for (SeaMine other : room.mines) { // a chain reaction
                    if (other != m && !other.gone && Vector2.dst(m.x, m.ballY(), other.x, other.ballY()) < SeaMine.BLAST + 0.2f) {
                        lightMine(other, 0.15f);
                    }
                }
            }
        }
        for (int i = room.mines.size - 1; i >= 0; i--) {
            if (room.mines.get(i).gone) room.mines.removeIndex(i);
        }
    }

    private void lightMine(SeaMine m, float fuse) {
        if (!m.isLit()) {
            sfx().play("zap", 0.6f);
        }
        m.light(fuse);
    }

    /** A player bullet that reaches a mine sets it off right away (so mines can be used on enemies). */
    private void hitMine(Bullet b) {
        for (SeaMine m : room.mines) {
            if (!m.gone && Vector2.dst(b.pos.x, b.pos.y, m.x, m.ballY()) < SeaMine.HIT_RADIUS + b.radius) {
                b.alive = false;
                lightMine(m, 0.05f);
                return;
            }
        }
    }

    /** The Fluffy Shark's tail wave caught the player: stars round the head, no moving for a moment. */
    public void stunPlayer(float seconds) {
        if (player.isDead() || player.isInvulnerable() || player.isRolling()) {
            return;
        }
        player.stun(seconds);
        sfx().play("hurt", 0.5f);
        particles.burst(player.centerX(), player.hitboxY(), 14, 0.4f, 1.6f, 0.5f, 0.05f, SEA_FOAM);
        toast("STUNNED!");
    }

    // ------------------------------------------------------------ Baa-llo's shop

    /**
     * Sets up Baa-llo's shop in the current room: his counter against the top wall (beside the door if
     * there's one up there), the sheep behind it, and a row of display stands with the wares on them.
     * Endless mode's travelling stall first clears the furniture out of its way.
     */
    public void openShop(Shop shop) {
        Arena a = Arena.now;
        boolean sky = room.stage.isSky();
        float cx = room.doors[Room.N] ? a.centerX - 1.9f : a.centerX;
        float cy = a.top - 0.75f; // (low enough that his cap stays in view)
        float rowY = cy - 1.6f;
        int n = 0; // (the flippers aren't in the row: they get a stand of their own beside the counter)
        for (Shop.Slot s : shop.slots) {
            if (!s.special) n++;
        }
        if (shop.travelling) { // the stall needs the room: furniture standing there is cleared away
            tmpRect.set(a.centerX - n * 0.5f - 0.4f, rowY - 0.6f, n * 1f + 0.8f, a.top - rowY + 0.6f);
            for (Prop p : room.props) {
                if (p.alive && p.solid.overlaps(tmpRect)) {
                    p.alive = false;
                    particles.burst(p.cx, p.cy, 10, 0.3f, 1.2f, 0.5f, 0.05f, DUST);
                }
            }
        }
        Prop counter = new Prop(sky ? Prop.Kind.CLOUD_COUNTER : Prop.Kind.COUNTER, cx, cy);
        addProp(counter);
        shop.counter = counter;
        shop.keeperX = cx;
        shop.keeperY = counter.solid.y + 0.36f; // behind it: the counter hides his feet
        int i = 0;
        for (Shop.Slot s : shop.slots) {
            // the flippers stand by the counter, on the side away from a door
            Prop stand = s.special
                    ? new Prop(sky ? Prop.Kind.CLOUD_STAND : Prop.Kind.STAND, cx + (room.doors[Room.E] ? -2f : 2f), cy - 0.1f)
                    : new Prop(sky ? Prop.Kind.CLOUD_STAND : Prop.Kind.STAND, a.centerX + (i++ - (n - 1) / 2f), rowY);
            addProp(stand);
            s.stand = stand;
            s.pickup.pos.set(stand.cx, stand.solid.y + 0.62f);
        }
        room.shop = shop;
        for (Pickup p : room.pickups) { // nothing left lying under the new furniture
            nudgeIntoTheOpen(p);
        }
        particles.burst(cx, cy, 16, 0.4f, 1.4f, 0.6f, 0.05f, DUST);
    }

    /** Endless: the travelling stall packs up (the bell was rung); what wasn't bought goes with it. */
    public void closeShop() {
        Shop shop = room.shop;
        bellRung = false;
        if (shop == null) {
            return;
        }
        shop.counter.alive = false;
        for (Shop.Slot s : shop.slots) {
            s.stand.alive = false;
        }
        particles.burst(shop.keeperX, shop.keeperY + 0.4f, 24, 0.4f, 1.6f, 0.6f, 0.06f, WOOL);
        room.shop = null;
        navDirty = true;
    }

    /** A saved run: Baa-llo still remembers being shot on an earlier floor. */
    public void setShopGrudge(boolean grudge) {
        shopGrudge = grudge;
    }

    /** Why the player can't use what's on this stand right now (no buying a heart at full health), or null. */
    private String cantTake(Shop.Ware ware) {
        switch (ware) {
            case HEART:
                return player.getHealth() < player.getMaxHealth() ? null : "YOU'RE ALREADY HEALTHY";
            case AMMO:
                return hasLimitedGunNeedingAmmo() ? null : "YOUR AMMO IS FULL";
            case KEY:
                return player.getKeys() < 9 ? null : "NO ROOM FOR MORE KEYS";
            case BLANK:
                return player.getBlanks() < 6 ? null : "NO ROOM FOR MORE BLANKS";
            default:
                return null;
        }
    }

    private void buy(Shop.Slot s) {
        String no = cantTake(s.ware);
        if (no != null) {
            sfx().play("empty", 1f);
            toast(no);
            return;
        }
        if (!player.spendCoins(s.price)) {
            sfx().play("empty", 1f);
            sfx().play("baa", 0.7f);
            room.shop.onTooPoor();
            return;
        }
        s.sold = true;
        s.pickup.pos.set(player.centerX(), player.hitboxY());
        collect(s.pickup); // as if picked up: a gun to carry, an item, a heart...
        sfx().play("kaching", 1f);
        room.shop.onSold();
        particles.burst(s.stand.cx, s.stand.solid.y + 0.5f, 14, 0.4f, 1.6f, 0.5f, 0.04f, GOLD);
    }

    /**
     * A player bullet that reaches Baa-llo (bullets fly over his counter): he ducks and complains, and
     * after {@link Shop#GRUDGE_HITS} hits his prices go up for the rest of the run, like Bello's.
     */
    private void hitShopkeeper(Bullet b) {
        Shop shop = room.shop;
        if (shop == null || shop.isDucking()) {
            return;
        }
        float hx = shop.keeperX;
        float hy = shop.keeperY + 0.45f;
        if (Intersector.distanceSegmentPoint(b.prev.x, b.prev.y, b.pos.x, b.pos.y, hx, hy) > 0.35f + b.radius) {
            return;
        }
        b.alive = false;
        particles.burst(hx, hy, 10, 0.4f, 1.4f, 0.5f, 0.05f, WOOL); // tufts of wool
        sfx().play("baa", 1f);
        if (shop.onShot(shopGrudge)) {
            shopGrudge = true;
            shop.reprice(true);
            shake(0.2f);
            toast("BAA-LLO HOLDS A GRUDGE - PRICES UP!");
        }
    }

    /** The wares on the stands, each with its price tag (green on sale, red when the purse is too light). */
    private void drawShopWares(SpriteBatch batch) {
        TextureRegion coin = game.sprites.prop("coin_0");
        for (Shop.Slot s : room.shop.slots) {
            if (s.sold) continue;
            s.pickup.draw(batch, game);
            String price = String.valueOf(s.price);
            float scale = 0.24f;
            float w = game.textWidth(price, scale);
            float cs = 0.2f;
            float x = s.stand.cx - (w + cs + 0.04f) / 2f;
            float y = s.stand.solid.y - 0.08f;
            batch.draw(coin, x, y - cs, cs, cs);
            Color c = player.getCoins() < s.price ? TOO_DEAR : s.sale ? SALE : PRICE;
            game.text(price, x + cs + 0.04f, y - 0.02f, scale, c, Align.left);
            if (s.sale) {
                game.text("SALE", s.stand.cx + 0.015f, s.stand.solid.y + 0.345f, 0.18f, Color.BLACK, Align.center);
                game.text("SALE", s.stand.cx, s.stand.solid.y + 0.36f, 0.18f, SALE, Align.center);
            }
        }
    }

    // ------------------------------------------------------------ active items

    /** F / RT: use the active item. */
    // ------------------------------------------------------------ 0.8 guns and gadgets

    /** The Cardboard Hero is standing: regular enemies aim at it (Enemy.targetX). */
    public boolean hasDecoy() {
        return decoy != null;
    }

    public float decoyX() {
        return decoy.x;
    }

    public float decoyY() {
        return decoy.centerY();
    }

    /**
     * The gadgets that run on their own: Toy Robot turrets shooting the nearest enemy, the Cardboard Hero
     * folding away, the Little Cloud's lightning, and the Treasure Map showing the whole floor.
     */
    private void updateGadgets(float delta) {
        if (map != null && player.hasItem(Item.TREASURE_MAP)) {
            map.revealed = true;
        }
        if (decoy != null) {
            decoy.update(delta);
            if (decoy.life <= 0) {
                decoy = null;
            }
        }
        for (int i = turrets.size - 1; i >= 0; i--) {
            Turret t = turrets.get(i);
            t.update(delta);
            if (t.life <= 0) {
                particles.burst(t.x, t.y + 0.15f, 8, 0.3f, 1f, 0.4f, 0.04f, SMOKE);
                turrets.removeIndex(i);
                continue;
            }
            Enemy target = nearestEnemy(t.x, t.y + 0.26f, Turret.RANGE, true);
            if (target == null) continue;
            t.angle = MathUtils.atan2Deg360(target.pos.y - t.y - 0.26f, target.pos.x - t.x);
            if (t.cooldown <= 0) {
                t.cooldown = Turret.FIRE_INTERVAL;
                Bullet b = new Bullet(true, t.muzzleX(), t.muzzleY(), t.angle + MathUtils.random(-4f, 4f), 8f, 0.035f,
                        3f * player.damageScale(), TURRET_SHOT);
                b.life = 0.8f;
                b.knockback = 0.3f;
                playerBullets.add(b);
                sfx().play("shoot", 0.25f);
            }
        }
        if (player.getHero() == Hero.GRANNY && !player.isDead()) {
            updateCat(delta);
        }
        if (player.hasItem(Item.LITTLE_CLOUD) && !player.isDead()) {
            littleCloud.update(delta, player.centerX(), player.hitboxY(), MathUtils.cosDeg(player.getAimAngle()) >= 0);
            if (littleCloud.cooldown <= 0) {
                Enemy target = nearestEnemy(littleCloud.pos.x, littleCloud.pos.y, LittleCloud.RANGE, false);
                if (target != null && onScreen(target.pos.x, target.pos.y, 0f)) {
                    littleCloud.struck();
                    float sx = littleCloud.pos.x;
                    float sy = littleCloud.boltY();
                    float dx = target.pos.x - sx;
                    float dy = target.pos.y - sy;
                    addBolt(sx, sy, dx, dy, 0.02f, VOLT);
                    damageEnemy(target, LittleCloud.DAMAGE * player.damageScale(), tmpVec.set(dx, dy), 0.2f);
                    particles.burst(target.pos.x, target.pos.y, 8, 0.5f, 1.8f, 0.25f, 0.035f, VOLT);
                    sfx().play("zap", 0.5f);
                }
            }
        }
    }

    /** Granny's cat: at her heels, and when it's ready it leaps at the nearest enemy on screen and bites. */
    private void updateCat(float delta) {
        cat.update(delta, player.centerX(), player.feetY(), MathUtils.cosDeg(player.getAimAngle()) >= 0);
        if (cat.isLeaping()) {
            if (catTarget == null || !catTarget.alive || catTarget.isUntouchable()) {
                catTarget = null;
                cat.comeBack();
            } else if (cat.leapTo(catTarget.pos.x, catTarget.feetY(), delta)) {
                damageEnemy(catTarget, Cat.DAMAGE * player.damageScale(), tmpVec.set(catTarget.pos).sub(cat.pos), 0.4f);
                particles.burst(catTarget.pos.x, catTarget.pos.y, 6, 0.4f, 1.4f, 0.25f, 0.035f, Color.WHITE);
                sfx().play("hit", 0.5f);
                catTarget = null;
                cat.comeBack();
            }
            return;
        }
        if (cat.cooldown <= 0) {
            Enemy target = nearestEnemy(cat.pos.x, cat.pos.y, Cat.RANGE, false);
            if (target != null && onScreen(target.pos.x, target.pos.y, 0f)) {
                catTarget = target;
                cat.pounce();
                sfx().play("swish", 0.4f);
            }
        }
    }

    /** A fight is over: granny's cat sometimes brings her a present - a heart if she's hurt, else ammo, a blank or a key. */
    private void catPresent() {
        if (player.getHero() != Hero.GRANNY || player.isDead() || MathUtils.random() > Cat.GIFT_CHANCE) {
            return;
        }
        Pickup.Type gift;
        float r = MathUtils.random();
        if (player.getHealth() < player.getMaxHealth() && r < 0.45f) {
            gift = Pickup.Type.HALF_HEART;
        } else if (r < 0.1f) {
            gift = Pickup.Type.KEY;
        } else if (r < 0.55f) {
            gift = Pickup.Type.BLANK;
        } else {
            gift = Pickup.Type.AMMO;
        }
        spawnPickup(new Pickup(gift, cat.pos.x, cat.pos.y + 0.1f));
        toast("THE CAT BROUGHT A PRESENT!");
    }

    /** The closest enemy that can be hit within {@code range} of (x, y) - optionally only one in plain sight. */
    private Enemy nearestEnemy(float x, float y, float range, boolean needSight) {
        Enemy best = null;
        float bestD2 = range * range;
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (!e.alive || e.isUntouchable()) continue;
            float d2 = e.pos.dst2(x, y);
            if (d2 < bestD2 && (!needSight || hasLineOfSight(x, y, e.pos.x, e.pos.y, SIGHT_THICKNESS))) {
                bestD2 = d2;
                best = e;
            }
        }
        return best;
    }

    /** A crackling bolt drawn from (x, y) along (dx, dy) for a moment (lightning, jumping sparks). */
    private void addBolt(float x, float y, float dx, float dy, float width, Color color) {
        Beam b = new Beam();
        b.x = x;
        b.y = y;
        b.angle = MathUtils.atan2Deg360(dy, dx);
        b.length = (float) Math.sqrt(dx * dx + dy * dy);
        b.width = width;
        b.ttl = 0.12f;
        b.color.set(color);
        beams.add(b);
    }

    /** Jumper Cables: the spark jumps from the enemy it hit to the next nearest one, {@code jumps} times. */
    private void chainSpark(Enemy from, int jumps, float damage) {
        Array<Enemy> hit = new Array<>(false, jumps + 1);
        hit.add(from);
        Enemy at = from;
        for (int j = 0; j < jumps; j++) {
            Enemy next = null;
            float bestD2 = 2.6f * 2.6f;
            for (int i = 0; i < enemies.size; i++) {
                Enemy e = enemies.get(i);
                if (!e.alive || e.isUntouchable() || hit.contains(e, true)) continue;
                float d2 = e.pos.dst2(at.pos);
                if (d2 < bestD2) {
                    bestD2 = d2;
                    next = e;
                }
            }
            if (next == null) {
                return;
            }
            float dx = next.pos.x - at.pos.x;
            float dy = next.pos.y - at.pos.y;
            addBolt(at.pos.x, at.pos.y, dx, dy, 0.014f, VOLT);
            damageEnemy(next, damage, tmpVec.set(dx, dy), 0.2f);
            particles.burst(next.pos.x, next.pos.y, 5, 0.4f, 1.4f, 0.2f, 0.03f, VOLT);
            hit.add(next);
            at = next;
        }
        sfx().play("zap", 0.35f);
    }

    /** The Pillow Cannon: a pillow bursts into a ring of feathers where it stops. */
    private void burstIntoFeathers(Bullet pillow) {
        float start = MathUtils.random(360f);
        for (int i = 0; i < pillow.splits; i++) {
            Bullet f = new Bullet(true, pillow.prev.x, pillow.prev.y, start + 360f * i / pillow.splits,
                    MathUtils.random(4f, 5.5f), 0.035f, pillow.damage * 0.35f, FEATHER);
            f.shape = Bullet.Shape.FEATHER;
            f.life = MathUtils.random(0.35f, 0.5f);
            f.knockback = 0.3f;
            f.alreadyHit.addAll(pillow.alreadyHit);
            playerBullets.add(f);
        }
        particles.burst(pillow.prev.x, pillow.prev.y, 10, 0.4f, 1.4f, 0.5f, 0.04f, FEATHER);
        sfx().play("pop", 0.5f);
    }

    /**
     * The Dust Bunny drifting along: it drags regular enemies within reach in toward itself and chews on
     * everything it touches, four bites a second (bosses only get bitten).
     */
    private void updateVortex(Bullet b, float delta) {
        b.tick -= delta;
        boolean bite = b.tick <= 0;
        if (bite) {
            b.tick = 0.25f;
        }
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (!e.alive || e.isUntouchable()) continue;
            float dx = b.pos.x - e.pos.x;
            float dy = b.pos.y - e.pos.y;
            float d = Math.max(0.01f, (float) Math.sqrt(dx * dx + dy * dy));
            if (!e.boss && d < b.vortex + e.radius && d > 0.1f) {
                e.shove(dx / d, dy / d, 2.4f);
            }
            if (bite && d < b.radius + e.radius + 0.1f) {
                damageEnemy(e, b.damage, tmpVec.set(-dx, -dy), 0f);
            }
        }
        if (MathUtils.random() < 20f * delta) { // dust swirling in
            float a = MathUtils.random(360f);
            float r = b.vortex * MathUtils.random(0.5f, 1f);
            particles.spawn(b.pos.x + MathUtils.cosDeg(a) * r, b.pos.y + MathUtils.sinDeg(a) * r,
                    -MathUtils.cosDeg(a) * r * 2f, -MathUtils.sinDeg(a) * r * 2f, 0.4f, 0.03f, b.color, 0f);
        }
    }

    /** The Frying Pan: a swing that whacks the enemies right in front and bats their bullets back the way you aim. */
    public void swingPan(float angle, float damage, float range) {
        float px = player.centerX();
        float py = player.hitboxY();
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (!e.alive || e.isUntouchable() || !inArc(e.pos.x, e.pos.y, px, py, angle, range + e.radius, 75f)) continue;
            damageEnemy(e, damage, tmpVec.set(e.pos).sub(px, py), e.boss ? 0f : 2f);
            particles.burst(e.pos.x, e.pos.y, 6, 0.6f, 2f, 0.2f, 0.04f, SPARK);
            sfx().play("hit", 0.8f);
        }
        int batted = 0;
        for (Bullet b : enemyBullets) {
            if (!b.alive || !inArc(b.pos.x, b.pos.y, px, py, angle, range + 0.15f, 80f)) continue;
            b.alive = false;
            Bullet back = new Bullet(true, b.pos.x, b.pos.y, angle + MathUtils.random(-8f, 8f),
                    Math.max(7f, b.vel.len() * 1.5f), Math.max(0.04f, b.radius), 8f * player.damageScale(),
                    player.currentWeapon().getBulletColor());
            back.life = 1.2f;
            back.knockback = 0.8f;
            playerBullets.add(back);
            batted++;
        }
        if (batted > 0) { // a satisfying clang
            sfx().play("break", 0.6f);
            shake(0.15f);
        }
        swings.add(new Vector3(px, py, angle));
        swingTimes.add(0f);
    }

    private static boolean inArc(float x, float y, float cx, float cy, float angle, float range, float halfArc) {
        float dx = x - cx;
        float dy = y - cy;
        if (dx * dx + dy * dy > range * range) {
            return false;
        }
        float diff = ((MathUtils.atan2Deg360(dy, dx) - angle + 540f) % 360f) - 180f;
        return Math.abs(diff) <= halfArc;
    }

    /** The Polaroid: a flash that wipes out enemy bullets in the picture and hurts every enemy in it. */
    public void cameraFlash(float x, float y, float angle, float damage) {
        for (Bullet b : enemyBullets) {
            if (b.alive && inArc(b.pos.x, b.pos.y, x, y, angle, FLASH_RANGE, FLASH_ARC)) {
                particles.spawn(b.pos.x, b.pos.y, 0, 0.3f, 0.3f, b.radius * 1.4f, Color.WHITE, 2f);
                b.alive = false;
            }
        }
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.alive && !e.isUntouchable() && inArc(e.pos.x, e.pos.y, x, y, angle, FLASH_RANGE + e.radius, FLASH_ARC)
                    && hasLineOfSight(x, y, e.pos.x, e.pos.y, SIGHT_THICKNESS)) {
                damageEnemy(e, damage, tmpVec.set(e.pos).sub(x, y), 0.5f);
                particles.burst(e.pos.x, e.pos.y, 8, 0.4f, 1.4f, 0.3f, 0.04f, Color.WHITE);
            }
        }
        flashShot.set(x, y, angle);
        flashTime = 0f;
    }

    /** The Prickly Sweater: getting hit sends a ring of quills flying out of it. */
    private void sprayQuills() {
        float start = MathUtils.random(360f);
        for (int i = 0; i < 14; i++) {
            Bullet q = new Bullet(true, player.centerX(), player.hitboxY(), start + 360f * i / 14f, 6f, 0.03f,
                    7f * player.damageScale(), QUILL);
            q.streak = 0.14f;
            q.life = 0.6f;
            playerBullets.add(q);
        }
        sfx().play("shotgun", 0.5f);
    }

    public void useActiveItem() {
        Item item = player.getActive();
        if (player.isDead() || item == null) {
            return;
        }
        if (player.getActiveCooldown() > 0) {
            sfx().play("empty", 0.8f);
            return;
        }
        switch (item) {
            case MED_KIT:
            case FLUFFY_CANDY:
                if (!player.heal(item == Item.MED_KIT ? 4 : player.getMaxHealth())) {
                    sfx().play("empty", 0.8f);
                    toast("ALREADY FULL HEALTH");
                    return;
                }
                player.useUpActive();
                sfx().play("heal", 1f);
                particles.burst(player.centerX(), player.hitboxY(), 18, 0.4f, 1.4f, 0.6f, 0.05f,
                        item == Item.MED_KIT ? Color.SCARLET : CANDY);
                toast(item == Item.MED_KIT ? "+2 HEARTS" : "FULLY HEALED!");
                break;
            case GRENADE: {
                // further the longer RT / F was held (Player charges it)
                Grenade g = new Grenade(player.centerX(), player.hitboxY(), player.getAimAngle(), player.grenadeDistance());
                g.damage *= player.damageScale();
                grenades.add(g);
                player.setActiveCooldown(GRENADE_COOLDOWN);
                sfx().play("flip", 0.6f);
                break;
            }
            case SNOW_GLOBE:
                globeTimer = GLOBE_TIME;
                player.setActiveCooldown(GLOBE_COOLDOWN);
                sfx().play("blank", 0.5f);
                for (int i = 0; i < 40; i++) { // a flurry of snow all over the screen
                    particles.spawn(view.x + MathUtils.random(view.width), view.y + MathUtils.random(view.height),
                            MathUtils.random(-0.2f, 0.2f), MathUtils.random(-0.6f, -0.2f), 1.5f, 0.03f, SNOW, 0f);
                }
                toast("EVERYTHING SLOWS DOWN...");
                break;
            case TOY_ROBOT:
                turrets.add(new Turret(player.centerX(), player.feetY(), player.getAimAngle()));
                player.setActiveCooldown(ROBOT_COOLDOWN);
                sfx().play("reload", 0.8f);
                break;
            case CARDBOARD_HERO:
                decoy = new Decoy(player.centerX(), player.feetY());
                player.setActiveCooldown(DECOY_COOLDOWN);
                sfx().play("flip", 0.8f);
                break;
            default:
                break;
        }
    }

    /**
     * The clouds (the owner's idea): during a fight, every few seconds a bird - now and then a little flock -
     * flies across the room at about the player's height, after a warning arrow at the edge. Something to
     * dodge, not to fight: touching one hurts, bullets fly past them, and they're gone once off the cloud.
     */
    private void updateBirds(float delta) {
        if (room.stage.isSky() && (room.type == Room.Type.COMBAT || room.type == Room.Type.ARENA) && director.inFight(this) && !player.isDead()) {
            birdTimer -= delta;
            if (birdTimer <= 0) {
                birdTimer = MathUtils.random(5.5f, 8.5f);
                int dir = MathUtils.randomSign();
                Arena a = Arena.now;
                float y = player.hitboxY() + MathUtils.random(-1f, 1f);
                int flock = MathUtils.random() < 0.25f ? 3 : 1;
                for (int i = 0; i < flock; i++) { // a flock comes in a slanting line, one after another
                    float by = MathUtils.clamp(y + (i - flock / 2) * 0.4f, a.bottom + 0.35f, a.top - 0.35f);
                    birds.add(new Bird(by, dir, 2.4f, i * 0.22f));
                }
            }
        }
        for (int i = birds.size - 1; i >= 0; i--) {
            Bird b = birds.get(i);
            if (b.update(delta) && i == birds.size - 1) {
                sfx().play("flap", 0.35f);
            }
            if (!b.alive) {
                birds.removeIndex(i);
            } else if (!b.isWarning() && Vector2.dst(b.x, b.y, player.centerX(), player.hitboxY())
                    < Bird.RADIUS + Player.HITBOX_RADIUS) {
                hurtPlayer();
            }
        }
    }

    private void updateGrenades(float delta) {
        for (int i = grenades.size - 1; i >= 0; i--) {
            Grenade g = grenades.get(i);
            if (!g.landed()) {
                float nx = g.pos.x + g.vel.x * delta;
                float ny = g.pos.y + g.vel.y * delta;
                // flies over furniture, but stops at the walls
                if (nx > Arena.now.left + 0.1f && nx < Arena.now.right - 0.1f
                        && ny > Arena.now.bottom + 0.1f && ny < Arena.now.top - 0.1f) {
                    g.pos.set(nx, ny);
                }
            }
            g.time += delta;
            if (g.shouldExplode()) {
                explode(g.pos.x, g.pos.y, g.radius, g.damage);
                grenades.removeIndex(i);
            }
        }
    }

    /**
     * Something of the player's going off (grenade, egg, rocket): hurts enemies and furniture within
     * {@code r}, wipes their bullets, never the player. {@code damage} already includes Hot Sauce.
     */
    private void explode(float x, float y, float r, float damage) {
        float k = r / Grenade.RADIUS;
        sfx().play("boom", 0.6f + 0.4f * Math.min(1f, k));
        shake(0.6f * k);
        particles.burst(x, y, (int) (30 * k), 0.6f * k, 3.2f * k, 0.5f, 0.08f, Color.ORANGE);
        particles.burst(x, y, (int) (20 * k), 0.4f * k, 1.6f * k, 0.8f, 0.1f, SMOKE);
        particles.burst(x, y, (int) (12 * k), 0.3f * k, 2.4f * k, 0.3f, 0.05f, Color.GOLD);
        clearEnemyBullets(x, y, r);
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.alive && !e.isUntouchable() && e.pos.dst(x, y) < r + e.radius) {
                Vector2 push = new Vector2(e.pos).sub(x, y).nor().scl(3f);
                damageEnemy(e, damage, push, e.boss ? 0f : 1.5f);
            }
        }
        for (Prop p : room.props) {
            if (p.alive && p.mimic && Vector2.dst(p.cx, p.cy, x, y) < r + 0.3f) {
                wakeMimic(p);
            } else if (p.alive && p.isDestructible() && Vector2.dst(p.cx, p.cy, x, y) < r + 0.3f && p.damage(30f)) {
                breakProp(p);
            }
        }
        explosions.add(new Vector3(x, y, r));
        explosionTimes.add(0f);
    }

    /**
     * One of many little blasts going off at once (the Boom-buss): the same harm as {@link #explode},
     * but quieter and with less shake, so a full spray doesn't deafen you or shake the screen to bits.
     */
    private void smallBlast(float x, float y, float r, float damage) {
        float trauma = this.trauma;
        explode(x, y, r, damage);
        this.trauma = Math.min(1f, trauma + 0.04f);
    }

    /** The Chomper's shark got its teeth into something: a snap, and bits flying. */
    private void chomp(Bullet b) {
        sfx().play("chomp", 0.8f);
        particles.burst(b.prev.x, b.prev.y, 6, 0.4f, 1.6f, 0.3f, 0.035f, Color.WHITE);
        particles.burst(b.prev.x, b.prev.y, 5, 0.3f, 1.2f, 0.4f, 0.05f, b.color);
        shake(0.15f);
    }

    /**
     * An enemy blowing up (dynamite): the same blast as the player's explosives - it hurts the enemies
     * around it too - and it also hurts the player if they're inside it (a dodge roll gets you through).
     */
    public void enemyBlast(float x, float y, float r) {
        explode(x, y, r, 20f);
        if (Vector2.dst(x, y, player.centerX(), player.hitboxY()) < r + Player.HITBOX_RADIUS) {
            hurtPlayer();
        }
    }

    /** The Egg Lobber: an egg lobbed {@code dist} away (longer the longer the trigger was held), over cover; it bursts as it lands. */
    public void lobEgg(float x, float y, float angle, float dist, float damage, float radius) {
        grenades.add(Grenade.egg(x, y, angle, dist, damage, radius));
    }

    /** A laser beam drawn for a moment. */
    private static final class Beam {
        float x;
        float y;
        float angle;
        float length;
        float width;
        float ttl;
        final Color color = new Color();
    }

    /**
     * A laser gun's shot: a beam from (x, y) up to {@code range} long, cut short by walls and cover.
     * It hits the first enemy along it, or every enemy if {@code piercing}; the hits land instantly.
     */
    public void fireBeam(float x, float y, float angle, float damage, float range, float width, Color color, boolean piercing) {
        float dx = MathUtils.cosDeg(angle);
        float dy = MathUtils.sinDeg(angle);
        float len = rayLength(x, y, angle, range);
        if (len < range) { // a mimic lamp doesn't like being poked with a laser either
            float ex = x + dx * (len + 0.06f);
            float ey = y + dy * (len + 0.06f);
            for (Prop p : room.props) {
                if (p.alive && p.mimic && p.bulletRect(game.sprites, dy > 0).contains(ex, ey)) {
                    wakeMimic(p);
                }
            }
        }
        if (!piercing) { // stops at the first enemy in its way
            for (int i = 0; i < enemies.size; i++) {
                Enemy e = enemies.get(i);
                if (!e.alive || e.isUntouchable()) continue;
                float along = (e.pos.x - x) * dx + (e.pos.y - y) * dy;
                if (along > 0 && along < len
                        && Intersector.distanceSegmentPoint(x, y, x + dx * len, y + dy * len, e.pos.x, e.pos.y) < e.radius + width) {
                    len = Math.max(0f, along - e.radius * 0.5f);
                }
            }
        }
        float ex = x + dx * len;
        float ey = y + dy * len;
        for (int i = 0; i < enemies.size; i++) {
            Enemy e = enemies.get(i);
            if (e.alive && !e.isUntouchable()
                    && Intersector.distanceSegmentPoint(x, y, ex, ey, e.pos.x, e.pos.y) < e.radius + width + 0.01f) {
                damageEnemy(e, damage, tmpVec.set(dx, dy), 0.05f);
                if (MathUtils.randomBoolean(0.3f)) {
                    particles.burst(ex, ey, 2, 0.4f, 1.2f, 0.15f, 0.025f, color);
                }
            }
        }
        Beam b = new Beam();
        b.x = x;
        b.y = y;
        b.angle = angle;
        b.length = len;
        b.width = width;
        b.ttl = 0.07f;
        b.color.set(color);
        beams.add(b);
    }

    // ------------------------------------------------------------ draw

    private static float sortY(Object o) {
        if (o instanceof Enemy) return ((Enemy) o).feetY();
        if (o instanceof Prop) return ((Prop) o).sortY();
        if (o instanceof Chest) return ((Chest) o).sortY();
        if (o instanceof Shop) return ((Shop) o).sortY();
        if (o instanceof Turret) return ((Turret) o).y;
        if (o instanceof Decoy) return ((Decoy) o).sortY();
        if (o instanceof Cat) return ((Cat) o).sortY();
        return ((Player) o).feetY();
    }

    public void draw(SpriteBatch batch) {
        Enemy giant = activeBoss();
        if (room.openSky() || giant != null && giant.viewZoom() > 1f) { // (the Kaiju in endless, too)
            drawSky(batch);
        }
        roomRenderer.drawFloor(batch, room);
        roomRenderer.drawBars(batch, room, game.draw);
        for (Rectangle pit : room.pits) {
            batch.draw(game.sprites.prop(pit == room.seaPit ? "pit_sea" : room.stage.pitSprite()), pit.x, pit.y,
                    pit.width, pit.height);
        }
        if (room.seaPit != null) { // the sea far below glints now and then
            float k = (clock * 0.7f) % 1f;
            if (k < 0.25f) {
                game.draw.circle(batch, room.seaPit.x + 0.35f + 0.3f * ((int) (clock * 0.7f) % 3) / 2f,
                        room.seaPit.y + 0.4f, 0.03f + 0.04f * MathUtils.sin(k * 4f * MathUtils.PI), Color.WHITE, 0.9f);
            }
        }
        for (Puddle pd : puddles) {
            pd.draw(batch, game.draw);
        }
        for (int i = 0; i < enemies.size; i++) {
            enemies.get(i).drawFloor(batch, game.draw);
        }
        for (Bird bird : birds) {
            bird.drawShadow(batch, game.draw);
        }
        if (room.exit) {
            // drawn standing up from the bottom of the spot you use it from
            TextureRegion ex = game.sprites.prop(exitKind().sprite);
            float ew = Sprites.w(ex);
            batch.draw(ex, exitX() - ew / 2f, exitY() - exitKind().h / 2f, ew, Sprites.h(ex));
        }

        for (Pickup p : room.pickups) {
            p.draw(batch, game);
        }
        for (SeaMine m : room.mines) {
            m.draw(batch, game.draw);
        }

        // everything that stands on the floor is drawn back-to-front
        drawList.clear();
        drawList.addAll(room.props);
        drawList.addAll(room.chests);
        drawList.addAll(enemies);
        drawList.addAll(dying);
        drawList.add(player);
        if (room.shop != null) {
            drawList.add(room.shop);
        }
        drawList.addAll(turrets);
        if (decoy != null) {
            drawList.add(decoy);
        }
        if (player.getHero() == Hero.GRANNY && !player.isDead()) {
            drawList.add(cat);
        }
        drawList.sort((a, b) -> Float.compare(sortY(b), sortY(a)));
        for (Object o : drawList) {
            if (o instanceof Prop) ((Prop) o).draw(batch, game.sprites);
            else if (o instanceof Chest) ((Chest) o).draw(batch, game.sprites);
            else if (o instanceof Enemy) ((Enemy) o).draw(batch, game.draw);
            else if (o instanceof Shop) ((Shop) o).draw(batch, game.sprites, shopGrudge);
            else if (o instanceof Turret) ((Turret) o).draw(batch, game.draw);
            else if (o instanceof Decoy) ((Decoy) o).draw(batch, game.draw);
            else if (o instanceof Cat) ((Cat) o).draw(batch, game.draw);
            else player.draw(batch, game.draw);
        }
        if (room.shop != null) {
            drawShopWares(batch);
        }
        drawMoons(batch);
        if (player.hasItem(Item.FLUFF_FIELD) && !player.isDead()) { // a faint pink ring, bright while it pushes
            float r = FIELD_RADIUS + MathUtils.sin(clock * 2.5f) * 0.03f;
            game.draw.ring(batch, player.centerX(), player.hitboxY(), r, FIELD_PINK, 0.14f + 0.2f * fieldGlow);
            if (fieldGlow > 0) {
                game.draw.circle(batch, player.centerX(), player.hitboxY(), r, FIELD_PINK, 0.05f * fieldGlow);
            }
        }
        if (player.isShieldUp() && !player.isDead()) { // Bubble Shield: a soap bubble around the hero
            float wob = MathUtils.sin(clock * 3f) * 0.015f;
            game.draw.circle(batch, player.centerX(), player.hitboxY() + 0.05f, 0.34f + wob, SHIELD, 0.16f);
            game.draw.ring(batch, player.centerX(), player.hitboxY() + 0.05f, 0.34f + wob, SHIELD, 0.5f);
        }
        for (GasCloud g : gas) { // in the air, over everyone, under the bullets
            g.draw(batch, game.draw);
        }
        if (room.stage.night) {
            drawNight(batch);
        }
        if (room.stage.isSea()) {
            drawUnderwater(batch);
        }

        for (Bullet b : playerBullets) {
            b.draw(batch, game.draw);
        }
        for (Bullet b : enemyBullets) {
            b.draw(batch, game.draw);
        }
        for (Grenade g : grenades) {
            g.draw(batch, game.draw);
        }
        for (Bird bird : birds) { // up in the air, over everything on the floor
            bird.draw(batch, game.draw, view);
        }
        if (player.hasItem(Item.LITTLE_CLOUD) && !player.isDead()) {
            littleCloud.drawShadow(batch, game.draw);
            littleCloud.draw(batch, game.draw);
        }
        for (int i = 0; i < swings.size; i++) { // the Frying Pan's swoosh: a crescent fading out
            Vector3 sw = swings.get(i);
            float k = swingTimes.get(i) / SWING_TIME;
            for (int j = -3; j <= 3; j++) {
                float a = sw.z + j * 22f;
                float r = 0.75f;
                game.draw.rotRect(batch, sw.x + MathUtils.cosDeg(a) * r, sw.y + MathUtils.sinDeg(a) * r, 0.08f, 0.32f,
                        a, Color.WHITE, 0.7f * (1f - k) * (1f - Math.abs(j) / 4f));
            }
        }
        if (flashTime >= 0) { // the Polaroid: rays of light in the picture's cone, and the whole screen goes white
            float k = flashTime / FLASH_TIME;
            for (int j = -3; j <= 3; j++) {
                float a = flashShot.z + j * FLASH_ARC / 3.5f;
                float cx = flashShot.x + MathUtils.cosDeg(a) * FLASH_RANGE / 2f;
                float cy = flashShot.y + MathUtils.sinDeg(a) * FLASH_RANGE / 2f;
                game.draw.rotRect(batch, cx, cy, FLASH_RANGE, 0.35f, a, Color.WHITE, 0.35f * (1f - k));
            }
            game.draw.rect(batch, view.x - 1f, view.y - 1f, view.width + 2f, view.height + 2f, Color.WHITE, 0.6f * (1f - k) * (1f - k));
        }
        for (int i = 0; i < explosions.size; i++) { // a quick flash ring where a grenade went off
            float k = explosionTimes.get(i) / 0.3f;
            Vector3 e = explosions.get(i);
            game.draw.circle(batch, e.x, e.y, e.z * (0.4f + 0.6f * k), Color.ORANGE, 0.35f * (1f - k));
        }
        for (Beam bm : beams) { // laser beams: a soft glow, the coloured beam and a white-hot core
            float cx = bm.x + MathUtils.cosDeg(bm.angle) * bm.length / 2f;
            float cy = bm.y + MathUtils.sinDeg(bm.angle) * bm.length / 2f;
            game.draw.rotRect(batch, cx, cy, bm.length, bm.width * 5f, bm.angle, bm.color, 0.25f);
            game.draw.rotRect(batch, cx, cy, bm.length, bm.width * 2f, bm.angle, bm.color, 1f);
            game.draw.rotRect(batch, cx, cy, bm.length, bm.width * 0.8f, bm.angle, Color.WHITE, 0.9f);
            game.draw.circle(batch, bm.x + MathUtils.cosDeg(bm.angle) * bm.length,
                    bm.y + MathUtils.sinDeg(bm.angle) * bm.length, bm.width * 2.5f, bm.color, 0.8f);
        }
        particles.draw(batch, game.draw);
        for (int i = 0; i < enemies.size; i++) {
            enemies.get(i).drawOverlay(batch, game.draw, view);
        }

        if (blankRing >= 0) {
            float k = blankRing / 0.5f;
            game.draw.ring(batch, blankCenter.x, blankCenter.y, 0.3f + k * 6f, BLANK_BLUE, 1f - k);
            game.draw.circle(batch, blankCenter.x, blankCenter.y, 0.3f + k * 6f, Color.WHITE, 0.25f * (1f - k));
        }
    }

    /**
     * The skyscraper's roof and the clouds: open sky all round instead of black, with clouds drifting past
     * far below (slower than the camera, so they feel distant). A storm darkens it.
     */
    private void drawSky(SpriteBatch batch) {
        Draw d = game.draw;
        d.rect(batch, view.x - 1f, view.y - 1f, view.width + 2f, view.height + 2f,
                "storm".equals(room.theme) ? STORM_SKY : SKY);
        for (int i = 0; i < 9; i++) {
            float span = view.width + 6f;
            float x = view.x - 3f + ((i * 3.7f + clock * (0.15f + 0.05f * (i % 3))) % span + span) % span;
            float y = view.y + (i * 2.3f) % (view.height + 1f) - 0.5f;
            float s = 0.5f + 0.25f * (i % 3);
            d.circle(batch, x, y, s, Color.WHITE, 0.85f);
            d.circle(batch, x + s * 0.9f, y - s * 0.15f, s * 0.75f, Color.WHITE, 0.85f);
            d.circle(batch, x - s * 0.9f, y - s * 0.2f, s * 0.65f, Color.WHITE, 0.85f);
        }
    }

    /**
     * The city at night: everything beyond a couple of units from the player fades into darkness,
     * and street lamps cast warm pools of light. Drawn before bullets, so those always stay visible.
     */
    private void drawNight(SpriteBatch batch) {
        float px = player.centerX();
        float py = player.hitboxY();
        batch.setColor(Color.WHITE);
        batch.draw(game.draw.darkness, px - 7f, py - 7f, 14f, 14f);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE); // additive: light adds up
        for (Prop p : room.props) {
            if (p.alive && p.kind == Prop.Kind.LAMP) {
                batch.setColor(LAMP_LIGHT.r, LAMP_LIGHT.g, LAMP_LIGHT.b, 0.33f);
                batch.draw(game.draw.glow, p.cx - 1.4f, p.solid.y + 1.0f - 1.4f, 2.8f, 2.8f);
            }
        }
        if (room.shop != null) { // the shop is lit: a warm glow over the counter and the stands
            Prop c = room.shop.counter;
            batch.setColor(LAMP_LIGHT.r, LAMP_LIGHT.g, LAMP_LIGHT.b, 0.45f);
            batch.draw(game.draw.glow, c.cx - 2.6f, c.cy - 3.1f, 5.2f, 5.2f);
            batch.draw(game.draw.glow, Arena.now.centerX - 3f, c.cy - 3.6f, 6f, 4f);
        }
        for (Enemy e : enemies) { // an awake mimic still carries its light around
            if (e instanceof LampMimic && e.alive) {
                LampMimic m = (LampMimic) e;
                batch.setColor(LAMP_LIGHT.r, LAMP_LIGHT.g * 0.8f, LAMP_LIGHT.b * 0.6f, 0.33f);
                batch.draw(game.draw.glow, m.lanternX() - 1.4f, m.lanternY() - 1.4f, 2.8f, 2.8f);
            }
        }
        batch.setColor(Color.WHITE);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * The secret sea: everything has a blue tint, light ripples down from the surface far above, and
     * bubbles rise. Drawn before bullets, so those always stay clear.
     */
    private void drawUnderwater(SpriteBatch batch) {
        Draw d = game.draw;
        d.rect(batch, view.x - 1f, view.y - 1f, view.width + 2f, view.height + 2f, SEA_TINT, 0.16f);
        for (int i = 0; i < 5; i++) { // slanted shafts of light, swaying
            float x = view.x + (i * 1.7f + MathUtils.sin(clock * 0.4f + i) * 0.4f) % (view.width + 1f);
            d.line(batch, x, view.y + view.height, view.height * 1.3f, 0.35f + 0.1f * (i % 2), -72f, Color.WHITE, 0.05f);
        }
        for (int i = 0; i < 14; i++) { // bubbles drifting up
            float x = view.x + (i * 0.53f * 7.3f) % view.width + MathUtils.sin(clock * 2f + i) * 0.06f;
            float y = view.y + ((clock * (0.35f + 0.05f * (i % 4)) + i * 0.71f) % 1f) * (view.height + 0.4f) - 0.2f;
            d.ring(batch, x, y, 0.025f + 0.01f * (i % 3), SEA_FOAM, 0.55f);
        }
    }

    public void dispose() {
        roomRenderer.dispose();
    }
}
