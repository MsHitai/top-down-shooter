package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * A marked critter of the Undercity (see Marked): a scruffy alley mutt, one floppy brown ear and one
 * green-yellow one sticking straight up. It runs rings round the player and lunges - it crouches, a red
 * line shows where, and it bolts down it and snaps at the end (a fan of teeth) - and now and then it stops,
 * throws its head back and howls: the howl rolls out as a ring of sound and calls a pack of marked rats out of
 * the dark (twice per dog at most, and not into a crowd). Big - half as big again as it was (the owner's call:
 * it never landed a bite, so nobody knew what it did).
 */
public class AlleyDog extends Enemy {

    private static final Color FUR = Color.valueOf("8a6a4a");
    private static final Color FUR_DARK = Color.valueOf("5a442e");
    private static final Color FUR_LIGHT = Color.valueOf("b8946a");
    private static final Color NOSE = Color.valueOf("1a1418");
    private static final Color HOWL = Color.valueOf("c8e85a");

    private static final float CROUCH = 0.45f;
    private static final float LUNGE = 0.35f;
    private static final float LUNGE_SPEED = 6.5f;
    /** How much bigger it's drawn than the first, small mutt (its hitbox grew with it). */
    private static final float SIZE = 1.5f;
    /** The snap at the end of a lunge: this many teeth in a fan. */
    static final int TEETH = 5;
    /** The howl's ring of sound. */
    static final int HOWL_RING = 12;
    private static final Color TOOTH = Color.valueOf("f4f0e0");
    private static final float REST = 0.6f;
    private static final float HOWL_TIME = 1.1f;
    /** It howls the pack in at most this many times. */
    public static final int HOWLS = 2;

    private float clock = MathUtils.random(10f);
    private float lungeTimer = MathUtils.random(1.5f, 2.5f);
    private float howlTimer = MathUtils.random(2f, 3.5f);
    private float crouch;
    private float lunge;
    private float rest;
    private float howl;
    private int howls;
    private final Vector2 dir = new Vector2();

    public AlleyDog(float x, float y) {
        super(x, y, 30f);
        radius = 0.16f * SIZE;
        speed = 2.2f;
        score = 40;
    }

    @Override
    public float feetY() {
        return pos.y - 0.14f * SIZE;
    }

    public int howls() {
        return howls;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        clock += delta;
        if (howl > 0) { // head back, howling: the pack comes when it's done
            howl -= delta;
            if (howl <= 0) {
                howlRing(world);
                callThePack(world);
            }
            return;
        }
        if (crouch > 0) {
            crouch -= delta;
            if (crouch <= 0) {
                lunge = LUNGE;
                world.sfx().play("swish", 0.5f);
            }
            return;
        }
        if (lunge > 0) { // bolting down the line, teeth first
            lunge -= delta;
            pos.mulAdd(dir, LUNGE_SPEED * delta);
            walkTime += delta * 2f;
            if (lunge <= 0) {
                rest = REST;
                snap(world);
            }
            return;
        }
        if (rest > 0) {
            rest -= delta;
            return;
        }
        keepDistance(delta, world, 1.5f, speed);
        lungeTimer -= delta;
        howlTimer -= delta;
        if (howlTimer <= 0 && howls < HOWLS && canShoot(world)) {
            howlTimer = MathUtils.random(7f, 9f);
            howl = HOWL_TIME;
            world.sfx().play("howl", 0.8f);
        } else if (lungeTimer <= 0 && canShoot(world)) {
            lungeTimer = MathUtils.random(1.8f, 2.6f) / world.getDifficulty();
            crouch = CROUCH;
            dir.set(MathUtils.cosDeg(aimAngle), MathUtils.sinDeg(aimAngle));
        }
    }

    /** The end of a lunge: the jaws snap shut on a fan of teeth flying on where it was going. */
    private void snap(GameWorld world) {
        float diff = world.getDifficulty();
        float a = dir.angleDeg();
        float mx = pos.x + dir.x * radius;
        float my = pos.y + dir.y * radius;
        for (int i = 0; i < TEETH; i++) {
            Bullet t = world.spawnEnemyBullet(mx, my, a + (i - (TEETH - 1) / 2f) * 14f, 3.2f * diff);
            t.shape = Bullet.Shape.DIAMOND;
            t.color.set(TOOTH);
            t.life = 0.9f;
        }
        world.sfx().play("chomp", 0.5f);
    }

