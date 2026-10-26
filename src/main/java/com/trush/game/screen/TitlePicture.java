package com.trush.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.trush.game.gfx.Draw;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.save.Unlocks;

/**
 * The title screen's picture (like ETG's castle), in the ending's picture-book style: the whole road
 * of the story laid out left to right - the village, the town, the city at night, the skyscraper and
 * the Fluffy King's castle up on its cloud - slowly panning by. Places you haven't reached yet are
 * grey shapes in the mist with a "?"; the first time the title screen sees a new one, it sparkles and
 * its name comes up. The castle sits under storm clouds until the King is beaten, then the rainbow is
 * back. A shark fin in the sea at the bottom right only once the sea has been found.
 */
class TitlePicture {

    private static final String SEEN_PREF = "titleSeen";
    /** The landscape is this wide; the view (7 units) pans across it. */
    private static final float WIDTH = 9f;
    private static final float GROUND = 1.4f;
    private static final float ARRIVAL_TIME = 4.5f;

    private static final Color FRAME = Color.valueOf("2a1e24");
    private static final Color SKY = Color.valueOf("8fd0f5");
    private static final Color SKY_LOW = Color.valueOf("ffd6e8");
    private static final Color SUN = Color.valueOf("ffd23a");
    private static final Color GRASS = Color.valueOf("6aa84a");
    private static final Color GRASS_DARK = Color.valueOf("4f8a3a");
    private static final Color HILL_PINK = Color.valueOf("f2b8d0");
    private static final Color WALL = Color.valueOf("f0e2c0");
    private static final Color THATCH = Color.valueOf("c49a45");
    private static final Color WOOD = Color.valueOf("8a5a3a");
    private static final Color TOWN_A = Color.valueOf("e8b48a");
    private static final Color TOWN_B = Color.valueOf("c0704f");
    private static final Color ROOF = Color.valueOf("8a3a26");
    private static final Color NIGHT = Color.valueOf("2a3458");
    private static final Color CITY = Color.valueOf("4a5a7a");
    private static final Color CITY_DARK = Color.valueOf("36425e");
    private static final Color WINDOW = Color.valueOf("ffe9a0");
    private static final Color GLASS = Color.valueOf("8ec5e8");
    private static final Color STEEL = Color.valueOf("5a6278");
    private static final Color CASTLE = Color.valueOf("ffc6dc");
    private static final Color CASTLE_DARK = Color.valueOf("e08ab0");
    private static final Color CASTLE_ROOF = Color.valueOf("8a5ab0");
    private static final Color STORM = Color.valueOf("6a6a7e");
    private static final Color SEA = Color.valueOf("3a7ac0");
    private static final Color SAND = Color.valueOf("f0d898");
    private static final Color SEA_LIGHT = Color.valueOf("6aaee8");
    private static final Color FIN = Color.valueOf("3f5a8a");
    private static final Color MIST = Color.valueOf("b8bcc8");
    private static final Color[] RAINBOW = {Color.valueOf("e84a4a"), Color.valueOf("f29a3a"), Color.valueOf("f2e05a"),
            Color.valueOf("6ac85a"), Color.valueOf("4a9ae8"), Color.valueOf("8a6ad8")};
    private static final Color[] FLOWERS = {Color.valueOf("e84a6a"), Color.valueOf("f2e86d"), Color.WHITE,
            Color.valueOf("e88fb4")};

    /** The places, in story order (floors 1-5); their middle x in the landscape and how high their "?" floats. */
    private static final String[] NAMES = {"THE VILLAGE", "THE TOWN", "THE CITY", "THE SKYSCRAPER", "THE FLUFFY KINGDOM"};
    private static final float[] PLACE_X = {1.1f, 2.8f, 4.6f, 6.2f, 7.8f};
    private static final float[] PLACE_TOP = {2.2f, 2.45f, 2.85f, 3.65f, 3.65f};

    private final ETGFanfic game;
    private final Color tmp = new Color();
    private final Color col = new Color();
    private final Color wallColor = new Color();
    private final Color darkColor = new Color();
    /** Floors reached (0-5) and whether the King is beaten. */
    private int reached;
    private boolean kingBeaten;
    /** What's new since the title screen last looked (-1 none; 5 = the castle lit up), and how long ago it showed. */
    private int arrival = -1;
    private float arrivalTime;
    /** The place being drawn right now is still a grey shape in the mist. */
    private boolean locked;

