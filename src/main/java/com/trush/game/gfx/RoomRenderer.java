package com.trush.game.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.TimeUtils;
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
    private static final Color SHUTTER_STEEL = Color.valueOf("9aa0aa");
    private static final Color CLOUD_WHITE = Color.valueOf("fbfaff");
    private static final Color CLOUD_SHADE = Color.valueOf("b8c0e0");
    private static final Color KELP = Color.valueOf("4a9a4a");
    private static final Color KELP_DARK = Color.valueOf("1e4a2a");
    private static final Color FIELD = Color.valueOf("6ad8ff");

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

        if (a.isShaped()) {
            paintShapedWalls(pm, ts, room, seed);
        } else {
            for (int y = 0; y < H; y += T) {
                for (int x = 0; x < W; x += T) {
                    pm.drawPixmap(ts, WALL_TOP * T, 0, T, T, x, y, T, T);
                }
            }
        }
        // floor: from x 8 px, y 24 px (below the top wall), one 16 px tile per layout cell
        for (int j = 0; j < a.rows; j++) {
            int row = a.rows - 1 - j;
            for (int i = a.floorStart(row); i < a.floorEnd(row); i++) {
                int v = floorVariant(i, j, seed);
                pm.drawPixmap(ts, (FLOOR_A + v) * T, 0, T, T, 8 + i * T, 24 + j * T, T, T);
            }
        }
        if (!a.isShaped()) {
            for (int i = 0; i < a.cols; i++) {
                pm.drawPixmap(ts, wallFace(i, seed) * T, 0, T, T, 8 + i * T, 8, T, T);
            }
            if (room.stage.isSky()) paintCloudRim(pm, ts, 8, 24, floorW, a.rows * T);
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
        } else if ("plaza".equals(room.theme)) {
            Vector2 center = new Vector2(a.centerX, a.centerY);
            a.clamp(center, 56f / PPU);
            paintPlaza(pm, Math.round(center.x * PPU), H - Math.round(center.y * PPU));
        } else if ("throne".equals(room.theme)) {
            paintCarpet(pm, W / 2, 24, H);
        } else if (room.theme.startsWith("bed_") && !"bed_dust".equals(room.theme)
                && !"bed_slats".equals(room.theme)) {
            paintBedRug(pm, room.theme, floorW, a.rows * T);
        }
        if (a.isShaped()) {
            shadeShapedFloor(pm, room, dy0);
        } else {
            // Stepped contact shadows anchor the walls without darkening the fighting area.
            // Open sky has a cloud rim instead of side walls.
            darken(pm, 8, 24, floorW, 2, 0.62f);
            darken(pm, 8, 26, floorW, 2, 0.80f);
            darken(pm, 8, 28, floorW, 2, 0.92f);
            if (!room.stage.isSky()) {
                int floorH = a.rows * T;
                for (int j = 0; j < floorH; j++) {
                    int py = 24 + j;
                    // Leave side door thresholds clear so the way out remains visible.
                    if (!(py >= dy0 && py < dy0 + 32 && room.doors[Room.W])) {
                        darken(pm, 8, py, 2, 1, 0.72f);
                        darken(pm, 10, py, 2, 1, 0.90f);
                    }
                    if (!(py >= dy0 && py < dy0 + 32 && room.doors[Room.E])) {
                        darken(pm, 8 + floorW - 2, py, 2, 1, 0.72f);
                        darken(pm, 8 + floorW - 4, py, 2, 1, 0.90f);
                    }
                }
            }
        }

        Texture t = new Texture(pm);
        t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pm.dispose();
        return t;
    }

    private static int wallFace(int col, int seed) {
        int k = Math.floorMod(col * 7 + seed, 5);
        return k == 1 ? WALL_WINDOW : (k == 3 && col % 3 == 0) ? WALL_DECO : WALL_FACE;
    }

    private static void paintShapedWalls(Pixmap pm, Pixmap ts, Room room, int seed) {
        Arena a = room.arena;
        int cap = WALL_TOP * T;
        if (room.stage.isSky()) {
            paintShapedCloudRim(pm, ts, a);
        } else {
            for (int row = 0; row < a.rows; row++) {
                int y = 24 + (a.rows - 1 - row) * T;
                int x0 = 8 + a.floorStart(row) * T;
                int x1 = 8 + a.floorEnd(row) * T;
                pm.drawPixmap(ts, cap, 0, 8, T, x0 - 8, y, 8, T);
                pm.drawPixmap(ts, cap + 8, 0, 8, T, x1, y, 8, T);
                for (int col = a.floorStart(row); col < a.floorEnd(row); col++) {
                    if (!a.hasFloor(col, row - 1)) {
                        pm.drawPixmap(ts, cap, 8, T, 8, 8 + col * T, y + T, T, 8);
                    }
                }
            }
        }
        for (int row = a.rows - 1; row >= 0; row--) {
            int y = 24 + (a.rows - 1 - row) * T;
            for (int col = a.floorStart(row); col < a.floorEnd(row); col++) {
                if (a.hasFloor(col, row + 1)) continue;
                int x = 8 + col * T;
                pm.drawPixmap(ts, cap, 0, T, 8, x, y - 24, T, 8);
                pm.drawPixmap(ts, wallFace(col, seed) * T, 0, T, T, x, y - T, T, T);
            }
        }
    }

    private static void shadeShapedFloor(Pixmap pm, Room room, int sideDoorY) {
        Arena a = room.arena;
        for (int row = 0; row < a.rows; row++) {
            int y = 24 + (a.rows - 1 - row) * T;
            int start = a.floorStart(row);
            int end = a.floorEnd(row);
            for (int col = start; col < end; col++) {
                if (!a.hasFloor(col, row + 1) && !(row == a.rows - 1 && room.doors[Room.N]
                        && col >= a.cols / 2 - 1 && col <= a.cols / 2 + 1)) {
                    int x = 8 + col * T;
                    darken(pm, x, y, T, 2, 0.62f);
                    darken(pm, x, y + 2, T, 2, 0.80f);
                    darken(pm, x, y + 4, T, 2, 0.92f);
                }
            }
            if (room.stage.isSky()) continue;
            for (int py = y; py < y + T; py++) {
                boolean atDoor = py >= sideDoorY && py < sideDoorY + 32;
                if (!(atDoor && start == 0 && room.doors[Room.W])) {
                    darken(pm, 8 + start * T, py, 2, 1, 0.72f);
                    darken(pm, 10 + start * T, py, 2, 1, 0.90f);
                }
                if (!(atDoor && end == a.cols && room.doors[Room.E])) {
                    darken(pm, 6 + end * T, py, 2, 1, 0.72f);
                    darken(pm, 4 + end * T, py, 2, 1, 0.90f);
                }
            }
        }
    }

    private static void paintShapedCloudRim(Pixmap pm, Pixmap ts, Arena a) {
        Color base = new Color(commonColor(ts, FLOOR_A * T));
        Color shade = base.cpy().mul(0.78f, 0.8f, 0.9f, 1f);
        for (int pass = 0; pass < 2; pass++) {
            int drop = pass == 0 ? 3 : 0;
            pm.setColor(pass == 0 ? shade : base);
            for (int row = 0; row < a.rows; row++) {
                int y = 24 + (a.rows - 1 - row) * T;
                int start = a.floorStart(row);
                int end = a.floorEnd(row);
                for (int i = 4; i < T; i += 8) {
                    pm.fillCircle(7 + start * T, y + i + drop, 6);
                    pm.fillCircle(8 + end * T, y + i + drop, 6);
                }
                for (int col = start; col < end; col++) {
                    if (!a.hasFloor(col, row - 1)) {
                        pm.fillCircle(12 + col * T, y + T + drop, 6);
                        pm.fillCircle(20 + col * T, y + T + drop, 6);
                    }
                }
            }
        }
    }

    /** Faded bedroom rugs: large woven shapes stay behind the bright actors and bullets. */
    private static void paintBedRug(Pixmap pm, String theme, int floorW, int floorH) {
        int x0 = 8 + 24, y0 = 24 + 24;
        int w = floorW - 48, h = floorH - 48;
        if (w < 48 || h < 48) return;
        Color cloth = Color.valueOf(switch (theme) {
            case "bed_yarn" -> "574353";
            case "bed_sock" -> "45465b";
            case "bed_tin" -> "4b4c50";
            case "bed_court" -> "51435c";
            default -> "514858";
        });
        Color edge = cloth.cpy().mul(.76f, .76f, .76f, 1);
        Color thread = cloth.cpy().mul(1.22f, 1.22f, 1.22f, 1);
        Color weave = cloth.cpy().mul(1.06f, 1.06f, 1.06f, 1);
        pm.setColor(edge);
        pm.fillRectangle(x0 - 2, y0 + 2, w + 4, h + 2);
        pm.setColor(cloth);
        pm.fillRectangle(x0, y0, w, h);
        // A doubled woven border and fringe give the pattern a room-sized frame.
        pm.setColor(edge);
        pm.drawRectangle(x0 + 4, y0 + 4, w - 8, h - 8);
        pm.drawRectangle(x0 + 9, y0 + 9, w - 18, h - 18);
        pm.setColor(thread);
        for (int x = x0 + 6; x < x0 + w - 4; x += 6) {
            pm.fillRectangle(x, y0 + 6, 3, 1);
            pm.fillRectangle(x, y0 + h - 7, 3, 1);
            pm.fillRectangle(x, y0 - 3, 1, 3);
            pm.fillRectangle(x, y0 + h, 1, 3);
        }
        for (int y = y0 + 6; y < y0 + h - 4; y += 6) {
            pm.fillRectangle(x0 + 6, y, 1, 3);
            pm.fillRectangle(x0 + w - 7, y, 1, 3);
        }
        // Oversized diamonds read as textile ornament, with no bright round marks
        // that could be mistaken for projectiles. Clip motifs inside the border.
        for (int y = y0 + 14; y < y0 + h - 13; y++) {
            for (int x = x0 + 14; x < x0 + w - 13; x++) {
                int u = Math.floorMod(x - (x0 + w / 2) + 24, 48) - 24;
                int v = Math.floorMod(y - (y0 + h / 2) + 24, 48) - 24;
                int diamond = Math.abs(u) + Math.abs(v);
                if (diamond == 20 || diamond == 15) {
                    pm.setColor(diamond == 20 ? edge : thread);
                    pm.drawPixel(x, y);
                } else if ((x + y) % 6 == 0 && (x - y) % 4 == 0) {
                    pm.setColor(weave);
                    pm.drawPixel(x, y);
                }
            }
        }
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

    /** A round mosaic of lighter paving in the middle of the plaza, where the Pigeon King's statue stands. */
    private static void paintPlaza(Pixmap pm, int cx, int cy) {
        Color stone = Color.valueOf("7e828c");
        Color edge = Color.valueOf("3c4048");
        for (int r = 56; r >= 20; r -= 12) {
            pm.setColor(edge);
            pm.fillCircle(cx, cy, r);
            pm.setColor(stone);
            pm.fillCircle(cx, cy, r - 2);
            stone = stone.cpy().mul(0.9f, 0.9f, 0.9f, 1f);
        }
        pm.setColor(edge);
        for (int a = 0; a < 360; a += 30) { // spokes
            double rad = Math.toRadians(a);
            for (int d = 20; d < 56; d++) {
                pm.drawPixel(cx + (int) Math.round(Math.cos(rad) * d), cy + (int) Math.round(Math.sin(rad) * d));
            }
        }
    }

    /**
     * The clouds: the floor doesn't stop in a straight line at the sides and bottom, it puffs out into
     * the open sky (the caps there are see-through) - round puffs of the floor's own colour, shaded below.
     */
    private static void paintCloudRim(Pixmap pm, Pixmap ts, int x0, int y0, int w, int h) {
        Color base = new Color();
        Color.rgba8888ToColor(base, commonColor(ts, FLOOR_A * T));
        Color shade = base.cpy().mul(0.78f, 0.8f, 0.9f, 1f);
        for (int pass = 0; pass < 2; pass++) { // all the shadows first, then the puffs over them
            pm.setColor(pass == 0 ? shade : base);
            int drop = pass == 0 ? 3 : 0;
            for (int y = y0 + 4; y < y0 + h; y += 9) {
                int r = 5 + Math.floorMod(y * 7, 3);
                pm.fillCircle(x0 - 1, y + drop, r);
                pm.fillCircle(x0 + w, y + drop, r);
            }
            for (int x = x0 + 2; x <= x0 + w; x += 9) {
                pm.fillCircle(x, y0 + h + drop, 5 + Math.floorMod(x * 5, 3));
            }
        }
    }

    /** The most common colour of a tile in the tileset (a floor tile's base colour). */
    private static int commonColor(Pixmap ts, int tileX) {
        ObjectMap<Integer, Integer> counts = new ObjectMap<>();
        int best = 0;
        int bestCount = 0;
        for (int y = 0; y < T; y++) {
            for (int x = 0; x < T; x++) {
                int c = ts.getPixel(tileX + x, y);
                int n = counts.get(c, 0) + 1;
                counts.put(c, n);
                if (n > bestCount) {
                    bestCount = n;
                    best = c;
                }
            }
        }
        return best;
    }

    /** The Fluffy King's throne room: a pink carpet with gold edges up the middle to his throne. */
    private static void paintCarpet(Pixmap pm, int cx, int top, int height) {
        pm.setColor(Color.valueOf("e8c040"));
        pm.fillRectangle(cx - 22, top, 44, height - top);
        pm.setColor(Color.valueOf("e8559a"));
        pm.fillRectangle(cx - 19, top, 38, height - top);
        pm.setColor(Color.valueOf("ff8ac8"));
        for (int y = top + 6; y < height; y += 16) { // a row of little hearts down the middle
            pm.fillRectangle(cx - 3, y, 2, 2);
            pm.fillRectangle(cx + 1, y, 2, 2);
            pm.fillRectangle(cx - 3, y + 2, 6, 2);
            pm.fillRectangle(cx - 1, y + 4, 2, 1);
        }
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

    /** The room's look changed (endless mode's arena going under the sea and back): bake it again. */
    public void forget() {
        currentRoom = null;
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
        if (room.stage.barricade == Stage.Barricade.SHUTTER) {
            drawShutters(b, room, d);
            return;
        }
        if (room.stage.barricade == Stage.Barricade.CLOUD) {
            drawCloudPlugs(b, room, d);
            return;
        }
        if (room.stage.barricade == Stage.Barricade.KELP) {
            drawKelp(b, room, d);
            return;
        }
        if (room.stage.barricade == Stage.Barricade.FORCEFIELD) {
            drawForceFields(b, room, d);
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

    /** The skyscraper: steel elevator doors slid shut across each door gap, with a red light. */
    private static void drawShutters(SpriteBatch b, Room room, Draw d) {
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
            d.rect(b, x0, y0, w, h, BARRIER_DARK);
            d.rect(b, x0 + 0.03f, y0 + 0.03f, w - 0.06f, h - 0.06f, SHUTTER_STEEL);
            if (w > h) {
                d.rect(b, x0 + w / 2f - 0.015f, y0, 0.03f, h, BARRIER_DARK); // where the two doors meet
            } else {
                d.rect(b, x0, y0 + h / 2f - 0.015f, w, 0.03f, BARRIER_DARK);
            }
            d.circle(b, x0 + w / 2f, y0 + h - 0.1f, 0.045f, Color.RED);
        }
    }

    /** The clouds: a thick puff of cloud rolls into each door gap and stays there until the fight is over. */
    private static void drawCloudPlugs(SpriteBatch b, Room room, Draw d) {
        Arena a = room.arena;
        float gap = a.doorX1 - a.doorX0;
        for (int dir = 0; dir < 4; dir++) {
            if (!room.doors[dir]) continue;
            float cx;
            float cy;
            boolean across = dir == Room.N || dir == Room.S; // puffs lined up left to right
            switch (dir) {
                case Room.N:
                    cx = (a.doorX0 + a.doorX1) / 2f; cy = a.top + 0.25f;
                    break;
                case Room.S:
                    cx = (a.doorX0 + a.doorX1) / 2f; cy = a.bottom - 0.05f;
                    break;
                case Room.W:
                    cx = a.left - 0.05f; cy = (a.doorY0 + a.doorY1) / 2f;
                    break;
                default:
                    cx = a.right + 0.05f; cy = (a.doorY0 + a.doorY1) / 2f;
                    break;
            }
            for (int pass = 0; pass < 2; pass++) {
                for (int i = -1; i <= 1; i++) {
                    float r = i == 0 ? 0.36f : 0.28f;
                    float x = cx + (across ? i * gap * 0.33f : 0f);
                    float y = cy + (across ? 0f : i * gap * 0.33f) + (i == 0 ? 0.06f : 0f);
                    if (pass == 0) {
                        d.circle(b, x + 0.02f, y - 0.06f, r, CLOUD_SHADE);
                    } else {
                        d.circle(b, x - 0.02f, y + 0.01f, r - 0.04f, CLOUD_WHITE);
                    }
                }
            }
        }
    }

    /** The sea: a thick curtain of kelp grows across each door gap until the fight is over. */
    private static void drawKelp(SpriteBatch b, Room room, Draw d) {
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
            d.rect(b, x0, y0, w, h, KELP_DARK);
            if (w > h) { // strands standing up across the gap
                for (float x = x0 + 0.04f; x < x0 + w - 0.03f; x += 0.12f) {
                    d.rect(b, x, y0, 0.06f, h, KELP);
                }
            } else { // strands lying across a side door
                for (float y = y0 + 0.04f; y < y0 + h - 0.03f; y += 0.12f) {
                    d.rect(b, x0, y, w, 0.06f, KELP);
                }
            }
        }
    }

    /** The Moon: a shimmering blue force field across each door gap until the fight is over. */
    private static void drawForceFields(SpriteBatch b, Room room, Draw d) {
        Arena a = room.arena;
        float t = (TimeUtils.millis() % 100000L) / 1000f;
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
            d.rect(b, x0, y0, w, h, FIELD, 0.3f + 0.08f * MathUtils.sin(t * 5f));
            for (int i = 0; i < 3; i++) { // bright lines rippling across it
                float k = ((t * 0.8f + i / 3f) % 1f);
                if (w > h) {
                    d.rect(b, x0, y0 + k * h, w, 0.03f, Color.WHITE, 0.5f);
                } else {
                    d.rect(b, x0 + k * w, y0, 0.03f, h, Color.WHITE, 0.5f);
                }
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
