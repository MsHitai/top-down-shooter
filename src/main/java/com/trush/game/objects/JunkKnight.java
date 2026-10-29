package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;

/**
 * The Junk Knight (after ETG's Ser Junkan): a little knight the hero builds out of the junk found in broken
 * chests. Every piece of junk is a level, up to {@link #MAX_LEVEL}: at first a wobbly tin can with a wooden
 * sword that barely scratches anyone; then a pot helmet, a pot-lid shield, a real sword, a cape, golden
 * armour - and at the last level wings and a bow, and it flies at the hero's shoulder shooting arrows.
 * GameWorld picks its targets and deals its damage; this only moves and draws it.
 */
public class JunkKnight {

    public static final int MAX_LEVEL = 7;
    /**
     * Not every piece of junk is alive: one in 30 is, and only that one wakes the knight up (the owner's call) -
     * at the level of all the junk found so far.
     */
    public static final float LIVING_CHANCE = 1f / 30f;
    /** Melee levels 1-6: damage a swing and seconds between swings. */
    private static final float[] DAMAGE = {2f, 3f, 5f, 7f, 9f, 12f};
    private static final float[] COOLDOWN = {1.0f, 0.9f, 0.85f, 0.8f, 0.7f, 0.65f};
    /** Level 7: an arrow this often, this hard. */
    public static final float ARROW_COOLDOWN = 0.32f;
    public static final float ARROW_DAMAGE = 8f;
    /** How far from the hero it goes looking for a fight (and shoots from). */
    public static final float RANGE = 4.5f;
    public static final float REACH = 0.3f;

    private static final Color TIN = Color.valueOf("a8adb8");
    private static final Color TIN_DARK = Color.valueOf("6e7380");
    private static final Color WOOD = Color.valueOf("9a6a3a");
    private static final Color STEEL = Color.valueOf("dfe6ee");
    private static final Color POT = Color.valueOf("4a4a55");
    private static final Color LID = Color.valueOf("c48a3a");
    private static final Color CAPE = Color.valueOf("c0392b");
    private static final Color GOLD = Color.valueOf("e8c040");
    private static final Color WING = Color.valueOf("fbfaff");

    public final Vector2 pos = new Vector2();
    private final Vector2 tmp = new Vector2();
    public float cooldown = 1f;
    private int level = 1;
    private float clock;
    private float swing;
    private float swingAngle;
    private boolean facingRight = true;
    private boolean placed;

    public void setLevel(int level) {
        this.level = MathUtils.clamp(level, 1, MAX_LEVEL);
    }

    public int level() {
        return level;
    }

    /** The last level: wings and a bow. */
    public boolean flies() {
        return level >= MAX_LEVEL;
    }

    public float damage() {
        return flies() ? ARROW_DAMAGE : DAMAGE[level - 1];
    }

    /**
     * Walks toward (tx, ty) - an enemy to hit, or a spot beside the hero - at its pace; the flying knight
     * just keeps to the hero's shoulder. Snaps to the hero when it's been left far behind (a new room).
     */
    public void update(float delta, float heroX, float heroY, boolean heroFacingRight, Vector2 enemy) {
        clock += delta;
        cooldown -= delta;
        swing = Math.max(0f, swing - delta);
        float tx;
        float ty;
        if (flies() || enemy == null) {
            tx = heroX + (heroFacingRight ? 0.4f : -0.4f);
            ty = heroY + (flies() ? 0.45f : -0.1f);
        } else {
            tx = enemy.x;
            ty = enemy.y;
        }
        if (!placed || pos.dst2(heroX, heroY) > 64f) {
            pos.set(heroX + (heroFacingRight ? 0.4f : -0.4f), heroY);
            placed = true;
        }
        tmp.set(tx, ty).sub(pos);
        float d = tmp.len();
        float pace = flies() ? 4f : 2.2f + 0.2f * level;
        float stop = enemy != null && !flies() ? REACH * 0.8f : 0.08f;
        if (d > stop) {
            pos.mulAdd(tmp.scl(1f / d), Math.min(d - stop, pace * delta));
        }
        if (Math.abs(tmp.x) > 0.02f) {
            facingRight = tmp.x > 0;
        }
        if (flies() && enemy != null) {
            facingRight = enemy.x > pos.x;
        }
    }

    /** Close enough to its target to swing at it. */
    public boolean canReach(float x, float y, float radius) {
        return pos.dst(x, y) < REACH + radius;
    }

