package com.trush.game.world;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RushShopTest {

    @Test
    void baalloComesAfterEverySecondBoss() {
        assertFalse(StoryDirector.rushShop(1, Stage.VILLAGE));
        assertTrue(StoryDirector.rushShop(2, Stage.TOWN));
        assertFalse(StoryDirector.rushShop(3, Stage.CITY));
        assertTrue(StoryDirector.rushShop(4, Stage.CLOUDS));
        assertTrue(StoryDirector.rushShop(10, Stage.MOON));
    }

    @Test
    void neverUnderTheSea() {
        assertFalse(StoryDirector.rushShop(2, Stage.SEA));
        assertFalse(StoryDirector.rushShop(6, Stage.SEA));
    }
}
