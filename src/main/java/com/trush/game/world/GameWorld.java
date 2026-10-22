package com.trush.game.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.audio.Sfx;
import com.trush.game.effects.Particles;
import com.trush.game.enemy.Enemy;
import com.trush.game.gfx.RoomRenderer;
import com.trush.game.input.Gamepad;
import com.trush.game.launcher.Dev;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Bullet;
import com.trush.game.objects.Chest;
import com.trush.game.objects.Pickup;
import com.trush.game.objects.Prop;
import com.trush.game.objects.Tier;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Player;
import lombok.Getter;

import java.util.Arrays;

/**
 * Owns every entity in the current room and runs the simulation: movement and collisions
 * against walls/furniture, bullets, pickups, effects and moving between rooms.
 * A {@link Director} decides what spawns (story stage or endless waves).
 */
@Getter
public class GameWorld {

    private static final Color ENEMY_BULLET = Color.valueOf("ff5e3a");
    private static final Color SPARK = Color.valueOf("fff3b0");
    private static final Color BLOOD = Color.valueOf("e43b44");
    private static final Color BRASS = Color.valueOf("d9b24c");
    private static final Color SMOKE = Color.valueOf("b8b8b8");
    private static final Color BLANK_BLUE = Color.valueOf("a8d8ff");
    private static final Color WOOD = Color.valueOf("a0642f");
    private static final Color STONE = Color.valueOf("8d8a86");
    private static final Color DUST = Color.valueOf("c8b49a");

    private final ETGFanfic game;
    private final Director director;
    /** Null in endless mode. */
    private final StageMap map;
    private Room room;
    private final Player player;
    private final Array<Enemy> enemies = new Array<>();
    private final Array<Enemy> pendingEnemies = new Array<>();
    private final Array<Bullet> playerBullets = new Array<>(false, 128);
    private final Array<Bullet> enemyBullets = new Array<>(false, 512);
    private final Particles particles = new Particles();
    private final RoomRenderer roomRenderer = new RoomRenderer();
    private final Vector2 aim = new Vector2();

    private float trauma;
    private float blankRing = -1f;
    private final Vector2 blankCenter = new Vector2();
    private int score;
    private int kills;
    private float difficulty = 1f;
    /** 1 -> 0 fade after walking through a door. */
    private float fade;

    private String toast;
    private float toastTimer;

    /** Where the boss room's exit hatch sits (centre); walking onto it and pressing interact ends the floor. */
    public static final float EXIT_X = Arena.CENTER_X;
    public static final float EXIT_Y = Arena.CENTER_Y + 0.75f;
    private static final float EXIT_W = 1.0f;
    private static final float EXIT_H = 0.75f;
    /** How close the feet must be to a chest to open it. */
    private static final float CHEST_REACH = 0.45f;
    /** Set when the player goes down the exit hatch. */
    private boolean exitTaken;
    /** What the interact button would do right now ("OPEN", "DESCEND"...), or null; shown above the target. */
    private String prompt;
    private final Vector2 promptPos = new Vector2();

    private final Rectangle tmpRect = new Rectangle();
    private final Vector2 tmpVec = new Vector2();
    private final Vector2 spawnPoint = new Vector2();
    private final Array<Object> drawList = new Array<>();

    /** Half-width of the line used for "can this enemy shoot the player" checks (about a bullet). */
    public static final float SIGHT_THICKNESS = 0.07f;
    // enemy navigation: a 0.25-unit grid over the floor, rebuilt lazily once per frame
    private static final float NAV_CELL = 0.25f;
    private static final int NAV_COLS = 26;
    private static final int NAV_ROWS = 16;
    private static final float NAV_CLEARANCE = 0.17f;
    private final boolean[] navFree = new boolean[NAV_COLS * NAV_ROWS];
    private final int[] navParent = new int[NAV_COLS * NAV_ROWS];
    private final int[] navQueue = new int[NAV_COLS * NAV_ROWS];
    private boolean navDirty = true;
    private final Rectangle navRect = new Rectangle();

    /** Story mode: a stage map of rooms. */
    public GameWorld(ETGFanfic game, StoryDirector story) {
        this(game, story, story.getMap(), story.startRoom());
    }

    /** Endless mode: one closed arena room with a random layout. */
    public GameWorld(ETGFanfic game, WaveDirector waves) {
        this(game, waves, null, arenaRoom());
    }

    private static Room arenaRoom() {
        Room r = new Room(0, 0, Room.Type.ARENA);
        String[] themes = {"house", "chapel", "yard", "cellar", "square"};
        r.theme = themes[MathUtils.random(themes.length - 1)];
        // dev shortcut: -Dfluffy.layout=N picks RoomTemplates.COMBAT[N] for the endless arena
        int layout = Dev.number("layout", -1);
        r.template = layout >= 0 && layout < RoomTemplates.COMBAT.length
                ? RoomTemplates.COMBAT[layout] : RoomTemplates.randomCombat();
        r.rebuildWalls();
        return r;
    }

