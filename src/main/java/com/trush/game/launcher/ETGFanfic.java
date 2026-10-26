package com.trush.game.launcher;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.trush.game.player.Hero;
import com.trush.game.save.Unlocks;
import com.trush.game.world.Stage;
import com.trush.game.audio.Sfx;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;
import com.trush.game.input.Gamepad;
import com.trush.game.screen.GameScreen;
import com.trush.game.screen.HubScreen;
import com.trush.game.screen.MainMenuScreen;
import com.trush.game.screen.RocketRideScreen;

public class ETGFanfic extends Game {

    public SpriteBatch batch;
    public BitmapFont font;
    public FitViewport viewport;
    public Draw draw;
    public Sprites sprites;
    public Sfx sfx;
    public Gamepad pad;
    public Preferences prefs;

    /** Options (kept in prefs, untouched by a progress reset): music and sound volume 0-1, fullscreen. */
    private static final String MUSIC_VOLUME = "musicVolume";
    private static final String SOUND_VOLUME = "soundVolume";
    private static final String FULLSCREEN = "fullscreen";
    private static final String FURTHEST_FLOOR = "furthestFloor";
    /** Sfx's own volume at 100% sound. */
    private static final float SFX_BASE = 0.7f;
    private static final int WINDOW_W = 1280;
    private static final int WINDOW_H = 980;

    /** Font scale that makes the generated font fit the 7x5 world. Text helpers scale relative to it. */
    public float fontScale;
    private final GlyphLayout layout = new GlyphLayout();

    @Override
    public void create() {
        batch = new SpriteBatch();
        viewport = new FitViewport(7, 5);

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
                Gdx.files.internal("assets/fonts/PressStart2P-Regular.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = Math.round(Gdx.graphics.getHeight() * 0.06f);
        font = generator.generateFont(parameter);
        generator.dispose();

        font.setUseIntegerPositions(false);
        fontScale = viewport.getWorldHeight() / Gdx.graphics.getHeight();
        font.getData().setScale(fontScale);

        draw = new Draw();
        sprites = new Sprites();
        sfx = new Sfx();
        pad = new Gamepad();
        prefs = Gdx.app.getPreferences("enter-the-fluffy");
        Unlocks.load(prefs);
        Hero.load(prefs);
        sfx.volume = SFX_BASE * getSoundVolume();
        if (isFullscreen()) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }

        String play = Dev.text("play"); // dev builds: -Dfluffy.play=story / endless / home skips the main menu
        if ("home".equalsIgnoreCase(play)) {
            this.setScreen(new HubScreen(this));
        } else if ("ride".equalsIgnoreCase(play)) { // the rocket ride up, then a story run (with moon=true, on the Moon)
            this.setScreen(new RocketRideScreen(this, () -> new GameScreen(this, GameScreen.Mode.STORY)));
        } else if ("story".equalsIgnoreCase(play) || "endless".equalsIgnoreCase(play)) {
            this.setScreen(new GameScreen(this, "story".equalsIgnoreCase(play) ? GameScreen.Mode.STORY : GameScreen.Mode.ENDLESS));
        } else {
            this.setScreen(new MainMenuScreen(this));
        }
    }

    /**
     * Draws text in world units. {@code align} is a {@link com.badlogic.gdx.utils.Align} constant
     * describing where (x, y) is relative to the text (left / center / right); y is the top of the line.
     */
    public void text(String s, float x, float y, float scale, Color color, int align) {
        font.getData().setScale(fontScale * scale);
        layout.setText(font, s, color, 0, align, false);
        font.draw(batch, layout, x, y);
        font.getData().setScale(fontScale);
    }

    /** How wide {@link #text} would draw this string at this scale. */
    public float textWidth(String s, float scale) {
        font.getData().setScale(fontScale * scale);
        layout.setText(font, s);
        font.getData().setScale(fontScale);
        return layout.width;
    }

