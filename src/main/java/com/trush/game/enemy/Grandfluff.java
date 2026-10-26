package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.trush.game.gfx.Draw;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;

/**
 * The Moon's boss and the true ending (the owner's idea): Grandfluff, the Fluffy King's grandfather - a good
 * old king gone grumpy, up in his castle on the Moon. A fight in three acts:
 * <ul>
 *   <li><b>the wise old king</b> - silver fluff, a long beard, spectacles and a walking stick: he draws
 *       constellations (stars appear, then light up into lines of bullets that drift apart), slow
 *       beautiful spirals, and swipes his stick in a crescent when you come close;</li>
 *   <li>at half health he has a snack of moon cheese and is <b>young again</b>: golden fluff, no stick, no
 *       glasses - quick hops that land in rings with a gap, comets with sparkling tails, faster spirals;</li>
 *   <li>when the young king's health runs out he doesn't fall: he floats up over the castle and <b>becomes the
 *       Moon</b> with a whole new life (the owner's call: his boss bar is gold over red, like the hero's golden
 *       hearts - the gold is the old and young king's, the red under it the Moon's): a giant moon face looming over
 *       the arena (the camera pulls back), sliding along the top after you - craters spitting rings, beams
 *       from his eyes sweeping across where you stand, his constellations again, slow moon-sized hops that
 *       land in rings (the owner's ideas, after playing it: the sides were too safe), and his mouth breathing
 *       in, pulling you and a stream of stardust towards it.</li>
 * </ul>
 */
public class Grandfluff extends Enemy {

    private enum Act { WISE, YOUNG, MOON }

    private enum Phase { WANDER, STARS, SPIRAL, CANE, SNACK, HOPS, COMETS, RISE, CRATERS, BEAMS, PULL, MOON_HOPS }

    private static final Color SILVER = Color.valueOf("eceef6");
    private static final Color SILVER_SHADE = Color.valueOf("b8bccc");
    private static final Color GOLDEN = Color.valueOf("ffe6a8");
    private static final Color GOLDEN_SHADE = Color.valueOf("f2b860");
    private static final Color ROBE = Color.valueOf("3a3a8a");
    private static final Color ROBE_STAR = Color.valueOf("ffe6a0");
    private static final Color BEARD = Color.valueOf("fbfbff");
    private static final Color CROWN = Color.valueOf("d8dce8");
    private static final Color CROWN_GOLD = Color.valueOf("ffc83d");
    private static final Color MOON_GEM = Color.valueOf("fff3b0");
    private static final Color STICK = Color.valueOf("8a5a30");
    private static final Color CHEESE = Color.valueOf("f2c84a");
    private static final Color FACE = Color.valueOf("e8e4d0");
    private static final Color FACE_SHADE = Color.valueOf("c4bea4");
    private static final Color CRATER = Color.valueOf("aaa48a");
    private static final Color MOUTH = Color.valueOf("3a2a3a");
    private static final Color STAR_BULLET = Color.valueOf("ffe066");
    private static final Color LINE_BULLET = Color.valueOf("bfe0ff");
    private static final Color SPIRAL_BULLET = Color.valueOf("c8a8ff");
    private static final Color CANE_BULLET = Color.valueOf("f0d090");
    private static final Color RING_BULLET = Color.valueOf("ffb4dc");
    private static final Color COMET = Color.valueOf("ffb43a");
    private static final Color DUST = Color.valueOf("e0e8ff");
    private static final Color BEAM = Color.valueOf("ff5a5a");

    /** His body is drawn this much bigger than a regular enemy's (like his grandson). */
    private static final float S = 1.8f;
    /** He's young again below this share of his (first) health bar. */
    private static final float YOUNG_AT = 0.5f;
    private static final float SNACK = 2.6f;
    private static final float RISE = 3.2f;
    private static final float STARS_WARN = 1.4f;
    private static final float HOP_AIR = 0.7f;
    private static final float HOP_RADIUS = 0.75f;
    private static final float COMET_TIME = 1.6f;
    /** The moon face: how far below the arena's top edge its middle hangs, and how big it is. */
    private static final float FACE_DROP = 1.0f;
    private static final float FACE_R = 2.0f;
    private static final float BEAM_WARN = 0.9f;
    private static final float BEAM_TIME = 2.0f;
    private static final float BEAM_WIDTH = 0.16f;
    private static final float PULL_TIME = 3.4f;
    private static final float PULL_SPEED = 1.15f;
    /** How fast the moon face slides along the top after you. */
    private static final float FACE_FOLLOW = 1.2f;
    /** The moon's hops: slower than the young king's, and higher. */
    private static final float MOON_HOP_AIR = 1.0f;
    private static final float MOON_HOP_LIFT = 1.4f;

    private static final float[][] PUFFS = {
            {-0.28f, 0.05f}, {0.28f, 0.05f}, {-0.2f, 0.24f}, {0.2f, 0.24f},
            {-0.2f, -0.17f}, {0.2f, -0.17f}, {0f, 0.3f}, {0f, -0.22f},
    };
    /** Craters on the moon face that spit rings (offsets from its middle). */
    private static final float[][] CRATERS = {{-1.15f, -0.35f}, {1.2f, -0.25f}, {-0.55f, -1.2f}, {0.7f, -1.15f}};

