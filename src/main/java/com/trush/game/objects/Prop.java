package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.trush.game.gfx.Sprites;
import com.trush.game.world.Room;

/**
 * Furniture in a room. Everything blocks movement; tables only block bullets once flipped
 * (bullets fly over a standing table, like in ETG). Most props can be shot to pieces.
 * {@code solid} is the footprint used for collisions; the sprite is drawn on top of it.
 */
public class Prop {

    public enum Kind {
        // a standing table is seen from above, so its footprint reaches back to cover the tabletop
        // (all but its last few pixels, which hide the feet of whoever stands behind it)
        TABLE("table", 1.0f, 0.4f, 0.24f, 12f),
        BARREL("barrel", 0.45f, 0.3f, 0f, 10f),
        CRATE("crate", 0.5f, 0.35f, 0f, 8f),
        HAY("hay", 0.78f, 0.35f, 0f, 14f),
        PILLAR("pillar", 0.5f, 0.35f, 0f, -1f),
        // the town's furniture
        BENCH("bench", 0.78f, 0.3f, 0f, 14f),
        LAMP("lamp", 0.3f, 0.25f, 0f, -1f);

        final String sprite;
        public final float w;
        public final float h;
        /** Extra footprint behind the centred {@code w x h} box (toward the top of the screen). */
        final float back;
        final float hp;

        Kind(String sprite, float w, float h, float back, float hp) {
            this.sprite = sprite;
            this.w = w;
            this.h = h;
            this.back = back;
            this.hp = hp;
        }
    }

    public final Kind kind;
    public final Rectangle solid = new Rectangle();
    public final float cx;
    public final float cy;
    public boolean alive = true;
    private boolean flipped;
    private int flipDir = -1;
    private float hp;
    private float shake;
    private float flipAnim;
    /** Cached {@link #bulletRect}; recomputed after a flip. */
    private final Rectangle bulletRect = new Rectangle();
    private boolean bulletRectValid;

    public Prop(Kind kind, float cx, float cy) {
        this.kind = kind;
        this.cx = cx;
        this.cy = cy;
        this.hp = kind.hp;
        solid.set(cx - kind.w / 2f, cy - kind.h / 2f, kind.w, kind.h + kind.back);
    }

    public boolean isFlipped() {
        return flipped;
    }

    public boolean canFlip() {
        return kind == Kind.TABLE && !flipped && alive;
    }

    public boolean blocksBullets() {
        return kind != Kind.TABLE || flipped;
    }

    public boolean isDestructible() {
        return kind.hp > 0;
    }

    /** Tips the table over toward {@code dir} (one of Room.N/S/W/E). It becomes a thin bullet-proof wall. */
    public void flip(int dir) {
        flipped = true;
        bulletRectValid = false;
        flipDir = dir;
        flipAnim = 0.15f;
        switch (dir) {
            case Room.N:
                solid.set(cx - 0.5f, cy + 0.08f, 1.0f, 0.2f);
                break;
            case Room.S:
                solid.set(cx - 0.5f, cy - 0.3f, 1.0f, 0.2f);
                break;
            case Room.W:
                solid.set(cx - 0.55f, cy - 0.45f, 0.2f, 0.9f);
                break;
            default:
                solid.set(cx + 0.35f, cy - 0.45f, 0.2f, 0.9f);
                break;
        }
    }

    /** Returns true if this hit destroyed the prop. */
    public boolean damage(float amount) {
        shake = 0.12f;
        if (!isDestructible() || !alive) {
            return false;
        }
        hp -= amount;
        if (hp <= 0) {
            alive = false;
            return true;
        }
        return false;
    }

    public void update(float delta) {
        shake = Math.max(0, shake - delta);
        flipAnim = Math.max(0, flipAnim - delta);
    }

    public float sortY() {
        return solid.y;
    }

    private String spriteName() {
        if (kind != Kind.TABLE || !flipped) {
            return kind.sprite;
        }
        switch (flipDir) {
            case Room.N:
                return "table_flip_up";
            case Room.S:
                return "table_flip_down";
            case Room.W:
                return "table_flip_left";
            default:
                return "table_flip_right";
        }
    }

    /** Where the sprite is drawn: taller than {@code solid}, which is only the footprint on the floor. */
    /**
     * Where bullets are stopped: the footprint, stretched up to just below the top of the sprite.
     * Pillars, barrels and flipped tables stand tall, so whoever is drawn behind them is covered -
     * the footprint alone would let bullets through to the body of someone hiding there.
     */
    public Rectangle bulletRect(Sprites sprites) {
        if (!bulletRectValid) {
            spriteRect(sprites, bulletRect);
            float top = Math.max(solid.y + solid.height, bulletRect.y + bulletRect.height - 0.1f);
            bulletRect.set(solid.x, solid.y, solid.width, top - solid.y);
            bulletRectValid = true;
        }
        return bulletRect;
    }

    public Rectangle spriteRect(Sprites sprites, Rectangle out) {
        TextureRegion r = sprites.prop(spriteName());
        float w = Sprites.w(r);
        return out.set(solid.x + solid.width / 2f - w / 2f, solid.y - 0.05f, w, Sprites.h(r));
    }

    public void draw(SpriteBatch b, Sprites sprites) {
        TextureRegion r = sprites.prop(spriteName());
        float w = Sprites.w(r);
        float h = Sprites.h(r);
        float x = solid.x + solid.width / 2f - w / 2f + (shake > 0 ? MathUtils.sin(shake * 120f) * 0.02f : 0);
        float y = solid.y - 0.05f + flipAnim * 0.8f;
        if (hp > 0 && isDestructible() && hp < kind.hp * 0.4f) {
            b.setColor(1f, 0.82f, 0.82f, 1f); // looks battered
        }
        b.draw(r, x, y, w, h);
        b.setColor(Color.WHITE);
    }
}
