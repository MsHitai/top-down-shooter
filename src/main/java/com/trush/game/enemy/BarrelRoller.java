package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.objects.Puddle;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * The Slums: a big goon in a vest and bandana who rolls rusty drums of acid at you. He hefts one over his
 * head, then bowls it along the floor; it bursts where it hits something - a pool of acid and a ring of
 * drops. Shoot a drum on its way and it bursts right there (the counterplay). He needs a moment to fetch
 * the next one. Big rubber boots keep the acid off his feet (the Slums' floors are full of it).
 */
public class BarrelRoller extends Enemy {

    private static final Color SKIN = Color.valueOf("c8905a");
    private static final Color VEST = Color.valueOf("3a3a44");
    private static final Color SHIRT = Color.valueOf("e8e0d0");
    private static final Color BANDANA = Color.valueOf("c0392b");
    private static final Color RUST = Color.valueOf("9a4a2a");
    private static final Color RUST_DARK = Color.valueOf("6a3018");
    private static final Color ACID = Color.valueOf("8ae83a");
    private static final Color BOOTS = Color.valueOf("e8c43a");

    private static final float W = 0.44f;
    private static final float H = 0.6f;
    private static final float HEFT = 0.7f;
    private static final float FETCH = 1.6f;
    private static final float ROLL_SPEED = 2.1f;
    public static final float DRUM_RADIUS = 0.17f;
    public static final float POOL_RADIUS = 0.45f;
    private static final int RING = 8;

    private float fireTimer = MathUtils.random(1.6f, 2.6f);
    private float heft;
    /** Fetching the next drum: empty-handed until it's back to 0. */
    private float fetch;
    /** His drums on their way: they burst when they stop. */
    private final Array<Bullet> drums = new Array<>();
    private final Vector2 tmp = new Vector2();

    public BarrelRoller(float x, float y) {
        super(x, y, 42f);
        radius = 0.24f;
        speed = 0.7f;
        score = 45;
        acidProof = true;
    }

    @Override
    public float feetY() {
        return pos.y - H / 2f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        updateDrums(world, diff);
        if (heft > 0) {
            heft -= delta;
            if (heft <= 0) {
                roll(world, diff);
            }
            return; // stands still with the drum over his head
        }
        fetch = Math.max(0f, fetch - delta);
        keepDistance(delta, world, 3f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && fetch <= 0 && canShoot(world)) {
            heft = HEFT;
        }
    }

    private void roll(GameWorld world, float diff) {
        Bullet b = shoot(world, aimAngle, ROLL_SPEED * Math.min(diff, 1.3f));
        b.shape = Bullet.Shape.BARREL;
        b.color.set(RUST);
        b.radius = DRUM_RADIUS;
        b.life = 4f;
        drums.add(b);
        fetch = FETCH;
        fireTimer = MathUtils.random(2.4f, 3.2f) / diff;
        world.sfx().play("flip", 0.7f);
    }

    /** A player's shot pops a drum on its way; a drum that has stopped (wall, furniture, the hero) bursts. */
    private void updateDrums(GameWorld world, float diff) {
        for (int i = drums.size - 1; i >= 0; i--) {
            Bullet d = drums.get(i);
            if (d.alive) {
                for (Bullet p : world.getPlayerBullets()) {
                    float r = d.radius + p.radius;
                    if (p.alive && p.pos.dst2(d.pos) < r * r) {
                        p.alive = false;
                        d.alive = false;
                        break;
                    }
                }
                if (d.alive) continue;
            }
            drums.removeIndex(i);
            if (world.getBlankRing() >= 0) continue; // a blank just makes it vanish
            tmp.set(d.vel).nor().scl(-0.15f).add(d.pos); // step back out of whatever it hit
            Arena.now.clamp(tmp, 0.08f);
            world.spillPuddle(Puddle.Kind.ACID, tmp.x, tmp.y, POOL_RADIUS, 4f, false);
            if (!world.getPlayer().isInvulnerable()) { // (no ring right after it hit the hero)
                float off = MathUtils.random(360f);
                for (int k = 0; k < RING; k++) {
                    Bullet s = world.spawnEnemyBullet(tmp.x, tmp.y, off + 360f * k / RING, 2f * diff);
                    s.shape = Bullet.Shape.DROP;
                    s.color.set(ACID);
                    s.radius = 0.06f;
                }
            }
            world.particles().burst(tmp.x, tmp.y, 12, 0.4f, 1.6f, 0.4f, 0.05f, RUST);
            world.sfx().play("break", 0.8f);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float left = pos.x - W / 2f;
        float face = facingRight() ? 1 : -1;
        drawFeet(b, d, pos.x, bottom, 0.12f);
        d.rect(b, pos.x - 0.17f, bottom - 0.02f, 0.13f, 0.08f, c(BOOTS)); // the rubber boots
        d.rect(b, pos.x + 0.04f, bottom - 0.02f, 0.13f, 0.08f, c(BOOTS));
        // a broad body: white shirt under a dark vest, big arms
        d.rect(b, left, bottom + 0.04f, W, H * 0.6f, c(SHIRT));
        d.rect(b, left, bottom + 0.04f, W * 0.3f, H * 0.6f, c(VEST));
        d.rect(b, left + W * 0.7f, bottom + 0.04f, W * 0.3f, H * 0.6f, c(VEST));
        // the head with a red bandana and a scowl
        float hy = bottom + H * 0.78f;
        d.circle(b, pos.x, hy, 0.15f, c(SKIN));
        d.rect(b, pos.x - 0.15f, hy + 0.03f, 0.3f, 0.08f, c(BANDANA));
        d.rect(b, pos.x - face * 0.17f - 0.03f, hy + 0.02f, 0.06f, 0.05f, c(BANDANA)); // the knot
        d.rect(b, pos.x + face * 0.06f - 0.03f, hy - 0.02f, 0.03f, 0.03f, PUPIL);
        d.rect(b, pos.x + face * 0.06f - 0.05f, hy + 0.015f, 0.06f, 0.015f, PUPIL); // the scowling brow
        // the drum: hugged to his chest, or hefted overhead before he bowls it
        if (fetch <= 0) {
            float lift = heft > 0 ? 1f - heft / HEFT : 0f;
            float dy = bottom + H * (0.32f + 0.6f * lift);
            d.rect(b, pos.x - 0.14f, dy - 0.12f, 0.28f, 0.24f, c(RUST));
            d.rect(b, pos.x - 0.14f, dy - 0.04f, 0.28f, 0.025f, c(RUST_DARK));
            d.rect(b, pos.x - 0.14f, dy + 0.06f, 0.28f, 0.025f, c(RUST_DARK));
            d.rect(b, pos.x - 0.04f, dy + 0.1f, 0.08f, 0.02f, ACID);
            d.rect(b, pos.x - 0.24f, dy - 0.04f, 0.1f, 0.08f, c(SKIN)); // hands either side
            d.rect(b, pos.x + 0.14f, dy - 0.04f, 0.1f, 0.08f, c(SKIN));
        } else {
            d.rect(b, left - 0.04f, bottom + H * 0.25f, 0.08f, 0.2f, c(SKIN)); // arms hanging, empty-handed
            d.rect(b, left + W - 0.04f, bottom + H * 0.25f, 0.08f, 0.2f, c(SKIN));
        }
    }
}
