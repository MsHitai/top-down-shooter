package com.trush.game.world;

import com.trush.game.enemy.Boss;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EndlessBossOrderTest {

    @Test
    void bossWavesClimbTheStages() {
        for (int i = 0; i < 20; i++) { // random picks: try a few times
            assertEquals(Stage.VILLAGE, WaveDirector.bossForWave(5).stage);
            assertEquals(Stage.TOWN, WaveDirector.bossForWave(10).stage);
            assertEquals(Stage.CITY, WaveDirector.bossForWave(15).stage);
            assertEquals(Stage.SKYSCRAPER, WaveDirector.bossForWave(20).stage);
        }
        assertEquals(Boss.FLUFFY_KING, WaveDirector.bossForWave(25));
    }
}
