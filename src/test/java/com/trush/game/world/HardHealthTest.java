package com.trush.game.world;

import com.trush.game.enemy.BulletKin;
import com.trush.game.enemy.RiotCop;
import com.trush.game.enemy.Turtle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HardHealthTest {

    @Test
    void hardModeEnemiesGetTougherFloorByFloorUpToFifteenPercent() {
        assertEquals(1.10f, GameWorld.hardHealth(1), 1e-4f);
        assertEquals(1.15f, GameWorld.hardHealth(2), 1e-4f);
        assertEquals(1.15f, GameWorld.hardHealth(3), 1e-4f);
        assertEquals(1.15f, GameWorld.hardHealth(StoryDirector.SLUMS_FLOOR), 1e-4f);
        assertEquals(1.15f, GameWorld.hardHealth(StoryDirector.UNDERCITY_FLOOR), 1e-4f);
        assertEquals(1.15f, GameWorld.hardHealth(7), 1e-4f, "never more than +15%");
    }

    @Test
    void enemiesWithArmourOfTheirOwnSkipHardModesHealth() {
        assertTrue(new RiotCop(0, 0).armoured());
        assertTrue(new Turtle(0, 0).armoured());
        assertFalse(new BulletKin(0, 0).armoured());
    }

    @Test
    void hardModeEnemiesDropAmmoMoreOften() {
        assertEquals(0.04f, GameWorld.AMMO_DROP_CHANCE, 1e-4f);
        assertEquals(0.06f, GameWorld.HARD_AMMO_DROP_CHANCE, 1e-4f);
    }
}
