# Combat, projectiles and damage

[Documentation index](README.md)

## Firing and hits

`Weapon.tryFire` builds the attack using gun stats, upgrades, player modifiers and active
synergies. `GameWorld` owns projectile movement and collision, with special paths for
beams, melee, flashes, shouts, lobbed explosives and meteors. `Bullet` stores motion and
behavior flags; its drawing shape does not define its collision geometry.

Projectile hits are circle-based. The player's belly hitbox is smaller than the visible
body; enemy subclasses can override `bodyContains` for large or multipart targets.
Fast player shots use path checks so a target between frames can still be hit.
Piercing and returning shots track prior hits; rebound/boomerang behavior controls when
another hit is allowed. Cover and beams share directional obstruction rules; see
[rooms and cover](rooms-and-cover.md).

Damage to an enemy goes through `GameWorld.damageEnemy`, including damage from items,
companions and hazards owned by the player. It drives Bubble Shield charge, hit feedback,
kill accounting, Flufficon, weapon growth and drops. Enemy invulnerability/dodge states
must be respected by projectile, beam and explosion paths alike.

## Blanks and interruptions

A blank clears enemy bullets, blows away gas and interrupts enemy aim. Bubble Wand uses
the same blast without spending a blank. Every cleared projectile is marked `blanked`:
an enemy tracking its own rocket, hairball or bomb must check this flag before bursting it.
Testing the visual blank ring's timer is insufficient because enemy daze lasts longer.
The [Small Blanks](dreams.md) rule limits the blast and its effects to a visible
two-unit radius around the hero.

Do not remove bullets directly during an update traversal: mark `alive = false`, then
let the world clean them up. Use `spawnEnemy` for new enemies, including boss helpers.

Megaphone clears a cone, deals damage and knockback, stuns ordinary enemies and slows
their next volley. Bosses can be hushed but are not stunned. Frying Pan and Mirror Moon
return enemy shots as player shots. Defensive orbitals can intercept shots before they
reach the player.

## Boss readability

Bullets spawned during a boss's behavior pass are restyled by `styleBossBullets`:
1.3 times visual size, 0.85 times speed in normal mode, full speed in hard mode.
`hitScale` preserves the original collision size so larger drawings do not close gaps.
Enemy bullets from ordinary actors keep their normal styling. Hard boss clocks and
Grandfluff's exception are described in [difficulty](difficulty.md).

## Death and room completion

Dead enemies leave the live list and animate in `GameWorld.dying`. Normal deaths topple
away from the hit and shrink; pit falls spin down; special enemies can vanish or shatter.
Bosses get a death show, slow motion, explosions and a final blast. Directors wait for
`bossDying()` to become false before awarding the room and opening the way out.
A Blue Blob family shares fight identity and health reporting across splits; popping
one member must not finish the whole fight or reset its flawless status.

## Implementation

- [Bullet](../src/main/java/com/trush/game/objects/Bullet.java)
- [Weapon](../src/main/java/com/trush/game/objects/Weapon.java)
- [GameWorld](../src/main/java/com/trush/game/world/GameWorld.java)
- [Enemy](../src/main/java/com/trush/game/enemy/Enemy.java)
- [BlueBlob](../src/main/java/com/trush/game/enemy/BlueBlob.java)

## Existing checks

- [BananaCollisionTest](../src/test/java/com/trush/game/world/BananaCollisionTest.java)
- [GrenadeWallTest](../src/test/java/com/trush/game/world/GrenadeWallTest.java)
- [BananaHitboxTest](../src/test/java/com/trush/game/objects/BananaHitboxTest.java)
- [MegaphoneTest](../src/test/java/com/trush/game/objects/MegaphoneTest.java)
- [EnemyHitFeedbackTest](../src/test/java/com/trush/game/enemy/EnemyHitFeedbackTest.java)
