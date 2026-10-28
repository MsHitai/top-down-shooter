package com.trush.game.save;

import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.trush.game.enemy.Enemy;
import com.trush.game.objects.Weapon;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
