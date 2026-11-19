package com.trush.game.world;

import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Chest;
import com.trush.game.objects.Item;
import com.trush.game.objects.Tier;
import com.trush.game.objects.Weapon;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class BedChestTest {
    @Test void everyChestContainsAGunEvenWhenTheRequestedTierOrPoolRunsOut() throws Exception {
        Arena previous = Arena.now;
        try {
            StoryDirector director = new StoryDirector(Dream.start(Dream.Kind.UNDER_BED), new Array<>(), new Array<Item>());
            Field pool = StoryDirector.class.getDeclaredField("guns");
            pool.setAccessible(true);
            @SuppressWarnings("unchecked") Array<Weapon> guns = (Array<Weapon>) pool.get(director);
            Method chest = StoryDirector.class.getDeclaredMethod("chest", GameWorld.class, Tier.class,
                    float.class, float.class, boolean.class, Boolean.class);
            chest.setAccessible(true);
            for (Tier tier : Tier.values()) {
                for (Boolean preference : new Boolean[]{null, false, true}) {
                    Chest reward = (Chest) chest.invoke(director, null, tier, 3f, 4f, true, preference);
                    assertNotNull(reward.gun, "Under the Bed always supplies a weapon, including item-preferring chests");
                    assertNull(reward.item);
                    assertEquals(reward.gun.getTier(), reward.tier);
                    assertFalse(guns.contains(reward.gun, true), "reward is removed from the floor's pool");
                }
            }
            guns.clear();
            Weapon last = Weapon.waterPistol();
            guns.add(last);
            Tier otherTier = last.getTier() == Tier.A ? Tier.B : Tier.A;
            Chest fallback = (Chest) chest.invoke(director, null, otherTier, 3f, 4f, false, false);
            assertSame(last, fallback.gun, "another tier is preferable to an item or an empty chest");
            assertEquals(last.getTier(), fallback.tier);
            Chest exhausted = (Chest) chest.invoke(director, null, Tier.S, 3f, 4f, false, null);
            assertNotNull(exhausted.gun, "even an exhausted pool still supplies a weapon");
            assertNull(exhausted.item);
        } finally {
            Arena.now = previous;
        }
    }
}
