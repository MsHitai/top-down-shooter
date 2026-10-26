package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * Moon enemy (the owner's idea): an astronaut in a puffy white suit and a gold-visored helmet, bouncing
 * along in the low gravity. Keeps its distance and fires a laser rifle in bursts of three; every few
 * seconds it fires its jetpack and boosts sideways (flames and all) to a new spot.
 */
public class Astronaut extends Enemy {

    private static final Color SUIT = Color.valueOf("eef0f6");
    private static final Color SUIT_SHADE = Color.valueOf("b8bcca");
    private static final Color VISOR = Color.valueOf("e8b440");
    private static final Color VISOR_SHINE = Color.valueOf("fff0b0");
    private static final Color PACK = Color.valueOf("8a8e9a");
    private static final Color STRIPE = Color.valueOf("4a7ae0");
    private static final Color LASER = Color.valueOf("6ae0ff");
    private static final Color FLAME = Color.valueOf("ffb43a");

    private static final float BOOST_TIME = 0.35f;
    private static final float BOOST_SPEED = 3.2f;

    private float fireTimer = MathUtils.random(1.2f, 2f);
    private int burst;
    private float burstTimer;
    private float boostTimer = MathUtils.random(2.5f, 4f);
    private float boost;
    private final Vector2 boostDir = new Vector2();

    public Astronaut(float x, float y) {
        super(x, y, 22f);
        radius = 0.17f;
        speed = 0.75f;
        score = 25;
    }

    @Override
    public float feetY() {
        return pos.y - 0.16f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        if (boost > 0) { // whoosh: sideways on the jetpack
            boost -= delta;
            pos.mulAdd(boostDir, BOOST_SPEED * delta);
            return;
        }
        boostTimer -= delta;
        if (boostTimer <= 0 && burst == 0) {
            boostTimer = MathUtils.random(2.8f, 4.2f);
            boost = BOOST_TIME;
            float side = aimAngle + 90f * MathUtils.randomSign();
            boostDir.set(MathUtils.cosDeg(side), MathUtils.sinDeg(side));
            world.sfx().play("dodge", 0.4f);
            return;
        }
        if (burst > 0) {
            burstTimer -= delta;
            if (burstTimer <= 0) {
                burstTimer = 0.12f;
                burst--;
                Bullet b = shoot(world, aimAngle + MathUtils.random(-4f, 4f), 3.4f * diff);
                b.shape = Bullet.Shape.DIAMOND;
                b.color.set(LASER);
                world.sfx().play("zap", 0.3f);
            }
            return;
        }
        keepDistance(delta, world, 2.4f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            burst = 3;
            burstTimer = 0.25f; // (a short aim first)
            fireTimer = MathUtils.random(1.8f, 2.6f) / diff;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float face = facingRight() ? 1f : -1f;
        float hopY = Math.abs(MathUtils.sin(walkTime * 5f)) * 0.06f; // floaty moon steps
        float x = pos.x;
        float y = pos.y + hopY;
        // jetpack on its back, flames while it boosts
        d.rect(b, x - face * 0.15f - 0.05f, y - 0.08f, 0.1f, 0.18f, c(PACK));
        if (boost > 0) {
            float fl = 0.06f + 0.04f * MathUtils.sin(walkTime * 50f + boost * 60f);
            d.ellipse(b, x - face * 0.15f, y - 0.13f, 0.04f, fl, FLAME, 0.9f);
        }
        drawFeet(b, d, x, pos.y - 0.13f + hopY, 0.06f);
        // the puffy suit
        d.circle(b, x, y - 0.04f, 0.15f, Draw.OUTLINE_COLOR);
        d.circle(b, x, y - 0.04f, 0.135f, c(SUIT_SHADE));
        d.circle(b, x - 0.015f, y - 0.025f, 0.115f, c(SUIT));
        d.rect(b, x - 0.1f, y - 0.06f, 0.2f, 0.025f, c(STRIPE));
        // the laser rifle
        d.line(b, x + face * 0.04f, y - 0.04f, 0.2f, 0.045f, aimAngle, GUN);
        d.circle(b, x + MathUtils.cosDeg(aimAngle) * 0.24f + face * 0.04f, y - 0.04f + MathUtils.sinDeg(aimAngle) * 0.24f,
                0.02f, LASER, burst > 0 ? 1f : 0.5f);
        // the helmet, gold visor facing the way it aims
        d.circle(b, x, y + 0.12f, 0.12f, Draw.OUTLINE_COLOR);
        d.circle(b, x, y + 0.12f, 0.105f, c(SUIT));
        d.ellipse(b, x + face * 0.025f, y + 0.12f, 0.075f, 0.06f, c(VISOR), 1f);
        d.ellipse(b, x + face * 0.005f, y + 0.145f, 0.025f, 0.015f, VISOR_SHINE, 0.9f);
    }
}
