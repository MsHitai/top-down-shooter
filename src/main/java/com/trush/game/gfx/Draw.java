package com.trush.game.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * Tiny immediate-mode drawing helpers built on top of SpriteBatch, so placeholder art
 * (enemies, bullets, pickups, HUD) can be drawn without image assets and without
 * switching between SpriteBatch and ShapeRenderer every frame.
 * <p>
 * Everything is in world units. Swap any of these calls for real sprites later.
 */
public class Draw implements Disposable {

    /** The dark outline colour placeholder art is drawn with. */
    public static final Color OUTLINE_COLOR = Color.valueOf("2a0a14");

    public final Texture pixel;
    public final Texture circle;
    public final Texture ring;
    /** A triangle pointing right and a five-pointed star, for shaped bullets ({@link #shape}). */
    public final Texture triangle;
    public final Texture star;

    public final Texture heartFull;
    public final Texture heartHalf;
    public final Texture heartEmpty;
    /** Hearts 9-16, layered on top of the red ones: gold full, gold half (see-through on the right), empty. */
    public final Texture heartGold;
    public final Texture heartGoldHalf;
    public final Texture heartGoldEmpty;
    public final Texture blankIcon;
    public final Texture keyIcon;
    /** Night: black, clear in the middle and darker toward the edge (drawn 14x14 units around the player). */
    public final Texture darkness;
    /** A soft white glow (lamp light, drawn with additive blending). */
    public final Texture glow;

    private static final String[] HEART = {
            ".XXX.XXX.",
            "XRWRXRRRX",
            "XWRRRRRRX",
            "XRRRRRRRX",
            ".XRRRRRX.",
            "..XRRRX..",
            "...XRX...",
            "....X....",
    };

    private static final String[] BLANK = {
            "..XXX..",
            ".XBBBX.",
            "XBWBBBX",
            "XBBBBBX",
            "XBBBBLX",
            ".XBBLX.",
            "..XXX..",
    };

    private static final String[] KEY = {
            ".XXX......",
            "XGWGXXXXXX",
            "XG.GGGGGGX",
            "XGGGXXGXGX",
            ".XXX..X.X.",
    };

    public Draw() {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        pixel = new Texture(p);
        p.dispose();

        Pixmap c = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
        c.setColor(Color.WHITE);
        c.fillCircle(64, 64, 63);
        circle = new Texture(c);
        circle.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        c.dispose();

        Pixmap r = new Pixmap(256, 256, Pixmap.Format.RGBA8888);
        r.setBlending(Pixmap.Blending.None);
        r.setColor(Color.WHITE);
        r.fillCircle(128, 128, 127);
        r.setColor(0, 0, 0, 0);
        r.fillCircle(128, 128, 112);
        ring = new Texture(r);
        ring.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        r.dispose();

        Pixmap t = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
        t.setColor(Color.WHITE);
        t.fillTriangle(124, 64, 30, 10, 30, 118);
        triangle = new Texture(t);
        triangle.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        t.dispose();

        Pixmap st = new Pixmap(128, 128, Pixmap.Format.RGBA8888);
        st.setColor(Color.WHITE);
        for (int i = 0; i < 5; i++) {
            double out = Math.toRadians(i * 72 - 90);
            double in1 = Math.toRadians(i * 72 - 90 + 36);
            double in0 = Math.toRadians(i * 72 - 90 - 36);
            int ox = (int) (64 + Math.cos(out) * 62);
            int oy = (int) (64 + Math.sin(out) * 62);
            st.fillTriangle(64, 64, ox, oy, (int) (64 + Math.cos(in1) * 26), (int) (64 + Math.sin(in1) * 26));
            st.fillTriangle(64, 64, ox, oy, (int) (64 + Math.cos(in0) * 26), (int) (64 + Math.sin(in0) * 26));
        }
        star = new Texture(st);
        star.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        st.dispose();

        ObjectMap<Character, Color> pal = new ObjectMap<>();
        pal.put('X', Color.valueOf("1a0d12"));
        pal.put('R', Color.valueOf("e43b44"));
        pal.put('W', Color.valueOf("ffd1d4"));
        heartFull = pattern(HEART, pal, -1, null);
        heartHalf = pattern(HEART, pal, 5, Color.valueOf("3a2a30"));
        heartEmpty = pattern(HEART, pal, 0, Color.valueOf("3a2a30"));
        ObjectMap<Character, Color> gold = new ObjectMap<>();
        gold.put('X', Color.valueOf("3a2410"));
        gold.put('R', Color.valueOf("ffc83d"));
        gold.put('W', Color.valueOf("fff3b0"));
        heartGold = pattern(HEART, gold, -1, null);
        heartGoldHalf = pattern(HEART, gold, 5, new Color(0, 0, 0, 0));
        heartGoldEmpty = pattern(HEART, gold, 0, Color.valueOf("8a6a20"));

        ObjectMap<Character, Color> blankPal = new ObjectMap<>();
        blankPal.put('X', Color.valueOf("0f1a2e"));
        blankPal.put('B', Color.valueOf("4fa4ff"));
        blankPal.put('L', Color.valueOf("2b6cc4"));
        blankPal.put('W', Color.WHITE);
        blankIcon = pattern(BLANK, blankPal, -1, null);

        ObjectMap<Character, Color> keyPal = new ObjectMap<>();
        keyPal.put('X', Color.valueOf("3a2410"));
        keyPal.put('G', Color.valueOf("ffc83d"));
        keyPal.put('W', Color.valueOf("fff3b0"));
        keyIcon = pattern(KEY, keyPal, -1, null);

        darkness = radial(128, 0.26f, 0.52f, 0f, 0.55f);
        glow = radial(64, 0f, 1f, 1f, 0f);
    }

