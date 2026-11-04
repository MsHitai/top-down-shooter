package com.trush.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.player.Hero;

/**
 * The picture when a dream is dreamt to its end, in the other endings' picture-book style: morning in the
 * kids' bedroom. The sun comes up in the window, the little alarm clock on the bedside table rings, the hero
 * sits up in bed and stretches - and the last of the dream floats away over their head in a thought bubble,
 * popping into stars.
 */
class DreamEndingPicture {

    private static final Color FRAME = Color.valueOf("2a1e24");
    private static final Color WALL = Color.valueOf("8a8ac8");
    private static final Color WALL_LIT = Color.valueOf("ffd6a0");
    private static final Color FLOOR = Color.valueOf("a07048");
    private static final Color FLOOR_GAP = Color.valueOf("6e4428");
    private static final Color DAWN = Color.valueOf("ffb07a");
    private static final Color DAWN_HIGH = Color.valueOf("8fd0f5");
    private static final Color SUN = Color.valueOf("ffd23a");
    private static final Color SILL = Color.valueOf("5a4a7a");
    private static final Color TABLE = Color.valueOf("8a5e36");
    private static final Color CLOCK = Color.valueOf("e05a4a");
    private static final Color CLOCK_FACE = Color.valueOf("f5f0e6");
    private static final Color BUBBLE = Color.valueOf("f0e4ff");
    private static final Color DREAM = Color.valueOf("3a2a6a");
    private static final Color STAR = Color.valueOf("ffe48a");

    private final Color tmp = new Color();

