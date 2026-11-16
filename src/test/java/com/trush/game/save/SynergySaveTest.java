package com.trush.game.save;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Item;
import com.trush.game.objects.Synergy;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SynergySaveTest {
    private static Preferences prefs() {
        Map<String, String> strings = new HashMap<>();
        Preferences prefs = mock(Preferences.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(prefs.getString(anyString(), anyString()))
                .thenAnswer(i -> strings.getOrDefault(i.getArgument(0), i.getArgument(1)));
        when(prefs.putString(anyString(), anyString())).thenAnswer(i -> {
            strings.put(i.getArgument(0), i.getArgument(1));
            return prefs;
        });
        return prefs;
    }

    @AfterEach
    void resetBook() {
        Flufficon.load(prefs());
    }

    @Test
    void discoveriesSurviveLoadingAndResetWithProgress() {
        Preferences prefs = prefs();
        Flufficon.load(prefs);
        assertFalse(Flufficon.hasFound(Synergy.ICY_WATER));
        Flufficon.foundSynergy(Synergy.ICY_WATER);
        Flufficon.foundSynergy(Synergy.BANANA_SPLIT);
        Flufficon.load(prefs);
        assertTrue(Flufficon.hasFound(Synergy.ICY_WATER));
        assertTrue(Flufficon.hasFound(Synergy.BANANA_SPLIT));
        assertFalse(Flufficon.hasFound(Synergy.BIG_FISH));
        Flufficon.load(prefs());
        assertFalse(Flufficon.hasFound(Synergy.ICY_WATER));
    }

    @Test
    void savedGunAndItemRecreateTheBonusWithoutSavingItTwice() {
        Weapon shark = Weapon.chomper();
        shark.apply(Weapon.Upgrade.DAMAGE);
        shark.restoreAmmo(1, 19);
        RunSave.GunData saved = RunSave.GunData.of(shark);
        Weapon back = saved.toWeapon();
        Player player = new Player(null);
        player.restore(6, 6, 2, 0, new Array<>(new Weapon[]{back}), 0);
        player.restoreItems(new Array<>(new Item[]{Item.FLIPPERS}), null, null);
        assertEquals(Synergy.BIG_FISH, player.synergyFor(back));
        assertEquals(shark.getDamage(), back.getDamage());
        assertEquals(19, back.getAmmo());
    }
}
