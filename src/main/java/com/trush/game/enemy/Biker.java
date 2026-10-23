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

    /** Drawn this much bigger than his first, toy-ish look. */
    private static final float S = 1.25f;

    private static final Color BIKE = Color.valueOf("2a2a30");
    private static final Color BIKE_DARK = Color.valueOf("16161a");
    private static final Color FLAME = Color.valueOf("ff5a2a");
    private static final Color JACKET = Color.valueOf("1e1a1c");
    private static final Color HEADLIGHT = Color.valueOf("fff3b0");
    private static final Color TIRE = Color.valueOf("1e1e22");
    private static final Color CHROME = Color.valueOf("c9ccd6");
    private static final Color BRASS = Color.valueOf("d9b24c");
    private static final Color HELMET = Color.valueOf("2a2a30");
    private static final Color VISOR = Color.valueOf("3a4a6a");
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
        radius = 0.26f;
        speed = 2.6f;
        score = 35;
        lastPos.set(x, y);
    }

    @Override
    public float feetY() {
        return pos.y - 0.24f;
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
        float lean = orbitDir * face * 6f; // leans into the turn
        float bottom = feetY();
        float cx = pos.x;
        // chunky wheels
        for (int i = -1; i <= 1; i += 2) {
            d.circle(b, cx + i * 0.21f * S, bottom + 0.09f * S, 0.1f * S, TIRE);
            d.circle(b, cx + i * 0.21f * S, bottom + 0.09f * S, 0.04f * S, CHROME);
        }
        // black frame and a tank with a flame on it
        d.rotRect(b, cx, bottom + 0.15f * S + bounce, 0.46f * S, 0.1f * S, lean, c(BIKE), 1f);
        d.rotRect(b, cx + face * 0.04f * S, bottom + 0.23f * S + bounce, 0.18f * S, 0.09f * S, lean, c(BIKE_DARK), 1f);
        d.rotRect(b, cx + face * 0.05f * S, bottom + 0.23f * S + bounce, 0.1f * S, 0.04f * S, lean, c(FLAME), 1f);
        // twin chrome exhausts, sputtering sparks
        d.rect(b, cx - face * 0.28f * S - 0.06f * S, bottom + 0.11f * S, 0.14f * S, 0.035f * S, CHROME);
        d.rect(b, cx - face * 0.28f * S - 0.06f * S, bottom + 0.07f * S, 0.12f * S, 0.03f * S, CHROME);
        if (MathUtils.randomBoolean(0.25f)) {
            d.circle(b, cx - face * 0.36f * S, bottom + 0.1f * S, 0.025f * S, FLAME);
        }
        // handlebars and headlight
        d.rect(b, cx + face * 0.19f * S - 0.015f, bottom + 0.2f * S + bounce, 0.03f, 0.14f * S, CHROME);
        d.circle(b, cx + face * 0.25f * S, bottom + 0.21f * S + bounce, 0.04f * S, HEADLIGHT);
        // rider: a bullet in a black leather jacket and a dark helmet with a mean visor
        float ry = bottom + 0.28f * S + bounce;
        d.rect(b, cx - 0.1f * S, ry, 0.2f * S, 0.2f * S, c(BRASS));
        d.rect(b, cx - 0.11f * S, ry, 0.22f * S, 0.13f * S, c(JACKET));
        d.rect(b, cx + face * 0.04f * S - 0.01f, ry + 0.02f * S, 0.02f, 0.1f * S, CHROME); // zip
        d.circle(b, cx, ry + 0.26f * S, 0.12f * S, c(HELMET));
        d.rotRect(b, cx + face * 0.05f * S, ry + 0.25f * S, 0.15f * S, 0.05f * S, face * -8f, VISOR, 1f);
        d.rotRect(b, cx + face * 0.03f * S, ry + 0.33f * S, 0.12f * S, 0.025f * S, face * 12f, c(FLAME), 1f);
        // gun hand pointing at the player
        d.line(b, cx + face * 0.08f * S, ry + 0.1f * S, 0.2f * S, 0.055f * S, aimAngle, GUN);
        if (burstLeft > 0) {
            d.circle(b, cx + face * 0.08f * S + MathUtils.cosDeg(aimAngle) * 0.21f * S,
                    ry + 0.1f * S + MathUtils.sinDeg(aimAngle) * 0.21f * S, 0.045f * S, TELEGRAPH, 0.8f);
        }
    }
}
