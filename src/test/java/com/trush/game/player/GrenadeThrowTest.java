package com.trush.game.player;

import com.trush.game.gfx.Sprites;
import com.trush.game.objects.Item;
import com.trush.game.world.Arena;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class GrenadeThrowTest {

    @Test
    void holdingThrowsFurtherUpToTheMax() {
        Player p = new Player(null);
        p.takeActive(Item.GRENADE);
        assertFalse(p.isChargingGrenade());
        assertEquals(Player.GRENADE_MIN, p.grenadeDistance(), 1e-4f);
        p.chargeGrenade(Player.GRENADE_FULL_CHARGE / 2f);
        assertTrue(p.isChargingGrenade());
        assertTrue(p.grenadeDistance() > Player.GRENADE_MIN && p.grenadeDistance() < Player.GRENADE_MAX);
        p.chargeGrenade(10f);
        assertEquals(Player.GRENADE_MAX, p.grenadeDistance(), 1e-4f);
        p.takeActive(Item.MED_KIT); // swapping items drops the charge
        assertFalse(p.isChargingGrenade());
    }

    @Test
    void aShotAtTheTopWallStartsOnTheFloorEvenThoughTheGunOverlapsTheWall() {
        Sprites sprites = mock(Sprites.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
        when(sprites.gun(anyString()).length()).thenReturn(0.5f);
        Player p = new Player(sprites);
        Arena.now = new Arena(Arena.SMALL_COLS, Arena.SMALL_ROWS);
        p.setFeet(Arena.now.centerX, Arena.now.top - Player.FEET_H - 0.001f);
        assertTrue(p.muzzleY() > Arena.now.top);
        assertEquals(Arena.now.top - 0.05f, p.shotY(), 1e-4f);
        assertTrue(Arena.now.contains(p.shotX(), p.shotY(), 0.04f));
    }
}
