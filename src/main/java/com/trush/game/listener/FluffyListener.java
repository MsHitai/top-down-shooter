package com.trush.game.listener;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.io.*;


public class FluffyListener implements ApplicationListener {

    private Texture backgroundTexture;
    private Texture fluffyTexture;
    private Texture dialogTexture;
    private Sound fluffySound;
    private Music music;
    private SpriteBatch spriteBatch;
    private FitViewport viewport;
    private Sprite fluffySprite;
    private Sprite dialogSprite;
    private Vector2 touchPos;
    private Rectangle fluffyRectangle;
    private Long count;
    private boolean isPulsating = false;
    private boolean dialogVisible;
    private float dialogTimer;
    private float pulsateTimer = 0f;
    private final float PULSATE_DURATION = 0.3f;
    private final float MAX_SCALE = 1.5f;
    private final float ORIGINAL_SCALE = 1f;
    private BitmapFont font;
    private boolean needsSave;


    @Override
    public void create() {
        backgroundTexture = new Texture("fluffy-background.png");
        fluffyTexture = new Texture("fluffy.png");
        fluffySound = Gdx.audio.newSound(Gdx.files.internal("fluffy-sound.mp3"));
        music = Gdx.audio.newMusic(Gdx.files.internal("Loyalty_Freak_Music.mp3"));
        music.setLooping(true);
        music.setVolume(.3f);
        music.play();
        spriteBatch = new SpriteBatch();
        viewport = new FitViewport(7, 8);
        fluffySprite = new Sprite(fluffyTexture);
        fluffySprite.setSize(2.5f, 2);
        fluffySprite.setPosition((viewport.getWorldWidth() - fluffySprite.getWidth()) / 2f - 1.5f,
                (viewport.getWorldHeight() - fluffySprite.getHeight()) / 2f - 0.5f);
        fluffySprite.setOriginCenter();
        dialogTexture = new Texture("pat_fluffy.png");
        dialogSprite = new Sprite(dialogTexture);
        dialogSprite.setSize(3f, 3f);
        dialogSprite.setPosition(fluffySprite.getX() + 1f, fluffySprite.getY() + 0.8f);
        touchPos = new Vector2();
        fluffyRectangle = new Rectangle();
        try (BufferedReader reader = new BufferedReader(new FileReader("src/main/resources/save.txt"))) {
            String line = reader.readLine();
            if (line.contains("count: ")) {
                String substring = line.replace("count: ", "");
                count = Long.parseLong(substring);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        font = new BitmapFont();
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        font.setUseIntegerPositions(false);
        font.getData().setScale(0.03f);
        font.setColor(Color.BLUE);
        dialogVisible = true;
        dialogTimer = 0f;
        needsSave = false;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void render() {
        input();
        pulsate();
        draw();
        if (needsSave) {
            save();
            needsSave = false;
        }
        dialogShow();
    }

    private void input() {
        if (Gdx.input.justTouched()) {
            touchPos.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(touchPos);

            if (fluffySprite.getBoundingRectangle().contains(touchPos.x, touchPos.y)) {
                isPulsating = true;
                pulsateTimer = 0f;
                fluffySound.play();
                count++;
                dialogTimer = 0f;
                dialogVisible = false;
                needsSave = true;
            }
        }
    }

    private void pulsate() {
        if (isPulsating) {
            pulsateTimer += Gdx.graphics.getDeltaTime();
            float halfDuration = PULSATE_DURATION / 2f;

            if (pulsateTimer <= halfDuration) {
                float scale = ORIGINAL_SCALE + (MAX_SCALE - ORIGINAL_SCALE) * (pulsateTimer / halfDuration);
                fluffySprite.setScale(scale);
            } else if (pulsateTimer <= PULSATE_DURATION) {
                float scale = MAX_SCALE - (MAX_SCALE - ORIGINAL_SCALE) * ((pulsateTimer - halfDuration) / halfDuration);
                fluffySprite.setScale(scale);
            } else {
                fluffySprite.setScale(ORIGINAL_SCALE);
                isPulsating = false;
            }
        }
    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);
        viewport.apply();

        spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
        float worldWidth = viewport.getWorldWidth();
        float worldHeight = viewport.getWorldHeight();
        spriteBatch.begin();

        spriteBatch.draw(backgroundTexture, 0, 0, worldWidth, worldHeight);
        fluffySprite.draw(spriteBatch);
        font.draw(spriteBatch, "Plushies: " + count, 0.2f, worldHeight - 0.4f);
        if (dialogVisible) {
            dialogSprite.draw(spriteBatch);
        }

        spriteBatch.end();
    }

    private void save() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/resources/save.txt"))) {
            writer.write("count: " + count);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void dialogShow() {
        dialogTimer += Gdx.graphics.getDeltaTime();
        if (dialogTimer > 5 && !dialogVisible) {
            dialogVisible = true;
        }
    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void dispose() {

    }
}
