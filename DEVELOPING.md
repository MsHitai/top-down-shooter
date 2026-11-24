# Developing Enter the Fluffy

### Build and run

    mvn clean package
    java -jar target/game-0.16.0-SNAPSHOT-jar-with-dependencies.jar

or run `com.trush.game.launcher.AppLauncher` from the IDE. Java 21, libGDX 1.13.5 (LWJGL3), Maven.

## Mechanics reference

The technical reference is organized by mechanic in [docs/README.md](docs/README.md).
It contains secret routes, unlocks and endings. Start with:

- [Architecture and ownership](docs/architecture.md)
- [Campaign and encounter flow](docs/campaign.md), [rooms and cover](docs/rooms-and-cover.md)
- [Weapons](docs/weapons.md) and [gun tables](docs/gun-reference.md)
- [Items](docs/items.md) and [item tables](docs/item-reference.md)
- [Enemies](docs/enemies.md), [bosses](docs/bosses.md), [difficulty](docs/difficulty.md)
- [Saving](docs/saving.md) and [progression](docs/progression.md)
- [Dev flags and recording](docs/debugging.md), [balancing bot](docs/balancing.md)
- [Generated assets](docs/assets.md), [rendering](docs/rendering.md)

The index also covers controls, home/UI, every secret floor, endless, dreams, Under the
Bed and the rocket games. New room layouts go in `RoomTemplates.COMBAT`; see
[room generation](docs/rooms-and-cover.md).

## Verification and maintenance

Run `mvn clean package` before finishing a change; it runs the existing tests and builds
the executable fat JAR. For debug shortcuts use `mvn clean package -Pdev`, then pass
`-Dfluffy.*` flags before `-jar`. A normal build ignores them.

Update the relevant `docs/` topic in the same change as its mechanic, including the gun
or item tables when stats change. Keep this file focused on getting started rather than
copying the mechanic descriptions back here. Coding conventions and the commit/push and
release workflow remain in [AGENTS.md](AGENTS.md).
