package com.trush.game.enemy;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.objects.Prop;
import com.trush.game.world.GameWorld;
import com.trush.game.world.Room;

/** Short visits to nearby cover; firing always requires stepping into a real clear sight line. */
final class EnemyCover {
    private Prop prop;
    private final Vector2 hide = new Vector2();
    private final Vector2 peek = new Vector2();
    private final Vector2 step = new Vector2();
    private float search;
    private float remaining;
    private float hiding;

    void fired() {
        if (prop != null) hiding = 0.8f;
    }

    boolean move(Enemy enemy, float delta, GameWorld world) {
        search -= delta;
        if (prop == null && search <= 0) {
            search = 5f;
            select(enemy, world);
        }
        if (prop == null) return false;
        remaining -= delta;
        float px = enemy.targetX(world), py = enemy.targetY(world);
        if (!prop.alive || remaining <= 0 || Vector2.dst(enemy.pos.x, enemy.pos.y, px, py) < 1.2f
                || world.hasLineOfSight(hide.x, hide.y, px, py, GameWorld.SIGHT_THICKNESS)
                    && !prop.canFlip()) {
            prop = null;
            search = 4f;
            return false;
        }
        Vector2 goal = hiding > 0 ? hide : peek;
        if (!world.walkable(enemy.pos.x, enemy.pos.y, goal.x, goal.y, enemy.radius)) {
            prop = null;
            search = 4f;
            return false;
        }
        float distance = enemy.pos.dst(goal);
        if (distance > 0.04f) {
            step.set(goal).sub(enemy.pos).setLength(Math.min(distance, enemy.speed * delta));
            enemy.pos.add(step);
            enemy.walkTime += delta;
        } else if (hiding > 0) {
            if (prop.canFlip()) {
                float dx = prop.cx - hide.x, dy = prop.cy - hide.y;
                prop.flip(Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? Room.E : Room.W) : (dy > 0 ? Room.N : Room.S));
                world.sfx().play("flip", 0.7f);
            }
            hiding -= delta;
        }
        return true;
    }

    private void select(Enemy enemy, GameWorld world) {
        float best = 2.5f * 2.5f;
        float px = enemy.targetX(world), py = enemy.targetY(world);
        for (int index = 0; index < world.getRoom().props.size; index++) {
            Prop p = world.getRoom().props.get(index);
            if (!p.alive || !(p.canFlip() || p.blocksBullets()) || p.burning || p.mimic) continue;
            Rectangle r = p.solid;
            float margin = enemy.radius + 0.3f;
            float cx = r.x + r.width / 2, cy = r.y + r.height / 2;
            // Try the far side of each footprint, with a short step out along either edge.
            for (int side = 0; side < 4; side++) {
                float dx = MathUtils.cosDeg(side * 90f), dy = MathUtils.sinDeg(side * 90f);
                float hx = cx + dx * (r.width / 2 + margin);
                float hy = cy + dy * (r.height / 2 + margin);
                if ((hx - cx) * (px - cx) + (hy - cy) * (py - cy) >= 0) continue;
                float d = enemy.pos.dst2(hx, hy);
                if (d >= best || !world.pointIsFree(hx, hy, enemy.radius)
                        || !world.walkable(enemy.pos.x, enemy.pos.y, hx, hy, enemy.radius)) continue;
                if (!p.canFlip() && world.hasLineOfSight(hx, hy, px, py, GameWorld.SIGHT_THICKNESS)) continue;
                boolean occupied = false;
                for (int n = 0; n < world.getEnemies().size; n++) {
                    Enemy other = world.getEnemies().get(n);
                    if (other != enemy && other.alive && other.pos.dst2(hx, hy) < 0.36f) occupied = true;
                }
                if (occupied) continue;
                for (int sign = -1; sign <= 1; sign += 2) {
                    float ex = hx - dy * sign * (r.width / 2 + margin + 0.3f);
                    float ey = hy + dx * sign * (r.height / 2 + margin + 0.3f);
                    if (!world.pointIsFree(ex, ey, enemy.radius)
                            || !world.walkable(hx, hy, ex, ey, enemy.radius)
                            || !world.hasLineOfSight(ex, ey, px, py, GameWorld.SIGHT_THICKNESS)) continue;
                    prop = p;
                    hide.set(hx, hy);
                    peek.set(ex, ey);
                    best = d;
                    hiding = 0.7f;
                    remaining = 6f;
                }
            }
        }
    }
}
