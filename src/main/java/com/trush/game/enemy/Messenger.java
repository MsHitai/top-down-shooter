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
 * Skyscraper boss: the Messenger of the Fluffy King - a fluffy herald in a winged helmet who rides
 * a cloud round and round you, swinging a magic sword.
 * <ul>
 *   <li>the sword's triangle: a solid wedge of bullets flying tip first - the shape the owner's son
 *       drew for it (enraged: two wedges at once);</li>
 *   <li>crescent slashes: arcs of blades swept out from the sword;</li>
 *   <li>cloud rain: drops pour down around you from the sky (each falls from a little cloud first).</li>
 * </ul>
 */
public class Messenger extends Enemy {

    private enum Phase { RIDE, TRIANGLES, SLASHES, RAIN }

    private static final Color CLOUD = Color.WHITE;
    private static final Color CLOUD_SHADE = Color.valueOf("d8e4f0");
    private static final Color WOOL = Color.valueOf("f5f0e8");
    private static final Color TUNIC = Color.valueOf("6a4ab0");
    private static final Color TUNIC_DARK = Color.valueOf("4a3280");
    private static final Color GOLD = Color.valueOf("ffd23a");
    private static final Color WING = Color.valueOf("f5f5ff");
    private static final Color BLADE = Color.valueOf("8ae8ff");
    private static final Color MAGIC = Color.valueOf("fff3b0");
    private static final Color RAIN = Color.valueOf("6ec6ff");

    private static final float SWING = 0.5f;

    private Phase phase = Phase.RIDE;
    private float phaseTimer = 2f;
    private int patternIndex;
    private boolean enraged;
    private float time;
    private float orbitAngle = 90f;
    private float orbitDir = MathUtils.randomSign();
    private float orbitRadius = 2.6f;
    private final Vector2 target = new Vector2();
    private final Vector2 step = new Vector2();
    private int shotsLeft;
    private float shotTimer;
    /** Sword swing animation, 1 -> 0. */
    private float swing;
    private float swordAngle;
    // rain: little clouds that each drop one raindrop when their timer runs out
    private final Vector2[] rainAt = new Vector2[12];
    private final float[] rainTimer = new float[12];
    private float rainSpawn;

    public Messenger(float x, float y, int tier) {
        super(x, y, 700f + 200f * tier);
        radius = 0.4f;
        speed = 1.6f;
        score = 750 + 250 * tier;
        boss = true;
        flying = true;
        for (int i = 0; i < rainAt.length; i++) {
            rainAt[i] = new Vector2();
        }
    }

