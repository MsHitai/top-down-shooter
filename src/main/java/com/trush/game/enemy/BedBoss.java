package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.BossArt.Part;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/** Six residents with their own attack geometry. Remote attacks announce their lanes during
 * wind-up; ordinary volleys communicate through shapes, spacing and committed headings. */
final class BedBoss extends Enemy {
    private enum Phase { RECOVER, ATTACK }

    private static final float WIND_UP = .9f;
    /** How much flight the previous front may still have left when the next attack releases. */
    private static final float TAIL_OVERLAP = .6f;
    /** ...but the tail rule may not hold the next attack longer than this. Other bosses pace their
     * patterns on a clock (the trash can hops for about two seconds between attacks), not on their
     * bullets; slow fronts - banked shells, wall-to-wall rows - must not stretch the gap past that. */
    static final float TAIL_HOLD = 1.7f;
    static final float TAIL_HOLD_ENRAGED = 1.2f;
    private static final Color DUST = Color.valueOf("d3b7cb");
    private static final Color SEAM = Color.valueOf("edccc9");
    private static final Color THREAD = Color.valueOf("cdb5ff");
    private static final Color YARN = Color.valueOf("ff88a7");
    private static final Color TIN = Color.valueOf("f6d081");
    private static final Color WAX = Color.valueOf("f493b0");
    /** Accents mark the shots that break formation: aimed, reacquired or enraged extras. */
    private static final Color STATIC = Color.valueOf("9ff0db");
    private static final Color IRON = Color.valueOf("ff9d68");
    private static final Color PIN = Color.valueOf("ffd964");
    private static final Color SPARE = Color.valueOf("7de8b8");
    private static final Color STEEL = Color.valueOf("8fd0ff");
    private static final Color JEWEL = Color.valueOf("c9a0ff");
    private static final float KIT_COOLDOWN = 5f;
    private static final int DUST_PILES = 8;
    private static final float LEVY_HALF_WIDTH = 1.9f;
    private static final int LEVY_SLOTS = 9;

