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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ScreenUtils;
import com.trush.game.enemy.Enemy;
import com.trush.game.gfx.Draw;
import com.trush.game.input.Gamepad;
import com.trush.game.input.Gamepad.Button;
import com.trush.game.launcher.Dev;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Player;
import com.trush.game.save.RunSave;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import com.trush.game.world.Room;
import com.trush.game.world.Stage;
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

    private static final Color WHEEL_SLOT = Color.valueOf("2a2025");

    private static final float MUSIC_VOLUME = 0.35f;
    /** The world runs this much slower while the hero is dying, like ETG's death slow-motion. */
    private static final float DEATH_SLOWMO = 0.45f;
    /** How far from the player the gamepad aim point sits. */
    private static final float PAD_AIM_DISTANCE = 1.6f;
    /** Holding Y longer than this opens the gun wheel; a shorter tap switches to the next gun. */
    private static final float WHEEL_DELAY = 0.2f;

    // no saving mid-floor (like ETG): quitting from the pause menu abandons the run
    private static final String[] PAUSE_ITEMS = {"RESUME", "RESTART", "QUIT TO MENU", "EXIT GAME"};
    private static final int RESUME = 0;
    private static final int RESTART = 1;
    private static final int QUIT_TO_MENU = 2;
    private static final int EXIT_GAME = 3;
    // after the boss: the only place a run can be saved
    private static final String[] CLEAR_ITEMS = {"NEXT FLOOR", "SAVE & QUIT TO MENU", "SAVE & EXIT GAME"};
    private static final int NEXT_FLOOR = 0;
    private static final int SAVE_TO_MENU = 1;
    private static final int SAVE_AND_EXIT = 2;

    private final ETGFanfic game;
    private final Mode mode;
    /** The run carried over from the previous floor, or null for a new run. */
    private final RunSave nextFloor;
    /** A new run started with "try again" / restart (always floor 1, unless the retryBoss dev flag is on). */
    private final boolean retry;
    private final MenuList pauseMenu = new MenuList(2.85f, 0.3f, 0.42f);
    private final MenuList clearMenu = new MenuList(1.95f, 0.3f, 0.42f);
    private GameWorld world;
    private State state = State.PLAYING;
    private float stateTimer;
    private boolean newBest;
    private final Vector3 mouse = new Vector3();
    private final Color tmpColor = new Color();
    private final Vector2 padAim = new Vector2(1, 0);
    private float rightStickIdle;
    /** Seconds Y has been held, or -1 when it isn't. */
    private float yHeld = -1f;
    private boolean wheelOpen;
    private int wheelChoice;

    private final ObjectMap<String, Music> tracks = new ObjectMap<>();
    private Music music;
    private Music fadingOut;
    private boolean musicMuted;

    /** A new run started from the main menu. */
    public GameScreen(ETGFanfic game, Mode mode) {
        this(game, mode, false);
    }

    private GameScreen(ETGFanfic game, Mode mode, boolean retry) {
        this.game = game;
        this.mode = mode;
        this.nextFloor = null;
        this.retry = retry;
    }

    /** Starts the next floor of a story run (continued from a save, or straight after a boss). */
    public GameScreen(ETGFanfic game, RunSave nextFloor) {
        this.game = game;
        this.mode = Mode.STORY;
        this.nextFloor = nextFloor;
        this.retry = false;
    }

    @Override
    public void show() {
        pauseMenu.setItems(PAUSE_ITEMS);
        clearMenu.setItems(CLEAR_ITEMS);
        if (nextFloor != null) {
            world = nextFloor.startFloor(game);
        } else {
            boolean atBoss = retry ? Dev.flag("retryBoss") : Dev.flag("bossRoom"); // dev builds only
            world = mode == Mode.STORY ? new GameWorld(game, new StoryDirector(Math.max(1, Dev.number("floor", 1)), atBoss)) : new GameWorld(game, new WaveDirector());
        }

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
        updateGunWheel(delta);
        updateAim(delta);

        stateTimer += delta;
        if (state == State.PLAYING && !wheelOpen) { // like ETG, the world freezes while picking a gun
            Player p = world.getPlayer();
            world.update(p.isDead() ? delta * DEATH_SLOWMO : delta);
            if (p.isDeathAnimationDone()) {
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
        if (wheelOpen) {
            drawGunWheel(game.batch);
        } else if (state == State.PAUSED) {
            drawPause(game.batch);
        } else if (state == State.GAME_OVER) {
            drawGameOver(game.batch);
        } else if (state == State.STAGE_CLEAR) {
            drawStageClear(game.batch);
        }
        game.batch.end();
    }

    private void handleStateInput() {
        Gamepad pad = game.pad;
        switch (state) {
            case PLAYING:
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)
                        || pad.justPressed(Button.START)) {
                    closeGunWheel();
                    openPause();
                }
                break;
            case PAUSED:
                int chosen = pauseMenu.update(game);
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)
                        || pad.justPressed(Button.START) || pad.justPressed(Button.B)) {
                    chosen = RESUME;
                }
                switch (chosen) {
                    case RESUME:
                        state = State.PLAYING;
                        resumeMusic();
                        break;
                    case RESTART:
                        restart(mode);
                        break;
                    case QUIT_TO_MENU:
                        toMenu();
                        break;
                    case EXIT_GAME:
                        Gdx.app.exit();
                        break;
                    default:
                        break;
                }
                break;
            case GAME_OVER:
                if (stateTimer > 1.2f) {
                    if (Gdx.input.isKeyJustPressed(Input.Keys.R) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                            || Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) || pad.justPressed(Button.A)) {
                        restart(mode);
                    } else if (Gdx.input.isKeyJustPressed(Input.Keys.M) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                            || pad.justPressed(Button.B) || pad.justPressed(Button.BACK)) {
                        toMenu();
                    }
                }
                break;
            case STAGE_CLEAR:
                if (stateTimer > 1.2f) {
                    switch (clearMenu.update(game)) {
                        case NEXT_FLOOR:
                            game.setScreen(new GameScreen(game, floorSave()));
                            dispose();
                            break;
                        case SAVE_TO_MENU:
                            saveRun();
                            toMenu();
                            break;
                        case SAVE_AND_EXIT:
                            saveRun();
                            Gdx.app.exit();
                            break;
                        default:
                            break;
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

    /** Mouse aims at the cursor; the right stick aims in its direction (or along the left stick if idle). */
    private void updateAim(float delta) {
        Player p = world.getPlayer();
        Gamepad pad = game.pad;
        if (!pad.isActive()) {
            mouse.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            game.viewport.unproject(mouse);
            world.getAim().set(mouse.x, mouse.y);
            return;
        }
        if (!wheelOpen) {
            if (!pad.rightStick().isZero()) {
                padAim.set(pad.rightStick()).nor();
                rightStickIdle = 0;
            } else {
                rightStickIdle += delta;
                if (rightStickIdle > 0.5f && !pad.leftStick().isZero()) {
                    padAim.set(pad.leftStick()).nor();
                }
            }
        }
        world.getAim().set(p.centerX() + padAim.x * PAD_AIM_DISTANCE, p.hitboxY() + padAim.y * PAD_AIM_DISTANCE);
    }

    /** Tap Y: next gun. Hold Y: gun wheel, pick with a stick and release Y to equip. */
    private void updateGunWheel(float delta) {
        Gamepad pad = game.pad;
        Player p = world.getPlayer();
        if (state != State.PLAYING || p.isDead()) {
            closeGunWheel();
            return;
        }
        if (pad.justPressed(Button.Y)) {
            yHeld = 0;
            wheelChoice = p.getWeaponIndex();
        } else if (yHeld < 0) {
            return;
        }
        int n = p.getWeapons().size;
        if (pad.pressed(Button.Y)) {
            yHeld += delta;
            if (yHeld >= WHEEL_DELAY && n > 1) {
                wheelOpen = true;
                Vector2 s = !pad.rightStick().isZero() ? pad.rightStick() : pad.leftStick();
                if (s.len() > 0.5f) {
                    // slot 0 is at the top, the rest go clockwise
                    float clockwise = (90f - s.angleDeg() + 360f) % 360f;
                    wheelChoice = Math.round(clockwise / (360f / n)) % n;
                }
            }
        } else {
            if (wheelOpen) {
                p.selectWeapon(wheelChoice);
            } else {
                p.cycleWeapon(1);
            }
            closeGunWheel();
        }
    }

    private void closeGunWheel() {
        yHeld = -1f;
        wheelOpen = false;
    }

    private void openPause() {
        state = State.PAUSED;
        pauseMenu.select(RESUME);
        pauseMusic();
    }

    private RunSave floorSave() {
        return RunSave.afterFloor(world, ((StoryDirector) world.getDirector()).getFloor());
    }

    /** Runs can only be saved between floors: on the stage clear screen after the boss. */
    private void saveRun() {
        if (state == State.STAGE_CLEAR && world.getDirector() instanceof StoryDirector) {
            RunSave save = floorSave();
            save.write(game.prefs);
        }
    }

    /** Called when the window is closed; saves only if the boss was just beaten. */
    public void saveIfBetweenFloors() {
        saveRun();
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

    /** "Try again" / restart: a brand new run from floor 1, like ETG. */
    private void restart(Mode m) {
        game.setScreen(new GameScreen(game, m, true));
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
        // second row: blanks, then keys (always shown in story mode, where chests need them)
        boolean showKeys = p.getKeys() > 0 || world.getDirector() instanceof StoryDirector;
        float keysX = 0.14f + p.getBlanks() * 0.21f + (p.getBlanks() > 0 ? 0.06f : 0f);
        float rowW = showKeys ? keysX + 0.52f - 0.12f : p.getBlanks() * 0.21f;
        boolean secondRow = p.getBlanks() > 0 || showKeys;
        float panelW = Math.max(heartCount * (hs + 0.04f), rowW) + 0.12f;
        d.rect(b, 0.04f, secondRow ? 4.38f : 4.62f, panelW, secondRow ? 0.6f : 0.36f, HUD_PANEL, 0.6f);
        for (int i = 0; i < heartCount; i++) {
            int hpHere = p.getHealth() - i * 2;
            Texture t = hpHere >= 2 ? d.heartFull : hpHere == 1 ? d.heartHalf : d.heartEmpty;
            d.texture(b, t, 0.12f + i * (hs + 0.04f), 4.95f - hs * 8f / 9f, hs, hs * 8f / 9f);
        }
        float bs = 0.17f;
        for (int i = 0; i < p.getBlanks(); i++) {
            d.texture(b, d.blankIcon, 0.14f + i * (bs + 0.04f), 4.6f - bs, bs, bs);
        }
        if (showKeys) {
            d.texture(b, d.keyIcon, keysX, 4.6f - bs + 0.03f, 0.26f, 0.13f);
            game.text("x" + p.getKeys(), keysX + 0.3f, 4.6f - 0.03f, 0.26f, HUD_TEXT, Align.left);
        }

        // what the interact button would do here (open a chest, go down the exit)
        if (world.getPrompt() != null && state == State.PLAYING) {
            String button = game.pad.isActive() ? "A  " : "E  ";
            game.text(button + world.getPrompt(), world.getPromptPos().x, world.getPromptPos().y, 0.24f, BANNER, Align.center);
        }

        // top-right: stage + minimap, or wave counter
        d.rect(b, 5.35f, 4.6f, 1.61f, 0.38f, HUD_PANEL, 0.6f);
        if (world.getDirector() instanceof WaveDirector) {
            game.text("WAVE " + ((WaveDirector) world.getDirector()).getWave(), 6.88f, 4.93f, 0.4f, HUD_TEXT, Align.right);
        } else {
            game.text(((StoryDirector) world.getDirector()).stageTitle(), 6.88f, 4.93f, 0.32f, HUD_TEXT, Align.right);
            if (!world.getRoom().isLocked()) { // like ETG, no minimap while the doors are shut for a fight
                drawMinimap(b, d);
            }
        }
        game.text("SCORE " + world.getScore(), 6.88f, 4.77f, 0.32f, HUD_DIM, Align.right);

        drawWeaponPanel(b, d, p);
        if (game.pad.isActive() && state == State.PLAYING && !p.isDead()) { // the mouse cursor is no help here
            game.draw.ring(b, world.getAim().x, world.getAim().y, 0.08f, HUD_TEXT, 0.8f);
            game.draw.circle(b, world.getAim().x, world.getAim().y, 0.02f, HUD_TEXT, 0.8f);
        }
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
            String hint = game.pad.isActive()
                    ? "LS move  RS aim  RB shoot  LB roll  A flip/open  R3 blank  X reload  hold Y guns"
                    : "WASD move  MOUSE shoot  SPACE roll  E flip/open  Q blank  R reload";
            game.text(hint, Arena.WORLD_W / 2f, 0.25f, 0.22f, HUD_DIM, Align.center);
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
        // compact bottom-right panel: name, magazine pips, ammo count, switch hint
        float right = 6.9f;
        boolean several = p.getWeapons().size > 1;
        float bottom = several ? 0.14f : 0.25f;
        d.rect(b, right - 1.25f, bottom, 1.31f, 0.63f - bottom, HUD_PANEL, 0.45f);
        game.text(w.getName().toUpperCase(), right, 0.58f, 0.24f, HUD_TEXT, Align.right);

        int mag = w.getMagSize();
        float gap = 0.012f;
        float pipW = Math.min(0.04f, 1.1f / mag - gap);
        float x = right - mag * (pipW + gap) + gap;
        for (int i = 0; i < mag; i++) {
            boolean full = i < w.getMag() && !w.isReloading();
            d.rect(b, x + i * (pipW + gap), 0.41f, pipW, 0.06f, full ? AMMO_PIP : AMMO_PIP_EMPTY);
        }
        String ammo = w.isReloading() ? "RELOADING" : w.ammoText();
        game.text(ammo, right, 0.36f, 0.22f, w.isEmpty() ? Color.RED : HUD_DIM, Align.right);
        if (several) {
            game.text((p.getWeaponIndex() + 1) + "/" + p.getWeapons().size
                    + (game.pad.isActive() ? "  HOLD Y" : "  SCROLL/TAB"), right, 0.22f, 0.18f, HUD_DIM, Align.right);
        }
    }

    private void drawBossBar(SpriteBatch b, Draw d) {
        for (Enemy e : world.getEnemies()) {
            if (e.boss && !e.isSpawning()) {
                float w = 3.6f;
                float x = 1.45f;
                float y = 4.64f;
                d.rect(b, x - 0.03f, y - 0.03f, w + 0.06f, 0.16f, HUD_PANEL);
                boolean rage = e.isEnraged();
                d.rect(b, x, y, w * Math.max(0, e.hp / e.maxHp), 0.1f, rage ? BOSS_BAR_RAGE : BOSS_BAR);
                game.text(e.bossName(), x + w / 2f, y + 0.3f, 0.3f, HUD_TEXT, Align.center);
                return;
            }
        }
    }

    private void drawGunWheel(SpriteBatch b) {
        Draw d = game.draw;
        Player p = world.getPlayer();
        d.rect(b, 0, 0, Arena.WORLD_W, Arena.WORLD_H, Color.BLACK, 0.55f);
        float cx = Arena.WORLD_W / 2f;
        float cy = Arena.WORLD_H / 2f - 0.1f;
        int n = p.getWeapons().size;
        float radius = n <= 2 ? 0.9f : 1.35f;
        for (int i = 0; i < n; i++) {
            Weapon w = p.getWeapons().get(i);
            float a = 90f - 360f * i / n;
            float x = cx + MathUtils.cosDeg(a) * radius;
            float y = cy + MathUtils.sinDeg(a) * radius;
            boolean sel = i == wheelChoice;
            float sw = 1.5f;
            float sh = 0.62f;
            if (sel) {
                d.rect(b, x - sw / 2f - 0.03f, y - sh / 2f - 0.03f, sw + 0.06f, sh + 0.06f, BANNER);
            }
            d.rect(b, x - sw / 2f, y - sh / 2f, sw, sh, WHEEL_SLOT);
            float gl = w.getGunLength() * 1.6f;
            d.line(b, x - gl / 2f, y + 0.12f, gl, w.getGunThickness() * 1.6f, 0, w.getGunColor());
            game.text(w.getName().toUpperCase(), x, y - 0.02f, 0.26f, sel ? BANNER : HUD_TEXT, Align.center);
            game.text(w.ammoText(), x, y - 0.17f, 0.22f, w.isEmpty() ? Color.RED : HUD_DIM, Align.center);
        }
        game.text("STICK choose   release Y equip", cx, 0.3f, 0.26f, HUD_DIM, Align.center);
    }

    private void drawPause(SpriteBatch b) {
        game.draw.rect(b, 0, 0, Arena.WORLD_W, Arena.WORLD_H, Color.BLACK, 0.6f);
        game.text("PAUSED", Arena.WORLD_W / 2f, 3.5f, 1f, BANNER, Align.center);
        pauseMenu.draw(game, stateTimer);
        String hint = game.pad.isActive() ? "A choose   START / B resume" : "ENTER choose   ESC resume   N toggle music";
        game.text(hint, Arena.WORLD_W / 2f, 1.15f, 0.28f, HUD_DIM, Align.center);
    }

    private void drawGameOver(SpriteBatch b) {
        float a = Math.min(1f, stateTimer / 0.8f);
        game.draw.rect(b, 0, 0, Arena.WORLD_W, Arena.WORLD_H, Color.BLACK, 0.7f * a);
        game.text("YOU DIED", Arena.WORLD_W / 2f, 3.5f, 1.2f, tmpColor.set(Color.RED).mul(1, 1, 1, a), Align.center);
        String where = world.getDirector() instanceof WaveDirector
                ? "Reached wave " + ((WaveDirector) world.getDirector()).getWave()
                : "Fell in " + ((StoryDirector) world.getDirector()).getStage().title.toLowerCase();
        game.text(where + "   Kills " + world.getKills(), Arena.WORLD_W / 2f, 2.75f, 0.4f, HUD_TEXT, Align.center);
        game.text("Score " + world.getScore() + (newBest ? "   NEW BEST!" : "   Best " + game.getHighScore()),
                Arena.WORLD_W / 2f, 2.45f, 0.4f, newBest ? Color.GOLD : HUD_TEXT, Align.center);
        if (stateTimer > 1.2f) {
            game.text(game.pad.isActive() ? "A try again   B menu" : "Click or R to try again   M for menu", Arena.WORLD_W / 2f, 1.8f, 0.36f, HUD_DIM, Align.center);
        }
    }

    private void drawStageClear(SpriteBatch b) {
        float a = Math.min(1f, stateTimer / 1f);
        game.draw.rect(b, 0, 0, Arena.WORLD_W, Arena.WORLD_H, Color.BLACK, 0.75f * a);
        int floor = ((StoryDirector) world.getDirector()).getFloor();
        game.text("FLOOR " + floor + " CLEAR!", Arena.WORLD_W / 2f, 3.75f, 1.1f, tmpColor.set(BANNER).mul(1, 1, 1, a), Align.center);
        Stage stage = ((StoryDirector) world.getDirector()).getStage();
        Stage next = Stage.forFloor(floor + 1);
        game.text(stage.clearText, Arena.WORLD_W / 2f, 3.1f, 0.4f, HUD_TEXT, Align.center);
        // after the last stage, floors replay it with tougher enemies
        game.text("Next: floor " + (floor + 1) + " - " + (next != stage ? next.title : stage.title.toLowerCase() + ", but meaner"),
                Arena.WORLD_W / 2f, 2.8f, 0.34f, HUD_DIM, Align.center);
        game.text("Score " + world.getScore() + "   Kills " + world.getKills() + (newBest ? "   NEW BEST!" : ""),
                Arena.WORLD_W / 2f, 2.45f, 0.4f, newBest ? Color.GOLD : HUD_TEXT, Align.center);
        if (stateTimer > 1.2f) {
            clearMenu.draw(game, stateTimer);
        }
    }

    @Override
    public void resize(int width, int height) {
        game.viewport.update(width, height, true);
    }

    @Override
    public void pause() {
        if (state == State.PLAYING) {
            closeGunWheel();
            openPause();
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
