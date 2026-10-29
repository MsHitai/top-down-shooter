package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * The Shadow King: when the Fluffy King himself plays (player/Hero.KING, pocket size), someone else sits on
 * his throne in the clouds - a pretender as big as the King once was, all dark fluff and a tarnished crown.
 * By then the player is very good, so he's harder than the King, and his patterns are his own - in the
 * spirit of the old Traffic Light's green streams that swung after you, not a copy:
 * <ul>
 *   <li>streams: two dark streams in pulses, swinging slowly after you - follow the gap between them;</li>
 *   <li>echoes: a ring with a gap, and the spot you stood on is marked - a moment later the shadow bursts
 *       out of the mark, so you can't stay put;</li>
 *   <li>glare: his eyes go red - while they glare, moving brings bolts at you, and dark orbs gather round
 *       you; when the glare ends the orbs close in, so then you must move (red light, then go!);</li>
 *   <li>shadow balls: fluff balls that burst into a ring - and again, half a step round, a moment later.</li>
 * </ul>
 * At half health he stops pretending: out of reach for a moment, every bullet gone, he swells up and his
 * eyes stay red - three streams, more echoes, and every pattern ends with an echo where you stand.
 */
public class ShadowKing extends Enemy {

    private enum Phase { WANDER, STREAMS, ECHOES, GLARE, BALLS, REVEAL }

    private static final Color FLUFF = Color.valueOf("3b2a30");
    private static final Color FLUFF_SHADE = Color.valueOf("221519");
    private static final Color FLUFF_EDGE = Color.valueOf("5a4048");
    private static final Color CROWN = Color.valueOf("9a7a3a");
    private static final Color GEM = Color.valueOf("8a3ae8");
    private static final Color EYE = Color.valueOf("c9a8ff");
    private static final Color GLARE = Color.valueOf("ff2a4a");
    private static final Color STREAM_BULLET = Color.valueOf("5a2a7a");
    private static final Color ECHO_BULLET = Color.valueOf("8a3ae8");
    private static final Color BOLT = Color.valueOf("ff2a4a");
    private static final Color ORB = Color.valueOf("2a1830");
    private static final Color MARK = Color.valueOf("4a2060");
    private static final Color SMOKE = Color.valueOf("3a2a40");

    /** As big as the King on his throne was. */
    private static final float S = 1.8f;
    private static final float GROW = 1.15f;
    /** He stops pretending below this share of his health. */
    private static final float PHASE_TWO = 0.5f;
    private static final float REVEAL = 2f;
    /** Streams: how long a stream fires, then rests; how fast it swings after you (degrees a second). */
    private static final float STREAM_ON = 0.55f;
    private static final float STREAM_OFF = 0.35f;
    private static final float STREAM_TURN = 38f;
    /** How long an echo waits on its mark before it bursts. */
    private static final float ECHO_DELAY = 1.05f;
    /** Glare: the warning, then the glare itself; the orbs gather this far round you. */
    private static final float GLARE_TELL = 0.8f;
    private static final float GLARE_TIME = 2.4f;
    private static final float ORB_RING = 2.1f;
    /** Moving faster than this while he glares is moving. */
    private static final float STILL = 0.45f;
    private static final float BALL_TIME = 0.9f;

    private static final float[][] PUFFS = {
            {-0.28f, 0.05f}, {0.28f, 0.05f}, {-0.2f, 0.24f}, {0.2f, 0.24f},
            {-0.2f, -0.17f}, {0.2f, -0.17f}, {0f, 0.3f}, {0f, -0.22f},
    };

    private Phase phase = Phase.WANDER;
    private float phaseTimer = 1.6f;
    private float shotTimer;
    private int shotsLeft;
    private boolean revealed;
    private int patternIndex;
    private float time;
    private float grow = 1f;
    /** The streams' directions (degrees), each swinging after the player on its own. */
    private final FloatArray streams = new FloatArray();
    private float streamClock;
    /** Glare: the spot it began on (the orbs gather round it) and the cooldown between bolts. */
    private final Vector2 glareAt = new Vector2();
    private float boltTimer;
    private final Vector2 target = new Vector2();
    private final Vector2 step = new Vector2();
    /** Rings waiting to burst out of a spot: x, y, time left, bullets, start angle, speed, gap angle (-1 none). */
    private final Array<float[]> echoes = new Array<>();
    private final Array<Bullet> balls = new Array<>();
    private final FloatArray ballTimers = new FloatArray();

