package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * One of the Skunk King's court in the Undercity (the owner's pick: the raccoon wasn't lost when the King
 * became a skunk): a sneaky raccoon in a bandit's mask and a little paper crown. It doesn't fight - it darts
 * in and snatches a handful of the player's coins, then runs for it with the sack. Shoot it down and it
 * drops what it took, and a bit more; let it be and after a while it dives down a drain with your coins.
 * If the player has no coins it just bites.
 */
public class Raccoon extends Enemy {

    private static final Color FUR = Color.valueOf("8a8a94");
    private static final Color FUR_DARK = Color.valueOf("4e4e58");
    private static final Color FUR_LIGHT = Color.valueOf("c8c8d0");
    private static final Color MASK = Color.valueOf("1e1c22");
    private static final Color CROWN = Color.valueOf("e8c040");
    private static final Color SACK = Color.valueOf("b8945a");

    /** It snatches at most this many coins in one go. */
    public static final int GRAB = 12;
    /** It runs with the sack this long, then it's off down a drain. */
    public static final float GETAWAY = 8f;
    private static final float SNATCH_REACH = 0.35f;

    private float clock = MathUtils.random(10f);
    /** The coins in its sack. */
    private int stolen;
    private float fleeing;
    private boolean escaped;
    private final Vector2 away = new Vector2();

    public Raccoon(float x, float y) {
        super(x, y, 26f);
        radius = 0.15f;
        speed = 2.1f;
        score = 35;
        hurtsToTouch = false; // (it bites only a hero with no coins to snatch - see behave)
    }

    public int stolen() {
        return stolen;
    }

    public boolean escaped() {
        return escaped;
    }

    @Override
    public float feetY() {
        return pos.y - 0.13f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        clock += delta;
        Player p = world.getPlayer();
        if (stolen > 0) { // off with the sack: away from the player, until it gets away down a drain
            fleeing -= delta;
            hurtsToTouch = false;
            away.set(pos).sub(p.centerX(), p.hitboxY());
            if (away.len2() < 0.0001f) away.set(1, 0);
            away.nor().rotateDeg(MathUtils.sin(clock * 3f) * 40f);
            pos.mulAdd(away, speed * 1.15f * delta);
            Arena.now.clamp(pos, radius + 0.1f);
            walkTime += delta;
            if (fleeing <= 0) getAway(world);
            return;
        }
        // nothing in the sack yet: dart in for the coins (or, if there are none, for a bite)
        hurtsToTouch = p.getCoins() == 0;
        if (!canSeePlayer && flank(delta, world, speed)) {
            return;
        }
        float a = aimAngle + MathUtils.sin(clock * 7f) * 25f;
        pos.add(MathUtils.cosDeg(a) * speed * delta, MathUtils.sinDeg(a) * speed * delta);
        walkTime += delta;
        if (p.getCoins() > 0 && !p.isDead() && distanceToPlayer(world) < SNATCH_REACH + radius) {
            snatch(world, p);
        }
    }

    private void snatch(GameWorld world, Player p) {
        stolen = Math.min(GRAB, p.getCoins());
        p.setCoins(p.getCoins() - stolen);
        fleeing = GETAWAY;
        world.sfx().play("coin", 1f);
        world.sfx().play("cackle", 0.5f);
        world.toast("THE RACCOON SNATCHED " + stolen + " COINS!");
        world.particles().burst(pos.x, pos.y + 0.1f, 8, 0.3f, 1.2f, 0.4f, 0.04f, CROWN);
    }

    /** Down a drain with the coins: gone, no score, no loot. */
    private void getAway(GameWorld world) {
        escaped = true;
        vanishOnDeath = true;
        alive = false;
        world.particles().burst(pos.x, pos.y, 12, 0.3f, 1.2f, 0.5f, 0.05f, FUR_DARK);
        world.sfx().play("dodge", 0.8f);
        world.toast("THE RACCOON GOT AWAY WITH " + stolen + " COINS!");
    }

    /** Shot down: the sack bursts - what it took, and a few more for the trouble. */
    @Override
    public void dropLoot(GameWorld world) {
        if (stolen > 0) {
            world.dropCoins(pos.x, pos.y, stolen + 4);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float face = facingRight() ? 1 : -1;
        float scurry = MathUtils.sin(walkTime * 22f) * 0.02f;
        float y = pos.y + scurry;
        // the ringed tail
        for (int i = 0; i < 4; i++) {
            d.circle(b, pos.x - face * (0.13f + i * 0.045f), y + 0.02f + i * 0.01f, 0.04f, c(i % 2 == 0 ? FUR_DARK : FUR_LIGHT));
        }
        // body, the sack over its shoulder once it has your coins
        d.ellipse(b, pos.x, y - 0.02f, 0.13f, 0.1f, c(FUR), 1f);
        if (stolen > 0) {
            d.circle(b, pos.x - face * 0.08f, y + 0.08f, 0.08f, SACK);
            d.rect(b, pos.x - face * 0.08f - 0.02f, y + 0.15f, 0.04f, 0.03f, FUR_DARK);
            d.circle(b, pos.x - face * 0.08f, y + 0.08f, 0.025f, CROWN); // a coin peeking out
        }
        // the head: pointy ears, the bandit's mask, a little paper crown
        float hx = pos.x + face * 0.1f;
        float hy = y + 0.08f;
        d.circle(b, hx - 0.05f, hy + 0.06f, 0.03f, c(FUR_DARK));
        d.circle(b, hx + 0.05f, hy + 0.06f, 0.03f, c(FUR_DARK));
        d.circle(b, hx, hy, 0.075f, c(FUR_LIGHT));
        d.rect(b, hx - 0.075f, hy - 0.005f, 0.15f, 0.04f, MASK);
        d.circle(b, hx + face * 0.03f, hy + 0.015f, 0.012f, Color.WHITE);
        d.circle(b, hx + face * 0.07f, hy - 0.03f, 0.016f, MASK); // the nose
        d.rect(b, hx - 0.05f, hy + 0.07f, 0.1f, 0.03f, CROWN);
        for (int i = -1; i <= 1; i++) {
            d.rect(b, hx + i * 0.035f - 0.012f, hy + 0.1f, 0.024f, 0.03f, CROWN);
        }
    }
}
