package com.trush.game.gfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.Viewport;

/** Renders scenery and actors on the same 32-pixel-per-unit grid as the sprite assets.
 * Text and menus are drawn afterwards at the window's resolution. */
public final class PixelScene implements Disposable {
    private final FrameBuffer buffer = new FrameBuffer(Pixmap.Format.RGBA8888, 224, 160, false);
    private final TextureRegion image;
    private final Matrix4 screen = new Matrix4().setToOrtho2D(0, 0, 7, 5);
    private final Matrix4 previous = new Matrix4();

    public PixelScene() {
        Texture texture = buffer.getColorBufferTexture();
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        image = new TextureRegion(texture);
        image.flip(false, true); // OpenGL framebuffers have their origin at the bottom left.
    }

    /** Call outside a SpriteBatch begin/end pair. */
    public void begin() {
        buffer.begin();
        ScreenUtils.clear(Color.BLACK);
    }

    /** Restores the window's letterboxed viewport, including on high-DPI displays. */
    public void end(Viewport viewport) {
        buffer.end();
        viewport.apply();
    }

    /** Presents the scene in screen space, then restores the caller's camera for labels. */
    public void draw(SpriteBatch batch) {
        previous.set(batch.getProjectionMatrix());
        batch.setProjectionMatrix(screen);
        batch.setColor(Color.WHITE);
        // The scene is already composited over black. Its accumulated alpha must not
        // dim translucent lighting and effects for a second time on presentation.
        boolean blending = batch.isBlendingEnabled();
        batch.disableBlending();
        batch.draw(image, 0, 0, 7, 5);
        if (blending) batch.enableBlending();
        batch.setProjectionMatrix(previous);
    }

    @Override
    public void dispose() {
        buffer.dispose();
    }
}