    private Act act = Act.WISE;
    private Phase phase = Phase.WANDER;
    private float phaseTimer = 1.6f;
    private float shotTimer;
    private int shotsLeft;
    private int patternIndex;
    private float spiralAngle;
    private float time;
    private final Vector2 target = new Vector2();
    private final Vector2 step = new Vector2();
    // constellations
    private final Array<Vector2> stars = new Array<>();
    // hops
    private final Vector2 hopFrom = new Vector2();
    private final Vector2 hopTo = new Vector2();
    /** Time left in the air on a hop (0 on the ground). */
    private float hopTimer;
    // comets and their sparkling tails
    private final Array<Bullet> comets = new Array<>();
    private final FloatArray cometTimers = new FloatArray();
    // the rise and the moon face
    private float faceRise;
    private final Vector2 riseFrom = new Vector2();
    private float beamAngle;
    /** Where the beams' sweep is centred: at the player when it starts. */
    private float beamCenter = 270f;
    /** The moon face's place along the top (from the arena's middle) and how high it's bounced. */
    private float faceOffset;
    private float faceLift;
    private float moonHopFrom;
    private float moonHopTo;
    /** Countdown to the next little fan a crater spits between his big moves. */
    private float ambient = 1.5f;
    private float pullHurt;

    public Grandfluff(float x, float y, int tier) {
        super(x, y, 1500f + 250f * tier); // (twice over: a golden life, then the Moon's red one)
        radius = 0.4f * S;
        speed = 0.45f;
        score = 1500 + 300 * tier;
        boss = true;
        target.set(x, y);
    }

    @Override
    public String bossName() {
        return Boss.GRANDFLUFF.title;
    }

    /** The boss bar turns red once he's become the Moon. */
    @Override
    public boolean isEnraged() {
        return act == Act.MOON;
    }

    /** Two lives: the old (then young) king's is the gold on top; under it the Moon's red one waits full. */
    private boolean firstLife() {
        return act != Act.MOON && phase != Phase.RISE;
    }

    @Override
    public float bossBarFraction() {
        return firstLife() ? 1f : super.bossBarFraction();
    }

    @Override
    public float bossBarGold() {
        return firstLife() ? super.bossBarFraction() : 0f;
    }

    @Override
    public float feetY() {
        return act == Act.MOON || phase == Phase.RISE ? Arena.now.top + 1f : pos.y - 0.42f * S;
    }

    /** Out of reach while he snacks, while he rises, and up in the air mid-hop. */
    @Override
    protected boolean isDodging() {
        return phase == Phase.SNACK || phase == Phase.RISE || phase == Phase.HOPS && hopTimer > 0;
    }

    @Override
    protected boolean castsShadow() {
        return act != Act.MOON && phase != Phase.RISE && !(phase == Phase.HOPS && hopTimer > 0);
    }

    /** The camera pulls back once he's a moon, to fit his face in. */
    @Override
    public float viewZoom() {
        return act == Act.MOON ? 1.3f : 1f;
    }

    /** As the moon face, any bullet that flies into the face hits him. */
    @Override
    public boolean bodyContains(float x, float y) {
        return act == Act.MOON && Vector2.dst2(x, y, faceX(), faceY()) < FACE_R * FACE_R;
    }

    private float faceX() {
        return Arena.now.centerX + faceOffset;
    }

    private float faceY() {
        return Arena.now.top - FACE_DROP + (1f - faceRise) * 3.5f + faceLift;
    }

    /** How far the face can slide from the middle and still keep most of itself over the arena. */
    private float faceRoom() {
        return Math.max(0f, (Arena.now.right - Arena.now.left) / 2f - FACE_R * 0.7f);
    }

