# Guns

Every gun in the game, sorted by tier (the chest it comes out of: D brown, C blue, B green, A red, S black).
Numbers come from the factory methods in `objects/Weapon.java` - update this file when a gun changes.

- **Damage** - per bullet / hit (x N = pellets per shot). Hot Sauce adds +25%.
- **Damage/s** - damage per second while the trigger is held, not counting reloads (every pellet hitting).
- **Magazine** - shots before a reload. **Total ammo** - all the ammo it carries, magazine included (INF = never runs out).
- Finding a gun you already carry upgrades it (+15% damage, +25% magazine/ammo or +15% bullet speed).

## How many in each tier

| Tier | Guns |
|------|-----:|
| Starter | 4 |
| D | 10 |
| C | 11 |
| B | 10 (9 + the Flufful once unlocked) |
| A | 10 |
| S | 2 |
| **Total** | **47** (42 in chests + the Flufful + 4 starters) |

## Starters (never in chests)

| Gun | Damage | Damage/s | Magazine | Total ammo | What it does |
|-----|-------:|---------:|---------:|-----------:|--------------|
| Rusty Sidearm | 5 | 25 | 10 | INF | The boy's starter: reliable single shots |
| Throwing Stars | 3.6 | 21 | 10 | INF | The sister's starter: quick spinning stars |
| Pearl Revolver | 9 | 30 | 6 | INF | Granny's starter: an antique revolver, six heavy shots, slow reload |
| Old Double-Barrel | 3 x 6 | 60 (22 with reload) | 2 | INF | Grandad's starter: two blasts, then he cracks it open; with his +1 about as good as the Fluff Shotgun |

## Tier D - 10 guns

| Gun | Damage | Damage/s | Magazine | Total ammo | What it does |
|-----|-------:|---------:|---------:|-----------:|--------------|
| Confetti Popper | 2.2 x 12 | 53 | 4 | 60 | Short-range burst of party-coloured pellets |
| Bubble Blaster | 7 | 25 | 14 | 120 | Slow wobbly bubbles, go through enemies and shove hard; wet |
| Laser Pointer | 1.4 | 28 | 120 | 720 | Thin red beam, hits instantly along 4.5 units |
| Water Pistol | 2.2 | 28 | 30 | 300 | Fast short stream of droplets, shoves hard; wet |
| Snowball Slinger | 6 | 19 | 10 | 120 | Snowballs slow enemies down, enough of them freeze it solid; wet |
| Slingshot | 5-15 | 15-26 (flicks are faster, full pulls hit harder) | 8 | 100 | Hold to pull back: a quick flick or a hard, fast pebble |
| Popcorn Popper | 3 (+3 puffs x 2.4) | 10 + puffs | 12 | 150 | Kernels pop in mid-air into three buttery puffs |
| Hair Dryer | 0.8 x 2 | 23 | 60 | 600 | Short hot-air stream: shoves enemies back, blows smoke and stink clouds away |
| Cap Gun | 4 (BANG 20) | 27 | 12 | 180 | Little clicks, every sixth shot a big BANG |
| Pea Shooter | 3 x 3 | 23 | 10 | 120 | Each pull spits three peas one after another |

## Tier C - 11 guns

| Gun | Damage | Damage/s | Magazine | Total ammo | What it does |
|-----|-------:|---------:|---------:|-----------:|--------------|
| Fluff Shotgun | 3.5 x 6 | 32 | 6 | 80 | Short-range spread, big knockback |
| Banana-rang | 9 | 20 | 4 | 60 | Boomerang: flies out ~2.7 units and back, hits on both ways, pierces |
| Super Bouncer | 6 | 20 | 8 | 120 | Rubber balls bounce off walls and cover 3 times |
| Plasma Zapper | 7 | 32 | 10 | 150 | Fast green laser bolts |
| Pillow Cannon | 9 | 16 | 6 | 70 | Slow pillow bursts into 8 feathers (~3 damage each) where it hits |
| Jumper Cables | 7 | 20 | 12 | 140 | A spark that jumps on to 3 more enemies |
| Acid Squirter | 2.4 | 27 | 40 | 360 | Short acid stream; the acid keeps eating at what it hits |
| Flare Gun | 9 | 16 | 6 | 60 | Sets what it hits on fire, leaves a patch of flames |
| Freeze Ray | 0.6 | 12 | 100 | 600 | Cold beam: piles ice on enemies until they freeze, ices the floor |
| Sticky Darts | 4 (+12 pop) | 29 | 9 | 120 | Darts stick in enemies; the third pops them all for extra damage |
| Kazoo | 4.5 x 3 | 32 | 10 | 120 | A slow, wobbly fan of music notes that go through enemies |

