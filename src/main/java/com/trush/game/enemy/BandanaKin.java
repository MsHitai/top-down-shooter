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

    private static final Color BRASS = Color.valueOf("d9b24c");
    private static final Color BRASS_DARK = Color.valueOf("a47e2a");
    private static final Color COPPER = Color.valueOf("c4662e");
    private static final Color BANDANA = Color.valueOf("d83a3a");
    private static final Color BANDANA_DARK = Color.valueOf("9a2424");
    private static final Color DOTS = Color.valueOf("ffe9e0");

    private static final float W = 0.36f;
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
        float left = pos.x - W / 2f;
        float face = facingRight() ? 1 : -1;
        drawFeet(b, d, pos.x, bottom, 0.085f);
        d.circle(b, pos.x, bottom + H * 0.64f, W / 2f, c(COPPER));
        d.rect(b, left, bottom, W, H * 0.64f, c(BRASS));
        d.rect(b, left, bottom, 0.04f, H * 0.58f, c(BRASS_DARK));
        // the bandana round its head, the knot's tails flapping behind
        float by = bottom + H * 0.6f;
        d.rect(b, left - 0.01f, by, W + 0.02f, 0.075f, c(BANDANA));
        d.circle(b, pos.x - 0.07f, by + 0.04f, 0.012f, DOTS);
        d.circle(b, pos.x + 0.06f, by + 0.03f, 0.012f, DOTS);
        float flap = MathUtils.sin(walkTime * 12f) * 12f;
        float kx = pos.x - face * (W / 2f + 0.01f);
        d.line(b, kx, by + 0.04f, 0.11f, 0.035f, (face > 0 ? 200f : -20f) + flap, c(BANDANA_DARK));
        d.line(b, kx, by + 0.03f, 0.09f, 0.03f, (face > 0 ? 225f : -45f) - flap, c(BANDANA_DARK));
        drawEyes(b, d, pos.x, bottom + H * 0.44f, 0.065f, 0.04f);

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
