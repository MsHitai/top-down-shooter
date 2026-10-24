package com.trush.game.objects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Sprites;

import java.util.function.Predicate;

/**
 * Baa-llo's shop (our ETG Bello): a woolly sheep behind a counter, and five display stands - two wares
 * (a gun or an item each, taken out of the run's chest pools so no chest hands them out again) and three
 * consumables (a heart, a key, and a blank or an ammo box). One of them is on sale. Walk up to a stand to
 * see the price; the interact button buys it. Every story floor has a shop room; endless mode gets a
 * travelling stall after each boss wave (ring the bell on the counter to start the next wave).
 * <p>
 * Like Bello, he holds a grudge: shoot him {@link #GRUDGE_HITS} times and his prices go up by half for the
 * rest of the run.
 */
public class Shop {

    public enum Ware { GUN, ITEM, HEART, AMMO, KEY, BLANK }

    /** Hits it takes before he's had enough (he ducks after each, so a single burst counts once). */
    public static final int GRUDGE_HITS = 3;
    public static final float GRUDGE_MARKUP = 1.5f;
    public static final float SALE = 0.75f;
    /** Gun / item prices for tiers D, C, B, A. */
    private static final int[] TIER_PRICES = {25, 45, 70, 100};
    /** How long a line stays in his speech bubble (as long as the Flufful's, so the bubbles pop the same). */
    public static final float SAY_TIME = FluffulVoice.SHOW;
    private static final float DUCK_TIME = 0.7f;

    private static final String[] WELCOME = {"BAA! WELCOME, FRIEND!", "COME IN, COME IN!", "FINEST GOODS IN TOWN!"};
    private static final String[] BACK = {"BACK AGAIN?", "BAA! MISSED ME?", "TAKE YOUR TIME."};
    private static final String[] SOLD = {"BAA-RGAIN!", "PLEASURE DOING BUSINESS!", "A FINE CHOICE!"};
    private static final String[] POOR = {"NO COINS, NO WOOL!", "COME BACK RICHER!", "THAT'S NOT ENOUGH, FRIEND."};
    private static final String[] HIT = {"HEY! NO SHOOTING!", "WATCH IT!", "MIND THE WOOL!"};

    /** A stand and what's on it. */
    public static class Slot {
        public final Ware ware;
        public final Tier tier;
        /** What's on display (and what the player gets): drawn on the stand, collected when bought. */
        public final Pickup pickup;
        public boolean sale;
        public boolean sold;
        public int price;
        public Prop stand;

        Slot(Ware ware, Tier tier, Pickup pickup) {
            this.ware = ware;
            this.tier = tier;
            this.pickup = pickup;
        }

        /** "Fluff Shotgun", "Heart"... */
        public String title() {
            switch (ware) {
                case GUN:
                    return pickup.weapon.getName();
                case ITEM:
                    return pickup.item.title;
                case AMMO:
                    return "Ammo";
                default:
                    return ware.name().charAt(0) + ware.name().substring(1).toLowerCase();
            }
        }
    }

    /** The story floor (1-5), or in endless the boss waves beaten: prices go up 10% a level. */
    public final int level;
    /** Endless mode's stall: set up after a boss wave, packed away when the bell rings. */
    public final boolean travelling;
    public final Array<Slot> slots = new Array<>();
    public Prop counter;
    /** Where the sheep stands behind the counter (the bottom of his sprite). */
    public float keeperX;
    public float keeperY;
    private int hits;
    private String line;
    private float shown;
    private float duck;
    private float happy;
    private float blink = 3f;
    private boolean visited;

    public Shop(int level, boolean travelling) {
        this.level = level;
        this.travelling = travelling;
    }

    // ------------------------------------------------------------ prices

    public static int basePrice(Ware ware, Tier tier) {
        switch (ware) {
            case GUN:
            case ITEM:
                return TIER_PRICES[tier.ordinal()];
            case HEART:
            case BLANK:
                return 15;
            case AMMO:
                return 20;
            default: // a key opens a chest: worth something
                return 30;
        }
    }

    /** The price on the tag: 10% more each level, a quarter off on sale, half again if he holds a grudge. */
    public static int price(Ware ware, Tier tier, int level, boolean sale, boolean grudge) {
        float p = basePrice(ware, tier) * (1f + 0.1f * Math.max(0, level - 1));
        if (sale) p *= SALE;
        if (grudge) p *= GRUDGE_MARKUP;
        return Math.max(1, Math.round(p));
    }

    /** Puts the price tags on (again, once he's angry). */
    public void reprice(boolean grudge) {
        for (Slot s : slots) {
            s.price = price(s.ware, s.tier, level, s.sale, grudge);
        }
    }

    // ------------------------------------------------------------ stock