## Tier B - 10 guns

| Gun | Damage | Damage/s | Magazine | Total ammo | What it does |
|-----|-------:|---------:|---------:|-----------:|--------------|
| Pocket Uzi | 2.2 | 34 | 32 | 400 | Very fast, wide spray |
| Bee Hive | 3 x 3 | 30 | 15 | 150 | Bees wiggle and home in on enemies |
| Gull Gatling | 2.4 | 48 | 150 | 900 | Slow to start, then a storm of bullets (48/s once spun up) |
| Egg Lobber | 22 blast | 37 | 4 | 48 | Hold to charge, lobs eggs over cover that burst on landing |
| Frying Pan | 14 | 33 | melee | INF | Swing hits what's close and bats enemy bullets back |
| Porcupine | 4 x 3 (+10 all round) | 29 at aim | 10 | 120 | Quills where you aim plus a ring of 10 all around you |
| Rubber Duck Launcher | 6 (+8 burst) | 35 | 6 | 72 | Ducks waddle off toward enemies and burst with a squeak; faster under water |
| Gloop Launcher | 12 | 20 | 5 | 50 | Glob of acid bursts into a pool that melts enemies |
| Boom-buss | 4 x 7 (+5 blast each) | up to 105 | 2 | 40 | Blunderbuss of little bombs, each blasts where it hits |
| Flufful | 5.5 | 34 | 24 | INF | <details><summary>Spoiler</summary>The talking gun: homing pom-poms, levels up with kills (20/60/120/200). Unlocked by beating the Fluffy Shark.</details> |

## Tier A - 10 guns

| Gun | Damage | Damage/s | Magazine | Total ammo | What it does |
|-----|-------:|---------:|---------:|-----------:|--------------|
| Big Magnum | 16 | 32 | 6 | 72 | Big fast piercing shots |
| Rainbow Ray | 2.8 | 43 | 40 | 480 | Super fast stream of piercing rainbow bullets |
| Party Rocket | 12 + 34 blast | 51 | 3 | 36 | Rockets speed up and blow up on what they hit |
| Long Shot | 45 | 50 | 4 | 36 | Sniper rifle with a laser sight, pierces everything in a line |
| Dust Bunny | 6 a bite (4/s) + 26 burst | ~24 + burst | 2 | 24 | Drifts out, sucks enemies in, chews on them and bursts |
| Polaroid | 28 | 31 | 1 | 14 | Camera flash: wipes out bullets in front of you, hurts all it catches |
| Chomper | 60 | 40 (with reload) | 1 | 50 | A homing shark that bites hard; faster under water |
| Heirloom | 4 -> 3.6 x 3 | 16 -> 77 | 8 -> 32 | INF | Grows from every kill while carried (15/40/80/140 kills): faster, piercing, 2 then 3 shots, homing at level 5. Grandad's kills count double |
| Bottle Rocket Rack | 5 x 4 (+6 pop each) | 73 | 5 | 60 | Each pull sends off four little homing rockets one by one |
| Fluff Cannon | 24 | 27 (hits everything in its way) | 3 | 30 | A giant pom-pom rolls slowly through enemies and bounces off walls 3 times |

## Tier S - 2 guns (black chests only)

From floor 3 on, a treasure chest - or the boss chest after a fight without a single hit - comes out black 1 time in 12. Baa-llo never sells S guns.

| Gun | Damage | Damage/s | Magazine | Total ammo | What it does |
|-----|-------:|---------:|---------:|-----------:|--------------|
| Unicorn Horn | 4 | 80 | 200 | 1000 | A long, thick rainbow beam that burns through everything in its way |
| Shooting Star | 20 (+30 blast, +8 x 8 stars) | 36 + blasts | 6 | 72 | A homing star that blows up and bursts into eight smaller stars |
