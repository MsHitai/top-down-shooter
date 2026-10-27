package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;

/**
 * The Chomper's bite, like ETG's shark gun: where the shark gets its teeth into an enemy a big shark head
 * rears up with its jaws wide, snaps them shut over it and lets go. A killing bite is bigger and the shark
 * thrashes its catch before it lets go.
 */
public class Bite {
    private static final Color OUTLINE = Color.valueOf("2a0a14");
    private static final Color BELLY = Color.valueOf("eef4ff");
    private static final Color MOUTH = Color.valueOf("8a1f3a");
    /** Jaws opening, then the snap. */
    private static final float OPEN = 0.12f;
    private static final float SNAP = 0.06f;
    private static final float HOLD = 0.2f;
    /** A killing bite holds on longer, thrashing. */
    private static final float KILL_HOLD = 0.45f;
    private static final float FADE = 0.15f;

    public final float x;
    public final float y;
    private final float angle;
    private final float size;
    public final boolean kill;
    private final Color color = new Color();
    private float age;

    /**
     * @param angle the way the shark was swimming
     * @param size  roughly the bitten enemy's radius
     */
    public Bite(float x, float y, float angle, float size, boolean kill, Color color) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.size = MathUtils.clamp(size, 0.16f, 0.45f) * (kill ? 1.3f : 1f);
        this.kill = kill;
        this.color.set(color);
    }

    private float hold() {
        return kill ? KILL_HOLD : HOLD;
    }

    /** True the moment the jaws snap shut this frame. */
    public boolean update(float delta) {
        boolean before = age < OPEN + SNAP;
        age += delta;
        return before && age >= OPEN + SNAP;
    }

    public boolean isDone() {
        return age >= OPEN + SNAP + hold() + FADE;
    }

    public void draw(SpriteBatch b, Draw d) {
        float s = size;
        float gape; // half the angle between the jaws
        float scale;
        if (age < OPEN) { // rears up, jaws opening wide
            float k = age / OPEN;
            gape = 42f * k;
            scale = 0.6f + 0.4f * k;
        } else if (age < OPEN + SNAP) { // snap!
            gape = 42f * (1f - (age - OPEN) / SNAP);
            scale = 1f;
        } else {
            gape = 0f;
            scale = 1f;
        }
        float held = age - OPEN - SNAP;
        float alpha = held > hold() ? 1f - (held - hold()) / FADE : 1f;
        float a = angle;
        if (kill && held > 0 && held < hold()) { // shakes its catch
            a += MathUtils.sin(held * 45f) * 16f * (1f - held / hold());
        }
        s *= scale;
        float ux = MathUtils.cosDeg(a);
        float uy = MathUtils.sinDeg(a);
        // the hinge sits behind the target; the jaws reach past it
        float hx = x - ux * s * 1.1f;
        float hy = y - uy * s * 1.1f;
        float len = s * 2.2f;
        float thick = s * 0.55f;
        // head and body behind the hinge, fin on top
        d.circle(b, hx - ux * s * 0.5f, hy - uy * s * 0.5f, s * 0.95f, OUTLINE, alpha);
        d.circle(b, hx - ux * s * 0.5f, hy - uy * s * 0.5f, s * 0.8f, color, alpha);
        d.shape(b, d.triangle, hx - ux * s * 0.7f - uy * s * 0.85f, hy - uy * s * 0.7f + ux * s * 0.85f, s * 0.45f,
                a + 90f, color, alpha);
        if (gape > 1f) { // the inside of the mouth, between the jaws
            float mx = hx + ux * len * 0.35f;
            float my = hy + uy * len * 0.35f;
            d.circle(b, mx, my, s * 0.9f * MathUtils.sinDeg(gape) + s * 0.15f, MOUTH, alpha);
        }
        for (int side = -1; side <= 1; side += 2) { // upper jaw (+1), lower jaw (-1)
            float ja = a + side * gape;
            float jx = MathUtils.cosDeg(ja);
            float jy = MathUtils.sinDeg(ja);
            float cx = hx + jx * len / 2f;
            float cy = hy + jy * len / 2f;
            // the jaw's outside edge sits away from the mouth
            float ox = -jy * side * thick * 0.35f;
            float oy = jx * side * thick * 0.35f;
            d.rotRect(b, cx + ox, cy + oy, len + 0.04f, thick + 0.04f, ja, OUTLINE, alpha);
            d.rotRect(b, cx + ox, cy + oy, len, thick, ja, side < 0 ? BELLY : color, alpha);
            for (int t = 0; t < 4; t++) { // teeth along the inside, pointing at the other jaw
                float along = len * (0.3f + 0.18f * t);
                d.shape(b, d.triangle, hx + jx * along, hy + jy * along, s * 0.16f, ja - side * 90f, Color.WHITE, alpha);
            }
            if (side > 0) { // a mean little eye
                d.circle(b, hx + jx * s * 0.3f + ox * 2f, hy + jy * s * 0.3f + oy * 2f, s * 0.14f, OUTLINE, alpha);
            }
        }
    }
}
