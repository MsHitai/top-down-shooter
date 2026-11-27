package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.CastArt;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * A marked critter of the Undercity (see Marked): a scruffy alley mutt with a floppy ear and a glowing
 * green eye. It runs rings round the player, crouches, then lunges and
 * snaps at the end (a fan of teeth). Now and then it stops, throws its head back and howls.
 * The howl rolls out as a ring of sound and calls a pack of marked rats out of
 * the dark (twice per dog at most, and not into a crowd). Big - half as big again as it was (the owner's call:
 * it never landed a bite, so nobody knew what it did).
 */
public class AlleyDog extends Enemy {

    private static final Color HOWL = Color.valueOf("c8e85a");

    private static final float CROUCH = 0.45f;
    private static final float LUNGE = 0.35f;
    private static final float LUNGE_SPEED = 6.5f;
    /** How much bigger it's drawn than the first, small mutt (its hitbox grew with it). */
    private static final float SIZE = 1.5f;
    private static final float ART_SCALE = 0.78f * SIZE;
    private static final int CROUCH_POSE = 5;
    private static final int LUNGE_POSE = 6;
    private static final int HOWL_POSE = 7;
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
        if (lunge > 0) { // lunging teeth first
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

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        boolean right = lunge > 0f ? dir.x >= 0f : facingRight();
        float face = right ? 1f : -1f;
        int pose = howl > 0f ? HOWL_POSE : lunge > 0f ? LUNGE_POSE : crouch > 0f ? CROUCH_POSE
                : CastArt.walkPose(rest > 0f ? 0f : walkTime);
        float pixel = ART_SCALE / 64f;
        float x = pos.x;
        float y = feetY() + 16f * pixel; // standing paws sit on row 40
        d.cast.centeredPose(b, "AlleyDog", x, y, pose, right, flash > 0f, c(Color.WHITE), ART_SCALE);
        if (flash <= 0f) {
            // Eye socket pixels in the authored poses, measured from their common frame center.
            int eyeX = switch (pose) {
                case CROUCH_POSE -> 34;
                case LUNGE_POSE -> 35;
                case HOWL_POSE -> 31;
                default -> 32;
            };
            int eyeY = switch (pose) {
                case CROUCH_POSE -> 25;
                case LUNGE_POSE -> 20;
                case HOWL_POSE -> 15;
                default -> 19;
            };
            Marked.eye(b, d, x + face * (eyeX - 23.5f) * pixel, y + (23.5f - eyeY) * pixel, pixel, clock);
        }
        if (howl > 0f) {
            float t = 1f - howl / HOWL_TIME;
            for (int i = 0; i < 3; i++) {
                float r = (((t * 3f + i) % 3f) * 0.25f + 0.1f) * SIZE;
                d.ring(b, x + face * 16f * pixel, y + 12f * pixel, r, HOWL, 0.6f * (1f - r / (0.85f * SIZE)));
            }
        }
    }
}
