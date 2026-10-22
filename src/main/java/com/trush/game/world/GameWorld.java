package com.trush.game.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.audio.Sfx;
import com.trush.game.effects.Particles;
import com.trush.game.enemy.Enemy;
import com.trush.game.launcher.ETGFanfic;
import com.trush.game.objects.Bullet;
import com.trush.game.objects.Pickup;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Player;
import lombok.Getter;

/**
 * Owns every entity in the room and runs the simulation: movement, bullets, collisions,
 * pickups, effects. The screen feeds it the aim position and draws the HUD on top.
 */
@Getter
public class GameWorld {

    private static final Color ENEMY_BULLET = Color.valueOf("ff5e3a");
    private static final Color SPARK = Color.valueOf("fff3b0");
    private static final Color BLOOD = Color.valueOf("e43b44");
    private static final Color BRASS = Color.valueOf("d9b24c");
    private static final Color SMOKE = Color.valueOf("b8b8b8");
    private static final Color BLANK_BLUE = Color.valueOf("a8d8ff");

    private final ETGFanfic game;
    private final Player player;
    private final Array<Enemy> enemies = new Array<>();
    private final Array<Enemy> pendingEnemies = new Array<>();
    private final Array<Bullet> playerBullets = new Array<>(false, 128);
    private final Array<Bullet> enemyBullets = new Array<>(false, 512);
    private final Array<Pickup> pickups = new Array<>();
    private final Particles particles = new Particles();
    private final WaveDirector waves;
    private final Vector2 aim = new Vector2();

    private float trauma;
    private float blankRing = -1f;
    private final Vector2 blankCenter = new Vector2();
    private int score;
    private int kills;
    private float difficulty = 1f;

    private String toast;
    private float toastTimer;

    public GameWorld(ETGFanfic game) {
        this.game = game;
        this.player = new Player(Arena.CENTER_X - 0.17f, Arena.BOTTOM + 0.4f);
        this.waves = new WaveDirector();
    }

    public Sfx sfx() {
        return game.sfx;
    }

    public Particles particles() {
        return particles;
    }

    public void setDifficulty(float difficulty) {
        this.difficulty = difficulty;
    }

    public void addScore(int amount) {
        score += amount;
    }

    public void shake(float amount) {
        trauma = Math.min(1f, trauma + amount);
    }

    public void toast(String text) {
        toast = text;
        toastTimer = 2.2f;
    }

    // ------------------------------------------------------------ spawning

    public void addPlayerBullet(Bullet b) {
        playerBullets.add(b);
    }

    public Bullet spawnEnemyBullet(float x, float y, float angle, float speed) {
        Bullet b = new Bullet(false, x, y, angle, speed, 0.065f, 1f, ENEMY_BULLET);
        b.life = 10f;
        enemyBullets.add(b);
        return b;
    }

    public void spawnEnemy(Enemy e) {
        Arena.clamp(e.pos, e.radius);
        pendingEnemies.add(e);
        sfx().play("spawn", 0.5f);
    }

    public void spawnPickup(Pickup p) {
        Arena.clamp(p.pos, 0.25f);
        pickups.add(p);
    }

    // ------------------------------------------------------------ update

    public void update(float delta) {
        player.update(delta, this);
        waves.update(delta, this);

        for (Enemy e : enemies) {
            e.update(delta, this);
        }
        enemies.addAll(pendingEnemies);
        pendingEnemies.clear();
        separateEnemies();

        updatePlayerBullets(delta);
        updateEnemyBullets(delta);
        checkContactDamage();

        for (int i = enemies.size - 1; i >= 0; i--) {
            if (!enemies.get(i).alive) {
                enemies.removeIndex(i);
            }
        }

        for (int i = pickups.size - 1; i >= 0; i--) {
            Pickup p = pickups.get(i);
            p.update(delta);
            if (!player.isDead() && Vector2.dst(p.pos.x, p.pos.y, player.centerX(), player.hitboxY()) < 0.32f) {
                collect(p);
            }
            if (!p.alive) {
                pickups.removeIndex(i);
            }
        }

        particles.update(delta);
        trauma = Math.max(0f, trauma - delta * 1.8f);
        if (blankRing >= 0) {
            blankRing += delta;
            if (blankRing > 0.5f) {
                blankRing = -1f;
            }
        }
        toastTimer -= delta;
    }

