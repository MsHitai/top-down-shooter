package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * One of the secret sea's bosses once the Fluffy Shark has been beaten (the owner's idea): the Gorgon, a
 * sea witch with a serpent's tail and a head of snakes. Her fight is about where you look.
 * <ul>
 *   <li>the stare: her eyes light up and "LOOK AWAY!" shows over her; when they flash, if you're in her
 *       gaze and aiming at her ({@link #lookingAt}), you're turned to stone for a moment
 *       ({@link Player#petrify}) - aim (and so look) away, or roll, when they flash; a ring of stone chips
 *       goes off either way;</li>
 *   <li>the hiss: her snakes spit at you one after another, wobbly green venom;</li>
 *   <li>the stone ring: a ring of stones flies out, slows to a stop, hangs - then every stone darts at you;</li>
 *   <li>enraged (below half): the snakes spin and pour out a spiral, the stare comes more often and the
 *       stone ring is thicker.</li>
 * </ul>
 */
public class Gorgon extends Enemy {

    private enum Phase { SWIM, HISS, STONES, STARE, SPIRAL }

    private static final Color SKIN = Color.valueOf("a8d89a");
    private static final Color SKIN_DARK = Color.valueOf("6aa070");
    private static final Color SCALES = Color.valueOf("3e8a6a");
    private static final Color SCALES_DARK = Color.valueOf("24604a");
    private static final Color BELLY = Color.valueOf("e8dca0");
    private static final Color SNAKE = Color.valueOf("4fae5a");
    private static final Color SNAKE_DARK = Color.valueOf("2e6e3a");
    private static final Color LIPS = Color.valueOf("8a3a8a");
    private static final Color PEARL = Color.valueOf("f8f4ff");
    private static final Color GLARE = Color.valueOf("fff36a");
    private static final Color VENOM = Color.valueOf("8aff5a");
    private static final Color STONE = Color.valueOf("a8a49a");
    private static final Color RAGE = Color.valueOf("ff3b3b");

    static final int SNAKES = 6;
    /** How long her eyes glow before they flash. */
    static final float STARE_WARN = 1.4f;
    /** The gaze is a cone this wide either side of where she looks. */
    static final float GAZE_HALF = 28f;
    /** Aiming within this of her counts as looking at her. */
    static final float LOOK_HALF = 75f;
    static final float STONE_TIME = 1.3f;
    /** The stone ring drifts out this long, then hangs this long before the stones dart at you. */
    static final float DRIFT = 0.9f;
    static final float HANG = 0.6f;

    private Phase phase = Phase.SWIM;
    private float phaseTimer = 2f;
    private int patternIndex;
    private boolean enraged;
    private float time;
    private float orbit = MathUtils.random(360f);
    private float orbitDir = MathUtils.randomSign();
    private final Vector2 target = new Vector2();
    private final Vector2 step = new Vector2();
    private int shotsLeft;
    private float shotTimer;
    private int snake;
    /** Which snake just spat (it lunges forward a little), and how long ago. */
    private int lunging = -1;
    private float lungeTime;
    private float gaze;
    private float flash;
    private final Array<Bullet> stones = new Array<>(false, 16);
    private float stoneTime;
    private float spin;

    public Gorgon(float x, float y, int tier) {
        super(x, y, 1150f + 230f * tier);
        radius = 0.6f;
        speed = 1.2f;
        score = 950 + 250 * tier;
        boss = true;
        flying = true; // she swims over the coral
    }

    @Override
    public String bossName() {
        return Boss.GORGON.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    @Override
    public float feetY() {
        return pos.y - 0.6f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        knock.setZero();
        flash = Math.max(0f, flash - delta * 3f);
        lungeTime += delta;
        if (!enraged && hp < maxHp * 0.5f) {
            enraged = true;
            speed = 1.5f;
            world.shake(0.7f);
            world.sfx().play("hiss", 1f);
            world.particles().burst(pos.x, pos.y + 0.4f, 24, 0.5f, 2f, 0.6f, 0.05f, VENOM);
        }
        float diff = world.getDifficulty();
        phaseTimer -= delta;
        updateStones(delta, world, diff);
        switch (phase) {
            case SWIM:
                swim(delta, world, 1f);
                if (phaseTimer <= 0 && canShoot(world)) nextPattern(world);
                break;
            case HISS:
                swim(delta, world, 0.5f);
                shotTimer -= delta;
                if (shotsLeft > 0 && shotTimer <= 0) {
                    shotTimer = enraged ? 0.1f : 0.13f;
                    shotsLeft--;
                    spit(world, diff);
                }
                if (shotsLeft <= 0 && shotTimer <= 0) toSwim();
                break;
            case STONES:
                if (phaseTimer <= 0) {
                    stoneRing(world, diff);
                    toSwim();
                }
                break;
            case STARE:
                if (phaseTimer > 0.3f) gaze = aimAngle; // (locks just before the flash)
                if (phaseTimer <= 0) {
                    stareFlash(world, diff);
                    toSwim();
                }
                break;
            case SPIRAL:
                spin += delta * 110f;
                shotTimer -= delta;
                if (shotTimer <= 0) {
                    shotTimer = 0.14f;
                    for (int i = 0; i < SNAKES; i++) {
                        float a = spin + i * 360f / SNAKES;
                        Bullet b = world.spawnEnemyBullet(snakeX(i), snakeY(i), a, 2.0f * diff);
                        b.shape = Bullet.Shape.DROP;
                        b.color.set(VENOM);
                    }
                }
                if (phaseTimer <= 0) toSwim();
                break;
        }
    }

    /** Circles the hero at a distance, swaying like a sea snake. */
    private void swim(float delta, GameWorld world, float pace) {
        Player p = world.getPlayer();
        orbit += orbitDir * 22f * pace * delta;
        if (MathUtils.random() < delta * 0.2f) orbitDir = -orbitDir;
        float dist = enraged ? 2.6f : 3f;
        target.set(p.centerX() + MathUtils.cosDeg(orbit) * dist, p.hitboxY() + 0.4f + MathUtils.sinDeg(orbit) * dist * 0.6f);
        Arena.now.clamp(target, radius + 0.5f);
        step.set(target).sub(pos);
        float len = step.len();
        if (len > 0.02f) {
            step.scl(Math.min(len, speed * pace * delta) / len);
            pos.add(step);
        }
    }

    private void toSwim() {
        phase = Phase.SWIM;
        phaseTimer = enraged ? 0.8f : 1.2f;
    }

    private void nextPattern(GameWorld world) {
        Phase[] order = enraged
                ? new Phase[]{Phase.STARE, Phase.SPIRAL, Phase.STONES, Phase.HISS, Phase.STARE, Phase.HISS}
                : new Phase[]{Phase.HISS, Phase.STONES, Phase.STARE, Phase.HISS, Phase.STONES};
        phase = order[patternIndex++ % order.length];
        switch (phase) {
            case HISS:
                shotsLeft = enraged ? 16 : 12;
                shotTimer = 0.2f;
                world.sfx().play("hiss", 0.6f);
                break;
            case STONES:
                phaseTimer = 0.4f;
                break;
            case STARE:
                phaseTimer = STARE_WARN;
                gaze = aimAngle;
                world.sfx().play("buzz", 0.7f);
                break;
            default: // SPIRAL
                phaseTimer = 2.6f;
                shotTimer = 0.3f;
                spin = aimAngle;
                world.sfx().play("hiss", 0.9f);
                break;
        }
    }

    /** The next snake spits a wobbly blob of venom at the hero. */
    private void spit(GameWorld world, float diff) {
        snake = (snake + 1) % SNAKES;
        lunging = snake;
        lungeTime = 0f;
        Player p = world.getPlayer();
        float sx = snakeX(snake);
        float sy = snakeY(snake);
        float a = MathUtils.atan2Deg360(p.hitboxY() - sy, p.centerX() - sx);
        Bullet b = world.spawnEnemyBullet(sx, sy, a + MathUtils.random(-6f, 6f), 2.6f * diff);
        b.shape = Bullet.Shape.DROP;
        b.color.set(VENOM);
        b.wobble = 60f;
        world.sfx().play("enemy_shoot", 0.4f);
    }

    /** Stones fly out in a ring, slow to a stop and hang (then {@link #updateStones} sends them at you). */
    private void stoneRing(GameWorld world, float diff) {
        int n = enraged ? 16 : 12;
        float turn = MathUtils.random(360f);
        for (int i = 0; i < n; i++) {
            Bullet b = world.spawnEnemyBullet(pos.x, pos.y, turn + i * 360f / n, 2.2f);
            b.accel = -2.2f / DRIFT;
            b.minSpeed = 0.02f;
            b.shape = Bullet.Shape.SQUARE;
            b.color.set(STONE);
            b.life = DRIFT + HANG + 0.3f;
            stones.add(b);
        }
        stoneTime = 0f;
        world.sfx().play("break", 0.6f);
    }

    private void updateStones(float delta, GameWorld world, float diff) {
        if (stones.size == 0) return;
        stoneTime += delta;
        if (stoneTime < DRIFT + HANG) return;
        Player p = world.getPlayer();
        for (Bullet s : stones) {
            if (!s.alive) continue;
            s.vel.set(p.centerX() - s.pos.x, p.hitboxY() - s.pos.y).setLength(0.6f);
            s.accel = 3.4f * diff;
            s.maxSpeed = 3.0f * diff;
            s.life = 6f;
        }
        stones.clear();
        world.sfx().play("swish", 0.5f);
    }

    /** Her eyes flash: whoever's in her gaze and looking at her turns to stone; stone chips fly either way. */
    private void stareFlash(GameWorld world, float diff) {
        flash = 1f;
        world.sfx().play("zap", 0.9f);
        Player p = world.getPlayer();
        if (petrifies(pos.x, eyeY(), gaze, p.centerX(), p.hitboxY(), p.getAimAngle())
                && !p.isRolling() && !p.isDead() && !p.isInvulnerable()) {
            p.petrify(STONE_TIME);
            world.toast("TURNED TO STONE!");
            world.particles().burst(p.centerX(), p.hitboxY(), 16, 0.3f, 1.4f, 0.5f, 0.05f, STONE);
        }
        int n = enraged ? 14 : 10;
        for (int i = 0; i < n; i++) {
            Bullet b = world.spawnEnemyBullet(pos.x, eyeY(), gaze + 180f / n + i * 360f / n, 1.7f * diff);
            b.shape = Bullet.Shape.SQUARE;
            b.color.set(STONE);
        }
    }

    /**
     * The stare turns the hero to stone: they're inside her gaze (a cone {@link #GAZE_HALF} either side of
     * {@code gaze}, from her eyes at (ex, ey)) and they're looking at her - aiming within {@link #LOOK_HALF}
     * of the way to her.
     */
    static boolean petrifies(float ex, float ey, float gaze, float px, float py, float playerAim) {
        float toPlayer = MathUtils.atan2Deg360(py - ey, px - ex);
        float toHer = toPlayer + 180f;
        return inCone(toPlayer, gaze, GAZE_HALF) && lookingAt(playerAim, toHer);
    }

    static boolean lookingAt(float playerAim, float toHer) {
        return inCone(playerAim, toHer, LOOK_HALF);
    }

    private static boolean inCone(float angle, float centre, float half) {
        float diff = ((angle - centre) % 360f + 540f) % 360f - 180f;
        return Math.abs(diff) <= half;
    }

    /** Her eyes, where she's drawn ({@link #SIZE} times her pieces' layout, grown from under her tail). */
    private float eyeY() {
        return grownY(headY());
    }

    /** Her face in the layout she's drawn from, before {@link #SIZE}. */
    private float headY() {
        return pos.y + 0.42f;
    }

    private float grownY(float y) {
        return pos.y - 0.6f + (y - (pos.y - 0.6f)) * SIZE;
    }

    /** Where snake {@code i}'s head is: a crown of them fanned round the top of her head. */
    private float snakeX(int i) {
        float a = 20f + i * 140f / (SNAKES - 1);
        return pos.x + MathUtils.cosDeg(a) * 0.36f * SIZE;
    }

    private float snakeY(int i) {
        float a = 20f + i * 140f / (SNAKES - 1);
        return grownY(headY() + 0.05f + MathUtils.sinDeg(a) * 0.3f);
    }

    // ------------------------------------------------------------ drawing

    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (phase == Phase.STARE) { // her gaze, brightening on the floor
            float k = 1f - Math.max(0f, phaseTimer) / STARE_WARN;
            for (int i = 0; i < 7; i++) {
                float a = gaze - GAZE_HALF + i * GAZE_HALF * 2f / 6f;
                d.line(b, pos.x, eyeY(), 6f, 0.05f, a, GLARE, 0.06f + 0.14f * k);
            }
        }
    }

    @Override
    public void drawOverlay(SpriteBatch b, Draw d, Rectangle view) {
        if (flash > 0) {
            d.circle(b, pos.x, eyeY(), 0.6f + 0.6f * (1f - flash), GLARE, 0.4f * flash);
        }
    }

    /** "LOOK AWAY!" over her while her eyes glow - so it's clear what to do. */
    public boolean staring() {
        return phase == Phase.STARE;
    }

    /** She's drawn this much bigger than her pieces are laid out (the card shows her at her own size). */
    private static final float SIZE = 1.4f;
    private final Matrix4 saved = new Matrix4();
    private final Matrix4 grown = new Matrix4();

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        saved.set(b.getTransformMatrix());
        grown.set(saved).translate(pos.x, pos.y - 0.6f, 0).scale(SIZE, SIZE, 1).translate(-pos.x, -(pos.y - 0.6f), 0);
        b.setTransformMatrix(grown);
        drawGorgon(b, d);
        b.setTransformMatrix(saved);
    }

    @Override
    public void drawPortrait(SpriteBatch b, Draw d, float aim) {
        aimAngle = aim;
        drawGorgon(b, d);
    }

    private void drawGorgon(SpriteBatch b, Draw d) {
        float x = pos.x;
        float y = pos.y + MathUtils.sin(time * 2f) * 0.04f;
        // the serpent tail, coiling under her
        for (int k = 9; k >= 0; k--) {
            float f = k / 9f;
            float tx = x + MathUtils.sin(time * 2.5f + f * 5f) * 0.25f * f + f * 0.2f;
            float ty = y - 0.15f - f * 0.55f;
            float r = 0.2f - f * 0.12f;
            d.circle(b, tx, ty, r + 0.025f, Draw.OUTLINE_COLOR);
            d.circle(b, tx, ty, r, c(k % 2 == 0 ? SCALES : SCALES_DARK));
            d.circle(b, tx - r * 0.2f, ty - r * 0.3f, r * 0.45f, c(BELLY), 0.8f);
        }
        // her body and arms, a pearl necklace
        d.ellipse(b, x, y + 0.08f, 0.2f, 0.24f, Draw.OUTLINE_COLOR, 1f);
        d.ellipse(b, x, y + 0.08f, 0.18f, 0.22f, c(SKIN), 1f);
        for (int side = -1; side <= 1; side += 2) {
            d.line(b, x + side * 0.15f, y + 0.18f, 0.24f, 0.08f, side > 0 ? -40f : 220f, Draw.OUTLINE_COLOR);
            d.line(b, x + side * 0.15f, y + 0.18f, 0.22f, 0.055f, side > 0 ? -40f : 220f, c(SKIN));
        }
        for (int i = 0; i < 5; i++) {
            d.circle(b, x + (i - 2f) * 0.06f, y + 0.24f - Math.abs(i - 2f) * 0.02f, 0.025f, PEARL);
        }
        // the snakes: each a wiggling neck and a little head, the one that just spat lunging forward
        for (int i = 0; i < SNAKES; i++) {
            float a = 20f + i * 140f / (SNAKES - 1) + (phase == Phase.SPIRAL ? MathUtils.sin(time * 12f + i) * 10f : 0f);
            float lunge = i == lunging ? Math.max(0f, 1f - lungeTime * 6f) * 0.08f : 0f;
            float sx = x;
            float sy = headY() + 0.05f;
            for (int k = 0; k < 4; k++) {
                float wig = MathUtils.sin(time * 6f + i * 1.3f + k) * 14f;
                sx += MathUtils.cosDeg(a + wig) * (0.1f + (k == 3 ? lunge : 0f));
                sy += MathUtils.sinDeg(a + wig) * (0.08f + (k == 3 ? lunge : 0f));
                d.circle(b, sx, sy, 0.05f, Draw.OUTLINE_COLOR);
                d.circle(b, sx, sy, 0.037f, c(k % 2 == 0 ? SNAKE : SNAKE_DARK));
            }
            d.circle(b, sx, sy, 0.065f, Draw.OUTLINE_COLOR);
            d.circle(b, sx, sy, 0.052f, c(SNAKE));
            d.circle(b, sx - 0.015f, sy + 0.015f, 0.014f, enraged ? RAGE : GLARE);
            if (i == lunging && lungeTime < 0.15f) {
                d.line(b, sx, sy, 0.08f, 0.015f, a, RAGE); // a flick of its tongue
            }
        }
        // her face
        float hy = headY();
        d.circle(b, x, hy, 0.22f, Draw.OUTLINE_COLOR);
        d.circle(b, x, hy, 0.2f, c(SKIN));
        d.circle(b, x - 0.07f, hy + 0.08f, 0.05f, Color.WHITE, 0.25f);
        boolean glowing = phase == Phase.STARE;
        float glow = glowing ? 1f - Math.max(0f, phaseTimer) / STARE_WARN : 0f;
        for (int side = -1; side <= 1; side += 2) {
            float ex = x + side * 0.08f;
            if (glowing) d.circle(b, ex, hy + 0.01f, 0.06f + 0.06f * glow, GLARE, 0.3f + 0.4f * glow);
            d.ellipse(b, ex, hy + 0.01f, 0.05f, 0.035f, Draw.OUTLINE_COLOR, 1f);
            d.ellipse(b, ex, hy + 0.01f, 0.04f, 0.026f, glowing ? GLARE : Color.WHITE, 1f);
            float lx = MathUtils.cosDeg(aimAngle) * 0.012f;
            d.rect(b, ex + lx - 0.006f, hy - 0.012f, 0.012f, 0.044f, glowing ? RAGE : SCALES_DARK);
            d.rotRect(b, ex, hy + 0.07f, 0.08f, 0.02f, side * (enraged ? -20f : -8f), SKIN_DARK, 1f);
        }
        d.ellipse(b, x, hy - 0.1f, 0.06f, 0.025f, LIPS, 1f); // a cold little smile
    }
}
