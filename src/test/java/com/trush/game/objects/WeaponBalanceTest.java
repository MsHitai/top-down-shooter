package com.trush.game.objects;

import com.badlogic.gdx.utils.Array;
import com.trush.game.audio.Sfx;
import com.trush.game.effects.Particles;
import com.trush.game.player.Player;
import com.trush.game.world.GameWorld;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WeaponBalanceTest {
    private static Bullet shot(Weapon gun) {
        GameWorld world = mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        Player player = new Player(null);
        player.addWeapon(gun);
        when(world.getPlayer()).thenReturn(player);
        when(world.sfx()).thenReturn(mock(Sfx.class, withSettings().mockMaker(MockMakers.SUBCLASS)));
        when(world.particles()).thenReturn(new Particles());
        assertTrue(gun.tryFire(world, 0, 0, 0));
        ArgumentCaptor<Bullet> shot = ArgumentCaptor.forClass(Bullet.class);
        verify(world).addPlayerBullet(shot.capture());
        return shot.getValue();
    }

    @Test
    void bananaTurnsSoonerAndCanReachTheHandAgainInUnderTwoSeconds() {
        Bullet banana = shot(Weapon.bananaRang());
        assertEquals(Bullet.Shape.BANANA, banana.shape);
        assertEquals(18f, banana.damage);
        assertEquals(2f, banana.knockback);
        float farthest = 0;
        float turnAt = 0;
        float time = 0;
        while (banana.alive && time < 2f) {
            banana.steerBoomerang(0, 0);
            if (banana.returning && turnAt == 0) turnAt = time;
            banana.update(1f / 60f);
            farthest = Math.max(farthest, banana.pos.x);
            time += 1f / 60f;
        }
        assertTrue(turnAt > 0.6f && turnAt < 0.8f, "turns in about 0.7 seconds");
        assertTrue(farthest > 3f && farthest < 3.7f, "shorter outbound path");
        assertFalse(banana.alive, "caught before two seconds");
        assertTrue(banana.life > 0, "caught rather than expired");
    }

    @Test
    void turningLetsTheBananaHitAgainButNotEveryFrameOfTheReturn() {
        Bullet banana = shot(Weapon.bananaRang());
        Object enemy = new Object();
        banana.alreadyHit.add(enemy);
        banana.returnBoomerang();
        assertFalse(banana.alreadyHit.contains(enemy, true));
        banana.alreadyHit.add(enemy);
        banana.returnBoomerang();
        assertTrue(banana.alreadyHit.contains(enemy, true));
    }

    @Test
    void popcornPuffsCanHitTheKernelTargetAndAllFlyForward() {
        Bullet kernel = shot(Weapon.popcornPopper());
        Object target = new Object();
        kernel.alreadyHit.add(target);
        Array<Bullet> puffs = kernel.splitBullets();
        assertEquals(3, puffs.size);
        for (Bullet puff : puffs) {
            assertFalse(puff.alreadyHit.contains(target, true));
            assertEquals(2.5f, puff.damage);
            assertTrue(puff.vel.x > 0);
            assertTrue(puff.vel.len() >= 6.5f);
        }
        assertTrue(puffs.first().vel.y < 0);
        assertEquals(kernel.vel.angleDeg(), puffs.get(1).vel.angleDeg(), 0.001f);
        assertTrue(puffs.peek().vel.y > 0);
        assertEquals(2.7f, kernel.vel.len() * kernel.life, 0.001f, "kernel range stays the same");
    }

    @Test
    void otherSplittingWeaponsStillExcludeAnEnemyAlreadyHit() {
        Bullet pillow = shot(Weapon.pillowCannon());
        Object target = new Object();
        pillow.alreadyHit.add(target);
        for (Bullet feather : pillow.splitBullets()) {
            assertTrue(feather.alreadyHit.contains(target, true));
            assertEquals(Bullet.Shape.FEATHER, feather.shape);
        }
    }

    @Test
    void fasterSnowballsKeepTheirColdAndTheirRange() {
        Bullet snow = shot(Weapon.snowballSlinger());
        assertEquals(7f, snow.damage);
        assertEquals(9f, snow.vel.len(), 0.001f);
        assertEquals(0.2f, snow.frost);
        assertEquals(1.6f, snow.chill);
        assertEquals(6.75f, snow.vel.len() * snow.life, 0.001f);
    }

    @Test
    void aHalfSecondFullyDrawsTheSlingButNotTheEggLobber() {
        Weapon sling = Weapon.slingshot();
        Weapon egg = Weapon.eggLobber();
        sling.holdCharge(0.5f);
        egg.holdCharge(0.5f);
        assertEquals(1f, sling.pull());
        assertTrue(egg.lobDistance() < Weapon.LOB_MAX);
        egg.holdCharge(0.3f);
        assertEquals(Weapon.LOB_MAX, egg.lobDistance());
    }
}
