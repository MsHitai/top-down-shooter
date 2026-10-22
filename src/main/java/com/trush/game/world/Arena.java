package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * The walkable area of the room in world units (world is 7x5, floor.png is 1536x1024).
 * The WALL_* values are the wall thickness of floor.png; tune them if the room asset changes
 * (decrease to let things get closer to the wall).
 */
public final class Arena {

    public static final float WORLD_W = 7f;
    public static final float WORLD_H = 5f;

    public static final float WALL_LEFT = 0.28f;
    public static final float WALL_RIGHT = 0.25f;
    public static final float WALL_BOTTOM = 0.38f;
    public static final float WALL_TOP = 0.28f;

    public static final float LEFT = WALL_LEFT;
    public static final float RIGHT = WORLD_W - WALL_RIGHT;
    public static final float BOTTOM = WALL_BOTTOM;
    public static final float TOP = WORLD_H - WALL_TOP;

    public static final float CENTER_X = (LEFT + RIGHT) / 2f;
    public static final float CENTER_Y = (BOTTOM + TOP) / 2f;

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
