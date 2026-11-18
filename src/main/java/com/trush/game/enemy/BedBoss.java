package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/** Each resident of the floor has three attacks with a visible wind-up and a recovery. */
final class BedBoss extends Enemy {
    private enum Phase { RECOVER, WARN, DASH }

    private static final Color DUST = Color.valueOf("a798ac");
    private static final Color SHEET = Color.valueOf("e8d9e6");
    private static final Color SOCK = Color.valueOf("544989");
    private static final Color YARN = Color.valueOf("d3638c");
    private static final Color TIN = Color.valueOf("b7ab76");
    private static final Color LINT = Color.valueOf("8485ae");
    private static final Color TEETH = Color.valueOf("fff2d2");
    private static final float DASH_LENGTH = 3.7f;

    private final Boss kind;
    private final Color color;
    private final Vector2 mark = new Vector2();
    private Bullet puff;
    private float puffTimer;
    private Phase phase = Phase.RECOVER;
    private float timer = 1.25f;
    private float angle;
    private float remaining;
    private float wave;
    private int attack = -1;
    private boolean angry;

    BedBoss(float x, float y, Boss kind) {
        super(x, y, kind == Boss.DUST_BUNNY ? 410f : 560f);
        this.kind = kind;
        color = switch (kind) {
            case DUST_BUNNY -> DUST;
            case BED_MONSTER -> SHEET;
            case SOCK_NINJA -> SOCK;
            case YARN_HYDRA -> YARN;
            case WIND_UP_GENERAL -> TIN;
            default -> LINT;
        };
        radius = 0.53f;
        boss = true;
        score = 1250;
    }

    @Override
    public String bossName() {
        return kind.title;
    }

    @Override
    public boolean isEnraged() {
        return angry;
    }

    @Override
    protected boolean isDodging() {
        return kind == Boss.DUST_BUNNY && angry && attack == 2 && phase == Phase.WARN;
    }

    @Override
    public void hit(float damage, Vector2 bulletVel, float knockback, GameWorld world) {
        super.hit(damage, bulletVel, 0f, world);
    }

    @Override
    public float feetY() {
        return pos.y - radius * 0.6f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        angry = hp < maxHp * 0.5f;
        updateProjectiles(delta, world);
        timer -= delta;
        switch (phase) {
            case RECOVER:
                if (kind != Boss.WIND_UP_GENERAL || timer > 0.75f) keepDistance(delta, world, 2.9f, 0.9f);
                if (timer <= 0) {
                    phase = Phase.WARN;
                    attack = (attack + 1) % 3;
                    timer = 0.85f;
                    angle = aimAngle;
                    mark.set(targetX(world), targetY(world));
                    Arena.now.clamp(mark, radius + 0.05f);
                }
                break;
            case WARN:
                if (timer <= 0) strike(world);
                break;
            case DASH:
                float distance = Math.min(remaining, 5.8f * delta);
                float nextX = pos.x + MathUtils.cosDeg(angle) * distance;
                float nextY = pos.y + MathUtils.sinDeg(angle) * distance;
                if (world.walkable(pos.x, pos.y, nextX, nextY, radius)) {
                    pos.set(nextX, nextY);
                    remaining -= distance;
                } else {
                    remaining = 0;
                }
                if (remaining <= 0) {
                    fan(world, pos.x, pos.y, angle, angry ? 9 : 7, 15, Bullet.Shape.DIAMOND);
                    recover();
                }
                break;
        }
    }

    private void updateProjectiles(float delta, GameWorld world) {
        if (puff != null) {
            puffTimer -= delta;
            if (puffTimer <= 0 && puff.alive) {
                fan(world, puff.pos.x, puff.pos.y, angle, 12, 30f, Bullet.Shape.CLOUD);
                puff.alive = false;
            }
            if (!puff.alive) puff = null;
        }
        if (wave <= 0) return;
        float next = wave + delta * 2.9f;
        if (!world.getPlayer().getHero().floats()) {
            float distance = Vector2.dst(mark.x, mark.y, world.getPlayer().feetX(), world.getPlayer().feetY());
            if (distance >= wave - 0.09f && distance <= next + 0.09f) world.hurtPlayer();
        }
        wave = next > 4.3f ? 0f : next;
    }

    private boolean dashes() {
        return attack == 0 && (kind == Boss.DUST_BUNNY || kind == Boss.SOCK_NINJA || kind == Boss.WIND_UP_GENERAL);
    }

