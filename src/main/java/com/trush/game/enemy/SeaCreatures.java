package com.trush.game.enemy;

import com.badlogic.gdx.math.MathUtils;

/**
 * The secret sea's own creatures (the owner's son's line-up): Flufferfish, Starfish, Diver, Turtle and
 * Swordfish - only ever met down there (story) or in endless mode's sea waves. Both directors pick and
 * create them through here.
 */
public final class SeaCreatures {

    public static final int FLUFFERFISH = 0;
    public static final int STARFISH = 1;
    public static final int DIVER = 2;
    public static final int TURTLE = 3;
    public static final int SWORDFISH = 4;

    private SeaCreatures() {
    }

    /** One for a room's budget; divers (long-range harpoons) at most two at a time. Starfish are the cheap filler. */
    public static int pick(int budget, int divers) {
        float r = MathUtils.random();
        if (budget >= 2 && r < 0.22f) return FLUFFERFISH;
        if (budget >= 2 && divers < 2 && r < 0.38f) return DIVER;
        if (budget >= 2 && r < 0.56f) return TURTLE;
        if (budget >= 2 && r < 0.74f) return SWORDFISH;
        return STARFISH;
    }

    public static int cost(int kind) {
        return kind == STARFISH ? 1 : 2;
    }

    public static Enemy create(int kind, float x, float y) {
        switch (kind) {
            case FLUFFERFISH:
                return new Flufferfish(x, y);
            case DIVER:
                return new Diver(x, y);
            case TURTLE:
                return new Turtle(x, y);
            case SWORDFISH:
                return new Swordfish(x, y);
            default:
                return new Starfish(x, y);
        }
    }
}
