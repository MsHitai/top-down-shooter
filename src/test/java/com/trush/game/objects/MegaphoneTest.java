package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MegaphoneTest {

    @Test
    void theMegaphoneIsATierCChestGun() {
        Weapon w = Weapon.byName("Megaphone");
        assertNotNull(w);
        assertEquals(Tier.C, w.getTier());
        assertTrue(w.getDamage() < 3f, "it stuns, it doesn't kill");
    }

    @Test
    void aShoutedAtBulletSlowsDownOnlyOnce() {
        Bullet b = new Bullet(false, 0f, 0f, 0f, 4f, 0.065f, 1f, Color.RED);
        b.accel = 2f;
        b.hush(0.5f);
        assertEquals(2f, b.vel.len(), 1e-4f);
        assertEquals(1f, b.accel, 1e-4f);
        assertTrue(b.hushed);
        b.hush(0.5f);
        assertEquals(2f, b.vel.len(), 1e-4f, "a second shout doesn't slow it further");
    }
}
