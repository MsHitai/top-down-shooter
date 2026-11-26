# Slums entrance, acid gang and trap rooms

[Documentation index](README.md)

## Opening the secret stairs

Each city map tries a roomy combat dead end without a north door for `slumsWay`, then
relaxes to other suitable fights and, rarely, another non-shop/non-boss room. Boards cover the top wall behind a burning oil drum. While the drum burns,
neither it nor the boards breaks. Wet or ice attacks call `GameWorld.splashDrum`; after
`Prop.SPLASHES_TO_DOUSE`, the fire goes out, the boards become destructible, and breaking
them opens `Room.stairsOpen`. E/A descends only when the room is unlocked.

Water Pistol, Bubble Blaster, Snowball Slinger, Freeze Ray and Frost Rounds can douse it.
Freeze Ray sends periodic splashes along its beam. City shop stock guarantees a suitable
gun when the player has no dousing option. Baa-llo has hints about the entrance.
`enterSlums` abandons the city permanently, shares loot pools, and tops up floor resources.

## Floor shape and hazards

Slums is a secret, unnumbered stage treated as floor 4 for progression. It uses night
lighting, junk furniture, lamps and alleys: 29×8 horizontally, 11×20 vertically, sometimes
19×12 courtyards or small rooms. An alley is not a small room merely because one axis
fits the screen. Each ordinary alley/courtyard has persistent old acid pools stored on
the room and recreated on entry; they do not dry or get evicted like temporary puddles.
Themes are backalley, slumstreet, underpass, junkyard, hideout, emptylot and store_slums.

| Enemy | Behavior |
| --- | --- |
| Sludger | Lobs acid globs |
| Acid Sprayer | Warned arc, then sweeping acid drops and periodic pools |
| Barrel Roller | Bowls a drum; shoots can detonate it early into a pool and ring |
| Marked rats | Larger packs, cannot be squished by rolling |
| Stink Pigeon | Flying pellets burst into stink gas |

The acid gang is acid-proof; selectors limit each kind (normally two per roomy room,
one per small room). Marked critters enter deeper rooms. Their shared green ear/eye
mark is drawn by `Marked`. The full Slums population is not added to endless.

## Trap rooms

The Slums draw two different traps from a pool of five and keep at least two ordinary fights.
The Village, Town, City and Undercity each have a 10% chance to replace one ordinary fight
with a trap room. The City room with the hidden Slums entrance is reserved. Trap rooms
stop at the skyscraper. Dreams use their existing Slums rooms when they draw that stage.

- **Rising acid.** Pools bubble up and persist for the duration.
- **Bursting pipes.** Pipes hiss before flame jets cross the alley.
- **Runaway garbage cart.** A cart rattles at one end, then barrels down the room.
  Go around its sides. Later passes reverse direction and shift off the centre.
- **Sewer backflow.** The channel gurgles before the surge pushes grounded actors along it.
  The next surge reverses direction. The curbs and raised islands give you dry footing.
- **Pressure manholes.** Stepping onto a closed lid opens it and drops the hero along with
  grounded enemies within 0.7 world units. The usual pit rules apply to dodge rolls,
  floating and fall damage. The lids reset when the timer expires.

The doors lock for a 20-second survival timer. A gunner joins first, then rat packs
arrive at four-second gaps. The HUD changes from `SURVIVE N` to `FINISH THEM!` when the
timer ends. Temporary trap puddles dry afterward, and the doors open after the last
enemy and the normal door delay. See [status effects](status-effects.md).

## Bosses and the way down

Blue Blob splits in two at 75% family health and each half again at 50%. Family members
share one boss bar, introduction and flawless record. Individual pops use splats rather
than ending the fight until the family is gone.

Alley Cat begins on a carried sofa, with claw crescents, bursting hairballs and coins.
Below 60% the sofa stays on the floor, Robbers join, and he prowls with warned pounces,
landing rings, double swipes and hairballs. A leap is untouchable.

Walking Building advances down a 17×40 street, entered from the side near the top.
Its facade is a moving solid blocker and shootable body; player push-out runs before
and after movement so a nudge cannot eject the hero outside the room. Windows warn and
fire, curtains sweep, antenna arcs leave gaps, and the mouth spits junk/acid. Below 40%
it enrages. It must not be selected for an incompatible top/bottom-entered boss room.

All three leave a sewer-hatch exit to [Undercity](undercity.md). They remain secret and
are excluded from ordinary random endless bosses.

## Implementation

- [StageMap](../src/main/java/com/trush/game/world/StageMap.java)
- [StoryDirector](../src/main/java/com/trush/game/world/StoryDirector.java)
- [TrapRoom](../src/main/java/com/trush/game/world/TrapRoom.java)
- [GameWorld](../src/main/java/com/trush/game/world/GameWorld.java)
- [Prop](../src/main/java/com/trush/game/objects/Prop.java)
- [Sludger](../src/main/java/com/trush/game/enemy/Sludger.java)
- [AcidSprayer](../src/main/java/com/trush/game/enemy/AcidSprayer.java)
- [BarrelRoller](../src/main/java/com/trush/game/enemy/BarrelRoller.java)
- [BlueBlob](../src/main/java/com/trush/game/enemy/BlueBlob.java)
- [AlleyCat](../src/main/java/com/trush/game/enemy/AlleyCat.java)
- [WalkingBuilding](../src/main/java/com/trush/game/enemy/WalkingBuilding.java)

## Existing checks

- [SlumsWayTest](../src/test/java/com/trush/game/world/SlumsWayTest.java)
- [TrapRoomTest](../src/test/java/com/trush/game/world/TrapRoomTest.java)
- [SlumsEnemiesTest](../src/test/java/com/trush/game/enemy/SlumsEnemiesTest.java)
- [SlumsBossesTest](../src/test/java/com/trush/game/enemy/SlumsBossesTest.java)
