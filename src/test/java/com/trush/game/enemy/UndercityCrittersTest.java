package com.trush.game.enemy;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/** The Undercity's sewer gator, alley dog and raccoon. */
class UndercityCrittersTest {

    private static final float STEP = 0.02f;

    /** A stand-in world with a real player at (5, 3), collecting what it's asked to spawn. */
    private static GameWorld world(Player p, Array<Enemy> spawned) {
        GameWorld w = mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
        when(w.getPlayer()).thenReturn(p);
        when(w.getDifficulty()).thenReturn(1f);
        when(w.onScreen(anyFloat(), anyFloat(), anyFloat())).thenReturn(true);
        when(w.getBlankRing()).thenReturn(-1f);
        when(w.findSpawnPoint(anyFloat())).thenReturn(new Vector2(2, 2));
        doAnswer(inv -> {
            spawned.add(inv.getArgument(0));
            return null;
        }).when(w).spawnEnemy(any());
        doAnswer(inv -> new com.trush.game.objects.Bullet(false, inv.getArgument(0), inv.getArgument(1),
                inv.getArgument(2), inv.getArgument(3), 0.06f, 1f, com.badlogic.gdx.graphics.Color.WHITE))
                .when(w).spawnEnemyBullet(anyFloat(), anyFloat(), anyFloat(), anyFloat());
        return w;
    }

    private static Player player(int coins) {
        Player p = new Player(null);
        p.restore(6, 6, 2, 0, new Array<>(), 0);
        p.setFeet(5, 3);
        p.setCoins(coins);
        return p;
    }

    @Test
    void theRaccoonSnatchesCoinsAndRuns() {
        Arena.now = new Arena(19, 12);
        Player p = player(30);
        GameWorld w = world(p, new Array<>());
        Raccoon r = new Raccoon(p.centerX(), p.hitboxY());
        r.spawnTimer = 0f;
        assertFalse(r.hurtsToTouch, "it's after your coins, not you");
        r.behave(STEP, w);
        assertEquals(Raccoon.GRAB, r.stolen());
        assertEquals(30 - Raccoon.GRAB, p.getCoins());
        for (float t = 0; t < Raccoon.GETAWAY - 1f; t += STEP) r.behave(STEP, w);
        assertTrue(r.alive, "still running with the sack");
        r.hit(r.hp + 1f, new Vector2(1, 0), 0f, w); // shot down
        verify(w).dropCoins(anyFloat(), anyFloat(), eq(Raccoon.GRAB + 4)); // what it took, and a bit more
    }

    @Test
    void theRaccoonGetsAwayIfLeftAlone() {
        Arena.now = new Arena(19, 12);
        Player p = player(5);
        GameWorld w = world(p, new Array<>());
        Raccoon r = new Raccoon(p.centerX(), p.hitboxY());
        r.behave(STEP, w);
        assertEquals(5, r.stolen(), "all you had");
        assertEquals(0, p.getCoins());
        for (float t = 0; t < Raccoon.GETAWAY + 0.5f && r.alive; t += STEP) r.behave(STEP, w);
        assertFalse(r.alive);
        assertTrue(r.escaped(), "down a drain with your coins");
    }

    @Test
    void aBrokeHeroJustGetsBitten() {
        Arena.now = new Arena(19, 12);
        Player p = player(0);
        GameWorld w = world(p, new Array<>());
        Raccoon r = new Raccoon(p.centerX() + 2f, p.hitboxY());
        r.behave(STEP, w);
        assertTrue(r.hurtsToTouch);
        assertEquals(0, r.stolen());
    }

    @Test
    void theGatorNeverHidesAndThrashesARingOfDrops() {
        Arena.now = new Arena(19, 12);
        Player p = player(0);
        GameWorld w = world(p, new Array<>());
        SewerGator g = new SewerGator(p.centerX() - 2f, p.hitboxY()); // (left of the hero: behave alone aims right)
        g.spawnTimer = 0f;
        float start = g.pos.dst(p.centerX(), p.hitboxY());
        for (float t = 0; t < 4.5f; t += STEP) {
            g.behave(STEP, w);
            assertFalse(g.isUntouchable(), "always there to be shot");
            assertTrue(g.hurtsToTouch, "and it bites");
        }
        assertTrue(g.pos.dst(p.centerX(), p.hitboxY()) < start, "it comes at you");
        verify(w, org.mockito.Mockito.atLeast(12)).spawnEnemyBullet(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }

    @Test
    void theSkunkKingLosesHisCrownAtHalfHealthAndPanics() {
        Arena.now = new Arena(25, 15);
        Player p = player(0);
        GameWorld w = world(p, new Array<>());
        SkunkKing king = new SkunkKing(Arena.now.centerX, Arena.now.top - 1.1f, 0);
        king.spawnTimer = 0f;
        assertEquals("THE SKUNK KING", king.bossName());
        for (float t = 0; t < 3f; t += STEP) king.behave(STEP, w);
        assertFalse(king.isEnraged(), "lording it on his throne");
        assertTrue(Math.abs(king.pos.y - (Arena.now.top - 1.0f)) < 0.2f, "riding his cart along the top");
        king.hit(king.maxHp * (1f - SkunkKing.CROWN_FALLS) + 1f, new Vector2(1, 0), 0f, w);
        king.behave(STEP, w);
        assertTrue(king.isEnraged(), "his crown fell off");
        assertTrue(king.isUntouchable(), "tumbling out of the cart");
        for (float t = 0; t < 2f; t += STEP) king.behave(STEP, w);
        assertFalse(king.isUntouchable(), "back on his feet, panicking");
        for (float t = 0; t < 15f; t += STEP) king.behave(STEP, w); // spins, throws junk, lobs bottles
        verify(w, org.mockito.Mockito.atLeastOnce()).releaseGas(any(), anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                anyFloat(), anyFloat());
    }

    @Test
    void theDogsHowlCallsAMarkedRatPackTwiceAtMost() {
        Arena.now = new Arena(19, 12);
        Player p = player(0);
        Array<Enemy> spawned = new Array<>();
        GameWorld w = world(p, spawned);
        AlleyDog dog = new AlleyDog(p.centerX() + 1.5f, p.hitboxY());
        dog.spawnTimer = 0f;
        for (float t = 0; t < 40f; t += STEP) dog.behave(STEP, w);
        assertEquals(AlleyDog.HOWLS, dog.howls());
        assertEquals(3 * AlleyDog.HOWLS, spawned.size);
        for (Enemy e : spawned) {
            assertTrue(e instanceof Rat && ((Rat) e).isMarked());
        }
    }
}
