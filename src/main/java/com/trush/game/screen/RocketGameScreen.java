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
import com.trush.game.enemy.Astronaut;
import com.trush.game.enemy.Boss;
import com.trush.game.enemy.Enemy;
import com.trush.game.enemy.FluffyKing;
import com.trush.game.enemy.Grandfluff;
import com.trush.game.enemy.HollowWitch;
import com.trush.game.enemy.LivingAsteroid;
import com.trush.game.enemy.Messenger;
import com.trush.game.enemy.MoonBunny;
import com.trush.game.enemy.NightHawk;
import com.trush.game.gfx.Draw;
import com.trush.game.input.Gamepad;
import com.trush.game.input.Gamepad.Button;
import com.trush.game.launcher.Dev;
import com.trush.game.launcher.ETGFanfic;

/**
 * The rocket game on the console by the TV at home (it turns up once the family's rocket has been up to
 * the Moon - {@code Unlocks.boardedRocket}): the ride up made into a score chase. The rocket flies up
 * through space, steered and fired like on the ride ({@link RocketRideScreen#fireHeld}), but here bumps
 * hurt - only on the porthole, the hitbox ({@link #WINDOW_RADIUS}): three hearts, meteors (the Moon's grumpy living asteroids - big ones crack into two small ones)
 * and little aliens that beam in at the top and shoot fans of green orbs, then (the owner's wish) moon
 * bunnies hopping down in pairs - a ring of carrots on every hop - and astronauts jetting across firing
 * laser bursts; after a minute and then every {@link #BOSS_EVERY} s a boss floats in at random (a
 * different one from the last; {@link BossKind}): the Fluffy King, the Night Hawk, the Messenger, the
 * Hollow Witch - and Grandfluff once he's been cheered up - each with three attacks of its own, drawn
 * with its real portrait, and a boss bar. It gets busier
 * the longer you last (slowly: {@link #busy} takes four minutes). When a boss comes, everything else is
 * swept away and it's just the two of you. Shot-down enemies sometimes drop a present (a heart or a blank:
 * Q / right click / X clears the bullets, {@link #useBlank}) or a star
 * (one more shot for good, up to {@link #MAX_SHOTS}; {@link #shotLevel}). The best score is kept
 * (prefs {@code rocketBest}, {@link ETGFanfic#getRocketBest}) and shown on the trophy shelf.
 * Esc / Start goes back to the living room.
 */
public class RocketGameScreen implements Screen {

    /** The rocket game was Egor's idea (the owner's son), from the ride up to the Moon onwards. */
    static final String CREDIT = "A GAME BY EGOR";
    private static final int HEARTS = 3;
    private static final float SPEED = 3.4f;
    /**
     * The rocket's hitbox is just its crayon porthole (the owner's idea, like in a bullet-hell game: you
     * weave through the bullets knowing which little circle counts): it sits {@code WINDOW_Y} above the
     * rocket's middle (pixel 21 of the 52-pixel box, drawn at 0.75 scale).
     */
    private static final float WINDOW_RADIUS = 0.075f;
    private static final float WINDOW_Y = 0.165f;
    /** Pickups are caught by the whole rocket, not just the window. */
    private static final float CATCH_RADIUS = 0.35f;
    private static final Color WINDOW_GLOW = Color.valueOf("8fd0ff");
    private static final float HURT_TIME = 1.5f;
    /** How many times the stars can upgrade the rocket's guns: a third shot, two angled ones, faster fire. */
    private static final int MAX_SHOTS = 3;
    private static final float ORB_SPEED = 1.9f;
    /** How often a shot-down enemy drops something (a present or a star). */
    private static final float DROP_ALIEN = 0.3f;
    private static final float DROP_ASTRONAUT = 0.25f;
    private static final float DROP_BUNNY = 0.08f;
    private static final float DROP_METEOR = 0.04f;
    /** Blanks, like in the game: one to start with, presents hold more (up to this many). */
    private static final int MAX_BLANKS = 3;
    /** How long a blank's ring takes to sweep out over the screen. */
    private static final float BLANK_RING = 0.45f;
    /** A present caught with nothing more it could give is worth this much instead. */
    private static final int PRESENT_SCORE = 100;
    /** The first alien beams in after this long. */
    private static final float ALIENS_FROM = 8f;
    private static final float BUNNIES_FROM = 20f;
    private static final float ASTRONAUTS_FROM = 35f;
    private static final float BOSS_FROM = 60f;
    private static final float BOSS_EVERY = 100f;
    private static final float BOSS_HP = 180f;
    /** How long a moon bunny's hop takes; it throws its carrots when it lands. */
    private static final float HOP = 1.1f;
    /** The Fluffy King's attacks in turn: ring, fan, fan again, spiral. */
    private static final int[] KING_ORDER = {0, 1, 1, 2};

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
    private static final Color CARROT = Color.valueOf("ff8a2a");
    private static final Color LASER = Color.valueOf("ff4a5a");
    private static final Color FLUFF = Color.valueOf("ffb4d8");
    private static final Color FLUFF_CORE = Color.valueOf("fff0f8");
    private static final Color BAR = Color.valueOf("ff6aa8");
    private static final Color TRACER = Color.valueOf("ffc84a");
    private static final Color ROCKET_SHELL = Color.valueOf("e05a4a");
    private static final Color BLADE = Color.valueOf("9adcff");
    private static final Color RAIN = Color.valueOf("6ab4ff");
    private static final Color HEX = Color.valueOf("b46aff");
    private static final Color LANTERN = Color.valueOf("ff9a2a");
    private static final Color STAR = Color.valueOf("ffe27a");
    private static final Color PRESENT = Color.valueOf("e8456a");
    private static final Color RIBBON = Color.valueOf("ffd166");

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

