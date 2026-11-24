package com.trush.game.world;

import com.trush.game.audio.Sfx;
import com.trush.game.enemy.BulletKin;
import com.trush.game.gfx.Sprites;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.player.Hero;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.withSettings;

class RoyalSceptreTest {
    private final Arena previousArena = Arena.now;
    private GameWorld world;
    private float x;
    private float y;

    @BeforeEach
    void setUp() {
        ETGFanfic game = mock(ETGFanfic.class,
                withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
        game.sfx = mock(Sfx.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        game.sprites = mock(Sprites.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        world = new GameWorld(game, new StoryDirector(1, false));
        world.getRoom().props.clear();
        world.getRoom().walls.clear();
        world.getEnemies().clear();
        x = world.getRoom().arena.centerX;
        y = world.getRoom().arena.centerY;
    }

    @AfterEach
    void restoreArena() {
        Arena.now = previousArena;
    }

    @Test
    void allSixFragmentsCanHitTheOriginalTarget() {
        BulletKin direct = target(x + 0.9f, y, 30f);
        BulletKin farther = target(x + 0.9f, y + 1.4f, 30f);

        fireAndResolve(x);

        assertEquals(16f, direct.hp);
        assertEquals(30f, farther.hp);
    }

    @Test
    void remainingFragmentsRetargetAfterTheNearestEnemyDies() {
        BulletKin direct = target(x + 0.9f, y, 6.5f);
        BulletKin next = target(x + 0.9f, y + 1.2f, 30f);
        BulletKin farther = target(x + 2.5f, y, 30f);

        fireAndResolve(x);

        assertFalse(direct.alive);
        assertEquals(22.5f, next.hp);
        assertEquals(30f, farther.hp);
    }

    @Test
    void anExpiredShotSendsItsFragmentsTowardTheNearestEnemy() {
        float shotX = x - 2f;
        BulletKin nearest = target(shotX + 3.6f, y + 0.8f, 30f);
        BulletKin farther = target(shotX + 3.6f, y - 1.5f, 30f);

        fireAndResolve(shotX);

        assertEquals(21f, nearest.hp);
        assertEquals(30f, farther.hp);
    }

    private BulletKin target(float targetX, float targetY, float hp) {
        BulletKin enemy = new BulletKin(targetX, targetY);
        enemy.update(BulletKin.SPAWN_TIME, world);
        enemy.hp = hp;
        world.getEnemies().add(enemy);
        return enemy;
    }

    private void fireAndResolve(float shotX) {
        world.getPlayer().setFeet(shotX, y);
        assertTrue(Hero.KING.starter().tryFire(world, shotX, y, 0f));
        for (int frame = 0; frame < 120 && world.getPlayerBullets().size > 0; frame++) {
            world.updatePlayerBullets(1f / 60f);
        }
        assertEquals(0, world.getPlayerBullets().size);
    }
}
