package com.trush.game.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.trush.game.objects.Bullet;
import com.trush.game.player.Player;
import com.trush.game.world.Arena;
import com.trush.game.world.GameWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/** The Bulletmancer's cousins (spiral, bloom) and the clouds' cherub and dandelion puff. */
class NewCastersTest {

    private static final float STEP = 0.02f;

    private final Array<Bullet> bullets = new Array<>();
    private Player player;
    private GameWorld world;

    @BeforeEach
    void setUp() {
        Arena.now = new Arena(19, 12);
        player = new Player(null);
        player.restore(6, 6, 2, 0, new Array<>(), 0);
        player.setFeet(5, 3);
        world = mock(GameWorld.class, withSettings().mockMaker(MockMakers.SUBCLASS).defaultAnswer(RETURNS_DEEP_STUBS));
        when(world.getPlayer()).thenReturn(player);
        when(world.getDifficulty()).thenReturn(1f);
        when(world.onScreen(anyFloat(), anyFloat(), anyFloat())).thenReturn(true);
        doAnswer(inv -> {
            Bullet b = new Bullet(false, inv.getArgument(0), inv.getArgument(1), inv.getArgument(2),
                    inv.getArgument(3), 0.065f, 1f, Color.WHITE);
            b.life = 10f;
            bullets.add(b);
            return b;
        }).when(world).spawnEnemyBullet(anyFloat(), anyFloat(), anyFloat(), anyFloat());
    }

    private void run(Enemy e, float seconds) {
        for (float t = 0; t < seconds; t += STEP) {
            e.behave(STEP, world);
            for (Bullet b : bullets) b.update(STEP);
        }
    }

    @Test
    void theSpiralmancerPoursOutASpiral() {
        Spiralmancer m = new Spiralmancer(player.centerX() + 2.5f, player.hitboxY());
        m.speed = 0f;
        m.cast(world);
        run(m, Spiralmancer.STREAM_TIME + 0.1f);
        int volleys = (int) (Spiralmancer.STREAM_TIME / Spiralmancer.SHOT_GAP);
        assertTrue(bullets.size >= volleys * Spiralmancer.ARMS, "a stream of arms: " + bullets.size);
        // the arms turn: the first and last bullets of one arm point different ways
        assertTrue(Math.abs(bullets.first().vel.angleDeg(bullets.get(bullets.size - Spiralmancer.ARMS).vel)) > 30f);
    }

    @Test
    void theBloomancersSeedsBloomIntoFlowers() {
        Bloomancer m = new Bloomancer(player.centerX() + 2.5f, player.hitboxY());
        m.speed = 0f;
        m.cast(world);
        assertEquals(Bloomancer.SEEDS, bullets.size);
        run(m, Bloomancer.BLOOM_AT + 0.05f);
        int alive = 0;
        for (Bullet b : bullets) if (b.alive) alive++;
        assertEquals(Bloomancer.SEEDS * Bloomancer.PETALS, alive, "every seed is a flower now");
    }

    @Test
    void theCherubAlternatesArrowsAndAHeart() {
        Cherub c = new Cherub(player.centerX() + 2.5f, player.hitboxY());
        c.spawnTimer = 0f;
        c.speed = 0f;
        run(c, 3.0f);
        assertTrue(bullets.size >= 3, "three little arrows first");
        int arrows = bullets.size;
        run(c, 3.5f);
        assertTrue(bullets.size - arrows >= Cherub.HEART_BULLETS, "then a heart");
    }

    @Test
    void theDandelionsSeedsHangThenDartAtYou() {
        Dandelion p = new Dandelion(player.centerX() + 2f, player.hitboxY() + 1f);
        p.spawnTimer = 0f;
        p.speed = 0f;
        for (float t = 0; t < 4f && bullets.size == 0; t += STEP) run(p, STEP);
        assertEquals(Dandelion.SEEDS, bullets.size);
        run(p, Dandelion.DRIFT + 0.2f);
        for (Bullet b : bullets) assertTrue(b.vel.len() < 0.3f, "hanging in the air");
        player.setFeet(9, 6); // the hero moves before they dart
        run(p, Dandelion.HANG + 0.05f);
        for (Bullet b : bullets) {
            float miss = Math.abs(b.vel.crs(player.centerX() - b.pos.x, player.hitboxY() - b.pos.y)) / b.vel.len();
            assertTrue(miss < 0.05f, "every seed goes for where the hero is now, missed by " + miss);
        }
    }
}
