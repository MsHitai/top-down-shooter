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
 *   <li>{@code gun=chomper} - start with that gun in hand (any chest gun or the Flufful, by name)</li>
 *   <li>{@code items=all} / {@code items=fluff_field,hot_sauce} - start with these items (the first active one is held)</li>
 *   <li>{@code bossRoom} - a new story run (from the main menu) starts at the boss</li>
 *   <li>{@code retryBoss} - "try again" / restart goes straight back to the boss instead of floor 1</li>
 *   <li>{@code keepSave} - continuing a saved run doesn't use the save up</li>
 *   <li>{@code startWave=N} - endless mode starts at wave N</li>
 *   <li>{@code layout=N} - endless arena uses RoomTemplates.COMBAT[N]</li>
 *   <li>{@code boss=haymaker} - always fight this boss (enemy.Boss name, any case)</li>
 *   <li>{@code floor=N} - a new story run starts on floor N (2 = the town)</li>
 *   <li>{@code mimic} - every town/city combat room hides a mimic street lamp</li>
 *   <li>{@code blackChest} - every treasure chest (and a flawless boss's chest) is black, while S guns are left</li>
 *   <li>{@code slums} - a new story run starts down in the Slums (with {@code bossRoom}, at its boss)</li>
 *   <li>{@code trap=acid} / {@code trap=pipes} - with {@code slums}: start in one of its trap rooms, with that trap</li>
 *   <li>{@code undercity} - a new story run starts down in the Undercity (with {@code bossRoom}, at its boss)</li>
 *   <li>{@code enemy=raccoon} - every story fight is just that kind of enemy (StoryDirector.NAMES lists them)</li>
 *   <li>{@code win} - a story run opens on the victory screen (with {@code undercity}: the normal ending)</li>
 *   <li>{@code fight} - a story run starts in the floor's deepest roomy fight (to try its enemies)</li>
 *   <li>{@code slumsWay} - with {@code floor=3}: a city run starts in the dead end with the burning drum</li>
 *   <li>{@code play=story} / {@code play=endless} - skip the main menu and start that mode; {@code play=home} - the living room</li>
 *   <li>{@code bossHp=N} - a story boss starts at N% of its health (to try its later phases)</li>
 *   <li>{@code moon} - a new story run starts up on the Moon (with {@code bossRoom}, at its boss)</li>
 *   <li>{@code play=ride} - the rocket ride up to the Moon, then a story run (add {@code moon} to land on it)</li>
 *   <li>{@code console} - the rocket game console is in the living room (as if the rocket had been up to the Moon; not saved)</li>
 *   <li>{@code book=N} - with {@code play=home}: the Flufficon opens at page N (0 records ... 5 items)</li>
 *   <li>{@code play=console} - straight into the rocket game on the console (with {@code bossRoom}, a boss after 3 s - {@code boss=witch} picks it; {@code god} keeps the hearts)</li>
 *   <li>{@code rocket=N} - the living room's rocket shows as built by the first N of the family (0-4; not saved)</li>
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

    /** A text flag, or null if it's not set (or this isn't a dev build). */
    public static String text(String name) {
        return ENABLED ? System.getProperty("fluffy." + name) : null;
    }

    public static int number(String name, int fallback) {
        return ENABLED ? Integer.getInteger("fluffy." + name, fallback) : fallback;
    }
}
