package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

import static com.trush.game.enemy.BedBoss.PROJECTILE_SPEED_BOOST;
import static com.trush.game.objects.Bullet.Shape.*;

/**
 * Each resident has its own projectile grammar - no move is shared between residents.
 * Each beat releases one readable formation. Most formations lock their aim,
 * giving the player time to move around or between the shots.
 */
final class BedPattern {
    /** Non-committed turns swing after the hero like the Traffic Light's green streams -
     * slow enough to outrun by circling, too fast to stand still in. */
    private static final float SWING_PER_BEAT = 18f;
    private static final float[] GENERAL_RANK_HEADINGS = {270f, 180f, 0f, 90f};

    private final Color color;
    private final Color accent;
    private final Vector2[] fortAnchor = {new Vector2(), new Vector2()};
    private float formationAim;
    /** Which side of the lane the clothesline's gap sits on; it flips every attack. */
    private int curtainSide = 1;

    BedPattern(Color color, Color accent) {
        this.color = color;
        this.accent = accent;
    }

    void fire(GameWorld world, BedBoss boss, Boss kind, int move, int beat, float aim) {
        boolean committedWall = move == 2 && (kind == Boss.DUST_BUNNY || kind == Boss.BED_MONSTER);
        boolean committedFinisher = move == (kind == Boss.LINT_REGENT ? 2 : 3);
        if (beat == 0) formationAim = aim;
        else if (kind != Boss.SOCK_NINJA && !committedFinisher && !committedWall) {
            // The ninja's sweeps and everyone's finishers stay on the baited line; the
            // middle moves' fronts hunt, capped.
            float diff = ((aim - formationAim + 540f) % 360f) - 180f;
            formationAim += MathUtils.clamp(diff, -SWING_PER_BEAT, SWING_PER_BEAT);
        }
        switch (kind) {
            case DUST_BUNNY -> dust(world, boss, move, beat, aim);
            case BED_MONSTER -> sheet(world, boss, move, beat, aim);
            case SOCK_NINJA -> blades(world, boss, move, beat, aim);
            case YARN_HYDRA -> yarn(world, boss, move, beat, aim);
            case WIND_UP_GENERAL -> clockwork(world, boss, move, beat, aim);
            case LINT_REGENT -> royalty(world, boss, move, beat);
            default -> throw new IllegalArgumentException(kind.name());
        }
    }

