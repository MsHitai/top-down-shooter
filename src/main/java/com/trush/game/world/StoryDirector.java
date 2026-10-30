package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.trush.game.enemy.BulletKin;
import com.trush.game.launcher.Dev;
import com.trush.game.enemy.Biker;
import com.trush.game.enemy.Bulletmancer;
import com.trush.game.enemy.Spiralmancer;
import com.trush.game.enemy.Bloomancer;
import com.trush.game.enemy.Cherub;
import com.trush.game.enemy.Dandelion;
import com.trush.game.enemy.Sniper;
import com.trush.game.enemy.Dynamite;
import com.trush.game.enemy.Robber;
import com.trush.game.enemy.Shapeshifter;
import com.trush.game.enemy.Sludger;
import com.trush.game.enemy.AcidSprayer;
import com.trush.game.enemy.BarrelRoller;
import com.trush.game.enemy.StinkPigeon;
import com.trush.game.enemy.SewerGator;
import com.trush.game.enemy.AlleyDog;
import com.trush.game.enemy.Raccoon;
import com.trush.game.enemy.Agent;
import com.trush.game.enemy.BowAngel;
import com.trush.game.enemy.SwordAngel;
import com.trush.game.enemy.Thundercloud;
import com.trush.game.enemy.Android;
import com.trush.game.enemy.Cube;
import com.trush.game.enemy.RoboCart;
import com.trush.game.enemy.Hydrant;
import com.trush.game.enemy.Rat;
import com.trush.game.enemy.Enemy;
import com.trush.game.enemy.MoonFolk;
import com.trush.game.enemy.SeaCreatures;
import com.trush.game.enemy.ShellCop;
import com.trush.game.enemy.Boss;
import com.trush.game.enemy.ShotgunKin;
import com.trush.game.enemy.VeteranKin;
import com.trush.game.enemy.SawnOffKin;
import com.trush.game.enemy.RiotCop;
import com.trush.game.enemy.BandanaKin;
import com.trush.game.enemy.BlueShotgunKin;
import com.trush.game.enemy.GhostKin;
import com.trush.game.enemy.PirateKin;
import com.trush.game.enemy.TankerKin;
import com.trush.game.objects.Chest;
import com.trush.game.objects.Coins;
import com.trush.game.objects.Item;
import com.trush.game.objects.FluffulVoice;
import com.trush.game.objects.Pickup;
import com.trush.game.objects.Shop;
import com.trush.game.objects.Tier;
import com.trush.game.objects.Weapon;
import com.trush.game.save.Unlocks;
import lombok.Getter;

/**
 * Story mode, stage 1 "The Village": explore a random map of rooms. Walking into an uncleared
 * combat room locks its doors until every enemy is dead. Like ETG, guns only come out of chests -
 * a locked chest in the treasure room (each floor drops exactly one key, more are rare finds),
 * the odd locked chest as a room reward, and a free chest from the boss - or Baa-llo's shop, for coins. Beating the boss opens a
 * hatch down to the next floor.
 */
@Getter
public class StoryDirector implements Director {


    private static final int KIN = 0;
    private static final int SHOTGUN = 1;
    private static final int MANCER = 2;
    private static final int HYDRANT = 3;
    private static final int RATS = 4;
    private static final int COP = 5;
    private static final int BIKER = 6;
    private static final int SNIPER = 7;
    private static final int ROBBER = 8;
    private static final int DYNAMITE = 9;
    private static final int SHAPESHIFTER = 10;
    private static final int ROBOCART = 11;
    private static final int ANDROID = 12;
    private static final int AGENT = 13;
    private static final int MOUNTAIN_CUBE = 14;
    private static final int LEAD_CUBE = 15;
    private static final int SWORD_ANGEL = 16;
    private static final int BOW_ANGEL = 17;
    private static final int THUNDERCLOUD = 18;
    private static final int SLUDGER = 19;
    // the Slums' own: the acid gang, and a few critters the Skunk King's stink has marked
    private static final int SPRAYER = 20;
    private static final int ROLLER = 21;
    private static final int MARKED_RATS = 22;
    private static final int STINK_PIGEON = 23;
    // the Undercity's own: more marked critters, and one of the King's court
    private static final int GATOR = 24;
    private static final int DOG = 25;
    private static final int RACCOON = 26;
    // the rest of the gun kin, met on every floor (a little less where the floor's own folk are at home)
    private static final int PIRATE = 27;
    private static final int BANDANA = 28;
    private static final int TANKER = 29;
    private static final int GHOST = 30;
    private static final int BLUE_SHOTGUN = 31;
    // the Bulletmancer's cousins, and the clouds' own fluffy folk
    private static final int SPIRALMANCER = 32;
    private static final int BLOOMANCER = 33;
    private static final int CHERUB = 34;
    private static final int DANDELION = 35;
    private static final int VETERAN = 36;
    private static final int SAWN_OFF = 37;
    private static final int RIOT_COP = 38;
    /** From this floor on every plain Bullet Kin comes as a Veteran Kin (bursts that lead you), like ETG. */
    static final int VETERAN_FLOOR = 3;
    /** From this floor on the Red Shotgun Kin comes as a Sawn-off Kin and the Shell Cop as a Riot Cop. */
    static final int TOUGH_FLOOR = 4;
    /** Hard mode (GameWorld#isHard): this many times the enemies a room, one more at a time, tougher kinds a floor sooner. */
    static final float HARD_BUDGET = 1.35f;
    private boolean hard;
    /** Ghosts a room may have: two fading in and out at once is plenty. */
    private static final int MAX_GHOSTS = 2;
    /** Enemy type names, by id (dev builds: -Dfluffy.enemy=raccoon fills every fight with that kind). */
    private static final String[] NAMES = {"kin", "shotgun", "mancer", "hydrant", "rats", "cop", "biker", "sniper",
            "robber", "dynamite", "shapeshifter", "robocart", "android", "agent", "mountain_cube", "lead_cube",
            "sword_angel", "bow_angel", "thundercloud", "sludger", "sprayer", "roller", "marked_rats", "stink_pigeon",
            "gator", "dog", "raccoon", "pirate", "bandana", "tanker", "ghost", "blue_shotgun",
            "spiralmancer", "bloomancer", "cherub", "dandelion", "veteran", "sawn_off", "riot_cop"};
    /** The Moon's folk by MoonFolk kind (dev builds: -Dfluffy.enemy=alien). */
    private static final String[] MOON_NAMES = {"bunnies", "asteroid", "astronaut", "alien"};

