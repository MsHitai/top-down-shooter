package com.trush.game.enemy;

import com.trush.game.world.Stage;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BossTest {

    @Test
    void villageNeverRollsTheFinalBoss() {
        Set<Boss> seen = EnumSet.noneOf(Boss.class);
        for (int i = 0; i < 2000; i++) {
            Boss b = Boss.pickFor(Stage.VILLAGE);
            assertNotEquals(Boss.FLUFFY_KING, b);
            seen.add(b);
        }
        assertEquals(EnumSet.of(Boss.HAYMAKER, Boss.HARVESTMAN, Boss.WITCH), seen);
    }

    @Test
    void aStageWithoutBossesFallsBackToTheVillage() {
        for (Stage stage : Stage.values()) {
            for (int i = 0; i < 200; i++) {
                Boss b = Boss.pickFor(stage);
                assertNotEquals(Boss.FLUFFY_KING, b);
                boolean own = false;
                for (Boss o : Boss.values()) own |= o.stage == stage;
                assertEquals(own ? stage : Stage.VILLAGE, b.stage);
            }
        }
    }

    @Test
    void bossNamesAreForgiving() {
        assertEquals(Boss.ROAD_RAGER, Boss.byName("road_rager"));
        assertEquals(Boss.ROAD_RAGER, Boss.byName("road_racer"));
        assertEquals(Boss.ROAD_RAGER, Boss.byName("The Road Rager"));
        assertEquals(Boss.TRASH_CAN, Boss.byName("trash_can"));
        assertEquals(Boss.TRASH_CAN, Boss.byName("trash that can"));
        assertEquals(Boss.WITCH, Boss.byName("WITCH"));
        assertEquals(Boss.FLUFFY_KING, Boss.byName("fluffy king"));
        assertNull(Boss.byName("dragon"));
    }

    @Test
    void endlessCanRollEveryBoss() {
        Set<Boss> seen = EnumSet.noneOf(Boss.class);
        for (int i = 0; i < 2000; i++) {
            seen.add(Boss.pickAny());
        }
        assertEquals(EnumSet.allOf(Boss.class), seen);
    }
}
