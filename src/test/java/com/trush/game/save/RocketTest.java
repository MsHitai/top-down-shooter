package com.trush.game.save;

import com.badlogic.gdx.Preferences;
import com.trush.game.player.Hero;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/** The family's rocket at home: one piece for each of them who beats the Fluffy King, remembered for good. */
class RocketTest {

    private final EnumSet<Hero> was = EnumSet.copyOf(Unlocks.kingBeatenBy);

    @AfterEach
    void restore() {
        Unlocks.kingBeatenBy.clear();
        Unlocks.kingBeatenBy.addAll(was);
    }

    private static Preferences prefs() {
        Map<String, String> store = new HashMap<>();
        Preferences p = mock(Preferences.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(p.putString(anyString(), anyString())).thenAnswer(i -> {
            store.put(i.getArgument(0), i.getArgument(1));
            return p;
        });
        when(p.getString(anyString(), anyString()))
                .thenAnswer(i -> store.getOrDefault(i.<String>getArgument(0), i.getArgument(1)));
        when(p.getBoolean(anyString(), anyBoolean())).thenAnswer(i -> i.getArgument(1));
        when(p.getInteger(anyString(), anyInt())).thenAnswer(i -> i.getArgument(1));
        return p;
    }

    @Test
    void eachOfTheFamilyAddsTheirPieceOnce() {
        Preferences prefs = prefs();
        Unlocks.load(prefs);
        assertTrue(Unlocks.kingBeatenBy.isEmpty());
        assertTrue(Unlocks.beatKing(Hero.SISTER, prefs));
        assertFalse(Unlocks.beatKing(Hero.SISTER, prefs), "a second win doesn't add a second piece");
        assertTrue(Unlocks.beatKing(Hero.BOY, prefs));
        assertFalse(Unlocks.rocketBuilt());

        Unlocks.load(prefs); // remembered between sessions
        assertEquals(EnumSet.of(Hero.BOY, Hero.SISTER), Unlocks.kingBeatenBy);
    }

    @Test
    void theRocketIsFinishedWhenAllFourHaveBeatenTheKing() {
        Preferences prefs = prefs();
        Unlocks.load(prefs);
        for (Hero h : Hero.values()) {
            assertFalse(Unlocks.rocketBuilt());
            Unlocks.beatKing(h, prefs);
        }
        assertTrue(Unlocks.rocketBuilt());
    }
}
