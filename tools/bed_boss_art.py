"""Painted silhouettes for the six residents of Under the Bed, in BossArt order."""
from PIL import Image, ImageDraw

PARTS = ("DUST_BUNNY", "BED_MONSTER", "SOCK_NINJA", "YARN_HYDRA", "WIND_UP_GENERAL", "LINT_REGENT")
INK = "#211827"


def face(d, x, y, scale=1):
    """Two narrow eyes looking out of a dark face, with brows tilted inward."""
    for side in (-1, 1):
        def point(dx, dy):
            return (round(x + side * dx * scale), round(y + dy * scale))
        d.polygon([point(3, 1), point(15, -4), point(14, 4), point(8, 6)], fill=INK)
        d.polygon([point(5, 1), point(13, -2), point(12, 3), point(8, 4)], fill="#ffd886")
        d.line([point(10, 0), point(9, 4)], fill="#a74650", width=max(1, round(2 * scale)))


def dust_bunny(d):
    for ear in (
        [(13,31),(5,13),(5,5),(10,2),(17,8),(24,27)],
        [(39,27),(48,9),(56,3),(60,6),(57,16),(51,33)],
    ):
        d.polygon(ear, fill="#7f7082", outline=INK, width=2)
    d.polygon([(13,25),(8,8),(12,7),(17,14),(20,25)], fill="#ba8998")
    d.polygon([(44,27),(53,10),(57,7),(55,15),(48,29)], fill="#a67b8e")
    fur = [(2,48),(7,40),(7,33),(13,33),(13,27),(21,29),(24,23),
           (30,26),(37,20),(39,25),(49,25),(49,31),(57,31),(56,38),(63,44),
           (60,52),(62,56),(53,59),(43,59),(37,63),(20,60),(10,61),(7,56)]
    d.polygon(fur, fill="#635767", outline=INK, width=2)
    for pts, fill in (
        ([(8,43),(17,33),(19,37),(27,28),(34,31),(42,26),(49,34),(55,35),(50,42),
          (41,39),(25,40),(16,49)], "#a397a8"),
        ([(9,53),(17,46),(23,48),(32,44),(42,48),(54,46),(60,51),
          (53,55),(39,54),(30,59),(24,56),(12,57)], "#807286"),
        ([(15,35),(21,32),(30,33),(26,37),(19,38)], "#c5b4c3"),
        ([(30,32),(37,27),(44,28),(41,32)], "#c5b4c3"),
    ):
        d.polygon(pts, fill=fill)
    for x, y in ((12,42),(19,42),(27,31),(35,35),(44,34),(51,36),(14,52),(48,54),(55,48),(21,56)):
        d.line([(x-2,y+2),(x,y),(x+2,y)], fill="#c1acba", width=1)
    d.polygon([(17,46),(24,43),(38,43),(47,47),(44,53),(37,57),(25,55)], fill=INK)
    for x,y,w in ((20,45,5),(26,44,4),(35,44,5),(42,46,4)):
        d.polygon([(x,y),(x+w,y),(x+w-2,y+7)], fill="#f6e3bb")
    for x in (7,12,17,47,52,57):
        d.polygon([(x,50),(x+3,52),(x+(4 if x > 30 else -2),59)], fill="#d4bec2")
    face(d, 32, 39, .77)


def bed_monster(d):
    d.polygon([(1,51),(3,41),(14,37),(16,23),(21,12),(33,6),(43,11),(50,22),
               (51,36),(61,39),(63,55),(58,61),(53,53),(47,58),(39,53),(31,61),
               (25,55),(15,59),(11,54),(8,62),(4,60)], fill="#74627e", outline=INK, width=2)
    d.polygon([(8,43),(20,36),(21,23),(26,13),(34,9),(43,15),(48,31),
               (44,39),(48,53),(40,49),(31,56),(31,44),(23,52),(15,50),(9,54)], fill="#c0b1c2")
    d.polygon([(20,39),(23,24),(28,18),(29,28),(27,40),(21,47),(14,49)], fill="#e1d2d8")
    d.polygon([(39,14),(46,21),(49,34),(52,40),(61,45),(56,49),(49,46),(45,41)], fill="#9a85a1")
    d.polygon([(20,35),(31,25),(44,31),(46,40),(37,47),(27,46),(19,40)], fill="#302338")
    face(d, 33, 35, .74)
    for x,y in ((10,43),(13,42),(23,50),(43,23),(45,26),(52,44),(56,46)):
        d.line([(x,y-2),(x+2,y+2)], fill="#ead1bf")
    d.line([(26,15),(23,22),(20,34),(17,40)], fill="#f4dfdc", width=2)
    for x, side in ((5, -1),(58,1)):
        for j in range(3):
            d.line([(x,49+j*3),(x+side*3,53+j*3)], fill="#393044", width=2)


def sock_ninja(d):
    d.polygon([(12,6),(36,2),(50,7),(48,31),(45,40),(57,44),(61,52),(56,59),
               (44,62),(23,59),(12,54),(13,40),(9,30)], fill="#4d4675", outline=INK, width=2)
    d.polygon([(13,10),(36,5),(46,9),(43,18),(13,20)], fill="#e0cbb6")
    d.polygon([(14,22),(44,18),(43,24),(16,29)], fill="#9875aa")
    d.polygon([(11,29),(46,23),(54,27),(63,23),(62,30),(56,34),(46,32),(14,39)],
              fill="#241d37", outline=INK)
    d.polygon([(16,42),(38,35),(43,43),(53,45),(57,52),(50,57),(41,56),(23,52)], fill="#766591")
    d.line([(19,46),(29,48),(39,46),(48,49),(55,51)], fill="#b99ab4", width=2)
    d.line([(19,8),(20,15),(16,25)], fill="#f6ddc3", width=2)
    for x in range(18, 45, 4):
        d.line([(x,11),(x+1,15)], fill="#a88994")
    for x,y in ((19,49),(24,51),(29,53),(35,55),(41,56),(47,56)):
        d.line([(x,y),(x+2,y+2)], fill="#3b355f")
    face(d, 30, 29, .8)


