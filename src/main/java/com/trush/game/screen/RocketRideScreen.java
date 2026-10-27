package com.trush.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;
import com.trush.game.input.Gamepad;
import com.trush.game.input.Gamepad.Button;
import com.trush.game.launcher.ETGFanfic;

import java.util.function.Supplier;

/**
 * The ride up to the Moon in the family's cardboard rocket (the owner's son's idea): half a minute of a
 * little shooter - up past the clouds into the stars, steering the rocket round the asteroids and
 * shooting them while the fire button is held ({@link #fireHeld}). Bumping into one never hurts (ruining a won run would be cruel): the rocket
 * just wobbles. A taste of the full rocket game on the console at home ({@link RocketGameScreen}). Then the Moon rises and you
 * land on it ({@code next}: the Moon's floor). Esc / Start skips it.
 */
public class RocketRideScreen implements Screen {

    /** How long the ride takes, landing included. */
    public static final float LENGTH = 30f;
    /** When the Moon starts to rise over the top of the screen. */
    private static final float MOON_RISES = 25f;
    /** Asteroids come from here until the Moon rises. */
    private static final float ROCKS_FROM = 5f;
    private static final float SPEED = 3.2f;
    static final float FIRE_EVERY = 0.16f;
    static final float BULLET_SPEED = 7f;
    private static final float SCALE = 0.75f;
    /** The rocket's hitbox, round its middle. */
    private static final float RADIUS = 0.3f;
    private static final String[] LAYERS = {"rocket_box", "rocket_cone", "rocket_fins", "rocket_scarf", "rocket_engine"};

    private static final Color SKY = Color.valueOf("7ec8f0");
    private static final Color SPACE = Color.valueOf("070818");
    private static final Color CLOUD = Color.valueOf("f4f2fc");
    private static final Color CLOUD_PINK = Color.valueOf("ffd6ec");
    private static final Color ROCK = Color.valueOf("8e8a86");
    private static final Color ROCK_DARK = Color.valueOf("5e5a58");
    private static final Color ROCK_LIGHT = Color.valueOf("b8b4ae");
    private static final Color SHOT = Color.valueOf("ffd166");
    private static final Color FLAME = Color.valueOf("ffb43a");
    private static final Color FLAME_CORE = Color.valueOf("fff3c0");
    private static final Color MOON = Color.valueOf("d4d4dc");
    private static final Color MOON_DARK = Color.valueOf("a8a8b4");
    private static final Color TEXT = Color.valueOf("ffd166");

    private static final class Rock {
        float x;
        float y;
        float vx;
        float vy;
        float r;
        float spin;
        float angle;
        int hp;
    }

    private static final class Bit {
        float x;
        float y;
        float vx;
        float vy;
        float life;
        float size;
        Color color;
    }

    private final ETGFanfic game;
    private final Supplier<Screen> next;
    private Music music;
    private float time;
    private final Vector2 rocket = new Vector2(3.5f, 1.1f);
    private float fireTimer;
    private float rockTimer = ROCKS_FROM;
    /** Wobbling after a bump (no harm done). */
    private float wobble;
    private int rocksShot;
    private boolean leaving;
    private final Array<Rock> rocks = new Array<>();
    private final Array<Vector2> shots = new Array<>();
    private final Array<Bit> bits = new Array<>();
    private final Vector2 move = new Vector2();

    public RocketRideScreen(ETGFanfic game, Supplier<Screen> next) {
        this.game = game;
        this.next = next;
    }

    @Override
    public void show() {
        music = Gdx.audio.newMusic(Gdx.files.internal("assets/music/sky.ogg"));
        music.setLooping(true);
        music.setVolume(0.45f * game.getMusicVolume());
        music.play();
    }

