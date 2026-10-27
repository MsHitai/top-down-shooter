package com.trush.game.objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GrandadVoiceTest {

    @Test
    void grumblesOnceTheBossDeathShowIsOver() {
        GrandadVoice v = new GrandadVoice();
        v.bossDown();
        v.update(1f);
        assertNull(v.line(), "quiet while the boss is still blowing up");
        v.update(1.5f);
        assertNotNull(v.line());
        v.update(FluffulVoice.SHOW + 0.1f);
        assertNull(v.line(), "and then he's done");
    }
}
