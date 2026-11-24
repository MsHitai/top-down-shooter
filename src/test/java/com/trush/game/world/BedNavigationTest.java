package com.trush.game.world;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.audio.Sfx;
import com.trush.game.enemy.BedHeavy;
import com.trush.game.gfx.Sprites;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Bullet;
import com.trush.game.objects.Prop;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class BedNavigationTest {
    @Test void crocWalksRoundASlipperAndAimsAtTheHeroInsteadOfTurningInPlace() {
        Arena original = Arena.now;
        try {
            ETGFanfic game = mock(ETGFanfic.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
            game.sfx = mock(Sfx.class, withSettings().mockMaker(MockMakers.SUBCLASS));
            game.sprites = mock(Sprites.class, withSettings().mockMaker(MockMakers.SUBCLASS));
            TextureRegion region = mock(TextureRegion.class, withSettings().mockMaker(MockMakers.SUBCLASS));
            when(region.getRegionWidth()).thenReturn(32);
            when(region.getRegionHeight()).thenReturn(32);
            when(game.sprites.prop(anyString())).thenReturn(region);
            GameWorld world = new GameWorld(game, new StoryDirector(1, false));
            world.getRoom().props.clear();
            world.getRoom().pits.clear();
            Arena a = Arena.now;
            world.getRoom().props.add(new Prop(Prop.Kind.SLIPPER, a.centerX, a.centerY));
            world.getRoom().props.add(new Prop(Prop.Kind.SPOOL, a.centerX - .6f, a.centerY + .2f));
            world.getPlayer().setFeet(a.centerX, a.centerY - 1.5f);
            world.setView(a.centerX, a.centerY, 1f);
            BedHeavy croc = new BedHeavy(a.centerX, a.centerY + 1f, BedHeavy.Kind.PEG_CROC);
            Vector2 start = croc.pos.cpy();
            boolean hit = false;
            for (int frame = 0; frame < 900 && !hit; frame++) {
                croc.update(1f / 60f, world);
                world.pushOutOfSolids(croc.pos, croc.radius * .8f);
                for (Bullet bullet : world.getEnemyBullets()) {
                    if (!bullet.alive) continue;
                    bullet.update(1f / 60f);
                    if (!world.hasLineOfSight(bullet.prev.x, bullet.prev.y, bullet.pos.x, bullet.pos.y, bullet.radius)) {
                        bullet.alive = false;
                        continue;
                    }
                    if (bullet.delay <= 0 && bullet.pos.dst(world.getPlayer().centerX(), world.getPlayer().hitboxY())
                            < bullet.radius * bullet.hitScale + .1f) hit = true;
                }
            }
            assertTrue(start.dst(croc.pos) > .5f, "the croc must leave the blocked firing position");
            assertTrue(hit, "a stationary hero behind a slipper must not be safe forever");
        } finally {
            Arena.now = original;
        }
    }
}
