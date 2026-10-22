package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.world.GameWorld;

/**
 * Town enemy: a sewer rat. Comes in packs, quick and fragile; doesn't shoot but scurries straight
 * at you in a zig-zag - touching it hurts. Runs around cover to get at you. Dodge-roll over it to squish it.
 */
public class Rat extends Enemy {

    private static final Color FUR = Color.valueOf("7a7078");
    private static final Color FUR_DARK = Color.valueOf("544c54");
    private static final Color PINK = Color.valueOf("e8a0a8");

    private final float zigPhase = MathUtils.random(MathUtils.PI2);
    private float time;

    public Rat(float x, float y) {
        super(x, y, 6f);
        radius = 0.13f;
        speed = 1.7f;
        score = 5;
        squishable = true;
    }

    @Override
    public float feetY() {
        return pos.y - 0.1f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        if (!canSeePlayer && flank(delta, world, speed)) {
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
        float scurry = MathUtils.sin(walkTime * 30f) * 0.01f;
        float y = pos.y + scurry;
        // tail
        d.line(b, pos.x - face * 0.12f, y - 0.03f, 0.16f, 0.02f, face > 0 ? 200f : -20f, PINK);
        // body and head
        d.circle(b, pos.x - face * 0.03f, y, 0.11f, c(FUR_DARK));
        d.circle(b, pos.x - face * 0.02f, y + 0.01f, 0.1f, c(FUR));
        d.circle(b, pos.x + face * 0.09f, y + 0.02f, 0.065f, c(FUR));
        d.circle(b, pos.x + face * 0.05f, y + 0.08f, 0.03f, c(PINK)); // ear
        d.circle(b, pos.x + face * 0.15f, y + 0.01f, 0.015f, PINK);   // nose
        d.circle(b, pos.x + face * 0.1f, y + 0.04f, 0.014f, Color.RED); // eye
    }
}
