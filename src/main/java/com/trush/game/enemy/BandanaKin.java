package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.world.GameWorld;

/**
 * A bullet kin in a red bandana with a machine pistol: sprays a quick burst of loose shots - and keeps
 * shuffling sideways while it does, so it's never quite where you aimed. Every floor has a few.
 */
public class BandanaKin extends Enemy {


    private static final float H = 0.52f;
    private static final float WINDUP = 0.35f;
    /** Shots in a burst, and the time between them. */
    static final int BURST = 6;
    private static final float BURST_GAP = 0.085f;

    private float fireTimer = MathUtils.random(1.2f, 2.2f);
    private float windup;
    private int burstLeft;
    private float burstTimer;

    public BandanaKin(float x, float y) {
        super(x, y, 15.5f); // tougher than a Bullet Kin: four shots of the sidearm, three of granny's revolver
        radius = 0.2f;
        speed = 1.0f;
        score = 20;
    }

    @Override
    public float feetY() {
        return pos.y - H / 2f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        if (burstLeft > 0) { // spraying - and still edging sideways
            keepDistance(delta, world, 2.2f, speed * 0.35f);
            burstTimer -= delta;
            if (burstTimer <= 0) {
                burstTimer = BURST_GAP;
                burstLeft--;
                shoot(world, aimAngle + MathUtils.random(-11f, 11f), 3.0f * diff);
                world.sfx().play("enemy_shoot", 0.35f);
                if (burstLeft == 0) {
                    fireTimer = MathUtils.random(1.7f, 2.5f) / diff;
                }
            }
            return;
        }
        if (windup > 0) {
            windup -= delta;
            if (windup <= 0) {
                burstLeft = diff > 1.25f ? BURST + 2 : BURST;
                burstTimer = 0;
            }
            return;
        }
        keepDistance(delta, world, 2.2f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            windup = WINDUP;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float face = facingRight() ? 1 : -1;
        d.kin.variant(b, d.actors, "BandanaKin", pos.x, bottom, walkTime, facingRight(),
                windup > 0 || burstLeft > 0, flash > 0, c(Color.WHITE));

        // the machine pistol: short, with a magazine hanging under it
        float gx = pos.x + face * 0.12f;
        float gy = bottom + H * 0.25f;
        d.line(b, gx, gy, 0.22f, 0.07f, aimAngle, GUN);
        d.line(b, gx + MathUtils.cosDeg(aimAngle) * 0.06f, gy + MathUtils.sinDeg(aimAngle) * 0.06f, 0.08f, 0.035f,
                aimAngle - 90f * face, GUN);
        if (windup > 0 || burstLeft > 0) {
            float mx = gx + MathUtils.cosDeg(aimAngle) * 0.22f;
            float my = gy + MathUtils.sinDeg(aimAngle) * 0.22f;
            float k = windup > 0 ? 1 - windup / WINDUP : 1f;
            d.circle(b, mx, my, 0.045f + 0.03f * k, TELEGRAPH, 0.9f);
        }
    }
}
