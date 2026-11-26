package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

/**
 * A room's geometry, in world units (32 px per unit): a floor of {@code cols x rows} layout cells of
 * 0.5, walls 0.25 thick on the left/right/bottom, and a 0.5 tall wall face + 0.25 cap at the top.
 * Shaped combat rooms cut cells out of that floor. Doors sit in the middle of each wall and are 1 unit wide. Rooms can have different sizes; the camera
 * follows the player when a room is bigger than the screen ({@link #VIEW_W} x {@link #VIEW_H}).
 * Gameplay code works in the room the player is in: {@link #now}. Code that builds or draws a specific
 * room uses that room's own {@code Room.arena}.
 */
public final class Arena {

    /** What the camera shows (the FitViewport size). */
    public static final float VIEW_W = 7f;
    public static final float VIEW_H = 5f;

    /** Layout grid cell size. */
    public static final float CELL = 0.5f;
    private static final float SIDE_WALL = 0.25f;
    private static final float TOP_WALL = 0.75f;

    /** A one-screen room (13 x 8 cells), the original size: start, treasure and small "trap" rooms. */
    public static final int SMALL_COLS = 13;
    public static final int SMALL_ROWS = 8;
    /** A normal combat room, about 1.5x a screen each way (the camera scrolls). */
    public static final int MEDIUM_COLS = 19;
    public static final int MEDIUM_ROWS = 12;
    /** A boss arena: lots of room to manoeuvre. */
    public static final int BOSS_COLS = 25;
    public static final int BOSS_ROWS = 15;

    /** The arena of the room the player is in. */
    public static Arena now = new Arena(SMALL_COLS, SMALL_ROWS);

    enum Shape {
        OCTAGON(21, 12, new int[]{3, 2, 1, 0, 0, 0}),
        DIAMOND(23, 16, new int[]{9, 8, 7, 5, 4, 2, 0, 0}),
        L_SHAPE(21, 13, new int[]{});

        private final int cols;
        private final int rows;
        private final int[] cornerTrim;

        Shape(int cols, int rows, int[] cornerTrim) {
            this.cols = cols;
            this.rows = rows;
            this.cornerTrim = cornerTrim;
        }
    }

    private final Shape shape;
    private final Array<Rectangle> cutouts = new Array<>();

    public final int cols;
    public final int rows;
    /** Whole room including the walls. */
    public final float worldW;
    public final float worldH;
    /** Floor edges (inside the walls). */
    public final float left;
    public final float right;
    public final float bottom;
    public final float top;
    public final float centerX;
    public final float centerY;
    /** Horizontal door gap (top and bottom doors). */
    public final float doorX0;
    public final float doorX1;
    /** Vertical door gap (left and right doors). */
    public final float doorY0;
    public final float doorY1;

    /** @param cols floor width in cells (keep it odd, so the doors line up with the 16 px tiles) */
    public Arena(int cols, int rows) {
        this(cols, rows, -1f);
    }

    /**
     * @param sideDoorFromTop where the left/right doors are, measured down from the top of the floor (their
     *                        middle); negative = half way up, as usual (the Walking Building's street has
     *                        them near the top, where it starts)
     */
    public Arena(int cols, int rows, float sideDoorFromTop) {
        this(cols, rows, sideDoorFromTop, null);
    }

    Arena(Shape shape) {
        this(shape.cols, shape.rows, -1f, shape);
    }

    private Arena(int cols, int rows, float sideDoorFromTop, Shape shape) {
        this.shape = shape;
        this.cols = cols;
        this.rows = rows;
        left = SIDE_WALL;
        right = left + cols * CELL;
        bottom = SIDE_WALL;
        top = bottom + rows * CELL;
        worldW = right + SIDE_WALL;
        worldH = top + TOP_WALL;
        centerX = (left + right) / 2f;
        centerY = (bottom + top) / 2f;
        doorX0 = centerX - 0.5f;
        doorX1 = centerX + 0.5f;
        float sideDoor = sideDoorFromTop < 0 ? centerY : top - sideDoorFromTop;
        doorY0 = sideDoor - 0.5f;
        doorY1 = sideDoor + 0.5f;
        if (isShaped()) {
            addCutouts(true);
            addCutouts(false);
        }
    }

