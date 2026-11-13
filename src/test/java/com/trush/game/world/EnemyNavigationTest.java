package com.trush.game.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.audio.Sfx;
import com.trush.game.enemy.BulletKin;
import com.trush.game.launcher.ETGFanfic;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class EnemyNavigationTest {

    private static GameWorld worldWithCentralPit() {
        ETGFanfic game = mock(ETGFanfic.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
        game.sfx = mock(Sfx.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        GameWorld world = new GameWorld(game, new StoryDirector(1, false));
        Arena a = world.getRoom().arena;
        world.getRoom().props.clear();
        world.getRoom().pits.add(new Rectangle(a.centerX - 0.5f, a.centerY - 0.5f, 1f, 1f));
        world.getPlayer().setFeet(a.centerX + 2f, a.centerY - 0.07f);
        return world;
    }

    @Test
    void aStraightWalkCannotShortcutThroughAPit() {
        GameWorld world = worldWithCentralPit();
        Arena a = Arena.now;
        assertFalse(world.walkable(a.centerX - 2f, a.centerY, a.centerX + 2f, a.centerY, 0.16f));
        assertTrue(world.walkable(a.centerX - 2f, a.centerY + 1f, a.centerX + 2f, a.centerY + 1f, 0.16f));
    }

    @Test
    void theRouteToAVisiblePlayerGoesAroundThePit() {
        GameWorld world = worldWithCentralPit();
        Arena a = Arena.now;
        Vector2 from = new Vector2(a.centerX - 2f, a.centerY);
        Vector2 next = new Vector2();
        assertTrue(world.findWalkPoint(from, 0.2f, world.getPlayer().centerX(), world.getPlayer().hitboxY(), next));
        assertTrue(Math.abs(next.y - a.centerY) > 0.4f, "the first shortcut should leave the blocked row, was " + next);
        assertTrue(world.walkable(from.x, from.y, next.x, next.y, 0.16f));
    }

    @Test
    void aGunKinActuallyTakesTheDetourInsteadOfPushingAgainstTheEdge() {
        GameWorld world = worldWithCentralPit();
        Arena a = Arena.now;
        BulletKin enemy = new BulletKin(a.centerX - 2f, a.centerY);
        enemy.update(1f, world);
        for (int i = 0; i < 60; i++) enemy.update(1f / 60f, world);
        assertTrue(Math.abs(enemy.pos.y - a.centerY) > 0.25f, "stayed on the blocked row at " + enemy.pos);
        assertFalse(world.inPit(enemy.pos.x, enemy.pos.y, 0.08f));
        assertTrue(enemy.alive);
    }

    @Test
    void anOrdinaryShotCanShoveAnEnemyOverTheEdge() {
        GameWorld world = worldWithCentralPit();
        Arena a = Arena.now;
        BulletKin enemy = new BulletKin(a.centerX, a.centerY - 0.4f);
        world.getEnemies().add(enemy);
        enemy.update(1f, world);
        enemy.hit(0f, new Vector2(0f, 1f), 0.6f, world);
        enemy.update(0.1f, world);
        world.updateEnemyPits();
        assertFalse(enemy.alive, "centre " + a.centerX + "," + a.centerY + "; ended at " + enemy.pos
                + " with knock speed " + enemy.knockSpeed() + "; in pit " + world.inPit(enemy.pos.x, enemy.pos.y, 0.08f));
        assertTrue(enemy.fellInPit);
    }
}
