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
import com.trush.game.objects.Cat;
import com.trush.game.player.Hero;
import com.trush.game.save.RunSave;
import com.trush.game.save.Unlocks;

import java.util.EnumSet;

/**
 * The family's living room, where the game starts (like ETG's Breach): walk around and use the
 * furniture - the front door goes outside (a story run, or the saved trip), the TV plays endless,
 * the picture book on the bookshelf tells the story again, the trophy shelf shows your records.
 * The rest of the family (player/Hero) stands about: walk up to one to play as them instead - grandad
 * only once the Fluffy King has been beaten ("maybe another time" till then). Granny's cat sits at
 * her feet, or follows her around. In the corner stands the family's cardboard rocket: it turns up with
 * the first win over the Fluffy King, and each of the family who beats him adds their piece
 * ({@link Unlocks#kingBeatenBy}); once all four have, it's finished - the way to space.
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
        TROPHIES(4.6f, 3.5f, "TROPHIES"),
        ROCKET(5.45f, 0.4f, "THE ROCKET");

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
        /** Its sprite, or the layers drawn one over the other (the rocket's pieces). */
        final String[] sprites;
        final float x;
        final float y;
        final Rectangle solid;

        Piece(String[] sprites, float x, float y, Rectangle solid) {
            this.sprites = sprites;
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
    /** A family member close enough to talk to / the one asked "play as ...?". */
    private Hero nearHero;
    private Hero askingHero;
    /** Who's talking in a speech bubble, what they say and for how much longer. */
    private Hero speaker;
    private String speech;
    private float speechTime;
    private final Cat cat = new Cat();
    private final MenuList question = new MenuList(2.35f, 0.32f, 0.42f);
    private boolean showTrophies;
    private boolean showRocket;
    /** Who has added their piece to the rocket; the rocket isn't there at all before the first win. */
    private final EnumSet<Hero> rocketBuilders = EnumSet.noneOf(Hero.class);
    private boolean rocketThere;
    /** The rocket's piece, to puff smoke from its engine once it's finished. */
    private Piece rocket;
    private RunSave saved;
    private final Rectangle tmpRect = new Rectangle();
    private final Rectangle tmpRect2 = new Rectangle();

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
        addRocket();
        Vector2 home = home(Hero.current);
        feet.set(home);
        facingRight = home.x < 3.5f;
    }

    /** Where each of the family stands in the room while someone else is being played. */
    private static Vector2 home(Hero h) {
        switch (h) {
            case SISTER:
                return new Vector2(1.9f, 3.3f);   // by the window
            case GRANNY:
                return new Vector2(1.75f, 1.55f); // beside her armchair
            case GRANDAD:
                return new Vector2(5.85f, 1.95f); // by the lamp, near the TV (the rocket's in the corner)
            default:
                return new Vector2(4.2f, 2.35f);  // on the rug
        }
    }

    /** The feet of the family members standing about: you can't walk through them. */
    private boolean bumpsIntoFamily(Rectangle r) {
        for (Hero h : Hero.values()) {
            if (h == Hero.current) continue;
            Vector2 p = home(h);
            if (tmpRect2.set(p.x - 0.15f, p.y, 0.3f, 0.12f).overlaps(r)) return true;
        }
        return false;
    }

    /** A piece of furniture at pixel (px, py) (top-left, y down); its solid foot is {@code footH} tall, inset {@code inset}. */
    private Piece addPiece(String sprite, int px, int py, float inset, float footH) {
        return addPiece(new String[] {sprite}, px, py, inset, footH);
    }

    private Piece addPiece(String[] sprites, int px, int py, float inset, float footH) {
        TextureRegion r = game.sprites.home(sprites[0]);
        float x = px / Sprites.PPU;
        float y = (160 - py - r.getRegionHeight()) / Sprites.PPU;
        Rectangle solid = new Rectangle(x + inset, y, Sprites.w(r) - inset * 2f, footH);
        Piece p = new Piece(sprites, x, y, solid);
        pieces.add(p);
        solids.add(solid);
        return p;
    }

    /**
     * The cardboard rocket in the bottom-right corner: the box once the King has been beaten at all (also
     * by wins from before the family's pieces were remembered), then each builder's piece on top. Dev
     * {@code -Dfluffy.rocket=N} shows it built by the first N of the family (not saved).
     */
    private void addRocket() {
        rocketBuilders.addAll(Unlocks.kingBeatenBy);
        int dev = Dev.number("rocket", -1);
        if (dev >= 0) {
            rocketBuilders.clear();
            for (Hero h : Hero.values()) {
                if (h.ordinal() < dev) rocketBuilders.add(h);
            }
        }
        rocketThere = dev > 0 || dev < 0 && (game.getWins() > 0 || !rocketBuilders.isEmpty());
        if (!rocketThere) {
            return;
        }
        Array<String> layers = new Array<>(String.class);
        layers.add("rocket_box");
        for (Hero h : Hero.values()) {
            if (rocketBuilders.contains(h)) layers.add(rocketPiece(h));
        }
        rocket = addPiece(layers.toArray(), 182, 106, 0.1f, 0.3f);
    }

    /** What each of the family adds to the rocket (sprite rocket_*). */
    private static String rocketPiece(Hero h) {
        switch (h) {
            case SISTER:
                return "rocket_fins";
            case GRANNY:
                return "rocket_scarf";
            case GRANDAD:
                return "rocket_engine";
            default:
                return "rocket_cone";
        }
    }

    private static String rocketPieceName(Hero h) {
        switch (h) {
            case SISTER:
                return "THE FINS";
            case GRANNY:
                return "A SCARF + THE CAT'S HELMET";
            case GRANDAD:
                return "THE ENGINE";
            default:
                return "THE NOSE CONE";
        }
    }

    private boolean rocketBuilt() {
        return rocketBuilders.size() == Hero.values().length;
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
        if (showTrophies || showRocket) {
            if (anyConfirm() || back()) {
                showTrophies = false;
                showRocket = false;
            }
            return;
        }
        speechTime -= delta;
        if (asking != null || askingHero != null) {
            int chosen = question.update(game);
            if (back()) {
                asking = null;
                askingHero = null;
            } else if (chosen >= 0 && askingHero != null) {
                if (chosen == 0) {
                    switchTo(askingHero);
                }
                askingHero = null;
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
        if (Hero.current == Hero.GRANNY) {
            cat.update(delta, feet.x, feet.y, facingRight);
        }
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
        nearHero = null;
        float best = REACH;
        for (Spot s : Spot.values()) {
            if (s == Spot.ROCKET && !rocketThere) continue;
            float d = feet.dst(s.x, s.y);
            if (d < best) {
                best = d;
                near = s;
            }
        }
        for (Hero h : Hero.values()) {
            if (h == Hero.current) continue;
            float d = feet.dst(home(h));
            if (d < best) {
                best = d;
                near = null;
                nearHero = h;
            }
        }
        boolean use = Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || pad.justPressed(Button.A);
        if (use && near != null) {
            use(near);
        } else if (use && nearHero != null) {
            talkTo(nearHero);
        }
    }

    /** Walked up to one of the family: "play as ...?" - or grandad, until the King's been beaten. */
    private void talkTo(Hero h) {
        if (!h.unlocked()) {
            say(h, "Maybe another time, kiddo.");
            game.sfx.play("hit", 0.3f);
            return;
        }
        askingHero = h;
        question.setItems("YES", "NO");
        question.select(0);
        game.sfx.play("door", 0.4f);
    }

    private void say(Hero who, String line) {
        speaker = who;
        speech = line;
        speechTime = 2.8f;
    }

    /** Swap: the one you were goes back to their spot, you carry on as {@code h} from right here. */
    private void switchTo(Hero h) {
        Hero.pick(h, game.prefs);
        cat.reset();
        game.sfx.play("dodge", 0.6f);
        say(h, h == Hero.GRANNY ? "Come along, puss!" : h == Hero.SISTER ? "Hi-yah!" : h == Hero.GRANDAD
                ? "Right, where's my coat?" : "Let's go!");
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
        if (bumpsIntoFamily(tmpRect)) {
            return;
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
        if (s == Spot.ROCKET) {
            showRocket = true;
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
        if (askingHero != null) {
            return "PLAY AS " + askingHero.title + "?";
        }
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
        for (Hero h : Hero.values()) {
            if (h != Hero.current) order.add(h);
        }
        order.add(cat);
        if (Hero.current != Hero.GRANNY) { // sitting at granny's feet while she waits
            cat.pos.set(home(Hero.GRANNY)).add(0.32f, -0.12f);
        }
        order.sort((p, q) -> Float.compare(baseY(q), baseY(p)));
        for (Object o : order) {
            if (o == this) {
                drawHero(b, d, Hero.current, feet.x, feet.y, facingRight, facingUp, walking);
            } else if (o instanceof Hero) {
                Vector2 p = home((Hero) o);
                drawHero(b, d, (Hero) o, p.x, p.y, p.x < feet.x, false, false);
            } else if (o == cat) {
                cat.draw(b, d);
            } else {
                drawPiece(b, d, (Piece) o);
            }
        }
    }

    private void drawPiece(SpriteBatch b, Draw d, Piece p) {
        float x = p.x;
        if (p == rocket && rocketBuilt() && time % 4f < 0.5f) { // grandad's engine rattles now and then
            x += (((int) (time * 30f)) % 2 == 0 ? 1 : -1) / Sprites.PPU;
        }
        for (String sprite : p.sprites) {
            TextureRegion r = game.sprites.home(sprite);
            b.draw(r, x, p.y, Sprites.w(r), Sprites.h(r));
        }
        if (p == rocket && rocketBuilt()) { // ... and puffs a little smoke out of the nozzle
            for (int i = 0; i < 3; i++) {
                float t = (time * 0.6f + i / 3f) % 1f;
                float px = p.x + 20 / Sprites.PPU + 0.25f * t + 0.04f * MathUtils.sin(time * 3f + i);
                float py = p.y + 0.06f + 0.2f * t;
                d.circle(b, px, py, 0.04f + 0.06f * t, Color.LIGHT_GRAY, 0.45f * (1f - t));
            }
        }
    }

    private float baseY(Object o) {
        if (o == this) return feet.y;
        if (o instanceof Hero) return home((Hero) o).y;
        if (o == cat) return cat.sortY();
        return ((Piece) o).y;
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

    private void drawHero(SpriteBatch b, Draw d, Hero who, float x, float y, boolean right, boolean up, boolean walks) {
        TextureRegion[][] frames = game.sprites.hero(who.sheet);
        int row = up ? 1 : 0;
        float t = walks ? animTime : time + who.ordinal() * 0.7f; // (the family doesn't breathe in step)
        TextureRegion r = walks ? frames[row][((int) (t * 10f)) % 4] : frames[row][4 + ((int) (t * 2f)) % 2];
        float w = Sprites.w(r);
        float h = Sprites.h(r);
        d.ellipse(b, x, y + 0.03f, 0.2f, 0.06f, Color.BLACK, 0.35f);
        b.draw(r.getTexture(), x - w / 2f, y, w / 2f, 0, w, h, 1f, 1f, 0f,
                r.getRegionX(), r.getRegionY(), r.getRegionWidth(), r.getRegionHeight(), !right, false);
    }

    /** Where someone's head is, for their speech bubble. */
    private Vector2 headOf(Hero h) {
        return h == Hero.current ? new Vector2(feet.x, feet.y + 1f) : home(h).add(0, 1f);
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
            if (game.getFalseKingBeaten() > 0) { // (a secret: only once you've found him)
                game.text("False King beaten: " + game.getFalseKingBeaten(), cx, 2.05f, 0.26f, GOLD, Align.center);
            }
            game.text(pad ? "A / B close" : "E / ESC close", cx, 1.85f, 0.24f, DIM, Align.center);
            return;
        }
        if (showRocket) {
            drawRocketPanel(b, d, cx, pad);
            return;
        }
        if (askingHero != null) {
            d.rect(b, 0, 0, 7, 5, Color.BLACK, 0.5f);
            panel(b, d, cx, 2.3f, 6.2f, 2.2f);
            game.text(questionTitle(), cx, 3.2f, 0.55f, GOLD, Align.center);
            game.text(askingHero.blurb, cx, 2.75f, 0.24f, TEXT, Align.center);
            question.setTop(2.3f);
            question.draw(game, time);
            return;
        }
        if (asking != null) {
            d.rect(b, 0, 0, 7, 5, Color.BLACK, 0.5f);
            panel(b, d, cx, 2.45f, 5.2f, 1.9f);
            game.text(questionTitle(), cx, 3.15f, 0.55f, GOLD, Align.center);
            if (asking == Spot.DOOR && saved != null && saved.hero() != Hero.current) {
                game.text("(it's " + saved.hero().title.toLowerCase() + "'s trip)", cx, 2.72f, 0.24f, DIM, Align.center);
            }
            question.setTop(2.35f);
            question.draw(game, time);
            return;
        }
        if (speechTime > 0 && speaker != null) {
            Vector2 head = headOf(speaker);
            float tw = game.textWidth(speech, 0.26f);
            float px = MathUtils.clamp(head.x, tw / 2f + 0.15f, 6.85f - tw / 2f);
            float py = Math.min(head.y + 0.25f, 4.85f);
            d.rect(b, px - tw / 2f - 0.1f, py - 0.24f, tw + 0.2f, 0.3f, Color.WHITE, 0.95f);
            d.rect(b, head.x - 0.04f, py - 0.32f, 0.08f, 0.08f, Color.WHITE, 0.95f);
            game.text(speech, px, py - 0.03f, 0.26f, PANEL, Align.center);
        } else if (near != null || nearHero != null) { // what you can do here, floating over the spot
            String key = pad ? "A" : "E";
            String what = near != null ? near.prompt : nearHero.unlocked() ? "PLAY AS " + nearHero.title : "TALK";
            float sx = near != null ? near.x : home(nearHero).x;
            float sy = near != null ? near.y : home(nearHero).y;
            float py = Math.min(sy + 1.15f, 4.8f);
            float tw = game.textWidth(key + "  " + what, 0.28f);
            float px = MathUtils.clamp(sx, tw / 2f + 0.15f, 6.85f - tw / 2f);
            d.rect(b, px - tw / 2f - 0.08f, py - 0.2f, tw + 0.16f, 0.26f, PANEL, 0.8f);
            game.text(key + "  " + what, px, py, 0.28f, GOLD, Align.center);
        }
        game.text(pad ? "START title screen" : "ESC title screen", 0.1f, 0.18f, 0.2f, TEXT, Align.left); // (the rocket's corner is right)
    }

    /** Who has built which piece of the rocket, and what it's for once it's finished. */
    private void drawRocketPanel(SpriteBatch b, Draw d, float cx, boolean pad) {
        d.rect(b, 0, 0, 7, 5, Color.BLACK, 0.6f);
        panel(b, d, cx, 2.5f, 6.4f, 2.7f);
        game.text("THE ROCKET", cx, 3.6f, 0.6f, GOLD, Align.center);
        float y = 3.05f;
        for (Hero h : Hero.values()) {
            boolean done = rocketBuilders.contains(h);
            game.text(h.title + ": " + (done ? rocketPieceName(h) : "?"), cx, y, 0.26f, done ? TEXT : DIM, Align.center);
            y -= 0.28f;
        }
        int left = Hero.values().length - rocketBuilders.size();
        if (rocketBuilt()) {
            game.text("WE SHOULD TRY IT SOMETIME...", cx, 1.8f, 0.26f, GOLD, Align.center);
            game.text("UP IN THE CLOUDS!", cx, 1.55f, 0.26f, GOLD, Align.center);
        } else {
            game.text("EVERYONE WHO BEATS THE FLUFFY KING", cx, 1.8f, 0.22f, TEXT, Align.center);
            game.text("ADDS A PIECE - " + left + " MORE TO GO!", cx, 1.55f, 0.22f, TEXT, Align.center);
        }
        game.text(pad ? "A / B close" : "E / ESC close", cx, 1.3f, 0.22f, DIM, Align.center);
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
