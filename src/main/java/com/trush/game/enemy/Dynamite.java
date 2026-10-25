package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Puddle;
import com.trush.game.world.GameWorld;

/**
 * A stick of dynamite on little legs (every stage): it runs straight at you and lights its fuse when
 * it's close - roll away or through the blast. Shoot it first and it goes off wherever it is, which
 * hurts any enemies standing next to it.
 */
public class Dynamite extends Enemy {

    private static final Color STICK = Color.valueOf("d63a2f");
    private static final Color STICK_DARK = Color.valueOf("9e2a22");
    private static final Color LABEL = Color.valueOf("f0e0b0");
    private static final Color FUSE = Color.valueOf("5a4a3a");
    private static final Color SPARK = Color.valueOf("ffd23a");

    private static final float W = 0.22f;
    private static final float H = 0.44f;
    private static final float FUSE_TIME = 0.8f;
    /** Lights up when this close to the player. */
    private static final float LIGHT_RANGE = 0.85f;
    public static final float BLAST_RADIUS = 0.95f;

    private float time = MathUtils.random(10f);
    private float fuse = -1f;

    public Dynamite(float x, float y) {
        super(x, y, 9f);
        radius = 0.16f;
        speed = 1.45f;
        score = 25;
        vanishOnDeath = true;
    }

    @Override
    public float feetY() {
        return pos.y - H * 0.5f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        if (fuse >= 0) {
            fuse -= delta;
            if (MathUtils.randomBoolean(0.6f)) {
                world.particles().spawn(pos.x, feetY() + H + 0.08f, 0, 0.5f, 0.2f, 0.03f, SPARK, 1f);
            }
            if (fuse <= 0) {
                hit(hp + 1f, knock, 0f, world); // boom (see dropLoot)
                return;
            }
        } else if (distanceToPlayer(world) < LIGHT_RANGE) {
            fuse = FUSE_TIME;
            world.sfx().play("zap", 0.6f);
        }
        if (!canSeePlayer && flank(delta, world, speed)) {
            return;
        }
        // waddle straight at the player (slower once the fuse is lit, so there's time to get away)
        float s = (fuse >= 0 ? 0.55f : 1f) * speed;
        float a = aimAngle + MathUtils.sin(time * 7f) * 12f;
        pos.add(MathUtils.cosDeg(a) * s * delta, MathUtils.sinDeg(a) * s * delta);
        walkTime += delta;
    }

    /** However it dies - shot, or its fuse ran out - it blows up. */
    @Override
    public void dropLoot(GameWorld world) {
        world.enemyBlast(pos.x, pos.y, BLAST_RADIUS);
        // and the floor keeps burning for a while (roll through it, or walk round)
        world.spillPuddle(Puddle.Kind.FIRE, pos.x, feetY() + 0.05f, BLAST_RADIUS * 0.7f, GameWorld.FIRE_LIFE, false);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float left = pos.x - W / 2f;
        boolean blink = fuse >= 0 && MathUtils.sin(fuse * (40f - 25f * fuse / FUSE_TIME)) > 0;
        if (fuse >= 0) { // the blast it's about to make
            float k = 1f - fuse / FUSE_TIME;
            d.circle(b, pos.x, pos.y, BLAST_RADIUS, TELEGRAPH, 0.08f + 0.12f * k);
            d.ring(b, pos.x, pos.y, BLAST_RADIUS, TELEGRAPH, 0.4f + 0.4f * k);
        }
        drawFeet(b, d, pos.x, bottom, 0.06f);
        Color body = blink ? Color.WHITE : c(STICK);
        d.rect(b, left, bottom, W, H, body);
        d.rect(b, left, bottom, 0.04f, H, blink ? Color.WHITE : c(STICK_DARK));
        d.rect(b, left, bottom + H * 0.55f, W, 0.09f, c(LABEL));
        d.rect(b, left + 0.03f, bottom + H * 0.58f, W - 0.06f, 0.03f, c(STICK_DARK));
        drawEyes(b, d, pos.x, bottom + H * 0.32f, 0.05f, 0.035f);
        // the fuse, burning down once lit
        float fuseLen = fuse >= 0 ? 0.14f * fuse / FUSE_TIME + 0.02f : 0.14f;
        float wig = MathUtils.sin(time * 6f) * 10f;
        d.line(b, pos.x, bottom + H, fuseLen, 0.025f, 90f + wig, FUSE);
        if (fuse >= 0) {
            float tx = pos.x + MathUtils.cosDeg(90f + wig) * fuseLen;
            float ty = bottom + H + MathUtils.sinDeg(90f + wig) * fuseLen;
            d.circle(b, tx, ty, 0.045f + MathUtils.random(0.015f), SPARK);
            d.circle(b, tx, ty, 0.022f, Color.WHITE);
        }
    }
}
