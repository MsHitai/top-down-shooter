# Boss roster and fight lifecycle

[Documentation index](README.md)

`Boss` is the registry: title, stage, factory, quote, defeat banner, music, arena geometry,
layout and spawn overrides. `Enemy` supplies common combat and presentation hooks.
Story selects a stage boss; final King selection depends on the playable hero.

## Registry and selection

| Place | Bosses | Distinct mechanics |
| --- | --- | --- |
| Village | Haymaker, Harvestman, Hollow Witch | Pulsing windup ring before charges, warned straw walls/crows/rain; chainsaw pursuit/lunges/blades; hex bolts/sparks/gap ring/lanterns |
| Town | Trash That Can, Road Rager, Traffic Light | Garbage bombs/trash walls; vehicle lasers/ram/helper drops; green streams, yellow warning, red movement punishment |
| City | Night Hawk, Pigeon King, Evil Train | Door gun/rockets/searchlight; statue, bread pigeons and flock lanes; oval track, wagons, smoke, steam and reversal |
| Skyscraper | Kaiju, Mega Mech, Messenger | Roof-edge mouth beam/slams/bite; jump shockwaves/spiral turret; sword triangles/crescents/cloud rain |
| Clouds | Fluffy King; Shadow King when playing King | King tantrum then furious patterns; Shadow streams/echoes/movement glare |
| Slums | Blue Blob, Alley Cat, Walking Building | Family splits; sofa then prowling; advancing solid facade |
| Undercity | Skunk King | Cart throne gas/decrees, then crown-off panic |
| Sea | Fluffy Shark, later Kraken and Gorgon | Waves/bites; tentacle lanes/whirlpool; gaze/petrification |
| Moon | Grandfluff | Old → young → giant Moon, two health layers |
| Dream finale | Alarm Clock, Nightmare, Big Bad Teddy | Separate dream boss attacks and arena/music |
| Under the Bed | Dust Bunny, Monster Under the Bed, Sock Ninja, Yarn Hydra, Wind-up General, Lint Regent | One resident assigned per hero, shared pattern engine |

`pickFor`, `pickAny`, `finalBoss`, `isDream` and `underBed` encode different eligibility.
Underground, Moon, dream, Under the Bed and Shadow bosses must not leak into ordinary
endless random selection. Sea bosses are unlock-gated; first shark victory enables
unbeaten-first selection among its three. See mode documents for their exact order.

## Start, phase changes and completion

When a boss appears, `GameScreen` glides the camera to it for a one-second lead, presents
`BossCard` while simulation waits, then returns to the player. The card uses a fresh
boss instance for its portrait, a title/quote and skip input. Grouped bosses use
`bossFight` to present one identity and `bossBarFraction` for the shared health bar.

Boss phase changes can clear bullets and temporarily make the boss untouchable. The
Fluffy King tantrums below 55%, then adds flop/shockwaves and extra attacks without angel
helpers. Blue Blob splits at 75% and again at 50%, retaining a family health pool and
flawless flag. Grandfluff's gold health covers old/young forms, then the red Moon life
starts full when gold runs out. See each region document for those special cases.

Boss death goes through `GameWorld.dying`; rewards and exit opening wait for the show.
The camera shows the final blast and starts following the player again as the death show ends. Flawless
rewards and progression use the fight outcome once, rather than each spawned body.

## Arenas and large bodies

Most bosses use an open 25×15 arena. Witch and Road Rager have cover; Traffic Light gets
cars because its green streams cannot be rolled through. Kaiju uses a short roof and
head hitbox, clipped body and zoomed view. Walking Building uses a 17×40 side-entered
street, a moving blocker and facade-wide targeting. Boss rendering, damage regions and
navigation blockers are separate concerns.

When adding a boss, register selection eligibility, create/quote/music/banner/portrait,
geometry, Flufficon discovery and reward behavior. Keep warnings and timing in simulation,
route damage and summons through the world, and preserve blanked tracked projectiles.
See [combat](combat.md), [difficulty](difficulty.md) and [Under the Bed](under-the-bed.md).

## Implementation

- [Boss](../src/main/java/com/trush/game/enemy/Boss.java)
- [Enemy](../src/main/java/com/trush/game/enemy/Enemy.java)
- [BossCard](../src/main/java/com/trush/game/screen/BossCard.java)
- [GameScreen](../src/main/java/com/trush/game/screen/GameScreen.java)
- [BlueBlob](../src/main/java/com/trush/game/enemy/BlueBlob.java)
- [Grandfluff](../src/main/java/com/trush/game/enemy/Grandfluff.java)
- [Patterns](../src/main/java/com/trush/game/enemy/Patterns.java)

## Existing checks

- [BossTest](../src/test/java/com/trush/game/enemy/BossTest.java)
- [SlumsBossesTest](../src/test/java/com/trush/game/enemy/SlumsBossesTest.java)
- [SeaBossesTest](../src/test/java/com/trush/game/enemy/SeaBossesTest.java)
- [GrandfluffTest](../src/test/java/com/trush/game/enemy/GrandfluffTest.java)
- [BedBossTest](../src/test/java/com/trush/game/enemy/BedBossTest.java)
