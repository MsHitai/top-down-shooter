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
 * City boss, designed by the owner's son: the Evil Train. It can only move along an oval railway loop
 * around the arena (it draws its own rails), pulling a coal tender and a boxcar - all of it hurts to touch.
 * <ul>
 *   <li>its smokestack keeps puffing harmful smoke that drifts upward: don't hang around above it;</li>
 *   <li>cruising, the cab throws coal and the wagons fire volleys from their sides;</li>
 *   <li>a whistle ("ALL ABOARD!"), then it races a whole lap at high speed - stay off the track;</li>
 *   <li>it brakes in a shower of sparks, vents a ring of steam and goes the other way.</li>
 * </ul>
 * Enraged: faster, more smoke, red eyes.
 */
public class EvilTrain extends Enemy {

    private enum Phase { CRUISE, WHISTLE, CHARGE, BRAKE }

    private static final Color BOILER = Color.valueOf("22222a");
    private static final Color BOILER_LIGHT = Color.valueOf("3a3a46");
    private static final Color CAB = Color.valueOf("7a2018");
    private static final Color RED = Color.valueOf("c0392b");
    private static final Color BRASS = Color.valueOf("d9b24c");
    private static final Color WHEEL = Color.valueOf("1a1a20");
    private static final Color FACE = Color.valueOf("9a9aa6");
    private static final Color EYE = Color.valueOf("ffd166");
    private static final Color COAL = Color.valueOf("2a2a30");
    private static final Color BOXCAR = Color.valueOf("5a2a22");
    private static final Color SMOKE = Color.valueOf("8a8a96");
    private static final Color STEAM = Color.valueOf("e8ecf0");
    private static final Color SLEEPER = Color.valueOf("4a3222");
    private static final Color RAIL = Color.valueOf("8a8a96");
    private static final Color SPARK = Color.valueOf("ffd166");

    private static final int WAGONS = 2;
    private static final float CAR_SPACING = 1.05f;
    /** Contact radius of a wagon. */
    private static final float WAGON_RADIUS = 0.38f;

    private Phase phase = Phase.CRUISE;
    private float phaseTimer = 3.5f;
    private boolean enraged;
    private float time;
    /** Position on the loop (radians) and direction (+1 / -1). */
    private float t = MathUtils.HALF_PI; // starts at the top, away from the doors the player comes in by
    private float dir = 1;
    private float trainSpeed = 0f;
    private float lapLeft;
    private float smokeTimer;
    private float coalTimer = 1.5f;
    private float volleyTimer = 2.2f;
    private boolean chargeNext = true;
    private float cx;
    private float cy;
    private float rx;
    private float ry;
    private final Vector2 car = new Vector2();
    private final Vector2 tangent = new Vector2();

    public EvilTrain(float x, float y, int tier) {
        super(x, y, 560f + 160f * tier);
        radius = 0.5f;
        speed = 1.6f;
        score = 500 + 250 * tier;
        boss = true;
        flying = true; // it runs on its rails: never pushed off them by furniture
        // the loop: an oval around the arena with room to stand inside and outside it
        Arena a = Arena.now;
        cx = a.centerX;
        cy = a.centerY;
        rx = Math.min(4.9f, (a.right - a.left) / 2f - 1.3f);
        ry = Math.min(2.6f, (a.top - a.bottom) / 2f - 1.1f);
        pointAt(t, pos);
    }

