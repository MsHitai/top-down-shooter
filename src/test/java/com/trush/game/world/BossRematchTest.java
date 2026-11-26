package com.trush.game.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.trush.game.audio.Sfx;
import com.trush.game.enemy.Boss;
import com.trush.game.enemy.Enemy;
import com.trush.game.gfx.Sprites;
import com.trush.game.input.Gamepad;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Chest;
import com.trush.game.objects.Pickup;
import com.trush.game.objects.Tier;
import com.trush.game.player.Hero;
import com.trush.game.save.Flufficon;
import com.trush.game.save.Flufficon.Medal;
import com.trush.game.save.Unlocks;
import com.trush.game.world.BossRematch.Difficulty;
import com.trush.game.world.Dream.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class BossRematchTest {
    private Input oldInput;

    @BeforeEach
    void setUpInput() {
        oldInput = Gdx.input;
        Gdx.input = mock(Input.class, withSettings().mockMaker(MockMakers.SUBCLASS));
    }

    @AfterEach
    void restoreInput() {
        Gdx.input = oldInput;
    }

    @Test
    void everyRematchStartsInATreasureRoomAdjoiningTheCorrectArena() {
        for (Boss boss : Boss.values()) {
            BossRematch rematch = new BossRematch(boss, Difficulty.NORMAL, Rule.SLOW_RELOAD);
            StoryDirector director = new StoryDirector(rematch);
            GameWorld world = world(director);

            Room treasure = director.getMap().start;
            assertSame(treasure, world.getRoom(), boss.title);
            assertEquals(2, director.getMap().rooms.size, boss.title);
            assertEquals(Room.Type.TREASURE, treasure.type, boss.title);
            assertTrue(treasure.cleared, boss.title);
            assertEquals(1, treasure.chests.size, boss.title);
            assertTrue(world.getPendingEnemies().isEmpty(), boss.title);
            assertFalse(world.rule(Rule.SLOW_RELOAD), boss.title);

            enterBossRoom(director, world);
            assertSame(director.getMap().boss, world.getRoom(), boss.title);
            assertEquals(boss.arenaCols(), world.getRoom().arena.cols, boss.title);
            assertEquals(boss.arenaRows(), world.getRoom().arena.rows, boss.title);
            assertEquals(1, world.getEnemies().size, boss.title);
            assertEquals(boss.title, world.getEnemies().first().bossName());
            assertTrue(world.getRoom().isLocked());
            assertTrue(world.rule(Rule.SLOW_RELOAD));
            assertFalse(world.rule(Rule.FAST_FORWARD));
            assertEquals(boss == Boss.WALKING_BUILDING, world.getRoom().onlySideDoors());
        }
    }

    @Test
    void chestOpensForFreeAndEquipsOneGunWithoutRecordingADiscovery() {
        GameWorld world = world(new StoryDirector(new BossRematch(Boss.BLUE_BLOB, Difficulty.NORMAL, Rule.SLOW_RELOAD)));
        Chest chest = world.getRoom().chests.first();
        boolean wasDiscovered = Flufficon.hasFound(chest.gun);
        int keys = world.getPlayer().getKeys();
        assertFalse(chest.locked);
        assertNull(chest.item);
        assertEquals(chest.tier, chest.gun.getTier());

        world.getPlayer().setFeet(chest.cx, chest.solid.y - 0.25f);
        world.interact();
        assertTrue(chest.opened);
        assertEquals(keys, world.getPlayer().getKeys());
        assertEquals(1, world.getPickups().size);
        Pickup loot = world.getPickups().first();
        assertEquals(Pickup.Type.GUN, loot.type);
        world.getPlayer().setFeet(loot.pos.x, loot.pos.y - (world.getPlayer().hitboxY() - world.getPlayer().feetY()));
        world.update(0f);
        assertSame(chest.gun, world.getPlayer().currentWeapon());
        assertEquals(wasDiscovered, Flufficon.hasFound(chest.gun));
        world.interact();
        assertTrue(world.getPickups().isEmpty());
    }

    @Test
    void medalCapsHoldAndHardImprovesTheOddsOfEveryHigherTier() {
        Tier[] caps = {Tier.B, Tier.A, Tier.S};
        for (Medal medal : Medal.values()) {
            Tier cap = caps[medal.ordinal()];
            int[] normal = new int[Tier.values().length];
            int[] hard = new int[normal.length];
            for (int i = 0; i < 10_000; i++) {
                float roll = (i + 0.5f) / 10_000;
                normal[BossRematch.chestTier(medal, false, roll).ordinal()]++;
                hard[BossRematch.chestTier(medal, true, roll).ordinal()]++;
            }
            for (int i = 0; i < normal.length; i++) {
                if (i > cap.ordinal()) {
                    assertEquals(0, normal[i]);
                    assertEquals(0, hard[i]);
                } else {
                    assertTrue(normal[i] > 0);
                    assertTrue(hard[i] > 0);
                    if (i > 0) {
                        assertTrue(normal[i - 1] > normal[i]);
                        assertTrue(hard[i] > normal[i]);
                    }
                }
            }
        }
    }

    @Test
    void aClearWaitsForTheLastEnemyAndTheBossDeathAnimation() {
        StoryDirector director = new StoryDirector(new BossRematch(Boss.BLUE_BLOB, Difficulty.HARD, Rule.SMALL_BLANKS));
        GameWorld world = world(director);
        enterBossRoom(director, world);

        director.update(1f, world);
        assertFalse(director.isFinished());
        Enemy boss = world.getEnemies().pop();
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
        game.pad = new Gamepad();
        game.sprites = mock(Sprites.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        TextureRegion region = new TextureRegion();
        when(game.sprites.prop(anyString())).thenReturn(region);
        return new GameWorld(game, director);
    }

    private void enterBossRoom(StoryDirector director, GameWorld world) {
        Room room = world.getRoom();
        Arena arena = room.arena;
        int door = room.doors[Room.E] ? Room.E : Room.N;
        assertTrue(room.doorOpen(door));
        assertSame(director.getMap().boss, director.getMap().neighbor(room, door));
        if (door == Room.E) {
            world.getPlayer().setFeet(arena.worldW - 0.08f, (arena.doorY0 + arena.doorY1) / 2f - 0.15f);
        } else {
            world.getPlayer().setFeet(arena.centerX, arena.top + 0.32f);
        }
        world.update(0f);
    }
}
