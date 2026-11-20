package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AttackWarningTest {
    @Test void arrowsFollowDiagonalAndCurvedShotsToTheirActualEntry() {
        Arena previous = Arena.now;
        try {
            Arena.now = new Arena(Arena.BOSS_COLS, Arena.BOSS_ROWS);
            Rectangle view = new Rectangle(3f, 2f, 4f, 4f);
            for (boolean curved : new boolean[]{false, true}) {
                Bullet actual = new Bullet(false, 1f, 3f, curved ? 90f : 30f, 3f, .065f, 1f, Color.WHITE);
                actual.curve = curved ? -90f : 0f;
                Draw draw = mock(Draw.class, withSettings().mockMaker(MockMakers.SUBCLASS));
                Vector2 center = new Vector2();
                doAnswer(call -> {
                    center.add((float) call.getArgument(1) / 2f, (float) call.getArgument(2) / 2f);
                    return null;
                }).when(draw).rotRect(isNull(), anyFloat(), anyFloat(), anyFloat(), anyFloat(),
                        anyFloat(), any(Color.class), anyFloat());
                Enemy boss = new Haymaker(4f, 4f, 0);
                boss.incomingArrow(null, draw, view, actual);
                assertEquals(1f, actual.pos.x, "drawing must not move the projectile");
                for (int frame = 0; frame < 480 && !view.contains(actual.pos); frame++) actual.update(1f / 120f);
                assertTrue(view.contains(actual.pos));
                assertEquals(MathUtils.clamp(actual.pos.x, view.x + .14f, view.x + view.width - .14f), center.x, .02f);
                assertEquals(MathUtils.clamp(actual.pos.y, view.y + .14f, view.y + view.height - .14f), center.y, .02f);
            }
        } finally {
            Arena.now = previous;
        }
    }

    @Test void outgoingBulletsDoNotProduceFalseWarnings() {
        Draw draw = mock(Draw.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        Enemy boss = new Haymaker(4f, 4f, 0);
        Bullet outgoing = new Bullet(false, 1f, 3f, 180f, 3f, .065f, 1f, Color.WHITE);
        boss.incomingArrow(null, draw, new Rectangle(3f, 2f, 4f, 4f), outgoing);
        verifyNoInteractions(draw);
    }
}
