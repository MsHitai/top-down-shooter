package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class EnemyHitFeedbackTest {
    @Test
    void bossHitsKeepColourDetailAndAlphaWithoutChangingThePalette() {
        TestEnemy enemy = new TestEnemy(true);
        Color base = new Color(0.2f, 0.4f, 0.6f, 0.5f);
        enemy.hit(1f, Vector2.Zero, 0f, null);
        Color hit = enemy.c(base);
        assertEquals(0.4f, hit.r, 0.0001f);
        assertEquals(0.55f, hit.g, 0.0001f);
        assertEquals(0.7f, hit.b, 0.0001f);
        assertEquals(base.a, hit.a);
        assertEquals(new Color(0.2f, 0.4f, 0.6f, 0.5f), base);
    }

    @Test
    void continuousHitsKeepDoingDamageButLeaveAGapBetweenBossPulses() {
        Arena previous = Arena.now;
        Arena.now = new Arena(Arena.BOSS_COLS, Arena.BOSS_ROWS);
        try {
            GameWorld world = mock(GameWorld.class,
                    withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
            when(world.getEnemyBullets()).thenReturn(new Array<>());
            TestEnemy enemy = new TestEnemy(true);
            enemy.hit(1f, Vector2.Zero, 0f, world);
            enemy.update(0.05f, world);
            enemy.hit(1f, Vector2.Zero, 0f, world);
            enemy.update(0.04f, world);
            enemy.hit(1f, Vector2.Zero, 0f, world);
            assertTrue(enemy.flash <= 0f);
            assertSame(Color.RED, enemy.c(Color.RED));
            enemy.update(0.08f, world);
            enemy.hit(1f, Vector2.Zero, 0f, world);
            assertTrue(enemy.flash > 0f);
            assertEquals(96f, enemy.hp);
        } finally {
            Arena.now = previous;
        }
    }

    @Test
    void regularEnemiesStillFlashWhiteOnEveryHit() {
        TestEnemy enemy = new TestEnemy(false);
        enemy.hit(1f, Vector2.Zero, 0f, null);
        assertSame(Color.WHITE, enemy.c(Color.RED));
        enemy.flash = -0.01f;
        enemy.hit(1f, Vector2.Zero, 0f, null);
        assertSame(Color.WHITE, enemy.c(Color.RED));
    }

    private static class TestEnemy extends Enemy {
        TestEnemy(boolean boss) {
            super(3f, 3f, 100f);
            this.boss = boss;
            hp = maxHp = 100f;
            spawnTimer = 0f;
        }

        @Override public float feetY() { return pos.y; }
        @Override protected void behave(float delta, GameWorld world) { }
        @Override protected void drawBody(SpriteBatch batch, Draw draw) { }
    }
}
