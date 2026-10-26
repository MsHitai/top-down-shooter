package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Queue;

/**
 * A randomly generated stage: a tree of rooms on a grid, like an ETG floor.
 * Start room -> combat rooms (one or two of them treasure rooms) -> boss room at the far end.
 */
public class StageMap {

    /** Share of combat rooms that are small (one screen; in the town: full of pits). */
    private static final float SMALL_ROOM_CHANCE = 0.3f;

    public final Array<Room> rooms = new Array<>();
    private final IntMap<Room> grid = new IntMap<>();
    public final Room start;
    public Room boss;
    /** Baa-llo's shop: a dead end of its own (null only if the map had no room for one). */
    public Room shop;

    private static int key(int x, int y) {
        return (x + 64) * 1000 + (y + 64);
    }

    public Room at(int x, int y) {
        return grid.get(key(x, y));
    }

    public Room neighbor(Room r, int dir) {
        return at(r.gx + Room.dx(dir), r.gy + Room.dy(dir));
    }

    private Room add(int x, int y, Room.Type type) {
        Room r = new Room(x, y, type);
        rooms.add(r);
        grid.put(key(x, y), r);
        return r;
    }

    private int neighbourCount(int x, int y) {
        int n = 0;
        for (int d = 0; d < 4; d++) {
            if (at(x + Room.dx(d), y + Room.dy(d)) != null) n++;
        }
        return n;
    }

    public StageMap(int roomCount, Stage stage) {
        start = add(0, 0, Room.Type.START);
        if (stage.isTower()) {
            buildTower(roomCount - 2);
        } else {
            buildTree(roomCount, stage);
        }
        theme(stage);
        if (stage == Stage.CITY) { // the secret way down to the Slums: a dead-end alley with no door in its top wall
            Room way = null;
            for (int pass = 0; pass < 3 && way == null; pass++) { // a roomy dead end; any roomy room; any room
                Array<Room> spots = new Array<>();
                for (Room r : rooms) {
                    if (r.type != Room.Type.COMBAT || r.doors[Room.N] || (pass < 2 && r.arena.isSmall())) continue;
                    if (pass == 0 && (r.depth < 1 || doorCount(r) != 1)) continue;
                    spots.add(r);
                }
                if (spots.size > 0) way = spots.random();
            }
            if (way != null) way.slumsWay = true;
        }
        if (stage.isSky()) { // the secret way down to the sea: a hole in one roomy combat room
            Array<Room> roomy = new Array<>();
            for (Room r : rooms) {
                if (r.type == Room.Type.COMBAT && r.depth >= 1 && !r.arena.isSmall()) roomy.add(r);
            }
            if (roomy.size == 0) {
                for (Room r : rooms) {
                    if (r.type == Room.Type.COMBAT) roomy.add(r);
                }
            }
            if (roomy.size > 0) roomy.random().seaHole = true;
        }
    }

    /**
     * The skyscraper: a straight climb. The lobby, then {@code stops} floors the elevator stops at
     * (one of them a treasure floor, one a shop), each 5-10 storeys above the last, and the roof (the boss).
     * Its fights are never the small rooms and grow with the climb ({@link #towerArena}).
     */
    private void buildTower(int stops) {
        start.floorNumber = 1;
        Room below = start;
        int treasureAt = MathUtils.random(2, Math.max(2, stops - 1));
        int shopAt = treasureAt;
        while (stops > 2 && (shopAt == treasureAt || shopAt == 1)) { // a gift shop on its own floor (not the first stop)
            shopAt = MathUtils.random(2, stops);
        }
        for (int i = 1; i <= stops + 1; i++) {
            Room.Type type = i == stops + 1 ? Room.Type.BOSS : i == treasureAt ? Room.Type.TREASURE
                    : i == shopAt ? Room.Type.SHOP : Room.Type.COMBAT;
            Room r = add(0, i, type);
            r.floorNumber = below.floorNumber + MathUtils.random(5, 10);
            link(below, Room.N);
            if (type == Room.Type.BOSS) {
                boss = r;
            } else if (type == Room.Type.SHOP) {
                shop = r;
            }
            below = r;
        }
        computeDepths();
    }

