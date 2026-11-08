package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * One of the secret sea's bosses once the Fluffy Shark has been beaten (the owner's idea): the Kraken, a
 * huge red squid with fluffy tufts on its head, wallowing along the top of its lair. Its fight is about
 * where you stand.
 * <ul>
 *   <li>tentacle slams: red ripples open on the floor (the first one where you are) and a moment later a
 *       tentacle bursts up out of each one, throwing a ring of drops - a raised tentacle can be shot, and
 *       that hurts the Kraken ({@link #bodyContains});</li>
 *   <li>the sweep: a red band across the lair at your height, then a long tentacle sweeps along it from wall
 *       to wall shedding drops - step out of the band, or roll over it;</li>
 *   <li>ink bombs: blobs of ink that burst into rings of drops;</li>
 *   <li>enraged (below half): the whirlpool - the water swirls round its beak, pulling you in (slower than
 *       you walk), while the beak pours out a spiral; and the slams come five at a time.</li>
 * </ul>
 */
public class Kraken extends Enemy {

    private enum Phase { WALLOW, SLAM, SWEEP, INK, WHIRL }

    private static final Color SKIN = Color.valueOf("d0504e");
    private static final Color SKIN_DARK = Color.valueOf("8e2c3a");
    private static final Color SKIN_LIGHT = Color.valueOf("f08a7a");
    private static final Color SUCKER = Color.valueOf("ffd6c4");
    private static final Color FLUFF = Color.valueOf("ffb0a8");
    private static final Color IRIS = Color.valueOf("ffd23a");
    private static final Color BEAK = Color.valueOf("3a2a2a");
    private static final Color DROP = Color.valueOf("8ad8ff");
    private static final Color INK = Color.valueOf("2a2440");
    private static final Color FOAM = Color.valueOf("e8faff");
    private static final Color RAGE = Color.valueOf("ff3b3b");

    /** How long a ripple shows before its tentacle bursts up, and how long the tentacle stays up. */
    static final float SLAM_WARN = 1.0f;
    static final float TENTACLE_UP = 1.6f;
    /** A tentacle bursting up hurts within this of its ripple's middle. */
    static final float SLAM_RADIUS = 0.45f;
    private static final float TENTACLE_H = 1.0f;
    static final float SWEEP_WARN = 1.1f;
    static final float SWEEP_SPEED = 7f;
    /** Half the sweeping tentacle's thickness, for hitting the hero. */
    static final float SWEEP_HALF = 0.28f;
    static final float INK_BURST = 0.9f;
    static final float WHIRL_TIME = 3.2f;
    /** The whirlpool pulls this fast (the hero walks faster: it's a drift, not a trap). */
    static final float WHIRL_PULL = 0.9f;

    private Phase phase = Phase.WALLOW;
    private float phaseTimer = 1.6f;
    private int patternIndex;
    private boolean enraged;
    private float time;
    private final Vector2 home = new Vector2();
    private final Array<Tentacle> tentacles = new Array<>();
    private int slamsLeft;
    private float slamTimer;
    // the sweep
    private float sweepY;
    private float sweepFrom;
    private float sweepDir;
    private float sweepTip;
    private boolean sweeping;
    private boolean sweepHit;
    private float sweepDropAt;
    private int sweepsLeft;
    // ink
    private final Array<Bullet> inkBlobs = new Array<>(false, 6);
    private final Array<Float> inkAges = new Array<>(false, 6);
    // the whirlpool
    private float spin;
    private float spiralTimer;
    /** 0 = beak shut, 1 = wide open. */
    private float beak;

    /** A tentacle: first a ripple on the floor ({@code warn} counting down), then raised ({@code up}). */
    static final class Tentacle {
        float x;
        float y;
        float warn = SLAM_WARN;
        float up;
    }

    public Kraken(float x, float y, int tier) {
        super(x, y, 1200f + 240f * tier);
        radius = 0.8f;
        speed = 0.7f;
        score = 950 + 250 * tier;
        boss = true;
        flying = true;
        home.set(x, y);
    }

    @Override
    public String bossName() {
        return Boss.KRAKEN.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    @Override
    public float feetY() {
        return pos.y - 0.7f;
    }

    @Override
    public float viewZoom() {
        return 1.2f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        knock.setZero();
        if (!enraged && hp < maxHp * 0.5f) {
            enraged = true;
            world.shake(0.8f);
            world.sfx().play("boss", 1f);
            world.particles().burst(pos.x, pos.y, 30, 0.6f, 2.4f, 0.7f, 0.06f, FOAM);
        }
        float diff = world.getDifficulty();
        wallow(delta, world);
        updateTentacles(delta, world, diff);
        updateInk(delta, world, diff);
        phaseTimer -= delta;
        beak = Math.max(0f, beak - delta * 2f);
        switch (phase) {
            case WALLOW:
                if (phaseTimer <= 0 && canShoot(world)) nextPattern(world);
                break;
            case SLAM:
                slamTimer -= delta;
                if (slamsLeft > 0 && slamTimer <= 0) {
                    addTentacle(world, slamsLeft == (enraged ? 5 : 3));
                    slamsLeft--;
                    slamTimer = enraged ? 0.25f : 0.35f;
                }
                if (slamsLeft <= 0 && phaseTimer <= 0) toWallow();
                break;
            case SWEEP:
                updateSweep(delta, world, diff);
                break;
            case INK:
                if (phaseTimer <= 0) {
                    squirtInk(world, diff);
                    toWallow();
                }
                break;
            case WHIRL:
                whirl(delta, world, diff);
                break;
        }
    }

    /** Drifts along the top of the lair after the hero, bobbing in the water. */
    private void wallow(float delta, GameWorld world) {
        Arena a = Arena.now;
        float wantX = MathUtils.clamp(world.getPlayer().centerX(), a.left + 2.2f, a.right - 2.2f);
        float pace = phase == Phase.WHIRL ? 0.3f : 1f;
        float dx = wantX - pos.x;
        pos.x += MathUtils.clamp(dx, -speed * pace * delta, speed * pace * delta);
        pos.y = Math.min(home.y, a.top - 1.1f) + MathUtils.sin(time * 1.3f) * 0.08f;
    }

    private void toWallow() {
        phase = Phase.WALLOW;
        phaseTimer = enraged ? 0.8f : 1.2f;
    }

    private void nextPattern(GameWorld world) {
        Phase[] order = enraged
                ? new Phase[]{Phase.SLAM, Phase.SWEEP, Phase.WHIRL, Phase.INK, Phase.SLAM, Phase.WHIRL}
                : new Phase[]{Phase.SLAM, Phase.INK, Phase.SWEEP, Phase.SLAM, Phase.INK};
        phase = order[patternIndex++ % order.length];
        switch (phase) {
            case SLAM:
                slamsLeft = enraged ? 5 : 3;
                slamTimer = 0f;
                phaseTimer = SLAM_WARN + 0.6f + slamsLeft * 0.3f;
                world.sfx().play("buzz", 0.5f);
                break;
            case SWEEP:
                sweepsLeft = enraged ? 2 : 1;
                startSweep(world);
                break;
            case INK:
                phaseTimer = 0.5f;
                beak = 1f;
                break;
            default: // WHIRL
                phaseTimer = WHIRL_TIME;
                spiralTimer = 0.4f;
                world.sfx().play("swish", 1f);
                break;
        }
    }

    // ------------------------------------------------------------ tentacle slams

    /** A ripple opens: the first right where the hero is, the rest round about. */
    private void addTentacle(GameWorld world, boolean first) {
        Player p = world.getPlayer();
        Tentacle t = new Tentacle();
        t.x = p.centerX();
        t.y = p.feetY();
        if (!first) {
            float a = MathUtils.random(360f);
            float r = MathUtils.random(0.9f, 1.8f);
            t.x += MathUtils.cosDeg(a) * r;
            t.y += MathUtils.sinDeg(a) * r;
        }
        Arena a = Arena.now;
        t.x = MathUtils.clamp(t.x, a.left + 0.4f, a.right - 0.4f);
        t.y = MathUtils.clamp(t.y, a.bottom + 0.3f, a.top - 0.6f);
        tentacles.add(t);
    }

    private void updateTentacles(float delta, GameWorld world, float diff) {
        Player p = world.getPlayer();
        for (int i = tentacles.size - 1; i >= 0; i--) {
            Tentacle t = tentacles.get(i);
            if (t.warn > 0) {
                t.warn -= delta;
                if (t.warn <= 0) { // it bursts up out of the floor
                    t.up = TENTACLE_UP;
                    if (Vector2.dst(t.x, t.y, p.centerX(), p.feetY() + 0.1f) < SLAM_RADIUS) {
                        world.hurtPlayer();
                    }
                    int n = enraged ? 10 : 8;
                    float turn = MathUtils.random(360f);
                    for (int k = 0; k < n; k++) {
                        Bullet b = world.spawnEnemyBullet(t.x, t.y + 0.2f, turn + k * 360f / n, 2.0f * diff);
                        b.shape = Bullet.Shape.DROP;
                        b.color.set(DROP);
                    }
                    world.sfx().play("break", 0.6f);
                    world.shake(0.25f);
                    world.particles().burst(t.x, t.y, 14, 0.4f, 1.8f, 0.5f, 0.05f, FOAM);
                }
            } else {
                t.up -= delta;
                if (t.up <= 0) tentacles.removeIndex(i);
            }
        }
    }

    // ------------------------------------------------------------ the sweep

    private void startSweep(GameWorld world) {
        Arena a = Arena.now;
        Player p = world.getPlayer();
        sweepY = MathUtils.clamp(p.hitboxY(), a.bottom + 0.4f, pos.y - 1.1f);
        sweepDir = p.centerX() < a.centerX ? -1f : 1f; // from the far side, so there's time to see it coming
        sweepFrom = sweepDir > 0 ? a.left : a.right;
        sweepTip = sweepFrom;
        sweeping = false;
        sweepHit = false;
        sweepDropAt = 0.6f;
        phaseTimer = SWEEP_WARN;
        world.sfx().play("buzz", 0.6f);
    }

    private void updateSweep(float delta, GameWorld world, float diff) {
        if (!sweeping) {
            if (phaseTimer <= 0) {
                sweeping = true;
                world.sfx().play("swish", 1f);
            }
            return;
        }
        Arena a = Arena.now;
        float step = SWEEP_SPEED * Math.min(1.2f, diff) * delta;
        sweepTip += sweepDir * step;
        sweepDropAt -= step;
        if (sweepDropAt <= 0) { // drops shaken off as it goes, up and down
            sweepDropAt = 0.6f;
            for (int s = -1; s <= 1; s += 2) {
                Bullet b = world.spawnEnemyBullet(sweepTip, sweepY, 90f * s + MathUtils.random(-15f, 15f), 1.5f * diff);
                b.shape = Bullet.Shape.DROP;
                b.color.set(DROP);
            }
        }
        Player p = world.getPlayer();
        if (!sweepHit && sweepCatches(p.centerX(), p.hitboxY()) && !p.isRolling()) {
            sweepHit = true;
            world.hurtPlayer();
        }
        if (sweepDir > 0 ? sweepTip >= a.right : sweepTip <= a.left) {
            if (--sweepsLeft > 0) {
                startSweep(world);
                phaseTimer = SWEEP_WARN * 0.8f;
            } else {
                sweeping = false;
                toWallow();
            }
        }
    }

    /** The sweeping tentacle lies over (x, y): within its band, between the wall it came from and its tip. */
    boolean sweepCatches(float x, float y) {
        if (!sweeping || Math.abs(y - sweepY) > SWEEP_HALF + Player.HITBOX_RADIUS) return false;
        float lo = Math.min(sweepFrom, sweepTip);
        float hi = Math.max(sweepFrom, sweepTip);
        return x >= lo - Player.HITBOX_RADIUS && x <= hi + Player.HITBOX_RADIUS;
    }

    // ------------------------------------------------------------ ink and the whirlpool

    private void squirtInk(GameWorld world, float diff) {
        int n = enraged ? 4 : 3;
        float aim = aimAngle;
        for (int i = 0; i < n; i++) {
            Bullet b = world.spawnEnemyBullet(beakX(), beakY(), aim + (i - (n - 1) / 2f) * 28f, 2.2f * diff);
            b.radius = 0.16f;
            b.shape = Bullet.Shape.GLOB;
            b.color.set(INK);
            inkBlobs.add(b);
            inkAges.add(0f);
        }
        world.sfx().play("pop", 0.8f);
    }

    private void updateInk(float delta, GameWorld world, float diff) {
        for (int i = inkBlobs.size - 1; i >= 0; i--) {
            Bullet ink = inkBlobs.get(i);
            float age = inkAges.get(i) + delta;
            inkAges.set(i, age);
            if (!ink.alive || age >= INK_BURST) {
                if (ink.alive) {
                    ink.alive = false;
                    int n = enraged ? 12 : 10;
                    float turn = MathUtils.random(360f);
                    for (int k = 0; k < n; k++) {
                        Bullet s = world.spawnEnemyBullet(ink.pos.x, ink.pos.y, turn + k * 360f / n, 1.9f * diff);
                        s.shape = Bullet.Shape.DROP;
                        s.color.set(INK);
                    }
                    world.particles().burst(ink.pos.x, ink.pos.y, 10, 0.2f, 1f, 0.4f, 0.05f, INK);
                }
                inkBlobs.removeIndex(i);
                inkAges.removeIndex(i);
            }
        }
    }

    /** The whirlpool: the hero drifts towards the beak while it pours out a two-armed spiral. */
    private void whirl(float delta, GameWorld world, float diff) {
        beak = Math.min(1f, beak + delta * 4f);
        spin += delta * 140f;
        Player p = world.getPlayer();
        float dx = beakX() - p.centerX();
        float dy = beakY() - p.hitboxY();
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 0.3f && !p.isDead() && !p.isRolling()) {
            float s = Math.min(dist, WHIRL_PULL * delta) / dist;
            p.nudge(dx * s, dy * s, world);
        }
        if (MathUtils.random() < delta * 20f) { // water streaming in
            float a = MathUtils.random(360f);
            float r = MathUtils.random(1.5f, 3f);
            world.particles().spawn(beakX() + MathUtils.cosDeg(a) * r, beakY() + MathUtils.sinDeg(a) * r,
                    -MathUtils.cosDeg(a) * 1.4f, -MathUtils.sinDeg(a) * 1.4f, 0.6f, 0.03f, FOAM, 0.7f);
        }
        spiralTimer -= delta;
        if (spiralTimer <= 0) {
            spiralTimer = 0.13f;
            for (int k = 0; k < 2; k++) {
                Bullet b = world.spawnEnemyBullet(beakX(), beakY(), spin * 1.3f + k * 180f, 2.1f * diff);
                b.shape = Bullet.Shape.DROP;
                b.color.set(DROP);
            }
        }
        if (phaseTimer <= 0) toWallow();
    }

    private float beakX() {
        return pos.x;
    }

    private float beakY() {
        return pos.y - 0.36f;
    }

    /** A raised tentacle can be shot: bullets that hit it hurt the Kraken. */
    @Override
    public boolean bodyContains(float x, float y) {
        for (Tentacle t : tentacles) {
            if (t.warn <= 0 && Math.abs(x - t.x) < 0.2f && y > t.y - 0.05f && y < t.y + TENTACLE_H) return true;
        }
        return false;
    }

    // ------------------------------------------------------------ drawing

    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (phase == Phase.WHIRL) { // the water swirling round its beak
            for (int i = 0; i < 4; i++) {
                float r = 0.6f + ((time * 0.8f + i * 0.5f) % 2f);
                d.ring(b, beakX(), beakY(), r, FOAM, 0.35f * (1f - r / 2.6f));
            }
        }
        for (Tentacle t : tentacles) {
            if (t.warn > 0) { // red ripples, faster as it comes
                float k = 1f - t.warn / SLAM_WARN;
                d.circle(b, t.x, t.y, SLAM_RADIUS, RAGE, 0.12f + 0.2f * k);
                float r = SLAM_RADIUS * ((time * (2f + 4f * k)) % 1f);
                d.ring(b, t.x, t.y, r, RAGE, 0.7f);
                d.ring(b, t.x, t.y, SLAM_RADIUS, RAGE, 0.5f);
            }
        }
        if (phase == Phase.SWEEP) {
            Arena a = Arena.now;
            if (!sweeping) { // the band it's about to sweep along
                float k = 1f - Math.max(0f, phaseTimer) / SWEEP_WARN;
                d.rect(b, a.left, sweepY - SWEEP_HALF, a.right - a.left, SWEEP_HALF * 2f, RAGE, 0.1f + 0.2f * k);
            } else {
                float lo = Math.min(sweepFrom, sweepTip);
                float len = Math.abs(sweepTip - sweepFrom);
                d.rect(b, lo, sweepY - SWEEP_HALF, len, SWEEP_HALF * 2f, Draw.OUTLINE_COLOR);
                d.rect(b, lo, sweepY - SWEEP_HALF + 0.03f, len, SWEEP_HALF * 2f - 0.06f, c(SKIN));
                d.rect(b, lo, sweepY + 0.04f, len, SWEEP_HALF - 0.08f, c(SKIN_LIGHT), 0.5f);
                for (float sx = sweepFrom; sweepDir > 0 ? sx < sweepTip : sx > sweepTip; sx += sweepDir * 0.3f) {
                    d.circle(b, sx, sweepY - 0.1f, 0.06f, SUCKER, 0.9f);
                }
                d.circle(b, sweepTip, sweepY, SWEEP_HALF + 0.03f, Draw.OUTLINE_COLOR);
                d.circle(b, sweepTip, sweepY, SWEEP_HALF, c(SKIN));
                d.circle(b, sweepTip, sweepY, SWEEP_HALF + 0.15f, FOAM, 0.3f);
            }
        }
    }

    @Override
    public void drawOverlay(SpriteBatch b, Draw d, Rectangle view) {
        for (Tentacle t : tentacles) {
            if (t.warn <= 0) drawTentacle(b, d, t);
        }
    }

    /** A tentacle rising out of the floor, curling at the tip, its suckers facing out. */
    private void drawTentacle(SpriteBatch b, Draw d, Tentacle t) {
        float rise = Math.min(1f, (TENTACLE_UP - t.up) * 8f) * Math.min(1f, t.up * 3f);
        d.ellipse(b, t.x, t.y, 0.32f, 0.1f, FOAM, 0.5f);
        int n = 9;
        float x = t.x;
        float y = t.y;
        for (int k = 0; k < n; k++) {
            float f = k / (float) n;
            float r = 0.15f - f * 0.1f;
            x = t.x + MathUtils.sin(time * 4f + f * 3f + t.x) * 0.08f * f + (k > 6 ? (k - 6) * 0.05f : 0f);
            y = t.y + f * TENTACLE_H * rise;
            d.circle(b, x, y, r + 0.025f, Draw.OUTLINE_COLOR);
            d.circle(b, x, y, r, c(k % 2 == 0 ? SKIN : SKIN_DARK));
            if (k % 2 == 1 && k < 7) d.circle(b, x - 0.05f, y, r * 0.35f, SUCKER, 0.9f);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        drawKraken(b, d, pos.x, pos.y, 1f);
    }

    @Override
    public void drawPortrait(SpriteBatch b, Draw d, float aim) {
        aimAngle = aim;
        drawKraken(b, d, pos.x, pos.y + 0.05f, 0.7f); // (it's big: the card shows it smaller)
    }

    private void drawKraken(SpriteBatch b, Draw d, float x, float y, float s) {
        // four great arms curling out to the sides behind the head
        for (int side = -1; side <= 1; side += 2) {
            for (int arm = 0; arm < 2; arm++) {
                float ax = x + side * 0.35f * s;
                float ay = y - 0.3f * s;
                float base = side > 0 ? -10f - arm * 35f : 190f + arm * 35f;
                for (int k = 0; k < 8; k++) {
                    float a = base + side * (k * (12f + arm * 4f) + MathUtils.sin(time * 2f + k * 0.5f + arm) * 10f);
                    ax += MathUtils.cosDeg(a) * 0.13f * s;
                    ay += MathUtils.sinDeg(a) * 0.13f * s;
                    float r = (0.13f - k * 0.012f) * s;
                    d.circle(b, ax, ay, r + 0.02f * s, Draw.OUTLINE_COLOR);
                    d.circle(b, ax, ay, r, c(k % 2 == 0 ? SKIN : SKIN_DARK));
                    if (k % 2 == 1) d.circle(b, ax, ay - r * 0.3f, r * 0.35f, SUCKER, 0.8f);
                }
            }
        }
        // the skirt of short arms under the head
        for (int i = 0; i < 6; i++) {
            float k = (i - 2.5f) / 2.5f;
            float wig = MathUtils.sin(time * 3f + i) * 0.03f * s;
            d.ellipse(b, x + k * 0.42f * s + wig, y - 0.45f * s, 0.1f * s, 0.2f * s, Draw.OUTLINE_COLOR, 1f);
            d.ellipse(b, x + k * 0.42f * s + wig, y - 0.45f * s, 0.08f * s, 0.18f * s, c(SKIN_DARK), 1f);
        }
        // the head: a tall mantle
        d.ellipse(b, x, y + 0.2f * s, 0.62f * s, 0.75f * s, Draw.OUTLINE_COLOR, 1f);
        d.ellipse(b, x, y + 0.2f * s, 0.59f * s, 0.72f * s, c(SKIN), 1f);
        d.ellipse(b, x - 0.15f * s, y + 0.5f * s, 0.25f * s, 0.3f * s, c(SKIN_LIGHT), 0.45f);
        for (int i = 0; i < 4; i++) { // spots
            d.circle(b, x + (i - 1.5f) * 0.25f * s, y + (0.55f + (i % 2) * 0.15f) * s, 0.05f * s, c(SKIN_DARK));
        }
        // a crown of fluffy tufts - it's a Fluffy Kingdom kraken
        for (int i = 0; i < 7; i++) {
            float k = (i - 3f) / 3f;
            d.circle(b, x + k * 0.36f * s, y + (0.86f - k * k * 0.14f) * s + MathUtils.sin(time * 4f + i) * 0.01f,
                    0.08f * s, c(FLUFF));
        }
        // big eyes with goat-slit pupils, looking at you; red and furious once enraged
        for (int side = -1; side <= 1; side += 2) {
            float ex = x + side * 0.3f * s;
            float ey = y + 0.02f * s;
            d.circle(b, ex, ey, 0.17f * s, Draw.OUTLINE_COLOR);
            d.circle(b, ex, ey, 0.15f * s, c(enraged ? RAGE : IRIS));
            float lx = MathUtils.cosDeg(aimAngle) * 0.05f * s;
            float ly = MathUtils.sinDeg(aimAngle) * 0.05f * s;
            d.circle(b, ex + lx, ey + ly, 0.075f * s, PUPIL); // a big round pupil, staring right at you
            d.circle(b, ex + lx - 0.025f * s, ey + ly + 0.03f * s, 0.025f * s, Color.WHITE, 0.9f);
            d.rotRect(b, ex, ey + 0.2f * s, 0.26f * s, 0.06f * s, side * (enraged ? -22f : -12f), SKIN_DARK, 1f);
        }
        // the beak, between the eyes and the arms: it gapes when it squirts or swirls
        float by = y - 0.3f * s;
        float open = 0.12f * beak * s;
        d.shape(b, d.triangle, x, by + open, 0.12f * s, 270f, BEAK, 1f);
        d.shape(b, d.triangle, x, by - open - 0.06f * s, 0.1f * s, 90f, BEAK, 1f);
        if (beak > 0.3f) d.circle(b, x, by - 0.03f * s, 0.06f * s * beak, INK);
    }
}
