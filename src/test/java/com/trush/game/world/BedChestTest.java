package com.trush.game.world;

import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Chest;
import com.trush.game.objects.Item;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockMakers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

class BedChestTest {
    @Test void treasureRoomsSplitTheirRewardsBetweenAGunAndAnItem() {
        Arena previous = Arena.now;
        try {
            StoryDirector director = new StoryDirector(Dream.start(Dream.Kind.UNDER_BED), new Array<>(), new Array<Item>());
            GameWorld world = mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
            for (Room room : director.getMap().rooms) {
                if (room.type == Room.Type.TREASURE) {
                    Arena.now = room.arena;
                    director.onRoomEntered(room, world);
                }
            }

            ArgumentCaptor<Chest> placed = ArgumentCaptor.forClass(Chest.class);
            verify(world, times(2)).placeChest(placed.capture());
            List<Chest> chests = placed.getAllValues();
            assertTrue(chests.stream().allMatch(chest -> chest.locked));
            assertEquals(1, chests.stream().filter(chest -> chest.gun != null && chest.item == null).count());
            assertEquals(1, chests.stream().filter(chest -> chest.item != null && chest.gun == null).count());
        } finally {
            Arena.now = previous;
        }
    }
}
