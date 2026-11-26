package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;
import com.trush.game.enemy.Boss;
import com.trush.game.player.Hero;
import com.trush.game.save.Flufficon;

/** One solo fight. A retry keeps the same boss, difficulty and rule. */
public record BossRematch(Boss boss, Difficulty difficulty, Dream.Rule rule) {
    public enum Difficulty {
        PRACTICE, NORMAL, HARD
    }

    private static final Dream.Rule[] RULES = {
            Dream.Rule.FAST_FORWARD, Dream.Rule.LIGHTS_OUT, Dream.Rule.SLOW_RELOAD, Dream.Rule.SMALL_BLANKS
    };

    public static Dream.Rule rollRule() {
        return RULES[MathUtils.random(RULES.length - 1)];
    }

    public static boolean available(Boss boss) {
        return Flufficon.timesBeaten(boss) > 0;
    }

    public boolean hard() {
        return difficulty == Difficulty.HARD && !Hero.current.easy();
    }

    public Flufficon.Medal medal() {
        if (difficulty == Difficulty.PRACTICE || Hero.current.easy()) return Flufficon.Medal.NONE;
        return hard() ? Flufficon.Medal.GOLD : Flufficon.Medal.SILVER;
    }
}