    /** Swung (or loosed an arrow) at (x, y): the cooldown starts and the sword sweeps. */
    public void attacked(float x, float y) {
        cooldown = flies() ? ARROW_COOLDOWN : COOLDOWN[level - 1];
        swing = 0.18f;
        swingAngle = MathUtils.atan2Deg360(y - pos.y, x - pos.x);
        facingRight = x > pos.x;
    }

    /** A new room: it comes along at the hero's side. */
    public void reset() {
        placed = false;
    }

    public void drawShadow(SpriteBatch b, Draw d) {
        d.ellipse(b, pos.x, pos.y - 0.13f - (flies() ? 0.25f : 0f), 0.1f, 0.035f, Color.BLACK, 0.3f);
    }

    public void draw(SpriteBatch b, Draw d) {
        float face = facingRight ? 1 : -1;
        float hop = flies() ? MathUtils.sin(clock * 3f) * 0.04f : Math.abs(MathUtils.sin(clock * 9f)) * 0.025f;
        float x = pos.x;
        float y = pos.y - 0.13f + hop;
        if (flies()) { // little white wings, flapping
            float flap = MathUtils.sin(clock * 14f) * 15f;
            for (int side = -1; side <= 1; side += 2) {
                d.rotRect(b, x + side * 0.1f, y + 0.17f, 0.2f, 0.09f, side * (25f + flap), Draw.OUTLINE_COLOR, 1f);
                d.rotRect(b, x + side * 0.1f, y + 0.17f, 0.18f, 0.07f, side * (25f + flap), WING, 1f);
            }
        }
        if (level >= 5) { // a cape behind
            d.rect(b, x - 0.07f - face * 0.02f, y + 0.02f, 0.14f, 0.16f, CAPE);
        }
        Color body = level >= 6 ? GOLD : TIN;
        // a tin can body on two little bolt feet
        d.rect(b, x - 0.06f, y, 0.04f, 0.04f, TIN_DARK);
        d.rect(b, x + 0.02f, y, 0.04f, 0.04f, TIN_DARK);
        d.rect(b, x - 0.085f, y + 0.035f, 0.17f, 0.17f, Draw.OUTLINE_COLOR);
        d.rect(b, x - 0.075f, y + 0.045f, 0.15f, 0.15f, body);
        d.rect(b, x - 0.075f, y + 0.09f, 0.15f, 0.02f, TIN_DARK);
        d.rect(b, x - 0.075f, y + 0.15f, 0.15f, 0.02f, TIN_DARK);
        // a head: a bottle cap - or a pot for a helmet
        float hy = y + 0.25f;
        d.circle(b, x, hy, 0.065f, Draw.OUTLINE_COLOR);
        d.circle(b, x, hy, 0.055f, level >= 6 ? GOLD : TIN);
        d.circle(b, x + face * 0.02f, hy, 0.012f, Draw.OUTLINE_COLOR);
        d.circle(b, x + face * 0.045f, hy, 0.012f, Draw.OUTLINE_COLOR);
        if (level >= 2) {
            d.rect(b, x - 0.07f, hy + 0.025f, 0.14f, 0.05f, level >= 6 ? GOLD : POT);
            d.rect(b, x - 0.09f, hy + 0.025f, 0.18f, 0.015f, level >= 6 ? GOLD : POT);
        }
        if (level >= 3 && !flies()) { // a pot-lid shield on the back arm
            d.circle(b, x - face * 0.09f, y + 0.11f, 0.06f, Draw.OUTLINE_COLOR);
            d.circle(b, x - face * 0.09f, y + 0.11f, 0.05f, LID);
            d.circle(b, x - face * 0.09f, y + 0.11f, 0.015f, TIN_DARK);
        }
        float hx = x + face * 0.09f;
        float hand = y + 0.11f;
        if (flies()) { // a little bow
            float aim = facingRight ? 0f : 180f;
            d.line(b, hx, hand, 0.14f, 0.02f, aim + 70f, WOOD);
            d.line(b, hx, hand, 0.14f, 0.02f, aim - 70f, WOOD);
            d.line(b, hx - face * 0.02f, hand - 0.13f, 0.26f, 0.008f, 90f, Color.WHITE);
            return;
        }
        // the sword: wooden at first, steel from level 4; it sweeps when it swings
        float angle = swing > 0 ? swingAngle + (swing / 0.18f - 0.5f) * 120f * face : (facingRight ? 60f : 120f);
        float length = level >= 4 ? 0.2f : 0.15f;
        d.line(b, hx, hand, length, 0.035f, angle, level >= 4 ? STEEL : WOOD);
        d.line(b, hx, hand, 0.04f, 0.05f, angle + 90f, level >= 4 ? GOLD : WOOD);
    }
}
