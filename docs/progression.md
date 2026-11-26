# Unlocks, achievements and the Flufficon

[Documentation index](README.md)

## Permanent progress

`Unlocks` loads persistent flags once at startup. `ETGFanfic` owns record/counter updates
and preferences; `Hero.load` follows unlock loading so a now-locked choice can fall back
to Boy. Progress reset clears unlocks, scores, records, seen story and saved run, while
keeping music/sound, fullscreen and the wear-crowns option.

| Trigger | Persistent result |
| --- | --- |
| Reach a road floor | `furthestFloor`, title-screen discovery feedback |
| Defeat Fluffy King as a regular hero | `wins`, Grandad and the Hard Mode/Story dare skull unlocked; eligible hero adds rocket piece |
| Any Baby ending | Separate `babyWins`, no regular rocket/crown progression |
| Defeat Skunk King | `falseKingBeaten`, separate from kingdom saves |
| Dive into the sea | `foundSea`, endless sea-wave eligibility |
| Defeat Fluffy Shark | `unlockedFlufful`; also proves old-save sea discovery |
| Carry Heirloom for kills | `heirloomKills`, retained between runs |
| All four builders defeat King | Completed family rocket |
| Board rocket | `boardedRocket`, console appears at home |
| Defeat Grandfluff | `trueEndings` (Baby uses own counter) |
| Eligible hero defeats Grandfluff hard | `crownedBy` and hard true-ending stat |
| All four builders crowned | Playable King and his room; Royal Family achievement |
| Complete bed/calendar dream | `dreamFastest` / daily records and Under the Bed entrance |

Boy, Sister, Granny and Grandad build the rocket; Baby and King do not. Crown visibility
is an option separate from earning one. `Unlocks.crownOwners` and `rocketBuilders` allow
dev display overrides without writing earned progress. Use these helpers consistently
in the hub and gameplay. The King faces Shadow King on his cloud throne.

## Discovery records

`Flufficon` stores seen/defeated enemies and bosses, found guns/items/synergies, statistics
and achievements under `dex*` keys. Enemy IDs default to the class name; shared variant
classes such as rats, cubes and bed residents override `dexId`. Add new ordinary enemies
to `Flufficon.ENEMIES`; boss enumeration comes from `Boss.values`, guns from `allGuns`.

`GameWorld.spawnEnemy` reports sightings, death handling reports defeats/flawless,
collection reports equipment, and shop/chest code reports stats. The hero's starter is
recorded on a new world. New discoveries are written promptly; accumulated counts flush
when `GameScreen` hides and when the game disposes. Older saves infer only discoveries
their established wins/unlocks prove, rather than revealing every entry.

The trophy shelf opens `FlufficonBook`: records, achievements, enemies, bosses, guns,
items and synergies. Unseen entries remain `?`; portraits use `drawPortrait` and cell
clipping. Q/E or LB/RB turns pages. Shared achievement banners draw over all screens.
Boss portraits show the best [rematch medal](dreams.md#boss-rematches). Gold replaces silver,
and the medals are saved under `dexBossMedals`.

## Achievements and run bookkeeping

Predicates in `Flufficon.Achievement` are checked about every half-second, with new
announcements queued. The enum is the full achievement catalog. Examples of cross-system
conditions are five flawless bosses in one run (Untouchable), every campaign stage in
one won run (Grand Tour), hard Grandfluff (Not Scared of Skulls), all builder crowns
(Royal Family), all dream bosses, boss rush, and five completed daily dates.

`stagesSeen` and `flawlessBosses` travel with `RunSave`. Under the Bed is excluded from
the campaign-wide stage requirement. Do not award a family boss multiple times for
splits, count a normal ending as a kingdom win, or count an Under the Bed clear as an
ordinary dream completion. See [campaign](campaign.md), [saving](saving.md) and [dreams](dreams.md).

## Implementation

- [Unlocks](../src/main/java/com/trush/game/save/Unlocks.java)
- [Flufficon](../src/main/java/com/trush/game/save/Flufficon.java)
- [ETGFanfic](../src/main/java/com/trush/game/launcher/ETGFanfic.java)
- [FlufficonBook](../src/main/java/com/trush/game/screen/FlufficonBook.java)
- [AchievementBanner](../src/main/java/com/trush/game/screen/AchievementBanner.java)
- [Hero](../src/main/java/com/trush/game/player/Hero.java)
- [Crown](../src/main/java/com/trush/game/player/Crown.java)

## Existing checks

- [FlufficonTest](../src/test/java/com/trush/game/save/FlufficonTest.java)
- [RocketTest](../src/test/java/com/trush/game/save/RocketTest.java)
- [BabyBrotherTest](../src/test/java/com/trush/game/player/BabyBrotherTest.java)
- [DreamTest](../src/test/java/com/trush/game/world/DreamTest.java)
