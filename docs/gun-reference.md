# Guns

[Documentation index](README.md)

Every gun in the game, sorted by tier (the chest it comes out of: D brown, C blue, B green, A red, S black).
Numbers come from [Weapon.java](../src/main/java/com/trush/game/objects/Weapon.java) - update this file when a gun changes.
Inventory, firing and save behavior are described in [weapons](weapons.md).

- **Damage** - per bullet / hit (x N = pellets per shot). Hot Sauce adds +25%.
- **Damage/s** - damage per second for repeated shots assuming every pellet hits, excluding reloads unless shown.
- **Magazine** - shots before a reload. **Total ammo** - all the ammo it carries, magazine included (INF = never runs out).
- Finding a gun you already carry upgrades it (+15% damage, +25% magazine/ammo or +15% bullet speed).

## How many in each tier

| Tier | Guns |
|------|-----:|
| Starter | 6 |
| D | 11 |
| C | 17 |
| B | 11 (one unlockable) |
| A | 10 |
| S | 5 |
| **Total** | **60** (53 in the base chest pool, one unlockable gun and six starters) |

## Starters (never in chests)

| Gun | Damage | Damage/s | Magazine | Total ammo | What it does |
|-----|-------:|---------:|---------:|-----------:|--------------|
| Rusty Sidearm | 5 | 25 | 10 | INF | The boy's starter: reliable single shots |
| Throwing Stars | 4 | 24 | 10 | INF | The sister's starter: quick spinning stars |
| Pearl Revolver | 6.5 | 24 | 6 | INF | Granny's starter: an antique revolver, six heavy shots, slow reload |
| Old Double-Barrel | 3 x 6 | 60 (22 with reload) | 2 | INF | Grandad's starter: two blasts, then he cracks it open; with his +1 about as good as the Fluff Shotgun |
| Baby Rattle | 2.2 | 20 | 24 | INF | The baby's starter: a quick spray of confetti beads |

## Tier D - 11 guns

| Gun | Damage | Damage/s | Magazine | Total ammo | What it does |
|-----|-------:|---------:|---------:|-----------:|--------------|
| Confetti Popper | 2.6 x 12 | 62 | 4 | 80 | Short-range burst of party-coloured pellets |
| Bubble Blaster | 8.5 | 30 | 14 | 160 | Slow wobbly bubbles, go through enemies and shove hard; wet |
| Laser Pointer | 1.4 | 28 | 120 | 720 | Thin red beam, hits instantly along 4.5 units |
| Water Pistol | 2.8 | 35 | 30 | 420 | Fast short stream of droplets, shoves hard; wet |
| Snowball Slinger | 7 | 25 | 10 | 120 | Wet snowballs slow enemies down and build frost until they freeze |
| Slingshot | 5.25–15 | Depends on charge | 8 | 100 | Hold to pull back. Full power takes 0.5 s |
| Popcorn Popper | 5 (+3 puffs x 2.5) | 17 + puffs | 12 | 150 | Kernels pop in flight or on impact, sending puffs forward that can also hit the original target |
| Hair Dryer | 1.4 x 2 | 40 | 60 | 900 | Short hot-air stream: shoves enemies back, blows smoke and stink clouds away |
| Cap Gun | 4 (BANG 20) | 27 | 12 | 180 | Little clicks, every sixth shot a big BANG |
| Pea Shooter | 3 x 3 | 23 | 10 | 120 | Each pull spits three peas one after another |
| Stinky Sock Launcher | 7.5 | 17 (+stink) | 6 | 90 | Smelly socks leave a cloud where they land that poisons enemies (never you) |

## Tier C - 17 guns

| Gun | Damage | Damage/s | Magazine | Total ammo | What it does |
|-----|-------:|---------:|---------:|-----------:|--------------|
| Fluff Shotgun | 4.5 x 6 | 42 | 6 | 110 | Short-range spread, big knockback |
| Banana-rang | 18 | 45 per pass | 5 | 70 | Pierces on both passes, returning after about 3.5 units or a wall hit. The curved banana knocks back enemies and briefly stuns regular foes |
| Super Bouncer | 8.5 | 28 | 10 | 120 | Rubber balls bounce off walls and cover 3 times |
| Plasma Zapper | 7 | 32 | 10 | 150 | Fast green laser bolts |
| Pillow Cannon | 12 | 24 | 7 | 70 | Slow pillow bursts into 8 feathers (~4 damage each) where it hits |
| Jumper Cables | 8.5 | 27 | 12 | 140 | A spark that jumps on to 3 more enemies |
| Acid Squirter | 2.4 | 27 | 40 | 360 | Short acid stream; the acid keeps eating at what it hits |
| Flare Gun | 13 | 26 | 7 | 60 | Sets what it hits on fire, leaves a patch of flames |
| Freeze Ray | 0.9 | 18 | 100 | 600 | Cold beam: piles ice on enemies until they freeze, ices the floor |
| Sticky Darts | 6.5 (+19.5 pop) | 42 | 9 | 120 | Darts stick in enemies; the third pops them all for extra damage |
| Kazoo | 5.5 x 3 | 39 | 10 | 120 | A slow, wobbly fan of music notes that go through enemies |
| Megaphone | 14 | 25 | 6 | 80 | A wide shout that clears bullets, shoves enemies and stuns regular enemies. Their next volley is slower |
| Paper Planes | 10 | 33 | 10 | 140 | Gliding planes that turn after wherever you aim, even in flight |
| Tennis Ball Launcher | 7 | 20 (+bounces) | 8 | 120 | Balls bounce off walls and enemies 4 times, faster every bounce |
| Crayon Box | 7 | 32 | 12 | 160 | Every shot a random crayon: red burns, blue chills, green poisons |
| Pinwheel | 7.5 (+8 petals x 4.5) | 17 + petals | 6 | 80 | Whirls out, stops and spins, then throws off a ring of petals |
| Nail Gun | 2.6 | 24 | 30 | 360 | A rapid, straight stream of piercing nails |

