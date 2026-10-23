package com.trush.game.objects;

import com.badlogic.gdx.math.MathUtils;

import java.util.EnumMap;
import java.util.Map;

/**
 * The Flufful's chatter (the talking gun, like ETG's Gunther). GameWorld reports what happens; the gun
 * answers with a line that GameScreen shows in a speech bubble over the hero. It doesn't talk over
 * itself: after a line it stays quiet for a few seconds, except for the big moments.
 */
public class FluffulVoice {

    public enum Event {
        /** Picked up for the first time this run. */
        MEET,
        SWITCH_TO,
        SWITCH_AWAY,
        KILL,
        HURT,
        LOW_HEALTH,
        RELOAD,
        BOSS,
        BOSS_DOWN,
        ROOM_CLEAR,
        IDLE,
        /** Just unlocked: a voice from nowhere (it isn't in the player's hands yet). */
        UNLOCK
    }

    private static final Map<Event, String[]> LINES = new EnumMap<>(Event.class);

    static {
        LINES.put(Event.MEET, new String[]{"FLUFFUL! Not Gundul. FLUFFUL.", "Finally! Someone fluffy!",
                "Hi! I'm Flufful. Let's shoot!"});
        LINES.put(Event.SWITCH_TO, new String[]{"Me again? Yay!", "Missed me?", "The fluffy one is back!"});
        LINES.put(Event.SWITCH_AWAY, new String[]{"Fine. Use the boring gun.", "Hey! I was warmed up!",
                "Call me if it gets hairy."});
        LINES.put(Event.KILL, new String[]{"Fluff 'em up!", "Poof! Gone!", "That one was rude anyway.",
                "Too fluffy for you?", "Pew pew! I did that.", "Soft bullets, hard hits!", "Next!"});
        LINES.put(Event.HURT, new String[]{"Ouch! Watch the fur!", "Roll! ROLL!", "That'll leave a knot.",
                "Dodge, buddy, dodge!"});
        LINES.put(Event.LOW_HEALTH, new String[]{"Find a heart! Please!", "We need a heart. NOW.",
                "Careful! I'm too fluffy to fall."});
        LINES.put(Event.RELOAD, new String[]{"Fluffing up...", "Brushing my fur, one sec!", "Reloading the fluff!"});
        LINES.put(Event.BOSS, new String[]{"Oh no. It's BIG.", "Dibs on its hat!", "We got this. Probably."});
        LINES.put(Event.BOSS_DOWN, new String[]{"We're a fluffy team!", "Told you we'd win!", "Fluff wins again!"});
        LINES.put(Event.ROOM_CLEAR, new String[]{"Clean! Like my fur.", "Next room! Go go go!", "Nobody left? Aww."});
        LINES.put(Event.IDLE, new String[]{"Are we there yet?", "Is it snack time?", "Brush me later?",
                "Gundul who? I'm FLUFFUL.", "Fluffiest gun ever. Me.", "Hey. Hey. Shoot something."});
        LINES.put(Event.UNLOCK, new String[]{"Psst! Find me in a chest!"});
    }

    /** How long a line stays up, and the quiet after one starts. */
    public static final float SHOW = 2.6f;
    private static final float QUIET = 4f;

    private final Map<Event, Integer> lastPick = new EnumMap<>(Event.class);
    private String line;
    private float shown;
    private float quiet;
    private float idle = MathUtils.random(12f, 20f);

    /** Something happened: maybe say something about it. */
    public void say(Event e) {
        boolean big = e == Event.MEET || e == Event.BOSS || e == Event.BOSS_DOWN || e == Event.UNLOCK
                || e == Event.LOW_HEALTH;
        if (!big && quiet > 0) {
            return;
        }
        if (e == Event.KILL && MathUtils.random() > 0.35f) {
            return; // it's chatty, not that chatty
        }
        String[] options = LINES.get(e);
        int pick = MathUtils.random(options.length - 1);
        Integer last = lastPick.get(e);
        if (options.length > 1 && last != null && pick == last) {
            pick = (pick + 1) % options.length; // never the same line twice in a row
        }
        lastPick.put(e, pick);
        line = options[pick];
        shown = 0;
        quiet = QUIET;
        idle = MathUtils.random(14f, 24f);
    }

    /** {@code held}: the Flufful is the gun in hand (it only gets bored then). */
    public void update(float delta, boolean held) {
        shown += delta;
        quiet -= delta;
        if (held && (idle -= delta) <= 0) {
            say(Event.IDLE);
        }
        if (line != null && shown > SHOW) {
            line = null;
        }
    }

    /** The line to show right now, or null. */
    public String line() {
        return line;
    }

    /** Seconds the current line has been up (for popping the bubble in and out). */
    public float shown() {
        return shown;
    }
}