    public ShadowKing(float x, float y, int tier) {
        super(x, y, 1500f + 280f * tier);
        radius = 0.4f * S;
        speed = 0.65f;
        score = 1100 + 300 * tier;
        boss = true;
        target.set(x, y);
    }

    @Override
    public String bossName() {
        return Boss.SHADOW_KING.title;
    }

    @Override
    public boolean isEnraged() {
        return revealed;
    }

    private float size() {
        return S * grow;
    }

    @Override
    public float feetY() {
        return pos.y - 0.42f * size();
    }

    @Override
    protected boolean isDodging() {
        return phase == Phase.REVEAL;
    }

    private boolean glaring() {
        return phase == Phase.GLARE && phaseTimer < GLARE_TIME;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        knock.setZero();
        float diff = world.getDifficulty();
        updateEchoes(delta, world, diff);
        updateBalls(delta, world, diff);
        if (!revealed && phase != Phase.REVEAL && hp < maxHp * PHASE_TWO) {
            startReveal(world);
        }
        float rage = revealed ? 1.3f : 1f;
        if (phase == Phase.WANDER || phase == Phase.STREAMS || phase == Phase.BALLS || phase == Phase.ECHOES) {
            drift(delta, world, rage);
        }
        phaseTimer -= delta;
        shotTimer -= delta;
        Player p = world.getPlayer();

        switch (phase) {
            case WANDER:
                if (phaseTimer <= 0) {
                    nextPattern(world, p);
                }
                break;
            case STREAMS:
                streamClock += delta;
                for (int i = 0; i < streams.size; i++) { // each stream swings after you, but slowly
                    float want = angleTo(p);
                    float now = streams.get(i);
                    float turn = MathUtils.clamp(((want - now + 540f) % 360f) - 180f, -STREAM_TURN * rage * delta,
                            STREAM_TURN * rage * delta);
                    streams.set(i, now + turn);
                }
                boolean on = streamClock % (STREAM_ON + STREAM_OFF) < STREAM_ON;
                if (on && shotTimer <= 0) {
                    for (int i = 0; i < streams.size; i++) {
                        Bullet b = shoot(world, streams.get(i), 2.7f * diff);
                        b.color.set(STREAM_BULLET);
                        b.radius = 0.07f;
                    }
                    shotTimer = 0.075f;
                    world.sfx().play("enemy_shoot", 0.3f);
                }
                if (phaseTimer <= 0) {
                    toWander(world, p);
                }
                break;
            case ECHOES:
                if (shotTimer <= 0 && shotsLeft > 0) {
                    int n = revealed ? 26 : 22;
                    float gap = angleTo(p) + MathUtils.random(-70f, 70f); // the gap is never just where you are
                    ring(world, pos.x, pos.y, n, gap, 2.2f * diff, gap, ECHO_BULLET);
                    addEcho(p.centerX(), p.hitboxY(), revealed ? 20 : 16, revealed ? 2f : 1.7f);
                    world.sfx().play("enemy_shoot", 0.8f);
                    shotsLeft--;
                    shotTimer = 0.8f / rage;
                }
                if (shotsLeft <= 0 && shotTimer <= 0) {
                    toWander(world, p);
                }
                break;
            case GLARE:
                if (phaseTimer >= GLARE_TIME) { // the eyes going red: a warning
                    glareAt.set(p.centerX(), p.hitboxY());
                    break;
                }
                boltTimer -= delta;
                if (p.getVelocity().len() > STILL && boltTimer <= 0) { // red light: you moved
                    Bullet b = shoot(world, angleTo(p), 4.6f * diff);
                    b.color.set(BOLT);
                    b.radius = 0.06f;
                    boltTimer = 0.2f / rage;
                    world.sfx().play("enemy_shoot", 0.6f);
                }
                if (phaseTimer <= 0) { // ...and go: the orbs close in on where you stood
                    int n = revealed ? 20 : 16;
                    float start = MathUtils.random(360f);
                    for (int i = 0; i < n; i++) {
                        float a = start + 360f * i / n;
                        float ox = glareAt.x + MathUtils.cosDeg(a) * ORB_RING;
                        float oy = glareAt.y + MathUtils.sinDeg(a) * ORB_RING;
                        Bullet b = world.spawnEnemyBullet(ox, oy, a + 180f, 2.1f * diff);
                        b.color.set(ORB);
                        b.radius = 0.08f;
                        b.life = ORB_RING * 2f / (2.1f * diff);
                    }
                    world.sfx().play("pop", 0.8f);
                    toWander(world, p);
                }
                break;
            case BALLS:
                if (shotTimer <= 0 && shotsLeft > 0) {
                    lobBall(world, p, shotsLeft == 1);
                    shotsLeft--;
                    shotTimer = 0.32f / rage;
                }
                if (shotsLeft <= 0 && shotTimer <= 0) {
                    toWander(world, p);
                }
                break;
            case REVEAL:
                grow = 1f + (GROW - 1f) * Interpolation.pow2Out.apply(1f - Math.max(0f, phaseTimer) / REVEAL);
                radius = 0.4f * size();
                if (MathUtils.random() < delta * 16f) {
                    world.particles().burst(pos.x + MathUtils.random(-0.5f, 0.5f) * size(), pos.y + 0.2f * size(),
                            2, 0.2f, 1.2f, 0.7f, 0.08f, SMOKE);
                }
                if (phaseTimer <= 0) {
                    world.shake(0.8f);
                    world.sfx().play("boss", 1f);
                    phase = Phase.WANDER;
                    phaseTimer = 0.6f;
                }
                break;
        }
    }

