package com.trush.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.trush.game.i18n.Language;
import com.trush.game.gfx.Sprites.GunSprite;
import com.trush.game.input.Gamepad;
import com.trush.game.input.Gamepad.Button;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Pickup.Type;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import com.trush.game.world.GameWorld.Sale;
import com.trush.game.world.GameWorld.SaleGroup;

final class ShopMenu {
    private static final Color WOOD = Color.valueOf("714c42");
    private static final Color INK = Color.valueOf("26212c");
    private static final Color TILE = Color.valueOf("38313c");
    private static final Color FOCUS = Color.valueOf("564442");
    private static final Color BRASS = Color.valueOf("efc777");
    private static final Color PAPER = Color.valueOf("fff0d3");
    private static final Color MUTED = Color.valueOf("c8b3ac");
    private static final Color STEEL = Color.valueOf("9ba7b4");
    private static final Color AMMO_GREEN = Color.valueOf("598a63");

    private static final SaleGroup[] GROUPS = SaleGroup.values();
    private static final String[] TAB_LABELS = {"GEAR", "SUPPLIES", "FLOOR LOOT"};
    private static final int COLUMNS = 4;
    private static final int PAGE_SIZE = COLUMNS * 3;
    private static final float GRID_X = .53f;
    private static final float GRID_TOP = 3.64f;
    private static final float COLUMN_STEP = .96f;
    private static final float ROW_STEP = .84f;
    private static final float TILE_W = .86f;
    private static final float TILE_H = .74f;
    private static final float DETAIL_X = 5.44f;

    private static final Rectangle PREVIOUS = new Rectangle(.53f, .79f, .60f, .31f);
    private static final Rectangle NEXT = new Rectangle(3.61f, .79f, .66f, .31f);
    private static final Rectangle SELL = new Rectangle(4.55f, 1.30f, 1.78f, .42f);
    private static final Rectangle BELL = new Rectangle(4.55f, .80f, 1.78f, .39f);
    private static final Rectangle CLOSE = new Rectangle(6.15f, 4.18f, .29f, .31f);
    private static final Rectangle CANCEL = new Rectangle(1.77f, 1.68f, 1.60f, .48f);
    private static final Rectangle ACCEPT = new Rectangle(3.63f, 1.68f, 1.60f, .48f);

    private enum Confirmation { SALE, BELL }

    private final Array<Sale> entries = new Array<>();
    private final Vector2 mouse = new Vector2();
    private final Rectangle bounds = new Rectangle();
    private Array<Sale> sales;
    private int tab;
    private int index;
    private boolean stickHeld;
    private boolean travelling;
    private Confirmation confirmation;
    private boolean accept;
    private String receipt;
    private Language language;

    boolean isOpen() {
        return sales != null;
    }

    void open(GameWorld world, Language language) {
        this.language = language;
        travelling = world.atTravellingCounter();
        confirmation = null;
        receipt = null;
        tab = 0;
        index = 0;
        refresh(world);
        if (entries.isEmpty() && sales.notEmpty()) selectTab(sales.first().group().ordinal());
    }

    private void refresh(GameWorld world) {
        sales = world.shopSales(language);
        filter();
    }

    private void filter() {
        entries.clear();
        for (Sale sale : sales) {
            if (sale.group() == GROUPS[tab]) entries.add(sale);
        }
        index = MathUtils.clamp(index, 0, Math.max(0, entries.size - 1));
    }

    private void selectTab(int next) {
        tab = Math.floorMod(next, GROUPS.length);
        index = 0;
        filter();
    }

    private int page() {
        return index / PAGE_SIZE;
    }

