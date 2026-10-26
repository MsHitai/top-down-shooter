#!/usr/bin/env python3
"""Enter the Fluffy soundtrack - gritty, minor-key "gun-rock" in the spirit of
Enter the Gungeon (original compositions, not covers).

usage: python3 tools/music/tracks.py <GeneralUser-GS.sf2> src/main/resources/assets/music [track ...]

Built on musiclib.py (borrowed from the Towards Dreams project). Every track
renders as a seamless loop (.ogg); delete the leftover .wav/.mid files after.
"""
import os
import sys

from musiclib import Song, Voicer, prog, bass_note

# GM drum notes
KICK, SNARE, CLAP, HAT, OHAT, CRASH, RIDE = 36, 38, 39, 42, 46, 49, 51
TOM_HI, TOM_MID, TOM_LO = 50, 47, 45


def power_chords(s, ch, base, chords, rhythm, vel, lo=40, hi=51, octave=True):
    """Distorted-guitar power chords. rhythm: 16-char string per bar,
    'x' = accented chug, 'o' = palm-muted chug, '-' = let ring (half note)."""
    for start, beats, sym in chords:
        root = bass_note(sym, lo, hi)
        steps = int(beats * 4)
        for i in range(steps):
            c = rhythm[i % len(rhythm)]
            if c == ".":
                continue
            t = base + start + i * 0.25
            dur = {"x": 0.24, "o": 0.18, "-": 2.0}[c]
            v = vel + (8 if c == "x" else -10 if c == "o" else 4)
            notes = [root, root + 7] + ([root + 12] if octave and c != "o" else [])
            for p in notes:
                s.note(ch, t, dur, p, v, jitter=0.006)


def riff(s, ch, base, bars, line, vel, tr=0):
    """line: [(step16, len16, pitch)] repeated each bar."""
    for bar in range(bars):
        for st, ln, p in line:
            s.note(ch, base + bar * 4 + st / 4, ln / 4, p + tr, vel, jitter=0.006)


# ======================================================================= MENU
def menu():
    """'The Gun Altar' - main menu. D minor, 92 bpm. Harpsichord ostinato,
    choir, a lonely horn theme, then strings and a half-time beat join."""
    s = Song("menu", 92, {
        0: (6, {7: 78, 10: 50, 91: 60}),     # harpsichord
        1: (33, {7: 96, 10: 64, 91: 20}),    # fingered bass
        2: (52, {7: 60, 10: 70, 91: 110}),   # choir aahs
        3: (48, {7: 70, 10: 80, 91: 90}),    # strings
        4: (60, {7: 92, 10: 58, 91: 70}),    # french horn
        9: (16, {7: 92, 91: 40}),            # power kit
    }, seed=3)
    vh, vc, vs = Voicer(57, 76), Voicer(55, 72), Voicer(60, 79)
    INTRO = prog("Dm | Bb | Gm | A7")
    A = prog("Dm | Bb | Gm | A7 | Dm | Bb | Gm | A7")
    B = prog("Dm | F | C | Gm | Bb | C | A7sus | A7")
    ARP = [(0, 0, .5, 0), (.5, 1, .5, -6), (1, 2, .5, -4), (1.5, 1, .5, -8),
           (2, 3, .5, -2), (2.5, 2, .5, -8), (3, 1, .5, -4), (3.5, 2, .5, -8)]
    BASS = [(0, "B", 1, 0), (1.5, "B", .5, -10), (2, "B8", .5, -6), (2.5, "B", 1, -4), (3.5, "B5", .5, -8)]
    HORN_A = [(0, 1.5, 69), (1.5, .5, 70), (2, 1, 69), (3, 1, 65), (4, 3, 62), (7, 1, 65),
              (8, 1.5, 67), (9.5, .5, 69), (10, 1, 70), (11, 1, 74), (12, 3, 73), (15, 1, 69),
              (16, 1.5, 74), (17.5, .5, 72), (18, 1, 70), (19, 1, 69), (20, 2, 70), (22, 1, 69),
              (23, 1, 67), (24, 1.5, 65), (25.5, .5, 67), (26, 1, 69), (27, 1, 70), (28, 2, 69), (30, 2, 61)]
    STR_B = [(0, 3, 74), (3, 1, 77), (4, 4, 72), (8, 3, 72), (11, 1, 74), (12, 4, 70),
             (16, 2, 74), (18, 2, 77), (20, 4, 79), (24, 4, 76), (28, 2, 73), (30, 2, 76)]
    HALF = {KICK: "x.....x...x.....", SNARE: "........x.......", HAT: "x.o.x.o.x.o.x.o."}
    t = 0
    s.pattern(0, t, INTRO, ARP, 74, vh)
    s.pad(2, t, INTRO, 64, vc, swell=(80, 120))
    s.pattern(1, t, INTRO, [(0, "B", 3.5, 0)], 76, Voicer(40, 55))
    s.melody(4, t, [(8, 3, 69), (12, 3, 73)], 64, shape=False)
    t += 16
    s.pattern(0, t, A, ARP, 54, vh)
    s.pattern(1, t, A, BASS, 82, Voicer(40, 55))
    s.pad(2, t, A, 46, vc)
    s.melody(4, t, HORN_A, 82)
    s.drums(t, 8, HALF, 0.8)
    s.note(9, t, 1, CRASH, 90)
    t += 32
    s.pattern(0, t, B, ARP, 56, vh)
    s.pattern(1, t, B, BASS, 86, Voicer(40, 55))
    s.pad(2, t, B, 52, vc, swell=(70, 115))
    s.melody(3, t, STR_B, 78)
    s.drums(t, 7, {**HALF, RIDE: "x...x...x...x..."}, 0.9)
    s.drums(t + 28, 1, {TOM_HI: "x.x.............", TOM_MID: "....x.x.........", TOM_LO: "........x.x.x.x."}, 0.9)
    s.note(9, t, 1, CRASH, 100)
    t += 32
    s.pattern(0, t, A, ARP, 54, vh)
    s.pattern(1, t, A, BASS, 82, Voicer(40, 55))
    s.pad(2, t, A, 50, vc)
    s.melody(4, t, HORN_A, 86)
    s.melody(3, t, [(p[0], p[1], p[2] - 12) for p in HORN_A], 52)
    s.drums(t, 8, HALF, 0.85)
    s.note(9, t, 1, CRASH, 100)
    t += 32
    return s, t, -18


# ===================================================================== VILLAGE
def village():
    """'Skirmish at the Well' - stage 1 (the village). E minor, 132 bpm.
    Chugging guitar + accordion folk melody, square-lead B section, breakdown."""
    s = Song("village", 132, {
        0: (29, {7: 74, 10: 44, 91: 25}),    # overdriven guitar
        1: (34, {7: 100, 10: 64, 91: 10}),   # picked bass
        2: (21, {7: 92, 10: 76, 91: 50}),    # accordion lead
        3: (105, {7: 62, 10: 90, 91: 40}),   # banjo chops
        4: (61, {7: 78, 10: 56, 91: 50}),    # brass section
        5: (80, {7: 66, 10: 70, 91: 50}),    # square lead
        9: (16, {7: 100, 91: 30}),
    }, seed=7)
    vb, vbr = Voicer(55, 72, n=3), Voicer(58, 76)
    A = prog("Em | C | D | B7 | Em | C | D | B7")
    B = prog("Am | Em | C | D | Am | Em | B7 | B7")
    BRK = prog("Em | Em | C | B7")
    BASS = [(0, "B", .5, 4), (.5, "B", .5, -10), (1, "B8", .5, -4), (1.5, "B", .5, -10),
            (2, "B5", .5, 0), (2.5, "B", .5, -10), (3, "B8", .5, -4), (3.5, "B5", .5, -8)]
    CHOP = [(.5, "all", .2, 0), (1.5, "all", .2, -6), (2.5, "all", .2, 0), (3.5, "all", .2, -6)]
    ROCK = {KICK: "x.....x.x.....x.", SNARE: "....x.......x...", HAT: "x.x.x.x.x.x.x.x."}
    ACC_A = [(0, .5, 71), (.5, .5, 74), (1, 1, 76), (2, .5, 74), (2.5, .5, 71), (3, 1, 67),
             (4, 1.5, 72), (5.5, .5, 71), (6, 1, 69), (7, 1, 67),
             (8, .5, 66), (8.5, .5, 67), (9, 1, 69), (10, 1, 74), (11, 1, 72),
             (12, 2, 71), (14, 1, 75), (15, 1, 71),
             (16, .5, 71), (16.5, .5, 74), (17, 1, 76), (18, .5, 79), (18.5, .5, 78), (19, 1, 76),
             (20, 1, 79), (21, 1, 76), (22, 1, 72), (23, 1, 76),
             (24, 1, 74), (25, .5, 72), (25.5, .5, 71), (26, 1, 69), (27, 1, 66),
             (28, 3, 71), (31, 1, 63)]
    LEAD_B = [(0, 1, 81), (1, 1, 79), (2, 2, 76), (4, 1, 79), (5, 1, 76), (6, 2, 71),
              (8, 1, 72), (9, 1, 76), (10, 1, 79), (11, 1, 84), (12, 3, 81), (15, 1, 78),
              (16, 1, 81), (17, .5, 83), (17.5, .5, 84), (18, 2, 81), (20, 1, 79), (21, 1, 76),
              (22, 2, 79), (24, 2, 78), (26, 2, 75), (28, 2, 71), (30, 1, 75), (31, 1, 78)]
    STABS = [(0, "all", .3, 0), (.75, "all", .3, -6), (2.5, "all", .5, 0)]
    t = 0
    # intro: bass + drums + guitar hits
    s.pattern(1, t, prog("Em | Em | C | B7"), BASS, 90, Voicer(40, 52))
    s.drums(t, 3, {KICK: "x.....x.x.....x.", HAT: "x.x.x.x.x.x.x.x."}, 0.85)
    s.drums(t + 12, 1, {SNARE: "....x.x.x.xxxxxx", KICK: "x.......x......."}, 0.9)
    power_chords(s, 0, t, prog("Em | Em | C | B7"), "x.....x.....x...", 76)
    t += 16
    for rep in range(2):
        s.note(9, t, 1, CRASH, 105)
        power_chords(s, 0, t, A, "xoooxoooxoooxoxo", 72)
        s.pattern(1, t, A, BASS, 92, Voicer(40, 52))
        s.pattern(3, t, A, CHOP, 64, vb)
        s.melody(2, t, ACC_A, 90 if rep == 0 else 96, tr=0 if rep == 0 else 12 * 0)
        if rep == 1:
            s.melody(5, t, ACC_A, 58, tr=12)
        s.drums(t, 8, ROCK, 0.95)
        t += 32
        s.note(9, t, 1, CRASH, 110)
        power_chords(s, 0, t, B, "x.x.xox.x.x.xoxo", 74)
        s.pattern(1, t, B, BASS, 94, Voicer(40, 52))
        s.pattern(4, t, B, STABS, 84, vbr)
        s.melody(5, t, LEAD_B, 84)
        s.drums(t, 7, {**ROCK, HAT: "x.xxx.xxx.xxx.xx"}, 1.0)
        s.drums(t + 28, 1, {SNARE: "x.x.x.x.xxxxxxxx", KICK: "x...x...x...x..."}, 1.0)
        t += 32
        if rep == 0:
            # breakdown: let-ring guitar, toms, accordion echoes
            s.note(9, t, 1, CRASH, 100)
            power_chords(s, 0, t, BRK, "-.......-.......", 66)
            s.pattern(1, t, BRK, [(0, "B", 2, 0), (2.5, "B", 1, -8)], 84, Voicer(40, 52))
            s.drums(t, 4, {TOM_LO: "x.....x...x.....", TOM_MID: "........x.......", HAT: "....x.......x..."}, 0.85)
            s.melody(2, t, [(0, 2, 76), (2, 2, 74), (4, 4, 71), (8, 2, 72), (10, 2, 71), (12, 4, 75)], 76)
            t += 16
    return s, t, -16


