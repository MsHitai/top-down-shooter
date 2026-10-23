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
    """'Main Street Shootout' - stage 2 (the town). G minor, 140 bpm.
    Walking bass, chugging guitar, percussive organ melody, brass + muted trumpet B section."""
    s = Song("town", 140, {
        0: (29, {7: 70, 10: 44, 91: 25}),    # overdriven guitar
        1: (32, {7: 104, 10: 64, 91: 15}),   # upright (acoustic) bass, walking
        2: (17, {7: 88, 10: 80, 91: 45}),    # percussive organ lead
        3: (61, {7: 76, 10: 54, 91: 50}),    # brass section
        4: (59, {7: 80, 10: 70, 91: 55}),    # muted trumpet
        9: (16, {7: 100, 91: 30}),
    }, seed=11)
    vbr = Voicer(58, 76)
    A = prog("Gm | Eb | F | D7 | Gm | Eb | Cm | D7")
    B = prog("Cm | Gm | Eb | F | Cm | Gm | D7 | D7")
    WALK = [(0, "B", 1, 4), (1, "B5", 1, -6), (2, "B8", 1, 0), (3, "B5", 1, -8)]
    STABS = [(0, "all", .3, 0), (1.5, "all", .3, -8), (2.5, "all", .4, -2)]
    BEAT = {KICK: "x.......x.x.....", SNARE: "....x.......x...", RIDE: "x.x.x.x.x.x.x.x."}
    ORG_A = [(0, .5, 67), (.5, .5, 70), (1, 1, 74), (2, 1, 72), (3, 1, 70), (4, 1.5, 75), (5.5, .5, 74),
             (6, 1, 72), (7, 1, 70), (8, 1, 72), (9, 1, 74), (10, 2, 77), (12, 2, 74), (14, 1, 78), (15, 1, 74),
             (16, .5, 79), (16.5, .5, 77), (17, 1, 74), (18, 1, 70), (19, 1, 74), (20, 1, 75), (21, 1, 74),
             (22, 2, 72), (24, 1, 72), (25, 1, 75), (26, 1, 74), (27, 1, 72), (28, 3, 74), (31, 1, 78)]
    LEAD_B = [(0, 2, 79), (2, 1, 77), (3, 1, 75), (4, 2, 74), (6, 2, 70), (8, 1, 72), (9, 1, 74), (10, 1, 75),
              (11, 1, 77), (12, 3, 79), (15, 1, 78), (16, 2, 82), (18, 1, 81), (19, 1, 79), (20, 2, 77),
              (22, 2, 74), (24, 2, 78), (26, 2, 74), (28, 3, 78), (31, 1, 74)]
    t = 0
    # intro: walking bass and ride, guitar hits
    s.pattern(1, t, prog("Gm | Gm | Eb | D7"), WALK, 92, Voicer(36, 50))
    s.drums(t, 3, {KICK: "x.......x.......", RIDE: "x.x.x.x.x.x.x.x."}, 0.85)
    s.drums(t + 12, 1, {SNARE: "....x.x.x.x.xxxx", KICK: "x.......x......."}, 0.9)
    power_chords(s, 0, t, prog("Gm | Gm | Eb | D7"), "x.....x.....x...", 74, lo=43, hi=54)
    t += 16
    for rep in range(2):
        s.note(9, t, 1, CRASH, 105)
        power_chords(s, 0, t, A, "x.oox.oox.oox.ox", 70, lo=43, hi=54)
        s.pattern(1, t, A, WALK, 94, Voicer(36, 50))
        s.melody(2, t, ORG_A, 92)
        if rep == 1:
            s.melody(4, t, ORG_A, 60, tr=-12)
        s.drums(t, 8, BEAT, 0.95)
        t += 32
        s.note(9, t, 1, CRASH, 110)
        power_chords(s, 0, t, B, "x.x.xox.x.x.xoxo", 72, lo=43, hi=54)
        s.pattern(1, t, B, WALK, 96, Voicer(36, 50))
        s.pattern(3, t, B, STABS, 82, vbr)
        s.melody(4, t, LEAD_B, 88)
        s.drums(t, 7, {**BEAT, HAT: "..x...x...x...x."}, 1.0)
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
    """'Neon Nights' - stage 3 (the city, at night). D minor, 124 bpm.
    Synthwave: octave synth bass, warm pad, saw lead, half-time guitar, off-beat hats."""
    s = Song("city", 124, {
        0: (39, {7: 96, 10: 64, 91: 20}),    # synth bass
        1: (89, {7: 62, 10: 64, 91: 90}),    # warm pad
        2: (81, {7: 82, 10: 70, 91: 60}),    # saw lead
        3: (29, {7: 64, 10: 44, 91: 25}),    # overdriven guitar
        9: (16, {7: 98, 91: 40}),
    }, seed=23)
    vp = Voicer(55, 74)
    A = prog("Dm | Bb | F | C | Dm | Bb | Gm | A7")
    B = prog("Gm | Dm | Bb | C | Gm | Bb | A7 | A7")
    OCT = [(0, "B", .5, 4), (.5, "B8", .5, -6), (1, "B", .5, 0), (1.5, "B8", .5, -6),
           (2, "B", .5, 4), (2.5, "B8", .5, -6), (3, "B", .5, 0), (3.5, "B8", .5, -6)]
    LEAD_A = [(0, 1.5, 74), (1.5, .5, 77), (2, 2, 81), (4, 1.5, 82), (5.5, .5, 81), (6, 2, 77),
              (8, 1.5, 77), (9.5, .5, 81), (10, 2, 84), (12, 1, 82), (13, 1, 81), (14, 2, 79),
              (16, 1.5, 74), (17.5, .5, 77), (18, 2, 81), (20, 1, 82), (21, 1, 84), (22, 2, 86),
              (24, 1, 82), (25, 1, 81), (26, 1, 79), (27, 1, 77), (28, 3, 76), (31, 1, 73)]
    LEAD_B = [(0, 3, 79), (3, 1, 77), (4, 4, 74), (8, 2, 77), (10, 2, 79), (12, 4, 81),
              (16, 3, 82), (19, 1, 81), (20, 4, 77), (24, 2, 76), (26, 2, 79), (28, 4, 81)]
    BEAT = {KICK: "x.......x.......", SNARE: "....x.......x...", HAT: "..x...x...x...x."}
    t = 0
    # intro: pad and bass alone, like a city waking up at night
    s.pad(1, t, prog("Dm | Bb | F | A7"), 64, vp)
    s.pattern(0, t, prog("Dm | Bb | F | A7"), OCT, 86, Voicer(36, 50))
    s.drums(t + 8, 2, {HAT: "..x...x...x...x.", KICK: "x.......x......."}, 0.8)
    t += 16
    for rep in range(2):
        s.note(9, t, 1, CRASH, 100)
        s.pattern(0, t, A, OCT, 92, Voicer(36, 50))
        s.pad(1, t, A, 58, vp)
        s.melody(2, t, LEAD_A, 90)
        if rep == 1:
            power_chords(s, 3, t, A, "-.......-.......", 62)
        s.drums(t, 8, BEAT, 0.95)
        t += 32
        s.note(9, t, 1, CRASH, 106)
        s.pattern(0, t, B, OCT, 94, Voicer(36, 50))
        s.pad(1, t, B, 62, vp)
        power_chords(s, 3, t, B, "x.......x.x.....", 66)
        s.melody(2, t, LEAD_B, 92)
        s.drums(t, 7, {**BEAT, SNARE: "....x.......x..x"}, 1.0)
        s.drums(t + 28, 1, {SNARE: "x.x.x.x.xxxxxxxx", KICK: "x...x...x...x..."}, 0.95)
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


TRACKS = {"menu": menu, "village": village, "town": town, "city": city, "boss": boss,
          "boss_village": boss_village, "boss_town": boss_town, "boss_city": boss_city}

if __name__ == "__main__":
    sf2, outdir = sys.argv[1], sys.argv[2]
    names = sys.argv[3:] or list(TRACKS)
    os.makedirs(outdir, exist_ok=True)
    for n in names:
        song, beats, lufs = TRACKS[n]()
        period, raw, gain = song.render(sf2, os.path.join(outdir, n), beats, lufs)
        print(f"{n}: {period:.1f}s loop, {raw:.1f} LUFS -> {lufs} ({gain:+.1f} dB)")
