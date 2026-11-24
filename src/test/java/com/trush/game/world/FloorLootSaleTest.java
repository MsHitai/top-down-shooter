package com.trush.game.world;

import com.badlogic.gdx.utils.Array;
import com.trush.game.audio.Sfx;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Item;
import com.trush.game.objects.Pickup;
import com.trush.game.objects.Shop;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Hero;
import com.trush.game.player.Player;
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
        assertTrue(world.takeShopMenuRequest());
        assertEquals(coins, world.getPlayer().getCoins());
        assertTrue(heart.alive);

        assertTrue(world.sell(saleNamed(world, "Ammo")));
        coins += Shop.sellPrice(remoteLoot[0], false);
        assertEquals(coins, world.getPlayer().getCoins());
        assertFalse(remoteLoot[0].alive);
        assertFalse(visited.pickups.contains(remoteLoot[0], true));
        assertTrue(heart.alive, "selecting a remote pickup leaves the first pickup alone");

        GameWorld.Sale local = saleNamed(world, heart.title());
        assertTrue(world.sell(local));
        coins += Shop.sellPrice(heart, false);
        assertFalse(world.sell(local), "a sold pickup cannot pay out twice");
        assertEquals(coins, world.getPlayer().getCoins());
        assertFalse(heart.alive);
        assertFalse(world.getPickups().contains(heart, true));
        assertTrue(coin.alive);
        assertTrue(unseenHeart.alive);
        for (GameWorld.Sale sale : world.shopSales()) {
            assertFalse(sale.title().equals("Coin") || sale.title().equals("Heart"));
        }
    }

    @Test
    void sellingAWeaponOrItemUpdatesTheCarriedLoadout() {
        ETGFanfic game = mock(ETGFanfic.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        game.sfx = mock(Sfx.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        GameWorld world = new GameWorld(game, new StoryDirector(1, false));
        Shop shop = new Shop(1, false);
        world.openShop(shop);
        Player player = world.getPlayer();
        player.setFeet(shop.counter.cx, shop.counter.solid.y - 0.14f);

        Weapon starter = player.currentWeapon();
        Weapon extra = Weapon.byName("Pocket Uzi");
        int capacity = extra.getMaxAmmo();
        int magazine = extra.getMagSize();
        player.addWeapon(extra);
        assertFalse(world.shopSales().select(sale -> sale.title().equals(starter.displayName())).iterator().hasNext());
        assertFalse(player.removeWeapon(starter));
        player.addPassive(Item.BIGGER_CLIPS);
        player.addPassive(Item.BOTTOMLESS_BAG);
        player.takeActive(Item.GRENADE);
        player.addPassive(Item.BACKPACK);
        player.takeActive(Item.PACKED_LUNCH);

        assertTrue(world.sell(saleNamed(world, Item.BOTTOMLESS_BAG.title)));
        assertEquals(capacity, extra.getMaxAmmo());
        assertEquals(capacity, extra.getAmmo());
        assertTrue(world.sell(saleNamed(world, Item.BIGGER_CLIPS.title)));
        assertEquals(magazine, extra.getMagSize());
        assertTrue(world.sell(saleNamed(world, Item.PACKED_LUNCH.title)));
        assertEquals(Item.GRENADE, player.getActive());
        assertTrue(world.sell(saleNamed(world, extra.displayName())));
        assertEquals(starter, player.currentWeapon());
        assertFalse(player.removeWeapon(starter));
    }

    @Test
    void reloadedStarterIsProtectedEvenWithAnotherGunEquipped() {
        for (Hero hero : Hero.values()) {
            Player player = new Player(null, hero);
            Weapon restoredStarter = Weapon.byName(player.currentWeapon().getName());
            Weapon other = Weapon.byName("Pocket Uzi");
            player.restore(hero.halfHearts, hero.halfHearts, 2, 0, Array.with(restoredStarter, other), 1);

            assertFalse(player.removeWeapon(restoredStarter), hero.title);
            assertTrue(player.removeWeapon(other), hero.title);
            assertEquals(restoredStarter, player.currentWeapon());
        }
    }

    @Test
    void sellingTheBackpackDropsItsContentsWithoutSellingThem() {
        ETGFanfic game = mock(ETGFanfic.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        game.sfx = mock(Sfx.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        GameWorld world = new GameWorld(game, new StoryDirector(1, false));
        Shop shop = new Shop(1, false);
        world.openShop(shop);
        Player player = world.getPlayer();
        player.setFeet(shop.counter.cx, shop.counter.solid.y - 0.14f);
        player.takeActive(Item.GRENADE);
        player.addPassive(Item.BACKPACK);
        player.takeActive(Item.PACKED_LUNCH);

        int coins = player.getCoins();
        GameWorld.Sale backpack = saleNamed(world, Item.BACKPACK.title);
        assertTrue(world.sell(backpack));
        assertEquals(coins + backpack.price(), player.getCoins());
        assertEquals(Item.PACKED_LUNCH, player.getActive());
        assertFalse(player.hasItem(Item.GRENADE));
        assertEquals("ON SHOP FLOOR", saleNamed(world, Item.GRENADE.title).source());
    }

    private static GameWorld.Sale saleNamed(GameWorld world, String name) {
        for (GameWorld.Sale sale : world.shopSales()) {
            if (sale.title().equals(name)) return sale;
        }
        throw new AssertionError("No sale for " + name);
    }
}
