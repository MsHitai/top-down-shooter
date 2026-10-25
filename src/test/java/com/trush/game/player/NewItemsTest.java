package com.trush.game.player;

import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Item;
import com.trush.game.objects.Weapon;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The 0.8 guns and items: the Guardian Lamb's second chance, pools and art for everything. */
class NewItemsTest {

    @Test
    void guardianLambSavesOneFatalHit() {
        Player p = new Player(null);
        p.restore(6, 1, 2, 0, new Array<>(), 0);
        p.addPassive(Item.GUARDIAN_LAMB);
        assertTrue(p.hurt(null));
        assertFalse(p.isDead(), "the lamb gives itself up instead");
        assertEquals(6, p.getHealth());
        assertFalse(p.hasItem(Item.GUARDIAN_LAMB));
        assertTrue(p.takeLambSave());
        assertFalse(p.takeLambSave(), "the show plays once");
    }

    @Test
    void withoutTheLambAFatalHitIsFatal() {
        Player p = new Player(null);
        p.restore(6, 1, 2, 0, new Array<>(), 0);
        assertTrue(p.hurt(null));
        assertTrue(p.isDead());
        assertFalse(p.takeLambSave());
    }

    @Test
    void endlessHasNoMapToRead() {
        assertFalse(Item.shuffledPool(false).contains(Item.TREASURE_MAP, true));
        assertTrue(Item.shuffledPool(true).contains(Item.TREASURE_MAP, true));
    }

    @Test
    void everyGunLoadsByNameAndHasArt() throws IOException {
        Set<String> sprites = atlasNames("assets/sprites/guns.txt");
        for (Weapon w : Weapon.chestGuns()) {
            assertNotNull(Weapon.byName(w.getName()), w.getName());
            assertTrue(sprites.contains(w.getSprite()), "no sprite for " + w.getName());
        }
    }

    @Test
    void everyItemHasAnIcon() throws IOException {
        Set<String> icons = atlasNames("assets/sprites/items.txt");
        for (Item i : Item.values()) {
            assertTrue(icons.contains(i.sprite), "no icon for " + i.title);
        }
    }

    private static Set<String> atlasNames(String path) throws IOException {
        Set<String> names = new HashSet<>();
        try (InputStream in = NewItemsTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(in, path);
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                if (!line.isBlank()) names.add(line.trim().split("\\s+")[0]);
            }
        }
        return names;
    }
}
