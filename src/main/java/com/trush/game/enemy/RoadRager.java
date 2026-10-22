package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * Town boss, designed by the owner's son: a monster truck. It drives around the room and
 * <ul>
 *   <li>fires big shells from the cannon on its roof;</li>
 *   <li>charges its headlights and fires twin lasers that sweep toward you (walls and cover stop them);</li>
 *   <li>pops the roof hatch and throws enemies into the room;</li>
 *   <li>revs up and rams straight across the room, crashing into the wall in a shower of debris.</li>
 * </ul>
 */
public class RoadRager extends Enemy {

    private enum Phase { DRIVE, CANNON, LASER_CHARGE, LASER, HATCH, REV, RAM, RECOVER }

    private static final Color BODY = Color.valueOf("c0392b");
    private static final Color BODY_DARK = Color.valueOf("8e2a20");
    private static final Color BODY_LIGHT = Color.valueOf("e05a48");
    private static final Color WINDOW = Color.valueOf("8ec5e8");
    private static final Color TIRE = Color.valueOf("2a2a30");
    private static final Color HUB = Color.valueOf("9a9aa6");
    private static final Color METAL = Color.valueOf("5a5a66");
    private static final Color HEADLIGHT = Color.valueOf("fff3b0");
    private static final Color LASER = Color.valueOf("ff3b3b");
    private static final Color SHELL = Color.valueOf("3a3a44");
    private static final Color DEBRIS = Color.valueOf("8a8a96");
    private static final Color DRIVER = Color.valueOf("d9b24c");

    private static final float LASER_CHARGE = 0.9f;
    private static final float LASER_TIME = 0.9f;
    private static final float BEAM_GAP = 0.16f;
    private static final float RAM_SPEED = 4.8f;

    private Phase phase = Phase.DRIVE;
    private float phaseTimer = 1.5f;
    private float shotTimer;
    private int shotsLeft;
    private int patternIndex;
    private boolean enraged;
    private float time;
    private float turretAngle = 270f;
    private float laserAngle;
    private final float[] beamLength = new float[2];
    private float rammed;
    private float hatchOpen;
    private final Vector2 target = new Vector2();
    private final Vector2 step = new Vector2();
    private final Vector2 ramDir = new Vector2();
    private final Vector2 lastPos = new Vector2();
    private final Vector2 beamFrom = new Vector2();
    private final Vector2 beamTo = new Vector2();
    private final Vector2 hit = new Vector2();

    public RoadRager(float x, float y, int tier) {
        super(x, y, 520f + 160f * tier);
        radius = 0.5f;
        speed = 1.3f;
        score = 500 + 250 * tier;
        boss = true;
        target.set(x, y);
    }

