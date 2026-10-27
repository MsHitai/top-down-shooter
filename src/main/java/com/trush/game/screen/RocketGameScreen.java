package com.trush.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.trush.game.enemy.Alien;
import com.trush.game.enemy.LivingAsteroid;
import com.trush.game.gfx.Draw;
import com.trush.game.input.Gamepad;
import com.trush.game.input.Gamepad.Button;
import com.trush.game.launcher.ETGFanfic;

/**
 * The rocket game on the console by the TV at home (it turns up once the family's rocket has been up to
 * the Moon - {@code Unlocks.boardedRocket}): the ride up made into a score chase. The rocket flies up
 * through space, steered and fired like on the ride ({@link RocketRideScreen#fireHeld}), but here bumps
 * hurt: three hearts, meteors (the Moon's grumpy living asteroids - big ones crack into two small ones)
 * and little aliens that beam in at the top and shoot fans of green orbs. It gets busier the longer you
 * last. Aliens sometimes drop a heart or a star (triple shots for a while). The best score is kept
 * (prefs {@code rocketBest}, {@link ETGFanfic#getRocketBest}) and shown on the trophy shelf.
 * Esc / Start goes back to the living room.
 */
public class RocketGameScreen implements Screen {

    private static final int HEARTS = 3;
    private static final float SPEED = 3.4f;
    /** The rocket's hitbox, smaller than on the ride: the orbs need room to miss. */
    private static final float RADIUS = 0.2f;
    private static final float HURT_TIME = 1.5f;
    private static final float TRIPLE_TIME = 8f;
    private static final float ORB_SPEED = 1.9f;
    private static final float PICKUP_CHANCE = 0.3f;
    /** The first alien beams in after this long. */
    private static final float ALIENS_FROM = 8f;

    private static final Color SPACE = Color.valueOf("070818");
    private static final Color SHOT = Color.valueOf("ffd166");
    private static final Color ORB = Color.valueOf("8aff6a");
    private static final Color ORB_CORE = Color.valueOf("e8ffe0");
    private static final Color BEAM = Color.valueOf("c8ffb4");
    private static final Color ROCK = Color.valueOf("8e8a86");
    private static final Color ROCK_LIGHT = Color.valueOf("b8b4ae");
    private static final Color PLANET = Color.valueOf("c87ad0");
    private static final Color PLANET_RING = Color.valueOf("f0c0ff");
    private static final Color TEXT = Color.valueOf("ffd166");
    private static final Color DIM = Color.valueOf("8a8079");
    private static final Color PANEL = Color.valueOf("1e1416");

    private static final class Meteor {
        final LivingAsteroid look = new LivingAsteroid(0, 0);
        float vx;
        float vy;
        float r;
        int hp;
        float flash;
    }

    private enum AlienPhase { IN, HOVER, OUT }

    private static final class Ufo {
        final Alien look = new Alien(0, 0);
        AlienPhase phase = AlienPhase.IN;
        float timer;
        float x;
        float y;
        float vx;
        float fireTimer;
        int volleys;
        int hp = 3;
        float flash;
    }

    private static final class Orb {
        float x;
        float y;
        float vx;
        float vy;
    }

    private static final class Bonus {
        float x;
        float y;
        boolean heart;
    }

    private static final class Bit {
        float x;
        float y;
        float vx;
        float vy;
        float life;
        float size;
        Color color;
    }

    private final ETGFanfic game;
    private Music music;
    private float time;
    private boolean over;
    private float overTime;
    private boolean newBest;
    private boolean leaving;
    private final MenuList overMenu = new MenuList(2.05f, 0.3f, 0.42f);

