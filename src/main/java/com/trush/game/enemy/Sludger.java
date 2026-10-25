package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.objects.Puddle;
import com.trush.game.world.GameWorld;

/**
 * An acid gunner (after ETG's poison-goop enemies): a goon in a yellow hazmat suit and a gas mask with a
 * tank of acid on its back. Keeps its distance and fires slow globs at where the player stands; each one
 * splats into a pool of acid - so the floor fills up and the player has to keep moving (or roll across).
 * Its suit keeps the acid off it. Rare on the regular floors; meant to be at home in a bad district
 * of its own one day (see ROADMAP).
 */
public class Sludger extends Enemy {

    private static final Color SUIT = Color.valueOf("e8c43a");
    private static final Color SUIT_DARK = Color.valueOf("b8962a");
    private static final Color MASK = Color.valueOf("3a3a44");
    private static final Color LENS = Color.valueOf("9ae8ff");
    private static final Color TANK = Color.valueOf("4a9a2a");
    private static final Color GLOB = Color.valueOf("8ae83a");

    private static final float W = 0.38f;
    private static final float H = 0.54f;
    private static final float WINDUP = 0.55f;
    private static final float GLOB_SPEED = 3f;
    public static final float POOL_RADIUS = 0.5f;

    private float fireTimer = MathUtils.random(1.5f, 2.5f);
    private float windup;
    private int volleys;

    public Sludger(float x, float y) {
        super(x, y, 24f);
        radius = 0.2f;
        speed = 0.85f;
        score = 35;
        acidProof = true;
    }

    @Override
    public float feetY() {
        return pos.y - H / 2f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        if (windup > 0) {
            windup -= delta;
            if (windup <= 0) {
                volleys++;
                // harder floors: every third volley is a spread of three
                boolean spread = diff >= 1.25f && volleys % 3 == 0;
                for (int i = spread ? -1 : 0; i <= (spread ? 1 : 0); i++) {
                    lob(world, aimAngle + i * 20f, diff);
                }
                world.sfx().play("pop", 0.7f);
                fireTimer = MathUtils.random(2f, 2.8f) / diff;
            }
            return; // stands still to pump the gun
        }
        keepDistance(delta, world, 2.6f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            windup = WINDUP;
        }
    }

    /** A glob that flies to about where the player stands and splats into a pool there. */
    private void lob(GameWorld world, float angle, float diff) {
        float speed = GLOB_SPEED * Math.min(diff, 1.3f);
        Bullet b = shoot(world, angle, speed);
        b.shape = Bullet.Shape.GLOB;
        b.color.set(GLOB);
        b.radius = 0.08f;
        b.life = MathUtils.clamp(distanceToPlayer(world) / speed, 0.4f, 2.2f);
        b.puddle = Puddle.Kind.ACID;
        b.puddleRadius = POOL_RADIUS;
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float left = pos.x - W / 2f;
        float face = facingRight() ? 1 : -1;
        drawFeet(b, d, pos.x, bottom, 0.09f);
        // the acid tank on its back, peeking out behind
        d.rect(b, pos.x - face * 0.2f - 0.06f, bottom + H * 0.25f, 0.12f, H * 0.5f, c(TANK));
        d.rect(b, pos.x - face * 0.2f - 0.03f, bottom + H * 0.7f, 0.06f, 0.05f, c(MASK));
        // the baggy suit and its hood
        d.rect(b, left, bottom, W, H * 0.66f, c(SUIT));
        d.rect(b, left, bottom, 0.045f, H * 0.6f, c(SUIT_DARK));
        d.circle(b, pos.x, bottom + H * 0.68f, W / 2f, c(SUIT));
        // the gas mask: two round lenses and a filter
        float my = bottom + H * 0.62f;
        d.circle(b, pos.x + face * 0.03f, my, 0.12f, c(MASK));
        d.circle(b, pos.x + face * 0.03f - 0.05f, my + 0.03f, 0.04f, LENS);
        d.circle(b, pos.x + face * 0.03f + 0.05f, my + 0.03f, 0.04f, LENS);
        d.circle(b, pos.x + face * 0.06f, my - 0.07f, 0.045f, c(SUIT_DARK));
        // the squirter, with a hose back to the tank
        float gx = pos.x + face * 0.13f;
        float gy = bottom + H * 0.28f;
        d.line(b, gx, gy, 0.22f, 0.07f, aimAngle, c(TANK));
        d.line(b, pos.x - face * 0.15f, gy, 0.28f, 0.025f, face > 0 ? 0f : 180f, c(MASK));
        if (windup > 0) {
            float k = 1f - windup / WINDUP;
            float mx = gx + MathUtils.cosDeg(aimAngle) * 0.22f;
            float my2 = gy + MathUtils.sinDeg(aimAngle) * 0.22f;
            d.circle(b, mx, my2, 0.04f + 0.05f * k, GLOB, 0.9f);
        }
    }
}