    /** Music volume from the options, 0-1: every track's own volume is multiplied by it. */
    public float getMusicVolume() {
        return prefs.getFloat(MUSIC_VOLUME, 1f);
    }

    public void setMusicVolume(float v) {
        prefs.putFloat(MUSIC_VOLUME, v);
        prefs.flush();
    }

    public float getSoundVolume() {
        return prefs.getFloat(SOUND_VOLUME, 1f);
    }

    public void setSoundVolume(float v) {
        prefs.putFloat(SOUND_VOLUME, v);
        prefs.flush();
        sfx.volume = SFX_BASE * v;
    }

    public boolean isFullscreen() {
        return prefs.getBoolean(FULLSCREEN, false);
    }

    public void setFullscreen(boolean on) {
        prefs.putBoolean(FULLSCREEN, on);
        prefs.flush();
        if (on) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            Gdx.graphics.setWindowedMode(WINDOW_W, WINDOW_H);
        }
    }

    /**
     * "Reset progress" in the options: forgets everything won and found - unlocks, best scores, wins,
     * the saved run, having seen the story - but keeps the options themselves.
     */
    public void resetProgress() {
        float music = getMusicVolume();
        float sound = getSoundVolume();
        boolean full = isFullscreen();
        prefs.clear();
        prefs.putFloat(MUSIC_VOLUME, music);
        prefs.putFloat(SOUND_VOLUME, sound);
        prefs.putBoolean(FULLSCREEN, full);
        prefs.flush();
        Unlocks.load(prefs);
        Hero.load(prefs);
    }

    /**
     * The furthest story floor ever reached (the title screen's picture draws in each place you've
     * been). Anyone who has won has been everywhere - also those who won before this was remembered.
     */
    public int getFurthestFloor() {
        return getWins() > 0 ? Stage.lastFloor() : prefs.getInteger(FURTHEST_FLOOR, 0);
    }

    public void reachFloor(int floor) {
        if (floor > prefs.getInteger(FURTHEST_FLOOR, 0)) {
            prefs.putInteger(FURTHEST_FLOOR, Math.min(floor, Stage.lastFloor()));
            prefs.flush();
        }
    }

    public int getHighScore() {
        return prefs.getInteger("highScore", 0);
    }

    public int getBestWave() {
        return prefs.getInteger("bestWave", 0);
    }

    /** How many story runs have been won (the last floor's boss beaten). */
    public int getWins() {
        return prefs.getInteger("wins", 0);
    }

    public void addWin() {
        prefs.putInteger("wins", getWins() + 1);
        prefs.flush();
        Unlocks.grandad = true; // the King is beaten: grandad wants to come along now
        Unlocks.beatKing(Hero.current, prefs); // their piece of the rocket at home
    }

    /**
     * How many times the False King (the Skunk King, at the end of the secret detour) has been beaten - the
     * normal ending. It's a win, but it doesn't save the kingdom: the real King still sulks in the clouds.
     */
    public int getFalseKingBeaten() {
        return prefs.getInteger("falseKingBeaten", 0);
    }

    public void addFalseKingBeaten() {
        prefs.putInteger("falseKingBeaten", getFalseKingBeaten() + 1);
        prefs.flush();
    }

    public void submitRun(int score, int wave) {
        if (score > getHighScore()) {
            prefs.putInteger("highScore", score);
        }
        if (wave > getBestWave()) {
            prefs.putInteger("bestWave", wave);
        }
        prefs.flush();
    }

    @Override
    public void render() {
        pad.update();
        super.render();
    }

    @Override
    public void dispose() {
        if (getScreen() instanceof GameScreen) {
            ((GameScreen) getScreen()).saveIfBetweenFloors(); // closing the window after a boss keeps the run (and records count)
        }
        super.dispose();
        if (getScreen() != null) {
            getScreen().dispose();
        }
        batch.dispose();
        font.dispose();
        draw.dispose();
        sprites.dispose();
        sfx.dispose();
    }
}
