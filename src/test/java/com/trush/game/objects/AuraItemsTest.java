package com.trush.game.objects;

import com.trush.game.enemy.BulletKin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuraItemsTest {

    @Test
    void theMagnetBecameTheFluffField() {
        assertEquals(Item.FLUFF_FIELD, Item.byName("MAGNET"), "old saves keep their item");
        assertEquals(Item.FLUFF_FIELD, Item.byName("FLUFF_FIELD"));
        assertEquals(Tier.C, Item.FLUFF_FIELD.tier);
        assertFalse(Item.FLUFF_FIELD.active);
        assertEquals(Tier.B, Item.STATIC_FUZZ.tier);
        assertFalse(Item.STATIC_FUZZ.active);
    }

    @Test
    void aShoveTopsTheKnockbackUpButNeverSlowsIt() {
        BulletKin kin = new BulletKin(2, 2);
        kin.shove(1, 0, 3.5f);
        assertEquals(3.5f, kin.knockSpeed(), 1e-4f);
        kin.shove(1, 0, 3.5f);
        assertEquals(3.5f, kin.knockSpeed(), 1e-4f, "held at the field's speed, not stacked");
        kin.shove(0, 1, 2f);
        assertTrue(kin.knockSpeed() > 3.5f, "a push the other way adds to it");
    }
}
