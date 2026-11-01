package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * Moon enemy (the owner's idea): a flying saucer. It hovers over furniture and craters, swaying round a
 * spot a little way from you, and takes turns at two things: its rim lights spin up and it throws out
 * three turning rings of orbs, or it switches on its tractor beam - a green cone down to the floor that
 * pulls you in towards its shadow - and when the beam cuts out, a ring bursts from where it shone.
 */
public class Ufo extends Enemy {

    private static final Color HULL = Color.valueOf("b8c0cc");
    private static final Color HULL_DARK = Color.valueOf("7a8494");
    private static final Color DOME = Color.valueOf("9ae8ff");
    private static final Color LIGHT = Color.valueOf("ffd23a");
    private static final Color ORB = Color.valueOf("ff8a3a");
    private static final Color BEAM = Color.valueOf("aaff8a");
    private static final Color PILOT = Color.valueOf("7ae05a");

    private enum Phase { HOVER, SPIN, BEAM }

    /** Rings in a spin, and the time between them. */
    private static final int RINGS = 3;
    private static final float RING_GAP = 0.3f;
    private static final float BEAM_TIME = 1.5f;
    private static final float PULL_SPEED = 0.85f;
    /** How far below the saucer its shadow (and the beam's spot) is. */
    private static final float HOVER = 0.42f;

    private Phase phase = Phase.HOVER;
    private float timer = MathUtils.random(1.6f, 2.4f);
    private float clock = MathUtils.random(10f);
    private int ringsLeft;
    private float ringTurn;
    private boolean nextBeam = MathUtils.randomBoolean();
    private final Vector2 drift = new Vector2();
    private final Vector2 step = new Vector2();

    public Ufo(float x, float y) {
        super(x, y, 38f);
        radius = 0.24f;
        speed = 0.9f;
        score = 50;
        flying = true;
    }

    @Override
    public float feetY() {
        return pos.y - HOVER;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        clock += delta;
        timer -= delta;
        Player p = world.getPlayer();
        if (phase != Phase.BEAM) { // sway round a spot above and to the side of the hero
            drift.set(p.centerX() + MathUtils.sin(clock * 0.8f) * 2f, p.hitboxY() + 1.5f + MathUtils.sin(clock * 1.3f) * 0.4f);
            Arena.now.clamp(drift, radius + 0.3f);
            drift.sub(pos);
            float dist = drift.len();
            float pace = phase == Phase.SPIN ? speed * 0.4f : speed;
            if (dist > 0.05f) {
                pos.mulAdd(drift.scl(1f / dist), Math.min(dist, pace * delta));
            }
        }
        switch (phase) {
            case HOVER:
                if (timer <= 0 && world.onScreen(pos.x, pos.y, 0.3f)) {
                    if (nextBeam) {
                        phase = Phase.BEAM;
                        timer = BEAM_TIME;
                        world.sfx().play("buzz", 0.5f);
                    } else {
                        phase = Phase.SPIN;
                        ringsLeft = RINGS;
                        ringTurn = MathUtils.random(360f);
                        timer = 0.45f / Math.min(1.3f, diff);
                        world.sfx().play("zap", 0.35f);
                    }
                    nextBeam = !nextBeam;
                }
                break;
            case SPIN:
                if (timer <= 0) {
                    ring(world, pos.x, pos.y, diff > 1.3f ? 11 : 9, ringTurn, 1.9f * diff);
                    ringTurn += 12f;
                    timer = RING_GAP;
                    if (--ringsLeft <= 0) {
                        phase = Phase.HOVER;
                        timer = MathUtils.random(1.8f, 2.6f) / diff;
                    }
                }
                break;
            default: // BEAM: pull the hero towards the spot under it
                step.set(pos.x - p.centerX(), feetY() - p.hitboxY());
                float dist = step.len();
                if (dist > 0.05f && !p.isDead() && !p.isRolling()) {
                    step.scl(Math.min(dist, PULL_SPEED * delta) / dist);
                    p.nudge(step.x, step.y, world);
                }
                if (MathUtils.random() < delta * 14f) {
                    world.particles().spawn(pos.x + MathUtils.random(-0.25f, 0.25f), feetY(), 0f, 0.7f, 0.5f, 0.025f, BEAM, 1f);
                }
                if (timer <= 0) {
                    ring(world, pos.x, feetY(), diff > 1.3f ? 12 : 10, MathUtils.random(360f), 1.6f * diff);
                    world.particles().burst(pos.x, feetY(), 12, 0.3f, 1.6f, 0.35f, 0.04f, BEAM);
                    phase = Phase.HOVER;
                    timer = MathUtils.random(1.8f, 2.6f) / diff;
                }
                break;
        }
    }

    private void ring(GameWorld world, float x, float y, int n, float start, float bulletSpeed) {
        for (int i = 0; i < n; i++) {
            float a = start + 360f * i / n;
            Bullet b = world.spawnEnemyBullet(x + MathUtils.cosDeg(a) * 0.2f, y + MathUtils.sinDeg(a) * 0.12f, a, bulletSpeed);
            b.color.set(ORB);
        }
        world.sfx().play("enemy_shoot", 0.4f);
    }

    /** The tractor beam's spot on the floor, shrinking to where the ring will burst. */
    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (phase != Phase.BEAM || !alive) return;
        float k = MathUtils.clamp(timer / BEAM_TIME, 0f, 1f);
        d.ring(b, pos.x, feetY(), 0.3f + 0.25f * k, TELEGRAPH, 0.8f);
        d.circle(b, pos.x, feetY(), 0.3f, BEAM, 0.25f);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float x = pos.x;
        float y = pos.y + MathUtils.sin(clock * 3f) * 0.025f;
        if (phase == Phase.BEAM) { // the cone of light down to the floor
            float flicker = 0.25f + 0.08f * MathUtils.sin(clock * 30f);
            for (int i = 0; i < 6; i++) {
                float t = i / 6f;
                float w = 0.16f + 0.4f * t;
                d.rect(b, x - w / 2f, y - 0.06f - (HOVER + 0.02f) * (t + 1f / 6f), w, (HOVER + 0.02f) / 6f + 0.01f, BEAM, flicker);
            }
        }
        // the saucer: a wide disc with a glass dome, its rim lights chasing round
        d.ellipse(b, x, y, 0.34f, 0.11f, Draw.OUTLINE_COLOR, 1f);
        d.ellipse(b, x, y, 0.32f, 0.095f, c(HULL), 1f);
        d.ellipse(b, x, y - 0.03f, 0.26f, 0.05f, c(HULL_DARK), 1f);
        d.circle(b, x, y + 0.07f, 0.14f, Draw.OUTLINE_COLOR);
        d.circle(b, x, y + 0.07f, 0.125f, c(DOME), 0.85f);
        d.rect(b, x - 0.13f, y - 0.005f, 0.26f, 0.06f, c(HULL)); // the dome sits in the hull
        // a little pilot peeking out
        float face = facingRight() ? 0.03f : -0.03f;
        d.circle(b, x + face, y + 0.1f, 0.05f, c(PILOT));
        d.circle(b, x + face - 0.02f, y + 0.11f, 0.012f, Draw.OUTLINE_COLOR);
        d.circle(b, x + face + 0.02f, y + 0.11f, 0.012f, Draw.OUTLINE_COLOR);
        d.circle(b, x - 0.06f, y + 0.14f, 0.025f, Color.WHITE, 0.5f); // a shine on the glass
        float spin = phase == Phase.SPIN ? clock * 900f : clock * 160f;
        for (int i = 0; i < 6; i++) {
            float a = spin + i * 60f;
            float s = MathUtils.sinDeg(a);
            if (s > 0.2f) continue; // the far side of the rim
            boolean hot = phase == Phase.SPIN || (phase == Phase.HOVER && timer < 0.4f);
            d.circle(b, x + MathUtils.cosDeg(a) * 0.27f, y - 0.01f + s * 0.05f, 0.028f, hot ? TELEGRAPH : LIGHT);
        }
    }
}
