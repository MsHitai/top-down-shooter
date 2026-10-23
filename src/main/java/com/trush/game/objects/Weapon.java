package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.trush.game.world.GameWorld;
import lombok.Getter;

/**
 * A gun. Stats are plain fields so new guns are just new factory methods at the bottom.
 * {@code ammo} is the total ammo including what's in the magazine; -1 means infinite.
 */
@Getter
public class Weapon {

    public static final int INFINITE = -1;

    private final String name;
    /** Quality: which chest it comes out of. */
    private Tier tier = Tier.D;
    private float damage = 5f;
    private float fireInterval = 0.25f;
    private float bulletSpeed = 7f;
    private float bulletRadius = 0.045f;
    private float bulletLife = 1.2f;
    private float spreadDeg = 4f;
    private int pellets = 1;
    private int magSize = 8;
    private float reloadTime = 1f;
    private int maxAmmo = INFINITE;
    private boolean piercing;
    private float knockback = 0.6f;
    private float shake = 0.12f;
    private String sound = "shoot";
    private final Color bulletColor = new Color(Color.YELLOW);
    /** Sprite in assets/sprites/guns.txt (see Sprites.gun). */
    private String sprite = "rusty_sidearm";
    /** How bullets are coloured: one colour, a new rainbow hue every shot, or random party colours. */
    public enum ColorMode { SOLID, RAINBOW, CONFETTI }
    private ColorMode colorMode = ColorMode.SOLID;
    private float curve;
    /** Bullets fly out, slow down and come back to the player like a boomerang (the banana). */
    private boolean boomerang;
    private float wobble;
    private float homing;
    private boolean bubbles;
    private int shotCount;

    private int mag;
    private int ammo;
    private float cooldown;
    private float reloadTimer;
    private boolean reloading;
    private float muzzleFlash;

    public Weapon(String name) {
        this.name = name;
    }

    /** Call after setting stats to fill the gun. */
    private Weapon fill() {
        mag = magSize;
        ammo = maxAmmo;
        return this;
    }

    public boolean isInfinite() {
        return maxAmmo == INFINITE;
    }

    private boolean hasReserve() {
        return isInfinite() || ammo > mag;
    }

    public void update(float delta) {
        cooldown -= delta;
        muzzleFlash -= delta;
        if (reloading) {
            reloadTimer -= delta;
            if (reloadTimer <= 0) {
                reloading = false;
                mag = isInfinite() ? magSize : Math.min(magSize, ammo);
            }
        }
    }

    /** Sets the ammo of a gun loaded from a saved run. */
    public void restoreAmmo(int mag, int ammo) {
        this.mag = Math.max(0, Math.min(magSize, mag));
        this.ammo = isInfinite() ? INFINITE : Math.max(this.mag, ammo);
        reloading = false;
    }

    private static final float BOOMERANG_DECEL = 9f;

    private static final Color[] CONFETTI = {
            Color.valueOf("ff5fa2"), Color.valueOf("ffd23a"), Color.valueOf("5ad1ff"),
            Color.valueOf("7bed6f"), Color.valueOf("c77dff"), Color.valueOf("ff8a3a"),
    };

    /** Fresh copies of every gun that can come out of a chest (everything but the starter). */
    public static Array<Weapon> chestGuns() {
        return new Array<>(new Weapon[]{confettiPopper(), bubbleBlaster(), fluffShotgun(), bananaRang(),
                pocketUzi(), beeHive(), bigMagnum(), rainbowRay()});
    }

    /** Creates a fresh gun from its name (saved runs store guns by name), or null if there's no such gun. */
    public static Weapon byName(String name) {
        if (rustySidearm().name.equals(name)) {
            return rustySidearm();
        }
        for (Weapon w : chestGuns()) {
            if (w.name.equals(name)) {
                return w;
            }
        }
        return null;
    }

    public void startReload(GameWorld world) {
        if (reloading || mag == magSize || !hasReserve()) {
            return;
        }
        reloading = true;
        reloadTimer = reloadTime;
        world.sfx().play("reload", 0.8f);
    }

    public void cancelReload() {
        reloading = false;
    }

    public float reloadProgress() {
        return reloading ? 1f - reloadTimer / reloadTime : 1f;
    }

    public boolean isEmpty() {
        return mag <= 0 && !hasReserve();
    }