def town():
    """'Sirens on Main Street' - stage 2 (the town), a step darker than the village. C minor
    (harmonic, with a Neapolitan Db), 144 bpm. Distorted galloping guitar, low tremolo strings,
    the town's percussive organ melody, brass stabs and a choir in the B section."""
    s = Song("town", 144, {
        0: (30, {7: 72, 10: 44, 91: 22}),    # distortion guitar
        1: (34, {7: 102, 10: 64, 91: 12}),   # picked bass
        2: (17, {7: 88, 10: 80, 91: 45}),    # percussive organ lead
        3: (44, {7: 70, 10: 50, 91: 70}),    # tremolo strings, low
        4: (61, {7: 80, 10: 56, 91: 50}),    # brass section
        5: (52, {7: 58, 10: 74, 91: 110}),   # choir
        9: (16, {7: 102, 91: 30}),
    }, seed=11)
    vs, vbr, vc = Voicer(48, 64), Voicer(55, 72), Voicer(55, 74)
    A = prog("Cm | Ab | Bb | G7 | Cm | Ab | Fm | G7")
    B = prog("Fm | Cm | Db | G7 | Fm | Cm | Db | G7")
    INTRO = prog("Cm | Cm | Db | G7")
    GALLOP = [(0, "B", .5, 6), (.5, "B", .25, -10), (.75, "B", .25, -10), (1, "B8", .5, 0), (1.5, "B", .25, -10),
              (1.75, "B", .25, -10), (2, "B", .5, 4), (2.5, "B", .25, -10), (2.75, "B", .25, -10),
              (3, "B5", .5, 0), (3.5, "B", .25, -10), (3.75, "B", .25, -10)]
    STABS = [(0, "all", .3, 0), (1.5, "all", .3, -8), (2.5, "all", .4, -2), (3.5, "all", .25, -10)]
    BEAT = {KICK: "x.....x.x.x.....", SNARE: "....x.......x...", HAT: "x.x.x.x.x.x.x.x."}
    ORG_A = [(0, .5, 72), (.5, .5, 75), (1, 1, 79), (2, 1, 77), (3, 1, 75),
             (4, 1.5, 80), (5.5, .5, 79), (6, 1, 77), (7, 1, 75),
             (8, 1, 74), (9, 1, 77), (10, 1.5, 82), (11.5, .5, 80),
             (12, 1, 79), (13, 1, 80), (14, 1, 79), (15, 1, 71),
             (16, .5, 72), (16.5, .5, 75), (17, 1, 79), (18, 1, 84), (19, 1, 82),
             (20, 1, 80), (21, 1, 79), (22, 2, 77),
             (24, 1, 77), (25, 1, 80), (26, 1, 84), (27, 1, 80),
             (28, 3, 79), (31, 1, 83)]
    LEAD_B = [(0, 2, 77), (2, 1, 75), (3, 1, 72), (4, 2, 75), (6, 2, 67), (8, 2, 68), (10, 1, 72), (11, 1, 77),
              (12, 3, 74), (15, 1, 71), (16, 2, 80), (18, 1, 79), (19, 1, 77), (20, 2, 75), (22, 2, 72),
              (24, 2, 73), (26, 2, 77), (28, 3, 79), (31, 1, 71)]
    t = 0
    # intro: a siren-like string swell over the galloping bass, guitar hits on the Db
    s.pad(3, t, INTRO, 70, vs, swell=(60, 120))
    s.pattern(1, t, INTRO, GALLOP, 88, Voicer(36, 50))
    s.drums(t, 3, {KICK: "x.....x.x.....x.", HAT: "x.x.x.x.x.x.x.x."}, 0.85)
    s.drums(t + 12, 1, {SNARE: "....x.x.x.xxxxxx", TOM_LO: "x.......x.......", KICK: "x...x...x...x..."}, 0.95)
    power_chords(s, 0, t, INTRO, "x.....x.....x...", 74, lo=36, hi=47)
    t += 16
    for rep in range(2):
        s.note(9, t, 1, CRASH, 108)
        power_chords(s, 0, t, A, "xoooxooxxoooxoxo", 70, lo=36, hi=47)
        s.pattern(1, t, A, GALLOP, 94, Voicer(36, 50))
        s.pad(3, t, A, 54, vs)
        s.melody(2, t, ORG_A, 92)
        if rep == 1:
            s.melody(4, t, ORG_A, 58, tr=-12)
        s.drums(t, 8, BEAT, 0.95)
        t += 32
        s.note(9, t, 1, CRASH, 112)
        power_chords(s, 0, t, B, "x.x.xox.x.x.xoxo", 72, lo=36, hi=47)
        s.pattern(1, t, B, GALLOP, 96, Voicer(36, 50))
        s.pattern(4, t, B, STABS, 84, vbr)
        s.pad(5, t, B, 60, vc, swell=(70, 115))
        s.melody(4, t, LEAD_B, 90)
        s.drums(t, 7, {**BEAT, KICK: "x.x...x.x.x...x."}, 1.0)
        s.drums(t + 28, 1, {SNARE: "x.x.x.x.xxxxxxxx", KICK: "x...x...x...x..."}, 1.0)
        t += 32
    return s, t, -16


# ======================================================================= BOSS
def boss():
    """'Tyrant of Fluff' - the Fluffy King's own theme (final boss). C minor (harmonic), 152 bpm.
    16th-note chug riff, orchestra hits, choir, square lead, synth brass."""
    s = Song("boss", 152, {
        0: (30, {7: 76, 10: 40, 91: 20}),    # distortion guitar
        1: (38, {7: 96, 10: 64, 91: 10}),    # synth bass
        2: (52, {7: 66, 10: 80, 91: 110}),   # choir
        3: (62, {7: 80, 10: 52, 91: 50}),    # synth brass
        4: (80, {7: 74, 10: 66, 91: 45}),    # square lead
        5: (55, {7: 76, 10: 64, 91: 70}),    # orchestra hit
        9: (16, {7: 104, 91: 30}),
    }, seed=13)
    vc, vbr = Voicer(55, 74), Voicer(58, 77)
    A = prog("Cm | Cm | Ab | Bb | Cm | Cm | Db | G7")
    B = prog("Fm | Cm | Ab | G | Fm | Db | G7 | G7")
    RIFF = [(0, 1, 36), (2, 1, 36), (3, 1, 36), (4, 1, 39), (6, 1, 36), (8, 1, 36), (9, 1, 36),
            (10, 1, 42), (11, 1, 41), (12, 1, 36), (14, 1, 39), (15, 1, 38)]
    LEAD_A = [(0, 1.5, 72), (1.5, .5, 75), (2, 1, 74), (3, 1, 72), (4, 1, 79), (5, 1, 77), (6, 1, 75), (7, 1, 74),
              (8, 1.5, 72), (9.5, .5, 75), (10, 1, 80), (11, 1, 79), (12, 2, 77), (14, 1, 74), (15, 1, 70),
              (16, 1.5, 72), (17.5, .5, 75), (18, 1, 79), (19, 1, 84), (20, 2, 83), (22, 1, 79), (23, 1, 75),
              (24, 1.5, 77), (25.5, .5, 80), (26, 1, 85), (27, 1, 84), (28, 2, 83), (30, 1, 79), (31, 1, 74)]
    LEAD_B = [(0, 3, 84), (3, 1, 80), (4, 4, 79), (8, 2, 80), (10, 2, 84), (12, 4, 83),
              (16, 3, 84), (19, 1, 85), (20, 4, 80), (24, 2, 79), (26, 2, 83), (28, 4, 86)]
    BLAST = {KICK: "x.x.x.x.x.x.x.x.", SNARE: "....x.......x...", HAT: "x.x.x.x.x.x.x.x."}
    STABS = [(0, "all", .25, 0), (.5, "all", .25, -8), (1.5, "all", .5, 0), (3, "all", .25, -4), (3.5, "all", .25, -4)]
    t = 0
    # intro: riff alone, then orchestra hits
    riff(s, 0, t, 4, [(st, ln, p + 12) for st, ln, p in RIFF], 80)
    riff(s, 1, t, 4, RIFF, 92)
    for bar in range(4):
        s.note(5, t + bar * 4, 1, 60, 100)
    s.drums(t + 8, 2, {KICK: "x...x...x...x...", SNARE: "............xxxx"}, 0.9)
    t += 16
    for rep in range(2):
        s.note(9, t, 1, CRASH, 112)
        riff(s, 0, t, 8, [(st, ln, p + 12) for st, ln, p in RIFF], 78)
        power_chords(s, 0, t, A, "x...............", 70)
        riff(s, 1, t, 8, RIFF, 94)
        s.melody(4, t, LEAD_A, 92)
        if rep == 1:
            s.melody(3, t, LEAD_A, 62, tr=-12)
        s.drums(t, 8, BLAST, 1.0)
        for bar in (0, 4):
            s.note(5, t + bar * 4, 1, 60, 104)
        t += 32
        s.note(9, t, 1, CRASH, 115)
        power_chords(s, 0, t, B, "x.x.x.x.x.x.x.x.", 72)
        s.pattern(1, t, B, [(0, "B", .25, 4), (.5, "B", .25, -6), (1, "B8", .25, 0), (1.5, "B", .25, -6),
                            (2, "B", .25, 4), (2.5, "B8", .25, -6), (3, "B", .25, 0), (3.5, "B5", .25, -6)],
                  94, Voicer(36, 48), bass_lo=36, bass_hi=47)
        s.pad(2, t, B, 64, vc, swell=(80, 120))
        s.pattern(3, t, B, STABS, 84, vbr)
        s.melody(4, t, LEAD_B, 92)
        s.drums(t, 7, {**BLAST, RIDE: "x...x...x...x...", HAT: ""}, 1.0)
        s.drums(t + 28, 1, {SNARE: "xxxxxxxx........", TOM_HI: "........xx......",
                            TOM_MID: "..........xx....", TOM_LO: "............xxxx"}, 1.0)
        t += 32
    return s, t, -15


