package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Chest;
import com.trush.game.objects.Pickup;
import com.trush.game.objects.Prop;
import com.trush.game.objects.Shop;

/**
 * One screen-sized room: its place on the stage map, doors, layout, props and leftover pickups.
 * Walls are rebuilt as collision rectangles whenever the doors open or close.
 */
public class Room {

    public enum Type { START, COMBAT, TREASURE, BOSS, ARENA, SHOP }

    public static final int N = 0;
    public static final int S = 1;
    public static final int W = 2;
    public static final int E = 3;

    public final int gx;
    public final int gy;
    public Type type;
    public String theme;
    /** This room's size: walls, doors and layout grid. */
    public Arena arena = new Arena(Arena.SMALL_COLS, Arena.SMALL_ROWS);
    /** Which stage's look (furniture, door barricades) this room has. */
    public Stage stage = Stage.VILLAGE;
    public String[] template;
    public int depth;
    /** The skyscraper: which storey the elevator stops at (the roof is the top one). */
    public int floorNumber;

    public final boolean[] doors = new boolean[4];
    public boolean visited;
    public boolean cleared;
    private boolean locked;

    public final Array<Prop> props = new Array<>();
    /** How much furniture the room was built with (endless restocks up to this). */
    public int startingProps;
    public final Array<Pickup> pickups = new Array<>();
    public final Array<Chest> chests = new Array<>();
    /** Baa-llo's shop, once it's set up (the shop room; endless: the stall between waves). */
    public Shop shop;
    /** The boss room's hatch down to the next floor, open once the boss is dead. */
    public boolean exit;
    /** Where the exit hatch is (set when it opens). */
    public float exitX;
    public float exitY;
    public final Array<Rectangle> walls = new Array<>();
    /** Open hatches in the floor (1x1 tiles; the hole is {@link #hole}): fall in and it costs half a heart. */
    public final Array<Rectangle> pits = new Array<>();
    private boolean built;

    /** Open sky beyond its edges (the skyscraper's roof, every room in the clouds) instead of black. */
    public boolean openSky() {
        return "roof".equals(theme) || stage.isSky();
    }

    public Room(int gx, int gy, Type type) {
        this.gx = gx;
        this.gy = gy;
        this.type = type;
    }

    public static int dx(int dir) {
        return dir == E ? 1 : dir == W ? -1 : 0;
    }

    public static int dy(int dir) {
        return dir == N ? 1 : dir == S ? -1 : 0;
    }

    public static int opposite(int dir) {
        return dir == N ? S : dir == S ? N : dir == W ? E : W;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        if (this.locked != locked) {
            this.locked = locked;
            rebuildWalls();
        }
    }

    public boolean doorOpen(int dir) {
        return doors[dir] && !locked;
    }

    /** Instantiates the props from the template the first time the room is entered. */
    public void build() {
        if (built) {
            return;
        }
        built = true;
        // a layout smaller than the room sits in its middle, with open floor around it
        int offCol = Math.max(0, (arena.cols - template[0].length()) / 2);
        int offRow = Math.max(0, (arena.rows - template.length) / 2);
        for (int row = 0; row < template.length && offRow + row < arena.rows; row++) {
            String line = template[row];
            int r = arena.rows - 1 - (offRow + row);
            for (int tc = 0; tc < line.length() && offCol + tc < arena.cols; tc++) {
                int col = offCol + tc;
                Prop.Kind kind = stage.propFor(line.charAt(tc));
                if (kind == null) {
                    continue;
                }
                float cx = arena.left + Arena.CELL * col + (kind.wide() ? Arena.CELL : Arena.CELL / 2f);
                float cy = arena.bottom + Arena.CELL * r + Arena.CELL / 2f;
                Prop p = new Prop(kind, cx, cy);
                if (!blocksADoor(p.solid)) {
                    props.add(p);
                }
            }
        }
        startingProps = props.size;
        // close the gaps between furniture stacked in neighbouring cells
        for (int i = 0; i < props.size; i++) { // (libGDX arrays can't nest for-each loops)
            Prop low = props.get(i);
            for (int j = 0; j < props.size; j++) {
                Prop high = props.get(j);
                if (low.canJoin() && high.canJoin() && Math.abs(high.cx - low.cx) < 0.01f
                        && Math.abs(high.cy - low.cy - Arena.CELL) < 0.01f) {
                    low.joinAbove(high);
                }
            }
        }
        if ((type == Type.COMBAT || type == Type.ARENA) && stage.pitChance > 0) {
            if (type == Type.COMBAT && arena.cols <= Arena.SMALL_COLS) {
                placePits(MathUtils.random(3, 4), true); // a small "trap" room: tight, with lots of pits
            } else if (MathUtils.random() < stage.pitChance) {
                placePits(MathUtils.random(2, 3), false);
            }
        }
        rebuildWalls();
    }

