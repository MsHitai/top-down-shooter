package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.Dream;
import com.trush.game.world.GameWorld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import java.util.EnumSet;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class BedEnemiesTest {
    private static final float STEP = 1f / 60f;
    private final Array<Bullet> shots = new Array<>();
    private final Vector2 target = new Vector2(7f, 4f);
    private Arena previous;
    private GameWorld world;

    @BeforeEach void setup() {
        previous = Arena.now;
        Arena.now = new Arena(Arena.BOSS_COLS, Arena.BOSS_ROWS);
        world = mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
        when(world.getDifficulty()).thenReturn(1f);
        when(world.onScreen(anyFloat(), anyFloat(), anyFloat())).thenReturn(true);
        when(world.walkable(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat())).thenReturn(true);
        when(world.hasLineOfSight(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat())).thenReturn(true);
        when(world.getPlayer().centerX()).thenAnswer(call -> target.x);
        when(world.getPlayer().hitboxY()).thenAnswer(call -> target.y);
        when(world.getPlayer().getVelocity()).thenReturn(new Vector2());
        doAnswer(call -> {
            Bullet shot = new Bullet(false, call.getArgument(0), call.getArgument(1), call.getArgument(2),
                    call.getArgument(3), .065f, 1f, Color.WHITE);
            shots.add(shot);
            return shot;
        }).when(world).spawnEnemyBullet(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }

    @AfterEach void restore() { Arena.now = previous; }

    private void tick(Enemy enemy) {
        enemy.aimAngle = MathUtils.atan2Deg360(target.y - enemy.pos.y, target.x - enemy.pos.x);
        enemy.behave(STEP, world);
    }

    private void firstVolley(Enemy enemy) {
        for (int frame = 0; frame < 480; frame++) {
            tick(enemy);
            if (shots.notEmpty()) return;
        }
        fail(enemy.dexId() + " never fired");
    }

    @Test void mixedUpBorrowsAForeignVolleyAfterTheMitesWindUp() {
        when(world.getEnemyBullets()).thenReturn(shots);
        when(world.rule(Dream.Rule.MIXED_UP)).thenReturn(true);
        BedCritter mite = new BedCritter(3f, 4f, BedCritter.Kind.MITE);
        for (int frame = 0; frame < 480 && shots.isEmpty(); frame++) mite.update(STEP, world);
        assertTrue(shots.size > 5);
        for (int i = 0; i < 5; i++) {
            assertEquals(Bullet.Shape.CLOUD, shots.get(i).shape);
            assertFalse(shots.get(i).alive);
        }
        for (int i = 5; i < shots.size; i++) {
            assertTrue(shots.get(i).alive);
            assertNotEquals(Bullet.Shape.CLOUD, shots.get(i).shape);
        }
    }

    @Test void mixedUpOffersFansSquaresAndRoboCartCrossesToBulletKin() {
        when(world.getEnemyBullets()).thenReturn(shots);
        BulletKin kin = new BulletKin(3f, 4f);
        kin.aimAngle = 23f;
        EnumSet<Bullet.Shape> shapes = EnumSet.noneOf(Bullet.Shape.class);
        boolean sawCross = false;
        Random previousRandom = MathUtils.random;
        MathUtils.random = new RandomXS128(1L);
        try {
            for (int i = 0; i < 30; i++) {
                shots.clear();
                assertTrue(MixedUp.replaceVolley(kin, world, 0));
                if (shots.size == 4) {
                    sawCross = true;
                    float start = shots.first().vel.y > 1f ? 45f : 0f;
                    for (int j = 0; j < shots.size; j++) {
                        Bullet bolt = shots.get(j);
                        assertEquals(start + j * 90f, bolt.vel.angleDeg(), .1f);
                        assertEquals(2.4f, bolt.vel.len(), .01f);
                        assertEquals(.18f, bolt.streak);
                        assertEquals(Bullet.Shape.BALL, bolt.shape);
                    }
                } else {
                    for (Bullet shot : shots) shapes.add(shot.shape);
                }
            }
        } finally {
            MathUtils.random = previousRandom;
        }
        assertEquals(EnumSet.of(Bullet.Shape.BALL, Bullet.Shape.SQUARE), shapes);
        assertTrue(sawCross);
    }

    @Test void dieAimsAFullSideAtTheHeroAndLeavesItsDoorwayToTheSide() {
        BedHeavy die = new BedHeavy(3f, 4f, BedHeavy.Kind.TUMBLE_DIE);
        firstVolley(die);
        assertEquals(26, shots.size);
        for (int i = 0; i < 13; i++) {
            Bullet pip = shots.get(i);
            pip.update(.6f);
            float dx = pip.pos.x - die.pos.x, dy = pip.pos.y - die.pos.y;
            assertEquals(2.05f, Math.max(Math.abs(dx), Math.abs(dy)), .005f,
                    "corner and edge bullets must remain on the same square perimeter");
            assertFalse(dy > 0 && Math.abs(dx) < 1f, "the indicated upper doorway stays empty");
        }
        for (int i = 13; i < shots.size; i++) assertEquals(.22f, shots.get(i).delay, .001f);
    }

    @Test void mothFiresFourFixedRibbonsWithGapsAndThreadsThatBendBothWays() {
        BedHeavy moth = new BedHeavy(3f, 4f, BedHeavy.Kind.QUILT_MOTH);
        firstVolley(moth);
        assertEquals(4, shots.size, "four ribbons leave together");
        Vector2 forward = target.cpy().sub(moth.pos).nor();
        Vector2 lateral = new Vector2(-forward.y, forward.x);
        Bullet left = shots.first();
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (int frame = 0; frame < 95; frame++) {
            for (Bullet thread : shots) thread.update(STEP);
            for (int lane = 1; lane < 4; lane++) {
                float width = new Vector2(shots.get(lane).pos).sub(shots.get(lane - 1).pos).dot(lateral);
                if (frame > 15) assertTrue(width >= .5f,
                        "adjacent ribbons leave a dodgeable lane beyond the wings: " + width + " at frame " + frame);
            }
            float y = new Vector2(left.pos).sub(moth.pos).dot(lateral);
            if (frame > 30) { min = Math.min(min, y); max = Math.max(max, y); }
        }
        assertTrue(max - min > .4f, "a wave bends back, rather than just travelling on a curved arc");
        target.set(3f, 7f);
        for (int frame = 0; frame < 65; frame++) tick(moth);
        assertEquals(24, shots.size);
        for (Bullet thread : shots) {
            assertTrue(thread.vel.dot(forward) > 0, "later threads retain the first four threads' direction");
        }
    }

    @Test void crocJawsMeetAtTheLaunchAimAndTheFanBlocksAStraightRetreat() {
        BedHeavy croc = new BedHeavy(3f, 4f, BedHeavy.Kind.PEG_CROC);
        firstVolley(croc);
        assertEquals(14, shots.size);
        Vector2 marked = target.cpy();
        target.set(3f, 7f);
        for (int i = 0; i < 10; i++) {
            Bullet tooth = shots.get(i);
            float closest = Float.MAX_VALUE;
            for (int frame = 0; frame < 240 && tooth.alive; frame++) {
                tooth.update(STEP);
                closest = Math.min(closest, tooth.pos.dst(marked));
            }
            assertTrue(closest < .08f, "teeth cross the launch aim and continue beyond it");
            assertTrue(tooth.pos.dst(marked) > 1f);
        }
    }

    @Test void marbleBanksOnceAndDimsTheReflectedShotWithoutADeathBurst() {
        BedCritter marble = new BedCritter(3f, 4f, BedCritter.Kind.MARBLE);
        firstVolley(marble);
        assertEquals(7, shots.size);
        for (int i = 0; i < 4; i++) assertEquals(1, shots.get(i).bounces);
        for (int i = 4; i < 7; i++) assertEquals(0, shots.get(i).bounces);
        assertTrue(shots.get(1).vel.x < 0 && shots.get(3).vel.x > 0);
        Color original = shots.first().color.cpy();
        marble.alive = false; // the tint must still change after the owner has died
        shots.first().onBounce();
        assertEquals(0, shots.first().bounces);
        assertTrue(shots.first().color.r < original.r);
        shots.first().alive = false;
        tick(marble);
        assertEquals(7, shots.size, "a stopped or blanked marble shot never bursts");
    }

    @Test void beetleStopsToFireAndTheOtherCrittersNeverChargeAtTheHero() {
        BedCritter beetle = new BedCritter(3f, 4f, BedCritter.Kind.BEETLE);
        assertTrue(beetle.isPursuer());
        Vector2 before = beetle.pos.cpy();
        boolean exhausted = false;
        for (int frame = 0; frame < 360; frame++) {
            int count = shots.size;
            tick(beetle);
            if (shots.size - count == 10) { exhausted = true; break; }
        }
        assertTrue(exhausted, "the pursuit ends with a ten-shot exhaust ring");
        assertTrue(beetle.pos.x - before.x > 2f, "the beetle really pursues");
        assertTrue(shots.size > 8, "the beetle fires sideways during the pursuit too");
        before.set(beetle.pos);
        for (int i = 0; i < 30; i++) tick(beetle);
        assertTrue(before.epsilonEquals(beetle.pos, .001f), "exhaust is followed by a recovery pause");
        for (BedCritter.Kind kind : new BedCritter.Kind[]{BedCritter.Kind.MARBLE, BedCritter.Kind.DUST_KIT}) {
            shots.clear();
            BedCritter other = new BedCritter(3f, 4f, kind);
            assertFalse(other.isPursuer());
            firstVolley(other);
            assertEquals(3f, other.pos.x, .015f, kind + " moves sideways, not into the hero");
            assertEquals(kind == BedCritter.Kind.MARBLE ? 7 : 3, shots.size);
        }
    }

    @Test void everyResidentThreatensAStationaryHeroFromDifferentDirections() {
        // The previous square and wave designs put their safe lane directly on the hero.
        // Use the actual per-frame circle distance, as the game's enemy-bullet collision does.
        for (int direction = 0; direction < 8; direction++) {
            target.set(Arena.now.centerX, Arena.now.centerY);
            Vector2 start = target.cpy().add(new Vector2(3.2f, 0f).rotateDeg(direction * 45f));
            Array<Enemy> residents = new Array<>();
            for (BedCritter.Kind kind : BedCritter.Kind.values()) residents.add(new BedCritter(start.x, start.y, kind));
            for (BedHeavy.Kind kind : BedHeavy.Kind.values()) residents.add(new BedHeavy(start.x, start.y, kind));
            for (Enemy enemy : residents) {
                shots.clear();
                boolean hit = false;
                for (int frame = 0; frame < 720 && !hit; frame++) {
                    tick(enemy);
                    if (enemy instanceof BedCritter critter && critter.isPursuer()
                            && enemy.pos.dst(target) < enemy.radius * .8f + .1f) hit = true;
                    for (Bullet shot : shots) {
                        if (!shot.alive) continue;
                        if (shot.delay > 0) { shot.update(STEP); continue; }
                        shot.update(STEP);
                        boolean wall = shot.pos.x < Arena.now.left || shot.pos.x > Arena.now.right
                                || shot.pos.y < Arena.now.bottom || shot.pos.y > Arena.now.top;
                        if (wall) {
                            if (shot.bounces == 0) { shot.alive = false; continue; }
                            if (shot.pos.x < Arena.now.left || shot.pos.x > Arena.now.right) shot.vel.x *= -1;
                            if (shot.pos.y < Arena.now.bottom || shot.pos.y > Arena.now.top) shot.vel.y *= -1;
                            shot.pos.set(shot.prev);
                            shot.onBounce();
                        }
                        if (shot.pos.dst(target) < shot.radius * shot.hitScale + .1f) hit = true;
                    }
                }
                assertTrue(hit, enemy.dexId() + " must hit a stationary hero from direction " + direction);
            }
        }
    }

    @Test void residentsMaintainFireBeyondTheirOpeningVolley() {
        // Keep attacks active, but cap sustained fire so mixed rooms leave time to dodge.
        for (BedCritter.Kind kind : BedCritter.Kind.values()) {
            shots.clear();
            Enemy enemy = new BedCritter(3f, 4f, kind);
            for (int frame = 0; frame < 720; frame++) tick(enemy);
            assertTrue(shots.size >= 20 && shots.size <= 80, kind + " must keep moderate pressure over twelve seconds: " + shots.size);
        }
        for (BedHeavy.Kind kind : BedHeavy.Kind.values()) {
            shots.clear();
            Enemy enemy = new BedHeavy(3f, 4f, kind);
            for (int frame = 0; frame < 720; frame++) tick(enemy);
            assertTrue(shots.size >= 45 && shots.size <= 165, kind + " must leave breathing room between formations over twelve seconds: " + shots.size);
        }
    }

    @Test void theMitesSecondSneezeThreadsTheFirstFansGapsAndRotatesTheSkippedOne() {
        BedCritter mite = new BedCritter(3f, 4f, BedCritter.Kind.MITE);
        firstVolley(mite);
        assertEquals(5, shots.size, "the first sneeze is a five-cloud fan");
        for (int frame = 0; frame < 30 && shots.size < 8; frame++) tick(mite);
        assertEquals(8, shots.size, "the answer is three clouds");
        float firstSkipped = skippedGap(shots.get(2).vel.angleDeg(), 5, 8);
        // a whole second sneeze: rest, windup, fan, answer
        for (int frame = 0; frame < 300 && shots.size < 16; frame++) tick(mite);
        assertEquals(16, shots.size);
        assertNotEquals(firstSkipped, skippedGap(shots.get(10).vel.angleDeg(), 13, 16),
                "the skipped gap rotates, so no lane stays safe two sneezes running");
    }

    /** Which of the four gaps between the fan's clouds the answer left empty. */
    private float skippedGap(float base, int from, int to) {
        for (float gap : new float[]{-31.5f, -10.5f, 10.5f, 31.5f}) {
            boolean filled = false;
            for (int i = from; i < to; i++) {
                float off = (shots.get(i).vel.angleDeg() - base + 540f) % 360f - 180f;
                if (Math.abs(off - gap) < .6f) filled = true;
            }
            if (!filled) return gap;
        }
        fail("every gap was filled - the answer no longer skips one");
        return 0f;
    }

    @Test void theMarbleDropsOneLateBallWhereTheHeroWent() {
        BedCritter marble = new BedCritter(3f, 4f, BedCritter.Kind.MARBLE);
        firstVolley(marble);
        assertEquals(7, shots.size);
        target.set(3f, 1f); // the hero dodged below while the volley was in the air
        for (int frame = 0; frame < 30 && shots.size < 8; frame++) tick(marble);
        assertEquals(8, shots.size, "one late ball follows the volley");
        assertEquals(270f, shots.get(7).vel.angleDeg(), 1f, "it is aimed where the hero is now, not where they were");
    }

}
