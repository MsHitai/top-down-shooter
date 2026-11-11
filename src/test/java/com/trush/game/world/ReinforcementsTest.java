package com.trush.game.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReinforcementsTest {

    @Test
    void secondGroupsComeMoreOftenHigherUp() {
        for (int floor = 1; floor < 7; floor++) {
            assertTrue(StoryDirector.reinforceChance(floor + 1) >= StoryDirector.reinforceChance(floor));
        }
        assertTrue(StoryDirector.reinforceChance(1) > 0f, "even the village has one now and then");
        assertTrue(StoryDirector.reinforceChance(20) <= StoryDirector.REINFORCE_MAX, "never every room");
    }
}
