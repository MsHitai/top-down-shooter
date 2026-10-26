package com.trush.game.enemy;

import com.trush.game.save.Unlocks;
import com.trush.game.world.Stage;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void everyBossHasACardLineAndIsFoundByTitle() {
        for (Boss b : Boss.values()) {
            assertEquals(b, Boss.forTitle(b.title));
            assertFalse(b.quote().isEmpty());
        }
        assertNull(Boss.forTitle(null));
    }

    @Test
    void theCloudsAreTheFluffyKingsFloor() {
        for (int i = 0; i < 50; i++) {
            assertEquals(Boss.FLUFFY_KING, Boss.pickFor(Stage.CLOUDS));
        }
    }

    @Test
    void aStageWithoutBossesFallsBackToTheVillage() {
        for (Stage stage : Stage.values()) {
            if (stage == Stage.CLOUDS) continue; // the last floor: the final boss (see above)
            for (int i = 0; i < 200; i++) {
                Boss b = Boss.pickFor(stage);
                assertNotEquals(Boss.FLUFFY_KING, b);
                boolean own = false;
                for (Boss o : Boss.values()) own |= o.stage == stage;
                Stage borrowed = stage == Stage.SLUMS ? Stage.CITY : Stage.VILLAGE; // the Slums borrow the city's
                assertEquals(own ? stage : borrowed, b.stage);
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
        assertEquals(Boss.TRAFFIC_LIGHT, Boss.byName("traffic_light"));
        assertEquals(Boss.NIGHT_HAWK, Boss.byName("night hawk"));
        assertEquals(Boss.PIGEON_KING, Boss.byName("pigeon_king"));
        assertEquals(Boss.EVIL_TRAIN, Boss.byName("evil train"));
        assertNull(Boss.byName("dragon"));
    }

    @Test
    void everyBossHasMusicThatExists() {
        for (Boss b : Boss.values()) {
            String file = "src/main/resources/assets/music/" + b.music() + ".ogg";
            org.junit.jupiter.api.Assertions.assertTrue(new java.io.File(file).isFile(), b + " -> " + file);
        }
        assertEquals("boss", Boss.FLUFFY_KING.music());
        assertEquals("boss_village", Boss.WITCH.music());
        assertEquals("boss_town", Boss.TRAFFIC_LIGHT.music());
        assertEquals("boss_city", Boss.NIGHT_HAWK.music());
    }

    @Test
    void endlessCanRollEveryBossButTheSlumsOnceTheSeaIsFound() {
        Set<Boss> seen = EnumSet.noneOf(Boss.class);
        Unlocks.foundSea = false;
        for (int i = 0; i < 2000; i++) {
            seen.add(Boss.pickAny());
        }
        // (the Slums' bosses never come to endless: they're secret)
        Set<Boss> slums = EnumSet.noneOf(Boss.class);
        for (Boss b : Boss.values()) {
            if (b.stage == Stage.SLUMS) slums.add(b);
        }
        Set<Boss> notYet = EnumSet.copyOf(slums);
        notYet.add(Boss.SHARK);
        assertEquals(EnumSet.complementOf(EnumSet.copyOf(notYet)), seen, "the shark stays a secret");
        Unlocks.foundSea = true;
        for (int i = 0; i < 2000; i++) {
            seen.add(Boss.pickAny());
        }
        Unlocks.foundSea = false;
        assertEquals(EnumSet.complementOf(EnumSet.copyOf(slums)), seen);
    }
}
