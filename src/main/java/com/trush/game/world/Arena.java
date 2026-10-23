package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * A room's geometry, in world units (32 px per unit): a floor of {@code cols x rows} layout cells of
 * 0.5, walls 0.25 thick on the left/right/bottom, and a 0.5 tall wall face + 0.25 cap at the top.
 * Doors sit in the middle of each wall and are 1 unit wide. Rooms can have different sizes; the camera
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
        doorY0 = centerY - 0.5f;
        doorY1 = centerY + 0.5f;
    }

    public boolean contains(float x, float y, float margin) {
        return x > left + margin && x < right - margin && y > bottom + margin && y < top - margin;
    }

    /** Clamps a circle-shaped entity at {@code p} with radius {@code r} inside the room. */
    public void clamp(Vector2 p, float r) {
        p.x = MathUtils.clamp(p.x, left + r, right - r);
        p.y = MathUtils.clamp(p.y, bottom + r, top - r);
    }
}
