# Room generation, navigation, cover and pits

[Documentation index](README.md)

## Geometry and generation

`StageMap` normally builds a random tree on a grid, with a start, combat rooms, treasure,
boss and optional shop. It puts shops in dead ends with suitable entrances and keeps
boss entry away from the boss's top spawn edge. Stage-specific exceptions include the
straight skyscraper, narrow underground alleys, short sea map and rush arenas.

`Stage` owns themes, music, furniture mappings, barricades, pit chance and exits.
Most branching stages, including Under the Bed, grow to 10–12 rooms before adding the
boss and shop. Two of the original rooms become treasure rooms. Skyscraper, sea and
Moon have one treasure room each. Sea construction uses its own 6–7-room map.

Typical start/treasure rooms are 13×8 tiles, ordinary roomy fights 19×12, and ordinary
boss arenas 25×15. Some fights are small 13×8 rooms. Bosses can override dimensions,
layout, spawn position and entrance geometry: Kaiju has a shallow roof, Walking Building
a tall side-entered street, and Under the Bed its own arena.

Up to three roomy fights per Village, Town, City or Clouds floor use an octagon,
flat-tipped diamond, L, T or M. Each has 228 walkable tiles before
furniture and hazards, the same area as a 19×12 room. They use the regular encounter
budget, simultaneous spawn cap and reinforcement rules for that floor, depth and
difficulty. Secret-route rooms keep their dedicated layouts. Every shaped room keeps
its door landings on the cardinal edges. The M uses east and west doors because its
notches occupy the north and south landings. Wall collision, enemy navigation, floor
art and both maps use the same tiles, including the M's split rows.

`RoomTemplates` supplies 8×13 layout strings centered in bigger rooms. Add a layout to
`COMBAT` and use the stage's prop mapping, rather than hard-coding village furniture.
Tower widening and underground alley builders add their own geometry. `Room` retains
props, chests, pickups, visit/clear/lock state and special entrance/hazard data.

## Collision and directional cover

Movement uses prop footprints (`solid`). Bullet obstruction uses
`Prop.bulletRect(sprites, northbound)`: upward shots hit the tall body, while downward
or sideways shots generally use the footprint. A street lamp has a narrow post and can
break. Enemy sight, beams and lasers must honor the same cover geometry. Road Rager's
sweep uses the tall cover shape throughout.

A bullet touching the visible player body is absorbed only when cover is actually
between bullet and hitbox (`coverBetween`); hiding behind a prop is directional.
Do not turn a sprite overlap into immunity from every angle. Shop counter furniture
is an exception that lets bullets pass so the keeper can be shot.

Standing and flipped tables can be pushed by leaning on them. Movement is blocked by
walls, props, chests and boss blockers. A table pushed over a pit disappears into it.
Rolling can cross a standing table and knocks it once per roll; the player cannot end
the roll standing on it. Other props may be destructible or unbreakable by kind.

## Navigation and pits

Walking enemies use a 0.25-unit nav grid and `walkable` / `findWalkPoint` to flank
obstacles. `findSightPoint` looks for a firing position. Navigation and spawns use the
current room's geometry; `pointIsFree` also avoids pits for pickups/chests.

Pit probability and appearance come from the stage. They are placed away from doors,
center and props. Walking enemies route around them but knockback can shove them in.
Bosses and flyers ignore them. A player who stops on a pit falls and returns to the last
safe point for half a heart; a roll crosses safely. Floating King and the Flippers sea
entrance are deliberate exceptions. See [sea](deep-sea.md).

## Map interface

Both minimap and big map use `GameScreen.drawRooms`. Visited rooms show leftover unopened
chests by tier, keys, other pickups and open exits. Treasure Map reveals the graph.
The big map pauses simulation, allows teleport to visited rooms through
`GameWorld.teleportTo`, and cannot open during a locked fight. Camera and drawing rules
are in [interface](screens-and-interface.md) and [rendering](rendering.md).

## Implementation

- [StageMap](../src/main/java/com/trush/game/world/StageMap.java)
- [Room](../src/main/java/com/trush/game/world/Room.java)
- [Arena](../src/main/java/com/trush/game/world/Arena.java)
- [RoomTemplates](../src/main/java/com/trush/game/world/RoomTemplates.java)
- [Prop](../src/main/java/com/trush/game/objects/Prop.java)
- [RoomRenderer](../src/main/java/com/trush/game/gfx/RoomRenderer.java)
- [GameWorld](../src/main/java/com/trush/game/world/GameWorld.java)

## Existing checks

- [RoomPitsTest](../src/test/java/com/trush/game/world/RoomPitsTest.java)
- [EnemyNavigationTest](../src/test/java/com/trush/game/world/EnemyNavigationTest.java)
- [PropCoverTest](../src/test/java/com/trush/game/objects/PropCoverTest.java)
- [BedNavigationTest](../src/test/java/com/trush/game/world/BedNavigationTest.java)
