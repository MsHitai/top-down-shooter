package com.trush.game.world;

import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Item;
import com.trush.game.objects.Prop;
import com.trush.game.objects.Shop;
import com.trush.game.objects.Weapon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The city's secret way down to the Slums: the burning drum, the boards, the water guns, the trip down. */
class SlumsWayTest {

    @Test
    void everyCityMapHidesOneWayDownInARoomWithNoTopDoor() {
        for (int run = 0; run < 50; run++) {
            StageMap map = new StageMap(Stage.CITY.roomCount(), Stage.CITY);
            int ways = 0;
            for (Room r : map.rooms) {
                if (!r.slumsWay) continue;
                ways++;
                assertEquals(Room.Type.COMBAT, r.type);
                assertFalse(r.doors[Room.N], "the boards stand where a top door would be");
            }
            assertEquals(1, ways);
        }
        for (Stage other : new Stage[]{Stage.VILLAGE, Stage.TOWN, Stage.CLOUDS, Stage.SLUMS}) {
            for (Room r : new StageMap(9, other).rooms) {
                assertFalse(r.slumsWay, other + " has no way down to the Slums");
            }
        }
    }

    @Test
    void theDrumBurnsInFrontOfTheBoardsAndWaterPutsItOut() {
        Room r = null;
        StageMap map = new StageMap(Stage.CITY.roomCount(), Stage.CITY);
        for (Room room : map.rooms) {
            if (room.slumsWay) r = room;
        }
        assertNotNull(r);
        r.build();
        assertTrue(r.props.contains(r.boards, true));
        assertTrue(r.props.contains(r.fireDrum, true));
        assertTrue(r.fireDrum.burning);
        assertFalse(r.boards.isDestructible(), "no way through while the fire burns");
        assertFalse(r.fireDrum.isDestructible());
        assertTrue(r.boards.solid.y + r.boards.solid.height <= r.arena.top + 0.001f, "against the top wall");
        for (int i = 1; i < Prop.SPLASHES_TO_DOUSE; i++) {
            assertFalse(r.fireDrum.splash());
        }
        assertTrue(r.fireDrum.splash(), "out it goes");
        assertFalse(r.fireDrum.burning);
        assertFalse(r.fireDrum.splash(), "only once");
    }

    @Test
    void theWaterGunsAreWet() {
        assertTrue(Weapon.waterPistol().isWet());
        assertTrue(Weapon.bubbleBlaster().isWet());
        assertTrue(Weapon.snowballSlinger().isWet());
        assertFalse(Weapon.flareGun().isWet());
    }

    @Test
    void theShopCanBeToldWhatToSellFirst() {
        Shop shop = new Shop(3, false);
        Weapon water = Weapon.waterPistol();
        shop.stock(new Array<>(), new Array<Item>(), new com.trush.game.objects.Tier[]{com.trush.game.objects.Tier.B},
                i -> false, false, false, water);
        boolean found = false;
        for (Shop.Slot s : shop.slots) {
            found |= s.ware == Shop.Ware.GUN && s.pickup.weapon == water;
        }
        assertTrue(found, "the water gun is on a stand");
    }

    @Test
    void goingDownLeavesTheCityForTheSlumsSharingItsChestPools() {
        StoryDirector city = new StoryDirector(3, new Array<>(), new Array<>());
        StoryDirector slums = city.intoSlums();
        assertEquals(Stage.SLUMS, slums.getStage());
        assertEquals(StoryDirector.SLUMS_FLOOR, slums.getFloor());
        assertSame(city.getGuns(), slums.getGuns(), "nothing handed out twice");
        assertSame(city.getItems(), slums.getItems());
        assertEquals("THE SLUMS", slums.stageTitle());
    }
}
