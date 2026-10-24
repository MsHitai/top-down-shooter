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
 * Clouds enemy: a grumpy little thundercloud. It drifts after you, then rumbles and crackles while a
 * ring on the floor marks where you stood; when the ring fills, lightning strikes it and sparks fly out
 * in a ring. Keep moving and it never touches you.
 */
public class Thundercloud extends Enemy {

    private static final Color CLOUD = Color.valueOf("8a90a8");
    private static final Color CLOUD_HI = Color.valueOf("b8bed2");
    private static final Color CLOUD_DARK = Color.valueOf("5a6078");
    private static final Color BOLT = Color.valueOf("fff06a");
    private static final Color MARK = Color.valueOf("ffe066");

    private static final float CHARGE = 1.0f;
    private static final float STRIKE_RADIUS = 0.42f;
    /** How long the lightning stays on screen. */
    private static final float FLASH = 0.16f;

    private float clock = MathUtils.random(10f);
    private float fireTimer = MathUtils.random(1.6f, 2.6f);
    private float charge;
    private float flash;
    private final Vector2 strikeAt = new Vector2();
    private final Vector2 drift = new Vector2();
    private final float[] zig = new float[6];

    public Thundercloud(float x, float y) {
        super(x, y, 30f);
        radius = 0.26f;
        speed = 0.7f;
        score = 40;
        flying = true;
    }

    /** It floats high: the shadow is well below it. */
    @Override
    public float feetY() {
        return pos.y - 0.55f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        clock += delta;
        flash = Math.max(0f, flash - delta);
        float diff = world.getDifficulty();
        Player p = world.getPlayer();
        // drifts toward a spot a little above and beside the player
        drift.set(p.centerX() + MathUtils.sin(clock * 0.7f) * 1.4f, p.hitboxY() + 1.3f).sub(pos);
        float dist = drift.len();
        float pace = charge > 0 ? speed * 0.3f : speed;
        if (dist > 0.05f) {
            pos.mulAdd(drift.scl(1f / dist), Math.min(dist, pace * delta));
        }
        if (charge > 0) {
            charge -= delta;
            if (MathUtils.random() < delta * 12f) { // crackling
                world.particles().burst(pos.x + MathUtils.random(-0.25f, 0.25f), pos.y - 0.1f, 1, 0.1f, 0.6f, 0.2f, 0.025f, BOLT);
            }
            if (charge <= 0) {
                strike(world, diff);
            }
            return;
        }
        fireTimer -= delta;
        if (fireTimer <= 0 && world.onScreen(pos.x, pos.y, 0.3f)) { // lightning comes from above: no cover stops it
            charge = CHARGE / Math.min(1.2f, diff);
            strikeAt.set(p.centerX(), p.hitboxY());
            Arena.now.clamp(strikeAt, 0.1f);
            world.sfx().play("buzz", 0.4f);
        }
    }

    private void strike(GameWorld world, float diff) {
        fireTimer = MathUtils.random(2.2f, 3.0f) / diff;
        flash = FLASH;
        for (int i = 0; i < zig.length; i++) {
            zig[i] = MathUtils.random(-0.12f, 0.12f);
        }
        Player p = world.getPlayer();
        if (Vector2.dst(strikeAt.x, strikeAt.y, p.centerX(), p.hitboxY()) < STRIKE_RADIUS + Player.HITBOX_RADIUS) {
            world.hurtPlayer();
        }
        int n = diff > 1.3f ? 10 : 8;
        float start = MathUtils.random(360f);
        for (int i = 0; i < n; i++) {
            float a = start + 360f * i / n;
            Bullet b = world.spawnEnemyBullet(strikeAt.x + MathUtils.cosDeg(a) * 0.15f, strikeAt.y + MathUtils.sinDeg(a) * 0.15f,
                    a, 1.9f * diff);
            b.shape = Bullet.Shape.STAR;
            b.color.set(BOLT);
        }
        world.particles().burst(strikeAt.x, strikeAt.y, 14, 0.3f, 2f, 0.35f, 0.04f, BOLT);
        world.shake(0.2f);
        world.sfx().play("zap", 0.8f);
    }

    /** The ring where the lightning will strike, filling up as it charges. */
    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (charge <= 0 || !alive) {
            return;
        }
        float k = MathUtils.clamp(1f - charge / CHARGE, 0f, 1f);
        d.ring(b, strikeAt.x, strikeAt.y, STRIKE_RADIUS, MARK, 0.85f);
        d.circle(b, strikeAt.x, strikeAt.y, STRIKE_RADIUS * k, MARK, 0.3f);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bob = MathUtils.sin(clock * 2.5f) * 0.03f;
        float y = pos.y + bob;
        boolean charging = charge > 0;
        float shiver = charging ? MathUtils.random(-0.012f, 0.012f) : 0f;
        float x = pos.x + shiver;
        Color body = charging && ((int) (clock * 18f)) % 3 == 0 ? CLOUD_HI : CLOUD;
        float[][] puffs = {{-0.17f, -0.03f, 0.13f}, {0f, 0.05f, 0.17f}, {0.17f, -0.02f, 0.13f}, {0.07f, -0.08f, 0.12f},
                {-0.07f, -0.08f, 0.12f}};
        for (float[] pf : puffs) {
            d.circle(b, x + pf[0], y + pf[1], pf[2] + 0.02f, Draw.OUTLINE_COLOR);
        }
        for (float[] pf : puffs) {
            d.circle(b, x + pf[0], y + pf[1] - 0.015f, pf[2], c(CLOUD_DARK));
        }
        for (float[] pf : puffs) {
            d.circle(b, x + pf[0] - 0.01f, y + pf[1] + 0.01f, pf[2] - 0.025f, c(body));
        }
        // a cross face: narrowed eyes and a frown
        drawEyes(b, d, x, y + 0.01f, 0.06f, 0.035f);
        d.rotRect(b, x - 0.07f, y + 0.07f, 0.08f, 0.022f, -20f, Draw.OUTLINE_COLOR, 1f);
        d.rotRect(b, x + 0.07f, y + 0.07f, 0.08f, 0.022f, 20f, Draw.OUTLINE_COLOR, 1f);
        d.rect(b, x - 0.04f, y - 0.07f, 0.08f, 0.02f, Draw.OUTLINE_COLOR);
        if (flash > 0) { // the bolt: a jagged line from the cloud down to the ring
            float a = flash / FLASH;
            float x0 = x;
            float y0 = y - 0.12f;
            for (int i = 0; i < zig.length; i++) {
                float t = (i + 1f) / zig.length;
                float x1 = i == zig.length - 1 ? strikeAt.x : MathUtils.lerp(x, strikeAt.x, t) + zig[i];
                float y1 = MathUtils.lerp(y - 0.12f, strikeAt.y, t);
                float len = Vector2.dst(x0, y0, x1, y1);
                float ang = MathUtils.atan2Deg360(y1 - y0, x1 - x0);
                d.line(b, x0, y0, len, 0.09f, ang, BOLT, 0.5f * a);
                d.line(b, x0, y0, len, 0.035f, ang, Color.WHITE, a);
                x0 = x1;
                y0 = y1;
            }
        }
    }
}
