package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * Skyscraper enemy: a humanoid security robot. It marches in to a comfortable range; its visor glows
 * red while it locks on, then it fires three fast laser bolts.
 */
public class Android extends Enemy {

    private static final Color PLATE = Color.valueOf("b8c0cc");
    private static final Color PLATE_DARK = Color.valueOf("7a8290");
    private static final Color JOINT = Color.valueOf("3a3f4a");
    private static final Color VISOR = Color.valueOf("2a2e38");
    private static final Color GLOW = Color.valueOf("4fe0ff");
    private static final Color BOLT = Color.valueOf("ff5a5a");

    private static final float W = 0.32f;
    private static final float H = 0.6f;
    private static final float WINDUP = 0.55f;

    private float fireTimer = MathUtils.random(1.4f, 2.2f);
    private float windup;
    private int boltsLeft;
    private float boltTimer;

    public Android(float x, float y) {
        super(x, y, 30f);
        radius = 0.19f;
        speed = 0.85f;
        score = 35;
    }

    @Override
    public float feetY() {
        return pos.y - H * 0.42f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        if (boltsLeft > 0) {
            boltTimer -= delta;
            if (boltTimer <= 0) {
                boltTimer = 0.12f;
                boltsLeft--;
                Bullet b = shoot(world, aimAngle, 4.2f * diff);
                b.color.set(BOLT);
                b.streak = 0.28f;
                world.sfx().play("zap", 0.4f);
                if (boltsLeft == 0) {
                    fireTimer = MathUtils.random(1.6f, 2.4f) / diff;
                }
            }
            return;
        }
        if (windup > 0) {
            windup -= delta;
            if (windup <= 0) {
                boltsLeft = 3;
                boltTimer = 0;
            }
            return;
        }
        keepDistance(delta, world, 2.4f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            windup = WINDUP;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float step = MathUtils.sin(walkTime * 10f) * 0.025f;
        // legs
        d.rect(b, pos.x - 0.1f, bottom + step, 0.07f, 0.2f, c(JOINT));
        d.rect(b, pos.x + 0.03f, bottom - step, 0.07f, 0.2f, c(JOINT));
        // torso
        d.rect(b, pos.x - W / 2f, bottom + 0.18f, W, 0.26f, c(PLATE));
        d.rect(b, pos.x - W / 2f, bottom + 0.18f, 0.05f, 0.26f, c(PLATE_DARK));
        d.circle(b, pos.x, bottom + 0.32f, 0.04f, GLOW); // power core
        // arm with a blaster
        float face = facingRight() ? 1 : -1;
        float ax = pos.x + face * 0.17f;
        float ay = bottom + 0.3f;
        d.line(b, ax, ay, 0.18f, 0.06f, aimAngle, JOINT);
        // head with a visor
        float hy = bottom + 0.52f;
        d.rect(b, pos.x - 0.12f, hy - 0.08f, 0.24f, 0.17f, c(PLATE));
        d.rect(b, pos.x - 0.1f, hy - 0.03f, 0.2f, 0.07f, VISOR);
        Color eye = windup > 0 || boltsLeft > 0 ? BOLT : GLOW;
        d.rect(b, pos.x - 0.08f + face * 0.03f, hy - 0.01f, 0.13f, 0.03f, eye);
        d.rect(b, pos.x - 0.01f, hy + 0.09f, 0.02f, 0.06f, JOINT); // antenna
        if (windup > 0) {
            float k = 1f - windup / WINDUP;
            d.circle(b, pos.x + face * 0.02f, hy, 0.12f + 0.08f * k, BOLT, 0.25f * k);
        }
    }
}
