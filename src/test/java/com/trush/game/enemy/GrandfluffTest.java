package com.trush.game.enemy;

import com.badlogic.gdx.math.Vector2;
import com.trush.game.world.GameWorld;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
}