def boss_village():
    """'Harvest of Fear' - village bosses. E minor with a Phrygian F, 146 bpm.
    Folk horror: chugging guitar, fiddle + accordion leads, church organ, choir."""
    s = Song("boss_village", 146, {
        0: (30, {7: 74, 10: 40, 91: 20}),    # distortion guitar
        1: (34, {7: 98, 10: 64, 91: 10}),    # picked bass
        2: (52, {7: 64, 10: 80, 91: 110}),   # choir
        3: (19, {7: 70, 10: 50, 91: 70}),    # church organ
        4: (110, {7: 88, 10: 70, 91: 45}),   # fiddle lead
        5: (21, {7: 80, 10: 90, 91: 40}),    # accordion
        9: (16, {7: 104, 91: 30}),
    }, seed=17)
    vc, vo = Voicer(55, 74), Voicer(52, 72)
    A = prog("Em | F | Em | D | Em | F | C | B7")
    B = prog("Am | Em | F | Em | Am | C | B7 | B7")
    RIFF = [(0, 1, 40), (2, 1, 40), (3, 1, 41), (4, 1, 40), (6, 1, 43), (8, 1, 40), (9, 1, 40),
            (10, 1, 41), (12, 1, 47), (13, 1, 45), (14, 1, 43), (15, 1, 41)]
    LEAD_A = [(0, 1, 76), (1, 1, 77), (2, 2, 79), (4, 1, 77), (5, 1, 76), (6, 2, 74),
              (8, 1, 76), (9, 1, 79), (10, 1, 81), (11, 1, 83), (12, 3, 84), (15, 1, 83),
              (16, 1, 81), (17, 1, 79), (18, 2, 77), (20, 1, 79), (21, 1, 77), (22, 2, 76),
              (24, 1, 72), (25, 1, 74), (26, 1, 76), (27, 1, 77), (28, 3, 75), (31, 1, 71)]
    LEAD_B = [(0, 3, 81), (3, 1, 79), (4, 4, 77), (8, 2, 79), (10, 2, 81), (12, 4, 83),
              (16, 3, 84), (19, 1, 83), (20, 4, 81), (24, 2, 79), (26, 2, 77), (28, 4, 75)]
    BEAT = {KICK: "x.x...x.x.x...x.", SNARE: "....x.......x...", HAT: "x.x.x.x.x.x.x.x."}
    t = 0
    # intro: the riff alone under a tolling organ
    riff(s, 0, t, 4, [(st, ln, p + 12) for st, ln, p in RIFF], 78)
    riff(s, 1, t, 4, RIFF, 92)
    s.pad(3, t, prog("Em | Em | F | B7"), 70, vo)
    s.drums(t + 8, 2, {KICK: "x...x...x...x...", SNARE: "............xxxx"}, 0.9)
    t += 16
    for rep in range(2):
        s.note(9, t, 1, CRASH, 112)
        riff(s, 0, t, 8, [(st, ln, p + 12) for st, ln, p in RIFF], 76)
        power_chords(s, 0, t, A, "x...............", 70)
        riff(s, 1, t, 8, RIFF, 94)
        s.melody(4, t, LEAD_A, 92)
        if rep == 1:
            s.melody(5, t, LEAD_A, 62, tr=-12)
        s.drums(t, 8, BEAT, 1.0)
        t += 32
        s.note(9, t, 1, CRASH, 115)
        power_chords(s, 0, t, B, "x.x.x.x.x.x.x.x.", 72)
        s.pattern(1, t, B, [(0, "B", .25, 4), (.5, "B", .25, -6), (1, "B8", .25, 0), (1.5, "B", .25, -6),
                            (2, "B", .25, 4), (2.5, "B8", .25, -6), (3, "B", .25, 0), (3.5, "B5", .25, -6)],
                  94, Voicer(36, 48), bass_lo=36, bass_hi=47)
        s.pad(2, t, B, 62, vc, swell=(80, 120))
        s.pad(3, t, B, 64, vo)
        s.melody(5, t, LEAD_B, 90)
        s.melody(4, t, LEAD_B, 70, tr=12)
        s.drums(t, 7, {**BEAT, RIDE: "x...x...x...x...", HAT: ""}, 1.0)
        s.drums(t + 28, 1, {SNARE: "xxxxxxxx........", TOM_HI: "........xx......",
                            TOM_MID: "..........xx....", TOM_LO: "............xxxx"}, 1.0)
        t += 32
    return s, t, -15


def boss_town():
    """'Rush Hour Rampage' - town bosses. A minor, 158 bpm.
    Pumping octave synth bass, guitar, brass stabs, a siren-like square lead, orchestra hits."""
    s = Song("boss_town", 158, {
        0: (30, {7: 72, 10: 40, 91: 20}),    # distortion guitar
        1: (39, {7: 96, 10: 64, 91: 10}),    # synth bass 2
        2: (61, {7: 80, 10: 52, 91: 50}),    # brass section
        3: (80, {7: 72, 10: 70, 91: 45}),    # square lead (siren)
        4: (81, {7: 62, 10: 58, 91: 45}),    # saw lead
        5: (55, {7: 76, 10: 64, 91: 70}),    # orchestra hit
        9: (16, {7: 104, 91: 30}),
    }, seed=19)
    vbr = Voicer(58, 77)
    A = prog("Am | Am | F | G | Am | Am | Dm | E7")
    B = prog("Dm | Am | F | G | Dm | F | E7 | E7")
    PUMP = [(0, 1, 45), (1, 1, 45), (2, 1, 57), (3, 1, 45), (4, 1, 45), (5, 1, 55), (6, 1, 45), (7, 1, 53),
            (8, 1, 45), (9, 1, 45), (10, 1, 57), (11, 1, 45), (12, 1, 52), (13, 1, 53), (14, 1, 55), (15, 1, 56)]
    SIREN = [(0, .5, 81), (.5, .5, 76), (1, .5, 81), (1.5, .5, 76), (2, 2, 84), (4, 1, 83), (5, 1, 81), (6, 2, 79),
             (8, .5, 81), (8.5, .5, 76), (9, .5, 81), (9.5, .5, 76), (10, 2, 86), (12, 1, 84), (13, 1, 83),
             (14, 2, 80), (16, .5, 81), (16.5, .5, 76), (17, .5, 81), (17.5, .5, 76), (18, 2, 84), (20, 1, 86),
             (21, 1, 88), (22, 2, 84), (24, 1, 83), (25, 1, 81), (26, 1, 79), (27, 1, 77), (28, 3, 76), (31, 1, 80)]
    LEAD_B = [(0, 3, 74), (3, 1, 76), (4, 4, 77), (8, 2, 76), (10, 2, 74), (12, 4, 72),
              (16, 3, 77), (19, 1, 79), (20, 4, 81), (24, 2, 80), (26, 2, 76), (28, 4, 80)]
    DRIVE = {KICK: "x...x...x...x...", SNARE: "....x.......x...", HAT: "xxxxxxxxxxxxxxxx"}
    STABS = [(0, "all", .25, 0), (.75, "all", .25, -6), (2, "all", .25, 0), (2.5, "all", .5, -4)]
    t = 0
    # intro: bass pump and hits, like a car revving
    riff(s, 1, t, 4, PUMP, 92)
    for bar in range(4):
        s.note(5, t + bar * 4, 1, 57, 100)
    s.drums(t, 3, {HAT: "x.x.x.x.x.x.x.x."}, 0.8)
    s.drums(t + 12, 1, {SNARE: "....x.x.x.xxxxxx", KICK: "x.......x......."}, 0.9)
    t += 16
    for rep in range(2):
        s.note(9, t, 1, CRASH, 112)
        power_chords(s, 0, t, A, "x.ox.ox.x.ox.ox.", 70, lo=40, hi=52)
        riff(s, 1, t, 8, PUMP, 94)
        s.melody(3, t, SIREN, 90)
        if rep == 1:
            s.melody(4, t, SIREN, 60, tr=-12)
        s.drums(t, 8, DRIVE, 1.0)
        for bar in (0, 4):
            s.note(5, t + bar * 4, 1, 57, 104)
        t += 32
        s.note(9, t, 1, CRASH, 115)
        power_chords(s, 0, t, B, "x.x.x.x.x.x.x.x.", 72, lo=40, hi=52)
        s.pattern(1, t, B, [(0, "B", .25, 4), (.5, "B8", .25, -6), (1, "B", .25, 0), (1.5, "B8", .25, -6),
                            (2, "B", .25, 4), (2.5, "B8", .25, -6), (3, "B", .25, 0), (3.5, "B5", .25, -6)],
                  94, Voicer(36, 48), bass_lo=36, bass_hi=47)
        s.pattern(2, t, B, STABS, 86, vbr)
        s.melody(4, t, LEAD_B, 90, tr=12)
        s.drums(t, 7, {**DRIVE, RIDE: "x...x...x...x...", HAT: "x.x.x.x.x.x.x.x."}, 1.0)
        s.drums(t + 28, 1, {SNARE: "xxxxxxxx........", TOM_HI: "........xx......",
                            TOM_MID: "..........xx....", TOM_LO: "............xxxx"}, 1.0)
        t += 32
    return s, t, -15


