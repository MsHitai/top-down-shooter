package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * The secret sea's boss, Egor's design: the Fluffy Shark - the shark that sits on the Flufful,
 * but big, deep blue and very angry (beating it tames it, and it unlocks the Flufful).
 * <ul>
 *   <li>fin guns: a gun in each fin, firing in turns at you;</li>
 *   <li>tail wave: it whips its tail round and a wave rolls out across the lair - get caught in it and
 *       you're stunned for a moment (roll through it, or keep out of its way);</li>
 *   <li>the bite: it opens its jaws, lunges at you and snaps them shut - twice, three times enraged.</li>
 * </ul>
 * Enraged (below half health) the fins fire wider fans, the tail sends two waves, and every bite throws
 * teeth.
 */
public class FluffyShark extends Enemy {

    private enum Phase { SWIM, FINS, WAVE, BITE_AIM, BITE }

    private static final Color BLUE = Color.valueOf("4a8ad8");
    private static final Color BLUE_DARK = Color.valueOf("2e62a8");
    private static final Color BLUE_LIGHT = Color.valueOf("8fc4e8");
    private static final Color BELLY = Color.valueOf("f5f8ff");
    private static final Color FLUFF = Color.valueOf("b8dcf8");
    private static final Color MOUTH = Color.valueOf("8a1e3a");
    private static final Color GUM = Color.valueOf("e85a7a");
    private static final Color TOOTH = Color.WHITE;
    private static final Color BROW = Color.valueOf("1e1416");
    private static final Color FIN_SHOT = Color.valueOf("9ae0ff");
    private static final Color WAVE = Color.valueOf("8ad8ff");
    private static final Color FOAM = Color.valueOf("e8faff");
    private static final Color RAGE = Color.valueOf("ff3b3b");

    /** Stunned this long when the tail wave catches you. */
    private static final float STUN = 0.85f;
    private static final float WAVE_SPEED = 3.0f;
    private static final float WAVE_MAX = 8f;
    private static final float WAVE_THICKNESS = 0.16f;
    private static final float WAVE_WINDUP = 0.7f;
    private static final float BITE_SPEED = 6.0f;
    private static final float BITE_TIME = 0.55f;

    private Phase phase = Phase.SWIM;
    private float phaseTimer = 2f;
    private int patternIndex;
    private boolean enraged;
    private float time;
    private float orbit = MathUtils.random(360f);
    private float orbitDir = MathUtils.randomSign();
    private final Vector2 target = new Vector2();
    private final Vector2 step = new Vector2();
    private int shotsLeft;
    private float shotTimer;
    private boolean leftFin;
    /** Fin recoil, 1 -> 0, for each fin. */
    private float recoilFront;
    private float recoilBack;
    /** 0 = jaws shut, 1 = wide open. */
    private float jaw;
    private float biteAngle;
    private int bitesLeft;
    private int wavesLeft;
    /** Tail whip animation, 1 -> 0. */
    private float whip;
    /** Facing: 1 = right, -1 = left (it's drawn side on, like the shark on the Flufful). */
    private float face = 1f;
    private final Array<Wave> waves = new Array<>();

    /** A wave rolling out from where the tail whipped. */
    private static final class Wave {
        float x;
        float y;
        float r = 0.4f;
        boolean caught;
    }

    public FluffyShark(float x, float y, int tier) {
        super(x, y, 1100f + 220f * tier);
        radius = 0.55f;
        speed = 1.3f;
        score = 900 + 250 * tier;
        boss = true;
        flying = true; // it swims over the coral
    }

