package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

import static com.trush.game.objects.Bullet.Shape.*;

/**
 * Each resident has its own projectile grammar, tuned to the Traffic Light and Haymaker
 * contract: residents stand their ground and threaten with volume and rhythm - dense fronts
 * whose spaces are streets you walk through, not seams you thread. One volley per beat.
 * Every turn commits its aim on the release beat, so players can bait a formation and
 * walk around it; nothing tracks the hero after release.
 */
final class BedPattern {
    /** Non-committed turns swing after the hero like the Traffic Light's green streams -
     * slow enough to outrun by circling, too fast to stand still in. */
    private static final float SWING_PER_BEAT = 18f;

    private final Color color;
    private final Color accent;
    private float formationAim;

    BedPattern(Color color, Color accent) {
        this.color = color;
        this.accent = accent;
    }

    void fire(GameWorld world, BedBoss boss, Boss kind, int move, int beat, float aim) {
        if (beat == 0) formationAim = aim;
        else if (kind != Boss.SOCK_NINJA && move != 3) {
            // The ninja's sweeps and everyone's finishers stay on the baited line; the
            // middle moves' fronts hunt, capped.
            float diff = ((aim - formationAim + 540f) % 360f) - 180f;
            formationAim += MathUtils.clamp(diff, -SWING_PER_BEAT, SWING_PER_BEAT);
        }
        // Snips split attention while the hero watches the spiral. They commit to the bait,
        // like every other front - walking away from the wind-up defeats them. The ninja's
        // needles and the General's banked shells already play this part.
        boolean snip = kind != Boss.SOCK_NINJA && kind != Boss.WIND_UP_GENERAL
                && move == 1 && beat % 2 == 1;
        switch (kind) {
            case DUST_BUNNY -> dust(world, boss, move, beat, aim);
            case BED_MONSTER -> sheet(world, boss, move, beat, aim);
            case SOCK_NINJA -> blades(world, boss, move, beat, aim);
            case YARN_HYDRA -> yarn(world, boss, move, beat, aim);
            case WIND_UP_GENERAL -> clockwork(world, boss, move, beat, aim);
            case LINT_REGENT -> royalty(world, boss, move, beat, aim);
            default -> throw new IllegalArgumentException(kind.name());
        }
        if (snip) snip(world, boss, kind);
    }

    /** Between the beats' geometry, every resident keeps up a small aimed pressure of its
     * own - the Fluffy King's fan inside his rings. Aimed at release, never tracked;
     * the ninja's needles already play this part. */
    private void snip(GameWorld w, BedBoss b, Boss kind) {
        float aim = MathUtils.atan2Deg360(b.baitY() - b.pos.y, b.baitX() - b.pos.x);
        switch (kind) {
            case DUST_BUNNY -> { // a sneeze of loose fluff
                for (int puff = -1; puff <= 1; puff += 2) launch(w, b, aim + puff * 9f, 2.6f, DUST_PUFF);
            }
            case BED_MONSTER -> launch(w, b, aim, 2.6f, SQUARE); // one reach from under the bed
            case YARN_HYDRA -> { for (int side = -1; side <= 1; side += 2)
                launch(w, b, aim + side * 6f, 2.6f, BALL); }
            case WIND_UP_GENERAL -> launch(w, b, aim, 2.2f, STAR); // the clock keeps ticking
            case LINT_REGENT -> { for (int side = -1; side <= 1; side += 2)
                launch(w, b, aim + side * 8f, 2.6f, BALL); } // the levy keeps collecting
            default -> { }
        }
    }

