# AGENTS.md

## Project

Top-down bullet-hell shooter in the spirit of *Enter the Gungeon*, written in **Java 21** with **libGDX 1.13.5** (LWJGL3 desktop backend). Built with **Maven** (deliberately not Gradle — the owner knows Maven).

## Design pillars

- **Difficulty and replayability target *Enter the Gungeon*, not a kids' game.** The most skilled player (Egor) beats the Fluffy King in hard mode in Fast Forward, kills Grandfluff with the Frying Pan and reaches endless wave 40; he clears Geometry Dash medium demons. Do not soften mechanics, patterns or numbers "to be kind" — tune for a challenge that stays readable, and keep the easier end of the existing difficulty settings for everyone else.
- **No warning line indicators for bullet attacks.** They cause nausea for the owner. Telegraph bullet attacks on the enemy never add line warnings before bullet attacks.

## Build & run

- Build: `mvn clean package`
- Run: `java -jar target/game-0.16.0-SNAPSHOT-jar-with-dependencies.jar` (the fat jar, not the thin one)
- Run from IDE: `com.trush.game.launcher.AppLauncher`

## Git workflow

Make changes on `develop` by default. When the change is finished and the tests and `mvn clean package` pass, commit and push it. Do not run the build if the change only included docs updates. If you're unsure which branch to use, ask the owner.

For a release, follow this order.

1. On `develop`, remove `-SNAPSHOT` from the version in `pom.xml`, update the build commands in `README.md`, `DEVELOPING.md`, and `AGENTS.md`, and prepare the changelog. Run `mvn clean package`, then commit and push the release preparation.
2. Merge `develop` into `master`. Build and test the merge with `mvn clean package`, tag the merge commit `vX.Y.Z`, and push `master` and the tag. Publish the GitHub release from that tag with release notes and the fat JAR from the release build, named `enter-the-fluffy-X.Y.Z.jar`.
3. Check out `develop` and bring in the release commits from `master`. Bump `pom.xml` to the next version with `-SNAPSHOT` and update the build commands in the same three documents. Run `mvn clean package`, then commit and push the version bump to `develop`.

## Mechanics documentation

The developer reference is [docs/README.md](docs/README.md), organized into separate
mechanic pages with implementation and test links. It contains spoilers. Start with
[architecture](docs/architecture.md) for ownership, then the relevant topic; the old
inline code map and debug list now live there.

- Runtime/input/UI: [architecture](docs/architecture.md), [controls](docs/controls.md), [screens](docs/screens-and-interface.md), [home](docs/hub-and-menus.md).
- Combat/equipment: [player](docs/player-and-heroes.md), [combat](docs/combat.md), [weapons](docs/weapons.md), [items](docs/items.md), [synergies](docs/synergies.md), [status](docs/status-effects.md).
- World/content: [campaign](docs/campaign.md), [rooms](docs/rooms-and-cover.md), [stages](docs/main-stages.md), [enemies](docs/enemies.md), [bosses](docs/bosses.md), [difficulty](docs/difficulty.md).
- Other modes/routes: use the index for Slums, Undercity, sea, Moon, endless, dreams, Under the Bed and rocket/console.
- Persistence/tooling: [saving](docs/saving.md), [progression](docs/progression.md), [debug flags](docs/debugging.md), [balancing](docs/balancing.md), [assets](docs/assets.md), [rendering](docs/rendering.md).

Read debug flags through `Dev.flag`/`Dev.number`/`Dev.text`, never `System.getProperty`.
Only `dev.properties` is Maven-filtered; never filter binary assets.

## Conventions

- Use ordinary imports and short class names in Java code, including tests; use static imports for helpers such as Mockito methods. Do not write fully qualified names in code (for example, `com.trush.game.world.Room` or `org.mockito.Mockito.doReturn`) unless required to resolve a name conflict and compile.