def city():
    """'Neon Nights' - stage 3 (the city, at night), the darkest room theme yet. D minor with a
    Phrygian Eb and a diminished turnaround, 138 bpm. Pulsing 16th synth bass, a tolling clock bell,
    cold pad, saw lead, chugging distorted guitar and a low choir; double kick in the B section."""
    s = Song("city", 138, {
        0: (39, {7: 96, 10: 64, 91: 20}),    # synth bass
        1: (95, {7: 60, 10: 64, 91: 100}),   # sweep pad
        2: (81, {7: 80, 10: 70, 91: 60}),    # saw lead
        3: (30, {7: 64, 10: 44, 91: 25}),    # distortion guitar
        4: (14, {7: 70, 10: 84, 91: 90}),    # tubular bell (the city clock)
        5: (52, {7: 60, 10: 50, 91: 110}),   # choir
        9: (16, {7: 100, 91: 40}),
    }, seed=23)
    vp, vc = Voicer(55, 74), Voicer(50, 67)
    A = prog("Dm | Bb | Eb | A7 | Dm | Bb | Gm | C#dim7")
    B = prog("Gm | Dm | Eb | A7 | Gm | Bb | Eb | A7")
    INTRO = prog("Dm | Eb | Dm | A7")
    PULSE = [(i * .25, "B" if i % 4 else "B8", .22, 4 if i % 4 == 0 else -8) for i in range(16)]
    LEAD_A = [(0, 1.5, 74), (1.5, .5, 77), (2, 2, 81),
              (4, 1.5, 82), (5.5, .5, 81), (6, 2, 77),
              (8, 1.5, 75), (9.5, .5, 79), (10, 2, 82),
              (12, 1, 81), (13, 1, 79), (14, 1, 76), (15, 1, 73),
              (16, 1.5, 74), (17.5, .5, 77), (18, 2, 81),
              (20, 1, 82), (21, 1, 86), (22, 2, 82),
              (24, 1, 79), (25, 1, 82), (26, 1, 86), (27, 1, 82),
              (28, 2, 85), (30, 1, 82), (31, 1, 79)]
    LEAD_B = [(0, 3, 79), (3, 1, 77), (4, 4, 74), (8, 2, 75), (10, 2, 79), (12, 4, 81),
              (16, 3, 82), (19, 1, 81), (20, 4, 77), (24, 2, 75), (26, 2, 79), (28, 2, 81), (30, 2, 73)]
    BEAT = {KICK: "x.......x.......", SNARE: "....x.......x...", HAT: "x.xxx.xxx.xxx.xx"}
    t = 0
    # intro: the clock tolls over a cold pad, then the bass starts pulsing
    s.pad(1, t, INTRO, 64, vp, swell=(50, 120))
    for bar in range(0, 4, 2):
        s.note(4, t + bar * 4, 3.5, 62, 96)
    s.pattern(0, t, INTRO[2:], PULSE, 84, Voicer(36, 50), bass_lo=33, bass_hi=45)
    s.drums(t + 8, 1, {HAT: "x.x.x.x.x.x.x.x.", KICK: "x.......x......."}, 0.8)
    s.drums(t + 12, 1, {SNARE: "....x...x.x.xxxx", KICK: "x...x...x...x..."}, 0.9)
    t += 16
    for rep in range(2):
        s.note(9, t, 1, CRASH, 104)
        s.pattern(0, t, A, PULSE, 92, Voicer(36, 50), bass_lo=33, bass_hi=45)
        s.pad(1, t, A, 56, vp)
        s.melody(2, t, LEAD_A, 90)
        for bar in (0, 4):
            s.note(4, t + bar * 4, 3.5, 62, 90)
        power_chords(s, 3, t, A, "x.......x.x....." if rep == 1 else "-.......-.......", 64, lo=38, hi=50)
        s.drums(t, 8, BEAT, 0.95)
        t += 32
        s.note(9, t, 1, CRASH, 110)
        s.pattern(0, t, B, PULSE, 94, Voicer(36, 50), bass_lo=33, bass_hi=45)
        s.pad(5, t, B, 62, vc, swell=(70, 120))
        power_chords(s, 3, t, B, "xoooxoooxoxoxooo", 66, lo=38, hi=50)
        s.melody(2, t, LEAD_B, 92)
        s.drums(t, 7, {**BEAT, KICK: "x.x.x.x.x.x.x.x.", SNARE: "....x.......x..x"}, 1.0)
        s.drums(t + 28, 1, {SNARE: "x.x.x.x.xxxxxxxx", KICK: "x...x...x...x...",
                            TOM_LO: "............x.x."}, 1.0)
        t += 32
    return s, t, -16


def boss_city():
    """'Searchlight' - city bosses. F# minor, 162 bpm.
    16th-note guitar riff, synth bass, choir, square lead, orchestra hits."""
    s = Song("boss_city", 162, {
        0: (30, {7: 74, 10: 40, 91: 20}),    # distortion guitar
        1: (38, {7: 96, 10: 64, 91: 10}),    # synth bass
        2: (52, {7: 62, 10: 80, 91: 110}),   # choir
        3: (80, {7: 76, 10: 66, 91: 45}),    # square lead
        4: (55, {7: 76, 10: 64, 91: 70}),    # orchestra hit
        9: (16, {7: 104, 91: 30}),
    }, seed=29)
    vc = Voicer(54, 73)
    A = prog("F#m | F#m | D | E | F#m | F#m | Bm | C#7")
    B = prog("Bm | F#m | D | E | Bm | D | C#7 | C#7")
    RIFF = [(0, 1, 42), (2, 1, 42), (3, 1, 42), (4, 1, 45), (6, 1, 42), (8, 1, 42), (9, 1, 42),
            (10, 1, 49), (11, 1, 47), (12, 1, 42), (14, 1, 45), (15, 1, 44)]
    LEAD_A = [(0, 1.5, 78), (1.5, .5, 81), (2, 1, 80), (3, 1, 78), (4, 1, 85), (5, 1, 83), (6, 1, 81),
              (7, 1, 80), (8, 1.5, 78), (9.5, .5, 81), (10, 1, 86), (11, 1, 85), (12, 2, 83), (14, 1, 80),
              (15, 1, 76), (16, 1.5, 78), (17.5, .5, 81), (18, 1, 85), (19, 1, 90), (20, 2, 88), (22, 1, 85),
              (23, 1, 81), (24, 1.5, 83), (25.5, .5, 86), (26, 1, 90), (27, 1, 88), (28, 2, 89), (30, 1, 85),
              (31, 1, 80)]
    LEAD_B = [(0, 3, 90), (3, 1, 86), (4, 4, 85), (8, 2, 86), (10, 2, 90), (12, 4, 89),
              (16, 3, 90), (19, 1, 91), (20, 4, 86), (24, 2, 85), (26, 2, 89), (28, 4, 92)]
    BLAST = {KICK: "x.x.x.x.x.x.x.x.", SNARE: "....x.......x...", HAT: "xxxxxxxxxxxxxxxx"}
    t = 0
    riff(s, 0, t, 4, [(st, ln, p + 12) for st, ln, p in RIFF], 80)
    riff(s, 1, t, 4, RIFF, 92)
    for bar in range(4):
        s.note(4, t + bar * 4, 1, 54, 100)
    s.drums(t + 8, 2, {KICK: "x...x...x...x...", SNARE: "............xxxx"}, 0.9)
    t += 16
    for rep in range(2):
        s.note(9, t, 1, CRASH, 112)
        riff(s, 0, t, 8, [(st, ln, p + 12) for st, ln, p in RIFF], 78)
        power_chords(s, 0, t, A, "x...............", 70, lo=40, hi=52)
        riff(s, 1, t, 8, RIFF, 94)
        s.melody(3, t, LEAD_A, 90)
        s.drums(t, 8, BLAST, 1.0)
        for bar in (0, 4):
            s.note(4, t + bar * 4, 1, 54, 104)
        t += 32
        s.note(9, t, 1, CRASH, 115)
        power_chords(s, 0, t, B, "x.x.x.x.x.x.x.x.", 72, lo=40, hi=52)
        s.pattern(1, t, B, [(0, "B", .25, 4), (.5, "B", .25, -6), (1, "B8", .25, 0), (1.5, "B", .25, -6),
                            (2, "B", .25, 4), (2.5, "B8", .25, -6), (3, "B", .25, 0), (3.5, "B5", .25, -6)],
                  94, Voicer(36, 48), bass_lo=36, bass_hi=47)
        s.pad(2, t, B, 62, vc, swell=(80, 120))
        s.melody(3, t, LEAD_B, 92)
        s.drums(t, 7, {**BLAST, RIDE: "x...x...x...x...", HAT: ""}, 1.0)
        s.drums(t + 28, 1, {SNARE: "xxxxxxxx........", TOM_HI: "........xx......",
                            TOM_MID: "..........xx....", TOM_LO: "............xxxx"}, 1.0)
        t += 32
    return s, t, -15


def tower():
    """'Glass and Steel' - stage 4 (the skyscraper, by day). E minor, 132 bpm. It opens like lift
    music - vibraphone and electric piano over a soft bossa - then the doors open on driving office
    rock: fingered bass, overdriven guitar, synth strings and a saw lead."""
    s = Song("tower", 132, {
        0: (33, {7: 96, 10: 64, 91: 20}),    # fingered bass
        1: (4, {7: 70, 10: 50, 91: 70}),     # electric piano
        2: (11, {7: 80, 10: 78, 91: 80}),    # vibraphone (the lift music)
        3: (29, {7: 66, 10: 44, 91: 30}),    # overdrive guitar
        4: (50, {7: 62, 10: 64, 91: 100}),   # synth strings
        5: (81, {7: 76, 10: 70, 91: 60}),    # saw lead
        9: (16, {7: 100, 91: 40}),
    }, seed=31)
    vp, vs = Voicer(55, 72), Voicer(52, 70)
    LIFT = prog("Em7 | A7 | Dmaj7 | B7")
    A = prog("Em | C | D | B7 | Em | C | Am | B7")
    B = prog("Am | Em | C | D | Am | Em | F#dim7 | B7")
    BOSSA = [(0, "B", .9, 0), (1.5, "all", .4, -10), (2, "B5", .9, -4), (3, "all", .4, -10), (3.5, "all", .4, -14)]
    VIBES = [(0, 1.5, 76), (1.5, .5, 74), (2, 2, 71), (4, 1, 73), (5, 1, 76), (6, 2, 79),
             (8, 1.5, 78), (9.5, .5, 76), (10, 2, 73), (12, 1, 75), (13, 1, 78), (14, 2, 81)]
    PUMP = [(i * .5, "B" if i % 2 == 0 else "B8", .45, 4 if i % 2 == 0 else -6) for i in range(8)]
    LEAD_A = [(0, 1, 76), (1, .5, 79), (1.5, .5, 83), (2, 2, 81), (4, 1, 79), (5, 1, 76), (6, 2, 78),
              (8, 1, 74), (9, .5, 78), (9.5, .5, 81), (10, 2, 83), (12, 1.5, 81), (13.5, .5, 78), (14, 2, 75),
              (16, 1, 76), (17, .5, 79), (17.5, .5, 83), (18, 2, 88), (20, 1, 86), (21, 1, 84), (22, 2, 83),
              (24, 1, 81), (25, 1, 84), (26, 2, 88), (28, 1.5, 87), (29.5, .5, 86), (30, 2, 83)]
    LEAD_B = [(0, 3, 84), (3, 1, 83), (4, 4, 79), (8, 2, 79), (10, 2, 83), (12, 4, 86),
              (16, 3, 84), (19, 1, 83), (20, 4, 79), (24, 2, 81), (26, 2, 84), (28, 4, 87)]
    BEAT = {KICK: "x.....x.x.......", SNARE: "....x.......x...", HAT: "x.x.x.x.x.x.x.x."}
    t = 0
    # intro: lift music (ding!), the doors shudder, then it all kicks in
    s.note(2, t, 1, 84, 90)
    s.note(2, t + 0.5, 1.5, 80, 86)
    s.pattern(1, t, LIFT, BOSSA, 64, vp, bass_lo=40, bass_hi=52)
    s.pattern(0, t, LIFT, [(0, "B", .9, 0), (2, "B5", .9, -6)], 70, Voicer(36, 48), bass_lo=33, bass_hi=45)
    s.melody(2, t, VIBES, 84)
    s.drums(t, 3, {RIDE: "x..x..x.x..x..x.", KICK: "x.......x.......", HAT: "....x.......x..."}, 0.6)
    s.drums(t + 12, 1, {SNARE: "x...x...x.x.xxxx", KICK: "x...x...x...x...", CRASH: "...............x"}, 0.9)
    t += 16
    for rep in range(2):
        s.note(9, t, 1, CRASH, 106)
        s.pattern(0, t, A, PUMP, 94, Voicer(36, 50), bass_lo=33, bass_hi=45)
        power_chords(s, 3, t, A, "x.x.x.oox.x.x.oo" if rep == 1 else "x.......x.x.....", 66, lo=38, hi=50)
        s.pad(4, t, A, 56, vs)
        s.melody(5, t, LEAD_A, 90)
        s.drums(t, 8, BEAT, 0.95)
        t += 32
        s.note(9, t, 1, CRASH, 110)
        s.pattern(0, t, B, PUMP, 94, Voicer(36, 50), bass_lo=33, bass_hi=45)
        s.pattern(1, t, B, [(0, "all", .45, -6), (1.5, "all", .45, -10), (3, "all", .45, -10)], 62, vp)
        power_chords(s, 3, t, B, "xoooxoooxoxoxooo", 68, lo=38, hi=50)
        s.melody(5, t, LEAD_B, 92)
        s.drums(t, 7, {**BEAT, KICK: "x.x...x.x.x...x.", HAT: "xxxxxxxxxxxxxxxx"}, 1.0)
        s.drums(t + 28, 1, {SNARE: "x.x.x.x.xxxxxxxx", KICK: "x...x...x...x...",
                            TOM_LO: "............x.x."}, 1.0)
        t += 32
    return s, t, -16


