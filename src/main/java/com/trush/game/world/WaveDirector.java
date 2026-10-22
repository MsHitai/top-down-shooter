package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.trush.game.enemy.BulletKin;
import com.trush.game.enemy.Bulletmancer;
import com.trush.game.enemy.Enemy;
import com.trush.game.enemy.FluffyKing;
import com.trush.game.enemy.ShotgunKin;
import com.trush.game.objects.Pickup;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Player;
import lombok.Getter;

/**
 * Runs the wave loop: intro banner -> fight (enemies trickle in) -> cleared (rewards) -> next wave.
 * Every 5th wave is a boss wave. Difficulty slowly ramps up bullet speed and fire rate.
 */
@Getter
public class WaveDirector {

    public enum State { INTRO, FIGHT, CLEARED }

    private static final int KIN = 0;
    private static final int SHOTGUN = 1;
    private static final int MANCER = 2;
    private static final int BOSS = 3;

    private int wave;
    private State state = State.INTRO;
    private float timer;
    private float spawnCooldown;
    private final IntArray queue = new IntArray();
    private final Array<Weapon> gunRewards = new Array<>();
    private int bossTier;

    public WaveDirector() {
        gunRewards.add(Weapon.fluffShotgun());
        gunRewards.add(Weapon.pocketUzi());
        gunRewards.add(Weapon.bigMagnum());
        // dev shortcut: java -Dfluffy.startWave=5 ... jumps straight to the first boss
        wave = Math.max(0, Integer.getInteger("fluffy.startWave", 1) - 1);
        nextWave();
    }

    public boolean isBossWave() {
        return wave % 5 == 0;
    }

    private void nextWave() {
        wave++;
        state = State.INTRO;
        timer = 2.2f;
        queue.clear();
        if (isBossWave()) {
            queue.add(BOSS);
            for (int i = 0; i < bossTier * 2; i++) {
                queue.add(KIN);
            }
        } else {
            int budget = 2 + wave * 2;
            while (budget > 0) {
                int type;
                float r = MathUtils.random();
                if (wave >= 3 && budget >= 3 && r < 0.25f) type = MANCER;
                else if (wave >= 2 && budget >= 2 && r < 0.55f) type = SHOTGUN;
                else type = KIN;
                queue.add(type);
                budget -= type == MANCER ? 3 : type == SHOTGUN ? 2 : 1;
            }
            queue.shuffle();
        }
    }

    private int maxActive() {
        return Math.min(9, 3 + wave / 2);
    }

    public void update(float delta, GameWorld world) {
        if (world.getPlayer().isDead()) {
            return;
        }
        world.setDifficulty(Math.min(1.45f, 1f + 0.045f * (wave - 1) + 0.1f * bossTier));
        switch (state) {
            case INTRO:
                timer -= delta;
                if (timer <= 0) {
                    state = State.FIGHT;
                    if (isBossWave()) {
                        world.sfx().play("boss", 1f);
                        world.shake(0.5f);
                    }
                }
                break;
            case FIGHT:
                spawnCooldown -= delta;
                int active = world.getEnemies().size + world.getPendingEnemies().size;
                if (queue.size > 0 && spawnCooldown <= 0 && active < maxActive()) {
                    spawn(world, queue.pop());
                    spawnCooldown = 0.4f;
                    break; // the spawned enemy only counts as active from now on
                }
                if (queue.size == 0 && active == 0) {
                    onCleared(world);
                }
                break;
            case CLEARED:
                timer -= delta;
                if (timer <= 0) {
                    nextWave();
                }
                break;
        }
    }

    private void spawn(GameWorld world, int type) {
        if (type == BOSS) {
            world.spawnEnemy(new FluffyKing(Arena.CENTER_X, Arena.TOP - 1.0f, bossTier));
            return;
        }
        Vector2 p = findSpawnPoint(world);
        Enemy e;
        switch (type) {
            case SHOTGUN:
                e = new ShotgunKin(p.x, p.y);
                break;
            case MANCER:
                e = new Bulletmancer(p.x, p.y);
                break;
            default:
                e = new BulletKin(p.x, p.y);
                break;
        }
        world.spawnEnemy(e);
    }

    private Vector2 findSpawnPoint(GameWorld world) {
        Player pl = world.getPlayer();
        Vector2 best = new Vector2(Arena.CENTER_X, Arena.TOP - 0.6f);
        float bestScore = -1;
        for (int i = 0; i < 30; i++) {
            float x = MathUtils.random(Arena.LEFT + 0.5f, Arena.RIGHT - 0.5f);
            float y = MathUtils.random(Arena.BOTTOM + 0.5f, Arena.TOP - 0.5f);
            float d = Vector2.dst(x, y, pl.centerX(), pl.hitboxY());
            float nearest = 99;
            for (Enemy e : world.getEnemies()) {
                nearest = Math.min(nearest, e.pos.dst(x, y));
            }
            float score = Math.min(d, 3.5f) + Math.min(nearest, 1f);
            if (d > 2.2f && nearest > 0.6f) {
                return best.set(x, y);
            }
            if (score > bestScore) {
                bestScore = score;
                best.set(x, y);
            }
        }
        return best;
    }

    private void onCleared(GameWorld world) {
        state = State.CLEARED;
        timer = 3f;
        world.sfx().play("wave_clear", 0.8f);
        world.addScore(wave * 20);

        // scatter rewards a bit so leftovers from earlier waves don't stack on top of each other
        float cx = Arena.CENTER_X + MathUtils.random(-1.2f, 1.2f);
        float cy = Arena.CENTER_Y + MathUtils.random(-0.6f, 0.6f);
        Player player = world.getPlayer();

        boolean giveGun = (wave == 2 || wave == 3 || isBossWave()) && gunRewards.size > 0;
        if (giveGun) {
            world.spawnPickup(new Pickup(Pickup.Type.GUN, cx, cy + 0.2f, gunRewards.removeIndex(0)));
        }
        if (isBossWave()) {
            bossTier++;
            player.ensureBlanks(2);
            world.spawnPickup(new Pickup(Pickup.Type.HEART, cx - 0.5f, cy - 0.3f));
            world.spawnPickup(new Pickup(Pickup.Type.HEART, cx + 0.5f, cy - 0.3f));
            if (world.hasLimitedGunNeedingAmmo()) {
                world.spawnPickup(new Pickup(Pickup.Type.AMMO, cx, cy - 0.5f));
            }
            return;
        }

        Array<Pickup.Type> options = new Array<>();
        if (player.getHealth() < player.getMaxHealth()) options.add(Pickup.Type.HEART);
        if (world.hasLimitedGunNeedingAmmo()) {
            options.add(Pickup.Type.AMMO);
            options.add(Pickup.Type.AMMO);
        }
        options.add(Pickup.Type.BLANK);
        Pickup.Type reward = options.random();
        world.spawnPickup(new Pickup(reward, cx + (giveGun ? 0.6f : 0f), cy - (giveGun ? 0.3f : 0f)));
    }

    /** Big centred text for the current state, or null. */
    public String banner() {
        switch (state) {
            case INTRO:
                return isBossWave() ? "BOSS: THE FLUFFY KING" : "WAVE " + wave;
            case CLEARED:
                return "WAVE " + wave + " CLEARED!";
            default:
                return null;
        }
    }

    public float bannerAlpha() {
        if (state == State.FIGHT) return 0;
        float total = state == State.INTRO ? 2.2f : 3f;
        float elapsed = total - timer;
        return MathUtils.clamp(Math.min(elapsed / 0.3f, timer / 0.5f), 0, 1);
    }
}
