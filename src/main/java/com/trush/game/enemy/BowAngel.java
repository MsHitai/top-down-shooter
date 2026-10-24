package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * Clouds enemy: an angel with a bow. It keeps well back and draws its bow - a faint golden line shows
 * where it aims, then holds still a moment before the arrow flies. Every few shots it looses a spread of
 * three instead, or shoots up into the sky: golden rings mark where the arrows will come down, so step out.
 */
public class BowAngel extends Angel {

    private static final Color ROBE = Color.valueOf("bfe0f5");
    private static final Color ROBE_SHADE = Color.valueOf("8ab8d8");
    private static final Color BOW = Color.valueOf("c8902a");
    private static final Color ARROW = Color.valueOf("ffe066");
    private static final Color AIM = Color.valueOf("ffe9a0");
    private static final Color MARK = Color.valueOf("ffd166");

    private static final float DRAW_TIME = 0.8f;
    private static final float LOCK = 0.25f;
    /** Arrows shot into the sky land this long after their rings appear. */
    private static final float RAIN_TIME = 1.0f;
    private static final float RAIN_RADIUS = 0.3f;

    private enum Shot { AIMED, VOLLEY, RAIN }

    private float fireTimer = MathUtils.random(1.4f, 2.2f);
    private float drawTimer;
    private float aim;
    private float aimLength;
    private Shot shot = Shot.AIMED;
    private int shotsFired;
    private final Vector2[] rainAt = {new Vector2(), new Vector2(), new Vector2(), new Vector2()};
    private final float[] rainTimer = new float[4];

