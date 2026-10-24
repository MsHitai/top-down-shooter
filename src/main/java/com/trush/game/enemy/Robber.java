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
 * City enemy: a robber - a bullet in a striped shirt and a black eye mask. Fires a couple of quick
 * shots, and when your bullets come at it, it dodge-rolls out of the way (bullets fly right through
 * it mid-roll), twice in a row at most before it needs a breather - so a steady stream catches it.
 */
public class Robber extends Enemy {

    private static final Color STRIPE_DARK = Color.valueOf("26232b");
    private static final Color STRIPE_LIGHT = Color.valueOf("e8e4dc");
    private static final Color BRASS = Color.valueOf("c9a24a");
    private static final Color MASK = Color.valueOf("111015");
    private static final Color BEANIE = Color.valueOf("3a3542");
    private static final Color BAG = Color.valueOf("8a7a55");
    private static final Color BULLET = Color.valueOf("ffd166");

    private static final float W = 0.34f;
    private static final float H = 0.52f;
    private static final float WINDUP = 0.35f;
    private static final float ROLL_TIME = 0.32f;
    private static final float ROLL_SPEED = 4.2f;

    private float fireTimer = MathUtils.random(1.2f, 2.2f);
    private float windup;
    private int shotsLeft;
    private float shotTimer;
    private float rollTimer;
    private float rollCooldown;
    private int rollsLeft = 2;
    private final Vector2 rollDir = new Vector2();
    private final Vector2 tmp = new Vector2();

    public Robber(float x, float y) {
        super(x, y, 24f);
        radius = 0.19f;
        speed = 1.1f;
        score = 35;
    }

    @Override
    public float feetY() {
        return pos.y - H * 0.42f;
    }

    @Override
    protected boolean isDodging() {
        return rollTimer > 0;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        rollCooldown -= delta;
        if (rollTimer > 0) {
            rollTimer -= delta;
            pos.mulAdd(rollDir, ROLL_SPEED * delta);
            walkTime += delta;
            if (MathUtils.randomBoolean(0.4f)) {
                world.particles().spawn(pos.x, feetY(), 0, 0.1f, 0.25f, 0.04f, Color.GRAY, 1f);
            }
            return;
        }
        if (rollCooldown <= 0 && dodgeFrom(world)) {
            return;
        }
        if (shotsLeft > 0) { // a quick double tap
            shotTimer -= delta;
            if (shotTimer <= 0) {
                shotTimer = 0.14f;
                shotsLeft--;
                shoot(world, aimAngle + MathUtils.random(-4f, 4f), 3.1f * diff).color.set(BULLET);
                world.sfx().play("enemy_shoot", 0.5f);
                if (shotsLeft == 0) {
                    fireTimer = MathUtils.random(1.4f, 2.2f) / diff;
                }
            }
            return;
        }
        if (windup > 0) {
            windup -= delta;
            if (windup <= 0) {
                shotsLeft = diff > 1.3f ? 3 : 2;
                shotTimer = 0;
            }
            return;
        }
        keepDistance(delta, world, 2.4f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            windup = WINDUP;
        }
    }

    /** A player bullet about to hit: roll sideways out of its path. Returns true if it rolled. */
    private boolean dodgeFrom(GameWorld world) {
        for (Bullet b : world.getPlayerBullets()) {
            if (!b.alive) continue;
            tmp.set(pos).sub(b.pos);
            float dist = tmp.len();
            if (dist > 1.4f || dist < 0.05f) continue;
            float speed = b.vel.len();
            if (speed < 0.5f) continue;
            // heading for us, and would pass close by?
            float along = (tmp.x * b.vel.x + tmp.y * b.vel.y) / speed;
            float side = (tmp.x * b.vel.y - tmp.y * b.vel.x) / speed;
            if (along < 0 || Math.abs(side) > radius + 0.25f) continue;
            // roll to the side the bullet is already passing on (or a random one dead centre)
            float s = Math.abs(side) < 0.02f ? MathUtils.randomSign() : Math.signum(side);
            rollDir.set(b.vel.y, -b.vel.x).nor().scl(s);
            tmp.set(pos).mulAdd(rollDir, ROLL_SPEED * ROLL_TIME);
            if (!Arena.now.contains(tmp.x, tmp.y, radius) || !world.walkable(pos.x, pos.y, tmp.x, tmp.y, radius)) {
                rollDir.scl(-1f); // that way's blocked: the other side
                tmp.set(pos).mulAdd(rollDir, ROLL_SPEED * ROLL_TIME);
                if (!Arena.now.contains(tmp.x, tmp.y, radius) || !world.walkable(pos.x, pos.y, tmp.x, tmp.y, radius)) {
                    continue;
                }
            }
            rollTimer = ROLL_TIME;
            windup = 0;
            shotsLeft = 0;
            rollsLeft--;
            rollCooldown = rollsLeft > 0 ? 0.45f : 2.6f;
            if (rollsLeft <= 0) {
                rollsLeft = 2;
            }
            world.sfx().play("dodge", 0.5f);
            return true;
        }
        return false;
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float left = pos.x - W / 2f;
        if (rollTimer > 0) { // tucked into a spinning ball
            float spin = (1f - rollTimer / ROLL_TIME) * 360f * Math.signum(rollDir.x == 0 ? 1 : rollDir.x) * -1f;
            float cy = bottom + 0.18f;
            d.circle(b, pos.x, cy, 0.2f, c(STRIPE_DARK));
            for (int i = 0; i < 3; i++) {
                d.rotRect(b, pos.x, cy, 0.38f, 0.05f, spin + i * 60f, c(STRIPE_LIGHT), 1f);
            }
            d.circle(b, pos.x, cy, 0.09f, c(BRASS));
            return;
        }
        drawFeet(b, d, pos.x, bottom, 0.08f);
        // loot bag on its back
        float face = facingRight() ? 1 : -1;
        d.circle(b, pos.x - face * 0.17f, bottom + H * 0.4f, 0.1f, c(BAG));
        // striped shirt
        d.rect(b, left, bottom, W, H * 0.62f, c(STRIPE_LIGHT));
        for (int i = 0; i < 4; i++) {
            d.rect(b, left, bottom + 0.02f + i * 0.08f, W, 0.04f, c(STRIPE_DARK));
        }
        // brass head with a black eye mask and a beanie
        d.circle(b, pos.x, bottom + H * 0.66f, W / 2f, c(BRASS));
        d.rect(b, left, bottom + H * 0.56f, W, 0.08f, MASK);
        drawEyes(b, d, pos.x, bottom + H * 0.6f, 0.065f, 0.035f);
        d.rect(b, left + 0.01f, bottom + H * 0.78f, W - 0.02f, 0.1f, c(BEANIE));
        d.circle(b, pos.x, bottom + H * 0.9f, 0.035f, c(BEANIE));

        float gx = pos.x + face * 0.13f;
        float gy = bottom + H * 0.3f;
        d.line(b, gx, gy, 0.17f, 0.055f, aimAngle, GUN);
        if (windup > 0) {
            float mx = gx + MathUtils.cosDeg(aimAngle) * 0.17f;
            float my = gy + MathUtils.sinDeg(aimAngle) * 0.17f;
            d.circle(b, mx, my, 0.045f + 0.03f * (1 - windup / WINDUP), TELEGRAPH, 0.9f);
        }
    }
}
