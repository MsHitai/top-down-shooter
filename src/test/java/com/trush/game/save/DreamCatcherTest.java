package com.trush.game.save;

import com.trush.game.objects.Item;
import com.trush.game.objects.Weapon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DreamCatcherTest {

    @Test
    void theDreamCatcherIsOnlyInStoryRuns() {
        assertTrue(Item.shuffledPool(true).contains(Item.DREAM_CATCHER, true));
        assertFalse(Item.shuffledPool(false).contains(Item.DREAM_CATCHER, true), "endless has no floor 1 to wake up on");
    }

    @Test
    void wakingUpFillsEveryGunAgain() {
        Weapon w = Weapon.chestGuns().first();
        int fullMag = w.getMag();
        int fullAmmo = w.getAmmo();
        w.restoreAmmo(0, 0);
        RunSave.GunData g = RunSave.GunData.of(w);
        assertEquals(0, g.toWeapon().getAmmo(), "an ordinary floor carries the empty gun over");
        g.full = true;
        Weapon back = g.toWeapon();
        assertEquals(fullMag, back.getMag());
        assertEquals(fullAmmo, back.getAmmo(), "the Dream Catcher's fresh start: loaded again");
    }
}