    @Override
    public String bossName() {
        return Boss.MESSENGER.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    /** It floats on its cloud: the shadow is far below. */
    @Override
    public float feetY() {
        return pos.y - 0.7f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        knock.setZero();
        swing = Math.max(0f, swing - delta / SWING);
        if (!enraged && hp < maxHp * 0.5f) {
            enraged = true;
            speed = 2.0f;
            world.shake(0.6f);
            world.sfx().play("boss", 1f);
            world.particles().burst(pos.x, pos.y, 24, 0.5f, 2f, 0.6f, 0.05f, MAGIC);
        }
        float diff = world.getDifficulty();
        phaseTimer -= delta;
        updateRain(delta, world, diff);
        ride(delta, world);
        swordAngle += (aimAngle - 40f * (1f - swing) * Math.signum(MathUtils.cosDeg(aimAngle)) - swordAngle) * Math.min(1f, delta * 8f);

        switch (phase) {
            case RIDE:
                if (phaseTimer <= 0) {
                    nextPattern(world);
                }
                break;
            case TRIANGLES:
                shotTimer -= delta;
                if (shotsLeft > 0 && shotTimer <= 0 && canShoot(world)) {
                    shotTimer = enraged ? 0.75f : 0.95f;
                    shotsLeft--;
                    swing = 1f;
                    float tipX = pos.x + MathUtils.cosDeg(aimAngle) * 0.55f;
                    float tipY = pos.y + MathUtils.sinDeg(aimAngle) * 0.55f;
                    if (enraged) { // two wedges, splaying apart
                        Patterns.triangle(world, tipX, tipY, aimAngle - 14f, 2.3f * diff, 5, 0.17f, Bullet.Shape.BALL, MAGIC);
                        Patterns.triangle(world, tipX, tipY, aimAngle + 14f, 2.3f * diff, 5, 0.17f, Bullet.Shape.BALL, MAGIC);
                    } else {
                        Patterns.triangle(world, tipX, tipY, aimAngle, 2.3f * diff, 6, 0.17f, Bullet.Shape.BALL, MAGIC);
                    }
                    world.sfx().play("zap", 0.8f);
                }
                if (shotsLeft <= 0 && shotTimer <= 0) {
                    toRide();
                }
                break;
            case SLASHES:
                shotTimer -= delta;
                if (shotsLeft > 0 && shotTimer <= 0) {
                    shotTimer = 0.5f;
                    shotsLeft--;
                    swing = 1f;
                    // a crescent of blades, swept out from the sword
                    int n = enraged ? 15 : 12;
                    float arc = 140f;
                    float off = shotsLeft % 2 == 0 ? 0f : arc / n / 2f;
                    for (int i = 0; i < n; i++) {
                        float a = aimAngle - arc / 2f + arc * i / (n - 1) + off;
                        float sx = pos.x + MathUtils.cosDeg(a) * 0.5f;
                        float sy = pos.y + MathUtils.sinDeg(a) * 0.5f;
                        Bullet b = world.spawnEnemyBullet(sx, sy, a, 2.6f * diff);
                        b.shape = Bullet.Shape.DIAMOND;
                        b.color.set(BLADE);
                    }
                    world.sfx().play("dodge", 0.9f);
                }
                if (shotsLeft <= 0 && shotTimer <= 0) {
                    toRide();
                }
                break;
            case RAIN:
                rainSpawn -= delta;
                if (rainSpawn <= 0 && phaseTimer > 0.6f) {
                    rainSpawn = enraged ? 0.07f : 0.11f;
                    startRaindrop(world);
                }
                if (phaseTimer <= 0) {
                    toRide();
                }
                break;
        }
    }

    /** Circles the player on its cloud, now and then swapping direction and distance. */
    private void ride(float delta, GameWorld world) {
        Player p = world.getPlayer();
        if (huntForSight(delta, world, target)) {
            step.set(target).sub(pos);
        } else {
            // sweeps to and fro over the player (left - above - right), never underneath them
            orbitAngle += orbitDir * (speed / orbitRadius) * MathUtils.radiansToDegrees * delta * 0.6f;
            if (orbitAngle > 165f || orbitAngle < 15f) {
                orbitAngle = MathUtils.clamp(orbitAngle, 15f, 165f);
                orbitDir = -orbitDir;
                orbitRadius = MathUtils.random(2.2f, 3.0f);
            }
            target.set(p.centerX() + MathUtils.cosDeg(orbitAngle) * orbitRadius,
                    p.hitboxY() + 0.4f + MathUtils.sinDeg(orbitAngle) * orbitRadius * 0.75f);
            Arena.now.clamp(target, radius + 0.3f);
            // squeezed by a wall: keep some distance anyway (slide along it, away from the player)
            if (target.dst(p.centerX(), p.hitboxY()) < 1.8f) {
                target.x = p.centerX() + (target.x >= p.centerX() ? 1.8f : -1.8f);
                Arena.now.clamp(target, radius + 0.3f);
            }
            step.set(target).sub(pos);
        }
        float len = step.len();
        if (len > 0.02f) {
            step.scl(Math.min(len, speed * delta) / len);
            pos.add(step);
        }
    }

    private void toRide() {
        phase = Phase.RIDE;
        phaseTimer = enraged ? 1.0f : 1.6f;
    }

    private void nextPattern(GameWorld world) {
        Phase[] order = {Phase.TRIANGLES, Phase.SLASHES, Phase.TRIANGLES, Phase.RAIN};
        phase = order[patternIndex++ % order.length];
        switch (phase) {
            case TRIANGLES:
                shotsLeft = enraged ? 4 : 3;
                shotTimer = 0.3f;
                break;
            case SLASHES:
                shotsLeft = enraged ? 4 : 3;
                shotTimer = 0.35f;
                break;
            case RAIN:
                phaseTimer = enraged ? 3.2f : 2.6f;
                rainSpawn = 0f;
                swing = 1f;
                world.sfx().play("buzz", 0.5f);
                break;
            default:
                break;
        }
    }

    /** A little cloud appears over a spot near the player; a moment later it drops a raindrop. */
    private void startRaindrop(GameWorld world) {
        for (int i = 0; i < rainAt.length; i++) {
            if (rainTimer[i] > 0) continue;
            Player p = world.getPlayer();
            rainAt[i].set(p.centerX() + MathUtils.random(-2.4f, 2.4f), p.hitboxY() + MathUtils.random(1.3f, 2.1f));
            Arena.now.clamp(rainAt[i], 0.2f);
            rainTimer[i] = 0.5f;
            return;
        }
    }

    private void updateRain(float delta, GameWorld world, float diff) {
        for (int i = 0; i < rainAt.length; i++) {
            if (rainTimer[i] <= 0) continue;
            rainTimer[i] -= delta;
            if (rainTimer[i] <= 0) {
                Bullet b = world.spawnEnemyBullet(rainAt[i].x, rainAt[i].y - 0.1f, 270f, 2.6f * diff);
                b.shape = Bullet.Shape.DROP;
                b.color.set(RAIN);
                b.radius = 0.06f;
            }
        }
    }

    @Override
    public void drawOverlay(SpriteBatch b, Draw d, com.badlogic.gdx.math.Rectangle view) {
        for (int i = 0; i < rainAt.length; i++) { // the rain clouds
            if (rainTimer[i] <= 0) continue;
            float k = 1f - rainTimer[i] / 0.5f;
            float s = 0.12f + 0.06f * k;
            float x = rainAt[i].x;
            float y = rainAt[i].y;
            d.circle(b, x - s, y, s * 0.8f, CLOUD_SHADE, 0.9f);
            d.circle(b, x + s, y, s * 0.8f, CLOUD_SHADE, 0.9f);
            d.circle(b, x, y + s * 0.3f, s, CLOUD, 0.95f);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bob = MathUtils.sin(time * 2.5f) * 0.05f;
        float x = pos.x;
        float y = pos.y + bob;
        float face = facingRight() ? 1 : -1;
        // the cloud it rides
        float cy = y - 0.42f;
        for (int i = -2; i <= 2; i++) {
            float r = 0.24f - Math.abs(i) * 0.03f;
            d.circle(b, x + i * 0.2f, cy - 0.03f + MathUtils.sin(time * 3f + i) * 0.02f, r + 0.03f, CLOUD_SHADE);
        }
        for (int i = -2; i <= 2; i++) {
            float r = 0.22f - Math.abs(i) * 0.03f;
            d.circle(b, x + i * 0.2f, cy + 0.04f + MathUtils.sin(time * 3f + i) * 0.02f, r, CLOUD);
        }
        // tunic and cape
        float capeWave = MathUtils.sin(time * 6f) * 0.04f;
        d.rect(b, x - face * 0.28f - 0.08f, y - 0.32f + capeWave, 0.16f, 0.46f, c(TUNIC_DARK));
        d.rect(b, x - 0.2f, y - 0.32f, 0.4f, 0.42f, c(TUNIC));
        d.rect(b, x - 0.2f, y - 0.1f, 0.4f, 0.05f, c(GOLD));
        // the magic sword
        float hx = x + face * 0.22f;
        float hy = y - 0.05f;
        float glow = swing > 0 ? 0.5f + 0.5f * swing : 0.25f;
        d.line(b, hx, hy, 0.7f, 0.14f, swordAngle, BLADE);
        d.line(b, hx, hy, 0.7f, 0.06f, swordAngle, Color.WHITE);
        d.circle(b, hx + MathUtils.cosDeg(swordAngle) * 0.7f, hy + MathUtils.sinDeg(swordAngle) * 0.7f, 0.08f + 0.06f * glow, MAGIC, glow);
        d.rotRect(b, hx + MathUtils.cosDeg(swordAngle) * 0.06f, hy + MathUtils.sinDeg(swordAngle) * 0.06f, 0.06f, 0.24f,
                swordAngle, GOLD, 1f); // crossguard
        // fluffy woolly head
        float headY = y + 0.25f;
        for (int i = 0; i < 6; i++) {
            float a = i * 60f + time * 15f;
            d.circle(b, x + MathUtils.cosDeg(a) * 0.14f, headY + MathUtils.sinDeg(a) * 0.12f, 0.12f, c(WOOL));
        }
        d.circle(b, x, headY, 0.16f, c(WOOL));
        drawEyes(b, d, x, headY - 0.02f, 0.06f, 0.035f);
        // the winged gold helmet of a herald
        d.rect(b, x - 0.17f, headY + 0.1f, 0.34f, 0.1f, c(GOLD));
        d.circle(b, x, headY + 0.2f, 0.13f, c(GOLD));
        float flap = MathUtils.sin(time * 9f) * 12f;
        for (int side = -1; side <= 1; side += 2) {
            float wx = x + side * 0.22f;
            float wy = headY + 0.2f;
            d.rotRect(b, wx, wy, 0.2f, 0.07f, side * (25f + flap), WING, 1f);
            d.rotRect(b, wx + side * 0.04f, wy + 0.05f, 0.16f, 0.06f, side * (40f + flap), WING, 1f);
        }
        Color gem = enraged ? Color.RED : BLADE;
        d.circle(b, x, headY + 0.15f, 0.035f, gem);
    }
}