    TitlePicture(ETGFanfic game) {
        this.game = game;
        refresh();
    }

    /** Reads the progress again (on show, after a progress reset); a new place gets its sparkle. */
    void refresh() {
        reached = game.getFurthestFloor();
        kingBeaten = game.getWins() > 0;
        int level = reached + (kingBeaten ? 1 : 0);
        int seen = game.prefs.getInteger(SEEN_PREF, 0);
        arrival = level > seen ? Math.min(level - 1, NAMES.length) : -1;
        arrivalTime = 0f;
        if (level != seen) { // (lower after a reset: the mist comes back quietly)
            game.prefs.putInteger(SEEN_PREF, level);
            game.prefs.flush();
        }
    }

    /** The colour to paint with: the real one, or mist grey for a place not reached yet. */
    private Color c(Color real) {
        return locked ? col.set(real).lerp(MIST, 0.8f) : real;
    }

    void draw(SpriteBatch b, Draw d, float t, float delta) {
        arrivalTime += delta;
        // starts on the castle's side, then drifts back and forth along the road
        float pan = (1f - MathUtils.cos(t * 0.09f + MathUtils.PI)) / 2f * (WIDTH - 7f);
        float x = -pan;

        int bands = 14;
        for (int i = 0; i < bands; i++) {
            float k = i / (float) (bands - 1);
            d.rect(b, 0, 5f * i / bands, 7f, 5f / bands + 0.01f, tmp.set(SKY_LOW).lerp(SKY, k));
        }
        drawSun(b, d, x + 0.6f, 3.55f, t);
        for (int i = 0; i < 4; i++) { // drifting clouds
            float cx = ((i * 2.6f + t * 0.1f) % (WIDTH + 1.5f)) - 0.8f;
            cloud(b, d, x + cx, 3.5f - (i % 2) * 0.45f, 0.16f, Color.WHITE, 0.85f);
        }

        // the far places first, the near ones and the hills in front
        drawCastle(b, d, x + PLACE_X[4], 2.75f, t);
        drawSkyscraper(b, d, x + PLACE_X[3], t);
        drawCity(b, d, x + PLACE_X[2], t);
        drawTown(b, d, x + PLACE_X[1]);
        drawVillage(b, d, x + PLACE_X[0], t);
        drawHills(b, d, x, t);
        drawSea(b, d, x, t);

        for (int i = 0; i < NAMES.length; i++) {
            boolean dark = i >= reached;
            if (dark) { // mist drifting over it, and a question mark
                float px = x + PLACE_X[i];
                for (int m = 0; m < 3; m++) {
                    float mx = px + MathUtils.sin(t * 0.4f + m * 2f + i) * 0.25f;
                    cloud(b, d, mx, (i == 4 ? 2.75f : GROUND + 0.25f) + m * 0.35f, 0.2f, MIST, 0.35f);
                }
                float bob = MathUtils.sin(t * 2f + i) * 0.04f;
                game.text("?", px, PLACE_TOP[i] + 0.3f + bob, 0.7f, FRAME, Align.center);
            }
        }
        drawArrival(b, d, x, t);
    }

    /** The newest place sparkles and its name comes up for a few seconds. */
    private void drawArrival(SpriteBatch b, Draw d, float x, float t) {
        if (arrival < 0 || arrivalTime > ARRIVAL_TIME) {
            return;
        }
        int place = Math.min(arrival, NAMES.length - 1);
        float fade = Math.min(1f, Math.min(arrivalTime * 2f, (ARRIVAL_TIME - arrivalTime) * 1.5f));
        float px = x + PLACE_X[place];
        float py = PLACE_TOP[place] - 0.4f;
        for (int i = 0; i < 8; i++) {
            float a = i * 45f + t * 90f;
            float r = 0.35f + 0.1f * MathUtils.sin(t * 6f + i);
            d.shape(b, d.star, px + MathUtils.cosDeg(a) * r, py + MathUtils.sinDeg(a) * r, 0.06f, t * 200f, SUN, fade);
        }
        String text = arrival == NAMES.length ? "THE KINGDOM IS SAVED!" : "NEW PLACE: " + NAMES[place];
        float tw = game.textWidth(text, 0.32f);
        d.rect(b, 3.5f - tw / 2f - 0.12f, 3.42f, tw + 0.24f, 0.36f, FRAME, 0.8f * fade); // just under the title
        game.text(text, 3.5f, 3.71f, 0.32f, tmp.set(SUN).mul(1, 1, 1, fade), Align.center);
    }

