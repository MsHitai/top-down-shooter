package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;

/**
 * A single projectile, fired either by the player or by an enemy.
 * {@code accel} changes speed over time (units/s²) and {@code curve} bends the path (deg/s),
 * which is what most bullet-hell patterns are built from.
 */
public class Bullet {

    private static final Color OUTLINE = Color.valueOf("2a0a14");
    private static final Color WING = Color.valueOf("5e667a");
    private static final Color NECK = Color.valueOf("4fa07a");
    private static final Color BEAK = Color.valueOf("e0a040");
    private static final Color BELLY = Color.valueOf("eef4ff");
    private static final Color MOUTH = Color.valueOf("8a1f3a");
    private static final Color SNOW_EDGE = Color.valueOf("3a78b0");
    private static final Color GLOB_GREEN = Color.valueOf("8ae83a");

    public final boolean fromPlayer;
    public final Vector2 pos = new Vector2();
    public final Vector2 vel = new Vector2();
    /** Where it was last frame: hits are checked along prev -> pos, so fast bullets can't skip enemies. */
    public final Vector2 prev = new Vector2();
    /** Keep {@link #prev} on the next update (a fresh player bullet sweeps from the gun hand). */
    private boolean keepPrev;
    public final Color color = new Color();
    public float radius;
    /** Hitbox radius = radius x this; boss bullets look bigger than they hit (fair to dodge). */
    public float hitScale = 0.8f;
    public float damage;
    public float life;
    public float accel;
    public float curve;
    public float minSpeed = 0.4f;
    /** Snakes left and right by up to this many deg/s (bubbles, bees). */
    public float wobble;
    /** Player bullets: turns toward the nearest enemy by up to this many deg/s (bees); see GameWorld. */
    public float homing;
    /** Player boomerang: flies out and slows down, then comes back to the player (see GameWorld). */
    public boolean boomerang;
    /** A boomerang on its way back. */
    public boolean returning;
    /** Drawn as a see-through bubble instead of a solid ball. */
    public boolean bubble;
    /** Bounces off walls and cover this many more times instead of stopping (rubber balls). */
    public int bounces;
    /** Blows up when it stops (hits something or runs out): blast radius and damage (rockets). */
    public float blastRadius;
    public float blastDamage;
    /** Only blows up when it hits something; run out of range and it just fizzles (the Boom-buss). */
    public boolean impactOnly;
    /** Drawn as a rocket with a smoke trail. */
    public boolean rocket;
    /** Drawn as a streak this long along its path (laser bolts). */
    public float streak;
    /** How a plain bullet is drawn (the special looks above - pigeon, rocket, streak, bubble - win). */
    public enum Shape { BALL, DROP, CLOUD, SQUARE, TRIANGLE, DIAMOND, STAR, PILLOW, FEATHER, DUST, GLOB, FLARE, SHARK, BOMB, BARREL, SNOWFLAKE, NOTE }
    public Shape shape = Shape.BALL;
    /** Drawn as a flapping pigeon flying the way it moves (the Pigeon King's flocks); body in {@link #color}. */
    public boolean pigeon;
    /** Player bullets: bursts into this many feathers wherever it stops (the Pillow Cannon). */
    public int splits;
    /** What it bursts into (feathers; the Popcorn Popper's puffs) and how hard each piece hits, x its damage. */
    public Shape splitShape = Shape.FEATHER;
    public float splitDamage = 0.35f;
    /** Hot air: shoves the smoke and stink clouds it passes through away (the Hair Dryer). */
    public boolean blowsGas;
    /** Sticks in what it hits (Sticky Darts; GameWorld pops a full set). */
    public boolean sticky;
    /** Waits this long at the gun before it flies (and can hit anything) - the later peas of a burst. */
    public float delay;
    /** Player bullets: slows whatever it hits down for this many seconds (the Snowball Slinger). */
    public float chill;
    /** Player bullets: water (or snow) - it puts fires out (the burning drum hiding the way to the Slums). */
    public boolean wet;
    /** Player bullets: the spark jumps on to this many more enemies from the one it hits (Jumper Cables). */
    public int chain;
    /** Player bullets: pulls regular enemies within this radius in and chews on them (the Dust Bunny). */
    public float vortex;
    /** Counts down between the Dust Bunny's bites. */
    public float tick;
    /** Player bullets: sets what it hits on fire / eats at it with acid for this many seconds. */
    public float burn;
    public float acid;
    /** Player bullets: freezes what it hits solid for this many seconds (Frost Rounds). */
    public float freeze;
    /**
     * Frost it adds to whoever it hits (1 = frozen solid): an enemy's snowflake doesn't hurt the hero, it
     * only chills; a player's snowball builds up ice on an enemy.
     */
    public float frost;
    /** The Chomper's shark: how wide its jaws are open (0-1), wider the closer it gets to its prey. */
    public float gape;
    /** Leaves a puddle of this kind and size wherever it stops (a flare's fire, an acid glob); null = none. */
    public Puddle.Kind puddle;
    public float puddleRadius;
    private float age;
    private final float phase = MathUtils.random(MathUtils.PI2);
    public float maxSpeed = 12f;
    public float knockback = 0.6f;
    public boolean piercing;
    public boolean alive = true;
    /** Enemies a piercing bullet already went through, so it only hits each once. */
    public final Array<Object> alreadyHit = new Array<>(false, 4);

