"""Authored cast sprites. Stable colour clusters, one-pixel edges, no random texture.

Six poses: four steps, idle and attack preparation. Raster source remains generated.
"""
import math
import os
from PIL import Image, ImageDraw, ImageColor

CAST_SHEETS = ('ShellCop', 'RiotCop', 'Sniper', 'Robber', 'Sludger', 'Agent',
            'Dynamite', 'Hydrant', 'GhostKin', 'RoboCart', 'Android', 'Dandelion')
INK = '#292631'

class Art:
    def __init__(self):
        self.image = Image.new('RGBA', (48,48))
        self.d = ImageDraw.Draw(self.image)
    def box(self,x,y,w,h,c): self.d.rectangle((x,y,x+w-1,y+h-1),fill=c)
    def oval(self,x,y,w,h,c): self.d.ellipse((x,y,x+w-1,y+h-1),fill=c)
    def poly(self,p,c): self.d.polygon(p,fill=c)
    def line(self,p,c,w=1): self.d.line(p,fill=c,width=w)
    def eyes(self,y=24):
        for x in (20,29):
            self.box(x-1,y-1,6,8,'#735141');self.box(x,y,4,6,'#fff0cf');self.box(x+2,y+1,2,5,INK)
    def feet(self,frame):
        for x,step in ((15,frame in (0,1)),(28,frame in (2,3))):
            self.box(x,42-int(step),7,4,INK);self.box(x+1,42-int(step),5,1,'#67616a')
    def edge(self):
        alpha=self.image.getchannel('A'); p=alpha.load(); out=Image.new('RGBA',(48,48)); op=out.load()
        for y in range(48):
            for x in range(48):
                if not p[x,y] and any(0<=x+dx<48 and 0<=y+dy<48 and p[x+dx,y+dy] for dx,dy in ((1,0),(-1,0),(0,1),(0,-1))):op[x,y]=(41,38,49,255)
        out.alpha_composite(self.image);return out

def uniform(a,frame,base,light,dark):
    a.feet(frame)
    a.oval(13,12,23,25,'#bc9245');a.oval(14,13,17,18,'#e9c674')
    a.box(16,15,4,7,'#ffe2a0');a.box(33,21,3,10,'#95713e')
    a.box(13,30,23,13,base);a.box(14,31,3,10,light);a.box(32,31,4,12,dark)
    a.box(18,32,3,5,light);a.box(26,32,4,5,light)
    a.box(13,40,23,3,dark);a.box(23,40,3,2,'#dcb96e');a.eyes()

