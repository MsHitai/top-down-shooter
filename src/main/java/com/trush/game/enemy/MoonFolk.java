package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.trush.game.gfx.Draw;
import com.trush.game.world.GameWorld;

/**
 * The Moon's own folk (the owner's line-up): red-eyed moon bunnies, living asteroids, astronauts,
 * aliens and flying saucers - only met up there - and the big ones ({@link #isBig}, one a room: the moonrock
 * golem and the cheese worm). The story director picks and spawns them through here. Deeper in come
 * their veterans ({@link #veteran}), a gold medal pinned on and patterns of their own (not the saucer).
 */
public final class MoonFolk {

    /** A pair of moon bunnies (three on harder floors). */
    public static final int BUNNIES = 0;
    public static final int ASTEROID = 1;
    public static final int ASTRONAUT = 2;
    public static final int ALIEN = 3;
    /** A flying saucer (Ufo): one a room at most. */
    public static final int UFO = 4;
    /** The big ones (like the sea's crab and octopus): one a room. */
    public static final int GOLEM = 5;
    public static final int WORM = 6;
    /** Added to a kind for its veteran: VETERAN + ALIEN is a veteran alien. */
    public static final int VETERAN = 7;
    /** How much of a room's budget a big one takes, and how deep a room has to be for one. */
    static final int BIG_COST = 4;
    public static final int BIG_DEPTH = 1;

    private static final Color MEDAL = Color.valueOf("f2c94c");
    private static final Color RIBBON = Color.valueOf("3a6fd8");

    private MoonFolk() {
    }

    /** One for a room's budget; big asteroids (they split in two) at most two at a time, one saucer, no big ones. */
    public static int pick(int budget, int asteroids, int ufos) {
        return pick(budget, asteroids, ufos, 1);
    }

    /** ... and a big one (golem or worm) while there's none in the room yet ({@code bigs}). */
    public static int pick(int budget, int asteroids, int ufos, int bigs) {
        if (budget >= BIG_COST && bigs < 1 && MathUtils.random() < 0.3f) {
            return MathUtils.randomBoolean() ? GOLEM : WORM;
        }
        float r = MathUtils.random();
        if (budget >= 3 && asteroids < 2 && r < 0.22f) return ASTEROID;
        if (budget >= 3 && ufos < 1 && r < 0.36f) return UFO;
        if (budget >= 2 && r < 0.48f) return ASTRONAUT;
        if (budget >= 2 && r < 0.72f) return ALIEN;
        return BUNNIES;
    }

    /** The veteran of a kind (a saucer and the big ones have none: they stay themselves). */
    public static int veteran(int kind) {
        int base = kind % VETERAN;
        return base == UFO || isBig(base) ? base : base + VETERAN;
    }

    public static boolean isBig(int kind) {
        return kind == GOLEM || kind == WORM;
    }

    public static boolean isVeteran(int kind) {
        return kind >= VETERAN;
    }

    public static int cost(int kind) {
        if (isBig(kind)) return BIG_COST;
        return kind % VETERAN == ASTEROID || kind == UFO ? 3 : 2;
    }

    public static void spawn(GameWorld world, int kind, float x, float y) {
        boolean veteran = isVeteran(kind);
        switch (kind % VETERAN) {
            case ASTEROID:
                world.spawnEnemy(veteran ? LivingAsteroid.veteran(x, y) : new LivingAsteroid(x, y));
                break;
            case ASTRONAUT:
                world.spawnEnemy(veteran ? Astronaut.veteran(x, y) : new Astronaut(x, y));
                break;
            case ALIEN:
                world.spawnEnemy(veteran ? Alien.veteran(x, y) : new Alien(x, y));
                break;
            case UFO:
                world.spawnEnemy(new Ufo(x, y));
                break;
            case GOLEM:
                world.spawnEnemy(new MoonGolem(x, y));
                break;
            case WORM:
                world.spawnEnemy(new CheeseWorm(x, y));
                break;
            default:
                int n = world.getDifficulty() > 1.6f ? 3 : 2;
                for (int i = 0; i < n; i++) {
                    float bx = x + MathUtils.random(-0.35f, 0.35f);
                    float by = y + MathUtils.random(-0.35f, 0.35f);
                    world.spawnEnemy(veteran ? MoonBunny.veteran(bx, by) : new MoonBunny(bx, by));
                }
                break;
        }
    }

    /** The veterans' medal: a blue ribbon and a gold star-shaped disc. */
    static void drawMedal(SpriteBatch b, Draw d, float x, float y, float alpha) {
        d.rect(b, x - 0.018f, y + 0.005f, 0.036f, 0.04f, RIBBON, alpha);
        d.circle(b, x, y, 0.026f, Draw.OUTLINE_COLOR, alpha);
        d.circle(b, x, y, 0.02f, MEDAL, alpha);
    }
}
