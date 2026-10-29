package com.trush.game.save;

import com.trush.game.objects.Item;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DreamCatcherTest {

    @Test
    void theDreamCatcherIsOnlyInStoryRuns() {
        assertTrue(Item.shuffledPool(true).contains(Item.DREAM_CATCHER, true));
        assertFalse(Item.shuffledPool(false).contains(Item.DREAM_CATCHER, true), "endless has no floor 1 to wake up on");
    }
}
