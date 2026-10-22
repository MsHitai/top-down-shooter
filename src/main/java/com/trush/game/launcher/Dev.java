package com.trush.game.launcher;

import java.io.InputStream;
import java.util.Properties;

/**
 * Debug switches, read from {@code -Dfluffy.<name>} JVM flags - but only in a dev build
 * ({@code mvn clean package -Pdev}, or running from the IDE without Maven filtering).
 * A normal build ignores them all, so they can't leak into a release.
 *
 * <ul>
 *   <li>{@code god} - hits register but never cost health</li>
 *   <li>{@code allGuns} - start with every gun</li>
 *   <li>{@code bossRoom} - a new story run (from the main menu) starts at the boss</li>
 *   <li>{@code retryBoss} - "try again" / restart goes straight back to the boss instead of floor 1</li>
 *   <li>{@code keepSave} - continuing a saved run doesn't use the save up</li>
 *   <li>{@code startWave=N} - endless mode starts at wave N</li>
 *   <li>{@code layout=N} - endless arena uses RoomTemplates.COMBAT[N]</li>
 * </ul>
 */
public final class Dev {

    public static final boolean ENABLED = load();

    private Dev() {
    }

    private static boolean load() {
        try (InputStream in = Dev.class.getResourceAsStream("/dev.properties")) {
            if (in == null) {
                return false;
            }
            Properties p = new Properties();
            p.load(in);
            String v = p.getProperty("dev", "false").trim();
            // an unfiltered placeholder means we run straight from the sources (IDE), which is development
            return v.equals("true") || v.startsWith("${");
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean flag(String name) {
        return ENABLED && Boolean.getBoolean("fluffy." + name);
    }

    public static int number(String name, int fallback) {
        return ENABLED ? Integer.getInteger("fluffy." + name, fallback) : fallback;
    }
}
