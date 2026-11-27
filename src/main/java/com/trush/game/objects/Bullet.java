package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.enemy.Enemy;
import com.trush.game.gfx.BossArt;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.ProjectileArt.Part;

/**
 * A single projectile, fired either by the player or by an enemy.
 * {@code accel} changes speed over time (units/s²) and {@code curve} bends the path (deg/s),
 * which is what most bullet-hell patterns are built from.
 */
public class Bullet {

    private static final Color OUTLINE = Color.valueOf("2a0a14");
    /** Scratch colour for drawing (the Disco Ball's tiles). */
    private static final Color TINT = new Color();
    // Render-only scratch colours; never mutate the projectile's gameplay colour.
    private static final Color SURFACE_SHADE = new Color();
    private static final Color SURFACE_LIGHT = new Color();
    private static final Color WING = Color.valueOf("5e667a");
    private static final Color NECK = Color.valueOf("4fa07a");
    private static final Color BEAK = Color.valueOf("e0a040");
    private static final Color PAPER_FOLD = Color.valueOf("c8d0dc");
    private static final Color NAIL_STEEL = Color.valueOf("c9ccd6");
    private static final Color KITTEN_DARK = Color.valueOf("c8742a");
    private static final Color KITTEN_NOSE = Color.valueOf("ff5f8a");
    private static final Color SOCK_STRIPE = Color.valueOf("e84a4a");
    private static final Color SOCK_STINK = Color.valueOf("b090e0");
    private static final Color SNOW_EDGE = Color.valueOf("3a78b0");
    private static final Color FEATHER_COLOR = Color.valueOf("fbf6ff");
    private static final Color POPCORN = Color.valueOf("f2c040");
    private static final float BANANA_SIZE = 0.65f;
    private static final float BANANA_RIM = 0.02f;
    private static final float BANANA_SPIN = 620f;
    private static final float BANANA_SWEEP_DEGREES = 5f;
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
    /** Half-hearts removed when an enemy bullet hits the player. */
    public int playerDamage = 1;
    public float life;
    public float accel;
    public float curve;
    public float minSpeed = 0.4f;
    /** Snakes left and right by up to this many deg/s (bubbles, bees). */
    public float wobble;
    /** Player bullets: turns toward the nearest enemy by up to this many deg/s (bees); see GameWorld. */
    public float homing;
    /** A burst's fragments all pick targets by distance from this shared point. */
    public Vector2 homingOrigin;
    /** The gun that fired this shot, if any. Kept even if the player switches weapons. */
    public String weaponName;
    /** This Banana-rang finished an enemy before turning back. */
    public boolean outboundKill;
    /** Player boomerang: flies out and slows down, then comes back to the player (see GameWorld). */
    public boolean boomerang;
    /** A boomerang on its way back. */
    public boolean returning;
    /** Drawn as a see-through bubble instead of a solid ball. */
    public boolean bubble;
    /** Bounces off walls and cover this many more times instead of stopping (rubber balls). */
    public int bounces;
    /** Optional material tint after a wall reflection (the lost marble's glass loses its bright shine). */
    public Color bouncedColor;
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
    public enum Shape { BALL, DROP, CLOUD, SQUARE, TRIANGLE, DIAMOND, STAR, PILLOW, FEATHER, DUST, GLOB, FLARE, SHARK, BOMB, BARREL, SNOWFLAKE, NOTE, POMPOM, DUCK, PLANE, TENNIS, CRAYON, PINWHEEL, PETAL, SOCK, DISCO, NAIL, KITTEN, BANANA, MISSILE, RUBBLE, JUNK_BAG,
        DUST_PUFF, NEEDLE, CARTRIDGE }
    public Shape shape = Shape.BALL;
    /** Drawn as a flapping pigeon flying the way it moves (the Pigeon King's flocks); body in {@link #color}. */
    public boolean pigeon;
    /** Player bullets: bursts into this many feathers wherever it stops (the Pillow Cannon). */
    public int splits;
    /** What it bursts into (feathers; the Popcorn Popper's puffs) and how hard each piece hits, x its damage. */
    public Shape splitShape = Shape.FEATHER;
    public float splitDamage = 0.35f;
    /** Homing turn rate in degrees per second for each fragment, or zero for free flight. */
    public float splitHoming;
    /** Hot air: shoves the smoke and stink clouds it passes through away (the Hair Dryer). */
    public boolean blowsGas;
    /** Sticks in what it hits (Sticky Darts; GameWorld pops a full set). */
    public boolean sticky;
    /** Player bullets: turns this many degrees a second after wherever the hero aims (Paper Planes). */
    public float guided;
    /** Enemy the Pinwheel is flying toward. While locked on, it keeps its launch speed. */
    public Enemy pinwheelTarget;
    /** Bounces off enemies too, while it has bounces left (the Tennis Ball Launcher). */
    public boolean rebounds;
    /** Each bounce makes it this many times faster (0: no change). */
    public float bounceBoost;
    /** Lets out a cloud of the hero's stink where it stops (the Stinky Sock Launcher). */
    public boolean stinks;
    /** Waits this long at the gun before it flies (and can hit anything) - the later peas of a burst. */
    public float delay;
    /** Player bullets: slows whatever it hits down for this many seconds (the Snowball Slinger). */
    public float chill;
    /** Player bullets: water (or snow) - it puts fires out (the burning drum hiding the way to the Slums). */
    public boolean wet;
    /** Player bullets: once stopped, hangs this many more seconds sweeping beams round (the Disco Ball). */
    public float disco;
    /** The Disco Ball's beams: counting down to the next sweep, and where they point. */
    public float discoTick;
    public float discoAngle;
    /** Player bullets: the spark jumps on to this many more enemies from the one it hits (Jumper Cables). */
    public int chain;
    /** Player bullets: pulls regular enemies within this radius in and chews on them (Grey Goo). */
    public float vortex;
    /** Counts down between Grey Goo's bites. */
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
    /** Heading of the last simulated pose, before GameWorld steers the next frame. */
    private float lastHeading;
    private float bananaSweepStart;
    private float bananaSweepRotation;
    private boolean bananaSweepReady;
    private final float phase = MathUtils.random(MathUtils.PI2);
    public float maxSpeed = 12f;
    public float knockback = 0.6f;
    public boolean piercing;
    public boolean alive = true;
    /**
     * Enemy bullets: swept away by a blank (or anything else clearing bullets), not stopped by a hit - so a bomb or
     * rocket that bursts where it stops just vanishes, even when its owner only notices after the blank's daze.
     */
    public boolean blanked;
    /** Enemy bullets: already slowed down by the Megaphone's shout (once is enough - see {@link #hush}). */
    public boolean hushed;
    /** Enemies a piercing bullet already went through, so it only hits each once. */
    public final Array<Object> alreadyHit = new Array<>(false, 4);

