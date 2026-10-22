# Changelog

## [Unreleased]

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
