package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;

/**
 * A single projectile, fired either by the player or by an enemy.
 * {@code accel} changes speed over time (units/s²) and {@code curve} bends the path (deg/s),
 * which is what most bullet-hell patterns are built from.
 */
public class Bullet {

    private static final Color OUTLINE = Color.valueOf("2a0a14");

    public final boolean fromPlayer;
    public final Vector2 pos = new Vector2();
    public final Vector2 vel = new Vector2();
    /** Where it was last frame: hits are checked along prev -> pos, so fast bullets can't skip enemies. */
    public final Vector2 prev = new Vector2();
    /** Keep {@link #prev} on the next update (a fresh player bullet sweeps from the gun hand). */
    private boolean keepPrev;
    public final Color color = new Color();
    public float radius;
    /** Hitbox radius = radius x this; boss bullets look bigger than they hit (fair to dodge). */
    public float hitScale = 0.8f;
    public float damage;
    public float life;
    public float accel;
    public float curve;
    public float minSpeed = 0.4f;
    /** Snakes left and right by up to this many deg/s (bubbles, bees). */
    public float wobble;
    /** Player bullets: turns toward the nearest enemy by up to this many deg/s (bees); see GameWorld. */
    public float homing;
    /** Drawn as a see-through bubble instead of a solid ball. */
    public boolean bubble;
    private float age;
    private final float phase = MathUtils.random(MathUtils.PI2);
    public float maxSpeed = 12f;
    public float knockback = 0.6f;
    public boolean piercing;
    public boolean alive = true;
    /** Enemies a piercing bullet already went through, so it only hits each once. */
    public final Array<Object> alreadyHit = new Array<>(false, 4);

    public Bullet(boolean fromPlayer, float x, float y, float angleDeg, float speed, float radius, float damage, Color color) {
        this.fromPlayer = fromPlayer;
        pos.set(x, y);
        prev.set(x, y);
        vel.set(MathUtils.cosDeg(angleDeg), MathUtils.sinDeg(angleDeg)).scl(speed);
        this.radius = radius;
        this.damage = damage;
        this.color.set(color);
        this.life = 6f;
    }

    /** The first update sweeps from (x, y) - e.g. the gun hand - instead of from where it was spawned. */
    public void sweepFrom(float x, float y) {
        prev.set(x, y);
        keepPrev = true;
    }

    public void update(float delta) {
        if (keepPrev) {
            keepPrev = false;
        } else {
            prev.set(pos);
        }
        age += delta;
        if (wobble != 0) {
            vel.rotateDeg(MathUtils.sin(age * 9f + phase) * wobble * delta);
        }
        if (curve != 0) {
            vel.rotateDeg(curve * delta);
        }
        if (accel != 0) {
            float speed = MathUtils.clamp(vel.len() + accel * delta, minSpeed, maxSpeed);
            vel.setLength(speed);
        }
        pos.mulAdd(vel, delta);
        life -= delta;
        if (life <= 0) {
            alive = false;
        }
    }

    public void draw(SpriteBatch b, Draw d) {
        if (bubble) {
            d.circle(b, pos.x, pos.y, radius, color, 0.25f);
            d.ring(b, pos.x, pos.y, radius, color, 0.9f);
            d.circle(b, pos.x - radius * 0.35f, pos.y + radius * 0.35f, radius * 0.25f, Color.WHITE, 0.9f);
        } else if (fromPlayer) {
            d.circle(b, pos.x, pos.y, radius * 1.9f, color, 0.25f);
            d.circle(b, pos.x, pos.y, radius, color);
            d.circle(b, pos.x, pos.y, radius * 0.5f, Color.WHITE);
        } else {
            d.circle(b, pos.x, pos.y, radius * 1.3f, OUTLINE);
            d.circle(b, pos.x, pos.y, radius, color);
            d.circle(b, pos.x - radius * 0.25f, pos.y + radius * 0.25f, radius * 0.4f, Color.WHITE, 0.85f);
        }
    }
}