    public Bullet(boolean fromPlayer, float x, float y, float angleDeg, float speed, float radius, float damage, Color color) {
        this.fromPlayer = fromPlayer;
        pos.set(x, y);
        prev.set(x, y);
        vel.set(MathUtils.cosDeg(angleDeg), MathUtils.sinDeg(angleDeg)).scl(speed);
        this.radius = radius;
        this.damage = damage;
        this.color.set(color);
        this.life = 6f;
    }

    /** The first update sweeps from (x, y) - e.g. the gun hand - instead of from where it was spawned. */
    public void sweepFrom(float x, float y) {
        prev.set(x, y);
        keepPrev = true;
    }

    public void update(float delta) {
        if (delay > 0) {
            delay -= delta;
            prev.set(pos);
            return;
        }
        if (keepPrev) {
            keepPrev = false;
        } else {
            prev.set(pos);
        }
        age += delta;
        if (wobble != 0) {
            vel.rotateDeg(MathUtils.sin(age * 9f + phase) * wobble * delta);
        }
        if (curve != 0) {
            vel.rotateDeg(curve * delta);
        }
        if (accel != 0) {
            float speed = MathUtils.clamp(vel.len() + accel * delta, minSpeed, maxSpeed);
            vel.setLength(speed);
        }
        pos.mulAdd(vel, delta);
        life -= delta;
        if (life <= 0) {
            alive = false;
        }
    }

    public void draw(SpriteBatch b, Draw d) {
        if (delay > 0) {
            return; // still in the pod
        }
        if (pigeon) {
            drawPigeon(b, d);
        } else if (rocket) {
            float angle = vel.angleDeg();
            float ux = MathUtils.cosDeg(angle);
            float uy = MathUtils.sinDeg(angle);
            d.rotRect(b, pos.x - ux * radius * 1.6f, pos.y - uy * radius * 1.6f, radius * 1.2f, radius * 2.4f, angle, OUTLINE, 1f);
            d.rotRect(b, pos.x, pos.y, radius * 3.2f, radius * 1.3f, angle, color, 1f);
            d.circle(b, pos.x + ux * radius * 1.6f, pos.y + uy * radius * 1.6f, radius * 0.65f, Color.WHITE);
            d.circle(b, pos.x - ux * radius * 2.2f, pos.y - uy * radius * 2.2f, radius * 0.7f, Color.ORANGE, 0.9f);
        } else if (streak > 0) {
            float angle = vel.angleDeg();
            float ux = MathUtils.cosDeg(angle) * streak / 2f;
            float uy = MathUtils.sinDeg(angle) * streak / 2f;
            d.rotRect(b, pos.x - ux, pos.y - uy, streak, radius * 3.2f, angle, color, 0.3f);
            d.rotRect(b, pos.x - ux, pos.y - uy, streak, radius * 1.6f, angle, color, 1f);
            d.rotRect(b, pos.x - ux, pos.y - uy, streak * 0.8f, radius * 0.7f, angle, Color.WHITE, 1f);
        } else if (bubble) {
            d.circle(b, pos.x, pos.y, radius, color, 0.25f);
            d.ring(b, pos.x, pos.y, radius, color, 0.9f);
            d.circle(b, pos.x - radius * 0.35f, pos.y + radius * 0.35f, radius * 0.25f, Color.WHITE, 0.9f);
        } else if (shape != Shape.BALL) {
            drawShape(b, d);
        } else if (fromPlayer) {
            d.circle(b, pos.x, pos.y, radius * 1.9f, color, 0.25f);
            d.circle(b, pos.x, pos.y, radius, color);
            d.circle(b, pos.x, pos.y, radius * 0.5f, Color.WHITE);
        } else {
            d.circle(b, pos.x, pos.y, radius * 1.3f, OUTLINE);
            d.circle(b, pos.x, pos.y, radius, color);
            d.circle(b, pos.x - radius * 0.25f, pos.y + radius * 0.25f, radius * 0.4f, Color.WHITE, 0.85f);
        }
    }

