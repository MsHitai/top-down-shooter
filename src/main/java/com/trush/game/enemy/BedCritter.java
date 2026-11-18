package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/** The small things living between the bed legs. */
public final class BedCritter extends Enemy {
    public enum Kind {
        MITE("DustMite", "ac9bb4", 15f),
        BEETLE("WindUpBeetle", "bf955e", 29f),
        MARBLE("LostMarble", "62cbd1", 24f),
        DUST_KIT("DustBunnyKit", "ac9bb4", 12f);

        final String id;
        final Color color;
        final float health;

        Kind(String id, String color, float health) {
            this.id = id;
            this.color = Color.valueOf(color);
            this.health = health;
        }
    }

    private final Kind kind;
    private float wait = MathUtils.random(0.55f, 1.25f);
    private float direction;
    private float charge;

    public BedCritter(float x, float y, Kind kind) {
        super(x, y, kind.health);
        this.kind = kind;
        radius = kind == Kind.MITE ? 0.16f : 0.22f;
        score = kind == Kind.MITE ? 15 : 25;
        squishable = kind == Kind.DUST_KIT;
    }

    @Override
    public String dexId() {
        return kind.id;
    }

    @Override
    public float feetY() {
        return pos.y - radius * 0.6f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        if (charge > 0) {
            charge -= delta;
            float x = pos.x, y = pos.y;
            float distance = delta * (kind == Kind.BEETLE ? 4.2f : 3.1f);
            // A charge ends on the first bed leg or wall.
            float nextX = x + MathUtils.cosDeg(direction) * distance;
            float nextY = y + MathUtils.sinDeg(direction) * distance;
            if (world.walkable(x, y, nextX, nextY, radius)) pos.set(nextX, nextY);
            if (pos.dst2(x, y) < distance * distance * 0.4f || charge <= 0) {
                charge = 0;
                if (kind == Kind.MARBLE) {
                    for (int i = 0; i < 7; i++) fire(world, direction + 360f * i / 7);
                }
            }
            return;
        }
        wait -= delta;
        if (wait > 0.4f || !canSeePlayer) {
            keepDistance(delta, world, kind == Kind.MITE ? 2.4f : 1.9f, 1.4f);
            direction = aimAngle;
            if (!canSeePlayer) wait = Math.max(wait, 0.45f);
        }
        if (wait > 0) return;
        wait = kind == Kind.MITE ? 1.4f : 1.8f;
        if (kind == Kind.MITE) {
            for (int i = -1; i <= 1; i++) fire(world, direction + i * 16f);
        } else {
            charge = 0.55f;
            world.sfx().play("pop", 0.35f);
        }
    }

    private void fire(GameWorld world, float angle) {
        Bullet shot = world.spawnEnemyBullet(pos.x, pos.y, angle, 2.9f * world.getDifficulty());
        shot.color.set(kind.color);
        if (kind == Kind.MITE) shot.shape = Bullet.Shape.CLOUD;
    }

    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (kind == Kind.MITE || wait > 0.4f || isSpawning() || !alive) return;
        d.line(b, pos.x, pos.y, 2.2f, 0.035f, direction, TELEGRAPH, 0.65f);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        if (kind == Kind.DUST_KIT) {
            float y = pos.y + (charge > 0 ? 0.09f * MathUtils.sin(charge / 0.55f * MathUtils.PI) : 0f);
            d.rotRect(b, pos.x - 0.10f, y + 0.18f, 0.07f, 0.27f, 18f, kind.color, 1f);
            d.rotRect(b, pos.x + 0.10f, y + 0.18f, 0.07f, 0.27f, -18f, kind.color, 1f);
            d.circle(b, pos.x, y, radius, kind.color);
            drawEyes(b, d, pos.x, y, 0.075f, 0.05f);
            return;
        }
        if (kind == Kind.MARBLE) {
            d.circle(b, pos.x, pos.y, radius, kind.color);
            d.ellipse(b, pos.x, pos.y, 0.09f, 0.20f, Color.TEAL, 0.8f);
            d.circle(b, pos.x - 0.07f, pos.y + 0.08f, 0.055f, Color.WHITE);
            return;
        }
        for (int i = -1; i <= 1; i++) {
            float y = pos.y + i * 0.10f;
            d.line(b, pos.x - 0.08f, y, 0.21f, 0.04f, 175f + i * 20f, FEET);
            d.line(b, pos.x + 0.08f, y, 0.21f, 0.04f, 5f - i * 20f, FEET);
        }
        d.ellipse(b, pos.x, pos.y, radius, radius * 1.12f, kind.color, 1f);
        if (kind == Kind.BEETLE) {
            d.rect(b, pos.x - 0.025f, pos.y + 0.03f, 0.05f, 0.22f, Color.GOLD);
            d.ring(b, pos.x, pos.y + 0.25f, 0.08f, Color.GOLD, 1f);
        }
        drawEyes(b, d, pos.x, pos.y - 0.035f, 0.08f, 0.05f);
    }
}
