package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.world.GameWorld;

/**
 * The Bullet Kin grown old in the war, like ETG's veterans: from floor 3 on every plain kin comes as one. An
 * olive beret, a medal, a steel-grey casing and a carbine - and it fires a quick burst of three: the first
 * at you, the other two at where you're running to, so neither standing still nor running straight is safe.
 */
public class VeteranKin extends Enemy {

    private static final Color STEEL = Color.valueOf("9aa3ae");
    private static final Color STEEL_DARK = Color.valueOf("6b737e");
    private static final Color COPPER = Color.valueOf("b0582a");
    private static final Color BERET = Color.valueOf("5d6b34");
    private static final Color BERET_DARK = Color.valueOf("434e24");
    private static final Color MEDAL = Color.valueOf("f2c94c");
    private static final Color RIBBON = Color.valueOf("3a6fd8");

    private static final float W = 0.36f;
    private static final float H = 0.52f;
    private static final float WINDUP = 0.32f;
    /** Shots in a burst, and the time between them. */
    static final int BURST = 3;
    private static final float BURST_GAP = 0.12f;
    private static final float BULLET_SPEED = 3.3f;

    private float fireTimer = MathUtils.random(1.0f, 1.8f);
    private float windup;
    private int burstLeft;
    private float burstTimer;

    public VeteranKin(float x, float y) {
        super(x, y, 14f);
        radius = 0.2f;
        speed = 1.1f;
        score = 15;
    }

    @Override
    public float feetY() {
        return pos.y - H / 2f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        if (burstLeft > 0) {
            burstTimer -= delta;
            if (burstTimer <= 0) {
                burstTimer = BURST_GAP;
                float speed = BULLET_SPEED * diff;
                // the first shot where you are, the rest where you're going
                float aim = burstLeft == BURST ? aimAngle : leadAngle(world, speed, 1f);
                shoot(world, aim + MathUtils.random(-3f, 3f), speed);
                world.sfx().play("enemy_shoot", 0.45f);
                if (--burstLeft == 0) {
                    fireTimer = MathUtils.random(1.6f, 2.4f) / diff;
                }
            }
            return; // planted while it fires
        }
        if (windup > 0) {
            windup -= delta;
            if (windup <= 0) {
                burstLeft = BURST;
                burstTimer = 0;
            }
            return;
        }
        keepDistance(delta, world, 2.6f, speed);
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
        d.rect(b, left, bottom, W, H * 0.64f, c(STEEL));
        d.rect(b, left, bottom + H * 0.58f, W, 0.035f, c(STEEL_DARK));
        d.rect(b, left, bottom, 0.04f, H * 0.58f, c(STEEL_DARK));
        // the beret, slouched to one side, with its badge
        float ty = bottom + H * 0.74f;
        d.ellipse(b, pos.x - face * 0.03f, ty, 0.2f, 0.07f, c(BERET), 1f);
        d.ellipse(b, pos.x - face * 0.03f, ty - 0.03f, 0.19f, 0.03f, c(BERET_DARK), 1f);
        d.circle(b, pos.x + face * 0.07f, ty + 0.01f, 0.018f, MEDAL);
        drawEyes(b, d, pos.x, bottom + H * 0.44f, 0.065f, 0.04f);
        // the medal on its chest
        float mx = pos.x - face * 0.08f;
        d.rect(b, mx - 0.02f, bottom + H * 0.24f, 0.04f, 0.05f, RIBBON);
        d.circle(b, mx, bottom + H * 0.22f, 0.026f, MEDAL);

        // the carbine: longer than the grunt's pistol
        float gx = pos.x + face * 0.12f;
        float gy = bottom + H * 0.25f;
        d.line(b, gx - MathUtils.cosDeg(aimAngle) * 0.06f, gy - MathUtils.sinDeg(aimAngle) * 0.06f, 0.3f, 0.065f,
                aimAngle, GUN);
        if (windup > 0 || burstLeft > 0) {
            float tx = gx + MathUtils.cosDeg(aimAngle) * 0.24f;
            float tyy = gy + MathUtils.sinDeg(aimAngle) * 0.24f;
            float k = windup > 0 ? 1 - windup / WINDUP : 1f;
            d.circle(b, tx, tyy, 0.045f + 0.03f * k, TELEGRAPH, 0.9f);
        }
    }
}
