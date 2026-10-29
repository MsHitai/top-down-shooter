package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.objects.GasCloud;
import com.trush.game.world.GameWorld;

/**
 * The Shell Cop in riot gear, from floor 4 on in its place: a helmet with a visor and a big see-through
 * shield held towards you - shots from the front only do {@link #SHIELD_DAMAGE} of their damage, so get round
 * it, or catch it while it lowers the shield to fire its burst. Now and then it lobs a tear-gas canister at
 * where you're standing (a ring shows where it lands), filling the spot with stinging gas.
 */
public class RiotCop extends Enemy {

    private static final Color UNIFORM = Color.valueOf("26303e");
    private static final Color UNIFORM_DARK = Color.valueOf("1a212c");
    private static final Color BRASS = Color.valueOf("d9b24c");
    private static final Color HELMET = Color.valueOf("2c3a52");
    private static final Color VISOR = Color.valueOf("9fd4ff");
    private static final Color SHIELD = Color.valueOf("bfe3ff");
    private static final Color SHIELD_RIM = Color.valueOf("5f7a99");
    private static final Color CANISTER = Color.valueOf("9aa83a");
    private static final Color BULLET = Color.valueOf("ffb84d");

    private static final float W = 0.36f;
    private static final float H = 0.54f;
    private static final float WINDUP = 0.45f;
    static final int BURST = 4;
    private static final float BURST_GAP = 0.11f;
    private static final float BULLET_SPEED = 3.3f;
    /** What a hit from the front does through the shield. */
    static final float SHIELD_DAMAGE = 0.2f;
    /** The shield covers this far either side of where it's facing. */
    static final float SHIELD_ARC = 65f;
    private static final float CANISTER_SPEED = 2.4f;

    private float fireTimer = MathUtils.random(1.4f, 2.4f);
    private float canTimer = MathUtils.random(2.5f, 4f);
    private float windup;
    private int burstLeft;
    private float burstTimer;
    private Bullet canister;
    private final Vector2 landing = new Vector2();

    public RiotCop(float x, float y) {
        super(x, y, 26f);
        radius = 0.21f;
        speed = 0.8f;
        score = 35;
    }

    @Override
    public float feetY() {
        return pos.y - H / 2f;
    }

    /** The shield's up unless it's aiming or firing its burst. */
    boolean shieldUp() {
        return windup <= 0 && burstLeft <= 0;
    }

    @Override
    public void hit(float damage, Vector2 bulletVel, float knockback, GameWorld world) {
        if (alive && shieldUp() && !bulletVel.isZero()) {
            float from = MathUtils.atan2Deg360(-bulletVel.y, -bulletVel.x); // where the shot came from
            float off = Math.abs(MathUtils.atan2Deg(MathUtils.sinDeg(from - aimAngle), MathUtils.cosDeg(from - aimAngle)));
            if (off < SHIELD_ARC) {
                damage *= SHIELD_DAMAGE;
                knockback *= 0.3f;
                if (world != null) {
                    float sx = pos.x + MathUtils.cosDeg(aimAngle) * 0.26f;
                    float sy = pos.y + MathUtils.sinDeg(aimAngle) * 0.26f;
                    world.particles().burst(sx, sy, 4, 0.1f, 1.2f, 0.2f, 0.03f, SHIELD);
                }
            }
        }
        super.hit(damage, bulletVel, knockback, world);
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        updateCanister(world);
        if (burstLeft > 0) {
            burstTimer -= delta;
            if (burstTimer <= 0) {
                burstTimer = BURST_GAP;
                burstLeft--;
                shoot(world, leadAngle(world, BULLET_SPEED * diff, 0.8f), BULLET_SPEED * diff).color.set(BULLET);
                world.sfx().play("enemy_shoot", 0.5f);
                if (burstLeft == 0) {
                    fireTimer = MathUtils.random(1.8f, 2.6f) / diff;
                }
            }
            return;
        }
        if (windup > 0) { // shield lowered, aiming
            windup -= delta;
            if (windup <= 0) {
                burstLeft = BURST;
                burstTimer = 0;
            }
            return;
        }
        keepDistance(delta, world, 2.2f, speed);
        fireTimer -= delta;
        canTimer -= delta;
        if (canTimer <= 0 && canister == null && canShoot(world)) {
            lobCanister(world);
            canTimer = MathUtils.random(4.5f, 6f) / diff;
        } else if (fireTimer <= 0 && canShoot(world)) {
            windup = WINDUP;
        }
    }

