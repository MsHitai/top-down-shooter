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
import com.trush.game.gfx.Draw;
import com.trush.game.gfx.Sprites;
import com.trush.game.input.Gamepad;
import com.trush.game.launcher.Dev;
import com.trush.game.input.Gamepad.Button;
import com.trush.game.objects.Prop;
import com.trush.game.objects.Weapon;
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
    private static final Color OUTLINE = Color.valueOf("1e1416");
    private static final Color RELOAD_BG = Color.valueOf("1b1b1b");
    private static final Color RELOAD_FG = Color.valueOf("f5f5f5");
    private static final Color DUST = Color.valueOf("c8b49a");

    private final Sprites sprites;
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
    private boolean dead;
    private float deathTime;
    /** Falling into a pit: shrinks out of sight, then GameWorld puts him back at the last safe spot. */
    private float fallTimer;

    private float invulnerable;
    private float rollTimer;
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
        this.sprites = sprites;
        weapons.add(Weapon.rustySidearm());
        if (Dev.flag("allGuns")) { // dev builds: -Dfluffy.allGuns=true
            weapons.addAll(Weapon.chestGuns());
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

    public void addWeapon(Weapon w) {
        weapons.add(w);
        selectWeapon(weapons.size - 1);
    }

    public boolean heal(int halfHearts) {
        if (health >= maxHealth) {
            return false;
        }
        health = Math.min(maxHealth, health + halfHearts);
        return true;
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
        boolean overTables = isRolling();
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
        rollBuffer -= delta;
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)
                || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) || pad.justPressed(Button.LB)) {
            rollBuffer = ROLL_BUFFER;
        }

        Weapon weapon = currentWeapon();
        weapon.update(delta);

        if (isRolling()) {
            rollTimer -= delta;
            float progress = 1f - Math.max(0, rollTimer) / ROLL_TIME;
            float k = 1f - 0.65f * progress;
            nudge(rollDir.x * ROLL_SPEED * k * delta, rollDir.y * ROLL_SPEED * k * delta, world);
            if (MathUtils.randomBoolean(0.5f)) {
                world.particles().spawn(centerX(), feetY() + 0.03f, -rollDir.x * 0.5f, 0.2f, 0.35f, 0.04f, DUST, 3f);
            }
            boolean onTable = world.rollOverTables(feetRect(tmp), rolledOver);
            if (rollTimer <= 0 && onTable) {
                // never stop on top of a table: keep sliding until clear (or, if something blocks the
                // far side, go back to where the roll started)
                rollOvertime += delta;
                if (rollOvertime < 0.35f) {
                    rollTimer = 0.0001f;
                } else {
                    x = rollStartX;
                    y = rollStartY;
                }
            }
            if (rollTimer <= 0) {
                rollCooldown = ROLL_COOLDOWN;
            }
        } else {
            nudge(moveDir.x * speed * delta, moveDir.y * speed * delta, world);

            if (rollBuffer > 0 && rollCooldown <= 0) {
                rollBuffer = 0;
                aimRoll(pad);
                rollTimer = ROLL_TIME;
                rolledOver.clear();
                rollOvertime = 0;
                rollStartX = x;
                rollStartY = y;
                world.sfx().play("dodge", 0.8f);
            } else if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) || pad.pressed(Button.RB)) {
                weapon.tryFire(world, muzzleX(), muzzleY(), aimAngle);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.F)
                    || pad.justPressed(Button.A)) {
                world.interact();
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
                    return sprites.hero[3][i];
                }
            }
            return sprites.heroDead;
        }
        if (isRolling()) {
            return sprites.hero[2][0];
        }
        int row = MathUtils.sinDeg(aimAngle) > 0.35f ? 1 : 0; // aiming up -> show his back
        if (moveDir.isZero()) {
            return sprites.hero[row][4 + ((int) (animTime * 2f)) % 2];
        }
        return sprites.hero[row][((int) (animTime * 10f)) % 4];
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
        boolean lying = r == sprites.heroDead;
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
