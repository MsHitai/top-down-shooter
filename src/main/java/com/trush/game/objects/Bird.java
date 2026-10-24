package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.trush.game.gfx.Draw;
import com.trush.game.world.Arena;

/**
 * The clouds' birds (the owner's idea): a moving obstacle to dodge, not to fight. A white arrow at the
 * room's edge warns where one will come in; then it flies straight across, flapping, and is gone once it
 * leaves the cloud. Touching it hurts; bullets pass by it. GameWorld spawns them during fights in the clouds.
 */
public class Bird {

    private static final Color BODY = Color.valueOf("fbfaff");
    private static final Color WING = Color.valueOf("d8deef");
    private static final Color TIP = Color.valueOf("5a6078");
    private static final Color BEAK = Color.valueOf("f2a03a");
    private static final Color WARN = Color.WHITE;

    /** The warning before it comes in. */
    public static final float WARN_TIME = 0.9f;
    public static final float RADIUS = 0.17f;
    /** How high above its shadow it flies. */
    private static final float HEIGHT = 0.45f;

    /** Its body, up in the air; it hurts the player at this spot (whose hitbox is up off the floor too). */
    public float x;
    public float y;
    private final float baseY;
    private final float dir;
    private final float speed;
    private float warn;
    private float clock = MathUtils.random(10f);
    public boolean alive = true;

    /** Comes in from the left ({@code dir} 1) or right (-1) edge of the room at height y, after a wait. */
    public Bird(float y, int dir, float speed, float delay) {
        Arena a = Arena.now;
        this.dir = dir;
        this.speed = speed;
        this.baseY = y;
        this.y = y;
        x = dir > 0 ? a.left - 0.4f : a.right + 0.4f;
        warn = WARN_TIME + delay;
    }

    /** Still waiting to come in (its warning arrow showing). */
    public boolean isWarning() {
        return warn > 0;
    }

    /** Returns true the moment it sets off (for the flap sound). */
    public boolean update(float delta) {
        clock += delta;
        if (warn > 0) {
            warn -= delta;
            return warn <= 0;
        }
        x += dir * speed * delta;
        y = baseY + MathUtils.sin(clock * 3f) * 0.12f;
        Arena a = Arena.now;
        if (dir > 0 ? x > a.right + 0.5f : x < a.left - 0.5f) {
            alive = false;
        }
        return false;
    }

    /** The shadow on the floor below it. */
    public void drawShadow(SpriteBatch b, Draw d) {
        if (warn <= 0) {
            d.ellipse(b, x, y - HEIGHT, 0.14f, 0.05f, Color.BLACK, 0.25f);
        }
    }

    public void draw(SpriteBatch b, Draw d, Rectangle view) {
        if (warn > 0) { // a white arrow at the edge it comes in from, kept on screen
            float edge = dir > 0 ? Arena.now.left + 0.15f : Arena.now.right - 0.15f;
            float ax = MathUtils.clamp(edge, view.x + 0.15f, view.x + view.width - 0.15f);
            float ay = MathUtils.clamp(baseY, view.y + 0.15f, view.y + view.height - 0.15f);
            float pulse = 0.5f + 0.5f * MathUtils.sin(clock * 16f);
            float angle = dir > 0 ? 0f : 180f;
            d.shape(b, d.triangle, ax, ay, 0.15f, angle, Draw.OUTLINE_COLOR, 0.6f + 0.3f * pulse);
            d.shape(b, d.triangle, ax, ay, 0.11f, angle, WARN, 0.6f + 0.4f * pulse);
            return;
        }
        float flap = MathUtils.sin(clock * 14f);
        // seen from the side: two wings beating up and down from the shoulders, the far one a shade darker
        float sx = x - dir * 0.02f;
        float sy = y + 0.03f;
        for (int far = 1; far >= 0; far--) {
            float tipX = -dir * 0.1f;
            float tipY = flap * 0.2f * (far == 1 ? 0.8f : 1f) + 0.04f;
            float len = (float) Math.sqrt(tipX * tipX + tipY * tipY);
            float ang = MathUtils.atan2Deg360(tipY, tipX);
            float wy = sy + far * 0.03f;
            d.line(b, sx, wy, len + 0.03f, 0.13f, ang, Draw.OUTLINE_COLOR);
            d.line(b, sx, wy, len, 0.095f, ang, far == 1 ? WING : BODY);
            d.circle(b, sx + MathUtils.cosDeg(ang) * len, wy + MathUtils.sinDeg(ang) * len, 0.04f, TIP);
        }
        d.ellipse(b, x, y, 0.19f, 0.1f, Draw.OUTLINE_COLOR, 1f);
        d.ellipse(b, x, y, 0.17f, 0.08f, BODY, 1f);
        d.rotRect(b, x - dir * 0.18f, y + 0.01f, 0.09f, 0.06f, 0f, WING, 1f); // tail
        d.circle(b, x + dir * 0.15f, y + 0.04f, 0.075f, Draw.OUTLINE_COLOR); // the head
        d.circle(b, x + dir * 0.15f, y + 0.04f, 0.06f, BODY);
        d.circle(b, x + dir * 0.17f, y + 0.06f, 0.018f, Draw.OUTLINE_COLOR); // an eye
        d.rotRect(b, x + dir * 0.235f, y + 0.03f, 0.07f, 0.03f, 0f, BEAK, 1f);
        // the near wing again over the body, so it beats in front of it
        float tipY = flap * 0.2f + 0.04f;
        float len = (float) Math.sqrt(0.01f + tipY * tipY);
        float ang = MathUtils.atan2Deg360(tipY, -dir * 0.1f);
        d.line(b, sx, sy, len, 0.08f, ang, BODY);
    }
}
