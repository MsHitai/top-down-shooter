package com.trush.game.enemy;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/** Borrows projectile patterns at release, after the shooter's own wind-up. */
final class MixedUp {
    static final float RECOVERY = 2.5f;

    private enum Pattern {
        SHOTGUN(ShotgunKin.class),
        SQUARE(Shapeshifter.class),
        SNEEZE(BedCritter.class);

        final Class<? extends Enemy> donor;

        Pattern(Class<? extends Enemy> donor) {
            this.donor = donor;
        }
    }

    private MixedUp() {
    }

    static boolean replaceVolley(Enemy shooter, GameWorld world, int first) {
        // These enemies release independent projectiles. Other enemies may keep a reference
        // to a shot for a remote detonation or use a telegraph tied to its exact trajectory.
        if (!(shooter instanceof BedCritter || shooter instanceof BedHeavy
                || shooter instanceof BulletKin || shooter instanceof BandanaKin || shooter instanceof VeteranKin
                || shooter instanceof ShotgunKin || shooter instanceof BlueShotgunKin || shooter instanceof SawnOffKin
                || shooter instanceof Shapeshifter || shooter instanceof Robber)) {
            return false;
        }

        Pattern[] patterns = Pattern.values();
        Pattern pattern;
        do {
            pattern = patterns[MathUtils.random(patterns.length - 1)];
        } while (pattern.donor.isInstance(shooter));

        Array<Bullet> bullets = world.getEnemyBullets();
        for (int i = first; i < bullets.size; i++) {
            Bullet bullet = bullets.get(i);
            bullet.alive = false;
            bullet.blanked = true;
        }
        switch (pattern) {
            case SHOTGUN -> ShotgunKin.fireFan(shooter, world, shooter.aimAngle, false);
            case SQUARE -> Shapeshifter.fireSquare(shooter, world, shooter.aimAngle, world.getDifficulty());
            case SNEEZE -> BedCritter.fireSneeze(shooter, world, shooter.aimAngle);
        }
        return true;
    }
}
