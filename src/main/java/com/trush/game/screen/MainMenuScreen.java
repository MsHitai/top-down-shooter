package com.trush.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.trush.game.launcher.ETGFanfic;

public class MainMenuScreen implements Screen {

    private static final Color TITLE = Color.CORAL;
    private static final Color TEXT = Color.valueOf("f5f0e6");
    private static final Color DIM = Color.valueOf("8a8079");

    private final ETGFanfic game;
    private Music music;
    private float time;
    private final Color tmp = new Color();

    public MainMenuScreen(ETGFanfic game) {
        this.game = game;
    }

    @Override
    public void show() {
        music = Gdx.audio.newMusic(Gdx.files.internal("assets/Enter the Gungeon - Enter the Gun - menu.mp3"));
        music.setLooping(true);
        music.setVolume(.5f);
        music.play();
    }

    @Override
    public void render(float delta) {
        time += delta;
        ScreenUtils.clear(Color.BLACK);

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);
        game.batch.begin();

        float cx = game.viewport.getWorldWidth() / 2f;
        game.text("Welcome to", cx, 4.5f, 0.5f, DIM, Align.center);
        game.text("ENTER THE FLUFFY!", cx, 4.15f, 1.05f, TITLE, Align.center);

        float pulse = 0.6f + 0.4f * MathUtils.sin(time * 4f);
        game.text("Click or press ENTER to begin!", cx, 3.25f, 0.55f, tmp.set(TEXT).mul(1, 1, 1, pulse), Align.center);

        float y = 2.5f;
        String[][] controls = {
                {"WASD / ARROWS", "move"},
                {"MOUSE", "aim, hold LMB to shoot"},
                {"SPACE / RMB", "dodge roll (invincible!)"},
                {"Q", "blank - clears all bullets"},
                {"R", "reload"},
                {"SCROLL / E / 1-4", "switch gun"},
                {"ESC", "pause"},
        };
        for (String[] row : controls) {
            game.text(row[0], cx - 0.15f, y, 0.32f, TEXT, Align.right);
            game.text(row[1], cx + 0.15f, y, 0.32f, DIM, Align.left);
            y -= 0.22f;
        }

        if (game.getHighScore() > 0) {
            game.text("Best score " + game.getHighScore() + "   Best wave " + game.getBestWave(),
                    cx, 0.45f, 0.34f, Color.GOLD, Align.center);
        }

        game.batch.end();

        if (time > 0.3f && (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE))) {
            game.setScreen(new FirstStageScreen(game));
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
