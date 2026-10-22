"""Small composition + rendering engine for Towards Dreams music (v2).

Write music as events (notes / CCs in beats), get automatic voice-led chord
voicings, humanized timing/velocity, optional swing, drum grids, and a
loop-seamless render through libfluidsynth + GeneralUser GS.
"""
import ctypes, itertools, random, struct, subprocess, wave
import numpy as np

SR = 44100

# ------------------------------------------------------------------ chords
_ROOT = {"C": 0, "D": 2, "E": 4, "F": 5, "G": 7, "A": 9, "B": 11}
_Q = {
    "": [4, 7], "m": [3, 7], "6": [4, 7, 9], "m6": [3, 7, 9], "69": [4, 7, 9, 14],
    "maj7": [4, 7, 11], "maj9": [4, 7, 11, 14], "maj7#11": [4, 7, 11, 18],
    "m7": [3, 7, 10], "m9": [3, 7, 10, 14], "m11": [3, 7, 10, 14, 17],
    "mmaj7": [3, 7, 11], "7": [4, 7, 10], "9": [4, 7, 10, 14], "13": [4, 10, 14, 21],
    "7b9": [4, 7, 10, 13], "7#9": [4, 7, 10, 15], "7sus": [5, 7, 10], "9sus": [5, 7, 10, 14],
    "sus": [5, 7], "sus2": [2, 7], "add9": [4, 7, 14], "madd9": [3, 7, 14],
    "dim7": [3, 6, 9], "m7b5": [3, 6, 10], "aug": [4, 8],
}
_PRIO = [3, 4, 10, 11, 9, 14, 2, 5, 17, 18, 13, 15, 21, 7, 6, 8]


def _pc(name):
    v = _ROOT[name[0]]
    for c in name[1:]:
        v += 1 if c == "#" else -1
    return v % 12


def parse_chord(sym):
    """'F#m7/E' -> (root_pc, bass_pc, [intervals])"""
    main, _, bass = sym.partition("/")
    n = 2 if len(main) > 1 and main[1] in "#b" else 1
    root, q = main[:n], main[n:]
    return _pc(root), (_pc(bass) if bass else _pc(root)), _Q[q]


def chord_pcs(sym, n=4):
    root, _, ivs = parse_chord(sym)
    ivs = sorted(ivs, key=lambda i: _PRIO.index(i) if i in _PRIO else 99)
    if len(ivs) >= 4 and 7 in ivs:
        ivs.remove(7)
    pcs = [(root + i) % 12 for i in ivs[:n]]
    if len(pcs) < n:
        pcs.append(root)
    return pcs[:n]


def bass_note(sym, lo=36, hi=47):
    _, b, _ = parse_chord(sym)
    p = lo + ((b - lo) % 12)
    return p if p <= hi else p - 12


class Voicer:
    """Chooses close voicings that move as little as possible."""

    def __init__(self, lo=52, hi=72, n=4, center=None):
        self.lo, self.hi, self.n = lo, hi, n
        self.center = center or (lo + hi) / 2 - 2
        self.prev = None

    def __call__(self, sym):
        pcs = chord_pcs(sym, self.n)
        cands = [[p for p in range(self.lo, self.hi + 1) if p % 12 == pc] for pc in pcs]
        best, bc = None, 1e9
        for combo in itertools.product(*cands):
            v = sorted(set(combo))
            if len(v) < len(pcs) or v[-1] - v[0] > 14:
                continue
            c = 0.25 * abs(sum(v) / len(v) - self.center)
            if self.prev and len(self.prev) == len(v):
                c += sum(abs(a - b) for a, b in zip(v, self.prev))
            if len(v) > 1 and v[-1] - v[-2] == 1:
                c += 3
            if len(v) > 1 and v[0] < 55 and v[1] - v[0] < 3:
                c += 2
            if c < bc:
                best, bc = v, c
        self.prev = best
        return best


