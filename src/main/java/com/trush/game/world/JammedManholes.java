package com.trush.game.world;

import java.util.Arrays;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/** A staggered sequence of lids that runs down the room and back. */
final class JammedManholes {
    static final float WARNING = 0.85f;
    private static final float OPEN_TIME = 1.55f;
    private final Room room;
    private final float[] openAt;
    private int index;
    private int direction = 1;
    private float nextWarning = 1.2f;

    JammedManholes(Room room) {
        this.room = room;
        openAt = new float[room.trapHatches.size];
        Arrays.fill(openAt, Float.POSITIVE_INFINITY);
    }

    void update(float clock, float progress, TrapRoom.Hazards world) {
        for (int i = 0; i < openAt.length; i++) {
            if (clock < openAt[i]) continue;
            Rectangle hatch = room.trapHatches.get(i);
            if (clock >= openAt[i] + OPEN_TIME) {
                room.pits.removeValue(hatch, true);
                openAt[i] = Float.POSITIVE_INFINITY;
                world.sound("door", 0.48f);
            } else if (!room.pits.contains(hatch, true)) {
                room.pits.add(hatch);
                world.sound("flip", 0.75f);
            }
        }
        if (clock >= nextWarning && openAt.length > 0 && openAt[index] == Float.POSITIVE_INFINITY) {
            openAt[index] = clock + WARNING;
            world.sound("rattle", 0.7f);
            nextWarning = clock + MathUtils.lerp(1.3f, 0.72f, progress);
            if (openAt.length > 1) {
                if (index + direction < 0 || index + direction == openAt.length) direction = -direction;
                index += direction;
            }
        }
    }

    float lidHop(Rectangle hatch, float clock) {
        int i = room.trapHatches.indexOf(hatch, true);
        if (i < 0 || openAt[i] - clock > WARNING + 0.001f || clock >= openAt[i]) return 0f;
        return 0.08f * Math.abs(MathUtils.sin(clock * 32f));
    }

    void close() {
        room.pits.removeAll(room.trapHatches, true);
    }
}
