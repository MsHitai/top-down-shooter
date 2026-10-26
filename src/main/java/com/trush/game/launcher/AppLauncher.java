package com.trush.game.launcher;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class AppLauncher {

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) {
            return;
        }
        createApplication();
    }

    private static Lwjgl3Application createApplication() {
        return new Lwjgl3Application(new ETGFanfic(), getDefaultConfiguration());
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("ENTER THE FLUFFY");
        configuration.useVsync(true);
        // some displays/VMs report a refresh rate of 0, which would cap the game at 1 FPS
        int refreshRate = Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate;
        configuration.setForegroundFPS(refreshRate >= 30 ? refreshRate + 1 : 60);
        configuration.setWindowedMode(1280, 980); // (ETGFanfic.WINDOW_W/H: leaving fullscreen goes back to this)
        configuration.setWindowIcon("libgdx128.png", "libgdx64.png", "libgdx32.png", "libgdx16.png");

        return configuration;
    }

}
