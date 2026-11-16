package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * A bullet kin gone to sea: tricorn hat, eye patch, a stubby hand cannon. It fires one big slow
 * cannonball that bursts into a ring of grapeshot halfway to you (if it doesn't hit something first),
 * then takes its time ramming in the next one.
 */
public class PirateKin extends Enemy {

    private static final Color CANNON = Color.valueOf("3a3636");
    private static final Color BALL = Color.valueOf("4a4550");
    private static final Color SHOT = Color.valueOf("ffb84d");

    private static final float H = 0.52f;
    private static final float WINDUP = 0.5f;
    /** The cannonball flies this long before it bursts, into this many bits of grapeshot. */
    static final float BURST_AT = 0.75f;
    static final int GRAPESHOT = 6;

    private float fireTimer = MathUtils.random(1.3f, 2.3f);
    private float windup;
    /** The cannonball in flight, and how long it's been flying. */
    private Bullet ball;
    private float ballTime;
    private float recoil;

    public PirateKin(float x, float y) {
        super(x, y, 15f);
        radius = 0.2f;
        speed = 0.85f;
        score = 20;
    }

    @Override
    public float feetY() {
        return pos.y - H / 2f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        recoil = Math.max(0f, recoil - delta);
        if (ball != null) {
            ballTime += delta;
            if (!ball.alive) {
                ball = null; // it hit something (or a blank caught it)
            } else if (ballTime >= BURST_AT) {
                ball.alive = false;
                int n = diff > 1.25f ? GRAPESHOT + 2 : GRAPESHOT;
                float turn = MathUtils.random(360f);
                for (int i = 0; i < n; i++) {
                    Bullet s = world.spawnEnemyBullet(ball.pos.x, ball.pos.y, turn + i * 360f / n, 2.2f * diff);
                    s.color.set(SHOT);
                }
                world.particles().burst(ball.pos.x, ball.pos.y, 6, 0.1f, 0.8f, 0.3f, 0.04f, SHOT);
                world.sfx().play("enemy_shoot", 0.5f);
                ball = null;
            }
        }
        if (windup > 0) {
            windup -= delta;
            if (windup <= 0) {
                ball = shoot(world, aimAngle, 2.0f * diff);
                ball.radius *= 2f;
                ball.hitScale = 0.7f;
                ball.color.set(BALL);
                ballTime = 0f;
                recoil = 0.2f;
                world.sfx().play("enemy_shoot", 0.8f);
                world.particles().burst(pos.x + MathUtils.cosDeg(aimAngle) * 0.3f, pos.y + MathUtils.sinDeg(aimAngle) * 0.3f,
                        5, 0.1f, 0.6f, 0.4f, 0.05f, Color.LIGHT_GRAY);
                fireTimer = MathUtils.random(2.4f, 3.2f) / diff; // ramming in the next ball takes a while
            }
            return;
        }
        keepDistance(delta, world, 2.5f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            windup = WINDUP;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float face = facingRight() ? 1 : -1;
        d.kin.variant(b, d.actors, "PirateKin", pos.x, bottom, walkTime, facingRight(),
                windup > 0, flash > 0, c(Color.WHITE));

        // the hand cannon: short and fat, kicking back when it fires
        float kick = recoil * 0.25f;
        float gx = pos.x + face * 0.12f - MathUtils.cosDeg(aimAngle) * kick;
        float gy = bottom + H * 0.25f - MathUtils.sinDeg(aimAngle) * kick;
        d.line(b, gx, gy, 0.22f, 0.1f, aimAngle, CANNON);
        d.circle(b, gx + MathUtils.cosDeg(aimAngle) * 0.22f, gy + MathUtils.sinDeg(aimAngle) * 0.22f, 0.06f, CANNON);
        if (windup > 0) {
            float mx = gx + MathUtils.cosDeg(aimAngle) * 0.26f;
            float my = gy + MathUtils.sinDeg(aimAngle) * 0.26f;
            d.circle(b, mx, my, 0.06f + 0.04f * (1 - windup / WINDUP), TELEGRAPH, 0.9f);
        }
    }
}
