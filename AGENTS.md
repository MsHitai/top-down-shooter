# AGENTS.md

## Project

Top-down shooter in the spirit of *Enter the Gungeon*, written in **Java 21** with **libGDX 1.13.5** (LWJGL3 desktop backend). Built with **Maven** (deliberately not Gradle — the owner knows Maven).

## Build & run

- Build: `mvn clean package`
- Run: `java -jar target/game-0.0.1-SNAPSHOT-jar-with-dependencies.jar` (the fat jar, not the thin one)
- Run from IDE: `com.trush.game.launcher.AppLauncher`

## Conventions

- Plain Maven project — **no Spring Boot**; keep it out of the POM.
- Test dependencies (JUnit, Mockito, Hamcrest) stay at `test` scope.
- The LWJGL3 backend extracts natives automatically — do not re-add `maven-nativedependencies-plugin` or `antrun` launch hacks (those were LWJGL2-era workarounds).
- Assets live under `src/main/resources/assets/` and are loaded with the `assets/` prefix, e.g. `new Texture("assets/player.png")`. Do not flatten assets into the classpath root.
- The shared font (`ETGFanfic.font`) is a FreeType-generated pixel font (Press Start 2P, Latin-only) created once in `ETGFanfic.create()` and disposed in `ETGFanfic.dispose()`. Screens must use it, not create/dispose their own.
- Never call `setUseIntegerPositions(true)` on fonts — the world is only 7×5 units and rounding to integer positions collapses small text to nothing.
- World: `FitViewport(7, 5)` world units. The floor texture has walls whose thickness (world units) is encoded as `WALL_LEFT/RIGHT/BOTTOM/TOP` constants in `FirstStageScreen.restrictMovement()`; tune those if the room asset changes (decrease to move closer to the wall).
- Sprites: keep image files cropped to content (no large transparent margins) — movement clamps act on sprite bounds, so padding makes collision/clamping look wrong. Sprite sizes are derived from texture aspect ratio, not hardcoded.
- Lombok (`@Getter`, `@Data`) is used; keep the annotation processor path in `maven-compiler-plugin`.
- `listener/DropListener.java` and `listener/FluffyListener.java` are leftover libGDX tutorial code, candidates for deletion.
