package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * A floating shape with a face (town and later): it morphs circle - square - triangle - star, and
 * each shape fires its own volley in its own bullets - a ring of balls, a hollow square that flies as
 * one block, a solid triangle tip-first, and a five-pointed star burst.
 */
public class Shapeshifter extends Enemy {

    private enum Form { CIRCLE, SQUARE, TRIANGLE, STAR }

    private static final Color[] COLORS = {
            Color.valueOf("4fc3f7"), Color.valueOf("7bed6f"), Color.valueOf("ffb347"), Color.valueOf("c77dff")};
    private static final Color GLOW = Color.WHITE;
    private static final float WINDUP = 0.6f;
    private static final float MORPH = 0.35f;

    private Form form = Form.values()[MathUtils.random(3)];
    private float fireTimer = MathUtils.random(1.5f, 2.5f);
    private float windup;
    /** 1 -> 0 while it changes into its next form. */
    private float morph;
    private float time = MathUtils.random(10f);

    public Shapeshifter(float x, float y) {
        super(x, y, 30f);
        radius = 0.2f;
        speed = 0.8f;
        score = 40;
        flying = true; // it floats: over furniture and pits
    }

    @Override
    public float feetY() {
        return pos.y - 0.32f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        morph = Math.max(0f, morph - delta / MORPH);
        float diff = world.getDifficulty();
        if (windup > 0) {
            windup -= delta;
            if (windup <= 0) {
                fire(world, diff);
                form = Form.values()[(form.ordinal() + 1) % 4]; // and change into the next shape
                morph = 1f;
                fireTimer = MathUtils.random(1.8f, 2.6f) / diff;
            }
            return;
        }
        keepDistance(delta, world, 2.6f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            windup = WINDUP;
        }
    }

    private void fire(GameWorld world, float diff) {
        Color col = COLORS[form.ordinal()];
        switch (form) {
            case CIRCLE: {
                int n = diff > 1.2f ? 14 : 12;
                float start = MathUtils.random(360f);
                for (int i = 0; i < n; i++) {
                    shoot(world, start + 360f * i / n, 2.1f * diff).color.set(col);
                }
                break;
            }
            case SQUARE:
                Patterns.square(world, pos.x, pos.y, aimAngle, 2.0f * diff, 4, 0.2f, Bullet.Shape.SQUARE, col);
                break;
            case TRIANGLE:
                Patterns.triangle(world, pos.x + MathUtils.cosDeg(aimAngle) * 0.3f, pos.y + MathUtils.sinDeg(aimAngle) * 0.3f,
                        aimAngle, 2.2f * diff, 4, 0.2f, Bullet.Shape.TRIANGLE, col);
                break;
            case STAR:
                for (int arm = 0; arm < 5; arm++) {
                    float a = aimAngle + arm * 72f;
                    for (int k = 0; k < 3; k++) {
                        Bullet b = shoot(world, a, (1.5f + 0.45f * k) * diff);
                        b.shape = Bullet.Shape.STAR;
                        b.color.set(col);
                    }
                }
                break;
        }
        world.sfx().play("zap", 0.5f);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bob = MathUtils.sin(time * 3f) * 0.04f;
        float cx = pos.x;
        float cy = pos.y + bob;
        Color col = COLORS[form.ordinal()];
        float pulse = windup > 0 ? 1f + 0.15f * MathUtils.sin(time * 30f) : 1f;
        float r = 0.2f * pulse * (1f - 0.5f * morph);
        float spin = morph * 180f;
        if (windup > 0) {
            d.circle(b, cx, cy, r * 1.9f, col, 0.2f + 0.3f * (1f - windup / WINDUP));
        }
        switch (form) {
            case CIRCLE:
                d.circle(b, cx, cy, r * 1.15f, Draw.OUTLINE_COLOR);
                d.circle(b, cx, cy, r, c(col));
                break;
            case SQUARE:
                d.rotRect(b, cx, cy, r * 2f, r * 2f, spin, Draw.OUTLINE_COLOR, 1f);
                d.rotRect(b, cx, cy, r * 1.7f, r * 1.7f, spin, c(col), 1f);
                break;
            case TRIANGLE:
                d.shape(b, d.triangle, cx, cy - r * 0.1f, r * 1.5f, 90f + spin, Draw.OUTLINE_COLOR, 1f);
                d.shape(b, d.triangle, cx, cy - r * 0.1f, r * 1.25f, 90f + spin, c(col), 1f);
                break;
            case STAR:
                d.shape(b, d.star, cx, cy, r * 1.5f, spin, Draw.OUTLINE_COLOR, 1f);
                d.shape(b, d.star, cx, cy, r * 1.25f, spin, c(col), 1f);
                break;
        }
        d.circle(b, cx - r * 0.35f, cy + r * 0.4f, r * 0.18f, GLOW, 0.7f);
        drawEyes(b, d, cx, cy - r * 0.05f, r * 0.3f, r * 0.2f);
    }
}
