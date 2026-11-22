package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

import static com.trush.game.objects.Bullet.Shape.*;

/**
 * Each resident has its own projectile grammar, tuned to the Gorgun contract: fronts are slow and
 * few enough that the spaces between them are streets you walk through, not seams you thread.
 * One volley per beat. Most turns commit their aim on the first beat, so players can bait
 * a formation and walk around it while individual shots keep their visible trajectories.
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
        if (move == 1) {
            // Static jumps between broad spokes. Each pulse rotates a little farther.
            for (int spoke = 0; spoke < 8; spoke++) {
                Bullet spark = launch(w, b, formationAim + spoke * 45f + beat * 12f, 2.1f, DIAMOND);
                spark.curve = spoke % 2 == 0 ? 12f : -12f;
            }
        } else if (move == 2) {
            // A wide breath rolls out in one direction. Walk around either end.
            for (int mote = -4; mote <= 4; mote++) {
                Bullet puff = launch(w, b, formationAim + mote * 16f + (beat - 1) * 14f,
                        1.65f + .12f * (4 - Math.abs(mote)), DUST_PUFF);
                puff.radius *= 1.25f;
            }
        } else {
            // The clump accelerates along one lane; the crumbs peel away from it.
            float heading = formationAim + beat * 22f;
            Bullet clump = launch(w, b, heading, 1.2f, POMPOM);
            clump.radius *= 1.7f;
            clump.color.set(accent);
            clump.accel = b.isEnraged() ? 1.9f : 1.4f;
            clump.maxSpeed = 4.2f * w.getDifficulty();
            for (int side = -1; side <= 1; side += 2) {
                for (int crumb = 1; crumb <= 2; crumb++) {
                    Bullet spark = launch(w, b, heading + side * (14f + crumb * 13f), 1.65f, DIAMOND);
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
                    Bullet panel = launch(w, b, formationAim, 2f, SQUARE);
                    offset(panel, formationAim, side * (.67f + fold * .42f), -fold * .15f);
                }
            }
        } else if (move == 2) {
            // The hem fans out from alternating sides, slow enough to step between squares.
            float heading = formationAim + (beat % 2 == 0 ? -24f : 24f);
            for (int fold = -4; fold <= 4; fold++) {
                Bullet hem = launch(w, b, heading + fold * 21f, 1.9f, SQUARE);
                hem.curve = -fold * 2f;
            }
        } else {
            // One big pillow followed by a small tuft of drifting feathers.
            float speed = beat == 2 ? 3.25f : 2.6f;
            float heading = beat == 2 ? b.leadAngle(w, speed * w.getDifficulty(), .7f)
                    : formationAim + (beat - 1) * 25f;
            Bullet pillow = launch(w, b, heading, speed, PILLOW);
            pillow.radius *= 1.7f;
            pillow.color.set(accent);
            for (int side = -1; side <= 1; side += 2) {
                for (int feather = 1; feather <= 2; feather++) {
                    Bullet loose = launch(w, b, heading + side * feather * 21f, 1.65f, FEATHER);
                    loose.curve = side * 11f;
                }
            }
        }
    }

    private void blades(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // Four paired shuriken blades sweep a quarter turn in short, quick steps.
            for (int arm = 0; arm < 4; arm++) {
                for (int edge = -1; edge <= 1; edge += 2) {
                    launch(w, b, formationAim + arm * 90f + beat * 23f + edge * 7f, 3.1f, NEEDLE);
                }
            }
        } else if (move == 2) {
            // Scissors close across the original line. The outside stays open.
            for (int side = -1; side <= 1; side += 2) {
                float blade = formationAim + side * (42f - beat * 17f);
                launch(w, b, blade, 3.6f, NEEDLE).color.set(accent);
                launch(w, b, blade + side * 10f, 2.8f, NEEDLE);
                Bullet sock = launch(w, b, blade + side * 31f, 1.75f, SOCK);
                sock.curve = -side * 10f;
            }
        } else {
            // Quick needles strafe across the baited line; the slow socks drift outside them.
            float heading = formationAim + (beat - 1) * 16f;
            for (int needle = -1; needle <= 1; needle++) {
                launch(w, b, heading + needle * 15f, 3.25f, NEEDLE).color.set(accent);
            }
            for (int side = -1; side <= 1; side += 2) {
                Bullet sock = launch(w, b, heading + side * 48f, 1.7f, SOCK);
                sock.curve = -side * 15f;
            }
        }
    }

    private void yarn(GameWorld w, BedBoss b, int move, int beat, float aim) {
        int head = beat - 1;
        float facing = formationAim + head * 35f;
        if (move == 1) {
            // Each head spins a fan in turn; the idle two heads leave room to pass.
            for (int bead = -4; bead <= 4; bead++) {
                Bullet thread = launch(w, b, facing + bead * 34f, 2f, BALL);
                yarnHead(thread, head);
                thread.curve = head < 0 ? -22f : 22f;
            }
        } else if (move == 2) {
            // Two curved strands peel apart from a straight, slower knot.
            for (int side = -1; side <= 1; side += 2) {
                for (int bead = 0; bead < 3; bead++) {
                    Bullet thread = launch(w, b, facing + side * (16f + bead * 13f), 2.15f, BALL);
                    yarnHead(thread, head);
                    thread.curve = side * 18f;
                }
            }
            Bullet knot = launch(w, b, facing, 1.55f, POMPOM);
            yarnHead(knot, head);
            knot.color.set(accent);
        } else {
            // The heads trade quick knots. Each knot has only a few loose threads.
            float speed = beat == 2 ? 3.75f : 2.65f;
            Bullet knot = launch(w, b, aim, speed, POMPOM);
            yarnHead(knot, head);
            float dx = b.targetX(w) - knot.pos.x;
            float dy = b.targetY(w) - knot.pos.y;
            Vector2 velocity = w.getPlayer().getVelocity();
            float intercept = beat == 2 ? Enemy.interceptTime(dx, dy, velocity.x, velocity.y, knot.vel.len()) * .8f : 0f;
            knot.vel.setAngleDeg(MathUtils.atan2Deg360(dy + velocity.y * intercept, dx + velocity.x * intercept));
            knot.color.set(accent);
            for (int bead = -2; bead <= 2; bead++) {
                Bullet thread = launch(w, b, facing + bead * 26f, 1.75f, BALL);
                yarnHead(thread, head);
                thread.curve = head * 16f;
            }
        }
    }

    private void clockwork(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // Raised and lowered ranks march in parallel, with a gap in the centre.
            for (int file = -3; file <= 3; file++) {
                if (file == 0) continue;
                Bullet rank = launch(w, b, formationAim + (beat - 1) * 12f, 2.4f, SQUARE);
                offset(rank, formationAim + (beat - 1) * 12f, file * .46f, -(Math.abs(file) % 2) * .5f);
            }
        } else if (move == 2) {
            // An open clock face keeps the same wide missing sector throughout the turn.
            for (int tick = 0; tick < 12; tick++) {
                if (tick == 1 || tick == 2) continue;
                launch(w, b, formationAim + tick * 30f, 2.05f, STAR);
            }
        } else {
            // Spring shells fan out slowly, then gather speed without changing lanes.
            for (int spring = -3; spring <= 3; spring++) {
                Bullet shell = launch(w, b, aim + spring * 20f, 1.1f + beat * .27f, CARTRIDGE);
                shell.accel = b.isEnraged() ? 1.65f : 1.1f;
                shell.maxSpeed = 4f * w.getDifficulty();
                b.heavyShell(shell);
            }
        }
    }

    private void royalty(GameWorld w, BedBoss b, int move, int beat, float aim) {
        if (move == 1) {
            // An open crown travels on its own, with space between the points and the brim.
            float heading = formationAim + (beat - 1) * 17f;
            for (int side = -1; side <= 1; side += 2) {
                for (int jewel = 1; jewel <= 3; jewel++) {
                    Bullet point = launch(w, b, heading, 2.25f, jewel == 3 ? STAR : DIAMOND);
                    offset(point, heading, side * jewel * .47f, (jewel - 1) * .25f);
                }
                Bullet brim = launch(w, b, heading, 2.25f, BALL);
                offset(brim, heading, side * 1.45f, -.54f);
            }
        } else if (move == 2) {
            // A hollow seal passes by. An entire corner stays open so it cannot box the hero in.
            float heading = formationAim + (beat % 2 == 0 ? -15f : 15f);
            for (int bead = 1; bead < 11; bead++) {
                float a = bead * 30f;
                float across = MathUtils.cosDeg(a);
                float along = MathUtils.sinDeg(a);
                float scale = 1.44f / (Math.abs(across) + Math.abs(along));
                Bullet seal = launch(w, b, heading, 1.95f, DIAMOND);
                offset(seal, heading, across * scale, along * scale);
            }
        } else {
            // The necklace flicks out in alternating arcs. Keep to the open inside edge.
            float heading = formationAim + (beat % 2 == 0 ? -38f : 38f);
            for (int link = -3; link <= 3; link++) {
                if (beat == 2 && link == 0) {
                    float speed = 3.15f;
                    launch(w, b, b.leadAngle(w, speed * w.getDifficulty(), .65f), speed, STAR).color.set(accent);
                } else {
                    Bullet bead = launch(w, b, heading + link * 17f, 2.0f, BALL);
                    bead.curve = beat % 2 == 0 ? 14f : -14f;
                }
            }
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
        b.life = 2.35f;
        boss.track(b);
        return b;
    }
}
