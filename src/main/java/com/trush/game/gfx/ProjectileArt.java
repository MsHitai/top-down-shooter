package com.trush.game.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;

/** Tinted silhouettes and neutral highlights shared by moving shots and their attack previews. */
public final class ProjectileArt implements Disposable {
    public enum Part { STAR, CLOUD, FEATHER, NOTE, ROCKET, WING, SHARD, SWORD, PETAL, PILLOW,
        SHARK_CLOSED, SHARK_OPEN, SHARK_GAPE, SHARK_TAIL, CARTRIDGE }

    private static final int CELL = 32;
    private final ActorInk ink;
    private final Texture sheet = new Texture("assets/sprites/projectile_parts.png");

    public ProjectileArt(ActorInk ink) {
        this.ink = ink;
        sheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    }

    public void star(SpriteBatch b, float x, float y, float radius, float angle, Color color, float alpha) {
        draw(b, Part.STAR, x, y, radius * 2f, radius * 2f, angle, false, color, alpha);
    }

    public void draw(SpriteBatch b, Part part, float x, float y, float width, float height,
                     float angle, boolean flipY, Color color, float alpha) {
        draw(b, part, x, y, width, height, angle, flipY, color, alpha, Color.WHITE);
    }

    public void draw(SpriteBatch b, Part part, float x, float y, float width, float height,
                     float angle, boolean flipY, Color color, float alpha, Color highlight) {
        float left = x - width / 2f, bottom = y - height / 2f;
        float opacity = color.a * alpha;
        float extent = Math.max(width, height) * 0.71f;
        ink.bounds(b, x - extent, y - extent, 2f * extent, 2f * extent);
        int source = part.ordinal() * CELL;
        b.setColor(color.r, color.g, color.b, opacity);
        b.draw(sheet, left, bottom, width / 2f, height / 2f, width, height, 1, 1, angle,
                source, 0, CELL, CELL, false, flipY);
        b.setColor(highlight.r, highlight.g, highlight.b, opacity);
        b.draw(sheet, left, bottom, width / 2f, height / 2f, width, height, 1, 1, angle,
                source, CELL, CELL, CELL, false, flipY);
        b.setColor(Color.WHITE);
    }

    public void shark(SpriteBatch b, float x, float y, float width, float height,
                      float angle, float gape, Color tint, float alpha) {
        Part jaw = gape > 0.65f ? Part.SHARK_GAPE : gape > 0.21f ? Part.SHARK_OPEN : Part.SHARK_CLOSED;
        draw(b, jaw, x, y, width, height, angle, MathUtils.cosDeg(angle) < 0f, tint, alpha);
    }

    @Override
    public void dispose() {
        sheet.dispose();
    }
}
