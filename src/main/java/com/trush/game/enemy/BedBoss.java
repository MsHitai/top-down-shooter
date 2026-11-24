package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.BossArt.Part;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/** Six residents with distinct attack rhythms. Remote attacks show their entry lanes. */
final class BedBoss extends Enemy {
    private enum Phase { RECOVER, ATTACK, CUT }

    private static final float WIND_UP = .9f;
    private static final float CUT_SPEED = 9f;
    private static final float CUT_HALF_WIDTH = .76f;
    private static final Color DUST = Color.valueOf("39cba3");
    private static final Color SEAM = Color.valueOf("519cff");
    private static final Color THREAD = Color.valueOf("a274f5");
    private static final Color YARN = Color.valueOf("ff765e");
    private static final Color TIN = Color.valueOf("f6ba48");
    private static final Color WAX = Color.valueOf("f455a6");
    /** Accents mark the shots that break formation: aimed, reacquired or enraged extras. */
    private static final Color STATIC = Color.valueOf("e1f65e");
    private static final Color IRON = Color.valueOf("ff9d68");
    private static final Color PIN = Color.valueOf("ffd964");
    private static final Color SPARE = Color.valueOf("7de8b8");
    private static final Color STEEL = Color.valueOf("8fd0ff");
    private static final Color JEWEL = Color.valueOf("c9a0ff");
    private static final float KIT_COOLDOWN = 5f;
    private static final int DUST_PILES = 8;
    private static final float LEVY_HALF_WIDTH = 2.2f;
    private static final int LEVY_SLOTS = 9;
    /** Crease walls: one fold thick and never accelerating, so a dodge roll punches straight
     * through them; the door is for walking, the roll is for panic. */
    private static final float CREASE_STEP = .42f;
    private static final float CREASE_SPEED = 2.5f;
    private static final float CREASE_DOOR = 1.7f;

    private final Boss kind;
    private final Part art;
    private final Color warning;
    private final Color accent;
    private float sweep;
    private final Vector2 target = new Vector2();
    private final Vector2 castAt = new Vector2();
    private final Vector2 cutEnd = new Vector2();
    private final Vector2 wander = new Vector2();
    private final Vector2 step = new Vector2();
    private final Vector2[] anchors = new Vector2[DUST_PILES];
    /** Remote projectiles tracked until they die, including later ricochets into view. */
    private final Array<Bullet> incoming = new Array<>();
    private final BedPattern pattern;
    private record WarningLane(AttackWarning lane, int beat) { }
    private final Array<WarningLane> warnings = new Array<>();
    private boolean preview;
    private final Bullet previewShot = new Bullet(false, 0f, 0f, 0f, 1f, .065f, 1f, Color.WHITE);
    private Phase phase = Phase.RECOVER;
    private float timer = 1.3f;
    private float clock;
    private float recoil;
    private int cycle;
    private int pulse;
    private BedCritter kit;
    private float kitCooldown;
    private boolean summonTurn;
    private boolean cutStarted;

    private boolean alternate() {
        return (cycle / period()) % 2 != 0;
    }

    /** Every resident plays five distinct moves before the dance repeats. */
    private int period() {
        return 5;
    }

    /** Which move of the cycle is under way (0 is the signature). */
    private int move() {
        return cycle % period();
    }

