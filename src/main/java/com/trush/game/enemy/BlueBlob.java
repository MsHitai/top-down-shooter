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
 * The Slums' first boss (Egor's idea, after ETG's Blobulord): a big, wobbly, cheerful blue blob.
 * At 75% of the fight's health it splits in two; at 50% each half splits again - four little blobs. Every
 * blob, big or small, does the same things, smaller ones just with less health and smaller patterns:
 * <ul>
 *   <li>a blobby volley - a fan of gooey drops at the player;</li>
 *   <li>a happy bounce - it jumps up joyfully toward the player and, as it lands, drops fly out in a ring.</li>
 * </ul>
 * The fight is won when the last blob pops. All the blobs share one {@link Family}: the boss bar shows its
 * health, there's one introduction card for the whole fight, and a blob that pops while others are left
 * just splats - only the last one gets a boss's death show.
 */
public class BlueBlob extends Enemy {

    /** The blobs of one fight, together. */
    static final class Family {
        final float maxHp;
        final Array<BlueBlob> members = new Array<>();
        boolean enraged;
        int score;

        Family(float maxHp) {
            this.maxHp = maxHp;
        }

        float hp() {
            float sum = 0;
            for (BlueBlob b : members) {
                if (b.alive) sum += Math.max(0f, b.hp);
            }
            return sum;
        }

        int alive() {
            int n = 0;
            for (BlueBlob b : members) {
                if (b.alive) n++;
            }
            return n;
        }
    }

    private static final Color GOO = Color.valueOf("4a8ae8");
    private static final Color GOO_LIGHT = Color.valueOf("8ac0ff");
    private static final Color GOO_DARK = Color.valueOf("2a5aa8");
    private static final Color CHEEK = Color.valueOf("ff8ac8");
    private static final Color DROP = Color.valueOf("5aa0ff");

    /** Sizes: 3 the big one, 2 a half, 1 a quarter. */
    public static final int BIG = 3;
    /** The fight's health left (a share of the whole) at which the big one splits, then the halves. */
    public static final float FIRST_SPLIT = 0.75f;
    public static final float SECOND_SPLIT = 0.5f;
    private static final float AIR_TIME = 0.85f;
    /** How hard twins squirt apart on a split (times the radius): far enough to clear each other. */
    private static final float SQUIRT = 9f;

    private final Family family;
    private int size;
    private float clock = MathUtils.random(10f);
    private float volleyTimer;
    private float bounceTimer;
    /** In the air on a bounce: time left, and where it's going. */
    private float air;
    private final Vector2 from = new Vector2();
    private final Vector2 to = new Vector2();
    /** A squash on landing / a jiggle when hit. */
    private float squish;
    private final Vector2 slide = new Vector2();
    private final Vector2 tmpAway = new Vector2();

    public BlueBlob(float x, float y, int tier) {
        this(x, y, BIG, new Family(900f + 300f * tier));
        hp = family.maxHp;
        maxHp = family.maxHp;
        score = 600 + 250 * tier;
        family.score = score;
    }

    private BlueBlob(float x, float y, int size, Family family) {
        super(x, y, family.maxHp);
        this.family = family;
        family.members.add(this);
        boss = true;
        setSize(size);
        volleyTimer = MathUtils.random(1.2f, 2f);
        bounceTimer = MathUtils.random(3f, 4.5f);
    }

    private void setSize(int size) {
        this.size = size;
        radius = size == BIG ? 0.62f : size == 2 ? 0.42f : 0.28f;
        speed = size == BIG ? 0.45f : size == 2 ? 0.65f : 0.85f;
    }

    public int size() {
        return size;
    }

    @Override
    public String bossName() {
        return Boss.BLUE_BLOB.title;
    }

    /** Enraged once it has split all the way: four little blobs left. */
    @Override
    public boolean isEnraged() {
        return family.enraged;
    }

    /** The boss bar: the whole family's health. */
    @Override
    public float bossBarFraction() {
        return family.hp() / family.maxHp;
    }

    /** One fight, one introduction card: the blobs share it. */
    @Override
    public Object bossFight() {
        return family;
    }

    @Override
    public float feetY() {
        return pos.y - radius * 0.7f;
    }

    /** Its height above the floor on a bounce. */
    private float lift() {
        return air > 0 ? MathUtils.sin((1f - air / AIR_TIME) * MathUtils.PI) * (0.5f + 0.3f * size) : 0f;
    }

