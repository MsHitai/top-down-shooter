package com.trush.game.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.save.Unlocks;
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;
import com.trush.game.input.Gamepad;
import com.trush.game.launcher.Dev;
import com.trush.game.input.Gamepad.Button;
import com.trush.game.objects.Coins;
import com.trush.game.objects.Item;
import com.trush.game.objects.Prop;
import com.trush.game.objects.Weapon;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import lombok.Getter;

/**
 * The hero. Position is the bottom-left of the sprite ({@code x}, {@code y}); collisions use a small
 * box around the feet, bullets hit a small circle around the belly. The sprite has no arms baked in:
 * like in ETG, a floating hand holds the gun and follows the cursor.
 */
@Getter
public class Player {

    public static final float WIDTH = Sprites.HERO_W / Sprites.PPU;
    public static final float HEIGHT = Sprites.HERO_H / Sprites.PPU;
    public static final float HITBOX_RADIUS = 0.1f;
    private static final float FEET_W = 0.3f;
    private static final float FEET_H = 0.14f;

    private static final float ROLL_TIME = 0.42f;
    private static final float ROLL_INVULNERABLE_TIME = 0.32f;
    private static final float ROLL_SPEED = 5.2f;
    private static final float ROLL_COOLDOWN = 0.18f;
    /** Speed (x roll speed) while sliding off a table at the end of a roll. */
    private static final float TABLE_SLIDE = 0.6f;
    /** A roll pressed this long before it's possible (mid-roll, cooldown) still happens. */
    private static final float ROLL_BUFFER = 0.2f;
    /** Releasing the stick a moment before pressing roll still rolls the way you were going. */
    private static final float ROLL_DIR_MEMORY = 0.15f;
    /** Raw stick push that counts for the roll direction even inside the walking dead zone. */
    private static final float ROLL_STICK_MIN = 0.15f;
    private static final float HURT_INVULNERABLE_TIME = 1.2f;
    private static final float FALL_TIME = 0.5f;
    /** Death animation: hit, knees buckle, slump, then he hits the floor at {@link #DEATH_FALL}. */
    private static final float[] DEATH_FRAMES = {0.12f, 0.24f, 0.36f};
    private static final float DEATH_FALL = 0.36f;
    /** When the death animation is over and the game over screen can show. */
    public static final float DEATH_DONE = 0.9f;
    /** Dev builds only: -Dfluffy.god=true - hits register but never cost health. */
    private static final boolean GOD_MODE = Dev.flag("god");

    private static final Color SKIN = Color.valueOf("d9a066");
    private static final Color STUN_STAR = Color.valueOf("ffe066");
    private static final Color OUTLINE = Color.valueOf("1e1416");
    private static final Color RELOAD_BG = Color.valueOf("1b1b1b");
    private static final Color RELOAD_FG = Color.valueOf("f5f5f5");
    private static final Color DUST = Color.valueOf("c8b49a");

    private final Sprites sprites;
    /** Who's playing (the boy, his sister, granny, grandad) and their sprite sheet. */
    private final Hero hero;
    private final TextureRegion[][] frames;
    private final TextureRegion deadFrame;
    private float x;
    private float y;
    private final Rectangle feet = new Rectangle();
    private final Vector2 moveDir = new Vector2();
    private final Vector2 rollDir = new Vector2();
    private final Array<Weapon> weapons = new Array<>();
    private int weaponIndex;

    private final float speed = 2.6f;
    private int maxHealth = 6; // in half hearts: 6 = 3 hearts
    private int health = 6;
    private int blanks = 2;
    private int keys;
    /** Coins for Baa-llo's shop (saved between floors, like keys). */
    private int coins;
    /** Passive items picked up this run, and the one active item (or null). */
    private final Array<Item> passives = new Array<>();
    private Item active;
    /** Until the active item can be used again (the grenade recharges). */
    private float activeCooldown;
    /** What the active item's recharge started from (the HUD fills its box by the fraction left). */
    private float activeCooldownMax = 1f;
    /** The Guardian Lamb just saved a fatal hit (GameWorld puts on the show and clears this). */
    private boolean savedByLamb;
    private static final float LAMB_INVULNERABLE_TIME = 2f;
    /** Bubble Shield: blocks one hit, then comes back after the player has dealt enough damage. */
    private boolean shieldUp;
    private float shieldCharge;
    /** Bike Helmet: on the head until a hit knocks it off; back on once the fight is over. */
    private boolean helmetOn;
    /** On fire (stood in flames) for this much longer; a dodge roll puts it out, like in ETG. */
    private float burning;
    private float burnTick;
    /** How long the hero has been standing in acid (it bites after ACID_DELAY). */
    private float acidSoak;
    public static final float BURN_TIME = 2.5f;
    /** The first burn lands this long after catching fire (time to roll it out), then every BURN_INTERVAL. */
    public static final float BURN_DELAY = 0.7f;
    private static final float BURN_INTERVAL = 1.5f;
    public static final float ACID_DELAY = 0.45f;
    private static final Color FLAME = Color.valueOf("ff8a1e");
    private static final Color FLAME_CORE = Color.valueOf("ffd84a");
    private static final Color HELMET = Color.valueOf("e8443a");
    private static final Color HELMET_SHINE = Color.valueOf("ff9a8a");
    /** Damage the player must deal to blow a new bubble (a few rooms of small fry, or part of a boss). */
    public static final float SHIELD_RECHARGE = 250f;
    private boolean dead;
    private float deathTime;
    /** Falling into a pit: shrinks out of sight, then GameWorld puts him back at the last safe spot. */
    private float fallTimer;