def yarn_hydra(d):
    d.ellipse((11,29,53,63), fill="#893751", outline=INK, width=2)
    for box, start, end, col in (
        ((11,31,52,61), 175, 355, "#f5a68d"), ((12,33,52,60), 4, 176, "#aa5069"),
        ((14,34,47,56), 165, 340, "#d06b7c"), ((20,35,51,59), 15, 174, "#f29a97"),
        ((12,39,44,62), 20, 135, "#e97a88"),
    ):
        d.arc(box, start, end, fill=col, width=2)
    for x,y,tail in ((13,21,24),(51,19,40),(32,13,32)):
        d.line([(tail,48),(x,33),(x,y)], fill=INK, width=9)
        d.line([(tail,48),(x,33),(x,y)], fill="#b55068", width=6)
        d.line([(tail-2,45),(x-2,33),(x-2,y)], fill="#eb8b93", width=1)
        d.polygon([(x-10,y+3),(x-9,y-6),(x-2,y-10),(x+4,y-9),(x+10,y-3),(x+10,y+6),
                   (x+4,y+10),(x-4,y+9)], fill="#d96778", outline=INK, width=2)
        d.line([(x-7,y-4),(x-3,y-7),(x+3,y-6)], fill="#ffb5a4", width=2)
        face(d,x,y,.48)
        d.line([(x-5,y+5),(x+4,y+6)], fill=INK, width=2)
        d.polygon([(x-3,y+5),(x,y+5),(x-2,y+8)], fill="#f8dfc4")


def wind_up_general(d):
    for x in (4,47):
        d.rounded_rectangle((x,20,x+12,60), radius=4, fill="#373746", outline=INK, width=2)
        for y in range(24,57,5):
            d.line([(x+2,y),(x+10,y-1)], fill="#a4977f", width=2)
    d.polygon([(11,27),(18,20),(43,19),(54,28),(53,50),(45,55),(15,55),(10,48)],
              fill="#766248", outline=INK, width=2)
    d.polygon([(14,30),(20,24),(42,23),(49,29),(46,35),(20,35)], fill="#c6ae75")
    d.rounded_rectangle((19,7,45,29), radius=6, fill="#807854", outline=INK, width=2)
    d.line([(21,13),(27,9),(38,9)], fill="#e4cc90", width=2)
    d.rectangle((21,15,42,24), fill="#3c3436")
    face(d,31,18,.64)
    d.line([(42,11),(51,8)], fill="#b88750", width=3)
    for x in (50,55):
        d.ellipse((x,3,x+7,12), outline="#eac26e", width=2)
    d.polygon([(25,31),(38,31),(37,58),(34,62),(28,62),(24,57)], fill="#aa946d", outline=INK, width=2)
    d.line([(27,34),(27,54)], fill="#dbc494", width=2)
    d.ellipse((26,56,35,61), fill="#312831", outline="#f1d293")
    for x,y in ((16,29),(46,30),(16,49),(48,49),(22,37),(43,37)):
        d.ellipse((x,y,x+3,y+3), fill="#372f32")
        d.point((x,y), fill="#e0c68d")


def lint_regent(d):
    d.polygon([(5,25),(7,10),(18,6),(48,9),(54,18),(54,55),(48,60),(8,59)],
              fill="#684453", outline=INK, width=2)
    d.line([(8,52),(10,15),(19,10),(46,13)], fill="#c4937e", width=2)
    for x,y in ((12,23),(17,17),(16,33),(47,17),(48,29),(49,41)):
        d.line([(x-2,y),(x,y-2),(x+2,y),(x,y+2),(x-2,y)], fill="#a06a70")
    d.polygon([(8,54),(11,36),(19,25),(46,27),(50,42),(55,54),(48,60),(18,62)],
              fill="#726083", outline=INK, width=2)
    d.polygon([(11,52),(22,31),(24,43),(34,50),(43,48),(48,57),(28,60),(17,59)], fill="#9a7496")
    d.ellipse((17,20,48,49), fill="#82738e", outline=INK, width=2)
    d.line([(21,28),(25,24),(31,25),(39,22),(44,26)], fill="#c6a5bc", width=2)
    d.polygon([(17,25),(15,7),(24,14),(30,2),(36,13),(46,6),(46,26)],
              fill="#d9a04c", outline=INK, width=2)
    d.line([(18,20),(29,20),(32,16),(37,21),(44,20)], fill="#fff09b", width=2)
    d.polygon([(31,19),(35,22),(32,26),(28,23)], fill="#bc4656")
    face(d,32,34,.84)
    d.line([(23,43),(28,40),(36,42),(42,39)], fill=INK, width=2)
    d.line([(49,56),(56,23)], fill="#d9ac67", width=4)
    d.rounded_rectangle((51,16,62,29), radius=2, fill="#be8360", outline=INK)
    for y in range(19,27,2):
        d.line([(52,y),(60,y)], fill="#efb78b")
    for x,y in ((16,52),(21,54),(39,52),(43,56),(26,45)):
        d.arc((x-3,y-2,x+3,y+2), 180, 345, fill="#cc9da9")


def part(name):
    image = Image.new("RGBA", (64, 64))
    paint = (dust_bunny, bed_monster, sock_ninja, yarn_hydra, wind_up_general, lint_regent)[PARTS.index(name)]
    paint(ImageDraw.Draw(image))
    return image
