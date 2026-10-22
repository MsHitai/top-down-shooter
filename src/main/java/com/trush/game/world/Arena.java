package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Room geometry shared by every room, in world units (world is 7x5; room art is 224x160 px, 32 px/unit).
 * Walls: 0.25 on the left/right/bottom, and a 0.5 tall wall face + 0.25 cap at the top.
 * Doors sit in the middle of each wall and are 1 unit wide.
 */
public final class Arena {

    public static final float WORLD_W = 7f;
    public static final float WORLD_H = 5f;

    public static final float LEFT = 0.25f;
    public static final float RIGHT = WORLD_W - 0.25f;
    public static final float BOTTOM = 0.25f;
    public static final float TOP = 4.25f;

    public static final float CENTER_X = (LEFT + RIGHT) / 2f;
    public static final float CENTER_Y = (BOTTOM + TOP) / 2f;

    /** Horizontal door gap (top and bottom doors). */
    public static final float DOOR_X0 = 3.0f;
    public static final float DOOR_X1 = 4.0f;
    /** Vertical door gap (left and right doors). */
    public static final float DOOR_Y0 = 1.75f;
    public static final float DOOR_Y1 = 2.75f;

    /** Room layout grid: 13 x 8 cells of 0.5 units inside the walls. */
    public static final float CELL = 0.5f;
    public static final int COLS = 13;
    public static final int ROWS = 8;

    private Arena() {
    }

    public static boolean contains(float x, float y, float margin) {
        return x > LEFT + margin && x < RIGHT - margin && y > BOTTOM + margin && y < TOP - margin;
    }

    /** Clamps a circle-shaped entity at {@code p} with radius {@code r} inside the room. */
    public static void clamp(Vector2 p, float r) {
        p.x = MathUtils.clamp(p.x, LEFT + r, RIGHT - r);
        p.y = MathUtils.clamp(p.y, BOTTOM + r, TOP - r);
    }
}
