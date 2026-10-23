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
             ("debris", debris()), ("exit", exit_hatch()), ("bench", bench()), ("lamp", lamp()), ("car", car()), ("pedestal", pedestal()),
             ("pit", pit())]
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


GUN_SPRITES = [("rusty_sidearm", gun_rusty_sidearm), ("fluff_shotgun", gun_fluff_shotgun),
               ("pocket_uzi", gun_pocket_uzi), ("big_magnum", gun_big_magnum),
               ("confetti_popper", gun_confetti_popper), ("bubble_blaster", gun_bubble_blaster),
               ("banana_rang", gun_banana_rang), ("bee_hive", gun_bee_hive), ("rainbow_ray", gun_rainbow_ray)]


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


def item_magnet():
    c = Canvas(16, 16)
    c.rect(2, 2, 4, 9, hexc("d63a2f"))
    c.rect(10, 2, 4, 9, hexc("d63a2f"))
    c.rect(2, 9, 12, 4, hexc("d63a2f"))
    c.rect(6, 2, 4, 7, (0, 0, 0, 0))
    c.rect(2, 1, 4, 3, hexc("c9ccd6"))        # tips
    c.rect(10, 1, 4, 3, hexc("c9ccd6"))
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


ITEM_SPRITES = [("slippers", item_slippers), ("pocket", item_pocket), ("medkit", item_medkit),
                ("magnet", item_magnet), ("clips", item_clips), ("hotsauce", item_hotsauce),
                ("clover", item_clover), ("grenade", item_grenade), ("shield", item_shield),
                ("ghost", item_ghost), ("candy", item_candy)]


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
    elif kind == "nightsky":  # dark sky with stars
        for _ in range(6):
            c.put(rnd.randrange(T), rnd.randrange(T), l if rnd.random() < 0.7 else hexc("ffd166"))
        c.put(rnd.randrange(T), rnd.randrange(T), b)
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
