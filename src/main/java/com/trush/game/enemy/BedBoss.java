package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.BossArt.Part;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/** Six residents with their own attack geometry. Positions are locked when the floor warnings appear. */
final class BedBoss extends Enemy {
    private enum Phase { RECOVER, WARN, ATTACK }

    private static final float WIND_UP = 1.15f;
    private static final Color DUST = Color.valueOf("d3b7cb");
    private static final Color SEAM = Color.valueOf("edccc9");
    private static final Color THREAD = Color.valueOf("cdb5ff");
    private static final Color YARN = Color.valueOf("ff88a7");
    private static final Color TIN = Color.valueOf("f6d081");
    private static final Color DECOY = new Color(0.70f, 0.60f, 1f, 0.43f);
    private static final Color WAX = Color.valueOf("f493b0");
    private static final int DUST_PILES = 8;
    private static final float LEVY_HALF_WIDTH = 1.9f;
    private static final int LEVY_SLOTS = 9;

    private final Boss kind;
    private final Part art;
    private final Color warning;
    private final Vector2 landing = new Vector2();
    private final Vector2 hopEnd = new Vector2();
    private final Vector2 takeoff = new Vector2();
    private static final float HOP_TIME = .27f;
    private float hop;
    private final Vector2 target = new Vector2();
    private final Vector2[] anchors = new Vector2[DUST_PILES];
    private final Vector2[] farEnds = {new Vector2(), new Vector2()};
    private final BedPattern pattern = new BedPattern();
    private Phase phase = Phase.RECOVER;
    private float timer = 1.3f;
    private float clock;
    private float recoil;
    private int cycle;
    private int pulse;
    private boolean enraged;

    private boolean alternate() {
        return (cycle / 4) % 2 != 0;
    }

    BedBoss(float x, float y, Boss kind) {
        super(x, y, kind == Boss.DUST_BUNNY ? 410f : 560f);
        this.kind = kind;
        art = switch (kind) {
            case DUST_BUNNY -> Part.DUST_BUNNY;
            case BED_MONSTER -> Part.BED_MONSTER;
            case SOCK_NINJA -> Part.SOCK_NINJA;
            case YARN_HYDRA -> Part.YARN_HYDRA;
            case WIND_UP_GENERAL -> Part.WIND_UP_GENERAL;
            case LINT_REGENT -> Part.LINT_REGENT;
            default -> throw new IllegalArgumentException(kind.name());
        };
        warning = switch (kind) {
            case DUST_BUNNY -> DUST;
            case BED_MONSTER -> SEAM;
            case SOCK_NINJA -> THREAD;
            case YARN_HYDRA -> YARN;
            case WIND_UP_GENERAL -> TIN;
            default -> WAX;
        };
        for (int i = 0; i < anchors.length; i++) anchors[i] = new Vector2();
        radius = kind == Boss.SOCK_NINJA ? 0.89f : 1.03f;
        boss = true;
        score = 1250;
    }

    @Override
    public String bossName() {
        return kind.title;
    }