def frame(kind,pose):
    a=Art();attack=pose==5
    if kind in ('ShellCop','RiotCop','Sniper','Robber','Sludger','Agent'):
        palettes={'ShellCop':('#385b91','#6482af','#253a64'),'RiotCop':('#344455','#627283','#242e3c'),
                  'Sniper':('#526541','#86946a','#374637'),'Robber':('#d7c8ac','#efe4cd','#655a60'),
                  'Sludger':('#beaa42','#e2ce69','#7d7734'),'Agent':('#383945','#626272','#232631')}
        uniform(a,pose,*palettes[kind])
        if kind=='ShellCop':
            a.poly([(11,16),(14,10),(32,10),(36,14),(35,18)],'#395e96');a.box(15,11,15,2,'#7698c5')
            a.box(12,17,25,3,'#202d4b');a.box(15,17,19,1,'#bb9d61')
            a.oval(23,12,4,4,'#f4cf76');a.box(24,12,2,1,'#fff0b5')
            a.poly([(29,33),(33,33),(32,37),(30,38)],'#e6bd60');a.box(30,33,2,1,'#fff0b1')
            a.box(21,32,1,8,'#9daac0');a.box(18,37,3,1,'#223350')
        elif kind=='RiotCop':
            a.oval(11,8,27,23,'#35485b');a.oval(14,10,20,15,'#4f687e')
            a.box(16,11,12,2,'#8095a2');a.box(12,22,25,3,'#1d2c3a')
            a.box(15,23,20,9,'#607e8f');a.box(16,24,18,2,'#a8c4c9');a.eyes(25)
            a.box(15,32,20,1,'#a1aeb0');a.box(18,35,11,3,'#212e3b');a.box(19,35,8,1,'#8293a0')
            for x in (16,31): a.box(x,34,2,2,'#a2a5a2')
        elif kind=='Sniper':
            a.poly([(11,17),(16,12),(31,11),(36,15),(33,19)],'#464d3a');a.box(16,13,13,2,'#7c8560')
            a.box(12,18,22,2,'#2f3b30');a.box(30,15,3,3,'#bcad72')
            for x,y in ((18,33),(27,37),(21,39)):a.box(x,y,4,2,'#a19d6b')
            a.line([(17,29),(29,41)],'#4d4038',2);a.box(29,35,4,4,'#766b4b')
        elif kind=='Robber':
            # Rounded wool hat, folded rim, mask and cloth stripes, with a tied loot sack.
            a.oval(5,30,13,13,'#8b7450');a.oval(6,31,8,9,'#b69a67');a.box(10,28,3,4,'#d3bb85')
            for y in (32,37):a.box(14,y,21,3,'#514d58');a.box(15,y,3,1,'#79717a')
            a.box(14,22,22,8,'#3b3541');a.eyes(24)
            a.oval(13,8,23,13,'#48434e');a.box(12,17,25,4,'#36333e');a.box(15,17,18,1,'#79717c')
            for x in (17,21,25):a.box(x,11,1,5,'#655d6a')
        elif kind=='Sludger':
            a.box(7,24,7,16,'#52684d');a.box(8,25,2,12,'#88a273');a.box(8,28,6,2,'#adad71')
            a.oval(12,10,26,26,'#c7b349');a.oval(15,13,19,19,'#e9d274')
            a.oval(16,19,19,15,'#343c40')
            for x in (18,28):
                a.oval(x,21,6,7,'#789aa0');a.box(x+1,22,3,3,'#b7e7dc');a.box(x+1,22,2,1,'#f2ffdc')
            a.oval(23,29,6,7,'#5c6552');a.box(24,31,4,1,'#a3ad71');a.box(24,33,4,1,'#262f32')
            a.box(22,36,1,5,'#f3df82');a.box(27,37,4,2,'#6c6f39')
        elif kind=='Agent':
            a.oval(15,12,20,22,'#bf8d5b');a.oval(16,13,15,17,'#e0b580')
            a.poly([(14,19),(14,12),(21,9),(31,10),(36,15),(34,21),(31,16),(23,16)],'#302b33')
            a.line([(18,12),(29,12),(32,14)],'#6d5a54',1)
            a.box(15,23,21,6,'#272c37');a.box(18,24,5,2,'#7d9fa9');a.box(29,24,4,1,'#adced0')
            a.poly([(20,31),(25,37),(29,31)],'#ece3d0');a.box(24,33,2,8,'#202430')
            a.poly([(17,31),(21,31),(23,38),(18,35)],'#73747b');a.poly([(30,31),(28,31),(27,38),(32,35)],'#505362')
            a.box(13,24,2,7,'#dad7cd');a.line([(14,29),(15,33),(18,34)],'#9b9fa5')
    elif kind=='Dynamite':
        a.feet(pose);a.box(17,10,16,33,'#aa343c');a.box(18,11,3,30,'#e16b58');a.box(30,11,3,32,'#742b37')
        a.box(20,11,9,1,'#f6a574');a.box(18,21,14,8,'#cfb784');a.box(19,22,12,1,'#f7deb0')
        a.box(21,25,8,2,'#9a5044');a.box(18,40,12,2,'#6d2834');a.eyes(31)
    elif kind=='Hydrant':
        a.box(11,41,27,4,'#8e3541');a.box(12,41,24,1,'#e0755a')
        a.box(15,23,19,19,'#ba4348');a.box(16,24,3,15,'#eb795e');a.box(30,25,4,16,'#7c303f')
        a.oval(14,12,21,20,'#c64a4d');a.oval(16,13,15,11,'#e36d57');a.box(18,14,8,2,'#ff9d71')
        a.box(22,9,6,5,'#b9994e');a.box(23,9,4,1,'#ffe1a0')
        for x in (9,33):
            a.box(x,29,6,7,'#b99143');a.box(x,29,6,2,'#efd183');a.box(x+1,34,4,2,'#765538')
        a.eyes(24);a.box(21,36,8,3,'#832f3d');a.box(22,36,6,1,'#e97862')
        for x in (13,34):a.box(x,42,2,2,'#c3b3a2')
    elif kind=='GhostKin':
        # One hood of fabric and one continuous, fluttering hem.
        wave = (1, 0, -1, 0, 0, 0)[pose]
        a.poly([(10,24),(11,17),(16,11),(22,8),(29,10),(34,15),(36,23),
                (37,30),(40,37),(38,40),(33,39+wave),(28,41+wave),(22,37),
                (16,40-wave),(9,38),(12,31)], '#afa9c3')
        a.poly([(12,23),(14,17),(18,12),(23,10),(29,12),(33,16),(34,23),
                (34,31),(36,36),(33,37+wave),(28,39+wave),(22,35),(16,37-wave),
                (11,36),(13,30)], '#ded8e6')
        a.poly([(14,21),(17,15),(22,12),(28,13),(31,17),(28,18),(20,17),
                (17,24),(15,31),(12,35),(14,29)], '#faf2ec')
        a.line([(17,30),(16,34),(14,36)], '#c6bed5', 2)
        a.line([(31,28),(31,33),(34,36)], '#c6bed5', 2)
        for x,y in ((20,22),(29,23)):
            a.oval(x,y,3,5,'#4c465f')
            a.box(x,y,1,1,'#82748c')
        if attack:
            a.oval(24,28,5,6,'#5f536f')
        else:
            a.line([(23,29),(24,30),(27,30),(28,29)],'#665a72')
    elif kind in ('RoboCart','Android'):
        metal,hi,shadow='#a6b2bd','#e6e5d9','#59697c'
        if kind=='RoboCart':
            for x in (13,29):
                a.oval(x,37,9,9,INK);a.box(x+2,39,5,2,shadow)
                t=math.radians(pose*45)
                a.line([(x+4-round(math.cos(t)*2),41-round(math.sin(t)*2)),
                        (x+4+round(math.cos(t)*2),41+round(math.sin(t)*2))],hi)
            a.box(11,16,28,25,shadow);a.poly([(12,16),(34,16),(37,19),(15,23),(12,22)],hi)
            a.poly([(12,23),(35,19),(35,39),(12,39)],metal);a.box(35,19,3,21,'#73889a')
            a.box(12,25,23,3,'#d8a448');a.box(13,25,21,1,'#f6d58a')
            a.oval(21,29,12,12,INK);a.oval(23,31,8,8,'#3e8396');a.oval(24,32,5,5,'#78e5dd');a.box(24,32,2,2,'#effee7')
            a.box(24,10,2,6,shadow);a.oval(22,7,6,5,'#e9a946' if attack else '#b3c2bc')
            for x in (14,33):a.box(x,30,1,2,'#edf0e5')
        else:
            a.feet(pose)
            for x,s in ((16,pose in (0,1)),(29,pose in (2,3))):
                a.box(x,34-int(s),4,9,shadow);a.box(x+1,36-int(s),1,5,hi)
            a.box(13,22,25,14,shadow);a.box(14,23,20,11,metal);a.box(15,23,18,2,hi)
            a.oval(22,26,7,7,'#3c879f');a.oval(24,27,3,3,'#8de8df')
            a.box(16,10,20,14,shadow);a.box(17,11,16,11,metal);a.box(18,11,14,2,hi)
            a.box(18,16,16,5,'#293d50');a.box(20,17,11,2,'#f1ad68' if attack else '#78d9e5')
            a.box(24,6,2,4,shadow);a.box(24,5,2,2,'#65d5e6')
            for x in (15,33):a.box(x,25,1,2,hi)
    elif kind in BED_CAST:
        bed_toy(a, kind, pose)
    elif kind=='Dandelion':
        # Clearly separated ivory petals around a warm, shaded seed head.
        a.line([(24,44),(25,34),(24,25)],'#567d4d',2)
        a.poly([(24,39),(17,34),(15,35),(19,40),(24,41)],'#7b9b58');a.line([(17,36),(23,40)],'#afbb75')
        for i in range(10):
            t=math.radians(i*36-90);x=24+math.cos(t)*12;y=22+math.sin(t)*12
            a.oval(round(x)-4,round(y)-6,9,12,'#a9a7ad');a.oval(round(x)-4,round(y)-6,8,10,'#e3ded3')
            a.oval(round(x)-3,round(y)-6,5,7,'#fff4db')
        a.oval(14,12,23,23,'#a17b48');a.oval(15,13,20,20,'#dfb66a');a.oval(16,14,16,14,'#f5d589')
        for x,y in ((21,15),(28,16),(18,20),(31,24),(21,30)):a.box(x,y,2,1,'#bc8f50')
        a.eyes(22);a.box(24,31,3,1,'#7c5742')
        if attack:a.oval(17,29,4,3,'#e89d7c');a.oval(32,28,3,3,'#e89d7c')
    else:
        extra(a,kind,pose)
    return a.edge()

