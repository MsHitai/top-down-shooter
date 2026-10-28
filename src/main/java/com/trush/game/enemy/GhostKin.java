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
 * The ghost of a bullet kin: floats over furniture, fades away (bullets go right through it), drifts
 * - barely there - to somewhere near you with a clear shot, fades back in and wails out a slow ring of
 * swirling ghost bullets with one quicker one aimed at you.
 */
public class GhostKin extends Enemy {

    private static final Color SHEET = Color.valueOf("e4e0f8");
    private static final Color SHEET_SHADE = Color.valueOf("b0a8d8");
    private static final Color HOLLOW = Color.valueOf("2a2240");
    private static final Color WISP = Color.valueOf("b48cff");

    static final float FADE = 0.4f;
    private static final float DRIFT_SPEED = 2.4f;
    private static final float WAIL = 0.5f;
    /** Bullets in its ring. */
    static final int RING = 8;

    enum Phase { FLOAT, OUT, DRIFT, IN, WAIL }

    private Phase phase = Phase.FLOAT;
    private float timer = MathUtils.random(1.4f, 2.4f);
    private final Vector2 spot = new Vector2();
    private final Vector2 step = new Vector2();
    private float bob = MathUtils.random(10f);

    public GhostKin(float x, float y) {
        super(x, y, 15f);
        radius = 0.19f;
        speed = 0.55f;
        score = 25;
        flying = true;
    }

    Phase phase() {
        return phase;
    }

    @Override
    public float feetY() {
        return pos.y - 0.26f;
    }

    /** Faded away: bullets pass through. */
    @Override
    protected boolean isDodging() {
        return phase == Phase.OUT || phase == Phase.DRIFT || (phase == Phase.IN && timer > FADE / 2f);
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        timer -= delta;
        bob += delta;
        walkTime += delta;
        switch (phase) {
            case FLOAT:
                keepDistance(delta, world, 2.4f, speed);
                if (timer <= 0 && world.onScreen(pos.x, pos.y, 0.3f)) {
                    phase = Phase.OUT;
                    timer = FADE;
                }
                break;
            case OUT:
                if (timer <= 0) {
                    pickSpot(world);
                    phase = Phase.DRIFT;
                }
                break;
            case DRIFT: // straight there, through anything
                step.set(spot).sub(pos);
                float len = step.len();
                if (len <= DRIFT_SPEED * delta) {
                    pos.set(spot);
                    phase = Phase.IN;
                    timer = FADE;
                } else {
                    pos.mulAdd(step, DRIFT_SPEED * delta / len);
                }
                break;
            case IN:
                if (timer <= 0) {
                    phase = Phase.WAIL;
                    timer = WAIL / world.getDifficulty();
                }
                break;
            default: // WAIL
                if (timer <= 0) {
                    wail(world);
                    phase = Phase.FLOAT;
                    timer = MathUtils.random(1.6f, 2.6f) / world.getDifficulty();
                }
                break;
        }
    }

    /** Somewhere 1.6-2.4 from the player with a clear shot (or where it is, if nowhere turns up). */
    private void pickSpot(GameWorld world) {
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
                return;
            }
        }
    }

    private void wail(GameWorld world) {
        float diff = world.getDifficulty();
        int n = diff > 1.25f ? RING + 2 : RING;
        float turn = MathUtils.random(360f);
        float swirl = MathUtils.randomSign() * 28f;
        for (int i = 0; i < n; i++) {
            Bullet b = shoot(world, turn + i * 360f / n, 1.5f * diff);
            b.color.set(WISP);
            b.curve = swirl;
            b.life = 5f;
        }
        shoot(world, aimAngle, 2.6f * diff).color.set(WISP);
        world.sfx().play("enemy_shoot", 0.45f);
    }

    /** How solid it is now: faint while drifting, fading in and out at either end. */
    private float alpha() {
        switch (phase) {
            case OUT:
                return 0.15f + 0.85f * Math.max(0f, timer / FADE);
            case DRIFT:
                return 0.15f;
            case IN:
                return 0.15f + 0.85f * (1f - Math.max(0f, timer / FADE));
            default:
                return 0.9f;
        }
    }

    @Override
    protected boolean castsShadow() {
        return phase == Phase.FLOAT || phase == Phase.WAIL;
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float alpha = alpha();
        float x = pos.x;
        float y = pos.y + MathUtils.sin(bob * 3f) * 0.03f;
        float face = facingRight() ? 1f : -1f;
        // a bullet-shaped sheet: round top, a tattered hem that ripples
        d.circle(b, x, y + 0.06f, 0.18f, c(SHEET), alpha);
        d.rect(b, x - 0.18f, y - 0.16f, 0.36f, 0.22f, c(SHEET), alpha);
        d.rect(b, x + 0.12f * face - 0.03f, y - 0.16f, 0.06f, 0.24f, c(SHEET_SHADE), alpha * 0.8f);
        for (int i = 0; i < 4; i++) {
            float hx = x - 0.135f + i * 0.09f;
            float hy = y - 0.17f + MathUtils.sin(bob * 7f + i * 1.7f) * 0.02f;
            d.circle(b, hx, hy, 0.045f, c(SHEET), alpha);
        }
        // hollow eyes and a wailing mouth
        for (int i = -1; i <= 1; i += 2) {
            d.ellipse(b, x + i * 0.06f + face * 0.03f, y + 0.07f, 0.03f, 0.045f, HOLLOW, alpha);
        }
        float mouth = phase == Phase.WAIL ? 0.045f : 0.022f;
        d.ellipse(b, x + face * 0.03f, y - 0.04f, 0.025f, mouth, HOLLOW, alpha);
        if (phase == Phase.WAIL) { // the wail building up
            float k = 1f - Math.max(0f, timer) * 2f;
            d.ring(b, x, y, 0.22f + 0.12f * k, WISP, 0.6f * alpha);
        }
    }
}
