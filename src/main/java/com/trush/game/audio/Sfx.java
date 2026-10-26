package com.trush.game.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectLongMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.TimeUtils;

/**
 * Loads all one-shot sound effects once and plays them with a little pitch variation.
 * A per-sound rate limit keeps bullet-hell patterns (dozens of shots per frame) from
 * stacking into noise.
 */
public class Sfx implements Disposable {

    private final ObjectMap<String, Sound> sounds = new ObjectMap<>();
    private final ObjectLongMap<String> lastPlayed = new ObjectLongMap<>();
    public float volume = 0.7f;

    public Sfx() {
        for (String name : new String[]{"shoot", "shotgun", "smg", "magnum", "enemy_shoot", "hit", "enemy_die",
                "hurt", "dodge", "blank", "reload", "empty", "spawn", "wave_clear", "boss", "flip", "break", "door",
                "death", "body_fall", "chest_open", "chainsaw", "cackle", "pop", "zap", "buzz", "whistle", "boom", "heal", "swish", "twang", "flap",
                "coin", "kaching", "baa", "chomp", "hiss", "siren"}) {
            load(name, "assets/sfx/" + name + ".wav");
        }
        load("pickup", "assets/drop.mp3");
    }

    private void load(String key, String path) {
        sounds.put(key, Gdx.audio.newSound(Gdx.files.internal(path)));
    }

    public void play(String key) {
        play(key, 1f);
    }

    public void play(String key, float vol) {
        Sound s = sounds.get(key);
        if (s == null) {
            return;
        }
        long now = TimeUtils.millis();
        if (now - lastPlayed.get(key, 0) < 35) {
            return;
        }
        lastPlayed.put(key, now);
        s.play(vol * volume, MathUtils.random(0.93f, 1.07f), 0f);
    }

    @Override
    public void dispose() {
        for (Sound s : sounds.values()) {
            s.dispose();
        }
        sounds.clear();
    }
}
