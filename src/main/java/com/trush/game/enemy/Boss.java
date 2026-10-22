package com.trush.game.enemy;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.trush.game.launcher.Dev;
import com.trush.game.world.Stage;

/**
 * The bosses. Like ETG, each stage rolls one of its own; the Fluffy King is the final boss
 * ({@code stage == null}) - until the final floor exists he only shows up in endless mode.
 */
public enum Boss {
    FLUFFY_KING("THE FLUFFY KING", null),
    HAYMAKER("THE HAYMAKER", Stage.VILLAGE),
    HARVESTMAN("THE HARVESTMAN", Stage.VILLAGE),
    WITCH("THE HOLLOW WITCH", Stage.VILLAGE),
    TRASH_CAN("THE TRASH THAT CAN", Stage.TOWN),
    ROAD_RAGER("THE ROAD RAGER", Stage.TOWN);

    public final String title;
    /** The stage whose floors can roll this boss; null for the final boss. */
    public final Stage stage;

    Boss(String title, Stage stage) {
        this.title = title;
        this.stage = stage;
    }

    public Enemy create(float x, float y, int tier) {
        switch (this) {
            case HAYMAKER:
                return new Haymaker(x, y, tier);
            case HARVESTMAN:
                return new Harvestman(x, y, tier);
            case WITCH:
                return new HollowWitch(x, y, tier);
            case TRASH_CAN:
                return new TrashThatCan(x, y, tier);
            case ROAD_RAGER:
                return new RoadRager(x, y, tier);
            default:
                return new FluffyKing(x, y, tier);
        }
    }

    /** A random boss of this stage (the village's if the stage has none yet). */
    public static Boss pickFor(Stage stage) {
        Boss forced = forced();
        if (forced != null) {
            return forced;
        }
        Array<Boss> pool = new Array<>();
        for (Boss b : values()) {
            if (b.stage == stage) pool.add(b);
        }
        if (pool.isEmpty()) {
            return pickFor(Stage.VILLAGE);
        }
        return pool.random();
    }

    /** Any boss, the Fluffy King included (endless mode). */
    public static Boss pickAny() {
        Boss forced = forced();
        return forced != null ? forced : values()[MathUtils.random(values().length - 1)];
    }

    /** Dev builds can force a boss with -Dfluffy.boss=haymaker. */
    private static Boss forced() {
        String forced = Dev.text("boss");
        if (forced != null) {
            for (Boss b : values()) {
                if (b.name().equalsIgnoreCase(forced.replace(' ', '_'))) {
                    return b;
                }
            }
        }
        return null;
    }
}
