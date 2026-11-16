"""Small, tintable projectile sprites with broad facets and a separate glint layer."""
from math import cos, pi, sin
from PIL import Image, ImageChops, ImageDraw

SIZE = 32
PARTS = ("STAR", "CLOUD", "FEATHER", "NOTE", "ROCKET", "WING", "SHARD", "SWORD", "PETAL", "PILLOW",
         "SHARK_CLOSED", "SHARK_OPEN", "SHARK_GAPE", "SHARK_TAIL")

def part(name):
    body = Image.new("RGBA", (SIZE, SIZE))
    light = Image.new("RGBA", body.size)
    d, glint = ImageDraw.Draw(body), ImageDraw.Draw(light)
    dark, shadow, mid, bright = "#291e29", "#898189", "#e5e0df", "#ffffff"
    if name == "STAR":
        points = [(round(15.5 + cos(i*pi/5-pi/2)*(15 if i%2==0 else 6.4)),
                   round(15.5 + sin(i*pi/5-pi/2)*(15 if i%2==0 else 6.4))) for i in range(10)]
        d.polygon(points, fill=dark)
        inner = [(round(15.5+(x-15.5)*.83),round(15.5+(y-15.5)*.83)) for x,y in points]
        for i in range(10):
            d.polygon([(15,15),inner[i],inner[(i+1)%10]],
                      fill=(bright,mid,shadow,mid,shadow,mid,shadow,mid,bright,mid)[i])
        glint.line([(15,5),(14,13),(7,13)], fill=(255,247,221,210), width=1)
        glint.polygon([(13,13),(17,13),(15,17)],fill=(255,252,236,175))
    elif name == "CLOUD":
        silhouette = [(1,19),(3,13),(8,12),(11,7),(16,5),(21,7),(24,11),(28,12),
                      (30,17),(30,22),(26,25),(20,25),(17,27),(10,25),(5,25),(2,22)]
        d.polygon(silhouette,fill=dark)
        d.polygon([(2,19),(5,14),(10,14),(12,9),(17,7),(21,9),(24,13),(27,14),
                   (29,18),(28,22),(24,24),(19,23),(16,25),(11,23),(5,23)],fill=shadow)
        d.polygon([(4,18),(6,15),(11,15),(13,10),(17,8),(21,10),(24,15),(27,16),
                   (26,20),(20,20),(17,22),(12,20),(6,21)],fill=mid)
        glint.polygon([(5,17),(11,16),(14,11),(18,9),(21,11),(22,15),(20,18),(13,17),(7,20)],
                      fill=(255,249,242,202))
    elif name == "FEATHER":
        d.polygon([(1,21),(7,13),(18,8),(27,8),(31,11),(28,16),(22,20),(12,22),(2,24)],fill=dark)
        d.polygon([(4,20),(9,13),(19,10),(27,10),(29,12),(23,17),(14,20)],fill=mid)
        d.polygon([(5,21),(15,18),(29,12),(24,17),(20,20),(13,22)],fill=shadow)
        d.line([(3,22),(26,12)],fill=bright,width=1)
        for x in (11,16,21):
            d.line([(x,17),(x+1,13)],fill=shadow)
        glint.line([(13,13),(20,11),(26,11)],fill=(255,250,231,154),width=2)
    elif name == "NOTE":
        d.polygon([(15,2),(20,2),(30,8),(29,14),(20,11),(20,23),(15,28),(7,31),(2,28),
                   (1,23),(5,19),(14,18)],fill=dark)
        d.polygon([(17,4),(20,4),(28,9),(27,11),(19,8),(18,23),(14,26),(7,29),(4,26),
                   (4,23),(8,21),(16,20)],fill=mid)
        d.line([(17,6),(17,20)],fill=bright,width=1)
        d.line([(8,26),(15,24)],fill=shadow,width=2)
        glint.line([(6,23),(10,22)],fill=(255,247,217,196),width=2)
    elif name == "ROCKET":
        d.polygon([(1,6),(8,9),(23,9),(31,15),(31,17),(23,23),(9,23),(1,27)],fill=dark)
        d.polygon([(5,8),(9,11),(22,11),(27,13),(30,16),(22,21),(10,21),(4,24)],fill=mid)
        d.polygon([(5,18),(22,17),(29,16),(22,21),(10,21),(4,24)],fill=shadow)
        d.rectangle((10,10,12,21),fill=bright)
        glint.line([(13,12),(22,12),(26,14)],fill=(255,249,229,209),width=2)
        glint.polygon([(27,13),(30,16),(27,19),(25,18),(25,15)], fill=(255,252,235,225))
    elif name == "WING":
        d.polygon([(1,24),(3,15),(9,6),(19,2),(29,3),(31,8),(28,17),(21,25),(12,30),(5,30)],fill=dark)
        d.polygon([(4,23),(6,15),(11,8),(20,4),(28,5),(28,11),(23,19),(15,26),(7,28)],fill=mid)
        for x,y in ((10,24),(16,20),(22,14)):
            d.line([(x,y),(x+4,y-8)], fill=shadow,width=2)
        glint.line([(9,14),(16,8),(22,6)],fill=(255,246,226,125),width=2)
    elif name == "SHARD":
        d.polygon([(1,26),(7,9),(21,2),(30,11),(25,24),(16,30)],fill=dark)
        d.polygon([(3,25),(9,11),(21,4),(28,11),(23,23),(15,27)],fill=mid)
        d.polygon([(3,25),(12,20),(23,23),(15,27)],fill=shadow)
        d.polygon([(9,11),(21,4),(22,12),(12,20)],fill=bright)
        glint.line([(10,12),(17,8)],fill=(255,251,230,162),width=2)
    elif name == "SWORD":
        d.polygon([(2,13),(7,13),(7,5),(10,5),(12,11),(26,11),(31,16),(25,21),
                   (12,21),(10,26),(7,26),(7,19),(2,19)],fill="#2e2837")
        d.polygon([(10,13),(26,13),(29,16),(25,19),(10,19)],fill="#9eaabd")
        d.polygon([(10,13),(26,13),(29,16),(10,16)],fill="#edf0df")
        d.rectangle((7,7,9,24),fill="#c79756")
        d.rectangle((7,8,7,23),fill="#f7d88c")
        d.rectangle((2,14,6,17),fill="#876458")
        glint.line([(13,13),(23,13)],fill=(255,252,230,192),width=1)
    elif name == "PETAL":
        d.polygon([(1,15),(7,8),(14,6),(23,7),(29,12),(30,17),(23,22),(16,24),(9,23),(3,19)],fill=dark)
        d.polygon([(3,15),(8,10),(15,8),(23,9),(28,13),(28,17),(22,20),(15,22),(9,21)],fill=mid)
        d.polygon([(5,16),(12,18),(20,16),(28,14),(27,18),(22,20),(15,22),(9,21)],fill=shadow)
        glint.line([(9,12),(15,10),(21,11)],fill=(255,242,235,160),width=2)
    elif name == "PILLOW":
        d.polygon([(1,5),(7,7),(24,7),(30,5),(28,13),(28,21),(31,28),(24,26),
                   (8,26),(1,28),(4,20),(4,12)],fill=dark)
        d.polygon([(4,8),(8,9),(24,9),(28,8),(26,14),(26,21),(28,25),(23,24),
                   (9,24),(4,25),(6,20),(6,12)],fill=shadow)
        d.polygon([(7,10),(13,9),(23,10),(26,13),(25,19),(23,22),(9,22),(7,19)],fill=bright)
        glint.line([(9,13),(14,11),(19,11)],fill=(255,249,231,193),width=2)
    elif name == "SHARK_TAIL":
        d.polygon([(2,3),(10,6),(19,12),(29,12),(31,16),(28,20),(19,19),
                   (9,26),(3,28),(7,20),(9,15),(6,10)],fill="#25283a")
        d.polygon([(5,6),(11,8),(20,14),(28,14),(29,16),(20,17),(12,23),(6,25),
                   (10,17),(10,13)],fill="#949aa7")
        d.polygon([(6,7),(12,9),(21,14),(18,16),(12,14)],fill="#e5e2df")
        glint.line([(7,8),(12,11),(17,13)],fill=(218,238,253,150))
    elif name in ("SHARK_CLOSED", "SHARK_OPEN", "SHARK_GAPE"):
        gap = {"SHARK_CLOSED": 0, "SHARK_OPEN": 3, "SHARK_GAPE": 6}[name]
        # A broad back with a dorsal fin, and a jaw that separates towards the nose.
        d.polygon([(0,15),(5,11),(10,10),(12,4),(15,2),(18,10),(23,10),
                   (30,13-gap),(31,16-gap),(28,19-gap),(31,21+gap),
                   (29,25+gap),(22,26),(13,26),(5,23),(0,21)], fill="#282538")
        d.polygon([(1,16),(6,12),(11,11),(14,4),(16,5),(18,11),(23,11),
                   (29,14-gap),(30,16-gap),(26,19-gap),(29,22+gap),
                   (28,24+gap),(22,25),(13,25),(5,22),(1,20)], fill=mid)
        d.polygon([(2,16),(7,13),(14,13),(20,12),(26,12-gap),(29,14-gap),
                   (22,15),(15,16),(9,18),(3,19)], fill=bright)
        d.polygon([(1,20),(8,20),(13,22),(22,22),(28,23+gap),(28,24+gap),
                   (22,25),(13,25),(6,22)], fill=shadow)
        glint.polygon([(6,20),(14,21),(19,21),(25,21+gap),(28,22+gap),
                       (27,24+gap),(22,25),(13,24),(8,23)],fill=(228,238,243,232))
        if gap:
            glint.polygon([(18,18),(24,17-gap),(29,17-gap),(29,21+gap),(25,21+gap),(20,20)],
                          fill="#733c57")
            glint.line([(21,20),(26,20+gap),(28,21+gap)],fill="#cd758b",width=2)
            for x in (21,24,27):
                y = 18 - round(gap * (x - 18) / 9)
                glint.polygon([(x,y-1),(x+2,y-1),(x+1,y+2)],fill="#fff5de")
        else:
            glint.line([(19,19),(23,20),(28,18)],fill="#50354b",width=1)
        eye_y = 14 - gap//2
        glint.ellipse((20,eye_y-2,24,eye_y+2),fill="#2b2b41")
        glint.point((21,eye_y-1),fill="#fff1cd")
        for x in (11,14):
            glint.line([(x,17),(x-1,19)],fill=(53,74,111,180))
        glint.line([(6,14),(10,12),(14,12)],fill=(229,246,251,194))
    else:
        raise ValueError(f"Unknown projectile sprite {name}")
    light.putalpha(ImageChops.multiply(light.getchannel("A"), body.getchannel("A")))
    return body, light


def generate(directory):
    sheet = Image.new("RGBA", (SIZE * len(PARTS), SIZE * 2))
    for i, name in enumerate(PARTS):
        body, glint = part(name)
        sheet.paste(body, (i * SIZE, 0))
        sheet.paste(glint, (i * SIZE, SIZE))
    sheet.save(directory / "projectile_parts.png")
