package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/** The gun kin: the red shotgun's temper, bandana, tanker, pirate, blue shotgun and ghost. */
class GunKinTest {

    private static final float STEP = 0.02f;

    private final Array<Bullet> bullets = new Array<>();
    private Player player;
    private GameWorld world;

    @BeforeEach
    void setUp() {
        Arena.now = new Arena(19, 12);
        player = new Player(null);
        player.restore(6, 6, 2, 0, new Array<>(), 0);
        player.setFeet(5, 3);
        world = mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
        when(world.getPlayer()).thenReturn(player);
        when(world.getDifficulty()).thenReturn(1f);
        when(world.onScreen(anyFloat(), anyFloat(), anyFloat())).thenReturn(true);
        doAnswer(inv -> {
            Bullet b = new Bullet(false, inv.getArgument(0), inv.getArgument(1), inv.getArgument(2),
                    inv.getArgument(3), 0.065f, 1f, Color.WHITE);
            bullets.add(b);
            return b;
        }).when(world).spawnEnemyBullet(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }

    /** Runs its brain for {@code seconds}, flying the bullets it fired along. */
    private void run(Enemy e, float seconds) {
        for (float t = 0; t < seconds; t += STEP) {
            e.behave(STEP, world);
            for (Bullet b : bullets) b.update(STEP);
        }
    }

    private int alive() {
        int n = 0;
        for (Bullet b : bullets) if (b.alive) n++;
        return n;
    }

    @Test
    void theBandanaSpraysABurst() {
        BandanaKin k = new BandanaKin(2, 3);
        run(k, 2.3f + 0.35f + BandanaKin.BURST * 0.1f);
        assertEquals(BandanaKin.BURST, bullets.size, "one burst of the machine pistol");
    }

    @Test
    void theTankerWalksItsFireAcrossTheSweep() {
        TankerKin k = new TankerKin(2, 3);
        run(k, 2.9f + 0.6f + TankerKin.STREAM * 0.1f);
        assertEquals(TankerKin.STREAM, bullets.size);
        float first = bullets.first().vel.angleDeg();
        float last = bullets.peek().vel.angleDeg();
        float swept = Math.abs(MathUtils.atan2Deg(MathUtils.sinDeg(last - first), MathUtils.cosDeg(last - first)));
        assertEquals(TankerKin.SWEEP, swept, 1f, "from one side of the player to the other");
    }

    @Test
    void thePiratesCannonballBurstsIntoGrapeshot() {
        PirateKin k = new PirateKin(2, 3);
        for (float t = 0; t < 3f && bullets.size == 0; t += STEP) k.behave(STEP, world);
        assertEquals(1, bullets.size, "one big ball");
        assertTrue(bullets.first().radius > 0.1f);
        run(k, PirateKin.BURST_AT + 0.05f);
        assertFalse(bullets.first().alive, "the ball's gone...");
        assertEquals(PirateKin.GRAPESHOT, alive(), "...into a ring of grapeshot");
    }

    @Test
    void theBlueShotgunPumpsTwiceAndPopsWhenKilled() {
        BlueShotgunKin k = new BlueShotgunKin(2, 3);
        k.spawnTimer = 0f;
        run(k, 2.9f + 0.55f + 0.4f);
        assertEquals(BlueShotgunKin.PELLETS * 2 - 1, bullets.size, "a fan, and one between its pellets");
        bullets.clear();
        k.hit(k.hp + 1f, new Vector2(1, 0), 0f, world);
        assertEquals(BlueShotgunKin.DEATH_RING, bullets.size, "its last pellets fly out in a ring");
    }

    @Test
    void theRedShotgunLosesItsTemperBelowHalfHealth() {
        ShotgunKin k = new ShotgunKin(2, 3);
        k.spawnTimer = 0f;
        run(k, 3.0f + 0.6f);
        assertEquals(ShotgunKin.PELLETS, bullets.size, "a calm fan");
        assertFalse(k.isAngry());
        k.hit(k.maxHp * 0.6f, new Vector2(1, 0), 0f, world);
        assertTrue(k.isAngry(), "it's lost its temper");
        bullets.clear();
        run(k, 3.0f + 0.6f); // (the calm wait it was already in runs out first)
        int angryFan = ShotgunKin.PELLETS + ShotgunKin.ANGRY_PELLETS;
        assertTrue(bullets.size >= angryFan && bullets.size % angryFan == 0, "wider fans: " + bullets.size);
    }

    @Test
    void theRedShotgunFiresATightFanWhereYoureHeading() {
        ShotgunKin k = new ShotgunKin(player.centerX() - 3f, player.hitboxY()); // level with the hero, to the left
        k.spawnTimer = 0f;
        k.speed = 0f; // stays level with them
        player.getVelocity().set(0, 2.6f); // really running up the screen
        run(k, 3.0f + 0.6f);
        assertEquals(ShotgunKin.PELLETS, bullets.size);
        float spread = bullets.peek().vel.angleDeg(bullets.first().vel);
        float middle = bullets.get(ShotgunKin.PELLETS / 2).vel.angleDeg();
        assertEquals(ShotgunKin.FAN, Math.abs(spread), 0.5f, "a tight clump of pellets");
        // right where they'd be is asin(2.6 / 3) = 60 degrees up; it leads 80% of the way
        assertTrue(middle > 30f && middle < 60f, "aimed up ahead of the hero, not at them: " + middle);
    }

    @Test
    void theRedShotgunDoesntLeadAHeroPushingIntoAWall() {
        ShotgunKin k = new ShotgunKin(player.centerX() - 3f, player.hitboxY());
        k.spawnTimer = 0f;
        player.getMoveDir().set(0, 1); // holding up, but not going anywhere
        run(k, 3.0f + 0.6f);
        Bullet middle = bullets.get(ShotgunKin.PELLETS / 2);
        // its line runs right through the hero
        float miss = Math.abs(middle.vel.crs(player.centerX() - middle.pos.x, player.hitboxY() - middle.pos.y))
                / middle.vel.len();
        assertTrue(miss < 0.05f, "straight at them, missed by " + miss);
    }

    @Test
    void anAngryLeadMeetsTheRunningHero() {
        // fired from 3 to the left at 3 u/s, a hero running up at 2 u/s: the bullet and the hero meet
        float t = Enemy.interceptTime(3f, 0f, 0f, 2f, 3f);
        assertEquals(3f * t, (float) Math.hypot(3f, 2f * t), 0.01f);
        assertEquals(0f, Enemy.interceptTime(3f, 0f, 5f, 0f, 3f), "can't catch someone running away faster");
    }

    @Test
    void aHydrantBehindCoverSpraysBackOnceYouShootIt() {
        Hydrant h = new Hydrant(player.centerX() + 2f, player.hitboxY());
        h.spawnTimer = 0f;
        when(world.hasLineOfSight(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat())).thenReturn(false);
        for (float t = 0; t < 4f; t += STEP) h.update(STEP, world);
        assertEquals(0, bullets.size, "it can't see you, so it waits");
        h.provoke(); // a shot slipped through to it
        for (float t = 0; t < 2.4f; t += STEP) h.update(STEP, world);
        assertTrue(bullets.size > 0, "and now it knows where you are");
    }

    @Test
    void theGhostFadesAwayAndWailsARing() {
        when(world.pointIsFree(anyFloat(), anyFloat(), anyFloat())).thenReturn(true);
        when(world.hasLineOfSight(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat())).thenReturn(true);
        GhostKin g = new GhostKin(9, 6);
        g.spawnTimer = 0f;
        assertTrue(g.flying, "it floats over furniture");
        boolean wentThrough = false;
        for (float t = 0; t < 10f && bullets.size == 0; t += STEP) {
            g.behave(STEP, world);
            if (g.phase() == GhostKin.Phase.DRIFT) {
                wentThrough = true;
                assertTrue(g.isUntouchable(), "bullets go right through it while it drifts");
            }
        }
        assertTrue(wentThrough);
        assertEquals(GhostKin.RING + 1, bullets.size, "a ring and one aimed at you");
        float d = g.pos.dst(player.centerX(), player.hitboxY());
        assertTrue(d > 1.4f && d < 2.6f, "it came in close: " + d);
    }
}