    public Bullet(boolean fromPlayer, float x, float y, float angleDeg, float speed, float radius, float damage, Color color) {
        this.fromPlayer = fromPlayer;
        pos.set(x, y);
        prev.set(x, y);
        vel.set(MathUtils.cosDeg(angleDeg), MathUtils.sinDeg(angleDeg)).scl(speed);
        lastHeading = vel.angleDeg();
        this.radius = radius;
        this.damage = damage;
        this.color.set(color);
        this.life = 6f;
    }

    public void onBounce() {
        bounces--;
        if (bouncedColor != null) color.set(bouncedColor);
    }

    private float bananaAngle() {
        return vel.angleDeg() + age * BANANA_SPIN;
    }

    private static float bananaBend(float t, float size) {
        return (1.2f * t * t - 0.55f) * size;
    }

    private static float bananaWidth(float t, float size) {
        return size * (0.5f - 0.22f * Math.abs(t));
    }

    /** Swept contact with a round enemy. A banana follows its drawn crescent, including its tips. */
    public boolean hitsCircle(float x, float y, float targetRadius) {
        if (shape != Shape.BANANA) {
            return Intersector.distanceSegmentPoint(prev.x, prev.y, pos.x, pos.y, x, y) < targetRadius + radius;
        }
        float size = radius * BANANA_SIZE;
        // Most targets are far away: skip the nine small swept circles that trace the curved skin.
        if (Intersector.distanceSegmentPoint(prev.x, prev.y, pos.x, pos.y, x, y)
                >= targetRadius + size * 2.6f + BANANA_RIM) return false;
        float startAngle = bananaSweepReady ? bananaSweepStart : bananaAngle();
        float rotation = bananaSweepReady ? bananaSweepRotation : 0f;
        int steps = Math.max(1, MathUtils.ceil(Math.abs(rotation) / BANANA_SWEEP_DEGREES));
        float ax = prev.x;
        float ay = prev.y;
        float ux = MathUtils.cosDeg(startAngle);
        float uy = MathUtils.sinDeg(startAngle);
        // Sweep short chords of each skin sample's actual moving, rotating path. At five degrees
        // their deviation from the arc is far smaller than one pixel, even at the banana's tips.
        for (int step = 1; step <= steps; step++) {
            float fraction = step / (float) steps;
            float bx = MathUtils.lerp(prev.x, pos.x, fraction);
            float by = MathUtils.lerp(prev.y, pos.y, fraction);
            float vx = MathUtils.cosDeg(startAngle + rotation * fraction);
            float vy = MathUtils.sinDeg(startAngle + rotation * fraction);
            for (int i = 0; i <= 8; i++) {
                float t = i / 4f - 1f;
                float along = t * size * 2.1f;
                float across = bananaBend(t, size);
                float reach = targetRadius + bananaWidth(t, size) + BANANA_RIM;
                if (Intersector.distanceSegmentPoint(
                        ax + ux * along - uy * across, ay + uy * along + ux * across,
                        bx + vx * along - vy * across, by + vy * along + vx * across, x, y) < reach) {
                    return true;
                }
            }
            ax = bx;
            ay = by;
            ux = vx;
            uy = vy;
        }
        return false;
    }

    /** Turns back once, allowing one more hit per enemy on the return journey. */
    public void returnBoomerang() {
        if (returning) return;
        returning = true;
        alreadyHit.clear();
        accel = 24f;
        maxSpeed = 12f;
        vel.setLength(Math.max(6f, vel.len()));
    }

    /** Slows on the way out, then homes back to the hand; returning bananas pass through cover. */
    public void steerBoomerang(float handX, float handY) {
        if (!returning && vel.len() <= minSpeed + 0.05f) returnBoomerang();
        if (returning) {
            float speed = vel.len();
            vel.set(handX - pos.x, handY - pos.y).nor().scl(speed);
            if (Vector2.dst2(pos.x, pos.y, handX, handY) < 0.3f * 0.3f) alive = false;
        }
    }

