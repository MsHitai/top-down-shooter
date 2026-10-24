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
            d.rotRect(b, cx + side * 0.02f, y - 0.02f, 0.4f, 0.21f, angle - 90f, Draw.OUTLINE_COLOR, 1f);
            d.rotRect(b, cx, y, 0.37f, 0.18f, angle - 90f, c(WING_SHADE), 1f);
            d.rotRect(b, cx - side * 0.01f, y + 0.03f, 0.31f, 0.11f, angle - 90f, c(WING), 1f);
        }
    }

    /** A long robe from the hem up to the shoulders, a belt of gold, the head with golden hair and a halo. */
    protected void drawRobeAndHead(SpriteBatch b, Draw d, Color robe, Color robeShade) {
        float hem = hem();
        d.rect(b, pos.x - 0.15f, hem - 0.015f, 0.3f, 0.34f, Draw.OUTLINE_COLOR);
        d.rect(b, pos.x - 0.135f, hem, 0.27f, 0.31f, c(robe));
        d.rect(b, pos.x - 0.135f, hem, 0.06f, 0.31f, c(robeShade));
        d.rect(b, pos.x - 0.135f, hem + 0.16f, 0.27f, 0.035f, c(GOLD));
        float headY = hem + 0.42f;
        d.circle(b, pos.x, headY, 0.125f, Draw.OUTLINE_COLOR);
        d.circle(b, pos.x, headY, 0.11f, c(SKIN));
        d.circle(b, pos.x, headY + 0.05f, 0.095f, c(HAIR));
        drawEyes(b, d, pos.x, headY - 0.015f, 0.045f, 0.028f);
        float haloY = headY + 0.16f + MathUtils.sin(clock * 2f) * 0.012f;
        d.ellipse(b, pos.x, haloY, 0.11f, 0.035f, HALO, 0.95f);
        d.ellipse(b, pos.x, haloY, 0.07f, 0.018f, Draw.OUTLINE_COLOR, 0.6f);
    }
}
