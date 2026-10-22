package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * Town enemy: a fire hydrant that never moves. It sweeps a spray of water bullets back and forth
 * across the player's direction, then rests. Fight it from cover or come at it from the side.
 */
public class Hydrant extends Enemy {

    private static final Color RED = Color.valueOf("d63a2f");
    private static final Color RED_DARK = Color.valueOf("9a2a22");
    private static final Color CAP = Color.valueOf("e8c040");
    private static final Color WATER = Color.valueOf("7ec8f0");

    private static final float W = 0.3f;
    private static final float H = 0.46f;
    private static final float SPRAY_TIME = 1.6f;
    private static final float SWEEP = 55f;

    private float restTimer = MathUtils.random(1.0f, 2.0f);
    private float sprayTimer;
    private float shotTimer;
    private float sprayCenter;
    private float sprayAngle;

    public Hydrant(float x, float y) {
        super(x, y, 30f);
        radius = 0.19f;
        speed = 0f;
        score = 25;
    }

    @Override
    public float feetY() {
        return pos.y - H / 2f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        knock.setZero(); // bolted to the pavement
        if (sprayTimer > 0) {
            sprayTimer -= delta;
            // sweep from one side of the player to the other and back
            float k = 1f - sprayTimer / SPRAY_TIME;
            sprayAngle = sprayCenter + MathUtils.sin(k * MathUtils.PI2) * SWEEP;
            shotTimer -= delta;
            if (shotTimer <= 0) {
                shotTimer = 0.07f / world.getDifficulty();
                Bullet b = shoot(world, sprayAngle, 2.4f * world.getDifficulty());
                b.color.set(WATER);
                b.radius = 0.055f;
            }
            if (sprayTimer <= 0) {
                restTimer = MathUtils.random(1.4f, 2.2f) / world.getDifficulty();
            }
            return;
        }
        restTimer -= delta;
        if (restTimer <= 0 && canSeePlayer) {
            sprayTimer = SPRAY_TIME;
            sprayCenter = aimAngle;
            world.sfx().play("enemy_shoot", 0.6f);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float left = pos.x - W / 2f;
        boolean spraying = sprayTimer > 0;
        d.rect(b, left - 0.04f, bottom, W + 0.08f, 0.06f, c(RED_DARK)); // base flange
        d.rect(b, left, bottom + 0.05f, W, H * 0.62f, c(RED));
        d.rect(b, left, bottom + 0.05f, 0.05f, H * 0.62f, c(RED_DARK));
        d.circle(b, pos.x, bottom + H * 0.68f, W / 2f, c(RED));
        d.rect(b, pos.x - 0.04f, bottom + H * 0.68f + W / 2f - 0.03f, 0.08f, 0.06f, c(CAP));
        // side outlets
        d.rect(b, left - 0.07f, bottom + H * 0.38f, 0.08f, 0.08f, c(CAP));
        d.rect(b, left + W - 0.01f, bottom + H * 0.38f, 0.08f, 0.08f, c(CAP));
        drawEyes(b, d, pos.x, bottom + H * 0.62f, 0.055f, 0.034f);
        if (spraying) { // the nozzle points where the water goes
            float nx = pos.x + MathUtils.cosDeg(sprayAngle) * 0.2f;
            float ny = pos.y + MathUtils.sinDeg(sprayAngle) * 0.2f;
            d.circle(b, nx, ny, 0.05f, WATER, 0.8f);
        } else if (restTimer < 0.35f) { // about to spray
            d.circle(b, pos.x, pos.y + 0.05f, 0.24f, TELEGRAPH, 0.35f);
        }
    }
}
