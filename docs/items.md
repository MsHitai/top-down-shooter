# Items, active slots and companions

[Documentation index](README.md)

The [item reference](item-reference.md) lists every passive, active, tier, cooldown and effect.
`Item` is the inventory enum; `Player` stores passives, the equipped active and an optional
bagged active. Item effects are applied where the mechanic runs, rather than by a single
generic item update.

## Acquisition and active slots

Chest and shop items are drawn from director pools, normally once per run. `applyOnPickup`
handles one-time effects such as Fluffy Heart's container and Key Ring's initial two keys.
Do not reapply those grants on save restoration. `Item.byName` retains compatibility for
old names, including `MAGNET` mapping to Fluff Field.

There is one active slot, or two with Fluffy Backpack. F/RT uses the selected item;
G/d-pad swaps slots. The bagged cooldown continues ticking. Consuming a single-use item
brings out the bagged one. Replacing an item drops the previous one using `waitForLeave`.
A bagged Packed Lunch can still prevent a fatal hit. Selling a full backpack drops its
extra active item at the player's feet.

Grenade is rechargeable and charges throwing distance while held (1 to 4.5 world units
over 0.8 s). Release throws it; the same landing-marker UI is shared with Egg Lobber.
Med Kit, Fluffy Candy and Packed Lunch are consumed; the turret, decoy, globe, wand,
invulnerability plush and Sugar Rush use their own cooldown/effect timers.

## Effect ownership

| Effect | Code to inspect |
| --- | --- |
| Walking, roll, damage, reload, spread | `Player.moveSpeed`, `damageScale`, `reloadScale`, `spreadScale` and movement |
| Magazine / ammo capacities | `Weapon.boostMagazine`, `doubleAmmo`, acquisition and restoration paths |
| Freeze, burn, bounce, homing and critical bullets | `GameWorld.addPlayerBullet` |
| Fluff Field / Static Fuzz | `GameWorld.updateAuras`: shove and damage have different radii |
| Orbitals and bullet interception | `GameWorld.updateMoons` / `drawMoons` |
| Little Cloud, fire trail, roll-clearing blanket, map reveal | `GameWorld.updateGadgets` |
| Reload bullet clearing | `Weapon.startReload` → `GameWorld.onReload` |
| Shield recharge | `GameWorld.damageEnemy` → `Player.chargeShield` |
| Helmet, quills, teddy, Blue Moon | Damage handling in `GameWorld` and `Player` |
| Lunch / lamb / Dream Catcher | Fatal damage, then post-death restart; see [saving](saving.md) |
| Floor grants | `RunSave.startFloor`, plus explicit secret-transition paths |

Cardboard Hero creates a decoy that changes ordinary enemies' targeting and absorbs their
bullets; bosses ignore it. Toy Robot creates a temporary `Turret`; Little Cloud is an
ongoing lightning companion. Granny's cat belongs to her hero, while Junk Knight awakens
from living junk; see [heroes](player-and-heroes.md) and [loot](loot-and-chests.md).

Treasure Map and Dream Catcher are excluded from endless by `Item.shuffledPool(false)`.
Flippers are shop-only. S items come from black chests in story and late reward ordering
in endless. Save enum names, update icon generation and reference tables when adding an
item, and check its interaction with selling, bagged slots and restoring an existing run.

## Implementation

- [Item](../src/main/java/com/trush/game/objects/Item.java)
- [Player](../src/main/java/com/trush/game/player/Player.java)
- [GameWorld](../src/main/java/com/trush/game/world/GameWorld.java)
- [Decoy](../src/main/java/com/trush/game/objects/Decoy.java)
- [Turret](../src/main/java/com/trush/game/objects/Turret.java)
- [LittleCloud](../src/main/java/com/trush/game/objects/LittleCloud.java)
- [Grenade](../src/main/java/com/trush/game/objects/Grenade.java)

## Existing checks

- [ItemsTest](../src/test/java/com/trush/game/player/ItemsTest.java)
- [MoreItemsTest](../src/test/java/com/trush/game/player/MoreItemsTest.java)
- [NewItemsTest](../src/test/java/com/trush/game/player/NewItemsTest.java)
- [GrenadeThrowTest](../src/test/java/com/trush/game/player/GrenadeThrowTest.java)
- [AuraItemsTest](../src/test/java/com/trush/game/objects/AuraItemsTest.java)
- [DreamCatcherTest](../src/test/java/com/trush/game/save/DreamCatcherTest.java)