    public boolean isShaped() {
        return shape != null;
    }

    /** Inclusive start and exclusive end of the floor on this row, counted from the south wall. */
    public int floorStart(int row) {
        return shape == null || shape == Shape.L_SHAPE ? 0
                : shape.cornerTrim[Math.min(row, rows - 1 - row)];
    }

    public int floorEnd(int row) {
        return shape == Shape.L_SHAPE && row >= 8 ? 12 : cols - floorStart(row);
    }

    public boolean hasFloor(int col, int row) {
        return row >= 0 && row < rows && col >= floorStart(row) && col < floorEnd(row);
    }

    // Merge consecutive rows with the same cut so collision, sight and pathfinding can reuse wall rectangles.
    private void addCutouts(boolean west) {
        for (int row = 0; row < rows; ) {
            int edge = west ? floorStart(row) : floorEnd(row);
            int end = row + 1;
            while (end < rows && (west ? floorStart(end) : floorEnd(end)) == edge) end++;
            int width = west ? edge : cols - edge;
            if (width > 0) {
                cutouts.add(new Rectangle(left + (west ? 0 : edge) * CELL, bottom + row * CELL,
                        width * CELL, (end - row) * CELL));
            }
            row = end;
        }
    }

    void addCutoutWalls(Array<Rectangle> walls) {
        walls.addAll(cutouts);
    }

    /** Fits a furniture or hazard footprint on the playable floor, including flush against a wall. */
    boolean fits(Rectangle r) {
        return r.x >= left && r.y >= bottom && r.x + r.width <= right && r.y + r.height <= top
                && avoidsCutouts(r.x, r.y, r.x + r.width, r.y + r.height);
    }

    private boolean avoidsCutouts(float x0, float y0, float x1, float y1) {
        for (Rectangle cutout : cutouts) {
            if (x0 < cutout.x + cutout.width && x1 > cutout.x
                    && y0 < cutout.y + cutout.height && y1 > cutout.y) return false;
        }
        return true;
    }

    /** A one-screen room (a small "trap" room), not just a narrow one like the Slums' alleys. */
    public boolean isSmall() {
        return cols <= SMALL_COLS && rows <= SMALL_ROWS;
    }

    public boolean contains(float x, float y, float margin) {
        float clearance = Math.max(0.00001f, margin);
        return x > left + margin && x < right - margin && y > bottom + margin && y < top - margin
                && (!isShaped() || avoidsCutouts(x - clearance, y - clearance, x + clearance, y + clearance));
    }

    /** Clamps a circle-shaped entity at {@code p} with radius {@code r} inside the room. */
    public void clamp(Vector2 p, float r) {
        float originalX = p.x;
        float originalY = p.y;
        p.x = MathUtils.clamp(originalX, left + r, right - r);
        p.y = MathUtils.clamp(originalY, bottom + r, top - r);
        if (!isShaped() || avoidsCutouts(p.x - r, p.y - r, p.x + r, p.y + r)) return;

        // An entity can straddle several tile rows. Find the nearest position inside a rectangle
        // of floor shared by those rows, leaving room for its full diameter.
        float bestX = centerX;
        float bestY = centerY;
        float bestDistance = Float.POSITIVE_INFINITY;
        for (int first = 0; first < rows; first++) {
            int startCol = 0;
            int endCol = cols;
            float minY = bottom + first * CELL + r;
            for (int last = first; last < rows; last++) {
                startCol = Math.max(startCol, floorStart(last));
                endCol = Math.min(endCol, floorEnd(last));
                float minX = left + startCol * CELL + r;
                float maxX = left + endCol * CELL - r;
                if (minX > maxX) break;
                float maxY = bottom + (last + 1) * CELL - r;
                if (minY > maxY) continue;
                float x = MathUtils.clamp(originalX, minX, maxX);
                float y = MathUtils.clamp(originalY, minY, maxY);
                float distance = Vector2.dst2(originalX, originalY, x, y);
                if (distance < bestDistance) {
                    bestX = x;
                    bestY = y;
                    bestDistance = distance;
                }
            }
        }
        p.set(bestX, bestY);
    }
}
