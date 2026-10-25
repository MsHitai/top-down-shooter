package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * Sea enemy: a deep-sea diver in a brass helmet with a harpoon gun. It keeps its distance and takes aim
 * - a pale aiming line follows you, then locks and blinks red - and fires one fast harpoon down the line.
 * Reloading takes a while (the spear's gone from the gun): that's the time to go for it. Cover stops the
 * line, and the harpoon.
 */
public class Diver extends Enemy {

    private static final Color SUIT = Color.valueOf("d9a03a");
    private static final Color SUIT_DARK = Color.valueOf("a8762a");
    private static final Color BRASS = Color.valueOf("c8a04a");
    private static final Color BRASS_HI = Color.valueOf("f0d080");
    private static final Color GLASS = Color.valueOf("4a8ab0");
    private static final Color HARPOON = Color.valueOf("c8ccd6");
    private static final Color AIM = Color.valueOf("bfefff");
    private static final Color BUBBLE = Color.valueOf("d8f4ff");

    private static final float W = 0.32f;
    private static final float H = 0.5f;
    private static final float AIM_TIME = 1.0f;
    private static final float LOCK_TIME = 0.3f;

    private float fireTimer = MathUtils.random(1.4f, 2.4f);
    private float aimTimer;
    private float reload;
    private float lineAngle;
    private float lineLength;
    private float time = MathUtils.random(5f);

    public Diver(float x, float y) {
        super(x, y, 26f);
        radius = 0.19f;
        speed = 0.75f;
        score = 35;
    }

    @Override
    public float feetY() {
        return pos.y - H / 2f;
    }

    private float muzzleX() {
        return pos.x + MathUtils.cosDeg(lineAngle) * 0.36f;
    }

    private float muzzleY() {
        return pos.y - 0.05f + MathUtils.sinDeg(lineAngle) * 0.36f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        float diff = world.getDifficulty();
        reload = Math.max(0f, reload - delta);
        if (MathUtils.random() < delta * 1.2f) { // breathing out through the helmet
            world.particles().spawn(pos.x + 0.06f, feetY() + H + 0.02f, MathUtils.random(-0.05f, 0.05f), 0.5f, 1.2f,
                    0.025f, BUBBLE, 0f);
        }
        if (aimTimer > 0) {
            aimTimer -= delta;
            if (aimTimer > LOCK_TIME) {
                lineAngle = aimAngle;
            }
            lineLength = world.rayLength(muzzleX(), muzzleY(), lineAngle, 10f);
            if (aimTimer <= 0) {
                Bullet b = world.spawnEnemyBullet(muzzleX(), muzzleY(), lineAngle, 6.0f * diff);
                b.streak = 0.32f;
                b.radius = 0.06f;
                b.color.set(HARPOON);
                world.sfx().play("twang", 0.8f);
                reload = 2.4f / diff;
                fireTimer = reload + MathUtils.random(0.4f, 1.0f);
            }
            return; // holds still while aiming
        }
        keepDistance(delta, world, 3.0f, speed);
        lineAngle = aimAngle;
        fireTimer -= delta;
        if (fireTimer <= 0 && reload <= 0 && canShoot(world)) {
            aimTimer = AIM_TIME / Math.min(1.25f, diff);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float left = pos.x - W / 2f;
        if (aimTimer > 0 && (aimTimer > LOCK_TIME || ((int) (aimTimer * 20f)) % 2 == 0)) {
            boolean locked = aimTimer <= LOCK_TIME;
            d.line(b, muzzleX(), muzzleY(), lineLength, locked ? 0.03f : 0.015f, lineAngle, locked ? TELEGRAPH : AIM,
                    locked ? 1f : 0.6f);
        }
        // flippers for feet
        float step = MathUtils.sin(walkTime * 10f) * 0.02f;
        d.rect(b, pos.x - 0.15f, bottom - 0.02f + step, 0.12f, 0.05f, c(GLASS));
        d.rect(b, pos.x + 0.03f, bottom - 0.02f - step, 0.12f, 0.05f, c(GLASS));
        // the suit, a weight belt, an air tank on the back
        float face = facingRight() ? 1f : -1f;
        d.rect(b, pos.x - face * 0.2f - 0.05f, bottom + 0.12f, 0.1f, 0.24f, c(BRASS));
        d.rect(b, left, bottom, W, H * 0.62f, c(SUIT));
        d.rect(b, left, bottom, 0.05f, H * 0.6f, c(SUIT_DARK));
        d.rect(b, left, bottom + H * 0.25f, W, 0.05f, c(Draw.OUTLINE_COLOR));
        // the round brass helmet with its little window
        float hy = bottom + H * 0.72f;
        d.circle(b, pos.x, hy, 0.17f, Draw.OUTLINE_COLOR);
        d.circle(b, pos.x, hy, 0.155f, c(BRASS));
        d.circle(b, pos.x - 0.05f, hy + 0.06f, 0.035f, BRASS_HI);
        float wx = pos.x + face * 0.04f;
        d.circle(b, wx, hy - 0.01f, 0.095f, c(BRASS_HI));
        d.circle(b, wx, hy - 0.01f, 0.08f, GLASS);
        drawEyes(b, d, wx - face * 0.01f, hy - 0.01f, 0.03f, 0.022f);
        // the harpoon gun, with the spear in it unless it's reloading
        float gx = pos.x;
        float gy = pos.y - 0.05f;
        d.line(b, gx, gy, 0.3f, 0.06f, lineAngle, Draw.OUTLINE_COLOR);
        d.line(b, gx, gy, 0.29f, 0.04f, lineAngle, GUN);
        if (reload <= 0) {
            d.line(b, gx + MathUtils.cosDeg(lineAngle) * 0.2f, gy + MathUtils.sinDeg(lineAngle) * 0.2f, 0.18f, 0.02f,
                    lineAngle, HARPOON);
            d.shape(b, d.triangle, gx + MathUtils.cosDeg(lineAngle) * 0.4f, gy + MathUtils.sinDeg(lineAngle) * 0.4f,
                    0.04f, lineAngle, HARPOON, 1f); // (the triangle points right at 0 degrees)
        }
    }
}
