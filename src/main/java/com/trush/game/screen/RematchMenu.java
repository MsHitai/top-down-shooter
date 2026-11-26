package com.trush.game.screen;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.trush.game.enemy.Boss;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.player.Hero;
import com.trush.game.save.Flufficon.Medal;
import com.trush.game.world.BossRematch;
import com.trush.game.world.BossRematch.Difficulty;
import com.trush.game.world.Dream.Rule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** The bedroom bell's boss list and the rule and difficulty preview. */
final class RematchMenu {
    private static final int PAGE_SIZE = 6;
    private static final Color PANEL = Color.valueOf("201b29");
    private static final Color GOLD = Color.valueOf("ffd166");
    private static final Color SILVER = Color.valueOf("c5d4ea");
    private static final Color TEXT = Color.valueOf("f5f0e6");

    private final ETGFanfic game;
    private final List<Boss> bosses = Arrays.stream(Boss.values()).filter(BossRematch::available).toList();
    private final Map<Boss, Rule> rules = new EnumMap<>(Boss.class);
    private final MenuList menu = new MenuList(3.57f, 0.35f, 0.28f);
    private int page;
    private Boss boss;
    private Rule rule;
    private boolean closed;

    RematchMenu(ETGFanfic game) {
        this.game = game;
        menu.setCenter(3.5f, 2.9f);
        showBosses(0);
    }

    boolean isClosed() {
        return closed;
    }

    BossRematch update(boolean back) {
        int chosen = menu.update(game);
        if (boss != null) {
            boolean practice = Hero.current.easy();
            if (back || chosen == (practice ? 1 : 2)) {
                showBosses(bosses.indexOf(boss) % PAGE_SIZE);
            } else if (chosen >= 0) {
                Difficulty difficulty = practice ? Difficulty.PRACTICE : chosen == 0 ? Difficulty.NORMAL : Difficulty.HARD;
                return new BossRematch(boss, difficulty, rule);
            }
        } else if (back || chosen == visibleBosses() + (pages() > 1 ? 2 : 0)) {
            closed = true;
        } else if (chosen >= 0 && chosen < visibleBosses()) {
            boss = bosses.get(page * PAGE_SIZE + chosen);
            rule = rules.computeIfAbsent(boss, ignored -> BossRematch.rollRule());
            menu.setTop(2.24f);
            if (Hero.current.easy()) menu.setItems("PRACTICE", "BACK");
            else menu.setItems("NORMAL  /  SILVER", "HARD  /  GOLD", "BACK");
            menu.select(0);
        } else if (chosen >= 0) {
            int direction = chosen == visibleBosses() ? -1 : 1;
            page = Math.floorMod(page + direction, pages());
            showBosses(0);
        }
        return null;
    }

    private int pages() {
        return Math.max(1, (bosses.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private int visibleBosses() {
        return Math.min(PAGE_SIZE, bosses.size() - page * PAGE_SIZE);
    }

    private void showBosses(int selected) {
        boss = null;
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < visibleBosses(); i++) {
            labels.add(bosses.get(page * PAGE_SIZE + i).title);
        }
        if (pages() > 1) {
            labels.add("PREVIOUS PAGE");
            labels.add("NEXT PAGE");
        }
        labels.add("BACK");
        menu.setTop(3.57f);
        menu.setItems(labels.toArray(String[]::new));
        menu.select(selected);
    }

    void draw(SpriteBatch batch, float time) {
        float cx = 3.5f;
        game.draw.rect(batch, 0, 0, 7, 5, Color.BLACK, 0.65f);
        game.draw.rect(batch, 0.32f, 0.32f, 6.36f, 4.36f, GOLD);
        game.draw.rect(batch, 0.36f, 0.36f, 6.28f, 4.28f, PANEL);
        game.text("BOSS REMATCHES", cx, 4.41f, 0.42f, GOLD, Align.center);
        if (boss == null) {
            String hint = bosses.isEmpty() ? "Beat a boss to unlock its rematch."
                    : "CHOOSE A BOSS" + (pages() > 1 ? "   " + (page + 1) + "/" + pages() : "");
            game.text(hint, cx, 3.91f, 0.25f, TEXT, Align.center);
        } else {
            game.text(boss.title, cx, 3.84f, 0.31f, TEXT, Align.center);
            game.text(rule.title, cx, 3.23f, 0.36f, GOLD, Align.center);
            game.text(rule.description, cx, 2.87f, 0.24f, TEXT, Align.center);
            if (Hero.current.easy()) {
                game.text("Baby can practise without medals.", cx, 1.28f, 0.23f, TEXT, Align.center);
            }
        }
        menu.draw(game, time);
    }

    static Color medalColor(Medal medal) {
        return medal == Medal.GOLD ? GOLD : SILVER;
    }
}