    @Override
    public String bossName() {
        return Boss.SHARK.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    @Override
    public float feetY() {
        return pos.y - 0.6f; // swimming above the sea bed
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        knock.setZero();
        recoilFront = Math.max(0f, recoilFront - delta * 5f);
        recoilBack = Math.max(0f, recoilBack - delta * 5f);
        whip = Math.max(0f, whip - delta * 2.5f);
        if (!enraged && hp < maxHp * 0.5f) {
            enraged = true;
            speed = 1.7f;
            world.shake(0.7f);
            world.sfx().play("boss", 1f);
            world.particles().burst(pos.x, pos.y, 30, 0.5f, 2.2f, 0.7f, 0.06f, FOAM);
        }
        float diff = world.getDifficulty();
        phaseTimer -= delta;
        updateWaves(delta, world);
        if (phase != Phase.BITE && phase != Phase.BITE_AIM) {
            jaw = Math.max(0f, jaw - delta * 4f);
            float want = MathUtils.cosDeg(aimAngle) >= 0 ? 1f : -1f;
            face += (want - face) * Math.min(1f, delta * 6f);
        }

        switch (phase) {
            case SWIM:
                swim(delta, world, 1f);
                if (phaseTimer <= 0) {
                    nextPattern(world);
                }
                break;
            case FINS:
                swim(delta, world, 0.4f);
                shotTimer -= delta;
                if (shotsLeft > 0 && shotTimer <= 0 && canShoot(world)) {
                    shotTimer = enraged ? 0.24f : 0.3f;
                    shotsLeft--;
                    finVolley(world, diff);
                }
                if (shotsLeft <= 0 && shotTimer <= 0) {
                    toSwim();
                }
                break;
            case WAVE:
                if (phaseTimer <= 0 && wavesLeft > 0) {
                    wavesLeft--;
                    whip = 1f;
                    Wave w = new Wave();
                    w.x = tailX();
                    w.y = pos.y - 0.1f;
                    waves.add(w);
                    world.sfx().play("swish", 1f);
                    world.shake(0.3f);
                    world.particles().burst(w.x, w.y, 20, 0.5f, 2f, 0.6f, 0.06f, FOAM);
                    phaseTimer = wavesLeft > 0 ? 0.45f : 0.5f;
                } else if (phaseTimer <= 0) {
                    // enraged, the wave's often followed by a bite while you're still seeing stars
                    if (enraged && MathUtils.randomBoolean(0.6f)) {
                        startBite(world, 2);
                    } else {
                        toSwim();
                    }
                }
                break;
            case BITE_AIM:
                jaw = Math.min(1f, jaw + delta * 3f);
                if (phaseTimer > 0.15f) {
                    biteAngle = aimAngle;
                    face = MathUtils.cosDeg(biteAngle) >= 0 ? 1f : -1f;
                }
                if (phaseTimer <= 0) {
                    phase = Phase.BITE;
                    phaseTimer = BITE_TIME;
                    world.sfx().play("dodge", 1f);
                }
                break;
            case BITE:
                float s = BITE_SPEED * Math.min(1.25f, diff) * delta;
                pos.add(MathUtils.cosDeg(biteAngle) * s, MathUtils.sinDeg(biteAngle) * s);
                Player p = world.getPlayer();
                float mouthX = pos.x + face * 0.55f;
                if (phaseTimer <= 0 || Vector2.dst(mouthX, pos.y, p.centerX(), p.hitboxY()) < 0.3f) {
                    snap(world, diff);
                }
                break;
        }
    }

    /** Circles the player at a distance, up and down a little - always keeping them in view. */
    private void swim(float delta, GameWorld world, float pace) {
        Player p = world.getPlayer();
        orbit += orbitDir * 25f * pace * delta;
        if (MathUtils.random() < delta * 0.2f) {
            orbitDir = -orbitDir;
        }
        float dist = enraged ? 2.4f : 2.8f;
        target.set(p.centerX() + MathUtils.cosDeg(orbit) * dist, p.hitboxY() + 0.3f + MathUtils.sinDeg(orbit) * dist * 0.6f);
        Arena.now.clamp(target, radius + 0.4f);
        step.set(target).sub(pos);
        float len = step.len();
        if (len > 0.02f) {
            step.scl(Math.min(len, speed * pace * delta) / len);
            pos.add(step);
        }
    }

    private void toSwim() {
        phase = Phase.SWIM;
        phaseTimer = enraged ? 0.9f : 1.4f;
        jaw = 0f;
    }

    private void nextPattern(GameWorld world) {
        Phase[] order = enraged
                ? new Phase[]{Phase.FINS, Phase.WAVE, Phase.BITE_AIM, Phase.FINS, Phase.BITE_AIM}
                : new Phase[]{Phase.FINS, Phase.BITE_AIM, Phase.FINS, Phase.WAVE};
        Phase next = order[patternIndex++ % order.length];
        switch (next) {
            case FINS:
                phase = Phase.FINS;
                shotsLeft = enraged ? 10 : 8;
                shotTimer = 0.3f;
                break;
            case WAVE:
                phase = Phase.WAVE;
                phaseTimer = WAVE_WINDUP;
                wavesLeft = enraged ? 2 : 1;
                world.sfx().play("buzz", 0.5f);
                break;
            default:
                startBite(world, enraged ? 3 : 2);
                break;
        }
    }

    private void startBite(GameWorld world, int bites) {
        bitesLeft = bites;
        phase = Phase.BITE_AIM;
        phaseTimer = enraged ? 0.45f : 0.6f;
    }

    /** One fin fires: a little fan aimed at the player (wider when enraged). */
    private void finVolley(GameWorld world, float diff) {
        leftFin = !leftFin;
        float fx = finX(leftFin);
        float fy = finY();
        if (leftFin) recoilBack = 1f;
        else recoilFront = 1f;
        Player p = world.getPlayer();
        float a = MathUtils.atan2Deg360(p.hitboxY() - fy, p.centerX() - fx);
        int n = enraged ? 5 : 3;
        float spread = enraged ? 14f : 11f;
        for (int i = 0; i < n; i++) {
            Bullet b = world.spawnEnemyBullet(fx, fy, a + (i - (n - 1) / 2f) * spread, 2.7f * diff);
            b.color.set(FIN_SHOT);
        }
        world.sfx().play("enemy_shoot", 0.6f);
    }

    /** The jaws snap shut: a bite (if the player's in them) - and when enraged, a spray of teeth. */
    private void snap(GameWorld world, float diff) {
        jaw = 0f;
        world.sfx().play("break", 0.8f);
        world.shake(0.3f);
        Player p = world.getPlayer();
        float mouthX = pos.x + face * 0.55f;
        if (Vector2.dst(mouthX, pos.y, p.centerX(), p.hitboxY()) < 0.45f) {
            world.hurtPlayer();
        }
        if (enraged) {
            for (int i = 0; i < 10; i++) {
                Bullet b = world.spawnEnemyBullet(mouthX, pos.y, biteAngle + i * 36f, 2.2f * diff);
                b.shape = Bullet.Shape.TRIANGLE;
                b.color.set(TOOTH);
            }
        }
        if (--bitesLeft > 0) {
            phase = Phase.BITE_AIM;
            phaseTimer = enraged ? 0.35f : 0.45f;
        } else {
            toSwim();
        }
    }

    private void updateWaves(float delta, GameWorld world) {
        Player p = world.getPlayer();
        for (int i = waves.size - 1; i >= 0; i--) {
            Wave w = waves.get(i);
            w.r += WAVE_SPEED * delta;
            if (!w.caught) {
                float d = Vector2.dst(w.x, w.y, p.centerX(), p.hitboxY());
                if (Math.abs(d - w.r) < WAVE_THICKNESS + Player.HITBOX_RADIUS && !p.isRolling() && !p.isInvulnerable()) {
                    w.caught = true; // one stun per wave
                    world.stunPlayer(STUN);
                }
            }
            if (w.r > WAVE_MAX) {
                waves.removeIndex(i);
            }
        }
    }

    private float tailX() {
        return pos.x - face * 0.8f;
    }

    /** The front fin (nearer the head) or the back one; both hang under the body. */
    private float finX(boolean back) {
        return pos.x + face * (back ? -0.12f : 0.22f);
    }

    private float finY() {
        return pos.y - 0.3f;
    }

    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        for (Wave w : waves) { // a ring of rolling water with foam on its crest
            float fade = 1f - w.r / WAVE_MAX;
            d.ring(b, w.x, w.y, w.r + 0.05f, WAVE, 0.5f * fade + 0.2f);
            d.ring(b, w.x, w.y, w.r, FOAM, 0.8f * fade + 0.2f);
            d.ring(b, w.x, w.y, w.r - 0.06f, WAVE, 0.6f * fade);
        }
    }

