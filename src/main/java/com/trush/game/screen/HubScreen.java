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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;
import com.trush.game.input.Gamepad;
import com.trush.game.input.Gamepad.Button;
import com.trush.game.launcher.Dev;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.save.RunSave;

/**
 * The family's living room, where the game starts (like ETG's Breach): walk around and use the
 * furniture - the front door goes outside (a story run, or the saved trip), the TV plays endless,
 * the picture book on the bookshelf tells the story again, the trophy shelf shows your records.
 * Esc / Start goes back to the title screen. One screen, 7x5 units; art from home.png.
 */
public class HubScreen implements Screen {

    private static final Color TEXT = Color.valueOf("f5f0e6");
    private static final Color DIM = Color.valueOf("8a8079");
    private static final Color GOLD = Color.valueOf("ffd166");
    private static final Color PANEL = Color.valueOf("1e1416");
    private static final Color TV_GLOW = Color.valueOf("6ac8ff");
    private static final float MUSIC_VOLUME = 0.4f;

    private static final float SPEED = 2.2f;
    /** How close (feet to spot) the hero must be to use something. */
    private static final float REACH = 0.6f;
    private static final float FEET_W = 0.24f;
    private static final float FEET_H = 0.1f;
    /** The floor: the wall's foot is at y 3.66 (px row 43 of 160). */
    private static final Rectangle FLOOR = new Rectangle(0.05f, 0.05f, 6.9f, 3.55f);

    /** Something in the room you can walk up to and use. */
    private enum Spot {
        DOOR(3.5f, 3.5f, "GO OUTSIDE"),
        TV(6.0f, 2.95f, "WATCH TV"),
        BOOKS(0.65f, 3.15f, "READ THE STORY"),
        TROPHIES(4.6f, 3.5f, "TROPHIES");

        final float x;
        final float y;
        final String prompt;

        Spot(float x, float y, String prompt) {
            this.x = x;
            this.y = y;
            this.prompt = prompt;
        }
    }

    /** Free-standing furniture: drawn sorted by depth with the hero, solid at its foot. */
    private static final class Piece {
        final String sprite;
        final float x;
        final float y;
        final Rectangle solid;

        Piece(String sprite, float x, float y, Rectangle solid) {
            this.sprite = sprite;
            this.x = x;
            this.y = y;
            this.solid = solid;
        }
    }

    private final ETGFanfic game;
    private Music music;
    private float time;
    private final Array<Piece> pieces = new Array<>();
    /** Walls of furniture baked into the background (bookshelf, TV cabinet) plus the pieces' feet. */
    private final Array<Rectangle> solids = new Array<>();
    /** The hero's feet (centre bottom). */
    private final Vector2 feet = new Vector2(3.5f, 1.9f);
    private final Vector2 move = new Vector2();
    private boolean facingRight = true;
    private boolean facingUp;
    private boolean walking;
    private float animTime;
    private Spot near;
    /** The question being asked (door, TV, book), or null. */
    private Spot asking;
    private final MenuList question = new MenuList(2.35f, 0.32f, 0.42f);
    private boolean showTrophies;
    private RunSave saved;
    private final Rectangle tmpRect = new Rectangle();

    public HubScreen(ETGFanfic game) {
        this.game = game;
        float u = 1f / Sprites.PPU;
        // free-standing furniture, placed by its top-left pixel in the 224x160 room
        addPiece("sofa", 74, 118, 0.15f, 0.55f);
        addPiece("armchair", 14, 96, 0.1f, 0.45f);
        addPiece("lamp", 158, 96, 0.2f, 0.12f);
        addPiece("plant", 82, 18, 0.05f, 0.25f);
        // against the wall, baked into the background
        solids.add(new Rectangle(4 * u, (160 - 54) * u, 34 * u, 54 * u)); // bookshelf
        solids.add(new Rectangle(168 * u, (160 - 60) * u, 48 * u, 60 * u)); // TV cabinet
    }

    /** A piece of furniture at pixel (px, py) (top-left, y down); its solid foot is {@code footH} tall, inset {@code inset}. */
    private void addPiece(String sprite, int px, int py, float inset, float footH) {
        TextureRegion r = game.sprites.home(sprite);
        float x = px / Sprites.PPU;
        float y = (160 - py - r.getRegionHeight()) / Sprites.PPU;
        Rectangle solid = new Rectangle(x + inset, y, Sprites.w(r) - inset * 2f, footH);
        pieces.add(new Piece(sprite, x, y, solid));
        solids.add(solid);
    }

    @Override
    public void show() {
        music = Gdx.audio.newMusic(Gdx.files.internal("assets/music/menu.ogg"));
        music.setLooping(true);
        music.setVolume(MUSIC_VOLUME * game.getMusicVolume());
        music.play();
        saved = RunSave.read(game.prefs);
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1 / 30f);
        time += delta;
        ScreenUtils.clear(Color.BLACK);
        OrthographicCamera cam = (OrthographicCamera) game.viewport.getCamera();
        cam.position.set(game.viewport.getWorldWidth() / 2f, game.viewport.getWorldHeight() / 2f, 0);
        cam.update();
        game.viewport.apply();

