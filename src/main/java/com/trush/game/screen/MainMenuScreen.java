package com.trush.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.trush.game.launcher.Dev;
import com.trush.game.launcher.ETGFanfic;

/**
 * The title screen: NEW GAME the very first time (the story picture book, then the living room),
 * CONTINUE after that (straight home - runs start from the front door, see {@link HubScreen});
 * OPTIONS (with "reset progress", which makes it NEW GAME again) and QUIT.
 */
public class MainMenuScreen implements Screen {

    private static final Color TITLE = Color.CORAL;
    private static final Color DIM = Color.valueOf("8a8079");
    private static final float MUSIC_VOLUME = 0.45f;

    private static final String NEW_GAME = "NEW GAME";
    private static final String CONTINUE = "CONTINUE";
    private static final String OPTIONS = "OPTIONS";
    private static final String QUIT = "QUIT";

    private final ETGFanfic game;
    private Music music;
    private float time;
    private final MenuList menu = new MenuList(2.9f, 0.36f, 0.55f);
    private final OptionsMenu options = new OptionsMenu(true);
    private boolean optionsOpen;
    /** Nothing played yet (or progress reset): the first line is NEW GAME, which tells the story first. */
    private boolean fresh;

    public MainMenuScreen(ETGFanfic game) {
        this.game = game;
    }

    @Override
    public void show() {
        music = Gdx.audio.newMusic(Gdx.files.internal("assets/music/menu.ogg"));
        music.setLooping(true);
        music.setVolume(MUSIC_VOLUME * game.getMusicVolume());
        music.play();
        refreshItems();
        menu.select(0);
    }

    private void refreshItems() {
        fresh = !game.prefs.getBoolean(IntroScreen.SEEN_PREF, false);
        menu.setItems(fresh ? NEW_GAME : CONTINUE, OPTIONS, QUIT);
    }

    @Override
    public void render(float delta) {
        time += delta;
        ScreenUtils.clear(Color.BLACK);

        OrthographicCamera cam = (OrthographicCamera) game.viewport.getCamera();
        cam.position.set(game.viewport.getWorldWidth() / 2f, game.viewport.getWorldHeight() / 2f, 0);
        cam.update();
        game.viewport.apply();
        int chosen = -1;
        if (optionsOpen) {
            OptionsMenu.Result r = options.update(game);
            optionsOpen = r == OptionsMenu.Result.OPEN;
            if (r == OptionsMenu.Result.RESET) {
                refreshItems(); // a fresh start: NEW GAME again
            }
            music.setVolume(MUSIC_VOLUME * game.getMusicVolume());
        } else {
            chosen = menu.update(game);
        }

        game.batch.setProjectionMatrix(cam.combined);
        game.batch.begin();
        float cx = game.viewport.getWorldWidth() / 2f;
        if (Dev.ENABLED) {
            game.text("DEV BUILD", 0.1f, 4.9f, 0.26f, Color.ORANGE, Align.left);
        }
        if (optionsOpen) {
            options.draw(game, time);
        } else {
            game.text("Welcome to", cx, 4.3f, 0.5f, DIM, Align.center);
            game.text("ENTER THE FLUFFY!", cx, 3.95f, 1.05f, TITLE, Align.center);
            menu.draw(game, time);
            String hint = game.pad.isActive() ? "A choose" : "ENTER choose";
            game.text(hint, cx, 0.4f, 0.28f, DIM, Align.center);
        }
        game.batch.end();

        if (time > 0.3f && chosen >= 0) {
            activate(chosen);
        }
    }

    private void activate(int index) {
        switch (index) {
            case 0:
                if (fresh) { // the very first time: the story, then home
                    game.setScreen(new IntroScreen(game, () -> game.setScreen(new HubScreen(game))));
                } else {
                    game.setScreen(new HubScreen(game));
                }
                dispose();
                break;
            case 1:
                options.open();
                optionsOpen = true;
                break;
            default:
                Gdx.app.exit();
                break;
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