    private static final class Bunny {
        final MoonBunny look = new MoonBunny(0, 0);
        float x;
        float y;
        float vx;
        float hop;
        int hp = 2;
        float flash;
    }

    private static final class Spaceman {
        final Astronaut look = new Astronaut(0, 0);
        float x;
        float y;
        float vx;
        float fireTimer = 0.8f;
        int burst;
        float burstTimer;
        int hp = 3;
        float flash;
    }

    /** The bosses that can turn up, all ones that fly; {@code reach} is their hitbox, {@code home} their height. */
    private enum BossKind {
        KING(Boss.FLUFFY_KING, 0.7f, 3.95f),
        HAWK(Boss.NIGHT_HAWK, 0.6f, 3.9f),
        MESSENGER(Boss.MESSENGER, 0.42f, 4.05f),
        WITCH(Boss.WITCH, 0.36f, 4.05f),
        GRANDFLUFF(Boss.GRANDFLUFF, 0.7f, 3.75f);

        final Boss boss;
        final float reach;
        final float home;

        BossKind(Boss boss, float reach, float home) {
            this.boss = boss;
            this.reach = reach;
            this.home = home;
        }

        Enemy create() {
            switch (this) {
                case HAWK:
                    return new NightHawk(0, 0, 0);
                case MESSENGER:
                    return new Messenger(0, 0, 0);
                case WITCH:
                    return new HollowWitch(0, 0, 0);
                case GRANDFLUFF:
                    return new Grandfluff(0, 0, 0);
                default:
                    return new FluffyKing(0, 0, 0);
            }
        }
    }

    /** A boss floating along the top. */
    private static final class BigOne {
        final BossKind kind;
        final Enemy look;
        float x = 3.5f;
        float y = 6.5f;
        float hp;
        float maxHp;
        float t;
        /** Down at its height yet; the sway only starts then, from the middle, so it never jumps. */
        boolean arrived;
        float swayTime;
        int attack;
        float attackTimer = 1.5f;
        /** A spiral / stream / sweep going on for this much longer. */
        float streamTimer;
        float streamAngle;
        float flash;

        BigOne(BossKind kind) {
            this.kind = kind;
            this.look = kind.create();
        }
    }

    private static final class Orb {
        float x;
        float y;
        float vx;
        float vy;
        float r = 0.075f;
        Color color = ORB;
        Color core = ORB_CORE;
        /** Waits this long first, shown as a ring (harmless till then). */
        float delay;
        /** Turns this many degrees a second. */
        float curve;
        /** Bursts into a ring of {@code burstN} after this long (0: never). */
        float burstIn;
        int burstN;
        /** Leaves a slow sparkle behind every this many seconds (0: none). */
        float trailEvery;
        float trailTimer;
    }

    private static final class Bonus {
        float x;
        float y;
        /** A present (a heart or a blank inside), or else a star (the guns' upgrade). */
        boolean heart;
    }

    private static final class Shot {
        float x;
        float y;
        float vx;

        Shot(float x, float y, float vx) {
            this.x = x;
            this.y = y;
            this.vx = vx;
        }
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
    private int blanks = 1;
    /** How long ago a blank went off (its ring sweeping out), or below 0 for none. */
    private float blankTime = -1f;
    private final Vector2 blankAt = new Vector2();
    private float hurtTimer;
    /** Stars caught (0 to {@link #MAX_SHOTS}): kept for the whole game. */
    private int shotLevel;
    private float fireTimer;
    private int score;
    private float meteorTimer = 1.5f;
    private float alienTimer = ALIENS_FROM;
    private float bunnyTimer = BUNNIES_FROM;
    private float astronautTimer = ASTRONAUTS_FROM;
    private float bossTimer = Dev.flag("bossRoom") ? 3f : BOSS_FROM; // (dev: a boss right away)
    private int bossesBeaten;
    private BigOne boss;
    private BossKind lastBoss;
    private final Array<BossKind> bossBag = new Array<>();
    private final Array<Meteor> meteors = new Array<>();
    private final Array<Ufo> ufos = new Array<>();
    private final Array<Bunny> bunnies = new Array<>();
    private final Array<Spaceman> spacemen = new Array<>();
    private final Array<Shot> shots = new Array<>();
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

    /** 0 at the start, 1 after four minutes: more meteors, more aliens, faster orbs. */
    private float busy() {
        return Math.min(1f, time / 240f);
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
            fireTimer = RocketRideScreen.FIRE_EVERY * (shotLevel >= 3 ? 0.75f : 1f);
            shots.add(new Shot(rocket.x - 0.12f, rocket.y + 0.75f, 0f));
            shots.add(new Shot(rocket.x + 0.12f, rocket.y + 0.75f, 0f));
            if (shotLevel >= 1) { // the first star: one more down the middle, a little ahead
                shots.add(new Shot(rocket.x, rocket.y + 0.85f, 0f));
            }
            if (shotLevel >= 2) { // the second: two going out at an angle
                float side = RocketRideScreen.BULLET_SPEED * MathUtils.sinDeg(10f);
                shots.add(new Shot(rocket.x - 0.2f, rocket.y + 0.6f, -side));
                shots.add(new Shot(rocket.x + 0.2f, rocket.y + 0.6f, side));
            }
            game.sfx.play("shoot", 0.12f);
        }
        for (int i = shots.size - 1; i >= 0; i--) {
            Shot s = shots.get(i);
            s.x += s.vx * delta;
            s.y += RocketRideScreen.BULLET_SPEED * delta;
            if (s.y > 5.3f || s.x < -0.2f || s.x > 7.2f) shots.removeIndex(i);
        }

