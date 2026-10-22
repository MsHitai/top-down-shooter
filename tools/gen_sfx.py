#!/usr/bin/env python3
"""Generates the short procedural sound effects in src/main/resources/assets/sfx/.

Run from the project root:  python3 tools/gen_sfx.py
Needs numpy. Tweak the recipes below and re-run to change how things sound.
"""
import os
import wave

import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "sfx")
rng = np.random.default_rng(7)


def t(dur):
    return np.arange(int(SR * dur)) / SR


def env(dur, attack=0.002, decay=8.0):
    x = t(dur)
    e = np.exp(-x * decay)
    a = np.clip(x / attack, 0, 1)
    return e * a


def noise(dur):
    return rng.uniform(-1, 1, int(SR * dur))


def lowpass(sig, k):
    # simple one-pole lowpass, k in (0,1], smaller = darker
    out = np.empty_like(sig)
    acc = 0.0
    for i, s in enumerate(sig):
        acc += k * (s - acc)
        out[i] = acc
    return out


def sweep(f0, f1, dur, shape="sine"):
    x = t(dur)
    f = f0 * (f1 / f0) ** (x / dur)
    ph = 2 * np.pi * np.cumsum(f) / SR
    if shape == "square":
        return np.sign(np.sin(ph))
    if shape == "tri":
        return 2 / np.pi * np.arcsin(np.sin(ph))
    return np.sin(ph)


def write(name, sig, gain=0.8):
    sig = sig / (np.max(np.abs(sig)) + 1e-9) * gain
    fade = min(len(sig), int(SR * 0.004))
    sig[-fade:] *= np.linspace(1, 0, fade)
    data = (sig * 32767).astype(np.int16)
    os.makedirs(OUT, exist_ok=True)
    with wave.open(os.path.join(OUT, name + ".wav"), "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data.tobytes())
    print("wrote", name)


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[: len(p)] += p
    return out


# player pistol: snappy crack + low thump
d = 0.16
write("shoot", mix(noise(d) * env(d, decay=40) * 0.9, sweep(180, 60, d) * env(d, decay=25)), 0.7)

# shotgun: longer, darker blast
d = 0.35
write("shotgun", mix(lowpass(noise(d), 0.35) * env(d, decay=14), sweep(120, 40, d) * env(d, decay=12) * 0.9), 0.85)

# smg: tiny tick
d = 0.08
write("smg", mix(noise(d) * env(d, decay=70) * 0.7, sweep(400, 120, d, "square") * env(d, decay=60) * 0.3), 0.55)

# magnum: big crack with tail
d = 0.45
write("magnum", mix(noise(d) * env(d, decay=18), sweep(220, 35, d) * env(d, decay=9)), 0.9)

# enemy shot: soft "pew"
d = 0.12
write("enemy_shoot", sweep(1100, 380, d, "tri") * env(d, decay=22), 0.35)

# enemy hit: short click
d = 0.05
write("hit", mix(sweep(600, 250, d, "square") * env(d, decay=80) * 0.5, noise(d) * env(d, decay=90) * 0.4), 0.45)

# enemy death: crunchy pop going down
d = 0.35
write("enemy_die", mix(lowpass(noise(d), 0.5) * env(d, decay=12) * 0.7, sweep(500, 70, d, "square") * env(d, decay=10) * 0.4), 0.6)

# player hurt: thud + buzz
d = 0.3
write("hurt", mix(sweep(160, 50, d) * env(d, decay=10), sweep(300, 90, d, "square") * env(d, decay=14) * 0.4), 0.9)

# dodge roll: whoosh
d = 0.28
x = t(d)
whoosh = lowpass(noise(d), 0.12) * np.sin(np.pi * x / d) ** 2
write("dodge", whoosh, 0.5)

# blank: deep boom with shimmer
d = 0.9
write("blank", mix(sweep(90, 30, d) * env(d, decay=4), lowpass(noise(d), 0.2) * env(d, decay=6) * 0.8,
                   sweep(1800, 600, d, "tri") * env(d, decay=7) * 0.15), 0.95)

# reload: two clicks
d = 0.32
clk = noise(0.02) * env(0.02, decay=200)
r = np.zeros(int(SR * d))
r[: len(clk)] += clk
r[int(SR * 0.22): int(SR * 0.22) + len(clk)] += clk * 1.2
write("reload", r, 0.5)

# empty mag click
write("empty", noise(0.03) * env(0.03, decay=150), 0.4)

# enemy spawn: rising shimmer
d = 0.45
write("spawn", sweep(250, 900, d, "tri") * np.sin(np.pi * t(d) / d) * 0.6, 0.3)

# wave cleared: little arpeggio
notes = [523.25, 659.25, 783.99, 1046.5]
seq = np.zeros(int(SR * 0.7))
for i, f in enumerate(notes):
    d = 0.3
    tone = np.sin(2 * np.pi * f * t(d)) * env(d, decay=9)
    s = int(SR * 0.09 * i)
    seq[s: s + len(tone)] += tone
write("wave_clear", seq, 0.5)

# boss roar: growl
d = 1.0
x = t(d)
growl = sweep(110, 55, d, "square") * (0.6 + 0.4 * np.sin(2 * np.pi * 23 * x)) * np.sin(np.pi * x / d)
write("boss", mix(lowpass(growl, 0.15), lowpass(noise(d), 0.05) * np.sin(np.pi * x / d) * 0.5), 0.8)

# table flip: wooden thud + scrape
d = 0.3
write("flip", mix(sweep(140, 60, d) * env(d, decay=14), lowpass(noise(d), 0.25) * env(d, decay=20) * 0.6), 0.8)

# wood breaking: crunchy splinters
d = 0.4
crack = np.zeros(int(SR * d))
for k in range(6):
    s0 = int(SR * (0.02 + 0.045 * k))
    burst = noise(0.03) * env(0.03, decay=120) * (1 - k * 0.12)
    crack[s0:s0 + len(burst)] += burst
write("break", mix(crack, sweep(200, 70, d) * env(d, decay=12) * 0.5), 0.8)

# doors locking: heavy clank
d = 0.45
write("door", mix(sweep(90, 45, d, "square") * env(d, decay=10) * 0.6, noise(0.06) * env(0.06, decay=60),
                  sweep(1200, 900, d, "tri") * env(d, decay=16) * 0.2), 0.8)
