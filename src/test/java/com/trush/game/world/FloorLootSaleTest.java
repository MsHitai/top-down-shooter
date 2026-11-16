package com.trush.game.world;

import com.trush.game.audio.Sfx;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Item;
import com.trush.game.objects.Pickup;
import com.trush.game.objects.Shop;
import com.trush.game.objects.Weapon;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class FloorLootSaleTest {

    @Test
    void theCounterBuysLocalAndRemoteLootOnePickupAtATime() {
        ETGFanfic game = mock(ETGFanfic.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        game.sfx = mock(Sfx.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        StoryDirector story = new StoryDirector(1, false);
        GameWorld world = new GameWorld(game, story);
        Shop shop = new Shop(1, false);
        world.openShop(shop);
        world.getRoom().chests.clear();
        world.getPlayer().setFeet(shop.counter.cx, shop.counter.solid.y - 0.14f);
        Room visited = story.getMap().shop;
        visited.visited = true;
        Room unvisited = story.getMap().boss;

        Pickup coin = Pickup.coin(0f, 0f, 5);
        world.getPickups().add(coin);
        Pickup heart = new Pickup(Pickup.Type.HALF_HEART, 0f, 0f);
        world.getPickups().add(heart);
        Pickup[] remoteLoot = {
                new Pickup(Pickup.Type.AMMO, 0f, 0f),
                new Pickup(Pickup.Type.SPREAD_AMMO, 0f, 0f),
                new Pickup(Pickup.Type.KEY, 0f, 0f),
                new Pickup(Pickup.Type.BLANK, 0f, 0f),
                new Pickup(Item.BIKE_HELMET, 0f, 0f),
                new Pickup(Pickup.Type.GUN, 0f, 0f, Weapon.fluffShotgun())
        };
        visited.pickups.addAll(remoteLoot);
        Pickup unseenHeart = new Pickup(Pickup.Type.HEART, 0f, 0f);
        unvisited.pickups.add(unseenHeart);

        int coins = world.getPlayer().getCoins();
        world.interact();
        assertEquals(coins + 4, world.getPlayer().getCoins());
        coins += 4;
        assertFalse(heart.alive);
        assertFalse(world.getPickups().contains(heart, true));
        for (Pickup pickup : remoteLoot) {
            world.interact();
            coins += Shop.sellPrice(pickup, false);
            assertEquals(coins, world.getPlayer().getCoins());
            assertFalse(pickup.alive);
            assertFalse(visited.pickups.contains(pickup, true));
        }
        world.interact();
        assertEquals(coins, world.getPlayer().getCoins());
        assertTrue(coin.alive);
        assertTrue(unseenHeart.alive);
    }
}
