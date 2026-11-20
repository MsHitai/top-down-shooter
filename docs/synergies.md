# Inventory synergies

[Documentation index](README.md)

Synergies are derived from the current inventory and hero. `Synergy.active` checks the
recipe and `affects` limits the relevant guns. `Player.synergyFor` exposes the match;
`Weapon.tryFire` and `GameWorld` apply it to fresh shots or combat effects. A synergy does
not permanently rewrite saved gun stats or spend extra ammunition.

| Name | Recipe | Effect |
| --- | --- | --- |
| Icy Water | Water Pistol + Freeze Ray | Water shots build frost; ray frost accumulates 50% faster |
| Banana Split | Banana-rang + Bouncy Socks | Two full-strength bananas for one ammo |
| Big Fish | Chomper + Flippers | Sharks 50% bigger, +35% damage and stronger knockback |
| Cat Lady | Kitty Cannon + Granny | Firing calls her cat to pounce; cat bites twice as hard while held |
| Extra Hot | Flare Gun + Hot Sauce | Flares +25% damage, longer burn, wider fire pools |
| Live Wire | Jumper Cables + Static Fuzz | Two extra chain targets and +20% spark damage |

The held gun has a colored glow/sparks. The Flufficon records discovered recipes under
`dexSynergies`. Loading a run recomputes bonuses from restored inventory; it must not add
the bonuses twice. Selling or swapping equipment can deactivate a recipe immediately.

To add a recipe, define it in `Synergy`, implement any special world/weapon behavior,
add its display and reference entry, and check both acquisition orders and save/load.

## Implementation

- [Synergy](../src/main/java/com/trush/game/objects/Synergy.java)
- [Weapon](../src/main/java/com/trush/game/objects/Weapon.java)
- [Player](../src/main/java/com/trush/game/player/Player.java)
- [GameWorld](../src/main/java/com/trush/game/world/GameWorld.java)
- [Flufficon](../src/main/java/com/trush/game/save/Flufficon.java)

## Existing checks

- [SynergyTest](../src/test/java/com/trush/game/objects/SynergyTest.java)
- [SynergySaveTest](../src/test/java/com/trush/game/save/SynergySaveTest.java)
