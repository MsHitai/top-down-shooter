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
 * The village's other boss: a huge scarecrow. Where the Fluffy King fills the room with rings and
 * spirals, the Haymaker makes you move: telegraphed charges that end in a slam, walls of straw
 * with a single gap, and crows that loop in from the sides. Enraged, hay rains from the ceiling.
 */
public class Haymaker extends Enemy {

    private enum Phase { WANDER, WINDUP, CHARGE, WALLS, CROWS }

    private static final Color STRAW = Color.valueOf("e6c35c");
    private static final Color STRAW_DARK = Color.valueOf("b08d2e");
    private static final Color SACK = Color.valueOf("c8a46a");
    private static final Color SACK_DARK = Color.valueOf("9c7a45");
    private static final Color SHIRT = Color.valueOf("b5543a");
    private static final Color PATCH = Color.valueOf("4f6fa8");
    private static final Color POLE = Color.valueOf("6e4020");
    private static final Color HAT = Color.valueOf("d9b54a");
    private static final Color STITCH = Color.valueOf("2b1d14");
    private static final Color CROW = Color.valueOf("2b2b3a");

    private static final float WINDUP = 0.75f;
    private static final float CHARGE_SPEED = 4.4f;
    private static final float CHARGE_MAX = 4.5f;

    private Phase phase = Phase.WANDER;
    private float phaseTimer = 1.5f;
    private float shotTimer;
    private int shotsLeft;
    private int patternIndex;
    private boolean enraged;
    private float time;
    private float rainTimer;
    private final Vector2 target = new Vector2();
    private final Vector2 step = new Vector2();
    private final Vector2 chargeDir = new Vector2();
    private final Vector2 lastPos = new Vector2();
    private float charged;
    private int gapSide;

    public Haymaker(float x, float y, int tier) {
        super(x, y, 440f + 160f * tier);
        radius = 0.42f;
        speed = 0.7f;
        score = 500 + 250 * tier;
        boss = true;
        target.set(x, y);
    }

    @Override
    public String bossName() {
        return Boss.HAYMAKER.title;
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
            world.sfx().play("boss", 1f);
        }
        float rage = enraged ? 1.3f : 1f;
        float diff = world.getDifficulty();

        if (enraged) { // hay falls from the ceiling the whole time
            rainTimer -= delta;
            if (rainTimer <= 0) {
                rainTimer = 0.32f / diff;
                Bullet b = world.spawnEnemyBullet(MathUtils.random(Arena.now.left + 0.2f, Arena.now.right - 0.2f),
                        Arena.now.top - 0.05f, 270f, 1.5f * diff);
                b.color.set(STRAW);
            }
        }

        if (phase != Phase.CHARGE && phase != Phase.WINDUP) {
            boolean hunting = huntForSight(delta, world, target);
            if (!hunting && pos.dst2(target) < 0.05f) {
                target.set(MathUtils.random(Arena.now.left + 1.2f, Arena.now.right - 1.2f),
                        MathUtils.random(Arena.now.centerY - 0.3f, Arena.now.top - 0.7f));
            }
            float moveSpeed = hunting ? speed * 2f : phase == Phase.WANDER ? speed * 1.4f : speed * 0.4f;
            step.set(target).sub(pos);
            if (step.len() > 0.01f) {
                step.nor().scl(moveSpeed * rage * delta);
                pos.add(step);
                walkTime += delta;
            }
        }

