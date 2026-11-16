package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.world.GameWorld;

/**
 * A big bullet in an army helmet with a heavy machine gun. Slow and tough; it plants its feet and walks
 * a long stream of fire across you from one side to the other - get behind something, or roll against the sweep.
 */
public class TankerKin extends Enemy {

    private static final Color STRAP = Color.valueOf("2e2a20");

    private static final float H = 0.58f;
    private static final float WINDUP = 0.6f;
    /** Shots in one sweep, the time between them, and how wide it sweeps. */
    static final int STREAM = 11;
    private static final float STREAM_GAP = 0.09f;
    static final float SWEEP = 44f;

    private float fireTimer = MathUtils.random(1.8f, 2.8f);
    private float windup;
    private int streamLeft;
    private int streamSize;
    private float streamTimer;
    /** Where the sweep started, and which way it goes. */
    private float sweepFrom;
    private float sweepDir;

    public TankerKin(float x, float y) {
        super(x, y, 26f);
        radius = 0.25f;
        speed = 0.6f;
        score = 35;
    }

    @Override
    public float feetY() {
        return pos.y - H / 2f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        if (streamLeft > 0) {
            streamTimer -= delta;
            if (streamTimer <= 0) {
                streamTimer = STREAM_GAP;
                int i = streamSize - streamLeft;
                streamLeft--;
                float a = sweepFrom + sweepDir * SWEEP * i / (streamSize - 1);
                shoot(world, a, 3.1f * diff);
                world.sfx().play("enemy_shoot", 0.4f);
                if (streamLeft == 0) {
                    fireTimer = MathUtils.random(2.2f, 3.0f) / diff;
                }
            }
            return; // planted while it fires
        }
        if (windup > 0) {
            windup -= delta;
            if (windup <= 0) {
                streamSize = diff > 1.25f ? STREAM + 3 : STREAM;
                streamLeft = streamSize;
                streamTimer = 0;
                sweepDir = MathUtils.randomSign();
                sweepFrom = aimAngle - sweepDir * SWEEP / 2f;
            }
            return;
        }
        keepDistance(delta, world, 2.6f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            windup = WINDUP;
        }
    }

    /** Where the barrel points: along the sweep while firing, at the player otherwise. */
    private float gunAngle() {
        if (streamLeft > 0) {
            int i = streamSize - streamLeft;
            return sweepFrom + sweepDir * SWEEP * i / Math.max(1, streamSize - 1);
        }
        return aimAngle;
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float face = facingRight() ? 1 : -1;
        d.kin.variant(b, d.actors, "TankerKin", pos.x, bottom, walkTime, facingRight(),
                windup > 0 || streamLeft > 0, flash > 0, c(Color.WHITE));

        // a heavy gun with a box of rounds under it
        float gx = pos.x + face * 0.1f;
        float gy = bottom + H * 0.28f;
        float a = gunAngle();
        d.line(b, gx, gy, 0.34f, 0.085f, a, GUN);
        d.line(b, gx + MathUtils.cosDeg(a) * 0.3f, gy + MathUtils.sinDeg(a) * 0.3f, 0.08f, 0.04f, a, STRAP);
        d.rect(b, gx - 0.05f, gy - 0.1f, 0.1f, 0.08f, STRAP);
        if (windup > 0 || streamLeft > 0) {
            float mx = gx + MathUtils.cosDeg(a) * 0.38f;
            float my = gy + MathUtils.sinDeg(a) * 0.38f;
            float k = windup > 0 ? 1 - windup / WINDUP : 1f;
            d.circle(b, mx, my, 0.05f + 0.04f * k, TELEGRAPH, 0.9f);
        }
    }
}
