package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * Moon enemy, one of the big ones (the owner's idea after the sea's crabs and octopuses made it hard): a
 * moonrock golem, grey boulders held together by glowing cheese-yellow cracks. It plods after you and takes
 * turns: both fists up - a red ring shows how far the slam reaches - and down: anything inside gets hurt and a
 * ring of rock shards flies out (a second one into the gaps on harder floors); or it tears up a boulder and
 * hurls it after you, steering it a little, and it bursts into shards. One a room, like the sea's big ones.
 */
public class MoonGolem extends Enemy {

    private static final Color ROCK = Color.valueOf("8a8796");
    private static final Color ROCK_DARK = Color.valueOf("5c5a68");
    private static final Color ROCK_LIGHT = Color.valueOf("b4b1c0");
    private static final Color GLOW = Color.valueOf("ffd65a");
    private static final Color SHARD = Color.valueOf("c8c4d6");

    private enum State { WALK, RAISE, SLAM, LIFT, THROWN }

    /** How far the slam hurts (the red ring), and how long it shows first. */
    static final float SLAM_RADIUS = 1.05f;
    static final float SLAM_WARN = 0.85f;
    private static final float LIFT_TIME = 0.6f;
    /** The thrown boulder bursts this long after it leaves the hands (or when it hits something). */
    static final float BOULDER_BURST = 1.0f;
    private static final float RECOVER = 0.6f;

    private State state = State.WALK;
    private float timer = MathUtils.random(1.4f, 2.2f);
    private boolean nextSlam = MathUtils.randomBoolean();
    private float clock = MathUtils.random(10f);
    private Bullet boulder;
    private float boulderTime;
    private float recover;
    private final Vector2 tmp = new Vector2();

    public MoonGolem(float x, float y) {
        super(x, y, 120f);
        radius = 0.4f;
        speed = 0.6f;
        score = 90;
    }

    @Override
    public float feetY() {
        return pos.y - 0.32f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        clock += delta;
        updateBoulder(delta, world, diff);
        if (recover > 0) {
            recover -= delta;
            return;
        }
        timer -= delta;
        switch (state) {
            case WALK:
                keepDistance(delta, world, 1.3f, speed);
                if (timer <= 0 && canShoot(world)) {
                    boolean close = distanceToPlayer(world) < 2.4f;
                    // close in, it slams; further off, it throws (and takes turns when it can't choose)
                    boolean slam = close || nextSlam && distanceToPlayer(world) < 3.2f;
                    nextSlam = !slam;
                    state = slam ? State.RAISE : State.LIFT;
                    timer = slam ? SLAM_WARN / Math.min(1.25f, diff) : LIFT_TIME;
                    world.sfx().play("buzz", 0.4f);
                }
                break;
            case RAISE: // fists up, the ring on the floor
                if (timer <= 0) {
                    slam(world, diff);
                    state = State.SLAM;
                    timer = 0.25f;
                }
                break;
            case SLAM:
                if (timer <= 0) {
                    state = State.WALK;
                    recover = RECOVER;
                    timer = MathUtils.random(2f, 2.8f) / diff;
                }
                break;
            case LIFT: // tearing a boulder out of the ground
                if (timer <= 0) {
                    float a = aimAngle;
                    boulder = world.spawnEnemyBullet(pos.x, pos.y + 0.45f, a, 2.1f * diff);
                    boulder.radius = 0.16f;
                    boulder.color.set(ROCK);
                    boulder.knockback = 1f;
                    boulderTime = 0f;
                    world.sfx().play("swish", 0.6f);
                    state = State.THROWN;
                    timer = 0.4f;
                }
                break;
            default: // THROWN: a moment to follow through
                if (timer <= 0) {
                    state = State.WALK;
                    timer = MathUtils.random(2f, 2.8f) / diff;
                }
                break;
        }
    }

    /** Both fists down: inside the ring hurts, and rock shards fly out of it. */
    private void slam(GameWorld world, float diff) {
        float py = world.getPlayer().hitboxY();
        float px = world.getPlayer().centerX();
        if (Vector2.dst(pos.x, pos.y - 0.2f, px, py) < SLAM_RADIUS && !world.getPlayer().isRolling()) {
            world.hurtPlayer();
        }
        int n = 16;
        float off = MathUtils.random(360f);
        for (int i = 0; i < n; i++) {
            Bullet s = world.spawnEnemyBullet(pos.x, pos.y - 0.2f, off + 360f * i / n, 1.9f * diff);
            s.shape = Bullet.Shape.SQUARE;
            s.color.set(SHARD);
        }
        if (diff > 1.3f) { // and a slower one into the gaps
            for (int i = 0; i < n; i++) {
                Bullet s = world.spawnEnemyBullet(pos.x, pos.y - 0.2f, off + 360f * (i + 0.5f) / n, 1.3f * diff);
                s.shape = Bullet.Shape.SQUARE;
                s.color.set(SHARD);
            }
        }
        world.particles().burst(pos.x, pos.y - 0.25f, 16, 0.3f, 1.6f, 0.5f, 0.05f, ROCK_LIGHT);
        world.shake(0.3f);
        world.sfx().play("boom", 0.6f);
    }

