package com.trush.game.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

/** Authored rat poses, with clean fur planes and the ending's fading mark. */
public final class RatArt implements Disposable {
    private static final float PPU = 64f;
    private final Texture sheet = new Texture("assets/sprites/rat.png");

    public RatArt() {
        sheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    public void draw(SpriteBatch batch, float x, float y, float size, float walkTime,
                     boolean right, boolean marked, float markFade, boolean flash, Color tint) {
        int frame = walkTime == 0 ? 4 : (int) (walkTime * 14f) % 4;
        int row = flash ? (marked ? 11 : 10) : marked ? 1 + Math.round(MathUtils.clamp(markFade, 0, 1) * 8) : 0;
        batch.setColor(tint);
        batch.draw(sheet, x - (right ? 24f : 16f) / PPU * size, y - 11f / PPU * size,
                40f / PPU * size, 28f / PPU * size, frame * 40, row * 28, 40, 28, !right, false);
        batch.setColor(Color.WHITE);
    }

    @Override
    public void dispose() {
        sheet.dispose();
    }
}
