package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;

/**
 * The Toy Robot (after ETG's Portable Turret): a little wind-up robot set down on the floor that turns
 * to the nearest enemy and shoots at it until it runs down. GameWorld picks targets and fires for it.
 */
public class Turret {

    private static final Color BODY = Color.valueOf("d8dce6");
    private static final Color DARK = Color.valueOf("8a90a0");
    private static final Color EYE = Color.valueOf("ff5a4a");
    private static final Color KEY = Color.valueOf("e8c040");

    /** How long it keeps going once wound up. */
    public static final float LIFE = 8f;
    public static final float FIRE_INTERVAL = 0.3f;
    public static final float RANGE = 5f;

    public final float x;
    public final float y;
    public float life = LIFE;
    public float cooldown = 0.3f;
    /** Which way its gun points (it turns toward its target). */
    public float angle;
    private float clock;

    public Turret(float x, float y, float angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    public void update(float delta) {
        life -= delta;
        cooldown -= delta;
        clock += delta;
    }

    /** Where its bullets come out: the end of the little gun on its head. */
    public float muzzleX() {
        return x + MathUtils.cosDeg(angle) * 0.18f;
    }

    public float muzzleY() {
        return y + 0.26f + MathUtils.sinDeg(angle) * 0.18f;
    }

    public void draw(SpriteBatch b, Draw d) {
        boolean winding = life > 1.2f || ((int) (clock * 8f)) % 2 == 0; // blinks as it runs down
        d.circle(b, x, y, 0.14f, Color.BLACK, 0.25f); // shadow
        d.rect(b, x - 0.12f, y, 0.24f, 0.2f, Draw.OUTLINE_COLOR);
        d.rect(b, x - 0.1f, y + 0.02f, 0.2f, 0.16f, BODY);
        d.rect(b, x - 0.1f, y + 0.02f, 0.2f, 0.04f, DARK);
        float spin = clock * 360f; // the wind-up key turning on its back
        d.rotRect(b, x, y + 0.12f, 0.16f * Math.abs(MathUtils.cosDeg(spin)), 0.04f, 0f, KEY, 1f);
        d.rotRect(b, muzzleX() - MathUtils.cosDeg(angle) * 0.08f, y + 0.26f + MathUtils.sinDeg(angle) * 0.1f,
                0.18f, 0.05f, angle, DARK, 1f);
        d.circle(b, x, y + 0.26f, 0.09f, Draw.OUTLINE_COLOR);
        d.circle(b, x, y + 0.26f, 0.075f, BODY);
        d.circle(b, x + MathUtils.cosDeg(angle) * 0.03f, y + 0.27f, 0.025f, winding ? EYE : DARK);
    }
}