    private float angleTo(Player p) {
        return MathUtils.atan2Deg360(p.hitboxY() - pos.y, p.centerX() - pos.x);
    }

    private void drift(float delta, GameWorld world, float rage) {
        boolean hunting = huntForSight(delta, world, target);
        if (!hunting && pos.dst2(target) < 0.05f) {
            target.set(MathUtils.random(Arena.now.left + radius + 0.6f, Arena.now.right - radius - 0.6f),
                    MathUtils.random(Arena.now.centerY - 0.2f, Arena.now.top - radius - 0.25f));
        }
        float moveSpeed = hunting ? speed * 2f : phase == Phase.WANDER ? speed * 1.6f : speed * 0.45f;
        step.set(target).sub(pos);
        if (step.len() > 0.01f) {
            step.nor().scl(moveSpeed * rage * delta);
            pos.add(step);
            walkTime += delta;
        }
    }

    /** A ring of {@code n} from (x, y); with {@code gap} >= 0, the bullets within one step of it are left out. */
    private void ring(GameWorld world, float x, float y, int n, float start, float speed, float gap, Color color) {
        float stepDeg = 360f / n;
        for (int i = 0; i < n; i++) {
            float a = start + stepDeg * i;
            if (gap >= 0 && Math.abs(((a - gap + 540f) % 360f) - 180f) < stepDeg * 1.6f) {
                continue;
            }
            Bullet b = world.spawnEnemyBullet(x, y, a, speed);
            b.color.set(color);
            b.radius = 0.07f;
        }
    }

    private void addEcho(float x, float y, int n, float speed) {
        echoes.add(new float[]{x, y, ECHO_DELAY, n, MathUtils.random(360f), speed});
    }

    private void updateEchoes(float delta, GameWorld world, float diff) {
        for (int i = echoes.size - 1; i >= 0; i--) {
            float[] e = echoes.get(i);
            e[2] -= delta;
            if (e[2] > 0) {
                continue;
            }
            ring(world, e[0], e[1], (int) e[3], e[4], e[5] * diff, -1f, ECHO_BULLET);
            world.particles().burst(e[0], e[1], 12, 0.3f, 1.4f, 0.5f, 0.06f, SMOKE);
            world.sfx().play("pop", 0.6f);
            echoes.removeIndex(i);
        }
    }

