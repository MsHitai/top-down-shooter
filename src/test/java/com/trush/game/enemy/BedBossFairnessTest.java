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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/** Headless projectile simulation, including wall rebounds and the real hard-mode clock. */
class BedBossFairnessTest {
    @Test
    void unrelatedAttacksWaitForTheirLastProjectileEvenWithoutRendering() {
        for (Boss kind : Boss.values()) {
            if (kind.stage != Stage.UNDER_BED) continue;
            Fixture f = new Fixture(kind, true, true, new Vector2(6f, 2.3f));
            int volleys = 0;
            for (int frame = 0; frame < 600; frame++) {
                int before = f.shots.size;
                f.boss.behave(1f / 60f, f.world);
                if (f.shots.size > before) volleys++;
            }
            assertEquals(3, volleys, kind + " overlapped a still-live attack");
            for (Bullet shot : f.shots) shot.alive = false;
            int before = f.shots.size;
            for (int frame = 0; frame < 150; frame++) f.boss.behave(1f / 60f, f.world);
            assertTrue(f.shots.size > before, kind + " did not resume after the bullets cleared");
        }
    }

    @Test
    void nextWindupUsesTheDepartingTailsWithoutOverlappingAttacks() {
        Fixture f = new Fixture(Boss.BED_MONSTER, false, false, new Vector2(6f, 2.3f));
        int volleys = 0;
        for (int frame = 0; frame < 600 && volleys < 3; frame++) {
            int before = f.shots.size;
            f.boss.behave(1f / 120f, f.world);
            if (f.shots.size > before) volleys++;
        }
        assertEquals(3, volleys);
        int oldCount = f.shots.size;
        for (Bullet shot : f.shots) shot.life = 1f;
        float elapsed = 0f;
        while (f.shots.size == oldCount && elapsed < 2f) {
            for (Bullet shot : f.shots) if (shot.alive) shot.update(1f / 120f);
            f.boss.behave(1f / 120f, f.world);
            elapsed += 1f / 120f;
        }
        assertTrue(f.shots.size > oldCount);
        assertTrue(elapsed <= 1.15f, "an extra wind-up was added after the tails expired: " + elapsed);
        for (int i = 0; i < oldCount; i++) assertTrue(!f.shots.get(i).alive);
    }

    @Test
    void generalsClockOpeningDoesNotChaseThePlayerBetweenRings() {
        Fixture f = new Fixture(Boss.WIND_UP_GENERAL, true, true, new Vector2(6f, 2.3f));
        int volleys = 0;
        for (int frame = 0; frame < 4000 && volleys < 6; frame++) {
            for (Bullet shot : f.shots) shot.alive = false;
            int before = f.shots.size;
            f.boss.behave(1f / 60f, f.world);
            if (f.shots.size > before) volleys++;
        }
        assertEquals(6, volleys);
        for (Bullet shot : f.shots) shot.alive = false;
        f.shots.clear();
        float[] headings = new float[86];
        int rings = 0;
        for (int frame = 0; frame < 600 && rings < 3; frame++) {
            int before = f.shots.size;
            f.boss.behave(1f / 60f, f.world);
            if (f.shots.size == before) continue;
            assertEquals(headings.length, f.shots.size - before);
            for (int i = 0; i < headings.length; i++) {
                float heading = f.shots.get(before + i).vel.angleDeg();
                if (rings == 0) headings[i] = heading;
                else assertEquals(headings[i], heading, .001f, "the clock's escape sector moved");
            }
            f.hero.set(rings % 2 == 0 ? 3f : 9f, 4f);
            rings++;
        }
        assertEquals(3, rings);
    }

    @Test
    void specialAttacksLeaveAWalkingRouteIncludingTheGeneralsRicochets() {
        Arena previous = Arena.now;
        try {
            Arena.now = new Arena(Arena.BOSS_COLS, Arena.BOSS_ROWS);
            for (Boss kind : Boss.values()) {
                if (kind.stage != Stage.UNDER_BED) continue;
                for (int mode = 0; mode < 4; mode++) {
                    boolean hard = (mode & 1) != 0;
                    boolean enraged = (mode & 2) != 0;
                    for (boolean alternate : new boolean[]{false, true}) {
                        for (Vector2 start : new Vector2[]{new Vector2(6f, 2.3f),
                                new Vector2(1f, 1f), new Vector2(11.8f, 6.5f)}) {
                            boolean escaped = false;
                            for (int direction = -1; direction < 32 && !escaped; direction++) {
                                for (float walkTime : new float[]{.35f, .7f, 1.2f, 2f, 15f}) {
                                    if (survives(kind, hard, enraged, alternate, start, direction, walkTime)) {
                                        escaped = true;
                                        break;
                                    }
                                }
                            }
                            assertTrue(escaped, kind + " hard=" + hard + " enraged=" + enraged
                                    + " alternate=" + alternate + " at " + start);
                        }
                    }
                }
            }
        } finally {
            Arena.now = previous;
        }
    }

