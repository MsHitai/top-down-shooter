package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Pickup;
import com.trush.game.world.GameWorld;

/**
 * A street lamp that was never a street lamp (town and city, very rare - see StoryDirector): it stands
 * among the real ones until the player walks up to it or shoots it, then rips itself out of the pavement
 * and hops after them. Every landing throws a ring of bullets with a gap; every third hop it spits a fan
 * of lantern sparks instead. Like ETG's mimics it pays out when it dies: a key or a blank.
 */
public class LampMimic extends Enemy {

    private static final Color POST = Color.valueOf("2a2a30");
    private static final Color POST_HI = Color.valueOf("4a4a54");
    private static final Color GLASS = Color.valueOf("ffd166");
    private static final Color GLASS_HOT = Color.valueOf("ff8a3a");
    private static final Color TEETH = Color.valueOf("fff8e0");
    private static final Color MOUTH = Color.valueOf("3a1010");

    private static final float WAKE = 0.7f;
    private static final float HOP = 0.5f;
    private static final float POST_H = 0.85f;

    private float wake = WAKE;
    private float rest = 0.5f;
    private float hop;
    private final Vector2 hopVel = new Vector2();
    private int hops;
    private float mouth;

    /** (x, y) is the foot of the lamp it pretended to be. */
    public LampMimic(float x, float y) {
        super(x, y + 0.12f, 80f);
        radius = 0.22f;
        speed = 2.6f;
        score = 60;
        spawnTimer = 0; // it was already standing here
    }

    @Override
    public float feetY() {
        return pos.y - 0.12f;
    }

    private boolean angry() {
        return hp < maxHp * 0.5f;
    }

    /** How high it is in its hop right now (drawing only - the hitbox stays on the floor). */
    private float lift() {
        if (wake > 0) {
            return 0.12f * MathUtils.sin(MathUtils.PI * (1f - wake / WAKE));
        }
        if (hop <= 0) {
            return 0f;
        }
        float t = 1f - hop / HOP;
        return 0.35f * 4f * t * (1f - t);
    }

    /** Where the lantern is, for the night light (GameWorld.drawNight). */
    public float lanternX() {
        return pos.x + lean();
    }

    public float lanternY() {
        return feetY() + lift() + POST_H + 0.1f;
    }

    private float lean() {
        return MathUtils.cosDeg(aimAngle) * 0.08f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        mouth -= delta;
        if (wake > 0) {
            wake -= delta;
            return;
        }
        if (hop > 0) {
            hop -= delta;
            pos.mulAdd(hopVel, delta);
            walkTime += delta;
            if (hop <= 0) {
                land(world);
            }
            return;
        }
        rest -= delta;
        if (rest <= 0) {
            float a = aimAngle + MathUtils.random(-25f, 25f);
            float dist = Math.min(speed * HOP, Math.max(0.3f, distanceToPlayer(world) - 0.6f));
            hopVel.set(MathUtils.cosDeg(a), MathUtils.sinDeg(a)).scl(dist / HOP);
            hop = HOP;
        }
    }

    private void land(GameWorld world) {
        hops++;
        world.shake(0.2f);
        world.sfx().play("body_fall", 0.6f);
        world.particles().burst(pos.x, feetY(), 8, 0.2f, 1f, 0.4f, 0.05f, POST_HI);
        rest = (angry() ? 0.45f : 0.8f) / world.getDifficulty();
        if (!canShoot(world)) {
            return;
        }
        float speed = 2.3f * world.getDifficulty();
        if (hops % 3 == 0) { // spits lantern sparks at the player
            mouth = 0.4f;
            for (int i = -2; i <= 2; i++) {
                shoot(world, aimAngle + i * 12f, speed * 1.35f).color.set(GLASS_HOT);
            }
            world.sfx().play("enemy_shoot", 0.7f);
            return;
        }
        int n = angry() ? 14 : 10;
        int gap = MathUtils.random(n - 1);
        float off = MathUtils.random(360f);
        for (int i = 0; i < n; i++) {
            if (i == gap || i == (gap + 1) % n) continue; // a gap to slip through
            shoot(world, off + i * 360f / n, speed);
        }
        world.sfx().play("enemy_shoot", 0.6f);
    }

    @Override
    public void dropLoot(GameWorld world) {
        world.spawnPickup(new Pickup(MathUtils.randomBoolean() ? Pickup.Type.KEY : Pickup.Type.BLANK, pos.x, pos.y));
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY() + lift();
        float lean = lean();
        drawFeet(b, d, pos.x, bottom, 0.07f);
        d.rect(b, pos.x - 0.1f, bottom, 0.2f, 0.1f, c(POST)); // the base it tore out of the ground
        // the post, bending toward the player in three segments
        float segH = POST_H / 3f;
        for (int i = 0; i < 3; i++) {
            float x = pos.x + lean * (i / 3f) - 0.05f;
            d.rect(b, x, bottom + 0.08f + i * segH, 0.1f, segH + 0.01f, c(POST));
            d.rect(b, x, bottom + 0.08f + i * segH, 0.03f, segH + 0.01f, c(POST_HI));
        }
        float lx = lanternX();
        float ly = bottom + POST_H + 0.1f;
        d.rect(b, lx - 0.15f, ly - 0.13f, 0.3f, 0.28f, c(POST));
        d.rect(b, lx - 0.12f, ly - 0.1f, 0.24f, 0.22f, c(angry() ? GLASS_HOT : GLASS));
        d.rect(b, lx - 0.04f, ly + 0.15f, 0.08f, 0.05f, c(POST));
        // an angry face in the glass: slanted brows, a mouth full of teeth
        float face = facingRight() ? 0.02f : -0.02f;
        drawEyes(b, d, lx + face, ly + 0.04f, 0.055f, 0.032f);
        d.rotRect(b, lx + face - 0.06f, ly + 0.085f, 0.07f, 0.018f, -20f, POST, 1f);
        d.rotRect(b, lx + face + 0.06f, ly + 0.085f, 0.07f, 0.018f, 20f, POST, 1f);
        float open = mouth > 0 || hop > 0 ? 0.05f : 0.025f;
        d.rect(b, lx + face - 0.08f, ly - 0.07f, 0.16f, open, MOUTH);
        for (int i = 0; i < 4; i++) {
            d.rect(b, lx + face - 0.075f + i * 0.04f, ly - 0.07f + open - 0.015f, 0.025f, 0.015f, TEETH);
        }
    }
}