    /** The howl's sound rolls out as a slow ring - green like the mark. */
    private void howlRing(GameWorld world) {
        float diff = world.getDifficulty();
        float off = MathUtils.random(360f);
        float hx = pos.x + (facingRight() ? 1 : -1) * 0.14f * SIZE;
        for (int i = 0; i < HOWL_RING; i++) {
            Bullet r = world.spawnEnemyBullet(hx, pos.y + 0.15f * SIZE, off + 360f * i / HOWL_RING, 1.6f * diff);
            r.color.set(HOWL);
        }
    }

    /** Out of the dark, a marked rat pack answers (not into a crowd). */
    private void callThePack(GameWorld world) {
        howls++;
        if (world.getEnemies().size + world.getPendingEnemies().size >= 8) {
            return;
        }
        Vector2 p = world.findSpawnPoint(2.2f);
        for (int i = 0; i < 3; i++) {
            world.spawnEnemy(Rat.marked(p.x + MathUtils.random(-0.3f, 0.3f), p.y + MathUtils.random(-0.3f, 0.3f)));
        }
    }

    /** The line of the lunge it's about to make. */
    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (crouch > 0 && alive) {
            float k = 1f - crouch / CROUCH;
            float len = LUNGE_SPEED * LUNGE;
            d.line(b, pos.x, pos.y - 0.1f * SIZE, len, 0.06f, dir.angleDeg(), TELEGRAPH, 0.25f + 0.5f * k);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float face = facingRight() ? 1 : -1;
        float k = SIZE;
        float run = MathUtils.sin(walkTime * 18f) * 0.03f * k;
        float low = crouch > 0 ? 0.04f * k : 0f;
        float y = pos.y - low;
        // the tail, wagging (or straight back while it lunges)
        float wag = lunge > 0 ? 0f : MathUtils.sin(clock * 12f) * 25f;
        d.rotRect(b, pos.x - face * 0.17f * k, y + 0.06f * k, 0.14f * k, 0.04f * k, (face > 0 ? 150f : 30f) + wag, c(FUR_DARK), 1f);
        // legs, body
        for (int s = -1; s <= 1; s += 2) {
            d.rect(b, pos.x + s * 0.08f * k - 0.02f * k, y - 0.14f * k + (s > 0 ? run : -run), 0.04f * k, 0.1f * k, c(FUR_DARK));
        }
        d.ellipse(b, pos.x - face * 0.02f * k, y, 0.15f * k, 0.08f * k, c(FUR), 1f);
        d.ellipse(b, pos.x - face * 0.02f * k, y - 0.03f * k, 0.1f * k, 0.04f * k, c(FUR_LIGHT), 1f);
        // the head, thrown back while it howls
        float tilt = howl > 0 ? 35f : 0f;
        float hx = pos.x + face * 0.14f * k;
        float hy = y + 0.08f * k + (howl > 0 ? 0.05f * k : 0f);
        d.circle(b, hx, hy, 0.07f * k, c(FUR));
        d.rotRect(b, hx + face * 0.07f * k, hy - 0.02f * k + (howl > 0 ? 0.04f * k : 0f), 0.08f * k, 0.05f * k, face * tilt, c(FUR_LIGHT), 1f);
        d.circle(b, hx + face * 0.11f * k, hy + (howl > 0 ? 0.04f * k : -0.01f * k), 0.018f * k, NOSE);
        d.rotRect(b, hx - face * 0.04f * k, hy - 0.02f * k, 0.04f * k, 0.08f * k, face * 15f, c(FUR_DARK), 1f); // the floppy ear
        Marked.ear(b, d, hx + face * 0.01f * k, hy + 0.04f * k, 0.09f * k, -face * 10f);                 // the odd one
        Marked.eye(b, d, hx + face * 0.04f * k, hy + 0.015f * k, 0.016f * k, clock);
        if (howl > 0) { // the howl rolling out in rings
            float t = 1f - howl / HOWL_TIME;
            for (int i = 0; i < 3; i++) {
                float r = (((t * 3f + i) % 3f) * 0.25f + 0.1f) * k;
                d.ring(b, hx + face * 0.12f * k, hy + 0.1f * k, r, HOWL, 0.6f * (1f - r / (0.85f * k)));
            }
        }
    }
}
