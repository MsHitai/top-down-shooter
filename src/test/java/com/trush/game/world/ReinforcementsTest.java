package com.trush.game.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinforcementsTest {

    @Test
    void secondGroupsComeMoreOftenHigherUpAndInHardMode() {
        for (int floor = 1; floor < 7; floor++) {
            assertTrue(StoryDirector.reinforceChance(floor + 1, false) >= StoryDirector.reinforceChance(floor, false));
            assertTrue(StoryDirector.reinforceChance(floor, true) > StoryDirector.reinforceChance(floor, false));
        }
        assertTrue(StoryDirector.reinforceChance(1, false) > 0f, "even the village has one now and then");
        assertTrue(StoryDirector.reinforceChance(9, true) <= StoryDirector.REINFORCE_MAX, "never every room");
    }
}
