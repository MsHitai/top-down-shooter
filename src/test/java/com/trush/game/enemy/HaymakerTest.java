package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.trush.game.gfx.Draw;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockingDetails;
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
                if (lastHay >= 0) worstGap = Math.max(worstGap, frame - lastHay);
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
    @Test void showerIsAnnouncedForEightTenthsOfARealSecondEvenInHardMode() {
        for (boolean hard : new boolean[]{false, true}) {
            shots.clear();
            when(world.isHard()).thenReturn(hard);
            Haymaker boss = new Haymaker(4f, 3.5f, 0);
            boss.hp = boss.maxHp * .4f;
            Draw draw = mock(Draw.class, withSettings().mockMaker(MockMakers.SUBCLASS));
            float realTime = 0f;
            while (realTime < .75f) {
                boss.behave(STEP * GameWorld.bossPace(hard), world);
                // Includes the ceiling itself: standing up must not hide the warning.
                boss.drawOverlay(null, draw, new Rectangle(Arena.now.left, Arena.now.bottom,
                        Arena.now.right - Arena.now.left, Arena.now.top - Arena.now.bottom));
                realTime += STEP;
            }
            assertTrue(mockingDetails(draw).getInvocations().size() > 0);
            assertEquals(0, shots.size, "rain fired before its warning could be read");
            while (realTime < .9f) {
                boss.behave(STEP * GameWorld.bossPace(hard), world);
                realTime += STEP;
            }
            assertTrue(shots.size > 0);
        }
    }

    private record MovingGap(Bullet wall, float y) { }

    @Test void fallingHayDoesNotCloseTheMovingWallGaps() {
        for (boolean hard : new boolean[]{false, true}) {
            when(world.isHard()).thenReturn(hard);
            when(world.getDifficulty()).thenReturn(hard ? 1.2f : 1f);
            for (int seed = 0; seed < 6; seed++) {
                MathUtils.random.setSeed(seed);
                shots.clear();
                Array<Bullet> rain = new Array<>();
                Array<MovingGap> gaps = new Array<>();
                Haymaker boss = new Haymaker(4f, 3.5f, 0);
                boss.hp = boss.maxHp * .4f;
                int walls = 0;
                for (int frame = 0; frame < 2400; frame++) {
                    if (seed >= 3) target.set(Arena.now.centerX + MathUtils.sin(frame * STEP * .7f) * 4f,
                            Arena.now.centerY + MathUtils.cos(frame * STEP * .7f) * 2f);
                    boss.aimAngle = MathUtils.atan2Deg360(target.y - boss.pos.y, target.x - boss.pos.x);
                    int before = shots.size;
                    boss.behave(STEP * GameWorld.bossPace(hard), world);
                    Bullet lastBale = null;
                    float widest = 0f, gap = 0f;
                    for (int i = before; i < shots.size; i++) {
                        Bullet shot = shots.get(i);
                        if (isHay(shot)) rain.add(shot);
                        shot.vel.scl(hard ? 1f : .85f);
                        if (shot.shape != Bullet.Shape.SQUARE) continue;
                        if (lastBale != null && shot.pos.y - lastBale.pos.y > widest) {
                            widest = shot.pos.y - lastBale.pos.y;
                            gap = (shot.pos.y + lastBale.pos.y) / 2f;
                        }
                        lastBale = shot;
                    }
                    if (lastBale != null) { gaps.add(new MovingGap(lastBale, gap)); walls++; }
                    for (Bullet shot : shots) {
                        if (!shot.alive) continue;
                        shot.update(STEP);
                        if (!Arena.now.contains(shot.pos.x, shot.pos.y, 0f)) shot.alive = false;
                    }
                    for (MovingGap moving : gaps) {
                        if (!moving.wall.alive) continue;
                        for (Bullet straw : rain) {
                            if (!straw.alive) continue;
                            assertTrue(Math.abs(straw.pos.y - moving.y) > .3f
                                            || Math.abs(straw.pos.x - moving.wall.pos.x) > .35f,
                                    "shower blocked a wall gap: hard=" + hard + " seed=" + seed + " frame=" + frame + " gap=" + moving.y + " width=" + widest + " rain=" + straw.pos + " wall=" + moving.wall.pos);
                        }
                    }
                }
                assertTrue(walls >= 4, "simulation must exercise moving walls");
            }
        }
    }

}
