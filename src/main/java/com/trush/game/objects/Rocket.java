package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.player.Hero;

/**
 * The family's cardboard rocket from the living room (screen/HubScreen), once the whole family has built
 * it: after the Fluffy King falls it comes roaring down onto the clouds beside the rainbow, flown by
 * grandad - or granny, when grandad is the one playing - who hops out and waits beside it. The way to
 * space (still being built: see ROADMAP).
 */
public class Rocket {

    /** How long it takes to come down from above the screen. */
    public static final float LAND_TIME = 2.2f;
    /** How high above its spot it starts. */
    private static final float DROP = 5f;
    /** The pilot hops out this long after touching down. */
    private static final float HOP_OUT = 0.5f;
    private static final String[] LAYERS = {"rocket_box", "rocket_cone", "rocket_fins", "rocket_scarf", "rocket_engine"};
    private static final Color FLAME = Color.valueOf("ffb43a");
    private static final Color FLAME_CORE = Color.valueOf("fff3c0");

    /** The middle of its foot, where it stands. */
    public final float x;
    public final float y;
    public final Hero pilot;
    private float time;

    public Rocket(float x, float y, Hero pilot) {
        this.x = x;
        this.y = y;
        this.pilot = pilot;
    }

    /** Who flies it: grandad, unless you're grandad - then granny. */
    public static Hero pilotFor(Hero playing) {
        return playing == Hero.GRANDAD ? Hero.GRANNY : Hero.GRANDAD;
    }

    /** @return true on the frame it touches down */
    public boolean update(float delta) {
        boolean wasFlying = !landed();
        time += delta;
        return wasFlying && landed();
    }

    public boolean landed() {
        return time >= LAND_TIME;
    }

    /** Can be climbed into: down, and the pilot has hopped out. */
    public boolean ready() {
        return time >= LAND_TIME + HOP_OUT;
    }

    /** The pilot's feet, beside the rocket. */
    public float pilotX() {
        return x + 0.78f;
    }

    public float sortY() {
        return y;
    }

    /** How far above its spot it still is (eases out as it comes down). */
    private float height() {
        float k = MathUtils.clamp(time / LAND_TIME, 0f, 1f);
        return DROP * (1f - k) * (1f - k);
    }

    public void draw(SpriteBatch b, ETGFanfic game) {
        Draw d = game.draw;
        float h = height();
        // the shadow grows as it comes down
        float shadow = MathUtils.clamp(1f - h / DROP, 0.2f, 1f);
        d.ellipse(b, x, y + 0.04f, 0.55f * shadow, 0.12f * shadow, Color.BLACK, 0.35f * shadow);

        TextureRegion box = game.sprites.home(LAYERS[0]);
        float w = Sprites.w(box);
        float left = x - w / 2f;
        float bottom = y - 2f / Sprites.PPU + h;
        if (!landed()) { // flames out of grandad's vacuum cleaner, braking
            float nx = left + 20f / Sprites.PPU;
            float flicker = 0.8f + 0.2f * MathUtils.sin(time * 40f);
            d.circle(b, nx, bottom - 0.12f, 0.16f * flicker, FLAME, 0.85f);
            d.circle(b, nx, bottom - 0.06f, 0.09f * flicker, FLAME_CORE, 0.95f);
        } else if (time % 4f < 0.5f) { // a rattle now and then, as at home
            left += (((int) (time * 30f)) % 2 == 0 ? 1 : -1) / Sprites.PPU;
        }
        for (String layer : LAYERS) {
            TextureRegion r = game.sprites.home(layer);
            b.draw(r, left, bottom, Sprites.w(r), Sprites.h(r));
        }
        if (landed()) { // a little smoke out of the nozzle
            for (int i = 0; i < 3; i++) {
                float t = (time * 0.6f + i / 3f) % 1f;
                d.circle(b, left + 20f / Sprites.PPU + 0.25f * t, y + 0.06f + 0.2f * t, 0.04f + 0.06f * t,
                        Color.LIGHT_GRAY, 0.45f * (1f - t));
            }
        }
        if (ready()) { // the pilot, standing beside it
            TextureRegion[][] frames = game.sprites.hero(pilot.sheet);
            TextureRegion r = frames[0][4 + ((int) (time * 2f)) % 2];
            float pw = Sprites.w(r);
            float px = pilotX();
            d.ellipse(b, px, y + 0.03f, 0.2f, 0.06f, Color.BLACK, 0.35f);
            b.draw(r.getTexture(), px - pw / 2f, y, pw / 2f, 0, pw, Sprites.h(r), 1f, 1f, 0f,
                    r.getRegionX(), r.getRegionY(), r.getRegionWidth(), r.getRegionHeight(), true, false);
        }
    }
}