## Tier B - 11 guns (one unlockable)

| Gun | Damage | Damage/s | Magazine | Total ammo | What it does |
|-----|-------:|---------:|---------:|-----------:|--------------|
| Pocket Uzi | 2.2 | 34 | 32 | 400 | Very fast, wide spray |
| Bee Hive | 3 x 3 | 30 | 15 | 150 | Bees wiggle and home in on enemies |
| Gull Gatling | 2.4 | 48 | 150 | 900 | Slow to start, then a storm of bullets (48/s once spun up) |
| Egg Lobber | 22 blast | 37 | 4 | 48 | Hold to charge, lobs eggs over cover that burst on landing |
| Frying Pan | 20 | 48 | melee | INF | Swing hits what's close and bats enemy bullets back |
| Porcupine | 4 x 3 (+10 all round) | 29 at aim | 10 | 120 | Quills where you aim plus a ring of 10 all around you |
| Rubber Duck Launcher | 6 (+8 burst) | 35 | 6 | 72 | Ducks waddle off toward enemies and burst with a squeak; faster under water |
| Gloop Launcher | 12 | 20 | 5 | 50 | Glob of acid bursts into a pool that melts enemies |
| Boom-buss | 4 x 7 (+5 blast each) | up to 105 | 2 | 40 | Blunderbuss of little bombs, each blasts where it hits |
| Kitty Cannon | 7 | 22 + rebounds | 9 | 126 | Fires homing kittens that pounce and rebound to other enemies twice |

## Tier A - 10 guns

| Gun | Damage | Damage/s | Magazine | Total ammo | What it does |
|-----|-------:|---------:|---------:|-----------:|--------------|
| Big Magnum | 16 | 32 | 6 | 72 | Big fast piercing shots |
| Rainbow Ray | 2.8 normal / 2.9 hard | 43 normal / 45 hard | 40 | 480 normal / 900 hard | Super fast stream of piercing rainbow bullets |
| Party Rocket | 12 + 34 blast | 51 | 3 | 36 | Rockets speed up and blow up on what they hit |
| Long Shot | 45 | 50 | 4 | 36 | Sniper rifle with a laser sight, pierces everything in a line |
| Dust Bunny | 6 a bite (4/s) + 26 burst | ~24 + burst | 2 | 24 | Drifts out, sucks enemies in, chews on them and bursts |
| Polaroid | 28 | 31 | 1 | 14 | Camera flash: wipes out bullets in front of you, hurts all it catches |
| Chomper | 60 | 40 (with reload) | 1 | 50 | A homing shark that bites hard; faster under water |
| Heirloom | 4 to 3.6 x 3 | 16 to 77 | 8 to 32 | 700 | Levels up at 40/120/260/480 kills made while carrying it. Gains piercing and extra shots, then homing at level 5. Level carries across runs. Grandad's kills count double |
| Bottle Rocket Rack | 5 x 4 (+6 pop each) | 73 | 5 | 60 | Each pull sends off four little homing rockets one by one |
| Fluff Cannon | 32 | 46 (hits everything in its way) | 3 | 45 | A giant pom-pom rolls slowly through enemies and bounces off walls 3 times |

## Tier S - 5 guns (black chests only)

From floor 3 on, a treasure chest - or the boss chest after a fight without a single hit - comes out black 1 time in 12. Baa-llo never sells S guns.

| Gun | Damage | Damage/s | Magazine | Total ammo | What it does |
|-----|-------:|---------:|---------:|-----------:|--------------|
| Unicorn Horn | 4 | 80 | 200 | 1000 | A long, thick rainbow beam that burns through everything in its way |
| Shooting Star | 20 (+30 blast, +8 x 8 stars) | 36 + blasts | 6 | 72 | A homing star that blows up and bursts into eight smaller stars |
| Disco Ball | 3 a beam | ~100 round the ball | 3 | 36 | Thrown out, it hangs in the air for 2.4 s sweeping four rainbow beams round, through everything |
| Teapot Tempest | 4 | 67 + burning | 80 | 640 | A stream of scalding steam that sets enemies alight; every fifth puff, lightning jumps to 3 more |
| Meteor Staff | 66 blast | 183 | 8 | 110 | Calls a meteor down where you aim: a golden ring, then a crash that also wipes enemy bullets |

<details>
<summary><b>Spoilers</b> (unlockable guns and all synergy recipes)</summary>

| Gun | Tier | Damage | Damage/s | Magazine | Total ammo | What it does |
|-----|------|-------:|---------:|---------:|-----------:|--------------|
| Flufful | B | 5.5 | 34 | 24 | INF | The talking gun, unlocked by beating the Fluffy Shark. Its homing shots grow stronger at 20/60/120/200 kills |
| Royal Sceptre | Starter | 5 (+6 x 1.3) | Up to 40 | 8 | INF | The pocket-size Fluffy King's starter. Six fragments converge on the enemy nearest the burst and can hit the original target |

The [synergy reference](synergies.md) lists all six recipes and their effects.

</details>