    private boolean survives(Boss kind, boolean hard, boolean enraged, boolean alternate,
                             Vector2 start, int direction, float walkTime) {
        Fixture f = new Fixture(kind, hard, enraged, start);
        float dt = 1f / 120f, pace = GameWorld.bossPace(hard);
        if (alternate) {
            int volleys = 0;
            for (int frame = 0; frame < 4000 && volleys < 12; frame++) {
                for (Bullet shot : f.shots) shot.alive = false;
                int before = f.shots.size;
                f.boss.behave(dt * pace, f.world);
                if (f.shots.size > before) volleys++;
            }
            for (Bullet shot : f.shots) shot.alive = false;
            f.shots.clear();
        }
        int beats = 0;
        float firstShot = -1f;
        for (int frame = 0; frame < 1800; frame++) {
            float time = frame * dt;
            int before = f.shots.size;
            // Stop at the end of this attack, but keep simulating all of its tails.
            if (beats < 3) f.boss.behave(dt * pace, f.world);
            if (f.shots.size > before) {
                beats++;
                if (firstShot < 0f) firstShot = time;
                for (int i = before; i < f.shots.size; i++) {
                    Bullet shot = f.shots.get(i);
                    float speed = (hard ? 1f : .85f);
                    shot.vel.scl(speed);
                    shot.accel *= speed;
                    shot.maxSpeed *= speed;
                }
            }
            if (direction >= 0 && firstShot >= 0f && time >= firstShot + .18f && time < firstShot + .18f + walkTime) {
                f.hero.add(MathUtils.cosDeg(direction * 11.25f) * 2.4f * dt,
                        MathUtils.sinDeg(direction * 11.25f) * 2.4f * dt);
                Arena.now.clamp(f.hero, .2f);
            }
            if (f.hero.dst(f.boss.pos) < f.boss.radius * .8f + Player.HITBOX_RADIUS) return false;
            boolean live = false;
            for (Bullet shot : f.shots) {
                if (!shot.alive) continue;
                shot.update(dt);
                reboundOrDie(shot);
                if (!shot.alive) continue;
                live = true;
                if (Intersector.distanceSegmentPoint(shot.prev, shot.pos, f.hero)
                        < Player.HITBOX_RADIUS + shot.radius * .8f) return false;
            }
            if (beats == 3 && !live) return true;
        }
        return false;
    }

    private void reboundOrDie(Bullet shot) {
        Arena a = Arena.now;
        float edge = shot.radius * 1.3f * .7f; // styled size used by scenery collision
        boolean x = shot.pos.x < a.left + edge || shot.pos.x > a.right - edge;
        boolean y = shot.pos.y < a.bottom + edge || shot.pos.y > a.top - edge;
        if (!x && !y) return;
        if (shot.bounces == 0) {
            shot.alive = false;
            return;
        }
        if (x) shot.vel.x = -shot.vel.x;
        if (y) shot.vel.y = -shot.vel.y;
        shot.pos.set(shot.prev);
        shot.onBounce();
    }

    private static class Fixture {
        final Array<Bullet> shots = new Array<>();
        final Vector2 hero;
        final GameWorld world = mock(GameWorld.class,
                withSettings().mockMaker(MockMakers.SUBCLASS).stubOnly().defaultAnswer(RETURNS_DEEP_STUBS));
        final Player player = mock(Player.class, withSettings().mockMaker(MockMakers.SUBCLASS).stubOnly());
        final BedBoss boss;

        Fixture(Boss kind, boolean hard, boolean enraged, Vector2 start) {
            hero = new Vector2(start);
            when(world.isHard()).thenReturn(hard);
            when(world.getDifficulty()).thenReturn(hard ? 1.55f : 1.35f);
            when(world.getPlayer()).thenReturn(player);
            when(player.centerX()).thenAnswer(call -> hero.x);
            when(player.hitboxY()).thenAnswer(call -> hero.y);
            doAnswer(call -> {
                Bullet shot = new Bullet(false, call.getArgument(0), call.getArgument(1),
                        call.getArgument(2), call.getArgument(3), .065f, 1f, Color.WHITE);
                shot.life = 10f;
                shots.add(shot);
                return shot;
            }).when(world).spawnEnemyBullet(anyFloat(), anyFloat(), anyFloat(), anyFloat());
            boss = new BedBoss(6f, 5f, kind);
            if (enraged) boss.hp = boss.maxHp * .4f;
        }
    }
}
