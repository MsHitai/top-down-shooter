package com.trush.game.enemy;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.BufferUtils;
import com.trush.game.gfx.BossArt.Part;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

import java.nio.IntBuffer;

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

    private static final Color SCALE = Color.valueOf("344343");
    private static final Color SCALE_DARK = Color.valueOf("202c30");
    private static final Color SCALE_LIGHT = Color.valueOf("53625c");
    private static final Color BELLY = Color.valueOf("626c62");
    private static final Color SPINE = Color.valueOf("86aaa5");
    private static final Color FURY_EYE = Color.valueOf("ff773d");
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
    private final Color plateLight = new Color();
    // laser
    private float laserFrom;
    private float laserTo;
    private float laserAngle;
    private float laserLength;
    private int sweepsLeft;
    // slams: one per arm - [0] is the left (far) arm, [1] the right (near) one; it has two arms, no more
    private final Vector2[] slamAt = {new Vector2(), new Vector2()};
    private final float[] slamTimer = new float[2];
    /** After a fist lands it rests on the roof a moment before lifting away. */
    private final float[] slamLinger = new float[2];
    /** The arm that slams next (they take turns). */
    private int nextArm;
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
        return pos.y - 0.45f;
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
        int i = !fistOut(nextArm) ? nextArm : !fistOut(1 - nextArm) ? 1 - nextArm : -1;
        if (i < 0) {
            return; // both arms busy: wait for one to come back
        }
        Player p = world.getPlayer();
        float spread = slamsLeft == (enraged ? 5 : 3) ? 0f : 1.2f;
        slamAt[i].set(p.centerX() + MathUtils.random(-spread, spread), p.hitboxY() + MathUtils.random(-spread, spread) * 0.6f);
        Arena.now.clamp(slamAt[i], SLAM_RADIUS * 0.6f);
        slamTimer[i] = SLAM_WARN;
        slamsLeft--;
        nextArm = 1 - i;
        nextSlam = enraged ? 0.38f : 0.55f;
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
        if (fistOut(0) || fistOut(1)) {
            d.actors.begin(b, pos.x, pos.y, radius, getClass().getSimpleName());
            try {
                for (int i = 0; i < slamAt.length; i++) {
                    if (fistOut(i)) drawFist(b, d, i, true);
                }
            } finally {
                d.actors.end(b, flash > 0);
            }
        }
    }

    /** A slamming arm: raised over its target, growing as it drops. {@code pawOnly}: just the paw. */
    private void drawFist(SpriteBatch b, Draw d, int i, boolean pawOnly) {
        float x = slamAt[i].x;
        float y = slamAt[i].y;
        float scale = 1f;
        if (slamTimer[i] > 0) {
            float k = 1f - slamTimer[i] / SLAM_WARN;
            y += (1f - k * k) * 2.2f;
            scale = 0.75f + 0.25f * k;
        }
        drawArm(b, d, i == 0 ? -1 : 1, x, y, scale, 0f, pawOnly);
    }

    /**
     * A creature's arm from its shoulder to a paw at (px, py): a thick upper arm, a bent elbow, a
     * tapering forearm, and a scaly paw with three big claws pointing the way it reaches.
     */
    private void drawArm(SpriteBatch b, Draw d, int side, float px, float py, float scale, float breathe, boolean pawOnly) {
        float sx = shoulderX(side);
        float sy = shoulderY(side) + breathe;
        // the elbow bends outward, away from the body
        float mx = (sx + px) / 2f;
        float my = (sy + py) / 2f;
        float ex = mx + side * 0.40f * scale;
        float ey = my + 0.3f * scale;
        // outlines first (the whole arm), then the scaly fill on top, so the joints blend
        for (int pass = 0; pass < 2; pass++) {
            if (!pawOnly) {
                limb(b, d, sx, sy, ex, ey, 0.46f * scale, 0.32f * scale, pass);   // muscly upper arm
                limb(b, d, ex, ey, px, py, 0.3f * scale, 0.22f * scale, pass);    // forearm, tapering to the wrist
            }
            float r = 0.38f * scale;
            d.circle(b, px, py, pass == 0 ? r + 0.06f : r, pass == 0 ? Draw.OUTLINE_COLOR : c(SCALE));
        }
        // the paw: knuckles, and three big claws pointing the way it reaches
        float fore = MathUtils.atan2Deg360(py - ey, px - ex);
        float r = 0.38f * scale;
        d.bosses.draw(b, Part.KAIJU_SCALES, px, py, r * 1.70f, r * 1.66f,
                fore - 90f, side < 0, flash > 0, c(Color.WHITE));
        for (int k = -1; k <= 1; k++) {
            float a = fore + k * 30f;
            d.circle(b, px + MathUtils.cosDeg(a) * r * 0.6f, py + MathUtils.sinDeg(a) * r * 0.6f, r * 0.32f, c(SCALE_DARK));
            float cx = px + MathUtils.cosDeg(a) * r * 1.1f;
            float cy = py + MathUtils.sinDeg(a) * r * 1.1f;
            d.shape(b, d.triangle, cx, cy, 0.2f * scale, a, Draw.OUTLINE_COLOR, 1f);
            d.shape(b, d.triangle, cx, cy, 0.15f * scale, a, TOOTH, 1f);
        }
    }

    /** A continuous tapered muscle with rounded joints; avoids the old beaded, noodle-like arms. */
    private void limb(SpriteBatch b, Draw d, float x0, float y0, float x1, float y1, float r0, float r1, int pass) {
        float len = Math.max(0.001f, Vector2.dst(x0, y0, x1, y1));
        float nx = -(y1 - y0) / len;
        float ny = (x1 - x0) / len;
        float a = r0 + (pass == 0 ? 0.055f : 0);
        float z = r1 + (pass == 0 ? 0.055f : 0);
        Color skin = pass == 0 ? Draw.OUTLINE_COLOR : c(SCALE);
        d.quad(b, x0 + nx * a, y0 + ny * a, x0 - nx * a, y0 - ny * a,
                x1 - nx * z, y1 - ny * z, x1 + nx * z, y1 + ny * z, skin);
        d.circle(b, x0, y0, a, skin);
        d.circle(b, x1, y1, z, skin);
        if (pass == 1) {
            d.line(b, x0 + nx * r0 * 0.45f, y0 + ny * r0 * 0.45f, len * 0.8f,
                    0.045f, MathUtils.atan2Deg360(y1 - y0, x1 - x0), c(SCALE_LIGHT), 0.55f);
        }
    }

    /** Its body leans in over the roof from behind and to the left (as Egor drew it). */
    private float bodyX() {
        return homeX + (pos.x - homeX) * 0.4f - 0.75f;
    }

    /** Where an arm starts: the far (left) shoulder or the near (right) one. */
    private float shoulderX(int side) {
        return side < 0 ? bodyX() - 1.0f : bodyX() + 2.15f;
    }

    private float shoulderY(int side) {
        return Arena.now.top + (side < 0 ? 1.15f : 0.9f);
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        Arena a = Arena.now;
        float bx = bodyX();
        float base = a.top + 0.1f;
        float breathe = MathUtils.sin(time * 1.4f) * 0.05f;
        // its body rises from behind the building: everything below the roof's far edge is hidden
        // (only the head, the neck and the arms lean out over the roof)
        clipBelow(b, base - 0.05f);
        // the tail, curling away to the left, with a ridge of spikes
        int segs = 9;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = segs; i >= 0; i--) {
                float t = i / (float) segs;
                float tx = bx - 1.8f - 3.4f * t;
                float ty = base + 0.9f + 0.7f * t + MathUtils.sin(t * MathUtils.PI) * 0.45f + MathUtils.sin(time + t * 3f) * 0.08f * t;
                float r = 0.8f * (1f - t) + 0.16f;
                if (pass == 0) {
                    d.circle(b, tx, ty, r + 0.08f, Draw.OUTLINE_COLOR);
                } else {
                    d.circle(b, tx, ty, r, c(SCALE));
                    if (i % 2 == 0 && i < segs) {
                        spike(b, d, tx, ty + r * 0.55f, 0.18f + 0.3f * (1f - t), 100f + 20f * t);
                    }
                }
            }
        }
        // torso: hips behind, chest leaning in over the roof
        float hipX = bx - 0.5f;
        float hipY = base + 1.9f + breathe;
        float chestX = bx + 0.2f;
        float chestY = base + 0.75f + breathe;
        float shoulderX = shoulderX(1) - 0.35f; // the near shoulder's bulge, so that arm joins the body
        float shoulderY = base + 1.0f + breathe;
        // Heavy upright haunches and a hunched chest, not a long horizontal lizard tube.
        d.ellipse(b, bx - 0.95f, base + 0.25f, 0.95f, 1.45f, Draw.OUTLINE_COLOR, 1f);
        d.ellipse(b, bx + 1.1f, base + 0.25f, 1.0f, 1.4f, Draw.OUTLINE_COLOR, 1f);
        d.ellipse(b, bx - 0.95f, base + 0.25f, 0.87f, 1.38f, c(SCALE_DARK), 1f);
        d.ellipse(b, bx + 1.1f, base + 0.25f, 0.92f, 1.32f, c(SCALE_DARK), 1f);
        d.ellipse(b, hipX, hipY, 1.48f, 1.85f, Draw.OUTLINE_COLOR, 1f);
        d.circle(b, chestX, chestY, 1.68f, Draw.OUTLINE_COLOR);
        d.circle(b, shoulderX, shoulderY, 0.98f, Draw.OUTLINE_COLOR);
        d.ellipse(b, hipX, hipY, 1.4f, 1.77f, c(SCALE), 1f);
        d.circle(b, chestX, chestY, 1.6f, c(SCALE));
        d.circle(b, shoulderX, shoulderY, 0.9f, c(SCALE));
        d.bosses.draw(b, Part.KAIJU_SCALES, hipX, hipY + 0.4f, 1.75f, 1.65f, false, flash > 0, c(Color.WHITE));
        d.bosses.draw(b, Part.KAIJU_SCALES, chestX - 0.38f, chestY + 0.6f, 1.48f, 1.25f, false, flash > 0, c(Color.WHITE));
        d.ellipse(b, chestX + 0.5f, base + 0.2f + breathe, 0.95f, 0.6f, c(BELLY), 1f);
        for (int i = 0; i < 3; i++) { // belly ridges
            d.rect(b, chestX - 0.25f + i * 0.08f, base - 0.15f + i * 0.22f + breathe, 1.5f - i * 0.16f, 0.035f, SCALE_DARK);
        }
        endClip(b);
        // the far arm, hand gripping the roof's edge (unless that fist is up for a slam)
        if (!armBusy(-1)) {
            restingArm(b, d, -1, shoulderX(-1) + 0.15f, base + 0.55f, breathe);
        }
        clipBelow(b, base - 0.05f);
        // the spikes down its back: two jagged rows along the spine, biggest at the shoulders
        for (int i = 0; i < 7; i++) {
            float t = i / 6f;
            float sx = bx + 0.4f - 2.0f * t;
            float sy = base + 1.05f + 1.3f * t + breathe;
            float size = 0.82f - 0.38f * t;
            spike(b, d, sx - 0.22f, sy + 0.05f, size, 110f);
            spike(b, d, sx + 0.22f, sy - 0.1f, size * 0.85f, 75f);
        }
        endClip(b);
        // A muscular taper from the chest to the skull, with the crest on the far side.
        float neckX = pos.x;
        float neckY = pos.y + 0.62f;
        for (int i = 0; i < 4; i++) {
            float t = i / 3f;
            spike(b, d, MathUtils.lerp(neckX - 0.44f, chestX - 0.5f, t),
                    MathUtils.lerp(neckY + 0.2f, chestY + 0.6f, t), 0.25f + t * 0.16f, 135f);
        }
        for (int pass = 0; pass < 2; pass++) {
            limb(b, d, chestX + 0.55f, chestY + 0.2f, neckX, neckY, 0.78f, 0.43f, pass);
        }
        // the near arm
        if (!armBusy(1)) {
            restingArm(b, d, 1, shoulderX(1) - 0.15f, base + 0.55f, breathe);
        }
        for (int i = 0; i < slamAt.length; i++) { // slamming arms reach out under its head (the paws go on top later)
            if (fistOut(i)) {
                drawFist(b, d, i, false);
            }
        }
        drawHead(b, d);
    }

    private static final IntBuffer GL_VIEW = BufferUtils.newIntBuffer(16);
    private static final Matrix4 CLIP_M = new Matrix4();
    private static final Vector3 CLIP_V = new Vector3();

    /** Stops drawing anything below world height {@code y} (until endClip), with the GL scissor. */
    private static void clipBelow(SpriteBatch b, float y) {
        b.flush();
        Gdx.gl.glGetIntegerv(GL20.GL_VIEWPORT, GL_VIEW);
        int vx = GL_VIEW.get(0);
        int vy = GL_VIEW.get(1);
        int vw = GL_VIEW.get(2);
        int vh = GL_VIEW.get(3);
        CLIP_M.set(b.getProjectionMatrix()).mul(b.getTransformMatrix());
        CLIP_V.set(0, y, 0).prj(CLIP_M);
        int py = MathUtils.clamp(Math.round(vy + (CLIP_V.y + 1f) / 2f * vh), vy, vy + vh);
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(vx, py, vw, vy + vh - py);
    }

    private static void endClip(SpriteBatch b) {
        b.flush();
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    /** Three-toothed dorsal plates, lighting up from the back before the atomic breath. */
    private void spike(SpriteBatch b, Draw d, float x, float y, float size, float angle) {
        float energy = phase == Phase.LASER ? 1f : phase == Phase.LASER_CHARGE
                ? MathUtils.clamp(1f - phaseTimer / LASER_CHARGE, 0, 1) : 0f;
        plateLight.set(SPINE).lerp(BEAM, energy * 0.9f);
        for (int pass = 0; pass < 2; pass++) {
            Color ink = pass == 0 ? Draw.OUTLINE_COLOR : c(plateLight);
            float k = pass == 0 ? 1.12f : 1f;
            d.shape(b, d.triangle, x, y, size * k, angle, ink, 1f);
            for (int side = -1; side <= 1; side += 2) {
                float a = angle + side * 42f;
                float px = x + MathUtils.cosDeg(a) * size * 0.24f;
                float py = y + MathUtils.sinDeg(a) * size * 0.24f;
                d.shape(b, d.triangle, px, py, size * 0.58f * k, a, ink, 1f);
            }
        }
        d.line(b, x, y, size * 0.62f, size * 0.10f, angle, SCALE_LIGHT, 0.7f);
    }

    private boolean armBusy(int side) {
        for (int i = 0; i < slamAt.length; i++) {
            if (fistOut(i) && ((i % 2 == 0) ? -1 : 1) == side) return true;
        }
        return false;
    }

    /** An arm reaching down to the roof's edge, its claws hooked over it. */
    private void restingArm(SpriteBatch b, Draw d, int side, float handX, float base, float breathe) {
        d.circle(b, shoulderX(side), shoulderY(side) + breathe, 0.62f, c(SCALE_DARK));
        drawArm(b, d, side, handX, base + 0.1f, 0.85f, breathe, false);
    }

    /** Just the head for a close-up: its body only makes sense rising from behind a roof. */
    @Override
    public void drawPortrait(SpriteBatch b, Draw d, float aim) {
        aimAngle = aim;
        d.actors.begin(b, pos.x, pos.y, radius, getClass().getSimpleName());
        try {
            drawHead(b, d);
        } finally {
            d.actors.end(b, flash > 0);
        }
    }

    /** A blunt, elongated reptile muzzle, small deep-set eyes and a muscular lower jaw. */
    private void drawHead(SpriteBatch b, Draw d) {
        float hx = pos.x;
        float hy = pos.y + 0.15f;
        boolean open = phase == Phase.LASER_CHARGE || phase == Phase.LASER || phase == Phase.ROAR
                || phase == Phase.BITE && biteLunge > 0.2f;
        float jaw = open ? 0.28f : 0.025f;
        // Neck crest continues the large back plates; no dragon horns.
        for (int i = -1; i <= 1; i++) {
            spike(b, d, hx + i * 0.28f, hy + 0.75f - Math.abs(i) * 0.1f,
                    i == 0 ? 0.34f : 0.22f, 90f + i * 20f);
        }
        // Heavy lower jaw opens around the unchanged beam origin.
        d.ellipse(b, hx, hy - 0.62f - jaw, 0.52f, 0.25f, Draw.OUTLINE_COLOR, 1f);
        d.ellipse(b, hx, hy - 0.61f - jaw, 0.46f, 0.19f, c(SCALE_DARK), 1f);
        d.ellipse(b, hx, hy - 0.51f - jaw * 0.45f, 0.44f, 0.10f + jaw * 0.72f, MOUTH, 1f);
        if (open) {
            for (int i = 0; i < 7; i++) {
                d.shape(b, d.triangle, hx - 0.34f + i * 0.113f, hy - 0.58f - jaw,
                        i == 0 || i == 6 ? 0.09f : 0.065f, 90, TOOTH, 1f);
            }
        }
        // Powerful cheeks, a low skull, and a long tapering bridge to the broad nose.
        for (int pass = 0; pass < 2; pass++) {
            float rim = pass == 0 ? 0.055f : 0;
            Color skin = pass == 0 ? Draw.OUTLINE_COLOR : c(SCALE);
            d.ellipse(b, hx, hy + 0.3f, 0.59f + rim, 0.57f + rim, skin, 1f);
            d.ellipse(b, hx - 0.43f, hy - 0.02f, 0.24f + rim, 0.34f + rim, skin, 1f);
            d.ellipse(b, hx + 0.43f, hy - 0.02f, 0.24f + rim, 0.34f + rim, skin, 1f);
            d.quad(b, hx - 0.45f - rim, hy + 0.16f, hx - 0.39f - rim, hy - 0.49f,
                    hx + 0.39f + rim, hy - 0.49f, hx + 0.45f + rim, hy + 0.16f, skin);
            d.ellipse(b, hx, hy - 0.42f, 0.43f + rim, 0.18f + rim, skin, 1f);
        }
        // Broad connected scale planes, cheek folds and a raised nasal ridge.
        d.quad(b, hx - 0.14f, hy + 0.45f, hx - 0.12f, hy - 0.33f,
                hx + 0.10f, hy - 0.33f, hx + 0.12f, hy + 0.45f, c(SCALE_LIGHT));
        for (int side = -1; side <= 1; side += 2) {
            d.line(b, hx + side * 0.4f, hy - 0.12f, 0.25f, 0.035f, side < 0 ? 245f : 295f, SCALE_DARK);
            d.rotRect(b, hx + side * 0.22f, hy - 0.39f, 0.085f, 0.035f,
                    side * 18f, Draw.OUTLINE_COLOR, 1f);
        }
        for (int side = -1; side <= 1; side += 2) {
            d.bosses.draw(b, Part.KAIJU_SCALES, hx + side * 0.32f, hy + 0.05f,
                    0.36f, 0.30f, side < 0, flash > 0, c(Color.WHITE));
        }
        // Teeth stay partly behind the lip at rest instead of forming a permanent white grin.
        if (open) {
            for (int i = 0; i < 7; i++) {
                d.shape(b, d.triangle, hx - 0.34f + i * 0.113f, hy - 0.57f,
                        i == 0 || i == 6 ? 0.085f : 0.06f, 270, TOOTH, 1f);
            }
        } else {
            d.line(b, hx - 0.34f, hy - 0.55f, 0.68f, 0.035f, 0, Draw.OUTLINE_COLOR);
        }
        Color eye = enraged ? FURY_EYE : EYE;
        for (int side = -1; side <= 1; side += 2) {
            float ex = hx + side * 0.37f;
            float ey = hy + 0.27f;
            d.ellipse(b, ex, ey, 0.13f, 0.085f, Draw.OUTLINE_COLOR, 1f);
            d.ellipse(b, ex, ey, 0.095f, 0.055f, eye, 1f);
            d.rect(b, ex - 0.015f, ey - 0.05f, 0.03f, 0.1f, Draw.OUTLINE_COLOR);
            d.rotRect(b, ex - side * 0.015f, ey + 0.09f, 0.30f, 0.085f,
                    side * 16f, c(SCALE_DARK), 1f);
        }
        if (phase == Phase.LASER_CHARGE || phase == Phase.LASER) {
            float k = phase == Phase.LASER ? 1f : MathUtils.clamp(1f - phaseTimer / LASER_CHARGE, 0, 1);
            d.circle(b, mouthX(), mouthY(), 0.13f + 0.16f * k, BEAM, 0.4f + 0.5f * k);
            d.circle(b, mouthX(), mouthY(), 0.06f + 0.08f * k, Color.WHITE, 0.6f + 0.4f * k);
        }
    }
}
