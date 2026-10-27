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
 * The final boss (the game is called Enter the Fluffy after all), on his throne in the clouds (floor 5)
 * and in endless mode's boss waves. A fight in two phases:
 * <ul>
 *   <li>phase one: he drifts about firing rings, spirals, aimed fans and fluff balls that land around you
 *       and burst into rings;</li>
 *   <li>at a little over half health he throws a tantrum - out of reach for a moment, every bullet gone - and
 *       swells up red and furious - he fights alone (his angel guard made it too hard);</li>
 *   <li>phase two: everything faster and thicker, and a belly flop: he leaps up, his shadow chases you, and he
 *       lands with a double shockwave you have to roll through.</li>
 * </ul>
 */
public class FluffyKing extends Enemy {

    private enum Phase { WANDER, RINGS, SPIRAL, FANS, PUFFS, TANTRUM, CROUCH, AIR, LAND }

    private static final Color FLUFF = Color.valueOf("ffd6e8");
    private static final Color FLUFF_SHADE = Color.valueOf("f5a3c7");
    private static final Color FURY = Color.valueOf("ff9cb8");
    private static final Color FURY_SHADE = Color.valueOf("e8557e");
    private static final Color CROWN = Color.valueOf("ffc83d");
    private static final Color GEM = Color.valueOf("e84393");
    private static final Color RING_BULLET = Color.valueOf("ff5e8a");
    private static final Color SPIRAL_BULLET = Color.valueOf("ff9f1a");
    private static final Color FAN_BULLET = Color.valueOf("ff3b3b");
    private static final Color PUFF_BULLET = Color.valueOf("ffc6dc");
    private static final Color WAVE = Color.valueOf("e84393");
    private static final Color STEAM = Color.valueOf("f5f0f8");

    /** He's the final boss: everything about his body is drawn this much bigger (and his hitbox matches). */
    private static final float S = 1.8f;
    /** In phase two he puffs up this much more. */
    private static final float FURY_GROW = 1.22f;
    /** Phase two starts below this share of his health. */
    private static final float PHASE_TWO = 0.55f;
    private static final float TANTRUM = 2.2f;
    private static final float CROUCH = 0.55f;
    private static final float AIR = 1.25f;
    private static final float LAND = 0.6f;
    private static final float STOMP_RADIUS = 0.9f;
    /** How long a fluff ball flies before it bursts. */
    private static final float PUFF_TIME = 0.95f;

    private static final float[][] PUFFS = {
            {-0.28f, 0.05f}, {0.28f, 0.05f}, {-0.2f, 0.24f}, {0.2f, 0.24f},
            {-0.2f, -0.17f}, {0.2f, -0.17f}, {0f, 0.3f}, {0f, -0.22f},
    };

    private Phase phase = Phase.WANDER;
    private float phaseTimer = 1.5f;
    private float shotTimer;
    private int shotsLeft;
    private float spiralAngle;
    /** Phase two (the boss bar turns red). */
    private boolean furious;
    /** Made up with his grandfather (the true ending's picture): no frown, a smile. */
    private boolean happy;
    private int patternIndex;
    private float time;
    /** 1 in phase one, growing to FURY_GROW through the tantrum. */
    private float grow = 1f;
    private final Vector2 target = new Vector2();
    private final Vector2 step = new Vector2();
    private final Vector2 landAt = new Vector2();
    private final Vector2 jumpFrom = new Vector2();
    private final Array<Bullet> fluffBalls = new Array<>();
    private final FloatArray fluffTimers = new FloatArray();

    /** For the true ending's picture: all smiles. */
    public void showHappy() {
        happy = true;
    }

    public FluffyKing(float x, float y, int tier) {
        super(x, y, 1300f + 250f * tier);
        radius = 0.4f * S;
        speed = 0.6f;
        score = 900 + 300 * tier;
        boss = true;
        target.set(x, y);
    }

    @Override
    public String bossName() {
        return Boss.FLUFFY_KING.title;
    }

