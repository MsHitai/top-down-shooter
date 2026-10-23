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

    public Grenade(float x, float y, float angle, float distance) {
        pos.set(x, y);
        vel.set(MathUtils.cosDeg(angle), MathUtils.sinDeg(angle)).scl(distance / FLIGHT);
    }

    public boolean landed() {
        return time >= FLIGHT;
    }

    public boolean shouldExplode() {
        return time >= FLIGHT + FUSE;
    }

    public void draw(SpriteBatch b, Draw d) {
        float t = Math.min(1f, time / FLIGHT);
        float height = landed() ? 0f : 4f * t * (1f - t) * 0.6f;
        d.ellipse(b, pos.x, pos.y - 0.04f, 0.08f, 0.03f, Color.BLACK, 0.35f);
        float y = pos.y + height;
        d.circle(b, pos.x, y, 0.075f, BODY);
        d.rect(b, pos.x - 0.075f, y - 0.01f, 0.15f, 0.02f, DARK);
        d.rect(b, pos.x - 0.025f, y + 0.06f, 0.05f, 0.04f, DARK);
        if (landed() && ((int) (time * 16f)) % 2 == 0) { // fizzing fuse
            d.circle(b, pos.x + 0.03f, y + 0.11f, 0.03f, SPARK, 0.9f);
        }
    }
}