    private void lobCanister(GameWorld world) {
        landing.set(world.getPlayer().centerX(), world.getPlayer().hitboxY());
        float a = MathUtils.atan2Deg360(landing.y - pos.y, landing.x - pos.x);
        Bullet b = shoot(world, a, CANISTER_SPEED);
        b.shape = Bullet.Shape.BOMB;
        b.color.set(CANISTER);
        b.radius = 0.08f;
        b.life = MathUtils.clamp(Vector2.dst(pos.x, pos.y, landing.x, landing.y) / CANISTER_SPEED, 0.4f, 2f);
        canister = b;
        world.sfx().play("pop", 0.5f);
    }

    /** The canister's down (or hit something): the gas billows out where it stopped. */
    private void updateCanister(GameWorld world) {
        if (canister == null || canister.alive) {
            return;
        }
        Bullet b = canister;
        canister = null;
        if (world.getBlankRing() >= 0) {
            return; // a blank just makes it vanish
        }
        for (int i = 0; i < 3; i++) {
            float a = 120f * i + MathUtils.random(-20f, 20f);
            world.releaseGas(GasCloud.Kind.STINK, b.prev.x, b.prev.y, MathUtils.cosDeg(a) * 0.5f, MathUtils.sinDeg(a) * 0.5f,
                    0.38f, 3.5f);
        }
        world.sfx().play("hiss", 0.6f);
    }

    /** Where the canister is going to land. */
    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (canister != null && canister.alive && alive) {
            d.ring(b, landing.x, landing.y, 0.45f, TELEGRAPH, 0.7f);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float bottom = feetY();
        float left = pos.x - W / 2f;
        float face = facingRight() ? 1 : -1;
        boolean behind = MathUtils.sinDeg(aimAngle) > 0.3f; // aiming up the screen: the shield's on the far side
        if (behind) drawShield(b, d, bottom);
        drawFeet(b, d, pos.x, bottom, 0.085f);
        d.circle(b, pos.x, bottom + H * 0.64f, W / 2f, c(BRASS));
        d.rect(b, left, bottom, W, H * 0.64f, c(UNIFORM));
        d.rect(b, left, bottom, 0.04f, H * 0.6f, c(UNIFORM_DARK));
        // the riot helmet and its visor
        d.circle(b, pos.x, bottom + H * 0.74f, W / 2f + 0.02f, c(HELMET));
        d.rect(b, left - 0.02f, bottom + H * 0.52f, W + 0.04f, 0.1f, c(HELMET));
        d.rect(b, left + 0.02f, bottom + H * 0.54f, W - 0.04f, 0.09f, VISOR, 0.55f);
        drawEyes(b, d, pos.x, bottom + H * 0.6f, 0.065f, 0.04f);

        float gx = pos.x + face * 0.12f;
        float gy = bottom + H * 0.25f;
        d.line(b, gx, gy, 0.2f, 0.06f, aimAngle, GUN);
        if (windup > 0) {
            float mx = gx + MathUtils.cosDeg(aimAngle) * 0.2f;
            float my = gy + MathUtils.sinDeg(aimAngle) * 0.2f;
            d.circle(b, mx, my, 0.05f + 0.03f * (1 - windup / WINDUP), TELEGRAPH, 0.9f);
        }
        if (!behind) drawShield(b, d, bottom);
    }

    /** A tall see-through shield, held out towards the hero - or tucked aside while it fires. */
    private void drawShield(SpriteBatch b, Draw d, float bottom) {
        boolean up = shieldUp();
        float a = up ? aimAngle : aimAngle + (facingRight() ? 70f : -70f);
        float sx = pos.x + MathUtils.cosDeg(a) * 0.24f;
        float sy = bottom + H * 0.4f + MathUtils.sinDeg(a) * 0.1f;
        float w = 0.1f + 0.14f * Math.abs(MathUtils.sinDeg(a)); // seen edge-on when held sideways
        d.rect(b, sx - w / 2f - 0.015f, sy - 0.25f, w + 0.03f, 0.5f, c(SHIELD_RIM));
        d.rect(b, sx - w / 2f, sy - 0.235f, w, 0.47f, SHIELD, up ? 0.6f : 0.4f);
    }
}
