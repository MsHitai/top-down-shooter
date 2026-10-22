package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;

/**
 * Hand-made room layouts. Each is 8 rows x 13 columns of 0.5-unit cells, top row first.
 * <pre>
 *   .  empty        T  table (2 cells wide, flippable)    H  hay bale (2 cells wide)
 *   B  barrel       C  crate                               P  stone pillar
 * </pre>
 * Keep the cells in front of the doors clear (middle 3 columns of the top/bottom 2 rows,
 * middle 2 rows of the outer 2 columns); props there are dropped anyway.
 * Add a new layout by adding a string array to {@link #COMBAT} - it's picked at random.
 */
public final class RoomTemplates {

    public static final String[][] COMBAT = {
            { // four tables
                    ".............",
                    "..T......T...",
                    ".............",
                    ".............",
                    ".............",
                    ".............",
                    "..T......T...",
                    ".............",
            },
            { // pillared hall
                    ".............",
                    "..P.......P..",
                    ".............",
                    "....P...P....",
                    ".............",
                    "..P.......P..",
                    ".............",
                    ".............",
            },
            { // storage
                    ".............",
                    ".BB.......CC.",
                    ".B.........C.",
                    ".....T.......",
                    ".............",
                    ".C.........B.",
                    ".CC.......BB.",
                    ".............",
            },
            { // barricade
                    ".............",
                    ".............",
                    "...H.....H...",
                    ".....T.T.....",
                    ".....T.T.....",
                    "...H.....H...",
                    ".............",
                    ".............",
            },
            { // scattered junk
                    ".............",
                    "...C.........",
                    ".......T.....",
                    ".B...........",
                    ".........B...",
                    "..T..........",
                    ".........P...",
                    ".............",
            },
            { // pews
                    ".............",
                    "..T.T...T.T..",
                    ".............",
                    "..T.T...T.T..",
                    ".............",
                    "..T.T...T.T..",
                    ".............",
                    ".............",
            },
            { // hay yard
                    ".............",
                    "..H.....H....",
                    ".............",
                    ".....T.......",
                    ".............",
                    "..H.....H....",
                    ".............",
                    ".............",
            },
            { // corners
                    ".............",
                    ".P.........P.",
                    ".............",
                    "....B...C....",
                    ".............",
                    ".............",
                    ".P.........P.",
                    ".............",
            },
            { // tavern
                    ".............",
                    ".B.T.....T.B.",
                    ".............",
                    ".............",
                    "......T......",
                    ".............",
                    ".C.T.....T.C.",
                    ".............",
            },
    };

    public static final String[] START = {
            ".............",
            ".BB.......HH.",
            ".B...........",
            ".............",
            ".............",
            ".............",
            "...........C.",
            ".............",
    };

    public static final String[] TREASURE = {
            ".............",
            "..P.......P..",
            ".............",
            ".............",
            ".............",
            ".............",
            "..P.......P..",
            ".............",
    };

    public static final String[] BOSS = {
            ".............",
            ".P.........P.",
            ".............",
            ".............",
            ".............",
            ".............",
            ".P.........P.",
            ".............",
    };

    private RoomTemplates() {
    }

    public static String[] randomCombat() {
        return COMBAT[MathUtils.random(COMBAT.length - 1)];
    }
}
