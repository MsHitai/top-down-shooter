package com.trush.game.objects;

import com.trush.game.world.Arena;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirdTest {

    @Test
    void warnsFirstThenFliesAcrossAndIsGoneOffTheCloud() {
        Arena.now = new Arena(Arena.SMALL_COLS, Arena.SMALL_ROWS);
        Bird bird = new Bird(3f, 1, 2.4f, 0f);
        float startX = bird.x;
        assertTrue(bird.isWarning());
        bird.update(Bird.WARN_TIME * 0.5f);
        assertEquals(startX, bird.x, 1e-6, "it waits at the edge while the arrow shows");
        boolean setOff = false;
        for (int i = 0; i < 20 && !setOff; i++) {
            setOff = bird.update(0.05f);
        }
        assertTrue(setOff);
        for (int i = 0; i < 400 && bird.alive; i++) {
            bird.update(0.02f);
            assertTrue(bird.x >= startX, "flies one way only");
        }
        assertFalse(bird.alive, "gone once it leaves the room");
        assertTrue(bird.x > Arena.now.right);
    }
}
