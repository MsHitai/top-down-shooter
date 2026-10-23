package com.trush.game.player;

import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Item;
import com.trush.game.objects.Weapon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemsTest {

    @Test
    void biggerClipsGrowEveryMagazineIncludingLaterGuns() {
        Player p = new Player(null);
        int before = p.getWeapons().first().getMagSize();
        p.addPassive(Item.BIGGER_CLIPS);
        assertEquals(Math.round(before * 1.5f), p.getWeapons().first().getMagSize());
        Weapon uzi = Weapon.pocketUzi();
        int uziBase = uzi.getMagSize();
        p.addWeapon(uzi);
        assertEquals(Math.round(uziBase * 1.5f), uzi.getMagSize());
    }

    @Test
    void bubbleShieldBlocksOneHitPerRoom() {
        Player p = new Player(null);
        assertFalse(p.consumeShield());
        p.addPassive(Item.BUBBLE_SHIELD);
        assertTrue(p.consumeShield());
        assertFalse(p.consumeShield());
        p.refreshShield();
        assertTrue(p.isShieldUp());
    }

    @Test
    void onlyOneActiveItemAtATime() {
        Player p = new Player(null);
        assertNull(p.takeActive(Item.MED_KIT));
        assertSame(Item.MED_KIT, p.takeActive(Item.GRENADE));
        assertTrue(p.hasItem(Item.GRENADE));
        assertFalse(p.hasItem(Item.MED_KIT));
    }

    @Test
    void sparePocketGivesABlankRightAway() {
        Player p = new Player(null);
        p.restore(6, 6, 2, 0, new Array<>(), 0);
        p.addPassive(Item.SPARE_POCKET);
        assertEquals(3, p.getBlanks());
    }

    @Test
    void namesRoundTrip() {
        for (Item i : Item.values()) {
            assertSame(i, Item.byName(i.name()));
        }
        assertNull(Item.byName("NOPE"));
    }
}