    /** Where along the top the face would be right over the player. */
    private float offsetOver(Player p) {
        return MathUtils.clamp(p.centerX() - Arena.now.centerX, -faceRoom(), faceRoom());
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        knock.setZero();
        float diff = world.getDifficulty();
        updateComets(delta, world, diff);
        if (act == Act.WISE && phase != Phase.SNACK && hp < maxHp * YOUNG_AT) {
            startSnack(world);
        }
        phaseTimer -= delta;
        shotTimer -= delta;
        if (act != Act.MOON && (phase == Phase.WANDER || phase == Phase.SPIRAL || phase == Phase.COMETS)) {
            drift(delta, world);
        }

        switch (phase) {
            case WANDER:
                if (phaseTimer <= 0) nextPattern(world);
                break;
            case STARS:
                if (phaseTimer <= 0) lightUpStars(world, diff);
                break;
            case SPIRAL:
                if (shotTimer <= 0) {
                    int arms = act == Act.WISE ? 2 : act == Act.YOUNG ? 3 : 4;
                    float sp = act == Act.WISE ? 1.4f : 2.0f;
                    for (int i = 0; i < arms; i++) { // two spirals turning opposite ways
                        for (int dir = -1; dir <= 1; dir += 2) {
                            Bullet b = shoot(world, dir * spiralAngle + 360f * i / arms, sp * diff);
                            b.color.set(SPIRAL_BULLET);
                            b.shape = Bullet.Shape.STAR;
                        }
                    }
                    spiralAngle += act == Act.WISE ? 9f : 13f;
                    shotTimer = act == Act.WISE ? 0.16f : act == Act.YOUNG ? 0.11f : 0.1f;
                    world.sfx().play("enemy_shoot", 0.3f);
                }
                if (phaseTimer <= 0) toWander();
                break;
            case CANE:
                if (shotTimer <= 0 && shotsLeft > 0) {
                    if (distanceToPlayer(world) < 2.6f) { // a swipe of the stick: a wide crescent
                        for (int i = 0; i < 13; i++) {
                            Bullet b = shoot(world, aimAngle - 70f + i * 140f / 12f, 2.3f * diff);
                            b.shape = Bullet.Shape.DIAMOND;
                            b.color.set(CANE_BULLET);
                        }
                        world.sfx().play("swish", 0.8f);
                    } else { // a tap of the stick: a tight little fan at you
                        for (int i = 0; i < 5; i++) {
                            Bullet b = shoot(world, aimAngle - 16f + i * 8f, 2.8f * diff);
                            b.color.set(CANE_BULLET);
                        }
                        world.sfx().play("enemy_shoot", 0.7f);
                    }
                    shotsLeft--;
                    shotTimer = 0.55f;
                }
                if (shotsLeft <= 0 && shotTimer <= 0) toWander();
                break;
            case SNACK:
                if (MathUtils.random() < delta * 10f) { // sparkles as the cheese works its magic
                    world.particles().burst(pos.x + MathUtils.random(-0.5f, 0.5f), pos.y + MathUtils.random(-0.3f, 0.6f),
                            2, 0.2f, 1f, 0.5f, 0.05f, MOON_GEM);
                }
                if (phaseTimer <= 0) {
                    act = Act.YOUNG;
                    speed = 0.9f;
                    world.particles().burst(pos.x, pos.y, 30, 0.6f, 2.6f, 0.7f, 0.08f, ROBE); // off with the robe!
                    world.shake(0.6f);
                    world.sfx().play("boss", 1f);
                    world.toast("GRANDFLUFF IS YOUNG AGAIN!");
                    toWander();
                }
                break;
            case HOPS:
                updateHops(delta, world, diff);
                break;
            case COMETS:
                if (shotTimer <= 0 && shotsLeft > 0) {
                    throwComet(world, diff);
                    shotsLeft--;
                    shotTimer = act == Act.MOON ? 0.4f : 0.7f;
                }
                if (shotsLeft <= 0 && shotTimer <= 0) toWander();
                break;
            case RISE: {
                float k = 1f - Math.max(0f, phaseTimer) / RISE;
                pos.set(riseFrom).lerp(step.set(faceX(), Arena.now.top + 2f), Interpolation.pow2In.apply(Math.min(1f, k * 1.6f)));
                faceRise = Interpolation.pow2Out.apply(MathUtils.clamp((k - 0.35f) / 0.65f, 0f, 1f));
                if (phaseTimer <= 0) {
                    act = Act.MOON;
                    hp = maxHp;
                    faceRise = 1f;
                    pos.set(faceX(), Arena.now.top - FACE_DROP - 0.4f);
                    radius = 1.5f;
                    flying = true;
                    world.shake(1f);
                    world.sfx().play("boss", 1f);
                    toWander();
                }
                break;
            }
            case CRATERS:
                if (shotTimer <= 0 && shotsLeft > 0) {
                    for (int c = 0; c < 1; c++) { // one crater at a time (two made a wall: the owner's playtest)
                        float[] cr = CRATERS[shotsLeft % CRATERS.length];
                        float cx = faceX() + cr[0];
                        float cy = faceY() + cr[1];
                        int n = 16;
                        float off = shotsLeft * 7f + c * 10f;
                        for (int i = 0; i < n; i++) {
                            Bullet b = world.spawnEnemyBullet(cx, cy, off + 360f * i / n, 1.7f * diff);
                            b.color.set(RING_BULLET);
                        }
                        world.particles().burst(cx, cy, 8, 0.3f, 1.2f, 0.4f, 0.06f, CRATER);
                    }
                    world.sfx().play("pop", 0.7f);
                    shotsLeft--;
                    shotTimer = 0.45f;
                }
                if (shotsLeft <= 0 && shotTimer <= 0) toWander();
                break;
            case BEAMS:
                updateBeams(delta, world);
                break;
            case PULL:
                updatePull(delta, world, diff);
                break;
            case MOON_HOPS:
                updateMoonHops(delta, world, diff);
                break;
            default:
                break;
        }
        if (act == Act.MOON) {
            ambient -= delta;
            // a crater spits a little fan at you between his big moves (not over the craters' or hops' rings:
            // together they made walls - the owner's playtest)
            boolean busy = phase == Phase.PULL || phase == Phase.CRATERS || phase == Phase.MOON_HOPS;
            if (ambient <= 0 && !busy) {
                ambient = 1.4f / diff;
                float[] cr = CRATERS[MathUtils.random(CRATERS.length - 1)];
                float cx = faceX() + cr[0];
                float cy = faceY() + cr[1];
                Player p = world.getPlayer();
                float at = MathUtils.atan2Deg360(p.hitboxY() - cy, p.centerX() - cx);
                for (int i = 0; i < 3; i++) {
                    Bullet b = world.spawnEnemyBullet(cx, cy, at - 14f + i * 14f, 2.2f * diff);
                    b.color.set(CRATER);
                }
                world.sfx().play("enemy_shoot", 0.4f);
            }
            if (phase != Phase.MOON_HOPS) { // the face slides along the top after you (slower while it beams)
                float want = offsetOver(world.getPlayer());
                float sp = (phase == Phase.BEAMS ? FACE_FOLLOW * 0.4f : FACE_FOLLOW) * delta;
                faceOffset += MathUtils.clamp(want - faceOffset, -sp, sp);
            }
            pos.set(faceX(), Arena.now.top - FACE_DROP - 0.4f + faceLift);
        }
    }

