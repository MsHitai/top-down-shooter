package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import com.trush.game.world.Stage;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class BedPatternTest {
    /** Escapes must be comfortable, not lucky: the simulated hero carries this much clearance
     * around her real hitbox, so a passing route is a street a human can actually walk. */
    private static final float ESCAPE_MARGIN = .10f;

    @Test
    void everyBodyPatternLeavesAComfortableWalkingEscapeAfterAReactionDelay() {
        Arena previous = Arena.now;
        try {
            Arena.now = new Arena(Arena.BOSS_COLS, Arena.BOSS_ROWS);
            for (Boss kind : Boss.values()) {
                if (kind.stage != Stage.UNDER_BED) continue;
                for (boolean hard : new boolean[]{false, true}) {
                    for (boolean enraged : new boolean[]{false, true}) {
                        for (int move = 1; move <= 3; move++) {
                            for (Vector2 start : new Vector2[]{new Vector2(6f, 2.3f),
                                    new Vector2(1f, 1f), new Vector2(11.8f, 6.5f)}) {
                                boolean escaped = false;
                                // Holding a direction (or standing) is deliberately simpler than a human
                                // weaving, rolling or blanking. Wait 180 ms before reacting to the first
                                // shot - with the inflated hitbox, only real streets survive this.
                                for (int direction = -1; direction < 32 && !escaped; direction++) {
                                    for (float walkTime : new float[]{.35f, .7f, 1.2f, 2f, 15f}) {
                                        if (survives(kind, move, hard, enraged, start, direction, walkTime,
                                                Player.HITBOX_RADIUS + ESCAPE_MARGIN)) {
                                            escaped = true;
                                            break;
                                        }
                                    }
                                }
                                // A follow-up aimed at your movement may require changing direction.
                                // Keep the same reaction delay, walking speed and clearance.
                                for (int direction = 0; direction < 32 && !escaped; direction += 2) {
                                    for (float turnAt : new float[]{.4f, .65f, .9f, 1.2f}) {
                                        for (int turn : new int[]{-12, -8, -4, 4, 8, 12, 16}) {
                                            if (survives(kind, move, hard, enraged, start, direction, 15f,
                                                    direction + turn, turnAt, Player.HITBOX_RADIUS + ESCAPE_MARGIN)) {
                                                escaped = true;
                                                break;
                                            }
                                        }
                                        if (escaped) break;
                                    }
                                }
                                assertTrue(escaped, kind + " move " + move + " hard=" + hard
                                        + " enraged=" + enraged + " at " + start);
                            }
                        }
                    }
                }
            }
        } finally {
            Arena.now = previous;
        }
    }

    private boolean survives(Boss kind, int move, boolean hard, boolean enraged, Vector2 start,
                             int direction, float walkTime, float hitbox) {
        return survives(kind, move, hard, enraged, start, direction, walkTime, direction, walkTime, hitbox);
    }

    private boolean survives(Boss kind, int move, boolean hard, boolean enraged, Vector2 start,
                             int direction, float walkTime, int turnDirection, float turnAt, float hitbox) {
        Array<Bullet> shots = new Array<>();
        GameWorld world = mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        Player player = mock(Player.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(world.getPlayer()).thenReturn(player);
        Vector2 velocity = new Vector2();
        when(player.getVelocity()).thenReturn(velocity);
        // The deepest Under the Bed boss room: 1.2 normal, +0.2 hard.
        when(world.getDifficulty()).thenReturn(hard ? 1.4f : 1.2f);
        doAnswer(call -> {
            Bullet shot = new Bullet(false, call.getArgument(0), call.getArgument(1), call.getArgument(2),
                    call.getArgument(3), .065f, 1f, Color.WHITE);
            shots.add(shot);
            return shot;
        }).when(world).spawnEnemyBullet(anyFloat(), anyFloat(), anyFloat(), anyFloat());
        BedBoss boss = new BedBoss(6f, 5f, kind);
        if (enraged) boss.hp = boss.maxHp * .4f;
        BedPattern pattern = new BedPattern(Color.WHITE, Color.WHITE);
        float aim = new Vector2(start).sub(boss.pos).angleDeg();
        Vector2 hero = new Vector2(start);
        float dt = 1f / 120f;
        float nextBeatAt = 0f;
        int beat = 0;
        int beatCount = BedBoss.beatsInAttack(kind, move);
        for (int frame = 0; frame < 720; frame++) {
            float time = frame * dt;
            if (beat < beatCount && time >= nextBeatAt) {
                int from = shots.size;
                when(player.centerX()).thenReturn(hero.x);
                when(player.hitboxY()).thenReturn(hero.y);
                float heading = move == 1 ? aim : new Vector2(hero).sub(boss.pos).angleDeg();
                pattern.fire(world, boss, kind, move, beat++, heading);
                nextBeatAt += BedBoss.bodyBeatInterval(kind, move, beat, enraged) / GameWorld.bossPace(hard);
                for (int i = from; i < shots.size; i++) {
                    Bullet bullet = shots.get(i);
                    float scale = hard ? 1f : .85f;
                    bullet.vel.scl(scale);
                    bullet.accel *= scale;
                    bullet.maxSpeed *= scale;
                }
            }
            if (direction >= 0 && time >= .18f && time < .18f + walkTime) {
                int facing = time < .18f + turnAt ? direction : turnDirection;
                velocity.set(MathUtils.cosDeg(facing * 11.25f) * 2.4f,
                        MathUtils.sinDeg(facing * 11.25f) * 2.4f);
                hero.mulAdd(velocity, dt);
                Arena.now.clamp(hero, .2f);
            } else {
                velocity.setZero();
            }
            if (hero.dst(boss.pos) < boss.radius + Player.HITBOX_RADIUS) return false;
            for (Bullet shot : shots) {
                if (!shot.alive) continue;
                shot.update(dt);
                if (!Arena.now.contains(shot.pos.x, shot.pos.y, 0f)) shot.alive = false;
                if (shot.alive && Intersector.distanceSegmentPoint(shot.prev, shot.pos, hero)
                        < hitbox + shot.radius * .8f) return false;
            }
        }
        return true;
    }
}
