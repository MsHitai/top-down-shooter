package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * A dream boss, drawn by Egor: the Nightmare - a big round creature floating over the arena on four curly
 * wings (one of them chequered dark), one huge eye with a cross for a pupil, one huge mouth full of zigzag
 * teeth with two star-flowers inside, and a crown with a sword stuck in each of its points. Everything it does
 * comes from the drawing:
 * <ul>
 *   <li>the eye: the cross in it turns, and each of its four arms pours out a stream - a turning cross of
 *       bullets (enraged: a second cross turning the other way);</li>
 *   <li>the crown: its swords fly up out of sight, then each comes down on a red ring where you stood and
 *       bursts into blades ({@link #SWORDS}, five enraged);</li>
 *   <li>the mouth: it opens wide and spits its star-flowers, which burst into rings of stars;</li>
 *   <li>the wings: gusts - every wing flaps out a fan;</li>
 *   <li>the chequered wing: it sheds a row of dark squares along the top of the arena that hang there, then
 *       drop - with a gap to stand in.</li>
 * </ul>
 * Below half health it's enraged: everything comes quicker and thicker.
 */
public class Nightmare extends Enemy {

    private static final Color BODY = Color.valueOf("4a2a6a");
    private static final Color BODY_DARK = Color.valueOf("32184e");
    private static final Color BODY_LIGHT = Color.valueOf("6a4a8e");
    private static final Color WING = Color.valueOf("d8cef0");
    private static final Color WING_EDGE = Color.valueOf("8a7aaa");
    private static final Color CHECKER = Color.valueOf("1e1428");
    private static final Color EYE = Color.valueOf("f5f0e6");
    private static final Color IRIS = Color.valueOf("c8324a");
    private static final Color MOUTH = Color.valueOf("1a0a14");
    private static final Color TOOTH = Color.valueOf("fbfaf0");
    private static final Color FLOWER = Color.valueOf("ff8ac8");
    private static final Color STAR = Color.valueOf("ffe48a");
    private static final Color GOLD = Color.valueOf("ffc83d");
    private static final Color GOLD_DARK = Color.valueOf("c8902a");
    private static final Color BLADE = Color.valueOf("dfe8f5");
    private static final Color CROSS_SHOT = Color.valueOf("ff6a8a");

    private enum Move { REST, EYE_CROSS, SWORDS, FLOWERS, GUST, CHECKERS }

    /** How long the turning cross pours. */
    static final float CROSS_TIME = 3.2f;
    /** Degrees a second the cross turns. */
    static final float CROSS_TURN = 38f;
    /** Swords in the crown (enraged: two more grow). */
    static final int SWORDS = 3;
    static final int SWORDS_ENRAGED = 5;
    /** A sword's red ring shows this long before it lands. */
    static final float SWORD_WARN = 0.9f;
    static final float SWORD_HIT_RADIUS = 0.45f;
    /** The star-flowers burst this long after they're spat. */
    static final float FLOWER_BURST = 1.1f;
    /** The dark squares hang this long before they drop. */
    static final float CHECKER_HANG = 0.85f;

    private Move move = Move.REST;
    private float moveTimer = 1.6f;
    private int nextMove;
    private float shotTimer;
    private float crossAngle;
    private float time;
    private float flap;
    private boolean enraged;
    private float mouthOpen;
    private int gusts;
    /** Swords out of the crown: where each lands and how long till it does (negative: not yet aimed / landed). */
    private final Array<Vector2> swordSpots = new Array<>();
    private final Array<Float> swordTimers = new Array<>();
    private int swordsOut;
    private final Array<Bullet> flowers = new Array<>();
    private float flowerClock;
    private final Array<Bullet> checkers = new Array<>();
    private float checkerClock;
    private final Vector2 home = new Vector2();
    private final Vector2 step = new Vector2();

    public Nightmare(float x, float y, int tier) {
        super(x, y, 1250f + 200f * tier);
        radius = 0.62f;
        speed = 0.7f;
        score = 2000 + 400 * tier;
        boss = true;
        flying = true;
        home.set(x, y - 0.6f);
    }

    @Override
    public String bossName() {
        return Boss.NIGHTMARE.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    @Override
    public float feetY() {
        return pos.y - 1.15f; // (it floats: its shadow is well below it)
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        flap += delta * (move == Move.GUST ? 9f : 3.5f);
        float diff = world.getDifficulty();
        if (!enraged && hp < maxHp * 0.5f) { // a roar: every bullet gone, its crown grows two more swords
            enraged = true;
            world.clearEnemyBullets(0, 0, -1f);
            flowers.clear();
            checkers.clear();
            world.shake(0.8f);
            world.sfx().play("boss", 1f);
            move = Move.REST;
            moveTimer = 1.2f;
        }
        drift(delta, world);
        updateSwords(delta, world, diff);
        updateFlowers(delta, world, diff);
        updateCheckers(delta, diff);
        mouthOpen += MathUtils.clamp((move == Move.FLOWERS ? 1f : 0f) - mouthOpen, -delta * 4f, delta * 4f);
        moveTimer -= delta;
        shotTimer -= delta;
        switch (move) {
            case REST:
                if (moveTimer <= 0) startNext(world);
                break;
            case EYE_CROSS:
                crossAngle += CROSS_TURN * (enraged ? 1.3f : 1f) * delta;
                if (shotTimer <= 0) {
                    shotTimer = (enraged ? 0.11f : 0.13f) / diff;
                    for (int i = 0; i < 4; i++) {
                        shootFrom(world, eyeX(), eyeY(), crossAngle + i * 90f, 2.4f * diff, CROSS_SHOT, Bullet.Shape.BALL);
                        if (enraged) {
                            shootFrom(world, eyeX(), eyeY(), -crossAngle * 1.2f + i * 90f + 45f, 2.1f * diff, IRIS, Bullet.Shape.BALL);
                        }
                    }
                    world.sfx().play("enemy_shoot", 0.25f);
                }
                if (moveTimer <= 0) rest();
                break;
            case GUST:
                if (shotTimer <= 0 && gusts > 0) {
                    gusts--;
                    shotTimer = 0.55f / diff;
                    gust(world, diff);
                }
                if (gusts == 0 && shotTimer <= 0) rest();
                break;
            default: // the swords, flowers and squares see themselves through
                if (moveTimer <= 0) rest();
                break;
        }
    }

    /** Floats about the top half of the arena, swaying over towards the player. */
    private void drift(float delta, GameWorld world) {
        Player p = world.getPlayer();
        float tx = MathUtils.clamp(p.centerX(), Arena.now.left + 2f, Arena.now.right - 2f);
        float ty = Arena.now.top - 2.2f + MathUtils.sin(time * 0.9f) * 0.25f;
        home.set(tx + MathUtils.sin(time * 0.5f) * 1.5f, ty);
        step.set(home).sub(pos);
        float max = speed * (enraged ? 1.3f : 1f) * delta;
        if (step.len() > max) step.setLength(max);
        pos.add(step);
    }

    private void rest() {
        move = Move.REST;
        moveTimer = enraged ? 0.6f : 0.95f;
    }

    private void startNext(GameWorld world) {
        Move[] order = {Move.EYE_CROSS, Move.SWORDS, Move.FLOWERS, Move.GUST, Move.CHECKERS};
        move = order[nextMove++ % order.length];
        shotTimer = 0f;
        switch (move) {
            case EYE_CROSS:
                moveTimer = CROSS_TIME;
                crossAngle = aimAngle + 45f; // (it starts between you and its arms)
                world.sfx().play("zap", 0.6f);
                break;
            case SWORDS:
                launchSwords(world);
                moveTimer = 0.6f + swordsOut * 0.35f + SWORD_WARN;
                break;
            case FLOWERS:
                moveTimer = 0.5f;
                flowerClock = 0.45f; // (the mouth opens first)
                break;
            case GUST:
                gusts = enraged ? 4 : 3;
                moveTimer = 0f;
                world.sfx().play("flap", 0.9f);
                break;
            case CHECKERS:
                dropCheckers(world);
                moveTimer = CHECKER_HANG + 0.4f;
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------ the crown's swords

    private void launchSwords(GameWorld world) {
        swordsOut = enraged ? SWORDS_ENRAGED : SWORDS;
        swordSpots.clear();
        swordTimers.clear();
        for (int i = 0; i < swordsOut; i++) {
            swordSpots.add(new Vector2());
            swordTimers.add(-(0.6f + i * 0.35f)); // up and out of sight first, one after another
        }
        world.sfx().play("swish", 1f);
    }

    private void updateSwords(float delta, GameWorld world, float diff) {
        Player p = world.getPlayer();
        for (int i = 0; i < swordTimers.size; i++) {
            float t = swordTimers.get(i);
            if (t < 0) { // still flying up: once it's gone it picks its spot - where you stand right now
                t += delta;
                if (t >= 0) {
                    Vector2 spot = swordSpots.get(i);
                    spot.set(p.centerX() + MathUtils.random(-0.3f, 0.3f), p.feetY() + MathUtils.random(-0.2f, 0.2f));
                    Arena.now.clamp(spot, 0.3f);
                    t = SWORD_WARN / Math.min(diff, 1.3f);
                }
                swordTimers.set(i, t);
                continue;
            }
            if (t == Float.MAX_VALUE) continue; // (landed)
            t -= delta;
            if (t <= 0) { // it lands: a burst of blades all round
                Vector2 spot = swordSpots.get(i);
                if (spot.dst(p.centerX(), p.feetY()) < SWORD_HIT_RADIUS) world.hurtPlayer();
                int n = enraged ? 12 : 10;
                float off = MathUtils.random(360f);
                for (int k = 0; k < n; k++) {
                    shootFrom(world, spot.x, spot.y + 0.1f, off + 360f * k / n, 2.0f * diff, BLADE, Bullet.Shape.DIAMOND);
                }
                world.shake(0.25f);
                world.sfx().play("break", 0.7f);
                t = Float.MAX_VALUE;
            }
            swordTimers.set(i, t);
        }
        boolean allDown = true;
        for (float t : swordTimers) allDown &= t == Float.MAX_VALUE;
        if (allDown) {
            swordTimers.clear();
            swordSpots.clear();
            swordsOut = 0; // they grow back in the crown
        }
    }

    // ------------------------------------------------------------ the mouth's star-flowers

    private void updateFlowers(float delta, GameWorld world, float diff) {
        if (move == Move.FLOWERS && flowerClock > 0) {
            flowerClock -= delta;
            if (flowerClock <= 0) { // spat out
                int n = enraged ? 4 : 2;
                for (int i = 0; i < n; i++) {
                    float a = aimAngle + (i - (n - 1) / 2f) * 28f;
                    Bullet f = shootFrom(world, pos.x, mouthY(), a, 1.6f * diff, FLOWER, Bullet.Shape.STAR);
                    f.radius = 0.16f;
                    f.life = FLOWER_BURST + 1f;
                    f.accel = -0.6f;
                    flowers.add(f);
                }
                flowerTimer = FLOWER_BURST;
                world.sfx().play("pop", 1f);
            }
        }
        if (flowers.size == 0) return;
        flowerTimer -= delta;
        if (flowerTimer > 0) return;
        for (Bullet f : flowers) { // each bursts into a ring of stars (those already gone don't)
            if (!f.alive) continue;
            f.alive = false;
            int n = enraged ? 14 : 11;
            float off = MathUtils.random(360f);
            for (int k = 0; k < n; k++) {
                shootFrom(world, f.pos.x, f.pos.y, off + 360f * k / n, 1.9f * diff, STAR, Bullet.Shape.STAR);
            }
        }
        flowers.clear();
        world.sfx().play("pop", 0.8f);
    }

    private float flowerTimer;

    // ------------------------------------------------------------ the wings

    /** A flap: each of the four wings throws a fan your way. */
    private void gust(GameWorld world, float diff) {
        int per = enraged ? 5 : 4;
        for (int w = 0; w < 4; w++) {
            float wx = pos.x + (w % 2 == 0 ? -1 : 1) * (w < 2 ? 1.0f : 0.85f);
            float wy = pos.y + (w < 2 ? 0.35f : -0.35f);
            float a = MathUtils.atan2Deg360(world.getPlayer().hitboxY() - wy, world.getPlayer().centerX() - wx);
            for (int i = 0; i < per; i++) {
                Bullet b = shootFrom(world, wx, wy, a + (i - (per - 1) / 2f) * 11f, 2.2f * diff, WING, Bullet.Shape.BALL);
                b.curve = (w % 2 == 0 ? -1 : 1) * 12f; // they sweep in from the sides
            }
        }
        world.sfx().play("flap", 0.7f);
    }

    /**
     * The chequered wing sheds a row of dark squares along the top of the arena: they hang, then drop
     * straight down - except for a gap about where you are, wide enough to stand in.
     */
    private void dropCheckers(GameWorld world) {
        float gapX = world.getPlayer().centerX() + MathUtils.random(-1.2f, 1.2f);
        float y = Arena.now.top - 0.5f;
        float spacing = enraged ? 0.42f : 0.5f;
        for (float x = Arena.now.left + 0.3f; x < Arena.now.right - 0.2f; x += spacing) {
            if (Math.abs(x - gapX) < 0.6f) continue;
            Bullet b = world.spawnEnemyBullet(x, y, 270f, 0.01f);
            b.vel.setZero();
            b.shape = Bullet.Shape.SQUARE;
            b.color.set(CHECKER);
            b.life = CHECKER_HANG + 8f;
            checkers.add(b);
        }
        checkerClock = CHECKER_HANG;
        world.sfx().play("spawn", 0.7f);
    }

    private void updateCheckers(float delta, float diff) {
        if (checkers.size == 0) return;
        checkerClock -= delta;
        for (Bullet b : checkers) b.vel.setZero(); // (hanging)
        if (checkerClock > 0) return;
        for (Bullet b : checkers) {
            if (b.alive) b.vel.set(0, -2.3f * diff);
        }
        checkers.clear();
    }

    private Bullet shootFrom(GameWorld world, float x, float y, float angle, float speed, Color color, Bullet.Shape shape) {
        Bullet b = world.spawnEnemyBullet(x, y, angle, speed);
        b.color.set(color);
        b.shape = shape;
        return b;
    }

    private float eyeX() {
        return pos.x;
    }

    private float eyeY() {
        return pos.y + 0.2f;
    }

    private float mouthY() {
        return pos.y - 0.32f;
    }

    // ------------------------------------------------------------ drawing

    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        for (int i = 0; i < swordTimers.size; i++) { // where the swords will come down: red rings closing in
            float t = swordTimers.get(i);
            if (t < 0 || t == Float.MAX_VALUE) continue;
            Vector2 s = swordSpots.get(i);
            float k = MathUtils.clamp(t / SWORD_WARN, 0f, 1f);
            d.ellipse(b, s.x, s.y, SWORD_HIT_RADIUS, SWORD_HIT_RADIUS * 0.5f, TELEGRAPH, 0.25f + 0.25f * (1f - k));
            d.ring(b, s.x, s.y, SWORD_HIT_RADIUS * (0.4f + k), TELEGRAPH, 0.8f);
        }
    }

    @Override
    public void drawOverlay(SpriteBatch b, Draw d, com.badlogic.gdx.math.Rectangle view) {
        for (int i = 0; i < swordTimers.size; i++) { // the swords falling, point first
            float t = swordTimers.get(i);
            if (t < 0 || t == Float.MAX_VALUE) continue;
            Vector2 s = swordSpots.get(i);
            float k = MathUtils.clamp(t / SWORD_WARN, 0f, 1f);
            drawSword(b, d, s.x, s.y + 0.2f + 4f * k * k, 270f, 1.4f);
        }
    }

    private void drawSword(SpriteBatch b, Draw d, float x, float y, float pointing, float s) {
        float c = MathUtils.cosDeg(pointing);
        float n = MathUtils.sinDeg(pointing);
        d.line(b, x - c * 0.32f * s, y - n * 0.32f * s, 0.34f * s, 0.07f * s, pointing, BLADE, 1f); // the blade
        d.shape(b, d.triangle, x + c * 0.04f * s, y + n * 0.04f * s, 0.05f * s, pointing - 90f, BLADE, 1f);
        d.rotRect(b, x - c * 0.32f * s, y - n * 0.32f * s, 0.18f * s, 0.04f * s, pointing + 90f, GOLD, 1f); // the guard
        d.line(b, x - c * 0.44f * s, y - n * 0.44f * s, 0.12f * s, 0.035f * s, pointing, GOLD_DARK, 1f); // the hilt
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float x = pos.x;
        float y = pos.y + MathUtils.sin(time * 1.7f) * 0.04f;
        float f = MathUtils.sin(flap);
        // the four curly wings: two big ones up top, two below, the lower left one chequered dark
        drawWing(b, d, x - 0.95f, y + 0.4f + f * 0.08f, -1, 0.75f, false);
        drawWing(b, d, x + 0.95f, y + 0.4f + f * 0.08f, 1, 0.75f, false);
        drawWing(b, d, x - 0.85f, y - 0.35f - f * 0.06f, -1, 0.6f, true);
        drawWing(b, d, x + 0.85f, y - 0.35f - f * 0.06f, 1, 0.6f, false);
        // the round body
        d.circle(b, x, y, 0.78f, Draw.OUTLINE_COLOR);
        d.circle(b, x, y, 0.74f, c(BODY));
        d.circle(b, x - 0.15f, y + 0.18f, 0.5f, c(BODY_LIGHT), 0.35f);
        d.circle(b, x + 0.2f, y - 0.25f, 0.45f, c(BODY_DARK), 0.4f);
        // the mouth, zigzag teeth top and bottom, two star-flowers inside
        float mh = 0.17f + 0.12f * mouthOpen;
        float my = y - 0.3f;
        d.ellipse(b, x, my, 0.5f, mh + 0.02f, Draw.OUTLINE_COLOR, 1f);
        d.ellipse(b, x, my, 0.47f, mh, MOUTH, 1f);
        if (move != Move.FLOWERS || flowerClock > 0) { // (until they're spat out)
            drawFlower(b, d, x - 0.13f, my - mh * 0.25f, 0.075f);
            drawFlower(b, d, x + 0.15f, my - mh * 0.2f, 0.09f);
        }
        for (int i = 0; i < 7; i++) {
            float tx = x - 0.39f + i * 0.13f;
            d.shape(b, d.triangle, tx, my + mh - 0.05f, 0.06f, 180f, TOOTH, 1f);
            d.shape(b, d.triangle, tx + 0.065f, my - mh + 0.05f, 0.06f, 0f, TOOTH, 1f);
        }
        // the one big eye, its pupil a cross that turns as it fires
        float ex = x;
        float ey = y + 0.2f;
        d.ellipse(b, ex, ey, 0.4f, 0.25f, Draw.OUTLINE_COLOR, 1f);
        d.ellipse(b, ex, ey, 0.37f, 0.22f, c(EYE), 1f);
        float lx = MathUtils.cosDeg(aimAngle) * 0.07f;
        float ly = MathUtils.sinDeg(aimAngle) * 0.04f;
        d.circle(b, ex + lx, ey + ly, 0.17f, IRIS, enraged ? 1f : 0.85f);
        d.ring(b, ex + lx, ey + ly, 0.12f, Draw.OUTLINE_COLOR, 0.5f);
        float cross = move == Move.EYE_CROSS ? crossAngle : 0f;
        d.rotRect(b, ex + lx, ey + ly, 0.05f, 0.34f, cross, Draw.OUTLINE_COLOR, 1f);
        d.rotRect(b, ex + lx, ey + ly, 0.05f, 0.34f, cross + 90f, Draw.OUTLINE_COLOR, 1f);
        // the crown with a sword in each point (none while they're out)
        float cy = y + 0.6f;
        d.rect(b, x - 0.32f, cy, 0.64f, 0.16f, GOLD);
        d.rect(b, x - 0.32f, cy, 0.64f, 0.04f, GOLD_DARK);
        int points = enraged ? SWORDS_ENRAGED : SWORDS;
        for (int i = 0; i < points; i++) {
            float px = x - 0.24f + 0.48f * i / (points - 1f);
            float tall = i == points / 2 ? 0.34f : 0.24f;
            d.shape(b, d.triangle, px, cy + 0.16f + tall * 0.4f, tall * 0.55f, 0f, GOLD, 1f);
            if (swordsOut == 0) {
                drawSword(b, d, px, cy + 0.38f + tall * 0.5f, 90f, 0.55f);
            }
            d.shape(b, d.triangle, px, cy + 0.2f, 0.035f, 180f, BODY_DARK, 1f); // the little holes Egor drew in them
        }
    }

    /** A curly wing: a soft lobe with a scalloped edge (the chequered one dark-squared). */
    private void drawWing(SpriteBatch b, Draw d, float wx, float wy, int side, float size, boolean chequered) {
        d.ellipse(b, wx, wy, size * 0.75f + 0.03f, size * 0.5f + 0.03f, Draw.OUTLINE_COLOR, 1f);
        d.ellipse(b, wx, wy, size * 0.75f, size * 0.5f, c(chequered ? WING_EDGE : WING), 1f);
        for (int i = 0; i < 4; i++) { // the curls along its lower edge
            float cx = wx + side * (size * 0.6f - i * size * 0.38f);
            d.circle(b, cx, wy - size * 0.45f, size * 0.16f, c(chequered ? WING_EDGE : WING));
            d.ring(b, cx, wy - size * 0.45f, size * 0.1f, WING_EDGE, 0.8f);
        }
        if (chequered) {
            for (int row = -1; row <= 1; row++) {
                for (int col = -2; col <= 2; col++) {
                    if ((row + col) % 2 != 0) continue;
                    d.rect(b, wx + col * size * 0.22f - 0.04f, wy + row * size * 0.2f - 0.04f, 0.08f, 0.08f, CHECKER);
                }
            }
        }
    }

    private void drawFlower(SpriteBatch b, Draw d, float x, float y, float r) {
        for (int i = 0; i < 5; i++) {
            float a = i * 72f + 90f;
            d.circle(b, x + MathUtils.cosDeg(a) * r * 0.7f, y + MathUtils.sinDeg(a) * r * 0.7f, r * 0.55f, FLOWER);
        }
        d.shape(b, d.star, x, y, r * 0.6f, 0f, STAR, 1f);
    }
}
