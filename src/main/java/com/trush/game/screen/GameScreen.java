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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ScreenUtils;
import com.trush.game.enemy.Enemy;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;
import com.trush.game.input.Gamepad;
import com.trush.game.input.Gamepad.Button;
import com.trush.game.launcher.Dev;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Chest;
import com.trush.game.objects.FluffulVoice;
import com.trush.game.objects.Shop;
import com.trush.game.objects.Grenade;
import com.trush.game.objects.Item;
import com.trush.game.objects.Pickup;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Hero;
import com.trush.game.player.Player;
import com.trush.game.save.Flufficon;
import com.trush.game.save.RunSave;
import com.trush.game.save.Unlocks;
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

    private enum State { PLAYING, PAUSED, GAME_OVER, STAGE_CLEAR, VICTORY }

    private static final Color HUD_TEXT = Color.valueOf("f5f0e6");
    private static final Color HUD_DIM = Color.valueOf("b5a99a");
    private static final Color HUD_PANEL = Color.valueOf("120c10");
    private static final Color BOSS_BAR = Color.valueOf("e84393");
    private static final Color BOSS_BAR_RAGE = Color.valueOf("ff3b3b");
    private static final Color BOSS_BAR_GOLD = Color.valueOf("ffc83d");
    private static final Color BOSS_BAR_GOLD_SHINE = Color.valueOf("fff0a0");
    private static final Color AMMO_PIP = Color.valueOf("ffe066");
    private static final Color AMMO_PIP_EMPTY = Color.valueOf("4a4036");
    private static final Color BANNER = Color.valueOf("ffd166");
    private static final Color EMPTY_TEXT = Color.valueOf("ff4d4d");
    private static final Color ELEVATOR_STEEL = Color.valueOf("8a909a");
    private static final Color ELEVATOR_DIGITS = Color.valueOf("ff8a3a");
    /** The blinking RELOAD / EMPTY hint (its alpha changes every frame). */
    private final Color reloadHint = new Color();
    private static final Color MAP_ROOM = Color.valueOf("8a7f72");
    private static final Color MAP_SEEN = Color.valueOf("3c3631");
    private static final Color MAP_HERE = Color.valueOf("f5f0e6");
    private static final Color MAP_BOSS = Color.valueOf("e84393");
    private static final Color MAP_TREASURE = Color.valueOf("ffd166");
    private static final Color MAP_SHOP = Color.valueOf("f2c230");
    private static final Color MAP_POM = Color.valueOf("ff8ac8");
    private static final Color MAP_EXIT = Color.valueOf("5ae06a");
    /** Any item left on the floor (gun, heart, ammo, blank...). */
    private static final Color MAP_ITEM = Color.valueOf("ff9f3a");
    private static final Color MAP_KEY = Color.valueOf("ffe066");
    /** Chest colours by tier (D brown, C blue, B green, A red, S black), as in tools/gen_sprites.py. */
    private static final Color[] TIER_COLORS = {
            Color.valueOf("8a5a2b"), Color.valueOf("3f6fc4"), Color.valueOf("3f9e4f"), Color.valueOf("c0392b"),
            Color.valueOf("2a2433"),
    };

    private static final Color WHEEL_SLOT = Color.valueOf("2a2025");

    private static final float MUSIC_VOLUME = 0.35f;
    /** The world runs this much slower while the hero is dying, like ETG's death slow-motion. */
    private static final float DEATH_SLOWMO = 0.45f;
    /** How far from the player the gamepad aim point sits. */
    private static final float PAD_AIM_DISTANCE = 1.6f;
    /** Holding Y longer than this opens the gun wheel; a shorter tap switches to the next gun. */
    private static final float WHEEL_DELAY = 0.2f;

    // no saving mid-floor (like ETG): quitting from the pause menu abandons the run
    private static final String[] PAUSE_ITEMS = {"RESUME", "RESTART", "OPTIONS", "GO HOME", "EXIT GAME"};
    private static final int RESUME = 0;
    private static final int RESTART = 1;
    private static final int OPTIONS = 2;
    private static final int QUIT_TO_MENU = 3;
    private static final int EXIT_GAME = 4;
    // after the boss: the only place a run can be saved
    private static final String[] CLEAR_ITEMS = {"NEXT FLOOR", "SAVE & GO HOME", "SAVE & EXIT GAME"};
    private static final String[] ROCKET_ITEMS = {"BLAST OFF!", "SAVE & GO HOME", "SAVE & EXIT GAME"};
    /** When the run ends (you died / you won): go again, go home, or quit the game. */
    private static final String[] OVER_ITEMS = {"TRY AGAIN", "GO HOME", "EXIT GAME"};
    private static final String[] WON_ITEMS = {"PLAY AGAIN", "GO HOME", "EXIT GAME"};
    /** The False King beaten: the trip can go on up to the real one (the owner's wish - a good build mustn't end). */
    private static final String[] NORMAL_ENDING_ITEMS = {"FIND THE REAL KING!", "PLAY AGAIN", "GO HOME", "EXIT GAME"};
    private static final int GO_ON = 0;
    private static final int AGAIN = 0;
    private static final int END_HOME = 1;
    private static final int END_EXIT = 2;
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
    private final MenuList overMenu = new MenuList(1.85f, 0.3f, 0.42f);
    private final MenuList wonMenu = new MenuList(0.78f, 0.24f, 0.32f);
    private final MenuList normalEndingMenu = new MenuList(0.86f, 0.21f, 0.3f);
    /** The options, opened from the pause menu (no "reset progress" mid-run). */
    private final OptionsMenu options = new OptionsMenu(false);
    private boolean optionsOpen;
    private GameWorld world;
    private State state = State.PLAYING;
    private float stateTimer;
    private boolean newBest;
    private final Vector3 mouse = new Vector3();
    private final Color tmpColor = new Color();
    private final Vector2 padAim = new Vector2(1, 0);
    /** How far the camera leans toward the aim, at most. */
    private static final float CAMERA_LEAN_MAX = 1.0f;
    /** The camera never puts the player closer than this to the screen's edge (as a centre offset). */
    private static final float CAMERA_KEEP_X = Arena.VIEW_W / 2f - 1.2f;
    private static final float CAMERA_KEEP_Y = Arena.VIEW_H / 2f - 1.0f;
    private final Vector2 camPos = new Vector2();
    /** How far the camera is pulled back (1 = normal; some bosses want more room, see Enemy.viewZoom). */
    private float camZoom = 1f;
    private final Vector2 camTarget = new Vector2();
    private final Vector2 camLean = new Vector2();
    /** While a boss is shown (introduced or dying) and a moment after, the camera pans slowly. */
    private static final float CAMERA_GLIDE = 1.0f;
    private float camGlide;
    private Room cameraRoom;
    /** The big map (hold LT / M) is open; the world is paused while it is. */
    private boolean mapOpen;
    private Room mapCursor;
    private boolean mapStickHeld;
    /** After a teleport the map stays shut until LT / M is let go (so it doesn't pop open again at once). */
    private boolean suppressMapUntilRelease;
    private float bigMapLeft;
    private float bigMapBottom;
    private float bigMapStep;
    private float bigMapCell;
    /** The gun whose name was last announced above the gun panel, and how long the name stays up. */
    private Weapon shownWeapon;
    private float weaponNameTimer;
    private float rightStickIdle;
    /**
     * The mouse wheel switches guns - but not right after a click on the map or the floor menu: a Magic
     * Mouse or a trackpad sends little scrolls with every touch, which used to swap the gun in hand.
     */
    private static final float SCROLL_QUIET = 0.6f;
    private float scrollQuiet = SCROLL_QUIET;
    /** Small trackpad scrolls add up to one gun switch; forgotten after a pause. */
    private float scrollSum;
    private float scrollIdle;
    /** Seconds Y has been held, or -1 when it isn't. */
    private float yHeld = -1f;
    /** How long every enemy still in the fight has been off screen (then arrows at the edge point the way). */
    private float lostTimer;
    /** ETG-style introduction card when a boss appears (the world holds still under it). */
    private final BossCard bossCard = new BossCard();
    private final EndingPicture endingPicture = new EndingPicture();
    private final NormalEndingPicture normalEndingPicture = new NormalEndingPicture();
    private final TrueEndingPicture trueEndingPicture = new TrueEndingPicture();
    /** The run was won in the Undercity, against the False King: the normal ending (not the kingdom saved). */
    private boolean normalEnding;
    /** The run ended on the Moon, Grandfluff beaten: the true ending. */
    private boolean trueEnding;
    /** Who just earned their golden skull crown (Grandfluff cheered up in hard mode, the first time), or null. */
    private Hero newCrown;
    private static final float LOST_DELAY = 1.2f;
    private static final Color ENEMY_ARROW = Color.valueOf("ff6b4a");
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
        overMenu.setItems(OVER_ITEMS);
        wonMenu.setItems(WON_ITEMS);
        normalEndingMenu.setItems(NORMAL_ENDING_ITEMS);
        if (nextFloor != null) {
            world = nextFloor.startFloor(game);
        } else {
            boolean atBoss = retry ? Dev.flag("retryBoss") : Dev.flag("bossRoom"); // dev builds only
            StoryDirector story = Dev.flag("moon") ? StoryDirector.moon(atBoss) // dev builds: up on the Moon
                    : Dev.flag("undercity") ? StoryDirector.undercity(atBoss) // dev builds: the Undercity
                    : Dev.flag("slums") ? StoryDirector.slums(atBoss) // dev builds: straight down to the Slums
                    : new StoryDirector(MathUtils.clamp(Dev.number("floor", 1), 1, Stage.lastFloor()), atBoss);
            world = mode == Mode.STORY ? new GameWorld(game, story) : new GameWorld(game, new WaveDirector());
        }
        // the title screen's picture draws in every place you've been (not the secret floors: they're off its road)
        if (mode == Mode.STORY && !((StoryDirector) world.getDirector()).getStage().isSecret()) {
            game.reachFloor(((StoryDirector) world.getDirector()).getFloor());
        }
        if (nextFloor != null && nextFloor.wokeUp) {
            world.toast("IT WAS ALL A BAD DREAM... TRY AGAIN!");
        }
        if (mode == Mode.STORY && nextFloor == null && Dev.flag("win")) { // dev builds: straight to the victory screen
            onRunEnded(State.VICTORY);
        }

        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Crosshair);
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (state != State.PLAYING || mapOpen || wheelOpen || bossCard.isShowing() || scrollQuiet > 0) {
                    return true;
                }
                scrollSum = scrollIdle > 0.3f ? amountY : scrollSum + amountY;
                scrollIdle = 0;
                if (Math.abs(scrollSum) >= 0.99f) {
                    world.getPlayer().cycleWeapon(scrollSum > 0 ? 1 : -1);
                    scrollSum = 0;
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
        float full = MUSIC_VOLUME * game.getMusicVolume();
        if (music != null && music.getVolume() < full) {
            music.setVolume(Math.min(full, music.getVolume() + delta * 0.6f));
        } else if (music != null) {
            music.setVolume(full); // turned down in the options
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
        updateCamera(0f); // first frame / new room: snap
        cam.position.set(camPos.x, camPos.y, 0);
        cam.zoom = camZoom; // (so the mouse aims right while zoomed out)
        cam.update();
        updateGunWheel(delta);
        updateAim(delta);
        if (suppressMapUntilRelease) {
            suppressMapUntilRelease = game.pad.pressed(Button.LT) || Gdx.input.isKeyPressed(Input.Keys.M);
        } else {
            updateMap();
        }

        stateTimer += delta;
        weaponNameTimer -= delta;
        scrollQuiet -= delta;
        scrollIdle += delta;
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && state != State.PLAYING) {
            scrollQuiet = SCROLL_QUIET;
        }
        if (state == State.PLAYING) {
            bossCard.maybeStart(world.activeBoss());
        }
        bossCard.update(delta, Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY) || Gdx.input.justTouched()
                || game.pad.justPressed(Button.A) || game.pad.justPressed(Button.B));
        // the world freezes while picking a gun, on the map and while a boss is being introduced
        if (state == State.PLAYING && !wheelOpen && !mapOpen && !bossCard.isShowing()) {
            Player p = world.getPlayer();
            world.update(p.isDead() ? delta * DEATH_SLOWMO : delta);
            if (p.isDeathAnimationDone() && mode == Mode.STORY && p.hasItem(Item.DREAM_CATCHER)) {
                wakeUp();
                return;
            } else if (p.isDeathAnimationDone()) {
                onRunEnded(State.GAME_OVER);
            } else if (world.getDirector().isFinished()) {
                onRunEnded(isLastFloor() && !rocketTrip() ? State.VICTORY : State.STAGE_CLEAR);
            }
        } else if (state != State.PAUSED) {
            world.particles().update(delta);
        }
        if (state != State.PAUSED && state != State.GAME_OVER) {
            updateMusic(delta);
        }
        if (state != State.PAUSED && !wheelOpen) {
            updateCamera(delta);
            updateLostEnemies(delta);
        }

        ScreenUtils.clear(Color.BLACK);
        game.viewport.apply();

        float shake = state == State.PLAYING ? world.getTrauma() * world.getTrauma() * 0.12f : 0f;
        cam.position.set(camPos.x + MathUtils.random(-shake, shake), camPos.y + MathUtils.random(-shake, shake), 0);
        cam.zoom = camZoom;
        cam.update();
        game.batch.setProjectionMatrix(cam.combined);
        game.batch.begin();
        world.draw(game.batch);
        drawWorldOverlays(game.batch);
        game.batch.end();

        cam.zoom = 1f; // the HUD never zooms
        cam.position.set(Arena.VIEW_W / 2f, Arena.VIEW_H / 2f, 0);
        cam.update();
        game.batch.setProjectionMatrix(cam.combined);
        game.batch.begin();
        if (world.getFade() > 0) {
            game.draw.rect(game.batch, 0, 0, Arena.VIEW_W, Arena.VIEW_H, Color.BLACK, world.getFade());
        }
        drawHud(game.batch);
        if (world.getElevatorTimer() > 0) {
            drawElevator(game.batch);
        }
        bossCard.draw(game.batch, game.draw, game, world.getClock() + stateTimer);
        if (mapOpen) {
            drawBigMap(game.batch);
        } else if (wheelOpen) {
            drawGunWheel(game.batch);
        } else if (state == State.PAUSED) {
            drawPause(game.batch);
        } else if (state == State.GAME_OVER) {
            drawGameOver(game.batch);
        } else if (state == State.STAGE_CLEAR) {
            drawStageClear(game.batch);
        } else if (state == State.VICTORY) {
            drawVictory(game.batch);
        }
        game.batch.end();
    }

    private void handleStateInput() {
        Gamepad pad = game.pad;
        switch (state) {
            case PLAYING:
                if (bossCard.isShowing()) {
                    break; // Esc skips the boss card instead of pausing
                }
                if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyJustPressed(Input.Keys.P)
                        || pad.justPressed(Button.START)) {
                    closeGunWheel();
                    openPause();
                }
                break;
            case PAUSED:
                if (optionsOpen) {
                    optionsOpen = options.update(game) == OptionsMenu.Result.OPEN;
                    break;
                }
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
                    case OPTIONS:
                        options.open();
                        optionsOpen = true;
                        break;
                    case QUIT_TO_MENU:
                        toMenu();
                        break;
                    case EXIT_GAME:
                        recordRun();
                        Gdx.app.exit();
                        break;
                    default:
                        break;
                }
                break;
            case GAME_OVER:
            case VICTORY:
                if (stateTimer > (state == State.VICTORY ? 2.5f : 1.2f)) {
                    int pick = (state == State.VICTORY ? normalEnding ? normalEndingMenu : wonMenu : overMenu).update(game);
                    if (state == State.VICTORY && normalEnding && pick >= 0) {
                        if (pick == GO_ON) {
                            goOnToTheRealKing();
                            break;
                        }
                        pick--; // the rest are the usual play again / go home / exit
                    }
                    if (Gdx.input.isKeyJustPressed(Input.Keys.R)) { // (the old shortcuts still work)
                        pick = AGAIN;
                    } else if (Gdx.input.isKeyJustPressed(Input.Keys.M) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                            || pad.justPressed(Button.B) || pad.justPressed(Button.BACK)) {
                        pick = END_HOME;
                    }
                    if (pick == AGAIN) {
                        restart(mode);
                    } else if (pick == END_HOME) {
                        toMenu();
                    } else if (pick == END_EXIT) {
                        Gdx.app.exit();
                    }
                }
                break;
            case STAGE_CLEAR:
                if (stateTimer > 1.2f) {
                    switch (clearMenu.update(game)) {
                        case NEXT_FLOOR:
                            game.setScreen(nextFloor(game, floorSave()));
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
        optionsOpen = false;
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
        recordRun();
    }

    private void onRunEnded(State end) {
        state = end;
        stateTimer = 0;
        if (end == State.GAME_OVER) {
            pauseMusic();
        }
        newBest = world.getScore() > game.getHighScore();
        if (end == State.STAGE_CLEAR && rocketTrip()) { // up the rocket: the King is beaten all the same
            game.addWin();
            Unlocks.boardRocket(game.prefs); // the rocket game console turns up at home
            clearMenu.setItems(ROCKET_ITEMS);
            game.sfx.play("wave_clear", 1f);
        }
        if (end == State.VICTORY) {
            normalEnding = world.getDirector() instanceof StoryDirector
                    && ((StoryDirector) world.getDirector()).getStage().isUndercity();
            trueEnding = world.getDirector() instanceof StoryDirector
                    && ((StoryDirector) world.getDirector()).getStage().isMoon();
            if (world.seenEverywhere() && !Hero.current.easy()) { // village to Moon, sea and sewers too
                Flufficon.addStat(Flufficon.GRAND_TOURS, 1);
            }
            newCrown = null;
            if (trueEnding && world.isHard()) {
                Flufficon.addStat(Flufficon.HARD_TRUE_ENDINGS, 1);
                Hero hero = world.getPlayer().getHero();
                if (Unlocks.crown(hero, game.prefs)) {
                    newCrown = hero;
                }
            }
            if (normalEnding) {
                game.addFalseKingBeaten();
            } else if (trueEnding) { // (the King's win was counted when they climbed into the rocket)
                game.addTrueEnding();
            } else {
                game.addWin();
            }
            game.sfx.play("wave_clear", 1f);
        }
        recordRun();
    }

    /**
     * Best score / best endless wave. Not only on death: quitting to the menu, restarting or closing the
     * window mid-run counts too (it used to be lost). Safe to call more than once - only records go in.
     */
    private void recordRun() {
        int wave = world.getDirector() instanceof WaveDirector ? ((WaveDirector) world.getDirector()).getWave() : 0;
        game.submitRun(world.getScore(), wave);
    }

    /** The floor ended in the family's rocket after the Fluffy King: next stop, the Moon. */
    private boolean rocketTrip() {
        return world.getDirector() instanceof StoryDirector && ((StoryDirector) world.getDirector()).boardedRocket();
    }

    /** The next floor of a saved / just-cleared trip - up to the Moon by the rocket ride first. */
    public static Screen nextFloor(ETGFanfic game, RunSave save) {
        return save.stage() == Stage.MOON ? new RocketRideScreen(game, () -> new GameScreen(game, save)) : new GameScreen(game, save);
    }

    /** The story run's last floor (beating its boss wins the game; endless never ends). */
    private boolean isLastFloor() {
        return world.getDirector() instanceof StoryDirector
                && ((StoryDirector) world.getDirector()).getStage().endsTheRun();
    }

    /**
     * After the False King: the trip goes on, everything carried along, up out of the sewers and into the
     * skyscraper as floor 6 (its bosses are too much fun to skip), then the Fluffy Kingdom as floor 7 - its King
     * and, with the rocket built, the Moon.
     */
    private void goOnToTheRealKing() {
        recordRun();
        RunSave next = floorSave();
        next.stage = Stage.SKYSCRAPER.name(); // (and after it the story's own way: floor 7 is the clouds)
        game.setScreen(nextFloor(game, next));
        dispose();
    }

    /** The Dream Catcher: it was all a bad dream - floor 1 again, with everything carried. */
    private void wakeUp() {
        game.setScreen(new GameScreen(game, RunSave.wakeUp(world)));
        dispose();
    }

    /** "Try again" / restart: a brand new run from floor 1, like ETG. */
    private void restart(Mode m) {
        recordRun();
        game.setScreen(new GameScreen(game, m, true));
        dispose();
    }

    private void toMenu() {
        recordRun();
        game.setScreen(new HubScreen(game)); // back home to the living room
        dispose();
    }

    // ------------------------------------------------------------------ HUD

    /** The skyscraper's elevator ride: shut steel doors and the storey display counting up (or down). */
    private void drawElevator(SpriteBatch b) {
        Draw d = game.draw;
        float t = world.getElevatorTimer();
        // the doors slide shut at the start and open again at the end
        float open = t > GameWorld.ELEVATOR_TIME - 0.3f ? (t - (GameWorld.ELEVATOR_TIME - 0.3f)) / 0.3f
                : t < 0.3f ? 1f - t / 0.3f : 0f;
        float half = Arena.VIEW_W / 2f * (1f - open);
        d.rect(b, 0, 0, half, Arena.VIEW_H, ELEVATOR_STEEL);
        d.rect(b, Arena.VIEW_W - half, 0, half, Arena.VIEW_H, ELEVATOR_STEEL);
        d.rect(b, half - 0.04f, 0, 0.04f, Arena.VIEW_H, HUD_PANEL);
        d.rect(b, Arena.VIEW_W - half, 0, 0.04f, Arena.VIEW_H, HUD_PANEL);
        if (open > 0.5f) {
            return;
        }
        // the storey display over the doors
        float cx = Arena.VIEW_W / 2f;
        d.rect(b, cx - 0.9f, 3.55f, 1.8f, 0.8f, HUD_PANEL);
        int floor = world.elevatorFloor();
        boolean up = world.getElevatorTo() >= world.getElevatorFrom();
        boolean roof = floor == world.getElevatorTo() && world.getRoom().type == Room.Type.BOSS;
        game.text(roof ? "ROOF" : String.valueOf(floor), cx + 0.15f, 4.18f, 0.5f, ELEVATOR_DIGITS, Align.center);
        float ax = cx - 0.6f;
        float ay = 3.95f;
        d.shape(b, d.triangle, ax, ay, 0.13f, up ? 90f : 270f, ELEVATOR_DIGITS, 1f);
    }

    private void drawHud(SpriteBatch b) {
        Draw d = game.draw;
        Player p = world.getPlayer();

        // hearts (top-left), in rows of 4 so extra heart containers never run into the boss bar
        float hs = 0.26f;
        float rowH = 0.27f;
        int perRow = 4;
        // up to 8 red hearts in two rows; hearts 9-16 are golden, layered on top of them
        int heartCount = Math.min(p.getMaxHealth(), Player.RED_HEARTS) / 2;
        int goldCount = Math.max(0, p.getMaxHealth() - Player.RED_HEARTS) / 2;
        int heartRows = Math.max(1, (heartCount + perRow - 1) / perRow);
        float drop = (heartRows - 1) * rowH; // everything below the hearts moves down this much
        // next row: blanks, then keys (always shown in story mode, where chests need them), then coins
        boolean showKeys = p.getKeys() > 0 || world.getDirector() instanceof StoryDirector;
        float keysX = 0.14f + p.getBlanks() * 0.21f + (p.getBlanks() > 0 ? 0.06f : 0f);
        float coinsX = showKeys ? keysX + 0.62f : keysX;
        String coins = "x" + p.getCoins();
        float rowW = coinsX + 0.22f + game.textWidth(coins, 0.26f) - 0.1f;
        float panelW = Math.max(Math.min(heartCount, perRow) * (hs + 0.04f), rowW) + 0.12f;
        float panelH = 0.6f + drop; // (the second row always has the coins)
        d.rect(b, 0.04f, 4.98f - panelH, panelW, panelH, HUD_PANEL, 0.6f);
        for (int i = 0; i < heartCount; i++) {
            int hpHere = p.getHealth() - i * 2;
            Texture t = hpHere >= 2 ? d.heartFull : hpHere == 1 ? d.heartHalf : d.heartEmpty;
            float hx = 0.12f + (i % perRow) * (hs + 0.04f);
            float hy = 4.95f - hs * 8f / 9f - (i / perRow) * rowH;
            boolean golden = i < goldCount;
            if (golden) { // its golden container peeks out from behind
                d.texture(b, d.heartGoldEmpty, hx + 0.035f, hy + 0.03f, hs, hs * 8f / 9f);
            }
            d.texture(b, t, hx, hy, hs, hs * 8f / 9f);
            int goldHere = p.getHealth() - Player.RED_HEARTS - i * 2;
            if (golden && goldHere > 0) {
                d.texture(b, goldHere >= 2 ? d.heartGold : d.heartGoldHalf, hx, hy, hs, hs * 8f / 9f);
            }
        }
        float bs = 0.17f;
        float rowY = 4.6f - drop;
        for (int i = 0; i < p.getBlanks(); i++) {
            d.texture(b, d.blankIcon, 0.14f + i * (bs + 0.04f), rowY - bs, bs, bs);
        }
        if (showKeys) {
            d.texture(b, d.keyIcon, keysX, rowY - bs + 0.03f, 0.26f, 0.13f);
            game.text("x" + p.getKeys(), keysX + 0.3f, rowY - 0.03f, 0.26f, HUD_TEXT, Align.left);
        }
        b.draw(game.sprites.prop("coin_0"), coinsX, rowY - bs, bs, bs);
        game.text(coins, coinsX + 0.22f, rowY - 0.03f, 0.26f, HUD_TEXT, Align.left);
        drawItems(b, d, p, 4.98f - panelH - 0.05f);

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
        drawBossBar(b, d);

        String banner = world.getDirector().banner();
        float alpha = world.getDirector().bannerAlpha();
        if (banner != null && alpha > 0) {
            d.rect(b, 0, 3.05f, Arena.VIEW_W, 0.5f, HUD_PANEL, 0.55f * alpha);
            float scale = banner.length() > 20 ? 0.6f : 0.85f;
            game.text(banner, Arena.VIEW_W / 2f, 3.43f - (0.85f - scale) * 0.15f, scale,
                    tmpColor.set(BANNER).mul(1, 1, 1, alpha), Align.center);
        }

        String note = world.getDirector().hudNote(); // a trap room's countdown, at the top in the middle
        if (note != null && state == State.PLAYING) {
            d.rect(b, Arena.VIEW_W / 2f - 0.9f, 4.5f, 1.8f, 0.34f, HUD_PANEL, 0.7f);
            game.text(note, Arena.VIEW_W / 2f, 4.76f, 0.4f, BANNER, Align.center);
        }

        if (world.getToastTimer() > 0 && world.getToast() != null) {
            float a = Math.min(1f, world.getToastTimer() / 0.4f);
            float tw = game.textWidth(world.getToast(), 0.4f); // a dark backing: gold is hard to read on the clouds
            d.rect(b, Arena.VIEW_W / 2f - tw / 2f - 0.1f, 0.85f - 0.3f, tw + 0.2f, 0.4f, HUD_PANEL, 0.75f * a);
            game.text(world.getToast(), Arena.VIEW_W / 2f, 0.85f, 0.4f, tmpColor.set(Color.GOLD).mul(1, 1, 1, a), Align.center);
        }

        if (world.getKills() == 0 && state == State.PLAYING && world.getEnemies().size == 0) {
            // two short lines between the item box (bottom-left) and the gun panel (bottom-right)
            d.rect(b, 0.8f, 0.06f, 4.75f, 0.4f, HUD_PANEL, 0.7f);
            String[] hint = game.pad.isActive()
                    ? new String[]{"LS move  RS aim  RB shoot  LB roll  A flip/open", "R3 blank  X reload  hold Y guns  RT item"}
                    : new String[]{"WASD move  MOUSE shoot  SPACE roll  E flip/open", "Q blank  R reload  F item"};
            game.text(hint[0], 3.175f, 0.41f, 0.22f, HUD_DIM, Align.center);
            game.text(hint[1], 3.175f, 0.23f, 0.22f, HUD_DIM, Align.center);
        }
    }

    /** Passive items in a row under the hearts; the active item in a box in the bottom-left corner, like ETG. */
    private void drawItems(SpriteBatch b, Draw d, Player p, float top) {
        float s = 0.24f;
        for (int i = 0; i < p.getPassives().size; i++) {
            float x = 0.06f + (i % 10) * (s + 0.03f);
            float y = top - s - (i / 10) * (s + 0.03f);
            d.rect(b, x - 0.01f, y - 0.01f, s + 0.02f, s + 0.02f, HUD_PANEL, 0.5f);
            Item item = p.getPassives().get(i);
            float charge = item == Item.BUBBLE_SHIELD ? p.shieldProgress() : 1f;
            if (charge < 1f) {
                b.setColor(1f, 1f, 1f, 0.35f);
            }
            b.draw(game.sprites.item(item.sprite), x, y, s, s);
            b.setColor(Color.WHITE);
            if (charge < 1f) { // a popped bubble: charging back up from the bottom as you deal damage
                d.rect(b, x, y, s * 0.12f, s * charge, HUD_TEXT, 0.8f);
            }
        }
        if (p.getActive() == null) {
            return;
        }
        float box = 0.62f;
        float bx = 0.08f;
        float by = 0.1f;
        d.rect(b, bx, by, box, box, HUD_PANEL, 0.7f);
        float cd = p.getActiveCooldown();
        if (cd > 0) {
            b.setColor(1f, 1f, 1f, 0.35f);
        }
        b.draw(game.sprites.item(p.getActive().sprite), bx + 0.07f, by + 0.07f, box - 0.14f, box - 0.14f);
        b.setColor(Color.WHITE);
        if (cd > 0) { // recharging: the box fills up from the bottom
            d.rect(b, bx, by, box, box * Math.min(1f, cd / p.getActiveCooldownMax()), Color.BLACK, 0.45f);
            game.text(String.valueOf((int) Math.ceil(cd)), bx + box / 2f, by + box / 2f + 0.08f, 0.4f, HUD_TEXT, Align.center);
        }
        game.text(game.pad.isActive() ? "RT" : "F", bx + box - 0.03f, by + box - 0.03f, 0.22f, BANNER, Align.right);
        game.text(p.getActive().title, bx, by + box + 0.17f, 0.24f, HUD_DIM, Align.left);
        if (p.getBagged() == null) {
            return;
        }
        // the Fluffy Backpack: the second item in a smaller box beside it, still recharging
        float bag = 0.38f;
        float gx = bx + box + 0.06f;
        d.rect(b, gx, by, bag, bag, HUD_PANEL, 0.6f);
        float bcd = p.getBaggedCooldown();
        b.setColor(1f, 1f, 1f, bcd > 0 ? 0.3f : 0.75f);
        b.draw(game.sprites.item(p.getBagged().sprite), gx + 0.05f, by + 0.05f, bag - 0.1f, bag - 0.1f);
        b.setColor(Color.WHITE);
        if (bcd > 0) {
            d.rect(b, gx, by, bag, bag * Math.min(1f, bcd / p.getBaggedCooldownMax()), Color.BLACK, 0.45f);
        }
        game.text(game.pad.isActive() ? "< >" : "G", gx + bag / 2f, by + bag + 0.15f, 0.18f, BANNER, Align.center);
    }

    /** Things drawn in the room (they scroll with the camera): the interact prompt and the pad's aim reticle. */
    private static final Color BUBBLE = Color.valueOf("fff4fa");
    private static final Color BUBBLE_EDGE = Color.valueOf("2a0a14");
    private static final Color BUBBLE_TEXT = Color.valueOf("5a1a3a");
    private final Color bubbleTmp = new Color();

    /**
     * The Flufful (or Baa-llo) talking: a speech bubble (pops in and out), kept inside the view. Its tail
     * points down at {@code x}; {@code anchor} is how much of the bubble lies left of it (0.5 = centred).
     */
    private void drawSpeechBubble(SpriteBatch b, String text, float shown, float x, float bottom, float anchor) {
        float pop = Math.min(1f, shown / 0.12f) * Math.min(1f, Math.max(0f, FluffulVoice.SHOW - shown) / 0.2f);
        float scale = 0.2f;
        float w = text.length() * 0.06f + 0.16f;
        float h = 0.2f;
        float half = Arena.VIEW_W / 2f - 0.1f;
        float bx = MathUtils.clamp(x - w * anchor, camPos.x - half, camPos.x + half - w);
        Draw d = game.draw;
        d.rect(b, bx - 0.02f, bottom - 0.02f, w + 0.04f, h + 0.04f, BUBBLE_EDGE, pop);
        d.rect(b, bx, bottom, w, h, BUBBLE, pop);
        d.rect(b, x - 0.05f, bottom - 0.06f, 0.08f, 0.06f, BUBBLE_EDGE, pop); // the tail points at the hero
        d.rect(b, x - 0.03f, bottom - 0.04f, 0.04f, 0.06f, BUBBLE, pop);
        bubbleTmp.set(BUBBLE_TEXT).a = pop;
        game.text(text, bx + w / 2f, bottom + h - 0.065f, scale, bubbleTmp, Align.center);
    }

    /** A dotted arc from (x, y) and a pulsing blast ring where a lobbed egg / grenade will come down. */
    private void drawLobMarker(SpriteBatch b, float x, float y, float angle, float dist, float blast) {
        Arena ar = Arena.now; // they fly over furniture but stop at the walls
        float lx = MathUtils.clamp(x + MathUtils.cosDeg(angle) * dist, ar.left + 0.1f, ar.right - 0.1f);
        float ly = MathUtils.clamp(y + MathUtils.sinDeg(angle) * dist, ar.bottom + 0.1f, ar.top - 0.1f);
        float pulse = 0.55f + 0.25f * MathUtils.sin(world.getClock() * 12f);
        for (int i = 1; i < 6; i++) {
            float t = i / 6f;
            float dx = MathUtils.lerp(x, lx, t);
            float dy = MathUtils.lerp(y, ly, t) + 4f * t * (1f - t) * 0.5f;
            game.draw.circle(b, dx, dy, 0.025f, HUD_TEXT, 0.6f);
        }
        game.draw.ring(b, lx, ly, blast, EMPTY_TEXT, pulse);
        game.draw.circle(b, lx, ly, 0.05f, EMPTY_TEXT, pulse);
    }

    private static final String[] FLAME_ICON = {
            "...#...",
            "..##...",
            "..###.#",
            ".####.#",
            ".######",
            "#######",
            "#######",
            ".#####.",
            "..###..",
    };
    private static final String[] DROP_ICON = {
            "...#...",
            "...#...",
            "..###..",
            "..###..",
            ".#####.",
            "#######",
            "#######",
            ".#####.",
            "..###..",
    };
    private static final String[] SNOW_ICON = {
            "#..#..#",
            ".#.#.#.",
            "..###..",
            "#######",
            "..###..",
            ".#.#.#.",
            "#..#..#",
    };
    private static final Color FLAME_FILL = Color.valueOf("ffc23a");
    private static final Color FROST_FILL = Color.valueOf("bfeaff");
    private static final Color FROST_DIM = Color.valueOf("2a4458");
    private static final Color FLAME_DIM = Color.valueOf("5a3418");
    private static final Color ACID_FILL = Color.valueOf("8ae03a");
    private static final Color ACID_DIM = Color.valueOf("2a4418");
    private static final float ICON_PIXEL = 0.026f;

    /**
     * A little pixel icon (bottom-left at x, y) that fills with colour from the bottom up as {@code fill}
     * goes from 0 to 1 - and blinks when it's nearly full, just before the hurt lands.
     */
    private void drawHazardIcon(SpriteBatch b, String[] icon, float x, float y, float fill, Color full, Color dim) {
        Draw d = game.draw;
        int rows = icon.length;
        float px = ICON_PIXEL;
        boolean blink = fill > 0.8f && MathUtils.sin(world.getClock() * 30f) > 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < icon[r].length(); c++) {
                if (icon[r].charAt(c) == '#') {
                    d.rect(b, x + c * px - px * 0.6f, y + (rows - 1 - r) * px - px * 0.6f, px * 2.2f, px * 2.2f, Color.BLACK);
                }
            }
        }
        for (int r = 0; r < rows; r++) {
            boolean lit = rows - r <= Math.round(fill * rows);
            Color col = blink ? Color.WHITE : lit ? full : dim;
            for (int c = 0; c < icon[r].length(); c++) {
                if (icon[r].charAt(c) == '#') {
                    d.rect(b, x + c * px, y + (rows - 1 - r) * px, px, px, col);
                }
            }
        }
    }

    private void drawWorldOverlays(SpriteBatch b) {
        Player p = world.getPlayer();
        // what the interact button would do here (open a chest, go down the exit)
        if (world.getPrompt() != null && state == State.PLAYING) {
            String button = game.pad.isActive() ? "A  " : "E  ";
            float tw = game.textWidth(button + world.getPrompt(), 0.24f);
            game.draw.rect(b, world.getPromptPos().x - tw / 2f - 0.07f, world.getPromptPos().y - 0.18f, tw + 0.14f, 0.24f,
                    HUD_PANEL, 0.75f);
            game.text(button + world.getPrompt(), world.getPromptPos().x, world.getPromptPos().y, 0.24f, BANNER, Align.center);
        }
        String said = world.getGrandad().line(); // grandad has the last word over the Flufful
        float saidFor = world.getGrandad().shown();
        if (said == null) {
            said = world.getFlufful().line();
            saidFor = world.getFlufful().shown();
        }
        if (said != null && !p.isDead() && state == State.PLAYING) {
            drawSpeechBubble(b, said, saidFor, p.centerX(), p.getY() + Player.HEIGHT + 0.48f, 0.5f);
        }
        Shop shop = world.getRoom().shop;
        if (shop != null && shop.line() != null && state == State.PLAYING) { // Baa-llo talks to the right of his head
            drawSpeechBubble(b, shop.line(), shop.shown(), shop.keeperX + 0.42f, shop.keeperY + 0.8f, 0.08f);
        }
        Weapon w = p.currentWeapon();
        if (w.isLaserSight() && state == State.PLAYING && !p.isDead() && !p.isRolling() && !w.isReloading()) {
            // the sniper's red aiming line, up to the first wall or cover
            float len = world.rayLength(p.muzzleX(), p.muzzleY(), p.getAimAngle(), 12f);
            game.draw.line(b, p.muzzleX(), p.muzzleY(), len, 0.012f, p.getAimAngle(), EMPTY_TEXT);
        }
        // like ETG: a hint above the hero when the magazine runs low, and when the gun is out of ammo
        if (state == State.PLAYING && !p.isDead()) { // where a charging egg / grenade would land
            if (w.isCharging() && w.isSlings()) { // a slingshot being pulled: a dotted line that grows with the pull
                for (int i = 1; i <= 2 + (int) (w.pull() * 6f); i++) {
                    game.draw.circle(b, p.muzzleX() + MathUtils.cosDeg(p.getAimAngle()) * 0.16f * i,
                            p.muzzleY() + MathUtils.sinDeg(p.getAimAngle()) * 0.16f * i, 0.02f, HUD_TEXT, 0.6f);
                }
            } else if (w.isCharging()) {
                drawLobMarker(b, p.muzzleX(), p.muzzleY(), p.getAimAngle(), w.lobDistance(), w.getBlastRadius());
            } else if (p.isChargingGrenade()) {
                drawLobMarker(b, p.centerX(), p.hitboxY(), p.getAimAngle(), p.grenadeDistance(), Grenade.RADIUS);
            }
        }
        if (state == State.PLAYING && !p.isDead() && (w.isEmpty() || w.wantsReload())) {
            boolean empty = w.isEmpty();
            String hint = empty ? "EMPTY" : "RELOAD";
            float hx = p.centerX();
            float hy = p.getY() + Player.HEIGHT + 0.24f;
            float blink = 0.8f + 0.2f * MathUtils.sin(world.getClock() * 10f);
            reloadHint.set(HUD_PANEL).a = blink; // a dark shadow, so it reads on any floor
            game.text(hint, hx + 0.02f, hy - 0.02f, 0.26f, reloadHint, Align.center);
            reloadHint.set(empty ? EMPTY_TEXT : HUD_TEXT).a = blink;
            game.text(hint, hx, hy, 0.26f, reloadHint, Align.center);
        }
        if (state == State.PLAYING && !p.isDead()) { // like ETG: fire / acid fill up beside the head before they hurt
            float ix = p.centerX() + 0.3f;
            float iy = p.getY() + Player.HEIGHT - 0.12f;
            float burn = p.burnWarning();
            if (burn >= 0) {
                drawHazardIcon(b, FLAME_ICON, ix, iy, burn, FLAME_FILL, FLAME_DIM);
                ix += 0.24f;
            }
            float acid = p.acidWarning();
            if (acid >= 0) {
                drawHazardIcon(b, DROP_ICON, ix, iy, acid, ACID_FILL, ACID_DIM);
                ix += 0.24f;
            }
            float frost = p.frostWarning();
            if (frost >= 0) {
                drawHazardIcon(b, SNOW_ICON, ix, iy, frost, FROST_FILL, FROST_DIM);
            }
        }
        if (state == State.PLAYING && !p.isDead() && lostTimer > LOST_DELAY) {
            drawEnemyArrows(b);
        }
        if (game.pad.isActive() && state == State.PLAYING && !p.isDead()) { // the mouse cursor is no help here
            game.draw.ring(b, world.getAim().x, world.getAim().y, 0.08f, HUD_TEXT, 0.8f);
            game.draw.circle(b, world.getAim().x, world.getAim().y, 0.02f, HUD_TEXT, 0.8f);
        }
    }

    /**
     * Counts how long the fight's last enemies have all been out of sight (a cube sitting in a far corner,
     * a hydrant behind you); any enemy on screen, or one still arriving, resets it.
     */
    private void updateLostEnemies(float delta) {
        boolean lost = state == State.PLAYING && world.getDirector().inFight(world)
                && world.getEnemies().size > 0 && world.getPendingEnemies().size == 0;
        for (int i = 0; i < world.getEnemies().size && lost; i++) {
            Enemy e = world.getEnemies().get(i);
            // (a giant standing beyond the arena, like the Kaiju, is never really lost)
            if (e.alive && (e.isSpawning() || e.viewZoom() > 1f || world.onScreen(e.pos.x, e.pos.y, -e.radius))) {
                lost = false;
            }
        }
        lostTimer = lost ? lostTimer + delta : 0f;
    }

    /** An arrow just inside the screen edge toward each enemy that's left, pulsing so it catches the eye. */
    private void drawEnemyArrows(SpriteBatch b) {
        Rectangle view = world.getView();
        float cx = view.x + view.width / 2f;
        float cy = view.y + view.height / 2f;
        float inset = 0.3f * camZoom;
        float alpha = Math.min(1f, (lostTimer - LOST_DELAY) / 0.3f) * (0.75f + 0.25f * MathUtils.sin(world.getClock() * 8f));
        for (Enemy e : world.getEnemies()) {
            if (!e.alive) continue;
            float dx = e.pos.x - cx;
            float dy = e.pos.y - cy;
            float angle = MathUtils.atan2Deg360(dy, dx);
            // where the line from the screen centre to the enemy leaves the (inset) screen
            float sx = Math.abs(dx) > 0.001f ? (view.width / 2f - inset) / Math.abs(dx) : Float.MAX_VALUE;
            float sy = Math.abs(dy) > 0.001f ? (view.height / 2f - inset) / Math.abs(dy) : Float.MAX_VALUE;
            float k = Math.min(sx, sy);
            float bob = 0.04f * MathUtils.sin(world.getClock() * 8f) * camZoom; // nudges toward the enemy
            float x = cx + dx * k + MathUtils.cosDeg(angle) * bob;
            float y = cy + dy * k + MathUtils.sinDeg(angle) * bob;
            float r = 0.13f * camZoom;
            game.draw.shape(b, game.draw.triangle, x, y, r * 1.35f, angle, HUD_PANEL, 0.8f * alpha);
            game.draw.shape(b, game.draw.triangle, x, y, r, angle, ENEMY_ARROW, alpha);
        }
    }

    /**
     * The camera follows the player and leans toward where they aim (a quarter of the way to the mouse,
     * or along the right stick), like ETG; it stays inside the room and centres a room that's smaller
     * than the screen (black beyond the walls). It snaps when the player enters another room.
     */
    private void updateCamera(float delta) {
        Player p = world.getPlayer();
        Arena a = Arena.now;
        camTarget.set(p.centerX(), p.hitboxY());
        // a boss being introduced, or going down: the camera goes over to it, then glides back
        Enemy intro = bossCard.isShowing() ? world.activeBoss() : null;
        Vector2 fall = intro == null ? world.bossDeathSpot() : null;
        if (intro != null || fall != null) {
            camTarget.set(intro != null ? intro.pos : fall);
            camGlide = CAMERA_GLIDE;
        } else {
            camGlide = Math.max(0f, camGlide - delta);
        }
        if (!p.isDead() && intro == null && fall == null) {
            if (game.pad.isActive()) {
                camLean.set(game.pad.rightStick()).scl(CAMERA_LEAN_MAX);
            } else {
                camLean.set(world.getAim()).sub(camTarget).scl(0.25f).limit(CAMERA_LEAN_MAX);
            }
            camTarget.add(camLean);
            // in a boss fight, lean toward the boss too so it's rarely off screen...
            Enemy boss = world.activeBoss();
            float keep = 1f;
            if (boss != null && boss.viewZoom() > 1f) {
                // a giant beyond the arena (the Kaiju): frame its head and shoulders above the player
                camTarget.add((boss.pos.x - p.centerX()) * 0.2f, (boss.pos.y + 1.4f - p.hitboxY()) * 0.5f);
                keep = boss.viewZoom();
            } else if (boss != null) {
                camTarget.add((boss.pos.x - p.centerX()) * 0.35f, (boss.pos.y - p.hitboxY()) * 0.35f);
            }
            // ...but always keep the player comfortably in view
            float keepX = keep > 1f ? Arena.VIEW_W / 2f * keep - 1f : CAMERA_KEEP_X;
            float keepY = keep > 1f ? Arena.VIEW_H / 2f * keep - 0.6f : CAMERA_KEEP_Y;
            camTarget.x = MathUtils.clamp(camTarget.x, p.centerX() - keepX, p.centerX() + keepX);
            camTarget.y = MathUtils.clamp(camTarget.y, p.hitboxY() - keepY, p.hitboxY() + keepY);
        }
        Enemy giant = world.activeBoss();
        float zoomTarget = giant != null ? giant.viewZoom() : fall != null ? world.bossDeathZoom() : 1f;
        camZoom += (zoomTarget - camZoom) * (1f - (float) Math.exp(-2.5f * delta));
        float hw = Arena.VIEW_W / 2f * camZoom;
        float hh = Arena.VIEW_H / 2f * camZoom;
        // a roof has sky beyond its edges worth seeing; elsewhere the camera stays inside the room
        float overX = world.getRoom().openSky() ? 1.5f : 0f;
        float overTop = zoomTarget > 1f ? 3f : overX; // room for the Kaiju above the roof
        camTarget.x = a.worldW <= hw * 2f ? a.worldW / 2f : MathUtils.clamp(camTarget.x, hw - overX, a.worldW - hw + overX);
        camTarget.y = MathUtils.clamp(camTarget.y, Math.min(hh, a.worldH / 2f), Math.max(a.worldH / 2f, a.worldH - hh + overTop));
        if (world.getRoom() != cameraRoom) {
            cameraRoom = world.getRoom();
            camPos.set(camTarget);
        } else {
            camPos.lerp(camTarget, 1f - (float) Math.exp(-(camGlide > 0f ? 3f : 8f) * delta));
        }
        world.setView(camPos.x, camPos.y, camZoom);
    }

    private void drawMinimap(SpriteBatch b, Draw d) {
        StageMap map = world.getMap();
        if (map == null) return;
        int[] bounds = mapBounds(map);
        float cell = 0.17f;
        float gap = 0.05f;
        float step = cell + gap;
        float w = (bounds[1] - bounds[0] + 1) * step + gap;
        float h = (bounds[3] - bounds[2] + 1) * step + gap;
        float x0 = 6.92f - w;
        float y0 = 4.56f - h;
        d.rect(b, x0 - 0.03f, y0 - 0.03f, w + 0.06f, h + 0.06f, HUD_PANEL, 0.6f);
        drawRooms(b, d, map, bounds, x0 + gap, y0 + gap, cell, gap, null);
    }

    /** {minX, maxX, minY, maxY} of the known rooms. */
    private static int[] mapBounds(StageMap map) {
        int[] r = {99, -99, 99, -99};
        for (Room room : map.rooms) {
            if (!map.isKnown(room)) continue;
            r[0] = Math.min(r[0], room.gx);
            r[1] = Math.max(r[1], room.gx);
            r[2] = Math.min(r[2], room.gy);
            r[3] = Math.max(r[3], room.gy);
        }
        return r;
    }

    /**
     * Draws the known rooms of the map (minimap or the big teleport map) with corridors, the boss and
     * treasure rooms, and in visited rooms what's still there: unopened chests (tier colour), guns,
     * hearts, keys and other pickups, and the open exit hatch. {@code selected} gets a highlight.
     */
    private void drawRooms(SpriteBatch b, Draw d, StageMap map, int[] bounds, float left, float bottom,
                           float cell, float gap, Room selected) {
        float step = cell + gap;
        float corridor = Math.max(0.04f, cell * 0.22f);
        Room here = world.getRoom();
        for (Room r : map.rooms) {
            if (!map.isKnown(r)) continue;
            float rx = left + (r.gx - bounds[0]) * step;
            float ry = bottom + (r.gy - bounds[2]) * step;
            if (r.visited) { // corridors
                if (r.doors[Room.E]) d.rect(b, rx + cell, ry + cell / 2f - corridor / 2f, gap, corridor, MAP_ROOM);
                if (r.doors[Room.N]) d.rect(b, rx + cell / 2f - corridor / 2f, ry + cell, corridor, gap, MAP_ROOM);
            }
            if (r == selected) {
                d.rect(b, rx - cell * 0.12f, ry - cell * 0.12f, cell * 1.24f, cell * 1.24f, BANNER);
            }
            Color c = r == here ? MAP_HERE : r.visited ? MAP_ROOM : MAP_SEEN;
            d.rect(b, rx, ry, cell, cell, c);
            float inset = cell * 0.28f;
            if (r.type == Room.Type.BOSS) {
                d.rect(b, rx + inset, ry + inset, cell - 2 * inset, cell - 2 * inset, MAP_BOSS);
            } else if (r.type == Room.Type.TREASURE) {
                d.rect(b, rx + inset, ry + inset, cell - 2 * inset, cell - 2 * inset, MAP_TREASURE);
            } else if (r.type == Room.Type.SHOP) { // Baa-llo's: a gold coin with its pom-pom
                d.circle(b, rx + cell / 2f, ry + cell / 2f, cell * 0.27f, Color.BLACK);
                d.circle(b, rx + cell / 2f, ry + cell / 2f, cell * 0.22f, MAP_SHOP);
                d.circle(b, rx + cell / 2f, ry + cell / 2f, cell * 0.08f, MAP_POM);
            }
            if (r.visited) {
                drawRoomIcons(b, d, r, rx, ry, cell);
            }
        }
    }

    /**
     * Icons along the bottom of a room cell for what's left in it, told apart by colour and shape:
     * chests are little chest shapes in their tier colour, keys yellow key shapes, every other item an
     * orange dot; all outlined so they show on any room colour (the current room is near-white).
     */
    private void drawRoomIcons(SpriteBatch b, Draw d, Room r, float rx, float ry, float cell) {
        float s = cell * 0.26f;
        float o = Math.max(0.008f, s * 0.14f); // outline
        float x = rx + cell * 0.08f;
        float y = ry + cell * 0.08f;
        int shown = 0;
        if (r.exit) {
            float ex = rx + cell - s - cell * 0.08f;
            float ey = ry + cell - s - cell * 0.08f;
            d.rect(b, ex - o, ey - o, s + 2 * o, s + 2 * o, Color.BLACK);
            d.rect(b, ex, ey, s, s, MAP_EXIT);
        }
        for (Chest c : r.chests) {
            if (c.opened || shown >= 3) continue;
            float w = s * 1.3f;
            float h = s * 0.95f;
            d.rect(b, x - o, y - o, w + 2 * o, h + 2 * o, Color.BLACK);
            d.rect(b, x, y, w, h, TIER_COLORS[c.tier.ordinal()]);
            d.rect(b, x, y + h * 0.55f, w, Math.max(0.006f, h * 0.12f), Color.BLACK); // lid line
            x += w + s * 0.35f;
            shown++;
        }
        for (Pickup p : r.pickups) {
            if (!p.alive || p.type == Pickup.Type.COIN || shown >= 3) continue; // (coins would crowd it)
            float cx = x + s / 2f;
            float cy = y + s / 2f;
            if (p.type == Pickup.Type.KEY) { // a ring with a stem
                d.circle(b, cx - s * 0.18f, cy, s * 0.36f, Color.BLACK);
                d.rect(b, cx - s * 0.1f, cy - s * 0.16f, s * 0.66f, s * 0.32f, Color.BLACK);
                d.circle(b, cx - s * 0.18f, cy, s * 0.26f, MAP_KEY);
                d.rect(b, cx - s * 0.05f, cy - s * 0.07f, s * 0.56f, s * 0.14f, MAP_KEY);
            } else {
                d.circle(b, cx, cy, s * 0.55f, Color.BLACK);
                d.circle(b, cx, cy, s * 0.42f, MAP_ITEM);
            }
            x += s * 1.35f;
            shown++;
        }
    }

    /** The big map (hold LT / M): pick a visited room and teleport there. */
    private void drawBigMap(SpriteBatch b) {
        Draw d = game.draw;
        StageMap map = world.getMap();
        d.rect(b, 0, 0, Arena.VIEW_W, Arena.VIEW_H, Color.BLACK, 0.72f);
        game.text("MAP", Arena.VIEW_W / 2f, 4.8f, 0.5f, BANNER, Align.center);
        int[] bounds = mapBounds(map);
        int cols = bounds[1] - bounds[0] + 1;
        int rows = bounds[3] - bounds[2] + 1;
        float step = Math.min(0.66f, Math.min(6.2f / cols, 3.6f / rows));
        float cell = step * 0.76f;
        float gap = step - cell;
        float left = Arena.VIEW_W / 2f - (cols * step - gap) / 2f;
        float bottom = 2.55f - (rows * step - gap) / 2f;
        bigMapLeft = left;
        bigMapBottom = bottom;
        bigMapStep = step;
        bigMapCell = cell;
        drawRooms(b, d, map, bounds, left, bottom, cell, gap, mapCursor);
        String hint;
        if (world.getRoom().isLocked()) {
            hint = "CAN'T TELEPORT DURING A FIGHT";
        } else if (mapCursor != null && world.canTeleportTo(mapCursor)) {
            hint = game.pad.isActive() ? "A  teleport here" : "CLICK / ENTER  teleport here";
        } else {
            hint = game.pad.isActive() ? "STICK  pick a visited room" : "CLICK a visited room";
        }
        game.text(hint, Arena.VIEW_W / 2f, 0.55f, 0.28f, HUD_TEXT, Align.center);
        game.text(game.pad.isActive() ? "release LT to close" : "release M to close",
                Arena.VIEW_W / 2f, 0.3f, 0.22f, HUD_DIM, Align.center);
    }

    /** Hold LT (pad) or M (keyboard) for the map; pick a visited room and press A / Enter / click to teleport. */
    private void updateMap() {
        StageMap map = world.getMap();
        Gamepad pad = game.pad;
        boolean hold = map != null && state == State.PLAYING && !wheelOpen && !world.getPlayer().isDead()
                && (pad.pressed(Button.LT) || Gdx.input.isKeyPressed(Input.Keys.M));
        if (hold && !mapOpen && world.getDirector().inFight(world)) {
            // no map mid-fight: a stray LT press would cover the screen (and its bullets) at the worst moment
            if (pad.justPressed(Button.LT) || Gdx.input.isKeyJustPressed(Input.Keys.M)) {
                world.toast("NO MAP DURING A FIGHT");
            }
            hold = false;
        }
        if (!hold) {
            mapOpen = false;
            return;
        }
        if (!mapOpen) {
            mapOpen = true;
            mapCursor = world.getRoom();
            mapStickHeld = true;
        }
        // mouse: hover picks a room (the map is drawn in screen units, so unproject with the HUD camera)
        OrthographicCamera cam = (OrthographicCamera) game.viewport.getCamera();
        cam.position.set(Arena.VIEW_W / 2f, Arena.VIEW_H / 2f, 0);
        cam.update();
        mouse.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        game.viewport.unproject(mouse);
        cam.position.set(camPos.x, camPos.y, 0);
        cam.update();
        Room hovered = null;
        int[] bounds = mapBounds(map);
        if (bigMapStep > 0) {
            for (Room r : map.rooms) {
                if (!map.isKnown(r)) continue;
                float rx = bigMapLeft + (r.gx - bounds[0]) * bigMapStep;
                float ry = bigMapBottom + (r.gy - bounds[2]) * bigMapStep;
                if (mouse.x >= rx && mouse.x <= rx + bigMapCell && mouse.y >= ry && mouse.y <= ry + bigMapCell) {
                    hovered = r;
                }
            }
        }
        if (hovered != null && (Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0)) {
            mapCursor = hovered;
        }
        // stick, d-pad or arrow keys: hop to the nearest visited room in that direction
        Vector2 stick = pad.leftStick();
        int dx = 0;
        int dy = 0;
        if (pad.justPressed(Button.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) dx = -1;
        if (pad.justPressed(Button.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) dx = 1;
        if (pad.justPressed(Button.UP) || Gdx.input.isKeyJustPressed(Input.Keys.UP)) dy = 1;
        if (pad.justPressed(Button.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) dy = -1;
        if (!mapStickHeld && stick.len() > 0.6f) {
            if (Math.abs(stick.x) > Math.abs(stick.y)) dx = stick.x > 0 ? 1 : -1;
            else dy = stick.y > 0 ? 1 : -1;
        }
        mapStickHeld = stick.len() > 0.3f;
        if (dx != 0 || dy != 0) {
            moveMapCursor(map, dx, dy);
        }
        boolean confirm = pad.justPressed(Button.A) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.E);
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && hovered != null) { // click a room
            mapCursor = hovered;
            confirm = true;
        }
        if (confirm && mapCursor != null) {
            if (world.teleportTo(mapCursor)) {
                mapOpen = false;
                suppressMapUntilRelease = true;
                scrollQuiet = SCROLL_QUIET;
            } else {
                world.sfx().play("empty", 1f);
            }
        }
    }

    /** Moves the map cursor to the closest visited room roughly in direction (dx, dy). */
    private void moveMapCursor(StageMap map, int dx, int dy) {
        Room from = mapCursor != null ? mapCursor : world.getRoom();
        Room best = null;
        float bestScore = Float.MAX_VALUE;
        for (Room r : map.rooms) {
            if (r == from || !r.visited) continue;
            int ox = r.gx - from.gx;
            int oy = r.gy - from.gy;
            float along = ox * dx + oy * dy;
            if (along <= 0) continue;
            float across = Math.abs(ox * dy - oy * dx);
            float score = along + across * 2f;
            if (score < bestScore) {
                bestScore = score;
                best = r;
            }
        }
        if (best != null) {
            mapCursor = best;
        }
    }

    private void drawWeaponPanel(SpriteBatch b, Draw d, Player p) {
        Weapon w = p.currentWeapon();
        if (w != shownWeapon) { // switched guns: say its name for a moment
            shownWeapon = w;
            weaponNameTimer = 1.6f;
        }
        // compact bottom-right panel: the gun's picture, ammo count, magazine pips, switch hint
        float right = 6.9f;
        boolean several = p.getWeapons().size > 1;
        float bottom = several ? 0.1f : 0.19f;
        float top = 0.68f;
        d.rect(b, right - 1.25f, bottom, 1.31f, top - bottom, HUD_PANEL, 0.45f);
        Sprites.GunSprite g = game.sprites.gun(w.getSprite());
        g.drawIcon(b, right - 0.84f, 0.48f, Math.min(2f, Math.min(0.72f / g.w, 0.34f / g.h)));
        String ammo = w.isReloading() ? "RELOAD" : w.ammoText();
        game.text(ammo, right, 0.6f, 0.22f, w.isEmpty() ? Color.RED : HUD_DIM, Align.right);

        int mag = w.getMagSize();
        float gap = 0.012f;
        float pipW = Math.min(0.04f, 1.15f / mag - gap);
        float x = right - mag * (pipW + gap) + gap;
        for (int i = 0; i < mag; i++) {
            boolean full = i < w.getMag() && !w.isReloading();
            d.rect(b, x + i * (pipW + gap), 0.25f, pipW, 0.05f, full ? AMMO_PIP : AMMO_PIP_EMPTY);
        }
        if (several) {
            game.text((p.getWeaponIndex() + 1) + "/" + p.getWeapons().size
                    + (game.pad.isActive() ? "  HOLD Y" : "  SCROLL/TAB"), right, 0.19f, 0.16f, HUD_DIM, Align.right);
        }
        if (weaponNameTimer > 0) {
            float a = Math.min(1f, weaponNameTimer / 0.4f);
            game.text(w.displayName().toUpperCase(), right, top + 0.16f, 0.28f, tmpColor.set(BANNER).mul(1, 1, 1, a), Align.right);
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
                float gold = e.bossBarGold();
                d.rect(b, x, y, w * e.bossBarFraction(), 0.1f, rage || gold > 0 ? BOSS_BAR_RAGE : BOSS_BAR);
                if (gold > 0) { // a golden life on top of the red one, lost first
                    d.rect(b, x, y, w * gold, 0.1f, BOSS_BAR_GOLD);
                    d.rect(b, x, y + 0.07f, w * gold, 0.02f, BOSS_BAR_GOLD_SHINE);
                }
                float nw = game.textWidth(e.bossName(), 0.3f); // (a dark backing: pale castle walls behind it)
                d.rect(b, x + w / 2f - nw / 2f - 0.08f, y + 0.08f, nw + 0.16f, 0.27f, HUD_PANEL, 0.7f);
                game.text(e.bossName(), x + w / 2f, y + 0.3f, 0.3f, HUD_TEXT, Align.center);
                return;
            }
        }
    }

    private void drawGunWheel(SpriteBatch b) {
        Draw d = game.draw;
        Player p = world.getPlayer();
        d.rect(b, 0, 0, Arena.VIEW_W, Arena.VIEW_H, Color.BLACK, 0.55f);
        float cx = Arena.VIEW_W / 2f;
        float cy = Arena.VIEW_H / 2f - 0.1f;
        int n = p.getWeapons().size;
        // an oval that grows with the collection, so slots never overlap
        float rx = n <= 2 ? 0.9f : n <= 4 ? 1.4f : 2.3f;
        float ry = n <= 2 ? 0.9f : n <= 4 ? 1.35f : 1.62f;
        float sw = n > 6 ? 1.25f : 1.5f;
        float sh = 0.8f;
        // with a big collection the cards overlap: the chosen one is drawn last (on top) and a bit bigger
        for (int k = 0; k <= n; k++) {
            int i = k < n ? k : wheelChoice;
            boolean sel = i == wheelChoice;
            if (i < 0 || i >= n || sel != (k == n)) continue;
            Weapon w = p.getWeapons().get(i);
            float a = 90f - 360f * i / n;
            float x = cx + MathUtils.cosDeg(a) * rx;
            float y = cy + MathUtils.sinDeg(a) * ry;
            float grow = sel && n > 6 ? 1.2f : 1f;
            float cw = sw * grow;
            float ch = sh * grow;
            if (sel) {
                d.rect(b, x - cw / 2f - 0.03f, y - ch / 2f - 0.03f, cw + 0.06f, ch + 0.06f, BANNER);
            }
            d.rect(b, x - cw / 2f, y - ch / 2f, cw, ch, WHEEL_SLOT);
            Sprites.GunSprite g = game.sprites.gun(w.getSprite());
            g.drawIcon(b, x, y + 0.17f * grow, grow * Math.min(1.4f, Math.min((sw - 0.2f) / g.w, 0.38f / g.h)));
            game.text(w.displayName().toUpperCase(), x, y - 0.08f * grow, 0.22f * grow, sel ? BANNER : HUD_TEXT, Align.center);
            game.text(w.ammoText(), x, y - 0.22f * grow, 0.2f * grow, w.isEmpty() ? Color.RED : HUD_DIM, Align.center);
        }
        game.text("STICK choose   release Y equip", cx, 0.22f, 0.24f, HUD_DIM, Align.center);
    }

    private void drawPause(SpriteBatch b) {
        game.draw.rect(b, 0, 0, Arena.VIEW_W, Arena.VIEW_H, Color.BLACK, 0.6f);
        if (optionsOpen) {
            game.draw.rect(b, 0, 0, Arena.VIEW_W, Arena.VIEW_H, Color.BLACK, 0.3f);
            options.draw(game, stateTimer);
            return;
        }
        game.text("PAUSED", Arena.VIEW_W / 2f, 3.5f, 1f, BANNER, Align.center);
        pauseMenu.draw(game, stateTimer);
        String hint = game.pad.isActive() ? "A choose   START / B resume" : "ENTER choose   ESC resume   N toggle music";
        game.text(hint, Arena.VIEW_W / 2f, 1.15f, 0.28f, HUD_DIM, Align.center);
    }

    private void drawGameOver(SpriteBatch b) {
        float a = Math.min(1f, stateTimer / 0.8f);
        game.draw.rect(b, 0, 0, Arena.VIEW_W, Arena.VIEW_H, Color.BLACK, 0.7f * a);
        game.text("YOU DIED", Arena.VIEW_W / 2f, 3.5f, 1.2f, tmpColor.set(Color.RED).mul(1, 1, 1, a), Align.center);
        String where = world.getDirector() instanceof WaveDirector
                ? "Reached wave " + ((WaveDirector) world.getDirector()).getWave()
                : "Fell in " + ((StoryDirector) world.getDirector()).getStage().title.toLowerCase();
        game.text(where + "   Kills " + world.getKills(), Arena.VIEW_W / 2f, 2.75f, 0.4f, HUD_TEXT, Align.center);
        game.text("Score " + world.getScore() + (newBest ? "   NEW BEST!" : "   Best " + game.getHighScore()),
                Arena.VIEW_W / 2f, 2.45f, 0.4f, newBest ? Color.GOLD : HUD_TEXT, Align.center);
        if (stateTimer > 1.2f) {
            overMenu.draw(game, stateTimer);
        }
    }

    private static final Color[] CONFETTI = {
            Color.valueOf("ff5fa2"), Color.valueOf("ffd23a"), Color.valueOf("5ad1ff"),
            Color.valueOf("7bed6f"), Color.valueOf("c77dff"), Color.valueOf("ff8a3a"),
    };

    /** Beat the last floor's boss: the run is won. Confetti, the run's numbers, play again or go to the menu. */
    private void drawVictory(SpriteBatch b) {
        float a = Math.min(1f, stateTimer / 1f);
        game.draw.rect(b, 0, 0, Arena.VIEW_W, Arena.VIEW_H, Color.BLACK, 0.75f * a);
        if (stateTimer > 0.6f) { // the ending picture: peace is back, and the hero flies off home
            if (normalEnding) { // the False King beaten: the critters' green fades, the real King still sulks above
                normalEndingPicture.draw(b, game.draw, game, 0.8f, 2.05f, 5.4f, 2.7f, stateTimer - 0.6f);
            } else if (trueEnding) { // Grandfluff beaten: the two kings make up on the Moon
                trueEndingPicture.draw(b, game.draw, game, 0.8f, 2.05f, 5.4f, 2.7f, stateTimer - 0.6f);
            } else {
                endingPicture.draw(b, game.draw, game, 0.8f, 2.05f, 5.4f, 2.7f, stateTimer - 0.6f);
            }
        }
        for (int i = 0; i < 70; i++) { // confetti raining down (positions from i, so no state to keep)
            float speed = 0.6f + (i * 37 % 10) * 0.08f;
            float x = (i * 0.731f + MathUtils.sin(stateTimer * 2f + i) * 0.15f) % Arena.VIEW_W;
            float y = Arena.VIEW_H + 0.2f - ((stateTimer * speed + i * 0.377f) % (Arena.VIEW_H + 0.4f));
            float w = 0.05f + 0.03f * Math.abs(MathUtils.sin(stateTimer * 6f + i));
            game.draw.rect(b, x, y, w, 0.07f, CONFETTI[i % CONFETTI.length], a);
        }
        if (trueEnding) {
            game.text("THE TRUE ENDING", Arena.VIEW_W / 2f, 1.9f, 0.8f, tmpColor.set(Color.GOLD).mul(1, 1, 1, a), Align.center);
            if (newCrown != null) { // hard mode: their golden skull crown
                game.text("A GOLDEN SKULL CROWN FOR " + newCrown.title + "!", Arena.VIEW_W / 2f, 1.42f, 0.28f,
                        Color.GOLD, Align.center);
            } else {
                game.text("Grandfluff cheers up at last - the whole kingdom is fluffy and happy again!",
                        Arena.VIEW_W / 2f, 1.42f, 0.26f, HUD_TEXT, Align.center);
            }
            game.text("Score " + world.getScore() + "   Kills " + world.getKills()
                            + (newBest ? "   NEW BEST!" : "   Best " + game.getHighScore()),
                    Arena.VIEW_W / 2f, 1.08f, 0.28f, newBest ? Color.GOLD : HUD_DIM, Align.center);
        } else if (normalEnding) {
            game.text("THE STREETS ARE QUIET", Arena.VIEW_W / 2f, 1.9f, 0.7f, tmpColor.set(Color.GOLD).mul(1, 1, 1, a), Align.center);
            game.text("The False King is beaten... but up in the clouds, the real King still sulks.",
                    Arena.VIEW_W / 2f, 1.42f, 0.26f, HUD_TEXT, Align.center);
            game.text("NORMAL ENDING   Score " + world.getScore() + "   Kills " + world.getKills()
                            + (newBest ? "   NEW BEST!" : "   Best " + game.getHighScore()),
                    Arena.VIEW_W / 2f, 1.08f, 0.28f, newBest ? Color.GOLD : HUD_DIM, Align.center);
        } else {
            game.text("VICTORY!", Arena.VIEW_W / 2f, 1.9f, 0.9f, tmpColor.set(Color.GOLD).mul(1, 1, 1, a), Align.center);
            game.text("The Fluffy King is dethroned - peace returns to the kingdom!",
                    Arena.VIEW_W / 2f, 1.38f, 0.3f, HUD_TEXT, Align.center);
            game.text("Score " + world.getScore() + "   Kills " + world.getKills() + (newBest ? "   NEW BEST!" : "   Best " + game.getHighScore())
                            + (Hero.current.easy() ? "   Baby's wins: " + game.getEasyWins() : "   Runs won: " + game.getWins()),
                    Arena.VIEW_W / 2f, 1.03f, 0.3f, newBest ? Color.GOLD : HUD_DIM, Align.center);
        }
        if (stateTimer > 2.5f) {
            (normalEnding ? normalEndingMenu : wonMenu).draw(game, stateTimer);
        }
    }

    private void drawStageClear(SpriteBatch b) {
        float a = Math.min(1f, stateTimer / 1f);
        game.draw.rect(b, 0, 0, Arena.VIEW_W, Arena.VIEW_H, Color.BLACK, 0.75f * a);
        if (rocketTrip()) { // the King is beaten - but the family's rocket is waiting
            game.text("THE KINGDOM IS SAVED!", Arena.VIEW_W / 2f, 3.75f, 0.8f, tmpColor.set(BANNER).mul(1, 1, 1, a), Align.center);
            game.text("...but the Moon is calling.", Arena.VIEW_W / 2f, 3.1f, 0.4f, HUD_TEXT, Align.center);
            game.text("Next: " + Stage.MOON.title, Arena.VIEW_W / 2f, 2.8f, 0.34f, HUD_DIM, Align.center);
            game.text("Score " + world.getScore() + "   Kills " + world.getKills() + (newBest ? "   NEW BEST!" : ""),
                    Arena.VIEW_W / 2f, 2.45f, 0.4f, newBest ? Color.GOLD : HUD_TEXT, Align.center);
            if (stateTimer > 1.2f) {
                clearMenu.draw(game, stateTimer);
            }
            return;
        }
        int floor = ((StoryDirector) world.getDirector()).getFloor();
        game.text("FLOOR " + floor + " CLEAR!", Arena.VIEW_W / 2f, 3.75f, 1.1f, tmpColor.set(BANNER).mul(1, 1, 1, a), Align.center);
        Stage stage = ((StoryDirector) world.getDirector()).getStage();
        Stage next = Stage.forFloor(floor + 1);
        game.text(stage.clearText, Arena.VIEW_W / 2f, 3.1f, 0.4f, HUD_TEXT, Align.center);
        // after the last stage, floors replay it with tougher enemies
        game.text("Next: floor " + (floor + 1) + " - " + (next != stage ? next.title : stage.title.toLowerCase() + ", but meaner"),
                Arena.VIEW_W / 2f, 2.8f, 0.34f, HUD_DIM, Align.center);
        game.text("Score " + world.getScore() + "   Kills " + world.getKills() + (newBest ? "   NEW BEST!" : ""),
                Arena.VIEW_W / 2f, 2.45f, 0.4f, newBest ? Color.GOLD : HUD_TEXT, Align.center);
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
        Flufficon.flush(); // the run's kills
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
