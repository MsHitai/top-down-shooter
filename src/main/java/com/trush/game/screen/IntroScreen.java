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
import com.trush.game.enemy.Hydrant;
import com.trush.game.enemy.ShellCop;
import com.trush.game.enemy.ShotgunKin;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;
import com.trush.game.input.Gamepad.Button;
import com.trush.game.launcher.ETGFanfic;

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
    private static final Color SKY_LOW = Color.valueOf("ffd0e4");
    private static final Color ANGRY_SKY = Color.valueOf("5a1e2a");
    private static final Color ANGRY_LOW = Color.valueOf("c0392b");
    private static final Color GRASS = Color.valueOf("6aa84a");
    private static final Color GRASS_DARK = Color.valueOf("4f8a3a");
    private static final Color HILL_PINK = Color.valueOf("f2b8d0");
    private static final Color CASTLE = Color.valueOf("ffc6dc");
    private static final Color CASTLE_DARK = Color.valueOf("e08ab0");
    private static final Color ROOF = Color.valueOf("8a5ab0");
    private static final Color SUN = Color.valueOf("ffd23a");
    private static final Color SMOKE = Color.valueOf("6b6670");
    private static final Color BULLET = Color.valueOf("ff8a3a");
    private static final Color WALL = Color.valueOf("f0e2c0");
    private static final Color THATCH = Color.valueOf("c49a45");
    private static final Color DUSK = Color.valueOf("f2a03a");
    private static final Color DUSK_HIGH = Color.valueOf("5a4a8a");
    private static final Color RED = Color.valueOf("ff3b3b");
    private static final Color DOOR = Color.valueOf("6e4a2e");
    private static final Color SMOKY_SKY = Color.valueOf("7a6a8a");
    private static final Color STREET = Color.valueOf("8a8079");
    private static final Color VILLAGE_SKY = Color.valueOf("8aa0c0");
    private static final Color SMOKY_LOW = Color.valueOf("c9a080");
    private static final Color VILLAGE_LOW = Color.valueOf("e0c8a0");
    private static final Color SUNSET = Color.valueOf("ffd166");

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
    private final FluffyKing king = new FluffyKing(0, 0, 0);
    private final Enemy[] mob = {new BulletKin(0, 0), new ShotgunKin(0, 0), new ShellCop(0, 0), new BulletKin(0, 0),
            new Hydrant(0, 0)};
    private final ShotgunKin boss = new ShotgunKin(0, 0);

    public IntroScreen(ETGFanfic game, Runnable next) {
        this.game = game;
        this.next = next;
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

    private void sky(SpriteBatch b, Draw d, Color high, Color low) {
        int bands = 12;
        for (int i = 0; i < bands; i++) {
            float k = i / (float) (bands - 1);
            d.rect(b, PX, PY + PH * i / bands, PW, PH / bands + 0.01f, tmp.set(low).lerp(high, k));
        }
    }

    private void cloud(SpriteBatch b, Draw d, float x, float y, float s, float alpha) {
        d.circle(b, x, y, s, Color.WHITE, alpha);
        d.circle(b, x + s * 0.9f, y - s * 0.15f, s * 0.75f, Color.WHITE, alpha);
        d.circle(b, x - s * 0.9f, y - s * 0.2f, s * 0.65f, Color.WHITE, alpha);
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

    /** Page 1: a soft pink castle on soft hills under soft clouds and a smiling sun. */
    private void drawSoftKingdom(SpriteBatch b, Draw d) {
        sky(b, d, SKY, SKY_LOW);
        d.circle(b, PX + 5.1f, PY + 2.35f, 0.35f, SUN);
        d.circle(b, PX + 4.98f, PY + 2.42f, 0.04f, FRAME);
        d.circle(b, PX + 5.22f, PY + 2.42f, 0.04f, FRAME);
        d.rotRect(b, PX + 5.05f, PY + 2.26f, 0.12f, 0.03f, -25f, FRAME, 1f); // a smile
        d.rotRect(b, PX + 5.15f, PY + 2.26f, 0.12f, 0.03f, 25f, FRAME, 1f);
        for (int i = 0; i < 4; i++) {
            float x = PX + ((i * 1.7f + time * 0.15f) % (PW + 1f)) - 0.4f;
            cloud(b, d, x, PY + 2.2f + (i % 2) * 0.4f, 0.22f, 0.95f);
        }
        // the castle
        float cx = PX + 3f;
        float base = PY + 0.45f;
        d.rect(b, cx - 0.7f, base, 1.4f, 0.9f, CASTLE);
        d.rect(b, cx - 0.15f, base, 0.3f, 0.4f, CASTLE_DARK);
        for (int i = -1; i <= 1; i++) {
            float tx = cx + i * 0.75f;
            float th = i == 0 ? 1.55f : 1.15f;
            d.rect(b, tx - 0.18f, base, 0.36f, th, CASTLE);
            d.rect(b, tx - 0.18f, base, 0.06f, th, CASTLE_DARK);
            d.shape(b, d.triangle, tx, base + th + 0.12f, 0.24f, 90f, ROOF, 1f);
            d.rect(b, tx - 0.01f, base + th + 0.25f, 0.02f, 0.2f, FRAME);
            d.rect(b, tx + 0.01f, base + th + 0.35f + MathUtils.sin(time * 4f + i) * 0.01f, 0.14f, 0.08f, HILL_PINK);
        }
        // soft rolling hills
        d.circle(b, PX + 1f, PY - 0.9f, 1.8f, GRASS);
        d.circle(b, PX + 5f, PY - 1.1f, 2.1f, GRASS_DARK);
        d.circle(b, PX + 3f, PY - 1.5f, 2.0f, HILL_PINK);
    }

    /** Page 2: the Fluffy King, huge and fuming, under a red sky. */
    private void drawAngryKing(SpriteBatch b, Draw d) {
        sky(b, d, ANGRY_SKY, ANGRY_LOW);
        float shake = pageTime > 1.5f ? 0.02f : 0f;
        float kx = PX + 3f + MathUtils.random(-shake, shake);
        float ky = PY + 1.3f + MathUtils.sin(time * 3f) * 0.03f;
        for (int i = 0; i < 6; i++) { // steam out of his ears
            float k = ((time * 0.7f + i / 6f) % 1f);
            float side = i % 2 == 0 ? -1f : 1f;
            d.circle(b, kx + side * (0.6f + k * 0.5f), ky + 0.9f + k * 0.7f, 0.08f + k * 0.15f, SMOKE, 0.8f * (1f - k));
        }
        portrait(b, d, king, kx, ky, 1.8f, 270f);
        // red in the face, and getting redder
        float flush = 0.12f + 0.1f * MathUtils.sin(time * 5f) + Math.min(0.15f, pageTime * 0.05f);
        d.circle(b, kx, ky + 0.1f, 0.85f, RED, flush);
        if (MathUtils.sin(time * 9f) > 0.6f) { // anger marks
            for (int i = 0; i < 3; i++) {
                d.rotRect(b, kx + 0.8f, ky + 1.1f, 0.18f, 0.04f, i * 60f, RED, 1f);
            }
        }
        if ((int) (time * 2f) % 3 == 0) { // a crack of lightning
            float lx = PX + 0.9f;
            d.rotRect(b, lx, PY + 2.5f, 0.5f, 0.05f, -60f, SUN, 1f);
            d.rotRect(b, lx + 0.15f, PY + 2.1f, 0.4f, 0.05f, -30f, SUN, 1f);
            d.rotRect(b, lx + 0.1f, PY + 1.75f, 0.45f, 0.05f, -65f, SUN, 1f);
        }
    }

    /** Page 3: everybody shooting at everybody. */
    private void drawShootout(SpriteBatch b, Draw d) {
        sky(b, d, SMOKY_SKY, SMOKY_LOW);
        d.rect(b, PX, PY, PW, 0.7f, STREET);
        float[][] spots = {{0.8f, 0.85f, 0f}, {2.2f, 1.5f, 200f}, {3.6f, 0.9f, 170f}, {4.8f, 1.6f, 210f}, {5.4f, 0.75f, 160f}};
        for (int i = 0; i < mob.length; i++) {
            portrait(b, d, mob[i], PX + spots[i][0], PY + spots[i][1], 1.6f, spots[i][2]);
        }
        for (int i = 0; i < 9; i++) { // bullets criss-crossing
            float k = (time * 0.6f + i * 0.37f) % 1f;
            boolean right = i % 2 == 0;
            float x = right ? PX + 0.3f + k * (PW - 0.6f) : PX + PW - 0.3f - k * (PW - 0.6f);
            float y = PY + 0.9f + (i % 4) * 0.42f + MathUtils.sin(k * 6f + i) * 0.05f;
            d.circle(b, x, y, 0.06f, FRAME);
            d.circle(b, x, y, 0.045f, BULLET);
        }
    }

    /** Page 4: the village, taken - a big shotgun bully with a crown on a hay bale, guns on the hay. */
    private void drawVillageTaken(SpriteBatch b, Draw d) {
        sky(b, d, VILLAGE_SKY, VILLAGE_LOW);
        d.rect(b, PX, PY, PW, 1.0f, GRASS);
        for (int i = 0; i < 3; i++) { // cottages
            float hx = PX + 0.7f + i * 2.2f;
            d.rect(b, hx, PY + 0.8f, 0.9f, 0.6f, WALL);
            d.shape(b, d.triangle, hx + 0.45f, PY + 1.6f, 0.55f, 90f, THATCH, 1f);
            d.rect(b, hx + 0.35f, PY + 0.8f, 0.2f, 0.32f, DOOR);
        }
        TextureRegion hay = game.sprites.prop("hay");
        float hw = Sprites.w(hay) * 1.6f;
        float hh = Sprites.h(hay) * 1.6f;
        for (int i = 0; i < 3; i++) {
            float x = PX + 0.6f + i * 2.0f;
            b.draw(hay, x, PY + 0.15f, hw, hh);
            if (i != 1) { // a gun lying on the armed hay
                game.sprites.gun(i == 0 ? "fluff_shotgun" : "pocket_uzi").drawIcon(b, x + hw / 2f, PY + 0.2f + hh, 1.4f);
            }
        }
        float bx = PX + 2.6f + hw / 2f;
        float by = PY + 0.2f + hh + 0.4f;
        portrait(b, d, boss, bx, by, 2f, MathUtils.sin(time) * 30f + 270f);
        // its crown
        float crownY = by + 0.55f;
        d.rect(b, bx - 0.18f, crownY, 0.36f, 0.1f, SUN);
        for (int i = -1; i <= 1; i++) {
            d.shape(b, d.triangle, bx + i * 0.13f, crownY + 0.13f, 0.07f, 90f, SUN, 1f);
        }
    }

    /** Page 5: the country boy at dusk, looking out, a rusty sidearm in his hand. */
    private void drawHero(SpriteBatch b, Draw d) {
        sky(b, d, DUSK_HIGH, DUSK);
        d.circle(b, PX + 4.6f, PY + 0.75f, 0.5f, SUNSET);
        d.rect(b, PX, PY, PW, 0.75f, GRASS_DARK);
        for (int i = 0; i < 7; i++) { // grass blowing in the wind
            float x = PX + 0.3f + i * 0.85f;
            d.rotRect(b, x, PY + 0.78f, 0.03f, 0.14f, MathUtils.sin(time * 3f + i) * 15f, GRASS, 1f);
        }
        TextureRegion hero = game.sprites.hero[0][4 + (int) (time * 2f) % 2];
        float s = 2.5f;
        float w = Sprites.w(hero) * s;
        float h = Sprites.h(hero) * s;
        float hx = PX + 2.2f;
        float hy = PY + 0.35f;
        b.draw(hero, hx - w / 2f, hy, w, h);
        Sprites.GunSprite gun = game.sprites.gun("rusty_sidearm");
        gun.drawIcon(b, hx + w * 0.5f, hy + h * 0.33f, 2.2f);
        if (pageTime > 2.5f) { // a glint of resolve
            float k = (pageTime * 2f) % 1f;
            d.circle(b, hx + 0.18f, hy + h * 0.8f, 0.04f + 0.04f * (1f - k), Color.WHITE, 1f - k);
        }
    }

    /** Page 6: black, with the title slamming in behind the shout. */
    private void drawEnded(SpriteBatch b, Draw d) {
        d.rect(b, PX, PY, PW, PH, Color.BLACK);
        float k = MathUtils.clamp((pageTime - 1.2f) / 0.6f, 0f, 1f);
        if (k > 0) {
            float scale = 1.0f + 0.4f * (1f - k);
            game.text("ENTER THE FLUFFY!", 3.5f, PY + PH / 2f + 0.25f, scale, tmp.set(Color.CORAL).mul(1, 1, 1, k), Align.center);
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
        if (music != null) {
            music.dispose();
            music = null;
        }
    }
}
