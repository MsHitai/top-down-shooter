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
 * A Slums boss (the owner's son wanted a cat boss): the Alley Cat - a big smug ginger tomcat with a torn
 * ear, a purple fur coat, gold chains and sunglasses. A gangster king, not a fairy-tale one.
 * <ul>
 *   <li>First he lounges on a red velvet sofa that two goons carry to and fro along the top of the lot,
 *   and fights from there: claw swipes that send crescents of blades, a hairball coughed up that bursts
 *   into a ring of fur, and gold coins flicked at you with a sneer.</li>
 *   <li>At 60% his goons drop the sofa (it stays where it fell) and join in (two Robbers); he leaps off and
 *   prowls on foot: pounces - a wiggle, a red ring where he'll land, a leap you can't hurt him in, a ring of
 *   claws on landing - double claw swipes, and the hairballs.</li>
 * </ul>
 */
public class AlleyCat extends Enemy {

    private static final Color FUR = Color.valueOf("e8923a");
    private static final Color FUR_LIGHT = Color.valueOf("f8c07a");
    private static final Color FUR_DARK = Color.valueOf("b86a24");
    private static final Color COAT = Color.valueOf("6a3a8a");
    private static final Color COAT_FLUFF = Color.valueOf("9a6ab8");
    private static final Color SHADES = Color.valueOf("16141c");
    private static final Color GOLD = Color.valueOf("f2c230");
    private static final Color NOSE = Color.valueOf("e86a8a");
    private static final Color SOFA = Color.valueOf("b8283a");
    private static final Color SOFA_DARK = Color.valueOf("7a1626");
    private static final Color SUIT = Color.valueOf("2a2a34");
    private static final Color HAT = Color.valueOf("3a3a46");
    private static final Color GOON_SKIN = Color.valueOf("d9a066");
    private static final Color CLAW = Color.valueOf("f5f0e6");
    private static final Color HAIR = Color.valueOf("c8843a");

    /** Below this share of his health, the goons drop the sofa. */
    public static final float OFF_THE_SOFA = 0.6f;
    private static final float SWIPE_WINDUP = 0.4f;
    private static final float COUGH = 0.6f;
    private static final float CROUCH = 0.6f;
    private static final float LEAP = 0.6f;
    private static final float SOFA_W = 1.5f;

    private enum Phase { SOFA, DROP, PROWL }

    private Phase phase = Phase.SOFA;
    private float clock;
    private float swipeTimer = 1.6f;
    private float hairballTimer = 3.5f;
    private float coinTimer = 5.5f;
    private float pounceTimer = 2f;
    private float windup;
    private int swipesLeft;
    private float cough;
    private float crouch;
    private float leap;
    private float dropTimer;
    private float sofaDir = 1f;
    /** Where the sofa is (it stays there once dropped). */
    private final Vector2 sofa = new Vector2();
    private final Vector2 leapFrom = new Vector2();
    private final Vector2 landAt = new Vector2();
    /** His hairballs in flight: each bursts into a ring of fur where it stops. */
    private final Array<Bullet> hairballs = new Array<>();
    private boolean enraged;
    private final int tier;

    public AlleyCat(float x, float y, int tier) {
        super(x, y, 1000f + 300f * tier);
        this.tier = tier;
        radius = 0.4f;
        speed = 0.8f;
        score = 700 + 250 * tier;
        boss = true;
        sofa.set(x, y);
    }

    @Override
    public String bossName() {
        return Boss.ALLEY_CAT.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    /** In the middle of a leap: up in the air, nothing touches him. */
    @Override
    protected boolean isDodging() {
        return leap > 0;
    }

    @Override
    public float feetY() {
        return phase == Phase.SOFA ? sofa.y - 0.55f : pos.y - 0.42f;
    }

    /** No round shadow while the goons carry him (the sofa and goons have their own). */
    @Override
    protected boolean castsShadow() {
        return phase != Phase.SOFA;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        clock += delta;
        float diff = world.getDifficulty();
        updateHairballs(world, diff);
        if (phase == Phase.SOFA && hp <= maxHp * OFF_THE_SOFA) {
            dropTheSofa(world);
        }
        switch (phase) {
            case SOFA:
                carried(delta);
                sofaAttacks(delta, world, diff);
                break;
            case DROP: // the sofa thuds down, he hops off with a sneer
                dropTimer -= delta;
                pos.set(sofa.x, sofa.y - 0.2f - 0.4f * MathUtils.clamp(1f - dropTimer / 0.8f, 0f, 1f));
                if (dropTimer <= 0) phase = Phase.PROWL;
                break;
            default:
                prowl(delta, world, diff);
                break;
        }
    }

    /** The goons carry the sofa to and fro along the top of the lot. */
    private void carried(float delta) {
        Arena a = Arena.now;
        sofa.x += sofaDir * 0.7f * delta;
        if (sofa.x > a.right - 2f) sofaDir = -1f;
        if (sofa.x < a.left + 2f) sofaDir = 1f;
        sofa.y = a.top - 2f;
        pos.set(sofa.x, sofa.y + 0.15f);
        walkTime += delta;
    }

    private void sofaAttacks(float delta, GameWorld world, float diff) {
        if (claws(delta, world, diff)) return;
        if (coughing(delta, world, diff)) return;
        swipeTimer -= delta;
        hairballTimer -= delta;
        coinTimer -= delta;
        if (!canShoot(world)) return;
        if (swipeTimer <= 0) {
            swipeTimer = MathUtils.random(1.6f, 2.2f) / diff;
            windup = SWIPE_WINDUP;
            swipesLeft = 1;
        } else if (hairballTimer <= 0) {
            hairballTimer = MathUtils.random(4f, 5f) / diff;
            cough = COUGH;
        } else if (coinTimer <= 0) {
            coinTimer = MathUtils.random(5.5f, 7f) / diff;
            flickCoins(world, diff);
        }
    }

    /** The goons have had enough: down goes the sofa, in come the goons, and the cat is on his feet. */
    private void dropTheSofa(GameWorld world) {
        phase = Phase.DROP;
        enraged = true;
        dropTimer = 0.8f;
        windup = 0f;
        cough = 0f;
        world.shake(0.6f);
        world.sfx().play("body_fall", 1f);
        world.particles().burst(sofa.x, sofa.y - 0.3f, 20, 0.4f, 1.6f, 0.6f, 0.07f, SOFA);
        int goons = tier >= 1 ? 3 : 2;
        for (int i = 0; i < goons; i++) {
            float side = i == 0 ? -1f : i == 1 ? 1f : 0f;
            world.spawnEnemy(new Robber(sofa.x + side * (SOFA_W / 2f + 0.2f), sofa.y - 0.5f));
        }
        pounceTimer = 1.4f;
        swipeTimer = 2.5f;
    }

    /** On foot: prowls after you, pounces, swipes twice in a row, coughs up hairballs. */
    private void prowl(float delta, GameWorld world, float diff) {
        if (leap > 0) { // in the air: he comes down on the ring
            leap -= delta;
            float k = 1f - Math.max(0f, leap) / LEAP;
            pos.set(leapFrom).lerp(landAt, k);
            if (leap <= 0) land(world, diff);
            return;
        }
        if (crouch > 0) { // the wiggle before the pounce; the ring creeps after you
            crouch -= delta;
            landAt.lerp(tmpTarget(world), Math.min(1f, delta * 3f));
            Arena.now.clamp(landAt, radius);
            if (crouch <= 0) {
                leapFrom.set(pos);
                leap = LEAP;
                world.sfx().play("swish", 0.9f);
            }
            return;
        }
        if (claws(delta, world, diff)) return;
        if (coughing(delta, world, diff)) return;
        keepDistance(delta, world, 1.6f, speed * 1.6f);
        pounceTimer -= delta;
        swipeTimer -= delta;
        hairballTimer -= delta;
        if (pounceTimer <= 0) {
            pounceTimer = MathUtils.random(2.6f, 3.4f) / diff;
            crouch = CROUCH;
            landAt.set(tmpTarget(world));
            Arena.now.clamp(landAt, radius);
        } else if (swipeTimer <= 0 && canShoot(world)) {
            swipeTimer = MathUtils.random(1.8f, 2.4f) / diff;
            windup = SWIPE_WINDUP;
            swipesLeft = 2;
        } else if (hairballTimer <= 0 && canShoot(world)) {
            hairballTimer = MathUtils.random(4.5f, 5.5f) / diff;
            cough = COUGH;
        }
    }

    private final Vector2 target = new Vector2();

    private Vector2 tmpTarget(GameWorld world) {
        return target.set(world.getPlayer().centerX(), world.getPlayer().hitboxY());
    }

    /** Thump: a ring of claw blades all round where he lands. */
    private void land(GameWorld world, float diff) {
        int n = 16;
        float off = MathUtils.random(360f);
        for (int i = 0; i < n; i++) {
            Bullet b = world.spawnEnemyBullet(pos.x, feetY() + 0.1f, off + 360f * i / n, 2f * diff);
            b.shape = Bullet.Shape.DIAMOND;
            b.color.set(CLAW);
        }
        world.shake(0.4f);
        world.sfx().play("body_fall", 0.9f);
        world.particles().burst(pos.x, feetY(), 14, 0.4f, 1.6f, 0.5f, 0.06f, FUR_LIGHT);
        swipeTimer = Math.min(swipeTimer, 0.5f); // and a swipe right after
    }

    /** A claw swipe winding up / going off; true while busy with it. */
    private boolean claws(float delta, GameWorld world, float diff) {
        if (windup <= 0) {
            return false;
        }
        windup -= delta;
        if (windup <= 0) {
            swipe(world, diff);
            if (--swipesLeft > 0) windup = SWIPE_WINDUP * 0.6f; // the other paw, quicker
        }
        return true;
    }

    /** A crescent of claw blades at the player (his paw sweeps across). */
    private void swipe(GameWorld world, float diff) {
        int n = 9;
        float side = swipesLeft % 2 == 0 ? 1f : -1f;
        for (int i = 0; i < n; i++) {
            float k = i / (n - 1f);
            float a = aimAngle + side * (-45f + 90f * k);
            // the middle of the crescent bulges ahead: those blades fly a touch faster
            float speed = (2.4f + 0.5f * MathUtils.sin(k * MathUtils.PI)) * diff;
            Bullet b = shoot(world, a, speed);
            b.shape = Bullet.Shape.DIAMOND;
            b.color.set(CLAW);
        }
        world.sfx().play("swish", 0.8f);
        world.particles().burst(pos.x + MathUtils.cosDeg(aimAngle) * 0.4f, pos.y + MathUtils.sinDeg(aimAngle) * 0.4f,
                8, 0.3f, 1.4f, 0.3f, 0.035f, CLAW);
    }

    /** A hairball being coughed up / going off; true while busy with it. */
    private boolean coughing(float delta, GameWorld world, float diff) {
        if (cough <= 0) {
            return false;
        }
        cough -= delta;
        if (cough <= 0) {
            Bullet b = shoot(world, aimAngle, 1.5f * diff);
            b.shape = Bullet.Shape.GLOB;
            b.color.set(HAIR);
            b.radius = 0.2f;
            b.life = MathUtils.clamp(distanceToPlayer(world) / 1.5f, 0.6f, 2f);
            hairballs.add(b);
            world.sfx().play("pop", 0.8f);
        }
        return true;
    }

    /** A hairball that has stopped bursts into a ring of fur (not when a blank made it vanish). */
    private void updateHairballs(GameWorld world, float diff) {
        for (int i = hairballs.size - 1; i >= 0; i--) {
            Bullet h = hairballs.get(i);
            if (h.alive) continue;
            hairballs.removeIndex(i);
            if (world.getBlankRing() >= 0) continue;
            int n = enraged ? 14 : 11;
            float off = MathUtils.random(360f);
            for (int k = 0; k < n; k++) {
                Bullet f = world.spawnEnemyBullet(h.prev.x, h.prev.y, off + 360f * k / n, 1.8f * diff);
                f.color.set(HAIR);
            }
            world.particles().burst(h.prev.x, h.prev.y, 12, 0.3f, 1.4f, 0.5f, 0.05f, HAIR);
        }
    }

    /** He flicks a handful of gold coins at you, can't be bothered to aim properly. */
    private void flickCoins(GameWorld world, float diff) {
        for (int i = 0; i < 7; i++) {
            Bullet b = shoot(world, aimAngle + MathUtils.random(-30f, 30f), MathUtils.random(1.8f, 2.8f) * diff);
            b.color.set(GOLD);
        }
        world.sfx().play("coin", 0.8f);
    }

    /** The sofa (carried, or where it fell), and the red ring where a pounce will land. */
    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (crouch > 0 && alive) {
            float k = 1f - crouch / CROUCH;
            d.ring(b, landAt.x, landAt.y - 0.3f, 0.6f, TELEGRAPH, 0.35f + 0.5f * k);
            d.circle(b, landAt.x, landAt.y - 0.3f, 0.6f * k, TELEGRAPH, 0.15f);
        }
        if (phase != Phase.SOFA) {
            drawSofa(b, d, sofa.x, sofa.y - 0.6f, false);
        }
    }

    private void drawSofa(SpriteBatch b, Draw d, float cx, float bottom, boolean carried) {
        float w = SOFA_W;
        d.ellipse(b, cx, bottom - 0.02f, w * 0.55f, 0.12f, Color.BLACK, carried ? 0.2f : 0.35f);
        d.rect(b, cx - w / 2f, bottom + 0.1f, w, 0.28f, c(SOFA_DARK));        // the seat
        d.rect(b, cx - w / 2f + 0.06f, bottom + 0.38f, w - 0.12f, 0.36f, c(SOFA)); // the back
        for (int i = 0; i < 3; i++) { // buttons in the velvet
            d.circle(b, cx - w / 3f + i * w / 3f, bottom + 0.58f, 0.025f, c(SOFA_DARK));
        }
        d.rect(b, cx - w / 2f - 0.1f, bottom + 0.1f, 0.18f, 0.42f, c(SOFA));   // the arms
        d.rect(b, cx + w / 2f - 0.08f, bottom + 0.1f, 0.18f, 0.42f, c(SOFA));
        d.rect(b, cx - w / 2f + 0.04f, bottom, 0.06f, 0.1f, GOLD);            // gold feet
        d.rect(b, cx + w / 2f - 0.1f, bottom, 0.06f, 0.1f, GOLD);
    }

    /** A goon in a dark suit and fedora, holding up a corner of the sofa. */
    private void drawGoon(SpriteBatch b, Draw d, float x, float bottom, float step) {
        d.ellipse(b, x, bottom, 0.16f, 0.05f, Color.BLACK, 0.3f);
        d.rect(b, x - 0.08f + step, bottom, 0.06f, 0.12f, FEET);
        d.rect(b, x + 0.02f - step, bottom, 0.06f, 0.12f, FEET);
        d.rect(b, x - 0.12f, bottom + 0.1f, 0.24f, 0.3f, SUIT);
        d.circle(b, x, bottom + 0.48f, 0.09f, GOON_SKIN);
        d.rect(b, x - 0.13f, bottom + 0.53f, 0.26f, 0.03f, HAT);
        d.rect(b, x - 0.08f, bottom + 0.55f, 0.16f, 0.08f, HAT);
        d.rect(b, x - 0.06f, bottom + 0.46f, 0.12f, 0.03f, SHADES);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        boolean onSofa = phase == Phase.SOFA;
        float hop = leap > 0 ? MathUtils.sin((1f - leap / LEAP) * MathUtils.PI) * 0.9f : 0f;
        float wiggle = crouch > 0 ? MathUtils.sin(clock * 40f) * 0.04f : 0f;
        float crouchDown = crouch > 0 ? 0.08f : 0f;
        if (onSofa) { // carried in by his goons, lounging
            float step = MathUtils.sin(walkTime * 9f) * 0.03f;
            float bottom = sofa.y - 0.55f;
            drawSofa(b, d, sofa.x, bottom + 0.12f, true);
            drawGoon(b, d, sofa.x - SOFA_W / 2f + 0.05f, bottom - 0.25f, step);
            drawGoon(b, d, sofa.x + SOFA_W / 2f - 0.05f, bottom - 0.25f, -step);
        }
        float cx = pos.x + wiggle;
        float base = (onSofa ? sofa.y - 0.15f : feetY()) + hop - crouchDown;
        float face = facingRight() ? 1 : -1;
        // the tail, swishing smugly
        float swish = MathUtils.sin(clock * 3f) * 25f;
        d.line(b, cx - face * 0.25f, base + 0.15f, 0.42f, 0.07f, (face > 0 ? 160f : 20f) + swish, c(FUR_DARK));
        if (!onSofa) { // legs when he's on foot
            d.rect(b, cx - 0.2f, base, 0.12f, 0.14f, c(FUR_DARK));
            d.rect(b, cx + 0.08f, base, 0.12f, 0.14f, c(FUR_DARK));
        }
        // the fur coat with its big fluffy collar, a gold chain over it
        d.ellipse(b, cx, base + 0.32f, 0.3f, 0.24f, c(COAT), 1f);
        d.ellipse(b, cx, base + 0.5f, 0.32f, 0.1f, c(COAT_FLUFF), 1f);
        d.rect(b, cx - 0.03f, base + 0.16f, 0.06f, 0.3f, c(FUR_LIGHT)); // the belly peeking out
        for (int i = 0; i < 5; i++) {
            float t = i / 4f;
            d.circle(b, cx - 0.14f + 0.28f * t, base + 0.42f - MathUtils.sin(t * MathUtils.PI) * 0.1f, 0.025f, GOLD);
        }
        d.circle(b, cx, base + 0.31f, 0.045f, GOLD); // the medallion
        // the paws: one raised, claws out, while a swipe winds up
        float pawY = base + 0.3f;
        if (windup > 0) {
            float k = 1f - windup / SWIPE_WINDUP;
            float px = cx + face * 0.3f;
            float py = pawY + 0.2f + 0.15f * k;
            d.circle(b, px, py, 0.08f, c(FUR));
            for (int i = -1; i <= 1; i++) {
                d.rotRect(b, px + face * 0.06f, py + 0.06f + i * 0.04f, 0.08f, 0.02f, face * 20f, CLAW, 0.5f + 0.5f * k);
            }
        } else {
            d.circle(b, cx + face * 0.24f, pawY, 0.07f, c(FUR));
        }
        d.circle(b, cx - face * 0.24f, pawY, 0.07f, c(FUR));
        // the big round head: ears (the left one torn), sunglasses, a smug grin, whiskers
        float hy = base + 0.78f + (cough > 0 ? MathUtils.sin(clock * 30f) * 0.02f : 0f);
        d.rotRect(b, cx - 0.17f, hy + 0.2f, 0.13f, 0.16f, 20f, c(FUR_DARK), 1f);
        d.rotRect(b, cx + 0.17f, hy + 0.2f, 0.13f, 0.16f, -20f, c(FUR_DARK), 1f);
        d.rect(b, cx - 0.2f, hy + 0.25f, 0.05f, 0.05f, c(COAT)); // the torn notch
        d.ellipse(b, cx, hy, 0.25f, 0.21f, c(FUR), 1f);
        d.ellipse(b, cx, hy - 0.07f, 0.13f, 0.08f, c(FUR_LIGHT), 1f); // the muzzle
        for (int s = -1; s <= 1; s += 2) { // stripes on the brow
            d.rect(b, cx + s * 0.07f - 0.015f, hy + 0.12f, 0.03f, 0.07f, c(FUR_DARK));
        }
        d.rect(b, cx - 0.2f, hy + 0.02f, 0.4f, 0.04f, SHADES); // the shades
        d.rect(b, cx - 0.19f, hy - 0.02f, 0.16f, 0.08f, SHADES);
        d.rect(b, cx + 0.03f, hy - 0.02f, 0.16f, 0.08f, SHADES);
        d.rect(b, cx - 0.16f, hy + 0.03f, 0.05f, 0.02f, Color.WHITE); // a glint
        d.circle(b, cx, hy - 0.06f, 0.025f, NOSE);
        if (cough > 0) { // mouth open, hacking up a hairball
            d.circle(b, cx, hy - 0.12f, 0.04f, SHADES);
        } else { // the smug grin, one side up
            d.rotRect(b, cx + face * 0.03f, hy - 0.12f, 0.1f, 0.02f, face * 15f, SHADES, 1f);
        }
        for (int s = -1; s <= 1; s += 2) {
            d.line(b, cx + s * 0.1f, hy - 0.07f, 0.14f, 0.01f, s > 0 ? 5f : 175f, Color.WHITE);
        }
    }
}
