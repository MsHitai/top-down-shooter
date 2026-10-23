package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.objects.Prop;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * City boss: the Pigeon King, a fat pigeon with a bottle-cap crown, perched on a statue pedestal (a
 * real prop: it blocks bullets and gives some cover).
 * <ul>
 *   <li>flock: a line of pigeons swoops across the room from one wall, with a couple of gaps
 *       (red dots on that wall warn where they come from);</li>
 *   <li>breadcrumbs: crumbs roll to a stop around the player, then turn into pigeons pecking at them;</li>
 *   <li>feather fans: curving volleys of feathers.</li>
 * </ul>
 * Enraged: he leaves the statue and flaps around, and flocks come from both sides at once.
 */
public class PigeonKing extends Enemy {

    private enum Phase { PERCH, FLOCK_WARN, CRUMBS, FEATHERS }

    private static final Color FEATHER = Color.valueOf("8a92a8");
    private static final Color FEATHER_DARK = Color.valueOf("5e667a");
    private static final Color NECK_GREEN = Color.valueOf("4fa07a");
    private static final Color NECK_PURPLE = Color.valueOf("8a5ab0");
    private static final Color BELLY = Color.valueOf("b8bfd0");
    private static final Color BEAK = Color.valueOf("e0a040");
    private static final Color FEET = Color.valueOf("e07a5a");
    private static final Color CAP = Color.valueOf("d63a2f");
    private static final Color CAP_EDGE = Color.valueOf("c9ccd6");
    private static final Color CRUMB = Color.valueOf("d9b878");
    private static final Color PIGEON = Color.valueOf("9aa2b4");

    private static final float FLOCK_WARN = 0.7f;
    /** Crumbs lie on the floor this long before they turn into pigeons. */
    private static final float CRUMB_WAIT = 1.3f;

    private Phase phase = Phase.PERCH;
    private float phaseTimer = 1.8f;
    private float shotTimer;
    private int shotsLeft;
    private int patternIndex;
    private boolean enraged;
    private boolean onStatue = true;
    private float time;
    private Prop statue;
    private final Vector2 target = new Vector2();
    private final Vector2 step = new Vector2();
    /** Which walls the coming flock enters from (-1 left, 1 right, 0 both) and where its gaps are. */
    private int flockSide;
    private final float[] gaps = new float[2];
    /** Crumbs on the floor, with how long each has been there. */
    private final Array<Bullet> crumbs = new Array<>();
    private final FloatArray crumbAge = new FloatArray();

    public PigeonKing(float x, float y, int tier) {
        super(x, y, 540f + 160f * tier);
        radius = 0.45f;
        speed = 1.4f;
        score = 500 + 250 * tier;
        boss = true;
        flying = true; // he's a bird: perched on his statue, then flapping about
        // wherever he's summoned, his statue stands just above the middle of the arena
        pos.set(Arena.now.centerX, Arena.now.centerY + 0.85f);
    }

