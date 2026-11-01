package com.trush.game.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaperPlanesTest {

    @Test
    void aPlaneTurnsTheShortWayRound() {
        assertEquals(20f, GameWorld.angleDiff(30f, 10f), 1e-4f);
        assertEquals(-20f, GameWorld.angleDiff(350f, 10f), 1e-4f, "across 0 degrees");
        assertEquals(20f, GameWorld.angleDiff(10f, 350f), 1e-4f);
        assertEquals(-90f, GameWorld.angleDiff(0f, 90f), 1e-4f);
    }
}
