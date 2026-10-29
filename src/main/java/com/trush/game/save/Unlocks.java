package com.trush.game.save;

import com.badlogic.gdx.Preferences;
import com.trush.game.launcher.Dev;
import com.trush.game.player.Hero;

import java.util.EnumSet;

/**
 * Things unlocked for good (across runs), kept in the game's preferences. Read once at startup
 * ({@link #load}); the gun pools check the flags directly.
 */
public final class Unlocks {

    private static final String FLUFFUL = "unlockedFlufful";
    private static final String FOUND_SEA = "foundSea";
    private static final String KING_BEATEN_BY = "kingBeatenBy";
    private static final String BOARDED_ROCKET = "boardedRocket";
    private static final String HEIRLOOM_KILLS = "heirloomKills";

    /** The Flufful talking gun can come out of chests and shops (unlocked by beating the Fluffy Shark). */
    public static boolean flufful;
    /**
     * The player has dived into the secret sea once in a story run: from then on endless floods at waves
     * 26-30 and the Fluffy Shark can turn up there - before that the secret stays a secret.
     */
    public static boolean foundSea;
    /** Grandad can be played (player/Hero): the Fluffy King has been beaten at least once. */
    public static boolean grandad;
    /**
     * Who of the family has beaten the Fluffy King in a story run (the Undercity's normal ending doesn't
     * count): each adds their piece to the cardboard rocket in the living room (screen/HubScreen).
     */
    public static final EnumSet<Hero> kingBeatenBy = EnumSet.noneOf(Hero.class);
    /** The family's rocket has taken someone up to the Moon: the rocket game console turns up at home. */
    public static boolean boardedRocket;
    /**
     * Every kill the Heirloom has ever been carried for: it's passed down, so it remembers - each time it's
     * found again it comes at the level these bought (objects/Weapon#heirloom).
     */
    public static int heirloomKills;

    private Unlocks() {
    }

    public static void load(Preferences prefs) {
        flufful = prefs.getBoolean(FLUFFUL, false);
        // (a Flufful unlocked before this was remembered means the shark was beaten: the sea was found)
        foundSea = prefs.getBoolean(FOUND_SEA, false) || flufful;
        grandad = prefs.getInteger("wins", 0) > 0; // (ETGFanfic.getWins: every past win counts)
        boardedRocket = prefs.getBoolean(BOARDED_ROCKET, false);
        heirloomKills = prefs.getInteger(HEIRLOOM_KILLS, 0);
        kingBeatenBy.clear();
        for (String name : prefs.getString(KING_BEATEN_BY, "").split(",")) {
            for (Hero h : Hero.values()) {
                if (h.name().equals(name)) kingBeatenBy.add(h);
            }
        }
    }

    /** The first trip up to the Moon; returns true only the first time. */
    public static boolean boardRocket(Preferences prefs) {
        if (boardedRocket) {
            return false;
        }
        boardedRocket = true;
        prefs.putBoolean(BOARDED_ROCKET, true);
        prefs.flush();
        return true;
    }

    /** {@code hero} has beaten the Fluffy King; returns true only the first time for them. */
    public static boolean beatKing(Hero hero, Preferences prefs) {
        if (!hero.buildsRocket() || !kingBeatenBy.add(hero)) {
            return false;
        }
        StringBuilder names = new StringBuilder();
        for (Hero h : kingBeatenBy) {
            names.append(names.length() > 0 ? "," : "").append(h.name());
        }
        prefs.putString(KING_BEATEN_BY, names.toString());
        prefs.flush();
        return true;
    }

    /**
     * Who has added their piece to the rocket: {@link #kingBeatenBy} - or, with dev {@code -Dfluffy.rocket=N},
     * the first N of the family (just shown, never saved).
     */
    public static EnumSet<Hero> rocketBuilders() {
        int dev = Dev.number("rocket", -1);
        if (dev < 0) {
            return EnumSet.copyOf(kingBeatenBy);
        }
        EnumSet<Hero> some = EnumSet.noneOf(Hero.class);
        for (Hero h : Hero.values()) {
            if (h.ordinal() < dev && h.buildsRocket()) some.add(h);
        }
        return some;
    }

    /** The whole family has beaten the King: the rocket is finished (the way to space). */
    public static boolean rocketBuilt() {
        return rocketBuilders().size() == Hero.rocketCrew();
    }

    /** The Heirloom has reached {@code kills} (written now, flushed with the rest of the prefs). */
    public static void heirloomKilled(int kills, Preferences prefs) {
        if (kills > heirloomKills) {
            heirloomKills = kills;
            prefs.putInteger(HEIRLOOM_KILLS, kills);
        }
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
