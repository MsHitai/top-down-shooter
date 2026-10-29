package com.trush.game.save;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.trush.game.enemy.*;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Item;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Hero;

import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * The Flufficon (our Ammonomicon): everything met and found across all runs, kept in the game's preferences -
 * the enemies seen and how many were beaten, the bosses, the guns and items found, and the achievements.
 * GameWorld reports what happens ({@link #saw}, {@link #defeated}, {@link #foundGun}, ...); the trophy shelf at
 * home opens it (screen/FlufficonBook). Counts live in memory and are written by {@link #save} - on anything
 * new, when a screen closes and when the game shuts down - so a kill doesn't write the whole file.
 * Achievements are checked every so often ({@link #update}) and announced with a banner (ETGFanfic draws it).
 */
public final class Flufficon {

    /** A kind of regular enemy: its {@link Enemy#dexId}, the name shown, where it lives, and a body to draw. */
    public static final class Entry {
        public final String id;
        public final String title;
        public final String home;
        private final BiFunction<Float, Float, Enemy> make;

        Entry(String id, String title, String home, BiFunction<Float, Float, Enemy> make) {
            this.id = id;
            this.title = title;
            this.home = home;
            this.make = make;
        }

        /** A fresh one at the origin, for a portrait. */
        public Enemy create() {
            return make.apply(0f, 0f);
        }
    }

    /** Every regular enemy in the book, by stage (secret stages' folk too - they stay "?" until met). */
    public static final Array<Entry> ENEMIES = new Array<>(new Entry[]{
            new Entry("BulletKin", "Bullet Kin", "THE VILLAGE", BulletKin::new),
            new Entry("VeteranKin", "Veteran Kin", "THE CITY", VeteranKin::new),
            new Entry("ShotgunKin", "Red Shotgun Kin", "THE VILLAGE", ShotgunKin::new),
            new Entry("SawnOffKin", "Sawn-off Kin", "THE SKYSCRAPER", SawnOffKin::new),
            new Entry("Bulletmancer", "Bulletmancer", "THE VILLAGE", Bulletmancer::new),
            new Entry("Spiralmancer", "Spiralmancer", "THE TOWN", Spiralmancer::new),
            new Entry("Bloomancer", "Bloomancer", "THE CITY", Bloomancer::new),
            new Entry("BandanaKin", "Bandana Kin", "EVERYWHERE", BandanaKin::new),
            new Entry("PirateKin", "Pirate Kin", "EVERYWHERE", PirateKin::new),
            new Entry("BlueShotgunKin", "Blue Shotgun Kin", "EVERYWHERE", BlueShotgunKin::new),
            new Entry("TankerKin", "Tanker Kin", "EVERYWHERE", TankerKin::new),
            new Entry("GhostKin", "Ghost Kin", "EVERYWHERE", GhostKin::new),
            new Entry("Dynamite", "Dynamite", "EVERYWHERE", Dynamite::new),
            new Entry("Hydrant", "Hydrant", "THE TOWN", Hydrant::new),
            new Entry("Rat", "Rat", "THE TOWN", Rat::new),
            new Entry("ShellCop", "Shell Cop", "THE TOWN", ShellCop::new),
            new Entry("RiotCop", "Riot Cop", "THE SLUMS", RiotCop::new),
            new Entry("Shapeshifter", "Shapeshifter", "THE TOWN", Shapeshifter::new),
            new Entry("LampMimic", "Lamp Mimic", "THE TOWN", LampMimic::new),
            new Entry("Biker", "Biker", "THE CITY", Biker::new),
            new Entry("Sniper", "Sniper", "THE CITY", Sniper::new),
            new Entry("Robber", "Robber", "THE CITY", Robber::new),
            new Entry("Sludger", "Sludger", "THE CITY", Sludger::new),
            new Entry("RoboCart", "Robo Cart", "THE SKYSCRAPER", RoboCart::new),
            new Entry("Android", "Android", "THE SKYSCRAPER", Android::new),
            new Entry("Agent", "Agent", "THE SKYSCRAPER", Agent::new),
            new Entry("MountainCube", "Mountain Cube", "THE SKYSCRAPER", Cube::mountain),
            new Entry("LeadCube", "Lead Cube", "THE SKYSCRAPER", Cube::lead),
            new Entry("SwordAngel", "Sword Angel", "THE CLOUDS", SwordAngel::new),
            new Entry("BowAngel", "Bow Angel", "THE CLOUDS", BowAngel::new),
            new Entry("Thundercloud", "Thundercloud", "THE CLOUDS", Thundercloud::new),
            new Entry("Cherub", "Cherub", "THE CLOUDS", Cherub::new),
            new Entry("Dandelion", "Dandelion Puff", "THE CLOUDS", Dandelion::new),
            new Entry("Flufferfish", "Flufferfish", "THE DEEP SEA", Flufferfish::new),
            new Entry("Starfish", "Starfish", "THE DEEP SEA", Starfish::new),
            new Entry("Diver", "Diver", "THE DEEP SEA", Diver::new),
            new Entry("Turtle", "Turtle", "THE DEEP SEA", Turtle::new),
            new Entry("Swordfish", "Swordfish", "THE DEEP SEA", Swordfish::new),
            new Entry("AcidSprayer", "Acid Sprayer", "THE SLUMS", AcidSprayer::new),
            new Entry("BarrelRoller", "Barrel Roller", "THE SLUMS", BarrelRoller::new),
            new Entry("MarkedRat", "Marked Rat", "THE SLUMS", Rat::marked),
            new Entry("StinkPigeon", "Stink Pigeon", "THE SLUMS", StinkPigeon::new),
            new Entry("SewerGator", "Sewer Gator", "THE UNDERCITY", SewerGator::new),
            new Entry("AlleyDog", "Alley Dog", "THE UNDERCITY", AlleyDog::new),
            new Entry("Raccoon", "Raccoon", "THE UNDERCITY", Raccoon::new),
            new Entry("MoonBunny", "Moon Bunny", "THE MOON", MoonBunny::new),
            new Entry("LivingAsteroid", "Living Asteroid", "THE MOON", LivingAsteroid::new),
            new Entry("Astronaut", "Astronaut", "THE MOON", Astronaut::new),
            new Entry("Alien", "Alien", "THE MOON", Alien::new),
    });

    /** Counters for achievements that aren't about meeting things. */
    public static final String FLAWLESS = "flawless";
    public static final String COINS_SPENT = "coinsSpent";
    public static final String BLACK_CHESTS = "blackChests";
    /** Runs with GameWorld.FLAWLESS_RUN bosses beaten without a hit. */
    public static final String FLAWLESS_RUNS = "flawlessRuns";
    /** Won runs that went through every place, the secret ones too. */
    public static final String GRAND_TOURS = "grandTours";
    /** Grandfluff cheered up in hard mode (the skull at home). */
    public static final String HARD_TRUE_ENDINGS = "hardTrueEndings";

    /** The achievements, in the order the book lists them; {@code secret} ones show as "???" until done. */
    public enum Achievement {
        FIRST_BOSS("BOSS BASHER", "Beat your first boss", false, g -> bossesBeaten() > 0),
        KINGDOM_SAVED("KINGDOM SAVED", "Beat the Fluffy King", false, g -> g.getWins() > 0),
        SAVED_FIVE("FLUFFY FOREVER", "Save the kingdom 5 times", false, g -> g.getWins() >= 5),
        WHOLE_FAMILY("FAMILY BUSINESS", "Beat the King with all the family", false,
                g -> Unlocks.kingBeatenBy.size() == Hero.rocketCrew()),
        NOT_A_SCRATCH("NOT A SCRATCH", "Beat a boss without getting hit", false, g -> stat(FLAWLESS) > 0),
        SPOTLESS("SPOTLESS", "Beat 10 bosses without getting hit", false, g -> stat(FLAWLESS) >= 10),
        UNTOUCHABLE("UNTOUCHABLE", "Beat 5 bosses without a hit in one run", false, g -> stat(FLAWLESS_RUNS) > 0),
        KILLS_100("WARMING UP", "Defeat 100 enemies", false, g -> totalKills() >= 100),
        KILLS_1000("PEST CONTROL", "Defeat 1000 enemies", false, g -> totalKills() >= 1000),
        KILLS_5000("UNSTOPPABLE", "Defeat 5000 enemies", false, g -> totalKills() >= 5000),
        ALL_ENEMIES("WHO'S WHO", "Meet every kind of enemy", false, g -> enemiesSeen() == ENEMIES.size),
        ALL_BOSSES("BOSS OF BOSSES", "Beat every boss", false, g -> bossesBeaten() == Boss.values().length),
        GUNS_20("GUN NUT", "Find 20 guns", false, g -> guns.size >= 20),
        ALL_GUNS("ARSENAL", "Find every gun", false, g -> gunsFound() == allGuns().size),
        ITEMS_20("POCKETS FULL", "Find 20 items", false, g -> items.size >= 20),
        ALL_ITEMS("HOARDER", "Find every item", false, g -> items.size == Item.values().length),
        BLACK_CHEST("BLACK MAGIC", "Open a black chest", false, g -> stat(BLACK_CHESTS) > 0),
        BIG_SPENDER("BAA-LLO'S FRIEND", "Spend 500 coins in the shop", false, g -> stat(COINS_SPENT) >= 500),
        HIGH_SCORE("HIGH SCORER", "Score 25000 in one run", false, g -> g.getHighScore() >= 25000),
        WAVE_10("TV ADDICT", "Reach wave 10 in endless", false, g -> g.getBestWave() >= 10),
        WAVE_30("COUCH POTATO", "Reach wave 30 in endless", false, g -> g.getBestWave() >= 30),
        FOUND_SEA("DEEP DIVER", "Find the secret sea", true, g -> Unlocks.foundSea),
        SHARK("SHARK TAMER", "Beat the Fluffy Shark", true, g -> Unlocks.flufful),
        SLUMS("STREETS ARE QUIET", "Beat the False King", true, g -> g.getFalseKingBeaten() > 0),
        ROCKET("BLAST OFF!", "Fly the family's rocket", true, g -> Unlocks.boardedRocket),
        MOON("OVER THE MOON", "Cheer up Grandfluff", true, g -> g.getTrueEndings() > 0),
        ROCKET_GAME("EGOR'S CHAMPION", "Score 10000 in the rocket game", true, g -> g.getRocketBest() >= 10000),
        GRAND_TOUR("GRAND TOUR", "Win a run that went everywhere", true, g -> stat(GRAND_TOURS) > 0),
        HARD_MOON("NOT SCARED OF SKULLS", "Cheer up Grandfluff in hard mode", true, g -> stat(HARD_TRUE_ENDINGS) > 0);

        public final String title;
        public final String description;
        public final boolean secret;
        private final Predicate<ETGFanfic> done;

        Achievement(String title, String description, boolean secret, Predicate<ETGFanfic> done) {
            this.title = title;
            this.description = description;
            this.secret = secret;
            this.done = done;
        }

        public boolean unlocked() {
            return achieved.contains(name());
        }
    }

    private static final String SEEN = "dexSeen";
    private static final String KILLS = "dexKills";
    private static final String BOSS_SEEN = "dexBossSeen";
    private static final String BOSS_BEATEN = "dexBossBeaten";
    private static final String GUNS = "dexGuns";
    private static final String ITEMS = "dexItems";
    private static final String STATS = "dexStats";
    private static final String ACHIEVED = "dexAchievements";
    /** How often the achievements are looked at, in seconds. */
    private static final float CHECK_EVERY = 0.5f;

    private static Preferences prefs;
    private static final ObjectIntMap<String> seen = new ObjectIntMap<>();
    private static final ObjectIntMap<String> kills = new ObjectIntMap<>();
    private static final ObjectIntMap<String> bossSeen = new ObjectIntMap<>();
    private static final ObjectIntMap<String> bossBeaten = new ObjectIntMap<>();
    private static final ObjectSet<String> guns = new ObjectSet<>();
    private static final ObjectSet<String> items = new ObjectSet<>();
    private static final ObjectIntMap<String> stats = new ObjectIntMap<>();
    private static final ObjectSet<String> achieved = new ObjectSet<>();
    /** Newly done achievements waiting for their banner. */
    private static final Array<Achievement> announce = new Array<>();
    private static int totalKills;
    private static boolean dirty;
    private static float checkTimer;
    private static Array<String> allGuns;

    private Flufficon() {
    }

    public static void load(Preferences p) {
        prefs = p;
        read(p.getString(SEEN, ""), seen);
        read(p.getString(KILLS, ""), kills);
        read(p.getString(BOSS_SEEN, ""), bossSeen);
        read(p.getString(BOSS_BEATEN, ""), bossBeaten);
        read(p.getString(STATS, ""), stats);
        readSet(p.getString(GUNS, ""), guns);
        readSet(p.getString(ITEMS, ""), items);
        readSet(p.getString(ACHIEVED, ""), achieved);
        announce.clear();
        totalKills = 0;
        for (ObjectIntMap.Entry<String> e : kills) {
            totalKills += e.value;
        }
        // bosses beaten before the book existed: what the other records tell us
        boolean sawAny = false;
        sawAny |= remember(Boss.FLUFFY_KING, p.getInteger("wins", 0));
        sawAny |= remember(Boss.SHARK, Unlocks.flufful ? 1 : 0);
        sawAny |= remember(Boss.SKUNK_KING, p.getInteger("falseKingBeaten", 0));
        sawAny |= remember(Boss.GRANDFLUFF, p.getInteger("trueEndings", 0));
        // the guns of whoever has beaten the King were surely in hand
        for (Hero h : Unlocks.kingBeatenBy) {
            sawAny |= guns.add(h.starter().getName());
        }
        if (sawAny) {
            save();
        }
    }

    private static boolean remember(Boss b, int times) {
        if (times <= 0 || bossBeaten.get(b.name(), 0) > 0) return false;
        bossBeaten.put(b.name(), times);
        bossSeen.put(b.name(), 1);
        dirty = true;
        return true;
    }

    // ------------------------------------------------------------ what GameWorld reports

    /** An enemy turned up (GameWorld.spawnEnemy). */
    public static void saw(Enemy e) {
        if (e.boss) {
            Boss b = Boss.forTitle(e.bossName());
            if (b != null && bossSeen.get(b.name(), 0) == 0) {
                bossSeen.put(b.name(), 1);
                save();
            }
            return;
        }
        if (seen.get(e.dexId(), 0) == 0) {
            seen.put(e.dexId(), 1);
            save();
        }
    }

    /** An enemy was beaten; {@code flawless}: a boss, and not a single hit taken in the fight. */
    public static void defeated(Enemy e, boolean flawless) {
        if (e.boss) {
            Boss b = Boss.forTitle(e.bossName());
            if (b != null) {
                bossSeen.put(b.name(), 1);
                bossBeaten.getAndIncrement(b.name(), 0, 1);
            }
            if (flawless) {
                stats.getAndIncrement(FLAWLESS, 0, 1);
            }
            save(); // (a boss is worth writing down at once)
            return;
        }
        seen.put(e.dexId(), 1);
        kills.getAndIncrement(e.dexId(), 0, 1);
        totalKills++;
        dirty = true;
    }

    public static void foundGun(String name) {
        if (guns.add(name)) save();
    }

    public static void foundItem(Item item) {
        if (items.add(item.name())) save();
    }

    public static void addStat(String stat, int amount) {
        stats.getAndIncrement(stat, 0, amount);
        dirty = true;
    }

    // ------------------------------------------------------------ what the book reads

    public static boolean hasSeen(String enemyId) {
        return seen.get(enemyId, 0) > 0;
    }

    public static int killsOf(String enemyId) {
        return kills.get(enemyId, 0);
    }

    public static boolean hasSeen(Boss b) {
        return bossSeen.get(b.name(), 0) > 0;
    }

    public static int timesBeaten(Boss b) {
        return bossBeaten.get(b.name(), 0);
    }

    public static boolean hasFound(Weapon w) {
        return guns.contains(w.getName());
    }

    public static boolean hasFound(Item item) {
        return items.contains(item.name());
    }

    public static int stat(String stat) {
        return stats.get(stat, 0);
    }

    public static int totalKills() {
        return totalKills;
    }

    public static int enemiesSeen() {
        int n = 0;
        for (Entry e : ENEMIES) {
            if (hasSeen(e.id)) n++;
        }
        return n;
    }

    public static int bossesBeaten() {
        int n = 0;
        for (Boss b : Boss.values()) {
            if (timesBeaten(b) > 0) n++;
        }
        return n;
    }

    public static int gunsFound() {
        int n = 0;
        for (String g : allGuns()) {
            if (guns.contains(g)) n++;
        }
        return n;
    }

    public static int itemsFound() {
        return items.size;
    }

    public static int achievementsDone() {
        int n = 0;
        for (Achievement a : Achievement.values()) {
            if (a.unlocked()) n++;
        }
        return n;
    }

    /** Every gun's name in book order: the family's starters, then the chest guns by tier, the Flufful too. */
    public static Array<String> allGuns() {
        if (allGuns == null) {
            Array<Weapon> list = new Array<>();
            list.add(Weapon.rustySidearm());
            for (Hero h : Hero.values()) {
                Weapon w = h.starter();
                boolean known = false;
                for (Weapon o : list) known |= o.getName().equals(w.getName());
                if (!known) list.add(w);
            }
            Array<Weapon> chest = Weapon.chestGuns();
            chest.sort((a, b) -> a.getTier().compareTo(b.getTier()));
            for (Weapon w : chest) {
                if (!w.isFlufful()) list.add(w);
            }
            list.add(Weapon.flufful());
            allGuns = new Array<>();
            for (Weapon w : list) allGuns.add(w.getName());
        }
        return allGuns;
    }

    // ------------------------------------------------------------ achievements

    /** Looks at the achievements now and then (called every frame by ETGFanfic). */
    public static void update(ETGFanfic game, float delta) {
        checkTimer -= delta;
        if (checkTimer > 0 || prefs == null) return;
        checkTimer = CHECK_EVERY;
        boolean any = false;
        for (Achievement a : Achievement.values()) {
            if (!a.unlocked() && a.done.test(game)) {
                achieved.add(a.name());
                announce.add(a);
                any = true;
            }
        }
        if (any) {
            save();
        }
    }

    /** The next achievement to announce, taken off the queue; null when there's none. */
    public static Achievement nextAnnouncement() {
        return announce.size > 0 ? announce.removeIndex(0) : null;
    }

    // ------------------------------------------------------------ saving

    /** Writes everything to the preferences (and the disk) if anything changed. */
    public static void save() {
        dirty = true;
        flush();
    }

    /** Writes the counts if there's anything new (screens closing, the game shutting down). */
    public static void flush() {
        if (!dirty || prefs == null) return;
        dirty = false;
        prefs.putString(SEEN, write(seen));
        prefs.putString(KILLS, write(kills));
        prefs.putString(BOSS_SEEN, write(bossSeen));
        prefs.putString(BOSS_BEATEN, write(bossBeaten));
        prefs.putString(STATS, write(stats));
        prefs.putString(GUNS, write(guns));
        prefs.putString(ITEMS, write(items));
        prefs.putString(ACHIEVED, write(achieved));
        prefs.flush();
    }

    /** "name:count,name:count" */
    static void read(String text, ObjectIntMap<String> into) {
        into.clear();
        for (String part : text.split(",")) {
            int colon = part.lastIndexOf(':');
            if (colon <= 0) continue;
            try {
                into.put(part.substring(0, colon), Integer.parseInt(part.substring(colon + 1)));
            } catch (NumberFormatException ignored) {
                // a damaged entry: skip it
            }
        }
    }

    static void readSet(String text, ObjectSet<String> into) {
        into.clear();
        for (String part : text.split(",")) {
            if (!part.isEmpty()) into.add(part);
        }
    }

    static String write(ObjectIntMap<String> map) {
        StringBuilder s = new StringBuilder();
        for (ObjectIntMap.Entry<String> e : map) {
            s.append(s.length() > 0 ? "," : "").append(e.key).append(':').append(e.value);
        }
        return s.toString();
    }

    static String write(ObjectSet<String> set) {
        StringBuilder s = new StringBuilder();
        for (String v : set) {
            s.append(s.length() > 0 ? "," : "").append(v);
        }
        return s.toString();
    }
}
