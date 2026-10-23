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
    private static final Color WING = Color.valueOf("5e667a");
    private static final Color NECK = Color.valueOf("4fa07a");
    private static final Color BEAK = Color.valueOf("e0a040");

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
    /** Player boomerang: flies out and slows down, then comes back to the player (see GameWorld). */
    public boolean boomerang;
    /** A boomerang on its way back. */
    public boolean returning;
    /** Drawn as a see-through bubble instead of a solid ball. */
    public boolean bubble;
    /** Drawn as a flapping pigeon flying the way it moves (the Pigeon King's flocks); body in {@link #color}. */
    public boolean pigeon;
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
        if (pigeon) {
            drawPigeon(b, d);
        } else if (bubble) {
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

    /** A little pigeon seen from above: flapping wings, tail, a shiny green neck and an orange beak. */
    private void drawPigeon(SpriteBatch b, Draw d) {
        float r = radius;
        float angle = vel.angleDeg();
        float ux = MathUtils.cosDeg(angle);
        float uy = MathUtils.sinDeg(angle);
        float flap = 0.35f + 0.65f * Math.abs(MathUtils.sin(age * 14f + phase));
        d.circle(b, pos.x, pos.y, r * 1.2f, OUTLINE, 0.6f);
        for (int side = -1; side <= 1; side += 2) { // wings stick out sideways
            float off = side * r * (0.55f + 0.7f * flap);
            d.rotRect(b, pos.x - uy * off, pos.y + ux * off, r * 0.95f, r * 1.6f * flap, angle, WING, 1f);
        }
        d.rotRect(b, pos.x - ux * r * 0.95f, pos.y - uy * r * 0.95f, r * 0.6f, r * 0.55f, angle, WING, 1f);
        d.circle(b, pos.x, pos.y, r * 0.85f, color);
        d.circle(b, pos.x + ux * r * 0.6f, pos.y + uy * r * 0.6f, r * 0.55f, NECK);
        d.circle(b, pos.x + ux * r * 0.8f, pos.y + uy * r * 0.8f, r * 0.42f, color);
        d.rotRect(b, pos.x + ux * r * 1.25f, pos.y + uy * r * 1.25f, r * 0.42f, r * 0.2f, angle, BEAK, 1f);
        d.circle(b, pos.x + ux * r * 0.9f - uy * r * 0.18f, pos.y + uy * r * 0.9f + ux * r * 0.18f, r * 0.1f, OUTLINE);
    }
}
