# Changelog

## [Unreleased]

### Changed
- Cleaned up `pom.xml`: removed Spring Boot parent/plugin, moved test dependencies (JUnit, Mockito, Hamcrest) to `test` scope, removed `packr` runtime dependency and LWJGL2-era natives plugins, pinned explicit plugin versions.
- Assets now load with the `assets/` prefix instead of being flattened into the jar root.
- Cropped `player.png` to its content (was 948×1135 with huge transparent margins) and shrank the player sprite to 0.8 world units tall, sized from the texture aspect ratio.
- Replaced the scaled-up default 15px `BitmapFont` with a FreeType-generated pixel font (Press Start 2P) created at native resolution — menu text is now crisp and centered via `GlyphLayout`.
- Player movement is now clamped to the actual walls of the new `floor.png` room via configurable `WALL_*` margin constants.

### Fixed
- Player bounding box was initialized with pixel dimensions instead of world units.
- README run command now points at the fat jar (`jar-with-dependencies`).