    /** -1 or 1: which way a bullet heading at {@code angle} trails a drip behind it. */
    private static float angleSide(float angle) {
        return MathUtils.cosDeg(angle) >= 0 ? 1f : -1f;
    }

    /** The shaped bullets: water drops, fluffy clouds, and spinning squares, triangles, diamonds and stars. */
    private void drawShape(SpriteBatch b, Draw d) {
        float r = radius;
        float angle = vel.angleDeg();
        float spin = age * 240f + phase * MathUtils.radiansToDegrees;
        switch (shape) {
            case DROP: { // round at the front, pointed at the back, like a falling drop
                float ux = MathUtils.cosDeg(angle);
                float uy = MathUtils.sinDeg(angle);
                float tx = pos.x - ux * r * 0.6f;
                float ty = pos.y - uy * r * 0.6f;
                d.rotRect(b, tx, ty, r * 1.45f, r * 1.45f, angle + 45f, OUTLINE, 0.8f);
                d.circle(b, pos.x, pos.y, r * 1.2f, OUTLINE, 0.8f);
                d.rotRect(b, tx, ty, r * 1.15f, r * 1.15f, angle + 45f, color, 1f);
                d.circle(b, pos.x, pos.y, r, color);
                d.circle(b, pos.x + ux * r * 0.2f - uy * r * 0.35f, pos.y + uy * r * 0.2f + ux * r * 0.35f, r * 0.3f,
                        Color.WHITE, 0.9f);
                break;
            }
            case CLOUD: { // a little puff of cloud: white on top, tinted underneath, bobbing as it drifts
                float bob = MathUtils.sin(age * 8f + phase) * r * 0.08f;
                float y = pos.y + bob;
                d.circle(b, pos.x, y, r * 1.35f, color, 0.18f);
                d.circle(b, pos.x - r * 0.62f, y - r * 0.12f, r * 0.62f, color);
                d.circle(b, pos.x + r * 0.62f, y - r * 0.12f, r * 0.62f, color);
                d.circle(b, pos.x, y - r * 0.2f, r * 0.7f, color);
                d.circle(b, pos.x - r * 0.55f, y + r * 0.02f, r * 0.55f, Color.WHITE);
                d.circle(b, pos.x + r * 0.55f, y + r * 0.02f, r * 0.55f, Color.WHITE);
                d.circle(b, pos.x - r * 0.1f, y + r * 0.3f, r * 0.68f, Color.WHITE);
                break;
            }
            case SQUARE:
                d.rotRect(b, pos.x, pos.y, r * 2.1f, r * 2.1f, spin, OUTLINE, 1f);
                d.rotRect(b, pos.x, pos.y, r * 1.6f, r * 1.6f, spin, color, 1f);
                d.rotRect(b, pos.x, pos.y, r * 0.6f, r * 0.6f, spin, Color.WHITE, 0.8f);
                break;
            case DIAMOND:
                d.rotRect(b, pos.x, pos.y, r * 1.85f, r * 1.85f, angle + 45f, OUTLINE, 1f); // a corner leads
                d.rotRect(b, pos.x, pos.y, r * 1.45f, r * 1.45f, angle + 45f, color, 1f);
                d.rotRect(b, pos.x, pos.y, r * 0.55f, r * 0.55f, angle + 45f, Color.WHITE, 0.8f);
                break;
            case TRIANGLE: // points the way it flies
                d.shape(b, d.triangle, pos.x, pos.y, r * 1.55f, angle, OUTLINE, 1f);
                d.shape(b, d.triangle, pos.x, pos.y, r * 1.2f, angle, color, 1f);
                d.shape(b, d.triangle, pos.x, pos.y, r * 0.5f, angle, Color.WHITE, 0.8f);
                break;
            case PILLOW: { // a plump pillow tumbling through the air, its corners poking out
                d.rotRect(b, pos.x, pos.y, r * 2.5f, r * 1.9f, spin * 0.5f, OUTLINE, 1f);
                d.rotRect(b, pos.x, pos.y, r * 2.2f, r * 1.6f, spin * 0.5f, color, 1f);
                d.rotRect(b, pos.x, pos.y, r * 1.4f, r * 0.9f, spin * 0.5f, Color.WHITE, 0.9f);
                break;
            }
            case FEATHER: { // a long thin feather with its quill down the middle, see-sawing as it flies
                float tilt = angle + MathUtils.sin(age * 20f + phase) * 25f;
                d.rotRect(b, pos.x, pos.y, r * 4f, r * 1.7f, tilt, OUTLINE, 0.8f);
                d.rotRect(b, pos.x, pos.y, r * 3.6f, r * 1.3f, tilt, color, 1f);
                d.rotRect(b, pos.x, pos.y, r * 3.8f, r * 0.3f, tilt, Draw.OUTLINE_COLOR, 0.5f);
                break;
            }
            case DUST: { // a dust bunny: a spinning grey fluffball with two little eyes
                for (int i = 0; i < 7; i++) {
                    float a = spin + i * 360f / 7f;
                    d.circle(b, pos.x + MathUtils.cosDeg(a) * r * 0.7f, pos.y + MathUtils.sinDeg(a) * r * 0.7f,
                            r * 0.5f, color, 0.85f);
                }
                d.circle(b, pos.x, pos.y, r * 0.8f, color);
                d.circle(b, pos.x - r * 0.25f, pos.y + r * 0.1f, r * 0.14f, Color.WHITE);
                d.circle(b, pos.x + r * 0.25f, pos.y + r * 0.1f, r * 0.14f, Color.WHITE);
                d.circle(b, pos.x - r * 0.25f, pos.y + r * 0.1f, r * 0.07f, OUTLINE);
                d.circle(b, pos.x + r * 0.25f, pos.y + r * 0.1f, r * 0.07f, OUTLINE);
                break;
            }
            case GLOB: { // a wobbling blob of acid with a bright fleck
                float wob = MathUtils.sin(age * 14f + phase) * 0.15f;
                d.ellipse(b, pos.x, pos.y, r * (1.25f + wob), r * (1.25f - wob), OUTLINE, 0.9f);
                d.ellipse(b, pos.x, pos.y, r * (1.05f + wob), r * (1.05f - wob), color, 1f);
                d.circle(b, pos.x - r * 0.3f, pos.y + r * 0.3f, r * 0.3f, Color.WHITE, 0.8f);
                d.circle(b, pos.x - angleSide(angle) * r * 1.4f, pos.y - r * 0.2f, r * 0.35f, color, 0.6f);
                break;
            }
            case FLARE: { // a burning flare: a white-hot head and sparks trailing behind
                float ux = MathUtils.cosDeg(angle);
                float uy = MathUtils.sinDeg(angle);
                for (int i = 1; i <= 3; i++) {
                    d.circle(b, pos.x - ux * r * 1.3f * i, pos.y - uy * r * 1.3f * i, r * (1f - i * 0.22f), color,
                            0.7f - i * 0.15f);
                }
                d.circle(b, pos.x, pos.y, r * 1.8f, color, 0.3f);
                d.circle(b, pos.x, pos.y, r, color);
                d.circle(b, pos.x, pos.y, r * 0.55f, Color.WHITE);
                break;
            }
            case SHARK: { // a little fluffy shark swimming the way it flies: wagging tail, fin, open jaws
                float ux = MathUtils.cosDeg(angle);
                float uy = MathUtils.sinDeg(angle);
                float wag = MathUtils.sin(age * 18f + phase) * 30f;
                float tx = pos.x - ux * r * 1.3f;
                float ty = pos.y - uy * r * 1.3f;
                d.shape(b, d.triangle, tx - ux * r * 0.3f, ty - uy * r * 0.3f, r * 0.85f, angle + 180f + wag, OUTLINE, 1f);
                d.shape(b, d.triangle, tx - ux * r * 0.3f, ty - uy * r * 0.3f, r * 0.65f, angle + 180f + wag, color, 1f);
                d.circle(b, pos.x, pos.y, r * 1.12f, OUTLINE);
                d.circle(b, pos.x - ux * r * 0.55f, pos.y - uy * r * 0.55f, r * 0.92f, OUTLINE);
                d.circle(b, pos.x, pos.y, r * 0.95f, color);
                d.circle(b, pos.x - ux * r * 0.55f, pos.y - uy * r * 0.55f, r * 0.75f, color);
                d.circle(b, pos.x + uy * r * 0.3f, pos.y - ux * r * 0.3f, r * 0.55f, BELLY, 0.9f);
                d.shape(b, d.triangle, pos.x - uy * r * 0.75f, pos.y + ux * r * 0.75f, r * 0.5f, angle + 90f, color, 1f);
                float jaw = MathUtils.lerp(0.25f + 0.2f * Math.abs(MathUtils.sin(age * 10f + phase)), 0.75f, gape);
                float mx = pos.x + ux * r * (0.75f + 0.2f * gape);
                float my = pos.y + uy * r * (0.75f + 0.2f * gape);
                d.circle(b, mx, my, r * jaw * 1.4f, MOUTH);
                if (gape > 0.3f) { // jaws wide open: rows of teeth, top and bottom
                    for (int side = -1; side <= 1; side += 2) {
                        for (int t = -1; t <= 1; t++) {
                            float tx2 = mx + ux * r * jaw * 0.5f * t - uy * side * r * jaw * 1.1f;
                            float ty2 = my + uy * r * jaw * 0.5f * t + ux * side * r * jaw * 1.1f;
                            d.shape(b, d.triangle, tx2, ty2, r * 0.22f, angle - side * 90f, Color.WHITE, gape);
                        }
                    }
                } else {
                    d.circle(b, mx, my, r * jaw * 0.6f, Color.WHITE, 0.9f);
                }
                d.circle(b, pos.x + ux * r * 0.25f - uy * r * 0.35f, pos.y + uy * r * 0.25f + ux * r * 0.35f, r * 0.18f, OUTLINE);
                break;
            }
            case SNOWFLAKE: { // six spinning arms with little branches: it chills, it doesn't hurt
                float ar = r * 1.5f;
                d.circle(b, pos.x, pos.y, r * 1.3f, color, 0.25f);
                for (int k = 0; k < 3; k++) {
                    d.rotRect(b, pos.x, pos.y, ar * 2f + 0.025f, r * 0.42f + 0.02f, spin + k * 60f, SNOW_EDGE, 1f);
                }
                for (int k = 0; k < 3; k++) {
                    d.rotRect(b, pos.x, pos.y, ar * 2f, r * 0.42f, spin + k * 60f, Color.WHITE, 1f);
                }
                for (int k = 0; k < 6; k++) { // a little V near the tip of each arm
                    float a = spin + k * 60f;
                    float bx = pos.x + MathUtils.cosDeg(a) * ar * 0.6f;
                    float by = pos.y + MathUtils.sinDeg(a) * ar * 0.6f;
                    d.line(b, bx, by, ar * 0.4f, r * 0.25f, a + 45f, Color.WHITE, 1f);
                    d.line(b, bx, by, ar * 0.4f, r * 0.25f, a - 45f, Color.WHITE, 1f);
                }
                d.circle(b, pos.x, pos.y, r * 0.45f, color, 1f);
                break;
            }
            case BOMB: { // a round little bomb with a sparking fuse
                float sx = pos.x + r * 0.6f;
                float sy = pos.y + r * 0.8f;
                d.circle(b, pos.x, pos.y, r * 1.25f, OUTLINE);
                d.circle(b, pos.x, pos.y, r, color);
                d.circle(b, pos.x - r * 0.35f, pos.y + r * 0.35f, r * 0.3f, Color.WHITE, 0.8f);
                d.circle(b, sx, sy, r * (0.35f + 0.15f * MathUtils.sin(age * 40f + phase)), Color.GOLD, 0.9f);
                break;
            }
            case BARREL: { // a rusty drum on its side, rolling along (the Barrel Roller's): its hoops roll past
                float along = r * 1.8f; // the drum's round side, the way it rolls
                float across = r * 2.4f; // its length, side to side
                float ux = MathUtils.cosDeg(angle);
                float uy = MathUtils.sinDeg(angle);
                d.ellipse(b, pos.x, pos.y - r * 0.9f, across * 0.5f, r * 0.3f, OUTLINE, 0.35f); // its shadow
                d.rotRect(b, pos.x, pos.y, along + 0.03f, across + 0.03f, angle, OUTLINE, 1f);
                d.rotRect(b, pos.x, pos.y, along, across, angle, color, 1f);
                float roll = age * vel.len() / (MathUtils.PI * along);
                for (int i = 0; i < 2; i++) { // two hoops, sliding over the round side as it turns
                    float k = ((roll + i * 0.5f) % 1f) - 0.5f;
                    d.rotRect(b, pos.x + ux * k * along, pos.y + uy * k * along, r * 0.16f, across, angle, OUTLINE, 0.75f);
                }
                d.circle(b, pos.x - uy * across * 0.5f, pos.y + ux * across * 0.5f, r * 0.45f, OUTLINE, 0.8f); // the ends
                d.circle(b, pos.x + uy * across * 0.5f, pos.y - ux * across * 0.5f, r * 0.45f, OUTLINE, 0.8f);
                d.circle(b, pos.x - uy * across * 0.3f, pos.y + ux * across * 0.3f, r * 0.2f, GLOB_GREEN, 0.9f); // a leak
                break;
            }
            case NOTE: { // a music note bobbing along: a round head, a stem and a flag
                float bob = MathUtils.sin(age * 10f + phase) * r * 0.3f;
                float hx = pos.x - r * 0.3f;
                float hy = pos.y - r * 0.4f + bob;
                d.ellipse(b, hx, hy, r * 0.85f, r * 0.65f, OUTLINE, 1f);
                d.ellipse(b, hx, hy, r * 0.65f, r * 0.48f, color, 1f);
                d.rect(b, hx + r * 0.42f, hy, r * 0.32f, r * 1.9f, OUTLINE, 1f); // the stem
                d.rotRect(b, hx + r * 0.95f, hy + r * 1.55f, r * 1.1f, r * 0.42f, -35f, OUTLINE, 1f); // the flag
                d.circle(b, hx - r * 0.2f, hy + r * 0.15f, r * 0.18f, Color.WHITE, 0.8f);
                break;
            }
            case STAR:
                d.shape(b, d.star, pos.x, pos.y, r * 1.6f, spin, OUTLINE, 1f);
                d.shape(b, d.star, pos.x, pos.y, r * 1.3f, spin, color, 1f);
                d.circle(b, pos.x, pos.y, r * 0.3f, Color.WHITE, 0.8f);
                break;
            default:
                break;
        }
    }

