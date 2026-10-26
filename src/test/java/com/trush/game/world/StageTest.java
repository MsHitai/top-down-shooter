package com.trush.game.world;

import com.badlogic.gdx.math.Vector3;
import com.trush.game.objects.Prop;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
    void theSlumsAreMostlyNarrowAlleysWithOldAcid() {
        int alleys = 0;
        int fights = 0;
        for (int run = 0; run < 20; run++) {
            StageMap map = new StageMap(11, Stage.SLUMS);
            for (Room r : map.rooms) {
                if (r.type != Room.Type.COMBAT) continue;
                fights++;
                Arena a = r.arena;
                if (Math.min(a.cols, a.rows) <= 11 && Math.max(a.cols, a.rows) >= 20) {
                    alleys++;
                    assertFalse(a.isSmall(), "an alley is no trap room");
                }
                r.build();
                assertTrue(a.isSmall() || r.acidPools.size >= 1, "acid everywhere but the pit-filled trap rooms: " + a.cols + "x" + a.rows);
                for (Vector3 pool : r.acidPools) {
                    assertTrue(a.contains(pool.x, pool.y, pool.z * 0.75f), "the pool lies on the floor");
                    for (Prop p : r.props) {
                        assertFalse(p.solid.contains(pool.x, pool.y), "not under the furniture");
                    }
                }
            }
        }
        assertTrue(alleys > fights / 2, "mostly alleys: " + alleys + " of " + fights);
    }

    @Test
    void anAlleyLayoutFitsItsRoomAndKeepsALaneClear() {
        for (int run = 0; run < 50; run++) {
            for (int[] size : new int[][]{{29, 8}, {11, 20}}) {
                String[] g = RoomTemplates.alley(size[0], size[1]);
                assertEquals(size[1], g.length);
                boolean across = size[0] > size[1];
                for (String line : g) {
                    assertEquals(size[0], line.length());
                }
                for (int i = 0; i < (across ? size[0] : size[1]); i++) { // the middle lane is open but for tables
                    char c = across ? g[size[1] / 2].charAt(i) : g[i].charAt(size[0] / 2);
                    assertTrue(c == '.' || c == 'T', "lane blocked by " + c);
                }
            }
        }
    }

    @Test
    void theSlumsAreASecretFloor() {
        assertTrue(Stage.SLUMS.isSecret());
        assertTrue(Stage.SEA.isSecret());
        assertFalse(Stage.CITY.isSecret());
        for (int f = 1; f <= 10; f++) {
            assertNotEquals(Stage.SLUMS, Stage.forFloor(f));
        }
        assertEquals("THE SLUMS", StoryDirector.slums(false).stageTitle());
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
            for (Room r : map.rooms) {
                r.build();
                if (r.type == Room.Type.COMBAT) {
                    assertTrue(r.mines.size >= 1, "sea combat rooms have mines");
                } else {
                    assertEquals(0, r.mines.size, "no mines in the " + r.type + " room");
                }
            }
        }
        assertEquals(Prop.Kind.CORAL, Stage.SEA.propFor('H'));
        assertEquals(Prop.Kind.ROCK, Stage.SEA.propFor('P'));
        assertEquals(Prop.Kind.SEAWEED, Stage.SEA.propFor('B'));
        assertEquals(Prop.Kind.CLAM, Stage.SEA.propFor('C'));
        for (char c : "THPBCK".toCharArray()) {
            Prop.Kind k = Stage.SEA.propFor(c);
            assertTrue(new Prop(k, 0, 0).isSeaFurniture(), "only sea things under the sea, not " + k);
        }
        for (Prop.Kind k : Prop.Kind.values()) { // the flooded endless arena: everything has a sea stand-in
            Prop.Kind sea = Prop.seaKindFor(k);
            assertTrue(new Prop(sea, 0, 0).isSeaFurniture());
            assertEquals(k.wide(), sea.wide(), k + " keeps its width under water");
        }
    }

    @Test
    void everyCloudFloorHidesOneHoleToTheSea() {
        for (int run = 0; run < 3000; run++) { // (many runs: it used to go missing in the odd crowded room)
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
