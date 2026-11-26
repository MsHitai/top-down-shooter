package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Chest;
import com.trush.game.objects.Tier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockMakers;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

class StoryDareTest {
    @Test
    void secretDetoursKeepTheSameRule() {
        StoryDirector story = new StoryDirector(3, Stage.CITY, false, Dare.GLASS_CANNON);
        for (StoryDirector leg : new StoryDirector[]{story, story.intoSlums(), story.diveIntoSea()}) {
            assertTrue(leg.rule(Dream.Rule.GLASS_CANNON));
            assertFalse(leg.rule(Dream.Rule.SMALL_BLANKS));
            assertEquals(Dare.GLASS_CANNON, leg.getDare());
        }
    }

    @Test
    void treasureGainsOneTierAtMostAndRemainsLocked() {
        for (Dare dare : Dare.values()) {
            assertEquals(Tier.C, treasure(dare, 0).tier);
            assertEquals(Tier.A, treasure(dare, 80).tier);
            assertEquals(Tier.A, treasure(dare, 99).tier);
        }
        assertEquals(Tier.D, treasure(null, 0).tier);
    }

    private Chest treasure(Dare dare, int roll) {
        StoryDirector director = new StoryDirector(1, Stage.VILLAGE, new Array<>(), new Array<>(), dare);
        Room room = director.getMap().rooms.select(r -> r.type == Room.Type.TREASURE).iterator().next();
        GameWorld world = mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS)
                .defaultAnswer(RETURNS_DEEP_STUBS));
        Arena previousArena = Arena.now;
        Random previousRandom = MathUtils.random;
        try {
            Arena.now = room.arena;
            MathUtils.random = new Random() {
                @Override
                public int nextInt(int bound) {
                    return bound == 100 ? roll : super.nextInt(bound);
                }
            };
            director.onRoomEntered(room, world);
        } finally {
            Arena.now = previousArena;
            MathUtils.random = previousRandom;
        }
        ArgumentCaptor<Chest> placed = ArgumentCaptor.forClass(Chest.class);
        verify(world).placeChest(placed.capture());
        Chest chest = placed.getValue();
        assertTrue(chest.locked);
        assertEquals(chest.tier, chest.gun != null ? chest.gun.getTier() : chest.item.tier);
        return chest;
    }
}
