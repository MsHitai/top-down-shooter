package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;

/**
 * A puff of something nasty hanging in the air: the Evil Train's smoke, the Skunk King's stink (planned).
 * Unlike a {@link Puddle} it's up in the air and drifts; standing in it fills the same drop warning as acid
 * and then hurts (Player.updateHazards, not stopped by the Acid Boots - they're for your feet). A dodge roll
 * goes through unharmed. Enemies' gas never hurts enemies. GameWorld.updateGas moves them.
 */
public class GasCloud {

    public enum Kind {
        SMOKE(Color.valueOf("5a5a66"), Color.valueOf("9a9aa8")),
        STINK(Color.valueOf("6a8a1e"), Color.valueOf("c8e85a"));

        final Color dark;
        final Color light;

        Kind(Color dark, Color light) {
            this.dark = dark;
            this.light = light;
        }
    }

    /** It swells up this fast when it comes out, and fades over its last FADE seconds. */
    private static final float GROW = 4f;
    private static final float FADE = 0.8f;
    /** Its speed falls off like this per second (puffs slow down and hang there). */
    private static final float DRAG = 1.4f;
    /** A blank blows clouds away: they fade out over this. */
    private static final float BLOWN_LIFE = 0.35f;

    public final Kind kind;
    public float x;
    public float y;
    public float vx;
    public float vy;
    private final float radius;
    public float life;
    private float age;
    private final float phase = MathUtils.random(MathUtils.PI2);

    public GasCloud(Kind kind, float x, float y, float vx, float vy, float radius, float life) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.life = life;
    }

    public void update(float delta) {
        life -= delta;
        age += delta;
        x += vx * delta;
        y += vy * delta;
        float slow = Math.max(0f, 1f - DRAG * delta);
        vx *= slow;
        vy *= slow;
    }

    public boolean alive() {
        return life > 0;
    }

    /** How big it is now: it puffs out quickly, then thins away. */
    public float radius() {
        return radius * Math.min(1f, 0.35f + age * GROW) * MathUtils.clamp(life / FADE, 0.3f, 1f);
    }

    /** Thick enough to hurt: not while it's still coming out or nearly gone. */
    public boolean harmful() {
        return age > 0.08f && life > FADE * 0.4f;
    }

    /** True if a circle (the hero's hitbox) touches the thick middle of the cloud. */
    public boolean touches(float px, float py, float pr) {
        float r = radius() * 0.8f + pr;
        float dx = px - x;
        float dy = py - y;
        return harmful() && dx * dx + dy * dy < r * r;
    }

    /** A blank: off it goes, away from (cx, cy), and it's gone in a moment. */
    public void blowAway(float cx, float cy) {
        float dx = x - cx;
        float dy = y - cy;
        float len = Math.max(0.01f, (float) Math.sqrt(dx * dx + dy * dy));
        vx = dx / len * 3f;
        vy = dy / len * 3f;
        life = Math.min(life, BLOWN_LIFE);
    }

    public void draw(SpriteBatch b, Draw d) {
        float r = radius();
        if (r <= 0.01f) {
            return;
        }
        float a = MathUtils.clamp(life / FADE, 0f, 1f);
        // a soft shadowed body and three lumps rolling round it, so it reads as a cloud and not a bullet
        d.circle(b, x, y - r * 0.08f, r, kind.dark, 0.45f * a);
        for (int i = 0; i < 3; i++) {
            float t = phase + age * 1.3f + i * MathUtils.PI2 / 3f;
            float lx = x + MathUtils.cos(t) * r * 0.38f;
            float ly = y + MathUtils.sin(t) * r * 0.3f + r * 0.08f;
            d.circle(b, lx, ly, r * 0.55f, kind.light, 0.35f * a);
        }
        d.circle(b, x - r * 0.2f, y + r * 0.3f, r * 0.22f, Color.WHITE, 0.12f * a);
    }
}