    @Override
    public boolean isEnraged() {
        return furious;
    }

    private float size() {
        return S * grow;
    }

    @Override
    public float feetY() {
        return pos.y - 0.42f * size();
    }

    /** Mid-tantrum and up in the air he's out of reach. */
    @Override
    protected boolean isDodging() {
        return phase == Phase.TANTRUM || phase == Phase.AIR;
    }

    @Override
    protected boolean castsShadow() {
        return phase != Phase.AIR;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        knock.setZero();
        float diff = world.getDifficulty();
        updateFluffBalls(delta, world, diff);
        if (!furious && phase != Phase.TANTRUM && hp < maxHp * PHASE_TWO && phase != Phase.AIR) {
            startTantrum(world);
        }
        float rage = furious ? 1.35f : 1f;

        if (phase != Phase.TANTRUM && phase != Phase.CROUCH && phase != Phase.AIR && phase != Phase.LAND) {
            drift(delta, world, rage);
        }
        phaseTimer -= delta;
        shotTimer -= delta;

        switch (phase) {
            case WANDER:
                if (phaseTimer <= 0) {
                    nextPattern(world);
                }
                break;
            case RINGS:
                if (shotTimer <= 0 && shotsLeft > 0) {
                    int n = furious ? 28 : 22;
                    float off = (shotsLeft % 2) * 180f / n;
                    for (int i = 0; i < n; i++) {
                        Bullet b = shoot(world, off + 360f * i / n, 2.0f * diff);
                        b.color.set(RING_BULLET);
                        b.radius = 0.075f;
                    }
                    world.sfx().play("enemy_shoot", 0.9f);
                    shotsLeft--;
                    shotTimer = 0.5f / rage;
                }
                if (shotsLeft <= 0 && shotTimer <= 0) {
                    toWander();
                }
                break;
            case SPIRAL:
                if (shotTimer <= 0) {
                    int arms = furious ? 5 : 3;
                    for (int i = 0; i < arms; i++) {
                        Bullet b = shoot(world, spiralAngle + 360f * i / arms, 1.9f * diff);
                        b.color.set(SPIRAL_BULLET);
                        if (furious && i % 2 == 0) {
                            b.curve = -20f;
                        }
                    }
                    spiralAngle += furious ? -11f : 11f;
                    shotTimer = 0.09f / rage;
                    world.sfx().play("enemy_shoot", 0.4f);
                }
                if (phaseTimer <= 0) {
                    toWander();
                }
                break;
            case FANS:
                if (shotTimer <= 0 && shotsLeft > 0) {
                    int n = furious ? 9 : 7;
                    float fan = 70f;
                    for (int i = 0; i < n; i++) {
                        Bullet b = shoot(world, aimAngle - fan / 2f + fan * i / (n - 1), 2.9f * diff);
                        b.color.set(FAN_BULLET);
                    }
                    world.sfx().play("enemy_shoot", 0.9f);
                    shotsLeft--;
                    shotTimer = 0.38f / rage;
                }
                if (shotsLeft <= 0 && shotTimer <= 0) {
                    toWander();
                }
                break;
            case PUFFS:
                if (shotTimer <= 0 && shotsLeft > 0) {
                    lobFluffBall(world, shotsLeft == 1);
                    shotsLeft--;
                    shotTimer = 0.28f / rage;
                }
                if (shotsLeft <= 0 && shotTimer <= 0) {
                    toWander();
                }
                break;
            case TANTRUM:
                grow = 1f + (FURY_GROW - 1f) * Interpolation.pow2Out.apply(1f - Math.max(0f, phaseTimer) / TANTRUM);
                radius = 0.4f * size();
                if (MathUtils.random() < delta * 14f) { // fuming
                    float side = MathUtils.randomSign();
                    world.particles().burst(pos.x + side * 0.45f * size(), pos.y + 0.3f * size(), 2, 0.2f, 1.2f, 0.6f, 0.07f, STEAM);
                }
                if (phaseTimer <= 0) {
                    world.shake(0.8f);
                    world.sfx().play("boss", 1f);
                    toWander();
                }
                break;
            case CROUCH:
                if (phaseTimer <= 0) {
                    phase = Phase.AIR;
                    phaseTimer = AIR;
                    jumpFrom.set(pos);
                    landAt.set(world.getPlayer().centerX(), world.getPlayer().hitboxY() + 0.42f * size());
                    clampLanding();
                    world.sfx().play("flap", 0.8f);
                    world.particles().burst(pos.x, feetY(), 16, 0.5f, 1.6f, 0.4f, 0.06f, STEAM);
                }
                break;
            case AIR: {
                if (phaseTimer > AIR * 0.35f) { // the landing spot chases the player for most of the jump
                    Player p = world.getPlayer();
                    landAt.lerp(step.set(p.centerX(), p.hitboxY() + 0.42f * size()), Math.min(1f, delta * 1.8f));
                    clampLanding();
                }
                pos.set(jumpFrom).lerp(landAt, 1f - Math.max(0f, phaseTimer) / AIR);
                if (phaseTimer <= 0) {
                    land(world, diff);
                }
                break;
            }
            case LAND:
                if (phaseTimer <= 0) {
                    toWander();
                }
                break;
        }
    }

