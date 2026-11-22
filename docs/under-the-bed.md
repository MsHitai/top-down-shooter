# Under the Bed residents, bosses and completion

[Documentation index](README.md)

## Access and route

`ETGFanfic.underBedUnlocked` requires a completed bed/calendar dream (`dreamFastest > 0`
or a recorded daily completion); boss rush alone does not open it. Baby sneezes near
the locked entrance as a hint. Once open, any selectable hero can crawl under through
the bedroom's `UNDER_BED` spot.

This is `Dream.Kind.UNDER_BED`: one floor, fixed `Stage.UNDER_BED`, six-room target,
one treasure room, no shop, no random dream rules and no disk save. Chest generation
forces a gun even if the selected tier is exhausted. The ending returns to the bedroom
entrance; it does not increment dream completion records or kingdom wins.

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

The die leaves a three-pip doorway in each layer. Regular recoveries are intentionally
longer than boss beats; changing bullet counts, pauses and crowd caps together can make
a room much harder than an isolated pattern. See [balancing](balancing.md).

## Hero-specific bosses

| Hero | Resident | Pattern identity |
| --- | --- | --- |
| Baby | Dust Bunny | Piles inhaled from the room's edges, a sneeze chain threading its own rotating gap, lintels that hang a breath then sweep the lane, and tumbling clumps that read your walk |
| Boy | Monster Under the Bed | Crease walls from the edges with a shared doorway, twin pleats, the parted hem that wraps as it passes, and a pillow fort that lands, waits, then blooms feathers |
| Sister | Sock Ninja | A marked dash across the sheets, the shuriken pinwheel, closing scissors, and a clothesline curtain whose gap holds one side all attack |
| Granny | Yarn Hydra | Heads that pluck in turns, knitting Vs that leave diamond streets, the dropped loop rolling away with a wide turning door, and the traded knot that reads your walk |
| Grandad | Wind-up General | Ricochet banks, marching ranks that close up and widen into a single row before crossing the baited position, the fixed clock-face opening, spring shells and the tolling bell |
| King | Lint Regent | Button levies crossing on alternating axes, the collectors' patrol along the walls, the royal seal that lands where you stood and opens outward, and the flicking necklace |

`Boss.underBed(hero)` selects the identity. `BedBoss` owns timing and signatures;
`BedPattern` builds four body-fired patterns per resident, so every resident plays
five distinct moves before the dance repeats - and no move is shared between
residents. Turns use two to six beats with distinct spacing. The wind-up pose
announces body shots; only attacks that enter from remote locations put lane
warnings on the floor. The bot reads the same active lane markers the game draws.
The Sock Ninja's dash telegraphs the same way - pose plus edge chevrons, never a
band drawn on the floor - and costs a whole heart if it connects; sidestepping any
time after the wind-up starts clears it.

Body volleys stay small enough to read, and every move is a formation with a
walking door, in the floor residents' idiom: aim commits at release, so baiting the
wind-up defeats the pattern, and nothing converges on the hero - the seal and the
lintels open outward or sweep past, never close in. Slow residue lingers into the
next attack (only fast shots must finish first), so the room stays busy without
smothering it. Everyone rages below 70% health (the ninja below half): beats come
a quarter faster and bullets fly a tenth faster.

All six residents have at most 700 HP. The General's blue cartridges deal half a
heart per hit. Spring shells ricochet once, and bank shots ricochet once or twice.
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
