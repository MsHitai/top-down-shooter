package com.trush.game.objects;

import com.badlogic.gdx.utils.Array;
import com.trush.game.audio.Sfx;
import com.trush.game.effects.Particles;
import com.trush.game.player.Hero;
import com.trush.game.player.Player;
import com.trush.game.world.GameWorld;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockMakers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SynergyTest {
    private static <T> T stub(Class<T> type) {
        return mock(type, withSettings().mockMaker(MockMakers.SUBCLASS));
    }

    private static GameWorld world(Player player) {
        GameWorld world = stub(GameWorld.class);
        when(world.getPlayer()).thenReturn(player);
        when(world.sfx()).thenReturn(stub(Sfx.class));
        when(world.particles()).thenReturn(new Particles());
        return world;
    }

    private static List<Bullet> shoot(GameWorld world, Weapon gun, int count) {
        assertTrue(gun.tryFire(world, 1f, 1f, 90f));
        ArgumentCaptor<Bullet> shots = ArgumentCaptor.forClass(Bullet.class);
        verify(world, times(count)).addPlayerBullet(shots.capture());
        return shots.getAllValues();
    }

    @Test
    void bananaSplitUsesOneAmmoAndBothBananasKeepFullDamage() {
        Player player = new Player(null);
        Weapon banana = Weapon.bananaRang();
        player.addWeapon(banana);
        GameWorld world = world(player);
        shoot(world, banana, 1);
        clearInvocations(world);
        banana.update(1f);
        player.addPassive(Item.BOUNCY_SOCKS);
        int ammo = banana.getAmmo();
        List<Bullet> shots = shoot(world, banana, 2);
        assertEquals(ammo - 1, banana.getAmmo());
        for (Bullet shot : shots) {
            assertEquals(banana.getDamage(), shot.damage, 0.001f);
            assertTrue(shot.boomerang);
            assertEquals(Synergy.BANANA_SPLIT.color, shot.color);
        }
        assertTrue(shots.get(0).vel.x * shots.get(1).vel.x < 0, "two distinct paths");
    }

    @Test
    void bananaSplitCombinesWithTripleScoop() {
        Player player = new Player(null);
        Weapon banana = Weapon.bananaRang();
        player.addWeapon(banana);
        player.addPassive(Item.BOUNCY_SOCKS);
        player.addPassive(Item.TRIPLE_SCOOP);
        List<Bullet> shots = shoot(world(player), banana, 6);
        assertEquals(banana.getDamage(), shots.get(0).damage, 0.001f);
        assertEquals(banana.getDamage() / 2f, shots.get(1).damage, 0.001f);
        assertEquals(banana.getDamage(), shots.get(3).damage, 0.001f);
    }

    @Test
    void waterAndRayWorkTogetherWhileTheOtherGunIsInTheBag() {
        Player player = new Player(null);
        Weapon water = Weapon.waterPistol();
        Weapon ray = Weapon.freezeRay();
        player.addWeapon(water);
        assertNull(player.synergyFor(water));
        player.addWeapon(ray);
        GameWorld world = world(player);
        Bullet shot = shoot(world, water, 1).getFirst();
        assertEquals(0.22f, shot.frost, 0.001f);
        assertTrue(shot.wet);
        assertTrue(ray.tryFire(world, 1, 1, 90));
        verify(world).fireBeam(eq(1f), eq(1f), eq(90f), eq(ray.getDamage()), eq(ray.getBeamRange()),
                eq(ray.getBeamWidth()), eq(Synergy.ICY_WATER.color), eq(ray.isPiercing()), eq(ray.getFrost() * 1.5f));
        assertNull(player.synergyFor(player.getWeapons().first()), "starter is unaffected");
    }

    @Test
    void bigFishDoesNotAccumulateOrChangeTheSavedGunStats() {
        Player player = new Player(null);
        Weapon shark = Weapon.chomper();
        player.addWeapon(shark);
        player.addPassive(Item.FLIPPERS);
        GameWorld world = world(player);
        Bullet first = shoot(world, shark, 1).getFirst();
        assertEquals(shark.getDamage() * 1.35f, first.damage, 0.001f);
        assertEquals(shark.getBulletRadius() * 1.5f, first.radius, 0.001f);
        clearInvocations(world);
        shark.update(10f);
        Bullet second = shoot(world, shark, 1).getFirst();
        assertEquals(first.damage, second.damage);
        assertEquals(60f, shark.getDamage());
        assertEquals(0.12f, shark.getBulletRadius());
        player.getPassives().removeValue(Item.FLIPPERS, true);
        assertNull(player.synergyFor(shark));
    }

    @Test
    void hotSauceStacksItsNormalDamageBonusWithTheFlareSynergy() {
        Player player = new Player(null);
        Weapon flare = Weapon.flareGun();
        player.addWeapon(flare);
        player.addPassive(Item.HOT_SAUCE);
        Bullet shot = shoot(world(player), flare, 1).getFirst();
        assertEquals(flare.getDamage() * player.damageScale() * 1.25f, shot.damage, 0.001f);
        assertEquals(flare.getBurn() * 1.5f, shot.burn, 0.001f);
        assertEquals(flare.getPuddleRadius() * 1.5f, shot.puddleRadius, 0.001f);
    }

    @Test
    void liveWireAddsTwoLightningJumps() {
        Player player = new Player(null);
        Weapon cables = Weapon.jumperCables();
        player.addWeapon(cables);
        player.addPassive(Item.STATIC_FUZZ);
        Bullet shot = shoot(world(player), cables, 1).getFirst();
        assertEquals(cables.getChain() + 2, shot.chain);
        assertEquals(cables.getDamage() * 1.2f, shot.damage, 0.001f);
    }

    @Test
    void onlyGrannysKittyCannonCallsTheCat() {
        for (Hero hero : new Hero[]{Hero.BOY, Hero.GRANNY}) {
            Player player = new Player(null, hero);
            Weapon kitty = Weapon.kittyCannon();
            player.addWeapon(kitty);
            GameWorld world = world(player);
            shoot(world, kitty, 1);
            verify(world, times(hero == Hero.GRANNY ? 1 : 0)).callCatPounce();
            assertEquals(hero == Hero.GRANNY, Synergy.CAT_LADY.active(player));
        }
    }

    @Test
    void restoringInventoryRebuildsTheRecipeWithoutExtraSaveFields() {
        Player restored = new Player(null);
        Weapon shark = Weapon.byName("Chomper");
        shark.apply(Weapon.Upgrade.DAMAGE);
        shark.restoreAmmo(1, 19);
        restored.restore(6, 6, 2, 0, new Array<>(new Weapon[]{shark}), 0);
        restored.restoreItems(new Array<>(new Item[]{Item.FLIPPERS}), null, null);
        assertEquals(Synergy.BIG_FISH, restored.synergyFor(shark));
        Bullet shot = shoot(world(restored), shark, 1).getFirst();
        assertEquals(60f * 1.15f * 1.35f, shot.damage, 0.001f);
        assertEquals(18, shark.getAmmo());
    }
}
