package com.trush.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.trush.game.player.Player;

public class MainGameScreen extends ScreenAdapter {

    private Player player;
    private SpriteBatch batch;

    public MainGameScreen() {
        player = new Player(50, 50);
        batch = new SpriteBatch();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        player.draw(batch);
        batch.end();

        update(delta);
    }

    private void update(float delta) {
        // Update game logic here
    }
}
