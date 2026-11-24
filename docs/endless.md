# Endless waves, rewards and travelling shops

[Documentation index](README.md)

The home TV starts a `GameScreen` with `WaveDirector`. It uses one arena, not a branching
campaign, and leaves a saved story trip intact. `isFinished` always returns false.

## Wave loop

The state sequence is `INTRO → FIGHT → CLEARED → next wave`, with `SHOP` after every
fifth (boss) wave. Introduction lasts 2.2 s. Enemies trickle from a budget queue while
the active cap permits; pending enemies count, and the spawn gap is 0.4 s. Completion
waits for an empty queue, no active enemies and no boss death show.

Population grows with `2 + 2 * wave` budget. Wave selectors introduce new regions and
variants independently of story depth. Plain kin become veterans from wave 11; red
shotgunners and cops get tougher variants from wave 16. Restrictive enemies such as
hydrants, bikers and snipers have early population caps. Music advances through village,
town, city, tower and clouds at waves 6, 11, 16 and 21.

| Boss wave | Source |
| --- | --- |
| 5 | Village |
| 10 | Town |
| 15 | City |
| 20 | Skyscraper |
| 25 | `Boss.finalBoss` (King or Shadow King for playable King) |
| 30 | Sea stage selection if discovered, otherwise eligible random boss |
| Later | `Boss.pickAny` with its secret/unlock exclusions |

## Flooding and rewards

Once `Unlocks.foundSea`, waves 26–30 flood and use sea creatures. Furniture swaps into
sea kinds in the same positions and is restored when drained; the arena drains for
Baa-llo's stall. The sea's first boss remains Shark until beaten, then stage selection
can choose Kraken/Gorgon too. No discovery means those waves remain dry.

Gun rewards are shuffled within tier then ordered low-to-high; S rewards come last.
Waves 2 and 3 and boss waves grant guns while the pool lasts. Boss clears fully heal
the player, ensure at least two blanks, restock furniture, and grant an item, a heart
container and a spread-ammo box when needed. Story-only Treasure Map and
Dream Catcher are excluded. Repeated guns upgrade/refill through the normal inventory path.

Ammo is deliberately more available than raw story drops: `ammoShortage` measures only
the finite-ammo guns, increases ordinary drop chance up to fourfold, and forces the wave
reward to ammo when the measured guns are half empty. A box drops only if some gun needs
it. The one-box allowance resets for each wave.

## Stall and records

After a boss, Baa-llo opens a travelling shop and waits for the player to ring the counter
bell. Its menu also sells carried/floor loot. Baby's first stall can stock the unlocked
Flufful. Closing the stall begins the next intro and resets ammo-drop allowance.
Score and best wave go to permanent records; there is no between-floor endless save.
See [economy](economy-and-shops.md) and [difficulty](difficulty.md).

## Implementation

- [WaveDirector](../src/main/java/com/trush/game/world/WaveDirector.java)
- [GameWorld](../src/main/java/com/trush/game/world/GameWorld.java)
- [Boss](../src/main/java/com/trush/game/enemy/Boss.java)
- [Shop](../src/main/java/com/trush/game/objects/Shop.java)
- [GameScreen](../src/main/java/com/trush/game/screen/GameScreen.java)

## Existing checks

- [EndlessBossOrderTest](../src/test/java/com/trush/game/world/EndlessBossOrderTest.java)
- [ShopTest](../src/test/java/com/trush/game/objects/ShopTest.java)
- [WeaponBalanceTest](../src/test/java/com/trush/game/objects/WeaponBalanceTest.java)
