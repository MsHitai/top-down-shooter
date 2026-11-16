# Developing Enter the Fluffy

Spoiler warning: this page mentions the game's secrets.

### Build and run

    mvn clean package
    java -jar target/game-0.14.0-SNAPSHOT-jar-with-dependencies.jar

or run `com.trush.game.launcher.AppLauncher` from the IDE. Java 21, libGDX 1.13.5 (LWJGL3), Maven.

### Guns and items

The full list (the README only sums them up, since there'll be more). Every gun and item has a tier,
D to A; chests and the shop hand them out by tier. Guns are static factories in `objects/Weapon`
(register new ones in `Weapon.byName` and `Weapon.chestGuns`), items are the `objects/Item` enum.
Stat tables by tier (damage, ammo, counts): [GUNS.md](GUNS.md) and [ITEMS.md](ITEMS.md) - keep them in step.

- **Starter**: Rusty Sidearm (the sister: Throwing Stars; granny: Pearl Revolver - six heavier shots, slow reload;
  grandad: Old Double-Barrel - two 6-pellet blasts, a good D-level gun that his +1 brings up to about the Fluff Shotgun;
  the baby: Baby Rattle - 2.2-damage confetti beads, 12-degree spread, 24 a magazine, sound `rattle`;
  starters are never in chests)
- **Guns**
  - D: Confetti Popper, Bubble Blaster, Laser Pointer (an instant beam), Water Pistol (a shoving stream
    of droplets), Snowball Slinger (slows what it hits; five or so in a row freeze it solid), Slingshot (hold to
    pull back: a flick or a hard fast pebble - `Weapon.slings`), Popcorn Popper (kernels pop into three puffs -
    `splits` with `splitShape` CLOUD), Hair Dryer (hot air: little damage, shoves enemies, blows gas away -
    `Bullet.blowsGas`), Cap Gun (every sixth shot a BANG for 5x - `bangEvery`), Pea Shooter (three peas a
    pull, one after another - `burst`/`Bullet.delay`), Stinky Sock Launcher (socks let out a cloud of
    `GasCloud.Kind.SOCK` where they land - `Bullet.stinks`; it poisons enemies in it, never the hero)
  - C: Fluff Shotgun, Banana-rang (a big boomerang: 18 damage out and again back, a hard shove, each hit bonks a regular enemy dizzy `GameWorld.BANANA_BONK` 0.5 s), Super Bouncer (bouncing balls), Plasma Zapper, Pillow
    Cannon (bursts into feathers), Jumper Cables (a spark that jumps between enemies), Acid Squirter (a
    short green stream; acid keeps eating at what it hits), Flare Gun (sets on fire, leaves flames where
    it lands), Freeze Ray (a cold beam that piles ice on enemies until they freeze solid, and ices the floor),
    Sticky Darts (the third dart in an enemy pops them all for 3x a dart - `Bullet.sticky`,
    `GameWorld.stickDart`; they fall out after 4 s), Kazoo (a wobbly fan of piercing music notes, `Shape.NOTE`),
    Megaphone (no bullets, a shout in a wide cone - `Weapon.shout` -> `GameWorld.shout`: a blank you aim -
    enemy bullets in the cone are blown away, every enemy in it (`SHOUT_RANGE` 4) takes 14 damage, is shoved back (`SHOUT_PUSH`) and regular ones are stunned
    `SHOUT_STUN` 1 s (`Enemy.shoutedAt`, drawn dazed like after a blank), and the next volley a shouted-at
    enemy fires within 3 s flies at half speed (`Bullet.hush`), bosses' too - they're never stunned; sfx `megaphone`), Paper Planes (go for the enemy nearest the aim line, round cover too -
    `Bullet.guided`, `GameWorld.steerPlane`/`PLANE_CONE`, `Shape.PLANE`), Tennis Ball Launcher (bounce off walls and enemies, faster each time -
    `Bullet.rebounds`/`bounceBoost`), Crayon Box (each bullet a random crayon: red `burn`, blue `chill`, green
    `acid` - `Weapon.crayons`), Pinwheel (slows to a stop about 2.5 units out and spins - `Weapon.stops` - then bursts into
    `Shape.PETAL`s), Nail Gun (a quick straight stream of piercing nails, `Shape.NAIL`)
  - B: Pocket Uzi, Bee Hive (homing bees), Gull Gatling (spins up), Egg Lobber (hold to lob further),
    Frying Pan (melee: bats enemy bullets back), Porcupine (quills all round, more where you aim), Gloop
    Launcher (a glob that bursts into a pool of acid enemies melt in), Boom-buss (a blunderbuss spraying
    little bombs that go off where they hit), Rubber Duck Launcher (homing ducks that burst with a squeak,
    faster under water - `Shape.DUCK`), Kitty Cannon (Egor's idea: homing kittens that pounce, scratch and
    spring off at the next enemy twice - homing + `rebounds`, `Shape.KITTEN`, sfx `meow`)
  - A: Big Magnum, Rainbow Ray, Party Rocket, Long Shot (a sniper rifle with a laser sight), Dust Bunny
    (sucks enemies in, chews on them, bursts), Polaroid (a flash that wipes out bullets in the picture), Chomper
    (one shot, then a reload: a little shark that swims at the nearest enemy and bites; faster under water),
    Heirloom (our Gunderfury: weak at first, grows from every kill made while carried - any gun - up to
    level 5 at 40/120/260/480 kills, kept for good between runs (prefs `heirloomKills`); grandad's kills count double; 700 ammo (endless until 0.12 - too strong next to the Flufful); sprite `heirloom_N`),
    Bottle Rocket Rack (four little homing rockets a pull, after the Yari Launcher), Fluff Cannon (a giant
    pom-pom that rolls through enemies and bounces off walls, `Shape.POMPOM`)
  - S (black chests only, never in the shop): Unicorn Horn (a thick rainbow beam, 80 damage/s through
    everything), Shooting Star (a homing star that blows up into eight smaller ones), Disco Ball
    (`Weapon.disco` -> `Bullet.disco`: it stops about 2.9 units out - against cover sooner - and `GameWorld.updateDisco` sweeps
    four rainbow beams round every `DISCO_EVERY`), Teapot Tempest (burning steam, `chainEvery` 5: every
    fifth puff carries a `chain` spark), Meteor Staff (`Weapon.meteor` -> `GameWorld.callMeteor`: a
    `Meteor.friendly` on the aim - on a pad (`GameWorld.padAim`) on the enemy nearest the aim line, else
    `METEOR_PAD_REACH` out - `explode` where it lands). From floor 3 a
    treasure chest - and the boss chest after a flawless fight - comes out black 1 time in 12
    (`StoryDirector.blackChest`) while an S gun or item is left; endless hands them out last
  - B, unlockable: **the Flufful** - a talking gun like ETG's Gunther, endless ammo, levels up with kills;
    unlocked for good by beating the Fluffy Shark (the secret sea, or endless wave 30 - endless only floods at 26-30 once the sea has been found in a story run)
- **Passive items**
  - D: Fluffy Slippers, Spare Pocket, Bouncy Socks (longer, faster rolls), Acid Boots (acid puddles
    can't hurt you), Quick Mittens (faster reloads, `Player.reloadScale`), Piggy Bank (some coins count
    double), Toy Telescope (steadier single shots, `Player.spreadScale`), Guard Moon (a big moon on the outer
    ring that blocks bullets), Mirror Moon (bats the bullets it touches back as yours)
  - C: Fluff Field (pushes enemies away), Bigger Clips, Fluffy Heart, Treasure Map (the whole floor on
    the map; story only), Bike Helmet (takes a hit, back on two fights later), Frost Rounds (1 bullet in 5
    freezes regular enemies solid; bosses only chill), Ember Rounds (1 bullet in 5 sets on fire), Rubber
    Rounds (plain bullets bounce once, `GameWorld.addPlayerBullet`), Fluffy Backpack (a second active slot,
    `Player.bagged`: d-pad left / right or G swaps via `GameWorld.swapActiveItem`, the bagged cooldown keeps
    ticking, `useUpActive` brings it out; saved as `RunSave.baggedItem`), Blue Moon (a hit stops time for
    1.6 s - `GameWorld.timeStop`: enemies don't `update`, their bullets don't move; 20 s recharge), Key
    Ring (+2 keys, +1 every floor - `RunSave.startFloor` and `GameWorld.enterSlums`)
  - B: Hot Sauce, Lucky Clover, Static Fuzz (zaps enemies that come close), Prickly Sweater (a hit sprays
    quills), Pom-Pom Moons (three pom-poms orbit you, stop enemy bullets and bump enemies), Puppy Rounds
    (plain bullets home in), Sweet Tooth (half a heart every 20 kills, `Player.sweetToothKill`), Grumpy
    Teddy (a hit makes you angry for 6 s: +50% damage, quicker reloads - `Player.loseTemper`/`isAngry`), Bobby
    Pin (picks `Player.BOBBY_PIN_USES` 5 locks, then snaps - `Player.usePin`, saved as `RunSave.pinUses`)
  - A: Bubble Shield (recharges as you deal damage), Ghost Bullets, Little Cloud (a cloud friend that
    throws lightning), Guardian Lamb (the owner's idea, after ETG's Pig: saves you once from a fatal hit,
    back to full health), Fizzy Soda (a reload clears bullets around you, `GameWorld.onReload`), Gold Star
    Rounds (damage grows with your coins), Jingly Keychain (and with your keys, `Player.damageScale`), Comet Skates (rolls leave friendly fire), Dream Catcher (our Clone: dying wakes you up on floor 1 with everything, every gun full again (`RunSave.GunData.full`) -
    `RunSave.wakeUp`, `GameScreen.wakeUp`; not in endless)
  - S (black chests only, like S guns): Triple Scoop (every bullet comes out as three, `Weapon.tryFire`),
    Family Photo (damage grows with the guns you carry), Bottomless Bag (`Weapon.doubleAmmo`, wherever
    Bigger Clips is applied), Cozy Blanket (`GameWorld.updateGadgets`: a roll clears enemy bullets within
    `BLANKET_RADIUS`), Lucky Monocle (`GameWorld.addPlayerBullet`: `MONOCLE_CHANCE` of bullets do
    `MONOCLE_DAMAGE`x)
  - shop only: the Flippers (Baa-llo's city shop: bubbles after every roll, and the way into the secret sea)
- **Active items** (one at a time, F / RT)
  - D: Med Kit, Cardboard Hero (a decoy regular enemies shoot at for 6 s; recharges), Packed Lunch
    (+1 heart, or eaten by itself on a fatal hit - before the Guardian Lamb)
  - C: Toy Robot (a wind-up turret that fights for 8 s; recharges), Star Plushie (3 s of invincibility; recharges)
  - B: Grenade (recharges; hold to throw further), Snow Globe (enemies and their bullets slow down for 5 s;
    recharges), Bubble Wand (a free blank; recharges)
  - A: Fluffy Candy (full health), Sugar Rush (6 s faster and +50% damage; recharges)
  - the Med Kit, Fluffy Candy and Grenade were Egor's ideas

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
Sludger, the Acid Sprayer, the Barrel Roller). Nothing burns in the sea or a flooded endless arena (`GameWorld.canBurn`). The Sludger (a
hazmat goon lobbing acid globs) is rare in the town, city and skyscraper (`StoryDirector.SLUDGER_CHANCE`)
and in endless from wave 7; its real home is the Slums, with the rest of its gang (see below).

### Freezing and ice

Enemies: `Enemy.addFrost` builds ice up (the Snowball Slinger's `Weapon.frost` 0.2 a snowball, the Freeze
Ray's beam 0.05 a tick through `GameWorld.fireBeam`); at 1 it's `freeze`d for `Enemy.BUILT_FREEZE`, and the
ice creeps up from its feet meanwhile; it thaws when left alone, fire clears it; bosses are only chilled.
More frost on a frozen enemy keeps it frozen; the ice rattles and cracks for its last `THAW_WARN`, and once
out it shakes itself off for `THAW_GRACE` and stays chilled a moment (no instant shot on thawing).
Frost Rounds still freeze at once. A frozen enemy is brittle: hits deal `GameWorld.FROZEN_DAMAGE` x1.5,
a dodge roll into one (once a roll, `Player.getRammed`) hits it for `GameWorld.ROLL_SMASH` and sends it
sliding, and one killed frozen shatters (`Enemy.shatter`, `GameWorld.shatterIce`): no death show, a ring of
`SHATTER_SHARDS` ice shards (player bullets - they can shatter the next frozen one) and a patch of ice.
The hero: enemy `Bullet.frost` bullets (`Shape.SNOWFLAKE`, the Mountain Cube's fan) don't hurt, they fill
`Player.addFrost` (a snowflake icon beside the head, `frostWarning`); full, he's frozen in a block of ice
for `Player.FROZEN_TIME` (`isFrozen`: no walking, rolling or shooting) - every button pressed chips
`BREAK_OUT` off it, a hit or fire breaks it at once (sfx `freeze` / `shatter`). Ice on the floor is
`Puddle.Kind.ICE` (the Mountain Cube's trail, shattered enemies, where the Freeze Ray lands): it hurts no
one, but the hero (`Player.setOnIce`: his walking speed only slowly follows the stick) and walking
enemies (`Enemy.onIce`, unless `slipsOnIce` is off) slide about on it. Fire and ice put each other out
in `spillPuddle`. Acid puts fire out too, and fire can't start on a pool of acid. Wet player bullets
(`Bullet.wet`: Water Pistol, Bubble Blaster, Snowball Slinger) put out fire patches they fly over and
`douse` a burning enemy they hit.

The Slums' enemies (`StoryDirector.slumsEnemy`; not in endless yet): the **Acid Sprayer** (`enemy/AcidSprayer`:
a faint arc on the floor shows the span, then it hoses a sweep of acid drops across it, every sixth
splashing into a small pool), the **Barrel Roller** (`enemy/BarrelRoller`: hefts a rusty drum and bowls it -
`Bullet.Shape.BARREL`; it bursts into a pool and a ring of drops where it stops, or early when shot), and
the **marked critters** (`enemy/Marked` draws the mark - a green-yellow ear and glowing eyes): marked rat
packs (`Rat.marked`: `MARKED_SIZE` 1.5x a town rat, too big to squish with a roll) and the **Stink Pigeon** (`enemy/StinkPigeon`: flies, spits pellets that burst into
`GasCloud.STINK`), only in deeper rooms.

Trap rooms (`world/TrapRoom`): two per Slums map (`StageMap.TRAP_ROOMS`, `Room.trap`) lock on a trap;
`StoryDirector` runs it (`GameWorld.setTrap`; it spills through `TrapRoom.Hazards`), sends in a gunner and
then rat packs (`trapWave`, every `TRAP_WAVE_GAP`), and clears the room once `TrapRoom.DURATION` is survived
and the last enemy is down (`Director.hudNote`: the countdown, then "FINISH THEM!"). ACID_FLOOD wells up acid
pools, BURST_PIPES fires jets of flame across the short way of the room; what it spilled dries up after.

### Dreams

The late game, from the kids' bedroom (right of the living room, there after any win - `HubScreen.inBedroom`):
the bed starts a dream, the calendar today's dream, the toy box the boss rush (`world/Dream`, `Dream.Kind`).
A dream is `Dream.FLOORS` 5 floors built by `StoryDirector(Dream, ...)`: each looks like a random found
stage (`pickLook`, never the sea), rolls enemies from stages up to a floor deeper (`enemyStage`), has a
beaten boss from about that deep and, on the last floor, one of the three dream bosses (`Boss.isDream`:
the Alarm Clock, the Nightmare - Egor's drawing: four wings, one eye, a crown of swords - and the Big Bad
Teddy; arena "dreamland", music "boss_dream"; never in endless). Secret ways down are removed
(`StageMap.forgetSecrets`), no rocket, no saving. Every floor has one or two twists (`Dream.Rule`, shown on
the HUD). Today's dream seeds the map and boss picks from the date; the boss rush is a start room and an
arena per boss (`StageMap.bossRush`), Baa-llo's shop beside the start after every second boss (`StoryDirector.rushShop`: floors 2, 4, 6...; not in the sea;
the Moon gets his cloud shop, `Stage.CLOUDS.shopTheme`) at floor-1 prices with an ammo box always on a stand
(`Shop.addAmmo`) and always a gun among the wares; its bosses drop `StoryDirector.RUSH_COINS` 2x coins and a brown
ammo box when a gun needs it (a green one only `RUSH_GREEN_AMMO_CHANCE` 25% of the time); a sea boss comes just before the King once the sea's been found (the Shark, or once it's beaten any of the
sea's three - `Dream.rushBosses`); its exits
say `Exit.NEXT_BOSS` / `TOY_BOX`. Records: prefs `dreams`, `bossRushes`, `dreamFastest`,
`bossRushFastest`, `dailyBest` (`ETGFanfic.finishDream`/`submitDaily`); the ending is
`screen/DreamEndingPicture`.

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
- `-Dfluffy.slums=true` - a story run starts down in the Slums (with `-Dfluffy.bossRoom=true`, at its boss)
- `-Dfluffy.undercity=true` - a story run starts down in the Undercity (with `-Dfluffy.bossRoom=true`, at the
  Skunk King)
- `-Dfluffy.enemy=raccoon` - every story fight is just that kind of enemy (names in `StoryDirector.NAMES`:
  kin, veteran, shotgun, sawn_off, riot_cop, pirate, bandana, tanker, ghost, blue_shotgun, rats, sludger, sprayer, roller, marked_rats,
  stink_pigeon, gator, dog, raccoon, spiralmancer, bloomancer, cherub, dandelion...; the Moon's: bunnies,
  asteroid, astronaut, alien, ufo, golem, worm, veteran_bunnies, veteran_asteroid, veteran_astronaut, veteran_alien - try them with `-Dfluffy.moon=true -Dfluffy.fight=true`; the sea's: flufferfish, starfish, diver, turtle, swordfish, crab, octopus - with `-Dfluffy.sea=true -Dfluffy.fight=true`)
- `-Dfluffy.win=true` - a story run opens on the victory screen (with `-Dfluffy.undercity=true`: the normal ending)
- `-Dfluffy.fight=true` - a story run starts in the floor's deepest roomy fight (to try its enemies; e.g. with
  `-Dfluffy.slums=true`)
- `-Dfluffy.slums=true -Dfluffy.trap=acid` (or `pipes`) - start in one of the Slums' trap rooms, with that trap
- `-Dfluffy.floor=3 -Dfluffy.slumsWay=true` - a city run starts in the dead end with the burning drum
  (the way down to the Slums; add `-Dfluffy.allGuns=true` for a water gun)
- `-Dfluffy.play=story` / `-Dfluffy.play=endless` - skip the main menu and start right away; `-Dfluffy.play=home` - straight into the living room
- `-Dfluffy.bossRoom=true` - a new story run starts at the boss
- `-Dfluffy.retryBoss=true` - "try again" goes straight back to the boss
- `-Dfluffy.boss=evil_train` - always fight this boss (names are forgiving, e.g. `night hawk`); the sea's `kraken` / `gorgon` with `-Dfluffy.sea=true -Dfluffy.bossRoom=true`
- `-Dfluffy.keepSave=true` - continuing a saved run doesn't use the save up
- `-Dfluffy.mimic=true` - every town / city combat room hides a mimic lamp
- `-Dfluffy.blackChest=true` - every treasure chest (and a flawless boss's chest) is a black S chest, on any floor
- `-Dfluffy.startWave=N`, `-Dfluffy.layout=N` - endless mode: start wave, arena layout
- `-Dfluffy.shop=true` - a story run starts in Baa-llo's shop; endless opens his stall before wave 1
- `-Dfluffy.coins=N` - start with N coins
- `-Dfluffy.junk=N` - start with N junk and the Junk Knight awake at level N (7 = wings and a bow)
- `-Dfluffy.gun=chomper` - start with that gun in hand (any chest gun or the Flufful, by name: `long_shot`, `party_rocket`...)
- `-Dfluffy.bossHp=N` - a story boss starts at N% of its health, to try its later phases (Grandfluff: 40 = young again; 1 = young with a sliver of gold left, one hit from the Moon)
- `-Dfluffy.moon=true` - a story run starts up on the Moon (with `-Dfluffy.bossRoom=true`, at its boss)
- `-Dfluffy.play=ride` - the rocket ride up to the Moon first, then a story run (add `-Dfluffy.moon=true` to land on the Moon)
- `-Dfluffy.play=console` - straight into the rocket game on the living room's console (add `-Dfluffy.bossRoom=true` for a boss after 3 s - `-Dfluffy.boss=nighthawk` / `messenger` / `witch` / `fluffyking` / `grandfluff` picks which - `-Dfluffy.god=true` to keep your hearts)
- `-Dfluffy.console=true` - the rocket game console stands in the living room, as if the rocket had been up to the Moon (not saved)
- `-Dfluffy.book=N` - with `-Dfluffy.play=home`: the Flufficon opens right away at page N (0 records, 1 achievements, 2 enemies, 3 bosses, 4 guns, 5 items)
- `-Dfluffy.prefs=test` - plays on a save file of its own (`~/.prefs/enter-the-fluffy-test`), the real save untouched
- `-Dfluffy.hero=baby` - plays as that one of the family (boy, sister, granny, grandad, baby; not saved)
- `-Dfluffy.skull=true` - the hard-mode skull is in the living room (as if the rocket were built)
- `-Dfluffy.hard=true` - every story run is in hard mode (not the baby's)
- `-Dfluffy.crowns=all` (or `boy,granny`) - those heroes wear the golden skull crown (not saved); `all`
  also brings the Fluffy King home
- `-Dfluffy.kingRoom=true` - the living room opens in the King's room (once he's moved in)
- `-Dfluffy.bot=true` - the balancing bot plays story runs by itself (dodges by predicting bullets, rolls, blanks when low, uses active items, explores, opens chests, fights the boss) and logs every fight and run as JSON lines; best with `-Dfluffy.play=story -Dfluffy.prefs=bot`. Extras: `-Dfluffy.botSpeed=N` (world steps a frame, default 3), `-Dfluffy.botRuns=N` (quit after N runs), `-Dfluffy.botFloors=N` (each run stops after N floors - with `-Dfluffy.floor`, `-Dfluffy.gun`, `-Dfluffy.items` to try a loadout), `-Dfluffy.botFloorLimit=S` (seconds on a floor before it gives up as stuck), `-Dfluffy.botKeepGun=true` (stays with the gun it starts with until it's empty), `-Dfluffy.botLog=path` (default `bot-log.jsonl`). It sees every bullet's path, so it dodges plain bullets better than a person and walks into telegraphed blasts, lasers and delayed attacks worse - compare enemies and bosses with each other, not with a human.
- `-Dfluffy.play=dream` / `daily` / `rush` - straight into a dream, today's dream or the boss rush
- `-Dfluffy.bedroom=true` - the kids' bedroom is there without a win, and home opens in it
- `-Dfluffy.dreamFloor=N` - a dream (or the boss rush) starts on its floor N (`5`: a dream boss; with `-Dfluffy.boss=nightmare` any dream floor gets that boss, `-Dfluffy.bossRoom=true` starts in its arena)
- `-Dfluffy.dreamRule=glass_cannon` - every dream floor has that twist (`world/Dream.Rule`: bouncy, gold_rush, tough_crowd, fast_forward, moon_boots, lights_out, glass_cannon, mixed_up)
- `-Dfluffy.record=clip.mp4` - films the game into a video (needs `ffmpeg` on the PATH): every frame is drawn as exactly 1/30 s of game time and piped to ffmpeg, so the video is smooth however slowly it's drawn; the window opens at 1120x800 (the view's 7:5), fullscreen is ignored and the main menu's DEV BUILD tag hidden. `-Dfluffy.recordSeconds=N` quits after N seconds of video, `-Dfluffy.recordSkip=N` leaves out the first N. With the bot, `-Dfluffy.botSpeed=2` (its two 1/60 s steps are one frame) and boss cards play out instead of being skipped - e.g. `-Dfluffy.play=story -Dfluffy.bot=true -Dfluffy.botSpeed=2 -Dfluffy.god=true -Dfluffy.floor=3 -Dfluffy.fight=true -Dfluffy.record=city.mp4 -Dfluffy.recordSeconds=30`.
- `-Dfluffy.rocket=N` - the living room's cardboard rocket shows as built by the first N of the family (boy, sister, granny, grandad - the baby never helps; 0-4, not saved) - try it with `-Dfluffy.play=home`

A dev build shows "DEV BUILD" in the main menu.

### Generated assets

The graphics experiment on `codex/feature-pixel-art` renders directly at the window's
resolution, with continuous camera motion. Do not downsample the scene to sprite resolution:
small stars, the King's fluff, rotated sprites and moving bullet outlines lose detail and
flicker as they cross a coarse pixel grid. Sprite assets retain Nearest filtering. Lights, bullets and floor effects keep their original
full-resolution rendering. Assembled enemy bodies retain native-resolution sampling: forcing moving body parts onto a
coarse pixel grid aliases their fine features. The scene and camera are never downsampled.
Smooth shape masks use white RGB even in transparent pixels, so filtering a tinted edge does
not introduce a dark fringe around pale fluff, stars and rings.

Authored 32x40 enemy sheets at 64 pixels per world unit now cover `BulletKin`, `ShotgunKin`,
`VeteranKin`, `BandanaKin`, `PirateKin`, `TankerKin`, `BlueShotgunKin`, `SawnOffKin`, and the
three mancers. Six poses cover four walking frames, idle and braced firing; the white silhouette
row preserves hit flashes. `gfx/KinArt` draws the costumes while the enemy classes keep their
weapon movement, wind-ups, pump actions, recoil, charge orbs and attack tells.

Town and marked rats use authored 40x28 frames in `rat.png` at 64 pixels/unit (`RatArt`),
with four scurrying poses and idle. Large connected fur planes, an inset eye, separate feet and a
bent tail follow the kin's art approach without a post-process texture. Marked rats retain their
1.5x scale and green landmarks; nine fade palettes support the ending, and each kind has its own
white damage-flash row. Collision, speed and squishing rules are unchanged.

World drawing uses two depth-sorted layers: floor objects/ground enemies, then small flying enemies.
`Enemy.drawsAboveFurniture()` defaults to `flying && !boss`, so hovering shapes, ghosts, angels,
clouds and other regular flyers remain visible over the props they cross. `CheeseWorm` opts out:
its `flying` flag disables navigation collisions for burrowing, not aerial movement. Boss scenery
keeps its existing depth rules. `EnemyDrawOrderTest` checks furniture crossings, ordinary ground
occlusion, ordering between flyers and the burrower's exception.

Common projectiles (`objects/Bullet`) have render-only surface lighting: round shots, drops and bombs
use an upper-left highlight and a lower-right shaded rim; squares, diamonds, triangles, stars and
pillows use a raised face with a bevel. Acid globs retain their wobble with a shaded belly. Lighting is
steady rather than pulsing. Hero shots keep their white centre and existing halo, enemy shots keep
their original outline, palette and outer dimensions. The helpers reuse scratch colours without
changing `Bullet.color`; collisions, motion, lifetime and damage remain independent of the art.

The family has distinct 40x56 sheets at 64 pixels per world unit: a hooded boy, a slim ninja
sister with a ponytail, crossed rear straps and a plain rear sash band, a rounder granny with a shawl and glasses, a broad grandad
with a waistcoat and moustache, a large-headed baby, and a puffy pocket-size King. Walk, back,
roll and all death frames share the original logical anchors. Faces follow the old 20x28 portraits:
compact eyes looking to one side, stepped head shapes and clean skin planes. Hair, glasses, the
moustache and the pacifier distinguish the family even at gameplay scale. Rear views have their own back seams,
hood/shawl/waistcoat backs, without front buttons or front pockets. The little King's crown overlaps
the upper fluff; his dot eyes and upturned smile sit on a single pink plane. His tucked roll retains a visible
face and folds his feet across his belly so the full rotation reads on a round silhouette.
Standing and rolling share the same face; the roll never paints an extra mouth. `Sprites.HERO_W/H` remain logical
20x28 sizes; use `Sprites.heroW/heroH` for source regions, and `Sprites.w/h` for props. The
player's world dimensions, feet box, belly hitbox and earned skull-crown anchors are unchanged. The hub,
rocket pilot and ending/intro pictures all use the hero-specific scale.

The remaining regular enemy cast uses authored 48x48 sheets (`cast_*.png`, `gfx/CastArt`),
from `tools/enemy_art.py`, called by `gen_sprites.py`. All 58 Flufficon entries now use authored
bodies or core layers. Humanoid costumes have material-specific construction (cap piping, lapels,
mask lenses, suit seams, pockets and metal panel edges). Dandelion has separate ivory petals and
an ochre seed head. Rats keep low connected fur planes and planted feet. No random surface grain
or full-scene pixelation is applied to these sprites.

Six columns hold four steps, idle and preparation; row two is an alpha-identical white hit silhouette.
Multipart creatures retain their original animated appendages and combat effects: wing beats,
pincers, shields, rotating shells/craters, veteran marks/medals, emerging worm segments, jumping
bunnies, jet flames and teleport alpha. Body, head, fist and drum layers can be placed independently.
Authored bodies bypass `ActorInk` to keep their own palette and outline. Enemy guns use layered
metal edges, a dark muzzle and a separate grip; the riot shield keeps its directional projection,
transparency and raised/lowered state. Hitboxes, health, timing and bullet patterns are unchanged.

Bosses use painted parts from `tools/boss_art.py` (`boss_parts.png`, read by `gfx/BossArt`).
Each part attaches to the same animated anchor as the original geometry: hinges, weapons, limbs
and faces can still move independently through their attack phases. The lower atlas half repeats
the silhouettes in white for hit flashes. The giant Grandfluff moon uses its own 128-pixel disc
in `boss_moon.png` so crater rims and ledges remain crisp across the arena. `ActorInk` still outlines the assembled bosses; the
painted fleece and crowns of the Fluffy King, Grandfluff, and Shadow King carry their own outlines
so the shading never cuts across their faces. Grandfluff shrinks to zero before the giant moon
starts appearing during the phase transition. The Fluffy King has dot eyes and a small smile on
his throne, animated eyes and brows in battle, and redder fleece in phase two.

The actor layer uses the current GL viewport's full physical pixel dimensions, resizes with the
window, preserves clipping/transform/blend state, and composites only the occupied rectangle.
Body sampling remains at native resolution, including transformed portraits, to keep small moving
features stable. It does not downsample the scene. Translucent material keeps its alpha, white hit flashes and
red attack signals keep their brightness, and the original two kin plus the soft-robed mancers
bypass the extra ink pass. The Night Hawk's
additive searchlight is drawn on the ground before the body pass so it still lights the floor.
Shaders live in `assets/shaders/actor.vert` and `actor.frag`; material masks are generated by
`Draw.materialSurface`, with no random texture noise or time-varying lighting.

The Kaiju also has an anatomy redesign: charcoal reptile scales, heavier upright haunches,
a long muzzle with small amber eyes, a moving lower jaw, short tucked resting forelimbs,
continuous tapered muscles, and large jagged dorsal plates that turn blue during breath charge.
Head hitbox, mouth/beam origin, slam targets, timing, warning zones and damage keep their values.
Raised paws and the Kraken's emerging tentacles receive the same material pass separately from
floor warnings and beams.

All 64 room themes use sparse, shallow-contrast floors (`quiet_floor`); the six village themes
retain their approved palette. Wood, worn slabs, raised panels, carpeting, moon craters, cheese,
sand, clouds and the dream floor each retain characteristic marks. Walls, windows, doors and
hazard colours keep their navigation/combat meaning. The living room and bedroom share calmer
wide floorboards; the King's room has broad low-contrast rose slabs.

Furniture and guns receive connected inset highlights and dark bevels before atlas packing
(`detailed_sprite`). Both atlases use 64 source pixels/unit. Chests have inset front panels,
raised riveted straps and hinges; stone supports have carved flutes and cracks, and furniture has
inset fasteners. Pit/exit/coin symbols and fire pixels only receive nearest-neighbour enlargement.
`props.txt` rectangles are source pixels; `Sprites.prop` returns scaled regions so `Sprites.w/h`
keep the original world dimensions and cover boxes. Home regions still use 32 px/unit.
`guns.txt` rectangles are source pixels, but grip coordinates stay logical 32 px/unit: gun size,
grip position and muzzle reach remain unchanged. Room backgrounds retain their original grid.
Check motion as well as still images when comparing with `develop`.

Dream Lights Out uses a separate darkness mask: 18% dark at the hero and 86% at the edge,
with a smaller clear area than normal night. Dream lamps use alpha blending instead of additive
white light, so pale floors and overlapping shop lights no longer wash out. Ordinary night keeps
its previous mask and lighting.

Everything except the font is generated by scripts in `tools/` - tweak and re-run them rather than
editing the files:

- `python3 tools/gen_sprites.py` - family sheets, authored enemies (`bullet_kin.png`, `shotgun_kin.png`, `actor_*.png`, `cast_*.png`, `kin_weapons.png`, `rat.png`; cast layers from `tools/enemy_art.py`), guns, items, furniture, chests and all room tilesets (needs Pillow)
- `python3 tools/gen_sfx.py` - sound effects (needs numpy; ffmpeg for the death cry)
- `python3 tools/music/tracks.py <GeneralUser-GS.sf2> src/main/resources/assets/music [track ...]` - the
  soundtrack: menu, home, village, town, city, tower, sky, sea, slums, undercity, moon, and the boss themes boss_moon (Grandfluff), boss_slums, boss_undercity, boss (the Fluffy King),
  boss_village, boss_town, boss_city, boss_tower, boss_sea. Needs numpy, ffmpeg and FluidSynth
  (`brew install fluid-synth`); uses `musiclib.py` from the Towards Dreams project. Delete the leftover `.wav`/`.mid` files afterwards.

### Room layouts

New room layouts: add an 8x13 string grid to `RoomTemplates.COMBAT` (it's centred in bigger rooms).

Notes for coding agents (code map, conventions) are in [AGENTS.md](AGENTS.md).
