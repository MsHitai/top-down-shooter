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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class PinwheelTest {
    private static final float STEP = 1f / 60f;
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
        world.getPlayer().setFeet(Arena.now.left + 0.5f, Arena.now.centerY);
        x = world.getPlayer().centerX();
        y = world.getPlayer().hitboxY();
    }

    @AfterEach
    void restoreArena() {
        Arena.now = previousArena;
    }

    @Test
    void padShotPursuesAnAimedAtEnemyPastTheReticleThenSpinsAndBursts() {
        world.setPadAim(true);
        world.getAim().set(x + 1.6f, y);
        BulletKin target = target(x + 3.5f, y);
        BulletKin offAim = target(x + 1f, y + 1f);
        Bullet shot = fire();

        assertSame(target, shot.pinwheelTarget);
        for (int i = 0; i < 15; i++) {
            world.updatePlayerBullets(STEP);
        }
        target.pos.y += 0.5f;
        for (int i = 0; i < 40 && shot.pinwheelTarget != null; i++) {
            world.updatePlayerBullets(STEP);
        }

        assertEquals(88f, target.hp, 0.001f);
        assertTrue(shot.vel.isZero());
        assertTrue(shot.alive);
        assertTrue(shot.pos.x > world.getAim().x);
        world.updatePlayerBullets(0.1f);
        assertTrue(shot.alive);
        assertEquals(88f, target.hp, 0.001f);

        for (int i = 0; i < 120 && world.getPlayerBullets().size > 0; i++) {
            world.updatePlayerBullets(STEP);
        }
        assertEquals(72f, target.hp, 0.001f);
        assertEquals(100f, offAim.hp);
    }

    @Test
    void mouseShotPrefersTheEnemyUnderTheCursor() {
        target(x + 2f, y + 0.1f);
        BulletKin underCursor = target(x + 3.5f, y);
        world.getAim().set(underCursor.pos);

        assertSame(underCursor, fire().pinwheelTarget);
    }

    @Test
    void pinwheelCannotLockThroughCover() {
        world.setPadAim(true);
        world.getAim().set(x + 1.6f, y);
        target(x + 3.5f, y);
        world.getRoom().walls.add(new Rectangle(x + 1.8f, y - 0.5f, 0.2f, 1f));

        assertNull(fire().pinwheelTarget);
    }

    private BulletKin target(float targetX, float targetY) {
        BulletKin enemy = new BulletKin(targetX, targetY);
        enemy.update(BulletKin.SPAWN_TIME, world);
        enemy.hp = 100f;
        world.getEnemies().add(enemy);
        return enemy;
    }

    private Bullet fire() {
        assertTrue(Weapon.pinwheel().tryFire(world, x, y, 0f));
        return world.getPlayerBullets().first();
    }
}
