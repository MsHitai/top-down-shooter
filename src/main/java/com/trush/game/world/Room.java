package com.trush.game.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
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
    public String[] template;
    public int depth;

    public final boolean[] doors = new boolean[4];
    public boolean visited;
    public boolean cleared;
    private boolean locked;

    public final Array<Prop> props = new Array<>();
    public final Array<Pickup> pickups = new Array<>();
    public final Array<Rectangle> walls = new Array<>();
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
        for (int row = 0; row < Arena.ROWS; row++) {
            String line = template[row];
            int r = Arena.ROWS - 1 - row;
            for (int col = 0; col < Arena.COLS && col < line.length(); col++) {
                Prop.Kind kind;
                switch (line.charAt(col)) {
                    case 'T': kind = Prop.Kind.TABLE; break;
                    case 'H': kind = Prop.Kind.HAY; break;
                    case 'B': kind = Prop.Kind.BARREL; break;
                    case 'C': kind = Prop.Kind.CRATE; break;
                    case 'P': kind = Prop.Kind.PILLAR; break;
                    default: continue;
                }
                boolean wide = kind == Prop.Kind.TABLE || kind == Prop.Kind.HAY;
                float cx = Arena.LEFT + Arena.CELL * col + (wide ? Arena.CELL : Arena.CELL / 2f);
                float cy = Arena.BOTTOM + Arena.CELL * r + Arena.CELL / 2f;
                Prop p = new Prop(kind, cx, cy);
                if (!blocksADoor(p.solid)) {
                    props.add(p);
                }
            }
        }
        rebuildWalls();
    }

    private static final Rectangle[] DOOR_ZONES = {
            new Rectangle(Arena.DOOR_X0 - 0.4f, Arena.TOP - 0.9f, 1.8f, 0.9f),
            new Rectangle(Arena.DOOR_X0 - 0.4f, Arena.BOTTOM, 1.8f, 0.9f),
            new Rectangle(Arena.LEFT, Arena.DOOR_Y0 - 0.3f, 0.9f, 1.6f),
            new Rectangle(Arena.RIGHT - 0.9f, Arena.DOOR_Y0 - 0.3f, 0.9f, 1.6f),
    };

    private static boolean blocksADoor(Rectangle r) {
        for (Rectangle z : DOOR_ZONES) {
            if (z.overlaps(r)) return true;
        }
        return false;
    }

    public void rebuildWalls() {
        walls.clear();
        float big = 4f;
        // top
        if (doorOpen(N)) {
            walls.add(new Rectangle(-big, Arena.TOP, Arena.DOOR_X0 + big, big));
            walls.add(new Rectangle(Arena.DOOR_X1, Arena.TOP, Arena.WORLD_W - Arena.DOOR_X1 + big, big));
        } else {
            walls.add(new Rectangle(-big, Arena.TOP, Arena.WORLD_W + 2 * big, big));
        }
        // bottom
        if (doorOpen(S)) {
            walls.add(new Rectangle(-big, Arena.BOTTOM - big, Arena.DOOR_X0 + big, big));
            walls.add(new Rectangle(Arena.DOOR_X1, Arena.BOTTOM - big, Arena.WORLD_W - Arena.DOOR_X1 + big, big));
        } else {
            walls.add(new Rectangle(-big, Arena.BOTTOM - big, Arena.WORLD_W + 2 * big, big));
        }
        // left
        if (doorOpen(W)) {
            walls.add(new Rectangle(Arena.LEFT - big, -big, big, Arena.DOOR_Y0 + big));
            walls.add(new Rectangle(Arena.LEFT - big, Arena.DOOR_Y1, big, Arena.WORLD_H - Arena.DOOR_Y1 + big));
        } else {
            walls.add(new Rectangle(Arena.LEFT - big, -big, big, Arena.WORLD_H + 2 * big));
        }
        // right
        if (doorOpen(E)) {
            walls.add(new Rectangle(Arena.RIGHT, -big, big, Arena.DOOR_Y0 + big));
            walls.add(new Rectangle(Arena.RIGHT, Arena.DOOR_Y1, big, Arena.WORLD_H - Arena.DOOR_Y1 + big));
        } else {
            walls.add(new Rectangle(Arena.RIGHT, -big, big, Arena.WORLD_H + 2 * big));
        }
    }
}
