package com.trush.game.world;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.trush.game.audio.Sfx;
import com.trush.game.enemy.Rat;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Puddle;
import com.trush.game.player.Hero;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/** Timed trap-room hazards and their placement on story maps. */
class TrapRoomTest {

    private static final float STEP = 0.02f;

    /** Records hazards without a graphics context. */
    private static final class RecordedHazards implements TrapRoom.Hazards {
        final Array<Puddle> puddles = new Array<>();
        final Array<Rectangle> crushAreas = new Array<>();
        final Array<Float> currents = new Array<>();

        @Override
        public Puddle spill(Puddle.Kind kind, float x, float y, float radius, float life) {
            Puddle p = new Puddle(kind, x, y, radius, life, false);
            puddles.add(p);
            return p;
        }

        @Override
        public void sound(String name, float volume) {
        }

        @Override
        public void steam(float x, float y) {
        }

        @Override
        public void crush(Rectangle sweep) {
            crushAreas.add(new Rectangle(sweep));
        }

        @Override
        public void current(float dx, float dy) {
            currents.add(dx + dy);
        }
    }

    private static Room roomWithTrap(TrapRoom.Kind kind, Arena arena) {
        Room room = new Room(0, 0, Room.Type.COMBAT);
        room.arena = arena;
        room.template = RoomTemplates.BOSS_OPEN;
        room.stage = Stage.VILLAGE;
        room.configureTrap(kind);
        room.build();
        return room;
    }

    private static void run(TrapRoom t, float seconds, RecordedHazards s, float px, float py) {
        for (float time = 0; time < seconds; time += STEP) {
            t.update(STEP, px, py, s);
        }
    }

    @Test
    void theAcidRisesUntilTheClockRunsOutThenDriesUp() {
        Arena a = new Arena(29, 8);
        TrapRoom t = new TrapRoom(roomWithTrap(TrapRoom.Kind.ACID_FLOOD, a));
        RecordedHazards s = new RecordedHazards();
        run(t, TrapRoom.DURATION / 2f, s, a.centerX, a.centerY);
        assertFalse(t.isOver());
        int half = s.puddles.size;
        assertTrue(half >= 6, "pools keep welling up: " + half);
        for (Puddle p : s.puddles) {
            assertEquals(Puddle.Kind.ACID, p.kind);
            assertTrue(a.contains(p.x, p.y, 0.2f), "on the floor");
            assertTrue(p.life > 5f, "it stays until the end");
        }
        run(t, TrapRoom.DURATION / 2f + 0.1f, s, a.centerX, a.centerY);
        assertTrue(s.puddles.size - half > half, "faster in the second half");
        assertTrue(t.isOver());
        for (Puddle p : s.puddles) {
            assertTrue(p.life <= 0.8f, "everything dries up once it's survived");
        }
        int after = s.puddles.size;
        run(t, 3f, s, a.centerX, a.centerY);
        assertEquals(after, s.puddles.size, "nothing more once it's over");
    }

    @Test
    void pipesBurstAcrossTheShortWayOfAnAlley() {
        Arena across = new Arena(29, 8); // a left-right alley: jets go up-down
        TrapRoom t = new TrapRoom(roomWithTrap(TrapRoom.Kind.BURST_PIPES, across));
        RecordedHazards s = new RecordedHazards();
        run(t, 5f, s, across.centerX, across.centerY);
        assertTrue(s.puddles.size > 0);
        float x = s.puddles.get(0).x;
        int line = 0;
        for (Puddle p : s.puddles) {
            assertEquals(Puddle.Kind.FIRE, p.kind);
            if (Math.abs(p.x - x) < 0.001f) line++;
        }
        assertTrue(line >= 6, "a whole line of flame from wall to wall: " + line);

        Arena upDown = new Arena(11, 20); // an up-down alley: jets go left-right
        t = new TrapRoom(roomWithTrap(TrapRoom.Kind.BURST_PIPES, upDown));
        s = new RecordedHazards();
        run(t, 5f, s, upDown.centerX, upDown.centerY);
        float y = s.puddles.get(0).y;
        line = 0;
        for (Puddle p : s.puddles) {
            if (Math.abs(p.y - y) < 0.001f) line++;
        }
        assertTrue(line >= 6, "across the alley: " + line);
    }

