package com.trush.game.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.audio.Sfx;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Bullet;
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

    private GameWorld underBed(Dream.Rule rule) {
        ETGFanfic game = new ETGFanfic();
        game.sfx = mock(Sfx.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        StoryDirector director = new StoryDirector(Dream.start(Dream.Kind.UNDER_BED), new Array<>(), new Array<>());
        director.getMap().start.dreamRule = rule;
        return new GameWorld(game, director);
    }
}
