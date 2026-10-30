package com.trush.game.objects;

import com.badlogic.gdx.utils.IntArray;

/**
 * Grandad's grumbling (player/Hero.GRANDAD, the owner's idea): once a boss has gone down, its death
 * show is over and the camera is back on him, the wise, grumpy, secretly sweet old man has something to
 * say about it. GameScreen shows the line in the same speech bubble as the Flufful's.
 */
public class GrandadVoice {

    private static final String[] BOSS_DOWN = {
            "In my day we ate these for dinner.",
            "Hmph. They were bigger back in '58.",
            "That's it? I've had worse colds.",
            "My knees hurt more than that did.",
            "Not bad for an old man, eh?",
            "Kids these days. No stamina.",
            "Now where did I put my glasses?",
            "I've seen scarier pigeons.",
            "Back to my crossword, then.",
            "Your granny hits harder than that.",
            "I'd have done it faster, but I stopped for tea.",
            "Write that one down for your grandchildren.",
            "Fell over quicker than my old tractor.",
            "Don't tell your mother I did that.",
            "Pfft. I've had stronger sneezes.",
            "Someone fetch my slippers. We're done here.",
            "That one owes me a new pair of trousers.",
            "Kids, take notes. That's how it's done.",
            "Lucky for him I left my good stick at home.",
            "Back in my day the monsters had manners.",
            "I've wrestled scarier things at the bingo hall.",
            "Well, that put a crease in my cardigan.",
            "Good. Now who's making lunch?",
            "Hmph. I didn't even need my reading glasses.",
    };

    /** Once the camera has glided back to him (GameScreen's CAMERA_GLIDE), a breath and then the line. */
    private static final float DELAY = 1.2f;

    private String line;
    private float shown;
    private float wait = -1f;
    /**
     * Lines not said yet, shuffled: every one comes up before any repeats, so a whole run of bosses never
     * hears the same line twice (static: a new floor is a new GameWorld, but it's the same grandad).
     */
    private static final IntArray bag = new IntArray();

    /** A boss is down: a line once its death show is over and the camera is back on him. */
    public void bossDown() {
        wait = DELAY;
    }

    /** @param watchingBoss the camera is still on the fallen boss (GameWorld#bossDeathSpot): hold the line */
    public void update(float delta, boolean watchingBoss) {
        shown += delta;
        if (wait >= 0 && watchingBoss) {
            wait = DELAY;
        } else if (wait >= 0 && (wait -= delta) < 0) {
            line = BOSS_DOWN[nextPick()];
            shown = 0;
        }
        if (line != null && shown > FluffulVoice.SHOW) {
            line = null;
        }
    }

    private static int nextPick() {
        if (bag.size == 0) {
            for (int i = 0; i < BOSS_DOWN.length; i++) bag.add(i);
            bag.shuffle();
        }
        return bag.pop();
    }

    /** Tests: start from a full bag. */
    static void refillBag() {
        bag.clear();
    }

    static int lineCount() {
        return BOSS_DOWN.length;
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
