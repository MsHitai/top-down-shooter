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
import java.util.Arrays;
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

/**
 * Headless projectile simulation, including wall rebounds and the real hard-mode clock.
 * These tests encode the Enter the Gungeon contract the floor is tuned to: every attack has a
 * comfortable walking route (a street, not a seam), and every volley can be crossed by one
 * dodge roll even where walking cannot - the Cannonbalrog walls.
 */
class BedBossFairnessTest {
    /** Escapes must be comfortable, not lucky: this much clearance rides along with the hitbox. */
    private static final float ESCAPE_MARGIN = .17f;

    @Test
    void attacksHoldForTheDyingTailInSimulationButNeverPastTheCap() {
        for (Boss kind : Boss.values()) {
            if (kind.stage != Stage.UNDER_BED) continue;
            // Hard + enraged fixture; its bullets never fly or expire, so only the cap frees the
            // next attack - which is exactly what it must do, at about the trash can's pause.
            Fixture f = new Fixture(kind, true, true, new Vector2(6f, 2.3f));
            float cap = BedBoss.TAIL_HOLD_ENRAGED / GameWorld.bossPace(true);
            int thirdVolley = -1, nextVolley = -1, volleys = 0;
            for (int frame = 0; frame < 600 && nextVolley < 0; frame++) {
                int before = f.shots.size;
                f.boss.behave(1f / 60f, f.world);
                if (f.shots.size > before) volleys++;
                if (volleys == 3 && thirdVolley < 0) thirdVolley = frame;
                else if (thirdVolley >= 0 && f.shots.size > before) nextVolley = frame;
            }
            assertTrue(thirdVolley >= 0, kind + " never fired its opening attack");
            assertTrue(nextVolley > thirdVolley, kind + " never followed up");
            float gap = (nextVolley - thirdVolley) / 60f;
            assertTrue(gap > cap, kind + " ignored a still-live front: " + gap);
            assertTrue(gap <= cap + 1.05f, kind + " held the next attack past the cap: " + gap);
            for (Bullet shot : f.shots) shot.alive = false;
            int before = f.shots.size;
            for (int frame = 0; frame < 150; frame++) f.boss.behave(1f / 60f, f.world);
            assertTrue(f.shots.size > before, kind + " did not resume after the bullets cleared");
        }
    }

    @Test
    void nextWindupOverlapsOnlyTheDyingTailOfThePreviousAttack() {
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
        assertTrue(elapsed <= 1.0f, "an extra wind-up was added after the tails expired: " + elapsed);
        for (int i = 0; i < oldCount; i++) {
            Bullet tail = f.shots.get(i);
            assertTrue(!tail.alive || tail.life <= .61f,
                    "the next attack released before the previous front was dying");
        }
    }

    @Test
    void generalsClockOpeningDoesNotChaseThePlayerBetweenRings() {
        Fixture f = new Fixture(Boss.WIND_UP_GENERAL, true, true, new Vector2(6f, 2.3f));
        int volleys = 0, matched = 0;
        float[] first = null;
        for (int frame = 0; frame < 4000 && volleys < 9; frame++) {
            for (Bullet shot : f.shots) shot.alive = false;
            int before = f.shots.size;
            f.boss.behave(1f / 60f, f.world);
            if (f.shots.size == before) continue;
            volleys++;
            if (volleys <= 6) continue; // the opening banks, then the committed clock attack begins
            float[] headings = new float[f.shots.size - before];
            for (int i = 0; i < headings.length; i++)
                headings[i] = f.shots.get(before + i).vel.angleDeg();
            Arrays.sort(headings);
            if (first == null) {
                first = headings;
            } else {
                assertEquals(first.length, headings.length, "the clock's bullet count changed between rings");
                for (int i = 0; i < first.length; i++)
                    assertEquals(first[i], headings[i], .001f, "the clock's escape sector moved");
                matched++;
            }
            f.hero.set(matched % 2 == 0 ? 3f : 9f, 4f);
        }
        assertEquals(2, matched, "the clock attack never fired its follow-up rings");
    }

