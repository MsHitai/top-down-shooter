package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToyBoxGunsTest {

    @Test
    void theNewGunsAreInChestsByTier() {
        String[] d = {"Slingshot", "Popcorn Popper", "Hair Dryer", "Cap Gun", "Pea Shooter"};
        String[] c = {"Sticky Darts", "Kazoo"};
        for (String name : d) {
            Weapon w = Weapon.byName(name);
            assertNotNull(w, name);
            assertEquals(Tier.D, w.getTier(), name);
        }
        for (String name : c) {
            assertEquals(Tier.C, Weapon.byName(name).getTier(), name);
        }
        int dGuns = 0;
        for (Weapon w : Weapon.chestGuns()) {
            if (w.getTier() == Tier.D) dGuns++;
        }
        assertEquals(11, dGuns);
    }

    @Test
    void theSlingshotPullsBack() {
        Weapon sling = Weapon.slingshot();
        assertTrue(sling.chargesShots());
        sling.holdCharge(0.01f);
        float flick = sling.pull();
        sling.holdCharge(10f);
        assertTrue(sling.pull() > flick);
        assertEquals(1f, sling.pull(), 1e-4f, "a full pull tops out");
    }

    @Test
    void aBurstPeaWaitsInThePodBeforeItFlies() {
        Bullet pea = new Bullet(true, 0f, 0f, 0f, 8f, 0.04f, 3f, Color.GREEN);
        pea.delay = 0.1f;
        pea.update(0.05f);
        assertEquals(0f, pea.pos.x, 1e-4f, "still waiting");
        pea.update(0.06f);
        pea.update(0.1f);
        assertTrue(pea.pos.x > 0f, "then off it goes");
        assertFalse(!pea.alive);
    }
}
