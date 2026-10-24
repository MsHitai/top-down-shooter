package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.trush.game.enemy.BulletKin;
import com.trush.game.enemy.Biker;
import com.trush.game.enemy.Bulletmancer;
import com.trush.game.enemy.Sniper;
import com.trush.game.enemy.Dynamite;
import com.trush.game.enemy.Robber;
import com.trush.game.enemy.Shapeshifter;
import com.trush.game.enemy.Agent;
import com.trush.game.enemy.BowAngel;
import com.trush.game.enemy.SwordAngel;
import com.trush.game.enemy.Thundercloud;
import com.trush.game.enemy.Android;
import com.trush.game.enemy.Cube;
import com.trush.game.enemy.RoboCart;
import com.trush.game.enemy.Enemy;
import com.trush.game.enemy.Hydrant;
import com.trush.game.enemy.Rat;
import com.trush.game.enemy.ShellCop;
import com.trush.game.enemy.Boss;
import com.trush.game.enemy.ShotgunKin;
import com.trush.game.launcher.Dev;
import com.trush.game.objects.Item;
import com.trush.game.objects.FluffulVoice;
import com.trush.game.objects.Pickup;
import com.trush.game.objects.Weapon;
import com.trush.game.player.Player;
import lombok.Getter;

/**
 * Runs the wave loop: intro banner -> fight (enemies trickle in) -> cleared (rewards) -> next wave.
 * Every 5th wave is a boss wave. Difficulty slowly ramps up bullet speed and fire rate.
 */
@Getter
public class WaveDirector implements Director {

    public enum State { INTRO, FIGHT, CLEARED }

    private static final int KIN = 0;
    private static final int SHOTGUN = 1;
    private static final int MANCER = 2;
    private static final int BOSS = 3;
    private static final int HYDRANT = 4;
    private static final int RATS = 5;
    private static final int COP = 6;
    private static final int BIKER = 7;
    private static final int SNIPER = 8;
    private static final int DYNAMITE = 9;
    private static final int SHAPESHIFTER = 10;
    private static final int ROBBER = 11;
    private static final int ROBOCART = 12;
    private static final int ANDROID = 13;
    private static final int AGENT = 14;
    private static final int CUBE = 15;
    private static final int ANGEL = 16;
    private static final int THUNDERCLOUD = 17;

    private int wave;
    private State state = State.INTRO;
    private float timer;
    private float spawnCooldown;
    private final IntArray queue = new IntArray();
    private final Array<Weapon> gunRewards = new Array<>();
    /** Every boss wave also gives an item. */
    private final Array<Item> itemRewards = Item.shuffledPool();
    private int bossTier;
    /** The boss of the next boss wave. */
    private Boss boss = Boss.pickAny();

    public WaveDirector() {
        // rewards get better as the waves go on: random order within each tier, low tiers first
        gunRewards.addAll(Weapon.chestGuns());
        gunRewards.shuffle();
        gunRewards.sort((a, b) -> a.getTier().compareTo(b.getTier()));
        // dev shortcut: java -Dfluffy.startWave=5 ... jumps straight to the first boss
        wave = Math.max(0, Dev.number("startWave", 1) - 1);
        nextWave();
    }

    public boolean isBossWave() {
        return wave % 5 == 0;
    }

