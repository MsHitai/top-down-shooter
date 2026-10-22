#!/usr/bin/env python3
"""Generates the pixel-art sprites and room tilesets in src/main/resources/assets/.

Run from the project root:  python3 tools/gen_sprites.py
Needs Pillow. Art scale: 32 px = 1 world unit (the world is 7x5 units -> 224x160 px).

Outputs
  sprites/player.png   hero sheet, 20x28 frames:
                       row 0 = front (aiming down) walk 0-3, idle 4-5
                       row 1 = back (aiming up)    walk 0-3, idle 4-5
                       row 2 = roll tuck (col 0)
                       row 3 = death: hit, knees buckle, slump (cols 0-2), then a 28x20
                               "lying on his back" frame at (60, 3*28+8), head to the right
  sprites/props.png    tables (normal + flipped 4 ways), barrel, crate, hay bale, stone pillar,
                       broken-plank debris, chests per tier (chest_d/c/b/a + _open) and the
                       floor exit hatch - see PROPS below for the atlas rects
  tiles/<theme>.png    room tileset per theme, 16x16 tiles in one row:
                       floor a, floor b, floor c, floor d, wall face, cap (wall top / border),
                       door frame, (unused), wall face with window, wall face with decoration
"""
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets")
OUTLINE = (30, 20, 22, 255)


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, k):
    return (max(0, min(255, int(c[0] * k))), max(0, min(255, int(c[1] * k))), max(0, min(255, int(c[2] * k))), c[3])


