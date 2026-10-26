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
    private static final float MENU_X = 1.45f;

    private static final String NEW_GAME = "NEW GAME";
    private static final String CONTINUE = "CONTINUE";
    private static final String OPTIONS = "OPTIONS";
    private static final String QUIT = "QUIT";

    private final ETGFanfic game;
    private Music music;
    private float time;
    /** Bottom left over the hills, like ETG's, so the picture shows. */
    private final MenuList menu = new MenuList(1.12f, 0.3f, 0.45f);
    private final OptionsMenu options = new OptionsMenu(true);
    private boolean optionsOpen;
    /** Nothing played yet (or progress reset): the first line is NEW GAME, which tells the story first. */
    private boolean fresh;
    private TitlePicture picture;
    private static final Color SHADE = Color.valueOf("1e1416");

    public MainMenuScreen(ETGFanfic game) {
        this.game = game;
    }

    @Override
    public void show() {
        music = Gdx.audio.newMusic(Gdx.files.internal("assets/music/menu.ogg"));
        music.setLooping(true);
        music.setVolume(MUSIC_VOLUME * game.getMusicVolume());
        music.play();
        picture = new TitlePicture(game);
        menu.setCenter(MENU_X, 1.15f);
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
                picture.refresh(); // and the mist comes back
            }
            music.setVolume(MUSIC_VOLUME * game.getMusicVolume());
        } else {
            chosen = menu.update(game);
        }

        game.batch.setProjectionMatrix(cam.combined);
        game.batch.begin();
        float cx = game.viewport.getWorldWidth() / 2f;
        picture.draw(game.batch, game.draw, time, delta);
        if (optionsOpen) {
            game.draw.rect(game.batch, 0, 0, 7f, 5f, SHADE, 0.88f);
            options.draw(game, time);
        } else {
            game.draw.rect(game.batch, 0, 3.9f, 7f, 1.1f, SHADE, 0.6f);
            game.text("Welcome to", cx, 4.82f, 0.4f, DIM, Align.center);
            game.text("ENTER THE FLUFFY!", cx, 4.52f, 1.0f, TITLE, Align.center);
            game.draw.rect(game.batch, MENU_X - 1.25f, 0.15f, 2.5f, 1.1f, SHADE, 0.7f);
            menu.draw(game, time);
        }
        if (Dev.ENABLED) {
            game.text("DEV BUILD", 0.1f, 4.9f, 0.26f, Color.ORANGE, Align.left);
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