    /** The part of a pit tile you fall into (inside the rim). */
    public static Rectangle hole(Rectangle pit, Rectangle out) {
        return out.set(pit.x + 0.16f, pit.y + 0.16f, pit.width - 0.32f, pit.height - 0.32f);
    }

    /**
     * Puts pits on free 2x2-cell spots: never in front of doors or on furniture. Normally they also keep
     * clear of the middle of the room and leave walking room around them; a {@code tight} trap room lets
     * them sit next to furniture and closer to the middle.
     */
    private void placePits(int count, boolean tight) {
        float middle = tight ? 0.8f : 1.3f;
        float margin = tight ? 0f : 0.1f;
        Rectangle probe = new Rectangle();
        for (int tries = 0; tries < 120 && pits.size < count; tries++) {
            int col = MathUtils.random(0, arena.cols - 2);
            int row = MathUtils.random(0, arena.rows - 2);
            Rectangle pit = new Rectangle(arena.left + col * Arena.CELL, arena.bottom + row * Arena.CELL, 1f, 1f);
            // the middle is where rewards drop and chests appear
            if (Vector2.dst(pit.x + 0.5f, pit.y + 0.5f, arena.centerX, arena.centerY) < middle) continue;
            probe.set(pit.x - margin, pit.y - margin, 1f + 2 * margin, 1f + 2 * margin); // walking room around it
            if (blocksADoor(probe)) continue;
            boolean clear = true;
            for (Prop p : props) {
                clear &= !p.solid.overlaps(probe);
            }
            for (Rectangle other : pits) {
                clear &= !other.overlaps(probe);
            }
            if (clear) {
                pits.add(pit);
            }
        }
    }

    private Rectangle[] doorZones;
    private Arena doorZonesFor;

    /** True if {@code r} is in the walkway in front of any door (furniture there could trap the player). */
    public boolean blocksADoor(Rectangle r) {
        if (doorZonesFor != arena) {
            doorZonesFor = arena;
            doorZones = new Rectangle[]{
                    new Rectangle(arena.doorX0 - 0.4f, arena.top - 0.9f, 1.8f, 0.9f),
                    new Rectangle(arena.doorX0 - 0.4f, arena.bottom, 1.8f, 0.9f),
                    new Rectangle(arena.left, arena.doorY0 - 0.3f, 0.9f, 1.6f),
                    new Rectangle(arena.right - 0.9f, arena.doorY0 - 0.3f, 0.9f, 1.6f),
            };
        }
        for (Rectangle z : doorZones) {
            if (z.overlaps(r)) return true;
        }
        return false;
    }

    public void rebuildWalls() {
        walls.clear();
        float big = 4f;
        // top
        if (doorOpen(N)) {
            walls.add(new Rectangle(-big, arena.top, arena.doorX0 + big, big));
            walls.add(new Rectangle(arena.doorX1, arena.top, arena.worldW - arena.doorX1 + big, big));
        } else {
            walls.add(new Rectangle(-big, arena.top, arena.worldW + 2 * big, big));
        }
        // bottom
        if (doorOpen(S)) {
            walls.add(new Rectangle(-big, arena.bottom - big, arena.doorX0 + big, big));
            walls.add(new Rectangle(arena.doorX1, arena.bottom - big, arena.worldW - arena.doorX1 + big, big));
        } else {
            walls.add(new Rectangle(-big, arena.bottom - big, arena.worldW + 2 * big, big));
        }
        // left
        if (doorOpen(W)) {
            walls.add(new Rectangle(arena.left - big, -big, big, arena.doorY0 + big));
            walls.add(new Rectangle(arena.left - big, arena.doorY1, big, arena.worldH - arena.doorY1 + big));
        } else {
            walls.add(new Rectangle(arena.left - big, -big, big, arena.worldH + 2 * big));
        }
        // right
        if (doorOpen(E)) {
            walls.add(new Rectangle(arena.right, -big, big, arena.doorY0 + big));
            walls.add(new Rectangle(arena.right, arena.doorY1, big, arena.worldH - arena.doorY1 + big));
        } else {
            walls.add(new Rectangle(arena.right, -big, big, arena.worldH + 2 * big));
        }
    }
}