        if (blankTime >= 0) {
            blankTime += delta;
            if (blankTime > BLANK_RING) blankTime = -1f;
        }
        if (blankPressed()) {
            useBlank();
        }
        spawn(delta);
        updateMeteors(delta);
        updateUfos(delta);
        updateBunnies(delta);
        updateSpacemen(delta);
        updateBoss(delta);
        updateOrbs(delta);
        updateBonuses(delta);
        updateBits(delta);
    }

    private void spawn(float delta) {
        float k = busy();
        if (boss == null) {
            bossTimer -= delta;
            if (bossTimer <= 0) {
                boss = new BigOne(pickBoss());
                boss.maxHp = BOSS_HP + 80f * bossesBeaten;
                boss.hp = boss.maxHp;
                lastBoss = boss.kind;
                game.sfx.play("boss", 0.6f);
                clearForBoss();
            }
        }
        if (boss != null) { // the boss has the sky to itself
            return;
        }
        meteorTimer -= delta;
        if (meteorTimer <= 0) {
            meteorTimer = MathUtils.lerp(1.1f, 0.5f, k) * MathUtils.random(0.7f, 1.3f);
            float r = MathUtils.random() < 0.15f + 0.25f * k ? MathUtils.random(0.3f, 0.36f) : MathUtils.random(0.15f, 0.24f);
            addMeteor(MathUtils.random(0.3f, 6.7f), 5.4f, r, MathUtils.random(-0.5f, 0.5f),
                    -MathUtils.random(1.2f, 2f + 1.2f * k));
        }
        bunnyTimer -= delta;
        if (bunnyTimer <= 0) {
            bunnyTimer = MathUtils.lerp(9f, 4.5f, k) * MathUtils.random(0.8f, 1.2f);
            int n = k > 0.6f ? 3 : 2;
            float x0 = MathUtils.random(0.8f, 6.2f - (n - 1) * 0.6f);
            for (int i = 0; i < n; i++) {
                Bunny bun = new Bunny();
                bun.x = x0 + i * 0.6f;
                bun.y = 5.3f + i * 0.3f;
                bun.vx = MathUtils.random(-0.4f, 0.4f);
                bun.hop = i * 0.25f;
                bunnies.add(bun);
            }
        }
        astronautTimer -= delta;
        if (time > ASTRONAUTS_FROM && astronautTimer <= 0 && spacemen.size < 1 + (int) (k * 2f)) {
            astronautTimer = MathUtils.lerp(8f, 4f, k) * MathUtils.random(0.8f, 1.2f);
            Spaceman a = new Spaceman();
            boolean left = MathUtils.randomBoolean();
            a.x = left ? -0.3f : 7.3f;
            a.y = MathUtils.random(2.8f, 4.1f);
            a.vx = (left ? 1f : -1f) * MathUtils.random(0.7f, 1f);
            spacemen.add(a);
        }
        alienTimer -= delta;
        int most = Math.min(3, 1 + (int) (time / 50f));
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
                Shot s = shots.get(j);
                if (Vector2.dst2(s.x, s.y, p.x, p.y) < m.r * m.r) {
                    shots.removeIndex(j);
                    burst(s.x, s.y, 3, ROCK_LIGHT);
                    m.flash = 0.08f;
                    if (--m.hp <= 0) {
                        gone = true;
                        score += m.r > 0.28f ? 30 : m.r > 0.2f ? 15 : 10;
                        burst(p.x, p.y, 10, ROCK);
                        game.sfx.play("break", 0.5f);
                        drop(p.x, p.y, m.r > 0.28f ? DROP_METEOR * 2.5f : DROP_METEOR);
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
                float aim = aimAt(u.x, u.y);
                float speed = ORB_SPEED * (1f + 0.4f * k);
                for (int j = 0; j < n; j++) {
                    orb(u.x, u.y - 0.05f, aim + (j - (n - 1) / 2f) * 16f, speed, 0.075f, ORB, ORB_CORE);
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
                Shot s = shots.get(j);
                if (Vector2.dst2(s.x, s.y, u.x, u.y + 0.03f) < 0.2f * 0.2f) {
                    shots.removeIndex(j);
                    u.flash = 0.08f;
                    burst(s.x, s.y, 3, ORB);
                    if (--u.hp <= 0) {
                        score += 50;
                        burst(u.x, u.y, 14, ORB);
                        game.sfx.play("enemy_die", 0.5f);
                        drop(u.x, u.y, DROP_ALIEN);
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

    /**
     * The next boss out of a shuffled bag: each of them once, in a random order, before any comes again
     * (and never the one just beaten twice running). Dev {@code -Dfluffy.boss=messenger}: always that one.
     */
    private BossKind pickBoss() {
        String dev = Dev.text("boss");
        if (dev != null) {
            Boss wanted = Boss.byName(dev);
            for (BossKind kind : BossKind.values()) {
                if (kind.boss == wanted) return kind;
            }
        }
        if (bossBag.isEmpty()) {
            for (BossKind kind : BossKind.values()) {
                if (kind == BossKind.GRANDFLUFF && game.getTrueEndings() == 0) continue; // (a secret till you've met him)
                bossBag.add(kind);
            }
            bossBag.shuffle();
            if (bossBag.size > 1 && bossBag.first() == lastBoss) bossBag.swap(0, bossBag.size - 1);
        }
        return bossBag.removeIndex(0);
    }

    private Orb orb(float x, float y, float angle, float speed, float r, Color color, Color core) {
        Orb o = new Orb();
        o.x = x;
        o.y = y;
        o.vx = MathUtils.cosDeg(angle) * speed;
        o.vy = MathUtils.sinDeg(angle) * speed;
        o.r = r;
        o.color = color;
        o.core = core;
        orbs.add(o);
        return o;
    }

    private float aimAt(float x, float y) {
        return MathUtils.atan2Deg360(rocket.y + 0.1f - y, rocket.x - x);
    }

    /** One of the rocket's shots within {@code r} of (x, y): it's used up (sparks) and true. */
    private boolean shotHits(float x, float y, float r, Color sparks) {
        for (int j = shots.size - 1; j >= 0; j--) {
            Shot s = shots.get(j);
            if (Vector2.dst2(s.x, s.y, x, y) < r * r) {
                shots.removeIndex(j);
                burst(s.x, s.y, 3, sparks);
                return true;
            }
        }
        return false;
    }

    /** Bunnies hop down the screen in floaty arcs; every landing throws a ring of carrots. */
    private void updateBunnies(float delta) {
        float k = busy();
        for (int i = bunnies.size - 1; i >= 0; i--) {
            Bunny bun = bunnies.get(i);
            bun.flash = Math.max(0f, bun.flash - delta);
            bun.y -= (0.55f + 0.3f * k) * delta;
            bun.x += bun.vx * delta;
            if (bun.x < 0.4f && bun.vx < 0 || bun.x > 6.6f && bun.vx > 0) bun.vx = -bun.vx;
            bun.hop += delta;
            if (bun.hop >= HOP) {
                bun.hop -= HOP;
                if (bun.y < 4.9f) {
                    int n = k > 0.5f ? 8 : 6;
                    float turn = MathUtils.random(360f);
                    for (int j = 0; j < n; j++) {
                        orb(bun.x, bun.y, turn + j * 360f / n, 1.4f + 0.4f * k, 0.065f, CARROT, ORB_CORE);
                    }
                    game.sfx.play("pop", 0.25f);
                }
            }
            float lift = MathUtils.sin(bun.hop / HOP * MathUtils.PI) * 0.35f;
            bun.look.pos.set(bun.x, bun.y + lift);
            boolean gone = bun.y < -0.5f;
            while (!gone && shotHits(bun.x, bun.y + lift, 0.2f, Color.WHITE)) {
                bun.flash = 0.08f;
                if (--bun.hp <= 0) {
                    gone = true;
                    score += 20;
                    burst(bun.x, bun.y + lift, 12, Color.WHITE);
                    drop(bun.x, bun.y + lift, DROP_BUNNY);
                    game.sfx.play("enemy_die", 0.4f);
                }
            }
            if (!gone && touchesRocket(bun.x, bun.y + lift, 0.15f)) {
                hurt();
            }
            if (gone) bunnies.removeIndex(i);
        }
    }

    /** Astronauts jet across the screen, stopping now and then for a burst of three lasers. */
    private void updateSpacemen(float delta) {
        float k = busy();
        for (int i = spacemen.size - 1; i >= 0; i--) {
            Spaceman a = spacemen.get(i);
            a.flash = Math.max(0f, a.flash - delta);
            boolean firing = a.burst > 0;
            if (!firing) {
                a.x += a.vx * delta;
                a.y += MathUtils.sin(time * 1.5f + i) * 0.2f * delta;
                a.fireTimer -= delta;
                if (a.fireTimer <= 0 && a.x > 0.3f && a.x < 6.7f) {
                    a.burst = 3;
                    a.burstTimer = 0.25f; // (a moment to see it aim)
                }
            } else {
                a.burstTimer -= delta;
                if (a.burstTimer <= 0) {
                    orb(a.x, a.y, aimAt(a.x, a.y), 2.6f + 0.6f * k, 0.05f, LASER, Color.WHITE);
                    game.sfx.play("zap", 0.2f);
                    a.burstTimer = 0.13f;
                    if (--a.burst == 0) a.fireTimer = MathUtils.lerp(1.8f, 1.1f, k);
                }
            }
            a.look.pos.set(a.x, a.y);
            boolean gone = a.vx > 0 ? a.x > 7.5f : a.x < -0.5f;
            while (!gone && shotHits(a.x, a.y, 0.2f, LASER)) {
                a.flash = 0.08f;
                if (--a.hp <= 0) {
                    gone = true;
                    score += 40;
                    burst(a.x, a.y, 14, Color.WHITE);
                    drop(a.x, a.y, DROP_ASTRONAUT);
                    game.sfx.play("enemy_die", 0.5f);
                }
            }
            if (!gone && touchesRocket(a.x, a.y, 0.15f)) {
                hurt();
            }
            if (gone) spacemen.removeIndex(i);
        }
    }

    /**
     * The boss floats down to the top of the screen and sways along it, taking its three attacks in turn,
     * faster below half health. Beaten, it drops a heart and a star.
     */
    private void updateBoss(float delta) {
        BigOne bo = boss;
        if (bo == null) {
            return;
        }
        bo.t += delta;
        bo.flash = Math.max(0f, bo.flash - delta);
        float home = bo.kind.home;
        if (!bo.arrived) {
            bo.y = Math.max(home, bo.y - 1.5f * delta);
            bo.arrived = bo.y <= home;
        } else { // sways along the top, easing after where the sway is (so nothing ever snaps)
            bo.swayTime += delta;
            float sway = bo.kind == BossKind.HAWK ? 0.8f : bo.kind == BossKind.WITCH ? 0.7f : 0.5f;
            float ease = Math.min(1f, delta * 3f);
            bo.x += (3.5f + MathUtils.sin(bo.swayTime * sway) * 2.3f - bo.x) * ease;
            bo.y += (home + MathUtils.sin(bo.swayTime * 1.3f) * 0.12f - bo.y) * ease;
            boolean angry = bo.hp < bo.maxHp / 2f;
            float pace = angry ? 0.7f : 1f;
            bo.attackTimer -= delta;
            if (bo.attackTimer <= 0) {
                int which = bo.kind == BossKind.KING ? KING_ORDER[bo.attack % KING_ORDER.length] : bo.attack % 3;
                bo.attackTimer = attack(bo, which, angry) * pace;
                bo.attack++;
                game.sfx.play("enemy_shoot", 0.4f);
            }
            if (bo.streamTimer > 0) {
                bo.streamTimer -= delta;
                stream(bo, delta, angry);
            }
        }
        bo.look.pos.set(bo.x, bo.y);
        while (shotHits(bo.x, bo.y, bo.kind.reach, Color.WHITE)) {
            bo.flash = 0.06f;
            bo.hp--;
        }
        if (bo.hp <= 0) {
            score += 500 + 250 * bossesBeaten;
            bossesBeaten++;
            for (int j = 0; j < 4; j++) {
                burst(bo.x + MathUtils.random(-0.5f, 0.5f), bo.y + MathUtils.random(-0.5f, 0.5f), 16, j % 2 == 0 ? Color.WHITE : SHOT);
            }
            game.sfx.play("boom", 0.8f);
            for (int j = 0; j < 2; j++) {
                Bonus bonus = new Bonus();
                bonus.x = bo.x + (j == 0 ? -0.3f : 0.3f);
                bonus.y = bo.y;
                bonus.heart = j == 0;
                bonuses.add(bonus);
            }
            orbs.clear(); // (its bullets go with it)
            boss = null;
            bossTimer = BOSS_EVERY;
            return;
        }
        if (touchesRocket(bo.x, bo.y, bo.kind.reach * 0.85f)) {
            hurt();
        }
    }

    /** Starts attack {@code which} (0-2) of the boss; returns how long until the next one. */
    private float attack(BigOne bo, int which, boolean angry) {
        float aim = aimAt(bo.x, bo.y);
        switch (bo.kind) {
            case HAWK:
                if (which == 0) { // the door gun: a stream of tracers at you
                    bo.streamTimer = angry ? 1.8f : 1.3f;
                    return bo.streamTimer + 0.8f;
                }
                if (which == 1) { // a fan of rockets that burst into rings
                    int n = angry ? 5 : 3;
                    for (int j = 0; j < n; j++) {
                        Orb o = orb(bo.x, bo.y - 0.3f, aim + (j - (n - 1) / 2f) * 25f, 1.3f, 0.1f, ROCKET_SHELL, TRACER);
                        o.burstIn = 1.1f;
                        o.burstN = 10;
                    }
                    return 2f;
                }
                bo.streamAngle = 200f; // a sweep across the screen below
                bo.streamTimer = 1.2f;
                return 2.2f;
            case MESSENGER:
                if (which == 0) { // Egor's triangle: rows of round bullets, tip first
                    triangle(bo.x, bo.y - 0.2f, aim, BLADE);
                    if (angry) {
                        triangle(bo.x, bo.y - 0.2f, aim - 25f, BLADE);
                        triangle(bo.x, bo.y - 0.2f, aim + 25f, BLADE);
                    }
                    return 1.6f;
                }
                if (which == 1) { // a crescent slash of blades, sweeping forward as one
                    int n = angry ? 13 : 9;
                    for (int j = 0; j < n; j++) {
                        float off = (j - (n - 1) / 2f) * 10f;
                        float ox = bo.x + MathUtils.cosDeg(aim + off) * 0.5f;
                        float oy = bo.y + MathUtils.sinDeg(aim + off) * 0.5f;
                        orb(ox, oy, aim, 1.7f, 0.07f, BLADE, Color.WHITE);
                    }
                    return 1.5f;
                }
                for (int j = 0, n = angry ? 9 : 6; j < n; j++) { // rain: drops gather over you, then fall
                    float x = MathUtils.clamp(rocket.x + MathUtils.random(-1.8f, 1.8f), 0.2f, 6.8f);
                    Orb o = orb(x, MathUtils.random(4.1f, 4.9f), 270f, 2.6f, 0.06f, RAIN, Color.WHITE);
                    o.delay = 0.7f + j * 0.08f;
                }
                return 2f;
            case WITCH:
                if (which == 0) { // hex bolts that curve in from both sides
                    int n = angry ? 8 : 6;
                    for (int j = 0; j < n; j++) {
                        boolean left = j % 2 == 0;
                        Orb o = orb(bo.x, bo.y, aim + (left ? 35f : -35f) + (j / 2) * (left ? 8f : -8f), 1.7f, 0.07f, HEX, Color.WHITE);
                        o.curve = left ? -38f : 38f;
                    }
                    return 1.6f;
                }
                if (which == 1) { // jack-o'-lanterns that burst
                    int n = angry ? 3 : 2;
                    for (int j = 0; j < n; j++) {
                        Orb o = orb(bo.x, bo.y, aim + (j - (n - 1) / 2f) * 30f, 1.2f, 0.12f, LANTERN, Color.YELLOW);
                        o.burstIn = 1.3f;
                        o.burstN = 8;
                    }
                    return 2f;
                }
                { // a hex ring round you that closes in, with one way out
                    int n = 16;
                    float gap = MathUtils.random(360f);
                    for (int j = 1; j < n - 1; j++) {
                        float a = gap + j * 360f / n;
                        float x = rocket.x + MathUtils.cosDeg(a) * 1.5f;
                        float y = rocket.y + 0.1f + MathUtils.sinDeg(a) * 1.5f;
                        Orb o = orb(x, y, a + 180f, angry ? 1.2f : 0.9f, 0.07f, HEX, Color.WHITE);
                        o.delay = 0.8f;
                    }
                    return 2.6f;
                }
            case GRANDFLUFF:
                if (which == 0) { // comets leaving sparkles behind
                    int n = angry ? 5 : 3;
                    for (int j = 0; j < n; j++) {
                        Orb o = orb(bo.x, bo.y, aim + (j - (n - 1) / 2f) * 18f, 2.4f, 0.09f, STAR, Color.WHITE);
                        o.trailEvery = 0.14f;
                    }
                    return 1.8f;
                }
                if (which == 1) { // a constellation: stars appear, then each bursts
                    for (int j = 0, n = angry ? 7 : 5; j < n; j++) {
                        Orb o = orb(MathUtils.random(0.6f, 6.4f), MathUtils.random(2.2f, 3.8f), 0f, 0f, 0.08f, STAR, Color.WHITE);
                        o.delay = 0.9f + j * 0.1f;
                        o.burstIn = 0.01f;
                        o.burstN = 6;
                    }
                    return 2.4f;
                }
                bo.streamTimer = angry ? 2.2f : 1.6f; // a four-armed spiral
                return bo.streamTimer + 1f;
            default: // the Fluffy King
                if (which == 0) { // a ring with a gap round where the rocket is
                    int n = angry ? 22 : 18;
                    float gapAt = aim + MathUtils.random(-20f, 20f);
                    for (int j = 0; j < n; j++) {
                        float off = j * 360f / n;
                        if (off < 30f || off > 330f) continue;
                        orb(bo.x, bo.y, gapAt + off, 1.5f, 0.09f, FLUFF, FLUFF_CORE);
                    }
                    return 1.6f;
                }
                if (which == 1) { // an aimed fan, twice
                    int n = angry ? 7 : 5;
                    for (int j = 0; j < n; j++) {
                        orb(bo.x, bo.y - 0.3f, aim + (j - (n - 1) / 2f) * 12f, 2.2f, 0.08f, FLUFF, FLUFF_CORE);
                    }
                    return bo.attack % KING_ORDER.length == 1 ? 0.5f : 1.4f; // (a quick second fan)
                }
                bo.streamTimer = angry ? 2.2f : 1.6f; // a spiral for a moment
                return bo.streamTimer + 1f;
        }
    }

    /** The ongoing part of an attack: the Hawk's door gun or sweep, the spirals. */
    private void stream(BigOne bo, float delta, boolean angry) {
        boolean tick12 = ((int) (bo.t * 12f)) != ((int) ((bo.t - delta) * 12f));
        switch (bo.kind) {
            case HAWK:
                if (bo.streamAngle > 0) { // the sweep
                    bo.streamAngle += 120f * delta;
                    if (bo.streamAngle > 340f) {
                        bo.streamAngle = 0f;
                        bo.streamTimer = 0f;
                    } else if (((int) (bo.t * 15f)) != ((int) ((bo.t - delta) * 15f))) {
                        orb(bo.x, bo.y - 0.2f, bo.streamAngle, 2.4f, 0.06f, TRACER, Color.WHITE);
                    }
                } else if (tick12) {
                    orb(bo.x, bo.y - 0.2f, aimAt(bo.x, bo.y) + MathUtils.random(-5f, 5f), 3.2f, 0.055f, TRACER, Color.WHITE);
                    game.sfx.play("shoot", 0.08f);
                }
                break;
            case GRANDFLUFF:
                bo.streamAngle += (angry ? 200f : 150f) * delta;
                if (tick12) {
                    for (int j = 0; j < 4; j++) {
                        orb(bo.x, bo.y, bo.streamAngle + j * 90f, 1.5f, 0.07f, STAR, Color.WHITE);
                        orb(bo.x, bo.y, -bo.streamAngle + j * 90f + 45f, 1.1f, 0.06f, STAR, Color.WHITE);
                    }
                }
                break;
            case KING:
                bo.streamAngle += 260f * delta;
                if (tick12) {
                    for (int j = 0; j < 3; j++) {
                        orb(bo.x, bo.y, bo.streamAngle + j * 120f, 1.6f, 0.07f, FLUFF, FLUFF_CORE);
                    }
                }
                break;
            default:
                bo.streamTimer = 0f;
                break;
        }
    }

    /** The Messenger's triangle: four rows of round bullets flying as one block, tip first. */
    private void triangle(float x, float y, float aim, Color color) {
        float cos = MathUtils.cosDeg(aim);
        float sin = MathUtils.sinDeg(aim);
        for (int row = 0; row < 4; row++) {
            for (int j = 0; j <= row; j++) {
                float side = (j - row / 2f) * 0.16f;
                float back = row * 0.16f;
                orb(x - cos * back - sin * side, y - sin * back + cos * side, aim, 1.8f, 0.06f, color, Color.WHITE);
            }
        }
    }

    private void updateOrbs(float delta) {
        for (int i = orbs.size - 1; i >= 0; i--) {
            Orb o = orbs.get(i);
            if (o.delay > 0) {
                o.delay -= delta;
                continue;
            }
            if (o.curve != 0) {
                float turn = o.curve * delta;
                float vx = o.vx;
                o.vx = vx * MathUtils.cosDeg(turn) - o.vy * MathUtils.sinDeg(turn);
                o.vy = vx * MathUtils.sinDeg(turn) + o.vy * MathUtils.cosDeg(turn);
            }
            o.x += o.vx * delta;
            o.y += o.vy * delta;
            if (o.trailEvery > 0 && (o.trailTimer -= delta) <= 0) {
                o.trailTimer = o.trailEvery;
                Orb sparkle = orb(o.x, o.y, MathUtils.random(360f), 0.25f, 0.05f, o.color, o.core);
                sparkle.vy -= 0.45f;
            }
            if (o.burstIn > 0 && (o.burstIn -= delta) <= 0) {
                float turn = MathUtils.random(360f);
                for (int j = 0; j < o.burstN; j++) {
                    orb(o.x, o.y, turn + j * 360f / o.burstN, 1.5f, 0.06f, o.color, o.core);
                }
                orbs.removeIndex(i); // (the new ones went on the end)
                continue;
            }
            boolean gone = o.x < -0.3f || o.x > 7.3f || o.y < -0.3f || o.y > 5.3f;
            if (!gone && touchesRocket(o.x, o.y, o.r * 0.8f)) {
                gone = hurt();
            }
            if (gone) orbs.removeIndex(i);
        }
    }

    private void updateBonuses(float delta) {
        for (int i = bonuses.size - 1; i >= 0; i--) {
            Bonus p = bonuses.get(i);
            p.y -= 0.9f * delta;
            if (Vector2.dst2(rocket.x, rocket.y + 0.1f, p.x, p.y) < CATCH_RADIUS * CATCH_RADIUS) {
                // the present opens: mostly a heart when one's missing, else a blank
                boolean wantsHeart = hearts < HEARTS && (blanks >= MAX_BLANKS || MathUtils.random() < 0.65f);
                if (p.heart && wantsHeart) {
                    hearts++;
                    burst(p.x, p.y, 10, PRESENT);
                    game.sfx.play("heal", 0.6f);
                } else if (p.heart && blanks < MAX_BLANKS) {
                    blanks++;
                    burst(p.x, p.y, 10, Color.WHITE);
                    game.sfx.play("reload", 0.6f);
                } else if (!p.heart && shotLevel < MAX_SHOTS) {
                    shotLevel++;
                    game.sfx.play("pop", 0.6f);
                } else { // nothing more it could give: points instead
                    score += PRESENT_SCORE;
                    game.sfx.play("coin", 0.5f);
                }
                bonuses.removeIndex(i);
            } else if (p.y < -0.3f) {
                bonuses.removeIndex(i);
            }
        }
    }

    /** The blank key: Q or right click, X / RS on the pad. */
    private boolean blankPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.Q) || Gdx.input.isButtonJustPressed(Input.Buttons.RIGHT)
                || game.pad.justPressed(Button.X) || game.pad.justPressed(Button.RS);
    }

    /**
     * A blank, like in the game: every bullet on the screen is gone, and the enemies are put off their aim
     * for a moment (the boss too). It doesn't hurt anything.
     */
    private void useBlank() {
        if (blanks <= 0) {
            game.sfx.play("empty", 0.5f);
            return;
        }
        blanks--;
        blankTime = 0f;
        blankAt.set(rocket.x, rocket.y + WINDOW_Y);
        for (Orb o : orbs) burst(o.x, o.y, 2, o.color);
        orbs.clear();
        for (Ufo u : ufos) u.fireTimer = Math.max(u.fireTimer, 1f);
        for (Spaceman a : spacemen) {
            a.burst = 0;
            a.fireTimer = Math.max(a.fireTimer, 1f);
        }
        for (Bunny bun : bunnies) bun.hop = Math.min(bun.hop, 0f);
        if (boss != null) {
            boss.attackTimer = Math.max(boss.attackTimer, 1.2f);
            boss.streamTimer = 0f;
        }
        game.sfx.play("blank", 0.7f);
    }

    /** Maybe drops something where an enemy fell: mostly presents, a star while the guns can still grow. */
    private void drop(float x, float y, float chance) {
        if (MathUtils.random() >= chance) {
            return;
        }
        Bonus bonus = new Bonus();
        bonus.x = x;
        bonus.y = y;
        bonus.heart = shotLevel >= MAX_SHOTS || MathUtils.random() < 0.6f;
        bonuses.add(bonus);
    }

    /** A boss is coming: everything else goes up in a puff (no points), and the boss has the sky to itself. */
    private void clearForBoss() {
        for (Meteor m : meteors) burst(m.look.pos.x, m.look.pos.y, 8, ROCK);
        for (Ufo u : ufos) burst(u.x, u.y, 8, ORB);
        for (Bunny bun : bunnies) burst(bun.x, bun.y, 8, Color.WHITE);
        for (Spaceman a : spacemen) burst(a.x, a.y, 8, Color.WHITE);
        for (Orb o : orbs) burst(o.x, o.y, 2, o.color);
        meteors.clear();
        ufos.clear();
        bunnies.clear();
        spacemen.clear();
        orbs.clear();
        game.sfx.play("blank", 0.5f);
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

    /** Something of radius {@code r} at (x, y) touches the rocket's porthole, its hitbox. */
    private boolean touchesRocket(float x, float y, float r) {
        float rr = r + WINDOW_RADIUS;
        return Vector2.dst2(rocket.x, rocket.y + WINDOW_Y, x, y) < rr * rr;
    }

    /** Something hit the rocket: a heart gone (unless it's still blinking from the last hit); true if it counted. */
    private boolean hurt() {
        if (hurtTimer > 0 || over) {
            return false;
        }
        if (!Dev.flag("god")) {
            hearts--;
        }
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
            if (p.heart) { // a present with a heart inside, wobbling as it falls
                float tilt = MathUtils.sin(time * 5f + p.x) * 10f;
                float h = 0.12f * pulse;
                d.circle(b, p.x, p.y, h * 1.6f, PRESENT, 0.25f);
                d.rotRect(b, p.x, p.y, h * 2f + 0.03f, h * 2f + 0.03f, tilt, Draw.OUTLINE_COLOR, 1f);
                d.rotRect(b, p.x, p.y, h * 2f, h * 2f, tilt, PRESENT, 1f);
                d.rotRect(b, p.x, p.y, 0.045f, h * 2f, tilt, RIBBON, 1f);
                d.rotRect(b, p.x, p.y, h * 2f, 0.045f, tilt, RIBBON, 1f);
                float bx = p.x - MathUtils.sinDeg(tilt) * h;
                float by = p.y + MathUtils.cosDeg(tilt) * h;
                d.circle(b, bx - 0.04f, by + 0.03f, 0.035f, RIBBON); // the bow
                d.circle(b, bx + 0.04f, by + 0.03f, 0.035f, RIBBON);
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
        if (boss != null) {
            boss.look.drawPortrait(b, d, aimAt(boss.x, boss.y));
            if (boss.flash > 0) d.circle(b, boss.x, boss.y, boss.kind.reach * 0.9f, Color.WHITE, 0.25f);
        }
        for (Ufo u : ufos) {
            drawUfo(b, d, u);
        }
        for (Bunny bun : bunnies) {
            bun.look.drawPortrait(b, d, aimAt(bun.x, bun.y));
            if (bun.flash > 0) d.circle(b, bun.look.pos.x, bun.look.pos.y, 0.17f, Color.WHITE, 0.6f);
        }
        for (Spaceman a : spacemen) {
            a.look.drawPortrait(b, d, aimAt(a.x, a.y));
            if (a.flash > 0) d.circle(b, a.x, a.y, 0.17f, Color.WHITE, 0.6f);
        }
        for (Shot s : shots) {
            d.rotRect(b, s.x, s.y, 0.05f, 0.18f, -s.vx * 8f, SHOT, 1f);
        }
        for (Orb o : orbs) {
            if (o.delay > 0) { // gathering: a ring where it will be
                d.ring(b, o.x, o.y, o.r * 1.8f, o.color, 0.4f + 0.3f * MathUtils.sin(time * 20f));
                continue;
            }
            d.circle(b, o.x, o.y, o.r, o.color, 0.9f);
            d.circle(b, o.x, o.y, o.r * 0.47f, o.core);
        }
        for (Bit p : bits) {
            d.rect(b, p.x - p.size / 2f, p.y - p.size / 2f, p.size, p.size, p.color, Math.min(1f, p.life * 3f));
        }
        if (!over) {
            boolean blink = hurtTimer > 0 && ((int) (time * 20f)) % 2 == 0;
            RocketRideScreen.drawRocket(game, b, d, rocket.x, rocket.y, time, hurtTimer > HURT_TIME - 0.4f, blink);
            // the hitbox: the porthole glows, brighter with bullets about
            float glow = orbs.size > 0 ? 0.55f + 0.25f * MathUtils.sin(time * 8f) : 0.3f;
            d.circle(b, rocket.x, rocket.y + WINDOW_Y, WINDOW_RADIUS + 0.03f, WINDOW_GLOW, glow * 0.35f);
            d.ring(b, rocket.x, rocket.y + WINDOW_Y, WINDOW_RADIUS + 0.01f, Color.WHITE, glow);
        }
        if (blankTime >= 0) { // the blank's ring sweeping out
            float k = blankTime / BLANK_RING;
            d.ring(b, blankAt.x, blankAt.y, 0.3f + 6f * k, Color.WHITE, 0.8f * (1f - k));
            d.circle(b, blankAt.x, blankAt.y, 0.3f + 6f * k, Color.WHITE, 0.12f * (1f - k));
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
        for (int i = 0; i < blanks; i++) {
            d.texture(b, d.blankIcon, 0.15f + i * 0.22f, 4.1f, 0.18f, 0.18f);
        }
        for (int i = 0; i < HEARTS; i++) {
            Texture t = i < hearts ? d.heartFull : d.heartEmpty;
            d.texture(b, t, 6.85f - (HEARTS - i) * 0.3f, 4.62f, 0.26f, 0.26f * 8f / 9f);
        }
        for (int i = 0; i < MAX_SHOTS; i++) { // the stars caught so far: the guns' level
            float sx = 6.85f - (MAX_SHOTS - i) * 0.3f + 0.13f;
            for (int j = 0; j < 5; j++) {
                d.line(b, sx, 4.42f, 0.09f, 0.035f, 90f + j * 72f, i < shotLevel ? SHOT : DIM, i < shotLevel ? 1f : 0.4f);
            }
        }
        if (over) {
            float cx = 3.5f;
            d.rect(b, 0, 0, 7, 5, Color.BLACK, Math.min(0.55f, overTime));
            d.rect(b, cx - 2.04f, 1.36f, 4.08f, 2.38f, TEXT, 0.9f);
            d.rect(b, cx - 2f, 1.4f, 4f, 2.3f, PANEL, 0.95f);
            game.text(CREDIT, cx, 4.05f, 0.26f, BAR, Align.center);
            game.text("GAME OVER", cx, 3.5f, 0.6f, TEXT, Align.center);
            game.text("Score: " + score, cx, 3.0f, 0.32f, Color.WHITE, Align.center);
            game.text(newBest ? "NEW BEST!" : "Best: " + game.getRocketBest(), cx, 2.65f, 0.28f,
                    newBest ? TEXT : DIM, Align.center);
            if (overTime >= 1f) {
                overMenu.draw(game, overTime);
            }
            return;
        }
        if (boss != null) { // its health bar along the bottom (it takes up the top)
            float w = 3.2f;
            d.rect(b, 3.5f - w / 2f - 0.03f, 0.17f, w + 0.06f, 0.14f, Color.BLACK, 0.7f);
            d.rect(b, 3.5f - w / 2f, 0.2f, w * Math.max(0f, boss.hp / boss.maxHp), 0.08f, BAR);
            game.text(boss.kind.boss.title, 3.5f, 0.6f, 0.26f, BAR, Align.center);
        }
        if (time < 4f) {
            game.text("ROCKET GAME", 3.5f, 3.4f, 0.6f, TEXT, Align.center);
            game.text(CREDIT, 3.5f, 2.98f, 0.26f, BAR, Align.center);
            game.text(RocketRideScreen.fireHint(game), 3.5f, 2.55f, 0.28f, Color.WHITE, Align.center);
            game.text(game.pad.isActive() ? "X - BLANK" : "Q / RIGHT CLICK - BLANK", 3.5f, 2.2f, 0.24f, DIM, Align.center);
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
