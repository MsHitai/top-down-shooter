package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * City enemy: a bullet on a motorcycle. It circles the player at speed (U-turning now and then) and
 * does drive-bys: five shots one after another, each aimed where the player is at that moment - since
 * the bike keeps moving, the stream sweeps across like a fan. Ramming into it hurts.
 */
public class Biker extends Enemy {

    private static final Color BIKE = Color.valueOf("c0392b");
    private static final Color BIKE_DARK = Color.valueOf("7a2018");
    private static final Color TIRE = Color.valueOf("1e1e22");
    private static final Color CHROME = Color.valueOf("c9ccd6");
    private static final Color BRASS = Color.valueOf("d9b24c");
    private static final Color HELMET = Color.valueOf("2a2a30");
    private static final Color VISOR = Color.valueOf("8ec5e8");
    private static final Color BULLET = Color.valueOf("ff8a3a");

    private float orbitAngle = MathUtils.random(360f);
    private float orbitRadius = MathUtils.random(2.0f, 2.8f);
    private float orbitDir = MathUtils.randomSign();
    private float turnTimer = MathUtils.random(3f, 6f);
    private float fireTimer = MathUtils.random(1.2f, 2.0f);
    private int burstLeft;
    private float burstTimer;
    private final Vector2 target = new Vector2();
    private final Vector2 step = new Vector2();
    private final Vector2 lastPos = new Vector2();
    private float heading;

    public Biker(float x, float y) {
        super(x, y, 26f);
        radius = 0.22f;
        speed = 2.6f;
        score = 35;
        lastPos.set(x, y);
    }

    @Override
    public float feetY() {
        return pos.y - 0.2f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        Player p = world.getPlayer();
        float diff = world.getDifficulty();
        // stuck against something (no progress last frame), or now and then: skid round the other way
        boolean stuck = pos.dst(lastPos) < speed * delta * 0.25f;
        turnTimer -= delta;
        if (turnTimer <= 0 || stuck && MathUtils.randomBoolean(0.1f)) {
            orbitDir = -orbitDir;
            turnTimer = MathUtils.random(3f, 6f);
            orbitRadius = MathUtils.random(2.0f, 2.8f);
            world.particles().burst(pos.x, feetY(), 6, 0.2f, 0.8f, 0.3f, 0.04f, TIRE);
        }
        lastPos.set(pos);
        // ride around the player
        orbitAngle += orbitDir * (speed / orbitRadius) * MathUtils.radiansToDegrees * delta;
        target.set(p.centerX() + MathUtils.cosDeg(orbitAngle) * orbitRadius,
                p.hitboxY() + MathUtils.sinDeg(orbitAngle) * orbitRadius);
        Arena.now.clamp(target, radius + 0.2f);
        step.set(target).sub(pos);
        if (step.len() > 0.02f) {
            heading = step.angleDeg();
            step.nor().scl(speed * delta);
            pos.add(step);
            walkTime += delta;
        }
        if (MathUtils.randomBoolean(0.3f)) { // exhaust
            world.particles().spawn(pos.x - MathUtils.cosDeg(heading) * 0.25f, feetY() + 0.08f, 0, 0.2f, 0.3f, 0.03f,
                    Color.GRAY, 1f);
        }

        // drive-by: a quick string of shots, each aimed at the player right now
        if (burstLeft > 0) {
            burstTimer -= delta;
            if (burstTimer <= 0) {
                burstTimer = 0.08f;
                burstLeft--;
                shoot(world, aimAngle, 3.0f * diff).color.set(BULLET);
                world.sfx().play("smg", 0.4f);
            }
            return;
        }
        fireTimer -= delta;
        if (fireTimer <= 0 && canShoot(world)) {
            burstLeft = 5;
            burstTimer = 0.15f;
            fireTimer = MathUtils.random(1.3f, 2.0f) / diff;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float face = MathUtils.cosDeg(heading) >= 0 ? 1 : -1;
        float bounce = MathUtils.sin(walkTime * 25f) * 0.008f;
        float bottom = feetY();
        // wheels
        for (int i = -1; i <= 1; i += 2) {
            d.circle(b, pos.x + i * 0.17f, bottom + 0.07f, 0.075f, TIRE);
            d.circle(b, pos.x + i * 0.17f, bottom + 0.07f, 0.03f, CHROME);
        }
        // frame, tank and exhaust pipe
        d.rect(b, pos.x - 0.18f, bottom + 0.1f + bounce, 0.36f, 0.08f, c(BIKE));
        d.rect(b, pos.x - 0.05f + face * 0.03f, bottom + 0.16f + bounce, 0.14f, 0.07f, c(BIKE_DARK));
        d.rect(b, pos.x - face * 0.22f - 0.04f, bottom + 0.09f, 0.1f, 0.03f, CHROME);
        d.rect(b, pos.x + face * 0.15f - 0.015f, bottom + 0.16f + bounce, 0.03f, 0.1f, CHROME); // handlebar
        // rider: a bullet in a helmet
        float ry = bottom + 0.22f + bounce;
        d.rect(b, pos.x - 0.08f, ry, 0.16f, 0.16f, c(BRASS));
        d.circle(b, pos.x, ry + 0.2f, 0.1f, c(HELMET));
        d.rect(b, pos.x + face * 0.02f - 0.03f + face * 0.02f, ry + 0.17f, 0.08f, 0.04f, VISOR);
        // gun hand pointing at the player
        d.line(b, pos.x + face * 0.06f, ry + 0.08f, 0.16f, 0.05f, aimAngle, GUN);
        if (burstLeft > 0) {
            d.circle(b, pos.x + face * 0.06f + MathUtils.cosDeg(aimAngle) * 0.17f,
                    ry + 0.08f + MathUtils.sinDeg(aimAngle) * 0.17f, 0.04f, TELEGRAPH, 0.8f);
        }
    }
}
