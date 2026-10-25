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
  sprites/guns.png     one sprite per gun pointing right + guns.txt (name x y w h gripX gripY)
  sprites/items.png    16x16 item icons + items.txt (name x y w h)
  sprites/props.png    tables (normal + flipped 4 ways), barrel, crate, hay bale, stone pillar,
                       broken-plank debris, chests per tier (chest_d/c/b/a + _open) and the
                       floor exit hatch - see PROPS below for the atlas rects
  tiles/<theme>.png    room tileset per theme, 16x16 tiles in one row:
                       floor a, floor b, floor c, floor d, wall face, cap (wall top / border),
                       door frame, (unused), wall face with window, wall face with decoration
"""
import math
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


def exit_gate():
    """The village's way out: a wooden gate in a stretch of fence, swung open onto the road, 40x36."""
    c = Canvas(40, 36)
    for x in (2, 34):                         # gate posts with little roofs
        c.rect(x, 6, 4, 29, WOOD_SH)
        c.rect(x, 6, 1, 29, WOOD)
        c.rect(x - 1, 4, 6, 2, WOOD_DK)
    c.rect(1, 2, 38, 3, hexc("8a4a2a"))       # the crossbeam roof over the gate
    c.rect(1, 2, 38, 1, hexc("b0603a"))
    c.rect(6, 8, 28, 26, hexc("c9b48a"))      # the dirt road beyond
    c.rect(6, 8, 28, 4, hexc("8fbf6a"))       # far-off fields
    c.rect(16, 12, 8, 22, hexc("b09a70"))     # the road narrowing away
    c.rect(18, 12, 4, 6, hexc("a08a62"))
    for y in (14, 22, 30):                    # the gate wing, swung open to the left
        c.rect(6, y, 7, 2, WOOD)
        c.rect(6, y, 7, 1, WOOD_HI)
    c.rect(6, 13, 2, 20, WOOD_SH)
    c.outline()
    return c.img


def exit_bus():
    """The town's way out: a bus stop - a shelter with a bench and a 'CITY' sign, 40x36."""
    c = Canvas(40, 36)
    glass = hexc("8ec5e8", 200)
    c.rect(2, 4, 26, 3, hexc("2f6f9a"))       # shelter roof
    c.rect(2, 4, 26, 1, hexc("4f9fca"))
    c.rect(3, 7, 2, 26, METAL_M)              # posts
    c.rect(25, 7, 2, 26, METAL_M)
    c.rect(5, 8, 20, 14, glass)               # glass back panel
    c.rect(6, 9, 3, 6, hexc("cfe8f5", 220))
    c.rect(6, 24, 18, 3, WOOD)                # bench
    c.rect(6, 24, 18, 1, WOOD_HI)
    c.rect(7, 27, 2, 5, METAL_D)
    c.rect(21, 27, 2, 5, METAL_D)
    c.rect(32, 6, 2, 28, METAL_D)             # the bus stop sign
    c.rect(29, 3, 8, 8, hexc("2f9e5b"))
    c.rect(30, 4, 6, 6, hexc("f5f0e6"))
    c.rect(31, 5, 4, 2, hexc("2f9e5b"))       # a little bus on it
    c.rect(31, 7, 1, 1, hexc("1e1e22"))
    c.rect(34, 7, 1, 1, hexc("1e1e22"))
    c.outline()
    return c.img


def exit_doors():
    """The city's way out: the skyscraper's glass doors, lit from inside, 40x40."""
    c = Canvas(40, 40)
    c.rect(1, 1, 38, 38, hexc("3a3f4f"))      # the tower's base
    c.rect(1, 1, 38, 3, hexc("5a6278"))
    for x in (4, 14, 26):                     # dark windows above
        c.rect(x, 5, 9, 6, hexc("1e2230"))
        c.rect(x + 1, 6, 3, 2, hexc("ffd166"))
    c.rect(6, 14, 28, 24, hexc("ffe7a0"))     # the warm lobby light
    c.rect(19, 14, 2, 24, METAL_D)            # door frames
    c.rect(6, 14, 28, 2, METAL_D)
    c.rect(6, 14, 2, 24, METAL_D)
    c.rect(32, 14, 2, 24, METAL_D)
    c.rect(9, 17, 3, 12, hexc("fff6d6"))      # glints on the glass
    c.rect(23, 17, 3, 12, hexc("fff6d6"))
    c.rect(16, 25, 2, 4, METAL_L)             # handles
    c.rect(22, 25, 2, 4, METAL_L)
    c.rect(4, 37, 32, 2, hexc("c9b48a"))      # doormat
    c.outline()
    return c.img


def exit_heli():
    """The skyscraper's way out: a little helicopter waiting on the roof, 64x44."""
    c = Canvas(64, 44)
    body = hexc("e8b33a")
    c.rect(6, 4, 52, 2, METAL_D)              # rotor blades
    c.rect(30, 6, 4, 5, METAL_M)              # rotor mast
    c.rect(16, 11, 30, 18, body)              # cabin
    c.rect(12, 15, 6, 12, body)
    c.rect(16, 11, 30, 2, shade(body, 1.2))
    c.rect(13, 14, 14, 9, hexc("8ec5e8"))     # big front window
    c.rect(14, 15, 4, 4, hexc("cfe8f5"))
    c.rect(30, 15, 8, 7, hexc("8ec5e8"))      # side window
    c.rect(46, 17, 14, 4, body)               # tail boom
    c.rect(57, 11, 3, 10, shade(body, 0.8))   # tail fin
    c.rect(55, 13, 7, 1, METAL_D)             # tail rotor
    c.rect(16, 33, 32, 2, METAL_D)            # skids
    c.rect(20, 29, 2, 4, METAL_D)
    c.rect(40, 29, 2, 4, METAL_D)
    c.outline()
    return c.img


def bench():
    """Park bench (town's stand-in for hay bales), 26x16."""
    c = Canvas(26, 16)
    for y in (2, 5):  # backrest slats
        c.rect(1, y, 24, 2, WOOD)
        c.rect(1, y, 24, 1, WOOD_HI)
    for y in (9, 11):  # seat
        c.rect(1, y, 24, 2, WOOD)
        c.rect(1, y + 1, 24, 1, WOOD_SH)
    for x in (2, 21):  # iron legs and arms
        c.rect(x, 1, 3, 14, hexc("3a3a44"))
        c.rect(x, 1, 1, 14, hexc("5a5a66"))
    c.outline()
    return c.img


def lamp():
    """Street lamp (town's stand-in for stone pillars), 12x40."""
    c = Canvas(12, 40)
    c.rect(4, 10, 4, 28, hexc("2a2a30"))
    c.rect(4, 10, 1, 28, hexc("4a4a54"))
    c.rect(3, 34, 6, 4, hexc("2a2a30"))   # base
    c.rect(2, 2, 8, 8, hexc("2a2a30"))    # lantern
    c.rect(3, 3, 6, 6, hexc("ffd166"))
    c.rect(4, 4, 2, 2, hexc("fff3b0"))
    c.rect(5, 0, 2, 2, hexc("2a2a30"))
    c.outline()
    return c.img


def pit():
    """Open sewer hatch in the town's floor - fall in and you lose half a heart (32x32 = 1x1 unit)."""
    c = Canvas(32, 32)
    rim = hexc("4a4a54")
    c.rect(2, 2, 28, 28, rim)
    c.rect(2, 2, 28, 2, hexc("6a6a76"))
    c.rect(5, 5, 22, 22, hexc("0e0a0c"))     # the hole
    c.rect(5, 5, 22, 4, hexc("2a2428"))      # far inner wall, lit from above
    c.rect(5, 9, 22, 3, hexc("1a1418"))
    c.rect(9, 22, 8, 1, hexc("2f4a3a"))      # glint of sewer water far below
    c.rect(17, 24, 5, 1, hexc("2f4a3a"))
    for x, y in ((3, 3), (27, 3), (3, 27), (27, 27)):  # rivets
        c.rect(x, y, 2, 2, hexc("8a8a96"))
    c.outline()
    return c.img


def car():
    """Parked car seen from the side (the city's stand-in for hay bales), 30x18."""
    c = Canvas(30, 18)
    body = hexc("4f6fa8")
    c.rect(2, 6, 26, 7, body)                # body
    c.rect(7, 2, 15, 5, body)                # cabin
    c.rect(9, 3, 5, 3, hexc("8ec5e8"))       # windows
    c.rect(16, 3, 5, 3, hexc("8ec5e8"))
    c.rect(2, 6, 26, 1, shade(body, 1.25))
    c.rect(26, 8, 2, 2, hexc("ffd166"))      # headlight
    c.rect(2, 8, 2, 2, hexc("ff4a3a"))       # tail light
    for x in (6, 21):                        # wheels
        c.rect(x, 11, 5, 5, hexc("1e1e22"))
        c.rect(x + 2, 13, 1, 1, hexc("9a9aa6"))
    c.outline()
    return c.img


def pedestal():
    """The Pigeon King's statue pedestal: a stone block with a brass plaque (and, well, pigeon marks), 26x24."""
    c = Canvas(26, 24)
    c.rect(1, 2, 24, 4, STONE_HI)           # top slab
    c.rect(3, 6, 20, 14, STONE)              # block
    c.rect(3, 6, 4, 14, STONE_HI)
    c.rect(19, 6, 4, 14, STONE_SH)
    c.rect(1, 20, 24, 3, STONE_SH)           # base
    c.rect(9, 10, 8, 5, hexc("c9a040"))      # plaque
    c.rect(10, 12, 6, 1, hexc("7a5a20"))
    for x, y in ((6, 3), (17, 2), (12, 7), (20, 9)):
        c.put(x, y, hexc("f5f5f5"))
    c.outline()
    return c.img


def desk():
    """Office desk with a computer (the skyscraper's stand-in for hay bales), 30x22."""
    c = Canvas(30, 22)
    top = hexc("c9a070")
    c.rect(1, 8, 28, 6, top)                 # desktop
    c.rect(1, 8, 28, 1, shade(top, 1.15))
    c.rect(1, 14, 28, 6, shade(top, 0.7))    # front panel
    c.rect(3, 15, 7, 4, shade(top, 0.6))     # drawers
    c.rect(5, 16, 3, 1, hexc("d9b24c"))
    c.rect(14, 1, 10, 7, hexc("2a2e38"))     # monitor
    c.rect(15, 2, 8, 5, hexc("4fa4ff"))
    c.rect(16, 3, 3, 1, hexc("d8f0ff"))
    c.rect(18, 7, 2, 1, hexc("2a2e38"))
    c.rect(6, 10, 6, 2, hexc("f5f5f5"))      # papers
    c.rect(24, 9, 3, 3, hexc("e84a3a"))      # a mug
    c.outline()
    return c.img


def plant():
    """Potted office plant (the skyscraper's stand-in for pillars), 16x30."""
    c = Canvas(16, 30)
    leaf = hexc("3a8a4a")
    for x, y, h in ((7, 2, 14), (3, 6, 10), (11, 5, 11), (5, 1, 9), (10, 1, 8)):
        c.rect(x, y, 3, h, leaf)
        c.put(x + 1, y, shade(leaf, 1.4))
    c.rect(3, 17, 10, 11, hexc("e0e0e0"))    # white pot
    c.rect(3, 17, 10, 2, hexc("f5f5f5"))
    c.rect(11, 19, 2, 9, hexc("b8b8b8"))
    c.outline()
    return c.img


def cooler():
    """Water cooler (the skyscraper's barrel), 14x30."""
    c = Canvas(14, 30)
    c.rect(3, 1, 8, 10, hexc("8ed8ff"))      # the big bottle
    c.rect(4, 2, 2, 7, hexc("d8f0ff"))
    c.rect(5, 0, 4, 1, hexc("4fa4ff"))
    c.rect(2, 11, 10, 17, hexc("e8e8ec"))    # the stand
    c.rect(2, 11, 10, 1, hexc("ffffff"))
    c.rect(10, 12, 2, 16, hexc("c0c0c8"))
    c.rect(4, 15, 2, 2, hexc("4f8ad8"))      # taps
    c.rect(8, 15, 2, 2, hexc("e84a3a"))
    c.outline()
    return c.img


# --- stage 5: the clouds
CLOUD = hexc("fbfaff")
CLOUD_SH = hexc("cfd6ee")
CLOUD_DK = hexc("a8b2d6")
GOLD = hexc("e8c040")
GOLD_HI = hexc("fff0a0")
GOLD_SH = hexc("a8841c")


