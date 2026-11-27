package com.trush.game.world;

/** The positive Dream rules offered as a reward for taking two Story Dares. */
public enum StoryModifier {
    BOUNCY(Dream.Rule.BOUNCY, "Plain bullets get two extra wall bounces."),
    GOLD_RUSH(Dream.Rule.GOLD_RUSH, "Coin drops are doubled."),
    MOON_BOOTS(Dream.Rule.MOON_BOOTS, "Dodge rolls travel 50% farther."),
    SOFT_LANDING(Dream.Rule.SOFT_LANDING, "Landing a dodge roll clears nearby bullets. Recharges in 5 seconds."),
    QUICK_DRAW(Dream.Rule.QUICK_DRAW, "The next attack after a dodge roll deals 50% more damage. Recharges in 3 seconds after firing.");

    public final Dream.Rule rule;
    public final String description;

    StoryModifier(Dream.Rule rule, String description) {
        this.rule = rule;
        this.description = description;
    }

    public static StoryModifier byName(String name) {
        for (StoryModifier modifier : values()) {
            if (modifier.name().equals(name)) return modifier;
        }
        return null;
    }
}
