# Difficulty, hard mode and encounter scaling

[Documentation index](README.md)

Difficulty affects bullet speed and attack behavior; regular-enemy health has a separate
multiplier. Bosses scale by their own tier instead of the regular-enemy health multiplier.
A numeric floor may differ from a stage's ordinary place after a detour.

## Story scaling

On room entry, the base difficulty is a depth term plus `0.15 * (level - 1)`.
Ordinary depth is `min(1.35, 1 + 0.05 * depth)`; skyscraper depth is
`min(1.5, 1 + 0.07 * depth)`. Regular health is `1 + 0.15 * (level - 1)`,
with another `0.04 * depth` in the skyscraper. The sea's effective level is two floors
above its departure floor and adds three budget points and one active enemy.

Spawn budgets and simultaneous counts are separate controls. Small rooms reduce both;
the skyscraper grows its budget and crowd with depth. Reinforcements add another group,
not a higher simultaneous cap. See [campaign](campaign.md).

## Hard mode

The completed rocket enables a skull in the hub. Its choice is copied into a new story
run, saved with it, and retained across floors. Baby always stays easy. Hard mode adds:

| Parameter | Change |
| --- | --- |
| `GameWorld.getDifficulty` | +0.2 |
| Boss behavior clock | ×1.2 (movement, warning, attack and recovery together) |
| Boss bullet styling speed | Full speed instead of normal ×0.85 |
| Room budget | ×1.35 |
| Active cap | +2 in roomy fights, +1 in small rooms |
| Grown-up enemy variants | One floor earlier |
| Ordinary ammo-drop chance | 6% instead of 4%, still one box per room |

Hard mode does not add a separate regular-enemy health boost: crowding and attack pressure
are the intended change. Grandfluff's young act uses ×1.1 pace and ×0.95 bullet speed
in hard mode, excluding his rise into the Moon. Under the Bed caps residents at four
normally or five hard, plus pursuer/heavy restrictions. Rainbow Ray's finite ammo and
damage are explicitly adjusted for hard mode; see [weapons](weapons.md).

## Endless and dreams

Endless difficulty is capped at 1.45:
`min(1.45, 1 + 0.045 * (wave - 1) + 0.1 * bossTier)`.
Regular health rises 4% per wave to a 1.75 cap. Tougher kin arrive at fixed wave thresholds.
Ammo becomes more available as finite guns empty; see [endless](endless.md).

Dream rules can change behavior independently of hard mode: Fast Forward advances the
world faster, Glass Cannon doubles hits both ways, and Tough Crowd selects tougher
variants. Boss rush uses the boss's home level and tier zero; ordinary dreams can promote
a shallower boss to a higher tier. See [dreams](dreams.md).

Use [balancing](balancing.md) for bot sampling and [debugging](debugging.md) for isolated
loadouts. Bot wins alone do not establish that humans can navigate a dense pattern.

## Implementation

- [StoryDirector](../src/main/java/com/trush/game/world/StoryDirector.java)
- [GameWorld](../src/main/java/com/trush/game/world/GameWorld.java)
- [WaveDirector](../src/main/java/com/trush/game/world/WaveDirector.java)
- [Dream](../src/main/java/com/trush/game/world/Dream.java)
- [Enemy](../src/main/java/com/trush/game/enemy/Enemy.java)
- [Grandfluff](../src/main/java/com/trush/game/enemy/Grandfluff.java)

## Existing checks

- [HardModeTest](../src/test/java/com/trush/game/world/HardModeTest.java)
- [ReinforcementsTest](../src/test/java/com/trush/game/world/ReinforcementsTest.java)
- [BedEncountersTest](../src/test/java/com/trush/game/world/BedEncountersTest.java)
- [RainbowRayHardModeTest](../src/test/java/com/trush/game/save/RainbowRayHardModeTest.java)
