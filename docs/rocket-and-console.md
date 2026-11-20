# Family rocket, Moon ride and console game

[Documentation index](README.md)

## Campaign rocket

Each of the four builders who defeats the Fluffy King contributes a rocket piece at
home. Once complete, a rocket lands beside the cloud rainbow after the King. The pilot
is Grandad, or Granny when Grandad is playing (`Rocket.pilotFor`). Interacting asks
whether to go higher; boarding marks the kingdom win and `boardedRocket`, then the
stage-clear menu offers Blast Off. See [progression](progression.md).

`RocketRideScreen` is a 30-second interlude before Moon: steer around asteroids and shoot
while held. Bumps do not cost health and there is nothing to collect. Esc skips to the
next screen. The Moon start room parks the rocket and pilot. This screen is distinct
from the console game's independent health, score and rewards.

## Home console

After boarding, a console appears by the TV. `RocketGameScreen` is Egor's idea and
credits `A GAME BY EGOR` on title/game over and in the console prompt. It is an endless
score chase with its own state, not a `GameWorld`/director run. It reuses real enemy/boss
portraits but implements its own attacks. Best score is `rocketBest`, shown on the shelf.

The rocket starts with three hearts. Only the glowing porthole (radius 0.075) is the hitbox;
pickups use the whole rocket. One blank starts available, up to three; presents grant a
heart/blank when useful or points when full. A blank clears bullets and interrupts aim.
Ordinary stars permanently improve the current game's shot level up to three. Bosses
drop golden stars that extend upgrades to five: extra wide fin shots, then faster fire.
A flawless boss adds a heart up to six.

## Population schedule

| Time | Addition |
| --- | --- |
| Start | Living meteors; large ones split |
| 8 s | Aliens beam in and fan orbs |
| 20 s | Bunny pairs hop down and land carrot rings |
| 35 s | Astronauts cross with laser bursts |
| 60 s | First boss, then every 100 s |
| 75 s | A UFO at a time: rotating rings or pulling beam, leaves after four attacks |
| 90 s | Increasing veteran share: more dangerous variants with medals |

The `busy` ramp increases pressure over four minutes. Iron meteors burst shards,
veteran bunnies add rings, astronauts fire five leading shots, and aliens fill fan gaps.
Veterans have greater drop chance. UFO pull is slower than rocket movement so steering
can resist it.

Bosses come from a shuffled bag, every eligible boss once before repeats, avoiding the
last boss when refilling. Choices are Fluffy King, Night Hawk, Messenger, Hollow Witch,
and Grandfluff after a true ending. Entry clears other enemies/bullets; no ordinary
spawns occur until the boss dies. Bosses sway in and ease into position rather than snap.
Attacks adapt rings/fans/spirals, missiles/sweeps, triangles/blades/rain, hex/lanterns or
comets/constellations to this screen. This is not the campaign boss state machine.

Fire is Space/J/left mouse or RB/RT/A. Console blanks use Q/right mouse/X/right-stick
click. Esc/Start leaves console play through its screen flow; see the local title/game-over
menus. Debugging can force an early boss or show the home console without earning it.

## Implementation

- [Rocket](../src/main/java/com/trush/game/objects/Rocket.java)
- [RocketRideScreen](../src/main/java/com/trush/game/screen/RocketRideScreen.java)
- [RocketGameScreen](../src/main/java/com/trush/game/screen/RocketGameScreen.java)
- [HubScreen](../src/main/java/com/trush/game/screen/HubScreen.java)
- [GameScreen](../src/main/java/com/trush/game/screen/GameScreen.java)
- [Unlocks](../src/main/java/com/trush/game/save/Unlocks.java)

## Existing checks

- [RocketTest](../src/test/java/com/trush/game/save/RocketTest.java)
