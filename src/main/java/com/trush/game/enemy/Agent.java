package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.world.GameWorld;

/**
 * Skyscraper enemy: a guard in a black suit and dark glasses, a pistol in each hand. Keeps its
 * distance, strafing fast, and fires both guns at once - two bullets side by side, three times.
 */
public class Agent extends Enemy {

    private static final Color SUIT = Color.valueOf("1c1c22");
    private static final Color SUIT_HI = Color.valueOf("34343e");
    private static final Color SHIRT = Color.valueOf("f5f5f5");
    private static final Color TIE = Color.valueOf("0e0e12");
    private static final Color SKIN = Color.valueOf("d9a066");
    private static final Color HAIR = Color.valueOf("2a1e18");
    private static final Color GLASSES = Color.valueOf("0a0a0e");
    private static final Color GLINT = Color.valueOf("cfe8ff");
    private static final Color BULLET = Color.valueOf("ffe066");

    private static final float W = 0.32f;
    private static final float H = 0.58f;
    private static final float WINDUP = 0.4f;

    private float fireTimer = MathUtils.random(1.3f, 2.1f);
    private float windup;
    private int pairsLeft;
    private float pairTimer;
    private float glint = MathUtils.random(3f);

    public Agent(float x, float y) {
        super(x, y, 26f);
        radius = 0.19f;
        speed = 1.25f;
        score = 35;
    }

    @Override
    public float feetY() {
        return pos.y - H * 0.42f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        glint -= delta;
        if (glint < -0.25f) glint = MathUtils.random(2f, 4f);
        if (pairsLeft > 0) {
            pairTimer -= delta;
            if (pairTimer <= 0) {
                pairTimer = 0.16f;
                pairsLeft--;
                // both pistols at once: two bullets side by side
                float px = -MathUtils.sinDeg(aimAngle) * 0.11f;
                float py = MathUtils.cosDeg(aimAngle) * 0.11f;
                for (int i = -1; i <= 1; i += 2) {
                    world.spawnEnemyBullet(pos.x + px * i, pos.y + py * i, aimAngle, 3.3f * diff).color.set(BULLET);
                }
                world.sfx().play("enemy_shoot", 0.5f);
                if (pairsLeft == 0) {
                    fireTimer = MathUtils.random(1.5f, 2.3f) / diff;
                }
            }
            return;
        }
        if (windup > 0) {
            windup -= delta;
            if (windup <= 0) {
                pairsLeft = 3;
                pairTimer = 0;
            }
            return;
        }
        keepDistance(delta, world, 2.8f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            windup = WINDUP;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float left = pos.x - W / 2f;
        drawFeet(b, d, pos.x, bottom, 0.07f);
        // black suit with a white shirt and a thin black tie
        d.rect(b, left, bottom, W, H * 0.6f, c(SUIT));
        d.rect(b, left, bottom, 0.04f, H * 0.6f, c(SUIT_HI));
        d.rect(b, pos.x - 0.05f, bottom + H * 0.32f, 0.1f, H * 0.28f, SHIRT);
        d.rect(b, pos.x - 0.015f, bottom + H * 0.3f, 0.03f, H * 0.28f, TIE);
        // two pistols
        for (int i = -1; i <= 1; i += 2) {
            float gx = pos.x + i * 0.16f;
            float gy = bottom + H * 0.3f;
            d.line(b, gx, gy, 0.15f, 0.045f, aimAngle, GUN);
        }
        // head: slicked hair, dark glasses that catch the light, an earpiece
        float hy = bottom + H * 0.74f;
        d.circle(b, pos.x, hy, 0.13f, c(SKIN));
        d.rect(b, pos.x - 0.13f, hy + 0.04f, 0.26f, 0.08f, HAIR);
        d.rect(b, pos.x - 0.12f, hy - 0.03f, 0.24f, 0.06f, GLASSES);
        if (glint > -0.25f && glint < 0f) {
            float k = -glint / 0.25f;
            d.rect(b, pos.x - 0.1f + 0.18f * k, hy - 0.02f, 0.03f, 0.04f, GLINT);
        }
        d.rect(b, pos.x + (facingRight() ? -0.14f : 0.12f), hy - 0.06f, 0.02f, 0.08f, SHIRT); // earpiece cord
        if (windup > 0) {
            float k = 1f - windup / WINDUP;
            for (int i = -1; i <= 1; i += 2) {
                float mx = pos.x + i * 0.16f + MathUtils.cosDeg(aimAngle) * 0.15f;
                float my = bottom + H * 0.3f + MathUtils.sinDeg(aimAngle) * 0.15f;
                d.circle(b, mx, my, 0.03f + 0.025f * k, TELEGRAPH, 0.9f);
            }
        }
    }
}
