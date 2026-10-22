# AGENTS.md

## Project

Top-down bullet-hell shooter in the spirit of *Enter the Gungeon*, written in **Java 21** with **libGDX 1.13.5** (LWJGL3 desktop backend). Built with **Maven** (deliberately not Gradle — the owner knows Maven).

## Build & run

- Build: `mvn clean package`
- Run: `java -jar target/game-0.0.1-SNAPSHOT-jar-with-dependencies.jar` (the fat jar, not the thin one)
- Run from IDE: `com.trush.game.launcher.AppLauncher`
- Debug flags only work in a **dev build**: `mvn clean package -Pdev` (Maven filters `src/main/resources/dev.properties`; running from the IDE without filtering also counts as dev). A normal `mvn clean package` ignores them all. Flags (see `launcher/Dev`): `-Dfluffy.god=true`, `-Dfluffy.allGuns=true`, `-Dfluffy.bossRoom=true` (a new story run from the menu starts at the boss), `-Dfluffy.retryBoss=true` (try again/restart goes back to the boss instead of floor 1), `-Dfluffy.keepSave=true` (continuing doesn't use up the save), `-Dfluffy.boss=haymaker` (force a boss), `-Dfluffy.startWave=N` and `-Dfluffy.layout=N` (endless). Read new flags through `Dev.flag`/`Dev.number`, never `System.getProperty`. Only `dev.properties` is filtered — never filter binary assets.

## Code map

- `launcher/ETGFanfic` — the `Game`; owns shared `batch`, `font`, `viewport`, `draw` (`gfx/Draw`), `sprites` (`gfx/Sprites`), `sfx` (`audio/Sfx`), `prefs` (best score/wave). Use `game.text(str, x, y, scale, color, Align.x)` for text.
- `screen/MainMenuScreen` (mode select), `screen/GameScreen` (story or endless) — input states (playing / paused / game over / stage clear), camera shake, music crossfades, HUD + minimap. All simulation lives in `world/GameWorld`.
- `save/RunSave` — between-floors save, like ETG: only offered on the stage clear screen after the boss (or when the window is closed there). Holds next floor number, score/kills, hearts, blanks, guns + ammo (by name via `Weapon.byName` — register new guns there). Continue starts `StoryDirector(floor, ownedGuns)`; until stage 2 exists a later floor is a new village map with +0.15 difficulty per floor. Deleted when continued or when a new run starts. No saving mid-floor.
- `screen/MenuList` — shared mouse/keyboard/gamepad text menu (main menu, pause menu).
- `world/GameWorld` — the current `Room` and everything in it: player, enemies, bullets, pickups (stored on the room), particles; collisions vs walls/props, table flipping, blanks, room transitions through doors.
- `world/Director` — game-mode interface. `StoryDirector` (stage 1 rooms: lock doors, spawn, rewards, boss, `isFinished`) and `WaveDirector` (endless waves).
- `world/StageMap` — random tree of rooms on a grid (start, combat, treasure, boss). `world/Room` — doors, lock state, props, wall collision rects. `world/RoomTemplates` — 8x13 layout strings. `world/Arena` — room geometry constants.
- `gfx/RoomRenderer` — bakes a room's floor/walls/doors from `assets/tiles/<theme>.png` into one texture; draws door bars when locked.
- `input/Gamepad` — Xbox-layout gamepad (gdx-controllers + Jamepad), polled once per frame in `ETGFanfic.render()`; `game.pad.justPressed(Button.X)`, sticks, `isActive()` (pad vs mouse used last). Hold Y opens the gun wheel in `GameScreen`.
- `player/Player` — movement with feet-box collision, dodge roll, `E` flip, weapons inventory, hearts (half-hearts), blanks; animated pixel sprite + floating gun hand.
- `objects/Chest` + `objects/Tier` — ETG-style loot: chests per tier (D brown, C blue, B green, A red); a chest holds the not-yet-owned gun of its tier (reserved from `StoryDirector`'s pool when the chest is placed) or else consumables. Locked chests cost a key (`Player.keys`, saved between floors); the boss chest is free. Story mode only hands out guns via chests: treasure room (locked), rare locked room-reward chest, boss chest. One guaranteed key per floor (cleared combat room #1–3), extra keys are rare drops. After the boss, `GameWorld.openExit()` shows a floor hatch; interacting on it (`GameWorld.interact`: chest > exit > flip table) ends the floor. Give every new gun a `tier`.
- `objects/Weapon` (add guns as static factories), `objects/Bullet` (`accel`, `curve` for patterns), `objects/Pickup`, `objects/Prop` (furniture: tables flip, most props destructible).
- `enemy/Enemy` — base class; behind cover, `keepDistance` paths around props to a spot with line of sight (`GameWorld.hasLineOfSight` / `findSightPoint`, a 0.25-unit nav grid) and enemies only fire when they can see the player; `BulletKin`, `ShotgunKin`, `Bulletmancer`; bosses `FluffyKing` (rings/spirals/fans), `Haymaker` (charges, straw walls, crows, hay rain) `Harvestman` (stalks you with a chainsaw: exhaust sprays, homing lunges with sawdust, boomerang blades) and `HollowWitch` (flies over furniture via `Enemy.flying`: curving hex bolts, swoops leaving delayed sparks, closing hex ring with one gap, jack-o'-lanterns that burst), listed in `enemy/Boss` — each floor and endless boss wave rolls one (`Boss.pick()`). Bosses use `Enemy.huntForSight` to come out from behind cover and override `bossName()`/`isEnraged()` for the boss bar.

## Conventions

- Plain Maven project — **no Spring Boot**; keep it out of the POM.
- Test dependencies (JUnit, Mockito, Hamcrest) stay at `test` scope.
- The LWJGL3 backend extracts natives automatically — do not re-add `maven-nativedependencies-plugin` or `antrun` launch hacks (those were LWJGL2-era workarounds).
- Assets live under `src/main/resources/assets/` and are loaded with the `assets/` prefix, e.g. `new Texture("assets/player.png")`. Do not flatten assets into the classpath root.
- The shared font (`ETGFanfic.font`) is a FreeType-generated pixel font (Press Start 2P, Latin-only — no `∞` etc.) created once in `ETGFanfic.create()` and disposed in `ETGFanfic.dispose()`. Screens must use it, not create/dispose their own.
- Never call `setUseIntegerPositions(true)` on fonts — the world is only 7×5 units and rounding to integer positions collapses small text to nothing.
- World: `FitViewport(7, 5)` world units; pixel art is 32 px per unit (rooms are 224x160 px). Room geometry (walls, door gaps, 13x8 layout grid) lives in `world/Arena.java` and must match `gfx/RoomRenderer`.
- Art is generated: `tools/gen_sprites.py` (hero, props + `props.txt` atlas, tilesets), `tools/gen_sfx.py`, `tools/music/tracks.py` (FluidSynth + GeneralUser GS soundfont from the Towards Dreams project). Edit the scripts and regenerate rather than hand-editing the PNG/WAV/OGG files. Use Nearest filtering for pixel art.
- The player collides with a small feet box (`Player.feetRect`), not the sprite; enemies are circles pushed out of walls/props (`GameWorld.pushOutOfSolids`).
- Enemies, bullets, guns and pickups are still placeholder art drawn with `gfx/Draw` (shapes on the SpriteBatch, no ShapeRenderer). Replace a `drawBody` with sprites when real art exists; keep hitboxes (`radius`) independent of art.
- Bullets (and enemy line of sight) are stopped by `Prop.bulletRect`: the footprint stretched up to just below the sprite top, so standing behind a pillar covers you. Movement still uses the footprint (`solid`).
- Collisions are circle-vs-circle. The player's hitbox is a small circle around the belly (`Player.hitboxY()`, `HITBOX_RADIUS`), not the sprite rectangle.
- Don't remove from `enemyBullets` while it may be iterated — mark `alive = false` and let `GameWorld` sweep (taking damage and blanks clear bullets mid-update).
- Spawn enemies via `GameWorld.spawnEnemy` (goes through a pending list), never by adding to `enemies` directly during update.
- Sound effects: `assets/sfx/*.wav` are generated by `tools/gen_sfx.py`; register new ones in `audio/Sfx`. `sfx/death.wav` is `assets/die-sound.mp3` with its leading silence cut (needs ffmpeg), so it lines up with the death animation; `body_fall` plays when the hero hits the floor. Regenerating rewrites every WAV — only commit the ones whose audio actually changed.
- Lombok (`@Getter`, `@Data`) is used; keep the annotation processor path in `maven-compiler-plugin`.
- `listener/DropListener.java` and `listener/FluffyListener.java` are leftover libGDX tutorial code, candidates for deletion. `objects/Coin.java` and `objects/Table.java` are unused stubs referencing textures that don't exist yet.
- Commit and push after every finished change (once it builds and tests pass): one change per commit, with a sensible conventional message (`feat: ...`, `fix: ...`), then `git push`. Don't leave work uncommitted.
- Commit with `git -c core.autocrlf=input` from Linux shells: some older files have CRLF line endings on disk and the owner's Mac git normalizes them.
- `assets/floor.png`, `assets/player.png` and the ETG `.mp3` tracks are no longer used by the game.
