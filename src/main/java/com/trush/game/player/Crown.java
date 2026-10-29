package com.trush.game.player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;

/**
 * The golden skull crown: the reward for cheering up Grandfluff in hard mode (save/Unlocks#crowned), a
 * gold crown with Egor's skull on the front, red-eyed like the one in the living room. Drawn pixel by
 * pixel on top of a hero's head - in runs (Player) and at home (screen/HubScreen).
 */
public final class Crown {

    private static final float P = 1f / Sprites.PPU;
    private static final Color OUTLINE = Color.valueOf("1e1416");
    private static final Color GOLD = Color.valueOf("f2c230");
    private static final Color GOLD_DARK = Color.valueOf("c48a1c");
    private static final Color SHINE = Color.valueOf("fff2a8");
    private static final Color BONE = Color.valueOf("f4ecd8");
    private static final Color EYE = Color.valueOf("e8343a");

    private Crown() {
    }

    /**
     * The top of {@code hero}'s head in their sheet, in pixels down from the frame's top, in the frames
     * where they're up a pixel (1, 3 and the first idle one) - the others sit a pixel lower.
     */
    private static int headTop(Hero hero) {
        switch (hero) {
            case SISTER:
                return 1;
            case GRANNY:
                return 0;
            case BABY:
                return 5;
            default:
                return 2;
        }
    }

    /** Pixels the head drops in frame column {@code col} of the hero sheet (walk 0-3, idle 4-5). */
    private static int bob(int col) {
        return col == 1 || col == 3 || col == 4 ? 0 : 1;
    }

    /**
     * Draws {@code hero}'s crown on their head: {@code cx} the sprite's middle, {@code bottom} its bottom
     * edge, {@code col} the frame column shown, {@code front} false when we see their back (no skull).
     */
    public static void draw(SpriteBatch b, Draw d, Hero hero, float cx, float bottom, int col, boolean front) {
        int top = headTop(hero) + bob(col);
        // it sits two pixels down over the hair
        drawAt(b, d, cx, bottom + (Sprites.HERO_H - top - 2) * P, front);
    }

    /** Draws the crown with its band's bottom at {@code base} (on top of a bike helmet, say). */
    public static void drawAt(SpriteBatch b, Draw d, float cx, float base, boolean front) {
        // the outline first: every gold piece a pixel bigger all round (a pixel's gap between the points)
        px(b, d, cx, base, -7, -1, 14, 5, OUTLINE);
        px(b, d, cx, base, -7, 3, 4, 4, OUTLINE);
        px(b, d, cx, base, -2, 3, 4, 5, OUTLINE);
        px(b, d, cx, base, 3, 3, 4, 4, OUTLINE);
        // the band and its three points, the middle one tallest
        px(b, d, cx, base, -6, 0, 12, 3, GOLD);
        px(b, d, cx, base, -6, 0, 12, 1, GOLD_DARK);
        px(b, d, cx, base, -6, 3, 2, 3, GOLD);
        px(b, d, cx, base, -1, 3, 2, 4, GOLD);
        px(b, d, cx, base, 4, 3, 2, 3, GOLD);
        px(b, d, cx, base, -6, 5, 1, 1, SHINE);
        px(b, d, cx, base, -1, 6, 1, 1, SHINE);
        px(b, d, cx, base, 4, 5, 1, 1, SHINE);
        if (!front) {
            return;
        }
        // Egor's skull on the front, red eyes and all: a round cranium, a narrower jaw
        px(b, d, cx, base, -2, 0, 4, 1, BONE);
        px(b, d, cx, base, -3, 1, 6, 3, BONE);
        px(b, d, cx, base, -2, 4, 4, 1, BONE);
        px(b, d, cx, base, -2, 2, 1, 1, EYE);
        px(b, d, cx, base, 1, 2, 1, 1, EYE);
        px(b, d, cx, base, -1, 0, 1, 1, OUTLINE); // the teeth
        px(b, d, cx, base, 0, 0, 1, 1, BONE);
    }

    /** A block of pixels {@code x, y} (from the crown's middle and base), {@code w} by {@code h}. */
    private static void px(SpriteBatch b, Draw d, float cx, float base, int x, int y, int w, int h, Color c) {
        d.rect(b, cx + x * P, base + y * P, w * P, h * P, c);
    }
}
