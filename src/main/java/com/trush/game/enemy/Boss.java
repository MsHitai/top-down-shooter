package com.trush.game.enemy;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.trush.game.launcher.Dev;
import com.trush.game.world.Arena;
import com.trush.game.world.RoomTemplates;
import com.trush.game.save.Unlocks;
import com.trush.game.world.Stage;

/**
 * The bosses. Like ETG, each stage rolls one of its own; the Fluffy King is the final boss
 * ({@code stage == null}): he waits on his throne in the clouds, the last floor (and in endless mode).
 */
public enum Boss {
    FLUFFY_KING("THE FLUFFY KING", null, "THE FLUFFY KING IS DETHRONED!", false),
    HAYMAKER("THE HAYMAKER", Stage.VILLAGE, "THE HAYMAKER HITS THE HAY!", false),
    HARVESTMAN("THE HARVESTMAN", Stage.VILLAGE, "THE HARVESTMAN IS CUT DOWN!", false),
    WITCH("THE HOLLOW WITCH", Stage.VILLAGE, "THE HOLLOW WITCH IS SWEPT AWAY!", true),
    TRASH_CAN("THE TRASH THAT CAN", Stage.TOWN, "THE TRASH THAT COULDN'T!", false),
    ROAD_RAGER("THE ROAD RAGER", Stage.TOWN, "THE ROAD RAGER IS TOTALED!", true),
    TRAFFIC_LIGHT("THE TRAFFIC LIGHT", Stage.TOWN, "THE TRAFFIC LIGHT IS OUT OF ORDER!", false),
    NIGHT_HAWK("THE NIGHT HAWK", Stage.CITY, "THE NIGHT HAWK IS GROUNDED!", false),
    PIGEON_KING("THE PIGEON KING", Stage.CITY, "THE PIGEON KING FLEW THE COOP!", false),
    EVIL_TRAIN("THE EVIL TRAIN", Stage.CITY, "THE EVIL TRAIN IS DERAILED!", false),
    KAIJU("THE KAIJU", Stage.SKYSCRAPER, "THE KAIJU TOPPLES!", false),
    MECH("THE MEGA MECH", Stage.SKYSCRAPER, "THE MEGA MECH IS SCRAP METAL!", false),
    MESSENGER("THE MESSENGER", Stage.SKYSCRAPER, "THE MESSENGER'S NEWS IS BAD NEWS!", false),
    // the secret sea's (the owner's son's design); beating it tames it - and unlocks the Flufful
    SHARK("THE FLUFFY SHARK", Stage.SEA, "THE FLUFFY SHARK IS TAMED!", false),
    // the secret Slums' (the owner's son's designs)
    BLUE_BLOB("THE BLUE BLOB", Stage.SLUMS, "THE BLUE BLOB GOES SPLAT!", false),
    ALLEY_CAT("THE ALLEY CAT", Stage.SLUMS, "THE ALLEY CAT USED UP ALL NINE LIVES!", false),
    WALKING_BUILDING("THE WALKING BUILDING", Stage.SLUMS, "THE WALKING BUILDING IS CONDEMNED!", false),
    // the Undercity's: the False King, the end of the secret detour
    SKUNK_KING("THE SKUNK KING", Stage.UNDERCITY, "THE FALSE KING FALLS!", false);

    public final String title;
    /** The stage whose floors can roll this boss; null for the final boss. */
    public final Stage stage;
    /** Banner when it's beaten. */
    public final String defeated;
    /** Its arena has some cover (pillars / lamps); the others fight in a wide open arena, like most of ETG. */
    public final boolean cover;

