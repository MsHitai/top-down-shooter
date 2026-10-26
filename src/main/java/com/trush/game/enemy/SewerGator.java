package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * A marked critter of the Undercity (see Marked): a sewer alligator with one odd green ear, dripping with
 * sewer water. It never hides (the owner's call: waiting for a hidden one to strike was dull) - it crawls
 * straight at the player, biting on touch, spits little fans of water, and every so often stops, bubbles boil
 * up round it and it thrashes - a ring of water drops all round.
 */
public class SewerGator extends Enemy {

    private static final Color SCALES = Color.valueOf("4a7a4a");
    private static final Color SCALES_DARK = Color.valueOf("2e5230");
    private static final Color BELLY = Color.valueOf("b8c88a");
    private static final Color TEETH = Color.valueOf("f5f0e6");
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
        if (!canSeePlayer && flank(delta, world, speed)) {
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
        float wag = MathUtils.sin(walkTime * 10f) * 6f;
        // a puddle of sewer water it carries along, and the bubbles' ring before a thrash
        d.ellipse(b, pos.x, y - 0.12f, 0.36f, 0.1f, WATER, 0.5f);
        if (bubbling > 0) {
            float k = 1f - bubbling / BUBBLE_TIME;
            d.ring(b, pos.x, y, 0.25f + 0.3f * k, DROP, 0.4f + 0.5f * k);
        }
        // the tail, the long body, stubby legs
        d.rotRect(b, pos.x - face * 0.36f, y - 0.02f, 0.3f, 0.08f, face * (10f + wag), c(SCALES_DARK), 1f);
        d.ellipse(b, pos.x - face * 0.05f, y, 0.3f, 0.13f, c(SCALES), 1f);
        d.ellipse(b, pos.x - face * 0.05f, y - 0.05f, 0.24f, 0.06f, c(BELLY), 1f);
        for (int i = 0; i < 4; i++) { // the ridges along its back
            d.rect(b, pos.x - face * (0.25f - i * 0.12f) - 0.02f, y + 0.1f, 0.04f, 0.04f, c(SCALES_DARK));
        }
        for (int s = -1; s <= 1; s += 2) {
            d.rect(b, pos.x - 0.16f + s * 0.12f - 0.03f, y - 0.15f, 0.06f, 0.06f, c(SCALES_DARK));
        }
        // the head: a long snout, jaws snapping, teeth
        float open = snap > 0 ? MathUtils.sin(snap / 0.4f * MathUtils.PI * 3f) * 18f : 0f;
        float hx = pos.x + face * 0.22f;
        d.rotRect(b, hx + face * 0.12f, y + 0.03f, 0.26f, 0.07f, face * open, c(SCALES), 1f);  // upper jaw
        d.rotRect(b, hx + face * 0.12f, y - 0.03f, 0.26f, 0.06f, -face * open, c(BELLY), 1f); // lower jaw
        for (int i = 0; i < 3; i++) {
            d.rect(b, hx + face * (0.04f + i * 0.07f) - 0.01f, y - 0.005f, 0.02f, 0.025f, TEETH);
        }
        d.circle(b, hx, y + 0.04f, 0.09f, c(SCALES));
        Marked.ear(b, d, hx - face * 0.04f, y + 0.09f, 0.1f, -face * 20f);
        Marked.eye(b, d, hx + face * 0.03f, y + 0.08f, 0.02f, clock);
    }
}