    /** A slow walk to a random spot in the upper part of the arena; he comes out from behind cover to find you. */
    private void drift(float delta, GameWorld world) {
        boolean hunting = huntForSight(delta, world, target);
        if (!hunting && pos.dst2(target) < 0.05f) {
            target.set(MathUtils.random(Arena.now.left + radius + 0.6f, Arena.now.right - radius - 0.6f),
                    MathUtils.random(Arena.now.centerY - 0.2f, Arena.now.top - radius - 0.25f));
        }
        float moveSpeed = hunting ? speed * 2f : phase == Phase.WANDER ? speed * 1.6f : speed * 0.5f;
        step.set(target).sub(pos);
        if (step.len() > 0.01f) {
            step.nor().scl(moveSpeed * delta);
            pos.add(step);
            walkTime += delta;
        }
    }

    private void toWander() {
        phase = Phase.WANDER;
        phaseTimer = act == Act.WISE ? 1.5f : act == Act.YOUNG ? 0.9f : 0.8f;
    }

    private void nextPattern(GameWorld world) {
        Phase[] order = act == Act.WISE ? new Phase[]{Phase.STARS, Phase.SPIRAL, Phase.CANE, Phase.STARS, Phase.CANE}
                : act == Act.YOUNG ? new Phase[]{Phase.HOPS, Phase.COMETS, Phase.SPIRAL, Phase.HOPS}
                : new Phase[]{Phase.CRATERS, Phase.BEAMS, Phase.STARS, Phase.MOON_HOPS, Phase.SPIRAL, Phase.PULL,
                Phase.COMETS, Phase.BEAMS, Phase.STARS, Phase.MOON_HOPS};
        phase = order[patternIndex++ % order.length];
        shotTimer = 0.4f;
        switch (phase) {
            case STARS:
                placeStars(world);
                phaseTimer = STARS_WARN;
                world.sfx().play("pop", 0.5f);
                break;
            case SPIRAL:
                phaseTimer = act == Act.WISE ? 3.6f : 3f;
                spiralAngle = MathUtils.random(360f);
                break;
            case CANE:
                shotsLeft = 3;
                break;
            case HOPS:
                shotsLeft = 3;
                hopTimer = 0f;
                phaseTimer = 0.25f; // a crouch before the first hop
                break;
            case COMETS:
                shotsLeft = act == Act.MOON ? 6 : 3;
                break;
            case CRATERS:
                shotsLeft = 8;
                break;
            case BEAMS: {
                phaseTimer = BEAM_WARN + BEAM_TIME;
                beamAngle = 0f;
                Player p = world.getPlayer(); // the sweep is centred on where you stand
                beamCenter = MathUtils.atan2Deg360(p.hitboxY() - eyeY(), p.centerX() - faceX());
                world.sfx().play("buzz", 0.7f);
                break;
            }
            case MOON_HOPS:
                shotsLeft = 2;
                hopTimer = 0f;
                phaseTimer = 0.4f;
                break;
            case PULL:
                phaseTimer = PULL_TIME;
                shotTimer = 0f;
                world.sfx().play("hiss", 0.8f);
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------ the wise old king

    /** Five or six stars around the player (never right on top of them), joined up in order. */
    private void placeStars(GameWorld world) {
        stars.clear();
        Player p = world.getPlayer();
        Arena a = Arena.now;
        int n = act == Act.MOON ? MathUtils.random(7, 8) : MathUtils.random(5, 6);
        float start = MathUtils.random(360f);
        for (int i = 0; i < n; i++) {
            float ang = start + i * 360f / n + MathUtils.random(-20f, 20f);
            float dist = MathUtils.random(1.3f, 2.6f);
            float x = MathUtils.clamp(p.centerX() + MathUtils.cosDeg(ang) * dist, a.left + 0.4f, a.right - 0.4f);
            float y = MathUtils.clamp(p.hitboxY() + MathUtils.sinDeg(ang) * dist, a.bottom + 0.4f, a.top - 0.4f);
            stars.add(new Vector2(x, y));
        }
    }

    /** The constellation lights up: every line between two stars becomes a row of bullets drifting apart, every star a little ring. */
    private void lightUpStars(GameWorld world, float diff) {
        Player p = world.getPlayer();
        for (int i = 0; i < stars.size; i++) {
            Vector2 s0 = stars.get(i);
            Vector2 s1 = stars.get((i + 1) % stars.size);
            float len = s0.dst(s1);
            float ang = MathUtils.atan2Deg360(s1.y - s0.y, s1.x - s0.x);
            float normal = ang + (i % 2 == 0 ? 90f : -90f);
            for (float t = 0.3f; t < len - 0.15f; t += 0.34f) {
                float x = s0.x + MathUtils.cosDeg(ang) * t;
                float y = s0.y + MathUtils.sinDeg(ang) * t;
                if (Vector2.dst2(x, y, p.centerX(), p.hitboxY()) < 0.5f * 0.5f) continue; // never right on you
                Bullet b = world.spawnEnemyBullet(x, y, normal, 0.85f * diff);
                b.color.set(LINE_BULLET);
                b.radius *= 0.85f;
            }
            for (int k = 0; k < 6; k++) {
                Bullet b = world.spawnEnemyBullet(s0.x, s0.y, k * 60f + 30f, 1.5f * diff);
                b.shape = Bullet.Shape.STAR;
                b.color.set(STAR_BULLET);
            }
        }
        stars.clear();
        world.sfx().play("zap", 0.8f);
        toWander();
    }

    /** Two thirds down: a bite of moon cheese - out of reach a moment, bullets gone - and he's young again. */
    private void startSnack(GameWorld world) {
        phase = Phase.SNACK;
        phaseTimer = SNACK;
        stars.clear();
        world.clearEnemyBullets(0, 0, -1f);
        world.shake(0.4f);
        world.sfx().play("chomp", 1f);
        world.toast("GRANDFLUFF HAS A BITE OF MOON CHEESE...");
    }

    // ------------------------------------------------------------ young again

    /** Three quick hops, each landing near you in a ring with a gap (where you can slip out). */
    private void updateHops(float delta, GameWorld world, float diff) {
        if (hopTimer > 0) { // in the air
            hopTimer -= delta;
            float k = 1f - Math.max(0f, hopTimer) / HOP_AIR;
            pos.set(hopFrom).lerp(hopTo, k);
            if (hopTimer <= 0) {
                hopTimer = 0f;
                pos.set(hopTo);
                Player p = world.getPlayer();
                if (Vector2.dst(pos.x, feetY() + 0.2f, p.centerX(), p.hitboxY()) < HOP_RADIUS) {
                    world.hurtPlayer();
                }
                int n = 24;
                float gap = aimAngle + MathUtils.random(-60f, 60f);
                for (int i = 0; i < n; i++) {
                    float a = 360f * i / n;
                    if (Math.abs(((a - gap) % 360f + 540f) % 360f - 180f) < 24f) continue; // the gap
                    Bullet b = world.spawnEnemyBullet(pos.x, feetY() + 0.15f, a, 1.9f * diff);
                    b.color.set(RING_BULLET);
                }
                world.shake(0.5f);
                world.sfx().play("boom", 0.6f);
                world.particles().burst(pos.x, feetY(), 18, 0.5f, 2f, 0.4f, 0.06f, GOLDEN);
                phaseTimer = 0.35f; // a moment on the ground
            }
            return;
        }
        if (phaseTimer > 0) {
            return;
        }
        if (shotsLeft <= 0) {
            toWander();
            return;
        }
        shotsLeft--;
        Player p = world.getPlayer();
        hopFrom.set(pos);
        hopTo.set(p.centerX() + MathUtils.random(-0.6f, 0.6f), p.hitboxY() + 0.42f * S + MathUtils.random(-0.4f, 0.4f));
        Arena a = Arena.now;
        hopTo.x = MathUtils.clamp(hopTo.x, a.left + radius + 0.2f, a.right - radius - 0.2f);
        hopTo.y = MathUtils.clamp(hopTo.y, a.bottom + 0.42f * S + 0.2f, a.top - radius - 0.25f);
        hopTimer = HOP_AIR;
        world.sfx().play("flap", 0.7f);
    }

    /** A comet thrown at you, leaving a trail of lingering sparkles. */
    private void throwComet(GameWorld world, float diff) {
        Bullet b = shoot(world, aimAngle + MathUtils.random(-10f, 10f), 2.8f * diff);
        b.shape = Bullet.Shape.STAR;
        b.color.set(COMET);
        b.radius = 0.14f;
        comets.add(b);
        cometTimers.add(COMET_TIME);
        world.sfx().play("swish", 0.7f);
    }

    private void updateComets(float delta, GameWorld world, float diff) {
        for (int i = comets.size - 1; i >= 0; i--) {
            float left = cometTimers.get(i) - delta;
            cometTimers.set(i, left);
            Bullet c = comets.get(i);
            if (c.alive && left > 0) {
                if (((int) (left * 10f)) != ((int) ((left + delta) * 10f))) { // a sparkle every tenth of a second
                    Bullet s = world.spawnEnemyBullet(c.pos.x, c.pos.y, MathUtils.random(360f), 0.35f * diff);
                    s.color.set(STAR_BULLET);
                    s.radius *= 0.7f;
                }
                continue;
            }
            comets.removeIndex(i);
            cometTimers.removeIndex(i);
        }
    }

    /** One third left: he floats up over the castle and rises again behind it - as big as the Moon. */
    private void startRise(GameWorld world) {
        hp = maxHp; // the gold is gone: the Moon's red life is all there under it
        phase = Phase.RISE;
        phaseTimer = RISE;
        riseFrom.set(pos);
        comets.clear();
        cometTimers.clear();
        world.clearEnemyBullets(0, 0, -1f);
        world.shake(0.8f);
        world.sfx().play("boss", 1f);
        world.toast("GRANDFLUFF GROWS AS BIG AS THE MOON!");
    }

    /** The young king's last hit doesn't finish him: he rises to become the Moon, his health full again. */
    @Override
    public void hit(float damage, Vector2 bulletVel, float knockback, GameWorld world) {
        if (act != Act.MOON && alive && !isSpawning() && hp - damage <= 0) {
            hp = 0f;
            flash = 0.08f;
            if (phase != Phase.RISE) {
                if (act == Act.WISE) { // (one huge hit straight through: he skips being young)
                    act = Act.YOUNG;
                }
                startRise(world);
            }
            return;
        }
        super.hit(damage, bulletVel, knockback, world);
    }

    // ------------------------------------------------------------ the moon

    private float eyeX(int side) {
        return faceX() + side * 0.7f;
    }

    private float eyeY() {
        return faceY() + 0.45f;
    }

    private float mouthY() {
        return faceY() - 0.75f;
    }

    /** A beam from each eye: a thin warning line first, then they sweep across the arena, crossing over. */
    private void updateBeams(float delta, GameWorld world) {
        float elapsed = BEAM_WARN + BEAM_TIME - Math.max(0f, phaseTimer);
        float k = MathUtils.clamp((elapsed - BEAM_WARN) / BEAM_TIME, 0f, 1f);
        beamAngle = k;
        if (elapsed > BEAM_WARN) {
            Player p = world.getPlayer();
            for (int side = -1; side <= 1; side += 2) {
                float ang = beamFor(side, k);
                float len = world.rayLength(eyeX(side), eyeY(), ang, 14f);
                float ex = eyeX(side) + MathUtils.cosDeg(ang) * len;
                float ey = eyeY() + MathUtils.sinDeg(ang) * len;
                if (Intersector.distanceSegmentPoint(eyeX(side), eyeY(), ex, ey, p.centerX(), p.hitboxY()) < BEAM_WIDTH + 0.08f) {
                    world.hurtPlayer();
                }
                if (shotTimer <= 0) { // sparks shed along the beam, drifting off either side
                    float t = MathUtils.random(1.2f, Math.max(1.3f, len));
                    Bullet sp = world.spawnEnemyBullet(eyeX(side) + MathUtils.cosDeg(ang) * t, eyeY() + MathUtils.sinDeg(ang) * t,
                            ang + 90f * MathUtils.randomSign(), 1.1f * world.getDifficulty());
                    sp.color.set(BEAM);
                    sp.radius *= 0.8f;
                }
            }
            if (shotTimer <= 0) {
                shotTimer = 0.09f;
            }
        }
        if (phaseTimer <= 0) toWander();
    }

    /** Where an eye's beam points, {@code k} of the way through the sweep: the left eye sweeps right, the right eye left. */
    private float beamFor(int side, float k) {
        return side < 0 ? beamCenter - 45f + 75f * k : beamCenter + 45f - 75f * k;
    }

    /** Moon-sized hops: the whole face bounces up and comes down over you, landing in a ring with a gap. */
    private void updateMoonHops(float delta, GameWorld world, float diff) {
        if (hopTimer > 0) {
            hopTimer -= delta;
            float k = 1f - Math.max(0f, hopTimer) / MOON_HOP_AIR;
            faceOffset = MathUtils.lerp(moonHopFrom, moonHopTo, Interpolation.sineOut.apply(k));
            faceLift = MathUtils.sin(k * MathUtils.PI) * MOON_HOP_LIFT;
            if (hopTimer <= 0) {
                hopTimer = 0f;
                faceLift = 0f;
                faceOffset = moonHopTo;
                Player p = world.getPlayer();
                if (Vector2.dst2(p.centerX(), p.hitboxY(), faceX(), faceY()) < FACE_R * FACE_R) {
                    world.hurtPlayer(); // (squashed under the moon)
                }
                float cx = faceX();
                float cy = faceY() - FACE_R * 0.8f;
                int n = 26;
                float gap = MathUtils.atan2Deg360(p.hitboxY() - cy, p.centerX() - cx) + MathUtils.random(-40f, 40f);
                for (int i = 0; i < n; i++) {
                    float a = 360f * i / n;
                    if (Math.abs(((a - gap) % 360f + 540f) % 360f - 180f) < 28f) continue; // the gap
                    Bullet b = world.spawnEnemyBullet(cx, cy, a, 1.5f * diff);
                    b.color.set(RING_BULLET);
                }
                world.shake(0.9f);
                world.sfx().play("boom", 0.9f);
                world.particles().burst(cx, cy, 24, 0.6f, 2.4f, 0.5f, 0.08f, FACE);
                phaseTimer = 0.5f;
            }
            return;
        }
        if (phaseTimer > 0) {
            return;
        }
        if (shotsLeft <= 0) {
            toWander();
            return;
        }
        shotsLeft--;
        moonHopFrom = faceOffset;
        moonHopTo = offsetOver(world.getPlayer());
        hopTimer = MOON_HOP_AIR;
        world.sfx().play("flap", 1f);
    }

    /** He breathes in: you get pulled towards his mouth, with a stream of stardust - too close, and it hurts. */
    private void updatePull(float delta, GameWorld world, float diff) {
        Player p = world.getPlayer();
        float mx = faceX();
        float my = mouthY();
        step.set(mx - p.centerX(), my - p.hitboxY());
        float dist = step.len();
        if (dist > 0.01f && !p.isDead()) {
            step.scl(PULL_SPEED * delta / dist);
            p.nudge(step.x, step.y, world);
        }
        pullHurt -= delta;
        if (dist < 0.9f && pullHurt <= 0) {
            world.hurtPlayer();
            pullHurt = 1f;
        }
        if (shotTimer <= 0) { // stardust flying in from the sides of the arena
            shotTimer = 0.08f / diff;
            Arena a = Arena.now;
            float sx = MathUtils.randomBoolean() ? a.left + 0.2f : a.right - 0.2f;
            float sy = MathUtils.random(a.bottom + 0.3f, my - 0.5f);
            Bullet b = world.spawnEnemyBullet(sx, sy, MathUtils.atan2Deg360(my - sy, mx - sx), 1.8f * diff);
            b.color.set(DUST);
            b.radius *= 0.8f;
        }
        if (phaseTimer <= 0) { // ...and breathes out: a big ring from his mouth
            int n = 36;
            for (int ring = 0; ring < 2; ring++) {
                for (int i = 0; i < n; i++) {
                    Bullet b = world.spawnEnemyBullet(mx, my, 360f * (i + ring * 0.5f) / n, (ring == 0 ? 2f : 1.4f) * diff);
                    b.color.set(DUST);
                }
            }
            world.shake(0.5f);
            world.sfx().play("swish", 1f);
            toWander();
        }
    }

    // ------------------------------------------------------------ drawing

    @Override
    public void drawFloor(SpriteBatch b, Draw d) {
        if (phase == Phase.STARS) { // the constellation, drawing itself star by star
            float k = 1f - Math.max(0f, phaseTimer) / STARS_WARN;
            int shown = Math.min(stars.size, 1 + (int) (k * stars.size * 1.2f));
            for (int i = 0; i < shown; i++) {
                Vector2 s0 = stars.get(i);
                if (i < shown - 1 || shown == stars.size && k > 0.85f) {
                    Vector2 s1 = stars.get((i + 1) % stars.size);
                    d.line(b, s0.x, s0.y, s0.dst(s1), 0.03f, MathUtils.atan2Deg360(s1.y - s0.y, s1.x - s0.x), LINE_BULLET, 0.35f + 0.4f * k);
                }
                float tw = 0.7f + 0.3f * MathUtils.sin(time * 12f + i);
                d.shape(b, d.star, s0.x, s0.y, 0.16f * tw, time * 60f, STAR_BULLET, 0.9f);
            }
        }
        if (phase == Phase.MOON_HOPS && hopTimer > 0) { // where the moon will come down
            float k = 1f - hopTimer / MOON_HOP_AIR;
            float lx = Arena.now.centerX + moonHopTo;
            float ly = Arena.now.top - FACE_DROP - FACE_R * 0.8f;
            d.ellipse(b, lx, ly, FACE_R * 0.9f, 0.35f, Color.BLACK, 0.15f + 0.25f * k);
            d.ring(b, lx, ly, FACE_R * 0.6f * (0.6f + 0.4f * k), TELEGRAPH, 0.3f + 0.5f * k);
        }
        if (phase == Phase.HOPS && hopTimer > 0) { // where he'll come down
            float k = 1f - hopTimer / HOP_AIR;
            d.ellipse(b, hopTo.x, hopTo.y - 0.42f * S, radius * (0.5f + 0.6f * k), radius * 0.35f * (0.5f + 0.6f * k), Color.BLACK, 0.3f);
            d.ring(b, hopTo.x, hopTo.y - 0.42f * S, HOP_RADIUS, TELEGRAPH, 0.4f + 0.5f * k);
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        if (act == Act.MOON || phase == Phase.RISE) {
            if (faceRise > 0) drawFace(b, d);
            if (act == Act.MOON) return;
        }
        float cx = pos.x;
        float cy = pos.y + MathUtils.sin(time * 3f) * 0.015f * S;
        if (phase == Phase.HOPS && hopTimer > 0) {
            float k = 1f - hopTimer / HOP_AIR;
            cy += MathUtils.sin(k * MathUtils.PI) * 1.4f;
        }
        float scale = 1f;
        if (phase == Phase.RISE) { // floating away up over the castle, getting smaller
            scale = 1f - 0.6f * Math.min(1f, (1f - phaseTimer / RISE) * 1.6f);
        }
        drawKing(b, d, cx, cy, S * scale);
    }

    /** The king himself: silver, bearded and bespectacled, or young and golden after his snack. */
    private void drawKing(SpriteBatch b, Draw d, float cx, float cy, float s) {
        boolean wise = act == Act.WISE;
        Color body = wise ? SILVER : GOLDEN;
        Color shade = wise ? SILVER_SHADE : GOLDEN_SHADE;
        if (wise) { // his starry robe, under the fluff
            d.shape(b, d.triangle, cx, cy - 0.12f * s, 0.42f * s, 90f, ROBE, 1f);
            for (int i = 0; i < 4; i++) {
                d.circle(b, cx + (i - 1.5f) * 0.09f * s, cy - 0.3f * s + (i % 2) * 0.06f * s, 0.012f * s, ROBE_STAR);
            }
            // the walking stick
            d.line(b, cx + 0.33f * s, cy - 0.4f * s, 0.62f * s, 0.035f * s, 88f, STICK);
            d.circle(b, cx + 0.34f * s, cy + 0.22f * s, 0.04f * s, MOON_GEM);
        }
        drawFeet(b, d, cx, cy - 0.42f * s + 0.03f, 0.18f * s);
        for (float[] p : PUFFS) {
            d.circle(b, cx + p[0] * s, cy + p[1] * s, 0.17f * s, c(shade));
        }
        for (float[] p : PUFFS) {
            d.circle(b, cx + p[0] * 0.92f * s, cy + (p[1] * 0.92f + 0.015f) * s, 0.15f * s, c(body));
        }
        d.circle(b, cx, cy + 0.03f * s, 0.27f * s, c(body));
        // crown: old silver with a crescent moon, or polished gold when he's young
        float top = cy + 0.38f * s;
        Color crown = wise ? CROWN : CROWN_GOLD;
        d.rect(b, cx - 0.2f * s, top, 0.4f * s, 0.09f * s, c(crown));
        for (int i = -1; i <= 1; i++) {
            d.rotRect(b, cx + i * 0.14f * s, top + 0.1f * s, 0.08f * s, 0.08f * s, 45, c(crown), 1f);
        }
        d.circle(b, cx, top + 0.05f * s, 0.035f * s, MOON_GEM);
        d.circle(b, cx + 0.015f * s, top + 0.055f * s, 0.028f * s, c(crown)); // (a crescent)
        // eyes: under bushy brows and spectacles when old; bright and cheeky when young
        float lookX = MathUtils.cosDeg(aimAngle) * 0.012f * s;
        float lookY = MathUtils.sinDeg(aimAngle) * 0.012f * s;
        for (int i = -1; i <= 1; i += 2) {
            float ex = cx + i * 0.1f * s;
            float ey = cy + 0.09f * s;
            d.circle(b, ex, ey, 0.045f * s, c(EYE_WHITE));
            d.circle(b, ex + lookX, ey + lookY, 0.025f * s, PUPIL);
            if (wise) {
                d.ring(b, ex, ey, 0.06f * s, PUPIL, 0.9f);
                d.rotRect(b, ex, ey + 0.075f * s, 0.12f * s, 0.035f * s, i * 15f, BEARD, 1f);
            } else {
                d.rotRect(b, ex, ey + 0.065f * s, 0.1f * s, 0.022f * s, -i * 10f, PUPIL, 1f);
            }
        }
        if (wise) {
            d.rect(b, cx - 0.03f * s, cy + 0.085f * s, 0.06f * s, 0.012f * s, PUPIL); // the specs' bridge
            // a long white beard, swaying
            float sway = MathUtils.sin(time * 1.5f) * 4f;
            d.shape(b, d.triangle, cx, cy - 0.12f * s, 0.24f * s, 270f + sway, BEARD, 1f);
            d.circle(b, cx - 0.08f * s, cy - 0.02f * s, 0.07f * s, BEARD);
            d.circle(b, cx + 0.08f * s, cy - 0.02f * s, 0.07f * s, BEARD);
            d.rect(b, cx - 0.06f * s, cy - 0.005f * s, 0.12f * s, 0.02f * s, PUPIL); // a grumpy mouth
        } else {
            d.circle(b, cx, cy - 0.04f * s, 0.035f * s, PUPIL); // a cheeky grin
            d.rect(b, cx - 0.035f * s, cy - 0.03f * s, 0.07f * s, 0.035f * s, c(body));
        }
        if (phase == Phase.SNACK) { // the wedge of moon cheese, getting smaller bite by bite
            float left = Math.max(0f, phaseTimer) / SNACK;
            d.shape(b, d.triangle, cx + 0.2f * s, cy - 0.05f * s, 0.12f * s * left, 200f, CHEESE, 1f);
        }
        boolean telling = (phase == Phase.CANE || phase == Phase.COMETS) && shotTimer > 0 && shotTimer < 0.4f;
        if (telling) {
            d.ring(b, cx, cy, radius * (1.6f - shotTimer), TELEGRAPH, 0.6f);
        }
    }

    /** The giant moon face looming over the arena: craters, bushy brows, his beard - and beams and breath. */
    private void drawFace(SpriteBatch b, Draw d) {
        float fx = faceX();
        float fy = faceY();
        float a = Math.min(1f, faceRise * 1.5f);
        d.circle(b, fx, fy, FACE_R + 0.06f, Draw.OUTLINE_COLOR, a);
        d.circle(b, fx, fy, FACE_R, c(FACE_SHADE), a);
        d.circle(b, fx - 0.15f, fy + 0.15f, FACE_R * 0.92f, c(FACE), a);
        for (float[] cr : CRATERS) {
            d.circle(b, fx + cr[0], fy + cr[1], 0.24f, c(CRATER), a);
            d.circle(b, fx + cr[0] - 0.04f, fy + cr[1] + 0.04f, 0.18f, c(FACE_SHADE), a);
        }
        // the old crown, now as big as a castle, on top
        float top = fy + FACE_R - 0.25f;
        d.rect(b, fx - 0.9f, top, 1.8f, 0.35f, CROWN, a);
        for (int i = -1; i <= 1; i++) {
            d.rotRect(b, fx + i * 0.6f, top + 0.4f, 0.32f, 0.32f, 45, CROWN, a);
        }
        // eyes under bushy brows; red once the beams come
        boolean beams = phase == Phase.BEAMS;
        for (int side = -1; side <= 1; side += 2) {
            float ex = eyeX(side);
            float ey = eyeY();
            d.circle(b, ex, ey, 0.28f, Draw.OUTLINE_COLOR, a);
            d.circle(b, ex, ey, 0.24f, EYE_WHITE, a);
            d.circle(b, ex + MathUtils.cosDeg(aimAngle) * 0.08f, ey + MathUtils.sinDeg(aimAngle) * 0.08f, 0.12f,
                    beams ? BEAM : PUPIL, a);
            d.rotRect(b, ex, ey + 0.34f, 0.62f, 0.16f, side * 14f, BEARD, a);
        }
        // the mouth: a grumpy line, or wide open breathing in
        float my = mouthY();
        if (phase == Phase.PULL) {
            float open = 0.3f + 0.05f * MathUtils.sin(time * 9f);
            d.ellipse(b, fx, my, 0.55f, open, MOUTH, a);
        } else {
            d.rect(b, fx - 0.4f, my - 0.04f, 0.8f, 0.08f, MOUTH, a);
        }
        // his beard spilling down over the castle's wall into the arena
        for (int i = -3; i <= 3; i++) {
            float sway = MathUtils.sin(time * 1.4f + i) * 0.05f;
            d.circle(b, fx + i * 0.32f + sway, my - 0.45f - (3 - Math.abs(i)) * 0.12f, 0.26f, BEARD, a * 0.95f);
        }
        if (beams) { // the warning lines, then the beams themselves
            float elapsed = BEAM_WARN + BEAM_TIME - Math.max(0f, phaseTimer);
            boolean firing = elapsed > BEAM_WARN;
            for (int side = -1; side <= 1; side += 2) {
                float ang = beamFor(side, beamAngle);
                float len = 14f; // (cut short by cover in the world - drawn long; the hit test uses rayLength)
                if (firing) {
                    d.line(b, eyeX(side), eyeY(), len, BEAM_WIDTH * 2f, ang, BEAM, 0.85f);
                    d.line(b, eyeX(side), eyeY(), len, BEAM_WIDTH * 0.8f, ang, Color.WHITE, 0.9f);
                } else {
                    d.line(b, eyeX(side), eyeY(), len, 0.03f, ang, TELEGRAPH, 0.6f);
                }
            }
        }
    }
}
