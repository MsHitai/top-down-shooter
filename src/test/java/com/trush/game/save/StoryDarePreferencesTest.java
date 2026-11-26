package com.trush.game.save;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.trush.game.world.Dare;
import com.trush.game.world.StoryModifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoryDarePreferencesTest {
    @Test
    void confirmedDaresSurviveReloadAndStayIndependentOfTheSavedTrip(@TempDir Path directory) {
        FileHandle file = new FileHandle(directory.resolve("preferences.xml").toFile());
        Preferences prefs = new Lwjgl3Preferences(file);
        assertTrue(Unlocks.storyDares(prefs).isEmpty());

        EnumSet<Dare> selected = EnumSet.of(Dare.FAST_FORWARD, Dare.MIXED_UP);
        Unlocks.saveStoryDares(selected, StoryModifier.QUICK_DRAW, prefs);
        Preferences reloaded = new Lwjgl3Preferences(file);
        assertEquals(selected, Unlocks.storyDares(reloaded));
        assertEquals(StoryModifier.QUICK_DRAW, Unlocks.storyModifier(reloaded));

        RunSave trip = new RunSave();
        trip.floor = 2;
        trip.dare = Dare.SMALL_BLANKS.name();
        trip.write(reloaded);
        assertEquals(EnumSet.of(Dare.SMALL_BLANKS), RunSave.read(reloaded).dares());
        RunSave.delete(reloaded);
        assertEquals(selected, Unlocks.storyDares(reloaded));
        assertEquals(StoryModifier.QUICK_DRAW, Unlocks.storyModifier(reloaded));

        Unlocks.saveStoryDares(EnumSet.noneOf(Dare.class), reloaded);
        Preferences cleared = new Lwjgl3Preferences(file);
        assertTrue(Unlocks.storyDares(cleared).isEmpty());
        assertNull(Unlocks.storyModifier(cleared));
    }
}
