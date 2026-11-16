package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.world.GameWorld;

/**
 * A chunky dark red shotgun shell. Slower, tankier, fires a tight fan of 5 bullets aimed where you're
 * really heading, not where you are - and below half health it loses its temper: glowing with rage, it walks
 * faster, fires sooner, leads you almost perfectly and puts two more pellets in every fan.
 */
public class ShotgunKin extends Enemy {

    private static final Color STEAM = Color.valueOf("d8d0d0");
    private static final float H = 0.56f;
    private static final float WINDUP = 0.55f;
    /** Below this share of its health it's angry. */
    static final float TEMPER = 0.5f;
    static final int PELLETS = 5;
    static final int ANGRY_PELLETS = 2;
    /** How wide its fan is, calm and angry (tight, so the pellets come in a clump). */
    static final float FAN = 34f;
    static final float ANGRY_FAN = 44f;
    /**
     * How far ahead of a running hero it aims (1 = right where they'll be, by how fast they're really going),
     * calm and angry - calm leaves a little room to dodge into, angry doesn't.
     */
    static final float LEAD = 0.8f;
    static final float ANGRY_LEAD = 1f;
    static final float BULLET_SPEED = 3f;

    private float fireTimer = MathUtils.random(1.8f, 2.8f);
    private float windup;
    private float steam;
    private float recoil;

    public ShotgunKin(float x, float y) {
        super(x, y, 18f);
        radius = 0.23f;
        speed = 0.7f;
        score = 25;
    }

    @Override
    public float feetY() {
        return pos.y - H / 2f;
    }

    /** Lost its temper (below half health). */
    boolean isAngry() {
        return hp < maxHp * TEMPER;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        recoil = Math.max(0f, recoil - delta);
        boolean angry = isAngry();
        if (angry) { // steam puffing off its top
            steam -= delta;
            if (steam <= 0) {
                steam = 0.35f;
                world.particles().burst(pos.x, feetY() + H + 0.02f, 2, 0.05f, 0.4f, 0.5f, 0.035f, STEAM);
            }
        }
        if (windup > 0) {
            windup -= delta;
            if (windup <= 0) {
                int count = (world.getDifficulty() > 1.2f ? PELLETS + 2 : PELLETS) + (angry ? ANGRY_PELLETS : 0);
                float fan = angry ? ANGRY_FAN : FAN;
                float speed = BULLET_SPEED * world.getDifficulty();
                float aim = leadAngle(world, speed, angry ? ANGRY_LEAD : LEAD);
                for (int i = 0; i < count; i++) {
                    float a = aim - fan / 2f + fan * i / (count - 1);
                    shoot(world, a, speed);
                }
                world.sfx().play("enemy_shoot", 0.7f);
                recoil = 0.16f;
                fireTimer = MathUtils.random(2.2f, 3.0f) / world.getDifficulty() * (angry ? 0.6f : 1f);
            }
            return;
        }
        keepDistance(delta, world, 1.8f, angry ? speed * 1.4f : speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            windup = WINDUP;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        d.kin.body(b, true, isAngry(), flash > 0, windup > 0, pos.x, bottom, walkTime,
                facingRight(), c(Color.WHITE));

        float face = facingRight() ? 1 : -1;
        float gx = pos.x + face * 0.14f;
        float gy = bottom + H * 0.35f;
        d.kin.weapon(b, true, gx, gy, aimAngle, recoil);
        if (windup > 0) {
            float mx = gx + MathUtils.cosDeg(aimAngle) * 0.26f;
            float my = gy + MathUtils.sinDeg(aimAngle) * 0.26f;
            d.circle(b, mx, my, 0.06f + 0.04f * (1 - windup / WINDUP), TELEGRAPH, 0.9f);
        }
    }
}
