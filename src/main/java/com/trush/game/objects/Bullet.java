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
    /** Bounces off walls and cover this many more times instead of stopping (rubber balls). */
    public int bounces;
    /** Blows up when it stops (hits something or runs out): blast radius and damage (rockets). */
    public float blastRadius;
    public float blastDamage;
    /** Drawn as a rocket with a smoke trail. */
    public boolean rocket;
    /** Drawn as a streak this long along its path (laser bolts). */
    public float streak;
    /** How a plain bullet is drawn (the special looks above - pigeon, rocket, streak, bubble - win). */
    public enum Shape { BALL, DROP, CLOUD, SQUARE, TRIANGLE, DIAMOND, STAR }
    public Shape shape = Shape.BALL;
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
        } else if (rocket) {
            float angle = vel.angleDeg();
            float ux = MathUtils.cosDeg(angle);
            float uy = MathUtils.sinDeg(angle);
            d.rotRect(b, pos.x - ux * radius * 1.6f, pos.y - uy * radius * 1.6f, radius * 1.2f, radius * 2.4f, angle, OUTLINE, 1f);
            d.rotRect(b, pos.x, pos.y, radius * 3.2f, radius * 1.3f, angle, color, 1f);
            d.circle(b, pos.x + ux * radius * 1.6f, pos.y + uy * radius * 1.6f, radius * 0.65f, Color.WHITE);
            d.circle(b, pos.x - ux * radius * 2.2f, pos.y - uy * radius * 2.2f, radius * 0.7f, Color.ORANGE, 0.9f);
        } else if (streak > 0) {
            float angle = vel.angleDeg();
            float ux = MathUtils.cosDeg(angle) * streak / 2f;
            float uy = MathUtils.sinDeg(angle) * streak / 2f;
            d.rotRect(b, pos.x - ux, pos.y - uy, streak, radius * 3.2f, angle, color, 0.3f);
            d.rotRect(b, pos.x - ux, pos.y - uy, streak, radius * 1.6f, angle, color, 1f);
            d.rotRect(b, pos.x - ux, pos.y - uy, streak * 0.8f, radius * 0.7f, angle, Color.WHITE, 1f);
        } else if (bubble) {
            d.circle(b, pos.x, pos.y, radius, color, 0.25f);
            d.ring(b, pos.x, pos.y, radius, color, 0.9f);
            d.circle(b, pos.x - radius * 0.35f, pos.y + radius * 0.35f, radius * 0.25f, Color.WHITE, 0.9f);
        } else if (shape != Shape.BALL) {
            drawShape(b, d);
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

    /** The shaped bullets: water drops, fluffy clouds, and spinning squares, triangles, diamonds and stars. */
    private void drawShape(SpriteBatch b, Draw d) {
        float r = radius;
        float angle = vel.angleDeg();
        float spin = age * 240f + phase * MathUtils.radiansToDegrees;
        switch (shape) {
            case DROP: { // round at the front, pointed at the back, like a falling drop
                float ux = MathUtils.cosDeg(angle);
                float uy = MathUtils.sinDeg(angle);
                float tx = pos.x - ux * r * 0.6f;
                float ty = pos.y - uy * r * 0.6f;
                d.rotRect(b, tx, ty, r * 1.45f, r * 1.45f, angle + 45f, OUTLINE, 0.8f);
                d.circle(b, pos.x, pos.y, r * 1.2f, OUTLINE, 0.8f);
                d.rotRect(b, tx, ty, r * 1.15f, r * 1.15f, angle + 45f, color, 1f);
                d.circle(b, pos.x, pos.y, r, color);
                d.circle(b, pos.x + ux * r * 0.2f - uy * r * 0.35f, pos.y + uy * r * 0.2f + ux * r * 0.35f, r * 0.3f,
                        Color.WHITE, 0.9f);
                break;
            }
            case CLOUD: { // a little puff of cloud: white on top, tinted underneath, bobbing as it drifts
                float bob = MathUtils.sin(age * 8f + phase) * r * 0.08f;
                float y = pos.y + bob;
                d.circle(b, pos.x, y, r * 1.35f, color, 0.18f);
                d.circle(b, pos.x - r * 0.62f, y - r * 0.12f, r * 0.62f, color);
                d.circle(b, pos.x + r * 0.62f, y - r * 0.12f, r * 0.62f, color);
                d.circle(b, pos.x, y - r * 0.2f, r * 0.7f, color);
                d.circle(b, pos.x - r * 0.55f, y + r * 0.02f, r * 0.55f, Color.WHITE);
                d.circle(b, pos.x + r * 0.55f, y + r * 0.02f, r * 0.55f, Color.WHITE);
                d.circle(b, pos.x - r * 0.1f, y + r * 0.3f, r * 0.68f, Color.WHITE);
                break;
            }
            case SQUARE:
                d.rotRect(b, pos.x, pos.y, r * 2.1f, r * 2.1f, spin, OUTLINE, 1f);
                d.rotRect(b, pos.x, pos.y, r * 1.6f, r * 1.6f, spin, color, 1f);
                d.rotRect(b, pos.x, pos.y, r * 0.6f, r * 0.6f, spin, Color.WHITE, 0.8f);
                break;
            case DIAMOND:
                d.rotRect(b, pos.x, pos.y, r * 1.85f, r * 1.85f, angle + 45f, OUTLINE, 1f); // a corner leads
                d.rotRect(b, pos.x, pos.y, r * 1.45f, r * 1.45f, angle + 45f, color, 1f);
                d.rotRect(b, pos.x, pos.y, r * 0.55f, r * 0.55f, angle + 45f, Color.WHITE, 0.8f);
                break;
            case TRIANGLE: // points the way it flies
                d.shape(b, d.triangle, pos.x, pos.y, r * 1.55f, angle, OUTLINE, 1f);
                d.shape(b, d.triangle, pos.x, pos.y, r * 1.2f, angle, color, 1f);
                d.shape(b, d.triangle, pos.x, pos.y, r * 0.5f, angle, Color.WHITE, 0.8f);
                break;
            case STAR:
                d.shape(b, d.star, pos.x, pos.y, r * 1.6f, spin, OUTLINE, 1f);
                d.shape(b, d.star, pos.x, pos.y, r * 1.3f, spin, color, 1f);
                d.circle(b, pos.x, pos.y, r * 0.3f, Color.WHITE, 0.8f);
                break;
            default:
                break;
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