    @Override
    public String bossName() {
        return Boss.ROAD_RAGER.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    @Override
    public float feetY() {
        return pos.y - 0.42f;
    }

    private float face() {
        if (phase == Phase.RAM || phase == Phase.REV) {
            return ramDir.x >= 0 ? 1 : -1;
        }
        return facingRight() ? 1 : -1;
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
        phaseTimer -= delta;
        shotTimer -= delta;
        hatchOpen = Math.max(0f, hatchOpen - delta * 1.5f);
        // the turret slowly follows the player
        float turn = ((aimAngle - turretAngle + 540f) % 360f) - 180f;
        turretAngle += MathUtils.clamp(turn, -150f * delta, 150f * delta);

        switch (phase) {
            case DRIVE:
                drive(delta, world, rage);
                if (phaseTimer <= 0) {
                    nextPattern(world);
                }
                break;
            case CANNON:
                drive(delta, world, rage * 0.4f);
                if (shotTimer <= 0 && shotsLeft > 0) {
                    shotTimer = 0.45f / rage;
                    shotsLeft--;
                    float tx = pos.x + MathUtils.cosDeg(turretAngle) * 0.42f;
                    float ty = pos.y + 0.32f + MathUtils.sinDeg(turretAngle) * 0.42f;
                    for (int i = -1; i <= 1; i++) {
                        Bullet b = world.spawnEnemyBullet(tx, ty, turretAngle + i * 14f, (i == 0 ? 2.8f : 2.3f) * diff);
                        b.radius = i == 0 ? 0.15f : 0.08f;
                        b.color.set(SHELL);
                    }
                    world.shake(0.12f);
                    world.sfx().play("magnum", 0.8f);
                }
                if (shotsLeft <= 0 && shotTimer <= 0) {
                    toDrive();
                }
                break;
            case LASER_CHARGE:
                // aim the headlights at the player while charging, then hold
                if (phaseTimer > 0.3f) {
                    laserAngle = angleFromHeadlights(world);
                }
                updateBeams(world);
                if (phaseTimer <= 0) {
                    phase = Phase.LASER;
                    phaseTimer = LASER_TIME;
                    world.sfx().play("blank", 0.5f);
                }
                break;
            case LASER:
                float wanted = angleFromHeadlights(world);
                float d = ((wanted - laserAngle + 540f) % 360f) - 180f;
                float sweep = (enraged ? 40f : 26f) * delta; // slow enough to outrun sideways
                laserAngle += MathUtils.clamp(d, -sweep, sweep);
                updateBeams(world);
                if (beamHitsPlayer(world)) {
                    world.hurtPlayer();
                }
                if (MathUtils.randomBoolean(0.5f)) {
                    world.particles().spawn(beamFrom.x + MathUtils.cosDeg(laserAngle) * beamLength[0],
                            beamFrom.y + MathUtils.sinDeg(laserAngle) * beamLength[0], 0, 0.3f, 0.2f, 0.05f, LASER, 2f);
                }
                if (phaseTimer <= 0) {
                    toDrive();
                }
                break;
            case HATCH:
                if (phaseTimer <= 0) {
                    throwEnemies(world);
                    toDrive();
                }
                break;
            case REV:
                if (phaseTimer > 0.25f) {
                    ramDir.set(MathUtils.cosDeg(aimAngle), MathUtils.sinDeg(aimAngle));
                }
                if (MathUtils.randomBoolean(0.5f)) { // exhaust
                    world.particles().spawn(pos.x - ramDir.x * 0.6f, pos.y, -ramDir.x * 0.5f, 0.4f, 0.4f, 0.06f, METAL, 1f);
                }
                if (phaseTimer <= 0) {
                    phase = Phase.RAM;
                    rammed = 0;
                    lastPos.set(pos);
                    world.sfx().play("dodge", 1f);
                }
                break;
            case RAM:
                ram(delta, world, rage, diff);
                break;
            case RECOVER:
                if (phaseTimer <= 0) {
                    toDrive();
                }
                break;
        }
    }

    /** Drives toward a spot that keeps some distance from the player (or around cover to see them). */
    private void drive(float delta, GameWorld world, float k) {
        boolean hunting = huntForSight(delta, world, target);
        Player p = world.getPlayer();
        if (!hunting && (pos.dst2(target) < 0.05f
                || target.dst(p.centerX(), p.hitboxY()) < 1.6f)) {
            for (int i = 0; i < 10; i++) {
                target.set(MathUtils.random(Arena.LEFT + 0.8f, Arena.RIGHT - 0.8f),
                        MathUtils.random(Arena.BOTTOM + 0.7f, Arena.TOP - 0.7f));
                if (target.dst(p.centerX(), p.hitboxY()) > 2f) break;
            }
        }
        step.set(target).sub(pos);
        if (step.len() > 0.02f) {
            step.nor().scl(speed * k * delta);
            pos.add(step);
            walkTime += delta;
        }
    }

    private float angleFromHeadlights(GameWorld world) {
        Player p = world.getPlayer();
        float hx = pos.x + face() * 0.55f;
        float hy = pos.y - 0.05f; // the drawn headlight
        return MathUtils.atan2Deg360(p.hitboxY() - hy, p.centerX() - hx);
    }

    /** Two parallel beams from the headlights, cut short by walls and cover. */
    private void updateBeams(GameWorld world) {
        float hx = pos.x + face() * 0.55f;
        float hy = pos.y - 0.05f; // the drawn headlight
        float nx = -MathUtils.sinDeg(laserAngle) * BEAM_GAP;
        float ny = MathUtils.cosDeg(laserAngle) * BEAM_GAP;
        beamFrom.set(hx, hy);
        beamLength[0] = world.rayLength(hx + nx, hy + ny, laserAngle, 9f);
        beamLength[1] = world.rayLength(hx - nx, hy - ny, laserAngle, 9f);
    }

    private boolean beamHitsPlayer(GameWorld world) {
        Player p = world.getPlayer();
        float nx = -MathUtils.sinDeg(laserAngle) * BEAM_GAP;
        float ny = MathUtils.cosDeg(laserAngle) * BEAM_GAP;
        for (int i = 0; i < 2; i++) {
            float sx = beamFrom.x + (i == 0 ? nx : -nx);
            float sy = beamFrom.y + (i == 0 ? ny : -ny);
            beamTo.set(sx + MathUtils.cosDeg(laserAngle) * beamLength[i], sy + MathUtils.sinDeg(laserAngle) * beamLength[i]);
            float dist = Intersector.distanceSegmentPoint(sx, sy, beamTo.x, beamTo.y, p.centerX(), p.hitboxY());
            if (dist < Player.HITBOX_RADIUS + 0.05f) {
                return true;
            }
        }
        return false;
    }

    /** The driver pops out of the roof hatch and throws a few enemies into the room. */
    private void throwEnemies(GameWorld world) {
        int alive = world.getEnemies().size + world.getPendingEnemies().size - 1;
        int n = Math.min(enraged ? 3 : 2, 5 - alive);
        for (int i = 0; i < n; i++) {
            float a = aimAngle + (i - (n - 1) / 2f) * 45f;
            hit.set(pos.x + MathUtils.cosDeg(a) * 1.3f, pos.y + MathUtils.sinDeg(a) * 1.3f);
            Arena.clamp(hit, 0.4f);
            int kind = MathUtils.random(2);
            if (kind == 0) {
                world.spawnEnemy(new BulletKin(hit.x, hit.y));
            } else if (kind == 1) {
                world.spawnEnemy(new ShellCop(hit.x, hit.y));
            } else {
                world.spawnEnemy(new Rat(hit.x, hit.y));
                world.spawnEnemy(new Rat(hit.x + 0.25f, hit.y + 0.1f));
            }
        }
        hatchOpen = 1f;
        world.sfx().play("door", 0.8f);
    }

    private void ram(float delta, GameWorld world, float rage, float diff) {
        boolean blocked = rammed > 0.3f && pos.dst(lastPos) < RAM_SPEED * delta * 0.3f;
        if (blocked || rammed > 7f || !Arena.contains(pos.x, pos.y, radius + 0.02f)) {
            int n = enraged ? 18 : 12;
            float off = MathUtils.random(360f);
            for (int i = 0; i < n; i++) {
                Bullet b = shoot(world, off + 360f * i / n, MathUtils.random(1.4f, 2.2f) * diff);
                b.color.set(DEBRIS);
            }
            world.shake(0.6f);
            world.sfx().play("break", 1f);
            world.particles().burst(pos.x + ramDir.x * 0.5f, pos.y, 18, 0.5f, 2f, 0.5f, 0.06f, DEBRIS);
            phase = Phase.RECOVER;
            phaseTimer = enraged ? 0.6f : 0.9f;
            return;
        }
        lastPos.set(pos);
        float move = RAM_SPEED * rage * delta;
        pos.mulAdd(ramDir, move);
        rammed += move;
        walkTime += delta * 3f;
    }

    private void toDrive() {
        phase = Phase.DRIVE;
        phaseTimer = enraged ? 1.0f : 1.6f;
    }

    private void nextPattern(GameWorld world) {
        Phase[] order = {Phase.CANNON, Phase.LASER_CHARGE, Phase.REV, Phase.HATCH};
        phase = order[patternIndex++ % order.length];
        switch (phase) {
            case CANNON:
                shotsLeft = enraged ? 5 : 3;
                shotTimer = 0.4f;
                break;
            case LASER_CHARGE:
                phaseTimer = LASER_CHARGE;
                laserAngle = angleFromHeadlights(world);
                break;
            case REV:
                phaseTimer = enraged ? 0.55f : 0.8f;
                ramDir.set(MathUtils.cosDeg(aimAngle), MathUtils.sinDeg(aimAngle));
                world.sfx().play("chainsaw", 0.6f); // engine roar
                break;
            case HATCH:
                phaseTimer = 0.6f;
                hatchOpen = 1f;
                break;
            default:
                break;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float face = face();
        boolean revving = phase == Phase.REV;
        float rumble = revving || phase == Phase.RAM ? MathUtils.random(-0.012f, 0.012f) : 0f;
        float cx = pos.x;
        float bottom = feetY() + rumble;

        // lasers (under the truck so the headlights sit on top)
        if (phase == Phase.LASER_CHARGE || phase == Phase.LASER) {
            float nx = -MathUtils.sinDeg(laserAngle) * BEAM_GAP;
            float ny = MathUtils.cosDeg(laserAngle) * BEAM_GAP;
            for (int i = 0; i < 2; i++) {
                float sx = beamFrom.x + (i == 0 ? nx : -nx);
                float sy = beamFrom.y + (i == 0 ? ny : -ny);
                if (phase == Phase.LASER_CHARGE) {
                    float pulse = 0.3f + 0.4f * MathUtils.sin(time * 25f);
                    d.line(b, sx, sy, beamLength[i], 0.02f, laserAngle, LASER);
                    d.circle(b, sx, sy, 0.06f + 0.04f * pulse, LASER, 0.5f);
                } else {
                    d.line(b, sx, sy, beamLength[i], 0.1f, laserAngle, LASER);
                    d.line(b, sx, sy, beamLength[i], 0.035f, laserAngle, Color.WHITE);
                }
            }
        }
        // giant wheels
        float spin = walkTime * 12f;
        for (int i = -1; i <= 1; i += 2) {
            float wx = cx + i * 0.36f;
            float wy = bottom + 0.16f;
            d.circle(b, wx, wy, 0.19f, TIRE);
            d.circle(b, wx, wy, 0.08f, HUB);
            d.rotRect(b, wx, wy, 0.14f, 0.03f, spin * 57f, METAL, 1f);
        }
        // body
        float by = bottom + 0.22f;
        d.rect(b, cx - 0.56f, by, 1.12f, 0.3f, c(BODY));
        d.rect(b, cx - 0.56f, by, 1.12f, 0.06f, c(BODY_DARK));
        d.rect(b, cx - 0.56f, by + 0.26f, 1.12f, 0.04f, c(BODY_LIGHT));
        // cab with windows, toward the front
        float cab = cx + face * 0.1f;
        d.rect(b, cab - 0.28f, by + 0.3f, 0.56f, 0.22f, c(BODY));
        d.rect(b, cab - 0.22f, by + 0.34f, 0.2f, 0.14f, WINDOW);
        d.rect(b, cab + 0.02f, by + 0.34f, 0.2f, 0.14f, WINDOW);
        // angry headlight eyes
        float hx = cx + face * 0.53f;
        boolean charging = phase == Phase.LASER_CHARGE || phase == Phase.LASER;
        d.circle(b, hx, by + 0.15f, 0.07f, charging ? LASER : HEADLIGHT);
        d.rotRect(b, hx - face * 0.02f, by + 0.24f, 0.12f, 0.03f, face * -20f, TIRE, 1f);
        // bumper grin
        d.rect(b, cx + face * 0.4f - 0.15f, by - 0.02f, 0.3f, 0.05f, HUB);
        // roof hatch with the driver popping out
        float hatchX = cab - face * 0.1f;
        if (hatchOpen > 0) {
            float up = Math.min(1f, hatchOpen * 2f) * 0.14f;
            d.circle(b, hatchX, by + 0.52f + up, 0.09f, DRIVER);
            d.circle(b, hatchX + face * 0.03f, by + 0.54f + up, 0.02f, Color.BLACK);
        }
        d.rect(b, hatchX - 0.1f, by + 0.51f, 0.2f, 0.04f, METAL);
        // cannon turret on the roof
        float tx = cab + face * 0.12f;
        float ty = by + 0.56f;
        d.rect(b, tx - 0.1f, ty - 0.04f, 0.2f, 0.1f, METAL);
        d.line(b, tx, ty + 0.02f, 0.32f, 0.07f, turretAngle, TIRE);
        if (revving) {
            d.circle(b, cx, pos.y, radius * (1.1f + 0.15f * MathUtils.sin(time * 40f)), TELEGRAPH, 0.5f);
        }
    }
}
