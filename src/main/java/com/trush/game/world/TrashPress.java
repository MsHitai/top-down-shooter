package com.trush.game.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;

/** Two wall-mounted press heads. The bays on either side of their stroke stay open. */
final class TrashPress {
    private static final float WARNING = 1.1f;
    private static final float HOLD = 0.45f;
    private static final float RETRACT = 0.7f;
    private static final float HEAD_DEPTH = 0.28f;
    private static final Color BAY = Color.valueOf("495864");
    private static final Color BAY_RIM = Color.valueOf("8598a1");
    private static final Color PISTON = Color.valueOf("353b43");
    private static final Color STEEL = Color.valueOf("929ba1");
    private static final Color AMBER = Color.valueOf("f8b755");

    private final Arena arena;
    private final float width;
    private final float left;
    private final Rectangle strokeArea = new Rectangle();
    private boolean fromSouth;
    private boolean cycling;
    private float next = 1.2f;
    private float stroke;
    private float extendTime;
    private float depth = HEAD_DEPTH;

    TrashPress(Arena arena) {
        this.arena = arena;
        width = (arena.right - arena.left) * 0.55f;
        left = arena.centerX - width / 2f;
    }

    void update(float delta, float progress, TrapRoom.Hazards world) {
        if (!cycling) {
            next -= delta;
            if (next <= 0f) {
                cycling = true;
                fromSouth = !fromSouth;
                stroke = -WARNING;
                extendTime = MathUtils.lerp(0.95f, 0.58f, progress);
                world.sound("rattle", 0.9f);
            }
            return;
        }
        float before = stroke;
        stroke += delta;
        if (stroke <= 0f) return;
        if (before <= 0f) world.sound("door", 0.9f);
        if (before < extendTime && stroke >= extendTime) world.sound("boom", 0.55f);
        float retractAt = extendTime + HOLD;
        float extension = Math.min(stroke / extendTime, 1f) - Math.max(0f, (stroke - retractAt) / RETRACT);
        depth = MathUtils.lerp(HEAD_DEPTH, arena.top - arena.bottom, Math.max(0f, extension));
        if (stroke < retractAt + RETRACT) {
            strokeArea.set(left, fromSouth ? arena.bottom : arena.top - depth, width, depth);
            world.crush(strokeArea);
        } else {
            reset();
            next = MathUtils.lerp(1.1f, 0.55f, progress);
        }
    }

    void reset() {
        cycling = false;
        depth = HEAD_DEPTH;
    }

    void draw(SpriteBatch batch, Draw draw, Sprites sprites, float clock) {
        float height = arena.top - arena.bottom;
        float bayWidth = left - arena.left;
        draw.rect(batch, arena.left, arena.bottom, bayWidth, height, BAY, 0.5f);
        draw.rect(batch, left + width, arena.bottom, bayWidth, height, BAY, 0.5f);
        draw.rect(batch, left - 0.08f, arena.bottom, 0.08f, height, BAY_RIM, 0.6f);
        draw.rect(batch, left + width, arena.bottom, 0.08f, height, BAY_RIM, 0.6f);
        if (!cycling) {
            drawHead(batch, sprites, false, HEAD_DEPTH);
            drawHead(batch, sprites, true, HEAD_DEPTH);
            return;
        }
        if (depth < height - HEAD_DEPTH) drawHead(batch, sprites, !fromSouth, HEAD_DEPTH);
        float span = depth - HEAD_DEPTH;
        if (span > 0f) {
            float y = fromSouth ? arena.bottom : arena.top - span;
            draw.rect(batch, left, y, width, span, PISTON);
            for (float x = left + width * 0.2f; x < left + width; x += width * 0.3f) {
                draw.rect(batch, x, y, 0.16f, span, STEEL);
            }
        }
        drawHead(batch, sprites, fromSouth, depth);
        if (stroke < 0f) {
            float y = fromSouth ? arena.bottom + HEAD_DEPTH : arena.top - HEAD_DEPTH;
            float blink = 0.55f + 0.4f * MathUtils.sin(clock * 22f);
            draw.circle(batch, left + 0.18f, y, 0.09f, AMBER, blink);
            draw.circle(batch, left + width - 0.18f, y, 0.09f, AMBER, blink);
        }
    }

    private void drawHead(SpriteBatch batch, Sprites sprites, boolean south, float travel) {
        TextureRegion head = sprites.prop("trap_press");
        float y = south ? arena.bottom + travel - HEAD_DEPTH : arena.top - travel;
        batch.draw(head, left, y, width / 2f, HEAD_DEPTH / 2f, width, HEAD_DEPTH,
                1f, 1f, south ? 0f : 180f);
    }
}
