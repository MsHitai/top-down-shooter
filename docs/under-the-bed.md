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

`StoryDirector` adds budget for this short trip, while preserving normal health scaling
and hard-mode behavior. Simultaneous residents cap at four normal or five hard. Only
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
| Baby | Dust Bunny | Spokes, cheek puffs, rolling dust and a little helper |
| Boy | Monster Under the Bed | Travelling folds, hems and pillows |
| Sister | Sock Ninja | Shuriken arms, scissor cuts and socks |
| Granny | Yarn Hydra | Spirals, braids and strands |
| Grandad | Wind-up General | Marching ranks, clock gaps, springs and bank shots |
| King | Lint Regent | Open crowns, diamond seals, buttons and crossed levies |

`Boss.underBed(hero)` selects the identity. `BedBoss` owns turns and signatures;
`BedPattern` builds three body-fired patterns per resident. Attacks run in three volleys
with short wind-ups/recovery and accent colors for aimed or pattern-breaking shots.
The main fronts travel at wall-of-bales speeds (base 1.4-1.9, aimed accents 1.7-2.6)
with wide paths between bullets. Each body attack marks three lanes during its wind-up.
Fast lead shots follow those fixed lanes in order, one ahead of each slow volley.
Body beats are 0.27 s apart normally, 0.21 s enraged. After recovery,
the next wind-up normally waits until the tracked previous front will have at most
0.6 s remaining at release.
Slow tails cannot stall the fight indefinitely: the extra hold caps at 1.7 s normally
or 1.2 s enraged, adjusted by boss pace, then the next attack proceeds anyway. This
tracking runs in simulation, including ricochets and curved shots, and cannot depend
on whether the boss was drawn.

Each boss has a main projectile color. Dust Bunny uses teal, the Monster blue,
Sock Ninja violet, Yarn Hydra coral, Wind-up General amber, and Lint Regent pink.
The General's spring and bank shots use cartridges that point along their flight path.

Each resident has its own base health, tuned against the assigned hero and starter weapon
with the average bot's wider-gap movement and safer roll choices.
Below half health, Dust Bunny fires an accent-colored aimed spark on each beat of its
spoke and cloud patterns. Its rolling clump accelerates harder in rage, as do Wind-up
General's spring shells. See the [boss sampling results](balancing.md#under-the-bed-boss-calibration)
for the fight setup and observed wins.

General's ranks/clock/bank shots retain walking openings; Sock Ninja's signature crossed
cuts commit to the initial target while other aimed attacks can reacquire. The Monster's
crease and the Regent's levy are Cannonbalrog walls: one fold thick, never accelerating,
spanning the room with a single 0.95-wide committed door offset from the marked spot -
walk to the door, or roll straight through the fold. The Regent's levy sends one row of
buttons from each end per beat, sharing one lane through all three crossings. Openings
vary between attacks. Body formations share their wind-up with the marked lead lanes.
Remote attacks and incoming off-screen shots have their own warnings, including returning ricochets.
Grandad keeps his own shorter roll; fairness must not assume the boy's reach.

## Validation limits

The headless fairness tests sample central and wall-adjacent positions, both phases,
normal/hard difficulty and special-attack variants. They include lifetimes/rebounds and
seek a walking route after a 180 ms reaction delay without rolls or blanks - carrying an
inflated hitbox, so a passing route is a street rather than a lucky seam. Every volley
of the opening attacks must also be crossable by a single dodge roll at the player's
real roll speed and invulnerability window. Bullet counts per attack are bounded both
below (the fight never goes quiet) and above (no wall of noise). This checks
sampled geometry, not every combined fight or human playability. Boss art/pupil layers
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