    @Test
    void theCartRattlesBeforeItMovesAndThenRunsBothWays() {
        Arena a = new Arena(29, 8);
        TrapRoom t = new TrapRoom(roomWithTrap(TrapRoom.Kind.GARBAGE_CART, a));
        RecordedHazards s = new RecordedHazards();
        run(t, 1.6f, s, a.centerX, a.centerY);
        assertTrue(t.cartVisible());
        assertTrue(s.crushAreas.isEmpty(), "the cart telegraphs before it can hurt");
        run(t, 10f, s, a.centerX, a.centerY);
        assertTrue(s.crushAreas.first().x < a.centerX);
        boolean movingBack = false;
        for (int i = 1; i < s.crushAreas.size; i++) {
            Rectangle sweep = s.crushAreas.get(i);
            if (sweep.x < s.crushAreas.get(i - 1).x && sweep.contains(a.centerX, a.centerY)) movingBack = true;
            assertFalse(sweep.contains(a.centerX, a.bottom + 0.2f), "room to dodge along the side");
        }
        assertTrue(movingBack, "a return pass crosses the same room");
    }

    @Test
    void theBackflowReversesAndLeavesDryFloorOnTheSides() {
        Arena a = new Arena(11, 20);
        TrapRoom t = new TrapRoom(roomWithTrap(TrapRoom.Kind.SEWER_BACKFLOW, a));
        RecordedHazards s = new RecordedHazards();
        run(t, 1.6f, s, a.centerX, a.centerY);
        assertTrue(s.currents.isEmpty());
        assertTrue(t.inCurrent(a.centerX, a.centerY));
        assertFalse(t.inCurrent(a.left + 0.2f, a.centerY));
        Rectangle island = TrapRoom.currentIslands(a)[0];
        assertFalse(t.inCurrent(island.x + 0.3f, island.y + 0.3f));
        run(t, 8f, s, a.centerX, a.centerY);
        assertTrue(s.currents.first() > 0f);
        boolean reversed = false;
        for (float flow : s.currents) reversed |= flow < 0f;
        assertTrue(reversed);
        run(t, TrapRoom.DURATION, s, a.centerX, a.centerY);
        int count = s.currents.size;
        run(t, 5f, s, a.centerX, a.centerY);
        assertEquals(count, s.currents.size);
    }

    @Test
    void theTrashPressWarnsThenSweepsFromBothWallsWithClearEscapeBays() {
        Room room = roomWithTrap(TrapRoom.Kind.TRASH_PRESS, new Arena(29, 8));
        Arena a = room.arena;
        assertTrue(a.isSmall());
        assertTrue(room.props.isEmpty());
        assertTrue(room.pits.isEmpty());
        TrapRoom t = new TrapRoom(room);
        RecordedHazards hazards = new RecordedHazards();
        run(t, 2f, hazards, a.centerX, a.centerY);
        assertTrue(hazards.crushAreas.isEmpty());
        run(t, 8f, hazards, a.centerX, a.centerY);
        assertEquals(a.bottom, hazards.crushAreas.first().y);
        boolean coversCentre = false;
        boolean fromNorth = false;
        for (Rectangle area : hazards.crushAreas) {
            coversCentre |= area.contains(a.centerX, a.centerY);
            fromNorth |= area.y > a.centerY && area.y + area.height >= a.top - 0.001f;
            assertFalse(area.contains(a.left + 0.3f, a.centerY));
            assertFalse(area.contains(a.right - 0.3f, a.centerY));
        }
        assertTrue(coversCentre);
        assertTrue(fromNorth);
        run(t, TrapRoom.DURATION, hazards, a.centerX, a.centerY);
        int count = hazards.crushAreas.size;
        assertTrue(t.isOver());
        run(t, 4f, hazards, a.centerX, a.centerY);
        assertEquals(count, hazards.crushAreas.size);
    }

    @Test
    void jammedManholesWarnOpenInSequenceAndCloseAgain() {
        Arena a = new Arena(29, 8);
        Room room = roomWithTrap(TrapRoom.Kind.JAMMED_MANHOLES, a);
        TrapRoom t = new TrapRoom(room);
        RecordedHazards hazards = new RecordedHazards();
        Rectangle first = room.trapHatches.first();
        Rectangle second = room.trapHatches.get(1);
        Rectangle last = room.trapHatches.peek();
        Rectangle staticPit = new Rectangle(0f, 0f, 1f, 1f);
        room.pits.add(staticPit);
        run(t, 1.55f, hazards, first.x, first.y);
        assertTrue(t.lidHop(first) > 0f);
        assertEquals(0f, t.lidHop(second));
        assertFalse(room.pits.contains(first, true));
        run(t, 0.75f, hazards, last.x, last.y);
        assertTrue(room.pits.contains(first, true), "opens even with the player elsewhere");
        assertFalse(room.pits.contains(second, true));
        run(t, 1.5f, hazards, last.x, last.y);
        assertFalse(room.pits.contains(first, true));
        assertTrue(room.pits.contains(second, true));
        boolean reachedFarEnd = false;
        boolean returned = false;
        for (int i = 0; i < 600; i++) {
            t.update(STEP, a.centerX, a.centerY, hazards);
            reachedFarEnd |= room.pits.contains(last, true);
            returned |= reachedFarEnd && room.pits.contains(first, true);
        }
        assertTrue(returned);
        run(t, TrapRoom.DURATION, hazards, a.centerX, a.centerY);
        assertTrue(t.isOver());
        assertEquals(1, room.pits.size);
        assertSame(staticPit, room.pits.first());
    }

