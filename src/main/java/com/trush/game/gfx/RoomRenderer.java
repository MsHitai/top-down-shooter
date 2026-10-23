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
import com.trush.game.world.Stage;

/**
 * Bakes a room's floor, walls and door openings from its theme's tileset
 * (assets/tiles/&lt;theme&gt;.png, 16 px tiles: floor a/b/c/d, wall face, cap, door frame, unused,
 * wall face with a window, wall face with a decoration) into one texture the size of the room
 * (224x160 for a one-screen room),
 * and draws a barricade over the doors of a locked room.
 */
public class RoomRenderer implements Disposable {

    private static final int T = 16;
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
    private static final Color TAPE_YELLOW = Color.valueOf("f2d23a");

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
        Arena a = room.arena;
        int W = Math.round(a.worldW * PPU);
        int H = Math.round(a.worldH * PPU);
        int floorW = a.cols * T;
        Pixmap pm = new Pixmap(W, H, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        int seed = room.gx * 31 + room.gy * 17 + room.theme.hashCode();

        // wall caps everywhere first
        for (int y = 0; y < H; y += T) {
            for (int x = 0; x < W; x += T) {
                pm.drawPixmap(ts, WALL_TOP * T, 0, T, T, x, y, T, T);
            }
        }
        // floor: from x 8 px, y 24 px (below the top wall), one 16 px tile per layout cell
        for (int j = 0; j < a.rows; j++) {
            for (int i = 0; i < a.cols; i++) {
                int v = floorVariant(i, j, seed);
                pm.drawPixmap(ts, (FLOOR_A + v) * T, 0, T, T, 8 + i * T, 24 + j * T, T, T);
            }
        }
        // top wall face: plain, with the odd window or decoration
        for (int i = 0; i < a.cols; i++) {
            int k = Math.floorMod(i * 7 + seed, 5);
            int face = k == 1 ? WALL_WINDOW : (k == 3 && i % 3 == 0) ? WALL_DECO : WALL_FACE;
            pm.drawPixmap(ts, face * T, 0, T, T, 8 + i * T, 8, T, T);
        }
        // door openings carry the floor through the wall
        int dx0 = (int) (a.doorX0 * PPU);
        int dy0 = (int) ((a.worldH - a.doorY1) * PPU);
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
                pm.drawPixmap(ts, FLOOR_A * T, 0, T, 8, dx0 + i * T, H - 8, T, 8);
            }
            darken(pm, dx0, H - 4, 32, 4, 0.6f);
        }
        if (room.doors[Room.W]) {
            for (int j = 0; j < 2; j++) {
                pm.drawPixmap(ts, FLOOR_A * T, 0, 8, T, 0, dy0 + j * T, 8, T);
            }
            darken(pm, 0, dy0, 4, 32, 0.6f);
        }
        if (room.doors[Room.E]) {
            for (int j = 0; j < 2; j++) {
                pm.drawPixmap(ts, FLOOR_A * T, 0, 8, T, W - 8, dy0 + j * T, 8, T);
            }
            darken(pm, W - 4, dy0, 4, 32, 0.6f);
        }
        if ("helipad".equals(room.theme)) {
            paintHelipad(pm, W / 2, (H - 8 + 24) / 2);
        }
        // soft shadow under the top wall
        darken(pm, 8, 24, floorW, 3, 0.75f);

        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pm.dispose();
        return t;
    }

    /** The landing marking in the middle of the helipad: a yellow circle with a white H. */
    private static void paintHelipad(Pixmap pm, int cx, int cy) {
        pm.setColor(Color.valueOf("e8c030"));
        for (int r = 44; r <= 47; r++) {
            pm.drawCircle(cx, cy, r);
        }
        pm.setColor(Color.valueOf("e8e8e8"));
        pm.fillRectangle(cx - 18, cy - 22, 7, 44);
        pm.fillRectangle(cx + 11, cy - 22, 7, 44);
        pm.fillRectangle(cx - 11, cy - 3, 22, 7);
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
        b.draw(current, 0, 0, room.arena.worldW, room.arena.worldH);
    }

    /** A barricade across every door of a locked room: planks in the village, road barriers in the town. */
    public void drawBars(SpriteBatch b, Room room, Draw d) {
        if (!room.isLocked()) {
            return;
        }
        if (room.stage.barricade == Stage.Barricade.ROAD) {
            drawRoadBarriers(b, room, d);
            return;
        }
        if (room.stage.barricade == Stage.Barricade.TAPE) {
            drawPoliceTape(b, room, d);
            return;
        }
        float w = room.arena.doorX1 - room.arena.doorX0;
        for (int dir = 0; dir < 4; dir++) {
            if (!room.doors[dir]) continue;
            switch (dir) {
                case Room.N:
                    planks(b, d, room.arena.doorX0, room.arena.top, w, 0.55f, true);
                    break;
                case Room.S:
                    planks(b, d, room.arena.doorX0, 0, w, room.arena.bottom + 0.05f, true);
                    break;
                case Room.W:
                    planks(b, d, 0, room.arena.doorY0, room.arena.left + 0.05f, w, false);
                    break;
                default:
                    planks(b, d, room.arena.right - 0.05f, room.arena.doorY0, room.arena.left + 0.05f, w, false);
                    break;
            }
        }
    }

    /** Two strips of yellow-and-black police tape crossing each door gap. */
    private static void drawPoliceTape(SpriteBatch b, Room room, Draw d) {
        Arena a = room.arena;
        for (int dir = 0; dir < 4; dir++) {
            if (!room.doors[dir]) continue;
            float x0;
            float y0;
            float w;
            float h;
            switch (dir) {
                case Room.N:
                    x0 = a.doorX0; y0 = a.top; w = a.doorX1 - a.doorX0; h = 0.55f;
                    break;
                case Room.S:
                    x0 = a.doorX0; y0 = 0f; w = a.doorX1 - a.doorX0; h = a.bottom + 0.05f;
                    break;
                case Room.W:
                    x0 = 0f; y0 = a.doorY0; w = a.left + 0.05f; h = a.doorY1 - a.doorY0;
                    break;
                default:
                    x0 = a.right - 0.05f; y0 = a.doorY0; w = a.left + 0.05f; h = a.doorY1 - a.doorY0;
                    break;
            }
            for (int k = 0; k < 2; k++) { // an X of tape
                float sx = k == 0 ? x0 : x0 + w;
                float ang = (float) Math.toDegrees(Math.atan2(h, k == 0 ? w : -w));
                float len = (float) Math.sqrt(w * w + h * h);
                d.line(b, sx, y0, len, 0.1f, ang, TAPE_YELLOW);
                for (float t = 0.05f; t < len; t += 0.2f) { // black marks along it
                    float mx = sx + (float) Math.cos(Math.toRadians(ang)) * t;
                    float my = y0 + (float) Math.sin(Math.toRadians(ang)) * t;
                    d.rotRect(b, mx, my, 0.06f, 0.07f, ang, BARRIER_DARK, 1f);
                }
            }
        }
    }

    private static void drawRoadBarriers(SpriteBatch b, Room room, Draw d) {
        float w = room.arena.doorX1 - room.arena.doorX0;
        for (int dir = 0; dir < 4; dir++) {
            if (!room.doors[dir]) continue;
            boolean horizontal = dir == Room.N || dir == Room.S;
            float x = horizontal ? room.arena.doorX0 - 0.04f : dir == Room.W ? 0f : room.arena.right - 0.05f;
            float y = horizontal ? (dir == Room.N ? room.arena.top + 0.12f : 0.02f) : room.arena.doorY0 - 0.04f;
            float len = w + 0.08f;
            float bw = horizontal ? len : room.arena.left + 0.05f;
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
