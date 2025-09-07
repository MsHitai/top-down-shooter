package com.trush.game.screen;

import com.badlogic.gdx.Screen;
import com.trush.game.launcher.ETGFanfic;

public class GameScreen implements Screen {

    private final ETGFanfic game;

    public GameScreen(ETGFanfic game) {
        this.game = game;
    }

    @Override
    public void show() {

    }

    @Override
    public void render(float delta) {

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
        /*
        backgroundTexture.dispose();
        dropSound.dispose();
        music.dispose();
        dropTexture.dispose();
        bucketTexture.dispose();*/
    }
}
