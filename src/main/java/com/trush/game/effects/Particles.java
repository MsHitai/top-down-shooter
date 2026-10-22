package com.trush.game.effects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.trush.game.gfx.Draw;

/** Cheap pooled circle particles: muzzle flashes, sparks, death bursts, dust. */
public class Particles {

    private static class P implements Pool.Poolable {
        float x, y, vx, vy, life, maxLife, size, drag;
        final Color color = new Color();

        @Override
        public void reset() {
        }
    }

    private final Array<P> active = new Array<>(false, 256);
    private final Pool<P> pool = new Pool<P>(256) {
        @Override
        protected P newObject() {
            return new P();
        }
    };

    public void spawn(float x, float y, float vx, float vy, float life, float size, Color color, float drag) {
        if (active.size > 1500) {
            return;
        }
        P p = pool.obtain();
        p.x = x;
        p.y = y;
        p.vx = vx;
        p.vy = vy;
        p.life = life;
        p.maxLife = life;
        p.size = size;
        p.drag = drag;
        p.color.set(color);
        active.add(p);
    }

    public void burst(float x, float y, int count, float minSpeed, float maxSpeed, float life, float size, Color color) {
        for (int i = 0; i < count; i++) {
            float a = MathUtils.random(360f);
            float s = MathUtils.random(minSpeed, maxSpeed);
            spawn(x, y, MathUtils.cosDeg(a) * s, MathUtils.sinDeg(a) * s,
                    life * MathUtils.random(0.6f, 1.2f), size * MathUtils.random(0.6f, 1.3f), color, 4f);
        }
    }

    public void update(float delta) {
        for (int i = active.size - 1; i >= 0; i--) {
            P p = active.get(i);
            p.life -= delta;
            if (p.life <= 0) {
                active.removeIndex(i);
                pool.free(p);
                continue;
            }
            float damp = Math.max(0f, 1f - p.drag * delta);
            p.vx *= damp;
            p.vy *= damp;
            p.x += p.vx * delta;
            p.y += p.vy * delta;
        }
    }

    public void draw(SpriteBatch b, Draw d) {
        for (P p : active) {
            float k = p.life / p.maxLife;
            d.circle(b, p.x, p.y, p.size * (0.3f + 0.7f * k), p.color, Math.min(1f, k * 1.5f));
        }
    }

    public void clear() {
        pool.freeAll(active);
        active.clear();
    }
}
