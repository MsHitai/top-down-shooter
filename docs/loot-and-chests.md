# Loot pools, chests, drops and junk

[Documentation index](README.md)

## Tiers and pools

| Tier | Chest color |
| --- | --- |
| D | Brown |
| C | Blue |
| B | Green |
| A | Red |
| S | Black |

`StoryDirector` owns shuffled gun/item pools. A chest reserves its reward when placed,
preventing two chests from promising the same equipment. When both kinds remain in its
tier, selection is normally half gun and half item. A two-treasure floor tries to split
its treasure rewards into one gun and one item. Exhausted pools fall back to consumables.
Shops draw from these same pools. Secret detours share the relevant campaign pools.

Treasure chests are locked, boss chests free, and occasional room-reward chests locked.
A key opens a locked chest; Bobby Pin spends one of its five uses first. One key per
floor is guaranteed within the first three cleared combat rooms; extra keys are rare drops.
Opening goes through `GameWorld.openChest`, collection through `collect`.

Starting at floor 3, a treasure chest or flawless boss chest can become black with
probability 1/12 while an S gun or item remains. Normal tier rolls and shop stock do not
select S. Endless orders S rewards late.

The [boss rematch](dreams.md#boss-rematches) entrance has one free, unbreakable chest
with a gun of its tier. The boss's medal caps the tier and Hard improves the odds.

## Story dare rewards

With a [Story dare](dreams.md#story-dares), treasure, boss and room-reward chests try
to move one tier up after the ordinary roll. The next tier must have eligible gear
remaining. Upgrades stop at red (A), and black (S) chest odds stay the same.
The same one-tier upgrade applies with two active dares, which also unlock a choice of [positive modifier](dreams.md#story-dares).

## Broken chests and Junk Knight

Unopened chests break after 24 player damage. Contents survive with 8% probability;
otherwise a break yields useful scraps or junk. Shooting is therefore a keyless gamble
rather than a normal way to claim the reserved equipment. Junk raises `Player.junk` and
is saved between floors.

Junk Knight only awakens when living junk is collected (1 in 15 junk rolls). Previous
junk already counts toward its level. Levels 1–6 walk up and swing a sword, adding armor,
shield, cape and gold; level 7 gains wings and shoots arrows. Store both junk count and
awakening flag: a pile of ordinary junk alone must not create the companion.

## Room, enemy and boss drops

Regular enemy deaths may drop hearts, blanks, keys or ammo in addition to coins.
Boss minions do not pay ordinary enemy coins while a boss is alive. Ammo only drops
when some finite-ammo gun needs it; normal per-kill chance is 4% (6% hard), with at
most one box per room. Endless adds shortage assistance and wave rewards.

A flawless boss is one whose fight never registered player damage. It grants a heart
container and improves eligibility for the black boss chest. Split bosses keep the same
fight/flawless identity. Rewards wait for the entire boss death show. Boss rush doubles
boss coins and supplies ammo with its own brown/green split; see [dreams](dreams.md).

Duplicate guns refill and upgrade rather than creating another inventory entry. Items
usually appear once per run. Companion gifts and special shop stands use their own paths.
See [weapons](weapons.md), [items](items.md), and [economy](economy-and-shops.md).

## Implementation

- [Chest](../src/main/java/com/trush/game/objects/Chest.java)
- [Tier](../src/main/java/com/trush/game/objects/Tier.java)
- [Pickup](../src/main/java/com/trush/game/objects/Pickup.java)
- [JunkKnight](../src/main/java/com/trush/game/objects/JunkKnight.java)
- [StoryDirector](../src/main/java/com/trush/game/world/StoryDirector.java)
- [GameWorld](../src/main/java/com/trush/game/world/GameWorld.java)

## Existing checks

- [BrokenChestTest](../src/test/java/com/trush/game/objects/BrokenChestTest.java)
- [BedChestTest](../src/test/java/com/trush/game/world/BedChestTest.java)
- [DuplicateGunTest](../src/test/java/com/trush/game/player/DuplicateGunTest.java)
- [HeirloomAndSTierTest](../src/test/java/com/trush/game/objects/HeirloomAndSTierTest.java)
