package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * The Red Shotgun Kin's grown-up cousin, from floor 4 on in its place: a charcoal shell with a stubby
 * sawn-off. It doesn't stand and trade shots - it revs up, rushes at you and fires both barrels point blank:
 * a wide, short spray, then a second one filling the gaps. Then it backs off to catch its breath. Rolling
 * away (or through it) is the answer, standing still isn't.
 */
public class SawnOffKin extends Enemy {

    private static final Color SHELL = Color.valueOf("3b3f46");
    private static final Color SHELL_DARK = Color.valueOf("26292e");
    private static final Color STRIPE = Color.valueOf("ff9a2e");
    private static final Color BRASS = Color.valueOf("d9b24c");

    private static final float W = 0.42f;
    private static final float H = 0.56f;
    static final float REV = 0.5f;
    /** It rushes until it's this close, or for this long. */
    static final float CLOSE = 1.3f;
    static final float RUSH_TIME = 0.8f;
    static final float RUSH_SPEED = 4.2f;
    /** It only rushes at a hero this near. */
    static final float REACH = 4.5f;
    static final int PELLETS = 9;
    static final float FAN = 80f;
    private static final float SECOND_BARREL = 0.28f;
    private static final float PELLET_SPEED = 3.6f;
    /** Short-lived pellets: a sawn-off spreads fast and doesn't carry. */
    private static final float PELLET_LIFE = 0.75f;
    private static final float RECOVER = 1.4f;

    private enum State { WALK, REV, RUSH, BLAST, RECOVER }

    private State state = State.WALK;
    private float timer;
    private float fireTimer = MathUtils.random(1.4f, 2.2f);
    private float aim;
    private final Vector2 dir = new Vector2();

    public SawnOffKin(float x, float y) {
        super(x, y, 22f);
        radius = 0.23f;
        speed = 0.9f;
        score = 30;
    }

    @Override
    public float feetY() {
        return pos.y - H / 2f;
    }

    boolean isRushing() {
        return state == State.RUSH;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        timer -= delta;
        switch (state) {
            case WALK:
                keepDistance(delta, world, 2.6f, speed);
                fireTimer -= delta;
                if (fireTimer <= 0 && canShoot(world) && distanceToPlayer(world) < REACH) {
                    state = State.REV;
                    timer = REV;
                }
                break;
            case REV: // stamping its feet, barrels glowing
                if (timer <= 0) {
                    state = State.RUSH;
                    timer = RUSH_TIME;
                    dir.set(MathUtils.cosDeg(aimAngle), MathUtils.sinDeg(aimAngle));
                    world.sfx().play("swish", 0.5f);
                }
                break;
            case RUSH:
                pos.mulAdd(dir, RUSH_SPEED * delta);
                walkTime += delta * 2f;
                if (timer <= 0 || distanceToPlayer(world) < CLOSE) {
                    blast(world, diff, 0f);
                    state = State.BLAST;
                    timer = SECOND_BARREL;
                }
                break;
            case BLAST:
                if (timer <= 0) {
                    blast(world, diff, FAN / (PELLETS - 1) / 2f); // the second barrel, into the gaps
                    state = State.RECOVER;
                    timer = RECOVER;
                }
                break;
            case RECOVER:
                keepDistance(delta, world, 3.2f, speed * 0.7f);
                if (timer <= 0) {
                    state = State.WALK;
                    fireTimer = MathUtils.random(1.2f, 2.0f) / diff;
                }
                break;
        }
    }

    private void blast(GameWorld world, float diff, float offset) {
        if (offset == 0f) {
            aim = leadAngle(world, PELLET_SPEED * diff, 0.5f);
        }
        for (int i = 0; i < PELLETS; i++) {
            Bullet b = shoot(world, aim - FAN / 2f + FAN * i / (PELLETS - 1) + offset, PELLET_SPEED * diff);
            b.life = PELLET_LIFE;
        }
        world.sfx().play("shotgun", 0.6f);
        world.shake(0.1f);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float face = facingRight() ? 1 : -1;
        float shakeX = state == State.REV ? MathUtils.sin(walkTime * 60f + timer * 90f) * 0.02f : 0f;
        float x = pos.x + shakeX;
        drawFeet(b, d, x, bottom, 0.1f);
        d.rect(b, x - W / 2f, bottom + H * 0.2f, W, H * 0.8f, c(SHELL));
        d.rect(b, x - W / 2f, bottom + H * 0.88f, W, H * 0.12f, c(SHELL_DARK));
        d.rect(b, x - W / 2f, bottom + H * 0.5f, W, 0.04f, c(STRIPE)); // a hazard stripe round its middle
        d.rect(b, x + W / 2f - 0.05f, bottom + H * 0.2f, 0.05f, H * 0.68f, c(SHELL_DARK));
        d.rect(b, x - W / 2f - 0.015f, bottom, W + 0.03f, H * 0.22f, c(BRASS));
        drawEyes(b, d, x, bottom + H * 0.66f, 0.075f, 0.043f);
        for (int i = -1; i <= 1; i += 2) { // always scowling
            float ex = x + i * 0.075f + face * 0.0375f;
            d.line(b, ex - 0.045f, bottom + H * 0.66f + 0.07f - i * 0.012f, 0.09f, 0.025f, i * 20f, PUPIL);
        }

        // two stubby barrels side by side
        float gx = x + face * 0.14f;
        float gy = bottom + H * 0.32f;
        float px = -MathUtils.sinDeg(aimAngle) * 0.03f;
        float py = MathUtils.cosDeg(aimAngle) * 0.03f;
        d.line(b, gx + px, gy + py, 0.2f, 0.05f, aimAngle, GUN);
        d.line(b, gx - px, gy - py, 0.2f, 0.05f, aimAngle, GUN);
        if (state == State.REV || state == State.RUSH) {
            float mx = gx + MathUtils.cosDeg(aimAngle) * 0.2f;
            float my = gy + MathUtils.sinDeg(aimAngle) * 0.2f;
            float k = state == State.REV ? 1 - timer / REV : 1f;
            d.circle(b, mx, my, 0.06f + 0.05f * k, TELEGRAPH, 0.9f);
        }
    }
}