def boss_tower():
    """'Above the Clouds' - skyscraper bosses, fought on the roof. C minor, 168 bpm.
    Galloping strings and guitar, brass stabs, a soaring choir, timpani and orchestra hits."""
    s = Song("boss_tower", 168, {
        0: (30, {7: 74, 10: 40, 91: 20}),    # distortion guitar
        1: (48, {7: 84, 10: 64, 91: 70}),    # strings
        2: (61, {7: 80, 10: 74, 91: 50}),    # brass section
        3: (52, {7: 64, 10: 84, 91: 110}),   # choir
        4: (47, {7: 90, 10: 64, 91: 40}),    # timpani
        5: (33, {7: 94, 10: 64, 91: 10}),    # fingered bass
        6: (55, {7: 72, 10: 64, 91: 70}),    # orchestra hit
        9: (16, {7: 104, 91: 30}),
    }, seed=37)
    vb, vc = Voicer(55, 74), Voicer(56, 76)
    A = prog("Cm | Cm | Ab | Bb | Cm | Cm | Fm | G7")
    B = prog("Ab | Eb | Bb | Cm | Ab | Eb | G7 | G7")
    GALLOP = [(0, "B", .2, 4), (.5, "B", .15, -8), (.75, "B", .15, -8), (1, "B", .2, 2), (1.5, "B", .15, -8),
              (1.75, "B", .15, -8), (2, "B8", .2, 2), (2.5, "B", .15, -8), (2.75, "B", .15, -8),
              (3, "B5", .2, 2), (3.5, "B", .15, -8), (3.75, "B", .15, -8)]
    STRINGS = [(i * .25, "top2", .22, 0 if i % 4 == 0 else -10) for i in range(16)]
    BRASS = [(0, 1.5, 72), (1.5, .5, 75), (2, 2, 79), (4, 1, 80), (5, 1, 79), (6, 2, 75),
             (8, 1.5, 77), (9.5, .5, 75), (10, 2, 74), (12, 1, 72), (13, 1, 74), (14, 2, 71),
             (16, 1.5, 72), (17.5, .5, 75), (18, 2, 79), (20, 1, 84), (21, 1, 82), (22, 2, 79),
             (24, 1, 80), (25, 1, 79), (26, 1, 77), (27, 1, 75), (28, 4, 74)]
    CHOIR = [(0, 4, 79), (4, 4, 75), (8, 4, 77), (12, 4, 79), (16, 4, 80), (20, 4, 79), (24, 4, 79), (28, 4, 74)]
    BLAST = {KICK: "x.x.x.x.x.x.x.x.", SNARE: "....x.......x...", HAT: "x.x.x.x.x.x.x.x."}
    t = 0
    # intro: timpani rolls and hits under a held choir chord
    s.pad(3, t, prog("Cm | Cm | Ab | G7"), 70, vc, swell=(40, 120))
    for bar in range(4):
        for k in range(4):
            s.note(4, t + bar * 4 + k, 0.5, 36 if k % 2 == 0 else 43, 96 if k == 0 else 74)
        s.note(6, t + bar * 4, 1, 60, 96)
    s.drums(t + 12, 1, {SNARE: "x.x.x.x.xxxxxxxx", KICK: "x...x...x...x...", CRASH: "...............x"}, 1.0)
    t += 16
    for rep in range(2):
        s.note(9, t, 1, CRASH, 112)
        s.pattern(5, t, A, GALLOP, 92, Voicer(36, 48), bass_lo=36, bass_hi=47)
        s.pattern(1, t, A, STRINGS, 74, vb)
        power_chords(s, 0, t, A, "x.ox.ox.x.ox.ox." if rep == 1 else "x...............", 70, lo=36, hi=48)
        s.melody(2, t, BRASS, 92)
        for bar in (0, 4):
            s.note(6, t + bar * 4, 1, 60, 104)
            s.note(4, t + bar * 4, 0.5, 36, 104)
        s.drums(t, 8, BLAST, 1.0)
        t += 32
        s.note(9, t, 1, CRASH, 116)
        s.pattern(5, t, B, GALLOP, 94, Voicer(36, 48), bass_lo=36, bass_hi=47)
        power_chords(s, 0, t, B, "x.x.x.x.x.x.x.x.", 72, lo=36, hi=48)
        s.melody(3, t, CHOIR, 84)
        s.melody(2, t, [(st, d, p + 12) for st, d, p in CHOIR[::2]], 78)
        s.pad(1, t, B, 66, vb, swell=(70, 120))
        s.drums(t, 7, {**BLAST, RIDE: "x...x...x...x...", HAT: ""}, 1.0)
        s.drums(t + 28, 1, {SNARE: "xxxxxxxx........", TOM_HI: "........xx......",
                            TOM_MID: "..........xx....", TOM_LO: "............xxxx"}, 1.0)
        t += 32
    return s, t, -15


def sky():
    """'Silver Lining' - stage 5 (the clouds, the Fluffy King's realm). B minor, 140 bpm. A harp and a
    celesta twinkle over a choir, then the band lifts off: driving drums, bass and guitar, soaring
    strings and a flute lead, the choir taking the tune in the second half."""
    s = Song("sky", 140, {
        0: (46, {7: 84, 10: 50, 91: 80}),    # harp
        1: (33, {7: 96, 10: 64, 91: 20}),    # fingered bass
        2: (52, {7: 66, 10: 78, 91: 110}),   # choir aahs
        3: (48, {7: 70, 10: 44, 91: 90}),    # strings
        4: (73, {7: 86, 10: 70, 91: 70}),    # flute lead
        5: (29, {7: 62, 10: 84, 91: 30}),    # overdrive guitar
        6: (8, {7: 70, 10: 64, 91: 90}),     # celesta
        9: (16, {7: 98, 91: 40}),
    }, seed=41)
    vh, vc, vs = Voicer(59, 79), Voicer(55, 74), Voicer(54, 72)
    INTRO = prog("Bm | G | D | F#")
    A = prog("Bm | G | D | A | Bm | G | Em | F#")
    B = prog("G | D | A | Bm | G | D | Em | F#7")
    HARP = [(i * .5, [0, 1, 2, 3, 2, 1, 2, 3][i], .9, 0 if i % 4 == 0 else -8) for i in range(8)]
    BELLS = [(0, 1, 83), (1, 1, 78), (2, 2, 74), (4, 1, 79), (5, 1, 74), (6, 2, 71),
             (8, 1, 78), (9, 1, 74), (10, 2, 81), (12, 1, 82), (13, 1, 78), (14, 2, 73)]
    PULSE = [(i * .5, "B" if i % 4 != 3 else "B8", .45, 4 if i % 2 == 0 else -6) for i in range(8)]
    LEAD_A = [(0, 1, 78), (1, .5, 76), (1.5, .5, 74), (2, 2, 71), (4, 1, 74), (5, 1, 76), (6, 2, 79),
              (8, 1.5, 78), (9.5, .5, 76), (10, 2, 74), (12, 1, 73), (13, 1, 76), (14, 2, 81),
              (16, 1, 78), (17, .5, 79), (17.5, .5, 81), (18, 2, 83), (20, 1, 81), (21, 1, 79), (22, 2, 74),
              (24, 1, 79), (25, 1, 78), (26, 2, 76), (28, 3, 78), (31, 1, 73)]
    CHOIR = [(0, 4, 79), (4, 4, 78), (8, 4, 76), (12, 4, 74), (16, 4, 79), (20, 4, 81), (24, 4, 79), (28, 4, 82)]
    BEAT = {KICK: "x.....x.x.......", SNARE: "....x.......x...", HAT: "x.x.x.x.x.x.x.x."}
    t = 0
    # intro: harp and celesta among the clouds, the choir swelling in, a drum fill to take off
    s.pattern(0, t, INTRO, HARP, 70, vh)
    s.melody(6, t, BELLS, 76)
    s.pad(2, t, INTRO, 58, vc, swell=(30, 110))
    s.drums(t + 12, 1, {SNARE: "....x...x.x.xxxx", KICK: "x...x...x...x...", CRASH: "...............x"}, 0.85)
    t += 16
    for rep in range(2):
        s.note(9, t, 1, CRASH, 108)
        s.pattern(0, t, A, HARP, 64, vh)
        s.pattern(1, t, A, PULSE, 92, Voicer(36, 50), bass_lo=35, bass_hi=47)
        power_chords(s, 5, t, A, "x.......x.x....." if rep == 0 else "x.x.x.oox.x.x.oo", 64, lo=38, hi=50)
        s.pad(3, t, A, 54, vs, swell=(60, 110))
        s.melody(4, t, LEAD_A, 92)
        s.drums(t, 8, BEAT, 0.95)
        t += 32
        s.note(9, t, 1, CRASH, 112)
        s.pattern(0, t, B, HARP, 62, vh)
        s.pattern(1, t, B, PULSE, 94, Voicer(36, 50), bass_lo=35, bass_hi=47)
        power_chords(s, 5, t, B, "xoooxoooxoxoxooo", 66, lo=38, hi=50)
        s.melody(2, t, CHOIR, 86)
        s.melody(4, t, [(st, d, p + 12) for st, d, p in CHOIR[::2]], 74)
        s.pad(3, t, B, 60, vs, swell=(70, 120))
        s.drums(t, 7, {**BEAT, KICK: "x.x...x.x.x...x.", RIDE: "x.x.x.x.x.x.x.x.", HAT: ""}, 1.0)
        s.drums(t + 28, 1, {SNARE: "x.x.x.x.xxxxxxxx", KICK: "x...x...x...x...",
                            TOM_LO: "............x.x."}, 1.0)
        t += 32
    return s, t, -16