    @Override
    public void hit(float damage, Vector2 bulletVel, float knockback, GameWorld world) {
        if (!alive || isSpawning()) {
            return;
        }
        squish = Math.max(squish, 0.15f);
        if (hp - damage <= 0 && family.alive() == 1) {
            score = family.score; // the last one carries the boss's score
        } else if (hp - damage <= 0) { // not the last: it just splats, no boss send-off
            boss = false;
            vanishOnDeath = true;
            world.particles().burst(pos.x, pos.y, 18, 0.4f, 2f, 0.5f, 0.06f, GOO_LIGHT);
            world.sfx().play("pop", 1f);
        }
        super.hit(damage, bulletVel, knockback, world);
        if (alive) {
            maybeSplit(world);
        }
    }

    /** Big at 75% of the fight's health, halves at 50%: each split leaves this one and a twin of half its health. */
    private void maybeSplit(GameWorld world) {
        float share = family.hp() / family.maxHp;
        boolean split = size == BIG ? share <= FIRST_SPLIT : size == 2 && share <= SECOND_SPLIT;
        if (!split) {
            return;
        }
        setSize(size - 1);
        hp /= 2f;
        float away = MathUtils.random(360f);
        BlueBlob twin = new BlueBlob(pos.x, pos.y, size, family);
        score = 120 * size; // (the last one to pop gets the boss's score back: see hit)
        twin.score = score;
        twin.hp = hp;
        twin.maxHp = hp;
        twin.spawnTimer = 0f; // it pops straight out, no summoning circle
        twin.slide.set(MathUtils.cosDeg(away), MathUtils.sinDeg(away)).scl(SQUIRT * radius);
        slide.set(twin.slide).scl(-1f);
        twin.volleyTimer = volleyTimer + 0.9f; // out of step, so they don't all fire at once
        twin.bounceTimer = bounceTimer + 1.7f;
        world.spawnEnemy(twin);
        air = 0f;
        squish = 0.3f;
        if (size == 1) family.enraged = true;
        world.particles().burst(pos.x, pos.y, 24, 0.5f, 2.2f, 0.6f, 0.07f, GOO_LIGHT);
        world.sfx().play("pop", 1f);
        world.shake(0.35f);
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        clock += delta;
        squish = Math.max(0f, squish - delta);
        float diff = world.getDifficulty();
        if (slide.len2() > 0.01f) { // squirting apart after a split
            pos.mulAdd(slide, delta);
            slide.scl(Math.max(0f, 1f - 4f * delta));
            Arena.now.clamp(pos, radius);
        }
        if (air <= 0) {
            keepApart(delta);
        }
        if (air > 0) { // on a bounce: it flies, harmless to touch, and comes down with a ring of drops
            air -= delta;
            float k = 1f - Math.max(0f, air) / AIR_TIME;
            pos.set(from).lerp(to, k);
            if (air <= 0) {
                land(world, diff);
            }
            return;
        }
        hurtsToTouch = true;
        // wobbles after the player at its own pace
        float a = aimAngle + MathUtils.sin(clock * 1.7f) * 30f;
        pos.add(MathUtils.cosDeg(a) * speed * delta, MathUtils.sinDeg(a) * speed * delta);
        walkTime += delta;
        Arena.now.clamp(pos, radius);

        float pace = (family.enraged ? 1.25f : 1f) * diff;
        volleyTimer -= delta * pace;
        if (volleyTimer <= 0 && canShoot(world)) {
            volley(world, diff);
            volleyTimer = MathUtils.random(1.6f, 2.2f);
        }
        bounceTimer -= delta * pace;
        if (bounceTimer <= 0) {
            bounce(world);
            bounceTimer = MathUtils.random(3.4f, 4.4f);
        }
    }

    /** Blobs on the floor ease off each other, so two never sit on one spot (their faces would flicker). */
    private void keepApart(float delta) {
        for (BlueBlob o : family.members) {
            if (o == this || !o.alive || o.air > 0) continue;
            float gap = radius + o.radius;
            float d2 = pos.dst2(o.pos);
            if (d2 < gap * gap) {
                float d = (float) Math.sqrt(d2);
                tmpAway.set(pos).sub(o.pos);
                if (d < 0.001f) tmpAway.set(1, 0).rotateDeg(MathUtils.random(360f));
                pos.add(tmpAway.nor().scl(Math.min(gap - d, 3f * delta)));
            }
        }
        Arena.now.clamp(pos, radius);
    }

    /** A fan of gooey drops at the player: five from the big one, three from a half, two from a quarter. */
    private void volley(GameWorld world, float diff) {
        int n = size == BIG ? 5 : size == 2 ? 3 : 2;
        float spread = size == BIG ? 14f : 16f;
        for (int i = 0; i < n; i++) {
            Bullet b = shoot(world, aimAngle + (i - (n - 1) / 2f) * spread, (2.4f + 0.2f * (3 - size)) * diff);
            b.shape = Bullet.Shape.GLOB;
            b.color.set(DROP);
        }
        squish = 0.2f;
        world.sfx().play("pop", 0.6f);
    }

