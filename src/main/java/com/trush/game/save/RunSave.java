package com.trush.game.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Item;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Hero;
import com.trush.game.player.Player;
import com.trush.game.world.GameWorld;
import com.trush.game.world.Stage;
import com.trush.game.world.StoryDirector;

/**
 * A story run between floors. Like in ETG, you can only save after beating a floor's boss: the save
 * holds what the player carries (hearts, blanks, keys, coins, guns and their ammo) and the score, and continuing
 * starts the next floor. It is stored as JSON in the game's preferences and used up when continued,
 * or when a new run starts.
 */
public class RunSave {

    private static final String KEY = "savedRun";
    private static final int VERSION = 2;

    public int version = VERSION;
    /** The floor to play next. */
    public int floor;
    /** Its stage when it's a secret one (the Undercity, under the Slums - by enum name); null: the story's own. */
    public String stage;
    public int score;
    public int kills;
    public int health;
    public int maxHealth;
    public int blanks;
    public int keys;
    /** Coins for Baa-llo's shop (an older save without them starts with none). */
    public int coins;
    /** Baa-llo was shot one time too many on an earlier floor: his prices stay up. */
    public boolean shopGrudge;
    public int weaponIndex;
    public Array<GunData> weapons = new Array<>();
    /** Passive items and the active one, by enum name. */
    public Array<String> items = new Array<>();
    public String activeItem;
    /** Who's playing (player/Hero by enum name; an older save without it was the boy's). */
    public String hero;

    public static class GunData {
        public String name;
        public int mag;
        public int ammo;
        /** Upgrades from finding the gun again, by enum name, in the order they came. */
        public Array<String> upgrades = new Array<>();
        /** The Flufful's kills (its level comes from them). */
        public int kills;

        static GunData of(Weapon w) {
            GunData g = new GunData();
            g.name = w.getName();
            g.mag = w.getMag();
            g.ammo = w.getAmmo();
            g.kills = w.getKills();
            for (Weapon.Upgrade u : w.getUpgrades()) {
                g.upgrades.add(u.name());
            }
            return g;
        }

        Weapon toWeapon() {
            Weapon w = Weapon.byName(name);
            if (w != null) {
                for (String u : upgrades) {
                    try {
                        w.apply(Weapon.Upgrade.valueOf(u));
                    } catch (IllegalArgumentException ignored) { // an upgrade this version doesn't know
                    }
                }
                w.restoreKills(kills);
                w.restoreAmmo(mag, ammo);
            }
            return w;
        }
    }

    /** Snapshot after a floor's boss is beaten (keys carry over to the next floor, like in ETG): the run continues on the floor after {@code clearedFloor}. */
    public static RunSave afterFloor(GameWorld world, int clearedFloor) {
        RunSave s = new RunSave();
        s.floor = clearedFloor + 1;
        if (world.getDirector() instanceof StoryDirector) { // the Slums' sewer hatch to the Undercity, the rocket to the Moon
            Stage next = ((StoryDirector) world.getDirector()).nextStage();
            s.stage = next != null ? next.name() : null;
        }
        s.score = world.getScore();
        s.kills = world.getKills();
        Player p = world.getPlayer();
        s.health = p.getHealth();
        s.maxHealth = p.getMaxHealth();
        s.blanks = p.getBlanks();
        s.keys = p.getKeys();
        s.coins = p.getCoins();
        s.shopGrudge = world.isShopGrudge();
        s.weaponIndex = p.getWeaponIndex();
        for (Weapon w : p.getWeapons()) {
            s.weapons.add(GunData.of(w));
        }
        for (Item i : p.getPassives()) {
            s.items.add(i.name());
        }
        s.activeItem = p.getActive() != null ? p.getActive().name() : null;
        s.hero = p.getHero().name();
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
        Array<Item> passives = new Array<>();
        for (String n : items) {
            Item i = Item.byName(n);
            if (i != null) passives.add(i);
        }
        Item active = activeItem != null ? Item.byName(activeItem) : null;
        Array<Item> owned = new Array<>(passives);
        if (active != null) owned.add(active);
        Hero.current = Hero.byName(hero); // the trip goes on with whoever started it
        GameWorld world = new GameWorld(game, new StoryDirector(floor, stage(), names, owned));
        world.restoreStats(score, kills);
        // like ETG, every floor starts with at least 2 blanks
        world.getPlayer().restore(maxHealth, health, Math.max(2, blanks), keys, guns, weaponIndex);
        world.getPlayer().restoreItems(passives, active);
        world.getPlayer().setCoins(coins);
        world.setShopGrudge(shopGrudge);
        if (passives.contains(Item.SPARE_POCKET, true)) { // +1 blank every floor
            world.getPlayer().addBlank();
        }
        return world;
    }

    /** The next floor's stage: a secret one it was saved with, or the story's own for the floor. */
    public Stage stage() {
        if (stage != null) {
            try {
                return Stage.valueOf(stage);
            } catch (IllegalArgumentException ignored) { // a stage this version doesn't know
            }
        }
        return Stage.forFloor(floor);
    }

    /** Main menu label. */
    public String describe() {
        Stage next = stage();
        return next.isSecret() ? "CONTINUE - " + next.title : "CONTINUE - FLOOR " + floor + ": " + next.title;
    }

    /** Who's on this trip (the living room's door says who's out there). */
    public Hero hero() {
        return Hero.byName(hero);
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
