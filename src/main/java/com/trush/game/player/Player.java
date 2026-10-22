package com.trush.game.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Weapon;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import lombok.Getter;

@Getter
public class Player {

    public static final float HEIGHT = 0.8f;
    public static final float HITBOX_RADIUS = 0.1f;

    private static final float ROLL_TIME = 0.42f;
    private static final float ROLL_INVULNERABLE_TIME = 0.32f;
    private static final float ROLL_SPEED = 5.2f;
    private static final float ROLL_COOLDOWN = 0.18f;
    private static final float HURT_INVULNERABLE_TIME = 1.2f;
    /** Dev/testing only: java -Dfluffy.god=true ... - hits register but never cost health. */
    private static final boolean GOD_MODE = Boolean.getBoolean("fluffy.god");

    private static final Color SKIN = Color.valueOf("d9a066");
    private static final Color RELOAD_BG = Color.valueOf("1b1b1b");
    private static final Color RELOAD_FG = Color.valueOf("f5f5f5");
    private static final Color DUST = Color.valueOf("c8b49a");

    private final Texture texture;
    private final Sprite playerSprite;
    private final Rectangle boundingBox;
    private final Vector2 moveDir = new Vector2();
    private final Vector2 rollDir = new Vector2();
    private final Array<Weapon> weapons = new Array<>();
    private int weaponIndex;

    private final float speed = 2.6f;
    private int maxHealth = 6; // in half hearts: 6 = 3 hearts
    private int health = 6;
    private int blanks = 2;
    private boolean dead;

    private float invulnerable;
    private float rollTimer;
    private float rollCooldown;
    private float walkTime;
    private float aimAngle;
    private boolean facingRight = true;

    public Player(float x, float y) {
        texture = new Texture("assets/player.png");
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        playerSprite = new Sprite(texture);
        playerSprite.setSize(HEIGHT * texture.getWidth() / texture.getHeight(), HEIGHT);
        playerSprite.setPosition(x, y);
        boundingBox = new Rectangle(x, y, playerSprite.getWidth(), playerSprite.getHeight());
        weapons.add(Weapon.rustySidearm());
        if (Boolean.getBoolean("fluffy.allGuns")) { // dev/testing: java -Dfluffy.allGuns=true ...
            weapons.add(Weapon.fluffShotgun());
            weapons.add(Weapon.pocketUzi());
            weapons.add(Weapon.bigMagnum());
        }
    }

    // ------------------------------------------------------------ geometry

    public float centerX() {
        return playerSprite.getX() + playerSprite.getWidth() / 2f;
    }

    /** Hitbox is small and sits around the belly, like in ETG - bullets grazing the head don't count. */
    public float hitboxY() {
        return playerSprite.getY() + HEIGHT * 0.33f;
    }

    public float feetY() {
        return playerSprite.getY();
    }

    private float pivotY() {
        return playerSprite.getY() + HEIGHT * 0.47f;
    }

    private float gunStart() {
        return 0.09f;
    }

    public float muzzleX() {
        return centerX() + MathUtils.cosDeg(aimAngle) * (gunStart() + currentWeapon().getGunLength());
    }

    public float muzzleY() {
        return pivotY() + MathUtils.sinDeg(aimAngle) * (gunStart() + currentWeapon().getGunLength());
    }

    // ------------------------------------------------------------ state

    public Weapon currentWeapon() {
        return weapons.get(weaponIndex);
    }

    public boolean isRolling() {
        return rollTimer > 0;
    }

