# ENTER THE FLUFFY

A top-down bullet-hell shooter in the spirit of *Enter the Gungeon*, written in Java 21 with libGDX
(LWJGL3 desktop backend) and built with Maven. Keyboard + mouse or an Xbox controller.

### How to run

    mvn clean package
    java -jar target/game-0.6.0-SNAPSHOT-jar-with-dependencies.jar

or run `com.trush.game.launcher.AppLauncher` from the IDE.

### Modes

- **Story**: it opens with the story of how it all began (a picture book - skippable, and "THE STORY" in
  the main menu replays it). Then floor after floor, each a new random map. Walking into a combat room locks the doors until
  every enemy is dead. Find chests, keys and the treasure room, then beat the floor's boss and take
  the way on it opens (a gate, a bus stop, the tower's doors, a helicopter). Like ETG you can only save
  **between floors** (on the floor-clear screen); *Continue* in the main menu picks the run up on the next floor.
  1. **The Village** - cottages, yards, barns, orchards
  2. **The Town** - streets, a market, alleys, a park, open sewer hatches to fall into
  3. **The City** - at night: avenues, a subway, rooftops, a construction site; lamps light the dark
  4. **The Skyscraper** - an elevator climb: offices, server rooms, a cafeteria, floor after floor up to
     the roof, where a giant waits; a helicopter flies you up into the clouds
  5. **The Fluffy Kingdom** - cloud islands in the open sky: pearly gates, a cloud temple, storm clouds,
     holes to fall through, and the Fluffy King on his throne. Beat him to win the run.
- **Endless**: one arena, waves forever. Each stage's enemies join in turn, and every 5th wave is a boss
  wave that climbs the stages (wave 5 a village boss, 10 town, 15 city, 20 skyscraper, 25 the Fluffy King,
  then anyone). A boss wave heals you, gives a heart container and an item, and new furniture replaces
  what was shot to pieces. Then Baa-llo sets up his travelling shop in the arena: ring the bell on his
  counter when you're ready for the next wave.

Rooms come in sizes: small one-screen rooms (in the town and city: "trap" rooms full of pits), roomy
combat rooms and big open boss arenas - the camera follows you and leans toward where you aim.

### Controls

| Keyboard + mouse | Xbox controller | Action |
| --- | --- | --- |
| WASD / arrows | left stick | move |
| mouse | right stick | aim |
| hold left button | RB | shoot |
| Space / right button / Left Shift | LB | dodge roll (invincible for most of it; roll over tables, squish rats) |
| E | A | open a chest / take the exit / flip a table |
| R | X | reload |
| Q | R3 (click the right stick) | blank: clears every enemy bullet and dazes enemies for a moment |
| F | RT | use your active item (med kit, candy; hold to throw the grenade further) |
| wheel / Tab / 1-9 | tap Y / hold Y | switch gun / gun wheel |
| hold M | hold LT | map: teleport to any room you've visited (not during a fight) |
| Esc / P | Start | pause |
| N | | toggle music |

### What's in it

- **Guns** - start with the Rusty Sidearm; the rest come out of chests, by tier:
  - D: Confetti Popper, Bubble Blaster, Laser Pointer (an instant beam)
  - C: Fluff Shotgun, Banana-rang (a boomerang), Super Bouncer (bouncing balls), Plasma Zapper
  - B: Pocket Uzi, Bee Hive (homing bees), Gull Gatling (spins up), Egg Lobber (hold to lob further)
  - A: Big Magnum, Rainbow Ray, Party Rocket, Long Shot (a sniper rifle with a laser sight)
  - and **the Flufful** (B), a talking gun like ETG's Gunther - a fluffy pink pistol with a sweet shark
    on top that chats between fights and grows with every kill. Unlock it by beating the Fluffy King
    without taking a hit.

  Finding a gun you already have upgrades it instead ("Pocket Uzi +1").
- **Chests and keys** - brown, blue, green and red chests (D-A); a chest holds a gun or an item of its
  tier, or else hearts, ammo, blanks and keys. Locked chests cost a key; each floor guarantees one.
- **Coins and Baa-llo's shop** - enemies, cleared rooms, chests and bosses drop coins (like ETG's shell
  casings). Every floor has a shop (a coin on the map) run by **Baa-llo**, a woolly sheep in a red cap
  and a monocle: two guns or items, a heart, a key and a blank or an ammo box, one of them on sale. Walk
  up to a stand to see the price, press E / A to buy. Guns and items cost 25 / 45 / 70 / 100 coins by tier
  (D-A), hearts and blanks 15, ammo 20, keys 30, and 10% more each floor. Don't shoot the shopkeeper:
  three hits and he holds a grudge - prices up by half for the rest of the run.
