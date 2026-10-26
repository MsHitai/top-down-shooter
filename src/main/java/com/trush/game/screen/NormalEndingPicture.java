package com.trush.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.trush.game.enemy.AlleyDog;
import com.trush.game.enemy.Boss;
import com.trush.game.enemy.Enemy;
import com.trush.game.enemy.Marked;
import com.trush.game.enemy.Rat;
import com.trush.game.enemy.SewerGator;
import com.trush.game.enemy.SkunkKing;
import com.trush.game.enemy.StinkPigeon;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.player.Hero;

/**
 * The normal ending's picture (beating the Skunk King, the False King, at the end of the secret detour), in
 * the same picture-book style as the true ending's {@link EndingPicture}. Below the street: the Undercity at
 * dawn, daylight falling through the open manhole, the critters in it - and as you watch, the green fades
 * from their ears and eyes: they're just critters again. The False King sits slumped by his fallen
 * cardboard crown. Above the street, a hint that there's more: up in the clouds the real Fluffy King still
 * sulks on his throne, a little storm cloud over his head.
 */
class NormalEndingPicture {

    private static final Color FRAME = Color.valueOf("2a1e24");
    private static final Color SKY = Color.valueOf("9ab8e0");
    private static final Color SKY_LOW = Color.valueOf("f2c8b8");
    private static final Color SKYLINE = Color.valueOf("6a7088");
    private static final Color ROAD = Color.valueOf("4a4a52");
    private static final Color KERB = Color.valueOf("8a8a94");
    private static final Color ROCK = Color.valueOf("1e1c1a");
    private static final Color BRICK = Color.valueOf("3a2e28");
    private static final Color BRICK_LINE = Color.valueOf("2a201c");
    private static final Color FLOOR = Color.valueOf("2e3432");
    private static final Color LIGHT = Color.valueOf("fff2c0");
    private static final Color WATER = Color.valueOf("4a8a6a");
    private static final Color CLOUD = Color.WHITE;
    private static final Color STORM = Color.valueOf("6a6a7a");
    private static final Color RAIN = Color.valueOf("8ab8e0");
    private static final Color CROWN = Color.valueOf("d9a84a");
    private static final Color SIGH = Color.valueOf("b8b8c0");
    /** How long the green takes to fade from the critters. */
    private static final float FADE_AFTER = 1.2f;
    private static final float FADE_TIME = 2.5f;

    private final Color tmp = new Color();
    private final Matrix4 saved = new Matrix4();
    private final Matrix4 placed = new Matrix4();
    private final Enemy fluffyKing = Boss.FLUFFY_KING.create(0, 0, 0);
    private final SkunkKing skunk = new SkunkKing(0, 0, 0);
    private final Enemy rat = Rat.marked(0, 0);
    private final Enemy dog = new AlleyDog(0, 0);
    private final Enemy gator = new SewerGator(0, 0);
    private final Enemy pigeon = new StinkPigeon(0, 0);

    NormalEndingPicture() {
        skunk.showBeaten();
    }

    /** Draws the picture into the box (x, y, w, h) in screen units; {@code t} is the time since it appeared. */
    void draw(SpriteBatch b, Draw d, ETGFanfic game, float x, float y, float w, float h, float t) {
        d.rect(b, x - 0.06f, y - 0.06f, w + 0.12f, h + 0.12f, FRAME);
        float street = y + h * 0.58f; // the street: sky above, the Undercity below
        drawSky(b, d, x, street, w, y + h - street, t);
        drawUnder(b, d, game, x, y, w, street - y, t);
    }

    /** Above: dawn over the city, and up in the clouds the real King sulking under his own little storm. */
    private void drawSky(SpriteBatch b, Draw d, float x, float y, float w, float h, float t) {
        int bands = 8;
        for (int i = 0; i < bands; i++) {
            d.rect(b, x, y + h * i / bands, w, h / bands + 0.01f, tmp.set(SKY_LOW).lerp(SKY, i / (bands - 1f)));
        }
        for (int i = 0; i < 9; i++) { // the city's skyline, quiet
            float bw = 0.3f + (i * 37 % 5) * 0.06f;
            float bh = 0.25f + (i * 53 % 7) * 0.07f;
            d.rect(b, x + 0.1f + i * 0.42f, y, bw, bh, SKYLINE);
        }
        d.rect(b, x, y - 0.08f, w, 0.1f, ROAD);
        d.rect(b, x, y + 0.02f, w, 0.02f, KERB);
        // the King's cloud, top right, and the King on it, arms crossed, sulking
        float kx = x + w - 1.05f;
        float ky = y + h - 0.62f;
        for (int i = -2; i <= 2; i++) {
            d.circle(b, kx + i * 0.2f, ky - 0.18f + Math.abs(i) * -0.03f, 0.2f - Math.abs(i) * 0.02f, CLOUD);
        }
        portrait(b, d, fluffyKing, kx, ky + 0.05f, 0.28f, 270f);
        float bob = MathUtils.sin(t * 2f) * 0.02f;
        float sx = kx + 0.05f;
        float sy = ky + 0.42f + bob;
        for (int i = -1; i <= 1; i++) { // his little storm cloud
            d.circle(b, sx + i * 0.11f, sy, 0.1f, STORM);
        }
        for (int i = 0; i < 4; i++) { // raining on him
            float k = (t * 2f + i * 0.25f) % 1f;
            d.rect(b, sx - 0.15f + i * 0.1f, sy - 0.1f - k * 0.3f, 0.015f, 0.06f, RAIN, 1f - k);
        }
        cloud(b, d, x + 0.7f, y + h - 0.35f, 0.14f);
        cloud(b, d, x + 2.1f, y + h - 0.5f, 0.11f);
    }

