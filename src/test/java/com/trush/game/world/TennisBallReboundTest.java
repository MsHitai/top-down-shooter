package com.trush.game.world;

import com.badlogic.gdx.math.Rectangle;
import com.trush.game.audio.Sfx;
import com.trush.game.enemy.BulletKin;
import com.trush.game.gfx.Sprites;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Bullet;
import com.trush.game.objects.Weapon;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class TennisBallReboundTest {
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
        x = Arena.now.centerX - 1f;
        y = Arena.now.centerY;
        world.getPlayer().setFeet(x, y);
    }

    @AfterEach
    void restoreArena() {
        Arena.now = previousArena;
    }

    @Test
    void reboundSkipsAnEnemyBehindCoverAndHitsTheNextVisibleOne() {
        BulletKin first = target(x + 0.9f, y);
        BulletKin covered = target(x + 0.9f, y - 0.7f);
        BulletKin open = target(x + 1.5f, y + 1f);
        world.getRoom().walls.add(new Rectangle(x + 0.35f, y - 0.5f, 1f, 0.2f));
        Bullet ball = fire();

        for (int frame = 0; frame < 75 && open.hp == 100f; frame++) {
            world.updatePlayerBullets(1f / 60f);
        }

        assertEquals(91.5f, first.hp, 0.001f);
        assertEquals(91.5f, open.hp, 0.001f);
        assertEquals(100f, covered.hp);
        assertEquals(2, ball.bounces);
        assertTrue(ball.alreadyHit.contains(first, true));
        assertTrue(ball.alreadyHit.contains(open, true));
    }

    @Test
    void ballStillBouncesBackAndSpeedsUpWithNoSecondTarget() {
        BulletKin first = target(x + 0.9f, y);
        Bullet ball = fire();
        float launchSpeed = ball.vel.len();

        for (int frame = 0; frame < 60 && first.hp == 100f; frame++) {
            world.updatePlayerBullets(1f / 60f);
        }

        assertEquals(91.5f, first.hp, 0.001f);
        assertTrue(ball.vel.x < 0f);
        assertEquals(launchSpeed * 1.2f, ball.vel.len(), 0.001f);
        assertEquals(3, ball.bounces);
    }

    private BulletKin target(float posX, float posY) {
        BulletKin enemy = new BulletKin(posX, posY);
        enemy.update(BulletKin.SPAWN_TIME, world);
        enemy.hp = 100f;
        world.getEnemies().add(enemy);
        return enemy;
    }

    private Bullet fire() {
        assertTrue(Weapon.tennisBalls().tryFire(world, x, y, 0f));
        return world.getPlayerBullets().first();
    }
}