    private void buildTree(int roomCount, Stage stage) {
        // random tree growth: only add cells touching exactly one existing room, so the map stays a tree
        int guard = 0;
        while (rooms.size < roomCount && guard++ < 2000) {
            Room from = rooms.get(MathUtils.random(rooms.size - 1));
            int d = MathUtils.random(3);
            int nx = from.gx + Room.dx(d);
            int ny = from.gy + Room.dy(d);
            if (at(nx, ny) != null || neighbourCount(nx, ny) != 1) continue;
            if (from == start && neighbourCount(start.gx, start.gy) >= 2) continue; // keep the start calm
            add(nx, ny, Room.Type.COMBAT);
            link(from, d);
        }

        computeDepths();
        // boss room hangs off the deepest room - never entered from its top door, where the boss appears
        // (south of a room only as a last resort, if nothing else fits)
        Array<Room> byDepth = new Array<>(rooms);
        byDepth.sort((a, b) -> b.depth - a.depth);
        outer:
        for (int[] dirs : new int[][]{{Room.N, Room.E, Room.W}, {Room.S}}) {
            for (Room r : byDepth) {
                for (int d : dirs) {
                    int nx = r.gx + Room.dx(d);
                    int ny = r.gy + Room.dy(d);
                    if (at(nx, ny) == null && neighbourCount(nx, ny) == 1) {
                        boss = add(nx, ny, Room.Type.BOSS);
                        link(r, d);
                        break outer;
                    }
                }
            }
        }
        computeDepths();
        if (!stage.isSea()) { // the shop first, so the treasure rooms don't take every spot it could hang off (Baa-llo can't swim)
            addShop();
        }

        // treasure rooms: dead ends that aren't the start or next to the boss (a deep combat room if there
        // aren't enough of those), as many as the stage has (two on the first floors)
        for (int t = 0; t < stage.treasureRooms(); t++) {
            Array<Room> deadEnds = new Array<>();
            for (Room r : rooms) {
                if (r.type == Room.Type.COMBAT && doorCount(r) == 1 && neighbor(r, doorTo(r)) != null
                        && neighbor(r, doorTo(r)).type != Room.Type.BOSS) {
                    deadEnds.add(r);
                }
            }
            Room treasure = deadEnds.size > 0 ? deadEnds.random() : null;
            if (treasure == null) {
                for (Room r : rooms) {
                    if (r.type == Room.Type.COMBAT && r.depth >= 1 && neighborOf(r, Room.Type.BOSS) == null) {
                        treasure = r;
                    }
                }
            }
            if (treasure != null) {
                treasure.type = Room.Type.TREASURE;
            }
        }
    }

    /**
     * Baa-llo's shop: one more room hung off the map as a dead end - never next to the start or the boss,
     * and never entered from below (its counter stands against the top wall).
     */
    private void addShop() {
        Array<Room> from = new Array<>(rooms);
        from.shuffle(); // anywhere on the floor, like ETG's
        for (Room r : from) {
            if (r.type != Room.Type.COMBAT || r.depth < 1) continue;
            for (int d : new int[]{Room.E, Room.W, Room.N}) {
                int nx = r.gx + Room.dx(d);
                int ny = r.gy + Room.dy(d);
                if (at(nx, ny) != null || neighbourCount(nx, ny) != 1) continue;
                shop = add(nx, ny, Room.Type.SHOP);
                link(r, d);
                computeDepths();
                return;
            }
        }
    }

