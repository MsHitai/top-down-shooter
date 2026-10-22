package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.player.Player;
import com.trush.game.world.GameWorld;

/**
 * Town enemy: a bullet in a police cap. Keeps its distance and fires 3-round bursts that lead the
 * target - aimed where the player is running, so moving in a straight line won't save you.
 */
public class ShellCop extends Enemy {

    private static final Color UNIFORM = Color.valueOf("2f4a8a");
    private static final Color UNIFORM_DARK = Color.valueOf("22366a");
    private static final Color BRASS = Color.valueOf("d9b24c");
    private static final Color BADGE = Color.valueOf("ffd166");
    private static final Color BULLET = Color.valueOf("ffb84d");

    private static final float W = 0.36f;
    private static final float H = 0.54f;
    private static final float WINDUP = 0.45f;
    private static final float BULLET_SPEED = 3.2f;

    private float fireTimer = MathUtils.random(1.4f, 2.4f);
    private float windup;
    private int burstLeft;
    private float burstTimer;
    private final Vector2 lead = new Vector2();

    public ShellCop(float x, float y) {
        super(x, y, 22f);
        radius = 0.2f;
        speed = 1.0f;
        score = 30;
    }

    @Override
    public float feetY() {
        return pos.y - H / 2f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        if (burstLeft > 0) {
            burstTimer -= delta;
            if (burstTimer <= 0) {
                burstTimer = 0.11f;
                burstLeft--;
                shoot(world, leadAngle(world, diff), BULLET_SPEED * diff).color.set(BULLET);
                world.sfx().play("enemy_shoot", 0.5f);
                if (burstLeft == 0) {
                    fireTimer = MathUtils.random(1.6f, 2.4f) / diff;
                }
            }
            return; // stand still while firing
        }
        if (windup > 0) {
            windup -= delta;
            if (windup <= 0) {
                burstLeft = 3;
                burstTimer = 0;
            }
            return;
        }
        keepDistance(delta, world, 2.0f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && canSeePlayer) {
            windup = WINDUP;
        }
    }

    /** Aims where the player will be when the bullet gets there (if they keep running the same way). */
    private float leadAngle(GameWorld world, float diff) {
        Player p = world.getPlayer();
        float t = distanceToPlayer(world) / (BULLET_SPEED * diff);
        lead.set(p.getMoveDir()).scl(p.getSpeed() * t * 0.8f).add(p.centerX(), p.hitboxY());
        return MathUtils.atan2Deg360(lead.y - pos.y, lead.x - pos.x);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float left = pos.x - W / 2f;
        drawFeet(b, d, pos.x, bottom, 0.085f);
        d.circle(b, pos.x, bottom + H * 0.64f, W / 2f, c(BRASS));
        d.rect(b, left, bottom, W, H * 0.64f, c(UNIFORM));
        d.rect(b, left, bottom, 0.04f, H * 0.6f, c(UNIFORM_DARK));
        d.rect(b, pos.x + (facingRight() ? 0.05f : -0.1f), bottom + H * 0.3f, 0.05f, 0.06f, BADGE);
        // police cap
        d.rect(b, left - 0.02f, bottom + H * 0.78f, W + 0.04f, 0.05f, c(UNIFORM_DARK));
        d.rect(b, left + 0.03f, bottom + H * 0.82f, W - 0.06f, 0.1f, c(UNIFORM));
        d.circle(b, pos.x, bottom + H * 0.87f, 0.025f, BADGE);
        drawEyes(b, d, pos.x, bottom + H * 0.6f, 0.065f, 0.04f);

        float face = facingRight() ? 1 : -1;
        float gx = pos.x + face * 0.12f;
        float gy = bottom + H * 0.25f;
        d.line(b, gx, gy, 0.2f, 0.06f, aimAngle, GUN);
        if (windup > 0) {
            float mx = gx + MathUtils.cosDeg(aimAngle) * 0.2f;
            float my = gy + MathUtils.sinDeg(aimAngle) * 0.2f;
            d.circle(b, mx, my, 0.05f + 0.03f * (1 - windup / WINDUP), TELEGRAPH, 0.9f);
        }
    }
}