    /** Draws the picture into the box (x, y, w, h) in screen units; {@code t} is the time since it appeared. */
    void draw(SpriteBatch b, Draw d, ETGFanfic game, float x, float y, float w, float h, float t) {
        float morning = MathUtils.clamp(t / 2.5f, 0f, 1f); // the room warms up as the sun rises
        d.rect(b, x - 0.06f, y - 0.06f, w + 0.12f, h + 0.12f, FRAME);
        float floorH = h * 0.32f;
        d.rect(b, x, y + floorH, w, h - floorH, tmp.set(WALL).lerp(WALL_LIT, morning * 0.7f));
        d.rect(b, x, y, w, floorH, FLOOR);
        for (int i = 1; i < 4; i++) {
            d.rect(b, x, y + floorH * i / 4f, w, 0.015f, FLOOR_GAP);
        }
        d.rect(b, x, y + floorH, w, 0.06f, SILL);

        // the window: dawn, the sun coming up over the roofs
        float wx = x + 0.45f;
        float wy = y + floorH + 0.55f;
        float ww = 1.3f;
        float wh = 1.15f;
        d.rect(b, wx - 0.06f, wy - 0.06f, ww + 0.12f, wh + 0.12f, SILL);
        for (int i = 0; i < 6; i++) {
            d.rect(b, wx, wy + wh * i / 6f, ww, wh / 6f + 0.01f, tmp.set(DAWN).lerp(DAWN_HIGH, i / 5f));
        }
        float sunY = wy + 0.05f + 0.5f * morning;
        d.circle(b, wx + ww * 0.62f, sunY, 0.22f, SUN);
        d.rect(b, wx, wy, ww, 0.18f, Color.valueOf("5a6a86")); // the roofs across the road
        d.rect(b, wx + 0.2f, wy + 0.18f, 0.25f, 0.12f, Color.valueOf("5a6a86"));
        d.rect(b, wx + ww / 2f - 0.03f, wy, 0.06f, wh, SILL);
        d.rect(b, wx, wy + wh / 2f - 0.03f, ww, 0.06f, SILL);
        d.rect(b, x + 0.2f, y + floorH, 2.3f, h - floorH, SUN, 0.08f * morning); // the light falling in

        // the bed, and the hero sitting up in it, stretching
        TextureRegion bed = game.sprites.home("bed");
        float bw = Sprites.w(bed) * 1.25f;
        float bh = Sprites.h(bed) * 1.25f;
        float bx = x + w - bw - 0.9f;
        float by = y + 0.2f;
        b.draw(bed, bx, by, bw, bh);
        TextureRegion[][] frames = game.sprites.hero(Hero.current.sheet);
        TextureRegion hero = frames[0][4 + ((int) (t * 2f)) % 2];
        float hw = Sprites.w(hero) * 1.25f;
        float hh = Sprites.h(hero) * 1.25f;
        float stretch = t > 1.2f ? MathUtils.clamp((t - 1.2f) * 3f, 0f, 1f) : 0f;
        float hx = bx + bw / 2f - hw / 2f;
        float hy = by + bh * 0.45f;
        b.draw(hero, hx, hy, hw, hh * 0.75f + 0.04f * stretch);
        for (int i = -1; i <= 1; i += 2) { // arms up: a big morning stretch
            d.rotRect(b, bx + bw / 2f + i * 0.2f, hy + hh * 0.55f + 0.1f * stretch, 0.06f, 0.2f + 0.1f * stretch,
                    i * (40f - 25f * stretch), Color.valueOf("d9a066"), 1f);
        }
        TextureRegion quilt = new TextureRegion(bed, 0, 26, bed.getRegionWidth(), 32); // the quilt over their legs
        b.draw(quilt, bx, by + 0.025f * 1.25f, bw, 32 / Sprites.PPU * 1.25f);

        // the bedside table and the alarm clock, ringing
        float tx = bx - 0.55f;
        d.rect(b, tx - 0.25f, y + 0.25f, 0.5f, 0.55f, TABLE);
        d.rect(b, tx - 0.28f, y + 0.78f, 0.56f, 0.06f, Color.valueOf("a8744a"));
        float ring = t < 3.5f ? MathUtils.sin(t * 60f) * 0.025f : 0f;
        float cx = tx + ring;
        float cy = y + 1.03f;
        d.circle(b, cx - 0.1f, cy + 0.17f, 0.06f, CLOCK);
        d.circle(b, cx + 0.1f, cy + 0.17f, 0.06f, CLOCK);
        d.circle(b, cx, cy, 0.18f, CLOCK);
        d.circle(b, cx, cy, 0.14f, CLOCK_FACE);
        d.line(b, cx, cy, 0.1f, 0.02f, 90f, FRAME);
        d.line(b, cx, cy, 0.07f, 0.02f, 0f, FRAME);
        d.rect(b, cx - 0.12f, cy - 0.21f, 0.04f, 0.06f, CLOCK);
        d.rect(b, cx + 0.08f, cy - 0.21f, 0.04f, 0.06f, CLOCK);
        if (ring != 0f) {
            for (int i = 0; i < 3; i++) { // little ring lines
                float a = 40f + i * 50f;
                d.line(b, cx + MathUtils.cosDeg(a) * 0.26f, cy + MathUtils.sinDeg(a) * 0.26f, 0.08f, 0.02f, a, FRAME);
            }
        }

        // the dream floats away in a thought bubble, and pops into stars
        float fade = MathUtils.clamp(1f - (t - 2f) / 1.5f, 0f, 1f);
        float qx = bx + bw / 2f + 0.55f;
        float qy = hy + hh + 0.15f + 0.1f * t;
        if (fade > 0f) {
            d.circle(b, qx - 0.35f, qy - 0.35f, 0.05f, BUBBLE, fade);
            d.circle(b, qx - 0.2f, qy - 0.2f, 0.08f, BUBBLE, fade);
            d.ellipse(b, qx + 0.3f, qy + 0.2f, 0.55f, 0.35f, BUBBLE, fade);
            d.ellipse(b, qx + 0.3f, qy + 0.2f, 0.47f, 0.28f, DREAM, fade);
            d.circle(b, qx + 0.3f, qy + 0.2f, 0.14f, Color.WHITE, fade); // the Nightmare's eye, still watching
            d.rect(b, qx + 0.28f, qy + 0.08f, 0.04f, 0.24f, FRAME, fade);
            d.rect(b, qx + 0.18f, qy + 0.18f, 0.24f, 0.04f, FRAME, fade);
        } else {
            for (int i = 0; i < 8; i++) {
                float k = MathUtils.clamp((t - 3.5f) / 1.2f, 0f, 1f);
                float a = i * 45f;
                d.shape(b, d.star, qx + 0.3f + MathUtils.cosDeg(a) * 0.6f * k, qy + 0.2f + MathUtils.sinDeg(a) * 0.4f * k,
                        0.06f, a + t * 90f, STAR, 1f - k);
            }
        }
        // whatever spilled over the edges is masked off, then the frame goes back on top
        float f = 0.06f;
        d.rect(b, 0, 0, 7f, y - f, Color.BLACK);
        d.rect(b, 0, y + h + f, 7f, 5f, Color.BLACK);
        d.rect(b, 0, 0, x - f, 5f, Color.BLACK);
        d.rect(b, x + w + f, 0, 7f, 5f, Color.BLACK);
    }
}
