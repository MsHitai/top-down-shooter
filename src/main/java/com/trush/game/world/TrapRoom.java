package com.trush.game.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;
import com.trush.game.objects.Puddle;

/**
 * The hazards in a timed survival room. The director sends enemies in and waits for the last one
 * before opening the doors. GameWorld handles actor collisions and pressure hatches.
 */
public class TrapRoom {

    public enum Kind {
        ACID_FLOOD("acid", "THE ACID RISES!"),
        BURST_PIPES("pipes", "THE PIPES BURST!"),
        GARBAGE_CART("cart", "RUNAWAY GARBAGE CART!"),
        SEWER_BACKFLOW("backflow", "THE SEWER BACKS UP!"),
        MANHOLES("manholes", "WATCH THE MANHOLES!");

        public final String id;
        public final String banner;

        Kind(String id, String banner) {
            this.id = id;
            this.banner = banner;
        }

        static Kind named(String name) {
            for (Kind kind : values()) {
                if (kind.id.equalsIgnoreCase(name)) return kind;
            }
            return null;
        }
    }

    /** What the trap does to the room: GameWorld in the game, a stand-in in the tests. */
    public interface Hazards {
        /** Spills a puddle (null if it fizzled, like fire under water). */
        Puddle spill(Puddle.Kind kind, float x, float y, float radius, float life);

        /** A sound effect, by its Sfx name. */
        void sound(String name, float volume);

        /** A puff of steam from a pipe about to burst. */
        void steam(float x, float y);

        /** The cart's footprint, swept over the movement this frame. */
        void cart(Rectangle sweep);

        /** Displacement from a surge, for actors standing in the channel. */
        void current(float dx, float dy);
    }

    /** How long to survive. */
    public static final float DURATION = 20f;
    /** How long a hazard's warning shows before it hits. */
    public static final float ACID_WARN = 0.75f;
    public static final float JET_WARN = 0.85f;
    /** How long a jet's line burns. */
    public static final float JET_BURN = 1.3f;
    /** Spacing of the flames along a jet. */
    private static final float JET_STEP = 0.42f;
    private static final Color ACID = Color.valueOf("8ae83a");
    private static final Color WARN = Color.valueOf("ff4d4d");
    private static final Color PIPE = Color.valueOf("6a6a74");
    private static final Color WATER = Color.valueOf("226877");
    private static final Color FOAM = Color.valueOf("a6e2cf");
    private static final Color CURB = Color.valueOf("818692");
    private static final float MOTION_WARN = 1f;
    private static final float CART_SPEED = 6.5f;
    private static final float CART_LENGTH = 1.2f;
    private static final float CART_WIDTH = 1.35f;
    private static final float SURGE_TIME = 1.6f;

    /** A hazard about to hit: an acid pool at (x, y, radius), or a jet along a line (vertical: at x = at). */
    static final class Warning {
        float x;
        float y;
        float radius;
        boolean jet;
        boolean vertical;
        float at;
        float time;
    }

    public final Kind kind;
    private final Arena arena;
    private final boolean alongX;
    private final float channelHalfWidth;
    private final Rectangle[] islands;
    private final Rectangle cartSweep = new Rectangle();
    private float timeLeft = DURATION;
    private float next = 1.2f; // a moment to take in the room first
    private final Array<Warning> warnings = new Array<>();
    private final Array<Puddle> spilled = new Array<>();
    private float clock;
    private boolean over;
    private int direction = -1;
    private float motionTime;
    private boolean movingHazard;
    private float cartAt;
    private float cartLane;

    public TrapRoom(Kind kind, Arena arena) {
        this.kind = kind;
        this.arena = arena;
        alongX = arena.right - arena.left >= arena.top - arena.bottom;
        channelHalfWidth = (alongX ? arena.top - arena.bottom : arena.right - arena.left) * 0.32f;
        islands = kind == Kind.SEWER_BACKFLOW ? currentIslands(arena) : null;
    }

    static Rectangle[] currentIslands(Arena arena) {
        float width = arena.right - arena.left;
        float height = arena.top - arena.bottom;
        float dx = width >= height ? width * 0.23f : 0f;
        float dy = width >= height ? 0f : height * 0.23f;
        return new Rectangle[]{
                new Rectangle(arena.centerX - dx - 0.45f, arena.centerY - dy - 0.4f, 0.9f, 0.8f),
                new Rectangle(arena.centerX + dx - 0.45f, arena.centerY + dy - 0.4f, 0.9f, 0.8f)
        };
    }

