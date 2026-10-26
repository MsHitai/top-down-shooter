package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * A marked critter of the Undercity (see Marked): a sewer alligator with one odd green ear. It glides along
 * under the sewer water - just its glowing eyes and a ripple showing, nothing can touch it - up to the
 * player; the moment it's close, bubbles boil up where it's about to burst out, then it surfaces with a snap
 * of its jaws and a splash of water drops all round. It stays up for a couple of seconds, coming straight at
 * the player (biting on touch) and spitting water at them - the time to shoot it - then sinks again with a
 * smaller splash, and is soon on its way back.
 */
public class SewerGator extends Enemy {

    private static final Color SCALES = Color.valueOf("4a7a4a");
    private static final Color SCALES_DARK = Color.valueOf("2e5230");
    private static final Color BELLY = Color.valueOf("b8c88a");
    private static final Color TEETH = Color.valueOf("f5f0e6");
    private static final Color WATER = Color.valueOf("3a6a5a");
    private static final Color DROP = Color.valueOf("6ad8a0");

    private enum State { UNDER, BUBBLING, UP, SINKING }

    private static final float BUBBLE_TIME = 0.55f;
    private static final float UP_TIME = 2.4f;
    private static final float SINK_TIME = 0.35f;
    private static final float GLIDE_SPEED = 2.1f;
    /** It surfaces as soon as it's glided this close. */
    private static final float STRIKE_RANGE = 1.2f;
    /** While up, it spits a little fan of water at the player this often. */
    private static final float SPIT_EVERY = 0.65f;

    private State state = State.UNDER;
    private float timer = MathUtils.random(0.3f, 0.8f);
    private float spit;
    private float clock = MathUtils.random(10f);
    /** Jaws snapping open and shut as it bursts out. */
    private float snap;

    public SewerGator(float x, float y) {
        super(x, y, 40f);
        radius = 0.24f;
        speed = 1.1f;
        score = 45;
        acidProof = true; // (it lives in the stuff)
        hurtsToTouch = false;
    }

    /** Under the water nothing touches it. */
    @Override
    protected boolean isDodging() {
        return state == State.UNDER || state == State.BUBBLING;
    }

    @Override
    public float feetY() {
        return pos.y - 0.12f;
    }

    @Override
    protected boolean castsShadow() {
        return state == State.UP;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        clock += delta;
        snap = Math.max(0f, snap - delta);
        timer -= delta;
        switch (state) {
            case UNDER: // gliding along under the water after the player
                hurtsToTouch = false;
                if (distanceToPlayer(world) > STRIKE_RANGE) {
                    if (!flank(delta, world, GLIDE_SPEED)) {
                        pos.add(MathUtils.cosDeg(aimAngle) * GLIDE_SPEED * delta, MathUtils.sinDeg(aimAngle) * GLIDE_SPEED * delta);
                    }
                    walkTime += delta;
                }
                if (distanceToPlayer(world) <= STRIKE_RANGE && timer <= 0) { // close: up it comes, right away
                    state = State.BUBBLING;
                    timer = BUBBLE_TIME;
                    world.sfx().play("pop", 0.5f);
                }
                break;
            case BUBBLING: // the water boils up where it'll burst out
                if (MathUtils.random() < 20f * delta) {
                    world.particles().spawn(pos.x + MathUtils.random(-0.3f, 0.3f), pos.y + MathUtils.random(-0.15f, 0.15f),
                            0f, 0.5f, 0.4f, 0.03f, DROP, 1f);
                }
                if (timer <= 0) surface(world);
                break;
            case UP: // straight at the player, jaws ready, spitting water
                hurtsToTouch = true;
                if (distanceToPlayer(world) > 0.25f) {
                    pos.add(MathUtils.cosDeg(aimAngle) * speed * delta, MathUtils.sinDeg(aimAngle) * speed * delta);
                    walkTime += delta;
                }
                spit -= delta;
                if (spit <= 0 && canShoot(world)) {
                    spit = SPIT_EVERY;
                    for (int i = -1; i <= 1; i++) {
                        Bullet b = shoot(world, aimAngle + i * 14f, 2.5f * world.getDifficulty());
                        b.shape = Bullet.Shape.DROP;
                        b.color.set(DROP);
                        b.radius = 0.06f;
                    }
                    snap = 0.4f;
                    world.sfx().play("chomp", 0.4f);
                }
                if (timer <= 0) {
                    state = State.SINKING;
                    timer = SINK_TIME;
                    splash(world, 7, 1.8f); // a smaller splash as it dives
                }
                break;
            default: // sinking back down
                if (timer <= 0) {
                    state = State.UNDER;
                    hurtsToTouch = false;
                    timer = MathUtils.random(0.5f, 0.9f);
                }
                break;
        }
    }

    /** Splash! Up it comes, jaws snapping, water drops flying out all round. */
    private void surface(GameWorld world) {
        state = State.UP;
        timer = UP_TIME;
        snap = 0.4f;
        spit = SPIT_EVERY * 0.6f;
        hurtsToTouch = true;
        splash(world, 12, 2.1f);
        world.sfx().play("chomp", 0.8f);
        world.shake(0.15f);
    }

    /** A ring of {@code n} water drops all round. */
    private void splash(GameWorld world, int n, float speed) {
        float off = MathUtils.random(360f);
        for (int i = 0; i < n; i++) {
            Bullet b = world.spawnEnemyBullet(pos.x, pos.y, off + 360f * i / n, speed * world.getDifficulty());
            b.shape = Bullet.Shape.DROP;
            b.color.set(DROP);
            b.radius = 0.06f;
        }
        world.particles().burst(pos.x, pos.y, 12 + n, 0.4f, 1.8f, 0.5f, 0.05f, DROP);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float face = facingRight() ? 1 : -1;
        if (state == State.UNDER || state == State.BUBBLING) { // a dark ripple, two glowing eyes, the odd ear
            float ripple = 0.28f + 0.03f * MathUtils.sin(clock * 6f);
            d.ellipse(b, pos.x, pos.y - 0.02f, ripple * 1.6f, ripple * 0.55f, WATER, 0.7f);
            d.ellipse(b, pos.x - face * 0.25f, pos.y - 0.02f, 0.18f, 0.05f, SCALES_DARK, 0.6f); // its back under the water
            Marked.ear(b, d, pos.x - face * 0.02f, pos.y + 0.02f, 0.08f, -face * 15f);
            Marked.eye(b, d, pos.x + face * 0.05f, pos.y + 0.03f, 0.018f, clock);
            Marked.eye(b, d, pos.x + face * 0.12f, pos.y + 0.03f, 0.018f, clock);
            if (state == State.BUBBLING) {
                float k = 1f - timer / BUBBLE_TIME;
                d.ring(b, pos.x, pos.y, 0.25f + 0.25f * k, DROP, 0.4f + 0.5f * k);
            }
            return;
        }
        float sink = state == State.SINKING ? 1f - timer / SINK_TIME : 0f;
        float y = pos.y - 0.12f * sink;
        float wag = MathUtils.sin(walkTime * 10f) * 6f;
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
        // the head: a long snout, jaws snapping open as it bursts out, teeth
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