    private float invulnerable;
    private float rollTimer;
    /** Stunned (the Fluffy Shark's tail wave): no walking, rolling or shooting until it wears off. */
    private float stunTimer;
    private float rollCooldown;
    private float rollBuffer;
    /** Tables already knocked during the current roll, and where the roll started. */
    private final Array<Prop> rolledOver = new Array<>();
    private float rollOvertime;
    private float rollStartX;
    private float rollStartY;
    private final Vector2 lastMoveDir = new Vector2();
    private float sinceMoved = 99f;
    private float animTime;
    private float aimAngle = 90f;
    private boolean facingRight = true;

    public Player(Sprites sprites) {
        this(sprites, Hero.BOY);
    }

    public Player(Sprites sprites, Hero hero) {
        this.sprites = sprites;
        this.hero = hero;
        this.frames = sprites == null ? null : sprites.hero(hero.sheet); // (tests run without sprites)
        this.deadFrame = sprites == null ? null : sprites.heroDead(hero.sheet);
        this.maxHealth = hero.halfHearts;
        this.health = hero.halfHearts;
        weapons.add(hero.starter());
        if (Dev.flag("allGuns")) { // dev builds: -Dfluffy.allGuns=true
            weapons.addAll(Weapon.chestGuns());
            if (!Unlocks.flufful) {
                weapons.add(Weapon.flufful()); // the talking gun too, unlocked or not
            }
        }
        String items = Dev.text("items"); // dev builds: -Dfluffy.items=all or e.g. -Dfluffy.items=fluff_field,grenade
        if (items != null) {
            for (Item i : Item.values()) {
                boolean wanted = items.equalsIgnoreCase("all");
                for (String n : items.split(",")) {
                    wanted |= n.trim().replace(' ', '_').equalsIgnoreCase(i.name());
                }
                if (!wanted) continue;
                if (!i.active) passives.add(i);
                else if (active == null) active = i;
                if (i == Item.FLUFFY_HEART) addHeartContainer(); // (its heart comes on pickup, so add it here too)
            }
            shieldUp = hasItem(Item.BUBBLE_SHIELD);
            helmetOn = hasItem(Item.BIKE_HELMET);
            if (hasItem(Item.BIGGER_CLIPS)) {
                for (Weapon w : weapons) w.boostMagazine(1.5f);
            }
        }
    }

    // ------------------------------------------------------------ geometry

    public float centerX() {
        return x + WIDTH / 2f;
    }

    public float feetX() {
        return centerX();
    }

    public float feetY() {
        return y;
    }

    /** Places the player so the middle of their feet is at (fx, fy). */
    public void setFeet(float fx, float fy) {
        x = fx - WIDTH / 2f;
        y = fy;
    }

    /** Small circle around the belly that enemy bullets must hit - grazing the head doesn't count. */
    public float hitboxY() {
        return y + HEIGHT * 0.32f;
    }

    public Rectangle feetRect(Rectangle out) {
        return out.set(centerX() - FEET_W / 2f, y, FEET_W, FEET_H);
    }

    // The gun hand floats just outside the torso (10 px wide) on the side he faces, at belly height,
    // and leans a couple of pixels toward the cursor - like the hands in ETG.
    private static final float HAND_SIDE = 7f / Sprites.PPU;
    private static final float HAND_Y = 9f / Sprites.PPU;
    private static final float HAND_REACH = 2f / Sprites.PPU;

    private float handAnchorX() {
        return centerX() + (facingRight ? HAND_SIDE : -HAND_SIDE);
    }

