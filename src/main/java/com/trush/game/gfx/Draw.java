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

    public final Texture pixel;
    public final Texture circle;
    public final Texture ring;

    public final Texture heartFull;
    public final Texture heartHalf;
    public final Texture heartEmpty;
    public final Texture blankIcon;

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

        ObjectMap<Character, Color> pal = new ObjectMap<>();
        pal.put('X', Color.valueOf("1a0d12"));
        pal.put('R', Color.valueOf("e43b44"));
        pal.put('W', Color.valueOf("ffd1d4"));
        heartFull = pattern(HEART, pal, -1, null);
        heartHalf = pattern(HEART, pal, 5, Color.valueOf("3a2a30"));
        heartEmpty = pattern(HEART, pal, 0, Color.valueOf("3a2a30"));

        ObjectMap<Character, Color> blankPal = new ObjectMap<>();
        blankPal.put('X', Color.valueOf("0f1a2e"));
        blankPal.put('B', Color.valueOf("4fa4ff"));
        blankPal.put('L', Color.valueOf("2b6cc4"));
        blankPal.put('W', Color.WHITE);
        blankIcon = pattern(BLANK, blankPal, -1, null);
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

    public void texture(SpriteBatch b, Texture t, float x, float y, float w, float h) {
        b.setColor(Color.WHITE);
        b.draw(t, x, y, w, h);
    }

    @Override
    public void dispose() {
        pixel.dispose();
        circle.dispose();
        ring.dispose();
        heartFull.dispose();
        heartHalf.dispose();
        heartEmpty.dispose();
        blankIcon.dispose();
    }
}
