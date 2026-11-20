package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

import static com.trush.game.objects.Bullet.Shape.*;

/** Each resident has its own projectile grammar. Headings are committed when each volley fires:
 * no late tracking, invisible floor emitters, or sudden reversals inside a pattern. */
final class BedPattern {
    private final Color color;
    private final Color accent;
    private float formationAim;
    private float rankSpeed = 1f;

    BedPattern(Color color, Color accent) {
        this.color = color;
        this.accent = accent;
    }

    void fire(GameWorld world, BedBoss boss, Boss kind, int move, int beat, float aim) {
        if (beat == 0) formationAim = aim;
        // Two speed-separated ranks sustain each front instead of letting one thin volley pass.
        // Both use the same geometry and escape sector; the rear rank never fills its opening.
        for (int rank = 0; rank < 2; rank++) {
            rankSpeed = rank == 0 ? 1f : .76f;
            fireRank(world, boss, kind, move, beat, aim);
        }
    }

    private void fireRank(GameWorld world, BedBoss boss, Boss kind, int move, int beat, float aim) {
        switch (kind) {
            case DUST_BUNNY -> dust(world, boss, move, beat, aim);
            case BED_MONSTER -> sheet(world, boss, move, beat, aim);
            case SOCK_NINJA -> blades(world, boss, move, beat, aim);
            case YARN_HYDRA -> yarn(world, boss, move, beat, aim);
            case WIND_UP_GENERAL -> clockwork(world, boss, move, beat, aim);
            case LINT_REGENT -> royalty(world, boss, move, beat, aim);
            default -> throw new IllegalArgumentException(kind.name());
        }
    }

