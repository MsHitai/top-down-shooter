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
                fireCross(this, world, diagonal, diff);
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

    static void fireCross(Enemy shooter, GameWorld world, boolean diagonal, float difficulty) {
        float start = diagonal ? 45f : 0f;
        for (int i = 0; i < 4; i++) {
            Bullet bolt = shooter.shoot(world, start + i * 90f, 2.4f * difficulty);
            bolt.color.set(BOLT);
            bolt.streak = 0.18f;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float left = pos.x - W / 2f;
        float spin = walkTime * 600f;
        d.cast.draw(b, "RoboCart", pos.x, bottom, walkTime, facingRight(), windup > 0, flash > 0, c(Color.WHITE));
    }
}