    private static void cloud(SpriteBatch b, Draw d, float x, float y, float s, Color c, float alpha) {
        d.circle(b, x, y, s, c, alpha);
        d.circle(b, x + s * 0.9f, y - s * 0.15f, s * 0.75f, c, alpha);
        d.circle(b, x - s * 0.9f, y - s * 0.2f, s * 0.65f, c, alpha);
    }

    private void drawSun(SpriteBatch b, Draw d, float sx, float sy, float t) {
        for (int i = 0; i < 8; i++) {
            float a = i * 45f + t * 15f;
            d.rotRect(b, sx + MathUtils.cosDeg(a) * 0.42f, sy + MathUtils.sinDeg(a) * 0.42f, 0.15f, 0.05f, a, SUN, 0.8f);
        }
        d.circle(b, sx, sy, 0.3f, SUN);
        d.circle(b, sx - 0.09f, sy + 0.06f, 0.032f, FRAME);
        d.circle(b, sx + 0.09f, sy + 0.06f, 0.032f, FRAME);
        d.rotRect(b, sx - 0.05f, sy - 0.08f, 0.1f, 0.03f, -25f, FRAME, 1f);
        d.rotRect(b, sx + 0.05f, sy - 0.08f, 0.1f, 0.03f, 25f, FRAME, 1f);
    }

    /** Floor 1: thatched cottages and a windmill turning. */
    private void drawVillage(SpriteBatch b, Draw d, float cx, float t) {
        locked = reached < 1;
        float mx = cx + 0.55f;
        d.rect(b, mx - 0.12f, GROUND, 0.24f, 0.62f, c(WALL));
        d.shape(b, d.triangle, mx, GROUND + 0.68f, 0.17f, 90f, c(ROOF), 1f);
        for (int i = 0; i < 4; i++) {
            float a = t * (locked ? 0f : 60f) + i * 90f;
            d.rotRect(b, mx + MathUtils.cosDeg(a) * 0.22f, GROUND + 0.55f + MathUtils.sinDeg(a) * 0.22f, 0.42f, 0.07f, a,
                    c(WOOD), 1f);
        }
        d.circle(b, mx, GROUND + 0.55f, 0.04f, c(FRAME));
        for (int i = 0; i < 2; i++) {
            float hx = cx - 0.75f + i * 0.6f;
            d.rect(b, hx, GROUND - 0.05f, 0.46f, 0.3f, c(WALL));
            d.shape(b, d.triangle, hx + 0.23f, GROUND + 0.33f, 0.28f, 90f, c(THATCH), 1f);
            d.rect(b, hx + 0.17f, GROUND - 0.05f, 0.11f, 0.17f, c(ROOF));
        }
    }

    /** Floor 2: rows of houses and a clock tower. */
    private void drawTown(SpriteBatch b, Draw d, float cx) {
        locked = reached < 2;
        d.rect(b, cx - 0.1f, GROUND, 0.22f, 1.0f, c(TOWN_B));
        d.shape(b, d.triangle, cx + 0.01f, GROUND + 1.07f, 0.16f, 90f, c(ROOF), 1f);
        d.circle(b, cx + 0.01f, GROUND + 0.8f, 0.07f, c(Color.WHITE));
        d.rotRect(b, cx + 0.01f, GROUND + 0.82f, 0.012f, 0.05f, 0f, c(FRAME), 1f);
        d.rotRect(b, cx + 0.025f, GROUND + 0.8f, 0.04f, 0.012f, 0f, c(FRAME), 1f);
        for (int i = 0; i < 4; i++) {
            float hx = cx - 0.85f + i * 0.45f + (i >= 2 ? 0.3f : 0f);
            float hh = 0.38f + (i % 2) * 0.15f;
            d.rect(b, hx, GROUND, 0.36f, hh, c(i % 2 == 0 ? TOWN_A : TOWN_B));
            d.shape(b, d.triangle, hx + 0.18f, GROUND + hh + 0.07f, 0.21f, 90f, c(ROOF), 1f);
            d.rect(b, hx + 0.12f, GROUND + 0.12f, 0.1f, 0.1f, c(WINDOW));
        }
    }