    /** Tries to fire from (x, y) at {@code angle} degrees. Returns true if bullets came out. */
    public boolean tryFire(GameWorld world, float x, float y, float angle) {
        if (reloading || cooldown > 0) {
            return false;
        }
        if (mag <= 0) {
            if (hasReserve()) {
                startReload(world);
            } else {
                world.sfx().play("empty");
                cooldown = 0.35f;
            }
            return false;
        }

        for (int i = 0; i < pellets; i++) {
            float a;
            float speed = bulletSpeed;
            if (pellets > 1) {
                // spread pellets evenly across the cone, with a bit of jitter
                float t = pellets == 1 ? 0.5f : i / (float) (pellets - 1);
                a = angle - spreadDeg / 2f + spreadDeg * t + MathUtils.random(-3f, 3f);
                speed *= MathUtils.random(0.85f, 1.1f);
            } else {
                a = angle + MathUtils.random(-spreadDeg / 2f, spreadDeg / 2f);
            }
            Bullet b = new Bullet(true, x, y, a, speed, bulletRadius, damage, bulletColor);
            b.life = bulletLife * (pellets > 1 ? MathUtils.random(0.8f, 1.1f) : 1f);
            b.piercing = piercing;
            b.knockback = knockback;
            b.curve = curve;
            if (boomerang) { // out to ~v^2 / 2a, then back (GameWorld.updateBoomerang)
                b.boomerang = true;
                b.accel = -BOOMERANG_DECEL;
                b.minSpeed = 0.35f;
            }
            b.wobble = wobble;
            b.homing = homing;
            b.bubble = bubbles;
            if (colorMode == ColorMode.RAINBOW) {
                b.color.fromHsv((shotCount * 29f) % 360f, 0.75f, 1f);
            } else if (colorMode == ColorMode.CONFETTI) {
                b.color.set(CONFETTI[MathUtils.random(CONFETTI.length - 1)]);
            }
            world.addPlayerBullet(b);
        }

        shotCount++;
        mag--;
        if (!isInfinite()) {
            ammo--;
        }
        cooldown = fireInterval;
        muzzleFlash = 0.05f;
        world.sfx().play(sound, 0.6f);
        world.shake(shake);
        world.particles().spawn(x, y, 0, 0, 0.06f, 0.09f + 0.03f * pellets, Color.WHITE, 0);
        if (mag == 0 && hasReserve()) {
            startReload(world);
        }
        return true;
    }

    /** Refills ammo by a fraction of the max (ammo pickups). */
    public void addAmmo(float fraction) {
        if (isInfinite()) {
            return;
        }
        ammo = Math.min(maxAmmo, ammo + Math.max(1, Math.round(maxAmmo * fraction)));
    }

    public boolean isFullAmmo() {
        return isInfinite() || ammo >= maxAmmo;
    }

    public String ammoText() {
        if (isInfinite()) {
            return mag + "/INF";
        }
        return mag + "/" + (ammo - mag);
    }

    // ------------------------------------------------------------------ guns

    /** Starter gun: infinite ammo, reliable. */
    public static Weapon rustySidearm() {
        Weapon w = new Weapon("Rusty Sidearm");
        w.damage = 5f;
        w.fireInterval = 0.2f;
        w.bulletSpeed = 7.5f;
        w.spreadDeg = 5f;
        w.magSize = 10;
        w.reloadTime = 0.9f;
        w.maxAmmo = INFINITE;
        w.bulletColor.set(Color.valueOf("ffe066"));
        w.sprite = "rusty_sidearm";
        return w.fill();
    }

    public static Weapon fluffShotgun() {
        Weapon w = new Weapon("Fluff Shotgun");
        w.tier = Tier.C;
        w.damage = 3.5f;
        w.pellets = 6;
        w.spreadDeg = 34f;
        w.fireInterval = 0.65f;
        w.bulletSpeed = 6.5f;
        w.bulletLife = 0.55f;
        w.bulletRadius = 0.04f;
        w.magSize = 6;
        w.reloadTime = 1.3f;
        w.maxAmmo = 80;
        w.knockback = 1.2f;
        w.shake = 0.3f;
        w.sound = "shotgun";
        w.bulletColor.set(Color.valueOf("ff9f43"));
        w.sprite = "fluff_shotgun";
        return w.fill();
    }

    public static Weapon pocketUzi() {
        Weapon w = new Weapon("Pocket Uzi");
        w.tier = Tier.B;
        w.damage = 2.2f;
        w.fireInterval = 0.065f;
        w.bulletSpeed = 8.5f;
        w.spreadDeg = 12f;
        w.bulletRadius = 0.035f;
        w.magSize = 32;
        w.reloadTime = 1.2f;
        w.maxAmmo = 400;
        w.knockback = 0.2f;
        w.shake = 0.06f;
        w.sound = "smg";
        w.bulletColor.set(Color.valueOf("7bed9f"));
        w.sprite = "pocket_uzi";
        return w.fill();
    }

    public static Weapon bigMagnum() {
        Weapon w = new Weapon("Big Magnum");
        w.tier = Tier.A;
        w.damage = 16f;
        w.fireInterval = 0.5f;
        w.bulletSpeed = 11f;
        w.spreadDeg = 1f;
        w.bulletRadius = 0.06f;
        w.bulletLife = 1.5f;
        w.magSize = 6;
        w.reloadTime = 1.4f;
        w.maxAmmo = 72;
        w.piercing = true;
        w.knockback = 1.6f;
        w.shake = 0.4f;
        w.sound = "magnum";
        w.bulletColor.set(Color.valueOf("70d6ff"));
        w.sprite = "big_magnum";
        return w.fill();
    }

