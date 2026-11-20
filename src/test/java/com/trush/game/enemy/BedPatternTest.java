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
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class BedPatternTest {
    @Test
    void everyBodyPatternLeavesAWalkingEscapeAfterAReactionDelay() {
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
                                // weaving, rolling or blanking. Wait 180 ms before reacting to the first shot.
                                for (int direction = -1; direction < 32 && !escaped; direction++) {
                                    for (float walkTime : new float[]{.35f, .7f, 1.2f, 2f, 15f}) {
                                        if (survives(kind, move, hard, enraged, start, direction, walkTime)) {
                                            escaped = true;
                                            break;
                                        }
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

    @Test
    void everyBodyAttackPunishesStandingStillAroundTheArena() {
        Arena previous = Arena.now;
        try {
            Arena.now = new Arena(Arena.BOSS_COLS, Arena.BOSS_ROWS);
            for (Boss kind : Boss.values()) {
                if (kind.stage != Stage.UNDER_BED) continue;
                for (int move = 1; move <= 3; move++) {
                    for (int direction = 0; direction < 360; direction += 45) {
                        Vector2 start = new Vector2(6f + MathUtils.cosDeg(direction) * 3f,
                                5f + MathUtils.sinDeg(direction) * 3f);
                        if (!Arena.now.contains(start.x, start.y, .2f)) continue;
                        assertFalse(survives(kind, move, false, false, start, -1, 0f),
                                kind + " move " + move + " leaves camping at " + direction);
                    }
                }
            }
        } finally {
            Arena.now = previous;
        }
    }

    @Test
    void bodyVolleysCoverTheRoomInsteadOfOnlyThePlayersHalf() {
        for (Boss kind : Boss.values()) {
            if (kind.stage != Stage.UNDER_BED) continue;
            for (int move = 1; move <= 3; move++) {
                TreeSet<Float> headings = new TreeSet<>();
                GameWorld world = mock(GameWorld.class,
                        withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
                when(world.getDifficulty()).thenReturn(1f);
                doAnswer(call -> {
                    Bullet shot = new Bullet(false, call.getArgument(0), call.getArgument(1),
                            call.getArgument(2), call.getArgument(3), .065f, 1f, Color.WHITE);
                    headings.add(shot.vel.angleDeg());
                    return shot;
                }).when(world).spawnEnemyBullet(anyFloat(), anyFloat(), anyFloat(), anyFloat());
                new BedPattern(Color.WHITE, Color.WHITE).fire(world, new BedBoss(6f, 5f, kind), kind, move, 0, 0f);
                float previous = headings.last() - 360f;
                for (float heading : headings) {
                    assertTrue(heading - previous <= 60.01f,
                            kind + " move " + move + " leaves a sector of " + (heading - previous));
                    previous = heading;
                }
            }
        }
    }

    private boolean survives(Boss kind, int move, boolean hard, boolean enraged, Vector2 start, int direction, float walkTime) {
        Array<Bullet> shots = new Array<>();
        GameWorld world = mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        Player player = mock(Player.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(world.getPlayer()).thenReturn(player);
        // A deep boss room can reach the floor's 1.35 difficulty cap before hard mode's bonus.
        when(world.getDifficulty()).thenReturn(hard ? 1.55f : 1.35f);
        doAnswer(call -> {
            Bullet shot = new Bullet(false, call.getArgument(0), call.getArgument(1), call.getArgument(2),
                    call.getArgument(3), .065f, 1f, Color.WHITE);
            shots.add(shot);
            return shot;
        }).when(world).spawnEnemyBullet(anyFloat(), anyFloat(), anyFloat(), anyFloat());
        BedBoss boss = new BedBoss(6f, 5f, kind);
        BedPattern pattern = new BedPattern(Color.WHITE, Color.WHITE);
        float aim = new Vector2(start).sub(boss.pos).angleDeg();
        Vector2 hero = new Vector2(start);
        float dt = 1f / 120f, interval = BedBoss.bodyBeatInterval(enraged) / GameWorld.bossPace(hard);
        int beat = 0;
        for (int frame = 0; frame < 720; frame++) {
            float time = frame * dt;
            if (beat < 3 && time >= beat * interval) {
                int from = shots.size;
                when(player.centerX()).thenReturn(hero.x);
                when(player.hitboxY()).thenReturn(hero.y);
                float heading = move == 1 ? aim : new Vector2(hero).sub(boss.pos).angleDeg();
                pattern.fire(world, boss, kind, move, beat++, heading);
                for (int i = from; i < shots.size; i++) {
                    Bullet bullet = shots.get(i);
                    float scale = hard ? 1f : .85f;
                    bullet.vel.scl(scale);
                    bullet.accel *= scale;
                    bullet.maxSpeed *= scale;
                }
            }
            if (direction >= 0 && time >= .18f && time < .18f + walkTime) {
                hero.add(MathUtils.cosDeg(direction * 11.25f) * 2.4f * dt,
                        MathUtils.sinDeg(direction * 11.25f) * 2.4f * dt);
                Arena.now.clamp(hero, .2f);
            }
            if (hero.dst(boss.pos) < boss.radius + Player.HITBOX_RADIUS) return false;
            for (Bullet shot : shots) {
                if (!shot.alive) continue;
                shot.update(dt);
                if (!Arena.now.contains(shot.pos.x, shot.pos.y, 0f)) shot.alive = false;
                if (shot.alive && Intersector.distanceSegmentPoint(shot.prev, shot.pos, hero)
                        < Player.HITBOX_RADIUS + shot.radius * .8f) return false;
            }
        }
        return true;
    }
}
