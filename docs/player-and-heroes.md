# Player movement, health and heroes

[Documentation index](README.md)

## Family

Health is stored in half-hearts. The table uses whole hearts; movement and roll multipliers
are relative to the boy. Starter weapons are excluded from chest pools.

| Hero | Hearts | Walk | Roll reach | Starter and special rules |
| --- | ---: | ---: | ---: | --- |
| Boy | 3 | 1 | 1 | Rusty Sidearm |
| Sister | 2 | 1.05 | 1.3 | Throwing Stars |
| Granny | 3 | 0.9 | 1 | Pearl Revolver; cat companion |
| Grandad | 3 | 1 | 0.72 | Old Double-Barrel, three shots per load (four with the magazine upgrade). Every acquired gun gets one upgrade, including his starter |
| Baby | 4 | 1 | 1.2 | Baby Rattle, Guardian Lamb, double somersault; easy mode |
| King | 3 | 1 | 1 | Royal Sceptre; floats over pits |

Grandad unlocks after a regular kingdom win. The King unlocks when all four rocket builders
have earned crowns. Baby wins are tracked separately and do not unlock Grandad or build
the rocket. Baby and King are excluded by `Hero.buildsRocket`; use `Hero.rocketCrew()`
instead of the number of enum values for rocket/crown requirements. `Hero.current` is the
hub selection; a saved trip restores its own hero. See [progression](progression.md).

## Walking and rolling

`Player.walk` resolves movement using `feetRect`, not the artwork. The small combat
hitbox is a circle at `hitboxY`, radius 0.1. Velocity is tracked after actual movement so
leading enemies do not aim ahead of a player who is pushing into a wall.

The base roll lasts 0.42 s with 0.32 s of invulnerability and a 0.18 s cooldown.
Hero reach, Bouncy Socks and Moon Boots alter its movement. Input buffering and recent
movement direction help rolls register during recovery and with a lightly pressed stick.
Rolling crosses pits and standing tables; each table is knocked once per roll, and the
roll must not finish on top of one. Walking against a table eventually pushes it, including
a flipped table; connected tables move together and can tip into pits.

## Health and defenses

A heart container increases maximum health by a whole heart, up to 16 hearts. The first
eight are red; extra containers and health are drawn as gold over them, and gold health
is lost first. Ordinary damage grants 1.2 s of invulnerability. A flawless boss grants
a heart container; other loot can grant them too.

`GameWorld.damagePlayer` coordinates equipment reactions. Bike Helmet intercepts a hit;
its refit counter includes the fight in which it broke and sits out the next two fights.
Bubble Shield recharges from damage dealt. A fatal hit consumes Packed Lunch before
Guardian Lamb; lunch leaves one heart and the lamb restores full health. Dream Catcher
acts later, after the death animation, restarting with the carried build. See
[items](items.md), [combat](combat.md), and [saving](saving.md).

Burning, acid exposure, frost, stun and petrification have separate rules; see
[status effects](status-effects.md). Falling returns the player to the last safe spot
for half a heart, unless the sea entrance redirects the fall. The King ignores ordinary
pits but can still dive through the sea hole with Flippers.

## Companions and dialogue

Granny's `Cat` follows her, pounces on a nearby visible enemy on cooldown, and may bring
a heart, blank, ammo or key after a fight. Little Cloud strikes enemies with lightning;
Toy Robot creates a temporary turret; Junk Knight grows from broken-chest junk.
Their updates live in `GameWorld`, rather than in the hero enum.

Grandad has a shuffled bag of 24 boss-defeat lines, with no repeat during a run. His
line starts one second after the boss camera returns to him and takes priority over
the Flufful's bubble. The Flufful has its own event-driven chatter; see [weapons](weapons.md).

## Implementation

- [Hero](../src/main/java/com/trush/game/player/Hero.java)
- [Player](../src/main/java/com/trush/game/player/Player.java)
- [Crown](../src/main/java/com/trush/game/player/Crown.java)
- [Cat](../src/main/java/com/trush/game/objects/Cat.java)
- [LittleCloud](../src/main/java/com/trush/game/objects/LittleCloud.java)
- [JunkKnight](../src/main/java/com/trush/game/objects/JunkKnight.java)
- [GrandadVoice](../src/main/java/com/trush/game/objects/GrandadVoice.java)

## Existing checks

- [FamilyTest](../src/test/java/com/trush/game/player/FamilyTest.java)
- [BabyBrotherTest](../src/test/java/com/trush/game/player/BabyBrotherTest.java)
- [HeartContainerTest](../src/test/java/com/trush/game/player/HeartContainerTest.java)
- [GrandadVoiceTest](../src/test/java/com/trush/game/objects/GrandadVoiceTest.java)