    /** Popcorn opens towards the aim; other splits start in a ring. */
    public Array<Bullet> splitBullets() {
        Array<Bullet> children = new Array<>(splits);
        boolean popcorn = splitShape == Shape.CLOUD;
        Vector2 burstOrigin = splitHoming > 0 ? prev.cpy() : null;
        // On impact, the two front feathers straddle the shot's heading and can hit the impact target again.
        Object pillowTarget = shape == Shape.PILLOW && !alreadyHit.isEmpty() ? alreadyHit.peek() : null;
        float start;
        if (popcorn) {
            start = vel.angleDeg() - 28f;
        } else if (pillowTarget != null) {
            start = vel.angleDeg() - 180f / splits;
        } else {
            start = MathUtils.random(360f);
        }
        for (int i = 0; i < splits; i++) {
            float angle = popcorn ? (splits == 1 ? vel.angleDeg() : start + 56f * i / (splits - 1))
                    : start + 360f * i / splits;
            Bullet child = new Bullet(true, prev.x, prev.y, angle,
                    popcorn ? MathUtils.random(6.5f, 8f) : MathUtils.random(4f, 5.5f),
                    popcorn ? 0.05f : 0.035f, damage * splitDamage,
                    popcorn ? POPCORN : splitShape == Shape.FEATHER ? FEATHER_COLOR : color);
            child.shape = splitShape;
            child.weaponName = weaponName;
            child.life = popcorn ? MathUtils.random(0.4f, 0.55f) : MathUtils.random(0.35f, 0.5f);
            child.knockback = 0.3f;
            if (burstOrigin != null) {
                child.homing = splitHoming;
                child.homingOrigin = burstOrigin;
                child.life = 1.2f;
            } else if (!popcorn) {
                child.alreadyHit.addAll(alreadyHit);
                if (pillowTarget != null && i < 2) {
                    child.alreadyHit.removeValue(pillowTarget, true);
                }
            }
            children.add(child);
        }
        return children;
    }

    /** The Megaphone: shouted at, it flies on at {@code k} x its speed (only the first shout counts). */
    public void hush(float k) {
        if (hushed) {
            return;
        }
        hushed = true;
        vel.scl(k);
        accel *= k;
        maxSpeed *= k;
    }

    /** The first update sweeps from (x, y) - e.g. the gun hand - instead of from where it was spawned. */
    public void sweepFrom(float x, float y) {
        prev.set(x, y);
        keepPrev = true;
    }

    // A thread or toy following a marked course. Points are immutable after launch.
    private Vector2[] course;
    private int nextPoint;
    private float courseStep;
    private float courseSpeedScale;
    private float courseCoastScale;

    public void followCourse(Vector2[] points) {
        followCourse(points, 0f, 1f);
    }

    /** Give each waypoint the same travel time, then coast along the last segment at a multiple of base speed. */
    public void followCourse(Vector2[] points, float distancePerStep, float coastSpeedScale) {
        course = points;
        nextPoint = 1; // the projectile starts at the first point
        courseStep = distancePerStep;
        courseSpeedScale = 1f;
        courseCoastScale = coastSpeedScale;
    }

    private void advanceCourse(float delta) {
        float speed = vel.len() / courseSpeedScale;
        if (speed == 0f || delta <= 0f) return;
        float distance = speed * delta;
        while (nextPoint < course.length) {
            Vector2 end = course[nextPoint];
            float remaining = pos.dst(end);
            if (remaining > 0.0001f) {
                courseSpeedScale = courseStep > 0f && nextPoint < course.length - 1
                        ? course[nextPoint - 1].dst(end) / courseStep : courseCoastScale;
                vel.set(end).sub(pos).scl(speed * courseSpeedScale / remaining);
                float baseDistance = remaining / courseSpeedScale;
                if (distance < baseDistance) {
                    pos.mulAdd(vel, distance / speed);
                    return;
                }
                distance -= baseDistance;
            }
            pos.set(end);
            nextPoint++;
        }
        alive = false;
    }

    public void update(float delta) {
        update(delta, 0f);
    }

