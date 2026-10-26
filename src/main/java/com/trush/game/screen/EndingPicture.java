package com.trush.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.player.Hero;

/**
 * The picture on the victory screen, in the opening story's picture-book style: peace is back. Under a
 * smiling sun the whole kingdom lies quiet - the village, the town, the city and the skyscraper - the
 * Fluffy Kingdom's pink castle floats on its cloud again, and the hero flies off on a little cloud,
 * waving, a rainbow trailing behind him.
 */
class EndingPicture {

    private static final Color FRAME = Color.valueOf("2a1e24");
    private static final Color SKY = Color.valueOf("8fd0f5");
    private static final Color SKY_LOW = Color.valueOf("ffd6e8");
    private static final Color SUN = Color.valueOf("ffd23a");
    private static final Color GRASS = Color.valueOf("6aa84a");
    private static final Color GRASS_DARK = Color.valueOf("4f8a3a");
    private static final Color HILL_PINK = Color.valueOf("f2b8d0");
    private static final Color WALL = Color.valueOf("f0e2c0");
    private static final Color THATCH = Color.valueOf("c49a45");
    private static final Color TOWN_A = Color.valueOf("e8b48a");
    private static final Color TOWN_B = Color.valueOf("c0704f");
    private static final Color ROOF = Color.valueOf("8a3a26");
    private static final Color CITY = Color.valueOf("7a8aa6");
    private static final Color CITY_DARK = Color.valueOf("5a6a86");
    private static final Color WINDOW = Color.valueOf("ffe9a0");
    private static final Color SKIN = Color.valueOf("d9a066");
    private static final Color SHIRT = Color.valueOf("2f9e5b");
    private static final Color GLASS = Color.valueOf("8ec5e8");
    private static final Color STEEL = Color.valueOf("5a6278");
    private static final Color CASTLE = Color.valueOf("ffc6dc");
    private static final Color CASTLE_DARK = Color.valueOf("e08ab0");
    private static final Color CASTLE_ROOF = Color.valueOf("8a5ab0");
    private static final Color[] RAINBOW = {Color.valueOf("e84a4a"), Color.valueOf("f29a3a"), Color.valueOf("f2e05a"),
            Color.valueOf("6ac85a"), Color.valueOf("4a9ae8"), Color.valueOf("8a6ad8")};
    private static final Color[] FLOWERS = {Color.valueOf("e84a6a"), Color.valueOf("f2e86d"), Color.WHITE,
            Color.valueOf("e88fb4")};

    private final Color tmp = new Color();

    /** Draws the picture into the box (x, y, w, h) in screen units; {@code t} is the time since it appeared. */
    void draw(SpriteBatch b, Draw d, ETGFanfic game, float x, float y, float w, float h, float t) {
        d.rect(b, x - 0.06f, y - 0.06f, w + 0.12f, h + 0.12f, FRAME);
        int bands = 12;
        for (int i = 0; i < bands; i++) {
            float k = i / (float) (bands - 1);
            d.rect(b, x, y + h * i / bands, w, h / bands + 0.01f, tmp.set(SKY_LOW).lerp(SKY, k));
        }
        // the sun is smiling again
        float sx = x + 0.7f;
        float sy = y + h - 0.6f;
        for (int i = 0; i < 8; i++) {
            float a = i * 45f + t * 20f;
            d.rotRect(b, sx + MathUtils.cosDeg(a) * 0.45f, sy + MathUtils.sinDeg(a) * 0.45f, 0.16f, 0.05f, a, SUN, 0.8f);
        }
        d.circle(b, sx, sy, 0.32f, SUN);
        d.circle(b, sx - 0.1f, sy + 0.06f, 0.035f, FRAME);
        d.circle(b, sx + 0.1f, sy + 0.06f, 0.035f, FRAME);
        d.rotRect(b, sx - 0.05f, sy - 0.08f, 0.11f, 0.03f, -25f, FRAME, 1f);
        d.rotRect(b, sx + 0.05f, sy - 0.08f, 0.11f, 0.03f, 25f, FRAME, 1f);
        for (int i = 0; i < 3; i++) { // drifting clouds
            float cx = x + ((i * 2.3f + t * 0.12f) % (w + 1.2f)) - 0.6f;
            cloud(b, d, cx, y + h - 0.45f - (i % 2) * 0.5f, 0.17f, 0.9f);
        }
        drawCastle(b, d, x + w - 0.75f, y + h - 0.85f, t);
        drawKingdom(b, d, x, y, w, t);
        drawHero(b, d, game, x, y, h, t);
        // whatever spilled over the edges is masked off, then the frame goes back on top
        float f = 0.06f;
        d.rect(b, 0, 0, 7f, y - f, Color.BLACK);
        d.rect(b, 0, y + h + f, 7f, 5f, Color.BLACK);
        d.rect(b, 0, 0, x - f, 5f, Color.BLACK);
        d.rect(b, x + w + f, 0, 2f, 5f, Color.BLACK);
        d.rect(b, x - f, y - f, w + 2 * f, f, FRAME);
        d.rect(b, x - f, y + h, w + 2 * f, f, FRAME);
        d.rect(b, x - f, y, f, h, FRAME);
        d.rect(b, x + w, y, f, h, FRAME);
    }

