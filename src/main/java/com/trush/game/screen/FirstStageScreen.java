package com.trush.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.trush.game.enemy.Enemy;
import com.trush.game.enemy.FluffyKing;
import com.trush.game.gfx.Draw;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

public class FirstStageScreen implements Screen {

    private enum State { PLAYING, PAUSED, GAME_OVER }

    private static final Color HUD_TEXT = Color.valueOf("f5f0e6");
    private static final Color HUD_DIM = Color.valueOf("b5a99a");
    private static final Color HUD_PANEL = Color.valueOf("120c10");
    private static final Color BOSS_BAR = Color.valueOf("e84393");
    private static final Color BOSS_BAR_RAGE = Color.valueOf("ff3b3b");
    private static final Color AMMO_PIP = Color.valueOf("ffe066");
    private static final Color AMMO_PIP_EMPTY = Color.valueOf("4a4036");
    private static final Color BANNER = Color.valueOf("ffd166");

    private final ETGFanfic game;
    private Texture backgroundTexture;
    private Sprite floor;
    private Music music;
    private GameWorld world;
    private State state = State.PLAYING;
    private float gameOverTimer;
    private boolean newBest;
    private final Vector3 mouse = new Vector3();
    private final Color tmpColor = new Color();

    public FirstStageScreen(ETGFanfic game) {
        this.game = game;
    }

