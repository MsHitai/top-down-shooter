package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.trush.game.enemy.BulletKin;
import com.trush.game.launcher.Dev;
import com.trush.game.enemy.Biker;
import com.trush.game.enemy.Bulletmancer;
import com.trush.game.enemy.Sniper;
import com.trush.game.enemy.Dynamite;
import com.trush.game.enemy.Robber;
import com.trush.game.enemy.Shapeshifter;
import com.trush.game.enemy.Sludger;
import com.trush.game.enemy.Agent;
import com.trush.game.enemy.BowAngel;
import com.trush.game.enemy.SwordAngel;
import com.trush.game.enemy.Thundercloud;
import com.trush.game.enemy.Android;
import com.trush.game.enemy.Cube;
import com.trush.game.enemy.RoboCart;
import com.trush.game.enemy.Hydrant;
import com.trush.game.enemy.Rat;
import com.trush.game.enemy.SeaCreatures;
import com.trush.game.enemy.ShellCop;
import com.trush.game.enemy.Boss;
import com.trush.game.enemy.ShotgunKin;
import com.trush.game.objects.Chest;
import com.trush.game.objects.Coins;
import com.trush.game.objects.Item;
import com.trush.game.objects.FluffulVoice;
import com.trush.game.objects.Pickup;
import com.trush.game.objects.Shop;
import com.trush.game.objects.Tier;
import com.trush.game.objects.Weapon;
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
    /** An acid gunner turns up now and then in the town, the city and the skyscraper (one a room at most). */
    private static final float SLUDGER_CHANCE = 0.05f;
    /** The sea's creatures: SEA + a SeaCreatures kind. */
    private static final int SEA = 100;

    private final StageMap map;
    /** The village on floor 1, the town on floor 2... */
    private final Stage stage;
    private Room room;
    private final IntArray queue = new IntArray();
    private float spawnCooldown;
    /** Guns still to hand out on this floor (shared with the sea below, so neither hands one out twice). */
    private final Array<Weapon> guns;
    /** Items still to hand out on this floor (shuffled; minus the ones the player already has). */
    private final Array<Item> items;
    private int combatRoomsCleared;
    private boolean bossStarted;
    private boolean bossDead;
    /** The floor's guaranteed key drops when this many combat rooms have been cleared. */
    private final int keyRoom = MathUtils.random(1, 3);
    private boolean finished;
    /** 1 for the first floor; later floors replay the village (until stage 2 exists), harder. */
    private final int floor;
    private boolean atBoss;
    /** Which boss guards this floor, rolled once per floor like in ETG (the clouds: the Fluffy King himself). */
    private final Boss boss;
    /** Chance that a town/city combat room hides a LampMimic among its lamps: about one every few runs. */
    private static final float MIMIC_CHANCE = 0.04f;

    private String banner;
    private float bannerTimer;
    private float bannerTotal;

    /** A new run; dev builds can start it on a later floor, at the boss room or in the shop. */
    public StoryDirector(int floor, boolean atBoss) {
        this(floor, new Array<>(), new Array<>());
        this.atBoss = atBoss;
    }

    /** A new floor; guns (by name) and items the player already carries aren't handed out again. */
    public StoryDirector(int floor, Array<String> ownedGuns, Array<Item> ownedItems) {
        this.floor = floor;
        stage = Stage.forFloor(floor);
        map = new StageMap(stage.roomCount(), stage);
        boss = Boss.pickFor(stage);
        guns = new Array<>();
        items = new Array<>();
        if (map.boss != null) {
            map.boss.template = boss.arenaLayout();
            if (boss.arenaRows() != map.boss.arena.rows) {
                map.boss.arena = new Arena(Arena.BOSS_COLS, boss.arenaRows());
                map.boss.rebuildWalls();
            }
            if (boss.arenaTheme() != null) {
                map.boss.theme = boss.arenaTheme();
            }
        }
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

    /** The player dived through the hole in the clouds: the sea below this floor. */
    public StoryDirector diveIntoSea() {
        return new StoryDirector(this);
    }

    /** Back up from the sea in the room they dived from: that room is done - a fight they dove out of counts as won. */
    public void surfaced(GameWorld world) {
        queue.clear();
        if (room != null && room.isLocked()) {
            clearRoom(world);
        }
    }

    /** "1. THE VILLAGE" (the secret sea has no number) */
    public String stageTitle() {
        return stage.isSea() ? stage.title : floor + ". " + stage.title;
    }

    public Room startRoom() {
        if (Dev.flag("shop") && map.shop != null) { // dev builds: -Dfluffy.shop=true starts in the shop
            return map.shop;
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
        int level = stage.isSea() ? floor + 1 : floor; // the sea is a step harder than the clouds above it
        // the skyscraper gets harder storey by storey: a steeper climb that goes higher than other floors
        float roomStep = stage.isTower() ? Math.min(1.5f, 1f + 0.07f * r.depth) : Math.min(1.35f, 1f + 0.05f * r.depth);
        world.setDifficulty(roomStep + 0.15f * (level - 1));
        // floor 2: +25% health, floor 3: +50%; in the skyscraper +4% more each stop up
        world.setEnemyHealth(1f + 0.25f * (level - 1) + (stage.isTower() ? 0.04f * r.depth : 0f));
        switch (r.type) {
            case START:
                if (first) {
                    r.cleared = true;
                    showBanner(stageTitle(), 2.5f);
                }
                break;
            case TREASURE:
                if (first) {
                    r.cleared = true;
                    world.placeChest(chest(world, rollTier(world, 30, 35, 25, 10), Arena.now.centerX, Arena.now.centerY, true));
                    showBanner("TREASURE!", 1.5f);
                }
                break;
            case SHOP:
                if (first) { // Baa-llo sets out his wares: two from the chest pools, a heart, a key, a blank or ammo
                    r.cleared = true;
                    Shop shop = new Shop(floor, false);
                    // the first floor's shop always has a gun, of the lowest tier (a gun isn't guaranteed otherwise)
                    Tier firstTier = floor == 1 ? Tier.D : rollTier(world, 40, 35, 20, 5);
                    shop.stock(guns, items, new Tier[]{firstTier, rollTier(world, 20, 35, 30, 15)},
                            world.getPlayer()::hasItem, world.isShopGrudge(), floor == 1);
                    if (stage == Stage.CITY && !world.getPlayer().hasItem(Item.FLIPPERS)) { // only ever sold here
                        shop.addFlippers(world.isShopGrudge());
                    }
                    world.openShop(shop);
                    showBanner("BAA-LLO'S SHOP", 1.6f);
                }
                break;
            case COMBAT:
                // very rarely one of the room's street lamps is a mimic (dev builds: -Dfluffy.mimic=true)
                if (first && stage != Stage.VILLAGE && (Dev.flag("mimic") || MathUtils.random() < MIMIC_CHANCE)) {
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
        if (bossDead) {
            finished |= world.isExitTaken();
            return;
        }
        boolean fightRoom = room.type == Room.Type.COMBAT || room.type == Room.Type.BOSS;
        if (fightRoom && !room.cleared && !room.isLocked() && world.playerInsideRoom(0.45f)) {
            startFight(world);
        }
        if (room.isLocked()) {
            spawnCooldown -= delta;
            int active = world.getEnemies().size + world.getPendingEnemies().size;
            int maxActive = smallRoom() ? 2 + room.depth / 4 : stage.isTower() ? 5 + room.depth / 2 : 5 + room.depth / 3;
            if (queue.size > 0 && spawnCooldown <= 0 && active < maxActive) {
                spawn(world, queue.pop());
                spawnCooldown = 0.45f;
            } else if (queue.size == 0 && active == 0 && !world.bossDying()) {
                clearRoom(world);
            }
        }
    }

    private void startFight(GameWorld world) {
        room.setLocked(true);
        world.sfx().play("door", 1f);
        world.shake(0.25f);
        queue.clear();
        spawnCooldown = 0.6f;
        if (room.type == Room.Type.BOSS) {
            bossStarted = true;
            world.spawnEnemy(boss.create(Arena.now.centerX, Arena.now.top - 1.1f, 0));
            world.sfx().play("boss", 1f);
            return;
        }
        // a small trap room (pits, no space to run) gets about half the enemies, fewer at a time
        boolean small = smallRoom();
        int budget = small ? 2 + room.depth / 2 + MathUtils.random(0, 1)
                : stage.isTower() ? 4 + room.depth * 3 / 2 + MathUtils.random(0, 2) // a big crowd near the top
                : 3 + room.depth + MathUtils.random(0, 2);
        int hydrants = 0;
        int snipers = 0;
        int bikers = 0;
        int cubes = 0;
        int storms = 0;
        int divers = 0;
        boolean sludger = false;
        while (budget > 0) {
            if (stage.isSea()) { // only its own creatures down here
                int kind = SeaCreatures.pick(budget, divers);
                if (kind == SeaCreatures.DIVER) divers++;
                queue.add(SEA + kind);
                budget -= SeaCreatures.cost(kind);
                continue;
            }
            if (!sludger && budget >= 2 && room.depth >= 2 && (stage == Stage.TOWN || stage == Stage.CITY
                    || stage == Stage.SKYSCRAPER) && MathUtils.random() < SLUDGER_CHANCE) {
                sludger = true;
                queue.add(SLUDGER);
                budget -= 2;
                continue;
            }
            int type = stage == Stage.VILLAGE ? villageEnemy(budget)
                    : stage == Stage.TOWN ? townEnemy(budget, hydrants)
                    : stage == Stage.CITY ? cityEnemy(budget, snipers, bikers < (small ? 1 : 3))
                    : stage == Stage.SKYSCRAPER ? towerEnemy(budget, snipers, cubes)
                    : skyEnemy(budget, storms, cubes);
            if (type == HYDRANT) hydrants++;
            if (type == SNIPER) snipers++;
            if (type == BIKER) bikers++;
            if (type == MOUNTAIN_CUBE || type == LEAD_CUBE) cubes++;
            if (type == THUNDERCLOUD) storms++;
            queue.add(type);
            budget -= cost(type);
        }
        queue.shuffle();
    }

    /** A one-screen combat room: the town's and city's are full of pits. */
    private boolean smallRoom() {
        return room.type == Room.Type.COMBAT && room.arena.cols <= Arena.SMALL_COLS;
    }

    private int villageEnemy(int budget) {
        float r = MathUtils.random();
        if (room.depth >= 2 && budget >= 2 && r < 0.07f) return DYNAMITE;
        if (room.depth >= 2 && budget >= 3 && r < 0.25f) return MANCER;
        if (room.depth >= 1 && budget >= 2 && r < 0.55f) return SHOTGUN;
        return KIN;
    }

    /** The town mixes its own enemies (hydrants, rat packs, shell cops) with the village's. */
    private int townEnemy(int budget, int hydrants) {
        float r = MathUtils.random();
        if (budget >= 3 && r < 0.07f) return SHAPESHIFTER;
        if (budget >= 2 && r < 0.14f) return DYNAMITE;
        if (budget >= 2 && r < 0.3f) return COP;
        if (budget >= 2 && r < 0.44f) return RATS;
        if (budget >= 2 && hydrants < 2 && r < 0.56f) return HYDRANT;
        if (budget >= 2 && r < 0.68f) return SHOTGUN;
        if (budget >= 3 && r < 0.77f) return MANCER;
        return KIN;
    }

    /** The city's own enemies (bikers, snipers) plus some of the town's and the village's. */
    private int cityEnemy(int budget, int snipers, boolean bikerAllowed) {
        float r = MathUtils.random();
        if (budget >= 2 && r < 0.16f) return ROBBER;
        if (budget >= 2 && bikerAllowed && r < 0.32f) return BIKER;
        if (budget >= 2 && snipers < 2 && r < 0.44f) return SNIPER;
        if (budget >= 3 && r < 0.52f) return SHAPESHIFTER;
        if (budget >= 2 && r < 0.6f) return DYNAMITE;
        if (budget >= 2 && r < 0.68f) return COP;
        if (budget >= 2 && r < 0.75f) return RATS;
        if (budget >= 2 && r < 0.83f) return SHOTGUN;
        if (budget >= 3 && r < 0.88f) return MANCER;
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
        if (budget >= 2 && snipers < 2 && r < 0.86f) return SNIPER;
        if (budget >= 2 && r < 0.93f) return SHOTGUN;
        return KIN;
    }

    /** The clouds: angels with swords and bows, thunderclouds, and a few old faces who made it up here. */
    private int skyEnemy(int budget, int storms, int cubes) {
        float r = MathUtils.random();
        if (budget >= 2 && r < 0.24f) return SWORD_ANGEL;
        if (budget >= 2 && r < 0.46f) return BOW_ANGEL;
        if (budget >= 2 && storms < 2 && r < 0.58f) return THUNDERCLOUD;
        if (budget >= 3 && r < 0.66f) return SHAPESHIFTER;
        if (budget >= 2 && cubes < 1 && r < 0.71f) return MOUNTAIN_CUBE; // ice: right at home this high up
        if (budget >= 2 && r < 0.78f) return DYNAMITE;
        if (budget >= 2 && r < 0.84f) return ANDROID;
        if (budget >= 3 && r < 0.9f) return MANCER;
        if (budget >= 2 && r < 0.95f) return SHOTGUN;
        return KIN;
    }

    private static int cost(int type) {
        return type == MANCER || type == SHAPESHIFTER || type == LEAD_CUBE ? 3 : type == KIN ? 1 : 2;
    }

    private void spawn(GameWorld world, int type) {
        Vector2 p = world.findSpawnPoint(1.8f);
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
            case SHOTGUN:
                world.spawnEnemy(new ShotgunKin(p.x, p.y));
                break;
            case MANCER:
                world.spawnEnemy(new Bulletmancer(p.x, p.y));
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
            world.placeChest(chest(world, rollTier(world, 0, 40, 40, 20), spot.x, spot.y, false));
            world.openExit();
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
            needs.add(Pickup.Type.AMMO);
        }
        if (needs.size > 0 && MathUtils.random() < 0.4f) {
            Pickup.Type need = MathUtils.random() < 0.2f ? Pickup.Type.BLANK : needs.random();
            if (need == Pickup.Type.AMMO) world.takeAmmoDrop();
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
        if (floor > 1 && MathUtils.random() < 0.25f * (floor - 1)) {
            i = Math.min(i + 1, tiers.length - 1);
        }
        if (world.getPlayer().hasItem(Item.LUCKY_CLOVER) && MathUtils.random() < 0.3f) {
            i = Math.min(i + 1, tiers.length - 1);
        }
        return tiers[i];
    }

    /**
     * A chest with a gun or an item of that tier inside (half and half when both are left; taken out of
     * the pools), or loot if there's none left.
     */
    private Chest chest(GameWorld world, Tier tier, float x, float y, boolean locked) {
        int gun = -1;
        for (int i = 0; i < guns.size && gun < 0; i++) { // the pools are shuffled: a random one of that tier
            if (guns.get(i).getTier() == tier) gun = i;
        }
        int item = -1;
        for (int i = 0; i < items.size && item < 0; i++) {
            if (items.get(i).tier == tier && !world.getPlayer().hasItem(items.get(i))) item = i;
        }
        if (item >= 0 && (gun < 0 || MathUtils.randomBoolean())) {
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

    /** True once the player goes down the exit hatch the boss leaves behind. */
    @Override
    public boolean isFinished() {
        return finished;
    }
}
