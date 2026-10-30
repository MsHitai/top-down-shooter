package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * Moon enemy (the owner's idea): a fluffy white moon bunny with glowing red eyes - the rabbit in the moon,
 * gone bad. They come in pairs. It crouches (eyes flaring), takes a long, floaty low-gravity hop towards
 * you, and where it lands throws a ring of little carrots; on harder floors the ring is fuller. Its
 * veteran ({@link #veteran}: a torn ear, a scar and a medal) is tougher, hops quicker and lands with two
 * rings - a fast one and a slow one into its gaps.
 */
public class MoonBunny extends Enemy {

    private static final Color FUR = Color.valueOf("f4f2fc");
    private static final Color FUR_SHADE = Color.valueOf("cfd2e6");
    private static final Color EAR_PINK = Color.valueOf("ffb4cc");
    private static final Color EYE = Color.valueOf("ff2a3a");
    private static final Color CARROT = Color.valueOf("ff8a2a");
    private static final Color SCAR = Color.valueOf("b07a8a");

    private static final float CROUCH = 0.4f;
    private static final float HOP_TIME = 0.85f;
    private static final float HOP_HEIGHT = 0.7f;
    /** A veteran's hops take this much of the time. */
    private static final float VETERAN_HOP = 0.8f;

    private final boolean veteran;

    private float rest = MathUtils.random(0.6f, 1.6f);
    private float crouch;
    private float hop;
    private final Vector2 from = new Vector2();
    private final Vector2 to = new Vector2();

    public MoonBunny(float x, float y) {
        this(x, y, false);
    }

    /** The veteran: a torn ear, a scar, a medal - and two rings of carrots where it lands. */
    public static MoonBunny veteran(float x, float y) {
        return new MoonBunny(x, y, true);
    }

    private MoonBunny(float x, float y, boolean veteran) {
        super(x, y, veteran ? 20f : 14f);
        this.veteran = veteran;
        radius = 0.16f;
        speed = 0.4f;
        score = veteran ? 30 : 20;
    }

    @Override
    public String dexId() {
        return veteran ? "VeteranMoonBunny" : "MoonBunny";
    }

    private float hopTime() {
        return veteran ? HOP_TIME * VETERAN_HOP : HOP_TIME;
    }

    @Override
    public float feetY() {
        return pos.y - 0.13f;
    }

    /** How high off the ground it is right now (a long, floaty arc). */
    private float height() {
        if (hop <= 0) return 0f;
        float k = 1f - hop / hopTime();
        return HOP_HEIGHT * 4f * k * (1f - k);
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        if (hop > 0) {
            hop -= delta;
            float k = 1f - Math.max(0f, hop) / hopTime();
            pos.set(from).lerp(to, k);
            if (hop <= 0) {
                land(world);
            }
            return;
        }
        if (crouch > 0) {
            crouch -= delta;
            if (crouch <= 0) {
                // off it goes: towards you, a little to one side, about two units
                float a = aimAngle + MathUtils.random(-35f, 35f);
                float dist = Math.min(distanceToPlayer(world) - 0.6f, MathUtils.random(1.6f, 2.4f));
                dist = Math.max(0.8f, dist);
                from.set(pos);
                to.set(pos.x + MathUtils.cosDeg(a) * dist, pos.y + MathUtils.sinDeg(a) * dist);
                Arena.now.clamp(to, radius + 0.1f);
                hop = hopTime();
                world.sfx().play("dodge", 0.35f);
            }
            return;
        }
        rest -= delta;
        walkTime += delta * 0.5f; // (its nose twitches)
        if (rest <= 0 && world.onScreen(pos.x, pos.y, 0.3f)) {
            crouch = CROUCH;
            rest = MathUtils.random(1.0f, 1.8f) / world.getDifficulty() * (veteran ? 0.85f : 1f);
        } else if (!world.onScreen(pos.x, pos.y, 0.3f)) {
            keepDistance(delta, world, 2f, speed * 2f);
        }
    }

    /** Thump: a ring of carrots (a fuller one on harder floors), turned a little each time; a veteran's has a slower one in its gaps. */
    private void land(GameWorld world) {
        float diff = world.getDifficulty();
        int n = diff > 1.3f ? 10 : 8;
        float turn = MathUtils.random(360f / n);
        if (canSeePlayer || world.onScreen(pos.x, pos.y, 0.2f)) {
            for (int i = 0; i < n; i++) {
                Bullet b = shoot(world, turn + i * 360f / n, 1.7f * diff);
                b.shape = Bullet.Shape.DROP;
                b.color.set(CARROT);
                if (veteran) {
                    Bullet slow = shoot(world, turn + (i + 0.5f) * 360f / n, 1.1f * diff);
                    slow.shape = Bullet.Shape.DROP;
                    slow.color.set(CARROT);
                }
            }
            world.sfx().play("pop", 0.4f);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float h = height();
        float squash = crouch > 0 ? 1f - 0.25f * (1f - crouch / CROUCH) : 1f;
        float x = pos.x;
        float y = pos.y + h - (1f - squash) * 0.06f;
        float face = facingRight() ? 1f : -1f;
        if (h > 0) { // the shadow stays on the ground
            d.ellipse(b, pos.x, feetY(), radius * (1.1f - h * 0.5f), radius * 0.3f, Color.BLACK, 0.3f);
        }
        // ears (long, pink inside, one flopping)
        float ear = MathUtils.sin(walkTime * 6f) * 6f;
        d.line(b, x - 0.05f, y + 0.08f, 0.2f * squash, 0.07f, 100f + ear, Draw.OUTLINE_COLOR);
        float tear = veteran ? 0.7f : 1f; // (a veteran's right ear is torn short)
        d.line(b, x + 0.05f, y + 0.08f, 0.18f * squash * tear, 0.07f, 70f - ear * 0.5f, Draw.OUTLINE_COLOR);
        d.line(b, x - 0.05f, y + 0.08f, 0.19f * squash, 0.05f, 100f + ear, c(FUR));
        d.line(b, x + 0.05f, y + 0.08f, 0.17f * squash * tear, 0.05f, 70f - ear * 0.5f, c(FUR));
        d.line(b, x - 0.05f, y + 0.1f, 0.13f * squash, 0.02f, 100f + ear, c(EAR_PINK));
        d.line(b, x + 0.05f, y + 0.1f, 0.12f * squash * tear, 0.02f, 70f - ear * 0.5f, c(EAR_PINK));
        // a round fluffy body, a fluffier head
        d.circle(b, x, y - 0.03f, 0.165f * (2f - squash) * 0.5f + 0.08f, Draw.OUTLINE_COLOR);
        d.circle(b, x, y - 0.03f, 0.15f * (2f - squash) * 0.5f + 0.075f, c(FUR_SHADE));
        d.circle(b, x - 0.02f, y, 0.13f, c(FUR));
        for (int i = 0; i < 5; i++) { // tufts of fluff
            float a = i * 72f + walkTime * 20f;
            d.circle(b, x + MathUtils.cosDeg(a) * 0.12f, y - 0.02f + MathUtils.sinDeg(a) * 0.1f, 0.04f, c(FUR));
        }
        d.circle(b, x - face * 0.12f, y - 0.1f, 0.04f, c(FUR)); // cotton tail
        // red eyes, glowing brighter as it crouches
        float glow = crouch > 0 ? 1f - crouch / CROUCH : 0.3f;
        for (int i = -1; i <= 1; i += 2) {
            float ex = x + i * 0.045f + face * 0.03f;
            d.circle(b, ex, y + 0.02f, 0.06f, EYE, 0.25f * glow);
            d.circle(b, ex, y + 0.02f, 0.025f, EYE);
            d.circle(b, ex - 0.008f, y + 0.03f, 0.008f, Color.WHITE);
        }
        d.circle(b, x + face * 0.05f, y - 0.03f, 0.014f, EAR_PINK); // nose
        if (veteran) { // a scar across one eye, and the medal on its chest
            d.line(b, x - 0.075f + face * 0.03f, y + 0.07f, 0.1f, 0.014f, -60f, SCAR);
            MoonFolk.drawMedal(b, d, x - face * 0.06f, y - 0.1f, 1f);
        }
    }
}
