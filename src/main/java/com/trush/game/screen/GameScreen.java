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
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ScreenUtils;
import com.trush.game.enemy.Enemy;
import com.trush.game.enemy.FluffyKing;
import com.trush.game.gfx.Draw;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import com.trush.game.world.Room;
import com.trush.game.world.StageMap;
import com.trush.game.world.StoryDirector;
import com.trush.game.world.WaveDirector;

/** Runs a story stage or endless mode: input states, camera shake, music, HUD and overlays. */
public class GameScreen implements Screen {

    public enum Mode { STORY, ENDLESS }

    private enum State { PLAYING, PAUSED, GAME_OVER, STAGE_CLEAR }

    private static final Color HUD_TEXT = Color.valueOf("f5f0e6");
    private static final Color HUD_DIM = Color.valueOf("b5a99a");
    private static final Color HUD_PANEL = Color.valueOf("120c10");
    private static final Color BOSS_BAR = Color.valueOf("e84393");
    private static final Color BOSS_BAR_RAGE = Color.valueOf("ff3b3b");
    private static final Color AMMO_PIP = Color.valueOf("ffe066");
    private static final Color AMMO_PIP_EMPTY = Color.valueOf("4a4036");
    private static final Color BANNER = Color.valueOf("ffd166");
    private static final Color MAP_ROOM = Color.valueOf("8a7f72");
    private static final Color MAP_SEEN = Color.valueOf("3c3631");
    private static final Color MAP_HERE = Color.valueOf("f5f0e6");
    private static final Color MAP_BOSS = Color.valueOf("e84393");
    private static final Color MAP_TREASURE = Color.valueOf("ffd166");

    private static final float MUSIC_VOLUME = 0.35f;

    private final ETGFanfic game;
    private final Mode mode;
    private GameWorld world;
    private State state = State.PLAYING;
    private float stateTimer;
    private boolean newBest;
    private final Vector3 mouse = new Vector3();
    private final Color tmpColor = new Color();

    private final ObjectMap<String, Music> tracks = new ObjectMap<>();
    private Music music;
    private Music fadingOut;
    private boolean musicMuted;

    public GameScreen(ETGFanfic game, Mode mode) {
        this.game = game;
        this.mode = mode;
    }

    @Override
    public void show() {
        world = mode == Mode.STORY ? new GameWorld(game, new StoryDirector()) : new GameWorld(game, new WaveDirector());

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

    // ------------------------------------------------------------------ music

    private Music track(String name) {
        Music m = tracks.get(name);
        if (m == null) {
            m = Gdx.audio.newMusic(Gdx.files.internal("assets/music/" + name + ".ogg"));
            m.setLooping(true);
            tracks.put(name, m);
        }
        return m;
    }

    /** Crossfades to the director's track. */
    private void updateMusic(float delta) {
        Music wanted = track(world.getDirector().music());
        if (wanted != music) {
            if (fadingOut != null) {
                fadingOut.stop();
            }
            fadingOut = music;
            music = wanted;
            music.setVolume(0f);
            music.setPosition(0f);
            if (!musicMuted && state != State.GAME_OVER) {
                music.play();
            }
        }
        if (music != null && music.getVolume() < MUSIC_VOLUME) {
            music.setVolume(Math.min(MUSIC_VOLUME, music.getVolume() + delta * 0.6f));
        }
        if (fadingOut != null) {
            float v = fadingOut.getVolume() - delta * 0.8f;
            if (v <= 0) {
                fadingOut.stop();
                fadingOut = null;
            } else {
                fadingOut.setVolume(v);
            }
        }
    }

    private void pauseMusic() {
        if (music != null) music.pause();
        if (fadingOut != null) {
            fadingOut.stop();
            fadingOut = null;
        }
    }

    private void resumeMusic() {
        if (music != null && !musicMuted) music.play();
    }

    // ------------------------------------------------------------------ loop

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

        stateTimer += delta;
        if (state == State.PLAYING) {
            world.update(delta);
            if (world.getPlayer().isDead()) {
                onRunEnded(State.GAME_OVER);
            } else if (world.getDirector().isFinished()) {
                onRunEnded(State.STAGE_CLEAR);
            }
        } else if (state != State.PAUSED) {
            world.particles().update(delta);
        }
        if (state != State.PAUSED && state != State.GAME_OVER) {
            updateMusic(delta);
        }

        ScreenUtils.clear(Color.BLACK);
        game.viewport.apply();

        float shake = state == State.PLAYING ? world.getTrauma() * world.getTrauma() * 0.12f : 0f;
        cam.position.add(MathUtils.random(-shake, shake), MathUtils.random(-shake, shake), 0);
        cam.update();
        game.batch.setProjectionMatrix(cam.combined);
        game.batch.begin();
        world.draw(game.batch);
        game.batch.end();

        cam.position.set(Arena.WORLD_W / 2f, Arena.WORLD_H / 2f, 0);
        cam.update();
        game.batch.setProjectionMatrix(cam.combined);
        game.batch.begin();
        if (world.getFade() > 0) {
            game.draw.rect(game.batch, 0, 0, Arena.WORLD_W, Arena.WORLD_H, Color.BLACK, world.getFade());
        }
        drawHud(game.batch);
        if (state == State.PAUSED) {
            drawPause(game.batch);
        } else if (state == State.GAME_OVER) {
            drawGameOver(game.batch);
        } else if (state == State.STAGE_CLEAR) {
            drawStageClear(game.batch);
        }
        game.batch.end();
    }

