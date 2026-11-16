package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BananaHitboxTest {
    private static Bullet banana() {
        Bullet bullet = new Bullet(true, 0, 0, 0, 10f, 0.16f, 18f, Color.YELLOW);
        bullet.shape = Bullet.Shape.BANANA;
        return bullet;
    }

    @Test
    void emptySpaceAboveTheCrescentDoesNotHit() {
        Bullet banana = banana();
        assertFalse(banana.hitsCircle(0, 0.14f, 0.01f), "the old round hitbox reached this empty space");
        assertFalse(banana.hitsCircle(0, -0.19f, 0.01f));
        assertFalse(banana.hitsCircle(0, 0.35f, 0.2f), "a regular enemy above the empty curve");
        assertTrue(banana.hitsCircle(0, -0.06f, 0.01f), "the solid middle of the banana");
    }

    @Test
    void bothVisibleTipsCanHit() {
        Bullet banana = banana();
        assertTrue(banana.hitsCircle(-0.24f, 0.07f, 0.01f));
        assertTrue(banana.hitsCircle(0.24f, 0.07f, 0.01f));
        assertFalse(banana.hitsCircle(0.31f, 0.07f, 0.01f), "past the visible tip");
    }

    @Test
    void collisionRotatesWithTheDrawing() {
        Bullet banana = banana();
        banana.vel.set(0, 10);
        assertTrue(banana.hitsCircle(-0.07f, 0.24f, 0.01f));
        assertFalse(banana.hitsCircle(0.24f, 0.07f, 0.01f));
        // After 90 degrees of tumbling, the same drawing and hitbox orientation is reached.
        banana.vel.set(10, 0);
        banana.vel.setZero();
        banana.update(90f / 620f);
        banana.vel.set(10, 0);
        banana.update(0f); // inspect the final pose, rather than the preceding quarter-turn sweep
        assertTrue(banana.hitsCircle(-0.07f, 0.24f, 0.01f));
        assertFalse(banana.hitsCircle(0.24f, 0.07f, 0.01f));
    }

    @Test
    void aFastMovingTipCannotSkipATargetBetweenFrames() {
        Bullet banana = banana();
        banana.pos.set(1, 0);
        assertTrue(banana.hitsCircle(0.5f, -0.06f, 0.01f));
        assertTrue(banana.hitsCircle(0.5f, 0.07f, 0.01f));
        assertFalse(banana.hitsCircle(0.5f, 0.2f, 0.01f));
    }

    @Test
    void movingAndRotatingAtThirtyFpsDoesNotHitEmptySpaceAtTheStartOfTheFrame() {
        Bullet banana = banana();
        banana.update(1f / 30f);
        assertFalse(banana.hitsCircle(-0.25f, -0.26f, 0.2f),
                "using the final 21-degree pose for the entire path falsely hits this regular enemy");
    }

    @Test
    void movingAndRotatingAtThirtyFpsStillHitsTheActualStartOfTheArc() {
        Bullet banana = banana();
        banana.update(1f / 30f);
        assertTrue(banana.hitsCircle(-0.25f, 0.26f, 0.2f),
                "the real early pose touches this enemy, but the final-pose sweep misses it");
    }

    @Test
    void steeringBeforeTheUpdateKeepsTheLastFramesOrientation() {
        Bullet banana = banana();
        banana.update(0f);
        banana.vel.set(0, 10);
        banana.update(1f / 30f);
        assertTrue(banana.hitsCircle(0.24f, 0.07f, 0.01f),
                "the tip starts in the previous rendered pose, before steering changed the velocity");
    }

    @Test
    void crossingZeroDegreesDoesNotInventAnAlmostFullTurn() {
        Bullet banana = banana();
        banana.vel.setAngleDeg(359f);
        banana.update(0f);
        banana.vel.setAngleDeg(1f);
        banana.update(1f / 30f);
        assertFalse(banana.hitsCircle(0, 0.14f, 0.01f),
                "two degrees of steering plus tumbling, rather than a backwards 358-degree sweep");
    }

    @Test
    void oneThirtyFpsFrameAgreesWithTwoSixtyFpsFrames() {
        float[][] targets = {{-0.25f, -0.26f}, {-0.25f, 0.26f}, {0, 0}, {0.2f, -0.3f},
                {0.4f, 0.3f}, {0.6f, 0.2f}, {0.5f, -0.3f}};
        Bullet thirty = banana();
        thirty.update(1f / 30f);
        Bullet sixty = banana();
        sixty.update(1f / 60f);
        boolean[] firstFrame = new boolean[targets.length];
        for (int i = 0; i < targets.length; i++) {
            firstFrame[i] = sixty.hitsCircle(targets[i][0], targets[i][1], 0.2f);
        }
        sixty.update(1f / 60f);
        for (int i = 0; i < targets.length; i++) {
            float x = targets[i][0];
            float y = targets[i][1];
            assertEquals(thirty.hitsCircle(x, y, 0.2f), firstFrame[i] || sixty.hitsCircle(x, y, 0.2f),
                    "the same moving, spinning path at different frame rates: " + x + ", " + y);
        }
    }

    @Test
    void aCompleteRevolutionBetweenFramesIsNotDiscarded() {
        Bullet banana = banana();
        banana.vel.setZero();
        banana.update(360f / 620f);
        assertTrue(banana.hitsCircle(0, 0.22f, 0.01f),
                "a tip passes through this point during the full turn, although the final pose misses it");
    }

    @Test
    void ordinaryBulletsKeepTheirRoundSweptHitbox() {
        Bullet bullet = banana();
        bullet.shape = Bullet.Shape.BALL;
        assertTrue(bullet.hitsCircle(0, 0.14f, 0.01f));
        assertFalse(bullet.hitsCircle(0.24f, 0.07f, 0.01f));
        bullet.pos.set(1, 0);
        assertTrue(bullet.hitsCircle(0.5f, 0.14f, 0.01f));
    }
}
