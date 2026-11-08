package com.trush.game.enemy;

import com.trush.game.save.Unlocks;
import com.trush.game.world.Stage;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The sea's two bosses after the Fluffy Shark: the Kraken and the Gorgon. */
class SeaBossesTest {

    @Test
    void theSeaRollsTheShark_untilItsBeaten() {
        boolean was = Unlocks.flufful;
        try {
            Unlocks.flufful = false;
            for (int i = 0; i < 200; i++) {
                assertEquals(Boss.SHARK, Boss.pickFor(Stage.SEA), "the sea's first boss is always the shark");
            }
            Unlocks.flufful = true;
            Set<Boss> seen = EnumSet.noneOf(Boss.class);
            for (int i = 0; i < 600; i++) {
                seen.add(Boss.pickFor(Stage.SEA));
            }
            assertEquals(EnumSet.of(Boss.SHARK, Boss.KRAKEN, Boss.GORGON), seen);
        } finally {
            Unlocks.flufful = was;
        }
    }

    @Test
    void theyAreBossesOfTheSea() {
        for (Boss b : new Boss[]{Boss.KRAKEN, Boss.GORGON}) {
            Enemy e = b.create(0, 0, 0);
            assertTrue(e.boss && e.flying);
            assertEquals(b.title, e.bossName());
            assertEquals(Stage.SEA, b.stage);
            assertEquals("boss_sea", b.music());
            assertTrue(b.create(0, 0, 1).maxHp > e.maxHp, "tougher at a higher tier");
        }
        assertTrue(Boss.KRAKEN.create(0, 0, 0) instanceof Kraken);
        assertTrue(Boss.GORGON.create(0, 0, 0) instanceof Gorgon);
    }

    @Test
    void theGorgonsStareOnlyCatchesThoseLookingAtHer() {
        // she's at the origin gazing straight down (270); the hero is 2 below her
        assertTrue(Gorgon.petrifies(0, 0, 270f, 0, -2f, 90f), "aiming up at her: stone");
        assertTrue(Gorgon.petrifies(0, 0, 270f, 0, -2f, 140f), "near enough to her counts");
        assertFalse(Gorgon.petrifies(0, 0, 270f, 0, -2f, 270f), "aiming away: safe");
        assertFalse(Gorgon.petrifies(0, 0, 270f, 0, -2f, 0f), "aiming sideways: safe");
        assertFalse(Gorgon.petrifies(0, 0, 270f, 2f, 0f, 180f), "out of her gaze: safe even looking at her");
        assertTrue(Gorgon.petrifies(0, 0, 350f, 2f, -0.2f, 175f), "the cone wraps round 0 degrees");
    }
}
