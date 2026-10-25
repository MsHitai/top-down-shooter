package com.trush.game.objects;

import com.badlogic.gdx.utils.Array;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShopTest {

    @Test
    void pricesGoUpWithTheTier() {
        assertEquals(25, Shop.price(Shop.Ware.GUN, Tier.D, 1, false, false));
        assertEquals(45, Shop.price(Shop.Ware.GUN, Tier.C, 1, false, false));
        assertEquals(70, Shop.price(Shop.Ware.ITEM, Tier.B, 1, false, false));
        assertEquals(100, Shop.price(Shop.Ware.ITEM, Tier.A, 1, false, false));
        assertEquals(15, Shop.price(Shop.Ware.HEART, Tier.D, 1, false, false));
        assertEquals(15, Shop.price(Shop.Ware.BLANK, Tier.D, 1, false, false));
        assertEquals(20, Shop.price(Shop.Ware.AMMO, Tier.D, 1, false, false));
        assertEquals(30, Shop.price(Shop.Ware.KEY, Tier.A, 1, false, false), "consumables ignore the tier");
    }

    @Test
    void laterFloorsSalesAndGrudgesChangeThePrice() {
        assertEquals(54, Shop.price(Shop.Ware.GUN, Tier.C, 3, false, false), "+10% a floor");
        assertEquals(140, Shop.price(Shop.Ware.GUN, Tier.A, 5, false, false));
        assertEquals(34, Shop.price(Shop.Ware.GUN, Tier.C, 1, true, false), "a quarter off");
        assertEquals(68, Shop.price(Shop.Ware.GUN, Tier.C, 1, false, true), "half again when he's angry");
        assertEquals(51, Shop.price(Shop.Ware.GUN, Tier.C, 1, true, true));
        assertTrue(Shop.price(Shop.Ware.HEART, Tier.D, 0, true, false) >= 1);
    }

    @Test
    void stockTakesTwoWaresOutOfThePoolsAndAddsConsumables() {
        for (int run = 0; run < 50; run++) {
            Array<Weapon> guns = Weapon.chestGuns();
            Array<Item> items = new Array<>(Item.values());
            int before = guns.size + items.size;
            Shop shop = new Shop(2, false);
            shop.stock(guns, items, new Tier[]{Tier.C, Tier.A}, i -> false, false, false);

            assertEquals(5, shop.slots.size);
            assertEquals(before - 2, guns.size + items.size, "the wares can't come out of a chest too");
            Shop.Slot first = shop.slots.first();
            Shop.Slot last = shop.slots.peek();
            assertTrue(first.ware == Shop.Ware.GUN || first.ware == Shop.Ware.ITEM, "a ware on the left stand");
            assertTrue(last.ware == Shop.Ware.GUN || last.ware == Shop.Ware.ITEM, "and on the right one");
            assertEquals(Tier.C, first.tier);
            assertEquals(Tier.A, last.tier);
            assertEquals(Shop.Ware.HEART, shop.slots.get(1).ware);
            assertTrue(shop.slots.get(2).ware == Shop.Ware.BLANK || shop.slots.get(2).ware == Shop.Ware.AMMO);
            assertEquals(Shop.Ware.KEY, shop.slots.get(3).ware);
            int sales = 0;
            for (Shop.Slot s : shop.slots) {
                if (s.sale) sales++;
                assertEquals(Shop.price(s.ware, s.tier, 2, s.sale, false), s.price);
                assertNotNull(s.pickup);
            }
            assertEquals(1, sales, "one thing on sale");
        }
    }

    @Test
    void ownedItemsAreNotOfferedAndAnEmptyTierFallsBack() {
        Array<Weapon> guns = new Array<>();
        Array<Item> items = new Array<>();
        items.add(Item.HOT_SAUCE);
        items.add(Item.FLUFF_FIELD);
        Shop shop = new Shop(1, false);
        // tier A has nothing left, Hot Sauce (B) is already carried: the closest is the Fluff Field (C)
        shop.stock(guns, items, new Tier[]{Tier.A}, i -> i == Item.HOT_SAUCE, false, false);
        assertEquals(4, shop.slots.size);
        assertEquals(Item.FLUFF_FIELD, shop.slots.first().pickup.item);
        assertEquals("Fluff Field", shop.slots.first().title());
        assertFalse(items.contains(Item.FLUFF_FIELD, true));
    }

    @Test
    void theFirstFloorsShopAlwaysSellsALowTierGun() {
        for (int run = 0; run < 50; run++) {
            Shop shop = new Shop(1, false);
            shop.stock(Weapon.chestGuns(), new Array<>(Item.values()), new Tier[]{Tier.D, Tier.B}, i -> false, false, true);
            assertEquals(Shop.Ware.GUN, shop.slots.first().ware);
            assertEquals(Tier.D, shop.slots.first().tier);
        }
        Shop noGuns = new Shop(1, false); // every gun found already: an item instead
        noGuns.stock(new Array<>(), new Array<>(Item.values()), new Tier[]{Tier.D}, i -> false, false, true);
        assertEquals(Shop.Ware.ITEM, noGuns.slots.first().ware);
    }

    @Test
    void threeShotsAndBaalloHoldsAGrudge() {
        Shop shop = new Shop(1, false);
        assertFalse(shop.onShot(false));
        assertTrue(shop.isDucking(), "he ducks behind the counter");
        shop.update(1f);
        assertFalse(shop.isDucking());
        assertFalse(shop.onShot(false));
        assertTrue(shop.onShot(false), "the third hit");
        assertEquals("THAT'S IT! PRICES ARE UP!", shop.line());
        assertFalse(shop.onShot(true), "a grudge is only held once");
    }

    @Test
    void repricingAfterAGrudge() {
        Shop shop = new Shop(1, false);
        shop.stock(Weapon.chestGuns(), new Array<>(Item.values()), new Tier[]{Tier.D}, i -> false, false, false);
        int heart = shop.slots.get(1).price;
        shop.reprice(true);
        assertTrue(shop.slots.get(1).price > heart);
    }

    @Test
    void theFlippersHaveAFixedPriceAndAreNeverOnSale() {
        for (int run = 0; run < 30; run++) {
            Shop shop = new Shop(3, false);
            shop.stock(Weapon.chestGuns(), Item.shuffledPool(), new Tier[]{Tier.C, Tier.B}, i -> false, false, false);
            shop.addFlippers(false);

            Shop.Slot flippers = shop.slots.peek();
            assertTrue(flippers.special);
            assertEquals(Item.FLIPPERS, flippers.pickup.item);
            assertFalse(flippers.sale);
            assertEquals(Shop.FLIPPERS_PRICE, flippers.price, "no city markup");
            shop.reprice(true);
            assertEquals(Math.round(Shop.FLIPPERS_PRICE * Shop.GRUDGE_MARKUP), flippers.price, "but he still holds a grudge");
        }
    }

    @Test
    void theFlippersAreNeverInAChest() {
        assertFalse(Item.shuffledPool().contains(Item.FLIPPERS, true));
        assertEquals(Item.FLIPPERS, Item.byName("FLIPPERS"), "saved between floors by name");
    }
}
