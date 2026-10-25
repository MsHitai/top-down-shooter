package com.trush.game.player;

import com.badlogic.gdx.utils.Array;
import com.trush.game.enemy.BulletKin;
import com.trush.game.enemy.Enemy;
import com.trush.game.enemy.Sludger;
import com.trush.game.objects.Item;
import com.trush.game.objects.Puddle;
import com.trush.game.world.Room;
import com.trush.game.world.Stage;
import com.trush.game.world.StageMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Fire and acid on the floor, burning / poisoned / frozen enemies, the Bike Helmet, and two treasure rooms. */
class FireAndAcidTest {

    private static final float STEP = 0.01f;

    private static Player player() {
        Player p = new Player(null);
        p.restore(6, 6, 2, 0, new Array<>(), 0);
        return p;
    }

    /** Runs the hazards for {@code seconds}; returns how many hits they dealt. */
    private static int stand(Player p, float seconds, boolean fire, boolean acid) {
        int hits = 0;
        for (float t = 0; t < seconds; t += STEP) {
            if (p.updateHazards(STEP, fire, acid)) hits++;
        }
        return hits;
    }

    @Test
    void steppingIntoFireBurnsAfterAMoment() {
        Player p = player();
        assertEquals(0, stand(p, Player.BURN_DELAY - 0.1f, true, false), "time to roll it out");
        assertTrue(p.isBurning());
        assertEquals(1, stand(p, 0.2f, false, false), "out of the flames, still on fire");
        p.douse();
        assertFalse(p.isBurning());
        assertEquals(0, stand(p, 3f, false, false));
    }

    @Test
    void fireBurnsOutOnItsOwn() {
        Player p = player();
        stand(p, 0.05f, true, false);
        stand(p, Player.BURN_TIME + 0.1f, false, false);
        assertFalse(p.isBurning());
    }

    @Test
    void acidBitesUnlessYouWearTheBoots() {
        Player p = player();
        assertEquals(0, stand(p, Player.ACID_DELAY - 0.05f, false, true));
        assertTrue(stand(p, 0.1f, false, true) >= 1);
        assertEquals(0, stand(p, 0.3f, false, false), "out of the acid, it stops");
        p.addPassive(Item.ACID_BOOTS);
        assertEquals(0, stand(p, 3f, false, true));
    }

    @Test
    void theHelmetTakesOneHitAndComesBackAfterTheFight() {
        Player p = player();
        assertFalse(p.consumeHelmet(), "no helmet, nothing to knock off");
        p.addPassive(Item.BIKE_HELMET);
        assertTrue(p.isHelmetOn());
        assertTrue(p.consumeHelmet());
        assertFalse(p.isHelmetOn());
        assertFalse(p.consumeHelmet());
        assertTrue(p.refitHelmet());
        assertFalse(p.refitHelmet(), "already on");
        assertTrue(p.isHelmetOn());
    }

    @Test
    void burningAndAcidWearEnemiesDown() {
        Enemy e = new BulletKin(0, 0);
        float dealt = 0;
        e.ignite(1f);
        e.poison(1f);
        for (float t = 0; t < 2f; t += STEP) {
            dealt += e.takeAilmentDamage(STEP);
        }
        assertEquals(Enemy.BURN_DPS + Enemy.POISON_DPS, dealt, 2f);
        assertFalse(e.isBurning());
        assertFalse(e.isPoisoned());
    }

    @Test
    void theSludgersSuitKeepsTheAcidOff() {
        Enemy s = new Sludger(0, 0);
        s.poison(3f);
        assertFalse(s.isPoisoned());
        s.ignite(1f);
        assertTrue(s.isBurning(), "fire still gets it");
    }

    @Test
    void frostFreezesRegularEnemiesButOnlyChillsBosses() {
        Enemy e = new BulletKin(0, 0);
        e.freeze(1f);
        assertTrue(e.isFrozen());
        e.ignite(1f);
        assertFalse(e.isFrozen(), "fire melts the ice");
        Enemy boss = new BulletKin(0, 0);
        boss.boss = true;
        boss.freeze(1f);
        assertFalse(boss.isFrozen());
    }

    @Test
    void puddlesAreFlatOvalsThatDryUp() {
        Puddle pd = new Puddle(Puddle.Kind.ACID, 5f, 5f, 0.5f, 3f, false);
        pd.update(0.5f); // spread out
        assertTrue(pd.covers(5.4f, 5f));
        assertFalse(pd.covers(5f, 5.45f), "less tall than wide");
        pd.update(3f);
        assertFalse(pd.alive());
    }

    @Test
    void theFirstFloorsHaveTwoTreasureRooms() {
        for (Stage stage : new Stage[]{Stage.VILLAGE, Stage.TOWN, Stage.CITY}) {
            for (int run = 0; run < 40; run++) {
                StageMap map = new StageMap(stage.roomCount(), stage);
                int treasures = 0;
                for (Room r : map.rooms) {
                    if (r.type != Room.Type.TREASURE) continue;
                    treasures++;
                    for (int d = 0; d < 4; d++) {
                        Room n = r.doors[d] ? map.neighbor(r, d) : null;
                        assertTrue(n == null || n.type != Room.Type.BOSS, "never next to the boss");
                    }
                }
                assertEquals(2, treasures, stage.title);
            }
        }
        StageMap tower = new StageMap(Stage.SKYSCRAPER.roomCount(), Stage.SKYSCRAPER);
        int treasures = 0;
        for (Room r : tower.rooms) {
            if (r.type == Room.Type.TREASURE) treasures++;
        }
        assertEquals(1, treasures, "a straight climb: one treasure floor");
    }
}
