package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;

/**
 * Granny's cat (player/Hero.GRANNY), a companion like ETG's dog: it pads along at her heels, now and
 * then pounces on the nearest enemy and bites, and after a fight it sometimes brings her a present
 * (GameWorld picks the target and the present). Also sits by granny's feet in the living room.
 */
public class Cat {

    private static final Color FUR = Color.valueOf("e8964a");
    private static final Color STRIPE = Color.valueOf("b8662a");
    private static final Color BELLY = Color.valueOf("f8dcb0");
    private static final Color NOSE = Color.valueOf("ff8aa0");

    public static final float COOLDOWN = 2.8f;
    public static final float RANGE = 3.6f;
    /** A nip, not a kill: a Bullet Kin (10.5 hp) takes three bites, a Red Shotgun Kin (18) five. */
    public static final float DAMAGE = 3.5f;
    /** A present after a fight, this often. */
    public static final float GIFT_CHANCE = 0.35f;
    private static final float WALK_SPEED = 3.2f;
    private static final float LEAP_SPEED = 7.5f;

    private enum State { FOLLOW, LEAP, BACK }

    public final Vector2 pos = new Vector2();
    private final Vector2 goal = new Vector2();
    private State state = State.FOLLOW;
    public float cooldown = COOLDOWN;
    private float clock;
    private boolean facingRight = true;
    private boolean moving;
    /** How long it has stood still: it only sits down after {@link #SIT_DELAY} (no flicker between poses). */
    private float still;
    private static final float SIT_DELAY = 0.3f;
    private boolean placed;

    /** Follows a spot just behind granny's feet (or runs back there after a pounce). */
    public void update(float delta, float feetX, float feetY, boolean heroFacingRight) {
        clock += delta;
        cooldown -= delta;
        float tx = feetX + (heroFacingRight ? -0.38f : 0.38f);
        float ty = feetY - 0.05f;
        if (!placed) {
            pos.set(tx, ty);
            placed = true;
        }
        if (state == State.LEAP) {
            moving = true;
            still = 0f;
            return; // GameWorld steers the leap (leapTo) and lands it
        }
        float d = pos.dst(tx, ty);
        float speed = state == State.BACK ? WALK_SPEED * 1.4f : WALK_SPEED;
        // starts walking once she's a step away, keeps walking until it's caught up (hysteresis, so it
        // doesn't flip between walking and sitting every frame while trailing her)
        moving = state == State.BACK || moving ? d > 0.1f : d > 0.35f;
        if (moving) {
            still = 0f;
            float step = Math.min(d, speed * delta);
            if (Math.abs(tx - pos.x) > 0.04f) facingRight = tx > pos.x; // not on every wobble of a vertical walk
            pos.add((tx - pos.x) / d * step, (ty - pos.y) / d * step);
        } else {
            still += delta;
            if (state == State.BACK) state = State.FOLLOW;
        }
    }

    public boolean isLeaping() {
        return state == State.LEAP;
    }

    public void pounce() {
        state = State.LEAP;
        cooldown = COOLDOWN;
    }

    /** One frame of a leap toward (x, y); true once it's there. */
    public boolean leapTo(float x, float y, float delta) {
        float d = pos.dst(x, y);
        facingRight = x > pos.x;
        if (d < LEAP_SPEED * delta + 0.15f) {
            pos.set(x, y);
            return true;
        }
        pos.add((x - pos.x) / d * LEAP_SPEED * delta, (y - pos.y) / d * LEAP_SPEED * delta);
        return false;
    }

    /** Done with that enemy (bitten, or gone): back to granny. */
    public void comeBack() {
        state = State.BACK;
    }

    /** Snaps to granny (a new room). */
    public void reset() {
        placed = false;
        state = State.FOLLOW;
    }

    public float sortY() {
        return pos.y;
    }

    /** A small orange tabby seen from the side: sitting, walking or mid-leap with its legs stretched out. */
    public void draw(SpriteBatch b, Draw d) {
        float x = pos.x;
        float hop = state == State.LEAP ? 0.12f + 0.04f * MathUtils.sin(clock * 20f) : 0f;
        float y = pos.y + hop;
        float s = facingRight ? 1f : -1f;
        d.ellipse(b, pos.x, pos.y, 0.14f, 0.04f, Color.BLACK, 0.3f);
        boolean sitting = !moving && still >= SIT_DELAY;
        float step = moving ? MathUtils.sin(clock * 16f) * 0.03f : 0f;
        // tail
        float wag = MathUtils.sin(clock * (sitting ? 2f : 8f)) * 0.04f;
        d.circle(b, x - s * 0.15f, y + 0.12f + wag, 0.035f, Draw.OUTLINE_COLOR);
        d.circle(b, x - s * 0.19f, y + 0.19f + wag, 0.035f, Draw.OUTLINE_COLOR);
        d.circle(b, x - s * 0.15f, y + 0.12f + wag, 0.025f, FUR);
        d.circle(b, x - s * 0.19f, y + 0.19f + wag, 0.025f, STRIPE);
        // legs
        if (!sitting) {
            for (int i = -1; i <= 1; i += 2) {
                d.rect(b, x + i * 0.07f - 0.02f + step * i, y, 0.04f, 0.07f, STRIPE);
            }
        }
        // body
        float bodyY = sitting ? y + 0.1f : y + 0.09f;
        d.ellipse(b, x, bodyY, sitting ? 0.1f : 0.14f, sitting ? 0.1f : 0.07f, Draw.OUTLINE_COLOR, 1f);
        d.ellipse(b, x, bodyY, sitting ? 0.085f : 0.125f, sitting ? 0.085f : 0.057f, FUR, 1f);
        d.rect(b, x - 0.02f, bodyY - 0.02f, 0.025f, 0.06f, STRIPE);
        d.rect(b, x + 0.035f * s - 0.01f, bodyY - 0.02f, 0.02f, 0.05f, STRIPE);
        d.ellipse(b, x + s * 0.02f, bodyY - 0.03f, 0.05f, 0.03f, BELLY, 1f);
        // head
        float hx = x + s * (sitting ? 0.04f : 0.12f);
        float hy = bodyY + (sitting ? 0.11f : 0.06f);
        d.circle(b, hx, hy, 0.075f, Draw.OUTLINE_COLOR);
        for (int i = -1; i <= 1; i += 2) { // ears
            d.shape(b, d.triangle, hx + i * 0.04f, hy + 0.06f, 0.045f, 90f, Draw.OUTLINE_COLOR, 1f);
            d.shape(b, d.triangle, hx + i * 0.04f, hy + 0.055f, 0.03f, 90f, FUR, 1f);
        }
        d.circle(b, hx, hy, 0.062f, FUR);
        d.circle(b, hx + s * 0.02f, hy - 0.015f, 0.03f, BELLY);
        d.circle(b, hx + s * 0.035f, hy + 0.012f, 0.011f, Draw.OUTLINE_COLOR); // eye
        d.circle(b, hx - s * 0.005f, hy + 0.012f, 0.011f, Draw.OUTLINE_COLOR);
        d.circle(b, hx + s * 0.05f, hy - 0.01f, 0.009f, NOSE);
    }
}