        if (time > 0.25f) { // (the key that brought us here mustn't use something right away)
            update(delta);
        }

        SpriteBatch b = game.batch;
        b.setProjectionMatrix(cam.combined);
        b.begin();
        drawRoom(b);
        drawOverlay(b);
        b.end();
    }

    // ------------------------------------------------------------ update

    private void update(float delta) {
        Gamepad pad = game.pad;
        if (showTrophies) {
            if (anyConfirm() || back()) {
                showTrophies = false;
            }
            return;
        }
        if (asking != null) {
            int chosen = question.update(game);
            if (back()) {
                asking = null;
            } else if (chosen >= 0) {
                answer(chosen);
            }
            return;
        }
        if (back()) {
            leave(new MainMenuScreen(game));
            return;
        }

        move.setZero();
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) move.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) move.x += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) move.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) move.y -= 1;
        if (move.isZero()) {
            move.set(pad.leftStick());
        }
        move.limit(1f);
        walking = move.len2() > 0.01f;
        if (walking) {
            animTime += delta;
            if (Math.abs(move.x) > 0.1f) {
                facingRight = move.x > 0;
            }
            facingUp = move.y > 0.35f;
            step(move.x * SPEED * delta, 0);
            step(0, move.y * SPEED * delta);
        } else {
            animTime += delta * 0.5f;
        }

        near = null;
        float best = REACH;
        for (Spot s : Spot.values()) {
            float d = feet.dst(s.x, s.y);
            if (d < best) {
                best = d;
                near = s;
            }
        }
        if (near != null && (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || pad.justPressed(Button.A))) {
            use(near);
        }
    }

    /** Moves the feet by (dx, dy) unless that walks into a wall or furniture. */
    private void step(float dx, float dy) {
        float nx = feet.x + dx;
        float ny = feet.y + dy;
        tmpRect.set(nx - FEET_W / 2f, ny, FEET_W, FEET_H);
        if (!FLOOR.contains(tmpRect)) {
            return;
        }
        for (Rectangle r : solids) {
            if (r.overlaps(tmpRect)) {
                return;
            }
        }
        feet.set(nx, ny);
    }

    private boolean back() {
        return Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || game.pad.justPressed(Button.B)
                || game.pad.justPressed(Button.START);
    }

    private boolean anyConfirm() {
        return Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.justTouched() || game.pad.justPressed(Button.A);
    }

    private void use(Spot s) {
        game.sfx.play("door", 0.4f);
        if (s == Spot.TROPHIES) {
            showTrophies = true;
            return;
        }
        asking = s;
        switch (s) {
            case DOOR:
                if (saved != null) {
                    question.setItems(saved.describe(), "START A NEW TRIP", "STAY HOME");
                } else {
                    question.setItems("YES", "NO");
                }
                break;
            default:
                question.setItems("YES", "NO");
                break;
        }
        question.select(0);
    }

    private String questionTitle() {
        switch (asking) {
            case DOOR:
                return saved != null ? "CONTINUE THE TRIP?" : "GO OUTSIDE?";
            case TV:
                return "PLAY ENDLESS?";
            default:
                return "READ THE STORY AGAIN?";
        }
    }

    private void answer(int chosen) {
        Spot s = asking;
        asking = null;
        switch (s) {
            case DOOR:
                if (saved != null && chosen == 0) {
                    if (!Dev.flag("keepSave")) {
                        RunSave.delete(game.prefs); // used up, like ETG: the next save point is after this floor's boss
                    }
                    leave(new GameScreen(game, saved));
                } else if (saved != null && chosen == 1 || saved == null && chosen == 0) {
                    RunSave.delete(game.prefs); // a new trip abandons the saved one
                    leave(new GameScreen(game, GameScreen.Mode.STORY));
                }
                break;
            case TV:
                if (chosen == 0) {
                    leave(new GameScreen(game, GameScreen.Mode.ENDLESS)); // (a saved trip stays saved)
                }
                break;
            default:
                if (chosen == 0) {
                    leave(new IntroScreen(game, () -> game.setScreen(new HubScreen(game))));
                }
                break;
        }
    }

    private void leave(Screen next) {
        game.setScreen(next);
        dispose();
    }

    // ------------------------------------------------------------ draw

    private void drawRoom(SpriteBatch b) {
        Draw d = game.draw;
        TextureRegion room = game.sprites.home("room");
        b.draw(room, 0, 0, Sprites.w(room), Sprites.h(room));
        drawTv(b, d);

        // the hero and the free-standing furniture, back to front
        Array<Object> order = new Array<>();
        order.addAll(pieces);
        order.add(this);
        order.sort((p, q) -> Float.compare(baseY(q), baseY(p)));
        for (Object o : order) {
            if (o == this) {
                drawHero(b, d);
            } else {
                Piece p = (Piece) o;
                TextureRegion r = game.sprites.home(p.sprite);
                b.draw(r, p.x, p.y, Sprites.w(r), Sprites.h(r));
            }
        }
    }

    private float baseY(Object o) {
        return o == this ? feet.y : ((Piece) o).y;
    }

    /** The old TV is on: a flickering blue picture with something moving about on it. */
    private void drawTv(SpriteBatch b, Draw d) {
        float u = 1f / Sprites.PPU;
        float x = 175 * u;
        float y = (160 - 31) * u;
        float w = 30 * u;
        float h = 22 * u;
        float flicker = 0.75f + 0.1f * MathUtils.sin(time * 23f) + 0.05f * MathUtils.sin(time * 61f);
        d.rect(b, x, y, w, h, TV_GLOW, 0.35f * flicker);
        for (int i = 0; i < 5; i++) { // little bullets flying across the screen
            float bx = x + ((time * 0.25f + i * 0.21f) % 1f) * w;
            float by = y + h * (0.2f + 0.6f * ((i * 0.37f) % 1f));
            d.circle(b, bx, by, 0.025f, Color.WHITE, 0.8f);
        }
        d.rect(b, x, y + h * ((time * 0.7f) % 1f), w, 0.02f, Color.WHITE, 0.15f); // the rolling scanline
        if (near == Spot.TV || asking == Spot.TV) {
            d.rect(b, x - 0.1f, y - 0.1f, w + 0.2f, h + 0.2f, TV_GLOW, 0.12f);
        }
    }

    private void drawHero(SpriteBatch b, Draw d) {
        Sprites s = game.sprites;
        int row = facingUp ? 1 : 0;
        TextureRegion r = walking ? s.hero[row][((int) (animTime * 10f)) % 4] : s.hero[row][4 + ((int) (animTime * 2f)) % 2];
        float w = Sprites.w(r);
        float h = Sprites.h(r);
        d.ellipse(b, feet.x, feet.y + 0.03f, 0.2f, 0.06f, Color.BLACK, 0.35f);
        b.draw(r.getTexture(), feet.x - w / 2f, feet.y, w / 2f, 0, w, h, 1f, 1f, 0f,
                r.getRegionX(), r.getRegionY(), r.getRegionWidth(), r.getRegionHeight(), !facingRight, false);
    }

    private void drawOverlay(SpriteBatch b) {
        Draw d = game.draw;
        float cx = game.viewport.getWorldWidth() / 2f;
        boolean pad = game.pad.isActive();
        if (showTrophies) {
            d.rect(b, 0, 0, 7, 5, Color.BLACK, 0.6f);
            panel(b, d, cx, 2.6f, 4.4f, 2.2f);
            game.text("TROPHIES", cx, 3.45f, 0.6f, GOLD, Align.center);
            game.text("Kingdom saved: " + game.getWins() + (game.getWins() == 1 ? " time" : " times"), cx, 2.9f, 0.32f, TEXT, Align.center);
            game.text("Best score: " + game.getHighScore(), cx, 2.6f, 0.32f, TEXT, Align.center);
            game.text("Best endless wave: " + game.getBestWave(), cx, 2.3f, 0.32f, TEXT, Align.center);
            game.text(pad ? "A / B close" : "E / ESC close", cx, 1.85f, 0.24f, DIM, Align.center);
            return;
        }
        if (asking != null) {
            d.rect(b, 0, 0, 7, 5, Color.BLACK, 0.5f);
            panel(b, d, cx, 2.45f, 5.2f, 1.9f);
            game.text(questionTitle(), cx, 3.15f, 0.55f, GOLD, Align.center);
            question.draw(game, time);
            return;
        }
        if (near != null) { // what you can do here, floating over the spot
            String key = pad ? "A" : "E";
            float py = Math.min(near.y + 1.15f, 4.8f);
            float tw = game.textWidth(key + "  " + near.prompt, 0.28f);
            float px = MathUtils.clamp(near.x, tw / 2f + 0.15f, 6.85f - tw / 2f);
            d.rect(b, px - tw / 2f - 0.08f, py - 0.2f, tw + 0.16f, 0.26f, PANEL, 0.8f);
            game.text(key + "  " + near.prompt, px, py, 0.28f, GOLD, Align.center);
        }
        game.text(pad ? "START title screen" : "ESC title screen", 6.9f, 0.18f, 0.2f, TEXT, Align.right);
    }

    private void panel(SpriteBatch b, Draw d, float cx, float cy, float w, float h) {
        d.rect(b, cx - w / 2f - 0.04f, cy - h / 2f - 0.04f, w + 0.08f, h + 0.08f, GOLD, 0.9f);
        d.rect(b, cx - w / 2f, cy - h / 2f, w, h, PANEL, 0.95f);
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
