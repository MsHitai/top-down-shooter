package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * A Bulletmancer in ember robes. Instead of rings it plants its feet and pours out a spiral: three arms
 * (four on harder floors) of fire turning around it for a while, the other way round every time. Walk
 * round with the spiral, or slip between two arms.
 */
public class Spiralmancer extends Bulletmancer {

    private static final Color ROBE = Color.valueOf("c25a24");
    private static final Color ROBE_DARK = Color.valueOf("7e3216");
    private static final Color HAT = Color.valueOf("8e1f22");
    private static final Color ORB = Color.valueOf("ffb347");
    private static final Color BULLET = Color.valueOf("ff8a3d");

    /** How long a spiral pours out, how fast it turns (degrees a second) and how often each arm fires. */
    static final float STREAM_TIME = 1.8f;
    static final float TURN = 150f;
    static final float SHOT_GAP = 0.12f;
    static final int ARMS = 3;

    private float stream;
    private float shotTimer;
    private float spin;
    private float turnWay = 1f;

    public Spiralmancer(float x, float y) {
        super(x, y, ROBE, ROBE_DARK, HAT, ORB);
    }

    @Override
    protected boolean channel(float delta, GameWorld world) {
        if (stream <= 0) {
            return false;
        }
        float diff = world.getDifficulty();
        stream -= delta;
        spin += TURN * turnWay * delta;
        shotTimer -= delta;
        if (shotTimer <= 0) {
            shotTimer += SHOT_GAP;
            int arms = diff > 1.2f ? ARMS + 1 : ARMS;
            for (int i = 0; i < arms; i++) {
                Bullet b = shoot(world, spin + 360f * i / arms, 1.6f * diff);
                b.accel = 0.6f * diff;
                b.maxSpeed = 2.6f * diff;
                b.color.set(BULLET);
                b.shape = Bullet.Shape.FLARE;
            }
            world.sfx().play("enemy_shoot", 0.25f);
        }
        if (stream <= 0) {
            turnWay = -turnWay; // the next spiral turns the other way
        }
        return true;
    }

    @Override
    protected void cast(GameWorld world) {
        stream = STREAM_TIME;
        shotTimer = 0f;
        spin = aimAngle + 180f / ARMS; // never an arm right at you to start with
        world.sfx().play("enemy_shoot", 0.7f);
    }

    @Override
    protected float castPause(GameWorld world) {
        return MathUtils.random(3.2f, 4.0f) / world.getDifficulty();
    }

    @Override
    protected float channelGlow() {
        return stream > 0 ? 1f : 0f;
    }
}
