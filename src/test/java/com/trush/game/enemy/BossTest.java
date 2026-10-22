package com.trush.game.enemy;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BossTest {

    @Test
    void villageNeverRollsTheFinalBoss() {
        Set<Boss> seen = EnumSet.noneOf(Boss.class);
        for (int i = 0; i < 2000; i++) {
            Boss b = Boss.pick(false);
            assertNotEquals(Boss.FLUFFY_KING, b);
            seen.add(b);
        }
        assertEquals(EnumSet.of(Boss.HAYMAKER, Boss.HARVESTMAN, Boss.WITCH), seen);
    }

    @Test
    void endlessCanRollEveryBoss() {
        Set<Boss> seen = EnumSet.noneOf(Boss.class);
        for (int i = 0; i < 2000; i++) {
            seen.add(Boss.pick(true));
        }
        assertEquals(EnumSet.allOf(Boss.class), seen);
    }
}
