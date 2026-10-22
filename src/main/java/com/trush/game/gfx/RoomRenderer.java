package com.trush.game.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.trush.game.world.Arena;
import com.trush.game.world.Room;

/**
 * Bakes a room's floor, walls and door openings from its theme's tileset
 * (assets/tiles/&lt;theme&gt;.png, 16 px tiles: floor a/b/c/d, wall face, cap, door frame, unused,
 * wall face with a window, wall face with a decoration) into one 224x160 texture,
 * and draws a barricade over the doors of a locked room.
 */
public class RoomRenderer implements Disposable {

    private static final int T = 16;
    private static final int W = 224;
    private static final int H = 160;
    private static final int PPU = 32;

    private static final int FLOOR_A = 0;
    private static final int WALL_FACE = 4;
    private static final int WALL_TOP = 5;
    private static final int WALL_WINDOW = 8;
    private static final int WALL_DECO = 9;

    private static final Color PLANK = Color.valueOf("8a5a2b");
    private static final Color PLANK_HI = Color.valueOf("b07d4c");
    private static final Color PLANK_DARK = Color.valueOf("3e2818");
    private static final Color BARRIER_RED = Color.valueOf("d63a2f");
    private static final Color BARRIER_WHITE = Color.valueOf("f5f0e6");
    private static final Color BARRIER_DARK = Color.valueOf("2a2a30");

    private final ObjectMap<String, Pixmap> tilesets = new ObjectMap<>();
    private Texture current;
    private Room currentRoom;

    private Pixmap tileset(String theme) {
        Pixmap p = tilesets.get(theme);
        if (p == null) {
            p = new Pixmap(Gdx.files.internal("assets/tiles/" + theme + ".png"));
            tilesets.put(theme, p);
        }
        return p;
    }

    private static int floorVariant(int x, int y, int seed) {
        int h = (x * 73856093) ^ (y * 19349663) ^ (seed * 83492791);
        h = (h >>> 3) % 100;
        return h < 55 ? 0 : h < 80 ? 1 : h < 93 ? 2 : 3;
    }

