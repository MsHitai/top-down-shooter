package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * Skyscraper boss: the Mega Mech, a giant security robot stomping round the roof.
 * <ul>
 *   <li>guns: laser bolts from its head and fans from the cannons on its arms, one arm then the other;</li>
 *   <li>jump: it leaps off the roof - a shadow grows where it'll land - and comes down with a crash
 *       and a shockwave ring with no gaps (roll through it);</li>
 *   <li>enraged: a turret folds out of its chest and keeps spraying spirals, whatever else it's doing.</li>
 * </ul>
 */
public class Mech extends Enemy {

    private enum Phase { WALK, GUNS, CROUCH, AIR, LAND }

    private static final Color ARMOR = Color.valueOf("c9cfd8");
    private static final Color ARMOR_DARK = Color.valueOf("8a929e");
    private static final Color FRAME = Color.valueOf("3a3f4a");
    private static final Color STRIPE = Color.valueOf("f2a03a");
    private static final Color VISOR = Color.valueOf("1e2230");
    private static final Color GLOW = Color.valueOf("4fe0ff");
    private static final Color BOLT = Color.valueOf("ff5a5a");
    private static final Color SHELL = Color.valueOf("ffd166");
    private static final Color WAVE = Color.valueOf("b8f0ff");

    private static final float CROUCH = 0.45f;
    private static final float AIR = 1.35f;
    private static final float LAND = 0.6f;
    private static final float STOMP_RADIUS = 0.95f;
    /** How high it's drawn at the top of its jump (it leaves the screen). */
    private static final float JUMP_HEIGHT = 6f;

    private Phase phase = Phase.WALK;
    private float phaseTimer = 2f;
    private int patternIndex;
    private boolean enraged;
    private float time;
    private final Vector2 target = new Vector2();
    private final Vector2 landAt = new Vector2();
    private final Vector2 jumpFrom = new Vector2();
    private final Vector2 step = new Vector2();
    // guns
    private int volleysLeft;
    private float volleyTimer;
    private int boltsLeft;
    private float boltTimer;
    private boolean leftArm;
    // chest turret
    private float turretOpen;
    private float turretTimer;
    private float turretAngle;

    public Mech(float x, float y, int tier) {
        super(x, y, 720f + 200f * tier);
        radius = 0.5f;
        speed = 0.75f;
        score = 700 + 250 * tier;
        boss = true;
    }

