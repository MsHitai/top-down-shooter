package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * The red Shotgun Kin's blue cousin: pumps twice - a wide fan of slow pellets, then a second fan that
 * fills the gaps of the first - and when it's shot down it pops, its last pellets flying out in a ring.
 */
public class BlueShotgunKin extends Enemy {

    private static final Color SHELL = Color.valueOf("2f6fc0");
    private static final Color SHELL_DARK = Color.valueOf("1f4a8a");
    private static final Color BRASS = Color.valueOf("d9b24c");
    private static final Color PELLET = Color.valueOf("8ac4ff");

    private static final float W = 0.42f;
    private static final float H = 0.56f;
    private static final float WINDUP = 0.55f;
    private static final float PUMP_GAP = 0.35f;
    static final int PELLETS = 6;
    private static final float FAN = 64f;
    /** Pellets in the ring it pops into. */
    static final int DEATH_RING = 8;

    private float fireTimer = MathUtils.random(1.8f, 2.8f);
    private float windup;
    /** The second pump is due in this long (0: none coming). */
    private float secondPump;
    private float pump;

    public BlueShotgunKin(float x, float y) {
        super(x, y, 20f);
        radius = 0.23f;
        speed = 0.7f;
        score = 30;
    }

    @Override
    public float feetY() {
        return pos.y - H / 2f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        pump = Math.max(0f, pump - delta);
        if (secondPump > 0) {
            secondPump -= delta;
            if (secondPump <= 0) {
                fan(world, PELLETS - 1, FAN * (PELLETS - 2) / (PELLETS - 1), diff); // between the first fan's pellets
                fireTimer = MathUtils.random(2.4f, 3.2f) / diff;
            }
            return;
        }
        if (windup > 0) {
            windup -= delta;
            if (windup <= 0) {
                fan(world, PELLETS, FAN, diff);
                secondPump = PUMP_GAP;
            }
            return;
        }
        keepDistance(delta, world, 1.9f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            windup = WINDUP;
        }
    }

    private void fan(GameWorld world, int count, float width, float diff) {
        for (int i = 0; i < count; i++) {
            float a = aimAngle - width / 2f + width * i / (count - 1);
            shoot(world, a, 2.0f * diff).color.set(PELLET);
        }
        pump = 0.2f;
        world.sfx().play("enemy_shoot", 0.7f);
    }

    /** Pops: a ring of pellets out of the shell. */
    @Override
    public void dropLoot(GameWorld world) {
        float turn = MathUtils.random(360f);
        for (int i = 0; i < DEATH_RING; i++) {
            Bullet b = world.spawnEnemyBullet(pos.x, pos.y, turn + i * 360f / DEATH_RING, 1.7f);
            b.color.set(PELLET);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float left = pos.x - W / 2f;
        drawFeet(b, d, pos.x, bottom, 0.1f);
        d.rect(b, left, bottom + H * 0.2f, W, H * 0.8f, c(SHELL));
        d.rect(b, left, bottom + H * 0.88f, W, H * 0.12f, c(SHELL_DARK));
        d.rect(b, left + W - 0.05f, bottom + H * 0.2f, 0.05f, H * 0.68f, c(SHELL_DARK));
        d.rect(b, left - 0.015f, bottom, W + 0.03f, H * 0.22f, c(BRASS));
        d.rect(b, left - 0.015f, bottom + H * 0.08f, W + 0.03f, 0.025f, c(SHELL_DARK));
        drawEyes(b, d, pos.x, bottom + H * 0.62f, 0.075f, 0.043f);

        // a pump shotgun: the slide jerks back after each shot
        float face = facingRight() ? 1 : -1;
        float gx = pos.x + face * 0.14f;
        float gy = bottom + H * 0.35f;
        d.line(b, gx, gy, 0.28f, 0.07f, aimAngle, GUN);
        float slide = 0.08f - pump * 0.25f;
        d.line(b, gx + MathUtils.cosDeg(aimAngle) * slide, gy + MathUtils.sinDeg(aimAngle) * slide - 0.03f, 0.09f, 0.045f,
                aimAngle, SHELL_DARK);
        if (windup > 0) {
            float mx = gx + MathUtils.cosDeg(aimAngle) * 0.28f;
            float my = gy + MathUtils.sinDeg(aimAngle) * 0.28f;
            d.circle(b, mx, my, 0.06f + 0.04f * (1 - windup / WINDUP), TELEGRAPH, 0.9f);
        }
    }
}
