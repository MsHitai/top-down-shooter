package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * Sea enemy: a starfish creeping over the sea bed, turning slowly. It spins up (glowing) and throws stars
 * off the tips of its five arms, three times, turning between each - a little spiral of stars (four on
 * harder floors). The sea's cheapest creature: they come in numbers.
 */
public class Starfish extends Enemy {

    private static final Color BODY = Color.valueOf("ff8a5a");
    private static final Color BODY_DARK = Color.valueOf("d8603a");
    private static final Color DOTS = Color.valueOf("ffd0a8");
    private static final Color STAR = Color.valueOf("ffb04a");

    private static final float WINDUP = 0.45f;
    private static final float SIZE = 0.2f;

    private float spin = MathUtils.random(72f);
    private float spinSpeed = 30f;
    private float fireTimer = MathUtils.random(1.5f, 2.5f);
    private float windup;
    private int volleys;
    private float volleyTimer;

    public Starfish(float x, float y) {
        super(x, y, 16f);
        radius = 0.17f;
        speed = 0.55f;
        score = 20;
    }

    @Override
    public float feetY() {
        return pos.y - 0.1f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        spin += spinSpeed * delta;
        if (windup > 0) { // spinning up
            windup -= delta;
            spinSpeed = 30f + 500f * (1f - windup / WINDUP);
            if (windup <= 0) {
                volleys = diff > 1.25f ? 4 : 3;
                volleyTimer = 0f;
            }
            return;
        }
        if (volleys > 0) {
            volleyTimer -= delta;
            if (volleyTimer <= 0) {
                volleyTimer = 0.22f;
                volleys--;
                for (int i = 0; i < 5; i++) { // one star off the tip of each arm
                    float a = spin + 90f + i * 72f;
                    Bullet b = world.spawnEnemyBullet(pos.x + MathUtils.cosDeg(a) * SIZE, pos.y + MathUtils.sinDeg(a) * SIZE,
                            a, 2.0f * diff);
                    b.shape = Bullet.Shape.STAR;
                    b.color.set(STAR);
                }
                spin += 24f;
                world.sfx().play("pop", 0.35f);
                if (volleys == 0) {
                    spinSpeed = 30f;
                    fireTimer = MathUtils.random(2.0f, 3.0f) / diff;
                }
            }
            return;
        }
        keepDistance(delta, world, 1.9f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            windup = WINDUP;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float rot = spin; // (the star texture points up at 0 degrees)
        if (windup > 0) {
            d.shape(b, d.star, pos.x, pos.y, SIZE * 1.5f, rot, STAR, 0.35f * (1f - windup / WINDUP));
        }
        d.shape(b, d.star, pos.x, pos.y, SIZE + 0.02f, rot, Draw.OUTLINE_COLOR, 1f);
        d.shape(b, d.star, pos.x, pos.y, SIZE, rot, c(BODY), 1f);
        d.shape(b, d.star, pos.x, pos.y - 0.01f, SIZE * 0.6f, rot, c(BODY_DARK), 0.6f);
        for (int i = 0; i < 5; i++) { // a row of bumps down each arm
            float a = rot + 90f + i * 72f;
            for (int k = 1; k <= 2; k++) {
                float dist = SIZE * 0.3f * k;
                d.circle(b, pos.x + MathUtils.cosDeg(a) * dist, pos.y + MathUtils.sinDeg(a) * dist, 0.015f, c(DOTS));
            }
        }
        drawEyes(b, d, pos.x, pos.y + 0.01f, 0.04f, 0.028f);
    }
}
