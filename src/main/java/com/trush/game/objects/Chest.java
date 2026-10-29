package com.trush.game.objects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;

/**
 * A treasure chest standing in a room. Locked chests cost a key to open; the boss's reward chest is free.
 * Its gun (if any) is picked when the chest is placed, so a gun can't come out of two chests.
 * Blocks movement like furniture, but bullets fly over it.
 */
public class Chest {

    public final Tier tier;
    public final boolean locked;
    /** The gun inside, or null: then it holds a handful of pickups. */
    public final Weapon gun;
    /** Or an item (null if it holds a gun or only loot). */
    public final Item item;
    public final Rectangle solid = new Rectangle();
    public final float cx;
    public final float cy;
    public boolean opened;
    /** Little jump when opened, and a shake when tried without a key (or shot). */
    private float bump;
    /**
     * Shot this much it breaks open (like ETG's chests): the lock doesn't matter, but what's inside rarely
     * survives - mostly junk for the Junk Knight, or half a heart or some ammo.
     */
    public static final float HP = 24f;
    public float hp = HP;
    /** Chance the gun or item inside comes through a breaking in one piece. */
    public static final float SURVIVES = 0.08f;
    /** If it doesn't: chance of half a heart or ammo (else junk). */
    public static final float SCRAPS = 0.4f;

    public Chest(Tier tier, float cx, float cy, boolean locked, Weapon gun, Item item) {
        this.item = item;
        this.tier = tier;
        this.cx = cx;
        this.cy = cy;
        this.locked = locked;
        this.gun = gun;
        solid.set(cx - 0.3f, cy - 0.15f, 0.6f, 0.3f);
    }

    public void bump() {
        bump = 0.2f;
    }

    /** A player bullet hit it: true once it's had enough and breaks. */
    public boolean hit(float damage) {
        hp -= damage;
        bump = 0.12f;
        return hp <= 0;
    }

    /** What comes out of a broken chest. */
    public enum Remains { CONTENTS, SCRAPS, JUNK }

    /** {@code roll} (0-1) to what's left: its gun or item in one piece, a scrap of something useful, or junk. */
    public Remains remains(float roll) {
        if ((gun != null || item != null) && roll < SURVIVES) {
            return Remains.CONTENTS;
        }
        return roll < SURVIVES + SCRAPS * (1f - SURVIVES) ? Remains.SCRAPS : Remains.JUNK;
    }

    public void update(float delta) {
        bump = Math.max(0, bump - delta);
    }

    public float sortY() {
        return solid.y;
    }

    private String spriteName() {
        return opened ? tier.chestSprite + "_open" : tier.chestSprite;
    }

    /** Where the sprite is drawn (taller than the footprint). */
    public Rectangle spriteRect(Sprites sprites, Rectangle out) {
        TextureRegion r = sprites.prop(spriteName());
        float w = Sprites.w(r);
        return out.set(cx - w / 2f, solid.y - 0.05f, w, Sprites.h(r));
    }

    public void draw(SpriteBatch b, Sprites sprites, Draw d) {
        TextureRegion r = sprites.prop(spriteName());
        float w = Sprites.w(r);
        float h = Sprites.h(r);
        float wobble = bump > 0 ? MathUtils.sin(bump * 90f) * 0.02f : 0;
        b.draw(r, cx - w / 2f + wobble, solid.y - 0.05f + Math.abs(wobble), w, h);
        if (!opened && hp < HP) { // cracks spreading across the lid
            float k = 1f - hp / HP;
            int cracks = 1 + (int) (k * 4f);
            float top = solid.y - 0.05f + h * 0.75f;
            for (int i = 0; i < cracks; i++) {
                float sx = cx - w * 0.35f + i * w * 0.18f;
                d.line(b, sx, top, 0.08f + k * 0.08f, 0.015f, -60f - i * 25f, Draw.OUTLINE_COLOR, 0.8f);
            }
        }
    }
}