    /** A slow drift toward a random point; hiding behind a pillar doesn't work for long - he comes to find you. */
    private void drift(float delta, GameWorld world, float rage) {
        boolean hunting = huntForSight(delta, world, target);
        if (!hunting && pos.dst2(target) < 0.05f) {
            // keep his (big) body and crown off the walls
            target.set(MathUtils.random(Arena.now.left + radius + 0.6f, Arena.now.right - radius - 0.6f),
                    MathUtils.random(Arena.now.centerY - 0.2f, Arena.now.top - radius - 0.25f));
        }
        float moveSpeed = hunting ? speed * 2f : phase == Phase.WANDER ? speed * 1.6f : speed * 0.5f;
        step.set(target).sub(pos);
        if (step.len() > 0.01f) {
            step.nor().scl(moveSpeed * rage * delta);
            pos.add(step);
            walkTime += delta;
        }
    }

    private void clampLanding() {
        Arena a = Arena.now;
        landAt.x = MathUtils.clamp(landAt.x, a.left + radius + 0.2f, a.right - radius - 0.2f);
        landAt.y = MathUtils.clamp(landAt.y, a.bottom + 0.42f * size() + 0.2f, a.top - radius - 0.25f);
    }

    /** Half health: a tantrum. Out of reach, every bullet gone, and he swells up red - phase two. */
    private void startTantrum(GameWorld world) {
        furious = true;
        phase = Phase.TANTRUM;
        phaseTimer = TANTRUM;
        fluffBalls.clear();
        fluffTimers.clear();
        world.clearEnemyBullets(0, 0, -1f);
        world.shake(0.6f);
        world.sfx().play("boss", 1f);
        world.toast("THE FLUFFY KING IS FURIOUS!");
    }

    /** Crash down: whoever's under him gets hurt, and two rings of bullets roll out (roll through them). */
    private void land(GameWorld world, float diff) {
        phase = Phase.LAND;
        phaseTimer = LAND;
        pos.set(landAt);
        Player p = world.getPlayer();
        if (Vector2.dst(pos.x, feetY() + 0.2f, p.centerX(), p.hitboxY()) < STOMP_RADIUS) {
            world.hurtPlayer();
        }
        world.shake(1f);
        world.sfx().play("boom", 1f);
        world.particles().burst(pos.x, feetY(), 30, 0.8f, 3f, 0.5f, 0.07f, FLUFF);
        int n = 36;
        for (int ring = 0; ring < 2; ring++) {
            for (int i = 0; i < n; i++) {
                Bullet b = world.spawnEnemyBullet(pos.x, feetY() + 0.15f, 360f * (i + ring * 0.5f) / n,
                        (ring == 0 ? 2.1f : 1.4f) * diff);
                b.color.set(WAVE);
                b.radius = 0.06f;
            }
        }
    }

