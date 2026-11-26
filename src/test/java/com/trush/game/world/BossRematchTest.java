package com.trush.game.world;

import com.trush.game.audio.Sfx;
import com.trush.game.enemy.Boss;
import com.trush.game.enemy.Enemy;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.player.Hero;
import com.trush.game.save.Flufficon.Medal;
import com.trush.game.save.Unlocks;
import com.trush.game.world.BossRematch.Difficulty;
import com.trush.game.world.Dream.Rule;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class BossRematchTest {
    @Test
    void everyBossStartsInItsOwnArenaWithTheSelectedRule() {
        for (Boss boss : Boss.values()) {
            BossRematch rematch = new BossRematch(boss, Difficulty.NORMAL, Rule.SLOW_RELOAD);
            StoryDirector director = new StoryDirector(rematch);
            GameWorld world = world(director);

            assertSame(director.getMap().boss, world.getRoom(), boss.title);
            assertEquals(boss.arenaCols(), world.getRoom().arena.cols, boss.title);
            assertEquals(boss.arenaRows(), world.getRoom().arena.rows, boss.title);
            assertEquals(1, world.getPendingEnemies().size, boss.title);
            assertEquals(boss.title, world.getPendingEnemies().first().bossName());
            assertTrue(world.getRoom().isLocked());
            assertTrue(world.rule(Rule.SLOW_RELOAD));
            assertFalse(world.rule(Rule.FAST_FORWARD));
            assertEquals(boss == Boss.WALKING_BUILDING, world.getRoom().onlySideDoors());
        }
    }

    @Test
    void aClearWaitsForTheLastEnemyAndTheBossDeathAnimation() {
        StoryDirector director = new StoryDirector(new BossRematch(Boss.BLUE_BLOB, Difficulty.HARD, Rule.SMALL_BLANKS));
        GameWorld world = world(director);

        director.update(1f, world);
        assertFalse(director.isFinished());
        Enemy boss = world.getPendingEnemies().pop();
        world.getDying().add(boss);
        director.update(1f, world);
        assertFalse(director.isFinished());

        world.getDying().clear();
        director.update(1f, world);
        assertTrue(director.isFinished());
        assertTrue(world.getRoom().pickups.isEmpty());
        assertFalse(world.isExitTaken());
    }

    @Test
    void difficultyAndMedalsAreLocalToTheRematch() {
        Hero oldHero = Hero.current;
        boolean oldHard = Unlocks.hardMode;
        try {
            Hero.current = Hero.BOY;
            Unlocks.hardMode = true;
            BossRematch normal = new BossRematch(Boss.HAYMAKER, Difficulty.NORMAL, Rule.FAST_FORWARD);
            assertFalse(world(new StoryDirector(normal)).isHard());
            assertEquals(Medal.SILVER, normal.medal());

            Unlocks.hardMode = false;
            BossRematch hard = new BossRematch(Boss.HAYMAKER, Difficulty.HARD, Rule.FAST_FORWARD);
            assertTrue(world(new StoryDirector(hard)).isHard());
            assertEquals(Medal.GOLD, hard.medal());
            assertFalse(Unlocks.hardMode);

            Hero.current = Hero.BABY;
            BossRematch practice = new BossRematch(Boss.HAYMAKER, Difficulty.PRACTICE, Rule.FAST_FORWARD);
            assertFalse(world(new StoryDirector(practice)).isHard());
            assertEquals(Medal.NONE, practice.medal());
        } finally {
            Hero.current = oldHero;
            Unlocks.hardMode = oldHard;
        }
    }

    private GameWorld world(StoryDirector director) {
        ETGFanfic game = new ETGFanfic();
        game.sfx = mock(Sfx.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        return new GameWorld(game, director);
    }
}
