# Under the Bed residents, bosses and completion

[Documentation index](README.md)

## Access and route

`ETGFanfic.underBedUnlocked` requires a completed bed/calendar dream (`dreamFastest > 0`
or a recorded daily completion); boss rush alone does not open it. Baby sneezes near
the locked entrance as a hint. Once open, any selectable hero can crawl under through
the bedroom's `UNDER_BED` spot.

This is `Dream.Kind.UNDER_BED`, a single floor with 12–14 rooms, including two locked
treasure chests and Baa-llo's shop. The shop uses the quilt tiles and standard
first-floor stock and prices, including a key. The treasure chests follow the usual
gun-and-item split when both rewards are available. This trip has no disk save.
Beating the boss returns the hero to the bedroom entrance, with dream completion
records and kingdom wins unaffected.

## Room modifiers

Each room has one [Dream rule](dreams.md), including the boss arena. The shop and
treasure rooms are exempt. The entrance rolls Bouncy Bullets or Moon Boots, so players
can try the modifier before the first fight. A room keeps its rule on revisits, and
the HUD shows which rule is active. Generation draws combat modifiers without
repeats from the shared Dream pool.

Tough Crowd fills the encounter budget with Quilt Moths and Clothespeg Crocs, using
their ordinary stats and the room's active formation caps. Mixed Up lets eligible
residents borrow other enemies' volleys. The boss arena draws from the modifiers
that apply to a boss fight. Glass Cannon doubles damage in both directions there too.

## Encounter rules

