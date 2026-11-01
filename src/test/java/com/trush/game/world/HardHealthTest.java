package com.trush.game.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HardHealthTest {

    @Test
    void hardModeEnemiesGetTougherFloorByFloorUpToThirtyPercent() {
        assertEquals(1.10f, GameWorld.hardHealth(1), 1e-4f);
        assertEquals(1.15f, GameWorld.hardHealth(2), 1e-4f);
        assertEquals(1.20f, GameWorld.hardHealth(3), 1e-4f);
        assertEquals(1.25f, GameWorld.hardHealth(StoryDirector.SLUMS_FLOOR), 1e-4f);
        assertEquals(1.30f, GameWorld.hardHealth(StoryDirector.UNDERCITY_FLOOR), 1e-4f);
        assertEquals(1.30f, GameWorld.hardHealth(7), 1e-4f, "never more than +30%");
    }
}
