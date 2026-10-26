package com.trush.game.world;

import com.trush.game.enemy.Boss;
import com.trush.game.save.RunSave;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Undercity: down the Slums' sewer hatch, the end of the secret detour. */
class UndercityTest {

    @Test
    void theSlumsHatchLeadsDownToTheUndercity() {
        assertEquals(Stage.UNDERCITY, Stage.SLUMS.below());
        assertNull(Stage.CITY.below(), "the story's own floors just go on to the next");
        assertNull(Stage.UNDERCITY.below());
        assertTrue(Stage.UNDERCITY.isSecret());
        assertTrue(Stage.UNDERCITY.isUnderground());
        for (int f = 1; f <= 10; f++) {
            assertNotEquals(Stage.UNDERCITY, Stage.forFloor(f), "never a story floor of its own");
        }
    }

    @Test
    void aSaveCarriesTheSecretFloorBelow() {
        RunSave s = new RunSave();
        s.floor = StoryDirector.UNDERCITY_FLOOR;
        s.stage = Stage.UNDERCITY.name();
        assertEquals(Stage.UNDERCITY, s.stage());
        assertEquals("CONTINUE - THE UNDERCITY", s.describe());
        s.stage = null; // an ordinary save
        assertEquals(Stage.CLOUDS, s.stage());
        assertTrue(s.describe().startsWith("CONTINUE - FLOOR 5"));
        s.stage = "SOMEWHERE_NEW"; // a stage this version doesn't know: the story's own floor
        assertEquals(Stage.CLOUDS, s.stage());
    }

    @Test
    void itIsTheLastFloorOfTheRun() {
        assertEquals(Stage.lastFloor(), StoryDirector.UNDERCITY_FLOOR, "beating its boss ends the run");
        StoryDirector d = StoryDirector.undercity(false);
        assertEquals(Stage.UNDERCITY, d.getStage());
        assertEquals("THE UNDERCITY", d.stageTitle());
        assertTrue(d.getMap().shop != null, "Baa-llo keeps a shop down here");
        assertEquals("store_undercity", d.getMap().shop.theme);
    }

    @Test
    void itsTunnelsAreNarrowAndItsBossRoomIsEnteredFromTheSide() {
        int narrow = 0;
        int fights = 0;
        for (int run = 0; run < 30; run++) {
            StageMap map = new StageMap(Stage.UNDERCITY.roomCount(), Stage.UNDERCITY);
            assertTrue(map.boss.onlySideDoors());
            for (Room r : map.rooms) {
                assertEquals(null, r.trap, "the trap rooms are the Slums'");
                if (r.type != Room.Type.COMBAT) continue;
                fights++;
                if (Math.min(r.arena.cols, r.arena.rows) <= 11 && Math.max(r.arena.cols, r.arena.rows) >= 20) narrow++;
            }
        }
        assertTrue(narrow > fights / 2, "mostly tunnels: " + narrow + " of " + fights);
    }

    @Test
    void untilTheSkunkKingComesTheSlumsBossesStandIn() {
        for (int i = 0; i < 100; i++) {
            assertEquals(Stage.SLUMS, Boss.pickFor(Stage.UNDERCITY).stage);
        }
    }
}
