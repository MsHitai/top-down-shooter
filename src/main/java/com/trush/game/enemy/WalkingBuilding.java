package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * A Slums boss (the owner's son's idea; the fight after ETG's Wall Monger): a tenement block on two
 * stomping legs, as wide as the long street it walks down. It fills the street from wall to wall and comes
 * on step by step; the player backs away down the street, and if it reaches the far end it squashes them
 * against the wall. Its face is the bottom of its front: two windows for eyes, the front door for a mouth.
 * <ul>
 *   <li>windows light up red, then fire a burst at the player;</li>
 *   <li>the whole bottom floor of windows fires straight down - curtains of bullets with gaps between;</li>
 *   <li>the rooftop antenna sends out arcs of radio waves, each with a gap to slip through;</li>
 *   <li>the front door opens and goons walk out (robbers, rats, dynamite).</li>
 * </ul>
 * Below 40% it's enraged: it walks faster, its glass cracks and it fires more. Its whole front takes hits
 * ({@link #bodyContains}); its round hitbox follows the player's side of the facade so beams and blasts land too.
 */
public class WalkingBuilding extends Enemy {

    /** Its street: narrow (it fills it) and long (it walks down it). */
    public static final int ARENA_COLS = 17;
    public static final int ARENA_ROWS = 40;

    private static final Color BRICK = Color.valueOf("8a5a4a");
    private static final Color BRICK_DARK = Color.valueOf("5a3a30");
    private static final Color TRIM = Color.valueOf("c8b8a0");
    private static final Color GLASS = Color.valueOf("2a3a5a");
    private static final Color LIT = Color.valueOf("ffd166");
    private static final Color WARN = Color.valueOf("ff4d4d");
    private static final Color DOOR = Color.valueOf("4a2a1a");
    private static final Color LEG = Color.valueOf("4a4a54");
    private static final Color BOOT = Color.valueOf("2a2a30");
    private static final Color PUPIL_DARK = Color.valueOf("16141c");
    private static final Color WAVE = Color.valueOf("8ae8ff");
    private static final Color SHOT = Color.valueOf("ff8a3a");

    private static final int COLS = 6;
    private static final int FLOORS = 5;
    private static final float LEG_H = 0.8f;
    private static final float FLOOR_H = 0.9f;
    /** Each stride: this far, over STEP_MOVE seconds, every STEP_TIME (faster when enraged). */
    private static final float STEP = 0.34f;
    private static final float STEP_MOVE = 0.4f;
    private static final float STEP_TIME = 1.45f;
    public static final float ENRAGE_AT = 0.4f;
    private static final float WINDOW_WARN = 0.45f;
    private static final float DOOR_OPEN = 0.7f;
    /** It stops this far from the far wall - the player is squashed in the gap that's left. */
    private static final float END_GAP = 0.05f;

    /** Where its feet are: the front of the building, coming down the street. */
    private float frontY;
    private float left;
    private float right;
    private boolean placed;
    private float stepTimer = 1.2f;
    private float stepping;
    private int stepLeg;
    private float windowTimer = 1f;
    private float curtainTimer = 5f;
    private float antennaTimer = 3.5f;
    private float doorTimer = 6f;
    private float door;
    private float clock;
    private boolean enraged;
    /** Windows about to fire: col, row (0 = the bottom floor), time left. */
    private final Array<float[]> warnings = new Array<>();
    /** The bottom floor's curtain: bullets still to pour out, and when the next goes. */
    private int curtainLeft;
    private float curtainTick;
    private final Rectangle wall = new Rectangle();
    private final Vector2 aimFrom = new Vector2();

    public WalkingBuilding(float x, float y, int tier) {
        super(x, y, 1500f + 350f * tier);
        radius = 0.9f;
        speed = 0f;
        score = 800 + 250 * tier;
        boss = true;
        flying = true; // (no pits, no puddles, no being pushed out of furniture: it IS the scenery)
        hurtsToTouch = false; // it doesn't bite: it squashes (GameWorld.pushPlayerOutOfBlockers)
        frontY = y - 2.1f;
    }

    @Override
    public String bossName() {
        return Boss.WALKING_BUILDING.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    /** Pulls the camera back to take in the whole street from wall to wall. */
    @Override
    public float viewZoom() {
        return 1.3f;
    }

    @Override
    public float feetY() {
        return frontY;
    }

    @Override
    protected boolean castsShadow() {
        return false;
    }

    /** Its front and everything behind it, wall to wall. */
    @Override
    public Rectangle blocker() {
        return placed ? wall.set(left - 1f, frontY, right - left + 2f, 60f) : null;
    }

    @Override
    public boolean bodyContains(float x, float y) {
        return placed && y >= frontY && x >= left && x <= right;
    }

    private float facadeBottom() {
        return frontY + LEG_H;
    }

    private float windowX(int col) {
        return left + (right - left) * (col + 0.5f) / COLS;
    }

    private float windowY(int row) {
        return facadeBottom() + 0.45f + row * FLOOR_H;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        if (!placed) {
            left = Arena.now.left;
            right = Arena.now.right;
            placed = true;
        }
        clock += delta;
        float diff = world.getDifficulty();
        if (!enraged && hp <= maxHp * ENRAGE_AT) {
            enraged = true;
            world.shake(0.6f);
            world.sfx().play("break", 1f);
            for (int i = 0; i < 12; i++) { // the glass cracks
                world.particles().burst(windowX(MathUtils.random(COLS - 1)), windowY(MathUtils.random(1)), 4, 0.3f, 1.4f,
                        0.6f, 0.04f, Color.WHITE);
            }
        }
        // its round hitbox keeps to the player's side of the front, so beams and blasts there land
        float px = world.getPlayer().centerX();
        pos.set(MathUtils.clamp(px, left + radius, right - radius), frontY + 0.6f);
        walk(delta, world);
        float pace = (enraged ? 1.35f : 1f) * diff;
        windows(delta, world, pace, diff);
        curtain(delta, world, diff);
        antenna(delta, world, pace, diff);
        frontDoor(delta, world, pace);
    }

    /** Stomp, stomp: a stride every so often until it reaches the far end of the street. */
    private void walk(float delta, GameWorld world) {
        float end = Arena.now.bottom + Player.FEET_H + END_GAP; // (the hero's feet still fit in the gap)
        if (stepping > 0) {
            float d = Math.min(stepping, delta);
            stepping -= d;
            frontY = Math.max(end, frontY - STEP * d / STEP_MOVE);
            if (stepping <= 0) { // the foot comes down
                world.shake(0.15f);
                world.sfx().play("body_fall", 0.7f);
                float fx = stepLeg == 0 ? left + (right - left) * 0.28f : right - (right - left) * 0.28f;
                world.particles().burst(fx, frontY + 0.05f, 10, 0.3f, 1.2f, 0.5f, 0.06f, BRICK_DARK);
            }
            return;
        }
        stepTimer -= delta;
        if (stepTimer <= 0) {
            stepTimer = enraged ? STEP_TIME * 0.7f : STEP_TIME;
            if (frontY > end) {
                stepping = STEP_MOVE;
                stepLeg = 1 - stepLeg;
            } else { // the end of the street: it stamps in place
                world.shake(0.25f);
                world.sfx().play("body_fall", 0.9f);
            }
        }
    }

    /** A window lights up red, then fires a burst at the player. Only the two floors nearest the street fire. */
    private void windows(float delta, GameWorld world, float pace, float diff) {
        for (int i = warnings.size - 1; i >= 0; i--) {
            float[] w = warnings.get(i);
            w[2] -= delta;
            if (w[2] <= 0) {
                warnings.removeIndex(i);
                aimFrom.set(windowX((int) w[0]), windowY((int) w[1]));
                float a = MathUtils.atan2(world.getPlayer().hitboxY() - aimFrom.y, world.getPlayer().centerX() - aimFrom.x)
                        * MathUtils.radiansToDegrees;
                int n = enraged ? 3 : 2;
                for (int k = 0; k < n; k++) {
                    Bullet b = world.spawnEnemyBullet(aimFrom.x, aimFrom.y, a + (k - (n - 1) / 2f) * 9f, 2.6f * diff);
                    b.color.set(SHOT);
                }
                world.sfx().play("enemy_shoot", 0.5f);
            }
        }
        windowTimer -= delta * pace;
        if (windowTimer <= 0) {
            windowTimer = MathUtils.random(0.6f, 0.9f);
            warnings.add(new float[]{MathUtils.random(COLS - 1), MathUtils.random(1), WINDOW_WARN});
        }
    }

    /** The whole bottom floor pours bullets straight down: columns with gaps between (two offset rows enraged). */
    private void curtain(float delta, GameWorld world, float diff) {
        if (curtainLeft > 0) {
            curtainTick -= delta;
            if (curtainTick <= 0) {
                curtainTick = 0.16f;
                curtainLeft--;
                boolean shifted = enraged && curtainLeft % 2 == 1;
                for (int c = 0; c < COLS; c++) {
                    float x = shifted ? windowX(c) + (right - left) / COLS / 2f : windowX(c);
                    if (x > right - 0.2f) continue;
                    Bullet b = world.spawnEnemyBullet(x, facadeBottom(), -90f, 2.2f * diff);
                    b.color.set(SHOT);
                }
            }
            return;
        }
        curtainTimer -= delta;
        if (curtainTimer <= 0) {
            curtainTimer = MathUtils.random(6f, 8f) / (enraged ? 1.3f : 1f);
            curtainLeft = enraged ? 8 : 5;
            curtainTick = 0.5f; // the windows all flash first
            world.sfx().play("whistle", 0.4f);
        }
    }

    /** The rooftop antenna: arcs of radio waves rolling down the street, each with one gap. */
    private void antenna(float delta, GameWorld world, float pace, float diff) {
        antennaTimer -= delta * pace;
        if (antennaTimer > 0) {
            return;
        }
        antennaTimer = MathUtils.random(4.5f, 6f);
        float ox = (left + right) / 2f;
        float oy = facadeBottom() + FLOORS * FLOOR_H;
        int n = 26;
        float gapAt = MathUtils.random(0.2f, 0.8f);
        for (int i = 0; i < n; i++) {
            float k = i / (n - 1f);
            if (Math.abs(k - gapAt) < 0.09f) continue; // the gap
            Bullet b = world.spawnEnemyBullet(ox, oy, 200f + 140f * k, 1.7f * diff);
            b.color.set(WAVE);
            b.shape = Bullet.Shape.DIAMOND;
        }
        world.sfx().play("zap", 0.6f);
    }

    /** The front door swings open and a goon (or a rat pack, or a dynamite) walks out - a few at a time at most. */
    private void frontDoor(float delta, GameWorld world, float pace) {
        if (door > 0) {
            door -= delta;
            if (door <= 0) {
                float x = (left + right) / 2f;
                float y = frontY - 0.35f;
                float r = MathUtils.random();
                if (r < 0.4f) {
                    world.spawnEnemy(new Robber(x, y));
                } else if (r < 0.75f) {
                    for (int i = 0; i < 3; i++) world.spawnEnemy(new Rat(x + MathUtils.random(-0.3f, 0.3f), y));
                } else {
                    world.spawnEnemy(new Dynamite(x, y));
                }
            }
            return;
        }
        doorTimer -= delta * pace;
        if (doorTimer <= 0) {
            doorTimer = MathUtils.random(7f, 9f);
            if (world.getEnemies().size + world.getPendingEnemies().size <= 4) {
                door = DOOR_OPEN;
                world.sfx().play("door", 0.7f);
            }
        }
    }

    /** The intro card: a small version of it, so the whole building fits in the picture. */
    @Override
    public void drawPortrait(SpriteBatch b, Draw d, float aim) {
        aimAngle = aim;
        drawBuilding(b, d, pos.x - 1.3f, pos.x + 1.3f, pos.y - 2.4f, 0.55f);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        if (!placed) {
            return;
        }
        drawBuilding(b, d, left, right, frontY, 1f);
    }

    /** The whole building between {@code l} and {@code r}, its feet at {@code front}, its floors {@code s} times as tall. */
    private void drawBuilding(SpriteBatch b, Draw d, float l, float r, float front, float s) {
        float w = r - l;
        float bottom = front + LEG_H * s;
        float floorH = FLOOR_H * s;
        float top = bottom + FLOORS * floorH;
        // the legs: two thick stilts, one lifted mid-stride, big boots
        for (int leg = 0; leg < 2; leg++) {
            float lx = leg == 0 ? l + w * 0.28f : r - w * 0.28f;
            float lift = stepping > 0 && leg == stepLeg ? MathUtils.sin((1f - stepping / STEP_MOVE) * MathUtils.PI) * 0.18f * s : 0f;
            d.ellipse(b, lx, front, 0.5f * s, 0.12f * s, Color.BLACK, 0.35f);
            d.rect(b, lx - 0.2f * s, front + lift + 0.1f * s, 0.4f * s, LEG_H * s, c(LEG));
            d.rect(b, lx - 0.32f * s, front + lift, 0.64f * s, 0.18f * s, c(BOOT));
        }
        // the facade: bricks, a cornice on each floor, a flat roof with its antenna
        d.rect(b, l - 0.04f, bottom - 0.04f, w + 0.08f, top - bottom + 0.08f, BRICK_DARK);
        d.rect(b, l, bottom, w, top - bottom, c(BRICK));
        for (int f = 1; f <= FLOORS; f++) {
            d.rect(b, l, bottom + f * floorH - 0.05f * s, w, 0.06f * s, c(TRIM));
        }
        float ax = (l + r) / 2f;
        d.rect(b, ax - 0.03f * s, top, 0.06f * s, 0.9f * s, LEG);
        d.circle(b, ax, top + 0.9f * s, 0.07f * s, MathUtils.sin(clock * 6f) > 0 ? WARN : LEG);
        // windows; the two above the door are its eyes, looking at the player
        int eyeL = COLS / 2 - 1;
        int eyeR = COLS / 2;
        float face = MathUtils.cosDeg(aimAngle);
        for (int f = 0; f < FLOORS; f++) {
            for (int c = 0; c < COLS; c++) {
                float wx = l + w * (c + 0.5f) / COLS;
                float wy = bottom + 0.45f * s + f * floorH;
                float ww = 0.42f * s;
                float wh = 0.42f * s;
                boolean eye = f == 1 && (c == eyeL || c == eyeR);
                Color glass = eye ? Color.WHITE : (c * 7 + f * 3) % 5 == 0 ? LIT : GLASS;
                if (f == 0 && curtainLeft > 0) glass = WARN;
                for (float[] warn : warnings) {
                    if ((int) warn[0] == c && (int) warn[1] == f) {
                        glass = MathUtils.sin(clock * 30f) > 0 ? WARN : LIT;
                    }
                }
                if (f == 0 && (c == eyeL || c == eyeR)) continue; // the door goes there
                d.rect(b, wx - ww / 2f - 0.03f * s, wy - wh / 2f - 0.03f * s, ww + 0.06f * s, wh + 0.06f * s, c(TRIM));
                d.rect(b, wx - ww / 2f, wy - wh / 2f, ww, wh, eye ? Color.WHITE : glass);
                if (eye) { // a pupil, looking at the player; angry lids when enraged
                    d.circle(b, wx + face * 0.08f * s, wy - 0.03f * s, 0.11f * s, PUPIL_DARK);
                    if (enraged) {
                        float tilt = c == eyeL ? -18f : 18f;
                        d.rotRect(b, wx, wy + wh * 0.42f, ww * 1.2f, 0.09f * s, tilt, BRICK_DARK, 1f);
                    }
                } else if (enraged && (c + f) % 2 == 0) { // cracked glass
                    d.line(b, wx - ww * 0.4f, wy + wh * 0.3f, ww * 0.8f, 0.015f, -35f, Color.WHITE);
                }
            }
        }
        // the front door: its mouth (an awning over it), open when a goon is coming out
        float dx = l + w * 0.5f;
        float dw = 0.8f * s;
        float dh = 0.75f * s;
        d.rect(b, dx - dw / 2f - 0.15f * s, bottom + dh, dw + 0.3f * s, 0.12f * s, WARN); // the awning
        d.rect(b, dx - dw / 2f, bottom, dw, dh, door > 0 ? PUPIL_DARK : c(DOOR));
        if (door <= 0) {
            d.rect(b, dx - 0.015f, bottom, 0.03f, dh, PUPIL_DARK);
            d.circle(b, dx + 0.12f * s, bottom + dh * 0.45f, 0.03f * s, LIT);
        }
    }
}
