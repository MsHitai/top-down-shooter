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
 * Sea enemy: a sea turtle that rams. It paddles closer, pulls into its shell (the warning: it spins and
 * glows red), then the shell shoots across the room in a straight line until it slams into a wall or
 * something solid. Then it has to catch its breath - head out, panting - and that's the time to hit it:
 * in its shell it shrugs most of the damage off. On harder floors the slam knocks loose a little ring of
 * pebbles.
 */
public class Turtle extends Enemy {

    private static final Color SHELL = Color.valueOf("4f8a4a");
    private static final Color SHELL_DARK = Color.valueOf("365e34");
    private static final Color SHELL_RIM = Color.valueOf("c8a86a");
    private static final Color SKIN = Color.valueOf("8ac07a");
    private static final Color SKIN_DARK = Color.valueOf("5e8a52");
    private static final Color SWEAT = Color.valueOf("9ad8ff");
    private static final Color PEBBLE = Color.valueOf("b8b0a0");
    private static final Color TONGUE = Color.valueOf("e86a7a");

    private enum State { PADDLE, TUCK, RAM, TIRED }

    private static final float TUCK_TIME = 0.55f;
    private static final float LOCK = 0.18f;
    private static final float RAM_TIME = 1.4f;
    private static final float RAM_SPEED = 4.6f;
    private static final float TIRED_TIME = 1.6f;
    /** In its shell it only takes this share of the damage. */
    private static final float SHELL_ARMOUR = 0.25f;

    private State state = State.PADDLE;
    private float timer = MathUtils.random(1.5f, 2.5f);
    private float ramAngle;
    private float shellSpin;
    private final Vector2 nose = new Vector2();

    public Turtle(float x, float y) {
        super(x, y, 42f);
        radius = 0.24f;
        speed = 0.5f;
        score = 35;
    }

    @Override
    public float feetY() {
        return pos.y - 0.16f;
    }

    private boolean inShell() {
        return state == State.TUCK || state == State.RAM;
    }

    @Override
    public void hit(float damage, Vector2 bulletVel, float knockback, GameWorld world) {
        if (alive && inShell()) { // bullets glance off the shell
            damage *= SHELL_ARMOUR;
            knockback = 0f;
            world.particles().burst(pos.x, pos.y + 0.05f, 3, 0.3f, 1.2f, 0.15f, 0.025f, Color.WHITE);
        }
        super.hit(damage, bulletVel, knockback, world);
    }

    @Override
    protected void behave(float delta, GameWorld world) {
        float diff = world.getDifficulty();
        timer -= delta;
        switch (state) {
            case PADDLE:
                keepDistance(delta, world, 2.4f, speed);
                if (timer <= 0 && canShoot(world)) {
                    state = State.TUCK;
                    timer = TUCK_TIME / Math.min(1.2f, diff);
                }
                break;
            case TUCK:
                shellSpin += delta * 900f * (1f - Math.max(0f, timer) / TUCK_TIME);
                if (timer > LOCK) {
                    ramAngle = aimAngle;
                }
                if (timer <= 0) {
                    state = State.RAM;
                    timer = RAM_TIME;
                    world.sfx().play("dodge", 0.7f);
                }
                break;
            case RAM:
                knock.setZero();
                shellSpin += delta * 1200f;
                float s = RAM_SPEED * Math.min(1.3f, diff) * delta;
                pos.add(MathUtils.cosDeg(ramAngle) * s, MathUtils.sinDeg(ramAngle) * s);
                nose.set(pos).add(MathUtils.cosDeg(ramAngle) * (radius + 0.06f), MathUtils.sinDeg(ramAngle) * (radius + 0.06f));
                if (!Arena.now.contains(nose.x, nose.y, 0f) || !world.pointIsFree(nose.x, nose.y, 0.04f)) {
                    bonk(world, diff);
                } else if (timer <= 0) {
                    state = State.TIRED;
                    timer = TIRED_TIME;
                }
                break;
            default: // TIRED: catching its breath
                if (MathUtils.random() < delta * 2f) {
                    world.particles().spawn(pos.x + MathUtils.random(-0.1f, 0.1f), pos.y + 0.2f, 0f, 0.4f, 0.6f, 0.025f,
                            SWEAT, 2f);
                }
                if (timer <= 0) {
                    state = State.PADDLE;
                    timer = MathUtils.random(1.8f, 2.6f) / diff;
                }
                break;
        }
    }

