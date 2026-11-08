package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * A dream boss: the Big Bad Teddy - the old teddy left at the bottom of the toy box, grown huge and grumpy,
 * stitches and button eyes. Its moves:
 * <ul>
 *   <li>HUG: arms wide, it aims (a line on the floor), then rushes along it; where it stops it claps - a
 *       ring of fluff (enraged: two hugs in a row);</li>
 *   <li>STUFFING: a seam rips and stuffing sprays out in a wide cone, slowing down and hanging in the air
 *       before it drifts on;</li>
 *   <li>NEEDLES: lines of sewing needles, one stitch after another, the second line in between the first;</li>
 *   <li>FRIENDS: it calls two of the toy box's soldiers (three when enraged) - Bullet Kin in its dream.</li>
 * </ul>
 * Below half health one button eye pops off and hangs by its thread, the stuffing pokes out, and it gets
 * properly cross.
 */
public class BigTeddy extends Enemy {

    private static final Color FUR = Color.valueOf("a8744a");
    private static final Color FUR_DARK = Color.valueOf("7e5232");
    private static final Color FUR_LIGHT = Color.valueOf("d8a878");
    private static final Color MUZZLE = Color.valueOf("f0d8b0");
    private static final Color BUTTON = Color.valueOf("3a2a4a");
    private static final Color STITCH = Color.valueOf("2a1e24");
    private static final Color THREAD = Color.valueOf("e05a8a");
    private static final Color STUFFING = Color.valueOf("fbfaf0");
    private static final Color BOW = Color.valueOf("4a8ae0");
    private static final Color NEEDLE = Color.valueOf("c8d0dc");

    private enum Move { REST, HUG_AIM, HUG_RUSH, STUFFING, NEEDLES, FRIENDS }

    static final float HUG_AIM = 0.75f;
    static final float HUG_SPEED = 6f;
    static final float HUG_REACH = 5.5f;
    static final float STUFF_TIME = 1.2f;
    static final int NEEDLE_LINES = 4;

    private Move move = Move.REST;
    private float moveTimer = 1.5f;
    private int nextMove;
    private float shotTimer;
    private float time;
    private boolean enraged;
    private int hugsLeft;
    private float hugAngle;
    private float hugRun;
    private int linesLeft;
    private final Vector2 step = new Vector2();

    public BigTeddy(float x, float y, int tier) {
        super(x, y, 1200f + 200f * tier);
        radius = 0.6f;
        speed = 0.85f;
        score = 2000 + 400 * tier;
        boss = true;
    }