    private void theme(Stage stage) {
        for (Room r : rooms) {
            r.stage = stage;
            switch (r.type) {
                case START:
                    r.theme = stage.startTheme;
                    r.template = RoomTemplates.START;
                    break;
                case TREASURE:
                    r.theme = stage.treasureTheme;
                    r.template = RoomTemplates.TREASURE;
                    break;
                case SHOP: // one screen, like the treasure room
                    r.theme = stage.shopTheme;
                    r.template = RoomTemplates.SHOP;
                    break;
                case BOSS: // a big arena; StoryDirector picks open or with cover once it knows the boss
                    r.theme = stage.bossTheme;
                    r.template = RoomTemplates.BOSS_OPEN;
                    r.arena = new Arena(Arena.BOSS_COLS, Arena.BOSS_ROWS);
                    break;
                default: // mostly roomy combat rooms, sometimes a small tight one
                    r.theme = stage.combatThemes[MathUtils.random(stage.combatThemes.length - 1)];
                    r.template = RoomTemplates.randomCombat();
                    if (stage.isSlums()) {
                        r.arena = slumsArena();
                        if (r.arena.cols == ALLEY_LONG || r.arena.rows == ALLEY_TALL) { // (else a courtyard or trap room)
                            r.template = RoomTemplates.alley(r.arena.cols, r.arena.rows);
                        }
                        break;
                    }
                    if (stage.isTower()) {
                        r.arena = towerArena(r);
                        r.template = RoomTemplates.widen(r.template, r.arena.cols, r.arena.rows);
                        break;
                    }
                    r.arena = MathUtils.random() < SMALL_ROOM_CHANCE
                            ? new Arena(Arena.SMALL_COLS, Arena.SMALL_ROWS)
                            : new Arena(Arena.MEDIUM_COLS, Arena.MEDIUM_ROWS);
                    break;
            }
            r.rebuildWalls();
        }
    }

    /** Slums alleys, in cells: ALLEY_LONG x one screen tall going left-right, ALLEY_WIDE x ALLEY_TALL going up-down. */
    private static final int ALLEY_LONG = 29;
    private static final int ALLEY_WIDE = 11;
    private static final int ALLEY_TALL = 20;

    /**
     * A Slums fight: mostly long narrow alleys - left-right (one screen tall) or up-down - now and then a
     * wider courtyard, or a small trap room full of open hatches.
     */
    private static Arena slumsArena() {
        float roll = MathUtils.random();
        if (roll < 0.4f) return new Arena(ALLEY_LONG, Arena.SMALL_ROWS);
        if (roll < 0.7f) return new Arena(ALLEY_WIDE, ALLEY_TALL);
        if (roll < 0.9f) return new Arena(Arena.MEDIUM_COLS, Arena.MEDIUM_ROWS);
        return new Arena(Arena.SMALL_COLS, Arena.SMALL_ROWS);
    }

    /** A skyscraper fight: the higher up, the bigger the floor - a normal room low down, nearly a boss arena at the top. */
    private Arena towerArena(Room r) {
        float climb = boss == null || boss.depth <= 1 ? 0f : (r.depth - 1f) / (boss.depth - 2f);
        if (climb < 0.34f) return new Arena(Arena.MEDIUM_COLS, Arena.MEDIUM_ROWS);
        if (climb < 0.67f) return new Arena(21, 13);
        return new Arena(23, 14);
    }

    private void link(Room from, int dir) {
        Room to = neighbor(from, dir);
        from.doors[dir] = true;
        to.doors[Room.opposite(dir)] = true;
    }

    /** A neighbour of {@code r} (through a door) of this type, or null. */
    private Room neighborOf(Room r, Room.Type type) {
        for (int d = 0; d < 4; d++) {
            Room n = r.doors[d] ? neighbor(r, d) : null;
            if (n != null && n.type == type) return n;
        }
        return null;
    }

    private static int doorCount(Room r) {
        int n = 0;
        for (boolean d : r.doors) if (d) n++;
        return n;
    }

    private static int doorTo(Room r) {
        for (int d = 0; d < 4; d++) if (r.doors[d]) return d;
        return 0;
    }

    private void computeDepths() {
        for (Room r : rooms) r.depth = -1;
        Queue<Room> q = new Queue<>();
        start.depth = 0;
        q.addLast(start);
        while (q.size > 0) {
            Room r = q.removeFirst();
            for (int d = 0; d < 4; d++) {
                if (!r.doors[d]) continue;
                Room n = neighbor(r, d);
                if (n != null && n.depth < 0) {
                    n.depth = r.depth + 1;
                    q.addLast(n);
                }
            }
        }
    }

    /** The Treasure Map is being carried: every room of the floor shows on the map. */
    public boolean revealed;

    /** A room shows on the minimap once visited, or (dimmed) once a visited room has a door to it. */
    public boolean isKnown(Room r) {
        if (r.visited || revealed) return true;
        for (int d = 0; d < 4; d++) {
            if (r.doors[d]) {
                Room n = neighbor(r, d);
                if (n != null && n.visited) return true;
            }
        }
        return false;
    }
}