    private static void cloud(SpriteBatch b, Draw d, float x, float y, float s, float alpha) {
        d.circle(b, x, y, s, Color.WHITE, alpha);
        d.circle(b, x + s * 0.9f, y - s * 0.15f, s * 0.75f, Color.WHITE, alpha);
        d.circle(b, x - s * 0.9f, y - s * 0.2f, s * 0.65f, Color.WHITE, alpha);
    }

    /** The Fluffy Kingdom: the pink castle floating on its cloud, flags flying. */
    private void drawCastle(SpriteBatch b, Draw d, float cx, float base, float t) {
        float bob = MathUtils.sin(t * 1.2f) * 0.04f;
        base += bob;
        for (int i = -1; i <= 1; i++) {
            float tx = cx + i * 0.32f;
            float th = i == 0 ? 0.62f : 0.44f;
            d.rect(b, tx - 0.09f, base, 0.18f, th, CASTLE);
            d.rect(b, tx - 0.09f, base, 0.04f, th, CASTLE_DARK);
            d.shape(b, d.triangle, tx, base + th + 0.06f, 0.12f, 90f, CASTLE_ROOF, 1f);
            d.rect(b, tx - 0.005f, base + th + 0.12f, 0.012f, 0.1f, FRAME);
            d.rect(b, tx + 0.008f, base + th + 0.17f + MathUtils.sin(t * 4f + i) * 0.008f, 0.08f, 0.045f, HILL_PINK);
        }
        d.rect(b, cx - 0.3f, base, 0.6f, 0.3f, CASTLE);
        d.rect(b, cx - 0.06f, base, 0.12f, 0.16f, CASTLE_DARK);
        cloud(b, d, cx - 0.2f, base - 0.05f, 0.2f, 1f);
        cloud(b, d, cx + 0.25f, base - 0.08f, 0.18f, 1f);
    }

