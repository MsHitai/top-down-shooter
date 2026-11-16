package com.trush.game.enemy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LampMimicTest {

    @Test
    void theWholeLampCanBeShotNotJustItsFoot() {
        LampMimic mimic = new LampMimic(5f, 5f); // the foot of the lamp
        assertTrue(mimic.bodyContains(mimic.lanternX(), mimic.lanternY()), "its lantern face, far above the round hitbox");
        assertTrue(mimic.bodyContains(5.03f, 5.5f), "halfway up the post");
        assertFalse(mimic.bodyContains(5.5f, 5.5f), "beside the post");
        assertFalse(mimic.bodyContains(mimic.lanternX(), mimic.lanternY() + 0.4f), "above the lantern");
    }
}