    @Override
    public String bossName() {
        return Boss.BIG_TEDDY.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    @Override
    public float feetY() {
        return pos.y - 0.75f;
    }

    @Override
    public void hit(float damage, Vector2 bulletVel, float knockback, GameWorld world) {
        super.hit(damage, bulletVel, 0f, world); // (far too big to knock about)
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        float diff = world.getDifficulty();
        if (!enraged && hp < maxHp * 0.5f) { // a button eye pops off: now it's cross
            enraged = true;
            world.clearEnemyBullets(0, 0, -1f);
            world.shake(0.7f);
            world.sfx().play("pop", 1f);
            world.toast("POP! A BUTTON EYE CAME OFF!");
            move = Move.REST;
            moveTimer = 0.9f;
        }
        moveTimer -= delta;
        shotTimer -= delta;
        switch (move) {
            case REST:
                keepDistance(delta, world, 2.8f, speed * (enraged ? 1.3f : 1f));
                if (moveTimer <= 0) startNext(world);
                break;
            case HUG_AIM: // arms out, the line on the floor follows you - then locks
                if (moveTimer > HUG_AIM * 0.35f) hugAngle = aimAngle;
                if (moveTimer <= 0) {
                    move = Move.HUG_RUSH;
                    hugRun = 0f;
                    world.sfx().play("dodge", 1f);
                }
                break;
            case HUG_RUSH:
                rush(delta, world, diff);
                break;
            case STUFFING:
                if (shotTimer <= 0) {
                    shotTimer = 0.07f / diff;
                    for (int i = 0; i < 2; i++) {
                        float a = aimAngle + MathUtils.random(-48f, 48f);
                        Bullet b = world.spawnEnemyBullet(pos.x, pos.y - 0.05f, a, MathUtils.random(2.2f, 3.4f) * diff);
                        b.shape = Bullet.Shape.CLOUD;
                        b.color.set(STUFFING);
                        b.accel = -2.6f; // it puffs out, hangs, then drifts on slowly
                        b.minSpeed = 0.55f * diff;
                    }
                    world.sfx().play("pop", 0.15f);
                }
                if (moveTimer <= 0) rest();
                break;
            case NEEDLES:
                if (shotTimer <= 0 && linesLeft > 0) {
                    shotTimer = 0.38f / diff;
                    needleLine(world, diff, (NEEDLE_LINES - linesLeft) % 2 == 1);
                    linesLeft--;
                }
                if (linesLeft == 0 && shotTimer <= 0) rest();
                break;
            case FRIENDS:
                if (moveTimer <= 0) rest();
                break;
        }
    }

    private void rest() {
        move = Move.REST;
        moveTimer = enraged ? 0.55f : 0.9f;
    }

    private void startNext(GameWorld world) {
        Move[] order = {Move.HUG_AIM, Move.STUFFING, Move.NEEDLES, Move.HUG_AIM, Move.FRIENDS, Move.NEEDLES};
        move = order[nextMove++ % order.length];
        if (move == Move.FRIENDS && world.getEnemies().size + world.getPendingEnemies().size > 2) {
            move = Move.STUFFING; // (its friends from last time are still about)
        }
        shotTimer = 0.2f;
        switch (move) {
            case HUG_AIM:
                hugsLeft = enraged ? 2 : 1;
                moveTimer = HUG_AIM;
                break;
            case STUFFING:
                moveTimer = STUFF_TIME;
                world.sfx().play("break", 0.6f);
                break;
            case NEEDLES:
                linesLeft = enraged ? NEEDLE_LINES + 2 : NEEDLE_LINES;
                break;
            case FRIENDS:
                moveTimer = 1f;
                int n = enraged ? 3 : 2;
                for (int i = 0; i < n; i++) {
                    float a = 90f + (i - (n - 1) / 2f) * 60f;
                    float x = MathUtils.clamp(pos.x + MathUtils.cosDeg(a) * 1.3f, Arena.now.left + 0.5f, Arena.now.right - 0.5f);
                    float y = MathUtils.clamp(pos.y + MathUtils.sinDeg(a) * 0.8f, Arena.now.bottom + 0.5f, Arena.now.top - 0.5f);
                    world.spawnEnemy(new BulletKin(x, y));
                }
                world.toast("THE TOY BOX ANSWERS!");
                world.sfx().play("spawn", 1f);
                break;
            default:
                break;
        }
    }

    /** Rushes along the aimed line until it's gone far enough or hits something, then claps: a ring. */
    private void rush(float delta, GameWorld world, float diff) {
        float d = HUG_SPEED * Math.min(diff, 1.4f) * delta;
        step.set(MathUtils.cosDeg(hugAngle), MathUtils.sinDeg(hugAngle)).scl(d);
        float bx = pos.x;
        float by = pos.y;
        pos.add(step);
        Arena.now.clamp(pos, radius);
        hugRun += d;
        boolean blocked = Vector2.dst(bx, by, pos.x, pos.y) < d * 0.5f;
        walkTime += delta * 2f;
        if (hugRun < HUG_REACH && !blocked) return;
        int n = enraged ? 18 : 14; // CLAP
        float off = MathUtils.random(360f);
        for (int i = 0; i < n; i++) {
            Bullet b = world.spawnEnemyBullet(pos.x, pos.y, off + 360f * i / n, 2.1f * diff);
            b.shape = Bullet.Shape.CLOUD;
            b.color.set(FUR_LIGHT);
        }
        world.shake(0.4f);
        world.sfx().play("boom", 0.5f);
        if (--hugsLeft > 0) {
            move = Move.HUG_AIM;
            moveTimer = HUG_AIM * 0.8f;
        } else {
            rest();
        }
    }

    /** A line of needles across your path: aimed at you, every other line shifted half a stitch. */
    private void needleLine(GameWorld world, float diff, boolean shifted) {
        int n = 7;
        float across = aimAngle + 90f;
        float gap = 0.42f;
        for (int i = 0; i < n; i++) {
            float o = (i - (n - 1) / 2f + (shifted ? 0.5f : 0f)) * gap;
            float x = pos.x + MathUtils.cosDeg(across) * o;
            float y = pos.y + MathUtils.sinDeg(across) * o;
            Bullet b = world.spawnEnemyBullet(x, y, aimAngle, 2.6f * diff);
            b.shape = Bullet.Shape.DIAMOND;
            b.color.set(NEEDLE);
        }
        world.sfx().play("swish", 0.5f);
    }

    // ------------------------------------------------------------ drawing

    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (move == Move.HUG_AIM) { // the line it'll rush along
            float locked = moveTimer <= HUG_AIM * 0.35f ? 1f : 0.5f;
            d.line(b, pos.x, feetY() + 0.2f, HUG_REACH, 0.5f, hugAngle, TELEGRAPH, 0.18f * locked + 0.08f);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float x = pos.x;
        float y = pos.y;
        float bottom = feetY();
        boolean hugging = move == Move.HUG_AIM || move == Move.HUG_RUSH;
        float stride = MathUtils.sin(walkTime * 8f) * 0.04f;
        // legs, the round tummy, the arms (out wide for a hug)
        for (int i = -1; i <= 1; i += 2) {
            d.circle(b, x + i * 0.3f, bottom + 0.16f + (i < 0 ? stride : -stride), 0.2f, c(FUR_DARK));
            d.ellipse(b, x + i * 0.3f, bottom + 0.1f, 0.12f, 0.08f, c(MUZZLE), 1f);
        }
        float armA = hugging ? 70f : 20f + MathUtils.sin(time * 2f) * 5f;
        for (int i = -1; i <= 1; i += 2) {
            float ax = x + i * 0.5f;
            float ay = y - 0.05f;
            d.rotRect(b, ax + i * 0.15f, ay, 0.4f, 0.22f, i * armA, c(FUR_DARK), 1f);
            d.circle(b, ax + i * MathUtils.cosDeg(armA) * 0.32f, ay + MathUtils.sinDeg(armA) * 0.3f, 0.13f, c(FUR));
        }
        d.circle(b, x, y - 0.1f, 0.56f, Draw.OUTLINE_COLOR);
        d.circle(b, x, y - 0.1f, 0.53f, c(FUR));
        d.circle(b, x, y - 0.18f, 0.3f, c(FUR_LIGHT));
        for (int i = 0; i < 6; i++) { // the stitches down its tummy
            d.rotRect(b, x, y - 0.4f + i * 0.09f, 0.1f, 0.02f, 0f, STITCH, 1f);
        }
        d.rect(b, x - 0.01f, y - 0.42f, 0.02f, 0.5f, STITCH);
        if (enraged || move == Move.STUFFING) { // stuffing poking out of a ripped seam
            for (int i = 0; i < 4; i++) {
                d.circle(b, x + 0.22f + (i % 2) * 0.08f, y - 0.05f + i * 0.06f + MathUtils.sin(time * 4f + i) * 0.01f, 0.06f, STUFFING);
            }
        }
        // the head: ears, a muzzle with a stitched-up mouth, a bow tie, button eyes
        float hy = y + 0.55f;
        for (int i = -1; i <= 1; i += 2) {
            d.circle(b, x + i * 0.33f, hy + 0.28f, 0.15f, c(FUR_DARK));
            d.circle(b, x + i * 0.33f, hy + 0.28f, 0.08f, c(MUZZLE));
        }
        d.circle(b, x, hy, 0.4f, Draw.OUTLINE_COLOR);
        d.circle(b, x, hy, 0.37f, c(FUR));
        d.ellipse(b, x, hy - 0.13f, 0.18f, 0.13f, c(MUZZLE), 1f);
        d.ellipse(b, x, hy - 0.06f, 0.06f, 0.045f, STITCH, 1f); // the nose
        float frown = enraged ? -1f : 0f;
        d.rotRect(b, x - 0.06f, hy - 0.18f + frown * 0.01f, 0.1f, 0.02f, 15f + frown * 25f, STITCH, 1f);
        d.rotRect(b, x + 0.06f, hy - 0.18f + frown * 0.01f, 0.1f, 0.02f, -15f - frown * 25f, STITCH, 1f);
        float look = MathUtils.cosDeg(aimAngle) * 0.02f;
        drawButton(b, d, x - 0.14f + look, hy + 0.1f);
        if (enraged) { // the other one dangles by its thread
            float sway = MathUtils.sin(time * 3f) * 0.05f;
            d.line(b, x + 0.14f, hy + 0.1f, 0.22f, 0.012f, -90f + sway * 200f, THREAD, 1f);
            drawButton(b, d, x + 0.14f + sway, hy - 0.12f);
            d.circle(b, x + 0.14f, hy + 0.1f, 0.05f, STUFFING);
        } else {
            drawButton(b, d, x + 0.14f + look, hy + 0.1f);
        }
        d.shape(b, d.triangle, x - 0.1f, hy - 0.4f, 0.09f, -90f, c(BOW), 1f);
        d.shape(b, d.triangle, x + 0.1f, hy - 0.4f, 0.09f, 90f, c(BOW), 1f);
        d.circle(b, x, hy - 0.4f, 0.045f, c(BOW));
    }

    private void drawButton(SpriteBatch b, Draw d, float x, float y) {
        d.circle(b, x, y, 0.07f, BUTTON);
        for (int i = 0; i < 4; i++) {
            d.circle(b, x + (i % 2 == 0 ? -0.02f : 0.02f), y + (i < 2 ? -0.02f : 0.02f), 0.01f, STUFFING, 0.7f);
        }
        d.line(b, x - 0.025f, y - 0.025f, 0.07f, 0.008f, 45f, THREAD, 1f);
    }
}
