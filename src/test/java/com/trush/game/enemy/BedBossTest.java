package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class BedBossTest {
    private final Array<Bullet> shots = new Array<>();
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
            return shot;
        }).when(world).spawnEnemyBullet(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }

    @AfterEach
    void restoreArena() {
        Arena.now = previous;
    }

    @Test
    void theGeneralBanksThroughOneWallThenTwo() {
        BedBoss general = new BedBoss(6f, 5f, Boss.WIND_UP_GENERAL);
        advance(general, 9);

        Bullet first = shots.get(1); // centre shell of the first three-shot salvo
        assertEquals(1, first.bounces);
        assertTrue(first.vel.x < 0f);
        checkBankReachesAim(first, Arena.now.left, 2f * Arena.now.left - 5.4f);

        Bullet secondPattern = shots.get(10); // first centre shell of the second pattern
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
        advance(bunny, 9);

        Bullet inhale = shots.first();
        Vector2 toBunny = new Vector2(6f, 5f).sub(inhale.pos);
        assertTrue(toBunny.len() > 2f);
        assertTrue(toBunny.dot(inhale.vel) > 0f);
        assertTrue(inhale.accel > 0f);

        Bullet exhale = shots.get(8);
        assertEquals(6f, exhale.pos.x);
        assertEquals(5f, exhale.pos.y);
        assertTrue(exhale.accel > 0f);
    }

    private void advance(BedBoss boss, int seconds) {
        for (int i = 0; i < seconds * 60; i++) boss.behave(1f / 60f, world);
    }
}
