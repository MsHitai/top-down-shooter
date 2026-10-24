# Changelog

## [Unreleased]

### Added
- **Floor 5, the Fluffy Kingdom** - the final floor, up in the clouds where the skyscraper's helicopter
  takes you: cloud islands with open sky all round, pearly gates, a cloud temple, storm clouds, a
  rainbow's-end treasure room, holes in the clouds to fall through, cloud banks, marble columns, golden urns
  and presents for furniture, and new music, "Silver Lining". The **Fluffy King** waits in his throne room;
  beat him and slide home on a rainbow to win the run (and, without a scratch, unlock the Flufful).
- **Angels and thunderclouds**: the **Sword Angel** raises its glowing sword, dashes in and slashes a
  crescent of blades; the **Bow Angel** aims (watch the golden line), looses volleys of three and shoots
  arrows into the sky that come down on golden rings; the **Thundercloud** drifts after you and calls
  lightning onto the spot you stood on. They also join endless mode from wave 16 (its music moves up to
  the clouds after wave 20).
- **Birds over the clouds** (the owner's idea): during a fight in the Fluffy Kingdom, birds - sometimes a
  little flock - fly across the room after a white arrow warns where they'll come in. Dodge them: touching
  one hurts, and your bullets fly right past.
- The angels got their own sounds (a sword swish, a bow twang) instead of the train's whistle, and the
  clouds have no more tables - a cloud bank stands in for each.
- **Arrows to the last enemies**: when nobody left in a fight is on screen, red arrows at the screen edge
  point to them - no more hunting for that last cube in a corner.
- `-Dfluffy.play=story` / `endless` (dev builds) skips the main menu.
- **Golden hearts**: the heart cap goes from 8 to 16. Hearts 9-16 are golden and sit on top of the
  first eight (no third row): a gold rim peeks out behind a red heart that has a golden one waiting,
  and gold covers it while it's filled. Hits take the golden hearts first.
- **An opening story** before your first run: how the Fluffy King got out of bed on the wrong side,
  the whole kingdom started shooting, and one country boy decided it was about time it all ENDED!
  Six picture-book pages; skip with Esc / B, replay any time from "THE STORY" in the main menu.
- **The Flufful has its own look**, drawn by the owner: a fluffy pink pistol with a sweet shark on top.
- **Floor 4, the Skyscraper**: a straight climb by elevator - the lobby, five to seven stops 5-10
  storeys apart (one a treasure floor), then the roof. Between floors a lift ride counts the storeys
  behind shut steel doors; each floor greets you with "FLOOR 23". Offices, server rooms, a cafeteria,
  desks, potted plants and water coolers, and its own music: "Glass and Steel" (it starts as lift music)
  and "Above the Clouds" for the roof. The run is now won on floor 4.
- **The Skyscraper's bosses**, on a roof with open sky all round:
  - **The Kaiju** (drawn by the owner's son) - a giant rising from behind the roof's edge, like ETG's
    Dragun (the camera pulls back to fit it in): spiky back and tail, a long toothy snout. A mouth
    laser that sweeps the roof, fist slams with warning circles, a bite into a warned zone.
  - **The Mega Mech** - head lasers and arm cannons; it leaps and lands with a shockwave you roll
    through; enraged, a turret folds out of its chest.
  - **The Messenger** of the Fluffy King - rides a cloud with a magic sword that fires triangles of
    bullets (drawn by the owner's son), slashes crescents and calls rain.
- **Skyscraper enemies**: the RoboCart (crosses of bolts), the Android (laser bolts), the Agent (a man in
  black with two pistols), and the Mountain and Lead Cubes that slide at you when you line up with them.
- **More new enemies**: the Robber (city - dodge-rolls out of the way of your bullets), the Dynamite
  (everywhere - runs at you and blows up; shoot it first and it takes its friends with it) and the
  Shapeshifter (town on - morphs between shapes and fires a matching volley: ring, square, triangle, star).
- **The Long Shot**, an A-tier sniper rifle with a red laser sight.
- **Death animations**: enemies topple over and vanish in a puff of smoke; bosses get a moment of slow
  motion, explosions bursting all over them and a big final blast before the chest appears.
- **A way on that makes sense**: the village opens a gate onto the town road, the town has a bus stop,
  the city the skyscraper's doors and the roof a helicopter - instead of a hatch down from every floor.
- **Blanks daze enemies**: no new attacks for about a second after a blank, so you can get some distance.
- The hydrant sprays **water drops**, the Flufful fires **little clouds** that grow with its level.
- Endless restocks shot-up furniture after each boss wave; its boss waves climb to the skyscraper
  (wave 20) before the Fluffy King (wave 25).

### Fixed
- Enemies no longer hang about just off screen (where they can't shoot): they come into view. Bikers
  ride round cover instead of into it.
- The Pigeon King's statue can be shot down once he's beaten.
- The best score and best endless wave are now also recorded when you quit to the menu, restart or
  close the window, not only when you die.
- You can no longer walk between two barrels (or crates) stacked on top of each other - it looked like
  walking right through them.
- **The Pigeon King's flocks no longer come out of nowhere**: red arrows on the edge of the screen show
  every incoming pigeon (and the gaps) until the flock flies into view, and the warnings show through
  the city's night darkness.
- The controls hint no longer covers the active item box.

### Added
- **The Flufful** (Fluffy + Gundul), a talking B-tier gun in the spirit of ETG's Gunther: soft homing
  pom-poms with endless ammo, and it chats in a speech bubble over your head - one line after each
  cleared room or beaten boss (never mid-fight, where a bubble could hide bullets), and about being
  picked up, put away, reloaded, or nothing at all. It grows fluffier with every level of kills
  (at 20, 60, 120 and 200): more damage, faster shots, bigger pom-poms. Unlock it for good by beating
  the Fluffy King without taking a hit; after that it can turn up in B chests.
- **A victory screen**: beating the last floor's boss (the city, for now) wins the run - confetti, your
  score and kills, and a count of runs won - instead of replaying the city forever. Endless mode is the
  place to keep going.
- **Items**, like ETG's: chests now hold a gun *or* an item of their tier, each item at most once per run
  (endless: every boss wave gives one). Passive: Fluffy Slippers (walk faster), Spare Pocket (+1 blank
  every floor), Magnet (pickups fly to you), Bigger Clips (+50% magazines), Hot Sauce (+25% damage),
  Lucky Clover (better chests, more drops), Bubble Shield (blocks a hit, recharges as you deal damage), Ghost Bullets
  (bullets fly through enemies). Active, used with RT / F and shown in the bottom-left corner: Med Kit
  (+2 hearts), Fluffy Candy (full health) - both one use - and a recharging Grenade. One active item at
  a time: picking up another leaves the old one on the floor. Items are saved between floors.
- **Fluffy Heart** (C item): +1 max heart.
- **RELOAD and EMPTY hints** over the hero, like ETG's: RELOAD blinks when the magazine is nearly empty,
  a red EMPTY when the gun is out of ammo.
- **Gun upgrades**: finding a gun you already have upgrades it instead (more damage, a bigger magazine,
  or faster bullets, at random) and refills it; the HUD shows it as "Pocket Uzi +1".
- **Mimic street lamps** (very rare, town and city): one lamp in a combat room may be alive. It shivers
  now and then; walk up to it or shoot it and it tears itself out of the pavement and hops after you,
  throwing bullet rings. It drops a key or a blank.
- **Six new guns**: Laser Pointer (D, a beam that hits instantly), Super Bouncer (C, rubber balls that
  bounce off walls three times), Plasma Zapper (C, fast laser bolts), Gull Gatling (B, a minigun that
  spins up, in the spirit of the Gatling Gull's), Egg Lobber (B, a grenade launcher that lobs eggs over
  cover - hold the trigger to throw further, with a marker where it will land) and Party Rocket (A, a rocket launcher).

### Changed
- **Bigger cubes**: the Mountain and Lead Cubes are about one and a half times bigger - proper blocks.
- **The Pigeon King** only calls his flocks once he leaves his statue (below half health), every fifth
  attack and from one side; the flock and his pecking breadcrumbs now look like flapping pigeons.
- **Small rooms** get about half the enemies, two at a time and at most one biker - three bikers in a
  room full of pits was too much.
- **Less ammo**: at most one ammo box per room (or endless wave), and enemies drop ammo as rarely as half
  hearts. (An ammo box adds 60% of every gun's ammo; it never refilled everything.)
- **The Grenade** is thrown like the Egg Lobber: hold RT / F to throw it further (1 to 4.5 units), let
  go to throw, with a marker where it will land. It used to land where you aimed - always close on a pad.
- **The Traffic Light's arena** has three parked cars to hide behind from his streams and bursts.
- **Gun fixes**: fast guns now really fire at their rate - the cooldown lost the part of a frame each
  shot came late by, so the Laser Pointer and the Gull Gatling did about a quarter less damage than
  intended. The Laser Pointer does 28 damage/s, the Bubble Blaster has faster, harder-hitting bubbles,
  and the Confetti Popper hits harder.
- **Enemies get tougher**: regular enemies have +25% health on floor 2 and +50% on floor 3; in endless
  +4% a wave (up to +75%).
- **Endless is fairer**: boss waves climb the stages (wave 5 a village boss, 10 a town boss, 15 a city
  boss, 20 the Fluffy King, then anyone), at most one hydrant, biker and sniper per wave until wave 10,
  and fewer enemies on screen at once.
- **Room music gets darker floor by floor**: the village keeps its folk-rock theme, the town has a new
  galloping harmonic-minor track with tremolo strings and choir, the city a cold synth track with a
  tolling clock bell and double kicks. Endless mode moves through the three as the waves go on.
- **Bubble Shield** no longer comes back in every room: once popped it recharges by dealing 250 damage
  (its HUD icon fills back up), like ETG's charged items.

## [0.4.0] - town and city

### Added
- **Floors 2 and 3**: the Town (streets, market, alley, park, sewer-hatch pits) and the City at night
  (avenue, subway, rooftop, construction site; a darkness vignette with lamplight), each with its own
  enemies (Hydrant, Rats, Shell Cop / Biker, Sniper), furniture, door barricades and music.
- **Bosses**: three per stage, one rolled per floor - Haymaker, Harvestman, Hollow Witch (village);
  Trash That Can, Road Rager, Traffic Light (town); Night Hawk, Pigeon King, Evil Train (city). The
  Fluffy King is now the final boss (endless only for now), bigger and tougher. Fun victory lines, boss
  music per stage, own arenas for some bosses.
- **Bigger rooms and a following camera**: small "trap" rooms, roomy combat rooms, big open boss arenas;
  the camera leans toward the aim. Boss bullets are chunkier and slower (same hitbox).
- **Chests, keys and tiers**: guns come out of D/C/B/A chests; one key per floor; heart containers
  from chests and flawless boss kills; half-heart drops.
- **Five new guns** (Confetti Popper, Bubble Blaster, Banana-rang, Bee Hive, Rainbow Ray) and pixel-art
  sprites for every gun (hand, floor, gun wheel, HUD).
- **Xbox controller** support with a hold-Y gun wheel; **map and teleporting** (hold LT / M) with icons
  for what's left in each room.
- **Saving between floors** (like ETG), main menu Continue/Quit, pause menu.
- Enemies path around cover to get a clear shot and only attack from on screen; bosses come out from
  behind cover. Dodge-rolling over tables and rats, pits you can fall in (and knock enemies into),
  death animation, dev build profile (`-Pdev`) for the debug flags.

### Changed
- The village looks like a village (cottage, yard, barn, orchard, square, chapel themes) instead of
  brick cells.
- Cover is directional; street lamps and many props can be destroyed.
- Point-blank shots land every pellet; fast bullets can't skip enemies.
- Endless boss waves fully heal and drop a heart container.
- The Rainbow Ray has more ammo and a bit more damage; the Banana-rang is a real boomerang.

### Fixed
- Rolling over a 2x2 block of tables no longer snaps you back to where the roll started.
- Map icons: one colour for items, key and chest shapes, all outlined.

## [0.3.0] - stage 1, the village

### Added
- **Story mode, stage 1 "The Village"** (`world/StoryDirector`, `world/StageMap`): a random map of 8-11 rooms per run (start, combat rooms, treasure room, boss room at the far end). Doors lock during fights and open when the room is cleared. Minimap in the top-right corner; stage-clear screen after the boss.
- **Room layouts** (`world/RoomTemplates`): 9 combat layouts plus start/treasure/boss rooms, picked at random. Endless mode uses a random layout too.
- **Room themes**: house, chapel, yard, cellar, village square - pixel-art tilesets baked into each room by `gfx/RoomRenderer` (replaces `floor.png`).
- **Furniture** (`objects/Prop`): flippable tables (`E`), barrels, crates, hay bales, stone pillars. Everything blocks movement; flipped tables and other props block bullets and can be destroyed.
- **New pixel-art hero** (`assets/sprites/player.png`): front/back walk and idle frames plus a roll pose. No arms baked in - a floating hand holds the gun, which fixes the "third arm" look.
- **Original soundtrack** in the spirit of ETG (`assets/music/`): menu, village and boss themes, seamless loops, crossfading between village and boss music.
- Sound effects for table flips, breaking furniture and doors locking.
- Generators: `tools/gen_sprites.py`, `tools/music/tracks.py` (+ `musiclib.py`).
- Dev flags `-Dfluffy.bossRoom=true` and `-Dfluffy.layout=N`.

### Changed
- `FirstStageScreen` replaced by `GameScreen` (story or endless); the main menu lets you pick the mode.
- Weapon switching moved from `E` to `Tab` (mouse wheel and 1-4 still work); `E` is now "interact / flip table".
- Room bounds (`world/Arena`) match the new room art: 0.25-unit walls, a 0.5-unit wall face on top, doors in the middle of each wall.
- Player collision uses a small box around the feet and slides along walls and furniture.
- The ETG music files are no longer used by the game.

## [0.2.0] - endless mode

### Added
- **Combat.** Mouse aiming with a visible gun that follows the cursor, hold-to-fire, magazines, reloading (`R`), muzzle flash, screen shake.
- **4 guns** (`objects/Weapon.java`): Rusty Sidearm (infinite), Fluff Shotgun, Pocket Uzi, Big Magnum (piercing). Switch with mouse wheel / `E` / `Tab` / `1-4`.
- **Dodge roll** (`Space` / right mouse / `Left Shift`) with invincibility frames.
- **Blanks** (`Q`): clear all enemy bullets and knock enemies back. Start with 2.
- **Health** as ETG-style half hearts (3 hearts), invincibility blink after a hit, small bullet-clear around the player when hit.
- **Enemies** (`enemy/`): Bullet Kin, Shotgun Kin, Bulletmancer, and the boss The Fluffy King (rings, spirals, aimed fans, enraged phase with minions). Spawn telegraphs and shot wind-ups so attacks are readable.
- **Waves** (`world/WaveDirector.java`): endless waves that ramp up in size and difficulty; boss every 5th wave; rewards (guns, hearts, ammo, blanks) after each wave.
- **Pickups** (`objects/Pickup.java`): hearts, ammo boxes, blanks, guns.
- **HUD**: hearts, blanks, wave, score, gun name, ammo and magazine pips, reload bar, boss health bar, wave banners, pickup messages.
- **Pause menu** (`Esc` / `P`), **game over screen** with retry, saved best score / best wave (libGDX Preferences).
- **Main menu** now lists the controls and shows the best score; starts with click / Enter / Space.
- Procedural sound effects (`assets/sfx/*.wav`) and their generator script `tools/gen_sfx.py`.
- Shared helpers: `gfx/Draw` (shapes, pixel-art hearts and blank icons drawn on the SpriteBatch), `audio/Sfx` (rate-limited one-shot sounds), `effects/Particles`, `world/Arena` (room bounds), `world/GameWorld` (simulation).
- Dev JVM flags: `-Dfluffy.startWave=N`, `-Dfluffy.god=true`, `-Dfluffy.allGuns=true`.

### Changed
- Room wall constants moved from `FirstStageScreen.restrictMovement()` to `world/Arena.java`.
- Stage music is now on (low volume); `N` toggles it.
- `ETGFanfic` owns the shared `Draw`, `Sfx` and `Preferences`, and has a `text(...)` helper for aligned text at any scale.
- Cleaned up `pom.xml`: removed Spring Boot parent/plugin, moved test dependencies (JUnit, Mockito, Hamcrest) to `test` scope, removed `packr` runtime dependency and LWJGL2-era natives plugins, pinned explicit plugin versions.
- Assets now load with the `assets/` prefix instead of being flattened into the jar root.
- Cropped `player.png` to its content (was 948×1135 with huge transparent margins) and shrank the player sprite to 0.8 world units tall, sized from the texture aspect ratio.
- Replaced the scaled-up default 15px `BitmapFont` with a FreeType-generated pixel font (Press Start 2P) created at native resolution — menu text is now crisp and centered via `GlyphLayout`.
- Player movement is now clamped to the actual walls of the new `floor.png` room via configurable `WALL_*` margin constants.

### Fixed
- Foreground FPS was set to `refreshRate + 1`, which became 1 FPS on displays that report a refresh rate of 0; now falls back to 60.
- Player bounding box was initialized with pixel dimensions instead of world units.
- README run command now points at the fat jar (`jar-with-dependencies`).
