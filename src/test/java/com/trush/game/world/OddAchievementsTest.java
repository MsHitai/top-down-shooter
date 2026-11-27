package com.trush.game.world;

import com.badlogic.gdx.Preferences;
import com.trush.game.audio.Sfx;
import com.trush.game.enemy.Boss;
import com.trush.game.enemy.BulletKin;
import com.trush.game.enemy.Enemy;
import com.trush.game.gfx.Sprites;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Bullet;
import com.trush.game.objects.Meteor;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Hero;
import com.trush.game.save.Flufficon;
import com.trush.game.save.Flufficon.Achievement;
import com.trush.game.save.Unlocks;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockMakers;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class OddAchievementsTest {
    private final Arena previousArena = Arena.now;
    private final Hero previousHero = Hero.current;
    private final boolean previousFlufful = Unlocks.flufful;
    private final Map<String, String> saved = new HashMap<>();
    private Preferences prefs;
    private GameWorld world;
    private float x;
    private float y;

    @BeforeEach
    void setUp() {
        prefs = mock(Preferences.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(prefs.getString(anyString(), anyString()))
                .thenAnswer(call -> saved.getOrDefault(call.getArgument(0), call.getArgument(1)));
        when(prefs.putString(anyString(), anyString())).thenAnswer(call -> {
            saved.put(call.getArgument(0), call.getArgument(1));
            return prefs;
        });
        Flufficon.load(prefs);
        Hero.current = Hero.SISTER;
        ETGFanfic game = mock(ETGFanfic.class,
                withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
        game.prefs = prefs;
        game.sfx = mock(Sfx.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        game.sprites = mock(Sprites.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        world = new GameWorld(game, new StoryDirector(1, false));
        world.getRoom().props.clear();
        world.getRoom().walls.clear();
        x = Arena.now.centerX - 0.8f;
        world.getPlayer().setFeet(x, Arena.now.centerY);
        y = world.getPlayer().hitboxY();
    }

    @AfterEach
    void restore() {
        Arena.now = previousArena;
        Hero.current = previousHero;
        Unlocks.flufful = previousFlufful;
        saved.clear();
        Flufficon.load(prefs);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void standardIssueUsesTheFiredWeaponAcrossASwap(boolean mixedDamage) {
        Enemy king = boss(Boss.FLUFFY_KING, 5f);
        if (mixedDamage) world.cameraFlash(x, y, 0f, 1f);
        king.hp = 1f;

        assertTrue(world.getPlayer().currentWeapon().tryFire(world, x, y, 0f));
        world.getPlayer().addWeapon(Weapon.chomper());
        world.getPlayer().selectWeapon(1);
        assertEquals(Weapon.CHOMPER, world.getPlayer().currentWeapon().getName());
        resolveShots();

        assertFalse(king.alive);
        assertEquals(!mixedDamage, Achievement.STANDARD_ISSUE.unlocked());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void familyRecipeCarriesDamageAcrossThePhaseChangeAndCountsReflectedShots(boolean mixedDamage) {
        Enemy grandfluff = boss(Boss.GRANDFLUFF, 5f);
        if (mixedDamage) {
            world.cameraFlash(x, y, 0f, 30f);
        } else {
            world.swingPan(0f, 30f, 2f);
        }
        assertTrue(grandfluff.alive);
        assertTrue(grandfluff.isUntouchable());
        assertFalse(Achievement.FAMILY_RECIPE.unlocked());

        grandfluff.update(3.3f, world);
        assertFalse(grandfluff.isUntouchable());
        grandfluff.pos.set(x + 0.8f, y);
        grandfluff.hp = 1f;
        world.clearEnemyBullets(0f, 0f, -1f);
        world.spawnEnemyBullet(x + 0.3f, y, 180f, 4f);
        world.swingPan(0f, 0f, 0.4f);
        resolveShots();

        assertFalse(grandfluff.alive);
        assertEquals(!mixedDamage, Achievement.FAMILY_RECIPE.unlocked());
    }

    @Test
    void groupPhotoRequiresThreeFinishingBlowsFromOneFlashAndPersists() {
        foe(x + 0.6f, y, 1f);
        foe(x + 0.6f, y + 0.1f, 1f);
        foe(x + 0.6f, y - 0.1f, 40f);
        assertTrue(Weapon.polaroid().tryFire(world, x, y, 0f));
        assertTrue(Weapon.polaroid().tryFire(world, x, y, 0f));
        assertFalse(Achievement.GROUP_PHOTO.unlocked());

        foe(x + 0.6f, y, 1f);
        foe(x + 0.6f, y + 0.1f, 1f);
        foe(x + 0.6f, y - 0.1f, 1f);
        assertTrue(Weapon.polaroid().tryFire(world, x, y, 0f));
        assertEquals(Achievement.GROUP_PHOTO, Flufficon.nextAnnouncement());
        Flufficon.award(Achievement.GROUP_PHOTO);
        assertNull(Flufficon.nextAnnouncement());
        Flufficon.load(prefs);
        assertTrue(Achievement.GROUP_PHOTO.unlocked());
    }

    @Test
    void returnToSenderRequiresAnOutboundKillFromTheSameBanana() {
        Enemy first = foe(x + 0.6f, y, 1f);
        assertTrue(Weapon.bananaRang().tryFire(world, x, y, 0f));
        for (int frame = 0; frame < 20 && first.alive; frame++) world.updatePlayerBullets(1f / 60f);
        assertFalse(first.alive);
        assertFalse(Achievement.RETURN_TO_SENDER.unlocked());
        Bullet original = world.getPlayerBullets().first();

        assertTrue(Weapon.bananaRang().tryFire(world, x, y, 0f));
        Bullet other = world.getPlayerBullets().peek();
        Enemy second = foe(x + 0.6f, y, 1f);
        other.pos.set(x + 0.65f, y);
        other.returnBoomerang();
        world.updatePlayerBullets(1f / 60f);
        assertFalse(second.alive);
        assertFalse(Achievement.RETURN_TO_SENDER.unlocked());
        other.alive = false;

        Enemy third = foe(x + 0.6f, y, 1f);
        original.pos.set(x + 0.65f, y);
        original.returnBoomerang();
        world.updatePlayerBullets(1f / 60f);
        assertFalse(third.alive);
        assertTrue(Achievement.RETURN_TO_SENDER.unlocked());
    }

    @Test
    void moonBaitRequiresAnEnemyMeteorToDamageTwoEnemiesAndSpareTheHero() {
        Enemy first = foe(x + 0.2f, y, 200f);
        Enemy second = foe(x + 0.3f, y, 200f);
        world.getMeteors().add(Meteor.friendly(x, y, 20f, Meteor.RADIUS));
        world.updateMeteors(Meteor.WARN_TIME);
        assertFalse(Achievement.MOON_BAIT.unlocked());

        int health = world.getPlayer().getHealth();
        world.getMeteors().add(new Meteor(x, y, 0f));
        world.updateMeteors(Meteor.WARN_TIME);
        assertTrue(world.getPlayer().getHealth() < health);
        assertFalse(Achievement.MOON_BAIT.unlocked());

        world.getPlayer().setFeet(x - 1f, y);
        world.getMeteors().add(new Meteor(x, y, 0f));
        world.updateMeteors(Meteor.WARN_TIME);
        assertTrue(first.alive && second.alive);
        assertTrue(Achievement.MOON_BAIT.unlocked());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void biggerFishRequiresTheChomperFinishingBlow(boolean chompFinishes) {
        Enemy shark = boss(Boss.SHARK, 100f);
        assertTrue(Weapon.chomper().tryFire(world, x, y, 0f));
        resolveShots();
        assertTrue(shark.alive && shark.hp < 100f);
        assertFalse(Achievement.BIGGER_FISH.unlocked());

        shark.hp = 1f;
        Weapon finisher = chompFinishes ? Weapon.chomper() : Hero.SISTER.starter();
        assertTrue(finisher.tryFire(world, x, y, 0f));
        resolveShots();
        assertFalse(shark.alive);
        assertEquals(chompFinishes, Achievement.BIGGER_FISH.unlocked());
    }

    private Enemy boss(Boss type, float hp) {
        Enemy enemy = type.create(x + 0.8f, y, 0);
        enemy.pos.set(x + 0.8f, y);
        return ready(enemy, hp);
    }

    private Enemy foe(float targetX, float targetY, float hp) {
        return ready(new BulletKin(targetX, targetY), hp);
    }

    private Enemy ready(Enemy enemy, float hp) {
        enemy.update(Enemy.SPAWN_TIME, world);
        enemy.hp = hp;
        world.getEnemies().add(enemy);
        return enemy;
    }

    private void resolveShots() {
        for (int frame = 0; frame < 120 && world.getPlayerBullets().size > 0; frame++) {
            world.updatePlayerBullets(1f / 60f);
        }
    }
}
