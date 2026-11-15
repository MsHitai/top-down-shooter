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
import com.trush.game.save.Flufficon;
import com.trush.game.save.Unlocks;
import com.trush.game.world.Dream;
import com.trush.game.world.Stage;
import com.trush.game.audio.Sfx;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;
import com.trush.game.input.Gamepad;
import com.trush.game.screen.AchievementBanner;
import com.trush.game.screen.GameScreen;
import com.trush.game.screen.HubScreen;
import com.trush.game.screen.MainMenuScreen;
import com.trush.game.screen.RocketGameScreen;
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
    private final AchievementBanner banner = new AchievementBanner();

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
        String devSave = Dev.text("prefs"); // dev builds: -Dfluffy.prefs=test plays on a save file of its own
        prefs = Gdx.app.getPreferences(devSave != null ? "enter-the-fluffy-" + devSave : "enter-the-fluffy");
        Unlocks.load(prefs);
        Hero.load(prefs);
        Flufficon.load(prefs);
        sfx.volume = SFX_BASE * getSoundVolume();
        if (isFullscreen() && !Recorder.ON) { // (filming keeps its window size)
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }

        String play = Dev.text("play"); // dev builds: -Dfluffy.play=story / endless / home skips the main menu
        if ("home".equalsIgnoreCase(play)) {
            this.setScreen(new HubScreen(this));
        } else if ("ride".equalsIgnoreCase(play)) { // the rocket ride up, then a story run (with moon=true, on the Moon)
            this.setScreen(new RocketRideScreen(this, () -> new GameScreen(this, GameScreen.Mode.STORY)));
        } else if ("dream".equalsIgnoreCase(play) || "daily".equalsIgnoreCase(play) || "rush".equalsIgnoreCase(play)) {
            this.setScreen(new GameScreen(this, Dream.start("dream".equalsIgnoreCase(play) ? Dream.Kind.DREAM
                    : "daily".equalsIgnoreCase(play) ? Dream.Kind.DAILY : Dream.Kind.BOSS_RUSH)));
        } else if ("console".equalsIgnoreCase(play)) { // the rocket game on the console at home
            this.setScreen(new RocketGameScreen(this));
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
        boolean crowns = Unlocks.wearCrowns;
        prefs.clear();
        prefs.putFloat(MUSIC_VOLUME, music);
        prefs.putFloat(SOUND_VOLUME, sound);
        prefs.putBoolean(FULLSCREEN, full);
        prefs.putBoolean(Unlocks.WEAR_CROWNS, crowns);
        prefs.flush();
        Unlocks.load(prefs);
        Hero.load(prefs);
        Flufficon.load(prefs);
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
        if (Hero.current.easy()) { // the baby's wins are his own: no grandad, no piece of the rocket
            addEasyWin();
            return;
        }
        prefs.putInteger("wins", getWins() + 1);
        prefs.flush();
        Unlocks.grandad = true; // the King is beaten: grandad wants to come along now
        Unlocks.beatKing(Hero.current, prefs); // their piece of the rocket at home
    }

    /** Runs won as the baby brother (easy mode), any ending: counted apart from everyone else's. */
    public int getEasyWins() {
        return prefs.getInteger("babyWins", 0);
    }

    private void addEasyWin() {
        prefs.putInteger("babyWins", getEasyWins() + 1);
        prefs.flush();
    }

    /**
     * How many times the False King (the Skunk King, at the end of the secret detour) has been beaten - the
     * normal ending. It's a win, but it doesn't save the kingdom: the real King still sulks in the clouds.
     */
    public int getFalseKingBeaten() {
        return prefs.getInteger("falseKingBeaten", 0);
    }

    public void addFalseKingBeaten() {
        if (Hero.current.easy()) {
            addEasyWin();
            return;
        }
        prefs.putInteger("falseKingBeaten", getFalseKingBeaten() + 1);
        prefs.flush();
    }

    /** The best score in the rocket game on the console at home (screen/RocketGameScreen). */
    public int getRocketBest() {
        return prefs.getInteger("rocketBest", 0);
    }

    /** Keeps {@code score} if it beats the rocket game's best; true when it did. */
    public boolean setRocketBest(int score) {
        if (score <= getRocketBest()) {
            return false;
        }
        prefs.putInteger("rocketBest", score);
        prefs.flush();
        return true;
    }

    /** How many times Grandfluff has been beaten up on the Moon: the true ending. */
    public int getTrueEndings() {
        return prefs.getInteger("trueEndings", 0);
    }

    public void addTrueEnding() {
        if (Hero.current.easy()) {
            addEasyWin();
            return;
        }
        prefs.putInteger("trueEndings", getTrueEndings() + 1);
        prefs.flush();
    }

    /** Dreams dreamt to the end in the bedroom (any kind), and the boss rushes among them. */
    public int getDreams() {
        return prefs.getInteger("dreams", 0);
    }

    public int getBossRushes() {
        return prefs.getInteger("bossRushes", 0);
    }

    /** The fastest dream / boss rush, in seconds (0: none yet). */
    public float getFastestDream() {
        return prefs.getFloat("dreamFastest", 0f);
    }

    public float getFastestBossRush() {
        return prefs.getFloat("bossRushFastest", 0f);
    }

    /** Today's dream's best score (0 if it hasn't been dreamt today). */
    public int getDailyBest() {
        return prefs.getLong("dailyDate", 0L) == Dream.todaySeed() ? prefs.getInteger("dailyBest", 0) : 0;
    }

    /** How many days' dreams have been dreamt to the end. */
    public int getDailiesDone() {
        return prefs.getInteger("dailiesDone", 0);
    }

    /**
     * A dream dreamt to its end (you woke up): counted, and its time kept if it's the fastest of its kind
     * (true then). Today's dream counts once a day.
     */
    public boolean finishDream(Dream d) {
        boolean rush = d.bossRush();
        prefs.putInteger("dreams", getDreams() + 1);
        if (rush) prefs.putInteger("bossRushes", getBossRushes() + 1);
        if (d.kind() == Dream.Kind.DAILY && prefs.getLong("dailyDone", 0L) != d.seed) {
            prefs.putLong("dailyDone", d.seed);
            prefs.putInteger("dailiesDone", getDailiesDone() + 1);
        }
        String key = rush ? "bossRushFastest" : "dreamFastest";
        float best = prefs.getFloat(key, 0f);
        boolean fastest = best <= 0f || d.time < best;
        if (fastest) prefs.putFloat(key, d.time);
        prefs.flush();
        return fastest;
    }

    /** Today's dream ended (woken up or not): its best score of the day. */
    public void submitDaily(long seed, int score) {
        if (prefs.getLong("dailyDate", 0L) != seed) {
            prefs.putLong("dailyDate", seed);
            prefs.putInteger("dailyBest", 0);
        }
        if (score > prefs.getInteger("dailyBest", 0)) {
            prefs.putInteger("dailyBest", score);
        }
        prefs.flush();
    }

    /** "3:07" */
    public static String clock(float seconds) {
        int s = Math.max(0, (int) seconds);
        return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
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
        float delta = Recorder.ON ? Recorder.FRAME : Gdx.graphics.getDeltaTime(); // (filming: every frame the same)
        if (screen != null) {
            screen.render(delta);
        }
        banner.update(this, delta);
        banner.draw(this);
        if (Recorder.ON) {
            Recorder.capture();
        }
    }

    @Override
    public void dispose() {
        if (getScreen() instanceof GameScreen) {
            ((GameScreen) getScreen()).saveIfBetweenFloors(); // closing the window after a boss keeps the run (and records count)
        }
        Flufficon.flush(); // (kills since the last thing worth writing down)
        Recorder.stop();
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
