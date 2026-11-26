package com.trush.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Align;
import com.trush.game.enemy.Boss;
import com.trush.game.enemy.Enemy;
import com.trush.game.input.Gamepad;
import com.trush.game.input.Gamepad.Button;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.player.Hero;
import com.trush.game.save.Flufficon.Medal;
import com.trush.game.world.BossRematch;
import com.trush.game.world.BossRematch.Difficulty;
import com.trush.game.world.Dream.Rule;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** The bedroom bell's scrollable roster and preview of the selected fight. */
final class RematchMenu {
    private enum Focus { ROSTER, NORMAL, HARD, CONFIRM, CANCEL }

    private static final Color PANEL = Color.valueOf("201b29");
    private static final Color INSET = Color.valueOf("171521");
    private static final Color SLOT = Color.valueOf("2e2639");
    private static final Color SELECTED = Color.valueOf("4a3743");
    private static final Color BORDER = Color.valueOf("665166");
    private static final Color GOLD = Color.valueOf("ffd166");
    private static final Color SILVER = Color.valueOf("c5d4ea");
    private static final Color TEXT = Color.valueOf("f5f0e6");
    private static final Color MUTED = Color.valueOf("bcb0c6");

    private static final Rectangle PORTRAIT = new Rectangle(.56f, 2.44f, 2.73f, 1.65f);
    private static final Rectangle ROSTER = new Rectangle(.56f, .68f, 2.61f, 1.65f);
    private static final int VISIBLE_ROWS = 5;
    private static final float ROW_HEIGHT = ROSTER.height / VISIBLE_ROWS;
    private static final float PREVIEW_X = 3.48f;
    private static final float PREVIEW_W = 2.96f;
    private static final float PREVIEW_CENTER = PREVIEW_X + PREVIEW_W / 2f;
    private static final Rectangle NORMAL = new Rectangle(3.62f, 1.85f, 2.68f, .37f);
    private static final Rectangle HARD = new Rectangle(3.62f, 1.39f, 2.68f, .37f);
    private static final Rectangle CONFIRM = new Rectangle(3.62f, .77f, 1.55f, .40f);
    private static final Rectangle CANCEL = new Rectangle(5.26f, .77f, 1.04f, .40f);
    private static final float STICK_REPEAT = .16f;

    private final ETGFanfic game;
    private final List<Boss> bosses = Arrays.stream(Boss.values()).filter(BossRematch::available).toList();
    private final Map<Boss, Rule> rules = new EnumMap<>(Boss.class);
    private final EnemyPortrait portrait = new EnemyPortrait();
    private final Rectangle scissors = new Rectangle();
    private final Vector2 mouse = new Vector2();
    private final boolean practice = Hero.current.easy();
    private Focus focus = Focus.ROSTER;
    private Difficulty difficulty = practice ? Difficulty.PRACTICE : Difficulty.NORMAL;
    private int selected;
    private int firstVisible;
    private Enemy portraitBody;
    private boolean brokenPortrait;
    private float stickTimer = .25f;
    private boolean horizontalHeld;
    private float wheel;
    private float wheelIdle;
    private boolean closed;

    RematchMenu(ETGFanfic game) {
        this.game = game;
        if (bosses.isEmpty()) focus = Focus.CANCEL;
        else preparePreview();
    }

    boolean isClosed() {
        return closed;
    }

    void scroll(float amount) {
        wheel = wheelIdle > .3f ? amount : wheel + amount;
        wheelIdle = 0f;
    }

