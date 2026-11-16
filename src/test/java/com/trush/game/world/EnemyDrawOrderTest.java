package com.trush.game.world;

import com.trush.game.enemy.BulletKin;
import com.trush.game.enemy.CheeseWorm;
import com.trush.game.enemy.Shapeshifter;
import com.trush.game.enemy.Thundercloud;
import com.trush.game.objects.Prop;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EnemyDrawOrderTest {
    @Test
    void hoveringShapeStaysVisibleAcrossFurnitureIncludingDuringItsDeath() {
        Shapeshifter shape = new Shapeshifter(0, 0);
        for (Prop.Kind kind : new Prop.Kind[]{Prop.Kind.TABLE, Prop.Kind.CRATE, Prop.Kind.PILLAR}) {
            shape.alive = true;
            Prop prop = new Prop(kind, 0, 0);
            for (float y : new float[]{-0.5f, 0, 0.5f}) {
                shape.pos.y = y;
                assertTrue(GameWorld.compareDrawOrder(shape, prop) > 0,
                        "a hovering body must be drawn after the furniture it flies over");
                assertTrue(GameWorld.compareDrawOrder(prop, shape) < 0);
            }
            shape.alive = false;
            assertTrue(GameWorld.compareDrawOrder(shape, prop) > 0);
        }
    }

    @Test
    void groundEnemiesStillPassBehindAndInFrontOfTables() {
        Prop table = new Prop(Prop.Kind.TABLE, 0, 0);
        BulletKin kin = new BulletKin(0, 1);
        assertTrue(GameWorld.compareDrawOrder(kin, table) < 0);
        kin.pos.y = -1;
        assertTrue(GameWorld.compareDrawOrder(kin, table) > 0);
    }

    @Test
    void airLayerRetainsDepthAndDoesNotPullBurrowersAboveFurniture() {
        Shapeshifter shape = new Shapeshifter(0, 1);
        Thundercloud cloud = new Thundercloud(0, -1);
        assertTrue(GameWorld.compareDrawOrder(shape, cloud) < 0);
        assertTrue(GameWorld.compareDrawOrder(cloud, shape) > 0);
        CheeseWorm worm = new CheeseWorm(0, 1);
        assertTrue(worm.flying, "burrowing movement also ignores furniture collisions");
        assertTrue(GameWorld.compareDrawOrder(worm, new Prop(Prop.Kind.TABLE, 0, 0)) < 0);
    }
}
