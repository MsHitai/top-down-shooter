package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;

/**
 * The Grenade item: thrown in an arc toward where you aim, lands, fizzes for a moment and blows up
 * (GameWorld.updateGrenades does the moving and the boom). It never hurts the player.
 */
public class Grenade {

    private static final Color BODY = Color.valueOf("4a7a3a");
    private static final Color DARK = Color.valueOf("2c4a24");
    private static final Color SPARK = Color.valueOf("ffd23f");
    private static final Color SHELL = Color.valueOf("f5ecd0");
    private static final Color OUTLINE_EGG = Color.valueOf("2a0a14");

    /** Seconds in the air, then on the floor until it goes off. */
    public static final float FLIGHT = 0.55f;
    public static final float FUSE = 0.45f;
    public static final float RADIUS = 1.4f;
    public static final float DAMAGE = 40f;

    /** Where it is on the floor (the shadow); the arc is drawn above it. */
    public final Vector2 pos = new Vector2();
    public final Vector2 vel = new Vector2();
    public float time;
    public boolean alive = true;
    public float radius = RADIUS;
    public float damage = DAMAGE;
    private float fuse = FUSE;
    /** An egg from the Egg Lobber: looks like an egg and cracks open almost as soon as it lands. */
    private boolean egg;

    public Grenade(float x, float y, float angle, float distance) {
        pos.set(x, y);
        vel.set(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle)).scl(distance / FLIGHT);
    }

    /** The Egg Lobber's shot: a smaller blast that goes off right as it lands. */
    public static Grenade egg(float x, float y, float angle, float distance, float damage, float radius) {
        Grenade g = new Grenade(x, y, angle, distance);
        g.egg = true;
        g.damage = damage;
        g.radius = radius;
        g.fuse = 0.08f;
        return g;
    }

    public boolean landed() {
        return time >= FLIGHT;
    }

    public boolean shouldExplode() {
        return time >= FLIGHT + fuse;
    }

    public void draw(SpriteBatch b, Draw d) {
        float t = Math.min(1f, time / FLIGHT);
        float height = landed() ? 0f : 4f * t * (1f - t) * 0.6f;
        d.ellipse(b, pos.x, pos.y - 0.04f, 0.08f, 0.03f, Color.BLACK, 0.35f);
        float y = pos.y + height;
        if (egg) {
            d.ellipse(b, pos.x, y, 0.065f, 0.085f, OUTLINE_EGG, 1f);
            d.ellipse(b, pos.x, y, 0.05f, 0.07f, SHELL, 1f);
            d.circle(b, pos.x - 0.015f, y + 0.025f, 0.015f, Color.WHITE, 0.9f);
            return;
        }
        d.circle(b, pos.x, y, 0.075f, BODY);
        d.rect(b, pos.x - 0.075f, y - 0.01f, 0.15f, 0.02f, DARK);
        d.rect(b, pos.x - 0.025f, y + 0.06f, 0.05f, 0.04f, DARK);
        if (landed() && ((int) (time * 16f)) % 2 == 0) { // fizzing fuse
            d.circle(b, pos.x + 0.03f, y + 0.11f, 0.03f, SPARK, 0.9f);
        }
    }
}