    public float timeLeft() {
        return Math.max(0f, timeLeft);
    }

    public boolean isOver() {
        return over;
    }

    /** How far along it is, 0 to 1 (it speeds up as it goes). */
    private float progress() {
        return MathUtils.clamp(1f - timeLeft / DURATION, 0f, 1f);
    }

    /** One step; {@code px, py} is where the hero's feet are (many hazards come at them). */
    public void update(float delta, float px, float py, Hazards world) {
        if (over) {
            return;
        }
        clock += delta;
        timeLeft -= delta;
        if (kind == Kind.GARBAGE_CART || kind == Kind.SEWER_BACKFLOW) {
            if (timeLeft <= 0f) {
                over = true;
                movingHazard = false;
            } else {
                updateMotion(delta, world);
            }
            return;
        }
        if (kind == Kind.MANHOLES) {
            over = timeLeft <= 0f;
            return;
        }
        for (int i = warnings.size - 1; i >= 0; i--) {
            Warning w = warnings.get(i);
            w.time -= delta;
            if (w.jet && w.time > 0 && MathUtils.random() < 6f * delta) { // the pipe hisses before it bursts
                world.steam(w.vertical ? w.at : arena.left + 0.1f, w.vertical ? arena.top - 0.1f : w.at);
            }
            if (w.time <= 0) {
                warnings.removeIndex(i);
                strike(w, world);
            }
        }
        if (timeLeft <= 0) { // survived: everything it spilled dries up
            over = true;
            warnings.clear();
            for (Puddle p : spilled) {
                p.life = Math.min(p.life, 0.8f);
            }
            spilled.clear();
            return;
        }
        next -= delta;
        if (next <= 0) {
            float k = progress();
            if (kind == Kind.ACID_FLOOD) {
                next = MathUtils.lerp(0.75f, 0.28f, k);
                warnAcid(px, py);
                if (k > 0.6f && MathUtils.random() < 0.4f) { // near the end it wells up two at a time
                    warnAcid(px, py);
                }
            } else {
                next = MathUtils.lerp(1.25f, 0.6f, k);
                warnJet(px, py, k);
                if (k > 0.3f && MathUtils.random() < 0.6f) { // then a second jet, somewhere else
                    warnJet(MathUtils.random(arena.left, arena.right), MathUtils.random(arena.bottom, arena.top), k);
                }
                if (k > 0.7f && MathUtils.random() < 0.4f) { // and near the end a third
                    warnJet(MathUtils.random(arena.left, arena.right), MathUtils.random(arena.bottom, arena.top), k);
                }
            }
        }
    }

    private float start() {
        return alongX ? arena.left : arena.bottom;
    }

    private float end() {
        return alongX ? arena.right : arena.top;
    }

    private float crossCenter() {
        return alongX ? arena.centerY : arena.centerX;
    }

    private void updateMotion(float delta, Hazards world) {
        if (!movingHazard) {
            next -= delta;
            if (next > 0f) return;
            direction = -direction;
            motionTime = -MOTION_WARN;
            movingHazard = true;
            cartAt = direction > 0 ? start() + CART_LENGTH / 2f : end() - CART_LENGTH / 2f;
            cartLane = crossCenter() + (progress() > 0.3f ? direction * 0.55f : 0f);
            world.sound(kind == Kind.GARBAGE_CART ? "rattle" : "hiss", 0.85f);
            return;
        }
        float previous = motionTime;
        motionTime += delta;
        if (motionTime <= 0f) return;
        if (previous <= 0f) world.sound(kind == Kind.GARBAGE_CART ? "door" : "swish", 0.85f);
        float activeDelta = motionTime - Math.max(0f, previous);
        if (kind == Kind.GARBAGE_CART) {
            float before = cartAt;
            float stop = direction > 0 ? end() + CART_LENGTH / 2f : start() - CART_LENGTH / 2f;
            cartAt = MathUtils.clamp(cartAt + direction * CART_SPEED * activeDelta,
                    start() - CART_LENGTH / 2f, end() + CART_LENGTH / 2f);
            setStrip(cartSweep, Math.min(before, cartAt) - CART_LENGTH / 2f, cartLane - CART_WIDTH / 2f,
                    Math.abs(cartAt - before) + CART_LENGTH, CART_WIDTH);
            world.cart(cartSweep);
            if (cartAt == stop) finishMotion();
        } else {
            float push = direction * MathUtils.lerp(2.2f, 3.4f, progress()) * Math.min(activeDelta, SURGE_TIME - Math.max(0f, previous));
            world.current(alongX ? push : 0f, alongX ? 0f : push);
            if (motionTime >= SURGE_TIME) finishMotion();
        }
    }

