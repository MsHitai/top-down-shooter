package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * Town boss: a living traffic light that plays "Red Light, Green Light".
 * <ul>
 *   <li>GREEN - go: glides after the player firing rapid streams of bullets;</li>
 *   <li>YELLOW - careful: stops and blinks, red is coming;</li>
 *   <li>RED - stop: every bullet on screen freezes and two rings of frozen bullets appear around it.
 *       Move (or roll) while it's red and it fires a fast "ticket" at you. When red ends, everything
 *       frozen bursts outward from the light.</li>
 * </ul>
 * Enraged: four streams, shorter yellow, faster bursts.
 */
public class TrafficLight extends Enemy {

    private enum Light { GREEN, YELLOW, RED }

    private static final Color HOUSING = Color.valueOf("2e3a2e");
    private static final Color HOUSING_LIGHT = Color.valueOf("4a5a48");
    private static final Color POLE = Color.valueOf("5a5a66");
    private static final Color GREEN_ON = Color.valueOf("5ae06a");
    private static final Color YELLOW_ON = Color.valueOf("ffd23a");
    private static final Color RED_ON = Color.valueOf("ff4a3a");
    private static final Color OFF = Color.valueOf("1e221e");
    private static final Color PUPIL = Color.valueOf("111111");

    private static final float GREEN_TIME = 3.2f;
    private static final float RED_TIME = 2.2f;
    /** The red lamp flashes this long before everything bursts. */
    private static final float BURST_WARN = 0.45f;

    // starts on yellow: a calm moment to read it, then the first red teaches "stop" before any streams
    private Light light = Light.YELLOW;
    private float lightTimer = 1.8f;
    private float shotTimer;
    private float ticketTimer;
    private float streamAngle;
    private boolean enraged;
    private boolean spotted;
    private float time;
    private final Vector2 target = new Vector2();
    private final Vector2 step = new Vector2();
    /** Bullets frozen by the red light; they burst outward when it ends. */
    private final Array<Bullet> frozen = new Array<>();

    public TrafficLight(float x, float y, int tier) {
        super(x, y, 500f + 160f * tier);
        radius = 0.36f;
        speed = 1.0f;
        score = 500 + 250 * tier;
        boss = true;
        target.set(x, y);
    }

