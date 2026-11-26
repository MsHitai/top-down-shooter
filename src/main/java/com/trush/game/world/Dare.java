package com.trush.game.world;

/** The dream rules the skull offers for an entire Story trip. */
public enum Dare {
    FAST_FORWARD(Dream.Rule.FAST_FORWARD, "The action runs 20% faster."),
    GLASS_CANNON(Dream.Rule.GLASS_CANNON, "You deal and take double damage."),
    SMALL_BLANKS(Dream.Rule.SMALL_BLANKS, "Blanks only reach 2 units, even from the Bubble Wand."),
    TOUGH_CROWD(Dream.Rule.TOUGH_CROWD, "Tougher enemy variants show up earlier."),
    LIGHTS_OUT(Dream.Rule.LIGHTS_OUT, "Rooms darken and your clear view shrinks."),
    SLOW_RELOAD(Dream.Rule.SLOW_RELOAD, "Reloading takes 30% longer."),
    MIXED_UP(Dream.Rule.MIXED_UP, "Enemies can borrow other volleys in combat rooms."),
    CROSS_DRAFT(Dream.Rule.CROSS_DRAFT, "Gusts push both sides' bullets in combat rooms."),
    FALLING_DUST(Dream.Rule.FALLING_DUST, "Falling debris targets your spot in combat rooms.");

    public final Dream.Rule rule;
    public final String description;

    Dare(Dream.Rule rule, String description) {
        this.rule = rule;
        this.description = description;
    }

    public static Dare byName(String name) {
        for (Dare dare : values()) {
            if (dare.name().equals(name)) return dare;
        }
        return null;
    }
}
