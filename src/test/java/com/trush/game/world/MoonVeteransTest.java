package com.trush.game.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Moon's veterans: only deeper in, a room sooner in hard mode. */
class MoonVeteransTest {

    @Test
    void deeperRoomsAndHardModeSendVeterans() {
        boolean deep = false;
        boolean hardSooner = false;
        for (int i = 0; i < 500; i++) {
            assertFalse(StoryDirector.moonVeteran(StoryDirector.MOON_VETERAN_DEPTH - 1, false), "not this near the landing");
            assertFalse(StoryDirector.moonVeteran(StoryDirector.MOON_VETERAN_DEPTH - 2, true));
            deep |= StoryDirector.moonVeteran(StoryDirector.MOON_VETERAN_DEPTH, false);
            hardSooner |= StoryDirector.moonVeteran(StoryDirector.MOON_VETERAN_DEPTH - 1, true);
        }
        assertTrue(deep);
        assertTrue(hardSooner);
    }
}
