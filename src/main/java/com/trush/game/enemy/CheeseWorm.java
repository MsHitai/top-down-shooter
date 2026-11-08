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
 * Moon enemy, one of the big ones: a giant cheese worm that tunnels through the moon. Underground it can't be
 * hurt - a mound of moon dust creeps after you - then it stops under where you stand: a red ring, the ground
 * shakes, and it bursts up (inside the ring hurts) in a ring of cheese chunks. Up, it rears and spits fans of
 * chunks at you, and that's when it can be shot - then it dives again. One a room, like the sea's big ones.
 */
public class CheeseWorm extends Enemy {

    private static final Color CHEESE = Color.valueOf("f2c94c");
    private static final Color CHEESE_DARK = Color.valueOf("c89a2a");
    private static final Color HOLE = Color.valueOf("a8781a");
    private static final Color MOUTH = Color.valueOf("5a1a24");
    private static final Color DUST = Color.valueOf("b8b4c4");
    private static final Color CHUNK = Color.valueOf("ffd65a");

    private enum State { UNDER, WARN, UP, DIVE }

    /** How long it tunnels after you before it comes up. */
    private static final float TUNNEL_MIN = 1.6f;
    private static final float TUNNEL_MAX = 2.3f;
    private static final float TUNNEL_SPEED = 2.3f;
    /** The red ring before it bursts up, and how far the burst hurts. */
    static final float WARN_TIME = 0.75f;
    static final float ERUPT_RADIUS = 0.6f;
    private static final float UP_TIME = 2.4f;
    private static final float DIVE_TIME = 0.4f;
    private static final float RISE_TIME = 0.25f;

    private State state = State.UP; // (comes up where it spawns; the book's portrait shows it up too)
    private float timer = 1.2f;
    private float upTime = RISE_TIME;
    private float spitTimer = 0.6f;
    private float clock = MathUtils.random(10f);

    public CheeseWorm(float x, float y) {
        super(x, y, 100f);
        radius = 0.34f;
        speed = TUNNEL_SPEED;
        score = 90;
        flying = true; // (it goes under furniture and craters)
    }

    @Override
    public float feetY() {
        return pos.y - 0.2f;
    }

    /** Underground (or diving / not up yet): bullets pass over it. */
    @Override
    protected boolean isDodging() {
        return state == State.UNDER || state == State.WARN || state == State.DIVE && timer < DIVE_TIME * 0.5f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        clock += delta;
        timer -= delta;
        hurtsToTouch = state == State.UP;
        switch (state) {
            case UNDER: { // the mound creeps after you
                float tx = world.getPlayer().centerX();
                float ty = world.getPlayer().feetY();
                float dist = Vector2.dst(pos.x, pos.y, tx, ty);
                if (dist > 0.05f) {
                    float step = Math.min(dist, TUNNEL_SPEED * Math.min(1.25f, diff) * delta);
                    pos.add((tx - pos.x) / dist * step, (ty - pos.y) / dist * step);
                }
                Arena.now.clamp(pos, radius);
                if (MathUtils.random() < delta * 12f) {
                    world.particles().spawn(pos.x + MathUtils.random(-0.2f, 0.2f), pos.y - 0.1f, 0, 0.3f, 0.5f, 0.04f, DUST, 1f);
                }
                if (timer <= 0 || dist < 0.15f && timer < TUNNEL_MAX - 0.8f) {
                    state = State.WARN;
                    timer = WARN_TIME / Math.min(1.2f, diff);
                    world.sfx().play("buzz", 0.4f);
                }
                break;
            }
            case WARN: // it's stopped: the ground shakes
                if (MathUtils.random() < delta * 20f) {
                    world.particles().spawn(pos.x + MathUtils.random(-0.4f, 0.4f), pos.y + MathUtils.random(-0.3f, 0.3f), 0,
                            0.4f, 0.4f, 0.04f, DUST, 1f);
                }
                if (timer <= 0) {
                    erupt(world, diff);
                    state = State.UP;
                    timer = UP_TIME;
                    upTime = 0f;
                    spitTimer = 0.7f;
                }
                break;
            case UP:
                upTime += delta;
                spitTimer -= delta;
                if (spitTimer <= 0 && canShoot(world)) {
                    spitTimer = 0.8f / diff;
                    int n = diff > 1.3f ? 6 : 5;
                    for (int i = 0; i < n; i++) {
                        Bullet b = shoot(world, aimAngle + (i - (n - 1) / 2f) * 13f, 2.4f * diff);
                        b.color.set(CHUNK);
                        b.shape = Bullet.Shape.SQUARE;
                    }
                    world.sfx().play("pop", 0.5f);
                }
                if (timer <= 0) {
                    state = State.DIVE;
                    timer = DIVE_TIME;
                }
                break;
            default: // DIVE: back down
                if (timer <= 0) {
                    state = State.UNDER;
                    timer = MathUtils.random(TUNNEL_MIN, TUNNEL_MAX);
                    world.particles().burst(pos.x, pos.y - 0.15f, 8, 0.2f, 1f, 0.4f, 0.04f, DUST);
                }
                break;
        }
    }

