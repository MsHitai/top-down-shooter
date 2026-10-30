package com.trush.game.objects;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GrandadVoiceTest {

    @Test
    void grumblesOnceTheBossDeathShowIsOver() {
        GrandadVoice v = new GrandadVoice();
        v.bossDown();
        for (int i = 0; i < 10; i++) v.update(1f, true);
        assertNull(v.line(), "quiet while the camera is still on the boss");
        v.update(0.5f, false);
        assertNull(v.line(), "the camera is still gliding back to him");
        v.update(1f, false);
        assertNotNull(v.line());
        v.update(FluffulVoice.SHOW + 0.1f, false);
        assertNull(v.line(), "and then he's done");
    }

    @Test
    void aWholeRunOfBossesNeverHearsALineTwice() {
        assertTrue(GrandadVoice.lineCount() >= 9, "enough lines for every boss of a full run");
        GrandadVoice.refillBag();
        GrandadVoice v = new GrandadVoice();
        Set<String> heard = new HashSet<>();
        for (int i = 0; i < GrandadVoice.lineCount(); i++) {
            v.bossDown();
            v.update(2f, false);
            assertTrue(heard.add(v.line()), "repeated: " + v.line());
            v.update(FluffulVoice.SHOW + 0.1f, false);
        }
    }
}
