package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;

/**
 * A sea mine (the secret sea): a spiky ball floating on its chain over the sea bed. Not an enemy - it
 * stays after the room is cleared - but a trap: anyone who comes close (the player, or a walking enemy)
 * lights it, it blinks for a moment and blows up, hurting whatever is in reach. Shooting it sets it off
 * at once, so it can be used on enemies. GameWorld updates and draws them ({@code Room.mines}).
 */
public class SeaMine {

    /** It lights up when the player's feet come this close. */
    public static final float TRIGGER = 0.6f;
    /** And when a walking enemy comes this close. */
    public static final float ENEMY_TRIGGER = 0.35f;
    public static final float FUSE = 0.75f;
    public static final float BLAST = 0.9f;
    /** Bullets hit it within this far of its middle. */
    public static final float HIT_RADIUS = 0.16f;

    private static final Color BALL = Color.valueOf("3a4250");
    private static final Color BALL_HI = Color.valueOf("5a6678");
    private static final Color SPIKE = Color.valueOf("8a94a4");
    private static final Color CHAIN = Color.valueOf("4a4a52");
    private static final Color LIGHT = Color.valueOf("ff3b3b");

    /** Where it floats (the ball's middle) and the anchor below it. */
    public final float x;
    public final float y;
    public boolean gone;
    /** Counting down once lit; below 0 while it waits. */
    public float fuse = -1f;
    private float time = MathUtils.random(10f);

    public SeaMine(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public boolean isLit() {
        return fuse >= 0f;
    }

    public void light(float after) {
        if (fuse < 0f || after < fuse) {
            fuse = after;
        }
    }

    /** True on the frame it goes off. */
    public boolean update(float delta) {
        time += delta;
        if (fuse < 0f || gone) {
            return false;
        }
        fuse -= delta;
        if (fuse <= 0f) {
            gone = true;
            return true;
        }
        return false;
    }

    public float ballY() {
        return y + 0.32f + MathUtils.sin(time * 1.6f) * 0.03f;
    }

    public void draw(SpriteBatch b, Draw d) {
        float by = ballY();
        d.ellipse(b, x, y, 0.12f, 0.04f, Color.BLACK, 0.3f);
        for (float cy = y; cy < by - 0.12f; cy += 0.05f) { // the chain down to its anchor
            d.rect(b, x - 0.01f, cy, 0.02f, 0.03f, CHAIN);
        }
        if (isLit()) { // the blast it's about to make
            float k = 1f - fuse / FUSE;
            d.circle(b, x, by, BLAST, LIGHT, 0.06f + 0.1f * k);
            d.ring(b, x, by, BLAST, LIGHT, 0.35f + 0.4f * k);
        }
        for (int i = 0; i < 8; i++) {
            float a = i * 45f + time * 10f;
            d.line(b, x, by, 0.17f, 0.035f, a, Draw.OUTLINE_COLOR);
            d.line(b, x, by, 0.16f, 0.02f, a, SPIKE);
        }
        d.circle(b, x, by, 0.125f, Draw.OUTLINE_COLOR);
        d.circle(b, x, by, 0.11f, BALL);
        d.circle(b, x - 0.035f, by + 0.035f, 0.035f, BALL_HI);
        boolean blink = isLit() ? ((int) (fuse * 14f)) % 2 == 0 : MathUtils.sin(time * 2f) > 0.9f;
        d.circle(b, x, by + 0.07f, 0.03f, blink ? LIGHT : BALL_HI);
    }
}