def sea():
    """'Under the Fluff' - the secret sea under the clouds. F# minor (dorian), 108 bpm. Vibraphone
    ripples and a slow warm pad, a fretless bass sliding along the sea bed, a pan flute singing the
    tune; in the second half a light band joins (ride and brushes, clean guitar) and the choir answers."""
    s = Song("sea", 108, {
        0: (11, {7: 80, 10: 50, 91: 90}),    # vibraphone
        1: (35, {7: 96, 10: 64, 91: 30}),    # fretless bass
        2: (89, {7: 62, 10: 70, 91: 110}),   # warm pad
        3: (75, {7: 84, 10: 76, 91: 80}),    # pan flute lead
        4: (27, {7: 58, 10: 40, 91: 70}),    # clean guitar
        5: (52, {7: 60, 10: 84, 91: 110}),   # choir aahs
        6: (8, {7: 64, 10: 64, 91: 100}),    # celesta: bubbles
        9: (0, {7: 84, 91: 50}),
    }, seed=53)
    vv, vp, vg = Voicer(57, 76), Voicer(52, 70), Voicer(55, 72)
    INTRO = prog("F#m | E | D | C#m")
    A = prog("F#m | E | D | E | F#m | B | D | C#7")
    B = prog("D | A | E | F#m | D | A | Bm | C#7")
    RIPPLE = [(i * .5, [0, 2, 1, 3, 2, 1, 3, 2][i], .8, 0 if i % 4 == 0 else -10) for i in range(8)]
    BUBBLES = [(3, .5, 90), (3.5, .5, 93), (7.5, .5, 97), (11, .5, 90), (11.5, .5, 95), (15, 1, 102)]
    SWAY = [(0, "B", 1.4, 4), (1.5, "B5", .5, -8), (2, "B8", 1.4, 0), (3.5, "B5", .5, -8)]
    LEAD_A = [(0, 2, 73), (2, 1, 76), (3, 1, 78), (4, 3, 76), (7, 1, 73), (8, 2, 74), (10, 1, 76),
              (11, 1, 73), (12, 4, 71), (16, 2, 73), (18, 1, 76), (19, 1, 78), (20, 2, 81), (22, 2, 78),
              (24, 2, 78), (26, 1, 76), (27, 1, 74), (28, 4, 73)]
    CHOIR = [(0, 4, 78), (4, 4, 76), (8, 4, 76), (12, 4, 73), (16, 4, 74), (20, 4, 76), (24, 4, 74), (28, 4, 77)]
    BEAT = {KICK: "x.......x.....x.", SNARE: "....x.......x...", RIDE: "x..x..x.x..x..x."}
    t = 0
    # intro: just the water - vibraphone ripples, the pad swelling in, bubbles rising on the celesta
    s.pattern(0, t, INTRO, RIPPLE, 62, vv)
    s.pad(2, t, INTRO, 56, vp, swell=(20, 100))
    s.melody(6, t, BUBBLES, 70)
    t += 16
    for rep in range(2):
        s.pattern(0, t, A, RIPPLE, 60, vv)
        s.pattern(1, t, A, SWAY, 86, Voicer(36, 50), bass_lo=37, bass_hi=49)
        s.pad(2, t, A, 52, vp, swell=(50, 100))
        s.melody(3, t, LEAD_A, 88)
        s.melody(6, t + 16, BUBBLES, 64)
        if rep == 1:
            s.drums(t, 8, BEAT, 0.8)
        t += 32
        s.pattern(0, t, B, RIPPLE, 58, vv)
        s.pattern(1, t, B, SWAY, 88, Voicer(36, 50), bass_lo=37, bass_hi=49)
        s.pattern(4, t, B, [(i * .5, "top2", .45, 0 if i % 2 == 0 else -8) for i in range(8)], 60, vg)
        s.melody(5, t, CHOIR, 80)
        s.melody(3, t, [(st, d, p + 12) for st, d, p in CHOIR[::2]], 70)
        s.drums(t, 8, BEAT, 0.85)
        t += 32
    return s, t, -17


def boss_sea():
    """'Jaws of Fluff' - the Fluffy Shark, in its lair. E minor, 160 bpm. Low strings creep in on a
    two-note semitone, closer and closer; then the whole band bites: galloping bass, chugging guitar,
    brass stabs and a choir over the top, timpani under every slam."""
    s = Song("boss_sea", 160, {
        0: (30, {7: 76, 10: 40, 91: 20}),    # distortion guitar
        1: (48, {7: 86, 10: 64, 91: 70}),    # strings
        2: (61, {7: 80, 10: 78, 91: 50}),    # brass
        3: (52, {7: 62, 10: 84, 91: 110}),   # choir
        4: (47, {7: 90, 10: 64, 91: 40}),    # timpani
        5: (33, {7: 96, 10: 64, 91: 10}),    # fingered bass
        9: (16, {7: 104, 91: 30}),
    }, seed=59)
    vs, vc = Voicer(52, 71), Voicer(55, 74)
    A = prog("Em | Em | C | D | Em | Em | Am | B7")
    B = prog("C | G | D | Em | C | G | B7 | B7")
    GALLOP = [(0, "B", .2, 4), (.5, "B", .15, -8), (.75, "B", .15, -8), (1, "B", .2, 2), (1.5, "B", .15, -8),
              (1.75, "B", .15, -8), (2, "B8", .2, 2), (2.5, "B", .15, -8), (2.75, "B", .15, -8),
              (3, "B5", .2, 2), (3.5, "B", .15, -8), (3.75, "B", .15, -8)]
    BRASS = [(0, 1, 76), (1, 1, 77), (2, 2, 76), (4, .5, 79), (4.5, .5, 77), (5, 1, 76), (6, 2, 74),
             (8, 1, 72), (9, 1, 74), (10, 2, 76), (12, 1, 78), (13, 1, 79), (14, 2, 78),
             (16, 1, 76), (17, 1, 77), (18, 2, 76), (20, 1, 79), (21, 1, 81), (22, 2, 83),
             (24, 1, 81), (25, 1, 79), (26, 1, 77), (27, 1, 76), (28, 4, 75)]
    CHOIR = [(0, 4, 76), (4, 4, 74), (8, 4, 74), (12, 4, 76), (16, 4, 76), (20, 4, 74), (24, 4, 75), (28, 4, 75)]
    BEAT = {KICK: "x.x.x.x.x.x.x.x.", SNARE: "....x.......x...", HAT: "x.x.x.x.x.x.x.x."}
    t = 0
    # intro: the two notes in the deep, getting faster, louder - it's coming
    for bar in range(8):
        speed = 1 if bar < 4 else 2
        for k in range(4 * speed):
            beat = t + bar * 4 + k / speed
            pitch = 28 if k % 2 == 0 else 29
            v = 70 + bar * 5
            s.note(5, beat, 0.9 / speed, pitch, v)
            s.note(1, beat, 0.9 / speed, pitch + 12, v - 10)
    s.note(4, t + 28, 4, 40, 100)
    s.drums(t + 28, 1, {SNARE: "x.x.x.x.xxxxxxxx", KICK: "x...x...x...x...", CRASH: "...............x"}, 1.0)
    t += 32
    for rep in range(2):
        s.note(9, t, 1, CRASH, 114)
        s.pattern(5, t, A, GALLOP, 94, Voicer(36, 48), bass_lo=28, bass_hi=40)
        power_chords(s, 0, t, A, "x.ox.ox.x.ox.ox." if rep == 1 else "x..x..x.x..x..x.", 72, lo=40, hi=52)
        s.pad(1, t, A, 66, vs, swell=(60, 110))
        s.melody(2, t, BRASS, 92)
        for bar in (0, 4):
            s.note(4, t + bar * 4, 0.5, 40, 104)
        s.drums(t, 8, BEAT, 1.0)
        t += 32
        s.note(9, t, 1, CRASH, 116)
        s.pattern(5, t, B, GALLOP, 96, Voicer(36, 48), bass_lo=28, bass_hi=40)
        power_chords(s, 0, t, B, "x.x.x.x.x.x.x.x.", 74, lo=40, hi=52)
        s.melody(3, t, CHOIR, 86)
        s.melody(2, t, [(st, d, p + 12) for st, d, p in CHOIR[::2]], 78)
        s.pad(1, t, B, 68, vs, swell=(70, 120))
        s.drums(t, 7, {**BEAT, RIDE: "x...x...x...x...", HAT: ""}, 1.0)
        s.drums(t + 28, 1, {SNARE: "xxxxxxxx........", TOM_HI: "........xx......",
                            TOM_MID: "..........xx....", TOM_LO: "............xxxx"}, 1.0)
        t += 32
    return s, t, -15


