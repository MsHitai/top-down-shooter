package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * The final boss (the game is called Enter the Fluffy after all) - for now he only rules endless mode's boss waves. Cycles through bullet patterns:
 * rings, spirals, aimed fans and (below half health) summons minions and gets faster.
 */
public class FluffyKing extends Enemy {

    private enum Phase { WANDER, RINGS, SPIRAL, FANS, SUMMON }

    private static final Color FLUFF = Color.valueOf("ffd6e8");
    private static final Color FLUFF_SHADE = Color.valueOf("f5a3c7");
    private static final Color CROWN = Color.valueOf("ffc83d");
    private static final Color GEM = Color.valueOf("e84393");
    private static final Color RING_BULLET = Color.valueOf("ff5e8a");
    private static final Color SPIRAL_BULLET = Color.valueOf("ff9f1a");
    private static final Color FAN_BULLET = Color.valueOf("ff3b3b");

    private static final float[][] PUFFS = {
            {-0.28f, 0.05f}, {0.28f, 0.05f}, {-0.2f, 0.24f}, {0.2f, 0.24f},
            {-0.2f, -0.17f}, {0.2f, -0.17f}, {0f, 0.3f}, {0f, -0.22f},
    };

    private Phase phase = Phase.WANDER;
    private float phaseTimer = 1.5f;
    private float shotTimer;
    private int shotsLeft;
    private float spiralAngle;
    private boolean enraged;
    private boolean summoned;
    private int patternIndex;
    private float time;
    private final Vector2 target = new Vector2();
    private final Vector2 step = new Vector2();

    public FluffyKing(float x, float y, int tier) {
        super(x, y, 420f + 160f * tier);
        radius = 0.4f;
        speed = 0.6f;
        score = 500 + 250 * tier;
        boss = true;
        target.set(x, y);
    }

    @Override
    public String bossName() {
        return Boss.FLUFFY_KING.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    @Override
    public float feetY() {
        return pos.y - 0.42f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        if (!enraged && hp < maxHp * 0.5f) {
            enraged = true;
            world.shake(0.6f);
            world.sfx().play("boss", 1f);
        }
        float rage = enraged ? 1.35f : 1f;
        float diff = world.getDifficulty();

        // hiding behind a pillar doesn't work for long: he walks to where he can see you
        boolean hunting = huntForSight(delta, world, target);
        // otherwise a slow drift toward a random point the whole fight
        if (!hunting && pos.dst2(target) < 0.05f) {
            target.set(MathUtils.random(Arena.LEFT + 1.2f, Arena.RIGHT - 1.2f),
                    MathUtils.random(Arena.CENTER_Y, Arena.TOP - 0.7f));
        }
        float moveSpeed = hunting ? speed * 2f : phase == Phase.WANDER ? speed * 1.6f : speed * 0.5f;
        Vector2 step = this.step.set(target).sub(pos);
        if (step.len() > 0.01f) {
            step.nor().scl(moveSpeed * rage * delta);
            pos.add(step);
            walkTime += delta;
        }

        phaseTimer -= delta;
        shotTimer -= delta;

        switch (phase) {
            case WANDER:
                if (phaseTimer <= 0) {
                    nextPattern();
                }
                break;
            case RINGS:
                if (shotTimer <= 0 && shotsLeft > 0) {
                    int n = enraged ? 28 : 22;
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
                    int arms = enraged ? 5 : 3;
                    for (int i = 0; i < arms; i++) {
                        Bullet b = shoot(world, spiralAngle + 360f * i / arms, 1.9f * diff);
                        b.color.set(SPIRAL_BULLET);
                        if (enraged && i % 2 == 0) {
                            b.curve = -20f;
                        }
                    }
                    spiralAngle += 11f;
                    shotTimer = 0.09f / rage;
                    world.sfx().play("enemy_shoot", 0.4f);
                }
                if (phaseTimer <= 0) {
                    toWander();
                }
                break;
            case FANS:
                if (shotTimer <= 0 && shotsLeft > 0) {
                    int n = enraged ? 9 : 7;
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
            case SUMMON:
                if (phaseTimer <= 0) {
                    for (int i = -1; i <= 1; i += 2) {
                        world.spawnEnemy(new BulletKin(pos.x + i * 0.9f, pos.y - 0.5f));
                    }
                    toWander();
                }
                break;
        }
    }

    private void toWander() {
        phase = Phase.WANDER;
        phaseTimer = enraged ? 0.8f : 1.4f;
    }

    private void nextPattern() {
        if (enraged && !summoned) {
            summoned = true;
            phase = Phase.SUMMON;
            phaseTimer = 0.8f;
            return;
        }
        Phase[] order = {Phase.RINGS, Phase.SPIRAL, Phase.FANS};
        phase = order[patternIndex++ % order.length];
        shotTimer = 0.4f; // short tell before the first shot
        switch (phase) {
            case RINGS:
                shotsLeft = enraged ? 5 : 3;
                break;
            case SPIRAL:
                phaseTimer = 3.2f;
                spiralAngle = MathUtils.random(360f);
                break;
            case FANS:
                shotsLeft = enraged ? 6 : 4;
                break;
            default:
                break;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float wob = MathUtils.sin(time * 4f) * 0.015f;
        float cx = pos.x;
        float cy = pos.y + wob;
        boolean telling = phase != Phase.WANDER && shotTimer > 0 && shotTimer < 0.4f && phase != Phase.SPIRAL;

        drawFeet(b, d, cx, feetY() + 0.03f, 0.18f);
        for (float[] p : PUFFS) {
            d.circle(b, cx + p[0], cy + p[1], 0.17f, c(FLUFF_SHADE));
        }
        for (float[] p : PUFFS) {
            d.circle(b, cx + p[0] * 0.92f, cy + p[1] * 0.92f + 0.015f, 0.15f, c(FLUFF));
        }
        d.circle(b, cx, cy + 0.03f, 0.27f, c(FLUFF));

        // crown
        float top = cy + 0.38f;
        d.rect(b, cx - 0.18f, top, 0.36f, 0.08f, c(CROWN));
        for (int i = -1; i <= 1; i++) {
            d.rotRect(b, cx + i * 0.13f, top + 0.09f, 0.08f, 0.08f, 45, c(CROWN), 1f);
        }
        d.circle(b, cx, top + 0.04f, 0.03f, GEM);

        // angry eyes
        float lookX = MathUtils.cosDeg(aimAngle) * 0.015f;
        float lookY = MathUtils.sinDeg(aimAngle) * 0.015f;
        for (int i = -1; i <= 1; i += 2) {
            float ex = cx + i * 0.1f;
            float ey = cy + 0.08f;
            d.circle(b, ex, ey, 0.055f, c(EYE_WHITE));
            d.circle(b, ex + lookX, ey + lookY, 0.03f, enraged ? Color.RED : PUPIL);
            d.rotRect(b, ex, ey + 0.065f, 0.11f, 0.025f, i * -20f, PUPIL, 1f);
        }
        // mouth
        d.rect(b, cx - 0.07f, cy - 0.07f, 0.14f, 0.025f, PUPIL);

        if (telling) {
            d.ring(b, cx, cy, radius * (1.6f - shotTimer), TELEGRAPH, 0.6f);
        }
    }
}
