package com.trush.game.objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.trush.game.save.Unlocks;
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
    /** A laser beam instead of bullets: every shot hits along a beam this long (GameWorld.fireBeam). */
    private float beamRange;
    private float beamWidth = 0.03f;
    /** Lobs eggs (they burst on landing) instead of firing bullets: hold the trigger to throw further, release to throw. */
    private boolean lobs;
    /** Seconds the trigger has been held for a lob; negative when not charging. */
    private float charge = -1f;
    /** Shots blow up where they stop: blast radius (0 = no blast), {@code blastDamage} to all in it. */
    private float blastRadius;
    private float blastDamage;
    private boolean rocket;
    private float bulletAccel;
    /** Bullets bounce off walls and cover this many times. */
    private int bounces;
    /** Bullets drawn as streaks this long (laser bolts). */
    private float streak;
    /** Gatling: the time between shots when the barrels are cold; they spin up to {@code fireInterval}. */
    private float coldInterval;
    private float spin;
    private float sinceShot;
    private int shotCount;
    /** The Flufful grows with what it kills: kills so far, and the level they bought (1 = fresh). */
    private int kills;
    private int level = 1;
    /** Kills it takes to reach level 2, 3, 4, 5. */
    public static final int[] LEVEL_KILLS = {20, 60, 120, 200};
    /** Boosts from finding this gun again (shown as "+1", "+2"... after the name). */
    private final Array<Upgrade> upgrades = new Array<>();

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
        sinceShot += delta;
        if (sinceShot > 0.25f) { // let go of the trigger and the barrels spin down
            spin = Math.max(0f, spin - delta * 1.5f);
        }
        muzzleFlash -= delta;
        if (reloading) {
            reloadTimer -= delta;
            if (reloadTimer <= 0) {
                reloading = false;
                mag = isInfinite() ? magSize : Math.min(magSize, ammo);
            }
        }
    }

    /** Bigger Clips: a bigger magazine, filled up. */
    public void boostMagazine(float factor) {
        magSize = Math.max(magSize + 1, Math.round(magSize * factor));
        mag = isInfinite() ? magSize : Math.min(magSize, Math.max(mag, Math.min(ammo, magSize)));
    }

    /** What finding a gun you already carry does to it: one stat goes up a bit, picked at random. */
    public enum Upgrade {
        DAMAGE("DAMAGE UP"), AMMO("BIGGER MAGAZINE"), SPEED("FASTER BULLETS");

        public final String text;

        Upgrade(String text) {
            this.text = text;
        }
    }

    /** A copy of this gun found again: a random small upgrade, and the gun is refilled. */
    public Upgrade upgrade() {
        Upgrade u = Upgrade.values()[MathUtils.random(Upgrade.values().length - 1)];
        apply(u);
        mag = magSize;
        ammo = maxAmmo;
        reloading = false;
        return u;
    }

    /** Applies an upgrade without refilling (saved runs put their upgrades back this way). */
    public void apply(Upgrade u) {
        switch (u) {
            case DAMAGE:
                damage *= 1.15f;
                blastDamage *= 1.15f;
                break;
            case AMMO:
                magSize = Math.max(magSize + 1, Math.round(magSize * 1.25f));
                if (!isInfinite()) {
                    maxAmmo = Math.round(maxAmmo * 1.25f);
                }
                break;
            case SPEED:
                bulletSpeed *= 1.15f;
                bulletLife /= 1.07f; // a little more range, not a lot
                beamRange *= 1.1f;
                if (lobs) {
                    blastRadius *= 1.1f; // eggs don't fly faster: they burst wider
                }
                break;
        }
        upgrades.add(u);
    }

    /** "Pocket Uzi", or "Pocket Uzi +2" once it has been upgraded twice; the Flufful shows its level. */
    public String displayName() {
        String n = level > 1 ? name + " Lv" + level : name;
        return upgrades.size == 0 ? n : n + " +" + upgrades.size;
    }

    /**
     * The Flufful scored a kill. Every level (at 20, 60, 120 and 200 kills) it hits harder, shoots
     * faster and its pom-poms get bigger. Returns true when this kill levelled it up.
     */
    public boolean addKill() {
        if (!isFlufful()) {
            return false;
        }
        kills++;
        if (level - 1 < LEVEL_KILLS.length && kills >= LEVEL_KILLS[level - 1]) {
            levelUp();
            return true;
        }
        return false;
    }

    private void levelUp() {
        level++;
        damage *= 1.15f;
        fireInterval *= 0.93f;
        bulletRadius += 0.008f;
        homing += 30f;
    }

    /** A saved Flufful: its kills and the levels they bought. */
    public void restoreKills(int savedKills) {
        while (level - 1 < LEVEL_KILLS.length && savedKills >= LEVEL_KILLS[level - 1]) {
            levelUp();
        }
        kills = savedKills;
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

    /** Fresh copies of every gun that can come out of a chest (everything but the starter; the Flufful once unlocked). */
    public static Array<Weapon> chestGuns() {
        Array<Weapon> guns = new Array<>(new Weapon[]{confettiPopper(), bubbleBlaster(), laserPointer(),
                fluffShotgun(), bananaRang(), bouncer(), plasmaZapper(),
                pocketUzi(), beeHive(), gullGatling(), eggLobber(),
                bigMagnum(), rainbowRay(), partyRocket()});
        if (Unlocks.flufful) {
            guns.add(flufful());
        }
        return guns;
    }

    /** True for the Flufful, the talking gun (GameWorld gives it a voice). */
    public boolean isFlufful() {
        return FLUFFUL.equals(name);
    }

    /** Creates a fresh gun from its name (saved runs store guns by name), or null if there's no such gun. */
    public static Weapon byName(String name) {
        if (rustySidearm().name.equals(name)) {
            return rustySidearm();
        }
        if (FLUFFUL.equals(name)) { // known even before it's unlocked, so a saved one always loads
            return flufful();
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

    /** Lobs: the shortest and the longest throw, and how long the trigger must be held for the longest. */
    public static final float LOB_MIN = 1.2f;
    public static final float LOB_MAX = 6f;
    public static final float FULL_CHARGE = 0.8f;

    /** This gun charges its shot while the trigger is held and fires on release (the Egg Lobber). */
    public boolean chargesShots() {
        return lobs;
    }

    /** Could shoot this instant (loaded, not reloading, not cooling down). */
    public boolean ready() {
        return !reloading && cooldown <= 0 && mag > 0;
    }

    /** The trigger is held: the throw gets longer, up to {@link #LOB_MAX} after {@link #FULL_CHARGE} s. */
    public void holdCharge(float delta) {
        charge = Math.min(FULL_CHARGE, Math.max(0f, charge) + delta);
    }

    public boolean isCharging() {
        return charge >= 0;
    }

    public void cancelCharge() {
        charge = -1f;
    }

    /** How far an egg thrown now would fly. */
    public float lobDistance() {
        return LOB_MIN + (LOB_MAX - LOB_MIN) * Math.max(0f, charge) / FULL_CHARGE;
    }

    public float reloadProgress() {
        return reloading ? 1f - reloadTimer / reloadTime : 1f;
    }

    /** Few shots left in the magazine and more to load: worth a "RELOAD" hint (guns reload on their own at 0). */
    public boolean wantsReload() {
        return !reloading && hasReserve() && mag <= Math.max(1, magSize / 5);
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

        float scale = world.getPlayer().damageScale();
        boolean ghost = world.getPlayer().hasItem(Item.GHOST_BULLETS);
        for (int i = 0; i < pellets; i++) {
            if (beamRange > 0) {
                world.fireBeam(x, y, angle, damage * scale, beamRange, beamWidth, bulletColor, piercing || ghost);
                continue;
            }
            if (lobs) {
                world.lobEgg(x, y, angle + MathUtils.random(-spreadDeg / 2f, spreadDeg / 2f), lobDistance(),
                        blastDamage * scale, blastRadius);
                continue;
            }
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
            Bullet b = new Bullet(true, x, y, a, speed, bulletRadius, damage * scale, bulletColor);
            b.life = bulletLife * (pellets > 1 ? MathUtils.random(0.8f, 1.1f) : 1f);
            b.piercing = piercing || ghost;
            b.bounces = bounces;
            b.blastRadius = blastRadius;
            b.blastDamage = blastDamage * scale;
            b.rocket = rocket;
            b.streak = streak;
            if (bulletAccel != 0) {
                b.accel = bulletAccel;
                b.maxSpeed = bulletSpeed * 3f;
            }
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
        // keep the part of a frame the shot came late by, so fast guns (and beams) really fire at their
        // rate: at 60 fps a 0.05 s gun used to wait 4 frames (0.067 s) and lost a quarter of its damage
        float interval = coldInterval > 0 ? MathUtils.lerp(coldInterval, fireInterval, spin) : fireInterval;
        cooldown = Math.max(cooldown, -0.02f) + interval;
        charge = -1f;
        spin = Math.min(1f, spin + 0.1f);
        sinceShot = 0;
        muzzleFlash = 0.05f;
        if (beamRange <= 0 || shotCount % 4 == 1) { // a beam hums, it doesn't rattle off a sound per tick
            world.sfx().play(sound, beamRange > 0 ? 0.35f : 0.6f);
        }
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
        w.damage = 2.2f;
        w.pellets = 12;
        w.spreadDeg = 42f;
        w.fireInterval = 0.5f;
        w.bulletSpeed = 7f;
        w.bulletLife = 0.45f;
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
        w.damage = 7f;
        w.fireInterval = 0.28f;
        w.bulletSpeed = 4.6f;
        w.bulletRadius = 0.09f;
        w.bulletLife = 1.8f;
        w.spreadDeg = 6f;
        w.wobble = 120f;
        w.bubbles = true;
        w.piercing = true;
        w.magSize = 14;
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

    // ------------------------------------------------------------------ lasers, launchers and toys

    /** D: a cat-toy laser pointer - a thin red beam that hits instantly along its whole length while held. */
    public static Weapon laserPointer() {
        Weapon w = new Weapon("Laser Pointer");
        w.tier = Tier.D;
        w.damage = 1.4f; // every 0.05 s: 28 damage/s, never misses what it touches
        w.fireInterval = 0.05f;
        w.beamRange = 4.5f;
        w.beamWidth = 0.03f;
        w.spreadDeg = 0f;
        w.magSize = 120;
        w.reloadTime = 1.2f;
        w.maxAmmo = 720;
        w.knockback = 0.05f;
        w.shake = 0f;
        w.sound = "zap";
        w.bulletColor.set(Color.valueOf("ff3a3a"));
        w.sprite = "laser_pointer";
        return w.fill();
    }

    /** C: rubber balls in party colours that bounce off walls and cover three times. */
    public static Weapon bouncer() {
        Weapon w = new Weapon("Super Bouncer");
        w.tier = Tier.C;
        w.damage = 6f;
        w.fireInterval = 0.3f;
        w.bulletSpeed = 6.5f;
        w.bulletRadius = 0.07f;
        w.bulletLife = 2.6f;
        w.spreadDeg = 5f;
        w.bounces = 3;
        w.magSize = 8;
        w.reloadTime = 1.1f;
        w.maxAmmo = 120;
        w.knockback = 0.9f;
        w.shake = 0.06f;
        w.sound = "pop";
        w.colorMode = ColorMode.CONFETTI;
        w.sprite = "bouncer";
        return w.fill();
    }

    /** C: a retro ray gun - fast green laser bolts. */
    public static Weapon plasmaZapper() {
        Weapon w = new Weapon("Plasma Zapper");
        w.tier = Tier.C;
        w.damage = 7f;
        w.fireInterval = 0.22f;
        w.bulletSpeed = 13f;
        w.bulletRadius = 0.035f;
        w.bulletLife = 0.9f;
        w.spreadDeg = 2f;
        w.streak = 0.32f;
        w.magSize = 10;
        w.reloadTime = 1.1f;
        w.maxAmmo = 150;
        w.knockback = 0.5f;
        w.shake = 0.06f;
        w.sound = "zap";
        w.bulletColor.set(Color.valueOf("5ae06a"));
        w.sprite = "plasma_zapper";
        return w.fill();
    }

    /** B: the gull's minigun (in the spirit of ETG's Gatling Gull) - slow to start, then a storm of feathers-white lead. */
    public static Weapon gullGatling() {
        Weapon w = new Weapon("Gull Gatling");
        w.tier = Tier.B;
        w.damage = 2f;
        w.coldInterval = 0.22f;
        w.fireInterval = 0.05f; // fully spun up: ~40 damage/s
        w.bulletSpeed = 9f;
        w.spreadDeg = 14f;
        w.bulletRadius = 0.04f;
        w.bulletLife = 1f;
        w.magSize = 150;
        w.reloadTime = 2f;
        w.maxAmmo = 750;
        w.knockback = 0.25f;
        w.shake = 0.05f;
        w.sound = "smg";
        w.bulletColor.set(Color.valueOf("f5f0e6"));
        w.sprite = "gull_gatling";
        return w.fill();
    }

    /** B: a grenade launcher that lobs eggs over cover to where you aim; they burst as they land. */
    public static Weapon eggLobber() {
        Weapon w = new Weapon("Egg Lobber");
        w.tier = Tier.B;
        w.lobs = true;
        w.blastDamage = 22f;
        w.blastRadius = 1f;
        w.fireInterval = 0.6f;
        w.spreadDeg = 4f;
        w.magSize = 4;
        w.reloadTime = 1.4f;
        w.maxAmmo = 48;
        w.shake = 0.1f;
        w.sound = "pop";
        w.sprite = "egg_lobber";
        return w.fill();
    }

    /** A: a rocket launcher - rockets speed up as they fly and blow up on whatever they hit. */
    public static Weapon partyRocket() {
        Weapon w = new Weapon("Party Rocket");
        w.tier = Tier.A;
        w.damage = 12f;
        w.rocket = true;
        w.blastDamage = 34f;
        w.blastRadius = 1.25f;
        w.bulletSpeed = 4f;
        w.bulletAccel = 10f;
        w.bulletRadius = 0.06f;
        w.bulletLife = 1.6f;
        w.spreadDeg = 1f;
        w.fireInterval = 0.9f;
        w.magSize = 3;
        w.reloadTime = 1.6f;
        w.maxAmmo = 36;
        w.knockback = 1f;
        w.shake = 0.35f;
        w.sound = "shotgun";
        w.bulletColor.set(Color.valueOf("d63a2f"));
        w.sprite = "party_rocket";
        return w.fill();
    }

    private static final String FLUFFUL = "Flufful";

    /**
     * B: the Flufful (Fluffy + Gundul), the owner's take on ETG's Gunther - a fluffy gun that talks while
     * you fight (objects/FluffulVoice). Soft pom-poms that home in a little, endless ammo, and it grows
     * stronger with every level of kills ({@link #addKill}). Not in any chest until it's unlocked
     * (save/Unlocks: beat the Fluffy King without a scratch).
     */
    public static Weapon flufful() {
        Weapon w = new Weapon(FLUFFUL);
        w.tier = Tier.B;
        w.damage = 5.5f;
        w.fireInterval = 0.16f; // ~34 damage/s, and it steers a bit
        w.bulletSpeed = 8f;
        w.bulletRadius = 0.06f;
        w.bulletLife = 1.1f;
        w.spreadDeg = 6f;
        w.homing = 90f;
        w.magSize = 24;
        w.reloadTime = 1.1f;
        w.maxAmmo = INFINITE; // a friend never runs dry
        w.knockback = 0.5f;
        w.shake = 0.06f;
        w.sound = "pop";
        w.bulletColor.set(Color.valueOf("ff9fc4"));
        w.sprite = "flufful";
        return w.fill();
    }
}
