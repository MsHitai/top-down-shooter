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
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.trush.game.enemy.BulletKin;
import com.trush.game.enemy.Enemy;
import com.trush.game.enemy.FluffyKing;
import com.trush.game.enemy.Haymaker;
import com.trush.game.enemy.Hydrant;
import com.trush.game.enemy.ShellCop;
import com.trush.game.enemy.ShotgunKin;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.RoomRenderer;
import com.trush.game.gfx.Sprites;
import com.trush.game.input.Gamepad.Button;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Bullet;
import com.trush.game.objects.Prop;
import com.trush.game.player.Hero;
import com.trush.game.world.Arena;
import com.trush.game.world.Room;
import com.trush.game.world.Stage;

/**
 * The opening story, told like a picture book before the first run: a page at a time, each with a
 * little scene drawn from the game's own art and the text typed out underneath. Any key, click or
 * A turns the page (or finishes the typing); Esc / B / Start skips the lot. Then {@code next} runs.
 */
public class IntroScreen implements Screen {

    /** Seen at least once (the menu then offers it as "THE STORY" instead of playing it every run). */
    public static final String SEEN_PREF = "seenIntro";

    private static final String[] PAGES = {
            "Long, long ago, in the Fluffy Kingdom, everything was soft. The clouds were soft. "
                    + "The hills were soft. Even the rocks were a little bit squishy.",
            "Then, one terrible morning, the Fluffy King got out of bed on the wrong side. "
                    + "So he tried the other side. That was wrong too. He became VERY, VERY ANGRY.",
            "His bad mood spread across the land like a cold. Nobody could stand anybody. "
                    + "So everyone did the only sensible thing: they started shooting at one another.",
            "The strongest took over everything. The cities. The towns. Even the village. "
                    + "Yes - even the village. Even the hay bales were armed.",
            "But in that village lived one ordinary country boy. He looked at the chaos. "
                    + "He picked up a rusty old sidearm. And he decided...",
            "...that it was about time it all",
    };
    /** Characters per line at the story's text size (the pixel font is monospaced). */
    private static final int LINE_CHARS = 52;

    private static final Color TEXT = Color.valueOf("f5f0e6");
    private static final Color DIM = Color.valueOf("8a8079");
    private static final Color FRAME = Color.valueOf("2a1e24");
    private static final Color SKY = Color.valueOf("9ad8f5");
    private static final Color STORM = Color.valueOf("4d405d");
    private static final Color SMOKE = Color.valueOf("b57e8d");
    private static final Color BULLET = Color.valueOf("ff634a");
    private static final Color RED = Color.valueOf("ff3b3b");

    /** The picture sits in this box (screen units: the view is 7 x 5). */
    private static final float PX = 0.5f;
    private static final float PY = 1.75f;
    private static final float PW = 6f;
    private static final float PH = 3.0f;
    /** Letters a second - slow enough to read along as it types. */
    private static final float TYPE_SPEED = 22f;
    /** A breath after a sentence ends, and a shorter one after a comma. */
    private static final float SENTENCE_PAUSE = 0.45f;
    private static final float COMMA_PAUSE = 0.18f;

    private final ETGFanfic game;
    private final Runnable next;
    private Music music;
    private int page;
    private float typed;
    private float pause;
    private float pageTime;
    private float time;
    private final Color tmp = new Color();
    private final Matrix4 saved = new Matrix4();
    private final Matrix4 placed = new Matrix4();
    private final RoomRenderer rooms = new RoomRenderer();
    private final Room cloudgate = backdrop(Stage.CLOUDS, Stage.CLOUDS.startTheme);
    private final Room throne = backdrop(Stage.CLOUDS, Stage.CLOUDS.bossTheme);
    private final Room street = backdrop(Stage.TOWN, Stage.TOWN.startTheme);
    private final Room barn = backdrop(Stage.VILLAGE, "barn");
    private final Room yard = backdrop(Stage.VILLAGE, Stage.VILLAGE.startTheme);
    private final Prop[] clouds = {
            new Prop(Prop.Kind.CLOUD, 1.5f, 1.4f), new Prop(Prop.Kind.COLUMN, 2.65f, 2.25f),
            new Prop(Prop.Kind.COLUMN, 4.35f, 2.25f), new Prop(Prop.Kind.CLOUD, 5.5f, 0.9f)
    };
    private final Prop[] streetProps = {
            new Prop(Prop.Kind.LAMP, 0.85f, 2.4f), new Prop(Prop.Kind.BENCH, 3.6f, 2.3f),
            new Prop(Prop.Kind.LAMP, 6.1f, 2.4f)
    };
    private final Prop[] hay = {
            new Prop(Prop.Kind.HAY, 1.5f, 1.2f), new Prop(Prop.Kind.HAY, 5.5f, 1.2f)
    };
    private final Prop[] yardProps = {
            new Prop(Prop.Kind.HAY, 1.2f, 1.9f), new Prop(Prop.Kind.CRATE, 5.75f, 2.25f),
            new Prop(Prop.Kind.BARREL, 6.2f, 1.9f)
    };
    private final FluffyKing kingAtPeace = new FluffyKing(0, 0, 0);
    private final FluffyKing king = new FluffyKing(0, 0, 0);
    private final Enemy[] mob = {new BulletKin(0, 0), new ShotgunKin(0, 0), new ShellCop(0, 0),
            new Hydrant(0, 0)};
    private final Haymaker boss = new Haymaker(0, 0, 0);
    private final String starterSprite = Hero.BOY.starter().getSprite();
    private final Bullet[] crossfire = new Bullet[12];