    public BowAngel(float x, float y) {
        super(x, y, 26f);
        radius = 0.19f;
        speed = 0.9f;
        score = 40;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        clock += delta;
        float diff = world.getDifficulty();
        updateRain(delta, world);
        if (drawTimer > 0) {
            drawTimer -= delta;
            if (drawTimer > LOCK) {
                aim = shot == Shot.RAIN ? 90f : aimAngle;
            }
            aimLength = shot == Shot.RAIN ? 0f : world.rayLength(pos.x, pos.y, aim, 9f);
            if (drawTimer <= 0) {
                loose(world, diff);
            }
            return; // holds still while drawing the bow
        }
        keepDistance(delta, world, 3.2f, speed);
        aim = aimAngle;
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            // two plain shots, then something special: a spread of three or a rain of arrows
            shot = shotsFired % 3 < 2 ? Shot.AIMED : MathUtils.randomBoolean() ? Shot.VOLLEY : Shot.RAIN;
            drawTimer = DRAW_TIME;
        }
    }

    private void loose(GameWorld world, float diff) {
        shotsFired++;
        fireTimer = MathUtils.random(1.6f, 2.3f) / diff;
        world.sfx().play("twang", 0.6f);
        switch (shot) {
            case VOLLEY:
                for (int i = -1; i <= 1; i++) {
                    arrow(world, aim + i * 16f, 4.2f * diff);
                }
                break;
            case RAIN: { // up it goes; it comes down around the player
                Player p = world.getPlayer();
                for (int i = 0; i < rainAt.length; i++) {
                    float a = MathUtils.random(360f);
                    float r = i == 0 ? 0f : MathUtils.random(0.6f, 1.1f); // one right where you stand
                    rainAt[i].set(p.centerX() + MathUtils.cosDeg(a) * r, p.hitboxY() + MathUtils.sinDeg(a) * r);
                    Arena.now.clamp(rainAt[i], 0.2f);
                    rainTimer[i] = RAIN_TIME + i * 0.12f;
                }
                world.particles().burst(pos.x, pos.y + 0.3f, 6, 0.2f, 1f, 0.3f, 0.03f, ARROW);
                break;
            }
            default:
                arrow(world, aim, 6.0f * diff);
                break;
        }
    }

    private void arrow(GameWorld world, float angle, float arrowSpeed) {
        Bullet b = shoot(world, angle, arrowSpeed);
        b.color.set(ARROW);
        b.streak = 0.26f;
    }

    /** The arrows shot into the sky: each lands on its ring, hurting whoever is still standing in it. */
    private void updateRain(float delta, GameWorld world) {
        Player p = world.getPlayer();
        for (int i = 0; i < rainAt.length; i++) {
            if (rainTimer[i] <= 0) continue;
            rainTimer[i] -= delta;
            if (rainTimer[i] <= 0) {
                if (Vector2.dst(rainAt[i].x, rainAt[i].y, p.centerX(), p.hitboxY()) < RAIN_RADIUS + Player.HITBOX_RADIUS) {
                    world.hurtPlayer();
                }
                world.particles().burst(rainAt[i].x, rainAt[i].y, 5, 0.2f, 0.9f, 0.25f, 0.03f, ARROW);
                world.sfx().play("hit", 0.25f);
            }
        }
    }

    /** The rings where the sky arrows will land, filling up, and each arrow as it drops. */
    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        for (int i = 0; i < rainAt.length; i++) {
            if (rainTimer[i] <= 0 || !alive) continue;
            float k = MathUtils.clamp(1f - rainTimer[i] / RAIN_TIME, 0f, 1f);
            d.ring(b, rainAt[i].x, rainAt[i].y, RAIN_RADIUS, MARK, 0.8f);
            d.circle(b, rainAt[i].x, rainAt[i].y, RAIN_RADIUS * k, MARK, 0.35f);
            if (rainTimer[i] < 0.3f) { // the arrow, falling point first
                float y = rainAt[i].y + rainTimer[i] / 0.3f * 1.6f;
                d.line(b, rainAt[i].x, y, 0.3f, 0.035f, 90f, ARROW);
                d.rotRect(b, rainAt[i].x, y, 0.07f, 0.07f, 45f, Color.WHITE, 1f);
            }
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        if (drawTimer > 0 && shot != Shot.RAIN && (drawTimer > LOCK || ((int) (drawTimer * 20f)) % 2 == 0)) {
            d.line(b, pos.x, pos.y, aimLength, drawTimer > LOCK ? 0.012f : 0.024f, aim, AIM, 0.7f);
        }
        drawWings(b, d, 0f);
        drawRobeAndHead(b, d, ROBE, ROBE_SHADE);
        // the bow: a curve of wood held out toward the aim, the string pulled back while drawing
        float handX = pos.x + MathUtils.cosDeg(aim) * 0.2f;
        float handY = hem() + 0.24f + MathUtils.sinDeg(aim) * 0.12f;
        float pull = drawTimer > 0 ? Math.min(1f, (DRAW_TIME - drawTimer) / 0.3f) * 0.1f : 0f;
        float nx = -MathUtils.sinDeg(aim);
        float ny = MathUtils.cosDeg(aim);
        for (int i = -3; i <= 3; i++) {
            float bend = (9 - i * i) * 0.006f;
            d.circle(b, handX + nx * i * 0.05f + MathUtils.cosDeg(aim) * bend,
                    handY + ny * i * 0.05f + MathUtils.sinDeg(aim) * bend, 0.026f, c(BOW));
        }
        float backX = handX - MathUtils.cosDeg(aim) * pull;
        float backY = handY - MathUtils.sinDeg(aim) * pull;
        for (int end = -1; end <= 1; end += 2) { // the string, from each tip to the pulled-back middle
            float tx = handX + nx * end * 0.15f;
            float ty = handY + ny * end * 0.15f;
            float len = Vector2.dst(tx, ty, backX, backY);
            d.line(b, tx, ty, len, 0.012f, MathUtils.atan2Deg360(backY - ty, backX - tx), Color.WHITE);
        }
        if (drawTimer > 0) { // the nocked arrow
            d.line(b, backX, backY, 0.3f, 0.025f, aim, ARROW);
        }
    }
}
