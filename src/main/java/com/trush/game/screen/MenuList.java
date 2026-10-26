package com.trush.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Align;
import com.trush.game.input.Gamepad;
import com.trush.game.input.Gamepad.Button;
import com.trush.game.launcher.ETGFanfic;

/**
 * A vertical text menu centred on the screen, driven by mouse (hover + click), keyboard
 * (W/S, arrows, ENTER/SPACE) or gamepad (d-pad / left stick + A).
 */
class MenuList {

    private static final Color TEXT = Color.valueOf("f5f0e6");
    private static final Color SELECTED = Color.valueOf("ffd166");

    private float topY;
    /** Where the items are centred and how far either side the mouse picks them (default: mid-screen). */
    private float centerX = -1f;
    private float halfWidth = 2.2f;
    private final float step;
    private final float scale;
    private String[] items = new String[0];
    private int selected;
    /** The left stick moves the selection once per push, not every frame. */
    private boolean stickHeld;
    private final Vector3 mouse = new Vector3();
    private final Color tmp = new Color();

    MenuList(float topY, float step, float scale) {
        this.topY = topY;
        this.step = step;
        this.scale = scale;
    }

    void setItems(String... items) {
        this.items = items;
        selected = MathUtils.clamp(selected, 0, items.length - 1);
    }

    void select(int index) {
        selected = index;
    }

    void setCenter(float x, float halfWidth) {
        this.centerX = x;
        this.halfWidth = halfWidth;
    }

    private float cx(ETGFanfic game) {
        return centerX >= 0 ? centerX : game.viewport.getWorldWidth() / 2f;
    }

    /** Moves the menu (one MenuList asking different questions). */
    void setTop(float y) {
        topY = y;
    }

    int selected() {
        return selected;
    }

    /** Handles input; returns the index of the item that was activated this frame, or -1. */
    int update(ETGFanfic game) {
        mouse.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        game.viewport.unproject(mouse);
        int hovered = -1;
        for (int i = 0; i < items.length; i++) {
            float y = topY - i * step;
            if (mouse.y < y + 0.05f && mouse.y > y - step + 0.05f && Math.abs(mouse.x - cx(game)) < halfWidth) {
                hovered = i;
            }
        }
        if (hovered >= 0 && (Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0)) {
            selected = hovered;
        }

        Gamepad pad = game.pad;
        float stickY = pad.leftStick().y;
        boolean stickUp = !stickHeld && stickY > 0.5f;
        boolean stickDown = !stickHeld && stickY < -0.5f;
        stickHeld = Math.abs(stickY) > 0.3f;
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)
                || pad.justPressed(Button.UP) || stickUp) {
            selected = (selected + items.length - 1) % items.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)
                || pad.justPressed(Button.DOWN) || stickDown) {
            selected = (selected + 1) % items.length;
        }

        if (Gdx.input.justTouched() && hovered >= 0) {
            selected = hovered;
            return selected;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || pad.justPressed(Button.A)) {
            return selected;
        }
        return -1;
    }

    void draw(ETGFanfic game, float time) {
        float cx = cx(game);
        float pulse = 0.65f + 0.35f * MathUtils.sin(time * 4f);
        for (int i = 0; i < items.length; i++) {
            boolean sel = i == selected;
            String label = sel ? "> " + items[i] + " <" : items[i];
            game.text(label, cx, topY - i * step, scale, sel ? tmp.set(SELECTED).mul(1, 1, 1, pulse) : TEXT, Align.center);
        }
    }
}
