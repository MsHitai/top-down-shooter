package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.world.GameWorld;

/**
 * City enemy: a sniper that keeps far back. Its red laser sight tracks the player, then locks and
 * blinks for a moment before one fast shot comes down the line - step out of the line, or put cover in
 * the way (the laser stops at walls and cover, which also shows where you're safe).
 */
public class Sniper extends Enemy {

    private static final Color CAMO = Color.valueOf("4a5a3a");
    private static final Color CAMO_DARK = Color.valueOf("34402a");
    private static final Color BRASS = Color.valueOf("d9b24c");
    private static final Color BERET = Color.valueOf("2a2a30");
    private static final Color RIFLE = Color.valueOf("3a3a44");
    private static final Color LASER = Color.valueOf("ff3b3b");
    private static final Color BULLET = Color.valueOf("ffe066");

    private static final float W = 0.34f;
    private static final float H = 0.52f;
    private static final float AIM_TIME = 1.4f;
    /** The laser stops following the player this long before the shot. */
    private static final float LOCK_TIME = 0.35f;

    private float fireTimer = MathUtils.random(1.5f, 2.5f);
    private float aimTimer;
    private float laserAngle;
    private float laserLength;

    public Sniper(float x, float y) {
        super(x, y, 18f);
        radius = 0.19f;
        speed = 0.7f;
        score = 35;
    }

    @Override
    public float feetY() {
        return pos.y - H / 2f;
    }

    private float muzzleX() {
        return pos.x + MathUtils.cosDeg(laserAngle) * 0.32f;
    }

    private float muzzleY() {
        return pos.y - 0.04f + MathUtils.sinDeg(laserAngle) * 0.32f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        if (aimTimer > 0) {
            aimTimer -= delta;
            if (aimTimer > LOCK_TIME) {
                laserAngle = aimAngle;
            }
            laserLength = world.rayLength(muzzleX(), muzzleY(), laserAngle, 12f);
            if (aimTimer <= 0) {
                shoot(world, laserAngle, 7.5f * diff).color.set(BULLET);
                world.sfx().play("magnum", 0.6f);
                fireTimer = MathUtils.random(2.2f, 3.0f) / diff;
            }
            return; // stands still while aiming
        }
        keepDistance(delta, world, 3.8f, speed);
        laserAngle = aimAngle;
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            aimTimer = AIM_TIME;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float left = pos.x - W / 2f;
        // laser sight while aiming: steady while tracking, blinking once locked
        if (aimTimer > 0 && (aimTimer > LOCK_TIME || ((int) (aimTimer * 20f)) % 2 == 0)) {
            d.line(b, muzzleX(), muzzleY(), laserLength, aimTimer > LOCK_TIME ? 0.015f : 0.03f, laserAngle, LASER);
        }
        d.cast.draw(b, "Sniper", pos.x, bottom, walkTime, facingRight(), aimTimer > 0, flash > 0, c(Color.WHITE));
        // long rifle with a scope glint
        d.firearm(b, pos.x, pos.y - 0.04f, 0.34f, 0.05f, laserAngle, RIFLE);
        float sx = pos.x + MathUtils.cosDeg(laserAngle) * 0.12f;
        float sy = pos.y + 0.0f + MathUtils.sinDeg(laserAngle) * 0.12f;
        d.circle(b, sx, sy, 0.025f, aimTimer > 0 ? LASER : Color.WHITE, 0.9f);
    }
}
