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
    public final Color color = new Color();
    public float radius;
    public float damage;
    public float life;
    public float accel;
    public float curve;
    public float minSpeed = 0.4f;
    public float maxSpeed = 12f;
    public float knockback = 0.6f;
    public boolean piercing;
    public boolean alive = true;
    /** Enemies a piercing bullet already went through, so it only hits each once. */
    public final Array<Object> alreadyHit = new Array<>(false, 4);

    public Bullet(boolean fromPlayer, float x, float y, float angleDeg, float speed, float radius, float damage, Color color) {
        this.fromPlayer = fromPlayer;
        pos.set(x, y);
        vel.set(MathUtils.cosDeg(angleDeg), MathUtils.sinDeg(angleDeg)).scl(speed);
        this.radius = radius;
        this.damage = damage;
        this.color.set(color);
        this.life = 6f;
    }

    public void update(float delta) {
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
        if (fromPlayer) {
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
