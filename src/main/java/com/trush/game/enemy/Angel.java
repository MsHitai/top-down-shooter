package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;

/**
 * The clouds' guards (floor 5): winged, haloed and floating a little off the floor, so they fly over
 * furniture and holes in the clouds. The {@link SwordAngel} dashes in and slashes; the {@link BowAngel}
 * keeps its distance with a bow. This draws what they share: wings, robe, face and halo.
 */
public abstract class Angel extends Enemy {

    protected static final Color WING = Color.valueOf("fbfaff");
    protected static final Color WING_SHADE = Color.valueOf("c8d0ea");
    protected static final Color SKIN = Color.valueOf("f2c9a0");
    protected static final Color HAIR = Color.valueOf("f2d36a");
    protected static final Color HALO = Color.valueOf("ffe066");
    protected static final Color GOLD = Color.valueOf("e8c040");

    /** How far its feet float above the floor (the shadow stays down there). */
    private static final float HOVER = 0.16f;

    protected float clock = MathUtils.random(10f);

    protected Angel(float x, float y, float hp) {
        super(x, y, hp);
        flying = true;
    }

    /** The shadow on the floor, under its hovering feet. */
    @Override
    public float feetY() {
        return pos.y - 0.42f;
    }

    /** Bobs gently up and down as it floats. */
    protected float bob() {
        return MathUtils.sin(clock * 3f) * 0.025f;
    }

    /** Where the body is drawn from: the hem of the robe, floating above the shadow. */
    protected float hem() {
        return feetY() + HOVER + bob();
    }

    /** Two white wings behind the body, flapping (faster when {@code beat} is high). */
    protected void drawWings(SpriteBatch b, Draw d, float beat) {
        float y = hem() + 0.3f;
        float flap = MathUtils.sin(clock * (6f + beat * 10f)) * 12f;
        for (int side = -1; side <= 1; side += 2) {
            float angle = 90f + side * (-55f + flap);
            float cx = pos.x + side * 0.2f;
            d.rotRect(b, cx + side * 0.02f, y - 0.02f, 0.4f, 0.21f, angle - 90f, Draw.ACTOR_EDGE, 1f);
            d.rotRect(b, cx, y, 0.37f, 0.18f, angle - 90f, c(WING_SHADE), 1f);
            d.rotRect(b, cx - side * 0.01f, y + 0.03f, 0.31f, 0.11f, angle - 90f, c(WING), 1f);
            for (int feather = 0; feather < 4; feather++) {
                float fy = y - 0.05f + feather * 0.025f;
                d.line(b, cx, fy, 0.16f + feather * 0.025f, 0.025f,
                        side > 0 ? angle - 90f : angle + 90f, c(WING), 0.9f);
            }
        }
    }

    /** A long robe from the hem up to the shoulders, a belt of gold, the head with golden hair and a halo. */
    protected void drawRobeAndHead(SpriteBatch b, Draw d, Color robe, Color robeShade) {
        d.cast.draw(b, this instanceof BowAngel ? "AngelBow" : "Angel", pos.x, hem(), walkTime, facingRight(), false, flash > 0, c(Color.WHITE));
    }
}
