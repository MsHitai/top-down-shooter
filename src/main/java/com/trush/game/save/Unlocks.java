package com.trush.game.save;

import com.badlogic.gdx.Preferences;

/**
 * Things unlocked for good (across runs), kept in the game's preferences. Read once at startup
 * ({@link #load}); the gun pools check the flags directly.
 */
public final class Unlocks {

    private static final String FLUFFUL = "unlockedFlufful";
    private static final String FOUND_SEA = "foundSea";

    /** The Flufful talking gun can come out of chests and shops (unlocked by beating the Fluffy Shark). */
    public static boolean flufful;
    /**
     * The player has dived into the secret sea once in a story run: from then on endless floods at waves
     * 26-30 and the Fluffy Shark can turn up there - before that the secret stays a secret.
     */
    public static boolean foundSea;
    /** Grandad can be played (player/Hero): the Fluffy King has been beaten at least once. */
    public static boolean grandad;

    private Unlocks() {
    }

    public static void load(Preferences prefs) {
        flufful = prefs.getBoolean(FLUFFUL, false);
        // (a Flufful unlocked before this was remembered means the shark was beaten: the sea was found)
        foundSea = prefs.getBoolean(FOUND_SEA, false) || flufful;
        grandad = prefs.getInteger("wins", 0) > 0; // (ETGFanfic.getWins: every past win counts)
    }

    /** The first dive into the sea; returns true only the first time. */
    public static boolean findSea(Preferences prefs) {
        if (foundSea) {
            return false;
        }
        foundSea = true;
        prefs.putBoolean(FOUND_SEA, true);
        prefs.flush();
        return true;
    }

    /** Unlocks the Flufful; returns true only the first time. */
    public static boolean unlockFlufful(Preferences prefs) {
        if (flufful) {
            return false;
        }
        flufful = true;
        prefs.putBoolean(FLUFFUL, true);
        prefs.flush();
        return true;
    }
}
