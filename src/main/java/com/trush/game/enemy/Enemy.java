package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * Base class for everything that walks around and shoots at the player.
 * Subclasses implement {@link #behave(float, GameWorld)} and {@link #drawBody(SpriteBatch, Draw)}.
 * {@code pos} is the centre of the body; {@code radius} is the hitbox.
 */
public abstract class Enemy {

    public static final float SPAWN_TIME = 0.9f;
    /** How long the player may hide behind cover before a boss walks over to get a clear shot. */
    private static final float HUNT_DELAY = 0.8f;

    protected static final Color EYE_WHITE = Color.WHITE;
    protected static final Color PUPIL = Color.valueOf("111111");
    protected static final Color GUN = Color.valueOf("4a4a55");
    protected static final Color FEET = Color.valueOf("2b2024");
    protected static final Color TELEGRAPH = Color.valueOf("ff4d4d");
    private static final Color SUMMON = Color.valueOf("ff6b81");
    private static final Color DAZE = Color.valueOf("ffe066");
    private static final Color BLAST = Color.valueOf("ffb347");
    private static final Color SMOKE = Color.valueOf("6b6670");

    /** How long a regular enemy takes to topple over and vanish, and a boss's whole death show. */
    private static final float DEATH_TIME = 0.55f;
    private static final float FALL_TIME = 0.4f;
    public static final float BOSS_DEATH_TIME = 2.6f;
    /** When the boss finally blows apart (it shrinks away after that). */
    private static final float BOSS_BLAST_AT = 2.0f;
    private static final Matrix4 savedTransform = new Matrix4();
    private static final Matrix4 deathTransform = new Matrix4();

    public final Vector2 pos = new Vector2();
    protected final Vector2 knock = new Vector2();
    private final Vector2 tmp = new Vector2();

    public float radius = 0.17f;
    public float hp;
    public float maxHp;
    public float speed = 0.9f;
    public int score = 10;
    public boolean alive = true;
    public boolean boss;
    /** Flies over furniture: not pushed out of props (walls still keep it in the room). */
    public boolean flying;
    /** Small critters (rats) die when the player dodge-rolls over them. */
    public boolean squishable;

    protected float flash;
    protected float spawnTimer = SPAWN_TIME;
    protected float aimAngle;
    protected float walkTime;
    protected float strafeDir = MathUtils.randomSign();
    protected float strafeTimer = MathUtils.random(1f, 2.5f);
    /** Whether a bullet fired now could reach the player (no wall or cover in between). */
    protected boolean canSeePlayer = true;
    private final Vector2 detour = new Vector2();
    private boolean hasDetour;
    private float repathTimer;
    private float hiddenTime;
    private boolean hunting;
    /** Dazed by a blank this frame (little stars circle its head). */
    private boolean dazed;
    private float dazeClock;
    /** Dying: time since the killing blow, which way it was hit, and whether it fell into a pit. */
    private float deathTime;
    private final Vector2 deathDir = new Vector2();
    public boolean fellInPit;
    /** Leaves nothing to topple over when it dies (it blew itself up). */
    protected boolean vanishOnDeath;
    private float nextBlast;
    /** Slowed down (a snowball, the Snow Globe) for this many more seconds: moves and attacks at half speed. */
    private float chill;
    private static final Color CHILL = Color.valueOf("9ad8f5");
    /** Frozen solid (Frost Rounds) for this many more seconds: doesn't move, shoot or hurt to touch. */
    private float frozen;
    /** On fire / eaten by acid for this many more seconds: GameWorld deals the damage (see takeAilmentDamage). */
    private float burning;
    private float poisoned;
    private float ailmentTick;
    /** Damage a second from burning and from acid, before the floor's enemy health scaling. */
    public static final float BURN_DPS = 6f;
    public static final float POISON_DPS = 4f;
    private static final float AILMENT_TICK = 0.33f;
    /** Walks through acid unharmed (the Sludger, in its hazmat suit). */
    public boolean acidProof;
    private static final Color BURN_TINT = Color.valueOf("ff7a3a");
    private static final Color POISON_TINT = Color.valueOf("8ae83a");
    private static final Color ICE = Color.valueOf("c8eeff");
    private static final Color FLAME = Color.valueOf("ff8a1e");
    private static final Color FLAME_CORE = Color.valueOf("ffd84a");
    private static final Color ACID_BUBBLE = Color.valueOf("c8ff8a");
    /** c() hands out tinted colours from a little ring, so a few can be held at once while drawing. */
    private final Color[] tints = {new Color(), new Color(), new Color(), new Color()};
    private int tintIndex;

    protected Enemy(float x, float y, float hp) {
        pos.set(x, y);
        this.hp = hp;
        this.maxHp = hp;
    }

    public boolean isSpawning() {
        return spawnTimer > 0;
    }

    /** Bullets, beams and bumping into it pass right through: still spawning, or mid-dodge (see isDodging). */
    public boolean isUntouchable() {
        return isSpawning() || isDodging();
    }

    /** Dodge-rolling out of the way of a bullet (the city's robbers). */
    protected boolean isDodging() {
        return false;
    }

    /** Bottom of the body, used for draw ordering against the player. */
    public abstract float feetY();

    public final void update(float delta, GameWorld world) {
        flash -= delta;
        if (spawnTimer > 0) {
            spawnTimer -= delta;
            return;
        }
        // regular enemies aim at the Cardboard Hero while it stands; bosses aren't fooled
        float tx = targetX(world);
        float ty = targetY(world);
        aimAngle = MathUtils.atan2Deg360(ty - pos.y, tx - pos.x);
        canSeePlayer = world.hasLineOfSight(pos.x, pos.y, tx, ty, GameWorld.SIGHT_THICKNESS);
        chill = Math.max(0f, chill - delta);
        if (frozen > 0) { // a block of ice: nothing happens until it thaws (a shove still slides it)
            frozen -= delta;
            pos.mulAdd(knock, delta);
            knock.scl(Math.max(0f, 1f - 10f * delta));
            Arena.now.clamp(pos, radius);
            return;
        }
        if (chill > 0) { // snowed on: everything it does runs slower (bosses shake it off better)
            delta *= boss ? 0.75f : 0.5f;
        }

        pos.mulAdd(knock, delta);
        knock.scl(Math.max(0f, 1f - 10f * delta));

        dazed = world.getBlankCalm() > 0;
        if (dazed) { // a blank just went off: no attacks for a moment
            dazeClock += delta;
            Arena.now.clamp(pos, radius);
            return;
        }
        int bulletsBefore = world.getEnemyBullets().size;
        behave(delta, world);
        if (boss) { // whatever a boss fired this frame gets the chunkier, slower boss look
            world.styleBossBullets(bulletsBefore);
        }
        Arena.now.clamp(pos, radius);
    }

    protected abstract void behave(float delta, GameWorld world);

    /** Slows it down for {@code seconds} (the Snowball Slinger, the Snow Globe); it turns icy blue meanwhile. */
    public void chill(float seconds) {
        chill = Math.max(chill, seconds);
    }

    /** Frost Rounds: frozen solid for {@code seconds}. Bosses are too big to freeze - they're only chilled. */
    public void freeze(float seconds) {
        if (boss) {
            chill(seconds * 1.5f);
        } else {
            frozen = Math.max(frozen, seconds);
        }
    }

    public boolean isFrozen() {
        return frozen > 0;
    }

    /** Set on fire for {@code seconds} (Ember Rounds, a flare, flames on the floor). */
    public void ignite(float seconds) {
        burning = Math.max(burning, seconds);
        frozen = 0; // the ice melts
    }

    /** Eaten at by acid for {@code seconds} (the acid guns, a pool of acid). */
    public void poison(float seconds) {
        if (!acidProof) {
            poisoned = Math.max(poisoned, seconds);
        }
    }

    public boolean isBurning() {
        return burning > 0;
    }

    public boolean isPoisoned() {
        return poisoned > 0;
    }

    /** Puts its flames out (it went under water). */
    public void douse() {
        burning = 0;
    }

    /**
     * Burning and acid wear it down in little bites a few times a second; returns the damage due this frame
     * (0 most frames). GameWorld deals it, so it counts as the player's damage.
     */
    public float takeAilmentDamage(float delta) {
        if (burning <= 0 && poisoned <= 0) {
            ailmentTick = 0;
            return 0f;
        }
        float dps = (burning > 0 ? BURN_DPS : 0f) + (poisoned > 0 ? POISON_DPS : 0f);
        burning = Math.max(0f, burning - delta);
        poisoned = Math.max(0f, poisoned - delta);
        ailmentTick += delta;
        if (ailmentTick < AILMENT_TICK) {
            return 0f;
        }
        ailmentTick -= AILMENT_TICK;
        return dps * AILMENT_TICK;
    }

    /** Where it aims: the player, or the Cardboard Hero while one stands (bosses always go for the player). */
    private float targetX(GameWorld world) {
        return !boss && world.hasDecoy() ? world.decoyX() : world.getPlayer().centerX();
    }

    private float targetY(GameWorld world) {
        return !boss && world.hasDecoy() ? world.decoyY() : world.getPlayer().hitboxY();
    }

    /**
     * Pushes it away along the unit vector (dx, dy) at {@code speed} or more (the Fluff Field). Works like a
     * bullet's knockback, so it can shove a walker into a pit.
     */
    public void shove(float dx, float dy, float speed) {
        float along = knock.x * dx + knock.y * dy;
        if (along < speed) {
            knock.mulAdd(tmp.set(dx, dy), speed - along);
        }
    }

    public void hit(float damage, Vector2 bulletVel, float knockback, GameWorld world) {
        if (!alive || isSpawning()) {
            return;
        }
        hp -= damage;
        flash = 0.08f;
        if (!boss) {
            tmp.set(bulletVel).nor().scl(knockback * 2.5f);
            knock.add(tmp);
        }
        if (hp <= 0) {
            alive = false;
            deathDir.set(bulletVel);
            flash = boss ? 0f : 0.1f; // a white flash as it goes down
            dropLoot(world);
            world.onEnemyKilled(this);
        }
    }

    // ---------------------------------------------------------------- helpers

    protected float distanceToPlayer(GameWorld world) {
        return Vector2.dst(pos.x, pos.y, targetX(world), targetY(world));
    }

    /**
     * Approach until roughly {@code preferred} units away, then circle-strafe.
     * Behind cover, walks around it to the nearest spot with a clear shot instead.
     */
    protected void keepDistance(float delta, GameWorld world, float preferred, float moveSpeed) {
        if (canSeePlayer) {
            repathTimer = 0; // look for a new way around as soon as the player ducks behind something
        } else if (flank(delta, world, moveSpeed)) {
            return;
        }
        strafeTimer -= delta;
        if (strafeTimer <= 0) {
            strafeTimer = MathUtils.random(1f, 2.5f);
            strafeDir = MathUtils.randomSign();
        }
        float dist = distanceToPlayer(world);
        float cos = MathUtils.cosDeg(aimAngle);
        float sin = MathUtils.sinDeg(aimAngle);
        float forward = 0;
        float strafe = 0.6f * strafeDir;
        if (!world.onScreen(pos.x, pos.y, 0.4f)) {
            // off screen it can't attack (no shots from outside the player's view): come into view first,
            // or it would hang about at its favourite distance just past the edge forever
            forward = 1;
            strafe *= 0.3f;
        } else if (dist > preferred + 0.3f) forward = 1;
        else if (dist < preferred - 0.3f) forward = -0.8f;
        tmp.set(cos * forward - sin * strafe, sin * forward + cos * strafe);
        if (!tmp.isZero()) {
            tmp.nor().scl(moveSpeed * delta);
            pos.add(tmp);
            walkTime += delta;
        }
    }

    /**
     * For bosses (too big for the nav grid): once the player has hidden behind cover for a moment,
     * points {@code target} at a nearby spot with a clear shot. Returns true while walking there.
     */
    protected boolean huntForSight(float delta, GameWorld world, Vector2 target) {
        if (canSeePlayer) {
            hiddenTime = 0;
            hunting = false;
            return false;
        }
        hiddenTime += delta;
        if (hiddenTime > HUNT_DELAY && (!hunting || pos.dst2(target) < 0.05f)) {
            hunting = world.findOpenShot(pos, radius, 1.8f, flying, target);
        }
        return hunting;
    }

    /** How fast a bullet hit is still shoving this enemy (pits: a shoved enemy can't stop at the edge). */
    public float knockSpeed() {
        return knock.len();
    }

    /** Can start an attack: sees the player and is on screen (no shots from outside the player's view). */
    protected boolean canShoot(GameWorld world) {
        return canSeePlayer && world.onScreen(pos.x, pos.y, 0.3f);
    }

    /**
     * Plays its death once it's been killed (GameWorld keeps it around until this returns false):
     * regular enemies topple over away from the shot and vanish in a puff of smoke, fallers spin down
     * their pit, and bosses shake and flash with explosions bursting all over them, then blow apart.
     */
    public boolean updateDeath(float delta, GameWorld world) {
        if (vanishOnDeath) {
            return false;
        }
        float before = deathTime;
        deathTime += delta;
        flash -= delta;
        if (boss) {
            if (deathTime < BOSS_BLAST_AT) {
                nextBlast -= delta;
                if (nextBlast <= 0) {
                    nextBlast = MathUtils.random(0.1f, 0.17f);
                    float x = pos.x + MathUtils.random(-1f, 1f) * radius * 1.2f;
                    float y = pos.y + MathUtils.random(-0.8f, 1f) * radius * 1.2f;
                    world.particles().burst(x, y, 10, 0.2f, 1.6f, 0.35f, 0.06f, BLAST);
                    world.particles().burst(x, y, 4, 0.1f, 0.6f, 0.6f, 0.07f, SMOKE);
                    world.sfx().play("boom", 0.45f);
                    world.shake(0.15f);
                    flash = 0.035f;
                }
            } else if (before < BOSS_BLAST_AT) { // the big one
                world.particles().burst(pos.x, pos.y, 80, 0.5f, 4.5f, 0.8f, 0.09f, BLAST);
                world.particles().burst(pos.x, pos.y, 40, 0.4f, 2f, 1f, 0.1f, SMOKE);
                world.particles().burst(pos.x, pos.y, 30, 0.3f, 3f, 0.6f, 0.05f, Color.WHITE);
                world.sfx().play("boom", 1f);
                world.shake(1.2f);
                flash = 0.6f;
            }
            return deathTime < BOSS_DEATH_TIME;
        }
        if (fellInPit) {
            return deathTime < FALL_TIME;
        }
        if (before < 0.3f && deathTime >= 0.3f) { // gone in a puff
            world.particles().burst(pos.x, feetY() + 0.1f, 8, 0.2f, 0.8f, 0.45f, 0.05f, SMOKE);
        }
        return deathTime < DEATH_TIME;
    }

    /** Draws the body twisted by the death animation (see updateDeath). */
    private void drawDying(SpriteBatch b, Draw d) {
        float px = pos.x;
        float py = feetY();
        float angle = 0;
        float scale;
        if (boss) {
            float shake = deathTime < BOSS_BLAST_AT ? 0.02f + 0.04f * deathTime / BOSS_BLAST_AT : 0f;
            px += MathUtils.random(-shake, shake);
            py += MathUtils.random(-shake, shake);
            scale = deathTime < BOSS_BLAST_AT ? 1f
                    : 1f - Interpolation.pow2In.apply((deathTime - BOSS_BLAST_AT) / (BOSS_DEATH_TIME - BOSS_BLAST_AT));
            py = pos.y;
        } else if (fellInPit) {
            float k = deathTime / FALL_TIME;
            angle = k * 360f;
            scale = 1f - k;
            py = pos.y;
        } else {
            // topples away from the shot, then shrinks into the floor
            float side = deathDir.x >= 0 ? -1f : 1f;
            angle = side * 90f * Interpolation.pow2Out.apply(Math.min(1f, deathTime / 0.25f));
            scale = deathTime < 0.3f ? 1f : 1f - (deathTime - 0.3f) / (DEATH_TIME - 0.3f);
        }
        if (scale <= 0.01f) {
            return;
        }
        if ((!boss || deathTime < BOSS_BLAST_AT) && castsShadow()) {
            d.ellipse(b, pos.x, feetY(), radius * 1.1f * scale, radius * 0.35f * scale, Color.BLACK, 0.35f);
        }
        savedTransform.set(b.getTransformMatrix());
        deathTransform.set(savedTransform).translate(px, py, 0).rotate(0, 0, 1, angle).scale(scale, scale, 1)
                .translate(-pos.x, -(boss || fellInPit ? pos.y : feetY()), 0);
        b.setTransformMatrix(deathTransform);
        drawBody(b, d);
        b.setTransformMatrix(savedTransform);
    }

    /** Called once when it dies (before GameWorld.onEnemyKilled): anything it always drops, like a mimic's loot. */
    public void dropLoot(GameWorld world) {
    }

    /** Drawn on the floor under everything (e.g. the Evil Train's rails); nothing by default. */
    public void drawFloor(SpriteBatch b, Draw d) {
    }

    /** Drawn over everything, even the night's darkness (warnings that must be seen); nothing by default. */
    public void drawOverlay(SpriteBatch b, Draw d, Rectangle view) {
    }

    /** Draws just the body, facing {@code aim} degrees, at {@code pos} (the opening story's pictures). */
    public void drawPortrait(SpriteBatch b, Draw d, float aim) {
        aimAngle = aim;
        drawBody(b, d);
    }

    /** How far the camera pulls back for this boss (1 = normal; the giant Kaiju wants room to see it all). */
    public float viewZoom() {
        return 1f;
    }

    /** Draws the usual round shadow under it (not for something standing far behind the arena). */
    protected boolean castsShadow() {
        return true;
    }

    /** Name on the boss health bar; null for regular enemies. */
    public String bossName() {
        return null;
    }

    /** Bosses below half health: the boss bar turns red. */
    public boolean isEnraged() {
        return false;
    }

    /** Walks toward the nearest spot with line of sight to the player. False if there is none. */
    protected boolean flank(float delta, GameWorld world, float moveSpeed) {
        repathTimer -= delta;
        if (repathTimer <= 0 || (hasDetour && pos.dst2(detour) < 0.05f * 0.05f)) {
            repathTimer = 0.3f;
            hasDetour = world.findSightPoint(pos, radius, 1f, detour);
        }
        if (!hasDetour) {
            return false;
        }
        tmp.set(detour).sub(pos);
        float len = tmp.len();
        if (len > 0.001f) {
            tmp.scl(Math.min(len, moveSpeed * 1.15f * delta) / len);
            pos.add(tmp);
            walkTime += delta;
        }
        return true;
    }

    protected Bullet shoot(GameWorld world, float angle, float bulletSpeed) {
        return world.spawnEnemyBullet(pos.x + MathUtils.cosDeg(angle) * radius,
                pos.y + MathUtils.sinDeg(angle) * radius, angle, bulletSpeed);
    }

    /** Colour to draw a body part with - white while flashing from a hit. */
    protected Color c(Color base) {
        if (flash > 0) {
            return Color.WHITE;
        }
        if (chill <= 0 && frozen <= 0 && burning <= 0 && poisoned <= 0) {
            return base;
        }
        tintIndex = (tintIndex + 1) % tints.length;
        Color t = tints[tintIndex].set(base);
        if (frozen > 0) return t.lerp(ICE, 0.65f);
        if (burning > 0) return t.lerp(BURN_TINT, 0.35f);
        if (poisoned > 0) return t.lerp(POISON_TINT, 0.35f);
        return t.lerp(CHILL, 0.5f);
    }

    protected boolean facingRight() {
        return MathUtils.cosDeg(aimAngle) >= 0;
    }

    protected void drawEyes(SpriteBatch b, Draw d, float cx, float cy, float spacing, float size) {
        float face = facingRight() ? 1 : -1;
        float lookX = MathUtils.cosDeg(aimAngle) * size * 0.35f;
        float lookY = MathUtils.sinDeg(aimAngle) * size * 0.35f;
        for (int i = -1; i <= 1; i += 2) {
            float ex = cx + i * spacing + face * spacing * 0.5f;
            d.circle(b, ex, cy, size, c(EYE_WHITE));
            d.circle(b, ex + lookX, cy + lookY, size * 0.55f, PUPIL);
        }
    }

    protected void drawFeet(SpriteBatch b, Draw d, float cx, float bottom, float spread) {
        float step = MathUtils.sin(walkTime * 14f) * 0.02f;
        d.rect(b, cx - spread - 0.04f, bottom - 0.03f + step, 0.08f, 0.05f, FEET);
        d.rect(b, cx + spread - 0.04f, bottom - 0.03f - step, 0.08f, 0.05f, FEET);
    }

    public final void draw(SpriteBatch b, Draw d) {
        if (!alive) {
            drawDying(b, d);
            return;
        }
        if (isSpawning()) {
            float k = spawnTimer / SPAWN_TIME;
            d.ellipse(b, pos.x, feetY(), radius * 1.6f, radius * 0.6f, SUMMON, 0.25f + 0.25f * (1 - k));
            d.ring(b, pos.x, pos.y, radius * (0.5f + 2.5f * k), SUMMON, 1f - k);
            return;
        }
        if (castsShadow()) {
            d.ellipse(b, pos.x, feetY(), radius * 1.1f, radius * 0.35f, Color.BLACK, 0.35f);
        }
        drawBody(b, d);
        drawAilments(b, d);
        if (dazed) {
            float top = pos.y + radius + 0.18f;
            for (int i = 0; i < 3; i++) {
                float a = dazeClock * 360f + i * 120f;
                d.circle(b, pos.x + MathUtils.cosDeg(a) * radius * 0.8f, top + MathUtils.sinDeg(a) * 0.05f, 0.03f, DAZE);
            }
        }
    }

    /** A block of ice round a frozen enemy, flames on a burning one, bubbles off one eaten by acid. */
    private void drawAilments(SpriteBatch b, Draw d) {
        float size = Math.min(radius, 0.45f);
        if (frozen > 0) {
            float w = size * 2.6f;
            float h = size * 3.2f;
            d.rect(b, pos.x - w / 2f, feetY() - 0.02f, w, h, ICE, 0.4f);
            d.rect(b, pos.x - w / 2f + 0.03f, feetY() + h * 0.5f, 0.03f, h * 0.4f, Color.WHITE, 0.6f);
        }
        if (burning > 0) {
            for (int i = 0; i < 3; i++) {
                float fx = pos.x + (i - 1) * size * 0.7f;
                float flick = 0.5f + 0.5f * MathUtils.sin(walkTime * 9f + deathTime + i * 2.1f + burning * 11f);
                float h = 0.06f + 0.07f * flick;
                float fy = pos.y + size * 0.3f + (i % 2) * size * 0.4f + h * 0.5f;
                d.ellipse(b, fx, fy, 0.04f, h, FLAME, 0.85f);
                d.ellipse(b, fx, fy - h * 0.2f, 0.02f, h * 0.55f, FLAME_CORE, 0.9f);
            }
        }
        if (poisoned > 0) {
            for (int i = 0; i < 3; i++) {
                float t = (poisoned * 1.3f + i / 3f) % 1f;
                d.circle(b, pos.x + (i - 1) * size * 0.6f, pos.y + size * (1f - t) * 1.5f, 0.02f + 0.02f * (1f - t),
                        ACID_BUBBLE, 0.8f * t);
            }
        }
    }

    protected abstract void drawBody(SpriteBatch b, Draw d);
}
