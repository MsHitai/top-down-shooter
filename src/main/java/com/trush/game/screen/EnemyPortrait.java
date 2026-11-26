package com.trush.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.trush.game.enemy.Boss;
import com.trush.game.enemy.Enemy;
import com.trush.game.gfx.Draw;

/** Draws the in-game body at the origin of a portrait frame. */
final class EnemyPortrait {
    private final Matrix4 saved = new Matrix4();
    private final Matrix4 placed = new Matrix4();

    static float bossScale(Boss boss, Enemy body) {
        // The sofa, building and throne extend well beyond their collision circle.
        float shrink = boss == Boss.ALLEY_CAT || boss == Boss.WALKING_BUILDING || boss == Boss.SKUNK_KING ? 0.6f : 1f;
        return MathUtils.clamp(0.16f / body.radius, 0.2f, 0.9f) * shrink;
    }

    boolean draw(SpriteBatch batch, Draw draw, Enemy body, float x, float y, float scale) {
        body.pos.setZero();
        saved.set(batch.getTransformMatrix());
        placed.set(saved).translate(x, y, 0).scale(scale, scale, 1);
        batch.setTransformMatrix(placed);
        try {
            body.drawPortrait(batch, draw, 270f);
            return true;
        } catch (RuntimeException ex) {
            Gdx.app.error("EnemyPortrait", "can't draw " + body.getClass().getSimpleName(), ex);
            return false;
        } finally {
            batch.setTransformMatrix(saved);
        }
    }
}
