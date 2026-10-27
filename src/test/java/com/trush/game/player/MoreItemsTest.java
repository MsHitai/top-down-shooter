package com.trush.game.player;

import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Item;
import com.trush.game.objects.Tier;
import com.trush.game.objects.Weapon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The 0.10 items: about ten a tier, two in S, and the ones that work on the Player itself. */
class MoreItemsTest {

    @Test
    void aboutTenItemsATierAndTwoInS() {
        int[] count = new int[Tier.values().length];
        for (Item i : Item.shuffledPool(true)) count[i.tier.ordinal()]++;
        for (Tier t : new Tier[]{Tier.D, Tier.C, Tier.B, Tier.A}) {
            assertEquals(10, count[t.ordinal()], "tier " + t);
        }
        assertEquals(2, count[Tier.S.ordinal()]);
    }

    @Test
    void packedLunchIsEatenToSaveAFatalHit() {
        Player p = new Player(null);
        p.restore(6, 1, 2, 0, new Array<>(), 0);
        p.takeActive(Item.PACKED_LUNCH);
        p.addPassive(Item.GUARDIAN_LAMB);
        assertTrue(p.hurt(null));
        assertFalse(p.isDead());
        assertEquals(2, p.getHealth(), "back up on one heart");
        assertNull(p.getActive(), "the lunch is gone");
        assertTrue(p.hasItem(Item.GUARDIAN_LAMB), "the lamb is kept for later");
        assertTrue(p.takeLunchSave());
        assertFalse(p.takeLunchSave());
    }

    @Test
    void sweetToothHealsEveryTwentyKillsAndWaitsAtFullHealth() {
        Player p = new Player(null);
        p.restore(6, 6, 2, 0, new Array<>(), 0);
        p.addPassive(Item.SWEET_TOOTH);
        for (int i = 0; i < Player.SWEET_KILLS; i++) {
            assertFalse(p.sweetToothKill(), "full health: nothing to heal yet");
        }
        p.hurt(null);
        assertTrue(p.sweetToothKill(), "the treat was kept for when it's needed");
        assertEquals(6, p.getHealth());
        assertFalse(p.sweetToothKill());
    }

    @Test
    void goldStarRoundsGrowWithCoins() {
        Player p = new Player(null);
        p.addPassive(Item.GOLD_STAR_ROUNDS);
        assertEquals(1f, p.damageScale(), 1e-4f);
        p.setCoins(60);
        assertEquals(1.2f, p.damageScale(), 1e-4f);
        p.setCoins(900);
        assertEquals(1.4f, p.damageScale(), 1e-4f, "capped");
    }

    @Test
    void familyPhotoCountsEveryGunBeyondTheFirst() {
        Player p = new Player(null);
        p.addPassive(Item.FAMILY_PHOTO);
        float one = p.damageScale();
        p.addWeapon(Weapon.byName("Big Magnum"));
        p.addWeapon(Weapon.byName("Pocket Uzi"));
        assertEquals(one * 1.12f, p.damageScale(), 1e-4f);
    }

    @Test
    void sugarRushIsFasterAndStrongerForAWhile() {
        Player p = new Player(null);
        float speed = p.moveSpeed();
        p.startSugarRush();
        assertTrue(p.isSugarRushing());
        assertEquals(1.5f, p.damageScale(), 1e-4f);
        assertTrue(p.moveSpeed() > speed);
    }

    @Test
    void mittensAndTelescope() {
        Player p = new Player(null);
        assertEquals(1f, p.reloadScale());
        assertEquals(1f, p.spreadScale());
        p.addPassive(Item.QUICK_MITTENS);
        p.addPassive(Item.TOY_TELESCOPE);
        assertTrue(p.reloadScale() < 1f);
        assertTrue(p.spreadScale() < 1f);
    }

    @Test
    void newItemsLoadByName() {
        for (Item i : Item.values()) {
            assertEquals(i, Item.byName(i.name()));
        }
    }
}
