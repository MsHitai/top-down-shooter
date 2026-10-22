package com.trush.game.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.objects.Prop;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomPitsTest {

    private static Room room(Stage stage, Room.Type type) {
        Room r = new Room(0, 0, type);
        r.stage = stage;
        r.theme = stage.combatThemes[0];
        r.template = type == Room.Type.COMBAT ? RoomTemplates.randomCombat() : RoomTemplates.START;
        for (int d = 0; d < 4; d++) r.doors[d] = true;
        r.build();
        return r;
    }

    @Test
    void onlySomeTownCombatRoomsHavePitsInSafePlaces() {
        int withPits = 0;
        for (int i = 0; i < 400; i++) {
            Room r = room(Stage.TOWN, Room.Type.COMBAT);
            if (r.pits.size > 0) withPits++;
            assertTrue(r.pits.size <= 2);
            for (Rectangle pit : r.pits) {
                assertTrue(Vector2.dst(pit.x + 0.5f, pit.y + 0.5f, Arena.CENTER_X, Arena.CENTER_Y) >= 1.3f, "not in the middle");
                assertFalse(Room.blocksADoor(pit), "not in front of a door");
                assertTrue(Arena.contains(pit.x + 0.5f, pit.y + 0.5f, 0.4f), "inside the room");
                for (Prop p : r.props) {
                    assertFalse(p.solid.overlaps(pit), "not under furniture");
                }
            }
        }
        assertTrue(withPits > 100 && withPits < 300, "roughly 45% of rooms, was " + withPits);
    }

    @Test
    void noPitsInTheVillageOrSpecialRooms() {
        for (int i = 0; i < 100; i++) {
            assertEquals(0, room(Stage.VILLAGE, Room.Type.COMBAT).pits.size);
            assertEquals(0, room(Stage.TOWN, Room.Type.START).pits.size);
        }
    }
}
