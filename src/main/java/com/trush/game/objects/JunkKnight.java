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
    /** A swing sweeps the sword through its arc in this long (and the bow's recoil lasts as long). */
    private static final float SWING_TIME = 0.26f;
    /** Half the arc a swing sweeps through, in degrees. */
    private static final float SWING_ARC = 80f;
    /** It raises the sword back this long before a swing, once the target is in reach. */
    private static final float WINDUP = 0.2f;

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
    /** Where the enemy it's after is (degrees), for the wind-up and the bow's aim. */
    private float targetAngle;
    /** It has an enemy: close enough to wind up (melee), or in sight (the bow). */
    private boolean engaged;
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
        engaged = enemy != null && (flies() || pos.dst(enemy) < REACH + 0.35f);
        if (enemy != null) {
            targetAngle = MathUtils.atan2Deg360(enemy.y - pos.y, enemy.x - pos.x);
        }
    }

    /** Close enough to its target to swing at it. */
    public boolean canReach(float x, float y, float radius) {
        return pos.dst(x, y) < REACH + radius;
    }

    /** Swung (or loosed an arrow) at (x, y): the cooldown starts and the sword sweeps. */
    public void attacked(float x, float y) {
        cooldown = flies() ? ARROW_COOLDOWN : COOLDOWN[level - 1];
        swing = SWING_TIME;
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

    /** 0 to 1 through the swing that's on, 0 when none is. */
    private float swingProgress() {
        return swing > 0 ? 1f - swing / SWING_TIME : 0f;
    }

    public void draw(SpriteBatch b, Draw d) {
        float face = facingRight ? 1 : -1;
        float hop = flies() ? MathUtils.sin(clock * 3f) * 0.04f : Math.abs(MathUtils.sin(clock * 9f)) * 0.025f;
        float x = pos.x;
        float y = pos.y - 0.13f + hop;
        if (swing > 0) { // a step into the blow (the bow kicks back instead)
            float push = MathUtils.sin(swingProgress() * MathUtils.PI) * (flies() ? -0.04f : 0.07f);
            x += MathUtils.cosDeg(swingAngle) * push;
            y += MathUtils.sinDeg(swingAngle) * push + (flies() ? 0f : Math.abs(push) * 0.4f);
        }
        if (flies()) { // little white wings, flapping
            float flap = MathUtils.sin(clock * 14f) * 15f;
            for (int side = -1; side <= 1; side += 2) {
                d.rotRect(b, x + side * 0.1f, y + 0.17f, 0.2f, 0.09f, side * (25f + flap), Draw.OUTLINE_COLOR, 1f);
                d.rotRect(b, x + side * 0.1f, y + 0.17f, 0.18f, 0.07f, side * (25f + flap), WING, 1f);
            }
        }
        boolean bowBehind = flies() && MathUtils.sinDeg(bowAim()) > 0.35f; // aiming up: the bow goes behind him
        if (bowBehind) {
            drawBow(b, d, x, y + 0.11f);
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
        float hand = y + 0.11f;
        if (flies()) {
            if (!bowBehind) {
                drawBow(b, d, x, hand);
            }
            return;
        }
        float hx = x + face * 0.09f;
        // the sword: wooden at first, steel from level 4. Raised back while it gets ready, then swept
        // through a wide arc across the enemy, a slash trailing behind
        float length = level >= 4 ? 0.2f : 0.15f;
        Color blade = level >= 4 ? STEEL : WOOD;
        float angle;
        if (swing > 0) {
            float eased = 1f - (float) Math.pow(1f - swingProgress(), 3);
            float from = swingAngle + SWING_ARC * face;
            angle = from - 2f * SWING_ARC * face * eased;
            drawSlash(b, d, hx, hand, length + 0.05f, from, angle, level >= 6 ? GOLD : Color.WHITE);
        } else if (engaged && cooldown < WINDUP) {
            float ready = 1f - Math.max(0f, cooldown) / WINDUP;
            angle = targetAngle + (SWING_ARC + 35f * ready) * face; // up over the shoulder
            if (ready > 0.85f) { // a glint on the blade just before
                d.circle(b, hx + MathUtils.cosDeg(angle) * length, hand + MathUtils.sinDeg(angle) * length,
                        0.03f, Color.WHITE, 0.9f);
            }
        } else {
            angle = facingRight ? 60f : 120f;
        }
        d.line(b, hx, hand, length, 0.035f, angle, blade);
        d.line(b, hx, hand, 0.04f, 0.05f, angle + 90f, level >= 4 ? GOLD : WOOD);
    }

    /** The slash trailing the sword: a fading crescent from where the swing began to the blade. */
    private void drawSlash(SpriteBatch b, Draw d, float hx, float hy, float radius, float from, float to, Color c) {
        float fade = swing / SWING_TIME;
        float width = 0.025f + 0.006f * level;
        int steps = 10;
        for (int i = 0; i <= steps; i++) {
            float a = MathUtils.lerp(from, to, i / (float) steps);
            float tail = 0.25f + 0.75f * i / steps; // thin at the start, full by the blade
            d.circle(b, hx + MathUtils.cosDeg(a) * radius, hy + MathUtils.sinDeg(a) * radius,
                    width * tail, c, 0.75f * fade * tail);
        }
    }

    /**
     * The level-7 bow, held out towards the enemy: the string comes back as the next arrow gets ready (one
     * nocked on it), then it twangs and the knight kicks back.
     */
    private void drawBow(SpriteBatch b, Draw d, float x, float hand) {
        float aim = bowAim();
        float cos = MathUtils.cosDeg(aim);
        float sin = MathUtils.sinDeg(aim);
        float gx = x + cos * 0.11f; // the grip
        float gy = hand + sin * 0.11f;
        float draw = engaged && swing <= 0 ? MathUtils.clamp(1f - cooldown / ARROW_COOLDOWN, 0f, 1f) : 0f;
        float bend = 70f - 12f * draw; // the limbs bend as it's drawn
        float limb = 0.14f;
        float ax = gx + MathUtils.cosDeg(aim + bend) * limb;
        float ay = gy + MathUtils.sinDeg(aim + bend) * limb;
        float bx = gx + MathUtils.cosDeg(aim - bend) * limb;
        float by = gy + MathUtils.sinDeg(aim - bend) * limb;
        float pull = 0.02f + 0.1f * draw; // the string's middle, pulled back past the grip
        float px = gx - cos * pull;
        float py = gy - sin * pull;
        if (swing > 0) { // just loosed: the string still quivering
            float quiver = MathUtils.sin(clock * 90f) * 0.012f * (swing / SWING_TIME);
            px += -sin * quiver;
            py += cos * quiver;
        }
        stringTo(b, d, px, py, ax, ay);
        stringTo(b, d, px, py, bx, by);
        if (draw > 0.15f) { // an arrow on the string
            d.line(b, px, py, 0.2f, 0.016f, aim, WOOD);
            d.line(b, px + cos * 0.2f, py + sin * 0.2f, 0.04f, 0.03f, aim, STEEL);
        }
        d.line(b, gx, gy, limb, 0.022f, aim + bend, WOOD);
        d.line(b, gx, gy, limb, 0.022f, aim - bend, WOOD);
    }

    /** Where the bow points: at the enemy while it has one, else the way it faces. */
    private float bowAim() {
        return engaged || swing > 0 ? targetAngle : (facingRight ? 0f : 180f);
    }

    private static void stringTo(SpriteBatch b, Draw d, float x, float y, float tx, float ty) {
        float dx = tx - x;
        float dy = ty - y;
        d.line(b, x, y, (float) Math.sqrt(dx * dx + dy * dy), 0.008f, MathUtils.atan2Deg360(dy, dx), Color.WHITE);
    }
}