    private float handAnchorY() {
        return y + HAND_Y;
    }

    /** Where the gun hand is: player bullets sweep from here on their first frame (point-blank hits). */
    public float gunHandX() {
        return handX();
    }

    public float gunHandY() {
        return handY();
    }

    private float handX() {
        return handAnchorX() + MathUtils.cosDeg(aimAngle) * HAND_REACH;
    }

    private float handY() {
        return handAnchorY() + MathUtils.sinDeg(aimAngle) * HAND_REACH;
    }

    public float muzzleX() {
        return handX() + MathUtils.cosDeg(aimAngle) * gunSprite().length();
    }

    public float muzzleY() {
        return handY() + MathUtils.sinDeg(aimAngle) * gunSprite().length();
    }

    private Sprites.GunSprite gunSprite() {
        return sprites.gun(currentWeapon().getSprite());
    }

    // ------------------------------------------------------------ state

    public Weapon currentWeapon() {
        return weapons.get(weaponIndex);
    }

    public boolean isStunned() {
        return stunTimer > 0 && !dead;
    }

    /** Knocked silly for {@code seconds}: stands still, can't roll or shoot (a roll in progress is unaffected). */
    public void stun(float seconds) {
        stunTimer = Math.max(stunTimer, seconds);
    }

    public boolean isRolling() {
        return rollTimer > 0;
    }

    public boolean isInvulnerable() {
        return invulnerable > 0 || fallTimer > 0 || (isRolling() && ROLL_TIME - rollTimer < ROLL_INVULNERABLE_TIME);
    }

    public boolean isFalling() {
        return fallTimer > 0;
    }

    public void startFall() {
        fallTimer = FALL_TIME;
        rollTimer = 0;
        currentWeapon().cancelReload();
    }

    /** Falling always costs half a heart, like in ETG, even right after another hit. */
    public void clearInvulnerability() {
        invulnerable = 0;
    }

    public void selectWeapon(int index) {
        if (index >= 0 && index < weapons.size && index != weaponIndex) {
            currentWeapon().cancelReload();
            currentWeapon().cancelCharge();
            weaponIndex = index;
        }
    }

    public void cycleWeapon(int dir) {
        if (weapons.size > 1) {
            selectWeapon(((weaponIndex + dir) % weapons.size + weapons.size) % weapons.size);
        }
    }

    /** Applies the state of a saved run. */
    public void restore(int maxHealth, int health, int blanks, int keys, Array<Weapon> saved, int index) {
        this.keys = keys;
        this.maxHealth = Math.max(2, maxHealth);
        this.health = Math.max(1, Math.min(this.maxHealth, health));
        this.blanks = blanks;
        if (saved.size > 0) {
            weapons.clear();
            weapons.addAll(saved);
        }
        weaponIndex = Math.max(0, Math.min(weapons.size - 1, index));
    }

    // ------------------------------------------------------------ items

    public boolean hasItem(Item item) {
        return passives.contains(item, true) || active == item;
    }

    /** Walking speed (Fluffy Slippers: faster). */
    public float moveSpeed() {
        return speed * hero.walkSpeed * (hasItem(Item.FLUFFY_SLIPPERS) ? 1.2f : 1f);
    }

    /** Bullet damage multiplier (Hot Sauce). */
    public float damageScale() {
        return hasItem(Item.HOT_SAUCE) ? 1.25f : 1f;
    }

    /** A passive item: works from now on (some effects apply right away). */
    public void addPassive(Item item) {
        passives.add(item);
        applyOnPickup(item);
    }

    private void applyOnPickup(Item item) {
        switch (item) {
            case SPARE_POCKET:
                addBlank();
                break;
            case BIGGER_CLIPS:
                for (Weapon w : weapons) w.boostMagazine(1.5f);
                break;
            case BUBBLE_SHIELD:
                shieldUp = true;
                break;
            case BIKE_HELMET:
                helmetOn = true;
                break;
            case FLUFFY_HEART:
                addHeartContainer();
                break;
            default:
                break;
        }
    }

    /** The Grenade: the shortest and the longest throw, and how long RT / F is held for the longest. */
    public static final float GRENADE_MIN = 1f;
    public static final float GRENADE_MAX = 4.5f;
    public static final float GRENADE_FULL_CHARGE = 0.8f;
    /** Seconds RT / F has been held to throw the Grenade; negative when not charging. */
    private float grenadeCharge = -1f;

    public boolean isChargingGrenade() {
        return grenadeCharge >= 0;
    }

