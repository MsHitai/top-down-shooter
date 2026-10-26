package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;

/**
 * The mark of the Skunk King's stink (the owner's idea): ordinary street critters it has turned into his
 * monsters. Not disgusting, just clearly wrong - one mismatched sickly green-yellow ear, and eyes glowing
 * the same colour (the eyes carry it in the dark; an ear alone is only a few pixels). Every marked critter
 * draws its mark with these, so they all look like they belong together. A few turn up in the Slums as a
 * hint of what's below; the Undercity will be full of them.
 */
public final class Marked {

    public static final Color MARK = Color.valueOf("c8e85a");
    private static final Color MARK_DARK = Color.valueOf("7a9a2a");
    /** What a critter's ear and eyes go back to once the mark fades (the normal ending). */
    private static final Color PLAIN = Color.valueOf("8a7a6a");
    private static final Color PLAIN_DARK = Color.valueOf("5a4e44");
    private static final Color PLAIN_EYE = Color.valueOf("16141c");

    /**
     * How far the mark has faded, 0 (all there) to 1 (gone): the normal ending's picture turns it up while it
     * draws the critters - the King is beaten and they're just critters again - and puts it back to 0.
     */
    public static float fade;
    private static final Color tmp = new Color();
    private static final Color tmpDark = new Color();

    private Marked() {
    }

    /** The odd ear: a pointed green-yellow ear at (x, y) (its base), {@code size} tall, tilted outwards. */
    public static void ear(SpriteBatch b, Draw d, float x, float y, float size, float tilt) {
        tmp.set(MARK).lerp(PLAIN, fade);
        tmpDark.set(MARK_DARK).lerp(PLAIN_DARK, fade);
        d.rotRect(b, x, y + size * 0.4f, size * 0.55f, size, tilt, tmpDark, 1f);
        d.rotRect(b, x, y + size * 0.45f, size * 0.35f, size * 0.75f, tilt, tmp, 1f);
        d.circle(b, x + MathUtils.sinDeg(-tilt) * size * 0.85f, y + MathUtils.cosDeg(tilt) * size * 0.85f, size * 0.15f,
                tmp, 1f);
    }

    /** A glowing eye: a soft halo and a bright core that pulses a little ({@code clock} in seconds). */
    public static void eye(SpriteBatch b, Draw d, float x, float y, float r, float clock) {
        float pulse = (0.25f + 0.1f * MathUtils.sin(clock * 5f)) * (1f - fade);
        d.circle(b, x, y, r * 2.4f, MARK, pulse);
        d.circle(b, x, y, r, tmp.set(MARK).lerp(PLAIN_EYE, fade), 1f);
        d.circle(b, x - r * 0.3f, y + r * 0.3f, r * 0.35f, Color.WHITE, 0.9f);
    }
}
