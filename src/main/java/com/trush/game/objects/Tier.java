package com.trush.game.objects;

/**
 * Item quality, like ETG: D (brown chest) &lt; C (blue) &lt; B (green) &lt; A (red).
 * A chest of a tier holds a gun of the same tier. (S / black chests can come once a gun deserves it.)
 */
public enum Tier {
    D("chest_d"),
    C("chest_c"),
    B("chest_b"),
    A("chest_a");

    /** Chest sprite in props.txt; "_open" is appended for the opened chest. */
    public final String chestSprite;

    Tier(String chestSprite) {
        this.chestSprite = chestSprite;
    }
}
