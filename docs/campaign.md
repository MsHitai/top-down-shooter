# Campaign floors, encounters and endings

[Documentation index](README.md)

## Main road and detours

| Route | Stage | Way onward |
| --- | --- | --- |
| Floor 1 | Village | Gate to town |
| Floor 2 | Town | Bus to city |
| Floor 3 | City | Skyscraper doors, or hidden Slums stairs |
| Floor 4 | Skyscraper | Helicopter to clouds |
| Floor 5 | Clouds / Fluffy Kingdom | King victory, optional sea detour or rocket |
| Secret route | Slums → Undercity | False King ending, optionally rejoin at skyscraper |
| Secret detour | Clouds → Deep Sea → same cloud room | Waterspout returns to the departure room |
| Rocket continuation | Clouds → Rocket Ride → Moon | Grandfluff and true ending |

`Stage.forFloor` describes the ordinary road; secret destinations use an explicit stage.
The numeric floor also influences difficulty and can exceed five after detours. Completion
uses `Stage.endsTheRun` and `GameScreen.isLastFloor`, not a comparison with a single final
floor number. Continuing after the False King goes to skyscraper floor 6 and clouds floor 7.
Stage-specific tower/elevator/cloud hazards are in [main-road stages](main-stages.md).
See [Slums](slums.md), [Undercity](undercity.md), [sea](deep-sea.md) and [Moon](moon.md).

## Encounter lifecycle

`StoryDirector.onRoomEntered` marks visits and configures difficulty/health. New combat
rooms lock, build a budgeted enemy queue and trickle enemies in. Small rooms have smaller
budgets and fewer active enemies. Spawn counts include pending enemies; queueing must
not bypass encounter caps. The director chooses from the stage's population, with limits
for disruptive enemies and deeper variants.

A roomy combat room may have reinforcements. Chance is
`min(0.7, 0.12 + 0.07 * (floor - 1))`; the second budget is 60% of the first, at least two.
Hard mode enlarges the budget without separately increasing that chance. Reinforcements
start after the first group is completely gone, with a brief sound/shake warning.

A room clears only when queues, reinforcements and active enemies are empty and the boss
death show has ended. Ordinary doors wait one second after the last enemy falls. Boss
rooms use the death show as the breather. Clearing grants room rewards and may open an
exit. A mimic waking in an already-cleared room relocks it without granting a second reward.

## Loot and transitions

Treasure, shop and boss rewards reserve equipment from shared pools; see
[loot](loot-and-chests.md). A two-treasure floor tries to provide one gun and one item.
Between-floor snapshots carry the player's build, stats and explicit destination stage.
Blanks are topped up to at least two, with Spare Pocket/Key Ring additions; direct secret
transitions have matching grant logic. See [saving](saving.md).

## Win accounting

The False King produces the normal ending and `falseKingBeaten`, not a kingdom win.
The player can go home or continue with the build toward the real King. Defeating the
Fluffy King normally calls `addWin`; boarding the rocket counts that win at stage clear,
then continues the same run. Grandfluff records the true ending after the earlier King
win. Baby results use `babyWins` rather than regular win/unlock counters. Dreams and Under
the Bed follow their own completion paths; see [dreams](dreams.md) and [Under the Bed](under-the-bed.md).

## Implementation

- [StoryDirector](../src/main/java/com/trush/game/world/StoryDirector.java)
- [Stage](../src/main/java/com/trush/game/world/Stage.java)
- [Director](../src/main/java/com/trush/game/world/Director.java)
- [GameScreen](../src/main/java/com/trush/game/screen/GameScreen.java)
- [RunSave](../src/main/java/com/trush/game/save/RunSave.java)

## Existing checks

- [StageTest](../src/test/java/com/trush/game/world/StageTest.java)
- [ReinforcementsTest](../src/test/java/com/trush/game/world/ReinforcementsTest.java)
- [UndercityTest](../src/test/java/com/trush/game/world/UndercityTest.java)
- [RunSaveTest](../src/test/java/com/trush/game/save/RunSaveTest.java)