    private void nextWave() {
        wave++;
        state = State.INTRO;
        timer = 2.2f;
        queue.clear();
        if (isBossWave()) {
            boss = bossForWave(wave);
            queue.add(BOSS);
            for (int i = 0; i < bossTier * 2; i++) {
                queue.add(KIN);
            }
        } else {
            int budget = 2 + wave * 2;
            // the tough ones come in small numbers: one hydrant / biker / sniper at a time at first
            int hydrants = 0;
            int bikers = 0;
            int snipers = 0;
            int dynamites = 0;
            int storms = 0;
            int heavyCap = wave <= 10 ? 1 : 2;
            while (budget > 0) {
                int type;
                float r = MathUtils.random();
                // back-to-back chances (a type not unlocked yet passes its share on to the next one):
                // the town's enemies join from the middle waves on, the city's a little later
                // the skyscraper's crowd joins after the city's (wave 11 on), the clouds' from wave 16
                // (a roll of its own, so the shares of everything else stay as they were)
                if (wave >= 16 && budget >= 2 && MathUtils.random() < 0.12f) {
                    type = storms < heavyCap && MathUtils.randomBoolean(0.3f) ? THUNDERCLOUD : ANGEL;
                } else if (wave >= 11 && budget >= 2 && r < 0.1f) {
                    type = new int[]{ROBOCART, ANDROID, AGENT, CUBE}[MathUtils.random(3)];
                } else if (wave >= 8 && budget >= 2 && r < 0.15f) type = ROBBER;
                else if (wave >= 5 && budget >= 3 && r < 0.2f) type = SHAPESHIFTER;
                else if (wave >= 2 && budget >= 2 && dynamites < 2 && r < 0.24f) type = DYNAMITE;
                else if (wave >= 7 && budget >= 2 && snipers < heavyCap && r < 0.29f) type = SNIPER;
                else if (wave >= 6 && budget >= 2 && bikers < heavyCap && r < 0.36f) type = BIKER;
                else if (wave >= 4 && budget >= 2 && r < 0.44f) type = COP;
                else if (wave >= 3 && budget >= 2 && r < 0.52f) type = RATS;
                else if (wave >= 6 && budget >= 2 && hydrants < heavyCap && r < 0.57f) type = HYDRANT;
                else if (wave >= 3 && budget >= 3 && r < 0.65f) type = MANCER;
                else if (wave >= 2 && budget >= 2 && r < 0.82f) type = SHOTGUN;
                else type = KIN;
                if (type == SNIPER) snipers++;
                if (type == BIKER) bikers++;
                if (type == HYDRANT) hydrants++;
                if (type == DYNAMITE) dynamites++;
                if (type == THUNDERCLOUD) storms++;
                queue.add(type);
                budget -= type == MANCER || type == SHAPESHIFTER ? 3 : type == KIN ? 1 : 2;
            }
            queue.shuffle();
        }
    }

    private int maxActive() {
        return Math.min(8, 3 + wave / 3);
    }

    /**
     * Bosses climb the stages like a story run: wave 5 a village boss, 10 a town one, 15 a city one,
     * 20 the Fluffy King; after that anyone.
     */
    static Boss bossForWave(int wave) {
        switch (wave / 5) {
            case 1:
                return Boss.pickFor(Stage.VILLAGE);
            case 2:
                return Boss.pickFor(Stage.TOWN);
            case 3:
                return Boss.pickFor(Stage.CITY);
            case 4:
                return Boss.pickFor(Stage.SKYSCRAPER);
            case 5:
                return Boss.finalBoss();
            default:
                return Boss.pickAny();
        }
    }

    @Override
    public void onRoomEntered(Room room, GameWorld world) {
    }

    @Override
    public String music() {
        if (isBossWave() && state != State.CLEARED) {
            return boss.music();
        }
        // the room theme moves on through the stages as their enemies join in
        return wave <= 5 ? Stage.VILLAGE.music : wave <= 10 ? Stage.TOWN.music
                : wave <= 15 ? Stage.CITY.music : wave <= 20 ? Stage.SKYSCRAPER.music : Stage.CLOUDS.music;
    }

