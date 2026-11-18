package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/** The larger lost toys. Only the die places hazards directly on the floor. */
public final class BedHeavy extends Enemy {
    public enum Kind {
        PEG_CROC("PegCroc", "d5a062", 54f),
        QUILT_MOTH("QuiltMoth", "cc98cf", 44f),
        TUMBLE_DIE("TumbleDie", "8edac7", 60f);

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
    private float timer = MathUtils.random(.5f, 1.1f);
    private boolean warning;
    private float heading;
    private int turn;
    private Bullet cocoon;

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
        if (cocoon != null && !cocoon.alive) {
            if (!cocoon.blanked && cocoon.life <= 0) {
                // A flying cocoon opens in midair; hitting cover snuffs it out.
                for (int i = 0; i < 8; i++) {
                    Bullet feather = shoot(world, cocoon.prev, heading + i * 45f, 3.3f, Bullet.Shape.FEATHER);
                    feather.curve = 33f;
                    feather.life = 1.45f;
                }
            }
            cocoon = null;
        }
        timer -= delta;
        if (!warning) {
            keepDistance(delta, world, kind == Kind.PEG_CROC ? 2.6f : 3.6f, flying ? 2.7f : 2.2f);
            if (timer > 0 || !canSeePlayer) return;
            mark.set(targetX(world), targetY(world));
            if (!world.hasDecoy()) {
                mark.mulAdd(world.getPlayer().getVelocity(), kind == Kind.TUMBLE_DIE ? .32f : .26f);
            }
            Arena.now.clamp(mark, kind == Kind.TUMBLE_DIE ? 1f : .2f);
            heading = leadAngle(world, 5.5f * world.getDifficulty(), .6f);
            if (kind == Kind.TUMBLE_DIE) {
                timer = .58f;
                warning = true;
                return;
            }
        }
        if (timer <= 0) {
            switch (kind) {
                case PEG_CROC -> snap(world);
                case QUILT_MOTH -> shedFeathers(world);
                case TUMBLE_DIE -> castPips(world);
            }
            world.sfx().play("pop", .55f);
            warning = false;
            turn++;
            timer = kind == Kind.TUMBLE_DIE ? 1.7f : 1.15f;
        }
    }

    private Bullet shoot(GameWorld world, Vector2 start, float angle, float speed, Bullet.Shape shape) {
        Bullet shot = world.spawnEnemyBullet(start.x, start.y, angle, speed * world.getDifficulty());
        shot.shape = shape;
        shot.color.set(kind.color);
        return shot;
    }

    private Vector2 jaw(int side) {
        Vector2 hinge = new Vector2(mark).lerp(pos, .52f);
        hinge.add(-MathUtils.sinDeg(heading) * side * .82f, MathUtils.cosDeg(heading) * side * .82f);
        Arena.now.clamp(hinge, .12f);
        return hinge;
    }

    private void snap(GameWorld world) {
        for (int side = -1; side <= 1; side += 2) {
            Vector2 middle = jaw(side);
            float a = MathUtils.atan2Deg360(middle.y - pos.y, middle.x - pos.x);
            Bullet tooth = shoot(world, pos, a, 6.8f, Bullet.Shape.TRIANGLE);
            tooth.radius = .105f;
            tooth.followCourse(new Vector2[]{pos.cpy(), middle, mark.cpy()});
        }
    }

    private void shedFeathers(GameWorld world) {
        if (turn % 3 == 2) {
            cocoon = shoot(world, pos, heading, 3.8f, Bullet.Shape.POMPOM);
            cocoon.radius = .15f;
            cocoon.life = .64f;
            return;
        }
        // The wings shed five feathers to one side of the sight line. The comb changes hands each time.
        for (int i = 0; i < 5; i++) {
            Bullet feather = shoot(world, pos, heading + (turn % 2 == 0 ? 1 : -1) * (i * 13 - 8),
                    3.4f + i * .45f, Bullet.Shape.FEATHER);
            feather.curve = turn % 2 == 0 ? -34f : 34f;
            feather.life = 1.8f;
        }
    }

    private Vector2 pip(int i) {
        if (i == 0) return mark.cpy();
        return new Vector2(mark.x + (i % 2 == 0 ? .67f : -.67f),
                mark.y + (i <= 2 ? (i == 1 ? .67f : -.67f) : (i == 3 ? -.67f : .67f)));
    }

    private int pips() { return turn % 2 == 0 ? 3 : 5; }

    private void castPips(GameWorld world) {
        for (int i = 0; i < pips(); i++) {
            Bullet dot = shoot(world, pip(i), 0, 0f, Bullet.Shape.BALL);
            dot.radius = .18f;
            dot.life = .82f;
        }
    }

    @Override public void drawFloor(SpriteBatch b, Draw d) {
        if (!warning || !alive) return;
        if (kind == Kind.TUMBLE_DIE) {
            for (int i = 0; i < pips(); i++) {
                Vector2 p = pip(i);
                d.ring(b, p.x, p.y, .20f, kind.color, .86f);
            }
        }
    }

    @Override protected void drawBody(SpriteBatch b, Draw d) {
        d.cast.centered(b, kind.id, pos.x, pos.y, walkTime, facingRight(), false, flash > 0, c(Color.WHITE),
                1.48f, 1.48f, 1f);
    }
}