    /** Up through the ground: inside the ring hurts, and cheese chunks fly out all round. */
    private void erupt(GameWorld world, float diff) {
        float px = world.getPlayer().centerX();
        float py = world.getPlayer().hitboxY();
        if (Vector2.dst(pos.x, pos.y, px, py) < ERUPT_RADIUS && !world.getPlayer().isRolling()) {
            world.hurtPlayer();
        }
        int n = diff > 1.3f ? 16 : 14;
        float off = MathUtils.random(360f);
        for (int i = 0; i < n; i++) {
            Bullet b = world.spawnEnemyBullet(pos.x, pos.y, off + 360f * i / n, 1.8f * diff);
            b.color.set(CHUNK);
            b.shape = Bullet.Shape.SQUARE;
        }
        world.particles().burst(pos.x, pos.y, 16, 0.3f, 1.6f, 0.5f, 0.05f, DUST);
        world.shake(0.25f);
        world.sfx().play("boom", 0.5f);
    }

    /** Underground: its mound of moon dust; about to come up: the red ring. */
    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (!alive) return;
        if (state == State.UNDER || state == State.WARN) {
            float shake = state == State.WARN ? MathUtils.sin(clock * 60f) * 0.03f : 0f;
            d.ellipse(b, pos.x + shake, pos.y - 0.12f, 0.3f, 0.12f, Draw.OUTLINE_COLOR, 0.5f);
            d.ellipse(b, pos.x + shake, pos.y - 0.1f, 0.26f, 0.1f, DUST, 1f);
            d.ellipse(b, pos.x + shake - 0.05f, pos.y - 0.07f, 0.12f, 0.04f, Color.WHITE, 0.3f);
        }
        if (state == State.WARN) {
            float k = 1f - Math.max(0f, timer) / WARN_TIME;
            d.circle(b, pos.x, pos.y, ERUPT_RADIUS * k, TELEGRAPH, 0.2f);
            d.ring(b, pos.x, pos.y, ERUPT_RADIUS, TELEGRAPH, 0.8f);
        }
        if (state == State.UP || state == State.DIVE) { // the hole it came out of
            d.ellipse(b, pos.x, pos.y - 0.18f, 0.34f, 0.13f, DUST, 1f);
            d.ellipse(b, pos.x, pos.y - 0.18f, 0.26f, 0.09f, Draw.OUTLINE_COLOR, 1f);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        if (state == State.UNDER || state == State.WARN) {
            return; // (just the mound, on the floor)
        }
        // how far out of the ground it is
        float out = state == State.DIVE ? Math.max(0f, timer / DIVE_TIME) : Math.min(1f, upTime / RISE_TIME);
        float sway = MathUtils.sin(clock * 3f) * 0.05f;
        float face = MathUtils.cosDeg(aimAngle) >= 0 ? 1f : -1f;
        float baseY = pos.y - 0.18f;
        int segments = 4;
        float hx = 0;
        float hy = 0;
        for (int i = 0; i < segments; i++) { // bottom up: fat cheesy rings, swaying
            float k = (i + 1f) / segments;
            float sy = baseY + 0.16f * i * out;
            float sx = pos.x + sway * k * 2f;
            float r = 0.2f - 0.015f * i;
            d.circle(b, sx, sy, r + 0.02f, Draw.OUTLINE_COLOR);
            d.circle(b, sx, sy, r, c(i % 2 == 0 ? CHEESE : CHEESE_DARK));
            d.circle(b, sx - r * 0.4f, sy + r * 0.2f, r * 0.2f, c(HOLE));
            d.circle(b, sx + r * 0.35f, sy - r * 0.3f, r * 0.14f, c(HOLE));
            hx = sx;
            hy = sy;
        }
        // the head: a round mouth with teeth, turned towards you, tiny glowing eyes
        float headY = hy + 0.2f * out;
        float headX = hx + face * 0.04f;
        d.circle(b, headX, headY, 0.21f, Draw.OUTLINE_COLOR);
        d.circle(b, headX, headY, 0.19f, c(CHEESE));
        float open = state == State.UP ? 0.08f + 0.03f * MathUtils.sin(clock * 10f) : 0.05f;
        float mx = headX + face * 0.07f;
        d.circle(b, mx, headY - 0.02f, open + 0.02f, Draw.OUTLINE_COLOR);
        d.circle(b, mx, headY - 0.02f, open, MOUTH);
        for (int i = 0; i < 6; i++) { // teeth round the mouth
            float a = i * 60f + clock * 30f;
            d.circle(b, mx + MathUtils.cosDeg(a) * open, headY - 0.02f + MathUtils.sinDeg(a) * open, 0.018f, Color.WHITE);
        }
        for (int s = -1; s <= 1; s += 2) {
            d.circle(b, headX - face * 0.06f + s * 0.05f, headY + 0.11f, 0.025f, Draw.OUTLINE_COLOR);
            d.circle(b, headX - face * 0.06f + s * 0.05f, headY + 0.11f, 0.016f, TELEGRAPH);
        }
    }
}