    /** Floor 3: the city at night - its own patch of dark sky with a moon, lit windows. */
    private void drawCity(SpriteBatch b, Draw d, float cx, float t) {
        locked = reached < 3;
        d.circle(b, cx + 0.05f, GROUND + 0.35f, 0.85f, c(NIGHT), 0.85f);
        d.circle(b, cx + 0.42f, GROUND + 0.95f, 0.09f, c(WINDOW));
        d.circle(b, cx + 0.46f, GROUND + 0.98f, 0.075f, c(NIGHT));
        for (int i = 0; i < 5; i++) {
            float sx = cx - 0.6f + i * 0.27f;
            d.circle(b, sx, GROUND + 0.95f + (i * 7 % 3) * 0.1f, 0.012f, c(Color.WHITE),
                    0.5f + 0.5f * MathUtils.sin(t * 3f + i));
        }
        float[][] towers = {{-0.62f, 0.75f}, {-0.3f, 1.05f}, {0.05f, 0.85f}, {0.38f, 0.65f}};
        for (float[] tw : towers) {
            float tx = cx + tw[0];
            d.rect(b, tx, GROUND, 0.28f, tw[1], c(CITY));
            d.rect(b, tx, GROUND, 0.06f, tw[1], c(CITY_DARK));
            for (float wy = GROUND + 0.1f; wy < GROUND + tw[1] - 0.1f; wy += 0.17f) {
                d.rect(b, tx + 0.1f, wy, 0.05f, 0.06f, c(WINDOW));
                d.rect(b, tx + 0.18f, wy, 0.05f, 0.06f, c(WINDOW));
            }
        }
    }

    /** Floor 4: the glass tower, its roof light blinking. */
    private void drawSkyscraper(SpriteBatch b, Draw d, float cx, float t) {
        locked = reached < 4;
        float top = 2.15f;
        d.rect(b, cx - 0.24f, GROUND, 0.48f, top, c(GLASS));
        d.rect(b, cx - 0.24f, GROUND, 0.09f, top, c(STEEL));
        for (float wy = GROUND + 0.12f; wy < GROUND + top - 0.05f; wy += 0.16f) {
            d.rect(b, cx - 0.14f, wy, 0.35f, 0.03f, c(STEEL));
        }
        d.rect(b, cx - 0.01f, GROUND + top, 0.02f, 0.18f, c(STEEL));
        boolean blink = !locked && MathUtils.sin(t * 5f) > 0;
        d.circle(b, cx, GROUND + top + 0.19f, 0.03f, blink ? Color.RED : c(STEEL));
    }

