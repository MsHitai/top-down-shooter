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
 * The village's horror-movie boss: a hulking masked farmhand with a sputtering chainsaw.
 * Unlike the other bosses he comes for you: he stalks you (touching him hurts) while his saw
 * coughs out sprays of bullets, revs up and lunges after you - steering as you move, so it has
 * to be dodge-rolled - leaving hanging sawdust behind, and throws saw blades that boomerang back.
 */
public class Harvestman extends Enemy {

    private enum Phase { STALK, REV, LUNGE, RECOVER, WINDUP_THROW }

    private static final Color OVERALLS = Color.valueOf("4a5a6e");
    private static final Color OVERALLS_DARK = Color.valueOf("323e4e");
    private static final Color APRON = Color.valueOf("d8cfc0");
    private static final Color STAIN = Color.valueOf("8e2a20");
    private static final Color MASK = Color.valueOf("e8d9b8");
    private static final Color MASK_DARK = Color.valueOf("b8a682");
    private static final Color HAIR = Color.valueOf("3b2a20");
    private static final Color BOOTS = Color.valueOf("3a2618");
    private static final Color STITCH = Color.valueOf("2b1d14");
    private static final Color MOTOR = Color.valueOf("e07a2a");
    private static final Color STEEL = Color.valueOf("c9ccd6");
    private static final Color EXHAUST = Color.valueOf("5a5560");
    private static final Color SAWDUST = Color.valueOf("c8a070");

    private static final float LUNGE_TIME = 1.3f;

    private Phase phase = Phase.STALK;
    private float phaseTimer = 2.5f;
    private float exhaustTimer = 1.5f;
    private float shotTimer;
    private float sawSoundTimer;
    private int lungesLeft;
    private boolean throwNext;
    private boolean enraged;
    private float time;
    private final Vector2 target = new Vector2();
    private final Vector2 step = new Vector2();
    private final Vector2 lungeDir = new Vector2();

    public Harvestman(float x, float y, int tier) {
        super(x, y, 460f + 160f * tier);
        radius = 0.4f;
        speed = 0.85f;
        score = 500 + 250 * tier;
        boss = true;
    }

