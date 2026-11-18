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
    private boolean secondPuff;
    private static final float DASH_TIME = 0.60f;

    private float dashSpeed() {
        return switch (kind) {
            case BEETLE -> 7.8f;
            case MARBLE -> 6.4f;
            default -> 5.7f;
        };
    }

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
            float previous = charge;
            charge -= delta;
            float x = pos.x, y = pos.y;
            float distance = Math.min(delta, previous) * dashSpeed();
            // A charge ends on the first bed leg or wall.
            float nextX = x + MathUtils.cosDeg(direction) * distance;
            float nextY = y + MathUtils.sinDeg(direction) * distance;
            if (world.walkable(x, y, nextX, nextY, radius)) pos.set(nextX, nextY);
            if (kind == Kind.MARBLE && previous > DASH_TIME / 2 && charge <= DASH_TIME / 2) {
                fire(world, direction - 90f);
                fire(world, direction + 90f);
            }
            if (pos.dst2(x, y) < distance * distance * 0.4f || charge <= 0) {
                charge = 0;
                if (kind == Kind.MARBLE) {
                    for (int i = 0; i < 7; i++) fire(world, direction + 360f * i / 7);
                }
            }
            return;
        }
        wait -= delta;
        keepDistance(delta, world, kind == Kind.MITE ? 3.4f : 2.5f, kind == Kind.BEETLE ? 2.3f : 2.7f);
        if (wait > 0 || !canSeePlayer) return;
        if (kind == Kind.MITE) {
            // A slow pair opens a lane. The quick second puff catches a runner who stays in it.
            direction = leadAngle(world, 6.2f * world.getDifficulty(), secondPuff ? .8f : .2f);
            if (secondPuff) {
                fire(world, direction);
            } else {
                fire(world, direction - 17f);
                fire(world, direction + 17f);
            }
            secondPuff = !secondPuff;
            wait = secondPuff ? .26f : 1.36f;
        } else {
            direction = leadAngle(world, dashSpeed(), kind == Kind.BEETLE ? .65f : .30f);
            wait = kind == Kind.BEETLE ? .78f : 1.15f;
            charge = DASH_TIME;
            world.sfx().play("pop", 0.35f);
        }
    }

    private void fire(GameWorld world, float angle) {
        Bullet shot = world.spawnEnemyBullet(pos.x, pos.y, angle, (kind == Kind.MITE && secondPuff ? 6.2f : 4.3f) * world.getDifficulty());
        shot.color.set(kind.color);
        if (kind == Kind.MITE) shot.shape = Bullet.Shape.CLOUD;
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        if (kind == Kind.DUST_KIT) {
            float y = pos.y + (charge > 0 ? 0.09f * MathUtils.sin(charge / DASH_TIME * MathUtils.PI) : 0f);
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
        if (kind == Kind.BEETLE) {
            d.cast.centered(b, kind.id, pos.x, pos.y, walkTime, facingRight(), charge > 0,
                    flash > 0, c(Color.WHITE), 1.04f, 1.04f, 1f);
            return;
        }
        for (int i = -1; i <= 1; i++) {
            float y = pos.y + i * 0.10f;
            d.line(b, pos.x - 0.08f, y, 0.21f, 0.04f, 175f + i * 20f, FEET);
            d.line(b, pos.x + 0.08f, y, 0.21f, 0.04f, 5f - i * 20f, FEET);
        }
        d.ellipse(b, pos.x, pos.y, radius, radius * 1.12f, kind.color, 1f);
        drawEyes(b, d, pos.x, pos.y - 0.035f, 0.08f, 0.05f);
    }
}