    /**
     * Floor 5: the Fluffy King's castle on its cloud. Reached but not won: storm clouds hang over it
     * and lightning flickers. Won: bright, flags flying, the rainbow back.
     */
    private void drawCastle(SpriteBatch b, Draw d, float cx, float base, float t) {
        locked = reached < 5;
        boolean stormy = !locked && !kingBeaten;
        float bob = MathUtils.sin(t * 1.2f) * 0.04f;
        base += bob;
        if (kingBeaten) { // the rainbow, arching down from the castle to the hills
            for (int band = 0; band < RAINBOW.length; band++) {
                for (int i = 0; i <= 30; i++) {
                    float s = i / 30f;
                    float px = cx - 0.3f - s * 1.9f;
                    float py = base - 0.1f - s * (base - GROUND - 0.2f) + MathUtils.sin(s * MathUtils.PI) * 0.45f
                            + (band - 2.5f) * 0.045f;
                    d.circle(b, px, py, 0.03f, RAINBOW[band], 0.85f);
                }
            }
        }
        // (own colours: c() hands back one shared colour, and these are used all the way down)
        Color wall = wallColor.set(stormy ? tmp.set(CASTLE).lerp(STORM, 0.55f) : c(CASTLE));
        Color dark = darkColor.set(stormy ? tmp.set(CASTLE_DARK).lerp(STORM, 0.55f) : c(CASTLE_DARK));
        for (int i = -1; i <= 1; i++) {
            float tx = cx + i * 0.32f;
            float th = i == 0 ? 0.62f : 0.44f;
            d.rect(b, tx - 0.09f, base, 0.18f, th, wall);
            d.rect(b, tx - 0.09f, base, 0.04f, th, dark);
            d.shape(b, d.triangle, tx, base + th + 0.06f, 0.12f, 90f, stormy ? STORM : c(CASTLE_ROOF), 1f);
            if (kingBeaten) {
                d.rect(b, tx - 0.005f, base + th + 0.12f, 0.012f, 0.1f, FRAME);
                d.rect(b, tx + 0.008f, base + th + 0.17f + MathUtils.sin(t * 4f + i) * 0.008f, 0.08f, 0.045f, HILL_PINK);
            }
        }
        d.rect(b, cx - 0.3f, base, 0.6f, 0.3f, wall);
        d.rect(b, cx - 0.06f, base, 0.12f, 0.16f, dark);
        Color puff = stormy ? STORM : c(Color.WHITE);
        cloud(b, d, cx - 0.2f, base - 0.05f, 0.2f, puff, 1f);
        cloud(b, d, cx + 0.25f, base - 0.08f, 0.18f, puff, 1f);
        if (stormy) { // the King's storm hangs over it
            for (int i = 0; i < 3; i++) {
                cloud(b, d, cx - 0.35f + i * 0.35f + MathUtils.sin(t * 0.7f + i) * 0.05f, base + 0.95f, 0.18f, STORM, 0.95f);
            }
            if ((t * 0.6f) % 3f < 0.12f) { // a flash of lightning now and then
                float lx = cx + 0.15f;
                d.rotRect(b, lx, base + 0.75f, 0.03f, 0.22f, -20f, SUN, 1f);
                d.rotRect(b, lx + 0.05f, base + 0.58f, 0.03f, 0.2f, 20f, SUN, 1f);
            }
        }
    }

    /** Soft rolling hills along the bottom, dotted with flowers. */
    private void drawHills(SpriteBatch b, Draw d, float x, float t) {
        d.rect(b, x - 0.5f, 0, WIDTH + 1f, GROUND - 0.4f, GRASS_DARK);
        d.circle(b, x + 1.0f, -1.3f, 2.75f, GRASS);
        d.circle(b, x + 3.6f, -1.8f, 3.2f, HILL_PINK);
        d.circle(b, x + 6.2f, -1.4f, 2.8f, GRASS_DARK);
        d.circle(b, x + 8.6f, -1.6f, 2.6f, GRASS);
        for (int i = 0; i < 24; i++) {
            float fx = x + 0.2f + i * 0.37f;
            float fy = 0.3f + (i * 7 % 5) * 0.12f;
            d.circle(b, fx, fy + MathUtils.sin(t * 2f + i) * 0.01f, 0.03f, FLOWERS[i % FLOWERS.length]);
        }
    }

    /** The sea at the bottom right, under the clouds - a shark fin cruising it once the secret's been found. */
    private void drawSea(SpriteBatch b, Draw d, float x, float t) {
        float sx = x + 6.9f;
        float top = 0.55f;
        d.rect(b, sx - 0.12f, 0, WIDTH - 6.9f + 0.2f, top + 0.06f, SAND); // a sandy shore round it
        d.rect(b, sx, 0, WIDTH - 6.9f + 0.1f, top, SEA);
        for (int i = 0; i < 7; i++) {
            float wx = sx + 0.15f + i * 0.3f + MathUtils.sin(t * 1.5f + i) * 0.05f;
            d.rotRect(b, wx, top - 0.08f - (i % 2) * 0.18f, 0.14f, 0.025f, 0f, SEA_LIGHT, 0.8f);
        }
        if (Unlocks.foundSea) {
            float fx = sx + 0.3f + ((t * 0.15f) % 1f) * 1.6f;
            float fy = 0.2f;
            d.shape(b, d.triangle, fx, fy + 0.08f, 0.12f, 90f, FIN, 1f);
            d.rotRect(b, fx - 0.12f, fy, 0.2f, 0.025f, 0f, SEA_LIGHT, 0.9f);
        }
    }
}
