# Controls and interaction

[Documentation index](README.md)

## During a run

| Action | Keyboard / mouse | Xbox layout |
| --- | --- | --- |
| Move | WASD / arrows | Left stick |
| Aim | Mouse | Right stick |
| Shoot | Hold left mouse | Hold RB |
| Roll | Space / left Shift / right mouse | LB |
| Interact | E | A |
| Reload | R | X |
| Blank | Q | Right-stick click |
| Active item | F (hold/release for grenade) | RT |
| Swap active slot | G | D-pad left / right |
| Select gun | Number keys, Tab, mouse wheel | Hold Y for gun wheel |
| Big map | Hold M | Hold LT |
| Pause | Esc / P | Start |

Screens interpret controls in context. A boss card takes any key/click or A/B as skip;
Esc skips the card instead of opening pause. The big map pauses the world and lets the
player select a visited room with directions/stick/mouse and teleport with Enter/E/A
or click. It is unavailable during a fight. See [interface](screens-and-interface.md).

`Gamepad` polls gdx-controllers/Jamepad once per frame. It maps Xbox buttons, applies a
0.25 stick dead zone and rescales the remaining movement range. Raw left-stick input
is retained for a gentle directional roll. Triggers count as pressed past halfway.
Recent pad or mouse use selects aim behavior and control hints. Native-controller
failure falls back to keyboard/mouse. `Gamepad.drive` supplies the same interface to the bot.

## World interactions

`GameWorld.interact` resolves nearby actions in priority order: chests, shop stands,
shop counter, exit, then tables, with special room interactions handled by the world.
Buying passes the purchase through the ordinary pickup collection path. An exit is usable
only after it opens; secret stairs and the rocket have their own prompts and prerequisites.

An active item dropped when replacing it uses `Pickup.waitForLeave` to prevent immediately
collecting it again. The backpack's second active slot keeps charging while bagged.

## Other screens

`MenuList` shares mouse, keyboard and pad navigation. The Flufficon turns pages with Q/E
or LB/RB. Rocket Ride and the console have their own controls: Space/J/left mouse and
RB/RT/A fire; the console uses Q/right mouse/X/right-stick click for a blank. See
[rocket and console](rocket-and-console.md).

## Implementation

- [Gamepad](../src/main/java/com/trush/game/input/Gamepad.java)
- [Player](../src/main/java/com/trush/game/player/Player.java)
- [MenuList](../src/main/java/com/trush/game/screen/MenuList.java)
- [GameScreen](../src/main/java/com/trush/game/screen/GameScreen.java)
- [GameWorld](../src/main/java/com/trush/game/world/GameWorld.java)