    public boolean isInvulnerable() {
        return invulnerable > 0 || (isRolling() && ROLL_TIME - rollTimer < ROLL_INVULNERABLE_TIME);
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

    // ------------------------------------------------------------ update

    public void update(float delta, GameWorld world) {
        if (dead) {
            return;
        }
        invulnerable -= delta;
        rollCooldown -= delta;

        Vector2 aim = world.getAim();
        aimAngle = MathUtils.atan2Deg360(aim.y - pivotY(), aim.x - centerX());
        facingRight = aim.x >= centerX();

        moveDir.setZero();
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) moveDir.x += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) moveDir.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) moveDir.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) moveDir.y -= 1;
        moveDir.nor();

        Weapon weapon = currentWeapon();
        weapon.update(delta);

        if (isRolling()) {
            rollTimer -= delta;
            float progress = 1f - Math.max(0, rollTimer) / ROLL_TIME;
            float k = 1f - 0.65f * progress;
            playerSprite.translate(rollDir.x * ROLL_SPEED * k * delta, rollDir.y * ROLL_SPEED * k * delta);
            if (MathUtils.randomBoolean(0.5f)) {
                world.particles().spawn(centerX(), feetY() + 0.03f, -rollDir.x * 0.5f, 0.2f, 0.35f, 0.04f, DUST, 3f);
            }
            if (rollTimer <= 0) {
                rollCooldown = ROLL_COOLDOWN;
            }
        } else {
            playerSprite.translate(moveDir.x * speed * delta, moveDir.y * speed * delta);
            if (!moveDir.isZero()) {
                walkTime += delta;
            }

            boolean rollPressed = Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                    || Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)
                    || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT);
            if (rollPressed && rollCooldown <= 0) {
                if (moveDir.isZero()) {
                    rollDir.set(MathUtils.cosDeg(aimAngle), MathUtils.sinDeg(aimAngle));
                } else {
                    rollDir.set(moveDir);
                }
                rollTimer = ROLL_TIME;
                world.sfx().play("dodge", 0.8f);
            } else if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                weapon.tryFire(world, muzzleX(), muzzleY(), aimAngle);
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            weapon.startReload(world);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            world.useBlank();
        }
        for (int i = 0; i < 9; i++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1 + i)) {
                selectWeapon(i);
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            cycleWeapon(1);
        }

        restrictMovement();
    }

    /** Clamps the sprite to the room walls (acts on sprite bounds, see AGENTS.md). */
    private void restrictMovement() {
        float x = MathUtils.clamp(playerSprite.getX(), Arena.LEFT, Arena.RIGHT - playerSprite.getWidth());
        float y = MathUtils.clamp(playerSprite.getY(), Arena.BOTTOM, Arena.TOP - playerSprite.getHeight());
        playerSprite.setPosition(x, y);
        boundingBox.setPosition(x, y);
    }

    // ------------------------------------------------------------ draw

    public void draw(SpriteBatch batch, Draw d) {
        d.ellipse(batch, centerX(), feetY() + 0.02f, 0.2f, 0.06f, Color.BLACK, 0.35f);

        boolean gunBehind = MathUtils.sinDeg(aimAngle) > 0.35f;
        if (!dead && !isRolling() && gunBehind) {
            drawGun(batch, d);
        }

        playerSprite.setFlip(!facingRight, false);
        playerSprite.setOriginCenter();
        if (dead) {
            playerSprite.setRotation(facingRight ? 90 : -90);
            playerSprite.setScale(1f);
        } else if (isRolling()) {
            float progress = 1f - rollTimer / ROLL_TIME;
            playerSprite.setRotation((rollDir.x >= 0 ? -1 : 1) * 360f * progress);
            playerSprite.setScale(0.9f, 0.8f);
        } else {
            playerSprite.setRotation(moveDir.isZero() ? 0 : MathUtils.sin(walkTime * 16f) * 4f);
            playerSprite.setScale(1f);
        }

        boolean blink = invulnerable > 0 && ((int) (invulnerable * 16)) % 2 == 0;
        playerSprite.setAlpha(blink ? 0.3f : 1f);
        playerSprite.draw(batch);
        playerSprite.setAlpha(1f);

        if (!dead && !isRolling() && !gunBehind) {
            drawGun(batch, d);
        }

        Weapon w = currentWeapon();
        if (!dead && w.isReloading()) {
            float bw = 0.5f;
            float bx = centerX() - bw / 2f;
            float by = playerSprite.getY() + HEIGHT + 0.08f;
            d.rect(batch, bx - 0.015f, by - 0.015f, bw + 0.03f, 0.08f, RELOAD_BG);
            d.rect(batch, bx, by, bw * w.reloadProgress(), 0.05f, RELOAD_FG);
        }
    }

    private void drawGun(SpriteBatch batch, Draw d) {
        Weapon w = currentWeapon();
        float cos = MathUtils.cosDeg(aimAngle);
        float sin = MathUtils.sinDeg(aimAngle);
        float sx = centerX() + cos * gunStart();
        float sy = pivotY() + sin * gunStart();
        d.line(batch, sx, sy, w.getGunLength(), w.getGunThickness(), aimAngle, w.getGunColor());
        d.circle(batch, sx, sy, 0.045f, SKIN);
        if (w.getMuzzleFlash() > 0) {
            d.circle(batch, muzzleX(), muzzleY(), 0.08f, Color.YELLOW, 0.9f);
            d.circle(batch, muzzleX(), muzzleY(), 0.045f, Color.WHITE);
        }
    }

    public void dispose() {
        texture.dispose();
    }
}
