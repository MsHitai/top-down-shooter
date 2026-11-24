# Main-road stages and environmental mechanics

[Documentation index](README.md)

The route and encounter lifecycle are in [campaign](campaign.md); generation and cover
in [rooms and cover](rooms-and-cover.md). This page records how each ordinary stage
changes the room and its hazards. `Stage` is the authority for theme, furniture,
barricade, pit chance, night flag, music and exit mapping.

## Village, town and city

Village starts in a yard with cottage, barn, orchard and square fights, chapel treasure
and square boss arena. Hay, barrels, crates and pillars establish the basic furniture
rules; wooden planks lock doors. There are no ordinary village pits. Its gate leads on.

Town uses streets, markets, alleys and parks, with benches, lamps and road barricades.
Open sewer-hatch pits enter the ordinary combat layouts. City uses night streets,
subways, rooftops and construction, with cars, lamps and police tape. Lamps provide
light and slim directional cover; bullets remain readable above darkness. Town and city
may hide a mimic lamp. City also contains the secret Slums entrance and the shop's
Flippers/dousing stock. See [enemies](enemies.md), [Slums](slums.md) and [economy](economy-and-shops.md).

## Skyscraper climb

`StageMap.buildTower` stacks a lobby, elevator stops and roof northward rather than
branching. One stop is treasure, one a shop; combats use office, server and cafeteria
themes. `Room.floorNumber` rises by 5–10 storeys between stops, independent of the
campaign floor. Elevator transitions hold simulation, close the screen doors and count
storeys before the next room's floor greeting.

Tower fights are roomy and get bigger as the climb progresses: 19×12, 21×13, then 23×14.
`RoomTemplates.widen` adds cover. Desk, plant and water-cooler furniture replaces village
props, and doors use steel shutters. Encounter budget, crowd, bullet difficulty and
regular health rise with stop depth. Tougher cube/shapeshifter availability also changes.
The roof has open sky/parapets for its boss; Kaiju overrides it to a shallow arena.
The helicopter leaves for the clouds. See [difficulty](difficulty.md) and [bosses](bosses.md).

## Clouds and birds

The cloud stage is titled The Fluffy Kingdom. Cloudgate starts it, clouds/temple/storm
supply fights, rainbow is treasure, throne the boss room. Open caps let sky show outside
the cloud rim; storm uses darker sky. Cloud banks replace hay/tables, marble columns
replace pillars, golden urns barrels, presents crates, and cloud plugs lock doors.
No ordinary flip tables are generated here. Cloud holes are pits; a special fin-marked
hole leads to the [sea](deep-sea.md). The exit rainbow slides home, unless the completed
[rocket](rocket-and-console.md) offers a continuation.

During cloud combat, birds cross at roughly player height every 5.5–8.5 seconds. A
quarter of arrivals are a flock of three. An edge arrow warns for 0.9 s, then birds fly
straight across and vanish beyond the far edge. Touching one goes through player damage;
bullets ignore them. They are moving hazards, not enemies to kill or budget entries.
The same hazard can appear in an endless cloud arena.

Cloud enemy selection favors its own flying residents. The throne holds Fluffy King,
or Shadow King when playing the King. Defeating the boss uses stage-based ending logic,
which still works when detours have increased the numeric floor.

## Implementation

- [Stage](../src/main/java/com/trush/game/world/Stage.java)
- [StageMap](../src/main/java/com/trush/game/world/StageMap.java)
- [StoryDirector](../src/main/java/com/trush/game/world/StoryDirector.java)
- [GameWorld](../src/main/java/com/trush/game/world/GameWorld.java)
- [Bird](../src/main/java/com/trush/game/objects/Bird.java)
- [GameScreen](../src/main/java/com/trush/game/screen/GameScreen.java)

## Existing checks

- [StageTest](../src/test/java/com/trush/game/world/StageTest.java)
- [BirdTest](../src/test/java/com/trush/game/objects/BirdTest.java)
- [RoomPitsTest](../src/test/java/com/trush/game/world/RoomPitsTest.java)
