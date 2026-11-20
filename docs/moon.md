# Moon, veterans and Grandfluff

[Documentation index](README.md)

The completed family rocket lands after the Fluffy King. Boarding counts the kingdom
win, unlocks the home console, and starts the Rocket Ride before an explicit Moon stage.
The ordinary route calls it floor 6; an explicit stage is necessary when continuing a
saved trip or arriving after a longer detour. See [rocket](rocket-and-console.md).

## Environment

Moon has no ordinary shop. Themes are landing, craters, cheesemine, observatory,
cheesecave and castle. `drawSpace` draws stars and Earth beyond open room edges; furniture
uses boulders, unbreakable crystals, cheese and supplies. Doors use force fields, pits
are craters, and `Exit.ROCKET` offers `FLY HOME`. The start room parks the family's rocket
and pilot. During combat in ordinary Moon rooms, meteor showers mark the hero's spot
and additional locations before blasts that can hurt both hero and enemies.

## Population

`MoonFolk` supplies kind IDs, costs, creation and veteran variants. Moon Bunny pairs
(or larger groups at difficulty) hop and land in carrot rings. Living Asteroids drift,
fire spirals and split into smaller aimed-shard asteroids. Astronauts keep distance,
jet sideways and fire laser bursts. Aliens teleport into clear firing positions and
fan green orbs. UFO sways above the player, choosing rotating rings or a tractor beam
that pulls except during rolls and then bursts at its shadow.

Moon Golem and Cheese Worm are heavy encounters with cost four and a one-per-room group
limit. Golem warns slams/rings or throws a bursting boulder. Worm burrows untouchably,
warns its eruption, then throws cheese rings/fans before diving. Its flying flag serves
burrowing collision rules, not aerial render ordering.

Veterans appear from depth 2 (one sooner hard), about half eligible rolls. Veteran bunnies
add an offset landing ring; iron asteroids use four-arm spirals and fan-firing pieces;
astronauts fire longer leading bursts; aliens follow a fan into its gaps. Golem, Worm and
UFO have no veteran variant. Each veteran has its own dex ID and visible medal.
The full Moon roster is not in ordinary endless; the console uses its own adaptations.

## Grandfluff's two lives and three acts

The boss bar has gold over red. Gold covers old and young Grandfluff; its last hit starts
the rise into the Moon at full red health, rather than killing the boss.

1. Old king: warned constellations, counter-turning spirals and stick swipes/fans.
2. At half gold: untouchable cheese-snack transition clears bullets, then young king
   hops with gap rings, comets and sparkle trails, and quicker spirals.
3. Gold exhausted: he rises into a giant face above the arena. The face tracks the hero
   horizontally and is broadly shootable. Crater rings, warned eye-beam sweeps, spirals,
   comets, constellations, giant warned hops and pulling breath replace the small-body fight.

Ambient aimed crater shots are suppressed during moves that otherwise create walls.
The young hard-mode act has distinct pace/bullet multipliers; see [difficulty](difficulty.md).
Beating Grandfluff produces `TrueEndingPicture` and records a true ending; the kingdom
win was counted on rocket boarding. Hard victory earns a crown for eligible heroes and
its achievement. See [progression](progression.md) and [bosses](bosses.md).

## Implementation

- [MoonFolk](../src/main/java/com/trush/game/enemy/MoonFolk.java)
- [MoonBunny](../src/main/java/com/trush/game/enemy/MoonBunny.java)
- [LivingAsteroid](../src/main/java/com/trush/game/enemy/LivingAsteroid.java)
- [Astronaut](../src/main/java/com/trush/game/enemy/Astronaut.java)
- [Alien](../src/main/java/com/trush/game/enemy/Alien.java)
- [Ufo](../src/main/java/com/trush/game/enemy/Ufo.java)
- [MoonGolem](../src/main/java/com/trush/game/enemy/MoonGolem.java)
- [CheeseWorm](../src/main/java/com/trush/game/enemy/CheeseWorm.java)
- [Grandfluff](../src/main/java/com/trush/game/enemy/Grandfluff.java)
- [Meteor](../src/main/java/com/trush/game/objects/Meteor.java)
- [TrueEndingPicture](../src/main/java/com/trush/game/screen/TrueEndingPicture.java)

## Existing checks

- [MoonTest](../src/test/java/com/trush/game/world/MoonTest.java)
- [MoonVeteransTest](../src/test/java/com/trush/game/world/MoonVeteransTest.java)
- [MoonFolkTest](../src/test/java/com/trush/game/enemy/MoonFolkTest.java)
- [GrandfluffTest](../src/test/java/com/trush/game/enemy/GrandfluffTest.java)
