package com.trush.game.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.Disposable;

import java.nio.IntBuffer;

/** Inks an assembled actor at native viewport resolution. Keeps every original pose and attachment.
 * Only the occupied rectangle is composited; the floor, bullets and UI never pass through this layer. */
public final class ActorInk implements Disposable {
    private final ShaderProgram shader;
    private FrameBuffer buffer;
    private final IntBuffer values = BufferUtils.newIntBuffer(16);
    private final Matrix4 projection = new Matrix4(), transform = new Matrix4();
    private final Matrix4 screen = new Matrix4(), combined = new Matrix4();
    private final Vector3 point = new Vector3();
    private final Color color = new Color();
    private ShaderProgram previousShader;
    private int vx, vy, width, height, framebuffer;
    private int sx, sy, sw, sh;
    private int src, dst, srcAlpha, dstAlpha;
    private boolean scissor, blending, active;
    private float minX, minY, maxX, maxY;
    private EnemyMaterial surface = EnemyMaterial.PLAIN;
    private boolean ghost;
    private float lightX, lightY, lightRadiusX, lightRadiusY;
    private float pixelX, pixelY;

    public ActorInk() {
        shader = new ShaderProgram(Gdx.files.internal("assets/shaders/actor.vert"),
                Gdx.files.internal("assets/shaders/actor.frag"));
        if (!shader.isCompiled()) throw new IllegalStateException("Actor ink: " + shader.getLog());
    }

    /** Only solid coloured body material gets surface lighting, never warnings, eyes or ink. */
    public boolean material(Color color, float alpha) {
        return active && (alpha * color.a > 0.98f || ghost && alpha * color.a > 0.3f && color.g > 0.6f)
                && color.r + color.g + color.b > 0.60f
                && Math.min(color.r, Math.min(color.g, color.b)) < 0.98f
                && !(color.r > 0.9f && color.r > color.g * 1.8f + 0.15f);
    }

    public EnemyMaterial surface() {
        return surface;
    }

