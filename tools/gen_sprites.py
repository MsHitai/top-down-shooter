#!/usr/bin/env python3
"""Generates the pixel-art sprites and room tilesets in src/main/resources/assets/.

Run from the project root:  python3 tools/gen_sprites.py
Needs Pillow. Art scale: 32 px = 1 world unit (the world is 7x5 units -> 224x160 px).

Outputs
  sprites/player.png   family sheets, 40x56 source frames at 64 px/world unit
                       (20x28 logical anchors, all coordinates below are logical):
                       row 0 = front (aiming down) walk 0-3, idle 4-5
                       row 1 = back (aiming up)    walk 0-3, idle 4-5
                       row 2 = roll tuck (col 0)
                       row 3 = death: hit, knees buckle, slump (cols 0-2), then a 28x20
                               "lying on his back" frame at (60, 3*28+8), head to the right
  sprites/guns.png     64 px/unit gun art + guns.txt (name x y w h gripX gripY);
                       atlas rectangles use source pixels, grips stay logical 32 px/unit
  sprites/items.png    16x16 item icons + items.txt (name x y w h)
  sprites/bullet_kin.png, shotgun_kin.png   32x40 frames at 64 px/world unit:
                       six poses, normal/angry/white damage-flash rows
  sprites/kin_weapons.png   two 32x16 held weapons at the same scale
  sprites/props.png    64 px/unit tables (normal + flipped 4 ways), barrel, crate, hay bale, stone pillar,
                       broken-plank debris, chests per tier (chest_d/c/b/a + _open) and the
                       floor exit hatch - see PROPS below for the atlas rects
  tiles/<theme>.png    room tileset per theme, 16x16 tiles in one row:
                       floor a, floor b, floor c, floor d, wall face, cap (wall top / border),
                       door frame, (unused), wall face with window, wall face with decoration
"""
import math
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets")
OUTLINE = (30, 20, 22, 255)


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, k):
    return (max(0, min(255, int(c[0] * k))), max(0, min(255, int(c[1] * k))), max(0, min(255, int(c[2] * k))), c[3])


def finish_surface(img, strength=0.12):
    """Connected bevels and broad shade planes; same alpha, size and pixel grid.

    Works per sprite before packing, so neighbouring atlas regions cannot bleed.
    Tiny lettering, eyes and dark outline pixels retain their original colours.
    """
    result = img.copy()
    src, dst = img.load(), result.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            c = src[x, y]
            if c[3] < 240 or max(c[:3]) < 65:
                continue
            # Only broad connected material gets a bevel. Keep small painted details.
            horizontal = x > 0 and x + 1 < w and src[x-1, y] == c and src[x+1, y] == c
            vertical = y > 0 and y + 1 < h and src[x, y-1] == c and src[x, y+1] == c
            if not (horizontal or vertical):
                continue
            top = y == 0 or src[x, y-1] != c
            left = x == 0 or src[x-1, y] != c
            bottom = y + 1 == h or src[x, y+1] != c
            right = x + 1 == w or src[x+1, y] != c
            light = 1.0 - strength * 0.32 * (y / max(1, h-1))
            if top and horizontal:
                light += strength * 0.6
            elif left and vertical:
                light += strength * 0.28
            if bottom and horizontal or right and vertical:
                light -= strength * 0.65
            dst[x, y] = shade(c, light)
    return result


def detailed_sprite(img, material="wood"):
    """64 px/unit art: preserve the silhouette and add one-source-pixel bevels.

    Connected highlights follow painted panels instead of sprinkling texture noise.
    Atlas anchors remain logical 32 px/unit coordinates.
    """
    result = img.resize((img.width * 2, img.height * 2), Image.Resampling.NEAREST)
    result = finish_surface(result, 0.20)
    src, dst = img.load(), result.load()
    for y in range(1, img.height - 1):
        x = 1
        while x < img.width - 1:
            col = src[x, y]
            end = x + 1
            while end < img.width - 1 and src[end, y] == col:
                end += 1
            # Broad surfaces get an inset edge; eyes, locks and thin trim stay intact.
            if end - x >= 5 and col[3] == 255 and max(col[:3]) > 75:
                if src[x, y - 1] != col and src[x, y + 1] == col:
                    for xx in range(x * 2 + 2, end * 2 - 2):
                        dst[xx, y * 2 + 1] = shade(col, 1.18 if material == "metal" else 1.10)
                if src[x, y + 1] != col and src[x, y - 1] == col:
                    for xx in range(x * 2 + 1, end * 2 - 1):
                        dst[xx, y * 2 + 1] = shade(col, 0.73)
            x = end
    return result


def detailed_prop(name, img):
    if name.startswith(("pit", "coin", "exit")) or name in ("drum_fire", "debris"):
        return img.resize((img.width * 2, img.height * 2), Image.Resampling.NEAREST)
    metal = name.startswith("chest") or name in ("lamp", "column", "pillar", "cooler", "drum", "car", "dumpster")
    result = detailed_sprite(img, "metal" if metal else "wood")
    c = Canvas(result.width, result.height)
    c.img = result
    c.px = result.load()
    if name.startswith("chest_"):
        body, trim = CHEST_TIERS[name.split("_")[1]]
        # Inset front panels, a deep right side, raised straps and four individual rivets.
        c.rect(3, 21, 4, 12, shade(body, 0.87))
        c.rect(37, 20, 4, 16, shade(body, 0.65))
        c.rect(13, 26, 18, 7, shade(body, 0.91))
        c.rect(14, 26, 16, 1, shade(body, 1.14))
        for x in (8, 32):
            c.rect(x, 21, 1, 16, shade(trim, 1.17))
            c.rect(x + 3, 21, 1, 16, shade(trim, 0.66))
            for y in (23, 34):
                c.rect(x + 1, y, 2, 2, shade(trim, 0.6))
                c.put(x + 1, y, shade(trim, 1.3))
        if name.endswith("_open"):
            c.rect(5, 10, 34, 2, shade(body, 0.6))
            for x in (11, 31): c.rect(x, 8, 3, 4, shade(trim, 0.8))
        else:
            c.rect(6, 8, 32, 2, shade(body, 1.2))
            c.rect(7, 10, 1, 5, shade(body, 0.85))
            c.rect(18, 12, 8, 1, shade(trim, 1.25))
            c.rect(24, 13, 2, 8, shade(trim, 0.7))
    elif name == "pillar":
        for x in (8, 15, 23):
            c.rect(x, 11, 1, 8, shade(STONE, 0.75))
            c.rect(x + 1, 11, 1, 8, shade(STONE, 1.1))
        c.rect(13, 37, 1, 6, STONE_SH); c.rect(14, 42, 4, 1, STONE_SH)
        c.rect(5, 53, 26, 1, STONE)
    elif name == "column":
        for x in (11, 17, 23):
            c.rect(x, 15, 1, 30, hexc("b4b0b5"))
            c.rect(x + 1, 15, 1, 30, hexc("fff5df"))
    elif name in ("table", "bench", "desk", "crate", "barrel", "dumpster", "drum", "cooler"):
        # Joinery/fasteners sit inside the silhouette, never changing cover geometry.
        for x, y in ((6, 8), (c.w - 8, 8), (6, c.h - 10), (c.w - 8, c.h - 10)):
            col = c.px[x, y]
            if col[3] and max(col[:3]) > 65:
                c.rect(x, y, 2, 2, shade(col, 0.65))
                c.put(x, y, shade(col, 1.3))
    return c.img


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


# The family (player/Hero): the boy is the hero above; his sister, granny and grandad are the same
# body in their own clothes, with their own hair drawn over the head.
BOY = {"shirt": (SHIRT, SHIRT_HI, SHIRT_SH), "pants": (PANTS, PANTS_SH), "shoe": (SHOE, SHOE_SH), "hair": None}

NINJA_HAIR = hexc("2a1a14")
BAND = hexc("d63a2f")
GREY_HAIR = hexc("c8c8d4")
GREY_HAIR_SH = hexc("9a9aa8")
SPECS = hexc("4a4a5a")


def sister_hair(c, hx, hy, back):
    """A dark ponytail and a red ninja headband with its tails flying out behind."""
    c.rect(hx + 1, hy - 1, 8, 3, NINJA_HAIR)          # hair on top
    c.rect(hx, hy, 10, 2, NINJA_HAIR)
    c.rect(hx, hy + 2, 10, 1, BAND)                   # the headband
    if back:
        c.rect(hx + 1, hy + 3, 8, 5, NINJA_HAIR)
        c.rect(hx + 3, hy + 8, 4, 3, NINJA_HAIR)      # ponytail hanging down
        c.rect(hx + 6, hy + 2, 2, 2, BAND)            # the knot, off to one side
        c.rect(hx + 8, hy + 3, 2, 1, BAND)            # its tails
        c.put(hx + 10, hy + 4, BAND)
    else:
        c.rect(hx, hy + 3, 2, 3, NINJA_HAIR)          # hair at the back of the head
        c.rect(hx - 3, hy + 1, 3, 2, NINJA_HAIR)      # the ponytail sticking out
        c.put(hx - 4, hy + 2, NINJA_HAIR)
        c.put(hx - 1, hy + 2, BAND)                   # headband tails fluttering
        c.rect(hx - 3, hy + 3, 2, 1, BAND)
        c.put(hx - 4, hy + 4, BAND)


def granny_hair(c, hx, hy, back):
    """Grey curls with a bun on top and round glasses."""
    c.rect(hx, hy - 1, 10, 3, GREY_HAIR)
    c.rect(hx + 3, hy - 2, 4, 2, GREY_HAIR)           # the bun
    c.rect(hx + 2, hy - 1, 6, 1, GREY_HAIR_SH)
    if back:
        c.rect(hx, hy + 2, 10, 5, GREY_HAIR)
        c.rect(hx + 1, hy + 7, 8, 1, GREY_HAIR_SH)
    else:
        c.rect(hx, hy + 2, 3, 4, GREY_HAIR)           # curls over the ear
        c.put(hx + 1, hy + 6, GREY_HAIR_SH)
        c.rect(hx + 4, hy + 3, 6, 1, SPECS)           # glasses: the rim over both eyes
        c.put(hx + 4, hy + 4, SPECS)
        c.put(hx + 7, hy + 4, SPECS)                  # (the bridge)
        c.put(hx + 6, hy + 4, (200, 230, 255, 255))   # a glint
        c.put(hx + 9, hy + 4, (200, 230, 255, 255))


def grandad_hair(c, hx, hy, back):
    """Bald on top (like the boy - it runs in the family), white hair round the sides, a big moustache."""
    if back:
        c.rect(hx, hy + 3, 10, 4, GREY_HAIR)
        c.rect(hx + 1, hy + 7, 8, 1, GREY_HAIR_SH)
    else:
        c.rect(hx, hy + 2, 2, 5, GREY_HAIR)           # white hair round the ear
        c.put(hx + 2, hy + 3, GREY_HAIR)
        c.rect(hx + 5, hy + 2, 2, 1, GREY_HAIR)       # bushy eyebrows
        c.rect(hx + 8, hy + 2, 2, 1, GREY_HAIR)
        c.rect(hx + 5, hy + 6, 5, 2, GREY_HAIR)       # the moustache
        c.put(hx + 10, hy + 7, GREY_HAIR)
        c.put(hx + 6, hy + 7, GREY_HAIR_SH)


SISTER = {"shirt": (hexc("2e2e44"), hexc("4a4a68"), hexc("1e1e2e")), "pants": (hexc("2a2a3a"), hexc("1a1a26")),
          "shoe": (hexc("1e1e2a"), hexc("12121a")), "hair": sister_hair}
GRANNY = {"shirt": (hexc("9a7ac0"), hexc("b89ad8"), hexc("7a5aa0")), "pants": (hexc("5a4a7a"), hexc("443860")),
          "shoe": (hexc("6a4a3a"), hexc("4a3428")), "hair": granny_hair, "skirt": True}
GRANDAD = {"shirt": (hexc("b08a5a"), hexc("c8a474"), hexc("8a6a44")), "pants": (hexc("6a6a74"), hexc("4e4e58")),
           "shoe": (hexc("5a3a2a"), hexc("3e281c")), "hair": grandad_hair}
PAMPER = hexc("f4f6fa")
PAMPER_SH = hexc("c8d0dc")
TAPE = hexc("7ab8f0")
CURL = hexc("ffd84a")
CURL_SH = hexc("d8a82a")
PACIFIER = hexc("ff8ac0")
PACIFIER_RING = hexc("6ab0f0")
# the baby brother (easy mode): bare skin, pampers, bare feet - his frame is drawn by baby_frame
BABY = {"shirt": (SKIN, SKIN_HI, SKIN_SH), "pants": (PAMPER, PAMPER_SH), "shoe": (SKIN, SKIN_SH), "hair": None,
        "baby": True}
# the Fluffy King, pocket size (the shrinking potion - he plays as the family's size once all four wear
# golden skull crowns): a pink ball of fluff on little feet, his gold crown, a gentle smile like on the
# true ending's picture. Drawn by king_frame, his roll by king_tuck.
KING_FLUFF = hexc("ffd6e8")
KING_FLUFF_HI = hexc("ffe6ef")
KING_FLUFF_SH = hexc("e6a4c0")
KING_FLUFF_DK = hexc("e07aa8")
KING_GOLD = hexc("ffc83d")
KING_GOLD_SH = hexc("d8962a")
KING_GEM = hexc("e84393")
KING = {"shirt": (KING_FLUFF, KING_FLUFF_HI, KING_FLUFF_SH), "pants": (KING_FLUFF_SH, KING_FLUFF_DK),
        "shoe": (KING_FLUFF_SH, KING_FLUFF_DK), "hair": None, "king": True}


def king_fluff(c, cx, cy):
    """A connected fluffy silhouette with three broad light planes, not white speckles."""
    mask = Canvas(c.w, c.h)
    disc(mask, cx, cy, 6.5, KING_FLUFF)
    for dx, dy in ((-6, 0), (6, 0), (-4, -5), (4, -5), (-4, 5), (4, 5), (0, -6), (0, 6)):
        disc(mask, cx + dx, cy + dy, 2, KING_FLUFF)
    for y in range(c.h):
        for x in range(c.w):
            if mask.px[x, y][3] == 0:
                continue
            dx, dy = x - cx, y - cy
            plane = dx * 0.4 + dy * 0.7
            col = KING_FLUFF_HI if plane < -3.3 else KING_FLUFF_SH if plane > 3.7 else KING_FLUFF
            c.put(x, y, col)
    # A single soft underside crease holds the body together.
    c.rect(cx - 2, cy + 6, 4, 1, KING_FLUFF_SH)


def king_crown(c, x, y, back):
    """His gold crown, three points and a pink gem (round the back, no gem)."""
    c.rect(x, y + 3, 9, 2, KING_GOLD)
    c.rect(x, y + 4, 9, 1, KING_GOLD_SH)
    c.rect(x, y + 1, 2, 2, KING_GOLD)
    c.rect(x + 7, y + 1, 2, 2, KING_GOLD)
    c.rect(x + 4, y, 1, 3, KING_GOLD)
    c.rect(x + 3, y + 1, 3, 2, KING_GOLD)
    c.rect(x + 1, y + 3, 7, 1, hexc("ffe69b"))
    c.put(x + 1, y + 1, hexc("fff1bd"))
    if not back:
        c.put(x + 4, y + 3, KING_GEM)


def king_frame(back=False, leg_phase=0, bob=0, kneel=0, dead=False):
    c = Canvas(FW, FH)
    by = bob + kneel
    lift_l = {0: 0, 1: 0, 2: 1, 3: 2, 4: 1}[leg_phase]
    lift_r = {0: 0, 1: 2, 2: 1, 3: 0, 4: 1}[leg_phase]
    for lx, lift in ((6, lift_l), (11, lift_r)):       # little pink feet
        c.rect(lx, 24 - lift, 4, 2, KING_FLUFF_SH)
        c.rect(lx, 25 - lift, 4, 1, KING_FLUFF_DK)
    cx, cy = 10, 16 + by
    king_fluff(c, cx, cy)
    king_crown(c, 6, 3 + by, back)
    if not back:                                        # the face, turned a little to the right
        if dead:
            for ex in (cx, cx + 3):
                for dx, dy in ((-1, -1), (1, -1), (0, 0), (-1, 1), (1, 1)):
                    c.put(ex + dx, cy - 2 + dy, EYE)
            c.rect(cx + 1, cy + 2, 2, 2, EYE)
        else:
            for ex in (cx, cx + 3):
                c.rect(ex - 1, cy - 3, 3, 3, hexc("77445f"))
                c.rect(ex - 1, cy - 3, 2, 2, hexc("fff5ef"))
                c.rect(ex, cy - 3, 1, 2, EYE)
            c.rect(cx + 1, cy + 1, 2, 1, EYE)           # a gentle smile, its ends turned up
            c.put(cx, cy, EYE)
            c.put(cx + 3, cy, EYE)
            c.put(cx - 2, cy - 1, KING_FLUFF_DK)        # rosy cheeks
            c.put(cx + 5, cy - 1, KING_FLUFF_DK)
    c.outline()
    return c.img


def king_tuck():
    """Rolled up: a ball of fluff, the crown still on top."""
    c = Canvas(FW, FH)
    king_fluff(c, 10, 17)
    king_crown(c, 6, 6, False)
    c.outline()
    return c.img


FAMILY = {"player": BOY, "player_sister": SISTER, "player_granny": GRANNY, "player_grandad": GRANDAD,
          "player_baby": BABY, "player_king": KING}


def baby_frame(back=False, leg_phase=0, bob=0, kneel=0, dead=False):
    """The baby brother: a big head on a little round body, pampers, chubby bare legs, a yellow curl on
    top and a pacifier. Shorter than the others - the head sits a few pixels lower in the same frame."""
    c = Canvas(FW, FH)
    by = bob + kneel
    lift_l = {0: 0, 1: 0, 2: 1, 3: 1, 4: 1}[leg_phase]
    lift_r = {0: 0, 1: 1, 2: 1, 3: 0, 4: 1}[leg_phase]
    for lx, lift in ((6, lift_l), (11, lift_r)):     # chubby legs and bare feet
        top = min(23 + by, 24)
        bottom = 25 - lift
        c.rect(lx, top, 3, max(1, bottom - top), SKIN)
        c.rect(lx + 2, top, 1, max(1, bottom - top), SKIN_SH)
        c.rect(lx, bottom, 3 + (0 if back else 1), 2, SKIN)
        c.rect(lx, bottom + 1, 3 + (0 if back else 1), 1, SKIN_SH)
    c.rect(6, 15 + by, 8, 6, SKIN)                   # the round little body
    c.rect(5, 16 + by, 10, 4, SKIN)
    c.rect(6, 15 + by, 2, 5, SKIN_HI)
    c.rect(13, 16 + by, 2, 4, SKIN_SH)
    c.rect(4, 16 + by, 1, 3, SKIN_HI)                # stubby arms
    c.rect(15, 16 + by, 1, 3, SKIN_SH)
    if not back:
        c.put(10, 18 + by, SKIN_SH)                  # belly button
    c.rect(5, 20 + by, 10, 4 - min(kneel, 2), PAMPER)  # the pampers
    c.rect(5, 23 + by - min(kneel, 2), 10, 1, PAMPER_SH)
    c.rect(13, 20 + by, 2, 3, PAMPER_SH)
    if not back:
        c.put(5, 20 + by, TAPE)                      # the sticky tabs
        c.put(14, 20 + by, TAPE)
    hx, hy = 5, 5 + by                               # the head: as big as everyone else's
    c.rect(hx + 1, hy, 8, 10, SKIN)
    c.rect(hx, hy + 1, 10, 8, SKIN)
    c.rect(hx + 1, hy + 9, 8, 1, SKIN_SH)
    c.rect(hx + 8, hy + 2, 2, 7, SKIN_SH)
    c.rect(hx + 2, hy + 1, 3, 2, SKIN_HI)
    for x, y, col in ((hx + 4, hy - 1, CURL), (hx + 5, hy - 1, CURL), (hx + 5, hy - 2, CURL),  # the curl
                      (hx + 6, hy - 3, CURL), (hx + 7, hy - 3, CURL), (hx + 7, hy - 2, CURL_SH),
                      (hx + 6, hy - 1, CURL_SH)):
        c.put(x, y, col)
    if back:
        c.rect(hx + 1, hy + 6, 8, 3, SKIN_SH)
    else:
        if dead:
            for ex in (hx + 5, hx + 8):
                for dx, dy in ((-1, -1), (1, -1), (0, 0), (-1, 1), (1, 1)):
                    c.put(ex + dx, hy + 4 + dy, EYE)
        else:
            c.rect(hx + 5, hy + 4, 1, 2, EYE)        # big baby eyes
            c.rect(hx + 8, hy + 4, 1, 2, EYE)
            c.put(hx + 5, hy + 4, (240, 240, 250, 255))
            c.put(hx + 8, hy + 4, (240, 240, 250, 255))
            c.put(hx + 4, hy + 6, hexc("f0a0a0"))    # rosy cheek
        c.rect(hx + 6, hy + 7, 3, 2, PACIFIER)       # the pacifier
        c.put(hx + 9, hy + 8, PACIFIER_RING)
        c.put(hx + 9, hy + 9, PACIFIER_RING)
        c.put(hx, hy + 5, SKIN_SH)
    c.outline()
    return c.img


def hero_frame(back=False, leg_phase=0, bob=0, kneel=0, dead=False, look=None):
    """leg_phase: 0 idle, 1 left fwd, 2 passing, 3 right fwd, 4 passing.
    kneel lowers the body onto its knees (legs get shorter); dead draws X eyes and an open mouth."""
    L = look or BOY
    if L.get("baby"):
        return baby_frame(back, leg_phase, bob, kneel, dead)
    if L.get("king"):
        return king_frame(back, leg_phase, bob, kneel, dead)
    SHIRT, SHIRT_HI, SHIRT_SH = L["shirt"]
    PANTS, PANTS_SH = L["pants"]
    SHOE, SHOE_SH = L["shoe"]
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
    if L.get("skirt"):  # a long skirt over the legs (granny)
        c.rect(5, 19 + by, 10, 6 - kneel, PANTS)
        c.rect(13, 19 + by, 2, 6 - kneel, PANTS_SH)
        c.rect(5, 24, 10, 1, PANTS_SH)
    hair = L["hair"]
    if hair is None:  # tuft of hair
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
    if hair is not None:
        hair(c, hx, hy, back)
    c.outline()
    return c.img


def hero_tuck(look=None):
    L = look or BOY
    if L.get("king"):
        return king_tuck()
    SHIRT = L["shirt"][0]
    PANTS = L["pants"][0]
    SHOE = L["shoe"][0]
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
    if L.get("baby"):  # his curl sticks out of the somersault
        c.put(9, 7, CURL)
        c.put(10, 6, CURL)
        c.put(11, 6, CURL)
    c.outline()
    return c.img


def hero_lying(look=None):
    """Flat on his back, head to the right (28x20)."""
    return hero_frame(dead=True, look=look).transpose(Image.ROTATE_270)


