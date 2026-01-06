package com.trush.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.ScreenUtils;
import com.trush.game.launcher.ETGFanfic;

public class MainMenuScreen implements Screen {

    private final ETGFanfic game;
    private BitmapFont menuFont;
    private Music music;

    public MainMenuScreen(ETGFanfic game) {
        this.game = game;
    }

    @Override
    public void show() {
        menuFont = new BitmapFont();
        menuFont.setUseIntegerPositions(false);
        menuFont.getData().setScale(
                game.viewport.getWorldHeight() / Gdx.graphics.getHeight() * 3.5f
        );
        music = Gdx.audio.newMusic(Gdx.files.internal("Enter the Gungeon - Enter the Gun - menu.mp3"));
        music.setLooping(true);
        music.setVolume(.5f);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.BLACK);

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);
        music.play();
        game.batch.begin();

        menuFont.setColor(Color.CORAL);
        menuFont.draw(game.batch, "Welcome to ENTER THE FLUFFY! ", 1.7f, 4.5f);
        menuFont.draw(game.batch, "Tap anywhere to begin!", 2, 3);
        game.batch.end();

        if (Gdx.input.isTouched()) {
            game.setScreen(new GameScreen(game));
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
        menuFont.dispose();
        music.dispose();
    }
}
