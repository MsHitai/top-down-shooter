package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.trush.game.enemy.BulletKin;
import com.trush.game.enemy.Bulletmancer;
import com.trush.game.enemy.FluffyKing;
import com.trush.game.enemy.ShotgunKin;
import com.trush.game.objects.Pickup;
import com.trush.game.objects.Weapon;
import lombok.Getter;

/**
 * Story mode, stage 1 "The Village": explore a random map of rooms. Walking into an uncleared
 * combat room locks its doors until every enemy is dead. A treasure room holds a gun; the boss
 * waits in the room furthest from the start.
 */
@Getter
public class StoryDirector implements Director {

    public static final String STAGE_NAME = "THE VILLAGE";

    private static final int KIN = 0;
    private static final int SHOTGUN = 1;
    private static final int MANCER = 2;

    private final StageMap map;
    private Room room;
    private final IntArray queue = new IntArray();
    private float spawnCooldown;
    private final Array<Weapon> guns = new Array<>();
    private int combatRoomsCleared;
    private boolean bossStarted;
    private boolean bossDead;
    private float finishTimer;

    private String banner;
    private float bannerTimer;
    private float bannerTotal;

    public StoryDirector() {
        map = new StageMap(MathUtils.random(8, 10));
        guns.add(Weapon.fluffShotgun());
        guns.add(Weapon.pocketUzi());
        guns.add(Weapon.bigMagnum());
        guns.shuffle();
    }

    public Room startRoom() {
        // dev shortcut: -Dfluffy.bossRoom=true starts the stage at the boss door
        return Boolean.getBoolean("fluffy.bossRoom") && map.boss != null ? map.boss : map.start;
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
        world.setDifficulty(Math.min(1.35f, 1f + 0.05f * r.depth));
        switch (r.type) {
            case START:
                if (first) {
                    r.cleared = true;
                    showBanner("1. " + STAGE_NAME, 2.5f);
                }
                break;
            case TREASURE:
                if (first) {
                    r.cleared = true;
                    if (guns.size > 0) {
                        world.spawnPickup(new Pickup(Pickup.Type.GUN, Arena.CENTER_X, Arena.CENTER_Y, guns.pop()));
                    }
                    world.spawnPickup(new Pickup(Pickup.Type.BLANK, Arena.CENTER_X - 1f, Arena.CENTER_Y - 0.8f));
                    showBanner("TREASURE!", 1.5f);
                }
                break;
            case BOSS:
                if (!r.cleared) {
                    showBanner("THE FLUFFY KING", 2.5f);
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
            finishTimer += delta;
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
            world.spawnEnemy(new FluffyKing(Arena.CENTER_X, Arena.TOP - 1.1f, 0));
            world.sfx().play("boss", 1f);
            return;
        }
        int budget = 3 + room.depth + MathUtils.random(0, 2);
        while (budget > 0) {
            float r = MathUtils.random();
            int type;
            if (room.depth >= 2 && budget >= 3 && r < 0.25f) type = MANCER;
            else if (room.depth >= 1 && budget >= 2 && r < 0.55f) type = SHOTGUN;
            else type = KIN;
            queue.add(type);
            budget -= type == MANCER ? 3 : type == SHOTGUN ? 2 : 1;
        }
        queue.shuffle();
    }

    private void spawn(GameWorld world, int type) {
        Vector2 p = world.findSpawnPoint(1.8f);
        switch (type) {
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
            world.spawnPickup(new Pickup(Pickup.Type.HEART, Arena.CENTER_X - 0.5f, Arena.BOTTOM + 0.8f));
            world.spawnPickup(new Pickup(Pickup.Type.HEART, Arena.CENTER_X + 0.5f, Arena.BOTTOM + 0.8f));
            showBanner("THE FLUFFY KING IS DEFEATED!", 3f);
            return;
        }

        combatRoomsCleared++;
        // the 3rd cleared room always coughs up a gun, otherwise a chance at a small reward
        if (combatRoomsCleared == 3 && guns.size > 0) {
            world.spawnPickup(new Pickup(Pickup.Type.GUN, Arena.CENTER_X, Arena.CENTER_Y, guns.pop()));
            showBanner("ROOM CLEARED - A GUN!", 1.5f);
        } else if (MathUtils.random() < 0.55f) {
            Array<Pickup.Type> options = new Array<>();
            if (world.getPlayer().getHealth() < world.getPlayer().getMaxHealth()) {
                options.add(Pickup.Type.HEART);
            }
            if (world.hasLimitedGunNeedingAmmo()) {
                options.add(Pickup.Type.AMMO);
                options.add(Pickup.Type.AMMO);
            }
            options.add(Pickup.Type.BLANK);
            world.spawnPickup(new Pickup(options.random(), Arena.CENTER_X, Arena.CENTER_Y));
        }
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
        return bossStarted && !bossDead ? "boss" : "village";
    }

    /** True a few seconds after the boss dies, so the victory banner and drops can be enjoyed. */
    @Override
    public boolean isFinished() {
        return bossDead && finishTimer > 4f;
    }
}