    /** The batch is drawing on entry and remains drawing throughout the caller's body rendering. */
    public void begin(SpriteBatch batch, float x, float y, float radius, String kind) {
        surface = EnemyMaterial.forKind(kind);
        ghost = kind.equals("GhostKin");
        if (active) throw new IllegalStateException("Nested actor drawing");
        Gdx.gl.glGetIntegerv(GL20.GL_VIEWPORT, values);
        vx = values.get(0); vy = values.get(1);
        width = values.get(2); height = values.get(3);
        Gdx.gl.glGetIntegerv(GL20.GL_FRAMEBUFFER_BINDING, values);
        framebuffer = values.get(0);
        scissor = Gdx.gl.glIsEnabled(GL20.GL_SCISSOR_TEST);
        if (scissor) {
            Gdx.gl.glGetIntegerv(GL20.GL_SCISSOR_BOX, values);
            sx = values.get(0); sy = values.get(1); sw = values.get(2); sh = values.get(3);
        }
        projection.set(batch.getProjectionMatrix());
        transform.set(batch.getTransformMatrix());
        combined.set(projection).mul(transform);
        point.set(x, y, 0).prj(combined);
        lightX = (point.x + 1) * 0.5f;
        lightY = (point.y + 1) * 0.5f;
        point.set(x + radius * 1.5f, y, 0).prj(combined);
        float xx = Math.abs((point.x + 1) * 0.5f - lightX);
        float xy = Math.abs((point.y + 1) * 0.5f - lightY);
        point.set(x, y + radius * 1.5f, 0).prj(combined);
        lightRadiusX = Math.max(1f / width, Math.max(xx, Math.abs((point.x + 1) * 0.5f - lightX)));
        lightRadiusY = Math.max(1f / height, Math.max(xy, Math.abs((point.y + 1) * 0.5f - lightY)));
        // Match the authored family/kin art: 64 source pixels per world unit.
        // Anchor the grid on the actor, so walking never crawls across a screen-fixed grid.
        float sourcePixel = Math.max(1f, (float) Math.hypot(xx * width, xy * height)
                / (radius * 1.5f * Sprites.DETAIL_PPU));
        pixelX = sourcePixel / width;
        pixelY = sourcePixel / height;
        color.set(batch.getColor());
        previousShader = batch.getShader();
        src = batch.getBlendSrcFunc(); dst = batch.getBlendDstFunc();
        srcAlpha = batch.getBlendSrcFuncAlpha(); dstAlpha = batch.getBlendDstFuncAlpha();
        blending = batch.isBlendingEnabled();
        batch.end();
        if (buffer == null || buffer.getWidth() != width || buffer.getHeight() != height) {
            if (buffer != null) buffer.dispose();
            buffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
            buffer.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        buffer.begin();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glClearColor(0, 0, 0, 0);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        if (scissor) {
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
            Gdx.gl.glScissor(sx - vx, sy - vy, sw, sh);
        }
        // Store premultiplied RGB and correct coverage, including translucent ghosts.
        batch.enableBlending();
        batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA,
                GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.begin();
        minX = width; minY = height; maxX = 0; maxY = 0;
        active = true;
    }

    /** Bounds from Draw's primitives, projected with the current body/death/portrait transform. */
    public void bounds(SpriteBatch batch, float x, float y, float w, float h) {
        if (!active) return;
        combined.set(batch.getProjectionMatrix()).mul(batch.getTransformMatrix());
        corner(x, y); corner(x + w, y); corner(x, y + h); corner(x + w, y + h);
    }

    private void corner(float x, float y) {
        point.set(x, y, 0).prj(combined);
        float px = (point.x + 1) * width * 0.5f;
        float py = (point.y + 1) * height * 0.5f;
        minX = Math.min(minX, px); minY = Math.min(minY, py);
        maxX = Math.max(maxX, px); maxY = Math.max(maxY, py);
    }

    public void end(SpriteBatch batch, boolean flash) {
        active = false;
        batch.end();
        buffer.end();
        Gdx.gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebuffer);
        Gdx.gl.glViewport(vx, vy, width, height);
        if (scissor) {
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
            Gdx.gl.glScissor(sx, sy, sw, sh);
        } else Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        float stroke = Math.max(pixelX * width, pixelY * height);
        float padding = stroke * 1.5f + 1f; // snapped coverage can extend half a pixel beyond the body
        int x0 = Math.max(0, (int) Math.floor(minX - padding));
        int y0 = Math.max(0, (int) Math.floor(minY - padding));
        int x1 = Math.min(width, (int) Math.ceil(maxX + padding));
        int y1 = Math.min(height, (int) Math.ceil(maxY + padding));
        if (x1 > x0 && y1 > y0) {
            batch.setProjectionMatrix(screen.setToOrtho2D(0, 0, width, height));
            batch.setTransformMatrix(combined.idt());
            batch.setShader(shader);
            batch.setColor(Color.WHITE);
            batch.setBlendFunctionSeparate(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA,
                    GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.begin();
            shader.setUniformf("u_step", pixelX, pixelY);
            shader.setUniformf("u_origin", lightX, lightY);
            shader.setUniformf("u_bounds", lightX - lightRadiusX, lightY - lightRadiusY,
                    lightRadiusX * 2, lightRadiusY * 2);
            shader.setUniformf("u_flash", flash ? 1f : 0f);
            batch.draw(buffer.getColorBufferTexture(), x0, y0, x1 - x0, y1 - y0,
                    (float) x0 / width, (float) y0 / height, (float) x1 / width, (float) y1 / height);
            batch.end();
        }
        batch.setShader(previousShader);
        batch.setProjectionMatrix(projection);
        batch.setTransformMatrix(transform);
        batch.setColor(color);
        batch.setBlendFunctionSeparate(src, dst, srcAlpha, dstAlpha);
        if (!blending) batch.disableBlending();
        batch.begin();
    }

    @Override
    public void dispose() {
        if (buffer != null) buffer.dispose();
        shader.dispose();
    }
}
