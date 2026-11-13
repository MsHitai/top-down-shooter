package com.trush.game.world;

import com.trush.game.objects.Grenade;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrenadeWallTest {

    @Test
    void aGrenadeThrownAlongTheTopWallKeepsFlying() {
        Arena arena = new Arena(Arena.SMALL_COLS, Arena.SMALL_ROWS);
        Grenade g = new Grenade(arena.centerX, arena.top + 0.14f, 0f, 2f);
        arena.clamp(g.pos, 0.1f);
        float x = g.pos.x;
        GameWorld.flyGrenade(g, 0.1f, arena);
        assertTrue(g.pos.x > x + 0.3f);
        assertEquals(arena.top - 0.1f, g.pos.y, 1e-4f);
    }

    @Test
    void aGrenadeThrownIntoTheTopWallStopsAtTheSamePlaceAsItsMarker() {
        Arena arena = new Arena(Arena.SMALL_COLS, Arena.SMALL_ROWS);
        Grenade g = new Grenade(arena.centerX, arena.top - 0.1f, 90f, 4.5f);
        GameWorld.flyGrenade(g, Grenade.FLIGHT, arena);
        assertEquals(arena.top - 0.1f, g.pos.y, 1e-4f);
        assertEquals(arena.centerX, g.pos.x, 1e-4f);
    }
}
