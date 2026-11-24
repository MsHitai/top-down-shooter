package com.trush.game.world;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Array;
import com.trush.game.enemy.Boss;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Item;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Hero;
import com.trush.game.save.RunSave;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/** The bedroom's dreams: five random floors that end, today's dream and the toy box's boss rush. */
class DreamTest {

    private static StoryDirector floorOf(Dream d) {
        return new StoryDirector(d, new Array<>(), new Array<Item>());
    }

    @Test
    void eachHeroGetsTheirOwnBossAndArenaUnderTheBed() {
        Hero original = Hero.current;
        try {
            checkUnderBed(Hero.BABY, Boss.DUST_BUNNY);
            checkUnderBed(Hero.BOY, Boss.BED_MONSTER);
            checkUnderBed(Hero.SISTER, Boss.SOCK_NINJA);
            checkUnderBed(Hero.GRANNY, Boss.YARN_HYDRA);
            checkUnderBed(Hero.GRANDAD, Boss.WIND_UP_GENERAL);
            checkUnderBed(Hero.KING, Boss.LINT_REGENT);
        } finally {
            Hero.current = original;
        }
    }

    private void checkUnderBed(Hero hero, Boss expected) {
        Hero.current = hero;
        Dream trip = Dream.start(Dream.Kind.UNDER_BED);
        StoryDirector floor = floorOf(trip);
        assertEquals(Stage.UNDER_BED, floor.getStage());
        assertEquals("UNDER THE BED", floor.stageTitle());
        assertTrue(trip.lastFloor());
        assertTrue(trip.rules().isEmpty());
        assertEquals(expected, floor.getBoss());
        StageMap map = floor.getMap();
        assertEquals(expected.arenaTheme(), map.boss.theme);
        assertNotNull(map.shop);
        assertEquals("bed_quilt", map.shop.theme);
        int treasures = 0;
        int fights = 0;
        for (Room room : map.rooms) {
            if (room.type == Room.Type.TREASURE) treasures++;
            if (room.type == Room.Type.COMBAT) fights++;
            assertTrue(Files.isRegularFile(Path.of("src/main/resources/assets/tiles", room.theme + ".png")));
        }
        assertEquals(2, treasures);
        assertTrue(fights >= 7 && fights <= 9, "full two-chest floor: " + fights + " combat rooms");
    }

