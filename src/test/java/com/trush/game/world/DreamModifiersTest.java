package com.trush.game.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.audio.Sfx;
import com.trush.game.enemy.BulletKin;
import com.trush.game.enemy.Enemy.AttackWarning;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Bullet;
import com.trush.game.objects.Item;
import com.trush.game.objects.Weapon;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class DreamModifiersTest {
    private final Arena previous = Arena.now;

    @AfterEach void restoreArena() {
        Arena.now = previous;
    }

    @Test void softLandingOnlyClearsNearbyBulletsAndHasACooldown() {
        GameWorld world = underBed(Dream.Rule.SOFT_LANDING);
        float x = world.getPlayer().centerX();
        float y = world.getPlayer().hitboxY();
        Bullet nearby = world.spawnEnemyBullet(x + 0.3f, y, 0f, 1f);
        Bullet distant = world.spawnEnemyBullet(x + 1.3f, y, 0f, 1f);

        world.onRollEnd();

        assertFalse(nearby.alive);
        assertTrue(nearby.blanked);
        assertTrue(distant.alive);
        Bullet duringCooldown = world.spawnEnemyBullet(x, y, 0f, 1f);
        world.onRollEnd();
        assertTrue(duringCooldown.alive);
    }

    @Test void quickDrawBoostsEveryPelletOfOneWeaponAttack() {
        GameWorld world = underBed(Dream.Rule.QUICK_DRAW);
        Weapon gun = Weapon.fluffShotgun();
        float base = gun.getDamage() * world.getPlayer().damageScale();
        world.onRollEnd();

        assertTrue(gun.tryFire(world, 3f, 3f, 0f));
        assertTrue(world.getPlayerBullets().size > 1);
        for (Bullet pellet : world.getPlayerBullets()) {
            assertEquals(base * 1.5f, pellet.damage, 0.001f);
        }

        world.onRollEnd();
        assertEquals(0f, world.quickDrawDamageBonus());
    }

    @Test void crossDraftDriftsFlyingBulletsAndPreservesScriptedPaths() {
        Bullet free = new Bullet(true, 1f, 1f, 90f, 2f, 0.07f, 1f, Color.WHITE);
        Bullet scripted = new Bullet(false, 1f, 1f, 90f, 2f, 0.07f, 1f, Color.WHITE);
        scripted.followCourse(new Vector2[]{new Vector2(1f, 1f), new Vector2(1f, 5f)});

        free.update(0.5f, 0.55f);
        scripted.update(0.5f, 0.55f);

        assertEquals(1.275f, free.pos.x, 0.001f);
        assertEquals(1f, scripted.pos.x, 0.001f);
        assertEquals(free.pos.y, scripted.pos.y, 0.001f);
        assertEquals(2f, free.vel.len(), 0.001f);
    }

    @Test void slowReloadMultipliesThePlayersUsualReloadTime() {
        GameWorld world = underBed(Dream.Rule.SLOW_RELOAD);
        Weapon gun = Weapon.fluffShotgun();
        world.getPlayer().addPassive(Item.QUICK_MITTENS);
        float normal = gun.getReloadTime() * world.getPlayer().reloadScale();

        assertTrue(gun.tryFire(world, 3f, 3f, 0f));
        gun.startReload(world);
        assertEquals(normal * 1.3f, gun.getReloadTotal(), 0.001f);
        gun.update(normal);
        assertTrue(gun.isReloading());
        gun.update(normal * 0.31f);
        assertFalse(gun.isReloading());
    }

    @Test void smallBlanksOnlyClearAndPushWithinTheVisibleRadius() {
        GameWorld world = underBed(Dream.Rule.SMALL_BLANKS);
        float x = world.getPlayer().centerX();
        float y = world.getPlayer().hitboxY();
        Bullet nearby = world.spawnEnemyBullet(x + 1.8f, y, 0f, 1f);
        Bullet distant = world.spawnEnemyBullet(x + 2.2f, y, 0f, 1f);
        BulletKin nearbyEnemy = new BulletKin(x + 1.8f, y);
        BulletKin distantEnemy = new BulletKin(x + 2.2f, y);
        nearbyEnemy.update(1f, world);
        distantEnemy.update(1f, world);
        world.getEnemies().add(nearbyEnemy, distantEnemy);
        world.getPlayer().ensureBlanks(1);
        int blanks = world.getPlayer().getBlanks();

        world.useBlank();

        assertEquals(blanks - 1, world.getPlayer().getBlanks());
        assertEquals(2f, world.getBlankRadius());
        assertTrue(nearby.blanked);
        assertTrue(distant.alive);
        assertTrue(nearbyEnemy.knockSpeed() > 0f);
        assertEquals(0f, distantEnemy.knockSpeed());
        assertEquals(0f, world.getBlankCalm());
    }

    @Test void fallingDustMarksAFixedSpotBeforeHittingAndStopsOnClear() {
        GameWorld world = underBed(Dream.Rule.FALLING_DUST);
        world.getRoom().setLocked(true);
        world.getPlayer().setFeet(3f, 2f);
        int health = world.getPlayer().getHealth();
        Array<AttackWarning> warnings = new Array<>();

        world.updateFallingDust(6f);
        world.collectRoomAttackWarnings(warnings);
        assertEquals(1, warnings.size);
        assertEquals(3f, warnings.first().x());
        assertEquals(health, world.getPlayer().getHealth());

        world.getPlayer().setFeet(4.5f, 2f);
        world.updateFallingDust(1.3f);
        assertEquals(health, world.getPlayer().getHealth());

        world.updateFallingDust(6f);
        world.updateFallingDust(1.3f);
        assertEquals(health - 1, world.getPlayer().getHealth());

        world.updateFallingDust(6f);
        world.getRoom().setLocked(false);
        world.updateFallingDust(1.3f);
        warnings.clear();
        world.collectRoomAttackWarnings(warnings);
        assertEquals(0, warnings.size);
        assertEquals(health - 1, world.getPlayer().getHealth());
    }

    private GameWorld underBed(Dream.Rule rule) {
        ETGFanfic game = new ETGFanfic();
        game.sfx = mock(Sfx.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        StoryDirector director = new StoryDirector(Dream.start(Dream.Kind.UNDER_BED), new Array<>(), new Array<>());
        director.getMap().start.dreamRule = rule;
        return new GameWorld(game, director);
    }
}
