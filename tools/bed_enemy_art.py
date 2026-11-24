"""Under-bed residents, in the cast's 48px, six-pose, top-left-lit pixel style.

Materials are drawn as deliberate colour clusters: loose dust, lacquered tin and
brass, swirled glass, end-grain wood and steel, quilted cloth, and chipped ivory.
The caller supplies enemy_art.Art so outlines and damage silhouettes stay shared.
"""
INK = '#292631'


def dust(a, pose, bunny=False):
    ready = pose == 5
    step = (0, 1, 0, -1, 0, 0)[pose]
    # The little kit shares the boss's grey-lilac dust, with tangled lint in its ears.
    if bunny:
        for x, tilt in ((15, -2), (28, 2)):
            a.poly([(x, 22), (x+tilt-2, 7), (x+tilt, 3), (x+tilt+5, 5), (x+7, 23)], '#72697e')
            a.poly([(x+1, 19), (x+tilt, 8), (x+tilt+2, 5), (x+tilt+4, 7), (x+5, 20)], '#c9bfd1')
            a.line([(x+3, 9), (x+4, 18)], '#a38d9e', 2)
            a.box(x+tilt, 5, 2, 2, '#eee2df')
        for x, y in ((10, 35+step), (29, 35-step)):
            a.oval(x, y, 10, 6, '#716779'); a.oval(x+1, y, 7, 3, '#b5a8bd')
    else:
        for side in (-1, 1):
            for i in range(3):
                x, y = 24+side*10, 25+i*5
                tip = x+side*(7+(i % 2))
                a.line([(x, y), (tip, y-2+step*side), (tip+side*2, y+1)], '#807384', 2)
                a.line([(x, y-1), (tip, y-3+step*side)], '#c6b2c4')
    for x,y,w,h in ((9,23,13,14),(14,14,17,20),(26,19,13,17),(17,30,17,12)):
        a.oval(x,y,w,h,'#7c7088')
    a.oval(11,19,27,21,'#aaa0b6'); a.oval(12,17,22,17,'#c9becf')
    a.oval(14,17,14,9,'#e5d9df')
    # Tufts interrupt the silhouette; sparse fibre marks follow the body curvature.
    for points in ([(9,25),(6,23),(10,21)],[(14,18),(13,13),(18,17)],
                   [(28,18),(31,14),(32,20)],[(36,28),(41,27),(37,32)],
                   [(16,38),(14,42),(21,39)]):
        a.poly(points, '#aaa0b6')
    for points in ([(13,28),(15,25),(18,24)],[(29,34),(32,32),(33,29)],
                   [(19,36),(23,37),(26,36)]):
        a.line(points, '#8c7e9a')
    for x,y in ((16,21),(20,18),(30,23),(12,30)):
        a.line([(x,y),(x+2,y-1)], '#f0e4e5')
    for x in (17,29):
        if ready:
            a.line([(x,26),(x+3,28),(x,29)], '#514558')
        else:
            a.oval(x-1,25,5,6,'#f2e6de'); a.box(x+1,26,2,4,INK)
            a.box(x,25,1,1,'#fff9e9')
    # A round, rosy nose makes the sneeze readable even at gameplay size.
    a.oval(21,28,9 if ready else 6,7 if ready else 5,'#a87d91')
    a.oval(22,28,6 if ready else 4,4 if ready else 3,'#e1abb1')
    a.box(23,28,2,1,'#ffe0d0')
    a.oval(23,35,4,4,'#584956') if ready else a.line([(23,34),(25,35),(27,34)], '#65566b')
    if bunny:
        a.oval(5,29,9,9,'#8f829c');a.oval(6,29,6,5,'#d5c8d6')
        a.line([(32,35),(38,34),(40,36)],'#d9c9d0')


