package com.trush.game.world;

import com.trush.game.enemy.Boss;
import com.trush.game.objects.Prop;
import com.trush.game.save.RunSave;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Moon: up the family's rocket after the Fluffy King - Grandfluff's home, the true ending. */
class MoonTest {

    @Test
    void itIsASecretFloorPastTheClouds() {
        assertTrue(Stage.MOON.isSecret());
        assertTrue(Stage.MOON.isMoon());
        assertEquals(Stage.lastFloor() + 1, StoryDirector.MOON_FLOOR, "one floor past the clouds: the hardest yet");
        for (int f = 1; f <= 10; f++) {
            assertNotEquals(Stage.MOON, Stage.forFloor(f), "only the rocket goes there");
        }
        assertNull(Stage.CLOUDS.below(), "the rainbow still slides home");
        StoryDirector d = StoryDirector.moon(false);
        assertEquals("THE MOON", d.stageTitle());
        assertFalse(d.boardedRocket());
        assertNull(d.nextStage(), "the Moon is the end of the road");
    }

    @Test
    void noShopUpThereBaalloCantFly() {
        for (int run = 0; run < 20; run++) {
            StageMap map = new StageMap(Stage.MOON.roomCount(), Stage.MOON);
            assertNull(map.shop);
            for (Room r : map.rooms) {
                assertTrue(r.openSky(), "the starry sky shows round every room");
            }
        }
    }

    @Test
    void moonFurniture() {
        assertEquals(Prop.Kind.BOULDER, Stage.MOON.propFor('H'));
        assertEquals(Prop.Kind.BOULDER, Stage.MOON.propFor('T'));
        assertEquals(Prop.Kind.CRYSTAL, Stage.MOON.propFor('P'));
        assertEquals(Prop.Kind.CHEESE, Stage.MOON.propFor('B'));
        assertEquals(Prop.Kind.SUPPLIES, Stage.MOON.propFor('C'));
        assertEquals("pit_moon", Stage.MOON.pitSprite());
        assertEquals(Stage.Exit.ROCKET, Stage.MOON.exit, "the family's rocket flies you home");
    }

    @Test
    void theFluffyKingStandsInUntilGrandfluffIsReady() {
        assertEquals(Boss.FLUFFY_KING, Boss.pickFor(Stage.MOON));
    }

    @Test
    void aSaveRemembersTheTripUp() {
        RunSave s = new RunSave();
        s.floor = StoryDirector.MOON_FLOOR;
        s.stage = Stage.MOON.name();
        assertEquals(Stage.MOON, s.stage());
        assertEquals("CONTINUE - THE MOON", s.describe());
    }
}
