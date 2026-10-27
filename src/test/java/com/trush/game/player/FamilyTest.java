package com.trush.game.player;

import com.trush.game.objects.Weapon;
import com.trush.game.save.Unlocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FamilyTest {

    @Test
    void theSisterHasTwoHeartsAndThrowingStars() {
        Player p = new Player(null, Hero.SISTER);
        assertEquals(4, p.getMaxHealth());
        assertEquals("Throwing Stars", p.currentWeapon().getName());
        assertTrue(p.currentWeapon().isInfinite());
        assertNotNull(Weapon.byName("Throwing Stars"), "a saved run can load her stars");
    }

    @Test
    void everyGunGrandadGetsIsPlusOne() {
        Player p = new Player(null, Hero.GRANDAD);
        assertEquals("Old Double-Barrel +1", p.currentWeapon().displayName());
        p.addWeapon(Weapon.pocketUzi());
        assertEquals("Pocket Uzi +1", p.currentWeapon().displayName());
        p.addWeapon(Weapon.pocketUzi()); // finding it again still upgrades it once more
        assertEquals("Pocket Uzi +2", p.currentWeapon().displayName());
    }

    @Test
    void grannyAndGrandadHaveTheirOwnStarters() {
        Weapon revolver = new Player(null, Hero.GRANNY).currentWeapon();
        assertEquals("Pearl Revolver", revolver.getName());
        assertTrue(revolver.isInfinite());
        Weapon shotgun = new Player(null, Hero.GRANDAD).currentWeapon();
        assertEquals("Old Double-Barrel", shotgun.getName());
        assertTrue(shotgun.isInfinite());
        Weapon plain = Weapon.doubleBarrel();
        assertTrue(plain.getDamage() * plain.getPellets()
                < Weapon.fluffShotgun().getDamage() * Weapon.fluffShotgun().getPellets(),
                "a starter, not a C gun - it's grandad's +1 that brings it up to the Fluff Shotgun");
        assertNotNull(Weapon.byName("Pearl Revolver"), "a saved run can load them");
        assertNotNull(Weapon.byName("Old Double-Barrel"));
        for (Weapon w : Weapon.chestGuns()) {
            assertFalse(w.getName().equals("Pearl Revolver") || w.getName().equals("Old Double-Barrel"),
                    "starters stay out of chests");
        }
    }

    @Test
    void grandadWaitsForTheKingToBeBeaten() {
        boolean was = Unlocks.grandad;
        try {
            Unlocks.grandad = false;
            assertFalse(Hero.GRANDAD.unlocked());
            assertTrue(Hero.GRANNY.unlocked());
            Unlocks.grandad = true;
            assertTrue(Hero.GRANDAD.unlocked());
        } finally {
            Unlocks.grandad = was;
        }
    }

    @Test
    void grannyWalksSlowerAndTheBoyIsUnchanged() {
        assertTrue(new Player(null, Hero.GRANNY).moveSpeed() < new Player(null, Hero.BOY).moveSpeed());
        Player boy = new Player(null);
        assertEquals(6, boy.getMaxHealth());
        assertEquals("Rusty Sidearm", boy.currentWeapon().displayName());
    }
}
