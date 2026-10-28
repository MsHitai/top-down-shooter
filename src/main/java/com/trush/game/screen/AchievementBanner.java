package com.trush.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.trush.game.gfx.Draw;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.save.Flufficon;
import com.badlogic.gdx.utils.Align;

/**
 * "ACHIEVEMENT!" - a gold banner sliding down from the top of the screen for each newly done achievement
 * (save/Flufficon), one after the other. Drawn by ETGFanfic over whatever screen is up, in screen units.
 */
public class AchievementBanner {

    private static final Color GOLD = Color.valueOf("ffd166");
    private static final Color PANEL = Color.valueOf("1e1416");
    private static final Color TEXT = Color.valueOf("f5f0e6");
    private static final float SHOW = 3.2f;
    private static final float SLIDE = 0.35f;
    private static final float W = 3.6f;
    private static final float H = 0.62f;

    private final OrthographicCamera camera = new OrthographicCamera();
    private Flufficon.Achievement showing;
    private float timer;

    public void update(ETGFanfic game, float delta) {
        Flufficon.update(game, delta);
        if (showing != null) {
            timer -= delta;
            if (timer <= 0) showing = null;
        }
        if (showing == null) {
            showing = Flufficon.nextAnnouncement();
            if (showing != null) {
                timer = SHOW;
                game.sfx.play("wave_clear", 0.6f);
            }
        }
    }

    public void draw(ETGFanfic game) {
        if (showing == null) return;
        float w = game.viewport.getWorldWidth();
        float h = game.viewport.getWorldHeight();
        camera.setToOrtho(false, w, h);
        camera.update();
        game.viewport.apply();
        SpriteBatch b = game.batch;
        b.setProjectionMatrix(camera.combined);
        b.begin();
        Draw d = game.draw;
        float in = Math.min(1f, Math.min(SHOW - timer, timer) / SLIDE);
        float top = h - 0.08f + (1f - Interpolation.pow2Out.apply(in)) * (H + 0.2f);
        float x = w / 2f - W / 2f;
        d.rect(b, x - 0.04f, top - H - 0.04f, W + 0.08f, H + 0.08f, GOLD, 0.95f);
        d.rect(b, x, top - H, W, H, PANEL, 0.95f);
        game.text("ACHIEVEMENT!", w / 2f, top - 0.08f, 0.26f, GOLD, Align.center);
        game.text(showing.title, w / 2f, top - 0.34f, 0.34f, TEXT, Align.center);
        b.end();
    }
}
