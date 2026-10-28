package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;

import java.util.Arrays;

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

    /** Baa-llo's shop: stock in the bottom corners; the counter and the stands are put in by GameWorld.openShop. */
    public static final String[] SHOP = {
            ".............",
            ".............",
            ".............",
            ".............",
            ".............",
            ".............",
            "B...........C",
            "BB.........CC",
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
     * The Traffic Light's arena: parked cars all over (sturdy cover from his streams and bursts - his
     * green streams can't be rolled through, so there must always be a car near to duck behind), one
     * in the middle under where he starts, the bottom door left clear.
     */
    public static final String[] BOSS_CARS = grid(Arena.BOSS_COLS, Arena.BOSS_ROWS, new int[][]{
            {11, 6}, {5, 4}, {18, 4}, {2, 8}, {22, 8}, {8, 9}, {15, 9}, {5, 12}, {18, 12},
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

    /**
     * {@code layout} in the middle of a {@code cols x rows} grid, with cover in the open floor around it:
     * a desk/bench near each corner, and on wide floors plants half-way along the top and bottom (clear
     * of the doors in the middle of each wall).
     */
    public static String[] widen(String[] layout, int cols, int rows) {
        char[][] g = new char[rows][cols];
        for (char[] line : g) Arrays.fill(line, '.');
        int offCol = Math.max(0, (cols - layout[0].length()) / 2);
        int offRow = Math.max(0, (rows - layout.length) / 2);
        for (int r = 0; r < layout.length && offRow + r < rows; r++) {
            for (int c = 0; c < layout[r].length() && offCol + c < cols; c++) {
                g[offRow + r][offCol + c] = layout[r].charAt(c);
            }
        }
        if (offCol >= 3 && offRow >= 2) {
            for (int[] at : new int[][]{{1, 1}, {cols - 3, 1}, {1, rows - 2}, {cols - 3, rows - 2}}) {
                g[at[1]][at[0]] = 'H';
            }
        }
        if (cols >= 21 && offRow >= 2) {
            for (int c : new int[]{cols / 4, cols - 1 - cols / 4}) {
                g[1][c] = 'P';
                g[rows - 2][c] = 'P';
            }
        }
        String[] out = new String[rows];
        for (int r = 0; r < rows; r++) out[r] = new String(g[r]);
        return out;
    }

    /**
     * A Slums alley, {@code cols x rows} (long and narrow, either way round): junk piled against both walls
     * along its length - dumpsters, burnt-out cars, drums, trash bags, the odd lamp - with a lane left clear
     * down the middle, and now and then a table out in the lane to flip.
     */
    public static String[] alley(int cols, int rows) {
        boolean across = cols > rows; // runs left-right (else up-down)
        int length = across ? cols : rows;
        int width = across ? rows : cols;
        char[][] g = new char[rows][cols];
        for (char[] line : g) Arrays.fill(line, '.');
        boolean near = MathUtils.randomBoolean(); // which wall the next pile goes against
        for (int at = 2 + MathUtils.random(1); at < length - 3; at += MathUtils.random(3, 5)) {
            int side = near ? 1 : width - 2;
            float roll = MathUtils.random();
            if (roll < 0.3f) { // something big: two cells along the wall
                char big = MathUtils.random() < 0.6f ? 'H' : 'K';
                if (across) {
                    put(g, at, side, big);
                } else {
                    put(g, near ? 1 : width - 3, at, big);
                }
            } else if (roll < 0.55f) { // a little heap: two or three drums and bags
                int n = MathUtils.random(2, 3);
                for (int k = 0; k < n; k++) {
                    char junk = MathUtils.randomBoolean() ? 'B' : 'C';
                    int sideways = k == 2 ? (near ? 1 : -1) : 0; // the third sits out from the wall
                    if (across) {
                        put(g, at + Math.min(k, 1), side + sideways, junk);
                    } else {
                        put(g, side + sideways, at + Math.min(k, 1), junk);
                    }
                }
            } else if (roll < 0.7f) {
                if (across) put(g, at, side, 'P');
                else put(g, side, at, 'P');
            } else if (roll < 0.85f) { // a table out in the lane, to flip for cover
                if (across) put(g, at, width / 2 - 1 + MathUtils.random(1), 'T');
                else put(g, width / 2 - 1, at, 'T');
            } else {
                char junk = MathUtils.randomBoolean() ? 'B' : 'C';
                if (across) put(g, at, side, junk);
                else put(g, side, at, junk);
            }
            near = MathUtils.random() < 0.3f ? near : !near;
        }
        String[] out = new String[rows];
        for (int r = 0; r < rows; r++) out[r] = new String(g[r]);
        return out;
    }

    /** Puts a layout letter at (col, row-from-top) if it's on the grid. */
    private static void put(char[][] g, int col, int row, char c) {
        if (row >= 0 && row < g.length && col >= 0 && col < g[row].length) {
            g[row][col] = c;
        }
    }

    public static String[] randomCombat() {
        return COMBAT[MathUtils.random(COMBAT.length - 1)];
    }
}