    private GameWorld(ETGFanfic game, Director director, StageMap map, Room first) {
        this.game = game;
        this.director = director;
        this.map = map;
        this.player = new Player(game.sprites);
        enterRoom(first, -1);
    }

    public Sfx sfx() {
        return game.sfx;
    }

    public Gamepad pad() {
        return game.pad;
    }

    public Particles particles() {
        return particles;
    }

    public Array<Pickup> getPickups() {
        return room.pickups;
    }

    public void setDifficulty(float difficulty) {
        this.difficulty = difficulty;
    }

    /** Carries the score over from the previous floor. */
    public void restoreStats(int score, int kills) {
        this.score = score;
        this.kills = kills;
    }

    public void addScore(int amount) {
        score += amount;
    }

    public void shake(float amount) {
        trauma = Math.min(1f, trauma + amount);
    }

    public void toast(String text) {
        toast = text;
        toastTimer = 2.2f;
    }

    // ------------------------------------------------------------ rooms

    /** @param fromDir direction the player walked to get here (Room.N...), or -1 for the first room. */
    private void enterRoom(Room next, int fromDir) {
        room = next;
        room.build();
        room.rebuildWalls();
        enemies.clear();
        pendingEnemies.clear();
        playerBullets.clear();
        enemyBullets.clear();
        particles.clear();
        blankRing = -1f;
        navDirty = true;

        float fx;
        float fy;
        switch (fromDir) {
            case Room.N: // came in through the bottom door
                fx = Arena.CENTER_X;
                fy = Arena.BOTTOM + 0.12f;
                break;
            case Room.S:
                fx = Arena.CENTER_X;
                fy = Arena.TOP - 0.45f;
                break;
            case Room.E:
                fx = Arena.LEFT + 0.3f;
                fy = (Arena.DOOR_Y0 + Arena.DOOR_Y1) / 2f - 0.15f;
                break;
            case Room.W:
                fx = Arena.RIGHT - 0.3f;
                fy = (Arena.DOOR_Y0 + Arena.DOOR_Y1) / 2f - 0.15f;
                break;
            default:
                fx = Arena.CENTER_X;
                fy = Arena.BOTTOM + 0.5f;
                break;
        }
        player.setFeet(fx, fy);
        if (fromDir >= 0) {
            fade = 1f;
        }
        director.onRoomEntered(room, this);
    }

    private void checkRoomTransition() {
        if (map == null || player.isDead()) {
            return;
        }
        float x = player.feetX();
        float y = player.feetY() + 0.07f;
        int dir = -1;
        if (y > Arena.TOP + 0.3f) dir = Room.N;
        else if (y < Arena.BOTTOM - 0.05f) dir = Room.S;
        else if (x < 0.12f) dir = Room.W;
        else if (x > Arena.WORLD_W - 0.12f) dir = Room.E;
        if (dir >= 0 && room.doorOpen(dir)) {
            Room next = map.neighbor(room, dir);
            if (next != null) {
                enterRoom(next, dir);
            }
        }
    }

    /** True when the player's feet are at least {@code margin} away from every wall. */
    public boolean playerInsideRoom(float margin) {
        return Arena.contains(player.feetX(), player.feetY() + 0.07f, margin);
    }

    // ------------------------------------------------------------ collision

    /** First movement-blocking rectangle (wall or prop) overlapping {@code r}, or null. */
    public Rectangle findSolid(Rectangle r) {
        for (Rectangle w : room.walls) {
            if (w.overlaps(r)) return w;
        }
        for (Prop p : room.props) {
            if (p.alive && p.solid.overlaps(r)) return p.solid;
        }
        for (Chest c : room.chests) {
            if (c.solid.overlaps(r)) return c.solid;
        }
        return null;
    }

    private static boolean circleOverlaps(Rectangle r, float x, float y, float radius) {
        float cx = MathUtils.clamp(x, r.x, r.x + r.width);
        float cy = MathUtils.clamp(y, r.y, r.y + r.height);
        float dx = x - cx;
        float dy = y - cy;
        return dx * dx + dy * dy < radius * radius;
    }

    /** Pushes a circle out of walls and props (used for enemies). */
    public void pushOutOfSolids(Vector2 p, float radius) {
        for (int pass = 0; pass < 2; pass++) {
            for (Prop prop : room.props) {
                if (prop.alive) pushOut(prop.solid, p, radius);
            }
            for (Chest c : room.chests) {
                pushOut(c.solid, p, radius);
            }
            for (Rectangle w : room.walls) {
                pushOut(w, p, radius);
            }
        }
    }

