package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * Skyscraper enemies after ETG's cubes: a block that sits still until you line up with it across or
 * down the room, then slides straight at you until it hits something (touching it hurts).
 * <ul>
 *   <li>the Mountain Cube is ice: quick and slippery;</li>
 *   <li>the Lead Cube is slower and heavier, and when it slams into a wall it bursts a ring of
 *       bullets.</li>
 * </ul>
 */
public class Cube extends Enemy {

    private static final Color ICE = Color.valueOf("9ad8f5");
    private static final Color ICE_DARK = Color.valueOf("5aa8d0");
    private static final Color LEAD = Color.valueOf("7a7e88");
    private static final Color LEAD_DARK = Color.valueOf("50545e");
    private static final Color RIVET = Color.valueOf("b8bcc4");

    /** How closely the player must be lined up (across or down) with its edge before it slides. */
    private static final float ALIGN = 0.12f;

    private final boolean lead;
    private final float size;
    private final Vector2 slide = new Vector2();
    private final Vector2 next = new Vector2();
    private float rest = MathUtils.random(0.6f, 1.2f);
    private boolean sliding;
    private float wobble;

    private Cube(float x, float y, boolean lead) {
        super(x, y, lead ? 60f : 40f);
        this.lead = lead;
        size = lead ? 0.78f : 0.66f; // big, heavy blocks (they used to look like dice)
        radius = size * 0.5f;
        speed = lead ? 3.0f : 4.6f;
        score = lead ? 45 : 35;
    }

    public static Cube mountain(float x, float y) {
        return new Cube(x, y, false);
    }

    public static Cube lead(float x, float y) {
        return new Cube(x, y, true);
    }

    @Override
    public float feetY() {
        return pos.y - size * 0.5f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        knock.setZero(); // far too heavy to shove around
        wobble = Math.max(0f, wobble - delta);
        if (sliding) {
            // feel ahead along the whole leading face: a big block mustn't graze past a corner
            for (int side = -1; side <= 1; side++) {
                next.set(pos).mulAdd(slide, radius + speed * delta + 0.02f)
                        .add(-slide.y * side * radius * 0.8f, slide.x * side * radius * 0.8f);
                if (!Arena.now.contains(next.x, next.y, 0.02f) || !world.pointIsFree(next.x, next.y, 0.05f)) {
                    slam(world);
                    return;
                }
            }
            pos.mulAdd(slide, speed * delta);
            return;
        }
        rest -= delta;
        if (rest > 0 || !canShoot(world)) {
            return;
        }
        float dx = world.getPlayer().centerX() - pos.x;
        float dy = world.getPlayer().hitboxY() - pos.y;
        float reach = radius + ALIGN; // lined up with any part of the block, not just its middle
        if (Math.abs(dy) < reach && Math.abs(dx) > radius) {
            slide.set(Math.signum(dx), 0);
        } else if (Math.abs(dx) < reach && Math.abs(dy) > radius) {
            slide.set(0, Math.signum(dy));
        } else {
            return;
        }
        sliding = true;
        wobble = 0.15f;
        world.sfx().play("dodge", lead ? 0.7f : 0.5f);
    }

    /** Hit a wall or furniture: stop, rest a moment; the Lead Cube bursts a ring of bullets. */
    private void slam(GameWorld world) {
        sliding = false;
        rest = lead ? 1.1f : 0.7f;
        wobble = 0.2f;
        world.shake(lead ? 0.25f : 0.1f);
        world.particles().burst(pos.x + slide.x * radius, pos.y + slide.y * radius, 8, 0.2f, 1f, 0.3f, 0.04f,
                lead ? LEAD_DARK : Color.WHITE);
        if (lead) {
            int n = world.getDifficulty() > 1.25f ? 12 : 10;
            float start = MathUtils.random(360f);
            for (int i = 0; i < n; i++) {
                shoot(world, start + 360f * i / n, 2.0f * world.getDifficulty()).color.set(RIVET);
            }
            world.sfx().play("boom", 0.4f);
        } else {
            world.sfx().play("hit", 0.5f);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float k = size / 0.5f; // details were drawn for a 0.5 block
        float squash = wobble > 0 ? MathUtils.sin(wobble * 60f) * 0.03f * k : 0f;
        float w = size + squash;
        float h = size - squash;
        float x = pos.x - w / 2f;
        float y = feetY();
        Color body = lead ? LEAD : ICE;
        Color dark = lead ? LEAD_DARK : ICE_DARK;
        d.rect(b, x - 0.025f, y - 0.025f, w + 0.05f, h + 0.05f, Draw.OUTLINE_COLOR);
        d.rect(b, x, y, w, h, c(body), lead ? 1f : 0.85f);
        d.rect(b, x, y, w, h * 0.22f, c(dark));
        d.rect(b, x + w - 0.07f * k, y, 0.07f * k, h, c(dark));
        if (lead) {
            for (float rx : new float[]{x + 0.06f * k, x + w - 0.12f * k}) {
                for (float ry : new float[]{y + h * 0.3f, y + h - 0.08f * k}) {
                    d.circle(b, rx + 0.03f * k, ry, 0.022f * k, RIVET);
                }
            }
        } else {
            d.rect(b, x + 0.05f * k, y + h - 0.12f * k, 0.12f * k, 0.05f * k, Color.WHITE, 0.8f); // a shine on the ice
            d.rect(b, x + 0.05f * k, y + h - 0.2f * k, 0.05f * k, 0.06f * k, Color.WHITE, 0.6f);
        }
        // an angry face
        float fy = y + h * 0.55f;
        drawEyes(b, d, pos.x - 0.02f * k, fy, 0.07f * k, 0.04f * k);
        float browY = fy + 0.06f * k;
        d.rotRect(b, pos.x - 0.1f * k, browY, 0.09f * k, 0.025f * k, -20f, Draw.OUTLINE_COLOR, 1f);
        d.rotRect(b, pos.x + 0.06f * k, browY, 0.09f * k, 0.025f * k, 20f, Draw.OUTLINE_COLOR, 1f);
        d.rect(b, pos.x - 0.07f * k, y + h * 0.3f, 0.12f * k, 0.025f * k, Draw.OUTLINE_COLOR); // a grim mouth
    }
}
