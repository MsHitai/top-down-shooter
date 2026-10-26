package com.trush.game.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Puddle;

/**
 * A Slums trap room (see ROADMAP): the doors lock with no enemies at all - just a trap to survive until
 * the clock runs out, then the room counts as cleared. Two kinds:
 * <ul>
 *   <li>the acid rises: pools of acid well up across the floor, faster and faster, often right where you
 *   stand - a bubbling ring marks each one first, so keep moving (or roll across);</li>
 *   <li>the pipes burst: jets of flame shoot across the room from the walls, along the short way across
 *   the alley - a red line and hissing steam warn first, then the line is on fire for a moment (stepping
 *   in sets you alight; a roll is in the air, and puts the flames out).</li>
 * </ul>
 * Hazards are spilled through {@link Hazards} (GameWorld), so the trap's timing can be tested on its own.
 * When it's over, whatever it spilled dries up.
 */
public class TrapRoom {

    public enum Kind {
        ACID_FLOOD("THE ACID RISES!"),
        BURST_PIPES("THE PIPES BURST!");

        public final String banner;

        Kind(String banner) {
            this.banner = banner;
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
    }

    /** How long to survive. */
    public static final float DURATION = 20f;
    /** How long a hazard's warning shows before it hits. */
    public static final float ACID_WARN = 0.9f;
    public static final float JET_WARN = 1.0f;
    /** How long a jet's line burns. */
    public static final float JET_BURN = 1.0f;
    /** Spacing of the flames along a jet. */
    private static final float JET_STEP = 0.42f;
    private static final Color ACID = Color.valueOf("8ae83a");
    private static final Color WARN = Color.valueOf("ff4d4d");
    private static final Color PIPE = Color.valueOf("6a6a74");

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
    private float timeLeft = DURATION;
    private float next = 1.2f; // a moment to take in the room first
    private final Array<Warning> warnings = new Array<>();
    private final Array<Puddle> spilled = new Array<>();
    private float clock;
    private boolean over;

    public TrapRoom(Kind kind, Arena arena) {
        this.kind = kind;
        this.arena = arena;
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
                next = MathUtils.lerp(1.1f, 0.5f, k);
                warnAcid(px, py);
            } else {
                next = MathUtils.lerp(1.7f, 0.95f, k);
                warnJet(px, py);
                if (k > 0.5f && MathUtils.random() < 0.5f) {
                    warnJet(MathUtils.random(arena.left, arena.right), MathUtils.random(arena.bottom, arena.top));
                }
            }
        }
    }

    private void warnAcid(float px, float py) {
        Warning w = new Warning();
        w.radius = MathUtils.random(0.45f, 0.7f);
        if (MathUtils.random() < 0.45f) { // right where you stand, more or less
            w.x = px + MathUtils.random(-0.9f, 0.9f);
            w.y = py + MathUtils.random(-0.6f, 0.6f);
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
     * gets either. Often lined up on the hero.
     */
    void warnJet(float px, float py) {
        Warning w = new Warning();
        w.jet = true;
        float wide = arena.right - arena.left;
        float tall = arena.top - arena.bottom;
        w.vertical = wide > tall * 1.4f || (tall <= wide * 1.4f && MathUtils.randomBoolean());
        boolean onHero = MathUtils.random() < 0.5f;
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
}