def disc(c, cx, cy, r, col):
    for y in range(int(cy - r) - 1, int(cy + r) + 2):
        for x in range(int(cx - r) - 1, int(cx + r) + 2):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                c.put(x, y, col)


def cloud_puffs(c, puffs, body=CLOUD, sh=CLOUD_SH, dk=CLOUD_DK):
    """Overlapping round puffs: a darker underside first, then the bright tops."""
    for x, y, r in puffs:
        disc(c, x + 0.5, y + 1.5, r, dk)
    for x, y, r in puffs:
        disc(c, x, y + 0.6, r, sh)
    for x, y, r in puffs:
        disc(c, x - 0.4, y - 0.2, r - 1, body)


def cloud_bank():
    """A solid lump of cloud (the clouds' stand-in for hay bales), 28x18."""
    c = Canvas(28, 18)
    cloud_puffs(c, [(6, 10, 5), (13, 7, 6.5), (21, 9, 5.5), (10, 12, 4.5), (18, 12, 4.5), (24, 12, 3.5)])
    c.rect(4, 7, 3, 1, hexc("ffffff"))
    c.rect(11, 3, 4, 1, hexc("ffffff"))
    c.outline(hexc("6a74a0"))
    return c.img


def column():
    """A white marble column with a gold capital (the clouds' pillar), 16x40. Unbreakable."""
    c = Canvas(16, 40)
    marble, hi, sh = hexc("f2eee6"), hexc("ffffff"), hexc("c8c0b0")
    c.rect(3, 6, 10, 28, marble)
    for x in (5, 8, 11):                       # flutes
        c.rect(x, 7, 1, 26, sh)
    c.rect(3, 6, 1, 28, hi)
    c.rect(1, 2, 14, 2, GOLD)                  # capital
    c.rect(2, 4, 12, 2, GOLD_SH)
    c.rect(1, 2, 14, 1, GOLD_HI)
    c.put(1, 4, GOLD)
    c.put(14, 4, GOLD)
    c.rect(2, 34, 12, 2, marble)               # base
    c.rect(1, 36, 14, 3, sh)
    c.outline()
    return c.img


def urn():
    """A golden amphora (the clouds' barrel), 14x22."""
    c = Canvas(14, 22)
    c.rect(4, 1, 6, 2, GOLD_SH)                # lip
    c.rect(5, 3, 4, 3, GOLD)                   # neck
    c.rect(2, 6, 10, 10, GOLD)                 # belly
    c.rect(1, 8, 12, 6, GOLD)
    c.rect(3, 16, 8, 2, GOLD_SH)
    c.rect(4, 18, 6, 3, GOLD)                  # foot
    c.rect(3, 7, 2, 6, GOLD_HI)
    c.rect(1, 10, 12, 1, hexc("e86aa8"))       # a pink band
    c.rect(10, 7, 2, 8, GOLD_SH)
    c.outline()
    return c.img


def gift():
    """A present wrapped in pink with a gold ribbon (the clouds' crate), 16x18."""
    c = Canvas(16, 18)
    pink = hexc("ff8ac8")
    c.rect(1, 5, 14, 12, pink)
    c.rect(1, 5, 14, 2, shade(pink, 1.12))
    c.rect(12, 7, 3, 10, shade(pink, 0.8))
    c.rect(7, 5, 2, 12, GOLD)                  # ribbon
    c.rect(1, 10, 14, 2, GOLD)
    c.rect(4, 2, 4, 3, GOLD)                   # bow
    c.rect(8, 2, 4, 3, GOLD)
    c.rect(7, 3, 2, 2, GOLD_SH)
    c.outline()
    return c.img


def pit_sky():
    """A hole in the clouds: fall through and you lose half a heart (32x32 = 1x1 unit)."""
    c = Canvas(32, 32)
    disc(c, 16, 16, 13.5, CLOUD_SH)            # the cloud thins out toward the hole
    disc(c, 16, 16.5, 11, hexc("5aa8e0"))      # sky far below
    disc(c, 16, 17.5, 9, hexc("7ec8f0"))
    c.rect(9, 19, 6, 3, hexc("6aa84a"))        # fields and a lake, miles down
    c.rect(17, 21, 5, 2, hexc("8ac868"))
    c.rect(14, 14, 4, 2, hexc("4a8ad0"))
    c.rect(19, 15, 3, 1, hexc("ffffff"))       # a wisp of cloud under you
    for a in range(0, 360, 30):                # a ragged rim of puffs
        x = 16 + math.cos(math.radians(a)) * 12.5
        y = 16 + math.sin(math.radians(a)) * 12.5
        disc(c, x, y, 3.2, CLOUD if a > 180 or a == 0 else CLOUD_SH)
    return c.img


def pit_sea():
    """The secret hole in the clouds: the sea far below, waves glinting, a shark fin cutting through (32x32)."""
    c = Canvas(32, 32)
    disc(c, 16, 16, 13.5, CLOUD_SH)
    disc(c, 16, 16.5, 11, hexc("1a5a8a"))      # deep blue sea far below
    disc(c, 16, 17.5, 9, hexc("2a7ab0"))
    for x, y in ((10, 14), (17, 12), (20, 19), (12, 21)):   # white wave crests
        c.rect(x, y, 3, 1, hexc("d8f4ff"))
        c.put(x + 1, y - 1, hexc("ffffff"))
    fin = hexc("4a5a74")                       # a little shark fin (a hint, for those who know)
    for i in range(4):
        c.rect(15 + i, 17 - i + (1 if i == 3 else 0), 1, i + 1 - (1 if i == 3 else 0), fin)
    c.rect(14, 18, 6, 1, hexc("bfefff"))       # the wake
    for a in range(0, 360, 30):
        x = 16 + math.cos(math.radians(a)) * 12.5
        y = 16 + math.sin(math.radians(a)) * 12.5
        disc(c, x, y, 3.2, CLOUD if a > 180 or a == 0 else CLOUD_SH)
    return c.img


