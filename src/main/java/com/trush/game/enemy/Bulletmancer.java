package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * A robed caster that hangs back and summons slow rings of bullets that speed up.
 * This is the "bullet-hell" pressure enemy: rings force you to find the gaps or dodge roll.
 */
public class Bulletmancer extends Enemy {

    private static final Color ROBE = Color.valueOf("3f4fb5");
    private static final Color ROBE_DARK = Color.valueOf("2a347a");
    private static final Color HAT = Color.valueOf("6c3fb5");
    private static final Color ORB = Color.valueOf("b388ff");
    private static final Color BULLET = Color.valueOf("c77dff");

    private static final float W = 0.38f;
    private static final float H = 0.6f;
    private static final float CHARGE = 0.8f;

    private float castTimer = MathUtils.random(1.5f, 2.5f);
    private float charge;
    private int castCount;
    private float time;

    public Bulletmancer(float x, float y) {
        super(x, y, 32f);
        radius = 0.21f;
        speed = 0.5f;
        score = 40;
    }

    @Override
    public float feetY() {
        return pos.y - H / 2f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        if (charge > 0) {
            charge -= delta;
            if (charge <= 0) {
                cast(world);
            }
            return;
        }
        keepDistance(delta, world, 2.8f, speed);
        castTimer -= delta;
        if (castTimer <= 0) {
            charge = CHARGE;
            castTimer = MathUtils.random(2.8f, 3.6f) / world.getDifficulty();
        }
    }

    private void cast(GameWorld world) {
        castCount++;
        int n = world.getDifficulty() > 1.2f ? 16 : 13;
        float offset = castCount % 2 == 0 ? 0 : 180f / n + aimAngle;
        boolean curving = castCount % 3 == 0;
        for (int i = 0; i < n; i++) {
            Bullet b = shoot(world, offset + 360f * i / n, 0.9f);
            b.accel = 1.4f * world.getDifficulty();
            b.maxSpeed = 3.2f * world.getDifficulty();
            b.color.set(BULLET);
            if (curving) {
                b.curve = 35f;
            }
        }
        world.sfx().play("enemy_shoot", 0.8f);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float hover = MathUtils.sin(time * 3f) * 0.02f;
        float left = pos.x - W / 2f;
        // robe: stacked rectangles narrowing upwards
        d.rect(b, left, bottom + hover, W, H * 0.35f, c(ROBE_DARK));
        d.rect(b, left + 0.02f, bottom + H * 0.3f + hover, W - 0.04f, H * 0.3f, c(ROBE));
        d.circle(b, pos.x, bottom + H * 0.66f + hover, W * 0.38f, c(ROBE));
        // hat
        d.rect(b, pos.x - W * 0.45f, bottom + H * 0.78f + hover, W * 0.9f, 0.04f, c(HAT));
        d.rect(b, pos.x - W * 0.25f, bottom + H * 0.82f + hover, W * 0.5f, 0.08f, c(HAT));
        d.rect(b, pos.x - W * 0.12f, bottom + H * 0.9f + hover, W * 0.24f, 0.07f, c(HAT));
        drawEyes(b, d, pos.x, bottom + H * 0.66f + hover, 0.055f, 0.034f);

        float orbX = pos.x + (facingRight() ? 0.24f : -0.24f);
        float orbY = bottom + H * 0.45f + hover;
        float glow = charge > 0 ? 1f - charge / CHARGE : 0f;
        d.circle(b, orbX, orbY, 0.07f + 0.12f * glow, ORB, 0.25f + 0.4f * glow);
        d.circle(b, orbX, orbY, 0.045f, c(ORB));
        d.circle(b, orbX, orbY, 0.02f, Color.WHITE);
    }
}