def slums():
    """'Back Alley Blues' - the secret Slums under the city. E minor (dorian), 88 bpm with a lazy
    swing. A walking fingered bass and dusty brushes, a Rhodes comping off the beat, a muted trumpet
    sneaking the tune; in the B part a dirty organ and a low sax answer, and the kick hits harder."""
    s = Song("slums", 88, {
        0: (33, {7: 96, 10: 64, 91: 30}),    # fingered bass
        1: (4, {7: 70, 10: 50, 91: 70}),     # Rhodes
        2: (59, {7: 84, 10: 76, 91: 80}),    # muted trumpet
        3: (17, {7: 56, 10: 40, 91: 60}),    # percussive organ
        4: (66, {7: 70, 10: 88, 91: 70}),    # tenor sax
        9: (0, {7: 84, 91: 40}),
    }, seed=61, swing=0.62)
    vr, vo = Voicer(52, 70), Voicer(55, 72)
    INTRO = prog("Em7 | A7 | Em7 | B7")
    A = prog("Em7 | A7 | Em7 | A7 | Cmaj7 | B7 | Em7 | B7")
    B = prog("Am7 | D7 | Gmaj7 | Cmaj7 | Am7 | B7 | Em7 | B7")
    WALK = [(0, "B", .9, 6), (1, "B5", .9, -6), (2, "B8", .9, 0), (3, "B5", .9, -8)]
    COMP = [(.5, "all", .3, 0), (1.5, "top2", .25, -10), (2.5, "all", .3, -4), (3.75, "top2", .2, -12)]
    STAB = [(1, "all", .4, 0), (3, "all", .4, -6)]
    LEAD_A = [(0, 1, 71), (1, .5, 74), (1.5, 1.5, 76), (3.5, .5, 74),
              (4, 2, 73), (6, 1, 71), (7, 1, 69),
              (8, 1, 71), (9, .5, 74), (9.5, 1.5, 79), (11, 1, 76),
              (12, 3, 73), (15, 1, 69),
              (16, 1.5, 71), (17.5, .5, 72), (18, 2, 71), (20, 1, 69), (21, 1, 67),
              (22, 2, 66), (24, 1, 64), (25, .5, 67), (25.5, 1.5, 71), (27, 1, 69),
              (28, 3, 66), (31, 1, 63)]
    SAX_B = [(0, 2, 60), (2, 1, 62), (3, 1, 64), (4, 3, 66), (7, 1, 62), (8, 4, 67), (12, 2, 64),
             (14, 2, 60), (16, 2, 57), (18, 1, 60), (19, 1, 64), (20, 3, 63), (23, 1, 59), (24, 4, 64),
             (28, 2, 63), (30, 2, 59)]
    BEAT = {KICK: "x.....x...x.....", SNARE: "....o.......o..g", HAT: "x.x.x.x.x.x.x.x."}
    BEAT_B = {KICK: "x.....x.x.x.....", SNARE: "....x.......x..g", HAT: "x.xxx.x.x.xxx.x.", OHAT: "..............x."}
    t = 0
    # intro: just the bass walking in and the Rhodes, the hats ticking like a dripping pipe
    s.pattern(0, t, INTRO, WALK, 84, Voicer(36, 50), bass_lo=33, bass_hi=45)
    s.pattern(1, t, INTRO, COMP, 58, vr)
    s.drums(t, 4, {HAT: "x...x...x...x..."}, 0.7)
    t += 16
    for rep in range(2):
        s.pattern(0, t, A, WALK, 90, Voicer(36, 50), bass_lo=33, bass_hi=45)
        s.pattern(1, t, A, COMP, 60, vr)
        s.melody(2, t, LEAD_A, 88)
        s.drums(t, 8, BEAT, 0.85)
        t += 32
        s.pattern(0, t, B, WALK, 94, Voicer(36, 50), bass_lo=33, bass_hi=45)
        s.pattern(3, t, B, STAB, 64, vo)
        s.melody(4, t, SAX_B, 86)
        if rep == 1:
            s.melody(2, t, [(st + 0.5, d, p + 12) for st, d, p in SAX_B[1::3]], 70)
        s.drums(t, 7, BEAT_B, 0.95)
        s.drums(t + 28, 1, {SNARE: "....x...x.x.xxxx", KICK: "x.......x.......", TOM_LO: "..........x.x..."}, 0.95)
        t += 32
    return s, t, -17


def boss_slums():
    """'Gutter Groove' - the Slums' bosses. E minor (dorian), 140 bpm. Slap bass bouncing on the
    octave (the Blue Blob's boing), a dirty organ, gritty guitar, a brass section blaring the hook,
    a sax wailing over the B part; four-on-the-floor with claps."""
    s = Song("boss_slums", 140, {
        0: (36, {7: 98, 10: 64, 91: 15}),    # slap bass 1
        1: (17, {7: 66, 10: 44, 91: 50}),    # percussive organ
        2: (29, {7: 64, 10: 84, 91: 25}),    # overdriven guitar
        3: (61, {7: 84, 10: 58, 91: 50}),    # brass section
        4: (66, {7: 80, 10: 70, 91: 60}),    # tenor sax
        9: (0, {7: 104, 91: 30}),
    }, seed=67)
    vo, vb = Voicer(55, 72), Voicer(58, 77)
    A = prog("Em7 | Em7 | G | A7 | Em7 | Em7 | C | B7")
    B = prog("Am7 | D7 | Gmaj7 | Cmaj7 | Am7 | B7 | Em7 | B7")
    BOING = [(0, "B", .3, 6), (.5, "B8", .2, -4), (.75, "B", .2, -8), (1.5, "B8", .3, 0),
             (2, "B", .3, 4), (2.75, "B5", .2, -6), (3, "B8", .3, 0), (3.5, "B", .2, -6)]
    COMP = [(.5, "all", .2, 0), (1.5, "all", .2, -8), (2.5, "all", .2, -2), (3.5, "all", .2, -8)]
    HOOK = [(0, .5, 76), (.5, .5, 79), (1, 1, 81), (2, .5, 79), (2.5, .5, 76), (3, 1, 74),
            (4, .5, 76), (4.5, .5, 79), (5, 1, 81), (6, 2, 83),
            (8, .5, 76), (8.5, .5, 79), (9, 1, 81), (10, .5, 79), (10.5, .5, 76), (11, 1, 74),
            (12, 1, 72), (13, 1, 74), (14, 2, 71),
            (16, .5, 76), (16.5, .5, 79), (17, 1, 81), (18, .5, 79), (18.5, .5, 76), (19, 1, 74),
            (20, .5, 76), (20.5, .5, 79), (21, 1, 81), (22, 2, 86),
            (24, 1, 84), (25, 1, 83), (26, 1, 81), (27, 1, 79), (28, 3, 78), (31, 1, 75)]
    SAX = [(0, 2, 69), (2, 1, 72), (3, 1, 74), (4, 3, 76), (7, 1, 74), (8, 4, 71), (12, 2, 72), (14, 2, 74),
           (16, 2, 69), (18, 1, 71), (19, 1, 72), (20, 3, 75), (23, 1, 71), (24, 4, 76), (28, 2, 75), (30, 2, 71)]
    BEAT = {KICK: "x...x...x...x...", CLAP: "....x.......x...", HAT: "x.xxx.xxx.xxx.xx", OHAT: "......x.......x."}
    t = 0
    # intro: the bass bouncing on its own, the organ joining, a clap fill
    s.pattern(0, t, A[:4], BOING, 90, Voicer(36, 50), bass_lo=28, bass_hi=40)
    s.pattern(1, t + 8, A[2:4], COMP, 60, vo)
    s.drums(t, 3, {HAT: "x.x.x.x.x.x.x.x."}, 0.8)
    s.drums(t + 12, 1, {CLAP: "....x...x.x.xxxx", KICK: "x.......x......."}, 0.9)
    t += 16
    for rep in range(2):
        s.note(9, t, 1, CRASH, 112)
        s.pattern(0, t, A, BOING, 96, Voicer(36, 50), bass_lo=28, bass_hi=40)
        s.pattern(1, t, A, COMP, 62, vo)
        s.melody(3, t, HOOK, 90)
        power_chords(s, 2, t, A, "x..x..x.x..x.x.." if rep == 1 else "-.......-.......", 64, lo=40, hi=52)
        s.drums(t, 8, BEAT, 1.0)
        t += 32
        s.note(9, t, 1, CRASH, 115)
        s.pattern(0, t, B, BOING, 96, Voicer(36, 50), bass_lo=28, bass_hi=40)
        s.pattern(3, t, B, [(0, "all", .3, 0), (1.5, "all", .3, -6), (3, "all", .5, -2)], 80, vb)
        s.melody(4, t, SAX, 92)
        s.drums(t, 7, {**BEAT, RIDE: "x...x...x...x..."}, 1.0)
        s.drums(t + 28, 1, {SNARE: "x.x.x.x.xxxxxxxx", KICK: "x...x...x...x...", TOM_LO: "............x.x."}, 1.0)
        t += 32
    return s, t, -15


def undercity():
    """'Down Where the King Sleeps' - the Undercity, the False King's streets under the city. B minor
    (harmonic), 84 bpm. A music box picking out a crooked lullaby over a low drone, pizzicato strings
    dripping like the pipes, a bass clarinet creeping along; in the B part a muted trumpet fanfare
    (a king's, played badly) and a slow heartbeat on the timpani."""
    s = Song("undercity", 84, {
        0: (10, {7: 80, 10: 64, 91: 100}),   # music box
        1: (45, {7: 70, 10: 40, 91: 70}),    # pizzicato strings
        2: (71, {7: 84, 10: 76, 91: 60}),    # clarinet (an octave down: the bass clarinet)
        3: (89, {7: 56, 10: 64, 91: 110}),   # warm pad (the drone)
        4: (59, {7: 76, 10: 88, 91: 70}),    # muted trumpet
        5: (47, {7: 90, 10: 64, 91: 60}),    # timpani
    }, seed=71)
    vp, vb = Voicer(47, 64), Voicer(60, 76)
    INTRO = prog("Bm | Bm | G | F#7")
    A = prog("Bm | Em | F#7 | Bm | G | Em | C#dim7 | F#7")
    B = prog("G | D | Em | Bm | G | Em | F#7 | F#7")
    DRIP = [(0, "B", .3, 0), (1.5, "B5", .3, -8), (2.5, "top2", .25, -6), (3.5, "B8", .2, -10)]
    BOX = [(0, 1, 83), (1, .5, 86), (1.5, .5, 85), (2, 2, 83), (4, 1, 81), (5, .5, 78), (5.5, .5, 79), (6, 2, 78),
           (8, 1, 79), (9, .5, 81), (9.5, .5, 83), (10, 1, 86), (11, 1, 85), (12, 3, 82), (15, 1, 78),
           (16, 1, 83), (17, .5, 86), (17.5, .5, 85), (18, 2, 83), (20, 1, 79), (21, 1, 78), (22, 2, 76),
           (24, 1, 77), (25, 1, 79), (26, 1, 80), (27, 1, 82), (28, 3, 82), (31, 1, 78)]
    CREEP = [(0, 2, 47), (2, 1, 49), (3, 1, 50), (4, 3, 52), (7, 1, 50), (8, 2, 49), (10, 2, 47), (12, 4, 46),
             (16, 2, 47), (18, 1, 50), (19, 1, 52), (20, 3, 54), (23, 1, 52), (24, 2, 49), (26, 2, 46), (28, 4, 46)]
    FANFARE = [(0, .5, 67), (.5, .5, 67), (1, 1, 71), (2, 2, 74), (4, 1, 72), (5, 1, 71), (6, 2, 69),
               (8, .5, 67), (8.5, .5, 67), (9, 1, 71), (10, 2, 76), (12, 1, 74), (13, 1, 73), (14, 2, 73),
               (16, .5, 71), (16.5, .5, 71), (17, 1, 74), (18, 2, 79), (20, 1, 78), (21, 1, 76), (22, 2, 74),
               (24, 2, 73), (26, 2, 74), (28, 4, 73)]
    t = 0
    # intro: just the drone and the dripping
    s.pad(3, t, INTRO, 54, vp, swell=(20, 90))
    s.pattern(1, t + 8, INTRO[2:], DRIP, 58, vb, bass_lo=35, bass_hi=47)
    t += 16
    for rep in range(2):
        s.pad(3, t, A, 50, vp)
        s.pattern(1, t, A, DRIP, 60, vb, bass_lo=35, bass_hi=47)
        s.melody(0, t, BOX, 82)
        if rep == 1:
            s.melody(2, t, CREEP, 74)
        for bar in range(8):  # the heartbeat
            s.note(5, t + bar * 4, .5, 35, 70)
            s.note(5, t + bar * 4 + .75, .5, 35, 56)
        t += 32
        s.pad(3, t, B, 52, vp, swell=(50, 100))
        s.pattern(1, t, B, DRIP, 62, vb, bass_lo=35, bass_hi=47)
        s.melody(4, t, FANFARE, 80)
        s.melody(2, t, CREEP, 70)
        for bar in range(8):
            s.note(5, t + bar * 4, .5, 35, 78)
            s.note(5, t + bar * 4 + .75, .5, 35, 62)
        t += 32
    return s, t, -18


