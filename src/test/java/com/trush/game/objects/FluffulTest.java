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
}
