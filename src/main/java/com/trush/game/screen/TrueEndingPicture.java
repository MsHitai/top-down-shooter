package com.trush.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.trush.game.enemy.Boss;
import com.trush.game.enemy.Enemy;
import com.trush.game.enemy.FluffyKing;
import com.trush.game.enemy.Grandfluff;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Rocket;
import com.trush.game.player.Hero;

/**
 * The true ending's picture (beating Grandfluff on the Moon), in the same picture-book style as
 * {@link EndingPicture}: the two kings making up (the ROADMAP's ending) - the Fluffy King and his grandfather
 * shuffle up to each other on the moon dust and hearts float up between them, while the family's grandad
 * (granny, when you're playing as grandad) stands by nodding. The cardboard rocket is parked behind, the hero
 * waves, and the Earth hangs in the starry sky with the Fluffy Kingdom a pink smudge on it.
 */
class TrueEndingPicture {

    private static final Color FRAME = Color.valueOf("2a1e24");
    private static final Color SPACE = Color.valueOf("070818");
    private static final Color SPACE_LOW = Color.valueOf("1c1838");
    private static final Color EARTH_SEA = Color.valueOf("3a7ad8");
    private static final Color EARTH_LAND = Color.valueOf("5ab85a");
    private static final Color KINGDOM_PINK = Color.valueOf("ffb4dc");
    private static final Color DUST = Color.valueOf("b4b4be");
    private static final Color DUST_DARK = Color.valueOf("8a8a96");
    private static final Color CRATER = Color.valueOf("74747f");
    private static final Color CASTLE = Color.valueOf("d8d4e8");
    private static final Color CASTLE_ROOF = Color.valueOf("8a6ad8");
    private static final Color HEART = Color.valueOf("ff6fa8");
    private static final Color FLAG = Color.valueOf("ff9ed8");
    private static final String[] ROCKET = {"rocket_box", "rocket_cone", "rocket_fins", "rocket_scarf", "rocket_engine"};
    /** The kings take this long to shuffle up to each other. */
    private static final float MEET_TIME = 1.8f;

    private final Color tmp = new Color();
    private final Matrix4 saved = new Matrix4();
    private final Matrix4 placed = new Matrix4();
    private final FluffyKing king = new FluffyKing(0, 0, 0);
    private final Grandfluff grandfluff = new Grandfluff(0, 0, 0);

    TrueEndingPicture() {
        king.showHappy();
        grandfluff.showSmile();
    }

    /** Draws the picture into the box (x, y, w, h) in screen units; {@code t} is the time since it appeared. */
    void draw(SpriteBatch b, Draw d, ETGFanfic game, float x, float y, float w, float h, float t) {
        d.rect(b, x - 0.06f, y - 0.06f, w + 0.12f, h + 0.12f, FRAME);
        float ground = y + h * 0.38f;
        drawSky(b, d, x, ground, w, y + h - ground, t);
        drawGround(b, d, x, y, w, ground - y);
        // the rocket parked on the left, with the family's flag beside it
        float rx = x + 0.6f;
        float ry = y + 0.32f;
        TextureRegion box = game.sprites.home(ROCKET[0]);
        float left = rx - Sprites.w(box) / 2f;
        for (String layer : ROCKET) {
            TextureRegion r = game.sprites.home(layer);
            b.draw(r, left, ry, Sprites.w(r), Sprites.h(r));
        }
        d.rect(b, rx + 0.5f, ry, 0.02f, 0.55f, DUST_DARK);
        float wave = MathUtils.sin(t * 3f) * 0.02f;
        d.rect(b, rx + 0.52f, ry + 0.38f + wave, 0.22f, 0.14f, FLAG);
        // the two kings, shuffling up to each other, then bobbing happily side by side
        float meet = Interpolation.smooth.apply(MathUtils.clamp(t / MEET_TIME, 0f, 1f));
        float mid = x + w * 0.5f;
        float gap = 0.95f - 0.42f * meet;
        float bob = meet >= 1f ? MathUtils.sin(t * 4f) * 0.025f : 0f;
        d.ellipse(b, mid - gap / 2f, y + 0.36f, 0.2f, 0.05f, Color.BLACK, 0.3f);
        d.ellipse(b, mid + gap / 2f, y + 0.36f, 0.2f, 0.05f, Color.BLACK, 0.3f);
        portrait(b, d, grandfluff, mid - gap / 2f, y + 0.62f + bob, 0.33f, 0f);
        portrait(b, d, king, mid + gap / 2f, y + 0.62f - bob, 0.33f, 180f);
        if (meet >= 1f) { // made up: hearts float up from between them
            for (int i = 0; i < 4; i++) {
                float k = (t * 0.5f + i / 4f) % 1f;
                float hx = mid + MathUtils.sin(t * 2f + i * 1.7f) * 0.12f;
                float hy = y + 1.0f + k * 0.8f;
                heart(b, d, hx, hy, 0.05f + 0.03f * k, 1f - k);
            }
        }
        // grandad (granny, if grandad is the hero) standing by, nodding along
        Hero elder = Rocket.pilotFor(Hero.current);
        TextureRegion er = game.sprites.hero(elder.sheet)[0][4];
        float nod = MathUtils.sin(t * 5f) > 0.6f ? 0.97f : 1f;
        float ex = mid + 1.05f;
        float ew = Sprites.w(er) * 0.95f;
        float eh = Sprites.h(er) * 0.95f;
        d.ellipse(b, ex, y + 0.24f, 0.17f, 0.05f, Color.BLACK, 0.3f);
        b.draw(er.getTexture(), ex - ew / 2f, y + 0.22f, ew / 2f, 0f, ew, eh * nod, 1f, 1f, 0f,
                er.getRegionX(), er.getRegionY(), er.getRegionWidth(), er.getRegionHeight(), true, false);
        // the hero on the right, waving
        TextureRegion hero = game.sprites.hero(Hero.current.sheet)[0][4 + (int) (t * 2f) % 2];
        float hx = x + w - 0.6f;
        d.ellipse(b, hx, y + 0.24f, 0.17f, 0.05f, Color.BLACK, 0.3f);
        b.draw(hero, hx - Sprites.w(hero) * 0.95f / 2f, y + 0.22f, Sprites.w(hero) * 0.95f, Sprites.h(hero) * 0.95f);
    }

