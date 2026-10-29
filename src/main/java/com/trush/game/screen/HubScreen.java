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
import com.trush.game.enemy.FluffyKing;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Cat;
import com.trush.game.player.Crown;
import com.trush.game.player.Hero;
import com.trush.game.save.RunSave;
import com.trush.game.save.Unlocks;

import java.util.EnumSet;

/**
 * The family's living room, where the game starts (like ETG's Breach): walk around and use the
 * furniture - the front door goes outside (a story run, or the saved trip), the TV plays endless,
 * the picture book on the bookshelf tells the story again, the trophy shelf opens the Flufficon (records,
 * achievements, everything met and found - screen/FlufficonBook).
 * The rest of the family (player/Hero) stands about: walk up to one to play as them instead - grandad
 * only once the Fluffy King has been beaten ("maybe another time" till then). Granny's cat sits at
 * her feet, or follows her around. In the corner stands the family's cardboard rocket: it turns up with
 * the first win over the Fluffy King, and each of the family who beats him adds their piece
 * ({@link Unlocks#kingBeatenBy}); once all four have, it's finished - the way to space. Once it has been up
 * to the Moon ({@link Unlocks#boardedRocket}) a game console sits in front of the TV: the rocket game
 * ({@link RocketGameScreen}).
 * Once all four rocket builders wear golden skull crowns the Fluffy King moves in ({@link #kingHere}): a
 * card says so the first time, and a royal carpet leads out of the living room's left side into his own
 * room ({@link #inKingRoom}, art king_room) - him full size on his big throne, his shrinking potion beside
 * it, which makes him our size to play.
 * Esc / Start goes back to the title screen. One screen, 7x5 units; art from home.png.
 */
public class HubScreen implements Screen {

    private static final Color TEXT = Color.valueOf("f5f0e6");
    private static final Color DIM = Color.valueOf("8a8079");
    private static final Color GOLD = Color.valueOf("ffd166");
    private static final Color BONE = Color.valueOf("efe8d8");
    private static final Color BONE_SH = Color.valueOf("c4b9a2");
    private static final Color HARD_GLOW = Color.valueOf("ff3a3a");
    private static final Color PANEL = Color.valueOf("1e1416");
    private static final Color TV_GLOW = Color.valueOf("6ac8ff");
    private static final float MUSIC_VOLUME = 0.4f;
    private static final Color POTION = Color.valueOf("8a5ae8");
    private static final Color GLASS = Color.valueOf("dfe8ff");
    /** prefs: the "THE FLUFFY KING MOVES IN!" card has been shown. */
    private static final String KING_MOVED_IN = "kingMovedIn";
    /** The doorway between the living room (its left side) and the King's room (its right side). */
    private static final float DOOR_BOTTOM = 1.9f;
    private static final float DOOR_TOP = 2.75f;
    /** The King's throne in his room: drawn this much bigger than its sprite, its foot at THRONE_Y. */
    private static final float THRONE_SCALE = 1.5f;
    private static final float THRONE_Y = 2.05f;
    /** Where the King sits on it (his body's middle), full size. */
    private static final float KING_X = 3.5f;
    private static final float KING_Y = 3.2f;
    /** His potion on its little table, right of the throne. */
    private static final float POTION_X = 5.05f;
    private static final float POTION_Y = 2.15f;
    private static final Color CARPET = Color.valueOf("c0306a");
    private static final Color CARPET_TRIM = Color.valueOf("ffc83d");

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
        TV(5.8f, 2.95f, "WATCH TV"),
        CONSOLE(6.55f, 2.4f, "PLAY THE ROCKET GAME"),
        BOOKS(0.65f, 3.15f, "READ THE STORY"),
        TROPHIES(4.6f, 3.5f, "TROPHIES"),
        ROCKET(5.45f, 0.4f, "THE ROCKET"),
        SKULL(1.75f, 0.62f, "THE SKULL");

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
    /** How long the title's and the living room's music take to fade in. */
    static final float MUSIC_FADE_IN = 1.2f;
    private float musicFade;
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
    /** The trophy shelf's book of records, achievements and everything met and found. */
    private FlufficonBook book;
    private boolean showRocket;
    /** Who has added their piece to the rocket; the rocket isn't there at all before the first win. */
    private final EnumSet<Hero> rocketBuilders = EnumSet.noneOf(Hero.class);
    private boolean rocketThere;
    /** The rocket game console in front of the TV: there once the rocket has been up to the Moon. */
    private final boolean consoleThere;
    /** Egor's skull on the floor: there once the rocket is built - it switches hard mode on and off. */
    private final boolean skullThere = Unlocks.skullThere();
    /** The rocket's piece, to puff smoke from its engine once it's finished. */
    private Piece rocket;
    private RunSave saved;
    /** The Fluffy King lives here now (all four crowned), and his throne pokes in from the left. */
    private final boolean kingHere = Hero.KING.unlocked();
    /** In the King's room, not the living room. */
    private boolean inKingRoom;
    /** The King's room's furniture: his throne's foot, the potion's table. */
    private final Array<Rectangle> kingSolids = new Array<>();
    /** The King on his throne, full size and smiling (drawn while someone else is played). */
    private final FluffyKing bigKing = new FluffyKing(KING_X, KING_Y, 0);
    /** The "moves in" card is up (the first time he's here). */
    private boolean movingIn;
    private final Rectangle tmpRect = new Rectangle();
    private final Rectangle tmpRect2 = new Rectangle();