    @Test
    void steppingOnTheLidDropsTheHeroAndNearbyGroundedEnemies() {
        Hero previous = Hero.current;
        Hero.current = Hero.BOY;
        try {
            ETGFanfic game = mock(ETGFanfic.class,
                    withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
            game.sfx = mock(Sfx.class, withSettings().mockMaker(MockMakers.SUBCLASS));
            GameWorld world = new GameWorld(game, new StoryDirector(1, false));
            Room room = world.getRoom();
            room.props.clear();
            Arena a = room.arena;
            Rectangle hatch = new Rectangle(a.centerX - 0.5f, a.centerY - 0.5f, 1f, 1f);
            room.trapHatches.add(hatch);
            room.configureTrap(TrapRoom.Kind.MANHOLES);
            world.setTrap(new TrapRoom(room));
            Rat near = new Rat(a.centerX + 0.58f, a.centerY);
            Rat far = new Rat(a.centerX + 1.4f, a.centerY);
            Rat flyer = new Rat(a.centerX + 0.2f, a.centerY);
            for (Rat rat : new Rat[]{near, far, flyer}) {
                rat.update(1f, world); // finish spawning
                world.getEnemies().add(rat);
            }
            flyer.flying = true;
            world.getPlayer().setFeet(a.centerX - 1f, a.centerY);
            world.updatePlayerPits();
            assertTrue(room.pits.isEmpty());
            assertFalse(world.getPlayer().isFalling());
            world.getPlayer().setFeet(a.centerX, a.centerY);
            world.updatePlayerPits();
            assertSame(hatch, GameWorld.pitUnderFeet(world.getPlayer(), room, new Rectangle(), new Rectangle()));
            assertTrue(world.getPlayer().isFalling());
            assertTrue(near.fellInPit);
            assertFalse(near.alive);
            assertTrue(far.alive);
            assertTrue(flyer.alive);
        } finally {
            Hero.current = previous;
        }
    }

    @Test
    void theSlumsDrawTwoDistinctTrapsFromTheWholePool() {
        EnumSet<TrapRoom.Kind> seen = EnumSet.noneOf(TrapRoom.Kind.class);
        for (int run = 0; run < 500; run++) {
            StageMap map = new StageMap(Stage.SLUMS.roomCount(), Stage.SLUMS);
            EnumSet<TrapRoom.Kind> kinds = EnumSet.noneOf(TrapRoom.Kind.class);
            int fights = 0;
            for (Room r : map.rooms) {
                if (r.type == Room.Type.COMBAT && r.trap == null) fights++;
                if (r.trap != null) {
                    assertTrue(kinds.add(r.trap), "a trap never repeats on the same map");
                    assertEquals(Room.Type.COMBAT, r.type);
                    assertTrue(r.depth >= 1, "not next to the start");
                    if (r.trap == TrapRoom.Kind.MANHOLES || r.trap == TrapRoom.Kind.JAMMED_MANHOLES) {
                        r.build();
                        assertTrue(r.trapHatches.notEmpty());
                        for (Rectangle hatch : r.trapHatches) {
                            assertFalse(r.blocksADoor(hatch));
                            for (Rectangle pit : r.pits) assertFalse(pit.overlaps(hatch));
                        }
                    }
                }
            }
            assertEquals(2, kinds.size());
            seen.addAll(kinds);
            assertTrue(fights >= 2, "most rooms are still fights");
        }
        assertEquals(EnumSet.allOf(TrapRoom.Kind.class), seen);
    }

    @Test
    void aSingleTrapCanRarelyAppearBeforeTheSkyscraper() {
        for (Stage stage : Stage.values()) {
            if (stage == Stage.SLUMS) continue;
            int withTrap = 0;
            for (int i = 0; i < 400; i++) {
                int traps = 0;
                for (Room room : new StageMap(10, stage).rooms) {
                    if (room.trap == null) continue;
                    traps++;
                    assertEquals(Room.Type.COMBAT, room.type);
                    assertFalse(room.slumsWay || room.arena.isShaped());
                }
                assertTrue(traps <= 1);
                withTrap += traps;
            }
            switch (stage) {
                case VILLAGE, TOWN, CITY, UNDERCITY -> assertTrue(withTrap > 15 && withTrap < 75, stage + ": " + withTrap);
                default -> assertEquals(0, withTrap, stage.toString());
            }
        }
    }
}
