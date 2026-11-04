package com.trush.game.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.trush.game.enemy.Boss;
import com.trush.game.launcher.Dev;
import com.trush.game.player.Hero;
import com.trush.game.save.Flufficon;
import com.trush.game.save.Unlocks;

import java.time.LocalDate;

/**
 * A dream, dreamt in the kids' bedroom at home (the late game - the owner's idea): a short run of
 * {@link #FLOORS} random floors that ends. Each floor looks like a random stage, its enemies come from all
 * over (more of the world the deeper the dream), its boss is one you've beaten, and the last floor's is one
 * of the three dream bosses ({@link Boss#isDream}); beating it wakes you up. Every floor has its own twist
 * ({@link Rule}). Two more ways to dream: today's dream ({@link Kind#DAILY} - the same floors, bosses and
 * twists for everyone all day, from a seed of the date) and the toy box's boss rush ({@link Kind#BOSS_RUSH}:
 * a boss from every place you've been, one after another, a dream boss last).
 * Carried from floor to floor in {@code RunSave.dream} (public fields for its JSON); never saved to disk -
 * a dream is short, like ETG's boss rush.
 */
public class Dream {

    public static final int FLOORS = 5;

    public enum Kind { DREAM, DAILY, BOSS_RUSH }

    /** A dream floor's twist (shown on the floor's banner and the HUD). */
    public enum Rule {
        BOUNCY("BOUNCY BULLETS", "Your bullets bounce off walls"),
        GOLD_RUSH("GOLD RUSH", "Coins come double"),
        TOUGH_CROWD("TOUGH CROWD", "Everyone comes as their tougher kind"),
        FAST_FORWARD("FAST FORWARD", "Everything moves faster"),
        MOON_BOOTS("MOON BOOTS", "Your rolls go much further"),
        LIGHTS_OUT("LIGHTS OUT", "It's night everywhere"),
        GLASS_CANNON("GLASS CANNON", "Your hits do double - and so do theirs"),
        MIXED_UP("MIXED UP", "Someone in the family lends you their gun");

        public final String title;
        public final String description;

        Rule(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }

    /** The world speeds up this much under {@link Rule#FAST_FORWARD}. */
    public static final float FAST_FORWARD = 1.2f;
    /** Rolls go this much further in {@link Rule#MOON_BOOTS}. */
    public static final float MOON_BOOTS = 1.5f;

    public String kind = Kind.DREAM.name();
    /** Which floor of the dream this is (1 = the first). */
    public int floor = 1;
    /** Today's dream: the date's seed; 0 for the others. */
    public long seed;
    /** This floor's twists (Rule names). */
    public Array<String> rules = new Array<>();
    /** Bosses met so far (Boss names): a dream never repeats one. */
    public Array<String> bosses = new Array<>();
    /** Stages the floors so far looked like (a dream doesn't show the same place twice in a row). */
    public Array<String> looks = new Array<>();
    /** The boss rush: its bosses in order (Boss names). */
    public Array<String> rush = new Array<>();
    /** Seconds dreamt so far (for the fastest dream). */
    public float time;

    /** A new dream of this kind, on its first floor. */
    public static Dream start(Kind kind) {
        Dream d = new Dream();
        d.kind = kind.name();
        if (kind == Kind.DAILY) {
            d.seed = todaySeed();
        }
        if (kind == Kind.BOSS_RUSH) {
            d.rush = rushBosses();
        }
        d.floor = MathUtils.clamp(Dev.number("dreamFloor", 1), 1, d.floors()); // dev: start deeper in the dream
        return d;
    }

    /** Today, as a number: the daily dream's seed and the key its record is kept under. */
    public static long todaySeed() {
        LocalDate t = LocalDate.now();
        return t.getYear() * 10000L + t.getMonthValue() * 100L + t.getDayOfMonth();
    }

    public Kind kind() {
        try {
            return Kind.valueOf(kind);
        } catch (IllegalArgumentException e) {
            return Kind.DREAM;
        }
    }

    public boolean bossRush() {
        return kind() == Kind.BOSS_RUSH;
    }

    public int floors() {
        return bossRush() ? rush.size : FLOORS;
    }

    /** Beating this floor's boss wakes you up. */
    public boolean lastFloor() {
        return floor >= floors();
    }

    /** The dream goes on: the next floor (its twists are rolled when it starts). */
    public Dream next() {
        Dream d = copy();
        d.floor = floor + 1;
        d.rules.clear();
        return d;
    }

    /** The same kind of dream from the start (try again; the Dream Catcher's dream within a dream). */
    public Dream again() {
        Dream d = start(kind());
        if (kind() == Kind.DAILY) d.seed = seed; // (today's dream stays today's, past midnight too)
        return d;
    }

