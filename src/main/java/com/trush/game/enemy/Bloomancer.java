package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * A Bulletmancer in garden green. It sows a ring of big slow seeds that drift out and stop in the air;
 * a moment later every seed blooms into a little flower of petals - the gaps between the flowers are
 * where to stand. The seeds burst early if they hit something.
 */
public class Bloomancer extends Bulletmancer {

    private static final Color ROBE = Color.valueOf("3f8a4a");
    private static final Color ROBE_DARK = Color.valueOf("26592e");
    private static final Color HAT = Color.valueOf("d0508a");
    private static final Color ORB = Color.valueOf("9bf07a");
    private static final Color SEED = Color.valueOf("6fcf4f");
    private static final Color PETAL = Color.valueOf("ff8ac2");

    /** Seeds in a ring, petals in each flower, and how long a seed flies before it blooms. */
    static final int SEEDS = 6;
    static final int PETALS = 6;
    static final float BLOOM_AT = 1.4f;

    private final Array<Bullet> seeds = new Array<>(false, 8);
    private float seedTime;

    public Bloomancer(float x, float y) {
        super(x, y, ROBE, ROBE_DARK, HAT, ORB);
    }

    @Override
    protected boolean channel(float delta, GameWorld world) {
        if (seeds.size == 0) {
            return false;
        }
        seedTime += delta;
        if (seedTime >= BLOOM_AT) {
            float diff = world.getDifficulty();
            for (Bullet seed : seeds) {
                if (!seed.alive) continue; // it hit something (or a blank caught it)
                seed.alive = false;
                float turn = MathUtils.random(360f);
                for (int i = 0; i < PETALS; i++) {
                    Bullet p = world.spawnEnemyBullet(seed.pos.x, seed.pos.y, turn + 360f * i / PETALS, 1.3f * diff);
                    p.accel = 0.8f * diff;
                    p.maxSpeed = 2.4f * diff;
                    p.color.set(PETAL);
                    p.shape = Bullet.Shape.DIAMOND;
                }
                world.particles().burst(seed.pos.x, seed.pos.y, 4, 0.1f, 0.6f, 0.3f, 0.04f, PETAL);
            }
            seeds.clear();
            world.sfx().play("enemy_shoot", 0.6f);
            return false;
        }
        return false; // it may walk about while its garden grows
    }

    @Override
    protected void cast(GameWorld world) {
        float diff = world.getDifficulty();
        int n = diff > 1.2f ? SEEDS + 1 : SEEDS; // (one more seed on harder floors, never more petals: a flower garden, not a wall)
        float turn = aimAngle + 180f / n; // the gaps between flowers line up with you
        for (int i = 0; i < n; i++) {
            Bullet b = shoot(world, turn + 360f * i / n, 1.6f);
            b.accel = -1.4f; // slows to a hover
            b.minSpeed = 0.05f;
            b.radius *= 1.6f;
            b.hitScale = 0.7f;
            b.color.set(SEED);
            seeds.add(b);
        }
        seedTime = 0f;
        world.sfx().play("enemy_shoot", 0.7f);
    }

    @Override
    protected float channelGlow() {
        return seeds.size > 0 ? seedTime / BLOOM_AT : 0f;
    }
}
