# Run saves, restoration and compatibility

[Documentation index](README.md)

## What can be saved

A story trip can be saved only between floors after defeating its boss, via the stage
clear menu or closing the window there. It resumes at the next floor, not in a room or
mid-fight. `GameScreen.saveRun` gates the write; `RunSave` is also used as an in-memory
carrier for transitions that are not disk saves. Dreams/rush/Under the Bed do not offer
disk resume. Starting endless does not consume an existing story trip.

The JSON lives in preferences under `savedRun`, with schema version 2. `read` tolerates
unknown JSON fields but rejects a different version, invalid next-floor number or broken
JSON and deletes that stored snapshot. Continuing normally consumes the save; starting
a new story trip deletes it. The dev `keepSave` flag changes consumption only in dev builds.

## Snapshot contents

`afterFloor` captures the next floor and explicit destination stage, score/kills, health
and maximum health, blanks, keys, coins, junk/knight awakening, pin uses, shop grudge,
selected weapon, hero, hard-mode flag, stages visited and flawless-boss count.
Story dares are recorded by enum name in `dare` and `secondDare`. The second field is optional,
so saves with zero or one dare still resume with their original selection.
The skull stores the confirmed selection for new Story trips in the `storyDares` preference.
Continuing or deleting a saved trip leaves that preference intact.
Equipment is represented by stable gun names and item enum names, with the equipped
and bagged active items stored separately.

`GunData` contains remaining magazine/ammo, upgrades in application order, growth kills,
and a `full` marker used for Dream Catcher. Do not rename saved gun identities without
updating `Weapon.byName`; use `Item.byName` for compatible item aliases.

## Restoration order

`startFloor` reconstructs gun factories, applies hard stats and saved upgrades, restores
growth and remaining ammo, resolves item names and restores the trip's hero. It builds
the appropriate `StoryDirector`, restores stats/run flags, then restores health,
inventory, money, junk, pin and shop state. Passive restoration must not grant one-time
pickup effects again or refill existing guns.

A new floor starts with at least two blanks, plus Spare Pocket's extra blank. Key Ring
grants one key per floor. Direct secret transitions have corresponding grant code and
must stay consistent. Synergies are recalculated from the restored inventory.

Explicit `stage` preserves Undercity/Moon and other off-road destinations. Falling back
to `Stage.forFloor` is appropriate only when no valid explicit destination is present.
Sea diving additionally retains the departure world/map in memory so surfacing returns
to the same cloud room; this is not a mid-floor save.

## Dream Catcher

After the death animation, `wakeUp` takes the current build back to floor one with full
health and full guns, preserving score/coins/items except Dream Catcher itself. A dream
restarts its own kind from the beginning; a daily retains its original seed. The player
gets the `IT WAS ALL A BAD DREAM` message. This is a revival transition, not continuing
a disk save, and the effect is excluded from endless pools.

Permanent records/unlocks are separate preferences described in [progression](progression.md).
Use isolated dev preferences when testing save migration or reset; see [debugging](debugging.md).

## Implementation

- [RunSave](../src/main/java/com/trush/game/save/RunSave.java)
- [GameScreen](../src/main/java/com/trush/game/screen/GameScreen.java)
- [HubScreen](../src/main/java/com/trush/game/screen/HubScreen.java)
- [Weapon](../src/main/java/com/trush/game/objects/Weapon.java)
- [Item](../src/main/java/com/trush/game/objects/Item.java)
- [ETGFanfic](../src/main/java/com/trush/game/launcher/ETGFanfic.java)

## Existing checks

- [RunSaveTest](../src/test/java/com/trush/game/save/RunSaveTest.java)
- [SynergySaveTest](../src/test/java/com/trush/game/save/SynergySaveTest.java)
- [DreamCatcherTest](../src/test/java/com/trush/game/save/DreamCatcherTest.java)
- [RainbowRayHardModeTest](../src/test/java/com/trush/game/save/RainbowRayHardModeTest.java)
