package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
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
 * City boss: the Night Hawk, a helicopter. It flies over everything (its shadow is far below) and
 * <ul>
 *   <li>hovers, firing door-gun bursts;</li>
 *   <li>makes strafing runs across the arena, the gun's stream sweeping as it flies past;</li>
 *   <li>fires fans of rockets that speed up and burst into rings when they hit something;</li>
 *   <li>sweeps a searchlight after the player, locks it (blinking red) and blows up that spot.</li>
 * </ul>
 * Enraged: two searchlight strikes in a row, more rockets, faster everything.
 */
public class NightHawk extends Enemy {

    private enum Phase { HOVER, STRAFE, ROCKETS, SEARCH, LOCK }

    private static final Color BODY = Color.valueOf("2e3a3a");
    private static final Color BODY_LIGHT = Color.valueOf("4a5a58");
    private static final Color STRIPE = Color.valueOf("e8c030");
    private static final Color GLASS = Color.valueOf("8ec5e8");
    private static final Color ROTOR = Color.valueOf("1a1d22");
    private static final Color SKID = Color.valueOf("5a5a66");
    private static final Color BEAM = Color.valueOf("fff3b0");
    private static final Color GUN_BULLET = Color.valueOf("ffd166");
    private static final Color ROCKET = Color.valueOf("c9ccd6");
    private static final Color BLAST = Color.valueOf("ff8a3a");

    /** It flies this high above its shadow. */
    private static final float ALTITUDE = 0.9f;
    private static final float SEARCH_TIME = 2.0f;
    private static final float LOCK_TIME = 0.6f;
    private static final float LIGHT_RADIUS = 0.6f;

    private Phase phase = Phase.HOVER;
    private float phaseTimer = 2.0f;
    private float shotTimer = 1f;
    private int patternIndex;
    private int strikesLeft;
    private boolean enraged;
    private float time;
    private float rotor;
    private final Vector2 target = new Vector2();
    private final Vector2 step = new Vector2();
    private final Vector2 strafeEnd = new Vector2();
    private final Vector2 light = new Vector2();
    private final Vector2 tmp = new Vector2();
    /** Rockets in flight: they burst into a ring when they hit something or run out of fuel. */
    private final Array<Bullet> rockets = new Array<>();

    public NightHawk(float x, float y, int tier) {
        super(x, y, 520f + 160f * tier);
        radius = 0.55f;
        speed = 1.2f;
        score = 500 + 250 * tier;
        boss = true;
        flying = true;
        target.set(x, y);
    }

    @Override
    public String bossName() {
        return Boss.NIGHT_HAWK.title;
    }

    @Override
    public boolean isEnraged() {
        return enraged;
    }

    /** The shadow on the ground, far below the helicopter. */
    @Override
    public float feetY() {
        return pos.y - ALTITUDE;
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        time += delta;
        rotor += delta * (enraged ? 1800f : 1400f);
        if (!enraged && hp < maxHp * 0.5f) {
            enraged = true;
            world.shake(0.6f);
            world.sfx().play("boss", 1f);
        }
        float rage = enraged ? 1.3f : 1f;
        float diff = world.getDifficulty();
        phaseTimer -= delta;
        shotTimer -= delta;
        updateRockets(world, diff);

        switch (phase) {
            case HOVER:
                hover(delta, world, rage);
                if (shotTimer <= 0 && canSeePlayer) { // door gun burst
                    shotTimer = (enraged ? 0.7f : 1.1f) / diff;
                    for (int i = 0; i < 4; i++) {
                        shoot(world, aimAngle + MathUtils.random(-6f, 6f), (2.6f + i * 0.25f) * diff).color.set(GUN_BULLET);
                    }
                    world.sfx().play("smg", 0.6f);
                }
                if (phaseTimer <= 0) {
                    nextPattern(world);
                }
                break;
            case STRAFE:
                strafe(delta, world, rage, diff);
                break;
            case ROCKETS:
                if (shotTimer <= 0) {
                    fireRockets(world, diff);
                    toHover();
                }
                break;
            case SEARCH:
                drift(delta, rage * 0.4f);
                // the light chases the player a bit slower than they can run
                Player p = world.getPlayer();
                tmp.set(p.feetX(), p.feetY() + 0.1f).sub(light);
                light.add(tmp.limit((enraged ? 2.4f : 2.0f) * delta));
                if (phaseTimer <= 0) {
                    phase = Phase.LOCK;
                    phaseTimer = LOCK_TIME;
                    world.sfx().play("door", 0.8f);
                }
                break;
            case LOCK:
                drift(delta, rage * 0.4f);
                if (phaseTimer <= 0) {
                    strike(world, diff);
                    if (--strikesLeft > 0) {
                        phase = Phase.SEARCH;
                        phaseTimer = SEARCH_TIME * 0.7f;
                    } else {
                        toHover();
                    }
                }
                break;
        }
    }

