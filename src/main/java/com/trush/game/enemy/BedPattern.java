package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

import static com.trush.game.objects.Bullet.Shape.*;

/**
 * Each resident has its own projectile grammar, tuned to the Gorgun contract: fronts are slow and
 * few enough that the spaces between them are streets you walk through, not seams you thread.
 * One volley per beat - pressure comes from the three beats sweeping in succession, never from
 * overlapping ranks fired in the same instant. Headings are committed when each volley fires:
 * no late tracking, invisible floor emitters, or sudden reversals inside a pattern.
 */
final class BedPattern {
    private final Color color;
    private final Color accent;
    private float formationAim;

    BedPattern(Color color, Color accent) {
        this.color = color;
        this.accent = accent;
    }

    void fire(GameWorld world, BedBoss boss, Boss kind, int move, int beat, float aim) {
        if (beat == 0) formationAim = aim;
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
        // In rage, a faster spark leads each spoke or cloud front and marks the aimed lane.
        if (b.isEnraged() && move != 3) {
            float heading = MathUtils.atan2Deg360(b.targetY(w) - b.pos.y, b.targetX(w) - b.pos.x);
            launch(w, b, heading, 3.2f, DIAMOND).color.set(accent);
        }
        if (move == 1) {
            // Ten crackling spokes turn between pulses, leaving wide paths between their tips.
            for (int spoke = 0; spoke < 10; spoke++) {
                for (int spark = 0; spark < 3; spark++) {
                    Bullet s = launch(w, b, aim + spoke * 36f + beat * 9f,
                            1.75f + spark * .25f, DIAMOND);
                    s.curve = (spoke == 0 ? 0f : spoke % 2 == 0 ? 9f : -9f);
                }
            }
        } else if (move == 2) {
            // A scalloped cloud front surrounds the bunny, with an offset passage to walk into.
            ring(w, b, beat, 20, 1.7f, DUST_PUFF, 0f);
        } else {
            // A rolling dust clump, with the loose static travelling beside it.
            Bullet clump = launch(w, b, aim, 1.8f, POMPOM);
            clump.radius *= 1.6f;
            clump.color.set(accent);
            clump.accel = b.isEnraged() ? 1.6f : .9f;
            clump.maxSpeed = (b.isEnraged() ? 4.4f : 3.6f) * w.getDifficulty();
            for (int side = -1; side <= 1; side += 2)
                for (int spark = 0; spark < 7; spark++)
                    launch(w, b, aim + side * (14f + spark * 13f),
                            1.8f + (spark % 2) * .4f, DIAMOND);
        }
    }

