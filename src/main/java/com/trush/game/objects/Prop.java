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
        LAMP("lamp", 0.3f, 0.25f, 0f, 30f), // breakable (sturdier than a table), so it's no safe hiding spot
        // the city's: a parked car - big, sturdy cover that can still be wrecked
        CAR("car", 0.9f, 0.32f, 0.12f, 40f),
        // the skyscraper's: office desks, potted plants and water coolers
        DESK("desk", 0.9f, 0.32f, 0.12f, 20f),
        PLANT("plant", 0.36f, 0.26f, 0f, 12f),
        COOLER("cooler", 0.34f, 0.28f, 0f, 12f),
        // the clouds': cloud banks (shot to wisps), marble columns, golden urns and presents
        CLOUD("cloud", 0.8f, 0.35f, 0f, 14f),
        COLUMN("column", 0.46f, 0.32f, 0f, -1f),
        URN("urn", 0.38f, 0.28f, 0f, 10f),
        GIFT("gift", 0.48f, 0.34f, 0f, 8f),
        // the Pigeon King's statue: unbreakable stone in the middle of his arena
        PEDESTAL("pedestal", 0.8f, 0.4f, 0f, -1f),
        // Baa-llo's shop: his counter and the display stands (waist-high: bullets fly over them, like over
        // a standing table - and over the counter at the sheep), in wood or, up in the clouds, marble
        COUNTER("counter", 1.9f, 0.35f, 0f, -1f),
        CLOUD_COUNTER("counter_sky", 1.9f, 0.35f, 0f, -1f),
        STAND("stand", 0.5f, 0.3f, 0f, -1f),
        CLOUD_STAND("stand_sky", 0.5f, 0.3f, 0f, -1f);

        final String sprite;
        public final float w;
        public final float h;
        /** Extra footprint behind the centred {@code w x h} box (toward the top of the screen). */
        final float back;
        final float hp;

        /** Two layout cells wide (it's centred on the line between them). */
        public boolean wide() {
            return this == TABLE || this == HAY || this == BENCH || this == CAR || this == DESK || this == CLOUD;
        }

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
    /** A street lamp that is really a LampMimic, waiting (GameWorld wakes it). It twitches now and then. */
    public boolean mimic;
    private float twitch = MathUtils.random(3f, 8f);
    /** The prop whose footprint this one reaches up to, if any (see joinAbove). */
    public Prop joinedAbove;
    private boolean flipped;
    private int flipDir = -1;
    private float hp;
    /** Health when undamaged; -1 = unbreakable (can change: see makeBreakable). */
    private float maxHp;
    private float shake;
    private float flipAnim;
    /** Cached {@link #bulletRect}s (low = footprint, tall = up to the sprite top); recomputed after a flip. */
    private final Rectangle bulletLow = new Rectangle();
    private final Rectangle bulletTall = new Rectangle();
    private boolean bulletRectValid;

    public Prop(Kind kind, float cx, float cy) {
        this.kind = kind;
        this.cx = cx;
        this.cy = cy;
        this.hp = kind.hp;
        this.maxHp = kind.hp;
        solid.set(cx - kind.w / 2f, cy - kind.h / 2f, kind.w, kind.h + kind.back);
    }

    /**
     * Stretches this footprint up to the prop standing in the cell right above it: barrels and crates
     * are shallower than a cell, and the player's feet would slip through the gap between the two
     * while the tall sprites make it look like walking through them.
     */
    public void joinAbove(Prop above) {
        joinedAbove = above;
        solid.height = above.solid.y - solid.y;
        bulletRectValid = false;
    }

    /** The prop above was destroyed: back to its own footprint. */
    public void unjoin() {
        joinedAbove = null;
        solid.height = kind.h + kind.back;
        bulletRectValid = false;
    }

    /** Joins the gap between its footprint and the prop above (see joinAbove); lone furniture never does. */
    public boolean canJoin() {
        return kind.back == 0 && !kind.wide();
    }

    public boolean isFlipped() {
        return flipped;
    }

    public boolean canFlip() {
        return kind == Kind.TABLE && !flipped && alive;
    }

    public boolean blocksBullets() {
        return kind == Kind.TABLE ? flipped : !isShopFurniture();
    }

    /** The shop's counter and display stands. */
    public boolean isShopFurniture() {
        return kind == Kind.COUNTER || kind == Kind.CLOUD_COUNTER || kind == Kind.STAND || kind == Kind.CLOUD_STAND;
    }

    public boolean isDestructible() {
        return maxHp > 0;
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

    /** An unbreakable prop that can now be shot to pieces (the Pigeon King's statue once he's gone). */
    public void makeBreakable(float health) {
        maxHp = health;
        hp = health;
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
        if (mimic && (twitch -= delta) <= 0) { // the only tell: once in a while it shivers
            shake = 0.2f;
            twitch = MathUtils.random(5f, 10f);
        }
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
     * Where bullets are stopped. Pillars, barrels and flipped tables stand tall, so a bullet travelling
     * up the screen ({@code northbound}) - toward someone drawn behind them - is stopped anywhere up to
     * just below the top of the sprite. Bullets going down or sideways pass behind/in front of the tall
     * part and only hit the footprint on the floor, so cover only works in the direction it really covers.
     * A street lamp only blocks with its slim post, not its whole base.
     */
    public Rectangle bulletRect(Sprites sprites, boolean northbound) {
        if (!bulletRectValid) {
            float x = solid.x;
            float w = solid.width;
            if (kind == Kind.LAMP) {
                w = 0.14f;
                x = cx - w / 2f;
            }
            bulletLow.set(x, solid.y, w, solid.height);
            spriteRect(sprites, bulletTall);
            float top = Math.max(solid.y + solid.height, bulletTall.y + bulletTall.height - 0.1f);
            bulletTall.set(x, solid.y, w, top - solid.y);
            bulletRectValid = true;
        }
        return northbound ? bulletTall : bulletLow;
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
        if (hp > 0 && isDestructible() && hp < maxHp * 0.4f) {
            b.setColor(1f, 0.82f, 0.82f, 1f); // looks battered
        }
        b.draw(r, x, y, w, h);
        b.setColor(Color.WHITE);
    }
}
