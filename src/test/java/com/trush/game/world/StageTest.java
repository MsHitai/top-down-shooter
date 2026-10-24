package com.trush.game.world;

import com.trush.game.objects.Prop;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageTest {

    @Test
    void theSkyscraperIsAStraightClimbToTheRoof() {
        for (int run = 0; run < 20; run++) {
            StageMap map = new StageMap(9, Stage.SKYSCRAPER);
            Room r = map.start;
            int treasures = 0;
            while (r != map.boss) {
                Room up = map.neighbor(r, Room.N);
                assertTrue(r.doors[Room.N] && up != null, "every floor leads up");
                assertFalse(r.doors[Room.E] || r.doors[Room.W], "no side rooms");
                int climb = up.floorNumber - r.floorNumber;
                assertTrue(climb >= 5 && climb <= 10, "5-10 storeys per ride, was " + climb);
                if (up.type == Room.Type.TREASURE) treasures++;
                r = up;
            }
            assertEquals(1, treasures);
            assertEquals(Room.Type.BOSS, map.boss.type);
            assertEquals("roof", map.boss.theme);
        }
    }

    @Test
    void floorsMapToStagesAndStayOnTheLastOne() {
        assertEquals(Stage.VILLAGE, Stage.forFloor(1));
        assertEquals(Stage.TOWN, Stage.forFloor(2));
        assertEquals(Stage.CITY, Stage.forFloor(3));
        assertEquals(Stage.SKYSCRAPER, Stage.forFloor(4));
        assertEquals(Stage.SKYSCRAPER, Stage.forFloor(7));
        assertEquals(4, Stage.lastFloor());
    }

    @Test
    void theTownSwapsFurniture() {
        assertEquals(Prop.Kind.HAY, Stage.VILLAGE.propFor('H'));
        assertEquals(Prop.Kind.BENCH, Stage.TOWN.propFor('H'));
        assertEquals(Prop.Kind.PILLAR, Stage.VILLAGE.propFor('P'));
        assertEquals(Prop.Kind.LAMP, Stage.TOWN.propFor('P'));
        assertEquals(Prop.Kind.TABLE, Stage.TOWN.propFor('T'));
        assertEquals(Prop.Kind.CAR, Stage.CITY.propFor('H'));
        assertEquals(Prop.Kind.LAMP, Stage.CITY.propFor('P'));
        assertEquals(Prop.Kind.DESK, Stage.SKYSCRAPER.propFor('H'));
        assertEquals(Prop.Kind.PLANT, Stage.SKYSCRAPER.propFor('P'));
        assertEquals(Prop.Kind.COOLER, Stage.SKYSCRAPER.propFor('B'));
        assertTrue(Stage.CITY.night);
        assertFalse(Stage.TOWN.night);
    }
}
