package com.trush.game.enemy;

import com.badlogic.gdx.math.MathUtils;

/**
 * The secret sea's own creatures (Egor's line-up): Flufferfish, Starfish, Diver, Turtle and
 * Swordfish, and the big ones - the Giant Crab and the Giant Octopus - only ever met down there (story) or in endless mode's sea waves. Both directors pick and
 * create them through here.
 */
public final class SeaCreatures {

    public static final int FLUFFERFISH = 0;
    public static final int STARFISH = 1;
    public static final int DIVER = 2;
    public static final int TURTLE = 3;
    public static final int SWORDFISH = 4;
    /** The big ones: tougher, bigger patterns, one a room at most. */
    public static final int GIANT_CRAB = 5;
    public static final int GIANT_OCTOPUS = 6;

    private SeaCreatures() {
    }

    /**
     * One for a room's budget; divers (long-range harpoons) at most two at a time, one of the big ones
     * ({@link #isBig}) a room. Starfish are the cheap filler (fewer of them since the sea got tougher).
     */
    public static int pick(int budget, int divers, int bigs) {
        float r = MathUtils.random();
        if (budget >= 5 && bigs < 1 && r < 0.3f) return r < 0.15f ? GIANT_CRAB : GIANT_OCTOPUS;
        r = MathUtils.random();
        if (budget >= 2 && r < 0.24f) return FLUFFERFISH;
        if (budget >= 2 && divers < 2 && r < 0.42f) return DIVER;
        if (budget >= 2 && r < 0.62f) return TURTLE;
        if (budget >= 2 && r < 0.84f) return SWORDFISH;
        return STARFISH;
    }

    public static boolean isBig(int kind) {
        return kind == GIANT_CRAB || kind == GIANT_OCTOPUS;
    }

    public static int cost(int kind) {
        return isBig(kind) ? 4 : kind == STARFISH ? 1 : 2;
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
            case GIANT_CRAB:
                return new GiantCrab(x, y);
            case GIANT_OCTOPUS:
                return new GiantOctopus(x, y);
            default:
                return new Starfish(x, y);
        }
    }
}
