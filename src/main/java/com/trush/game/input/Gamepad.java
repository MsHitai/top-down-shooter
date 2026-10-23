package com.trush.game.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.math.Vector2;

import java.util.Arrays;

/**
 * Polls the current gamepad (Xbox layout) once per frame so screens can ask for "just pressed" buttons
 * and dead-zoned sticks. {@link #isActive()} tells whether the pad or the mouse was used last, which
 * decides how the player aims and which control hints are shown.
 */
public class Gamepad {

    /** RS = clicking the right stick (R3); LT = the left trigger (L2), pressed more than halfway. */
    public enum Button { A, B, X, Y, LB, RB, RS, LT, START, BACK, UP, DOWN, LEFT, RIGHT }

    /** SDL's left-trigger axis (LEFTX, LEFTY, RIGHTX, RIGHTY, TRIGGERLEFT, TRIGGERRIGHT). */
    private static final int LEFT_TRIGGER_AXIS = 4;

    private static final float DEADZONE = 0.25f;

    private final boolean[] down = new boolean[Button.values().length];
    private final boolean[] wasDown = new boolean[Button.values().length];
    private final Vector2 left = new Vector2();
    private final Vector2 right = new Vector2();
    private final Vector2 leftRaw = new Vector2();
    private boolean active;
    private boolean available = true;

    /** Call once at the start of every frame. */
    public void update() {
        System.arraycopy(down, 0, wasDown, 0, down.length);
        Controller c = current();
        if (c == null) {
            Arrays.fill(down, false);
            left.setZero();
            right.setZero();
            leftRaw.setZero();
            active = false;
            return;
        }
        ControllerMapping m = c.getMapping();
        read(c, Button.A, m.buttonA);
        read(c, Button.B, m.buttonB);
        read(c, Button.X, m.buttonX);
        read(c, Button.Y, m.buttonY);
        read(c, Button.LB, m.buttonL1);
        read(c, Button.RB, m.buttonR1);
        read(c, Button.RS, m.buttonRightStick);
        // triggers are usually axes, not buttons
        down[Button.LT.ordinal()] = (m.buttonL2 != ControllerMapping.UNDEFINED && c.getButton(m.buttonL2))
                || (c.getAxisCount() > LEFT_TRIGGER_AXIS && c.getAxis(LEFT_TRIGGER_AXIS) > 0.5f);
        read(c, Button.START, m.buttonStart);
        read(c, Button.BACK, m.buttonBack);
        read(c, Button.UP, m.buttonDpadUp);
        read(c, Button.DOWN, m.buttonDpadDown);
        read(c, Button.LEFT, m.buttonDpadLeft);
        read(c, Button.RIGHT, m.buttonDpadRight);
        // SDL reports "up" as negative
        leftRaw.set(c.getAxis(m.axisLeftX), -c.getAxis(m.axisLeftY));
        stick(c, m.axisLeftX, m.axisLeftY, left);
        stick(c, m.axisRightX, m.axisRightY, right);

        boolean used = !left.isZero() || !right.isZero();
        for (boolean b : down) {
            used |= b;
        }
        if (used) {
            active = true;
        } else if (Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0 || Gdx.input.isTouched()) {
            active = false;
        }
    }

    private Controller current() {
        if (!available) {
            return null;
        }
        try {
            Controller c = Controllers.getCurrent();
            return c != null && c.isConnected() ? c : null;
        } catch (Throwable t) { // no native SDL for this platform: play with mouse + keyboard
            Gdx.app.error("Gamepad", "controllers unavailable", t);
            available = false;
            return null;
        }
    }

    private void read(Controller c, Button b, int index) {
        down[b.ordinal()] = index != ControllerMapping.UNDEFINED && c.getButton(index);
    }

    private static void stick(Controller c, int axisX, int axisY, Vector2 out) {
        out.set(c.getAxis(axisX), -c.getAxis(axisY));
        float len = out.len();
        if (len < DEADZONE) {
            out.setZero();
        } else { // rescale so movement starts at 0 just outside the dead zone
            out.scl(Math.min(1f, (len - DEADZONE) / (1f - DEADZONE)) / len);
        }
    }

    public boolean pressed(Button b) {
        return down[b.ordinal()];
    }

    public boolean justPressed(Button b) {
        return down[b.ordinal()] && !wasDown[b.ordinal()];
    }

    public boolean justReleased(Button b) {
        return !down[b.ordinal()] && wasDown[b.ordinal()];
    }

    /** Left stick, up is +y, length 0..1. */
    public Vector2 leftStick() {
        return left;
    }

    /** Left stick without the dead zone: a light push that doesn't walk can still aim a dodge roll. */
    public Vector2 leftStickRaw() {
        return leftRaw;
    }

    /** Right stick, up is +y, length 0..1. */
    public Vector2 rightStick() {
        return right;
    }

    /** True if the gamepad was used more recently than the mouse. */
    public boolean isActive() {
        return active;
    }
}
