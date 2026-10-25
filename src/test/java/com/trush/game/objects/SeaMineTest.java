package com.trush.game.objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeaMineTest {

    @Test
    void aMineWaitsUntilLitThenBlowsOnce() {
        SeaMine m = new SeaMine(1f, 1f);
        for (int i = 0; i < 100; i++) {
            assertFalse(m.update(0.1f), "it just floats there until something lights it");
        }
        m.light(SeaMine.FUSE);
        assertTrue(m.isLit());
        boolean blew = false;
        for (int i = 0; i < 20; i++) {
            if (m.update(0.05f)) {
                assertFalse(blew, "it only goes off once");
                blew = true;
            }
        }
        assertTrue(blew);
        assertTrue(m.gone);
    }

    @Test
    void aShotShortensTheFuseButNeverLengthensIt() {
        SeaMine m = new SeaMine(0f, 0f);
        m.light(SeaMine.FUSE);
        m.light(0.05f); // shot while blinking: goes off right away
        assertTrue(m.update(0.06f));
        SeaMine n = new SeaMine(0f, 0f);
        n.light(0.05f);
        n.light(SeaMine.FUSE); // walking up to one that's already about to go doesn't buy time
        assertTrue(n.update(0.06f));
    }
}
