package com.trush.game.enemy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeaCreaturesTest {

    @Test
    void picksFitTheBudgetAndKeepDiversFew() {
        for (int i = 0; i < 500; i++) {
            int kind = SeaCreatures.pick(1, 0);
            assertEquals(SeaCreatures.STARFISH, kind, "one point left buys a starfish");
            assertTrue(SeaCreatures.pick(5, 2) != SeaCreatures.DIVER, "two divers at most");
            int any = SeaCreatures.pick(5, 0);
            assertTrue(SeaCreatures.cost(any) <= 5);
        }
    }

    @Test
    void everyKindIsASeaCreature() {
        assertTrue(SeaCreatures.create(SeaCreatures.FLUFFERFISH, 0, 0) instanceof Flufferfish);
        assertTrue(SeaCreatures.create(SeaCreatures.STARFISH, 0, 0) instanceof Starfish);
        assertTrue(SeaCreatures.create(SeaCreatures.DIVER, 0, 0) instanceof Diver);
        assertTrue(SeaCreatures.create(SeaCreatures.TURTLE, 0, 0) instanceof Turtle);
        assertTrue(SeaCreatures.create(SeaCreatures.SWORDFISH, 0, 0) instanceof Swordfish);
        Enemy shark = Boss.SHARK.create(0, 0, 0);
        assertTrue(shark instanceof FluffyShark && shark.boss);
        assertEquals("THE FLUFFY SHARK", shark.bossName());
    }
}