    @Override
    public String bossName() {
        return Boss.MECH.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    @Override
    public float feetY() {
        return pos.y - 0.55f;
    }

    /** Up in the air: out of reach. */
    @Override
    protected boolean isDodging() {
        return phase == Phase.AIR;
    }

    @Override
    protected boolean castsShadow() {
        return phase != Phase.AIR;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        knock.setZero();
        if (!enraged && hp < maxHp * 0.5f) {
            enraged = true;
            world.shake(0.6f);
            world.sfx().play("boss", 1f);
            world.particles().burst(pos.x, pos.y, 20, 0.4f, 2f, 0.5f, 0.05f, STRIPE);
        }
        float diff = world.getDifficulty();
        phaseTimer -= delta;
        updateTurret(delta, world, diff);

        switch (phase) {
            case WALK: {
                if (!huntForSight(delta, world, target)) {
                    float dist = distanceToPlayer(world);
                    if (dist > 3.2f) {
                        target.set(world.getPlayer().centerX(), world.getPlayer().hitboxY());
                    } else if (dist < 2f) {
                        target.set(pos).sub(world.getPlayer().centerX() - pos.x, world.getPlayer().hitboxY() - pos.y);
                    } else {
                        target.set(pos);
                    }
                }
                step.set(target).sub(pos);
                if (step.len() > 0.05f) {
                    step.nor().scl(speed * delta);
                    pos.add(step);
                    walkTime += delta;
                }
                if (phaseTimer <= 0) {
                    nextPattern(world);
                }
                break;
            }
            case GUNS:
                boltTimer -= delta;
                if (boltsLeft > 0 && boltTimer <= 0) { // the head laser
                    boltTimer = 0.11f;
                    boltsLeft--;
                    Bullet b = world.spawnEnemyBullet(pos.x, headY(), aimAngle, 4.4f * diff);
                    b.color.set(BOLT);
                    b.streak = 0.3f;
                    world.sfx().play("zap", 0.5f);
                }
                volleyTimer -= delta;
                if (volleysLeft > 0 && volleyTimer <= 0) { // arm cannons, one side then the other
                    volleyTimer = enraged ? 0.4f : 0.55f;
                    volleysLeft--;
                    leftArm = !leftArm;
                    float ax = pos.x + (leftArm ? -0.62f : 0.62f);
                    float ay = pos.y + 0.05f;
                    float a = MathUtils.atan2Deg360(world.getPlayer().hitboxY() - ay, world.getPlayer().centerX() - ax);
                    int n = enraged ? 7 : 5;
                    for (int i = 0; i < n; i++) {
                        world.spawnEnemyBullet(ax, ay, a - 30f + 60f * i / (n - 1), 2.5f * diff).color.set(SHELL);
                    }
                    world.sfx().play("shotgun", 0.6f);
                }
                if (boltsLeft <= 0 && volleysLeft <= 0 && volleyTimer <= 0) {
                    toWalk();
                }
                break;
            case CROUCH:
                if (phaseTimer <= 0) {
                    phase = Phase.AIR;
                    phaseTimer = AIR;
                    jumpFrom.set(pos);
                    landAt.set(world.getPlayer().centerX(), world.getPlayer().hitboxY() + 0.3f);
                    Arena.now.clamp(landAt, radius + 0.2f);
                    world.sfx().play("boom", 0.6f);
                    world.particles().burst(pos.x, feetY(), 16, 0.5f, 1.6f, 0.4f, 0.06f, Color.GRAY);
                }
                break;
            case AIR: {
                // the landing spot keeps creeping after the player for most of the jump
                if (phaseTimer > AIR * 0.35f) {
                    Player p = world.getPlayer();
                    landAt.lerp(step.set(p.centerX(), p.hitboxY() + 0.3f), Math.min(1f, delta * 1.6f));
                    Arena.now.clamp(landAt, radius + 0.2f);
                }
                float k = 1f - phaseTimer / AIR;
                pos.set(jumpFrom).lerp(landAt, k);
                if (phaseTimer <= 0) {
                    land(world, diff);
                }
                break;
            }
            case LAND:
                if (phaseTimer <= 0) {
                    toWalk();
                }
                break;
        }
    }

    private float headY() {
        return pos.y + 0.55f;
    }

    private void toWalk() {
        phase = Phase.WALK;
        phaseTimer = enraged ? 1.3f : 1.9f;
    }

    private void nextPattern(GameWorld world) {
        Phase[] order = {Phase.GUNS, Phase.CROUCH, Phase.GUNS};
        phase = order[patternIndex++ % order.length];
        if (phase == Phase.GUNS) {
            boltsLeft = enraged ? 5 : 3;
            boltTimer = 0.2f;
            volleysLeft = enraged ? 6 : 4;
            volleyTimer = 0.5f;
        } else {
            phaseTimer = CROUCH;
            world.sfx().play("buzz", 0.6f);
        }
    }

    /** Crash down: whoever's under it gets hurt, and a gapless shockwave rolls out across the roof. */
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
        world.particles().burst(pos.x, feetY(), 30, 0.8f, 3f, 0.5f, 0.07f, Color.GRAY);
        int n = 40;
        for (int i = 0; i < n; i++) {
            Bullet b = world.spawnEnemyBullet(pos.x, feetY() + 0.15f, 360f * i / n, 2.1f * diff);
            b.color.set(WAVE);
            b.radius = 0.06f;
        }
        if (enraged) { // and a second, slower ring right behind it
            for (int i = 0; i < n; i++) {
                Bullet b = world.spawnEnemyBullet(pos.x, feetY() + 0.15f, 360f * (i + 0.5f) / n, 1.4f * diff);
                b.color.set(WAVE);
                b.radius = 0.06f;
            }
        }
    }