    /** How far the Grenade would fly if thrown now. */
    public float grenadeDistance() {
        return GRENADE_MIN + (GRENADE_MAX - GRENADE_MIN) * Math.max(0f, grenadeCharge) / GRENADE_FULL_CHARGE;
    }

    /** Charges the Grenade throw as if RT / F were held this long (tests). */
    void chargeGrenade(float seconds) {
        grenadeCharge = Math.min(GRENADE_FULL_CHARGE, Math.max(0f, grenadeCharge) + seconds);
    }

    /** Takes an active item; returns the one it replaces (dropped on the floor), or null. */
    public Item takeActive(Item item) {
        Item old = active;
        active = item;
        activeCooldown = 0;
        grenadeCharge = -1f;
        return old;
    }

    public Item getActive() {
        return active;
    }

    public Array<Item> getPassives() {
        return passives;
    }

    public float getActiveCooldown() {
        return activeCooldown;
    }

    public boolean isShieldUp() {
        return shieldUp;
    }

    public void useUpActive() {
        active = null;
    }

    public void setActiveCooldown(float seconds) {
        activeCooldown = seconds;
        activeCooldownMax = Math.max(0.01f, seconds);
    }

    public float getActiveCooldownMax() {
        return activeCooldownMax;
    }

    /** True once after the Guardian Lamb saved a fatal hit. */
    public boolean takeLambSave() {
        boolean saved = savedByLamb;
        savedByLamb = false;
        return saved;
    }

    /**
     * Bubble Shield: damage dealt to enemies charges a popped bubble back up, like ETG's charged items.
     * Returns true when this hit made it whole again.
     */
    public boolean chargeShield(float damage) {
        if (shieldUp || !hasItem(Item.BUBBLE_SHIELD) || damage <= 0) {
            return false;
        }
        shieldCharge += damage;
        if (shieldCharge < SHIELD_RECHARGE) {
            return false;
        }
        shieldCharge = 0;
        shieldUp = true;
        return true;
    }

    /** How far a popped bubble has charged back up, 0..1. */
    public float shieldProgress() {
        return shieldUp ? 1f : Math.min(1f, shieldCharge / SHIELD_RECHARGE);
    }

    /** Bubble Shield: soaks up this hit if it's up. */
    public boolean consumeShield() {
        if (!shieldUp) {
            return false;
        }
        shieldUp = false;
        shieldCharge = 0;
        invulnerable = 1f;
        return true;
    }

    /** Items from a saved run (Bigger Clips is applied to the restored guns again). */
    public void restoreItems(Array<Item> savedPassives, Item savedActive) {
        passives.clear();
        for (Item i : savedPassives) {
            passives.add(i);
            if (i == Item.BIGGER_CLIPS) {
                for (Weapon w : weapons) w.boostMagazine(1.5f);
            }
        }
        active = savedActive;
        shieldUp = hasItem(Item.BUBBLE_SHIELD);
        helmetOn = hasItem(Item.BIKE_HELMET);
    }

    public boolean isHelmetOn() {
        return helmetOn;
    }

    /** Bike Helmet: takes this hit if it's on (it flies off). */
    public boolean consumeHelmet() {
        if (!helmetOn) {
            return false;
        }
        helmetOn = false;
        invulnerable = 1f;
        return true;
    }

    /** The fight is over: the Bike Helmet goes back on. True if it was off. */
    public boolean refitHelmet() {
        if (helmetOn || !hasItem(Item.BIKE_HELMET)) {
            return false;
        }
        helmetOn = true;
        return true;
    }

    // ------------------------------------------------------------ fire and acid on the floor

    public boolean isBurning() {
        return burning > 0 && !dead;
    }

    /** Puts the flames out (a roll, water). */
    public void douse() {
        burning = 0;
    }

    /**
     * Fire and acid underfoot, like ETG's goop: stepping into flames sets the hero alight - a hit after
     * {@link #BURN_DELAY}, then every so often until it burns out - and standing in acid bites after
     * {@link #ACID_DELAY} (not with the Acid Boots). A dodge roll is in the air: it passes over both, and
     * rolling puts the flames out. Returns true when it's time for a hit (GameWorld deals it).
     */
    public boolean updateHazards(float delta, boolean inFire, boolean inAcid) {
        if (dead || isFalling()) {
            return false;
        }
        if (isRolling()) {
            if (burning > 0) {
                burning = 0;
                rolledOutFire = true;
            }
            acidSoak = 0;
            return false;
        }
        boolean hit = false;
        if (inFire) {
            if (burning <= 0) {
                burnTick = BURN_DELAY;
            }
            burning = Math.max(burning, BURN_TIME);
        }
        if (burning > 0) {
            burning -= delta;
            burnTick -= delta;
            if (burnTick <= 0) {
                burnTick = BURN_INTERVAL;
                hit = true;
            }
        }
        if (inAcid && !hasItem(Item.ACID_BOOTS)) {
            acidSoak += delta;
            if (acidSoak >= ACID_DELAY) {
                acidSoak = 0;
                hit = true;
            }
        } else {
            acidSoak = 0;
        }
        return hit;
    }

