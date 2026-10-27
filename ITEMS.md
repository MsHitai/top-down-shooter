# Items

Every item in the game, sorted by tier (the chest it comes out of: D brown, C blue, B green, A red).
A chest holds either a gun or an item of its tier; each item turns up at most once per run.
Numbers come from `objects/Item.java`, `player/Player.java` and `world/GameWorld.java` - update this file when an item changes.

- **Passive** - works as soon as you pick it up.
- **Active** - you carry one at a time and use it with RT / F; picking up another drops the old one.
  Single use, or it recharges after the cooldown.

## How many in each tier

| Tier | Items | Passive | Active |
|------|------:|--------:|-------:|
| D | 6 | 4 | 2 |
| C | 8 | 7 | 1 |
| B | 7 | 5 | 2 |
| A | 5 | 4 | 1 |
| Shop only | 1 | 1 | 0 |
| **Total** | **27** | **21** | **6** |

## Tier D - 6 items

| Item | Type | What it does |
|------|------|--------------|
| Fluffy Slippers | Passive | Walk 20% faster |
| Spare Pocket | Passive | +1 blank every floor |
| Med Kit | Active, single use | +2 hearts (can't be used at full health) |
| Bouncy Socks | Passive | Dodge rolls go 30% further and faster |
| Cardboard Hero | Active, 15 s recharge | Puts down a decoy for 6 s: regular enemies shoot at it and their bullets break on it (bosses aren't fooled) |
| Acid Boots | Passive | Walk through acid unharmed (not stink gas) |

## Tier C - 8 items

| Item | Type | What it does |
|------|------|--------------|
| Fluff Field | Passive | Pushes regular enemies back out of a 1.1-unit circle around you |
| Bigger Clips | Passive | +50% magazine size on every gun |
| Fluffy Heart | Passive | +1 max heart (once) |
| Treasure Map | Passive | Shows the whole floor on the map (story mode only) |
| Toy Robot | Active, 16 s recharge | Puts down a turret that fights for you for 8 s |
| Bike Helmet | Passive | Takes one hit for you; back on after each fight |
| Frost Rounds | Passive | 20% of your bullets freeze enemies |
| Ember Rounds | Passive | 20% of your bullets set enemies on fire |

## Tier B - 7 items

| Item | Type | What it does |
|------|------|--------------|
| Hot Sauce | Passive | +25% damage |
| Lucky Clover | Passive | Better chests and more drops |
| Static Fuzz | Passive | Zaps enemies within 1.25 units for 3 damage every 0.5 s |
| Grenade | Active, 7 s recharge | Hold to throw further (1 to 4.5 units), 40 damage blast |
| Prickly Sweater | Passive | Getting hit sprays quills (7 damage each) all around |
| Snow Globe | Active, 20 s recharge | For 5 s every enemy is chilled and enemy bullets fly at 40% speed |
| Pom-Pom Moons | Passive | Three orbiting pom-poms block enemy bullets and bump enemies (4 damage) |

## Tier A - 5 items

| Item | Type | What it does |
|------|------|--------------|
| Bubble Shield | Passive | Blocks a hit, recharges after you deal 250 damage |
| Ghost Bullets | Passive | Your bullets fly through enemies |
| Fluffy Candy | Active, single use | Full health |
| Little Cloud | Passive | A cloud friend that strikes the nearest enemy with lightning (9 damage every 1.3 s) |
| Guardian Lamb | Passive | Saves you once from a fatal blow and restores full health |

## Shop only - 1 item

<details>
<summary>Spoiler</summary>

| Item | Tier | Type | What it does |
|------|------|------|--------------|
| Flippers | B | Passive | Sold only by Baa-llo in the city (65 coins). Each roll ends in 3 homing bubbles (3 damage, 1.2 s cooldown) - and they let you dive into the hidden sea |

</details>