def generate(root):
    for kind in CAST_SHEETS:
        sheet=Image.new('RGBA',(48*6,48*2))
        for pose in range(6):
            img=frame(kind,pose);sheet.paste(img,(pose*48,0))
            white=Image.new('RGBA',img.size,(255,255,255,0));white.putalpha(img.getchannel('A'));sheet.paste(white,(pose*48,48))
        sheet.save(os.path.join(root,'cast_'+kind.lower()+'.png'))

# Multi-part enemies use these drawn core layers while their existing animation still controls
# wings, arms, pincers, shells, weapons and attack warnings.
EXTRA_CAST = ('Biker','LampMimic','CubeIce','CubeLead','Angel','AngelBow','Cherub','Thundercloud',
              'Diver','AcidSprayer','BarrelRoller','Flufferfish','TurtleShell','GiantCrab',
              'GiantOctopus','StinkPigeon','SewerGator','AlleyDog','Raccoon','MoonBunny',
              'LivingAsteroid','IronAsteroid','Astronaut','VeteranAstronaut','Alien','VeteranAlien',
              'Ufo','MoonGolem','GolemHead','CheeseWorm','WormHead','Starfish','Swordfish',
              'ShapeCircle','ShapeSquare','ShapeTriangle','ShapeStar',
              'CrabClaw','GolemFist','RustDrum','PigeonHead','GatorHead','DogHead','RaccoonHead')