def beetle(a, pose):
    step = (0, 2, 0, -2, 0, 0)[pose]
    ready = pose == 5
    # Jointed brass legs remain separate from the enamel body.
    for i, (x,y) in enumerate(((13,25),(21,26),(29,27))):
        swing = step if i % 2 == 0 else -step
        for side in (-1,1):
            end = y+side*11
            a.line([(x,y),(x-3+swing,end),(x-7+swing,end+side*2)], '#765845', 3)
            a.line([(x,y-1),(x-3+swing,end-1),(x-6+swing,end+side)], '#d2ad68')
            a.oval(x-4+swing,end-2,3,3,'#f5cf82')
    a.oval(4,17,26,18,'#243e47'); a.oval(5,17,23,14,'#417b78')
    a.oval(6,17,19,9,'#70a698'); a.oval(7,18,11,4,'#b7d4b0')
    a.poly([(6,27),(15,29),(27,26),(25,32),(13,34),(6,31)],'#31575c')
    a.line([(5,25),(13,23),(21,23),(28,26)],'#243c45',2)
    a.line([(7,24),(14,22),(22,23)],'#91b8a4')
    for x,y in ((9,28),(19,30),(23,20)):
        a.oval(x,y,3,3,'#b8a574');a.box(x,y,1,1,'#ffe5a7')
    # A worn paint chip reveals the tin under the shell, rather than random noise.
    a.poly([(9,20),(12,20),(13,22),(10,23)],'#91a8aa')
    a.line([(12,20),(14,20)],'#d9ddd0')
    a.oval(25,20,10,14,'#2b4148');a.oval(26,20,7,9,'#668578')
    a.line([(27,22),(31,21)],'#b7c4a0')
    a.oval(31,22,9,10,'#33434a');a.oval(32,22,6,6,'#6b8274')
    for y, side in ((23,-1),(29,1)):
        a.line([(37,y),(41,y+side*3),(45,y+side*3),(46,y+side)],'#8e6d49',3)
        a.line([(38,y),(41,y+side*3),(44,y+side*3)],'#ecd196')
    a.box(35,23,2,2,'#fff0af' if ready else '#e4af61')
    a.line([(33,22),(35,17),(39,15)],'#adc4a1')
    # The key changes silhouette between all four running poses.
    a.line([(16,20),(17,14),(17,11)],'#8a6746',3)
    a.line([(16,19),(16,13)],'#ffe0a0')
    width = (6,4,2,4,6,7)[pose]
    for side in (-1,1):
        x = 17+side*(width//2)-width//2
        a.oval(x,6,width+2,7,'#b88643')
        a.oval(x,6,width+1,4,'#efc76f')
        if width > 3:a.box(x+2,8,width-2,3,INK)
    a.box(16,7,3,4,'#e8b75a');a.box(16,6,3,1,'#ffe9a7')


def marble(a, pose):
    ready=pose==5
    a.oval(8,8,33,33,'#315967')
    a.oval(9,8,30,30,'#4c9da8');a.oval(10,9,25,24,'#83d5cf')
    a.poly([(11,24),(14,33),(23,38),(33,34),(37,25),(34,30),(25,32),(17,29)],'#39818e')
    a.line([(11,25),(14,32),(23,36),(32,33)],'#b0e5d4',2)
    # The coloured ribbon bends inside the glass and rotates as the marble rolls.
    shift=(0,2,4,2,0,0)[pose]
    a.poly([(16+shift,11),(23+shift,12),(29,19),(24,25),(22,31),(29,35),
            (21,34),(17,28),(21,21),(22,17)],'#536b9d')
    a.line([(18+shift,12),(25,18),(22,24),(20,29),(25,33)],'#a9aed8',2)
    a.line([(17+shift,12),(23,18)],'#e5dce7')
    a.oval(12,11,12,8,'#d3f2df');a.oval(13,12,7,4,'#fff8df')
    a.box(12,20,2,4,'#d9f5df');a.box(32,17,2,2,'#d8f6e1')
    a.oval(28,27,5,5,'#b2e8d6');a.box(29,28,2,2,'#f3ffe8')
    # Eyes are suspended dark glass inclusions, with light caught on their upper rims.
    for x,y in ((19,23),(29,22)):
        a.oval(x-1,y-1,5,6,'#acdcd0');a.oval(x,y,3,4,'#2d5263');a.box(x,y,1,1,'#efffe8')
    if ready:
        for x,y in ((9,17),(36,12)):
            a.line([(x-2,y),(x+2,y)],'#e5ffed');a.line([(x,y-2),(x,y+2)],'#e5ffed')


def croc(a, pose):
    ready=pose==5
    step=(0,1,0,-1,0,0)[pose]
    gape=5 if ready else 1
    for x in (11,29):
        a.poly([(x,30),(x+5,30),(x+5+step,37),(x+7+step,39),(x-2+step,39),(x+step,36)],'#765444')
        a.line([(x+1,32),(x+2+step,37),(x+5+step,38)],'#d6ac75')
    # Thick wooden halves have separate lit top, end grain and darker undersides.
    a.poly([(3,18),(9,15),(22,20),(42,16-gape),(46,19-gape),(44,23-gape),
            (25,28),(12,24),(3,23)],'#996346')
    a.poly([(3,18),(9,15),(22,20),(42,16-gape),(45,19-gape),(24,24),(12,21)],'#dcad70')
    a.line([(5,18),(10,17),(21,22),(40,18-gape)],'#ffe0a0',2)
    a.poly([(3,30),(15,24),(24,24),(45,28+gape),(44,33+gape),(22,30),(9,34),(3,34)],'#8e5d43')
    a.poly([(4,29),(15,24),(23,25),(44,28+gape),(42,31+gape),(22,28),(9,32),(4,32)],'#c08b59')
    a.line([(24,27),(40,30+gape)],'#f1c88a')
    for x,y in ((7,20),(11,21),(27,22-gape),(34,20-gape),(28,29+gape)):
        a.line([(x,y),(x+4,y-1)],'#b27d50')
    a.line([(6,30),(10,29),(12,30)],'#8b5b42')
    a.box(4,19,2,3,'#edca90');a.box(4,30,2,2,'#e4b980')
    for x in (30,36,41):
        a.poly([(x,23-gape),(x+3,22-gape),(x+1,27-gape)],'#eee0b9')
        a.box(x,23-gape,2,1,'#fff4d7')
        if ready:a.poly([(x,28+gape),(x+2,29+gape),(x+1,26+gape)],'#e0ceaa')
    # Steel spring with inner hole, three coil turns, and long arms along both jaws.
    a.line([(11,20),(19,25),(30,30+gape)],'#4d525d',3)
    a.line([(11,19),(19,24),(30,29+gape)],'#c2c7bf')
    a.oval(15,20,11,11,'#565b66');a.oval(16,20,9,9,'#b9c3c0')
    a.oval(18,22,5,5,'#555662');a.oval(19,23,3,3,'#d5b17d')
    a.line([(17,21),(21,21),(23,23)],'#f0eee0')
    for x in (16,19,22):a.line([(x,27),(x+1,29)],'#7e8991')
    a.oval(30,13-gape,7,7,'#8c634a');a.oval(30,12-gape,6,6,'#fff0cf')
    a.box(33,13-gape,2,3,INK);a.box(33,13-gape,1,1,'#fffdf0')
    a.box(42,19-gape,2,1,'#634c42')


def moth(a, pose):
    ready=pose==5
    flap=(-1,1,3,1,0,-3)[pose]
    for side in (-1,1):
        def p(x,y):return (24+side*x,y)
        # Each wing has a rolled seam, padded panel and inset patch of contrasting fabric.
        a.poly([p(2,23),p(9,12+flap),p(19,7+flap),p(22,10+flap),p(21,22),p(13,31),p(5,30)],'#79607f')
        a.poly([p(3,22),p(10,13+flap),p(19,9+flap),p(20,11+flap),p(19,21),p(12,28),p(5,28)],'#bd93b7')
        a.poly([p(6,21),p(11,15+flap),p(17,12+flap),p(15,20),p(10,25)],'#e7bace')
        a.line([p(4,22),p(10,13+flap),p(18,10+flap)],'#f4d9d4')
        a.poly([p(4,27),p(13,25),p(18,32-flap),p(14,40-flap),p(7,36)],'#507f87')
        a.poly([p(5,28),p(12,27),p(16,33-flap),p(13,37-flap),p(8,34)],'#8cbab4')
        a.poly([p(6,28),p(11,28),p(13,32-flap),p(9,32)],'#c2d9bc')
        a.line([p(6,29),p(13,36-flap)],'#eee0b6')
        # Tiny stitches sit on seams, never a noisy texture over the whole panel.
        for x,y in ((10,16+flap),(12,20),(9,24),(10,32),(13,34-flap)):
            a.line([p(x,y),p(x+1,y+1)],'#79596f')
        for x,y in ((18,13+flap),(17,17),(15,22)):
            a.line([p(x,y),p(x+1,y+1)],'#f7dfd2')
        for x in (9,12,15):
            a.line([p(x,36-flap),p(x+1,39-flap)],'#b4c7b3')
        a.line([p(1,19),p(4,12),p(7,10)],'#8f7a70',2)
        a.line([p(1,18),p(4,11),p(7,9)],'#f2dfb7')
        a.oval(*p(7,8),2,2,'#ffe6b6')
    a.oval(20,20,9,17,'#8b6e72');a.oval(20,20,7,14,'#c2a18b')
    for y in (28,32):a.line([(22,y),(26,y+1)],'#e3c69f')
    a.oval(19,17,11,11,'#d5ba96');a.oval(20,17,8,7,'#f5dfb8')
    for x in (20,27):
        a.oval(x,20,3,5,'#5c4e60');a.box(x,20,1,1,'#fff8de')
    a.line([(23,26),(25,27),(26,26)],'#8b6e72')


def die(a, pose):
    ready=pose==5
    step=(0,1,0,-1,0,0)[pose]
    for x in (12,29):
        a.oval(x,38+step,9,6,'#514652');a.line([(x+2,39+step),(x+5,39+step)],'#a59ba3')
    # Three planes, bevelled rims, recessed pips and chips reveal a solid old ivory toy.
    a.poly([(8,14),(21,7),(39,13),(39,33),(27,40),(8,34)],'#65958e')
    a.poly([(8,14),(21,7),(39,13),(27,20)],'#eee9cd')
    a.poly([(9,15),(26,20),(26,38),(9,33)],'#bed5b8')
    a.poly([(28,21),(38,15),(38,32),(28,38)],'#7fb3a4')
    a.line([(10,15),(25,21),(25,36)],'#e7ead0')
    a.line([(10,16),(10,32),(24,36)],'#e3e1bb',2)
    a.line([(12,14),(21,9),(35,13)],'#fff4d8')
    a.line([(29,23),(36,19)],'#9bc8b3')
    for x,y in ((13,20),(20,23),(13,28),(20,31),(30,23),(34,28),(22,12)):
        a.oval(x,y,4,4,'#dce2c3');a.oval(x,y,3,3,'#3e6867');a.box(x,y,2,1,'#2c4d55')
    a.poly([(9,28),(11,29),(11,32),(9,32)],'#8aa69a')
    a.poly([(33,12),(35,13),(34,15),(31,15)],'#b3bba3')
    a.line([(34,13),(35,13)],'#fff1ca')
    a.line([(29,34),(31,32),(32,32)],'#5a887f')
    # A face on the front keeps it a character rather than an ordinary floor prop.
    for x,y in ((13,24),(20,26)):
        a.box(x,y,3,3,INK);a.box(x,y,1,1,'#e8edce')
    a.line([(15,32),(17,34),(20,34)],'#56716b')
    if ready:
        a.line([(9,16),(25,21)],'#ffdc86',2)
        a.box(22,11,4,2,'#fff2b1');a.box(23,10,2,4,'#ffda72')


def draw(a, kind, pose):
    if kind=='DustMite':dust(a,pose)
    elif kind=='DustBunnyKit':dust(a,pose,True)
    elif kind=='WindUpBeetle':beetle(a,pose)
    elif kind=='LostMarble':marble(a,pose)
    elif kind=='PegCroc':croc(a,pose)
    elif kind=='QuiltMoth':moth(a,pose)
    elif kind=='TumbleDie':die(a,pose)
    else:raise ValueError(kind)
