package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.trush.game.player.Hero;
import com.trush.game.player.Player;

/** Inventory recipes, evaluated without changing a gun's saved stats or spending extra ammo. */
public enum Synergy {
    ICY_WATER("ICY WATER", "water_pistol", "freeze_ray", null, null, "9cecff",
            "Water Pistol + Freeze Ray", "Water shots build frost. The ray freezes 50% faster."),
    BANANA_SPLIT("BANANA SPLIT", "banana_rang", null, Item.BOUNCY_SOCKS, null, "c5ff73",
            "Banana-rang + Bouncy Socks", "Two full-strength bananas for one ammo."),
    BIG_FISH("BIG FISH", "chomper", null, Item.FLIPPERS, null, "57e5cf",
            "Chomper + Flippers", "Sharks are 50% bigger and deal 35% more damage."),
    CAT_LADY("CAT LADY", "kitty_cannon", null, null, Hero.GRANNY, "ffbb77",
            "Kitty Cannon + Granny", "Firing calls her cat to pounce. Its bites deal double damage while held."),
    EXTRA_HOT("EXTRA HOT", "flare_gun", null, Item.HOT_SAUCE, null, "ff8870",
            "Flare Gun + Hot Sauce", "Flares deal 25% more damage, burn longer and leave wider flames."),
    LIVE_WIRE("LIVE WIRE", "jumper_cables", null, Item.STATIC_FUZZ, null, "c7a0ff",
            "Jumper Cables + Static Fuzz", "Lightning jumps to two more enemies and deals 20% more damage.");

    public final String title;
    public final String gunSprite;
    public final String partnerSprite;
    public final Item item;
    public final Hero hero;
    public final Color color;
    public final String recipe;
    public final String description;

    Synergy(String title, String gunSprite, String partnerSprite, Item item, Hero hero, String color,
            String recipe, String description) {
        this.title = title;
        this.gunSprite = gunSprite;
        this.partnerSprite = partnerSprite;
        this.item = item;
        this.hero = hero;
        this.color = Color.valueOf(color);
        this.recipe = recipe;
        this.description = description;
    }

    public boolean active(Player player) {
        return owns(player, gunSprite) && (partnerSprite == null || owns(player, partnerSprite))
                && (item == null || player.hasItem(item)) && (hero == null || player.getHero() == hero);
    }

    private static boolean owns(Player player, String sprite) {
        for (Weapon w : player.getWeapons()) {
            if (w.getSprite().equals(sprite)) return true;
        }
        return false;
    }

    public boolean affects(Weapon weapon) {
        return gunSprite.equals(weapon.getSprite()) || partnerSprite != null && partnerSprite.equals(weapon.getSprite());
    }

    /** Applied once to a freshly made shot; the original gun remains unchanged. */
    public void apply(Bullet bullet) {
        bullet.color.set(color);
        switch (this) {
            case ICY_WATER -> bullet.frost = Math.max(bullet.frost, 0.22f);
            case BIG_FISH -> {
                bullet.radius *= 1.5f;
                bullet.damage *= 1.35f;
                bullet.knockback *= 1.5f;
            }
            case EXTRA_HOT -> {
                bullet.damage *= 1.25f;
                bullet.burn *= 1.5f;
                bullet.puddleRadius *= 1.5f;
            }
            case LIVE_WIRE -> {
                bullet.chain += 2;
                bullet.damage *= 1.2f;
            }
            default -> { }
        }
    }
}
