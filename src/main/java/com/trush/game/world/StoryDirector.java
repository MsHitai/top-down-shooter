package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.trush.game.enemy.BulletKin;
import com.trush.game.enemy.Bulletmancer;
import com.trush.game.enemy.Hydrant;
import com.trush.game.enemy.Rat;
import com.trush.game.enemy.ShellCop;
import com.trush.game.enemy.Boss;
import com.trush.game.enemy.ShotgunKin;
import com.trush.game.objects.Chest;
import com.trush.game.objects.Pickup;
import com.trush.game.objects.Tier;
import com.trush.game.objects.Weapon;
import lombok.Getter;

/**
 * Story mode, stage 1 "The Village": explore a random map of rooms. Walking into an uncleared
 * combat room locks its doors until every enemy is dead. Like ETG, guns only come out of chests:
 * a locked chest in the treasure room (each floor drops exactly one key, more are rare finds),
 * the odd locked chest as a room reward, and a free chest from the boss. Beating the boss opens a
 * hatch down to the next floor.
 */
@Getter
public class StoryDirector implements Director {


    private static final int KIN = 0;
    private static final int SHOTGUN = 1;
    private static final int MANCER = 2;
    private static final int HYDRANT = 3;
    private static final int RATS = 4;
    private static final int COP = 5;

    private final StageMap map;
    /** The village on floor 1, the town on floor 2... */
    private final Stage stage;
    private Room room;
    private final IntArray queue = new IntArray();
    private float spawnCooldown;
    private final Array<Weapon> guns = new Array<>();
    private int combatRoomsCleared;
    private boolean bossStarted;
    private boolean bossDead;
    /** The floor's guaranteed key drops when this many combat rooms have been cleared. */
    private final int keyRoom = MathUtils.random(1, 3);
    private boolean finished;
    /** 1 for the first floor; later floors replay the village (until stage 2 exists), harder. */
    private final int floor;
    private boolean atBoss;
    /** Which boss guards this floor, rolled once per floor like in ETG (never the final boss, yet). */
    private final Boss boss;

    private String banner;
    private float bannerTimer;
    private float bannerTotal;

    /** A new run; dev builds can start it on a later floor or at the boss room. */
    public StoryDirector(int floor, boolean atBoss) {
        this(floor, new Array<>());
        this.atBoss = atBoss;
    }

    /** A new floor; guns the player already carries ({@code ownedGuns}, by name) aren't handed out again. */
    public StoryDirector(int floor, Array<String> ownedGuns) {
        this.floor = floor;
        stage = Stage.forFloor(floor);
        map = new StageMap(MathUtils.random(8, 10), stage);
        boss = Boss.pickFor(stage);
        for (Weapon w : Weapon.chestGuns()) {
            if (!ownedGuns.contains(w.getName(), false)) {
                guns.add(w);
            }
        }
        guns.shuffle();
    }

    /** "1. THE VILLAGE" */
    public String stageTitle() {
        return floor + ". " + stage.title;
    }

    public Room startRoom() {
        return atBoss && map.boss != null ? map.boss : map.start;
    }

    private void showBanner(String text, float seconds) {
        banner = text;
        bannerTimer = seconds;
        bannerTotal = seconds;
    }