    private void hover(float delta, GameWorld world, float rage) {
        Player p = world.getPlayer();
        boolean hunting = huntForSight(delta, world, target);
        if (!hunting && (pos.dst2(target) < 0.05f || target.dst(p.centerX(), p.hitboxY()) < 2.3f)) {
            for (int i = 0; i < 10; i++) {
                target.set(MathUtils.random(Arena.now.left + 1f, Arena.now.right - 1f),
                        MathUtils.random(Arena.now.centerY - 0.5f, Arena.now.top - 0.6f));
                if (target.dst(p.centerX(), p.hitboxY()) > 2.6f) break;
            }
        }
        drift(delta, (hunting ? 1.8f : 1f) * rage);
    }

    private void drift(float delta, float k) {
        step.set(target).sub(pos);
        if (step.len() > 0.02f) {
            step.nor().scl(speed * k * delta);
            pos.add(step);
        }
    }

    /** Roars across the arena past the player, the door gun's stream sweeping as it goes. */
    private void strafe(float delta, GameWorld world, float rage, float diff) {
        step.set(strafeEnd).sub(pos);
        float move = 3.6f * rage * delta;
        if (step.len() <= move || phaseTimer <= 0) {
            toHover();
            return;
        }
        pos.mulAdd(step.nor(), move);
        if (shotTimer <= 0) {
            shotTimer = enraged ? 0.05f : 0.07f;
            shoot(world, aimAngle + MathUtils.random(-4f, 4f), 3.2f * diff).color.set(GUN_BULLET);
            world.sfx().play("smg", 0.3f);
        }
    }

    private void fireRockets(GameWorld world, float diff) {
        int n = enraged ? 6 : 4;
        float fan = 70f;
        for (int i = 0; i < n; i++) {
            Bullet b = shoot(world, aimAngle - fan / 2f + fan * i / (n - 1), 1.0f);
            b.accel = 3.2f * diff;
            b.maxSpeed = 4.5f * diff;
            b.radius = 0.1f;
            b.life = 2.6f;
            b.color.set(ROCKET);
            rockets.add(b);
        }
        world.sfx().play("dodge", 1f);
        world.shake(0.2f);
    }

    private void updateRockets(GameWorld world, float diff) {
        for (int i = rockets.size - 1; i >= 0; i--) {
            Bullet r = rockets.get(i);
            if (MathUtils.randomBoolean(0.5f) && r.alive) { // smoke trail
                world.particles().spawn(r.pos.x, r.pos.y, 0, 0, 0.3f, 0.04f, Color.GRAY, 1f);
            }
            boolean outOfFuel = r.alive && r.life < 0.1f;
            if (r.alive && !outOfFuel) continue;
            rockets.removeIndex(i);
            r.alive = false;
            // blanks and a freshly hit player get no burst; walls, cover or running out of fuel do
            if (world.getBlankRing() >= 0 || world.getPlayer().isInvulnerable()) continue;
            tmp.set(r.vel).nor().scl(-0.2f).add(r.pos);
            Arena.now.clamp(tmp, 0.08f);
            int n = enraged ? 12 : 10;
            float off = MathUtils.random(360f);
            for (int k = 0; k < n; k++) {
                world.spawnEnemyBullet(tmp.x, tmp.y, off + 360f * k / n, 1.9f * diff).color.set(BLAST);
            }
            world.particles().burst(tmp.x, tmp.y, 12, 0.4f, 1.6f, 0.4f, 0.05f, BLAST);
            world.sfx().play("break", 0.7f);
        }
    }

    /** The searchlight's spot blows up: a ring outward and a few slower bullets in the middle. */
    private void strike(GameWorld world, float diff) {
        int n = enraged ? 18 : 14;
        float off = MathUtils.random(360f);
        for (int k = 0; k < n; k++) {
            world.spawnEnemyBullet(light.x, light.y, off + 360f * k / n, 2.1f * diff).color.set(BLAST);
        }
        for (int k = 0; k < 6; k++) {
            world.spawnEnemyBullet(light.x, light.y, off + 30f + 60f * k, 1.0f * diff).color.set(BEAM);
        }
        world.particles().burst(light.x, light.y, 20, 0.5f, 2.2f, 0.5f, 0.06f, BLAST);
        world.shake(0.4f);
        world.sfx().play("break", 1f);
    }