    /** The starry sky, the Earth far off with home on it, and Grandfluff's castle on the horizon. */
    private void drawSky(SpriteBatch b, Draw d, float x, float y, float w, float h, float t) {
        int bands = 8;
        for (int i = 0; i < bands; i++) {
            d.rect(b, x, y + h * i / bands, w, h / bands + 0.01f, tmp.set(SPACE_LOW).lerp(SPACE, i / (bands - 1f)));
        }
        for (int i = 0; i < 40; i++) { // stars, twinkling (positions from i, so no state to keep)
            float sx = x + (i * 0.617f % 1f) * w;
            float sy = y + 0.1f + (i * 0.383f % 1f) * (h - 0.15f);
            float tw = 0.5f + 0.5f * MathUtils.sin(t * (1f + i % 5 * 0.5f) + i);
            d.rect(b, sx, sy, i % 7 == 0 ? 0.03f : 0.018f, i % 7 == 0 ? 0.03f : 0.018f, Color.WHITE, tw);
        }
        float ex = x + w - 0.85f;
        float ey = y + h - 0.55f;
        d.circle(b, ex, ey, 0.36f, EARTH_SEA);
        d.circle(b, ex - 0.1f, ey + 0.07f, 0.14f, EARTH_LAND);
        d.circle(b, ex + 0.12f, ey - 0.1f, 0.1f, EARTH_LAND);
        d.circle(b, ex + 0.07f, ey + 0.15f, 0.07f, KINGDOM_PINK, 0.9f);
        d.circle(b, ex + 0.14f, ey + 0.17f, 0.05f, KINGDOM_PINK, 0.8f);
        d.circle(b, ex + 0.1f, ey + 0.1f, 0.36f, SPACE, 0.3f);
        // Grandfluff's castle, small on the horizon
        float cx = x + w * 0.3f;
        d.rect(b, cx - 0.3f, y, 0.6f, 0.28f, CASTLE);
        for (int i = -1; i <= 1; i++) {
            float tx = cx + i * 0.3f;
            float th = i == 0 ? 0.48f : 0.38f;
            d.rect(b, tx - 0.07f, y, 0.14f, th, CASTLE);
            d.shape(b, d.triangle, tx, y + th + 0.05f, 0.1f, 90f, CASTLE_ROOF, 1f);
        }
    }

    private void drawGround(SpriteBatch b, Draw d, float x, float y, float w, float h) {
        d.rect(b, x, y, w, h, DUST);
        d.rect(b, x, y + h - 0.04f, w, 0.04f, DUST_DARK);
        float[][] craters = {{0.18f, 0.7f, 0.22f}, {0.42f, 0.25f, 0.14f}, {0.8f, 0.62f, 0.18f}, {0.95f, 0.18f, 0.12f}};
        for (float[] c : craters) {
            d.ellipse(b, x + c[0] * w, y + c[1] * h, c[2], c[2] * 0.35f, CRATER, 0.8f);
            d.ellipse(b, x + c[0] * w, y + c[1] * h + c[2] * 0.08f, c[2] * 0.8f, c[2] * 0.22f, DUST_DARK, 0.8f);
        }
    }

    /** A little heart: two circles and a point. */
    private void heart(SpriteBatch b, Draw d, float x, float y, float s, float alpha) {
        d.circle(b, x - s * 0.5f, y, s * 0.6f, HEART, alpha);
        d.circle(b, x + s * 0.5f, y, s * 0.6f, HEART, alpha);
        d.shape(b, d.triangle, x, y - s * 0.55f, s * 0.95f, 270f, HEART, alpha);
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
