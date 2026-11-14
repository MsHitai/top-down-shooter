package com.trush.game.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Rectangle;
import com.trush.game.audio.Sfx;
import com.trush.game.gfx.Sprites;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Bullet;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BananaCollisionTest {
    @Test
    void wallTurnsTheBananaBackAndReturningCoverDoesNotDestroyIt() {
        ETGFanfic game = mock(ETGFanfic.class,
                withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
        game.sfx = mock(Sfx.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        game.sprites = mock(Sprites.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        GameWorld world = new GameWorld(game, new StoryDirector(1, false));
        world.getRoom().props.clear();
        Arena arena = world.getRoom().arena;
        float x = arena.centerX;
        float y = arena.centerY;
        world.getPlayer().setFeet(x - 1f, y);
        world.getRoom().walls.add(new Rectangle(x + 0.12f, y - 0.5f, 0.2f, 1f));
        Bullet banana = new Bullet(true, x, y, 0, 10f, 0.16f, 18f, Color.YELLOW);
        banana.boomerang = true;
        banana.piercing = true;
        world.addPlayerBullet(banana);
        world.updatePlayerBullets(1f / 60f);
        assertTrue(banana.alive);
        assertTrue(banana.returning);
        assertTrue(world.getPlayerBullets().contains(banana, true));
        // Put cover on the way home as well: returning bananas must reach the hand.
        world.getRoom().walls.add(new Rectangle(x - 0.4f, y - 0.5f, 0.2f, 1f));
        for (int i = 0; i < 30 && banana.alive; i++) world.updatePlayerBullets(1f / 60f);
        assertFalse(banana.alive);
        assertTrue(banana.pos.x < x - 0.5f, "crossed the cover on its way to the hero");
        assertFalse(world.getPlayerBullets().contains(banana, true));
    }
}
