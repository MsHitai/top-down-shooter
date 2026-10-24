package com.trush.game.objects;

/**
 * The run's money, like ETG's shell casings: enemies, cleared rooms, chests and bosses drop coins, and
 * Baa-llo's shop takes them. Just the numbers here (GameWorld spills the coins, StoryDirector and
 * WaveDirector hand out the rewards), so they can be tested.
 * <p>
 * A story floor comes to about 80-110 coins: one good buy, or two or three small ones.
 */
public final class Coins {

    /** What a big coin is worth (bosses and chests spill those, so a reward isn't 30 little coins). */
    public static final int BIG = 5;
    /** The purse is full at this (the HUD has room for three digits). */
    public static final int MAX = 999;

    private Coins() {
    }

    /**
     * Coins a regular enemy drops: its score / 20 on average - half a coin for a bullet kin, one or two for a
     * shotgun kin, two for a bulletmancer, a quarter for a rat. {@code roll} (0..1) decides the leftover
     * fraction: an average of 1.25 is one coin, or two a quarter of the time.
     */
    public static int forEnemy(int score, float roll) {
        float average = score / 20f;
        int n = (int) average;
        return roll < average - n ? n + 1 : n;
    }

    /** A cleared combat room: 3-5 coins ({@code extra} is 0-2), more deeper into the floor. */
    public static int forRoom(int depth, int extra) {
        return 3 + extra + depth / 2;
    }

    /** A boss: 25 coins on floor 1, five more each floor (endless: each boss wave). */
    public static int forBoss(int level) {
        return 20 + 5 * level;
    }

    /** An endless wave cleared: a few coins, more as the waves go on. */
    public static int forWave(int wave) {
        return 2 + wave / 3;
    }

    /** A chest without a gun or an item spills some coins too: better chests, more. */
    public static int forChest(Tier tier, int extra) {
        return 3 + tier.ordinal() * 2 + extra;
    }

    /** How many of {@code total} coins come as big ones (the rest, 3 to 7, as single coins). */
    public static int bigCoins(int total) {
        return total >= 8 ? (total - 3) / BIG : 0;
    }
}
