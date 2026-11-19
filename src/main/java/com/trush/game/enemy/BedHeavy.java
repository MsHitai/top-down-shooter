package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/** Lost toys with distinct, readable formations: closing jaws, cloth waves and an expanding square. */
public final class BedHeavy extends Enemy {
    public enum Kind {
        PEG_CROC("PegCroc", "f0cc92", 54f),
        QUILT_MOTH("QuiltMoth", "e4abd7", 44f),
        TUMBLE_DIE("TumbleDie", "a9e4ca", 60f);

        final String id;
        final Color color;
        final float hp;

        Kind(String id, String color, float hp) {
            this.id = id;
            this.color = Color.valueOf(color);
            this.hp = hp;
        }
    }

    private final Kind kind;
    private final Vector2 mark = new Vector2();
    private final Vector2 origin = new Vector2();
    private float timer = MathUtils.random(.9f, 1.8f);
    private boolean warning;
    private float heading;
    private int turn;
    private int threads;
    private float threadTimer;
    private float ribbonSide;

    public BedHeavy(float x, float y, Kind kind) {
        super(x, y, kind.hp);
        this.kind = kind;
        radius = .33f;
        flying = kind == Kind.QUILT_MOTH;
        score = 65;
    }

    @Override public String dexId() { return kind.id; }
    @Override public float feetY() { return pos.y - radius * .75f; }

    @Override protected void behave(float delta, GameWorld world) {
        if (threads > 0) {
            threadTimer -= delta;
            if (threadTimer <= 0) {
                wavePair(world);
                threads--;
                threadTimer += .11f;
            }
            return; // wings stay planted until the whole ribbon has left
        }
        timer -= delta;
        if (!warning) {
            // An occasional slow reposition, never a rush towards the hero.
            if (timer > .35f || !canSeePlayer || !world.onScreen(pos.x, pos.y, .5f)) reposition(delta, world);
            if (timer > 0 || !canSeePlayer || !world.onScreen(pos.x, pos.y, .5f)) return;
            origin.set(pos);
            mark.set(targetX(world), targetY(world));
            if (!world.hasDecoy()) mark.mulAdd(world.getPlayer().getVelocity(), kind == Kind.PEG_CROC ? .65f : .45f);
            Arena.now.clamp(mark, .2f);
            heading = MathUtils.atan2Deg360(mark.y - pos.y, mark.x - pos.x);
            ribbonSide = turn % 2 == 0 ? 1f : -1f;
            // A full side aims at the hero; the escape doorway is ninety degrees to the side.
            if (kind == Kind.TUMBLE_DIE) heading += turn % 2 == 0 ? 90f : -90f;
            warning = true;
            timer = .6f;
            return;
        }
        if (timer > 0) return;
        switch (kind) {
            case PEG_CROC -> snap(world);
            case QUILT_MOTH -> {
                threads = 8;
                wavePair(world);
                threads--;
                threadTimer = .11f;
            }
            case TUMBLE_DIE -> square(world);
        }
        world.sfx().play("pop", .45f);
        warning = false;
        turn++;
        timer = kind == Kind.TUMBLE_DIE ? 1.65f : 1.2f;
    }

    private void reposition(float delta, GameWorld world) {
        if (kind == Kind.PEG_CROC) {
            // Walk to a useful firing range and flank furniture, without trying to touch the hero.
            keepDistance(delta, world, 2.8f, 1.1f);
            return;
        }
        float angle = switch (kind) {
            case PEG_CROC -> aimAngle + (turn % 2 == 0 ? 90f : -90f);
            case QUILT_MOTH -> aimAngle + 110f * (turn % 2 == 0 ? 1 : -1);
            case TUMBLE_DIE -> turn * 90f;
        };
        // Off screen, slowly enter view so a distant survivor cannot strand the fight.
        if (!canSeePlayer || !world.onScreen(pos.x, pos.y, .5f)) {
            if (!flying) {
                keepDistance(delta, world, 3.4f, .95f);
                return;
            }
            angle = aimAngle;
        } else if (flying && distanceToPlayer(world) > 3.6f) {
            angle = aimAngle;
        }
        float distance = delta * (flying ? .9f : .65f);
        float x = pos.x + MathUtils.cosDeg(angle) * distance;
        float y = pos.y + MathUtils.sinDeg(angle) * distance;
        if (flying ? Arena.now.contains(x, y, radius) : world.walkable(pos.x, pos.y, x, y, radius * .8f)) {
            pos.set(x, y);
            walkTime += delta;
        }
    }

