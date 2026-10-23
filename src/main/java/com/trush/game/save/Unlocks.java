package com.trush.game.save;

import com.badlogic.gdx.Preferences;

/**
 * Things unlocked for good (across runs), kept in the game's preferences. Read once at startup
 * ({@link #load}); the gun pools check the flags directly.
 */
public final class Unlocks {

    private static final String FLUFFUL = "unlockedFlufful";

    /** The Flufful talking gun can come out of chests (unlocked by beating the Fluffy King without a scratch). */
    public static boolean flufful;

    private Unlocks() {
    }

    public static void load(Preferences prefs) {
        flufful = prefs.getBoolean(FLUFFUL, false);
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