    Boss(String title, Stage stage, String defeated, boolean cover) {
        this.title = title;
        this.stage = stage;
        this.defeated = defeated;
        this.cover = cover;
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
            case TRAFFIC_LIGHT:
                return new TrafficLight(x, y, tier);
            case NIGHT_HAWK:
                return new NightHawk(x, y, tier);
            case PIGEON_KING:
                return new PigeonKing(x, y, tier);
            case EVIL_TRAIN:
                return new EvilTrain(x, y, tier);
            case KAIJU:
                return new Kaiju(x, y, tier);
            case MECH:
                return new Mech(x, y, tier);
            case MESSENGER:
                return new Messenger(x, y, tier);
            case SHARK:
                return new FluffyShark(x, y, tier);
            case BLUE_BLOB:
                return new BlueBlob(x, y, tier);
            case ALLEY_CAT:
                return new AlleyCat(x, y, tier);
            case WALKING_BUILDING:
                return new WalkingBuilding(x, y, tier);
            case SKUNK_KING:
                return new SkunkKing(x, y, tier);
            default:
                return new FluffyKing(x, y, tier);
        }
    }

    /** What it says on its introduction card (GameScreen's BossCard), like the quips under ETG's boss names. */
    public String quote() {
        switch (this) {
            case FLUFFY_KING:
                return "WHO WOKE ME UP?!";
            case HAYMAKER:
                return "Make hay while the sun shines!";
            case HARVESTMAN:
                return "Harvest time. And you're the crop.";
            case WITCH:
                return "Double, double, toil and... bullets!";
            case TRASH_CAN:
                return "You're going in the bin!";
            case ROAD_RAGER:
                return "Get off my road!";
            case TRAFFIC_LIGHT:
                return "Red light... green light...";
            case NIGHT_HAWK:
                return "Nowhere to hide from the light.";
            case PIGEON_KING:
                return "Coo. Coo! COOOO!";
            case EVIL_TRAIN:
                return "All aboard the pain train!";
            case KAIJU:
                return "ROOOAAAARRR!";
            case MECH:
                return "TARGET ACQUIRED.";
            case SHARK:
                return "You're swimming in MY sea now!";
            case BLUE_BLOB:
                return "Wanna play? BOING BOING!";
            case ALLEY_CAT:
                return "This is MY alley, kid.";
            case WALKING_BUILDING:
                return "Going down? You are.";
            case SKUNK_KING:
                return "BOW before your KING! ...and hold your nose.";
            default: // the Messenger
                return "The King sends his regards.";
        }
    }

    /** The boss whose title an enemy shows on its health bar (Enemy.bossName), or null. */
    public static Boss forTitle(String title) {
        for (Boss b : values()) {
            if (b.title.equals(title)) return b;
        }
        return null;
    }

    /** Its arena's layout: wide open, pillars / lamps for the bosses with {@code cover}, parked cars for the Traffic Light. */
    public String[] arenaLayout() {
        if (this == TRAFFIC_LIGHT) {
            return RoomTemplates.BOSS_CARS;
        }
        return cover ? RoomTemplates.BOSS_COVER : RoomTemplates.BOSS_OPEN;
    }

    /** Its arena's height in cells: the Kaiju's roof is wide and short (like ETG's Dragun), so it all fits on screen. */
    public int arenaRows() {
        return this == KAIJU ? 10 : this == WALKING_BUILDING ? WalkingBuilding.ARENA_ROWS : Arena.BOSS_ROWS;
    }

    /** Where its arena's side doors are, down from the top (negative: half way, as usual): the Walking Building's street near the top. */
    public float sideDoorFromTop() {
        return this == WALKING_BUILDING ? WalkingBuilding.SIDE_DOOR_FROM_TOP : -1f;
    }

    /** Where it appears: near the top of its arena; the Walking Building right above the player, who came in from the side. */
    public float spawnY(Arena a, float playerFeetY) {
        if (this == WALKING_BUILDING) {
            return WalkingBuilding.frontFor(a, playerFeetY) + WalkingBuilding.FRONT_BELOW;
        }
        return a.top - 1.1f;
    }

    /** Its arena's width in cells: the Walking Building's is a long narrow street it fills from wall to wall. */
    public int arenaCols() {
        return this == WALKING_BUILDING ? WalkingBuilding.ARENA_COLS : Arena.BOSS_COLS;
    }

    /** Its own arena look, if it has one (otherwise the stage's boss room theme). */
    public String arenaTheme() {
        switch (this) {
            case PIGEON_KING:
                return "plaza";
            case EVIL_TRAIN:
                return "railyard";
            default:
                return null;
        }
    }

    /** Fight music: the stage's boss track; the Fluffy King has his own theme, "Tyrant of Fluff". */
    public String music() {
        return stage == null ? "boss" : stage.bossMusic;
    }

    /** A random boss of this stage (the Slums: the city's for now; else the village's if the stage has none yet); the last floor's is the Fluffy King. */
    public static Boss pickFor(Stage stage) {
        Boss forced = forced();
        if (forced != null) {
            return forced;
        }
        if (stage == Stage.forFloor(Stage.lastFloor())) {
            return FLUFFY_KING;
        }
        Array<Boss> pool = new Array<>();
        for (Boss b : values()) {
            if (b.stage == stage) pool.add(b);
        }
        if (pool.isEmpty() && stage == Stage.MOON) { // until Grandfluff is ready, the Fluffy King stands in
            return FLUFFY_KING;
        }
        if (pool.isEmpty()) { // no bosses of its own yet: the Slums borrow the city's, anything else the village's
            return pickFor(stage == Stage.SLUMS ? Stage.CITY : Stage.VILLAGE);
        }
        return pool.random();
    }

    /** The final boss (the Fluffy King), unless dev builds force another. */
    public static Boss finalBoss() {
        Boss forced = forced();
        return forced != null ? forced : FLUFFY_KING;
    }

    /** Any boss, the Fluffy King included (endless mode); the Fluffy Shark only once the sea has been found. */
    public static Boss pickAny() {
        Boss forced = forced();
        if (forced != null) {
            return forced;
        }
        Boss b;
        do { // secret floors' bosses only once their floor has been found (the shark: the sea)
            b = values()[MathUtils.random(values().length - 1)];
        } while (b.stage == Stage.SEA && !Unlocks.foundSea || b.stage != null && b.stage.isUnderground()); // (secret)
        return b;
    }

    /** Dev builds can force a boss with -Dfluffy.boss=haymaker. */
    private static Boss forced() {
        String forced = Dev.text("boss");
        if (forced == null) {
            return null;
        }
        Boss b = byName(forced);
        if (b != null && !normalize(forced).equals(normalize(b.name()))) {
            // a forgiven typo: say which boss it became, so a wrong match never goes unnoticed
            System.err.println("[dev] boss '" + forced + "' -> " + b.name().toLowerCase());
        }
        if (b == null) {
            StringBuilder names = new StringBuilder();
            for (Boss o : values()) names.append(' ').append(o.name().toLowerCase());
            System.err.println("[dev] unknown boss '" + forced + "', picking at random. Bosses:" + names);
        }
        return b;
    }

    /**
     * Finds a boss by enum name or title, forgiving case, spaces/underscores, a leading "the" and
     * small typos ("road_racer" finds the Road Rager). Null if nothing is close.
     */
    public static Boss byName(String text) {
        String wanted = normalize(text);
        Boss best = null;
        int bestDist = 3; // up to 2 typos
        for (Boss b : values()) {
            for (String name : new String[]{b.name(), b.title}) {
                int dist = distance(wanted, normalize(name));
                if (dist < bestDist) {
                    bestDist = dist;
                    best = b;
                }
            }
        }
        return best;
    }

    private static String normalize(String s) {
        String letters = s.toLowerCase().replaceAll("[^a-z]", "");
        return letters.startsWith("the") && letters.length() > 3 ? letters.substring(3) : letters;
    }

    /** Levenshtein edit distance. */
    private static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }
}
