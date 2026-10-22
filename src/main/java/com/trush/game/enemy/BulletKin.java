package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.world.GameWorld;

/** The basic grunt: a walking bullet with a pistol. Keeps its distance and fires single aimed shots. */
public class BulletKin extends Enemy {

    private static final Color BRASS = Color.valueOf("d9b24c");
    private static final Color BRASS_DARK = Color.valueOf("a47e2a");
    private static final Color COPPER = Color.valueOf("c4662e");

    private static final float W = 0.36f;
    private static final float H = 0.52f;
    private static final float WINDUP = 0.4f;

    private float fireTimer = MathUtils.random(1.2f, 2.2f);
    private float windup;

    public BulletKin(float x, float y) {
        super(x, y, 15f);
        radius = 0.2f;
        speed = 0.95f;
        score = 10;
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
                float spread = 6f;
                shoot(world, aimAngle + MathUtils.random(-spread, spread), 2.7f * world.getDifficulty());
                world.sfx().play("enemy_shoot", 0.5f);
                fireTimer = MathUtils.random(1.4f, 2.4f) / world.getDifficulty();
            }
            return; // stand still while aiming
        }
        keepDistance(delta, world, 2.2f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && canSeePlayer) {
            windup = WINDUP;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float left = pos.x - W / 2f;
        drawFeet(b, d, pos.x, bottom, 0.085f);
        d.circle(b, pos.x, bottom + H * 0.64f, W / 2f, c(COPPER));
        d.rect(b, left, bottom, W, H * 0.64f, c(BRASS));
        d.rect(b, left, bottom + H * 0.58f, W, 0.035f, c(BRASS_DARK));
        d.rect(b, left, bottom, 0.04f, H * 0.58f, c(BRASS_DARK));
        drawEyes(b, d, pos.x, bottom + H * 0.42f, 0.065f, 0.04f);

        float face = facingRight() ? 1 : -1;
        float gx = pos.x + face * 0.12f;
        float gy = bottom + H * 0.25f;
        d.line(b, gx, gy, 0.2f, 0.065f, aimAngle, GUN);
        if (windup > 0) {
            float mx = gx + MathUtils.cosDeg(aimAngle) * 0.2f;
            float my = gy + MathUtils.sinDeg(aimAngle) * 0.2f;
            d.circle(b, mx, my, 0.05f + 0.03f * (1 - windup / WINDUP), TELEGRAPH, 0.9f);
        }
    }
}
