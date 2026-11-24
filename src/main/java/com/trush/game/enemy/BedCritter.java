package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/** A sneezer, one clockwork pursuer, a banking marble, and a little dust bunny.
 * Their rests are deliberately a touch different, so a room's volleys drift in and out of step. */
public final class BedCritter extends Enemy {
    private static final Color BANKED_GLASS = Color.valueOf("50a8b0");
    public enum Kind {
        MITE("DustMite", "d5b9db", 15f),
        BEETLE("WindUpBeetle", "f0cb81", 29f),
        MARBLE("LostMarble", "83e8e3", 24f),
        DUST_KIT("DustBunnyKit", "cbbadf", 12f);

        final String id;
        final Color color;
        final float health;

        Kind(String id, String color, float health) {
            this.id = id;
            this.color = Color.valueOf(color);
            this.health = health;
        }
    }

    private enum Phase { REST, MOVE, WINDUP, RECOVER }
    private final Kind kind;
    private final Vector2 bankTarget = new Vector2();
    private Phase phase = Phase.REST;
    private float timer = MathUtils.random(.35f, .85f);
    private float exhaustTimer;
    private float heading;
    private float attackHeading;
    private int turn;

    public BedCritter(float x, float y, Kind kind) {
        super(x, y, kind.health);
        this.kind = kind;
        radius = kind == Kind.MITE ? .16f : .22f;
        score = kind == Kind.MITE ? 15 : 25;
        squishable = kind == Kind.DUST_KIT;
    }

    /** Only this resident actively runs into the player. The director allows one at a time. */
    public boolean isPursuer() { return kind == Kind.BEETLE; }
    @Override public String dexId() { return kind.id; }
    @Override public float feetY() { return pos.y - radius * .6f; }

    @Override protected void behave(float delta, GameWorld world) {
        if (phase == Phase.REST && (!canSeePlayer || !world.onScreen(pos.x, pos.y, .3f))) {
            keepDistance(delta, world, 3.1f, .6f);
            return;
        }
        timer -= delta;
        switch (kind) {
            case MITE -> sneezer(delta, world);
            case BEETLE -> beetle(delta, world);
            case MARBLE, DUST_KIT -> sideways(delta, world);
        }
    }

    private void sneezer(float delta, GameWorld world) {
        if (phase == Phase.REST) {
            if (timer > .25f) keepDistance(delta, world, 3.1f, .55f);
            if (timer <= 0 && ready(world)) {
                phase = Phase.WINDUP;
                timer = .3f;
            }
        } else if (timer <= 0) {
            if (phase == Phase.WINDUP) {
                attackHeading = leadAngle(world, 4f * world.getDifficulty(), .55f);
                fireSneeze(this, world, attackHeading);
                step(world, attackHeading + 180f, .12f);
                world.sfx().play("hiss", .3f);
                phase = Phase.RECOVER;
                timer = .2f;
            } else {
                // The second sneeze threads the first fan's gaps without tracking a dodging hero,
                // skipping a different gap each time - a wall to walk through, not five lonely clouds.
                float[] gaps = {-31.5f, -10.5f, 10.5f, 31.5f};
                for (int i = 0; i < gaps.length; i++) {
                    if (i == turn % gaps.length) continue;
                    fire(world, attackHeading + gaps[i], 3.7f, Bullet.Shape.CLOUD);
                }
                turn++;
                rest(1.0f);
            }
        }
    }

    private void beetle(float delta, GameWorld world) {
        switch (phase) {
            case REST -> {
                if (timer <= 0 && ready(world)) {
                    phase = Phase.WINDUP;
                    timer = .3f;
                    heading = aimAngle;
                    world.sfx().play("reload", .3f);
                }
            }
            case WINDUP -> {
                if (timer <= 0) {
                    phase = Phase.MOVE;
                    timer = 1.9f;
                    exhaustTimer = .42f;
                }
            }
            case MOVE -> {
                // The turning circle makes a sidestep useful, rather than snapping back at the hero.
                float turnBy = MathUtils.clamp((aimAngle - heading + 540f) % 360f - 180f, -135f * delta, 135f * delta);
                heading += turnBy;
                exhaustTimer -= delta;
                if (exhaustTimer <= 0) {
                    fire(world, heading - 105f, 3.2f, Bullet.Shape.STAR);
                    fire(world, heading + 105f, 3.2f, Bullet.Shape.STAR);
                    exhaustTimer += .42f;
                }
                boolean touching = distanceToPlayer(world) < radius * .8f + Player.HITBOX_RADIUS;
                if (touching || timer <= 0 || !step(world, heading, 3.05f * delta)) {
                    phase = Phase.RECOVER;
                    timer = .3f;
                    attackHeading = aimAngle;
                }
            }
            case RECOVER -> {
                if (timer <= 0) {
                    for (int i = 0; i < 10; i++) fire(world, attackHeading + i * 36f, 3.5f, Bullet.Shape.STAR);
                    world.sfx().play("pop", .35f);
                    rest(1.2f);
                }
            }
        }
    }