    public IntroScreen(ETGFanfic game, Runnable next) {
        this.game = game;
        this.next = next;
        kingAtPeace.showHappy();
        for (int i = 0; i < crossfire.length; i++) {
            crossfire[i] = new Bullet(false, 0f, 0f, i % 2 == 0 ? 0f : 180f, 1f, 0.07f, 0f, BULLET);
        }
    }

    @Override
    public void show() {
        music = Gdx.audio.newMusic(Gdx.files.internal("assets/music/menu.ogg"));
        music.setLooping(true);
        music.setVolume(.4f * game.getMusicVolume());
        music.play();
        game.prefs.putBoolean(SEEN_PREF, true);
        game.prefs.flush();
    }

    /** Types the page out letter by letter, pausing a moment after each comma and sentence. */
    private void typeText(float delta) {
        if (pause > 0) {
            pause -= delta;
            return;
        }
        String text = PAGES[page];
        int before = (int) typed;
        typed += delta * TYPE_SPEED;
        int after = Math.min((int) typed, text.length());
        for (int i = before; i < after; i++) {
            char ch = text.charAt(i);
            boolean end = i + 1 >= text.length() || text.charAt(i + 1) == ' ';
            if (end && (ch == '.' || ch == '!' || ch == '?' || ch == ':')) {
                pause = SENTENCE_PAUSE;
            } else if (end && ch == ',') {
                pause = COMMA_PAUSE;
            }
            if (pause > 0) {
                typed = i + 1; // stop right after the punctuation
                return;
            }
        }
    }

    private int pageLength() {
        return PAGES[page].length();
    }

