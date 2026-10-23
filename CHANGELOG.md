# Changelog

## [Unreleased]

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
