package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Chest;
import com.trush.game.objects.Pickup;
import com.trush.game.objects.Prop;

/**
 * One screen-sized room: its place on the stage map, doors, layout, props and leftover pickups.
 * Walls are rebuilt as collision rectangles whenever the doors open or close.
 */
public class Room {

    public enum Type { START, COMBAT, TREASURE, BOSS, ARENA }

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

    public final boolean[] doors = new boolean[4];
    public boolean visited;
    public boolean cleared;
    private boolean locked;

    public final Array<Prop> props = new Array<>();
    public final Array<Pickup> pickups = new Array<>();
    public final Array<Chest> chests = new Array<>();
    /** The boss room's hatch down to the next floor, open once the boss is dead. */
    public boolean exit;
    public final Array<Rectangle> walls = new Array<>();
    /** Open hatches in the floor (1x1 tiles; the hole is {@link #hole}): fall in and it costs half a heart. */
    public final Array<Rectangle> pits = new Array<>();
    private boolean built;

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
        for (int row = 0; row < arena.rows; row++) {
            String line = template[row];
            int r = arena.rows - 1 - row;
            for (int col = 0; col < arena.cols && col < line.length(); col++) {
                Prop.Kind kind = stage.propFor(line.charAt(col));
                if (kind == null) {
                    continue;
                }
                boolean wide = kind == Prop.Kind.TABLE || kind == Prop.Kind.HAY || kind == Prop.Kind.BENCH;
                float cx = arena.left + Arena.CELL * col + (wide ? Arena.CELL : Arena.CELL / 2f);
                float cy = arena.bottom + Arena.CELL * r + Arena.CELL / 2f;
                Prop p = new Prop(kind, cx, cy);
                if (!blocksADoor(p.solid)) {
                    props.add(p);
                }
            }
        }
        if ((type == Type.COMBAT || type == Type.ARENA) && MathUtils.random() < stage.pitChance) {
            placePits(MathUtils.random(1, 2));
        }
        rebuildWalls();
    }

    /** The part of a pit tile you fall into (inside the rim). */
    public static Rectangle hole(Rectangle pit, Rectangle out) {
        return out.set(pit.x + 0.16f, pit.y + 0.16f, pit.width - 0.32f, pit.height - 0.32f);
    }

    /** Puts pits on free 2x2-cell spots: not in front of doors, on furniture or in the middle of the room. */
    private void placePits(int count) {
        Rectangle probe = new Rectangle();
        for (int tries = 0; tries < 60 && pits.size < count; tries++) {
            int col = MathUtils.random(0, arena.cols - 2);
            int row = MathUtils.random(0, arena.rows - 2);
            Rectangle pit = new Rectangle(arena.left + col * Arena.CELL, arena.bottom + row * Arena.CELL, 1f, 1f);
            // the middle is where rewards drop and chests appear
            if (Vector2.dst(pit.x + 0.5f, pit.y + 0.5f, arena.centerX, arena.centerY) < 1.3f) continue;
            probe.set(pit.x - 0.1f, pit.y - 0.1f, 1.2f, 1.2f); // a little walking room around it
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
