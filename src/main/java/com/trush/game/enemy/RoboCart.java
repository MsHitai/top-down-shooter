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
 * Skyscraper enemy: a delivery robot - a box on wheels with one big lens. It trundles about in
 * straight lines, turning when it bumps into something, and fires crosses of bolts that alternate
 * between + and x.
 */
public class RoboCart extends Enemy {

    private static final Color BODY = Color.valueOf("d8dce4");
    private static final Color BODY_DARK = Color.valueOf("9aa0aa");
    private static final Color STRIPE = Color.valueOf("f2a03a");
    private static final Color LENS = Color.valueOf("4fc3f7");
    private static final Color WHEEL = Color.valueOf("2a2a30");
    private static final Color BOLT = Color.valueOf("7bd8ff");

    private static final float W = 0.42f;
    private static final float H = 0.34f;
    private static final float WINDUP = 0.4f;

    private final Vector2 heading = new Vector2();
    private final Vector2 next = new Vector2();
    private final Vector2 lastPos = new Vector2();
    private float turnTimer;
    private float fireTimer = MathUtils.random(1.2f, 2f);
    private float windup;
    private boolean diagonal;

    public RoboCart(float x, float y) {
        super(x, y, 28f);
        radius = 0.21f;
        speed = 0.9f;
        score = 30;
        pickHeading();
        lastPos.set(x, y);
    }

    @Override
    public float feetY() {
        return pos.y - H * 0.55f;
    }

    private void pickHeading() {
        heading.set(1, 0).rotateDeg(MathUtils.random(4) * 90f + (MathUtils.randomBoolean(0.3f) ? 45f : 0f));
        turnTimer = MathUtils.random(1.5f, 3f);
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        if (windup > 0) {
            windup -= delta;
            if (windup <= 0) {
                float diff = world.getDifficulty();
                for (int i = 0; i < 4; i++) {
                    Bullet b = shoot(world, i * 90f + (diagonal ? 45f : 0f), 2.4f * diff);
                    b.color.set(BOLT);
                    b.streak = 0.18f;
                }
                diagonal = !diagonal;
                world.sfx().play("zap", 0.45f);
                fireTimer = MathUtils.random(1.4f, 2.0f) / diff;
            }
            return;
        }
        // trundle on; turn when blocked (or bored)
        turnTimer -= delta;
        boolean stuck = pos.dst2(lastPos) < (speed * delta * 0.3f) * (speed * delta * 0.3f);
        lastPos.set(pos);
        next.set(pos).mulAdd(heading, radius + 0.15f);
        if (turnTimer <= 0 || stuck || !Arena.now.contains(next.x, next.y, radius)
                || !world.pointIsFree(next.x, next.y, radius * 0.8f)) {
            pickHeading();
        }
        pos.mulAdd(heading, speed * delta);
        walkTime += delta;
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            windup = WINDUP;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float left = pos.x - W / 2f;
        float spin = walkTime * 600f;
        for (int i = -1; i <= 1; i += 2) { // wheels
            float wx = pos.x + i * W * 0.32f;
            d.circle(b, wx, bottom + 0.05f, 0.065f, WHEEL);
            d.rotRect(b, wx, bottom + 0.05f, 0.09f, 0.02f, spin, BODY_DARK, 1f);
        }
        d.rect(b, left, bottom + 0.06f, W, H, c(BODY));
        d.rect(b, left, bottom + 0.06f, W, 0.06f, c(BODY_DARK));
        d.rect(b, left, bottom + 0.06f + H * 0.55f, W, 0.04f, c(STRIPE));
        d.rect(b, pos.x - 0.03f, bottom + 0.06f + H, 0.06f, 0.08f, BODY_DARK); // antenna
        d.circle(b, pos.x, bottom + 0.06f + H + 0.09f, 0.03f, windup > 0 ? TELEGRAPH : STRIPE);
        float lx = pos.x + MathUtils.cosDeg(aimAngle) * 0.04f;
        float ly = bottom + 0.06f + H * 0.32f + MathUtils.sinDeg(aimAngle) * 0.02f;
        d.circle(b, lx, ly, 0.085f, Draw.OUTLINE_COLOR);
        d.circle(b, lx, ly, 0.065f, windup > 0 ? TELEGRAPH : LENS);
        d.circle(b, lx - 0.02f, ly + 0.02f, 0.02f, Color.WHITE);
    }
}