On normal difficulty, `StoryDirector` sizes each regular encounter for the selected hero.
Depth and room size set the starting budget. Roomy fights get one extra point before
the hero multiplier, and a fractional point gives a chance to round up. Hard mode uses
its own larger groups. See the [floor benchmark](balancing.md#under-the-bed-floor-calibration)
for the survival target and measured routes.

Simultaneous residents cap at four normal or five hard. Only
one Wind-up Beetle may pursue at a time. Heavy formation caps are one in small rooms,
two in roomy fights, or three in deep hard-mode fights. Both active and pending actors
count. Rolling the total budget and admitting the next spawn are separate constraints.

Regular attacks communicate through body preparation rather than floor aim lines.
Their current formations are defined in `BedCritter` and `BedHeavy` (counts below are
before any world boss styling, which does not apply to these regular enemies).

| Resident | Current attack |
| --- | --- |
| Dust Mite | Five-shot fan, then three shots chosen from four gaps, leaving a rotating opening |
| Wind-up Beetle | Pursuit with sideways exhaust, then a ten-shot ring after stopping |
| Lost Marble | Two bank shots toward each side wall, three direct shots, then one reacquired shot |
| Little Dust Bunny | Three-shot fan, then a two-shot follow-up; can be squished |
| Clothespeg Croc | Two five-tooth closing courses and a four-shot central fan |
| Quilt Moth | Four parallel ribbons of six bullets; fixed heading and shared translated courses |
| Tumble Die | Two thirteen-shot open squares, 0.22 s apart, second rotated 12 degrees |

The Clothespeg Croc repositions around cover at a base speed of 1.03 world units per second.

The die leaves a three-pip doorway in each layer. Regular recoveries are intentionally
longer than boss beats; changing bullet counts, pauses and crowd caps together can make
a room much harder than an isolated pattern. See [balancing](balancing.md).

## Hero-specific bosses

| Hero | Resident | Pattern identity |
| --- | --- | --- |
| Baby | Dust Bunny | Piles inhaled from the room's edges, a seven-lane sneeze chain, evenly spaced lintels that hang a breath then sweep the lane, and tumbling clumps that read your walk |
| Boy | Monster Under the Bed | Evenly spaced crease walls and pleats, a hem that wraps as it passes, and a pillow fort that blooms full circles of feathers |
| Sister | Sock Ninja | A marked dash across the sheets, an evenly spaced shuriken pinwheel, closing scissors, and a full clothesline curtain |
| Granny | Yarn Hydra | Heads that pluck in turns, knitting Vs, expanding rings of evenly spaced beads, a braid with even initial spacing, and the traded knot that reads your walk |
| Grandad | Wind-up General | Ricochet banks, four blocks of marching ranks that fan out into charging lines, evenly spaced clock-face rings, spring shells and the tolling bell |
| King | Lint Regent | Evenly spaced button rows crossing on alternating axes, the collectors' patrol along the walls, the flicking necklace, and a slow sceptre sweep |

`Boss.underBed(hero)` selects the identity. `BedBoss` owns timing and signatures;
`BedPattern` builds the follow-up patterns. The Lint Regent plays four distinct
moves per cycle. The other residents play five. Turns use one to six beats with
distinct spacing. The wind-up pose announces body shots. Attacks that enter from
remote locations put lane warnings on the floor. The bot reads the same active
lane markers the game draws.
The Dust Bunny fights solo. Little Dust Bunnies appear in regular combat rooms.
The Sock Ninja telegraphs the dash with a wind-up pose and edge chevrons. The dash
deals half a heart on contact. Sidestepping after the wind-up starts clears it.

Body volleys stay small enough to read. Bait the wind-up and walk between shots
or around the edge of a formation. The lintels sweep past. Slow residue lingers
into the next attack, while fast shots and the General's marching ranks get time
to clear.
Everyone rages below 70% health (the ninja below half): beats come
a quarter faster and bullets fly a tenth faster.

All six residents have at most 700 HP. The General fires four 3×4 blocks in
quick succession, down, left, right, and up. The rear ranks quickly fan into
the outer slots while each formation accelerates. Each line tilts slightly so
the wall clips the shots in succession. The clock face fires four rings of 14
evenly spaced stars. The tolling bell fires slower rings with a rotating opening.
All six residents have an 8% projectile speed boost, with separate timing for
the General's ranks as they fold. The General's blue cartridges have a further
8% boost and deal half a heart per hit. Spring shells ricochet once, and bank
shots ricochet once or twice.
The hero's own roll reach still matters, particularly for Grandad. See
[balancing](balancing.md#under-the-bed-boss-calibration) for earlier bot measurements
and the conditions they used.

## Validation limits

The headless fairness tests sample central and wall-adjacent positions, both phases,
normal/hard difficulty and special-attack variants. They include lifetimes/rebounds and
seek a walking route after a 180 ms reaction delay without rolls or blanks - carrying an
inflated hitbox, so a passing route is a street rather than a lucky seam. Opening volleys are also checked against the player's roll speed and invulnerability
window. Tests bound the volley size and verify that a boss clears its previous shots
before firing a new attack. These tests cover sampled geometry and timing. Boss art/pupil layers
and native-resolution rendering are described in [assets](assets.md).

## Implementation

- [Dream](../src/main/java/com/trush/game/world/Dream.java)
- [StoryDirector](../src/main/java/com/trush/game/world/StoryDirector.java)
- [Stage](../src/main/java/com/trush/game/world/Stage.java)
- [HubScreen](../src/main/java/com/trush/game/screen/HubScreen.java)
- [GameScreen](../src/main/java/com/trush/game/screen/GameScreen.java)
- [ETGFanfic](../src/main/java/com/trush/game/launcher/ETGFanfic.java)
- [BedCritter](../src/main/java/com/trush/game/enemy/BedCritter.java)
- [BedHeavy](../src/main/java/com/trush/game/enemy/BedHeavy.java)
- [BedBoss](../src/main/java/com/trush/game/enemy/BedBoss.java)
- [BedPattern](../src/main/java/com/trush/game/enemy/BedPattern.java)
- [Boss](../src/main/java/com/trush/game/enemy/Boss.java)

## Existing checks

- [BedEncountersTest](../src/test/java/com/trush/game/world/BedEncountersTest.java)
- [BedChestTest](../src/test/java/com/trush/game/world/BedChestTest.java)
- [BedNavigationTest](../src/test/java/com/trush/game/world/BedNavigationTest.java)
- [BedEnemiesTest](../src/test/java/com/trush/game/enemy/BedEnemiesTest.java)
- [BedBossTest](../src/test/java/com/trush/game/enemy/BedBossTest.java)
- [BedPatternTest](../src/test/java/com/trush/game/enemy/BedPatternTest.java)
- [BedBossFairnessTest](../src/test/java/com/trush/game/enemy/BedBossFairnessTest.java)
