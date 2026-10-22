package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
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
    private final Color gunColor = new Color(Color.GRAY);
    private float gunLength = 0.2f;
    private float gunThickness = 0.06f;

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
            world.addPlayerBullet(b);
        }

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
        w.gunColor.set(Color.valueOf("8a7a6a"));
        w.gunLength = 0.18f;
        return w.fill();
    }

    public static Weapon fluffShotgun() {
        Weapon w = new Weapon("Fluff Shotgun");
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
        w.gunColor.set(Color.valueOf("6b4a2f"));
        w.gunLength = 0.27f;
        w.gunThickness = 0.07f;
        return w.fill();
    }

    public static Weapon pocketUzi() {
        Weapon w = new Weapon("Pocket Uzi");
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
        w.gunColor.set(Color.valueOf("3d3d4a"));
        w.gunLength = 0.2f;
        return w.fill();
    }

    public static Weapon bigMagnum() {
        Weapon w = new Weapon("Big Magnum");
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
        w.gunColor.set(Color.valueOf("c0c4cc"));
        w.gunLength = 0.3f;
        w.gunThickness = 0.07f;
        return w.fill();
    }
}
