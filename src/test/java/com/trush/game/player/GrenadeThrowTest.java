package com.trush.game.player;

import com.trush.game.objects.Item;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