    private Bullet shoot(GameWorld world, Vector2 start, float angle, float speed, Bullet.Shape shape) {
        Bullet shot = world.spawnEnemyBullet(start.x, start.y, angle, speed * world.getDifficulty());
        shot.shape = shape;
        shot.color.set(kind.color);
        shot.life = 3.5f;
        return shot;
    }

    private Vector2 jaw(int side) {
        Vector2 hinge = new Vector2(origin).lerp(mark, .52f);
        hinge.add(-MathUtils.sinDeg(heading) * side * .95f, MathUtils.cosDeg(heading) * side * .95f);
        Arena.now.clamp(hinge, .12f);
        return hinge;
    }

    private void snap(GameWorld world) {
        for (int side = -1; side <= 1; side += 2) {
            Vector2 middle = jaw(side);
            float a = MathUtils.atan2Deg360(middle.y - origin.y, middle.x - origin.x);
            Vector2 beyond = new Vector2(mark).mulAdd(new Vector2(mark).sub(middle).nor(), 1.4f);
            for (int i = 0; i < 4; i++) {
                Bullet tooth = shoot(world, origin, a, 4.1f, Bullet.Shape.TRIANGLE);
                tooth.radius = .075f;
                tooth.delay = i * .1f;
                tooth.followCourse(new Vector2[]{origin.cpy(), middle.cpy(), mark.cpy(), beyond.cpy()});
            }
        }
    }

    private void wavePair(GameWorld world) {
        float ux = MathUtils.cosDeg(heading), uy = MathUtils.sinDeg(heading);
        float targetDistance = origin.dst(mark);
        for (int side = -1; side <= 1; side += 2) {
            Vector2[] course = new Vector2[41];
            for (int i = 0; i < course.length; i++) {
                float forward = i * .16f;
                float open = Math.min(1f, forward / .8f);
                // One ribbon passes through the locked aim point. The safe lane is beside it.
                float wave = .28f * (MathUtils.sin(forward * 3f) - MathUtils.sin(targetDistance * 3f));
                float lateral = side * (.28f + .3f * open) + (wave + ribbonSide * .58f) * open;
                course[i] = new Vector2(origin.x + ux * forward - uy * lateral,
                        origin.y + uy * forward + ux * lateral);
            }
            Bullet thread = shoot(world, course[0], heading, 3.3f, Bullet.Shape.BALL);
            if (side < 0) thread.color.set(.6f, .86f, .87f, 1f);
            thread.radius = .07f;
            thread.followCourse(course);
        }
    }

    private void square(GameWorld world) {
        float ux = MathUtils.cosDeg(heading), uy = MathUtils.sinDeg(heading);
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                if (Math.abs(x) != 2 && Math.abs(y) != 2) continue;
                if (x == 2 && Math.abs(y) < 2) continue; // the lit side has a three-bullet-wide doorway
                Vector2 outward = new Vector2(ux * x - uy * y, uy * x + ux * y);
                Vector2 start = new Vector2(origin).mulAdd(outward, .11f);
                Bullet pip = shoot(world, start, outward.angleDeg(), outward.len() * 1.25f, Bullet.Shape.BALL);
                pip.radius = .075f;
            }
        }
    }

    @Override public void drawFloor(SpriteBatch b, Draw d) {
        if (!warning || !alive) return;
        if (kind == Kind.PEG_CROC) {
            for (int side = -1; side <= 1; side += 2) {
                Vector2 middle = jaw(side);
                guide(b, d, origin, middle);
                guide(b, d, middle, mark);
            }
            d.ring(b, mark.x, mark.y, .2f, kind.color, .6f);
        } else if (kind == Kind.TUMBLE_DIE) {
            // A bright broken side announces the square's safe doorway.
            float ux = MathUtils.cosDeg(heading), uy = MathUtils.sinDeg(heading);
            for (int side = -1; side <= 1; side += 2) {
                d.line(b, origin.x + ux * .48f - uy * side * .28f,
                        origin.y + uy * .48f + ux * side * .28f,
                        .17f, .035f, heading + 90f, Color.GOLD, .9f);
            }
        }
    }

    private void guide(SpriteBatch b, Draw d, Vector2 from, Vector2 to) {
        d.line(b, from.x, from.y, from.dst(to), .018f,
                MathUtils.atan2Deg360(to.y - from.y, to.x - from.x), kind.color, .45f);
    }

    @Override protected void drawBody(SpriteBatch b, Draw d) {
        float animation = warning || threads > 0 || timer < .8f ? 0f : walkTime;
        d.cast.centered(b, kind.id, pos.x, pos.y, animation, facingRight(), warning || threads > 0,
                flash > 0, c(Color.WHITE), 1.48f, 1.48f, 1f);
    }
}