    private Texture bake(Room room) {
        Pixmap ts = tileset(room.theme);
        Pixmap pm = new Pixmap(W, H, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        int seed = room.gx * 31 + room.gy * 17 + room.theme.hashCode();

        // wall caps everywhere first
        for (int y = 0; y < H; y += T) {
            for (int x = 0; x < W; x += T) {
                pm.drawPixmap(ts, WALL_TOP * T, 0, T, T, x, y, T, T);
            }
        }
        // floor: px x 8..216, y 24..152
        for (int j = 0; j < 8; j++) {
            for (int i = 0; i < 13; i++) {
                int v = floorVariant(i, j, seed);
                pm.drawPixmap(ts, (FLOOR_A + v) * T, 0, T, T, 8 + i * T, 24 + j * T, T, T);
            }
        }
        // top wall face: plain, with the odd window or decoration
        for (int i = 0; i < 13; i++) {
            int k = Math.floorMod(i * 7 + seed, 5);
            int face = k == 1 ? WALL_WINDOW : (k == 3 && i % 3 == 0) ? WALL_DECO : WALL_FACE;
            pm.drawPixmap(ts, face * T, 0, T, T, 8 + i * T, 8, T, T);
        }
        // door openings carry the floor through the wall
        int dx0 = (int) (Arena.DOOR_X0 * PPU);
        int dy0 = (int) ((Arena.WORLD_H - Arena.DOOR_Y1) * PPU);
        if (room.doors[Room.N]) {
            for (int i = 0; i < 2; i++) {
                pm.drawPixmap(ts, FLOOR_A * T, 0, T, T, dx0 + i * T, 8, T, T);
                pm.drawPixmap(ts, FLOOR_A * T, 0, T, 8, dx0 + i * T, 0, T, 8);
            }
            darken(pm, dx0, 0, 32, 10, 0.55f);
            darken(pm, dx0 - 3, 0, 3, 24, 0.5f);
            darken(pm, dx0 + 32, 0, 3, 24, 0.5f);
        }
        if (room.doors[Room.S]) {
            for (int i = 0; i < 2; i++) {
                pm.drawPixmap(ts, FLOOR_A * T, 0, T, 8, dx0 + i * T, 152, T, 8);
            }
            darken(pm, dx0, 156, 32, 4, 0.6f);
        }
        if (room.doors[Room.W]) {
            for (int j = 0; j < 2; j++) {
                pm.drawPixmap(ts, FLOOR_A * T, 0, 8, T, 0, dy0 + j * T, 8, T);
            }
            darken(pm, 0, dy0, 4, 32, 0.6f);
        }
        if (room.doors[Room.E]) {
            for (int j = 0; j < 2; j++) {
                pm.drawPixmap(ts, FLOOR_A * T, 0, 8, T, 216, dy0 + j * T, 8, T);
            }
            darken(pm, 220, dy0, 4, 32, 0.6f);
        }
        // soft shadow under the top wall
        darken(pm, 8, 24, 208, 3, 0.75f);

        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pm.dispose();
        return t;
    }

    private static void darken(Pixmap pm, int x0, int y0, int w, int h, float k) {
        Color c = new Color();
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                if (x < 0 || y < 0 || x >= pm.getWidth() || y >= pm.getHeight()) continue;
                Color.rgba8888ToColor(c, pm.getPixel(x, y));
                c.mul(k, k, k, 1f);
                pm.drawPixel(x, y, Color.rgba8888(c));
            }
        }
    }

    /** Draws the room background (re-baking it when the room changes). */
    public void drawFloor(SpriteBatch b, Room room) {
        if (room != currentRoom || current == null) {
            if (current != null) {
                current.dispose();
            }
            current = bake(room);
            currentRoom = room;
        }
        b.setColor(Color.WHITE);
        b.draw(current, 0, 0, Arena.WORLD_W, Arena.WORLD_H);
    }

    /** A barricade across every door of a locked room: planks in the village, road barriers in the town. */
    public void drawBars(SpriteBatch b, Room room, Draw d) {
        if (!room.isLocked()) {
            return;
        }
        if (room.stage.roadBarriers) {
            drawRoadBarriers(b, room, d);
            return;
        }
        float w = Arena.DOOR_X1 - Arena.DOOR_X0;
        for (int dir = 0; dir < 4; dir++) {
            if (!room.doors[dir]) continue;
            switch (dir) {
                case Room.N:
                    planks(b, d, Arena.DOOR_X0, Arena.TOP, w, 0.55f, true);
                    break;
                case Room.S:
                    planks(b, d, Arena.DOOR_X0, 0, w, Arena.BOTTOM + 0.05f, true);
                    break;
                case Room.W:
                    planks(b, d, 0, Arena.DOOR_Y0, Arena.LEFT + 0.05f, w, false);
                    break;
                default:
                    planks(b, d, Arena.RIGHT - 0.05f, Arena.DOOR_Y0, Arena.LEFT + 0.05f, w, false);
                    break;
            }
        }
    }

    private static void drawRoadBarriers(SpriteBatch b, Room room, Draw d) {
        float w = Arena.DOOR_X1 - Arena.DOOR_X0;
        for (int dir = 0; dir < 4; dir++) {
            if (!room.doors[dir]) continue;
            boolean horizontal = dir == Room.N || dir == Room.S;
            float x = horizontal ? Arena.DOOR_X0 - 0.04f : dir == Room.W ? 0f : Arena.RIGHT - 0.05f;
            float y = horizontal ? (dir == Room.N ? Arena.TOP + 0.12f : 0.02f) : Arena.DOOR_Y0 - 0.04f;
            float len = w + 0.08f;
            float bw = horizontal ? len : Arena.LEFT + 0.05f;
            float bh = horizontal ? 0.16f : len;
            d.rect(b, x - 0.02f, y - 0.02f, bw + 0.04f, bh + 0.04f, BARRIER_DARK);
            // red and white stripes along the board
            float step = 0.14f;
            for (float o = 0; o < len; o += step) {
                Color c = ((int) (o / step)) % 2 == 0 ? BARRIER_RED : BARRIER_WHITE;
                float seg = Math.min(step, len - o);
                if (horizontal) d.rect(b, x + o, y, seg, bh, c);
                else d.rect(b, x, y + o, bw, seg, c);
            }
            if (dir == Room.N) { // the top door is tall: a second board
                d.rect(b, x - 0.02f, y + 0.24f, bw + 0.04f, bh + 0.04f, BARRIER_DARK);
                for (float o = 0; o < len; o += step) {
                    Color c = ((int) (o / step)) % 2 == 1 ? BARRIER_RED : BARRIER_WHITE;
                    d.rect(b, x + o, y + 0.26f, Math.min(step, len - o), bh, c);
                }
            }
        }
    }

    /** Planks nailed across a door gap (horizontal door: boards run across it, plus a diagonal one). */
    private static void planks(SpriteBatch b, Draw d, float x, float y, float w, float h, boolean horizontal) {
        float t = 0.09f;
        if (horizontal) {
            for (float py = y + 0.04f; py < y + h - 0.02f; py += 0.17f) {
                d.rect(b, x - 0.04f, py - 0.01f, w + 0.08f, t + 0.02f, PLANK_DARK);
                d.rect(b, x - 0.03f, py, w + 0.06f, t, PLANK);
                d.rect(b, x - 0.03f, py + t - 0.02f, w + 0.06f, 0.02f, PLANK_HI);
            }
            if (h > 0.3f) {
                float len = (float) Math.sqrt(w * w + h * h);
                float ang = (float) Math.toDegrees(Math.atan2(h, w));
                d.line(b, x, y + 0.03f, len, t + 0.03f, ang, PLANK_DARK);
                d.line(b, x, y + 0.03f, len, t, ang, PLANK_HI);
            }
        } else {
            for (float py = y + 0.05f; py < y + h; py += 0.2f) {
                d.rect(b, x - 0.01f, py - 0.01f, w + 0.02f, t + 0.02f, PLANK_DARK);
                d.rect(b, x, py, w, t, PLANK);
            }
        }
    }

    @Override
    public void dispose() {
        if (current != null) {
            current.dispose();
        }
        for (Pixmap p : tilesets.values()) {
            p.dispose();
        }
        tilesets.clear();
    }
}