def family_frame(look, back=False, phase=0, bob=0, kneel=0, dead=False, tuck=False):
    """Authored 40x56 family frames, 64 px/unit; logical anchors stay 20x28 at 32 px/unit."""
    c = Canvas(40, 56)
    kind = "king" if look.get("king") else "baby" if look.get("baby") else "granny" if look.get("skirt") else "sister" if look is SISTER else "grandad" if look is GRANDAD else "boy"
    by = (bob + kneel) * 2
    ink = hexc("29232e")
    def box(x, y, w, h, color): c.rect(x, y + by, w, h, color)
    def ball(x, y, r, color): disc(c, x, y + by, r, color)
    if kind == "king":
        by = (bob + min(kneel,3)) * 2
        ink = hexc("63495b")
        cx, cy = 20, 34 + by
        mask = Canvas(40, 56)
        # One soft, slightly taller puff, with small tufts confined to the silhouette.
        for y in range(cy - 15, cy + 15):
            for x in range(cx - 14, cx + 15):
                width = 13 if y >= cy - 3 else 12
                if ((x - cx) / width)**2 + ((y - cy) / 15)**2 <= 1:
                    mask.put(x, y, KING_FLUFF)
        for dx, dy, r in ((-9,-9,4),(8,-10,4),(-12,1,3),(12,0,3),(-8,10,4),(8,10,4)):
            disc(mask, cx+dx, cy+dy, r, KING_FLUFF)
        if not tuck:
            for x, lift in ((12, 2 if phase == 1 else 0), (23, 2 if phase == 3 else 0)):
                foot = min(52, 48 + by) - lift
                c.rect(x, foot-1, 6, 4, KING_FLUFF_SH)
                c.rect(x, foot+2, 6, 1, KING_FLUFF_DK)
        for y in range(56):
            for x in range(40):
                if mask.px[x, y][3]:
                    # Leave one calm pink plane behind the entire face.
                    dx, dy = x - cx, y - cy
                    upper_light = dy < -6 and ((dx + 5) / 13)**2 + ((dy + 11) / 6)**2 < 1
                    underside = dy > 8 + (2 if abs(dx) < 5 else 0) or (dx > 10 and dy > 0)
                    c.put(x, y, KING_FLUFF_HI if upper_light else KING_FLUFF_SH if underside else KING_FLUFF)
        # The crown rests on the fluff. The face uses the old dot eyes and upturned smile.
        box(12, 17, 17, 5, KING_GOLD)
        for x in (12, 20, 27): box(x, 10 if x == 20 else 13, 2, 7 if x == 20 else 5, KING_GOLD)
        box(19, 12, 4, 5, KING_GOLD)
        box(14, 17, 13, 1, hexc("fff0ae")); box(12, 21, 17, 1, KING_GOLD_SH)
        if not back:
            box(19, 18, 3, 3, KING_GEM)
            for ex in (19, 25):
                if dead:
                    for i in range(5):
                        box(ex-2+i,29+i,1,1,EYE); box(ex+2-i,29+i,1,1,EYE)
                else:
                    box(ex, 30, 2, 3, EYE)
            if dead:
                box(21, 36, 4, 3, EYE)
            else:
                box(21, 36, 4, 2, EYE)
                box(19, 35, 2, 1, EYE); box(25, 35, 2, 1, EYE)
        if tuck:
            # Follow the curve of the puff with two tiny tucked feet.
            box(12, 43, 5, 3, KING_FLUFF_DK)
            box(23, 43, 5, 3, KING_FLUFF_DK)
        c.outline(ink)
        return c.img
    shirt, shirt_hi, shirt_sh = look["shirt"]
    pants, pants_sh = look["pants"]
    shoe, shoe_sh = look["shoe"]
    if tuck:
        ball(20, 33, 16, shirt)
        ball(16, 29, 10, shirt_hi)
        box(10, 34, 20, 8, pants); box(25, 37, 8, 6, shoe_sh)
        ball(21, 26, 7, SKIN)
        box(18, 22, 4, 2, SKIN_HI)
        box(22, 25, 2, 3, EYE)
        box(26, 25, 1, 2, EYE)
        if kind == "sister": box(7, 23, 10, 3, BAND)
        if kind == "granny": ball(15, 20, 6, GREY_HAIR)
        if kind == "grandad": box(20, 28, 9, 3, GREY_HAIR)
        if kind == "baby":
            box(20, 17, 4, 3, CURL)
            box(24, 29, 4, 2, PACIFIER)
            box(13, 39, 17, 5, PAMPER)
        c.outline(ink)
        return c.img
    # Different shoulders, waistlines and postures, on the same feet/world anchors.
    torso_x, torso_w = {"boy": (10,20), "sister": (12,17), "granny": (8,24), "grandad": (8,24), "baby": (13,16)}[kind]
    torso_y = 26 if kind != "baby" else 33
    if kind == "granny": torso_y = 28
    body_h = max(6, (17 if kind != "baby" else 10) - kneel*2)
    for x, lift in ((12, 2 if phase in (2,3) else 0), (23, 2 if phase in (1,4) else 0)):
        top = 42 if kind != "baby" else 44
        c.rect(x, top, 7, max(1, 50-lift-top), pants)
        c.rect(x+4, top, 3, max(1, 50-lift-top), pants_sh)
        c.rect(x-1, 50-lift, 9, 3, shoe); c.rect(x-1, 52-lift, 9, 1, shoe_sh)
        c.rect(x, 50-lift, 5, 1, shade(shoe, 1.15))
    # A short neck joins the narrower head to each outfit throughout the walk cycle.
    neck_y = {"boy": 23, "sister": 23, "granny": 26, "grandad": 24, "baby": 30}[kind]
    box(16, neck_y, 8, torso_y - neck_y + 1, SKIN_SH)
    box(17, neck_y, 4, torso_y - neck_y + 1, SKIN)
    box(torso_x, torso_y, torso_w, body_h, shirt)
    box(torso_x, torso_y, 3, body_h, shirt_hi)
    box(torso_x+torso_w-4, torso_y, 4, body_h, shirt_sh)
    box(torso_x, torso_y+body_h-2, torso_w, 2, shirt_sh)
    box(torso_x-2, torso_y+2, 3, 10, shirt_hi)
    box(torso_x+torso_w-1, torso_y+2, 3, 10, shirt_sh)
    if kind == "boy":
        box(12, 25, 16, 4, shirt_sh)  # hood/collar
        box(15, 25, 10, 2, shirt_hi)
        if back:
            box(13,26,14,6,shirt_sh); box(14,26,12,1,shirt_hi)
            box(19,33,1,8,shade(shirt,0.94))
        else:
            box(19, 29, 1, 11, shirt_sh)
            box(14, 36, 12, 5, shade(shirt,0.93)); box(14, 36, 12, 1, shirt_hi)
            for x in (13,25): box(x, 29, 1, 5, hexc("e7dbc2"))
    elif kind == "sister":
        box(11, 39, 20, 3, BAND)
        if back:
            # Crossed ninja straps; the sash wraps around as one plain band.
            for i in range(12):
                box(14 + i, 27 + i, 2, 1, shirt_sh)
                box(25 - i, 27 + i, 2, 1, shirt_hi)
        else:
            box(24, 42, 4, 5, BAND)
            box(10, 29, 3, 5, hexc("e3d8c7")); box(29, 29, 2, 5, hexc("b8b3ae"))
            box(15, 33, 11, 1, shirt_hi)
    elif kind == "granny":
        box(7, 40, 26, max(2, 11-by), pants)
        box(29, 41, 4, max(1,10-by), pants_sh)
        if back:
            box(8,28,24,6,shirt_hi)  # continuous shawl across her shoulders
            box(12,34,16,1,shirt_sh)
            box(19,36,1,6,shade(shirt,0.94))
        else:
            box(7, 28, 12, 5, shirt_hi); box(20, 28, 12, 5, shirt_hi)
            for y in (34,38,42): box(19,y,2,2,hexc("e4c89d"))
            box(10, 37, 5, 4, shirt_sh); box(10, 37, 5, 1, shirt_hi)
    elif kind == "grandad":
        if back:
            box(10,28,20,1,shirt_hi)
            box(19,30,1,10,shirt_sh)
            box(11,39,18,2,shade(shirt,0.9))
        else:
            box(18, 27, 5, 15, hexc("d6c5a4"))  # shirt between the vest halves
            box(8, 28, 8, 13, shirt_hi); box(25, 28, 7, 13, shirt_sh)
            for y in (30,35,40): box(20,y,2,2,hexc("5b463c"))
            box(26, 33, 5, 1, hexc("dfc38b")); box(27,35,2,2,hexc("bca06b"))
    elif kind == "baby":
        box(12, 41, 18, 7, PAMPER); box(13, 46, 16, 2, PAMPER_SH)
        if back: box(14,43,14,1,PAMPER_SH)
        else:
            box(13, 42, 3, 2, TAPE); box(26,42,3,2,TAPE); box(20,43,3,2,hexc("e3b84c"))
    # Back to the original narrow, gently stepped heads and a small three-quarter face.
    hx, hy, hw, hh = {"boy": (10,4,20,20), "sister": (11,4,18,20),
                       "granny": (10,7,20,20), "grandad": (10,4,20,21), "baby": (10,11,20,20)}[kind]
    box(hx+3, hy, hw-6, hh, SKIN)
    box(hx+1, hy+2, hw-2, hh-4, SKIN)
    box(hx, hy+5, hw, hh-10, SKIN)
    box(hx+hw-2, hy+6, 2, hh-11, SKIN_SH)
    box(hx+4, hy+hh-2, hw-8, 2, SKIN_SH)
    box(hx+3, hy+3, 5, 2, SKIN_HI)
    if not back:
        box(hx+1, hy+10, 2, 3, SKIN_SH)
    else:
        box(hx+3, hy+hh-5, hw-6, 3, SKIN_SH)
    if kind == "sister":
        # Long high ponytail and asymmetric bangs give her an athletic silhouette.
        box(10,2,20,5,NINJA_HAIR); box(9,6,5,9,NINJA_HAIR)
        box(3,4,8,5,NINJA_HAIR); box(2,9,6,8,NINJA_HAIR)
        box(9,7,21,3,BAND); box(4,8,6,2,BAND); box(2,13,6,2,BAND)
        if back:
            box(11,10,18,13,NINJA_HAIR)
            box(17,11,7,3,shade(BAND,0.8))
            box(17,14,7,12,NINJA_HAIR)
            box(18,15,2,9,shade(NINJA_HAIR,1.35))
            box(18,24,5,2,BAND)
    elif kind == "granny":
        ball(20,4,4,GREY_HAIR); box(11,5,19,6,GREY_HAIR)
        box(9,10,5,12,GREY_HAIR); box(28,10,3,10,GREY_HAIR_SH)
        if back: box(12,10,17,12,GREY_HAIR)
    elif kind == "grandad":
        box(9,10,3,11,GREY_HAIR); box(29,9,3,12,GREY_HAIR)
        if back: box(12,15,17,7,GREY_HAIR)
    elif kind == "boy":
        box(18,2,4,3,HAIR); box(20,1,3,2,HAIR)  # the familiar little tuft
    elif kind == "baby":
        box(20,8,3,4,CURL); box(22,6,4,3,CURL); box(25,5,2,3,CURL)
        box(23,8,2,2,CURL_SH)
    if not back:
        ey = hy+9
        near, far = hx+hw-10, hx+hw-4
        for ex in (near, far):
            if dead:
                for dx,dy in ((-1,-1),(0,0),(1,1),(-1,1),(1,-1)):
                    box(ex+dx,ey+dy,1,1,EYE)
            else:
                box(ex,ey,2,4,EYE)
                if ex == near or kind == "baby":
                    box(ex,ey-1,2,2,hexc("fff5e9"))
        if kind == "granny":
            box(near-2,ey-2,11,1,SPECS)
            box(near-2,ey-1,1,5,SPECS)
            box(near+3,ey-1,1,3,SPECS)
            box(far+2,ey-1,1,4,SPECS)
        if kind == "grandad":
            box(near-2,hy+14,10,2,GREY_HAIR)
            box(near-3,hy+16,11,1,GREY_HAIR)
            box(near-1,hy+17,7,1,GREY_HAIR_SH)
            box(near,ey-3,3,1,GREY_HAIR_SH); box(far,ey-3,2,1,GREY_HAIR_SH)
        elif kind == "baby":
            box(near,hy+15,7,4,PACIFIER)
            box(far,hy+16,3,3,PACIFIER_RING)
            box(far,hy+16,1,1,hexc("a4d8fa"))
        elif dead:
            box(near+2,hy+15,4,3,EYE)
        else:
            box(near+2,hy+15,4,1,SKIN_SH)
    c.outline(ink)
    return finish_surface(c.img, 0.07)


def player_sheet(look=None):
    look = look or BOY
    sheet = Image.new("RGBA", (40 * 6, 56 * 4), (0, 0, 0, 0))
    for row, back in ((0, False), (1, True)):
        for i, (phase, bob) in enumerate(((1,1),(2,0),(3,1),(4,0))):
            sheet.paste(family_frame(look, back, phase, bob), (i*40,row*56))
        for i,bob in ((4,0),(5,1)):
            sheet.paste(family_frame(look, back, bob=bob), (i*40,row*56))
    sheet.paste(family_frame(look,tuck=True), (0,112))
    for i,kneel in enumerate((0,3,6)):
        sheet.paste(family_frame(look,kneel=kneel,dead=True), (i*40,168))
    sheet.paste(family_frame(look,dead=True).transpose(Image.ROTATE_270), (120,184))
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
    c = Canvas(34,26)
    c.rect(2,1,30,16,WOOD); c.rect(1,3,32,12,WOOD)
    c.rect(3,1,28,1,WOOD_HI); c.rect(1,3,1,12,WOOD_HI)
    c.rect(2,9,30,1,shade(WOOD,0.88))
    c.rect(3,3,27,1,shade(WOOD,1.06))
    c.rect(2,16,30,4,WOOD_SH); c.rect(2,19,30,1,WOOD_DK)
    for x in (4,27):
        c.rect(x,20,3,5,WOOD_DK); c.rect(x,20,1,4,WOOD_SH)
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
    for _ in range(10):
        c.rect(rnd.randint(2, 21), rnd.randint(4, 15), 2, 1, shade(HAY, 0.91))
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
    "s": (hexc("1e1a24"), hexc("ffd166")),   # black, gold-trimmed
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