    /** True once after a roll put the flames out (GameWorld puffs some smoke). */
    private boolean rolledOutFire;

    public boolean takeRolledOutFire() {
        boolean r = rolledOutFire;
        rolledOutFire = false;
        return r;
    }

    /**
     * Takes a gun. One the player already carries isn't added twice: their copy gets a random upgrade
     * (returned) and is refilled instead. Returns null for a new gun.
     */
    public Weapon.Upgrade addWeapon(Weapon w) {
        for (int i = 0; i < weapons.size; i++) {
            if (weapons.get(i).getName().equals(w.getName())) {
                selectWeapon(i);
                return weapons.get(i).upgrade();
            }
        }
        if (hasItem(Item.BIGGER_CLIPS)) {
            w.boostMagazine(1.5f);
        }
        if (hero == Hero.GRANDAD) { // every gun grandad gets is a +1
            w.upgrade();
        }
        weapons.add(w);
        selectWeapon(weapons.size - 1);
        return null;
    }

    /** Most hearts the player can have (in half hearts): 16 - the 9th to 16th are golden, layered over the first 8. */
    public static final int MAX_HEALTH_CAP = 32;
    /** Red hearts (in half hearts); every heart past these is a golden one. */
    public static final int RED_HEARTS = 16;

    /** A heart container: one more heart, filled. False if already at the cap. */
    public boolean addHeartContainer() {
        if (maxHealth >= MAX_HEALTH_CAP) {
            return heal(2);
        }
        maxHealth += 2;
        health = Math.min(maxHealth, health + 2);
        return true;
    }

    public boolean heal(int halfHearts) {
        if (health >= maxHealth) {
            return false;
        }
        health = Math.min(maxHealth, health + halfHearts);
        return true;
    }

    public void addCoins(int n) {
        coins = Math.min(coins + n, Coins.MAX);
    }

    /** Pays {@code price} if the purse holds that much; false (and nothing spent) if not. */
    public boolean spendCoins(int price) {
        if (coins < price) {
            return false;
        }
        coins -= price;
        return true;
    }

    /** A saved run's coins. */
    public void setCoins(int coins) {
        this.coins = Math.max(0, Math.min(coins, Coins.MAX));
    }

    public void addKey() {
        keys = Math.min(keys + 1, 9);
    }

    public boolean consumeKey() {
        if (keys <= 0) {
            return false;
        }
        keys--;
        return true;
    }

    public void addBlank() {
        blanks = Math.min(blanks + 1, 6);
    }

    public void ensureBlanks(int min) {
        blanks = Math.max(blanks, min);
    }

    public boolean consumeBlank() {
        if (blanks <= 0) {
            return false;
        }
        blanks--;
        return true;
    }

    /** Returns true if the hit landed. */
    public boolean hurt(GameWorld world) {
        if (dead || isInvulnerable()) {
            return false;
        }
        if (GOD_MODE) {
            invulnerable = HURT_INVULNERABLE_TIME;
            return true;
        }
        health--;
        invulnerable = HURT_INVULNERABLE_TIME;
        if (health <= 0 && passives.removeValue(Item.GUARDIAN_LAMB, true)) {
            // like ETG's Pig: the lamb gives itself up, and you're back on your feet at full health
            health = maxHealth;
            invulnerable = LAMB_INVULNERABLE_TIME;
            savedByLamb = true;
        }
        if (health <= 0) {
            health = 0;
            dead = true;
        }
        return true;
    }

    // ------------------------------------------------------------ movement

    private final Rectangle tmp = new Rectangle();

    /** Moves by (dx, dy), sliding along walls and furniture. */
    public void nudge(float dx, float dy, GameWorld world) {
        nudge(dx, dy, world, isRolling());
    }

    /** @param overTables standing tables don't block (a roll, including its slide off the far side) */
    private void nudge(float dx, float dy, GameWorld world, boolean overTables) {
        if (dx != 0) {
            x += dx;
            Rectangle hit = world.findSolid(feetRect(tmp), overTables);
            if (hit != null) {
                float cx = dx > 0 ? hit.x - FEET_W / 2f - 0.001f : hit.x + hit.width + FEET_W / 2f + 0.001f;
                x = cx - WIDTH / 2f;
            }
        }
        if (dy != 0) {
            y += dy;
            Rectangle hit = world.findSolid(feetRect(tmp), overTables);
            if (hit != null) {
                y = dy > 0 ? hit.y - FEET_H - 0.001f : hit.y + hit.height + 0.001f;
            }
        }
    }

