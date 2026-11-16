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
        // The same head as the travelling shark, enlarged around the target.
        d.projectiles.shark(b, x - ux * s * 0.64f, y - uy * s * 0.64f,
                s * 3.6f, s * 2.9f, a, gape / 42f, color, alpha);
    }
}
