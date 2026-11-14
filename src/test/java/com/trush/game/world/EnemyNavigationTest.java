package com.trush.game.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.audio.Sfx;
import com.trush.game.enemy.BulletKin;
import com.trush.game.gfx.Sprites;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Prop;
import com.trush.game.objects.Bullet;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class EnemyNavigationTest {

    private static GameWorld worldWithCentralPit() {
        ETGFanfic game = mock(ETGFanfic.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
        game.sfx = mock(Sfx.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        game.sprites = mock(Sprites.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        TextureRegion region = mock(TextureRegion.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(region.getRegionWidth()).thenReturn(32);
        when(region.getRegionHeight()).thenReturn(32);
        when(game.sprites.prop(anyString())).thenReturn(region);
        GameWorld world = new GameWorld(game, new StoryDirector(1, false));
        Arena a = world.getRoom().arena;
        world.getRoom().props.clear();
        world.getRoom().pits.add(new Rectangle(a.centerX - 0.5f, a.centerY - 0.5f, 1f, 1f));
        world.getPlayer().setFeet(a.centerX + 2f, a.centerY - 0.07f);
        return world;
    }

    @Test
    void pursuitCanReachTheBankWhenTheTargetIsOverAPit() {
        GameWorld world = worldWithCentralPit();
        Arena a = Arena.now;
        Vector2 from = new Vector2(a.centerX - 2f, a.centerY);
        Vector2 next = new Vector2();
        assertTrue(world.findWalkPoint(from, 0.2f, a.centerX, a.centerY, next));
        assertTrue(world.walkable(from.x, from.y, next.x, next.y, 0.16f));
    }

    @Test
    void aGunKinFlipsNearbyTableAndLeavesCoverToShoot() {
        GameWorld world = worldWithCentralPit();
        world.getRoom().pits.clear();
        Arena a = Arena.now;
        Prop table = new Prop(
                Prop.Kind.TABLE, a.centerX, a.centerY);
        world.getRoom().props.add(table);
        world.getPlayer().setFeet(a.centerX + 2f, a.centerY - 0.07f);
        BulletKin enemy = new BulletKin(a.centerX - 1f, a.centerY);
        // A shot approaching the kin, rather than unrelated fire elsewhere in the room.
        world.addPlayerBullet(new Bullet(true, a.centerX + 1f, a.centerY, 180f,
                4f, 0.065f, 1f, Color.WHITE));
        enemy.update(1f, world);
        boolean shot = false;
        Rectangle standing = new Rectangle(table.solid);
        for (int i = 0; i < 600; i++) {
            boolean flipped = table.isFlipped();
            int bulletsBefore = world.getEnemyBullets().size;
            enemy.update(1f / 60f, world);
            if (!flipped && table.isFlipped()) {
                float x = Math.max(standing.x, Math.min(enemy.pos.x, standing.x + standing.width));
                float y = Math.max(standing.y, Math.min(enemy.pos.y, standing.y + standing.height));
                assertTrue(enemy.pos.dst(x, y) <= enemy.radius + 0.11f,
                        "must walk within arm's reach before flipping");
            }
            for (int n = bulletsBefore; n < world.getEnemyBullets().size; n++) {
                Bullet bullet = world.getEnemyBullets().get(n);
                Vector2 end = new Vector2(bullet.vel).setLength(bullet.pos.dst(
                        world.getPlayer().centerX(), world.getPlayer().hitboxY())).add(bullet.pos);
                assertTrue(world.hasLineOfSight(bullet.pos.x, bullet.pos.y, end.x, end.y, bullet.radius),
                        "the actual shot must clear the table, including spread");
                shot = true;
            }
            assertTrue(world.pointIsFree(enemy.pos.x, enemy.pos.y, enemy.radius * 0.8f));
        }
        assertTrue(table.isFlipped(), "the grunt should turn the table into cover");
        assertTrue(shot, "taking cover must not prevent attacks");
    }

    @Test
    void aKinIgnoresCoverWhenThePlayerAimsAndShootsElsewhere() {
        GameWorld world = worldWithCentralPit();
        world.getRoom().pits.clear();
        Arena a = Arena.now;
        Prop table = new Prop(Prop.Kind.TABLE, a.centerX, a.centerY);
        world.getRoom().props.add(table);
        BulletKin enemy = new BulletKin(a.centerX - 1f, a.centerY);
        float before = enemy.pos.dst(world.getPlayer().centerX(), world.getPlayer().hitboxY());
        world.addPlayerBullet(new Bullet(true, a.centerX + 2f, a.centerY, 90f,
                4f, 0.065f, 1f, Color.WHITE));
        enemy.update(1f, world);
        for (int i = 0; i < 180; i++) {
            enemy.update(1f / 60f, world);
            world.pushOutOfSolids(enemy.pos, enemy.radius * 0.8f);
        }
        assertFalse(table.isFlipped(), "unrelated fire must not trigger cover");
        assertTrue(enemy.pos.dst(world.getPlayer().centerX(), world.getPlayer().hitboxY()) < before);
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