    private final Vector2 rocket = new Vector2(3.5f, 1.1f);
    private final Vector2 move = new Vector2();
    private int hearts = HEARTS;
    private float hurtTimer;
    private float tripleTimer;
    private float fireTimer;
    private int score;
    private float meteorTimer = 1.5f;
    private float alienTimer = ALIENS_FROM;
    private final Array<Meteor> meteors = new Array<>();
    private final Array<Ufo> ufos = new Array<>();
    private final Array<Vector2> shots = new Array<>();
    private final Array<Orb> orbs = new Array<>();
    private final Array<Bonus> bonuses = new Array<>();
    private final Array<Bit> bits = new Array<>();

    public RocketGameScreen(ETGFanfic game) {
        this.game = game;
        overMenu.setItems("PLAY AGAIN", "GO HOME");
    }

    @Override
    public void show() {
        music = Gdx.audio.newMusic(Gdx.files.internal("assets/music/moon.ogg"));
        music.setLooping(true);
        music.setVolume(0.45f * game.getMusicVolume());
        music.play();
    }

    /** 0 at the start, 1 after two minutes: more meteors, more aliens, faster orbs. */
    private float busy() {
        return Math.min(1f, time / 120f);
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1 / 30f);
        OrthographicCamera cam = (OrthographicCamera) game.viewport.getCamera();
        cam.position.set(game.viewport.getWorldWidth() / 2f, game.viewport.getWorldHeight() / 2f, 0);
        cam.update();
        game.viewport.apply();
        update(delta);
        if (leaving) {
            return;
        }
        ScreenUtils.clear(Color.BLACK);
        SpriteBatch b = game.batch;
        b.setProjectionMatrix(cam.combined);
        b.begin();
        draw(b, game.draw);
        b.end();
    }

    // ------------------------------------------------------------ update

    private void update(float delta) {
        Gamepad pad = game.pad;
        boolean back = Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || pad.justPressed(Button.START);
        if (over) {
            overTime += delta;
            updateBits(delta);
            if (overTime < 1f) { // (a fire button still held mustn't pick for you)
                return;
            }
            int chosen = overMenu.update(game);
            if (back || pad.justPressed(Button.B) || chosen == 1) {
                leave(new HubScreen(game));
            } else if (chosen == 0) {
                leave(new RocketGameScreen(game));
            }
            return;
        }
        if (back) {
            leave(new HubScreen(game));
            return;
        }
        time += delta;
        hurtTimer = Math.max(0f, hurtTimer - delta);
        tripleTimer = Math.max(0f, tripleTimer - delta);

        move.setZero();
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) move.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) move.x += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) move.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) move.y -= 1;
        if (move.isZero()) {
            move.set(pad.leftStick());
        }
        move.limit(1f);
        rocket.mulAdd(move, SPEED * delta);
        rocket.x = MathUtils.clamp(rocket.x, 0.4f, 6.6f);
        rocket.y = MathUtils.clamp(rocket.y, 0.7f, 3.9f);

        fireTimer -= delta;
        if (fireTimer <= 0 && RocketRideScreen.fireHeld(game)) {
            fireTimer = RocketRideScreen.FIRE_EVERY;
            shots.add(new Vector2(rocket.x - 0.12f, rocket.y + 0.75f));
            shots.add(new Vector2(rocket.x + 0.12f, rocket.y + 0.75f));
            if (tripleTimer > 0) { // the star: one more down the middle, a little ahead
                shots.add(new Vector2(rocket.x, rocket.y + 0.85f));
            }
            game.sfx.play("shoot", 0.12f);
        }
        for (int i = shots.size - 1; i >= 0; i--) {
            Vector2 s = shots.get(i);
            s.y += RocketRideScreen.BULLET_SPEED * delta;
            if (s.y > 5.3f) shots.removeIndex(i);
        }

        spawn(delta);
        updateMeteors(delta);
        updateUfos(delta);
        updateOrbs(delta);
        updateBonuses(delta);
        updateBits(delta);
    }

    private void spawn(float delta) {
        float k = busy();
        meteorTimer -= delta;
        if (meteorTimer <= 0) {
            meteorTimer = MathUtils.lerp(1.0f, 0.4f, k) * MathUtils.random(0.7f, 1.3f);
            float r = MathUtils.random() < 0.15f + 0.25f * k ? MathUtils.random(0.3f, 0.36f) : MathUtils.random(0.15f, 0.24f);
            addMeteor(MathUtils.random(0.3f, 6.7f), 5.4f, r, MathUtils.random(-0.5f, 0.5f),
                    -MathUtils.random(1.2f, 2f + 1.2f * k));
        }
        alienTimer -= delta;
        int most = Math.min(4, 1 + (int) (time / 30f));
        if (time > ALIENS_FROM && alienTimer <= 0 && ufos.size < most) {
            alienTimer = MathUtils.lerp(6f, 2.5f, k) * MathUtils.random(0.8f, 1.2f);
            Ufo u = new Ufo();
            u.x = MathUtils.random(0.6f, 6.4f);
            u.y = MathUtils.random(3.6f, 4.4f);
            u.vx = MathUtils.randomSign() * MathUtils.random(0.4f, 0.8f);
            u.timer = 0.5f;
            u.fireTimer = MathUtils.random(0.8f, 1.4f);
            ufos.add(u);
            game.sfx.play("zap", 0.35f);
        }
    }

    private void addMeteor(float x, float y, float r, float vx, float vy) {
        Meteor m = new Meteor();
        m.look.pos.set(x, y);
        m.look.radius = r;
        m.r = r;
        m.vx = vx;
        m.vy = vy;
        m.hp = r > 0.28f ? 4 : r > 0.2f ? 2 : 1;
        meteors.add(m);
    }

    private void updateMeteors(float delta) {
        for (int i = meteors.size - 1; i >= 0; i--) {
            Meteor m = meteors.get(i);
            Vector2 p = m.look.pos;
            p.add(m.vx * delta, m.vy * delta);
            m.flash = Math.max(0f, m.flash - delta);
            boolean gone = p.y < -0.6f;
            for (int j = shots.size - 1; j >= 0 && !gone; j--) {
                Vector2 s = shots.get(j);
                if (Vector2.dst2(s.x, s.y, p.x, p.y) < m.r * m.r) {
                    shots.removeIndex(j);
                    burst(s.x, s.y, 3, ROCK_LIGHT);
                    m.flash = 0.08f;
                    if (--m.hp <= 0) {
                        gone = true;
                        score += m.r > 0.28f ? 30 : m.r > 0.2f ? 15 : 10;
                        burst(p.x, p.y, 10, ROCK);
                        game.sfx.play("break", 0.5f);
                        if (m.r > 0.28f) { // a big one cracks into two small ones
                            addMeteor(p.x - 0.1f, p.y, 0.17f, -0.9f, m.vy * 0.8f);
                            addMeteor(p.x + 0.1f, p.y, 0.17f, 0.9f, m.vy * 0.8f);
                        }
                    }
                }
            }
            if (!gone && touchesRocket(p.x, p.y, m.r * 0.85f)) {
                gone = hurt(); // (blinking after a hit, the rocket just flies through)
                if (gone) burst(p.x, p.y, 12, ROCK);
            }
            if (gone) meteors.removeIndex(i);
        }
    }

    private void updateUfos(float delta) {
        float k = busy();
        for (int i = ufos.size - 1; i >= 0; i--) {
            Ufo u = ufos.get(i);
            u.timer -= delta;
            u.flash = Math.max(0f, u.flash - delta);
            if (u.phase == AlienPhase.IN) {
                if (u.timer <= 0) u.phase = AlienPhase.HOVER;
                continue;
            }
            if (u.phase == AlienPhase.OUT) {
                if (u.timer <= 0) ufos.removeIndex(i);
                continue;
            }
            // drifting to and fro along the top, leaning towards the rocket
            u.x += (u.vx + (rocket.x - u.x) * 0.15f) * delta;
            if (u.x < 0.4f && u.vx < 0 || u.x > 6.6f && u.vx > 0) u.vx = -u.vx;
            u.y += MathUtils.sin(time * 2f + i) * 0.15f * delta;
            u.fireTimer -= delta;
            if (u.fireTimer <= 0) {
                int n = k > 0.5f ? 5 : 3;
                float aim = MathUtils.atan2Deg360(rocket.y + 0.1f - u.y, rocket.x - u.x);
                float speed = ORB_SPEED * (1f + 0.4f * k);
                for (int j = 0; j < n; j++) {
                    float a = aim + (j - (n - 1) / 2f) * 16f;
                    Orb o = new Orb();
                    o.x = u.x;
                    o.y = u.y - 0.05f;
                    o.vx = MathUtils.cosDeg(a) * speed;
                    o.vy = MathUtils.sinDeg(a) * speed;
                    orbs.add(o);
                }
                game.sfx.play("enemy_shoot", 0.35f);
                u.fireTimer = MathUtils.lerp(2.2f, 1.3f, k) * MathUtils.random(0.85f, 1.15f);
                if (++u.volleys >= 4) { // done: beams off again
                    u.phase = AlienPhase.OUT;
                    u.timer = 0.5f;
                    game.sfx.play("zap", 0.3f);
                    continue;
                }
            }
            for (int j = shots.size - 1; j >= 0; j--) {
                Vector2 s = shots.get(j);
                if (Vector2.dst2(s.x, s.y, u.x, u.y + 0.03f) < 0.2f * 0.2f) {
                    shots.removeIndex(j);
                    u.flash = 0.08f;
                    burst(s.x, s.y, 3, ORB);
                    if (--u.hp <= 0) {
                        score += 50;
                        burst(u.x, u.y, 14, ORB);
                        game.sfx.play("enemy_die", 0.5f);
                        if (MathUtils.random() < PICKUP_CHANCE) {
                            Bonus bonus = new Bonus();
                            bonus.x = u.x;
                            bonus.y = u.y;
                            bonus.heart = hearts < HEARTS && MathUtils.randomBoolean();
                            bonuses.add(bonus);
                        }
                        ufos.removeIndex(i);
                        break;
                    }
                }
            }
            if (u.hp > 0 && touchesRocket(u.x, u.y, 0.15f)) {
                hurt();
            }
        }
    }

    private void updateOrbs(float delta) {
        for (int i = orbs.size - 1; i >= 0; i--) {
            Orb o = orbs.get(i);
            o.x += o.vx * delta;
            o.y += o.vy * delta;
            boolean gone = o.x < -0.3f || o.x > 7.3f || o.y < -0.3f || o.y > 5.3f;
            if (!gone && touchesRocket(o.x, o.y, 0.05f)) {
                gone = hurt();
            }
            if (gone) orbs.removeIndex(i);
        }
    }

    private void updateBonuses(float delta) {
        for (int i = bonuses.size - 1; i >= 0; i--) {
            Bonus p = bonuses.get(i);
            p.y -= 0.9f * delta;
            if (touchesRocket(p.x, p.y, 0.15f)) {
                if (p.heart) {
                    hearts = Math.min(HEARTS, hearts + 1);
                    game.sfx.play("heal", 0.6f);
                } else {
                    tripleTimer = TRIPLE_TIME;
                    game.sfx.play("pop", 0.6f);
                }
                bonuses.removeIndex(i);
            } else if (p.y < -0.3f) {
                bonuses.removeIndex(i);
            }
        }
    }

    private void updateBits(float delta) {
        for (int i = bits.size - 1; i >= 0; i--) {
            Bit p = bits.get(i);
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.life -= delta;
            if (p.life <= 0) bits.removeIndex(i);
        }
    }

    private boolean touchesRocket(float x, float y, float r) {
        float rr = r + RADIUS;
        return Vector2.dst2(rocket.x, rocket.y + 0.1f, x, y) < rr * rr;
    }

    /** Something hit the rocket: a heart gone (unless it's still blinking from the last hit); true if it counted. */
    private boolean hurt() {
        if (hurtTimer > 0 || over) {
            return false;
        }
        hearts--;
        hurtTimer = HURT_TIME;
        if (hearts <= 0) {
            gameOver();
        } else {
            game.sfx.play("hurt", 0.6f);
        }
        return true;
    }

    private void gameOver() {
        over = true;
        burst(rocket.x, rocket.y, 30, SHOT);
        burst(rocket.x, rocket.y, 20, ROCK_LIGHT);
        game.sfx.play("boom", 0.7f);
        newBest = game.setRocketBest(score);
        overMenu.select(0);
    }

    private void burst(float x, float y, int n, Color color) {
        for (int i = 0; i < n; i++) {
            Bit p = new Bit();
            p.x = x;
            p.y = y;
            float a = MathUtils.random(MathUtils.PI2);
            float sp = MathUtils.random(0.5f, 2f);
            p.vx = MathUtils.cos(a) * sp;
            p.vy = MathUtils.sin(a) * sp;
            p.life = MathUtils.random(0.3f, 0.6f);
            p.size = MathUtils.random(0.03f, 0.07f);
            p.color = color;
            bits.add(p);
        }
    }

    private void leave(Screen next) {
        leaving = true;
        game.setScreen(next);
        dispose();
    }

    // ------------------------------------------------------------ draw

    private void draw(SpriteBatch b, Draw d) {
        d.rect(b, 0, 0, 7, 5, SPACE);
        for (int i = 0; i < 70; i++) { // stars streaming past, the near ones faster
            float sx = (i * 2.371f) % 7f;
            float sy = ((i * 1.913f) - time * (0.6f + (i % 3) * 0.6f)) % 5.2f;
            if (sy < 0) sy += 5.2f;
            float s = i % 7 == 0 ? 0.03f : 0.018f;
            d.rect(b, sx, sy, s, s, Color.WHITE, 0.6f + 0.3f * (i % 3) / 2f);
        }
        // a ringed planet drifting slowly by now and then
        float py = 6.5f - (time * 0.12f) % 9f;
        d.circle(b, 1.3f, py, 0.5f, PLANET, 0.7f);
        d.rotRect(b, 1.3f, py, 1.5f, 0.07f, -12f, PLANET_RING, 0.6f);

        for (Bonus p : bonuses) {
            float pulse = 1f + 0.15f * MathUtils.sin(time * 8f);
            if (p.heart) {
                d.texture(b, d.heartFull, p.x - 0.13f * pulse, p.y - 0.12f * pulse, 0.26f * pulse, 0.23f * pulse);
            } else {
                d.circle(b, p.x, p.y, 0.16f * pulse, SHOT, 0.3f);
                for (int i = 0; i < 5; i++) { // a little star
                    d.line(b, p.x, p.y, 0.12f * pulse, 0.05f, 90f + i * 72f + time * 90f, SHOT);
                }
            }
        }
        for (Meteor m : meteors) {
            m.look.drawPortrait(b, d, 270f);
            if (m.flash > 0) d.circle(b, m.look.pos.x, m.look.pos.y, m.r, Color.WHITE, 0.6f);
        }
        for (Ufo u : ufos) {
            drawUfo(b, d, u);
        }
        for (Vector2 s : shots) {
            d.rect(b, s.x - 0.025f, s.y - 0.09f, 0.05f, 0.18f, SHOT);
        }
        for (Orb o : orbs) {
            d.circle(b, o.x, o.y, 0.075f, ORB, 0.9f);
            d.circle(b, o.x, o.y, 0.035f, ORB_CORE);
        }
        for (Bit p : bits) {
            d.rect(b, p.x - p.size / 2f, p.y - p.size / 2f, p.size, p.size, p.color, Math.min(1f, p.life * 3f));
        }
        if (!over) {
            boolean blink = hurtTimer > 0 && ((int) (time * 20f)) % 2 == 0;
            RocketRideScreen.drawRocket(game, b, d, rocket.x, rocket.y, time, hurtTimer > HURT_TIME - 0.4f, blink);
            if (tripleTimer > 0) {
                d.ring(b, rocket.x, rocket.y + 0.1f, 0.42f, SHOT, Math.min(0.5f, tripleTimer * 0.25f));
            }
        }
        // an old telly: faint scanlines over the picture
        for (float y = 0; y < 5f; y += 0.08f) {
            d.rect(b, 0, y, 7, 0.02f, Color.BLACK, 0.12f);
        }
        drawHud(b, d);
    }

    /** The alien rides its beam in (and out again), then hovers along the top with its ray gun. */
    private void drawUfo(SpriteBatch b, Draw d, Ufo u) {
        float beam = u.phase == AlienPhase.HOVER ? 0f : Math.max(0f, u.timer / 0.5f);
        if (u.phase != AlienPhase.HOVER) {
            float a = u.phase == AlienPhase.IN ? beam : 1f - beam;
            d.rect(b, u.x - 0.14f, u.y - 0.2f, 0.28f, 5.4f - u.y, BEAM, 0.35f * (1f - a) + 0.1f);
            boolean shown = u.phase == AlienPhase.IN ? beam < 0.5f : beam > 0.5f;
            if (!shown || ((int) (time * 25f)) % 2 == 0) return; // flickering as it beams
        }
        u.look.pos.set(u.x, u.y);
        u.look.drawPortrait(b, d, MathUtils.atan2Deg360(rocket.y - u.y, rocket.x - u.x));
        if (u.flash > 0) d.circle(b, u.x, u.y + 0.05f, 0.17f, Color.WHITE, 0.6f);
    }

    private void drawHud(SpriteBatch b, Draw d) {
        game.text("SCORE " + score, 0.15f, 4.88f, 0.32f, TEXT, Align.left);
        game.text("BEST " + Math.max(game.getRocketBest(), over ? 0 : score), 0.15f, 4.58f, 0.22f, DIM, Align.left);
        for (int i = 0; i < HEARTS; i++) {
            Texture t = i < hearts ? d.heartFull : d.heartEmpty;
            d.texture(b, t, 6.85f - (HEARTS - i) * 0.3f, 4.62f, 0.26f, 0.26f * 8f / 9f);
        }
        if (over) {
            float cx = 3.5f;
            d.rect(b, 0, 0, 7, 5, Color.BLACK, Math.min(0.55f, overTime));
            d.rect(b, cx - 2.04f, 1.36f, 4.08f, 2.38f, TEXT, 0.9f);
            d.rect(b, cx - 2f, 1.4f, 4f, 2.3f, PANEL, 0.95f);
            game.text("GAME OVER", cx, 3.5f, 0.6f, TEXT, Align.center);
            game.text("Score: " + score, cx, 3.0f, 0.32f, Color.WHITE, Align.center);
            game.text(newBest ? "NEW BEST!" : "Best: " + game.getRocketBest(), cx, 2.65f, 0.28f,
                    newBest ? TEXT : DIM, Align.center);
            if (overTime >= 1f) {
                overMenu.draw(game, overTime);
            }
            return;
        }
        if (time < 4f) {
            game.text("ROCKET GAME", 3.5f, 3.3f, 0.6f, TEXT, Align.center);
            game.text(RocketRideScreen.fireHint(game), 3.5f, 2.8f, 0.28f, Color.WHITE, Align.center);
        }
        game.text(game.pad.isActive() ? "START home" : "ESC home", 0.1f, 0.18f, 0.2f, Color.WHITE, Align.left);
    }

    @Override
    public void resize(int width, int height) {
        game.viewport.update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        music.dispose();
    }
}
