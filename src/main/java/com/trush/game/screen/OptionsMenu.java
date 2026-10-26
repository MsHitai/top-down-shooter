package com.trush.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.trush.game.input.Gamepad;
import com.trush.game.input.Gamepad.Button;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.world.Stage;

/**
 * The options, drawn over whatever screen opened them (the title, the pause menu): music and sound
 * volume, fullscreen, the controls, and - not in the middle of a run - resetting all progress, which
 * asks "are you sure?" first. Left / right (or A) change a setting.
 */
class OptionsMenu {

    /** What {@link #update} reports. */
    enum Result { OPEN, CLOSED, RESET }

    private static final Color TITLE = Color.valueOf("ffd166");
    private static final Color TEXT = Color.valueOf("f5f0e6");
    private static final Color DIM = Color.valueOf("8a8079");
    private static final Color WARN = Color.valueOf("ff6a5a");
    private static final float STEP = 0.1f;

    private static final int MUSIC = 0;
    private static final int SOUND = 1;
    private static final int FULLSCREEN = 2;
    private static final int CONTROLS = 3;
    private static final int STATS = 4;

    private enum Page { MAIN, CONTROLS, STATS, CONFIRM_RESET }

    /** Offers "reset progress" (the title screen does, the pause menu doesn't). */
    private final boolean canReset;
    private final MenuList menu = new MenuList(3.5f, 0.32f, 0.45f);
    private final MenuList confirm = new MenuList(2.1f, 0.32f, 0.45f);
    private Page page = Page.MAIN;
    private boolean stickHeld;

    OptionsMenu(boolean canReset) {
        this.canReset = canReset;
        confirm.setItems("NO, KEEP IT", "YES, RESET");
    }

    /** Opens on the first line. */
    void open() {
        page = Page.MAIN;
        menu.select(0);
    }

    private int resetIndex() {
        return canReset ? 5 : -1;
    }

    private int backIndex() {
        return canReset ? 6 : 5;
    }

    private void refresh(ETGFanfic game) {
        String music = "MUSIC  < " + percent(game.getMusicVolume()) + " >";
        String sound = "SOUND  < " + percent(game.getSoundVolume()) + " >";
        String full = "FULLSCREEN  " + (game.isFullscreen() ? "ON" : "OFF");
        if (canReset) {
            menu.setItems(music, sound, full, "CONTROLS", "STATS", "RESET PROGRESS", "BACK");
        } else {
            menu.setItems(music, sound, full, "CONTROLS", "STATS", "BACK");
        }
    }

    private static String percent(float v) {
        return Math.round(v * 100) + "%";
    }

    Result update(ETGFanfic game) {
        Gamepad pad = game.pad;
        boolean back = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || pad.justPressed(Button.B)
                || pad.justPressed(Button.START);
        switch (page) {
            case CONTROLS:
            case STATS:
                if (back || Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                        || Gdx.input.justTouched() || pad.justPressed(Button.A)) {
                    page = Page.MAIN;
                }
                return Result.OPEN;
            case CONFIRM_RESET: {
                int chosen = confirm.update(game);
                if (back || chosen == 0) {
                    page = Page.MAIN;
                } else if (chosen == 1) {
                    game.resetProgress();
                    game.sfx.play("break", 0.8f);
                    page = Page.MAIN;
                    return Result.RESET;
                }
                return Result.OPEN;
            }
            default:
                break;
        }

        refresh(game);
        int chosen = menu.update(game);
        if (back) {
            return Result.CLOSED;
        }
        int sel = menu.selected();
        int dir = sideways(game);
        if (dir != 0 && (sel == MUSIC || sel == SOUND || sel == FULLSCREEN)) {
            change(game, sel, dir, false);
        }
        if (chosen == MUSIC || chosen == SOUND || chosen == FULLSCREEN) {
            change(game, chosen, 1, true);
        } else if (chosen == CONTROLS) {
            page = Page.CONTROLS;
        } else if (chosen == STATS) {
            page = Page.STATS;
        } else if (chosen >= 0 && chosen == resetIndex()) {
            page = Page.CONFIRM_RESET;
            confirm.select(0);
        } else if (chosen >= 0 && chosen == backIndex()) {
            return Result.CLOSED;
        }
        return Result.OPEN;
    }

