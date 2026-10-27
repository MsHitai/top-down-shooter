package com.trush.game.objects;

/**
 * Item quality, like ETG: D (brown chest) &lt; C (blue) &lt; B (green) &lt; A (red) &lt; S (black).
 * A chest of a tier holds a gun of the same tier. S (guns and two items) only comes from rare black chests
 * ({@code StoryDirector.blackChest}); the shop never sells it.
 */
public enum Tier {
    D("chest_d"),
    C("chest_c"),
    B("chest_b"),
    A("chest_a"),
    S("chest_s");

    /** Chest sprite in props.txt; "_open" is appended for the opened chest. */
    public final String chestSprite;

    Tier(String chestSprite) {
        this.chestSprite = chestSprite;
    }
}
