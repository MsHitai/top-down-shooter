package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.world.GameWorld;

/**
 * Town enemy: a sewer rat. Comes in packs, quick and fragile; doesn't shoot but scurries straight
 * at you in a zig-zag - touching it hurts. Runs around cover to get at you. Dodge-roll over it to squish it.
 * A marked rat ({@link #marked}, the Slums - the Skunk King's stink got to it) has grown on it: half as big
 * again, quicker and tougher, with a green ear and glowing eyes - and too big to squish with a roll.
 */
public class Rat extends Enemy {

    private static final Color FUR = Color.valueOf("7a7078");
    private static final Color FUR_DARK = Color.valueOf("544c54");
    private static final Color PINK = Color.valueOf("e8a0a8");

    /** How much bigger a marked rat is than a town one. */
    static final float MARKED_SIZE = 1.5f;

    private final float zigPhase = MathUtils.random(MathUtils.PI2);
    private float time;
    private boolean marked;

    /** A rat the Skunk King's stink has marked (see Marked). */
    public static Rat marked(float x, float y) {
        Rat r = new Rat(x, y);
        r.marked = true;
        r.hp = r.maxHp = 12f;
        r.speed = 2f;
        r.score = 10;
        r.radius = 0.13f * MARKED_SIZE;
        r.squishable = false; // (the owner's call: a roll just goes over it)
        return r;
    }

    public boolean isMarked() {
        return marked;
    }

    public Rat(float x, float y) {
        super(x, y, 6f);
        radius = 0.13f;
        speed = 1.7f;
        score = 5;
        squishable = true;
    }

    @Override
    public String dexId() {
        return marked ? "MarkedRat" : "Rat";
    }

    @Override
    public float feetY() {
        return pos.y - 0.1f * size();
    }

    private float size() {
        return marked ? MARKED_SIZE : 1f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        if (flank(delta, world, speed)) {
            return;
        }
        // zig-zag toward the player
        float a = aimAngle + MathUtils.sin(time * 9f + zigPhase) * 35f;
        pos.add(MathUtils.cosDeg(a) * speed * delta, MathUtils.sinDeg(a) * speed * delta);
        walkTime += delta;
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float face = facingRight() ? 1 : -1;
        float k = size();
        float scurry = MathUtils.sin(walkTime * 30f) * 0.01f * k;
        float y = pos.y + scurry;
        // tail
        d.line(b, pos.x - face * 0.12f * k, y - 0.03f * k, 0.16f * k, 0.02f * k, face > 0 ? 200f : -20f, PINK);
        // body and head
        d.circle(b, pos.x - face * 0.03f * k, y, 0.11f * k, c(FUR_DARK));
        d.circle(b, pos.x - face * 0.02f * k, y + 0.01f * k, 0.1f * k, c(FUR));
        d.circle(b, pos.x + face * 0.09f * k, y + 0.02f * k, 0.065f * k, c(FUR));
        if (marked) { // the mark: a green ear and a glowing eye
            Marked.ear(b, d, pos.x + face * 0.05f * k, y + 0.06f * k, 0.09f * k, -face * 20f);
        } else {
            d.circle(b, pos.x + face * 0.05f * k, y + 0.08f * k, 0.03f * k, c(PINK)); // ear
        }
        d.circle(b, pos.x + face * 0.15f * k, y + 0.01f * k, 0.015f * k, PINK);   // nose
        if (marked) {
            Marked.eye(b, d, pos.x + face * 0.1f * k, y + 0.04f * k, 0.016f * k, time);
        } else {
            d.circle(b, pos.x + face * 0.1f * k, y + 0.04f * k, 0.014f * k, Color.RED); // eye
        }
    }
}