    @Override
    public String bossName() {
        return Boss.PIGEON_KING.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    /** On the statue his feet are just in front of its top; flying, his shadow is below him. */
    @Override
    public float feetY() {
        return onStatue && statue != null ? statue.solid.y - 0.01f : pos.y - 0.6f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        if (statue == null) { // the first time he acts: put his statue under him
            statue = new Prop(Prop.Kind.PEDESTAL, pos.x, pos.y - 0.55f);
            world.addProp(statue);
        }
        if (!enraged && hp < maxHp * 0.5f) {
            enraged = true;
            onStatue = false; // off the statue, flapping around the arena
            target.set(pos.x, pos.y - 1f);
            world.shake(0.6f);
            world.sfx().play("boss", 1f);
            world.particles().burst(pos.x, pos.y, 20, 0.5f, 2f, 0.5f, 0.05f, FEATHER);
        }
        float diff = world.getDifficulty();
        phaseTimer -= delta;
        shotTimer -= delta;
        updateCrumbs(delta, world, diff);
        if (!onStatue) {
            flap(delta, world);
        }

        switch (phase) {
            case PERCH:
                if (phaseTimer <= 0) {
                    nextPattern(world);
                }
                break;
            case FLOCK_WARN:
                if (phaseTimer <= 0) {
                    flock(world, diff);
                    toPerch();
                }
                break;
            case CRUMBS:
                if (shotTimer <= 0 && shotsLeft > 0) {
                    shotTimer = 0.35f;
                    shotsLeft--;
                    throwCrumbs(world);
                }
                if (shotsLeft <= 0 && shotTimer <= 0) {
                    toPerch();
                }
                break;
            case FEATHERS:
                if (shotTimer <= 0 && shotsLeft > 0) {
                    shotTimer = enraged ? 0.35f : 0.5f;
                    shotsLeft--;
                    int n = enraged ? 11 : 9;
                    float fan = 80f;
                    float bend = (shotsLeft % 2 == 0 ? 1 : -1) * 30f;
                    for (int i = 0; i < n; i++) {
                        Bullet b = shoot(world, aimAngle - fan / 2f + fan * i / (n - 1), 2.4f * diff);
                        b.curve = bend;
                        b.color.set(FEATHER);
                    }
                    world.sfx().play("enemy_shoot", 0.9f);
                }
                if (shotsLeft <= 0 && shotTimer <= 0) {
                    toPerch();
                }
                break;
        }
    }

    /** Enraged: flaps between spots around the arena, keeping some distance from the player. */
    private void flap(float delta, GameWorld world) {
        Player p = world.getPlayer();
        if (pos.dst2(target) < 0.05f || target.dst(p.centerX(), p.hitboxY()) < 2f) {
            for (int i = 0; i < 10; i++) {
                target.set(MathUtils.random(Arena.now.left + 1f, Arena.now.right - 1f),
                        MathUtils.random(Arena.now.bottom + 1.2f, Arena.now.top - 0.6f));
                if (target.dst(p.centerX(), p.hitboxY()) > 2.4f) break;
            }
        }
        step.set(target).sub(pos);
        if (step.len() > 0.02f) {
            step.nor().scl(speed * delta);
            pos.add(step);
        }
    }

    /** A column of pigeons right across the room, entering from a wall, with two gaps to slip through. */
    private void flock(GameWorld world, float diff) {
        Arena a = Arena.now;
        for (int side = -1; side <= 1; side += 2) {
            if (flockSide != 0 && flockSide != side) continue;
            float x = side < 0 ? a.left + 0.08f : a.right - 0.08f;
            float dir = side < 0 ? 0f : 180f;
            float gap = side < 0 ? gaps[0] : gaps[1];
            for (float y = a.bottom + 0.12f; y < a.top - 0.05f; y += 0.26f) {
                if (Math.abs(y - gap) < 0.55f) continue;
                Bullet b = world.spawnEnemyBullet(x, y, dir, 2.0f * diff);
                b.wobble = 40f;
                b.color.set(PIGEON);
            }
        }
        world.sfx().play("dodge", 1f);
    }

    /** Crumbs flung around the player; they slide to a stop (see updateCrumbs). */
    private void throwCrumbs(GameWorld world) {
        Player p = world.getPlayer();
        int n = enraged ? 7 : 5;
        for (int i = 0; i < n; i++) {
            float tx = p.centerX() + MathUtils.random(-1.6f, 1.6f);
            float ty = p.hitboxY() + MathUtils.random(-1.1f, 1.1f);
            float dist = MathUtils.clamp(Vector2.dst(pos.x, pos.y, tx, ty), 0.6f, 6f);
            float decel = 3f;
            Bullet b = shoot(world, MathUtils.atan2Deg360(ty - pos.y, tx - pos.x), (float) Math.sqrt(2f * decel * dist));
            b.accel = -decel;
            b.minSpeed = 0.02f;
            b.radius = 0.075f;
            b.color.set(CRUMB);
            crumbs.add(b);
            crumbAge.add(0f);
        }
        world.sfx().play("pop", 0.6f);
    }

    /** Crumbs that have lain long enough become pigeons pecking straight at the player. */
    private void updateCrumbs(float delta, GameWorld world, float diff) {
        Player p = world.getPlayer();
        for (int i = crumbs.size - 1; i >= 0; i--) {
            Bullet b = crumbs.get(i);
            float age = crumbAge.get(i) + delta;
            crumbAge.set(i, age);
            if (!b.alive) {
                crumbs.removeIndex(i);
                crumbAge.removeIndex(i);
            } else if (age > CRUMB_WAIT) {
                float a = MathUtils.atan2Deg360(p.hitboxY() - b.pos.y, p.centerX() - b.pos.x);
                b.vel.set(MathUtils.cosDeg(a), MathUtils.sinDeg(a)).scl(2.3f * diff);
                b.accel = 0;
                b.radius = 0.12f; // (already restyled as a boss bullet, so set its final size here)
                b.color.set(PIGEON);
                crumbs.removeIndex(i);
                crumbAge.removeIndex(i);
                world.particles().spawn(b.pos.x, b.pos.y, 0, 0.3f, 0.3f, 0.06f, FEATHER, 1f);
            }
        }
    }

    private void toPerch() {
        phase = Phase.PERCH;
        phaseTimer = enraged ? 1.1f : 1.7f;
    }

    private void nextPattern(GameWorld world) {
        Phase[] order = {Phase.FLOCK_WARN, Phase.CRUMBS, Phase.FEATHERS};
        phase = order[patternIndex++ % order.length];
        Arena a = Arena.now;
        switch (phase) {
            case FLOCK_WARN:
                flockSide = enraged ? 0 : MathUtils.randomSign();
                gaps[0] = MathUtils.random(a.bottom + 0.8f, a.top - 0.8f);
                gaps[1] = MathUtils.random(a.bottom + 0.8f, a.top - 0.8f);
                phaseTimer = FLOCK_WARN;
                world.sfx().play("buzz", 0.4f); // the flock flapping in
                break;
            case CRUMBS:
                shotsLeft = enraged ? 3 : 2;
                shotTimer = 0.3f;
                break;
            case FEATHERS:
                shotsLeft = enraged ? 4 : 3;
                shotTimer = 0.4f;
                break;
            default:
                break;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        // warning dots on the wall(s) the flock will come from (the gaps stay dark)
        if (phase == Phase.FLOCK_WARN) {
            Arena a = Arena.now;
            float alpha = 0.3f + 0.5f * (1f - phaseTimer / FLOCK_WARN);
            for (int side = -1; side <= 1; side += 2) {
                if (flockSide != 0 && flockSide != side) continue;
                float x = side < 0 ? a.left + 0.08f : a.right - 0.08f;
                float gap = side < 0 ? gaps[0] : gaps[1];
                for (float y = a.bottom + 0.12f; y < a.top - 0.05f; y += 0.26f) {
                    if (Math.abs(y - gap) < 0.55f) continue;
                    d.circle(b, x, y, 0.05f, TELEGRAPH, alpha);
                }
            }
        }
        float face = facingRight() ? 1 : -1;
        float flapK = onStatue ? MathUtils.sin(time * 3f) * 0.1f : MathUtils.sin(time * 18f);
        float bob = onStatue ? MathUtils.sin(time * 2f) * 0.015f : MathUtils.sin(time * 9f) * 0.04f;
        float cx = pos.x;
        float cy = pos.y + bob;
        // feet (tucked when flying)
        if (onStatue) {
            d.rect(b, cx - 0.14f, cy - 0.42f, 0.08f, 0.06f, FEET);
            d.rect(b, cx + 0.06f, cy - 0.42f, 0.08f, 0.06f, FEET);
        }
        // wings
        for (int i = -1; i <= 1; i += 2) {
            float wy = cy + 0.02f + flapK * 0.12f;
            d.rotRect(b, cx + i * 0.38f, wy, 0.3f, 0.14f, i * (15f + flapK * 25f), c(FEATHER_DARK), 1f);
        }
        // fat body and belly
        d.circle(b, cx, cy - 0.06f, 0.4f, c(FEATHER));
        d.circle(b, cx + face * 0.05f, cy - 0.14f, 0.27f, c(BELLY));
        // shiny neck and head
        d.circle(b, cx + face * 0.12f, cy + 0.22f, 0.2f, c(NECK_GREEN));
        d.circle(b, cx + face * 0.08f, cy + 0.18f, 0.14f, c(NECK_PURPLE));
        d.circle(b, cx + face * 0.16f, cy + 0.34f, 0.15f, c(FEATHER));
        d.rotRect(b, cx + face * 0.31f, cy + 0.32f, 0.1f, 0.05f, face * -15f, BEAK, 1f);
        Color eye = enraged ? Color.RED : Color.valueOf("ff8a3a");
        d.circle(b, cx + face * 0.2f, cy + 0.37f, 0.035f, eye);
        d.circle(b, cx + face * 0.21f, cy + 0.37f, 0.016f, PUPIL);
        // bottle-cap crown
        float crownY = cy + 0.5f;
        d.circle(b, cx + face * 0.14f, crownY, 0.1f, CAP_EDGE);
        d.circle(b, cx + face * 0.14f, crownY, 0.08f, CAP);
        for (int i = 0; i < 6; i++) {
            float a = i * 60f + time * 20f;
            d.circle(b, cx + face * 0.14f + MathUtils.cosDeg(a) * 0.095f, crownY + MathUtils.sinDeg(a) * 0.095f, 0.018f, CAP_EDGE);
        }
    }
}
