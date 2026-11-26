package com.trush.game.save;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.trush.game.enemy.Boss;
import com.trush.game.enemy.Enemy;
import com.trush.game.objects.Weapon;
import com.trush.game.save.Flufficon.Medal;
import com.trush.game.world.BossRematch;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class FlufficonTest {

    @Test
    void everyEntryIsTheKindItsEnemyReports() {
        for (Flufficon.Entry e : Flufficon.ENEMIES) {
            Enemy made = e.create();
            assertEquals(e.id, made.dexId(), e.title);
        }
    }

    @Test
    void everyGunInTheBookCanBeMade() {
        for (String name : Flufficon.allGuns()) {
            assertNotNull(Weapon.byName(name), name);
        }
        assertTrue(Flufficon.allGuns().contains("Flufful", false), "the Flufful is in the book even while locked");
    }

    @Test
    void countsSurviveWritingAndReading() {
        ObjectIntMap<String> kills = new ObjectIntMap<>();
        kills.put("BulletKin", 12);
        kills.put("MarkedRat", 3);
        ObjectIntMap<String> back = new ObjectIntMap<>();
        Flufficon.read(Flufficon.write(kills), back);
        assertEquals(12, back.get("BulletKin", 0));
        assertEquals(3, back.get("MarkedRat", 0));

        ObjectSet<String> guns = new ObjectSet<>();
        guns.add("Rusty Sidearm");
        guns.add("Banana-rang");
        ObjectSet<String> gunsBack = new ObjectSet<>();
        Flufficon.readSet(Flufficon.write(guns), gunsBack);
        assertEquals(guns, gunsBack);
    }

    @Test
    void damagedEntriesAreSkipped() {
        ObjectIntMap<String> back = new ObjectIntMap<>();
        Flufficon.read("Rat:2,broken,Biker:x,:4", back);
        assertEquals(1, back.size);
        assertEquals(2, back.get("Rat", 0));
    }

    @Test
    void rematchMedalsRequireAPreviousWinAndKeepTheBestRankAcrossLoads() {
        Map<String, String> strings = new HashMap<>();
        Preferences prefs = mock(Preferences.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(prefs.getString(anyString(), anyString()))
                .thenAnswer(i -> strings.getOrDefault(i.getArgument(0), i.getArgument(1)));
        when(prefs.putString(anyString(), anyString())).thenAnswer(i -> {
            strings.put(i.getArgument(0), i.getArgument(1));
            return prefs;
        });
        try {
            Flufficon.load(prefs);
            Boss boss = Boss.HAYMAKER;
            Enemy enemy = boss.create(0, 0, 0);
            Flufficon.saw(enemy);
            assertFalse(BossRematch.available(boss));
            assertFalse(Flufficon.awardMedal(boss, Medal.GOLD));
            Flufficon.defeated(enemy, false);
            assertTrue(BossRematch.available(boss));
            assertEquals(Medal.NONE, Flufficon.medal(boss));
            assertTrue(Flufficon.awardMedal(boss, Medal.SILVER));
            Flufficon.load(prefs);
            assertEquals(Medal.SILVER, Flufficon.medal(boss));
            assertTrue(Flufficon.awardMedal(boss, Medal.GOLD));
            assertFalse(Flufficon.awardMedal(boss, Medal.SILVER));
            Flufficon.load(prefs);
            assertEquals(Medal.GOLD, Flufficon.medal(boss));
            assertEquals(1, Flufficon.timesBeaten(boss));
        } finally {
            strings.clear();
            Flufficon.load(prefs);
        }
    }
}
