package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/** Grandfluff's two lives: a golden one (the old, then the young king), then the Moon's red one. */
class GrandfluffTest {

    private static GameWorld world() {
        return mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(Answers.RETURNS_DEEP_STUBS));
    }

    @Test
    void theGoldenLifeGoesFirstAndTheRedOneWaitsFullUnderIt() {
        Grandfluff g = new Grandfluff(0, 0, 0);
        g.spawnTimer = 0f;
        assertEquals(1f, g.bossBarFraction(), "the red bar sits full under the gold");
        assertEquals(1f, g.bossBarGold());
        g.hp = g.maxHp * 0.3f;
        assertEquals(0.3f, g.bossBarGold(), 0.001f);
        assertEquals(1f, g.bossBarFraction());
    }

    @Test
    void theLastHitOnTheGoldDoesNotFinishHimHeRisesAsTheMoon() {
        GameWorld world = world();
        Grandfluff g = new Grandfluff(0, 0, 0);
        g.spawnTimer = 0f;
        g.hp = 5f;
        g.hit(50f, new Vector2(1, 0), 0f, world);
        assertTrue(g.alive, "not beaten yet");
        assertEquals(g.maxHp, g.hp, "a whole new life");
        assertEquals(0f, g.bossBarGold(), "the gold is gone");
        assertEquals(1f, g.bossBarFraction(), "the Moon's red bar, full");
        assertTrue(g.isUntouchable(), "out of reach while he rises");
        assertFalse(g.isEnraged(), "(red once he's up there)");
    }

    @Test
    void moonBeamsClearOldBulletsBeforeTheWarningAndPauseCraterFans() {
        Arena.now = new Arena(Arena.MEDIUM_COLS, Arena.MEDIUM_ROWS);
        GameWorld world = world();
        when(world.getDifficulty()).thenReturn(1f);
        when(world.getPlayer().centerX()).thenReturn(Arena.now.centerX);
        when(world.getPlayer().hitboxY()).thenReturn(Arena.now.bottom + 1f);
        when(world.rayLength(anyFloat(), anyFloat(), anyFloat(), anyFloat())).thenReturn(10f);
        Array<Bullet> bullets = new Array<>();
        doAnswer(inv -> {
            Bullet b = new Bullet(false, inv.getArgument(0), inv.getArgument(1), inv.getArgument(2),
                    inv.getArgument(3), 0.065f, 1f, Color.WHITE);
            bullets.add(b);
            return b;
        }).when(world).spawnEnemyBullet(anyFloat(), anyFloat(), anyFloat(), anyFloat());
        Grandfluff g = new Grandfluff(Arena.now.centerX, Arena.now.centerY, 0);
        g.spawnTimer = 0f;
        g.hit(g.maxHp, new Vector2(1f, 0f), 0f, world);
        g.behave(3.3f, world); // rise, then the Moon's first attack: crater rings
        g.behave(0.81f, world);
        for (int i = 0; i < 9; i++) g.behave(0.46f, world);
        assertFalse(bullets.isEmpty(), "the previous attack leaves bullets on the field");
        bullets.clear();
        clearInvocations(world);

        g.behave(0.81f, world); // begin the eye-beam warning
        verify(world).clearEnemyBullets(0f, 0f, -1f);
        g.behave(0.85f, world);
        assertTrue(bullets.isEmpty(), "no crater fans during the beam warning");
        for (int i = 0; i < 20; i++) g.behave(0.1f, world);
        assertFalse(bullets.isEmpty(), "the beams still shed their own sparks");
        for (Bullet b : bullets) {
            assertEquals(Color.valueOf("ff5a5a"), b.color, "only beam sparks during the sweep");
        }
        verify(world).clearEnemyBullets(0f, 0f, -1f); // once at entry, never each frame
    }
}
