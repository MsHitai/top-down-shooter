package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.world.GameWorld;

/** The basic grunt: a walking bullet with a pistol. Keeps its distance and fires single aimed shots. */
public class BulletKin extends Enemy {

    private static final float H = 0.52f;
    private static final float WINDUP = 0.4f;

    private float fireTimer = MathUtils.random(1.2f, 2.2f);
    private float windup;
    private final EnemyCover cover = new EnemyCover();

    public BulletKin(float x, float y) {
        super(x, y, 10.5f); // three shots of the Rusty Sidearm or the Throwing Stars, like ETG's kin
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
                if (!world.hasLineOfSight(pos.x, pos.y, targetX(world), targetY(world), GameWorld.SIGHT_THICKNESS)) {
                    fireTimer = 0.2f;
                    return;
                }
                float angle = aimAngle + MathUtils.random(-6f, 6f);
                float range = distanceToPlayer(world) + radius;
                // Check the actual spread shot, including the bullet's width, not just the aim line.
                if (!world.hasLineOfSight(pos.x, pos.y,
                        pos.x + MathUtils.cosDeg(angle) * range,
                        pos.y + MathUtils.sinDeg(angle) * range, 0.065f)) {
                    fireTimer = 0.2f;
                    return;
                }
                cover.fired();
                shoot(world, angle, 2.7f * world.getDifficulty());
                world.sfx().play("enemy_shoot", 0.5f);
                fireTimer = MathUtils.random(1.4f, 2.4f) / world.getDifficulty();
            }
            return; // stand still while aiming
        }
        if (!cover.move(this, delta, world)) keepDistance(delta, world, 2.2f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && cover.canFire() && canShoot(world)
                && world.hasLineOfSight(pos.x, pos.y, targetX(world), targetY(world), 0.08f)) {
            windup = WINDUP;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        drawFeet(b, d, pos.x, bottom, 0.085f);
        b.setColor(c(Color.WHITE));
        b.draw(d.bulletKin, pos.x - 0.25f, bottom - 1f / 32f, 0.5f, 20f / 32f);
        b.setColor(Color.WHITE);
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