    private void strike(GameWorld world) {
        world.sfx().play("enemy_shoot", 0.65f);
        if (dashes()) {
            phase = Phase.DASH;
            remaining = DASH_LENGTH;
            return;
        }
        switch (kind) {
            case DUST_BUNNY -> {
                if (attack == 1) {
                    puff = shot(world, pos.x, pos.y, angle, 2.2f, Bullet.Shape.CLOUD);
                    puffTimer = 0.85f;
                    fan(world, pos.x, pos.y, angle, 6, 18f, Bullet.Shape.DIAMOND);
                } else {
                    if (angry) pos.set(mark);
                    fan(world, pos.x, pos.y, angle, 14, 360f / 14f, Bullet.Shape.CLOUD);
                    if (world.getEnemies().size + world.getPendingEnemies().size < 3) {
                        world.spawnEnemy(new BedCritter(pos.x, pos.y, BedCritter.Kind.DUST_KIT));
                    }
                }
            }
            case BED_MONSTER -> {
                if (attack == 2) fan(world, mark.x, mark.y, angle, 15, 24f, Bullet.Shape.BALL);
                else {
                    float side = attack == 0 ? -1 : 1;
                    float x = pos.x + side * 0.74f;
                    fan(world, x, pos.y - 0.2f, angle + side * 22f, angry ? 11 : 8, 13f, Bullet.Shape.BALL);
                }
                world.shake(0.28f);
            }
            case SOCK_NINJA -> {
                float left = Math.max(Arena.now.left + 0.5f, pos.x - 1.15f);
                float right = Math.min(Arena.now.right - 0.5f, pos.x + 1.15f);
                fan(world, pos.x, pos.y, angle, 7, 13f, Bullet.Shape.DIAMOND);
                if (attack == 2) {
                    fan(world, left, pos.y, angle - 18f, 4, 12f, Bullet.Shape.DIAMOND);
                    fan(world, right, pos.y, angle + 18f, 4, 12f, Bullet.Shape.DIAMOND);
                }
            }
            case YARN_HYDRA -> {
                for (int lane = -1; lane <= 1; lane++) {
                    float x = pos.x + lane * 0.55f;
                    for (int bead = 0; bead < 4; bead++) {
                        shot(world, x, pos.y, angle + lane * 27f, 2.1f + bead * 0.7f, Bullet.Shape.BALL);
                    }
                }
                if (attack == 2) fan(world, pos.x, pos.y, angle + 14f, 12, 30f, Bullet.Shape.BALL);
            }
            case WIND_UP_GENERAL -> {
                int count = attack == 1 ? 5 : 9;
                for (int i = 0; i < count; i++) {
                    shot(world, pos.x, pos.y, angle + (i - (count - 1) / 2f) * 16f, 4.2f, Bullet.Shape.BALL).bounces = 1;
                }
            }
            case LINT_REGENT -> {
                if (attack == 0) {
                    mark.set(pos);
                    wave = 0.18f;
                } else {
                    fan(world, pos.x, pos.y, angle, attack == 1 ? 9 : 15, attack == 1 ? 14f : 24f, Bullet.Shape.BALL);
                }
            }
            default -> throw new IllegalStateException(kind.name());
        }
        recover();
    }

    private void recover() {
        phase = Phase.RECOVER;
        timer = kind == Boss.WIND_UP_GENERAL ? 1.35f : angry ? 0.75f : 1.15f;
    }

    private void fan(GameWorld world, float x, float y, float center, int count, float spread, Bullet.Shape shape) {
        for (int i = 0; i < count; i++) {
            shot(world, x, y, center + (i - (count - 1) / 2f) * spread, 3.2f, shape);
        }
    }

