package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;
import com.trush.game.launcher.ETGFanfic;

/** Something lying on the floor that the player collects by walking over it. */
public class Pickup {

    public enum Type { HEART, HALF_HEART, HEART_CONTAINER, AMMO, BLANK, GUN, KEY, ITEM, COIN, JUNK }

    private static final Color AMMO_BOX = Color.valueOf("3c8d3f");
    private static final Color AMMO_DARK = Color.valueOf("1f4a22");
    private static final Color BRASS = Color.valueOf("e6c35c");
    private static final Color BLANK_GLOW = Color.valueOf("4fa4ff");
    private static final Color BULLET_TIP = Color.valueOf("c96b2c");
    private static final Color TIN = Color.valueOf("a8adb8");
    private static final Color TIN_DARK = Color.valueOf("6e7380");
    private static final Color RUST = Color.valueOf("a0522d");

    public final Type type;
    public final Weapon weapon;
    /** For ITEM pickups. */
    public final Item item;
    public final Vector2 pos = new Vector2();
    public boolean alive = true;
    private float time = MathUtils.random(10f);
    /** Drop "pop" animation. */
    private float popTimer = 0.4f;
    /** For COIN pickups: what it's worth (a big coin is {@link Coins#BIG}). */
    public int value = 1;
    /** For JUNK: the rare piece that's alive (it wakes the Junk Knight up) - see {@link JunkKnight#LIVING_CHANCE}. */
    public boolean living;
    /** An item the player just swapped out: not picked up again until they step away from it. */
    public boolean waitForLeave;

    public Pickup(Type type, float x, float y) {
        this(type, x, y, null);
    }

    public Pickup(Type type, float x, float y, Weapon weapon) {
        this.type = type;
        this.weapon = weapon;
        this.item = null;
        pos.set(x, y);
    }

    /** An item lying on the floor. */
    public Pickup(Item item, float x, float y) {
        this.type = Type.ITEM;
        this.weapon = null;
        this.item = item;
        pos.set(x, y);
    }

    /** A coin worth {@code value}, spinning on the floor. */
    public static Pickup coin(float x, float y, int value) {
        Pickup p = new Pickup(Type.COIN, x, y);
        p.value = value;
        return p;
    }

    public void update(float delta) {
        time += delta;
        popTimer = Math.max(0, popTimer - delta);
    }

