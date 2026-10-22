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
                       broken-plank debris - see PROPS below for the atlas rects
  tiles/<theme>.png    room tileset per theme, 16x16 tiles in one row:
                       floor a, floor b, floor c, floor d, wall face, wall top, door frame, door bars
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
             ("debris", debris())]
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
THEMES = {
    # floor kind, floor colours, wall colours
    "house":   dict(floor="planks", f=("9c6b3f", "86592f", "b07d4c"), wall=("c9b79a", "a8957a", "6b5a48"), top="3d2b22"),
    "chapel":  dict(floor="flags",  f=("8e8a84", "7a766f", "a19c95"), wall=("9a9fa8", "7c818a", "50545c"), top="2c2f36"),
    "yard":    dict(floor="grass",  f=("5e8f3e", "4f7c33", "6fa34a"), wall=("8a6a45", "6e5236", "4a3622"), top="2f4a24"),
    "cellar":  dict(floor="bricks", f=("6e4e44", "5c4038", "805c50"), wall=("5a4a44", "483a35", "2e2522"), top="1c1614"),
    "square":  dict(floor="cobble", f=("a39a8a", "8c8374", "b8af9e"), wall=("b5a58a", "958670", "5f5545"), top="3a342a"),
}
T = 16


def floor_tile(kind, cols, seed):
    rnd = random.Random(seed)
    base, dark, light = (hexc(x) for x in cols)
    c = Canvas(T, T)
    c.rect(0, 0, T, T, base)
    if kind == "planks":
        for y in range(0, T, 4):
            c.rect(0, y + 3, T, 1, dark)
            off = rnd.randint(0, 15)
            c.put(off, y + rnd.randint(0, 2), dark)
            c.rect((off + 8) % T, y, 1, 3, dark)
        for _ in range(4):
            c.put(rnd.randrange(T), rnd.randrange(T), light)
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
        if seed % 4 == 3:  # a few flowers
            for fx, fy, col in ((3, 4, "f2e86d"), (11, 9, "f5f5f5"), (6, 12, "e88fb4")):
                c.put(fx, fy, hexc(col))
                c.put(fx, fy + 1, dark)
    elif kind == "bricks":
        for y in range(0, T, 4):
            c.rect(0, y, T, 1, dark)
            sh = 0 if (y // 4) % 2 == 0 else 4
            for x in range(sh, T, 8):
                c.rect(x, y, 1, 4, dark)
        for _ in range(5):
            c.put(rnd.randrange(T), rnd.randrange(T), light)
    elif kind == "cobble":
        c.rect(0, 0, T, T, dark)
        for cy in range(0, T, 5):
            sh = rnd.randint(0, 3)
            for cx in range(-sh, T, 6):
                w, h = rnd.randint(4, 5), 4
                c.rect(cx + 1, cy + 1, w - 1, h - 1, base)
                c.put(cx + 1, cy + 1, light)
    return c.img


def wall_face(cols, seed):
    base, dark, deep = (hexc(x) for x in cols)
    c = Canvas(T, T)
    c.rect(0, 0, T, T, base)
    for y in range(0, T, 4):
        c.rect(0, y, T, 1, dark)
        sh = 0 if (y // 4) % 2 == 0 else 5
        for x in range(sh, T, 10):
            c.rect(x, y, 1, 4, dark)
    c.rect(0, T - 2, T, 2, deep)  # baseboard shadow
    return c.img


def solid_tile(col):
    c = Canvas(T, T)
    c.rect(0, 0, T, T, hexc(col))
    return c.img


def door_frame(cols):
    c = Canvas(T, T)
    c.rect(0, 0, T, T, hexc("1a1210"))
    c.rect(0, 0, 3, T, hexc(cols[2]))
    c.rect(T - 3, 0, 3, T, hexc(cols[2]))
    return c.img


def door_bars():
    c = Canvas(T, T)
    for x in range(1, T, 4):
        c.rect(x, 0, 2, T, IRON)
        c.rect(x, 0, 1, T, hexc("8a8a96"))
    c.rect(0, 3, T, 2, hexc("3a3a44"))
    c.rect(0, 11, T, 2, hexc("3a3a44"))
    return c.img


def tileset(name, th):
    tiles = [floor_tile(th["floor"], th["f"], s) for s in range(4)]
    tiles += [wall_face(th["wall"], 0), solid_tile(th["top"]), door_frame(th["wall"]), door_bars()]
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
