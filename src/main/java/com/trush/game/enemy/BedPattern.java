package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

import static com.trush.game.objects.Bullet.Shape.*;

/** One beat of a bedroom boss attack. The bullets follow the same stitches drawn on the floor. */
final class BedPattern {
    private record Stitch(Bullet.Shape shape, float speed, Vector2[] points) {}
    private final Array<Stitch> stitches = new Array<>();
    private final Vector2 center = new Vector2();
    private final Vector2 origin = new Vector2();

    void plan(Boss boss, int move, int beat, Vector2 target, Vector2 source) {
        stitches.clear();
        center.set(target);
        origin.set(source);
        switch (boss) {
            case DUST_BUNNY -> dust(move, beat);
            case BED_MONSTER -> sheet(move, beat);
            case SOCK_NINJA -> sock(move, beat);
            case YARN_HYDRA -> yarn(move, beat);
            case WIND_UP_GENERAL -> tin(move, beat);
            case LINT_REGENT -> lint(move, beat);
            default -> throw new IllegalArgumentException(boss.name());
        }
    }

    private Vector2 at(float x, float y) {
        Vector2 point = new Vector2(center.x + x, center.y + y);
        Arena.now.clamp(point, 0.15f);
        return point;
    }

    private Vector2 around(float degrees, float radius) {
        return at(MathUtils.cosDeg(degrees) * radius, MathUtils.sinDeg(degrees) * radius);
    }

    private void stitch(Bullet.Shape shape, float speed, Vector2... points) {
        stitches.add(new Stitch(shape, speed, points));
    }

    private void dust(int move, int beat) {
        float row = (beat - 1) * .78f;
        switch (move) {
            case 1 -> { // Dust devils curl in from opposite sides, leaving the centre clear until the last beat.
                for (int sign : new int[]{-1, 1}) {
                    float r = 2.1f - beat * .65f;
                    stitch(CLOUD, 5.8f, at(sign * 2.8f, -r), at(sign * r, -r),
                            at(sign * r, r), at(0, r));
                }
            }
            case 2 -> { // A sneeze rolls three clumps down one lane at different speeds; step off its track.
                for (int i = -1; i <= 1; i++) {
                    stitch(DUST, 6.8f - Math.abs(i) * 1.6f, origin.cpy(), at(row + i * .48f, .3f),
                            at(row + i * .9f, -2f));
                }
            }
            case 3 -> { // Bounding tumbleweeds skip down alternating diagonals.
                int hand = beat % 2 == 0 ? 1 : -1;
                for (int i = 0; i < 3; i++) {
                    float y = row + i * .55f - .55f;
                    stitch(POMPOM, 5.6f, at(-3 * hand, y), at(-1.5f * hand, y + .8f),
                            at(0, y), at(1.5f * hand, y + .8f), at(3 * hand, y));
                }
            }
        }
    }

    private void sheet(int move, int beat) {
        float step = beat - 1;
        switch (move) {
            case 1 -> { // Two pleats zip together from the far end of the sheet.
                for (int side : new int[]{-1, 1})
                    stitch(PILLOW, 5.8f, at(side * (2.4f - beat * .45f), 2.2f), at(side * .28f, step),
                            at(side * .28f, -2.2f));
            }
            case 2 -> { // A lifted corner folds over on a diagonal. Slip behind its trailing edge.
                for (int i = 0; i < 5; i++) {
                    float x = -2.3f + i * .56f + step;
                    stitch(SQUARE, 4.8f, at(x, 2), at(x + 1.6f, .4f), at(x, -1.2f));
                }
            }
            case 3 -> { // Mattress springs compress upward, then kick back past their starting row.
                for (int i = -1; i <= 1; i++) {
                    float x = i * 1.15f + step * .35f;
                    stitch(PILLOW, 5.5f, at(x, -1), at(x, .9f + i * .3f), at(x, -2.4f));
                }
            }
        }
    }

    private void sock(int move, int beat) {
        int hand = beat % 2 == 0 ? 1 : -1;
        switch (move) {
            case 1 -> { // A dropped sock draws the eye. Two needles hook out from its heel.
                Vector2 heel = at(hand * 2.1f, 1.3f - beat);
                for (int i : new int[]{-1, 1})
                    stitch(NAIL, 7.2f, heel, at(hand * .65f, i * .72f), at(-hand * 2.5f, i * .18f));
            }
            case 2 -> { // A pin laces through a row of eyelets and its twin follows the opposite lace.
                for (int i : new int[]{-1, 1})
                    stitch(DIAMOND, 6.6f, at(-2.7f, i), at(-1.4f, -i * .6f), at(0, i * .6f),
                            at(1.4f, -i * .6f), at(2.7f, i));
            }
            case 3 -> { // Three relay throws hand the sock from corner to corner, cutting the middle last.
                Vector2 a = around(90 + beat * 120, 2.6f);
                Vector2 b = around(210 + beat * 120, 2.6f);
                stitch(SOCK, 7.4f, a, b, at(0, 0), around(30 + beat * 120, 2.2f));
            }
        }
    }