    private void sheet(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // Eight travelling pleats cover every side; the spread between folds stays a street
            // you can stand in, like walking between the hanging folds of a curtain.
            for (int side = 0; side < 8; side++) {
                float heading = aim + side * 45f + beat * 11f;
                for (float across : new float[]{-1.2f, -.4f, .4f, 1.2f}) {
                    Bullet fold = launch(w, b, heading, 1.9f, SQUARE);
                    offset(fold, heading, across, -Math.abs(across) * .06f);
                }
            }
        } else if (move == 2) {
            // Scalloped hem, three nested arches rather than an aimed shotgun fan.
            for (int i = -8; i < 8; i++) {
                Bullet hem = launch(w, b, formationAim + i * 22.5f + beat * 7f, 1.7f, SQUARE);
                hem.curve = -MathUtils.sinDeg(i * 22.5f) * 5f;
            }
        } else {
            // Pillows lead, loose feathers peel away behind them.
            for (int side = -1; side <= 1; side += 2) {
                Bullet pillow = launch(w, b, aim + side * (26f - beat * 13f), 2.3f, PILLOW);
                pillow.radius *= 1.45f;
                pillow.color.set(accent);
                for (int i = 0; i < 8; i++) {
                    Bullet feather = launch(w, b, aim + side * (38f + i * 15f), 1.9f, FEATHER);
                    feather.curve = side * 13f;
                }
            }
        }
    }

    private void blades(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // Eight shuriken arms, each a short blade of three needles, turning between pulses.
            for (int arm = 0; arm < 8; arm++)
                for (int edge = -1; edge <= 1; edge++)
                    launch(w, b, aim + arm * 45f + beat * 11f + edge * 3f, 2.3f, NEEDLE);
        } else if (move == 2) {
            // An open needle ring carries the scissor cuts across the room.
            ring(w, b, beat, 18, 1.8f, NEEDLE, 0f);
            // Reacquired scissor cuts close onto the hero on the final beat.
            for (int side = -1; side <= 1; side += 2)
                for (int cut = -1; cut <= 1; cut += 2)
                    for (int tip = 0; tip < 3; tip++) {
                        Bullet scissor = launch(w, b, aim + side * (12f - beat * 6f) + cut * 6f,
                                2f + tip * .2f, NEEDLE);
                        scissor.color.set(accent);
                    }
        } else {
            // Aimed needles make standing between the flanking cuts unsafe.
            for (int needle = -1; needle <= 1; needle++) {
                Bullet pinned = launch(w, b, aim + needle * 9f, 2.6f, NEEDLE);
                pinned.color.set(accent);
            }
            // Fluttering socks frame the straight sword cuts - wide enough apart that walking
            // between the flanks, away from the pinned line, is the intended read.
            for (int side = -1; side <= 1; side += 2) {
                for (int edge = 0; edge < 4; edge++) {
                    launch(w, b, aim + side * (26f + edge * 16f), 2.2f, NEEDLE);
                    Bullet sock = launch(w, b, aim + side * (78f + edge * 18f), 1.7f, SOCK);
                    sock.curve = -side * 12f;
                }
            }
        }
    }

    private void yarn(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move != 3) {
            Bullet knot = launch(w, b, aim, 1.8f, POMPOM);
            yarnHead(knot, 0);
            knot.vel.setAngleDeg(MathUtils.atan2Deg360(b.targetY(w) - knot.pos.y,
                    b.targetX(w) - knot.pos.x));
            knot.color.set(accent);
        }
        if (move == 1) {
            // Three heads unravel broad spirals that turn between beats, with seams between strands.
            for (int head = 0; head < 3; head++) {
                for (int strand = -1; strand <= 1; strand++) {
                    for (int bead = 0; bead < 3; bead++) {
                        Bullet thread = launch(w, b, aim + head * 120f + beat * 30f + strand * 26f,
                                1.6f + bead * .3f, BALL);
                        yarnHead(thread, head - 1);
                        thread.curve = 16f;
                    }
                }
            }
        } else if (move == 2) {
            // A braid: paired strands bend together, with a third straight strand between.
            for (int head = -1; head <= 1; head++) {
                float heading = aim + head * 120f + beat * 15f;
                for (int side = -1; side <= 1; side += 2) {
                    for (int bead = 0; bead < 2; bead++) {
                        Bullet thread = launch(w, b, heading + side * 16f, 1.7f + bead * .3f, BALL);
                        yarnHead(thread, head);
                        thread.curve = -side * 8f;
                    }
                }
                Bullet lead = launch(w, b, heading, 2.1f + beat * .3f, POMPOM);
                yarnHead(lead, head);
                lead.color.set(accent);
            }
        } else {
            // All three heads unravel, sweeping around the bed between beats.
            for (int head = -1; head <= 1; head++)
                for (int bead = 0; bead < 5; bead++) {
                    Bullet knot = launch(w, b, aim + head * 120f + beat * 30f + (bead - 2) * 12f,
                            1.6f + bead * .25f, bead == 0 ? POMPOM : BALL);
                    yarnHead(knot, head);
                    knot.curve = head * 8f;
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
            // A clockwork perimeter with raised and lowered ranks, and a missing file off the line.
            ring(w, b, beat, 20, 1.8f, SQUARE, .35f);
        } else if (move == 2) {
            // The missing sector stays committed for all three faces, away from the hero's starting line.
            ring(w, b, 0, 20, 1.9f, STAR, 0f);
        } else {
            // Wound springs: slow shells visibly gather speed, with no ricochet clutter.
            for (int i = -8; i < 8; i++) {
                Bullet spring = launch(w, b, aim + i * 22.5f, 1.3f + beat * .3f, CARTRIDGE);
                spring.accel = b.isEnraged() ? 1.6f : .9f;
                spring.maxSpeed = (b.isEnraged() ? 4f : 3.2f) * w.getDifficulty();
                spring.color.set(accent);
            }
        }
    }

    private void royalty(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            ring(w, b, beat, 18, 1.7f, BALL, .2f);
            // A crown silhouette travels intact: tall outer points and an open button-studded brim.
            for (int rank = 0; rank < 2; rank++) {
                for (int i = -2; i <= 2; i++) {
                    if (i == 0) continue; // An open centre leaves an aisle through both ranks.
                    Bullet jewel = launch(w, b, aim, 1.8f, DIAMOND);
                    offset(jewel, aim, i * .6f, -rank * .5f);
                    if (Math.abs(i) == 2) {
                        Bullet point = launch(w, b, aim, 1.8f, STAR);
                        offset(point, aim, i * .6f, .5f - rank * .5f);
                        point.color.set(accent);
                    }
                }
            }
        } else if (move == 2) {
            // Aimed buttons accompany the hollow royal seal.
            ring(w, b, beat, 18, 1.8f, BALL, 0f);
            // A royal seal: a hollow diamond procession, each outline preserving its centre.
            for (int side = 0; side < 4; side++) {
                for (int bead = 0; bead < 4; bead++) {
                    float t = bead / 4f;
                    float a = side * 90f, next = a + 90f;
                    float across = MathUtils.lerp(MathUtils.cosDeg(a), MathUtils.cosDeg(next), t) * 1.1f;
                    float along = MathUtils.lerp(MathUtils.sinDeg(a), MathUtils.sinDeg(next), t) * 1.1f;
                    // Break one corner of the seal so its denser outline cannot enclose the hero.
                    if (across > .55f && Math.abs(along) < .6f) continue;
                    Bullet seal = launch(w, b, aim + (beat - 1) * 12f, 1.7f, DIAMOND);
                    offset(seal, aim + (beat - 1) * 12f, across, along);
                }
            }
        } else {
            // The necklace surrounds the room; its missing links move a little between waves.
            ring(w, b, beat, 18, 1.8f, BALL, .15f);
        }
    }

    /** Broad fronts leave a local opening, rather than a safe half of the arena.
     * The opening is offset from the initial aim so holding fire in place is never the solution.
     * Six-degree steps let the player follow it without a sudden closing gate. */
    private void ring(GameWorld w, BedBoss b, int beat, int count,
                      float speed, Bullet.Shape shape, float ripple) {
        for (int i = 0; i < count; i++) {
            float relative = i * 360f / count;
            if (Math.abs(MathUtils.sinDeg((relative - 30f) * .5f)) < MathUtils.sinDeg(9f)) continue;
            float heading = formationAim + relative + beat * 6f;
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
        Bullet b = world.spawnEnemyBullet(boss.pos.x, boss.pos.y, heading, speed * world.getDifficulty());
        b.color.set(color);
        b.shape = shape;
        b.life = 5f;
        boss.track(b);
        return b;
    }
}