    private final Boss kind;
    private final Part art;
    private final Color warning;
    private final Color accent;
    private float sweep;
    private final Vector2 target = new Vector2();
    private final Vector2 step = new Vector2();
    private final Vector2[] anchors = new Vector2[DUST_PILES];
    /** Remote projectiles tracked until they die, including later ricochets into view. */
    private final Array<Bullet> incoming = new Array<>();
    private final BedPattern pattern;
    private record WarningLane(float x, float y, float heading, int beat) { }
    private final Array<WarningLane> warnings = new Array<>();
    private boolean preview;
    private final Bullet previewShot = new Bullet(false, 0f, 0f, 0f, 1f, .065f, 1f, Color.WHITE);
    private Phase phase = Phase.RECOVER;
    private float timer = 1.3f;
    private float clock;
    private float recoil;
    private int cycle;
    private int pulse;
    private boolean enraged;
    private BedCritter kit;
    private float kitCooldown;
    private boolean summonTurn;

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
        accent = switch (kind) {
            case DUST_BUNNY -> STATIC;
            case BED_MONSTER -> IRON;
            case SOCK_NINJA -> PIN;
            case YARN_HYDRA -> SPARE;
            case WIND_UP_GENERAL -> STEEL;
            default -> JEWEL;
        };
        pattern = new BedPattern(warning, accent);
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
        return 1.8f;
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
        // Track in simulation too: rendering must not decide when another attack may start.
        for (int i = incoming.size - 1; i >= 0; i--) {
            if (!incoming.get(i).alive) incoming.removeIndex(i);
        }
        clock += delta;
        recoil = Math.max(0f, recoil - 3f * delta);
        enraged = hp < maxHp * 0.5f;
        if (kind == Boss.DUST_BUNNY) {
            if (kit != null && !kit.alive) {
                kit = null;
                kitCooldown = KIT_COOLDOWN;
            } else if (kitCooldown > 0f) {
                kitCooldown -= delta / GameWorld.bossPace(world.isHard());
            }
        }
        timer -= delta;
        if (phase == Phase.RECOVER) {
            if (kind != Boss.WIND_UP_GENERAL || cycle % 4 != 0 || timer > 0.5f) {
                reposition(delta, world);
            }
            if (timer <= 0) {
                // Wind up while the previous front is on its way out: release may overlap its
                // last TAIL_OVERLAP seconds of flight, so the room never sits empty between
                // attacks while no two full patterns close each other's escape routes. A slow
                // tail can outstay that welcome, so past TAIL_HOLD the next attack goes out
                // anyway, at about the pause other bosses keep between patterns.
                float pace = GameWorld.bossPace(world.isHard());
                float lead = (windUp() + TAIL_OVERLAP) / pace;
                float hold = (enraged ? TAIL_HOLD_ENRAGED : TAIL_HOLD) / pace;
                for (Bullet shot : incoming) if (shot.life > lead && timer > -hold) return;
                if (summonTurn && kit == null && kitCooldown <= 0f) {
                    kit = new BedCritter(pos.x, pos.y, BedCritter.Kind.DUST_KIT);
                    world.spawnEnemy(kit);
                    recoil = 1f;
                    timer = .68f;
                } else {
                    prepare(world);
                }
                summonTurn = false;
            }
            return;
        }
        if (timer > 0) return;
        strike(world);
        recoil = 1f;
        if (++pulse >= 3) {
            phase = Phase.RECOVER;
            timer = enraged ? .30f : .42f;
            if (kind == Boss.DUST_BUNNY) timer = enraged ? .28f : .38f;
            cycle++;
            summonTurn = kind == Boss.DUST_BUNNY && cycle % 4 == 0;
        } else if (cycle % 4 == 0) {
            // The bunny's inhale pulses need their spacing: tighter beats stack its fronts
            // into a wall (see the fairness walk-route check).
            timer = kind == Boss.DUST_BUNNY ? .28f : enraged ? .28f : .36f;
        } else {
            timer = bodyBeatInterval(enraged);
        }
    }

    /** Keep the emission point inside the room, so outward fronts do not disappear into a wall. */
    private void reposition(float delta, GameWorld world) {
        float speed = kind == Boss.DUST_BUNNY ? 2.1f : .8f;
        step.set(Arena.now.centerX, Arena.now.centerY).sub(pos);
        if (step.len2() > 1.7f * 1.7f) {
            step.limit(speed * delta);
            if (world.walkable(pos.x, pos.y, pos.x + step.x, pos.y + step.y, radius * .8f)) {
                pos.add(step);
                walkTime += delta;
            }
        } else {
            keepDistance(delta, world, 3.4f, speed);
        }
    }

    static float bodyBeatInterval(boolean enraged) {
        return enraged ? .21f : .27f;
    }

    private float windUp() {
        if (cycle % 4 != 0) return enraged ? .34f : .4f;
        // The bunny's inhale lanes must stay readable: the warning test holds them to half a second.
        return kind == Boss.DUST_BUNNY ? .6f : enraged ? .75f : WIND_UP;
    }

    private void prepare(GameWorld world) {
        phase = Phase.ATTACK;
        pulse = 0;
        target.set(targetX(world), targetY(world));
        if (cycle % 4 != 0) {
            sweep = angle(pos, target);
            timer = windUp();
            return;
        }
        timer = windUp();
        if (kind == Boss.BED_MONSTER || kind == Boss.LINT_REGENT) Arena.now.clamp(target, 2.20f);
        else if (kind == Boss.DUST_BUNNY) Arena.now.clamp(target, .18f);
        switch (kind) {
            case DUST_BUNNY -> prepareDust();
            case WIND_UP_GENERAL -> prepareBanks(world);
            default -> { } // The crease and levy enter from the edges of the room.
        }
        // Reuse the actual emission geometry before release, without spawning or changing timers.
        warnings.clear();
        preview = true;
        for (pulse = 0; pulse < 3; pulse++) {
            switch (kind) {
                case DUST_BUNNY -> { if (!alternate()) breatheDust(world); }
                case BED_MONSTER -> pressCrease(world);
                case LINT_REGENT -> collectLevy(world);
                default -> { }
            }
        }
        preview = false;
        pulse = 0;
    }

    /** Draws loose piles in a crescent from alternating sides, then inhales them in groups. */
    private void prepareDust() {
        float facing = angle(pos, target);
        for (int i = 0; i < DUST_PILES; i++) {
            // The first lane crosses the marked player position. Later pulses fill its flanks.
            int lane = (i + 1) / 2 * (i % 2 == 0 ? -1 : 1);
            float a = facing + lane * (alternate() ? 13f : 8f);
            float reach = Math.max(3.4f, pos.dst(target) + 2.8f);
            float dx = MathUtils.cosDeg(a), dy = MathUtils.sinDeg(a);
            Arena arena = Arena.now;
            if (dx > .001f) reach = Math.min(reach, (arena.right - .12f - pos.x) / dx);
            if (dx < -.001f) reach = Math.min(reach, (arena.left + .12f - pos.x) / dx);
            if (dy > .001f) reach = Math.min(reach, (arena.top - .12f - pos.y) / dy);
            if (dy < -.001f) reach = Math.min(reach, (arena.bottom + .12f - pos.y) / dy);
            anchors[i].set(pos).add(dx * reach, dy * reach);
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
        }
    }

    private void strike(GameWorld world) {
        world.sfx().play(kind == Boss.WIND_UP_GENERAL ? "shotgun" : "enemy_shoot", 0.62f);
        if (cycle % 4 != 0) {
            // Formations and clock rings keep their opening heading; aimed fans reacquire at release.
            boolean committed = cycle % 4 == 1 || (kind == Boss.WIND_UP_GENERAL && cycle % 4 == 2);
            float heading = committed ? sweep
                    : MathUtils.atan2Deg360(targetY(world) - pos.y, targetX(world) - pos.x);
            pattern.fire(world, this, kind, cycle % 4, pulse, heading);
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
            // Without a floor warning, an inward puff needs room to become readable.
            if (!alternate() && pile.dst(targetX(world), targetY(world)) < 2f) continue;
            Vector2 start = alternate() ? pos : pile;
            float heading = alternate() ? angle(pos, pile) : angle(pile, pos);
            for (int rank = 0; rank < 4; rank++) {
                float pace = 1f - rank * .08f;
                for (int mote = -2; mote <= 2; mote++) {
                    float direction = heading + mote * 10f;
                    // All exhaled piles share one escape sector, across every beat and rank.
                    float gap = angle(pos, target) + 45f;
                    if (alternate() && Math.abs(MathUtils.sinDeg((direction - gap) * .5f))
                            < MathUtils.sinDeg(18f)) continue;
                    Bullet puff = shot(world, start.x, start.y, direction, 2.8f * pace, Bullet.Shape.CLOUD);
                    puff.radius *= 1.28f;
                    puff.accel = 3.2f * pace;
                    puff.maxSpeed = 6.8f * pace * world.getDifficulty();
                    puff.life = alternate() ? 3.2f : pile.dst(pos) / Math.max(1f, puff.vel.len());
                }
            }
            if (enraged) {
                Bullet dust = shot(world, start.x, start.y, heading + (i % 2 == 0 ? -11f : 11f), 2.5f, Bullet.Shape.DIAMOND);
                dust.life = 1.65f;
                dust.color.set(accent);
            }
        }
    }

    /** The sheet buckles towards a committed cross, each fold carrying on down its seam. */
    private void pressCrease(GameWorld world) {
        float offset = creaseOffset(pulse);
        float x = target.x + offset;
        float y = target.y - offset;
        boolean vertical = !alternate() || pulse != 1;
        boolean horizontal = !alternate() || pulse != 0;
        if (vertical) {
            float origin = alternate() ? Arena.now.top - .18f
                    : Arena.now.bottom + .18f;
            crease(world, x, origin, alternate() ? 270f : 90f);
        }
        if (horizontal) {
            float origin = alternate() ? Arena.now.left + .18f
                    : Arena.now.right - .18f;
            crease(world, origin, y, alternate() ? 0f : 180f);
        }
        if (!preview) world.shake(pulse == 2 ? 0.26f : 0.12f);
    }

    private void crease(GameWorld world, float x, float y, float direction) {
        // Broad folds sweep a band rather than a single line. Their origins stay at the wall.
        for (int pleat = -4; pleat <= 4; pleat++) {
            float sx = x - MathUtils.sinDeg(direction) * pleat * .28f;
            float sy = y + MathUtils.cosDeg(direction) * pleat * .28f;
            if (!Arena.now.contains(sx, sy, 0f)
                    || Vector2.dst(sx, sy, targetX(world), targetY(world)) < 1.6f) continue;
            for (int rank = 0; rank < 3; rank++) {
                float pace = 1f - rank * .12f;
                Bullet fold = shot(world, sx, sy, direction, 2.7f * pace, Bullet.Shape.SQUARE);
                fold.radius = 0.145f;
                fold.accel = 1.4f * pace;
                fold.maxSpeed = 4.2f * pace;
            }
        }
    }

    private float creaseOffset(int beat) {
        return (2 - beat) * (!alternate() ? 0.55f : -0.55f);
    }

    /** Crossed swords close on a committed target, leaving room to bait the entire sequence. */
    private void sew(GameWorld world) {
        float facing = angle(pos, target);
        for (int side = -1; side <= 1; side += 2) {
            float direction = facing + side * (36f - pulse * 18f);
            for (int cut = -1; cut <= 1; cut++) {
                for (int pin = 0; pin < 8; pin++) {
                    Bullet blade = shot(world, pos.x, pos.y, direction + cut * 10f, 3.6f + pin * .24f, Bullet.Shape.NAIL);
                    blade.life = 2.1f;
                }
            }
        }
    }

    /** Three yarn heads pluck in succession, each spilling a curved strand and a loose knot. */
    private void pullStitch(GameWorld world) {
        float facing = angle(pos, target) + (alternate() ? 1f : -1f) * (pulse - 1) * 38f;
        for (int strand = -1; strand <= 1; strand++) {
            for (int bead = 0; bead < 12; bead++) {
                Bullet thread = shot(world, pos.x, pos.y, facing + strand * 14f, 2.8f + bead * .18f, Bullet.Shape.BALL);
                BedPattern.yarnHead(thread, pulse - 1);
                thread.curve = (alternate() ? -1f : 1f) * 22f;
                thread.life = 2.6f;
            }
        }
        Bullet knot = shot(world, pos.x, pos.y, facing + (alternate() ? -24f : 24f), 3f, Bullet.Shape.POMPOM);
        BedPattern.yarnHead(knot, pulse - 1);
        knot.life = 2.6f;
        knot.color.set(accent);
    }

    private void bankShot(GameWorld world) {
        Vector2 wall = anchors[(cycle / 4 + pulse) % 2];
        float heading = angle(pos, wall);
        for (int rank = 0; rank < 4; rank++) {
            for (int i = -5; i <= 5; i++) {
                if (Math.abs(i) <= 1) continue; // A banked aisle through every rank.
                Bullet shell = shot(world, pos.x, pos.y, heading + i * 3f,
                        (alternate() ? 6.65f : 5.5f) + pulse * 0.30f - rank * .6f, Bullet.Shape.BALL);
                shell.bounces = alternate() ? 2 : 1;
                shell.life = alternate() ? 5.8f : 4.8f;
            }
        }
    }

    /** Four rows of buttons squeeze each approved aisle. The empty slots leave a lane through every row. */
    private void collectLevy(GameWorld world) {
        boolean vertical = levyVertical(pulse);
        float center = levyCenter(pulse);
        float from = vertical ? Arena.now.bottom : Arena.now.left;
        float to = vertical ? Arena.now.top : Arena.now.right;
        float direct = vertical ? 90f : 0f;
        for (int slot = 0; slot < LEVY_SLOTS; slot++) {
            if (Math.abs(slot - levyGap()) <= 1) continue;
            float through = center + levySlot(slot);
            for (int side = 0; side < 2; side++) {
                float origin = side == 0 ? from + 0.17f : to - 0.17f;
                float direction = side == 0 ? direct : direct + 180f;
                float x = vertical ? through : origin, y = vertical ? origin : through;
                if (Vector2.dst(x, y, targetX(world), targetY(world)) < 1.6f) continue;
                for (int rank = 0; rank < 4; rank++)
                    shot(world, x, y, direction, 3.0f + rank * .4f + pulse * .42f, Bullet.Shape.BALL);
            }
        }
    }

    private boolean levyVertical(int beat) {
        return (cycle / 4 + beat) % 2 == 0;
    }

    private float levyCenter(int beat) {
        return levyVertical(beat) ? target.x : target.y;
    }

    private int levyGap() {
        // Crossed levies share one safe intersection for all three beats.
        return 2 + (cycle * 2) % 5;
    }

    private float levySlot(int slot) {
        return MathUtils.lerp(-LEVY_HALF_WIDTH, LEVY_HALF_WIDTH, slot / (float) (LEVY_SLOTS - 1));
    }

    private Bullet shot(GameWorld world, float x, float y, float direction, float speed, Bullet.Shape shape) {
        if (preview) {
            warnings.add(new WarningLane(x, y, direction, pulse));
            return previewShot;
        }
        Bullet b = world.spawnEnemyBullet(x, y, direction, speed * world.getDifficulty());
        b.shape = shape;
        b.color.set(warning);
        track(b);
        return b;
    }

    void track(Bullet bullet) {
        incoming.add(bullet);
    }

    private static float angle(Vector2 a, Vector2 b) {
        return MathUtils.atan2Deg360(b.y - a.y, b.x - a.x);
    }

    /** Announced remote lanes, followed by arrows tracking actual off-screen projectiles. */
    @Override
    public void drawOverlay(SpriteBatch b, Draw d, Rectangle view) {
        if (phase == Phase.ATTACK && cycle % 4 == 0) {
            for (WarningLane lane : warnings) {
                if (lane.beat >= pulse) drawWarningLane(b, d, view, lane.x, lane.y, lane.heading, clock);
            }
        }
        for (int i = incoming.size - 1; i >= 0; i--) {
            Bullet shot = incoming.get(i);
            boolean onScreen = shot.pos.x > view.x && shot.pos.x < view.x + view.width
                    && shot.pos.y > view.y && shot.pos.y < view.y + view.height;
            if (!shot.alive) {
                incoming.removeIndex(i);
                continue;
            }
            if (!onScreen) incomingArrow(b, d, view, shot);
        }
    }

    @Override
    public void drawPortrait(SpriteBatch b, Draw d, float aim) {
        float size = kind == Boss.WIND_UP_GENERAL ? 2.17f : 2.38f;
        d.bosses.draw(b, art, pos.x, pos.y, size, size, false, false, Color.WHITE);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float breath = 0.036f * MathUtils.sin(clock * 3.6f);
        float lean = recoil * 5f;
        boolean flip = MathUtils.cosDeg(aimAngle) < -0.16f;
        float width = kind == Boss.SOCK_NINJA ? 2.53f : 2.93f;
        d.bosses.drawLooking(b, art, pos.x, pos.y + 0.53f + breath + recoil * 0.085f, width,
                3.05f + breath, flip ? -lean : lean, flip, Color.WHITE, aimAngle);
    }
}