    private static void pushOut(Rectangle r, Vector2 p, float radius) {
        float cx = MathUtils.clamp(p.x, r.x, r.x + r.width);
        float cy = MathUtils.clamp(p.y, r.y, r.y + r.height);
        float dx = p.x - cx;
        float dy = p.y - cy;
        float d2 = dx * dx + dy * dy;
        if (d2 >= radius * radius) {
            return;
        }
        if (d2 > 1e-6f) {
            float d = (float) Math.sqrt(d2);
            p.x += dx / d * (radius - d);
            p.y += dy / d * (radius - d);
        } else { // centre inside the rect: push out along the shortest axis
            float left = p.x - r.x;
            float right = r.x + r.width - p.x;
            float down = p.y - r.y;
            float up = r.y + r.height - p.y;
            float m = Math.min(Math.min(left, right), Math.min(down, up));
            if (m == left) p.x = r.x - radius;
            else if (m == right) p.x = r.x + r.width + radius;
            else if (m == down) p.y = r.y - radius;
            else p.y = r.y + r.height + radius;
        }
    }

    private boolean pointIsFree(float x, float y, float radius) {
        if (!Arena.contains(x, y, radius)) return false;
        for (Prop p : room.props) {
            if (p.alive && circleOverlaps(p.solid, x, y, radius)) return false;
        }
        for (Chest c : room.chests) {
            if (circleOverlaps(c.solid, x, y, radius)) return false;
        }
        return true;
    }

    /** A random free spot for an enemy, preferably at least {@code minPlayerDist} from the player. */
    public Vector2 findSpawnPoint(float minPlayerDist) {
        float bestScore = -1;
        spawnPoint.set(Arena.CENTER_X, Arena.TOP - 0.6f);
        for (int i = 0; i < 60; i++) {
            float x = MathUtils.random(Arena.LEFT + 0.4f, Arena.RIGHT - 0.4f);
            float y = MathUtils.random(Arena.BOTTOM + 0.4f, Arena.TOP - 0.4f);
            if (!pointIsFree(x, y, 0.3f)) continue;
            float d = Vector2.dst(x, y, player.centerX(), player.hitboxY());
            float nearest = 9;
            for (Enemy e : enemies) nearest = Math.min(nearest, e.pos.dst(x, y));
            for (Enemy e : pendingEnemies) nearest = Math.min(nearest, e.pos.dst(x, y));
            if (d > minPlayerDist && nearest > 0.6f) {
                return spawnPoint.set(x, y);
            }
            float s = Math.min(d, 3.5f) + Math.min(nearest, 1f);
            if (s > bestScore) {
                bestScore = s;
                spawnPoint.set(x, y);
            }
        }
        return spawnPoint;
    }

    // ------------------------------------------------------------ enemy sight and navigation

    private boolean segmentHits(Rectangle r, float x1, float y1, float x2, float y2, float thickness) {
        navRect.set(r.x - thickness, r.y - thickness, r.width + 2 * thickness, r.height + 2 * thickness);
        return Intersector.intersectSegmentRectangle(x1, y1, x2, y2, navRect);
    }

    /** True if a bullet could fly from (x1, y1) to (x2, y2): no wall or bullet-blocking prop in between. */
    public boolean hasLineOfSight(float x1, float y1, float x2, float y2, float thickness) {
        for (Rectangle w : room.walls) {
            if (segmentHits(w, x1, y1, x2, y2, thickness)) return false;
        }
        for (Prop p : room.props) {
            if (p.alive && p.blocksBullets() && segmentHits(p.bulletRect(game.sprites), x1, y1, x2, y2, thickness)) return false;
        }
        return true;
    }

    /** True if something of the given radius could walk in a straight line from (x1, y1) to (x2, y2). */
    private boolean walkable(float x1, float y1, float x2, float y2, float radius) {
        for (Rectangle w : room.walls) {
            if (segmentHits(w, x1, y1, x2, y2, radius)) return false;
        }
        for (Prop p : room.props) {
            if (p.alive && segmentHits(p.solid, x1, y1, x2, y2, radius)) return false;
        }
        for (Chest c : room.chests) {
            if (segmentHits(c.solid, x1, y1, x2, y2, radius)) return false;
        }
        return true;
    }

    /**
     * For big enemies that don't fit the nav grid (the boss): the nearest of a few random free spots that
     * can be walked to in a straight line and has a clear shot at the player, at least {@code minDist}
     * away from them. Returns false if none of the candidates works.
     */
    public boolean findOpenShot(Vector2 from, float radius, float minDist, Vector2 out) {
        float px = player.centerX();
        float py = player.hitboxY();
        float best = Float.MAX_VALUE;
        for (int i = 0; i < 48; i++) {
            float x = MathUtils.random(Arena.LEFT + radius, Arena.RIGHT - radius);
            float y = MathUtils.random(Arena.BOTTOM + radius, Arena.TOP - radius);
            float d = from.dst2(x, y);
            if (d >= best || Vector2.dst(x, y, px, py) < minDist || !pointIsFree(x, y, radius)) continue;
            if (hasLineOfSight(x, y, px, py, SIGHT_THICKNESS) && walkable(from.x, from.y, x, y, radius * 0.8f)) {
                best = d;
                out.set(x, y);
            }
        }
        return best < Float.MAX_VALUE;
    }

