package com.trush.game.enemy;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.world.GameWorld;
import com.trush.game.world.Stage;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

/** The Slums' bosses: the Blue Blob (splits at 75% and 50%; only the last blob ends the fight) and the Alley Cat. */
class SlumsBossesTest {

    private static final Vector2 SHOT = new Vector2(1, 0);

    /** A stand-in world that just collects the blobs it's asked to spawn. */
    private static GameWorld world(Array<BlueBlob> spawned) {
        GameWorld w = mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
        doAnswer(inv -> {
            spawned.add(inv.getArgument(0));
            return null;
        }).when(w).spawnEnemy(any());
        return w;
    }

    private static BlueBlob ready(BlueBlob b) {
        b.spawnTimer = 0f;
        return b;
    }

    @Test
    void itSplitsInTwoThenFourAndOnlyTheLastPopEndsTheFight() {
        Array<BlueBlob> blobs = new Array<>();
        GameWorld w = world(blobs);
        BlueBlob big = ready(new BlueBlob(5, 5, 0));
        blobs.add(big);
        float max = big.maxHp;
        assertEquals(BlueBlob.BIG, big.size());
        assertTrue(big.boss);

        big.hit(max * 0.2f, SHOT, 0, w);
        assertEquals(1, blobs.size, "not yet");
        big.hit(max * 0.06f, SHOT, 0, w); // down past 75%
        assertEquals(2, blobs.size, "split in two");
        assertEquals(2, big.size());
        assertEquals(2, blobs.get(1).size());
        assertEquals(big.hp, blobs.get(1).hp, 0.01f, "the health shared between them");
        assertEquals(0.74f, big.bossBarFraction(), 0.01f, "the bar shows them both");
        assertFalse(big.isEnraged());
        assertSame(big.bossFight(), blobs.get(1).bossFight(), "one fight, one introduction card");

        while (blobs.size < 4) { // whittle the halves down past 50%
            for (int i = 0; i < blobs.size && blobs.size < 4; i++) {
                ready(blobs.get(i)).hit(max * 0.02f, SHOT, 0, w);
            }
        }
        assertTrue(big.isEnraged(), "in pieces: angry");
        for (int i = 0; i < 6 && blobs.size < 6; i++) { // the halves split once more each
            for (int j = 0; j < blobs.size; j++) {
                BlueBlob b = ready(blobs.get(j));
                if (b.size() == 2) b.hit(max * 0.02f, SHOT, 0, w);
            }
        }
        for (BlueBlob b : blobs) {
            assertEquals(1, b.size(), "four little ones, no more splitting: " + blobs.size);
        }
        assertEquals(4, blobs.size);

        for (int i = 0; i < 3; i++) { // three pop as plain splats
            BlueBlob b = blobs.get(i);
            b.hit(b.hp + 1f, SHOT, 0, w);
            assertFalse(b.alive);
            assertFalse(b.boss, "not the last: no boss send-off");
        }
        BlueBlob last = blobs.get(3);
        last.hit(last.hp + 1f, SHOT, 0, w);
        assertFalse(last.alive);
        assertTrue(last.boss, "the last one ends the fight like a boss");
        assertEquals(0f, last.bossBarFraction(), 0.001f);
    }

    @Test
    void theAlleyCatsGoonsDropTheSofaAndJoinIn() {
        for (int tier = 0; tier <= 1; tier++) {
            Array<Enemy> spawned = new Array<>();
            GameWorld w = mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
            doAnswer(inv -> {
                spawned.add(inv.getArgument(0));
                return null;
            }).when(w).spawnEnemy(any());
            org.mockito.Mockito.when(w.getDifficulty()).thenReturn(1f);
            org.mockito.Mockito.when(w.getBlankRing()).thenReturn(-1f);
            AlleyCat cat = new AlleyCat(5, 5, tier);
            cat.spawnTimer = 0f;
            assertEquals("THE ALLEY CAT", cat.bossName());
            cat.behave(0.016f, w);
            assertFalse(cat.isEnraged(), "lounging on his sofa");
            assertEquals(0, spawned.size);
            cat.hit(cat.maxHp * (1f - AlleyCat.OFF_THE_SOFA) + 1f, SHOT, 0, w);
            cat.behave(0.016f, w);
            assertTrue(cat.isEnraged(), "off the sofa");
            assertEquals(tier >= 1 ? 3 : 2, spawned.size, "his goons join in");
            for (Enemy e : spawned) {
                assertTrue(e instanceof Robber);
            }
            cat.behave(0.016f, w);
            assertEquals(tier >= 1 ? 3 : 2, spawned.size, "only once");
        }
    }

    @Test
    void itIsTheSlumsBossAndStaysOutOfEndless() {
        assertEquals(Stage.SLUMS, Boss.BLUE_BLOB.stage);
        assertEquals("boss_slums", Boss.BLUE_BLOB.music());
        for (int i = 0; i < 300; i++) {
            assertTrue(Boss.pickAny().stage != Stage.SLUMS, "the Slums' bosses stay secret");
        }
        assertTrue(Boss.BLUE_BLOB.create(0, 0, 0) instanceof BlueBlob);
    }
}
