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
 * that fly off and keep shooting (single aimed shards) - like the old Asteroids game. Its veteran
 * ({@link #veteran}) is iron-dark with glowing red cracks and a medal stuck on: tougher, a four-armed
 * spiral, and its little ones throw fans of three.
 */
public class LivingAsteroid extends Enemy {

    private static final Color ROCK = Color.valueOf("8e8a86");
    private static final Color ROCK_DARK = Color.valueOf("5e5a58");
    private static final Color ROCK_LIGHT = Color.valueOf("b8b4ae");
    private static final Color SHARD = Color.valueOf("c8b49a");
    private static final Color GLOW = Color.valueOf("ff8a3a");
    private static final Color IRON = Color.valueOf("5a5c66");
    private static final Color IRON_DARK = Color.valueOf("383a44");
    private static final Color IRON_LIGHT = Color.valueOf("8a8e9c");
    private static final Color CRACK = Color.valueOf("ff4a3a");

    private final boolean big;
    private final boolean veteran;
    private final Vector2 drift = new Vector2();
    private float spin = MathUtils.random(360f);
    private final float spinSpeed = MathUtils.random(-60f, 60f);
    private float fireTimer = MathUtils.random(1.5f, 2.5f);
    private int volleys;
    private float volleyTimer;
    private float spiral;

    public LivingAsteroid(float x, float y) {
        this(x, y, true, false, MathUtils.random(360f));
    }

    /** The veteran: iron, red-hot cracks, four arms to its spiral; its bits throw fans of three. */
    public static LivingAsteroid veteran(float x, float y) {
        return new LivingAsteroid(x, y, true, true, MathUtils.random(360f));
    }

    private LivingAsteroid(float x, float y, boolean big, boolean veteran, float heading) {
        super(x, y, big ? (veteran ? 42f : 32f) : (veteran ? 14f : 10f));
        this.big = big;
        this.veteran = veteran;
        radius = big ? 0.3f : 0.17f;
        speed = big ? 0.7f : 1.15f;
        score = (big ? 20 : 10) + (veteran ? 10 : 0);
        flying = true;
        drift.set(MathUtils.cosDeg(heading), MathUtils.sinDeg(heading)).scl(speed);
    }

    @Override
    public String dexId() {
        return veteran ? "VeteranAsteroid" : "LivingAsteroid";
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
                int arms = veteran ? 4 : 3;
                for (int i = 0; i < arms; i++) { // the arms of a turning spiral
                    Bullet b = shoot(world, spiral + i * 360f / arms, 1.9f * diff);
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
            } else { // the little ones just throw a shard at you (a veteran's, a fan of three)
                int n = veteran ? 3 : 1;
                for (int i = 0; i < n; i++) {
                    Bullet b = shoot(world, aimAngle + (i - (n - 1) / 2f) * 16f, 2.4f * diff);
                    b.shape = Bullet.Shape.SQUARE;
                    b.color.set(SHARD);
                }
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
            LivingAsteroid bit = new LivingAsteroid(pos.x + i * 0.12f, pos.y, false, veteran, heading + i * 70f);
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
        Color dark = veteran ? IRON_DARK : ROCK_DARK;
        d.circle(b, x, y, r, c(dark));
        d.circle(b, x - r * 0.1f, y + r * 0.1f, r * 0.88f, c(veteran ? IRON : ROCK));
        for (int i = 0; i < 3; i++) { // lumps and craters turning as it spins
            float a = spin + i * 120f;
            d.circle(b, x + MathUtils.cosDeg(a) * r * 0.6f, y + MathUtils.sinDeg(a) * r * 0.6f, r * 0.22f, c(dark));
            d.circle(b, x + MathUtils.cosDeg(a + 50f) * r * 0.75f, y + MathUtils.sinDeg(a + 50f) * r * 0.75f, r * 0.12f,
                    c(veteran ? IRON_LIGHT : ROCK_LIGHT));
            if (veteran) { // red-hot cracks
                d.line(b, x + MathUtils.cosDeg(a + 85f) * r * 0.3f, y + MathUtils.sinDeg(a + 85f) * r * 0.3f, r * 0.5f,
                        0.018f, a + 85f, CRACK, 0.6f + 0.3f * MathUtils.sin(walkTime * 5f + i));
            }
        }
        if (volleys > 0) { // hot cracks while it fires
            d.circle(b, x, y, r * 1.15f, GLOW, 0.25f);
        }
        // grumpy eyes under a frowning brow
        float s = big ? 0.045f : 0.03f;
        drawEyes(b, d, x, y + r * 0.1f, s * 1.1f, s);
        d.line(b, x - s * 2.4f, y + r * 0.1f + s * 1.6f, s * 2.2f, 0.025f, -15f, ROCK_DARK);
        d.line(b, x + s * 0.2f, y + r * 0.1f + s * 1.1f, s * 2.2f, 0.025f, 15f, ROCK_DARK);
        if (veteran && big) {
            MoonFolk.drawMedal(b, d, x + r * 0.35f, y - r * 0.45f, 1f);
        }
    }
}
