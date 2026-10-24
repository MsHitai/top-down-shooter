package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * Clouds enemy: an angel with a sword. It hovers close, raises its glowing sword (the warning),
 * dashes at you and swings - a crescent of blade-shaped bullets fans out from the end of the dash.
 * On harder floors a second, slower crescent follows the first.
 */
public class SwordAngel extends Angel {

    private static final Color ROBE = Color.valueOf("f5f2ea");
    private static final Color ROBE_SHADE = Color.valueOf("cfc8b8");
    private static final Color BLADE = Color.valueOf("dfe8f5");
    private static final Color BLADE_GLOW = Color.valueOf("bfe6ff");
    private static final Color SLASH = Color.valueOf("8fd8ff");

    private enum State { HOVER, WINDUP, DASH, RECOVER }

    private static final float WINDUP = 0.6f;
    /** The dash direction stops following the player this long before it goes. */
    private static final float LOCK = 0.18f;
    private static final float DASH_TIME = 0.28f;
    private static final float DASH_SPEED = 5.2f;

    private State state = State.HOVER;
    private float timer = MathUtils.random(1.2f, 2f);
    private float dashAngle;
    private final Vector2 trail = new Vector2();
    /** 0..1 while the sword swings through after the slash. */
    private float swing;

    public SwordAngel(float x, float y) {
        super(x, y, 34f);
        radius = 0.2f;
        speed = 1.15f;
        score = 40;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        clock += delta;
        swing = Math.max(0f, swing - delta * 3f);
        float diff = world.getDifficulty();
        timer -= delta;
        switch (state) {
            case HOVER:
                keepDistance(delta, world, 1.7f, speed);
                if (timer <= 0 && canShoot(world)) {
                    state = State.WINDUP;
                    timer = WINDUP / Math.min(1.25f, diff);
                    world.sfx().play("spawn", 0.3f); // a holy shimmer as the sword lights up
                }
                break;
            case WINDUP:
                if (timer > LOCK) {
                    dashAngle = aimAngle;
                }
                if (timer <= 0) {
                    state = State.DASH;
                    timer = DASH_TIME;
                    trail.set(pos);
                    world.sfx().play("dodge", 0.6f);
                }
                break;
            case DASH:
                knock.setZero();
                pos.add(MathUtils.cosDeg(dashAngle) * DASH_SPEED * delta, MathUtils.sinDeg(dashAngle) * DASH_SPEED * delta);
                // stop short rather than run the player over: the slash is the attack
                if (timer <= 0 || distanceToPlayer(world) < 0.65f) {
                    slash(world, diff);
                }
                break;
            default: // RECOVER
                if (timer <= 0) {
                    state = State.HOVER;
                    timer = MathUtils.random(1.6f, 2.4f) / diff;
                }
                break;
        }
    }

    /** The swing: a crescent of blades toward the player (and on harder floors a second, slower one). */
    private void slash(GameWorld world, float diff) {
        state = State.RECOVER;
        timer = 0.7f;
        swing = 1f;
        int n = 7;
        for (int i = 0; i < n; i++) {
            float a = aimAngle - 50f + 100f * i / (n - 1);
            Bullet b = shoot(world, a, 2.7f * diff);
            b.shape = Bullet.Shape.DIAMOND;
            b.color.set(SLASH);
        }
        if (diff > 1.3f) {
            for (int i = 0; i < n - 1; i++) {
                Bullet b = shoot(world, aimAngle - 42f + 84f * i / (n - 2), 1.8f * diff);
                b.shape = Bullet.Shape.DIAMOND;
                b.color.set(SLASH);
            }
        }
        world.particles().burst(pos.x + MathUtils.cosDeg(aimAngle) * 0.3f, pos.y + MathUtils.sinDeg(aimAngle) * 0.3f,
                8, 0.3f, 1.4f, 0.3f, 0.035f, BLADE_GLOW);
        world.sfx().play("swish", 0.7f);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        if (state == State.DASH) { // a streak of light behind the dash
            float len = pos.dst(trail);
            d.line(b, trail.x, trail.y, len, 0.18f, dashAngle, BLADE_GLOW, 0.35f);
        }
        drawWings(b, d, state == State.DASH ? 1f : 0f);
        drawRobeAndHead(b, d, ROBE, ROBE_SHADE);
        // the sword: raised high while winding up, pointed along the dash, swung through after the slash
        float face = facingRight() ? 1f : -1f;
        float handX = pos.x + face * 0.15f;
        float handY = hem() + 0.22f;
        float angle;
        if (state == State.WINDUP) {
            angle = 90f + face * -25f;
        } else if (state == State.DASH) {
            angle = dashAngle;
        } else {
            angle = aimAngle - face * 40f * (1f - swing) + face * 70f * swing;
        }
        float glow = state == State.WINDUP ? 0.4f + 0.4f * MathUtils.sin(clock * 30f) : 0f;
        if (glow > 0) {
            d.line(b, handX, handY, 0.46f, 0.12f, angle, BLADE_GLOW, glow);
        }
        d.line(b, handX, handY, 0.44f, 0.065f, angle, Draw.OUTLINE_COLOR);
        d.line(b, handX, handY, 0.42f, 0.04f, angle, c(BLADE));
        d.rotRect(b, handX + MathUtils.cosDeg(angle) * 0.06f, handY + MathUtils.sinDeg(angle) * 0.06f,
                0.03f, 0.14f, angle, GOLD, 1f); // the crossguard
        d.circle(b, handX, handY, 0.035f, c(SKIN));
    }
}