    /** A little pigeon seen from above: flapping wings, tail, a shiny green neck and an orange beak. */
    private void drawPigeon(SpriteBatch b, Draw d) {
        float r = radius;
        float angle = vel.angleDeg();
        float ux = MathUtils.cosDeg(angle);
        float uy = MathUtils.sinDeg(angle);
        float flap = 0.35f + 0.65f * Math.abs(MathUtils.sin(age * 14f + phase));
        d.circle(b, pos.x, pos.y, r * 1.2f, OUTLINE, 0.6f);
        for (int side = -1; side <= 1; side += 2) { // wings stick out sideways
            float off = side * r * (0.55f + 0.7f * flap);
            d.rotRect(b, pos.x - uy * off, pos.y + ux * off, r * 0.95f, r * 1.6f * flap, angle, WING, 1f);
        }
        d.rotRect(b, pos.x - ux * r * 0.95f, pos.y - uy * r * 0.95f, r * 0.6f, r * 0.55f, angle, WING, 1f);
        d.circle(b, pos.x, pos.y, r * 0.85f, color);
        d.circle(b, pos.x + ux * r * 0.6f, pos.y + uy * r * 0.6f, r * 0.55f, NECK);
        d.circle(b, pos.x + ux * r * 0.8f, pos.y + uy * r * 0.8f, r * 0.42f, color);
        d.rotRect(b, pos.x + ux * r * 1.25f, pos.y + uy * r * 1.25f, r * 0.42f, r * 0.2f, angle, BEAK, 1f);
        d.circle(b, pos.x + ux * r * 0.9f - uy * r * 0.18f, pos.y + uy * r * 0.9f + ux * r * 0.18f, r * 0.1f, OUTLINE);
    }
}