    // ------------------------------------------------------------ update

    public void update(float delta, GameWorld world) {
        if (dead) {
            float before = deathTime;
            deathTime += delta;
            if (before < DEATH_FALL && deathTime >= DEATH_FALL) { // thud
                world.particles().burst(centerX(), feetY() + 0.05f, 12, 0.4f, 1.4f, 0.4f, 0.05f, DUST);
                world.shake(0.3f);
                world.sfx().play("body_fall", 0.9f);
            }
            return;
        }
        if (fallTimer > 0) {
            fallTimer -= delta;
            if (fallTimer <= 0) {
                world.onPlayerFell();
            }
            return;
        }
        invulnerable -= delta;
        stunTimer -= delta;
        activeCooldown = Math.max(0, activeCooldown - delta);
        rollCooldown -= delta;
        animTime += delta;

        Vector2 aim = world.getAim();
        facingRight = aim.x >= centerX();
        aimAngle = MathUtils.atan2Deg360(aim.y - handAnchorY(), aim.x - handAnchorX());

        moveDir.setZero();
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) moveDir.x += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) moveDir.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) moveDir.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) moveDir.y -= 1;
        moveDir.nor();
        Gamepad pad = world.pad();
        if (!pad.leftStick().isZero()) {
            moveDir.set(pad.leftStick()); // analog: a light push walks slower
        }

        if (!moveDir.isZero()) {
            lastMoveDir.set(moveDir).nor();
            sinceMoved = 0;
        } else {
            sinceMoved += delta;
        }
        if (isStunned()) {
            moveDir.setZero();
        }
        rollBuffer -= delta;
        if (!isStunned() && (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)
                || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) || pad.justPressed(Button.LB))) {
            rollBuffer = ROLL_BUFFER;
        }

        Weapon weapon = currentWeapon();
        weapon.update(delta);

        if (isRolling()) {
            rollTimer -= delta;
            float progress = 1f - Math.max(0, rollTimer) / ROLL_TIME;
            // slows down toward the end; still on a table after the roll: slide across at a decent pace
            float k = rollOvertime > 0 ? TABLE_SLIDE : 1f - 0.65f * progress;
            float beforeX = x;
            float beforeY = y;
            float want = ROLL_SPEED * hero.rollReach * (hasItem(Item.BOUNCY_SOCKS) ? 1.3f : 1f) * k * delta; // socks: further
            // the whole roll passes over tables - including the slide after the timer has run out
            // (otherwise the tables turn solid mid-slide and snap the player back to the near side)
            nudge(rollDir.x * want, rollDir.y * want, world, true);
            boolean blocked = Vector2.dst(beforeX, beforeY, x, y) < want * 0.3f;
            if (MathUtils.randomBoolean(0.5f)) {
                world.particles().spawn(centerX(), feetY() + 0.03f, -rollDir.x * 0.5f, 0.2f, 0.35f, 0.04f, DUST, 3f);
            }
            boolean onTable = world.rollOverTables(feetRect(tmp), rolledOver);
            if (rollTimer <= 0 && onTable) {
                // never stop on top of a table: keep sliding across (a 2x2 block of tables, diagonally,
                // takes a while); if something blocks the far side, hop to the nearest free spot instead
                rollOvertime += delta;
                if (rollOvertime < 0.6f && !(blocked && rollOvertime > 0.05f)) {
                    rollTimer = 0.0001f;
                } else {
                    hopToFreeSpot(world);
                }
            }
            if (rollTimer <= 0) {
                rollCooldown = ROLL_COOLDOWN;
                world.onRollEnd();
            }
        } else {
            nudge(moveDir.x * moveSpeed() * delta, moveDir.y * moveSpeed() * delta, world);

            if (rollBuffer > 0 && rollCooldown <= 0) {
                rollBuffer = 0;
                aimRoll(pad);
                rollTimer = ROLL_TIME;
                rolledOver.clear();
                rollOvertime = 0;
                rollStartX = x;
                rollStartY = y;
                world.sfx().play("dodge", 0.8f);
            } else {
                boolean trigger = !isStunned() && (Gdx.input.isButtonPressed(Input.Buttons.LEFT) || pad.pressed(Button.RB));
                if (weapon.chargesShots()) { // the Egg Lobber: hold to throw further, let go to throw
                    if (trigger && weapon.ready()) {
                        weapon.holdCharge(delta);
                    } else if (trigger || weapon.isCharging()) {
                        weapon.tryFire(world, muzzleX(), muzzleY(), aimAngle); // the throw, or reload / empty click
                    }
                } else if (trigger) {
                    weapon.tryFire(world, muzzleX(), muzzleY(), aimAngle);
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.E) || pad.justPressed(Button.A)) {
                world.interact();
            }
            boolean itemKey = Gdx.input.isKeyPressed(Input.Keys.F) || pad.pressed(Button.RT); // R2, like ETG
            if (active == Item.GRENADE && activeCooldown <= 0) { // hold to throw further, let go to throw
                if (itemKey) {
                    grenadeCharge = Math.min(GRENADE_FULL_CHARGE, Math.max(0f, grenadeCharge) + delta);
                } else if (grenadeCharge >= 0) {
                    world.useActiveItem();
                    grenadeCharge = -1f;
                }
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.F) || pad.justPressed(Button.RT)) {
                world.useActiveItem();
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R) || pad.justPressed(Button.X)) {
            weapon.startReload(world);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q) || pad.justPressed(Button.RS)) { // R3, like ETG
            world.useBlank();
        }
        for (int i = 0; i < 9; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                selectWeapon(i);
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            cycleWeapon(1);
        }
    }

    /**
     * Finds the nearest spot where the feet don't overlap anything (tables included), trying the roll's
     * direction first, and moves there. Used when a roll would otherwise end on top of a table.
     */
    private void hopToFreeSpot(GameWorld world) {
        float startX = x;
        float startY = y;
        float rollAngle = rollDir.angleDeg();
        for (float r = 0.1f; r <= 2.5f; r += 0.1f) {
            for (int i = 0; i < 16; i++) {
                // 0, +22.5, -22.5, +45, -45... degrees around the roll direction
                float a = rollAngle + (i % 2 == 0 ? 1 : -1) * ((i + 1) / 2) * 22.5f;
                x = startX + MathUtils.cosDeg(a) * r;
                y = startY + MathUtils.sinDeg(a) * r;
                feetRect(tmp);
                if (Arena.now.contains(tmp.x + tmp.width / 2f, tmp.y, 0.1f) && world.findSolid(tmp, false) == null) {
                    return;
                }
            }
        }
        x = rollStartX; // nowhere free nearby (shouldn't happen): back to where the roll started
        y = rollStartY;
    }

    /** Like ETG, the roll follows the movement input; only standing still rolls toward the aim. */
    private void aimRoll(Gamepad pad) {
        if (!moveDir.isZero()) {
            rollDir.set(moveDir).nor();
        } else if (pad.leftStickRaw().len() > ROLL_STICK_MIN) {
            rollDir.set(pad.leftStickRaw()).nor();
        } else if (sinceMoved < ROLL_DIR_MEMORY) {
            rollDir.set(lastMoveDir);
        } else {
            rollDir.set(MathUtils.cosDeg(aimAngle), MathUtils.sinDeg(aimAngle));
        }
    }

    // ------------------------------------------------------------ draw

    public boolean isDeathAnimationDone() {
        return dead && deathTime >= DEATH_DONE;
    }

    private TextureRegion frame() {
        if (dead) {
            for (int i = 0; i < DEATH_FRAMES.length; i++) {
                if (deathTime < DEATH_FRAMES[i]) {
                    return frames[3][i];
                }
            }
            return deadFrame;
        }
        if (isRolling()) {
            return frames[2][0];
        }
        int row = MathUtils.sinDeg(aimAngle) > 0.35f ? 1 : 0; // aiming up -> show his back
        if (moveDir.isZero()) {
            return frames[row][4 + ((int) (animTime * 2f)) % 2];
        }
        return frames[row][((int) (animTime * 10f)) % 4];
    }

    public void draw(SpriteBatch batch, Draw d) {
        float fall = fallTimer > 0 ? fallTimer / FALL_TIME : 1f; // 1 -> 0 while falling into a pit
        if (fall >= 1f) {
            d.ellipse(batch, centerX(), feetY() + 0.03f, 0.2f, 0.06f, Color.BLACK, 0.35f);
        }

        boolean gunBehind = MathUtils.sinDeg(aimAngle) > 0.35f;
        if (!dead && !isRolling() && fall >= 1f && gunBehind) {
            drawGun(batch, d);
        }

        boolean blink = !dead && invulnerable > 0 && ((int) (invulnerable * 16)) % 2 == 0;
        batch.setColor(1f, 1f, 1f, blink ? 0.3f : 1f);
        TextureRegion r = frame();
        boolean flip = !facingRight;
        float rotation = 0;
        if (dead) {
            rotation = 0; // the death frames are drawn upright, even if he died mid-roll
        } else if (isRolling()) {
            float progress = 1f - rollTimer / ROLL_TIME;
            rotation = (rollDir.x >= 0 ? -1 : 1) * 360f * progress;
            flip = rollDir.x < 0;
        }
        float sw = Sprites.w(r);
        float sh = Sprites.h(r);
        // lying flat he falls backwards: head away from where he was facing
        boolean lying = r == deadFrame;
        if (lying) {
            flip = facingRight;
        }
        float sink = (1f - fall) * 0.25f;
        float originY = fall < 1f ? 0f : sh / 2f; // shrink into the pit at the feet; rolls spin around the middle
        batch.draw(r.getTexture(), centerX() - sw / 2f, y - sink, sw / 2f, originY, sw, sh, fall, fall,
                rotation + (1f - fall) * 200f,
                r.getRegionX(), r.getRegionY(), r.getRegionWidth(), r.getRegionHeight(), flip, false);
        batch.setColor(Color.WHITE);

        if (!dead && !isRolling() && fall >= 1f && !gunBehind) {
            drawGun(batch, d);
        }

        if (helmetOn && !dead && !isRolling() && fall >= 1f) { // a shiny red bike helmet on top of the head
            float hx = centerX() + (facingRight ? -0.01f : 0.01f);
            float hy = y + HEIGHT - 0.1f;
            d.ellipse(batch, hx, hy, 0.17f, 0.1f, OUTLINE, 1f);
            d.ellipse(batch, hx, hy, 0.15f, 0.085f, HELMET, 1f);
            d.rect(batch, hx - 0.15f, hy - 0.03f, 0.3f, 0.03f, OUTLINE);
            d.ellipse(batch, hx + (facingRight ? 0.05f : -0.05f), hy + 0.035f, 0.05f, 0.025f, HELMET_SHINE, 1f);
        }
        if (isBurning()) { // flames licking up all over him
            for (int i = 0; i < 5; i++) {
                float fx = centerX() - 0.16f + i * 0.08f;
                float flick = 0.5f + 0.5f * MathUtils.sin(animTime * 15f + i * 2.1f);
                float h = 0.08f + 0.08f * flick;
                float fy = y + 0.12f + (i % 2) * 0.18f + h * 0.5f;
                d.ellipse(batch, fx, fy, 0.045f, h, FLAME, 0.85f);
                d.ellipse(batch, fx, fy - h * 0.2f, 0.022f, h * 0.55f, FLAME_CORE, 0.9f);
            }
        }
        if (isStunned()) { // stars circling his head
            for (int i = 0; i < 3; i++) {
                float a = animTime * 400f + i * 120f;
                d.circle(batch, centerX() + MathUtils.cosDeg(a) * 0.16f, y + HEIGHT + 0.05f + MathUtils.sinDeg(a) * 0.04f,
                        0.03f, STUN_STAR);
            }
        }
        Weapon w = currentWeapon();
        if (!dead && w.isReloading()) {
            float bw = 0.5f;
            float bx = centerX() - bw / 2f;
            float by = y + HEIGHT + 0.08f;
            d.rect(batch, bx - 0.015f, by - 0.015f, bw + 0.03f, 0.08f, RELOAD_BG);
            d.rect(batch, bx, by, bw * w.reloadProgress(), 0.05f, RELOAD_FG);
        }
    }

    private void drawGun(SpriteBatch batch, Draw d) {
        Weapon w = currentWeapon();
        float sx = handX();
        float sy = handY();
        gunSprite().drawHeld(batch, sx, sy, aimAngle, MathUtils.cosDeg(aimAngle) < 0);
        // the floating hand (pixel-sized square with outline)
        float hs = 3f / Sprites.PPU;
        d.rect(batch, sx - hs / 2f - 1f / Sprites.PPU, sy - hs / 2f - 1f / Sprites.PPU, hs + 2f / Sprites.PPU,
                hs + 2f / Sprites.PPU, OUTLINE);
        d.rect(batch, sx - hs / 2f, sy - hs / 2f, hs, hs, SKIN);
        if (w.getMuzzleFlash() > 0) {
            d.circle(batch, muzzleX(), muzzleY(), 0.08f, Color.YELLOW, 0.9f);
            d.circle(batch, muzzleX(), muzzleY(), 0.045f, Color.WHITE);
        }
    }
}