    /** A big ball of fluff lobbed at a spot near the player (the last one right at them); it bursts on landing. */
    private void lobFluffBall(GameWorld world, boolean atPlayer) {
        Player p = world.getPlayer();
        float tx = p.centerX() + (atPlayer ? 0f : MathUtils.random(-1.6f, 1.6f));
        float ty = p.hitboxY() + (atPlayer ? 0f : MathUtils.random(-1.1f, 1.1f));
        float dist = Vector2.dst(pos.x, pos.y, tx, ty);
        float angle = MathUtils.atan2Deg360(ty - pos.y, tx - pos.x);
        Bullet b = shoot(world, angle, Math.max(0.6f, dist / PUFF_TIME));
        b.shape = Bullet.Shape.CLOUD;
        b.color.set(PUFF_BULLET);
        b.radius = 0.15f;
        fluffBalls.add(b);
        fluffTimers.add(PUFF_TIME);
        world.sfx().play("pop", 0.6f);
    }

    /** Fluff balls that have flown their time burst into a ring (unless something stopped them first). */
    private void updateFluffBalls(float delta, GameWorld world, float diff) {
        for (int i = fluffBalls.size - 1; i >= 0; i--) {
            float left = fluffTimers.get(i) - delta;
            fluffTimers.set(i, left);
            Bullet ball = fluffBalls.get(i);
            if (left > 0 && ball.alive) {
                continue;
            }
            if (ball.alive) {
                ball.alive = false;
                int n = furious ? 10 : 8;
                float start = MathUtils.random(360f);
                for (int k = 0; k < n; k++) {
                    Bullet b = world.spawnEnemyBullet(ball.pos.x, ball.pos.y, start + 360f * k / n, 1.7f * diff);
                    b.color.set(RING_BULLET);
                }
                world.particles().burst(ball.pos.x, ball.pos.y, 10, 0.3f, 1.4f, 0.4f, 0.05f, FLUFF);
                world.sfx().play("pop", 0.5f);
            }
            fluffBalls.removeIndex(i);
            fluffTimers.removeIndex(i);
        }
    }

    private void toWander() {
        phase = Phase.WANDER;
        phaseTimer = furious ? 0.8f : 1.4f;
    }

