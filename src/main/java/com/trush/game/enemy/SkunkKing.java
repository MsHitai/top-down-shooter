package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.objects.GasCloud;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * The False King of the Undercity (the owner's pick over the raccoon): a fat scruffy skunk whose white
 * stripe looks like a cheap royal sash, in a cardboard crown painted gold (the paint has run), an old
 * shower curtain for a cape and a plunger for a sceptre. The real King is fluffy and lives above everything;
 * this one stinks and lives below. His stink is his own invention - the gas that marks the Undercity's
 * critters. The end of the secret detour: beating him ends the run.
 * <ul>
 *   <li>On his throne - a shopping cart piled with trash bags, which he paddles to and fro with the plunger:
 *   tail spray (the tail goes up and shakes while a green cone shows where, then clouds of stink billow out
 *   with a fan of stink pellets), perfume bottles (lobbed, they burst into a ring and a little cloud), and
 *   royal decrees (paper planes that unfold into lines of bullets).</li>
 *   <li>At half health his crown falls off and he tumbles out of the cart in a panic: he runs about, spins
 *   round spraying stink in a spiral that slowly fills the room (gas fades, and the room holds only so
 *   much), digs junk out of the trash and throws it, and keeps lobbing bottles - no more decrees, he's lost
 *   his authority.</li>
 * </ul>
 */
public class SkunkKing extends Enemy {

    private static final Color FUR = Color.valueOf("26242c");
    private static final Color FUR_LIGHT = Color.valueOf("3e3c46");
    private static final Color STRIPE = Color.valueOf("f2f0ea");
    private static final Color NOSE = Color.valueOf("e86a8a");
    private static final Color CROWN = Color.valueOf("d9a84a");
    private static final Color CROWN_RUN = Color.valueOf("8a6a3a");
    private static final Color CURTAIN = Color.valueOf("9ad0e8");
    private static final Color DUCK = Color.valueOf("f2d23a");
    private static final Color PLUNGER = Color.valueOf("c8283a");
    private static final Color STICK = Color.valueOf("a0642f");
    private static final Color CART = Color.valueOf("9a9aa6");
    private static final Color BAG = Color.valueOf("1e1e24");
    private static final Color STINK = Color.valueOf("c8e85a");
    private static final Color PERFUME = Color.valueOf("ff8ac8");
    private static final Color PAPER = Color.valueOf("f5f0e6");
    private static final Color JUNK = Color.valueOf("9a8a6a");

    /** Below this share of his health the crown falls off and he panics. */
    public static final float CROWN_FALLS = 0.5f;
    private static final float SPRAY_WINDUP = 0.8f;
    private static final float SPRAY_CONE = 50f;
    private static final float SPIN_TIME = 2.2f;
    private static final float TUMBLE_TIME = 1f;

    private enum Phase { THRONE, TUMBLE, WILD }

    private Phase phase = Phase.THRONE;
    private float clock;
    private float sprayTimer = 2.5f;
    private float bottleTimer = 1.6f;
    private float decreeTimer = 3.5f;
    private float junkTimer = 1.5f;
    private float spinTimer = 2f;
    private float spray;
    private float sprayAim;
    private float spin;
    private float spinAngle;
    private float spinTick;
    private float tumble;
    private float cartDir = 1f;
    private float cartX;
    private float throneY;
    private final Vector2 cart = new Vector2();
    /** Where his crown fell. */
    private final Vector2 crownAt = new Vector2();
    private final Array<Bullet> bottles = new Array<>();
    private final Array<Bullet> decrees = new Array<>();
    private int decreesSent;

    public SkunkKing(float x, float y, int tier) {
        super(x, y, 1500f + 300f * tier);
        radius = 0.42f;
        speed = 1.5f;
        score = 1000 + 250 * tier;
        boss = true;
        cartX = x;
        throneY = y;
        cart.set(x, y);
    }

    @Override
    public String bossName() {
        return Boss.SKUNK_KING.title;
    }

    /** As he is once beaten: crownless, out of his cart (the normal ending's picture). */
    public void showBeaten() {
        phase = Phase.WILD;
    }

    /** Crownless, panicking: the bar turns red. */
    @Override
    public boolean isEnraged() {
        return phase != Phase.THRONE;
    }

    /** Tumbling out of his cart, nothing touches him. */
    @Override
    protected boolean isDodging() {
        return tumble > 0;
    }

    @Override
    public float feetY() {
        return pos.y - 0.42f;
    }

    @Override
    protected boolean castsShadow() {
        return phase != Phase.THRONE; // (the cart has its own)
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        clock += delta;
        float diff = world.getDifficulty();
        updateBottles(world, diff);
        updateDecrees(world, diff);
        if (phase == Phase.THRONE && hp <= maxHp * CROWN_FALLS) {
            loseTheCrown(world);
        }
        switch (phase) {
            case THRONE:
                paddle(delta);
                throneAttacks(delta, world, diff);
                break;
            case TUMBLE: // rolling out of the cart, crown flying
                tumble -= delta;
                pos.x += cartDir * 1.2f * delta;
                pos.y -= 0.8f * delta;
                if (tumble <= 0) phase = Phase.WILD;
                break;
            default:
                wild(delta, world, diff);
                break;
        }
    }

    /** He paddles his cart to and fro along the top of the room with the plunger. */
    private void paddle(float delta) {
        Arena a = Arena.now;
        cartX += cartDir * 0.6f * delta;
        if (cartX > a.centerX + 2.6f) cartDir = -1f;
        if (cartX < a.centerX - 2.6f) cartDir = 1f;
        cart.set(cartX, throneY);
        pos.set(cartX, throneY + 0.1f);
        walkTime += delta;
    }

    private void throneAttacks(float delta, GameWorld world, float diff) {
        if (tailSpray(delta, world, diff)) return;
        sprayTimer -= delta;
        bottleTimer -= delta;
        decreeTimer -= delta;
        if (!canShoot(world)) return;
        if (sprayTimer <= 0) {
            sprayTimer = MathUtils.random(4.5f, 5.5f) / diff;
            spray = SPRAY_WINDUP;
            sprayAim = aimAngle;
            world.sfx().play("hiss", 0.4f);
        } else if (bottleTimer <= 0) {
            bottleTimer = MathUtils.random(3f, 3.6f) / diff;
            lobBottle(world, diff, 0f);
            lobBottle(world, diff, MathUtils.randomSign() * 0.9f);
        } else if (decreeTimer <= 0) {
            decreeTimer = MathUtils.random(3.6f, 4.4f) / diff;
            sendDecree(world, diff);
        }
    }

    /** The tail goes up and shakes, then the stink billows out in a cone; true while busy with it. */
    private boolean tailSpray(float delta, GameWorld world, float diff) {
        if (spray <= 0) {
            return false;
        }
        spray -= delta;
        if (spray > 0) {
            return true;
        }
        for (int i = 0; i < 4; i++) {
            float a = sprayAim - SPRAY_CONE / 2f + SPRAY_CONE * i / 3f + MathUtils.random(-5f, 5f);
            float s = MathUtils.random(1.8f, 2.3f);
            world.releaseGas(GasCloud.Kind.STINK, pos.x, pos.y - 0.1f, MathUtils.cosDeg(a) * s, MathUtils.sinDeg(a) * s,
                    0.42f, 4.5f);
        }
        for (int i = 0; i < 9; i++) {
            Bullet b = shoot(world, sprayAim - 32f + 64f * i / 8f, 2.4f * diff);
            b.color.set(STINK);
            b.radius = 0.06f;
        }
        world.sfx().play("hiss", 0.9f);
        world.shake(0.2f);
        return true;
    }

    /** A perfume bottle lobbed at (about) the player: it bursts into a ring and a little cloud. */
    private void lobBottle(GameWorld world, float diff, float sideways) {
        float px = world.getPlayer().centerX() + sideways;
        float py = world.getPlayer().hitboxY() + MathUtils.random(-0.3f, 0.3f);
        float a = MathUtils.atan2(py - pos.y, px - pos.x) * MathUtils.radiansToDegrees;
        float s = 2.2f * Math.min(diff, 1.3f);
        Bullet b = shoot(world, a, s);
        b.shape = Bullet.Shape.BOMB;
        b.color.set(PERFUME);
        b.radius = 0.09f;
        b.life = MathUtils.clamp(Vector2.dst(pos.x, pos.y, px, py) / s, 0.5f, 2.2f);
        bottles.add(b);
        world.sfx().play("pop", 0.5f);
    }

    private void updateBottles(GameWorld world, float diff) {
        for (int i = bottles.size - 1; i >= 0; i--) {
            Bullet b = bottles.get(i);
            if (b.alive) continue;
            bottles.removeIndex(i);
            if (world.getBlankRing() >= 0) continue; // (a blank just makes it vanish)
            int n = 10;
            float off = MathUtils.random(360f);
            for (int k = 0; k < n; k++) {
                Bullet r = world.spawnEnemyBullet(b.prev.x, b.prev.y, off + 360f * k / n, 1.8f * diff);
                r.color.set(PERFUME);
            }
            world.releaseGas(GasCloud.Kind.STINK, b.prev.x, b.prev.y, 0f, 0f, 0.3f, 2.5f);
            world.particles().burst(b.prev.x, b.prev.y, 10, 0.3f, 1.2f, 0.4f, 0.04f, PERFUME);
            world.sfx().play("break", 0.6f);
        }
    }

    /** A royal decree: a paper plane that flies a little way, then unfolds into lines of bullets. */
    private void sendDecree(GameWorld world, float diff) {
        Bullet b = shoot(world, aimAngle + MathUtils.random(-10f, 10f), 2.4f);
        b.shape = Bullet.Shape.TRIANGLE;
        b.color.set(PAPER);
        b.radius = 0.1f;
        b.life = 1.1f;
        decrees.add(b);
        world.sfx().play("swish", 0.6f);
    }

    /** Unfolded: a plus, then an x (in turn), each arm a line of three bullets at different speeds. */
    private void updateDecrees(GameWorld world, float diff) {
        for (int i = decrees.size - 1; i >= 0; i--) {
            Bullet b = decrees.get(i);
            if (b.alive) continue;
            decrees.removeIndex(i);
            if (world.getBlankRing() >= 0) continue;
            float off = decreesSent++ % 2 == 0 ? 0f : 45f;
            for (int arm = 0; arm < 4; arm++) {
                for (int k = 0; k < 3; k++) {
                    Bullet r = world.spawnEnemyBullet(b.prev.x, b.prev.y, off + arm * 90f, (1.5f + 0.45f * k) * diff);
                    r.shape = Bullet.Shape.SQUARE;
                    r.color.set(PAPER);
                }
            }
            world.particles().burst(b.prev.x, b.prev.y, 8, 0.3f, 1.2f, 0.4f, 0.04f, PAPER);
        }
    }

    /** The crown falls off: he tumbles out of the cart in a panic. */
    private void loseTheCrown(GameWorld world) {
        phase = Phase.TUMBLE;
        tumble = TUMBLE_TIME;
        spray = 0f;
        crownAt.set(pos.x + MathUtils.random(-0.6f, 0.6f), pos.y + 0.2f);
        world.shake(0.6f);
        world.sfx().play("body_fall", 1f);
        world.sfx().play("break", 0.8f);
        world.particles().burst(pos.x, pos.y + 0.4f, 16, 0.4f, 1.6f, 0.6f, 0.05f, CROWN);
        world.toast("HIS CROWN FELL OFF!");
    }

    /** Crownless: he runs about in a panic, spins spraying stink, throws junk, lobs bottles. */
    private void wild(float delta, GameWorld world, float diff) {
        if (spin > 0) { // spinning round, spraying stink in a spiral
            spin -= delta;
            spinAngle += 260f * delta;
            spinTick -= delta;
            if (spinTick <= 0) {
                spinTick = 0.28f;
                float s = 1.6f;
                world.releaseGas(GasCloud.Kind.STINK, pos.x, pos.y - 0.1f, MathUtils.cosDeg(spinAngle) * s,
                        MathUtils.sinDeg(spinAngle) * s, 0.36f, 5f);
                for (int k = 0; k < 2; k++) {
                    Bullet b = world.spawnEnemyBullet(pos.x, pos.y, spinAngle + k * 180f, 2.2f * diff);
                    b.color.set(STINK);
                    b.radius = 0.06f;
                }
            }
            return;
        }
        // scurrying about, never quite still
        keepDistance(delta, world, 2.3f, speed * (1f + 0.3f * MathUtils.sin(clock * 3f)));
        walkTime += delta;
        spinTimer -= delta;
        junkTimer -= delta;
        bottleTimer -= delta;
        if (spinTimer <= 0) {
            spinTimer = MathUtils.random(5.5f, 6.5f) / diff;
            spin = SPIN_TIME;
            spinAngle = aimAngle;
            spinTick = 0f;
            world.sfx().play("hiss", 0.9f);
        } else if (junkTimer <= 0 && canShoot(world)) { // digging out whatever's to hand and throwing it
            junkTimer = MathUtils.random(1.4f, 1.9f) / diff;
            for (int i = 0; i < 5; i++) {
                Bullet b = shoot(world, aimAngle + MathUtils.random(-28f, 28f), MathUtils.random(2f, 2.8f) * diff);
                b.shape = Bullet.Shape.SQUARE;
                b.color.set(JUNK);
            }
            world.sfx().play("swish", 0.5f);
        } else if (bottleTimer <= 0 && canShoot(world)) {
            bottleTimer = MathUtils.random(3.6f, 4.4f) / diff;
            lobBottle(world, diff, 0f);
        }
    }

    /** The cart where he left it, his fallen crown, and the cone his tail spray is about to fill. */
    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (phase != Phase.THRONE) {
            drawCart(b, d, cart.x, cart.y - 0.42f);
            d.rect(b, crownAt.x - 0.14f, crownAt.y - 0.05f, 0.28f, 0.1f, CROWN); // fallen on its side
            d.rect(b, crownAt.x - 0.14f, crownAt.y - 0.05f, 0.28f, 0.02f, CROWN_RUN);
        }
        if (spray > 0 && alive) {
            float k = 1f - spray / SPRAY_WINDUP;
            for (int edge = -1; edge <= 1; edge++) {
                float a = sprayAim + edge * SPRAY_CONE / 2f;
                for (int i = 1; i <= 6; i++) {
                    float r = 0.4f * i;
                    d.circle(b, pos.x + MathUtils.cosDeg(a) * r, pos.y - 0.1f + MathUtils.sinDeg(a) * r, 0.035f,
                            STINK, 0.2f + 0.5f * k);
                }
            }
        }
    }

    /** His throne: a shopping cart piled with trash bags. */
    private void drawCart(SpriteBatch b, Draw d, float x, float bottom) {
        d.ellipse(b, x, bottom, 0.6f, 0.12f, Color.BLACK, 0.35f);
        for (int s = -1; s <= 1; s += 2) { // wheels
            d.circle(b, x + s * 0.4f, bottom + 0.05f, 0.06f, Color.BLACK);
        }
        d.rect(b, x - 0.55f, bottom + 0.1f, 1.1f, 0.42f, CART, 0.35f); // the wire basket
        for (int i = 0; i <= 5; i++) {
            d.rect(b, x - 0.55f + i * 0.22f, bottom + 0.1f, 0.02f, 0.42f, CART);
        }
        for (int i = 0; i <= 2; i++) {
            d.rect(b, x - 0.55f, bottom + 0.1f + i * 0.2f, 1.1f, 0.02f, CART);
        }
        d.rect(b, x + 0.55f, bottom + 0.5f, 0.25f, 0.03f, CART); // the handle
        for (int i = 0; i < 3; i++) { // the trash bags heaped in it
            d.circle(b, x - 0.32f + i * 0.32f, bottom + 0.5f, 0.17f, BAG);
            d.circle(b, x - 0.36f + i * 0.32f, bottom + 0.56f, 0.04f, FUR_LIGHT);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        boolean throne = phase == Phase.THRONE;
        float face = facingRight() ? 1 : -1;
        float base = throne ? cart.y + 0.15f : feetY();
        float x = pos.x;
        if (tumble > 0) { // flailing as he rolls out
            base += MathUtils.sin(tumble / TUMBLE_TIME * MathUtils.PI) * 0.4f;
        }
        if (throne) {
            drawCart(b, d, cart.x, cart.y - 0.42f);
        }
        float shake = spray > 0 ? MathUtils.sin(clock * 50f) * 0.04f : 0f;
        float tailUp = spray > 0 ? 0.25f : 0f;
        // the big striped tail, curled up behind him (raised high and shaking before a spray)
        float tx = x - face * 0.32f + shake;
        float ty = base + 0.55f + tailUp;
        d.ellipse(b, tx, ty, 0.24f, 0.36f, c(FUR), 1f);
        d.ellipse(b, tx, ty + 0.05f, 0.07f, 0.3f, STRIPE, 1f);
        d.ellipse(b, tx + face * 0.06f, ty + 0.3f, 0.16f, 0.12f, c(FUR), 1f);
        // the shower-curtain cape, ducks on it
        d.rect(b, x - 0.34f, base + 0.05f, 0.68f, 0.5f, CURTAIN, 0.9f);
        for (int i = 0; i < 4; i++) {
            d.circle(b, x - 0.25f + (i % 2) * 0.4f + (i / 2) * 0.1f, base + 0.15f + (i / 2) * 0.22f, 0.04f, DUCK);
        }
        // the round body and the white stripe across it - his "royal sash"
        d.ellipse(b, x, base + 0.3f, 0.32f, 0.3f, c(FUR), 1f);
        d.rotRect(b, x, base + 0.32f, 0.12f, 0.62f, face * 35f, STRIPE, 1f);
        if (!throne) { // little legs, scurrying
            float run = MathUtils.sin(walkTime * 16f) * 0.03f;
            d.rect(b, x - 0.17f, base - 0.04f + run, 0.1f, 0.08f, c(FUR));
            d.rect(b, x + 0.07f, base - 0.04f - run, 0.1f, 0.08f, c(FUR));
        }
        // the plunger sceptre
        float sx = x + face * 0.3f;
        d.rotRect(b, sx, base + 0.4f, 0.04f, 0.5f, -face * 15f, STICK, 1f);
        d.ellipse(b, sx + face * 0.06f, base + 0.66f, 0.09f, 0.06f, PLUNGER, 1f);
        d.circle(b, x + face * 0.26f, base + 0.3f, 0.07f, c(FUR)); // the paw holding it
        // the head: a white streak up the middle, a pink nose, smug half-shut eyes (wide and panicky later)
        float hx = x + face * 0.04f;
        float hy = base + 0.68f;
        d.circle(b, hx, hy, 0.2f, c(FUR));
        d.rect(b, hx - 0.035f, hy + 0.02f, 0.07f, 0.2f, STRIPE);
        d.circle(b, hx - 0.14f, hy + 0.16f, 0.06f, c(FUR)); // ears
        d.circle(b, hx + 0.14f, hy + 0.16f, 0.06f, c(FUR));
        d.circle(b, hx + face * 0.12f, hy - 0.06f, 0.04f, NOSE);
        for (int s = -1; s <= 1; s += 2) {
            float ex = hx + s * 0.08f + face * 0.03f;
            if (throne) { // smug: lids half down
                d.rect(b, ex - 0.035f, hy, 0.07f, 0.035f, Color.WHITE);
                d.rect(b, ex - 0.035f + face * 0.01f, hy, 0.03f, 0.03f, PUPIL);
                d.rect(b, ex - 0.04f, hy + 0.035f, 0.08f, 0.02f, c(FUR_LIGHT));
            } else { // panicking: wide eyes, tiny pupils
                d.circle(b, ex, hy + 0.02f, 0.045f, Color.WHITE);
                d.circle(b, ex + face * 0.01f, hy + 0.02f, 0.015f, PUPIL);
            }
        }
        if (throne) { // the cardboard crown, gold paint running
            d.rect(b, hx - 0.15f, hy + 0.17f, 0.3f, 0.09f, CROWN);
            for (int i = 0; i < 3; i++) {
                d.rect(b, hx - 0.15f + i * 0.12f, hy + 0.26f, 0.06f, 0.07f, CROWN);
                d.rect(b, hx - 0.13f + i * 0.12f, hy + 0.13f, 0.015f, 0.05f, CROWN_RUN); // drips
            }
        } else { // a messy tuft where the crown was, and an "O" of a mouth
            for (int i = -1; i <= 1; i++) {
                d.rotRect(b, hx + i * 0.05f, hy + 0.22f, 0.025f, 0.08f, i * 25f, c(FUR_LIGHT), 1f);
            }
            d.circle(b, hx + face * 0.06f, hy - 0.12f, 0.03f, PUPIL);
        }
    }
}
