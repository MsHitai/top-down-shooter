package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * Sea enemy, one of the big ones (the owner's idea): a giant red crab. It scuttles sideways round you,
 * then either raises a claw - it glows - scuttles in and snaps, throwing a crescent of shell shards, or
 * plants itself, lifts both claws and claps them: a ring of bubbles, a second one into its gaps on harder
 * floors.
 */
public class GiantCrab extends Enemy {

    private static final Color SHELL = Color.valueOf("d8483a");
    private static final Color SHELL_DARK = Color.valueOf("9a2a24");
    private static final Color BELLY = Color.valueOf("f2a07a");
    private static final Color SHARD = Color.valueOf("ffb08a");
    private static final Color BUBBLE = Color.valueOf("bfeaff");

    private enum State { SCUTTLE, RAISE, SNAP, CLAP }

    private static final float RAISE_TIME = 0.55f;
    private static final float SNAP_TIME = 0.35f;
    private static final float SNAP_SPEED = 3.2f;
    private static final float CLAP_TIME = 0.7f;

    private State state = State.SCUTTLE;
    private float timer = MathUtils.random(1.6f, 2.4f);
    /** Which way round the player it scuttles. */
    private float side = MathUtils.randomBoolean() ? 1f : -1f;
    private boolean nextClap = MathUtils.randomBoolean();
    private float snapAngle;
    private float clock;

    public GiantCrab(float x, float y) {
        super(x, y, 95f);
        radius = 0.36f;
        speed = 0.75f;
        score = 80;
    }

    @Override
    public float feetY() {
        return pos.y - 0.2f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        timer -= delta;
        clock += delta;
        switch (state) {
            case SCUTTLE: { // sideways round the hero, keeping a claw's reach and a bit
                keepDistance(delta, world, 2.1f, speed);
                float sx = -MathUtils.sinDeg(aimAngle) * side;
                float sy = MathUtils.cosDeg(aimAngle) * side;
                if (world.pointIsFree(pos.x + sx * 0.3f, pos.y + sy * 0.3f, radius)) {
                    pos.add(sx * speed * 0.6f * delta, sy * speed * 0.6f * delta);
                    walkTime += delta;
                } else {
                    side = -side;
                }
                if (MathUtils.random() < delta * 0.3f) side = -side;
                if (timer <= 0 && canShoot(world)) {
                    state = nextClap ? State.CLAP : State.RAISE;
                    timer = (nextClap ? CLAP_TIME : RAISE_TIME) / Math.min(1.3f, diff);
                    nextClap = !nextClap;
                    world.sfx().play("buzz", 0.35f);
                }
                break;
            }
            case RAISE:
                snapAngle = aimAngle;
                if (timer <= 0) {
                    state = State.SNAP;
                    timer = SNAP_TIME;
                }
                break;
            case SNAP: {
                float s = SNAP_SPEED * delta;
                float nx = pos.x + MathUtils.cosDeg(snapAngle) * s;
                float ny = pos.y + MathUtils.sinDeg(snapAngle) * s;
                if (world.pointIsFree(nx, ny, radius)) pos.set(nx, ny);
                if (timer <= 0) {
                    int n = diff > 1.3f ? 9 : 7;
                    for (int i = 0; i < n; i++) {
                        Bullet b = shoot(world, snapAngle + (i - (n - 1) / 2f) * 12f, (2.3f + 0.15f * Math.abs(i - (n - 1) / 2f)) * diff);
                        b.shape = Bullet.Shape.DIAMOND;
                        b.color.set(SHARD);
                    }
                    world.sfx().play("chomp", 0.5f);
                    world.shake(0.1f);
                    state = State.SCUTTLE;
                    timer = MathUtils.random(1.8f, 2.6f) / diff;
                }
                break;
            }
            default: // CLAP: both claws up, then together
                if (timer <= 0) {
                    clapRing(world, diff, aimAngle);
                    if (diff > 1.3f) {
                        clapRing(world, diff * 0.8f, aimAngle + 180f / 14f);
                    }
                    world.particles().burst(pos.x, pos.y + 0.3f, 10, 0.2f, 1.2f, 0.4f, 0.04f, BUBBLE);
                    world.sfx().play("pop", 0.7f);
                    state = State.SCUTTLE;
                    timer = MathUtils.random(1.8f, 2.6f) / diff;
                }
                break;
        }
    }

    private void clapRing(GameWorld world, float diff, float start) {
        int n = 14;
        for (int i = 0; i < n; i++) {
            Bullet b = shoot(world, start + 360f * i / n, 1.7f * diff);
            b.bubble = true;
            b.color.set(BUBBLE);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float x = pos.x;
        float y = pos.y;
        boolean scuttling = state == State.SCUTTLE;
        // four legs a side, ticking as it scuttles
        for (int s = -1; s <= 1; s += 2) {
            for (int i = 0; i < 4; i++) {
                float tick = scuttling ? MathUtils.sin(walkTime * 14f + i * 1.6f) * 12f : 0f;
                float ly = y - 0.12f + i * 0.06f;
                d.line(b, x + s * 0.2f, ly, 0.22f, 0.05f, s > 0 ? -25f + tick : 205f - tick, Draw.OUTLINE_COLOR);
                d.line(b, x + s * 0.2f, ly, 0.21f, 0.035f, s > 0 ? -25f + tick : 205f - tick, c(SHELL_DARK));
            }
        }
        // the shell: wide and low, a paler belly rim
        d.ellipse(b, x, y, 0.33f, 0.22f, Draw.OUTLINE_COLOR, 1f);
        d.ellipse(b, x, y - 0.02f, 0.31f, 0.19f, c(BELLY), 1f);
        d.ellipse(b, x, y + 0.02f, 0.3f, 0.18f, c(SHELL), 1f);
        for (int i = -1; i <= 1; i++) { // bumps on the shell
            d.circle(b, x + i * 0.12f, y + 0.08f - Math.abs(i) * 0.03f, 0.035f, c(SHELL_DARK));
        }
        d.circle(b, x - 0.12f, y + 0.13f, 0.03f, Color.WHITE, 0.35f);
        // eyes on stalks
        for (int i = -1; i <= 1; i += 2) {
            float ex = x + i * 0.08f;
            d.rect(b, ex - 0.012f, y + 0.16f, 0.024f, 0.1f, c(SHELL_DARK));
            d.circle(b, ex, y + 0.28f, 0.045f, Draw.OUTLINE_COLOR);
            d.circle(b, ex, y + 0.28f, 0.035f, Color.WHITE);
            d.circle(b, ex + MathUtils.cosDeg(aimAngle) * 0.012f, y + 0.28f + MathUtils.sinDeg(aimAngle) * 0.012f, 0.018f,
                    Draw.OUTLINE_COLOR);
        }
        // the claws: up and open when about to snap or clap, glowing on the one about to snap
        float face = facingRight() ? 1f : -1f;
        for (int s = -1; s <= 1; s += 2) {
            boolean raised = state == State.CLAP || (state != State.SCUTTLE && s == face);
            float cx = x + s * (raised ? 0.34f : 0.38f);
            float cy = y + (raised ? 0.26f : 0.02f) + (scuttling ? MathUtils.sin(clock * 3f + s) * 0.02f : 0f);
            if (state == State.CLAP && timer < 0.12f) cx = x + s * 0.12f; // together - clap!
            Color glow = (state == State.RAISE && s == face) || (state == State.CLAP && timer > 0.12f)
                    ? (((int) (timer * 16f)) % 2 == 0 ? TELEGRAPH : SHELL) : SHELL;
            float ax = x + s * 0.24f;
            d.line(b, ax, y, Math.max(0.05f, Vector2.dst(ax, y, cx, cy) - 0.08f), 0.06f,
                    MathUtils.atan2(cy - y, cx - ax) * MathUtils.radDeg, c(SHELL_DARK));
            d.circle(b, cx, cy, 0.12f, Draw.OUTLINE_COLOR);
            d.circle(b, cx, cy, 0.105f, c(glow));
            float open = raised ? 0.06f : 0.025f;
            d.rect(b, cx - 0.02f, cy + 0.04f, 0.04f, open + 0.04f, c(glow));
            d.rect(b, cx - 0.005f, cy + 0.03f, 0.01f, open, Draw.OUTLINE_COLOR); // the pincer's gap
        }
    }
}
