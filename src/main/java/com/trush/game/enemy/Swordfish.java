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
 * Sea enemy: a swordfish. It circles at a distance, lines up (a faint line shows where it's going) and
 * shoots off like an arrow, sword first. If the dash ends in a wall, a rock or a lump of coral, its nose
 * gets stuck there for a moment - it wiggles to get free, and that's your chance. On harder floors it
 * blows a ring of bubbles when it pulls free.
 */
public class Swordfish extends Enemy {

    private static final Color BODY = Color.valueOf("4a7ab8");
    private static final Color BODY_DARK = Color.valueOf("2e5088");
    private static final Color BELLY = Color.valueOf("d8e8f5");
    private static final Color SWORD = Color.valueOf("c8d0dc");
    private static final Color FIN = Color.valueOf("6a4ab0");
    private static final Color AIM = Color.valueOf("bfefff");
    private static final Color BUBBLE = Color.valueOf("bfefff");
    private static final Color DIZZY = Color.valueOf("ffe066");

    private enum State { SWIM, AIM, DASH, STUCK, RECOVER }

    private static final float AIM_TIME = 0.7f;
    private static final float LOCK = 0.2f;
    private static final float DASH_TIME = 0.9f;
    private static final float DASH_SPEED = 6.5f;
    private static final float STUCK_TIME = 1.3f;
    /** From the middle of the body to the tip of the sword. */
    private static final float SWORD_TIP = 0.42f;

    private State state = State.SWIM;
    private float timer = MathUtils.random(1.4f, 2.4f);
    private float heading;
    private float lineLength;
    private float time = MathUtils.random(5f);
    private final Vector2 tip = new Vector2();

    public Swordfish(float x, float y) {
        super(x, y, 24f);
        radius = 0.17f;
        speed = 1.1f;
        score = 35;
    }

    @Override
    public float feetY() {
        return pos.y - 0.2f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        float diff = world.getDifficulty();
        timer -= delta;
        switch (state) {
            case SWIM:
                keepDistance(delta, world, 2.6f, speed);
                heading = turnToward(heading, aimAngle, delta * 400f);
                if (timer <= 0 && canShoot(world)) {
                    state = State.AIM;
                    timer = AIM_TIME / Math.min(1.25f, diff);
                }
                break;
            case AIM:
                if (timer > LOCK) {
                    heading = turnToward(heading, aimAngle, delta * 900f);
                }
                lineLength = world.rayLength(pos.x, pos.y, heading, 8f);
                pos.sub(MathUtils.cosDeg(heading) * 0.15f * delta, MathUtils.sinDeg(heading) * 0.15f * delta); // draws back
                if (timer <= 0) {
                    state = State.DASH;
                    timer = DASH_TIME;
                    world.sfx().play("swish", 0.7f);
                }
                break;
            case DASH:
                knock.setZero();
                float s = DASH_SPEED * Math.min(1.3f, diff) * delta;
                pos.add(MathUtils.cosDeg(heading) * s, MathUtils.sinDeg(heading) * s);
                tip.set(pos).add(MathUtils.cosDeg(heading) * SWORD_TIP, MathUtils.sinDeg(heading) * SWORD_TIP);
                if (!Arena.now.contains(tip.x, tip.y, -0.05f) || !world.pointIsFree(tip.x, tip.y, 0.02f)) {
                    state = State.STUCK; // sword in the wall
                    timer = STUCK_TIME;
                    world.sfx().play("break", 0.45f);
                    world.shake(0.08f);
                    world.particles().burst(tip.x, tip.y, 8, 0.3f, 1.2f, 0.35f, 0.035f, Color.WHITE);
                } else if (timer <= 0) {
                    state = State.RECOVER;
                    timer = 0.5f;
                }
                break;
            case STUCK:
                knock.setZero();
                if (timer <= 0) {
                    pos.sub(MathUtils.cosDeg(heading) * 0.1f, MathUtils.sinDeg(heading) * 0.1f); // pops free
                    if (diff > 1.15f) {
                        for (int i = 0; i < 6; i++) {
                            Bullet b = shoot(world, heading + 180f + i * 60f, 1.7f * diff);
                            b.color.set(BUBBLE);
                        }
                    }
                    world.sfx().play("pop", 0.5f);
                    state = State.RECOVER;
                    timer = 0.4f;
                }
                break;
            default: // RECOVER
                if (timer <= 0) {
                    state = State.SWIM;
                    timer = MathUtils.random(1.6f, 2.4f) / diff;
                }
                break;
        }
    }

    private static float turnToward(float from, float to, float maxStep) {
        float diff = ((to - from + 540f) % 360f) - 180f;
        return from + MathUtils.clamp(diff, -maxStep, maxStep);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        if (state == State.AIM) {
            d.line(b, pos.x, pos.y, lineLength, 0.02f, heading, timer <= LOCK ? TELEGRAPH : AIM, 0.5f);
        }
        float wiggle = state == State.STUCK ? MathUtils.sin(time * 40f) * 8f : MathUtils.sin(time * 6f) * 4f;
        float a = heading + wiggle;
        float ux = MathUtils.cosDeg(a);
        float uy = MathUtils.sinDeg(a);
        // which side is "up" for the fish: its fins stay on top whichever way it swims
        float up = ux >= 0 ? 1f : -1f;
        float nx = -uy * up;
        float ny = ux * up;
        float x = pos.x;
        float y = pos.y;
        // the sword
        d.line(b, x + ux * 0.12f, y + uy * 0.12f, SWORD_TIP - 0.1f, 0.045f, a, Draw.OUTLINE_COLOR);
        d.line(b, x + ux * 0.12f, y + uy * 0.12f, SWORD_TIP - 0.11f, 0.03f, a, c(SWORD));
        // the tail: a forked crescent
        float tx = x - ux * 0.22f;
        float ty = y - uy * 0.22f;
        d.rotRect(b, tx + nx * 0.05f, ty + ny * 0.05f, 0.14f, 0.05f, a + 55f * up, c(BODY_DARK), 1f);
        d.rotRect(b, tx - nx * 0.05f, ty - ny * 0.05f, 0.14f, 0.05f, a - 55f * up, c(BODY_DARK), 1f);
        // the body, sleek and long
        d.rotRect(b, x, y, 0.42f, 0.17f, a, Draw.OUTLINE_COLOR, 1f);
        d.rotRect(b, x, y, 0.4f, 0.15f, a, c(BODY), 1f);
        d.rotRect(b, x - nx * 0.035f, y - ny * 0.035f, 0.34f, 0.06f, a, c(BELLY), 1f);
        d.circle(b, x + ux * 0.14f, y + uy * 0.14f, 0.075f, c(BODY));
        // the tall sail of a dorsal fin
        d.rotRect(b, x + nx * 0.1f - ux * 0.02f, y + ny * 0.1f - uy * 0.02f, 0.2f, 0.09f, a + 15f * up, c(FIN), 1f);
        // a mean little eye
        float ex = x + ux * 0.15f + nx * 0.025f;
        float ey = y + uy * 0.15f + ny * 0.025f;
        d.circle(b, ex, ey, 0.028f, EYE_WHITE);
        d.circle(b, ex + ux * 0.008f, ey + uy * 0.008f, 0.016f, PUPIL);
        d.rotRect(b, ex + nx * 0.03f, ey + ny * 0.03f, 0.06f, 0.015f, a - 20f * up, Draw.OUTLINE_COLOR, 1f);
        if (state == State.STUCK) { // little stars: stuck!
            for (int i = 0; i < 2; i++) {
                float s = time * 300f + i * 180f;
                d.circle(b, x + MathUtils.cosDeg(s) * 0.12f, y + 0.2f + MathUtils.sinDeg(s) * 0.03f, 0.022f, DIZZY);
            }
        }
    }
}
