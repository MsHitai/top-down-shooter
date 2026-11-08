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
 * A dream boss: the Alarm Clock - a giant red twin-bell clock on little legs, the morning that won't let you
 * sleep. Its moves:
 * <ul>
 *   <li>TICK TOCK: its two hands sweep round and pour bullets along themselves - a two-armed spiral, the minute
 *       hand fast, the hour hand slow;</li>
 *   <li>RING: the hammer rattles between the bells and rings out rings of notes, each with a gap;</li>
 *   <li>SNOOZE: it nods off for a moment (Zs drift off it and float your way) - hit it while it dozes;</li>
 *   <li>TIME'S UP: three hops at you, a ring where it lands;</li>
 *   <li>enraged (below half, OVERTIME): the CLOCK FACE - twelve numbers appear in a circle round where you
 *       stand, one missing, and after a moment they all close in on the middle: get out through the gap.</li>
 * </ul>
 */
public class AlarmClock extends Enemy {

    private static final Color CASE = Color.valueOf("e04a3a");
    private static final Color CASE_DARK = Color.valueOf("a8302a");
    private static final Color CASE_LIGHT = Color.valueOf("ff7a62");
    private static final Color BELL = Color.valueOf("ffc83d");
    private static final Color BELL_DARK = Color.valueOf("c8902a");
    private static final Color FACE = Color.valueOf("f5f0e6");
    private static final Color INK = Color.valueOf("2a1e24");
    private static final Color LEG = Color.valueOf("3a3a44");
    private static final Color MINUTE = Color.valueOf("ff6a5a");
    private static final Color HOUR = Color.valueOf("ffd166");
    private static final Color NOTE = Color.valueOf("8ac8ff");
    private static final Color SNORE = Color.valueOf("c8a8ff");
    private static final Color NUMBER = Color.valueOf("fbfaf0");

    private enum Move { REST, TICK_TOCK, RING, SNOOZE, HOPS, CLOCK_FACE }

    static final float TICK_TIME = 3.4f;
    /** Degrees a second the minute hand sweeps (the hour hand a third of it, the other way round). */
    static final float MINUTE_TURN = 95f;
    /** Rings of notes it rings out, and the gap in each (degrees). */
    static final int RINGS = 3;
    static final float RING_GAP = 50f;
    static final float SNOOZE_TIME = 2.2f;
    /** Dozing, it takes this much more damage. */
    static final float SNOOZE_DAMAGE = 1.5f;
    static final int HOPS = 3;
    static final float HOP_TIME = 0.55f;
    /** The clock face: its radius round your spot, and how long the numbers hang before closing in. */
    static final float FACE_RADIUS = 2.1f;
    static final float FACE_HANG = 1.1f;

    private Move move = Move.REST;
    private float moveTimer = 1.4f;
    private int nextMove;
    private float shotTimer;
    private float minute;
    private float hour;
    private float time;
    private boolean enraged;
    private int ringsLeft;
    private int hopsLeft;
    private float hopClock;
    private final Vector2 hopFrom = new Vector2();
    private final Vector2 hopTo = new Vector2();
    private float hopHeight;
    private final Array<Bullet> numbers = new Array<>();
    private final Vector2 faceCenter = new Vector2();
    private float faceClock;
    private final Vector2 step = new Vector2();

    public AlarmClock(float x, float y, int tier) {
        super(x, y, 1150f + 200f * tier);
        radius = 0.55f;
        speed = 0.9f;
        score = 2000 + 400 * tier;
        boss = true;
        minute = 90f;
        hour = 90f;
    }