    // ------------------------------------------------------------------ weird guns

    /** D: a party popper - a burst of tiny pellets in random bright colours, short range. */
    public static Weapon confettiPopper() {
        Weapon w = new Weapon("Confetti Popper");
        w.tier = Tier.D;
        w.damage = 1.6f;
        w.pellets = 12;
        w.spreadDeg = 50f;
        w.fireInterval = 0.55f;
        w.bulletSpeed = 6f;
        w.bulletLife = 0.4f;
        w.bulletRadius = 0.03f;
        w.magSize = 4;
        w.reloadTime = 1f;
        w.maxAmmo = 60;
        w.knockback = 0.45f;
        w.shake = 0.15f;
        w.sound = "pop";
        w.colorMode = ColorMode.CONFETTI;
        w.sprite = "confetti_popper";
        return w.fill();
    }

    /** D: big slow wobbly bubbles that go through enemies and shove them hard (nice near pits). */
    public static Weapon bubbleBlaster() {
        Weapon w = new Weapon("Bubble Blaster");
        w.tier = Tier.D;
        w.damage = 4f;
        w.fireInterval = 0.32f;
        w.bulletSpeed = 3.2f;
        w.bulletRadius = 0.09f;
        w.bulletLife = 2.2f;
        w.spreadDeg = 6f;
        w.wobble = 120f;
        w.bubbles = true;
        w.piercing = true;
        w.magSize = 12;
        w.reloadTime = 1.1f;
        w.maxAmmo = 120;
        w.knockback = 1.4f;
        w.shake = 0.04f;
        w.sound = "pop";
        w.bulletColor.set(Color.valueOf("8ed8ff"));
        w.sprite = "bubble_blaster";
        return w.fill();
    }

    /**
     * C: a boomerang - flies straight at the aim, slows to a stop about 2.7 units out, then comes back to
     * the player, hitting everything on the way out and again on the way back.
     */
    public static Weapon bananaRang() {
        Weapon w = new Weapon("Banana-rang");
        w.tier = Tier.C;
        w.damage = 9f;
        w.fireInterval = 0.45f;
        w.bulletSpeed = 7f;
        w.bulletRadius = 0.08f;
        w.bulletLife = 3f;
        w.spreadDeg = 0f;
        w.boomerang = true;
        w.piercing = true;
        w.magSize = 4;
        w.reloadTime = 1f;
        w.maxAmmo = 60;
        w.knockback = 0.8f;
        w.shake = 0.08f;
        w.sound = "dodge";
        w.bulletColor.set(Color.valueOf("f2d23a"));
        w.sprite = "banana_rang";
        return w.fill();
    }

    /** B: three bees per shot that wiggle and home in on the nearest enemy. */
    public static Weapon beeHive() {
        Weapon w = new Weapon("Bee Hive");
        w.tier = Tier.B;
        w.damage = 3f;
        w.pellets = 3;
        w.spreadDeg = 40f;
        w.fireInterval = 0.3f;
        w.bulletSpeed = 4.2f;
        w.bulletRadius = 0.04f;
        w.bulletLife = 2.5f;
        w.wobble = 200f;
        w.homing = 280f;
        w.magSize = 15;
        w.reloadTime = 1.3f;
        w.maxAmmo = 150;
        w.knockback = 0.3f;
        w.shake = 0.04f;
        w.sound = "buzz";
        w.bulletColor.set(Color.valueOf("f2c230"));
        w.sprite = "bee_hive";
        return w.fill();
    }

    /** A: a super fast stream of piercing bullets, every one a new colour of the rainbow. */
    public static Weapon rainbowRay() {
        Weapon w = new Weapon("Rainbow Ray");
        w.tier = Tier.A;
        w.damage = 2.8f; // ~43 damage/s, piercing (3.2 / 0.06 s, ~53/s, melted bosses; 2.4 ran dry on them)
        w.fireInterval = 0.065f;
        w.bulletSpeed = 12f;
        w.bulletRadius = 0.04f;
        w.bulletLife = 0.9f;
        w.spreadDeg = 2f;
        w.piercing = true;
        w.magSize = 40;
        w.reloadTime = 1.5f;
        w.maxAmmo = 480; // a full supply (~1340 damage) can finish any boss, even missing a third of the shots
        w.knockback = 0.25f;
        w.shake = 0.03f;
        w.sound = "zap";
        w.colorMode = ColorMode.RAINBOW;
        w.sprite = "rainbow_ray";
        return w.fill();
    }
}
