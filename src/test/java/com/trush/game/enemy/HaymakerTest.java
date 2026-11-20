package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class HaymakerTest {
    private static final float STEP = 1f / 60f;
    private final Array<Bullet> shots = new Array<>();
    private final Vector2 target = new Vector2(7f, 4f);
    private Arena previous;
    private GameWorld world;

    @BeforeEach void setup() {
        previous = Arena.now;
        Arena.now = new Arena(Arena.BOSS_COLS, Arena.BOSS_ROWS);
        world = mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
        when(world.getDifficulty()).thenReturn(1f);
        when(world.hasLineOfSight(anyFloat(), anyFloat(), anyFloat(), anyFloat(), anyFloat())).thenReturn(true);
        when(world.getPlayer().centerX()).thenAnswer(call -> target.x);
        when(world.getPlayer().hitboxY()).thenAnswer(call -> target.y);
        when(world.getPlayer().getVelocity()).thenReturn(new Vector2());
        doAnswer(call -> {
            Bullet shot = new Bullet(false, call.getArgument(0), call.getArgument(1), call.getArgument(2),
                    call.getArgument(3), .065f, 1f, Color.WHITE);
            shots.add(shot);
            return shot;
        }).when(world).spawnEnemyBullet(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }

    @AfterEach void restore() {
        Arena.now = previous;
    }

    private void tick(Enemy enemy) {
        enemy.aimAngle = MathUtils.atan2Deg360(target.y - enemy.pos.y, target.x - enemy.pos.x);
        enemy.behave(STEP, world);
    }

    /** A bullet dropped from the ceiling: near the top, pointing straight down. */
    private boolean isHay(Bullet shot) {
        return shot.pos.y > Arena.now.top - 0.15f && MathUtils.isEqual((shot.vel.angleDeg() + 360f) % 360f, 270f, 0.5f);
    }

    @Test void enragedHayRainsFromTheCeilingTheWholeTime() {
        Haymaker boss = new Haymaker(4f, 3.5f, 0);
        boss.hp = boss.maxHp * 0.4f; // enraged from the first tick
        int lastHay = -1, worstGap = 0;
        for (int frame = 0; frame < 600; frame++) { // ten seconds
            int count = shots.size;
            tick(boss);
            if (shots.size > count) {
                worstGap = Math.max(worstGap, frame - lastHay);
                lastHay = frame;
            }
        }
        int hay = 0;
        boolean leftHalf = false, rightHalf = false;
        for (Bullet shot : shots) {
            if (!isHay(shot)) continue;
            hay++;
            if (shot.pos.x < Arena.now.centerX) leftHalf = true;
            else rightHalf = true;
        }
        assertTrue(hay >= 25 && hay <= 40, "a steady shower, about three straws a second: " + hay);
        assertTrue(leftHalf && rightHalf, "it falls across the whole ceiling, not just on the player");
        assertTrue(worstGap <= 30, "the shower never stalls for a warning: worst gap " + worstGap + " frames");
    }
}