class Canvas:
    def __init__(self, w, h):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.px = self.img.load()
        self.w, self.h = w, h

    def put(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[x, y] = c

    def rect(self, x, y, w, h, c):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.put(xx, yy, c)

    def outline(self, color=OUTLINE):
        solid = [[self.px[x, y][3] > 0 for y in range(self.h)] for x in range(self.w)]
        for x in range(self.w):
            for y in range(self.h):
                if solid[x][y]:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < self.w and 0 <= ny < self.h and solid[nx][ny]:
                        self.px[x, y] = color
                        break


# ---------------------------------------------------------------------- hero
SKIN = hexc("d9a066")
SKIN_HI = hexc("eec39a")
SKIN_SH = hexc("b07a48")
SHIRT = hexc("2f9e5b")
SHIRT_HI = hexc("4cc27a")
SHIRT_SH = hexc("1f6e3f")
PANTS = hexc("4a2c55")
PANTS_SH = hexc("33203c")
SHOE = hexc("707078")
SHOE_SH = hexc("4a4a52")
EYE = hexc("1b1b2a")
HAIR = hexc("3b2a20")

FW, FH = 20, 28


def hero_frame(back=False, leg_phase=0, bob=0, kneel=0, dead=False):
    """leg_phase: 0 idle, 1 left fwd, 2 passing, 3 right fwd, 4 passing.
    kneel lowers the body onto its knees (legs get shorter); dead draws X eyes and an open mouth."""
    c = Canvas(FW, FH)
    by = bob + kneel  # body offset (down)
    # legs + shoes (drawn first)
    lift_l = {0: 0, 1: 0, 2: 1, 3: 2, 4: 1}[leg_phase]
    lift_r = {0: 0, 1: 2, 2: 1, 3: 0, 4: 1}[leg_phase]
    for lx, lift in ((6, lift_l), (10, lift_r)):
        top = 20 + by
        bottom = 25 - lift
        c.rect(lx, top, 4, bottom - top, PANTS)
        c.rect(lx + 3, top, 1, bottom - top, PANTS_SH)
        c.rect(lx, bottom, 4 + (0 if back else 1), 2, SHOE)
        c.rect(lx, bottom + 1, 4 + (0 if back else 1), 1, SHOE_SH)
    # torso
    c.rect(5, 12 + by, 10, 9, SHIRT)
    c.rect(5, 12 + by, 2, 8, SHIRT_HI)
    c.rect(13, 12 + by, 2, 9, SHIRT_SH)
    c.rect(5, 20 + by, 10, 1, SHIRT_SH)
    # short sleeves (no arms - the gun hand is drawn by the game)
    c.rect(4, 13 + by, 1, 3, SHIRT_HI)
    c.rect(15, 13 + by, 1, 3, SHIRT_SH)
    if not back:
        c.rect(8, 12 + by, 4, 1, SKIN_SH)  # collar / neck
    # head
    hx, hy = 5, 2 + by
    c.rect(hx + 1, hy, 8, 10, SKIN)
    c.rect(hx, hy + 1, 10, 8, SKIN)
    c.rect(hx + 1, hy + 9, 8, 1, SKIN_SH)
    c.rect(hx + 8, hy + 2, 2, 7, SKIN_SH)
    c.rect(hx + 2, hy + 1, 3, 2, SKIN_HI)  # bald shine
    # tuft of hair
    c.put(hx + 4, hy - 1, HAIR)
    c.put(hx + 5, hy - 2, HAIR)
    c.put(hx + 6, hy - 1, HAIR)
    if back:
        c.rect(hx + 1, hy + 6, 8, 3, SKIN_SH)  # back of the head
        c.put(hx - 1 + 1, hy + 4, SKIN_SH)
        c.put(hx + 9, hy + 4, SKIN_SH)
    else:
        # 3/4 face looking right
        if dead:
            for ex in (hx + 5, hx + 8):  # X eyes
                for dx, dy in ((-1, -1), (1, -1), (0, 0), (-1, 1), (1, 1)):
                    c.put(ex + dx, hy + 4 + dy, EYE)
            c.rect(hx + 7, hy + 7, 2, 2, EYE)  # open mouth
        else:
            c.rect(hx + 5, hy + 4, 1, 2, EYE)
            c.rect(hx + 8, hy + 4, 1, 2, EYE)
            c.put(hx + 5, hy + 4, (240, 240, 250, 255))
            c.put(hx + 7, hy + 7, SKIN_SH)  # mouth
            c.put(hx + 8, hy + 7, SKIN_SH)
        c.put(hx, hy + 5, SKIN_SH)  # ear
    c.outline()
    return c.img


def hero_tuck():
    c = Canvas(FW, FH)
    cx, cy, r = 10, 16, 8
    for y in range(FH):
        for x in range(FW):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= r:
                col = SHIRT if y > cy - 2 else SKIN
                if d > r - 2 and x > cx:
                    col = shade(col, 0.75)
                c.put(x, y, col)
    c.rect(6, 19, 8, 3, PANTS)
    c.rect(12, 20, 3, 2, SHOE)
    c.rect(7, 10, 3, 2, SKIN_HI)
    c.outline()
    return c.img


def hero_lying():
    """Flat on his back, head to the right (28x20)."""
    return hero_frame(dead=True).transpose(Image.ROTATE_270)


def player_sheet():
    sheet = Image.new("RGBA", (FW * 6, FH * 4), (0, 0, 0, 0))
    walk = [(1, 1), (2, 0), (3, 1), (4, 0)]
    for row, back in ((0, False), (1, True)):
        for i, (phase, bob) in enumerate(walk):
            sheet.paste(hero_frame(back, phase, bob), (i * FW, row * FH))
        sheet.paste(hero_frame(back, 0, 0), (4 * FW, row * FH))
        sheet.paste(hero_frame(back, 0, 1), (5 * FW, row * FH))
    sheet.paste(hero_tuck(), (0, 2 * FH))
    for i, kneel in enumerate((0, 3, 6)):
        sheet.paste(hero_frame(kneel=kneel, dead=True), (i * FW, 3 * FH))
    sheet.paste(hero_lying(), (3 * FW, 3 * FH + FW - 12))
    return sheet


# --------------------------------------------------------------------- props
WOOD = hexc("a0642f")
WOOD_HI = hexc("c98a4b")
WOOD_SH = hexc("6e4020")
WOOD_DK = hexc("4a2a15")
IRON = hexc("5a5a66")
STONE = hexc("8d8a86")
STONE_HI = hexc("b4b0aa")
STONE_SH = hexc("5e5b58")
HAY = hexc("d9b54a")
HAY_SH = hexc("a68530")

# name -> (x, y, w, h) in props.png; filled in by props_sheet()
PROPS = {}


def table_top():
    c = Canvas(34, 26)
    c.rect(1, 1, 32, 16, WOOD)
    for y in range(1, 17, 4):
        c.rect(1, y, 32, 1, WOOD_SH)
    c.rect(1, 1, 32, 1, WOOD_HI)
    c.rect(1, 17, 32, 3, WOOD_SH)  # front edge
    c.rect(3, 20, 3, 5, WOOD_DK)   # legs
    c.rect(28, 20, 3, 5, WOOD_DK)
    c.outline()
    return c.img


def table_flipped_front():
    """Flipped toward the camera / away (we see the table top face-on as a wall)."""
    c = Canvas(34, 22)
    c.rect(1, 1, 32, 18, WOOD)
    for x in range(1, 33, 6):
        c.rect(x, 1, 1, 18, WOOD_SH)
    c.rect(1, 1, 32, 2, WOOD_HI)
    c.rect(1, 17, 32, 2, WOOD_SH)
    c.rect(4, 5, 2, 2, IRON)
    c.rect(28, 5, 2, 2, IRON)
    c.outline()
    return c.img


def table_flipped_back():
    """Flipped away from the camera: we see the underside with the legs sticking toward us."""
    c = Canvas(34, 22)
    c.rect(1, 1, 32, 12, WOOD_SH)
    c.rect(1, 1, 32, 1, WOOD)
    c.rect(1, 13, 32, 3, WOOD_DK)
    c.rect(4, 6, 3, 14, WOOD_DK)   # legs pointing at us
    c.rect(27, 6, 3, 14, WOOD_DK)
    c.rect(4, 6, 1, 14, WOOD)
    c.rect(27, 6, 1, 14, WOOD)
    c.outline()
    return c.img


def table_flipped_side():
    """Flipped to the right (we see its edge); mirror for left."""
    c = Canvas(12, 34)
    c.rect(1, 1, 6, 30, WOOD)
    c.rect(1, 1, 2, 30, WOOD_HI)
    c.rect(6, 1, 1, 30, WOOD_SH)
    c.rect(7, 4, 4, 3, WOOD_DK)   # legs sticking out
    c.rect(7, 25, 4, 3, WOOD_DK)
    c.rect(1, 30, 6, 3, WOOD_SH)
    c.outline()
    return c.img


def barrel():
    c = Canvas(18, 22)
    c.rect(2, 3, 14, 17, WOOD)
    c.rect(1, 5, 16, 13, WOOD)
    for x in (4, 8, 12):
        c.rect(x, 3, 1, 17, WOOD_SH)
    c.rect(1, 6, 16, 2, IRON)
    c.rect(1, 15, 16, 2, IRON)
    c.rect(3, 1, 12, 3, WOOD_HI)  # lid
    c.rect(4, 2, 10, 1, WOOD_SH)
    c.rect(14, 4, 2, 15, WOOD_SH)
    c.outline()
    return c.img


def crate():
    c = Canvas(18, 20)
    c.rect(1, 1, 16, 17, WOOD)
    c.rect(1, 1, 16, 2, WOOD_HI)
    c.rect(1, 16, 16, 2, WOOD_SH)
    c.rect(1, 1, 2, 17, WOOD_SH)
    c.rect(15, 1, 2, 17, WOOD_SH)
    for i in range(14):  # diagonal brace
        c.rect(2 + i, 3 + i, 2, 1, WOOD_DK)
    c.outline()
    return c.img


def hay():
    c = Canvas(26, 20)
    c.rect(1, 2, 24, 16, HAY)
    rnd = random.Random(4)
    for _ in range(40):
        c.put(rnd.randint(1, 24), rnd.randint(2, 17), HAY_SH)
    c.rect(1, 2, 24, 2, shade(HAY, 1.15))
    c.rect(7, 2, 2, 16, WOOD_DK)   # twine
    c.rect(17, 2, 2, 16, WOOD_DK)
    c.rect(1, 16, 24, 2, HAY_SH)
    c.outline()
    return c.img


def pillar():
    c = Canvas(18, 30)
    c.rect(2, 4, 14, 22, STONE)
    c.rect(2, 4, 3, 22, STONE_HI)
    c.rect(13, 4, 3, 22, STONE_SH)
    c.rect(1, 1, 16, 4, STONE_HI)
    c.rect(1, 25, 16, 4, STONE_SH)
    for y in (10, 17):
        c.rect(2, y, 14, 1, STONE_SH)
    c.outline()
    return c.img


# chest tiers like ETG: D brown, C blue, B green, A red (body, trim)
CHEST_TIERS = {
    "d": (hexc("8a5a2b"), hexc("5a5a66")),
    "c": (hexc("3f6fc4"), hexc("c9ccd6")),
    "b": (hexc("3f9e4f"), hexc("d9b54a")),
    "a": (hexc("c0392b"), hexc("ffd166")),
}
HOLE = hexc("1a110e")


def chest(body, trim, opened=False):
    c = Canvas(22, 20)
    hi = shade(body, 1.25)
    sh = shade(body, 0.7)
    # box
    c.rect(1, 9, 20, 10, body)
    c.rect(1, 9, 20, 1, hi)
    c.rect(1, 16, 20, 3, sh)
    c.rect(4, 9, 2, 10, trim)
    c.rect(16, 9, 2, 10, trim)
    if opened:
        c.rect(2, 1, 18, 4, sh)      # lid tipped back
        c.rect(4, 1, 2, 4, trim)
        c.rect(16, 1, 2, 4, trim)
        c.rect(2, 5, 18, 4, HOLE)    # dark inside
    else:
        c.rect(2, 3, 18, 6, body)    # rounded lid
        c.rect(1, 4, 20, 5, body)
        c.rect(2, 3, 18, 1, hi)
        c.rect(1, 8, 20, 1, sh)
        c.rect(4, 3, 2, 6, trim)
        c.rect(16, 3, 2, 6, trim)
        c.rect(9, 6, 4, 5, trim)     # lock with keyhole
        c.rect(10, 8, 2, 2, HOLE)
    c.outline()
    return c.img


def exit_hatch():
    """Open trapdoor with a ladder going down: the way to the next floor."""
    c = Canvas(32, 24)
    c.rect(1, 1, 30, 22, STONE_SH)
    c.rect(1, 1, 30, 2, STONE)
    c.rect(3, 4, 26, 17, HOLE)
    for x in (10, 20):               # ladder rails
        c.rect(x, 4, 2, 17, WOOD_SH)
    for y in range(6, 21, 4):        # rungs
        c.rect(10, y, 12, 1, WOOD)
    c.outline()
    return c.img


def debris():
    c = Canvas(20, 10)
    rnd = random.Random(9)
    for _ in range(6):
        x, y = rnd.randint(1, 14), rnd.randint(1, 7)
        c.rect(x, y, rnd.randint(2, 5), 1, WOOD_SH if rnd.random() < 0.5 else WOOD)
    return c.img


def props_sheet():
    parts = [("table", table_top()), ("table_flip_down", table_flipped_front()),
             ("table_flip_up", table_flipped_back()), ("table_flip_right", table_flipped_side()),
             ("table_flip_left", table_flipped_side().transpose(Image.FLIP_LEFT_RIGHT)),
             ("barrel", barrel()), ("crate", crate()), ("hay", hay()), ("pillar", pillar()),
             ("debris", debris()), ("exit", exit_hatch())]
    for tier, (body, trim) in CHEST_TIERS.items():
        parts.append(("chest_" + tier, chest(body, trim)))
        parts.append(("chest_" + tier + "_open", chest(body, trim, opened=True)))
    w = sum(p.width + 2 for _, p in parts)
    h = max(p.height for _, p in parts)
    sheet = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    x = 0
    for name, p in parts:
        sheet.paste(p, (x, 0))
        PROPS[name] = (x, 0, p.width, p.height)
        x += p.width + 2
    return sheet


# --------------------------------------------------------------------- tiles
# Each theme: floor (kind + colours), wall face (kind + colours) with a window and a decoration
# variant, and the "cap" that fills the wall tops and the room border (thatch, hedge, roof...).
THEMES = {
    # --- stage 1: the village
    "cottage": dict(floor=("planks", "a0703f", "8a5d32", "b8854f"),
                    face=("timber", "f0e2c0", "6e4a2e", "3e2a1c"),
                    cap=("thatch", "c49a45", "8e6a2a", "e0c070")),
    "yard":    dict(floor=("grass", "6aa84a", "568f3a", "82c060"),
                    face=("fence", "f2ead8", "c9bfa8", "3f7a35"),
                    cap=("hedge", "3a6e30", "2a5426", "5a9a45")),
    "barn":    dict(floor=("barnfloor", "8a6038", "6e4a2a", "e0c060"),
                    face=("barn", "b5452f", "8a3020", "f0e8d8"),
                    cap=("shingle", "5a3a26", "3e2818", "7a5236")),
    "orchard": dict(floor=("dirt", "8a6a48", "6e5236", "6fa34a"),
                    face=("stonewall", "9a8f80", "6e665a", "4f8a3a"),
                    cap=("canopy", "4a8a3a", "2f6a2a", "d03a2a")),
    "square":  dict(floor=("cobble", "b3a58a", "948770", "cbbea2"),
                    face=("facade", "e8d8b8", "7a5236", "3e2a1c"),
                    cap=("rooftiles", "b5543a", "8a3a26", "d07050")),
    "chapel":  dict(floor=("flags", "c2b28e", "a39472", "d6c8a6"),
                    face=("chapel", "eeeae0", "b8b0a0", "6a6458"),
                    cap=("slate", "5a6070", "3e4452", "7a8090")),
}
T = 16
GLASS = hexc("8ec5e8")
GLASS_HI = hexc("d8f0ff")
SHUTTER = hexc("4f8a5a")
FLOWERS = [hexc("e84a6a"), hexc("f2e86d"), hexc("f5f5f5"), hexc("e88fb4")]


def floor_tile(kind, cols, seed):
    rnd = random.Random(seed * 7 + len(kind))
    base, dark, light = (hexc(x) for x in cols)
    c = Canvas(T, T)
    c.rect(0, 0, T, T, base)
    if kind in ("planks", "barnfloor"):
        for y in range(0, T, 4):
            c.rect(0, y + 3, T, 1, dark)
            off = rnd.randint(0, 15)
            c.put(off, y + rnd.randint(0, 2), dark)
            c.rect((off + 8) % T, y, 1, 3, dark)
        for _ in range(4):
            c.put(rnd.randrange(T), rnd.randrange(T), shade(base, 1.15))
        if kind == "barnfloor":  # loose hay
            for _ in range({0: 1, 1: 2, 2: 4, 3: 6}[seed]):
                x, y = rnd.randrange(T - 3), rnd.randrange(T)
                c.rect(x, y, rnd.randint(2, 4), 1, light)
    elif kind == "flags":
        c.rect(0, 0, T, 1, dark)
        c.rect(0, 0, 1, T, dark)
        c.rect(0, 8, T, 1, dark)
        c.rect(8 if seed % 2 else 4, 0, 1, 8, dark)
        c.rect(12 if seed % 2 else 10, 8, 1, 8, dark)
        for _ in range(6):
            c.put(rnd.randrange(T), rnd.randrange(T), light)
    elif kind == "grass":
        for _ in range(26):
            x, y = rnd.randrange(T), rnd.randrange(T)
            c.put(x, y, light if rnd.random() < 0.5 else dark)
            c.put(x, y - 1, light)
        for _ in range({0: 0, 1: 1, 2: 3, 3: 5}[seed]):  # flowers on the rarer tiles
            fx, fy = rnd.randint(1, 14), rnd.randint(1, 13)
            c.put(fx, fy, FLOWERS[rnd.randrange(len(FLOWERS))])
            c.put(fx, fy + 1, dark)
    elif kind == "dirt":
        for _ in range(12):
            c.put(rnd.randrange(T), rnd.randrange(T), dark)
        for _ in range(3):
            c.put(rnd.randrange(T), rnd.randrange(T), shade(base, 1.25))
        for _ in range(seed + 1):  # grass tufts
            x, y = rnd.randint(1, 13), rnd.randint(2, 14)
            for dx, dy in ((0, 0), (1, -1), (2, 0)):
                c.put(x + dx, y + dy, light)
        if seed == 3:  # a fallen apple
            c.rect(9, 9, 2, 2, hexc("d03a2a"))
    elif kind == "cobble":
        c.rect(0, 0, T, T, dark)
        for cy in range(0, T, 5):
            sh = rnd.randint(0, 3)
            for cx in range(-sh, T, 6):
                w, h = rnd.randint(4, 5), 4
                c.rect(cx + 1, cy + 1, w - 1, h - 1, base)
                c.put(cx + 1, cy + 1, light)
    return c.img


def window(c, x0, y0, w, h, frame, shutters=True):
    c.rect(x0, y0, w, h, frame)
    c.rect(x0 + 1, y0 + 1, w - 2, h - 2, GLASS)
    c.put(x0 + 2, y0 + 2, GLASS_HI)
    c.put(x0 + 3, y0 + 2, GLASS_HI)
    c.rect(x0 + w // 2, y0 + 1, 1, h - 2, frame)
    c.rect(x0 + 1, y0 + h // 2, w - 2, 1, frame)
    if shutters:
        c.rect(x0 - 2, y0, 2, h, SHUTTER)
        c.rect(x0 + w, y0, 2, h, SHUTTER)


def flower_box(c, x0, y0, w):
    c.rect(x0, y0 + 1, w, 2, hexc("7a5236"))
    for i in range(w):
        if i % 2 == 0:
            c.put(x0 + i, y0, FLOWERS[i % len(FLOWERS)])


def face_tile(kind, cols, variant):
    """variant 0 = plain wall, 1 = with a window (or similar), 2 = with a decoration."""
    a, b, d = (hexc(x) for x in cols)
    c = Canvas(T, T)
    if kind == "timber":  # cottage: plaster between dark timber beams
        c.rect(0, 0, T, T, a)
        c.rect(0, 0, T, 2, b)
        c.rect(0, 0, 2, 13, b)
        c.rect(0, 13, T, 1, b)
        c.rect(0, 14, T, 2, d)
        if variant == 0:
            for i in range(9):  # diagonal brace
                c.rect(3 + i, 11 - i, 2, 1, b)
        elif variant == 1:
            window(c, 6, 3, 7, 7, b)
            flower_box(c, 5, 10, 9)
        else:  # lantern on a bracket
            c.rect(6, 3, 5, 1, d)
            c.rect(9, 4, 1, 2, d)
            c.rect(8, 6, 3, 4, hexc("ffd166"))
            c.rect(8, 6, 3, 1, d)
            c.put(9, 7, hexc("fff3b0"))
    elif kind == "fence":  # yard: white picket fence in front of a hedge
        c.rect(0, 0, T, T, hexc("3f7a35"))
        for _ in range(10):
            x, y = random.randrange(T), random.randrange(10)
            c.put(x, y, hexc("5a9a45"))
        for x in range(0, T, 4):
            c.rect(x + 1, 4, 2, 10, a)
            c.put(x + 1, 3, a)
            c.put(x + 2, 3, a)
            c.put(x + 1, 2, a) if x % 8 == 0 else None
            c.rect(x + 3, 4, 1, 10, b)
        c.rect(0, 6, T, 1, b)
        c.rect(0, 11, T, 1, b)
        c.rect(0, 14, T, 2, hexc("568f3a"))
        if variant == 1:  # sunflower
            c.rect(7, 6, 1, 9, hexc("3a6e30"))
            c.rect(5, 2, 5, 5, hexc("f2c230"))
            c.rect(6, 3, 3, 3, hexc("6e4a2a"))
        elif variant == 2:  # birdhouse on a post
            c.rect(7, 7, 2, 8, hexc("7a5236"))
            c.rect(5, 3, 6, 5, hexc("c0392b"))
            c.rect(4, 2, 8, 1, hexc("6e4a2a"))
            c.rect(7, 5, 2, 2, hexc("2b1d14"))
    elif kind == "barn":  # red boards with white trim
        c.rect(0, 0, T, T, a)
        for x in range(0, T, 4):
            c.rect(x, 0, 1, T, b)
        c.rect(0, 0, T, 2, d)
        c.rect(0, 13, T, 1, d)
        c.rect(0, 14, T, 2, shade(b, 0.7))
        if variant == 1:  # hay-loft opening
            c.rect(4, 3, 8, 9, d)
            c.rect(5, 4, 6, 7, hexc("2b1d14"))
            c.rect(5, 8, 6, 3, hexc("e0c060"))
            c.put(6, 7, hexc("e0c060"))
            c.put(9, 7, hexc("e0c060"))
        elif variant == 2:  # lucky horseshoe
            c.rect(6, 4, 1, 5, hexc("9a9aa6"))
            c.rect(10, 4, 1, 5, hexc("9a9aa6"))
            c.rect(7, 9, 3, 1, hexc("9a9aa6"))
    elif kind == "stonewall":  # orchard: rounded fieldstones, ivy
        c.rect(0, 0, T, T, b)
        rnd = random.Random(5)
        for y in range(0, 14, 4):
            sh = rnd.randint(0, 3)
            for x in range(-sh, T, 5):
                col = shade(a, rnd.uniform(0.85, 1.12))
                c.rect(x + 1, y + 1, 4, 3, col)
                c.put(x + 1, y + 1, shade(col, 1.2))
        c.rect(0, 14, T, 2, shade(b, 0.7))
        ivy = hexc(cols[2])
        rnd = random.Random(11 + variant)
        for _ in range(4 if variant == 0 else 14):
            x = rnd.randrange(T)
            for y in range(rnd.randint(2, 9)):
                c.put(x + (y % 2), y, ivy if (x + y) % 3 else shade(ivy, 1.3))
        if variant == 2:  # flowers in the ivy
            for x, y in ((4, 5), (11, 3), (8, 8)):
                c.put(x, y, FLOWERS[(x + y) % 4])
    elif kind == "facade":  # village square: house fronts
        c.rect(0, 0, T, T, a)
        c.rect(0, 0, T, 1, b)
        c.rect(0, 0, 1, T, b)
        c.rect(0, 13, T, 1, b)
        c.rect(0, 14, T, 2, d)
        if variant == 1:
            window(c, 5, 3, 7, 7, b)
            flower_box(c, 4, 10, 9)
        elif variant == 2:  # hanging shop sign
            c.rect(3, 2, 10, 1, d)
            c.rect(5, 3, 1, 2, d)
            c.rect(10, 3, 1, 2, d)
            c.rect(4, 5, 8, 5, hexc("a0642f"))
            c.rect(6, 7, 4, 1, hexc("f2e86d"))
        else:
            window(c, 5, 3, 7, 7, b, shutters=False)
    elif kind == "chapel":
        c.rect(0, 0, T, T, a)
        c.rect(0, 12, T, 2, b)
        c.rect(0, 14, T, 2, d)
        if variant == 1:  # stained glass
            c.rect(5, 2, 6, 10, d)
            panes = [hexc("e84a6a"), hexc("4f8ad8"), hexc("f2c230"), hexc("5ab05a")]
            for i, (x, y) in enumerate(((6, 4), (8, 4), (6, 7), (8, 7), (6, 10), (8, 10))):
                c.rect(x, y - 1, 2, 2, panes[i % 4])
            c.rect(6, 2, 4, 1, panes[2])
        elif variant == 2:  # candle
            c.rect(7, 7, 2, 4, hexc("f5f0e6"))
            c.rect(7, 5, 2, 2, hexc("ffd166"))
            c.rect(5, 11, 6, 1, d)
    return c.img


def cap_tile(kind, cols):
    a, b, l = (hexc(x) for x in cols)
    c = Canvas(T, T)
    c.rect(0, 0, T, T, a)
    rnd = random.Random(len(kind))
    if kind == "thatch":
        for _ in range(40):
            x, y = rnd.randrange(T), rnd.randrange(T)
            c.rect(x, y, 1, rnd.randint(2, 4), b if rnd.random() < 0.6 else l)
    elif kind in ("hedge", "canopy"):
        for _ in range(9):
            x, y = rnd.randrange(-2, T), rnd.randrange(-2, T)
            c.rect(x, y, 4, 4, shade(a, 1.15))
            c.rect(x + 1, y + 1, 2, 2, shade(a, 1.3))
            c.put(x + 3, y + 3, b)
        if kind == "canopy":
            for _ in range(3):
                c.rect(rnd.randrange(T - 2), rnd.randrange(T - 2), 2, 2, l)
    elif kind == "shingle":
        for y in range(0, T, 4):
            sh = 0 if (y // 4) % 2 == 0 else 3
            c.rect(0, y + 3, T, 1, b)
            for x in range(sh, T, 6):
                c.rect(x, y, 1, 3, b)
                c.put(x + 1, y, l)
    elif kind in ("rooftiles", "slate"):
        for y in range(0, T, 4):
            sh = 0 if (y // 4) % 2 == 0 else 2
            for x in range(-sh, T, 4):
                c.rect(x, y + 2, 4, 2, b)
                c.put(x + 1, y, l)
                c.put(x, y + 3, shade(b, 0.8))
    return c.img


def door_frame(col):
    c = Canvas(T, T)
    c.rect(0, 0, T, T, hexc("1a1210"))
    c.rect(0, 0, 3, T, hexc(col))
    c.rect(T - 3, 0, 3, T, hexc(col))
    return c.img


def unused_tile():
    return Canvas(T, T).img


def tileset(name, th):
    fk, *fc = th["floor"]
    wk, *wc = th["face"]
    ck, *cc = th["cap"]
    random.seed(sum(map(ord, name)))  # stable between runs (str hash() is randomised)
    tiles = [floor_tile(fk, fc, s) for s in range(4)]
    # 4 face, 5 cap, 6 door frame, 7 (unused, was iron bars), 8 face + window, 9 face + decoration
    tiles += [face_tile(wk, wc, 0), cap_tile(ck, cc), door_frame(cc[1]), unused_tile(),
              face_tile(wk, wc, 1), face_tile(wk, wc, 2)]
    sheet = Image.new("RGBA", (T * len(tiles), T), (0, 0, 0, 0))
    for i, t in enumerate(tiles):
        sheet.paste(t, (i * T, 0))
    return sheet


def main():
    os.makedirs(os.path.join(ROOT, "sprites"), exist_ok=True)
    os.makedirs(os.path.join(ROOT, "tiles"), exist_ok=True)
    player_sheet().save(os.path.join(ROOT, "sprites", "player.png"))
    props_sheet().save(os.path.join(ROOT, "sprites", "props.png"))
    with open(os.path.join(ROOT, "sprites", "props.txt"), "w") as f:  # atlas read by Sprites.java
        for k, (x, y, w, h) in PROPS.items():
            f.write(f"{k} {x} {y} {w} {h}\n")
    for name, th in THEMES.items():
        tileset(name, th).save(os.path.join(ROOT, "tiles", name + ".png"))
    print("props atlas:")
    for k, v in PROPS.items():
        print(f"  {k}: {v}")


if __name__ == "__main__":
    main()
