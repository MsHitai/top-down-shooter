package com.trush.game.enemy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Moon's folk: bunnies, living asteroids, astronauts and aliens. */
class MoonFolkTest {

    @Test
    void bigAsteroidsOnlyWhenThereIsRoomForThem() {
        for (int i = 0; i < 500; i++) {
            assertNotEquals(MoonFolk.ASTEROID, MoonFolk.pick(2, 0), "an asteroid costs three");
            assertNotEquals(MoonFolk.ASTEROID, MoonFolk.pick(10, 2), "two at a time at most");
            int kind = MoonFolk.pick(10, 0);
            assertTrue(kind >= MoonFolk.BUNNIES && kind <= MoonFolk.ALIEN);
        }
        assertEquals(3, MoonFolk.cost(MoonFolk.ASTEROID));
        assertEquals(2, MoonFolk.cost(MoonFolk.BUNNIES));
    }

    @Test
    void whatTheyAre() {
        assertTrue(new LivingAsteroid(0, 0).flying, "asteroids float over furniture and craters");
        assertFalse(new MoonBunny(0, 0).flying);
        Alien alien = new Alien(0, 0);
        assertFalse(alien.isDodging(), "only while beamed out");
        assertTrue(new LivingAsteroid(0, 0).maxHp > new MoonBunny(0, 0).maxHp);
    }

    @Test
    void veteransAreTheirOwnToughKind() {
        assertEquals(MoonFolk.VETERAN + MoonFolk.ALIEN, MoonFolk.veteran(MoonFolk.ALIEN));
        assertEquals(MoonFolk.veteran(MoonFolk.ALIEN), MoonFolk.veteran(MoonFolk.veteran(MoonFolk.ALIEN)));
        assertTrue(MoonFolk.isVeteran(MoonFolk.veteran(MoonFolk.BUNNIES)));
        assertFalse(MoonFolk.isVeteran(MoonFolk.ASTEROID));
        assertEquals(3, MoonFolk.cost(MoonFolk.veteran(MoonFolk.ASTEROID)), "same cost as the plain kind");
        assertTrue(MoonBunny.veteran(0, 0).maxHp > new MoonBunny(0, 0).maxHp);
        assertTrue(LivingAsteroid.veteran(0, 0).maxHp > new LivingAsteroid(0, 0).maxHp);
        assertTrue(Astronaut.veteran(0, 0).maxHp > new Astronaut(0, 0).maxHp);
        assertTrue(Alien.veteran(0, 0).maxHp > new Alien(0, 0).maxHp);
        assertEquals("VeteranAlien", Alien.veteran(0, 0).dexId());
        assertEquals("Alien", new Alien(0, 0).dexId());
    }
}
