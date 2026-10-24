package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * Skyscraper boss: a kaiju taller than the tower, standing beyond the far edge of the roof - like
 * ETG's Dragun, it doesn't walk; you have the roof to run around on and its head to shoot at.
 * <ul>
 *   <li>mouth laser: it charges up, then sweeps a beam across the roof (roll through it, or hide);</li>
 *   <li>fist slams: red circles where its fists will come down, a ring of debris from each;</li>
 *   <li>bite: its head lunges down onto the near part of the roof (a red zone warns where);</li>
 *   <li>rubble roar: fans of rubble between the big moves.</li>
 * </ul>
 * Enraged it slams more, sweeps the laser there and back, and roars harder. The camera pulls back
 * to fit it in ({@link #viewZoom}).
 */
public class Kaiju extends Enemy {

    private enum Phase { IDLE, LASER_CHARGE, LASER, SLAMS, BITE_WARN, BITE, ROAR }

    private static final Color SCALE = Color.valueOf("3f7a4a");
    private static final Color SCALE_DARK = Color.valueOf("2a5434");
    private static final Color SCALE_LIGHT = Color.valueOf("5a9a5e");
    private static final Color BELLY = Color.valueOf("b8c88a");
    private static final Color SPINE = Color.valueOf("c9e8b8");
    private static final Color EYE = Color.valueOf("ffd23a");
    private static final Color TOOTH = Color.valueOf("f5f0e0");
    private static final Color MOUTH = Color.valueOf("5a1a22");
    private static final Color BEAM = Color.valueOf("8ae8ff");
    private static final Color RUBBLE = Color.valueOf("9a948a");

    private static final float LASER_CHARGE = 1.1f;
    private static final float LASER_TIME = 1.7f;
    private static final float SLAM_WARN = 0.85f;
    private static final float SLAM_RADIUS = 0.85f;
    private static final float BITE_WARN = 1.0f;
    private static final float BITE_TIME = 0.55f;
    private static final float BEAM_WIDTH = 0.16f;

    private Phase phase = Phase.IDLE;
    private float phaseTimer = 2.2f;
    private int patternIndex;
    private boolean enraged;
    private float time;
    private float homeX;
    private float homeY;
    // laser
    private float laserFrom;
    private float laserTo;
    private float laserAngle;
    private float laserLength;
    private int sweepsLeft;
    // slams: up to 3 fists in the air at once, each with its own countdown to impact
    private final Vector2[] slamAt = {new Vector2(), new Vector2(), new Vector2()};
    private final float[] slamTimer = new float[3];
    /** After a fist lands it rests on the roof a moment before lifting away. */
    private final float[] slamLinger = new float[3];
    private int slamsLeft;
    private float nextSlam;
    // bite
    private final Rectangle biteZone = new Rectangle();
    private float biteLunge;
    // roar
    private int wavesLeft;
    private float waveTimer;

    public Kaiju(float x, float y, int tier) {
        super(x, y, 900f + 250f * tier);
        radius = 0.75f;
        score = 800 + 300 * tier;
        boss = true;
        flying = true; // it stands outside the roof: furniture doesn't push it about
        Arena a = Arena.now;
        homeX = a.centerX;
        homeY = a.top - 0.85f;
        pos.set(homeX, homeY);
    }

    @Override
    public String bossName() {
        return Boss.KAIJU.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    @Override
    public float viewZoom() {
        return 1.45f;
    }

    @Override
    protected boolean castsShadow() {
        return false;
    }

    /** Drawn behind everything on the roof (it stands beyond the far edge). */
    @Override
    public float feetY() {
        return Arena.now.top + 1f;
    }

    private float mouthX() {
        return pos.x;
    }

    private float mouthY() {
        return pos.y - 0.35f;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        knock.setZero();
        if (!enraged && hp < maxHp * 0.5f) {
            enraged = true;
            world.shake(0.8f);
            world.sfx().play("boss", 1f);
        }
        float diff = world.getDifficulty();
        phaseTimer -= delta;
        updateSlams(delta, world, diff);
        // the head sways a little as it breathes (and lunges down for a bite)
        float sway = phase == Phase.BITE || phase == Phase.BITE_WARN ? 0f : MathUtils.sin(time * 0.6f) * 1.2f;
        pos.x += (homeX + sway - pos.x) * Math.min(1f, delta * 2f);
        pos.y = homeY - biteLunge;

        switch (phase) {
            case IDLE:
                if (phaseTimer <= 0) {
                    nextPattern(world);
                }
                break;
            case LASER_CHARGE:
                laserAngle = laserFrom;
                laserLength = world.rayLength(mouthX(), mouthY(), laserAngle, 16f);
                if (phaseTimer <= 0) {
                    phase = Phase.LASER;
                    phaseTimer = LASER_TIME;
                    world.sfx().play("zap", 1f);
                    world.shake(0.3f);
                }
                break;
            case LASER: {
                float k = 1f - phaseTimer / LASER_TIME;
                laserAngle = MathUtils.lerp(laserFrom, laserTo, k * k * (3 - 2 * k));
                laserLength = world.rayLength(mouthX(), mouthY(), laserAngle, 16f);
                if (beamHits(world.getPlayer())) {
                    world.hurtPlayer();
                }
                if (MathUtils.randomBoolean(0.6f)) {
                    float ex = mouthX() + MathUtils.cosDeg(laserAngle) * laserLength;
                    float ey = mouthY() + MathUtils.sinDeg(laserAngle) * laserLength;
                    world.particles().spawn(ex, ey, MathUtils.random(-1f, 1f), MathUtils.random(0.5f, 1.5f), 0.3f, 0.05f, BEAM, 1f);
                }
                if (phaseTimer <= 0) {
                    if (--sweepsLeft > 0) { // enraged: and back again
                        float t = laserFrom;
                        laserFrom = laserTo;
                        laserTo = t;
                        phaseTimer = LASER_TIME;
                    } else {
                        toIdle();
                    }
                }
                break;
            }
            case SLAMS:
                nextSlam -= delta;
                if (slamsLeft > 0 && nextSlam <= 0) {
                    startSlam(world);
                }
                if (slamsLeft <= 0 && !slamming()) {
                    toIdle();
                }
                break;
            case BITE_WARN:
                biteLunge = Math.max(0f, biteLunge - delta); // rear back
                if (phaseTimer <= 0) {
                    phase = Phase.BITE;
                    phaseTimer = BITE_TIME;
                    world.sfx().play("boom", 0.8f);
                    world.shake(0.7f);
                    Player p = world.getPlayer();
                    if (biteZone.contains(p.centerX(), p.hitboxY())) {
                        world.hurtPlayer();
                    }
                    world.particles().burst(biteZone.x + biteZone.width / 2f, biteZone.y + 0.3f, 24, 0.8f, 2.5f, 0.5f, 0.06f, RUBBLE);
                }
                break;
            case BITE: {
                // the jaws snap down onto the zone, then pull back
                float k = 1f - phaseTimer / BITE_TIME;
                float depth = homeY - (biteZone.y + 0.6f);
                biteLunge = depth * (k < 0.25f ? k / 0.25f : 1f - (k - 0.25f) / 0.75f);
                if (phaseTimer <= 0) {
                    biteLunge = 0f;
                    toIdle();
                }
                break;
            }
            case ROAR:
                waveTimer -= delta;
                if (waveTimer <= 0 && wavesLeft > 0) {
                    waveTimer = enraged ? 0.32f : 0.45f;
                    wavesLeft--;
                    int n = enraged ? 9 : 7;
                    float fan = 70f;
                    float off = MathUtils.random(-8f, 8f);
                    for (int i = 0; i < n; i++) {
                        Bullet b = world.spawnEnemyBullet(mouthX(), mouthY(), aimAngle + off - fan / 2f + fan * i / (n - 1),
                                2.4f * diff);
                        b.shape = Bullet.Shape.SQUARE;
                        b.color.set(RUBBLE);
                    }
                    world.sfx().play("enemy_shoot", 0.9f);
                }
                if (wavesLeft <= 0 && waveTimer <= 0) {
                    toIdle();
                }
                break;
        }
    }

    private void toIdle() {
        phase = Phase.IDLE;
        phaseTimer = enraged ? 0.9f : 1.4f;
    }

    private void nextPattern(GameWorld world) {
        Phase[] order = {Phase.SLAMS, Phase.ROAR, Phase.LASER_CHARGE, Phase.ROAR, Phase.BITE_WARN};
        Phase next = order[patternIndex++ % order.length];
        Player p = world.getPlayer();
        Arena a = Arena.now;
        switch (next) {
            case LASER_CHARGE: {
                // sweep across the roof, starting on the side away from the player
                boolean fromLeft = p.centerX() > pos.x;
                laserFrom = fromLeft ? 215f : 325f;
                laserTo = fromLeft ? 325f : 215f;
                sweepsLeft = enraged ? 2 : 1;
                phaseTimer = LASER_CHARGE;
                world.sfx().play("buzz", 0.7f);
                break;
            }
            case SLAMS:
                slamsLeft = enraged ? 5 : 3;
                nextSlam = 0f;
                break;
            case BITE_WARN: {
                float bx = MathUtils.clamp(p.centerX(), a.left + 2.2f, a.right - 2.2f);
                biteZone.set(bx - 2.2f, a.top - 3.6f, 4.4f, 3.6f);
                homeX = bx; // turn to face it
                phaseTimer = BITE_WARN;
                world.sfx().play("boss", 0.6f);
                break;
            }
            case ROAR:
                wavesLeft = enraged ? 5 : 3;
                waveTimer = 0.3f;
                break;
            default:
                break;
        }
        if (next != Phase.BITE_WARN) {
            homeX = a.centerX;
        }
        phase = next;
    }

    private boolean slamming() {
        for (int i = 0; i < slamAt.length; i++) {
            if (fistOut(i)) return true;
        }
        return false;
    }

    /** That fist is raised or resting where it landed (so its arm isn't on the roof's edge). */
    private boolean fistOut(int i) {
        return slamTimer[i] > 0 || slamLinger[i] > 0;
    }

    /** Raises a fist over where the player is now (the later ones a little off, to herd them). */
    private void startSlam(GameWorld world) {
        for (int i = 0; i < slamAt.length; i++) {
            if (slamTimer[i] > 0) continue;
            Player p = world.getPlayer();
            float spread = slamsLeft == (enraged ? 5 : 3) ? 0f : 1.2f;
            slamAt[i].set(p.centerX() + MathUtils.random(-spread, spread), p.hitboxY() + MathUtils.random(-spread, spread) * 0.6f);
            Arena.now.clamp(slamAt[i], SLAM_RADIUS * 0.6f);
            slamTimer[i] = SLAM_WARN;
            slamsLeft--;
            nextSlam = enraged ? 0.38f : 0.55f;
            return;
        }
    }

    private void updateSlams(float delta, GameWorld world, float diff) {
        for (int i = 0; i < slamAt.length; i++) {
            slamLinger[i] = Math.max(0f, slamLinger[i] - delta);
            if (slamTimer[i] <= 0) continue;
            slamTimer[i] -= delta;
            if (slamTimer[i] > 0) continue;
            // the fist comes down
            slamLinger[i] = 0.3f;
            Vector2 at = slamAt[i];
            Player p = world.getPlayer();
            if (Vector2.dst(at.x, at.y, p.centerX(), p.hitboxY()) < SLAM_RADIUS) {
                world.hurtPlayer();
            }
            world.shake(0.45f);
            world.sfx().play("boom", 0.7f);
            world.particles().burst(at.x, at.y, 18, 0.6f, 2f, 0.5f, 0.06f, RUBBLE);
            int n = enraged ? 12 : 10;
            float start = MathUtils.random(360f);
            for (int k = 0; k < n; k++) {
                Bullet b = world.spawnEnemyBullet(at.x, at.y, start + 360f * k / n, 1.8f * diff);
                b.shape = Bullet.Shape.SQUARE;
                b.color.set(RUBBLE);
            }
        }
    }

    private boolean beamHits(Player p) {
        float ex = mouthX() + MathUtils.cosDeg(laserAngle) * laserLength;
        float ey = mouthY() + MathUtils.sinDeg(laserAngle) * laserLength;
        return Intersector.distanceSegmentPoint(mouthX(), mouthY(), ex, ey, p.centerX(), p.hitboxY())
                < BEAM_WIDTH / 2f + Player.HITBOX_RADIUS;
    }

    // ---------------------------------------------------------------- drawing

    /** Warnings on the roof: where the fists will land and where it's going to bite. */
    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        for (int i = 0; i < slamAt.length; i++) {
            if (slamTimer[i] <= 0) continue;
            float k = 1f - slamTimer[i] / SLAM_WARN;
            d.circle(b, slamAt[i].x, slamAt[i].y, SLAM_RADIUS, TELEGRAPH, 0.12f + 0.2f * k);
            d.ring(b, slamAt[i].x, slamAt[i].y, SLAM_RADIUS * (0.3f + 0.7f * k), TELEGRAPH, 0.8f);
        }
        if (phase == Phase.BITE_WARN) {
            float k = 1f - phaseTimer / BITE_WARN;
            float blink = 0.15f + 0.15f * k + (MathUtils.sin(time * 20f) > 0 ? 0.08f : 0f);
            d.rect(b, biteZone.x, biteZone.y, biteZone.width, biteZone.height, TELEGRAPH, blink);
        }
    }

    /** The laser and the fists coming down are drawn over everything on the roof. */
    @Override
    public void drawOverlay(SpriteBatch b, Draw d, Rectangle view) {
        if (!alive) {
            return;
        }
        if (phase == Phase.LASER_CHARGE) {
            float k = 1f - phaseTimer / LASER_CHARGE;
            d.line(b, mouthX(), mouthY(), laserLength, 0.02f + 0.02f * k, laserAngle, TELEGRAPH);
        } else if (phase == Phase.LASER) {
            float w = BEAM_WIDTH * (0.85f + 0.15f * MathUtils.sin(time * 40f));
            d.line(b, mouthX(), mouthY() + w * 0.9f, laserLength, w * 2.2f, laserAngle, BEAM);
            d.line(b, mouthX(), mouthY(), laserLength, w, laserAngle, Color.WHITE);
        }
        // fists: an arm reaching out over the roof, the fist growing as it drops
        for (int i = 0; i < slamAt.length; i++) {
            if (slamTimer[i] > 0) {
                float k = 1f - slamTimer[i] / SLAM_WARN;
                float lift = (1f - k * k) * 2.2f;
                drawFist(b, d, slamAt[i].x, slamAt[i].y + lift, 0.75f + 0.25f * k, i);
            } else if (slamLinger[i] > 0) {
                drawFist(b, d, slamAt[i].x, slamAt[i].y, 1f, i);
            }
        }
    }

    private void drawFist(SpriteBatch b, Draw d, float x, float y, float scale, int i) {
        float side = (i % 2 == 0) ? -1f : 1f;
        float sx = homeX + side * 2.4f;
        float sy = Arena.now.top + 1.6f;
        float ang = MathUtils.atan2Deg360(y - sy, x - sx);
        float len = Vector2.dst(sx, sy, x, y);
        d.line(b, sx, sy, len, 0.55f * scale, ang, SCALE_DARK);
        d.line(b, sx, sy, len, 0.4f * scale, ang, SCALE);
        float r = 0.42f * scale;
        d.circle(b, x, y, r * 1.12f, Draw.OUTLINE_COLOR);
        d.circle(b, x, y, r, SCALE);
        d.circle(b, x - r * 0.3f, y + r * 0.3f, r * 0.4f, SCALE_LIGHT);
        for (int c = -1; c <= 1; c++) { // claws
            d.shape(b, d.triangle, x + c * r * 0.55f, y - r * 0.85f, r * 0.3f, 270f, TOOTH, 1f);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        Arena a = Arena.now;
        float cx = homeX + (pos.x - homeX) * 0.4f;
        float base = a.top + 0.1f;
        float breathe = MathUtils.sin(time * 1.4f) * 0.05f;
        // dorsal spines behind the shoulders
        for (int i = -3; i <= 3; i++) {
            float s = 0.55f - Math.abs(i) * 0.08f;
            d.shape(b, d.triangle, cx + i * 0.6f, base + 3.1f - Math.abs(i) * 0.25f + breathe, s, 90f, Draw.OUTLINE_COLOR, 1f);
            d.shape(b, d.triangle, cx + i * 0.6f, base + 3.1f - Math.abs(i) * 0.25f + breathe, s * 0.8f, 90f, c(SPINE), 1f);
        }
        // the huge torso rising behind the roof
        d.circle(b, cx, base + 1.6f + breathe, 2.75f, Draw.OUTLINE_COLOR);
        d.circle(b, cx, base + 1.6f + breathe, 2.65f, c(SCALE));
        d.circle(b, cx - 0.9f, base + 2.4f + breathe, 0.9f, c(SCALE_LIGHT), 0.5f);
        d.ellipse(b, cx, base + 0.9f + breathe, 1.35f, 1.6f, c(BELLY), 1f);
        for (int i = 0; i < 4; i++) { // belly plates
            d.rect(b, cx - 1.1f + i * 0.08f, base + 0.2f + i * 0.45f + breathe, 2.2f - i * 0.16f, 0.04f, SCALE_DARK);
        }
        // shoulders and resting arms (hands gripping the roof's edge) unless they're slamming
        for (int side = -1; side <= 1; side += 2) {
            boolean busy = false;
            for (int i = 0; i < slamAt.length; i++) {
                if (fistOut(i) && ((i % 2 == 0) ? -1 : 1) == side) busy = true;
            }
            float sx = cx + side * 2.4f;
            d.circle(b, sx, base + 1.6f + breathe, 0.75f, c(SCALE_DARK));
            if (!busy) {
                d.line(b, sx, base + 1.5f, 1.4f, 0.5f, side < 0 ? 280f : 260f, c(SCALE));
                d.circle(b, sx + side * -0.25f, base + 0.15f, 0.4f, c(SCALE));
                for (int k = -1; k <= 1; k++) {
                    d.shape(b, d.triangle, sx + side * -0.25f + k * 0.2f, base - 0.18f, 0.12f, 270f, TOOTH, 1f);
                }
            }
        }
        drawHead(b, d);
    }

    private void drawHead(SpriteBatch b, Draw d) {
        float hx = pos.x;
        float hy = pos.y + 0.15f;
        boolean open = phase == Phase.LASER_CHARGE || phase == Phase.LASER || phase == Phase.ROAR
                || phase == Phase.BITE && biteLunge > 0.2f;
        // skull and snout
        d.circle(b, hx, hy + 0.15f, 0.86f, Draw.OUTLINE_COLOR);
        d.circle(b, hx, hy + 0.15f, 0.78f, c(SCALE));
        d.circle(b, hx - 0.3f, hy + 0.45f, 0.3f, c(SCALE_LIGHT), 0.6f);
        float jaw = open ? 0.28f : 0.05f;
        d.rect(b, hx - 0.55f, hy - 0.55f - jaw, 1.1f, 0.4f, c(SCALE_DARK)); // lower jaw
        if (open) {
            d.rect(b, hx - 0.48f, hy - 0.5f - jaw, 0.96f, 0.35f + jaw, MOUTH);
            if (phase == Phase.LASER_CHARGE || phase == Phase.LASER) { // the beam's glow in its throat
                float k = phase == Phase.LASER ? 1f : 1f - phaseTimer / LASER_CHARGE;
                d.circle(b, mouthX(), mouthY(), 0.15f + 0.25f * k, BEAM, 0.5f + 0.5f * k);
                d.circle(b, mouthX(), mouthY(), 0.08f + 0.12f * k, Color.WHITE);
            }
        }
        d.rect(b, hx - 0.6f, hy - 0.2f, 1.2f, 0.32f, c(SCALE)); // upper jaw
        for (int i = 0; i < 6; i++) { // teeth
            float tx = hx - 0.48f + i * 0.19f;
            d.shape(b, d.triangle, tx, hy - 0.26f, 0.08f, 270f, TOOTH, 1f);
            d.shape(b, d.triangle, tx + 0.09f, hy - 0.52f - jaw + 0.38f, 0.07f, 90f, TOOTH, 1f);
        }
        // nostrils and furious eyes
        d.circle(b, hx - 0.14f, hy + 0.02f, 0.04f, Draw.OUTLINE_COLOR);
        d.circle(b, hx + 0.14f, hy + 0.02f, 0.04f, Draw.OUTLINE_COLOR);
        Color eye = enraged ? Color.RED : EYE;
        for (int side = -1; side <= 1; side += 2) {
            float ex = hx + side * 0.38f;
            float ey = hy + 0.42f;
            d.circle(b, ex, ey, 0.13f, Draw.OUTLINE_COLOR);
            d.circle(b, ex, ey, 0.1f, eye);
            d.rect(b, ex - 0.02f, ey - 0.08f, 0.04f, 0.16f, Draw.OUTLINE_COLOR); // slit pupils
            d.rotRect(b, ex - side * 0.02f, ey + 0.15f, 0.24f, 0.06f, side * -18f, SCALE_DARK, 1f); // brow
        }
    }
}