    private void rebuildNav() {
        if (!navDirty) {
            return;
        }
        navDirty = false;
        for (int c = 0; c < navFree.length; c++) {
            navFree[c] = pointIsFree(navX(c), navY(c), NAV_CLEARANCE);
        }
    }

    private static float navX(int cell) {
        return Arena.LEFT + (cell % NAV_COLS + 0.5f) * NAV_CELL;
    }

    private static float navY(int cell) {
        return Arena.BOTTOM + (cell / NAV_COLS + 0.5f) * NAV_CELL;
    }

    private static int navCell(float x, float y) {
        int col = MathUtils.clamp((int) ((x - Arena.LEFT) / NAV_CELL), 0, NAV_COLS - 1);
        int row = MathUtils.clamp((int) ((y - Arena.BOTTOM) / NAV_CELL), 0, NAV_ROWS - 1);
        return row * NAV_COLS + col;
    }

    /**
     * For an enemy behind cover: breadth-first search over free floor from {@code from} to the nearest
     * spot that has a clear shot at the player and is at least {@code minDist} away from them.
     * Writes the next point to walk to (the furthest point along the path reachable in a straight line)
     * into {@code out}. Returns false if no such spot is reachable.
     */
    public boolean findSightPoint(Vector2 from, float radius, float minDist, Vector2 out) {
        rebuildNav();
        int start = navCell(from.x, from.y);
        Arrays.fill(navParent, -2); // -2 = not visited yet
        navParent[start] = -1;
        int head = 0;
        int tail = 0;
        navQueue[tail++] = start;
        float px = player.centerX();
        float py = player.hitboxY();
        int goal = -1;
        while (head < tail) {
            int c = navQueue[head++];
            float cx = navX(c);
            float cy = navY(c);
            if (c != start && Vector2.dst(cx, cy, px, py) >= minDist
                    && hasLineOfSight(cx, cy, px, py, SIGHT_THICKNESS)) {
                goal = c;
                break;
            }
            int col = c % NAV_COLS;
            int row = c / NAV_COLS;
            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int nc = col + dx;
                    int nr = row + dy;
                    if ((dx == 0 && dy == 0) || nc < 0 || nr < 0 || nc >= NAV_COLS || nr >= NAV_ROWS) continue;
                    int n = nr * NAV_COLS + nc;
                    if (!navFree[n] || navParent[n] != -2) continue;
                    // no cutting corners diagonally
                    if (dx != 0 && dy != 0 && (!navFree[row * NAV_COLS + nc] || !navFree[nr * NAV_COLS + col])) continue;
                    navParent[n] = c;
                    navQueue[tail++] = n;
                }
            }
        }
        if (goal < 0) {
            return false;
        }
        // walk back from the goal; the first cell we can reach directly is the furthest shortcut
        int step = goal;
        for (int c = goal; c != start; c = navParent[c]) {
            step = c;
            if (walkable(from.x, from.y, navX(c), navY(c), radius * 0.8f)) {
                break;
            }
        }
        out.set(navX(step), navY(step));
        return true;
    }

    // ------------------------------------------------------------ spawning

    public void addPlayerBullet(Bullet b) {
        playerBullets.add(b);
    }

    public Bullet spawnEnemyBullet(float x, float y, float angle, float speed) {
        Bullet b = new Bullet(false, x, y, angle, speed, 0.065f, 1f, ENEMY_BULLET);
        b.life = 10f;
        enemyBullets.add(b);
        return b;
    }

    public void spawnEnemy(Enemy e) {
        Arena.clamp(e.pos, e.radius);
        pushOutOfSolids(e.pos, e.radius);
        pendingEnemies.add(e);
        sfx().play("spawn", 0.5f);
    }

    /** True if a pickup at (x, y) would be in the open: not under any furniture's sprite. */
    private boolean pickupSpotFree(float x, float y) {
        if (!Arena.contains(x, y, 0.2f)) return false;
        for (Prop p : room.props) {
            if (p.alive && circleOverlaps(p.spriteRect(game.sprites, tmpRect), x, y, 0.2f)) return false;
        }
        for (Chest c : room.chests) {
            if (circleOverlaps(c.spriteRect(game.sprites, tmpRect), x, y, 0.2f)) return false;
        }
        return true;
    }

    /** Drops a pickup, nudged to the nearest spot where furniture doesn't hide it. */
    public void spawnPickup(Pickup p) {
        Arena.clamp(p.pos, 0.25f);
        if (!pickupSpotFree(p.pos.x, p.pos.y)) {
            outer:
            for (float r = 0.25f; r < 3f; r += 0.25f) {
                for (int a = 0; a < 360; a += 30) {
                    float x = p.pos.x + MathUtils.cosDeg(a) * r;
                    float y = p.pos.y + MathUtils.sinDeg(a) * r;
                    if (pickupSpotFree(x, y)) {
                        p.pos.set(x, y);
                        break outer;
                    }
                }
            }
        }
        room.pickups.add(p);
    }

    // ------------------------------------------------------------ update

    public void update(float delta) {
        navDirty = true; // props get flipped and destroyed
        fade = Math.max(0f, fade - delta * 4f);
        player.update(delta, this);
        checkRoomTransition();
        director.update(delta, this);

        for (Enemy e : enemies) {
            e.update(delta, this);
        }
        enemies.addAll(pendingEnemies);
        pendingEnemies.clear();
        separateEnemies();
        for (Enemy e : enemies) {
            if (!e.isSpawning()) {
                pushOutOfSolids(e.pos, e.radius * 0.8f);
            }
        }

        for (Prop p : room.props) {
            p.update(delta);
        }
        for (Chest c : room.chests) {
            c.update(delta);
        }
        updatePrompt();

        updatePlayerBullets(delta);
        updateEnemyBullets(delta);
        checkContactDamage();

        for (int i = enemies.size - 1; i >= 0; i--) {
            if (!enemies.get(i).alive) {
                enemies.removeIndex(i);
            }
        }
        for (int i = room.props.size - 1; i >= 0; i--) {
            if (!room.props.get(i).alive) {
                room.props.removeIndex(i);
            }
        }

        Array<Pickup> pickups = room.pickups;
        for (int i = pickups.size - 1; i >= 0; i--) {
            Pickup p = pickups.get(i);
            p.update(delta);
            if (!player.isDead() && Vector2.dst(p.pos.x, p.pos.y, player.centerX(), player.hitboxY()) < 0.32f) {
                collect(p);
            }
            if (!p.alive) {
                pickups.removeIndex(i);
            }
        }

        particles.update(delta);
        trauma = Math.max(0f, trauma - delta * 1.8f);
        if (blankRing >= 0) {
            blankRing += delta;
            if (blankRing > 0.5f) {
                blankRing = -1f;
            }
        }
        toastTimer -= delta;
    }

    private void separateEnemies() {
        for (int i = 0; i < enemies.size; i++) {
            Enemy a = enemies.get(i);
            if (a.isSpawning()) continue;
            for (int j = i + 1; j < enemies.size; j++) {
                Enemy b = enemies.get(j);
                if (b.isSpawning()) continue;
                float dx = b.pos.x - a.pos.x;
                float dy = b.pos.y - a.pos.y;
                float min = a.radius + b.radius;
                float d2 = dx * dx + dy * dy;
                if (d2 < min * min && d2 > 0.0001f) {
                    float d = (float) Math.sqrt(d2);
                    float push = (min - d) / 2f;
                    float wa = b.boss ? 2f : (a.boss ? 0f : 1f);
                    float wb = a.boss ? 2f : (b.boss ? 0f : 1f);
                    a.pos.sub(dx / d * push * wa, dy / d * push * wa);
                    b.pos.add(dx / d * push * wb, dy / d * push * wb);
                }
            }
        }
    }

    /** Returns true if the bullet hit a wall or a bullet-blocking prop (and damages the prop). */
    private boolean bulletHitsScenery(Bullet b) {
        if (b.pos.x < -0.2f || b.pos.x > Arena.WORLD_W + 0.2f || b.pos.y < -0.2f || b.pos.y > Arena.WORLD_H + 0.2f) {
            return true;
        }
        for (Rectangle w : room.walls) {
            if (w.contains(b.pos)) {
                return true;
            }
        }
        for (Prop p : room.props) {
            if (p.alive && p.blocksBullets() && circleOverlaps(p.bulletRect(game.sprites), b.pos.x, b.pos.y, b.radius * 0.7f)) {
                float dmg = b.fromPlayer ? b.damage * 0.3f : 1f;
                if (p.damage(dmg)) {
                    breakProp(p);
                } else {
                    particles.burst(b.pos.x, b.pos.y, 3, 0.4f, 1.2f, 0.25f, 0.03f,
                            p.kind == Prop.Kind.PILLAR ? STONE : WOOD);
                }
                return true;
            }
        }
        return false;
    }

    private void breakProp(Prop p) {
        float x = p.solid.x + p.solid.width / 2f;
        float y = p.solid.y + p.solid.height / 2f;
        particles.burst(x, y, 18, 0.6f, 2.2f, 0.6f, 0.05f, p.kind == Prop.Kind.HAY ? BRASS : WOOD);
        particles.burst(x, y, 8, 0.2f, 0.8f, 0.6f, 0.07f, DUST);
        sfx().play("break", 0.9f);
        shake(0.15f);
    }

    private void updatePlayerBullets(float delta) {
        for (int i = playerBullets.size - 1; i >= 0; i--) {
            Bullet b = playerBullets.get(i);
            b.update(delta);
            if (b.alive && bulletHitsScenery(b)) {
                b.alive = false;
                particles.burst(b.pos.x, b.pos.y, 3, 0.3f, 1f, 0.2f, 0.03f, SMOKE);
            }
            if (b.alive) {
                for (Enemy e : enemies) {
                    if (!e.alive || e.isSpawning() || b.alreadyHit.contains(e, true)) continue;
                    float r = e.radius + b.radius;
                    if (b.pos.dst2(e.pos) < r * r) {
                        e.hit(b.damage, b.vel, b.knockback, this);
                        particles.burst(b.pos.x, b.pos.y, 4, 0.8f, 2f, 0.18f, 0.03f, SPARK);
                        sfx().play("hit", 0.6f);
                        if (b.piercing) {
                            b.alreadyHit.add(e);
                        } else {
                            b.alive = false;
                            break;
                        }
                    }
                }
            }
            if (!b.alive) {
                playerBullets.removeIndex(i);
            }
        }
    }

    private void updateEnemyBullets(float delta) {
        float px = player.centerX();
        float py = player.hitboxY();
        for (int i = enemyBullets.size - 1; i >= 0; i--) {
            Bullet b = enemyBullets.get(i);
            if (!b.alive) {
                continue; // cleared by a blank / hit earlier this frame, swept below
            }
            b.update(delta);
            if (bulletHitsScenery(b)) {
                b.alive = false;
                particles.burst(b.pos.x, b.pos.y, 2, 0.2f, 0.6f, 0.2f, 0.03f, b.color);
            } else if (!player.isDead() && !player.isInvulnerable()) {
                float r = b.radius * 0.8f + Player.HITBOX_RADIUS;
                if (Vector2.dst2(b.pos.x, b.pos.y, px, py) < r * r) {
                    b.alive = false;
                    damagePlayer();
                }
            }
        }
        removeDeadEnemyBullets();
    }

    private void removeDeadEnemyBullets() {
        for (int i = enemyBullets.size - 1; i >= 0; i--) {
            if (!enemyBullets.get(i).alive) {
                enemyBullets.removeIndex(i);
            }
        }
    }

    private void checkContactDamage() {
        if (player.isDead() || player.isInvulnerable()) {
            return;
        }
        for (Enemy e : enemies) {
            if (e.isSpawning() || !e.alive) continue;
            float r = e.radius * 0.8f + Player.HITBOX_RADIUS;
            if (Vector2.dst2(e.pos.x, e.pos.y, player.centerX(), player.hitboxY()) < r * r) {
                damagePlayer();
                return;
            }
        }
    }

    private void damagePlayer() {
        if (!player.hurt(this)) {
            return;
        }
        shake(0.7f);
        particles.burst(player.centerX(), player.hitboxY(), 14, 0.8f, 2.5f, 0.4f, 0.05f, BLOOD);
        if (player.isDead()) {
            sfx().play("death", 1f);
            clearEnemyBullets(0, 0, -1f);
        } else {
            sfx().play("hurt", 1f);
            // small free "blank" around the player so a hit doesn't chain into another one
            clearEnemyBullets(player.centerX(), player.hitboxY(), 1.1f);
        }
    }

    /** Marks enemy bullets within {@code radius} (negative = all) as dead. Safe to call mid-iteration. */
    private void clearEnemyBullets(float x, float y, float radius) {
        for (Bullet b : enemyBullets) {
            if (b.alive && (radius < 0 || b.pos.dst2(x, y) < radius * radius)) {
                particles.spawn(b.pos.x, b.pos.y, 0, 0.3f, 0.3f, b.radius * 1.4f, SMOKE, 2f);
                b.alive = false;
            }
        }
    }

    public void useBlank() {
        if (player.isDead() || blankRing >= 0 || !player.consumeBlank()) {
            return;
        }
        float x = player.centerX();
        float y = player.hitboxY();
        clearEnemyBullets(x, y, -1f);
        removeDeadEnemyBullets();
        for (Enemy e : enemies) {
            if (e.boss || e.isSpawning()) continue;
            Vector2 push = new Vector2(e.pos).sub(x, y).nor().scl(2.5f);
            e.hit(0, push, 1.2f, this);
        }
        blankRing = 0f;
        blankCenter.set(x, y);
        shake(0.5f);
        sfx().play("blank", 1f);
    }

    // ------------------------------------------------------------ interacting: chests, exit, tables

    /** Closest unopened chest within reach of the player's feet, or null. */
    private Chest chestInReach() {
        float fx = player.feetX();
        float fy = player.feetY() + 0.07f;
        Chest best = null;
        float bestDist = CHEST_REACH;
        for (Chest c : room.chests) {
            if (c.opened) continue;
            float cx = MathUtils.clamp(fx, c.solid.x, c.solid.x + c.solid.width);
            float cy = MathUtils.clamp(fy, c.solid.y, c.solid.y + c.solid.height);
            float d = Vector2.dst(fx, fy, cx, cy);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        return best;
    }

    private boolean onExit() {
        return room.exit && Math.abs(player.feetX() - EXIT_X) < EXIT_W / 2f + 0.1f
                && Math.abs(player.feetY() + 0.07f - EXIT_Y) < EXIT_H / 2f + 0.1f;
    }

    private void updatePrompt() {
        prompt = null;
        if (player.isDead()) {
            return;
        }
        Chest c = chestInReach();
        if (c != null) {
            if (!c.locked) prompt = "OPEN";
            else prompt = player.getKeys() > 0 ? "OPEN  (-1 KEY)" : "LOCKED - NEEDS A KEY";
            Rectangle r = c.spriteRect(game.sprites, tmpRect);
            promptPos.set(c.cx, r.y + r.height + 0.22f);
        } else if (onExit()) {
            prompt = "GO DOWN";
            promptPos.set(EXIT_X, EXIT_Y + EXIT_H / 2f + 0.3f);
        }
    }

    /** E key / A button: open a chest, go down the exit, or flip a table - whichever is in reach. */
    public void interact() {
        Chest c = chestInReach();
        if (c != null) {
            openChest(c);
        } else if (onExit()) {
            exitTaken = true;
            sfx().play("door", 1f);
        } else {
            tryFlipTable();
        }
    }

    private void openChest(Chest c) {
        c.bump();
        if (c.locked && !player.consumeKey()) {
            sfx().play("empty", 1f);
            toast("YOU NEED A KEY");
            return;
        }
        c.opened = true;
        sfx().play("chest_open", 1f);
        shake(0.12f);
        particles.burst(c.cx, c.cy + 0.15f, 14, 0.5f, 1.6f, 0.5f, 0.04f, Color.GOLD);
        // loot pops out in front of the chest
        float y = c.solid.y - 0.35f;
        if (c.gun != null) {
            spawnPickup(new Pickup(Pickup.Type.GUN, c.cx, y, c.gun));
            return;
        }
        int n = c.tier == Tier.D ? MathUtils.random(1, 2) : c.tier == Tier.A ? 3 : 2;
        for (int i = 0; i < n; i++) {
            spawnPickup(new Pickup(randomLoot(), c.cx + (i - (n - 1) / 2f) * 0.38f, y));
        }
    }

    /** A consumable for chests without a gun: something useful right now if possible. */
    private Pickup.Type randomLoot() {
        Array<Pickup.Type> options = new Array<>();
        options.add(Pickup.Type.BLANK);
        if (player.getHealth() < player.getMaxHealth()) {
            options.add(Pickup.Type.HEART);
            options.add(Pickup.Type.HEART);
        }
        if (hasLimitedGunNeedingAmmo()) {
            options.add(Pickup.Type.AMMO);
            options.add(Pickup.Type.AMMO);
        }
        return MathUtils.random() < 0.05f ? Pickup.Type.KEY : options.random();
    }

    /** A free spot for a chest: away from doors, furniture and the player. */
    public Vector2 findChestSpot() {
        for (int i = 0; i < 80; i++) {
            float x = MathUtils.random(Arena.LEFT + 0.6f, Arena.RIGHT - 0.6f);
            float y = MathUtils.random(Arena.BOTTOM + 0.6f, Arena.TOP - 0.8f);
            navRect.set(x - 0.45f, y - 0.3f, 0.9f, 0.6f);
            if (pointIsFree(x, y, 0.5f) && !Room.blocksADoor(navRect)
                    && Vector2.dst(x, y, player.feetX(), player.feetY()) > 0.9f) {
                return spawnPoint.set(x, y);
            }
        }
        return spawnPoint.set(Arena.CENTER_X, Arena.CENTER_Y);
    }

    /** Puts a chest into the current room, stepping the player out of the way if it lands on them. */
    public void placeChest(Chest c) {
        room.chests.add(c);
        if (c.solid.overlaps(player.feetRect(tmpRect))) {
            player.setFeet(player.feetX(), c.solid.y - 0.16f);
        }
        particles.burst(c.cx, c.cy, 12, 0.3f, 1.2f, 0.5f, 0.05f, DUST);
    }

    /** Opens the hatch to the next floor (boss room, after the boss). */
    public void openExit() {
        room.exit = true;
        particles.burst(EXIT_X, EXIT_Y, 20, 0.3f, 1.2f, 0.6f, 0.06f, DUST);
        sfx().play("door", 1f);
    }

    /** E key / A button: flips the closest standing table next to the player, away from the player. */
    public boolean tryFlipTable() {
        float fx = player.feetX();
        float fy = player.feetY() + 0.08f;
        Prop best = null;
        float bestDist = 0.5f;
        for (Prop p : room.props) {
            if (!p.canFlip()) continue;
            float cx = MathUtils.clamp(fx, p.solid.x, p.solid.x + p.solid.width);
            float cy = MathUtils.clamp(fy, p.solid.y, p.solid.y + p.solid.height);
            float d = Vector2.dst(fx, fy, cx, cy);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        if (best == null) {
            return false;
        }
        float dx = best.cx - fx;
        float dy = best.cy - fy;
        int dir;
        if (Math.abs(dx) > Math.abs(dy) * 1.6f) {
            dir = dx > 0 ? Room.E : Room.W;
        } else {
            dir = dy > 0 ? Room.N : Room.S;
        }
        best.flip(dir);
        // don't leave the player stuck inside the new footprint
        if (best.solid.overlaps(player.feetRect(tmpRect))) {
            Vector2 away = tmpVec.set(Room.dx(dir), Room.dy(dir)).scl(-0.25f);
            player.nudge(away.x, away.y, this);
        }
        particles.burst(best.cx, best.cy, 10, 0.4f, 1.4f, 0.4f, 0.05f, DUST);
        sfx().play("flip", 1f);
        shake(0.15f);
        return true;
    }

    public void onEnemyKilled(Enemy e) {
        kills++;
        score += e.score;
        int n = e.boss ? 70 : 16;
        particles.burst(e.pos.x, e.pos.y, n, 0.5f, e.boss ? 4f : 2.5f, 0.6f, e.boss ? 0.09f : 0.05f, BRASS);
        particles.burst(e.pos.x, e.pos.y, n / 2, 0.3f, 1.2f, 0.7f, 0.07f, SMOKE);
        sfx().play("enemy_die", e.boss ? 1f : 0.7f);
        shake(e.boss ? 1f : 0.15f);

        if (!e.boss) {
            float roll = MathUtils.random();
            // enemies rarely drop anything, and only what you need (keys come from rooms and chests)
            if (roll < 0.03f && player.getHealth() < player.getMaxHealth()) {
                spawnPickup(new Pickup(Pickup.Type.HALF_HEART, e.pos.x, e.pos.y));
            } else if (roll < 0.09f && hasLimitedGunNeedingAmmo()) {
                spawnPickup(new Pickup(Pickup.Type.AMMO, e.pos.x, e.pos.y));
            }
        }
    }

    public boolean hasLimitedGunNeedingAmmo() {
        for (Weapon w : player.getWeapons()) {
            if (!w.isFullAmmo()) return true;
        }
        return false;
    }

    private void collect(Pickup p) {
        switch (p.type) {
            case HEART:
                if (!player.heal(2)) {
                    return; // leave it on the floor for later
                }
                toast("+1 HEART");
                break;
            case HALF_HEART:
                if (!player.heal(1)) {
                    return;
                }
                toast("+1/2 HEART");
                break;
            case AMMO:
                if (!hasLimitedGunNeedingAmmo()) {
                    return;
                }
                for (Weapon w : player.getWeapons()) {
                    w.addAmmo(0.6f);
                }
                toast("AMMO REFILLED");
                break;
            case BLANK:
                player.addBlank();
                toast("+1 BLANK");
                break;
            case KEY:
                player.addKey();
                toast("+1 KEY");
                break;
            case GUN:
                player.addWeapon(p.weapon);
                toast("GOT " + p.weapon.getName().toUpperCase() + "!");
                break;
        }
        p.alive = false;
        sfx().play("pickup", 1f);
        particles.burst(p.pos.x, p.pos.y, 10, 0.5f, 1.5f, 0.4f, 0.04f, Color.GOLD);
    }

    // ------------------------------------------------------------ draw

    private static float sortY(Object o) {
        if (o instanceof Enemy) return ((Enemy) o).feetY();
        if (o instanceof Prop) return ((Prop) o).sortY();
        if (o instanceof Chest) return ((Chest) o).sortY();
        return ((Player) o).feetY();
    }

    public void draw(SpriteBatch batch) {
        roomRenderer.drawFloor(batch, room);
        roomRenderer.drawBars(batch, room, game.draw);
        if (room.exit) {
            batch.draw(game.sprites.prop("exit"), EXIT_X - EXIT_W / 2f, EXIT_Y - EXIT_H / 2f, EXIT_W, EXIT_H);
        }

        for (Pickup p : room.pickups) {
            p.draw(batch, game);
        }

        // everything that stands on the floor is drawn back-to-front
        drawList.clear();
        drawList.addAll(room.props);
        drawList.addAll(room.chests);
        drawList.addAll(enemies);
        drawList.add(player);
        drawList.sort((a, b) -> Float.compare(sortY(b), sortY(a)));
        for (Object o : drawList) {
            if (o instanceof Prop) ((Prop) o).draw(batch, game.sprites);
            else if (o instanceof Chest) ((Chest) o).draw(batch, game.sprites);
            else if (o instanceof Enemy) ((Enemy) o).draw(batch, game.draw);
            else player.draw(batch, game.draw);
        }

        for (Bullet b : playerBullets) {
            b.draw(batch, game.draw);
        }
        for (Bullet b : enemyBullets) {
            b.draw(batch, game.draw);
        }
        particles.draw(batch, game.draw);

        if (blankRing >= 0) {
            float k = blankRing / 0.5f;
            game.draw.ring(batch, blankCenter.x, blankCenter.y, 0.3f + k * 6f, BLANK_BLUE, 1f - k);
            game.draw.circle(batch, blankCenter.x, blankCenter.y, 0.3f + k * 6f, Color.WHITE, 0.25f * (1f - k));
        }
    }

    public void dispose() {
        roomRenderer.dispose();
    }
}