    /** The enemy type a dev build forces with -Dfluffy.enemy=name, or -1. */
    private static int forcedEnemy() {
        String name = Dev.text("enemy");
        if (name == null) return -1;
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equalsIgnoreCase(name.trim())) return i;
        }
        for (int i = 0; i < MOON_NAMES.length; i++) {
            if (MOON_NAMES[i].equalsIgnoreCase(name.trim())) return MOON + i;
        }
        System.err.println("[dev] unknown enemy '" + name + "'. Enemies: " + String.join(" ", NAMES) + " "
                + String.join(" ", MOON_NAMES));
        return -1;
    }
    /** An acid gunner turns up now and then in the town, the city and the skyscraper (one a room at most). */
    private static final float SLUDGER_CHANCE = 0.05f;
    /** Bikers a city room may have (one in a small room): three circling at once, with cops, was too much. */
    private static final int MAX_BIKERS = 2;
    /** The sea's creatures: SEA + a SeaCreatures kind. */
    private static final int SEA = 100;
    /** How many floors harder the sea is than the floor it's under, and how many more creatures a room has. */
    static final int SEA_LEVELS_UP = 2;
    static final int SEA_EXTRA_BUDGET = 3;
    /** The Moon's folk: MOON + a MoonFolk kind. */
    private static final int MOON = 200;

    private final StageMap map;
    /** The village on floor 1, the town on floor 2... */
    private final Stage stage;
    private Room room;
    private final IntArray queue = new IntArray();
    /** A second group held back until the first is down (like ETG's reinforcement waves); empty for most rooms. */
    private final IntArray reinforcements = new IntArray();
    /**
     * How likely a combat room is to send in a second group once the first is down: more likely floor by
     * floor ({@link #REINFORCE_PER_FLOOR}), more again in hard mode, never more than {@link #REINFORCE_MAX}.
     * The second group is {@link #REINFORCE_SHARE} of the room's enemies again.
     */
    static final float REINFORCE_BASE = 0.12f;
    static final float REINFORCE_PER_FLOOR = 0.07f;
    static final float REINFORCE_HARD = 0.15f;
    static final float REINFORCE_MAX = 0.7f;
    static final float REINFORCE_SHARE = 0.6f;
    private float spawnCooldown;
    /**
     * The doors open this long after the last enemy goes down: a roll toward the door you came in by
     * used to carry you straight out - maybe into a new fight - before you'd had a breather.
     */
    static final float DOOR_DELAY = 1f;
    private float clearWait;
    /** Guns still to hand out on this floor (shared with the sea below, so neither hands one out twice). */
    private final Array<Weapon> guns;
    /** Items still to hand out on this floor (shuffled; minus the ones the player already has). */
    private final Array<Item> items;
    private int combatRoomsCleared;
    private boolean bossStarted;
    private boolean bossDead;
    /** The floor's guaranteed key drops when this many combat rooms have been cleared. */
    private final int keyRoom = MathUtils.random(1, 3);
    /**
     * What the floor's first treasure chest held (null until one is opened up): like ETG, a floor with two
     * treasure rooms gives one gun and one item rather than two of a kind.
     */
    private Boolean treasureGun;
    private boolean finished;
    /** The floor ended in the family's rocket (after the Fluffy King): the next floor is the Moon. */
    private boolean boardedRocket;
    /** 1 for the first floor; later floors replay the village (until stage 2 exists), harder. */
    private final int floor;
    private boolean atBoss;
    /** Which boss guards this floor, rolled once per floor like in ETG (the clouds: the Fluffy King himself). */
    private final Boss boss;
    /** Chance that a town/city combat room hides a LampMimic among its lamps: about one every few runs. */
    private static final float MIMIC_CHANCE = 0.04f;

    /** The trap running in this room (the Slums' trap rooms), or null. */
    private TrapRoom trap;
    /** Enemies join the trap: a gunner who suits it first, then rat packs to chase you into it. */
    private float trapSpawn;
    private int trapWaves;
    private static final int TRAP_WAVES = 5;
    private static final float TRAP_WAVE_GAP = 4f;

    private String banner;
    private float bannerTimer;
    private float bannerTotal;

    /** A new run; dev builds can start it on a later floor, at the boss room or in the shop. */
    public StoryDirector(int floor, boolean atBoss) {
        this(floor, new Array<>(), new Array<>());
        this.atBoss = atBoss;
    }

    /** The Slums count as this floor (they're entered from the city, floor 3): as hard as the skyscraper. */
    public static final int SLUMS_FLOOR = 4;

    /** A new floor; guns (by name) and items the player already carries aren't handed out again. */
    public StoryDirector(int floor, Array<String> ownedGuns, Array<Item> ownedItems) {
        this(floor, Stage.forFloor(floor), ownedGuns, ownedItems);
    }

    /** The Undercity counts as the last floor: beating its boss ends the run (the normal ending). */
    public static final int UNDERCITY_FLOOR = 5;

    /** The Moon (up the family's rocket after the Fluffy King) counts as one floor past the clouds: the hardest yet. */
    public static final int MOON_FLOOR = 6;

    /** A new run that starts on the Moon (dev builds: -Dfluffy.moon=true; with bossRoom, at its boss). */
    public static StoryDirector moon(boolean atBoss) {
        StoryDirector d = new StoryDirector(MOON_FLOOR, Stage.MOON, new Array<>(), new Array<>());
        d.atBoss = atBoss;
        return d;
    }

    /** A new run that starts down in the Undercity (dev builds: -Dfluffy.undercity=true; with bossRoom, at its boss). */
    public static StoryDirector undercity(boolean atBoss) {
        StoryDirector d = new StoryDirector(UNDERCITY_FLOOR, Stage.UNDERCITY, new Array<>(), new Array<>());
        d.atBoss = atBoss;
        return d;
    }

    /** A new run that starts down in the Slums (dev builds: -Dfluffy.slums=true; with bossRoom, at its boss). */
    public static StoryDirector slums(boolean atBoss) {
        StoryDirector d = new StoryDirector(SLUMS_FLOOR, Stage.SLUMS, new Array<>(), new Array<>());
        d.atBoss = atBoss;
        return d;
    }

    /** A floor of this stage (a secret one - the Slums - or the story's own for the floor). */
    public StoryDirector(int floor, Stage stage, Array<String> ownedGuns, Array<Item> ownedItems) {
        this.floor = floor;
        this.stage = stage;
        map = new StageMap(stage.roomCount(), stage);
        boss = pickBoss();
        guns = new Array<>();
        items = new Array<>();
        setUpBossRoom();
        for (Weapon w : Weapon.chestGuns()) {
            if (!ownedGuns.contains(w.getName(), false)) {
                guns.add(w);
            }
        }
        guns.shuffle();
        for (Item i : Item.shuffledPool()) {
            if (!ownedItems.contains(i, true)) {
                items.add(i);
            }
        }
    }

    /**
     * The secret sea under the clouds (dived into from {@code above}'s floor): a smaller map with no shop,
     * a sunken ship for treasure, and the shark's lair - sharing the floor's chest pools.
     */
    private StoryDirector(StoryDirector above) {
        floor = above.floor;
        stage = Stage.SEA;
        map = new StageMap(MathUtils.random(6, 7), stage);
        boss = Boss.SHARK; // (always: the sea has just the one)
        guns = above.guns;
        items = above.items;
    }

    /**
     * The floor's boss, rolled from its stage's - but the Walking Building only where its street is entered
     * from the side (it comes down the street from the far end; dev builds can still force it anywhere).
     */
    private Boss pickBoss() {
        Boss b = Boss.pickFor(stage);
        for (int i = 0; i < 20 && b == Boss.WALKING_BUILDING && map.boss != null && !map.boss.onlySideDoors()
                && Dev.text("boss") == null; i++) {
            b = Boss.pickFor(stage);
        }
        return b;
    }

    /** The boss arena suits its boss: its layout (open, cover, cars), its height, its own look. */
    private void setUpBossRoom() {
        if (map.boss == null) {
            return;
        }
        map.boss.template = boss.arenaLayout();
        if (boss.arenaRows() != map.boss.arena.rows || boss.arenaCols() != map.boss.arena.cols) {
            map.boss.arena = new Arena(boss.arenaCols(), boss.arenaRows(), boss.sideDoorFromTop());
            map.boss.rebuildWalls();
        }
        if (boss.arenaTheme() != null) {
            map.boss.theme = boss.arenaTheme();
        }
    }

    /**
     * Down the steps behind the burning drum: the Slums, instead of the rest of the city (like ETG's
     * Oubliette, there's no coming back up). The city's chest pools carry on, so nothing is handed out twice.
     */
    private StoryDirector(StoryDirector city, Stage below) {
        floor = SLUMS_FLOOR;
        stage = below;
        map = new StageMap(stage.roomCount(), stage);
        boss = pickBoss();
        guns = city.guns;
        items = city.items;
        setUpBossRoom();
    }

    /** The player went down the hidden steps in the city: the Slums. */
    public StoryDirector intoSlums() {
        return new StoryDirector(this, Stage.SLUMS);
    }

    /** The player dived through the hole in the clouds: the sea below this floor. */
    public StoryDirector diveIntoSea() {
        return new StoryDirector(this);
    }

    /** Back up from the sea in the room they dived from: that room is done - a fight they dove out of counts as won. */
    public void surfaced(GameWorld world) {
        queue.clear();
        reinforcements.clear();
        if (room != null && room.isLocked()) {
            clearRoom(world);
        }
    }

    /** "1. THE VILLAGE" (secret floors - the sea, the Slums - have no number) */
    public String stageTitle() {
        return stage.isSecret() ? stage.title : floor + ". " + stage.title;
    }

    public Room startRoom() {
        if (Dev.flag("shop") && map.shop != null) { // dev builds: -Dfluffy.shop=true starts in the shop
            return map.shop;
        }
        if (Dev.flag("fight")) { // dev builds: -Dfluffy.fight=true starts in a roomy fight, to try a floor's enemies
            Room best = null;
            for (Room r : map.rooms) {
                if (r.type == Room.Type.COMBAT && !r.arena.isSmall() && (best == null || r.depth > best.depth)) best = r;
            }
            if (best != null) return best;
        }
        String trapKind = Dev.text("trap"); // dev builds: -Dfluffy.slums=true -Dfluffy.trap=acid (or pipes)
        if (trapKind != null) {
            for (Room r : map.rooms) {
                if (r.trap != null) {
                    r.trap = trapKind.startsWith("p") ? TrapRoom.Kind.BURST_PIPES : TrapRoom.Kind.ACID_FLOOD;
                    return r;
                }
            }
        }
        if (Dev.flag("slumsWay")) { // dev builds: -Dfluffy.floor=3 -Dfluffy.slumsWay=true starts by the burning drum
            for (Room r : map.rooms) {
                if (r.slumsWay) return r;
            }
        }
        return atBoss && map.boss != null ? map.boss : map.start;
    }

    private void showBanner(String text, float seconds) {
        banner = text;
        bannerTimer = seconds;
        bannerTotal = seconds;
    }

    @Override
    public void onRoomEntered(Room r, GameWorld world) {
        room = r;
        boolean first = !r.visited;
        r.visited = true;
        // the sea is two steps harder than the clouds above it: whoever finds it comes with a strong build
        int level = stage.isSea() ? floor + SEA_LEVELS_UP : floor;
        // the skyscraper gets harder storey by storey: a steeper climb that goes higher than other floors
        float roomStep = stage.isTower() ? Math.min(1.5f, 1f + 0.07f * r.depth) : Math.min(1.35f, 1f + 0.05f * r.depth);
        world.setDifficulty(roomStep + 0.15f * (level - 1));
        // floor 2: +15% health, floor 3: +30%; in the skyscraper +4% more each stop up
        world.setEnemyHealth(1f + 0.15f * (level - 1) + (stage.isTower() ? 0.04f * r.depth : 0f));
        switch (r.type) {
            case START:
                if (first) {
                    r.cleared = true;
                    showBanner(stageTitle(), 2.5f);
                    if (stage.isMoon()) { // the rocket you came up in, parked, the pilot waiting by it
                        world.parkRocket(Arena.now.centerX + 1.8f, Arena.now.centerY + 0.6f);
                    }
                }
                break;
            case TREASURE:
                if (first) {
                    r.cleared = true;
                    Tier tier = blackChest(world, rollTier(world, 30, 35, 25, 10));
                    Boolean want = treasureGun == null ? null : !treasureGun; // the second one is the other kind
                    Chest chest = chest(world, tier, Arena.now.centerX, Arena.now.centerY, true, want);
                    world.placeChest(chest);
                    if (chest.gun != null || chest.item != null) {
                        treasureGun = chest.gun != null;
                    }
                    showBanner("TREASURE!", 1.5f);
                }
                break;
            case SHOP:
                if (first) { // Baa-llo sets out his wares: two from the chest pools, a heart, a key, a blank or ammo
                    r.cleared = true;
                    Shop shop = new Shop(floor, false);
                    // the first floor's shop always has a gun, of the lowest tier (a gun isn't guaranteed otherwise)
                    Tier firstTier = floor == 1 ? Tier.D : rollTier(world, 40, 35, 20, 5);
                    // the city always sells something that shoots water (the way down to the Slums needs it),
                    // in an ordinary gun slot - unless the player already carries one
                    Weapon water = stage == Stage.CITY && !world.getPlayer().hasWetGun() ? takeWaterGun() : null;
                    shop.stock(guns, items, new Tier[]{firstTier, rollTier(world, 20, 35, 30, 15)},
                            world.getPlayer()::hasItem, world.isShopGrudge(), floor == 1, water);
                    if (stage == Stage.CITY && !world.getPlayer().hasItem(Item.FLIPPERS)) { // only ever sold here
                        shop.addFlippers(world.isShopGrudge());
                    }
                    Weapon flufful = floor == 1 && world.getPlayer().getHero().easy() ? takeFlufful() : null;
                    if (flufful != null) { // the baby's first shop: the talking gun, if he wants it
                        shop.addFlufful(flufful, world.isShopGrudge());
                    }
                    shop.rumour = stage == Stage.CITY; // Baa-llo has heard things rumbling under the city
                    world.openShop(shop);
                    showBanner("BAA-LLO'S SHOP", 1.6f);
                }
                break;
            case COMBAT:
                // very rarely one of the room's street lamps is a mimic (dev builds: -Dfluffy.mimic=true)
                if (first && stage != Stage.VILLAGE && r.trap == null && (Dev.flag("mimic") || MathUtils.random() < MIMIC_CHANCE)) {
                    world.hideMimicLamp();
                }
                if (first && stage.isTower()) { // stepping out of the elevator
                    showBanner("FLOOR " + r.floorNumber, 1.4f);
                }
                break;
            // (the boss gets its introduction card from GameScreen when it appears, instead of a banner)
            default:
                break;
        }
    }

    /**
     * Who joins a trap: first a gunner who suits it - a Sludger adding to the rising acid, a Shell Cop
     * pinning you between the jets - then rat packs (marked ones deeper in) to chase you into the hazards.
     */
    private int trapWave(int wave) {
        if (wave == 0) {
            return room.trap == TrapRoom.Kind.ACID_FLOOD ? SLUDGER : COP;
        }
        return room.depth >= 2 && MathUtils.randomBoolean() ? MARKED_RATS : RATS;
    }

    @Override
    public String hudNote() {
        if (trap == null) return null;
        return trap.isOver() ? "FINISH THEM!" : "SURVIVE " + MathUtils.ceil(trap.timeLeft());
    }

    @Override
    public boolean inFight(GameWorld world) {
        return room != null && room.isLocked();
    }

    @Override
    public void update(float delta, GameWorld world) {
        bannerTimer -= delta;
        if (world.getPlayer().isDead() || room == null) {
            return;
        }
        if (bossDead) { // the way on is open - but rooms not yet seen still have their fights waiting
            finished |= world.isExitTaken();
            boardedRocket |= world.isRocketBoarded();
        }
        boolean fightRoom = room.type == Room.Type.COMBAT || room.type == Room.Type.BOSS;
        if (fightRoom && !room.cleared && !room.isLocked() && world.playerInsideRoom(0.45f)) {
            startFight(world);
        }
        if (room.isLocked() && trap != null) { // a trap room: survive the clock, then finish off who's left
            int active = world.getEnemies().size + world.getPendingEnemies().size;
            trapSpawn -= delta;
            if (!trap.isOver() && trapSpawn <= 0 && trapWaves < TRAP_WAVES && active < 8) {
                trapSpawn = TRAP_WAVE_GAP;
                spawn(world, trapWave(trapWaves++));
            }
            clearWait = trap.isOver() && active == 0 ? clearWait + delta : 0f;
            if (clearWait >= DOOR_DELAY) {
                trap = null;
                world.setTrap(null);
                showBanner("YOU MADE IT!", 1.5f);
                clearRoom(world);
            }
            return;
        }
        if (room.isLocked()) {
            spawnCooldown -= delta;
            int active = world.getEnemies().size + world.getPendingEnemies().size;
            int maxActive = smallRoom() ? 2 + room.depth / 4 : stage.isTower() ? 5 + room.depth / 2 : 5 + room.depth / 3;
            if (stage.isSea()) maxActive++; // shoals
            if (world.isHard()) maxActive++;
            if (queue.size > 0 && spawnCooldown <= 0 && active < maxActive) {
                spawn(world, queue.pop());
                spawnCooldown = 0.45f;
            }
            if (queue.size == 0 && active == 0 && reinforcements.size > 0) { // the first group is down: here come more
                queue.addAll(reinforcements);
                reinforcements.clear();
                spawnCooldown = 0.8f;
                showBanner("MORE ARE COMING!", 1.4f);
                world.sfx().play("door", 0.8f);
                world.shake(0.15f);
            }
            boolean done = queue.size == 0 && reinforcements.size == 0 && active == 0 && !world.bossDying();
            clearWait = done ? clearWait + delta : 0f;
            if (done && (clearWait >= DOOR_DELAY || room.type == Room.Type.BOSS)) { // (a boss's death show is the breather)
                clearRoom(world);
            }
        }
    }

    /** The Flufful out of the chest pool (it's only there once unlocked), or null. */
    private Weapon takeFlufful() {
        for (Weapon w : guns) {
            if (w.isFlufful()) {
                guns.removeValue(w, true);
                return w;
            }
        }
        return null;
    }

    /** A water gun out of the pool (a fresh one if the pool has none left - it's needed). */
    private Weapon takeWaterGun() {
        Array<Weapon> wet = new Array<>();
        for (Weapon w : guns) {
            if (w.isWet()) wet.add(w);
        }
        if (wet.size == 0) {
            return Weapon.waterPistol();
        }
        Weapon w = wet.random();
        guns.removeValue(w, true);
        return w;
    }

    private void startFight(GameWorld world) {
        room.setLocked(true);
        clearWait = 0f;
        world.sfx().play("door", 1f);
        world.shake(0.25f);
        queue.clear();
        reinforcements.clear();
        spawnCooldown = 0.6f;
        if (room.trap != null) { // no enemies: just the trap
            trap = new TrapRoom(room.trap, room.arena);
            world.setTrap(trap);
            trapSpawn = 1.5f;
            trapWaves = 0;
            showBanner(room.trap.banner, 2f);
            return;
        }
        if (room.type == Room.Type.BOSS) {
            bossStarted = true;
            Enemy b = boss.create(Arena.now.centerX, boss.spawnY(Arena.now, world.getPlayer().feetY()), 0);
            int percent = Dev.number("bossHp", 100); // dev builds: -Dfluffy.bossHp=40 starts it at 40% (to try its later phases)
            if (percent < 100) {
                b.hp = b.maxHp * Math.max(1, percent) / 100f;
            }
            world.spawnEnemy(b);
            world.sfx().play("boss", 1f);
            return;
        }
        // a small trap room (pits, no space to run) gets about half the enemies, fewer at a time
        boolean small = smallRoom();
        int budget = small ? 2 + room.depth / 2 + MathUtils.random(0, 1)
                : stage.isTower() ? 4 + room.depth * 3 / 2 + MathUtils.random(0, 2) // a big crowd near the top
                : 3 + room.depth + MathUtils.random(0, 2);
        if (stage.isSea()) {
            budget += SEA_EXTRA_BUDGET;
        }
        hard = world.isHard();
        if (hard) { // hard mode: a crowd
            budget = Math.round(budget * HARD_BUDGET);
        }
        roll(queue, budget, small);
        if (!small && MathUtils.random() < reinforceChance(floor, hard)) {
            roll(reinforcements, Math.max(2, Math.round(budget * REINFORCE_SHARE)), false);
        }
    }

    /** How likely a (roomy) combat room on this story floor is to send in a second group. */
    static float reinforceChance(int floor, boolean hard) {
        float chance = REINFORCE_BASE + REINFORCE_PER_FLOOR * (floor - 1) + (hard ? REINFORCE_HARD : 0f);
        return Math.min(REINFORCE_MAX, chance);
    }

    /** Rolls a group of this floor's enemies worth {@code budget} into {@code into}, shuffled. */
    private void roll(IntArray into, int budget, boolean small) {
        int hydrants = 0;
        int snipers = 0;
        int bikers = 0;
        int cubes = 0;
        int storms = 0;
        int divers = 0;
        int asteroids = 0;
        int[] slumsCount = new int[NAMES.length];
        int ghosts = 0;
        boolean sludger = false;
        int forced = forcedEnemy();
        while (budget > 0 && forced >= 0) { // dev builds: just this kind
            into.add(forced);
            budget -= cost(forced);
        }
        while (budget > 0) {
            if (stage.isMoon()) { // only the Moon's own folk up here
                int kind = MoonFolk.pick(budget, asteroids);
                if (kind == MoonFolk.ASTEROID) asteroids++;
                into.add(MOON + kind);
                budget -= MoonFolk.cost(kind);
                continue;
            }
            if (stage.isSea()) { // only its own creatures down here
                int kind = SeaCreatures.pick(budget, divers);
                if (kind == SeaCreatures.DIVER) divers++;
                into.add(SEA + kind);
                budget -= SeaCreatures.cost(kind);
                continue;
            }
            if (!sludger && budget >= 2 && room.depth >= 2 && (stage == Stage.TOWN || stage == Stage.CITY
                    || stage == Stage.SKYSCRAPER) && MathUtils.random() < SLUDGER_CHANCE) {
                sludger = true;
                into.add(SLUDGER);
                budget -= 2;
                continue;
            }
            if (stage.isUnderground()) { // the bad part of town / the False King's streets
                int type = stage.isUndercity() ? undercityEnemy(budget, slumsCount, small)
                        : slumsEnemy(budget, slumsCount, small);
                if (type == GHOST && ghosts++ >= MAX_GHOSTS) type = BANDANA;
                type = grownUp(type);
                slumsCount[type]++;
                into.add(type);
                budget -= cost(type);
                continue;
            }
            int type = stage == Stage.VILLAGE ? villageEnemy(budget)
                    : stage == Stage.TOWN ? townEnemy(budget, hydrants)
                    : stage == Stage.CITY ? cityEnemy(budget, snipers, bikers < (small ? 1 : MAX_BIKERS))
                    : stage == Stage.SKYSCRAPER ? towerEnemy(budget, snipers, cubes)
                    : skyEnemy(budget, storms, cubes);
            if (type == GHOST && ghosts++ >= MAX_GHOSTS) type = BANDANA;
            type = grownUp(type);
            if (type == HYDRANT) hydrants++;
            if (type == SNIPER) snipers++;
            if (type == BIKER) bikers++;
            if (type == MOUNTAIN_CUBE || type == LEAD_CUBE) cubes++;
            if (type == THUNDERCLOUD) storms++;
            into.add(type);
            budget -= cost(type);
        }
        into.shuffle();
    }

    /** Higher up the old easy ones come as their tougher kinds, with patterns of their own. */
    private int grownUp(int type) {
        int f = hard ? floor + 1 : floor; // hard mode: the tougher kinds a floor sooner
        if (type == KIN && f >= VETERAN_FLOOR) return VETERAN;
        if (type == SHOTGUN && f >= TOUGH_FLOOR) return SAWN_OFF;
        if (type == COP && f >= TOUGH_FLOOR) return RIOT_COP;
        return type;
    }

    /** A one-screen combat room: the town's and city's are full of pits. */
    private boolean smallRoom() {
        return room.type == Room.Type.COMBAT && room.arena.isSmall();
    }

    private int villageEnemy(int budget) {
        float r = MathUtils.random();
        if (room.depth >= 2 && budget >= 2 && r < 0.07f) return DYNAMITE;
        if (room.depth >= 2 && budget >= 3 && r < 0.22f) return MANCER;
        if (room.depth >= 1 && budget >= 2 && r < 0.42f) return SHOTGUN;
        if (budget >= 2 && r < 0.7f) return gunKin(budget);
        return KIN;
    }

    /** The town mixes its own enemies (hydrants, rat packs, shell cops) with the village's. */
    private int townEnemy(int budget, int hydrants) {
        float r = MathUtils.random();
        if (budget >= 3 && r < 0.07f) return SHAPESHIFTER;
        if (budget >= 2 && r < 0.14f) return DYNAMITE;
        if (budget >= 2 && r < 0.28f) return COP;
        if (budget >= 2 && r < 0.4f) return RATS;
        if (budget >= 2 && hydrants < 2 && r < 0.5f) return HYDRANT;
        if (budget >= 2 && r < 0.6f) return SHOTGUN;
        if (budget >= 3 && r < 0.65f) return MANCER;
        if (budget >= 3 && room.depth >= 2 && r < 0.7f) return SPIRALMANCER;
        if (budget >= 2 && r < 0.88f) return gunKin(budget);
        return KIN;
    }

    /** The city's own enemies (bikers, snipers) plus some of the town's and the village's. */
    private int cityEnemy(int budget, int snipers, boolean bikerAllowed) {
        float r = MathUtils.random();
        if (budget >= 2 && r < 0.14f) return ROBBER;
        if (budget >= 2 && bikerAllowed && r < 0.28f) return BIKER;
        if (budget >= 2 && snipers < 2 && r < 0.38f) return SNIPER;
        if (budget >= 3 && r < 0.45f) return SHAPESHIFTER;
        if (budget >= 2 && r < 0.52f) return DYNAMITE;
        if (budget >= 2 && r < 0.6f) return COP;
        if (budget >= 2 && r < 0.67f) return RATS;
        if (budget >= 2 && r < 0.74f) return SHOTGUN;
        if (budget >= 3 && r < 0.76f) return MANCER;
        if (budget >= 3 && r < 0.8f) return SPIRALMANCER;
        if (budget >= 3 && r < 0.84f) return BLOOMANCER;
        if (budget >= 2 && r < 0.95f) return gunKin(budget);
        return KIN;
    }

    /**
     * The Slums: the acid gang at home - Sludgers lobbing globs, Sprayers hosing arcs, Barrel Rollers
     * bowling drums (a couple of each a room at most, fewer in the small rooms) - rat packs, robbers,
     * dynamite and the city's cops; and, deeper in, a few marked critters (rats, stink pigeons) as a hint
     * of what's below. No bikers (no room to ride in these alleys), no snipers.
     */
    private int slumsEnemy(int budget, int[] count, boolean small) {
        int cap = small ? 1 : 2;
        float r = MathUtils.random();
        if (budget >= 2 && count[SLUDGER] < cap && r < 0.13f) return SLUDGER;
        if (budget >= 2 && count[SPRAYER] < cap && r < 0.26f) return SPRAYER;
        if (budget >= 2 && count[ROLLER] < cap && r < 0.36f) return ROLLER;
        if (budget >= 2 && room.depth >= 2 && count[MARKED_RATS] < 1 && r < 0.42f) return MARKED_RATS;
        if (budget >= 2 && room.depth >= 2 && count[STINK_PIGEON] < cap && r < 0.47f) return STINK_PIGEON;
        if (budget >= 2 && r < 0.56f) return RATS;
        if (budget >= 2 && r < 0.66f) return ROBBER;
        if (budget >= 2 && r < 0.72f) return DYNAMITE;
        if (budget >= 2 && r < 0.81f) return COP;
        if (budget >= 2 && r < 0.89f) return SHOTGUN;
        if (budget >= 3 && r < 0.93f) return SHAPESHIFTER;
        if (budget >= 2 && r < 0.97f) return gunKin(budget);
        return KIN;
    }

    /**
     * The Undercity: the Skunk King's marked critters at home - marked rat packs, stink pigeons, sewer gators,
     * alley dogs that howl the rats in - the odd raccoon of his court after your coins (one a room at most),
     * and a few of the Slums' acid gang and street toughs who came down with them.
     */
    private int undercityEnemy(int budget, int[] count, boolean small) {
        int cap = small ? 1 : 2;
        float r = MathUtils.random();
        if (budget >= 2 && count[MARKED_RATS] < cap + 1 && r < 0.2f) return MARKED_RATS;
        if (budget >= 2 && count[STINK_PIGEON] < cap && r < 0.33f) return STINK_PIGEON;
        if (budget >= 3 && count[GATOR] < cap && r < 0.46f) return GATOR;
        if (budget >= 2 && count[DOG] < cap && r < 0.58f) return DOG;
        if (budget >= 2 && count[RACCOON] < 1 && r < 0.64f) return RACCOON;
        if (budget >= 2 && count[SLUDGER] < 1 && r < 0.7f) return SLUDGER;
        if (budget >= 2 && count[SPRAYER] < 1 && r < 0.75f) return SPRAYER;
        if (budget >= 2 && count[ROLLER] < 1 && r < 0.79f) return ROLLER;
        if (budget >= 2 && r < 0.85f) return RATS;
        if (budget >= 2 && r < 0.9f) return DYNAMITE;
        if (budget >= 2 && r < 0.95f) return ROBBER;
        if (budget >= 2 && r < 0.98f) return gunKin(budget);
        return KIN;
    }

    /** The skyscraper's floors: its own staff and machines, plus the worst of the streets below. */
    private int towerEnemy(int budget, int snipers, int cubes) {
        float r = MathUtils.random();
        if (budget >= 2 && r < 0.16f) return AGENT;
        if (budget >= 2 && r < 0.3f) return ANDROID;
        if (budget >= 2 && r < 0.42f) return ROBOCART;
        if (budget >= 2 && cubes < 2 && r < 0.5f) return MOUNTAIN_CUBE;
        if (budget >= 3 && cubes < 2 && room.depth >= 3 && r < 0.56f) return LEAD_CUBE; // the tough ones higher up
        if (budget >= 2 && r < 0.64f) return ROBBER;
        if (budget >= 3 && room.depth >= 2 && r < 0.71f) return SHAPESHIFTER;
        if (budget >= 2 && r < 0.79f) return DYNAMITE;
        if (budget >= 2 && snipers < 2 && r < 0.84f) return SNIPER;
        if (budget >= 2 && r < 0.88f) return SHOTGUN;
        if (budget >= 3 && r < 0.92f) return MathUtils.randomBoolean() ? SPIRALMANCER : BLOOMANCER;
        if (budget >= 2 && r < 0.97f) return gunKin(budget);
        return KIN;
    }

    /**
     * The clouds: angels with swords and bows, thunderclouds, cherubs, dandelion puffs, the mancers' fancier
     * cousins and a few old faces who made it up here - but no plain bullet kin (too easy this high up).
     */
    private int skyEnemy(int budget, int storms, int cubes) {
        float r = MathUtils.random();
        if (budget >= 2 && r < 0.18f) return SWORD_ANGEL;
        if (budget >= 2 && r < 0.34f) return BOW_ANGEL;
        if (budget >= 2 && storms < 2 && r < 0.46f) return THUNDERCLOUD;
        if (budget >= 2 && r < 0.61f) return CHERUB;
        if (budget >= 2 && r < 0.75f) return DANDELION;
        if (budget >= 3 && r < 0.82f) return MathUtils.randomBoolean() ? SPIRALMANCER : BLOOMANCER;
        if (budget >= 3 && r < 0.86f) return SHAPESHIFTER;
        if (budget >= 2 && cubes < 1 && r < 0.89f) return MOUNTAIN_CUBE; // ice: right at home this high up
        if (budget >= 2 && r < 0.93f) return DYNAMITE;
        if (budget >= 2 && r < 0.96f) return ANDROID;
        if (budget >= 2 && r < 0.98f) return gunKin(budget);
        return CHERUB; // even the last point of the budget goes to a cloud creature
    }

    /**
     * The rest of the gun kin - bandanas with machine pistols, pirates with hand cannons, blue shotgun
     * shells, helmeted tankers and ghosts - turn up on every floor from the first (the tankers and ghosts
     * deeper in on floor 1). Only called with a budget of 2 or more.
     */
    private int gunKin(int budget) {
        boolean deep = floor > 1 || room.depth >= 2;
        float r = MathUtils.random();
        if (deep && budget >= 3 && r < 0.17f) return TANKER;
        if (deep && r < 0.33f) return GHOST;
        if ((deep || room.depth >= 1) && r < 0.53f) return BLUE_SHOTGUN;
        if (r < 0.77f) return BANDANA;
        return PIRATE;
    }

    private static int cost(int type) {
        return type == MANCER || type == SPIRALMANCER || type == BLOOMANCER || type == SHAPESHIFTER || type == LEAD_CUBE || type == GATOR || type == TANKER ? 3 : type == KIN || type == VETERAN ? 1 : 2;
    }

    private void spawn(GameWorld world, int type) {
        Vector2 p = world.findSpawnPoint(1.8f);
        if (type >= MOON) {
            MoonFolk.spawn(world, type - MOON, p.x, p.y);
            return;
        }
        if (type >= SEA) {
            world.spawnEnemy(SeaCreatures.create(type - SEA, p.x, p.y));
            return;
        }
        switch (type) {
            case HYDRANT:
                world.spawnEnemy(new Hydrant(p.x, p.y));
                break;
            case RATS: // a pack of three
                for (int i = 0; i < 3; i++) {
                    world.spawnEnemy(new Rat(p.x + MathUtils.random(-0.3f, 0.3f), p.y + MathUtils.random(-0.3f, 0.3f)));
                }
                break;
            case COP:
                world.spawnEnemy(new ShellCop(p.x, p.y));
                break;
            case BIKER:
                world.spawnEnemy(new Biker(p.x, p.y));
                break;
            case SNIPER:
                world.spawnEnemy(new Sniper(p.x, p.y));
                break;
            case ROBBER:
                world.spawnEnemy(new Robber(p.x, p.y));
                break;
            case DYNAMITE:
                world.spawnEnemy(new Dynamite(p.x, p.y));
                break;
            case SHAPESHIFTER:
                world.spawnEnemy(new Shapeshifter(p.x, p.y));
                break;
            case ROBOCART:
                world.spawnEnemy(new RoboCart(p.x, p.y));
                break;
            case ANDROID:
                world.spawnEnemy(new Android(p.x, p.y));
                break;
            case AGENT:
                world.spawnEnemy(new Agent(p.x, p.y));
                break;
            case MOUNTAIN_CUBE:
                world.spawnEnemy(Cube.mountain(p.x, p.y));
                break;
            case LEAD_CUBE:
                world.spawnEnemy(Cube.lead(p.x, p.y));
                break;
            case SWORD_ANGEL:
                world.spawnEnemy(new SwordAngel(p.x, p.y));
                break;
            case BOW_ANGEL:
                world.spawnEnemy(new BowAngel(p.x, p.y));
                break;
            case THUNDERCLOUD:
                world.spawnEnemy(new Thundercloud(p.x, p.y));
                break;
            case SLUDGER:
                world.spawnEnemy(new Sludger(p.x, p.y));
                break;
            case SPRAYER:
                world.spawnEnemy(new AcidSprayer(p.x, p.y));
                break;
            case ROLLER:
                world.spawnEnemy(new BarrelRoller(p.x, p.y));
                break;
            case MARKED_RATS: // a pack of three gone wrong
                for (int i = 0; i < 3; i++) {
                    world.spawnEnemy(Rat.marked(p.x + MathUtils.random(-0.3f, 0.3f), p.y + MathUtils.random(-0.3f, 0.3f)));
                }
                break;
            case STINK_PIGEON:
                world.spawnEnemy(new StinkPigeon(p.x, p.y));
                break;
            case GATOR:
                world.spawnEnemy(new SewerGator(p.x, p.y));
                break;
            case DOG:
                world.spawnEnemy(new AlleyDog(p.x, p.y));
                break;
            case RACCOON:
                world.spawnEnemy(new Raccoon(p.x, p.y));
                break;
            case SHOTGUN:
                world.spawnEnemy(new ShotgunKin(p.x, p.y));
                break;
            case PIRATE:
                world.spawnEnemy(new PirateKin(p.x, p.y));
                break;
            case BANDANA:
                world.spawnEnemy(new BandanaKin(p.x, p.y));
                break;
            case TANKER:
                world.spawnEnemy(new TankerKin(p.x, p.y));
                break;
            case GHOST:
                world.spawnEnemy(new GhostKin(p.x, p.y));
                break;
            case BLUE_SHOTGUN:
                world.spawnEnemy(new BlueShotgunKin(p.x, p.y));
                break;
            case MANCER:
                world.spawnEnemy(new Bulletmancer(p.x, p.y));
                break;
            case SPIRALMANCER:
                world.spawnEnemy(new Spiralmancer(p.x, p.y));
                break;
            case BLOOMANCER:
                world.spawnEnemy(new Bloomancer(p.x, p.y));
                break;
            case CHERUB:
                world.spawnEnemy(new Cherub(p.x, p.y));
                break;
            case DANDELION:
                world.spawnEnemy(new Dandelion(p.x, p.y));
                break;
            case VETERAN:
                world.spawnEnemy(new VeteranKin(p.x, p.y));
                break;
            case SAWN_OFF:
                world.spawnEnemy(new SawnOffKin(p.x, p.y));
                break;
            case RIOT_COP:
                world.spawnEnemy(new RiotCop(p.x, p.y));
                break;
            default:
                world.spawnEnemy(new BulletKin(p.x, p.y));
                break;
        }
    }

    private void clearRoom(GameWorld world) {
        room.cleared = true;
        room.setLocked(false);
        world.sfx().play("wave_clear", 0.8f);
        world.addScore(25 + room.depth * 10);

        if (room.type == Room.Type.BOSS) {
            bossDead = true;
            world.spawnPickup(new Pickup(Pickup.Type.HEART, Arena.now.centerX, Arena.now.bottom + 0.8f));
            world.dropCoins(Arena.now.centerX + 1.6f, Arena.now.centerY - 1f, Coins.forBoss(floor));
            Vector2 spot = world.chestSpotNear(Arena.now.centerX, Arena.now.centerY - 0.35f);
            Tier bossTier = rollTier(world, 0, 40, 40, 20);
            if (world.isLastBossFlawless()) { // not a scratch: now and then the boss chest comes out black
                bossTier = blackChest(world, bossTier);
            }
            world.placeChest(chest(world, bossTier, spot.x, spot.y, false));
            world.openExit();
            if (stage == Stage.CLOUDS && Unlocks.rocketBuilt()) { // the family's rocket comes for them
                world.landRocket();
            }
            showBanner(boss.defeated, 3f);
            return;
        }

        combatRoomsCleared++;
        world.fluffulSays(FluffulVoice.Event.ROOM_CLEAR);
        world.dropCoins(Arena.now.centerX, Arena.now.centerY - 0.6f, Coins.forRoom(room.depth, MathUtils.random(0, 2)));
        if (combatRoomsCleared == keyRoom) { // the floor's one guaranteed key
            world.spawnPickup(new Pickup(Pickup.Type.KEY, Arena.now.centerX, Arena.now.centerY));
            showBanner("ROOM CLEARED - A KEY!", 1.5f);
        } else if (MathUtils.random() < 0.08f) { // rarely, a locked chest
            Vector2 p = world.findChestSpot();
            world.placeChest(chest(world, rollTier(world, 55, 35, 10, 0), p.x, p.y, true));
            showBanner("ROOM CLEARED - A CHEST!", 1.5f);
        } else {
            Pickup.Type reward = roomReward(world);
            if (reward != null) {
                world.spawnPickup(new Pickup(reward, Arena.now.centerX, Arena.now.centerY));
            }
        }
    }

    /**
     * Small reward for a cleared room, ETG-stingy: usually only something you need (half a heart when
     * hurt, ammo when low), rarely a blank, very rarely an extra key. Often nothing.
     */
    private Pickup.Type roomReward(GameWorld world) {
        if (MathUtils.random() < 0.03f) {
            return Pickup.Type.KEY;
        }
        Array<Pickup.Type> needs = new Array<>();
        if (world.getPlayer().getHealth() < world.getPlayer().getMaxHealth()) {
            needs.add(Pickup.Type.HALF_HEART);
        }
        if (world.hasLimitedGunNeedingAmmo() && !world.isAmmoDropped()) { // not if the room already dropped one
            needs.add(world.ammoPickup());
        }
        if (needs.size > 0 && MathUtils.random() < 0.25f) {
            Pickup.Type need = MathUtils.random() < 0.2f ? Pickup.Type.BLANK : needs.random();
            if (need == Pickup.Type.AMMO || need == Pickup.Type.SPREAD_AMMO) world.takeAmmoDrop();
            return need;
        }
        return MathUtils.random() < 0.1f ? Pickup.Type.BLANK : null;
    }

    /** Random chest tier; weights for D, C, B, A. Later floors (and the Lucky Clover) shift the odds up. */
    private Tier rollTier(GameWorld world, int d, int c, int b, int a) {
        Tier[] tiers = Tier.values();
        int roll = MathUtils.random(d + c + b + a - 1);
        int[] weights = {d, c, b, a};
        int i = 0;
        while (roll >= weights[i]) {
            roll -= weights[i++];
        }
        int top = Tier.A.ordinal(); // S only ever comes from a black chest
        if (floor > 1 && MathUtils.random() < 0.25f * (floor - 1)) {
            i = Math.min(i + 1, top);
        }
        if (world.getPlayer().hasItem(Item.LUCKY_CLOVER) && MathUtils.random() < 0.3f) {
            i = Math.min(i + 1, top);
        }
        return tiers[i];
    }

    /** From this floor on a treasure chest (or a flawless boss's chest) may come out black. */
    static final int BLACK_CHEST_FLOOR = 3;
    static final float BLACK_CHEST_CHANCE = 1f / 12f;

    /** Like ETG's black chests: now and then (1 in 12) an S gun or item instead - while there's one left to find. */
    private Tier blackChest(GameWorld world, Tier tier) {
        boolean sLeft = false;
        for (Weapon w : guns) sLeft |= w.getTier() == Tier.S;
        for (Item i : items) sLeft |= i.tier == Tier.S && !world.getPlayer().hasItem(i);
        boolean dev = Dev.flag("blackChest");
        if (sLeft && (dev || floor >= BLACK_CHEST_FLOOR && MathUtils.random() < BLACK_CHEST_CHANCE)) {
            return Tier.S;
        }
        return tier;
    }

    /**
     * A chest with a gun or an item of that tier inside (half and half when both are left; taken out of
     * the pools), or loot if there's none left.
     */
    private Chest chest(GameWorld world, Tier tier, float x, float y, boolean locked) {
        return chest(world, tier, x, y, locked, null);
    }

    /** The same, but a gun (wantGun true) or an item (false) when that tier has one left; null: either. */
    private Chest chest(GameWorld world, Tier tier, float x, float y, boolean locked, Boolean wantGun) {
        int gun = -1;
        for (int i = 0; i < guns.size && gun < 0; i++) { // the pools are shuffled: a random one of that tier
            if (guns.get(i).getTier() == tier) gun = i;
        }
        int item = -1;
        for (int i = 0; i < items.size && item < 0; i++) {
            if (items.get(i).tier == tier && !world.getPlayer().hasItem(items.get(i))) item = i;
        }
        boolean pickItem = wantGun == null || gun < 0 || item < 0 ? MathUtils.randomBoolean() : !wantGun;
        if (item >= 0 && (gun < 0 || pickItem)) {
            return new Chest(tier, x, y, locked, null, items.removeIndex(item));
        }
        return new Chest(tier, x, y, locked, gun >= 0 ? guns.removeIndex(gun) : null, null);
    }

    @Override
    public String banner() {
        return bannerTimer > 0 ? banner : null;
    }

    @Override
    public float bannerAlpha() {
        if (bannerTimer <= 0) return 0;
        float elapsed = bannerTotal - bannerTimer;
        return MathUtils.clamp(Math.min(elapsed / 0.3f, bannerTimer / 0.5f), 0, 1);
    }

    @Override
    public String music() {
        return bossStarted && !bossDead ? boss.music() : stage.music;
    }

    /** The floor ended by climbing into the family's rocket: the King is beaten all the same, and the Moon is next. */
    public boolean boardedRocket() {
        return boardedRocket;
    }

    /** Where the way on leads, if not simply to the next story floor: up to the Moon, or down to the Undercity. */
    public Stage nextStage() {
        return boardedRocket ? Stage.MOON : stage.below();
    }

    /** True once the player goes down the exit hatch the boss leaves behind. */
    @Override
    public boolean isFinished() {
        return finished;
    }
}