    private Dream copy() {
        Dream d = new Dream();
        d.kind = kind;
        d.floor = floor;
        d.seed = seed;
        d.rules.addAll(rules);
        d.bosses.addAll(bosses);
        d.looks.addAll(looks);
        d.rush.addAll(rush);
        d.time = time;
        return d;
    }

    public boolean has(Rule r) {
        return rules.contains(r.name(), false);
    }

    /** This floor's twists, in order. */
    public Array<Rule> rules() {
        Array<Rule> out = new Array<>();
        for (String r : rules) {
            try {
                out.add(Rule.valueOf(r));
            } catch (IllegalArgumentException ignored) { // a rule this version doesn't know
            }
        }
        return out;
    }

    // ------------------------------------------------------------ what a floor is

    /**
     * Rolls this floor's twists: one, two from the fourth floor on (the boss rush: one every other boss). Dev
     * builds can force one with -Dfluffy.dreamRule=glass_cannon.
     */
    void rollRules() {
        rules.clear();
        String forced = Dev.text("dreamRule");
        if (forced != null) {
            for (Rule r : Rule.values()) {
                if (r.name().equalsIgnoreCase(forced.trim())) rules.add(r.name());
            }
            if (rules.size > 0) return;
        }
        int n = bossRush() ? floor % 2 == 0 ? 1 : 0 : floor >= 4 ? 2 : 1;
        Array<Rule> pool = new Array<>(Rule.values());
        pool.shuffle();
        for (int i = 0; i < n && i < pool.size; i++) {
            rules.add(pool.get(i).name());
        }
    }

    /** The floor a stage belongs to on the story's road (its enemies' strength, its bosses' place in a dream). */
    static int homeFloor(Stage s) {
        switch (s) {
            case VILLAGE:
                return 1;
            case TOWN:
                return 2;
            case CITY:
                return 3;
            case SKYSCRAPER:
            case SLUMS:
                return 4;
            case CLOUDS:
            case UNDERCITY:
                return 5;
            default: // the sea, the Moon
                return 6;
        }
    }

    /** A boss's place on the road: its stage's floor (the Fluffy King and the dream bosses: the clouds'). */
    static int homeFloor(Boss b) {
        return b.stage == null ? 5 : homeFloor(b.stage);
    }

    /** Places you've found and so can dream of: the story's road, and the secret floors once you've been there. */
    static boolean found(Stage s) {
        switch (s) {
            case SEA:
                return Unlocks.foundSea;
            case MOON:
                return Unlocks.boardedRocket;
            case SLUMS:
                return Flufficon.hasSeen("AcidSprayer") || Flufficon.hasSeen("BarrelRoller");
            case UNDERCITY:
                return Flufficon.hasSeen("SewerGator") || Flufficon.hasSeen("AlleyDog");
            default:
                return true;
        }
    }

    /**
     * What this floor looks like: any place you've found (not the sea - its rooms are all water, and its
     * waterspout leads back up) other than the last floor's, and none twice in one dream if it can help it.
     */
    Stage pickLook() {
        Array<Stage> pool = new Array<>();
        for (Stage s : Stage.values()) {
            if (s != Stage.SEA && found(s) && !looks.contains(s.name(), false)) pool.add(s);
        }
        if (pool.isEmpty()) {
            for (Stage s : Stage.values()) {
                if (s != Stage.SEA && found(s) && (looks.isEmpty() || !looks.peek().equals(s.name()))) pool.add(s);
            }
        }
        Stage s = pool.random();
        looks.add(s.name());
        return s;
    }

    /** Where an enemy of this floor comes from: anywhere you've found, the deeper the dream the further afield. */
    Stage enemyStage() {
        Array<Stage> pool = new Array<>();
        for (Stage s : Stage.values()) {
            if (found(s) && homeFloor(s) <= floor + 1) pool.add(s);
        }
        return pool.random();
    }

    /** Bosses a dream's floors (not its last) can roll: the ones from stages, and the Fluffy King - not the special ones. */
    private static boolean dreamable(Boss b) {
        return !b.isDream() && b != Boss.SHADOW_KING && b != Boss.GRANDFLUFF && b.stage != Stage.SEA
                && (b.stage == null || found(b.stage));
    }