    @Override
    public String bossName() {
        return Boss.HARVESTMAN.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    @Override
    public float feetY() {
        return pos.y - 0.45f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        if (!enraged && hp < maxHp * 0.5f) {
            enraged = true;
            world.shake(0.6f);
            world.sfx().play("chainsaw", 1f);
        }
        float rage = enraged ? 1.3f : 1f;
        float diff = world.getDifficulty();
        phaseTimer -= delta;
        shotTimer -= delta;

        switch (phase) {
            case STALK:
                stalk(delta, world, rage, diff);
                if (phaseTimer <= 0) {
                    if (throwNext) {
                        phase = Phase.WINDUP_THROW;
                        phaseTimer = 0.45f;
                    } else {
                        startRev(world);
                        lungesLeft = enraged ? 2 : 1;
                    }
                    throwNext = !throwNext;
                }
                break;
            case REV:
                if (MathUtils.randomBoolean(0.4f)) { // exhaust smoke while revving
                    world.particles().spawn(pos.x - (facingRight() ? 0.2f : -0.2f), pos.y + 0.1f,
                            MathUtils.random(-0.3f, 0.3f), 0.6f, 0.4f, 0.05f, EXHAUST, 1f);
                }
                if (phaseTimer <= 0) {
                    phase = Phase.LUNGE;
                    phaseTimer = LUNGE_TIME;
                    sawSoundTimer = 0.4f; // the rev sound is still playing
                    lungeDir.set(MathUtils.cosDeg(aimAngle), MathUtils.sinDeg(aimAngle));
                }
                break;
            case LUNGE:
                lunge(delta, world, rage);
                break;
            case RECOVER: // catching his breath: the moment to punish him
                if (phaseTimer <= 0) {
                    if (lungesLeft > 0) {
                        startRev(world);
                    } else {
                        phase = Phase.STALK;
                        phaseTimer = enraged ? 1.6f : 2.4f;
                    }
                }
                break;
            case WINDUP_THROW:
                if (phaseTimer <= 0) {
                    throwBlades(world, diff);
                    phase = Phase.STALK;
                    phaseTimer = enraged ? 1.6f : 2.4f;
                }
                break;
        }
    }

    /** Walks straight at the player (or around cover to find them); the saw sputters bullets. */
    private void stalk(float delta, GameWorld world, float rage, float diff) {
        boolean hunting = huntForSight(delta, world, target);
        if (!hunting) {
            target.set(world.getPlayer().centerX(), world.getPlayer().hitboxY());
        }
        step.set(target).sub(pos);
        if (step.len() > 0.05f) {
            step.nor().scl(speed * rage * delta);
            pos.add(step);
            walkTime += delta;
        }
        exhaustTimer -= delta;
        if (exhaustTimer <= 0 && canSeePlayer) {
            exhaustTimer = (enraged ? 0.9f : 1.3f) / diff;
            int n = enraged ? 7 : 5;
            float fan = 40f;
            for (int i = 0; i < n; i++) {
                Bullet b = shoot(world, aimAngle - fan / 2f + fan * i / (n - 1), 2.4f * diff);
                b.color.set(EXHAUST);
            }
            world.sfx().play("enemy_shoot", 0.8f);
        }
    }

    private void startRev(GameWorld world) {
        phase = Phase.REV;
        phaseTimer = enraged ? 0.5f : 0.7f;
        world.sfx().play("chainsaw", 1f);
    }

    /** Sprints at the player, turning after them at a limited rate; drops lingering sawdust. */
    private void lunge(float delta, GameWorld world, float rage) {
        float wanted = aimAngle;
        float current = lungeDir.angleDeg();
        float diffAngle = ((wanted - current + 540f) % 360f) - 180f;
        float maxTurn = (enraged ? 150f : 110f) * delta;
        lungeDir.rotateDeg(MathUtils.clamp(diffAngle, -maxTurn, maxTurn));
        pos.mulAdd(lungeDir, 3.0f * rage * delta);
        walkTime += delta * 2.5f;

        if (shotTimer <= 0) { // sawdust that slows down and hangs in the air for a moment
            shotTimer = 0.06f;
            Bullet b = world.spawnEnemyBullet(pos.x, feetY() + 0.15f, MathUtils.random(360f), 0.7f);
            b.accel = -1.4f;
            b.minSpeed = 0.02f;
            b.life = 1.8f;
            b.radius = 0.05f;
            b.color.set(SAWDUST);
        }
        sawSoundTimer -= delta;
        if (sawSoundTimer <= 0) { // the rev sound is 0.7 s long: replay it at a steady pace, not every frame
            sawSoundTimer = 0.5f;
            world.sfx().play("chainsaw", 0.6f);
        }
        if (phaseTimer <= 0 || !Arena.now.contains(pos.x, pos.y, radius + 0.02f)) {
            lungesLeft--;
            phase = Phase.RECOVER;
            phaseTimer = enraged ? 0.45f : 0.7f;
            world.shake(0.2f);
        }
    }

    /** Spinning saw blades that fly out in a loop and come back like boomerangs. */
    private void throwBlades(GameWorld world, float diff) {
        int pairs = enraged ? 2 : 1;
        for (int p = 0; p < pairs; p++) {
            for (int side = -1; side <= 1; side += 2) {
                Bullet b = shoot(world, aimAngle + side * (15f + p * 25f), 2.6f * diff);
                b.curve = side * 150f;
                b.radius = 0.13f;
                b.life = 2.6f;
                b.color.set(STEEL);
            }
        }
        world.sfx().play("chainsaw", 0.8f);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        boolean revving = phase == Phase.REV;
        float jitter = revving ? MathUtils.random(-0.015f, 0.015f) : 0f;
        float cx = pos.x + jitter;
        float bottom = feetY();
        float face = facingRight() ? 1 : -1;
        float panting = phase == Phase.RECOVER ? MathUtils.sin(time * 18f) * 0.015f : 0f;

        if (revving) {
            d.ring(b, cx, pos.y, radius * (1.2f + 0.2f * MathUtils.sin(time * 40f)), TELEGRAPH, 0.6f);
        }
        // boots
        float stride = MathUtils.sin(walkTime * 10f) * 0.03f;
        d.rect(b, cx - 0.17f, bottom + stride, 0.14f, 0.1f, c(BOOTS));
        d.rect(b, cx + 0.03f, bottom - stride, 0.14f, 0.1f, c(BOOTS));
        // overalls and a stained apron
        d.rect(b, cx - 0.24f, bottom + 0.08f, 0.48f, 0.5f + panting, c(OVERALLS));
        d.rect(b, cx - 0.24f, bottom + 0.08f, 0.06f, 0.5f + panting, c(OVERALLS_DARK));
        d.rect(b, cx - 0.16f, bottom + 0.12f, 0.32f, 0.36f, c(APRON));
        d.circle(b, cx - 0.06f, bottom + 0.3f, 0.04f, c(STAIN));
        d.circle(b, cx + 0.07f, bottom + 0.2f, 0.03f, c(STAIN));
        d.rect(b, cx - 0.2f, bottom + 0.56f + panting, 0.4f, 0.06f, c(OVERALLS_DARK)); // shoulders
        // masked head
        float hy = bottom + 0.78f + panting;
        d.circle(b, cx, hy, 0.18f, c(MASK_DARK));
        d.circle(b, cx + face * 0.01f, hy + 0.01f, 0.165f, c(MASK));
        d.rect(b, cx - 0.15f, hy + 0.1f, 0.3f, 0.07f, c(HAIR));
        Color eye = enraged ? Color.RED : STITCH;
        d.circle(b, cx + face * 0.03f - 0.05f, hy + 0.02f, 0.03f, eye);
        d.circle(b, cx + face * 0.03f + 0.06f, hy + 0.02f, 0.03f, eye);
        d.rect(b, cx + face * 0.03f - 0.07f, hy - 0.08f, 0.14f, 0.015f, STITCH); // stitched mouth
        for (int i = 0; i < 4; i++) {
            d.rect(b, cx + face * 0.03f - 0.06f + i * 0.04f, hy - 0.1f, 0.01f, 0.05f, STITCH);
        }
        // the chainsaw, held toward the player
        float hx = cx + face * 0.2f;
        float hyHand = bottom + 0.38f;
        d.rect(b, hx - 0.09f, hyHand - 0.07f, 0.18f, 0.14f, c(MOTOR));
        float bx = hx + MathUtils.cosDeg(aimAngle) * 0.08f;
        float by = hyHand + MathUtils.sinDeg(aimAngle) * 0.08f;
        d.line(b, bx, by, 0.42f, 0.085f, aimAngle, c(STEEL));
        float spin = (phase == Phase.LUNGE || revving) ? (time * 3f) % 0.06f : 0f;
        for (float t = 0.02f + spin; t < 0.42f; t += 0.06f) { // teeth
            d.circle(b, bx + MathUtils.cosDeg(aimAngle) * t, by + MathUtils.sinDeg(aimAngle) * t, 0.014f, STITCH);
        }
    }
}
