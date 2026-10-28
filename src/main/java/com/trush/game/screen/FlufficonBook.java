package com.trush.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.trush.game.enemy.Boss;
import com.trush.game.enemy.Enemy;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;
import com.trush.game.input.Gamepad;
import com.trush.game.input.Gamepad.Button;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Item;
import com.trush.game.objects.Tier;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Hero;
import com.trush.game.save.Flufficon;
import com.trush.game.save.Unlocks;
import com.trush.game.world.Stage;

/**
 * The Flufficon, opened at the trophy shelf at home (like ETG's Ammonomicon): pages of records,
 * achievements, the enemies met (and how many beaten), the bosses, the guns and the items - with a "?"
 * for whatever hasn't turned up yet. Q / E or LB / RB (or a click on a tab) turn the page, arrows / stick /
 * d-pad / mouse pick an entry, Esc / B closes. Everything it shows comes from save/Flufficon.
 */
public class FlufficonBook {

    private enum Page {
        RECORDS("RECORDS"), FEATS("ACHIEVEMENTS"), ENEMIES("ENEMIES"), BOSSES("BOSSES"), GUNS("GUNS"), ITEMS("ITEMS");

        final String title;

        Page(String title) {
            this.title = title;
        }
    }

    private static final Color TEXT = Color.valueOf("f5f0e6");
    private static final Color DIM = Color.valueOf("8a8079");
    private static final Color GOLD = Color.valueOf("ffd166");
    private static final Color PANEL = Color.valueOf("1e1416");
    private static final Color SLOT = Color.valueOf("2e2225");
    private static final Color SLOT_LIT = Color.valueOf("4a3438");
    private static final Color[] TIER_COLORS = {
            Color.valueOf("a0703c"), Color.valueOf("4f8fe0"), Color.valueOf("5cc46a"),
            Color.valueOf("e0504a"), Color.valueOf("8a6cff"),
    };

    private static final float LEFT = 0.2f;
    private static final float RIGHT = 6.8f;
    private static final float TABS_Y = 4.28f;
    private static final float CONTENT_TOP = 3.98f;
    private static final float CELL = 0.6f;
    private static final int COLS = 7;
    private static final int ROWS = 5;
    private static final float GRID_X = 0.38f;
    private static final float DETAIL_X = 4.72f;
    private static final float DETAIL_W = 1.92f;
    private static final float FEAT_ROW = 0.42f;
    private static final int FEAT_ROWS = 8;
    private static final float STICK_REPEAT = 0.18f;

    private final ETGFanfic game;
    private Page page = Page.RECORDS;
    private int selected;
    private int scroll;
    private float stickTimer;
    private final Vector2 mouse = new Vector2();
    private final Vector2 lastMouse = new Vector2();
    private final Matrix4 saved = new Matrix4();
    private final Matrix4 placed = new Matrix4();
    private final Enemy[] enemies;
    private final Enemy[] bosses;
    private final Array<Weapon> guns = new Array<>();
    private final Item[] items = Item.values();
    private final Array<String> starters = new Array<>();
    /** Portraits that failed to draw once (a body that needs the world): drawn as a "?" from then on. */
    private final boolean[] brokenEnemy;
    private final boolean[] brokenBoss;
    private boolean open;

    public FlufficonBook(ETGFanfic game) {
        this.game = game;
        enemies = new Enemy[Flufficon.ENEMIES.size];
        for (int i = 0; i < enemies.length; i++) {
            enemies[i] = Flufficon.ENEMIES.get(i).create();
        }
        bosses = new Enemy[Boss.values().length];
        for (int i = 0; i < bosses.length; i++) {
            bosses[i] = Boss.values()[i].create(0, 0, 0);
        }
        brokenEnemy = new boolean[enemies.length];
        brokenBoss = new boolean[bosses.length];
        for (String name : Flufficon.allGuns()) {
            guns.add(Weapon.byName(name));
        }
        starters.add(Weapon.rustySidearm().getName());
        for (Hero h : Hero.values()) {
            starters.add(h.starter().getName());
        }
    }

