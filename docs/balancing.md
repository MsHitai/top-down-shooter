# Balancing, bot runs and test evidence

[Documentation index](README.md)

## Bot operation

`bot/Bot` drives the virtual gamepad through `Gamepad.drive`, using the same player
controls. Dev `bot=true` enables fixed 1/60 s simulation, accelerated by `botSpeed`
(default three steps per frame), no menus, muted sound and no focus-loss pause. It
explores, fights, opens chests, rolls, uses blanks/actives and records JSON lines.

The default `botProfile=average` updates its combat movement and aim every 0.18–0.30
game seconds. It considers eight movement directions and tracks the nearest live bullet
within 1.5 units, looking ahead up to 0.24 seconds. It may spend a roll or blank on a shot that
a sidestep could have avoided. Use `-Dfluffy.botProfile=strong`
for the original bot, which checks 16 directions against every bullet each step and
looks ahead up to 0.48 seconds. Both profiles use the same exploration and equipment
logic. Each JSON line records its `profile`. Logs from before profiles existed used
the strong bot.

For a sample from the current loadout:

```sh
mvn clean package -Pdev
java -Dfluffy.play=story -Dfluffy.prefs=bot -Dfluffy.bot=true -Dfluffy.botRuns=8 -Dfluffy.botLog=bot-log.jsonl -jar target/game-*-jar-with-dependencies.jar
```

`botFloors` ends after a chosen number of floors; `botFloorLimit` bounds game seconds
before a stuck result; `botKeepGun` retains the starting gun until empty. `floor`, `gun`
and `items` can isolate a loadout. `play=underbed` isolates the short bedroom route.
Do not compare a forced invulnerable loadout with ordinary runs without labeling it.

## Interpreting evidence

The average profile approximates limited attention. Its settings have yet to be calibrated
against player runs. Both profiles can miss warning-based blasts, lasers and delayed attacks. Compare conditions
and inspect play/video; a high win rate is not proof of human playability. Record hero,
mode, build, flags, sample size and changes to random layouts/loot. Paired seeds are
needed before treating two random samples as a controlled comparison.

Logs include fight/run `lambSaves` and `lamb_save` events. Count those as fatal hits when
balancing Baby: a lamb's full heal is not visible in `halvesLost`. Also inspect rolls,
blanks, time, stuck rooms and where damage occurs, rather than win rate alone.

Headless attack checks establish specific properties such as readable warnings,
projectile geometry, sampled walking escapes, collision and queue caps. They do not
model every overlapping fight. Under the Bed checks sample a 180 ms reaction delay,
wall-adjacent positions, phases and hard mode; see [Under the Bed](under-the-bed.md).

## Initial profile comparison

On 2026-11-21, Boy fought the Haymaker on normal difficulty with the Rusty Sidearm,
starting directly in the boss room each time (`bossRoom=true`, `retryBoss=true`).
In ten fights per profile, average cleared zero and used 29 rolls and nine blanks.
Strong cleared ten and used 40 rolls and zero blanks. In eight separate average runs
starting at the village entrance with normal loot, all eight reached the Haymaker
and two cleared the floor. These samples used random layouts and attack sequences.

## Historical Under the Bed observations

These notes preserve the earlier evidence from DEVELOPING.md, not a statement of the
current attack counts or current difficulty. Use the source and current mechanic page
for present behavior.

Balance check, 2026-11-19: full Under the Bed runs with the usual starter and random loot, isolated
preferences, fixed 1/60-second simulation steps (`botSpeed=24`, `botFloorLimit=480`). No forced enemies,
guns, items or invulnerability in the measured runs. Before the change, Baby and hard-mode Boy each won
all eight runs; across their 64 regular combat rooms the bot rolled zero times and lost one half-heart.
That earlier, denser version produced:

| Hero / mode | Runs | Wins | Deaths | Lamb resurrections | Rolls in regular rooms |
| --- | ---: | ---: | ---: | ---: | ---: |
| Baby / easy | 24 | 24 | 0 | 1 | 38 |
| Boy / hard | 8 | 5 | 3 | 0 | 11 |

All three deaths and the resurrection happened in regular combat rooms, before the boss. These are
random samples with different layouts and loot, not paired trials or a measure of human difficulty.

Human feedback, 2026-11-19: an experienced player could not clear three rooms as Baby.
Reduced regular-enemy fire accordingly: fewer shots per volley for the mite, beetle, marble, croc
and moth; longer recovery pauses for all seven residents. The die retains both squares, with its
pause increased from 1.05 to 1.65 seconds. Boss patterns and encounter populations are unchanged.
The bot results above predate this adjustment and do not establish human playability.

## Implementation

- [Bot](../src/main/java/com/trush/game/bot/Bot.java)
- [Gamepad](../src/main/java/com/trush/game/input/Gamepad.java)
- [Recorder](../src/main/java/com/trush/game/launcher/Recorder.java)

## Existing checks

- [BedPatternTest](../src/test/java/com/trush/game/enemy/BedPatternTest.java)
- [BedBossFairnessTest](../src/test/java/com/trush/game/enemy/BedBossFairnessTest.java)
- [BedEncountersTest](../src/test/java/com/trush/game/world/BedEncountersTest.java)
- [ReinforcementsTest](../src/test/java/com/trush/game/world/ReinforcementsTest.java)