    public HubScreen(ETGFanfic game) {
        this.game = game;
        float u = 1f / Sprites.PPU;
        // free-standing furniture, placed by its top-left pixel in the 224x160 room
        addPiece("sofa", 74, 118, 0.15f, 0.55f);
        // the King's throne needs the corner by the bookshelf: granny's armchair sits a little lower then
        addPiece("armchair", 14, kingHere ? 104 : 96, 0.1f, 0.45f);
        if (kingHere) {
            bigKing.showHappy();
            movingIn = !game.prefs.getBoolean(KING_MOVED_IN, false);
            float tw = Sprites.w(game.sprites.home("throne")) * THRONE_SCALE;
            kingSolids.add(new Rectangle(KING_X - tw / 2f + 0.2f, THRONE_Y, tw - 0.4f, 0.4f));
            kingSolids.add(new Rectangle(POTION_X - 0.2f, POTION_Y, 0.4f, 0.15f));
        }
        addPiece("lamp", 158, 96, 0.2f, 0.12f);
        addPiece("plant", 82, 18, 0.05f, 0.25f);
        // against the wall, baked into the background
        solids.add(new Rectangle(4 * u, (160 - 54) * u, 34 * u, 54 * u)); // bookshelf
        solids.add(new Rectangle(168 * u, (160 - 60) * u, 48 * u, 60 * u)); // TV cabinet
        addRocket();
        if (skullThere) { // (you bump into it rather than walk over it)
            solids.add(new Rectangle(Spot.SKULL.x - 0.13f, Spot.SKULL.y, 0.26f, 0.12f));
        }
        consoleThere = Unlocks.boardedRocket || Dev.flag("console");
        if (consoleThere) {
            addPiece("console", 197, 59, 0f, 0f); // (flat on the floor: walked over, not into)
        }
        Vector2 home = home(Hero.current);
        feet.set(home);
        // (the potion's in his room: that's where he's played from; dev builds: -Dfluffy.kingRoom=true starts there)
        inKingRoom = kingHere && (Hero.current == Hero.KING || Dev.flag("kingRoom"));
        if (inKingRoom && Hero.current != Hero.KING) {
            feet.set(POTION_X - 0.6f, 1.4f);
        }
        facingRight = home.x < 3.5f;
        int page = Dev.number("book", -1); // dev builds: -Dfluffy.book=N opens the Flufficon at page N
        if (page >= 0) {
            book = new FlufficonBook(game);
            book.open();
            book.turnTo(page);
        }
    }

    /** Where each of the family stands in the room while someone else is being played. */
    private static Vector2 home(Hero h) {
        switch (h) {
            case KING:
                return new Vector2(POTION_X, POTION_Y - 0.2f); // (in his room: his potion - he sits on his throne)
            case SISTER:
                return new Vector2(1.9f, 3.3f);   // by the window
            case GRANNY:
                return new Vector2(1.75f, 1.55f); // beside her armchair
            case GRANDAD:
                return new Vector2(5.85f, 1.95f); // by the lamp, near the TV (the rocket's in the corner)
            case BABY:
                return new Vector2(2.75f, 2.05f); // playing on the edge of the rug
            default:
                return new Vector2(4.2f, 2.35f);  // on the rug
        }
    }