- **Items** - passive: Fluffy Slippers, Spare Pocket, Magnet, Bigger Clips, Fluffy Heart, Hot Sauce,
  Lucky Clover, Bubble Shield (recharges as you deal damage), Ghost Bullets. Active (one at a time, RT / F): Med Kit, Fluffy Candy (full health; the
  son's idea, like the Med Kit and the Grenade), Grenade (recharges).
- **Hearts** - half hearts drop now and then; **heart containers** (+1 max heart) come from chests, the
  Fluffy Heart, endless boss waves, or beating a boss **without taking a single hit**. Up to 16 hearts:
  past 8 they turn golden and sit on top of the red ones.
- **Cover** - flip tables, hide behind pillars, barrels, cars; most furniture can be shot to pieces.
  Cover only protects you in the direction it really covers.
- **Enemies** - village: Bullet Kin, Shotgun Kin, Bulletmancer. Town: Hydrant, Rats, Shell Cop,
  Shapeshifter. City: Biker, Sniper, Robber (dodge-rolls your bullets). Skyscraper: RoboCart, Android,
  Agent, Mountain and Lead Cubes. Fluffy Kingdom: Sword Angel, Bow Angel, Thundercloud. Everywhere: the Dynamite (shoot it before it reaches you). And, very
  rarely, a street lamp that isn't a lamp at all.
- **Bosses** (one per floor, rolled from the stage's three):
  - Village: the Haymaker, the Harvestman, the Hollow Witch
  - Town: the Trash That Can, the Road Rager, the Traffic Light
  - City: the Night Hawk, the Pigeon King, the Evil Train
  - Skyscraper (on the roof): the Kaiju (a giant beyond the roof's edge, like ETG's Dragun), the Mega
    Mech, the Messenger of the Fluffy King
  - and the **Fluffy King**, the final boss, on his throne in the Fluffy Kingdom (floor 5)

The Trash That Can, the Road Rager and the Evil Train were designed by the owner's son, who also drew
the Kaiju and the Messenger's sword attack, and came up with the Fluffy Kingdom and the Walking Building
- see [ROADMAP.md](ROADMAP.md) for what's next. The Flufful's look was drawn by the owner.

### Dev builds and shortcuts

Debug flags only work in a dev build - `mvn clean package -Pdev` (running from the IDE counts as dev too);
a normal build ignores them. Pass them as JVM options, e.g.
`java -Dfluffy.floor=3 -Dfluffy.bossRoom=true -jar target/...`:

- `-Dfluffy.god=true` - hits never cost health
- `-Dfluffy.allGuns=true` - start with every gun
- `-Dfluffy.items=all` or `-Dfluffy.items=magnet,grenade` - start with these items
- `-Dfluffy.floor=N` - a new story run starts on floor N (2 = the town, 3 = the city, 4 = the skyscraper,
  5 = the Fluffy Kingdom)
- `-Dfluffy.play=story` / `-Dfluffy.play=endless` - skip the main menu and start right away
- `-Dfluffy.bossRoom=true` - a new story run starts at the boss
- `-Dfluffy.retryBoss=true` - "try again" goes straight back to the boss
- `-Dfluffy.boss=evil_train` - always fight this boss (names are forgiving, e.g. `night hawk`)
- `-Dfluffy.keepSave=true` - continuing a saved run doesn't use the save up
- `-Dfluffy.mimic=true` - every town / city combat room hides a mimic lamp
- `-Dfluffy.startWave=N`, `-Dfluffy.layout=N` - endless mode: start wave, arena layout
- `-Dfluffy.shop=true` - a story run starts in Baa-llo's shop; endless opens his stall before wave 1
- `-Dfluffy.coins=N` - start with N coins

A dev build shows "DEV BUILD" in the main menu.

### Generated assets

Everything except the font is generated by scripts in `tools/` - tweak and re-run them rather than
editing the files:

- `python3 tools/gen_sprites.py` - hero, guns, items, furniture, chests and every room tileset (needs Pillow)
- `python3 tools/gen_sfx.py` - sound effects (needs numpy; ffmpeg for the death cry)
- `python3 tools/music/tracks.py <GeneralUser-GS.sf2> src/main/resources/assets/music [track ...]` - the
  soundtrack: menu, village, town, city, tower, and the boss themes boss (the Fluffy King), boss_village,
  boss_town, boss_city, boss_tower. Needs numpy, ffmpeg and FluidSynth (`brew install fluid-synth`); uses
  `musiclib.py` from the Towards Dreams project. Delete the leftover `.wav`/`.mid` files afterwards.

New room layouts: add an 8x13 string grid to `RoomTemplates.COMBAT` (it's centred in bigger rooms).
Notes for contributors (and coding agents) are in [AGENTS.md](AGENTS.md).
