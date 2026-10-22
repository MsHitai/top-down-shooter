package com.trush.game.save;

import com.badlogic.gdx.Preferences;
import com.trush.game.objects.Weapon;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RunSaveTest {

    /** Preferences backed by a map. */
    private static Preferences prefs() {
        Map<String, String> store = new HashMap<>();
        Preferences p = mock(Preferences.class);
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
    void roundTripsThroughPreferences() {
        RunSave s = new RunSave();
        s.floor = 2;
        s.score = 420;
        s.kills = 13;
        s.health = 3;
        s.maxHealth = 6;
        s.blanks = 1;
        s.keys = 2;
        s.weaponIndex = 1;
        Weapon shotgun = Weapon.fluffShotgun();
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
        assertEquals(1, back.weaponIndex);
        assertEquals(2, back.weapons.size);
        Weapon restored = back.weapons.get(1).toWeapon();
        assertEquals("Fluff Shotgun", restored.getName());
        assertEquals(2, restored.getMag());
        assertEquals(17, restored.getAmmo());
        assertEquals("CONTINUE - FLOOR 2: THE TOWN", back.describe());

        RunSave.delete(prefs);
        assertFalse(RunSave.exists(prefs));
        assertNull(RunSave.read(prefs));
    }

    @Test
    void dropsSavesFromTheOldFormat() {
        Preferences prefs = prefs();
        prefs.putString("savedRun", "{\"version\":1,\"story\":true,\"score\":5}");
        assertNull(RunSave.read(prefs));
        assertFalse(RunSave.exists(prefs));
    }

    @Test
    void everyGunCanBeRecreatedByName() {
        for (Weapon w : new Weapon[]{Weapon.rustySidearm(), Weapon.fluffShotgun(), Weapon.pocketUzi(), Weapon.bigMagnum()}) {
            assertEquals(w.getName(), Weapon.byName(w.getName()).getName());
        }
        assertNull(Weapon.byName("Nerf Blaster"));
    }
}
