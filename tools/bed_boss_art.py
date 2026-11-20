"""Under-the-Bed bosses, redrawn from output/imagegen/under-the-bed-jokes-v1.

Draw at 128 px so the domestic jokes survive the boss card and book close-ups.
The compact 64 px parts remain available in the shared atlas; gameplay uses this
module's dedicated sheet. No generated concept images are needed to rebuild it.
"""
from PIL import Image, ImageDraw
import math

PARTS = ("DUST_BUNNY", "BED_MONSTER", "SOCK_NINJA", "YARN_HYDRA", "WIND_UP_GENERAL", "LINT_REGENT")
SIZE = 128
COLUMNS = 3
INK = "#211827"
CREAM = "#f2ead8"


def poly(d, points, color, width=2):
    d.polygon(points, fill=color, outline=INK, width=width)


def stroke(d, points, color, width=3):
    d.line(points, fill=INK, width=width + 4, joint="curve")
    d.line(points, fill=color, width=width, joint="curve")


def button(d, x, y, r, color=CREAM, cross=False):
    d.ellipse((x-r, y-r, x+r, y+r), fill=color, outline=INK, width=2)
    d.arc((x-r+2, y-r+2, x+r-2, y+r-2), 140, 305, fill="#bc879f", width=1)
    if cross:
        for dx in (-2, 2):
            d.line((x+dx, y-2, x-dx, y+2), fill=INK, width=1)
    else:
        for dx in (-2, 2):
            for dy in (-2, 2):
                d.ellipse((x+dx-1, y+dy-1, x+dx, y+dy), fill=INK)


