package com.trush.game.world;

import com.badlogic.gdx.utils.Array;
import com.trush.game.audio.Sfx;
import com.trush.game.enemy.BulletKin;
import com.trush.game.launcher.ETGFanfic;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class HardModeTest {

    @Test
    void hardModeBossBulletsFlyFasterThanANormalRuns() {
        assertEquals(0.85f, GameWorld.bossBulletSpeed(false), 1e-4f, "slowed down to read better");
        assertEquals(1f, GameWorld.bossBulletSpeed(true), 1e-4f, "not in hard mode");
    }

    @Test
    void hardModeBossesActFaster() {
        assertEquals(1f, GameWorld.bossPace(false), 1e-4f);
        assertEquals(1.2f, GameWorld.bossPace(true), 1e-4f);
    }

    @Test
    void hardModeDoesNotMakeBossesSpongier() {
        ETGFanfic game = mock(ETGFanfic.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
        game.sfx = mock(Sfx.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        GameWorld world = new GameWorld(game, new StoryDirector(1, false));
        world.restoreRun(true, new Array<>(), 0);
        BulletKin boss = new BulletKin(3f, 3f);
        boss.boss = true;
        float hp = boss.hp;
        world.spawnEnemy(boss);
        assertEquals(hp, boss.hp, 1e-4f);
    }

    @Test
    void hardModeEnemiesDropAmmoMoreOften() {
        assertEquals(0.04f, GameWorld.AMMO_DROP_CHANCE, 1e-4f);
        assertEquals(0.06f, GameWorld.HARD_AMMO_DROP_CHANCE, 1e-4f);
    }
}