    private void separateEnemies() {
        for (int i = 0; i < enemies.size; i++) {
            Enemy a = enemies.get(i);
            if (a.isSpawning()) continue;
            for (int j = i + 1; j < enemies.size; j++) {
                Enemy b = enemies.get(j);
                if (b.isSpawning()) continue;
                float dx = b.pos.x - a.pos.x;
                float dy = b.pos.y - a.pos.y;
                float min = a.radius + b.radius;
                float d2 = dx * dx + dy * dy;
                if (d2 < min * min && d2 > 0.0001f) {
                    float d = (float) Math.sqrt(d2);
                    float push = (min - d) / 2f;
                    float wa = b.boss ? 2f : (a.boss ? 0f : 1f);
                    float wb = a.boss ? 2f : (b.boss ? 0f : 1f);
                    a.pos.sub(dx / d * push * wa, dy / d * push * wa);
                    b.pos.add(dx / d * push * wb, dy / d * push * wb);
                }
            }
        }
    }

    private void updatePlayerBullets(float delta) {
        for (int i = playerBullets.size - 1; i >= 0; i--) {
            Bullet b = playerBullets.get(i);
            b.update(delta);
            if (!Arena.contains(b.pos.x, b.pos.y, 0)) {
                b.alive = false;
                particles.burst(b.pos.x, b.pos.y, 3, 0.3f, 1f, 0.2f, 0.03f, SMOKE);
            }
            if (b.alive) {
                for (Enemy e : enemies) {
                    if (!e.alive || e.isSpawning() || b.alreadyHit.contains(e, true)) continue;
                    float r = e.radius + b.radius;
                    if (b.pos.dst2(e.pos) < r * r) {
                        e.hit(b.damage, b.vel, b.knockback, this);
                        particles.burst(b.pos.x, b.pos.y, 4, 0.8f, 2f, 0.18f, 0.03f, SPARK);
                        sfx().play("hit", 0.6f);
                        if (b.piercing) {
                            b.alreadyHit.add(e);
                        } else {
                            b.alive = false;
                            break;
                        }
                    }
                }
            }
            if (!b.alive) {
                playerBullets.removeIndex(i);
            }
        }
    }

    private void updateEnemyBullets(float delta) {
        float px = player.centerX();
        float py = player.hitboxY();
        for (int i = enemyBullets.size - 1; i >= 0; i--) {
            Bullet b = enemyBullets.get(i);
            if (!b.alive) {
                continue; // cleared by a blank / hit earlier this frame, swept below
            }
            b.update(delta);
            if (!Arena.contains(b.pos.x, b.pos.y, -0.05f)) {
                b.alive = false;
                particles.burst(b.pos.x, b.pos.y, 2, 0.2f, 0.6f, 0.2f, 0.03f, b.color);
            } else if (!player.isDead() && !player.isInvulnerable()) {
                float r = b.radius * 0.8f + Player.HITBOX_RADIUS;
                if (Vector2.dst2(b.pos.x, b.pos.y, px, py) < r * r) {
                    b.alive = false;
                    damagePlayer();
                }
            }
        }
        removeDeadEnemyBullets();
    }

    private void removeDeadEnemyBullets() {
        for (int i = enemyBullets.size - 1; i >= 0; i--) {
            if (!enemyBullets.get(i).alive) {
                enemyBullets.removeIndex(i);
            }
        }
    }

    private void checkContactDamage() {
        if (player.isDead() || player.isInvulnerable()) {
            return;
        }
        for (Enemy e : enemies) {
            if (e.isSpawning() || !e.alive) continue;
            float r = e.radius * 0.8f + Player.HITBOX_RADIUS;
            if (Vector2.dst2(e.pos.x, e.pos.y, player.centerX(), player.hitboxY()) < r * r) {
                damagePlayer();
                return;
            }
        }
    }

    private void damagePlayer() {
        if (!player.hurt(this)) {
            return;
        }
        shake(0.7f);
        particles.burst(player.centerX(), player.hitboxY(), 14, 0.8f, 2.5f, 0.4f, 0.05f, BLOOD);
        if (player.isDead()) {
            sfx().play("death", 1f);
            clearEnemyBullets(0, 0, -1f);
        } else {
            sfx().play("hurt", 1f);
            // small free "blank" around the player so a hit doesn't chain into another one
            clearEnemyBullets(player.centerX(), player.hitboxY(), 1.1f);
        }
    }

