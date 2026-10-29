package com.trush.game.objects;

import com.badlogic.gdx.math.Vector2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Shooting chests open, and the Junk Knight built from what's left. */
class BrokenChestTest {

    @Test
    void aChestTakesAFewShotsToBreak() {
        Chest c = new Chest(Tier.D, 3, 3, true, null, Item.GRENADE);
        int shots = 0;
        while (!c.hit(3.5f)) shots++; // the starter pistol
        assertTrue(shots >= 5, "not by accident: " + shots);
    }

    @Test
    void whatsInsideRarelySurvives() {
        Chest withItem = new Chest(Tier.C, 3, 3, false, null, Item.GRENADE);
        Chest empty = new Chest(Tier.C, 3, 3, false, null, null);
        int contents = 0;
        int scraps = 0;
        int junk = 0;
        for (int i = 0; i < 1000; i++) {
            switch (withItem.remains(i / 1000f)) {
                case CONTENTS: contents++; break;
                case SCRAPS: scraps++; break;
                default: junk++;
            }
            assertFalse(empty.remains(i / 1000f) == Chest.Remains.CONTENTS, "nothing to save in an empty one");
        }
        assertTrue(contents > 0 && contents < 100, "a very small chance: " + contents);
        assertTrue(scraps > contents, "half a heart or ammo more often: " + scraps);
        assertTrue(junk > scraps, "and junk most of all: " + junk);
    }

    @Test
    void theJunkKnightGrowsUpToWingsAndABow() {
        JunkKnight k = new JunkKnight();
        k.setLevel(1);
        float first = k.damage();
        assertFalse(k.flies());
        for (int level = 2; level < JunkKnight.MAX_LEVEL; level++) {
            float before = k.damage();
            k.setLevel(level);
            assertTrue(k.damage() > before, "level " + level + " hits harder");
            assertFalse(k.flies());
        }
        k.setLevel(99);
        assertEquals(JunkKnight.MAX_LEVEL, k.level());
        assertTrue(k.flies(), "the last level has wings and a bow");
        assertTrue(first <= 2f, "the wooden sword barely scratches");
    }

    @Test
    void itWalksToItsTargetAndSwings() {
        JunkKnight k = new JunkKnight();
        k.setLevel(1);
        Vector2 enemy = new Vector2(5f, 3f);
        for (int i = 0; i < 200; i++) k.update(0.02f, 3f, 3f, true, enemy);
        assertTrue(k.canReach(enemy.x, enemy.y, 0.2f), "close enough to hit: " + k.pos);
    }
}