def pit_lid():
    """The same sewer hatch with a loose, rust-streaked pressure lid."""
    c = Canvas(32, 32)
    c.rect(2, 2, 28, 28, hexc("4a4a54"))
    c.rect(2, 2, 28, 2, hexc("6a6a76"))
    c.rect(5, 5, 22, 22, hexc("24242b"))
    c.rect(6, 6, 19, 19, hexc("777481"))
    c.rect(6, 6, 19, 2, hexc("aaa2a3"))
    for y in (10, 14, 18, 22):
        for x in (8, 14, 20):
            c.rect(x + ((y // 4) % 2), y, 3, 1, hexc("a3958f"))
    c.rect(15, 12, 4, 5, hexc("38323b"))
    c.rect(22, 21, 4, 2, hexc("a36345"))
    for x, y in ((3, 3), (27, 3), (3, 27), (27, 27)):
        c.rect(x, y, 2, 2, hexc("8a8a96"))
    c.outline()
    return c.img


def trap_cart():
    """Top-down steel garbage cart with caster wheels, trash bags and a hazard-striped front."""
    c = Canvas(38, 42)
    for x in (6, 26):
        for y in (2, 34):
            c.rect(x, y, 6, 6, hexc("242228"))
            c.rect(x + 1, y + 1, 4, 2, hexc("55505b"))
    c.rect(3, 7, 5, 28, hexc("835f4d"))
    c.rect(6, 5, 29, 32, hexc("57545a"))
    c.rect(7, 6, 27, 29, hexc("958780"))
    c.rect(10, 9, 21, 24, hexc("343236"))
    for x, y, color in ((16, 15, "4c4e40"), (25, 19, "5c5448"), (18, 26, "52534a")):
        disc(c, x, y, 5, hexc(color))
        c.rect(x - 2, y - 3, 4, 1, shade(hexc(color), 1.3))
        c.rect(x + 2, y - 5, 2, 2, hexc("a69d81"))
    c.rect(32, 7, 3, 28, hexc("d5a354"))
    for y in range(8, 35, 6):
        c.rect(32, y, 3, 3, hexc("49403a"))
    c.outline()
    return c.img


def trap_press():
    """Steel compactor head, with a striped crushing edge facing up."""
    c = Canvas(96, 12)
    c.rect(0, 0, 96, 12, hexc("282b31"))
    c.rect(1, 1, 94, 5, hexc("c09443"))
    c.rect(1, 6, 94, 4, hexc("777f84"))
    c.rect(1, 10, 94, 1, hexc("50555e"))
    for x in range(3, 90, 8):
        for y in range(1, 6):
            c.rect(x + y - 1, y, 3, 1, hexc("37302b"))
    for x in (4, 25, 48, 70, 91):
        c.rect(x, 7, 2, 2, hexc("c3c7c6"))
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


def boulder():
    """A moon boulder, pitted with little craters (the Moon's hay bale), 28x20."""
    c = Canvas(28, 20)
    rock, hi, sh = hexc("9a9aa6"), hexc("c0c0ca"), hexc("6e6e7a")
    disc(c, 14, 12, 8.5, sh)
    disc(c, 13, 11, 8, rock)
    disc(c, 6, 14, 5, sh)
    disc(c, 5.5, 13.5, 4.5, rock)
    disc(c, 22, 14, 4.5, sh)
    disc(c, 21.5, 13.5, 4, rock)
    disc(c, 10, 7, 2.5, hi)
    for x, y in ((15, 10), (8, 15), (19, 13)):  # craters
        c.rect(x, y, 3, 2, sh)
        c.rect(x, y - 1, 3, 1, hi)
    c.outline(hexc("2a2a36"))
    return c.img


def crystal():
    """A cluster of glowing moon crystals (the Moon's unbreakable pillar), 16x34."""
    c = Canvas(16, 34)
    ice, hi, sh = hexc("8ae0ff"), hexc("e0faff"), hexc("4a9ad0")
    for x0, top, w in ((3, 10, 4), (6, 2, 5), (10, 12, 4)):
        for y in range(top, 31):
            k = min(1, (y - top) / 3)
            ww = max(1, int(w * k))
            c.rect(x0 + (w - ww) // 2, y, ww, 1, ice)
        c.rect(x0 + 1, top + 3, 1, 31 - top - 6, hi)
        c.rect(x0 + w - 1, top + 4, 1, 31 - top - 6, sh)
    c.rect(2, 30, 12, 3, hexc("8e8e9a"))         # the rock it grows from
    c.outline(hexc("1e2a4a"))
    return c.img


def cheese():
    """A wedge of moon cheese (the Moon's barrel), 18x16."""
    c = Canvas(18, 16)
    y0, yd, yl = hexc("f2c84a"), hexc("c8962a"), hexc("ffe08a")
    for y in range(3, 15):                       # the sloping top
        w = 4 + (y - 3) * 12 // 11
        c.rect(1, y, w, 1, yl if y < 6 else y0)
    c.rect(1, 9, 16, 6, y0)
    c.rect(1, 9, 16, 1, yl)
    for x, y in ((4, 11), (10, 12), (14, 10), (6, 6)):  # holes
        c.rect(x, y, 2, 2, yd)
    c.outline(hexc("5a3e10"))
    return c.img


def supplies():
    """A silver supply crate with a blue stripe and a star (the Moon's crate), 16x18."""
    c = Canvas(16, 18)
    m, hi, sh = hexc("c8ccd6"), hexc("eef0f6"), hexc("8a8e9a")
    c.rect(1, 3, 14, 14, m)
    c.rect(1, 3, 14, 1, hi)
    c.rect(12, 4, 3, 13, sh)
    c.rect(1, 9, 14, 2, hexc("4a7ae0"))
    c.rect(7, 5, 2, 3, hexc("ffd166"))
    c.rect(6, 6, 4, 1, hexc("ffd166"))
    for x, y in ((2, 4), (13, 4), (2, 15), (13, 15)):
        c.put(x, y, sh)
    c.outline()
    return c.img


def pit_moon():
    """A deep crater on the Moon: fall in and you float back out for half a heart (32x32)."""
    c = Canvas(32, 32)
    disc(c, 16, 16, 13.5, hexc("8e8e9a"))       # the rim, sloping down
    disc(c, 16, 16.5, 11, hexc("5a5a66"))
    disc(c, 16, 17.5, 9, hexc("2a2a34"))
    disc(c, 16, 18.5, 6.5, hexc("101018"))      # dark all the way down
    for a in range(200, 340, 20):               # the lit far rim
        x = 16 + math.cos(math.radians(a)) * 12.5
        y = 16 + math.sin(math.radians(a)) * 12.5
        disc(c, x, y, 1.6, hexc("dcdce6"))
    for a in range(20, 160, 30):                # pebbles on the near rim
        x = 16 + math.cos(math.radians(a)) * 12.5
        y = 16 + math.sin(math.radians(a)) * 12.5
        c.put(int(x), int(y), hexc("b4b4be"))
    return c.img


def exit_rocket():
    """The family's cardboard rocket (as in the living room), waiting to fly home from the Moon."""
    img = Image.new("RGBA", (ROCKET_W, ROCKET_H), (0, 0, 0, 0))
    for layer in (home_rocket_box, home_rocket_cone, home_rocket_fins, home_rocket_scarf, home_rocket_engine):
        img.alpha_composite(layer())
    return img


def exit_dream():
    """A dream's way on: a doorway of soft lilac light standing on a cloud, a crescent moon over it and stars
    twinkling round its frame, 28x44."""
    c = Canvas(28, 44)
    frame, frame_sh, glow, glow_hi = hexc("8a6ad8"), hexc("6a4ab0"), hexc("c8a8ff"), hexc("f0e4ff")
    star = hexc("ffe48a")
    c.rect(5, 10, 18, 28, frame)                # the frame, rounded at the top
    disc(c, 14, 12, 9, frame)
    c.rect(8, 13, 12, 25, glow)                 # the light inside
    disc(c, 14, 14, 6, glow)
    for y in range(14, 38):                     # brighter towards its middle
        c.rect(12, y, 4, 1, glow_hi)
    c.rect(5, 36, 18, 2, frame_sh)
    disc(c, 22, 3, 3.2, star)                   # a crescent moon over it
    disc(c, 23.5, 2, 2.6, (0, 0, 0, 0))
    for x, y in ((3, 14), (25, 18), (2, 28), (26, 30), (6, 5)):  # little stars round the frame
        c.put(x, y, star)
        c.put(x - 1, y, shade(star, 0.8))
        c.put(x + 1, y, shade(star, 0.8))
        c.put(x, y - 1, shade(star, 0.8))
        c.put(x, y + 1, shade(star, 0.8))
    for x, y, r in ((6, 40, 4), (14, 41, 5), (22, 40, 4)):  # the cloud it stands on
        disc(c, x, y, r, hexc("e8e4f8"))
    c.rect(3, 41, 22, 2, hexc("c8c2e0"))
    c.outline()
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


def dumpster():
    """The Slums' big cover (for hay bales): a dented green dumpster, lid half open, 30x22."""
    c = Canvas(30, 22)
    body, dark = hexc("3a6a4a"), hexc("2a4e36")
    c.rect(2, 7, 26, 12, body)
    c.rect(2, 7, 26, 1, shade(body, 1.3))
    c.rect(2, 16, 26, 3, dark)
    for x in (8, 15, 22):  # ribs
        c.rect(x, 8, 1, 8, dark)
    c.rect(1, 3, 28, 4, hexc("2a2a30"))   # the lid, propped open a crack
    c.rect(1, 3, 28, 1, hexc("4a4a54"))
    c.rect(4, 6, 6, 2, hexc("1a1a1e"))    # garbage peeking out
    c.rect(19, 6, 4, 2, hexc("c9a040"))
    c.rect(11, 10, 8, 3, hexc("e8e0c8"))  # a scuffed label
    c.rect(12, 11, 6, 1, dark)
    c.put(25, 12, hexc("8a4a2a"))         # rust and a dent
    c.put(26, 13, hexc("8a4a2a"))
    for x in (4, 24):                     # little wheels
        c.rect(x, 19, 3, 2, hexc("1e1e22"))
    c.outline()
    return c.img


def car_burnt():
    """A burnt-out car (the Slums' other big cover), 30x18: charred, rusty, windows gone, on its rims."""
    c = Canvas(30, 18)
    body = hexc("3a3434")
    c.rect(2, 7, 26, 6, body)
    c.rect(7, 3, 15, 5, body)
    c.rect(9, 4, 5, 3, hexc("120e10"))    # empty windows
    c.rect(16, 4, 5, 3, hexc("120e10"))
    c.put(10, 4, hexc("6a6a74"))          # a shard of glass left
    for x, y in ((4, 8), (12, 9), (19, 10), (24, 8), (8, 11)):  # rust blotches
        c.rect(x, y, 2, 2, hexc("8a4a2a"))
    c.rect(2, 7, 26, 1, shade(body, 1.4))
    c.rect(22, 3, 1, 1, hexc("6a6a74"))
    for x in (6, 21):                     # bare rims, sagging
        c.rect(x, 13, 5, 3, hexc("5a5a66"))
        c.rect(x + 2, 14, 1, 1, hexc("2a2a30"))
    c.outline()
    return c.img


def drum():
    """A rusty oil drum (the Slums' barrel), 16x22, a green dribble down its side."""
    c = Canvas(16, 22)
    rust, rust_dk = hexc("9a4a2a"), hexc("6a3018")
    c.rect(1, 3, 14, 17, rust)
    c.rect(1, 3, 3, 17, shade(rust, 1.2))
    c.rect(12, 3, 3, 17, rust_dk)
    for y in (7, 14):                     # the rolling hoops
        c.rect(1, y, 14, 1, rust_dk)
    c.rect(2, 1, 12, 3, hexc("5a5a66"))   # the lid
    c.rect(3, 2, 10, 1, hexc("3a3a44"))
    c.rect(9, 2, 2, 1, hexc("8ae83a"))    # acid at the bung
    c.rect(9, 4, 1, 6, hexc("8ae83a"))    # and dribbling down
    c.put(9, 10, hexc("3c8a1e"))
    c.outline()
    return c.img


def trash():
    """A pile of black trash bags (the Slums' crate), 18x16."""
    c = Canvas(18, 16)
    bag, bag_hi, bag_dk = hexc("2a2a30"), hexc("4a4a58"), hexc("18181c")
    for bx, by, w, h in ((1, 7, 9, 8), (8, 6, 9, 9), (4, 2, 9, 8)):
        c.rect(bx, by + 1, w, h - 2, bag)
        c.rect(bx + 1, by, w - 2, h, bag)
        c.rect(bx + 2, by + 1, 2, 2, bag_hi)
        c.rect(bx + 1, by + h - 2, w - 2, 1, bag_dk)
    c.rect(8, 1, 1, 2, bag)                # tied tops
    c.rect(7, 0, 3, 1, bag_hi)
    c.rect(14, 5, 1, 2, bag)
    c.put(12, 11, hexc("c9a040"))         # something poking out
    c.put(3, 13, hexc("d8d0b8"))
    c.outline()
    return c.img


def drum_fire(lit=True):
    """The city's secret: a rusty oil drum with a fire in it (16x28; GameWorld adds flickering flames and a
    wisp of smoke). Put out ({@code lit=False}): a charred rim and a last thread of smoke."""
    c = Canvas(16, 28)
    rust, rust_dk = hexc("9a4a2a"), hexc("6a3018")
    c.rect(1, 9, 14, 17, rust)
    c.rect(1, 9, 3, 17, shade(rust, 1.2))
    c.rect(12, 9, 3, 17, rust_dk)
    for y in (13, 20):
        c.rect(1, y, 14, 1, rust_dk)
    for x in (5, 8, 11):                  # holes punched in the side, glowing (or dark)
        c.rect(x, 16, 1, 2, hexc("ffb040") if lit else hexc("1a1210"))
    c.rect(1, 7, 14, 3, hexc("2a1a14"))   # the open top, soot all round it
    if lit:
        c.rect(2, 8, 12, 1, hexc("ff8a1e"))
        for x, h, col in ((3, 4, "ff8a1e"), (5, 7, "ffd84a"), (7, 5, "ff8a1e"), (9, 8, "ffd84a"), (11, 5, "ff8a1e")):
            c.rect(x, 8 - h, 2, h, hexc(col))
            c.put(x, 8 - h - 1, hexc("ff5a1e"))
        c.rect(6, 4, 4, 3, hexc("fff3b0"))  # the hot heart of it
    else:
        c.rect(2, 8, 12, 1, hexc("3a3434"))
        c.rect(7, 2, 1, 5, hexc("9a9aa8"))  # a last thread of smoke
        c.put(8, 1, hexc("9a9aa8"))
    c.outline()
    return c.img


def boards():
    """Planks nailed across a doorway in the city's top wall (32x26): the way down to the Slums is behind."""
    c = Canvas(32, 26)
    c.rect(2, 2, 28, 22, hexc("0e0a0c"))  # the dark doorway
    c.rect(0, 0, 2, 26, hexc("3a3a44"))   # its frame
    c.rect(30, 0, 2, 26, hexc("3a3a44"))
    c.rect(0, 0, 32, 2, hexc("3a3a44"))
    for y, tilt in ((3, 0), (8, 1), (13, 0), (18, -1)):
        for x in range(1, 31):
            yy = y + (tilt * (x - 16)) // 14
            c.rect(x, yy, 1, 4, WOOD if x % 9 else WOOD_SH)
            c.put(x, yy, WOOD_HI)
            c.put(x, yy + 3, WOOD_DK)
        c.put(3, y + 1, IRON)              # nails
        c.put(28, y + 1, IRON)
    c.rect(6, 9, 6, 2, hexc("ff5fa2"))    # a stray squiggle of graffiti across them
    c.rect(11, 8, 4, 1, hexc("ff5fa2"))
    c.outline()
    return c.img


def stairs():
    """The doorway once the boards are gone (32x26): steps going down into the dark."""
    c = Canvas(32, 26)
    c.rect(0, 0, 32, 26, hexc("3a3a44"))
    c.rect(2, 2, 28, 24, hexc("06040a"))
    for i, y in enumerate(range(24, 6, -4)):  # each step further down is darker and narrower
        k = 1.0 - i * 0.17
        w = 26 - i * 2
        c.rect(16 - w // 2, y - 2, w, 2, shade(hexc("6a6470"), k))
        c.rect(16 - w // 2, y, w, 1, shade(hexc("3a3640"), k))
    for x in (4, 27):                     # splinters of the boards left in the frame
        c.rect(x, 4, 1, 3, WOOD_SH)
        c.rect(x, 15, 1, 2, WOOD_SH)
    c.outline()
    return c.img


def exit_ladder():
    """The Undercity's way out after its boss: an iron ladder up to an open manhole, daylight falling in, 28x40."""
    c = Canvas(28, 40)
    light = hexc("fff6c8")
    for i in range(10):  # a shaft of daylight from above
        c.rect(8 - i // 3, i * 4, 12 + 2 * (i // 3), 4, (*light[:3], 60 + i * 6))
    c.rect(4, 0, 20, 4, hexc("3a3a44"))  # the manhole rim at the top
    c.rect(6, 0, 16, 2, light)
    for x in (8, 19):  # the rails
        c.rect(x, 2, 2, 36, hexc("5a5a66"))
        c.rect(x, 2, 1, 36, hexc("8a8a96"))
    for y in range(6, 38, 5):  # the rungs
        c.rect(8, y, 13, 2, hexc("6a6a74"))
    c.rect(4, 36, 20, 4, hexc("101418"))  # where it meets the floor
    return c.img  # (no outline: it would ring the soft shaft of light)


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


def bed_leg():
    c = Canvas(16, 24)
    dark, wood, light = map(hexc, ("473643", "86574b", "c68c64"))
    c.rect(2, 2, 12, 5, dark)
    c.rect(3, 2, 10, 3, light)
    c.rect(4, 7, 8, 13, wood)
    c.rect(5, 8, 2, 11, light)
    c.rect(9, 7, 3, 12, dark)
    c.rect(3, 19, 10, 3, dark)
    c.rect(4, 19, 7, 1, light)
    c.outline()
    return c.img


def lost_slipper():
    c = Canvas(25, 17)
    wool, trim, inside = map(hexc, ("628d9c", "bbd5c9", "374654"))
    c.rect(2, 10, 21, 4, wool)
    c.rect(4, 8, 17, 6, trim)
    c.rect(5, 9, 7, 3, inside)
    c.rect(12, 7, 10, 6, wool)
    c.rect(13, 4, 3, 5, wool)
    c.rect(17, 2, 3, 6, wool)
    c.rect(18, 3, 1, 4, trim)
    c.put(20, 9, inside)
    c.rect(22, 11, 2, 2, hexc("dd9caa"))
    c.rect(4, 14, 17, 1, inside)
    c.outline()
    return c.img


def picture_book():
    c = Canvas(20, 14)
    cover, binding, pages = map(hexc, ("9d565c", "603e52", "e2c9a2"))
    c.rect(2, 3, 16, 9, cover)
    c.rect(3, 9, 15, 2, pages)
    c.rect(2, 3, 3, 9, binding)
    c.rect(6, 4, 10, 1, hexc("bc7770"))
    c.rect(10, 5, 2, 3, pages)
    c.rect(9, 6, 4, 1, pages)
    c.outline()
    return c.img


def alphabet_block():
    c = Canvas(15, 16)
    c.rect(2, 2, 11, 12, hexc("995344"))
    c.rect(2, 2, 11, 3, hexc("f2c68a"))
    c.rect(3, 5, 8, 8, hexc("ce7962"))
    ink = hexc("fff0c3")
    c.rect(5, 7, 1, 5, ink)
    c.rect(8, 7, 1, 5, ink)
    c.rect(6, 6, 2, 1, ink)
    c.rect(6, 9, 2, 1, ink)
    c.outline()
    return c.img


def thread_spool():
    c = Canvas(14, 17)
    c.rect(2, 2, 10, 3, hexc("e0ad78"))
    c.rect(3, 5, 8, 8, hexc("986180"))
    for row in (5, 8, 11):
        c.rect(3, row, 6, 1, hexc("d699ab"))
    c.rect(2, 13, 10, 2, hexc("b68665"))
    c.rect(6, 2, 3, 1, hexc("614653"))
    c.outline()
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
             ("dumpster", dumpster()), ("car_burnt", car_burnt()), ("drum", drum()), ("trash", trash()),
             ("drum_fire", drum_fire()), ("drum_out", drum_fire(False)), ("boards", boards()), ("stairs", stairs()),
             ("exit_ladder", exit_ladder()),
             ("boulder", boulder()), ("crystal", crystal()), ("cheese", cheese()), ("supplies", supplies()),
             ("pit_moon", pit_moon()), ("exit_rocket", exit_rocket()), ("exit_dream", exit_dream()),
             ("coin_0", coin(0)), ("coin_1", coin(1)), ("coin_2", coin(2)), ("coin_3", coin(3)),
             ("coin_big", coin_big()), ("counter", counter()), ("counter_sky", counter(True)),
             ("stand", stand()), ("stand_sky", stand(True)), ("baallo", baallo()),
             ("baallo_blink", baallo("blink")), ("baallo_happy", baallo("happy")), ("baallo_angry", baallo("angry"))]
    for tier, (body, trim) in CHEST_TIERS.items():
        parts.append(("chest_" + tier, chest(body, trim)))
        parts.append(("chest_" + tier + "_open", chest(body, trim, opened=True)))
    parts.extend([("bed_leg", bed_leg()), ("lost_slipper", lost_slipper()),
                  ("alphabet_block", alphabet_block()), ("thread_spool", thread_spool()),
                  ("picture_book", picture_book()), ("pit_lid", pit_lid()), ("trap_cart", trap_cart()),
                  ("trap_press", trap_press())])
    parts = [(name, detailed_prop(name, p)) for name, p in parts]
    x = y = width = row_height = 0
    PROPS.clear()
    for name, p in parts:
        if x + p.width + 2 > 4096:
            y += row_height + 2
            x = row_height = 0
        PROPS[name] = (x, y, p.width, p.height)
        x += p.width + 2
        width = max(width, x)
        row_height = max(row_height, p.height)
    sheet = Image.new("RGBA", (width, y + row_height), (0, 0, 0, 0))
    for name, p in parts:
        sheet.paste(p, PROPS[name][:2])
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
    """A red enamel launcher with a flared brass muzzle and a striped firework."""
    c = Canvas(22, 11)
    draw = ImageDraw.Draw(c.img)
    draw.polygon([(0,2),(4,1),(6,2),(15,2),(17,1),(19,1),(19,2),(21,3),
                  (21,5),(19,6),(19,7),(12,7),(11,10),(7,10),(8,7),(4,7),(0,8)], fill="#332939")
    draw.polygon([(1,3),(4,2),(6,3),(6,6),(4,6),(1,7)], fill="#69717d")
    c.rect(1,3,2,1,hexc("a8adb0"))
    draw.polygon([(5,3),(16,3),(17,4),(16,6),(5,6)], fill="#c44748")
    c.rect(6,3,10,1,hexc("f47d62"))
    c.rect(6,6,9,1,hexc("833044"))
    for x in (7, 11):
        draw.line([(x+1,3),(x,5)], fill="#ffd082")
    c.put(12,5,hexc("fff0bf"))
    c.rect(16,2,2,5,hexc("e7ab52"))
    c.put(16,2,hexc("fff0ba")); c.put(17,6,hexc("996136"))
    c.rect(18,3,1,3,hexc("763e48"))
    c.rect(19,3,1,3,hexc("f5d9a4"))
    c.put(20,4,hexc("ff9d6e"))
    c.rect(8,8,3,2,hexc("66627b"))
    c.put(9,9,hexc("aaa0a7"))
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


def _disc(c, cx, cy, r, col):
    """Fills a round blob centred on (cx, cy)."""
    for y in range(c.h):
        for x in range(c.w):
            if (x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r:
                c.put(x, y, col)


def gun_water_pistol():
    """D: a see-through blue toy water pistol with an orange tank on top."""
    c = Canvas(14, 10)
    c.rect(2, 3, 10, 3, hexc("4aa8e8"))      # body
    c.rect(2, 3, 10, 1, hexc("8ad4ff"))
    c.rect(12, 4, 2, 1, hexc("f5f0e6"))      # nozzle
    c.rect(4, 0, 5, 3, hexc("ff9f3a"))       # water tank
    c.rect(5, 1, 1, 1, hexc("ffd0a0"))
    c.rect(2, 6, 3, 3, hexc("3a8ad0"))       # grip
    c.rect(6, 6, 1, 2, hexc("ffd23a"))       # trigger
    c.outline()
    return c.img, 3, 4


def gun_snowball_slinger():
    """D: a wooden slingshot with a snowball in its band."""
    c = Canvas(14, 11)
    c.rect(1, 5, 6, 2, WOOD)                 # handle
    c.rect(1, 6, 6, 1, WOOD_SH)
    c.rect(7, 2, 2, 4, WOOD)                 # the fork's two prongs
    c.rect(7, 6, 2, 3, WOOD)
    c.rect(9, 1, 2, 2, WOOD_HI)
    c.rect(9, 8, 2, 2, WOOD_HI)
    for y in range(2, 9):                    # the band
        c.put(11, y, hexc("c0392b"))
    _disc(c, 12, 5.5, 2.2, hexc("eef8ff"))   # the snowball
    c.put(11, 4, hexc("ffffff"))
    c.put(13, 6, hexc("a8d8f0"))
    c.outline()
    return c.img, 3, 5


def gun_freeze_ray():
    """C: a retro ray gun gone cold - an icy blue body, a coolant tank on top, a frosty dish at the muzzle."""
    c = Canvas(16, 11)
    c.rect(3, 3, 8, 4, hexc("6ab8e8"))       # body
    c.rect(3, 3, 8, 1, hexc("a8dcff"))
    c.rect(5, 1, 4, 2, hexc("dff4ff"))       # coolant tank
    c.put(6, 1, hexc("ffffff"))
    c.rect(11, 4, 3, 3, hexc("3a78b0"))      # barrel
    c.rect(14, 2, 1, 7, hexc("bfeaff"))      # the frosty dish
    c.put(14, 5, hexc("ffffff"))
    c.put(7, 5, hexc("ffffff"))              # a little snowflake mark
    c.put(6, 4, hexc("dff4ff"))
    c.put(8, 4, hexc("dff4ff"))
    c.put(6, 6, hexc("dff4ff"))
    c.put(8, 6, hexc("dff4ff"))
    c.rect(4, 7, 3, 3, hexc("4a4a55"))       # grip
    c.outline()
    return c.img, 5, 5


def gun_pillow_cannon():
    """C: a stubby lilac cannon with a pillow poking out of its mouth."""
    c = Canvas(18, 11)
    c.rect(3, 3, 9, 5, hexc("8a7ab8"))       # barrel
    c.rect(3, 3, 9, 1, hexc("b0a4d8"))
    c.rect(2, 2, 2, 7, hexc("5a4a8a"))       # back rim
    c.rect(11, 2, 2, 7, hexc("5a4a8a"))      # muzzle rim
    c.rect(13, 3, 4, 5, hexc("fbf6ff"))      # the pillow, plump and white
    c.put(13, 2, hexc("fbf6ff"))
    c.put(16, 2, hexc("fbf6ff"))
    c.put(13, 8, hexc("fbf6ff"))
    c.put(16, 8, hexc("fbf6ff"))
    c.rect(14, 5, 2, 1, hexc("d8d0ee"))
    c.rect(4, 8, 3, 2, WOOD_SH)              # grip
    c.outline()
    return c.img, 5, 5


def gun_jumper_cables():
    """C: a red crocodile clip on a cable, with a spark between its jaws."""
    c = Canvas(16, 10)
    for x in range(0, 6):                    # the cable coming from the battery
        c.put(x, 6 + (1 if x % 4 < 2 else 0), hexc("2a2a30"))
    c.rect(5, 4, 4, 4, hexc("d63a2f"))       # the grip of the clip
    c.rect(5, 4, 4, 1, hexc("ff6a5a"))
    c.rect(9, 3, 5, 2, METAL_L)              # upper jaw
    c.rect(9, 6, 5, 2, METAL_L)              # lower jaw
    for x in (10, 12):
        c.put(x, 5, METAL_D)                 # teeth
    c.put(14, 4, hexc("ffe94a"))             # a spark
    c.put(15, 5, hexc("ffe94a"))
    c.put(14, 6, hexc("fff8c0"))
    c.outline()
    return c.img, 6, 5


def gun_frying_pan():
    """B: a cast-iron frying pan held by its handle."""
    c = Canvas(18, 12)
    c.rect(0, 5, 7, 2, hexc("6b4a2f"))       # wooden handle
    c.rect(0, 5, 7, 1, hexc("8a6a48"))
    c.put(1, 6, hexc("2a1a10"))              # the hole to hang it up by
    _disc(c, 12, 6, 5.4, hexc("3a3a44"))     # the pan
    _disc(c, 12, 6, 4.2, hexc("5a5a66"))
    c.rect(9, 3, 3, 1, hexc("9a9aa6"))       # a shine
    c.put(9, 4, hexc("9a9aa6"))
    c.outline()
    return c.img, 3, 6


def gun_porcupine():
    """B: a cross little porcupine, quills bristling, held like a gun - nose to the right."""
    c = Canvas(16, 13)
    for i, (x, y) in enumerate(((3, 1), (5, 0), (7, 0), (9, 1), (2, 3), (11, 2), (1, 6), (12, 4))):
        c.rect(x, y, 1, 3, hexc("e8d8b0") if i % 2 else hexc("6b4a2f"))  # quills
    _disc(c, 7, 7, 5, hexc("8a5a38"))        # body
    _disc(c, 12, 8, 2.6, hexc("d8b088"))     # face
    c.put(15, 8, hexc("2a1a10"))             # nose
    c.put(12, 7, hexc("2a1a10"))             # a cross little eye
    c.put(11, 6, hexc("2a1a10"))
    c.rect(5, 11, 2, 2, hexc("5a3a20"))      # feet
    c.rect(9, 11, 2, 2, hexc("5a3a20"))
    c.outline()
    return c.img, 6, 8


def gun_dust_bunny():
    """A: a little hand vacuum running backwards, a grey dust bunny with eyes bobbing at its nozzle."""
    c = Canvas(18, 11)
    c.rect(2, 3, 8, 4, hexc("e05a8a"))       # body
    c.rect(2, 3, 8, 1, hexc("ff8ab0"))
    c.rect(3, 4, 3, 2, hexc("c0c8d8"))       # dust tank
    c.rect(10, 4, 3, 2, METAL_M)             # nozzle
    for x, y in ((14, 3), (16, 4), (15, 6), (13, 5), (16, 2)):
        c.rect(x, y, 2, 2, hexc("9a94a0"))   # the dust bunny
    c.put(15, 4, hexc("ffffff"))
    c.put(16, 4, hexc("2a1a10"))
    c.rect(3, 7, 3, 3, hexc("a03a60"))       # grip
    c.outline()
    return c.img, 4, 5


def gun_polaroid():
    """A: an instant camera - white with a rainbow stripe, a big lens and a flash on top."""
    c = Canvas(14, 12)
    c.rect(1, 3, 10, 7, hexc("f5f0e6"))      # body
    c.rect(1, 3, 10, 1, hexc("ffffff"))
    for i, col in enumerate(("ff4a4a", "ff9f3a", "ffd23a", "5ae06a", "4f8ad8")):
        c.put(1, 4 + i, hexc(col))           # the rainbow stripe
    c.rect(3, 1, 3, 2, METAL_M)              # the flash
    c.rect(4, 1, 1, 1, hexc("fff8c0"))
    _disc(c, 10, 6.5, 3, hexc("3a3a44"))     # the lens, pointing ahead
    _disc(c, 10.5, 6.5, 1.6, hexc("4f8ad8"))
    c.put(10, 5, hexc("e0f6ff"))
    c.rect(2, 10, 3, 2, hexc("c9c4b8"))      # grip
    c.outline()
    return c.img, 3, 6


def gun_acid_squirter():
    """C: a garden sprayer full of bubbling green acid: a round tank, a pump handle and a long nozzle."""
    c = Canvas(16, 11)
    _disc(c, 5.5, 4.5, 3.6, hexc("6ac83a"))  # the tank
    _disc(c, 5.5, 4.5, 2.4, hexc("8ae83a"))
    c.put(4, 3, hexc("d8ff9a"))
    c.put(6, 5, hexc("d8ff9a"))
    c.rect(4, 0, 3, 1, hexc("4a4a55"))       # pump handle
    c.rect(5, 1, 1, 1, hexc("4a4a55"))
    c.rect(9, 4, 6, 1, hexc("5a5a66"))       # the nozzle
    c.rect(14, 3, 2, 3, hexc("3a3a44"))
    c.rect(2, 7, 3, 3, hexc("3a8a2a"))       # grip
    c.outline()
    return c.img, 3, 4


def gun_flare_gun():
    """C: a chunky orange flare pistol with a fat short barrel."""
    c = Canvas(15, 10)
    c.rect(3, 2, 10, 4, hexc("ff7a2a"))      # body and barrel
    c.rect(3, 2, 10, 1, hexc("ffb070"))
    c.rect(12, 1, 3, 6, hexc("e05a1a"))      # the wide muzzle
    c.rect(13, 3, 2, 2, hexc("3a1a10"))
    c.rect(2, 6, 3, 4, hexc("c84a1a"))       # grip
    c.rect(6, 6, 1, 2, hexc("4a4a55"))       # trigger
    c.outline()
    return c.img, 3, 4


def gun_gloop_launcher():
    """B: a stubby launcher with a glass drum of glowing green gloop on top."""
    c = Canvas(19, 12)
    c.rect(2, 5, 14, 4, hexc("4a5a4a"))      # the body
    c.rect(2, 5, 14, 1, hexc("6a7a6a"))
    c.rect(15, 4, 4, 6, hexc("3a4a3a"))      # the fat muzzle
    c.rect(16, 6, 3, 2, hexc("8ae83a"))
    c.rect(5, 0, 7, 5, hexc("c8f0ff"))       # the glass drum
    c.rect(6, 1, 5, 4, hexc("7ad83a"))
    c.put(7, 2, hexc("d8ff9a"))
    c.put(9, 3, hexc("d8ff9a"))
    c.rect(3, 9, 3, 3, hexc("2a3a2a"))       # grip
    c.outline()
    return c.img, 4, 7


def gun_chomper():
    """A: the Chomper - a launcher shaped like a fluffy shark's head, jaws wide open at the muzzle,
    a dorsal fin for a sight; each shot is a little shark that swims off and bites."""
    c = Canvas(22, 13)
    shark, shark_sh, shark_hi, belly = hexc("5f8fd8"), hexc("3f6aae"), hexc("8fb8f0"), hexc("eef4ff")
    c.rect(3, 4, 13, 6, shark)               # the head-tube
    c.rect(3, 4, 13, 1, shark_hi)
    c.rect(3, 8, 13, 2, belly)               # white belly underneath
    c.rect(8, 1, 3, 3, shark_sh)             # dorsal fin sight
    c.put(9, 0, shark_sh)
    c.rect(0, 5, 3, 4, shark_sh)             # tail end
    c.put(0, 4, shark_sh)
    c.put(0, 9, shark_sh)
    c.rect(16, 2, 6, 3, shark)               # upper jaw, opened wide
    c.rect(16, 2, 6, 1, shark_hi)
    c.rect(16, 9, 6, 3, belly)               # lower jaw
    c.rect(16, 5, 6, 4, hexc("8a1f3a"))      # the open mouth
    c.rect(18, 7, 3, 1, hexc("d85a7a"))      # tongue
    for x in (16, 18, 20):
        c.put(x, 5, hexc("ffffff"))          # pointy teeth, top
        c.put(x + 1, 8, hexc("ffffff"))      # and bottom
    c.rect(13, 5, 2, 2, hexc("1e1416"))      # an angry eye
    c.put(13, 5, hexc("ffffff"))
    c.rect(12, 4, 3, 1, shark_sh)            # frowning brow
    c.rect(6, 10, 3, 3, hexc("2f3a52"))      # grip
    c.outline()
    return c.img, 7, 6


def gun_boom_buss():
    """B: the Boom-buss - a blunderbuss with a huge brass bell, red and yellow bands like a firework,
    a lit fuse at the back; its pellets go off where they land."""
    c = Canvas(24, 11)
    brass, brass_d, brass_hi = hexc("d8a83a"), hexc("a07a20"), hexc("ffe08a")
    c.rect(7, 3, 11, 3, brass)               # the barrel
    c.rect(7, 3, 11, 1, brass_hi)
    c.rect(7, 5, 11, 1, brass_d)
    for x in (10, 14):
        c.rect(x, 3, 1, 3, hexc("d63a2f"))   # red bands
    c.rect(18, 2, 2, 5, brass)               # the bell flares out
    c.rect(20, 1, 2, 7, brass)
    c.rect(22, 0, 2, 9, brass_d)
    c.rect(20, 1, 2, 1, brass_hi)
    c.rect(22, 3, 2, 3, hexc("2a1a10"))      # the dark mouth
    c.rect(0, 4, 8, 3, WOOD)                 # wooden stock
    c.rect(0, 4, 8, 1, hexc("9a6a3a"))
    c.rect(0, 6, 3, 2, WOOD)
    c.rect(6, 6, 3, 4, hexc("5a3218"))       # grip
    c.put(5, 2, hexc("6b4a2f"))              # the fuse
    c.put(4, 1, hexc("6b4a2f"))
    c.put(3, 0, hexc("ffd23a"))              # and its spark
    c.outline()
    return c.img, 8, 4


def gun_throwing_stars():
    """The sister's throwing star (shuriken), held between two fingers - a four-pointed steel star."""
    c = Canvas(11, 11)
    steel, steel_hi, steel_sh = hexc("c9ccd6"), hexc("f0f2f8"), hexc("7a7e8a")
    c.rect(4, 4, 3, 3, steel)
    for i in range(1, 5):                     # the four points
        w = 1 if i > 2 else 2
        c.rect(5 - (w // 2), 4 - i, w, 1, steel_hi if i < 3 else steel)
        c.rect(5 - (w // 2), 6 + i, w, 1, steel_sh)
        c.rect(4 - i, 5 - (w // 2), 1, w, steel)
        c.rect(6 + i, 5 - (w // 2), 1, w, steel_hi)
    c.put(5, 5, hexc("3a3a44"))               # the hole in the middle
    c.outline()
    return c.img, 1, 5


def gun_slingshot():
    """D: a forked wooden slingshot with a red rubber band and a pebble in the pouch, the fork ahead."""
    c = Canvas(14, 12)
    band, pebble = hexc("d63a2f"), hexc("9a948a")
    c.rect(1, 5, 6, 2, WOOD)                 # the handle
    c.rect(1, 5, 6, 1, WOOD_HI)
    c.rect(6, 4, 2, 4, WOOD)                 # where it forks
    c.rect(8, 1, 2, 3, WOOD)                 # the two prongs
    c.rect(8, 8, 2, 3, WOOD)
    c.rect(10, 0, 2, 2, WOOD_SH)
    c.rect(10, 10, 2, 2, WOOD_SH)
    c.rect(11, 2, 1, 3, band)                # the band, stretched to the pouch
    c.rect(11, 7, 1, 3, band)
    c.rect(11, 5, 2, 2, hexc("6b4a2f"))      # the pouch
    c.put(12, 5, pebble)
    c.put(12, 6, pebble)
    c.outline()
    return c.img, 3, 5


def gun_popcorn_popper():
    """D: a little red-and-white striped popcorn cart's popper - a kettle full of popcorn, a spout ahead."""
    c = Canvas(16, 13)
    red, white, corn, corn_sh = hexc("d63a2f"), hexc("f5f0e6"), hexc("fff4d0"), hexc("f2c040")
    c.rect(2, 4, 8, 6, white)                # the striped tub
    for x in (3, 5, 7, 9):
        c.rect(x, 4, 1, 6, red)
    for x, y in ((2, 2), (4, 1), (6, 2), (8, 1), (3, 3), (7, 3), (5, 0)):
        c.rect(x, y, 2, 2, corn)             # popcorn heaped on top
    c.put(4, 2, corn_sh)
    c.put(8, 2, corn_sh)
    c.rect(10, 5, 5, 2, METAL_M)             # the spout
    c.rect(10, 5, 5, 1, METAL_L)
    c.rect(14, 4, 2, 4, METAL_D)
    c.rect(3, 10, 3, 3, hexc("a02a20"))      # grip
    c.outline()
    return c.img, 4, 5


def gun_paper_planes():
    """C: a paper plane launcher - a folded white plane resting on a little wooden launcher, nose ahead."""
    c = Canvas(16, 13)
    paper, fold, wood, wood_sh = hexc("f4f6fa"), hexc("c8d0dc"), hexc("b07a48"), hexc("7a5030")
    c.rect(2, 6, 10, 2, wood)                # the launcher's rail
    c.rect(2, 7, 10, 1, wood_sh)
    for i in range(9):                       # the plane: a long triangle, nose at the right
        c.rect(4 + i, 4 - i // 3 + 0, 1, 1 + (8 - i) // 3, paper)
    c.rect(4, 2, 2, 3, paper)
    c.rect(4, 4, 10, 1, fold)                # the crease
    c.put(14, 4, paper)
    c.rect(3, 8, 3, 4, wood_sh)              # grip
    c.outline()
    return c.img, 4, 6


def gun_tennis_balls():
    """C: a tennis ball launcher - a green tube with white stripes, a ball peeking out of the muzzle."""
    c = Canvas(16, 13)
    green, green_sh, ball = hexc("3a9a5a"), hexc("2a6a40"), hexc("d8ec3a")
    c.rect(1, 3, 11, 5, green)
    c.rect(1, 6, 11, 2, green_sh)
    c.rect(4, 3, 1, 5, hexc("f4f6fa"))       # stripes
    c.rect(8, 3, 1, 5, hexc("f4f6fa"))
    _disc(c, 13.5, 5, 2.6, ball)             # the ball
    c.put(13, 4, hexc("ffffff"))
    c.rect(12, 6, 3, 1, hexc("f4f6fa"))      # its seam
    c.rect(3, 8, 3, 4, METAL_D)              # grip
    c.outline()
    return c.img, 4, 5


def gun_crayon_box():
    """C: a box of crayons - a yellow box with a green stripe, red, blue and green crayon tips poking out."""
    c = Canvas(16, 13)
    box, box_sh, stripe = hexc("f2c640"), hexc("c89a28"), hexc("3a9a5a")
    c.rect(1, 2, 9, 8, box)
    c.rect(1, 8, 9, 2, box_sh)
    c.rect(1, 5, 9, 2, stripe)
    for y, col in ((2, "e8443a"), (4, "3a7ae8"), (6, "4ac850")):
        c.rect(10, y, 3, 2, hexc(col))       # the crayons
        c.put(13, y, hexc(col))
        c.put(14, y + (1 if y == 4 else 0), hexc(col))
    c.rect(3, 10, 3, 3, box_sh)              # grip
    c.outline()
    return c.img, 4, 5


def gun_pinwheel():
    """C: a pinwheel on a stick - a candy-striped stick, the four-sailed wheel at the front."""
    c = Canvas(16, 13)
    pink, white = hexc("ff7ab8"), hexc("f4f6fa")
    c.rect(1, 6, 9, 1, hexc("e8443a"))       # the stick
    for x in (2, 5, 8):
        c.put(x, 6, white)
    for (x, y, w, h), col in (((10, 1, 3, 5), pink), ((13, 4, 3, 3), white),
                               ((12, 7, 3, 5), pink), ((9, 6, 3, 3), white)):
        c.rect(x, y, w, h, col)              # the sails
    c.put(12, 6, hexc("e0a040"))             # the pin
    c.rect(2, 7, 2, 5, hexc("b07a48"))       # grip
    c.outline()
    return c.img, 3, 6


def gun_sock_launcher():
    """D: a stinky sock launcher - a laundry-blue tube with a striped sock flopping out of the muzzle."""
    c = Canvas(16, 13)
    tube, tube_sh, sock, stripe = hexc("6a9ad8"), hexc("4a6aa8"), hexc("f0ece0"), hexc("e84a4a")
    c.rect(1, 3, 10, 5, tube)
    c.rect(1, 6, 10, 2, tube_sh)
    c.rect(10, 2, 2, 7, tube_sh)             # the muzzle rim
    c.rect(12, 3, 3, 3, sock)                # the sock, its foot hanging down
    c.rect(12, 3, 1, 3, stripe)
    c.rect(13, 6, 3, 3, sock)
    c.put(14, 0, hexc("b090e0"))             # a whiff of stink
    c.put(15, 1, hexc("b090e0"))
    c.rect(3, 8, 3, 4, METAL_D)              # grip
    c.outline()
    return c.img, 4, 5


def gun_hair_dryer():
    """D: a pink hair dryer - a round body, a nozzle ahead, a grip and its cable trailing off."""
    c = Canvas(16, 13)
    pink, pink_hi, pink_sh = hexc("ff8ab0"), hexc("ffc0d8"), hexc("d05a88")
    _disc(c, 5, 4.5, 4, pink)                # the motor body
    _disc(c, 4.5, 4, 2.5, pink_hi)
    _disc(c, 4, 4.5, 1.6, hexc("5a5a66"))    # the air vent at the back
    c.put(4, 4, hexc("9a9aa6"))
    c.rect(8, 3, 6, 4, pink)                 # the nozzle
    c.rect(8, 3, 6, 1, pink_hi)
    c.rect(14, 2, 2, 6, pink_sh)
    c.rect(14, 4, 2, 2, hexc("3a3a44"))      # its mouth
    c.rect(5, 8, 3, 4, pink_sh)              # grip
    c.put(6, 9, hexc("ffffff"))              # the on switch
    c.put(4, 12, hexc("2a2a30"))             # the cable
    c.put(3, 12, hexc("2a2a30"))
    c.outline()
    return c.img, 6, 5


def gun_cap_gun():
    """D: a toy cap gun - a shiny red plastic pistol with a silver barrel and a roll of caps showing."""
    c = Canvas(14, 9)
    red, red_hi, red_sh = hexc("e04a3a"), hexc("ff8a7a"), hexc("a02a20")
    c.rect(1, 1, 9, 3, red)                  # the body
    c.rect(1, 1, 9, 1, red_hi)
    c.rect(10, 1, 4, 2, SILVER)              # the barrel
    c.rect(10, 1, 4, 1, hexc("eef0f5"))
    c.rect(13, 1, 1, 2, hexc("ff9f3a"))      # the orange toy tip
    c.rect(4, 2, 3, 1, hexc("ffd23a"))       # the roll of caps in its window
    c.put(5, 2, red_sh)
    c.rect(1, 4, 3, 4, red_sh)               # grip
    c.rect(4, 4, 2, 1, red_sh)               # trigger guard
    c.put(2, 5, hexc("ffd23a"))              # a star on the grip
    c.outline()
    return c.img, 3, 2


def gun_pea_shooter():
    """D: a fat green pea pod, split open at the front with peas lined up inside."""
    c = Canvas(17, 9)
    pod, pod_hi, pod_sh, pea = hexc("5ab83a"), hexc("8ae86a"), hexc("3a8a2a"), hexc("a8f070")
    c.rect(2, 2, 13, 5, pod)                 # the pod
    c.rect(3, 1, 11, 1, pod_hi)
    c.rect(3, 7, 11, 1, pod_sh)
    c.rect(15, 3, 2, 3, pod_sh)              # the open tip
    for x in (5, 8, 11, 14):
        _disc(c, x, 4, 1.4, pea)             # peas inside
        c.put(x - 1, 3, hexc("e0ffc0"))
    c.rect(0, 4, 3, 1, hexc("6b4a2f"))       # the stalk
    c.rect(3, 7, 3, 2, pod_sh)               # grip
    c.outline()
    return c.img, 3, 4


def gun_sticky_darts():
    """C: a bright orange toy dart blaster with a dart loaded, its red suction cup poking out."""
    c = Canvas(17, 10)
    orange, orange_hi, blue = hexc("ff8a3a"), hexc("ffb070"), hexc("4f8ad8")
    c.rect(2, 2, 10, 4, orange)              # the body
    c.rect(2, 2, 10, 1, orange_hi)
    c.rect(4, 3, 5, 1, blue)                 # a blue stripe
    c.rect(0, 2, 2, 3, blue)                 # the plunger at the back
    c.rect(12, 3, 3, 2, hexc("f5f0e6"))      # the dart's foam
    c.rect(15, 2, 2, 4, hexc("d63a2f"))      # its suction cup
    c.rect(3, 6, 3, 4, hexc("d06a20"))       # grip
    c.rect(6, 6, 2, 1, blue)                 # trigger
    c.outline()
    return c.img, 4, 3


def gun_kazoo():
    """C: a golden kazoo with a round buzzer on top, music notes floating out of the end."""
    c = Canvas(16, 10)
    gold, gold_hi, gold_sh = hexc("e8c050"), hexc("fff0a0"), hexc("b08a2a")
    c.rect(1, 4, 10, 3, gold)                # the tube, narrowing ahead
    c.rect(1, 4, 10, 1, gold_hi)
    c.rect(11, 4, 2, 2, gold)
    c.rect(1, 6, 10, 1, gold_sh)
    _disc(c, 5, 3, 2, gold_sh)               # the buzzer on top
    _disc(c, 5, 3, 1.2, gold_hi)
    c.rect(14, 1, 1, 3, hexc("2a1a10"))      # a little music note
    c.rect(13, 3, 2, 1, hexc("2a1a10"))
    c.put(15, 1, hexc("2a1a10"))
    c.rect(1, 7, 3, 3, gold_sh)              # grip
    c.outline()
    return c.img, 3, 5


def gun_megaphone():
    """C: a red cheerleader's megaphone - a horn widening ahead with a white rim, a handle under it."""
    c = Canvas(16, 12)
    red, red_hi, red_sh = hexc("e04a3a"), hexc("ff8a6a"), hexc("a02a24")
    c.rect(1, 4, 3, 3, METAL_M)              # the mouthpiece
    c.rect(1, 4, 3, 1, METAL_L)
    for x in range(4, 14):                   # the horn, widening ahead
        half = 1 + (x - 4) * 4 // 9
        c.rect(x, 5 - half, 1, 2 * half + 1, red)
        c.put(x, 5 - half, red_hi)
        c.put(x, 5 + half, red_sh)
    c.rect(14, 0, 1, 11, hexc("f5f0e6"))     # the white rim of the bell
    c.rect(15, 1, 1, 9, hexc("c9c4b8"))
    c.rect(7, 4, 1, 3, hexc("f5f0e6"))       # a white stripe round the horn
    c.rect(4, 7, 2, 1, METAL_D)              # trigger
    c.rect(5, 8, 3, 4, hexc("3a3a44"))       # handle
    c.outline()
    return c.img, 5, 5


def gun_heirloom(level):
    """A: the Heirloom, the family's old pistol - it grows with its kills: tarnished brass at level 1,
    polished at 2, a longer barrel at 3, silver with a second barrel at 4, gold with a gem at 5."""
    c = Canvas(18, 10)
    if level >= 5:
        metal, hi, sh = hexc("e8c050"), hexc("fff0a0"), hexc("b08a2a")
    elif level >= 4:
        metal, hi, sh = SILVER, hexc("eef0f5"), METAL_L
    elif level >= 2:
        metal, hi, sh = hexc("c8a058"), hexc("e8c888"), hexc("8a6a3a")
    else:
        metal, hi, sh = hexc("8a7a5a"), hexc("a89a78"), hexc("5a4a32")
    length = 9 if level < 3 else 13
    c.rect(4, 2, length, 2, metal)           # the barrel
    c.rect(4, 2, length, 1, hi)
    if level >= 4:
        c.rect(6, 4, length - 2, 1, sh)      # a second barrel underneath
    c.rect(3, 1, 4, 4, sh)                   # the frame
    c.rect(3, 1, 4, 1, metal)
    c.rect(1, 4, 3, 5, hexc("5a3218"))       # the old walnut grip
    c.rect(1, 4, 1, 5, hexc("3a2010"))
    c.rect(4, 5, 2, 1, sh)                   # trigger guard
    if level == 1:
        c.put(8, 2, hexc("6a5a3a"))          # tarnish
        c.put(11, 3, hexc("6a5a3a"))
    if level >= 2:
        c.put(2, 6, hi)                      # a polished grip pin
    if level >= 3:
        for x in (8, 11, 14):
            c.put(x, 3, sh)                  # engraving
    if level >= 5:
        c.put(5, 3, hexc("e04a7a"))          # a gem set in the frame
        c.put(length + 3, 1, hexc("fff8c0")) # and a glint at the muzzle
    c.outline()
    return c.img, 3, 2


def gun_bottle_rocket_rack():
    """A: a wooden rack of four bottle rockets in bright paper, their sticks poking out the back."""
    c = Canvas(18, 12)
    for i, col in enumerate(("e04a3a", "3f8ad8", "f2c230", "5ab83a")):
        y = 1 + i * 2
        c.rect(0, y + 1, 6, 1, hexc("c98a4b"))    # the stick
        c.rect(6, y, 8, 2, hexc(col))            # the paper rocket
        c.rect(6, y, 8, 1, hexc("ffffff"))
        c.rect(14, y, 2, 2, hexc("f5f0e6"))      # its nose cone
        c.put(16, y, hexc("f5f0e6"))
    c.rect(5, 0, 2, 10, WOOD)                    # the rack
    c.rect(11, 0, 2, 10, WOOD_SH)
    c.rect(5, 10, 3, 2, WOOD_DK)                 # grip
    c.outline()
    return c.img, 6, 5


def gun_fluff_cannon():
    """A: a squat pink cannon with a giant pom-pom bulging out of its muzzle."""
    c = Canvas(22, 13)
    pink, pink_hi, pink_sh = hexc("ff9fc4"), hexc("ffd0e4"), hexc("d06a98")
    c.rect(3, 4, 12, 6, METAL_D)             # the barrel
    c.rect(3, 4, 12, 1, METAL_L)
    c.rect(6, 4, 1, 6, hexc("d8a83a"))       # brass bands
    c.rect(11, 4, 1, 6, hexc("d8a83a"))
    c.rect(0, 5, 3, 4, METAL_M)              # the back of it
    _disc(c, 17, 7, 4.6, pink_sh)            # the pom-pom
    _disc(c, 17, 7, 3.8, pink)
    for x, y in ((14, 3), (20, 4), (19, 10), (15, 11), (21, 7)):
        c.put(x, y, pink)                    # tufts
    _disc(c, 16, 6, 1.4, pink_hi)
    c.rect(4, 10, 3, 3, WOOD_SH)             # grip
    c.outline()
    return c.img, 5, 7


def gun_duck_launcher():
    """B: a bathtub-blue launcher with a rubber duck peeking out of the muzzle."""
    c = Canvas(20, 12)
    blue, blue_hi, blue_sh = hexc("5ab8f0"), hexc("a0dcff"), hexc("3a88c0")
    duck, beak = hexc("ffd23a"), hexc("ff9f3a")
    c.rect(2, 4, 11, 5, blue)                # the tube
    c.rect(2, 4, 11, 1, blue_hi)
    c.rect(2, 8, 11, 1, blue_sh)
    c.rect(13, 3, 2, 7, blue_sh)             # the muzzle rim
    _disc(c, 16.5, 7, 2.5, duck)             # the duck's body
    _disc(c, 17, 4, 1.8, duck)               # its head
    c.rect(18, 4, 2, 1, beak)
    c.put(17, 3, hexc("2a1a10"))             # an eye
    for x, y in ((5, 2), (8, 1), (10, 2)):
        c.put(x, y, hexc("e0f6ff"))          # bath bubbles
    c.rect(3, 9, 3, 3, blue_sh)              # grip
    c.outline()
    return c.img, 4, 6


def gun_unicorn_horn():
    """S: a spiral unicorn horn on a white-and-gold grip, glowing with a rainbow at the tip."""
    c = Canvas(20, 9)
    pearl, gold = hexc("fbf6ff"), hexc("e8c050")
    for x in range(5, 18):                   # the horn tapers to a point
        h = max(1, 5 - (x - 5) // 3)
        top = 4 - h // 2
        c.rect(x, top, 1, h, pearl)
        if x % 2 == 0:
            c.put(x, top + h - 1, hexc("d8c8f0"))  # the spiral
    for i, col in enumerate(("ff4a4a", "ffd23a", "5ae06a", "4f8ad8")):
        c.put(18 + i % 2, 3 + i // 2 - (1 if i < 2 else 0), hexc(col))  # rainbow sparkle at the tip
    c.rect(2, 2, 4, 5, gold)                 # the gold collar
    c.rect(2, 2, 4, 1, hexc("fff0a0"))
    c.rect(0, 4, 3, 5, pearl)                # the grip
    c.put(1, 5, hexc("ff9fe0"))
    c.outline()
    return c.img, 3, 4


def gun_shooting_star():
    """S: a wand of dark blue with a big golden star on the end, trailing sparkles."""
    c = Canvas(18, 13)
    star, star_hi = hexc("ffd23a"), hexc("fff8c0")
    c.rect(0, 6, 10, 2, hexc("2f3a7a"))      # the wand
    c.rect(0, 6, 10, 1, hexc("5a6ac0"))
    for x in (3, 6):
        c.put(x, 7, hexc("e8c050"))          # gold rings
    cx, cy = 13, 6
    c.rect(cx - 1, cy - 5, 3, 11, star)      # a five-pointed star, chunky
    c.rect(cx - 5, cy - 1, 11, 3, star)
    c.rect(cx - 3, cy - 3, 7, 7, star)
    c.rect(cx - 1, cy - 1, 2, 2, star_hi)
    c.put(cx - 4, cy + 4, star)
    c.put(cx + 4, cy + 4, star)
    c.put(2, 3, star_hi)                     # sparkles
    c.put(6, 10, star_hi)
    c.outline()
    return c.img, 3, 6


def gun_disco_ball():
    """S: a mirror ball held on a short silver handle, its tiles catching rainbow light."""
    c = Canvas(18, 13)
    c.rect(0, 6, 7, 2, SILVER)               # the handle
    c.rect(0, 6, 7, 1, hexc("f2f4fa"))
    c.rect(6, 5, 2, 4, hexc("8a8a96"))       # the cap it hangs from
    _disc(c, 12.5, 6.5, 5.5, hexc("c8ccd8"))
    tiles = ("ff6a8a", "ffd23a", "6ae0ff", "c08aff", "ffffff", "8aff9a")
    for i, (x, y) in enumerate(((10, 3), (13, 3), (9, 6), (12, 6), (15, 6), (10, 9), (13, 9), (11, 5))):
        c.put(x, y, hexc(tiles[i % len(tiles)]))
        c.put(x + 1, y, hexc("e8ecf4"))
    for y in (4, 7, 10):
        for x in range(8, 18, 2):
            if c.px[x, y][3]:
                c.put(x, y, hexc("9a9eac"))   # the grid between the mirrors
    c.put(10, 2, hexc("ffffff"))
    c.outline()
    return c.img, 2, 6


def gun_teapot_tempest():
    """S: granny's best teapot - round, blue and white china, a gold lid knob, steam and a spark at the spout."""
    c = Canvas(20, 13)
    china, china_sh, blue = hexc("f4f6fc"), hexc("c8d0e4"), hexc("3a6ad8")
    c.rect(0, 5, 3, 5, china_sh)             # the handle at the back
    c.rect(1, 6, 1, 3, (0, 0, 0, 0))
    _disc(c, 8.5, 7.5, 5, china)
    c.rect(4, 10, 9, 2, china_sh)            # its foot
    for x in (6, 9, 11):
        c.put(x, 7, blue)                    # the blue flowers
        c.put(x, 8, blue)
        c.put(x - 1, 8, blue)
    c.rect(6, 1, 5, 2, china)                # the lid and its gold knob
    c.rect(8, 0, 1, 1, hexc("e8c050"))
    c.rect(13, 5, 3, 2, china)               # the spout, rising to the right
    c.rect(15, 4, 2, 2, china)
    c.put(17, 3, hexc("e0ecff"))             # a puff of steam
    c.put(18, 2, hexc("e0ecff"))
    c.put(19, 4, hexc("fff060"))             # and a spark of lightning in it
    c.put(18, 5, hexc("fff060"))
    c.outline()
    return c.img, 3, 4


def gun_meteor_staff():
    """S: a long wooden staff with a lump of moon rock in a golden claw on top, glowing."""
    c = Canvas(22, 11)
    wood, wood_hi = hexc("8a5a34"), hexc("b07a48")
    c.rect(0, 5, 15, 2, wood)
    c.rect(0, 5, 15, 1, wood_hi)
    for x in (4, 9):
        c.put(x, 6, hexc("e8c050"))          # gold bands
    c.rect(14, 3, 2, 6, hexc("e8c050"))      # the claw holding the rock
    _disc(c, 18.5, 5.5, 3.5, hexc("9a94a8"))
    c.put(17, 4, hexc("6c6680"))             # craters
    c.put(19, 7, hexc("6c6680"))
    c.put(20, 4, hexc("ffd23a"))             # a glow
    c.put(18, 2, hexc("fff1a0"))
    c.outline()
    return c.img, 3, 5


def gun_pearl_revolver():
    """Granny's antique revolver: a long engraved silver barrel, a fluted cylinder, gold hammer and
    trigger, and a curved mother-of-pearl grip."""
    c = Canvas(18, 10)
    silver_hi, gold, pearl, pearl_sh, pearl_hi = hexc("f2f4fa"), hexc("e8c050"), hexc("f4ecf4"), hexc("c8b8d0"), hexc("ffffff")
    c.rect(8, 2, 9, 2, SILVER)               # the long barrel
    c.rect(8, 2, 9, 1, silver_hi)
    for x in (10, 12, 14):
        c.put(x, 3, gold)                    # engraved swirls
    c.rect(16, 1, 1, 1, SILVER)              # front sight
    c.rect(4, 1, 5, 5, METAL_L)              # fluted cylinder
    c.rect(4, 1, 5, 1, silver_hi)
    for x in (5, 7):
        c.rect(x, 2, 1, 3, METAL_M)
    c.put(3, 0, gold)                        # the hammer
    c.rect(3, 1, 1, 2, gold)
    c.rect(5, 6, 3, 1, gold)                 # trigger guard
    c.put(6, 7, gold)
    c.rect(1, 4, 3, 5, pearl)                # the pearl grip, curving back
    c.rect(0, 7, 2, 3, pearl)
    c.rect(1, 4, 1, 3, pearl_hi)
    c.rect(3, 6, 1, 3, pearl_sh)
    c.put(2, 6, hexc("e8c8e0"))              # a pink pearly sheen
    c.put(1, 8, hexc("d8e8f8"))              # and a blue one
    c.outline()
    return c.img, 3, 2


def gun_royal_sceptre():
    """The Fluffy King's starter (pocket size): a gold sceptre with a ball of pink fluff on its tip, a little
    crown under it - it fires fluff balls that burst into rings."""
    c = Canvas(17, 9)
    gold, gold_sh, gold_hi = hexc("ffc83d"), hexc("d8962a"), hexc("fff0a0")
    c.rect(0, 4, 11, 2, gold)                # the rod
    c.rect(0, 5, 11, 1, gold_sh)
    c.rect(2, 4, 3, 1, gold_hi)
    c.rect(4, 3, 1, 4, gold)                 # a ring round it
    c.rect(10, 2, 2, 6, gold)                # the little crown holding the fluff
    c.put(11, 1, gold)
    c.put(11, 7, gold)
    disc(c, 14, 4, 3, hexc("ffd6e8"))        # the fluff ball
    disc(c, 13, 3, 1, hexc("fff0f6"))
    c.put(16, 5, hexc("f5a3c7"))
    c.put(15, 6, hexc("f5a3c7"))
    c.outline()
    return c.img, 2, 4


def gun_double_barrel():
    """Grandad's old side-by-side shotgun: two long dark barrels, an engraved silver lock, a polished
    walnut stock worn pale where he's held it for years."""
    c = Canvas(24, 9)
    walnut, walnut_hi, walnut_sh = hexc("7a4a28"), hexc("a06a3a"), hexc("4e2c16")
    c.rect(10, 1, 14, 2, METAL_D)            # the top barrel
    c.rect(10, 1, 14, 1, METAL_L)
    c.rect(10, 3, 14, 2, METAL_M)            # the one beside it
    c.rect(10, 4, 14, 1, METAL_D)
    c.put(23, 0, SILVER)                     # bead sight
    c.rect(12, 5, 6, 2, walnut)              # fore-end under the barrels
    c.rect(12, 5, 6, 1, walnut_hi)
    c.rect(6, 1, 4, 5, SILVER)               # the lock, engraved
    c.rect(6, 1, 4, 1, hexc("eef0f5"))
    c.put(7, 3, METAL_M)
    c.put(8, 4, METAL_M)
    c.put(9, 2, METAL_M)
    c.rect(7, 6, 3, 1, METAL_D)              # trigger guard
    c.put(8, 7, METAL_D)
    c.rect(0, 2, 7, 4, walnut)               # the stock
    c.rect(0, 2, 7, 1, walnut_hi)
    c.rect(0, 5, 4, 2, walnut)
    c.rect(0, 6, 4, 1, walnut_sh)
    c.rect(3, 3, 2, 1, hexc("c08a58"))       # worn pale where he holds it
    c.rect(0, 2, 1, 5, walnut_sh)            # butt plate
    c.outline()
    return c.img, 9, 2


def gun_baby_rattle():
    """The baby brother's rattle: a round see-through head full of coloured beads on a chubby pink
    handle with a ring at the end."""
    c = Canvas(14, 10)
    handle, handle_sh, ring = hexc("ff9ad0"), hexc("e070b0"), hexc("6ab0f0")
    c.rect(0, 3, 2, 3, ring)                 # the ring at the end of the handle
    c.put(0, 4, (0, 0, 0, 0))
    c.rect(2, 4, 5, 2, handle)               # the handle
    c.rect(2, 5, 5, 1, handle_sh)
    for x in range(7, 14):                   # the round head
        for y in range(0, 10):
            if (x - 10) ** 2 + (y - 4.5) ** 2 <= 11:
                c.put(x, y, hexc("fff4c0"))
    for x, y, col in ((9, 3, "ff5a5a"), (11, 2, "5ad1ff"), (10, 5, "7bed6f"), (12, 5, "ffd23a"), (9, 6, "c77dff")):
        c.put(x, y, hexc(col))               # the beads inside
    c.put(8, 2, (255, 255, 255, 255))        # a shine
    c.outline()
    return c.img, 3, 4


def gun_nail_gun():
    """C: a builder's nail gun - safety-orange body, a black nose, a strip of nails underneath."""
    c = Canvas(17, 11)
    orange, orange_hi, orange_sh = hexc("ff8a2a"), hexc("ffb46a"), hexc("c85a10")
    c.rect(2, 2, 11, 5, orange)              # the body
    c.rect(2, 2, 11, 1, orange_hi)
    c.rect(2, 6, 11, 1, orange_sh)
    c.rect(13, 3, 3, 3, METAL_D)             # the nose
    c.rect(16, 4, 1, 1, METAL_L)
    c.rect(8, 7, 6, 2, METAL_M)              # the nail strip
    for x in range(8, 14, 2):
        c.put(x, 9, SILVER)
    c.rect(3, 7, 3, 4, hexc("3a3a44"))       # grip
    c.rect(3, 7, 3, 1, hexc("5a5a66"))
    c.outline()
    return c.img, 4, 4


def gun_kitty_cannon():
    """B: Egor's kitty cannon - a cat-carrier of a gun, pink with a paw print, a kitten peeking out of the muzzle."""
    c = Canvas(20, 13)
    pink, pink_hi, pink_sh = hexc("ff9ac8"), hexc("ffc8e2"), hexc("d86aa0")
    fur, fur_d, nose = hexc("ffb45a"), hexc("d8842a"), hexc("ff5f8a")
    c.rect(2, 4, 11, 6, pink)                # the carrier
    c.rect(2, 4, 11, 1, pink_hi)
    c.rect(2, 9, 11, 1, pink_sh)
    _disc(c, 6, 7, 1.3, pink_sh)             # a paw print on the side
    for x, y in ((4, 5), (6, 4.6), (8, 5)):
        c.put(int(x), int(y) + 1, pink_sh)
    c.rect(13, 3, 2, 8, pink_sh)             # the muzzle rim
    _disc(c, 17, 7, 2.6, fur)                # the kitten's head
    c.rect(15, 3, 2, 2, fur)                 # its ears
    c.rect(18, 3, 2, 2, fur)
    c.put(15, 3, fur_d)
    c.put(19, 3, fur_d)
    c.put(16, 6, hexc("2a1a10"))             # eyes
    c.put(18, 6, hexc("2a1a10"))
    c.put(17, 8, nose)
    c.rect(3, 10, 3, 3, pink_sh)             # grip
    c.outline()
    return c.img, 3, 7


GUN_SPRITES = [("rusty_sidearm", gun_rusty_sidearm), ("fluff_shotgun", gun_fluff_shotgun),
               ("pocket_uzi", gun_pocket_uzi), ("big_magnum", gun_big_magnum),
               ("confetti_popper", gun_confetti_popper), ("bubble_blaster", gun_bubble_blaster),
               ("banana_rang", gun_banana_rang), ("bee_hive", gun_bee_hive), ("rainbow_ray", gun_rainbow_ray),
               ("laser_pointer", gun_laser_pointer), ("bouncer", gun_bouncer), ("plasma_zapper", gun_plasma_zapper),
               ("gull_gatling", gun_gull_gatling), ("egg_lobber", gun_egg_lobber), ("party_rocket", gun_party_rocket),
               ("flufful", gun_flufful), ("long_shot", gun_long_shot),
               ("water_pistol", gun_water_pistol), ("snowball_slinger", gun_snowball_slinger),
               ("pillow_cannon", gun_pillow_cannon), ("jumper_cables", gun_jumper_cables),
               ("frying_pan", gun_frying_pan), ("porcupine", gun_porcupine),
               ("dust_bunny", gun_dust_bunny), ("polaroid", gun_polaroid),
               ("acid_squirter", gun_acid_squirter), ("flare_gun", gun_flare_gun),
               ("gloop_launcher", gun_gloop_launcher),
               ("chomper", gun_chomper), ("boom_buss", gun_boom_buss), ("freeze_ray", gun_freeze_ray),
               ("throwing_stars", gun_throwing_stars), ("pearl_revolver", gun_pearl_revolver),
               ("double_barrel", gun_double_barrel), ("slingshot", gun_slingshot),
               ("popcorn_popper", gun_popcorn_popper), ("hair_dryer", gun_hair_dryer), ("cap_gun", gun_cap_gun),
               ("pea_shooter", gun_pea_shooter), ("sticky_darts", gun_sticky_darts), ("kazoo", gun_kazoo),
               ("megaphone", gun_megaphone),
               ("bottle_rocket_rack", gun_bottle_rocket_rack), ("fluff_cannon", gun_fluff_cannon),
               ("duck_launcher", gun_duck_launcher), ("unicorn_horn", gun_unicorn_horn),
               ("shooting_star", gun_shooting_star), ("baby_rattle", gun_baby_rattle),
               ("royal_sceptre", gun_royal_sceptre), ("paper_planes", gun_paper_planes),
               ("tennis_balls", gun_tennis_balls), ("crayon_box", gun_crayon_box), ("pinwheel", gun_pinwheel),
               ("sock_launcher", gun_sock_launcher), ("disco_ball", gun_disco_ball),
               ("teapot_tempest", gun_teapot_tempest), ("meteor_staff", gun_meteor_staff),
               ("nail_gun", gun_nail_gun), ("kitty_cannon", gun_kitty_cannon)] + \
              [("heirloom_%d" % lv, (lambda lv=lv: gun_heirloom(lv))) for lv in range(1, 6)]


def guns_sheet():
    parts = []
    for name, fn in GUN_SPRITES:
        img, gx, gy = fn()
        parts.append((name, detailed_sprite(img, "metal"), gx, gy))
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


def item_socks():
    """Bouncy Socks: a striped sock on a spring."""
    c = Canvas(16, 16)
    for y in range(1, 9):
        c.rect(5, y, 5, 1, hexc("ff5fa2") if (y // 2) % 2 == 0 else hexc("f5f0e6"))  # the leg, striped
    c.rect(5, 9, 8, 3, hexc("ff5fa2"))       # the foot
    c.rect(11, 9, 2, 3, hexc("f5f0e6"))      # the toe
    for i, y in enumerate((12, 13, 14, 15)):
        c.rect(6 + (i % 2) * 2, y, 4, 1, METAL_L)  # a spring under the heel
    c.outline()
    return c.img


def item_cardboard():
    """Cardboard Hero: a cardboard cutout of the hero on a little stand."""
    c = Canvas(16, 16)
    c.rect(3, 1, 10, 12, hexc("c8a070"))     # the board
    c.rect(3, 1, 10, 1, hexc("e0c090"))
    _disc(c, 8, 4.5, 2.4, hexc("f2c8a0"))    # painted-on face
    c.rect(6, 2, 4, 1, hexc("6b4a2f"))       # hair
    c.put(7, 4, OUTLINE)
    c.put(9, 4, OUTLINE)
    c.rect(5, 7, 6, 5, hexc("3fa85a"))       # shirt
    c.rect(7, 13, 2, 2, hexc("9a7448"))      # the stand
    c.rect(5, 15, 6, 1, hexc("9a7448"))
    c.outline()
    return c.img


def item_map():
    """Treasure Map: an old parchment with a dotted path to a red X."""
    c = Canvas(16, 16)
    c.rect(1, 2, 14, 12, hexc("f0d8a0"))
    c.rect(1, 2, 14, 1, hexc("d8b878"))
    c.rect(1, 13, 14, 1, hexc("c8a060"))
    for x, y in ((3, 10), (4, 9), (6, 9), (7, 8), (8, 6), (9, 5)):
        c.put(x, y, hexc("8a6a48"))          # the dotted path
    for i in range(3):
        c.put(10 + i, 3 + i, hexc("d63a2f"))  # X marks the spot
        c.put(12 - i, 3 + i, hexc("d63a2f"))
    c.outline()
    return c.img


def item_robot():
    """Toy Robot: a little tin wind-up robot with a key on its back."""
    c = Canvas(16, 16)
    c.rect(4, 6, 8, 7, hexc("d8dce6"))       # body
    c.rect(4, 6, 8, 1, hexc("f0f2f8"))
    c.rect(5, 1, 6, 5, hexc("c0c6d4"))       # head
    c.put(6, 3, hexc("ff5a4a"))              # glowing eyes
    c.put(9, 3, hexc("ff5a4a"))
    c.rect(7, 0, 2, 1, hexc("8a90a0"))       # antenna
    c.rect(12, 8, 3, 1, hexc("e8c040"))      # the wind-up key
    c.rect(14, 7, 1, 3, hexc("e8c040"))
    c.rect(5, 13, 2, 2, hexc("8a90a0"))      # feet
    c.rect(9, 13, 2, 2, hexc("8a90a0"))
    c.rect(6, 8, 4, 2, hexc("8a90a0"))       # a dial on its chest
    c.outline()
    return c.img


def item_sweater():
    """Prickly Sweater: a green knitted jumper with quills sticking out of it."""
    c = Canvas(16, 16)
    c.rect(4, 4, 8, 10, hexc("3f8a4a"))      # body
    c.rect(1, 4, 3, 7, hexc("3f8a4a"))       # sleeves
    c.rect(12, 4, 3, 7, hexc("3f8a4a"))
    c.rect(6, 4, 4, 1, hexc("2a6a34"))       # collar
    for x in range(4, 12, 2):
        c.put(x, 9, hexc("f5f0e6"))          # a knitted pattern
    for x, y in ((3, 2), (7, 1), (11, 2), (0, 7), (15, 7), (5, 15), (10, 15)):
        c.put(x, y, hexc("e8d8b0"))          # quills poking out
    c.outline()
    return c.img


def item_globe():
    """Snow Globe: a glass dome with a tiny snowy tree, on a wooden base."""
    c = Canvas(16, 16)
    _disc(c, 8, 7, 6, hexc("bfe6ff"))        # the glass
    c.rect(4, 2, 2, 2, hexc("ffffff"))       # shine
    c.rect(7, 6, 2, 1, hexc("2f8a4a"))       # a little tree
    c.rect(6, 7, 4, 2, hexc("2f8a4a"))
    c.rect(4, 9, 8, 2, hexc("ffffff"))       # snow on the ground inside
    for x, y in ((5, 5), (10, 4), (11, 7), (6, 3)):
        c.put(x, y, hexc("ffffff"))          # flakes
    c.rect(3, 12, 10, 3, WOOD)               # base
    c.rect(3, 12, 10, 1, WOOD_HI)
    c.outline()
    return c.img


def item_cloud():
    """Little Cloud: a tiny grey cloud with a face, a lightning bolt underneath."""
    c = Canvas(16, 16)
    for cx, cy, r in ((5, 6, 3.2), (9, 4.5, 3.6), (12, 7, 2.6), (8, 7.5, 3)):
        _disc(c, cx, cy, r, hexc("c8d0e0"))
    _disc(c, 8, 4, 2, hexc("ffffff"))
    c.put(6, 7, OUTLINE)                     # sleepy face
    c.put(10, 7, OUTLINE)
    for x, y in ((8, 10), (7, 11), (8, 12), (7, 13), (6, 14)):
        c.put(x, y, hexc("ffe94a"))          # a zig-zag bolt
        c.put(x + 1, y, hexc("ffe94a"))
    c.outline()
    return c.img


def item_lamb():
    """Guardian Lamb: a cuddly plush lamb with a little golden halo."""
    c = Canvas(16, 16)
    for x in range(5, 12):
        c.put(x, 1, hexc("ffd23a"))          # halo
    for cx, cy in ((5, 8), (8, 7), (11, 8), (6, 10.5), (10, 10.5), (8, 10)):
        _disc(c, cx, cy, 2.6, hexc("fbf6ff"))  # woolly body
    _disc(c, 8, 4.5, 2.4, hexc("4a3a3a"))    # face
    c.rect(5, 3, 1, 2, hexc("4a3a3a"))       # ears
    c.rect(10, 3, 1, 2, hexc("4a3a3a"))
    c.put(7, 4, hexc("ffffff"))
    c.put(9, 4, hexc("ffffff"))
    c.rect(5, 13, 2, 2, hexc("4a3a3a"))      # legs
    c.rect(9, 13, 2, 2, hexc("4a3a3a"))
    c.outline()
    return c.img


def item_boots():
    """Acid Boots: tall green rubber boots with a yellow hazard stripe."""
    c = Canvas(16, 16)
    for x0 in (2, 8):
        c.rect(x0 + 1, 2, 4, 9, hexc("4aa83a"))   # the shaft
        c.rect(x0 + 1, 2, 1, 9, hexc("6ac85a"))
        c.rect(x0 + 1, 6, 4, 1, hexc("ffd23a"))   # hazard stripe
        c.rect(x0, 11, 7 if x0 == 8 else 6, 3, hexc("3a8a2a"))  # the foot
        c.rect(x0, 13, 7 if x0 == 8 else 6, 1, hexc("2a5a1a"))
    c.outline()
    return c.img


def item_helmet():
    """Bike Helmet: a shiny red helmet with air vents and a strap."""
    c = Canvas(16, 16)
    _disc(c, 8, 9, 6.5, hexc("e8443a"))
    c.rect(0, 9, 16, 7, (0, 0, 0, 0))              # cut it in half: a dome
    c.rect(1, 9, 14, 2, hexc("b8342a"))            # the rim
    for x in (5, 8, 11):
        c.rect(x, 4, 1, 3, hexc("3a1a1a"))         # vents
    c.rect(4, 4, 1, 1, hexc("ffb0a0"))             # shine
    c.rect(3, 11, 1, 3, hexc("3a3a44"))            # strap
    c.rect(12, 11, 1, 3, hexc("3a3a44"))
    c.rect(4, 14, 8, 1, hexc("3a3a44"))
    c.outline()
    return c.img


def item_frost():
    """Frost Rounds: three icy blue bullets."""
    c = Canvas(16, 16)
    for x0 in (1, 6, 11):
        c.rect(x0, 7, 4, 7, hexc("8ad8ff"))
        _disc(c, x0 + 2, 7, 2, hexc("dff4ff"))
        c.rect(x0, 12, 4, 2, hexc("5aa8d8"))
        c.put(x0 + 1, 9, hexc("ffffff"))
    c.put(7, 1, hexc("ffffff"))                     # a snowflake sparkle
    c.rect(6, 2, 3, 1, hexc("ffffff"))
    c.put(7, 3, hexc("ffffff"))
    c.outline()
    return c.img


def item_ember():
    """Ember Rounds: three bullets with glowing orange tips and a little flame."""
    c = Canvas(16, 16)
    for x0 in (1, 6, 11):
        c.rect(x0, 8, 4, 6, hexc("d9b24c"))
        _disc(c, x0 + 2, 8, 2, hexc("ff7a2a"))
        c.put(x0 + 2, 7, hexc("ffd84a"))
        c.rect(x0, 12, 4, 2, hexc("a47e2a"))
    c.rect(7, 1, 2, 3, hexc("ff8a1e"))               # the flame
    c.put(7, 0, hexc("ffd84a"))
    c.put(8, 2, hexc("ffd84a"))
    c.outline()
    return c.img


def item_moons():
    """Pom-Pom Moons: three pom-poms in a ring."""
    c = Canvas(16, 16)
    for x in range(16):
        for y in range(16):
            d = math.hypot(x + 0.5 - 8, (y + 0.5 - 8) * 1.2)
            if 5.2 <= d < 6.0:
                c.put(x, y, hexc("c8b8e8"))         # the orbit
    for (cx, cy), col in (((8, 2.5), "ff8ac8"), ((3, 11), "8ad8ff"), ((13, 11), "ffe08a")):
        _disc(c, cx, cy, 2.6, hexc(col))
        c.put(int(cx) - 1, int(cy) - 1, hexc("ffffff"))
    c.outline()
    return c.img


def _moon_icon(col, hi, r):
    """A single moon on its orbit (the Guard, Mirror and Blue Moons)."""
    c = Canvas(16, 16)
    for x in range(16):
        for y in range(16):
            d = math.hypot(x + 0.5 - 8, (y + 0.5 - 8) * 1.2)
            if 6.2 <= d < 7.0:
                c.put(x, y, hexc("c8b8e8"))         # the orbit
    _disc(c, 8, 8, r, col)
    _disc(c, 7, 7, r * 0.45, hi)
    return c


def item_guardmoon():
    """Guard Moon: one big silver moon on its orbit."""
    c = _moon_icon(hexc("c8ccd8"), hexc("eef0f6"), 4.6)
    c.put(10, 10, hexc("9a9eb0"))                   # a crater or two
    c.put(9, 11, hexc("9a9eb0"))
    c.outline()
    return c.img


def item_mirrormoon():
    """Mirror Moon: a pearly moon with a bright streak of shine across it."""
    c = _moon_icon(hexc("dce6fa"), hexc("ffffff"), 4)
    for i in range(5):
        c.put(6 + i, 10 - i, hexc("ffffff"))
    c.put(11, 11, hexc("a8b8d8"))
    c.outline()
    return c.img


def item_bluemoon():
    """Blue Moon: a deep blue moon with clock hands on it - time stands still."""
    c = _moon_icon(hexc("3a6ae8"), hexc("7aa0ff"), 4.6)
    c.rect(8, 5, 1, 4, hexc("ffffff"))              # the hands
    c.rect(8, 8, 3, 1, hexc("ffffff"))
    c.outline()
    return c.img


def item_teddy():
    """Grumpy Teddy: a brown teddy bear's head with cross eyebrows and a frown, steaming."""
    c = Canvas(16, 16)
    fur, fur_sh, muzzle = hexc("a8703a"), hexc("7a4e28"), hexc("e8c89a")
    _disc(c, 3.5, 5, 2.4, fur)                      # ears
    _disc(c, 12.5, 5, 2.4, fur)
    _disc(c, 3.5, 5, 1.1, muzzle)
    _disc(c, 12.5, 5, 1.1, muzzle)
    _disc(c, 8, 9.5, 5.6, fur)                      # the head
    c.rect(4, 13, 8, 2, fur_sh)
    _disc(c, 8, 11.5, 2.4, muzzle)                  # the muzzle
    c.rect(7, 10, 2, 1, hexc("3a2018"))             # the nose
    c.rect(6, 13, 4, 1, hexc("3a2018"))             # a frown
    c.put(5, 12, hexc("3a2018"))
    c.put(10, 12, hexc("3a2018"))
    c.rect(4, 7, 3, 1, hexc("3a2018"))              # cross eyebrows, slanting in
    c.put(6, 8, hexc("3a2018"))
    c.rect(9, 7, 3, 1, hexc("3a2018"))
    c.put(9, 8, hexc("3a2018"))
    c.put(5, 9, hexc("1a1010"))                     # eyes
    c.put(10, 9, hexc("1a1010"))
    c.put(14, 1, hexc("ffffff"))                    # steam
    c.put(15, 0, hexc("ffffff"))
    c.put(1, 1, hexc("ffffff"))
    c.outline()
    return c.img


def item_mittens():
    """Quick Mittens: a pair of knitted red mittens with white cuffs and speed lines."""
    c = Canvas(16, 16)
    for x0 in (3, 9):
        c.rect(x0, 4, 5, 7, hexc("e84a4a"))
        c.rect(x0 + (-1 if x0 == 3 else 5), 6, 1, 3, hexc("e84a4a"))   # thumbs
        c.rect(x0, 11, 5, 3, hexc("fbf6ff"))                          # cuffs
        c.put(x0 + 1, 5, hexc("ff8a8a"))
        for y in (7, 9):
            c.put(x0 + 2, y, hexc("b8342a"))                          # knit
    c.outline()
    for y in (3, 6):
        c.rect(0, y, 2, 1, hexc("ffffff"))                            # whoosh
    return c.img


def item_lunch():
    """Packed Lunch: a sandwich in a little red lunch box."""
    c = Canvas(16, 16)
    c.rect(1, 8, 14, 7, hexc("d8443a"))            # the box
    c.rect(1, 8, 14, 1, hexc("ff7a6a"))
    c.rect(6, 10, 4, 2, hexc("ffd23a"))            # latch
    c.rect(3, 4, 10, 2, hexc("f0c070"))            # the sandwich on top: bread
    c.rect(3, 6, 10, 1, hexc("6ac85a"))            # lettuce
    c.rect(3, 7, 10, 1, hexc("f0c070"))
    c.rect(4, 3, 8, 1, hexc("f8d898"))
    c.outline()
    return c.img


def item_piggy():
    """Piggy Bank: a round pink pig with a coin going in the slot."""
    c = Canvas(16, 16)
    _disc(c, 8, 10, 5.2, hexc("ffaacc"))           # body
    c.rect(4, 14, 2, 2, hexc("e888aa"))            # legs
    c.rect(10, 14, 2, 2, hexc("e888aa"))
    c.rect(12, 9, 3, 3, hexc("ff8ab8"))            # snout
    c.put(13, 10, hexc("b8506a"))
    c.put(10, 8, hexc("3a2a2a"))                   # eye
    c.rect(4, 5, 2, 2, hexc("ff8ab8"))             # ear
    c.rect(6, 5, 4, 1, hexc("b8506a"))             # the slot
    c.rect(7, 1, 2, 4, hexc("ffd23a"))             # a coin dropping in
    c.put(7, 2, hexc("fff0a0"))
    c.outline()
    return c.img


def item_telescope():
    """Toy Telescope: a chunky striped spyglass on a little stand."""
    c = Canvas(16, 16)
    c.rect(1, 6, 3, 3, hexc("9a7a2a"))            # eyepiece
    c.rect(4, 5, 4, 5, hexc("4a7ad8"))            # the tube, wider and wider
    c.rect(8, 4, 2, 7, hexc("e8c030"))
    c.rect(10, 3, 4, 9, hexc("4a7ad8"))
    c.rect(14, 3, 1, 9, hexc("e8c030"))
    c.rect(5, 6, 2, 1, hexc("8ab0ff"))            # shine
    c.rect(11, 4, 2, 1, hexc("8ab0ff"))
    c.rect(8, 11, 1, 4, hexc("9a7a2a"))           # the stand
    c.rect(6, 14, 5, 1, hexc("9a7a2a"))
    c.outline()
    return c.img


def item_rubber():
    """Rubber Rounds: a stack of three striped bouncy balls."""
    c = Canvas(16, 16)
    for cx, cy, col in ((4.5, 11.5, "e84a4a"), (11.5, 11.5, "4a9ae8"), (8, 5, "6ac85a")):
        _disc(c, cx, cy, 3.4, hexc(col))
        c.rect(int(cx) - 3, int(cy), 7, 1, hexc("ffd84a"))   # a stripe round the middle
        c.put(int(cx) - 1, int(cy) - 2, hexc("ffffff"))
    c.outline()
    return c.img


def item_plushie():
    """Star Plushie: a smiling yellow star with stitches."""
    c = Canvas(16, 16)
    pts = []
    for k in range(10):
        r = 7.2 if k % 2 == 0 else 3.2
        a = math.radians(-90 + 36 * k)
        pts.append((8 + math.cos(a) * r, 8.5 + math.sin(a) * r))
    for y in range(16):
        for x in range(16):
            px, py, inside = x + 0.5, y + 0.5, False
            for i in range(10):                       # point in polygon
                (x1, y1), (x2, y2) = pts[i], pts[(i + 1) % 10]
                if (y1 > py) != (y2 > py) and px < x1 + (py - y1) * (x2 - x1) / (y2 - y1):
                    inside = not inside
            if inside:
                c.put(x, y, hexc("ffd84a"))
    c.put(6, 8, hexc("3a2a2a"))                   # eyes
    c.put(10, 8, hexc("3a2a2a"))
    c.rect(7, 10, 3, 1, hexc("d84a6a"))            # smile
    c.put(5, 10, hexc("ffa8c8"))                   # cheeks
    c.put(11, 10, hexc("ffa8c8"))
    c.outline()
    return c.img


def item_puppy():
    """Puppy Rounds: a bullet with floppy dog ears and a nose."""
    c = Canvas(16, 16)
    c.rect(3, 6, 8, 6, hexc("d9b24c"))             # casing
    _disc(c, 11, 9, 3, hexc("e8c070"))             # round nose of the bullet
    c.rect(2, 6, 2, 6, hexc("a47e2a"))
    c.rect(5, 3, 3, 4, hexc("8a5a3a"))             # floppy ears
    c.rect(5, 11, 3, 3, hexc("8a5a3a"))
    c.put(10, 7, hexc("3a2a2a"))                   # eye
    c.rect(13, 8, 2, 2, hexc("3a2a2a"))            # nose
    c.outline()
    return c.img


def item_wand():
    """Bubble Wand: a pink wand with a soap ring and a bubble floating off."""
    c = Canvas(16, 16)
    for i in range(7):                             # the handle
        c.rect(2 + i, 13 - i, 2, 2, hexc("ff8ac8"))
    for a in range(0, 360, 10):                    # the ring
        c.put(int(round(10 + math.cos(math.radians(a)) * 2.6)), int(round(5 + math.sin(math.radians(a)) * 2.6)), hexc("ff8ac8"))
    c.outline()
    for a in range(0, 360, 12):                    # a bubble (no outline: it's see-through)
        c.put(int(round(13 + math.cos(math.radians(a)) * 2)), int(round(12 + math.sin(math.radians(a)) * 2)), hexc("9ae8ff"))
    c.put(12, 11, hexc("ffffff"))
    return c.img


def item_tooth():
    """Sweet Tooth: a happy tooth holding a striped candy."""
    c = Canvas(16, 16)
    c.rect(3, 2, 9, 8, hexc("fbf6ff"))             # the crown
    c.rect(3, 10, 3, 4, hexc("fbf6ff"))            # roots
    c.rect(9, 10, 3, 4, hexc("fbf6ff"))
    c.rect(4, 3, 1, 3, hexc("ffffff"))
    c.put(6, 5, hexc("3a2a2a"))
    c.put(9, 5, hexc("3a2a2a"))
    c.rect(6, 7, 4, 1, hexc("d84a6a"))
    _disc(c, 13, 12, 2.4, hexc("ff4a8a"))          # the candy
    c.put(13, 12, hexc("ffffff"))
    c.put(12, 11, hexc("ffffff"))
    c.outline()
    return c.img


def item_soda():
    """Fizzy Soda: a blue can with bubbles fizzing out of the top."""
    c = Canvas(16, 16)
    c.rect(4, 5, 8, 10, hexc("4a8ae8"))
    c.rect(4, 5, 8, 1, hexc("c8c8d0"))             # the lid
    c.rect(4, 14, 8, 1, hexc("9a9aa6"))
    c.rect(5, 6, 1, 8, hexc("8ac0ff"))             # shine
    c.rect(6, 9, 5, 2, hexc("ffffff"))             # label
    c.outline()
    for x, y in ((7, 3), (9, 2), (6, 1), (10, 0), (8, 0)):
        c.put(x, y, hexc("c8f0ff"))
    return c.img


def item_goldstar():
    """Gold Star Rounds: a gold star sticker on a bullet."""
    c = Canvas(16, 16)
    c.rect(1, 10, 10, 5, hexc("d9b24c"))           # a round lying under it
    _disc(c, 11, 12.5, 2.5, hexc("e8c070"))
    c.rect(1, 10, 2, 5, hexc("a47e2a"))
    pts = []
    for k in range(10):
        r = 6 if k % 2 == 0 else 2.6
        a = math.radians(-90 + 36 * k)
        pts.append((9 + math.cos(a) * r, 6.5 + math.sin(a) * r))
    for y in range(16):
        for x in range(16):
            px, py, inside = x + 0.5, y + 0.5, False
            for i in range(10):
                (x1, y1), (x2, y2) = pts[i], pts[(i + 1) % 10]
                if (y1 > py) != (y2 > py) and px < x1 + (py - y1) * (x2 - x1) / (y2 - y1):
                    inside = not inside
            if inside:
                c.put(x, y, hexc("ffd23a"))
    c.put(8, 4, hexc("fff4c0"))
    c.put(8, 5, hexc("fff4c0"))
    c.outline()
    return c.img


def item_skates():
    """Comet Skates: a roller skate with a flaming trail."""
    c = Canvas(16, 16)
    c.rect(6, 2, 6, 7, hexc("fbf6ff"))             # the boot
    c.rect(6, 8, 9, 3, hexc("fbf6ff"))
    c.rect(7, 4, 4, 1, hexc("e84a4a"))             # laces
    c.rect(7, 6, 4, 1, hexc("e84a4a"))
    c.rect(6, 11, 9, 1, hexc("9a9aa6"))
    for x in (7, 12):
        _disc(c, x + 0.5, 13.5, 1.6, hexc("ff8a1e"))   # wheels
    c.outline()
    c.rect(0, 9, 4, 2, hexc("ff8a1e"))             # the comet trail
    c.rect(2, 7, 3, 1, hexc("ffd84a"))
    c.rect(1, 12, 3, 1, hexc("ffd84a"))
    return c.img


def item_sugar():
    """Sugar Rush: a big swirly lollipop."""
    c = Canvas(16, 16)
    c.rect(7, 9, 2, 7, hexc("fbf6ff"))             # stick
    _disc(c, 8, 6, 5.5, hexc("ff7ac8"))
    for t in range(0, 600, 8):                     # the swirl
        r = t / 600 * 5
        a = math.radians(t)
        c.put(int(8 + math.cos(a) * r), int(6 + math.sin(a) * r), hexc("ffffff"))
    c.outline()
    return c.img


def item_bobbypin():
    """Bobby Pin: granny's hairpin with a pink flower on it."""
    c = Canvas(16, 16)
    c.rect(4, 3, 1, 11, hexc("6a6a7a"))           # the straight prong
    for y in range(3, 14):                         # the wavy one
        c.put(7 + (1 if (y // 2) % 2 else 0), y, hexc("6a6a7a"))
    c.rect(4, 14, 5, 1, hexc("6a6a7a"))            # the bend
    for cx, cy in ((10, 3), (13, 3), (10, 6), (13, 6)):
        _disc(c, cx, cy, 1.6, hexc("ff8ac8"))      # the flower's petals
    _disc(c, 11.5, 4.5, 1.2, hexc("ffd23a"))
    c.outline()
    return c.img


def _key(c, x, y, col, dark):
    """A little key hanging down from (x, y): a round bow, a shaft, two teeth."""
    _disc(c, x + 0.5, y + 1.5, 1.6, col)
    c.put(x, y + 1, dark)                          # the bow's hole
    c.rect(x, y + 3, 1, 5, col)                    # the shaft
    c.rect(x + 1, y + 6, 1, 1, col)                # the teeth
    c.rect(x + 1, y + 8, 1, 1, col)
    c.put(x, y + 8, col)


def item_keyring():
    """Key Ring: a big steel ring with three keys hanging off it - brass, silver and gold."""
    c = Canvas(16, 16)
    for k in range(40):                            # the ring
        a = math.radians(k * 9)
        c.put(int(round(8 + math.cos(a) * 4.5)), int(round(5 + math.sin(a) * 3.5)), hexc("9a9aaa"))
    _key(c, 3, 7, hexc("d8a050"), hexc("8a5a20"))
    _key(c, 7, 8, hexc("d8dce6"), hexc("7a7e8a"))
    _key(c, 11, 7, hexc("ffd23a"), hexc("a47e2a"))
    c.outline()
    return c.img


def item_keychain():
    """Jingly Keychain: a gold key on a chain with a little silver bell that jingles."""
    c = Canvas(16, 16)
    for i in range(5):                             # the chain, link by link
        c.put(2 + i, 1 + i, hexc("b8bcc8") if i % 2 else hexc("8a8e9a"))
    _disc(c, 4.5, 11, 3, hexc("d8dce6"))           # the bell
    c.rect(2, 13, 6, 1, hexc("9a9eaa"))
    c.put(4, 14, hexc("5a5e6a"))                   # its clapper
    c.put(3, 9, hexc("ffffff"))
    _disc(c, 9, 6.5, 2.6, hexc("ffd23a"))          # the key's bow
    c.put(9, 6, hexc("a47e2a"))
    for i in range(6):                             # its shaft, slanting down
        c.put(10 + i // 2 + (i % 2), 8 + i, hexc("ffd23a"))
    c.rect(13, 12, 2, 1, hexc("ffd23a"))           # the teeth
    c.rect(12, 10, 2, 1, hexc("ffd23a"))
    c.outline()
    return c.img


def item_scoop():
    """Triple Scoop: three scoops of ice cream on a cone."""
    c = Canvas(16, 16)
    for i in range(6):                             # the cone
        c.rect(5 + i // 2, 10 + i, 6 - i, 1, hexc("d8a050"))
    c.put(7, 12, hexc("a87030"))
    c.put(9, 11, hexc("a87030"))
    _disc(c, 5, 8, 2.8, hexc("ffb3d1"))            # strawberry
    _disc(c, 11, 8, 2.8, hexc("8ad08a"))           # mint
    _disc(c, 8, 4, 3, hexc("fff0d0"))              # vanilla
    c.put(8, 1, hexc("e84a4a"))                    # a cherry
    c.put(7, 3, hexc("ffffff"))
    c.outline()
    return c.img


def item_bag():
    """S: a bottomless bag - a fat sack tied with gold string, bullets spilling out of the top."""
    c = Canvas(16, 16)
    _disc(c, 8, 10, 5.5, hexc("7a5ac8"))
    c.rect(4, 13, 8, 2, hexc("5a3ea0"))
    c.rect(6, 4, 4, 2, hexc("7a5ac8"))       # the neck
    c.rect(5, 5, 6, 1, hexc("e8c050"))       # the gold string
    for x, y in ((5, 2), (8, 1), (11, 2)):
        c.rect(x, y, 1, 2, hexc("ffd23a"))   # bullets peeking out
        c.put(x, y, hexc("fff0a0"))
    c.put(6, 9, hexc("b8a0f0"))              # a shine
    c.put(6, 10, hexc("b8a0f0"))
    c.outline()
    return c.img


def item_blanket():
    """S: a cozy rolled-up blanket, pink and cream stripes, a little star on it."""
    c = Canvas(16, 16)
    for i, y in enumerate(range(3, 14, 2)):
        c.rect(2, y, 12, 2, hexc("ff9fc8") if i % 2 == 0 else hexc("fff2e0"))
    c.rect(11, 3, 3, 11, hexc("e87aa8"))     # the rolled end
    c.rect(12, 5, 1, 7, hexc("ffc0da"))
    c.put(6, 7, hexc("ffd23a"))              # a star
    c.put(5, 8, hexc("ffd23a"))
    c.put(7, 8, hexc("ffd23a"))
    c.put(6, 9, hexc("ffd23a"))
    c.outline()
    return c.img


def item_monocle():
    """S: a golden monocle on its chain, the lens catching a glint."""
    c = Canvas(16, 16)
    _disc(c, 7, 7, 5.5, hexc("e8c050"))
    _disc(c, 7, 7, 4, hexc("bfe6ff"))
    c.put(5, 5, hexc("ffffff"))
    c.put(6, 4, hexc("ffffff"))
    for i in range(5):
        c.put(11 + i // 2, 11 + i, hexc("e8c050"))  # the chain
    c.outline()
    return c.img


def item_photo():
    """Family Photo: a framed picture of the four of them."""
    c = Canvas(16, 16)
    c.rect(1, 2, 14, 12, hexc("b8843a"))           # the frame
    c.rect(2, 3, 12, 10, hexc("bfe6ff"))           # the sky behind them
    c.rect(2, 10, 12, 3, hexc("6ac85a"))
    for x, hair, h in ((3, "5a3a1a", 4), (6, "3a2a2a", 3), (9, "dcdce0", 4), (12, "f5f5f5", 4)):
        c.rect(x, 12 - h, 2, h, hexc("e84a4a") if x % 2 else hexc("4a7ad8"))   # bodies
        c.rect(x, 12 - h - 2, 2, 2, hexc("ffd8b0"))                         # faces
        c.rect(x, 12 - h - 2, 2, 1, hexc(hair))                             # hair
    c.outline()
    return c.img


def item_backpack():
    """Fluffy Backpack: a pink plush bag with tufty fur edges and a heart patch on its pocket."""
    c = Canvas(16, 16)
    fur, dark, light = hexc("ff9ad0"), hexc("e070b0"), hexc("ffd6ec")
    c.rect(6, 1, 4, 1, dark)                   # carry loop
    c.put(5, 2, dark)
    c.put(10, 2, dark)
    c.rect(3, 3, 10, 11, fur)                  # the bag
    for x in range(3, 13, 2):                  # tufts sticking out of the fur
        c.put(x, 2 + (x // 2) % 2, fur)
        c.put(x + 1, 14, fur)
    for y in range(4, 14, 3):
        c.put(2, y, fur)
        c.put(13, y + 1, fur)
    c.rect(3, 6, 10, 1, dark)                  # the fluffy flap's edge
    for x in range(4, 12, 3):
        c.put(x, 4, light)                     # fluff highlights
        c.put(x + 1, 5, light)
    c.rect(4, 8, 8, 5, light)                  # front pocket
    for x, y in ((6, 9), (7, 9), (9, 9), (10, 9), (7, 11), (8, 11), (9, 11), (8, 12)):
        c.put(x, y, hexc("e84a6a"))            # the heart patch
    c.rect(6, 10, 5, 1, hexc("e84a6a"))
    c.outline()
    return c.img


def item_dreamcatcher():
    """Dream Catcher: a willow hoop with a web and a star in the middle, beads and feathers hanging off it."""
    import math
    c = Canvas(16, 16)
    hoop, web, star = hexc("b07a44"), hexc("e8e0ff"), hexc("ffd23a")
    for i in range(48):                        # the hoop
        a = i / 48 * 2 * math.pi
        c.put(round(8 + 5 * math.cos(a)), round(6 + 5 * math.sin(a)), hoop)
    for i in range(6):                         # the web's threads
        a = i / 6 * 2 * math.pi
        for k in range(1, 5):
            c.put(round(8 + k * math.cos(a)), round(6 + k * math.sin(a)), web)
    c.put(8, 6, star)
    c.put(7, 6, star)
    c.put(8, 5, star)
    for x, col in ((4, "8ad0ff"), (8, "ff9ad0"), (12, "8ad0ff")):   # strings, beads, feathers
        c.put(x, 11 + (x == 8), hexc("c8b8a8"))
        c.put(x, 12 + (x == 8), hexc(col))
        c.rect(x, 13 + (x == 8), 1, 2 - (x == 8), hexc("fbf6ff"))
    c.outline()
    return c.img


ITEM_SPRITES = [("slippers", item_slippers), ("pocket", item_pocket), ("medkit", item_medkit),
                ("field", item_field), ("clips", item_clips), ("hotsauce", item_hotsauce),
                ("clover", item_clover), ("grenade", item_grenade), ("shield", item_shield),
                ("ghost", item_ghost), ("candy", item_candy), ("heart", item_heart),
                ("fuzz", item_fuzz), ("flippers", item_flippers),
                ("socks", item_socks), ("cardboard", item_cardboard), ("map", item_map), ("robot", item_robot),
                ("sweater", item_sweater), ("globe", item_globe), ("cloud", item_cloud), ("lamb", item_lamb),
                ("boots", item_boots), ("helmet", item_helmet), ("frost", item_frost), ("ember", item_ember),
                ("moons", item_moons), ("mittens", item_mittens), ("lunch", item_lunch), ("piggy", item_piggy),
                ("telescope", item_telescope), ("rubber", item_rubber), ("plushie", item_plushie),
                ("puppy", item_puppy), ("wand", item_wand), ("tooth", item_tooth), ("soda", item_soda),
                ("goldstar", item_goldstar), ("skates", item_skates), ("sugar", item_sugar),
                ("bobbypin", item_bobbypin), ("scoop", item_scoop), ("photo", item_photo), ("bag", item_bag), ("blanket", item_blanket), ("monocle", item_monocle),
                ("backpack", item_backpack), ("dreamcatcher", item_dreamcatcher), ("guardmoon", item_guardmoon),
                ("mirrormoon", item_mirrormoon), ("bluemoon", item_bluemoon), ("teddy", item_teddy),
                ("keyring", item_keyring), ("keychain", item_keychain)]


def items_sheet():
    sheet = Image.new("RGBA", ((16 + 2) * len(ITEM_SPRITES), 16), (0, 0, 0, 0))
    for i, (name, fn) in enumerate(ITEM_SPRITES):
        sheet.paste(fn(), (i * 18, 0))
        ITEMS[name] = (i * 18, 0, 16, 16)
    return sheet


def kin_frame(shell, frame, angry=False):
    """32x40 original enemy art: broad lit planes, small eyes, boots and a firm outline.

    Four walking poses, an idle pose and a braced firing pose. 64 px/world unit,
    twice the old casing detail without changing the world's render resolution.
    """
    c = Canvas(32, 40)
    # Body bob is continuous in KinArt; only the stepping boots change by frame.
    bob = 0
    lean = 1 if frame == 5 else 0
    def box(x, y, w, h, color):
        c.rect(x + lean, y + bob, w, h, hexc(color))
    # The boots alternate their lifted heel; the lowest sole stays at the anchor.
    for x, lift in ((7, 1 if frame == 1 else 0), (20, 1 if frame == 3 else 0)):
        c.rect(x, 34 - lift, 7, 4, hexc("211d27"))
        c.rect(x + 1, 34 - lift, 5, 1, hexc("58505b"))
    if shell:
        main, lit, shadow = (("be433c", "ee7753", "7b2935") if angry else
                             ("a74342", "d5735a", "672c38"))
        box(5, 5, 22, 27, main)
        box(7, 3, 18, 4, main)
        box(6, 6, 3, 20, lit)
        box(23, 6, 4, 22, shadow)
        box(7, 4, 18, 3, "482a32")
        box(9, 4, 14, 1, "e5aa79")
        box(10, 9, 1, 14, "bf5a4f")
        box(20, 9, 1, 14, shadow)
        box(4, 28, 24, 6, "c59449")
        box(5, 28, 22, 2, "f2d381")
        box(5, 32, 22, 2, "80573c")
        eye_y = 16
    else:
        box(6, 13, 20, 20, "bd984d")
        # Rounded copper nose, shaded in large connected clusters.
        box(6, 10, 20, 6, "ae6848")
        box(7, 7, 18, 5, "bf7a50")
        box(9, 4, 14, 4, "cf8e5c")
        box(12, 3, 8, 3, "d99d68")
        box(9, 8, 3, 5, "edbc81")
        box(12, 5, 7, 2, "edbc81")
        box(23, 10, 3, 5, "80503e")
        box(7, 16, 3, 13, "efd28b")
        box(23, 16, 3, 14, "89683d")
        box(6, 15, 20, 2, "f0cc7e")
        box(5, 30, 22, 4, "8c683f")
        box(6, 30, 20, 1, "e5bf6d")
        eye_y = 21
    # Smaller inset eyes and a mouth replace the old protruding googly circles.
    for x in (12, 20):
        box(x - 1, eye_y - 1, 5, 6, "6d4a3b" if not shell else "522d36")
        box(x, eye_y, 3, 4, "fff2d6")
        box(x + 2, eye_y + 1, 2, 3, "24212b")
        if angry or frame == 5:
            box(x - 1, eye_y - 1, 5, 2, "24212b")
    box(16, eye_y + 7, 4, 1, "513c36")
    if angry:
        box(15, eye_y + 6, 6, 2, "321f2b")
    c.outline(hexc("25212b"))
    return c.img


def kin_sheet(shell=False):
    sheet = Image.new("RGBA", (32 * 6, 40 * 3), (0, 0, 0, 0))
    for row in range(3):
        for frame in range(6):
            art = kin_frame(shell, frame, angry=row == 1)
            if row == 2:  # a real white silhouette for the existing damage flash
                art = Image.new("RGBA", art.size, (255, 255, 255, 0))
                art.putalpha(kin_frame(shell, frame).getchannel("A"))
            sheet.paste(art, (frame * 32, row * 40))
    return sheet


KIN_VARIANTS = ("VeteranKin", "BandanaKin", "PirateKin", "BlueShotgunKin", "TankerKin", "SawnOffKin",
                "Bulletmancer", "Spiralmancer", "Bloomancer")


def variant_kin_frame(kind, frame):
    ink = hexc("25212b")
    if "mancer" in kind:
        c = Canvas(32,40)
        palettes = {"Bulletmancer": ("5864a5","374474","8b6bad"),
                    "Spiralmancer": ("b7764a","79513f","b95852"),
                    "Bloomancer": ("668658","405b43","ca8ca6")}
        base,dark,hat = map(hexc,palettes[kind])
        edge=hexc("343040")
        def ellipse(cx,cy,rx,ry,col):
            for y in range(max(0,int(cy-ry)),min(40,int(cy+ry)+1)):
                for x in range(max(0,int(cx-rx)),min(32,int(cx+rx)+1)):
                    if ((x-cx)/rx)**2+((y-cy)/ry)**2<=1:c.put(x,y,col)
        # Draped shoulders and a curved hem rather than a square robe and black face panel.
        ellipse(16,25,10,10,base)
        c.rect(7,25,19,11,base); ellipse(16,34,11,3,base)
        c.rect(8,29,2,6,shade(base,1.18)); c.rect(23,29,2,6,dark)
        c.rect(10,36,13,1,dark)
        ellipse(16,18,7,8,shade(dark,1.12))
        for x in (13,20):
            c.rect(x-1,17,3,4,hexc("fff3dd"))
            c.rect(x,18,2,3,edge)
        # A floppy tapered hat with a broad curved brim.
        if kind=="Bloomancer":
            ellipse(16,9,10,7,hat)
            for x,y in ((9,7),(15,4),(23,7)):
                disc(c,x,y,3,hexc("e4afbf")); disc(c,x,y,1,hexc("f3d88e"))
            c.rect(6,11,4,2,hexc("91ac78")); c.rect(24,10,4,2,hexc("91ac78"))
        else:
            for y in range(2,12):
                left=17-(y-2)
                right=23+(y-2)//3
                c.rect(left,y,right-left,1,hat)
                c.put(left,y,shade(hat,1.18));c.put(right-1,y,shade(hat,0.8))
            ellipse(16,12,13,3,hat)
            c.rect(7,11,14,1,shade(hat,1.18))
            if kind=="Spiralmancer":
                for x,y in ((22,3),(25,4),(26,6),(27,8)): c.put(x,y,hexc("e4b884"))
            else:
                c.rect(20,6,1,3,hexc("ecdbac"));c.rect(19,7,3,1,hexc("ecdbac"))
        # One clasp and a folded lapel carry the costume detail, leaving the face uncluttered.
        disc(c,16,26,2,hexc("ba985a"));c.put(16,25,hexc("f5df9a"))
        for y in range(28,35): c.rect(17+(y-28)//3,y,2,1,dark)
        c.rect(11,31,2,3,shade(base,1.12))
        for x in (9,20): c.rect(x,37-(1 if frame in (1,3) else 0),4,2,edge)
        c.outline(edge)
        return c.img
    shell=kind in ("BlueShotgunKin","SawnOffKin")
    c=Canvas(32,40); c.img=kin_frame(shell,frame); c.px=c.img.load()
    recolor={}
    if kind=="VeteranKin":
        recolor={"bd984d":"89949e","efd28b":"bac4ca","89683d":"52616c","8c683f":"4e5963","e5bf6d":"a4b1b8"}
    if kind=="BlueShotgunKin":
        recolor={"a74342":"3e76b1","d5735a":"73add0","672c38":"304e79","bf5a4f":"578fc0","482a32":"273d5c","e5aa79":"a4cbe0"}
    if kind=="SawnOffKin":
        recolor={"a74342":"3b414a","d5735a":"68717a","672c38":"272b35","bf5a4f":"4c5660","482a32":"222630","e5aa79":"b3b6b0"}
    table={hexc(a):hexc(b) for a,b in recolor.items()}
    for y in range(40):
        for x in range(32):
            if c.px[x,y] in table:c.put(x,y,table[c.px[x,y]])
    if kind=="VeteranKin":
        c.rect(6,2,18,7,hexc("56633f")); c.rect(4,5,22,5,hexc("56633f")); c.rect(4,9,23,4,hexc("3b4834"))
        c.rect(7,3,14,3,hexc("75815a")); c.rect(21,8,3,3,hexc("e8ca76"))
        c.rect(9,27,3,4,hexc("4d6f97")); c.rect(9,30,4,3,hexc("dcbb64"))
        c.rect(17,29,5,1,hexc("c1c8ca"))
    elif kind=="BandanaKin":
        c.rect(4,13,24,4,hexc("b84445")); c.rect(5,13,21,1,hexc("e77c65"))
        for x in (9,19):c.rect(x,14,2,1,hexc("f6d2b2"))
        c.rect(1,17,5,3,hexc("80323d")); c.rect(2,21+(frame%2),4,2,hexc("b84445"))
        c.rect(9,28,3,2,hexc("e5c785"))
    elif kind=="PirateKin":
        for y in (28,31):c.rect(7,y,18,2,hexc("41648c"))
        c.rect(3,8,26,5,ink); disc(c,16,7,8,ink)
        c.rect(3,7,4,4,ink); c.rect(26,7,4,4,ink)
        c.rect(5,11,22,1,hexc("d2af61")); c.rect(15,5,4,4,hexc("f2e3bd"))
        c.put(15,6,ink); c.put(18,6,ink); c.rect(16,9,2,1,hexc("f2e3bd"))
        c.rect(19,20,7,6,ink)
        for i in range(16):c.put(8+i,17+i//4,ink)
    elif kind=="TankerKin":
        disc(c,16,10,9,hexc("526448")); c.rect(3,12,26,4,hexc("354438"))
        c.rect(7,4,4,7,hexc("829172")); c.rect(23,6,2,6,hexc("364536"))
        c.rect(5,16,2,11,hexc("655b45")); c.rect(25,16,2,11,hexc("655b45"))
        for x in (9,16):
            c.rect(x,28,5,5,hexc("736447")); c.rect(x+1,28,3,1,hexc("d2b36e"))
    elif kind=="BlueShotgunKin":
        for x in (8,22):c.rect(x,8,2,5,hexc("293d5c"))
        c.rect(5,29,22,2,hexc("7a9ba9")); c.rect(6,29,20,1,hexc("c0d2d1"))
    elif kind=="SawnOffKin":
        c.rect(5,12,22,3,hexc("e0aa4d")); c.rect(6,12,20,1,hexc("f3d189"))
        for x in (11,19):c.rect(x,15,6,2,ink)
    c.outline(ink)
    return c.img


def variant_kin_sheet(kind):
    sheet=Image.new("RGBA",(192,120),(0,0,0,0))
    for row in range(3):
        for frame in range(6):
            art=variant_kin_frame(kind,frame)
            if row==2:
                white=Image.new("RGBA",art.size,(255,255,255,0)); white.putalpha(art.getchannel("A")); art=white
            sheet.paste(art,(frame*32,row*40))
    return sheet


def kin_weapons():
    c = Canvas(64, 16)
    for shell, offset in ((False, 0), (True, 32)):
        length = 21 if shell else 17
        c.rect(offset + 5, 5, length, 5, hexc("65636c"))
        c.rect(offset + 6, 5, length - 2, 1, hexc("aaa4a0"))
        c.rect(offset + 6, 9, length - 1, 2, hexc("35313c"))
        c.rect(offset + 3, 8, 7, 4, hexc("8b5943"))
        c.rect(offset + 4, 8, 5, 1, hexc("c08d60"))
        c.rect(offset + 7, 10, 3, 4, hexc("4e3a38"))
        if shell:
            c.rect(offset + 13, 9, 8, 3, hexc("9b704d"))
            c.rect(offset + 13, 9, 8, 1, hexc("d1a06b"))
            c.rect(offset + 24, 4, 2, 7, hexc("312b35"))
        else:
            c.rect(offset + 18, 4, 2, 2, hexc("312b35"))
        c.rect(offset + 7, 10, 4, 3, hexc("d6ad70"))  # the gripping hand
    c.outline(hexc("25212b"))
    return c.img


# --------------------------------------------------------------------- tiles
# Each theme: floor (kind + colours), wall face (kind + colours) with a window and a decoration
# variant, and the "cap" that fills the wall tops and the room border (thatch, hedge, roof...).
THEMES = {
    # --- stage 1: the village
    "cottage": dict(floor=("planks", "a0703f", "8a5d32", "b8854f"),
                    face=("timber", "f0e2c0", "6e4a2e", "3e2a1c"),
                    cap=("thatch", "c49a45", "8e6a2a", "e0c070")),
    "yard":    dict(floor=("grass", "668653", "587449", "7a9a60"),
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
    "park":     dict(floor=("grass", "6a8e58", "59784c", "80a468"),
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
    # --- the true ending: the Moon, up the family's rocket (no shop up here - Baa-llo can't fly)
    "landing":     dict(floor=("moondust", "c4c4cc", "9a9aa4", "e2e2ea"),
                        face=("moonrock", "8e8e9a", "6a6a76", "e05a8a"),
                        cap=("open", "000000", "000000", "000000")),
    "craters":     dict(floor=("moondust", "bcbcc6", "8e8e9a", "dcdce6"),
                        face=("moonrock", "868692", "62626e", "e05a8a"),
                        cap=("open", "000000", "000000", "000000")),
    "cheesemine":  dict(floor=("cheese", "f2c84a", "c8962a", "ffe08a"),
                        face=("cheesewall", "e8b83a", "b8862a", "7a5a20"),
                        cap=("open", "000000", "000000", "000000")),
    "observatory": dict(floor=("raised", "9aa0ac", "747a86", "bcc2ce"),
                        face=("domewall", "e8eaf0", "a8acb8", "ff4a3a"),
                        cap=("open", "000000", "000000", "000000")),
    "cheesecave":  dict(floor=("cheese", "f6d060", "cc9a30", "fff0a0"),
                        face=("cheesewall", "f0c448", "c08c2c", "7a5a20"),
                        cap=("open", "000000", "000000", "000000")),
    "castle":      dict(floor=("marble", "d8dcf0", "b4bad6", "ffffff"),
                        face=("columns", "eceff8", "b0b6d0", "8a9ad8"),
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
    # --- the secret Slums, down from the city: grime, graffiti, boarded windows, chain-link (at night)
    "backalley":  dict(floor=("cracked", "3a3634", "2a2624", "5a6a3a"),       # start: a dead-end back alley
                       face=("graffiti", "5a3e36", "3e2a24", "6a6a74", "5affc0"),
                       cap=("slate", "1e1c22", "16141a", "2e2a32")),
    "slumstreet": dict(floor=("cracked", "34343a", "26262c", "5a6a3a"),
                       face=("shutters", "6a5a52", "4a3e38", "2a2a30", "ff5fa2"),
                       cap=("nightsky", "100e18", "0a0a12", "c8c0d8")),
    "underpass":  dict(floor=("concrete", "4a4844", "3a3834", "5e5a54"),
                       face=("graffiti", "5e5a54", "4a4844", "6a6a74", "ffd166"),
                       cap=("slate", "1e1c22", "16141a", "2e2a32")),
    "junkyard":   dict(floor=("dirt", "4e4436", "3a3228", "5a6a3a"),
                       face=("chainlink", "8a8a96", "1a1a22", "5a5a66"),
                       cap=("nightsky", "100e18", "0a0a12", "c8c0d8")),
    "hideout":    dict(floor=("planks", "5a4630", "463624", "6a5438"),          # treasure: a gang's stash
                       face=("graffiti", "4a3a40", "34282e", "6a6a74", "e8c040"),
                       cap=("slate", "1e1c22", "16141a", "2e2a32")),
    "emptylot":   dict(floor=("cracked", "3a3a3e", "2a2a2e", "5a6a3a"),        # the boss's turf
                       face=("chainlink", "8a8a96", "141418", "5a5a66"),
                       cap=("nightsky", "100e18", "0a0a12", "c8c0d8")),
    # --- the Undercity, under the Slums: sewers and streets buried under the city, glowing green water
    "sewergate":    dict(floor=("sewer", "3a4442", "26302e", "6ad87a"),      # start: where the hatch comes down
                         face=("sewerwall", "4a3e3a", "342a28", "5a5a66", "8ae83a"),
                         cap=("rock", "2a2826", "1a1816", "6ad87a")),
    "sewer":        dict(floor=("sewer", "343e3c", "222a28", "6ad87a"),
                         face=("sewerwall", "4a3a34", "30241f", "5a5a66", "8ae83a"),
                         cap=("rock", "26241f", "181612", "6ad87a")),
    "buriedstreet": dict(floor=("cobble", "4a4640", "38342e", "5a564e"),
                         face=("buriedfront", "5a4a42", "3a2e28", "4a4a54", "8ae83a"),
                         cap=("rock", "2a2826", "1a1816", "6ad87a")),
    "cistern":      dict(floor=("seastone", "3a4a48", "2a3836", "5a6a68"),
                         face=("sewerwall", "3e4a48", "2a3432", "5a5a66", "6ae0c0"),
                         cap=("rock", "222826", "141816", "6ae0c0")),
    "hoard":        dict(floor=("planks", "5a4a30", "463824", "6a5a38"),        # treasure: the King's stash
                         face=("buriedfront", "5a4a3a", "3a2e24", "c9a040", "e8c040"),
                         cap=("rock", "2a2826", "1a1816", "e8c040")),
    "throneroom":   dict(floor=("checker", "3a3a2a", "2a2a1e", "4a4a36"),       # the False King's court
                         face=("sewerwall", "4a3e2e", "30281c", "c9a040", "c8e85a"),
                         cap=("rock", "2a2826", "1a1816", "c8e85a")),
    "store_undercity": dict(floor=("checker", "2e3432", "222826", "3e4442"),
                            face=("store", "3e4440", "262c2a", "161a18", "8ae83a"),
                            cap=("rock", "2a2826", "1a1816", "6ad87a")),
    # --- under the bed: dusty floorboards and the underside of the frame
    "bed_dust": dict(floor=("bedboards", "494151", "433b4b", "514958"),
                       face=("bedframe", "49353e", "302632", "967659"),
                       cap=("bedframe", "302632", "433747", "967659")),
    "bed_slats": dict(floor=("bedboards", "4d4353", "463c4c", "554b5b"),
                       face=("bedframe", "554047", "382835", "987053"),
                       cap=("bedframe", "382835", "48373f", "987053")),
    "bed_quilt": dict(floor=("patchwork", "484151", "423b4b", "504959"),
                       face=("bedframe", "504055", "392d41", "9c807c"),
                       cap=("bedframe", "362e40", "524355", "897172")),
    "bed_sock": dict(floor=("bedboards", "474055", "40394d", "4f485d"),
                       face=("bedframe", "473354", "2e2643", "80618e"),
                       cap=("bedframe", "2e2643", "37334d", "80618e")),
    "bed_yarn": dict(floor=("bedboards", "4e4052", "46394b", "56485a"),
                       face=("bedframe", "633c50", "392335", "b2738b"),
                       cap=("bedframe", "392335", "462f41", "b2738b")),
    "bed_tin": dict(floor=("bedboards", "49444f", "423d48", "514c57"),
                       face=("bedframe", "4f473e", "332c2a", "967c5b"),
                       cap=("bedframe", "332c2a", "393a3c", "967c5b")),
    "bed_court": dict(floor=("bedboards", "493e53", "42374c", "51465b"),
                       face=("bedframe", "51405b", "31253e", "ae8752"),
                       cap=("bedframe", "31253e", "3d2d46", "ae8752")),
    # --- the dream bosses' arena: a kid's bedroom grown huge, at night (the dream run's last floor)
    "dreamland":    dict(floor=("dreamy", "5a4a8a", "4a3c74", "ffe48a"),
                         face=("dreamwall", "2e2a5a", "221e46", "ffe48a", "8ac8ff"),
                         cap=("nightsky", "161232", "0e0c24", "f0e4ff")),
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
    "store_slums":   dict(floor=("checker", "3a3434", "2a2626", "4a4444"),
                          face=("store", "4a4040", "2e2828", "1e1818", "ffd166"),
                          cap=("slate", "1e1c22", "16141a", "2e2a32")),
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
        # Long boards and clustered grain keep the floor quieter than the actors.
        seam = shade(base, 0.84)
        grain = shade(base, 1.06)
        for y in range(0, T, 4):
            c.rect(0, y + 3, T, 1, seam)
            c.rect(0, y, T, 1, grain)
            # Only one board joint per tile, rather than a short brick in every row.
            if y == (seed % 4) * 4:
                off = (seed * 5 + 3) % 14 + 1
                c.rect(off, y, 1, 3, seam)
                c.put(off + 1, y + 1, dark)
        c.rect(3 + seed, 5, 4, 1, grain)
        c.rect(9 - seed, 13, 3, 1, seam)
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
        # Sparse tufts instead of bright single-pixel confetti across the whole arena.
        for _ in range(9):
            x, y = rnd.randint(1, 14), rnd.randint(2, 14)
            c.put(x, y, dark)
            c.put(x - 1, y - 1, light)
            c.put(x, y - 2, light)
        for _ in range({0: 0, 1: 0, 2: 1, 3: 2}[seed]):  # flowers on the rarer tiles
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
    elif kind == "sewer":  # the Undercity: wet stone slabs, a glint of green water in the cracks
        c.rect(0, 0, T, 1, dark)
        c.rect(0, 0, 1, T, dark)
        c.rect(0, 8, T, 1, dark)
        c.rect(8 if seed % 2 else 5, 0, 1, 8, dark)
        c.rect(11 if seed % 2 else 3, 8, 1, 8, dark)
        for _ in range(5):
            c.put(rnd.randrange(T), rnd.randrange(T), shade(base, 1.15))
        if seed >= 2:  # green water standing in the joints
            c.rect(1, 8, rnd.randint(3, 7), 1, light)
        if seed == 3:  # a drain grate
            c.rect(4, 3, 8, 3, hexc("101418"))
            for gx in range(5, 12, 2):
                c.rect(gx, 3, 1, 3, shade(base, 1.3))
    elif kind == "cracked":  # the Slums: broken tarmac, litter, weeds in the cracks
        for _ in range(12):
            c.put(rnd.randrange(T), rnd.randrange(T), dark if rnd.random() < 0.6 else shade(base, 1.15))
        x, y = rnd.randint(1, 6), rnd.randint(2, 12)
        for i in range(rnd.randint(5, 9)):  # a crack wandering across
            c.put(x + i, y, dark)
            y += rnd.choice((-1, 0, 0, 1))
        if seed == 1:  # a scrap of paper
            c.rect(9, 4, 3, 2, hexc("d8d0b8"))
            c.put(9, 5, hexc("a8a088"))
        if seed == 2:  # weeds poking out
            for wx in (4, 6, 5):
                c.rect(wx, 9, 1, 2, light)
            c.put(5, 8, light)
        if seed == 3:  # a storm drain grate
            c.rect(3, 5, 10, 6, hexc("1a1a1e"))
            for gx in range(4, 13, 2):
                c.rect(gx, 5, 1, 6, shade(base, 1.3))
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
    elif kind == "moondust":  # the Moon: grey dust, now and then a soft crater (shadow top-left, lit rim bottom-right)
        for _ in range(3):
            c.put(rnd.randrange(T), rnd.randrange(T), shade(base, 0.94))
        if seed in (1, 3):
            cx, cy, r = rnd.randrange(5, 11), rnd.randrange(5, 11), 3 if seed == 1 else 4
            bowl = tuple((base[i] * 3 + dark[i]) // 4 for i in range(3)) + (255,)
            for y in range(cy - r - 1, cy + r + 2):
                for x in range(cx - r - 1, cx + r + 2):
                    d2 = (x - cx) ** 2 + (y - cy) ** 2
                    if d2 <= r * r:
                        inner = (x - cx) + (y - cy) < -r * 0.6   # the shadowed side of the bowl
                        c.put(x % T, y % T, shade(bowl, 0.92) if inner else bowl)
                    elif d2 <= (r + 1) ** 2 and (x - cx) + (y - cy) > 0:
                        c.put(x % T, y % T, light)
    elif kind == "dreamy":  # the dream arena: a soft lilac carpet, now and then a little star in the weave
        for y in range(0, T, 2):
            for x in range((y // 2) % 2, T, 2):
                if rnd.random() < 0.12:
                    c.put(x, y, shade(base, 1.12))
        c.rect(0, 0, T, 1, dark)
        c.rect(0, 0, 1, T, dark)
        if seed in (1, 3):
            sx, sy = rnd.randrange(4, 12), rnd.randrange(4, 12)
            c.put(sx, sy, light)
            c.put(sx - 1, sy, shade(light, 0.75))
            c.put(sx + 1, sy, shade(light, 0.75))
            c.put(sx, sy - 1, shade(light, 0.75))
            c.put(sx, sy + 1, shade(light, 0.75))
    elif kind == "cheese":  # the moon-cheese mine: yellow cheese with holes in it
        for _ in range({0: 0, 1: 1, 2: 0, 3: 2}[seed]):
            cx, cy, r = rnd.randrange(T), rnd.randrange(T), rnd.choice((2, 2.5))
            for y in range(int(cy - r) - 1, int(cy + r) + 2):
                for x in range(int(cx - r) - 1, int(cx + r) + 2):
                    d2 = (x - cx) ** 2 + (y - cy) ** 2
                    if d2 <= r * r:
                        c.put(x % T, y % T, dark if y > cy - r / 2 else shade(dark, 0.8))
                    elif d2 <= (r + 1) ** 2 and y > cy:
                        c.put(x % T, y % T, light)
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
        # Broad, shallow joints instead of a dense field of bright little blocks.
        # Keep texture contrast below the actors and bullets, especially while panning.
        joint = tuple(round(base[i] * 0.65 + dark[i] * 0.35) for i in range(3)) + (255,)
        edge = tuple(round(base[i] * 0.7 + light[i] * 0.3) for i in range(3)) + (255,)
        for cy in (0, 8):
            c.rect(0, cy, T, 1, joint)
            offset = (4 if cy else 0) + (seed % 2) * 2
            for cx in range(offset - 8, T, 8):
                c.rect(cx, cy + 1, 1, 7, joint)
                c.rect(cx + 2, cy + 1, 5, 1, edge)
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
    if kind == "bedframe":
        c.rect(0, 0, T, T, b)
        c.rect(0, 0, T, 5, a)
        c.rect(0, 4, T, 1, d)
        c.rect(0, 13, T, 3, a)
        if variant == 1:
            c.rect(2, 6, 12, 6, shade(d, 0.65))  # light between the slats
            c.rect(3, 7, 10, 2, d)
        elif variant == 2:
            c.rect(5, 4, 6, 10, a)
            c.rect(5, 5, 1, 8, d)
    elif kind == "timber":  # cottage: plaster between dark timber beams
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
    elif kind == "sewerwall":  # the Undercity: old brick under the city, pipes spilling glowing green
        c.rect(0, 0, T, T, a)
        for y in range(0, 14, 3):
            c.rect(0, y, T, 1, b)
            sh = 0 if (y // 3) % 2 == 0 else 3
            for x in range(sh, T, 6):
                c.rect(x, y, 1, 3, b)
        c.rect(0, 13, T, 1, hexc(cols[3]) if len(cols) > 3 else hexc("8ae83a"))  # green slime at the foot
        c.rect(0, 14, T, 2, shade(b, 0.6))
        green = hexc(cols[3]) if len(cols) > 3 else hexc("8ae83a")
        if variant == 1:  # a pipe sticking out, dribbling green
            c.rect(4, 4, 8, 4, d)
            c.rect(4, 4, 8, 1, shade(d, 1.3))
            c.rect(6, 5, 4, 2, hexc("101418"))
            c.rect(7, 7, 2, 6, green)
            c.put(8, 12, shade(green, 1.3))
        elif variant == 2:  # a barred culvert, green glow behind
            c.rect(3, 3, 10, 9, hexc("101418"))
            c.rect(4, 7, 8, 5, shade(green, 0.5))
            for x in range(4, 13, 2):
                c.rect(x, 3, 1, 9, d)
    elif kind == "buriedfront":  # the Undercity: an old shopfront buried under the city, its sign dead
        c.rect(0, 0, T, T, a)
        c.rect(0, 0, T, 3, b)  # the old sign board
        c.rect(2, 1, 12, 1, shade(b, 1.4))
        c.rect(0, 14, T, 2, shade(b, 0.6))
        c.rect(2, 4, 12, 9, hexc("101418"))  # the shop window, dark
        c.rect(2, 4, 12, 1, d)
        c.rect(7, 4, 1, 9, d)
        if variant == 1:  # a cracked pane with a green glow inside
            green = hexc(cols[3]) if len(cols) > 3 else hexc("8ae83a")
            c.rect(9, 8, 4, 4, shade(green, 0.6))
            for i in range(4):
                c.put(3 + i, 5 + i * 2, hexc("c8c8d0"))
        elif variant == 2:  # boarded over
            for py in (5, 8, 11):
                c.rect(1, py, 14, 2, WOOD_SH)
                c.rect(1, py, 14, 1, WOOD)
    elif kind == "graffiti":  # the Slums: grimy dark bricks, spray-paint tags, a boarded-up window
        c.rect(0, 0, T, T, a)
        for y in range(0, 14, 4):
            c.rect(0, y, T, 1, b)
            sh = 0 if (y // 4) % 2 == 0 else 4
            for x in range(sh, T, 8):
                c.rect(x, y, 1, 4, b)
        c.rect(0, 14, T, 2, shade(b, 0.6))
        c.rect(0, 11, T, 3, shade(a, 0.85))  # grime splashed up from the street
        if variant == 1:  # a spray-painted tag ("FLF"), outlined, with drips
            tag = hexc(cols[3]) if len(cols) > 3 else hexc("ff5fa2")
            letters = ["XXX.X...XXX",
                       "X...X...X..",
                       "XX..X...XX.",
                       "X...X...X..",
                       "X...XXX.X.."]
            pts = [(2 + x, 3 + y) for y, row in enumerate(letters) for x, ch in enumerate(row) if ch == "X"]
            for x, y in pts:  # a dark outline round the letters first
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    c.put(x + dx, y + dy, hexc("1a1418"))
            for x, y in pts:
                c.put(x, y, tag)
            c.put(3, 3, shade(tag, 1.4))
            c.put(7, 3, shade(tag, 1.4))
            c.put(2, 8, tag)  # drips
            c.put(2, 9, tag)
            c.put(8, 9, tag)
        elif variant == 2:  # a window boarded up with planks
            c.rect(3, 2, 10, 9, hexc("1a1418"))
            for py, tilt in ((3, 0), (6, 1), (8, 0)):
                c.rect(2, py, 12, 2, WOOD_SH)
                c.rect(2, py, 12, 1, WOOD)
                c.put(3, py + tilt, IRON)
                c.put(12, py + tilt, IRON)
    elif kind == "shutters":  # the Slums: rusty roll-down shop shutters, sprayed over
        c.rect(0, 0, T, T, a)
        c.rect(0, 0, T, 2, b)
        for y in range(3, 14, 2):
            c.rect(1, y, 14, 1, shade(a, 0.75))
        c.rect(0, 2, 1, 12, b)
        c.rect(15, 2, 1, 12, b)
        c.rect(0, 14, T, 2, shade(b, 0.6))
        for _ in range(4):  # rust spots
            c.put(random.randrange(1, 15), random.randrange(3, 14), hexc("8a4a2a"))
        if variant == 1:  # a scrawled tag
            tag = hexc(cols[3]) if len(cols) > 3 else hexc("5affc0")
            for i in range(10):
                c.put(3 + i, 7 + (i % 3 == 0) - (i % 4 == 1), tag)
            c.rect(4, 5, 1, 4, tag)
            c.rect(10, 5, 1, 4, tag)
        elif variant == 2:  # a padlock at the bottom
            c.rect(7, 11, 3, 3, hexc("c9a040"))
            c.rect(8, 10, 1, 1, hexc("c9a040"))
    elif kind == "chainlink":  # the Slums: a chain-link fence, darkness beyond
        c.rect(0, 0, T, T, b)
        for i in range(-T, T, 3):  # the diamond mesh
            for k in range(T):
                c.put(i + k, k, a)
                c.put(i + T - k, k, a)
        c.rect(0, 0, T, 1, d)
        c.rect(0, 13, T, 1, d)
        c.rect(0, 14, T, 2, shade(b, 0.6))
        c.rect(0, 0, 1, 14, d)
        if variant == 1:  # KEEP OUT
            c.rect(3, 4, 10, 6, hexc("e8c030"))
            c.rect(4, 6, 8, 1, hexc("2a2a30"))
            c.rect(4, 8, 6, 1, hexc("2a2a30"))
        elif variant == 2:  # torn open at the bottom
            c.rect(5, 8, 6, 5, b)
            c.put(5, 7, a)
            c.put(10, 7, a)
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
    elif kind == "moonrock":  # the Moon's walls: a jagged grey ridge, the starry sky above it
        rnd = random.Random(variant * 17 + 5)
        for x in range(T):                     # the ridge: higher here and there
            top = 5 + int(2.5 * math.sin(x * 0.7 + variant)) + rnd.randint(0, 1)
            c.rect(x, top, 1, 14 - top, a)
            c.put(x, top, shade(a, 1.25))
        for _ in range(3):                     # craters in the rock
            x, y = rnd.randrange(2, 14), rnd.randrange(8, 12)
            c.rect(x, y, 2, 1, b)
            c.put(x, y - 1, shade(a, 1.2))
        if variant == 1:                       # the family's flag, planted on top (a little heart on it)
            c.rect(5, 0, 1, 7, hexc("c8c8d0"))
            c.rect(6, 0, 6, 4, hexc(cols[2]))
            c.put(8, 1, hexc("ffffff"))
            c.put(9, 1, hexc("ffffff"))
            c.put(8, 2, hexc("ffffff"))
            c.put(9, 2, hexc("ffffff"))
        elif variant == 2:                     # a glowing crystal poking out
            for x, y, h in ((6, 5, 5), (8, 3, 7), (10, 6, 4)):
                c.rect(x, y, 2, h, hexc("8ae0ff"))
                c.put(x, y, hexc("e0faff"))
        c.rect(0, 13, T, 1, b)
        c.rect(0, 14, T, 2, shade(b, 0.75))
    elif kind == "cheesewall":  # the mine: a wall of cheese, propped up by beams
        c.rect(0, 0, T, T, a)
        rnd = random.Random(variant * 11 + 3)
        for _ in range(4):
            x, y, r = rnd.randrange(T), rnd.randrange(1, 12), rnd.choice((1.5, 2))
            disc(c, x, y, r, b)
        c.rect(0, 0, 2, 14, hexc("8a5a30"))     # pit props
        c.rect(14, 0, 2, 14, hexc("8a5a30"))
        c.rect(0, 0, T, 2, hexc("6a4422"))
        if variant == 1:                       # a lantern on a hook
            c.rect(7, 2, 1, 2, hexc("4a4a52"))
            c.rect(6, 4, 3, 4, hexc("ffd166"))
            c.put(7, 5, hexc("fff6c8"))
        elif variant == 2:                     # a pickaxe leaning on the wall
            for i in range(8):
                c.put(5 + i // 2, 12 - i, hexc("8a5a30"))
            c.rect(5, 4, 6, 1, hexc("9a9aa6"))
        c.rect(0, 13, T, 1, d)
        c.rect(0, 14, T, 2, shade(d, 0.8))
    elif kind == "domewall":  # the old observatory: white panels, bolts, a porthole full of stars
        c.rect(0, 0, T, T, a)
        c.rect(0, 0, T, 1, b)
        c.rect(0, 7, T, 1, b)
        c.rect(0, 0, 1, 13, b)
        for x in (3, 12):
            c.put(x, 3, b)
            c.put(x, 10, b)
        if variant == 1:
            disc(c, 8, 6, 4, hexc("4a4a56"))
            disc(c, 8, 6, 3, hexc("0a0a1e"))
            for x, y in ((7, 5), (9, 7), (8, 4)):
                c.put(x, y, hexc("ffffff"))
        elif variant == 2:                     # a control panel, one light blinking red
            c.rect(4, 3, 8, 6, hexc("3a3e48"))
            for x in (5, 7, 9):
                c.put(x, 5, hexc("6ae07a"))
            c.put(10, 7, hexc(cols[2]))
        c.rect(0, 13, T, 1, b)
        c.rect(0, 14, T, 2, shade(b, 0.75))
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
    elif kind == "dreamwall":  # the dream arena: night-blue wallpaper, moons and stars, a skirting board
        c.rect(0, 0, T, T, a)
        for x, y in ((3, 3), (11, 7), (6, 10)):
            c.put(x, y, d)
        c.rect(0, 13, T, 3, b)
        c.rect(0, 13, T, 1, shade(a, 1.3))
        if variant == 1:  # a window: the moon outside
            c.rect(3, 1, 10, 10, b)
            c.rect(4, 2, 8, 8, hexc("101030"))
            disc(c, 9, 5, 2.2, d)
            disc(c, 10, 4.5, 1.8, hexc("101030"))
            c.rect(7, 2, 1, 8, b)
            c.rect(4, 6, 8, 1, b)
        elif variant == 2:  # a child's drawing pinned up: the family under a sun
            c.rect(3, 2, 10, 8, hexc("f5f0e6"))
            disc(c, 11, 4, 1.4, hexc("ffd166"))
            for i, col in enumerate(("2f9e5b", "e05a8a", "9a7ac0")):
                c.rect(5 + i * 2, 6, 1, 3, hexc(col))
            c.put(8, 1, hexc(cols[3]))
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
    if kind == "bedframe":
        c.rect(0, 0, T, T, a)
        c.rect(0, 2, T, 2, b)
        c.rect(0, 11, T, 2, b)
        c.rect(0, 13, T, 1, shade(l, 0.6))
        return c.img
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
    elif kind == "rock":  # the Undercity: the rough rock ceiling of the world under the city, a drip of green
        for _ in range(7):
            x, y = rnd.randrange(-2, T), rnd.randrange(-2, T)
            c.rect(x, y, rnd.randint(3, 6), rnd.randint(2, 4), shade(a, 1.2))
            c.put(x + 1, y + 1, b)
        c.put(rnd.randrange(T), rnd.randrange(T), l)
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


# First-floor art pilot: the floor supports the action rather than competing with it.
# Only the floor tiles change; bright doors, walls, pickups and hazards keep their colours.
VILLAGE_FLOORS = {
    "cottage": "6b5140", "barn": "66503c", "orchard": "645941",
    "yard": "536546", "square": "6c675b", "chapel": "787267",
}


def village_floor(name, kind, variant):
    base = hexc(VILLAGE_FLOORS[name])
    c = Canvas(T, T)
    c.rect(0, 0, T, T, base)
    seam, soft, glint = shade(base, 0.91), shade(base, 0.97), shade(base, 1.05)
    if kind in ("planks", "barnfloor"):
        # One wide board, not four narrow high-contrast stripes in every tile.
        c.rect(0, 15, T, 1, seam)
        if variant in (1, 3):
            x = 5 if variant == 1 else 11
            c.rect(x, 0, 1, 15, seam)
            c.rect(x + 1, 1, 1, 13, glint)
        if variant == 2:
            c.rect(4, 7, 7, 1, soft)
        if kind == "barnfloor" and variant == 3:
            c.rect(3, 5, 3, 1, hexc("92815b"))
            c.rect(11, 11, 2, 1, hexc("92815b"))
    elif kind in ("flags", "cobble"):
        # Worn slabs merge into broad calm patches, with only occasional joints.
        if variant in (1, 3):
            c.rect(0, 0, T, 1, seam)
            c.rect(0, 0, 1, T, seam)
            c.rect(2, 1, 11, 1, glint)
        if variant == 2:
            c.rect(3, 5, 9, 5, soft)
        if variant == 3:
            for x, y in ((9, 9), (10, 10), (10, 11), (11, 12)):
                c.put(x, y, seam)
    else:  # soft earth/grass: sparse tufts, no evenly spread bright pixel noise
        if variant in (1, 2):
            c.rect(2, 5, 11, 5, soft)
            c.rect(4, 4, 7, 7, soft)
        if variant == 3:
            for x, y in ((5, 8), (6, 6), (7, 8)):
                c.put(x, y, glint)
            c.rect(11, 12, 2, 1, seam)
    return c.img


def quiet_floor(name, kind, cols, variant):
    """All locations share sparse marks and shallow joints, with their own material and palette."""
    if name in VILLAGE_FLOORS:
        return village_floor(name, kind, variant)
    original = hexc(cols[0])
    grey = sum(original[i] * k for i, k in enumerate((0.3, 0.59, 0.11)))
    brightness = 1 if kind == "cloud" or grey < 85 else 0.92
    base = tuple(round((original[i] * 0.86 + grey * 0.14) * brightness) for i in range(3)) + (255,)
    c = Canvas(T, T)
    c.rect(0, 0, T, T, base)
    seam, patch, shine = shade(base, 0.91), shade(base, 0.975), shade(base, 1.045)
    if kind == "patchwork":
        # Muted quilt squares, inset hems and short stitches; all four variants
        # have structure because the plain variant covers most of a room.
        cloth = (hexc("514857"), hexc("454653"), hexc("534550"), hexc("4b4b59"))[variant]
        c.rect(0, 0, T, T, cloth)
        hem, thread = shade(cloth, .82), shade(cloth, 1.16)
        c.rect(0, 0, T, 1, hem)
        c.rect(0, 0, 1, T, hem)
        for q in range(3, T - 1, 4):
            c.rect(q, 2, 2, 1, thread)
            c.rect(2, q, 1, 2, thread)
        if variant in (1, 3):
            for x, y in ((8, 5), (10, 7), (8, 9), (6, 7)):
                c.rect(x, y, 2, 1, shade(cloth, 1.10))
    elif kind == "bedboards":
        # Long floorboards with continuous seams, staggered end joints and
        # broad wood grain rather than isolated bright specks.
        edge, grain = shade(base, .80), shade(base, 1.10)
        c.rect(0, T - 1, T, 1, edge)
        c.rect(0, 0, T, 1, grain)
        c.rect(2, 5, 11, 1, shade(base, .94))
        c.rect(5, 10, 9, 1, shade(base, 1.05))
        if variant in (1, 3):
            joint = 4 if variant == 1 else 11
            c.rect(joint, 1, 1, T - 2, edge)
            c.rect(joint + 1, 2, 1, T - 4, grain)
        if variant == 2:
            c.rect(5, 7, 5, 1, edge)
            c.rect(4, 8, 2, 1, edge)
            c.rect(9, 8, 2, 1, edge)
            c.rect(5, 9, 5, 1, grain)
    elif kind in ("planks", "barnfloor"):
        c.rect(0, 15, T, 1, seam)
        if variant in (1, 3):
            x = 4 if variant == 1 else 11
            c.rect(x, 0, 1, 15, seam)
            c.rect(x + 1, 1, 1, 13, shine)
        if variant == 2: c.rect(3, 8, 8, 1, patch)
    elif kind in ("checker", "marble", "subwaytile"):
        if variant in (1, 3):
            c.rect(0, 0, T, T, patch)
            c.rect(0, 0, T, 1, seam)
            c.rect(0, 0, 1, T, seam)
            c.rect(2, 1, 11, 1, shine)
        if kind == "marble" and variant == 3:
            for x, y in ((5, 7), (6, 8), (7, 8), (8, 9), (9, 9)): c.put(x, y, seam)
    elif kind in ("paving", "flags", "cobble", "redbrick", "sewer", "seastone", "raised"):
        if variant in (1, 3):
            c.rect(0, 0, T, 1, seam)
            c.rect(0, 0, 1, T, seam)
            c.rect(2, 1, 11, 1, shine)
        if variant == 2: c.rect(3, 5, 9, 4, patch)
        if variant == 3 and kind == "raised":
            for y in (6, 8, 10): c.rect(5, y, 6, 1, seam)
        elif variant == 3 and kind == "redbrick": c.rect(0, 8, 8, 1, seam)
        elif variant == 3 and kind == "sewer": c.rect(2, 12, 4, 1, hexc("435b4b"))
        elif variant == 3:
            for x, y in ((9, 8), (10, 9), (10, 10), (11, 11)): c.put(x, y, seam)
    elif kind == "cloud":
        if variant in (1, 3):
            disc(c, 7, 9, 6, patch)
            disc(c, 11, 8, 4, patch)
            c.rect(4, 5, 6, 1, shine)
    elif kind in ("moondust", "cheese"):
        if variant in (2, 3):
            disc(c, 8, 8, 3 if variant == 2 else 2, seam)
            disc(c, 8, 7, 2 if variant == 2 else 1, shade(base, 0.95))
            c.rect(6, 11, 4, 1, shine)
    elif kind == "sand":
        if variant in (1, 3):
            c.rect(2, 8, 8, 1, patch)
            c.rect(5, 7, 7, 1, shine)
        if variant == 3: c.rect(10, 12, 2, 1, seam)
    elif kind == "carpet":
        if variant == 1: c.rect(0, 0, T, T, patch)
        if variant == 3:
            c.rect(0, 15, T, 1, seam)
            c.rect(4, 7, 8, 1, patch)
    else:  # tarmac, gravel, grass, earth, cracked concrete and the dream floor
        if variant in (1, 2):
            c.rect(3, 4, 9, 6, patch)
            c.rect(5, 3, 5, 8, patch)
        if variant == 3:
            if kind == "dreamy":
                c.rect(7, 6, 1, 3, shine); c.rect(6, 7, 3, 1, shine)
            elif kind == "grass":
                for x, y in ((5, 8), (6, 6), (7, 8)): c.put(x, y, shine)
            elif kind == "cracked":
                for x, y in ((5, 5), (6, 6), (6, 7), (7, 8), (7, 9)): c.put(x, y, seam)
            else: c.rect(9, 10, 3, 1, seam)
    return c.img


def tileset(name, th):
    fk, *fc = th["floor"]
    wk, *wc = th["face"]
    ck, *cc = th["cap"]
    random.seed(sum(map(ord, name)))  # stable between runs (str hash() is randomised)
    tiles = [quiet_floor(name, fk, fc, s) for s in range(4)]
    # 4 face, 5 cap, 6 door frame, 7 (unused, was iron bars), 8 face + window, 9 face + decoration
    tiles += [face_tile(wk, wc, 0), cap_tile(ck, cc), door_frame(cc[1]), unused_tile(),
              face_tile(wk, wc, 1), face_tile(wk, wc, 2)]
    sheet = Image.new("RGBA", (T * len(tiles), T), (0, 0, 0, 0))
    for i, t in enumerate(tiles):
        sheet.paste(t, (i * T, 0))
    return sheet


# ---------------------------------------------------------------------- home
# The living room the game starts in (screen/HubScreen): one 224x160 background (7x5 units - one
# screen) with everything that stands against the wall baked in, plus the furniture the family walks
# in front of / behind as separate sprites. Atlas sprites/home.png + home.txt (name x y w h).
HOME = {}


def _paste(img, c, x, y):
    img.alpha_composite(c.img if isinstance(c, Canvas) else c, (x, y))


def home_floor(c, start=43, base="89694f"):
    wood = hexc(base)
    for row, y in enumerate(range(start, c.h, 14)):
        c.rect(0, y, c.w, 14, shade(wood, 1.0 if row % 2 else 0.985))
        c.rect(0, y, c.w, 1, shade(wood, 0.90))
        c.rect(0, y + 1, c.w, 1, shade(wood, 1.035))
        for x in range((row * 43) % 82, c.w, 82):
            c.rect(x, y, 1, 14, shade(wood, 0.90))


def home_room():
    W, H = 224, 160
    c = Canvas(W, H)
    paper, paper_s = hexc("ddc4a8"), hexc("d6bca0")
    for x in range(W):                          # striped wallpaper
        c.rect(x, 0, 1, 40, paper_s if (x // 6) % 2 else paper)
    for x in range(3, W, 24):                   # little flowers on the stripes
        for y in (9, 25):
            c.put(x, y + (x // 12) % 2 * 6, hexc("c86a5a"))
    c.rect(0, 38, W, 5, hexc("8a5a3a"))          # skirting board
    c.rect(0, 38, W, 1, hexc("a8744a"))
    home_floor(c)
    # the rug in the middle
    c.rect(52, 78, 120, 52, hexc("e8c070"))
    c.rect(55, 81, 114, 46, hexc("b8443e"))
    c.rect(62, 88, 100, 32, hexc("c85a4a"))
    for x in range(66, 160, 10):
        c.rect(x, 102, 3, 2, hexc("ca9c68"))
    for x in range(52, 172, 3):                  # tassels
        c.put(x, 77, hexc("f0d898"))
        c.put(x, 130, hexc("f0d898"))
    # the window, left of the door: blue sky, a cloud, red curtains
    c.rect(48, 6, 30, 24, hexc("6a4a30"))
    c.rect(50, 8, 26, 20, hexc("8fd0ff"))
    c.rect(54, 12, 9, 3, hexc("ffffff"))
    c.rect(56, 10, 5, 2, hexc("ffffff"))
    c.rect(62, 8, 2, 20, hexc("6a4a30"))
    c.rect(50, 17, 26, 2, hexc("6a4a30"))
    c.rect(44, 4, 6, 30, hexc("c0392b"))
    c.rect(76, 4, 6, 30, hexc("c0392b"))
    c.rect(44, 4, 2, 30, hexc("e05a4a"))
    c.rect(76, 4, 2, 30, hexc("e05a4a"))
    c.rect(42, 3, 42, 2, hexc("6a4a30"))         # curtain rod
    # the front door, top centre, with a door mat
    c.rect(98, 4, 28, 39, hexc("5a3218"))
    c.rect(100, 6, 24, 37, hexc("8a4e28"))
    for x, y in ((103, 9), (113, 9), (103, 24), (113, 24)):
        c.rect(x, y, 8, 12, hexc("9a5e34"))
        c.rect(x, y, 8, 1, hexc("b07448"))
    c.rect(119, 24, 3, 3, hexc("ffd23a"))       # knob
    c.rect(96, 43, 32, 8, hexc("6a8a3a"))        # mat
    c.rect(98, 45, 28, 4, hexc("7aa04a"))
    # the trophy shelf, right of the door
    c.rect(134, 20, 26, 3, hexc("6a4a30"))
    for x, col in ((137, hexc("ffd23a")), (146, hexc("c9ccd6")), (154, hexc("e09a5a"))):
        c.rect(x, 12, 5, 4, col)                  # cups
        c.rect(x + 1, 16, 3, 2, col)
        c.rect(x, 18, 5, 2, shade(col, 0.7))
    c.rect(136, 26, 22, 10, hexc("f5f0e6"))     # a family photo under it
    c.rect(138, 28, 18, 6, hexc("8fd0ff"))
    for x, col in ((140, hexc("2f9e5b")), (144, hexc("e05a8a")), (148, hexc("9a7ac0")), (152, hexc("6a6a7a"))):
        c.rect(x, 30, 2, 4, col)                 # four little people
        c.put(x, 29, SKIN)
    img = c.img
    # the bookshelf against the left wall
    b = Canvas(34, 50)
    b.rect(0, 0, 34, 50, hexc("6a4a30"))
    b.rect(2, 2, 30, 46, hexc("4a3020"))
    books = [hexc(h) for h in ("c0392b", "2f6eb0", "2f9e5b", "e0a030", "8a4ab0", "e05a8a", "f5f0e6")]
    for shelf, y in enumerate((3, 15, 27, 39)):
        b.rect(2, y + 10, 30, 2, hexc("6a4a30"))
        x = 3
        i = shelf * 3
        while x < 30:
            w = 2 + (i * 7) % 3
            hgt = 7 + (i * 5) % 3
            b.rect(x, y + 10 - hgt, w, hgt, books[i % len(books)])
            x += w + (1 if i % 4 == 0 else 0)
            i += 1
    b.rect(10, 1, 14, 2, hexc("ffd166"))          # the picture book, shining a little
    b.outline()
    _paste(img, finish_surface(b.img), 4, 4)
    # the TV on its cabinet, right side against the wall
    t = Canvas(48, 54)
    t.rect(0, 30, 48, 24, hexc("7a4a2a"))          # cabinet
    t.rect(0, 30, 48, 3, hexc("9a6a3a"))
    t.rect(3, 36, 19, 15, hexc("6a3a20"))
    t.rect(26, 36, 19, 15, hexc("6a3a20"))
    t.rect(19, 43, 2, 2, hexc("ffd23a"))
    t.rect(27, 43, 2, 2, hexc("ffd23a"))
    t.rect(4, 0, 40, 30, hexc("2a2a32"))           # the old TV
    t.rect(4, 0, 40, 2, hexc("4a4a56"))
    t.rect(7, 3, 30, 22, hexc("1a2a3a"))           # screen (lit up in code)
    t.rect(39, 6, 3, 3, hexc("c0392b"))            # knobs
    t.rect(39, 12, 3, 3, hexc("9a9aa6"))
    t.rect(14, 26, 20, 4, hexc("3a3a44"))          # stand
    t.rect(10, -1, 1, 1, hexc("9a9aa6"))
    t.outline()
    _paste(img, finish_surface(t.img), 168, 6)
    return img


def home_sofa():
    """Upholstered back, rounded arm caps, piped cushions and a recessed plinth."""
    c = Canvas(76, 30)
    fabric, light, shadow = map(hexc, ("527eaa", "7399bc", "355572"))
    c.rect(2, 6, 72, 21, shadow)
    c.rect(0, 8, 9, 18, fabric); c.rect(67, 8, 9, 18, shadow)
    c.rect(1, 5, 7, 5, light); c.rect(68, 5, 7, 5, fabric)
    for x in (9, 29, 49):
        c.rect(x, 1, 18, 10, fabric)
        c.rect(x+1, 0, 16, 2, light)
        c.rect(x+16, 3, 2, 8, shadow)
        c.rect(x+1, 9, 16, 1, shadow)
    c.rect(7, 11, 62, 15, fabric)
    c.rect(8, 11, 60, 1, light)
    c.rect(8, 24, 60, 2, shadow)
    for x in (21, 48):
        c.rect(x, 14, 1, 8, shade(fabric,0.88))
    c.rect(7, 27, 62, 1, hexc("263745"))
    for x in (7,66): c.rect(x,28,3,2,hexc("49392f"))
    c.outline()
    return c.img


def home_armchair():
    c = Canvas(30, 32)
    fabric, light, shadow = map(hexc, ("88709f", "ae94bd", "594b73"))
    c.rect(5,1,20,16,fabric); c.rect(7,0,16,2,light)
    c.rect(6,3,17,11,hexc("a08d98"))  # a folded throw with broad woven stripes
    for y in (5,11): c.rect(7,y,15,2,hexc("b9a38b"))
    c.rect(1,12,6,16,shadow); c.rect(23,12,6,16,shadow)
    c.rect(2,11,5,3,light); c.rect(24,11,4,3,fabric)
    c.rect(7,16,16,10,fabric); c.rect(8,16,14,2,light)
    c.rect(8,24,14,2,shadow)
    for x in (3,25): c.rect(x,28,3,4,hexc("49392f"))
    c.outline()
    return c.img


def home_lamp():
    c = Canvas(14,44)
    for y in range(10):
        inset = 2 if y < 3 else 1 if y < 7 else 0
        c.rect(inset,y,14-inset*2,1,hexc("e8d5a0"))
        c.put(13-inset,y,hexc("bda66d"))
    c.rect(3,0,8,1,hexc("fff0c9")); c.rect(0,9,14,1,hexc("bda66d"))
    c.rect(6,10,2,30,hexc("5e626c")); c.rect(6,11,1,28,hexc("a3a09a"))
    c.rect(3,40,8,3,hexc("43414c")); c.rect(4,40,6,1,hexc("7b7882"))
    c.outline()
    return c.img


def home_plant():
    c = Canvas(16,26)
    leaf, light, shadow = map(hexc, ("51815a", "7f9c6a", "355640"))
    c.rect(7,5,2,14,shadow)
    for x,y,r in ((5,6,4),(11,5,4),(4,11,3),(11,11,4),(8,2,3)):
        disc(c,x,y,r,leaf); c.rect(x-1,y-2,2,3,light)
    c.rect(3,16,10,9,hexc("a76b4e")); c.rect(2,15,12,3,hexc("d2976d"))
    c.rect(10,18,3,6,hexc("79503e")); c.rect(4,19,2,4,hexc("bc8360"))
    c.outline()
    return c.img


# The family's cardboard rocket (HubScreen): one layer per piece, all the same size so they stack -
# the box comes with the first win over the Fluffy King, then each hero who beats him adds theirs.
ROCKET_W, ROCKET_H = 40, 52


def home_rocket_box():
    """A big cardboard box standing on end, a porthole drawn on in crayon, packing tape down the middle."""
    c = Canvas(ROCKET_W, ROCKET_H)
    card, card_s, card_h = hexc("c89a5a"), hexc("a87a40"), hexc("dcb47a")
    c.rect(12, 14, 16, 31, card)
    c.rect(12, 14, 2, 31, card_h)
    c.rect(26, 14, 2, 31, card_s)
    c.rect(19, 14, 2, 31, hexc("d8c49a"))          # tape
    c.rect(12, 14, 16, 1, card_h)                  # the flaps folded in
    blue = hexc("3a6ad0")                          # the crayon porthole, a bit wobbly
    for x, y in ((17, 19), (18, 18), (19, 18), (20, 18), (21, 18), (22, 19), (23, 20), (23, 21), (23, 22),
                 (22, 23), (21, 24), (20, 24), (19, 24), (18, 24), (17, 23), (16, 22), (16, 21), (16, 20)):
        c.put(x, y, blue)
    c.rect(17, 19, 6, 5, hexc("8fd0ff"))
    c.put(18, 20, hexc("ffffff"))
    for x, y in ((14, 31), (24, 35), (15, 38)):     # crayon stars
        c.put(x, y, hexc("ffd23a"))
        c.put(x - 1, y, hexc("ffd23a"))
        c.put(x + 1, y, hexc("ffd23a"))
        c.put(x, y - 1, hexc("ffd23a"))
        c.put(x, y + 1, hexc("ffd23a"))
    c.outline()
    return c.img


def home_rocket_cone():
    """The boy's piece: a traffic cone for the nose."""
    c = Canvas(ROCKET_W, ROCKET_H)
    orange, orange_s = hexc("f07a2a"), hexc("c85a1a")
    for y in range(0, 14):
        half = 1 + y * 7 // 13
        c.rect(20 - half, y, half * 2, 1, orange)
        c.put(20 + half - 1, y, orange_s)
    c.rect(15, 5, 10, 2, hexc("f5f0e6"))           # its white stripes
    c.rect(14, 9, 12, 2, hexc("f5f0e6"))
    c.rect(11, 13, 18, 2, orange_s)                # the square foot
    c.outline()
    return c.img


def home_rocket_fins():
    """Sister's piece: two fins cut like throwing stars, painted dark with her red ninja sign."""
    c = Canvas(ROCKET_W, ROCKET_H)
    navy, navy_h = hexc("2e2e44"), hexc("4a4a6a")
    for side in (-1, 1):
        x0 = 12 if side < 0 else 27
        for i in range(14):                          # a slanted fin sweeping down and out
            w = 2 + i * 5 // 13
            x = x0 - w if side < 0 else x0 + 1
            c.rect(x, 32 + i, w, 1, navy)
        tip = 5 if side < 0 else 34
        c.rect(tip - 1, 46, 3, 2, navy)              # the star points
        c.put(tip, 48, navy)
        c.put(tip + 2 * side, 44, navy)
        c.rect(x0 - 3 if side < 0 else x0 + 2, 39, 2, 2, hexc("d63a2f"))  # the sign
        c.put(x0 - 1 if side < 0 else x0 + 1, 33, navy_h)
    c.outline()
    return c.img


def home_rocket_scarf():
    """Granny's piece: a knitted scarf round the top of the box, and a little space helmet for the cat."""
    c = Canvas(ROCKET_W, ROCKET_H)
    purple, yellow = hexc("9a7ac0"), hexc("e8c070")
    for x in range(11, 30):
        c.rect(x, 14, 1, 3, purple if (x // 2) % 2 else yellow)
    for y in range(17, 28):                          # the end hanging down the side
        c.rect(28, y, 3, 1, purple if (y // 2) % 2 else yellow)
    for x in (28, 30):
        c.put(x, 28, yellow)                         # fringe
    # the cat's helmet: a glass bowl on the floor beside the rocket
    glass, rim = hexc("bfe6ff"), hexc("9a9aa8")
    c.rect(33, 44, 6, 5, glass)
    c.rect(34, 43, 4, 1, glass)
    c.put(34, 45, hexc("ffffff"))
    c.rect(32, 49, 8, 2, rim)
    c.outline()
    return c.img


def home_rocket_engine():
    """Grandad's piece: the old vacuum cleaner bolted on underneath as the engine, his toolbox beside it."""
    c = Canvas(ROCKET_W, ROCKET_H)
    grey, grey_s, grey_h = hexc("8a8a96"), hexc("6a6a74"), hexc("aaaab6")
    c.rect(14, 45, 12, 5, grey)
    c.rect(14, 45, 12, 1, grey_h)
    c.rect(14, 49, 12, 1, grey_s)
    for x in (15, 24):
        c.put(x, 47, hexc("4a4a52"))               # bolts
    c.rect(17, 50, 6, 2, hexc("4a4a52"))           # the nozzle
    c.rect(26, 46, 2, 2, hexc("3a6ad0"))           # the hose curling round the back
    c.rect(28, 47, 2, 3, hexc("3a6ad0"))
    # the toolbox
    c.rect(0, 46, 9, 5, hexc("c0392b"))
    c.rect(0, 46, 9, 1, hexc("e05a4a"))
    c.rect(3, 44, 3, 1, hexc("4a4a52"))
    c.put(2, 45, hexc("4a4a52"))
    c.put(6, 45, hexc("4a4a52"))
    c.outline()
    return c.img


def home_console():
    """The rocket game console on the floor in front of the TV (once the rocket has been up to the Moon):
    a little grey box with a cartridge in, its cable going up to the TV, a pad lying in front of it."""
    c = Canvas(26, 17)
    grey, grey_s, grey_h = hexc("c8c8d4"), hexc("9a9aa8"), hexc("e8e8f0")
    c.rect(19, 0, 1, 4, hexc("3a3a44"))           # the cable up to the TV
    c.rect(10, 4, 16, 8, grey)                     # the console
    c.rect(10, 4, 16, 2, grey_h)
    c.rect(10, 11, 16, 1, grey_s)
    c.rect(13, 2, 8, 3, hexc("e05a4a"))            # the rocket game's cartridge
    c.rect(14, 2, 6, 1, hexc("ffd166"))
    c.rect(22, 7, 2, 2, hexc("ff4a4a"))            # power light
    c.rect(12, 8, 6, 1, grey_s)                    # vents
    c.rect(12, 10, 6, 1, grey_s)
    c.rect(9, 11, 1, 2, hexc("3a3a44"))            # the pad's cord
    c.rect(7, 12, 2, 1, hexc("3a3a44"))
    c.rect(0, 12, 8, 5, hexc("4a4a56"))            # the pad
    c.rect(1, 13, 1, 3, hexc("2a2a32"))            # d-pad
    c.rect(0, 14, 3, 1, hexc("2a2a32"))
    c.put(5, 14, hexc("e05a4a"))                   # buttons
    c.put(6, 13, hexc("4a8ae0"))
    c.outline()
    return c.img


def home_throne():
    """The Fluffy King's throne: far too big for his room, it pokes out into the living room - a tall pink
    fluffy back in a gold frame, a plump seat, gold claw feet."""
    c = Canvas(46, 50)
    pink, pink_sh, pink_hi = hexc("ffd6e8"), hexc("f5a3c7"), hexc("fff0f6")
    gold, gold_sh = hexc("ffc83d"), hexc("d8962a")
    c.rect(5, 2, 36, 32, gold)                       # the gold frame of the back
    c.rect(8, 5, 30, 28, pink)                       # the fluffy back
    for x in range(8, 38, 6):                        # puffs along its top
        disc(c, x + 3, 6, 3, pink)
    disc(c, 14, 12, 4, pink_hi)
    for x, y in ((11, 20), (23, 14), (33, 22), (18, 27), (29, 29)):  # tufts
        c.put(x, y, pink_sh)
        c.put(x + 1, y, pink_sh)
    c.rect(20, 0, 6, 4, gold)                        # a crown on top of the back
    c.rect(19, 0, 1, 2, gold)
    c.rect(26, 0, 1, 2, gold)
    c.put(23, 2, hexc("e84393"))
    c.rect(0, 22, 9, 20, gold)                       # the arms
    c.rect(37, 22, 9, 20, gold_sh)
    disc(c, 4, 22, 4, pink)
    disc(c, 41, 22, 4, pink_sh)
    c.rect(7, 31, 32, 11, pink)                      # the seat cushion
    c.rect(7, 31, 32, 2, pink_hi)
    c.rect(7, 40, 32, 2, pink_sh)
    c.rect(4, 42, 38, 3, gold)                       # the base
    c.rect(4, 44, 38, 1, gold_sh)
    for x in (4, 38):                                # claw feet
        c.rect(x, 45, 4, 5, gold)
        c.rect(x, 48, 4, 2, gold_sh)
    c.outline()
    return c.img


def home_king_room():
    """The Fluffy King's room, off the living room to the left: lilac wallpaper with little gold crowns, two
    tall windows onto the sky, pink banners, a pale pink floor like his throne room in the clouds, a round
    rug for his throne (drawn in code, big) and the royal carpet out through the doorway on the right."""
    W, H = 224, 160
    c = Canvas(W, H)
    paper, paper_s, gold, gold_sh = hexc("e8c8e8"), hexc("dcb4dc"), hexc("ffc83d"), hexc("d8962a")
    for x in range(W):                                   # striped wallpaper
        c.rect(x, 0, 1, 40, paper_s if (x // 8) % 2 else paper)
    for x in range(8, W, 32):                            # little gold crowns on it
        for y in (8, 24):
            cx = x + (16 if y == 24 else 0)
            c.rect(cx, y + 2, 5, 2, gold)
            c.put(cx, y + 1, gold)
            c.put(cx + 2, y, gold)
            c.put(cx + 4, y + 1, gold)
    c.rect(0, 38, W, 5, gold_sh)                          # a gold skirting board
    c.rect(0, 38, W, 1, gold)
    for row, y in enumerate(range(43, H, 16)):           # wide, low-contrast rose slabs
        for col, x in enumerate(range(0, W, 16)):
            c.rect(x, y, 16, 16, hexc("f0dce6") if (row + col) % 2 else hexc("ead5df"))
    for wx in (14, 180):                                  # two tall windows onto the sky
        c.rect(wx, 4, 30, 32, gold_sh)
        c.rect(wx + 2, 6, 26, 30, hexc("8fd0ff"))
        c.rect(wx + 5, 14, 10, 3, hexc("ffffff"))
        c.rect(wx + 7, 12, 6, 2, hexc("ffffff"))
        c.rect(wx + 16, 24, 9, 3, hexc("ffffff"))
        c.rect(wx + 14, 6, 2, 30, gold_sh)
        c.rect(wx + 2, 20, 26, 2, gold_sh)
    for bx in (64, 146):                                  # pink banners with a crown
        c.rect(bx, 2, 14, 26, hexc("e84393"))
        c.rect(bx, 2, 14, 2, gold)
        c.rect(bx, 26, 4, 4, hexc("e84393"))
        c.rect(bx + 10, 26, 4, 4, hexc("e84393"))
        c.rect(bx + 4, 12, 6, 3, gold)
        c.put(bx + 4, 11, gold)
        c.put(bx + 7, 10, gold)
        c.put(bx + 9, 11, gold)
    rug, rug_sh = hexc("f5a3c7"), hexc("e07aa8")         # the round rug under the throne
    for y in range(52, 112):
        for x in range(56, 168):
            if ((x - 112) / 56) ** 2 + ((y - 82) / 30) ** 2 <= 1:
                c.put(x, y, rug)
                if ((x - 112) / 52) ** 2 + ((y - 82) / 26) ** 2 > 1:
                    c.put(x, y, gold)
    for y in range(52, 112):
        for x in range(56, 168):
            if 0.55 < ((x - 112) / 56) ** 2 + ((y - 82) / 30) ** 2 <= 0.62:
                c.put(x, y, rug_sh)
    c.rect(150, 74, W - 150, 26, hexc("c0306a"))          # the royal carpet out to the living room
    c.rect(150, 74, W - 150, 2, gold)
    c.rect(150, 98, W - 150, 2, gold)
    c.rect(W - 4, 70, 4, 34, hexc("5a3a4a"))              # the doorway
    return c.img


def home_bedroom():
    """The kids' bedroom, off the living room to the right (the dreams start here): night-blue wallpaper with
    little moons, the window onto the night with the moon in it, a calendar on the wall (today's dream), the
    same floorboards as the living room, a round rug with a star and the doorway out on the left. The glow
    stars on the wall, one a finished dream, are drawn in code; the bed and the toy box stand free."""
    W, H = 224, 160
    c = Canvas(W, H)
    paper, paper_s, moon = hexc("3a3a72"), hexc("34346a"), hexc("8a8ac0")
    for x in range(W):                                   # striped wallpaper
        c.rect(x, 0, 1, 40, paper_s if (x // 8) % 2 else paper)
    for x in range(6, W, 24):                            # little moons on it
        for y in (9, 27):
            cx = x + (12 if y == 27 else 0)
            disc(c, cx, y, 2, moon)
            disc(c, cx + 1, y - 1, 1.6, paper_s if (cx // 8) % 2 else paper)
    c.rect(0, 38, W, 5, hexc("5a4a7a"))                   # skirting board
    c.rect(0, 38, W, 1, hexc("7a6a9a"))
    home_floor(c, base="7e6658")
    rug, rug_sh, star = hexc("4a6ab8"), hexc("3a5498"), hexc("ffe48a")  # the round rug with a star
    for y in range(84, 136):
        for x in range(60, 160):
            e = ((x - 110) / 50) ** 2 + ((y - 110) / 26) ** 2
            if e <= 1:
                c.put(x, y, rug_sh if e > 0.8 else rug)
    for y in range(96, 125):                             # its star, squashed flat like the rug
        for x in range(86, 135):
            dx, dy = (x - 110) / 1.7, (y - 110)
            r = math.hypot(dx, dy)
            a = (math.degrees(math.atan2(dy, dx)) + 90) % 72 - 36   # 0 at a point, +-36 between points
            if r <= 13 * (1 - 0.55 * abs(a) / 36):
                c.put(x, y, star)
    # the window: the night outside, the moon and a few stars, blue curtains
    c.rect(40, 5, 34, 26, hexc("5a4a7a"))
    c.rect(42, 7, 30, 22, hexc("141438"))
    disc(c, 63, 14, 5, hexc("fff4c8"))
    disc(c, 65, 12, 4, hexc("141438"))
    for x, y in ((47, 11), (52, 22), (68, 24), (57, 9)):
        c.put(x, y, hexc("ffffff"))
    c.rect(56, 7, 2, 22, hexc("5a4a7a"))
    c.rect(42, 17, 30, 2, hexc("5a4a7a"))
    for cx in (36, 74):
        c.rect(cx, 3, 6, 32, hexc("4a6ab8"))
        c.rect(cx, 3, 2, 32, hexc("6a8ad8"))
    c.rect(34, 2, 48, 2, hexc("5a4a7a"))
    # the calendar: a page with today circled in red
    c.rect(100, 8, 22, 26, hexc("f5f0e6"))
    c.rect(100, 8, 22, 6, hexc("e05a4a"))
    c.rect(110, 5, 2, 4, hexc("6a6a74"))
    for row in range(4):
        for col in range(5):
            c.rect(102 + col * 4, 16 + row * 4, 2, 2, hexc("8a8a96"))
    for t in range(16):
        a = t / 16 * 2 * math.pi
        c.put(int(111 + math.cos(a) * 3), int(23 + math.sin(a) * 3), hexc("e0302a"))
    # a nightlight in the wall socket, glowing
    disc(c, 140, 33, 6, (255, 228, 138, 60))
    c.rect(137, 30, 6, 6, hexc("fff4c8"))
    c.rect(138, 31, 4, 3, hexc("ffe48a"))
    # the doorway out to the living room, on the left, and its runner
    c.rect(0, 74, 40, 26, hexc("4a6ab8"))
    c.rect(0, 74, 40, 2, star)
    c.rect(0, 98, 40, 2, star)
    c.rect(0, 70, 4, 34, hexc("2a2238"))
    return c.img


def home_bed(opened=False):
    """The kids' bed against the wall: a wooden headboard with a moon cut out, a fat pillow and a starry blue
    quilt - the hero climbs in to dream."""
    c = Canvas(56, 60)
    wood, wood_sh, wood_hi = hexc("b07d4c"), hexc("8a5e36"), hexc("c8945e")
    c.rect(2, 0, 52, 22, wood)                     # the headboard
    c.rect(2, 0, 52, 3, wood_hi)
    disc(c, 28, 10, 6, wood_sh)                    # a moon cut out of it
    disc(c, 30, 8, 5, wood)
    c.rect(0, 0, 4, 58, wood_sh)                   # the posts
    c.rect(52, 0, 4, 58, wood_sh)
    c.rect(4, 18, 48, 8, hexc("f5f0e6"))           # the pillow
    c.rect(6, 17, 44, 2, hexc("ffffff"))
    c.rect(4, 25, 48, 1, hexc("d8d0c0"))
    quilt, quilt_sh, star = hexc("4a6ab8"), hexc("3a5498"), hexc("ffe48a")
    c.rect(4, 26, 48, 26, quilt)                   # the quilt
    c.rect(4, 26, 48, 3, hexc("f5f0e6"))           # its folded-back top
    for x, y in ((12, 34), (26, 40), (40, 33), (18, 46), (44, 45), (33, 31)):
        c.put(x, y, star)
        c.put(x - 1, y, shade(star, 0.75))
        c.put(x + 1, y, shade(star, 0.75))
        c.put(x, y - 1, shade(star, 0.75))
        c.put(x, y + 1, shade(star, 0.75))
    c.rect(4, 50, 48, 2, quilt_sh)
    c.rect(2, 52, 52, 6, wood)                     # the foot board
    c.rect(2, 52, 52, 1, wood_hi)
    if opened:
        # The quilt curls up at the middle of the footboard, exposing a dark gap.
        rows = ((50, 9), (51, 13), (52, 17), (53, 21),
                (54, 23), (55, 25), (56, 27), (57, 27))
        for y, width in rows:
            x = 28 - width // 2
            c.rect(x, y, width, 1, hexc("181420"))
            if y <= 54:
                c.rect(x - 1, y - 1, width + 2, 1, quilt_sh)
                c.rect(x, y - 2, width, 1, shade(quilt, 1.12))
    c.outline()
    return c.img


def home_toybox():
    """The toy box (the boss rush): a wooden chest painted with stars, its lid propped open, little toy bosses
    peeking out - a crown, a traffic light, a tiny train funnel."""
    c = Canvas(32, 30)
    red, red_sh, trim = hexc("c0504a"), hexc("94383a"), hexc("ffd166")
    c.rect(2, 4, 28, 4, red_sh)                    # the lid, propped open behind
    c.rect(2, 4, 28, 1, trim)
    c.rect(6, 6, 4, 6, hexc("ffc83d"))             # toys poking out: a crown...
    c.put(6, 5, hexc("ffc83d"))
    c.put(9, 5, hexc("ffc83d"))
    c.rect(13, 5, 4, 8, hexc("2e3a2e"))            # ...a traffic light...
    c.put(14, 6, hexc("ff4a3a"))
    c.put(14, 9, hexc("5ae06a"))
    c.rect(20, 7, 4, 5, hexc("3a3a44"))            # ...a train's funnel...
    c.rect(19, 6, 6, 2, hexc("5a5a66"))
    disc(c, 27, 9, 2.4, hexc("f4f2fc"))            # ...a bit of cloud
    c.rect(1, 12, 30, 17, red)                     # the box
    c.rect(1, 12, 30, 2, trim)
    c.rect(1, 27, 30, 2, red_sh)
    for x, y in ((7, 19), (16, 22), (25, 18)):     # painted stars
        c.put(x, y, trim)
        c.put(x - 1, y, trim)
        c.put(x + 1, y, trim)
        c.put(x, y - 1, trim)
        c.put(x, y + 1, trim)
    c.outline()
    return c.img


def home_sheet():
    parts = [("room", home_room()), ("sofa", home_sofa()), ("armchair", home_armchair()),
             ("lamp", home_lamp()), ("plant", home_plant()),
             ("rocket_box", home_rocket_box()), ("rocket_cone", home_rocket_cone()),
             ("rocket_fins", home_rocket_fins()), ("rocket_scarf", home_rocket_scarf()),
             ("rocket_engine", home_rocket_engine()), ("console", home_console()),
             ("throne", home_throne()), ("king_room", home_king_room()),
             ("bedroom", home_bedroom()), ("bed", home_bed()), ("toybox", home_toybox()),
             ("bed_open", home_bed(opened=True))]
    w = sum(p.width + 2 for _, p in parts)
    h = max(p.height for _, p in parts)
    sheet = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    x = 0
    for name, p in parts:
        if name not in ("room", "king_room", "bedroom", "plant"):
            p = finish_surface(p)
        sheet.paste(p, (x, 0))
        HOME[name] = (x, 0, p.width, p.height)
        x += p.width + 2
    return sheet



def rat_frame(frame=4, mark=None):
    """Authored scurrying rat, like the kin: firm silhouette and three clean fur planes.

    mark=None is a town rat; 0..1 fades the marked ear/eye for the normal ending.
    """
    c = Canvas(40, 28)
    ink, fur, hi, sh = map(hexc, ("25212b", "91838f", "b3a3ad", "615664"))
    pink, pink_sh = hexc("e9a4aa"), hexc("ab6c81")
    bob = 1 if frame in (0, 2) else 0
    # Bent tail and planted feet, with a separate pair of scurrying poses.
    for x, y, w in ((4, 22, 4), (7, 21, 4), (10, 20, 5), (14, 19, 3)):
        c.rect(x, y + bob, w, 1, pink)
        c.rect(x + 1, y + 1 + bob, w - 1, 1, pink_sh)
    for x, step in ((18, 1 if frame in (0, 1) else 0), (27, 1 if frame in (2, 3) else 0)):
        c.rect(x + step, 22 + bob, 4, 2 - bob, sh)
        c.rect(x + step + 1, 24, 3, 1, pink_sh)
    # A long low back and angular muzzle, not overlapping shaded circles.
    for y in range(8, 23):
        for x in range(13, 32):
            if ((x-22)/9)**2 + ((y-16-bob)/6.5)**2 <= 1:
                c.put(x, y, hi if y < 13 + bob and x < 25 else sh if y > 19 + bob or x > 28 else fur)
    # Taper the cheek into the muzzle instead of adding a square step to the back.
    c.rect(27, 13 + bob, 5, 7, fur)
    c.rect(23, 14 + bob, 5, 2, hi)
    c.rect(18, 16 + bob, 3, 1, hi)
    c.rect(20, 19 + bob, 5, 1, sh)
    c.rect(30, 15 + bob, 5, 5, fur)
    c.rect(32, 17 + bob, 4, 3, hi)
    c.rect(34, 17 + bob, 2, 2, pink)
    c.rect(28, 19 + bob, 5, 2, sh)
    # One large ear and one compact inset eye. The mark changes these landmarks only.
    if mark is None:
        disc(c, 27, 11 + bob, 3, sh)
        disc(c, 27, 11 + bob, 2, pink_sh)
        c.rect(26, 10 + bob, 2, 2, pink)
        eye = hexc("cf666e")
    else:
        def mix(a, b): return tuple(round(x*(1-mark)+y*mark) for x,y in zip(a,b))
        ear = mix(hexc("c8e85a"), hexc("8a7a6a"))
        ear_sh = mix(hexc("7a9a2a"), hexc("5a4e44"))
        c.rect(25, 6 + bob, 5, 7, ear_sh)
        c.rect(26, 5 + bob, 3, 7, ear)
        c.put(27, 4 + bob, ear)
        eye = mix(hexc("c8e85a"), hexc("16141c"))
    c.rect(30, 13 + bob, 3, 3, ink)
    c.rect(31, 13 + bob, 2, 2, eye)
    c.put(31, 13 + bob, hexc("fff5e9"))
    c.outline(ink)
    return c.img


def rat_sheet():
    # Plain, nine stages of the ending's mark fade, then plain/marked white hit-flash rows.
    sheet = Image.new("RGBA", (40 * 5, 28 * 12))
    for row in range(12):
        for frame in range(5):
            img = rat_frame(frame, None if row in (0,10) else 0 if row == 11 else (row-1)/8)
            if row >= 10:
                white = Image.new("RGBA", img.size, "white")
                white.putalpha(img.getchannel("A")); img = white
            sheet.paste(img, (frame*40, row*28))
    return sheet

def main():
    import boss_art
    import enemy_art
    import projectile_art
    from pathlib import Path
    os.makedirs(os.path.join(ROOT, "sprites"), exist_ok=True)
    enemy_art.generate(os.path.join(ROOT, "sprites"))
    boss_art.generate(Path(ROOT) / "sprites")
    projectile_art.generate(Path(ROOT) / "sprites")
    os.makedirs(os.path.join(ROOT, "tiles"), exist_ok=True)
    for name, look in FAMILY.items():
        player_sheet(look).save(os.path.join(ROOT, "sprites", name + ".png"))
    props_sheet().save(os.path.join(ROOT, "sprites", "props.png"))
    kin_sheet().save(os.path.join(ROOT, "sprites", "bullet_kin.png"))
    kin_sheet(shell=True).save(os.path.join(ROOT, "sprites", "shotgun_kin.png"))
    kin_weapons().save(os.path.join(ROOT, "sprites", "kin_weapons.png"))
    rat_sheet().save(os.path.join(ROOT, "sprites", "rat.png"))
    for kind in KIN_VARIANTS:
        variant_kin_sheet(kind).save(os.path.join(ROOT,"sprites","actor_"+kind.lower()+".png"))
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
    home_sheet().save(os.path.join(ROOT, "sprites", "home.png"))
    with open(os.path.join(ROOT, "sprites", "home.txt"), "w") as f:  # name x y w h
        for k, v in HOME.items():
            f.write(k + " " + " ".join(str(n) for n in v) + "\n")
    for name, th in THEMES.items():
        tileset(name, th).save(os.path.join(ROOT, "tiles", name + ".png"))
    print("props atlas:")
    for k, v in PROPS.items():
        print(f"  {k}: {v}")


if __name__ == "__main__":
    main()