    private void cloud(SpriteBatch b, Draw d, float x, float y, float s) {
        d.circle(b, x, y, s, CLOUD, 0.85f);
        d.circle(b, x + s * 0.9f, y - s * 0.15f, s * 0.75f, CLOUD, 0.85f);
        d.circle(b, x - s * 0.9f, y - s * 0.2f, s * 0.65f, CLOUD, 0.85f);
    }

    /** Below: the Undercity, daylight pouring down the manhole, the critters turning ordinary, the beaten skunk. */
    private void drawUnder(SpriteBatch b, Draw d, ETGFanfic game, float x, float y, float w, float h, float t) {
        d.rect(b, x, y, w, h, ROCK);
        d.rect(b, x, y + h * 0.25f, w, h * 0.65f, BRICK);
        for (float by = y + h * 0.25f; by < y + h * 0.9f; by += 0.12f) {
            d.rect(b, x, by, w, 0.012f, BRICK_LINE);
        }
        d.rect(b, x, y, w, h * 0.25f, FLOOR);
        d.ellipse(b, x + w * 0.75f, y + 0.12f, 0.35f, 0.06f, WATER, 0.7f); // a puddle, no longer glowing
        // the manhole up to the street is open: daylight falls in
        float mx = x + w * 0.38f;
        for (int i = 0; i < 6; i++) {
            float k = i / 5f;
            float bw = 0.4f + 0.9f * k;
            d.rect(b, mx - bw / 2f, y + h - (h - 0.05f) * (k + 0.17f), bw, (h - 0.05f) / 6f + 0.01f, LIGHT, 0.16f);
        }
        d.rect(b, mx - 0.22f, y + h - 0.06f, 0.44f, 0.06f, LIGHT);
        for (int i = 0; i < 6; i++) { // the ladder
            d.rect(b, mx + 0.25f, y + 0.1f + i * 0.2f, 0.18f, 0.025f, KERB);
        }
        d.rect(b, mx + 0.25f, y + 0.1f, 0.02f, h - 0.15f, KERB);
        d.rect(b, mx + 0.41f, y + 0.1f, 0.02f, h - 0.15f, KERB);
        // the critters in the light - the green fading from their ears and eyes as you watch
        Marked.fade = Interpolation.smooth.apply(MathUtils.clamp((t - FADE_AFTER) / FADE_TIME, 0f, 1f));
        portrait(b, d, rat, mx - 0.55f, y + 0.18f, 0.9f, 0f);
        portrait(b, d, dog, mx - 0.1f, y + 0.22f, 0.9f, 180f);
        portrait(b, d, gator, mx - 1.1f, y + 0.2f, 0.8f, 0f);
        portrait(b, d, pigeon, mx - 0.3f + MathUtils.sin(t * 1.5f) * 0.25f, y + 0.85f, 0.8f, 0f);
        Marked.fade = 0f;
        // the hero on the bottom of the ladder, waving
        TextureRegion hero = game.sprites.hero(Hero.current.sheet)[0][4 + (int) (t * 2f) % 2];
        float s = 0.85f;
        b.draw(hero, mx + 0.34f - Sprites.w(hero) * s / 2f, y + 0.1f, Sprites.w(hero) * s, Sprites.h(hero) * s);
        // the False King in the dark, slumped by his crown, sighing
        float kx = x + w - 0.75f;
        portrait(b, d, skunk, kx, y + 0.42f, 0.55f, 180f);
        d.rect(b, kx + 0.25f, y + 0.08f, 0.22f, 0.07f, CROWN);
        float k = (t * 0.6f) % 1f;
        d.circle(b, kx - 0.2f, y + 0.85f + k * 0.25f, 0.05f + 0.04f * k, SIGH, 0.6f * (1f - k));
    }

    /** Draws an enemy's body at (x, y), {@code scale} times its size. */
    private void portrait(SpriteBatch b, Draw d, Enemy e, float x, float y, float scale, float aim) {
        e.pos.set(0, 0);
        saved.set(b.getTransformMatrix());
        placed.set(saved).translate(x, y, 0).scale(scale, scale, 1);
        b.setTransformMatrix(placed);
        e.drawPortrait(b, d, aim);
        b.setTransformMatrix(saved);
    }
}
