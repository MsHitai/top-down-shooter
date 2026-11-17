package com.trush.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.trush.game.input.Gamepad.Button;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import com.trush.game.world.GameWorld.Sale;

import java.util.Locale;

final class ShopMenu {
    private static final float CENTER = Arena.VIEW_W / 2f;
    private static final int ROWS = 8;
    private final MenuList list = new MenuList(3.63f, 0.30f, 0.30f);
    private final MenuList confirm = new MenuList(2.45f, 0.38f, 0.38f);
    private Array<Sale> sales;
    private Sale selected;
    private String receipt;
    private boolean travelling;

    ShopMenu() {
        list.setVisibleRows(ROWS);
        list.setCenter(CENTER, 2.9f);
    }

    boolean isOpen() {
        return sales != null;
    }

    void open(GameWorld world) {
        travelling = world.atTravellingCounter();
        selected = null;
        receipt = null;
        refresh(world);
        list.select(0);
    }

    private int firstSale() {
        return travelling ? 2 : 1;
    }

    private void refresh(GameWorld world) {
        sales = world.shopSales();
        String[] rows = new String[sales.size + firstSale()];
        rows[0] = travelling ? "RING BELL - NEXT WAVE" : "LEAVE COUNTER";
        if (travelling) rows[1] = "LEAVE COUNTER";
        for (int i = 0; i < sales.size; i++) {
            Sale sale = sales.get(i);
            rows[i + firstSale()] = name(sale) + "  [" + sale.source() + "]  "
                    + GameWorld.coinPrice(sale.price());
        }
        list.setItems(rows);
    }

    void scroll(float amount) {
        if (selected == null && amount != 0) list.move(amount > 0 ? 1 : -1);
    }

    void update(ETGFanfic game, GameWorld world) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || game.pad.justPressed(Button.B)
                || game.pad.justPressed(Button.START)) {
            if (selected != null) selected = null;
            else sales = null;
            return;
        }
        if (selected != null) {
            int choice = confirm.update(game);
            if (choice < 0) return;
            if (choice == 1) {
                receipt = world.sell(selected)
                        ? "SOLD " + name(selected) + " FOR " + GameWorld.coinPrice(selected.price())
                        : "ITEM COULD NOT BE SOLD";
                refresh(world);
            }
            selected = null;
            return;
        }

        int choice = list.update(game);
        if (choice < 0) return;
        if (choice < firstSale()) {
            if (travelling && choice == 0) world.ringShopBell();
            sales = null;
        } else {
            selected = sales.get(choice - firstSale());
            confirm.setItems("KEEP " + name(selected), "SELL FOR " + GameWorld.coinPrice(selected.price()));
            confirm.select(0);
        }
    }

    void draw(ETGFanfic game, GameWorld world, float time) {
        game.draw.rect(game.batch, 0, 0, Arena.VIEW_W, Arena.VIEW_H, Color.BLACK, 0.65f);
        game.draw.rect(game.batch, 0.32f, 0.32f, 6.36f, 4.36f, Color.BLACK, 0.86f);
        game.text("SELL ITEMS", CENTER, 4.45f, 0.67f, Color.GOLD, Align.center);
        game.text(GameWorld.coinPrice(world.getPlayer().getCoins()) + " IN YOUR PURSE",
                CENTER, 3.97f, 0.32f, Color.WHITE, Align.center);
        if (selected == null) {
            list.draw(game, time);
            if (sales.size == 0) {
                game.text("NO ITEMS TO SELL", CENTER, 2.55f, 0.35f, Color.WHITE, Align.center);
            }
            if (sales.size + firstSale() > ROWS) {
                int at = list.selected() - firstSale() + 1;
                String position = at > 0 ? "ITEM " + at + " OF " + sales.size : sales.size + " ITEMS";
                game.text(position, CENTER, 1.20f, 0.26f, Color.LIGHT_GRAY, Align.center);
            }
            if (receipt != null) game.text(receipt, CENTER, 0.93f, 0.29f, Color.GOLD, Align.center);
        } else {
            game.text("SELL " + name(selected) + "?", CENTER, 3.08f, 0.48f, Color.WHITE, Align.center);
            game.text(selected.source(), CENTER, 2.72f, 0.29f, Color.LIGHT_GRAY, Align.center);
            confirm.draw(game, time);
        }
        String controls = game.pad.isActive() ? "D-PAD move   A choose   B back" : "SCROLL / ARROWS move   ENTER / CLICK choose   ESC back";
        game.text(controls, CENTER, 0.62f, 0.26f, Color.LIGHT_GRAY, Align.center);
    }

    private static String name(Sale sale) {
        return sale.title().toUpperCase(Locale.ROOT);
    }
}
