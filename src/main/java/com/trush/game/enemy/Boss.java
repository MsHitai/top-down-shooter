package com.trush.game.enemy;

import com.badlogic.gdx.math.MathUtils;
import com.trush.game.launcher.Dev;

/**
 * The bosses. Like ETG, each floor rolls one of its three; the Fluffy King is saved for the final
 * floor (until that floor exists he only shows up in endless mode).
 */
public enum Boss {
    FLUFFY_KING("THE FLUFFY KING", true),
    HAYMAKER("THE HAYMAKER", false),
    HARVESTMAN("THE HARVESTMAN", false),
    WITCH("THE HOLLOW WITCH", false);

    public final String title;
    /** The final boss: never rolled for the village. */
    public final boolean finalBoss;

    Boss(String title, boolean finalBoss) {
        this.title = title;
        this.finalBoss = finalBoss;
    }

    public Enemy create(float x, float y, int tier) {
        switch (this) {
            case HAYMAKER:
                return new Haymaker(x, y, tier);
            case HARVESTMAN:
                return new Harvestman(x, y, tier);
            case WITCH:
                return new HollowWitch(x, y, tier);
            default:
                return new FluffyKing(x, y, tier);
        }
    }

    /**
     * A random boss; {@code includeFinal} lets the Fluffy King in (endless mode).
     * Dev builds can force any boss with -Dfluffy.boss=haymaker.
     */
    public static Boss pick(boolean includeFinal) {
        String forced = Dev.text("boss");
        if (forced != null) {
            for (Boss b : values()) {
                if (b.name().equalsIgnoreCase(forced.replace(' ', '_'))) {
                    return b;
                }
            }
        }
        Boss b;
        do {
            b = values()[MathUtils.random(values().length - 1)];
        } while (b.finalBoss && !includeFinal);
        return b;
    }
}
