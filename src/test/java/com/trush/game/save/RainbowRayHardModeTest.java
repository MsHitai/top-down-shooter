package com.trush.game.save;

import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Item;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RainbowRayHardModeTest {
    @Test
    void normalRunsKeepTheOriginalRainbowRay() {
        Player player = new Player(null);
        Weapon gun = Weapon.rainbowRay();
        player.addWeapon(gun);
        assertEquals(480, gun.getAmmo());
        assertEquals(480, gun.getMaxAmmo());
        assertEquals(2.8f, gun.getDamage(), 0.0001f);
        assertEquals(40, gun.getMagSize());
    }

    @Test
    void hardRunsBoostBothStartingAndNewRainbowRaysWithoutRefillingTwice() {
        Player player = new Player(null);
        Weapon gun = Weapon.rainbowRay();
        player.addWeapon(gun);
        player.setHardMode(true, true);
        assertEquals(900, gun.getAmmo());
        assertEquals(2.9f, gun.getDamage(), 0.0001f);
        assertEquals(40, gun.getMagSize());
        gun.restoreAmmo(20, 700);
        player.setHardMode(true, true);
        assertEquals(700, gun.getAmmo());

        Player other = new Player(null);
        other.setHardMode(true, true);
        Weapon found = Weapon.rainbowRay();
        other.addWeapon(found);
        assertEquals(900, found.getAmmo());
        assertEquals(900, found.getMaxAmmo());
        assertEquals(2.9f, found.getDamage(), 0.0001f);
    }

    @Test
    void savedHardGunKeepsUpgradesSpentAmmoAndBottomlessBag() {
        Weapon gun = Weapon.rainbowRay();
        gun.setHardMode(true, true);
        gun.apply(Weapon.Upgrade.DAMAGE);
        gun.apply(Weapon.Upgrade.AMMO);
        gun.restoreAmmo(25, 700);
        Weapon restored = RunSave.GunData.of(gun).toWeapon(true);
        Player player = new Player(null);
        player.setHardMode(true, false);
        player.restore(6, 6, 2, 0, Array.with(restored), 0);
        assertEquals(25, restored.getMag());
        player.restoreItems(Array.with(Item.BOTTOMLESS_BAG), null, null);
        assertEquals(700, restored.getAmmo(), "continuing must not grant extra ammo");
        assertEquals(100, restored.getMag(), "the bag loads its doubled magazine as usual");
        assertEquals(2250, restored.getMaxAmmo());
        assertEquals(2.9f * 1.15f, restored.getDamage(), 0.0001f);
    }

    @Test
    void dreamCatcherRefillsToTheHardCapacity() {
        RunSave.GunData saved = RunSave.GunData.of(Weapon.rainbowRay());
        saved.full = true;
        Weapon restored = saved.toWeapon(true);
        assertEquals(900, restored.getAmmo());
        assertEquals(40, restored.getMag());
    }
}
