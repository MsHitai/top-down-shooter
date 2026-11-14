package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import com.trush.game.world.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class EnemyPursuitTest {

    private final Array<Bullet> bullets = new Array<>();
    private final Array<Bullet> enemyBullets = new Array<>();
    private Player player;
    private GameWorld world;

    @BeforeEach
    void setUp() {
        Arena.now = new Arena(Arena.MEDIUM_COLS, Arena.MEDIUM_ROWS);
        player = new Player(null);
        player.setFeet(5f, 3f);
        world = mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
        when(world.getPlayer()).thenReturn(player);
        doReturn(new Room(0, 0, Room.Type.COMBAT)).when(world).getRoom();
        when(world.getEnemyBullets()).thenReturn(enemyBullets);
        when(world.getDifficulty()).thenReturn(1f);
        when(world.hasLineOfSight(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat())).thenReturn(true);
        when(world.walkable(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat())).thenReturn(true);
        when(world.pointIsFree(anyFloat(), anyFloat(), anyFloat())).thenReturn(true);
        doAnswer(inv -> {
            Bullet b = new Bullet(false, inv.getArgument(0), inv.getArgument(1), inv.getArgument(2),
                    inv.getArgument(3), 0.065f, 1f, Color.WHITE);
            bullets.add(b);
            return b;
        }).when(world).spawnEnemyBullet(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }

    private void run(Enemy enemy, float seconds) {
        enemy.update(1f, world);
        for (float t = 0; t < seconds; t += 0.02f) enemy.update(0.02f, world);
    }

    @Test
    void aVisibleGunKinApproachesAndFiresFromBeyondTheCamera() {
        when(world.onScreen(anyFloat(), anyFloat(), anyFloat())).thenReturn(false);
        BulletKin enemy = new BulletKin(1f, 3f);
        float before = enemy.distanceToPlayer(world);
        run(enemy, 3f);
        assertTrue(enemy.distanceToPlayer(world) < before);
        assertFalse(bullets.isEmpty(), "a clear shot is allowed even while the enemy is off camera");
    }

    @Test
    void aCubeCanStartItsAttackFromBeyondTheCamera() {
        when(world.onScreen(anyFloat(), anyFloat(), anyFloat())).thenReturn(false);
        Cube cube = Cube.lead(1f, player.hitboxY());
        float x = cube.pos.x;
        run(cube, 3f);
        assertTrue(cube.pos.x > x || !bullets.isEmpty());
    }

    @Test
    void enteringTheCameraDoesNotImmediatelyTurnASniperAround() {
        when(world.onScreen(anyFloat(), anyFloat(), anyFloat())).thenReturn(false);
        Sniper sniper = new Sniper(5f, player.hitboxY() - 3f);
        sniper.update(1f, world);
        sniper.update(0.02f, world);
        when(world.onScreen(anyFloat(), anyFloat(), anyFloat())).thenAnswer(inv -> inv.getArgument(2, Float.class) <= 0.4f);
        float y = sniper.pos.y;
        sniper.update(0.02f, world);
        assertTrue(sniper.pos.y > y, "it should finish entering the frame before retreating to its preferred distance");
    }

    @Test
    void aBlockedStrafeChangesDirectionWithoutWaitingForTheTimer() {
        BulletKin enemy = new BulletKin(2.8f, player.hitboxY());
        enemy.strafeDir = 1;
        enemy.strafeTimer = 2.5f;
        // The route towards the player is clear, but furniture blocks the upward sidestep.
        when(world.walkable(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat()))
                .thenAnswer(inv -> inv.getArgument(3, Float.class) <= enemy.pos.y);
        float y = enemy.pos.y;
        enemy.keepDistance(1f / 60f, world, 2.2f, enemy.speed);
        assertTrue(enemy.pos.y < y, "must step away from the obstruction on the first frame");
        assertEquals(-1f, enemy.strafeDir);
    }

    @Test
    void anOrdinaryShotShovesEnoughButBossesStayPut() {
        BulletKin enemy = new BulletKin(1f, 3f);
        enemy.update(1f, world);
        enemy.hit(0f, new Vector2(1f, 0f), 0.6f, world);
        assertEquals(2.4f, enemy.knockSpeed(), 1e-4f);
        BulletKin boss = new BulletKin(1f, 3f);
        boss.boss = true;
        boss.update(1f, world);
        boss.hit(0f, new Vector2(1f, 0f), 3f, world);
        assertEquals(0f, boss.knockSpeed(), 1e-4f);
    }
}
