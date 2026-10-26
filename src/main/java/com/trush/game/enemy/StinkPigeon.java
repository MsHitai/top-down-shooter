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
 * A marked critter (see Marked): a street pigeon the Skunk King's stink got to - a green ear where no
 * pigeon has one, glowing eyes. It flutters about at a distance over furniture and pits, puffs up its
 * green cheeks and spits a stink pellet that bursts into a cloud of stink where it lands (GasCloud.STINK:
 * the acid drop warning, then it hurts). Rare in the Slums.
 */
public class StinkPigeon extends Enemy {

    private static final Color FEATHER = Color.valueOf("8a90a0");
    private static final Color FEATHER_DARK = Color.valueOf("5e667a");
    private static final Color NECK = Color.valueOf("4fa07a");
    private static final Color BEAK = Color.valueOf("e0a040");

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
            if (world.getBlankRing() < 0) { // (a blank just makes it vanish)
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
        float bob = MathUtils.sin(clock * 6f) * 0.03f;
        float x = pos.x;
        float y = pos.y + bob;
        float flap = MathUtils.sin(clock * 22f);
        float swell = puff > 0 ? 1f - puff / PUFF : 0f;
        // wings beating
        d.rotRect(b, x - face * 0.04f, y + 0.04f, 0.2f, 0.07f, face * (25f + flap * 35f), c(FEATHER_DARK), 1f);
        // body, tail and breast
        d.rotRect(b, x - face * 0.15f, y - 0.02f, 0.1f, 0.06f, face > 0 ? 200f : -20f, c(FEATHER_DARK), 1f);
        d.circle(b, x, y, 0.12f, c(FEATHER));
        d.circle(b, x + face * 0.05f, y - 0.02f, 0.08f + 0.04f * swell, c(NECK)); // the green throat puffs up
        // head, the odd ear, the beak, a glowing eye
        float hx = x + face * 0.1f;
        float hy = y + 0.1f;
        d.circle(b, hx, hy, 0.065f, c(FEATHER));
        Marked.ear(b, d, hx - face * 0.02f, hy + 0.04f, 0.08f, -face * 25f);
        d.rotRect(b, hx + face * 0.07f, hy - 0.01f, 0.05f, 0.025f, face > 0 ? -10f : 190f, BEAK, 1f);
        Marked.eye(b, d, hx + face * 0.025f, hy + 0.01f, 0.014f, clock);
        if (swell > 0) { // stink leaking from the beak
            d.circle(b, hx + face * 0.12f, hy - 0.03f, 0.02f + 0.03f * swell, Marked.MARK, 0.6f);
        }
    }
}