    /** Drift shifts free-flight shots while leaving the direction of travel and fixed paths intact. */
    public void update(float delta, float driftX) {
        if (delay > 0) {
            delay -= delta;
            prev.set(pos);
            lastHeading = vel.angleDeg();
            bananaSweepReady = false;
            return;
        }
        if (keepPrev) {
            keepPrev = false;
        } else {
            prev.set(pos);
        }
        if (shape == Shape.BANANA) bananaSweepStart = lastHeading + age * BANANA_SPIN;
        age += delta;
        if (wobble != 0) {
            vel.rotateDeg(MathUtils.sin(age * 9f + phase) * wobble * delta);
        }
        if (curve != 0) {
            vel.rotateDeg(curve * delta);
        }
        if (accel != 0 && pinwheelTarget == null) {
            float speed = MathUtils.clamp(vel.len() + accel * delta, minSpeed, maxSpeed);
            vel.setLength(speed);
        }
        if (shape == Shape.BANANA) {
            float heading = vel.angleDeg();
            // Steering crosses 0 degrees by the shortest turn; tumbling retains complete revolutions.
            float steering = (heading - lastHeading + 540f) % 360f - 180f;
            bananaSweepRotation = steering + delta * BANANA_SPIN;
            bananaSweepReady = true;
            lastHeading = heading;
        }
        if (course == null) {
            pos.mulAdd(vel, delta);
            if (driftX != 0f && !vel.isZero()) pos.x += driftX * delta;
        } else {
            advanceCourse(delta);
        }
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
            drawMissile(b, d);
        } else if (streak > 0) {
            float angle = vel.angleDeg();
            float x = pos.x - MathUtils.cosDeg(angle) * streak;
            float y = pos.y - MathUtils.sinDeg(angle) * streak;
            d.shotBeam(b, x, y, streak, radius * 3.2f, radius * 0.7f, angle, color, 0.88f);
        } else if (bubble && !fromPlayer) { // an enemy's bubble: outlined and filled like its other bullets, so it
            // never passes for the hero's own see-through ones (the Flippers', the Bubble Blaster's)
            d.circle(b, pos.x, pos.y, radius * 1.3f, OUTLINE);
            d.circle(b, pos.x, pos.y, radius, color, 0.75f);
            surfaceColours();
            d.ring(b, pos.x, pos.y, radius * 0.87f, SURFACE_SHADE, 0.86f);
            d.ring(b, pos.x - radius * 0.06f, pos.y + radius * 0.07f, radius * 0.73f, Color.WHITE, 0.56f);
            d.circle(b, pos.x - radius * 0.35f, pos.y + radius * 0.35f, radius * 0.3f, Color.WHITE, 0.95f);
        } else if (bubble) {
            d.circle(b, pos.x, pos.y, radius, color, 0.25f);
            d.ring(b, pos.x, pos.y, radius, color, 0.9f);
            d.ring(b, pos.x - radius * 0.08f, pos.y + radius * 0.1f, radius * 0.75f, Color.WHITE, 0.3f);
            d.circle(b, pos.x - radius * 0.35f, pos.y + radius * 0.35f, radius * 0.25f, Color.WHITE, 0.9f);
        } else if (shape != Shape.BALL) {
            drawShape(b, d);
        } else if (fromPlayer) {
            d.circle(b, pos.x, pos.y, radius * 1.9f, color, 0.25f);
            drawOrb(b, d, radius);
            d.circle(b, pos.x, pos.y, radius * 0.45f, Color.WHITE);
        } else {
            d.circle(b, pos.x, pos.y, radius * 1.3f, OUTLINE);
            drawOrb(b, d, radius);
        }
    }

    /** Stable upper-left lighting, contained inside the original filled radius. */
    private void drawOrb(SpriteBatch b, Draw d, float r) {
        surfaceColours();
        d.circle(b, pos.x, pos.y, r, SURFACE_SHADE);
        d.circle(b, pos.x - r * 0.06f, pos.y + r * 0.08f, r * 0.88f, color);
        d.circle(b, pos.x - r * 0.18f, pos.y + r * 0.24f, r * 0.52f, SURFACE_LIGHT);
        d.circle(b, pos.x - r * 0.3f, pos.y + r * 0.36f, r * 0.19f, Color.WHITE, 0.92f);
        d.circle(b, pos.x + r * 0.42f, pos.y - r * 0.48f, r * 0.09f, SURFACE_LIGHT);
    }

    private void surfaceColours() {
        SURFACE_SHADE.set(color).mul(0.72f, 0.72f, 0.78f, 1);
        SURFACE_LIGHT.set(color).lerp(Color.WHITE, 0.24f);
        SURFACE_LIGHT.a = color.a;
    }

    /** A raised face and a shaded bevel; the existing outline and rotation remain unchanged. */
    private void drawBlockFace(SpriteBatch b, Draw d, float w, float h, float angle) {
        surfaceColours();
        d.rotRect(b, pos.x, pos.y, w, h, angle, SURFACE_SHADE, 1);
        d.rotRect(b, pos.x - radius * 0.04f, pos.y + radius * 0.06f, w * 0.85f, h * 0.85f, angle, color, 1);
        d.rotRect(b, pos.x - radius * 0.14f, pos.y + radius * 0.18f, w * 0.4f, h * 0.24f, angle, SURFACE_LIGHT, 1);
        d.circle(b, pos.x - radius * 0.22f, pos.y + radius * 0.24f, radius * 0.12f, Color.WHITE, 0.9f);
    }

    private void drawGemFace(SpriteBatch b, Draw d, Texture mask, float r, float angle) {
        surfaceColours();
        d.shape(b, mask, pos.x, pos.y, r, angle, SURFACE_SHADE, 1);
        d.shape(b, mask, pos.x, pos.y, r * 0.84f, angle, color, 1);
        d.shape(b, mask, pos.x, pos.y, r * 0.42f, angle, SURFACE_LIGHT, 1);
        d.circle(b, pos.x - radius * 0.08f, pos.y + radius * 0.12f, radius * 0.13f, Color.WHITE, 0.9f);
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
            case BANANA: {
                // Match the crescent hitbox, but draw chunky pixels instead of overlapping smooth circles.
                r *= BANANA_SIZE;
                float pixel = r * 0.2f;
                float w = d.banana.getWidth() * pixel;
                float h = d.banana.getHeight() * pixel;
                b.setColor(Color.WHITE);
                b.draw(d.banana, pos.x - w / 2f, pos.y - h / 2f, w / 2f, h / 2f,
                        w, h, 1f, 1f, bananaAngle(), 0, 0,
                        d.banana.getWidth(), d.banana.getHeight(), false, false);
                break;
            }
            case DROP: { // round at the front, pointed at the back, like a falling drop
                float ux = MathUtils.cosDeg(angle);
                float uy = MathUtils.sinDeg(angle);
                float tx = pos.x - ux * r * 0.6f;
                float ty = pos.y - uy * r * 0.6f;
                d.rotRect(b, tx, ty, r * 1.45f, r * 1.45f, angle + 45f, OUTLINE, 0.8f);
                d.circle(b, pos.x, pos.y, r * 1.2f, OUTLINE, 0.8f);
                d.rotRect(b, tx, ty, r * 1.15f, r * 1.15f, angle + 45f, color, 1f);
                drawOrb(b, d, r);
                break;
            }
            case CLOUD:
            case DUST_PUFF: {
                float y = pos.y + MathUtils.sin(age * 8f + phase) * r * 0.08f;
                boolean dustPuff = shape == Shape.DUST_PUFF;
                SURFACE_LIGHT.set(color).lerp(Color.WHITE, dustPuff ? 0.24f : 0.54f);
                d.projectiles.draw(b, Part.CLOUD, pos.x, y, r * 2.63f, r * 2.20f,
                        0f, false, dustPuff ? color : SURFACE_LIGHT, 1f,
                        dustPuff ? SURFACE_LIGHT : Color.WHITE);
                break;
            }
            case SQUARE:
                d.rotRect(b, pos.x, pos.y, r * 2.1f, r * 2.1f, spin, OUTLINE, 1f);
                drawBlockFace(b, d, r * 1.6f, r * 1.6f, spin);
                break;
            case DIAMOND:
                d.rotRect(b, pos.x, pos.y, r * 1.85f, r * 1.85f, angle + 45f, OUTLINE, 1f); // a corner leads
                drawBlockFace(b, d, r * 1.45f, r * 1.45f, angle + 45f);
                break;
            case TRIANGLE: // points the way it flies
                d.shape(b, d.triangle, pos.x, pos.y, r * 1.55f, angle, OUTLINE, 1f);
                drawGemFace(b, d, d.triangle, r * 1.2f, angle);
                break;
            case PILLOW: { // a plump pillow tumbling through the air, its corners poking out
                d.projectiles.draw(b, Part.PILLOW, pos.x, pos.y, r * 2.55f, r * 2.22f,
                        spin * 0.5f, false, color, 1f);
                break;
            }
            case FEATHER: {
                float tilt = angle + MathUtils.sin(age * 20f + phase) * 25f;
                d.projectiles.draw(b, Part.FEATHER, pos.x, pos.y, r * 4f, r * 2.4f,
                        tilt, false, color, 1f);
                break;
            }
            case DUST: { // Grey Goo: a spinning grey blob with two little eyes
                for (int i = 0; i < 7; i++) {
                    float a = spin + i * 360f / 7f;
                    d.circle(b, pos.x + MathUtils.cosDeg(a) * r * 0.7f, pos.y + MathUtils.sinDeg(a) * r * 0.7f,
                            r * 0.5f, color, 0.85f);
                }
                drawOrb(b, d, r * 0.8f);
                d.circle(b, pos.x - r * 0.25f, pos.y + r * 0.1f, r * 0.14f, Color.WHITE);
                d.circle(b, pos.x + r * 0.25f, pos.y + r * 0.1f, r * 0.14f, Color.WHITE);
                d.circle(b, pos.x - r * 0.25f, pos.y + r * 0.1f, r * 0.07f, OUTLINE);
                d.circle(b, pos.x + r * 0.25f, pos.y + r * 0.1f, r * 0.07f, OUTLINE);
                break;
            }
            case GLOB: { // a wobbling blob of acid with a bright fleck
                float wob = MathUtils.sin(age * 14f + phase) * 0.15f;
                d.ellipse(b, pos.x, pos.y, r * (1.25f + wob), r * (1.25f - wob), OUTLINE, 0.9f);
                surfaceColours();
                d.ellipse(b, pos.x, pos.y, r * (1.05f + wob), r * (1.05f - wob), SURFACE_SHADE, 1f);
                d.ellipse(b, pos.x - r * 0.06f, pos.y + r * 0.08f,
                        r * (0.9f + wob * 0.8f), r * (0.9f - wob * 0.8f), color, 1f);
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
                drawOrb(b, d, r);
                d.circle(b, pos.x, pos.y, r * 0.46f, Color.WHITE);
                break;
            }
            case SHARK: {
                float ux = MathUtils.cosDeg(angle), uy = MathUtils.sinDeg(angle);
                boolean left = ux < 0f;
                float stroke = MathUtils.sin(age * 18f + phase);
                d.projectiles.draw(b, Part.SHARK_TAIL, pos.x - ux * r * 1.28f, pos.y - uy * r * 1.28f,
                        r * 1.66f, r * 1.86f, angle + 21f * stroke, left, color, 1f);
                float open = Math.max(gape, 0.12f + 0.14f * Math.abs(stroke));
                d.projectiles.shark(b, pos.x - ux * r * 0.15f, pos.y - uy * r * 0.15f,
                        r * 3.08f, r * 2.6f, angle, open, color, 1f);
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
                d.circle(b, pos.x, pos.y, r * 0.51f, SNOW_EDGE, 1f);
                drawOrb(b, d, r * 0.37f);
                break;
            }
            case BOMB: { // a round little bomb with a sparking fuse
                float sx = pos.x + r * 0.6f;
                float sy = pos.y + r * 0.8f;
                d.circle(b, pos.x, pos.y, r * 1.25f, OUTLINE);
                drawOrb(b, d, r);
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
                drawBlockFace(b, d, along, across, angle);
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
            case NOTE: {
                float y = pos.y + MathUtils.sin(age * 10f + phase) * r * 0.3f;
                d.projectiles.draw(b, Part.NOTE, pos.x, y + r * 0.25f, r * 2.8f, r * 3f,
                        0f, false, color, 1f);
                break;
            }
            case POMPOM: { // a giant pom-pom rolling along: a ring of fluffy tufts turning round a soft middle
                float roll = age * vel.len() / r * MathUtils.radiansToDegrees * 0.5f;
                d.ellipse(b, pos.x, pos.y - r * 0.85f, r * 0.9f, r * 0.25f, OUTLINE, 0.3f); // its shadow
                for (int i = 0; i < 9; i++) {
                    float a = roll + i * 40f;
                    d.circle(b, pos.x + MathUtils.cosDeg(a) * r * 0.72f, pos.y + MathUtils.sinDeg(a) * r * 0.72f,
                            r * 0.38f, color, 1f);
                }
                drawOrb(b, d, r * 0.85f);
                d.circle(b, pos.x - r * 0.3f, pos.y + r * 0.3f, r * 0.35f, Color.WHITE, 0.6f);
                break;
            }
            case DUCK: { // a rubber duck waddling the way it goes: yellow body, round head, orange beak
                float side = angleSide(angle); // faces left or right, whichever way it heads
                float waddle = MathUtils.sin(age * 16f + phase) * 12f;
                d.ellipse(b, pos.x, pos.y - r * 0.9f, r * 1.1f, r * 0.3f, OUTLINE, 0.3f);
                d.ellipse(b, pos.x, pos.y, r * 1.25f, r * 0.85f, OUTLINE, 1f);
                surfaceColours();
                d.ellipse(b, pos.x, pos.y, r * 1.1f, r * 0.7f, SURFACE_SHADE, 1f);
                d.ellipse(b, pos.x - r * 0.08f, pos.y + r * 0.12f, r * 0.95f, r * 0.52f, color, 1f);
                d.ellipse(b, pos.x - r * 0.26f, pos.y + r * 0.22f, r * 0.42f, r * 0.12f, SURFACE_LIGHT, 1f);
                d.rotRect(b, pos.x - side * r * 0.9f, pos.y + r * 0.35f, r * 0.5f, r * 0.35f, waddle - side * 30f, color, 1f); // tail
                float hx = pos.x + side * r * 0.55f;
                float hy = pos.y + r * 0.7f;
                d.circle(b, hx, hy, r * 0.62f, OUTLINE);
                d.circle(b, hx, hy, r * 0.5f, color);
                d.rotRect(b, hx + side * r * 0.55f, hy - r * 0.1f, r * 0.45f, r * 0.25f, 0f, BEAK, 1f);
                d.circle(b, hx + side * r * 0.15f, hy + r * 0.15f, r * 0.11f, OUTLINE);
                break;
            }
            case CARTRIDGE:
                d.projectiles.draw(b, Part.CARTRIDGE, pos.x, pos.y, r * 2.65f, r * 2.6f,
                        angle, false, color, 1f);
                break;
            case NAIL:
            case NEEDLE: { // point first, with a thin shaft and a flat head at the back
                float ux = MathUtils.cosDeg(angle);
                float uy = MathUtils.sinDeg(angle);
                surfaceColours();
                d.rotRect(b, pos.x - ux * r * 1.2f, pos.y - uy * r * 1.2f, r * 5f, r * 1.1f, angle, OUTLINE, 1f);
                d.rotRect(b, pos.x - ux * r * 1.2f, pos.y - uy * r * 1.2f, r * 4.6f, r * 0.6f,
                        angle, shape == Shape.NEEDLE ? color : NAIL_STEEL, 1f);
                d.rotRect(b, pos.x - ux * r * 1.2f - uy * r * 0.15f,
                        pos.y - uy * r * 1.2f + ux * r * 0.15f, r * 4f, r * 0.15f,
                        angle, shape == Shape.NEEDLE ? SURFACE_LIGHT : Color.WHITE, 0.8f);
                d.rotRect(b, pos.x - ux * r * 3.5f, pos.y - uy * r * 3.5f, r * 0.7f, r * 2f, angle, OUTLINE, 1f);
                d.rotRect(b, pos.x - ux * r * 3.5f, pos.y - uy * r * 3.5f, r * 0.45f, r * 1.7f, angle, color, 1f);
                break;
            }
            case KITTEN: { // a little ginger kitten mid-pounce: paws out, ears up, tail swishing
                float side = angleSide(angle);
                float swish = MathUtils.sin(age * 14f + phase) * 25f;
                d.ellipse(b, pos.x, pos.y - r * 1.1f, r * 1.1f, r * 0.3f, OUTLINE, 0.3f); // its shadow
                d.rotRect(b, pos.x - side * r * 1.1f, pos.y + r * 0.3f, r * 1.1f, r * 0.3f, (side > 0 ? 150f : 30f) + swish, KITTEN_DARK, 1f);
                d.ellipse(b, pos.x, pos.y, r * 1.2f, r * 0.8f, OUTLINE, 1f);
                surfaceColours();
                d.ellipse(b, pos.x, pos.y, r * 1.05f, r * 0.65f, SURFACE_SHADE, 1f);
                d.ellipse(b, pos.x - r * 0.06f, pos.y + r * 0.1f, r * 0.92f, r * 0.49f, color, 1f);
                d.ellipse(b, pos.x - r * 0.26f, pos.y + r * 0.23f, r * 0.38f, r * 0.15f, SURFACE_LIGHT, 1f);
                for (int i = -1; i <= 1; i += 2) { // paws reaching out
                    d.circle(b, pos.x + side * r * (0.9f + 0.15f * i), pos.y - r * 0.55f, r * 0.25f, Color.WHITE);
                }
                float hx = pos.x + side * r * 0.75f;
                float hy = pos.y + r * 0.45f;
                d.rotRect(b, hx - r * 0.32f, hy + r * 0.55f, r * 0.35f, r * 0.45f, 15f, OUTLINE, 1f); // ears
                d.rotRect(b, hx + r * 0.32f, hy + r * 0.55f, r * 0.35f, r * 0.45f, -15f, OUTLINE, 1f);
                d.circle(b, hx, hy, r * 0.68f, OUTLINE);
                d.circle(b, hx, hy, r * 0.55f, color);
                d.circle(b, hx - r * 0.2f, hy + r * 0.08f, r * 0.1f, OUTLINE); // eyes
                d.circle(b, hx + r * 0.2f, hy + r * 0.08f, r * 0.1f, OUTLINE);
                d.circle(b, hx, hy - r * 0.15f, r * 0.08f, KITTEN_NOSE);
                break;
            }
            case PLANE: { // a folded paper plane, nose first: two wings and a crease down the middle
                float ux = MathUtils.cosDeg(angle);
                float uy = MathUtils.sinDeg(angle);
                d.ellipse(b, pos.x, pos.y - r * 1.6f, r * 1.2f, r * 0.3f, OUTLINE, 0.25f); // its shadow far below
                d.shape(b, d.triangle, pos.x, pos.y, r * 2.3f, angle, OUTLINE, 1f);
                drawGemFace(b, d, d.triangle, r * 1.9f, angle);
                d.rotRect(b, pos.x - ux * r * 0.3f, pos.y - uy * r * 0.3f, r * 2.2f, r * 0.28f, angle, PAPER_FOLD, 1f);
                break;
            }
            case TENNIS: { // a fuzzy yellow-green ball with its white seam, spinning
                d.circle(b, pos.x, pos.y, r * 1.2f, OUTLINE);
                drawOrb(b, d, r);
                d.rotRect(b, pos.x, pos.y, r * 1.9f, r * 0.22f, spin, Color.WHITE, 0.9f);
                d.circle(b, pos.x - r * 0.35f, pos.y + r * 0.35f, r * 0.25f, Color.WHITE, 0.5f);
                break;
            }
            case CRAYON: { // a stubby crayon, point first, with its paper wrapper
                float ux = MathUtils.cosDeg(angle);
                float uy = MathUtils.sinDeg(angle);
                d.rotRect(b, pos.x, pos.y, r * 3.6f, r * 1.6f, angle, OUTLINE, 1f);
                drawBlockFace(b, d, r * 3.2f, r * 1.2f, angle);
                d.rotRect(b, pos.x - ux * r * 0.4f, pos.y - uy * r * 0.4f, r * 1.4f, r * 1.25f, angle, Color.WHITE, 0.55f);
                d.shape(b, d.triangle, pos.x + ux * r * 1.9f, pos.y + uy * r * 1.9f, r * 0.75f, angle, color, 1f);
                break;
            }
            case PINWHEEL: { // four curled sails round a pin, spinning faster as it slows down
                float whirl = age * 900f + phase * MathUtils.radiansToDegrees;
                surfaceColours();
                for (int i = 0; i < 4; i++) {
                    float a = whirl + i * 90f;
                    float cx = pos.x + MathUtils.cosDeg(a) * r * 0.55f;
                    float cy = pos.y + MathUtils.sinDeg(a) * r * 0.55f;
                    d.shape(b, d.triangle, cx, cy, r * 0.85f, a + 90f, OUTLINE, 1f);
                    d.shape(b, d.triangle, cx, cy, r * 0.65f, a + 90f, i % 2 == 0 ? SURFACE_SHADE : Color.LIGHT_GRAY, 1f);
                    d.shape(b, d.triangle, cx, cy, r * 0.48f, a + 90f, i % 2 == 0 ? SURFACE_LIGHT : Color.WHITE, 1f);
                }
                d.circle(b, pos.x, pos.y, r * 0.22f, BEAK);
                break;
            }
            case DISCO: { // a mirror ball on a string: silver tiles, a few catching the light as it turns
                d.line(b, pos.x, pos.y + r, r * 2.2f, r * 0.12f, 90f, OUTLINE, 0.8f);
                d.circle(b, pos.x, pos.y, r * 1.15f, OUTLINE);
                d.circle(b, pos.x, pos.y, r, Color.GRAY);
                d.circle(b, pos.x - r * 0.1f, pos.y + r * 0.1f, r * 0.83f, Color.LIGHT_GRAY);
                float turn = age * 3f;
                for (int i = 0; i < 6; i++) {
                    float tx = pos.x + MathUtils.sin(turn + i * 1.05f) * r * 0.6f;
                    float ty = pos.y + (i % 3 - 1) * r * 0.5f;
                    TINT.fromHsv((age * 200f + i * 60f) % 360f, 0.8f, 1f);
                    d.rect(b, tx - r * 0.25f, ty - r * 0.25f, r * 0.5f, r * 0.5f, TINT);
                }
                d.circle(b, pos.x - r * 0.4f, pos.y + r * 0.45f, r * 0.15f, Color.WHITE, 0.8f);
                break;
            }
            case PETAL: // a pink petal, fluttering
                d.projectiles.draw(b, Part.PETAL, pos.x, pos.y, r * 3.02f, r * 2.67f,
                        angle + MathUtils.sin(age * 18f + phase) * 30f, false, color, 1f);
                break;
            case SOCK: { // an old sock tumbling through the air: a stripe at the top, a heel, a toe
                d.rotRect(b, pos.x, pos.y + r * 0.3f, r * 1.4f, r * 2.4f, spin * 0.6f, OUTLINE, 1f);
                surfaceColours();
                d.rotRect(b, pos.x, pos.y + r * 0.3f, r * 1.1f, r * 2.1f, spin * 0.6f, SURFACE_SHADE, 1f);
                d.rotRect(b, pos.x - r * 0.05f, pos.y + r * 0.36f, r * 0.9f, r * 1.9f, spin * 0.6f, color, 1f);
                float fx = pos.x + MathUtils.cosDeg(spin * 0.6f - 90f) * r * 0.75f;
                float fy = pos.y + r * 0.3f + MathUtils.sinDeg(spin * 0.6f - 90f) * r * 0.75f;
                d.circle(b, fx + MathUtils.cosDeg(spin * 0.6f) * r * 0.4f, fy + MathUtils.sinDeg(spin * 0.6f) * r * 0.4f,
                        r * 0.6f, color);
                d.rotRect(b, pos.x - MathUtils.cosDeg(spin * 0.6f - 90f) * r * 0.75f,
                        pos.y + r * 0.3f - MathUtils.sinDeg(spin * 0.6f - 90f) * r * 0.75f,
                        r * 1.1f, r * 0.35f, spin * 0.6f, SOCK_STRIPE, 1f);
                d.circle(b, pos.x + MathUtils.sin(age * 5f) * r, pos.y + r * 1.8f, r * 0.3f, SOCK_STINK, 0.6f);
                break;
            }
            case STAR:
                d.projectiles.star(b, pos.x, pos.y, r * 1.6f, spin, color, 1f);
                break;
            case MISSILE:
                drawMissile(b, d);
                break;
            case RUBBLE:
                d.projectiles.draw(b, Part.SHARD, pos.x, pos.y, r * 2.4f, r * 2.4f,
                        spin, false, color, 1f);
                break;
            case JUNK_BAG:
                d.bosses.draw(b, BossArt.Part.TRASH_BAG, pos.x, pos.y, r * 2.6f, r * 2.6f,
                        spin * 0.5f, false, false, Color.WHITE);
                break;
            default:
                break;
        }
    }

    private void drawMissile(SpriteBatch b, Draw d) {
        float angle = vel.angleDeg();
        float x = MathUtils.cosDeg(angle) * radius;
        float y = MathUtils.sinDeg(angle) * radius;
        d.circle(b, pos.x - x * 2.05f, pos.y - y * 2.05f, radius * 0.61f, Color.ORANGE, 0.82f);
        d.circle(b, pos.x - x * 1.86f, pos.y - y * 1.86f, radius * 0.28f, Color.WHITE, 0.8f);
        d.projectiles.draw(b, Part.ROCKET, pos.x, pos.y, radius * 3.55f, radius * 2.83f,
                angle, angleSide(angle) < 0f, color, 1f);
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
            d.projectiles.draw(b, Part.WING, pos.x - uy * off, pos.y + ux * off,
                    r * 1.12f, r * 1.75f * flap, angle, side < 0, WING, 1f);
        }
        d.rotRect(b, pos.x - ux * r * 0.95f, pos.y - uy * r * 0.95f, r * 0.6f, r * 0.55f, angle, WING, 1f);
        drawOrb(b, d, r * 0.85f);
        d.circle(b, pos.x + ux * r * 0.6f, pos.y + uy * r * 0.6f, r * 0.55f, NECK);
        d.circle(b, pos.x + ux * r * 0.8f, pos.y + uy * r * 0.8f, r * 0.42f, color);
        d.rotRect(b, pos.x + ux * r * 1.25f, pos.y + uy * r * 1.25f, r * 0.42f, r * 0.2f, angle, BEAK, 1f);
        d.circle(b, pos.x + ux * r * 0.9f - uy * r * 0.18f, pos.y + uy * r * 0.9f + ux * r * 0.18f, r * 0.1f, OUTLINE);
    }
}
