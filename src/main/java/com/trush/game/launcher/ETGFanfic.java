package com.trush.game.launcher;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.trush.game.screen.MainMenuScreen;

public class ETGFanfic extends Game {

    public SpriteBatch batch;
    public BitmapFont font;
    public FitViewport viewport;

    @Override
    public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        viewport = new FitViewport(8, 5);

        font.setUseIntegerPositions(false);
        font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() * 1.5f);

        this.setScreen(new MainMenuScreen(this));
    }

    public void render() {
        super.render();
    }

    public void dispose() {
        batch.dispose();
        font.dispose();
        super.dispose();
    }
}
