package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * Moon enemy (the owner's idea): an asteroid that lives up here - a grumpy floating rock with eyes. It
 * drifts in a straight line, bouncing off the walls (floating over furniture and craters), and now and
 * then throws a little spiral of rock shards. Shoot a big one apart and it breaks into two small ones
 * that fly off and keep shooting (single aimed shards) - like the old Asteroids game.
 */
public class LivingAsteroid extends Enemy {

    private static final Color ROCK = Color.valueOf("8e8a86");
    private static final Color ROCK_DARK = Color.valueOf("5e5a58");
    private static final Color ROCK_LIGHT = Color.valueOf("b8b4ae");
    private static final Color SHARD = Color.valueOf("c8b49a");
    private static final Color GLOW = Color.valueOf("ff8a3a");

    private final boolean big;
    private final Vector2 drift = new Vector2();
    private float spin = MathUtils.random(360f);
    private final float spinSpeed = MathUtils.random(-60f, 60f);
    private float fireTimer = MathUtils.random(1.5f, 2.5f);
    private int volleys;
    private float volleyTimer;
    private float spiral;

    public LivingAsteroid(float x, float y) {
        this(x, y, true, MathUtils.random(360f));
    }

    private LivingAsteroid(float x, float y, boolean big, float heading) {
        super(x, y, big ? 32f : 10f);
        this.big = big;
        radius = big ? 0.3f : 0.17f;
        speed = big ? 0.7f : 1.15f;
        score = big ? 20 : 10;
        flying = true;
        drift.set(MathUtils.cosDeg(heading), MathUtils.sinDeg(heading)).scl(speed);
    }

    @Override
    public float feetY() {
        return pos.y - radius - 0.25f; // (it floats: the shadow sits below it)
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        spin += spinSpeed * delta;
        pos.mulAdd(drift, delta);
        Arena a = Arena.now; // bounce off the walls
        if (pos.x < a.left + radius && drift.x < 0 || pos.x > a.right - radius && drift.x > 0) drift.x = -drift.x;
        if (pos.y < a.bottom + radius && drift.y < 0 || pos.y > a.top - radius && drift.y > 0) drift.y = -drift.y;
        walkTime += delta;

        float diff = world.getDifficulty();
        if (volleys > 0) {
            volleyTimer -= delta;
            if (volleyTimer <= 0) {
                volleyTimer = 0.18f;
                volleys--;
                for (int i = 0; i < 3; i++) { // three arms of a turning spiral
                    Bullet b = shoot(world, spiral + i * 120f, 1.9f * diff);
                    b.shape = Bullet.Shape.SQUARE;
                    b.color.set(SHARD);
                }
                spiral += 18f;
            }
            return;
        }
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            if (big) {
                volleys = diff > 1.3f ? 5 : 4;
                spiral = aimAngle;
                fireTimer = MathUtils.random(2.4f, 3.2f) / diff;
            } else { // the little ones just throw a shard at you
                Bullet b = shoot(world, aimAngle, 2.4f * diff);
                b.shape = Bullet.Shape.SQUARE;
                b.color.set(SHARD);
                fireTimer = MathUtils.random(1.4f, 2.0f) / diff;
            }
            world.sfx().play("enemy_shoot", 0.3f);
        }
    }

    /** A big one breaks into two little ones, flying off either side of the shot. */
    @Override
    public void dropLoot(GameWorld world) {
        if (!big || fellInPit) {
            return;
        }
        float heading = MathUtils.atan2Deg360(drift.y, drift.x);
        for (int i = -1; i <= 1; i += 2) {
            LivingAsteroid bit = new LivingAsteroid(pos.x + i * 0.12f, pos.y, false, heading + i * 70f);
            bit.spawnTimer = 0f; // (no summoning circle: it was here all along)
            world.spawnEnemy(bit);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bob = MathUtils.sin(walkTime * 2.5f) * 0.03f;
        float x = pos.x;
        float y = pos.y + bob;
        float r = radius;
        d.circle(b, x, y, r + 0.02f, Draw.OUTLINE_COLOR);
        d.circle(b, x, y, r, c(ROCK_DARK));
        d.circle(b, x - r * 0.1f, y + r * 0.1f, r * 0.88f, c(ROCK));
        for (int i = 0; i < 3; i++) { // lumps and craters turning as it spins
            float a = spin + i * 120f;
            d.circle(b, x + MathUtils.cosDeg(a) * r * 0.6f, y + MathUtils.sinDeg(a) * r * 0.6f, r * 0.22f, c(ROCK_DARK));
            d.circle(b, x + MathUtils.cosDeg(a + 50f) * r * 0.75f, y + MathUtils.sinDeg(a + 50f) * r * 0.75f, r * 0.12f, c(ROCK_LIGHT));
        }
        if (volleys > 0) { // hot cracks while it fires
            d.circle(b, x, y, r * 1.15f, GLOW, 0.25f);
        }
        // grumpy eyes under a frowning brow
        float s = big ? 0.045f : 0.03f;
        drawEyes(b, d, x, y + r * 0.1f, s * 1.1f, s);
        d.line(b, x - s * 2.4f, y + r * 0.1f + s * 1.6f, s * 2.2f, 0.025f, -15f, ROCK_DARK);
        d.line(b, x + s * 0.2f, y + r * 0.1f + s * 1.1f, s * 2.2f, 0.025f, 15f, ROCK_DARK);
    }
}