    @Override
    public void show() {
        backgroundTexture = new Texture("assets/floor.png");
        backgroundTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        floor = new Sprite(backgroundTexture);
        floor.setPosition(0, 0);
        floor.setSize(game.viewport.getWorldWidth(), game.viewport.getWorldHeight());

        world = new GameWorld(game);

        music = Gdx.audio.newMusic(Gdx.files.internal("assets/Enter the Gungeon room_one.mp3"));
        music.setLooping(true);
        music.setVolume(.12f);
        music.play();

        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Crosshair);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (state == State.PLAYING && amountY != 0) {
                    world.getPlayer().cycleWeapon(amountY > 0 ? 1 : -1);
                }
                return true;
            }
        });
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 30f); // avoid tunnelling through things on frame hitches
        handleStateInput();
        if (game.getScreen() != this) {
            return; // we switched screens this frame
        }

        // aim is computed with the un-shaken camera
        OrthographicCamera cam = (OrthographicCamera) game.viewport.getCamera();
        cam.position.set(Arena.WORLD_W / 2f, Arena.WORLD_H / 2f, 0);
        cam.update();
        mouse.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        game.viewport.unproject(mouse);
        world.getAim().set(mouse.x, mouse.y);

        if (state == State.PLAYING) {
            world.update(delta);
            if (world.getPlayer().isDead()) {
                onPlayerDied();
            }
        } else if (state == State.GAME_OVER) {
            gameOverTimer += delta;
            world.particles().update(delta);
        }

        ScreenUtils.clear(Color.BLACK);
        game.viewport.apply();

        float shake = state == State.PLAYING ? world.getTrauma() * world.getTrauma() * 0.12f : 0f;
        cam.position.add(MathUtils.random(-shake, shake), MathUtils.random(-shake, shake), 0);
        cam.update();
        game.batch.setProjectionMatrix(cam.combined);
        game.batch.begin();
        floor.draw(game.batch);
        world.draw(game.batch);
        game.batch.end();

        cam.position.set(Arena.WORLD_W / 2f, Arena.WORLD_H / 2f, 0);
        cam.update();
        game.batch.setProjectionMatrix(cam.combined);
        game.batch.begin();
        drawHud(game.batch);
        if (state == State.PAUSED) {
            drawPause(game.batch);
        } else if (state == State.GAME_OVER) {
            drawGameOver(game.batch);
        }
        game.batch.end();
    }

    private void handleStateInput() {
        switch (state) {
            case PLAYING:
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)) {
                    state = State.PAUSED;
                    music.pause();
                }
                break;
            case PAUSED:
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)) {
                    state = State.PLAYING;
                    music.play();
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                    toMenu();
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                    restart();
                }
                break;
            case GAME_OVER:
                if (gameOverTimer > 1.2f) {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.R) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                            || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
                        restart();
                    } else if (Gdx.input.isKeyJustPressed(Input.Keys.M) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                        toMenu();
                    }
                }
                break;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            if (music.isPlaying()) music.pause();
            else if (state == State.PLAYING) music.play();
        }
    }

    private void onPlayerDied() {
        state = State.GAME_OVER;
        gameOverTimer = 0;
        music.stop();
        newBest = world.getScore() > game.getHighScore();
        game.submitRun(world.getScore(), world.getWaves().getWave());
    }

    private void restart() {
        game.setScreen(new FirstStageScreen(game));
        dispose();
    }

    private void toMenu() {
        game.setScreen(new MainMenuScreen(game));
        dispose();
    }

    // ------------------------------------------------------------------ HUD

    private void drawHud(SpriteBatch b) {
        Draw d = game.draw;
        Player p = world.getPlayer();

        // hearts (top-left)
        float hs = 0.26f;
        int heartCount = p.getMaxHealth() / 2;
        float panelW = Math.max(heartCount * (hs + 0.04f), p.getBlanks() * 0.21f) + 0.12f;
        d.rect(b, 0.04f, p.getBlanks() > 0 ? 4.38f : 4.62f, panelW, p.getBlanks() > 0 ? 0.6f : 0.36f, HUD_PANEL, 0.6f);
        for (int i = 0; i < p.getMaxHealth() / 2; i++) {
            int hpHere = p.getHealth() - i * 2;
            Texture t = hpHere >= 2 ? d.heartFull : hpHere == 1 ? d.heartHalf : d.heartEmpty;
            d.texture(b, t, 0.12f + i * (hs + 0.04f), 4.95f - hs * 8f / 9f, hs, hs * 8f / 9f);
        }
        // blanks under the hearts
        float bs = 0.17f;
        for (int i = 0; i < p.getBlanks(); i++) {
            d.texture(b, d.blankIcon, 0.14f + i * (bs + 0.04f), 4.6f - bs, bs, bs);
        }

        // wave + score (top-right)
        d.rect(b, 5.55f, 4.6f, 1.41f, 0.38f, HUD_PANEL, 0.6f);
        game.text("WAVE " + world.getWaves().getWave(), 6.88f, 4.93f, 0.4f, HUD_TEXT, Align.right);
        game.text("SCORE " + world.getScore(), 6.88f, 4.77f, 0.32f, HUD_DIM, Align.right);

        drawWeaponPanel(b, d, p);
        drawBossBar(b, d);

        // banner
        String banner = world.getWaves().banner();
        float alpha = world.getWaves().bannerAlpha();
        if (banner != null && alpha > 0) {
            d.rect(b, 0, 3.05f, Arena.WORLD_W, 0.5f, HUD_PANEL, 0.55f * alpha);
            game.text(banner, Arena.WORLD_W / 2f, 3.43f, 0.85f, tmpColor.set(BANNER).mul(1, 1, 1, alpha), Align.center);
        }

        // pickup toast
        if (world.getToastTimer() > 0 && world.getToast() != null) {
            float a = Math.min(1f, world.getToastTimer() / 0.4f);
            game.text(world.getToast(), Arena.WORLD_W / 2f, 0.85f, 0.4f, tmpColor.set(Color.GOLD).mul(1, 1, 1, a), Align.center);
        }

        if (world.getWaves().getWave() == 1 && world.getKills() == 0 && state == State.PLAYING) {
            d.rect(b, 0.9f, 0.08f, 5.2f, 0.24f, HUD_PANEL, 0.7f);
            game.text("WASD move  MOUSE aim+shoot  SPACE/RMB roll  Q blank  R reload",
                    Arena.WORLD_W / 2f, 0.27f, 0.24f, HUD_DIM, Align.center);
        }
    }

    private void drawWeaponPanel(SpriteBatch b, Draw d, Player p) {
        Weapon w = p.currentWeapon();
        float right = 6.88f;
        float y = 0.62f;
        float bottom = p.getWeapons().size > 1 ? 0.3f : 0.45f;
        d.rect(b, 4.95f, bottom, 2.01f, 1.02f - bottom, HUD_PANEL, 0.6f);
        game.text(w.getName().toUpperCase(), right, y + 0.22f, 0.32f, HUD_TEXT, Align.right);
        String ammo = w.isReloading() ? "RELOADING" : w.ammoText();
        game.text(ammo, right, y, 0.3f, w.isEmpty() ? Color.RED : HUD_DIM, Align.right);

        // magazine pips
        int mag = w.getMagSize();
        float pipW = Math.min(0.05f, 1.6f / mag - 0.015f);
        float x = right - mag * (pipW + 0.015f);
        for (int i = 0; i < mag; i++) {
            boolean full = i < w.getMag() && !w.isReloading();
            d.rect(b, x + i * (pipW + 0.015f), y + 0.3f, pipW, 0.09f, full ? AMMO_PIP : AMMO_PIP_EMPTY);
        }

        if (p.getWeapons().size > 1) {
            game.text((p.getWeaponIndex() + 1) + "/" + p.getWeapons().size + "  SCROLL/E", right, y - 0.16f, 0.22f, HUD_DIM, Align.right);
        }
    }

    private void drawBossBar(SpriteBatch b, Draw d) {
        for (Enemy e : world.getEnemies()) {
            if (e.boss && !e.isSpawning()) {
                float w = 4f;
                float x = (Arena.WORLD_W - w) / 2f;
                float y = 4.62f;
                d.rect(b, x - 0.03f, y - 0.03f, w + 0.06f, 0.16f, HUD_PANEL);
                boolean rage = e instanceof FluffyKing && ((FluffyKing) e).isEnraged();
                d.rect(b, x, y, w * Math.max(0, e.hp / e.maxHp), 0.1f, rage ? BOSS_BAR_RAGE : BOSS_BAR);
                game.text("THE FLUFFY KING", Arena.WORLD_W / 2f, y + 0.3f, 0.32f, HUD_TEXT, Align.center);
                return;
            }
        }
    }

    private void drawPause(SpriteBatch b) {
        game.draw.rect(b, 0, 0, Arena.WORLD_W, Arena.WORLD_H, Color.BLACK, 0.6f);
        game.text("PAUSED", Arena.WORLD_W / 2f, 3.3f, 1f, BANNER, Align.center);
        game.text("ESC  resume", Arena.WORLD_W / 2f, 2.6f, 0.4f, HUD_TEXT, Align.center);
        game.text("R  restart", Arena.WORLD_W / 2f, 2.35f, 0.4f, HUD_TEXT, Align.center);
        game.text("M  main menu", Arena.WORLD_W / 2f, 2.1f, 0.4f, HUD_TEXT, Align.center);
        game.text("N  toggle music", Arena.WORLD_W / 2f, 1.85f, 0.4f, HUD_TEXT, Align.center);
    }

    private void drawGameOver(SpriteBatch b) {
        float a = Math.min(1f, gameOverTimer / 0.8f);
        game.draw.rect(b, 0, 0, Arena.WORLD_W, Arena.WORLD_H, Color.BLACK, 0.7f * a);
        game.text("YOU DIED", Arena.WORLD_W / 2f, 3.5f, 1.2f, tmpColor.set(Color.RED).mul(1, 1, 1, a), Align.center);
        game.text("Reached wave " + world.getWaves().getWave() + "   Kills " + world.getKills(),
                Arena.WORLD_W / 2f, 2.75f, 0.4f, HUD_TEXT, Align.center);
        game.text("Score " + world.getScore() + (newBest ? "   NEW BEST!" : "   Best " + game.getHighScore()),
                Arena.WORLD_W / 2f, 2.45f, 0.4f, newBest ? Color.GOLD : HUD_TEXT, Align.center);
        if (gameOverTimer > 1.2f) {
            game.text("Click or R to try again   M for menu", Arena.WORLD_W / 2f, 1.8f, 0.36f, HUD_DIM, Align.center);
        }
    }

    @Override
    public void resize(int width, int height) {
        game.viewport.update(width, height, true);
    }

    @Override
    public void pause() {
        if (state == State.PLAYING) {
            state = State.PAUSED;
            music.pause();
        }
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        music.dispose();
        world.dispose();
    }
}
