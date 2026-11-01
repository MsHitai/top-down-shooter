package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * Sea enemy, one of the big ones: a giant purple octopus. It slinks about at a distance and takes turns
 * at two things: its eight arms whirl and pour out a turning spiral of bullets, or it squirts a big blob
 * of ink at you that bursts into a ring of ink drops a moment later (if it doesn't hit something first).
 */
public class GiantOctopus extends Enemy {

    private static final Color SKIN = Color.valueOf("9a5ad8");
    private static final Color SKIN_DARK = Color.valueOf("6a3aa8");
    private static final Color SUCKER = Color.valueOf("f2c0ff");
    private static final Color INK = Color.valueOf("2a2440");
    private static final Color SPIRAL = Color.valueOf("e08aff");

    private enum State { SLINK, WHIRL, SQUIRT }

    private static final int ARMS = 8;
    private static final float WHIRL_TIME = 1.6f;
    private static final float WHIRL_EVERY = 0.16f;
    private static final float SQUIRT_TIME = 0.5f;
    /** The ink blob bursts this long after it's squirted. */
    static final float INK_BURST = 0.8f;

    private State state = State.SLINK;
    private float timer = MathUtils.random(1.6f, 2.4f);
    private float clock;
    private float spin;
    private float shotTimer;
    private boolean nextWhirl = MathUtils.randomBoolean();
    private Bullet ink;
    private float inkTime;

    public GiantOctopus(float x, float y) {
        super(x, y, 100f);
        radius = 0.34f;
        speed = 0.45f;
        score = 80;
    }

    @Override
    public float feetY() {
        return pos.y - 0.24f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        timer -= delta;
        clock += delta;
        if (ink != null) {
            inkTime += delta;
            if (!ink.alive) {
                ink = null;
            } else if (inkTime >= INK_BURST) {
                ink.alive = false;
                int n = diff > 1.3f ? 12 : 10;
                float turn = MathUtils.random(360f);
                for (int i = 0; i < n; i++) {
                    Bullet s = world.spawnEnemyBullet(ink.pos.x, ink.pos.y, turn + i * 360f / n, 2f * diff);
                    s.shape = Bullet.Shape.DROP;
                    s.color.set(INK);
                }
                world.particles().burst(ink.pos.x, ink.pos.y, 10, 0.2f, 1f, 0.4f, 0.05f, INK);
                world.sfx().play("pop", 0.5f);
                ink = null;
            }
        }
        switch (state) {
            case SLINK:
                keepDistance(delta, world, 2.8f, speed);
                if (timer <= 0 && canShoot(world)) {
                    state = nextWhirl ? State.WHIRL : State.SQUIRT;
                    timer = nextWhirl ? WHIRL_TIME : SQUIRT_TIME / Math.min(1.3f, diff);
                    nextWhirl = !nextWhirl;
                    shotTimer = 0.3f;
                    spin = aimAngle;
                }
                break;
            case WHIRL: // a turning spiral out of every arm
                spin += delta * 70f * Math.min(1.3f, diff);
                shotTimer -= delta;
                if (shotTimer <= 0) {
                    shotTimer = WHIRL_EVERY;
                    for (int i = 0; i < ARMS; i += diff > 1.3f ? 1 : 2) {
                        Bullet b = shoot(world, spin + i * 360f / ARMS, 1.8f * diff);
                        b.color.set(SPIRAL);
                    }
                    world.sfx().play("enemy_shoot", 0.25f);
                }
                if (timer <= 0) {
                    state = State.SLINK;
                    timer = MathUtils.random(1.8f, 2.6f) / diff;
                }
                break;
            default: // SQUIRT: swells up, then the ink
                if (timer <= 0) {
                    ink = shoot(world, aimAngle, 2.2f * diff);
                    ink.radius *= 2.2f;
                    ink.hitScale = 0.7f;
                    ink.color.set(INK);
                    inkTime = 0f;
                    world.sfx().play("pop", 0.6f);
                    state = State.SLINK;
                    timer = MathUtils.random(1.8f, 2.6f) / diff;
                }
                break;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float x = pos.x;
        float y = pos.y;
        boolean whirl = state == State.WHIRL;
        // eight arms, curling: each one a chain of shrinking blobs
        for (int i = 0; i < ARMS; i++) {
            float base = (whirl ? spin : 0f) + i * 360f / ARMS + 22.5f;
            float ax = x;
            float ay = y - 0.06f;
            for (int k = 0; k < 5; k++) {
                float a = base + MathUtils.sin(clock * 3f + i + k * 0.6f) * 18f + k * (whirl ? 14f : 6f);
                ax += MathUtils.cosDeg(a) * 0.07f;
                ay += MathUtils.sinDeg(a) * 0.05f;
                float r = 0.06f - k * 0.009f;
                d.circle(b, ax, ay, r + 0.012f, Draw.OUTLINE_COLOR);
                d.circle(b, ax, ay, r, c(k % 2 == 0 ? SKIN : SKIN_DARK));
                if (k == 1 || k == 3) d.circle(b, ax, ay, r * 0.35f, SUCKER, 0.7f);
            }
        }
        // the head: a big dome, swelling when it's about to squirt
        float swell = state == State.SQUIRT ? 1f + 0.12f * (1f - Math.max(0f, timer) / SQUIRT_TIME) : 1f;
        float bob = MathUtils.sin(clock * 2f) * 0.02f;
        d.ellipse(b, x, y + 0.1f + bob, 0.25f * swell, 0.24f * swell, Draw.OUTLINE_COLOR, 1f);
        d.ellipse(b, x, y + 0.1f + bob, 0.23f * swell, 0.22f * swell, c(state == State.SQUIRT && ((int) (timer * 16f)) % 2 == 0
                ? TELEGRAPH : SKIN), 1f);
        d.circle(b, x - 0.08f, y + 0.22f + bob, 0.04f, Color.WHITE, 0.35f);
        for (int i = -1; i <= 1; i += 2) { // spots
            d.circle(b, x + i * 0.12f, y + 0.18f + bob, 0.03f, c(SKIN_DARK));
        }
        drawEyes(b, d, x + (facingRight() ? 0.02f : -0.02f), y + 0.04f + bob, 0.09f, 0.045f);
    }
}
