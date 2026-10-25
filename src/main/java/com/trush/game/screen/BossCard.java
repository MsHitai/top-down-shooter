package com.trush.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.trush.game.enemy.Boss;
import com.trush.game.enemy.Enemy;
import com.trush.game.gfx.Draw;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.world.Arena;

/**
 * The boss introduction, like ETG's: the moment a boss has appeared the world holds still, the camera
 * glides over to it (GameScreen, for {@link #LEAD}), then a slanted band sweeps across the screen with a big close-up of the boss, its name in large letters and
 * its line typed out underneath. Any key, click or A skips it. Drawn in HUD space (7 x 5).
 */
class BossCard {

    static final float TIME = 3.2f;
    /** First the camera glides over to the boss and lets you see it for a moment. */
    static final float LEAD = 1.0f;
    private static final float IN = 0.35f;
    private static final float OUT = 0.3f;
    /** It can't be skipped straight away (a held trigger would skip it unseen). */
    private static final float SKIPPABLE = 0.5f;
    private static final float TYPE_SPEED = 30f;

    private static final Color DIM = Color.BLACK;
    private static final Color BAND = Color.valueOf("1c1020");
    private static final Color EDGE = Color.valueOf("e84393");
    private static final Color GLOW = Color.valueOf("ff9ed8");
    private static final Color NAME = Color.valueOf("ffd166");
    private static final Color SMALL = Color.valueOf("f5f0e6");
    private static final Color LINE = Color.valueOf("f5f0e6");

    private Enemy shownFor;
    private Boss boss;
    private Enemy portrait;
    private float t = -1f;
    /** The camera's pan over to the boss before the card slides in (the world already holds still). */
    private float lead;
    private final Color tmp = new Color();
    private final Matrix4 saved = new Matrix4();
    private final Matrix4 placed = new Matrix4();

    boolean isShowing() {
        return t >= 0f || lead > 0f;
    }

    /** Starts the card the first time this boss is seen (each new fight with it gets one). */
    void maybeStart(Enemy active) {
        if (active == null || active == shownFor) {
            return;
        }
        shownFor = active;
        boss = Boss.forTitle(active.bossName());
        if (boss == null) {
            return;
        }
        // a fresh one just for the picture, so drawing it never moves the real boss
        portrait = boss.create(0, 0, 0);
        lead = LEAD;
    }

    void update(float delta, boolean skip) {
        if (lead > 0f) {
            lead -= delta;
            if (lead <= 0f) {
                t = 0f;
            }
            return;
        }
        if (t < 0f) {
            return;
        }
        t += delta;
        if (skip && t > SKIPPABLE && t < TIME - OUT) {
            t = TIME - OUT; // slide out now
        }
        if (t >= TIME) {
            t = -1f;
        }
    }

    void draw(SpriteBatch b, Draw d, ETGFanfic game, float clock) {
        if (t < 0f) {
            return;
        }
        float in = Interpolation.pow3Out.apply(Math.min(1f, t / IN));
        float out = t > TIME - OUT ? Interpolation.pow2In.apply((t - (TIME - OUT)) / OUT) : 0f;
        float alpha = in * (1f - out);
        float slide = (1f - in) * 9f - out * 9f; // in from the right, out to the left
        d.rect(b, 0, 0, Arena.VIEW_W, Arena.VIEW_H, DIM, 0.6f * alpha);

        // the band across the middle, tilted like ETG's
        float cy = 2.65f;
        d.rotRect(b, 3.5f + slide, cy, 10f, 2.05f, 6f, EDGE, alpha);
        d.rotRect(b, 3.5f + slide, cy, 10f, 1.85f, 6f, BAND, alpha);
        for (int i = 0; i < 5; i++) { // speed streaks racing along it
            float sx = ((clock * 6f + i * 2.3f) % 11f) - 2f + slide;
            d.rotRect(b, sx, cy - 0.6f + i * 0.3f, 0.9f, 0.025f, 6f, GLOW, 0.25f * alpha);
        }

        // the close-up, bursting out of the band on the left (comes in a beat behind it)
        float pin = Interpolation.pow3Out.apply(MathUtils.clamp((t - 0.1f) / IN, 0f, 1f));
        float px = 1.65f - (1f - pin) * 4f - out * 9f;
        float py = cy - 0.05f + MathUtils.sin(clock * 2f) * 0.03f;
        d.circle(b, px, py, 1.05f, GLOW, 0.18f * alpha);
        float scale = MathUtils.clamp(0.75f / portrait.radius, 1.2f, 2.3f);
        portrait.pos.set(0, 0);
        saved.set(b.getTransformMatrix());
        placed.set(saved).translate(px, py, 0).scale(scale, scale, 1);
        b.setTransformMatrix(placed);
        portrait.drawPortrait(b, d, 270f);
        b.setTransformMatrix(saved);

        // the name: a small "THE" over the rest in big letters, fitted to the space right of the picture
        float nin = Interpolation.pow3Out.apply(MathUtils.clamp((t - 0.2f) / IN, 0f, 1f));
        float nx = 4.6f + (1f - nin) * 5f - out * 9f;
        String title = boss.title;
        String small = null;
        if (title.startsWith("THE ")) {
            small = "THE";
            title = title.substring(4);
        }
        float size = Math.min(0.85f, 3.9f / Math.max(0.1f, game.textWidth(title, 1f)));
        if (small != null) {
            game.text(small, nx, cy + 0.55f, 0.32f, tmp.set(SMALL).mul(1, 1, 1, alpha), Align.center);
        }
        game.text(title, nx + 0.03f, cy + 0.27f - 0.03f, size, tmp.set(Color.BLACK).mul(1, 1, 1, alpha), Align.center);
        game.text(title, nx, cy + 0.27f, size, tmp.set(NAME).mul(1, 1, 1, alpha), Align.center);

        // its line, typed out under the band
        String quote = "\"" + boss.quote() + "\"";
        int shown = MathUtils.clamp((int) ((t - 0.45f) * TYPE_SPEED), 0, quote.length());
        if (shown > 0) {
            float w = game.textWidth(quote, 0.32f) + 0.4f;
            d.rect(b, 3.5f - w / 2f, 0.9f, w, 0.32f, BAND, 0.85f * alpha);
            game.text(quote.substring(0, shown), 3.5f, 1.15f, 0.32f, tmp.set(LINE).mul(1, 1, 1, alpha), Align.center);
        }
    }
}
