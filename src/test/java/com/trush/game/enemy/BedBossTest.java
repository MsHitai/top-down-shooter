package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Bullet;
import com.trush.game.gfx.Draw;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import com.trush.game.world.Stage;
import java.util.HashSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class BedBossTest {
    private final Array<Bullet> shots = new Array<>();
    private final Array<Bullet> pending = new Array<>();
    private Arena previous;
    private GameWorld world;

    @BeforeEach
    void arena() {
        previous = Arena.now;
        Arena.now = new Arena(Arena.BOSS_COLS, Arena.BOSS_ROWS);
        world = mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
        when(world.getDifficulty()).thenReturn(1f);
        when(world.getPlayer().centerX()).thenReturn(5.4f);
        when(world.getPlayer().hitboxY()).thenReturn(3.1f);
        doAnswer(call -> {
            Bullet shot = new Bullet(false, call.getArgument(0), call.getArgument(1), call.getArgument(2),
                    call.getArgument(3), 0.065f, 1f, Color.WHITE);
            shots.add(shot);
            pending.add(shot);
            return shot;
        }).when(world).spawnEnemyBullet(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }

    @AfterEach
    void restoreArena() {
        Arena.now = previous;
    }

    @Test
    void underTheBedBossesStayAtOrBelowSevenHundredHealth() {
        for (Boss boss : Boss.values()) {
            if (boss.stage == Stage.UNDER_BED) {
                assertTrue(new BedBoss(6f, 5f, boss).maxHp <= 700f, boss.title);
            }
        }
    }

    @Test
    void aBossAtTheWallReturnsTowardsTheMiddleBetweenAttacks() {
        when(world.walkable(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat())).thenReturn(true);
        when(world.getPlayer().centerX()).thenReturn(Arena.now.right - .2f);
        when(world.getPlayer().hitboxY()).thenReturn(4f);
        BedBoss general = new BedBoss(Arena.now.right - 1.1f, 4f, Boss.WIND_UP_GENERAL);
        float distance = general.pos.dst(Arena.now.centerX, Arena.now.centerY);
        advance(general, 1);
        assertTrue(general.pos.dst(Arena.now.centerX, Arena.now.centerY) < distance - .5f,
                "the boss followed a camper towards the wall instead of keeping its fronts in the arena");
        assertTrue(Arena.now.contains(general.pos.x, general.pos.y, general.radius));
    }

    @Test
    void theGeneralBanksThroughOneWallThenTwo() {
        BedBoss general = new BedBoss(6f, 5f, Boss.WIND_UP_GENERAL);
        advance(general, 20);

        Bullet first = shots.get(1); // centre shell follows the committed bank
        assertEquals(1, first.bounces);
        assertEquals(2, first.playerDamage);
        assertTrue(first.vel.x < 0f);
        checkBankReachesAim(first, Arena.now.left, 2f * Arena.now.left - 5.4f);

        Bullet secondPattern = shots.select(b -> b.bounces == 2).iterator().next();
        secondPattern = shots.get(shots.indexOf(secondPattern, true) + 1);
        assertEquals(2, secondPattern.bounces);
        assertTrue(secondPattern.vel.x > 0f);
        float image = 2f * Arena.now.right - (2f * Arena.now.left - 5.4f);
        checkBankReachesAim(secondPattern, Arena.now.right, image);
    }

    private void checkBankReachesAim(Bullet shot, float wall, float image) {
        float wallY = shot.pos.y + (wall - shot.pos.x) * shot.vel.y / shot.vel.x;
        assertTrue(wallY > Arena.now.bottom && wallY < Arena.now.top);
        assertEquals(3.1f, shot.pos.y + (image - shot.pos.x) * shot.vel.y / shot.vel.x, 0.015f);
    }

    @Test
    void dustComesInFromPilesAndGoesOutFromTheBunny() {
        BedBoss bunny = new BedBoss(6f, 5f, Boss.DUST_BUNNY);
        advance(bunny, 2);

        Bullet inhale = shots.first();
        Vector2 toBunny = new Vector2(6f, 5f).sub(inhale.pos);
        assertTrue(toBunny.len() > 2f);
        assertTrue(toBunny.dot(inhale.vel) > 0f);
        assertTrue(inhale.accel > 0f);

        for (int frame = 0; frame < 18 * 60; frame++) {
            shots.clear();
            step(bunny, 1f / 60f);
            for (Bullet puff : shots) {
                if (puff.accel > 0 && puff.pos.epsilonEquals(bunny.pos, .01f)) {
                    Vector2 towardPlayer = new Vector2(5.4f, 3.1f).sub(puff.pos);
                    assertTrue(towardPlayer.dot(puff.vel) > 0f);
                    return;
                }
            }
        }
        fail("the bunny never exhaled from its new position");
    }

    @Test
    void bunnySummonsOnItsOwnFifthTurnAndGivesThePlayerTimeAfterAKitDies() {
        when(world.isHard()).thenReturn(true);
        Array<Enemy> kits = new Array<>();
        doAnswer(call -> {
            kits.add(call.getArgument(0, Enemy.class));
            return null;
        }).when(world).spawnEnemy(org.mockito.ArgumentMatchers.any(Enemy.class));
        BedBoss bunny = new BedBoss(6f, 5f, Boss.DUST_BUNNY);
        float step = GameWorld.bossPace(true) / 60f;
        int volleys = 0;
        for (int frame = 0; frame < 900 && kits.isEmpty(); frame++) {
            shots.clear();
            step(bunny, step);
            if (!shots.isEmpty()) volleys++;
        }
        assertEquals(11, volleys); // the clump attack has two beats, the other three have three
        assertTrue(shots.isEmpty());
        assertEquals(1, kits.size);
        assertEquals("DustBunnyKit", kits.first().dexId());
        for (int frame = 0; frame < 1200; frame++) step(bunny, step);
        assertEquals(1, kits.size);
        kits.first().alive = false;
        for (int frame = 0; frame < 300; frame++) step(bunny, step);
        assertEquals(1, kits.size);
        for (int frame = 0; frame < 900; frame++) step(bunny, step);
        assertEquals(2, kits.size);
    }

    @Test
    void inhaleNeverAppearsBesideAPlayerWaitingAtTheWall() {
        when(world.getPlayer().centerX()).thenReturn(11.8f);
        when(world.getPlayer().hitboxY()).thenReturn(6.5f);
        advance(new BedBoss(3f, 3f, Boss.DUST_BUNNY), 2);
        assertFalse(shots.isEmpty());
        for (Bullet puff : shots) {
            assertTrue(puff.pos.dst(11.8f, 6.5f) >= 2f,
                    "unmarked dust piles must give the player space to react");
        }
    }

    @Test
    void remoteAttacksWarnDuringTheExistingWindupEvenWithVisibleRoomEdges() {
        for (Boss kind : new Boss[]{Boss.DUST_BUNNY, Boss.BED_MONSTER, Boss.LINT_REGENT}) {
            for (boolean hard : new boolean[]{false, true}) {
                shots.clear();
                when(world.isHard()).thenReturn(hard);
                BedBoss boss = new BedBoss(6f, 5f, kind);
                Draw draw = mock(Draw.class, withSettings().mockMaker(MockMakers.SUBCLASS));
                Rectangle view = new Rectangle(Arena.now.left, Arena.now.bottom,
                        Arena.now.right - Arena.now.left, Arena.now.top - Arena.now.bottom);
                float warned = 0f;
                boolean seen = false;
                for (int frame = 0; frame < 300 && shots.isEmpty(); frame++) {
                    boss.behave(GameWorld.bossPace(hard) / 120f, world);
                    boss.drawOverlay(null, draw, view);
                    if (!mockingDetails(draw).getInvocations().isEmpty()) seen = true;
                    if (seen) warned += 1f / 120f;
                }
                assertFalse(shots.isEmpty(), kind + " failed to fire");
                assertTrue(warned >= .5f, kind + " only warned for " + warned + " seconds");
            }
        }
    }

    @Test
    void offscreenWarningsOnlyPointToShotsThatCanEnterTheView() {
        Rectangle view = new Rectangle(3f, 2f, 7f, 5f);
        Bullet shot = new Bullet(false, 1f, 4f, 0f, 3f, .1f, 1f, Color.WHITE);
        assertTrue(BedBoss.approachingView(shot, view));
        shot.vel.x = -3f;
        assertFalse(BedBoss.approachingView(shot, view));
        shot.vel.set(3f, 0f);
        shot.pos.y = 8f;
        assertFalse(BedBoss.approachingView(shot, view));
        shot.pos.set(12f, 8f);
        shot.vel.set(-3f, -2f);
        assertTrue(BedBoss.approachingView(shot, view));
        float entry = BedBoss.incomingTime(shot, view);
        assertEquals(10f, shot.pos.x + shot.vel.x * entry, .001f);
        assertTrue(shot.pos.y + shot.vel.y * entry <= 7f);
        shot.life = .1f;
        assertFalse(BedBoss.approachingView(shot, view));
    }

    /** Press, but readable: each attack (three volleys) stays dense enough that the fight never
     * goes quiet, and sparse enough that the spaces between bullets are streets, not seams.
     * No single volley may become a wall of noise on its own. */
    @Test
    void attacksPressWithoutBecomingNoise() {
        for (Boss kind : Boss.values()) {
            if (kind.stage != Stage.UNDER_BED) continue;
            for (boolean enraged : new boolean[]{false, true}) {
                BedBoss boss = new BedBoss(6f, 5f, kind);
                if (enraged) boss.hp = boss.maxHp * .4f;
                int turns = 0, volleys = 0, bullets = 0;
                shots.clear();
                for (int frame = 0; frame < 2400 && turns < 8; frame++) {
                    step(boss, 1f / 60f);
                    if (shots.isEmpty()) continue;
                    assertTrue(shots.size <= 18, kind + " fired " + shots.size
                            + " bullets in one volley - a wall of noise, not a pattern");
                    bullets += shots.size;
                    shots.clear();
                    if (++volleys == BedBoss.beatsInAttack(kind, turns % 4)) {
                        assertTrue(bullets >= 9, kind + " attack " + (turns + 1)
                                + " enraged=" + enraged + " fired only " + bullets);
                        assertTrue(bullets <= 48, kind + " attack " + (turns + 1)
                                + " enraged=" + enraged + " fired a crushing " + bullets);
                        turns++;
                        volleys = 0;
                        bullets = 0;
                    }
                }
                assertEquals(8, turns, kind.title);
            }
        }
    }

    @Test
    void everyResidentPlaysFourDifferentAttacksBeforeRepeating() {
        for (Boss kind : Boss.values()) {
            if (kind.stage != Stage.UNDER_BED) continue;
            BedBoss boss = new BedBoss(6f, 5f, kind);
            var patterns = new HashSet<String>();
            int turns = 0, beats = 0;
            StringBuilder signature = new StringBuilder();
            shots.clear();
            for (int frame = 0; frame < 1200 && turns < 4; frame++) {
                step(boss, 1f / 60f);
                if (shots.isEmpty()) continue;
                for (Bullet bullet : shots) {
                    // Every fourth attack is the warned special: its lanes are announced during
                    // wind-up, so they may enter from the room's edges rather than the body.
                    if (turns > 0) {
                        assertTrue(bullet.pos.dst(boss.pos) <= 1.6f,
                                kind.title + " must keep unmarked formations within its visible body");
                    }
                    signature.append(bullet.shape);
                    for (int tick = 0; tick < 8; tick++) {
                        bullet.update(.1f);
                        signature.append('/').append(Math.round(bullet.pos.x * 10))
                                .append(',').append(Math.round(bullet.pos.y * 10));
                    }
                }
                shots.clear();
                if (++beats == BedBoss.beatsInAttack(kind, turns)) {
                    turns++;
                    beats = 0;
                    patterns.add(signature.toString());
                    signature.setLength(0);
                }
            }
            assertEquals(4, patterns.size(), kind.title);
        }
    }

    @Test
    void everyResidentThreatensAStationaryPlayerInEveryDirection() {
        when(world.walkable(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat())).thenReturn(true);
        for (Boss kind : Boss.values()) {
            if (kind.stage != Stage.UNDER_BED) continue;
            for (int direction = 0; direction < 360; direction += 45) {
                float x = 6f + MathUtils.cosDeg(direction) * 3f;
                float y = 4f + MathUtils.sinDeg(direction) * 3f;
                when(world.getPlayer().centerX()).thenReturn(x);
                when(world.getPlayer().hitboxY()).thenReturn(y);
                BedBoss boss = new BedBoss(6f, 4f, kind);
                Vector2 hero = new Vector2(x, y);
                shots.clear();
                boolean threatened = false;
                for (int frame = 0; frame < 960 && !threatened; frame++) {
                    boss.behave(1f / 60f, world);
                    for (Bullet shot : shots) {
                        if (!shot.alive) continue;
                        shot.update(1f / 60f);
                        if (Intersector.distanceSegmentPoint(shot.prev, shot.pos, hero)
                                < Player.HITBOX_RADIUS + shot.radius * .8f) {
                            threatened = true;
                            break;
                        }
                    }
                }
                assertTrue(threatened, kind + " leaves a permanent safe spot at " + direction);
            }
        }
    }

    @Test
    void ninjaAimsOneFollowUpWhileTheRestSweepTheBaitedLine() {
        BedBoss ninja = new BedBoss(6f, 4f, Boss.SOCK_NINJA);
        int volleys = 0;
        float firstHeading = 0f;
        for (int frame = 0; frame < 1200 && volleys < 13; frame++) {
            shots.clear();
            step(ninja, 1f / 60f);
            if (shots.isEmpty()) continue;
            if (volleys >= 9) {
                float heading = shots.get(1).vel.angleDeg();
                if (volleys == 9) firstHeading = heading;
                if (volleys == 11) {
                    float aimed = new Vector2(ninja.targetX(world), ninja.targetY(world)).sub(ninja.pos).angleDeg();
                    assertEquals(aimed, heading, .02f);
                } else {
                    float expected = MathUtils.cosDeg(volleys == 12 ? 43f : (volleys - 9) * 18f);
                    assertEquals(expected, MathUtils.cosDeg(heading - firstHeading), .002f);
                }
                when(world.getPlayer().centerX()).thenReturn(volleys % 2 == 0 ? 3f : 9f);
                when(world.getPlayer().hitboxY()).thenReturn(volleys % 2 == 0 ? 2f : 6f);
            }
            volleys++;
        }
        assertEquals(13, volleys);
    }

    /** Geometry tests consume each volley; lifetime/overlap is exercised separately. */
    private void step(BedBoss boss, float delta) {
        for (Bullet shot : pending) shot.alive = false;
        pending.clear();
        boss.behave(delta, world);
    }

    private void advance(BedBoss boss, int seconds) {
        for (int i = 0; i < seconds * 60; i++) step(boss, 1f / 60f);
    }
}
