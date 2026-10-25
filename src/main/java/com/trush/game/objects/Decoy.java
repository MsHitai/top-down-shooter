package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;

/**
 * The Cardboard Hero (after ETG's Decoy): a life-size cardboard cutout of the hero on a little stand.
 * While it stands, regular enemies aim at it instead of you (Enemy.targetX), and their bullets break on it.
 */
public class Decoy {

    private static final Color CARD = Color.valueOf("c8a070");
    private static final Color CARD_DARK = Color.valueOf("9a7448");
    private static final Color SKIN = Color.valueOf("f2c8a0");
    private static final Color SHIRT = Color.valueOf("3fa85a");
    private static final Color HAIR = Color.valueOf("6b4a2f");

    public static final float LIFE = 6f;
    /** Bullets break on it within this distance of its middle. */
    public static final float RADIUS = 0.2f;

    public final float x;
    public final float y;
    public float life = LIFE;
    private float wobble;

    public Decoy(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void update(float delta) {
        life -= delta;
        wobble = Math.max(0f, wobble - delta);
    }

    /** Got shot: it rocks on its stand. */
    public void hit() {
        wobble = 0.25f;
    }

    /** The middle of the cutout, where enemies aim. */
    public float centerY() {
        return y + 0.3f;
    }

    public float sortY() {
        return y;
    }

    public void draw(SpriteBatch b, Draw d) {
        float tilt = MathUtils.sin(wobble * 40f) * 8f * wobble / 0.25f;
        float alpha = life < 1f ? life : 1f; // folds away at the end
        d.circle(b, x, y, 0.16f, Color.BLACK, 0.25f * alpha);
        d.rect(b, x - 0.03f, y, 0.06f, 0.12f, CARD_DARK, alpha); // the stand
        // the cutout itself, a flat board leaning as it's hit
        d.rotRect(b, x, y + 0.34f, 0.36f, 0.52f, tilt, Draw.OUTLINE_COLOR, alpha);
        d.rotRect(b, x, y + 0.34f, 0.32f, 0.48f, tilt, CARD, alpha);
        d.rotRect(b, x, y + 0.26f, 0.22f, 0.18f, tilt, SHIRT, alpha);   // a painted-on hero
        d.circle(b, x, y + 0.45f, 0.08f, SKIN, alpha);
        d.rotRect(b, x, y + 0.52f, 0.16f, 0.05f, tilt, HAIR, alpha);
        d.circle(b, x - 0.03f, y + 0.45f, 0.012f, Draw.OUTLINE_COLOR, alpha);
        d.circle(b, x + 0.03f, y + 0.45f, 0.012f, Draw.OUTLINE_COLOR, alpha);
        d.rotRect(b, x, y + 0.41f, 0.05f, 0.01f, tilt, Draw.OUTLINE_COLOR, alpha); // a brave little smile
    }
}