    /** 0 on the clouds, 1 out in space (the sky darkens over the first part of the ride). */
    private float space() {
        return MathUtils.clamp((time - 3f) / 8f, 0f, 1f);
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1 / 30f);
        time += delta;
        OrthographicCamera cam = (OrthographicCamera) game.viewport.getCamera();
        cam.position.set(game.viewport.getWorldWidth() / 2f, game.viewport.getWorldHeight() / 2f, 0);
        cam.update();
        game.viewport.apply();
        update(delta);
        if (leaving) {
            return;
        }
        ScreenUtils.clear(Color.BLACK);
        SpriteBatch b = game.batch;
        b.setProjectionMatrix(cam.combined);
        b.begin();
        draw(b, game.draw);
        b.end();
    }

    // ------------------------------------------------------------ update

    private void update(float delta) {
        Gamepad pad = game.pad;
        if (time > 0.4f && (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || pad.justPressed(Button.START))
                || time >= LENGTH) {
            land();
            return;
        }
        wobble = Math.max(0f, wobble - delta);

        // steering (towards the end the rocket lines itself up to land)
        if (time < MOON_RISES + 1.5f) {
            move.setZero();
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) move.x -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) move.x += 1;
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) move.y += 1;
            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) move.y -= 1;
            if (move.isZero()) {
                move.set(pad.leftStick());
            }
            move.limit(1f);
            rocket.mulAdd(move, SPEED * delta);
            rocket.x = MathUtils.clamp(rocket.x, 0.4f, 6.6f);
            rocket.y = MathUtils.clamp(rocket.y, 0.7f, 3.6f);
        } else {
            rocket.x += (3.5f - rocket.x) * Math.min(1f, delta * 2f);
            rocket.y += 1.6f * delta; // up towards the Moon
        }

        // two little shots side by side while the fire button is held
        fireTimer -= delta;
        if (fireTimer <= 0 && time > 0.4f && time < MOON_RISES && fireHeld(game)) {
            fireTimer = FIRE_EVERY;
            shots.add(new Vector2(rocket.x - 0.12f, rocket.y + 0.75f));
            shots.add(new Vector2(rocket.x + 0.12f, rocket.y + 0.75f));
            game.sfx.play("shoot", 0.12f);
        }
        for (int i = shots.size - 1; i >= 0; i--) {
            Vector2 s = shots.get(i);
            s.y += BULLET_SPEED * delta;
            if (s.y > 5.3f) shots.removeIndex(i);
        }

        // asteroids: more and more of them, bigger ones later
        rockTimer -= delta;
        if (time > ROCKS_FROM && time < MOON_RISES - 1f && rockTimer <= 0) {
            float k = (time - ROCKS_FROM) / (MOON_RISES - ROCKS_FROM);
            rockTimer = MathUtils.lerp(0.9f, 0.35f, k) * MathUtils.random(0.7f, 1.3f);
            Rock r = new Rock();
            r.r = MathUtils.random(0.16f, 0.26f + 0.18f * k);
            r.x = MathUtils.random(0.3f, 6.7f);
            r.y = 5.4f;
            r.vx = MathUtils.random(-0.5f, 0.5f);
            r.vy = -MathUtils.random(1.3f, 2.1f + 1f * k);
            r.spin = MathUtils.random(-120f, 120f);
            r.hp = r.r > 0.3f ? 3 : r.r > 0.22f ? 2 : 1;
            rocks.add(r);
        }
        for (int i = rocks.size - 1; i >= 0; i--) {
            Rock r = rocks.get(i);
            r.x += r.vx * delta;
            r.y += r.vy * delta;
            r.angle += r.spin * delta;
            boolean gone = r.y < -0.6f;
            for (int j = shots.size - 1; j >= 0 && !gone; j--) {
                Vector2 s = shots.get(j);
                if (Vector2.dst2(s.x, s.y, r.x, r.y) < r.r * r.r) {
                    shots.removeIndex(j);
                    burst(s.x, s.y, 3, ROCK_LIGHT);
                    if (--r.hp <= 0) {
                        gone = true;
                        rocksShot++;
                        burst(r.x, r.y, 10, ROCK);
                        game.sfx.play("break", 0.5f);
                    }
                }
            }
            if (!gone && Vector2.dst2(rocket.x, rocket.y + 0.1f, r.x, r.y) < (r.r + RADIUS) * (r.r + RADIUS)) {
                gone = true; // a bump: the asteroid crumbles, the rocket wobbles - nothing worse
                wobble = 0.6f;
                burst(r.x, r.y, 12, ROCK);
                game.sfx.play("hit", 0.6f);
            }
            if (gone) rocks.removeIndex(i);
        }
        for (int i = bits.size - 1; i >= 0; i--) {
            Bit p = bits.get(i);
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.life -= delta;
            if (p.life <= 0) bits.removeIndex(i);
        }
    }

    private void burst(float x, float y, int n, Color color) {
        for (int i = 0; i < n; i++) {
            Bit p = new Bit();
            p.x = x;
            p.y = y;
            float a = MathUtils.random(MathUtils.PI2);
            float sp = MathUtils.random(0.5f, 2f);
            p.vx = MathUtils.cos(a) * sp;
            p.vy = MathUtils.sin(a) * sp;
            p.life = MathUtils.random(0.3f, 0.6f);
            p.size = MathUtils.random(0.03f, 0.07f);
            p.color = color;
            bits.add(p);
        }
    }

    /** On to the Moon's landing site. */
    private void land() {
        leaving = true;
        game.sfx.play("boom", 0.5f);
        game.setScreen(next.get());
        dispose();
    }

    // ------------------------------------------------------------ draw

    private void draw(SpriteBatch b, Draw d) {
        float sp = space();
        Color sky = new Color(SKY).lerp(SPACE, sp);
        d.rect(b, 0, 0, 7, 5, sky);
        // stars come out as the sky goes dark, streaming past
        for (int i = 0; i < 60; i++) {
            float sx = (i * 2.371f) % 7f;
            float sy = ((i * 1.913f) - time * (0.6f + (i % 3) * 0.5f)) % 5.2f;
            if (sy < 0) sy += 5.2f;
            d.rect(b, sx, sy, i % 7 == 0 ? 0.03f : 0.018f, i % 7 == 0 ? 0.03f : 0.018f, Color.WHITE, sp * 0.9f);
        }
        // the clouds of the kingdom falling away below, then a last few rushing past
        if (time < 12f) {
            float drop = time * 1.4f;
            for (int i = 0; i < 9; i++) {
                float cx = i * 0.85f;
                float cy = 0.4f - drop + (i % 2) * 0.25f;
                d.circle(b, cx, cy, 0.7f, i % 3 == 0 ? CLOUD_PINK : CLOUD);
            }
            for (int i = 0; i < 6; i++) {
                float cy = 6f + i * 1.6f - time * 2.4f;
                float cx = (i * 2.7f) % 7f;
                if (sp < 1f) {
                    d.circle(b, cx, cy, 0.45f, CLOUD);
                    d.circle(b, cx + 0.4f, cy - 0.1f, 0.35f, CLOUD);
                }
            }
        }
        // the Moon rising over the top at the end
        if (time > MOON_RISES - 2f) {
            float k = MathUtils.clamp((time - (MOON_RISES - 2f)) / (LENGTH - MOON_RISES + 1f), 0f, 1f);
            float my = 8.5f - 4f * k;
            float mr = 2.4f + 1.8f * k;
            d.circle(b, 3.5f, my, mr, MOON);
            d.circle(b, 2.6f, my - mr * 0.45f, mr * 0.18f, MOON_DARK);
            d.circle(b, 4.4f, my - mr * 0.25f, mr * 0.12f, MOON_DARK);
            d.circle(b, 3.3f, my - mr * 0.75f, mr * 0.09f, MOON_DARK);
        }
        for (Rock r : rocks) {
            d.circle(b, r.x, r.y, r.r, ROCK_DARK);
            d.circle(b, r.x - r.r * 0.12f, r.y + r.r * 0.12f, r.r * 0.85f, ROCK);
            float a = r.angle * MathUtils.degRad;
            d.circle(b, r.x + MathUtils.cos(a) * r.r * 0.4f, r.y + MathUtils.sin(a) * r.r * 0.4f, r.r * 0.22f, ROCK_DARK);
            d.circle(b, r.x - r.r * 0.35f, r.y + r.r * 0.35f, r.r * 0.18f, ROCK_LIGHT);
        }
        for (Vector2 s : shots) {
            d.rect(b, s.x - 0.025f, s.y - 0.09f, 0.05f, 0.18f, SHOT);
        }
        for (Bit p : bits) {
            d.rect(b, p.x - p.size / 2f, p.y - p.size / 2f, p.size, p.size, p.color, Math.min(1f, p.life * 3f));
        }
        drawRocket(b, d);

        if (time < 3f) {
            game.text("TO THE MOON!", 3.5f, 3.4f, 0.7f, TEXT, Align.center);
        }
        if (time < 7f && rocksShot == 0) {
            game.text(fireHint(game), 3.5f, 2.9f, 0.3f, Color.WHITE, Align.center);
        }
        if (time > MOON_RISES + 1f) {
            d.rect(b, 1.6f, 0.62f, 3.8f, 0.92f, SPACE, 0.75f);
            game.text("LANDING...", 3.5f, 1.4f, 0.5f, TEXT, Align.center);
            game.text("Asteroids shot: " + rocksShot, 3.5f, 0.9f, 0.3f, Color.WHITE, Align.center);
        }
        // how far to go: a little bar up the right edge, the Moon at the top
        float k = Math.min(1f, time / LENGTH);
        d.rect(b, 6.82f, 0.4f, 0.06f, 4.2f, Color.WHITE, 0.25f);
        d.rect(b, 6.82f, 0.4f, 0.06f, 4.2f * k, TEXT, 0.8f);
        d.circle(b, 6.85f, 4.7f, 0.08f, MOON);
        game.text(game.pad.isActive() ? "START skip" : "ESC skip", 0.1f, 0.18f, 0.2f, Color.WHITE, Align.left);
    }

    /** The fire button: held down, the rocket keeps shooting (Space / J / left click, RB / RT / A on the pad). */
    static boolean fireHeld(ETGFanfic game) {
        return Gdx.input.isKeyPressed(Input.Keys.SPACE) || Gdx.input.isKeyPressed(Input.Keys.J)
                || Gdx.input.isButtonPressed(Input.Buttons.LEFT)
                || game.pad.pressed(Button.RB) || game.pad.pressed(Button.RT) || game.pad.pressed(Button.A);
    }

    static String fireHint(ETGFanfic game) {
        return game.pad.isActive() ? "HOLD RB / A TO SHOOT" : "HOLD SPACE OR CLICK TO SHOOT";
    }

    private void drawRocket(SpriteBatch b, Draw d) {
        drawRocket(game, b, d, rocket.x, rocket.y, time, wobble > 0, wobble > 0 && ((int) (time * 20f)) % 2 == 0);
    }

    /** The family's rocket, its middle at (rx, ry), the engine roaring under it; {@code shake} wobbles it, {@code blink} fades it. */
    static void drawRocket(ETGFanfic game, SpriteBatch b, Draw d, float rx, float ry, float time, boolean shake, boolean blink) {
        TextureRegion box = game.sprites.home(LAYERS[0]);
        float w = Sprites.w(box) * SCALE;
        float x = rx - w / 2f + (shake ? MathUtils.sin(time * 60f) * 0.05f : 0f);
        float y = ry - 0.55f;
        float nx = x + 20f / Sprites.PPU * SCALE;
        float flicker = 0.8f + 0.2f * MathUtils.sin(time * 40f);
        d.circle(b, nx, y - 0.12f, 0.17f * flicker, FLAME, 0.85f); // the vacuum cleaner roaring
        d.circle(b, nx, y - 0.28f, 0.11f * flicker, FLAME, 0.6f);
        d.circle(b, nx, y - 0.08f, 0.09f * flicker, FLAME_CORE, 0.95f);
        if (blink) {
            b.setColor(1f, 1f, 1f, 0.6f);
        }
        for (String layer : LAYERS) {
            TextureRegion r = game.sprites.home(layer);
            b.draw(r, x, y, Sprites.w(r) * SCALE, Sprites.h(r) * SCALE);
        }
        b.setColor(Color.WHITE);
    }

    @Override
    public void resize(int width, int height) {
        game.viewport.update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        music.dispose();
    }
}