    @Override
    public String bossName() {
        return Boss.TRAFFIC_LIGHT.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    @Override
    public float feetY() {
        return pos.y - 0.5f;
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
        lightTimer -= delta;
        shotTimer -= delta;

        switch (light) {
            case GREEN:
                glide(delta, world);
                if (shotTimer <= 0 && canSeePlayer) {
                    shotTimer = (enraged ? 0.07f : 0.09f) / diff;
                    int streams = enraged ? 4 : 3;
                    streamAngle += 0.35f; // the streams sway a little from shot to shot
                    for (int i = 0; i < streams; i++) {
                        float a = aimAngle + (i - (streams - 1) / 2f) * 28f + MathUtils.sin(streamAngle) * 10f;
                        shoot(world, a, 2.6f * diff).color.set(GREEN_ON);
                    }
                    world.sfx().play("smg", 0.35f);
                }
                if (lightTimer <= 0) {
                    switchTo(Light.YELLOW, world);
                }
                break;
            case YELLOW:
                if (lightTimer <= 0) {
                    switchTo(Light.RED, world);
                }
                break;
            case RED:
                red(delta, world, diff);
                if (lightTimer <= 0) {
                    burst(world, diff);
                    switchTo(Light.GREEN, world);
                }
                break;
        }
    }

    /** On green it glides toward a spot a couple of units from the player (or around cover to see them). */
    private void glide(float delta, GameWorld world) {
        Player p = world.getPlayer();
        boolean hunting = huntForSight(delta, world, target);
        if (!hunting && (pos.dst2(target) < 0.05f || target.dst(p.centerX(), p.hitboxY()) < 1.6f)) {
            float a = MathUtils.random(360f);
            target.set(p.centerX() + MathUtils.cosDeg(a) * 2.3f, p.hitboxY() + MathUtils.sinDeg(a) * 2.3f);
            Arena.now.clamp(target, radius + 0.4f);
        }
        step.set(target).sub(pos);
        if (step.len() > 0.02f) {
            step.nor().scl(speed * (enraged ? 1.3f : 1f) * delta);
            pos.add(step);
            walkTime += delta;
        }
    }

    /** Red: watches the player - anyone who moves gets a ticket. */
    private void red(float delta, GameWorld world, float diff) {
        Player p = world.getPlayer();
        boolean moving = !p.isDead() && (p.isRolling() || !p.getMoveDir().isZero());
        // a short grace period right after it turns red, to react
        spotted = moving && lightTimer < RED_TIME - 0.35f && lightTimer > BURST_WARN;
        ticketTimer -= delta;
        if (spotted && ticketTimer <= 0) {
            ticketTimer = 0.35f;
            Bullet b = shoot(world, aimAngle, 4.2f * diff);
            b.color.set(RED_ON);
            b.radius = 0.07f;
            world.sfx().play("enemy_shoot", 1f);
        }
    }

    private void switchTo(Light next, GameWorld world) {
        light = next;
        switch (next) {
            case GREEN:
                lightTimer = GREEN_TIME * (enraged ? 0.85f : 1f);
                world.toast("GREEN LIGHT - GO!");
                break;
            case YELLOW:
                lightTimer = enraged ? 0.8f : 1.2f;
                break;
            case RED:
                lightTimer = RED_TIME;
                ticketTimer = 0;
                freeze(world);
                world.toast("RED LIGHT - STOP!");
                world.sfx().play("door", 1f);
                break;
        }
    }

    /** Every enemy bullet stops dead, and two rings of frozen bullets appear around the light. */
    private void freeze(GameWorld world) {
        frozen.clear();
        for (Bullet b : world.getEnemyBullets()) {
            if (b.alive) {
                hold(b);
            }
        }
        for (int ring = 0; ring < 2; ring++) {
            int n = ring == 0 ? 12 : 18;
            float r = ring == 0 ? 0.65f : 1.15f;
            float off = ring * 10f;
            for (int i = 0; i < n; i++) {
                float a = off + 360f * i / n;
                float x = pos.x + MathUtils.cosDeg(a) * r;
                float y = pos.y + MathUtils.sinDeg(a) * r;
                if (!Arena.now.contains(x, y, 0.05f)) continue;
                Bullet b = world.spawnEnemyBullet(x, y, a, 0.01f);
                b.color.set(RED_ON);
                hold(b);
            }
        }
    }

    private void hold(Bullet b) {
        b.vel.setZero();
        b.accel = 0;
        b.curve = 0;
        b.life = Math.max(b.life, RED_TIME + 1f);
        frozen.add(b);
    }

    /** Red is over: everything frozen flies outward, away from the light. */
    private void burst(GameWorld world, float diff) {
        float speed = (enraged ? 2.4f : 2.0f) * diff;
        for (Bullet b : frozen) {
            if (!b.alive) continue;
            float a = MathUtils.atan2Deg360(b.pos.y - pos.y, b.pos.x - pos.x);
            b.vel.set(MathUtils.cosDeg(a), MathUtils.sinDeg(a)).scl(speed);
            b.life = 6f;
        }
        frozen.clear();
        spotted = false;
        world.shake(0.3f);
        world.sfx().play("blank", 0.6f);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float cx = pos.x;
        float bottom = feetY();
        float bob = light == Light.GREEN ? MathUtils.sin(walkTime * 8f) * 0.02f : 0f;
        // little feet and the pole
        float stride = MathUtils.sin(walkTime * 10f) * 0.03f;
        d.rect(b, cx - 0.12f, bottom + stride, 0.1f, 0.05f, c(POLE));
        d.rect(b, cx + 0.02f, bottom - stride, 0.1f, 0.05f, c(POLE));
        d.rect(b, cx - 0.04f, bottom + 0.04f, 0.08f, 0.3f, c(POLE));
        // housing with three lamps
        float hx = cx - 0.2f;
        float hy = bottom + 0.3f + bob;
        float hw = 0.4f;
        float hh = 0.86f;
        d.rect(b, hx - 0.02f, hy - 0.02f, hw + 0.04f, hh + 0.04f, Color.BLACK);
        d.rect(b, hx, hy, hw, hh, c(HOUSING));
        d.rect(b, hx, hy, 0.05f, hh, c(HOUSING_LIGHT));
        boolean yellowBlink = light == Light.YELLOW && ((int) (time * 6f)) % 2 == 0;
        boolean redFlash = light == Light.RED && lightTimer < BURST_WARN && ((int) (time * 14f)) % 2 == 0;
        Color[] lamps = {
                light == Light.RED && !redFlash ? RED_ON : redFlash ? Color.WHITE : OFF,
                yellowBlink ? YELLOW_ON : OFF,
                light == Light.GREEN ? GREEN_ON : OFF,
        };
        Color[] glows = {RED_ON, YELLOW_ON, GREEN_ON};
        for (int i = 0; i < 3; i++) {
            float ly = hy + hh - 0.15f - i * 0.28f;
            boolean on = lamps[i] != OFF;
            if (on) {
                d.circle(b, cx, ly, 0.2f, glows[i], 0.3f);
            }
            d.rect(b, cx - 0.14f, ly + 0.1f, 0.28f, 0.04f, Color.BLACK); // visor
            d.circle(b, cx, ly, 0.11f, flash > 0 ? Color.WHITE : lamps[i]);
            if (on) { // the lit lamp is its face
                float look = MathUtils.cosDeg(aimAngle) * 0.02f;
                float eyeY = ly + 0.01f;
                d.circle(b, cx - 0.045f + look, eyeY, 0.025f, PUPIL);
                d.circle(b, cx + 0.045f + look, eyeY, 0.025f, PUPIL);
                if (light == Light.RED) { // angry brows
                    d.rotRect(b, cx - 0.045f, eyeY + 0.045f, 0.06f, 0.015f, -20f, PUPIL, 1f);
                    d.rotRect(b, cx + 0.045f, eyeY + 0.045f, 0.06f, 0.015f, 20f, PUPIL, 1f);
                }
            }
        }
        // spotted you moving on red: a big red "!"
        if (spotted) {
            float ex = cx + 0.32f;
            float ey = hy + hh;
            d.rect(b, ex - 0.03f, ey, 0.06f, 0.18f, RED_ON);
            d.rect(b, ex - 0.03f, ey - 0.08f, 0.06f, 0.05f, RED_ON);
        }
    }
}
