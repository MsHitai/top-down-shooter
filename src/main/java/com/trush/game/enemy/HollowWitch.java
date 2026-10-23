package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * The witch from the campfire stories, riding a broomstick. She flies over furniture, so cover only
 * stops her bullets. Hovers and flings curving hex bolts; swoops across the room leaving sparks that
 * fire at you a moment later; closes a ring of bullets around you (one gap); lobs jack-o'-lanterns
 * that roll to a stop and burst. Meant to be about as hard as the Fluffy King.
 */
public class HollowWitch extends Enemy {

    private enum Phase { HOVER, SWOOP, HEX, PUMPKINS }

    private static final Color ROBE = Color.valueOf("3b2350");
    private static final Color ROBE_DARK = Color.valueOf("26163a");
    private static final Color SKIN = Color.valueOf("9cc47a");
    private static final Color SKIN_DARK = Color.valueOf("6f9a55");
    private static final Color HAIR = Color.valueOf("b8b8c8");
    private static final Color HAT = Color.valueOf("1e1428");
    private static final Color HAT_BAND = Color.valueOf("8e44ad");
    private static final Color BROOM = Color.valueOf("8a5a2b");
    private static final Color BRISTLES = Color.valueOf("d9b54a");
    private static final Color HEX = Color.valueOf("c77dff");
    private static final Color SPARK = Color.valueOf("7dffb3");
    private static final Color PUMPKIN = Color.valueOf("f0802a");

    /** Body floats this high above the shadow on the floor. */
    private static final float HOVER_HEIGHT = 0.35f;
    private static final float HEX_WINDUP = 0.75f;

    private Phase phase = Phase.HOVER;
    private float phaseTimer = 1.6f;
    private float shotTimer = 1f;
    private int patternIndex;
    private boolean enraged;
    private float time;
    private final Vector2 target = new Vector2();
    private final Vector2 step = new Vector2();
    private final Vector2 swoopEnd = new Vector2();
    private final Vector2 hexCenter = new Vector2();
    /** Pumpkins in flight: they burst into a ring once they roll to a stop. */
    private final Array<Bullet> pumpkins = new Array<>();

    public HollowWitch(float x, float y, int tier) {
        super(x, y, 420f + 160f * tier);
        radius = 0.34f;
        speed = 1.0f;
        score = 500 + 250 * tier;
        boss = true;
        flying = true;
        target.set(x, y);
    }

