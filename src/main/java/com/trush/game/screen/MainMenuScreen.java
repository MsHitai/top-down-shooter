package com.trush.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.ScreenUtils;
import com.trush.game.launcher.ETGFanfic;

public class MainMenuScreen implements Screen {

    private final ETGFanfic game;
    private final GlyphLayout layout = new GlyphLayout();
    private Music music;

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
        ScreenUtils.clear(Color.BLACK);

        game.viewport.apply();
        game.batch.setProjectionMatrix(game.viewport.getCamera().combined);
        game.batch.begin();

        BitmapFont font = game.font;
        font.setColor(Color.CORAL);
        float baseScale = font.getData().scaleX;

        drawCentered(font, "Welcome to ENTER THE FLUFFY!", 4.5f, baseScale * 0.7f);
        drawCentered(font, "Tap anywhere to begin!", 3f, baseScale);

        game.batch.end();

        if (Gdx.input.isTouched()) {
            game.setScreen(new FirstStageScreen(game));
            dispose();
        }
    }

    private void drawCentered(BitmapFont font, String text, float y, float scale) {
        font.getData().setScale(scale);
        layout.setText(font, text);
        font.draw(game.batch, layout, (game.viewport.getWorldWidth() - layout.width) / 2f, y);
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
