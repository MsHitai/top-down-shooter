# Architecture and runtime

[Documentation index](README.md)

The desktop entry point is `AppLauncher`; `ETGFanfic` is the libGDX `Game`. It owns the
shared batch, font, viewport, drawing helpers, sprite atlases, sound effects, gamepad and
preferences. Screens borrow those resources. Use `game.text(...)` for the shared pixel font;
screens must not create or dispose their own copy.

## Ownership and simulation

| Layer | Responsibility |
| --- | --- |
| `screen/*` | Menus, input modes, camera, HUD, transitions and presentation |
| `world/GameWorld` | Current room, player, actors, projectiles, collision, interactions and effects |
| `world/Director` | Mode policy: encounters, rewards, music, banners and completion |
| `StoryDirector` | Campaign floors, secret routes, dreams, rush and Under the Bed |
| `WaveDirector` | Endless encounter/reward/shop loop |
| `Room`, `StageMap`, `Arena` | Room state, graph generation and geometry |
| `player/*`, `objects/*`, `enemy/*` | Actors, equipment, hazards and attack behavior |
| `save/*` | Between-floor snapshots, permanent unlocks and discovery records |
| `gfx/*`, `audio/*`, `effects/*` | Rendering, sound and particles |

`GameScreen` controls whether simulation advances. Pause, the big map, boss introductions,
and transitions can hold the world still. A normal frame polls the gamepad in
`ETGFanfic.render`, processes the current screen, updates the world when allowed, and draws
world and interface passes. Bot and recording modes use fixed simulation steps; see
[debugging](debugging.md) and [balancing](balancing.md).

## Coordinates and collision

The normal `FitViewport` shows 7 by 5 world units (`Arena.VIEW_W/H`). A room can be larger
than the view. `Arena.now` is selected by `GameWorld.enterRoom`: gameplay uses the current
arena, whereas generation or rendering for another room must use `room.arena`.

Movement uses the player's feet rectangle and enemies' collision circles; projectile
hits use a small belly circle. Art size is independent of those shapes. Sprite source
resolutions differ between heroes, props, guns and home art; use `Sprites` size helpers
rather than deriving collision dimensions from texture pixels.

## Mutation rules

Spawn enemies through `GameWorld.spawnEnemy`, which queues them for safe insertion.
Do not append to the active collection from an enemy's update. When clearing bullets,
mark them dead and let the world sweep them; damage and blanks can occur during iteration.
Killed enemies move to `dying`, where their death animation continues. A boss fight does
not finish until its death show ends.

Route player-caused damage through `GameWorld.damageEnemy`: shield charge, kill credit,
loot, records and equipment growth depend on this path. Keep rendering free of combat
state progression; headless attack tests must produce the same timing as a drawn fight.

For a change, start with the mechanic's document in the [index](README.md), then its
implementation and tests. Future plans belong in [ROADMAP.md](../ROADMAP.md), shipped
history in [CHANGELOG.md](../CHANGELOG.md).

## Implementation

- [AppLauncher](../src/main/java/com/trush/game/launcher/AppLauncher.java)
- [ETGFanfic](../src/main/java/com/trush/game/launcher/ETGFanfic.java)
- [GameWorld](../src/main/java/com/trush/game/world/GameWorld.java)
- [Director](../src/main/java/com/trush/game/world/Director.java)
- [GameScreen](../src/main/java/com/trush/game/screen/GameScreen.java)
- [Arena](../src/main/java/com/trush/game/world/Arena.java)
