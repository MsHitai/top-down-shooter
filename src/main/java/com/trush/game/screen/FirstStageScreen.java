package com.trush.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.player.Player;

public class FirstStageScreen implements Screen {

    private final ETGFanfic game;
    private Texture backgroundTexture;
    private Player player;
    private Music music;

    public FirstStageScreen(ETGFanfic game) {
        this.game = game;
    }

    @Override
    public void show() {
        backgroundTexture = new Texture("room_background.png");
        player = new Player(3, 1);
        music = Gdx.audio.newMusic(Gdx.files.internal("Enter the Gungeon room_one.mp3"));
        music.setLooping(true);
        music.setVolume(.06f);
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.BLACK);
        music.play();
        player.move(delta);
        restrictMovement();

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);

        game.batch.begin();
        game.batch.draw(backgroundTexture, 0, 0, game.viewport.getWorldWidth(), game.viewport.getWorldHeight());
        player.getPlayerSprite().draw(game.batch);
        game.batch.end();
    }

    private void restrictMovement() {
        float roomWidth = game.viewport.getWorldWidth();
        float roomHeight = game.viewport.getWorldHeight();

        Sprite sprite = player.getPlayerSprite();
        float playerWidth = sprite.getWidth();
        float playerHeight = sprite.getHeight();

        float restrictedX = MathUtils.clamp(sprite.getX(), 0f, roomWidth - playerWidth + 0.5f);
        float restrictedY = MathUtils.clamp(sprite.getY(), 0f, roomHeight - playerHeight + 0.5f);

        sprite.setPosition(restrictedX, restrictedY);
        player.getBoundingBox().setPosition(restrictedX, restrictedY);
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
        backgroundTexture.dispose();
        music.dispose();
    }
}