    /** The whole kingdom along the bottom, at peace: village, town, city and the skyscraper, flowers on the hills. */
    private void drawKingdom(SpriteBatch b, Draw d, float x, float y, float w, float t) {
        float ground = y + 0.5f;
        // the skyscraper (far right), then the city's towers, both standing behind the hills
        float kx = x + w - 0.75f;
        float top = 1.2f;
        d.rect(b, kx - 0.22f, ground, 0.44f, top, GLASS);
        d.rect(b, kx - 0.22f, ground, 0.08f, top, STEEL);
        for (float wy = ground + 0.12f; wy < ground + top - 0.05f; wy += 0.16f) {
            d.rect(b, kx - 0.13f, wy, 0.32f, 0.03f, STEEL);
        }
        d.rect(b, kx - 0.01f, ground + top, 0.02f, 0.16f, STEEL);
        d.circle(b, kx, ground + top + 0.17f, 0.025f, MathUtils.sin(t * 5f) > 0 ? Color.RED : STEEL);
        float[][] towers = {{x + 3.2f, 0.9f}, {x + 3.55f, 1.2f}, {x + 3.95f, 0.75f}, {x + 4.3f, 1.05f}};
        for (float[] tw : towers) {
            d.rect(b, tw[0], ground, 0.3f, tw[1], CITY);
            d.rect(b, tw[0], ground, 0.06f, tw[1], CITY_DARK);
            for (float wy = ground + 0.12f; wy < ground + tw[1] - 0.1f; wy += 0.18f) {
                d.rect(b, tw[0] + 0.1f, wy, 0.05f, 0.06f, WINDOW);
                d.rect(b, tw[0] + 0.19f, wy, 0.05f, 0.06f, WINDOW);
            }
        }
        // the town's houses
        for (int i = 0; i < 3; i++) {
            float hx = x + 1.75f + i * 0.45f;
            float hh = 0.4f + (i % 2) * 0.15f;
            d.rect(b, hx, ground, 0.38f, hh, i % 2 == 0 ? TOWN_A : TOWN_B);
            d.shape(b, d.triangle, hx + 0.19f, ground + hh + 0.08f, 0.22f, 90f, ROOF, 1f);
            d.rect(b, hx + 0.13f, ground + 0.12f, 0.1f, 0.1f, WINDOW);
        }
        // the village's cottages
        for (int i = 0; i < 2; i++) {
            float hx = x + 0.35f + i * 0.65f;
            d.rect(b, hx, ground - 0.05f, 0.5f, 0.32f, WALL);
            d.shape(b, d.triangle, hx + 0.25f, ground + 0.36f, 0.3f, 90f, THATCH, 1f);
            d.rect(b, hx + 0.19f, ground - 0.05f, 0.12f, 0.18f, ROOF);
        }
        // soft rolling hills in front, dotted with flowers
        d.circle(b, x + 1.0f, y - 1.0f, 1.6f, GRASS);
        d.circle(b, x + 3.2f, y - 1.3f, 1.9f, HILL_PINK);
        d.circle(b, x + 5.3f, y - 1.0f, 1.6f, GRASS_DARK);
        for (int i = 0; i < 16; i++) {
            float fx = x + 0.2f + i * 0.37f;
            float fy = y + 0.12f + (i * 7 % 5) * 0.06f;
            d.circle(b, fx, fy + MathUtils.sin(t * 2f + i) * 0.01f, 0.03f, FLOWERS[i % FLOWERS.length]);
        }
    }

    /** The hero on a little cloud, gliding in from the left and waving, with a rainbow trailing behind. */
    private void drawHero(SpriteBatch b, Draw d, ETGFanfic game, float x, float y, float h, float t) {
        float k = Interpolation.pow2Out.apply(Math.min(1f, t / 3.5f));
        float hx = x + 0.3f + 2.4f * k;
        float hy = y + h * 0.5f + 0.25f * k + MathUtils.sin(t * 2f) * 0.05f;
        // the rainbow trail: bands arching from the left edge up to his cloud
        for (int band = 0; band < RAINBOW.length; band++) {
            for (int i = 0; i <= 24; i++) {
                float s = i / 24f;
                float px = x - 0.2f + (hx - 0.25f - x + 0.2f) * s;
                float py = y + 0.6f + (hy - 0.05f - y - 0.6f) * s + MathUtils.sin(s * MathUtils.PI) * 0.35f
                        + (band - 2.5f) * 0.045f;
                d.circle(b, px, py, 0.03f, RAINBOW[band], 0.9f);
            }
        }
        cloud(b, d, hx, hy - 0.05f, 0.18f, 1f);
        TextureRegion hero = game.sprites.hero(Hero.current.sheet)[0][4 + (int) (t * 2f) % 2]; // whoever won it
        float s = 1.0f;
        float w = Sprites.w(hero) * s;
        float hh = Sprites.h(hero) * s;
        b.draw(hero, hx - w / 2f, hy + 0.02f, w, hh);
        // waving goodbye: an arm up from his shoulder, swinging side to side
        float shoulderX = hx - w * 0.36f;
        float shoulderY = hy + 0.02f + hh * 0.52f;
        float arm = 115f + MathUtils.sin(t * 8f) * 20f;
        d.line(b, shoulderX, shoulderY, 0.2f, 0.07f, arm, FRAME);
        d.line(b, shoulderX, shoulderY, 0.18f, 0.045f, arm, SHIRT);
        d.circle(b, shoulderX + MathUtils.cosDeg(arm) * 0.2f, shoulderY + MathUtils.sinDeg(arm) * 0.2f, 0.04f, SKIN);
    }
}
