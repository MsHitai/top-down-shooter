# Guns

Every gun in the game, sorted by tier (the chest it comes out of: D brown, C blue, B green, A red).
Numbers come from the factory methods in `objects/Weapon.java` - update this file when a gun changes.

- **Damage** - per bullet / hit (x N = pellets per shot). Hot Sauce adds +25%.
- **Damage/s** - damage per second while the trigger is held, not counting reloads (every pellet hitting).
- **Magazine** - shots before a reload. **Total ammo** - all the ammo it carries, magazine included (INF = never runs out).
- Finding a gun you already carry upgrades it (+15% damage, +25% magazine/ammo or +15% bullet speed).

## How many in each tier

| Tier | Guns |
|------|-----:|
| Starter | 4 |
| D | 5 |
| C | 9 |
| B | 9 (8 + the Flufful once unlocked) |
| A | 7 |
| **Total** | **34** (29 in chests + the Flufful + 4 starters) |

## Starters (never in chests)

| Gun | Damage | Damage/s | Magazine | Total ammo | What it does |
|-----|-------:|---------:|---------:|-----------:|--------------|
| Rusty Sidearm | 5 | 25 | 10 | INF | The boy's starter: reliable single shots |
| Throwing Stars | 3.6 | 21 | 10 | INF | The sister's starter: quick spinning stars |
| Pearl Revolver | 9 | 30 | 6 | INF | Granny's starter: an antique revolver, six heavy shots, slow reload |
| Old Double-Barrel | 4 x 7 | 93 (40 with reload) | 2 | INF | Grandad's starter: two big blasts, then he cracks it open; +1 like all his guns |

## Tier D - 5 guns

| Gun | Damage | Damage/s | Magazine | Total ammo | What it does |
|-----|-------:|---------:|---------:|-----------:|--------------|
| Confetti Popper | 2.2 x 12 | 53 | 4 | 60 | Short-range burst of party-coloured pellets |
| Bubble Blaster | 7 | 25 | 14 | 120 | Slow wobbly bubbles, go through enemies and shove hard; wet |
| Laser Pointer | 1.4 | 28 | 120 | 720 | Thin red beam, hits instantly along 4.5 units |
| Water Pistol | 2.2 | 28 | 30 | 300 | Fast short stream of droplets, shoves hard; wet |
| Snowball Slinger | 6 | 19 | 10 | 120 | Snowballs slow enemies down, enough of them freeze it solid; wet |

## Tier C - 9 guns

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

## Tier B - 9 guns

| Gun | Damage | Damage/s | Magazine | Total ammo | What it does |
|-----|-------:|---------:|---------:|-----------:|--------------|
| Pocket Uzi | 2.2 | 34 | 32 | 400 | Very fast, wide spray |
| Bee Hive | 3 x 3 | 30 | 15 | 150 | Bees wiggle and home in on enemies |
| Gull Gatling | 2 | 40 | 150 | 750 | Slow to start, then a storm of bullets (40/s once spun up) |
| Egg Lobber | 22 blast | 37 | 4 | 48 | Hold to charge, lobs eggs over cover that burst on landing |
| Frying Pan | 14 | 33 | melee | INF | Swing hits what's close and bats enemy bullets back |
| Porcupine | 4 x 3 (+10 all round) | 29 at aim | 10 | 120 | Quills where you aim plus a ring of 10 all around you |
| Gloop Launcher | 12 | 20 | 5 | 50 | Glob of acid bursts into a pool that melts enemies |
| Boom-buss | 4 x 7 (+5 blast each) | up to 105 | 2 | 40 | Blunderbuss of little bombs, each blasts where it hits |
| Flufful | 5.5 | 34 | 24 | INF | <details><summary>Spoiler</summary>The talking gun: homing pom-poms, levels up with kills (20/60/120/200). Unlocked by beating the Fluffy Shark.</details> |

## Tier A - 7 guns

| Gun | Damage | Damage/s | Magazine | Total ammo | What it does |
|-----|-------:|---------:|---------:|-----------:|--------------|
| Big Magnum | 16 | 32 | 6 | 72 | Big fast piercing shots |
| Rainbow Ray | 2.8 | 43 | 40 | 480 | Super fast stream of piercing rainbow bullets |
| Party Rocket | 12 + 34 blast | 51 | 3 | 36 | Rockets speed up and blow up on what they hit |
| Long Shot | 45 | 50 | 4 | 36 | Sniper rifle with a laser sight, pierces everything in a line |
| Dust Bunny | 6 a bite (4/s) + 26 burst | ~24 + burst | 2 | 24 | Drifts out, sucks enemies in, chews on them and bursts |
| Polaroid | 28 | 31 | 1 | 14 | Camera flash: wipes out bullets in front of you, hurts all it catches |
| Chomper | 60 | 40 (with reload) | 1 | 50 | A homing shark that bites hard; faster under water |
