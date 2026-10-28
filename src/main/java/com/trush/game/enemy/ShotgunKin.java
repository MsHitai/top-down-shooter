package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.player.Player;
import com.trush.game.world.GameWorld;

/**
 * A chunky dark red shotgun shell. Slower, tankier, fires a tight fan of 5 bullets aimed where you're
 * heading, not where you are - and below half health it loses its temper: glowing with rage, it walks
 * faster, fires sooner, leads you almost perfectly and puts two more pellets in every fan.
 */
public class ShotgunKin extends Enemy {

    private static final Color SHELL = Color.valueOf("8e1f22");
    private static final Color SHELL_DARK = Color.valueOf("5e1216");
    private static final Color RAGE = Color.valueOf("ff3b2e");
    private static final Color STEAM = Color.valueOf("d8d0d0");
    private static final Color BRASS = Color.valueOf("d9b24c");

    private static final float W = 0.42f;
    private static final float H = 0.56f;
    private static final float WINDUP = 0.55f;
    /** Below this share of its health it's angry. */
    static final float TEMPER = 0.5f;
    static final int PELLETS = 5;
    static final int ANGRY_PELLETS = 2;
    /** How wide its fan is, calm and angry (tight, so the pellets come in a clump). */
    static final float FAN = 34f;
    static final float ANGRY_FAN = 44f;
    /** How far ahead of a running hero it aims (1 = right where they'll be), calm and angry. */
    static final float LEAD = 0.6f;
    static final float ANGRY_LEAD = 0.9f;
    private static final float BULLET_SPEED = 2.4f;

    private float fireTimer = MathUtils.random(1.8f, 2.8f);
    private float windup;
    private float steam;
    private final Vector2 aheadOf = new Vector2();

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

    /** Aims {@code lead} of the way to where the hero will be when the pellets get there (if they keep running). */
    private float leadAngle(GameWorld world, float bulletSpeed, float lead) {
        if (world.hasDecoy()) {
            return aimAngle; // a cardboard hero doesn't run anywhere
        }
        Player p = world.getPlayer();
        float t = distanceToPlayer(world) / bulletSpeed;
        aheadOf.set(p.getMoveDir()).scl(p.getSpeed() * t * lead).add(p.centerX(), p.hitboxY());
        return MathUtils.atan2Deg360(aheadOf.y - pos.y, aheadOf.x - pos.x);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float left = pos.x - W / 2f;
        drawFeet(b, d, pos.x, bottom, 0.1f);
        d.rect(b, left, bottom + H * 0.2f, W, H * 0.8f, c(SHELL));
        d.rect(b, left, bottom + H * 0.88f, W, H * 0.12f, c(SHELL_DARK));
        d.rect(b, left + W - 0.05f, bottom + H * 0.2f, 0.05f, H * 0.68f, c(SHELL_DARK));
        d.rect(b, left - 0.015f, bottom, W + 0.03f, H * 0.22f, c(BRASS));
        drawEyes(b, d, pos.x, bottom + H * 0.62f, 0.075f, 0.043f);
        if (isAngry()) { // glowing with rage, brows down
            float pulse = 0.25f + 0.15f * MathUtils.sin(walkTime * 10f);
            d.rect(b, left, bottom + H * 0.2f, W, H * 0.8f, RAGE, pulse);
            float face = facingRight() ? 1 : -1;
            for (int i = -1; i <= 1; i += 2) {
                float ex = pos.x + i * 0.075f + face * 0.0375f;
                d.line(b, ex - 0.045f, bottom + H * 0.62f + 0.07f - i * 0.012f, 0.09f, 0.025f, i * 20f, PUPIL);
            }
        }

        float face = facingRight() ? 1 : -1;
        float gx = pos.x + face * 0.14f;
        float gy = bottom + H * 0.35f;
        d.line(b, gx, gy, 0.26f, 0.07f, aimAngle, GUN);
        if (windup > 0) {
            float mx = gx + MathUtils.cosDeg(aimAngle) * 0.26f;
            float my = gy + MathUtils.sinDeg(aimAngle) * 0.26f;
            d.circle(b, mx, my, 0.06f + 0.04f * (1 - windup / WINDUP), TELEGRAPH, 0.9f);
        }
    }
}