    @Test
    void pastDreamWinsUnlockTheEntranceForTheSave() {
        ETGFanfic game = new ETGFanfic();
        game.prefs = mock(Preferences.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(game.prefs.getFloat("bossRushFastest", 0f)).thenReturn(120f);
        when(game.prefs.getInteger("dreams", 0)).thenReturn(1);
        assertFalse(game.underBedUnlocked(), "Boss Rush alone cannot open the entrance");

        when(game.prefs.getFloat("dreamFastest", 0f)).thenReturn(180f);
        assertTrue(game.underBedUnlocked(), "the older Dream time already records bed and calendar wins");

        when(game.prefs.getFloat("dreamFastest", 0f)).thenReturn(0f);
        when(game.prefs.getInteger("dailiesDone", 0)).thenReturn(1);
        assertTrue(game.underBedUnlocked(), "a recorded Daily win qualifies too");
    }

    @Test
    void theDustBunnyGunLoadsUnderItsNewName() {
        assertEquals("Grey Goo", Weapon.byName("Dust Bunny").getName());
        assertEquals("Grey Goo", Weapon.byName("Grey Goo").getName());
    }

    @Test
    void aDreamIsFiveFloorsAndEndsWithADreamBoss() {
        Dream d = Dream.start(Dream.Kind.DREAM);
        for (int f = 1; f <= Dream.FLOORS; f++) {
            assertEquals(f, d.floor);
            StoryDirector floor = floorOf(d);
            assertEquals(f == Dream.FLOORS, d.lastFloor());
            assertEquals(f == Dream.FLOORS, floor.getBoss().isDream(), "only the last floor has a dream boss: " + floor.getBoss());
            assertNotEquals(Stage.SEA, floor.getStage(), "the sea's rooms are all water");
            assertFalse(d.rules().isEmpty(), "every floor has a twist");
            assertNull(floor.nextStage(), "a dream doesn't go on to the story's next place");
            assertTrue(floor.stageTitle().startsWith("DREAM " + f + ": "));
            for (Room r : floor.getMap().rooms) {
                assertFalse(r.slumsWay || r.seaHole, "no secret ways down in a dream");
            }
            if (f < Dream.FLOORS) d = d.next();
        }
        Array<String> bosses = d.bosses;
        for (int i = 0; i < bosses.size; i++) {
            assertEquals(i, bosses.indexOf(bosses.get(i), false), "no boss twice in one dream: " + bosses);
        }
    }

    @Test
    void dreamBossesStayInTheirDreams() {
        for (Boss b : new Boss[]{Boss.ALARM_CLOCK, Boss.NIGHTMARE, Boss.BIG_TEDDY}) {
            assertTrue(b.isDream());
            assertNull(b.stage);
            assertEquals("dreamland", b.arenaTheme());
            assertEquals("boss_dream", b.music());
            assertNotNull(b.quote());
        }
        for (int i = 0; i < 300; i++) {
            assertFalse(Boss.pickAny().isDream(), "never in endless");
        }
        assertTrue(Dream.pickDreamBoss().isDream());
    }

    @Test
    void theBossRushGoesDownItsListAndEndsInADream() {
        Dream d = Dream.start(Dream.Kind.BOSS_RUSH);
        assertTrue(d.rush.size >= 5, "village, town, city, skyscraper, the King, a dream boss: " + d.rush);
        assertEquals(d.rush.size, d.floors());
        assertTrue(Boss.valueOf(d.rush.peek()).isDream());
        assertEquals(Boss.FLUFFY_KING.name(), d.rush.get(d.rush.size - 2));
        for (int f = 1; f <= d.floors(); f++) {
            StoryDirector floor = floorOf(d);
            assertEquals(d.rush.get(f - 1), floor.getBoss().name());
            assertNotNull(floor.getMap().boss);
            assertTrue(floor.getMap().rooms.size <= 3, "a start room, the arena (and a shop)");
            assertEquals(f % 2 == 0, floor.getMap().shop != null, "Baa-llo's shop after every second boss");
            assertTrue(floor.stageTitle().startsWith("BOSS " + f + " / " + d.floors()));
            if (f < d.floors()) d = d.next();
        }
        assertTrue(d.lastFloor());
    }

    @Test
    void theBossRushGoesUnderTheSeaOnceItsBeenFound() {
        boolean was = com.trush.game.save.Unlocks.foundSea;
        try {
            com.trush.game.save.Unlocks.foundSea = true;
            Dream d = Dream.start(Dream.Kind.BOSS_RUSH);
            int shark = d.rush.indexOf(Boss.SHARK.name(), false);
            assertEquals(d.rush.indexOf(Boss.FLUFFY_KING.name(), false) - 1, shark, "the sea comes just before the King: " + d.rush);
            d.floor = shark + 1;
            StoryDirector floor = floorOf(d);
            assertEquals(Stage.SEA, floor.getStage());
            assertNull(floor.getMap().shop, "Baa-llo can't swim");
        } finally {
            com.trush.game.save.Unlocks.foundSea = was;
        }
    }

    @Test
    void theMoonGetsBaallosCloudShopInTheBossRush() {
        StageMap moon = StageMap.bossRush(Stage.MOON, false, true);
        assertNotNull(moon.shop);
        assertEquals(Stage.CLOUDS.shopTheme, moon.shop.theme);
        assertEquals(Stage.VILLAGE.shopTheme, StageMap.bossRush(Stage.VILLAGE, false, true).shop.theme);
    }

    @Test
    void todaysDreamIsTheSameAllDay() {
        Dream a = Dream.start(Dream.Kind.DAILY);
        Dream b = Dream.start(Dream.Kind.DAILY);
        assertEquals(Dream.todaySeed(), a.seed);
        for (int f = 1; f <= 3; f++) {
            StoryDirector fa = floorOf(a);
            StoryDirector fb = floorOf(b);
            assertEquals(fa.getStage(), fb.getStage());
            assertEquals(fa.getBoss(), fb.getBoss());
            assertEquals(a.rules, b.rules);
            assertEquals(fa.getMap().rooms.size, fb.getMap().rooms.size);
            a = a.next();
            b = b.next();
        }
        Dream again = a.again();
        assertEquals(1, again.floor);
        assertEquals(a.seed, again.seed, "trying again is still today's dream");
        assertEquals(0, Dream.start(Dream.Kind.DREAM).seed);
    }

    @Test
    void aDreamTravelsInTheRunSaveAndWakingUpStartsItAgain() {
        RunSave s = new RunSave();
        s.dream = Dream.start(Dream.Kind.DREAM);
        s.dream.floor = 3;
        s.dream.rules.add(Dream.Rule.GOLD_RUSH.name());
        s.dream.rules.add("A_RULE_FROM_THE_FUTURE");
        assertEquals(1, s.dream.rules().size, "an unknown rule is skipped");
        assertTrue(s.dream.has(Dream.Rule.GOLD_RUSH));
        Dream next = s.dream.next();
        assertEquals(4, next.floor);
        assertTrue(next.rules.isEmpty(), "the next floor rolls its own twists");
        assertEquals(1, s.dream.again().floor);
    }
}