def coral():
    """A lump of coral reef (the sea's hay bale), 28x20: orange branches, pink brain coral, a sea fan."""
    c = Canvas(28, 20)
    rock, rock_sh = hexc("6a6a7a"), hexc("4a4a5a")
    disc(c, 14, 15, 9, rock_sh)
    disc(c, 13.5, 14, 8, rock)
    for x, h in ((4, 8), (7, 11), (10, 7)):     # orange branching coral
        c.rect(x, 15 - h, 2, h, hexc("ff8a4a"))
        c.rect(x - 2, 14 - h // 2, 2, 1, hexc("ff8a4a"))
        c.put(x, 14 - h, hexc("ffc08a"))
    disc(c, 17, 10, 4, hexc("ff7aa8"))          # brain coral
    for y in (8, 10, 12):
        c.rect(15, y, 4, 1, hexc("d85a88"))
    for i in range(5):                          # a purple sea fan
        c.rect(21 + i, 4 + abs(2 - i), 1, 10 - abs(2 - i), hexc("a87ae8"))
    c.outline(hexc("1e2a3a"))
    return c.img


def rock_spire():
    """A tall rock spire with a starfish on it (the sea's unbreakable pillar), 16x34."""
    c = Canvas(16, 34)
    rock, hi, sh = hexc("5a6878"), hexc("7a8a9a"), hexc("3a4656")
    for y in range(3, 32):
        half = 3 + (y - 3) * 0.14
        c.rect(int(8 - half), y, int(half * 2), 1, rock)
    c.rect(5, 2, 5, 2, rock)
    c.rect(5, 4, 2, 24, hi)
    c.rect(10, 12, 2, 18, sh)
    c.rect(2, 30, 12, 3, sh)
    star = hexc("ff8a5a")                       # a starfish clinging on
    c.rect(7, 14, 3, 1, star)
    c.rect(8, 13, 1, 3, star)
    c.put(6, 16, star)
    c.put(10, 16, star)
    c.outline(hexc("1e2a3a"))
    return c.img


def seaweed():
    """A clump of seaweed swaying up from a rock (the sea's barrel), 16x26."""
    c = Canvas(16, 26)
    dark, mid, light = hexc("2e6a3a"), hexc("4a9a4a"), hexc("8ad07a")
    for x0, h, col in ((3, 20, dark), (7, 24, mid), (11, 18, dark), (5, 14, mid), (10, 22, mid)):
        for y in range(25 - h, 23):
            x = x0 + (1 if ((y + x0) // 4) % 2 else 0)
            c.rect(x, y, 2, 1, col)
            if y % 5 == 0:
                c.put(x + 2, y, light)          # a little leaf
    c.rect(4, 21, 8, 4, hexc("6a6a7a"))      # the rock it grows on
    c.rect(5, 21, 3, 1, hexc("8a8a9a"))
    c.outline(hexc("1e2a3a"))
    return c.img


def clam():
    """A giant clam, half open, a pearl inside (the sea's crate), 18x16."""
    c = Canvas(18, 16)
    shell, ridge, inner = hexc("c89ad8"), hexc("9a6ab0"), hexc("ffd8e8")
    for y in range(2, 8):                      # the top shell, lifted open
        half = 7 - abs(y - 4) * 0.6
        c.rect(int(9 - half), y, int(half * 2), 1, shell)
    for x in (4, 7, 10, 13):
        c.rect(x, 3, 1, 4, ridge)
    c.rect(3, 8, 12, 2, inner)                 # inside, and the pearl
    disc(c, 9, 8.5, 1.6, hexc("f8f8ff"))
    c.put(8, 8, hexc("ffffff"))
    for y in range(10, 15):                    # the bottom shell
        half = 8 - (y - 10) * 0.9
        c.rect(int(9 - half), y, int(half * 2), 1, shell)
    for x in (4, 7, 10, 13):
        c.rect(x, 11, 1, 3, ridge)
    c.outline(hexc("1e2a3a"))
    return c.img


def exit_spout():
    """The sea's way back up: a roaring waterspout from the sea bed to the clouds, 32x52."""
    c = Canvas(32, 52)
    water, light, foam = hexc("4ab0e0"), hexc("9ae0ff"), hexc("ffffff")
    for y in range(0, 46):
        half = 4 + abs(y - 30) * 0.18 + (6 if y < 8 else 0) * (1 - y / 8)
        x0 = 16 + math.sin(y * 0.35) * 1.5
        for x in range(int(x0 - half), int(x0 + half) + 1):
            c.put(x, y, water if (x + y) % 5 else light)
        c.put(int(x0 - half / 2), y, light)
    disc(c, 16, 46, 9, hexc("2a7ab0"))          # the swirl at its foot
    disc(c, 16, 46, 6, water)
    for x, y in ((8, 44), (24, 45), (12, 49), (21, 49), (4, 47), (28, 47)):
        disc(c, x, y, 1.6, foam)
    for x, y in ((9, 3), (22, 2), (6, 8), (26, 9), (14, 1)):   # spray at the top
        c.put(x, y, foam)
    c.outline(hexc("1a4a6e"))
    return c.img


def exit_rainbow():
    """The clouds' way out (the end of the run): a rainbow arching down to the ground, 48x34."""
    c = Canvas(48, 34)
    bands = ["e84a4a", "f29a3a", "f2e05a", "6ac85a", "4a9ae8", "8a6ad8"]
    for i, col in enumerate(bands):
        r_out = 22 - i * 2
        for y in range(0, 30):
            for x in range(0, 48):
                d2 = (x - 24) ** 2 + (y - 26) ** 2
                if (r_out - 2) ** 2 < d2 <= r_out ** 2 and y <= 26:
                    c.put(x, y, hexc(col))
    cloud_puffs(c, [(5, 27, 4.5), (10, 29, 4), (2, 30, 3)])
    cloud_puffs(c, [(43, 27, 4.5), (38, 29, 4), (46, 30, 3)])
    c.outline(hexc("6a74a0"))
    return c.img


# ---------------------------------------------------------------------- the shop
COIN_GOLD = hexc("f2c230")
COIN_HI = hexc("fff0a0")
COIN_SH = hexc("b88a1c")
COIN_EDGE = hexc("7a5a10")
POM = hexc("ff8ac8")


def coin(frame):
    """A spinning gold coin with a pom-pom stamped on it, 10x10; frames 0-3 turn it edge-on and back."""
    c = Canvas(10, 10)
    half = [4.5, 3.0, 1.0, 3.0][frame]           # half the visible width as it turns
    for y in range(10):
        for x in range(10):
            dx = (x + 0.5 - 5) / max(half, 0.6)
            dy = (y + 0.5 - 5) / 4.5
            if dx * dx + dy * dy <= 1:
                c.put(x, y, COIN_GOLD if frame != 2 else COIN_SH)
    if frame == 0:                                # face-on: rim shading and the pom-pom stamp
        c.rect(3, 4, 4, 2, POM)
        c.rect(4, 3, 2, 4, POM)
        c.put(4, 4, hexc("ffc0e0"))
        c.rect(2, 1, 2, 1, COIN_HI)
        c.put(1, 2, COIN_HI)
        c.rect(6, 8, 2, 1, COIN_SH)
        c.put(8, 7, COIN_SH)
    elif frame in (1, 3):
        c.rect(5 if frame == 1 else 4, 3, 1, 4, POM)
        c.put(3 if frame == 1 else 6, 2, COIN_HI)
    c.outline(COIN_EDGE)
    return c.img


def coin_big():
    """A big coin worth five, 14x14 (bosses and chests spill these)."""
    c = Canvas(14, 14)
    disc(c, 7, 7, 6.2, COIN_SH)
    disc(c, 6.7, 6.7, 5.6, COIN_GOLD)
    disc(c, 7, 7, 3.6, shade(COIN_GOLD, 0.9))
    disc(c, 7, 7, 2.6, POM)                        # the pom-pom stamp
    c.put(6, 6, hexc("ffc0e0"))
    c.rect(3, 2, 3, 1, COIN_HI)
    c.rect(2, 3, 1, 2, COIN_HI)
    c.outline(COIN_EDGE)
    return c.img


def counter(sky=False):
    """Baa-llo's shop counter, 64x30: a wooden top, a panelled front with a gold coin on it, a service bell.
    In the clouds it's white marble wrapped in a cloud."""
    c = Canvas(64, 30)
    top, front, dark = (hexc("f5f2ea"), hexc("e6dfd0"), hexc("c8bfae")) if sky else (WOOD_HI, WOOD, WOOD_SH)
    c.rect(1, 6, 62, 6, top)                      # the counter top, seen from above
    c.rect(1, 6, 62, 1, shade(top, 1.1))
    c.rect(1, 11, 62, 2, dark)                    # its front edge
    c.rect(2, 13, 60, 16, front)                  # the front
    for x in (2, 22, 42):                         # three panels
        c.rect(x + 2, 15, 16, 12, dark)
        c.rect(x + 3, 16, 14, 10, front)
    c.rect(2, 27, 60, 2, dark)                    # skirting
    disc(c, 32, 21, 4.5, COIN_SH)                 # the shop's sign: a big coin in the middle panel
    disc(c, 31.7, 20.7, 3.8, COIN_GOLD)
    c.rect(30, 19, 3, 3, POM)
    c.rect(52, 2, 6, 4, COIN_GOLD)                # the bell ("ring for service")
    c.rect(53, 1, 4, 1, COIN_GOLD)
    c.put(54, 0, COIN_SH)
    c.rect(51, 6, 8, 1, COIN_SH)
    c.put(53, 2, COIN_HI)
    c.rect(6, 3, 8, 3, hexc("f5f0e6"))           # a ledger lying open
    c.rect(10, 3, 1, 3, hexc("c8bfae"))
    c.rect(7, 4, 2, 1, hexc("8a8a96"))
    c.rect(11, 4, 2, 1, hexc("8a8a96"))
    if sky:                                       # a cloud wrapped round its foot, gold trim on top
        c.rect(1, 11, 62, 1, GOLD)
        cloud_puffs(c, [(5, 26, 4), (14, 27, 4.5), (26, 27, 4), (38, 27, 4.5), (50, 27, 4), (59, 26, 4)])
    c.outline()
    return c.img


def stand(sky=False):
    """A display stand for the wares, 18x16: a little cabinet with a cloth on top (gold and white up in the clouds)."""
    c = Canvas(18, 16)
    body, sh = (hexc("f5f2ea"), hexc("c8bfae")) if sky else (WOOD, WOOD_SH)
    cloth = GOLD if sky else hexc("c0392b")
    c.rect(2, 5, 14, 10, body)
    c.rect(2, 13, 14, 2, sh)
    c.rect(13, 5, 3, 10, sh)
    c.rect(1, 2, 16, 4, cloth)                    # the cloth, draped over the edges
    c.rect(1, 2, 16, 1, shade(cloth, 1.2))
    c.rect(1, 6, 2, 2, cloth)
    c.rect(15, 6, 2, 2, cloth)
    c.rect(8, 6, 2, 3, shade(cloth, 0.8))
    c.outline()
    return c.img


SHEEP_FACE = hexc("4a3a44")
SHEEP_FACE_ANGRY = hexc("8a2a2a")


def baallo(mood="idle"):
    """Baa-llo the sheep, the shopkeeper (30x36): a round woolly merchant in a little red cap and a gold
    monocle, with a red bow tie. Moods: idle, blink, happy (eyes shut, mouth open), angry (red, frowning)."""
    c = Canvas(30, 36)
    face = SHEEP_FACE_ANGRY if mood == "angry" else SHEEP_FACE
    wool, wool_sh, wool_dk = hexc("fbfaf5"), hexc("dedad0"), hexc("b8b4a8")
    cloud_puffs(c, [(7, 25, 6.5), (15, 22, 8), (23, 25, 6.5), (9, 31, 6), (21, 31, 6), (15, 29, 8)],
                wool, wool_sh, wool_dk)                         # the woolly body
    c.rect(4, 12, 5, 3, face)                                   # ears, sticking out
    c.rect(21, 12, 5, 3, face)
    c.rect(5, 13, 3, 1, hexc("e8a0b0"))
    c.rect(22, 13, 3, 1, hexc("e8a0b0"))
    c.rect(10, 8, 10, 12, face)                                 # the face, a soft oval
    c.rect(9, 10, 12, 8, face)
    c.rect(11, 20, 8, 1, face)
    cloud_puffs(c, [(11, 7, 3.5), (15, 6, 4), (19, 7, 3.5)], wool, wool_sh, wool_dk)  # a tuft on top
    c.rect(10, 2, 10, 3, hexc("d03a3a"))                        # the little red cap
    c.rect(11, 1, 8, 1, hexc("d03a3a"))
    c.rect(8, 5, 14, 1, hexc("8a2020"))                         # its brim
    c.rect(14, 0, 2, 1, COIN_GOLD)                              # a gold button on top
    if mood == "angry":                                         # knocked askew
        c.rect(19, 2, 2, 2, hexc("d03a3a"))
    # big friendly eyes, a gold monocle on the right one (on a little chain)
    c.rect(16, 10, 4, 1, GOLD)
    c.rect(16, 14, 4, 1, GOLD)
    c.rect(15, 11, 1, 3, GOLD)
    c.rect(20, 11, 1, 3, GOLD)
    c.put(20, 15, GOLD)
    c.put(21, 16, GOLD)
    c.put(21, 17, GOLD)
    for ex in (11, 16):
        if mood == "blink":
            c.rect(ex, 12, 3, 1, hexc("1b1b2a"))
        elif mood == "happy":                                   # ^ ^
            c.put(ex, 12, hexc("ffffff"))
            c.put(ex + 1, 11, hexc("ffffff"))
            c.put(ex + 2, 12, hexc("ffffff"))
        else:
            c.rect(ex, 11, 3, 3, hexc("ffffff"))
            c.rect(ex + 1, 12, 1, 2, hexc("1b1b2a"))
    if mood == "angry":                                         # frowning brows
        c.rect(10, 9, 3, 1, hexc("1b1b2a"))
        c.put(13, 10, hexc("1b1b2a"))
        c.rect(17, 9, 3, 1, hexc("1b1b2a"))
        c.put(16, 10, hexc("1b1b2a"))
    c.rect(14, 16, 2, 1, hexc("e8a0b0"))                        # a pink nose
    if mood == "happy":
        c.rect(13, 18, 4, 2, hexc("2a0a14"))
        c.rect(14, 19, 2, 1, hexc("e86a8a"))
    elif mood == "angry":
        c.rect(13, 19, 4, 1, hexc("1b1b2a"))
    else:
        c.rect(13, 18, 1, 1, hexc("2a1a24"))
        c.rect(14, 19, 2, 1, hexc("2a1a24"))
        c.rect(16, 18, 1, 1, hexc("2a1a24"))
    c.rect(11, 21, 3, 3, hexc("d03a3a"))                        # the red bow tie
    c.rect(16, 21, 3, 3, hexc("d03a3a"))
    c.rect(14, 22, 2, 2, hexc("8a2020"))
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
             ("debris", debris()), ("exit", exit_hatch()), ("exit_gate", exit_gate()),
             ("exit_bus", exit_bus()), ("exit_doors", exit_doors()), ("exit_heli", exit_heli()), ("bench", bench()), ("lamp", lamp()), ("car", car()), ("pedestal", pedestal()),
             ("desk", desk()), ("plant", plant()), ("cooler", cooler()),
             ("pit", pit()), ("cloud", cloud_bank()), ("column", column()), ("urn", urn()),
             ("gift", gift()), ("pit_sky", pit_sky()), ("exit_rainbow", exit_rainbow()),
             ("pit_sea", pit_sea()), ("coral", coral()), ("rock", rock_spire()), ("exit_spout", exit_spout()),
             ("seaweed", seaweed()), ("clam", clam()),
             ("coin_0", coin(0)), ("coin_1", coin(1)), ("coin_2", coin(2)), ("coin_3", coin(3)),
             ("coin_big", coin_big()), ("counter", counter()), ("counter_sky", counter(True)),
             ("stand", stand()), ("stand_sky", stand(True)), ("baallo", baallo()),
             ("baallo_blink", baallo("blink")), ("baallo_happy", baallo("happy")), ("baallo_angry", baallo("angry"))]
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


# ---------------------------------------------------------------------- guns
# Each gun points right. GUNS[name] = (x, y, w, h, grip_x, grip_y): the hand holds it at the grip pixel,
# and the barrel runs along the grip row to the right edge (that's where bullets come out).
GUNS = {}
METAL_D = hexc("3a3a44")
METAL_M = hexc("5a5a66")
METAL_L = hexc("9a9aa6")
SILVER = hexc("c9ccd6")


def gun_rusty_sidearm():
    c = Canvas(12, 8)
    c.rect(1, 1, 10, 3, hexc("8a7a6a"))      # slide
    c.rect(1, 1, 10, 1, hexc("a89a88"))
    c.rect(4, 2, 2, 1, hexc("a0522d"))       # rust
    c.rect(8, 3, 1, 1, hexc("a0522d"))
    c.rect(1, 4, 4, 3, hexc("6b4a2f"))       # grip
    c.rect(5, 4, 2, 1, METAL_D)              # trigger guard
    c.outline()
    return c.img, 3, 2


def gun_fluff_shotgun():
    c = Canvas(19, 8)
    c.rect(7, 1, 11, 2, METAL_D)             # double barrel
    c.rect(7, 3, 11, 1, METAL_M)
    c.rect(7, 1, 11, 1, METAL_L)
    c.rect(9, 4, 5, 2, WOOD)                 # fore-end
    for x, y in ((1, 2), (3, 1), (3, 4), (5, 2), (1, 5)):  # fluffy pink stock
        c.rect(x, y, 3, 3, hexc("ffb3d1"))
    c.rect(2, 3, 3, 2, hexc("ff8fbf"))
    c.outline()
    return c.img, 7, 2


def gun_pocket_uzi():
    c = Canvas(13, 11)
    c.rect(1, 1, 10, 4, METAL_D)             # body
    c.rect(1, 1, 10, 1, METAL_M)
    c.rect(11, 2, 1, 2, METAL_M)             # stubby barrel
    c.rect(5, 5, 3, 5, METAL_M)              # long magazine
    c.rect(5, 9, 3, 1, METAL_D)
    c.rect(1, 5, 3, 3, METAL_D)              # grip
    c.outline()
    return c.img, 3, 3


def gun_big_magnum():
    c = Canvas(18, 9)
    c.rect(7, 1, 10, 2, SILVER)              # long barrel
    c.rect(7, 1, 10, 1, hexc("eef0f5"))
    c.rect(4, 1, 5, 4, METAL_L)              # cylinder
    for x in (5, 7):
        c.put(x, 2, METAL_D)
        c.put(x, 4, METAL_D)
    c.rect(1, 3, 4, 5, hexc("7a4a2a"))       # wooden grip
    c.rect(1, 3, 1, 5, hexc("5a3218"))
    c.outline()
    return c.img, 4, 2


def gun_confetti_popper():
    c = Canvas(13, 10)
    stripes = [hexc("ff5fa2"), hexc("ffd23a"), hexc("5ad1ff")]
    for i in range(9):                       # party-popper cone widening to the right
        h = 1 + i // 2
        c.rect(3 + i, 5 - h // 2 - 1, 1, h + 1, stripes[i // 3])
    c.rect(1, 4, 3, 4, hexc("f5f0e6"))       # handle with a string
    c.put(0, 7, hexc("c0392b"))
    c.outline()
    return c.img, 2, 5


def gun_bubble_blaster():
    c = Canvas(15, 11)
    c.rect(3, 5, 10, 3, hexc("ff8a3a"))      # toy body
    c.rect(13, 6, 1, 1, hexc("ffd23a"))      # nozzle
    c.rect(4, 1, 6, 4, hexc("8ed8ff"))       # see-through tank
    c.rect(5, 2, 1, 1, hexc("e0f6ff"))
    c.rect(4, 8, 3, 2, hexc("ff8a3a"))       # grip
    c.outline()
    return c.img, 5, 6


def gun_banana_rang():
    c = Canvas(15, 9)
    for i, (x, y) in enumerate(((2, 5), (4, 4), (6, 3), (8, 3), (10, 3), (12, 4))):  # curved banana
        c.rect(x, y, 3, 2, hexc("f2d23a"))
        c.put(x, y + 1, hexc("d9b020"))
    c.rect(14, 4, 1, 2, hexc("6b4a2f"))      # tip
    c.rect(1, 5, 2, 2, hexc("6b4a2f"))       # stem
    c.rect(5, 6, 2, 2, METAL_D)              # trigger
    c.outline()
    return c.img, 5, 4


def gun_bee_hive():
    c = Canvas(15, 12)
    for i, y in enumerate(range(1, 10, 2)):  # striped hive
        w = 8 - abs(i - 2) * 2
        c.rect(7 - w // 2 + 2, y, w + 2, 2, hexc("f2c230") if i % 2 == 0 else hexc("b88a20"))
    c.rect(12, 5, 2, 2, hexc("2b1d14"))      # the hole the bees fly out of
    c.rect(1, 6, 5, 2, WOOD)                 # stick
    c.rect(1, 8, 2, 3, WOOD_SH)
    c.outline()
    return c.img, 3, 6


def gun_rainbow_ray():
    c = Canvas(17, 10)
    c.rect(3, 3, 9, 4, hexc("f5f0e6"))       # sleek body
    c.rect(3, 3, 9, 1, hexc("ffffff"))
    rainbow = ["ff4a4a", "ff9f3a", "ffd23a", "5ae06a", "4f8ad8", "a06ae0"]
    for i, col in enumerate(rainbow):        # rainbow fins
        c.put(4 + i, 7, hexc(col))
    c.rect(12, 2, 2, 6, METAL_L)             # emitter dish
    c.rect(14, 4, 2, 2, hexc("ff5fa2"))
    c.rect(3, 7, 3, 2, hexc("f5f0e6"))       # grip
    c.outline()
    return c.img, 4, 5


def gun_laser_pointer():
    """D: a cat-toy laser pointer - a slim pen with a red tip."""
    c = Canvas(12, 7)
    c.rect(1, 2, 9, 3, hexc("4a4a55"))       # pen body
    c.rect(1, 2, 9, 1, hexc("7a7a88"))
    c.rect(4, 1, 2, 1, hexc("d63a2f"))       # button
    c.rect(10, 3, 1, 1, hexc("ff3a3a"))      # tip
    c.rect(1, 5, 3, 1, hexc("4a4a55"))       # pocket clip
    c.outline()
    return c.img, 2, 3


def gun_bouncer():
    """C: a toy gun loaded with rubber balls - a fat orange drum and a wide muzzle."""
    c = Canvas(15, 11)
    c.rect(4, 2, 8, 4, hexc("ff8a3a"))       # body
    c.rect(4, 2, 8, 1, hexc("ffb070"))
    c.rect(12, 1, 2, 6, hexc("ff5fa2"))      # wide muzzle
    c.rect(5, 6, 4, 2, hexc("5ad1ff"))       # ball hopper
    c.put(6, 6, hexc("ffffff"))
    c.rect(2, 4, 3, 6, hexc("e06a20"))       # grip
    c.outline()
    return c.img, 3, 4


def gun_plasma_zapper():
    """C: a retro ray gun - bulbous body, rings on the barrel, a green coil."""
    c = Canvas(16, 11)
    c.rect(2, 2, 6, 5, hexc("9a9aa6"))       # bulb
    c.rect(2, 2, 6, 1, hexc("c9ccd6"))
    c.rect(8, 3, 6, 2, METAL_M)              # barrel
    for x in (9, 11, 13):
        c.rect(x, 2, 1, 4, hexc("5ae06a"))   # coil rings
    c.rect(14, 3, 1, 2, hexc("b8ffb0"))
    c.rect(3, 7, 3, 3, METAL_D)              # grip
    c.outline()
    return c.img, 4, 4


def gun_gull_gatling():
    """B: a seagull's minigun - six barrels in a bundle, a feathery white stock and a belt."""
    c = Canvas(20, 12)
    c.rect(1, 2, 6, 5, hexc("f5f0e6"))       # feathered stock
    c.rect(1, 2, 6, 1, hexc("ffffff"))
    c.put(2, 4, hexc("c9ccd6"))
    c.put(4, 5, hexc("c9ccd6"))
    c.rect(7, 2, 3, 5, METAL_D)              # rotor housing
    c.rect(10, 2, 9, 1, METAL_L)             # barrels
    c.rect(10, 4, 9, 1, METAL_L)
    c.rect(10, 6, 9, 1, METAL_L)
    c.rect(10, 3, 9, 1, METAL_D)
    c.rect(10, 5, 9, 1, METAL_D)
    c.rect(17, 2, 1, 5, METAL_M)             # barrel clamp
    c.rect(7, 7, 2, 4, hexc("d9b24c"))       # ammo belt
    c.rect(3, 7, 3, 3, hexc("e0a040"))       # grip (a beak-yellow handle)
    c.outline()
    return c.img, 4, 4


def gun_egg_lobber():
    """B: a grenade launcher that lobs eggs - a wide tube with an egg in the breech."""
    c = Canvas(18, 11)
    c.rect(3, 2, 12, 5, hexc("4a7a3a"))      # tube
    c.rect(3, 2, 12, 1, hexc("6a9a50"))
    c.rect(15, 1, 2, 7, hexc("2c4a24"))      # muzzle ring
    c.rect(7, 3, 3, 3, hexc("f5ecd0"))       # an egg showing in the breech
    c.put(8, 3, hexc("ffffff"))
    c.rect(1, 3, 2, 3, hexc("2c4a24"))       # stock
    c.rect(4, 7, 3, 3, hexc("2c4a24"))       # grip
    c.outline()
    return c.img, 5, 4


def gun_party_rocket():
    """A: a rocket launcher painted like a party - a big red tube, a rocket tip poking out."""
    c = Canvas(22, 11)
    c.rect(2, 2, 16, 5, hexc("d63a2f"))      # tube
    c.rect(2, 2, 16, 1, hexc("ff6a5a"))
    for x in (5, 9, 13):
        c.rect(x, 3, 1, 3, hexc("ffd23a"))   # stripes
    c.rect(18, 3, 2, 3, hexc("f5f0e6"))      # rocket tip
    c.put(20, 4, hexc("f5f0e6"))
    c.rect(0, 1, 2, 7, METAL_D)              # back blast cone
    c.rect(8, 7, 3, 3, METAL_D)              # grip
    c.rect(5, 0, 3, 2, METAL_M)              # sight
    c.outline()
    return c.img, 9, 4


def gun_flufful():
    """The Flufful (Fluffy + Gundul), as the owner drew it: a pink pistol made of fluffy fur, with a
    sweet cartoon shark sitting on top of the barrel, smiling."""
    c = Canvas(24, 20)
    fur, fur_sh, fur_hi = hexc("ff9fc4"), hexc("e07aa8"), hexc("ffd0e4")
    # the barrel: a fat furry tube with tufty edges
    c.rect(6, 9, 15, 5, fur)
    for x in range(6, 21, 2):
        c.put(x, 8, fur)                      # tufts along the top
        c.put(x + 1, 14, fur_sh)              # and underneath
    c.rect(6, 9, 15, 1, fur_hi)
    c.rect(6, 13, 15, 1, fur_sh)
    c.rect(21, 9, 2, 5, fur_sh)               # the muzzle
    c.rect(22, 10, 1, 3, hexc("c0396b"))
    # the grip: fluffy too, leaning back
    for i in range(6):
        x0 = 6 - (1 if i >= 3 else 0)
        c.rect(x0, 13 + i, 5, 1, fur)
        c.put(x0, 13 + i, fur_hi)
        c.put(x0 + 4, 13 + i, fur_sh)
        if i % 2 == 0:
            c.put(x0 - 1, 13 + i, fur)        # fuzzy edge
    c.rect(11, 14, 3, 1, fur_sh)              # trigger guard
    c.rect(13, 14, 1, 2, fur_sh)
    c.put(12, 15, hexc("c0396b"))
    # the shark on top: round, blue-grey with a white belly, a fin, a big eye and a smile
    shark, shark_sh, belly = hexc("8fc4e8"), hexc("6aa2cc"), hexc("f5f8ff")
    for x, y, w, h in ((11, 3, 9, 6), (10, 4, 11, 4), (12, 2, 7, 8)):
        c.rect(x, y, w, h, shark)
    c.rect(12, 7, 8, 2, belly)
    c.rect(14, 0, 2, 2, shark_sh)             # dorsal fin
    c.put(14, 2, shark_sh)
    c.put(15, 2, shark)
    c.rect(9, 5, 2, 2, shark_sh)              # tail fin, pointing back
    c.put(8, 4, shark_sh)
    c.put(8, 7, shark_sh)
    c.rect(16, 3, 2, 2, hexc("1e1416"))       # the eye, with a glint
    c.put(16, 3, hexc("ffffff"))
    c.rect(15, 6, 4, 1, hexc("1e1416"))       # a sweet smile
    c.put(19, 5, hexc("1e1416"))
    c.put(16, 7, hexc("ffffff"))              # two little teeth
    c.put(18, 7, hexc("ffffff"))
    c.put(17, 7, hexc("ff5f8a"))              # tongue
    c.rect(13, 5, 2, 1, hexc("ffb3c8"))       # blush
    c.outline()
    return c.img, 7, 11


def gun_long_shot():
    c = Canvas(26, 9)
    c.rect(12, 3, 13, 1, METAL_D)            # long thin barrel
    c.rect(24, 2, 1, 3, METAL_D)             # muzzle brake
    c.rect(5, 2, 8, 3, METAL_L)              # receiver
    c.rect(5, 2, 8, 1, SILVER)
    c.rect(7, 0, 6, 2, hexc("26232b"))       # scope
    c.put(12, 0, hexc("70d6ff"))             # lens glint
    c.rect(0, 3, 6, 3, hexc("7a4a2a"))       # wooden stock
    c.rect(0, 3, 6, 1, hexc("9a6a3a"))
    c.rect(7, 5, 2, 3, hexc("5a3218"))       # grip
    c.outline()
    return c.img, 7, 3


GUN_SPRITES = [("rusty_sidearm", gun_rusty_sidearm), ("fluff_shotgun", gun_fluff_shotgun),
               ("pocket_uzi", gun_pocket_uzi), ("big_magnum", gun_big_magnum),
               ("confetti_popper", gun_confetti_popper), ("bubble_blaster", gun_bubble_blaster),
               ("banana_rang", gun_banana_rang), ("bee_hive", gun_bee_hive), ("rainbow_ray", gun_rainbow_ray),
               ("laser_pointer", gun_laser_pointer), ("bouncer", gun_bouncer), ("plasma_zapper", gun_plasma_zapper),
               ("gull_gatling", gun_gull_gatling), ("egg_lobber", gun_egg_lobber), ("party_rocket", gun_party_rocket),
               ("flufful", gun_flufful), ("long_shot", gun_long_shot)]


def guns_sheet():
    parts = [(name, *fn()) for name, fn in GUN_SPRITES]
    w = sum(img.width + 2 for _, img, _, _ in parts)
    h = max(img.height for _, img, _, _ in parts)
    sheet = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    x = 0
    for name, img, gx, gy in parts:
        sheet.paste(img, (x, 0))
        GUNS[name] = (x, 0, img.width, img.height, gx, gy)
        x += img.width + 2
    return sheet


# ---------------------------------------------------------------------- items
# 16x16 icons for passive and active items (objects/Item), atlas sprites/items.png + items.txt.
ITEMS = {}


def item_slippers():
    c = Canvas(16, 16)
    for x, y in ((2, 8), (5, 7), (8, 7), (11, 8)):
        c.rect(x, y, 4, 4, hexc("ffb3d1"))   # fluffy top
    c.rect(1, 11, 14, 3, hexc("e88fb4"))      # sole
    c.rect(3, 9, 2, 1, hexc("ffffff"))
    c.outline()
    return c.img


def item_pocket():
    c = Canvas(16, 16)
    c.rect(2, 3, 12, 11, hexc("3f5f9a"))      # denim
    c.rect(2, 3, 12, 2, hexc("2f4a7a"))
    for x in range(3, 13, 2):
        c.put(x, 5, hexc("e8c030"))           # stitching
    c.rect(6, 7, 4, 4, hexc("4fa4ff"))        # a blank peeking out
    c.put(7, 8, hexc("ffffff"))
    c.outline()
    return c.img


def item_medkit():
    c = Canvas(16, 16)
    c.rect(1, 4, 14, 10, hexc("f5f0e6"))
    c.rect(6, 2, 4, 3, hexc("9a9aa6"))        # handle
    c.rect(7, 6, 2, 6, hexc("e84a4a"))        # red cross
    c.rect(5, 8, 6, 2, hexc("e84a4a"))
    c.outline()
    return c.img


def item_field():
    """The Fluff Field: a pink pom-pom inside a glowing bubble of force."""
    c = Canvas(16, 16)
    ring, glow = hexc("ff8ac8"), hexc("ffd6ec")
    for a in range(0, 360, 3):                 # the field's rim
        c.put(int(round(8 + math.cos(math.radians(a)) * 6.6)), int(round(8 + math.sin(math.radians(a)) * 6.6)), ring)
    c.rect(4, 3, 2, 1, glow)                   # a shine on the bubble
    c.put(3, 4, glow)
    for x, y in ((6, 7), (8, 6), (10, 7), (7, 9), (9, 9), (8, 8)):   # the pom-pom
        c.rect(x - 1, y - 1, 2, 2, hexc("ff6ab4"))
    c.rect(7, 7, 2, 2, hexc("ffc0e0"))
    c.outline()
    return c.img


def item_fuzz():
    """Static Fuzz: a blue fuzzball crackling with sparks, like a sock rubbed on the carpet."""
    c = Canvas(16, 16)
    blue, light, dark = hexc("5aa8ff"), hexc("b8dcff"), hexc("2a62c0")
    disc(c, 8, 8.5, 4.6, dark)
    disc(c, 7.6, 8.1, 3.9, blue)
    for x, y in ((5, 4), (8, 3), (11, 4), (3, 7), (13, 8), (4, 11), (12, 12), (8, 13)):  # fuzzy tufts
        c.put(x, y, blue)
    c.rect(6, 6, 2, 2, light)
    c.rect(6, 9, 1, 1, hexc("1b1b2a"))         # a little face: it's pleased with itself
    c.rect(9, 9, 1, 1, hexc("1b1b2a"))
    spark = hexc("fff06a")
    for pts in (((1, 2), (2, 3), (1, 4), (2, 5)), ((14, 2), (13, 3), (14, 4)), ((14, 12), (15, 13), (14, 14))):
        for x, y in pts:
            c.put(x, y, spark)
    c.outline()
    return c.img


def item_clips():
    c = Canvas(16, 16)
    for i, x in enumerate((2, 6, 10)):
        c.rect(x, 3 + i, 4, 11 - i, hexc("5a5a66"))
        c.rect(x + 1, 1 + i, 2, 3, hexc("d9b24c"))   # bullet tips peeking out
    c.outline()
    return c.img


def item_hotsauce():
    c = Canvas(16, 16)
    c.rect(5, 1, 6, 3, hexc("4fa060"))        # cap
    c.rect(6, 4, 4, 2, hexc("c0392b"))        # neck
    c.rect(3, 6, 10, 9, hexc("d63a2f"))       # bottle
    c.rect(5, 8, 6, 4, hexc("f5f0e6"))        # label
    c.rect(7, 9, 2, 2, hexc("ff8a3a"))        # flame
    c.outline()
    return c.img


def item_clover():
    c = Canvas(16, 16)
    for ox, oy in ((-3, -3), (3, -3), (-3, 3), (3, 3)):  # four round leaves
        for y in range(16):
            for x in range(16):
                if ((x - 7.5 - ox) ** 2 + (y - 6.5 - oy) ** 2) ** 0.5 <= 2.7:
                    c.put(x, y, hexc("4fbf5a"))
        c.put(int(7.5 + ox) - 1, int(6.5 + oy) - 1, hexc("8ae08a"))
    c.rect(7, 6, 2, 2, hexc("2f8a3a"))        # middle
    c.rect(8, 11, 1, 4, hexc("2f8a3a"))       # stem
    c.outline()
    return c.img


def item_grenade():
    c = Canvas(16, 16)
    c.rect(4, 5, 8, 10, hexc("3f6a3a"))       # body
    c.rect(3, 7, 10, 6, hexc("3f6a3a"))
    for y in (8, 11):
        c.rect(3, y, 10, 1, hexc("2a4a28"))   # grooves
    c.rect(6, 2, 4, 3, hexc("9a9aa6"))        # top
    c.rect(10, 1, 3, 3, hexc("c9ccd6"))       # pin ring
    c.rect(11, 2, 1, 1, (0, 0, 0, 0))
    c.outline()
    return c.img


def item_shield():
    c = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d <= 7:
                c.put(x, y, hexc("8ed8ff") if d > 5.5 else hexc("4f8ad8"))
    c.rect(4, 4, 3, 2, hexc("e0f6ff"))        # shine
    c.outline()
    return c.img


def item_ghost():
    c = Canvas(16, 16)
    c.rect(4, 2, 8, 9, hexc("e8ecf5"))        # ghostly bullet
    c.rect(5, 1, 6, 1, hexc("e8ecf5"))
    for x in (4, 7, 10):
        c.rect(x, 11, 2, 3, hexc("e8ecf5"))   # wavy tail
    c.rect(6, 5, 1, 2, hexc("2a2a30"))        # eyes
    c.rect(9, 5, 1, 2, hexc("2a2a30"))
    c.outline()
    return c.img


def item_candy():
    c = Canvas(16, 16)
    for x, y in ((2, 3), (6, 1), (9, 3), (4, 6), (8, 6)):
        c.rect(x, y, 5, 5, hexc("ffb3d1"))   # fluffy candy cloud
        c.put(x + 1, y + 1, hexc("ffe0ee"))
    c.rect(7, 10, 2, 5, hexc("f5f0e6"))       # stick
    c.outline()
    return c.img


def item_heart():
    c = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            # two round lobes on top, a point at the bottom
            lobe = min(((x - 4.5) ** 2 + (y - 5.5) ** 2) ** 0.5, ((x - 10.5) ** 2 + (y - 5.5) ** 2) ** 0.5) <= 3.6
            point = y >= 5 and abs(x - 7.5) <= 7 - (y - 5) * 0.85
            if lobe or point:
                c.put(x, y, hexc("e8475a"))
    for x, y in ((3, 2), (4, 2), (2, 3), (5, 3), (3, 4), (11, 2), (12, 3)):
        c.put(x, y, hexc("ffc2d4"))           # fluffy tufts of fur on top
    c.rect(4, 4, 2, 2, hexc("ff9aaa"))        # shine
    c.rect(7, 6, 2, 6, hexc("ffffff"))        # a white plus: one more heart
    c.rect(5, 8, 6, 2, hexc("ffffff"))
    c.outline()
    return c.img


def item_flippers():
    """Flippers, sold only in the city by Baa-llo: a pair of sea-blue swim fins with a bubble rising."""
    c = Canvas(16, 16)
    fin, light, strap = hexc("2fa8c8"), hexc("8ae6f0"), hexc("1a6a8a")
    for cx in (3.5, 11.5):
        for y in range(1, 10):                 # the fin: wide at the tip, narrowing to the foot
            half = 3.1 - (y - 1) * 0.18
            for x in range(16):
                if abs(x + 0.5 - cx) <= half and not (y == 1 and abs(x + 0.5 - cx) < 1):  # a notch at the tip
                    c.put(x, y, fin)
        for dx in (-1.5, 1.5):                 # ribs along the fin
            c.rect(int(cx + dx), 2, 1, 6, light)
        c.rect(int(cx) - 2, 10, 5, 4, strap)   # the foot pocket
        c.rect(int(cx) - 1, 11, 3, 2, hexc("0f4058"))
    c.put(7, 2, hexc("e0f6ff"))               # a little bubble rising between them
    c.put(8, 4, hexc("e0f6ff"))
    c.outline()
    return c.img


ITEM_SPRITES = [("slippers", item_slippers), ("pocket", item_pocket), ("medkit", item_medkit),
                ("field", item_field), ("clips", item_clips), ("hotsauce", item_hotsauce),
                ("clover", item_clover), ("grenade", item_grenade), ("shield", item_shield),
                ("ghost", item_ghost), ("candy", item_candy), ("heart", item_heart),
                ("fuzz", item_fuzz), ("flippers", item_flippers)]


def items_sheet():
    sheet = Image.new("RGBA", ((16 + 2) * len(ITEM_SPRITES), 16), (0, 0, 0, 0))
    for i, (name, fn) in enumerate(ITEM_SPRITES):
        sheet.paste(fn(), (i * 18, 0))
        ITEMS[name] = (i * 18, 0, 16, 16)
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
    # --- stage 2: the town
    "street":   dict(floor=("paving", "a8a8a0", "8a8a84", "c0c0b8"),
                     face=("shops", "e8b48a", "3a5a8a", "2a2a30"),
                     cap=("slate", "4a5060", "34384a", "6a7080")),
    "market":   dict(floor=("checker", "d8d0c0", "b8ac98", "e8e0d0"),
                     face=("stalls", "8a5a2b", "c0392b", "f5f0e6"),
                     cap=("rooftiles", "c86a3a", "9a4a26", "e08a5a")),
    "alley":    dict(floor=("redbrick", "b5654a", "8a4a35", "c9846a"),
                     face=("brickwall", "c0704f", "8a4a35", "6a6a74"),
                     cap=("slate", "4a4450", "343040", "6a6470")),
    "park":     dict(floor=("grass", "72b050", "5e9a40", "8ac868"),
                     face=("ironfence", "2a2a30", "3f7a35", "f2c230"),
                     cap=("canopy", "4a8a3a", "2f6a2a", "6fb04f")),
    "townhall": dict(floor=("marble", "e8e4dc", "3a3a44", "ffffff"),
                     face=("columns", "e0dccf", "b8b0a0", "8a2a3a"),
                     cap=("slate", "5a6070", "3e4452", "7a8090")),
    "shop":     dict(floor=("planks", "9a6a3a", "7e5530", "b07d4c"),
                     face=("shelves", "7a5236", "5a3a26", "3e2818"),
                     cap=("shingle", "5a3a26", "3e2818", "7a5236")),
    # --- stage 3: the city, at night
    "avenue":       dict(floor=("asphalt", "2e3138", "24262c", "c8c040"),
                         face=("skyscraper", "3a4258", "2a3042", "ffd166"),
                         cap=("nightsky", "141a2e", "0e1222", "f5f0e6")),
    "subway":       dict(floor=("subwaytile", "4e545e", "3c414a", "d8c040"),
                         face=("tiles", "8a929c", "6a7280", "2f6ab0"),
                         cap=("slate", "2a2e38", "1e2128", "3a404c")),
    "rooftop":      dict(floor=("gravel", "4a4a52", "3a3a42", "5e5e68"),
                         face=("parapet", "5e5e68", "46464e", "ffd166"),
                         cap=("nightsky", "141a2e", "0e1222", "f5f0e6")),
    "construction": dict(floor=("concrete", "5e5a54", "4c4842", "7a756c"),
                         face=("hoarding", "8a6a3a", "6a4e2a", "e8c030"),
                         cap=("nightsky", "141a2e", "0e1222", "f5f0e6")),
    "helipad":      dict(floor=("helipad", "3a3d46", "2c2f36", "e8e8e8"),
                         face=("parapet", "5e5e68", "46464e", "ff4a3a"),
                         cap=("nightsky", "141a2e", "0e1222", "f5f0e6")),
    "plaza":        dict(floor=("flags", "5a5e68", "464a54", "70747e"),        # the Pigeon King's arena
                         face=("ironfence", "16161c", "24402a", "ffd166", "2e4a34"),
                         cap=("nightsky", "141a2e", "0e1222", "f5f0e6")),
    "railyard":     dict(floor=("gravel", "4e4a44", "3c3934", "66605a"),        # the Evil Train's arena
                         face=("brickwall", "6a3e30", "4a2a20", "5a5a66"),
                         cap=("nightsky", "141a2e", "0e1222", "f5f0e6")),
    "pawnshop":     dict(floor=("checker", "3a3440", "2c2832", "4a4250"),
                         face=("neonshop", "3a2e3e", "2a2030", "ff5fa2"),
                         cap=("slate", "2a2e38", "1e2128", "3a404c")),
    # --- stage 4: the skyscraper, by day
    "lobby":     dict(floor=("marble", "e6e2d8", "c8c2b4", "ffffff"),
                      face=("glasswall", "8ec5e8", "5a6278", "3a7a4a"),
                      cap=("ceiling", "c8ccd4", "aab0ba", "fff6d6")),
    "office":    dict(floor=("carpet", "6a7a96", "5a6a84", "7a8aa6"),
                      face=("officewall", "e0d8c4", "b8ae96", "4a5a7a"),
                      cap=("ceiling", "c8ccd4", "aab0ba", "fff6d6")),
    "servers":   dict(floor=("raised", "8a92a0", "6a727e", "a8b0bc"),
                      face=("serverrack", "2a2e38", "1a1d24", "5ae06a"),
                      cap=("ceiling", "9aa0aa", "80868f", "d8f0ff")),
    "cafeteria": dict(floor=("checker", "e8e0d0", "c06a4a", "f5f0e6"),
                      face=("officewall", "f2e0b0", "c9b080", "c06a4a"),
                      cap=("ceiling", "c8ccd4", "aab0ba", "fff6d6")),
    "vault":     dict(floor=("marble", "3a3440", "2a2430", "d9b24c"),
                      face=("columns", "4a4250", "34303c", "d9b24c"),
                      cap=("slate", "2a2e38", "1e2128", "3a404c")),
    "roof":      dict(floor=("helipad", "8a8d96", "767982", "e8e8e8"),
                      face=("parapetday", "a0a4ae", "80848e", "ff4a3a"),
                      cap=("sky", "7ec8f0", "a8dcf8", "ffffff")),
    # --- stage 5: the clouds (the caps are see-through: the live sky shows beyond the cloud's edge)
    "cloudgate": dict(floor=("cloud", "f4f2fc", "d6dcf0", "ffffff"),
                      face=("pearlygate", "fbfaff", "cfd6ee", "e8c040"),
                      cap=("open", "000000", "000000", "000000")),
    "clouds":    dict(floor=("cloud", "f4f2fc", "d6dcf0", "ffffff"),
                      face=("cloudbank", "fbfaff", "cfd6ee", "ffd166"),
                      cap=("open", "000000", "000000", "000000")),
    "temple":    dict(floor=("marble", "f2eee6", "e0d6c0", "e8c040"),
                      face=("columns", "f5f2ea", "d8d0c0", "4a9ae8"),
                      cap=("open", "000000", "000000", "000000")),
    "storm":     dict(floor=("cloud", "a8aec4", "8a90a8", "c0c6d8"),
                      face=("stormbank", "b8bed2", "8a90a8", "fff06a"),
                      cap=("open", "000000", "000000", "000000")),
    "rainbow":   dict(floor=("cloud", "fcf0f8", "ecd2e6", "ffffff"),     # treasure: a rainbow's end
                      face=("cloudbank", "fff6fc", "ecd2e6", "ff8ac8"),
                      cap=("open", "000000", "000000", "000000")),
    "throne":    dict(floor=("marble", "fbf6ee", "f2d6e6", "e8c040"),     # the Fluffy King's throne room
                      face=("columns", "fbf6ee", "e8c040", "ff6ab4"),
                      cap=("open", "000000", "000000", "000000")),
    # --- the secret floor: the deep sea under the clouds (no shop down here - Baa-llo can't swim)
    "seabed":  dict(floor=("sand", "c2b48a", "a89a72", "ddd0a8"),
                    face=("reef", "4a5a6a", "3a4654", "ff7a8a"),
                    cap=("deepwater", "123a5a", "1a4a6e", "2a6a8e")),
    "reef":    dict(floor=("sand", "bcae86", "a0946e", "d8cca4"),
                    face=("reef", "4e5c6c", "3c4858", "ff9a5a"),
                    cap=("deepwater", "123a5a", "1a4a6e", "2a6a8e")),
    "kelp":    dict(floor=("sand", "a8a47e", "8e8a66", "c4c09a"),
                    face=("kelp", "2e4048", "22323a", "4a9a4a"),
                    cap=("deepwater", "0f3250", "164264", "2a6a8e")),
    "trench":  dict(floor=("seastone", "4a5868", "3a4656", "6a7a8a"),
                    face=("cavewall", "34404e", "262f3a", "6ae0d0"),
                    cap=("deepwater", "0a2440", "102e50", "1e4a70")),
    "wreck":   dict(floor=("planks", "6a6448", "524c36", "7e7858"),          # treasure: a sunken ship
                    face=("hull", "5a4a36", "3e3226", "d9b24c"),
                    cap=("deepwater", "123a5a", "1a4a6e", "2a6a8e")),
    "lair":    dict(floor=("seastone", "3a4656", "2c3644", "5a6878"),        # the shark's lair
                    face=("cavewall", "2a3440", "1e2630", "e8e4dc"),
                    cap=("deepwater", "081e36", "0e2846", "1a4268")),
    # --- Baa-llo's shop, one look per stage (the counter, stands and the sheep himself are props)
    "store_village": dict(floor=("planks", "b07d4c", "966a3e", "c8945e"),
                          face=("store", "7a5236", "e8d8b8", "3e2818", "c0392b"),
                          cap=("thatch", "c49a45", "8e6a2a", "e0c070")),
    "store_town":    dict(floor=("checker", "e8e0d0", "b5543a", "f5f0e6"),
                          face=("store", "8a5a2b", "f2e6c8", "3a5a8a", "3a5a8a"),
                          cap=("rooftiles", "c86a3a", "9a4a26", "e08a5a")),
    "store_city":    dict(floor=("checker", "4a3e58", "362c42", "5a4e68"),
                          face=("store", "5a4a6a", "2e2438", "1e1828", "5affc0"),
                          cap=("slate", "2a2e38", "1e2128", "3a404c")),
    "store_tower":   dict(floor=("carpet", "a84a4a", "923e3e", "b85a5a"),
                          face=("store", "c8b89a", "f2ead8", "5a4a3a", "d9b24c"),
                          cap=("ceiling", "c8ccd4", "aab0ba", "fff6d6")),
    "store_sky":     dict(floor=("cloud", "fcf6ec", "eedcc8", "ffffff"),
                          face=("cloudbank", "fffaf0", "eedcc8", "e8c040"),
                          cap=("open", "000000", "000000", "000000")),
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
    elif kind == "paving":  # town street: square slabs
        c.rect(0, 0, T, 1, dark)
        c.rect(0, 8, T, 1, dark)
        c.rect(0, 0, 1, T, dark)
        c.rect(8, 0, 1, T, dark)
        for _ in range(6):
            c.put(rnd.randrange(T), rnd.randrange(T), light)
        if seed == 2:  # crack
            for i in range(5):
                c.put(3 + i, 3 + i // 2 + (i % 2), dark)
        if seed == 3:  # a bit of litter
            c.rect(10, 10, 3, 2, hexc("f5f0e6"))
            c.put(11, 10, hexc("c0392b"))
    elif kind == "checker":
        for y in range(0, T, 8):
            for x in range(0, T, 8):
                c.rect(x, y, 8, 8, base if (x + y) // 8 % 2 == 0 else light)
        c.rect(0, 0, T, 1, dark)
        c.rect(0, 0, 1, T, dark)
    elif kind == "redbrick":
        for y in range(0, T, 4):
            c.rect(0, y, T, 1, dark)
            sh = 0 if (y // 4) % 2 == 0 else 4
            for x in range(sh, T, 8):
                c.rect(x, y, 1, 4, dark)
        for _ in range(5):
            c.put(rnd.randrange(T), rnd.randrange(T), light)
        if seed == 3:  # puddle
            c.rect(4, 6, 8, 4, hexc("6a8ab0"))
            c.rect(5, 5, 6, 6, hexc("6a8ab0"))
            c.rect(6, 6, 3, 1, hexc("b8d0e8"))
    elif kind == "marble":
        for y in range(0, T, 8):
            for x in range(0, T, 8):
                c.rect(x, y, 8, 8, base if (x + y) // 8 % 2 == 0 else dark)
        for _ in range(3):  # veins
            x, y = rnd.randrange(T), rnd.randrange(T)
            c.put(x, y, light if (x // 8 + y // 8) % 2 else shade(dark, 1.4))
    elif kind == "asphalt":  # city street at night: dark tarmac, lane paint
        for _ in range(10):
            c.put(rnd.randrange(T), rnd.randrange(T), dark if rnd.random() < 0.6 else shade(base, 1.2))
        if seed == 2:  # a crack
            for i in range(6):
                c.put(4 + i, 9 + (i % 2), dark)
        if seed == 3:  # a manhole-cover-sized oil stain
            c.rect(5, 5, 6, 5, shade(base, 0.8))
    elif kind == "subwaytile":  # platform tiles with the yellow safety line
        for y in range(0, T, 8):
            c.rect(0, y, T, 1, dark)
        for x in range(0, T, 8):
            c.rect(x, 0, 1, T, dark)
        if seed == 3:  # old gum
            c.rect(9, 10, 2, 2, hexc("8a8090"))
    elif kind == "gravel":  # flat roof
        for _ in range(40):
            c.put(rnd.randrange(T), rnd.randrange(T), dark if rnd.random() < 0.5 else light)
        if seed == 3:  # a puddle catching the moon
            c.rect(4, 6, 7, 3, hexc("2a3450"))
            c.rect(5, 6, 2, 1, hexc("6a7aa0"))
    elif kind == "concrete":  # construction site slab
        for _ in range(14):
            c.put(rnd.randrange(T), rnd.randrange(T), dark)
        if seed == 2:  # rebar poking out
            c.rect(3, 5, 9, 1, hexc("8a4a2a"))
            c.rect(6, 9, 7, 1, hexc("8a4a2a"))
        if seed == 3:
            c.rect(0, 0, T, 1, dark)
            c.rect(0, 0, 1, T, dark)
    elif kind == "helipad":  # roof with landing markings
        for _ in range(8):
            c.put(rnd.randrange(T), rnd.randrange(T), dark)
        if seed == 3:  # tyre scuff (the big H is painted on by RoomRenderer)
            c.rect(3, 8, 9, 1, dark)
    elif kind == "carpet":  # office carpet tiles, a faint weave
        for y in range(0, T, 2):
            for x in range((y // 2) % 2, T, 2):
                c.put(x, y, light if rnd.random() < 0.15 else base)
        c.rect(0, 0, T, 1, dark)
        c.rect(0, 0, 1, T, dark)
        if seed == 3:  # a coffee stain
            c.rect(6, 7, 4, 3, shade(base, 0.82))
    elif kind == "raised":  # server room: raised floor panels, some vented
        c.rect(0, 0, T, 1, dark)
        c.rect(0, 0, 1, T, dark)
        c.rect(1, 1, T - 1, 1, light)
        if seed in (1, 3):
            for y in range(4, 13, 2):
                c.rect(4, y, 8, 1, dark)
    elif kind == "cloud":  # soft heaps of cloud, shaded underneath (wrapping, so tiles join up)
        soft = tuple((base[i] * 2 + dark[i]) // 3 for i in range(3)) + (255,)
        for _ in range(3):
            px, py, r = rnd.randrange(T), rnd.randrange(T), rnd.randint(4, 6)
            for y in range(py - r - 1, py + r + 2):
                for x in range(px - r - 1, px + r + 2):
                    d2 = (x - px) ** 2 + (y - py) ** 2
                    if d2 <= r * r and y > py + r // 2 and d2 > (r - 1.2) ** 2:
                        c.put(x % T, y % T, soft)
                    elif d2 <= (r - 3) ** 2 and y < py - 1 and x < px:
                        c.put(x % T, y % T, light)
        if seed == 3:  # a glint of sunshine
            c.put(9, 6, hexc("fff6c8"))
            c.put(10, 6, hexc("fff6c8"))
    elif kind == "sand":  # the sea bed: rippled sand, pebbles, now and then a shell or a starfish
        for y in range(2, T, 5):
            ph = rnd.randint(0, 5)
            for x in range(T):
                c.put(x, (y + (1 if (x + ph) % 6 < 3 else 0)) % T, dark)
        for _ in range(3):
            c.put(rnd.randrange(T), rnd.randrange(T), light)
        if seed == 2:  # a little shell
            c.rect(5, 7, 3, 2, hexc("f2e6d8"))
            c.put(6, 6, hexc("f2e6d8"))
            c.put(6, 8, hexc("d8c0b0"))
        if seed == 3:  # a tiny starfish
            star = hexc("ff8a5a")
            c.rect(10, 9, 3, 1, star)
            c.rect(11, 8, 1, 3, star)
            c.put(9, 11, star)
            c.put(13, 11, star)
    elif kind == "seastone":  # rocky sea floor: flat stones with dark cracks, a little sand between
        c.rect(0, 0, T, 1, dark)
        c.rect(0, 0, 1, T, dark)
        c.rect(1, 1, T - 2, 1, light)
        x = rnd.randint(4, 11)
        for y in range(2, T):
            if rnd.random() < 0.3:
                x += rnd.choice((-1, 1))
            c.put(x % T, y, dark)
        if seed in (1, 3):
            c.rect(rnd.randint(2, 10), rnd.randint(3, 12), 3, 2, shade(base, 1.12))
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
    a, b, d = (hexc(x) for x in cols[:3])  # some kinds take an extra 4th colour
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
    elif kind == "shops":  # town street: pastel shopfronts
        c.rect(0, 0, T, T, a)
        c.rect(0, 0, T, 1, b)
        c.rect(0, 13, T, 1, b)
        c.rect(0, 14, T, 2, d)
        if variant == 1:  # big shop window under a striped awning
            for x in range(T):
                c.rect(x, 1, 1, 3, hexc("c0392b") if (x // 2) % 2 == 0 else hexc("f5f0e6"))
            c.rect(1, 4, 14, 1, shade(a, 0.7))
            c.rect(2, 5, 12, 8, b)
            c.rect(3, 6, 10, 6, GLASS)
            c.rect(4, 7, 2, 1, GLASS_HI)
            c.rect(9, 9, 3, 3, hexc("f2c230"))  # goods in the window
        elif variant == 2:  # shop sign
            c.rect(2, 3, 12, 5, b)
            c.rect(4, 5, 8, 1, hexc("f5f0e6"))
            window(c, 5, 9, 6, 4, b, shutters=False)
        else:
            window(c, 5, 3, 6, 8, b, shutters=False)
    elif kind == "stalls":  # market: wooden stalls with striped awnings
        c.rect(0, 0, T, T, hexc("e8d8b8"))
        for x in range(T):
            c.rect(x, 0, 1, 4, b if (x // 2) % 2 == 0 else d)
        c.rect(0, 4, T, 1, shade(b, 0.7))
        c.rect(0, 8, T, 6, a)
        c.rect(0, 8, T, 1, shade(a, 1.3))
        c.rect(0, 14, T, 2, shade(a, 0.5))
        if variant == 1:  # crates of fruit
            for x0, col in ((1, "e84a3a"), (6, "f2c230"), (11, "6fbf4a")):
                c.rect(x0, 6, 4, 2, hexc(col))
                c.put(x0 + 1, 6, shade(hexc(col), 1.3))
        elif variant == 2:  # lanterns
            for x0 in (3, 11):
                c.rect(x0, 5, 2, 2, hexc("ffd166"))
    elif kind == "brickwall":  # alley: warm bricks, posters, pipes
        c.rect(0, 0, T, T, a)
        for y in range(0, 14, 4):
            c.rect(0, y, T, 1, b)
            sh = 0 if (y // 4) % 2 == 0 else 4
            for x in range(sh, T, 8):
                c.rect(x, y, 1, 4, b)
        c.rect(0, 14, T, 2, shade(b, 0.6))
        if variant == 1:  # poster
            c.rect(4, 2, 8, 10, hexc("f2e8c0"))
            c.rect(5, 3, 6, 4, hexc("4f8ad8"))
            c.rect(5, 8, 6, 1, hexc("c0392b"))
            c.rect(5, 10, 4, 1, hexc("2a2a30"))
        elif variant == 2:  # drainpipe and vent
            c.rect(3, 0, 2, 14, d)
            c.rect(3, 0, 1, 14, shade(d, 1.3))
            c.rect(9, 4, 5, 4, shade(d, 0.8))
            for y in (5, 6):
                c.rect(10, y, 3, 1, hexc("2a2a30"))
    elif kind == "ironfence":  # park: black railings in front of a hedge
        c.rect(0, 0, T, T, b)
        for _ in range(10):
            c.put(random.randrange(T), random.randrange(12), shade(b, 1.3))
        for x in range(1, T, 3):
            c.rect(x, 3, 1, 11, a)
            c.put(x, 2, a)
        c.rect(0, 5, T, 1, a)
        c.rect(0, 12, T, 1, a)
        c.rect(0, 14, T, 2, hexc(cols[3] if len(cols) > 3 else "5e9a40"))  # grass strip (dark at night)
        if variant == 1:  # street lamp
            c.rect(7, 2, 2, 12, a)
            c.rect(6, 0, 4, 3, hexc(cols[2]))
        elif variant == 2:  # flower bed
            for x in range(1, T, 2):
                c.put(x, 13, FLOWERS[x % 4])
                c.put(x + 1, 12, FLOWERS[(x + 1) % 4])
    elif kind in ("cloudbank", "stormbank", "pearlygate"):  # puffy cloud walls, open sky above
        bright = hexc("ffffff") if kind != "stormbank" else shade(a, 1.12)
        dk = shade(b, 0.82)
        puffs = [(1, 8, 4), (6, 6, 4.5), (11, 7, 4), (15, 8, 4)]
        if kind == "pearlygate":  # golden bars between pearly posts, the clouds behind
            puffs = [(1, 11, 3.5), (6, 10, 3.5), (11, 11, 3.5), (15, 10, 3.5)]
        for x, y, r in puffs:
            disc(c, x, y + 1, r, b)
        for x, y, r in puffs:
            disc(c, x - 0.3, y, r - 1, a)
            disc(c, x - 1, y - 1.5, max(1, r - 3), bright)
        c.rect(0, 11, T, 3, a)
        c.rect(0, 13, T, 1, b)
        c.rect(0, 14, T, 2, dk)
        if kind == "pearlygate":
            gold = hexc(cols[2])
            for x in range(1, T, 3):
                c.rect(x, 2, 1, 11, gold)
                c.put(x, 1, hexc("fff0a0"))
            c.rect(0, 4, T, 1, gold)
            if variant == 1:  # a pearly post
                c.rect(6, 0, 4, 14, hexc("f5f0f8"))
                c.rect(6, 0, 1, 14, hexc("ffffff"))
                disc(c, 8, 1, 2.5, hexc("f5f0f8"))
        elif variant == 1 and kind == "cloudbank":  # a golden star twinkling in the sky above
            star = hexc(cols[2])
            c.rect(9, 1, 1, 3, star)
            c.rect(8, 2, 3, 1, star)
        elif variant == 2 and kind == "cloudbank":  # a little rainbow over the cloud
            for i, col in enumerate(("e84a4a", "f2e05a", "6ac85a", "4a9ae8")):
                for x in range(3, 14):
                    y = 6 - i + int(((x - 8.5) ** 2) / 8)
                    if y < 9 - i:
                        c.put(x, y, hexc(col))
        elif variant == 2:  # the storm: a lightning bolt
            bolt = hexc(cols[2])
            for x, y in ((9, 0), (8, 1), (8, 2), (7, 3), (8, 3), (9, 3), (8, 4), (7, 5), (7, 6)):
                c.put(x, y, bolt)
    elif kind in ("reef", "kelp", "cavewall"):  # the sea's walls: rock, with coral, kelp or glowing crystals
        accent = hexc(cols[2])
        c.rect(0, 0, T, T, a)
        rnd = random.Random(variant * 13 + len(kind))
        for _ in range(5):                     # boulders
            x, y, r = rnd.randrange(T), rnd.randrange(2, 13), rnd.uniform(2, 3.5)
            disc(c, x, y, r, shade(a, 1.12))
            c.put(int(x - r / 2), int(y - r / 2), shade(a, 1.3))
        if kind == "reef":
            for x in range(0, T, 3):            # coral along the top
                h = rnd.randint(2, 5)
                c.rect(x, 12 - h, 2, h, accent if x % 2 else shade(accent, 0.8))
                c.put(x, 11 - h, shade(accent, 1.2))
            if variant == 1:                   # a big pink fan coral
                for i in range(7):
                    c.rect(4 + i, 2 + abs(3 - i), 1, 9 - abs(3 - i), hexc("ff6aa8"))
            elif variant == 2:                 # a sea anemone
                for i in range(5):
                    c.rect(5 + i * 2, 4 + (i % 2), 1, 5, hexc("c88aff"))
                c.rect(5, 9, 9, 2, hexc("8a5ad8"))
        elif kind == "kelp":
            for x in range(1, T, 4):            # kelp swaying up from the floor
                for y in range(0, 14):
                    c.put(x + (1 if (y // 3) % 2 else 0), y, accent if y % 4 else shade(accent, 1.25))
            if variant == 1:                   # a fish swimming past
                fish = hexc("f2c230")
                c.rect(6, 5, 4, 2, fish)
                c.put(10, 4, fish)
                c.put(10, 7, fish)
                c.put(6, 5, hexc("1b1b2a"))
            elif variant == 2:                 # bubbles rising
                for x, y in ((7, 9), (8, 6), (7, 3), (8, 1)):
                    c.put(x, y, hexc("bfefff"))
        else:  # cavewall: dark rock with glowing crystals (the lair: old bones)
            if variant >= 1:
                for x, y in ((4, 6), (10, 4), (12, 9)) if variant == 1 else ((6, 5), (11, 8)):
                    c.rect(x, y, 2, 4, accent)
                    c.put(x, y - 1, shade(accent, 1.2))
        c.rect(0, 13, T, 1, b)
        c.rect(0, 14, T, 2, d)
    elif kind == "hull":  # a sunken ship's side: planks, a porthole, a rusty chain
        c.rect(0, 0, T, T, a)
        for y in range(0, 13, 3):
            c.rect(0, y + 2, T, 1, b)
            c.put((y * 5) % T, y + 1, b)
        if variant == 1:  # a porthole, the brass rim green with age
            disc(c, 8, 6, 4, hexc(cols[2]))
            disc(c, 8, 6, 3, hexc("1a4a6e"))
            c.put(7, 5, hexc("6ac0e0"))
        elif variant == 2:  # a chain hanging down
            for y in range(0, 12, 2):
                c.rect(7 + (y // 2) % 2, y, 2, 1, hexc("8a6a4a"))
        c.rect(0, 13, T, 1, b)
        c.rect(0, 14, T, 2, d)
    elif kind == "columns":  # town hall: stone with pilasters
        c.rect(0, 0, T, T, a)
        c.rect(0, 0, 4, T, b)
        c.rect(1, 0, 1, T, shade(b, 0.85))
        c.rect(0, 0, T, 2, b)
        c.rect(0, 12, T, 2, b)
        c.rect(0, 14, T, 2, shade(b, 0.6))
        if variant == 1:  # red banner with a gold emblem
            c.rect(7, 2, 6, 10, d)
            c.put(9, 12, d)
            c.put(10, 12, d)
            c.rect(9, 5, 2, 3, hexc("f2c230"))
        elif variant == 2:  # clock
            c.rect(7, 3, 7, 7, hexc("3a3a44"))
            c.rect(8, 4, 5, 5, hexc("f5f0e6"))
            c.rect(10, 5, 1, 2, hexc("2a2a30"))
            c.rect(10, 6, 2, 1, hexc("2a2a30"))
    elif kind == "skyscraper":  # city at night: a grid of windows, some lit
        c.rect(0, 0, T, T, a)
        rnd = random.Random(variant * 13 + 7)
        for y in (2, 7):
            for x in (1, 5, 9, 13):
                lit = rnd.random() < (0.55 if variant == 1 else 0.3)
                c.rect(x, y, 2, 3, d if lit else b)
                if lit:
                    c.put(x, y, shade(d, 1.15))
        c.rect(0, 13, T, 1, b)
        c.rect(0, 14, T, 2, shade(b, 0.6))
        if variant == 2:  # neon sign
            c.rect(2, 11, 12, 2, hexc("ff5fa2"))
            c.rect(3, 11, 10, 1, hexc("ffc0dc"))
    elif kind == "tiles":  # subway wall: tiles with a blue stripe
        c.rect(0, 0, T, T, a)
        for y in range(0, T, 4):
            c.rect(0, y, T, 1, b)
        for x in range(0, T, 4):
            c.rect(x, 0, 1, 12, b)
        c.rect(0, 8, T, 2, d)
        c.rect(0, 14, T, 2, shade(b, 0.6))
        if variant == 1:  # subway map
            c.rect(3, 1, 10, 6, hexc("f5f0e6"))
            c.rect(4, 3, 8, 1, hexc("c0392b"))
            c.rect(7, 2, 1, 4, hexc("4f8ad8"))
        elif variant == 2:  # clock
            c.rect(6, 1, 5, 5, hexc("2a2a30"))
            c.rect(7, 2, 3, 3, hexc("f5f0e6"))
    elif kind == "parapet":  # rooftop edge, AC units and antennas
        c.rect(0, 6, T, 8, a)
        c.rect(0, 6, T, 1, shade(a, 1.2))
        c.rect(0, 14, T, 2, b)
        c.rect(0, 0, T, 6, hexc("141a2e"))     # sky above the parapet
        c.put(3, 2, hexc("f5f0e6"))
        c.put(12, 4, hexc("f5f0e6"))
        if variant == 1:  # air conditioning unit
            c.rect(3, 3, 9, 7, b)
            c.rect(4, 4, 7, 5, shade(b, 0.8))
            for y in range(5, 9, 2):
                c.rect(4, y, 7, 1, shade(b, 1.3))
        elif variant == 2:  # antenna with a blinking light
            c.rect(8, 0, 1, 7, b)
            c.rect(7, 0, 3, 1, hexc(cols[2]))
    elif kind == "hoarding":  # construction fence with warning stripes
        c.rect(0, 0, T, T, a)
        for x in range(0, T, 4):
            c.rect(x, 0, 1, 12, b)
        for x in range(0, T):  # hazard stripes along the bottom
            c.rect(x, 11, 1, 3, d if ((x // 2) % 2 == 0) else hexc("1e1e22"))
        c.rect(0, 14, T, 2, shade(b, 0.6))
        if variant == 1:  # "keep out" sign
            c.rect(3, 2, 10, 6, hexc("f5f0e6"))
            c.rect(4, 4, 8, 2, hexc("c0392b"))
        elif variant == 2:  # work light
            c.rect(7, 2, 3, 3, hexc("ffd166"))
            c.rect(8, 5, 1, 6, hexc("3a3a44"))
    elif kind == "neonshop":  # pawn shop: dark wall, neon "OPEN", shelves
        c.rect(0, 0, T, T, a)
        c.rect(0, 9, T, 1, b)
        for x in range(1, T - 1, 3):
            c.rect(x, 6, 2, 3, [hexc("c9ccd6"), hexc("ffd166"), hexc("8ed8ff")][x % 3])
        c.rect(0, 14, T, 2, shade(b, 0.6))
        if variant == 1:
            c.rect(2, 1, 12, 4, hexc(cols[2]))
            c.rect(3, 2, 10, 2, shade(a, 0.7))
            for x in (4, 7, 10):
                c.rect(x, 2, 2, 2, hexc("ffc0dc"))
    elif kind == "glasswall":  # lobby: tall glass panes in steel frames, sky beyond
        c.rect(0, 0, T, T, a)
        c.rect(0, 0, T, 6, shade(a, 1.15))
        c.rect(0, 0, 1, 14, b)
        c.rect(8, 0, 1, 14, b)
        c.rect(0, 13, T, 1, b)
        c.rect(0, 14, T, 2, shade(b, 0.6))
        c.rect(2, 2, 2, 4, hexc("d8f0ff"))
        if variant == 1:  # a potted plant against the glass
            c.rect(10, 9, 4, 4, hexc("8a5a3a"))
            for x, y in ((9, 6), (11, 4), (13, 6), (12, 7), (10, 7)):
                c.rect(x, y, 2, 3, d)
        elif variant == 2:  # the company logo
            c.rect(4, 4, 8, 6, hexc("f5f0e6"))
            c.rect(5, 5, 6, 4, hexc("ffd166"))
            c.rect(7, 6, 2, 2, b)
    elif kind == "officewall":  # office: painted wall, skirting, a whiteboard or a window
        c.rect(0, 0, T, T, a)
        c.rect(0, 12, T, 2, b)
        c.rect(0, 14, T, 2, shade(b, 0.6))
        if variant == 1:  # window with blinds
            c.rect(2, 1, 12, 9, b)
            c.rect(3, 2, 10, 7, hexc("8ec5e8"))
            for y in range(2, 9, 2):
                c.rect(3, y, 10, 1, hexc("d8dce4"))
        elif variant == 2:  # whiteboard with a chart
            c.rect(2, 2, 12, 8, hexc("f5f5f5"))
            c.rect(2, 9, 12, 1, b)
            for i, h in enumerate((2, 4, 3, 6)):
                c.rect(4 + i * 2, 8 - h, 1, h, hexc(cols[2]))
    elif kind == "serverrack":  # server room: racks of servers with blinking lights
        c.rect(0, 0, T, T, b)
        for x in (1, 9):
            c.rect(x, 1, 6, 12, a)
            for y in range(2, 12, 2):
                c.rect(x + 1, y, 4, 1, shade(a, 1.4))
                if (x + y + variant) % 3 == 0:
                    c.put(x + 4, y, d)
                elif (x + y) % 5 == 0:
                    c.put(x + 4, y, hexc("ffd166"))
        c.rect(0, 14, T, 2, shade(b, 0.6))
    elif kind == "parapetday":  # the roof's edge by day: concrete parapet with the sky above
        c.rect(0, 0, T, 6, hexc("7ec8f0"))
        c.rect(0, 6, T, 8, a)
        c.rect(0, 6, T, 1, shade(a, 1.15))
        c.rect(0, 14, T, 2, b)
        if variant == 1:  # a cloud drifting by
            c.rect(3, 2, 7, 2, hexc("ffffff"))
            c.rect(5, 1, 3, 1, hexc("ffffff"))
        elif variant == 2:  # warning light
            c.rect(7, 3, 2, 3, b)
            c.rect(7, 2, 2, 1, hexc(cols[2]))
    elif kind == "shelves":  # shop: shelves of colourful goods
        c.rect(0, 0, T, T, b)
        goods = [hexc("e84a3a"), hexc("4f8ad8"), hexc("f2c230"), hexc("6fbf4a"), hexc("e88fb4")]
        rnd = random.Random(variant + 3)
        for y in (5, 10):
            c.rect(0, y, T, 1, a)
            for x in range(1, T - 1, 3):
                if rnd.random() < 0.8:
                    hgt = rnd.randint(2, 3)
                    c.rect(x, y - hgt, 2, hgt, goods[rnd.randrange(len(goods))])
        c.rect(0, 13, T, 1, a)
        c.rect(0, 14, T, 2, d)
        if variant == 1:  # price tag
            c.rect(6, 11, 4, 2, hexc("f5f0e6"))
    elif kind == "store":  # Baa-llo's shop: shelves of goods, bunting in the stage's colour, a coin sign
        accent = hexc(cols[3])
        c.rect(0, 0, T, T, b)
        goods = [hexc("e84a3a"), hexc("4f8ad8"), hexc("f2c230"), hexc("6fbf4a"), hexc("e88fb4")]
        rnd = random.Random(variant + 11)
        for y in (6, 11):
            c.rect(0, y, T, 1, a)
            c.rect(0, y + 1, T, 1, shade(a, 0.7))
            for x in range(1, T - 1, 3):
                if rnd.random() < 0.75:
                    hgt = rnd.randint(2, 3)
                    c.rect(x, y - hgt, 2, hgt, goods[rnd.randrange(len(goods))])
        c.rect(0, 13, T, 1, a)
        c.rect(0, 14, T, 2, d)
        if variant == 1:  # bunting across the top
            c.rect(0, 0, T, 1, shade(accent, 0.7))
            for x0 in (0, 5, 10):
                c.rect(x0 + 1, 1, 3, 1, accent)
                c.put(x0 + 2, 2, accent)
        elif variant == 2:  # a hanging coin sign
            c.rect(0, 0, T, 6, b)
            c.rect(5, 0, 1, 1, d)
            c.rect(10, 0, 1, 1, d)
            disc(c, 7.5, 3.5, 3.2, COIN_SH)
            disc(c, 7.3, 3.3, 2.6, COIN_GOLD)
            c.rect(7, 3, 1, 1, POM)
    return c.img


def cap_tile(kind, cols):
    a, b, l = (hexc(x) for x in cols)
    c = Canvas(T, T)
    if kind == "open":  # see-through: the game draws the open sky behind the room
        return c.img
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
    elif kind == "nightsky":  # dark sky with stars
        for _ in range(6):
            c.put(rnd.randrange(T), rnd.randrange(T), l if rnd.random() < 0.7 else hexc("ffd166"))
        c.put(rnd.randrange(T), rnd.randrange(T), b)
    elif kind == "ceiling":  # office ceiling tiles with a light panel
        c.rect(0, 0, T, 1, b)
        c.rect(0, 0, 1, T, b)
        c.rect(0, 8, T, 1, b)
        c.rect(8, 0, 1, T, b)
        c.rect(10, 10, 5, 5, l)
    elif kind == "sky":  # open sky around the roof, with wisps of cloud
        for _ in range(3):
            x, y = rnd.randrange(-2, T), rnd.randrange(T)
            c.rect(x, y, rnd.randint(4, 7), 1, b)
        if rnd.random() < 0.5:
            c.rect(rnd.randrange(T - 6), rnd.randrange(T - 3), 6, 2, l)
    elif kind == "deepwater":  # deep water all round, light rippling across it
        for _ in range(4):
            x, y = rnd.randrange(-2, T), rnd.randrange(T)
            c.rect(x, y, rnd.randint(3, 6), 1, b)
        c.put(rnd.randrange(T), rnd.randrange(T), l)
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
    guns_sheet().save(os.path.join(ROOT, "sprites", "guns.png"))
    with open(os.path.join(ROOT, "sprites", "guns.txt"), "w") as f:  # name x y w h gripX gripY
        for k, v in GUNS.items():
            f.write(k + " " + " ".join(str(n) for n in v) + "\n")
    items_sheet().save(os.path.join(ROOT, "sprites", "items.png"))
    with open(os.path.join(ROOT, "sprites", "items.txt"), "w") as f:  # name x y w h
        for k, v in ITEMS.items():
            f.write(k + " " + " ".join(str(n) for n in v) + "\n")
    for name, th in THEMES.items():
        tileset(name, th).save(os.path.join(ROOT, "tiles", name + ".png"))
    print("props atlas:")
    for k, v in PROPS.items():
        print(f"  {k}: {v}")


if __name__ == "__main__":
    main()