    private int pages() {
        return Math.max(1, (entries.size + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private void turnPage(int direction) {
        int page = MathUtils.clamp(page() + direction, 0, pages() - 1);
        if (page != page()) index = page * PAGE_SIZE;
    }

    void scroll(float amount) {
        if (isOpen() && confirmation == null && amount != 0) turnPage(amount > 0 ? 1 : -1);
    }

    private void move(int amount) {
        int next = index + amount;
        if (next >= 0 && next < entries.size) index = next;
    }

    void update(ETGFanfic game, GameWorld world) {
        Gamepad pad = game.pad;
        Input input = Gdx.input;
        game.viewport.unproject(mouse.set(input.getX(), input.getY()));
        boolean click = input.isButtonJustPressed(Input.Buttons.LEFT);
        if (input.isKeyJustPressed(Input.Keys.ESCAPE) || pad.justPressed(Button.B)
                || pad.justPressed(Button.START) || click && CLOSE.contains(mouse)) {
            if (confirmation != null) confirmation = null;
            else sales = null;
            return;
        }
        boolean choose = input.isKeyJustPressed(Input.Keys.ENTER) || input.isKeyJustPressed(Input.Keys.SPACE)
                || pad.justPressed(Button.A);
        Vector2 stick = pad.leftStick();
        boolean stickMove = !stickHeld && stick.len2() > .36f;
        stickHeld = stick.len2() > .12f;
        boolean horizontal = Math.abs(stick.x) >= Math.abs(stick.y);
        boolean left = input.isKeyJustPressed(Input.Keys.LEFT) || input.isKeyJustPressed(Input.Keys.A)
                || pad.justPressed(Button.LEFT) || stickMove && horizontal && stick.x < 0;
        boolean right = input.isKeyJustPressed(Input.Keys.RIGHT) || input.isKeyJustPressed(Input.Keys.D)
                || pad.justPressed(Button.RIGHT) || stickMove && horizontal && stick.x > 0;

        if (confirmation != null) {
            updateConfirmation(world, choose, click, left, right);
            return;
        }
        if (input.isKeyJustPressed(Input.Keys.Q) || pad.justPressed(Button.LB)) selectTab(tab - 1);
        if (input.isKeyJustPressed(Input.Keys.E) || pad.justPressed(Button.RB)) selectTab(tab + 1);
        if (input.isKeyJustPressed(Input.Keys.PAGE_UP) || pad.justPressed(Button.LT)) turnPage(-1);
        if (input.isKeyJustPressed(Input.Keys.PAGE_DOWN) || pad.justPressed(Button.RT)) turnPage(1);
        if (left) move(-1);
        else if (right) move(1);
        else if (input.isKeyJustPressed(Input.Keys.UP) || input.isKeyJustPressed(Input.Keys.W)
                || pad.justPressed(Button.UP) || stickMove && !horizontal && stick.y > 0) move(-COLUMNS);
        else if (input.isKeyJustPressed(Input.Keys.DOWN) || input.isKeyJustPressed(Input.Keys.S)
                || pad.justPressed(Button.DOWN) || stickMove && !horizontal && stick.y < 0) move(COLUMNS);

        if (travelling && (input.isKeyJustPressed(Input.Keys.B) || pad.justPressed(Button.Y)
                || click && BELL.contains(mouse))) {
            confirm(Confirmation.BELL);
        } else if (click) {
            clickInventory();
        } else if (choose && entries.notEmpty()) {
            confirm(Confirmation.SALE);
        } else if (input.getDeltaX() != 0 || input.getDeltaY() != 0) {
            int hovered = hoveredEntry();
            if (hovered >= 0) index = hovered;
        }
    }

    private void clickInventory() {
        for (int i = 0; i < GROUPS.length; i++) {
            if (tabBounds(i).contains(mouse)) {
                selectTab(i);
                return;
            }
        }
        if (PREVIOUS.contains(mouse)) {
            turnPage(-1);
        } else if (NEXT.contains(mouse)) {
            turnPage(1);
        } else if (entries.notEmpty()) {
            int hovered = hoveredEntry();
            if (hovered >= 0) index = hovered;
            if (hovered >= 0 || SELL.contains(mouse)) confirm(Confirmation.SALE);
        }
    }

    private void confirm(Confirmation action) {
        confirmation = action;
        accept = false;
    }

    private void updateConfirmation(GameWorld world, boolean choose, boolean click, boolean left, boolean right) {
        if (left) accept = false;
        if (right) accept = true;
        if (click) {
            if (ACCEPT.contains(mouse)) accept = true;
            else if (CANCEL.contains(mouse)) accept = false;
            else return;
        } else if (!choose) {
            return;
        }
        if (accept && confirmation == Confirmation.BELL) {
            world.ringShopBell();
            sales = null;
        } else if (accept) {
            Sale sale = entries.get(index);
            receipt = world.sell(sale) ? language.format("SOLD FOR %s", GameWorld.coinPrice(sale.price(), language)) : "ITEM COULD NOT BE SOLD";
            refresh(world);
        }
        confirmation = null;
    }

    private Rectangle tabBounds(int tab) {
        return bounds.set(GRID_X + tab * 1.27f, 3.77f, 1.20f, .31f);
    }

    private Rectangle slotBounds(int slot) {
        return bounds.set(GRID_X + slot % COLUMNS * COLUMN_STEP,
                GRID_TOP - TILE_H - slot / COLUMNS * ROW_STEP, TILE_W, TILE_H);
    }

    private int hoveredEntry() {
        int start = page() * PAGE_SIZE;
        for (int i = start; i < Math.min(start + PAGE_SIZE, entries.size); i++) {
            if (slotBounds(i - start).contains(mouse)) return i;
        }
        return -1;
    }

    void draw(ETGFanfic game, GameWorld world) {
        game.draw.rect(game.batch, 0, 0, Arena.VIEW_W, Arena.VIEW_H, Color.BLACK, .68f);
        game.draw.rect(game.batch, .32f, .31f, 6.36f, 4.38f, WOOD);
        game.draw.rect(game.batch, .36f, .35f, 6.28f, 4.30f, INK);
        game.draw.rect(game.batch, .36f, 4.12f, 6.28f, .53f, FOCUS);
        game.batch.draw(game.sprites.prop("baallo"), .48f, 4.16f, .35f, .42f);
        game.text("BAA-LLO'S COUNTER", .98f, 4.51f, .43f, PAPER, Align.left);
        game.batch.draw(game.sprites.prop("coin_0"), 5.05f, 4.26f, .19f, .19f);
        game.text(GameWorld.coinPrice(world.getPlayer().getCoins(), language), 6.01f, 4.45f, .29f, BRASS, Align.right);
        button(game, CLOSE, "X", false);
        for (int i = 0; i < GROUPS.length; i++) button(game, tabBounds(i), TAB_LABELS[i], i == tab);
        drawGrid(game);
        drawDetails(game);
        if (receipt != null) game.text(receipt, 2.40f, .72f, .25f, BRASS, Align.center);
        boolean pad = game.pad.isActive();
        String hints = pad ? "D-PAD MOVE   LB/RB TABS   LT/RT PAGES   A SELECT   B CLOSE"
                : "ARROWS MOVE   Q/E TABS   SCROLL PAGES   ENTER SELECT   ESC CLOSE";
        game.text(hints, Arena.VIEW_W / 2f, .52f, .25f, MUTED, Align.center);
        if (confirmation != null) drawConfirmation(game);
    }

    private void drawGrid(ETGFanfic game) {
        int start = page() * PAGE_SIZE;
        for (int slot = 0; slot < PAGE_SIZE; slot++) {
            Rectangle tile = slotBounds(slot);
            int item = start + slot;
            boolean focused = item < entries.size && index == item;
            box(game, tile, focused);
            if (item >= entries.size) continue;
            Sale sale = entries.get(item);
            float cx = tile.x + tile.width / 2f;
            drawIcon(game, sale, cx, tile.y + .46f, .46f);
            game.batch.draw(game.sprites.prop("coin_0"), cx - .17f, tile.y + .07f, .13f, .13f);
            game.text(Integer.toString(sale.price()), cx + .09f, tile.y + .19f, .25f, BRASS, Align.center);
        }
        if (entries.isEmpty()) {
            game.draw.rect(game.batch, 1.15f, 2.06f, 2.50f, .46f, INK);
            game.fitText("NOTHING TO SELL HERE", 2.40f, 2.36f, .34f, 2.3f, PAPER, Align.center);
        }
        if (pages() > 1) {
            if (page() > 0) button(game, PREVIOUS, "<", false);
            if (page() + 1 < pages()) button(game, NEXT, ">", false);
            game.text(game.trFormat("PAGE %s / %s", page() + 1, pages()), 2.40f, 1.01f, .28f, MUTED, Align.center);
        }
    }

    private void drawDetails(ETGFanfic game) {
        game.draw.rect(game.batch, 4.38f, .78f, 2.12f, 3.30f, TILE);
        if (entries.notEmpty()) {
            Sale sale = entries.get(index);
            game.draw.circle(game.batch, DETAIL_X, 3.31f, .47f, FOCUS);
            drawIcon(game, sale, DETAIL_X, 3.31f, .78f);
            detailText(game, language.upper(sale.title()), 2.70f, .34f, PAPER);
            detailText(game, sale.source(), 2.36f, .27f, MUTED);
            button(game, SELL, game.trFormat("SELL FOR %s", GameWorld.coinPrice(sale.price(), language)), false);
        }
        if (travelling) {
            button(game, BELL, game.pad.isActive() ? "RING BELL [Y]" : "RING BELL [B]", false);
        }
    }

    private void detailText(ETGFanfic game, String label, float y, float scale, Color colour) {
        float fit = Math.min(1f, 1.88f / game.textWidth(label, scale));
        game.text(label, DETAIL_X, y, scale * fit, colour, Align.center);
    }

    private void drawConfirmation(ETGFanfic game) {
        game.draw.rect(game.batch, 0, 0, Arena.VIEW_W, Arena.VIEW_H, Color.BLACK, .56f);
        game.draw.rect(game.batch, 1.49f, 1.46f, 4.02f, 2.02f, WOOD);
        game.draw.rect(game.batch, 1.53f, 1.50f, 3.94f, 1.94f, INK);
        boolean selling = confirmation == Confirmation.SALE;
        Sale sale = selling ? entries.get(index) : null;
        String title = selling ? game.trFormat("SELL %s?", language.upper(sale.title())) : "START THE NEXT WAVE?";
        game.fitText(title, 3.5f, 3.12f, .37f, 3.7f, PAPER, Align.center);
        if (selling) drawIcon(game, sale, 3.5f, 2.59f, .55f);
        button(game, CANCEL, selling ? "KEEP" : "STAY", !accept);
        button(game, ACCEPT, selling ? game.trFormat("SELL FOR %s", GameWorld.coinPrice(sale.price(), language)) : "RING BELL", accept);
        game.text(game.pad.isActive() ? "LEFT / RIGHT   A CONFIRM" : "LEFT / RIGHT   ENTER CONFIRM",
                3.5f, 1.62f, .22f, MUTED, Align.center);
    }

    private void box(ETGFanfic game, Rectangle r, boolean focused) {
        game.draw.rect(game.batch, r.x, r.y, r.width, r.height, focused ? BRASS : WOOD);
        game.draw.rect(game.batch, r.x + .025f, r.y + .025f, r.width - .05f, r.height - .05f,
                focused ? FOCUS : TILE);
    }

    private void button(ETGFanfic game, Rectangle r, String label, boolean focused) {
        box(game, r, focused);
        game.fitText(label, r.x + r.width / 2f, r.y + r.height / 2f + .045f,
                .27f, r.width - .1f, focused ? BRASS : PAPER, Align.center);
    }

    private void drawIcon(ETGFanfic game, Sale sale, float x, float y, float size) {
        switch (sale.type()) {
            case GUN -> {
                GunSprite gun = game.sprites.gun(sale.sprite());
                gun.drawIcon(game.batch, x, y, size / Math.max(gun.w, gun.h));
            }
            case ITEM -> game.batch.draw(game.sprites.item(sale.sprite()), x - size / 2f, y - size / 2f, size, size);
            case KEY -> game.draw.texture(game.batch, game.draw.keyIcon, x - size / 2f, y - size / 4f, size, size / 2f);
            case BLANK -> game.draw.texture(game.batch, game.draw.blankIcon, x - size / 2f, y - size / 2f, size, size);
            case HEART, HALF_HEART, HEART_CONTAINER -> {
                var heart = switch (sale.type()) {
                    case HALF_HEART -> game.draw.heartHalf;
                    case HEART_CONTAINER -> game.draw.heartGold;
                    default -> game.draw.heartFull;
                };
                game.draw.texture(game.batch, heart, x - size / 2f, y - size * .44f, size, size * .88f);
            }
            case AMMO, SPREAD_AMMO -> {
                game.draw.rect(game.batch, x - size * .42f, y - size * .23f, size * .84f, size * .36f,
                        sale.type() == Type.AMMO ? AMMO_GREEN : WOOD);
                for (int i = -1; i <= 1; i++) {
                    game.draw.rect(game.batch, x + size * (.20f * i - .055f), y - size * .04f,
                            size * .11f, size * .29f, BRASS);
                }
            }
            case JUNK -> {
                game.draw.rect(game.batch, x - size * .25f, y - size * .30f, size * .50f, size * .60f, STEEL);
                game.draw.rect(game.batch, x - size * .25f, y + size * .12f, size * .50f, size * .08f, FOCUS);
            }
            case COIN -> game.batch.draw(game.sprites.prop("coin_0"), x - size / 2f, y - size / 2f, size, size);
        }
    }
}