    /** A dark fluff ball lobbed near you (the last right at you); it bursts into a ring - and echoes. */
    private void lobBall(GameWorld world, Player p, boolean atPlayer) {
        float tx = p.centerX() + (atPlayer ? 0f : MathUtils.random(-1.8f, 1.8f));
        float ty = p.hitboxY() + (atPlayer ? 0f : MathUtils.random(-1.2f, 1.2f));
        float dist = Vector2.dst(pos.x, pos.y, tx, ty);
        Bullet b = shoot(world, MathUtils.atan2Deg360(ty - pos.y, tx - pos.x), Math.max(0.6f, dist / BALL_TIME));
        b.shape = Bullet.Shape.CLOUD;
        b.color.set(FLUFF_EDGE);
        b.radius = 0.15f;
        balls.add(b);
        ballTimers.add(BALL_TIME);
        world.sfx().play("pop", 0.6f);
    }

    private void updateBalls(float delta, GameWorld world, float diff) {
        for (int i = balls.size - 1; i >= 0; i--) {
            float left = ballTimers.get(i) - delta;
            ballTimers.set(i, left);
            Bullet ball = balls.get(i);
            if (left > 0 && ball.alive) {
                continue;
            }
            if (ball.alive) {
                ball.alive = false;
                int n = revealed ? 12 : 10;
                float start = MathUtils.random(360f);
                ring(world, ball.pos.x, ball.pos.y, n, start, 1.7f * diff, -1f, STREAM_BULLET);
                // the echo: the same ring again, half a step round
                echoes.add(new float[]{ball.pos.x, ball.pos.y, 0.55f, n, start + 180f / n, 1.7f});
                world.particles().burst(ball.pos.x, ball.pos.y, 10, 0.3f, 1.4f, 0.4f, 0.05f, SMOKE);
                world.sfx().play("pop", 0.5f);
            }
            balls.removeIndex(i);
            ballTimers.removeIndex(i);
        }
    }

    private void startReveal(GameWorld world) {
        revealed = true;
        phase = Phase.REVEAL;
        phaseTimer = REVEAL;
        balls.clear();
        ballTimers.clear();
        echoes.clear();
        world.clearEnemyBullets(0, 0, -1f);
        world.shake(0.6f);
        world.sfx().play("boss", 1f);
        world.toast("THE SHADOW KING STOPS PRETENDING!");
    }

    private void toWander(GameWorld world, Player p) {
        phase = Phase.WANDER;
        phaseTimer = revealed ? 0.7f : 1.2f;
        if (revealed) { // every pattern ends with an echo where you stand
            addEcho(p.centerX(), p.hitboxY(), 14, 1.8f);
        }
    }

