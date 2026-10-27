package com.trush.game.objects;

import com.badlogic.gdx.math.MathUtils;

/**
 * Grandad's grumbling (player/Hero.GRANDAD, the owner's idea): once a boss has gone down and its death
 * show is over, the wise, grumpy, secretly sweet old man has something to say about it. GameScreen
 * shows the line in the same speech bubble as the Flufful's.
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
    };

    /** Waits out the boss's death show (slow motion, explosions) before saying anything. */
    private static final float DELAY = 2.2f;

    private String line;
    private float shown;
    private float wait = -1f;
    private int lastPick = -1;

    /** A boss is down: a line once its death show is over. */
    public void bossDown() {
        wait = DELAY;
    }

    public void update(float delta) {
        shown += delta;
        if (wait >= 0 && (wait -= delta) < 0) {
            int pick = MathUtils.random(BOSS_DOWN.length - 1);
            if (pick == lastPick) {
                pick = (pick + 1) % BOSS_DOWN.length; // never the same line twice in a row
            }
            lastPick = pick;
            line = BOSS_DOWN[pick];
            shown = 0;
        }
        if (line != null && shown > FluffulVoice.SHOW) {
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
