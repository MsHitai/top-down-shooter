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
    private static final String QUIT = "QUIT";

    private final ETGFanfic game;
    private Music music;
    private float time;
    private final MenuList menu = new MenuList(3.5f, 0.3f, 0.5f);
    /** A run saved between floors that can be continued, or null. */
    private RunSave saved;

    public MainMenuScreen(ETGFanfic game) {
        this.game = game;
    }

    @Override
    public void show() {
        music = Gdx.audio.newMusic(Gdx.files.internal("assets/music/menu.ogg"));
        music.setLooping(true);
        music.setVolume(.45f);
        music.play();
        saved = RunSave.read(game.prefs);
        if (saved != null) {
            menu.setItems(saved.describe(), STORY, ENDLESS, QUIT);
        } else {
            menu.setItems(STORY, ENDLESS, QUIT);
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
        int chosen = menu.update(game);

        game.batch.setProjectionMatrix(cam.combined);
        game.batch.begin();
        float cx = game.viewport.getWorldWidth() / 2f;
        game.text("Welcome to", cx, 4.6f, 0.5f, DIM, Align.center);
        if (Dev.ENABLED) {
            game.text("DEV BUILD", 0.1f, 4.9f, 0.26f, Color.ORANGE, Align.left);
        }
        game.text("ENTER THE FLUFFY!", cx, 4.25f, 1.05f, TITLE, Align.center);
        menu.draw(game, time);

        float y = 2.2f;
        String[][] controls = game.pad.isActive() ? new String[][]{
                {"LEFT STICK", "move"},
                {"RIGHT STICK", "aim"},
                {"RB", "shoot"},
                {"LB", "dodge roll (invincible!)"},
                {"A", "flip a table, open a chest"},
                {"R3 (CLICK RS)", "blank - clears all bullets"},
                {"X", "reload"},
                {"Y / HOLD Y", "next gun / gun wheel"},
                {"START", "pause"},
        } : new String[][]{
                {"WASD / ARROWS", "move"},
                {"MOUSE", "aim, hold LMB to shoot"},
                {"SPACE / RMB", "dodge roll (invincible!)"},
                {"E", "flip a table, open a chest"},
                {"Q", "blank - clears all bullets"},
                {"R", "reload"},
                {"SCROLL / TAB / 1-4", "switch gun"},
                {"ESC", "pause"},
        };
        for (String[] row : controls) {
            game.text(row[0], cx - 0.15f, y, 0.3f, TEXT, Align.right);
            game.text(row[1], cx + 0.15f, y, 0.3f, DIM, Align.left);
            y -= 0.18f;
        }

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
            case 1:
                RunSave.delete(game.prefs); // starting a new run abandons the suspended one
                start(new GameScreen(game, index == 0 ? GameScreen.Mode.STORY : GameScreen.Mode.ENDLESS));
                break;
            default:
                Gdx.app.exit();
                break;
        }
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