    /**
     * Builds a pixel-art texture from a string pattern. '.' is transparent.
     * Columns >= {@code emptyFromColumn} use {@code emptyColor} for every non-outline pixel
     * (used for half/empty hearts). Pass -1 to disable.
     */
    private static Texture pattern(String[] rows, ObjectMap<Character, Color> palette, int emptyFromColumn, Color emptyColor) {
        int h = rows.length;
        int w = rows[0].length();
        Pixmap pm = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                char ch = rows[y].charAt(x);
                if (ch == '.') {
                    pm.drawPixel(x, y, 0);
                    continue;
                }
                Color col = palette.get(ch, Color.MAGENTA);
                if (emptyFromColumn >= 0 && x >= emptyFromColumn && ch != 'X') {
                    col = emptyColor;
                }
                pm.drawPixel(x, y, Color.rgba8888(col));
            }
        }
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pm.dispose();
        return t;
    }

    /**
     * A square texture whose alpha goes from {@code innerAlpha} (inside {@code from} of the radius)
     * to {@code outerAlpha} (beyond {@code to}); colour is white for glows, black for darkness.
     */
    private static Texture radial(int size, float from, float to, float innerAlpha, float outerAlpha) {
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        boolean dark = outerAlpha > innerAlpha;
        float half = size / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float r = (float) Math.sqrt((x + 0.5f - half) * (x + 0.5f - half) + (y + 0.5f - half) * (y + 0.5f - half)) / half;
                float k = Math.max(0f, Math.min(1f, (r - from) / (to - from)));
                k = k * k * (3 - 2 * k); // smoothstep
                float a = innerAlpha + (outerAlpha - innerAlpha) * k;
                pm.drawPixel(x, y, dark ? Color.rgba8888(0, 0, 0, a) : Color.rgba8888(1, 1, 1, a));
            }
        }
        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return t;
    }

    private static void tint(SpriteBatch b, Color c, float alpha) {
        b.setColor(c.r, c.g, c.b, c.a * alpha);
    }

    public void rect(SpriteBatch b, float x, float y, float w, float h, Color c) {
        rect(b, x, y, w, h, c, 1f);
    }

    public void rect(SpriteBatch b, float x, float y, float w, float h, Color c, float alpha) {
        tint(b, c, alpha);
        b.draw(pixel, x, y, w, h);
        b.setColor(Color.WHITE);
    }

    public void circle(SpriteBatch b, float cx, float cy, float r, Color c) {
        circle(b, cx, cy, r, c, 1f);
    }

    public void circle(SpriteBatch b, float cx, float cy, float r, Color c, float alpha) {
        tint(b, c, alpha);
        b.draw(circle, cx - r, cy - r, r * 2, r * 2);
        b.setColor(Color.WHITE);
    }

    public void ellipse(SpriteBatch b, float cx, float cy, float rx, float ry, Color c, float alpha) {
        tint(b, c, alpha);
        b.draw(circle, cx - rx, cy - ry, rx * 2, ry * 2);
        b.setColor(Color.WHITE);
    }

    public void ring(SpriteBatch b, float cx, float cy, float r, Color c, float alpha) {
        tint(b, c, alpha);
        b.draw(ring, cx - r, cy - r, r * 2, r * 2);
        b.setColor(Color.WHITE);
    }

    /** A thick line starting at (x, y), {@code length} long, pointing at {@code degrees}. */
    public void line(SpriteBatch b, float x, float y, float length, float thickness, float degrees, Color c) {
        tint(b, c, 1f);
        b.draw(pixel, x, y - thickness / 2f, 0, thickness / 2f, length, thickness, 1, 1, degrees, 0, 0, 1, 1, false, false);
        b.setColor(Color.WHITE);
    }

    /** A rectangle centred on (cx, cy) rotated by {@code degrees}. */
    public void rotRect(SpriteBatch b, float cx, float cy, float w, float h, float degrees, Color c, float alpha) {
        tint(b, c, alpha);
        b.draw(pixel, cx - w / 2f, cy - h / 2f, w / 2f, h / 2f, w, h, 1, 1, degrees, 0, 0, 1, 1, false, false);
        b.setColor(Color.WHITE);
    }

    /** A white shape texture ({@link #triangle}, {@link #star}...) {@code r} in radius, turned and tinted. */
    public void shape(SpriteBatch b, Texture t, float cx, float cy, float r, float degrees, Color c, float alpha) {
        tint(b, c, alpha);
        b.draw(t, cx - r, cy - r, r, r, r * 2f, r * 2f, 1, 1, degrees, 0, 0, t.getWidth(), t.getHeight(), false, false);
        b.setColor(Color.WHITE);
    }

    public void texture(SpriteBatch b, Texture t, float x, float y, float w, float h) {
        b.setColor(Color.WHITE);
        b.draw(t, x, y, w, h);
    }

    @Override
    public void dispose() {
        triangle.dispose();
        star.dispose();
        pixel.dispose();
        circle.dispose();
        ring.dispose();
        heartFull.dispose();
        heartHalf.dispose();
        heartEmpty.dispose();
        heartGold.dispose();
        heartGoldHalf.dispose();
        heartGoldEmpty.dispose();
        blankIcon.dispose();
        keyIcon.dispose();
        darkness.dispose();
        glow.dispose();
    }
}
