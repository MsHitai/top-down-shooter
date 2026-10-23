package com.trush.game.objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EggLobberTest {

    @Test
    void holdingTheTriggerThrowsFurther() {
        Weapon egg = Weapon.eggLobber();
        assertTrue(egg.chargesShots());
        assertFalse(Weapon.pocketUzi().chargesShots());
        assertFalse(egg.isCharging());
        egg.holdCharge(0.01f);
        assertTrue(egg.isCharging());
        float tap = egg.lobDistance();
        egg.holdCharge(Weapon.FULL_CHARGE / 2f);
        assertTrue(egg.lobDistance() > tap);
        egg.holdCharge(10f);
        assertEquals(Weapon.LOB_MAX, egg.lobDistance(), 1e-4f, "tops out");
        egg.cancelCharge();
        assertFalse(egg.isCharging());
        assertEquals(Weapon.LOB_MIN, egg.lobDistance(), 1e-4f);
    }
}