    private void dust(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // A dust spiral: four arms unwind from the bunny, turning all the while.
            for (int arm = 0; arm < 5; arm++) {
                Bullet spark = launch(w, b, formationAim + beat * 16f + arm * 72f, 2.6f, DIAMOND);
                spark.curve = arm % 2 == 0 ? 10f : -10f;
            }
        } else if (move == 2) {
            // A ring of loose fur blooms around the bait and closes in; the gap circles round.
            bloomRing(w, b, DUST_PUFF, 18, 3, formationAim + beat * 60f);
        } else if (move == 4) {
            // The sneeze cone: a tight puff cone sweeping across the bait, beat by beat.
            for (int puff = -3; puff <= 3; puff++) {
                Bullet cone = launch(w, b, formationAim + (beat - 2) * 40f + puff * 8f, 2.4f, DUST_PUFF);
                cone.curve = (beat - 2) * 12f;
            }
        } else {
            // The tumble: a wide fan sweeps the bait while the heavy clumps read the walk.
            aimedFan(w, b, 3.2f, DUST_PUFF);
            float base = beat == 0 ? formationAim : b.leadAngle(w, 3.3f * w.getDifficulty(), .6f);
            float lane = base + (beat == 0 ? 0f : (beat - 2) * 55f);
            Bullet clump = launch(w, b, lane, 3.3f, POMPOM);
            heavy(clump, .19f);
            clump.accel = b.isEnraged() ? 2.9f : 2.2f;
            clump.maxSpeed = 6.8f * w.getDifficulty();
            for (int side = -1; side <= 1; side += 2) {
                for (int crumb = 1; crumb <= 2; crumb++) {
                    Bullet spark = launch(w, b, lane + side * (14f + crumb * 13f), 2.6f, DIAMOND);
                    spark.curve = side * 12f;
                }
            }
        }
    }

    private void sheet(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // Two broad pleats travel on either side of a straight aisle.
            for (int side = -1; side <= 1; side += 2) {
                for (int fold = 0; fold < 3; fold++) {
                    Bullet panel = launch(w, b, formationAim, 2.9f, SQUARE);
                    offset(panel, formationAim, side * (.67f + fold * .42f), -fold * .15f);
                }
            }
        } else if (move == 2) {
            // The tucked sheet snaps shut around the bait: a ring of folds closing in.
            bloomRing(w, b, SQUARE, 16, 2, formationAim - beat * 60f);
        } else if (move == 4) {
            // The mattress quake: a ring of folds rolls out with every beat, its gap circling.
            for (int i = 0; i < 16; i++) {
                if (i == beat * 3 % 16 || i == (beat * 3 + 1) % 16 || i == (beat * 3 + 2) % 16) continue;
                launch(w, b, 360f * i / 16 + beat * 15f, 2.3f, SQUARE);
            }
        } else {
            // Pillows down the fan's lanes: the monster sets traps, it doesn't read your walk.
            aimedFan(w, b, 3.2f, SQUARE);
            float spread = beat == 0 ? 0f : beat == 1 ? 34f : -34f;
            Bullet pillow = launch(w, b, formationAim + spread, beat == 0 ? 3.0f : 2.6f, PILLOW);
            heavy(pillow, .145f);
            for (int side = -1; side <= 1; side += 2) {
                for (int feather = 1; feather <= 2; feather++) {
                    Bullet loose = launch(w, b, formationAim + spread + side * feather * 21f, 2.6f, FEATHER);
                    loose.curve = side * 11f;
                }
            }
        }
    }

    private void blades(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // Six paired shuriken sweep the pinwheel, offset so the baited line falls
            // between the arms - readable from any range.
            for (int arm = 0; arm < 6; arm++) {
                float heading = formationAim + 30f + arm * 60f + beat * 20f;
                for (int edge = -1; edge <= 1; edge += 2) {
                    launch(w, b, heading + edge * 7f, 3.1f, NEEDLE);
                }
            }
        } else if (move == 2) {
            // Scissors close across the original line. The outside stays open.
            for (int side = -1; side <= 1; side += 2) {
                float blade = formationAim + side * (42f - beat * 17f);
                launch(w, b, blade, 3.6f, NEEDLE).color.set(accent);
                launch(w, b, blade + side * 10f, 2.8f, NEEDLE);
                launch(w, b, blade + side * 20f, 2.2f, NEEDLE);
                Bullet sock = launch(w, b, blade + side * 31f, 1.75f, SOCK);
                sock.curve = -side * 10f;
            }
        } else if (move == 4) {
            // The needle web: one needle per bearing, tightening turn by turn.
            for (int spoke = 0; spoke < 8; spoke++) {
                launch(w, b, formationAim + spoke * 45f + beat * 22.5f, 2.6f, NEEDLE);
            }
        } else {
            // Strafe the baited line, then two five-needle fans scissor across the escape.
            if (beat < 2) {
                for (int needle = -1; needle <= 1; needle++) {
                    launch(w, b, formationAim + needle * 18f, needle == 0 ? 3.6f : 2.8f, NEEDLE);
                }
                for (int side = -1; side <= 1; side += 2) {
                    Bullet sock = launch(w, b, formationAim + side * 48f, 1.7f, SOCK);
                    sock.curve = -side * 15f;
                }
            } else {
                float swing = beat == 2 ? 25f : -25f;
                for (int pin = -2; pin <= 2; pin++) {
                    Bullet needle = launch(w, b, formationAim + swing + (beat == 2 ? pin : -pin) * 14f,
                            pin == 0 ? 3.4f : 2.9f, NEEDLE);
                    if (pin == 0) needle.color.set(accent);
                }
            }
        }
    }

    private void yarn(GameWorld w, BedBoss b, int move, int beat, float aim) {
        // The three heads take turns; longer turns wrap back to the first head.
        int head = beat % 3 - 1;
        // Heads step by one bead spacing, so every fan keeps a bead on the baited line.
        float facing = formationAim + head * 28f;
        if (move == 1) {
            // The heads knit a four-strand spiral, turn after turn.
            for (int arm = 0; arm < 5; arm++) {
                Bullet thread = launch(w, b, formationAim + beat * 18f + arm * 72f, 2.7f, BALL);
                yarnHead(thread, head);
                thread.curve = head < 0 ? -18f : 18f;
            }
        } else if (move == 2) {
            // The heads loop a ring of yarn around the bait and pull it tight.
            bloomRing(w, b, BALL, 16, 2, formationAim + beat * 55f);
        } else if (move == 4) {
            // The braid: two strands weave over and under, crossing on the bait.
            for (int side = -1; side <= 1; side += 2) {
                for (int bead = 0; bead < 3; bead++) {
                    Bullet strand = launch(w, b, formationAim + side * (10f + bead * 14f), 2.4f, BALL);
                    yarnHead(strand, head);
                    strand.curve = side * -16f;
                }
            }
        } else {
            // The heads trade a heavy knot down the line while a fan of beads sweeps the bait.
            aimedFan(w, b, 3.2f, BALL);
            float lane = beat == 0 ? formationAim : b.leadAngle(w, 3.1f * w.getDifficulty(), .5f);
            Bullet knot = launch(w, b, lane, 3.1f, POMPOM);
            heavy(knot, .14f);
            for (int bead = -3; bead <= 3; bead++) {
                Bullet thread = launch(w, b, facing + bead * 22f, 2.7f, BALL);
                yarnHead(thread, head);
                thread.curve = head * 16f;
            }
        }
    }

    private void clockwork(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // The mainspring unwinds: a tight six-file spiral, ticking round and round.
            for (int file = -4; file <= 4; file++) {
                if (file == 0) continue;
                Bullet tick = launch(w, b, formationAim + beat * 15f, 2.5f, SQUARE);
                tick.curve = file % 2 == 0 ? 10f : -10f; // the spring's coils bend as they unwind
                offset(tick, formationAim + beat * 15f, file * .36f, 0f);
            }
        } else if (move == 2) {
            // An open clock face keeps the same wide missing sector throughout the turn.
            for (int tick = 0; tick < 12; tick++) {
                if (tick == 1 || tick == 2) continue;
                launch(w, b, formationAim + tick * 30f, 2.9f, STAR);
            }
        } else if (move == 4) {
            // The bell tolls: a slow star ring rolls out, the missing chime circling round.
            for (int i = 0; i < 18; i++) {
                if (i == beat * 3 || i == beat * 3 + 1 || i == (beat * 3 + 2) % 18) continue;
                launch(w, b, 360f * i / 18, 2.1f, STAR);
            }
        } else {
            // Spring shells fan out slowly, then gather speed without changing lanes.
            for (int spring = -3; spring <= 3; spring++) {
                Bullet shell = launch(w, b, aim + spring * 20f, 1.4f + beat * .32f, CARTRIDGE);
                shell.accel = b.isEnraged() ? 1.65f : 1.1f;
                shell.maxSpeed = 4.6f * w.getDifficulty();
                b.heavyShell(shell);
            }
        }
    }

    private void royalty(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // The crown spins: jewels and beads unwind together, beat after beat.
            for (int jewel = 0; jewel < 5; jewel++) {
                Bullet j = launch(w, b, formationAim + beat * 15f + jewel * 72f, 2.9f,
                        jewel % 2 == 0 ? DIAMOND : STAR);
                j.curve = jewel % 2 == 0 ? 8f : -8f;
            }
        } else if (move == 2) {
            // The royal seal closes around the bait itself - the open corner is the way out.
            bloomRing(w, b, DIAMOND, 16, 2, formationAim - beat * 55f);
        } else if (move == 4) {
            // The sceptre sweep: a wide, slow arc of jewels rolling across the floor.
            for (int jewel = -6; jewel <= 6; jewel++) {
                Bullet arc = launch(w, b, formationAim + beat * 25f + jewel * 11f, 2.0f,
                        jewel % 2 == 0 ? DIAMOND : BALL);
                arc.curve = beat * 6f;
            }
        } else {
            // The necklace flicks out in alternating arcs, led by one heavy seal bead,
            // with the crown's fan sweeping the bait.
            aimedFan(w, b, 3.2f, DIAMOND);
            float heading = formationAim + (beat % 2 == 0 ? -38f : 38f);
            Bullet seal = launch(w, b, formationAim + 11f + (beat - 1) * 22f, 3.4f, STAR);
            heavy(seal, .13f);
            for (int link = -3; link <= 3; link++) {
                if (link == 0) continue;
                Bullet bead = launch(w, b, heading + link * 20f, 2.8f, BALL);
                bead.curve = beat % 2 == 0 ? 14f : -14f;
            }
        }
    }

    /** Accent colour and size mark the pattern-breakers; only the General's shells
     * and the ninja's dash cost a whole heart. */
    private void heavy(Bullet shot, float radius) {
        shot.color.set(accent);
        shot.radius = radius;
    }

    static void yarnHead(Bullet bullet, int head) {
        bullet.pos.add(head * .65f, head == 0 ? .4f : 1.05f);
        bullet.prev.set(bullet.pos);
    }

    /** Shift a coherent formation in its local frame, keeping collision history at its origin. */
    private static void offset(Bullet b, float heading, float across, float along) {
        b.pos.add(MathUtils.cosDeg(heading) * along - MathUtils.sinDeg(heading) * across,
                MathUtils.sinDeg(heading) * along + MathUtils.cosDeg(heading) * across);
        b.prev.set(b.pos);
    }

    /** A ring blooms around the bait and closes in: the bullets creep into place, hang there
     * a moment - the tell, like the red light's frozen rings - then rush together. One sector
     * stays open as the walking gap, and it travels a different way each beat. */
    private void bloomRing(GameWorld w, BedBoss b, Bullet.Shape shape, int count, int gapSlots, float gapAngle) {
        float cx = b.baitX(), cy = b.baitY();
        for (int i = 0; i < count; i++) {
            if (i < gapSlots) continue;
            float a = gapAngle + 360f * i / count;
            float x = cx + MathUtils.cosDeg(a) * 2.15f;
            float y = cy + MathUtils.sinDeg(a) * 2.15f;
            if (!Arena.now.contains(x, y, .1f)) continue;
            Bullet petal = launch(w, b, MathUtils.atan2Deg360(cy - y, cx - x), .05f, shape);
            petal.accel = 5.5f;
            petal.maxSpeed = 2.4f * w.getDifficulty();
        }
    }

    /** The King's own fan: nine bullets across seventy degrees, aimed at the bait. */
    private void aimedFan(GameWorld w, BedBoss b, float speed, Bullet.Shape shape) {
        float aim = MathUtils.atan2Deg360(b.baitY() - b.pos.y, b.baitX() - b.pos.x);
        for (int i = 0; i < 11; i++) {
            launch(w, b, aim - 35f + 7f * i, speed, shape);
        }
    }

    private Bullet launch(GameWorld world, BedBoss boss, float heading, float speed, Bullet.Shape shape) {
        Bullet b = world.spawnEnemyBullet(boss.pos.x, boss.pos.y, heading,
                speed * world.getDifficulty() * (boss.isEnraged() ? 1.12f : 1f));
        b.color.set(color);
        b.shape = shape;
        boss.track(b);
        return b;
    }
}
