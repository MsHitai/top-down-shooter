package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.objects.GasCloud;
import com.trush.game.player.Player;
import com.trush.game.world.GameWorld;

/**
 * A marked critter (see Marked): a street pigeon the Skunk King's stink got to. Its eye glows as it
 * flutters about at a distance over furniture and pits. It puffs up its
 * green cheeks and spits a stink pellet that bursts into a cloud of stink where it lands (GasCloud.STINK:
 * the acid drop warning, then it hurts). Rare in the Slums.
 */
public class StinkPigeon extends Enemy {

    private static final float ART_SCALE = 0.82f;
    private static final float PUFF = 0.5f;
    private static final float PELLET_SPEED = 2.2f;
    public static final float STINK_RADIUS = 0.34f;
    private static final float STINK_LIFE = 3.2f;

    private float clock = MathUtils.random(10f);
    private float fireTimer = MathUtils.random(1.8f, 2.8f);
    private float puff;
    private final float orbit = MathUtils.random(360f);
    private final Vector2 target = new Vector2();
    /** Its stink pellets in flight: each bursts into a cloud where it stops. */
    private final Array<Bullet> pellets = new Array<>();

    public StinkPigeon(float x, float y) {
        super(x, y, 14f);
        radius = 0.15f;
        speed = 1.3f;
        score = 20;
        flying = true;
    }

    /** It flutters well above the floor. */
    @Override
    public float feetY() {
        return pos.y - 0.4f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        clock += delta;
        float diff = world.getDifficulty();
        for (int i = pellets.size - 1; i >= 0; i--) {
            Bullet p = pellets.get(i);
            if (p.alive) continue;
            pellets.removeIndex(i);
            if (!p.blanked) { // (a blank just makes it vanish)
                world.releaseGas(GasCloud.Kind.STINK, p.prev.x, p.prev.y, 0f, 0f, STINK_RADIUS, STINK_LIFE);
                world.sfx().play("pop", 0.5f);
            }
        }
        if (puff > 0) {
            puff -= delta;
            if (puff <= 0) {
                spit(world, diff);
            }
            return; // hovers still to puff up
        }
        // circles the player at a distance, drifting round slowly
        Player pl = world.getPlayer();
        float a = orbit + clock * 25f;
        target.set(pl.centerX() + MathUtils.cosDeg(a) * 2.4f, pl.hitboxY() + 0.4f + MathUtils.sinDeg(a) * 1.6f).sub(pos);
        float dist = target.len();
        if (dist > 0.05f) {
            pos.mulAdd(target.scl(1f / dist), Math.min(dist, speed * delta));
        }
        walkTime += delta;
        fireTimer -= delta;
        if (fireTimer <= 0 && world.onScreen(pos.x, pos.y, 0.3f)) {
            puff = PUFF;
        }
    }

    private void spit(GameWorld world, float diff) {
        float speed = PELLET_SPEED * Math.min(diff, 1.3f);
        Bullet b = shoot(world, aimAngle, speed);
        b.shape = Bullet.Shape.GLOB;
        b.color.set(Marked.MARK);
        b.radius = 0.07f;
        b.life = MathUtils.clamp(distanceToPlayer(world) / speed, 0.4f, 2.2f);
        pellets.add(b);
        fireTimer = MathUtils.random(2.6f, 3.6f) / diff;
        world.sfx().play("flap", 0.5f);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float face = facingRight() ? 1 : -1;
        float x = pos.x;
        float y = pos.y + MathUtils.sin(clock * 6f) * 0.03f;
        float pixel = ART_SCALE / 64f;
        float wingTime = walkTime == 0f ? 0f : clock * 2f;
        d.cast.centered(b, "StinkPigeon", x, y, wingTime, facingRight(), puff > 0f, flash > 0f,
                c(Color.WHITE), ART_SCALE, ART_SCALE, 1f);
        if (flash <= 0f) {
            Marked.eye(b, d, x + face * 9.5f * pixel, y + 5.5f * pixel, 1.1f * pixel, clock);
        }
        if (puff > 0f) {
            float swell = 1f - puff / PUFF;
            d.circle(b, x + face * 21f * pixel, y + 2f * pixel,
                    0.02f + 0.03f * swell, Marked.MARK, 0.6f);
        }
    }
}