    private void nextPattern(GameWorld world) {
        Phase[] order = furious
                ? new Phase[]{Phase.CROUCH, Phase.SPIRAL, Phase.PUFFS, Phase.FANS, Phase.CROUCH, Phase.RINGS}
                : new Phase[]{Phase.RINGS, Phase.SPIRAL, Phase.PUFFS, Phase.FANS};
        phase = order[patternIndex++ % order.length];
        shotTimer = 0.4f; // short tell before the first shot
        switch (phase) {
            case RINGS:
                shotsLeft = furious ? 5 : 3;
                break;
            case SPIRAL:
                phaseTimer = 3.2f;
                spiralAngle = MathUtils.random(360f);
                break;
            case FANS:
                shotsLeft = furious ? 6 : 4;
                break;
            case PUFFS:
                shotsLeft = furious ? 7 : 5;
                break;
            case CROUCH:
                phaseTimer = CROUCH;
                world.sfx().play("buzz", 0.5f);
                break;
            default:
                break;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float s = size();
        float wob = MathUtils.sin(time * 4f) * 0.015f * s;
        float cx = pos.x;
        float cy = pos.y + wob;
        float squash = 1f;
        if (phase == Phase.AIR) { // up he goes and down again; his shadow waits where he'll land
            float k = 1f - Math.max(0f, phaseTimer) / AIR;
            float h = MathUtils.sin(k * MathUtils.PI) * 2.2f;
            d.ellipse(b, landAt.x, landAt.y - 0.42f * s, radius * (0.5f + 0.6f * k), radius * 0.35f * (0.5f + 0.6f * k),
                    Color.BLACK, 0.25f + 0.2f * k);
            d.ring(b, landAt.x, landAt.y - 0.42f * s, STOMP_RADIUS, TELEGRAPH, 0.4f + 0.5f * k);
            cy += h;
        } else if (phase == Phase.CROUCH) {
            squash = 1f - 0.12f * (1f - Math.max(0f, phaseTimer) / CROUCH);
        } else if (phase == Phase.LAND) {
            squash = 1f - 0.15f * Math.max(0f, phaseTimer) / LAND;
        } else if (phase == Phase.TANTRUM) { // shaking with rage
            cx += MathUtils.random(-0.03f, 0.03f);
            cy += MathUtils.random(-0.03f, 0.03f);
        }
        boolean telling = (phase == Phase.RINGS || phase == Phase.FANS || phase == Phase.PUFFS)
                && shotTimer > 0 && shotTimer < 0.4f;
        float fury = furious ? Math.min(1f, (grow - 1f) / (FURY_GROW - 1f)) : 0f;
        Color body = fury > 0.5f ? FURY : FLUFF;
        Color shade = fury > 0.5f ? FURY_SHADE : FLUFF_SHADE;

        if (phase != Phase.AIR) {
            drawFeet(b, d, cx, feetY() + 0.03f, 0.18f * s);
        }
        float sy = s * squash;
        for (float[] p : PUFFS) {
            d.circle(b, cx + p[0] * s, cy + p[1] * sy, 0.17f * s, c(shade));
        }
        for (float[] p : PUFFS) {
            d.circle(b, cx + p[0] * 0.92f * s, cy + (p[1] * 0.92f + 0.015f) * sy, 0.15f * s, c(body));
        }
        d.circle(b, cx, cy + 0.03f * sy, 0.27f * s, c(body));

        // crown
        float top = cy + 0.38f * sy;
        d.rect(b, cx - 0.18f * s, top, 0.36f * s, 0.08f * s, c(CROWN));
        for (int i = -1; i <= 1; i++) {
            d.rotRect(b, cx + i * 0.13f * s, top + 0.09f * s, 0.08f * s, 0.08f * s, 45, c(CROWN), 1f);
        }
        d.circle(b, cx, top + 0.04f * s, 0.03f * s, GEM);

        // angry eyes (red pupils once he's furious)
        float lookX = MathUtils.cosDeg(aimAngle) * 0.015f * s;
        float lookY = MathUtils.sinDeg(aimAngle) * 0.015f * s;
        for (int i = -1; i <= 1; i += 2) {
            float ex = cx + i * 0.1f * s;
            float ey = cy + 0.08f * sy;
            d.circle(b, ex, ey, 0.055f * s, c(EYE_WHITE));
            d.circle(b, ex + lookX, ey + lookY, 0.03f * s, furious ? Color.RED : PUPIL);
            if (!happy) {
                d.rotRect(b, ex, ey + 0.065f * s, 0.11f * s, 0.025f * s, i * (furious ? 30f : 20f), PUPIL, 1f);
            }
        }
        // mouth: a flat line, a shouting O in phase two, a smile once he's made up
        if (happy) {
            d.circle(b, cx, cy - 0.05f * sy, 0.075f * s, PUPIL);
            d.circle(b, cx, cy - 0.025f * sy, 0.075f * s, c(body));
        } else if (furious) {
            d.circle(b, cx, cy - 0.07f * sy, 0.045f * s, PUPIL);
            d.circle(b, cx, cy - 0.08f * sy, 0.025f * s, FURY_SHADE);
        } else {
            d.rect(b, cx - 0.07f * s, cy - 0.07f * sy, 0.14f * s, 0.025f * s, PUPIL);
        }

        if (telling) {
            d.ring(b, cx, cy, radius * (1.6f - shotTimer), TELEGRAPH, 0.6f);
        }
    }
}