    private void finishMotion() {
        movingHazard = false;
        next = MathUtils.lerp(1.8f, 0.75f, progress());
    }

    boolean inCurrent(float x, float y) {
        for (Rectangle island : islands) {
            if (island.contains(x, y)) return false;
        }
        return Math.abs((alongX ? y : x) - crossCenter()) < channelHalfWidth;
    }

    private void setStrip(Rectangle out, float along, float across, float length, float width) {
        if (alongX) out.set(along, across, length, width);
        else out.set(across, along, width, length);
    }

    private void warnAcid(float px, float py) {
        Warning w = new Warning();
        w.radius = MathUtils.random(0.5f, 0.8f);
        if (MathUtils.random() < 0.65f) { // right where you stand, more or less
            w.x = px + MathUtils.random(-0.7f, 0.7f);
            w.y = py + MathUtils.random(-0.5f, 0.5f);
        } else {
            w.x = MathUtils.random(arena.left, arena.right);
            w.y = MathUtils.random(arena.bottom, arena.top);
        }
        w.x = MathUtils.clamp(w.x, arena.left + 0.3f, arena.right - 0.3f);
        w.y = MathUtils.clamp(w.y, arena.bottom + 0.3f, arena.top - 0.4f);
        w.time = ACID_WARN;
        warnings.add(w);
    }

    /**
     * A jet across the short way of the room (a long left-right alley gets up-down jets); a roomy courtyard
     * gets either. Most are lined up on the hero; in the second half ({@code k} past 0.5) the odd one runs
     * the long way, down the whole alley.
     */
    void warnJet(float px, float py, float k) {
        Warning w = new Warning();
        w.jet = true;
        float wide = arena.right - arena.left;
        float tall = arena.top - arena.bottom;
        w.vertical = wide > tall * 1.4f || (tall <= wide * 1.4f && MathUtils.randomBoolean());
        if (k > 0.5f && MathUtils.random() < 0.25f) {
            w.vertical = !w.vertical;
        }
        boolean onHero = MathUtils.random() < 0.65f;
        if (w.vertical) {
            w.at = onHero ? px + MathUtils.random(-0.2f, 0.2f) : MathUtils.random(arena.left, arena.right);
            w.at = MathUtils.clamp(w.at, arena.left + 0.3f, arena.right - 0.3f);
        } else {
            w.at = onHero ? py + MathUtils.random(-0.2f, 0.2f) : MathUtils.random(arena.bottom, arena.top);
            w.at = MathUtils.clamp(w.at, arena.bottom + 0.3f, arena.top - 0.4f);
        }
        w.time = JET_WARN;
        warnings.add(w);
    }

    private void strike(Warning w, Hazards world) {
        if (!w.jet) {
            Puddle p = world.spill(Puddle.Kind.ACID, w.x, w.y, w.radius, timeLeft + 1.5f);
            if (p != null) spilled.add(p);
            world.sound("pop", 0.5f);
            return;
        }
        float from = w.vertical ? arena.bottom + 0.2f : arena.left + 0.2f;
        float to = w.vertical ? arena.top - 0.2f : arena.right - 0.2f;
        for (float s = from; s <= to; s += JET_STEP) {
            Puddle p = w.vertical ? world.spill(Puddle.Kind.FIRE, w.at, s, 0.3f, JET_BURN)
                    : world.spill(Puddle.Kind.FIRE, s, w.at, 0.3f, JET_BURN);
            if (p != null) spilled.add(p);
        }
        world.sound("boom", 0.45f);
    }

