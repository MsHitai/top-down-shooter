package com.trush.game.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HardModeTest {

    @Test
    void hardModeEnemiesDropAmmoMoreOften() {
        assertEquals(0.04f, GameWorld.AMMO_DROP_CHANCE, 1e-4f);
        assertEquals(0.06f, GameWorld.HARD_AMMO_DROP_CHANCE, 1e-4f);
    }
}
