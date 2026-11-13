package com.trush.game.world;

import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class ReinforcementsTest {

    @Test
    void secondGroupsComeMoreOftenHigherUp() {
        for (int floor = 1; floor < 7; floor++) {
            assertTrue(StoryDirector.reinforceChance(floor + 1) >= StoryDirector.reinforceChance(floor));
        }
        assertTrue(StoryDirector.reinforceChance(1) > 0f, "even the village has one now and then");
        assertTrue(StoryDirector.reinforceChance(20) <= StoryDirector.REINFORCE_MAX, "never every room");
    }

    @Test
    void everyDoorwayStartsTheFightBeforeThePlayerCanSlipAlongTheWall() {
        Arena a = new Arena(Arena.MEDIUM_COLS, Arena.MEDIUM_ROWS);
        assertTrue(a.contains(a.centerX, a.bottom + 0.19f, StoryDirector.FIGHT_ENTRY_MARGIN));
        assertTrue(a.contains(a.centerX, a.top - 0.38f, StoryDirector.FIGHT_ENTRY_MARGIN));
        assertTrue(a.contains(a.left + 0.3f, a.centerY, StoryDirector.FIGHT_ENTRY_MARGIN));
        assertTrue(a.contains(a.right - 0.3f, a.centerY, StoryDirector.FIGHT_ENTRY_MARGIN));
    }

    @Test
    void enteringAnUnclearedCombatRoomLocksItAtTheDoorway() {
        StoryDirector director = new StoryDirector(1, false);
        Room combat = null;
        for (Room r : director.getMap().rooms) {
            if (r.type == Room.Type.COMBAT) {
                combat = r;
                break;
            }
        }
        Arena.now = combat.arena;
        GameWorld world = mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
        when(world.playerInsideRoom(StoryDirector.FIGHT_ENTRY_MARGIN)).thenReturn(true);
        director.onRoomEntered(combat, world);
        director.update(1f / 60f, world);
        assertTrue(combat.isLocked());
    }
}
