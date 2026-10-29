package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * A robed caster that hangs back and summons slow rings of bullets that speed up.
 * This is the "bullet-hell" pressure enemy: rings force you to find the gaps or dodge roll.
 */
public class Bulletmancer extends Enemy {

    private static final Color ROBE = Color.valueOf("3f4fb5");
    private static final Color ROBE_DARK = Color.valueOf("2a347a");
    private static final Color HAT = Color.valueOf("6c3fb5");
    private static final Color ORB = Color.valueOf("b388ff");
    private static final Color BULLET = Color.valueOf("c77dff");

    /** Its colours: the Spiralmancer and the Bloomancer are the same caster in other robes. */
    private final Color robe;
    private final Color robeDark;
    private final Color hat;
    protected final Color orb;

    private static final float W = 0.38f;
    private static final float H = 0.6f;
    private static final float CHARGE = 0.8f;

    private float castTimer = MathUtils.random(1.5f, 2.5f);
    private float charge;
    private int castCount;
    private float time;

    public Bulletmancer(float x, float y) {
        this(x, y, ROBE, ROBE_DARK, HAT, ORB);
    }

    protected Bulletmancer(float x, float y, Color robe, Color robeDark, Color hat, Color orb) {
        super(x, y, 32f);
        radius = 0.21f;
        speed = 0.5f;
        score = 40;
        this.robe = robe;
        this.robeDark = robeDark;
        this.hat = hat;
        this.orb = orb;
    }

    /**
     * A spell that takes a while (the Spiralmancer's stream, the Bloomancer's seeds): called every frame before
     * anything else; true while it's busy and should stand still.
     */
    protected boolean channel(float delta, GameWorld world) {
        return false;
    }

    /** How long between spells. */
    protected float castPause(GameWorld world) {
        return MathUtils.random(2.8f, 3.6f) / world.getDifficulty();
    }

    @Override
    public float feetY() {
        return pos.y - H / 2f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        if (channel(delta, world)) {
            return;
        }
        if (charge > 0) {
            charge -= delta;
            if (charge <= 0) {
                cast(world);
            }
            return;
        }
        keepDistance(delta, world, 2.8f, speed);
        castTimer -= delta;
        if (castTimer <= 0 && canShoot(world)) {
            charge = CHARGE;
            castTimer = castPause(world);
        }
    }

    protected void cast(GameWorld world) {
        castCount++;
        int n = world.getDifficulty() > 1.2f ? 16 : 13;
        float offset = castCount % 2 == 0 ? 0 : 180f / n + aimAngle;
        boolean curving = castCount % 3 == 0;
        for (int i = 0; i < n; i++) {
            Bullet b = shoot(world, offset + 360f * i / n, 0.9f);
            b.accel = 1.4f * world.getDifficulty();
            b.maxSpeed = 3.2f * world.getDifficulty();
            b.color.set(BULLET);
            if (curving) {
                b.curve = 35f;
            }
        }
        world.sfx().play("enemy_shoot", 0.8f);
    }

    /** How bright its orb burns while it channels (0-1). */
    protected float channelGlow() {
        return 0f;
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float hover = MathUtils.sin(time * 3f) * 0.02f;
        float left = pos.x - W / 2f;
        // robe: stacked rectangles narrowing upwards
        d.rect(b, left, bottom + hover, W, H * 0.35f, c(robeDark));
        d.rect(b, left + 0.02f, bottom + H * 0.3f + hover, W - 0.04f, H * 0.3f, c(robe));
        d.circle(b, pos.x, bottom + H * 0.66f + hover, W * 0.38f, c(robe));
        // hat
        d.rect(b, pos.x - W * 0.45f, bottom + H * 0.78f + hover, W * 0.9f, 0.04f, c(hat));
        d.rect(b, pos.x - W * 0.25f, bottom + H * 0.82f + hover, W * 0.5f, 0.08f, c(hat));
        d.rect(b, pos.x - W * 0.12f, bottom + H * 0.9f + hover, W * 0.24f, 0.07f, c(hat));
        drawEyes(b, d, pos.x, bottom + H * 0.66f + hover, 0.055f, 0.034f);

        float orbX = pos.x + (facingRight() ? 0.24f : -0.24f);
        float orbY = bottom + H * 0.45f + hover;
        float glow = Math.max(charge > 0 ? 1f - charge / CHARGE : 0f, channelGlow());
        d.circle(b, orbX, orbY, 0.07f + 0.12f * glow, orb, 0.25f + 0.4f * glow);
        d.circle(b, orbX, orbY, 0.045f, c(orb));
        d.circle(b, orbX, orbY, 0.02f, Color.WHITE);
    }
}
