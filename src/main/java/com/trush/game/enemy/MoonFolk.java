package com.trush.game.enemy;

import com.badlogic.gdx.math.MathUtils;
import com.trush.game.world.GameWorld;

/**
 * The Moon's own folk (the owner's line-up): red-eyed moon bunnies, living asteroids, astronauts and
 * aliens - only met up there. The story director picks and spawns them through here.
 */
public final class MoonFolk {

    /** A pair of moon bunnies (three on harder floors). */
    public static final int BUNNIES = 0;
    public static final int ASTEROID = 1;
    public static final int ASTRONAUT = 2;
    public static final int ALIEN = 3;

    private MoonFolk() {
    }

    /** One for a room's budget; big asteroids (they split in two) at most two at a time. */
    public static int pick(int budget, int asteroids) {
        float r = MathUtils.random();
        if (budget >= 3 && asteroids < 2 && r < 0.22f) return ASTEROID;
        if (budget >= 2 && r < 0.48f) return ASTRONAUT;
        if (budget >= 2 && r < 0.72f) return ALIEN;
        return BUNNIES;
    }

    public static int cost(int kind) {
        return kind == ASTEROID ? 3 : 2;
    }

    public static void spawn(GameWorld world, int kind, float x, float y) {
        switch (kind) {
            case ASTEROID:
                world.spawnEnemy(new LivingAsteroid(x, y));
                break;
            case ASTRONAUT:
                world.spawnEnemy(new Astronaut(x, y));
                break;
            case ALIEN:
                world.spawnEnemy(new Alien(x, y));
                break;
            default:
                int n = world.getDifficulty() > 1.6f ? 3 : 2;
                for (int i = 0; i < n; i++) {
                    world.spawnEnemy(new MoonBunny(x + MathUtils.random(-0.35f, 0.35f), y + MathUtils.random(-0.35f, 0.35f)));
                }
                break;
        }
    }
}