    /**
     * Fills the stands: a ware of each of {@code tiers} - a gun or an item of that tier (half and half when
     * both are left, a nearby tier if that one has run dry), taken out of the pools - then a heart, a blank
     * or an ammo box, and a key. The wares go on the outer stands; one stand is on sale.
     *
     * @param owned items the player already has (not offered again)
     */
    public void stock(Array<Weapon> guns, Array<Item> items, Tier[] tiers, Predicate<Item> owned, boolean grudge) {
        Array<Slot> wares = new Array<>();
        for (Tier t : tiers) {
            Slot s = takeWare(guns, items, t, owned);
            if (s != null) wares.add(s);
        }
        if (wares.size > 0) slots.add(wares.get(0));
        slots.add(new Slot(Ware.HEART, Tier.D, new Pickup(Pickup.Type.HEART, 0, 0)));
        slots.add(MathUtils.randomBoolean()
                ? new Slot(Ware.BLANK, Tier.D, new Pickup(Pickup.Type.BLANK, 0, 0))
                : new Slot(Ware.AMMO, Tier.D, new Pickup(Pickup.Type.AMMO, 0, 0)));
        slots.add(new Slot(Ware.KEY, Tier.D, new Pickup(Pickup.Type.KEY, 0, 0)));
        for (int i = 1; i < wares.size; i++) slots.add(wares.get(i));
        slots.random().sale = true;
        reprice(grudge);
    }

    /** A gun or an item of {@code tier}, or the closest tier that still has one (lower first); null if all gone. */
    private static Slot takeWare(Array<Weapon> guns, Array<Item> items, Tier tier, Predicate<Item> owned) {
        Tier[] all = Tier.values();
        for (int d = 0; d < all.length; d++) {
            for (int sign : new int[]{-1, 1}) {
                int t = tier.ordinal() + sign * d;
                if (t < 0 || t >= all.length || (d == 0 && sign > 0)) continue;
                Slot s = takeOfTier(guns, items, all[t], owned);
                if (s != null) return s;
            }
        }
        return null;
    }

    private static Slot takeOfTier(Array<Weapon> guns, Array<Item> items, Tier tier, Predicate<Item> owned) {
        int gun = -1;
        for (int i = 0; i < guns.size && gun < 0; i++) {
            if (guns.get(i).getTier() == tier) gun = i;
        }
        int item = -1;
        for (int i = 0; i < items.size && item < 0; i++) {
            if (items.get(i).tier == tier && !owned.test(items.get(i))) item = i;
        }
        if (item >= 0 && (gun < 0 || MathUtils.randomBoolean())) {
            return new Slot(Ware.ITEM, tier, new Pickup(items.removeIndex(item), 0, 0));
        }
        if (gun >= 0) {
            return new Slot(Ware.GUN, tier, new Pickup(Pickup.Type.GUN, 0, 0, guns.removeIndex(gun)));
        }
        return null;
    }

    // ------------------------------------------------------------ the shopkeeper

    /** The player walks in (or the stall opens). */
    public void greet(boolean grudge) {
        say(grudge ? "OH. IT'S YOU." : visited ? pick(BACK) : pick(WELCOME));
        visited = true;
    }

    public void onSold() {
        say(pick(SOLD));
        happy = 1.2f;
    }

    public void onTooPoor() {
        say(pick(POOR));
    }

    /** True while he's ducked behind the counter after a shot (bullets fly over him). */
    public boolean isDucking() {
        return duck > 0;
    }

    /**
     * A bullet hit him: he ducks and complains. Returns true on the hit that makes him angry for good
     * (the caller raises the prices); a grudge isn't held twice.
     */
    public boolean onShot(boolean grudge) {
        duck = DUCK_TIME;
        if (grudge) {
            say("OUCH! STILL ANGRY!");
            return false;
        }
        if (++hits >= GRUDGE_HITS) {
            say("THAT'S IT! PRICES ARE UP!");
            return true;
        }
        say(pick(HIT));
        return false;
    }

    public void update(float delta) {
        shown += delta;
        if (line != null && shown > SAY_TIME) line = null;
        duck = Math.max(0, duck - delta);
        happy = Math.max(0, happy - delta);
        blink -= delta;
        if (blink < -0.15f) blink = MathUtils.random(2f, 5f);
    }

    public void say(String text) {
        line = text;
        shown = 0;
    }

    /** What he's saying right now, or null. */
    public String line() {
        return line;
    }

    public float shown() {
        return shown;
    }

    public float sortY() {
        return keeperY;
    }

    public void draw(SpriteBatch b, Sprites sprites, boolean grudge) {
        String mood = grudge ? "baallo_angry" : happy > 0 ? "baallo_happy" : blink < 0 ? "baallo_blink" : "baallo";
        TextureRegion r = sprites.prop(mood);
        float w = Sprites.w(r);
        // he bobs a little; shot at, he ducks down behind the counter
        float y = keeperY + MathUtils.sin(shown * 3f) * 0.012f - (duck > 0 ? 0.42f * Math.min(1f, duck / 0.15f) : 0f);
        b.draw(r, keeperX - w / 2f, y, w, Sprites.h(r));
    }

    private static String pick(String[] lines) {
        return lines[MathUtils.random(lines.length - 1)];
    }
}
