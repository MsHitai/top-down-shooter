package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.world.GameWorld;

/** A chunky red shotgun shell. Slower, tankier, fires a fan of 5 bullets. */
public class ShotgunKin extends Enemy {

    private static final Color SHELL = Color.valueOf("c0392b");
    private static final Color SHELL_DARK = Color.valueOf("8e2a20");
    private static final Color BRASS = Color.valueOf("d9b24c");

    private static final float W = 0.42f;
    private static final float H = 0.56f;
    private static final float WINDUP = 0.55f;

    private float fireTimer = MathUtils.random(1.8f, 2.8f);
    private float windup;

    public ShotgunKin(float x, float y) {
        super(x, y, 28f);
        radius = 0.23f;
        speed = 0.7f;
        score = 25;
    }

    @Override
    public float feetY() {
        return pos.y - H / 2f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        if (windup > 0) {
            windup -= delta;
            if (windup <= 0) {
                int count = world.getDifficulty() > 1.2f ? 7 : 5;
                float fan = 50f;
                for (int i = 0; i < count; i++) {
                    float a = aimAngle - fan / 2f + fan * i / (count - 1);
                    shoot(world, a, 2.3f * world.getDifficulty());
                }
                world.sfx().play("enemy_shoot", 0.7f);
                fireTimer = MathUtils.random(2.2f, 3.0f) / world.getDifficulty();
            }
            return;
        }
        keepDistance(delta, world, 1.8f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0) {
            windup = WINDUP;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float left = pos.x - W / 2f;
        drawFeet(b, d, pos.x, bottom, 0.1f);
        d.rect(b, left, bottom + H * 0.2f, W, H * 0.8f, c(SHELL));
        d.rect(b, left, bottom + H * 0.88f, W, H * 0.12f, c(SHELL_DARK));
        d.rect(b, left + W - 0.05f, bottom + H * 0.2f, 0.05f, H * 0.68f, c(SHELL_DARK));
        d.rect(b, left - 0.015f, bottom, W + 0.03f, H * 0.22f, c(BRASS));
        drawEyes(b, d, pos.x, bottom + H * 0.62f, 0.075f, 0.043f);

        float face = facingRight() ? 1 : -1;
        float gx = pos.x + face * 0.14f;
        float gy = bottom + H * 0.35f;
        d.line(b, gx, gy, 0.26f, 0.07f, aimAngle, GUN);
        if (windup > 0) {
            float mx = gx + MathUtils.cosDeg(aimAngle) * 0.26f;
            float my = gy + MathUtils.sinDeg(aimAngle) * 0.26f;
            d.circle(b, mx, my, 0.06f + 0.04f * (1 - windup / WINDUP), TELEGRAPH, 0.9f);
        }
    }
}
