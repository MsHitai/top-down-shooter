package com.trush.game.enemy;

import com.badlogic.gdx.math.MathUtils;
import com.trush.game.launcher.Dev;

/** The bosses a floor (or an endless boss wave) can roll, like ETG's pool of bosses per floor. */
public enum Boss {
    FLUFFY_KING("THE FLUFFY KING"),
    HAYMAKER("THE HAYMAKER");

    public final String title;

    Boss(String title) {
        this.title = title;
    }

    public Enemy create(float x, float y, int tier) {
        switch (this) {
            case HAYMAKER:
                return new Haymaker(x, y, tier);
            default:
                return new FluffyKing(x, y, tier);
        }
    }

    /** A random boss; dev builds can force one with -Dfluffy.boss=haymaker. */
    public static Boss pick() {
        String forced = Dev.text("boss");
        if (forced != null) {
            for (Boss b : values()) {
                if (b.name().equalsIgnoreCase(forced.replace(' ', '_'))) {
                    return b;
                }
            }
        }
        return values()[MathUtils.random(values().length - 1)];
    }
}