    private void dust(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // Sixteen crackling spokes turn between pulses, leaving narrow paths between their tips.
            for (int spoke = 0; spoke < 16; spoke++) {
                for (int spark = 0; spark < 4; spark++) {
                    Bullet s = launch(w, b, aim + spoke * 22.5f + beat * 7.5f,
                            2.8f + spark * .4f, DIAMOND);
                    s.curve = (spoke == 0 ? 0f : spoke % 2 == 0 ? 9f : -9f) * rankSpeed;
                }
            }
        } else if (move == 2) {
            // A scalloped cloud front surrounds the bunny, with an offset passage to walk into.
            ring(w, b, beat, 48, 2.7f, CLOUD, 0f);
        } else {
            // A rolling dust clump, with the loose static travelling beside it.
            Bullet clump = launch(w, b, aim, 2.4f, POMPOM);
            clump.radius *= 1.6f;
            clump.color.set(accent);
            clump.accel = 1.3f * rankSpeed;
            clump.maxSpeed = 5.2f * rankSpeed * w.getDifficulty();
            for (int side = -1; side <= 1; side += 2)
                for (int spark = 0; spark < 18; spark++)
                    launch(w, b, aim + side * (10f + spark * 10f),
                            2.8f + (spark % 2) * .5f, DIAMOND);
        }
    }

    private void sheet(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // Eight travelling pleats cover every side; spaces between neighbouring folds remain walkable.
            for (int side = 0; side < 8; side++) {
                float heading = aim + side * 45f;
                for (int i = -4; i <= 4; i++) {
                    if (Math.abs(i) == 2) continue;
                    Bullet fold = launch(w, b, heading, 3.1f, SQUARE);
                    offset(fold, heading, i * .25f, -Math.abs(i) * .06f);
                }
            }
        } else if (move == 2) {
            // Scalloped hem, three nested arches rather than an aimed shotgun fan.
            for (int i = -18; i < 18; i++) {
                Bullet hem = launch(w, b, formationAim + i * 10f + beat * 3f, 2.7f, SQUARE);
                hem.curve = -MathUtils.sinDeg(i * 10f) * 6f * rankSpeed;
            }
        } else {
            // Pillows lead, loose feathers peel away behind them.
            for (int side = -1; side <= 1; side += 2) {
                Bullet pillow = launch(w, b, aim + side * (26f - beat * 13f), 3.3f, PILLOW);
                pillow.radius *= 1.45f;
                pillow.color.set(accent);
                for (int i = 0; i < 16; i++) {
                    Bullet feather = launch(w, b, aim + side * (38f + i * 9f), 2.6f, FEATHER);
                    feather.curve = side * 13f * rankSpeed;
                }
            }
        }
    }

    private void blades(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // Twelve shuriken arms, each a short blade of five needles.
            for (int arm = 0; arm < 12; arm++)
                for (int edge = -2; edge <= 2; edge++)
                    launch(w, b, aim + arm * 30f + beat * 5f + edge * 3f, 4.2f, NAIL);
        } else if (move == 2) {
            // An open needle ring carries the scissor cuts across the room.
            ring(w, b, beat, 36, 2.5f, NAIL, 0f);
            // Reacquired scissor cuts close onto the hero on the final beat.
            for (int side = -1; side <= 1; side += 2)
                for (int cut = -1; cut <= 1; cut++)
                    for (int tip = 0; tip < 4; tip++) {
                        Bullet scissor = launch(w, b, aim + side * (12f - beat * 6f) + cut * 5f,
                                2.2f + tip * .25f, NAIL);
                        scissor.color.set(accent);
                    }
        } else {
            // Aimed needles make standing between the flanking cuts unsafe.
            for (int needle = -1; needle <= 1; needle++) {
                Bullet pinned = launch(w, b, aim + needle * 9f, 3.8f, NAIL);
                pinned.color.set(accent);
            }
            // Fluttering socks frame the straight sword cuts.
            for (int side = -1; side <= 1; side += 2) {
                for (int edge = 0; edge < 12; edge++) {
                    launch(w, b, aim + side * (18f + edge * 8f), 4.2f, NAIL);
                    Bullet sock = launch(w, b, aim + side * (65f + edge * 10f), 2.6f, SOCK);
                    sock.curve = -side * 15f * rankSpeed;
                }
            }
        }
    }

    private void yarn(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move != 3) {
            Bullet knot = launch(w, b, aim, 2.6f, POMPOM);
            yarnHead(knot, 0);
            knot.vel.setAngleDeg(MathUtils.atan2Deg360(b.targetY(w) - knot.pos.y,
                    b.targetX(w) - knot.pos.x));
            knot.color.set(accent);
        }
        if (move == 1) {
            // Three heads unravel broad spirals, with small seams between adjacent strands.
            for (int head = 0; head < 3; head++) {
                for (int strand = -2; strand <= 2; strand++) {
                    for (int bead = 0; bead < 5; bead++) {
                        Bullet thread = launch(w, b, aim + head * 120f + beat * 18f + strand * 20f,
                                2.2f + bead * .4f, BALL);
                        yarnHead(thread, head - 1);
                        thread.curve = 18f * rankSpeed;
                    }
                }
            }
        } else if (move == 2) {
            // A braid: paired strands bend together, with a third straight strand between.
            for (int head = -1; head <= 1; head++) {
                float heading = aim + head * 120f;
                for (int side = -3; side <= 3; side++) {
                    if (side == 0) continue;
                    for (int bead = 0; bead < 4; bead++) {
                        Bullet thread = launch(w, b, heading + side * 16f, 2.3f + bead * .4f, BALL);
                        yarnHead(thread, head);
                        thread.curve = -Math.signum(side) * 8f * rankSpeed;
                    }
                }
                Bullet lead = launch(w, b, heading, 2.6f + beat * .3f, POMPOM);
                yarnHead(lead, head);
                lead.color.set(accent);
            }
        } else {
            // All three heads unravel; the lead strand advances from head to head each beat.
            for (int head = -1; head <= 1; head++)
                for (int bead = 0; bead < 7; bead++) {
                    Bullet knot = launch(w, b, aim + head * 120f + (beat - 1) * 6f + (bead - 3) * 12f,
                            2.2f + bead * .3f, bead == 0 ? POMPOM : BALL);
                    yarnHead(knot, head);
                    knot.curve = head * 8f * rankSpeed;
                    if (head == 0 && bead == 0) {
                        // Aim from the actual mouth, rather than a parallel line below it.
                        knot.vel.setAngleDeg(MathUtils.atan2Deg360(b.targetY(w) - knot.pos.y,
                                b.targetX(w) - knot.pos.x));
                        knot.color.set(accent);
                    }
                }
        }
    }

    private void clockwork(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // A clockwork perimeter, with four raised ranks and a missing file off the firing line.
            ring(w, b, beat, 48, 3.1f, SQUARE, .25f);
        } else if (move == 2) {
            // The missing sector stays committed for all three faces, away from the hero's starting line.
            ring(w, b, 0, 48, 3.3f, STAR, 0f);
        } else {
            // Wound springs: slow shells visibly gather speed, with no ricochet clutter.
            for (int i = -18; i < 18; i++) {
                Bullet spring = launch(w, b, aim + i * 10f, 1.8f + beat * .35f, SQUARE);
                spring.accel = 1.7f * rankSpeed;
                spring.maxSpeed = 5.8f * rankSpeed * w.getDifficulty();
                spring.color.set(accent);
            }
        }
    }

    private void royalty(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            ring(w, b, beat, 36, 2.5f, BALL, .2f);
            // A crown silhouette travels intact: tall outer points and an open button-studded brim.
            for (int rank = 0; rank < 2; rank++) {
                for (int i = -3; i <= 3; i++) {
                    if (i == 0) continue; // An open centre leaves an aisle through both ranks.
                    Bullet jewel = launch(w, b, aim, 2.5f, DIAMOND);
                    offset(jewel, aim, i * .32f, -rank * .45f);
                    if (Math.abs(i) == 3) {
                        Bullet point = launch(w, b, aim, 2.5f, STAR);
                        offset(point, aim, i * .32f, .45f - rank * .45f);
                        point.color.set(accent);
                    }
                }
            }
        } else if (move == 2) {
            // Aimed buttons accompany the hollow royal seal.
            ring(w, b, beat, 48, 2.6f, BALL, 0f);
            // A royal seal: a hollow diamond procession, each outline preserving its centre.
            for (int side = 0; side < 4; side++) {
                for (int bead = 0; bead < 6; bead++) {
                    float t = bead / 6f;
                    float a = side * 90f, next = a + 90f;
                    float across = MathUtils.lerp(MathUtils.cosDeg(a), MathUtils.cosDeg(next), t) * 1.15f;
                    float along = MathUtils.lerp(MathUtils.sinDeg(a), MathUtils.sinDeg(next), t) * 1.15f;
                    // Break one corner of the seal so its denser outline cannot enclose the hero.
                    if (across > .55f && Math.abs(along) < .6f) continue;
                    Bullet seal = launch(w, b, aim + (beat - 1) * 12f, 2.4f, DIAMOND);
                    offset(seal, aim + (beat - 1) * 12f, across, along);
                }
            }
        } else {
            // The necklace surrounds the room; its missing links move a little between waves.
            ring(w, b, beat, 48, 3f, BALL, .15f);
        }
    }

    /** Broad fronts leave a local opening, rather than a safe half of the arena.
     * The opening is offset from the initial aim so holding fire in place is never the solution.
     * Four-degree steps let the player follow it without a sudden closing gate. */
    private void ring(GameWorld w, BedBoss b, int beat, int count,
                      float speed, Bullet.Shape shape, float ripple) {
        for (int i = 0; i < count; i++) {
            float relative = i * 360f / count;
            if (Math.abs(MathUtils.sinDeg((relative - 30f) * .5f)) < MathUtils.sinDeg(9f)) continue;
            float heading = formationAim + relative + beat * 4f;
            launch(w, b, heading, speed + ripple * MathUtils.cosDeg(relative * 4f), shape);
        }
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
        Bullet b = world.spawnEnemyBullet(boss.pos.x, boss.pos.y, heading, speed * rankSpeed * world.getDifficulty());
        b.color.set(color);
        b.shape = shape;
        b.life = 5f;
        boss.track(b);
        return b;
    }
}