    @Override
    public String bossName() {
        return Boss.ALARM_CLOCK.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    @Override
    public float feetY() {
        return pos.y - 0.72f;
    }

    /** In the air on a hop: bullets pass under it. */
    @Override
    protected boolean isDodging() {
        return move == Move.HOPS && hopHeight > 0.25f;
    }

    @Override
    public void hit(float damage, Vector2 bulletVel, float knockback, GameWorld world) {
        super.hit(move == Move.SNOOZE ? damage * SNOOZE_DAMAGE : damage, bulletVel, 0f, world); // (too heavy to knock back)
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        float diff = world.getDifficulty();
        if (!enraged && hp < maxHp * 0.5f) { // OVERTIME: it rings like mad, and the clock face comes into it
            enraged = true;
            world.clearEnemyBullets(0, 0, -1f);
            world.shake(0.8f);
            world.toast("OVERTIME!");
            world.sfx().play("alarm", 1f);
            move = Move.REST;
            moveTimer = 1f;
        }
        updateFace(delta, world, diff);
        moveTimer -= delta;
        shotTimer -= delta;
        switch (move) {
            case REST:
                walk(delta, world);
                if (moveTimer <= 0) startNext(world);
                break;
            case TICK_TOCK:
                walk(delta * 0.4f, world);
                minute -= MINUTE_TURN * (enraged ? 1.25f : 1f) * delta; // (clockwise)
                hour += MINUTE_TURN / 3f * delta;
                if (shotTimer <= 0) {
                    shotTimer = (enraged ? 0.08f : 0.1f) / diff;
                    hand(world, minute, 2.6f * diff, MINUTE);
                    hand(world, hour, 2.0f * diff, HOUR);
                    if (enraged) { // the other end of each hand too
                        hand(world, minute + 180f, 2.6f * diff, MINUTE);
                    }
                }
                if (moveTimer <= 0) rest();
                break;
            case RING:
                if (shotTimer <= 0 && ringsLeft > 0) {
                    ringsLeft--;
                    shotTimer = 0.42f / diff;
                    ring(world, diff);
                }
                if (ringsLeft == 0 && shotTimer <= 0) rest();
                break;
            case SNOOZE:
                if (shotTimer <= 0) { // a Z drifts off it, your way
                    shotTimer = 0.5f / diff;
                    Bullet z = world.spawnEnemyBullet(pos.x + 0.3f, pos.y + 0.5f, aimAngle + MathUtils.random(-30f, 30f), 1.1f * diff);
                    z.color.set(SNORE);
                    z.shape = Bullet.Shape.SQUARE;
                    z.wobble = 1.5f;
                }
                if (moveTimer <= 0) {
                    world.sfx().play("alarm", 0.8f); // ...and wakes with a start
                    ringsLeft = 1;
                    ring(world, diff);
                    rest();
                }
                break;
            case HOPS:
                hop(delta, world, diff);
                break;
            case CLOCK_FACE:
                if (moveTimer <= 0) rest();
                break;
        }
    }

    /** Waddles towards a good distance from the player. */
    private void walk(float delta, GameWorld world) {
        keepDistance(delta, world, 3f, speed * (enraged ? 1.25f : 1f));
    }

    private void rest() {
        move = Move.REST;
        moveTimer = enraged ? 0.55f : 0.9f;
    }

    private void startNext(GameWorld world) {
        Move[] order = enraged ? new Move[]{Move.TICK_TOCK, Move.CLOCK_FACE, Move.RING, Move.HOPS, Move.CLOCK_FACE, Move.SNOOZE}
                : new Move[]{Move.TICK_TOCK, Move.RING, Move.HOPS, Move.SNOOZE};
        move = order[nextMove++ % order.length];
        shotTimer = 0.3f;
        switch (move) {
            case TICK_TOCK:
                moveTimer = TICK_TIME;
                minute = aimAngle + 70f; // (the hands start off to the side of you)
                hour = aimAngle - 70f;
                world.sfx().play("reload", 0.6f);
                break;
            case RING:
                ringsLeft = enraged ? RINGS + 1 : RINGS;
                world.sfx().play("alarm", 1f);
                break;
            case SNOOZE:
                moveTimer = SNOOZE_TIME;
                world.toast("SNOOZE...");
                break;
            case HOPS:
                hopsLeft = HOPS;
                startHop(world);
                break;
            case CLOCK_FACE:
                clockFace(world);
                moveTimer = FACE_HANG + 1.3f;
                break;
            default:
                break;
        }
    }

    /** A bullet along a hand, from its tip. */
    private void hand(GameWorld world, float angle, float speed, Color color) {
        Bullet b = world.spawnEnemyBullet(pos.x + MathUtils.cosDeg(angle) * 0.45f, pos.y + 0.1f + MathUtils.sinDeg(angle) * 0.45f,
                angle, speed);
        b.color.set(color);
    }

    /** A ring of notes with a gap - the gap a little off your way each time, so you have to move. */
    private void ring(GameWorld world, float diff) {
        int n = enraged ? 26 : 22;
        float gapAt = aimAngle + MathUtils.random(-50f, 50f);
        for (int i = 0; i < n; i++) {
            float a = gapAt + RING_GAP / 2f + (360f - RING_GAP) * i / (n - 1f);
            Bullet b = world.spawnEnemyBullet(pos.x, pos.y + 0.6f, a, 1.9f * diff);
            b.shape = Bullet.Shape.NOTE;
            b.color.set(NOTE);
        }
        world.shake(0.2f);
        world.sfx().play("alarm", 0.5f);
    }

    // ------------------------------------------------------------ hopping

    private void startHop(GameWorld world) {
        Player p = world.getPlayer();
        hopFrom.set(pos);
        hopTo.set(p.centerX(), p.feetY() + 0.72f);
        step.set(hopTo).sub(hopFrom);
        if (step.len() > 2.6f) step.setLength(2.6f); // (it can't reach you in one hop from far off)
        hopTo.set(hopFrom).add(step);
        Arena.now.clamp(hopTo, radius + 0.3f);
        hopClock = 0f;
    }

    private void hop(float delta, GameWorld world, float diff) {
        hopClock += delta * Math.min(diff, 1.3f);
        float k = MathUtils.clamp(hopClock / HOP_TIME, 0f, 1f);
        pos.set(hopFrom).lerp(hopTo, k);
        hopHeight = MathUtils.sin(k * MathUtils.PI) * 0.6f;
        if (k < 1f) return;
        hopHeight = 0f;
        int n = enraged ? 16 : 13; // it lands: a ring
        float off = MathUtils.random(360f);
        for (int i = 0; i < n; i++) {
            Bullet b = world.spawnEnemyBullet(pos.x, feetY() + 0.1f, off + 360f * i / n, 2.1f * diff);
            b.color.set(CASE_LIGHT);
        }
        world.shake(0.35f);
        world.sfx().play("boom", 0.5f);
        if (--hopsLeft > 0) {
            startHop(world);
        } else {
            rest();
        }
    }

    // ------------------------------------------------------------ the clock face

    /** Twelve numbers round where you stand, one missing; they hang, then all close in on the middle. */
    private void clockFace(GameWorld world) {
        Player p = world.getPlayer();
        faceCenter.set(p.centerX(), p.hitboxY());
        int missing = MathUtils.random(11);
        numbers.clear();
        for (int i = 0; i < 12; i++) {
            if (i == missing) continue;
            for (int twin = 0; twin < 2; twin++) { // each number is a pair, a bit apart: no squeezing between
                float a = 90f - i * 30f + (twin - 0.5f) * 10f;
                float x = faceCenter.x + MathUtils.cosDeg(a) * FACE_RADIUS;
                float y = faceCenter.y + MathUtils.sinDeg(a) * FACE_RADIUS;
                Bullet b = world.spawnEnemyBullet(x, y, a + 180f, 0.01f);
                b.vel.setZero();
                b.shape = Bullet.Shape.DIAMOND;
                b.color.set(NUMBER);
                b.life = FACE_HANG + 6f;
                numbers.add(b);
            }
        }
        faceClock = FACE_HANG;
        world.sfx().play("reload", 1f);
    }

    private void updateFace(float delta, GameWorld world, float diff) {
        if (numbers.size == 0) return;
        faceClock -= delta;
        if (faceClock > 0) {
            for (Bullet b : numbers) b.vel.setZero();
            return;
        }
        for (Bullet b : numbers) {
            if (!b.alive) continue;
            float a = MathUtils.atan2Deg360(faceCenter.y - b.pos.y, faceCenter.x - b.pos.x);
            b.vel.set(MathUtils.cosDeg(a), MathUtils.sinDeg(a)).scl(2.4f * diff);
            b.life = FACE_RADIUS * 2.2f / (2.4f * diff); // (through the middle and out the other side a bit)
        }
        numbers.clear();
        world.sfx().play("swish", 0.8f);
    }

    // ------------------------------------------------------------ drawing

    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (numbers.size > 0) { // the clock face, faint on the floor
            d.ring(b, faceCenter.x, faceCenter.y, FACE_RADIUS, NUMBER, 0.35f);
            d.circle(b, faceCenter.x, faceCenter.y, 0.08f, TELEGRAPH, 0.7f);
        }
        if (move == Move.HOPS && hopHeight > 0f) { // where it'll land
            d.ellipse(b, hopTo.x, hopTo.y - 0.72f, radius * 1.1f, radius * 0.4f, TELEGRAPH, 0.35f);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float x = pos.x;
        float lift = hopHeight;
        boolean ringing = move == Move.RING || move == Move.SNOOZE && moveTimer < 0.3f
                || enraged && MathUtils.sin(time * 3f) > 0.8f;
        float shake = ringing ? MathUtils.sin(time * 70f) * 0.03f : 0f;
        float y = pos.y + lift;
        float bottom = feetY() + lift;
        boolean dozing = move == Move.SNOOZE;
        // little legs
        float stride = MathUtils.sin(walkTime * 10f) * 0.04f;
        d.rotRect(b, x - 0.25f, bottom + 0.1f + stride, 0.07f, 0.24f, -15f, c(LEG), 1f);
        d.rotRect(b, x + 0.25f, bottom + 0.1f - stride, 0.07f, 0.24f, 15f, c(LEG), 1f);
        d.ellipse(b, x - 0.3f, bottom, 0.1f, 0.05f, c(LEG), 1f);
        d.ellipse(b, x + 0.3f, bottom, 0.1f, 0.05f, c(LEG), 1f);
        // the bells and the hammer between them
        for (int i = -1; i <= 1; i += 2) {
            float bx = x + i * 0.36f + shake * i;
            float by = y + 0.58f;
            d.circle(b, bx, by, 0.24f, Draw.OUTLINE_COLOR);
            d.circle(b, bx, by, 0.21f, c(BELL));
            d.circle(b, bx - 0.05f, by + 0.06f, 0.08f, Color.WHITE, 0.4f);
            d.rect(b, bx - 0.04f, by - 0.25f, 0.08f, 0.1f, c(BELL_DARK));
        }
        float hammer = ringing ? MathUtils.sin(time * 70f) * 25f : 0f;
        d.rotRect(b, x, y + 0.62f, 0.04f, 0.3f, hammer, c(LEG), 1f);
        d.circle(b, x + MathUtils.sinDeg(-hammer) * 0.15f, y + 0.77f, 0.06f, c(LEG));
        // the case
        d.circle(b, x + shake, y, 0.6f, Draw.OUTLINE_COLOR);
        d.circle(b, x + shake, y, 0.56f, c(CASE));
        d.circle(b, x + shake - 0.12f, y + 0.12f, 0.4f, c(CASE_LIGHT), 0.35f);
        d.circle(b, x + shake + 0.15f, y - 0.18f, 0.35f, c(CASE_DARK), 0.4f);
        // the face: hour marks, the two hands, its eyes and mouth
        float fx = x + shake;
        float fy = y;
        d.circle(b, fx, fy, 0.44f, c(FACE));
        for (int i = 0; i < 12; i++) {
            float a = i * 30f;
            float r = i % 3 == 0 ? 0.36f : 0.38f;
            d.rotRect(b, fx + MathUtils.cosDeg(a) * r, fy + MathUtils.sinDeg(a) * r, i % 3 == 0 ? 0.07f : 0.04f, 0.025f, a, INK, 1f);
        }
        float showMinute = move == Move.TICK_TOCK ? minute : 90f - time * 30f;
        float showHour = move == Move.TICK_TOCK ? hour : 90f - time * 2.5f;
        d.line(b, fx, fy, 0.33f, 0.035f, showMinute, MINUTE);
        d.line(b, fx, fy, 0.22f, 0.05f, showHour, INK);
        d.circle(b, fx, fy, 0.04f, INK);
        if (dozing) { // eyes shut, snoring
            d.rotRect(b, fx - 0.15f, fy + 0.12f, 0.12f, 0.025f, 0f, INK, 1f);
            d.rotRect(b, fx + 0.15f, fy + 0.12f, 0.12f, 0.025f, 0f, INK, 1f);
            d.ellipse(b, fx, fy - 0.2f, 0.06f, 0.04f + 0.02f * MathUtils.sin(time * 3f), INK, 1f);
        } else {
            float lx = MathUtils.cosDeg(aimAngle) * 0.025f;
            float ly = MathUtils.sinDeg(aimAngle) * 0.025f;
            for (int i = -1; i <= 1; i += 2) {
                d.circle(b, fx + i * 0.15f, fy + 0.13f, 0.075f, Color.WHITE);
                d.circle(b, fx + i * 0.15f + lx, fy + 0.13f + ly, 0.04f, INK);
                d.rotRect(b, fx + i * 0.15f, fy + 0.24f, 0.11f, 0.025f, enraged ? -i * 20f : i * 8f, INK, 1f); // brows
            }
            if (ringing) { // shouting WAKE UP
                d.ellipse(b, fx, fy - 0.2f, 0.11f, 0.08f, INK, 1f);
            } else {
                d.rotRect(b, fx, fy - 0.2f, 0.16f, 0.025f, 0f, INK, 1f);
            }
        }
        if (dozing) { // the Zs drifting up off it
            for (int i = 0; i < 3; i++) {
                float t = (time * 0.6f + i / 3f) % 1f;
                float zx = x + 0.45f + t * 0.35f;
                float zy = y + 0.6f + t * 0.6f;
                float s = 0.05f + 0.04f * t;
                d.rect(b, zx - s, zy + s, s * 2f, 0.025f, SNORE, 1f - t);
                d.rotRect(b, zx, zy, 0.025f, s * 2.6f, 45f, SNORE, 1f - t);
                d.rect(b, zx - s, zy - s, s * 2f, 0.025f, SNORE, 1f - t);
            }
        }
    }
}
