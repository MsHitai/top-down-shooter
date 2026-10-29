package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * Clouds enemy: a big fluffy dandelion puff drifting in the wind. It puffs up, then blows a ring of
 * seeds that drift out and hang in the air - and a moment later every seed darts at where you're
 * standing then. Keep moving once they stop.
 */
public class Dandelion extends Enemy {

    private static final Color FLUFF = Color.valueOf("fbfaff");
    private static final Color FLUFF_SHADE = Color.valueOf("d8dcef");
    private static final Color HEART = Color.valueOf("f2d36a");
    private static final Color STEM = Color.valueOf("7ab85a");
    private static final Color SEED = Color.valueOf("f4f1e0");

    private static final float PUFF_UP = 0.7f;
    /** Seeds a puff, how long they drift out before they stop, and how long they hang there. */
    static final int SEEDS = 10;
    static final float DRIFT = 0.55f;
    static final float HANG = 0.5f;

    private float clock = MathUtils.random(10f);
    private float fireTimer = MathUtils.random(1.8f, 2.8f);
    private float puff;
    private final Array<Bullet> seeds = new Array<>(false, 12);
    private float seedTime;

    public Dandelion(float x, float y) {
        super(x, y, 26f);
        radius = 0.22f;
        speed = 0.55f;
        score = 35;
        flying = true;
    }

    /** It floats: the shadow is well below it. */
    @Override
    public float feetY() {
        return pos.y - 0.4f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        clock += delta;
        float diff = world.getDifficulty();
        updateSeeds(delta, world, diff);
        if (puff > 0) {
            puff -= delta;
            if (puff <= 0) {
                blow(world, diff);
            }
            return;
        }
        keepDistance(delta, world, 2.4f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && seeds.size == 0 && canShoot(world)) {
            puff = PUFF_UP;
        }
    }

    private void blow(GameWorld world, float diff) {
        int n = diff > 1.2f ? SEEDS + 2 : SEEDS;
        float turn = MathUtils.random(360f);
        for (int i = 0; i < n; i++) {
            Bullet b = shoot(world, turn + 360f * i / n, 1.8f);
            b.accel = -1.8f / DRIFT;
            b.minSpeed = 0.02f;
            b.shape = Bullet.Shape.CLOUD;
            b.color.set(SEED);
            b.life = DRIFT + HANG + 0.2f; // left behind when it pops, they just blow away
            seeds.add(b);
        }
        seedTime = 0f;
        fireTimer = MathUtils.random(3.0f, 3.8f) / diff;
        world.sfx().play("swish", 0.6f);
    }

    /** Once they've hung in the air a moment, every seed darts at where the hero is now. */
    private void updateSeeds(float delta, GameWorld world, float diff) {
        if (seeds.size == 0) {
            return;
        }
        seedTime += delta;
        if (seedTime < DRIFT + HANG) {
            return;
        }
        float tx = world.getPlayer().centerX();
        float ty = world.getPlayer().hitboxY();
        for (Bullet s : seeds) {
            if (!s.alive) continue;
            s.vel.set(tx - s.pos.x, ty - s.pos.y).setLength(0.6f);
            s.accel = 3.5f * diff;
            s.maxSpeed = 3.2f * diff;
            s.life = 6f;
        }
        seeds.clear();
        world.sfx().play("swish", 0.4f);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bob = MathUtils.sin(clock * 2.2f) * 0.03f;
        float cy = feetY() + 0.42f + bob;
        float swell = puff > 0 ? 1f + 0.25f * (1f - puff / PUFF_UP) : 1f;
        d.line(b, pos.x, cy - 0.32f, 0.3f, 0.03f, 90f + MathUtils.sin(clock * 1.5f) * 8f, STEM); // a little stem
        float r = 0.22f * swell;
        d.circle(b, pos.x, cy, r + 0.015f, Draw.OUTLINE_COLOR);
        d.circle(b, pos.x, cy, r, c(FLUFF_SHADE));
        for (int i = 0; i < 10; i++) { // tufts all round
            float a = i * 36f + clock * 10f;
            d.circle(b, pos.x + MathUtils.cosDeg(a) * r * 0.75f, cy + MathUtils.sinDeg(a) * r * 0.75f, r * 0.35f, c(FLUFF));
        }
        d.circle(b, pos.x, cy + 0.02f, r * 0.55f, c(FLUFF));
        d.circle(b, pos.x, cy - 0.01f, 0.05f, c(HEART));
        drawEyes(b, d, pos.x, cy + 0.02f, 0.06f, 0.034f);
        if (puff > 0) { // cheeks blown out
            d.circle(b, pos.x - 0.1f, cy - 0.04f, 0.03f, Color.PINK, 0.6f);
            d.circle(b, pos.x + 0.1f, cy - 0.04f, 0.03f, Color.PINK, 0.6f);
        }
    }
}
