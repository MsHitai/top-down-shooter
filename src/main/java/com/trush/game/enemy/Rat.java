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
        d.rats.draw(b, pos.x, pos.y, size(), walkTime, facingRight(), marked, Marked.fade,
                flash > 0, c(Color.WHITE));
    }
}