    private void handleStateInput() {
        switch (state) {
            case PLAYING:
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)) {
                    state = State.PAUSED;
                    pauseMusic();
                }
                break;
            case PAUSED:
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)) {
                    state = State.PLAYING;
                    resumeMusic();
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                    toMenu();
                } else if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
                    restart(mode);
                }
                break;
            case GAME_OVER:
            case STAGE_CLEAR:
                if (stateTimer > 1.2f) {
                    if (state == State.GAME_OVER && (Gdx.input.isKeyJustPressed(Input.Keys.R)
                            || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                            || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT))) {
                        restart(mode);
                    } else if (state == State.STAGE_CLEAR && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                        restart(Mode.ENDLESS);
                    } else if (Gdx.input.isKeyJustPressed(Input.Keys.M) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                            || (state == State.STAGE_CLEAR && Gdx.input.isKeyJustPressed(Input.Keys.ENTER))) {
                        toMenu();
                    }
                }
                break;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            musicMuted = !musicMuted;
            if (musicMuted) pauseMusic();
            else if (state == State.PLAYING) resumeMusic();
        }
    }

    private void onRunEnded(State end) {
        state = end;
        stateTimer = 0;
        if (end == State.GAME_OVER) {
            pauseMusic();
        }
        newBest = world.getScore() > game.getHighScore();
        int wave = world.getDirector() instanceof WaveDirector ? ((WaveDirector) world.getDirector()).getWave() : 0;
        game.submitRun(world.getScore(), wave);
    }

    private void restart(Mode m) {
        game.setScreen(new GameScreen(game, m));
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
        for (int i = 0; i < heartCount; i++) {
            int hpHere = p.getHealth() - i * 2;
            Texture t = hpHere >= 2 ? d.heartFull : hpHere == 1 ? d.heartHalf : d.heartEmpty;
            d.texture(b, t, 0.12f + i * (hs + 0.04f), 4.95f - hs * 8f / 9f, hs, hs * 8f / 9f);
        }
        float bs = 0.17f;
        for (int i = 0; i < p.getBlanks(); i++) {
            d.texture(b, d.blankIcon, 0.14f + i * (bs + 0.04f), 4.6f - bs, bs, bs);
        }

        // top-right: stage + minimap, or wave counter
        d.rect(b, 5.35f, 4.6f, 1.61f, 0.38f, HUD_PANEL, 0.6f);
        if (world.getDirector() instanceof WaveDirector) {
            game.text("WAVE " + ((WaveDirector) world.getDirector()).getWave(), 6.88f, 4.93f, 0.4f, HUD_TEXT, Align.right);
        } else {
            game.text("1. " + StoryDirector.STAGE_NAME, 6.88f, 4.93f, 0.32f, HUD_TEXT, Align.right);
            drawMinimap(b, d);
        }
        game.text("SCORE " + world.getScore(), 6.88f, 4.77f, 0.32f, HUD_DIM, Align.right);

        drawWeaponPanel(b, d, p);
        drawBossBar(b, d);

        String banner = world.getDirector().banner();
        float alpha = world.getDirector().bannerAlpha();
        if (banner != null && alpha > 0) {
            d.rect(b, 0, 3.05f, Arena.WORLD_W, 0.5f, HUD_PANEL, 0.55f * alpha);
            float scale = banner.length() > 20 ? 0.6f : 0.85f;
            game.text(banner, Arena.WORLD_W / 2f, 3.43f - (0.85f - scale) * 0.15f, scale,
                    tmpColor.set(BANNER).mul(1, 1, 1, alpha), Align.center);
        }

        if (world.getToastTimer() > 0 && world.getToast() != null) {
            float a = Math.min(1f, world.getToastTimer() / 0.4f);
            game.text(world.getToast(), Arena.WORLD_W / 2f, 0.85f, 0.4f, tmpColor.set(Color.GOLD).mul(1, 1, 1, a), Align.center);
        }

        if (world.getKills() == 0 && state == State.PLAYING && world.getEnemies().size == 0) {
            d.rect(b, 0.55f, 0.06f, 5.9f, 0.24f, HUD_PANEL, 0.7f);
            game.text("WASD move  MOUSE shoot  SPACE roll  E flip table  Q blank  R reload",
                    Arena.WORLD_W / 2f, 0.25f, 0.22f, HUD_DIM, Align.center);
        }
    }

    private void drawMinimap(SpriteBatch b, Draw d) {
        StageMap map = world.getMap();
        if (map == null) return;
        int minX = 99, maxX = -99, minY = 99, maxY = -99;
        for (Room r : map.rooms) {
            if (!map.isKnown(r)) continue;
            minX = Math.min(minX, r.gx);
            maxX = Math.max(maxX, r.gx);
            minY = Math.min(minY, r.gy);
            maxY = Math.max(maxY, r.gy);
        }
        float cell = 0.17f;
        float gap = 0.05f;
        float step = cell + gap;
        float w = (maxX - minX + 1) * step + gap;
        float h = (maxY - minY + 1) * step + gap;
        float right = 6.92f;
        float top = 4.56f;
        float x0 = right - w;
        float y0 = top - h;
        d.rect(b, x0 - 0.03f, y0 - 0.03f, w + 0.06f, h + 0.06f, HUD_PANEL, 0.6f);
        Room here = world.getRoom();
        for (Room r : map.rooms) {
            if (!map.isKnown(r)) continue;
            float rx = x0 + gap + (r.gx - minX) * step;
            float ry = y0 + gap + (r.gy - minY) * step;
            // corridors
            if (r.visited) {
                if (r.doors[Room.E]) d.rect(b, rx + cell, ry + cell / 2f - 0.02f, gap, 0.04f, MAP_ROOM);
                if (r.doors[Room.N]) d.rect(b, rx + cell / 2f - 0.02f, ry + cell, 0.04f, gap, MAP_ROOM);
            }
            Color c = r == here ? MAP_HERE : r.visited ? MAP_ROOM : MAP_SEEN;
            d.rect(b, rx, ry, cell, cell, c);
            if (r.type == Room.Type.BOSS) {
                d.rect(b, rx + 0.05f, ry + 0.05f, cell - 0.1f, cell - 0.1f, MAP_BOSS);
            } else if (r.type == Room.Type.TREASURE) {
                d.rect(b, rx + 0.05f, ry + 0.05f, cell - 0.1f, cell - 0.1f, MAP_TREASURE);
            }
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

        int mag = w.getMagSize();
        float pipW = Math.min(0.05f, 1.6f / mag - 0.015f);
        float x = right - mag * (pipW + 0.015f);
        for (int i = 0; i < mag; i++) {
            boolean full = i < w.getMag() && !w.isReloading();
            d.rect(b, x + i * (pipW + 0.015f), y + 0.3f, pipW, 0.09f, full ? AMMO_PIP : AMMO_PIP_EMPTY);
        }
        if (p.getWeapons().size > 1) {
            game.text((p.getWeaponIndex() + 1) + "/" + p.getWeapons().size + "  SCROLL/TAB", right, y - 0.16f, 0.22f, HUD_DIM, Align.right);
        }
    }

    private void drawBossBar(SpriteBatch b, Draw d) {
        for (Enemy e : world.getEnemies()) {
            if (e.boss && !e.isSpawning()) {
                float w = 3.6f;
                float x = 1.45f;
                float y = 4.64f;
                d.rect(b, x - 0.03f, y - 0.03f, w + 0.06f, 0.16f, HUD_PANEL);
                boolean rage = e instanceof FluffyKing && ((FluffyKing) e).isEnraged();
                d.rect(b, x, y, w * Math.max(0, e.hp / e.maxHp), 0.1f, rage ? BOSS_BAR_RAGE : BOSS_BAR);
                game.text("THE FLUFFY KING", x + w / 2f, y + 0.3f, 0.3f, HUD_TEXT, Align.center);
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
        float a = Math.min(1f, stateTimer / 0.8f);
        game.draw.rect(b, 0, 0, Arena.WORLD_W, Arena.WORLD_H, Color.BLACK, 0.7f * a);
        game.text("YOU DIED", Arena.WORLD_W / 2f, 3.5f, 1.2f, tmpColor.set(Color.RED).mul(1, 1, 1, a), Align.center);
        String where = world.getDirector() instanceof WaveDirector
                ? "Reached wave " + ((WaveDirector) world.getDirector()).getWave()
                : "Fell in " + StoryDirector.STAGE_NAME.toLowerCase();
        game.text(where + "   Kills " + world.getKills(), Arena.WORLD_W / 2f, 2.75f, 0.4f, HUD_TEXT, Align.center);
        game.text("Score " + world.getScore() + (newBest ? "   NEW BEST!" : "   Best " + game.getHighScore()),
                Arena.WORLD_W / 2f, 2.45f, 0.4f, newBest ? Color.GOLD : HUD_TEXT, Align.center);
        if (stateTimer > 1.2f) {
            game.text("Click or R to try again   M for menu", Arena.WORLD_W / 2f, 1.8f, 0.36f, HUD_DIM, Align.center);
        }
    }

    private void drawStageClear(SpriteBatch b) {
        float a = Math.min(1f, stateTimer / 1f);
        game.draw.rect(b, 0, 0, Arena.WORLD_W, Arena.WORLD_H, Color.BLACK, 0.75f * a);
        game.text("STAGE 1 CLEAR!", Arena.WORLD_W / 2f, 3.6f, 1.1f, tmpColor.set(BANNER).mul(1, 1, 1, a), Align.center);
        game.text("The village is safe... for now.", Arena.WORLD_W / 2f, 2.95f, 0.4f, HUD_TEXT, Align.center);
        game.text("Next: 2. THE SMALL TOWN  (coming soon)", Arena.WORLD_W / 2f, 2.65f, 0.36f, HUD_DIM, Align.center);
        game.text("Score " + world.getScore() + "   Kills " + world.getKills() + (newBest ? "   NEW BEST!" : ""),
                Arena.WORLD_W / 2f, 2.25f, 0.4f, newBest ? Color.GOLD : HUD_TEXT, Align.center);
        if (stateTimer > 1.2f) {
            game.text("ENTER  main menu     E  endless mode", Arena.WORLD_W / 2f, 1.6f, 0.36f, HUD_DIM, Align.center);
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
            pauseMusic();
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
        for (Music m : tracks.values()) {
            m.dispose();
        }
        tracks.clear();
        world.dispose();
    }
}
