# Weapons, ammo and upgrades

[Documentation index](README.md)

The complete tier tables and recipes are in the [gun reference](gun-reference.md).
Guns are static factories in `Weapon`; their sprite names refer to `guns.png`/`guns.txt`.
Starters come from `Hero.starter`, chest availability from `Weapon.chestGuns`, and saved
names from `Weapon.byName`. New guns need all applicable registrations.

## Inventory and ammunition

The player carries multiple guns and one is selected. Firing spends its magazine and
finite ammo; reloading uses its reload time, modified by Quick Mittens and temporary buffs.
Ammo tables include the magazine. Infinite starters, Frying Pan and the Flufful do not
need finite-ammo drops. Reload/empty hints and the pad gun wheel are screen presentation.

Finding an owned gun calls `Player.addWeapon`: it refills and randomly upgrades the existing
gun, rather than adding a duplicate. Upgrades are +15% damage, +25% magazine/ammo, or
+15% bullet speed, displayed as `+N` and saved in their application order. Grandad grants
one upgrade to newly acquired guns and his starter.

Bigger Clips changes magazine capacity; Bottomless Bag changes ammo capacity. Fresh pickups
and restored guns must take different paths so a resume preserves remaining ammo rather
than refilling it. Rainbow Ray has separate normal/hard stats (480/900 total ammo and
2.8/2.9 damage); `setHardMode` is applied during reconstruction before restoring ammo.

## Attack families

| Family | Examples | Main implementation |
| --- | --- | --- |
| Ordinary, spread, burst, spin-up | Sidearm, Fluff Shotgun, Pea Shooter, Gull Gatling | `Weapon.tryFire`, delayed `Bullet`s |
| Beam | Laser Pointer, Freeze Ray, Unicorn Horn | `GameWorld.fireBeam`, cover-limited rays |
| Charged / lobbed | Slingshot, Egg Lobber | Player charge state, release-to-fire, `lobEgg`, landing marker |
| Returning / bouncing / guided | Banana-rang, Tennis Balls, Paper Planes | World steering, wall and enemy rebound paths |
| Splitting / impact blasts | Pillow Cannon, Popcorn, Party Rocket, Boom-buss | Split flags, feathers, `explode`, `smallBlast` |
| Close range / clearing | Frying Pan, Polaroid, Megaphone | `swingPan`, `cameraFlash`, `shout` |
| Persistent effects | Dust Bunny, Disco Ball, Pinwheel | Vortex, rotating beams, stop-and-split updates |
| Status / environmental | Water Pistol, Crayons, Gloop, Stinky Sock | Wet, burn, chill, acid, puddles and gas |
| Special targets | Chomper, Meteor Staff | Shark bite effects; mouse point or pad aim-line targeting |

Paper Planes use the aim direction, including on pad, choosing a target near the aim line
and steering around cover. Meteor Staff uses the mouse point or a target near the pad aim
line, with a fixed-distance fallback. Do not treat the pad's nearby aim point as a distant
world target. Chomper and Rubber Duck shots move faster underwater; wet/frost attacks can
douse the Slums entrance drum. See [status effects](status-effects.md).

Kazoo fires a straight middle note and two gently wobbling side notes in a narrow spread.
All three pierce. On a direct Pillow Cannon hit, the two forward feathers can hit the
impact target again while the rest fan out to hit other enemies.

Tennis Ball Launcher sends a rebound toward the nearest enemy it has yet to hit within
2.6 units of the bounce when the path is clear. With no such target, the ball bounces
away from the enemy. A wall bounce lets it hit the same enemies again. Wall and enemy
bounces share the same four-bounce limit.

Pinwheel locks onto a visible enemy near the aim direction within 5.5 units of the
muzzle. On controller, the right stick gives the direction. With a mouse, it favors the
enemy closest to the cursor within that cone. It follows the chosen enemy and spins in
place for a quarter second on contact. With no target, the shot slows to a stop about
2.5 units from the muzzle at base speed before bursting.

Pinwheel's eight petals and Royal Sceptre's six fragments home toward the hittable enemy
nearest the burst, including the original target. If that enemy dies, remaining fragments
seek the next closest one. Walls and cover can stop them.

## Growing guns

The Flufful unlocks permanently after the Fluffy Shark. It has infinite ammo and grows
at 20/60/120/200 kills made with it selected; its current kills are part of the run save.
`FluffulVoice` responds to world events and normally stays quiet during fights, announcing
a room clear or level-up afterward. It also comments on acquisition, switching, reloading
and idle moments, with quiet time between lines.

Heirloom gains progress while carried, regardless of which gun makes the kill. Grandad's
kills count double. Its levels use 40/120/260/480 kills; permanent progress is stored in
`Unlocks.heirloomKills`. Level changes apply stat ratios so existing upgrades survive.
Unlike the Flufful, it has finite ammo.

## Adding a weapon

Add a factory, tier and sprite; register the name and pool membership. Generate its art
through `tools/gen_sprites.py`. Route new damage through the world, preserve cover and
blank rules, verify save reconstruction, and update the stat reference. See
[synergies](synergies.md), [loot](loot-and-chests.md), and [assets](assets.md).

## Implementation

- [Weapon](../src/main/java/com/trush/game/objects/Weapon.java)
- [Bullet](../src/main/java/com/trush/game/objects/Bullet.java)
- [FluffulVoice](../src/main/java/com/trush/game/objects/FluffulVoice.java)
- [Player](../src/main/java/com/trush/game/player/Player.java)
- [GameWorld](../src/main/java/com/trush/game/world/GameWorld.java)
- [RunSave](../src/main/java/com/trush/game/save/RunSave.java)

## Existing checks

- [WeaponBalanceTest](../src/test/java/com/trush/game/objects/WeaponBalanceTest.java)
- [DuplicateGunTest](../src/test/java/com/trush/game/player/DuplicateGunTest.java)
- [ToyBoxGunsTest](../src/test/java/com/trush/game/objects/ToyBoxGunsTest.java)
- [FluffulTest](../src/test/java/com/trush/game/objects/FluffulTest.java)
- [HeirloomAndSTierTest](../src/test/java/com/trush/game/objects/HeirloomAndSTierTest.java)
- [RainbowRayHardModeTest](../src/test/java/com/trush/game/save/RainbowRayHardModeTest.java)