    private void toHover() {
        phase = Phase.HOVER;
        phaseTimer = enraged ? 1.3f : 2.0f;
    }

    private void nextPattern(GameWorld world) {
        Phase[] order = {Phase.STRAFE, Phase.ROCKETS, Phase.SEARCH};
        phase = order[patternIndex++ % order.length];
        Player p = world.getPlayer();
        switch (phase) {
            case STRAFE: // over the player and out the far side
                step.set(p.centerX(), p.hitboxY()).sub(pos).nor().scl(3f);
                strafeEnd.set(p.centerX() + step.x, p.hitboxY() + step.y + ALTITUDE * 0.5f);
                Arena.now.clamp(strafeEnd, radius + 0.2f);
                phaseTimer = 3.5f;
                shotTimer = 0.3f;
                world.sfx().play("chainsaw", 0.5f); // engine roar
                break;
            case ROCKETS:
                shotTimer = 0.5f;
                break;
            case SEARCH:
                light.set(pos.x, feetY());
                phaseTimer = SEARCH_TIME;
                strikesLeft = enraged ? 2 : 1;
                world.toast("SEARCHLIGHT - KEEP MOVING!");
                break;
            default:
                break;
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        // the searchlight on the ground (additive, so it lights up the night)
        if (phase == Phase.SEARCH || phase == Phase.LOCK) {
            boolean blink = phase == Phase.LOCK && ((int) (time * 14f)) % 2 == 0;
            b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            b.setColor(blink ? 1f : BEAM.r, blink ? 0.3f : BEAM.g, blink ? 0.25f : BEAM.b, 0.55f);
            b.draw(d.glow, light.x - LIGHT_RADIUS * 1.6f, light.y - LIGHT_RADIUS * 1.6f, LIGHT_RADIUS * 3.2f, LIGHT_RADIUS * 3.2f);
            b.setColor(Color.WHITE);
            b.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            d.ring(b, light.x, light.y, LIGHT_RADIUS, blink ? TELEGRAPH : BEAM, 0.7f);
        }
        float face = facingRight() ? 1 : -1;
        float bob = MathUtils.sin(time * 3f) * 0.04f;
        float cx = pos.x;
        float cy = pos.y + bob;
        // tail boom and tail rotor, behind the body
        d.rect(b, face > 0 ? cx - 0.85f : cx + 0.25f, cy + 0.05f, 0.6f, 0.1f, c(BODY));
        float tailX = cx - face * 0.85f;
        float tr = MathUtils.sinDeg(rotor * 1.3f) * 0.14f;
        d.rect(b, tailX - 0.02f, cy + 0.1f - Math.abs(tr), 0.04f, Math.abs(tr) * 2f + 0.02f, ROTOR);
        // skids
        d.rect(b, cx - 0.35f, cy - 0.32f, 0.7f, 0.04f, SKID);
        d.rect(b, cx - 0.22f, cy - 0.3f, 0.03f, 0.1f, SKID);
        d.rect(b, cx + 0.19f, cy - 0.3f, 0.03f, 0.1f, SKID);
        // fuselage with a police stripe and a cockpit bubble
        d.circle(b, cx, cy, 0.34f, c(BODY));
        d.rect(b, cx - 0.34f, cy - 0.05f, 0.68f, 0.07f, c(STRIPE));
        d.circle(b, cx + face * 0.16f, cy + 0.06f, 0.17f, c(GLASS));
        d.circle(b, cx + face * 0.2f, cy + 0.11f, 0.05f, Color.WHITE, 0.7f);
        d.circle(b, cx - face * 0.1f, cy + 0.2f, 0.06f, c(BODY_LIGHT));
        // searchlight lamp under the nose
        d.circle(b, cx + face * 0.3f, cy - 0.18f, 0.05f, phase == Phase.SEARCH || phase == Phase.LOCK ? BEAM : SKID);
        // main rotor: a long blade seen edge-on, spinning
        float len = Math.abs(MathUtils.cosDeg(rotor)) * 0.95f + 0.1f;
        d.rect(b, cx - len, cy + 0.36f, len * 2f, 0.035f, ROTOR);
        d.rect(b, cx - 0.03f, cy + 0.3f, 0.06f, 0.07f, ROTOR);
        // enraged: smoke from the engine
        if (enraged && MathUtils.randomBoolean(0.3f)) {
            d.circle(b, cx - face * 0.2f, cy + 0.3f + MathUtils.random(0.1f), 0.06f, Color.DARK_GRAY, 0.5f);
        }
    }
}