    public void draw(SpriteBatch b, ETGFanfic game) {
        float bob = MathUtils.sin(time * 4f) * 0.03f + popTimer * 0.6f;
        float x = pos.x;
        float y = pos.y + bob;
        game.draw.ellipse(b, pos.x, pos.y - 0.1f, 0.12f, 0.035f, Color.BLACK, 0.35f);

        switch (type) {
            case HEART: {
                float s = 0.26f;
                game.draw.texture(b, game.draw.heartFull, x - s / 2f, y - s * 0.45f, s, s * 8f / 9f);
                break;
            }
            case HEART_CONTAINER: { // a big heart in a golden glow: one more max heart
                float s = 0.38f;
                float pulse = 0.5f + 0.5f * MathUtils.sin(time * 4f);
                game.draw.circle(b, x, y, 0.3f, Color.GOLD, 0.15f + 0.15f * pulse);
                game.draw.ring(b, x, y, 0.26f, Color.GOLD, 0.6f + 0.3f * pulse);
                game.draw.texture(b, game.draw.heartFull, x - s / 2f, y - s * 0.45f, s, s * 8f / 9f);
                game.text("+1", x, y + 0.42f, 0.3f, Color.GOLD, Align.center);
                break;
            }
            case COIN: { // spins: face-on, turning, edge-on, turning back (a big one just glints)
                TextureRegion r = game.sprites.prop(value > 1 ? "coin_big" : "coin_" + (int) (time * 8f) % 4);
                float w = Sprites.w(r);
                float h = Sprites.h(r);
                b.draw(r, x - w / 2f, y - h / 2f, w, h);
                break;
            }
            case HALF_HEART: {
                float s = 0.26f;
                game.draw.texture(b, game.draw.heartHalf, x - s / 2f, y - s * 0.45f, s, s * 8f / 9f);
                break;
            }
            case BLANK: {
                float s = 0.2f;
                game.draw.circle(b, x, y, 0.17f, BLANK_GLOW, 0.2f + 0.1f * MathUtils.sin(time * 6));
                game.draw.texture(b, game.draw.blankIcon, x - s / 2f, y - s / 2f, s, s);
                break;
            }
            case AMMO: {
                game.draw.rect(b, x - 0.13f, y - 0.09f, 0.26f, 0.16f, AMMO_DARK);
                game.draw.rect(b, x - 0.12f, y - 0.08f, 0.24f, 0.14f, AMMO_BOX);
                for (int i = 0; i < 3; i++) {
                    float bx = x - 0.08f + i * 0.07f;
                    game.draw.rect(b, bx, y + 0.04f, 0.035f, 0.08f, BRASS);
                    game.draw.circle(b, bx + 0.0175f, y + 0.12f, 0.0175f, BULLET_TIP);
                }
                break;
            }
            case KEY: {
                float s = 0.3f;
                float pulse = 0.5f + 0.5f * MathUtils.sin(time * 5f);
                game.draw.circle(b, x, y, 0.16f, Color.GOLD, 0.1f + 0.1f * pulse);
                game.draw.texture(b, game.draw.keyIcon, x - s / 2f, y - s * 0.25f, s, s * 0.5f);
                break;
            }
            case ITEM: {
                float pulse = 0.5f + 0.5f * MathUtils.sin(time * 5f);
                game.draw.circle(b, x, y, 0.27f, Color.SKY, 0.12f + 0.12f * pulse);
                float s = 0.4f;
                b.draw(game.sprites.item(item.sprite), x - s / 2f, y - s / 2f, s, s);
                game.text(item.title, x, y + 0.36f, 0.3f, Color.SKY, Align.center);
                break;
            }
            case JUNK: { // a bent tin can, a spring and a bolt: the Junk Knight's next piece
                game.draw.rect(b, x - 0.09f, y - 0.07f, 0.12f, 0.15f, Draw.OUTLINE_COLOR);
                game.draw.rect(b, x - 0.08f, y - 0.06f, 0.1f, 0.13f, TIN);
                game.draw.rect(b, x - 0.08f, y - 0.01f, 0.1f, 0.02f, TIN_DARK);
                game.draw.line(b, x - 0.08f, y + 0.07f, 0.1f, 0.02f, -15f, TIN_DARK);
                for (int i = 0; i < 4; i++) {
                    game.draw.ring(b, x + 0.08f, y - 0.05f + i * 0.035f, 0.025f, RUST, 1f);
                }
                game.draw.circle(b, x + 0.02f, y - 0.09f, 0.025f, TIN_DARK);
                if (living) { // two little eyes peeking out of the can - and it twitches
                    float blink = MathUtils.sin(time * 2f) > 0.95f ? 0.004f : 0.016f;
                    game.draw.rect(b, x - 0.06f, y + 0.02f, 0.016f, blink, Draw.OUTLINE_COLOR);
                    game.draw.rect(b, x - 0.025f, y + 0.02f, 0.016f, blink, Draw.OUTLINE_COLOR);
                }
                break;
            }
            case GUN: {
                float pulse = 0.5f + 0.5f * MathUtils.sin(time * 5f);
                game.draw.circle(b, x, y, 0.25f, Color.GOLD, 0.12f + 0.12f * pulse);
                game.sprites.gun(weapon.getSprite()).drawIcon(b, x, y, 1.4f);
                game.text(weapon.getName(), x, y + 0.34f, 0.32f, Color.GOLD, Align.center);
                break;
            }
        }
    }
}
