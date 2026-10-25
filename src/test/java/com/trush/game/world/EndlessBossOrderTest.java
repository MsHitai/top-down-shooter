package com.trush.game.world;

import com.trush.game.enemy.Boss;
import com.trush.game.save.Unlocks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EndlessBossOrderTest {

    @AfterEach
    void forgetTheSea() {
        Unlocks.foundSea = false;
    }

    @Test
    void bossWavesClimbTheStages() {
        Unlocks.foundSea = true;
        for (int i = 0; i < 20; i++) { // random picks: try a few times
            assertEquals(Stage.VILLAGE, WaveDirector.bossForWave(5).stage);
            assertEquals(Stage.TOWN, WaveDirector.bossForWave(10).stage);
            assertEquals(Stage.CITY, WaveDirector.bossForWave(15).stage);
            assertEquals(Stage.SKYSCRAPER, WaveDirector.bossForWave(20).stage);
        }
        assertEquals(Boss.FLUFFY_KING, WaveDirector.bossForWave(25));
        assertEquals(Boss.SHARK, WaveDirector.bossForWave(30), "after the King, the sea waves end with the shark");
        assertTrue(WaveDirector.isSeaWave(26));
        assertTrue(WaveDirector.isSeaWave(30));
        assertFalse(WaveDirector.isSeaWave(31));
        assertEquals(Stage.SEA, Boss.SHARK.stage);
        assertEquals(Boss.SHARK, Boss.pickFor(Stage.SEA));
    }

    @Test
    void theSeaStaysASecretUntilItsFound() {
        Unlocks.foundSea = false;
        for (int w = 26; w <= 30; w++) {
            assertFalse(WaveDirector.isSeaWave(w), "wave " + w + " stays dry");
        }
        for (int i = 0; i < 200; i++) {
            assertNotEquals(Boss.SHARK, WaveDirector.bossForWave(30));
            assertNotEquals(Boss.SHARK, Boss.pickAny());
        }
    }
}
