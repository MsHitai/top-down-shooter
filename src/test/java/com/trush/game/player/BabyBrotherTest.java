package com.trush.game.player;

import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Item;
import com.trush.game.objects.Shop;
import com.trush.game.objects.Tier;
import com.trush.game.objects.Weapon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The baby brother, the easy mode. */
class BabyBrotherTest {

    @Test
    void heStartsWithFourHeartsHisRattleAndAGuardianLamb() {
        Player baby = new Player(null, Hero.BABY);
        assertEquals(8, baby.getMaxHealth(), "four hearts");
        assertEquals("Baby Rattle", baby.currentWeapon().getName());
        assertTrue(baby.hasItem(Item.GUARDIAN_LAMB));
        assertTrue(Hero.BABY.unlocked(), "playable from the start");
        assertTrue(Hero.BABY.rollReach > Hero.BOY.rollReach, "a speedy roll");
    }

    @Test
    void hisRattleIsWeakerThanTheSidearmButForgiving() {
        Weapon rattle = Weapon.babyRattle();
        Weapon sidearm = Weapon.rustySidearm();
        assertTrue(rattle.getDamage() / rattle.getFireInterval() < sidearm.getDamage() / sidearm.getFireInterval(),
                "anything found is a step up");
        assertTrue(rattle.getMagSize() > sidearm.getMagSize(), "a big magazine");
        assertEquals("Baby Rattle", Weapon.byName("Baby Rattle").getName(), "saved runs load it");
        for (Weapon w : Weapon.chestGuns()) {
            assertFalse(w.getName().equals("Baby Rattle"), "never in chests");
        }
    }

    @Test
    void theFluffulWaitsOnAStandOfItsOwnAtAFixedPrice() {
        Shop shop = new Shop(1, false);
        shop.stock(new Array<>(), new Array<>(), new Tier[0], i -> false, false, false);
        shop.addFlufful(Weapon.flufful(), false);
        Shop.Slot s = shop.slots.peek();
        assertTrue(s.special);
        assertEquals(Shop.FLUFFUL_PRICE, s.price);
    }
}
