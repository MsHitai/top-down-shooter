package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.audio.Sfx;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/** Grandfluff's two lives: a golden one (the old, then the young king), then the Moon's red one. */
class GrandfluffTest {

    private Arena previousArena;

    @BeforeEach
    void setUpArena() {
        previousArena = Arena.now;
        Arena.now = new Arena(Arena.MEDIUM_COLS, Arena.MEDIUM_ROWS);
    }

    @AfterEach
    void restoreArena() {
        Arena.now = previousArena;
    }

    private static GameWorld world() {
        return mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(Answers.RETURNS_DEEP_STUBS));
    }

    private static GameWorld attackWorld(Array<Bullet> bullets) {
        GameWorld world = world();
        when(world.getDifficulty()).thenReturn(1f);
        when(world.getPlayer().centerX()).thenReturn(Arena.now.centerX);
        when(world.getPlayer().hitboxY()).thenReturn(Arena.now.bottom + 1f);
        when(world.rayLength(anyFloat(), anyFloat(), anyFloat(), anyFloat())).thenReturn(10f);
        when(world.getEnemyBullets()).thenReturn(bullets);
        doAnswer(inv -> {
            Bullet b = new Bullet(false, inv.getArgument(0), inv.getArgument(1), inv.getArgument(2),
                    inv.getArgument(3), 0.065f, 1f, Color.WHITE);
            bullets.add(b);
            return b;
        }).when(world).spawnEnemyBullet(anyFloat(), anyFloat(), anyFloat(), anyFloat());
        return world;
    }

    private static Grandfluff beginWarning(GameWorld world, boolean moon, String sound, float volume) {
        Grandfluff g = new Grandfluff(Arena.now.centerX, Arena.now.centerY, 0);
        g.spawnTimer = 0f;
        if (moon) {
            g.hit(g.maxHp, new Vector2(1f, 0f), 0f, world);
            g.behave(3.3f, world);
        }
        boolean[] warned = {false};
        Sfx sfx = world.sfx();
        doAnswer(inv -> {
            warned[0] = true;
            return null;
        }).when(sfx).play(sound, volume);
        for (int frame = 0; frame < 2400 && !warned[0]; frame++) g.update(1f / 120f, world);
        assertTrue(warned[0], "the boss must begin the warning");
        return g;
    }

    @ParameterizedTest
    @CsvSource({"false, false", "true, false", "false, true", "true, true"})
    void constellationsGiveThePlayerOnePointFourSecondsInBothModes(boolean hard, boolean moon) {
        Array<Bullet> bullets = new Array<>();
        GameWorld world = attackWorld(bullets);
        when(world.isHard()).thenReturn(hard);
        Grandfluff g = beginWarning(world, moon, "pop", 0.5f);
        boolean[] lit = {false};
        Sfx sfx = world.sfx();
        doAnswer(inv -> {
            lit[0] = true;
            return null;
        }).when(sfx).play("zap", 0.8f);

        for (int frame = 0; frame < 167; frame++) g.update(1f / 120f, world);
        assertFalse(lit[0], "the constellation must not fire before 1.4 seconds");
        for (int frame = 0; frame < 2; frame++) g.update(1f / 120f, world);
        assertTrue(lit[0], "the constellation must fire at 1.4 seconds, within one frame");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void eyeBeamsWarnForPointNineSecondsAndKeepTheirOriginalSweepPace(boolean hard) {
        Array<Bullet> bullets = new Array<>();
        GameWorld world = attackWorld(bullets);
        when(world.isHard()).thenReturn(hard);
        Grandfluff g = beginWarning(world, true, "buzz", 0.7f);
        bullets.clear();

        for (int frame = 0; frame < 107; frame++) g.update(1f / 120f, world);
        assertTrue(bullets.isEmpty(), "no beam sparks before the full 0.9-second warning");
        for (int frame = 0; frame < 2; frame++) g.update(1f / 120f, world);
        assertFalse(bullets.isEmpty(), "the beams must start at 0.9 seconds, within one frame");
        for (Bullet b : bullets) assertEquals(Color.valueOf("ff5a5a"), b.color);

        int frames = 0;
        int quietFrames = 0;
        while (frames++ < 300 && quietFrames < 20) {
            bullets.clear();
            g.update(1f / 120f, world);
            quietFrames = bullets.isEmpty() ? quietFrames + 1 : 0;
        }
        float sweepSeconds = (frames - 1 - quietFrames) / 120f;
        assertEquals(hard ? 2f / 1.2f : 2f, sweepSeconds, 0.12f,
                "only the warning changes; the hard-mode sweep stays faster");
    }

    @Test
    void theGoldenLifeGoesFirstAndTheRedOneWaitsFullUnderIt() {
        Grandfluff g = new Grandfluff(0, 0, 0);
        g.spawnTimer = 0f;
        assertEquals(1f, g.bossBarFraction(), "the red bar sits full under the gold");
        assertEquals(1f, g.bossBarGold());
        g.hp = g.maxHp * 0.3f;
        assertEquals(0.3f, g.bossBarGold(), 0.001f);
        assertEquals(1f, g.bossBarFraction());
    }

    @Test
    void theLastHitOnTheGoldDoesNotFinishHimHeRisesAsTheMoon() {
        GameWorld world = world();
        Grandfluff g = new Grandfluff(0, 0, 0);
        g.spawnTimer = 0f;
        g.hp = 5f;
        g.hit(50f, new Vector2(1, 0), 0f, world);
        assertTrue(g.alive, "not beaten yet");
        assertEquals(g.maxHp, g.hp, "a whole new life");
        assertEquals(0f, g.bossBarGold(), "the gold is gone");
        assertEquals(1f, g.bossBarFraction(), "the Moon's red bar, full");
        assertTrue(g.isUntouchable(), "out of reach while he rises");
        assertFalse(g.isEnraged(), "(red once he's up there)");
    }

    @Test
    void moonBeamsClearOldBulletsBeforeTheWarningAndPauseCraterFans() {
        Array<Bullet> bullets = new Array<>();
        GameWorld world = attackWorld(bullets);
        Grandfluff g = new Grandfluff(Arena.now.centerX, Arena.now.centerY, 0);
        g.spawnTimer = 0f;
        g.hit(g.maxHp, new Vector2(1f, 0f), 0f, world);
        g.behave(3.3f, world); // rise, then the Moon's first attack: crater rings
        g.behave(0.81f, world);
        for (int i = 0; i < 9; i++) g.behave(0.46f, world);
        assertFalse(bullets.isEmpty(), "the previous attack leaves bullets on the field");
        bullets.clear();
        clearInvocations(world);

        g.behave(0.81f, world); // begin the eye-beam warning
        verify(world).clearEnemyBullets(0f, 0f, -1f);
        g.behave(0.85f, world);
        assertTrue(bullets.isEmpty(), "no crater fans during the beam warning");
        for (int i = 0; i < 20; i++) g.behave(0.1f, world);
        assertFalse(bullets.isEmpty(), "the beams still shed their own sparks");
        for (Bullet b : bullets) {
            assertEquals(Color.valueOf("ff5a5a"), b.color, "only beam sparks during the sweep");
        }
        verify(world).clearEnemyBullets(0f, 0f, -1f); // once at entry, never each frame
    }
}
