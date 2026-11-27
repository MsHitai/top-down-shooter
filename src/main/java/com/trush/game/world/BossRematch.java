package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.trush.game.enemy.Boss;
import com.trush.game.objects.Chest;
import com.trush.game.objects.Tier;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Hero;
import com.trush.game.save.Flufficon;
import com.trush.game.save.Flufficon.Medal;
import com.trush.game.save.Unlocks;

/** One solo fight. A retry keeps the same boss, difficulty and rule. */
public record BossRematch(Boss boss, Difficulty difficulty, Dream.Rule rule) {
    public enum Difficulty {
        PRACTICE, NORMAL, HARD
    }

    private static final Dream.Rule[] RULES = {
            Dream.Rule.FAST_FORWARD, Dream.Rule.LIGHTS_OUT, Dream.Rule.SLOW_RELOAD, Dream.Rule.SMALL_BLANKS
    };
    private static final int[] NORMAL_CHEST_WEIGHTS = {40, 30, 20, 8, 2};
    private static final int[] HARD_CHEST_WEIGHTS = {33, 31, 22, 10, 4};

    public static Dream.Rule rollRule() {
        return RULES[MathUtils.random(RULES.length - 1)];
    }

    public static boolean available(Boss boss) {
        return Flufficon.timesBeaten(boss) > 0;
    }

    public boolean hard() {
        return difficulty == Difficulty.HARD && !Hero.current.easy();
    }

    Chest rollChest(float x, float y) {
        Tier tier = chestTier(Flufficon.medal(boss), hard(), MathUtils.random());
        Array<Weapon> pool = new Array<>();
        for (Weapon weapon : Weapon.chestGuns()) {
            if (weapon.getTier() == tier) pool.add(weapon);
        }
        if (!Unlocks.flufful && tier == Tier.B) pool.add(Weapon.flufful());
        return new Chest(tier, x, y, false, pool.random(), null);
    }

    static Tier chestTier(Medal medal, boolean hard, float roll) {
        Tier cap = switch (medal) {
            case NONE -> Tier.B;
            case SILVER -> Tier.A;
            case GOLD -> Tier.S;
        };
        int[] weights = hard ? HARD_CHEST_WEIGHTS : NORMAL_CHEST_WEIGHTS;
        int total = 0;
        for (int i = 0; i <= cap.ordinal(); i++) {
            total += weights[i];
        }
        float remaining = roll * total;
        Tier[] tiers = Tier.values();
        for (int i = 0; i < cap.ordinal(); i++) {
            remaining -= weights[i];
            if (remaining < 0) return tiers[i];
        }
        return cap;
    }

    public Flufficon.Medal medal() {
        if (difficulty == Difficulty.PRACTICE || Hero.current.easy()) return Flufficon.Medal.NONE;
        return hard() ? Flufficon.Medal.GOLD : Flufficon.Medal.SILVER;
    }
}
