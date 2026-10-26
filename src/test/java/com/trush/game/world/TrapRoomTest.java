package com.trush.game.world;

import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Puddle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Slums' trap rooms: survive the clock; acid wells up, pipes burst across the short way. */
class TrapRoomTest {

    private static final float STEP = 0.02f;

    /** Records what the trap spills, like GameWorld would. */
    private static final class Spills implements TrapRoom.Hazards {
        final Array<Puddle> puddles = new Array<>();

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
    }

    private static void run(TrapRoom t, float seconds, Spills s, float px, float py) {
        for (float time = 0; time < seconds; time += STEP) {
            t.update(STEP, px, py, s);
        }
    }

    @Test
    void theAcidRisesUntilTheClockRunsOutThenDriesUp() {
        Arena a = new Arena(29, 8);
        TrapRoom t = new TrapRoom(TrapRoom.Kind.ACID_FLOOD, a);
        Spills s = new Spills();
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
        TrapRoom t = new TrapRoom(TrapRoom.Kind.BURST_PIPES, across);
        Spills s = new Spills();
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
        t = new TrapRoom(TrapRoom.Kind.BURST_PIPES, upDown);
        s = new Spills();
        run(t, 5f, s, upDown.centerX, upDown.centerY);
        float y = s.puddles.get(0).y;
        line = 0;
        for (Puddle p : s.puddles) {
            if (Math.abs(p.y - y) < 0.001f) line++;
        }
        assertTrue(line >= 6, "across the alley: " + line);
    }

    @Test
    void everySlumsMapHasItsTrapRooms() {
        for (int run = 0; run < 30; run++) {
            StageMap map = new StageMap(Stage.SLUMS.roomCount(), Stage.SLUMS);
            int acid = 0;
            int pipes = 0;
            int fights = 0;
            for (Room r : map.rooms) {
                if (r.type == Room.Type.COMBAT && r.trap == null) fights++;
                if (r.trap == TrapRoom.Kind.ACID_FLOOD) acid++;
                if (r.trap == TrapRoom.Kind.BURST_PIPES) pipes++;
                if (r.trap != null) {
                    assertEquals(Room.Type.COMBAT, r.type);
                    assertTrue(r.depth >= 1, "not next to the start");
                }
            }
            assertEquals(1, acid);
            assertEquals(1, pipes);
            assertTrue(fights >= 2, "most rooms are still fights");
        }
        for (Room r : new StageMap(10, Stage.CITY).rooms) {
            assertEquals(null, r.trap, "only the Slums have trap rooms");
        }
    }
}
