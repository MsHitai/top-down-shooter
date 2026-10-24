package com.trush.game.objects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoinsTest {

    @Test
    void enemiesDropAboutAScoreTwentiethOfACoin() {
        assertEquals(0, Coins.forEnemy(10, 0.6f), "a bullet kin: half the time nothing");
        assertEquals(1, Coins.forEnemy(10, 0.4f));
        assertEquals(1, Coins.forEnemy(25, 0.5f), "a shotgun kin: 1.25 on average");
        assertEquals(2, Coins.forEnemy(25, 0.1f));
        assertEquals(2, Coins.forEnemy(40, 0.99f), "a bulletmancer: always two");
        assertEquals(0, Coins.forEnemy(5, 0.3f), "a rat: a quarter of the time");
        float total = 0;
        for (int i = 0; i < 1000; i++) {
            total += Coins.forEnemy(30, (i + 0.5f) / 1000f);
        }
        assertEquals(1.5f, total / 1000f, 0.01f);
    }

    @Test
    void roomsBossesAndWaves() {
        assertEquals(3, Coins.forRoom(1, 0));
        assertEquals(8, Coins.forRoom(6, 2));
        assertEquals(25, Coins.forBoss(1));
        assertEquals(45, Coins.forBoss(5));
        assertEquals(2, Coins.forWave(1));
        assertEquals(5, Coins.forWave(9));
        assertEquals(3, Coins.forChest(Tier.D, 0));
        assertEquals(11, Coins.forChest(Tier.A, 2));
    }

    @Test
    void bigAmountsComeMostlyAsBigCoins() {
        for (int total = 0; total < 80; total++) {
            int big = Coins.bigCoins(total);
            int small = total - big * Coins.BIG;
            assertTrue(small >= 0, "never more than the total");
            assertTrue(total < 8 ? big == 0 : small >= 3 && small <= 7, total + " -> " + big + " big, " + small + " small");
        }
        assertEquals(4, Coins.bigCoins(25));
    }
}
