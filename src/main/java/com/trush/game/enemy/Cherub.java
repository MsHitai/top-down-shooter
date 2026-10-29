package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * Clouds enemy: a chubby little cherub with fluffy curls, tiny wings and a toy bow. It flutters about at
 * a distance and takes turns: a quick fan of three little arrows, then a big heart of bullets that flies at
 * you as one piece - roll through the dent at its top or step out of its way.
 */
public class Cherub extends Angel {

    private static final Color BODY = Color.valueOf("ffd7c2");
    private static final Color BODY_SHADE = Color.valueOf("e8ae98");
    private static final Color CURLS = Color.valueOf("fff3c4");
    private static final Color BOW = Color.valueOf("ff8ac2");
    private static final Color HEART = Color.valueOf("ff5c9a");
    private static final Color ARROW = Color.valueOf("ffe066");

    private static final float DRAW_TIME = 0.6f;
    /** Bullets around the heart's outline, and how big it is. */
    static final int HEART_BULLETS = 16;
    static final float HEART_SIZE = 0.042f;

    private float fireTimer = MathUtils.random(1.4f, 2.4f);
    private float drawTimer;
    private int shots;

    public Cherub(float x, float y) {
        super(x, y, 22f);
        radius = 0.18f;
        speed = 1.2f;
        score = 35;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        clock += delta;
        if (drawTimer > 0) {
            drawTimer -= delta;
            if (drawTimer <= 0) {
                loose(world);
            }
            return;
        }
        keepDistance(delta, world, 2.8f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            drawTimer = DRAW_TIME;
        }
    }

    private void loose(GameWorld world) {
        float diff = world.getDifficulty();
        if (shots++ % 2 == 0) { // three little arrows
            for (int i = -1; i <= 1; i++) {
                Bullet b = shoot(world, aimAngle + i * 14f, 3.2f * diff);
                b.shape = Bullet.Shape.DIAMOND;
                b.color.set(ARROW);
            }
            fireTimer = MathUtils.random(0.9f, 1.3f) / diff;
        } else {
            heart(world, diff);
            fireTimer = MathUtils.random(2.2f, 2.8f) / diff;
        }
        world.sfx().play("twang", 0.5f);
    }

    /** A heart of bullets, upright on the screen, flying at the hero all together. */
    private void heart(GameWorld world, float diff) {
        float cx = pos.x + MathUtils.cosDeg(aimAngle) * 0.5f;
        float cy = pos.y + MathUtils.sinDeg(aimAngle) * 0.5f;
        for (int i = 0; i < HEART_BULLETS; i++) {
            float t = MathUtils.PI2 * i / HEART_BULLETS;
            float s = MathUtils.sin(t);
            float hx = 16f * s * s * s;
            float hy = 13f * MathUtils.cos(t) - 5f * MathUtils.cos(2 * t) - 2f * MathUtils.cos(3 * t) - MathUtils.cos(4 * t);
            Bullet b = world.spawnEnemyBullet(cx + hx * HEART_SIZE, cy + hy * HEART_SIZE, aimAngle, 1.7f * diff);
            b.color.set(HEART);
        }
        world.particles().burst(cx, cy, 6, 0.1f, 0.6f, 0.4f, 0.04f, HEART);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        drawWings(b, d, drawTimer > 0 ? 1f : 0.4f);
        float hem = hem();
        // a round little body and a nappy of cloud
        d.circle(b, pos.x, hem + 0.12f, 0.15f, Draw.OUTLINE_COLOR);
        d.circle(b, pos.x, hem + 0.12f, 0.135f, c(BODY));
        d.circle(b, pos.x - 0.05f, hem + 0.09f, 0.06f, c(BODY_SHADE));
        d.ellipse(b, pos.x, hem + 0.03f, 0.13f, 0.06f, c(Color.WHITE), 1f);
        float headY = hem + 0.32f;
        d.circle(b, pos.x, headY, 0.12f, Draw.OUTLINE_COLOR);
        d.circle(b, pos.x, headY, 0.105f, c(BODY));
        for (int i = -2; i <= 2; i++) { // fluffy curls
            d.circle(b, pos.x + i * 0.045f, headY + 0.08f - Math.abs(i) * 0.015f, 0.045f, c(CURLS));
        }
        drawEyes(b, d, pos.x, headY - 0.02f, 0.042f, 0.026f);
        d.circle(b, pos.x - 0.065f, headY - 0.05f, 0.022f, BOW, 0.5f); // rosy cheeks
        d.circle(b, pos.x + 0.065f, headY - 0.05f, 0.022f, BOW, 0.5f);
        // the toy bow, drawn back while it aims
        float face = facingRight() ? 1 : -1;
        float bx = pos.x + face * 0.16f;
        float by = hem + 0.16f;
        float pull = drawTimer > 0 ? 1f - drawTimer / DRAW_TIME : 0f;
        d.line(b, bx, by, 0.16f, 0.025f, aimAngle + 90f, BOW);
        d.line(b, bx, by, 0.16f, 0.025f, aimAngle - 90f, BOW);
        if (pull > 0) {
            d.line(b, bx - MathUtils.cosDeg(aimAngle) * 0.08f * pull, by - MathUtils.sinDeg(aimAngle) * 0.08f * pull,
                    0.2f, 0.02f, aimAngle, ARROW);
        }
    }
}
