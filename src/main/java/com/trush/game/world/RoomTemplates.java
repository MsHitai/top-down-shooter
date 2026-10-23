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

    /** Boss arena (25 x 15): wide open - room to dodge, nowhere to hide. */
    public static final String[] BOSS_OPEN = grid(Arena.BOSS_COLS, Arena.BOSS_ROWS, new int[][]{});

    /** Boss arena with some cover (pillars, or lamps in the town) for the bosses that suit it. */
    public static final String[] BOSS_COVER = grid(Arena.BOSS_COLS, Arena.BOSS_ROWS, new int[][]{
            {5, 3}, {19, 3}, {5, 11}, {19, 11}, {9, 7}, {15, 7}, {2, 7}, {22, 7},
    });

    /**
     * The Traffic Light's arena: three parked cars (sturdy cover from his streams and bursts) - two
     * out to the sides and one in the middle, under where he starts.
     */
    public static final String[] BOSS_CARS = grid(Arena.BOSS_COLS, Arena.BOSS_ROWS, new int[][]{
            {5, 9}, {18, 9}, {11, 6},
    }, 'K');

    /** An empty layout with pillars at the given {col, row-from-top} cells. */
    private static String[] grid(int cols, int rows, int[][] pillars) {
        return grid(cols, rows, pillars, 'P');
    }

    /** An empty layout with {@code prop} (a layout letter) at the given {col, row-from-top} cells. */
    private static String[] grid(int cols, int rows, int[][] pillars, char prop) {
        String[] out = new String[rows];
        for (int r = 0; r < rows; r++) {
            StringBuilder line = new StringBuilder();
            for (int c = 0; c < cols; c++) {
                boolean pillar = false;
                for (int[] p : pillars) {
                    pillar |= p[0] == c && p[1] == r;
                }
                line.append(pillar ? prop : '.');
            }
            out[r] = line.toString();
        }
        return out;
    }

    private RoomTemplates() {
    }

    public static String[] randomCombat() {
        return COMBAT[MathUtils.random(COMBAT.length - 1)];
    }
}
