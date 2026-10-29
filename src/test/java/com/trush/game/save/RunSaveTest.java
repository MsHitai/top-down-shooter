package com.trush.game.save;

import com.badlogic.gdx.Preferences;
import com.trush.game.objects.Weapon;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class RunSaveTest {

    /** Preferences backed by a map. */
    private static Preferences prefs() {
        Map<String, String> store = new HashMap<>();
        Preferences p = mock(Preferences.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(p.putString(anyString(), anyString())).thenAnswer(i -> {
            store.put(i.getArgument(0), i.getArgument(1));
            return p;
        });
        when(p.getString(anyString())).thenAnswer(i -> store.get(i.<String>getArgument(0)));
        when(p.contains(anyString())).thenAnswer(i -> store.containsKey(i.<String>getArgument(0)));
        doAnswer(i -> store.remove(i.<String>getArgument(0))).when(p).remove(anyString());
        return p;
    }

    @Test
    void aHardRunStaysHardAndRemembersWhereItHasBeen() {
        RunSave s = new RunSave();
        s.floor = 3;
        s.hard = true;
        s.stagesSeen.add("VILLAGE");
        s.stagesSeen.add("TOWN");
        s.flawlessBosses = 2;
        Preferences prefs = prefs();
        s.write(prefs);
        RunSave back = RunSave.read(prefs);
        assertNotNull(back);
        assertTrue(back.hard);
        assertEquals(2, back.stagesSeen.size);
        assertEquals("TOWN", back.stagesSeen.get(1));
        assertEquals(2, back.flawlessBosses);
    }

    @Test
    void roundTripsThroughPreferences() {
        RunSave s = new RunSave();
        s.floor = 2;
        s.score = 420;
        s.kills = 13;
        s.health = 3;
        s.maxHealth = 6;
        s.blanks = 1;
        s.keys = 2;
        s.coins = 87;
        s.junk = 4;
        s.junkKnight = true;
        s.shopGrudge = true;
        s.weaponIndex = 1;
        Weapon shotgun = Weapon.fluffShotgun();
        shotgun.apply(Weapon.Upgrade.DAMAGE);
        shotgun.apply(Weapon.Upgrade.AMMO);
        shotgun.restoreAmmo(2, 17);
        s.weapons.add(RunSave.GunData.of(Weapon.rustySidearm()));
        s.weapons.add(RunSave.GunData.of(shotgun));

        Preferences prefs = prefs();
        s.write(prefs);
        assertTrue(RunSave.exists(prefs));

        RunSave back = RunSave.read(prefs);
        assertNotNull(back);
        assertEquals(2, back.floor);
        assertEquals(420, back.score);
        assertEquals(13, back.kills);
        assertEquals(3, back.health);
        assertEquals(1, back.blanks);
        assertEquals(2, back.keys);
        assertEquals(87, back.coins);
        assertEquals(4, back.junk);
        assertTrue(back.junkKnight);
        assertTrue(back.shopGrudge);
        assertEquals(1, back.weaponIndex);
        assertEquals(2, back.weapons.size);
        Weapon restored = back.weapons.get(1).toWeapon();
        assertEquals("Fluff Shotgun", restored.getName());
        assertEquals(2, restored.getMag());
        assertEquals(17, restored.getAmmo());
        assertEquals("Fluff Shotgun +2", restored.displayName());
        assertEquals(shotgun.getDamage(), restored.getDamage(), 1e-4f);
        assertEquals(shotgun.getMaxAmmo(), restored.getMaxAmmo());
        assertEquals("CONTINUE - FLOOR 2: THE TOWN", back.describe());

        RunSave.delete(prefs);
        assertFalse(RunSave.exists(prefs));
        assertNull(RunSave.read(prefs));
    }

    @Test
    void aSaveFromBeforeTheShopHasNoCoins() {
        Preferences prefs = prefs();
        prefs.putString("savedRun", "{\"version\":2,\"floor\":3,\"health\":6,\"maxHealth\":6,\"keys\":1}");
        RunSave back = RunSave.read(prefs);
        assertNotNull(back);
        assertEquals(0, back.coins);
        assertEquals(0, back.junk, "no junk before chests could break");
        assertFalse(back.junkKnight);
        assertFalse(back.shopGrudge);
    }

    @Test
    void dropsSavesFromTheOldFormat() {
        Preferences prefs = prefs();
        prefs.putString("savedRun", "{\"version\":1,\"story\":true,\"score\":5}");
        assertNull(RunSave.read(prefs));
        assertFalse(RunSave.exists(prefs));
    }

    @Test
    void everyTierHasAtLeastThreeChestGuns() {
        java.util.Map<com.trush.game.objects.Tier, Integer> perTier = new java.util.EnumMap<>(com.trush.game.objects.Tier.class);
        for (Weapon w : Weapon.chestGuns()) {
            perTier.merge(w.getTier(), 1, Integer::sum);
        }
        for (com.trush.game.objects.Tier t : com.trush.game.objects.Tier.values()) {
            int least = t == com.trush.game.objects.Tier.S ? 1 : 3; // S is the rare black-chest tier
            assertTrue(perTier.getOrDefault(t, 0) >= least, "guns of tier " + t);
        }
    }

    @Test
    void everyGunCanBeRecreatedByName() {
        assertEquals("Rusty Sidearm", Weapon.byName("Rusty Sidearm").getName());
        for (Weapon w : Weapon.chestGuns()) {
            assertEquals(w.getName(), Weapon.byName(w.getName()).getName());
        }
        assertNull(Weapon.byName("Nerf Blaster"));
    }
}