        phaseTimer -= delta;
        shotTimer -= delta;
        switch (phase) {
            case WANDER:
                if (phaseTimer <= 0) {
                    nextPattern();
                }
                break;
            case WINDUP:
                // track the player, then lock the direction a moment before the dash so it can be dodged
                if (phaseTimer > 0.25f) {
                    chargeDir.set(MathUtils.cosDeg(aimAngle), MathUtils.sinDeg(aimAngle));
                }
                if (phaseTimer <= 0) {
                    phase = Phase.CHARGE;
                    charged = 0;
                    shotTimer = 0;
                    lastPos.set(pos);
                    world.sfx().play("dodge", 1f);
                }
                break;
            case CHARGE:
                charge(delta, world, rage, diff);
                break;
            case WALLS:
                if (shotTimer <= 0 && shotsLeft > 0) {
                    strawWall(world, diff);
                    shotsLeft--;
                    shotTimer = 1.0f / rage;
                }
                if (shotsLeft <= 0 && shotTimer <= 0) {
                    toWander();
                }
                break;
            case CROWS:
                if (shotTimer <= 0 && shotsLeft > 0) {
                    crows(world, diff);
                    shotsLeft--;
                    shotTimer = 0.55f / rage;
                }
                if (shotsLeft <= 0 && shotTimer <= 0) {
                    toWander();
                }
                break;
        }
    }

    private void charge(float delta, GameWorld world, float rage, float diff) {
        // stuck against a wall or furniture (we got pushed back last frame) or far enough: slam
        boolean blocked = charged > 0.3f && pos.dst(lastPos) < CHARGE_SPEED * delta * 0.3f;
        if (blocked || charged >= CHARGE_MAX || !Arena.now.contains(pos.x, pos.y, radius + 0.02f)) {
            slam(world, diff);
            return;
        }
        lastPos.set(pos);
        float move = CHARGE_SPEED * rage * delta;
        pos.mulAdd(chargeDir, move);
        charged += move;
        walkTime += delta * 2f;
        // straw trail drifting off to both sides
        if (shotTimer <= 0) {
            shotTimer = 0.08f;
            float a = chargeDir.angleDeg();
            for (int side = -1; side <= 1; side += 2) {
                Bullet b = shoot(world, a + 90f * side + MathUtils.random(-15f, 15f), 0.9f * diff);
                b.color.set(STRAW_DARK);
            }
        }
    }

    private void slam(GameWorld world, float diff) {
        int n = enraged ? 20 : 14;
        float off = MathUtils.random(360f);
        for (int i = 0; i < n; i++) {
            Bullet b = shoot(world, off + 360f * i / n, 1.8f * diff);
            b.color.set(STRAW);
            b.radius = 0.075f;
        }
        world.shake(0.4f);
        world.sfx().play("break", 1f);
        world.particles().burst(pos.x, feetY(), 16, 0.5f, 1.8f, 0.5f, 0.05f, STRAW);
        shotsLeft--;
        if (shotsLeft > 0) {
            phase = Phase.WINDUP;
            phaseTimer = (enraged ? 0.55f : WINDUP);
        } else {
            toWander();
        }
    }

    /** A full-height wall of straw rolling toward the player, with one gap to slip through. */
    private void strawWall(GameWorld world, float diff) {
        float dir = world.getPlayer().centerX() >= pos.x ? 0f : 180f;
        float gapH = 0.95f;
        // gaps alternate low/high so you have to keep moving
        gapSide = 1 - gapSide;
        float gapY = gapSide == 0
                ? MathUtils.random(Arena.now.bottom + 0.6f, Arena.now.centerY)
                : MathUtils.random(Arena.now.centerY, Arena.now.top - 0.6f);
        float x = pos.x + (dir == 0f ? radius : -radius);
        for (float y = Arena.now.bottom + 0.12f; y < Arena.now.top - 0.05f; y += 0.24f) {
            if (Math.abs(y - gapY) < gapH / 2f) continue;
            Bullet b = world.spawnEnemyBullet(x, y, dir, 1.45f * diff);
            b.color.set(STRAW);
        }
        world.sfx().play("enemy_shoot", 1f);
    }

    /** Crows fly out to both sides and loop back in toward the player. */
    private void crows(GameWorld world, float diff) {
        int perSide = enraged ? 5 : 4;
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < perSide; i++) {
                float spread = 45f + i * 12f;
                Bullet b = shoot(world, aimAngle + side * spread, 2.1f * diff);
                b.curve = -side * 70f; // turn back toward the line to the player
                b.color.set(CROW);
            }
        }
        world.sfx().play("enemy_shoot", 0.9f);
    }

    private void toWander() {
        phase = Phase.WANDER;
        phaseTimer = enraged ? 0.9f : 1.5f;
    }

    private void nextPattern() {
        Phase[] order = {Phase.WINDUP, Phase.WALLS, Phase.CROWS};
        phase = order[patternIndex++ % order.length];
        shotTimer = 0.4f;
        switch (phase) {
            case WINDUP:
                shotsLeft = enraged ? 3 : 2; // number of charges
                phaseTimer = WINDUP;
                break;
            case WALLS:
                shotsLeft = enraged ? 4 : 3;
                break;
            case CROWS:
                shotsLeft = enraged ? 4 : 3;
                break;
            default:
                break;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float sway = phase == Phase.CHARGE ? 0 : MathUtils.sin(time * 2.5f) * 0.02f;
        float cx = pos.x;
        float bottom = feetY();

        if (phase == Phase.WINDUP) { // shows where the charge will go
            float pulse = 0.35f + 0.3f * MathUtils.sin(time * 30f);
            d.line(b, cx, pos.y, 3.5f, 0.05f, chargeDir.angleDeg(), TELEGRAPH);
            d.ring(b, cx, pos.y, radius * 1.3f, TELEGRAPH, pulse);
        }
        // pole and a single hopping foot
        float hop = phase == Phase.CHARGE ? Math.abs(MathUtils.sin(walkTime * 18f)) * 0.04f : 0f;
        d.rect(b, cx - 0.04f, bottom + hop, 0.08f, 0.4f, c(POLE));
        // arms: a crossbar with straw tufts
        float armY = bottom + 0.55f + hop;
        d.rect(b, cx - 0.5f, armY, 1.0f, 0.07f, c(POLE));
        for (int i = -1; i <= 1; i += 2) {
            float ax = cx + i * 0.52f;
            d.circle(b, ax, armY + 0.03f, 0.07f, c(STRAW));
            d.rotRect(b, ax + i * 0.05f, armY - 0.02f, 0.1f, 0.03f, i * 30f, c(STRAW_DARK), 1f);
        }
        // patched shirt
        d.rect(b, cx - 0.24f, bottom + 0.28f + hop, 0.48f, 0.36f, c(SHIRT));
        d.rect(b, cx + 0.04f, bottom + 0.34f + hop, 0.12f, 0.1f, c(PATCH));
        d.rect(b, cx - 0.24f, bottom + 0.28f + hop, 0.48f, 0.04f, c(STRAW_DARK));
        // sack head
        float hy = bottom + 0.82f + hop + sway;
        d.circle(b, cx + sway, hy, 0.21f, c(SACK_DARK));
        d.circle(b, cx + sway, hy + 0.01f, 0.19f, c(SACK));
        float look = facingRight() ? 0.03f : -0.03f;
        Color eye = enraged ? Color.RED : STITCH;
        d.rotRect(b, cx - 0.07f + look, hy + 0.04f, 0.07f, 0.07f, 45f, eye, 1f);
        d.rotRect(b, cx + 0.07f + look, hy + 0.04f, 0.07f, 0.07f, 45f, eye, 1f);
        // stitched grin
        d.rect(b, cx - 0.09f + look, hy - 0.08f, 0.18f, 0.02f, STITCH);
        for (int i = 0; i < 4; i++) {
            d.rect(b, cx - 0.08f + look + i * 0.05f, hy - 0.1f, 0.012f, 0.06f, STITCH);
        }
        // straw hat
        d.rect(b, cx - 0.3f + sway, hy + 0.13f, 0.6f, 0.06f, c(HAT));
        d.rect(b, cx - 0.14f + sway, hy + 0.18f, 0.28f, 0.14f, c(HAT));
        d.rect(b, cx - 0.14f + sway, hy + 0.19f, 0.28f, 0.03f, c(SHIRT));
    }
}
