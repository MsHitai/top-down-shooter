package com.trush.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.trush.game.launcher.ETGFanfic;

public class MainMenuScreen implements Screen {

    private static final Color TITLE = Color.CORAL;
    private static final Color TEXT = Color.valueOf("f5f0e6");
    private static final Color DIM = Color.valueOf("8a8079");
    private static final Color SELECTED = Color.valueOf("ffd166");

    private static final String[] ITEMS = {"STORY - 1. THE VILLAGE", "ENDLESS"};
    private static final float ITEM_Y = 3.35f;
    private static final float ITEM_STEP = 0.36f;

    private final ETGFanfic game;
    private Music music;
    private float time;
    private int selected;
    private final Color tmp = new Color();
    private final Vector3 mouse = new Vector3();

    public MainMenuScreen(ETGFanfic game) {
        this.game = game;
    }

    @Override
    public void show() {
        music = Gdx.audio.newMusic(Gdx.files.internal("assets/music/menu.ogg"));
        music.setLooping(true);
        music.setVolume(.45f);
        music.play();
    }

    @Override
    public void render(float delta) {
        time += delta;
        ScreenUtils.clear(Color.BLACK);

        OrthographicCamera cam = (OrthographicCamera) game.viewport.getCamera();
        cam.position.set(game.viewport.getWorldWidth() / 2f, game.viewport.getWorldHeight() / 2f, 0);
        cam.update();
        game.viewport.apply();
        mouse.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        game.viewport.unproject(mouse);
        int hovered = -1;
        for (int i = 0; i < ITEMS.length; i++) {
            float y = ITEM_Y - i * ITEM_STEP;
            if (mouse.y < y + 0.05f && mouse.y > y - 0.3f && Math.abs(mouse.x - 3.5f) < 2.2f) {
                hovered = i;
            }
        }
        if (hovered >= 0 && (Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0)) {
            selected = hovered;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selected = (selected + ITEMS.length - 1) % ITEMS.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selected = (selected + 1) % ITEMS.length;
        }

        game.batch.setProjectionMatrix(cam.combined);
        game.batch.begin();
        float cx = game.viewport.getWorldWidth() / 2f;
        game.text("Welcome to", cx, 4.6f, 0.5f, DIM, Align.center);
        game.text("ENTER THE FLUFFY!", cx, 4.25f, 1.05f, TITLE, Align.center);

        float pulse = 0.65f + 0.35f * MathUtils.sin(time * 4f);
        for (int i = 0; i < ITEMS.length; i++) {
            boolean sel = i == selected;
            String label = sel ? "> " + ITEMS[i] + " <" : ITEMS[i];
            game.text(label, cx, ITEM_Y - i * ITEM_STEP, 0.55f,
                    sel ? tmp.set(SELECTED).mul(1, 1, 1, pulse) : TEXT, Align.center);
        }

        float y = 2.35f;
        String[][] controls = {
                {"WASD / ARROWS", "move"},
                {"MOUSE", "aim, hold LMB to shoot"},
                {"SPACE / RMB", "dodge roll (invincible!)"},
                {"E", "flip a table for cover"},
                {"Q", "blank - clears all bullets"},
                {"R", "reload"},
                {"SCROLL / TAB / 1-4", "switch gun"},
                {"ESC", "pause"},
        };
        for (String[] row : controls) {
            game.text(row[0], cx - 0.15f, y, 0.3f, TEXT, Align.right);
            game.text(row[1], cx + 0.15f, y, 0.3f, DIM, Align.left);
            y -= 0.2f;
        }

        if (game.getHighScore() > 0) {
            game.text("Best score " + game.getHighScore() + "   Best endless wave " + game.getBestWave(),
                    cx, 0.4f, 0.32f, Color.GOLD, Align.center);
        }
        game.batch.end();

        boolean clicked = time > 0.3f && Gdx.input.justTouched() && hovered >= 0;
        if (clicked) {
            selected = hovered;
        }
        if (time > 0.3f && (clicked || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) {
            game.setScreen(new GameScreen(game, selected == 0 ? GameScreen.Mode.STORY : GameScreen.Mode.ENDLESS));
            dispose();
        }
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
