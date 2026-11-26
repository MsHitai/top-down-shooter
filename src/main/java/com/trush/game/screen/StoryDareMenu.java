package com.trush.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.trush.game.gfx.Draw;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.world.Dare;
import com.trush.game.world.StoryModifier;

import java.util.EnumSet;

/** The skull's list of Story rules and a preview of the focused rule. */
final class StoryDareMenu {
    enum Result { OPEN, CONFIRM, CANCEL }

    private static final Dare[] DARES = Dare.values();
    private static final StoryModifier[] MODIFIERS = StoryModifier.values();
    private static final int CONFIRM = DARES.length + MODIFIERS.length;
    private static final int CANCEL = CONFIRM + 1;

    private static final float LIST_X = 0.42f;
    private static final float LIST_WIDTH = 2.77f;
    private static final float LIST_TOP = 4.20f;
    private static final float ROW_STEP = 0.234f;
    private static final float DETAILS_X = 3.55f;
    private static final float DETAILS_WIDTH = 2.84f;
    private static final float DESCRIPTION_SCALE = 0.35f;

    private static final Color PANEL = Color.valueOf("1e1416");
    private static final Color DETAILS = Color.valueOf("2e2225");
    private static final Color TEXT = Color.valueOf("f5f0e6");
    private static final Color MUTED = Color.valueOf("baacaa");
    private static final Color GOLD = Color.valueOf("ffd166");
    private static final Color DARE_COLOR = Color.valueOf("f5ad98");
    private static final Color MODIFIER_COLOR = Color.valueOf("a9ddb7");

    private final ETGFanfic game;
    private final MenuList list = new MenuList(LIST_TOP, ROW_STEP, 0.36f);
    private final TextureRegion blankIcon;
    private final EnumSet<Dare> dares = EnumSet.noneOf(Dare.class);
    private StoryModifier modifier;

    StoryDareMenu(ETGFanfic game, EnumSet<Dare> selected, StoryModifier modifier) {
        this.game = game;
        blankIcon = new TextureRegion(game.draw.blankIcon);
        dares.addAll(selected);
        this.modifier = modifier;
        list.setCenter(LIST_X + LIST_WIDTH / 2f, LIST_WIDTH / 2f);
        String[] labels = new String[CANCEL + 1];
        for (Dare dare : DARES) labels[dare.ordinal()] = dare.rule.title;
        for (StoryModifier choice : MODIFIERS) labels[DARES.length + choice.ordinal()] = choice.rule.title;
        labels[CONFIRM] = "CONFIRM";
        labels[CANCEL] = "CANCEL";
        list.setItems(labels);
        if (!dares.isEmpty()) list.select(dares.iterator().next().ordinal());
    }

    EnumSet<Dare> dares() {
        return dares.clone();
    }

    StoryModifier modifier() {
        return modifier;
    }

    Result update(boolean back) {
        if (back) return Result.CANCEL;
        int row = list.update(game);
        if (row < 0) return Result.OPEN;
        if (row == CANCEL) return Result.CANCEL;
        if (row == CONFIRM) return Result.CONFIRM;
        if (row < DARES.length) {
            Dare dare = DARES[row];
            if (dares.remove(dare)) {
                modifier = null;
            } else if (hasTwoDares()) {
                game.sfx.play("hit", 0.3f);
            } else {
                dares.add(dare);
            }
        } else if (hasTwoDares()) {
            StoryModifier choice = MODIFIERS[row - DARES.length];
            modifier = modifier == choice ? null : choice;
        } else {
            game.sfx.play("hit", 0.3f);
        }
        return Result.OPEN;
    }

    private boolean hasTwoDares() {
        return dares.size() == Dare.LIMIT;
    }

    void draw(SpriteBatch batch, Draw draw) {
        draw.rect(batch, 0, 0, 7, 5, Color.BLACK, 0.72f);
        draw.rect(batch, 0.23f, 0.23f, 6.54f, 4.54f, GOLD, 0.9f);
        draw.rect(batch, 0.26f, 0.26f, 6.48f, 4.48f, PANEL);
        game.text("STORY DARES", LIST_X, 4.61f, 0.58f, GOLD, Align.left);
        for (Dare dare : DARES) {
            drawRow(batch, draw, dare.ordinal(), dare.rule.title, icon(dare), dares.contains(dare), DARE_COLOR);
        }
        for (StoryModifier choice : MODIFIERS) {
            drawRow(batch, draw, DARES.length + choice.ordinal(), choice.rule.title, icon(choice),
                    modifier == choice, hasTwoDares() ? MODIFIER_COLOR : MUTED);
        }
        drawRow(batch, draw, CONFIRM, "CONFIRM", null, false, GOLD);
        drawRow(batch, draw, CANCEL, "CANCEL", null, false, TEXT);
        drawDetails(batch, draw);
        String controls = game.pad.isActive() ? "D-PAD browse   A select   B back" : "UP/DOWN browse   ENTER select   ESC back";
        game.text(controls, 3.5f, 0.48f, 0.29f, MUTED, Align.center);
    }

