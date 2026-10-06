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
    private Sprite floor;
    private Player player;
    private Music music;

    public FirstStageScreen(ETGFanfic game) {
        this.game = game;
    }

    @Override
    public void show() {
        backgroundTexture = new Texture("assets/floor.png");
        floor = new Sprite(backgroundTexture);
        floor.setPosition(0, 0);
        floor.setSize(game.viewport.getWorldWidth(), game.viewport.getWorldHeight());
        player = new Player(3, 1);
        music = Gdx.audio.newMusic(Gdx.files.internal("assets/Enter the Gungeon room_one.mp3"));
        music.setLooping(true);
        music.setVolume(.06f);
        //music.play();
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(Color.BLACK);
        player.move(delta);
        restrictMovement();

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);

        game.batch.begin();
        floor.draw(game.batch);
        //game.batch.draw(backgroundTexture, 0, 0, game.viewport.getWorldWidth(), game.viewport.getWorldHeight());
        player.getPlayerSprite().draw(game.batch);
        game.batch.end();
    }

    // wall thickness of floor.png in world units (world is 7x5, image is 1536x1024)
    private static final float WALL_LEFT = 0.28f;
    private static final float WALL_RIGHT = 0.25f;
    private static final float WALL_BOTTOM = 0.38f;
    private static final float WALL_TOP = 0.28f;

    private void restrictMovement() {
        float roomWidth = game.viewport.getWorldWidth();
        float roomHeight = game.viewport.getWorldHeight();

        Sprite sprite = player.getPlayerSprite();
        float playerWidth = sprite.getWidth();
        float playerHeight = sprite.getHeight();

        float restrictedX = MathUtils.clamp(sprite.getX(), WALL_LEFT, roomWidth - playerWidth - WALL_RIGHT);
        float restrictedY = MathUtils.clamp(sprite.getY(), WALL_BOTTOM, roomHeight - playerHeight - WALL_TOP);

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
