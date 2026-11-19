package com.trush.game.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.enemy.BedCritter;
import com.trush.game.enemy.BedHeavy;
import com.trush.game.enemy.Enemy;
import com.trush.game.objects.Item;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class BedEncountersTest {
    @Test void queuedAndPendingResidentsRespectBothRoleLimitsAndEveryFightCanFinish() {
        Arena original = Arena.now;
        int beetles = 0, heavies = 0;
        try {
            for (int run = 0; run < 24; run++) {
                StoryDirector director = new StoryDirector(Dream.start(Dream.Kind.UNDER_BED), new Array<>(), new Array<Item>());
                Room room = null;
                for (Room candidate : director.getMap().rooms) {
                    if (candidate.type == Room.Type.COMBAT && (room == null || candidate.depth > room.depth)) room = candidate;
                }
                assertTrue(room != null);
                Arena.now = room.arena;
                Array<Enemy> active = new Array<>(), pending = new Array<>();
                GameWorld world = mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
                when(world.getEnemies()).thenReturn(active);
                when(world.getPendingEnemies()).thenReturn(pending);
                when(world.playerInsideRoom(StoryDirector.FIGHT_ENTRY_MARGIN)).thenReturn(true);
                when(world.isHard()).thenReturn(run % 2 == 0);
                when(world.findSpawnPoint(anyFloat())).thenReturn(new Vector2(3f, 3f));
                when(world.findChestSpot()).thenReturn(new Vector2(4f, 4f));
                doAnswer(call -> {
                    pending.add(call.getArgument(0, Enemy.class));
                    return null;
                }).when(world).spawnEnemy(any(Enemy.class));
                director.onRoomEntered(room, world);
                for (int frame = 0; frame < 60 * 180 && !room.cleared; frame++) {
                    director.update(1f / 60f, world);
                    int pursuers = 0, formations = 0;
                    for (int list = 0; list < 2; list++) {
                        for (Enemy enemy : list == 0 ? active : pending) {
                            if (enemy instanceof BedCritter critter && critter.isPursuer()) pursuers++;
                            if (enemy instanceof BedHeavy) formations++;
                        }
                    }
                    assertTrue(pursuers <= 1, "pending spawns count against the beetle limit");
                    int formationLimit = !room.arena.isSmall() && room.depth >= 3 ? 2 : 1;
                    assertTrue(formations <= formationLimit, "two patterns only in deeper, roomy fights");
                    assertTrue(active.size + pending.size <= 4, "including hard mode");
                    // Hold pending enemies for a full second to exercise spawn reservations.
                    if (frame % 60 == 59) {
                        for (Enemy enemy : pending) {
                            if (enemy instanceof BedCritter critter && critter.isPursuer()) beetles++;
                            if (enemy instanceof BedHeavy) heavies++;
                        }
                        active.addAll(pending);
                        pending.clear();
                    }
                    if (frame % 150 == 149 && active.notEmpty()) {
                        active.first().alive = false;
                        active.removeIndex(0);
                    }
                }
                assertTrue(room.cleared, "a blocked role must not strand enemies in the queue");
            }
            assertTrue(beetles > 0 && heavies > 0, "exercise both occupied roles");
        } finally {
            Arena.now = original;
        }
    }
}