    /** The feet of the family members standing about: you can't walk through them. */
    private boolean bumpsIntoFamily(Rectangle r) {
        for (Hero h : Hero.values()) {
            if (h == Hero.current || h == Hero.KING) continue; // (the King's on his throne: that's solid)
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
        if (footH > 0) {
            solids.add(solid);
        }
        return p;
    }

    /**
     * The cardboard rocket in the bottom-right corner: the box once the King has been beaten at all (also
     * by wins from before the family's pieces were remembered), then each builder's piece on top
     * ({@link Unlocks#rocketBuilders}, which dev {@code -Dfluffy.rocket=N} can override).
     */
    private void addRocket() {
        rocketBuilders.addAll(Unlocks.rocketBuilders());
        rocketThere = !rocketBuilders.isEmpty() || Dev.number("rocket", -1) < 0 && game.getWins() > 0;
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
        return rocketBuilders.size() == Hero.rocketCrew();
    }

    @Override
    public void show() {
        music = Gdx.audio.newMusic(Gdx.files.internal("assets/music/home.ogg")); // "Home Sweet Fluff"
        music.setLooping(true);
        musicFade = 0f;
        music.setVolume(0f);
        music.play();
        saved = RunSave.read(game.prefs);
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1 / 30f);
        time += delta;
        musicFade = Math.min(1f, musicFade + delta / MUSIC_FADE_IN);
        music.setVolume(MUSIC_VOLUME * game.getMusicVolume() * musicFade);
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
        if (book != null && book.isOpen()) {
            book.update(delta);
            return;
        }
        if (showRocket) {
            if (anyConfirm() || back()) {
                showRocket = false;
            }
            return;
        }
        if (movingIn) {
            if (anyConfirm() || back()) {
                movingIn = false;
                game.prefs.putBoolean(KING_MOVED_IN, true);
                game.prefs.flush();
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
            if (inKingRoom || s == Spot.ROCKET && !rocketThere || s == Spot.CONSOLE && !consoleThere || s == Spot.SKULL && !skullThere) continue;
            float d = feet.dst(s.x, s.y);
            if (d < best) {
                best = d;
                near = s;
            }
        }
        for (Hero h : Hero.values()) {
            if (!atHome(h)) continue;
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
                ? "Right, where's my coat?" : h == Hero.BABY ? "Goo goo! Ga!" : h == Hero.KING
                ? "*gulp* Ahem. Royally small!" : "Let's go!");
    }

    /**
     * Stands about the room we're in while someone else is played: the family in the living room, the King
     * (once he's moved in) in his own.
     */
    private boolean atHome(Hero h) {
        return h != Hero.current && (h == Hero.KING ? kingHere && inKingRoom : !inKingRoom);
    }

    /** "PLAY AS ...?" - or, for the King, his shrinking potion. */
    private static String playAs(Hero h) {
        return h == Hero.KING ? "DRINK THE SHRINKING POTION" : "PLAY AS " + h.title;
    }

    /** Moves the feet by (dx, dy) unless that walks into a wall or furniture. */
    private void step(float dx, float dy) {
        float nx = feet.x + dx;
        float ny = feet.y + dy;
        tmpRect.set(nx - FEET_W / 2f, ny, FEET_W, FEET_H);
        boolean inDoorway = kingHere && ny > DOOR_BOTTOM && ny + FEET_H < DOOR_TOP;
        if (inDoorway && !inKingRoom && tmpRect.x < FLOOR.x) { // through to the King's room...
            inKingRoom = true;
            feet.set(FLOOR.x + FLOOR.width - FEET_W, ny);
            game.sfx.play("door", 0.4f);
            return;
        }
        if (inDoorway && inKingRoom && tmpRect.x + tmpRect.width > FLOOR.x + FLOOR.width) { // ...and back
            inKingRoom = false;
            feet.set(FLOOR.x + FEET_W, ny);
            game.sfx.play("door", 0.4f);
            return;
        }
        if (!FLOOR.contains(tmpRect)) {
            return;
        }
        for (Rectangle r : inKingRoom ? kingSolids : solids) {
            if (r.overlaps(tmpRect)) {
                return;
            }
        }
        if (!inKingRoom && bumpsIntoFamily(tmpRect)) {
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
            if (book == null) {
                book = new FlufficonBook(game);
            }
            book.open();
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
            return playAs(askingHero) + "?";
        }
        switch (asking) {
            case DOOR:
                return saved != null ? "CONTINUE THE TRIP?" : "GO OUTSIDE?";
            case TV:
                return "PLAY ENDLESS?";
            case CONSOLE:
                return "PLAY THE ROCKET GAME?";
            case SKULL:
                return Unlocks.hardMode ? "TURN HARD MODE OFF?" : "ACTIVATE HARD MODE?";
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
                    leave(GameScreen.nextFloor(game, saved));
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
            case CONSOLE:
                if (chosen == 0) {
                    leave(new RocketGameScreen(game));
                }
                break;
            case SKULL:
                if (chosen == 0) {
                    Unlocks.setHardMode(!Unlocks.hardMode, game.prefs);
                    game.sfx.play(Unlocks.hardMode ? "boss" : "heal", 0.6f);
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
        if (inKingRoom) {
            drawKingRoom(b, d);
            return;
        }
        TextureRegion room = game.sprites.home("room");
        b.draw(room, 0, 0, Sprites.w(room), Sprites.h(room));
        drawTv(b, d);
        if (kingHere) { // the royal carpet out to the King's room
            d.rect(b, 0, DOOR_BOTTOM - 0.05f, 1.1f, DOOR_TOP - DOOR_BOTTOM + 0.1f, CARPET);
            d.rect(b, 0, DOOR_BOTTOM - 0.05f, 1.1f, 0.06f, CARPET_TRIM);
            d.rect(b, 0, DOOR_TOP - 0.01f, 1.1f, 0.06f, CARPET_TRIM);
            d.rect(b, 0, DOOR_BOTTOM - 0.15f, 0.1f, DOOR_TOP - DOOR_BOTTOM + 0.3f, Color.valueOf("5a3a4a"));
        }

        // the hero and the free-standing furniture, back to front
        Array<Object> order = new Array<>();
        order.addAll(pieces);
        order.add(this);
        for (Hero h : Hero.values()) {
            if (atHome(h) && h != Hero.KING) order.add(h); // (the King is drawn on his throne)
        }
        order.add(cat);
        if (skullThere) {
            order.add(Spot.SKULL);
        }
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
            } else if (o == Spot.SKULL) {
                drawSkull(b, d);
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
        if (o instanceof Spot) return ((Spot) o).y;
        return ((Piece) o).y;
    }

    /**
     * The King's room: his big throne on its rug, him on it full size and smiling (empty while he's being
     * played), his shrinking potion on a little gold table - and whoever's playing, in front or behind.
     */
    private void drawKingRoom(SpriteBatch b, Draw d) {
        TextureRegion room = game.sprites.home("king_room");
        b.draw(room, 0, 0, Sprites.w(room), Sprites.h(room));
        boolean behind = feet.y > THRONE_Y + 0.3f;
        if (behind) {
            drawPlayerHere(b, d);
        }
        TextureRegion throne = game.sprites.home("throne");
        float tw = Sprites.w(throne) * THRONE_SCALE;
        b.draw(throne, KING_X - tw / 2f, THRONE_Y, tw, Sprites.h(throne) * THRONE_SCALE);
        if (Hero.current != Hero.KING) {
            bigKing.showScale(1f);
            bigKing.pos.set(KING_X, KING_Y + MathUtils.sin(time * 1.5f) * 0.02f);
            bigKing.drawPortrait(b, d, feet.x < KING_X ? 180f : 0f);
        }
        d.ellipse(b, POTION_X, POTION_Y + 0.02f, 0.24f, 0.06f, Color.BLACK, 0.3f); // the little gold table
        d.rect(b, POTION_X - 0.03f, POTION_Y, 0.06f, 0.3f, CARPET_TRIM);
        d.ellipse(b, POTION_X, POTION_Y + 0.3f, 0.22f, 0.07f, CARPET_TRIM, 1f);
        if (Hero.current != Hero.KING) { // (he's drunk it)
            drawPotion(b, d, POTION_X, POTION_Y + 0.3f, 1f);
        }
        if (!behind) {
            drawPlayerHere(b, d);
        }
    }

    private void drawPlayerHere(SpriteBatch b, Draw d) {
        drawHero(b, d, Hero.current, feet.x, feet.y, facingRight, facingUp, walking);
        if (Hero.current == Hero.GRANNY) { // her cat comes along
            cat.draw(b, d);
        }
    }

    /** A round bottle of purple shrinking potion, bubbling. */
    private void drawPotion(SpriteBatch b, Draw d, float x, float y, float s) {
        d.ellipse(b, x, y, 0.09f * s, 0.03f * s, Color.BLACK, 0.3f);
        d.circle(b, x, y + 0.08f * s, 0.085f * s, Draw.OUTLINE_COLOR);
        d.circle(b, x, y + 0.08f * s, 0.07f * s, GLASS);
        d.circle(b, x, y + 0.065f * s, 0.055f * s, POTION);
        d.rect(b, x - 0.03f * s, y + 0.15f * s, 0.06f * s, 0.07f * s, Draw.OUTLINE_COLOR);
        d.rect(b, x - 0.02f * s, y + 0.15f * s, 0.04f * s, 0.06f * s, GLASS);
        d.rect(b, x - 0.025f * s, y + 0.21f * s, 0.05f * s, 0.03f * s, Color.valueOf("9a6a3a")); // the cork
        float t = (time * 0.8f) % 1f; // a bubble rising out of it
        d.circle(b, x + 0.02f * s, y + (0.24f + 0.25f * t) * s, 0.015f * s, POTION, 1f - t);
    }

    /** The first time the King is home: a card - he's moved in, and how to play as him. */
    private void drawMovingIn(SpriteBatch b, Draw d, float cx) {
        d.rect(b, 0, 0, 7, 5, Color.BLACK, 0.6f);
        panel(b, d, cx, 2.5f, 6.4f, 4.3f);
        game.text("THE FLUFFY KING MOVES IN!", cx, 4.35f, 0.5f, GOLD, Align.center);
        bigKing.showScale(1f);
        bigKing.pos.set(cx, 3.05f + MathUtils.sin(time * 2f) * 0.03f);
        bigKing.drawPortrait(b, d, 0f);
        drawPotion(b, d, cx + 1.05f, 2.25f, 1.6f);
        game.text("Four golden crowns in one family - he wants to belong too!", cx, 1.75f, 0.24f, TEXT, Align.center);
        game.text("His room is through the left side - throne and all.", cx, 1.42f, 0.24f, TEXT, Align.center);
        game.text("Drink his shrinking potion to play as him.", cx, 1.09f, 0.24f, TEXT, Align.center);
        game.text(game.pad.isActive() ? "A  OK" : "E  OK", cx, 0.6f, 0.26f, DIM, Align.center);
    }

    /** Egor's skull, sitting on the floor: its eyes glow red while hard mode is on. */
    private void drawSkull(SpriteBatch b, Draw d) {
        float x = Spot.SKULL.x;
        float y = Spot.SKULL.y;
        boolean on = Unlocks.hardMode;
        if (on) {
            d.circle(b, x, y + 0.16f, 0.26f + 0.02f * MathUtils.sin(time * 4f), HARD_GLOW, 0.25f);
        }
        d.ellipse(b, x, y, 0.15f, 0.04f, Color.BLACK, 0.3f);
        d.circle(b, x, y + 0.19f, 0.14f, BONE);
        d.rect(b, x - 0.085f, y + 0.02f, 0.17f, 0.12f, BONE);
        d.rect(b, x + 0.05f, y + 0.02f, 0.035f, 0.17f, BONE_SH);
        for (int i = -1; i <= 1; i += 2) {
            d.circle(b, x + i * 0.055f, y + 0.17f, 0.04f, on ? HARD_GLOW : Color.BLACK, on ? 0.6f + 0.4f * MathUtils.sin(time * 6f) : 0.85f);
        }
        d.rect(b, x - 0.012f, y + 0.09f, 0.024f, 0.03f, Color.BLACK, 0.85f); // the nose
        for (int i = -1; i <= 1; i++) {
            d.rect(b, x + i * 0.04f - 0.006f, y + 0.02f, 0.012f, 0.05f, BONE_SH); // teeth
        }
    }

    /** The old TV is on: a flickering blue picture with something moving about on it. */
    private void drawTv(SpriteBatch b, Draw d) {
        float u = 1f / Sprites.PPU;
        float x = 175 * u;
        float y = (160 - 31) * u;
        float w = 30 * u;
        float h = 22 * u;
        float flicker = 0.75f + 0.1f * MathUtils.sin(time * 23f) + 0.05f * MathUtils.sin(time * 61f);
        if (near == Spot.CONSOLE || asking == Spot.CONSOLE) { // the rocket game's title screen: stars and the rocket
            d.rect(b, x, y, w, h, Color.BLACK, 0.85f);
            for (int i = 0; i < 6; i++) {
                float sy = y + ((i * 0.31f - time * 0.4f) % 1f + 1f) % 1f * h;
                d.rect(b, x + ((i * 0.43f) % 1f) * w, sy, 0.02f, 0.02f, Color.WHITE, 0.8f);
            }
            float rx = x + w / 2f + MathUtils.sin(time * 1.5f) * w * 0.25f;
            d.rect(b, rx - 0.03f, y + h * 0.25f, 0.06f, 0.14f, GOLD);
            d.circle(b, rx, y + h * 0.2f, 0.03f, Color.ORANGE, 0.8f);
            d.rect(b, x, y + h * ((time * 0.7f) % 1f), w, 0.02f, Color.WHITE, 0.15f);
            return;
        }
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
        int col = walks ? ((int) (t * 10f)) % 4 : 4 + ((int) (t * 2f)) % 2;
        TextureRegion r = frames[row][col];
        float w = Sprites.w(r);
        float h = Sprites.h(r);
        d.ellipse(b, x, y + 0.03f, 0.2f, 0.06f, Color.BLACK, 0.35f);
        b.draw(r.getTexture(), x - w / 2f, y, w / 2f, 0, w, h, 1f, 1f, 0f,
                r.getRegionX(), r.getRegionY(), r.getRegionWidth(), r.getRegionHeight(), !right, false);
        if (Unlocks.wearsCrown(who)) { // hard mode's golden skull crown
            Crown.draw(b, d, who, x, y, col, !up);
        }
    }

    /** Where someone's head is, for their speech bubble. */
    private Vector2 headOf(Hero h) {
        if (h == Hero.KING && h != Hero.current) {
            return new Vector2(KING_X + 0.5f, KING_Y + 0.75f);
        }
        return h == Hero.current ? new Vector2(feet.x, feet.y + 1f) : home(h).add(0, 1f);
    }

    private void drawOverlay(SpriteBatch b) {
        Draw d = game.draw;
        float cx = game.viewport.getWorldWidth() / 2f;
        boolean pad = game.pad.isActive();
        if (book != null && book.isOpen()) {
            book.draw(b, d);
            return;
        }
        if (movingIn) {
            drawMovingIn(b, d, cx);
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
            if (asking == Spot.CONSOLE) {
                game.text(RocketGameScreen.CREDIT, cx, 2.72f, 0.24f, DIM, Align.center);
            }
            if (asking == Spot.SKULL) { // (the baby always plays it easy)
                game.text(Hero.current.easy() ? "(not for the baby - he plays it easy)"
                        : "Tougher, faster, more enemies", cx, 2.72f, 0.24f, DIM, Align.center);
            }
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
            String what = near != null ? near.prompt : nearHero.unlocked() ? playAs(nearHero) : "TALK";
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
            if (!h.buildsRocket()) continue; // (the baby's too little to help)
            boolean done = rocketBuilders.contains(h);
            game.text(h.title + ": " + (done ? rocketPieceName(h) : "?"), cx, y, 0.26f, done ? TEXT : DIM, Align.center);
            y -= 0.28f;
        }
        int left = Hero.rocketCrew() - rocketBuilders.size();
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
