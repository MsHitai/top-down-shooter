package com.trush.game.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Player;
import com.trush.game.world.GameWorld;
import com.trush.game.world.StoryDirector;

/**
 * A story run between floors. Like in ETG, you can only save after beating a floor's boss: the save
 * holds what the player carries (hearts, blanks, guns and their ammo) and the score, and continuing
 * starts the next floor. It is stored as JSON in the game's preferences and used up when continued,
 * or when a new run starts.
 */
public class RunSave {

    private static final String KEY = "savedRun";
    private static final int VERSION = 2;

    public int version = VERSION;
    /** The floor to play next. */
    public int floor;
    public int score;
    public int kills;
    public int health;
    public int maxHealth;
    public int blanks;
    public int keys;
    public int weaponIndex;
    public Array<GunData> weapons = new Array<>();

    public static class GunData {
        public String name;
        public int mag;
        public int ammo;

        static GunData of(Weapon w) {
            GunData g = new GunData();
            g.name = w.getName();
            g.mag = w.getMag();
            g.ammo = w.getAmmo();
            return g;
        }

        Weapon toWeapon() {
            Weapon w = Weapon.byName(name);
            if (w != null) {
                w.restoreAmmo(mag, ammo);
            }
            return w;
        }
    }

    /** Snapshot after a floor's boss is beaten (keys carry over to the next floor, like in ETG): the run continues on the floor after {@code clearedFloor}. */
    public static RunSave afterFloor(GameWorld world, int clearedFloor) {
        RunSave s = new RunSave();
        s.floor = clearedFloor + 1;
        s.score = world.getScore();
        s.kills = world.getKills();
        Player p = world.getPlayer();
        s.health = p.getHealth();
        s.maxHealth = p.getMaxHealth();
        s.blanks = p.getBlanks();
        s.keys = p.getKeys();
        s.weaponIndex = p.getWeaponIndex();
        for (Weapon w : p.getWeapons()) {
            s.weapons.add(GunData.of(w));
        }
        return s;
    }

    /** Builds the world for the next floor, with the player's loadout carried over. */
    public GameWorld startFloor(ETGFanfic game) {
        Array<Weapon> guns = new Array<>();
        Array<String> names = new Array<>();
        for (GunData g : weapons) {
            Weapon w = g.toWeapon();
            if (w != null) {
                guns.add(w);
                names.add(w.getName());
            }
        }
        GameWorld world = new GameWorld(game, new StoryDirector(floor, names));
        world.restoreStats(score, kills);
        // like ETG, every floor starts with at least 2 blanks
        world.getPlayer().restore(maxHealth, health, Math.max(2, blanks), keys, guns, weaponIndex);
        return world;
    }

    /** Main menu label. */
    public String describe() {
        return "CONTINUE - FLOOR " + floor;
    }

    // ------------------------------------------------------------ storage

    private static Json json() {
        Json json = new Json(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
        json.setIgnoreUnknownFields(true);
        return json;
    }

    public void write(Preferences prefs) {
        prefs.putString(KEY, json().toJson(this));
        prefs.flush();
    }

    public static boolean exists(Preferences prefs) {
        return prefs.contains(KEY);
    }

    /** The saved run, or null if there is none or it can't be read (a broken or outdated save is deleted). */
    public static RunSave read(Preferences prefs) {
        if (!exists(prefs)) {
            return null;
        }
        try {
            RunSave s = json().fromJson(RunSave.class, prefs.getString(KEY));
            if (s != null && s.version == VERSION && s.floor > 1) {
                return s;
            }
        } catch (Exception e) {
            if (Gdx.app != null) {
                Gdx.app.error("RunSave", "can't read the saved run", e);
            }
        }
        delete(prefs);
        return null;
    }

    public static void delete(Preferences prefs) {
        prefs.remove(KEY);
        prefs.flush();
    }
}