    /**
     * This floor's boss: one you've beaten that belongs about this deep (one floor either way), not met yet in
     * this dream - else any of those, beaten or not; on the last floor, a dream boss (one you haven't beaten
     * first). The boss rush goes down its list.
     */
    Boss pickBoss() {
        Boss forced = Dev.text("boss") != null ? Boss.byName(Dev.text("boss")) : null; // dev: -Dfluffy.boss=nightmare
        Boss b = forced != null ? forced : bossRush() ? rushBoss() : lastFloor() ? pickDreamBoss() : pickFloorBoss();
        bosses.add(b.name());
        return b;
    }

    private Boss pickFloorBoss() {
        for (int pass = 0; pass < 3; pass++) {
            Array<Boss> pool = new Array<>();
            for (Boss b : Boss.values()) {
                if (!dreamable(b) || bosses.contains(b.name(), false)) continue;
                if (pass < 2 && Math.abs(homeFloor(b) - floor) > 1) continue;
                if (pass < 1 && Flufficon.timesBeaten(b) == 0) continue;
                if (b == Boss.FLUFFY_KING && floor < 4) continue; // (he's saved for deep in the dream)
                pool.add(b);
            }
            if (pool.size > 0) return pool.random();
        }
        return Boss.HAYMAKER;
    }

    /** One of the three dream bosses: one you haven't beaten yet if there is one. */
    static Boss pickDreamBoss() {
        Array<Boss> fresh = new Array<>();
        Array<Boss> all = new Array<>();
        for (Boss b : Boss.values()) {
            if (!b.isDream()) continue;
            all.add(b);
            if (Flufficon.timesBeaten(b) == 0) fresh.add(b);
        }
        return (fresh.size > 0 ? fresh : all).random();
    }

    private Boss rushBoss() {
        String name = rush.get(MathUtils.clamp(floor - 1, 0, rush.size - 1));
        try {
            return Boss.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Boss.HAYMAKER;
        }
    }

    /**
     * The boss rush's bosses: one from every place on the road you've been to, in order - the village, the
     * town, the city, the skyscraper, the Slums and the Undercity once found, the Fluffy King (the Shadow King
     * when the King plays), the Fluffy Shark once the sea's been found, Grandfluff once he's been beaten - and a
     * dream boss to end it. Beaten ones first.
     */
    static Array<String> rushBosses() {
        Array<String> out = new Array<>();
        for (Stage s : new Stage[]{Stage.VILLAGE, Stage.TOWN, Stage.CITY, Stage.SKYSCRAPER, Stage.SLUMS, Stage.UNDERCITY}) {
            if (!found(s)) continue;
            Array<Boss> beaten = new Array<>();
            Array<Boss> all = new Array<>();
            for (Boss b : Boss.values()) {
                if (b.stage != s || b == Boss.SHADOW_KING) continue;
                all.add(b);
                if (Flufficon.timesBeaten(b) > 0) beaten.add(b);
            }
            if (all.size > 0) out.add((beaten.size > 0 ? beaten : all).random().name());
        }
        out.add((Hero.current == Hero.KING ? Boss.SHADOW_KING : Boss.FLUFFY_KING).name());
        if (found(Stage.SEA)) { // under the clouds: the shark, or once it's beaten any of the sea's
            Array<Boss> sea = new Array<>();
            for (Boss b : Boss.values()) {
                if (b.stage == Stage.SEA && (b == Boss.SHARK || Boss.sharkBeaten())) sea.add(b);
            }
            out.add(sea.random().name());
        }
        if (Flufficon.timesBeaten(Boss.GRANDFLUFF) > 0) {
            out.add(Boss.GRANDFLUFF.name());
        }
        out.add(pickDreamBoss().name());
        return out;
    }

    /** The stage a boss rush floor looks like: the boss's own (a dream boss: a random place you've found). */
    Stage rushLook(Boss b) {
        if (b == Boss.FLUFFY_KING || b == Boss.SHADOW_KING) return Stage.CLOUDS;
        if (b.stage != null) return b.stage;
        return pickLook();
    }

    /**
     * How hard this floor is, like a story floor's number: the dream's floor, a little further on for the twists
     * that make it easier; the boss rush: the boss's own place.
     */
    int level(Boss b) {
        return bossRush() ? homeFloor(b) : floor;
    }

    /** How much stronger a boss comes than at home (its tier): a village boss deep in a dream has grown. */
    int bossTier(Boss b) {
        return bossRush() ? 0 : Math.max(0, (floor - homeFloor(b) + 1) / 2);
    }

    /** "DREAM 3", "TODAY'S DREAM 3", "BOSS 4 / 9". */
    public String title() {
        switch (kind()) {
            case DAILY:
                return "TODAY'S DREAM " + floor;
            case BOSS_RUSH:
                return "BOSS " + floor + " / " + floors();
            default:
                return "DREAM " + floor;
        }
    }
}