    private void nextPattern(GameWorld world, Player p) {
        Phase[] order = revealed
                ? new Phase[]{Phase.STREAMS, Phase.GLARE, Phase.ECHOES, Phase.BALLS, Phase.STREAMS, Phase.ECHOES}
                : new Phase[]{Phase.STREAMS, Phase.ECHOES, Phase.GLARE, Phase.BALLS};
        phase = order[patternIndex++ % order.length];
        shotTimer = 0.4f;
        switch (phase) {
            case STREAMS: {
                phaseTimer = revealed ? 4.2f : 3.6f;
                streamClock = 0f;
                streams.clear();
                float at = angleTo(p);
                int n = revealed ? 3 : 2;
                for (int i = 0; i < n; i++) { // they start wide of you and swing in
                    streams.add(at + (i - (n - 1) / 2f) * 70f + (n == 2 ? 0f : 0f));
                }
                break;
            }
            case ECHOES:
                shotsLeft = revealed ? 4 : 3;
                break;
            case GLARE:
                phaseTimer = GLARE_TELL + GLARE_TIME;
                boltTimer = 0f;
                world.sfx().play("buzz", 0.6f);
                break;
            case BALLS:
                shotsLeft = revealed ? 6 : 5;
                break;
            default:
                break;
        }
    }

    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        for (float[] e : echoes) { // the marks the echoes will burst out of, closing in as they come
            float k = MathUtils.clamp(e[2] / ECHO_DELAY, 0f, 1f);
            d.circle(b, e[0], e[1], 0.32f, MARK, 0.35f);
            d.ring(b, e[0], e[1], 0.32f + 0.5f * k, ECHO_BULLET, 0.8f - 0.4f * k);
        }
        if (glaring()) { // the orbs gathering round where you stood
            float k = 1f - phaseTimer / GLARE_TIME;
            int n = revealed ? 20 : 16;
            for (int i = 0; i < n; i++) {
                float a = 360f * i / n + time * 20f;
                d.circle(b, glareAt.x + MathUtils.cosDeg(a) * ORB_RING, glareAt.y + MathUtils.sinDeg(a) * ORB_RING,
                        0.04f + 0.05f * k, ORB, 0.4f + 0.5f * k);
            }
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float s = size();
        float wob = MathUtils.sin(time * 3.5f) * 0.015f * s;
        float cx = pos.x;
        float cy = pos.y + wob;
        if (phase == Phase.REVEAL) {
            cx += MathUtils.random(-0.03f, 0.03f);
            cy += MathUtils.random(-0.03f, 0.03f);
        }
        drawFeet(b, d, cx, feetY() + 0.03f, 0.18f * s);
        for (float[] p : PUFFS) {
            d.circle(b, cx + p[0] * s, cy + p[1] * s, 0.17f * s, c(FLUFF_SHADE));
        }
        for (float[] p : PUFFS) {
            d.circle(b, cx + p[0] * 0.92f * s, cy + (p[1] * 0.92f + 0.015f) * s, 0.15f * s, c(FLUFF));
        }
        d.circle(b, cx, cy + 0.03f * s, 0.27f * s, c(FLUFF));
        d.circle(b, cx - 0.1f * s, cy + 0.14f * s, 0.08f * s, c(FLUFF_EDGE)); // a dull sheen

        // the tarnished crown, a little crooked - it isn't his
        float top = cy + 0.38f * s;
        d.rotRect(b, cx, top + 0.04f * s, 0.36f * s, 0.08f * s, -6f, c(CROWN), 1f);
        for (int i = -1; i <= 1; i++) {
            d.rotRect(b, cx + i * 0.13f * s, top + 0.1f * s - i * 0.012f * s, 0.08f * s, 0.08f * s, 39, c(CROWN), 1f);
        }
        d.circle(b, cx, top + 0.04f * s, 0.03f * s, GEM);

        // glowing eyes - narrowed, violet; red while he glares (and for good once he stops pretending)
        boolean red = revealed || phase == Phase.GLARE;
        boolean warning = phase == Phase.GLARE && phaseTimer >= GLARE_TIME && ((int) (time * 10f)) % 2 == 0;
        Color eye = red && !warning ? GLARE : EYE;
        for (int i = -1; i <= 1; i += 2) {
            float ex = cx + i * 0.1f * s;
            float ey = cy + 0.08f * s;
            d.ellipse(b, ex, ey, 0.09f * s, 0.06f * s, eye, red ? 0.35f : 0.2f); // the glow
            d.ellipse(b, ex, ey, 0.055f * s, 0.03f * s, eye, 1f);
            d.rotRect(b, ex, ey + 0.045f * s, 0.12f * s, 0.025f * s, i * 22f, FLUFF_SHADE, 1f);
        }
        // a smirk
        float my = cy - 0.075f * s;
        d.rect(b, cx - 0.05f * s, my, 0.1f * s, 0.022f * s, PUPIL);
        d.rotRect(b, cx + 0.065f * s, my + 0.015f * s, 0.05f * s, 0.022f * s, 30f, PUPIL, 1f);
        if (glaring()) {
            d.ring(b, cx, cy, radius * (1.3f + 0.15f * MathUtils.sin(time * 12f)), GLARE, 0.5f);
        }
    }
}