    /** Splits a page into lines of at most LINE_CHARS, breaking between words. */
    private static Array<String> wrap(String text) {
        Array<String> lines = new Array<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > LINE_CHARS) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        lines.add(line.toString());
        return lines;
    }

    @Override
    public void render(float delta) {
        time += delta;
        pageTime += delta;
        typeText(delta);
        if (skipPressed()) {
            finish();
            return;
        }
        if (pageTime > 0.25f && advancePressed()) {
            if (typed < pageLength()) {
                typed = pageLength(); // show the whole page at once
                pause = 0;
            } else if (page < PAGES.length - 1) {
                page++;
                typed = 0;
                pause = 0;
                pageTime = 0;
            } else {
                finish();
                return;
            }
        }

        ScreenUtils.clear(Color.BLACK);
        OrthographicCamera cam = (OrthographicCamera) game.viewport.getCamera();
        cam.position.set(game.viewport.getWorldWidth() / 2f, game.viewport.getWorldHeight() / 2f, 0);
        cam.update();
        game.viewport.apply();
        SpriteBatch b = game.batch;
        b.setProjectionMatrix(cam.combined);
        b.begin();
        drawPicture(b, game.draw);
        drawText();
        b.end();
    }

    private boolean advancePressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.justTouched() || game.pad.justPressed(Button.A);
    }

    private boolean skipPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || game.pad.justPressed(Button.B)
                || game.pad.justPressed(Button.START);
    }

    private void finish() {
        if (music != null) {
            music.stop();
        }
        next.run();
        dispose();
    }

    // ------------------------------------------------------------ text

    private void drawText() {
        float cx = 3.5f;
        int left = (int) typed;
        float y = 1.5f;
        Array<String> lines = wrap(PAGES[page]);
        for (String line : lines) { // typed out line by line
            if (left <= 0) break;
            game.text(line.substring(0, Math.min(line.length(), left)), cx, y, 0.34f, TEXT, Align.center);
            left -= line.length() + 1;
            y -= 0.23f;
        }
        if (page == PAGES.length - 1 && typed >= pageLength()) {
            // the last word, shouted, shaking
            float k = Math.min(1f, (typed - pageLength()) / 6f);
            float shake = 0.03f * (1f - k * 0.5f);
            game.text("ENDED!", cx + MathUtils.random(-shake, shake), 1.2f + MathUtils.random(-shake, shake),
                    0.6f + 0.6f * (1f - k), RED, Align.center);
        }
        boolean done = typed >= pageLength();
        String hint = game.pad.isActive() ? (done ? "A - next" : "A - faster") + "    B - skip"
                : (done ? "CLICK / SPACE - next" : "CLICK / SPACE - faster") + "    ESC - skip";
        game.text(hint, cx, 0.35f, 0.24f, DIM, Align.center);
        for (int i = 0; i < PAGES.length; i++) { // page dots
            game.draw.circle(game.batch, cx - 0.3f + i * 0.12f, 0.55f, 0.025f, i == page ? TEXT : DIM);
        }
    }

    // ------------------------------------------------------------ pictures

    private void drawPicture(SpriteBatch b, Draw d) {
        d.rect(b, PX - 0.06f, PY - 0.06f, PW + 0.12f, PH + 0.12f, FRAME);
        switch (page) {
            case 0:
                drawSoftKingdom(b, d);
                break;
            case 1:
                drawAngryKing(b, d);
                break;
            case 2:
                drawShootout(b, d);
                break;
            case 3:
                drawVillageTaken(b, d);
                break;
            case 4:
                drawHero(b, d);
                break;
            default:
                drawEnded(b, d);
                break;
        }
        // whatever spilled over the edges (hills, clouds) is cut off by a black mask, then the frame
        float f = 0.06f;
        d.rect(b, 0, 0, 7f, PY - f, Color.BLACK);
        d.rect(b, 0, PY + PH + f, 7f, 5f, Color.BLACK);
        d.rect(b, 0, 0, PX - f, 5f, Color.BLACK);
        d.rect(b, PX + PW + f, 0, 2f, 5f, Color.BLACK);
        d.rect(b, PX - f, PY - f, PW + 2 * f, f, FRAME);
        d.rect(b, PX - f, PY + PH, PW + 2 * f, f, FRAME);
        d.rect(b, PX - f, PY, f, PH, FRAME);
        d.rect(b, PX + PW, PY, f, PH, FRAME);
    }

    /** A wide, shallow room fits the picture without stretching the tiles or the actors. */
    private static Room backdrop(Stage stage, String theme) {
        Room room = new Room(0, 0, Room.Type.ARENA);
        room.arena = new Arena(Arena.SMALL_COLS, 5);
        room.stage = stage;
        room.theme = theme;
        return room;
    }

    private void beginRoom(SpriteBatch b, Room room, Prop... props) {
        float scale = PW / room.arena.worldW;
        b.setTransformMatrix(placed.idt().translate(PX, PY, 0).scale(scale, scale, 1));
        rooms.drawFloor(b, room);
        for (Prop prop : props) {
            prop.draw(b, game.sprites);
        }
    }

    /** Draws an enemy's gameplay portrait larger than life, within the room's own coordinates. */
    private void portrait(SpriteBatch b, Draw d, Enemy enemy, float x, float y, float scale, float aim) {
        saved.set(b.getTransformMatrix());
        b.setTransformMatrix(placed.set(saved).translate(x, y, 0).scale(scale, scale, 1));
        enemy.drawPortrait(b, d, aim);
        b.setTransformMatrix(saved);
    }

    private void sky(SpriteBatch b, Draw d, Color color) {
        d.rect(b, PX, PY, PW, PH, color);
        for (int i = 0; i < 4; i++) {
            float x = PX + ((i * 1.7f + time * 0.15f) % (PW + 1f)) - 0.4f;
            float y = PY + 2.2f + (i % 2) * 0.4f;
            d.circle(b, x, y, 0.28f, Color.WHITE, 0.75f);
            d.circle(b, x + 0.24f, y - 0.05f, 0.19f, Color.WHITE, 0.75f);
        }
    }

    /** Page 1: the gates of the Fluffy Kingdom, with soft cloud banks and marble columns. */
    private void drawSoftKingdom(SpriteBatch b, Draw d) {
        sky(b, d, SKY);
        beginRoom(b, cloudgate, clouds);
        portrait(b, d, kingAtPeace, cloudgate.arena.centerX, 1.38f, 0.75f, 270f);
        b.setTransformMatrix(placed.idt());
    }

    /** Page 2: the King fumes in the real throne room. */
    private void drawAngryKing(SpriteBatch b, Draw d) {
        sky(b, d, STORM);
        beginRoom(b, throne);
        d.rect(b, 0f, 0f, throne.arena.worldW, throne.arena.worldH, STORM, 0.35f);
        float x = throne.arena.centerX + MathUtils.sin(time * 31f) * 0.025f;
        float y = 1.3f + MathUtils.sin(time * 3f) * 0.025f;
        for (int i = 0; i < 8; i++) {
            float k = (time * 0.75f + i / 8f) % 1f;
            float side = i % 2 == 0 ? -1f : 1f;
            d.circle(b, x + side * (0.96f + k * 0.65f), y + 0.38f + k * 1.1f, 0.06f + k * 0.2f,
                    SMOKE, 0.9f * (1f - k));
        }
        portrait(b, d, king, x, y, 1.4f, 270f);
        b.setTransformMatrix(placed.idt());
    }

    /** Page 3: town and village enemies trade shots in the street. */
    private void drawShootout(SpriteBatch b, Draw d) {
        beginRoom(b, street, streetProps);
        portrait(b, d, mob[0], 1.4f, 2.03f, 1.35f, 344f);
        portrait(b, d, mob[1], 5.5f, 1.95f, 1.35f, 198f);
        portrait(b, d, mob[2], 2.5f, 0.82f, 1.35f, 18f);
        portrait(b, d, mob[3], 4.8f, 0.82f, 1.25f, 166f);
        for (int i = 0; i < crossfire.length; i++) {
            float k = (time * 0.75f + i * 0.37f) % 1f;
            boolean right = i % 2 == 0;
            float x = 1.7f + (right ? k : 1f - k) * 3.7f;
            float y = 0.85f + (i % 4) * 0.34f + MathUtils.sin(k * 6f + i) * 0.08f;
            crossfire[i].pos.set(x, y);
            crossfire[i].draw(b, d);
        }
        b.setTransformMatrix(placed.idt());
    }

    /** Page 4: the Haymaker and two armed hay bales take over the village barn. */
    private void drawVillageTaken(SpriteBatch b, Draw d) {
        beginRoom(b, barn, hay);
        game.sprites.gun("fluff_shotgun").drawIcon(b, 1.5f, 1.72f, 1.5f);
        game.sprites.gun("pocket_uzi").drawIcon(b, 5.5f, 1.72f, 1.5f);
        portrait(b, d, boss, barn.arena.centerX, 1.48f + MathUtils.sin(time * 2f) * 0.025f, 1.3f, 270f);
        b.setTransformMatrix(placed.idt());
    }

    /** Page 5: a close-up of the boy and his starter weapon in the village yard. */
    private void drawHero(SpriteBatch b, Draw d) {
        beginRoom(b, yard, yardProps);
        TextureRegion hero = game.sprites.hero(Hero.BOY.sheet)[0][4 + (int) (time * 2f) % 2];
        float scale = 2.5f;
        float w = Sprites.heroW(hero) * scale;
        float h = Sprites.heroH(hero) * scale;
        float x = 3.3f;
        float y = 0.65f;
        d.ellipse(b, x, y + 0.04f, 0.45f, 0.12f, Color.BLACK, 0.35f);
        b.draw(hero, x - w / 2f, y, w, h);
        saved.set(b.getTransformMatrix());
        b.setTransformMatrix(placed.set(saved).translate(x + 0.32f, y + h * 0.43f, 0).scale(scale, scale, 1));
        game.sprites.gun(starterSprite).drawHeld(b, 0f, 0f, 5f, false);
        b.setTransformMatrix(placed.idt());
    }

    /** Page 6: black, with the title slamming in behind the shout. */
    private void drawEnded(SpriteBatch b, Draw d) {
        d.rect(b, PX, PY, PW, PH, Color.BLACK);
        float k = MathUtils.clamp((pageTime - 1.2f) / 0.6f, 0f, 1f);
        if (k > 0) {
            String title = "ENTER THE FLUFFY!";
            float scale = Math.min(1f + 0.4f * (1f - k), (PW - 0.5f) / game.textWidth(title, 1f));
            game.text(title, 3.5f, PY + PH / 2f + 0.25f, scale, tmp.set(Color.CORAL).mul(1, 1, 1, k), Align.center);
        }
    }

    @Override
    public void resize(int width, int height) {
        game.viewport.update(width, height);
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
        rooms.dispose();
        if (music != null) {
            music.dispose();
            music = null;
        }
    }
}