    private void drawRow(SpriteBatch batch, Draw draw, int row, String title,
                         TextureRegion icon, boolean picked, Color color) {
        float y = LIST_TOP - row * ROW_STEP;
        boolean focused = list.selected() == row;
        draw.rect(batch, LIST_X, y - 0.184f, LIST_WIDTH, 0.218f,
                focused ? GOLD : color, focused ? 0.26f : picked ? 0.18f : 0.07f);
        if (focused || picked) {
            draw.rect(batch, LIST_X, y - 0.184f, 0.035f, 0.218f, focused ? GOLD : color);
        }
        if (icon != null) drawIcon(batch, icon, LIST_X + 0.21f, y - 0.075f, 0.17f);
        game.text(title, LIST_X + 0.39f, y, 0.37f, focused ? TEXT : color, Align.left);
        if (picked) game.text("[X]", LIST_X + LIST_WIDTH - 0.10f, y, 0.32f, color, Align.right);
    }

    private void drawDetails(SpriteBatch batch, Draw draw) {
        draw.rect(batch, 3.36f, 0.57f, 3.23f, 3.67f, DETAILS);
        int row = list.selected();
        if (row < DARES.length) {
            Dare dare = DARES[row];
            drawPreview(batch, icon(dare), "DARE", dare.rule.title, dare.description, DARE_COLOR);
        } else if (row < CONFIRM) {
            StoryModifier choice = MODIFIERS[row - DARES.length];
            drawPreview(batch, icon(choice), "REWARD MODIFIER", choice.rule.title, choice.description, MODIFIER_COLOR);
        } else {
            boolean confirm = row == CONFIRM;
            drawPreview(batch, game.sprites.prop("chest_a"), "STORY SETUP", confirm ? "CONFIRM" : "CANCEL",
                    confirm ? "Use these selections for your next Story trip." : "Return to the skull menu and keep your previous selections.", GOLD);
        }

        draw.rect(batch, DETAILS_X, 1.93f, 2.84f, 0.018f, GOLD, 0.6f);
        game.text("DARES " + dares.size() + "/" + Dare.LIMIT, DETAILS_X, 1.78f, 0.36f, DARE_COLOR, Align.left);
        float y = 1.57f;
        for (Dare dare : dares) {
            game.text(dare.rule.title, DETAILS_X, y, 0.33f, TEXT, Align.left);
            y -= 0.17f;
        }
        game.text("Dares can raise chest tier.", DETAILS_X, y, 0.30f, MUTED, Align.left);
        game.text("MODIFIER", DETAILS_X, 1.05f, 0.36f, MODIFIER_COLOR, Align.left);
        String reward = modifier != null ? modifier.rule.title : hasTwoDares() ? "Choose one, or leave empty." : "Available with two Dares.";
        game.text(reward, DETAILS_X, 0.83f, 0.33f, modifier == null ? MUTED : TEXT, Align.left);
    }

    private void drawPreview(SpriteBatch batch, TextureRegion icon, String category, String title, String description, Color color) {
        game.text(category, DETAILS_X, 4.05f, 0.32f, color, Align.left);
        drawIcon(batch, icon, 4.98f, 3.46f, 0.68f);
        game.text(title, DETAILS_X, 2.97f, 0.48f, TEXT, Align.left);
        float y = 2.73f;
        String line = "";
        for (String word : description.split(" ")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (game.textWidth(candidate, DESCRIPTION_SCALE) > DETAILS_WIDTH) {
                game.text(line, DETAILS_X, y, DESCRIPTION_SCALE, TEXT, Align.left);
                y -= 0.18f;
                line = word;
            } else {
                line = candidate;
            }
        }
        game.text(line, DETAILS_X, y, DESCRIPTION_SCALE, TEXT, Align.left);
    }

    private TextureRegion icon(Dare dare) {
        return switch (dare) {
            case FAST_FORWARD -> game.sprites.item("sugar");
            case GLASS_CANNON -> game.sprites.item("hotsauce");
            case SMALL_BLANKS -> blankIcon;
            case TOUGH_CROWD -> game.sprites.item("helmet");
            case LIGHTS_OUT -> game.sprites.prop("lamp");
            case SLOW_RELOAD -> game.sprites.item("clips");
            case MIXED_UP -> game.sprites.item("scoop");
            case CROSS_DRAFT -> game.sprites.item("cloud");
            case FALLING_DUST -> game.sprites.prop("boulder");
        };
    }

    private TextureRegion icon(StoryModifier choice) {
        return switch (choice) {
            case BOUNCY -> game.sprites.item("rubber");
            case GOLD_RUSH -> game.sprites.prop("coin_big");
            case MOON_BOOTS -> game.sprites.item("boots");
            case SOFT_LANDING -> game.sprites.item("blanket");
            case QUICK_DRAW -> game.sprites.item("mittens");
        };
    }

    private void drawIcon(SpriteBatch batch, TextureRegion icon, float x, float y, float size) {
        float scale = size / Math.max(icon.getRegionWidth(), icon.getRegionHeight());
        float width = icon.getRegionWidth() * scale;
        float height = icon.getRegionHeight() * scale;
        batch.draw(icon, x - width / 2f, y - height / 2f, width, height);
    }
}
