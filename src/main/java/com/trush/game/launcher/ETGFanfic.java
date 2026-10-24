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
import com.trush.game.save.Unlocks;
import com.trush.game.audio.Sfx;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;
import com.trush.game.input.Gamepad;
import com.trush.game.screen.GameScreen;
import com.trush.game.screen.MainMenuScreen;

public class ETGFanfic extends Game {

    public SpriteBatch batch;
    public BitmapFont font;
    public FitViewport viewport;
    public Draw draw;
    public Sprites sprites;
    public Sfx sfx;
    public Gamepad pad;
    public Preferences prefs;

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

        String play = Dev.text("play"); // dev builds: -Dfluffy.play=story / endless skips the main menu
        if ("story".equalsIgnoreCase(play) || "endless".equalsIgnoreCase(play)) {
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
