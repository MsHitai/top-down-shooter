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
    void theGatorCantBeTouchedUnderTheWaterThenSurfacesBesideYou() {
        Arena.now = new Arena(19, 12);
        Player p = player(0);
        GameWorld w = world(p, new Array<>());
        SewerGator g = new SewerGator(p.centerX() + 0.5f, p.hitboxY());
        g.spawnTimer = 0f;
        assertTrue(g.isUntouchable(), "under the water");
        boolean surfaced = false;
        for (float t = 0; t < 4f && !surfaced; t += STEP) {
            g.behave(STEP, w);
            surfaced = !g.isUntouchable();
        }
        assertTrue(surfaced, "up it comes");
        assertTrue(g.hurtsToTouch, "and it bites");
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
