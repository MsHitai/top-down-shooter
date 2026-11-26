package com.trush.game.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.objects.Prop;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShapedRoomTest {

    @Test
    void footprintMatchesWallCollisionAndKeepsTheRegularFloorArea() {
        for (Arena.Shape shape : Arena.Shape.values()) {
            Room room = new Room(0, 0, Room.Type.COMBAT);
            Arena arena = room.arena = new Arena(shape);
            room.rebuildWalls();

            int tiles = 0;
            for (int row = 0; row < arena.rows; row++) {
                for (int col = 0; col < arena.cols; col++) {
                    float x = arena.left + (col + 0.5f) * Arena.CELL;
                    float y = arena.bottom + (row + 0.5f) * Arena.CELL;
                    boolean floor = arena.hasFloor(col, row);
                    assertEquals(floor, arena.contains(x, y, 0.2f), shape.toString());
                    assertEquals(!floor, hitsWall(room, x, y), shape.toString());
                    if (floor) tiles++;
                    Vector2 position = new Vector2(x, y);
                    arena.clamp(position, 0.22f);
                    assertTrue(arena.contains(position.x, position.y, 0.219f), shape.toString());
                }
            }
            assertFalse(arena.isSmall());
            assertEquals(Arena.MEDIUM_COLS * Arena.MEDIUM_ROWS, tiles);
        }
    }

    @Test
    void everyShapeLetsThePlayerReachAllFourDoorsAndSealsThemDuringCombat() {
        for (Arena.Shape shape : Arena.Shape.values()) {
            Room room = new Room(0, 0, Room.Type.COMBAT);
            Arena a = room.arena = new Arena(shape);
            for (int d = 0; d < room.doors.length; d++) room.doors[d] = true;
            room.rebuildWalls();
            assertDoorLandings(room, a);

            room.setLocked(true);
            assertTrue(hitsWall(room, a.centerX, a.top + 0.15f));
            assertTrue(hitsWall(room, a.centerX, a.bottom - 0.1f));
            assertTrue(hitsWall(room, a.left - 0.1f, a.centerY));
            assertTrue(hitsWall(room, a.right + 0.1f, a.centerY));

            room.setLocked(false);
            assertDoorLandings(room, a);
        }
    }

    @Test
    void generatedShapesKeepFurnitureAndPitsOnTheFloor() {
        for (Stage stage : new Stage[]{Stage.VILLAGE, Stage.TOWN, Stage.CITY, Stage.CLOUDS}) {
            int shaped = 0;
            for (int i = 0; i < 20; i++) {
                StageMap map = new StageMap(10, stage);
                for (Room room : map.rooms) {
                    if (!room.arena.isShaped()) continue;
                    shaped++;
                    assertEquals(Room.Type.COMBAT, room.type);
                    assertFalse(room.slumsWay || room.seaHole || room.arena.isSmall());
                    room.build();
                    for (Prop prop : room.props) assertTrue(room.arena.fits(prop.solid));
                    for (Rectangle pit : room.pits) assertTrue(room.arena.fits(pit));
                }
            }
            assertTrue(shaped > 0, stage.toString());
        }
    }

    private static boolean hitsWall(Room room, float x, float y) {
        for (Rectangle wall : room.walls) {
            if (wall.contains(x, y)) return true;
        }
        return false;
    }

    private static void assertDoorLandings(Room room, Arena a) {
        assertFalse(hitsWall(room, a.centerX, a.top + 0.15f));
        assertFalse(hitsWall(room, a.centerX, a.bottom - 0.1f));
        assertFalse(hitsWall(room, a.left - 0.1f, a.centerY));
        assertFalse(hitsWall(room, a.right + 0.1f, a.centerY));
        assertTrue(a.contains(a.centerX, a.top - 0.45f, 0.2f));
        assertTrue(a.contains(a.centerX, a.bottom + 0.2f, 0.16f));
        assertTrue(a.contains(a.left + 0.3f, a.centerY - 0.15f, 0.2f));
        assertTrue(a.contains(a.right - 0.3f, a.centerY - 0.15f, 0.2f));
    }
}