def tuft(d, x, y, r, color, light):
    points = [(x-r, y+2), (x-r+1,y-r//2), (x-r//2,y-r//2),
              (x-r//3,y-r), (x+2,y-r+1), (x+r//2,y-r//2),
              (x+r,y-r//3), (x+r-1,y+3), (x+r//2,y+3),
              (x+r//3,y+r), (x-2,y+r-1), (x-r//2,y+r//2)]
    poly(d, points, color, 1)
    d.line(points[:6], fill=light, width=2)


def dust_bunny(d):
    fur, shade, light, pink = "#817087", "#605368", "#a18b9e", "#bd86a6"
    # Two bent, dusty ears with the cobweb still caught between them.
    for points in ([(43,51),(26,34),(15,12),(19,5),(29,3),(39,10),(46,31),(54,47)],
                   [(66,45),(80,24),(91,14),(102,14),(112,24),(109,33),(101,35),(95,29),(81,51)]):
        poly(d, points, fur)
    poly(d, [(41,40),(27,24),(23,11),(29,10),(36,17),(46,39)], pink, 1)
    poly(d, [(77,42),(91,24),(98,21),(100,27),(86,45)], pink, 1)
    for points in ([(43,16),(56,22),(73,23),(90,18)], [(47,17),(58,31),(81,28)],
                   [(56,21),(59,33),(68,28),(75,31),(77,23)], [(51,23),(65,25),(83,22)]):
        d.line(points, fill=light, width=1)
    poly(d, [(21,70),(16,61),(22,52),(34,48),(39,38),(50,41),(60,35),(73,38),
             (85,47),(96,50),(104,65),(108,83),(104,105),(97,116),(72,120),
             (54,116),(32,120),(20,108)], shade)
    for x,y,r in ((31,63,14),(87,62,15),(50,56,17),(69,49,17),(43,81,19),
                  (74,78,20),(91,92,14),(31,98,15),(61,101,17)):
        tuft(d,x,y,r,fur,light)
    d.ellipse((48,81,77,108), fill=pink)
    # Hanging arms, pink mitts and broad rabbit feet.
    for x in (24,92):
        tuft(d,x,79,13,fur,light)
        tuft(d,x,92,12,fur,light)
        d.rounded_rectangle((x-9,98,x+10,112), radius=6, fill=pink, outline=INK, width=2)
        for off in (-3,3):
            d.line((x+off,105,x+off,111), fill=shade, width=1)
    for x in (36,84):
        d.ellipse((x-18,111,x+17,123), fill=fur, outline=INK, width=2)
        d.ellipse((x-12,115,x+13,123), fill=pink, outline=INK, width=1)
        for off in (-4,3):
            d.line((x+off,118,x+off,122), fill=shade)
    poly(d, [(39,54),(51,58),(57,55),(54,65),(44,65)], INK, 1)
    poly(d, [(66,56),(78,51),(82,57),(76,64),(67,64)], INK, 1)
    for x,y in ((49,60),(74,58)):
        d.rectangle((x,y,x+2,y+2), fill="#f6d081")
    poly(d, [(44,69),(57,72),(74,68),(78,73),(68,81),(53,80)], INK, 1)
    d.rectangle((57,71,62,77), fill=CREAM)
    d.rectangle((64,71,68,77), fill=CREAM)
    button(d,62,67,5,pink,True)
    for points in ([(55,66),(39,63),(29,65),(25,63)], [(53,68),(38,70),(30,75)],
                   [(69,66),(85,61),(94,62)], [(70,69),(87,68),(99,71)]):
        d.line(points, fill=CREAM, width=1)
    for x,y,r in ((11,43,3),(112,57,5),(116,103,3),(12,112,3),(106,44,2),(18,29,2)):
        tuft(d,x,y,r,fur,light)
    d.line([(81,74),(96,76),(111,70)], fill=CREAM, width=2)
    tuft(d,116,69,7,fur,light)


def bed_monster(d):
    body, cloth, shadow, seam = "#815487", "#c3a8d1", "#7e668f", "#f4b3bd"
    # Four hands: two lift the sheet, two poke out of it.
    for pts in ([(20,37),(7,20),(9,9),(16,5),(24,7),(30,16),(26,23),(18,21),(30,38)],
                [(89,42),(98,23),(95,16),(97,12),(104,12),(108,18),(117,17),(123,23),(121,30),(112,36),(104,50)],
                [(23,59),(12,62),(10,73),(16,82),(24,81),(28,75),(23,68),(33,67)],
                [(91,59),(105,54),(117,59),(124,69),(118,80),(111,82),(107,77),(112,73),(103,71),(93,76)]):
        poly(d,pts,body)
    for pts in ([(35,85),(29,100),(14,115),(12,122),(46,122),(49,112),(56,96)],
                [(70,92),(76,114),(83,122),(115,122),(113,115),(100,100),(96,85)]):
        poly(d,pts,body)
    for x in (18,31,42,86,98,109):
        poly(d,[(x,115),(x-4,121),(x+4,121)], "#f4d5b4",1)
    poly(d,[(18,22),(33,31),(43,18),(50,6),(62,3),(75,10),(81,24),(96,34),
            (109,32),(103,46),(98,61),(99,80),(105,95),(95,102),(84,96),
            (74,101),(60,98),(49,103),(37,98),(28,100),(20,92),(29,67),(26,44)],cloth)
    for pts in ([(18,24),(28,36),(36,40),(30,68),(24,88),(36,79),(43,52),(45,29)],
                [(60,42),(50,81),(37,95),(54,88)], [(72,43),(83,72),(94,94),(84,88)],
                [(34,37),(39,32),(35,49)], [(61,65),(68,88),(77,96),(66,87)],
                [(90,36),(95,40),(91,59),(86,48)]):
        d.polygon(pts,fill=shadow)
    for x,y in ((25,36),(23,42),(22,49),(27,89),(33,95),(40,96),(48,98),(58,96),
                (69,97),(79,96),(89,94),(98,96),(61,9),(61,15),(62,21),
                (16,66),(22,64),(108,59),(113,62)):
        d.line((x-2,y-1,x+2,y+1),fill=seam,width=2)
    # Eye holes, and the embarrassed little tongue poking through a mended tear.
    for pts in ([(48,30),(54,22),(58,31),(57,44),(50,47),(46,42)],
                [(63,30),(67,23),(73,31),(76,42),(70,46),(63,42)]):
        poly(d,pts,INK,1)
    d.ellipse((50,32,56,42), fill=CREAM)
    d.ellipse((65,32,71,41), fill=CREAM)
    d.rectangle((54,35,55,37),fill="#d3a151")
    d.rectangle((66,34,67,36),fill="#d3a151")
    poly(d,[(75,48),(79,44),(83,48),(80,53),(83,56),(78,60),(73,56)],INK,1)
    poly(d,[(77,53),(82,53),(84,61),(80,59)],"#f4d5b4",1)
    d.line([(74,47),(78,44),(83,47),(81,51)],fill=seam,width=2)


def sock_ninja(d):
    purple, light, stripe, dark = "#b689e4", "#d2aff6", "#f4dfc1", "#52426e"
    # Crossed knitting needles, complete with round wooden stops.
    for a,b in (((26,18),(94,91)),((93,18),(29,93))):
        stroke(d,[a,b],"#cf9452",5)
        d.ellipse((a[0]-5,a[1]-6,a[0]+5,a[1]+4),fill="#eebd70",outline=INK,width=2)
    poly(d,[(64,28),(87,25),(102,20),(120,23),(117,35),(104,33),(92,38),(78,36)],purple)
    poly(d,[(88,34),(103,41),(119,42),(116,51),(103,47),(84,42)],purple)
    # Dynamic stance with the sock toe forming the bent front leg.
    poly(d,[(52,70),(66,72),(80,87),(87,104),(98,113),(96,122),(76,123),(70,114),
            (61,100),(48,100),(39,111),(36,122),(16,122),(17,113),(29,96),(40,82)],purple)
    for pts in ([(48,79),(57,85),(38,104),(29,104)],[(64,86),(72,85),(82,103),(73,107)],
                [(18,115),(34,114),(34,121),(18,121)],[(80,113),(92,114),(95,121),(84,121)]):
        d.polygon(pts,fill=stripe)
    poly(d,[(43,36),(41,25),(48,16),(64,7),(76,4),(84,11),(81,24),(74,36),
            (73,54),(67,63),(72,80),(61,90),(43,83),(41,63),(36,52)],purple)
    for pts in ([(44,23),(68,12),(80,14),(78,22),(43,34)],
                [(40,45),(74,43),(70,51),(42,55)],[(43,66),(66,61),(69,69),(45,78)]):
        d.polygon(pts,fill=stripe)
    for x in range(46,74,4):
        d.line((x,26,x-2,34),fill=light)
        d.line((x,55,x+1,62),fill=light)
    # A real toe through the worn-out sock tip.
    d.ellipse((67,6,84,23),fill=INK)
    d.ellipse((70,7,82,21),fill="#efa785")
    d.arc((71,9,80,17),180,355,fill=stripe,width=2)
    poly(d,[(38,32),(76,26),(79,35),(41,43)],dark)
    button(d,60,33,5,"#edc35c",True)
    poly(d,[(43,43),(56,46),(54,51),(45,49)],INK,1)
    poly(d,[(60,45),(72,40),(70,47),(61,50)],INK,1)
    d.line((46,45,52,47),fill=stripe,width=2)
    d.line((63,46,69,43),fill=stripe,width=2)
    for points in ([(45,56),(65,80)],[(65,55),(45,79)]):
        stroke(d,points,dark,4)
    # Peg and cat's cradle, held wide so the crossed strings remain visible.
    stroke(d,[(44,59),(28,62),(18,63)],purple,7)
    stroke(d,[(70,57),(87,57),(94,62)],stripe,7)
    poly(d,[(10,50),(16,46),(24,51),(19,67),(13,73),(7,70)],"#dbab6e")
    d.line((17,50,12,68),fill=CREAM,width=2)
    button(d,16,56,3,"#aaa5ad",True)
    for points in ([(20,57),(90,65),(84,56),(20,64),(90,65)],
                   [(20,57),(39,71),(71,70),(90,58)]):
        d.line(points,fill=INK,width=3)
        d.line(points,fill="#d9a6ff",width=1)
    for x,y in ((22,110),(78,110)):
        poly(d,[(x-7,y-7),(x+10,y-3),(x+8,y+7),(x-10,y+5)],dark,1)
        for off in (0,4):
            d.line((x-6,y-4+off,x+7,y+off),fill=purple)
    for x,y in ((64,91),(67,94),(69,98),(71,101)):
        d.line((x-2,y+2,x+2,y-2),fill=dark)


def braid(d, points):
    stroke(d,points,"#b73d6a",10)
    for (x1,y1),(x2,y2) in zip(points,points[1:]):
        steps = max(1,round(math.hypot(x2-x1,y2-y1)/5))
        for i in range(steps):
            x,y = x1+(x2-x1)*i/steps, y1+(y2-y1)*i/steps
            d.line((round(x-4),round(y-3),round(x+3),round(y+2)),fill="#f478a4",width=3)


def yarn_head(d,x,y,r,mouth):
    d.ellipse((x-r,y-r,x+r,y+r),fill="#db5180",outline=INK,width=2)
    for off in (-8,-2,5):
        d.arc((x-r+2+off//3,y-r+3,x+r-2,y+r-1+off),185,340,fill="#ff8db4",width=2)
        d.arc((x-r+3,y-r+1,x+r+off//3,y+r-3),15,160,fill="#9f325a",width=1)
    button(d,x-6,y-1,4,"#f2b9cf")
    button(d,x+6,y-3,4,"#b73d6a",True)
    d.line((x-11,y-8,x-2,y-5),fill=INK,width=2)
    d.line((x+1,y-7,x+11,y-9),fill=INK,width=2)
    d.ellipse((x-5,y+5,x+6,y+12),fill=INK)
    if mouth:
        d.ellipse((x,y+8,x+4,y+15),fill="#ff8db4",outline=INK)
    else:
        d.polygon([(x-3,y+5),(x,y+5),(x-1,y+8)],fill=CREAM)
    stroke(d,[(x-r+2,y-9),(x-r-1,y-16),(x-5,y-r-2),(x,y-r)],"#db5180",2)


def yarn_hydra(d):
    poly(d,[(16,97),(25,85),(47,78),(71,80),(95,91),(111,108),(105,120),
            (82,123),(68,118),(47,123),(21,121),(11,114)],"#b73d6a")
    for x,y in ((28,105),(49,101),(71,105),(89,109)):
        d.arc((x-16,y-14,x+16,y+14),190,345,fill="#ed6a96",width=4)
        d.arc((x-15,y-12,x+14,y+16),5,160,fill="#ed6a96",width=3)
    braid(d,[(42,94),(26,77),(20,59),(23,39)])
    braid(d,[(64,96),(62,81),(59,68)])
    braid(d,[(78,98),(90,80),(95,56),(101,40)])
    braid(d,[(98,113),(116,104),(116,88),(112,76)])
    stroke(d,[(112,79),(115,66),(121,60),(124,61),(125,65),(121,69)],"#e8b260",3)
    # Granny-square blanket: cream borders and pink flowers, not reptile scales.
    for x,y in ((30,91),(48,87),(68,90),(88,96)):
        poly(d,[(x,y-10),(x+13,y-4),(x+10,y+9),(x-3,y+7),(x-10,y)],CREAM,1)
        for dx,dy in ((-4,0),(1,-4),(5,1),(0,5)):
            d.ellipse((x+dx-3,y+dy-3,x+dx+3,y+dy+3),fill="#b73d6a")
        d.ellipse((x-2,y-2,x+2,y+2),fill="#ff8db4")
    for x,y in ((29,117),(59,119),(91,118)):
        d.ellipse((x-12,y-7,x+12,y+6),fill="#db5180",outline=INK,width=2)
        for off in (-5,2):
            d.arc((x+off-3,y-4,x+off+4,y+7),190,310,fill="#ff8db4",width=2)
    yarn_head(d,23,25,19,False)
    yarn_head(d,59,57,17,True)
    yarn_head(d,102,29,19,True)
    # The heads have pulled one another's yarn taut across their faces.
    stroke(d,[(31,36),(93,46)],"#f2b9cf",2)
    for y in (51,57,63):
        d.line((89,y,101,y+3),fill="#f2b9cf",width=2)
    for x in (54,66):
        stroke(d,[(x,42),(x+(3 if x>60 else -3),27)],"#d5a252",2)
        d.ellipse((x-5 if x<60 else x+1,24,x-1 if x<60 else x+5,29),fill="#edc66e",outline=INK)


def wind_up_general(d):
    red, gold, wood = "#923e49", "#f4c65e", "#d6b58a"
    # Oversized winding key, not caterpillar tracks or a tank barrel.
    stroke(d,[(24,38),(46,47)],gold,6)
    for y in (26,51):
        d.ellipse((5,y-12,29,y+12),fill=gold,outline=INK,width=2)
        d.ellipse((12,y-5,22,y+5),fill=INK)
    d.rectangle((14,25,21,51),fill=gold)
    # One planted foot, the other kicked out in a self-important march.
    poly(d,[(45,95),(62,99),(54,113),(54,119),(34,123),(27,121),(29,116)],wood)
    poly(d,[(73,94),(87,94),(95,106),(105,98),(115,100),(117,109),(106,118),(94,119),(82,109)],wood)
    poly(d,[(32,112),(51,113),(53,121),(28,123),(25,120)],"#51404a")
    poly(d,[(99,104),(109,92),(116,94),(119,103),(108,116),(101,116)],"#51404a")
    d.line((29,120,51,119),fill=gold,width=2)
    d.line((105,111,116,98),fill=gold,width=2)
    poly(d,[(40,54),(50,49),(75,49),(90,57),(96,94),(83,107),(64,104),(47,108),(30,104)],red)
    d.line((35,101,52,97,61,101),fill=gold,width=2)
    stroke(d,[(41,62),(26,79),(33,89)],red,11)
    stroke(d,[(85,60),(100,62),(109,48)],red,10)
    d.ellipse((25,83,40,95),fill=wood,outline=INK,width=2)
    poly(d,[(102,50),(103,41),(109,43),(112,39),(116,41),(120,37),(124,40),(119,47),(111,52)],wood)
    d.ellipse((102,21,124,43),fill=gold,outline=INK,width=2)
    d.ellipse((105,24,118,35),fill="#ffdc8a")
    # Round wooden face, button eye, pocket-watch monocle and enormous moustache.
    d.ellipse((39,17,85,57),fill=wood,outline=INK,width=2)
    d.line((62,19,66,54),fill="#9d795e",width=1)
    for x,y in ((45,30),(48,44),(72,47),(76,28)):
        d.ellipse((x,y,x+1,y+1),fill=INK)
    button(d,53,31,6)
    d.ellipse((69,23,84,38),fill=gold,outline=INK,width=2)
    d.ellipse((71,25,82,36),fill=CREAM)
    d.line((76,26,77,31,81,28),fill=INK)
    d.line((84,33,88,42),fill=gold,width=2)
    poly(d,[(65,32),(61,38),(68,39)],red,1)
    poly(d,[(62,39),(55,38),(48,42),(39,39),(42,46),(52,48),(62,43),
            (70,44),(78,46),(87,40),(84,49),(76,51),(67,46)],"#aaa0aa",1)
    d.line((44,43,53,44,60,40),fill="#d1c5c8",width=2)
    # Red peaked cap.
    poly(d,[(33,28),(36,16),(54,5),(69,3),(77,8),(77,18),(86,20),(83,26),(57,24),(41,33)],red)
    d.line((39,25,60,15,76,14),fill=gold,width=3)
    poly(d,[(45,29),(59,21),(78,18),(87,21),(85,26),(65,25)],"#51404a",1)
    star(d,66,10,4,gold)
    # Thimble epaulettes and an absurd wall of medals.
    for x in (42,86):
        d.rounded_rectangle((x-7,52,x+7,61),radius=4,fill=gold,outline=INK,width=1)
        for dx in (-3,1,4):
            d.point((x+dx,55),fill=INK)
            d.line((x+dx,61,x+dx,66),fill=gold,width=2)
    for x,y,col in ((54,61,red),(70,63,"#61838c"),(59,74,gold),(80,72,red)):
        d.rectangle((x-3,y+3,x+3,y+11),fill=CREAM)
        d.ellipse((x-6,y-6,x+6,y+6),fill=gold,outline=INK,width=1)
        d.ellipse((x-4,y-4,x+4,y+4),fill=col)
        if col==red: star(d,x,y,3,CREAM)
    # Open belly reveals gears and a coiled mainspring.
    d.ellipse((54,81,90,105),fill=wood,outline=INK,width=2)
    d.ellipse((58,84,86,102),fill=INK)
    for x,y,r in ((65,91,6),(71,99,4)):
        star(d,x,y,r,gold,8)
        d.ellipse((x-2,y-2,x+2,y+2),fill=INK)
    for r in (3,6,9):
        d.arc((79-r,93-r,79+r,93+r),15,335,fill="#ac9072",width=1)


def star(d,x,y,r,color,tips=5):
    points=[]
    for i in range(tips*2):
        a=-math.pi/2+i*math.pi/tips
        reach=r if i%2==0 else r*.48
        points.append((round(x+math.cos(a)*reach),round(y+math.sin(a)*reach)))
    d.polygon(points,fill=color)


def lint_regent(d):
    lint, dark, light, pink, gold = "#8a8494", "#6a6478", "#a99db3", "#f493b0", "#cfad73"
    # The entire kingdom is a shallow dryer filter, with mesh and pull handle.
    poly(d,[(13,109),(111,109),(124,119),(120,126),(8,126),(4,119)],CREAM)
    poly(d,[(17,110),(108,110),(116,119),(11,119)],dark,1)
    for x in range(12,114,6):
        d.line((x,111,x+8,118),fill=CREAM)
        d.line((x,118,x+8,111),fill=CREAM)
    d.rectangle((48,120,78,125),fill=light,outline=INK)
    d.rectangle((54,120,72,122),fill=INK)
    poly(d,[(32,53),(53,43),(77,48),(93,63),(98,89),(110,108),(98,114),
            (22,114),(12,108),(22,83)],dark)
    # Structured overlapping lint curls, including the ermine-like robe edges.
    for row in range(5):
        for col in range(4):
            x=35+col*16+(row%2)*3
            y=63+row*10
            tuft(d,x,y,10,lint,light)
    poly(d,[(28,57),(40,51),(44,61),(37,80),(28,91),(23,108),(12,111),
            (19,93),(19,77)],"#c9b1ca",1)
    poly(d,[(88,55),(98,62),(97,77),(107,97),(113,110),(99,111),
            (88,95),(83,71)],"#c9b1ca",1)
    for x,y in ((29,62),(28,76),(23,94),(20,104),(95,68),(99,87),(105,102)):
        d.ellipse((x-2,y-2,x+2,y+2),fill=INK)
    # Button-topped sceptre, and the lazy palm waiting for its button tax.
    stroke(d,[(24,102),(14,34)],gold,3)
    tuft(d,13,31,12,lint,light)
    button(d,13,31,7)
    tuft(d,23,72,9,lint,light)
    d.line((18,69,26,67),fill=dark,width=2)
    stroke(d,[(92,65),(104,61),(112,54)],lint,7)
    poly(d,[(105,62),(103,53),(106,51),(109,56),(113,54),(119,49),(123,49),
            (122,53),(116,59),(109,63)],lint,1)
    # Long monobrow and beard are all lint rather than a metal king's face.
    tuft(d,64,43,23,lint,light)
    poly(d,[(47,42),(59,44),(57,49),(49,48)],INK,1)
    poly(d,[(65,44),(77,40),(75,47),(67,49)],INK,1)
    d.line((50,44,56,45),fill=CREAM,width=2)
    d.line((68,45,74,43),fill=CREAM,width=2)
    for pts in ([(40,41),(48,36),(57,38),(62,42),(56,43),(47,40),(42,44)],
                [(64,41),(71,36),(81,35),(83,39),(72,40),(67,44)],
                [(56,53),(47,57),(40,53),(42,60),(53,61),(61,55)],
                [(61,52),(75,55),(83,50),(81,58),(69,61),(65,57)],
                [(57,56),(72,59),(76,65),(70,76),(64,82),(66,70),(59,73),(53,68)]):
        poly(d,pts,light,1)
    # Bent lint crown, white shirt-button jewel and a stray pink thread.
    poly(d,[(41,33),(36,24),(39,20),(48,24),(54,13),(47,11),(41,15),(32,13),
            (27,19),(26,12),(33,7),(44,5),(53,8),(62,7),(68,13),(74,10),(80,16),
            (78,31),(69,34)],dark)
    d.line([(38,23),(44,26),(51,17),(63,22),(73,15),(75,29),(42,33)],fill=gold,width=3)
    button(d,63,23,6)
    d.line([(49,7),(45,13),(45,19)],fill=pink,width=2)
    # Wax-pink sash, a belt pocket full of button coins and floating tax payments.
    poly(d,[(41,67),(46,64),(91,83),(87,88),(60,78)],pink,1)
    button(d,46,70,5)
    poly(d,[(43,73),(48,77),(43,87),(40,83),(36,86)],pink,1)
    d.line((44,91,79,91),fill=dark,width=4)
    for x,y in ((78,88),(86,86),(92,90)):
        button(d,x,y,4,"#e4c88f")
    poly(d,[(74,89),(97,90),(94,103),(78,104)],light,1)
    d.line((76,92,85,95,95,92),fill=dark,width=2)
    button(d,87,98,3,"#e4c88f")
    for x,y in ((100,23),(112,14),(119,33)):
        button(d,x,y,6,pink)
    for pts,col in (([(35,91),(30,99),(33,103)],pink), ([(58,93),(61,99),(56,104)],"#6ab0b8"),
                    ([(83,109),(88,106),(91,111)],pink), ([(37,49),(33,45),(35,39)],"#6ab0b8")):
        d.line(pts,fill=col,width=2)
    for x in (43,82):
        d.ellipse((x-11,107,x+10,116),fill=lint,outline=INK,width=1)
        d.line((x,110,x-2,115),fill=dark)


def portrait(name):
    image = Image.new("RGBA", (SIZE, SIZE))
    paint = (dust_bunny, bed_monster, sock_ninja, yarn_hydra, wind_up_general, lint_regent)[PARTS.index(name)]
    paint(ImageDraw.Draw(image))
    return image


def part(name):
    return portrait(name).resize((64,64),Image.Resampling.NEAREST)


def generate(directory):
    rows = (len(PARTS)+COLUMNS-1)//COLUMNS
    sheet = Image.new("RGBA", (COLUMNS*SIZE, rows*SIZE*2))
    for index,name in enumerate(PARTS):
        cell=portrait(name)
        x,y=index%COLUMNS*SIZE,index//COLUMNS*SIZE
        sheet.paste(cell,(x,y))
        flash=Image.new("RGBA",cell.size,"white")
        flash.putalpha(cell.getchannel("A"))
        sheet.paste(flash,(x,y+rows*SIZE))
    sheet.save(directory / "boss_under_bed.png")
