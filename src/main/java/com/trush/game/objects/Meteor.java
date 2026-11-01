package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;

/**
 * One of the Moon's meteor showers (a moon trick, like the clouds' birds): a red ring marks where it will
 * land, its shadow grows in the ring, then a rock streaks down out of the sky and crashes there. GameWorld
 * spawns them during fights on the Moon and blows up the spot when it lands. The Meteor Staff calls down
 * {@link #friendly} ones: a quicker golden ring, a blast only enemies feel.
 */
public class Meteor {

    private static final Color WARN = Color.valueOf("ff3b30");
    private static final Color ROCK = Color.valueOf("8a8496");
    private static final Color ROCK_LIGHT = Color.valueOf("b9b3c4");
    private static final Color CRATER = Color.valueOf("5c5668");
    private static final Color TAIL = Color.valueOf("ffb347");
    private static final Color TAIL_HOT = Color.valueOf("fff1c4");
    private static final Color WARN_FRIENDLY = Color.valueOf("ffd23a");
    /** A friendly one's warning: it's the hero's, so no need to dodge it - just quick to read. */
    public static final float FRIENDLY_WARN = 0.55f;

    /** From the ring showing up to the crash. */
    public static final float WARN_TIME = 1.3f;
    /** The last part of it, when the rock is in sight, streaking down. */
    private static final float FALL_TIME = 0.35f;
    /** How far round it hits. */
    public static final float RADIUS = 0.45f;
    /** Where it falls from: up and off to one side. */
    private static final float DROP = 3.5f;

    /** The spot it lands on. */
    public final float x;
    public final float y;
    private final float side;
    /** The hero's (the Meteor Staff): hurts enemies, never the hero; how much and how far round. */
    public final boolean friendly;
    public final float damage;
    public final float radius;
    private final float warn;
    private float timer;
    private float clock;
    public boolean alive = true;

    /** Lands on (x, y) after {@code delay} more than the warning. */
    public Meteor(float x, float y, float delay) {
        this(x, y, delay, false, 0f, RADIUS, WARN_TIME);
    }

    private Meteor(float x, float y, float delay, boolean friendly, float damage, float radius, float warn) {
        this.x = x;
        this.y = y;
        this.friendly = friendly;
        this.damage = damage;
        this.radius = radius;
        this.warn = warn;
        side = MathUtils.randomSign();
        timer = warn + delay;
    }

    /** The Meteor Staff's: lands on (x, y) soon, a blast of {@code damage} {@code radius} round. */
    public static Meteor friendly(float x, float y, float damage, float radius) {
        return new Meteor(x, y, 0f, true, damage, radius, FRIENDLY_WARN);
    }

    /** Still waiting before its ring shows (part of a shower coming one after another). */
    private boolean isWaiting() {
        return timer > warn;
    }

    /** Returns true the moment it hits the ground (GameWorld does the crash). */
    public boolean update(float delta) {
        clock += delta;
        timer -= delta;
        if (timer <= 0) {
            alive = false;
            return true;
        }
        return false;
    }

    /** The red ring on the floor and the shadow growing in it. */
    public void drawFloor(SpriteBatch b, Draw d) {
        if (isWaiting()) {
            return;
        }
        float k = 1f - timer / warn; // 0 when it shows up, 1 at the crash
        float pulse = 0.5f + 0.5f * MathUtils.sin(clock * (10f + 14f * k));
        Color ring = friendly ? WARN_FRIENDLY : WARN;
        d.ring(b, x, y, radius, ring, 0.45f + 0.4f * pulse);
        d.circle(b, x, y, radius, ring, 0.08f + 0.1f * k);
        d.ellipse(b, x, y, radius * 0.8f * k, radius * 0.5f * k, Color.BLACK, 0.35f * k);
    }

    /** The rock itself, streaking down in the last moments, over everything on the floor. */
    public void draw(SpriteBatch b, Draw d) {
        if (timer > FALL_TIME) {
            return;
        }
        float k = timer / FALL_TIME; // 1 up high, 0 on the ground
        float rx = x + side * DROP * 0.45f * k;
        float ry = y + DROP * k;
        float ang = MathUtils.atan2Deg360(DROP, side * DROP * 0.45f); // the way back up its path
        d.line(b, rx, ry, 1.1f, 0.2f, ang, friendly ? WARN_FRIENDLY : TAIL, 0.55f);
        d.line(b, rx, ry, 0.7f, 0.1f, ang, TAIL_HOT, 0.8f);
        d.circle(b, rx, ry, 0.24f, Draw.OUTLINE_COLOR);
        d.circle(b, rx, ry, 0.2f, ROCK);
        d.circle(b, rx - 0.05f, ry + 0.06f, 0.09f, ROCK_LIGHT);
        d.circle(b, rx + 0.07f, ry - 0.05f, 0.05f, CRATER);
    }
}