def prog(text, bpb=4):
    """'Dmaj9 | F#m7 | A7sus A7' -> [(start, beats, sym)] (chords split bar evenly)."""
    out, t = [], 0.0
    for bar in text.split("|"):
        syms = bar.split()
        for s in syms:
            out.append((t, bpb / len(syms), s))
            t += bpb / len(syms)
    return out


# ------------------------------------------------------------------ song
class Song:
    def __init__(self, name, bpm, setup, seed=1, swing=None):
        """setup: {ch: (program, {cc: val})}; ch 9 = drums (program = kit)."""
        self.name, self.bpm, self.setup, self.swing = name, bpm, setup, swing
        self.rng = random.Random(seed)
        self.ev = []
        self.length = 0.0

    # --- primitives
    def _sw(self, b):
        if not self.swing:
            return b
        f = b % 1.0
        i = b - f
        s = self.swing
        return i + (f * 2 * s if f < 0.5 else s + (f - 0.5) * 2 * (1 - s))

    def note(self, ch, beat, dur, pitch, vel, jitter=0.015, swing=True):
        b0, b1 = (self._sw(beat), self._sw(beat + dur)) if swing else (beat, beat + dur)
        s = max(0.0, b0 + self.rng.gauss(0, jitter))
        e = max(s + 0.05, b1 - 0.02 + self.rng.gauss(0, jitter / 2))
        v = int(max(1, min(127, vel + self.rng.randint(-5, 5))))
        self.ev.append((s, 1, "on", ch, int(pitch), v))
        self.ev.append((e, 0, "off", ch, int(pitch), 0))
        self.length = max(self.length, beat + dur)

    def cc(self, ch, beat, num, val):
        self.ev.append((max(0.0, beat), 0, "cc", ch, num, int(max(0, min(127, val)))))

    def ramp(self, ch, b0, b1, num, v0, v1, steps=16, curve="lin"):
        for k in range(steps + 1):
            x = k / steps
            y = np.sin(np.pi * x) if curve == "swell" else x
            v = v0 + (v1 - v0) * y
            self.cc(ch, b0 + (b1 - b0) * x, num, v)

    def pedal(self, beat):
        """Piano sustain-pedal change at `beat` on every channel using it."""
        for ch in self.pedal_chs:
            self.cc(ch, beat + 0.01, 64, 0)
            self.cc(ch, beat + 0.07, 64, 110)

    pedal_chs = (0,)

    # --- writers
    def melody(self, ch, base, mel, vel, tr=0, legato=0.97, shape=True, jitter=0.015):
        for s, d, p in mel:
            v = vel
            if shape:
                v += 6 if d >= 2 else (-6 if d <= 0.5 else 0)
            self.note(ch, base + s, d * legato, p + tr, v, jitter)

    def pattern(self, ch, base, chords, pat, vel, voicer, period=4, bass_lo=36,
                bass_hi=47, roll=0.0, pedal=False):
        """pat: [(offset, token, dur, dvel)] repeated every `period` beats within
        each chord. tokens: 'B' bass, 'B5' fifth above bass, 'B8' octave,
        int index into the voicing (neg from top), 'all', 'top2'."""
        for start, beats, sym in chords:
            v = voicer(sym)
            bn = bass_note(sym, bass_lo, bass_hi)
            root, _, ivs = parse_chord(sym)
            fifth = bn + (7 if 7 in ivs or 6 not in ivs else 6)
            if pedal:
                self.pedal(base + start)
            off = 0.0
            while off < beats - 1e-6:
                for o, tok, dur, dv in pat:
                    if off + o >= beats - 1e-6:
                        continue
                    t = base + start + off + o
                    d = min(dur, beats - off - o + 0.5)
                    if tok == "B":
                        ps = [bn]
                    elif tok == "B5":
                        ps = [fifth]
                    elif tok == "B8":
                        ps = [bn + 12]
                    elif tok == "all":
                        ps = v
                    elif tok == "top2":
                        ps = v[-2:]
                    else:
                        ps = [v[tok % len(v)]]
                    for k, p in enumerate(ps):
                        self.note(ch, t + k * roll, d, p, vel + dv)
                off += period

    def pad(self, ch, base, chords, vel, voicer, swell=None, skip_low=0):
        for start, beats, sym in chords:
            v = voicer(sym)[skip_low:]
            for p in v:
                self.note(ch, base + start, beats - 0.04, p, vel, jitter=0.03, swing=False)
            if swell:
                self.ramp(ch, base + start, base + start + beats, 11, swell[0], swell[1],
                          curve="swell")

    def drums(self, base, bars, grid, vel=1.0, steps=16, bpb=4, jitter=0.008):
        """grid: {gm_note: 'x...o...'} ; x=100 o=72 g=40 (ghost), per bar."""
        vmap = {"x": 100, "X": 118, "o": 72, "g": 40}
        for bar in range(bars):
            for p, row in grid.items():
                for i, c in enumerate(row):
                    if c in vmap:
                        t = base + bar * bpb + i * bpb / steps
                        self.note(9, t, 0.25, p, vmap[c] * vel, jitter)

    # --- output
    def write_mid(self, path):
        tpb = 480

        def vl(n):
            o = [n & 0x7F]
            n >>= 7
            while n:
                o.append((n & 0x7F) | 0x80)
                n >>= 7
            return bytes(reversed(o))

        raw = [(0, b"\xff\x51\x03" + int(60e6 / self.bpm).to_bytes(3, "big"))]
        for ch, (prog_, ccs) in self.setup.items():
            raw.append((0, bytes([0xC0 | ch, prog_])))
            for k, v in ccs.items():
                raw.append((0, bytes([0xB0 | ch, k, v])))
        for b, o, k, ch, a, v in sorted(self.ev, key=lambda e: (e[0], e[1])):
            t = int(b * tpb)
            if k == "on":
                raw.append((t, bytes([0x90 | ch, a, v])))
            elif k == "off":
                raw.append((t, bytes([0x80 | ch, a, 0])))
            else:
                raw.append((t, bytes([0xB0 | ch, a, v])))
        trk, last = b"", 0
        for t, d in raw:
            trk += vl(t - last) + d
            last = t
        trk += b"\x00\xff\x2f\x00"
        with open(path, "wb") as f:
            f.write(b"MThd" + struct.pack(">IHHH", 6, 0, 1, tpb)
                    + b"MTrk" + struct.pack(">I", len(trk)) + trk)

    def render(self, sf2, out_base, total_beats, lufs, reverb=(0.78, 0.35, 1.0, 0.7)):
        fl = None
        for lib in ("libfluidsynth.so.3", "libfluidsynth.3.dylib", "libfluidsynth.dylib",
                    "/opt/homebrew/lib/libfluidsynth.dylib", "/usr/local/lib/libfluidsynth.dylib"):
            try:
                fl = ctypes.CDLL(lib)
                break
            except OSError:
                pass
        assert fl is not None, "FluidSynth library not found (macOS: brew install fluid-synth)"
        vp, cp, ci, cd = ctypes.c_void_p, ctypes.c_char_p, ctypes.c_int, ctypes.c_double
        fl.new_fluid_settings.restype = vp
        fl.fluid_settings_setnum.argtypes = [vp, cp, cd]
        fl.fluid_settings_setint.argtypes = [vp, cp, ci]
        fl.new_fluid_synth.restype = vp
        fl.new_fluid_synth.argtypes = [vp]
        fl.fluid_synth_sfload.argtypes = [vp, cp, ci]
        fl.fluid_synth_program_change.argtypes = [vp, ci, ci]
        fl.fluid_synth_noteon.argtypes = [vp, ci, ci, ci]
        fl.fluid_synth_cc.argtypes = [vp, ci, ci, ci]
        fl.fluid_synth_noteoff.argtypes = [vp, ci, ci]
        fl.fluid_synth_write_float.argtypes = [vp, ci, vp, ci, ci, vp, ci, ci]
        fl.delete_fluid_synth.argtypes = [vp]
        st = fl.new_fluid_settings()
        fl.fluid_settings_setnum(st, b"synth.sample-rate", float(SR))
        fl.fluid_settings_setnum(st, b"synth.gain", 0.3)
        fl.fluid_settings_setint(st, b"synth.polyphony", 1024)
        for k, v in zip((b"room-size", b"damp", b"width", b"level"), reverb):
            fl.fluid_settings_setnum(st, b"synth.reverb." + k, v)
        fl.fluid_settings_setnum(st, b"synth.chorus.level", 0.6)
        syn = fl.new_fluid_synth(st)
        assert fl.fluid_synth_sfload(syn, sf2.encode(), 1) >= 0
        for ch, (p, ccs) in self.setup.items():
            fl.fluid_synth_program_change(syn, ch, p)
            for k, v in ccs.items():
                fl.fluid_synth_cc(syn, ch, k, v)

        spb = 60.0 / self.bpm
        period = total_beats * spb
        evs = []
        for rep in range(2):  # keep 2nd pass -> its head holds the 1st pass's tail
            for b, o, k, ch, a, v in self.ev:
                evs.append((b * spb + rep * period, o, k, ch, a, v))
        evs.sort(key=lambda e: (e[0], e[1]))
        L, R = [], []
        cur = 0

        def run(n):
            nonlocal cur
            while n > 0:
                m = min(n, 4096)
                l = np.zeros(m, np.float32)
                r = np.zeros(m, np.float32)
                fl.fluid_synth_write_float(syn, m, l.ctypes.data, 0, 1, r.ctypes.data, 0, 1)
                L.append(l)
                R.append(r)
                n -= m
                cur += m

        for t, o, k, ch, a, v in evs:
            tgt = int(round(t * SR))
            if tgt > cur:
                run(tgt - cur)
            if k == "on":
                fl.fluid_synth_noteon(syn, ch, a, v)
            elif k == "off":
                fl.fluid_synth_noteoff(syn, ch, a)
            else:
                fl.fluid_synth_cc(syn, ch, a, v)
        end = int(round(2 * period * SR))
        if end > cur:
            run(end - cur)
        fl.delete_fluid_synth(syn)
        audio = np.stack([np.concatenate(L), np.concatenate(R)], 1)
        P = int(round(period * SR))
        loop = audio[P:2 * P]
        loop = loop / max(1e-6, np.abs(loop).max()) * 0.89
        wav = out_base + ".wav"
        with wave.open(wav, "wb") as w:
            w.setnchannels(2)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes((loop * 32767).astype("<i2").tobytes())
        # match target loudness, encode
        r = subprocess.run(["ffmpeg", "-hide_banner", "-i", wav, "-af",
                            "loudnorm=print_format=summary", "-f", "null", "-"],
                           capture_output=True, text=True).stderr
        cur_lufs = float([l for l in r.splitlines() if "Input Integrated" in l][0].split()[2])
        gain = lufs - cur_lufs
        encoders = subprocess.run(["ffmpeg", "-hide_banner", "-encoders"], capture_output=True, text=True).stdout
        # libvorbis if this ffmpeg has it (e.g. Linux builds), else ffmpeg's own experimental vorbis encoder
        codec = ["-c:a", "libvorbis", "-q:a", "5"] if "libvorbis" in encoders \
            else ["-c:a", "vorbis", "-strict", "-2", "-b:a", "160k"]
        subprocess.run(["ffmpeg", "-hide_banner", "-y", "-i", wav, "-af", f"volume={gain:.2f}dB",
                        *codec, out_base + ".ogg"],
                       check=True, capture_output=True)
        self.write_mid(out_base + ".mid")
        return period, cur_lufs, gain
