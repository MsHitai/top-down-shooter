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
 * Base class for everything that walks around and shoots at the player.
 * Subclasses implement {@link #behave(float, GameWorld)} and {@link #drawBody(SpriteBatch, Draw)}.
 * {@code pos} is the centre of the body; {@code radius} is the hitbox.
 */
public abstract class Enemy {

    public static final float SPAWN_TIME = 0.9f;

    protected static final Color EYE_WHITE = Color.WHITE;
    protected static final Color PUPIL = Color.valueOf("111111");
    protected static final Color GUN = Color.valueOf("4a4a55");
    protected static final Color FEET = Color.valueOf("2b2024");
    protected static final Color TELEGRAPH = Color.valueOf("ff4d4d");
    private static final Color SUMMON = Color.valueOf("ff6b81");

    public final Vector2 pos = new Vector2();
    protected final Vector2 knock = new Vector2();
    private final Vector2 tmp = new Vector2();

    public float radius = 0.17f;
    public float hp;
    public float maxHp;
    public float speed = 0.9f;
    public int score = 10;
    public boolean alive = true;
    public boolean boss;

    protected float flash;
    protected float spawnTimer = SPAWN_TIME;
    protected float aimAngle;
    protected float walkTime;
    protected float strafeDir = MathUtils.randomSign();
    protected float strafeTimer = MathUtils.random(1f, 2.5f);

    protected Enemy(float x, float y, float hp) {
        pos.set(x, y);
        this.hp = hp;
        this.maxHp = hp;
    }

    public boolean isSpawning() {
        return spawnTimer > 0;
    }

    /** Bottom of the body, used for draw ordering against the player. */
    public abstract float feetY();

    public final void update(float delta, GameWorld world) {
        flash -= delta;
        if (spawnTimer > 0) {
            spawnTimer -= delta;
            return;
        }
        Player p = world.getPlayer();
        aimAngle = MathUtils.atan2Deg360(p.hitboxY() - pos.y, p.centerX() - pos.x);

        pos.mulAdd(knock, delta);
        knock.scl(Math.max(0f, 1f - 10f * delta));

        behave(delta, world);
        Arena.clamp(pos, radius);
    }

    protected abstract void behave(float delta, GameWorld world);

    public void hit(float damage, Vector2 bulletVel, float knockback, GameWorld world) {
        if (!alive || isSpawning()) {
            return;
        }
        hp -= damage;
        flash = 0.08f;
        if (!boss) {
            tmp.set(bulletVel).nor().scl(knockback * 2.5f);
            knock.add(tmp);
        }
        if (hp <= 0) {
            alive = false;
            world.onEnemyKilled(this);
        }
    }

    // ---------------------------------------------------------------- helpers

    protected float distanceToPlayer(GameWorld world) {
        Player p = world.getPlayer();
        return Vector2.dst(pos.x, pos.y, p.centerX(), p.hitboxY());
    }

    /** Approach until roughly {@code preferred} units away, then circle-strafe. */
    protected void keepDistance(float delta, GameWorld world, float preferred, float moveSpeed) {
        strafeTimer -= delta;
        if (strafeTimer <= 0) {
            strafeTimer = MathUtils.random(1f, 2.5f);
            strafeDir = MathUtils.randomSign();
        }
        float dist = distanceToPlayer(world);
        float cos = MathUtils.cosDeg(aimAngle);
        float sin = MathUtils.sinDeg(aimAngle);
        float forward = 0;
        if (dist > preferred + 0.3f) forward = 1;
        else if (dist < preferred - 0.3f) forward = -0.8f;
        float strafe = 0.6f * strafeDir;
        tmp.set(cos * forward - sin * strafe, sin * forward + cos * strafe);
        if (!tmp.isZero()) {
            tmp.nor().scl(moveSpeed * delta);
            pos.add(tmp);
            walkTime += delta;
        }
    }

    protected Bullet shoot(GameWorld world, float angle, float bulletSpeed) {
        return world.spawnEnemyBullet(pos.x + MathUtils.cosDeg(angle) * radius,
                pos.y + MathUtils.sinDeg(angle) * radius, angle, bulletSpeed);
    }

    /** Colour to draw a body part with - white while flashing from a hit. */
    protected Color c(Color base) {
        return flash > 0 ? Color.WHITE : base;
    }

    protected boolean facingRight() {
        return MathUtils.cosDeg(aimAngle) >= 0;
    }

    protected void drawEyes(SpriteBatch b, Draw d, float cx, float cy, float spacing, float size) {
        float face = facingRight() ? 1 : -1;
        float lookX = MathUtils.cosDeg(aimAngle) * size * 0.35f;
        float lookY = MathUtils.sinDeg(aimAngle) * size * 0.35f;
        for (int i = -1; i <= 1; i += 2) {
            float ex = cx + i * spacing + face * spacing * 0.5f;
            d.circle(b, ex, cy, size, c(EYE_WHITE));
            d.circle(b, ex + lookX, cy + lookY, size * 0.55f, PUPIL);
        }
    }

    protected void drawFeet(SpriteBatch b, Draw d, float cx, float bottom, float spread) {
        float step = MathUtils.sin(walkTime * 14f) * 0.02f;
        d.rect(b, cx - spread - 0.04f, bottom - 0.03f + step, 0.08f, 0.05f, FEET);
        d.rect(b, cx + spread - 0.04f, bottom - 0.03f - step, 0.08f, 0.05f, FEET);
    }

    public final void draw(SpriteBatch b, Draw d) {
        if (isSpawning()) {
            float k = spawnTimer / SPAWN_TIME;
            d.ellipse(b, pos.x, feetY(), radius * 1.6f, radius * 0.6f, SUMMON, 0.25f + 0.25f * (1 - k));
            d.ring(b, pos.x, pos.y, radius * (0.5f + 2.5f * k), SUMMON, 1f - k);
            return;
        }
        d.ellipse(b, pos.x, feetY(), radius * 1.1f, radius * 0.35f, Color.BLACK, 0.35f);
        drawBody(b, d);
    }

    protected abstract void drawBody(SpriteBatch b, Draw d);
}
