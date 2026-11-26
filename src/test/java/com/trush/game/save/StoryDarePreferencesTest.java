package com.trush.game.save;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Preferences;
import com.badlogic.gdx.files.FileHandle;
import com.trush.game.world.Dare;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoryDarePreferencesTest {
    @Test
    void confirmedDaresSurviveReloadAndStayIndependentOfTheSavedTrip(@TempDir Path directory) {
        FileHandle file = new FileHandle(directory.resolve("preferences.xml").toFile());
        Preferences prefs = new Lwjgl3Preferences(file);
        assertTrue(Unlocks.storyDares(prefs).isEmpty());

        EnumSet<Dare> selected = EnumSet.of(Dare.FAST_FORWARD, Dare.MIXED_UP);
        Unlocks.saveStoryDares(selected, prefs);
        Preferences reloaded = new Lwjgl3Preferences(file);
        assertEquals(selected, Unlocks.storyDares(reloaded));

        RunSave trip = new RunSave();
        trip.floor = 2;
        trip.dare = Dare.SMALL_BLANKS.name();
        trip.write(reloaded);
        assertEquals(EnumSet.of(Dare.SMALL_BLANKS), RunSave.read(reloaded).dares());
        RunSave.delete(reloaded);
        assertEquals(selected, Unlocks.storyDares(reloaded));

        Unlocks.saveStoryDares(EnumSet.noneOf(Dare.class), reloaded);
        assertTrue(Unlocks.storyDares(new Lwjgl3Preferences(file)).isEmpty());
    }
}