    @Override
    public boolean inFight(GameWorld world) {
        return state == State.FIGHT;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public void update(float delta, GameWorld world) {
        if (world.getPlayer().isDead()) {
            return;
        }
        world.setDifficulty(Math.min(1.45f, 1f + 0.045f * (wave - 1) + 0.1f * bossTier));
        world.setEnemyHealth(Math.min(1.75f, 1f + 0.04f * (wave - 1))); // +4% health a wave
        switch (state) {
            case INTRO:
                timer -= delta;
                if (timer <= 0) {
                    state = State.FIGHT;
                    if (isBossWave()) {
                        world.sfx().play("boss", 1f);
                        world.shake(0.5f);
                    }
                }
                break;
            case FIGHT:
                spawnCooldown -= delta;
                int active = world.getEnemies().size + world.getPendingEnemies().size;
                if (queue.size > 0 && spawnCooldown <= 0 && active < maxActive()) {
                    spawn(world, queue.pop());
                    spawnCooldown = 0.4f;
                    break; // the spawned enemy only counts as active from now on
                }
                if (queue.size == 0 && active == 0 && !world.bossDying()) {
                    onCleared(world);
                }
                break;
            case CLEARED:
                timer -= delta;
                if (timer <= 0) {
                    nextWave();
                    world.allowAmmoDrop(); // a new wave may drop an ammo box again
                }
                break;
        }
    }

    private void spawn(GameWorld world, int type) {
        if (type == BOSS) {
            world.spawnEnemy(boss.create(Arena.now.centerX, Arena.now.top - 1.0f, bossTier));
            return;
        }
        Vector2 p = world.findSpawnPoint(2.2f);
        Enemy e;
        switch (type) {
            case SHOTGUN:
                e = new ShotgunKin(p.x, p.y);
                break;
            case MANCER:
                e = new Bulletmancer(p.x, p.y);
                break;
            case HYDRANT:
                e = new Hydrant(p.x, p.y);
                break;
            case COP:
                e = new ShellCop(p.x, p.y);
                break;
            case BIKER:
                e = new Biker(p.x, p.y);
                break;
            case SNIPER:
                e = new Sniper(p.x, p.y);
                break;
            case DYNAMITE:
                e = new Dynamite(p.x, p.y);
                break;
            case SHAPESHIFTER:
                e = new Shapeshifter(p.x, p.y);
                break;
            case ROBBER:
                e = new Robber(p.x, p.y);
                break;
            case ROBOCART:
                e = new RoboCart(p.x, p.y);
                break;
            case ANDROID:
                e = new Android(p.x, p.y);
                break;
            case AGENT:
                e = new Agent(p.x, p.y);
                break;
            case ANGEL:
                e = MathUtils.randomBoolean() ? new SwordAngel(p.x, p.y) : new BowAngel(p.x, p.y);
                break;
            case THUNDERCLOUD:
                e = new Thundercloud(p.x, p.y);
                break;
            case CUBE:
                e = MathUtils.randomBoolean() ? Cube.mountain(p.x, p.y) : Cube.lead(p.x, p.y);
                break;
            case RATS: // a pack of three
                world.spawnEnemy(new Rat(p.x + 0.3f, p.y));
                world.spawnEnemy(new Rat(p.x - 0.3f, p.y));
                e = new Rat(p.x, p.y + 0.3f);
                break;
            default:
                e = new BulletKin(p.x, p.y);
                break;
        }
        world.spawnEnemy(e);
    }

    private void onCleared(GameWorld world) {
        state = State.CLEARED;
        timer = 3f;
        world.sfx().play("wave_clear", 0.8f);
        world.addScore(wave * 20);

        // scatter rewards a bit so leftovers from earlier waves don't stack on top of each other
        float cx = Arena.now.centerX + MathUtils.random(-1.2f, 1.2f);
        float cy = Arena.now.centerY + MathUtils.random(-0.6f, 0.6f);
        // (spawnPickup nudges these out of furniture)
        Player player = world.getPlayer();

        boolean giveGun = (wave == 2 || wave == 3 || isBossWave()) && gunRewards.size > 0;
        if (giveGun) {
            world.spawnPickup(new Pickup(Pickup.Type.GUN, cx, cy + 0.2f, gunRewards.removeIndex(0)));
        }
        if (isBossWave()) {
            // a boss down: fully healed, plus a heart container to grow on (endless has no floors to rest on)
            bossTier++;
            world.restockProps(); // and new furniture where the old was shot to pieces
            player.ensureBlanks(2);
            player.heal(player.getMaxHealth());
            // keep a flawless-kill message that's still showing
            world.toast(world.getToastTimer() > 0 && world.getToast() != null
                    ? world.getToast() + " - FULLY HEALED!" : "FULLY HEALED!");
            world.spawnPickup(new Pickup(Pickup.Type.HEART_CONTAINER, cx, cy - 0.3f));
            if (itemRewards.size > 0) {
                world.spawnPickup(new Pickup(itemRewards.pop(), cx - 0.6f, cy - 0.1f));
            }
            if (world.hasLimitedGunNeedingAmmo()) {
                world.spawnPickup(new Pickup(Pickup.Type.AMMO, cx, cy - 0.5f));
            }
            return;
        }

        world.fluffulSays(FluffulVoice.Event.ROOM_CLEAR);
        Array<Pickup.Type> options = new Array<>();
        if (player.getHealth() < player.getMaxHealth()) options.add(Pickup.Type.HEART);
        if (world.hasLimitedGunNeedingAmmo()) {
            options.add(Pickup.Type.AMMO);
            options.add(Pickup.Type.AMMO);
        }
        options.add(Pickup.Type.BLANK);
        Pickup.Type reward = options.random();
        world.spawnPickup(new Pickup(reward, cx + (giveGun ? 0.6f : 0f), cy - (giveGun ? 0.3f : 0f)));
    }

    @Override
    public String banner() {
        switch (state) {
            case INTRO:
                return isBossWave() ? "BOSS: " + boss.title : "WAVE " + wave;
            case CLEARED:
                return isBossWave() ? boss.defeated : "WAVE " + wave + " CLEARED!";
            default:
                return null;
        }
    }

    @Override
    public float bannerAlpha() {
        if (state == State.FIGHT) return 0;
        float total = state == State.INTRO ? 2.2f : 3f;
        float elapsed = total - timer;
        return MathUtils.clamp(Math.min(elapsed / 0.3f, timer / 0.5f), 0, 1);
    }
}
