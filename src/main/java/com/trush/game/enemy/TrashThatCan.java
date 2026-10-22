package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * Town boss, designed by the owner's son: a trash can with eyes and a tongue. It hops around, and
 * when it attacks the lid flips open and garbage flies out:
 * <ul>
 *   <li>garbage bombs - big bags that burst into bullets in every direction when they hit something
 *       (like the Gatling Gull's rockets);</li>
 *   <li>a trash wall - a line of bullets right across the room with no gap: roll over it;</li>
 *   <li>a wavy diagonal line - also gapless, so roll again.</li>
 * </ul>
 * Walls and the diagonal are telegraphed (warning line / dotted path) before they appear.
 * Enraged: junk sprays on every landing and one more wall.
 */
public class TrashThatCan extends Enemy {

    private enum Phase { HOP, BOMBS, WALL, DIAGONAL }

    private static final Color CAN = Color.valueOf("7f8c7a");
    private static final Color CAN_DARK = Color.valueOf("5a6656");
    private static final Color CAN_LIGHT = Color.valueOf("a3b09c");
    private static final Color TONGUE = Color.valueOf("e8708a");
    private static final Color EYE = Color.valueOf("f2e86d");
    private static final Color BAG = Color.valueOf("4a5a3a");
    private static final Color BANANA = Color.valueOf("f2d23a");
    private static final Color[] JUNK = {
            Color.valueOf("8a6a3a"), Color.valueOf("c0392b"), Color.valueOf("6fbf4a"), Color.valueOf("d8d0c0"),
    };

    private static final float HOP_TIME = 0.4f;
    private static final float LINE_SPACING = 0.19f;
    /** Warning shown where a wall / the diagonal will appear. */
    private static final float WARN_TIME = 0.5f;
    private static final float DIAGONAL_WARN = 0.75f;
    private static final float WALL_SPEED = 1.5f;
    private static final float DIAGONAL_SPEED = 1.35f;

    private Phase phase = Phase.HOP;
    private float phaseTimer = 1.2f;
    private float shotTimer;
    private int shotsLeft;
    private int hopsLeft = 2;
    private int patternIndex;
    private boolean enraged;
    private float time;
    private float hopTime;
    private float lidOpen;
    private final Vector2 target = new Vector2();
    private final Vector2 huntTarget = new Vector2();
    private final Vector2 hopFrom = new Vector2();
    private final Vector2 tmp = new Vector2();
    private float warnTimer;
    private boolean wallVertical;
    private float wallLine;
    private float wallDir;
    private float diagLx;
    private float diagLy;
    private float diagNx;
    private float diagNy;
    private final Vector2 diagOrigin = new Vector2();
    /** Garbage bombs in flight: they burst when they hit something. */
    private final Array<Bullet> bombs = new Array<>();

    public TrashThatCan(float x, float y, int tier) {
        super(x, y, 480f + 160f * tier);
        radius = 0.4f;
        speed = 2.8f;
        score = 500 + 250 * tier;
        boss = true;
        target.set(x, y);
    }

    @Override
    public String bossName() {
        return Boss.TRASH_CAN.title;
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
        float diff = world.getDifficulty();
        phaseTimer -= delta;
        shotTimer -= delta;
        lidOpen = Math.max(0f, lidOpen - delta * 2.5f);
        updateBombs(world, diff);

        switch (phase) {
            case HOP:
                hop(delta, world, diff);
                break;
            case BOMBS:
                if (shotTimer <= 0 && shotsLeft > 0) {
                    shotTimer = 0.5f;
                    shotsLeft--;
                    Bullet b = shoot(world, aimAngle + MathUtils.random(-8f, 8f), 2.5f * diff);
                    b.radius = 0.16f;
                    b.life = 6f;
                    b.color.set(BAG);
                    bombs.add(b);
                    lidOpen = 1f;
                    world.sfx().play("enemy_shoot", 1f);
                }
                if (shotsLeft <= 0 && shotTimer <= 0) {
                    toHop();
                }
                break;
            case WALL:
                // walls are spaced so a dodge roll (and its cooldown) is over before the next one arrives
                if (warnTimer > 0) {
                    warnTimer -= delta;
                    if (warnTimer <= 0) {
                        trashWall(world, diff);
                        shotsLeft--;
                        shotTimer = enraged ? 1.1f : 1.4f; // a roll + its cooldown takes 0.6 s
                    }
                } else if (shotTimer <= 0) {
                    if (shotsLeft > 0) {
                        aimWall(world);
                    } else {
                        toHop();
                    }
                }
                break;
            case DIAGONAL:
                if (phaseTimer <= 0) {
                    wavyDiagonal(world, diff);
                    toHop();
                }
                break;
        }
    }

    /** Short hops toward a spot (or around cover to see the player); a puff of dust on landing. */
    private void hop(float delta, GameWorld world, float diff) {
        boolean hunting = huntForSight(delta, world, huntTarget);
        if (hopTime > 0) {
            hopTime -= delta;
            float k = 1f - Math.max(0f, hopTime) / HOP_TIME;
            pos.set(hopFrom).lerp(target, k);
            if (hopTime <= 0) {
                world.particles().burst(pos.x, feetY(), 8, 0.3f, 1.2f, 0.35f, 0.05f, CAN_LIGHT);
                world.shake(0.08f);
                if (enraged) {
                    junkSpray(world, diff);
                }
            }
            return;
        }
        if (phaseTimer > 0) {
            return; // resting between hops
        }
        if (hopsLeft <= 0) {
            nextPattern(world);
            return;
        }
        hopsLeft--;
        phaseTimer = enraged ? 0.35f : 0.55f;
        hopFrom.set(pos);
        if (hunting) {
            target.set(huntTarget);
        } else {
            // hop a little way, keeping some distance from the player
            float a = MathUtils.random(360f);
            target.set(pos).add(MathUtils.cosDeg(a) * 1.1f, MathUtils.sinDeg(a) * 1.1f);
            Arena.clamp(target, radius + 0.3f);
            if (target.dst(world.getPlayer().centerX(), world.getPlayer().hitboxY()) < 1.4f) {
                target.set(hopFrom);
            }
        }
        if (target.dst(hopFrom) > 1.6f) { // hunting: one hop covers at most 1.6
            tmp.set(target).sub(hopFrom).nor().scl(1.6f);
            target.set(hopFrom).add(tmp);
        }
        hopTime = HOP_TIME;
    }

    /** Junk flung toward the player (enraged, on every landing). */
    private void junkSpray(GameWorld world, float diff) {
        lidOpen = 1f;
        for (int i = 0; i < 5; i++) {
            Bullet b = shoot(world, aimAngle + MathUtils.random(-35f, 35f), MathUtils.random(1.4f, 2.2f) * diff);
            b.color.set(JUNK[i % JUNK.length]);
        }
    }

    private void updateBombs(GameWorld world, float diff) {
        for (int i = bombs.size - 1; i >= 0; i--) {
            Bullet b = bombs.get(i);
            if (b.alive) continue;
            bombs.removeIndex(i);
            // blanks and a freshly hit player get no burst; everything else (walls, furniture...) does
            if (world.getBlankRing() >= 0 || world.getPlayer().isInvulnerable()) continue;
            // step back out of whatever it hit, then splash bullets everywhere
            tmp.set(b.vel).nor().scl(-0.22f).add(b.pos);
            Arena.clamp(tmp, 0.08f);
            int n = enraged ? 16 : 12;
            float off = MathUtils.random(360f);
            for (int k = 0; k < n; k++) {
                Bullet s = world.spawnEnemyBullet(tmp.x, tmp.y, off + 360f * k / n, 2.0f * diff);
                s.color.set(k % 3 == 0 ? BANANA : BAG);
            }
            world.particles().burst(tmp.x, tmp.y, 12, 0.4f, 1.6f, 0.4f, 0.05f, BAG);
            world.sfx().play("break", 0.8f);
        }
    }

    /** Picks where the next trash wall goes; a warning line shows there until it spawns. */
    private void aimWall(GameWorld world) {
        float px = world.getPlayer().centerX();
        float py = world.getPlayer().hitboxY();
        wallVertical = Math.abs(px - pos.x) > Math.abs(py - pos.y) * 1.4f; // player is to the side
        wallLine = wallVertical ? pos.x : pos.y;
        wallDir = wallVertical ? (px > pos.x ? 0f : 180f) : (py > pos.y ? 90f : 270f);
        warnTimer = WARN_TIME;
        lidOpen = 0.6f;
    }

    /** A full line of bullets across the room, rolling toward the player - no gap, so roll over it. */
    private void trashWall(GameWorld world, float diff) {
        lidOpen = 1f;
        if (wallVertical) {
            for (float y = Arena.BOTTOM + 0.1f; y < Arena.TOP - 0.05f; y += LINE_SPACING) {
                world.spawnEnemyBullet(wallLine, y, wallDir, WALL_SPEED * diff).color.set(JUNK[0]);
            }
        } else {
            for (float x = Arena.LEFT + 0.1f; x < Arena.RIGHT - 0.05f; x += LINE_SPACING) {
                world.spawnEnemyBullet(x, wallLine, wallDir, WALL_SPEED * diff).color.set(JUNK[0]);
            }
        }
        world.sfx().play("enemy_shoot", 1f);
        world.shake(0.15f);
    }

    /** Locks the diagonal line's position (shown as a dotted warning until it spawns). */
    private void aimDiagonal(GameWorld world) {
        float px = world.getPlayer().centerX();
        float py = world.getPlayer().hitboxY();
        // line along one diagonal, moving along the other one toward the player
        diagLx = 0.7071f;
        diagLy = MathUtils.randomBoolean() ? 0.7071f : -0.7071f;
        diagNx = -diagLy;
        diagNy = diagLx;
        if ((px - pos.x) * diagNx + (py - pos.y) * diagNy < 0) {
            diagNx = -diagNx;
            diagNy = -diagNy;
        }
        diagOrigin.set(pos);
    }

    /** Position of the i-th bullet of the wavy diagonal into {@code out}; false if it's outside the room. */
    private boolean diagonalPoint(float s, Vector2 out) {
        float wave = MathUtils.sin(s * 2.4f) * 0.28f;
        out.set(diagOrigin.x + diagLx * s + diagNx * wave, diagOrigin.y + diagLy * s + diagNy * wave);
        return Arena.contains(out.x, out.y, 0.06f);
    }

    /** A long wavy diagonal line of bullets sweeping toward the player - gapless again. */
    private void wavyDiagonal(GameWorld world, float diff) {
        float moveAngle = MathUtils.atan2Deg360(diagNy, diagNx);
        lidOpen = 1f;
        for (float s = -8f; s <= 8f; s += LINE_SPACING) {
            if (diagonalPoint(s, tmp)) {
                world.spawnEnemyBullet(tmp.x, tmp.y, moveAngle, DIAGONAL_SPEED * diff).color.set(BANANA);
            }
        }
        world.sfx().play("enemy_shoot", 1f);
        world.shake(0.15f);
    }

    /** Faint red dots where the next wall / the diagonal will appear, getting stronger. */
    private void drawWarnings(SpriteBatch b, Draw d) {
        if (phase == Phase.WALL && warnTimer > 0) {
            float a = 0.2f + 0.5f * (1f - warnTimer / WARN_TIME);
            if (wallVertical) {
                for (float y = Arena.BOTTOM + 0.1f; y < Arena.TOP - 0.05f; y += LINE_SPACING) {
                    d.circle(b, wallLine, y, 0.04f, TELEGRAPH, a);
                }
            } else {
                for (float x = Arena.LEFT + 0.1f; x < Arena.RIGHT - 0.05f; x += LINE_SPACING) {
                    d.circle(b, x, wallLine, 0.04f, TELEGRAPH, a);
                }
            }
        } else if (phase == Phase.DIAGONAL && phaseTimer > 0) {
            float a = 0.2f + 0.5f * (1f - phaseTimer / DIAGONAL_WARN);
            for (float s = -8f; s <= 8f; s += LINE_SPACING) {
                if (diagonalPoint(s, tmp)) {
                    d.circle(b, tmp.x, tmp.y, 0.04f, TELEGRAPH, a);
                }
            }
        }
    }

    private void toHop() {
        phase = Phase.HOP;
        hopsLeft = enraged ? 1 : 2;
        phaseTimer = 0.4f;
    }

    private void nextPattern(GameWorld world) {
        Phase[] order = {Phase.BOMBS, Phase.WALL, Phase.DIAGONAL};
        phase = order[patternIndex++ % order.length];
        shotTimer = 0.45f; // lid rattles before the first shot
        switch (phase) {
            case BOMBS:
                shotsLeft = enraged ? 4 : 3;
                break;
            case WALL:
                shotsLeft = enraged ? 4 : 3;
                shotTimer = 0.2f;
                break;
            case DIAGONAL:
                phaseTimer = DIAGONAL_WARN;
                aimDiagonal(world);
                break;
            default:
                break;
        }
        lidOpen = 0.4f;
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        drawWarnings(b, d);
        float jump = hopTime > 0 ? MathUtils.sin((1f - hopTime / HOP_TIME) * MathUtils.PI) * 0.25f : 0f;
        float rattle = (phase != Phase.HOP && shotTimer > 0 && shotTimer < 0.45f) ? MathUtils.random(-0.01f, 0.01f) : 0f;
        float cx = pos.x + rattle;
        float bottom = feetY() + jump;
        float w = 0.62f;
        float h = 0.6f;
        float left = cx - w / 2f;
        float face = facingRight() ? 1 : -1;

        // can with ridges
        d.rect(b, left, bottom, w, h, c(CAN));
        d.rect(b, left, bottom, 0.07f, h, c(CAN_DARK));
        for (float x = left + 0.14f; x < left + w - 0.05f; x += 0.11f) {
            d.rect(b, x, bottom + 0.05f, 0.025f, h - 0.12f, c(CAN_DARK));
        }
        d.rect(b, left - 0.03f, bottom + h - 0.06f, w + 0.06f, 0.06f, c(CAN_LIGHT)); // rim
        // eyes peeking out under the lid (wider when it's open)
        float open = 0.04f + lidOpen * 0.12f;
        d.rect(b, left + 0.03f, bottom + h, w - 0.06f, open, Color.BLACK);
        Color eye = enraged ? Color.RED : EYE;
        d.circle(b, cx - 0.1f + face * 0.05f, bottom + h + open * 0.5f, 0.045f, eye);
        d.circle(b, cx + 0.1f + face * 0.05f, bottom + h + open * 0.5f, 0.045f, eye);
        // lid, hinged at the back
        float lidY = bottom + h + open;
        d.rotRect(b, cx, lidY + 0.03f, w + 0.08f, 0.07f, lidOpen * 25f * -face, c(CAN_LIGHT), 1f);
        d.rect(b, cx - 0.06f, lidY + 0.06f, 0.12f, 0.05f, c(CAN_DARK)); // handle
        // tongue hanging over the rim
        float tx = cx + face * 0.1f;
        float wag = MathUtils.sin(time * 6f) * 0.015f;
        d.rect(b, tx - 0.05f + wag, bottom + h - 0.2f, 0.1f, 0.2f, TONGUE);
        d.circle(b, tx + wag, bottom + h - 0.2f, 0.05f, TONGUE);
        // flies buzzing around
        for (int i = 0; i < 3; i++) {
            float a = time * (4f + i) + i * 2.1f;
            d.circle(b, cx + MathUtils.cos(a) * 0.45f, bottom + h + 0.2f + MathUtils.sin(a * 1.7f) * 0.15f, 0.018f, Color.BLACK);
        }
    }
}