    @Test
    void specialAttacksLeaveAComfortableWalkingRouteIncludingTheGeneralsRicochets() {
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
                                    if (survives(kind, hard, enraged, alternate, start, direction, walkTime,
                                            Player.HITBOX_RADIUS + ESCAPE_MARGIN)) {
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

    /** The Cannonbalrog contract: wherever walking cannot get through, one dodge roll can.
     * Every volley of the opening attacks must be crossable by rolling - invulnerable for the
     * first 0.32 s of the roll, travelling at the player's real roll speed, chained rolls
     * allowed at the real cooldown. */
    @Test
    void everyVolleyCanBeCrossedByOneDodgeRoll() {
        Arena previous = Arena.now;
        try {
            Arena.now = new Arena(Arena.BOSS_COLS, Arena.BOSS_ROWS);
            for (Boss kind : Boss.values()) {
                if (kind.stage != Stage.UNDER_BED) continue;
                for (boolean hard : new boolean[]{false, true}) {
                    for (Vector2 start : new Vector2[]{new Vector2(6f, 2.3f),
                            new Vector2(1f, 1f), new Vector2(11.8f, 6.5f)}) {
                        Fixture f = new Fixture(kind, hard, false, start);
                        float dt = 1f / 120f, pace = GameWorld.bossPace(hard);
                        for (int volleys = 0; volleys < 9; ) {
                            int before = f.shots.size;
                            f.boss.behave(dt * pace, f.world);
                            if (f.shots.size == before) continue;
                            volleys++;
                            Array<Bullet> volley = new Array<>();
                            for (int i = before; i < f.shots.size; i++) {
                                Bullet shot = f.shots.get(i);
                                Bullet copy = new Bullet(false, shot.pos.x, shot.pos.y,
                                        shot.vel.angleDeg(), shot.vel.len(), shot.radius, 1f, Color.WHITE);
                                copy.accel = shot.accel;
                                copy.maxSpeed = shot.maxSpeed;
                                copy.curve = shot.curve;
                                copy.bounces = shot.bounces;
                                float scale = hard ? 1f : .85f;
                                copy.vel.scl(scale);
                                copy.accel *= scale;
                                copy.maxSpeed *= scale;
                                volley.add(copy);
                            }
                            assertTrue(rollSurvives(volley, start, dt),
                                    kind + " volley " + volleys + " cannot be crossed by one roll");
                        }
                    }
                }
            }
        } finally {
            Arena.now = previous;
        }
    }

    /** Rolls in every direction; one survivor is enough - the player picks her moment. */
    private boolean rollSurvives(Array<Bullet> volley, Vector2 start, float dt) {
        for (int direction = 0; direction < 16; direction++)
            if (rollAttempt(volley, start, direction * 22.5f, dt)) return true;
        return false;
    }

    private boolean rollAttempt(Array<Bullet> volley, Vector2 start, float heading, float dt) {
        final float rollTime = .42f, invulnerableTime = .32f, rollSpeed = 5.2f, cooldown = .18f;
        Vector2 hero = new Vector2(start);
        Vector2 dir = new Vector2(MathUtils.cosDeg(heading), MathUtils.sinDeg(heading));
        boolean rolling = false, cooling = false, invulnerable = false;
        float rollTimer = 0f, coolTimer = 0f;
        for (int frame = 0; frame < 1800; frame++) {
            if (!rolling && !cooling) {
                // Roll at the last readable moment: the nearest closing shot is half a second out.
                float nearest = Float.MAX_VALUE;
                for (Bullet shot : volley) {
                    if (!shot.alive) continue;
                    Vector2 toHero = new Vector2(hero).sub(shot.pos);
                    if (toHero.dot(shot.vel) > 0f)
                        nearest = Math.min(nearest, toHero.len() / Math.max(shot.vel.len(), 1f));
                }
                if (nearest < .5f) {
                    rolling = true;
                    rollTimer = 0f;
                }
            }
            if (rolling) {
                rollTimer += dt;
                hero.add(dir.x * rollSpeed * dt, dir.y * rollSpeed * dt);
                Arena.now.clamp(hero, .2f);
                invulnerable = rollTimer <= invulnerableTime;
                if (rollTimer >= rollTime) {
                    rolling = false;
                    cooling = true;
                    coolTimer = 0f;
                    invulnerable = false;
                }
            } else if (cooling) {
                coolTimer += dt;
                if (coolTimer >= cooldown) cooling = false;
            }
            boolean live = false;
            for (Bullet shot : volley) {
                if (!shot.alive) continue;
                shot.update(dt);
                reboundOrDie(shot);
                if (!shot.alive) continue;
                live = true;
                if (!invulnerable && Intersector.distanceSegmentPoint(shot.prev, shot.pos, hero)
                        < Player.HITBOX_RADIUS + shot.radius * .8f) return false;
            }
            if (!live) return true;
        }
        return false;
    }

    private boolean survives(Boss kind, boolean hard, boolean enraged, boolean alternate,
                             Vector2 start, int direction, float walkTime, float hitbox) {
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
            if (f.hero.dst(f.boss.pos) < f.boss.radius * .8f + hitbox) return false;
            boolean live = false;
            for (Bullet shot : f.shots) {
                if (!shot.alive) continue;
                shot.update(dt);
                reboundOrDie(shot);
                if (!shot.alive) continue;
                live = true;
                if (Intersector.distanceSegmentPoint(shot.prev, shot.pos, f.hero)
                        < hitbox + shot.radius * .8f) return false;
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