    @Override
    public void drawOverlay(SpriteBatch b, Draw d, com.badlogic.gdx.math.Rectangle view) {
        if (phase == Phase.BITE_AIM) { // where it's about to lunge
            d.line(b, pos.x, pos.y, 3.2f, 0.08f, biteAngle, RAGE, 0.25f);
        }
        if (phase == Phase.WAVE && wavesLeft > 0) { // the tail winds up, glowing
            float k = 1f - Math.max(0f, phaseTimer) / WAVE_WINDUP;
            d.circle(b, tailX(), pos.y - 0.05f, 0.25f + 0.2f * k, FOAM, 0.2f + 0.3f * k);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float f = face >= 0 ? 1f : -1f;
        float x = pos.x;
        float y = pos.y + MathUtils.sin(time * 2f) * 0.04f;
        float swish = MathUtils.sin(time * 5f) * 12f + whip * 50f * MathUtils.sin(time * 30f);

        // the tail fin: a big crescent, swishing (whipping round for the wave)
        float tx = x - f * 0.72f;
        d.rotRect(b, tx - f * 0.08f, y + 0.12f, 0.36f, 0.14f, f * (50f + swish), Draw.OUTLINE_COLOR, 1f);
        d.rotRect(b, tx - f * 0.08f, y + 0.12f, 0.33f, 0.11f, f * (50f + swish), c(BLUE_DARK), 1f);
        d.rotRect(b, tx - f * 0.06f, y - 0.1f, 0.28f, 0.12f, f * (-45f + swish), Draw.OUTLINE_COLOR, 1f);
        d.rotRect(b, tx - f * 0.06f, y - 0.1f, 0.25f, 0.09f, f * (-45f + swish), c(BLUE_DARK), 1f);
        // the back fin gun (behind the body)
        drawFin(b, d, true);
        // the dorsal fin
        // (the triangle texture points right at 0 degrees: 90 is up, leaning back the way it came)
        d.shape(b, d.triangle, x - f * 0.1f, y + 0.42f, 0.2f, 90f + f * 15f, Draw.OUTLINE_COLOR, 1f);
        d.shape(b, d.triangle, x - f * 0.1f, y + 0.4f, 0.17f, 90f + f * 15f, c(BLUE_DARK), 1f);
        // the big round body, like the little one on the gun
        d.ellipse(b, x, y, 0.72f, 0.4f, Draw.OUTLINE_COLOR, 1f);
        d.ellipse(b, x, y, 0.69f, 0.37f, c(BLUE), 1f);
        d.ellipse(b, x - f * 0.05f, y + 0.14f, 0.55f, 0.18f, c(BLUE_LIGHT), 0.5f);
        d.ellipse(b, x + f * 0.05f, y - 0.16f, 0.55f, 0.18f, c(BELLY), 1f);
        // fluffy tufts along its back - the Fluffy Shark
        for (int i = 0; i < 7; i++) {
            float k = (i - 3f) / 3f;
            float fx = x + k * 0.5f;
            float fy = y + 0.33f - k * k * 0.12f + MathUtils.sin(time * 4f + i) * 0.01f;
            d.circle(b, fx, fy, 0.06f, c(FLUFF));
        }
        // gill slits
        for (int i = 0; i < 3; i++) {
            d.rotRect(b, x + f * (0.12f - i * 0.07f), y + 0.02f, 0.02f, 0.14f, f * 10f, BLUE_DARK, 1f);
        }
        // the mouth: shut, a zig-zag of teeth; open, a wide red maw full of them
        float mx = x + f * 0.42f;
        float my = y - 0.08f;
        if (jaw > 0.05f) {
            float open = 0.24f * jaw;
            d.ellipse(b, mx, my, 0.22f, open, MOUTH, 1f);
            d.ellipse(b, mx - f * 0.02f, my - open * 0.4f, 0.14f, open * 0.35f, GUM, 1f);
            for (int i = 0; i < 5; i++) {
                float tx2 = mx - 0.16f + i * 0.08f;
                d.shape(b, d.triangle, tx2, my + open - 0.03f, 0.035f, 270f, TOOTH, 1f);
                d.shape(b, d.triangle, tx2 + 0.04f, my - open + 0.03f, 0.035f, 90f, TOOTH, 1f);
            }
        } else {
            d.rect(b, mx - 0.18f, my - 0.01f, 0.36f, 0.025f, BROW);
            for (int i = 0; i < 6; i++) {
                d.shape(b, d.triangle, mx - 0.15f + i * 0.06f, my + (i % 2 == 0 ? -0.03f : 0.03f), 0.025f,
                        i % 2 == 0 ? 90f : 270f, TOOTH, 1f);
            }
        }
        // the eye, glaring under a furious brow (red once enraged)
        float ex = x + f * 0.36f;
        float ey = y + 0.12f;
        d.circle(b, ex, ey, 0.09f, Draw.OUTLINE_COLOR);
        d.circle(b, ex, ey, 0.075f, c(EYE_WHITE));
        float lx = MathUtils.cosDeg(aimAngle) * 0.025f;
        float ly = MathUtils.sinDeg(aimAngle) * 0.025f;
        d.circle(b, ex + lx, ey + ly, 0.04f, enraged ? RAGE : PUPIL);
        d.circle(b, ex + lx - 0.012f, ey + ly + 0.012f, 0.012f, Color.WHITE);
        d.rotRect(b, ex - f * 0.01f, ey + 0.1f, 0.2f, 0.05f, f * -22f, BROW, 1f);
        // the front fin gun (in front of the body)
        drawFin(b, d, false);
        if (enraged && MathUtils.random() < 0.3f) { // fuming: bubbles of rage
            d.circle(b, x + MathUtils.random(-0.3f, 0.3f), y + 0.45f + MathUtils.random(0.1f), 0.03f, FOAM, 0.8f);
        }
    }

    /** A pectoral fin with a gun strapped in it, pointing at the player; kicks back when it fires. */
    private void drawFin(SpriteBatch b, Draw d, boolean back) {
        float f = face >= 0 ? 1f : -1f;
        float fx = finX(back);
        float fy = finY() + MathUtils.sin(time * 2f) * 0.04f;
        float recoil = back ? recoilBack : recoilFront;
        float fin = f * (back ? -35f : -25f);
        d.rotRect(b, fx - f * 0.05f, fy, 0.3f, 0.13f, fin, Draw.OUTLINE_COLOR, 1f);
        d.rotRect(b, fx - f * 0.05f, fy, 0.27f, 0.1f, fin, c(back ? BLUE_DARK : BLUE), 1f);
        float gunLen = 0.3f - recoil * 0.06f;
        d.line(b, fx, fy, gunLen, 0.09f, aimAngle, Draw.OUTLINE_COLOR);
        d.line(b, fx, fy, gunLen - 0.01f, 0.065f, aimAngle, GUN);
        if (recoil > 0.6f) { // muzzle flash
            float mx = fx + MathUtils.cosDeg(aimAngle) * gunLen;
            float my = fy + MathUtils.sinDeg(aimAngle) * gunLen;
            d.circle(b, mx, my, 0.08f, FIN_SHOT, 0.9f);
            d.circle(b, mx, my, 0.04f, Color.WHITE);
        }
    }
}
