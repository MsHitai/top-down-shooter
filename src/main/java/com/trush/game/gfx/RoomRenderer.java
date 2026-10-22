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
 * (assets/tiles/&lt;theme&gt;.png, 16 px tiles: floor a/b/c/d, wall face, wall top, door frame, bars)
 * into one 224x160 texture, and draws the iron bars over locked doors.
 */
public class RoomRenderer implements Disposable {

    private static final int T = 16;
    private static final int W = 224;
    private static final int H = 160;
    private static final int PPU = 32;

    private static final int FLOOR_A = 0;
    private static final int WALL_FACE = 4;
    private static final int WALL_TOP = 5;

    private static final Color IRON = Color.valueOf("5a5a66");
    private static final Color IRON_HI = Color.valueOf("8a8a96");

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
        // top wall face
        for (int i = 0; i < 13; i++) {
            pm.drawPixmap(ts, WALL_FACE * T, 0, T, T, 8 + i * T, 8, T, T);
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

    /** Iron bars across every door of a locked room. */
    public void drawBars(SpriteBatch b, Room room, Draw d) {
        if (!room.isLocked()) {
            return;
        }
        for (int dir = 0; dir < 4; dir++) {
            if (!room.doors[dir]) continue;
            for (int i = 0; i < 5; i++) {
                float k = i * 0.2f + 0.07f;
                switch (dir) {
                    case Room.N:
                        d.rect(b, Arena.DOOR_X0 + k, Arena.TOP, 0.07f, 0.55f, IRON);
                        d.rect(b, Arena.DOOR_X0 + k, Arena.TOP, 0.025f, 0.55f, IRON_HI);
                        break;
                    case Room.S:
                        d.rect(b, Arena.DOOR_X0 + k, 0, 0.07f, Arena.BOTTOM + 0.05f, IRON);
                        break;
                    case Room.W:
                        d.rect(b, 0, Arena.DOOR_Y0 + k, Arena.LEFT + 0.05f, 0.07f, IRON);
                        break;
                    default:
                        d.rect(b, Arena.RIGHT - 0.05f, Arena.DOOR_Y0 + k, Arena.LEFT + 0.05f, 0.07f, IRON);
                        break;
                }
            }
        }
        if (room.doors[Room.N]) {
            d.rect(b, Arena.DOOR_X0, Arena.TOP + 0.15f, Arena.DOOR_X1 - Arena.DOOR_X0, 0.06f, IRON);
            d.rect(b, Arena.DOOR_X0, Arena.TOP + 0.4f, Arena.DOOR_X1 - Arena.DOOR_X0, 0.06f, IRON);
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
