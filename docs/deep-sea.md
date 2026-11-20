# Deep Sea detour and underwater rules

[Documentation index](README.md)

## Diving and returning

Every cloud map chooses a combat room for a shark-fin hole (`seaHole`, `seaPit`),
preferring a roomy fight and falling back to a smaller fight when necessary.
Stopping on it with Flippers redirects a fall into the sea; without them it is an
ordinary damaging fall with a hint. The King can dive with Flippers despite floating.
Flippers come from the city shop, cost 65, and also fire homing bubbles after rolls.

`GameWorld` retains the departure director, map and room. The sea director builds a
6–7-room detour using the same loot pools, two effective levels above the departure
floor, with three extra budget points and one extra active enemy. It has no shop, one
wreck treasure room, and a boss lair. The waterspout surfaces in the original room,
counts its interrupted fight as cleared, replenishes floor resources and closes the
hole. This is one dive per run, not an ordinary next-floor increment.

`Unlocks.findSea` records discovery on a story dive. That enables endless sea waves;
an old save with the Flufful unlocked also proves sea discovery.

## Environment and enemies

Themes are seabed, reef, kelp, trench, wreck and lair. Furniture becomes coral, rock
spires, seaweed and clams; locked doors get kelp. `drawUnderwater` adds tint, shafts and
bubbles. Fire cannot burn underwater; ordinary fire/gas behavior must honor `canBurn`.
Chomper and Rubber Duck projectiles swim faster. Sea mines are room hazards, not enemies:
nearby feet or walking enemies arm them, then they flash and blast, chaining nearby
mines; a player bullet sets one off immediately.

| Creature | Attack / defense |
| --- | --- |
| Flufferfish | Puff and needle rings, offset second ring at higher difficulty |
| Starfish | Three turning star volleys |
| Diver | Harpoon aim, lock, fast shot; limited population |
| Turtle | Armored shell ram; takes one-quarter damage while shelled |
| Swordfish | Lined-up dash; gets stuck when hitting a wall or prop |
| Giant Crab | Warned claw crescent or clap bubble rings |
| Giant Octopus | Turning spiral or tracked ink bomb that bursts |

`SeaCreatures` shares costs, selection and creation between story and endless. Large
creatures cost four with a one-per-room group limit; divers have their own cap.

## Boss unlock order

The first boss is Fluffy Shark. Fin guns alternate; warned bites lunge; tail waves stun
unless rolled through. Below half health it adds paired waves and bite teeth. Moving
toward the shark with the roll can cross both enraged waves; rolling away is not the
same escape. A wave does not re-stun a player already stunned.

Beating the shark unlocks the infinite-ammo Flufful permanently, without requiring
flawless. Later sea visits choose among Shark, Kraken and Gorgon, preferring an unbeaten
boss. Kraken warns tentacle slams/sweeps, exposes raised tentacles to shots, and adds
whirlpool pull when enraged. Gorgon uses venom, delayed stone shots and a gaze that
petrifies a player who is both in her cone and looking at her when it flashes.

The same gating applies to the rush sea choice. Normal dreams exclude sea bosses because
their arenas are dry. Endless floods waves 26–30 only after discovery; see [endless](endless.md).

## Implementation

- [StoryDirector](../src/main/java/com/trush/game/world/StoryDirector.java)
- [GameWorld](../src/main/java/com/trush/game/world/GameWorld.java)
- [StageMap](../src/main/java/com/trush/game/world/StageMap.java)
- [SeaCreatures](../src/main/java/com/trush/game/enemy/SeaCreatures.java)
- [SeaMine](../src/main/java/com/trush/game/objects/SeaMine.java)
- [FluffyShark](../src/main/java/com/trush/game/enemy/FluffyShark.java)
- [Kraken](../src/main/java/com/trush/game/enemy/Kraken.java)
- [Gorgon](../src/main/java/com/trush/game/enemy/Gorgon.java)
- [Unlocks](../src/main/java/com/trush/game/save/Unlocks.java)

## Existing checks

- [FloorLootSaleTest](../src/test/java/com/trush/game/world/FloorLootSaleTest.java)
- [SeaCreaturesTest](../src/test/java/com/trush/game/enemy/SeaCreaturesTest.java)
- [SeaBossesTest](../src/test/java/com/trush/game/enemy/SeaBossesTest.java)
- [SeaMineTest](../src/test/java/com/trush/game/objects/SeaMineTest.java)