    /** Marks enemy bullets within {@code radius} (negative = all) as dead. Safe to call mid-iteration. */
    private void clearEnemyBullets(float x, float y, float radius) {
        for (Bullet b : enemyBullets) {
            if (b.alive && (radius < 0 || b.pos.dst2(x, y) < radius * radius)) {
                particles.spawn(b.pos.x, b.pos.y, 0, 0.3f, 0.3f, b.radius * 1.4f, SMOKE, 2f);
                b.alive = false;
            }
        }
    }

    public void useBlank() {
        if (player.isDead() || blankRing >= 0 || !player.consumeBlank()) {
            return;
        }
        float x = player.centerX();
        float y = player.hitboxY();
        clearEnemyBullets(x, y, -1f);
        removeDeadEnemyBullets();
        for (Enemy e : enemies) {
            if (e.boss || e.isSpawning()) continue;
            Vector2 push = new Vector2(e.pos).sub(x, y).nor().scl(2.5f);
            e.hit(0, push, 1.2f, this);
        }
        blankRing = 0f;
        blankCenter.set(x, y);
        shake(0.5f);
        sfx().play("blank", 1f);
    }

    public void onEnemyKilled(Enemy e) {
        kills++;
        score += e.score;
        int n = e.boss ? 70 : 16;
        particles.burst(e.pos.x, e.pos.y, n, 0.5f, e.boss ? 4f : 2.5f, 0.6f, e.boss ? 0.09f : 0.05f, BRASS);
        particles.burst(e.pos.x, e.pos.y, n / 2, 0.3f, 1.2f, 0.7f, 0.07f, SMOKE);
        sfx().play("enemy_die", e.boss ? 1f : 0.7f);
        shake(e.boss ? 1f : 0.15f);

        if (!e.boss) {
            float roll = MathUtils.random();
            if (roll < 0.05f && player.getHealth() < player.getMaxHealth()) {
                spawnPickup(new Pickup(Pickup.Type.HEART, e.pos.x, e.pos.y));
            } else if (roll < 0.11f && hasLimitedGunNeedingAmmo()) {
                spawnPickup(new Pickup(Pickup.Type.AMMO, e.pos.x, e.pos.y));
            }
        }
    }

    public boolean hasLimitedGunNeedingAmmo() {
        for (Weapon w : player.getWeapons()) {
            if (!w.isFullAmmo()) return true;
        }
        return false;
    }

    private void collect(Pickup p) {
        switch (p.type) {
            case HEART:
                if (!player.heal(2)) {
                    return; // leave it on the floor for later
                }
                toast("+1 HEART");
                break;
            case AMMO:
                if (!hasLimitedGunNeedingAmmo()) {
                    return;
                }
                for (Weapon w : player.getWeapons()) {
                    w.addAmmo(0.6f);
                }
                toast("AMMO REFILLED");
                break;
            case BLANK:
                player.addBlank();
                toast("+1 BLANK");
                break;
            case GUN:
                player.addWeapon(p.weapon);
                toast("GOT " + p.weapon.getName().toUpperCase() + "!");
                break;
        }
        p.alive = false;
        sfx().play("pickup", 1f);
        particles.burst(p.pos.x, p.pos.y, 10, 0.5f, 1.5f, 0.4f, 0.04f, Color.GOLD);
    }

    // ------------------------------------------------------------ draw

    public void draw(SpriteBatch batch) {
        for (Pickup p : pickups) {
            p.draw(batch, game);
        }

        enemies.sort((a, b) -> Float.compare(b.feetY(), a.feetY()));
        boolean playerDrawn = false;
        for (Enemy e : enemies) {
            if (!playerDrawn && e.feetY() < player.feetY()) {
                player.draw(batch, game.draw);
                playerDrawn = true;
            }
            e.draw(batch, game.draw);
        }
        if (!playerDrawn) {
            player.draw(batch, game.draw);
        }

        for (Bullet b : playerBullets) {
            b.draw(batch, game.draw);
        }
        for (Bullet b : enemyBullets) {
            b.draw(batch, game.draw);
        }
        particles.draw(batch, game.draw);

        if (blankRing >= 0) {
            float k = blankRing / 0.5f;
            game.draw.ring(batch, blankCenter.x, blankCenter.y, 0.3f + k * 6f, BLANK_BLUE, 1f - k);
            game.draw.circle(batch, blankCenter.x, blankCenter.y, 0.3f + k * 6f, Color.WHITE, 0.25f * (1f - k));
        }
    }

    public void dispose() {
        player.dispose();
    }
}
