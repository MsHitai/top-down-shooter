package com.trush.game.objects;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeirloomAndSTierTest {

    @Test
    void theHeirloomGrowsFromWeakToAKillingMachine() {
        Weapon h = Weapon.heirloom();
        assertEquals(Tier.A, h.getTier());
        assertTrue(h.isInfinite());
        float weak = h.getDamage() * h.getPellets() / h.getFireInterval();
        assertFalse(h.addKill(Weapon.HEIRLOOM_KILLS[0] - 1));
        assertTrue(h.addKill(1), "level 2 at the first threshold");
        assertEquals(2, h.getLevel());
        assertTrue(h.addKill(500), "lots of kills at once climb several levels");
        assertEquals(5, h.getLevel());
        assertFalse(h.addKill(500), "and it stops at level 5");
        float strong = h.getDamage() * h.getPellets() / h.getFireInterval();
        assertTrue(strong > weak * 4, "a killing machine at the end");
        assertTrue(h.isPiercing());
        assertEquals("heirloom_5", h.getSprite());
    }

    @Test
    void aSavedHeirloomKeepsItsLevelAndUpgrades() {
        Weapon h = Weapon.heirloom();
        h.apply(Weapon.Upgrade.DAMAGE); // grandad's +1
        h.restoreKills(Weapon.HEIRLOOM_KILLS[2]);
        assertEquals(4, h.getLevel());
        Weapon plain = Weapon.heirloom();
        plain.restoreKills(Weapon.HEIRLOOM_KILLS[2]);
        assertEquals(plain.getDamage() * 1.15f, h.getDamage(), 1e-3f, "the +1 stays on top of the level");
    }

    @Test
    void sGunsAreNeverSoldAndComeInBlackChests() {
        int s = 0;
        for (Weapon w : Weapon.chestGuns()) {
            if (w.getTier() == Tier.S) s++;
        }
        assertEquals(2, s);
        assertNotNull(Weapon.byName("Unicorn Horn"));
        Array<Weapon> guns = new Array<>();
        guns.add(Weapon.unicornHorn());
        Shop shop = new Shop(1, false);
        shop.stock(guns, new Array<>(), new Tier[]{Tier.A, Tier.A}, i -> false, false, false);
        assertEquals(1, guns.size, "Baa-llo doesn't sell S guns");
    }
}