    BedBoss(float x, float y, Boss kind) {
        super(x, y, switch (kind) {
            case DUST_BUNNY -> 700f;
            case BED_MONSTER -> 700f;
            case SOCK_NINJA -> 700f;
            case YARN_HYDRA -> 700f;
            case WIND_UP_GENERAL -> 700f;
            case LINT_REGENT -> 700f;
            default -> throw new IllegalArgumentException(kind.name());
        });
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
        float threshold = switch (kind) {
            case SOCK_NINJA -> .5f;
            default -> .7f;
        };
        return hp < maxHp * threshold;
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
        if (phase == Phase.CUT) {
            advanceCut(delta, world);
            return;
        }
        boolean enraged = isEnraged();
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
            if (kind != Boss.WIND_UP_GENERAL || move() != 0 || timer > 0.5f) {
                reposition(delta, world);
            }
            if (timer <= 0) {
                // Begin the wind-up while the old front fades - but like the Fluffy King,
                // only the fast shots must finish: slow residue lingers and keeps the room busy.
                float pace = GameWorld.bossPace(world.isHard());
                if (timer > -tailHold() / pace) {
                    for (Bullet shot : incoming) {
                        if (shot.alive && shot.vel.len() > 3.2f * world.getDifficulty()) return;
                    }
                }
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
        if (timer > 0) return; // the wind-up plants the feet: residents stand their ground
        if (pulse == 0 && move() != 0) sweep = angle(pos, castAt);
        if (isCutAttack() && !cutStarted && pulse < (move() == 0 ? 2 : 1)) {
            cutStarted = true;
            phase = Phase.CUT;
            world.sfx().play("swish", .9f);
            return;
        }
        strike(world);
        recoil = 1f;
        int beats = beatsInAttack(kind, move());
        if (++pulse >= beats) {
            // Keep the final beat's number: its marked lanes describe bullets now in flight.
            pulse = beats - 1;
            phase = Phase.RECOVER;
            timer = enraged ? .14f : .18f;
            cycle++;
            summonTurn = kind == Boss.DUST_BUNNY && move() == 0;
        } else if (move() == 0) {
            timer = switch (kind) {
                case DUST_BUNNY -> .38f;
                case BED_MONSTER -> .57f;
                case SOCK_NINJA -> pulse == 1 ? .69f : .55f;
                case YARN_HYDRA -> .52f;
                case WIND_UP_GENERAL -> .56f;
                case LINT_REGENT -> .48f;
                default -> throw new IllegalStateException(kind.name());
            };
            if (kind == Boss.SOCK_NINJA && pulse == 1) prepareCut(world);
        } else {
            timer = bodyBeatInterval(kind, move(), pulse, enraged);
        }
    }

    private boolean isCutAttack() {
        return kind == Boss.SOCK_NINJA && (move() == 0 || move() == 3);
    }

    /** Cross the marked lane once without turning toward the player during the cut. */
    private void advanceCut(float delta, GameWorld world) {
        step.set(cutEnd).sub(pos).limit(CUT_SPEED * delta);
        float x = pos.x + step.x;
        float y = pos.y + step.y;
        if (!world.walkable(pos.x, pos.y, x, y, radius * .8f)) {
            phase = Phase.ATTACK;
            timer = .08f;
            return;
        }
        float distance = Intersector.distanceSegmentPoint(pos.x, pos.y, x, y, targetX(world), targetY(world));
        if (distance < CUT_HALF_WIDTH + Player.HITBOX_RADIUS) world.hurtPlayer();
        pos.set(x, y);
        walkTime += delta;
        if (pos.dst2(cutEnd) < .0025f) {
            phase = Phase.ATTACK;
            timer = .08f;
        }
    }

    static int beatsInAttack(Boss kind, int move) {
        if (move == 0) return kind == Boss.BED_MONSTER ? 2 : 3;
        return switch (kind) {
            case DUST_BUNNY -> move == 1 ? 6 : move == 4 ? 5 : move == 2 ? 6 : 4;
            case BED_MONSTER -> move == 1 ? 2 : move == 4 ? 4 : move == 2 ? 2 : 4;
            case SOCK_NINJA -> move == 3 ? 4 : move == 4 ? 6 : 3;
            case YARN_HYDRA -> move == 1 ? 6 : move == 4 ? 5 : move == 2 ? 3 : 4;
            case WIND_UP_GENERAL -> move == 1 ? 4 : move == 4 ? 4 : move == 2 ? 4 : 3;
            // The seal's missing corner alternates, so its blooms must not overlap.
            case LINT_REGENT -> move == 1 ? 2 : move == 4 ? 5 : move == 2 ? 3 : 4;
            default -> throw new IllegalArgumentException(kind.name());
        };
    }

    /** Residents hold their ground like the Fluffy King: a slow glide between standing spots,
     * never homing on the player. Only the ninja patrols the whole bed. */
    private void reposition(float delta, GameWorld world) {
        float glide = switch (kind) {
            case SOCK_NINJA -> 1.7f;
            case BED_MONSTER -> 1.05f;
            default -> .85f;
        };
        if (wander.isZero() || pos.dst2(wander) < .04f) pickStandingSpot();
        step.set(wander).sub(pos).limit(glide * delta);
        float x = pos.x + step.x;
        float y = pos.y + step.y;
        if (Arena.now.contains(x, y, radius) && world.walkable(pos.x, pos.y, x, y, radius * .8f)) {
            pos.add(step);
            walkTime += delta;
        } else {
            pickStandingSpot();
        }
    }

    /** Each resident keeps to its own part of the bedroom; the ninja works the whole bed. */
    private void pickStandingSpot() {
        Arena arena = Arena.now;
        // A resident caught near a wall pulls its next standing spot well inside, so its
        // fronts stay in the arena instead of clipping the edge for a wall-camping hero.
        float inset = arena.contains(pos.x, pos.y, 1.6f) ? 1.1f : 2.2f;
        float margin = radius + inset;
        float lowY = kind == Boss.SOCK_NINJA ? arena.bottom + margin : arena.centerY - 1.2f;
        wander.set(MathUtils.random(arena.left + margin, arena.right - margin),
                MathUtils.random(lowY, arena.top - margin));
    }

    static float bodyBeatInterval(Boss kind, int move, int nextBeat, boolean enraged) {
        float interval = switch (kind) {
            // Fan chains and marching ranks keep a marching beat; lintels and blooms take
            // a breath to hang, so their beats space out.
            case DUST_BUNNY -> move == 1 ? .34f : move == 4 ? .3f : move == 2 ? .5f : .38f;
            case BED_MONSTER -> move == 1 ? .42f : move == 4 ? .55f : move == 2 ? .55f : .5f;
            case SOCK_NINJA -> move == 4 ? .3f : nextBeat % 2 == 1 ? .24f : .51f;
            case YARN_HYDRA -> move == 1 ? .42f : move == 4 ? .3f : move == 2 ? .55f : .38f;
            case WIND_UP_GENERAL -> move == 1 ? .46f : move == 4 ? .5f : move == 2 ? .46f : .34f;
            case LINT_REGENT -> move == 1 ? .5f : move == 4 ? .4f : move == 2 ? .55f : nextBeat == 1 ? .36f : .46f;
            default -> throw new IllegalStateException(kind.name());
        };
        return enraged ? interval * .75f : interval;
    }

    /** Let the General's shells complete their banks before the next attack. */
    float tailHold() {
        return kind == Boss.WIND_UP_GENERAL && move() == 1 ? 2.05f : .55f;
    }

    private float windUp() {
        if (isCutAttack()) return .78f;
        if (move() != 0) return kind == Boss.SOCK_NINJA ? .55f : isEnraged() ? .39f : .48f;
        // The bunny's inhale lanes must stay readable: the warning test holds them to half a second.
        return kind == Boss.DUST_BUNNY ? .6f : kind == Boss.SOCK_NINJA ? .78f
                : isEnraged() ? .75f : WIND_UP;
    }

    private void prepare(GameWorld world) {
        phase = Phase.ATTACK;
        pulse = 0;
        castAt.set(targetX(world), targetY(world));
        target.set(castAt);
        warnings.clear();
        cutStarted = false;
        timer = windUp();
        if (move() != 0) {
            sweep = angle(pos, target);
            if (isCutAttack()) prepareCut(world);
            return;
        }
        if (kind == Boss.BED_MONSTER || kind == Boss.LINT_REGENT) Arena.now.clamp(target, 2.20f);
        else if (kind == Boss.DUST_BUNNY) Arena.now.clamp(target, .18f);
        switch (kind) {
            case DUST_BUNNY -> prepareDust();
            case WIND_UP_GENERAL -> prepareBanks(world);
            case SOCK_NINJA -> prepareCut(world);
            default -> { } // The crease and levy enter from the edges of the room.
        }
        // Reuse the actual emission geometry before release, without spawning or changing timers.
        preview = true;
        for (pulse = 0; pulse < beatsInAttack(kind, 0); pulse++) {
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

    private void prepareCut(GameWorld world) {
        target.set(targetX(world), targetY(world));
        step.set(target).sub(pos);
        float distance = Math.min(step.len() + .9f, 5.4f);
        cutEnd.set(pos).add(step.setLength(distance));
        Arena.now.clamp(cutEnd, radius);
        warnings.clear();
        warnings.add(new WarningLane(new AttackWarning(pos.x, pos.y, angle(pos, cutEnd),
                pos.dst(cutEnd), CUT_HALF_WIDTH), pulse));
        cutStarted = false;
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
        if (move() != 0) {
            // Formations and clock rings keep their opening heading; aimed fans reacquire at release.
            boolean committed = move() == 1 || (kind == Boss.WIND_UP_GENERAL && move() == 2);
            float heading = committed ? sweep
                    : MathUtils.atan2Deg360(targetY(world) - pos.y, targetX(world) - pos.x);
            pattern.fire(world, this, kind, move(), pulse, heading);
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
        float heroX = targetX(world);
        float heroY = targetY(world);
        for (int i = pulse; i < DUST_PILES; i += 3) {
            Vector2 pile = anchors[i];
            if (!alternate() && (pile.dst(castAt) < 2f || pile.dst(heroX, heroY) < 2f)) continue;
            Vector2 start = alternate() ? pos : pile;
            float heading = alternate() ? angle(pos, pile) : angle(pile, pos);
            for (int mote = -1; mote <= 1; mote++) {
                float direction = heading + mote * 14f;
                float gap = angle(pos, target) + 45f;
                if (alternate() && Math.abs(MathUtils.sinDeg((direction - gap) * .5f))
                        < MathUtils.sinDeg(18f)) continue;
                Bullet puff = shot(world, start.x, start.y, direction, 1.9f, Bullet.Shape.DUST_PUFF);
                puff.radius = .083f;
                puff.accel = 1.45f;
                puff.maxSpeed = 3.7f * world.getDifficulty();
            }
        }
    }

    /** Two folds cross from one edge with a common doorway and room to go around both ends. */
    private void pressCrease(GameWorld world) {
        boolean horizontal = !alternate();
        float direction = horizontal ? (target.y > Arena.now.centerY ? 270f : 90f)
                : (target.x > Arena.now.centerX ? 180f : 0f);
        float fixed = switch ((int) direction) {
            case 90 -> Arena.now.bottom + .18f;
            case 270 -> Arena.now.top - .18f;
            case 180 -> Arena.now.right - .18f;
            default -> Arena.now.left + .18f;
        };
        float center = horizontal ? target.x : target.y;
        float door = center + (alternate() ? -1.15f : 1.15f);
        float from = Math.max(center - 3.65f, horizontal ? Arena.now.left : Arena.now.bottom);
        float to = Math.min(center + 3.65f, horizontal ? Arena.now.right : Arena.now.top);
        float heroX = targetX(world);
        float heroY = targetY(world);
        for (float across = from + CREASE_STEP * .5f; across < to; across += CREASE_STEP) {
            if (Math.abs(across - door) < CREASE_DOOR * .5f) continue;
            float x = horizontal ? across : fixed;
            float y = horizontal ? fixed : across;
            if (castAt.dst(x, y) < 1.4f || Vector2.dst(x, y, heroX, heroY) < 1.4f) continue;
            Bullet fold = shot(world, x, y, direction, CREASE_SPEED, Bullet.Shape.SQUARE);
            fold.radius = .16f;
        }
        if (!preview) world.shake(.12f);
    }

    /** Crossed swords close on a committed target, leaving room to bait the entire sequence. */
    private void sew(GameWorld world) {
        float facing = angle(pos, target);
        for (int side = -1; side <= 1; side += 2) {
            float direction = facing + side * (36f - pulse * 18f);
            for (int pin = 0; pin < 3; pin++) {
                Bullet blade = shot(world, pos.x, pos.y, direction + side * pin * 9f,
                        3.8f - pin * .31f, Bullet.Shape.NEEDLE);
                if (pin == 0) blade.color.set(accent);
            }
        }
    }

    /** Three yarn heads pluck in succession, each spilling a curved strand and a loose knot. */
    private void pullStitch(GameWorld world) {
        float facing = angle(pos, target) + (alternate() ? 1f : -1f) * (pulse - 1) * 38f;
        for (int strand = -1; strand <= 1; strand++) {
            for (int bead = 0; bead < 2; bead++) {
                Bullet thread = shot(world, pos.x, pos.y, facing + strand * 23f, 2.5f + bead * .45f, Bullet.Shape.BALL);
                BedPattern.yarnHead(thread, pulse - 1);
                thread.curve = (alternate() ? -1f : 1f) * 16f;
            }
        }
        // The middle beat's knot rides the baited line exactly - the one throw you cannot sidestep
        // by standing still; the flanking beats throw from the heads as usual.
        boolean middle = pulse == 1;
        Bullet knot = shot(world, pos.x, pos.y,
                facing + (middle ? 0f : alternate() ? -24f : 24f), 2.9f, Bullet.Shape.POMPOM);
        if (!middle) BedPattern.yarnHead(knot, pulse - 1);
        knot.color.set(accent);
    }

    private void bankShot(GameWorld world) {
        Vector2 wall = anchors[(cycle / 4 + pulse) % 2];
        float heading = angle(pos, wall);
        for (int shellIndex = -1; shellIndex <= 1; shellIndex++) {
            Bullet shell = shot(world, pos.x, pos.y, heading + shellIndex * 9f,
                    (alternate() ? 5.6f : 4.9f) + pulse * .2f, Bullet.Shape.CARTRIDGE);
            heavyShell(shell);
            shell.bounces = alternate() ? 2 : 1;
        }
    }

    void heavyShell(Bullet shell) {
        shell.color.set(accent);
        shell.radius = .082f;
    }

    /** One row of buttons from each end of the room per beat. The empty slots leave a single
     * shared lane through every beat of the levy - bait all three crossings in one spot. */
    private void collectLevy(GameWorld world) {
        boolean vertical = levyVertical(pulse);
        float center = levyCenter(pulse);
        float from = vertical ? Arena.now.bottom : Arena.now.left;
        float to = vertical ? Arena.now.top : Arena.now.right;
        float direct = vertical ? 90f : 0f;
        float heroX = targetX(world);
        float heroY = targetY(world);
        for (int slot = 0; slot < LEVY_SLOTS; slot++) {
            if (Math.abs(slot - levyGap()) <= 1) continue;
            float through = center + levySlot(slot);
            for (int side = 0; side < 2; side++) {
                float origin = side == 0 ? from + 0.17f : to - 0.17f;
                float direction = side == 0 ? direct : direct + 180f;
                float x = vertical ? through : origin, y = vertical ? origin : through;
                if (castAt.dst(x, y) < 1.4f || Vector2.dst(x, y, heroX, heroY) < 1.4f) continue;
                Bullet button = shot(world, x, y, direction, 3.0f + pulse * .3f, Bullet.Shape.BALL);
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
            warnings.add(new WarningLane(new AttackWarning(x, y, direction, Float.POSITIVE_INFINITY), pulse));
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

    /** Where the bait stood when this attack was prepared - blooms close in on it. */
    float baitX() {
        return castAt.x;
    }

    float baitY() {
        return castAt.y;
    }

    private static float angle(Vector2 a, Vector2 b) {
        return MathUtils.atan2Deg360(b.y - a.y, b.x - a.x);
    }

    @Override
    public void collectAttackWarnings(Array<AttackWarning> out) {
        if (!alive) return;
        // Lanes stay readable through the recovery after the final beat: the bullets they
        // announced are in flight, so the marked lanes still describe real danger.
        for (WarningLane warning : warnings) {
            if (warning.beat == pulse && !(cutStarted && warning.lane.halfWidth() > 0f)) out.add(warning.lane);
        }
    }

    /** Announced remote lanes and arrows tracking actual off-screen projectiles. */
    @Override
    public void drawOverlay(SpriteBatch b, Draw d, Rectangle view) {
        if (phase == Phase.ATTACK) {
            for (WarningLane warning : warnings) {
                if (warning.beat != pulse) continue;
                AttackWarning lane = warning.lane;
                if (lane.halfWidth() <= 0f || !cutStarted) {
                    drawWarningLane(b, d, view, lane.x(), lane.y(), lane.heading(), clock);
                }
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
        float charge = phase == Phase.ATTACK && pulse == 0 ? MathUtils.clamp(1f - timer / windUp(), 0f, 1f) : 0f;
        float stretch = switch (kind) {
            case DUST_BUNNY -> -.22f;
            case BED_MONSTER -> .24f;
            case LINT_REGENT -> .14f;
            default -> -.06f;
        } * charge;
        float lean = recoil * 5f;
        lean -= (kind == Boss.SOCK_NINJA ? 14f : 6f) * charge;
        boolean flip = MathUtils.cosDeg(aimAngle) < -0.16f;
        float width = (kind == Boss.SOCK_NINJA ? 2.53f : 2.93f) - stretch * .55f;
        d.bosses.drawLooking(b, art, pos.x, pos.y + 0.53f + breath + recoil * 0.085f, width,
                3.05f + breath + stretch, flip ? -lean : lean, flip, Color.WHITE, aimAngle);
    }
}
