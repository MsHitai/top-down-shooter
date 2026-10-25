package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;

/**
 * The Little Cloud (after ETG's companions): a tiny cloud with a face that floats along behind the hero's
 * shoulder and now and then drops a bolt of lightning on the nearest enemy (GameWorld picks it and strikes).
 */
public class LittleCloud {

    private static final Color GREY = Color.valueOf("c8d0e0");

    public static final float COOLDOWN = 1.3f;
    public static final float RANGE = 4.2f;
    public static final float DAMAGE = 9f;

    public final Vector2 pos = new Vector2();
    private final Vector2 target = new Vector2();
    public float cooldown = COOLDOWN;
    private float clock;
    /** Puffed up with static just after a strike. */
    private float zap;
    private boolean placed;

    /** Drifts after a spot just behind and above the hero. */
    public void update(float delta, float heroX, float heroY, boolean facingRight) {
        float tx = heroX + (facingRight ? -0.35f : 0.35f);
        float ty = heroY + 0.55f;
        if (!placed) {
            pos.set(tx, ty);
            placed = true;
        }
        pos.lerp(target.set(tx, ty), 1f - (float) Math.exp(-4f * delta));
        clock += delta;
        cooldown -= delta;
        zap = Math.max(0f, zap - delta);
    }

    /** Snaps to the hero (a new room). */
    public void reset() {
        placed = false;
    }

    public void struck() {
        zap = 0.25f;
        cooldown = COOLDOWN;
    }

    /** The bottom of the cloud, where its lightning comes from. */
    public float boltY() {
        return pos.y - 0.06f + bob();
    }

    private float bob() {
        return MathUtils.sin(clock * 3f) * 0.03f;
    }

    public void drawShadow(SpriteBatch b, Draw d) {
        d.circle(b, pos.x, pos.y - 0.55f, 0.1f, Color.BLACK, 0.18f);
    }

    public void draw(SpriteBatch b, Draw d) {
        float y = pos.y + bob();
        Color body = zap > 0 ? Color.WHITE : GREY;
        d.circle(b, pos.x, y, 0.15f, Draw.OUTLINE_COLOR, 0.8f);
        d.circle(b, pos.x - 0.1f, y - 0.02f, 0.09f, Draw.OUTLINE_COLOR, 0.8f);
        d.circle(b, pos.x + 0.1f, y - 0.02f, 0.09f, Draw.OUTLINE_COLOR, 0.8f);
        d.circle(b, pos.x, y, 0.13f, body);
        d.circle(b, pos.x - 0.1f, y - 0.02f, 0.075f, body);
        d.circle(b, pos.x + 0.1f, y - 0.02f, 0.075f, body);
        d.circle(b, pos.x - 0.03f, y + 0.05f, 0.07f, Color.WHITE);
        d.circle(b, pos.x - 0.04f, y, 0.014f, Draw.OUTLINE_COLOR); // a sleepy little face
        d.circle(b, pos.x + 0.04f, y, 0.014f, Draw.OUTLINE_COLOR);
        if (zap > 0) {
            d.circle(b, pos.x, y, 0.22f, Color.YELLOW, 0.3f * zap / 0.25f);
        }
    }
}