    public void open() {
        open = true;
        Flufficon.flush();
        stickTimer = 0.3f;
        lastMouse.set(Gdx.input.getX(), Gdx.input.getY());
    }

    public boolean isOpen() {
        return open;
    }

    // ------------------------------------------------------------ input

    public void update(float delta) {
        Gamepad pad = game.pad;
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || pad.justPressed(Button.B) || pad.justPressed(Button.START)) {
            open = false;
            game.sfx.play("door", 0.4f);
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q) || Gdx.input.isKeyJustPressed(Input.Keys.TAB) && shift()
                || pad.justPressed(Button.LB)) {
            turnTo((page.ordinal() + Page.values().length - 1) % Page.values().length);
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.TAB)
                || pad.justPressed(Button.RB)) {
            turnTo((page.ordinal() + 1) % Page.values().length);
        }

        int dx = 0;
        int dy = 0;
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A) || pad.justPressed(Button.LEFT)) dx = -1;
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D) || pad.justPressed(Button.RIGHT)) dx = 1;
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W) || pad.justPressed(Button.UP)) dy = -1;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S) || pad.justPressed(Button.DOWN)) dy = 1;
        stickTimer -= delta;
        Vector2 stick = pad.leftStick();
        if (dx == 0 && dy == 0 && stick.len2() > 0.3f && stickTimer <= 0) {
            if (Math.abs(stick.x) > Math.abs(stick.y)) dx = stick.x > 0 ? 1 : -1;
            else dy = stick.y > 0 ? -1 : 1;
            stickTimer = STICK_REPEAT;
        } else if (stick.len2() <= 0.3f) {
            stickTimer = 0;
        }
        int n = count();
        if (n > 0 && (dx != 0 || dy != 0)) {
            int next = selected + dx + dy * (page == Page.FEATS ? 2 : COLS);
            next = MathUtils.clamp(next, 0, n - 1);
            if (next != selected) {
                selected = next;
                game.sfx.play("hit", 0.15f);
            }
        }
        updateMouse();
        keepInView();
    }

    private boolean shift() {
        return Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
    }

    void turnTo(int index) {
        page = Page.values()[MathUtils.clamp(index, 0, Page.values().length - 1)];
        selected = 0;
        scroll = 0;
        game.sfx.play("swish", 0.4f);
    }

    /** Clicking a tab turns to it; pointing at an entry picks it (only when the mouse really moved). */
    private void updateMouse() {
        boolean moved = lastMouse.dst2(Gdx.input.getX(), Gdx.input.getY()) > 4f;
        lastMouse.set(Gdx.input.getX(), Gdx.input.getY());
        mouse.set(Gdx.input.getX(), Gdx.input.getY());
        game.viewport.unproject(mouse);
        if (Gdx.input.justTouched() && Math.abs(mouse.y - (TABS_Y - 0.1f)) < 0.2f) {
            int tab = (int) ((mouse.x - LEFT) / tabWidth());
            if (tab >= 0 && tab < Page.values().length && tab != page.ordinal()) {
                turnTo(tab);
            }
            return;
        }
        if (!moved && !Gdx.input.justTouched()) return;
        int n = count();
        if (isGrid()) {
            int col = (int) Math.floor((mouse.x - GRID_X) / CELL);
            int row = (int) Math.floor((CONTENT_TOP - mouse.y) / CELL);
            if (col >= 0 && col < COLS && row >= 0 && row < ROWS) {
                int i = (scroll + row) * COLS + col;
                if (i < n) selected = i;
            }
        } else if (page == Page.FEATS) {
            int row = (int) Math.floor((CONTENT_TOP - mouse.y) / FEAT_ROW);
            int col = mouse.x < (LEFT + RIGHT) / 2f ? 0 : 1;
            if (row >= 0 && row < FEAT_ROWS && mouse.x > LEFT && mouse.x < RIGHT) {
                int i = (scroll + row) * 2 + col;
                if (i < n) selected = i;
            }
        }
    }

    private void keepInView() {
        if (!isGrid() && page != Page.FEATS) return;
        int row = page == Page.FEATS ? selected / 2 : selected / COLS;
        int rows = page == Page.FEATS ? FEAT_ROWS : ROWS;
        if (row < scroll) scroll = row;
        if (row >= scroll + rows) scroll = row - rows + 1;
    }

    private boolean isGrid() {
        return page == Page.ENEMIES || page == Page.BOSSES || page == Page.GUNS || page == Page.ITEMS;
    }

    private int count() {
        switch (page) {
            case FEATS:
                return Flufficon.Achievement.values().length;
            case ENEMIES:
                return enemies.length;
            case BOSSES:
                return bosses.length;
            case GUNS:
                return guns.size;
            case ITEMS:
                return items.length;
            default:
                return 0;
        }
    }

    // ------------------------------------------------------------ drawing

    public void draw(SpriteBatch b, Draw d) {
        boolean pad = game.pad.isActive();
        d.rect(b, 0, 0, 7, 5, Color.BLACK, 0.7f);
        d.rect(b, LEFT - 0.04f, 0.11f, RIGHT - LEFT + 0.08f, 4.78f, GOLD, 0.95f);
        d.rect(b, LEFT, 0.15f, RIGHT - LEFT, 4.7f, PANEL, 0.97f);
        game.text("THE FLUFFICON", 3.5f, 4.78f, 0.42f, GOLD, Align.center);
        float tw = tabWidth();
        for (Page p : Page.values()) {
            float cx = LEFT + tw * (p.ordinal() + 0.5f);
            boolean on = p == page;
            if (on) d.rect(b, cx - tw / 2f + 0.04f, TABS_Y - 0.26f, tw - 0.08f, 0.03f, GOLD);
            fitText(p.title, cx, TABS_Y, 0.2f, tw - 0.1f, on ? GOLD : DIM);
        }
        switch (page) {
            case RECORDS:
                drawRecords();
                break;
            case FEATS:
                drawFeats(b, d);
                break;
            default:
                drawGrid(b, d);
                drawDetail(b, d);
                break;
        }
        game.text(pad ? "LB / RB page    B close" : "Q / E page    ARROWS pick    ESC close", 3.5f, 0.42f, 0.18f, DIM, Align.center);
    }

    private float tabWidth() {
        return (RIGHT - LEFT) / Page.values().length;
    }

    private void drawRecords() {
        float y = 3.8f;
        y = line("Kingdom saved: " + game.getWins() + (game.getWins() == 1 ? " time" : " times"), y, TEXT);
        y = line("Best score: " + game.getHighScore(), y, TEXT);
        y = line("Best endless wave: " + game.getBestWave(), y, TEXT);
        if (game.getFalseKingBeaten() > 0) { // (a secret: only once you've found him)
            y = line("False King beaten: " + game.getFalseKingBeaten(), y, GOLD);
        }
        if (game.getTrueEndings() > 0) { // (a secret too: Grandfluff, up on the Moon)
            y = line("Grandfluff cheered up: " + game.getTrueEndings(), y, GOLD);
        }
        if (Unlocks.boardedRocket) {
            y = line("Rocket game best: " + game.getRocketBest(), y, TEXT);
        }
        y -= 0.12f;
        y = line("Enemies defeated: " + Flufficon.totalKills(), y, TEXT);
        y = line("Enemies met: " + Flufficon.enemiesSeen() + " / " + enemies.length, y, TEXT);
        y = line("Bosses beaten: " + Flufficon.bossesBeaten() + " / " + bosses.length, y, TEXT);
        y = line("Guns found: " + Flufficon.gunsFound() + " / " + guns.size, y, TEXT);
        y = line("Items found: " + Flufficon.itemsFound() + " / " + items.length, y, TEXT);
        line("Achievements: " + Flufficon.achievementsDone() + " / " + Flufficon.Achievement.values().length, y, GOLD);
    }

    private float line(String text, float y, Color color) {
        game.text(text, 3.5f, y, 0.26f, color, Align.center);
        return y - 0.27f;
    }

    /** Two columns of achievements (scrolling), done ones lit with a gold star; secret ones stay "???" until done. */
    private void drawFeats(SpriteBatch b, Draw d) {
        Flufficon.Achievement[] all = Flufficon.Achievement.values();
        float colW = (RIGHT - LEFT - 0.4f) / 2f;
        for (int i = scroll * 2; i < all.length && i < (scroll + FEAT_ROWS) * 2; i++) {
            Flufficon.Achievement a = all[i];
            float x = LEFT + 0.1f + (i % 2) * (colW + 0.1f);
            float top = CONTENT_TOP - (i / 2 - scroll) * FEAT_ROW;
            boolean done = a.unlocked();
            boolean hidden = a.secret && !done;
            d.rect(b, x, top - FEAT_ROW + 0.04f, colW, FEAT_ROW - 0.06f, i == selected ? SLOT_LIT : SLOT);
            if (done) {
                d.shape(b, d.star, x + 0.2f, top - FEAT_ROW / 2f + 0.01f, 0.13f, 90f, GOLD, 1f);
            } else {
                d.ring(b, x + 0.2f, top - FEAT_ROW / 2f + 0.01f, 0.1f, DIM, 0.8f);
            }
            fitText(hidden ? "???" : a.title, x + 0.4f, top - 0.06f, 0.2f, colW - 0.45f, done ? GOLD : TEXT, Align.left);
            fitText(hidden ? "A secret..." : a.description, x + 0.4f, top - 0.24f, 0.15f, colW - 0.45f, DIM, Align.left);
        }
        scrollBar(b, d, RIGHT - 0.12f, FEAT_ROWS * FEAT_ROW, (all.length + 1) / 2, FEAT_ROWS);
    }

    private void drawGrid(SpriteBatch b, Draw d) {
        int n = count();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int i = (scroll + row) * COLS + col;
                if (i >= n) continue;
                float x = GRID_X + col * CELL;
                float top = CONTENT_TOP - row * CELL;
                float cx = x + CELL / 2f;
                float cy = top - CELL / 2f;
                if (i == selected) {
                    d.rect(b, x + 0.01f, top - CELL + 0.01f, CELL - 0.02f, CELL - 0.02f, GOLD);
                }
                d.rect(b, x + 0.04f, top - CELL + 0.04f, CELL - 0.08f, CELL - 0.08f, i == selected ? SLOT_LIT : SLOT);
                Tier tier = tierOf(i);
                if (tier != null && known(i)) {
                    d.rect(b, x + 0.04f, top - CELL + 0.04f, CELL - 0.08f, 0.04f, TIER_COLORS[tier.ordinal()]);
                }
                if (!known(i)) {
                    game.text("?", cx, cy + 0.09f, 0.3f, DIM, Align.center);
                } else {
                    clipTo(b, x + 0.04f, top - CELL + 0.04f, CELL - 0.08f, CELL - 0.08f);
                    drawIcon(b, d, i, cx, cy, false);
                    unclip(b);
                }
            }
        }
        scrollBar(b, d, GRID_X + COLS * CELL + 0.06f, ROWS * CELL, (n + COLS - 1) / COLS, ROWS);
    }

    /** A scroll bar of height {@code h} when there are more rows than fit. */
    private void scrollBar(SpriteBatch b, Draw d, float x, float h, int rows, int visible) {
        if (rows <= visible) return;
        d.rect(b, x, CONTENT_TOP - h, 0.05f, h, SLOT);
        float knob = h * visible / rows;
        d.rect(b, x, CONTENT_TOP - knob - (h - knob) * scroll / (rows - visible), 0.05f, knob, DIM);
    }

    private boolean known(int i) {
        switch (page) {
            case ENEMIES:
                return Flufficon.hasSeen(Flufficon.ENEMIES.get(i).id);
            case BOSSES:
                return Flufficon.hasSeen(Boss.values()[i]);
            case GUNS:
                return Flufficon.hasFound(guns.get(i));
            case ITEMS:
                return Flufficon.hasFound(items[i]);
            default:
                return false;
        }
    }

    private Tier tierOf(int i) {
        if (page == Page.GUNS) return starters.contains(guns.get(i).getName(), false) ? null : guns.get(i).getTier();
        if (page == Page.ITEMS) return items[i].tier;
        return null;
    }

    private void drawIcon(SpriteBatch b, Draw d, int i, float cx, float cy, boolean big) {
        switch (page) {
            case ENEMIES:
                if (!brokenEnemy[i]) {
                    Enemy e = enemies[i];
                    brokenEnemy[i] = !portrait(b, d, e, cx, cy - 0.02f, MathUtils.clamp(0.19f / e.radius, 0.35f, 1.1f) * (big ? 2f : 1f));
                }
                break;
            case BOSSES:
                if (!brokenBoss[i]) {
                    Enemy e = bosses[i];
                    Boss boss = Boss.values()[i];
                    // (the ones riding something big: the cat's sofa, the building, the skunk's throne)
                    float shrink = boss == Boss.ALLEY_CAT || boss == Boss.WALKING_BUILDING || boss == Boss.SKUNK_KING ? 0.6f : 1f;
                    brokenBoss[i] = !portrait(b, d, e, cx, cy - 0.02f,
                            MathUtils.clamp(0.16f / e.radius, 0.2f, 0.9f) * shrink * (big ? 2f : 1f));
                }
                break;
            case GUNS: {
                Sprites.GunSprite g = game.sprites.gun(guns.get(i).getSprite());
                float max = big ? 1.4f : 0.46f;
                g.drawIcon(b, cx, cy, Math.min(big ? 3f : 1.4f, Math.min(max / g.w, max * 0.75f / g.h)));
                break;
            }
            case ITEMS: {
                float s = big ? 0.9f : 0.42f;
                b.draw(game.sprites.item(items[i].sprite), cx - s / 2f, cy - s / 2f, s, s);
                break;
            }
            default:
                break;
        }
    }

    private final Rectangle clipArea = new Rectangle();
    private final Rectangle scissors = new Rectangle();
    private boolean clipped;

    /** Keeps what's drawn next inside this box (big bosses would spill over their cells). */
    private void clipTo(SpriteBatch b, float x, float y, float w, float h) {
        if (page != Page.ENEMIES && page != Page.BOSSES) return;
        b.flush();
        clipArea.set(x, y, w, h);
        ScissorStack.calculateScissors(game.viewport.getCamera(), game.viewport.getScreenX(), game.viewport.getScreenY(),
                game.viewport.getScreenWidth(), game.viewport.getScreenHeight(), b.getTransformMatrix(), clipArea, scissors);
        clipped = ScissorStack.pushScissors(scissors);
    }

    private void unclip(SpriteBatch b) {
        if (!clipped) return;
        b.flush();
        ScissorStack.popScissors();
        clipped = false;
    }

    /** Draws an enemy's body at (x, y), {@code scale} times its size; false if it couldn't be drawn. */
    private boolean portrait(SpriteBatch b, Draw d, Enemy e, float x, float y, float scale) {
        e.pos.set(0, 0);
        saved.set(b.getTransformMatrix());
        placed.set(saved).translate(x, y, 0).scale(scale, scale, 1);
        b.setTransformMatrix(placed);
        try {
            e.drawPortrait(b, d, 270f);
            return true;
        } catch (RuntimeException ex) {
            Gdx.app.error("Flufficon", "can't draw " + e.getClass().getSimpleName(), ex);
            return false;
        } finally {
            b.setTransformMatrix(saved);
        }
    }

    /** The picked entry: big, with its name and what's known about it. */
    private void drawDetail(SpriteBatch b, Draw d) {
        float cx = DETAIL_X + DETAIL_W / 2f;
        d.rect(b, DETAIL_X, CONTENT_TOP - ROWS * CELL, DETAIL_W, ROWS * CELL, SLOT);
        int i = selected;
        if (i >= count()) return;
        boolean known = known(i);
        float iconY = CONTENT_TOP - 0.75f;
        if (known) {
            clipTo(b, DETAIL_X, CONTENT_TOP - 1.4f, DETAIL_W, 1.4f);
            drawIcon(b, d, i, cx, iconY, true);
            unclip(b);
        } else {
            game.text("?", cx, iconY + 0.2f, 0.8f, DIM, Align.center);
        }
        float y = CONTENT_TOP - 1.55f;
        float w = DETAIL_W - 0.16f;
        switch (page) {
            case ENEMIES: {
                Flufficon.Entry e = Flufficon.ENEMIES.get(i);
                fitText(known ? e.title.toUpperCase() : "???", cx, y, 0.22f, w, known ? GOLD : DIM);
                if (known) {
                    fitText(e.home, cx, y - 0.3f, 0.16f, w, DIM);
                    fitText("DEFEATED: " + Flufficon.killsOf(e.id), cx, y - 0.6f, 0.18f, w, TEXT);
                } else {
                    fitText("NOT MET YET", cx, y - 0.3f, 0.16f, w, DIM);
                }
                break;
            }
            case BOSSES: {
                Boss boss = Boss.values()[i];
                fitText(known ? boss.title : "???", cx, y, 0.2f, w, known ? GOLD : DIM);
                if (known) {
                    Stage stage = boss.stage != null ? boss.stage : Stage.forFloor(Stage.lastFloor());
                    fitText(stage.title, cx, y - 0.3f, 0.16f, w, DIM);
                    int beaten = Flufficon.timesBeaten(boss);
                    fitText(beaten > 0 ? "BEATEN: " + beaten : "NOT BEATEN YET", cx, y - 0.6f, 0.18f, w, beaten > 0 ? TEXT : DIM);
                } else {
                    fitText("NOT MET YET", cx, y - 0.3f, 0.16f, w, DIM);
                }
                break;
            }
            case GUNS: {
                Weapon g = guns.get(i);
                fitText(known ? g.getName().toUpperCase() : "???", cx, y, 0.2f, w, known ? GOLD : DIM);
                boolean starter = starters.contains(g.getName(), false);
                Tier tier = g.getTier();
                if (known) {
                    fitText(starter ? "A STARTER" : "TIER " + tier.name(), cx, y - 0.3f, 0.18f, w,
                            starter ? DIM : TIER_COLORS[tier.ordinal()]);
                } else {
                    fitText("NOT FOUND YET", cx, y - 0.3f, 0.16f, w, DIM);
                }
                break;
            }
            case ITEMS: {
                Item item = items[i];
                fitText(known ? item.title.toUpperCase() : "???", cx, y, 0.2f, w, known ? GOLD : DIM);
                if (known) {
                    fitText("TIER " + item.tier.name() + (item.active ? " - ACTIVE" : ""), cx, y - 0.3f, 0.18f, w,
                            TIER_COLORS[item.tier.ordinal()]);
                    wrap(item.description, cx, y - 0.58f, 0.15f, w, TEXT);
                } else {
                    fitText("NOT FOUND YET", cx, y - 0.3f, 0.16f, w, DIM);
                }
                break;
            }
            default:
                break;
        }
    }

    private void fitText(String s, float x, float y, float scale, float maxW, Color c) {
        fitText(s, x, y, scale, maxW, c, Align.center);
    }

    /** Text shrunk to fit {@code maxW}. */
    private void fitText(String s, float x, float y, float scale, float maxW, Color c, int align) {
        float w = game.textWidth(s, scale);
        game.text(s, x, y, w > maxW ? scale * maxW / w : scale, c, align);
    }

    /** Text broken into lines that fit {@code maxW}. */
    private void wrap(String s, float x, float y, float scale, float maxW, Color c) {
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String tried = line.length() == 0 ? word : line + " " + word;
            if (line.length() > 0 && game.textWidth(tried, scale) > maxW) {
                game.text(line.toString(), x, y, scale, c, Align.center);
                y -= scale * 1.15f;
                line.setLength(0);
                line.append(word);
            } else {
                line.setLength(0);
                line.append(tried);
            }
        }
        if (line.length() > 0) game.text(line.toString(), x, y, scale, c, Align.center);
    }
}
