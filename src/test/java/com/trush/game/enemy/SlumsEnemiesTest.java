package com.trush.game.enemy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Slums' own enemies: the acid gang and the marked critters. */
class SlumsEnemiesTest {

    @Test
    void theAcidGangKeepsItsOwnAcidOffItself() {
        assertTrue(new Sludger(0, 0).acidProof);
        assertTrue(new AcidSprayer(0, 0).acidProof);
        assertTrue(new BarrelRoller(0, 0).acidProof, "his rubber boots");
        assertFalse(Rat.marked(0, 0).acidProof, "the critters aren't in the gang");
    }

    @Test
    void aMarkedRatIsABiggerQuickerTougherRat() {
        Rat plain = new Rat(0, 0);
        Rat marked = Rat.marked(0, 0);
        assertTrue(marked.isMarked());
        assertFalse(plain.isMarked());
        assertTrue(marked.maxHp > plain.maxHp);
        assertTrue(marked.speed > plain.speed);
        assertTrue(marked.radius > plain.radius * 1.4f, "half as big again");
        assertTrue(plain.squishable);
        assertFalse(marked.squishable, "too big to squish with a roll");
    }

    @Test
    void theAlleyDogIsBigAndBitesBack() {
        AlleyDog dog = new AlleyDog(0, 0);
        assertTrue(dog.radius > Rat.marked(0, 0).radius, "bigger than the rats it calls");
        assertTrue(AlleyDog.TEETH > 0 && AlleyDog.HOWL_RING > 0, "a lunge ends in a snap, a howl in a ring");
    }

    @Test
    void theStinkPigeonFliesOverEverything() {
        StinkPigeon p = new StinkPigeon(0, 0);
        assertTrue(p.flying);
        assertTrue(p.hurtsToTouch);
    }
}