    @Override
    public String bossName() {
        return Boss.EVIL_TRAIN.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    @Override
    public float feetY() {
        return pos.y - 0.4f;
    }

    /** A point on the loop. */
    private Vector2 pointAt(float angle, Vector2 out) {
        return out.set(cx + rx * MathUtils.cos(angle), cy + ry * MathUtils.sin(angle));
    }

    /** How fast the loop's position moves per radian here (to keep a steady speed along the rails). */
    private float stretch(float angle) {
        float sx = rx * MathUtils.sin(angle);
        float sy = ry * MathUtils.cos(angle);
        return (float) Math.sqrt(sx * sx + sy * sy);
    }

    /** Where car {@code i} is (0 = the locomotive, then the wagons behind it). */
    private float carAngle(int i) {
        float a = t;
        for (int k = 0; k < i; k++) {
            a -= dir * CAR_SPACING / stretch(a);
        }
        return a;
    }

    private float face(float angle) {
        tangent.set(-rx * MathUtils.sin(angle), ry * MathUtils.cos(angle)).scl(dir);
        return tangent.x >= 0 ? 1 : -1;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        if (!enraged && hp < maxHp * 0.5f) {
            enraged = true;
            world.shake(0.6f);
            world.sfx().play("whistle", 1f);
        }
        float diff = world.getDifficulty();
        float rage = enraged ? 1.3f : 1f;
        phaseTimer -= delta;

        float wanted;
        switch (phase) {
            case CRUISE:
                wanted = 1.6f * rage;
                cruiseAttacks(delta, world, diff);
                if (phaseTimer <= 0) {
                    if (chargeNext) {
                        phase = Phase.WHISTLE;
                        phaseTimer = 1.0f;
                        world.sfx().play("whistle", 1f);
                        world.toast("ALL ABOARD!");
                    } else {
                        phase = Phase.BRAKE;
                        phaseTimer = 0.7f;
                    }
                    chargeNext = !chargeNext;
                }
                break;
            case WHISTLE:
                wanted = 0.8f;
                if (phaseTimer <= 0) {
                    phase = Phase.CHARGE;
                    lapLeft = MathUtils.PI2;
                }
                break;
            case CHARGE:
                wanted = 4.0f * rage;
                if (lapLeft <= 0) {
                    phase = Phase.CRUISE;
                    phaseTimer = 3.5f;
                }
                break;
            default: // BRAKE
                wanted = 0f;
                if (MathUtils.randomBoolean(0.6f)) {
                    world.particles().spawn(pos.x + MathUtils.random(-0.3f, 0.3f), feetY() + 0.05f,
                            MathUtils.random(-1f, 1f), MathUtils.random(0.3f, 1.2f), 0.3f, 0.03f, SPARK, 2f);
                }
                if (phaseTimer <= 0) {
                    steamRing(world, diff);
                    dir = -dir;
                    phase = Phase.CRUISE;
                    phaseTimer = 3.5f;
                }
                break;
        }
        // ease toward the wanted speed (braking is quick, accelerating a bit slower)
        float accel = wanted < trainSpeed ? 6f : 2.5f;
        trainSpeed += MathUtils.clamp(wanted - trainSpeed, -accel * delta, accel * delta);
        float dt = dir * trainSpeed * delta / stretch(t);
        t += dt;
        if (phase == Phase.CHARGE) {
            lapLeft -= Math.abs(dt);
        }
        pointAt(t, pos);
        walkTime += trainSpeed * delta;

        smoke(delta, world, diff);
        wagonsHurt(world);
    }

    /** The smokestack never stops: puffs that drift up and hurt. */
    private void smoke(float delta, GameWorld world, float diff) {
        smokeTimer -= delta;
        if (smokeTimer > 0) {
            return;
        }
        smokeTimer = (enraged ? 0.15f : 0.22f) * (phase == Phase.CHARGE ? 0.6f : 1f);
        float f = face(t);
        Bullet b = world.spawnEnemyBullet(pos.x + f * 0.22f, pos.y + 0.45f, 90f + MathUtils.random(-35f, 35f),
                MathUtils.random(0.45f, 0.7f) * diff);
        b.accel = -0.25f;
        b.minSpeed = 0.15f;
        b.radius = 0.12f;
        b.life = 2.4f;
        b.color.set(SMOKE);
    }

    private void cruiseAttacks(float delta, GameWorld world, float diff) {
        if (!canSeePlayer) {
            return;
        }
        coalTimer -= delta;
        if (coalTimer <= 0) { // the cab throws lumps of coal
            coalTimer = (enraged ? 1.0f : 1.5f) / diff;
            for (int i = -1; i <= 1; i++) {
                shoot(world, aimAngle + i * 12f, 2.6f * diff).color.set(COAL);
            }
            world.sfx().play("enemy_shoot", 0.8f);
        }
        volleyTimer -= delta;
        if (volleyTimer <= 0) { // the wagons fire from their sides
            volleyTimer = (enraged ? 1.5f : 2.2f) / diff;
            Player p = world.getPlayer();
            for (int w = 1; w <= WAGONS; w++) {
                pointAt(carAngle(w), car);
                float a = MathUtils.atan2Deg360(p.hitboxY() - car.y, p.centerX() - car.x);
                for (int i = -1; i <= 1; i++) {
                    world.spawnEnemyBullet(car.x, car.y, a + i * 15f, 2.3f * diff).color.set(RED);
                }
            }
            world.sfx().play("enemy_shoot", 0.7f);
        }
    }

    private void steamRing(GameWorld world, float diff) {
        int n = enraged ? 20 : 16;
        float off = MathUtils.random(360f);
        for (int i = 0; i < n; i++) {
            Bullet b = shoot(world, off + 360f * i / n, 1.8f * diff);
            b.color.set(STEAM);
        }
        world.shake(0.3f);
        world.sfx().play("blank", 0.5f);
    }

    /** The wagons hurt to touch too (the locomotive uses the normal contact damage). */
    private void wagonsHurt(GameWorld world) {
        Player p = world.getPlayer();
        for (int w = 1; w <= WAGONS; w++) {
            pointAt(carAngle(w), car);
            float r = WAGON_RADIUS + Player.HITBOX_RADIUS;
            if (Vector2.dst2(car.x, car.y, p.centerX(), p.hitboxY()) < r * r) {
                world.hurtPlayer();
                return;
            }
        }
    }

    /** The rails, drawn on the floor under everything. */
    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        int sleepers = 64;
        for (int k = 0; k < sleepers; k++) {
            float a = MathUtils.PI2 * k / sleepers;
            pointAt(a, car);
            float ang = MathUtils.atan2Deg360(ry * MathUtils.cos(a), -rx * MathUtils.sin(a));
            d.rotRect(b, car.x, car.y, 0.09f, 0.44f, ang, SLEEPER, 1f);
        }
        int segments = 160;
        for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; k < segments; k++) {
                float a = MathUtils.PI2 * k / segments;
                float nx = ry * MathUtils.cos(a);
                float ny = rx * MathUtils.sin(a);
                float nl = (float) Math.sqrt(nx * nx + ny * ny);
                float px = cx + rx * MathUtils.cos(a) + side * 0.15f * nx / nl;
                float py = cy + ry * MathUtils.sin(a) + side * 0.15f * ny / nl;
                float ang = MathUtils.atan2Deg360(ry * MathUtils.cos(a), -rx * MathUtils.sin(a));
                float len = stretch(a) * MathUtils.PI2 / segments + 0.02f;
                d.rotRect(b, px, py, len, 0.04f, ang, RAIL, 1f);
            }
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        for (int w = WAGONS; w >= 1; w--) { // wagons first: the locomotive is in front
            float a = carAngle(w);
            pointAt(a, car);
            drawWagon(b, d, car.x, car.y - 0.4f, face(a), w == 1);
        }
        drawLocomotive(b, d, pos.x, feetY(), face(t));
    }

    private void drawWheels(SpriteBatch b, Draw d, float x, float bottom, float[] offsets, float r) {
        float spin = walkTime * 400f;
        for (float o : offsets) {
            d.circle(b, x + o, bottom + r, r, WHEEL);
            d.rotRect(b, x + o, bottom + r, r * 1.6f, r * 0.25f, spin, RED, 1f);
            d.rotRect(b, x + o, bottom + r, r * 1.6f, r * 0.25f, spin + 90f, RED, 1f);
        }
    }

    private void drawWagon(SpriteBatch b, Draw d, float x, float bottom, float f, boolean tender) {
        drawWheels(b, d, x, bottom, new float[]{-0.24f, 0.24f}, 0.1f);
        d.rect(b, x - 0.38f, bottom + 0.16f, 0.76f, 0.42f, c(tender ? COAL : BOXCAR));
        d.rect(b, x - 0.38f, bottom + 0.16f, 0.76f, 0.05f, c(BOILER_LIGHT));
        if (tender) { // a heap of coal on top
            for (int i = -1; i <= 1; i++) {
                d.circle(b, x + i * 0.18f, bottom + 0.6f, 0.12f, c(COAL));
            }
        } else { // boxcar door
            d.rect(b, x - 0.12f, bottom + 0.22f, 0.24f, 0.32f, c(CAB));
            d.rect(b, x - 0.01f, bottom + 0.22f, 0.02f, 0.32f, c(BOILER));
        }
        d.rect(b, x - f * 0.42f - 0.03f, bottom + 0.2f, 0.06f, 0.05f, BOILER); // coupling
    }

    private void drawLocomotive(SpriteBatch b, Draw d, float x, float bottom, float f) {
        boolean angry = enraged || phase == Phase.CHARGE || phase == Phase.WHISTLE;
        drawWheels(b, d, x, bottom, new float[]{-0.3f, 0f, 0.3f}, 0.14f);
        d.rect(b, x - 0.36f, bottom + 0.14f, 0.72f, 0.03f, RED); // connecting rod
        // boiler, cab at the back, smokestack at the front
        d.rect(b, x - 0.42f, bottom + 0.22f, 0.84f, 0.34f, c(BOILER));
        d.rect(b, x - 0.42f, bottom + 0.5f, 0.84f, 0.04f, c(BOILER_LIGHT));
        float cabX = x - f * 0.3f;
        d.rect(b, cabX - 0.16f, bottom + 0.22f, 0.32f, 0.54f, c(CAB));
        d.rect(b, cabX - 0.2f, bottom + 0.74f, 0.4f, 0.05f, c(BOILER));
        d.rect(b, cabX - 0.08f, bottom + 0.5f, 0.16f, 0.14f, EYE); // lit window
        float stackX = x + f * 0.16f;
        d.rect(b, stackX - 0.06f, bottom + 0.55f, 0.12f, 0.2f, c(BOILER));
        d.rect(b, stackX - 0.09f, bottom + 0.73f, 0.18f, 0.05f, c(BOILER_LIGHT));
        d.rect(b, x + f * 0.02f - 0.03f, bottom + 0.55f, 0.06f, 0.07f, BRASS); // bell
        // cowcatcher
        d.rotRect(b, x + f * 0.46f, bottom + 0.12f, 0.16f, 0.14f, f * 35f, c(RED), 1f);
        // the evil face on the front
        float fx = x + f * 0.42f;
        float fy = bottom + 0.39f;
        d.circle(b, fx, fy, 0.19f, c(FACE));
        Color eye = angry ? Color.RED : EYE;
        for (int i = -1; i <= 1; i += 2) {
            d.circle(b, fx + i * 0.07f, fy + 0.05f, 0.04f, eye);
            d.rotRect(b, fx + i * 0.07f, fy + 0.11f, 0.08f, 0.02f, i * 20f, BOILER, 1f); // angry brows
        }
        d.rect(b, fx - 0.08f, fy - 0.09f, 0.16f, 0.04f, BOILER); // grim mouth
        for (int i = 0; i < 3; i++) {
            d.rect(b, fx - 0.06f + i * 0.05f, fy - 0.09f, 0.02f, 0.04f, STEAM); // teeth
        }
        if (phase == Phase.WHISTLE) { // steam whistle blowing
            d.circle(b, x + f * 0.02f, bottom + 0.75f + MathUtils.random(0.05f), 0.08f, STEAM, 0.8f);
        }
    }
}
