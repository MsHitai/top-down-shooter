package com.trush.game.objects;

import com.badlogic.gdx.Preferences;
import com.trush.game.save.Unlocks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

class FluffulTest {

    @AfterEach
    void relock() {
        Unlocks.flufful = false;
    }

    private static boolean poolHasFlufful() {
        for (Weapon w : Weapon.chestGuns()) {
            if (w.isFlufful()) return true;
        }
        return false;
    }

    @Test
    void onlyInChestsOnceUnlocked() {
        assertFalse(poolHasFlufful());
        assertTrue(Weapon.byName("Flufful").isFlufful(), "a saved Flufful always loads");
        Preferences prefs = mock(Preferences.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        assertTrue(Unlocks.unlockFlufful(prefs));
        verify(prefs).putBoolean("unlockedFlufful", true);
        assertFalse(Unlocks.unlockFlufful(prefs), "only the first time counts");
        assertTrue(poolHasFlufful());
    }

    @Test
    void talksThenStaysQuietForABit() {
        FluffulVoice v = new FluffulVoice();
        v.say(FluffulVoice.Event.MEET);
        String first = v.line();
        assertNotNull(first);
        v.say(FluffulVoice.Event.HURT); // too soon after a line: ignored
        assertEquals(first, v.line());
        v.say(FluffulVoice.Event.BOSS); // big moments always get a line
        assertNotEquals(first, v.line());
        v.update(FluffulVoice.SHOW + 0.1f, false);
        assertNull(v.line(), "the bubble goes away");
    }

    @Test
    void neverTheSameLineTwiceInARow() {
        FluffulVoice v = new FluffulVoice();
        String last = null;
        for (int i = 0; i < 30; i++) {
            v.say(FluffulVoice.Event.BOSS);
            assertNotEquals(last, v.line());
            last = v.line();
        }
    }

    @Test
    void growsWithKillsAndNeverRunsDry() {
        Weapon f = Weapon.flufful();
        assertTrue(f.isInfinite());
        float baseDamage = f.getDamage();
        int levelUps = 0;
        for (int i = 0; i < Weapon.LEVEL_KILLS[0]; i++) {
            if (f.addKill()) levelUps++;
        }
        assertEquals(1, levelUps);
        assertEquals(2, f.getLevel());
        assertEquals("Flufful Lv2", f.displayName());
        assertTrue(f.getDamage() > baseDamage);
        for (int i = 0; i < 1000; i++) {
            f.addKill();
        }
        assertEquals(Weapon.LEVEL_KILLS.length + 1, f.getLevel(), "tops out");
        assertFalse(Weapon.pocketUzi().addKill(), "only the Flufful levels up");
    }

    @Test
    void aSavedFluffulKeepsItsLevel() {
        Weapon f = Weapon.flufful();
        for (int i = 0; i < 70; i++) f.addKill();
        Weapon back = Weapon.flufful();
        back.restoreKills(f.getKills());
        assertEquals(f.getLevel(), back.getLevel());
        assertEquals(f.getDamage(), back.getDamage(), 1e-4f);
        assertEquals(70, back.getKills());
    }
}
