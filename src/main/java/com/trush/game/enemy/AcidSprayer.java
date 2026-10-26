package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.objects.Puddle;
import com.trush.game.world.GameWorld;

/**
 * The Slums: the Sludger's gang-mate with a hose. A goon in a green hazmat suit and a big tank on its
 * back; it plants its feet, a faint arc shows the span it's about to cover, then it hoses a stream of acid
 * drops sweeping across it - a moving wall to roll through. Now and then a drop splashes into a small pool.
 * Its suit keeps the acid off it.
 */
public class AcidSprayer extends Enemy {

    private static final Color SUIT = Color.valueOf("5aa83a");
    private static final Color SUIT_DARK = Color.valueOf("3a7a26");
    private static final Color MASK = Color.valueOf("3a3a44");
    private static final Color LENS = Color.valueOf("ffd84a");
    private static final Color TANK = Color.valueOf("c8c8d0");
    private static final Color TANK_DARK = Color.valueOf("8a8a96");
    private static final Color ACID = Color.valueOf("8ae83a");

    private static final float W = 0.38f;
    private static final float H = 0.54f;
    private static final float WINDUP = 0.65f;
    /** The sweep: this wide (degrees, centred on the player), this long, a drop every DROP_EVERY. */
    public static final float SWEEP = 100f;
    public static final float SWEEP_TIME = 1.2f;
    private static final float DROP_EVERY = 0.05f;
    private static final float DROP_SPEED = 3.2f;
    /** Every n-th drop splashes into a little pool where it lands. */
    private static final int POOL_EVERY = 6;

    private float fireTimer = MathUtils.random(1.5f, 2.5f);
    private float windup;
    private float spraying;
    private float sweepFrom;
    private float sweepDir;
    private float dropTimer;
    private int drops;
    /** Where the nozzle points right now (it sweeps while spraying). */
    private float nozzle;

    public AcidSprayer(float x, float y) {
        super(x, y, 30f);
        radius = 0.2f;
        speed = 0.8f;
        score = 40;
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
            nozzle = sweepFrom;
            if (windup <= 0) {
                spraying = SWEEP_TIME;
                dropTimer = 0;
            }
            return;
        }
        if (spraying > 0) { // planted, hosing the arc from one end to the other
            spraying -= delta;
            float k = 1f - Math.max(0f, spraying) / SWEEP_TIME;
            nozzle = sweepFrom + sweepDir * SWEEP * k;
            dropTimer -= delta;
            while (dropTimer <= 0 && spraying > 0) {
                dropTimer += DROP_EVERY;
                spray(world, diff);
            }
            if (spraying <= 0) {
                fireTimer = MathUtils.random(2.2f, 3f) / diff;
            }
            return;
        }
        nozzle = aimAngle;
        keepDistance(delta, world, 2.3f, speed);
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            windup = WINDUP;
            sweepDir = MathUtils.randomSign();
            sweepFrom = aimAngle - sweepDir * SWEEP / 2f;
        }
    }

    private void spray(GameWorld world, float diff) {
        Bullet b = shoot(world, nozzle + MathUtils.random(-3f, 3f), DROP_SPEED * Math.min(diff, 1.3f));
        b.shape = Bullet.Shape.DROP;
        b.color.set(ACID);
        b.radius = 0.055f;
        b.life = 1.05f;
        if (++drops % POOL_EVERY == 0) {
            b.puddle = Puddle.Kind.ACID;
            b.puddleRadius = 0.28f;
        }
        if (drops % 3 == 0) {
            world.sfx().play("pop", 0.25f);
        }
    }

    /** The hose's span shows on the floor as a faint arc while it winds up. */
    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (windup <= 0 || !alive) {
            return;
        }
        float k = 1f - windup / WINDUP;
        for (int i = 0; i <= 10; i++) {
            float a = sweepFrom + sweepDir * SWEEP * i / 10f;
            float r = 0.6f + 0.25f * k;
            d.circle(b, pos.x + MathUtils.cosDeg(a) * r, pos.y + MathUtils.sinDeg(a) * r, 0.025f, ACID, 0.25f + 0.4f * k);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float left = pos.x - W / 2f;
        float face = MathUtils.cosDeg(nozzle) >= 0 ? 1 : -1;
        drawFeet(b, d, pos.x, bottom, 0.09f);
        // the big tank on its back
        d.rect(b, pos.x - face * 0.22f - 0.08f, bottom + H * 0.2f, 0.16f, H * 0.6f, c(TANK));
        d.rect(b, pos.x - face * 0.22f - 0.08f, bottom + H * 0.2f, 0.04f, H * 0.6f, c(TANK_DARK));
        d.circle(b, pos.x - face * 0.22f, bottom + H * 0.8f, 0.08f, c(TANK));
        d.rect(b, pos.x - face * 0.22f - 0.05f, bottom + H * 0.45f, 0.1f, 0.05f, ACID); // the gauge
        // the suit and hood
        d.rect(b, left, bottom, W, H * 0.66f, c(SUIT));
        d.rect(b, left, bottom, 0.045f, H * 0.6f, c(SUIT_DARK));
        d.circle(b, pos.x, bottom + H * 0.68f, W / 2f, c(SUIT));
        // a gas mask with a single wide visor
        float my = bottom + H * 0.62f;
        d.rect(b, pos.x + face * 0.03f - 0.1f, my - 0.02f, 0.2f, 0.08f, c(MASK));
        d.rect(b, pos.x + face * 0.03f - 0.08f, my, 0.16f, 0.04f, LENS);
        d.circle(b, pos.x + face * 0.05f, my - 0.08f, 0.045f, c(MASK));
        // the hose from the tank to a long wand
        float gx = pos.x + face * 0.1f;
        float gy = bottom + H * 0.3f;
        d.line(b, pos.x - face * 0.2f, gy - 0.02f, 0.32f, 0.03f, face > 0 ? 0f : 180f, c(MASK));
        d.line(b, gx, gy, 0.3f, 0.045f, nozzle, c(TANK_DARK));
        float tipX = gx + MathUtils.cosDeg(nozzle) * 0.3f;
        float tipY = gy + MathUtils.sinDeg(nozzle) * 0.3f;
        if (windup > 0) { // it dribbles before it gushes
            float k = 1f - windup / WINDUP;
            d.circle(b, tipX, tipY - 0.03f - 0.05f * k, 0.025f + 0.02f * k, ACID, 0.9f);
        } else if (spraying > 0) {
            d.circle(b, tipX, tipY, 0.05f, ACID, 0.9f);
        }
    }
}