    /** The boulder steers after the hero a little, and bursts into shards (not when a blank swept it away). */
    private void updateBoulder(float delta, GameWorld world, float diff) {
        if (boulder == null) {
            return;
        }
        boulderTime += delta;
        if (boulder.alive && boulderTime < BOULDER_BURST) {
            float want = MathUtils.atan2Deg360(world.getPlayer().hitboxY() - boulder.pos.y,
                    world.getPlayer().centerX() - boulder.pos.x);
            float now = boulder.vel.angleDeg();
            float turn = MathUtils.clamp(((want - now + 540f) % 360f) - 180f, -50f * delta, 50f * delta);
            boulder.vel.rotateDeg(turn);
            return;
        }
        Bullet b = boulder;
        boulder = null;
        if (b.blanked || world.getPlayer().isInvulnerable()) {
            b.alive = false;
            return;
        }
        b.alive = false;
        tmp.set(b.vel).nor().scl(-0.15f).add(b.pos); // step back out of whatever it hit
        int n = diff > 1.3f ? 10 : 8;
        float off = MathUtils.random(360f);
        for (int i = 0; i < n; i++) {
            Bullet s = world.spawnEnemyBullet(tmp.x, tmp.y, off + 360f * i / n, 2.2f * diff);
            s.shape = Bullet.Shape.SQUARE;
            s.color.set(SHARD);
        }
        world.particles().burst(tmp.x, tmp.y, 8, 0.2f, 1.2f, 0.4f, 0.04f, ROCK_LIGHT);
        world.sfx().play("pop", 0.6f);
    }

    /** The slam's reach, filling up red before it lands. */
    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (state == State.RAISE && alive) {
            float k = 1f - Math.max(0f, timer) / SLAM_WARN;
            d.circle(b, pos.x, pos.y - 0.2f, SLAM_RADIUS * k, TELEGRAPH, 0.18f);
            d.ring(b, pos.x, pos.y - 0.2f, SLAM_RADIUS, TELEGRAPH, 0.75f);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float x = pos.x;
        float bob = state == State.WALK ? Math.abs(MathUtils.sin(walkTime * 5f)) * 0.03f : 0f;
        float y = pos.y + bob;
        boolean up = state == State.RAISE || state == State.LIFT;
        float pulse = 0.6f + 0.4f * MathUtils.sin(clock * 4f);
        // legs: two stubby boulders, stomping in turn
        for (int s = -1; s <= 1; s += 2) {
            float lift = state == State.WALK ? Math.max(0f, MathUtils.sin(walkTime * 5f + (s > 0 ? 0f : MathUtils.PI))) * 0.05f : 0f;
            d.circle(b, x + s * 0.17f, pos.y - 0.24f + lift, 0.13f, Draw.OUTLINE_COLOR);
            d.circle(b, x + s * 0.17f, pos.y - 0.24f + lift, 0.115f, c(ROCK_DARK));
        }
        // the body: a big boulder with glowing cracks
        d.circle(b, x, y, 0.36f, Draw.OUTLINE_COLOR);
        d.circle(b, x, y, 0.34f, c(ROCK));
        d.circle(b, x - 0.1f, y + 0.12f, 0.12f, c(ROCK_LIGHT), 0.6f);
        d.circle(b, x + 0.14f, y - 0.12f, 0.09f, c(ROCK_DARK), 0.8f);
        d.line(b, x - 0.2f, y - 0.05f, 0.22f, 0.03f, 25f, GLOW, pulse);
        d.line(b, x + 0.02f, y + 0.04f, 0.18f, 0.025f, -40f, GLOW, pulse);
        d.line(b, x + 0.05f, y - 0.2f, 0.14f, 0.025f, 70f, GLOW, pulse);
        // the head: a smaller rock with two glowing eyes
        float hy = y + 0.36f;
        d.circle(b, x, hy, 0.17f, Draw.OUTLINE_COLOR);
        d.circle(b, x, hy, 0.155f, c(ROCK));
        float look = MathUtils.cosDeg(aimAngle) * 0.02f;
        for (int s = -1; s <= 1; s += 2) {
            d.circle(b, x + s * 0.06f + look, hy + 0.01f, 0.035f, GLOW, 0.4f);
            d.circle(b, x + s * 0.06f + look, hy + 0.01f, 0.022f, up ? TELEGRAPH : GLOW);
        }
        // the arms: down by its sides, or both fists up over its head
        for (int s = -1; s <= 1; s += 2) {
            float fx = up ? x + s * 0.24f : state == State.SLAM ? x + s * 0.3f : x + s * 0.42f;
            float fy = up ? y + 0.62f : state == State.SLAM ? pos.y - 0.28f : y - 0.06f + MathUtils.sin(walkTime * 5f + s) * 0.03f;
            float sx = x + s * 0.28f;
            float sy = y + 0.1f;
            d.line(b, sx, sy, Math.max(0.05f, Vector2.dst(sx, sy, fx, fy)), 0.09f,
                    MathUtils.atan2(fy - sy, fx - sx) * MathUtils.radDeg, c(ROCK_DARK));
            d.circle(b, fx, fy, 0.13f, Draw.OUTLINE_COLOR);
            d.circle(b, fx, fy, 0.115f, c(ROCK_LIGHT));
        }
        if (state == State.LIFT) { // the boulder coming up out of the ground, over its head
            float k = 1f - Math.max(0f, timer) / LIFT_TIME;
            d.circle(b, x, y + 0.35f + 0.4f * k, 0.18f, Draw.OUTLINE_COLOR);
            d.circle(b, x, y + 0.35f + 0.4f * k, 0.16f, ROCK);
        }
    }
}