    /** The warnings, on the floor: bubbling rings where acid wells up, a red line and a pipe end for a jet. */
    public void drawFloor(SpriteBatch b, Draw d) {
        for (Warning w : warnings) {
            if (!w.jet) {
                float k = 1f - w.time / ACID_WARN;
                d.ellipse(b, w.x, w.y, w.radius * k, w.radius * 0.75f * k, ACID, 0.25f);
                d.ring(b, w.x, w.y, w.radius, ACID, 0.3f + 0.4f * k);
                for (int i = 0; i < 3; i++) {
                    float a = clock * 2f + i * 2.1f;
                    float t = (clock * 1.5f + i * 0.33f) % 1f;
                    d.circle(b, w.x + MathUtils.cos(a) * w.radius * 0.5f, w.y + MathUtils.sin(a) * w.radius * 0.35f,
                            0.02f + 0.03f * t, ACID, 0.8f * (1f - t));
                }
                continue;
            }
            float k = 1f - w.time / JET_WARN;
            float blink = 0.25f + 0.45f * k * (0.6f + 0.4f * MathUtils.sin(clock * 30f));
            if (w.vertical) {
                for (float y = arena.bottom; y < arena.top; y += 0.3f) {
                    d.rect(b, w.at - 0.03f, y, 0.06f, 0.16f, WARN, blink);
                }
                d.rect(b, w.at - 0.1f, arena.top - 0.12f, 0.2f, 0.12f, PIPE, 1f); // the pipe end in the top wall
            } else {
                for (float x = arena.left; x < arena.right; x += 0.3f) {
                    d.rect(b, x, w.at - 0.03f, 0.16f, 0.06f, WARN, blink);
                }
                d.rect(b, arena.left, w.at - 0.1f, 0.12f, 0.2f, PIPE, 1f); // the pipe end in the side wall
            }
        }
    }

    void drawChannel(SpriteBatch b, Draw d) {
        float from = start();
        float length = end() - from;
        float edge = crossCenter() - channelHalfWidth;
        boolean surging = movingHazard && motionTime > 0f;
        float alpha = surging ? 0.56f : 0.16f;
        if (alongX) {
            d.rect(b, from, edge, length, 2f * channelHalfWidth, WATER, alpha);
            d.rect(b, from, edge - 0.1f, length, 0.1f, CURB, 0.75f);
            d.rect(b, from, edge + 2f * channelHalfWidth, length, 0.1f, CURB, 0.75f);
        } else {
            d.rect(b, edge, from, 2f * channelHalfWidth, length, WATER, alpha);
            d.rect(b, edge - 0.1f, from, 0.1f, length, CURB, 0.75f);
            d.rect(b, edge + 2f * channelHalfWidth, from, 0.1f, length, CURB, 0.75f);
        }
        if (movingHazard) {
            for (int i = 0; i < 24; i++) {
                float phase = (clock * (surging ? 1.75f : 0.9f) + i * 0.618f) % 1f;
                float travel = surging ? length - 0.3f : 0.55f;
                float at = direction > 0 ? from + 0.15f + phase * travel : end() - 0.15f - phase * travel;
                float cross = edge + 0.15f + (2f * channelHalfWidth - 0.3f) * ((i * 0.381f) % 1f);
                float radius = 0.035f + 0.025f * phase;
                d.ellipse(b, alongX ? at : cross, alongX ? cross : at,
                        alongX && surging ? 3f * radius : radius, !alongX && surging ? 3f * radius : radius,
                        FOAM, 0.8f * (1f - phase));
            }
        }
        for (Rectangle island : islands) {
            d.rect(b, island.x, island.y, island.width, island.height, CURB, 1f);
            d.rect(b, island.x + 0.06f, island.y + 0.12f, island.width - 0.12f, island.height - 0.18f, PIPE, 1f);
        }
    }

    boolean cartVisible() {
        return kind == Kind.GARBAGE_CART && movingHazard;
    }

    float cartY() {
        return alongX ? cartLane - CART_WIDTH / 2f : cartAt - CART_LENGTH / 2f;
    }

    void drawCart(SpriteBatch b, Sprites sprites) {
        float jitter = motionTime < 0f ? 0.05f * MathUtils.sin(clock * 48f) : 0f;
        float x = alongX ? cartAt : cartLane + jitter;
        float y = alongX ? cartLane + jitter : cartAt;
        float rotation = alongX ? 0f : 90f;
        if (direction < 0) rotation += 180f;
        b.draw(sprites.prop("trap_cart"), x - CART_LENGTH / 2f, y - CART_WIDTH / 2f,
                CART_LENGTH / 2f, CART_WIDTH / 2f, CART_LENGTH, CART_WIDTH, 1f, 1f, rotation);
    }
}
