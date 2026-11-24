# Fire, acid, gas, frost and control effects

[Documentation index](README.md)

## Fire and acid

Like ETG's goop and status effects. `objects/Puddle` is a patch of FIRE or ACID on the floor
(`GameWorld.spillPuddle`, cleared on leaving the room, with a limit of 48): a dynamite leaves flames where it
blew up, the Flare Gun where its flare stops, the Gloop Launcher and the Sludger's globs leave acid.
Walking into enemy fire sets the hero alight and displays `Draw.burningFlame` on the hero.
`Player.updateHazards` deals the first burn hit after `BURN_DELAY` (1.6 s), then every 1.5 s while burning, standing in acid bites after `ACID_DELAY` (not with the Acid Boots); a
dodge roll is in the air - it passes over both and puts the flames out. The player's own puddles only
hurt enemies. Enemies: `Enemy.ignite` / `poison` (damage over time, dealt by `GameWorld.updatePuddles`
through `damageEnemy`) and `freeze` (no `behave`, no contact damage; fire melts it; bosses only get
chilled); walking enemies catch fire / get eaten by acid in any puddle (`Enemy.acidProof`: the
Sludger, the Acid Sprayer, the Barrel Roller). Nothing burns in the sea or a flooded endless arena (`GameWorld.canBurn`). The Sludger (a
hazmat goon lobbing acid globs) is rare in the town, city and skyscraper (`StoryDirector.SLUDGER_CHANCE`)
and in endless from wave 7; its real home is the Slums, with the rest of its gang (see [Slums](slums.md)).

## Freezing and ice

Enemies: `Enemy.addFrost` builds ice up (the Snowball Slinger's `Weapon.frost` 0.2 a snowball, the Freeze
Ray's beam 0.05 a tick through `GameWorld.fireBeam`); at 1 it's `freeze`d for `Enemy.BUILT_FREEZE`, and the
ice creeps up from its feet meanwhile; it thaws when left alone, fire clears it; bosses are only chilled.
More frost on a frozen enemy keeps it frozen; the ice rattles and cracks for its last `THAW_WARN`, and once
out it shakes itself off for `THAW_GRACE` and stays chilled a moment (no instant shot on thawing).
Frost Rounds still freeze at once. A frozen enemy is brittle: hits deal `GameWorld.FROZEN_DAMAGE` x1.5,
a dodge roll into one (once a roll, `Player.getRammed`) hits it for `GameWorld.ROLL_SMASH` and sends it
sliding, and one killed frozen shatters (`Enemy.shatter`, `GameWorld.shatterIce`): no death show, a ring of
`SHATTER_SHARDS` ice shards (player bullets - they can shatter the next frozen one) and a patch of ice.
The hero: enemy `Bullet.frost` bullets (`Shape.SNOWFLAKE`, the Mountain Cube's fan) don't hurt, they fill
`Player.addFrost` (a snowflake icon beside the head, `frostWarning`); full, he's frozen in a block of ice
for `Player.FROZEN_TIME` (`isFrozen`: no walking, rolling or shooting) - every button pressed chips
`BREAK_OUT` off it, a hit or fire breaks it at once (sfx `freeze` / `shatter`). Ice on the floor is
`Puddle.Kind.ICE` (the Mountain Cube's trail, shattered enemies, where the Freeze Ray lands): it hurts no
one, but the hero (`Player.setOnIce`: his walking speed only slowly follows the stick) and walking
enemies (`Enemy.onIce`, unless `slipsOnIce` is off) slide about on it. Fire and ice put each other out
in `spillPuddle`. Acid puts fire out too, and fire can't start on a pool of acid. Wet player bullets
(`Bullet.wet`: Water Pistol, Bubble Blaster, Snowball Slinger) put out fire patches they fly over and
`douse` a burning enemy they hit.

## Gas, stun and petrification

`GasCloud` drifts and slows in the air. Enemy smoke ignites the player, or directly hurts
underwater where burning is disabled. Stink contributes to the acid exposure timer but
Acid Boots do not protect against it. Player sock gas poisons enemies and never the hero.
The room has a cap of 18 gas clouds; oldest clouds go first. Blanks and Hair Dryer blow
gas away. Rolling is safe through enemy gas.

Stun prevents walking, rolling and shooting; the shark's wave uses it, and does not stun
again while the current stun lasts. Petrification is a stone-colored stun used by Gorgon
when the player both lies in the gaze cone and aims toward her during the flash.
These are separate from frost and from invulnerability. Enemy chill slows behavior
(normal enemies to half, bosses to three quarters); ordinary bosses cannot freeze solid.

## Integration rules

Use `spillPuddle`, `releaseGas`, `damageEnemy` and the player's hazard helpers so immunity,
exposure warnings, damage attribution and room cleanup stay consistent. Persistent Slums
acid is recreated from room data and must not be evicted as an ordinary short-lived spill.
Transient puddles are cleared on changing rooms. See [Slums traps](slums.md) and
[combat](combat.md) for environmental damage and blank interactions.

## Implementation

- [Puddle](../src/main/java/com/trush/game/objects/Puddle.java)
- [GasCloud](../src/main/java/com/trush/game/objects/GasCloud.java)
- [Player](../src/main/java/com/trush/game/player/Player.java)
- [Enemy](../src/main/java/com/trush/game/enemy/Enemy.java)
- [GameWorld](../src/main/java/com/trush/game/world/GameWorld.java)

## Existing checks

- [FireAndAcidTest](../src/test/java/com/trush/game/player/FireAndAcidTest.java)
- [FluffulTest](../src/test/java/com/trush/game/objects/FluffulTest.java)
- [SlumsWayTest](../src/test/java/com/trush/game/world/SlumsWayTest.java)
