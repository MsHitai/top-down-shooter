package com.trush.game.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeartContainerTest {

    @Test
    void aContainerAddsAFullHeartUpToTheCap() {
        Player p = new Player(null); // sprites are only needed for drawing
        assertEquals(6, p.getMaxHealth());
        p.restore(6, 3, 2, 0, new com.badlogic.gdx.utils.Array<>(), 0);
        assertTrue(p.addHeartContainer());
        assertEquals(8, p.getMaxHealth());
        assertEquals(5, p.getHealth(), "the new heart comes filled");
        while (p.getMaxHealth() < Player.MAX_HEALTH_CAP) {
            assertTrue(p.addHeartContainer());
        }
        assertEquals(32, p.getMaxHealth(), "16 hearts: 8 red and 8 golden on top");
        // at the cap a container still heals...
        p.restore(32, 10, 2, 0, new com.badlogic.gdx.utils.Array<>(), 0);
        assertTrue(p.addHeartContainer());
        assertEquals(32, p.getMaxHealth());
        assertEquals(12, p.getHealth());
        // ...but when full it stays on the floor
        p.restore(32, 32, 2, 0, new com.badlogic.gdx.utils.Array<>(), 0);
        assertFalse(p.addHeartContainer());
    }
}