    @Override
    public String bossName() {
        return Boss.WITCH.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    /** The shadow on the floor; she floats above it. */
    @Override
    public float feetY() {
        return pos.y - 0.12f - HOVER_HEIGHT; // broom just below the hitbox centre, so the robe and head get hit
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        if (!enraged && hp < maxHp * 0.5f) {
            enraged = true;
            world.shake(0.6f);
            world.sfx().play("cackle", 1f);
        }
        float rage = enraged ? 1.3f : 1f;
        float diff = world.getDifficulty();
        phaseTimer -= delta;
        shotTimer -= delta;
        updatePumpkins(world, diff);

        switch (phase) {
            case HOVER:
                hover(delta, world, rage, diff);
                if (phaseTimer <= 0) {
                    nextPattern(world);
                }
                break;
            case SWOOP:
                swoop(delta, world, rage, diff);
                break;
            case HEX:
                drift(delta, rage * 0.4f);
                if (phaseTimer <= 0) {
                    closeHex(world, diff);
                    toHover();
                }
                break;
            case PUMPKINS:
                drift(delta, rage * 0.4f);
                if (phaseTimer <= 0) {
                    lobPumpkins(world);
                    toHover();
                }
                break;
        }
    }

    /** Drifts between random spots anywhere in the room, flinging curving hex bolts. */
    private void hover(float delta, GameWorld world, float rage, float diff) {
        boolean hunting = huntForSight(delta, world, target);
        if (!hunting && pos.dst2(target) < 0.05f) {
            target.set(MathUtils.random(Arena.now.left + 0.8f, Arena.now.right - 0.8f),
                    MathUtils.random(Arena.now.centerY - 0.4f, Arena.now.top - 0.6f));
        }
        drift(delta, (hunting ? 2f : 1.2f) * rage);
        if (shotTimer <= 0 && canSeePlayer) {
            shotTimer = (enraged ? 0.7f : 1.0f) / diff;
            for (int i = -1; i <= 1; i++) {
                Bullet b = shoot(world, aimAngle + i * 18f, 2.6f * diff);
                b.curve = -i * 25f; // the outer bolts bend back in
                b.color.set(HEX);
            }
            world.sfx().play("enemy_shoot", 0.7f);
        }
    }

    private void drift(float delta, float k) {
        step.set(target).sub(pos);
        if (step.len() > 0.02f) {
            step.nor().scl(speed * k * delta);
            pos.add(step);
        }
    }

    /** Dives over the player to the far side, dropping sparks that fire at the player a moment later. */
    private void swoop(float delta, GameWorld world, float rage, float diff) {
        step.set(swoopEnd).sub(pos);
        float move = 3.4f * rage * delta;
        if (step.len() <= move || phaseTimer <= 0) {
            toHover();
            return;
        }
        pos.mulAdd(step.nor(), move);
        if (shotTimer <= 0) {
            shotTimer = enraged ? 0.05f : 0.07f;
            // starts almost still, then speeds up toward where the player is right now
            Bullet b = world.spawnEnemyBullet(pos.x, pos.y - 0.1f, aimAngle + MathUtils.random(-6f, 6f), 0.05f);
            b.accel = 2.4f * diff;
            b.minSpeed = 0.05f;
            b.maxSpeed = 3f * diff;
            b.radius = 0.055f;
            b.color.set(SPARK);
        }
    }

    /** A ring of bullets around the player (with one gap) that closes in. */
    private void closeHex(GameWorld world, float diff) {
        int n = enraged ? 22 : 18;
        float r = enraged ? 1.15f : 1.35f;
        int gap = MathUtils.random(n - 1);
        for (int i = 0; i < n; i++) {
            if (i == gap || i == (gap + 1) % n) continue;
            float a = 360f * i / n;
            float x = hexCenter.x + MathUtils.cosDeg(a) * r;
            float y = hexCenter.y + MathUtils.sinDeg(a) * r;
            if (!Arena.now.contains(x, y, 0.05f)) continue;
            Bullet b = world.spawnEnemyBullet(x, y, a + 180f, 0.05f);
            b.accel = 1.5f * diff;
            b.minSpeed = 0.05f;
            b.maxSpeed = 2.2f * diff;
            b.color.set(HEX);
        }
        world.sfx().play("blank", 0.4f);
    }

    /** Jack-o'-lanterns lobbed so they roll to a stop around the player. */
    private void lobPumpkins(GameWorld world) {
        int n = enraged ? 3 : 2;
        float px = world.getPlayer().centerX();
        float py = world.getPlayer().hitboxY();
        for (int i = 0; i < n; i++) {
            float tx = px + MathUtils.random(-1f, 1f);
            float ty = py + MathUtils.random(-0.7f, 0.7f);
            float dist = MathUtils.clamp(Vector2.dst(pos.x, pos.y, tx, ty), 0.8f, 4f);
            float decel = 2.5f;
            float angle = MathUtils.atan2Deg360(ty - pos.y, tx - pos.x);
            // v0 so that it comes to rest after "dist": v0^2 = 2 * a * d
            Bullet b = shoot(world, angle, (float) Math.sqrt(2f * decel * dist));
            b.accel = -decel;
            b.minSpeed = 0.02f;
            b.radius = 0.12f;
            b.life = 4f;
            b.color.set(PUMPKIN);
            pumpkins.add(b);
        }
        world.sfx().play("enemy_shoot", 1f);
    }

    private void updatePumpkins(GameWorld world, float diff) {
        for (int i = pumpkins.size - 1; i >= 0; i--) {
            Bullet p = pumpkins.get(i);
            if (!p.alive) { // hit a wall, the player or a blank: no burst
                pumpkins.removeIndex(i);
            } else if (p.vel.len() <= 0.03f || p.life < 0.1f) {
                p.alive = false;
                pumpkins.removeIndex(i);
                int n = enraged ? 12 : 10;
                float off = MathUtils.random(360f);
                for (int k = 0; k < n; k++) {
                    Bullet b = world.spawnEnemyBullet(p.pos.x, p.pos.y, off + 360f * k / n, 1.9f * diff);
                    b.color.set(PUMPKIN);
                }
                world.particles().burst(p.pos.x, p.pos.y, 10, 0.4f, 1.5f, 0.4f, 0.05f, PUMPKIN);
                world.sfx().play("break", 0.6f);
            }
        }
    }

    private void toHover() {
        phase = Phase.HOVER;
        phaseTimer = enraged ? 1.1f : 1.7f;
    }

    private void nextPattern(GameWorld world) {
        Phase[] order = {Phase.SWOOP, Phase.HEX, Phase.PUMPKINS};
        phase = order[patternIndex++ % order.length];
        float px = world.getPlayer().centerX();
        float py = world.getPlayer().hitboxY();
        switch (phase) {
            case SWOOP:
                // over the player and out the other side
                step.set(px, py).sub(pos).nor().scl(2f);
                swoopEnd.set(px + step.x, py + step.y);
                Arena.now.clamp(swoopEnd, radius + 0.1f);
                phaseTimer = 2.5f;
                shotTimer = 0;
                world.sfx().play("dodge", 1f);
                break;
            case HEX:
                hexCenter.set(px, py);
                phaseTimer = HEX_WINDUP;
                break;
            case PUMPKINS:
                phaseTimer = 0.5f;
                break;
            default:
                break;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        if (phase == Phase.HEX) { // where the hex circle will close
            float k = 1f - Math.max(0, phaseTimer) / HEX_WINDUP;
            float r = enraged ? 1.15f : 1.35f;
            d.ring(b, hexCenter.x, hexCenter.y, r, HEX, 0.25f + 0.5f * k);
        }
        float face = facingRight() ? 1 : -1;
        float bob = MathUtils.sin(time * 3f) * 0.04f;
        float cx = pos.x;
        float by = feetY() + HOVER_HEIGHT + bob; // broom height
        float tilt = phase == Phase.SWOOP ? -face * 12f : face * 4f;

        // broomstick with bristles at the back
        d.line(b, cx - face * 0.42f, by - face * 0.02f, 0.86f, 0.05f, face > 0 ? tilt : 180f + tilt, c(BROOM));
        float tailX = cx - face * 0.42f;
        for (int i = -1; i <= 1; i++) {
            d.rotRect(b, tailX - face * 0.05f, by + i * 0.035f, 0.16f, 0.04f, i * 12f * face, c(BRISTLES), 1f);
        }
        // robe sitting on the broom
        d.rect(b, cx - 0.17f, by - 0.02f, 0.34f, 0.24f, c(ROBE_DARK));
        d.rect(b, cx - 0.14f, by + 0.18f, 0.28f, 0.2f, c(ROBE));
        // head: green face, long nose, grey hair
        float hy = by + 0.47f;
        d.circle(b, cx, hy, 0.13f, c(SKIN_DARK));
        d.circle(b, cx + face * 0.01f, hy + 0.01f, 0.12f, c(SKIN));
        d.rotRect(b, cx + face * 0.13f, hy - 0.02f, 0.1f, 0.035f, face * -20f, c(SKIN_DARK), 1f);
        d.rect(b, cx - face * 0.13f - 0.04f, hy - 0.12f, 0.08f, 0.2f, c(HAIR));
        Color eye = enraged ? Color.RED : Color.YELLOW;
        d.circle(b, cx + face * 0.05f, hy + 0.03f, 0.022f, eye);
        // tall crooked hat
        d.rect(b, cx - 0.22f, hy + 0.07f, 0.44f, 0.05f, c(HAT));
        d.rect(b, cx - 0.12f, hy + 0.12f, 0.24f, 0.1f, c(HAT));
        d.rect(b, cx - 0.12f, hy + 0.12f, 0.24f, 0.03f, c(HAT_BAND));
        d.rect(b, cx - 0.08f - face * 0.02f, hy + 0.22f, 0.16f, 0.09f, c(HAT));
        d.rotRect(b, cx - face * 0.07f, hy + 0.33f, 0.09f, 0.07f, face * 30f, c(HAT), 1f);
        // trail of sparkles behind her while she swoops
        if (phase == Phase.SWOOP) {
            d.circle(b, tailX - face * 0.15f, by, 0.06f, SPARK, 0.5f);
        }
    }
}
