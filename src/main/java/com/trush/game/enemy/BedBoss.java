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

/** Six residents with their own attack geometry, marked lead shots and remote lanes. */
final class BedBoss extends Enemy {
    private enum Phase { RECOVER, ATTACK }

    private static final float WIND_UP = .9f;
    private static final float LEAD_SPEED = 14f;
    private static final float LEAD_STEP = 13f;
    /** How much flight the previous front may still have left when the next attack releases. */
    private static final float TAIL_OVERLAP = .6f;
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
    private static final float CREASE_STEP = .26f;
    private static final float CREASE_SPEED = 1.5f;
    private static final float CREASE_DOOR = .95f;

    private final Boss kind;
    private final Part art;
    private final Color warning;
    private final Color accent;
    private final Bullet.Shape leadShape;
    private float sweep;
    private final Vector2 target = new Vector2();
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

    private boolean alternate() {
        return (cycle / 4) % 2 != 0;
    }

    BedBoss(float x, float y, Boss kind) {
        super(x, y, switch (kind) {
            case DUST_BUNNY -> 700f;
            case BED_MONSTER -> 700f;
            case SOCK_NINJA -> 445f;
            case YARN_HYDRA -> 700f;
            case WIND_UP_GENERAL -> 700f;
            case LINT_REGENT -> 640f;
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
        leadShape = switch (kind) {
            case DUST_BUNNY -> Bullet.Shape.DIAMOND;
            case BED_MONSTER -> Bullet.Shape.PILLOW;
            case SOCK_NINJA -> Bullet.Shape.NEEDLE;
            case YARN_HYDRA -> Bullet.Shape.POMPOM;
            case WIND_UP_GENERAL -> Bullet.Shape.CARTRIDGE;
            case LINT_REGENT -> Bullet.Shape.STAR;
            default -> throw new IllegalArgumentException(kind.name());
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
            case DUST_BUNNY, BED_MONSTER, WIND_UP_GENERAL -> .7f;
            default -> .5f;
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
                float hold = tailHold(enraged) / pace;
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
            timer = enraged ? .18f : .26f;
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

    /** Banked shells and wall-to-wall rows cannot stretch recovery indefinitely. */
    float tailHold(boolean enraged) {
        float hold = switch (kind) {
            case DUST_BUNNY -> .10f;
            case BED_MONSTER -> .18f;
            case SOCK_NINJA -> .65f;
            case WIND_UP_GENERAL -> .26f;
            case LINT_REGENT -> 1.2f;
            default -> .72f;
        };
        return enraged ? hold * .75f : hold;
    }

    private float windUp() {
        if (cycle % 4 != 0) return isEnraged() ? .34f : .4f;
        // The bunny's inhale lanes must stay readable: the warning test holds them to half a second.
        return kind == Boss.DUST_BUNNY ? .6f : isEnraged() ? .75f : WIND_UP;
    }

    private void prepare(GameWorld world) {
        phase = Phase.ATTACK;
        pulse = 0;
        target.set(targetX(world), targetY(world));
        warnings.clear();
        if (cycle % 4 != 0) {
            sweep = angle(pos, target);
            timer = windUp();
            float length = pos.dst(target) + .8f;
            for (int beat = 0; beat < 3; beat++) {
                warnings.add(new WarningLane(new AttackWarning(pos.x, pos.y, leadHeading(beat), length), beat));
            }
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
            Bullet lead = shot(world, pos.x, pos.y, leadHeading(pulse), LEAD_SPEED, leadShape);
            // At 60 Hz, keep travel per step below the combined bullet and player hitbox diameters.
            lead.vel.limit(19.5f);
            lead.radius = .11f;
            lead.life = 1f;
            lead.color.set(accent);
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

    private float leadHeading(int beat) {
        return sweep + switch (beat) {
            case 1 -> -LEAD_STEP;
            case 2 -> LEAD_STEP;
            default -> 0f;
        };
    }

    private void breatheDust(GameWorld world) {
        for (int i = pulse; i < DUST_PILES; i += 3) {
            Vector2 pile = anchors[i];
            // Without a floor warning, an inward puff needs room to become readable.
            if (!alternate() && pile.dst(targetX(world), targetY(world)) < 2f) continue;
            Vector2 start = alternate() ? pos : pile;
            float heading = alternate() ? angle(pos, pile) : angle(pile, pos);
            for (int rank = 0; rank < 2; rank++) {
                float pace = 1f - rank * .16f;
                for (int mote = -2; mote <= 2; mote++) {
                    float direction = heading + mote * 10f;
                    // All exhaled piles share one escape sector, across every beat and rank.
                    float gap = angle(pos, target) + 45f;
                    if (alternate() && Math.abs(MathUtils.sinDeg((direction - gap) * .5f))
                            < MathUtils.sinDeg(18f)) continue;
                    Bullet puff = shot(world, start.x, start.y, direction, 2.0f * pace, Bullet.Shape.DUST_PUFF);
                    puff.radius *= 1.28f;
                    puff.accel = 2.2f * pace;
                    puff.maxSpeed = 4.0f * pace * world.getDifficulty();
                    puff.life = alternate() ? 3.2f : pile.dst(pos) / Math.max(1f, puff.vel.len());
                }
            }
            if (isEnraged()) {
                Bullet dust = shot(world, start.x, start.y, heading + (i % 2 == 0 ? -11f : 11f), 1.9f, Bullet.Shape.DIAMOND);
                dust.life = 1.65f;
                dust.color.set(accent);
            }
        }
    }

    /** The sheet buckles along committed creases: one slow wall per beat, each with its own door.
     * A fold rises from the hem, one slides in from the hero's side, the last settles from the
     * headboard. Each wall is a single fold thick and never accelerates - walk through the door,
     * or roll straight through the fold when there is no time. */
    private void pressCrease(GameWorld world) {
        float direction = switch (pulse) {
            case 0 -> 90f;                                  // rises from the floor's hem
            case 1 -> target.x >= pos.x ? 180f : 0f;        // slides in from the hero's side
            default -> 270f;                                // settles from the headboard
        };
        Vector2 door = new Vector2(target);
        float shift = (pulse % 2 == 0 ? 1f : -1f) * (alternate() ? -2f : 2f);
        if (direction == 90f || direction == 270f) door.x += shift;
        else door.y += shift;
        Arena.now.clamp(door, .7f);
        crease(world, direction, door.x, door.y);
        if (!preview) world.shake(pulse == 2 ? 0.26f : 0.12f);
    }

    private void crease(GameWorld world, float direction, float doorX, float doorY) {
        // A solid rank of folds spans the room edge and slides across; only the door and the
        // roll get through it.
        boolean horizontalWall = direction == 90f || direction == 270f; // spans x, travels in y
        float fixed = horizontalWall ? (direction == 90f ? Arena.now.bottom + .18f : Arena.now.top - .18f)
                : (direction == 0f ? Arena.now.right - .18f : Arena.now.left + .18f);
        float from = horizontalWall ? Arena.now.left : Arena.now.bottom;
        float to = horizontalWall ? Arena.now.right : Arena.now.top;
        float door = horizontalWall ? doorX : doorY;
        for (float across = from + CREASE_STEP * .5f; across < to - .05f; across += CREASE_STEP) {
            if (Math.abs(across - door) < CREASE_DOOR * .5f) continue;
            float x = horizontalWall ? across : fixed;
            float y = horizontalWall ? fixed : across;
            if (!Arena.now.contains(x, y, 0f)
                    || Vector2.dst(x, y, targetX(world), targetY(world)) < .9f) continue;
            Bullet fold = shot(world, x, y, direction, CREASE_SPEED, Bullet.Shape.SQUARE);
            fold.radius = 0.1f;
        }
    }

    /** Crossed swords close on a committed target, leaving room to bait the entire sequence. */
    private void sew(GameWorld world) {
        float facing = angle(pos, target);
        for (int side = -1; side <= 1; side += 2) {
            float direction = facing + side * (36f - pulse * 18f);
            for (int cut = -1; cut <= 1; cut += 2) {
                for (int pin = 0; pin < 4; pin++) {
                    Bullet blade = shot(world, pos.x, pos.y, direction + cut * 10f,
                            2.3f + pin * .3f, Bullet.Shape.NEEDLE);
                    blade.life = 2.1f;
                }
            }
        }
    }

    /** Three yarn heads pluck in succession, each spilling a curved strand and a loose knot. */
    private void pullStitch(GameWorld world) {
        float facing = angle(pos, target) + (alternate() ? 1f : -1f) * (pulse - 1) * 38f;
        for (int strand = -1; strand <= 1; strand++) {
            for (int bead = 0; bead < 5; bead++) {
                Bullet thread = shot(world, pos.x, pos.y, facing + strand * 14f, 1.9f + bead * .2f, Bullet.Shape.BALL);
                BedPattern.yarnHead(thread, pulse - 1);
                thread.curve = (alternate() ? -1f : 1f) * 16f;
                thread.life = 2.6f;
            }
        }
        Bullet knot = shot(world, pos.x, pos.y, facing + (alternate() ? -24f : 24f), 2.1f, Bullet.Shape.POMPOM);
        BedPattern.yarnHead(knot, pulse - 1);
        knot.life = 2.6f;
        knot.color.set(accent);
    }

    private void bankShot(GameWorld world) {
        Vector2 wall = anchors[(cycle / 4 + pulse) % 2];
        float heading = angle(pos, wall);
        for (int rank = 0; rank < 2; rank++) {
            for (int i = -5; i <= 5; i++) {
                if (Math.abs(i) <= 1) continue; // A banked aisle through every rank.
                Bullet shell = shot(world, pos.x, pos.y, heading + i * 3f,
                        (alternate() ? 5.0f : 4.2f) + pulse * 0.30f - rank * 1.2f, Bullet.Shape.CARTRIDGE);
                heavyShell(shell);
                shell.bounces = alternate() ? 2 : 1;
                shell.life = alternate() ? 5.8f : 4.8f;
            }
        }
    }

    void heavyShell(Bullet shell) {
        shell.playerDamage = 2;
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
        for (int slot = 0; slot < LEVY_SLOTS; slot++) {
            if (Math.abs(slot - levyGap()) <= 1) continue;
            float through = center + levySlot(slot);
            for (int side = 0; side < 2; side++) {
                float origin = side == 0 ? from + 0.17f : to - 0.17f;
                float direction = side == 0 ? direct : direct + 180f;
                float x = vertical ? through : origin, y = vertical ? origin : through;
                if (Vector2.dst(x, y, targetX(world), targetY(world)) < 1.4f) continue;
                shot(world, x, y, direction, 1.9f + pulse * .25f, Bullet.Shape.BALL);
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

    private static float angle(Vector2 a, Vector2 b) {
        return MathUtils.atan2Deg360(b.y - a.y, b.x - a.x);
    }

    @Override
    public void collectAttackWarnings(Array<AttackWarning> out) {
        if (!alive || phase != Phase.ATTACK) return;
        for (WarningLane warning : warnings) {
            if (warning.beat >= pulse) out.add(warning.lane);
        }
    }

    /** Announced lead and remote lanes, followed by arrows tracking actual off-screen projectiles. */
    @Override
    public void drawOverlay(SpriteBatch b, Draw d, Rectangle view) {
        if (phase == Phase.ATTACK) {
            for (WarningLane warning : warnings) {
                if (warning.beat < pulse) continue;
                AttackWarning lane = warning.lane;
                if (Float.isInfinite(lane.length())) {
                    drawWarningLane(b, d, view, lane.x(), lane.y(), lane.heading(), clock);
                } else {
                    d.line(b, lane.x(), lane.y(), lane.length(), .035f, lane.heading(), accent,
                            warning.beat == pulse ? .75f : .35f);
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
        float lean = recoil * 5f;
        boolean flip = MathUtils.cosDeg(aimAngle) < -0.16f;
        float width = kind == Boss.SOCK_NINJA ? 2.53f : 2.93f;
        d.bosses.drawLooking(b, art, pos.x, pos.y + 0.53f + breath + recoil * 0.085f, width,
                3.05f + breath, flip ? -lean : lean, flip, Color.WHITE, aimAngle);
    }
}