    /** Up it goes, happily, to a spot toward the player. */
    private void bounce(GameWorld world) {
        from.set(pos);
        float reach = 1.2f + 0.3f * size;
        float dist = Math.min(reach, distanceToPlayer(world));
        to.set(pos).add(MathUtils.cosDeg(aimAngle) * dist, MathUtils.sinDeg(aimAngle) * dist);
        Arena.now.clamp(to, radius);
        for (BlueBlob o : family.members) { // never come down on top of another blob (or where one is landing)
            if (o == this || !o.alive) continue;
            Vector2 there = o.air > 0 ? o.to : o.pos;
            float gap = radius + o.radius + 0.1f;
            if (to.dst2(there) < gap * gap) {
                tmpAway.set(to).sub(there);
                if (tmpAway.len2() < 0.0001f) tmpAway.set(1, 0).rotateDeg(MathUtils.random(360f));
                to.set(there).add(tmpAway.nor().scl(gap));
                Arena.now.clamp(to, radius);
            }
        }
        air = AIR_TIME;
        hurtsToTouch = false;
        world.sfx().play("dodge", 0.8f);
    }

    /** Splat: a ring of drops all round (bigger blobs, bigger rings; a gap-free ring from the big one). */
    private void land(GameWorld world, float diff) {
        squish = 0.35f;
        hurtsToTouch = true;
        int n = size == BIG ? 22 : size == 2 ? 14 : 9;
        float off = MathUtils.random(360f);
        for (int i = 0; i < n; i++) {
            Bullet b = world.spawnEnemyBullet(pos.x, feetY() + 0.05f, off + 360f * i / n, 1.9f * diff);
            b.shape = Bullet.Shape.GLOB;
            b.color.set(DROP);
        }
        world.particles().burst(pos.x, feetY(), 14, 0.4f, 1.6f, 0.4f, 0.06f, GOO_LIGHT);
        world.sfx().play("body_fall", 0.8f);
        world.shake(0.15f + 0.1f * size);
    }

    @Override
    protected boolean castsShadow() {
        return true;
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float lift = lift();
        float wob = MathUtils.sin(clock * 5f) * 0.05f;
        float sq = squish > 0 ? squish * 0.8f : 0f;
        float stretch = air > 0 ? 0.12f : 0f; // long and thin in the air, flat on landing
        float rx = radius * (1.05f + wob + sq - stretch);
        float ry = radius * (0.85f - wob - sq * 0.8f + stretch);
        float cx = pos.x;
        float cy = feetY() + ry + lift;
        d.ellipse(b, cx, cy, rx + 0.03f, ry + 0.03f, GOO_DARK, 1f);
        d.ellipse(b, cx, cy, rx, ry, c(GOO), 1f);
        d.ellipse(b, cx, cy - ry * 0.35f, rx * 0.85f, ry * 0.5f, c(GOO_DARK), 0.35f); // a darker belly
        d.ellipse(b, cx - rx * 0.4f, cy + ry * 0.45f, rx * 0.25f, ry * 0.18f, Color.WHITE, 0.55f); // the shine
        // a happy face: round eyes, rosy cheeks, a big smile (an "o" while in the air)
        float face = facingRight() ? 1 : -1;
        float ex = cx + face * rx * 0.15f;
        float ey = cy + ry * 0.2f;
        float er = radius * 0.16f;
        for (int s = -1; s <= 1; s += 2) {
            d.circle(b, ex + s * rx * 0.3f, ey, er, Color.WHITE);
            d.circle(b, ex + s * rx * 0.3f + face * er * 0.3f, ey - er * 0.1f, er * 0.55f, PUPIL);
            d.circle(b, ex + s * rx * 0.48f, ey - er * 1.5f, er * 0.6f, CHEEK, 0.6f);
        }
        if (air > 0) {
            d.circle(b, ex, ey - er * 2.2f, er * 0.55f, GOO_DARK);
        } else {
            d.rotRect(b, ex, ey - er * 2f, rx * 0.4f, er * 0.5f, 0f, GOO_DARK, 1f);
            d.rotRect(b, ex - rx * 0.2f, ey - er * 1.7f, rx * 0.12f, er * 0.5f, 30f, GOO_DARK, 1f);
            d.rotRect(b, ex + rx * 0.2f, ey - er * 1.7f, rx * 0.12f, er * 0.5f, -30f, GOO_DARK, 1f);
        }
        if (family.enraged) { // little angry brows once it's in pieces
            d.rotRect(b, ex - rx * 0.3f, ey + er * 1.4f, er * 1.6f, er * 0.4f, -15f * face, GOO_DARK, 1f);
            d.rotRect(b, ex + rx * 0.3f, ey + er * 1.4f, er * 1.6f, er * 0.4f, 15f * face, GOO_DARK, 1f);
        }
    }
}
