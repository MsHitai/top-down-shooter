package com.trush.game.launcher;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.BufferUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Films the game (dev builds, -Dfluffy.record=clip.mp4): every frame is drawn as exactly {@link #FRAME} of game
 * time and piped raw into ffmpeg, so the video plays smoothly however slowly the frames were drawn - for trailers.
 * With the bot, -Dfluffy.botSpeed=2 makes its two 1/60 s steps one frame. -Dfluffy.recordSeconds=N stops (and quits)
 * after N seconds of video; -Dfluffy.recordSkip=N leaves the first N seconds out.
 */
public final class Recorder {

    private static final String OUT = Dev.text("record");
    public static final boolean ON = OUT != null;
    public static final int FPS = 30;
    public static final float FRAME = 1f / FPS;
    private static final int SECONDS = Dev.number("recordSeconds", 0);
    private static final int SKIP = Dev.number("recordSkip", 0);

    private static Process ffmpeg;
    private static OutputStream pipe;
    private static ByteBuffer pixels;
    private static byte[] row;
    private static int width, height, frames;

    private Recorder() {
    }

    /** After the frame is drawn: hands it to ffmpeg. */
    public static void capture() {
        frames++;
        if (frames <= SKIP * FPS) {
            return;
        }
        int w = Gdx.graphics.getBackBufferWidth(), h = Gdx.graphics.getBackBufferHeight();
        try {
            if (pipe == null) {
                start(w, h);
            }
            if (w != width || h != height) {
                return; // (the window was resized: the video keeps its size)
            }
            pixels.clear();
            Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);
            Gdx.gl.glReadPixels(0, 0, w, h, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);
            for (int y = 0; y < h; y++) { // (ffmpeg flips it: rows go out bottom first)
                pixels.position(y * w * 4);
                pixels.get(row);
                pipe.write(row);
            }
        } catch (IOException e) {
            throw new RuntimeException("recording failed", e);
        }
        if (SECONDS > 0 && frames - SKIP * FPS >= SECONDS * FPS) {
            Gdx.app.exit();
        }
    }

    private static void start(int w, int h) throws IOException {
        width = w;
        height = h;
        pixels = BufferUtils.newByteBuffer(w * h * 4);
        row = new byte[w * 4];
        ffmpeg = new ProcessBuilder("ffmpeg", "-y", "-loglevel", "error",
                "-f", "rawvideo", "-pix_fmt", "rgba", "-s", w + "x" + h, "-r", String.valueOf(FPS), "-i", "-",
                "-vf", "vflip", "-c:v", "libx264", "-preset", "fast", "-crf", "16", "-pix_fmt", "yuv420p", OUT)
                .redirectErrorStream(true).redirectOutput(new File(OUT + ".log")).start();
        pipe = ffmpeg.getOutputStream();
    }

    /** On the way out: lets ffmpeg finish the file. */
    public static void stop() {
        if (pipe == null) {
            return;
        }
        try {
            pipe.close();
            ffmpeg.waitFor();
        } catch (IOException | InterruptedException e) {
            Gdx.app.error("Recorder", "couldn't finish the video", e);
        }
        pipe = null;
    }
}