    private void dust(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // Every sneeze threads the same seven lanes after the faster opening puff.
            for (int puff = -3; puff <= 3; puff++) {
                launch(w, b, formationAim + puff * 15f, beat == 0 ? 2.7f : 2.5f, DUST_PUFF);
            }
        } else if (move == 2) {
            // Clinging lintels hang briefly off the bunny before sweeping along the baited lane.
            float lane = formationAim;
            float x = b.pos.x + MathUtils.cosDeg(lane) * 1.9f;
            float y = b.pos.y + MathUtils.sinDeg(lane) * 1.9f;
            for (int i = -4; i <= 4; i++) {
                float px = x - MathUtils.sinDeg(lane) * i * .6f;
                float py = y + MathUtils.cosDeg(lane) * i * .6f;
                if (!Arena.now.contains(px, py, .1f)) continue;
                Bullet puff = launchAt(w, b, px, py, lane, .05f, DUST_PUFF);
                puff.accel = 6f * PROJECTILE_SPEED_BOOST;
                puff.maxSpeed = 2.5f * w.getDifficulty() * PROJECTILE_SPEED_BOOST;
            }
        } else if (move == 4) {
            // The sneeze cone: a tight puff cone sweeping across the bait, beat by beat.
            for (int puff = -3; puff <= 3; puff++) {
                Bullet cone = launch(w, b, formationAim + (beat - 2) * 40f + puff * 8f, 2.4f, DUST_PUFF);
                cone.curve = (beat - 2) * 12f;
            }
        } else {
            // The tumble: heavy clumps read the walk - the bunny lobs where you are going.
            float base = beat == 0 ? formationAim
                    : b.leadAngle(w, 3.3f * w.getDifficulty() * PROJECTILE_SPEED_BOOST, .6f);
            float lane = base + (beat == 0 ? 0f : (beat - 2) * 55f);
            Bullet clump = launch(w, b, lane, 3.3f, POMPOM);
            heavy(clump, .19f);
            clump.accel = (b.isEnraged() ? 2.9f : 2.2f) * PROJECTILE_SPEED_BOOST;
            clump.maxSpeed = 6.8f * w.getDifficulty() * PROJECTILE_SPEED_BOOST;
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
            // Two broad pleats travel side by side, with even spacing at the seam.
            for (int side = -1; side <= 1; side += 2) {
                for (int fold = 0; fold < 3; fold++) {
                    Bullet panel = launch(w, b, formationAim, 2.9f, SQUARE);
                    offset(panel, formationAim, side * (.3f + fold * .6f), -fold * .15f);
                }
            }
        } else if (move == 2) {
            // A line of folds rides the baited lane, with trailing corners curling inward.
            for (int post = -2; post <= 2; post++) {
                Bullet fold = launch(w, b, formationAim, 2.3f, SQUARE);
                offset(fold, formationAim, post * .6f, 1f);
            }
            for (int side = -1; side <= 1; side += 2) {
                Bullet corner = launch(w, b, formationAim, 2.3f, SQUARE);
                offset(corner, formationAim, side * 1.2f, .45f);
                corner.curve = -side * 10f; // the sheet wraps as it travels
            }
        } else if (move == 4) {
            // The mattress quake turns a ring of folds with every beat.
            for (int i = 0; i < 16; i++) {
                launch(w, b, 360f * i / 16 + beat * 15f, 2.3f, SQUARE);
            }
        } else {
            // Pillow fort: pillows are lobbed to spots around the bait, land and lie
            // there a beat, then the feathers bloom around each pillow.
            if (beat % 2 == 0) {
                for (int side = -1; side <= 1; side += 2) {
                    float heading = aim + side * 40f;
                    fortAnchor[(beat / 2 + (side + 1) / 2) % 2].set(
                            b.baitX() + MathUtils.cosDeg(heading) * 1.9f,
                            b.baitY() + MathUtils.sinDeg(heading) * 1.9f);
                    Bullet pillow = launch(w, b, heading, 4.1f, PILLOW);
                    pillow.accel = -3.4f * PROJECTILE_SPEED_BOOST;
                    heavy(pillow, .145f);
                    pillow.life = 2.6f;
                }
            } else {
                for (int anchor = 0; anchor < 2; anchor++) {
                    Vector2 spot = fortAnchor[anchor];
                    if (!Arena.now.contains(spot.x, spot.y, .1f)) continue;
                    for (int feather = 0; feather < 5; feather++) {
                        Bullet loose = launchAt(w, b, spot.x, spot.y,
                                aim + anchor * 180f + feather * 72f, .05f, FEATHER);
                        loose.accel = 4.5f * PROJECTILE_SPEED_BOOST;
                        loose.maxSpeed = 2.3f * w.getDifficulty() * PROJECTILE_SPEED_BOOST;
                    }
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
                    launch(w, b, heading + edge * 15f, 3.1f, NEEDLE);
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
            // The clothesline rides the baited heading, with a sock flicking off one flank.
            // The scissor fans follow the curtains.
            if (beat < 2) {
                if (beat == 0) curtainSide = -curtainSide;
                for (int i = 0; i < 6; i++) {
                    float slot = i - 2.5f;
                    Bullet needle = launch(w, b, formationAim, 2.8f, NEEDLE);
                    offset(needle, formationAim, slot * .6f, -Math.abs(slot) * .12f);
                    if (Math.abs(slot) < 1f) needle.color.set(accent);
                }
                Bullet sock = launch(w, b, formationAim - curtainSide * 26f, 2.2f, SOCK);
                sock.curve = curtainSide * 14f;
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
            // The knit: each beat one head lays a V - a fast leg and a slow leg - with
            // the marker bead between. The Vs march across the sheets; the diamond gaps
            // between legs are the streets.
            for (int leg = -1; leg <= 1; leg += 2) {
                Bullet strand = launch(w, b, facing + leg * 13f, leg < 0 ? 3.1f : 2.3f, BALL);
                yarnHead(strand, head);
            }
            Bullet marker = launch(w, b, facing, 2.7f, BALL);
            yarnHead(marker, head);
            marker.color.set(accent);
        } else if (move == 2) {
            // Full loops of evenly spaced beads rotate as they expand across the floor.
            float start = aim + 150f + beat * 55f;
            for (int i = 0; i < 19; i++) {
                Bullet bead = launch(w, b, start + 360f * i / 19f, 2.1f, BALL);
                bead.curve = beat % 2 == 0 ? 8f : -8f;
            }
        } else if (move == 4) {
            // The braid: two strands weave over and under, crossing on the bait.
            for (int side = -1; side <= 1; side += 2) {
                for (int bead = 0; bead < 3; bead++) {
                    Bullet strand = launch(w, b, formationAim + side * (7f + bead * 14f), 2.4f, BALL);
                    yarnHead(strand, head);
                    strand.curve = side * -16f;
                }
            }
        } else {
            // The heads trade a heavy knot down the line - the one throw that reads your
            // walk - while the head on duty sweeps the flanks with beads.
            float lane = beat == 0 ? formationAim
                    : b.leadAngle(w, 3.1f * w.getDifficulty() * PROJECTILE_SPEED_BOOST, .5f);
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
            marchRanks(w, b, GENERAL_RANK_HEADINGS[beat]);
        } else if (move == 2) {
            // All four rings keep the same evenly spaced spokes.
            for (int tick = 0; tick < 14; tick++) {
                launch(w, b, formationAim + tick * (360f / 14f), 3.3f, STAR);
            }
        } else if (move == 4) {
            // The bell tolls: a slow star ring rolls out, the missing chime circling round.
            for (int i = 0; i < 18; i++) {
                if (i == beat * 3 || i == beat * 3 + 1 || i == (beat * 3 + 2) % 18) continue;
                launch(w, b, 360f * i / 18, 2.4f, STAR);
            }
        } else {
            // Spring shells fan out slowly, gather speed, and ricochet once.
            for (int spring = -3; spring <= 3; spring++) {
                Bullet shell = launch(w, b, aim + spring * 20f, 1.4f + beat * .32f, CARTRIDGE);
                shell.accel = b.isEnraged() ? 1.65f : 1.1f;
                shell.maxSpeed = 4.6f * w.getDifficulty();
                shell.bounces = 1;
                b.heavyShell(shell);
            }
        }
    }

    private void marchRanks(GameWorld world, BedBoss boss, float heading) {
        Vector2 forward = new Vector2(MathUtils.cosDeg(heading), MathUtils.sinDeg(heading));
        Vector2 across = new Vector2(-forward.y, forward.x);
        Arena arena = Arena.now;
        float wallX = (forward.x > 0f ? arena.right - boss.pos.x : boss.pos.x - arena.left) / Math.abs(forward.x);
        float wallY = (forward.y > 0f ? arena.top - boss.pos.y : boss.pos.y - arena.bottom) / Math.abs(forward.y);
        // Lean toward the first wall across the front so even a square-on charge peels away.
        float lean = Math.copySign(.28f, wallX < wallY ? across.x * forward.x : across.y * forward.y);
        float travel = Vector2.len(arena.worldW, arena.worldH) + 1f;
        float chargeDistance = 2.4f;
        float foldFraction = .4f;
        int steps = 15;
        float foldSpeedScale = 1.05f;
        float stepDistance = chargeDistance / (steps * foldSpeedScale);
        int tiltSteps = 6;
        float chargeScale = 2.1f;
        // Blend a steady advance with a fourth-power ramp so the line reaches
        // charge speed at the join, even with the shorter fold timing.
        float marchScale = (4f - chargeScale / foldSpeedScale) / 3f;
        for (int rank = 0; rank < 3; rank++) {
            for (int column = 0; column < 4; column++) {
                float slot = column - 1.5f;
                float file = slot * .24f;
                float lineSlot = (slot + Math.copySign(2f * rank, slot)) * .18f;
                Vector2[] course = new Vector2[steps + tiltSteps + 2];
                for (int step = 0; step <= steps; step++) {
                    float progress = step / (float) steps;
                    float square = progress * progress;
                    float fold = Math.min(progress / foldFraction, 1f);
                    float spread = fold * fold * (3f - 2f * fold);
                    float advance = MathUtils.lerp(square * square, progress, marchScale);
                    float depth = advance * chargeDistance - (1f - spread) * rank * .32f;
                    course[step] = boss.pos.cpy().mulAdd(forward, depth)
                            .mulAdd(across, MathUtils.lerp(file, lineSlot, spread));
                }
                // Tip the joined line as it charges, then keep the stagger through the wall.
                for (int step = 1; step <= tiltSteps; step++) {
                    float depth = chargeScale * stepDistance * step + lean * lineSlot * step / tiltSteps;
                    course[steps + step] = course[steps].cpy().mulAdd(forward, depth);
                }
                course[steps + tiltSteps + 1] = course[steps + tiltSteps].cpy().mulAdd(forward, travel);
                Vector2 start = course[0];
                Bullet soldier = launchAt(world, boss, start.x, start.y, heading, 1.9f, SQUARE);
                soldier.followCourse(course, stepDistance, chargeScale);
            }
        }
    }

    private void royalty(GameWorld w, BedBoss b, int move, int beat) {
        if (move == 1) {
            // The collectors' patrol: a tax collector rides the bedroom's walls while
            // the border lights up with button tolls, still where they fall and
            // short-lived - the middle of the room stays open.
            Arena arena = Arena.now;
            boolean east = beat == 0;
            float x0 = east ? arena.right - .3f : arena.left + .3f;
            float x1 = east ? arena.left + .3f : arena.right - .3f;
            Vector2 nearCorner = new Vector2(x1, arena.bottom + .3f);
            Vector2 farCorner = new Vector2(x0, arena.bottom + .3f);
            Bullet collector = launch(w, b, 0f, 3.1f, STAR);
            collector.color.set(accent);
            collector.followCourse(new Vector2[]{new Vector2(b.pos.x, b.pos.y),
                    nearCorner, farCorner, new Vector2(x0, arena.top - .3f)});
            for (int toll = 0; toll < 4; toll++) {
                float t = (toll + 1) / 5f;
                float bx = MathUtils.lerp(nearCorner.x, farCorner.x, t);
                float by = MathUtils.lerp(nearCorner.y, farCorner.y, t);
                Bullet button = launchAt(w, b, bx, by, 90f, 0f, BALL);
                button.life = 2.8f;
            }
        } else if (move == 3) {
            // The sceptre sweep: a wide, slow arc of jewels rolling across the floor.
            for (int jewel = -6; jewel <= 6; jewel++) {
                Bullet arc = launch(w, b, formationAim + beat * 25f + jewel * 11f, 2.0f,
                        jewel % 2 == 0 ? DIAMOND : BALL);
                arc.curve = beat * 6f;
            }
        } else {
            // The necklace flicks out in alternating arcs, led by one heavy seal bead,
            // and reacquires nothing - walking away from the wind-up defeats it.
            float heading = formationAim + (beat % 2 == 0 ? -38f : 38f);
            Bullet seal = launch(w, b, formationAim + 11f + (beat - 1) * 22f, 3.4f, STAR);
            heavy(seal, .13f);
            for (int link = -3; link <= 3; link++) {
                Bullet bead = launch(w, b, heading + link * 20f, 2.8f, BALL);
                bead.curve = beat % 2 == 0 ? 14f : -14f;
            }
        }
    }

    /** Accent colour and size mark the pattern-breakers. */
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

    private Bullet launch(GameWorld world, BedBoss boss, float heading, float speed, Bullet.Shape shape) {
        return launchAt(world, boss, boss.pos.x, boss.pos.y, heading, speed, shape);
    }

    private Bullet launchAt(GameWorld world, BedBoss boss, float x, float y, float heading,
                            float speed, Bullet.Shape shape) {
        Bullet b = boss.spawnShot(world, x, y, heading, speed * (boss.isEnraged() ? 1.12f : 1f));
        b.color.set(color);
        b.shape = shape;
        return b;
    }
}