    @Override
    public float viewZoom() {
        return 1.24f;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    @Override
    public void hit(float damage, Vector2 bulletVel, float knockback, GameWorld world) {
        super.hit(damage, bulletVel, 0f, world);
    }

    @Override
    public float feetY() {
        return pos.y - radius * 0.75f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        clock += delta;
        recoil = Math.max(0f, recoil - 3f * delta);
        enraged = hp < maxHp * 0.5f;
        timer -= delta;
        if (hop > 0f) {
            hop = Math.max(0f, hop - delta);
            pos.set(takeoff).lerp(hopEnd, 1f - hop / HOP_TIME);
        }
        if (phase == Phase.RECOVER) {
            if (hop == 0f && (kind != Boss.WIND_UP_GENERAL || cycle % 4 != 0 || timer > 0.5f)) {
                keepDistance(delta, world, 3.4f, kind == Boss.DUST_BUNNY ? 2.1f : 0.8f);
            }
            if (timer <= 0) prepare(world);
            return;
        }
        if (timer > 0) return;
        phase = Phase.ATTACK;
        strike(world);
        recoil = 1f;
        if (++pulse >= 3) {
            phase = Phase.RECOVER;
            timer = enraged ? 0.55f : 0.82f;
            if (kind == Boss.DUST_BUNNY) timer = enraged ? 0.46f : 0.68f;
            cycle++;
        } else {
            timer = cycle % 4 == 0 && kind == Boss.DUST_BUNNY ? 0.28f : enraged ? 0.48f : 0.60f;
            if (cycle % 4 != 0) planBeat(world);
        }
    }

    private void planBeat(GameWorld world) {
        // Each beat marks a fresh position, then commits to it for the whole warning.
        target.set(targetX(world), targetY(world));
        Arena.now.clamp(target, 1.15f);
        pattern.plan(kind, cycle % 4, pulse, target, pos);
        if (movesOnBeat()) {
            float side = pulse % 2 == 0 ? 1f : -1f;
            landing.set(target).add(side * 1.85f, (pulse - 1) * .85f);
            Arena.now.clamp(landing, radius);
        }
    }

    private boolean movesOnBeat() {
        return cycle % 4 == 3 && (kind == Boss.DUST_BUNNY || kind == Boss.SOCK_NINJA);
    }

    private void prepare(GameWorld world) {
        phase = Phase.WARN;
        pulse = 0;
        timer = kind == Boss.DUST_BUNNY ? 0.64f : WIND_UP;
        target.set(targetX(world), targetY(world));
        Arena.now.clamp(target, kind == Boss.DUST_BUNNY ? 0.18f : 2.20f);
        if (cycle % 4 != 0) {
            planBeat(world);
        } else {
            switch (kind) {
                case DUST_BUNNY -> prepareDust();
                case SOCK_NINJA -> prepareStitches();
                case YARN_HYDRA -> prepareYarn();
                case WIND_UP_GENERAL -> prepareBanks(world);
                default -> { } // the crease and decree are drawn relative to the locked target
            }
        }
        world.sfx().play("swish", 0.6f);
    }

    /** Draws loose piles in a crescent from alternating sides, then inhales them in groups. */
    private void prepareDust() {
        float facing = angle(pos, target);
        for (int i = 0; i < DUST_PILES; i++) {
            // The first lane crosses the marked player position. Later pulses fill its flanks.
            int lane = (i + 1) / 2 * (i % 2 == 0 ? -1 : 1);
            float a = facing + lane * (alternate() ? 13f : 8f);
            float reach = Math.max(3.4f, pos.dst(target) + 1.7f);
            float dx = MathUtils.cosDeg(a), dy = MathUtils.sinDeg(a);
            Arena arena = Arena.now;
            if (dx > .001f) reach = Math.min(reach, (arena.right - .12f - pos.x) / dx);
            if (dx < -.001f) reach = Math.min(reach, (arena.left + .12f - pos.x) / dx);
            if (dy > .001f) reach = Math.min(reach, (arena.top - .12f - pos.y) / dy);
            if (dy < -.001f) reach = Math.min(reach, (arena.bottom + .12f - pos.y) / dy);
            anchors[i].set(pos).add(dx * reach, dy * reach);
        }
    }

    /** The two ghosts mark slanted stitch lines, each starting at an opposite end of the room. */
    private void prepareStitches() {
        float hand = !alternate() ? 1f : -1f;
        for (int i = 0; i < 2; i++) {
            float side = i == 0 ? -1f : 1f;
            anchors[i].set(target).add(side * 3.05f, -hand * 1.65f);
            farEnds[i].set(target).add(-side * 3.05f, hand * 1.65f);
            Arena.now.clamp(anchors[i], 0.38f);
            Arena.now.clamp(farEnds[i], 0.38f);
        }
    }

    private void prepareYarn() {
        for (int i = 0; i < 3; i++) {
            setAnchor(anchors[i], target, 90f + 120f * i + 42f * (cycle % 3), 2.20f, 0.35f);
        }
    }

    /** Unfold the walls to find where a one- or two-bounce shot meets each wall. */
    private void prepareBanks(GameWorld world) {
        target.set(targetX(world), targetY(world));
        for (int i = 0; i < 2; i++) {
            float wall = i == 0 ? Arena.now.left : Arena.now.right;
            float other = i == 0 ? Arena.now.right : Arena.now.left;
            float reflected = alternate() ? 2f * other - target.x : target.x;
            float image = 2f * wall - reflected;
            float across = (wall - pos.x) / (image - pos.x);
            anchors[i].set(wall, MathUtils.lerp(pos.y, target.y, across));
            if (alternate()) {
                float secondWall = 2f * wall - other;
                float second = (secondWall - pos.x) / (image - pos.x);
                farEnds[i].set(other, MathUtils.lerp(pos.y, target.y, second));
            }
        }
    }

    private void strike(GameWorld world) {
        world.sfx().play(kind == Boss.WIND_UP_GENERAL ? "shotgun" : "enemy_shoot", 0.62f);
        if (cycle % 4 != 0) {
            pattern.fire(world, warning);
            if (movesOnBeat()) {
                if (kind == Boss.SOCK_NINJA) pos.set(landing);
                else {
                    takeoff.set(pos);
                    hopEnd.set(landing);
                    hop = HOP_TIME;
                }
            }
            return;
        }
        switch (kind) {
            case DUST_BUNNY -> breatheDust(world);
            case BED_MONSTER -> pressCrease(world);
            case SOCK_NINJA -> sew(world);
            case YARN_HYDRA -> pullStitch(world);
            case WIND_UP_GENERAL -> bankShot(world);
            case LINT_REGENT -> collectLevy(world);
            default -> throw new IllegalStateException(kind.name());
        }
    }

    private void breatheDust(GameWorld world) {
        for (int i = pulse; i < DUST_PILES; i += 3) {
            Vector2 pile = anchors[i];
            if (pulse == 2 && i == 2 && world.getEnemies().size + world.getPendingEnemies().size < 4) {
                world.spawnEnemy(new BedCritter(pile.x, pile.y, BedCritter.Kind.DUST_KIT));
            }
            Vector2 start = alternate() ? pos : pile;
            float heading = alternate() ? angle(pos, pile) : angle(pile, pos);
            Bullet puff = shot(world, start.x, start.y, heading, 5.8f, Bullet.Shape.CLOUD);
            puff.radius *= 1.28f;
            puff.accel = 4.7f;
            puff.maxSpeed = 8.4f * world.getDifficulty();
            puff.life = alternate() ? 3.2f : pile.dst(pos) / Math.max(1f, puff.vel.len());
            if (enraged) {
                Bullet dust = shot(world, start.x, start.y, heading + (i % 2 == 0 ? -11f : 11f), 2.5f, Bullet.Shape.DIAMOND);
                dust.life = 1.65f;
            }
        }
    }

    /** The sheet buckles toward the marked cross, then each fold carries on down its seam. */
    private void pressCrease(GameWorld world) {
        float offset = creaseOffset(pulse);
        float x = target.x + offset;
        float y = target.y - offset;
        boolean vertical = !alternate() || pulse != 1;
        boolean horizontal = !alternate() || pulse != 0;
        if (vertical) {
            float origin = alternate() ? Math.min(y + 2.1f, Arena.now.top - 0.18f)
                    : Math.max(y - 2.1f, Arena.now.bottom + 0.18f);
            crease(world, x, origin, alternate() ? 270f : 90f);
        }
        if (horizontal) {
            float origin = alternate() ? Math.max(x - 2.1f, Arena.now.left + 0.18f)
                    : Math.min(x + 2.1f, Arena.now.right - 0.18f);
            crease(world, origin, y, alternate() ? 0f : 180f);
        }
        world.shake(pulse == 2 ? 0.26f : 0.12f);
    }

    private void crease(GameWorld world, float x, float y, float direction) {
        Bullet fold = shot(world, x, y, direction, 2.7f, Bullet.Shape.SQUARE);
        fold.radius = 0.145f;
        fold.accel = 1.4f;
        fold.maxSpeed = 4.2f;
    }

    private float creaseOffset(int beat) {
        return (2 - beat) * (!alternate() ? 0.55f : -0.55f);
    }

    /** Pairs of pins zip down the two threads, a beat apart with a half-stitch offset. */
    private void sew(GameWorld world) {
        for (int i = 0; i < 2; i++) {
            Vector2 start = anchors[i];
            Vector2 end = farEnds[i];
            float direction = angle(start, end);
            float lead = (pulse + 1) * 0.20f;
            float x = MathUtils.lerp(start.x, end.x, lead);
            float y = MathUtils.lerp(start.y, end.y, lead);
            shot(world, x, y, direction, 4.1f, Bullet.Shape.NAIL).life = 1.42f;
            shot(world, x, y, direction + (i == 0 ? 84f : -84f), 3.25f, Bullet.Shape.NAIL).life = 1.15f;
            if (enraged && pulse == 2) {
                shot(world, x, y, direction + (i == 0 ? -84f : 84f), 3.25f, Bullet.Shape.NAIL).life = 1.15f;
            }
        }
    }

    /** A three-cornered loop, with one corner plucked at a time. */
    private void pullStitch(GameWorld world) {
        int index = !alternate() ? pulse : 2 - pulse;
        int next = (index + (!alternate() ? 1 : 2)) % 3;
        Vector2 knot = anchors[index];
        Vector2 edge = alternate() && pulse < 2 ? target : anchors[next];
        for (int i = 0; i < 5; i++) {
            float along = i * 0.16f;
            float x = MathUtils.lerp(knot.x, edge.x, along);
            float y = MathUtils.lerp(knot.y, edge.y, along);
            Bullet bead = shot(world, x, y, angle(knot, edge), 3.7f, Bullet.Shape.BALL);
            bead.life = knot.dst(edge) * (1f - along) / Math.max(1f, bead.vel.len() * 0.85f);
        }
        float cross = angle(knot, alternate() && pulse < 2 ? anchors[next] : target);
        shot(world, knot.x, knot.y, cross, 3.2f, Bullet.Shape.DIAMOND).life = 1.25f;
        if (enraged) shot(world, knot.x, knot.y, cross + 19f, 3.2f, Bullet.Shape.DIAMOND).life = 1.25f;
    }

    private void bankShot(GameWorld world) {
        Vector2 wall = anchors[(cycle / 4 + pulse) % 2];
        float heading = angle(pos, wall);
        for (int i = -1; i <= 1; i++) {
            Bullet shell = shot(world, pos.x, pos.y, heading + i * (alternate() ? 4f : 6f), (alternate() ? 6.65f : 5.5f) + pulse * 0.30f, Bullet.Shape.BALL);
            shell.bounces = alternate() ? 2 : 1;
            shell.life = alternate() ? 7.5f : 4.8f;
        }
    }

    /** Two rows of buttons squeeze each approved aisle. Each aisle is shown before the first shot. */
    private void collectLevy(GameWorld world) {
        boolean vertical = levyVertical(pulse);
        float center = levyCenter(pulse);
        float from = vertical ? Arena.now.bottom : Arena.now.left;
        float to = vertical ? Arena.now.top : Arena.now.right;
        float direct = vertical ? 90f : 0f;
        for (int slot = 0; slot < LEVY_SLOTS; slot++) {
            if (Math.abs(slot - levyGap(pulse)) <= 1) continue;
            float through = center + levySlot(slot);
            for (int side = 0; side < 2; side++) {
                float origin = side == 0 ? from + 0.17f : to - 0.17f;
                float direction = side == 0 ? direct : direct + 180f;
                float speed = 3.9f + pulse * 0.42f;
                shot(world, vertical ? through : origin, vertical ? origin : through, direction, speed, Bullet.Shape.BALL);
            }
        }
    }

    private boolean levyVertical(int beat) {
        return (cycle / 4 + beat) % 2 == 0;
    }

    private float levyCenter(int beat) {
        return levyVertical(beat) ? target.x : target.y;
    }

    private int levyGap(int beat) {
        return 2 + (cycle * 2 + beat * 2) % 5;
    }

    private float levySlot(int slot) {
        return MathUtils.lerp(-LEVY_HALF_WIDTH, LEVY_HALF_WIDTH, slot / (float) (LEVY_SLOTS - 1));
    }

    private Bullet shot(GameWorld world, float x, float y, float direction, float speed, Bullet.Shape shape) {
        Bullet b = world.spawnEnemyBullet(x, y, direction, speed * world.getDifficulty());
        b.shape = shape;
        b.color.set(warning);
        return b;
    }

    private static float angle(Vector2 a, Vector2 b) {
        return MathUtils.atan2Deg360(b.y - a.y, b.x - a.x);
    }

    private static void setAnchor(Vector2 out, Vector2 center, float a, float reach, float margin) {
        out.set(center).add(MathUtils.cosDeg(a) * reach, MathUtils.sinDeg(a) * reach);
        Arena.now.clamp(out, margin);
    }

    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (!alive || phase == Phase.RECOVER) return;
        float alpha = phase == Phase.WARN ? 0.7f + MathUtils.sin(clock * 17f) * 0.18f : 0.64f;
        if (cycle % 4 != 0) {
            pattern.draw(b, d, warning, alpha);
            if (movesOnBeat()) {
                d.ring(b, landing.x, landing.y, radius, warning, alpha);
                if (kind == Boss.SOCK_NINJA)
                    d.bosses.draw(b, art, landing.x, landing.y + .2f, 1.32f, 1.4f, false, false, DECOY);
            }
            return;
        }
        switch (kind) {
            case DUST_BUNNY -> {
                for (int i = 0; i < DUST_PILES; i++) {
                    if (i % 3 < pulse) continue;
                    Vector2 pile = anchors[i];
                    d.circle(b, pile.x, pile.y, 0.14f, warning, alpha * 0.55f);
                    d.ring(b, pile.x, pile.y, 0.24f, warning, alpha);
                    line(b, d, pile, pos, 0.019f, alpha * 0.65f);
                }
            }
            case BED_MONSTER -> {
                float offset = creaseOffset(pulse);
                if (!alternate() || pulse != 1) {
                    d.line(b, target.x + offset, Arena.now.bottom, Arena.now.rows * Arena.CELL, 0.40f, 90f,
                            warning, alpha * 0.38f);
                }
                if (!alternate() || pulse != 0) {
                    d.line(b, Arena.now.left, target.y - offset, Arena.now.cols * Arena.CELL, 0.40f, 0f,
                            warning, alpha * 0.38f);
                }
            }
            case SOCK_NINJA -> {
                for (int i = 0; i < 2; i++) {
                    Vector2 ghost = anchors[i];
                    line(b, d, ghost, farEnds[i], 0.032f, alpha);
                    d.ring(b, ghost.x, ghost.y, 0.30f, warning, alpha);
                }
            }
            case YARN_HYDRA -> {
                for (int i = 0; i < 3; i++) {
                    Vector2 knot = anchors[i];
                    int active = !alternate() ? pulse : 2 - pulse;
                    int next = (i + (!alternate() ? 1 : 2)) % 3;
                    d.ring(b, knot.x, knot.y, 0.23f, warning, i == active ? alpha : alpha * 0.55f);
                    boolean towardMiddle = alternate() && pulse < 2 && i == active;
                    line(b, d, knot, towardMiddle ? target : anchors[next], i == active ? 0.045f : 0.023f, alpha);
                    if (i == active) line(b, d, knot, towardMiddle ? anchors[next] : target, 0.025f, alpha * 0.7f);
                }
            }
            case WIND_UP_GENERAL -> {
                int side = (cycle / 4 + pulse) % 2;
                Vector2 wall = anchors[side];
                line(b, d, pos, wall, 0.04f, alpha);
                if (alternate()) {
                    Vector2 second = farEnds[side];
                    line(b, d, wall, second, 0.04f, alpha);
                    line(b, d, second, target, 0.04f, alpha);
                    d.ring(b, second.x, second.y, 0.28f, warning, alpha);
                } else {
                    line(b, d, wall, target, 0.04f, alpha);
                }
                d.ring(b, wall.x, wall.y, 0.28f, warning, alpha);
            }
            case LINT_REGENT -> drawLevy(b, d, alpha);
            default -> { }
        }
    }

