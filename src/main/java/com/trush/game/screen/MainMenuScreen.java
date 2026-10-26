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
import com.trush.game.save.RunSave;

public class MainMenuScreen implements Screen {

    private static final Color TITLE = Color.CORAL;
    private static final Color TEXT = Color.valueOf("f5f0e6");
    private static final Color DIM = Color.valueOf("8a8079");

    private static final String STORY = "STORY - 1. THE VILLAGE";
    private static final String ENDLESS = "ENDLESS";
    private static final String INTRO = "THE STORY";
    private static final String OPTIONS = "OPTIONS";
    private static final String QUIT = "QUIT";

    private static final float MUSIC_VOLUME = 0.45f;

    private final ETGFanfic game;
    private Music music;
    private float time;
    private final MenuList menu = new MenuList(3.5f, 0.3f, 0.5f);
    /** A run saved between floors that can be continued, or null. */
    private RunSave saved;
    private final OptionsMenu options = new OptionsMenu(true);
    private boolean optionsOpen;

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
    }

    private void refreshItems() {
        saved = RunSave.read(game.prefs);
        if (saved != null) {
            menu.setItems(saved.describe(), STORY, ENDLESS, INTRO, OPTIONS, QUIT);
        } else {
            menu.setItems(STORY, ENDLESS, INTRO, OPTIONS, QUIT);
        }
        menu.select(0);
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
                refreshItems(); // the saved run is gone
            }
            music.setVolume(MUSIC_VOLUME * game.getMusicVolume());
        } else {
            chosen = menu.update(game);
        }

        game.batch.setProjectionMatrix(cam.combined);
        game.batch.begin();
        float cx = game.viewport.getWorldWidth() / 2f;
        game.text("Welcome to", cx, 4.6f, 0.5f, DIM, Align.center);
        if (Dev.ENABLED) {
            game.text("DEV BUILD", 0.1f, 4.9f, 0.26f, Color.ORANGE, Align.left);
        }
        if (optionsOpen) {
            options.draw(game, time);
            game.batch.end();
            return;
        }
        game.text("ENTER THE FLUFFY!", cx, 4.25f, 1.05f, TITLE, Align.center);
        menu.draw(game, time);

        if (game.getHighScore() > 0) {
            game.text("Best score " + game.getHighScore() + "   Best endless wave " + game.getBestWave(),
                    cx, 0.4f, 0.32f, Color.GOLD, Align.center);
        }
        game.batch.end();

        if (time > 0.3f && chosen >= 0) {
            activate(chosen);
        }
    }

    private void activate(int index) {
        if (saved != null) {
            if (index == 0) {
                if (!Dev.flag("keepSave")) {
                    RunSave.delete(game.prefs); // used up, like ETG: the next save point is after this floor's boss
                }
                start(new GameScreen(game, saved));
                return;
            }
            index--;
        }
        switch (index) {
            case 0:
                RunSave.delete(game.prefs); // starting a new run abandons the suspended one
                if (!game.prefs.getBoolean(IntroScreen.SEEN_PREF, false)) { // the very first run: the story first
                    startIntro(() -> game.setScreen(new GameScreen(game, GameScreen.Mode.STORY)));
                } else {
                    start(new GameScreen(game, GameScreen.Mode.STORY));
                }
                break;
            case 1:
                RunSave.delete(game.prefs);
                start(new GameScreen(game, GameScreen.Mode.ENDLESS));
                break;
            case 2: // watch the story again, then back here
                startIntro(() -> game.setScreen(new MainMenuScreen(game)));
                break;
            case 3:
                options.open();
                optionsOpen = true;
                break;
            default:
                Gdx.app.exit();
                break;
        }
    }

    private void startIntro(Runnable then) {
        game.setScreen(new IntroScreen(game, then));
        dispose();
    }

    private void start(GameScreen screen) {
        game.setScreen(screen);
        dispose();
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
