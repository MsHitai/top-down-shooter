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
    void everyFloorHasOneShopOnADeadEnd() {
        for (Stage stage : Stage.values()) {
            if (stage.isSea()) continue; // (Baa-llo can't swim: see below)
            for (int run = 0; run < 30; run++) {
                StageMap map = new StageMap(9, stage);
                int shops = 0;
                for (Room r : map.rooms) {
                    if (r.type == Room.Type.SHOP) shops++;
                }
                assertEquals(1, shops, stage + " has a shop");
                Room shop = map.shop;
                assertEquals(stage.shopTheme, shop.theme);
                assertTrue(shop.depth > 1, "not right next to the start");
                if (stage.isTower()) {
                    assertTrue(shop.doors[Room.N] && shop.doors[Room.S], "a gift shop on one of the floors on the way up");
                    continue;
                }
                int doors = 0;
                for (boolean d : shop.doors) {
                    if (d) doors++;
                }
                assertEquals(1, doors, "a dead end");
                assertFalse(shop.doors[Room.N], "its counter stands against the top wall");
                for (int d = 0; d < 4; d++) {
                    Room n = map.neighbor(shop, d);
                    assertTrue(n == null || n.type != Room.Type.BOSS && n.type != Room.Type.START);
                }
            }
        }
    }

    @Test
    void theCloudsHaveSkyAllRoundAndHolesToFallThrough() {
        assertTrue(Stage.CLOUDS.isSky());
        assertFalse(Stage.SKYSCRAPER.isSky());
        assertEquals("pit_sky", Stage.CLOUDS.pitSprite());
        assertEquals("pit", Stage.TOWN.pitSprite());
        StageMap map = new StageMap(9, Stage.CLOUDS);
        assertEquals("cloudgate", map.start.theme);
        assertTrue(map.start.openSky());
    }

    @Test
    void floorsMapToStagesAndStayOnTheLastOne() {
        assertEquals(Stage.VILLAGE, Stage.forFloor(1));
        assertEquals(Stage.TOWN, Stage.forFloor(2));
        assertEquals(Stage.CITY, Stage.forFloor(3));
        assertEquals(Stage.SKYSCRAPER, Stage.forFloor(4));
        assertEquals(Stage.CLOUDS, Stage.forFloor(5));
        assertEquals(Stage.CLOUDS, Stage.forFloor(7));
        assertEquals(5, Stage.lastFloor());
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
        assertEquals(Prop.Kind.CLOUD, Stage.CLOUDS.propFor('H'));
        assertEquals(Prop.Kind.COLUMN, Stage.CLOUDS.propFor('P'));
        assertEquals(Prop.Kind.URN, Stage.CLOUDS.propFor('B'));
        assertEquals(Prop.Kind.GIFT, Stage.CLOUDS.propFor('C'));
        assertEquals(Prop.Kind.CLOUD, Stage.CLOUDS.propFor('T'));
        assertTrue(Stage.CITY.night);
        assertFalse(Stage.TOWN.night);
    }

    @Test
    void theSeaIsASecretFloorWithoutAShop() {
        assertTrue(Stage.SEA.isSea());
        assertEquals(Stage.CLOUDS, Stage.forFloor(6), "never a story floor of its own");
        for (int run = 0; run < 30; run++) {
            StageMap map = new StageMap(7, Stage.SEA);
            assertEquals(null, map.shop);
            int treasures = 0;
            for (Room r : map.rooms) {
                assertTrue(r.type != Room.Type.SHOP);
                assertFalse(r.seaHole, "no hole down from the sea itself");
                if (r.type == Room.Type.TREASURE) treasures++;
            }
            assertEquals(1, treasures, "a sunken ship with a chest");
            assertEquals("seabed", map.start.theme);
            assertEquals("lair", map.boss.theme);
            assertEquals(Stage.Exit.SPOUT, Stage.SEA.exit);
        }
        assertEquals(Prop.Kind.CORAL, Stage.SEA.propFor('H'));
        assertEquals(Prop.Kind.ROCK, Stage.SEA.propFor('P'));
        assertEquals(Prop.Kind.BARREL, Stage.SEA.propFor('B'), "sunken cargo");
    }

    @Test
    void everyCloudFloorHidesOneHoleToTheSea() {
        for (int run = 0; run < 50; run++) {
            StageMap map = new StageMap(9, Stage.CLOUDS);
            int holes = 0;
            Room hole = null;
            for (Room r : map.rooms) {
                if (r.seaHole) {
                    holes++;
                    hole = r;
                }
            }
            assertEquals(1, holes);
            assertEquals(Room.Type.COMBAT, hole.type);
            hole.build();
            assertTrue(hole.seaPit != null && hole.pits.contains(hole.seaPit, true), "it's a pit you can fall into");
        }
        for (Stage stage : new Stage[]{Stage.VILLAGE, Stage.CITY, Stage.SKYSCRAPER}) {
            for (Room r : new StageMap(9, stage).rooms) {
                assertFalse(r.seaHole, stage + " has no way down to the sea");
            }
        }
    }
}