    private void drawLevy(SpriteBatch b, Draw d, float alpha) {
        boolean vertical = levyVertical(pulse);
        float center = levyCenter(pulse);
        float x = vertical ? 0f : Arena.now.left;
        float y = vertical ? Arena.now.bottom : 0f;
        for (int slot = 0; slot < LEVY_SLOTS; slot++) {
            float through = center + levySlot(slot);
            boolean aisle = Math.abs(slot - levyGap(pulse)) <= 1;
            d.line(b, vertical ? through : x, vertical ? y : through,
                    (vertical ? Arena.now.rows : Arena.now.cols) * Arena.CELL,
                    aisle ? 0.025f : 0.049f, vertical ? 90f : 0f,
                    aisle ? TIN : warning, alpha * (aisle ? 0.65f : 1f));
        }
    }

    private void line(SpriteBatch b, Draw d, Vector2 from, Vector2 to, float width, float alpha) {
        d.line(b, from.x, from.y, from.dst(to), width, angle(from, to), warning, alpha);
    }

    @Override
    public void drawPortrait(SpriteBatch b, Draw d, float aim) {
        float size = kind == Boss.WIND_UP_GENERAL ? 2.17f : 2.38f;
        d.bosses.draw(b, art, pos.x, pos.y, size, size, false, false, Color.WHITE);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float breath = 0.036f * MathUtils.sin(clock * 3.6f) + .60f * MathUtils.sin(MathUtils.PI * hop / HOP_TIME);
        float lean = (phase == Phase.WARN ? -3.2f : 0f) + recoil * 5f;
        if (kind == Boss.SOCK_NINJA && cycle % 4 == 0 && alive && phase != Phase.RECOVER) {
            for (int i = 0; i < 2; i++) {
                Vector2 ghost = anchors[i];
                d.bosses.draw(b, art, ghost.x, ghost.y + 0.25f, 1.32f, 1.4f,
                        i == 0, false, DECOY);
            }
        }
        boolean flip = MathUtils.cosDeg(aimAngle) < -0.16f;
        float width = kind == Boss.SOCK_NINJA ? 2.53f : 2.93f;
        d.bosses.draw(b, art, pos.x, pos.y + 0.53f + breath + recoil * 0.085f, width,
                3.05f + breath, flip ? -lean : lean, flip, flash > 0, Color.WHITE);
    }
}
