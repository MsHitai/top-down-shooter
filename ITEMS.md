# Items

Every item in the game, sorted by tier (the chest it comes out of: D brown, C blue, B green, A red, S black).
A chest holds either a gun or an item of its tier; each item turns up at most once per run.
Numbers come from `objects/Item.java`, `player/Player.java` and `world/GameWorld.java` - update this file when an item changes.

- **Passive** - works as soon as you pick it up.
- **Active** - you carry one at a time and use it with RT / F; picking up another drops the old one
  (the Fluffy Backpack holds a second one: swap with the d-pad or G).
  Single use, or it recharges after the cooldown.

## How many in each tier

| Tier | Items | Passive | Active |
|------|------:|--------:|-------:|
| D | 10 | 7 | 3 |
| C | 11 | 9 | 2 |
| B | 10 | 7 | 3 |
| A | 10 | 8 | 2 |
| S | 2 | 2 | 0 |
| Shop only | 1 | 1 | 0 |
| **Total** | **44** | **34** | **10** |

## Tier D - 10 items

| Item | Type | What it does |
|------|------|--------------|
| Fluffy Slippers | Passive | Walk 20% faster |
| Spare Pocket | Passive | +1 blank every floor |
| Med Kit | Active, single use | +2 hearts (can't be used at full health) |
| Bouncy Socks | Passive | Dodge rolls go 30% further and faster |
| Cardboard Hero | Active, 15 s recharge | Puts down a decoy for 6 s: regular enemies shoot at it and their bullets break on it (bosses aren't fooled) |
| Acid Boots | Passive | Walk through acid unharmed (not stink gas) |
| Quick Mittens | Passive | Reloads take 35% less time |
| Packed Lunch | Active, single use | +1 heart; a fatal hit while you carry it eats it instead and leaves you on 1 heart (before the Guardian Lamb) |
| Piggy Bank | Passive | 30% of coins you pick up count double |
| Toy Telescope | Passive | Single shots spread only 35% as much (shotgun cones stay as they are) |

## Tier C - 11 items

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
| Rubber Rounds | Passive | Plain bullets bounce off a wall once (not rockets, bombs, boomerangs, darts...) |
| Star Plushie | Active, 25 s recharge | A big hug: nothing can hurt you for 3 s |
| Fluffy Backpack | Passive | Holds a second active item: d-pad left / right or G swaps them; the bagged one keeps recharging, and a single-use one used up brings the bagged one out (a Packed Lunch in the bag still saves you) |

## Tier B - 10 items

| Item | Type | What it does |
|------|------|--------------|
| Hot Sauce | Passive | +25% damage |
| Lucky Clover | Passive | Better chests and more drops |
| Static Fuzz | Passive | Zaps enemies within 1.25 units for 3 damage every 0.5 s |
| Grenade | Active, 7 s recharge | Hold to throw further (1 to 4.5 units), 40 damage blast |
| Prickly Sweater | Passive | Getting hit sprays quills (7 damage each) all around |
| Snow Globe | Active, 20 s recharge | For 5 s every enemy is chilled and enemy bullets fly at 40% speed |
| Pom-Pom Moons | Passive | Three orbiting pom-poms block enemy bullets and bump enemies (4 damage) |
| Puppy Rounds | Passive | Plain bullets turn towards enemies (140 degrees a second) |
| Bubble Wand | Active, 30 s recharge | A free blank |
| Sweet Tooth | Passive | Every 20 kills heal half a heart (kept until you're hurt) |

## Tier A - 11 items

| Item | Type | What it does |
|------|------|--------------|
| Bubble Shield | Passive | Blocks a hit, recharges after you deal 250 damage |
| Ghost Bullets | Passive | Your bullets fly through enemies |
| Fluffy Candy | Active, single use | Full health |
| Little Cloud | Passive | A cloud friend that strikes the nearest enemy with lightning (9 damage every 1.3 s) |
| Guardian Lamb | Passive | Saves you once from a fatal blow and restores full health |
| Fizzy Soda | Passive | Starting a reload clears enemy bullets within 1.6 units |
| Gold Star Rounds | Passive | +1% damage for every 3 coins you carry, up to +40% |
| Comet Skates | Passive | Dodge rolls leave a trail of flames that only burn enemies (not under water) |
| Sugar Rush | Active, 30 s recharge | For 6 s: +50% damage, walk 25% faster |
| Bobby Pin | Passive | Opens locked chests without a key |
| Dream Catcher | Passive (story only) | Like ETG's Clone: if you fall, it was all a bad dream - back to floor 1 with every gun and item, full hearts, score and coins (used up) |

## Tier S - 2 items

Only in black chests (`StoryDirector.blackChest`, from floor 3), never in the shop; endless hands them out last.

| Item | Type | What it does |
|------|------|--------------|
| Triple Scoop | Passive | Every bullet comes with two more beside it (11 degrees out) at half damage |
| Family Photo | Passive | +6% damage for every gun you carry beyond the first, up to +60% |

## Shop only - 1 item

<details>
<summary>Spoiler</summary>

| Item | Tier | Type | What it does |
|------|------|------|--------------|
| Flippers | B | Passive | Sold only by Baa-llo in the city (65 coins). Each roll ends in 3 homing bubbles (3 damage, 1.2 s cooldown) - and they let you dive into the hidden sea |

</details>
