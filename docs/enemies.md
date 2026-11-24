# Regular enemies and AI

[Documentation index](README.md)

## Shared behavior

`Enemy` owns health, collision radius, sight, aim, status effects, navigation, death and
common firing helpers. Walking enemies flank obstacles on a 0.25-unit grid and seek a
clear firing point. Flying bypasses movement obstacles; it does not automatically mean
a creature should render above everything (Cheese Worm is a burrower).

A player hit provokes an enemy for 2.5 seconds: it can retaliate even when its ordinary
sight test is blocked or it is off screen. Some attacks lead the hero using actual
post-collision velocity. Ordinary decoy targeting uses `targetX/targetY`; bosses ignore
the decoy. Dodging/teleporting enemies expose `isDodging`/`isUntouchable`, respected by
bullets, beams and blasts. The world keeps all spawning and damage bookkeeping.

## Populations

| Region / family | Actors and behavior |
| --- | --- |
| Shared gun kin | Bullet Kin single shots; Shotgun Kin led blasts and low-health temper; Bandana Kin bursts; Pirate Kin cannonball/grapeshot; Tanker Kin swept stream; Ghost Kin fading ring; Blue Shotgun Kin two pumps and death ring |
| Grown-up variants | Veteran Kin led three-shot bursts; Sawn-off Kin rush and double blast; Riot Cop directional shield |
| Village / caster family | Bulletmancer channels volleys; town Spiralmancer rotating spiral; city Bloomancer seeds that bloom into petal rings |
| Town | Hydrant swept water, squishable rat packs, Shell Cop led bursts, Shapeshifter formations |
| City | Biker drive-by streams, Sniper cover-limited laser lock, Robber bullet-dodging double taps |
| Tower | RoboCart straight movement/crosses, Android laser bursts, Agent paired shots, Mountain and Lead Cubes aligned charges |
| Clouds | Sword Angel slashes, Bow Angel lock/sky arrows, Thundercloud marked lightning, Cherub heart formations, Dandelion hanging seeds that dart |
| Underground | Acid gang and marked critters; see [Slums](slums.md) and [Undercity](undercity.md) |
| Sea | Seven water creatures with population limits; see [Deep Sea](deep-sea.md) |
| Moon | Bunnies, asteroids, astronauts, aliens, UFO, golem, worm and veterans; see [Moon](moon.md) |
| Under the Bed | Critters and formation heavies; see [Under the Bed](under-the-bed.md) |

Dynamite appears broadly: approaches, lights a fuse and explodes, damaging hero and
nearby enemies. Contact itself is harmless so a bump cannot grant immunity to its blast.
Shooting it detonates it early and it leaves fire. Sludger can rarely appear outside its
home in town/city/tower and later endless waves.

Stage selectors determine availability and costs, separately from class behavior.
`StoryDirector.gunKin` mixes special kin into regular populations with a declining share
on deeper stages; sea and Moon use their own selectors. `WaveDirector` has independent
wave thresholds. Do not assume adding an enemy factory makes it available in every mode.

## Mimic lamps

A rare town/city combat room (4%) attaches a mimic to a lamp prop. Approaching, shooting
or blasting wakes it into a hopping enemy: gap rings on landings and spark fans every
third hop. Its visible post and lantern are shootable through `bodyContains`.
Waking after room clear relocks the doors until it dies, with no second room-clear reward.
It drops a key or blank. The forced `mimic` debug flag helps reproduce this path.

## Adding an enemy

Implement its behavior, collision and readable attack tells; add selectors/costs/caps in
the relevant director or family helper. Spawn through `GameWorld.spawnEnemy`. Add a
stable Flufficon ID/catalog entry and portrait, generated art and sound registrations.
Variants sharing a class need distinct `dexId`s. Check navigation, pending-spawn limits,
blanked explosive behavior and death rewards. See [combat](combat.md) and [assets](assets.md).

## Implementation

- [Enemy](../src/main/java/com/trush/game/enemy/Enemy.java)
- [EnemyCover](../src/main/java/com/trush/game/enemy/EnemyCover.java)
- [Patterns](../src/main/java/com/trush/game/enemy/Patterns.java)
- [LampMimic](../src/main/java/com/trush/game/enemy/LampMimic.java)
- [Marked](../src/main/java/com/trush/game/enemy/Marked.java)
- [StoryDirector](../src/main/java/com/trush/game/world/StoryDirector.java)
- [WaveDirector](../src/main/java/com/trush/game/world/WaveDirector.java)
- [Flufficon](../src/main/java/com/trush/game/save/Flufficon.java)

## Existing checks

- [EnemyPursuitTest](../src/test/java/com/trush/game/enemy/EnemyPursuitTest.java)
- [EnemyNavigationTest](../src/test/java/com/trush/game/world/EnemyNavigationTest.java)
- [GunKinTest](../src/test/java/com/trush/game/enemy/GunKinTest.java)
- [LampMimicTest](../src/test/java/com/trush/game/enemy/LampMimicTest.java)
- [AttackWarningTest](../src/test/java/com/trush/game/enemy/AttackWarningTest.java)
- [NewCastersTest](../src/test/java/com/trush/game/enemy/NewCastersTest.java)
