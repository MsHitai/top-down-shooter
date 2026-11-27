package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * A marked critter of the Undercity (see Marked): a sewer alligator with a glowing green eye, dripping with
 * sewer water. It never hides (the owner's call: waiting for a hidden one to strike was dull) - it crawls
 * straight at the player, biting on touch, spits little fans of water, and every so often stops, bubbles boil
 * up round it and it thrashes - a ring of water drops all round.
 */
public class SewerGator extends Enemy {

    private static final float ART_SCALE = 1.35f;
    private static final Color WATER = Color.valueOf("3a6a5a");
    private static final Color DROP = Color.valueOf("6ad8a0");

    /** Bubbles boil up for this long before it thrashes. */
    private static final float BUBBLE_TIME = 0.55f;
    /** It spits a little fan of water at the player this often. */
    private static final float SPIT_EVERY = 0.9f;

    private float clock = MathUtils.random(10f);
    private float spit = MathUtils.random(0.6f, 1.2f);
    private float thrashTimer = MathUtils.random(2f, 3f);
    private float bubbling;
    /** Jaws snapping open and shut. */
    private float snap;

    public SewerGator(float x, float y) {
        super(x, y, 40f);
        radius = 0.24f;
        speed = 1.2f;
        score = 45;
        acidProof = true; // (it lives in the stuff)
    }

    @Override
    public float feetY() {
        return pos.y - 0.12f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        clock += delta;
        snap = Math.max(0f, snap - delta);
        float diff = world.getDifficulty();
        if (bubbling > 0) { // the water boils up round it: a thrash is coming
            bubbling -= delta;
            if (MathUtils.random() < 20f * delta) {
                world.particles().spawn(pos.x + MathUtils.random(-0.3f, 0.3f), pos.y + MathUtils.random(-0.15f, 0.15f),
                        0f, 0.5f, 0.4f, 0.03f, DROP, 1f);
            }
            if (bubbling <= 0) thrash(world, diff);
            return;
        }
        // straight at the player
        if (flank(delta, world, speed)) {
            return;
        }
        if (distanceToPlayer(world) > 0.25f) {
            pos.add(MathUtils.cosDeg(aimAngle) * speed * delta, MathUtils.sinDeg(aimAngle) * speed * delta);
            walkTime += delta;
        }
        spit -= delta;
        if (spit <= 0 && canShoot(world)) {
            spit = SPIT_EVERY / Math.min(diff, 1.3f);
            for (int i = -1; i <= 1; i++) {
                Bullet b = shoot(world, aimAngle + i * 14f, 2.5f * diff);
                b.shape = Bullet.Shape.DROP;
                b.color.set(DROP);
                b.radius = 0.06f;
            }
            snap = 0.4f;
            world.sfx().play("chomp", 0.4f);
        }
        thrashTimer -= delta;
        if (thrashTimer <= 0) {
            thrashTimer = MathUtils.random(2.6f, 3.6f) / diff;
            bubbling = BUBBLE_TIME;
            world.sfx().play("pop", 0.5f);
        }
    }

    /** It thrashes: water drops fly out all round. */
    private void thrash(GameWorld world, float diff) {
        int n = 12;
        float off = MathUtils.random(360f);
        for (int i = 0; i < n; i++) {
            Bullet b = world.spawnEnemyBullet(pos.x, pos.y, off + 360f * i / n, 2.1f * diff);
            b.shape = Bullet.Shape.DROP;
            b.color.set(DROP);
            b.radius = 0.06f;
        }
        snap = 0.4f;
        world.particles().burst(pos.x, pos.y, 20, 0.4f, 1.8f, 0.5f, 0.05f, DROP);
        world.sfx().play("chomp", 0.8f);
        world.shake(0.15f);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float face = facingRight() ? 1 : -1;
        float y = pos.y;
        // a puddle of sewer water it carries along, and the bubbles' ring before a thrash
        d.ellipse(b, pos.x, y - 0.12f, 0.36f, 0.1f, WATER, 0.5f);
        if (bubbling > 0) {
            float k = 1f - bubbling / BUBBLE_TIME;
            d.ring(b, pos.x, y, 0.25f + 0.3f * k, DROP, 0.4f + 0.5f * k);
        }
        boolean jawsOpen = snap > 0f && MathUtils.sin(snap / 0.4f * MathUtils.PI * 3f) > 0f;
        d.cast.centered(b, "SewerGator", pos.x, y, walkTime, facingRight(), jawsOpen, flash > 0,
                c(Color.WHITE), ART_SCALE, ART_SCALE, 1f);
        // Keep the fading mark inside the sheet's eye socket (pixel 33, 21).
        if (flash <= 0) {
            float pixel = ART_SCALE / 64f;
            Marked.eye(b, d, pos.x + face * 9.5f * pixel, y + 2.5f * pixel, 0.9f * pixel, clock);
        }
    }
}
