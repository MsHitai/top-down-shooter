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
    /** Shows a red aiming line while held (the Long Shot). */
    private boolean laserSight;
    /** How its bullets look (see Bullet.Shape). */
    private Bullet.Shape shape = Bullet.Shape.BALL;
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
    /** The blast only comes when a shot hits something (see Bullet.impactOnly). */
    private boolean impactOnly;
    private boolean rocket;
    private float bulletAccel;
    /** Bullets bounce off walls and cover this many times. */
    private int bounces;
    /** Bullets drawn as streaks this long (laser bolts). */
    private float streak;
    /** Gatling: the time between shots when the barrels are cold; they spin up to {@code fireInterval}. */
    private float coldInterval;
    /** Bullet extras for the guns that have them (see Bullet): feathers, snow, sparks that jump, a vortex. */
    private int splits;
    private float chill;
    /** Frost each bullet adds to an enemy (1 = frozen solid; see Enemy.addFrost). */
    private float frost;
    /** Shoots water (or snow): puts out the burning drum in the city that hides the way down to the Slums. */
    private boolean wet;
    private int chain;
    private float vortex;
    /** Paper Planes: bullets turn after the aim this many degrees a second (Bullet.guided). */
    private float guided;
    /** Tennis balls: bounce off enemies as well as walls, faster each time (Bullet.rebounds, bounceBoost). */
    private boolean rebounds;
    private float bounceBoost;
    /** Crayon Box: every bullet is a random crayon - red burns, blue chills, green poisons. */
    private boolean crayons;
    /** The Pinwheel: bullets slow right down to a stop (and spin there) instead of keeping some speed. */
    private boolean stops;
    /** Socks let out the hero's stink where they land (Bullet.stinks). */
    private boolean stinks;
    /** Sets what it hits on fire / eats at it with acid for this long; leaves a puddle where it stops. */
    private float burn;
    private float acid;
    private Puddle.Kind puddle;
    private float puddleRadius;
    /** Also fires this many bullets evenly all the way round (the Porcupine). */
    private int ringShots;
    /** A melee swing this far instead of a shot (the Frying Pan): hits what's close, bats bullets back. */
    private float melee;
    /** A camera flash instead of a shot (the Polaroid): wipes out bullets and hurts what's in the picture. */
    private boolean flash;
    /** A shout instead of a shot (the Megaphone): stuns what's in front of you and slows enemy bullets down. */
    private boolean shout;
    /** Pulled back like a slingshot: hold to pull, let go to shoot - the longer the pull, the harder and faster. */
    private boolean slings;
    /** Every this many shots one goes BANG for {@link #BANG} x the damage (the Cap Gun); 0 = never. */
    private int bangEvery;
    /** Each pull of the trigger fires this many bullets one after another, {@code burstGap} s apart (the Pea Shooter). */
    private int burst = 1;
    private float burstGap;
    /** What {@code splits} burst into, and how hard each piece hits (x the bullet's damage). */
    private Bullet.Shape splitShape = Bullet.Shape.FEATHER;
    private float splitDamage = 0.35f;
    /** Hot air: shoves smoke and stink clouds away (the Hair Dryer). */
    private boolean blowsGas;
    /** Plays its sound only every this many shots (a fast stream hums instead of rattling). */
    private int soundEvery = 1;
    /** Darts stick in what they hit; the {@code GameWorld.DARTS_TO_POP}th one pops them all (Sticky Darts). */
    private boolean sticky;
    private float spin;
    private float sinceShot;
    private int shotCount;
    /** The Flufful grows with what it kills: kills so far, and the level they bought (1 = fresh). */
    private int kills;
    private int level = 1;
    /** Kills it takes to reach level 2, 3, 4, 5. */
    public static final int[] LEVEL_KILLS = {20, 60, 120, 200};
    /** The Heirloom grows from every kill made while it's carried (any gun); kills for levels 2-5. */
    private boolean heirloom;
    /** Kept for good (save/Unlocks#heirloomKills), so it grows slower than the Flufful, over many runs. */
    public static final int[] HEIRLOOM_KILLS = {40, 120, 260, 480};
    /** The Heirloom's stats per level (index 0 = level 1): weak and plain at first, a killing machine at the end. */
    private static final float[] HEIR_DAMAGE = {4f, 5f, 5.5f, 4f, 3.6f};
    private static final float[] HEIR_INTERVAL = {0.25f, 0.2f, 0.18f, 0.16f, 0.14f};
    private static final int[] HEIR_PELLETS = {1, 1, 1, 2, 3};
    private static final float[] HEIR_SPREAD = {4f, 4f, 3f, 8f, 12f};
    private static final int[] HEIR_MAG = {8, 12, 16, 24, 32};
    private static final float[] HEIR_SPEED = {8f, 9f, 10f, 11f, 12f};
    /** Boosts from finding this gun again (shown as "+1", "+2"... after the name). */
    private final Array<Upgrade> upgrades = new Array<>();

    private int mag;
    private int ammo;
    private float cooldown;
    private float reloadTimer;
    /** What reloadTimer started from (Quick Mittens make it shorter than reloadTime). */
    private float reloadTotal = 1f;
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

    /** Grows with kills: the Flufful (kills made with it) and the Heirloom (kills made while carrying it). */
    public boolean growsWithKills() {
        return isFlufful() || heirloom;
    }

    private int[] levelKills() {
        return heirloom ? HEIRLOOM_KILLS : LEVEL_KILLS;
    }

    /** One kill for it (see {@link #addKill(int)}). */
    public boolean addKill() {
        return addKill(1);
    }

    /**
     * Kills for a gun that grows with them. The Flufful (at 20, 60, 120 and 200) hits harder, shoots
     * faster and its pom-poms get bigger; the Heirloom (40, 120, 260, 480, kept between runs) turns from a plain old pistol
     * into a piercing, homing stream. Returns true when these kills levelled it up.
     */
    public boolean addKill(int n) {
        if (!growsWithKills()) {
            return false;
        }
        kills += n;
        boolean up = false;
        int[] at = levelKills();
        while (level - 1 < at.length && kills >= at[level - 1]) {
            levelUp();
            up = true;
        }
        return up;
    }

    private void levelUp() {
        if (heirloom) {
            heirloomLevelUp();
            return;
        }
        level++;
        damage *= 1.15f;
        fireInterval *= 0.93f;
        bulletRadius += 0.012f;
        homing += 30f;
    }

    /**
     * The Heirloom's next level. Its stats go up by the ratio between levels, so upgrades (grandad's +1,
     * finding it again) and Bigger Clips stay on top.
     */
    private void heirloomLevelUp() {
        int was = level - 1;
        level++;
        int now = level - 1;
        damage *= HEIR_DAMAGE[now] / HEIR_DAMAGE[was];
        fireInterval *= HEIR_INTERVAL[now] / HEIR_INTERVAL[was];
        bulletSpeed *= HEIR_SPEED[now] / HEIR_SPEED[was];
        magSize = Math.round(magSize * HEIR_MAG[now] / (float) HEIR_MAG[was]);
        pellets = HEIR_PELLETS[now];
        spreadDeg = HEIR_SPREAD[now];
        piercing = level >= 3;
        homing = level >= 5 ? 60f : 0f;
        bulletRadius += 0.006f;
        shake += 0.02f;
        mag = magSize;
        reloading = false;
        sprite = "heirloom_" + level;
        if (level >= 4) {
            bulletColor.set(Color.valueOf(level >= 5 ? "ffd23a" : "e8f0ff"));
        }
    }

    /** A saved gun that grows with kills (the Flufful, the Heirloom): its kills and the levels they bought. */
    public void restoreKills(int savedKills) {
        int[] at = levelKills();
        while (level - 1 < at.length && savedKills >= at[level - 1]) {
            levelUp();
        }
        kills = Math.max(kills, savedKills);
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
                waterPistol(), snowballSlinger(), slingshot(), popcornPopper(), hairDryer(), capGun(), peaShooter(),
                fluffShotgun(), bananaRang(), bouncer(), plasmaZapper(), pillowCannon(), jumperCables(),
                acidSquirter(), flareGun(), freezeRay(), stickyDarts(), kazoo(), megaphone(),
                paperPlanes(), tennisBalls(), crayonBox(), pinwheel(), sockLauncher(),
                pocketUzi(), beeHive(), gullGatling(), eggLobber(), fryingPan(), porcupine(), gloopLauncher(),
                boomBuss(),
                heirloom(), bottleRocketRack(), fluffCannon(), unicornHorn(), shootingStar(), rubberDuckLauncher(),
                bigMagnum(), rainbowRay(), partyRocket(), longShot(), dustBunny(), polaroid(), chomper()});
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
        if (THROWING_STARS.equals(name)) {
            return throwingStars();
        }
        if (BABY_RATTLE.equals(name)) {
            return babyRattle();
        }
        if (ROYAL_SCEPTRE.equals(name)) {
            return royalSceptre();
        }
        if (PEARL_REVOLVER.equals(name)) {
            return pearlRevolver();
        }
        if (DOUBLE_BARREL.equals(name)) {
            return doubleBarrel();
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
        reloadTotal = reloadTime * world.getPlayer().reloadScale();
        reloadTimer = reloadTotal;
        world.sfx().play("reload", 0.8f);
        world.onReload();
    }

    public void cancelReload() {
        reloading = false;
    }

    /** Lobs: the shortest and the longest throw, and how long the trigger must be held for the longest. */
    public static final float LOB_MIN = 1.2f;
    public static final float LOB_MAX = 6f;
    public static final float FULL_CHARGE = 0.8f;

    /** This gun charges its shot while the trigger is held and fires on release (the Egg Lobber, the Slingshot). */
    public boolean chargesShots() {
        return lobs || slings;
    }

    /** A Cap Gun's bang does this many times the damage of a click. */
    public static final float BANG = 5f;

    /** How far a slingshot is pulled back, 0-1. */
    public float pull() {
        return Math.max(0f, charge) / FULL_CHARGE;
    }

    /** The next shot is a Cap Gun's bang. */
    private boolean bangNext() {
        return bangEvery > 0 && shotCount % bangEvery == bangEvery - 1;
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
        return reloading ? 1f - reloadTimer / reloadTotal : 1f;
    }

    /** Few shots left in the magazine and more to load: worth a "RELOAD" hint (guns reload on their own at 0). */
    public boolean wantsReload() {
        return magSize > 1 && !reloading && hasReserve() && mag <= Math.max(1, magSize / 5);
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
        float aim = world.getPlayer().spreadScale(); // the Toy Telescope steadies single shots
        // the Triple Scoop: every bullet comes with two more beside it, at half damage
        int scoops = world.getPlayer().hasItem(Item.TRIPLE_SCOOP) ? 3 : 1;
        for (int i = 0; i < pellets; i++) {
            if (beamRange > 0) {
                if (colorMode == ColorMode.RAINBOW) { // the Unicorn Horn's beam runs through the rainbow
                    bulletColor.fromHsv((shotCount * 12f) % 360f, 0.7f, 1f);
                }
                world.fireBeam(x, y, angle, damage * scale, beamRange, beamWidth, bulletColor, piercing || ghost, frost);
                continue;
            }
            if (lobs) {
                world.lobEgg(x, y, angle + MathUtils.random(-spreadDeg / 2f, spreadDeg / 2f) * aim, lobDistance(),
                        blastDamage * scale, blastRadius);
                continue;
            }
            if (melee > 0) {
                world.swingPan(angle, damage * scale, melee);
                continue;
            }
            if (flash) {
                world.cameraFlash(x, y, angle, damage * scale);
                continue;
            }
            if (shout) {
                world.shout(x, y, angle, damage * scale, bulletColor);
                continue;
            }
            if (slings) { // a quick flick or a full pull: harder and faster the further it's pulled
                float k = pull();
                Bullet pebble = makeBullet(x, y, angle + MathUtils.random(-spreadDeg / 2f, spreadDeg / 2f) * aim,
                        bulletSpeed * (0.55f + 0.45f * k), scale, ghost, false);
                pebble.damage *= 0.35f + 0.65f * k;
                pebble.life /= 0.55f + 0.45f * k; // (the same reach, slow or fast)
                world.addPlayerBullet(pebble);
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
                a = angle + MathUtils.random(-spreadDeg / 2f, spreadDeg / 2f) * aim;
            }
            boolean bang = bangNext();
            for (int k = 0; k < burst; k++) {
                for (int sc = 0; sc < scoops; sc++) {
                    Bullet bl = makeBullet(x, y, a + (sc == 0 ? 0f : sc == 1 ? SCOOP_ANGLE : -SCOOP_ANGLE), speed, scale,
                            ghost, pellets > 1);
                    bl.delay = k * burstGap; // a burst: one after another down the same line
                    if (sc > 0) {
                        bl.damage *= 0.5f;
                        bl.blastDamage *= 0.5f;
                    }
                    if (bang) {
                        bl.damage *= BANG;
                        bl.radius *= 1.8f;
                        bl.knockback *= 2.5f;
                    }
                    world.addPlayerBullet(bl);
                }
            }
        }
        for (int i = 0; i < ringShots; i++) {
            world.addPlayerBullet(makeBullet(x, y, angle + 180f / ringShots + 360f * i / ringShots, bulletSpeed, scale,
                    ghost, true));
        }

        shotCount++;
        if (melee <= 0) { // a frying pan never runs dry
            mag--;
        }
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
        boolean banged = bangEvery > 0 && shotCount % bangEvery == 0;
        if (banged) {
            world.sfx().play("magnum", 0.9f);
        } else if (beamRange > 0 ? shotCount % 4 == 1 : shotCount % soundEvery == 0) { // a beam hums, it doesn't rattle off a sound per tick
            world.sfx().play(sound, beamRange > 0 ? 0.35f : 0.6f);
        }
        world.shake(banged ? shake * 4f : shake);
        world.particles().spawn(x, y, 0, 0, 0.06f, 0.09f + 0.03f * pellets, Color.WHITE, 0);
        if (mag == 0 && hasReserve()) {
            startReload(world);
        }
        return true;
    }

    /** The Crayon Box's red, blue and green, and what each does: burns, chills, poisons (seconds). */
    private static final Color[] CRAYONS = {Color.valueOf("e8443a"), Color.valueOf("3a7ae8"), Color.valueOf("4ac850")};
    private static final float CRAYON_BURN = 2.5f;
    private static final float CRAYON_CHILL = 1.5f;
    private static final float CRAYON_ACID = 2.5f;

    /** How far the Triple Scoop's extra bullets fly off to the sides, in degrees. */
    private static final float SCOOP_ANGLE = 11f;

    /** One bullet of this gun, with all its extras. */
    private Bullet makeBullet(float x, float y, float a, float speed, float scale, boolean ghost, boolean jitter) {
        Bullet b = new Bullet(true, x, y, a, speed, bulletRadius, damage * scale, bulletColor);
        b.life = bulletLife * (jitter ? MathUtils.random(0.8f, 1.1f) : 1f);
        b.piercing = piercing || ghost;
        b.bounces = bounces;
        b.blastRadius = blastRadius;
        b.blastDamage = blastDamage * scale;
        b.impactOnly = impactOnly;
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
        b.shape = shape;
        b.splits = splits;
        b.splitShape = splitShape;
        b.splitDamage = splitDamage;
        b.blowsGas = blowsGas;
        b.sticky = sticky;
        b.chill = chill;
        b.frost = frost;
        b.wet = wet;
        b.chain = chain;
        b.vortex = vortex;
        b.burn = burn;
        b.acid = acid;
        b.puddle = puddle;
        b.puddleRadius = puddleRadius;
        b.guided = guided;
        b.rebounds = rebounds;
        b.bounceBoost = bounceBoost;
        b.stinks = stinks;
        if (stops) {
            b.minSpeed = 0f;
        }
        if (crayons) { // whichever crayon comes out of the box
            int pick = MathUtils.random(2);
            b.color.set(CRAYONS[pick]);
            if (pick == 0) b.burn = CRAYON_BURN;
            else if (pick == 1) b.chill = CRAYON_CHILL;
            else b.acid = CRAYON_ACID;
        }
        if (colorMode == ColorMode.RAINBOW) {
            b.color.fromHsv((shotCount * 29f) % 360f, 0.75f, 1f);
        } else if (colorMode == ColorMode.CONFETTI) {
            b.color.set(CONFETTI[MathUtils.random(CONFETTI.length - 1)]);
        }
        return b;
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
        w.reloadTime = 1.2f;
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

    /**
     * A: the Long Shot, a sniper rifle - one huge, very fast shot that goes through everything in a
     * line, a small magazine and a slow bolt, and a red laser sight while you hold it.
     */
    public static Weapon longShot() {
        Weapon w = new Weapon("Long Shot");
        w.tier = Tier.A;
        w.damage = 45f;
        w.fireInterval = 0.9f;
        w.bulletSpeed = 18f;
        w.spreadDeg = 0f;
        w.bulletRadius = 0.045f;
        w.bulletLife = 1.0f;
        w.streak = 0.45f;
        w.magSize = 4;
        w.reloadTime = 1.5f;
        w.maxAmmo = 36;
        w.piercing = true;
        w.knockback = 2f;
        w.shake = 0.35f;
        w.sound = "magnum";
        w.bulletColor.set(Color.valueOf("fff3b0"));
        w.laserSight = true;
        w.sprite = "long_shot";
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
        w.wet = true;
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
        w.damage = 13f;
        w.fireInterval = 0.4f;
        w.bulletSpeed = 7f;
        w.bulletRadius = 0.08f;
        w.bulletLife = 3f;
        w.spreadDeg = 0f;
        w.boomerang = true;
        w.piercing = true;
        w.magSize = 5;
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

    /**
     * C: the Freeze Ray - a cold blue beam that hardly hurts but piles ice on whatever it touches until it
     * freezes solid (then hit it hard: frozen enemies shatter). Bosses only slow down. Where it meets the
     * floor it leaves slippery ice.
     */
    public static Weapon freezeRay() {
        Weapon w = new Weapon("Freeze Ray");
        w.tier = Tier.C;
        w.damage = 0.9f; // every 0.05 s: 18 damage/s - the ice is the point
        w.frost = 0.05f; // a second of beam freezes most things
        w.fireInterval = 0.05f;
        w.beamRange = 4f;
        w.beamWidth = 0.05f;
        w.spreadDeg = 0f;
        w.magSize = 100;
        w.reloadTime = 1.3f;
        w.maxAmmo = 600;
        w.knockback = 0f;
        w.shake = 0f;
        w.sound = "hiss";
        w.bulletColor.set(Color.valueOf("8fd8ff"));
        w.sprite = "freeze_ray";
        return w.fill();
    }

    /** C: rubber balls in party colours that bounce off walls and cover three times. */
    public static Weapon bouncer() {
        Weapon w = new Weapon("Super Bouncer");
        w.tier = Tier.C;
        w.damage = 8.5f;
        w.fireInterval = 0.3f;
        w.bulletSpeed = 6.5f;
        w.bulletRadius = 0.07f;
        w.bulletLife = 2.6f;
        w.spreadDeg = 5f;
        w.bounces = 3;
        w.magSize = 10;
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
        w.damage = 2.4f;
        w.coldInterval = 0.18f;
        w.fireInterval = 0.05f; // fully spun up: ~48 damage/s
        w.bulletSpeed = 9f;
        w.spreadDeg = 14f;
        w.bulletRadius = 0.04f;
        w.bulletLife = 1f;
        w.magSize = 150;
        w.reloadTime = 2f;
        w.maxAmmo = 900; // like ETG's: a gatling should hardly ever run dry
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

    // ------------------------------------------------------------------ 0.8 guns

    /** D: a water pistol (after ETG's Mega Douser) - a fast stream of droplets, short range, shoves hard. */
    public static Weapon waterPistol() {
        Weapon w = new Weapon("Water Pistol");
        w.tier = Tier.D;
        w.damage = 2.2f;
        w.fireInterval = 0.08f;
        w.bulletSpeed = 6.5f;
        w.bulletRadius = 0.04f;
        w.bulletLife = 0.42f;
        w.spreadDeg = 7f;
        w.shape = Bullet.Shape.DROP;
        w.wet = true;
        w.magSize = 30;
        w.reloadTime = 1.2f;
        w.maxAmmo = 300;
        w.knockback = 1.1f;
        w.shake = 0.03f;
        w.sound = "pop";
        w.bulletColor.set(Color.valueOf("5ab8f0"));
        w.sprite = "water_pistol";
        return w.fill();
    }

    /**
     * D: snowballs (after ETG's freeze guns) - whatever they hit slows right down for a moment, and enough
     * of them in a row freeze it solid (it's a weak gun: five or so).
     */
    public static Weapon snowballSlinger() {
        Weapon w = new Weapon("Snowball Slinger");
        w.tier = Tier.D;
        w.damage = 6f;
        w.fireInterval = 0.32f;
        w.bulletSpeed = 6f;
        w.bulletRadius = 0.06f;
        w.bulletLife = 1.1f;
        w.spreadDeg = 4f;
        w.chill = 1.6f;
        w.frost = 0.2f;
        w.wet = true;
        w.magSize = 10;
        w.reloadTime = 1.1f;
        w.maxAmmo = 120;
        w.knockback = 0.5f;
        w.shake = 0.06f;
        w.sound = "pop";
        w.bulletColor.set(Color.valueOf("dff4ff"));
        w.sprite = "snowball_slinger";
        return w.fill();
    }

    /** C: a pillow cannon (after ETG's splitting shots) - a slow pillow that bursts into feathers on whatever it hits. */
    public static Weapon pillowCannon() {
        Weapon w = new Weapon("Pillow Cannon");
        w.tier = Tier.C;
        w.damage = 12f;
        w.fireInterval = 0.5f;
        w.bulletSpeed = 5f;
        w.bulletRadius = 0.09f;
        w.bulletLife = 1.2f;
        w.spreadDeg = 2f;
        w.shape = Bullet.Shape.PILLOW;
        w.splits = 8;
        w.magSize = 7;
        w.reloadTime = 1.3f;
        w.maxAmmo = 70;
        w.knockback = 1.2f;
        w.shake = 0.15f;
        w.sound = "pop";
        w.bulletColor.set(Color.valueOf("f0e6ff"));
        w.sprite = "pillow_cannon";
        return w.fill();
    }

    /** C: jumper cables (after ETG's chain lightning) - a spark that jumps from enemy to enemy. */
    public static Weapon jumperCables() {
        Weapon w = new Weapon("Jumper Cables");
        w.tier = Tier.C;
        w.damage = 8.5f;
        w.fireInterval = 0.32f;
        w.bulletSpeed = 10f;
        w.bulletRadius = 0.035f;
        w.bulletLife = 0.8f;
        w.spreadDeg = 3f;
        w.streak = 0.25f;
        w.chain = 3;
        w.magSize = 12;
        w.reloadTime = 1.2f;
        w.maxAmmo = 140;
        w.knockback = 0.3f;
        w.shake = 0.06f;
        w.sound = "zap";
        w.bulletColor.set(Color.valueOf("ffe94a"));
        w.sprite = "jumper_cables";
        return w.fill();
    }

    /** B: a frying pan (after ETG's Casey) - a swing that whacks what's close and bats enemy bullets back. */
    public static Weapon fryingPan() {
        Weapon w = new Weapon("Frying Pan");
        w.tier = Tier.B;
        w.damage = 14f;
        w.melee = 1.0f;
        w.fireInterval = 0.42f;
        w.magSize = 1;
        w.maxAmmo = INFINITE;
        w.shake = 0.12f;
        w.sound = "swish";
        w.bulletColor.set(Color.valueOf("ff9f43")); // the bullets it bats back
        w.sprite = "frying_pan";
        return w.fill();
    }

    /** B: a porcupine that spits quills all around you, with a few more where you aim. */
    public static Weapon porcupine() {
        Weapon w = new Weapon("Porcupine");
        w.tier = Tier.B;
        w.damage = 4f;
        w.pellets = 3;
        w.spreadDeg = 14f;
        w.ringShots = 10;
        w.fireInterval = 0.42f;
        w.bulletSpeed = 6.5f;
        w.bulletRadius = 0.03f;
        w.bulletLife = 0.55f;
        w.streak = 0.14f;
        w.magSize = 10;
        w.reloadTime = 1.3f;
        w.maxAmmo = 120;
        w.knockback = 0.5f;
        w.shake = 0.1f;
        w.sound = "shotgun";
        w.bulletColor.set(Color.valueOf("e8d8b0"));
        w.sprite = "porcupine";
        return w.fill();
    }

    /** A: a dust bunny (after ETG's Black Hole Gun) - drifts out slowly, sucks enemies in, chews on them and bursts. */
    public static Weapon dustBunny() {
        Weapon w = new Weapon("Dust Bunny");
        w.tier = Tier.A;
        w.damage = 6f; // each bite, four a second
        w.vortex = 1.7f;
        w.blastDamage = 26f;
        w.blastRadius = 1.1f;
        w.bulletSpeed = 3.2f;
        w.bulletAccel = -1.6f;
        w.bulletRadius = 0.13f;
        w.bulletLife = 2.4f;
        w.spreadDeg = 0f;
        w.piercing = true;
        w.fireInterval = 1.1f;
        w.magSize = 2;
        w.reloadTime = 1.6f;
        w.maxAmmo = 24;
        w.knockback = 0f;
        w.shake = 0.12f;
        w.sound = "swish";
        w.shape = Bullet.Shape.DUST;
        w.bulletColor.set(Color.valueOf("9a94a0"));
        w.sprite = "dust_bunny";
        return w.fill();
    }

    /** A: an instant camera (after ETG's Camera) - its flash wipes out bullets in front of you and hurts all it catches. */
    public static Weapon polaroid() {
        Weapon w = new Weapon("Polaroid");
        w.tier = Tier.A;
        w.damage = 28f;
        w.flash = true;
        w.fireInterval = 0.9f;
        w.magSize = 1;
        w.reloadTime = 1.1f;
        w.maxAmmo = 14;
        w.shake = 0.2f;
        w.sound = "blank";
        w.bulletColor.set(Color.WHITE);
        w.sprite = "polaroid";
        return w.fill();
    }

    private static final String FLUFFUL = "Flufful";

    // ------------------------------------------------------------------ fire and acid

    /** C: an acid squirter (after ETG's poison guns) - a short green stream; the acid keeps eating at what it hits. */
    public static Weapon acidSquirter() {
        Weapon w = new Weapon("Acid Squirter");
        w.tier = Tier.C;
        w.damage = 2.4f;
        w.fireInterval = 0.09f;
        w.bulletSpeed = 6f;
        w.bulletRadius = 0.04f;
        w.bulletLife = 0.5f;
        w.spreadDeg = 8f;
        w.shape = Bullet.Shape.DROP;
        w.acid = 2.5f;
        w.magSize = 40;
        w.reloadTime = 1.3f;
        w.maxAmmo = 360;
        w.knockback = 0.3f;
        w.shake = 0.03f;
        w.sound = "pop";
        w.bulletColor.set(Color.valueOf("8ae83a"));
        w.sprite = "acid_squirter";
        return w.fill();
    }

    /** C: a flare gun - one slow flare that sets what it hits on fire and leaves a patch of flames where it lands. */
    public static Weapon flareGun() {
        Weapon w = new Weapon("Flare Gun");
        w.tier = Tier.C;
        w.damage = 13f;
        w.fireInterval = 0.5f;
        w.bulletSpeed = 6.5f;
        w.bulletRadius = 0.06f;
        w.bulletLife = 0.9f;
        w.spreadDeg = 2f;
        w.shape = Bullet.Shape.FLARE;
        w.burn = 4f;
        w.puddle = Puddle.Kind.FIRE;
        w.puddleRadius = 0.4f;
        w.magSize = 7;
        w.reloadTime = 1.2f;
        w.maxAmmo = 60;
        w.knockback = 0.5f;
        w.shake = 0.1f;
        w.sound = "shoot";
        w.bulletColor.set(Color.valueOf("ff6a2a"));
        w.sprite = "flare_gun";
        return w.fill();
    }

    /** B: a gloop launcher - a fat glob of acid that bursts into a pool where it stops; enemies in it melt. */
    public static Weapon gloopLauncher() {
        Weapon w = new Weapon("Gloop Launcher");
        w.tier = Tier.B;
        w.damage = 12f;
        w.fireInterval = 0.6f;
        w.bulletSpeed = 5f;
        w.bulletRadius = 0.09f;
        w.bulletLife = 1f;
        w.spreadDeg = 3f;
        w.shape = Bullet.Shape.GLOB;
        w.acid = 3f;
        w.puddle = Puddle.Kind.ACID;
        w.puddleRadius = 0.7f;
        w.magSize = 5;
        w.reloadTime = 1.4f;
        w.maxAmmo = 50;
        w.knockback = 0.8f;
        w.shake = 0.14f;
        w.sound = "shotgun";
        w.bulletColor.set(Color.valueOf("7ad83a"));
        w.sprite = "gloop_launcher";
        return w.fill();
    }

    /**
     * B: the Flufful (Fluffy + Gundul), the owner's take on ETG's Gunther - a fluffy gun that talks while
     * you fight (objects/FluffulVoice). Soft pom-poms that home in a little, endless ammo, and it grows
     * stronger with every level of kills ({@link #addKill}). Not in any chest until it's unlocked
     * (save/Unlocks: tame the Fluffy Shark - the shark on top of it - in the secret sea, or endless wave 30).
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
        w.shape = Bullet.Shape.CLOUD; // little pink-bottomed clouds that grow with its level
        w.sprite = "flufful";
        return w.fill();
    }

    /**
     * A: the Chomper (after ETG's shark gun) - one shot and it reloads: a little fluffy shark swims
     * out, turns toward the nearest enemy and bites hard. Under water it swims faster and steers better.
     */
    public static Weapon chomper() {
        Weapon w = new Weapon("Chomper");
        w.tier = Tier.A;
        w.damage = 60f;
        w.bulletSpeed = 6f;
        w.bulletRadius = 0.12f;
        w.bulletLife = 1.7f;
        w.spreadDeg = 0f;
        w.homing = 110f;
        w.fireInterval = 0.3f;
        w.magSize = 1;
        w.reloadTime = 1.2f;
        w.maxAmmo = 50;
        w.knockback = 2.5f;
        w.shake = 0.3f;
        w.sound = "swish";
        w.shape = Bullet.Shape.SHARK;
        w.bulletColor.set(Color.valueOf("5f8fd8"));
        w.sprite = "chomper";
        return w.fill();
    }

    /**
     * B: the Boom-buss, a blunderbuss (after ETG's exploding shotguns) - a wide spray of little bombs,
     * each one going off with a small blast where it hits something; out of range they just fizzle.
     */
    public static Weapon boomBuss() {
        Weapon w = new Weapon("Boom-buss");
        w.tier = Tier.B;
        w.damage = 4f;
        w.pellets = 7;
        w.spreadDeg = 38f;
        w.blastRadius = 0.4f;
        w.blastDamage = 5f;
        w.impactOnly = true;
        w.bulletSpeed = 7f;
        w.bulletRadius = 0.06f;
        w.bulletLife = 0.5f;
        w.fireInterval = 0.6f;
        w.magSize = 2;
        w.reloadTime = 1.7f;
        w.maxAmmo = 40;
        w.knockback = 1.2f;
        w.shake = 0.45f;
        w.sound = "shotgun";
        w.shape = Bullet.Shape.BOMB;
        w.bulletColor.set(Color.valueOf("4a4a58"));
        w.sprite = "boom_buss";
        return w.fill();
    }

    private static final String THROWING_STARS = "Throwing Stars";
    private static final String BABY_RATTLE = "Baby Rattle";
    private static final String ROYAL_SCEPTRE = "Royal Sceptre";

    /**
     * The baby brother's starter (player/Hero.BABY): his rattle, shaken hard - a spray of little beads,
     * quick and forgiving (no need to aim well), each one weak; a big magazine and a quick reload. Below
     * the Rusty Sidearm, so anything found feels like a step up. Never in chests.
     */
    public static Weapon babyRattle() {
        Weapon w = new Weapon(BABY_RATTLE);
        w.damage = 2.2f;
        w.fireInterval = 0.11f;
        w.bulletSpeed = 7.5f;
        w.bulletRadius = 0.045f;
        w.bulletLife = 0.75f;
        w.spreadDeg = 12f;
        w.magSize = 24;
        w.reloadTime = 0.8f;
        w.knockback = 0.3f;
        w.shake = 0.04f;
        w.sound = "rattle";
        w.colorMode = ColorMode.CONFETTI;
        w.sprite = "baby_rattle";
        return w.fill();
    }

    /**
     * The pocket-size Fluffy King's starter (player/Hero.KING): his royal sceptre throws balls of fluff that
     * burst into a little ring wherever they stop - his own fluff balls, small enough for him now. Never in
     * chests.
     */
    public static Weapon royalSceptre() {
        Weapon w = new Weapon(ROYAL_SCEPTRE);
        w.damage = 5f;
        w.splits = 6;
        w.splitShape = Bullet.Shape.BALL;
        w.splitDamage = 0.35f;
        w.fireInterval = 0.32f;
        w.bulletSpeed = 6.5f;
        w.bulletRadius = 0.08f;
        w.bulletLife = 0.55f;
        w.spreadDeg = 3f;
        w.magSize = 8;
        w.reloadTime = 0.9f;
        w.knockback = 0.6f;
        w.shake = 0.06f;
        w.sound = "pop";
        w.shape = Bullet.Shape.POMPOM;
        w.bulletColor.set(Color.valueOf("ffc6dc"));
        w.sprite = "royal_sceptre";
        return w.fill();
    }

    /**
     * The sister's starter (player/Hero.SISTER), instead of the Rusty Sidearm: endless little throwing
     * stars, quick and spinning, a bit weaker each. Never in chests.
     */
    public static Weapon throwingStars() {
        Weapon w = new Weapon(THROWING_STARS);
        w.damage = 4f;
        w.fireInterval = 0.17f;
        w.bulletSpeed = 9f;
        w.bulletRadius = 0.05f;
        w.bulletLife = 0.9f;
        w.spreadDeg = 5f;
        w.magSize = 10;
        w.reloadTime = 0.9f;
        w.knockback = 0.5f;
        w.shake = 0.08f;
        w.sound = "swish";
        w.shape = Bullet.Shape.STAR;
        w.bulletColor.set(Color.valueOf("c9ccd6"));
        w.sprite = "throwing_stars";
        return w.fill();
    }

    // ------------------------------------------------------------------ toy box (D) and two odd ones (C)

    /** D: a slingshot - hold to pull back, let go: a quick flick is a weak pebble, a full pull a hard fast one. */
    public static Weapon slingshot() {
        Weapon w = new Weapon("Slingshot");
        w.tier = Tier.D;
        w.slings = true;
        w.damage = 15f; // at full pull; a flick does about a third
        w.fireInterval = 0.2f;
        w.bulletSpeed = 12f;
        w.bulletRadius = 0.05f;
        w.bulletLife = 0.45f;
        w.spreadDeg = 1f;
        w.magSize = 8;
        w.reloadTime = 1f;
        w.maxAmmo = 100;
        w.knockback = 1f;
        w.shake = 0.08f;
        w.sound = "twang";
        w.bulletColor.set(Color.valueOf("9a948a"));
        w.sprite = "slingshot";
        return w.fill();
    }

    /** D: a popcorn popper - kernels fly out and pop in mid-air (or on what they hit) into three buttery puffs. */
    public static Weapon popcornPopper() {
        Weapon w = new Weapon("Popcorn Popper");
        w.tier = Tier.D;
        w.damage = 3f;
        w.splits = 3;
        w.splitShape = Bullet.Shape.CLOUD;
        w.splitDamage = 0.8f;
        w.fireInterval = 0.3f;
        w.bulletSpeed = 6f;
        w.bulletRadius = 0.04f;
        w.bulletLife = 0.45f;
        w.spreadDeg = 8f;
        w.magSize = 12;
        w.reloadTime = 1.1f;
        w.maxAmmo = 150;
        w.knockback = 0.3f;
        w.shake = 0.05f;
        w.sound = "pop";
        w.bulletColor.set(Color.valueOf("f2c040"));
        w.sprite = "popcorn_popper";
        return w.fill();
    }

    /**
     * D: a hair dryer - a short stream of hot air: it hardly hurts, but it shoves enemies back and blows
     * smoke and stink clouds away.
     */
    public static Weapon hairDryer() {
        Weapon w = new Weapon("Hair Dryer");
        w.tier = Tier.D;
        w.damage = 0.8f;
        w.pellets = 2;
        w.spreadDeg = 24f;
        w.blowsGas = true;
        w.piercing = true;
        w.fireInterval = 0.07f; // ~23 damage/s up close
        w.bulletSpeed = 6f;
        w.bulletRadius = 0.05f;
        w.bulletLife = 0.3f;
        w.shape = Bullet.Shape.CLOUD;
        w.magSize = 60;
        w.reloadTime = 1.2f;
        w.maxAmmo = 600;
        w.knockback = 0.9f;
        w.shake = 0f;
        w.sound = "hiss";
        w.soundEvery = 5;
        w.bulletColor.set(Color.valueOf("ffc890"));
        w.sprite = "hair_dryer";
        return w.fill();
    }

    /** D: a toy cap gun - little clicks, and every sixth shot a big BANG. */
    public static Weapon capGun() {
        Weapon w = new Weapon("Cap Gun");
        w.tier = Tier.D;
        w.damage = 4f;
        w.bangEvery = 6;
        w.fireInterval = 0.2f;
        w.bulletSpeed = 8f;
        w.bulletRadius = 0.04f;
        w.bulletLife = 0.9f;
        w.spreadDeg = 4f;
        w.magSize = 12;
        w.reloadTime = 1f;
        w.maxAmmo = 180;
        w.knockback = 0.4f;
        w.shake = 0.06f;
        w.sound = "pop";
        w.bulletColor.set(Color.valueOf("ff5a4a"));
        w.sprite = "cap_gun";
        return w.fill();
    }

    /** D: a pea pod - every pull spits three peas one after another down the same line. */
    public static Weapon peaShooter() {
        Weapon w = new Weapon("Pea Shooter");
        w.tier = Tier.D;
        w.damage = 3f;
        w.burst = 3;
        w.burstGap = 0.07f;
        w.fireInterval = 0.4f;
        w.bulletSpeed = 8f;
        w.bulletRadius = 0.045f;
        w.bulletLife = 0.9f;
        w.spreadDeg = 3f;
        w.magSize = 10;
        w.reloadTime = 1f;
        w.maxAmmo = 120;
        w.knockback = 0.35f;
        w.shake = 0.04f;
        w.sound = "pop";
        w.bulletColor.set(Color.valueOf("7ad83a"));
        w.sprite = "pea_shooter";
        return w.fill();
    }

    /** C: sticky darts - suction-cup darts stick in what they hit, and the third one pops them all for extra damage. */
    public static Weapon stickyDarts() {
        Weapon w = new Weapon("Sticky Darts");
        w.tier = Tier.C;
        w.damage = 6.5f;
        w.sticky = true;
        w.fireInterval = 0.25f;
        w.bulletSpeed = 9f;
        w.bulletRadius = 0.035f;
        w.bulletLife = 0.9f;
        w.spreadDeg = 2f;
        w.streak = 0.16f;
        w.magSize = 9;
        w.reloadTime = 1.1f;
        w.maxAmmo = 120;
        w.knockback = 0.3f;
        w.shake = 0.04f;
        w.sound = "swish";
        w.bulletColor.set(Color.valueOf("ff8a3a"));
        w.sprite = "sticky_darts";
        return w.fill();
    }

    /** C: a kazoo - a slow fan of wobbly music notes that buzz through everything in their way. */
    public static Weapon kazoo() {
        Weapon w = new Weapon("Kazoo");
        w.tier = Tier.C;
        w.damage = 4.5f;
        w.pellets = 3;
        w.spreadDeg = 30f;
        w.wobble = 160f;
        w.piercing = true;
        w.fireInterval = 0.42f;
        w.bulletSpeed = 5f;
        w.bulletRadius = 0.06f;
        w.bulletLife = 1.4f;
        w.shape = Bullet.Shape.NOTE;
        w.colorMode = ColorMode.CONFETTI;
        w.magSize = 10;
        w.reloadTime = 1.2f;
        w.maxAmmo = 120;
        w.knockback = 0.4f;
        w.shake = 0.04f;
        w.sound = "buzz";
        w.sprite = "kazoo";
        return w.fill();
    }

    /**
     * C: a megaphone - no bullets, a shout: everything in a wide cone in front of you is stunned for a
     * moment and takes a tiny bit of damage, enemy bullets in it slow down, and so does the next volley a
     * shouted-at enemy fires (a breather, like a little blank). Bosses don't get stunned, only slowed.
     */
    public static Weapon megaphone() {
        Weapon w = new Weapon("Megaphone");
        w.tier = Tier.C;
        w.damage = 2.5f;
        w.shout = true;
        w.fireInterval = 0.75f;
        w.magSize = 6;
        w.reloadTime = 1.4f;
        w.maxAmmo = 60;
        w.shake = 0.1f;
        w.sound = "megaphone";
        w.bulletColor.set(Color.valueOf("fff2b0")); // the sound waves
        w.sprite = "megaphone";
        return w.fill();
    }

    /** C: paper planes - they glide out and turn after wherever you aim, even after they've left your hand. */
    public static Weapon paperPlanes() {
        Weapon w = new Weapon("Paper Planes");
        w.tier = Tier.C;
        w.damage = 8f;
        w.fireInterval = 0.3f;
        w.bulletSpeed = 5f;
        w.bulletRadius = 0.045f;
        w.bulletLife = 1.8f;
        w.spreadDeg = 6f;
        w.guided = 260f;
        w.shape = Bullet.Shape.PLANE;
        w.magSize = 10;
        w.reloadTime = 1.1f;
        w.maxAmmo = 140;
        w.knockback = 0.3f;
        w.shake = 0.05f;
        w.sound = "swish";
        w.bulletColor.set(Color.valueOf("f4f6fa"));
        w.sprite = "paper_planes";
        return w.fill();
    }

    /** C: a tennis ball launcher - balls bounce off walls and enemies alike, faster with every bounce. */
    public static Weapon tennisBalls() {
        Weapon w = new Weapon("Tennis Ball Launcher");
        w.tier = Tier.C;
        w.damage = 7f;
        w.fireInterval = 0.35f;
        w.bulletSpeed = 5.5f;
        w.bulletRadius = 0.055f;
        w.bulletLife = 2f;
        w.spreadDeg = 4f;
        w.bounces = 4;
        w.rebounds = true;
        w.bounceBoost = 1.2f;
        w.shape = Bullet.Shape.TENNIS;
        w.magSize = 8;
        w.reloadTime = 1.1f;
        w.maxAmmo = 120;
        w.knockback = 0.5f;
        w.sound = "pop";
        w.bulletColor.set(Color.valueOf("d8ec3a"));
        w.sprite = "tennis_balls";
        return w.fill();
    }

    /** C: a box of crayons - every shot a different colour: red sets enemies on fire, blue chills, green poisons. */
    public static Weapon crayonBox() {
        Weapon w = new Weapon("Crayon Box");
        w.tier = Tier.C;
        w.damage = 7f;
        w.fireInterval = 0.22f;
        w.bulletSpeed = 7f;
        w.bulletRadius = 0.04f;
        w.bulletLife = 1.1f;
        w.spreadDeg = 5f;
        w.crayons = true;
        w.shape = Bullet.Shape.CRAYON;
        w.magSize = 12;
        w.reloadTime = 1.1f;
        w.maxAmmo = 160;
        w.shake = 0.06f;
        w.bulletColor.set(Color.valueOf("e8443a"));
        w.sprite = "crayon_box";
        return w.fill();
    }

    /** C: a pinwheel - it whirls out, stops and spins for a moment, then throws off a ring of petals. */
    public static Weapon pinwheel() {
        Weapon w = new Weapon("Pinwheel");
        w.tier = Tier.C;
        w.damage = 6f;
        w.fireInterval = 0.45f;
        w.bulletSpeed = 5.5f;
        w.bulletAccel = -12f; // (stopped in under half a second, then it spins there)
        w.stops = true;
        w.bulletRadius = 0.07f;
        w.bulletLife = 0.85f;
        w.spreadDeg = 3f;
        w.piercing = true;
        w.splits = 8;
        w.splitShape = Bullet.Shape.PETAL;
        w.splitDamage = 0.6f;
        w.shape = Bullet.Shape.PINWHEEL;
        w.magSize = 6;
        w.reloadTime = 1.2f;
        w.maxAmmo = 80;
        w.knockback = 0.2f;
        w.sound = "swish";
        w.bulletColor.set(Color.valueOf("ff7ab8"));
        w.sprite = "pinwheel";
        return w.fill();
    }

    /** D: a sock launcher - smelly socks that leave a cloud of stink where they land; only enemies mind it. */
    public static Weapon sockLauncher() {
        Weapon w = new Weapon("Stinky Sock Launcher");
        w.tier = Tier.D;
        w.damage = 6f;
        w.fireInterval = 0.45f;
        w.bulletSpeed = 5f;
        w.bulletRadius = 0.06f;
        w.bulletLife = 0.75f;
        w.spreadDeg = 5f;
        w.stinks = true;
        w.shape = Bullet.Shape.SOCK;
        w.magSize = 6;
        w.reloadTime = 1.1f;
        w.maxAmmo = 90;
        w.knockback = 0.4f;
        w.sound = "pop";
        w.bulletColor.set(Color.valueOf("f0ece0"));
        w.sprite = "sock_launcher";
        return w.fill();
    }

    // ------------------------------------------------------------------ A: growing, swarming, rolling; S

    /**
     * A: the Heirloom (our take on ETG's Gunderfury) - the family's old pistol, passed down the
     * generations. Plain and weak when found, it grows from every kill made while you carry it (any gun):
     * faster, then piercing, then two and three shots, homing at level 5. Endless ammo. Grandad's kills
     * count double (GameWorld) - in his hands it's a killing machine. Its kills are kept for good: found
     * again in a later run, it comes at the level it had reached.
     */
    public static Weapon heirloom() {
        Weapon w = new Weapon("Heirloom");
        w.tier = Tier.A;
        w.heirloom = true;
        w.damage = HEIR_DAMAGE[0];
        w.fireInterval = HEIR_INTERVAL[0];
        w.bulletSpeed = HEIR_SPEED[0];
        w.bulletRadius = 0.045f;
        w.bulletLife = 1f;
        w.spreadDeg = HEIR_SPREAD[0];
        w.magSize = HEIR_MAG[0];
        w.reloadTime = 1f;
        w.maxAmmo = INFINITE;
        w.knockback = 0.6f;
        w.shake = 0.1f;
        w.sound = "shoot";
        w.bulletColor.set(Color.valueOf("e0c890"));
        w.sprite = "heirloom_1";
        w.restoreKills(Unlocks.heirloomKills);
        return w.fill();
    }

    /** A: a rack of bottle rockets (after ETG's Yari Launcher) - each pull sends four little homing rockets off one by one. */
    public static Weapon bottleRocketRack() {
        Weapon w = new Weapon("Bottle Rocket Rack");
        w.tier = Tier.A;
        w.damage = 5f;
        w.burst = 4;
        w.burstGap = 0.07f;
        w.rocket = true;
        w.blastRadius = 0.5f;
        w.blastDamage = 6f;
        w.impactOnly = true; // a little pop where one hits; a miss just fizzles
        w.homing = 200f;
        w.bulletSpeed = 4f;
        w.bulletAccel = 8f;
        w.bulletRadius = 0.04f;
        w.bulletLife = 1.4f;
        w.spreadDeg = 20f;
        w.fireInterval = 0.6f;
        w.magSize = 5;
        w.reloadTime = 1.4f;
        w.maxAmmo = 60;
        w.knockback = 0.5f;
        w.shake = 0.12f;
        w.sound = "swish";
        w.colorMode = ColorMode.CONFETTI;
        w.sprite = "bottle_rocket_rack";
        return w.fill();
    }

    /** A: a cannon that fires a giant pom-pom - it rolls slowly across the room, flattening everything, and bounces off walls. */
    public static Weapon fluffCannon() {
        Weapon w = new Weapon("Fluff Cannon");
        w.tier = Tier.A;
        w.damage = 24f;
        w.piercing = true;
        w.bounces = 3;
        w.bulletSpeed = 3.2f;
        w.bulletRadius = 0.22f;
        w.bulletLife = 4f;
        w.spreadDeg = 0f;
        w.shape = Bullet.Shape.POMPOM;
        w.fireInterval = 0.9f;
        w.magSize = 3;
        w.reloadTime = 1.5f;
        w.maxAmmo = 30;
        w.knockback = 3f;
        w.shake = 0.3f;
        w.sound = "shotgun";
        w.bulletColor.set(Color.valueOf("ff9fc4"));
        w.sprite = "fluff_cannon";
        return w.fill();
    }

    /**
     * B: a rubber duck launcher - ducks waddle off toward the nearest enemy and burst with a squeak when they
     * reach it. They swim faster in water.
     */
    public static Weapon rubberDuckLauncher() {
        Weapon w = new Weapon("Rubber Duck Launcher");
        w.tier = Tier.B;
        w.damage = 6f;
        w.blastRadius = 0.6f;
        w.blastDamage = 8f;
        w.impactOnly = true;
        w.homing = 160f;
        w.wobble = 60f;
        w.bulletSpeed = 3.5f;
        w.bulletRadius = 0.07f;
        w.bulletLife = 2.2f;
        w.spreadDeg = 10f;
        w.shape = Bullet.Shape.DUCK;
        w.fireInterval = 0.4f;
        w.magSize = 6;
        w.reloadTime = 1.2f;
        w.maxAmmo = 72;
        w.knockback = 0.6f;
        w.shake = 0.06f;
        w.sound = "pop";
        w.bulletColor.set(Color.valueOf("ffd23a"));
        w.sprite = "duck_launcher";
        return w.fill();
    }

    /** S: the Unicorn Horn - a long, thick rainbow beam that burns through everything in its way. */
    public static Weapon unicornHorn() {
        Weapon w = new Weapon("Unicorn Horn");
        w.tier = Tier.S;
        w.damage = 4f; // every 0.05 s: 80 damage/s through every enemy on the beam
        w.fireInterval = 0.05f;
        w.beamRange = 7f;
        w.beamWidth = 0.07f;
        w.piercing = true;
        w.spreadDeg = 0f;
        w.magSize = 200;
        w.reloadTime = 1.5f;
        w.maxAmmo = 1000;
        w.knockback = 0.1f;
        w.shake = 0.02f;
        w.sound = "zap";
        w.colorMode = ColorMode.RAINBOW;
        w.bulletColor.set(Color.valueOf("ff9fe0"));
        w.sprite = "unicorn_horn";
        return w.fill();
    }

    /**
     * S: the Shooting Star - a wand that throws a homing star; where it lands it blows up and bursts into
     * eight smaller stars.
     */
    public static Weapon shootingStar() {
        Weapon w = new Weapon("Shooting Star");
        w.tier = Tier.S;
        w.damage = 20f;
        w.blastRadius = 1.2f;
        w.blastDamage = 30f;
        w.splits = 8;
        w.splitShape = Bullet.Shape.STAR;
        w.splitDamage = 0.4f;
        w.homing = 150f;
        w.bulletSpeed = 9f;
        w.bulletRadius = 0.1f;
        w.bulletLife = 1.2f;
        w.spreadDeg = 0f;
        w.shape = Bullet.Shape.STAR;
        w.fireInterval = 0.55f;
        w.magSize = 6;
        w.reloadTime = 1.3f;
        w.maxAmmo = 72;
        w.knockback = 1.2f;
        w.shake = 0.2f;
        w.sound = "zap";
        w.bulletColor.set(Color.valueOf("ffe066"));
        w.sprite = "shooting_star";
        return w.fill();
    }

    private static final String PEARL_REVOLVER = "Pearl Revolver";

    /**
     * Granny's starter (player/Hero.GRANNY): an antique revolver, engraved silver with a mother-of-pearl
     * grip. Six heavier shots than the sidearm, a bit slower, and a slow reload of the cylinder. Never in chests.
     */
    public static Weapon pearlRevolver() {
        Weapon w = new Weapon(PEARL_REVOLVER);
        w.damage = 6.5f; // still drops a Bullet Kin in two, but tougher kin take three
        w.fireInterval = 0.27f;
        w.bulletSpeed = 9f;
        w.bulletRadius = 0.05f;
        w.bulletLife = 1.1f;
        w.spreadDeg = 2f;
        w.magSize = 6;
        w.reloadTime = 1.2f;
        w.knockback = 0.9f;
        w.shake = 0.15f;
        w.sound = "magnum";
        w.bulletColor.set(Color.valueOf("f4ecff"));
        w.sprite = "pearl_revolver";
        return w.fill();
    }

    private static final String DOUBLE_BARREL = "Old Double-Barrel";

    /**
     * Grandad's starter (player/Hero.GRANDAD): his old side-by-side shotgun - two blasts, then he cracks
     * it open to reload. Like ETG's Gunslinger's starter: a good D-level gun that can beat the first boss,
     * and with grandad's +1 on everything it's about as good as the Fluff Shotgun (C). Never in chests.
     */
    public static Weapon doubleBarrel() {
        Weapon w = new Weapon(DOUBLE_BARREL);
        w.damage = 3f;
        w.pellets = 6;
        w.spreadDeg = 30f;
        w.fireInterval = 0.3f; // both barrels quickly, then the reload: ~22 damage/s up close (+1: ~26-28)
        w.bulletSpeed = 7f;
        w.bulletLife = 0.6f;
        w.bulletRadius = 0.045f;
        w.magSize = 2;
        w.reloadTime = 1.3f;
        w.knockback = 1.4f;
        w.shake = 0.35f;
        w.sound = "shotgun";
        w.bulletColor.set(Color.valueOf("ffc870"));
        w.sprite = "double_barrel";
        return w.fill();
    }

    /** Puts out the burning drum: water, snow or anything cold (the Freeze Ray). */
    public boolean douses() {
        return wet || frost > 0;
    }

}