def boss_undercity():
    """'The False King's March' - the Skunk King. C minor, 124 bpm. A pompous royal march played by the wrong
    band: oom-pah tuba, a snare that won't stop rolling, a nasal oboe blowing the fanfare, a wheezy organ, a
    big bass drum; in the B part the brass takes over and the harmony goes sour (a king who stinks)."""
    s = Song("boss_undercity", 124, {
        0: (58, {7: 100, 10: 64, 91: 25}),   # tuba
        1: (68, {7: 84, 10: 76, 91: 50}),    # oboe
        2: (20, {7: 62, 10: 44, 91: 60}),    # reed organ
        3: (61, {7: 84, 10: 58, 91: 50}),    # brass section
        4: (56, {7: 76, 10: 70, 91: 50}),    # trumpet
        9: (48, {7: 104, 91: 40}),           # orchestral kit
    }, seed=79)
    vo, vb = Voicer(55, 72), Voicer(58, 77)
    A = prog("Cm | G7 | Cm | Ab | Fm | Cm | G7 | Cm")
    B = prog("Ab | Eb | Fm | Bdim7 | Ab | Db | G7 | G7")
    OOMPAH = [(0, "B", .4, 6), (1, "top2", .3, -10), (2, "B5", .4, 0), (3, "top2", .3, -10)]
    FANFARE = [(0, .5, 72), (.5, .5, 72), (1, 1, 79), (2, 1.5, 79), (3.5, .5, 77), (4, 1, 75), (5, 1, 74), (6, 2, 72),
               (8, .5, 72), (8.5, .5, 72), (9, 1, 80), (10, 1.5, 80), (11.5, .5, 79), (12, 1, 77), (13, 1, 75), (14, 2, 74),
               (16, .5, 77), (16.5, .5, 77), (17, 1, 80), (18, 1, 84), (19, 1, 83), (20, 2, 80), (22, 1, 79), (23, 1, 77),
               (24, 1, 75), (25, 1, 74), (26, 1, 72), (27, 1, 71), (28, 3, 72), (31, 1, 67)]
    BRASS = [(0, "all", .5, 0), (.75, "all", .25, -8), (2, "all", 1, -2)]
    LEAD_B = [(0, 2, 80), (2, 1, 79), (3, 1, 77), (4, 3, 79), (7, 1, 75), (8, 2, 77), (10, 1, 75), (11, 1, 74),
              (12, 4, 77), (16, 2, 80), (18, 1, 82), (19, 1, 80), (20, 3, 77), (23, 1, 74), (24, 4, 79), (28, 4, 74)]
    MARCH = {KICK: "x.......x.......", SNARE: "....x.g.x.g.x.xx", CRASH: "x..............."}
    t = 0
    # intro: a snare roll and the tuba clearing its throat, then the royal stab
    s.drums(t, 2, {SNARE: "xxxxxxxxxxxxxxxx"}, 0.7)
    s.pattern(0, t + 8, A[:2], OOMPAH, 90, vo, bass_lo=36, bass_hi=48)
    s.drums(t + 12, 1, {SNARE: "x.x.x.x.xxxxxxxx", KICK: "x.......x......."}, 0.9)
    t += 16
    for rep in range(2):
        s.pattern(0, t, A, OOMPAH, 96, vo, bass_lo=36, bass_hi=48)
        s.pad(2, t, A, 50, vo)
        s.melody(1, t, FANFARE, 92)
        if rep == 1:
            s.melody(4, t, [(st, d, p - 12) for st, d, p in FANFARE[::2]], 70)
        s.drums(t, 8, {KICK: "x.......x.......", SNARE: "....x.g.x.g.x.xx"}, 0.95)
        t += 32
        s.note(9, t, 1, CRASH, 112)
        s.pattern(0, t, B, OOMPAH, 98, vo, bass_lo=36, bass_hi=48)
        s.pattern(3, t, B, BRASS, 86, vb)
        s.melody(4, t, LEAD_B, 90)
        s.drums(t, 7, MARCH, 1.0)
        s.drums(t + 28, 1, {SNARE: "xxxxxxxxxxxxxxxx", KICK: "x...x...x...x..."}, 1.0)
        t += 32
    return s, t, -15


def moon():
    """'Cheese Moon Lullaby' - the Moon (up the family's rocket, Grandfluff's home). D Lydian, 100 bpm,
    floating: a music box and a crystal pad drift over a soft synth bass, a vibraphone sings the tune,
    then light drums come in and strings carry the second half - low gravity, a long way from home."""
    s = Song("moon", 100, {
        0: (10, {7: 80, 10: 50, 91: 100}),   # music box
        1: (38, {7: 92, 10: 64, 91: 30}),    # synth bass
        2: (88, {7: 62, 10: 78, 91: 120}),   # new age pad
        3: (48, {7: 66, 10: 44, 91: 100}),   # strings
        4: (11, {7: 90, 10: 70, 91: 90}),    # vibraphone lead
        6: (98, {7: 58, 10: 84, 91: 110}),   # crystal sparkles
        9: (0, {7: 84, 91: 50}),
    }, seed=61)
    vm, vp, vs = Voicer(62, 81), Voicer(55, 74), Voicer(54, 72)
    INTRO = prog("D | E | Bm | G")
    A = prog("D | E | Bm | G | D | E | G | A")
    B = prog("G | A | F#m | Bm | G | A | E | A")
    BOX = [(i * .5, [0, 2, 1, 3, 2, 1, 3, 2][i], .8, 0 if i % 4 == 0 else -10) for i in range(8)]
    SPARKS = [(0, 2, 86), (3, 1, 90), (6, 2, 88), (10, 2, 93), (13, 1, 88), (15, 1, 86)]
    BASS = [(0, "B", 1.5, 6), (1.5, "B", .5, -8), (2, "B8", 1, -4), (3, "B", 1, -6)]
    LEAD_A = [(0, 1.5, 74), (1.5, .5, 73), (2, 2, 69), (4, 1, 71), (5, 1, 73), (6, 2, 76),
              (8, 1.5, 74), (9.5, .5, 71), (10, 2, 68), (12, 4, 69),
              (16, 1, 78), (17, 1, 76), (18, 2, 74), (20, 1, 73), (21, 1, 71), (22, 2, 69),
              (24, 1, 71), (25, 1, 74), (26, 2, 76), (28, 4, 73)]
    STRINGS = [(0, 4, 71), (4, 4, 73), (8, 4, 69), (12, 4, 66), (16, 4, 71), (20, 4, 73), (24, 4, 68), (28, 4, 69)]
    BEAT = {KICK: "x.........x.....", CLAP: "........x.......", HAT: "..x...x...x...x."}
    t = 0
    # intro: the music box alone in the dark, the pad swelling, sparkles like stars coming out
    s.pattern(0, t, INTRO, BOX, 66, vm)
    s.pad(2, t, INTRO, 56, vp, swell=(20, 100))
    s.melody(6, t, SPARKS, 70)
    t += 16
    for rep in range(2):
        s.pattern(0, t, A, BOX, 60, vm)
        s.pattern(1, t, A, BASS, 88, Voicer(36, 50), bass_lo=35, bass_hi=47)
        s.pad(2, t, A, 50, vp, swell=(50, 100))
        s.melody(4, t, LEAD_A, 88)
        if rep == 1:
            s.drums(t, 8, BEAT, 0.8)
        t += 32
        s.note(9, t, 1, CRASH, 84)
        s.pattern(0, t, B, BOX, 58, vm)
        s.pattern(1, t, B, BASS, 90, Voicer(36, 50), bass_lo=35, bass_hi=47)
        s.melody(3, t, STRINGS, 84)
        s.melody(4, t, [(st, d, p + 12) for st, d, p in STRINGS[::2]], 70)
        s.melody(6, t, [(st + 16, d, p) for st, d, p in SPARKS], 64)
        s.pad(2, t, B, 56, vp, swell=(60, 110))
        s.drums(t, 8, {**BEAT, RIDE: "x...x...x...x...", HAT: ""}, 0.85)
        t += 32
    return s, t, -16


TRACKS = {"menu": menu, "village": village, "town": town, "city": city, "boss": boss,
          "boss_village": boss_village, "boss_town": boss_town, "boss_city": boss_city,
          "tower": tower, "boss_tower": boss_tower, "sky": sky, "sea": sea, "boss_sea": boss_sea,
          "slums": slums, "boss_slums": boss_slums, "undercity": undercity,
          "boss_undercity": boss_undercity, "moon": moon}

if __name__ == "__main__":
    sf2, outdir = sys.argv[1], sys.argv[2]
    names = sys.argv[3:] or list(TRACKS)
    os.makedirs(outdir, exist_ok=True)
    for n in names:
        song, beats, lufs = TRACKS[n]()
        period, raw, gain = song.render(sf2, os.path.join(outdir, n), beats, lufs)
        print(f"{n}: {period:.1f}s loop, {raw:.1f} LUFS -> {lufs} ({gain:+.1f} dB)")
