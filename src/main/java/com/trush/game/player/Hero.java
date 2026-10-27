package com.trush.game.player;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.trush.game.objects.Weapon;
import com.trush.game.save.Unlocks;

/**
 * Who you play: the family in the living room (screen/HubScreen). The boy is the plain hero; his
 * sister rolls further with throwing stars but has a heart less, granny brings her cat (objects/Cat)
 * and walks a bit slower, grandad gives every gun he gets a +1 but rolls shorter - and only joins in
 * once the Fluffy King has been beaten ({@link Unlocks#grandad}).
 */
public enum Hero {
    BOY("THE BOY", "player", "2f9e5b", 6, 1f, 1f, "Just the boy. Three hearts and the Rusty Sidearm."),
    SISTER("SISTER", "player_sister", "2e2e44", 4, 1.05f, 1.3f,
            "A ninja wannabe: rolls further, throws stars. Only two hearts."),
    GRANNY("GRANNY", "player_granny", "9a7ac0", 6, 0.9f, 1f,
            "Her cat pounces on enemies and brings presents. Walks a bit slower."),
    GRANDAD("GRANDAD", "player_grandad", "b08a5a", 6, 1f, 0.72f,
            "Every gun he gets is +1. A shorter roll - his back, you know.");

    private static final String PREF = "hero";

    public final String title;
    /** The sprite sheet in assets/sprites/ (without .png), laid out like the boy's player.png. */
    public final String sheet;
    /** Their shirt colour (as in tools/gen_sprites.py FAMILY), for arms drawn over the sprite. */
    public final Color shirt;
    /** Hearts to start with, in half hearts. */
    public final int halfHearts;
    public final float walkSpeed;
    /** How far (and fast) a dodge roll goes, x the boy's. */
    public final float rollReach;
    public final String blurb;

    /** Who's playing now (picked in the living room, remembered between sessions). */
    public static Hero current = BOY;

    Hero(String title, String sheet, String shirt, int halfHearts, float walkSpeed, float rollReach, String blurb) {
        this.title = title;
        this.sheet = sheet;
        this.shirt = Color.valueOf(shirt);
        this.halfHearts = halfHearts;
        this.walkSpeed = walkSpeed;
        this.rollReach = rollReach;
        this.blurb = blurb;
    }

    /** The gun they start a run with. */
    public Weapon starter() {
        switch (this) {
            case SISTER:
                return Weapon.throwingStars();
            case GRANNY:
                return Weapon.pearlRevolver();
            case GRANDAD: {
                Weapon w = Weapon.doubleBarrel();
                w.upgrade(); // his +1 on everything, his old shotgun too
                return w;
            }
            default:
                return Weapon.rustySidearm();
        }
    }

    /** Can be picked (grandad waits until the Fluffy King has been beaten once). */
    public boolean unlocked() {
        return this != GRANDAD || Unlocks.grandad;
    }

    /** Reads the last pick (call after {@link Unlocks#load}); a pick that's locked again goes back to the boy. */
    public static void load(Preferences prefs) {
        current = BOY;
        for (Hero h : values()) {
            if (h.name().equals(prefs.getString(PREF, BOY.name())) && h.unlocked()) {
                current = h;
            }
        }
    }

    public static void pick(Hero hero, Preferences prefs) {
        current = hero;
        prefs.putString(PREF, hero.name());
        prefs.flush();
    }

    /** A hero by enum name (saved runs), or the boy. */
    public static Hero byName(String name) {
        for (Hero h : values()) {
            if (h.name().equals(name)) return h;
        }
        return BOY;
    }
}
