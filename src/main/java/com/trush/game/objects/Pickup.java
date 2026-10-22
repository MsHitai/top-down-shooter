package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.trush.game.launcher.ETGFanfic;

/** Something lying on the floor that the player collects by walking over it. */
public class Pickup {

    public enum Type { HEART, AMMO, BLANK, GUN, KEY }

    private static final Color AMMO_BOX = Color.valueOf("3c8d3f");
    private static final Color AMMO_DARK = Color.valueOf("1f4a22");
    private static final Color BRASS = Color.valueOf("e6c35c");
    private static final Color BLANK_GLOW = Color.valueOf("4fa4ff");
    private static final Color BULLET_TIP = Color.valueOf("c96b2c");

    public final Type type;
    public final Weapon weapon;
    public final Vector2 pos = new Vector2();
    public boolean alive = true;
    private float time = MathUtils.random(10f);
    /** Drop "pop" animation. */
    private float popTimer = 0.4f;

    public Pickup(Type type, float x, float y) {
        this(type, x, y, null);
    }

    public Pickup(Type type, float x, float y, Weapon weapon) {
        this.type = type;
        this.weapon = weapon;
        pos.set(x, y);
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
            case GUN: {
                float pulse = 0.5f + 0.5f * MathUtils.sin(time * 5f);
                game.draw.circle(b, x, y, 0.25f, Color.GOLD, 0.12f + 0.12f * pulse);
                game.draw.line(b, x - weapon.getGunLength() / 2f, y + 0.02f, weapon.getGunLength(),
                        weapon.getGunThickness(), 0, weapon.getGunColor());
                game.draw.rotRect(b, x - weapon.getGunLength() / 2f + 0.03f, y - 0.04f, 0.05f, 0.1f, -15,
                        weapon.getGunColor(), 1f);
                game.text(weapon.getName(), x, y + 0.34f, 0.32f, Color.GOLD, Align.center);
                break;
            }
        }
    }
}