    private Bullet shot(GameWorld world, float x, float y, float direction, float speed, Bullet.Shape shape) {
        Bullet b = world.spawnEnemyBullet(x, y, direction, speed * world.getDifficulty());
        b.shape = shape;
        b.color.set(color).lerp(Color.WHITE, 0.30f);
        return b;
    }

    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (!alive) return;
        if (wave > 0) d.ring(b, mark.x, mark.y, wave, Color.GOLD, 0.8f);
        if (phase != Phase.WARN) return;
        if (dashes()) {
            d.line(b, pos.x, pos.y, DASH_LENGTH, 0.045f, angle, TELEGRAPH, 0.85f);
        } else if (kind == Boss.YARN_HYDRA) {
            for (int lane = -1; lane <= 1; lane++) {
                d.line(b, pos.x + lane * 0.55f, pos.y, 7f, 0.045f, angle + lane * 27f, YARN, 0.9f);
            }
        } else if (kind == Boss.SOCK_NINJA && attack == 2) {
            sock(b, d, Math.max(Arena.now.left + 0.5f, pos.x - 1.15f), pos.y, 0.35f);
            sock(b, d, Math.min(Arena.now.right - 0.5f, pos.x + 1.15f), pos.y, 0.35f);
        } else if ((kind == Boss.BED_MONSTER && attack == 2) || isDodging()) {
            d.ring(b, mark.x, mark.y, 0.7f, TELEGRAPH, 0.9f);
        } else {
            d.ring(b, pos.x, pos.y, 0.75f, TELEGRAPH, 0.65f);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float x = pos.x, y = pos.y;
        switch (kind) {
            case DUST_BUNNY -> {
                if (isDodging()) {
                    d.ellipse(b, x, y, 0.55f, 0.14f, DUST, 0.8f);
                    return;
                }
                d.rotRect(b, x - 0.30f, y + 0.56f, 0.24f, 0.83f, 20f, DUST, 1f);
                d.rotRect(b, x + 0.27f, y + 0.60f, 0.24f, 0.83f, -13f, DUST, 1f);
                for (int i = 0; i < 11; i++) {
                    float a = 360f * i / 11;
                    d.circle(b, x + 0.43f * MathUtils.cosDeg(a), y + 0.31f * MathUtils.sinDeg(a),
                            0.23f + i % 3 * 0.04f, DUST);
                }
                d.ellipse(b, x, y, 0.55f, 0.39f, DUST, 1f);
                d.ellipse(b, x, y - 0.20f, 0.32f, 0.17f, FEET, 1f);
                for (int i = -2; i <= 2; i++) d.rect(b, x + i * 0.095f - 0.025f, y - 0.23f, 0.05f, 0.11f, TEETH);
                for (int i = -1; i <= 1; i++) {
                    d.line(b, x - 0.55f + i * 0.05f, y - 0.22f, 0.17f, 0.035f, 235f, TEETH);
                    d.line(b, x + 0.55f + i * 0.05f, y - 0.22f, 0.17f, 0.035f, 305f, TEETH);
                }
            }
            case BED_MONSTER -> {
                d.ellipse(b, x, y + 0.08f, 0.6f, 0.64f, SHEET, 1f);
                for (int i = -1; i <= 1; i += 2) {
                    float reach = phase == Phase.WARN && (attack == 2 || attack == (i + 1) / 2) ? 0.32f : 0f;
                    d.rotRect(b, x + i * (0.64f + reach / 2f), y - 0.19f, 0.58f + reach, 0.18f, -i * 18f, SHEET, 1f);
                    d.circle(b, x + i * (0.84f + reach), y - 0.29f, 0.15f, SHEET);
                }
                d.ellipse(b, x, y, 0.37f, 0.21f, FEET, 1f);
            }
            case SOCK_NINJA -> sock(b, d, x, y, 1f);
            case YARN_HYDRA -> {
                d.circle(b, x, y - 0.10f, 0.48f, YARN);
                for (int i = -1; i <= 1; i++) {
                    float headX = x + i * 0.55f;
                    d.line(b, x + i * 0.07f, y - 0.09f, 0.57f, 0.16f, 90f - i * 44f, YARN);
                    d.circle(b, headX, y + 0.39f - Math.abs(i) * 0.13f, 0.23f, YARN);
                    drawEyes(b, d, headX, y + 0.41f - Math.abs(i) * 0.13f, 0.09f, 0.065f);
                }
                for (int i = -2; i <= 2; i++) d.line(b, x - 0.32f, y - 0.12f + i * 0.09f, 0.61f, 0.025f, i * 12f, TEETH, 0.55f);
                return;
            }
            case WIND_UP_GENERAL -> {
                for (int side = -1; side <= 1; side += 2) {
                    d.rect(b, x + side * 0.47f - 0.12f, y - 0.44f, 0.24f, 0.7f, FEET);
                    for (int i = 0; i < 6; i++) d.rect(b, x + side * 0.47f - 0.10f, y - 0.40f + i * 0.11f, 0.20f, 0.035f, TIN);
                }
                d.rect(b, x - 0.39f, y - 0.35f, 0.78f, 0.58f, TIN);
                d.firearm(b, x, y - 0.11f, 0.60f, 0.14f, aimAngle, TIN);
                d.circle(b, x, y + 0.30f, 0.27f, TIN);
                d.ring(b, x + 0.27f, y + 0.57f, 0.115f, Color.GOLD, 1f);
                d.ring(b, x + 0.44f, y + 0.57f, 0.115f, Color.GOLD, 1f);
            }
            case LINT_REGENT -> {
                d.rect(b, x - 0.54f, y - 0.46f, 1.08f, 1.15f, YARN);
                d.rect(b, x - 0.47f, y - 0.41f, 0.94f, 0.17f, Color.GOLD);
                d.ellipse(b, x, y - 0.02f, 0.40f, 0.47f, LINT, 1f);
                d.rect(b, x - 0.25f, y + 0.35f, 0.5f, 0.13f, Color.GOLD);
                for (int i = -1; i <= 1; i++) d.rect(b, x + i * 0.17f - 0.045f, y + 0.44f, 0.09f, 0.15f, Color.GOLD);
                d.rotRect(b, x + 0.45f, y, 0.12f, 0.75f, -18f, TIN, 1f);
                d.circle(b, x + 0.57f, y + 0.34f, 0.17f, TIN);
            }
            default -> throw new IllegalStateException(kind.name());
        }
        drawEyes(b, d, x, kind == Boss.WIND_UP_GENERAL ? y + 0.29f : y + 0.07f, 0.16f, 0.10f);
    }

    private void sock(SpriteBatch b, Draw d, float x, float y, float alpha) {
        d.rotRect(b, x, y + 0.10f, 0.52f, 1.05f, 14f, SOCK, alpha);
        d.ellipse(b, x + 0.20f, y - 0.31f, 0.40f, 0.24f, SOCK, alpha);
        d.rotRect(b, x - 0.10f, y + 0.37f, 0.51f, 0.11f, 14f, TEETH, alpha);
        if (phase == Phase.WARN && alpha >= 1f) {
            d.rotRect(b, x - 0.13f, y + 0.49f, 0.50f, 0.085f, 14f, Color.GOLD, 1f);
        }
    }
}