    /** Slammed into something: a thud, a little dizzy - and on harder floors a ring of pebbles knocked loose. */
    private void bonk(GameWorld world, float diff) {
        state = State.TIRED;
        timer = TIRED_TIME;
        pos.sub(MathUtils.cosDeg(ramAngle) * 0.08f, MathUtils.sinDeg(ramAngle) * 0.08f);
        world.shake(0.15f);
        world.sfx().play("break", 0.5f);
        world.particles().burst(nose.x, nose.y, 10, 0.3f, 1.4f, 0.4f, 0.04f, PEBBLE);
        if (diff > 1.2f) {
            for (int i = 0; i < 8; i++) {
                Bullet b = shoot(world, ramAngle + 180f + i * 45f, 1.8f * diff);
                b.color.set(PEBBLE);
                b.shape = Bullet.Shape.SQUARE;
            }
        }
    }

    @Override
    protected void drawBody(SpriteBatch b, Draw d) {
        float face = facingRight() ? 1f : -1f;
        float x = pos.x;
        float y = pos.y;
        boolean shell = inShell();
        boolean tired = state == State.TIRED;
        if (!shell) { // flippers and head poke out
            float paddle = MathUtils.sin(walkTime * 6f) * 20f;
            for (int i = -1; i <= 1; i += 2) {
                d.rotRect(b, x + 0.17f * i, y - 0.1f, 0.14f, 0.07f, i * (20f + paddle), c(SKIN_DARK), 1f);
                d.rotRect(b, x + 0.15f * i, y + 0.08f, 0.15f, 0.07f, i * (-25f - paddle), c(SKIN), 1f);
            }
            float pant = tired ? MathUtils.sin(timer * 20f) * 0.02f : 0f;
            float hx = x + face * 0.26f;
            float hy = y + 0.04f + pant;
            d.circle(b, hx, hy, 0.105f, Draw.OUTLINE_COLOR);
            d.circle(b, hx, hy, 0.095f, c(SKIN));
            if (tired) { // eyes shut, tongue out
                d.rect(b, hx - 0.05f, hy + 0.02f, 0.035f, 0.012f, Draw.OUTLINE_COLOR);
                d.rect(b, hx + 0.015f, hy + 0.02f, 0.035f, 0.012f, Draw.OUTLINE_COLOR);
                d.rect(b, hx + face * 0.04f - 0.015f, hy - 0.07f, 0.03f, 0.04f, TONGUE);
            } else {
                drawEyes(b, d, hx, hy + 0.02f, 0.035f, 0.025f);
            }
        }
        // the shell: a dome with a pattern of plates (it spins while ramming)
        Color glow = state == State.TUCK ? TELEGRAPH : null;
        d.circle(b, x, y, 0.235f, Draw.OUTLINE_COLOR);
        d.circle(b, x, y, 0.22f, c(SHELL_RIM));
        d.circle(b, x, y + 0.015f, 0.19f, c(glow != null && ((int) (timer * 16f)) % 2 == 0 ? glow : SHELL));
        for (int i = 0; i < 6; i++) {
            float a = shellSpin + i * 60f;
            d.circle(b, x + MathUtils.cosDeg(a) * 0.12f, y + 0.015f + MathUtils.sinDeg(a) * 0.1f, 0.045f, c(SHELL_DARK));
        }
        d.circle(b, x, y + 0.015f, 0.06f, c(SHELL_DARK));
        d.circle(b, x - 0.06f, y + 0.1f, 0.03f, Color.WHITE, 0.4f); // a shine
    }
}