    private void sideways(float delta, GameWorld world) {
        if (phase == Phase.REST) {
            if (timer <= 0 && ready(world)) {
                heading = aimAngle + (turn++ % 2 == 0 ? 90f : -90f);
                phase = Phase.MOVE;
                timer = kind == Kind.MARBLE ? .4f : .28f;
            }
        } else if (phase == Phase.MOVE) {
            boolean moved = step(world, heading, (kind == Kind.MARBLE ? 1.3f : 1.8f) * delta);
            if (!moved || timer <= 0) {
                phase = Phase.WINDUP;
                timer = .3f;
            }
    } else if (timer <= 0) {
        if (phase == Phase.RECOVER) {
            if (kind == Kind.MARBLE) {
                // The marble that rolls off the bed late: aimed where the hero is now, not where they were.
                fire(world, aimAngle, 4f, Bullet.Shape.BALL);
                rest(1.1f);
            } else {
                for (int i = 0; i < 2; i++) fire(world, attackHeading + (i - .5f) * 18f, 3.4f, Bullet.Shape.DUST);
                rest(1.35f);
            }
            return;
        }
            attackHeading = leadAngle(world, 4.2f * world.getDifficulty(), .55f);
            if (kind == Kind.MARBLE) {
                bankTarget.set(targetX(world), targetY(world));
                if (!world.hasDecoy()) bankTarget.mulAdd(world.getPlayer().getVelocity(), .35f);
                for (int side = 0; side < 2; side++) {
                    for (int i = -1; i <= 1; i += 2) {
                        Bullet bank = fire(world, bankAngle(side) + i * 8f, 4.3f, Bullet.Shape.BALL);
                        bank.bounces = 1;
                        bank.bouncedColor = BANKED_GLASS;
                        bank.bubble = true;
                        bank.life = 4.5f;
                    }
                }
                // Direct fire keeps the hero moving before the banked fans return.
                for (int i = -1; i <= 1; i++) fire(world, attackHeading + i * 24f, 4.2f, Bullet.Shape.BALL);
                phase = Phase.RECOVER;
                timer = .35f;
            } else {
                for (int i = -1; i <= 1; i++) fire(world, attackHeading + i * 24f, 3.6f, Bullet.Shape.DUST);
                phase = Phase.RECOVER;
                timer = .2f;
            }
            world.sfx().play("pop", .35f);
        }
    }

    private float bankAngle(int side) {
        float wall = side == 0 ? Arena.now.left : Arena.now.right;
        return MathUtils.atan2Deg360(bankTarget.y - pos.y, 2f * wall - bankTarget.x - pos.x);
    }

    private boolean ready(GameWorld world) {
        return canSeePlayer && world.onScreen(pos.x, pos.y, .3f);
    }

    private void rest(float seconds) {
        phase = Phase.REST;
        timer = seconds + MathUtils.random(.25f);
    }

    private boolean step(GameWorld world, float angle, float distance) {
        float x = pos.x + MathUtils.cosDeg(angle) * distance;
        float y = pos.y + MathUtils.sinDeg(angle) * distance;
        if (!world.walkable(pos.x, pos.y, x, y, radius * .8f)) return false;
        pos.set(x, y);
        walkTime += distance;
        return true;
    }

    static void fireSneeze(Enemy shooter, GameWorld world, float heading) {
        for (int i = -2; i <= 2; i++) {
            fire(world, shooter.pos, Kind.MITE.color, heading + i * 21f, 4f, Bullet.Shape.CLOUD);
        }
    }

    private Bullet fire(GameWorld world, float angle, float speed, Bullet.Shape shape) {
        return fire(world, pos, kind.color, angle, speed, shape);
    }

    private static Bullet fire(GameWorld world, Vector2 origin, Color color, float angle, float speed, Bullet.Shape shape) {
        Bullet shot = world.spawnEnemyBullet(origin.x, origin.y, angle, speed * world.getDifficulty());
        shot.color.set(color);
        shot.shape = shape;
        shot.life = 3.2f;
        return shot;
    }

    @Override protected void drawBody(SpriteBatch b, Draw d) {
        boolean preparing = phase == Phase.WINDUP || phase == Phase.RECOVER;
        float scale = kind == Kind.MITE ? .84f : kind == Kind.DUST_KIT ? .9f : 1.04f;
        float hop = kind == Kind.DUST_KIT && phase == Phase.MOVE
                ? .17f * MathUtils.sin(MathUtils.clamp(timer / .35f, 0f, 1f) * MathUtils.PI) : 0f;
        float squash = kind == Kind.MITE && preparing ? 1f + .12f * MathUtils.sin(timer * 16f) : 1f;
        float animation = phase == Phase.MOVE || kind == Kind.MITE && !preparing ? walkTime : 0f;
        d.cast.centered(b, kind.id, pos.x, pos.y + hop, animation, facingRight(), preparing,
                flash > 0, c(Color.WHITE), scale * squash, scale / squash, 1f);
    }
}
