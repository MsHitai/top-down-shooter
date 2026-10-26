# Developing Enter the Fluffy

Spoiler warning: this page mentions the game's secrets.

### Build and run

    mvn clean package
    java -jar target/game-0.9.0-SNAPSHOT-jar-with-dependencies.jar

or run `com.trush.game.launcher.AppLauncher` from the IDE. Java 21, libGDX 1.13.5 (LWJGL3), Maven.

### Guns and items

The full list (the README only sums them up, since there'll be more). Every gun and item has a tier,
D to A; chests and the shop hand them out by tier. Guns are static factories in `objects/Weapon`
(register new ones in `Weapon.byName` and `Weapon.chestGuns`), items are the `objects/Item` enum.

- **Starter**: Rusty Sidearm (the sister: Throwing Stars - never in chests; grandad's sidearm is +1)
- **Guns**
  - D: Confetti Popper, Bubble Blaster, Laser Pointer (an instant beam), Water Pistol (a shoving stream
    of droplets), Snowball Slinger (slows what it hits)
  - C: Fluff Shotgun, Banana-rang (a boomerang), Super Bouncer (bouncing balls), Plasma Zapper, Pillow
    Cannon (bursts into feathers), Jumper Cables (a spark that jumps between enemies), Acid Squirter (a
    short green stream; acid keeps eating at what it hits), Flare Gun (sets on fire, leaves flames where
    it lands)
  - B: Pocket Uzi, Bee Hive (homing bees), Gull Gatling (spins up), Egg Lobber (hold to lob further),
    Frying Pan (melee: bats enemy bullets back), Porcupine (quills all round, more where you aim), Gloop
    Launcher (a glob that bursts into a pool of acid enemies melt in), Boom-buss (a blunderbuss spraying
    little bombs that go off where they hit)
  - A: Big Magnum, Rainbow Ray, Party Rocket, Long Shot (a sniper rifle with a laser sight), Dust Bunny
    (sucks enemies in, chews on them, bursts), Polaroid (a flash that wipes out bullets in the picture), Chomper
    (one shot, then a reload: a little shark that swims at the nearest enemy and bites; faster under water)
  - B, unlockable: **the Flufful** - a talking gun like ETG's Gunther, endless ammo, levels up with kills;
    unlocked for good by beating the Fluffy Shark (the secret sea, or endless wave 30 - endless only floods at 26-30 once the sea has been found in a story run)
- **Passive items**
  - D: Fluffy Slippers, Spare Pocket, Bouncy Socks (longer, faster rolls), Acid Boots (acid puddles
    can't hurt you)
  - C: Fluff Field (pushes enemies away), Bigger Clips, Fluffy Heart, Treasure Map (the whole floor on
    the map; story only), Bike Helmet (takes a hit, back on after the fight), Frost Rounds (1 bullet in 5
    freezes regular enemies solid; bosses only chill), Ember Rounds (1 bullet in 5 sets on fire)
  - B: Hot Sauce, Lucky Clover, Static Fuzz (zaps enemies that come close), Prickly Sweater (a hit sprays
    quills), Pom-Pom Moons (three pom-poms orbit you, stop enemy bullets and bump enemies)
  - A: Bubble Shield (recharges as you deal damage), Ghost Bullets, Little Cloud (a cloud friend that
    throws lightning), Guardian Lamb (the owner's idea, after ETG's Pig: saves you once from a fatal hit,
    back to full health)
  - shop only: the Flippers (Baa-llo's city shop: bubbles after every roll, and the way into the secret sea)
- **Active items** (one at a time, F / RT)
  - D: Med Kit, Cardboard Hero (a decoy regular enemies shoot at for 6 s; recharges)
  - C: Toy Robot (a wind-up turret that fights for 8 s; recharges)
  - B: Grenade (recharges; hold to throw further), Snow Globe (enemies and their bullets slow down for 5 s;
    recharges)
  - A: Fluffy Candy (full health)
  - the Med Kit, Fluffy Candy and Grenade were the owner's son's ideas

### Fire and acid

Like ETG's goop and status effects. `objects/Puddle` is a patch of FIRE or ACID on the floor
(`GameWorld.spillPuddle`, cleared on leaving the room, at most 30): a dynamite leaves flames where it
blew up, the Flare Gun where its flare stops, the Gloop Launcher and the Sludger's globs leave acid.
Walking into enemy fire sets the hero alight (`Player.updateHazards`: a hit after `BURN_DELAY`, then
every 1.5 s until it burns out), standing in acid bites after `ACID_DELAY` (not with the Acid Boots); a
dodge roll is in the air - it passes over both and puts the flames out. The player's own puddles only
hurt enemies. Enemies: `Enemy.ignite` / `poison` (damage over time, dealt by `GameWorld.updatePuddles`
through `damageEnemy`) and `freeze` (no `behave`, no contact damage; fire melts it; bosses only get
chilled); walking enemies catch fire / get eaten by acid in any puddle (`Enemy.acidProof`: the
Sludger). Nothing burns in the sea or a flooded endless arena (`GameWorld.canBurn`). The Sludger (a
hazmat goon lobbing acid globs) is rare in the town, city and skyscraper (`StoryDirector.SLUDGER_CHANCE`)
and in endless from wave 7; its real home is the planned secret district (ROADMAP).

### Dev builds and shortcuts

Debug flags only work in a dev build - `mvn clean package -Pdev` (running from the IDE counts as dev too);
a normal build ignores them. Pass them as JVM options, e.g.
`java -Dfluffy.floor=3 -Dfluffy.bossRoom=true -jar target/...`:

- `-Dfluffy.god=true` - hits never cost health
- `-Dfluffy.allGuns=true` - start with every gun
- `-Dfluffy.items=all` or `-Dfluffy.items=fluff_field,grenade` - start with these items
- `-Dfluffy.floor=N` - a new story run starts on floor N (2 = the town, 3 = the city, 4 = the skyscraper,
  5 = the Fluffy Kingdom, 6 = the secret sea)
- `-Dfluffy.sea=true` - a story run dives straight into the secret sea (with `-Dfluffy.bossRoom=true`:
  into the Fluffy Shark's lair)
- `-Dfluffy.play=story` / `-Dfluffy.play=endless` - skip the main menu and start right away; `-Dfluffy.play=home` - straight into the living room
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
  soundtrack: menu, village, town, city, tower, sky, sea, and the boss themes boss (the Fluffy King),
  boss_village, boss_town, boss_city, boss_tower, boss_sea. Needs numpy, ffmpeg and FluidSynth
  (`brew install fluid-synth`); uses `musiclib.py` from the Towards Dreams project. Delete the leftover `.wav`/`.mid` files afterwards.

### Room layouts

New room layouts: add an 8x13 string grid to `RoomTemplates.COMBAT` (it's centred in bigger rooms).

Notes for coding agents (code map, conventions) are in [AGENTS.md](AGENTS.md).
