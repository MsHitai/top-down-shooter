package com.trush.game.i18n;

import com.trush.game.enemy.Boss;
import com.trush.game.objects.Item;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Hero;
import com.trush.game.save.Flufficon;
import com.trush.game.world.Dare;
import com.trush.game.world.Dream;
import com.trush.game.world.GameWorld;
import com.trush.game.world.Stage;
import com.trush.game.world.StoryModifier;
import org.junit.jupiter.api.Test;

import static com.trush.game.i18n.Language.ENGLISH;
import static com.trush.game.i18n.Language.RUSSIAN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class LanguageTest {

    @Test
    void looksUpRussianAndFallsBackToEnglish() {
        assertEquals(ENGLISH, Language.fromCode(""));
        assertEquals(ENGLISH, Language.fromCode("unknown"));
        assertEquals(RUSSIAN, Language.fromCode("ru"));
        assertEquals("OPTIONS", ENGLISH.text("OPTIONS"));
        assertEquals("НАСТРОЙКИ", RUSSIAN.text("OPTIONS"));
        assertEquals("New message", RUSSIAN.text("New message"));
        assertEquals("ЭТАЖ 2 ЗАЧИЩЕН!", RUSSIAN.format("FLOOR %s CLEAR!", 2));
    }

    @Test
    void localizesDisplayNamesAndCoinCounts() {
        assertEquals("Rusty Sidearm", Weapon.rustySidearm().getName());
        assertEquals("Ржавый пистолет", Weapon.rustySidearm().displayName(RUSSIAN));
        assertEquals("Пухпал ур. 2 +1", Weapon.displayName(RUSSIAN, Weapon.flufful().getName(), 2, 1));
        assertEquals("21 МОНЕТА", GameWorld.coinPrice(21, RUSSIAN));
        assertEquals("24 МОНЕТЫ", GameWorld.coinPrice(24, RUSSIAN));
        assertEquals("11 МОНЕТ", GameWorld.coinPrice(11, RUSSIAN));
        assertEquals("14 МОНЕТ", GameWorld.coinPrice(14, RUSSIAN));
        assertEquals("1 COIN", GameWorld.coinPrice(1, ENGLISH));
    }

    @Test
    void translatesPublishedContentCatalogs() {
        for (String gun : Flufficon.allGuns()) translated(gun);
        for (Flufficon.Entry enemy : Flufficon.ENEMIES) {
            translated(enemy.title);
            translated(enemy.home);
        }
        for (Flufficon.Achievement achievement : Flufficon.Achievement.values()) {
            translated(achievement.title);
            translated(achievement.description);
        }
        for (Stage stage : Stage.values()) {
            translated(stage.title);
            if (!stage.clearText.isEmpty()) translated(stage.clearText);
        }
        for (Stage.Exit exit : Stage.Exit.values()) translated(exit.prompt);
        for (Boss boss : Boss.values()) {
            translated(boss.title);
            translated(boss.defeated);
            translated(boss.quote());
        }
        for (Hero hero : Hero.values()) {
            translated(hero.title);
            translated(hero.blurb);
        }
        for (Item item : Item.values()) {
            translated(item.title);
            translated(item.description);
        }
        for (Dream.Rule rule : Dream.Rule.values()) {
            translated(rule.title);
            translated(rule.description);
        }
        for (Dare dare : Dare.values()) translated(dare.description);
        for (StoryModifier modifier : StoryModifier.values()) translated(modifier.description);
    }

    private static void translated(String source) {
        assertNotEquals(source, RUSSIAN.text(source), "Missing Russian text for " + source);
    }
}
