package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

import static com.trush.game.objects.Bullet.Shape.*;

/** Moving volleys fired from each resident. A volley keeps its heading once released. */
final class BedPattern {
    private final Bullet.Shape shot;
    private final Bullet.Shape special;
    private final Color color;

    BedPattern(Boss kind, Color color) {
        this.color = color;
        shot = switch (kind) {
            case DUST_BUNNY -> CLOUD;
            case BED_MONSTER -> SQUARE;
            case SOCK_NINJA -> NAIL;
            case YARN_HYDRA -> BALL;
            case WIND_UP_GENERAL -> STAR;
            case LINT_REGENT -> DIAMOND;
            default -> throw new IllegalArgumentException(kind.name());
        };
        special = switch (kind) {
            case DUST_BUNNY -> POMPOM;
            case BED_MONSTER -> PILLOW;
            case SOCK_NINJA -> SOCK;
            case YARN_HYDRA -> POMPOM;
            case WIND_UP_GENERAL -> SQUARE;
            default -> STAR;
        };
    }

    void fire(GameWorld world, BedBoss boss, Boss kind, int move, int beat, float sweep) {
        float aim = boss.leadAngle(world, 6f * world.getDifficulty(), .65f);
        if (move == 1) {
            switch (kind) {
                case BED_MONSTER -> {
                    // A rippling sheet: the centre travels slowly while the corners curl in.
                    for (int i = -4; i <= 4; i++) {
                        Bullet fold = launch(world, boss, aim + i * 14f, 2.8f + .42f * Math.abs(i), shot);
                        fold.curve = -i * 7f;
                    }
                }
                case SOCK_NINJA -> {
                    for (int i = 0; i < 4; i++)
                        launch(world, boss, sweep + i * 90f + beat * 24f, 6.8f, shot);
                }
                case LINT_REGENT -> {
                    // Tall centre and outer points give each travelling crown its silhouette.
                    for (int i = -3; i <= 3; i++) {
                        float speed = i == 0 || Math.abs(i) == 3 ? 5.6f : 3.9f;
                        launch(world, boss, aim + i * 11f, speed, special);
                    }
                }
                default -> {
                    int count = kind == Boss.YARN_HYDRA ? 12 : kind == Boss.DUST_BUNNY ? 8 : 10;
                    for (int i = 1; i < count; i++) {
                        Bullet b = launch(world, boss, sweep + (i + beat * .45f) * 360f / count, 3.5f, shot);
                        if (kind == Boss.YARN_HYDRA) b.curve = 24f;
                    }
                }
            }
        } else if (move == 2) {
            // Sweep a fixed arc. The quicker last volley overtakes the first if the player backs straight up.
            for (int i = -2; i <= 2; i++) {
                Bullet b = launch(world, boss, sweep + (beat - 1) * 24f + i * 12f, 3.6f + beat * 1.1f, shot);
                if (kind == Boss.WIND_UP_GENERAL) b.bounces = 1;
                if (kind == Boss.YARN_HYDRA || kind == Boss.DUST_BUNNY) b.curve = i * 11f;
            }
        } else {
            switch (kind) {
                case SOCK_NINJA -> {
                    launch(world, boss, aim - 12f, 6.7f, shot);
                    launch(world, boss, aim + 12f, 6.7f, shot);
                    launch(world, boss, aim, 3.4f, special).curve = beat % 2 == 0 ? 36f : -36f;
                }
                case WIND_UP_GENERAL -> {
                    for (int i = -1; i <= 1; i++)
                        launch(world, boss, aim + i * 21f, 3.2f, special).accel = 3.4f;
                }
                case YARN_HYDRA, LINT_REGENT -> {
                    // Two strands cross in front of the player, then peel apart.
                    for (int side = -1; side <= 1; side += 2) {
                        launch(world, boss, aim + side * 30f, 4.8f, special).curve = -side * 62f;
                        launch(world, boss, aim + side * 52f, 4f, shot).curve = -side * 48f;
                    }
                }
                default -> {
                    Bullet clump = launch(world, boss, aim, 3.6f, special);
                    clump.radius *= 1.65f;
                    clump.accel = 2.7f;
                    for (int side = -1; side <= 1; side += 2)
                        launch(world, boss, aim + side * 25f, 5.8f, shot);
                }
            }
        }
    }

    private Bullet launch(GameWorld world, BedBoss boss, float heading, float speed, Bullet.Shape shape) {
        Bullet b = world.spawnEnemyBullet(boss.pos.x, boss.pos.y, heading, speed * world.getDifficulty());
        b.color.set(color);
        b.shape = shape;
        b.life = 3.4f;
        return b;
    }
}
