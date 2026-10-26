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
 * Moon enemy (the owner's idea): a little green alien with big black eyes and a bobbing antenna. It
 * beams away (gone - nothing hits it), beams back in somewhere near you, and fires a fan of glowing
 * green orbs from its ray gun - then wanders a moment and beams off again.
 */
public class Alien extends Enemy {

    private static final Color SKIN = Color.valueOf("7ae05a");
    private static final Color SKIN_SHADE = Color.valueOf("4aa83a");
    private static final Color EYE = Color.valueOf("12121e");
    private static final Color ORB = Color.valueOf("8aff6a");
    private static final Color BEAM = Color.valueOf("c8ffb4");

    private static final float FADE = 0.35f;

    private enum Phase { WANDER, OUT, IN, AIM }

    private Phase phase = Phase.WANDER;
    private float timer = MathUtils.random(1.5f, 2.5f);
    private final Vector2 spot = new Vector2();

    public Alien(float x, float y) {
        super(x, y, 16f);
        radius = 0.15f;
        speed = 0.6f;
        score = 25;
    }

    @Override
    public float feetY() {
        return pos.y - 0.14f;
    }

    /** Beamed out: bullets pass through. */
    @Override
    protected boolean isDodging() {
        return phase == Phase.OUT;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        timer -= delta;
        walkTime += delta;
        switch (phase) {
            case WANDER:
                keepDistance(delta, world, 2.2f, speed);
                if (timer <= 0 && world.onScreen(pos.x, pos.y, 0.3f)) {
                    phase = Phase.OUT;
                    timer = FADE;
                    world.sfx().play("zap", 0.35f);
                }
                break;
            case OUT:
                if (timer <= 0) { // pick a spot near the player, on the floor, with a clear shot
                    float px = world.getPlayer().centerX();
                    float py = world.getPlayer().hitboxY();
                    spot.set(pos);
                    for (int i = 0; i < 20; i++) {
                        float a = MathUtils.random(360f);
                        float dist = MathUtils.random(1.6f, 2.4f);
                        float x = px + MathUtils.cosDeg(a) * dist;
                        float y = py + MathUtils.sinDeg(a) * dist;
                        if (Arena.now.contains(x, y, radius + 0.2f) && world.pointIsFree(x, y, radius)
                                && world.hasLineOfSight(x, y, px, py, GameWorld.SIGHT_THICKNESS)) {
                            spot.set(x, y);
                            break;
                        }
                    }
                    pos.set(spot);
                    phase = Phase.IN;
                    timer = FADE;
                }
                break;
            case IN:
                if (timer <= 0) {
                    phase = Phase.AIM;
                    timer = 0.4f / world.getDifficulty();
                }
                break;
            default: // AIM: a moment to see it coming, then the fan
                if (timer <= 0) {
                    float diff = world.getDifficulty();
                    int n = diff > 1.3f ? 7 : 5;
                    for (int i = 0; i < n; i++) {
                        Bullet b = shoot(world, aimAngle + (i - (n - 1) / 2f) * 13f, 2.1f * diff);
                        b.color.set(ORB);
                        b.radius *= 1.2f;
                    }
                    world.sfx().play("enemy_shoot", 0.4f);
                    phase = Phase.WANDER;
                    timer = MathUtils.random(1.8f, 2.6f) / diff;
                }
                break;
        }
    }

    @Override
    protected boolean castsShadow() {
        return phase != Phase.OUT;
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float alpha = phase == Phase.OUT ? Math.max(0f, timer / FADE) : phase == Phase.IN ? 1f - Math.max(0f, timer / FADE) : 1f;
        if (phase == Phase.OUT || phase == Phase.IN) { // the beam it rides
            d.rect(b, pos.x - 0.12f, pos.y - 0.15f, 0.24f, 0.7f, BEAM, 0.35f * (1f - Math.abs(alpha - 0.5f) * 2f) + 0.1f);
        }
        if (alpha <= 0.02f) return;
        float face = facingRight() ? 1f : -1f;
        float x = pos.x;
        float y = pos.y;
        b.setColor(1f, 1f, 1f, alpha);
        drawFeet(b, d, x, y - 0.12f, 0.05f);
        // a little body, a big head
        d.ellipse(b, x, y - 0.06f, 0.08f, 0.08f, c(SKIN_SHADE), alpha);
        d.circle(b, x, y + 0.07f, 0.14f, Draw.OUTLINE_COLOR, alpha);
        d.circle(b, x, y + 0.07f, 0.125f, c(SKIN), alpha);
        // antenna with a bobbing ball
        float bob = MathUtils.sin(walkTime * 6f) * 0.02f;
        d.line(b, x, y + 0.18f, 0.1f, 0.02f, 90f + bob * 300f, c(SKIN_SHADE), alpha);
        d.circle(b, x + bob, y + 0.29f, 0.03f, ORB, alpha);
        // huge black eyes
        for (int i = -1; i <= 1; i += 2) {
            d.ellipse(b, x + i * 0.05f + face * 0.02f, y + 0.07f, 0.035f, 0.05f, EYE, alpha);
            d.circle(b, x + i * 0.05f + face * 0.02f - 0.01f, y + 0.09f, 0.01f, Color.WHITE, alpha);
        }
        // the ray gun, glowing as it aims
        d.line(b, x + face * 0.06f, y - 0.05f, 0.14f, 0.04f, aimAngle, GUN, alpha);
        if (phase == Phase.AIM) {
            d.circle(b, x + face * 0.06f + MathUtils.cosDeg(aimAngle) * 0.16f, y - 0.05f + MathUtils.sinDeg(aimAngle) * 0.16f,
                    0.05f, ORB, 0.8f * alpha);
        }
        b.setColor(Color.WHITE);
    }
}