- Plain Maven project — **no Spring Boot**; keep it out of the POM.
- Test dependencies (JUnit, Mockito) stay at `test` scope.
- The LWJGL3 backend extracts natives automatically — do not re-add `maven-nativedependencies-plugin` or `antrun` launch hacks (those were LWJGL2-era workarounds).
- Assets live under `src/main/resources/assets/` and are loaded with the `assets/` prefix, e.g. `new Texture("assets/sprites/player.png")`. Do not flatten assets into the classpath root.
- The shared font (`ETGFanfic.font`) is a FreeType-generated pixel font (Press Start 2P, with Latin and Russian Cyrillic glyphs) created once in `ETGFanfic.create()` and disposed in `ETGFanfic.dispose()`. Screens must use it, not create/dispose their own.
- Never call `setUseIntegerPositions(true)` on fonts — the world is only 7×5 units and rounding to integer positions collapses small text to nothing.
- World: `FitViewport(7, 5)` - the camera shows 7x5 units (`Arena.VIEW_W/H`); props/rooms use 32 px per unit; family sheets use 64 (`Sprites.heroW/heroH`, logical 20×28 frames). Each room has its own geometry (`Room.arena`, an `Arena(cols, rows)`: walls, door gaps, layout grid; 13x8 = one screen); gameplay code uses the current room's `Arena.now` (set in `GameWorld.enterRoom`), code that builds/draws a specific room uses `room.arena`. `GameScreen.updateCamera` follows the player and leans toward the aim (and toward the boss in boss fights, always keeping the player well in view), clamped to the room (black beyond the walls); the HUD is drawn with a fixed camera in 7x5 screen units, world-space overlays (prompts, pad reticle) in `drawWorldOverlays`.
- Art is generated: `tools/gen_sprites.py` (hero, props + `props.txt` atlas, tilesets), `tools/gen_sfx.py`, `tools/music/tracks.py` (FluidSynth + GeneralUser GS soundfont from the Towards Dreams project; tracks: menu (the title screen; it fades in, `HubScreen.MUSIC_FADE_IN`), home ("Home Sweet Fluff", the living room: nylon guitar, Rhodes, clarinet, grandad's harmonica, brushes - also fades in), village, town, city (room themes get darker floor by floor: folk-rock village, harmonic-minor galloping town, cold synth city with a tolling bell; endless switches village → town → city → tower → sky at waves 6, 11, 16 and 21), boss (the Fluffy King's theme), boss_village, boss_town, boss_city, tower (lift-music intro, then office rock), boss_tower (orchestral, on the roof), sea / boss_sea (the secret sea and its shark), slums ("Back Alley Blues", the secret Slums: walking bass, Rhodes, muted trumpet), boss_slums ("Gutter Groove", slap bass, organ, brass), undercity ("Down Where the King Sleeps": a crooked music-box lullaby, dripping pizzicato, a bad king's fanfare), boss_undercity ("The False King's March": oom-pah tuba, snare rolls, a nasal oboe fanfare), sky ("Silver Lining", the clouds: harp, celesta, choir, then the band; their boss is the Fluffy King, so the boss track is his "boss") - `Stage.bossMusic`, `Boss.music()`; falls back to ffmpeg's built-in vorbis encoder when libvorbis is missing). Edit the scripts and regenerate rather than hand-editing the PNG/WAV/OGG files. Use Nearest filtering for pixel art.
- The player collides with a small feet box (`Player.feetRect`), not the sprite; enemies are circles pushed out of walls/props (`GameWorld.pushOutOfSolids`).
- Enemy sprites use `KinArt`, `RatArt` or `CastArt`; multipart bosses use `BossArt` and animated layers. Projectiles use `ProjectileArt` and `Draw` on the SpriteBatch. Keep collision geometry independent of the art. The actor pass renders at the full viewport resolution.
- Bullets (and enemy line of sight, lasers) are stopped by `Prop.bulletRect(sprites, northbound)`: a bullet going up the screen hits the whole tall part (up to just below the sprite top, so standing behind a pillar covers you); going down or sideways it only hits the footprint. Street lamps block with a slim 0.14 post and are breakable (30 hp). Road Rager uses the tall cover shape throughout its sweep. Bullets touching the player's visible body only break on it when cover is really between them (`GameWorld.coverBetween`) - never all-round immunity. Movement still uses the footprint (`solid`). Tests that mock libGDX classes need `MockMakers.SUBCLASS` (inline mocking fails on newer JDKs).
- Boss bullets: everything a boss fires during its `behave` is restyled by `GameWorld.styleBossBullets` - 1.3x bigger and 0.85x slower (hard mode: full speed, `GameWorld.bossBulletSpeed`), with `Bullet.hitScale` keeping the hitbox the same size (patterns read better, gaps don't shrink). Regular enemies' bullets are unchanged.
- Collisions are circle-vs-circle. The player's hitbox is a small circle around the belly (`Player.hitboxY()`, `HITBOX_RADIUS`), not the sprite rectangle.
- Bullets a blank (any `clearEnemyBullets`) sweeps away get `Bullet.blanked`: enemies that burst their own bullets where they stop (rockets, bombs, hairballs, bottles...) check it, never the blank ring's timing - the blank's daze outlasts the ring, so they'd only notice after it and burst on the hero.
- Don't remove from `enemyBullets` while it may be iterated — mark `alive = false` and let `GameWorld` sweep (taking damage and blanks clear bullets mid-update).
- Killed enemies move from `enemies` to `GameWorld.dying` and play their death there (`Enemy.updateDeath`/`drawDying`: regular ones topple away from the shot and shrink, pit fallers spin down, bosses get slow motion, ~2 s of explosions and a big blast); the room only clears when `GameWorld.bossDying()` is false. Code that needs a boss gone (rewards) waits for that.
- Spawn enemies via `GameWorld.spawnEnemy` (goes through a pending list), never by adding to `enemies` directly during update.
- Sound effects: `assets/sfx/*.wav` are generated by `tools/gen_sfx.py`; register new ones in `audio/Sfx` (the angels use `swish` / `twang`, birds `flap`, coins `coin`, the shop `kaching` and Baa-llo `baa`; `whistle` is the Evil Train's, `hiss` the city's burning drum going out, `siren` the Walking Building's alarm before its bullet curtain, `howl` the alley dog's). `sfx/death.wav` is `assets/die-sound.mp3` with its leading silence cut (needs ffmpeg), so it lines up with the death animation; `body_fall` plays when the hero hits the floor. Regenerating rewrites every WAV — only commit the ones whose audio actually changed.
- Lombok (`@Getter`, `@Data`) is used; keep the annotation processor path in `maven-compiler-plugin`.
- Commit and push after every finished change (once it builds and tests pass): one change per commit, with a sensible conventional message (`feat: ...`, `fix: ...`), then `git push`. Don't leave work uncommitted.
- Commit with `git -c core.autocrlf=input` from Linux shells: some older files have CRLF line endings on disk and the owner's Mac git normalizes them.
- Docs: `README.md` is for players; secrets stay in its collapsed "Spoilers" block. `DEVELOPING.md` is the build/onboarding entry point. Mechanic rules, full gun/item references, dev flags and asset/layout instructions live in `docs/` (start at `docs/README.md`). Update the relevant topic and catalog in the same change as the mechanic; keep the flag reference aligned with `launcher/Dev` and its call sites. The jar name appears in README, DEVELOPING and here - bump all three on a version change.
- The only loose files in `assets/` are `drop.mp3` (the pickup sound) and `die-sound.mp3` (the source of `sfx/death.wav`); everything else sits in its folder (`sprites`, `tiles`, `sfx`, `music`, `fonts`). The window icons are the stock `libgdx*.png` in the resources root.