BED_CAST = ('DustMite', 'WindUpBeetle', 'LostMarble', 'DustBunnyKit', 'PegCroc', 'QuiltMoth', 'TumbleDie')
CAST_SHEETS += EXTRA_CAST + BED_CAST

def extra(a,kind,pose):
    attack=pose==5
    if kind in ('CrabClaw','GolemFist','RustDrum','PigeonHead','GatorHead','DogHead','RaccoonHead'):
        if kind=='CrabClaw':
            a.oval(10,10,28,28,'#884749');a.oval(11,10,24,24,'#c97759');a.oval(13,12,17,12,'#edaa75')
            a.line([(15,13),(23,12),(28,15)],'#ffcf92',2);a.box(16,31,13,2,'#a65c50')
        elif kind=='GolemFist':
            a.poly([(11,13),(19,9),(33,12),(38,22),(34,36),(19,38),(10,28)],'#56576c')
            a.poly([(12,14),(20,10),(32,13),(34,22),(30,33),(17,34),(12,26)],'#99909e')
            a.poly([(12,14),(20,10),(32,13),(29,18),(18,21)],'#c3b0b5')
            a.line([(18,23),(19,29)],'#736c81');a.line([(25,21),(26,28)],'#736c81')
        elif kind=='RustDrum':
            a.box(9,11,30,26,'#874b3d');a.box(10,12,27,24,'#b76b43');a.box(11,13,4,22,'#dca05d')
            for yy in (17,29):a.box(9,yy,30,3,'#654644');a.box(10,yy,27,1,'#c39768')
            a.box(17,11,13,2,'#dcb569');a.box(20,10,8,2,'#adbf65');a.box(29,22,3,2,'#8a5140')
        else:
            palettes={'PigeonHead':('#818899','#b3b0ba','#515d73'),'GatorHead':('#7d9761','#b2bf80','#4d694d'),
                      'DogHead':('#a37a54','#e1b980','#634d48'),'RaccoonHead':('#b0a5ae','#e0c8ba','#62596b')}
            base,hi,dark=palettes[kind]
            a.oval(10,12,27,25,dark);a.oval(11,12,23,21,base);a.oval(13,13,16,12,hi)
            a.poly([(24,24),(39,25),(41,31),(27,35),(23,31)],hi)
            a.box(37,26,4,4,dark);a.line([(29,32),(36,32)],dark)
            if kind=='RaccoonHead':
                for x in (12,29):a.poly([(x,16),(x+2,6),(x+8,15)],dark)
                a.box(13,23,23,6,dark);a.box(20,23,4,3,'#f2e0ca');a.box(22,24,2,2,INK)
            elif kind=='DogHead':a.poly([(12,16),(8,23),(13,34),(19,24)],dark)
            elif kind=='GatorHead':
                a.box(24,14,10,3,dark);a.line([(16,20),(20,18),(23,20)],base)
            else:a.box(14,17,6,2,'#d3c6c5');a.box(26,27,6,1,dark)
    elif kind in ('Starfish','Swordfish') or kind.startswith('Shape'):
        if kind=='Swordfish':
            a.poly([(9,25),(17,19),(32,21),(39,25),(31,30),(17,30)],'#48728d')
            a.poly([(10,25),(18,26),(34,27),(31,30),(17,30)],'#b4c4bb')
            a.poly([(17,20),(22,12),(30,21)],'#57738e');a.line([(21,17),(23,20),(27,20)],'#91a6b5')
            a.line([(15,23),(24,22),(30,23)],'#94c6cd')
            for x in (18,22,26):a.line([(x,24),(x+2,25)],'#679cae')
        else:
            shape=kind[5:] if kind.startswith('Shape') else 'Star'
            color={'Circle':'#58acc0','Square':'#a080bc','Triangle':'#e8ad55','Star':'#e47f63'}[shape]
            if shape=='Circle':a.oval(5,5,39,39,color)
            elif shape=='Square':a.box(7,7,34,34,color)
            elif shape=='Triangle':a.poly([(24,3),(45,42),(3,42)],color)
            else:
                points=[]
                for i in range(10):
                    t=math.radians(i*36-90);r=22 if i%2==0 else 10
                    points.append((round(24+math.cos(t)*r),round(24+math.sin(t)*r)))
                a.poly(points,color)
            # Broad bevel planes follow the actual mask, never a full-frame filter.
            mask=a.image.getchannel('A');pix=a.image.load()
            rgb=ImageColor.getrgb(color)
            for y in range(48):
                for x in range(48):
                    if not mask.getpixel((x,y)):continue
                    if any(not (0<=x+dx<48 and 0<=y+dy<48) or not mask.getpixel((x+dx,y+dy)) for dx,dy in ((-2,0),(0,-2))):pix[x,y]=tuple(min(255,int(v*1.2)) for v in rgb)+(255,)
                    elif y>29 or x>34:pix[x,y]=tuple(int(v*.78) for v in rgb)+(255,)
            if kind=='Starfish':
                for i in range(5):
                    t=math.radians(i*72-90)
                    for r in (11,16):a.oval(round(24+math.cos(t)*r)-1,round(24+math.sin(t)*r)-1,3,3,'#ffcf96')
            else:a.box(17,14,6,2,'#f3e4c0')
    elif kind in ('Diver','AcidSprayer','BarrelRoller'):
        if kind=='Diver':
            uniform(a,pose,'#536357','#849382','#344640')
            a.box(7,24,7,16,'#ab844d');a.box(8,25,2,13,'#e4c481');a.box(8,27,6,2,'#72563c')
            a.oval(11,9,28,27,'#8d693d');a.oval(12,10,25,23,'#c29a55');a.oval(14,11,19,18,'#e4c67e')
            a.oval(17,17,19,16,'#725c3d');a.oval(19,18,15,13,'#3d6778');a.oval(20,19,11,8,'#739fa6')
            a.box(21,20,4,2,'#c4e5d7');a.eyes(23)
            for x,y in ((14,20),(34,16),(30,33)):a.box(x,y,2,2,'#fff0bd')
            for x in (15,28):a.box(x,43,8,3,'#3d889b');a.box(x,43,6,1,'#93bec0')
        elif kind=='AcidSprayer':
            uniform(a,pose,'#66845a','#a0b27a','#3e5747')
            a.box(5,20,10,21,'#475b54');a.oval(5,16,10,10,'#62796a');a.box(6,21,2,17,'#97ac88')
            a.box(7,28,6,6,'#242e32');a.box(8,29,3,4,'#bad96a')
            a.oval(12,10,26,25,'#6c895b');a.oval(14,12,21,19,'#95a76e')
            a.box(16,20,20,10,'#303d3c');a.box(18,22,16,5,'#74a5a4');a.box(19,22,12,1,'#d6e7ce')
            a.oval(24,29,7,7,'#323d3b');a.box(25,31,5,1,'#8b9b79')
        else:
            uniform(a,pose,'#c3b9a4','#f0ddba','#8e857d')
            for x in (13,30):a.box(x,30,6,12,'#44444a');a.box(x+1,31,2,9,'#68626b')
            a.oval(15,12,20,21,'#ba8656');a.oval(16,13,15,16,'#e6b27b');a.box(16,16,20,5,'#a94443');a.box(17,16,15,1,'#e08163')
            a.box(11,18,5,3,'#713c41');a.eyes(23)
    elif kind=='Biker':
        for x in (10,31):
            a.oval(x-5,33,12,12,'#282834');a.oval(x-2,36,6,6,'#747a85');a.box(x,37,3,2,'#d3d3cb')
        a.poly([(9,36),(15,29),(30,28),(37,35),(31,38),(14,39)],'#45424d');a.line([(10,36),(17,30),(30,30),(35,36)],'#a5a9ae',2)
        a.box(6,37,11,2,'#b7b9b6');a.box(7,39,9,1,'#777d88');a.box(16,28,14,5,'#733f3d');a.box(18,28,9,1,'#d17a50')
        a.box(18,19,12,12,'#33323d');a.poly([(19,19),(23,24),(28,19)],'#6b6874');a.box(24,24,1,7,'#c3b7a2')
        a.oval(17,7,16,17,'#353640');a.oval(18,8,12,10,'#555562');a.box(20,9,8,2,'#85818b')
        a.poly([(21,13),(33,15),(32,19),(22,17)],'#788c9c');a.line([(23,14),(30,15)],'#c9d2ca')
        a.box(34,27,3,8,'#b2b6b0');a.oval(34,29,6,6,'#e9d390')
    elif kind=='LampMimic':
        a.box(13,11,24,29,'#38313a');a.box(15,13,20,25,'#aa8246');a.box(17,14,16,21,'#edcc7c')
        a.box(17,14,2,14,'#fff0ae');a.box(31,14,2,22,'#bf904e');a.box(21,7,8,4,'#51424a')
        for x in (15,34):a.box(x,12,1,26,'#87746e')
        a.eyes(21)
    elif kind.startswith('Cube'):
        lead=kind=='CubeLead'; base,hi,dark=('#6c7382','#9c9ca2','#424c5c') if lead else ('#6aa9bb','#b2e1d9','#356a89')
        a.box(7,10,34,34,dark);a.poly([(8,10),(37,10),(39,12),(13,18),(8,17)],hi)
        a.poly([(8,18),(37,12),(37,39),(8,41)],base);a.poly([(8,41),(37,39),(39,41),(39,43),(8,43)],dark)
        if lead:
            for x,y in ((11,16),(34,15),(12,37),(34,36)):
                a.box(x,y,3,3,dark);a.box(x,y,2,1,hi)
            a.line([(11,30),(15,29),(17,31)],'#535e71');a.box(29,34,4,1,'#8b8c95')
        else:
            a.poly([(10,19),(16,18),(14,32),(10,34)],'#99cbd0');a.line([(27,13),(24,18),(28,21)],'#def4e5')
            a.line([(30,34),(27,37),(28,41)],'#4a7d98')
        a.eyes(23);a.box(23,35,7,1,INK)
    elif kind in ('Angel','AngelBow','Cherub'):
        cherub=kind=='Cherub'
        if not cherub:
            a.poly([(17,27),(31,27),(36,43),(12,43)],'#b6aaa1');a.poly([(17,28),(30,28),(32,41),(15,40)],'#e4d8ba')
            if kind=='AngelBow':
                pixels=a.image.load()
                for yy in range(27,44):
                    for xx in range(12,37):
                        old=pixels[xx,yy]
                        table={(182,170,161,255):(65,105,137,255),(228,216,186,255):(122,170,187,255)}
                        if old in table:pixels[xx,yy]=table[old]
            a.box(15,39,18,2,'#d3ad58');a.box(16,39,15,1,'#ffe1a0');a.line([(22,28),(20,36)],'#fff0d2',2)
        else:
            a.oval(15,28,20,15,'#e7b195');a.oval(16,29,14,10,'#f6cfad');a.oval(15,38,20,7,'#d4d4dc');a.oval(16,38,16,4,'#fff2dc')
        a.oval(15,12,21,21,'#a57458');a.oval(16,13,18,18,'#e9b38b');a.oval(17,14,12,13,'#f6d09d')
        for x,y,r in ((17,13,8),(23,10,10),(30,13,7)):
            a.oval(x-3,y-3,r,r,'#ddba61');a.oval(x-3,y-3,r-2,r-3,'#ffe09a')
        a.eyes(23)
        if not cherub:
            a.oval(14,6,23,5,'#aa8043');a.oval(16,6,19,3,'#ffe6a3')
    elif kind=='Thundercloud':
        for x,y,w,h in ((5,23,21,20),(13,15,24,26),(28,22,17,19)):
            a.oval(x,y,w,h,'#454d64');a.oval(x,y-1,w-2,h-4,'#67718b');a.oval(x+2,y,w-6,h-9,'#8a8fa4')
        a.box(13,35,23,5,'#505a73');a.eyes(28);a.line([(19,26),(24,27)],INK);a.line([(30,27),(35,26)],INK)
        a.poly([(15,17),(18,12),(20,17)],'#a7a5b6');a.box(25,37,6,1,INK)
    elif kind in ('Flufferfish','TurtleShell','GiantCrab','GiantOctopus'):
        palette={'Flufferfish':('#d9ae53','#f4dc8a','#a27d42'),'TurtleShell':('#66805b','#a1ad71','#3f574a'),
                 'GiantCrab':('#c36e52','#efac72','#914e48'),'GiantOctopus':('#9877b0','#ccb0ce','#674d87')}
        base,hi,dark=palette[kind]
        a.oval(8,10,33,30,dark);a.oval(9,10,29,26,base);a.oval(11,12,22,14,hi)
        if kind=='TurtleShell':
            a.oval(10,12,28,24,base)
            for points in ([(20,14),(29,14),(33,21),(29,29),(20,29),(16,21),(20,14)],[(10,21),(16,21)],[(29,29),(33,35)],[(20,29),(16,35)]):a.line(points,dark)
            a.line([(20,15),(28,15),(31,20)],hi,2)
        elif kind=='GiantCrab':
            a.poly([(9,29),(17,32),(32,32),(39,28),(36,37),(14,38)],'#e6b17c')
            for x,y in ((15,24),(30,22),(25,18)):a.box(x,y,3,2,dark)
            a.line([(14,13),(23,12),(29,14)],'#ffd2a0',2)
        elif kind=='GiantOctopus':
            for x,y in ((15,22),(32,20),(29,13)):a.oval(x,y,3,3,base)
            a.box(14,15,5,2,'#f3d8dd');a.eyes(28)
        else:
            a.oval(12,26,22,12,'#ead39b')
            for x,y in ((15,18),(23,14),(32,20)):a.oval(x,y,3,3,'#b88941')
            a.eyes(23);a.poly([(25,30),(20,29),(20,33)],'#c78256');a.line([(21,30),(24,31)],'#f5c582')
    elif kind in ('MoonBunny','LivingAsteroid','IronAsteroid','MoonGolem','GolemHead','CheeseWorm','WormHead','Starfish','Swordfish',
              'ShapeCircle','ShapeSquare','ShapeTriangle','ShapeStar',
              'CrabClaw','GolemFist','RustDrum','PigeonHead','GatorHead','DogHead','RaccoonHead'):
        if kind=='MoonBunny':
            a.oval(12,12,25,29,'#b5abb9');a.oval(13,12,22,25,'#eee1d3');a.oval(15,13,16,16,'#fff2df')
            a.oval(10,32,8,8,'#e8d9c9');a.oval(29,36,9,5,'#d3c4c1')
            for x in (21,29):a.box(x,23,3,4,'#b64f62');a.box(x,23,1,1,'#fff1d8')
            a.box(26,30,2,2,'#d38c94');a.line([(24,34),(26,35),(29,34)],'#9c7c83')
            a.line([(16,19),(18,17),(21,17)],'#d3c4c1')
        else:
            cheese='Worm' in kind;iron=kind=='IronAsteroid'
            base,hi,dark=('#cda850','#efcd77','#99743b') if cheese else ('#6b7488','#a0a6b1','#434b63') if iron else ('#837b8c','#b0a3ad','#57566d')
            r=14 if kind in ('GolemHead','WormHead') else 20
            a.poly([(24-r,20),(29-r,27-r),(25,24-r),(r+19,16),(r+24,27),(r+15,39),(18,r+24),(24-r,33)],dark)
            a.poly([(25-r,18),(30-r,28-r),(24,25-r),(r+18,17),(r+20,29),(r+12,36),(17,r+20),(25-r,31)],base)
            a.poly([(26-r,18),(30-r,28-r),(24,25-r),(r+10,15),(24,16),(19,22)],hi)
            for x,y in ((13,25),(29,18),(30,33)):
                a.oval(x,y,5,4,dark);a.line([(x+1,y+4),(x+4,y+4)],hi)
            if kind in ('MoonGolem','GolemHead'):
                a.line([(15,21),(21,25),(20,29),(29,32)],'#a1cfb0');a.line([(32,18),(28,23)],'#c7e6c2')
            elif iron:a.line([(13,24),(20,28),(18,33)],'#d8936c')
            if kind in ('GolemHead','WormHead','LivingAsteroid','IronAsteroid'):
                if kind=='WormHead':
                    a.line([(16,20),(18,17),(22,16)],hi,2)
                elif kind=='GolemHead':a.eyes(25)
    elif kind in ('Astronaut','VeteranAstronaut','Alien','VeteranAlien'):
        vet=kind.startswith('Veteran')
        if 'Astronaut' in kind:
            a.feet(pose);a.box(8,26,7,14,'#5c6b7a');a.box(9,27,2,11,'#b4bec1')
            a.oval(13,26,24,18,'#9d9dab');a.oval(14,27,20,14,'#e0d6c7');a.box(16,34,17,3,'#ae6261' if vet else '#b99155')
            a.box(21,29,8,5,'#727d8c');a.box(22,30,2,2,'#a7d0c6');a.box(25,30,2,2,'#dfac74')
            a.oval(12,8,27,27,'#9897a4');a.oval(13,9,24,22,'#ece0c9');a.oval(17,15,19,14,'#785e4a')
            a.oval(18,16,17,11,'#c27e58' if vet else '#bf9b55');a.oval(19,16,13,7,'#e4bc79');a.box(20,17,6,2,'#fff0bd')
            a.box(14,19,2,6,'#bbb8b4');a.box(36,19,2,6,'#6f7888')
        else:
            base,hi,dark=('#8889b5','#c4b3d1','#535d83') if vet else ('#80ad75','#bbce8c','#506c58')
            a.feet(pose);a.oval(18,29,13,15,dark);a.oval(19,30,10,11,base);a.box(22,34,5,1,hi)
            a.oval(11,12,28,23,dark);a.oval(12,12,25,20,base);a.oval(14,13,17,10,hi)
            a.line([(25,12),(24,7),(27,4)],dark,2);a.oval(25,2,5,5,'#e7c993' if vet else '#bbde9c')
            for x in (18,29):a.oval(x,21,5,8,'#333640');a.box(x+1,22,2,2,'#e7e4ca')
            a.box(24,32,5,1,dark)
            if vet:a.line([(17,18),(20,22),(20,27)],'#bc879c')
    elif kind=='Ufo':
        a.oval(3,27,43,14,'#485169');a.oval(4,25,40,12,'#9fa9b2');a.oval(7,25,32,7,'#e2daca')
        a.oval(13,10,24,23,'#456c80');a.oval(14,10,21,20,'#8fc1be');a.oval(16,11,12,11,'#cee4ce')
        a.oval(22,20,10,9,'#7a9e74');a.box(24,23,2,2,INK);a.box(29,23,2,2,INK)
        a.box(9,30,30,2,'#777e92');a.box(13,27,22,3,'#c9c8bf')
        for x in (10,19,29,37):a.box(x,33,3,2,'#f3d285' if attack else '#83cdbf')
    elif kind in ('StinkPigeon','SewerGator','AlleyDog','Raccoon'):
        # Core body only. The original marked ears, moving jaws/wings and coin bag stay live.
        pal={'StinkPigeon':('#7e8398','#b0a8b6','#4a566f'),'SewerGator':('#6e865a','#a0b274','#3f5948'),
             'AlleyDog':('#a07856','#d6b17a','#674d45'),'Raccoon':('#8b8995','#bfb5ba','#545264')}
        base,hi,dark=pal[kind]
        a.oval(8,16,32,23,dark);a.oval(9,16,28,18,base);a.oval(12,17,19,10,hi)
        a.oval(13,28,22,9,hi)
        for x,y in ((13,24),(18,20),(29,26)):
            a.line([(x,y+2),(x+2,y),(x+4,y+1)],base)
        if kind=='SewerGator':
            for x in (11,18,25,32):a.poly([(x,17),(x+2,13),(x+4,17)],dark)
        if kind=='StinkPigeon':
            a.oval(24,19,15,17,'#708e69');a.line([(26,21),(30,23),(31,29)],'#a7b283')


def bed_toy(a, kind, pose):
    from bed_enemy_art import draw
    draw(a, kind, pose)
