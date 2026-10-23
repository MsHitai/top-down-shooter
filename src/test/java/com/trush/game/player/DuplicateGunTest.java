package com.trush.game.player;

import com.trush.game.objects.Weapon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DuplicateGunTest {

    @Test
    void aSecondCopyUpgradesTheGunInsteadOfAddingIt() {
        Player p = new Player(null);
        assertNull(p.addWeapon(Weapon.pocketUzi()));
        int guns = p.getWeapons().size;
        Weapon uzi = p.currentWeapon();
        Weapon fresh = Weapon.pocketUzi();
        uzi.restoreAmmo(3, 10);

        Weapon.Upgrade up = p.addWeapon(Weapon.pocketUzi());
        assertNotNull(up);
        assertEquals(guns, p.getWeapons().size, "no second uzi");
        assertEquals("Pocket Uzi +1", uzi.displayName());
        assertEquals(uzi.getMaxAmmo(), uzi.getAmmo(), "refilled");
        boolean better = uzi.getDamage() > fresh.getDamage() || uzi.getMagSize() > fresh.getMagSize()
                || uzi.getBulletSpeed() > fresh.getBulletSpeed();
        assertTrue(better);
    }
}