    BossRematch update(float delta, boolean back) {
        if (back) {
            closed = true;
            return null;
        }

        Input input = Gdx.input;
        Gamepad pad = game.pad;
        wheelIdle += delta;
        if (!bosses.isEmpty()) {
            int steps = (int) wheel;
            if (steps != 0) {
                wheel -= steps;
                select(selected + steps);
            }
            navigate(delta, input, pad);
        }

        if (input.isButtonJustPressed(Input.Buttons.LEFT)) {
            game.viewport.unproject(mouse.set(input.getX(), input.getY()));
            if (CANCEL.contains(mouse)) {
                closed = true;
            } else if (!bosses.isEmpty()) {
                if (ROSTER.contains(mouse)) {
                    int row = (int) ((ROSTER.y + ROSTER.height - mouse.y) / ROW_HEIGHT);
                    int index = firstVisible + row;
                    if (row < VISIBLE_ROWS && index < bosses.size()) select(index);
                } else if (NORMAL.contains(mouse) && !practice) {
                    chooseDifficulty(Focus.NORMAL);
                } else if (HARD.contains(mouse) && !practice) {
                    chooseDifficulty(Focus.HARD);
                } else if (CONFIRM.contains(mouse)) {
                    return rematch();
                }
            }
            return null;
        }

        if (input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.SPACE)
                || pad.justPressed(Button.A)) {
            switch (focus) {
                case ROSTER -> focus = Focus.CONFIRM;
                case NORMAL, HARD -> chooseDifficulty(focus);
                case CONFIRM -> { return rematch(); }
                case CANCEL -> closed = true;
            }
        }
        return null;
    }

    private void navigate(float delta, Input input, Gamepad pad) {
        Vector2 stick = pad.leftStick();
        boolean tilted = stick.len2() > .3f;
        boolean vertical = Math.abs(stick.y) >= Math.abs(stick.x);
        boolean horizontal = tilted && !vertical;
        boolean moveAcross = horizontal && !horizontalHeld;
        horizontalHeld = horizontal;

        stickTimer -= delta;
        if (!tilted) stickTimer = 0f;
        boolean repeat = tilted && vertical && stickTimer <= 0f;
        if (repeat) stickTimer = STICK_REPEAT;

        if (input.isKeyJustPressed(Input.Keys.LEFT) || input.isKeyJustPressed(Input.Keys.A)
                || pad.justPressed(Button.LEFT) || moveAcross && stick.x < 0f) {
            focus = focus == Focus.CANCEL ? Focus.CONFIRM : Focus.ROSTER;
        } else if (input.isKeyJustPressed(Input.Keys.RIGHT) || input.isKeyJustPressed(Input.Keys.D)
                || pad.justPressed(Button.RIGHT) || moveAcross && stick.x > 0f) {
            if (focus == Focus.ROSTER) focus = difficultyFocus();
            else if (focus == Focus.NORMAL || focus == Focus.HARD) focus = Focus.CONFIRM;
            else if (focus == Focus.CONFIRM) focus = Focus.CANCEL;
        }

        if (input.isKeyJustPressed(Input.Keys.HOME)) {
            select(0);
        } else if (input.isKeyJustPressed(Input.Keys.END)) {
            select(bosses.size() - 1);
        } else if (input.isKeyJustPressed(Input.Keys.UP) || input.isKeyJustPressed(Input.Keys.W)
                || pad.justPressed(Button.UP) || repeat && stick.y > 0f) {
            switch (focus) {
                case ROSTER -> select(selected - 1);
                case HARD -> focus = Focus.NORMAL;
                case CONFIRM, CANCEL -> focus = practice ? Focus.ROSTER : Focus.HARD;
                case NORMAL -> { }
            }
        } else if (input.isKeyJustPressed(Input.Keys.DOWN) || input.isKeyJustPressed(Input.Keys.S)
                || pad.justPressed(Button.DOWN) || repeat && stick.y < 0f) {
            switch (focus) {
                case ROSTER -> select(selected + 1);
                case NORMAL -> focus = Focus.HARD;
                case HARD -> focus = Focus.CONFIRM;
                case CONFIRM, CANCEL -> { }
            }
        }
    }

    private Focus difficultyFocus() {
        return switch (difficulty) {
            case PRACTICE -> Focus.CONFIRM;
            case NORMAL -> Focus.NORMAL;
            case HARD -> Focus.HARD;
        };
    }

    private void chooseDifficulty(Focus choice) {
        difficulty = choice == Focus.HARD ? Difficulty.HARD : Difficulty.NORMAL;
        focus = Focus.CONFIRM;
    }

    private void select(int index) {
        focus = Focus.ROSTER;
        int next = MathUtils.clamp(index, 0, bosses.size() - 1);
        if (next == selected) return;
        selected = next;
        firstVisible = MathUtils.clamp(firstVisible, selected - VISIBLE_ROWS + 1, selected);
        preparePreview();
    }

    private void preparePreview() {
        Boss boss = bosses.get(selected);
        portraitBody = boss.create(0, 0, 0);
        brokenPortrait = false;
        rules.computeIfAbsent(boss, ignored -> BossRematch.rollRule());
    }

    private BossRematch rematch() {
        Boss boss = bosses.get(selected);
        return new BossRematch(boss, difficulty, rules.get(boss));
    }

    void draw(SpriteBatch batch) {
        game.draw.rect(batch, 0, 0, 7, 5, Color.BLACK, .72f);
        game.draw.rect(batch, .32f, .31f, 6.36f, 4.38f, GOLD);
        game.draw.rect(batch, .35f, .34f, 6.3f, 4.32f, PANEL);
        game.text("BOSS REMATCHES", 3.5f, 4.51f, .45f, GOLD, Align.center);

        if (bosses.isEmpty()) {
            game.text("Beat a boss to unlock its rematch.", 3.5f, 2.76f, .32f, TEXT, Align.center);
            drawButton(batch, CANCEL, "CANCEL", Focus.CANCEL);
            return;
        }
        drawRoster(batch);
        drawPreview(batch);
        String help = game.pad.isActive() ? "D-PAD / STICK: MOVE     A: SELECT     B: CANCEL"
                : "WHEEL / UP / DOWN: BOSSES     RIGHT: OPTIONS     ENTER: SELECT     ESC: CANCEL";
        game.text(help, 3.5f, .56f, .19f, MUTED, Align.center);
    }

    private void drawRoster(SpriteBatch batch) {
        Boss boss = bosses.get(selected);
        game.draw.rect(batch, PORTRAIT.x, PORTRAIT.y, PORTRAIT.width, PORTRAIT.height, INSET);
        float cx = PORTRAIT.x + PORTRAIT.width / 2f;
        float cy = PORTRAIT.y + PORTRAIT.height / 2f;
        game.draw.ellipse(batch, cx, cy - .44f, .72f, .16f, SLOT, 1f);
        if (!brokenPortrait) {
            ScissorStack.calculateScissors(game.viewport.getCamera(), game.viewport.getScreenX(), game.viewport.getScreenY(),
                    game.viewport.getScreenWidth(), game.viewport.getScreenHeight(), batch.getTransformMatrix(), PORTRAIT, scissors);
            batch.flush();
            if (ScissorStack.pushScissors(scissors)) {
                brokenPortrait = !portrait.draw(batch, game.draw, portraitBody, cx, cy - .12f,
                        EnemyPortrait.bossScale(boss, portraitBody) * 2.6f);
                batch.flush();
                ScissorStack.popScissors();
            }
        }
        for (int row = 0; row < VISIBLE_ROWS && firstVisible + row < bosses.size(); row++) {
            int index = firstVisible + row;
            float y = ROSTER.y + ROSTER.height - (row + 1) * ROW_HEIGHT;
            boolean active = index == selected;
            game.draw.rect(batch, ROSTER.x, y + .026f, ROSTER.width, ROW_HEIGHT - .035f, active ? SELECTED : SLOT);
            if (active) {
                game.draw.rect(batch, ROSTER.x, y + .026f, .035f, ROW_HEIGHT - .035f, focus == Focus.ROSTER ? GOLD : MUTED);
            }
            fitText(bosses.get(index).title, ROSTER.x + .13f, y + .21f, .26f, ROSTER.width - .23f,
                    active ? GOLD : TEXT, Align.left);
        }

        if (bosses.size() > VISIBLE_ROWS) {
            float x = ROSTER.x + ROSTER.width + .055f;
            float knob = ROSTER.height * VISIBLE_ROWS / bosses.size();
            float travel = ROSTER.height - knob;
            game.draw.rect(batch, x, ROSTER.y, .052f, ROSTER.height, INSET);
            game.draw.rect(batch, x, ROSTER.y + travel * (1f - (float) firstVisible / (bosses.size() - VISIBLE_ROWS)),
                    .052f, knob, MUTED);
        }
    }

    private void drawPreview(SpriteBatch batch) {
        Rule rule = rules.get(bosses.get(selected));
        game.draw.rect(batch, PREVIEW_X, .68f, PREVIEW_W, 3.41f, SLOT);
        game.draw.rect(batch, PREVIEW_X + .14f, 2.77f, PREVIEW_W - .28f, .025f, BORDER);
        game.text("ACTIVE RULE", PREVIEW_CENTER, 3.92f, .24f, MUTED, Align.center);
        game.text(rule.title, PREVIEW_CENTER, 3.55f, .44f, GOLD, Align.center);
        fitText(rule.description, PREVIEW_CENTER, 3.12f, .28f, PREVIEW_W - .28f, TEXT, Align.center);
        if (practice) {
            game.text("PRACTICE", PREVIEW_CENTER, 2.52f, .32f, TEXT, Align.center);
            game.text("Baby can practise without medals.", PREVIEW_CENTER, 2.05f, .24f, MUTED, Align.center);
        } else {
            game.text("DIFFICULTY", PREVIEW_CENTER, 2.55f, .24f, MUTED, Align.center);
            drawDifficulty(batch, NORMAL, "NORMAL", "SILVER", SILVER, Focus.NORMAL);
            drawDifficulty(batch, HARD, "HARD", "GOLD", GOLD, Focus.HARD);
        }
        drawButton(batch, CONFIRM, "CONFIRM", Focus.CONFIRM);
        drawButton(batch, CANCEL, "CANCEL", Focus.CANCEL);
    }

    private void drawDifficulty(SpriteBatch batch, Rectangle box, String name, String medal, Color color, Focus target) {
        boolean active = difficultyFocus() == target;
        drawButtonFrame(batch, box, target, active ? SELECTED : INSET);
        game.draw.circle(batch, box.x + .14f, box.y + .18f, .045f, active ? GOLD : BORDER);
        game.text(name, box.x + .25f, box.y + .22f, .27f, TEXT, Align.left);
        game.text(medal, box.x + box.width - .13f, box.y + .22f, .24f, color, Align.right);
    }

    private void drawButton(SpriteBatch batch, Rectangle box, String label, Focus target) {
        boolean primary = target == Focus.CONFIRM;
        drawButtonFrame(batch, box, target, primary ? GOLD : INSET);
        game.text(label, box.x + box.width / 2f, box.y + .24f, .27f, primary ? INSET : TEXT, Align.center);
    }

    private void drawButtonFrame(SpriteBatch batch, Rectangle box, Focus target, Color fill) {
        game.draw.rect(batch, box.x, box.y, box.width, box.height, focus == target ? TEXT : BORDER);
        game.draw.rect(batch, box.x + .024f, box.y + .024f, box.width - .048f, box.height - .048f, fill);
    }

    private void fitText(String text, float x, float y, float scale, float maxWidth, Color color, int align) {
        float width = game.textWidth(text, scale);
        game.text(text, x, y, width > maxWidth ? scale * maxWidth / width : scale, color, align);
    }

    static Color medalColor(Medal medal) {
        return medal == Medal.GOLD ? GOLD : SILVER;
    }
}