    private void yarn(int move, int beat) {
        switch (move) {
            case 1 -> { // Three heads reel in separate coils. The safe pocket shrinks between turns.
                for (int head = 0; head < 3; head++) {
                    Vector2[] coil = new Vector2[8];
                    for (int i = 0; i < coil.length; i++)
                        coil[i] = around(head * 120 + i * 24 + beat * 21, 2.7f - i * .31f);
                    stitch(BALL, 4.8f, coil);
                }
            }
            case 2 -> { // A shuttle weaves between two stationary warp threads, then reverses.
                int hand = beat % 2 == 0 ? 1 : -1;
                for (int i = -1; i <= 1; i++) {
                    float y = i * .65f;
                    stitch(BALL, 5.7f, at(-3 * hand, y - .4f), at(-hand, y + .65f),
                            at(hand, y - .65f), at(3 * hand, y + .4f));
                }
            }
            case 3 -> { // Pull one strand of a bow: its loop runs out, crossing its own neck.
                int hand = beat % 2 == 0 ? 1 : -1;
                float y = (beat - 1) * .48f;
                stitch(POMPOM, 6.2f, at(-2.6f * hand, y - .6f), at(0, y), at(1.5f * hand, y + 1.2f),
                        at(2.2f * hand, y), at(1.5f * hand, y - 1.2f), at(0, y), at(-2.6f * hand, y + .6f));
            }
        }
    }

    private void tin(int move, int beat) {
        float step = beat - 1;
        switch (move) {
            case 1 -> { // Clockwork infantry wheel ninety degrees as a formation.
                for (int i = -1; i <= 1; i++)
                    stitch(SQUARE, 5.6f, at(-3, step + i * .42f), at(step + i * .42f, step + i * .42f),
                            at(step + i * .42f, -2.4f));
            }
            case 2 -> { // Gear teeth travel round the escapement; a missing tooth rotates with the gear.
                for (int i = 0; i < 7; i++) {
                    float a = i * 45 + beat * 45;
                    float r = 1.9f - .52f * beat;
                    stitch(STAR, 4.2f, around(a, r), around(a + 45, r), around(a + 90, r));
                }
            }
            case 3 -> { // A ratchet advances twice then slips one notch.
                for (int i = -1; i <= 1; i++) {
                    float y = step + i * .45f;
                    stitch(NAIL, 7f, at(-3, y), at(-.4f, y), at(-1.25f, y), at(2.6f, y));
                }
            }
        }
    }

    private void lint(int move, int beat) {
        float step = beat - 1;
        switch (move) {
            case 1 -> { // Stamp a succession of wax seals around the marked spot; escape through the open edge.
                float r = 1.9f - beat * .55f;
                for (int i = 0; i < 5; i++) {
                    float a = 60 * (beat + i);
                    stitch(BALL, 4.6f, around(a, r), around(a + 60, r));
                }
            }
            case 2 -> { // Two ushers unroll a carpet away from the throne, then fold its edges inward.
                for (int side : new int[]{-1, 1}) {
                    float x = side * (1.8f - beat * .45f);
                    stitch(DIAMOND, 6f, at(x, 2.2f), at(x, -1.6f), at(0, -1.6f), at(0, step));
                }
            }
            case 3 -> { // Trace the three points of a crown in succession.
                for (int i = 0; i < 3; i++) {
                    float x = step * 1.25f;
                    float y = i * .32f;
                    stitch(STAR, 5.6f, at(x - .8f, y - 1.2f), at(x, y + (beat == 1 ? 1.6f : .85f)),
                            at(x + .8f, y - 1.2f));
                }
            }
        }
    }

    void fire(GameWorld world, Color color) {
        for (Stitch stitch : stitches) {
            Vector2 start = stitch.points[0];
            float heading = stitch.points[1].cpy().sub(start).angleDeg();
            Bullet shot = world.spawnEnemyBullet(start.x, start.y, heading, stitch.speed * world.getDifficulty());
            shot.shape = stitch.shape;
            shot.color.set(color);
            shot.followCourse(stitch.points);
        }
    }

    void draw(SpriteBatch batch, Draw draw, Color color, float alpha) {
        for (Stitch stitch : stitches) {
            Vector2 start = stitch.points[0];
            draw.ring(batch, start.x, start.y, .13f, color, alpha);
            for (int i = 1; i < stitch.points.length; i++) {
                Vector2 a = stitch.points[i - 1], b = stitch.points[i];
                draw.line(batch, a.x, a.y, a.dst(b), .023f, MathUtils.atan2Deg360(b.y - a.y, b.x - a.x),
                        color, alpha * .65f);
            }
        }
    }
}
