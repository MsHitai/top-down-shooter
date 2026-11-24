package com.trush.game.gfx;

import com.trush.game.enemy.BedHeavy;
import com.trush.game.enemy.BedCritter;
import com.trush.game.enemy.Boss;
import com.trush.game.enemy.BulletKin;
import com.trush.game.enemy.Enemy;
import com.trush.game.enemy.ShotgunKin;
import com.trush.game.save.Flufficon;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** New catalogue entries should not silently lose the material detail pass. */
class EnemyMaterialCoverageTest {
    @Test
    void everyProceduralEnemyAndBossHasAMaterial() {
        for (Flufficon.Entry entry : Flufficon.ENEMIES) check(entry.create());
        for (Boss boss : Boss.values()) check(boss.create(0, 0, 0));
    }

    private void check(Enemy enemy) {
        if (enemy instanceof BulletKin || enemy instanceof ShotgunKin) return; // authored sheets
        if (enemy instanceof BedHeavy || enemy instanceof BedCritter) {
            String sheet = "/assets/sprites/cast_" + enemy.dexId().toLowerCase(Locale.ROOT) + ".png";
            assertNotNull(getClass().getResource(sheet), sheet);
            return;
        }
        String kind = enemy.getClass().getSimpleName();
        assertNotEquals(EnemyMaterial.PLAIN, EnemyMaterial.forKind(kind), kind + " needs an art material");
    }
}
