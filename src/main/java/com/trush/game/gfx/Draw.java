package com.trush.game.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
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

    /** Neutral ink shared by authored bodies and their moving equipment. */
    public static final Color ACTOR_EDGE = Color.valueOf("292631");
    private static final Color GUN_LIGHT = Color.valueOf("b3b7bb");
    private static final Color GUN_GRIP = Color.valueOf("916a52");

    public final Texture pixel;
    public final Texture circle;
    public final Texture ring;
    /** A triangle pointing right and a five-pointed star, for shaped bullets ({@link #shape}). */
    public final Texture triangle;
    public final Texture star;
    /** Small, nearest-filtered crescent for the spinning Banana-rang projectile. */
    public final Texture banana;
    /** Authored, animated art for the first two gun enemies. */
    public final KinArt kin;
    public final CastArt cast = new CastArt();
    public final RatArt rats;
    /** Native-resolution silhouette ink and material lighting for assembled enemies and bosses. */
    public final ActorInk actors;
    public final BossArt bosses;
    private final Texture actorCircle;
    private final Texture actorPanel;
    private final Texture[] detailedCircles = new Texture[EnemyMaterial.values().length];
    private final Texture[] detailedPanels = new Texture[EnemyMaterial.values().length];
    private final float[] quadVertices = new float[20];

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
    /** Dream darkness also dims the centre, keeping pale rooms from looking floodlit. */
    public final Texture lightsOut;
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

    private static final String[] BANANA = {
            ".........................",
            "XXXX..................XXX",
            "XTTXX...............XXYTX",
            "XTYYXX.............XXYYTX",
            "XTYYHYX...........XYHYYTX",
            ".XSYYHYXX.......XXYHYYSX.",
            "..XSYYHHYXXXXXXXYHHYYSX..",
            "...XSSYYHHHYYYHHHYYSSX...",
            "....XSSYYYYHHHYYYYSSX....",
            ".....XSSSYYYYYYYSSSX.....",
            "......XXSSSSSSSSSXX......",
            "........XXXSSSXXX........",
            "...........XXX...........",
    };

    public Draw() {
        kin = new KinArt();
        rats = new RatArt();
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        pixel = new Texture(p);
        p.dispose();

        Pixmap c = shapeMask(128);
        c.setColor(Color.WHITE);
        c.fillCircle(64, 64, 63);
        circle = new Texture(c);
        circle.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        c.dispose();

        Pixmap r = shapeMask(256);
        r.setColor(Color.WHITE);
        r.fillCircle(128, 128, 127);
        r.setColor(1, 1, 1, 0);
        r.fillCircle(128, 128, 112);
        ring = new Texture(r);
        ring.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        r.dispose();

        Pixmap t = shapeMask(128);
        t.setColor(Color.WHITE);
        t.fillTriangle(124, 64, 30, 10, 30, 118);
        triangle = new Texture(t);
        triangle.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        t.dispose();

        Pixmap st = shapeMask(128);
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

        ObjectMap<Character, Color> bananaPal = new ObjectMap<>();
        bananaPal.put('X', OUTLINE_COLOR);
        bananaPal.put('Y', Color.valueOf("f2d23a"));
        bananaPal.put('S', Color.valueOf("d9b020"));
        bananaPal.put('H', Color.valueOf("fff2a0"));
        bananaPal.put('T', Color.valueOf("79512c"));
        banana = pattern(BANANA, bananaPal, -1, null);

        darkness = radial(128, 0.26f, 0.52f, 0f, 0.55f);
        lightsOut = radial(128, 0.12f, 0.48f, 0.18f, 0.86f);
        glow = radial(64, 0f, 1f, 1f, 0f);
        actorCircle = materialSurface(true);
        actorPanel = materialSurface(false);
        for (EnemyMaterial material : EnemyMaterial.values()) {
            detailedCircles[material.ordinal()] = materialSurface(true, material);
            detailedPanels[material.ordinal()] = materialSurface(false, material);
        }
        actors = new ActorInk();
        bosses = new BossArt(actors);
    }

    /** Broad connected light/shadow planes for body parts; no texture noise or animation. */
    private static Texture materialSurface(boolean round) {
        return materialSurface(round, EnemyMaterial.PLAIN);
    }

    private static Texture materialSurface(boolean round, EnemyMaterial material) {
        int size = 128;
        Pixmap pm = shapeMask(size);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - 63.5f) / 63f;
                float dy = (63.5f - y) / 63f;
                float light;
                if (round) {
                    float radius = dx * dx + dy * dy;
                    if (radius > 1) continue;
                    float normal = -dx * 0.35f + dy * 0.5f + (float) Math.sqrt(1 - radius) * 0.79f;
                    light = normal > 0.85f ? 1f : normal > 0.38f ? 0.92f : 0.78f;
                } else {
                    light = y < 8 ? 1f : x < 8 ? 0.97f : x > 113 || y > 113 ? 0.8f : 0.94f;
                }
                pm.drawPixel(x, y, Color.rgba8888(light, light, light, 1));
            }
        }
        materialMarks(pm, material, round);
        Texture texture = new Texture(pm);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return texture;
    }

    private Texture surface(boolean round, boolean detail) {
        return detail ? (round ? detailedCircles : detailedPanels)[actors.surface().ordinal()]
                : round ? actorCircle : actorPanel;
    }

    /** A small number of connected, authored marks; no random grain or animated texture. */
    private static void materialMarks(Pixmap pm, EnemyMaterial material, boolean round) {
        switch (material) {
            case FABRIC -> {
                // Stitched hem, lapel folds and a small pocket edge.
                for (int y = 25; y < 105; y += 16) {
                    mark(pm, 17, y, 3, 7, 0.74f);
                    mark(pm, 106, y, 3, 7, 0.76f);
                }
                mark(pm, 35, 82, 24, 3, 0.72f); mark(pm, 35, 85, 24, 2, 1f);
                for (int y = 24; y < 48; y++) mark(pm, 45 + (y-24)/3, y, 3, 1, 0.81f);
                mark(pm, 67, 58, 4, 4, 0.68f); mark(pm, 67, 73, 4, 4, 0.68f);
            }
            case METAL -> {
                for (int x : new int[]{26, 98}) for (int y : new int[]{27, 98}) {
                    mark(pm, x-3, y-3, 7, 7, 0.58f);
                    mark(pm, x-2, y-2, 4, 4, 1f);
                }
                if (!round) {
                    mark(pm, 37, 47, 54, 3, 0.76f); mark(pm, 37, 50, 54, 2, 1f);
                    for (int x = 46; x < 85; x += 10) mark(pm, x, 64, 4, 12, 0.68f);
                } else {
                    mark(pm, 32, 40, 5, 18, 1f); mark(pm, 37, 34, 14, 5, 1f);
                }
            }
            case STONE -> {
                for (int y = 25; y < 83; y++) {
                    int x = 48 + (y < 48 ? (y-25)/4 : 6-(y-48)/7);
                    mark(pm, x, y, 3, 1, 0.71f); mark(pm, x+3, y, 2, 1, 1f);
                }
                mark(pm, 63, 78, 24, 4, 0.84f);
                for (int i = 0; i < 14; i++) mark(pm, 50+i, 51-i/2, 2, 2, 0.76f);
            }
            case SCALES -> {
                for (int[] xy : new int[][]{{45,40},{74,54},{48,76}}) {
                    mark(pm,xy[0]-6,xy[1],12,3,0.78f);
                    mark(pm,xy[0]-9,xy[1]-3,4,4,0.78f);
                    mark(pm,xy[0]+5,xy[1]-3,4,4,0.78f);
                    mark(pm,xy[0]-5,xy[1]+3,10,2,1f);
                }
            }
            case FUR -> { /* Clean colour planes: tiny repeated tufts shimmer in motion. */ }
            case WOOD -> {
                for (int y = 22; y < 110; y++) {
                    mark(pm,34+(y/26)%2*3,y,2,1,0.78f);
                    mark(pm,86-(y/20)%2*2,y,2,1,0.81f);
                }
                mark(pm,50,68,18,4,0.76f); mark(pm,55,72,9,3,0.86f);
            }
            case SOFT -> {
                mark(pm,31,37,5,14,1f); mark(pm,36,32,16,5,1f);
                mark(pm,85,82,6,3,0.83f);
            }
            default -> { }
        }
    }

    private static void mark(Pixmap pm, int x, int y, int w, int h, float light) {
        for (int py = y; py < y+h; py++) for (int px = x; px < x+w; px++) {
            if (px>=0 && py>=0 && px<pm.getWidth() && py<pm.getHeight() && (pm.getPixel(px,py)&255)>250)
                pm.drawPixel(px,py,Color.rgba8888(light,light,light,1));
        }
    }

    /** White RGB on both sides of the alpha edge prevents dark fringes when a tinted
     * shape is linearly filtered, particularly where the King's pale fluff overlaps. */
    private static Pixmap shapeMask(int size) {
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        pm.setColor(1, 1, 1, 0);
        pm.fill();
        return pm;
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
        actors.bounds(b, x, y, w, h);
        tint(b, c, alpha);
        b.draw(w > 0.10f && h > 0.10f && actors.material(c, alpha) ? surface(false, w > 0.45f && h > 0.40f) : pixel, x, y, w, h);
        b.setColor(Color.WHITE);
    }

    public void circle(SpriteBatch b, float cx, float cy, float r, Color c) {
        circle(b, cx, cy, r, c, 1f);
    }

    public void circle(SpriteBatch b, float cx, float cy, float r, Color c, float alpha) {
        actors.bounds(b, cx - r, cy - r, r * 2, r * 2);
        tint(b, c, alpha);
        b.draw(r > 0.065f && actors.material(c, alpha) ? surface(true, r > 0.32f) : circle, cx - r, cy - r, r * 2, r * 2);
        b.setColor(Color.WHITE);
    }

    public void ellipse(SpriteBatch b, float cx, float cy, float rx, float ry, Color c, float alpha) {
        actors.bounds(b, cx - rx, cy - ry, rx * 2, ry * 2);
        tint(b, c, alpha);
        b.draw(Math.min(rx, ry) > 0.05f && actors.material(c, alpha) ? surface(true, Math.min(rx, ry) > 0.32f) : circle,
                cx - rx, cy - ry, rx * 2, ry * 2);
        b.setColor(Color.WHITE);
    }

    public void ring(SpriteBatch b, float cx, float cy, float r, Color c, float alpha) {
        actors.bounds(b, cx - r, cy - r, r * 2, r * 2);
        tint(b, c, alpha);
        b.draw(ring, cx - r, cy - r, r * 2, r * 2);
        b.setColor(Color.WHITE);
    }

    /** A thick line starting at (x, y), {@code length} long, pointing at {@code degrees}. */
    /** Layered enemy firearm: receiver edge, barrel glint, dark muzzle and a separate grip. */
    public void firearm(SpriteBatch b, float x, float y, float length, float thickness, float angle, Color metal) {
        firearm(b, x, y, length, thickness, angle, metal, 1);
    }

    public void firearm(SpriteBatch b, float x, float y, float length, float thickness, float angle, Color metal, float alpha) {
        float ux = MathUtils.cosDeg(angle), uy = MathUtils.sinDeg(angle);
        float nx = -uy, ny = ux;
        line(b, x, y, length, thickness + 0.022f, angle, ACTOR_EDGE, alpha);
        line(b, x, y, length - 0.015f, thickness, angle, metal, alpha);
        line(b, x + nx * thickness * 0.3f, y + ny * thickness * 0.3f,
                length * 0.76f, 0.012f, angle, GUN_LIGHT, alpha);
        line(b, x + ux * length * 0.12f, y + uy * length * 0.12f,
                thickness * 1.2f, thickness * 0.8f, angle - 90, ACTOR_EDGE, alpha);
        line(b, x + ux * length * 0.12f, y + uy * length * 0.12f,
                thickness, thickness * 0.5f, angle - 90, GUN_GRIP, alpha);
        line(b, x + ux * (length - 0.035f), y + uy * (length - 0.035f),
                0.035f, thickness, angle, ACTOR_EDGE, alpha);
    }

    public void line(SpriteBatch b, float x, float y, float length, float thickness, float degrees, Color c) {
        line(b, x, y, length, thickness, degrees, c, 1f);
    }

    public void line(SpriteBatch b, float x, float y, float length, float thickness, float degrees, Color c, float alpha) {
        float ex = x + com.badlogic.gdx.math.MathUtils.cosDeg(degrees) * length;
        float ey = y + com.badlogic.gdx.math.MathUtils.sinDeg(degrees) * length;
        actors.bounds(b, Math.min(x, ex) - thickness, Math.min(y, ey) - thickness,
                Math.abs(ex - x) + thickness * 2, Math.abs(ey - y) + thickness * 2);
        tint(b, c, alpha);
        Texture surface = thickness > 0.055f && actors.material(c, alpha) ? surface(false, thickness > 0.14f && length > 0.25f) : pixel;
        b.draw(surface, x, y - thickness / 2f, 0, thickness / 2f, length, thickness, 1, 1, degrees,
                0, 0, surface.getWidth(), surface.getHeight(), false, false);
        b.setColor(Color.WHITE);
    }

    /** A rectangle centred on (cx, cy) rotated by {@code degrees}. */
    public void rotRect(SpriteBatch b, float cx, float cy, float w, float h, float degrees, Color c, float alpha) {
        float extent = (float) Math.sqrt(w * w + h * h) * 0.5f;
        actors.bounds(b, cx - extent, cy - extent, extent * 2, extent * 2);
        tint(b, c, alpha);
        Texture surface = Math.min(w, h) > 0.10f && actors.material(c, alpha) ? surface(false, Math.min(w, h) > 0.22f) : pixel;
        b.draw(surface, cx - w / 2f, cy - h / 2f, w / 2f, h / 2f, w, h, 1, 1, degrees,
                0, 0, surface.getWidth(), surface.getHeight(), false, false);
        b.setColor(Color.WHITE);
    }

    /** A white shape texture ({@link #triangle}, {@link #star}...) {@code r} in radius, turned and tinted. */
    public void shape(SpriteBatch b, Texture t, float cx, float cy, float r, float degrees, Color c, float alpha) {
        actors.bounds(b, cx - r * 1.42f, cy - r * 1.42f, r * 2.84f, r * 2.84f);
        tint(b, c, alpha);
        b.draw(t, cx - r, cy - r, r, r, r * 2f, r * 2f, 1, 1, degrees, 0, 0, t.getWidth(), t.getHeight(), false, false);
        b.setColor(Color.WHITE);
    }

    public void texture(SpriteBatch b, Texture t, float x, float y, float w, float h) {
        actors.bounds(b, x, y, w, h);
        b.setColor(Color.WHITE);
        b.draw(t, x, y, w, h);
    }

    /** A convex body plane, used for tapered reptile limbs rather than a chain of circles. */
    public void quad(SpriteBatch b, float x0, float y0, float x1, float y1,
                     float x2, float y2, float x3, float y3, Color color) {
        float minX = Math.min(Math.min(x0, x1), Math.min(x2, x3));
        float minY = Math.min(Math.min(y0, y1), Math.min(y2, y3));
        actors.bounds(b, minX, minY, Math.max(Math.max(x0, x1), Math.max(x2, x3)) - minX,
                Math.max(Math.max(y0, y1), Math.max(y2, y3)) - minY);
        float packed = color.toFloatBits();
        for (int i = 0; i < 4; i++) quadVertices[i * 5 + 2] = packed;
        quadVertices[0] = x0; quadVertices[1] = y0;
        quadVertices[5] = x1; quadVertices[6] = y1;
        quadVertices[10] = x2; quadVertices[11] = y2;
        quadVertices[15] = x3; quadVertices[16] = y3;
        quadVertices[3] = 0; quadVertices[4] = 0;
        quadVertices[8] = 0; quadVertices[9] = 1;
        quadVertices[13] = 1; quadVertices[14] = 1;
        quadVertices[18] = 1; quadVertices[19] = 0;
        b.draw(actors.material(color, 1) ? surface(false, true) : pixel, quadVertices, 0, 20);
    }

    @Override
    public void dispose() {
        actorCircle.dispose();
        actorPanel.dispose();
        for (Texture texture : detailedCircles) texture.dispose();
        for (Texture texture : detailedPanels) texture.dispose();
        bosses.dispose();
        actors.dispose();
        kin.dispose();
        rats.dispose();
        cast.dispose();
        triangle.dispose();
        star.dispose();
        banana.dispose();
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
        lightsOut.dispose();
        glow.dispose();
    }
}