    /** Enraged: the chest turret folds out and sprays a double spiral (not while it's jumping). */
    private void updateTurret(float delta, GameWorld world, float diff) {
        turretOpen = MathUtils.clamp(turretOpen + (enraged ? delta : -delta) * 2f, 0f, 1f);
        if (turretOpen < 1f || phase == Phase.CROUCH || phase == Phase.AIR) {
            return;
        }
        turretTimer -= delta;
        turretAngle += delta * 95f;
        if (turretTimer <= 0) {
            turretTimer = 0.16f;
            for (int i = 0; i < 2; i++) {
                Bullet b = world.spawnEnemyBullet(pos.x, pos.y + 0.12f, turretAngle + i * 180f, 1.9f * diff);
                b.color.set(STRIPE);
                b.shape = Bullet.Shape.DIAMOND;
            }
        }
    }

    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (phase == Phase.AIR) { // its shadow grows where it's going to land
            float k = 1f - phaseTimer / AIR;
            d.ellipse(b, landAt.x, landAt.y - 0.55f, STOMP_RADIUS * (0.4f + 0.6f * k), STOMP_RADIUS * 0.45f * (0.4f + 0.6f * k),
                    Color.BLACK, 0.2f + 0.3f * k);
            d.ring(b, landAt.x, landAt.y - 0.4f, STOMP_RADIUS, TELEGRAPH, 0.4f + 0.5f * k);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float lift = 0f;
        float squat = 0f;
        if (phase == Phase.CROUCH) {
            squat = 0.12f * (1f - phaseTimer / CROUCH);
        } else if (phase == Phase.AIR) {
            float k = 1f - phaseTimer / AIR;
            lift = MathUtils.sin(k * MathUtils.PI) * JUMP_HEIGHT;
        } else if (phase == Phase.LAND) {
            squat = 0.15f * (phaseTimer / LAND);
        }
        float x = pos.x;
        float feet = feetY() + lift;
        float step = phase == Phase.WALK ? MathUtils.sin(walkTime * 6f) * 0.05f : 0f;
        // legs
        for (int side = -1; side <= 1; side += 2) {
            float lx = x + side * 0.24f;
            float lh = 0.38f - squat + (side < 0 ? step : -step);
            d.rect(b, lx - 0.12f, feet, 0.24f, 0.12f, c(FRAME));                  // foot
            d.rect(b, lx - 0.08f, feet + 0.1f, 0.16f, lh, c(ARMOR_DARK));        // shin
        }
        float body = feet + 0.46f - squat;
        // torso
        d.rect(b, x - 0.48f, body, 0.96f, 0.62f, c(ARMOR));
        d.rect(b, x - 0.48f, body, 0.96f, 0.08f, c(ARMOR_DARK));
        d.rect(b, x - 0.48f, body + 0.5f, 0.96f, 0.05f, c(STRIPE));
        d.rect(b, x - 0.48f, body, 0.08f, 0.62f, c(ARMOR_DARK));
        // chest: a hatch that opens into a turret when enraged
        float cy = body + 0.28f;
        d.rect(b, x - 0.22f, cy - 0.16f, 0.44f, 0.32f, c(FRAME));
        if (turretOpen > 0) {
            float o = turretOpen;
            d.rect(b, x - 0.22f - 0.18f * o, cy - 0.16f, 0.2f, 0.32f, c(ARMOR_DARK)); // hatch doors sliding apart
            d.rect(b, x + 0.02f + 0.18f * o, cy - 0.16f, 0.2f, 0.32f, c(ARMOR_DARK));
            d.circle(b, x, cy, 0.12f * o, FRAME);
            d.line(b, x, cy, 0.25f * o, 0.08f, turretAngle, FRAME);
            d.line(b, x, cy, 0.25f * o, 0.08f, turretAngle + 180f, FRAME);
            d.circle(b, x, cy, 0.06f * o, STRIPE);
        } else {
            d.rect(b, x - 0.2f, cy - 0.14f, 0.19f, 0.28f, c(ARMOR_DARK));
            d.rect(b, x + 0.01f, cy - 0.14f, 0.19f, 0.28f, c(ARMOR_DARK));
            d.circle(b, x, cy, 0.05f, GLOW);
        }
        // arms with cannons
        for (int side = -1; side <= 1; side += 2) {
            float sx = x + side * 0.62f;
            float sy = body + 0.45f;
            d.circle(b, sx, sy, 0.2f, c(ARMOR_DARK));                            // shoulder
            float aimFrom = side < 0 == leftArm && phase == Phase.GUNS ? 0.05f : 0f;
            d.rect(b, sx - 0.11f, sy - 0.5f - aimFrom, 0.22f, 0.45f, c(ARMOR));  // arm
            d.rect(b, sx - 0.08f, sy - 0.62f - aimFrom, 0.16f, 0.14f, c(FRAME)); // cannon barrel
            d.circle(b, sx, sy - 0.64f - aimFrom, 0.05f, VISOR);
        }
        // head with a visor and the laser eye
        float hy = body + 0.7f;
        d.rect(b, x - 0.25f, hy, 0.5f, 0.32f, c(ARMOR));
        d.rect(b, x - 0.21f, hy + 0.1f, 0.42f, 0.13f, VISOR);
        boolean firing = phase == Phase.GUNS && boltsLeft > 0;
        float eyeX = x + MathUtils.cosDeg(aimAngle) * 0.12f;
        d.rect(b, eyeX - 0.08f, hy + 0.14f, 0.16f, 0.05f, firing || enraged ? BOLT : GLOW);
        d.rect(b, x - 0.03f, hy + 0.32f, 0.06f, 0.1f, c(FRAME)); // antenna
        d.circle(b, x, hy + 0.44f, 0.04f, enraged ? BOLT : STRIPE);
    }
}
