# Coins, Baa-llo and selling loot

[Documentation index](README.md)

## Coin rewards

`Coins` keeps reward arithmetic separate from spawning and collection. Coins are pickups;
big coins are worth five, and the purse caps at 999. After combat and a short calm delay,
loose coins fly to the hero. Piggy Bank can double a collected coin and Gold Rush doubles
coin rewards. The HUD shows money beside keys.

| Source | Amount before modifiers |
| --- | --- |
| Regular enemy | Average `score / 20`, fractional remainder rolled |
| Cleared room | `3 + random(0..2) + depth / 2` |
| Boss | `20 + 5 * level` |
| Endless wave | `2 + wave / 3` |
| Chest with consumables | `3 + 2 * tier.ordinal() + extra` |

Boss helpers do not generate normal enemy coins while the boss is alive. Raccoons can
steal coins and repay them when killed; see [Undercity](undercity.md).

## Buying

Baa-llo's shop stocks two equipment choices plus heart, blank-or-ammo and key. One regular
slot is on sale. Equipment comes from director pools, with nearest-tier fallback if empty;
the first floor's first equipment offer is a D gun. Story shops initialize on first entry.
Under the Bed uses standard first-floor stock and prices. The sea and the Moon
have no regular shop.

| Goods | Base price |
| --- | ---: |
| D / C / B / A equipment | 25 / 45 / 70 / 100 |
| Heart / blank | 15 |
| Ammo | 20 |
| Key | 30 |

`Shop.price` adds 10% per level above the initial level, applies a 25% sale reduction,
and a 50% grudge markup. S equipment is not stocked. Buying from a stand checks whether
the pickup can be used (for example, no healing at full health), spends money, then goes
through normal collection. Shop furniture allows shots over the counter. Three hits on
the keeper cause a grudge that persists across the run and is stored in `RunSave`.

## Special stock

The city shop adds Flippers on their own stand until bought: fixed 65 coins, no floor
markup or sale, but grudge still applies. If the player lacks a dousing weapon or Frost
Rounds, its first gun offer supplies water/ice access for the Slums entrance.

After the Flufful is unlocked, Baby's first-floor shops and first endless stall have a
45-coin Flufful stand. Boss rush shops follow every second boss, excluding sea; they use
level-one pricing, guarantee a gun, and include an ammo stand. Moon rush uses the cloud shop.

## Selling and endless stalls

Interacting at the counter opens a grid of item icons and prices. Gear lists carried guns
and items, Supplies lists keys, blanks and junk, and Floor Loot lists loose pickups in
visited rooms on the current floor. Use Q/E or the shoulder buttons to switch tabs, and
the mouse wheel, Page Up/Down or the triggers to turn pages. Pick an item and confirm
the sale. Every hero keeps their starting gun. Selling a full backpack drops its extra
active item.
`sellPrice` pays half the base price, or a quarter with a grudge. Sale/floor purchase
markups do not turn into a higher resale value.

Endless opens a travelling stall after each boss; furniture in its way is cleared.
The next wave waits for the counter bell. Choose Ring Bell in the counter menu or press
B on the keyboard or Y on the controller, then confirm to start the next wave.
See [endless](endless.md) and [saving](saving.md).

## Implementation

- [Coins](../src/main/java/com/trush/game/objects/Coins.java)
- [Shop](../src/main/java/com/trush/game/objects/Shop.java)
- [ShopMenu](../src/main/java/com/trush/game/screen/ShopMenu.java)
- [GameWorld](../src/main/java/com/trush/game/world/GameWorld.java)
- [StoryDirector](../src/main/java/com/trush/game/world/StoryDirector.java)
- [WaveDirector](../src/main/java/com/trush/game/world/WaveDirector.java)

## Existing checks

- [CoinsTest](../src/test/java/com/trush/game/objects/CoinsTest.java)
- [ShopTest](../src/test/java/com/trush/game/objects/ShopTest.java)
- [FloorLootSaleTest](../src/test/java/com/trush/game/world/FloorLootSaleTest.java)
- [RushShopTest](../src/test/java/com/trush/game/world/RushShopTest.java)
