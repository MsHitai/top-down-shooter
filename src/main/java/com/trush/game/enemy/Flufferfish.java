package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * Sea enemy: the Flufferfish, our pufferfish (the owner's son's). A round fluffy fish that drifts about
 * until it gets cross: it puffs itself up (the warning - its fluff stands up into spikes) and shoots a ring
 * of needles all round, then stays big and slow for a moment before it shrinks back. On harder floors a
 * second ring follows, turned to fill the gaps of the first.
 */
public class Flufferfish extends Enemy {

    private static final Color BODY = Color.valueOf("ffd36a");
    private static final Color BODY_DARK = Color.valueOf("e0a83a");
    private static final Color FLUFF = Color.valueOf("fff0c0");
    private static final Color SPOT = Color.valueOf("e8903a");
    private static final Color FIN = Color.valueOf("ff9a5a");
    private static final Color NEEDLE = Color.valueOf("fff6d0");

    private enum State { SWIM, PUFF, PUFFED }

    private static final float PUFF_TIME = 0.6f;
    private static final float PUFFED_TIME = 0.9f;
    private static final float SMALL = 0.17f;
    private static final float BIG = 0.28f;
    private static final int NEEDLES = 12;

    private State state = State.SWIM;
    private float timer = MathUtils.random(1.5f, 2.5f);
    /** 0 = its usual size, 1 = puffed up all the way. */
    private float puff;
    private boolean secondRing;
    private float time = MathUtils.random(10f);

    public Flufferfish(float x, float y) {
        super(x, y, 22f);
        radius = SMALL;
        speed = 0.8f;
        score = 30;
    }

    @Override
    public float feetY() {
        return pos.y - 0.24f; // it swims a little above the sea bed
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        float diff = world.getDifficulty();
        timer -= delta;
        switch (state) {
            case SWIM:
                puff = Math.max(0f, puff - delta * 3f);
                keepDistance(delta, world, 2.0f, speed);
                if (timer <= 0 && canShoot(world)) {
                    state = State.PUFF;
                    timer = PUFF_TIME / Math.min(1.2f, diff);
                    world.sfx().play("pop", 0.4f);
                }
                break;
            case PUFF:
                puff = Math.min(1f, puff + delta / (PUFF_TIME / Math.min(1.2f, diff)));
                if (timer <= 0) {
                    needles(world, diff, 0f);
                    state = State.PUFFED;
                    timer = PUFFED_TIME;
                    secondRing = diff > 1.3f;
                }
                break;
            default: // PUFFED: big and slow, a sitting duck - and maybe one more ring
                puff = 1f;
                keepDistance(delta, world, 2.0f, speed * 0.3f);
                if (secondRing && timer < PUFFED_TIME - 0.35f) {
                    secondRing = false;
                    needles(world, diff, 180f / NEEDLES);
                }
                if (timer <= 0) {
                    state = State.SWIM;
                    timer = MathUtils.random(2.2f, 3.2f) / diff;
                }
                break;
        }
        radius = SMALL + (BIG - SMALL) * puff; // a bigger fish is a bigger target
    }

    private void needles(GameWorld world, float diff, float offset) {
        float start = aimAngle + offset;
        for (int i = 0; i < NEEDLES; i++) {
            Bullet b = shoot(world, start + i * 360f / NEEDLES, 2.3f * diff);
            b.streak = 0.14f;
            b.radius = 0.045f;
            b.color.set(NEEDLE);
        }
        world.sfx().play("swish", 0.5f);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float face = facingRight() ? 1f : -1f;
        float r = SMALL + (BIG - SMALL) * puff;
        float x = pos.x;
        float y = pos.y + MathUtils.sin(time * 3f) * 0.025f;
        // tail fin, flapping
        float flap = MathUtils.sin(time * 10f) * 15f;
        d.rotRect(b, x - face * (r + 0.05f), y, 0.12f, 0.14f, 45f + flap, Draw.OUTLINE_COLOR, 1f);
        d.rotRect(b, x - face * (r + 0.05f), y, 0.09f, 0.11f, 45f + flap, c(FIN), 1f);
        // the fluff all round: soft tufts, standing up into needles as it puffs
        for (int i = 0; i < 14; i++) {
            float a = i * 360f / 14f + time * 8f;
            if (puff > 0.3f) {
                d.line(b, x + MathUtils.cosDeg(a) * r * 0.9f, y + MathUtils.sinDeg(a) * r * 0.9f, 0.04f + 0.07f * puff,
                        0.022f, a, c(NEEDLE));
            } else {
                d.circle(b, x + MathUtils.cosDeg(a) * r, y + MathUtils.sinDeg(a) * r, 0.045f, c(FLUFF));
            }
        }
        d.circle(b, x, y, r + 0.015f, Draw.OUTLINE_COLOR);
        d.circle(b, x, y, r, c(BODY));
        d.circle(b, x - face * r * 0.2f, y - r * 0.35f, r * 0.55f, c(FLUFF)); // pale belly
        d.circle(b, x - face * r * 0.45f, y + r * 0.35f, r * 0.14f, c(SPOT));
        d.circle(b, x - face * r * 0.1f, y + r * 0.55f, r * 0.11f, c(SPOT));
        d.circle(b, x + face * r * 0.75f, y - r * 0.25f, r * 0.12f, c(BODY_DARK)); // a little pursed mouth
        d.rotRect(b, x - face * r * 0.1f, y - r * 0.05f, 0.08f, 0.05f, face * -30f, c(FIN), 1f); // side fin
        drawEyes(b, d, x + face * r * 0.35f, y + r * 0.2f, r * 0.22f, 0.035f + 0.01f * puff);
        if (puff > 0.5f) { // cross eyebrows
            for (int i = -1; i <= 1; i += 2) {
                float ex = x + face * r * 0.35f + i * r * 0.22f + face * r * 0.11f;
                d.rotRect(b, ex, y + r * 0.42f, 0.07f, 0.02f, i * face * -20f, Draw.OUTLINE_COLOR, 1f);
            }
        }
    }
}
