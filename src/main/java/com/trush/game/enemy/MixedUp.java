package com.trush.game.enemy;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Bullet;
import com.trush.game.world.GameWorld;

/** Borrows projectile patterns at release, after the shooter's own wind-up. */
final class MixedUp {
    static final float RECOVERY = 2.5f;

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

        Array<Bullet> bullets = world.getEnemyBullets();
        for (int i = first; i < bullets.size; i++) {
            Bullet bullet = bullets.get(i);
            bullet.alive = false;
            bullet.blanked = true;
        }
        // A donor always borrows the other pattern.
        boolean useFan = !(shooter instanceof ShotgunKin)
                && (shooter instanceof Shapeshifter || MathUtils.randomBoolean());
        if (useFan) {
            ShotgunKin.fireFan(shooter, world, shooter.aimAngle, false);
        } else {
            Shapeshifter.fireSquare(shooter, world, shooter.aimAngle, world.getDifficulty());
        }
        return true;
    }
}
