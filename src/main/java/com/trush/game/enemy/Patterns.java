package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/**
 * Bullet formations that fly as one block (every bullet has the same velocity, only their starting
 * spots differ): a solid triangle with its tip leading - the shape the owner's son drew for the
 * Messenger's sword - and a hollow square.
 */
final class Patterns {

    private Patterns() {
    }

    /**
     * A filled triangle of bullets, tip first, flying at {@code angle}: row 0 is the tip (one bullet),
     * each row behind it one bullet wider, {@code spacing} apart.
     */
    static void triangle(GameWorld world, float x, float y, float angle, float speed, int rows, float spacing,
                         Bullet.Shape shape, Color color) {
        float ux = MathUtils.cosDeg(angle);
        float uy = MathUtils.sinDeg(angle);
        for (int row = 0; row < rows; row++) {
            float back = row * spacing * 0.87f;
            for (int i = 0; i <= row; i++) {
                float side = (i - row / 2f) * spacing;
                Bullet b = world.spawnEnemyBullet(x - ux * back - uy * side, y - uy * back + ux * side, angle, speed);
                b.shape = shape;
                b.color.set(color);
            }
        }
    }

    /** A hollow square of bullets, {@code size} bullets to a side, {@code spacing} apart, flying at {@code angle}. */
    static void square(GameWorld world, float x, float y, float angle, float speed, int size, float spacing,
                       Bullet.Shape shape, Color color) {
        float ux = MathUtils.cosDeg(angle);
        float uy = MathUtils.sinDeg(angle);
        float half = (size - 1) / 2f;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i != 0 && j != 0 && i != size - 1 && j != size - 1) continue; // hollow
                float f = (i - half) * spacing;
                float s = (j - half) * spacing;
                Bullet b = world.spawnEnemyBullet(x + ux * f - uy * s, y + uy * f + ux * s, angle, speed);
                b.shape = shape;
                b.color.set(color);
            }
        }
    }
}