    /** -1 / 1 for a push left / right this frame (keys, d-pad, stick), 0 otherwise. */
    private int sideways(ETGFanfic game) {
        Gamepad pad = game.pad;
        float x = pad.leftStick().x;
        boolean stickLeft = !stickHeld && x < -0.5f;
        boolean stickRight = !stickHeld && x > 0.5f;
        stickHeld = Math.abs(x) > 0.3f;
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)
                || pad.justPressed(Button.LEFT) || stickLeft) {
            return -1;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)
                || pad.justPressed(Button.RIGHT) || stickRight) {
            return 1;
        }
        return 0;
    }

    /** One step of a setting; {@code wrap}: pressing A on a full volume goes round to 0. */
    private void change(ETGFanfic game, int line, int dir, boolean wrap) {
        switch (line) {
            case MUSIC:
                game.setMusicVolume(stepped(game.getMusicVolume(), dir, wrap));
                break;
            case SOUND:
                game.setSoundVolume(stepped(game.getSoundVolume(), dir, wrap));
                game.sfx.play("coin", 1f); // so you hear how loud it is now
                break;
            default:
                game.setFullscreen(!game.isFullscreen());
                break;
        }
    }

    private static float stepped(float v, int dir, boolean wrap) {
        float next = Math.round((v + dir * STEP) * 10f) / 10f;
        if (wrap && next > 1f + 0.001f) {
            return 0f;
        }
        return MathUtils.clamp(next, 0f, 1f);
    }

    void draw(ETGFanfic game, float time) {
        float cx = game.viewport.getWorldWidth() / 2f;
        switch (page) {
            case CONTROLS:
                drawControls(game, cx);
                break;
            case STATS:
                drawStats(game, cx);
                break;
            case CONFIRM_RESET:
                game.text("RESET PROGRESS?", cx, 3.6f, 0.8f, WARN, Align.center);
                game.text("Unlocked family and guns, the sea, wins,", cx, 3.0f, 0.3f, TEXT, Align.center);
                game.text("best scores and your saved run will be gone.", cx, 2.8f, 0.3f, TEXT, Align.center);
                confirm.draw(game, time);
                break;
            default:
                game.text("OPTIONS", cx, 4.3f, 0.9f, TITLE, Align.center);
                menu.draw(game, time);
                String hint = game.pad.isActive() ? "LEFT / RIGHT change   B back" : "LEFT / RIGHT change   ESC back";
                game.text(hint, cx, 0.5f, 0.28f, DIM, Align.center);
                break;
        }
    }

    /** Your records: wins, best score and wave, how far the story has got. */
    private static void drawStats(ETGFanfic game, float cx) {
        game.text("STATS", cx, 4.3f, 0.9f, TITLE, Align.center);
        int wins = game.getWins();
        int floor = game.getFurthestFloor();
        int falseKing = game.getFalseKingBeaten();
        String[][] all = {
                {"KINGDOM SAVED", wins == 1 ? "1 time" : wins + " times"},
                {"FURTHEST PLACE", floor > 0 ? Stage.forFloor(floor).title : "not out yet"},
                {"BEST SCORE", String.valueOf(game.getHighScore())},
                {"BEST ENDLESS WAVE", String.valueOf(game.getBestWave())},
                // (a secret: only shows once you've found him)
                {"FALSE KING BEATEN", falseKing == 1 ? "1 time" : falseKing + " times"},
        };
        String[][] rows = falseKing > 0 ? all : java.util.Arrays.copyOf(all, all.length - 1);
        float y = 3.3f;
        for (String[] row : rows) {
            game.text(row[0], cx - 0.15f, y, 0.38f, TEXT, Align.right);
            game.text(row[1], cx + 0.15f, y, 0.38f, TITLE, Align.left);
            y -= 0.42f;
        }
        game.text(game.pad.isActive() ? "B back" : "ESC back", cx, 0.5f, 0.28f, DIM, Align.center);
    }

    private static void drawControls(ETGFanfic game, float cx) {
        game.text("CONTROLS", cx, 4.5f, 0.9f, TITLE, Align.center);
        String[][] controls = game.pad.isActive() ? new String[][]{
                {"LEFT STICK", "move"},
                {"RIGHT STICK", "aim"},
                {"RB", "shoot"},
                {"LB", "dodge roll (invincible!)"},
                {"A", "flip a table, open a chest"},
                {"R3 (CLICK RS)", "blank - clears all bullets"},
                {"RT", "use your item"},
                {"X", "reload"},
                {"Y / HOLD Y", "next gun / gun wheel"},
                {"HOLD LT", "map - teleport to a visited room"},
                {"START", "pause"},
        } : new String[][]{
                {"WASD / ARROWS", "move"},
                {"MOUSE", "aim, hold LMB to shoot"},
                {"SPACE / RMB", "dodge roll (invincible!)"},
                {"E", "flip a table, open a chest"},
                {"Q", "blank - clears all bullets"},
                {"F", "use your item"},
                {"R", "reload"},
                {"SCROLL / TAB / 1-4", "switch gun"},
                {"HOLD M", "map - teleport to a visited room"},
                {"ESC", "pause"},
        };
        float y = 3.7f;
        for (String[] row : controls) {
            game.text(row[0], cx - 0.15f, y, 0.32f, TEXT, Align.right);
            game.text(row[1], cx + 0.15f, y, 0.32f, DIM, Align.left);
            y -= 0.24f;
        }
        game.text(game.pad.isActive() ? "B back" : "ESC back", cx, 0.5f, 0.28f, DIM, Align.center);
    }
}
