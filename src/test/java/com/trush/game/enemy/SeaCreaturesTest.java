package com.trush.game.enemy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeaCreaturesTest {

    @Test
    void picksFitTheBudgetAndKeepDiversFew() {
        for (int i = 0; i < 500; i++) {
            int kind = SeaCreatures.pick(1, 0, 0);
            assertEquals(SeaCreatures.STARFISH, kind, "one point left buys a starfish");
            assertTrue(SeaCreatures.pick(5, 2, 0) != SeaCreatures.DIVER, "two divers at most");
            int any = SeaCreatures.pick(5, 0, 0);
            assertTrue(SeaCreatures.cost(any) <= 5);
            assertFalse(SeaCreatures.isBig(SeaCreatures.pick(4, 0, 0)), "a big one needs room in the budget");
            assertFalse(SeaCreatures.isBig(SeaCreatures.pick(12, 0, 1)), "one big one a room");
        }
    }

    @Test
    void everyKindIsASeaCreature() {
        assertTrue(SeaCreatures.create(SeaCreatures.FLUFFERFISH, 0, 0) instanceof Flufferfish);
        assertTrue(SeaCreatures.create(SeaCreatures.STARFISH, 0, 0) instanceof Starfish);
        assertTrue(SeaCreatures.create(SeaCreatures.DIVER, 0, 0) instanceof Diver);
        assertTrue(SeaCreatures.create(SeaCreatures.TURTLE, 0, 0) instanceof Turtle);
        assertTrue(SeaCreatures.create(SeaCreatures.SWORDFISH, 0, 0) instanceof Swordfish);
        assertTrue(SeaCreatures.create(SeaCreatures.GIANT_CRAB, 0, 0) instanceof GiantCrab);
        assertTrue(SeaCreatures.create(SeaCreatures.GIANT_OCTOPUS, 0, 0) instanceof GiantOctopus);
        assertTrue(new GiantCrab(0, 0).maxHp > new Turtle(0, 0).maxHp * 2, "the big ones are big");
        Enemy shark = Boss.SHARK.create(0, 0, 0);
        assertTrue(shark instanceof FluffyShark && shark.boss);
        assertEquals("THE FLUFFY SHARK", shark.bossName());
    }
}