    @Override
    public void onRoomEntered(Room r, GameWorld world) {
        room = r;
        boolean first = !r.visited;
        r.visited = true;
        world.setDifficulty(Math.min(1.35f, 1f + 0.05f * r.depth) + 0.15f * (floor - 1));
        switch (r.type) {
            case START:
                if (first) {
                    r.cleared = true;
                    showBanner(stageTitle(), 2.5f);
                }
                break;
            case TREASURE:
                if (first) {
                    r.cleared = true;
                    world.placeChest(chest(rollTier(30, 35, 25, 10), Arena.now.centerX, Arena.now.centerY, true));
                    showBanner("TREASURE!", 1.5f);
                }
                break;
            case BOSS:
                if (!r.cleared) {
                    showBanner(boss.title, 2.5f);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void update(float delta, GameWorld world) {
        bannerTimer -= delta;
        if (world.getPlayer().isDead() || room == null) {
            return;
        }
        if (bossDead) {
            finished |= world.isExitTaken();
            return;
        }
        boolean fightRoom = room.type == Room.Type.COMBAT || room.type == Room.Type.BOSS;
        if (fightRoom && !room.cleared && !room.isLocked() && world.playerInsideRoom(0.45f)) {
            startFight(world);
        }
        if (room.isLocked()) {
            spawnCooldown -= delta;
            int active = world.getEnemies().size + world.getPendingEnemies().size;
            if (queue.size > 0 && spawnCooldown <= 0 && active < 5 + room.depth / 3) {
                spawn(world, queue.pop());
                spawnCooldown = 0.45f;
            } else if (queue.size == 0 && active == 0) {
                clearRoom(world);
            }
        }
    }

    private void startFight(GameWorld world) {
        room.setLocked(true);
        world.sfx().play("door", 1f);
        world.shake(0.25f);
        queue.clear();
        spawnCooldown = 0.6f;
        if (room.type == Room.Type.BOSS) {
            bossStarted = true;
            world.spawnEnemy(boss.create(Arena.now.centerX, Arena.now.top - 1.1f, 0));
            world.sfx().play("boss", 1f);
            return;
        }
        int budget = 3 + room.depth + MathUtils.random(0, 2);
        int hydrants = 0;
        while (budget > 0) {
            int type = stage == Stage.TOWN ? townEnemy(budget, hydrants) : villageEnemy(budget);
            if (type == HYDRANT) hydrants++;
            queue.add(type);
            budget -= cost(type);
        }
        queue.shuffle();
    }

    private int villageEnemy(int budget) {
        float r = MathUtils.random();
        if (room.depth >= 2 && budget >= 3 && r < 0.25f) return MANCER;
        if (room.depth >= 1 && budget >= 2 && r < 0.55f) return SHOTGUN;
        return KIN;
    }

    /** The town mixes its own enemies (hydrants, rat packs, shell cops) with the village's. */
    private int townEnemy(int budget, int hydrants) {
        float r = MathUtils.random();
        if (budget >= 2 && r < 0.22f) return COP;
        if (budget >= 2 && r < 0.38f) return RATS;
        if (budget >= 2 && hydrants < 2 && r < 0.52f) return HYDRANT;
        if (budget >= 2 && r < 0.66f) return SHOTGUN;
        if (budget >= 3 && r < 0.76f) return MANCER;
        return KIN;
    }

    private static int cost(int type) {
        return type == MANCER ? 3 : type == KIN ? 1 : 2;
    }

    private void spawn(GameWorld world, int type) {
        Vector2 p = world.findSpawnPoint(1.8f);
        switch (type) {
            case HYDRANT:
                world.spawnEnemy(new Hydrant(p.x, p.y));
                break;
            case RATS: // a pack of three
                for (int i = 0; i < 3; i++) {
                    world.spawnEnemy(new Rat(p.x + MathUtils.random(-0.3f, 0.3f), p.y + MathUtils.random(-0.3f, 0.3f)));
                }
                break;
            case COP:
                world.spawnEnemy(new ShellCop(p.x, p.y));
                break;
            case SHOTGUN:
                world.spawnEnemy(new ShotgunKin(p.x, p.y));
                break;
            case MANCER:
                world.spawnEnemy(new Bulletmancer(p.x, p.y));
                break;
            default:
                world.spawnEnemy(new BulletKin(p.x, p.y));
                break;
        }
    }

    private void clearRoom(GameWorld world) {
        room.cleared = true;
        room.setLocked(false);
        world.sfx().play("wave_clear", 0.8f);
        world.addScore(25 + room.depth * 10);

        if (room.type == Room.Type.BOSS) {
            bossDead = true;
            world.spawnPickup(new Pickup(Pickup.Type.HEART, Arena.now.centerX, Arena.now.bottom + 0.8f));
            world.placeChest(chest(rollTier(0, 40, 40, 20), Arena.now.centerX, Arena.now.centerY - 0.35f, false));
            world.openExit();
            showBanner(boss.defeated, 3f);
            return;
        }

        combatRoomsCleared++;
        if (combatRoomsCleared == keyRoom) { // the floor's one guaranteed key
            world.spawnPickup(new Pickup(Pickup.Type.KEY, Arena.now.centerX, Arena.now.centerY));
            showBanner("ROOM CLEARED - A KEY!", 1.5f);
        } else if (MathUtils.random() < 0.08f) { // rarely, a locked chest
            Vector2 p = world.findChestSpot();
            world.placeChest(chest(rollTier(55, 35, 10, 0), p.x, p.y, true));
            showBanner("ROOM CLEARED - A CHEST!", 1.5f);
        } else {
            Pickup.Type reward = roomReward(world);
            if (reward != null) {
                world.spawnPickup(new Pickup(reward, Arena.now.centerX, Arena.now.centerY));
            }
        }
    }

    /**
     * Small reward for a cleared room, ETG-stingy: usually only something you need (half a heart when
     * hurt, ammo when low), rarely a blank, very rarely an extra key. Often nothing.
     */
    private Pickup.Type roomReward(GameWorld world) {
        if (MathUtils.random() < 0.03f) {
            return Pickup.Type.KEY;
        }
        Array<Pickup.Type> needs = new Array<>();
        if (world.getPlayer().getHealth() < world.getPlayer().getMaxHealth()) {
            needs.add(Pickup.Type.HALF_HEART);
        }
        if (world.hasLimitedGunNeedingAmmo()) {
            needs.add(Pickup.Type.AMMO);
        }
        if (needs.size > 0 && MathUtils.random() < 0.4f) {
            return MathUtils.random() < 0.2f ? Pickup.Type.BLANK : needs.random();
        }
        return MathUtils.random() < 0.1f ? Pickup.Type.BLANK : null;
    }

    /** Random chest tier; weights for D, C, B, A. Later floors shift the odds up one step per floor. */
    private Tier rollTier(int d, int c, int b, int a) {
        Tier[] tiers = Tier.values();
        int roll = MathUtils.random(d + c + b + a - 1);
        int[] weights = {d, c, b, a};
        int i = 0;
        while (roll >= weights[i]) {
            roll -= weights[i++];
        }
        if (floor > 1 && MathUtils.random() < 0.25f * (floor - 1)) {
            i = Math.min(i + 1, tiers.length - 1);
        }
        return tiers[i];
    }

    /** A chest with this floor's gun of that tier inside (taken out of the pool), or loot if there's none left. */
    private Chest chest(Tier tier, float x, float y, boolean locked) {
        Weapon gun = null;
        for (int i = 0; i < guns.size; i++) { // the pool is shuffled, so this is a random gun of that tier
            if (guns.get(i).getTier() == tier) {
                gun = guns.removeIndex(i);
                break;
            }
        }
        return new Chest(tier, x, y, locked, gun);
    }

    @Override
    public String banner() {
        return bannerTimer > 0 ? banner : null;
    }

    @Override
    public float bannerAlpha() {
        if (bannerTimer <= 0) return 0;
        float elapsed = bannerTotal - bannerTimer;
        return MathUtils.clamp(Math.min(elapsed / 0.3f, bannerTimer / 0.5f), 0, 1);
    }

    @Override
    public String music() {
        return bossStarted && !bossDead ? boss.music() : stage.music;
    }

    /** True once the player goes down the exit hatch the boss leaves behind. */
    @Override
    public boolean isFinished() {
        return finished;
    }
}
