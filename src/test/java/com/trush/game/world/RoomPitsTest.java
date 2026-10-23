package com.trush.game.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.objects.Prop;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomPitsTest {

    private static Room room(Stage stage, Room.Type type, int cols, int rows) {
        Room r = new Room(0, 0, type);
        r.stage = stage;
        r.arena = new Arena(cols, rows);
        r.theme = stage.combatThemes[0];
        r.template = type == Room.Type.COMBAT ? RoomTemplates.randomCombat() : RoomTemplates.START;
        for (int d = 0; d < 4; d++) r.doors[d] = true;
        r.build();
        return r;
    }

    private static void assertSafe(Room r) {
        for (Rectangle pit : r.pits) {
            assertTrue(Vector2.dst(pit.x + 0.5f, pit.y + 0.5f, r.arena.centerX, r.arena.centerY) >= 0.8f, "not in the middle");
            assertFalse(r.blocksADoor(pit), "not in front of a door");
            assertTrue(r.arena.contains(pit.x + 0.5f, pit.y + 0.5f, 0.4f), "inside the room");
            for (Prop p : r.props) {
                assertFalse(p.solid.overlaps(pit), "not under furniture");
            }
        }
    }

    @Test
    void someRoomyTownRoomsHaveAFewPits() {
        int withPits = 0;
        for (int i = 0; i < 400; i++) {
            Room r = room(Stage.TOWN, Room.Type.COMBAT, Arena.MEDIUM_COLS, Arena.MEDIUM_ROWS);
            if (r.pits.size > 0) withPits++;
            assertTrue(r.pits.size <= 3);
            assertSafe(r);
        }
        assertTrue(withPits > 100 && withPits < 300, "roughly 45% of rooms, was " + withPits);
    }

    @Test
    void smallTownRoomsAreTrapRoomsFullOfPits() {
        int pits = 0;
        for (int i = 0; i < 200; i++) {
            Room r = room(Stage.TOWN, Room.Type.COMBAT, Arena.SMALL_COLS, Arena.SMALL_ROWS);
            assertTrue(r.pits.size <= 4, "was " + r.pits.size); // a very busy layout can leave no room for one
            pits += r.pits.size;
            assertSafe(r);
        }
        assertTrue(pits / 200f > 2f, "usually 3-4 pits, average " + pits / 200f);
    }

    @Test
    void noPitsInTheVillageOrSpecialRooms() {
        for (int i = 0; i < 100; i++) {
            assertEquals(0, room(Stage.VILLAGE, Room.Type.COMBAT, Arena.SMALL_COLS, Arena.SMALL_ROWS).pits.size);
            assertEquals(0, room(Stage.VILLAGE, Room.Type.COMBAT, Arena.MEDIUM_COLS, Arena.MEDIUM_ROWS).pits.size);
            assertEquals(0, room(Stage.TOWN, Room.Type.START, Arena.SMALL_COLS, Arena.SMALL_ROWS).pits.size);
        }
    }

    @Test
    void layoutsSitInTheMiddleOfBiggerRooms() {
        Room small = room(Stage.VILLAGE, Room.Type.START, Arena.SMALL_COLS, Arena.SMALL_ROWS);
        Room big = room(Stage.VILLAGE, Room.Type.START, Arena.MEDIUM_COLS, Arena.MEDIUM_ROWS);
        // (the big room can keep a piece the small one drops for blocking a doorway)
        assertTrue(big.props.size >= small.props.size);
        float dx = big.arena.centerX - small.arena.centerX;
        float dy = big.arena.centerY - small.arena.centerY;
        for (Prop sp : small.props) {
            boolean found = false;
            for (Prop bp : big.props) {
                found |= bp.kind == sp.kind && Math.abs(bp.cx - (sp.cx + dx)) < 0.26f && Math.abs(bp.cy - (sp.cy + dy)) < 0.26f;
            }
            assertTrue(found, sp.kind + " moved to the middle");
        }
    }

    @Test
    void bossArenasAreOpenOrHaveCover() {
        assertEquals(Arena.BOSS_ROWS, RoomTemplates.BOSS_OPEN.length);
        assertEquals(Arena.BOSS_COLS, RoomTemplates.BOSS_OPEN[0].length());
        assertFalse(String.join("", RoomTemplates.BOSS_OPEN).contains("P"));
        assertEquals(8, String.join("", RoomTemplates.BOSS_COVER).chars().filter(c -> c == 'P').count());
    }
}
