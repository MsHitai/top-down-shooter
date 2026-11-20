package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

import static com.trush.game.objects.Bullet.Shape.*;

/** Each resident has its own projectile grammar. Headings are committed before the burst:
 * no late tracking, invisible floor emitters, or sudden reversals inside a pattern. */
final class BedPattern {
    private final Color color;

    BedPattern(Color color) {
        this.color = color;
    }

    void fire(GameWorld world, BedBoss boss, Boss kind, int move, int beat, float aim) {
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
            // Static crackles along eight spokes. Whole spokes are separated by broad wedges.
            for (int spoke = 0; spoke < 8; spoke++) {
                for (int spark = 0; spark < 4; spark++) {
                    Bullet s = launch(w, b, aim + spoke * 45f + beat * 9f,
                            2.8f + spark * .4f, DIAMOND);
                    s.curve = spoke % 2 == 0 ? 9f : -9f;
                }
            }
        } else if (move == 2) {
            // Two cheeks puff outwards, leaving a clear corridor down the middle.
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 7; i++) {
                    Bullet puff = launch(w, b, aim + side * (24f + i * 9f), 2.6f + (i % 2) * .6f, CLOUD);
                    puff.curve = side * 12f;
                    puff.radius *= 1.2f;
                }
            }
        } else {
            // A rolling dust clump, with the loose static travelling beside it.
            Bullet clump = launch(w, b, aim + (beat - 1) * 22f, 2.4f, POMPOM);
            clump.radius *= 1.6f;
            clump.accel = 1.3f;
            clump.maxSpeed = 5.2f * w.getDifficulty();
            for (int side = -1; side <= 1; side += 2)
                for (int spark = 0; spark < 7; spark++)
                    launch(w, b, aim + (beat - 1) * 22f + side * (24f + spark * 9f),
                            2.8f + (spark % 2) * .5f, DIAMOND);
        }
    }

    private void sheet(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // A travelling pleat: parallel bullets preserve the same openings as it moves.
            for (int i = -8; i <= 8; i++) {
                if (Math.abs(i) <= 1) continue;
                Bullet fold = launch(w, b, aim, 3.4f, SQUARE);
                offset(fold, aim, i * .17f, -Math.abs(i) * .08f);
            }
        } else if (move == 2) {
            // Scalloped hem, three nested arches rather than an aimed shotgun fan.
            for (int i = -9; i <= 9; i++) {
                Bullet hem = launch(w, b, aim + i * 10f, 2.5f + .45f * beat, SQUARE);
                hem.curve = -i * 1.5f;
            }
        } else {
            // Pillows lead, loose feathers peel away behind them.
            for (int side = -1; side <= 1; side += 2) {
                Bullet pillow = launch(w, b, aim + side * (26f - beat * 5f), 3.3f, PILLOW);
                pillow.radius *= 1.45f;
                for (int i = 0; i < 7; i++) {
                    Bullet feather = launch(w, b, aim + side * (38f + i * 9f), 2.6f, FEATHER);
                    feather.curve = side * 13f;
                }
            }
        }
    }

    private void blades(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // Six shuriken arms, each a short blade of five needles.
            for (int arm = 0; arm < 6; arm++)
                for (int edge = -2; edge <= 2; edge++)
                    launch(w, b, aim + arm * 60f + beat * 10f + edge * 3f, 4.2f, NAIL);
        } else if (move == 2) {
            // Scissor cuts close towards the centre but never retarget the hero.
            for (int side = -1; side <= 1; side += 2)
                for (int cut = -1; cut <= 1; cut++)
                    for (int tip = 0; tip < 4; tip++)
                        launch(w, b, aim + side * (48f - beat * 14f) + cut * 9f,
                                3.2f + tip * .45f, NAIL);
        } else {
            // A fluttering sock distracts from two straight, narrow sword cuts.
            for (int side = -1; side <= 1; side += 2) {
                for (int edge = 0; edge < 5; edge++) {
                    launch(w, b, aim + side * (18f + edge * 8f), 4.2f, NAIL);
                    Bullet sock = launch(w, b, aim + side * (65f + edge * 10f), 2.6f, SOCK);
                    sock.curve = -side * 15f;
                }
            }
        }
    }

    private void yarn(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // Three heads unravel three spirals. The large sectors between arms stay open.
            for (int head = 0; head < 3; head++) {
                for (int strand = -1; strand <= 1; strand++) {
                    for (int bead = 0; bead < 5; bead++) {
                        Bullet thread = launch(w, b, aim + head * 120f + beat * 18f + strand * 12f,
                                2.2f + bead * .4f, BALL);
                        yarnHead(thread, head - 1);
                        thread.curve = 18f;
                    }
                }
            }
        } else if (move == 2) {
            // A braid: paired strands bend together, with a third straight strand between.
            for (int head = -1; head <= 1; head++) {
                float heading = aim + head * 42f;
                for (int side = -1; side <= 1; side += 2) {
                    for (int bead = 0; bead < 4; bead++) {
                        Bullet thread = launch(w, b, heading + side * 16f, 2.3f + bead * .4f, BALL);
                        yarnHead(thread, head);
                        thread.curve = -side * 12f;
                    }
                }
                yarnHead(launch(w, b, heading, 2.6f + beat * .3f, POMPOM), head);
            }
        } else {
            // All three heads unravel; the lead strand advances from head to head each beat.
            for (int head = -1; head <= 1; head++)
                for (int bead = 0; bead < 7; bead++) {
                    Bullet knot = launch(w, b, aim + head * 42f + (beat - 1) * 12f
                                    + (bead % 2 == 0 ? -4f : 4f),
                            2.2f + bead * .3f, bead == 0 ? POMPOM : BALL);
                    yarnHead(knot, head);
                    knot.curve = head * 8f;
                }
        }
    }

    private void clockwork(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // Marching ranks: a missing file is a straight aisle through all three rows.
            for (int file = -8; file <= 8; file++) {
                if (Math.abs(file) <= 1) continue;
                Bullet soldier = launch(w, b, aim, 3.8f, SQUARE);
                offset(soldier, aim, file * .18f, 0f);
            }
        } else if (move == 2) {
            // Clock face: the missing hour advances one tick per beat, never randomly.
            for (int hour = 0; hour < 24; hour++) {
                if ((hour - beat * 2 + 24) % 24 < 4) continue;
                launch(w, b, aim + hour * 15f, 3.3f, STAR);
            }
        } else {
            // Wound springs: slow shells visibly gather speed, with no ricochet clutter.
            for (int i = -7; i <= 7; i++) {
                Bullet spring = launch(w, b, aim + i * 12f, 1.8f + beat * .35f, SQUARE);
                spring.accel = 1.7f;
                spring.maxSpeed = 5.8f * w.getDifficulty();
            }
        }
    }

    private void royalty(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // A crown silhouette travels intact: tall outer points and an open button-studded brim.
            for (int rank = 0; rank < 2; rank++) {
                for (int i = -3; i <= 3; i++) {
                    if (i == 0) continue; // An open centre leaves an aisle through both ranks.
                    Bullet jewel = launch(w, b, aim, 2.2f + rank * .6f + beat * .2f, DIAMOND);
                    offset(jewel, aim, i * .32f, 0f);
                    if (Math.abs(i) == 3) {
                        Bullet point = launch(w, b, aim, 2.2f + rank * .6f + beat * .2f, STAR);
                        offset(point, aim, i * .32f, .45f);
                    }
                }
            }
        } else if (move == 2) {
            // A royal seal: a hollow diamond procession, each outline preserving its centre.
            for (int side = 0; side < 4; side++) {
                for (int bead = 0; bead < 6; bead++) {
                    float t = bead / 6f;
                    float a = side * 90f, next = a + 90f;
                    float across = MathUtils.lerp(MathUtils.cosDeg(a), MathUtils.cosDeg(next), t) * 1.15f;
                    float along = MathUtils.lerp(MathUtils.sinDeg(a), MathUtils.sinDeg(next), t) * 1.15f;
                    // Break one corner of the seal so its denser outline cannot enclose the hero.
                    if (across > .55f && Math.abs(along) < .6f) continue;
                    Bullet seal = launch(w, b, aim + (beat - 1) * 12f, 3.2f, DIAMOND);
                    offset(seal, aim + (beat - 1) * 12f, across, along);
                }
            }
        } else {
            // Button necklace: four empty neighbouring links form a generous escape sector.
            for (int link = 4; link < 28; link++)
                launch(w, b, aim + (link + beat * .5f) * 360f / 28f, 3f + beat * .25f, BALL);
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
        b.life = 3f;
        boss.track(b);
        return b;
    }
}
