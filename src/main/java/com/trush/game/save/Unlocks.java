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
    private static final String HARD_MODE = "hardMode";
    private static final String CROWNED = "crownedBy";
    /** An option, not progress: ETGFanfic#resetProgress keeps it. */
    public static final String WEAR_CROWNS = "wearCrowns";

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
    /**
     * Hard mode is on (Egor's idea: the skull that turns up in the living room once the rocket is built -
     * {@link #skullThere}): story runs started now are hard all the way (world/GameWorld#isHard).
     */
    public static boolean hardMode;
    /**
     * Who of the family has cheered up Grandfluff in hard mode: each earned a golden skull crown, worn in
     * runs and at home (player/Crown) - unless {@link #wearCrowns} is off (Egor wanted them to come off).
     */
    public static final EnumSet<Hero> crowned = EnumSet.noneOf(Hero.class);
    /** The options' "WEAR CROWNS": off hides every crown (they stay earned). */
    public static boolean wearCrowns = true;

    private Unlocks() {
    }

    public static void load(Preferences prefs) {
        flufful = prefs.getBoolean(FLUFFUL, false);
        // (a Flufful unlocked before this was remembered means the shark was beaten: the sea was found)
        foundSea = prefs.getBoolean(FOUND_SEA, false) || flufful;
        grandad = prefs.getInteger("wins", 0) > 0; // (ETGFanfic.getWins: every past win counts)
        boardedRocket = prefs.getBoolean(BOARDED_ROCKET, false);
        heirloomKills = prefs.getInteger(HEIRLOOM_KILLS, 0);
        hardMode = prefs.getBoolean(HARD_MODE, false);
        wearCrowns = prefs.getBoolean(WEAR_CROWNS, true);
        readHeroes(prefs.getString(KING_BEATEN_BY, ""), kingBeatenBy);
        readHeroes(prefs.getString(CROWNED, ""), crowned);
    }

    private static void readHeroes(String names, EnumSet<Hero> into) {
        into.clear();
        for (String name : names.split(",")) {
            for (Hero h : Hero.values()) {
                if (h.name().equals(name)) into.add(h);
            }
        }
    }

    private static String heroNames(EnumSet<Hero> heroes) {
        StringBuilder names = new StringBuilder();
        for (Hero h : heroes) {
            names.append(names.length() > 0 ? "," : "").append(h.name());
        }
        return names.toString();
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
        prefs.putString(KING_BEATEN_BY, heroNames(kingBeatenBy));
        prefs.flush();
        return true;
    }

    /** {@code hero} has cheered up Grandfluff in hard mode: their golden skull crown. True only the first time. */
    public static boolean crown(Hero hero, Preferences prefs) {
        if (!hero.buildsRocket() || !crowned.add(hero)) {
            return false;
        }
        prefs.putString(CROWNED, heroNames(crowned));
        prefs.flush();
        return true;
    }

    /**
     * Who has a crown: {@link #crowned} - or, with dev {@code -Dfluffy.crowns=all} (or {@code boy,granny}),
     * those (just shown, never saved).
     */
    public static EnumSet<Hero> crownOwners() {
        String dev = Dev.text("crowns");
        if (dev == null) {
            return EnumSet.copyOf(crowned);
        }
        EnumSet<Hero> some = EnumSet.noneOf(Hero.class);
        for (Hero h : Hero.values()) {
            if (h.buildsRocket() && (dev.equalsIgnoreCase("all") || dev.toUpperCase().contains(h.name()))) some.add(h);
        }
        return some;
    }

    /** Draw {@code hero} with their crown on: they've earned it and crowns are worn. */
    public static boolean wearsCrown(Hero hero) {
        return wearCrowns && crownOwners().contains(hero);
    }

    /** The whole rocket crew is crowned (the Fluffy King's way into the family, see ROADMAP). */
    public static boolean allCrowned() {
        return crownOwners().size() == Hero.rocketCrew();
    }

    public static void setWearCrowns(boolean on, Preferences prefs) {
        wearCrowns = on;
        prefs.putBoolean(WEAR_CROWNS, on);
        prefs.flush();
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

    /** The skull is in the living room: everything's been beaten and the rocket is built (dev: {@code -Dfluffy.skull=true}). */
    public static boolean skullThere() {
        return rocketBuilt() || Dev.flag("skull");
    }

    public static void setHardMode(boolean on, Preferences prefs) {
        hardMode = on;
        prefs.putBoolean(HARD_MODE, on);
        prefs.flush();
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
