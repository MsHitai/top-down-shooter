package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;

/**
 * A patch of something nasty on the floor, like ETG's goop: fire (a dynamite's blast, a flare) or acid (the
 * Sludger's globs, the acid guns). Walking through it sets you on fire / eats at your feet; a dodge roll
 * passes over it unharmed (GameWorld.updatePuddles). A player's own puddles only hurt enemies. Ice (a
 * Mountain Cube's trail, a shattered enemy, the Freeze Ray) doesn't hurt anyone: it's slippery - the hero
 * and walking enemies slide about on it. Fire and ice put each other out.
 */
public class Puddle {

    public enum Kind { FIRE, ACID, ICE }

    private static final Color FIRE_RED = Color.valueOf("c8341e");
    private static final Color FIRE_ORANGE = Color.valueOf("ff8a1e");
    private static final Color FIRE_YELLOW = Color.valueOf("ffd84a");
    private static final Color ACID_DARK = Color.valueOf("3c8a1e");
    private static final Color ACID = Color.valueOf("8ae83a");
    private static final Color ACID_FOAM = Color.valueOf("d8ff9a");
    private static final Color ICE_EDGE = Color.valueOf("7ec4e8");
    private static final Color ICE = Color.valueOf("cdeeff");
    /** The last second it shrinks away. */
    private static final float FADE = 1f;

    public final Kind kind;
    public final float x;
    public final float y;
    private final float radius;
    /** Made by the player (a gun of theirs): it never hurts them. */
    public final boolean friendly;
    public float life;
    /** Old acid that was there before you came (the Slums): it never dries up or gets pushed out by new spills. */
    public boolean old;
    private float age;
    private final float phase = MathUtils.random(MathUtils.PI2);

    public Puddle(Kind kind, float x, float y, float radius, float life, boolean friendly) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.life = life;
        this.friendly = friendly;
    }

    public void update(float delta) {
        if (!old) {
            life -= delta;
        }
        age += delta;
    }

    public boolean alive() {
        return life > 0;
    }

    /** How big it is now: it spreads out quickly when spilled and shrinks as it dries up / burns out. */
    public float radius() {
        return radius * Math.min(1f, age * 6f) * MathUtils.clamp(life / FADE, 0f, 1f);
    }

    /** True if a point (feet, an enemy's centre) is in it; puddles are flat ovals, a little wider than tall. */
    public boolean covers(float px, float py) {
        float r = radius();
        float dx = (px - x) / r;
        float dy = (py - y) / (r * 0.75f);
        return r > 0.02f && dx * dx + dy * dy < 1f;
    }

    public void draw(SpriteBatch b, Draw d) {
        float r = radius();
        if (r <= 0.01f) {
            return;
        }
        if (kind == Kind.FIRE) { // scorched glow, then flames licking up all over it
            d.ellipse(b, x, y, r, r * 0.75f, FIRE_RED, 0.45f);
            d.ellipse(b, x, y, r * 0.7f, r * 0.5f, FIRE_ORANGE, 0.35f);
            int flames = Math.max(3, (int) (r * 14f));
            for (int i = 0; i < flames; i++) {
                float a = phase + i * 2.39996f; // golden angle: spread evenly over the patch
                float k = (float) Math.sqrt((i + 0.5f) / flames);
                float fx = x + MathUtils.cos(a) * r * 0.8f * k;
                float fy = y + MathUtils.sin(a) * r * 0.6f * k;
                float flick = 0.5f + 0.5f * MathUtils.sin(age * 13f + i * 1.7f);
                float h = 0.06f + 0.06f * flick;
                d.ellipse(b, fx, fy + h * 0.5f, 0.035f, h, FIRE_ORANGE, 0.85f);
                d.ellipse(b, fx, fy + h * 0.35f, 0.018f, h * 0.55f, FIRE_YELLOW, 0.9f);
            }
        } else if (kind == Kind.ICE) { // a glossy sheet of ice with a few bright glints
            d.ellipse(b, x, y, r, r * 0.75f, ICE_EDGE, 0.55f);
            d.ellipse(b, x, y + r * 0.04f, r * 0.88f, r * 0.64f, ICE, 0.55f);
            for (int i = 0; i < 3; i++) {
                float a = phase + i * 2.1f;
                float gx = x + MathUtils.cos(a) * r * 0.45f;
                float gy = y + MathUtils.sin(a) * r * 0.3f;
                float glint = 0.5f + 0.5f * MathUtils.sin(age * 2.5f + i * 2f);
                d.line(b, gx - r * 0.12f, gy - r * 0.06f, r * 0.28f, 0.015f, 25f, Color.WHITE, 0.4f + 0.5f * glint);
            }
        } else { // a bubbling green pool
            d.ellipse(b, x, y, r, r * 0.75f, ACID_DARK, 0.7f);
            d.ellipse(b, x, y + r * 0.05f, r * 0.85f, r * 0.6f, ACID, 0.6f);
            for (int i = 0; i < 4; i++) {
                float t = (age * 0.9f + i * 0.25f + phase) % 1f;
                float a = phase * 3f + i * 1.9f;
                float bx = x + MathUtils.cos(a) * r * 0.55f;
                float by = y + MathUtils.sin(a) * r * 0.4f;
                d.circle(b, bx, by, 0.015f + 0.03f * t, ACID_FOAM, 0.8f * (1f - t));
            }
        }
    }
}
