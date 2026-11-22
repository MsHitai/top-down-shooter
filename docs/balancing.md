# Balancing, bot runs and test evidence

[Documentation index](README.md)

## Bot operation

`bot/Bot` drives the virtual gamepad through `Gamepad.drive`, using the same player
controls. Dev `bot=true` enables fixed 1/60 s simulation, accelerated by `botSpeed`
(default three steps per frame), no menus, muted sound and no focus-loss pause. It
explores, fights, opens chests, rolls, uses blanks/actives and records JSON lines.

The default `botProfile=average` updates its combat movement and aim every 0.18–0.28
game seconds. It considers eight movement directions and tracks up to two live bullets
within 1.5 units, prioritizing those projected to pass closest over the next 0.32 seconds.
It favors keeping its heading when the choices are close and scores the clearance
along the first 0.24 seconds of each walk. When the best walk is too tight, it looks
for a roll with a clear route and room at the exposed end. Use `-Dfluffy.botProfile=strong`
for the original bot, which checks 16 directions against every bullet each step and
looks ahead up to 0.48 seconds. Both profiles use the same exploration and equipment
logic. Each JSON line records its `profile`. Logs from before profiles existed used
the strong bot.

Both profiles use the Under the Bed lanes shown during wind-up to score walking
routes. The average profile reads them on its usual decision tick. Live projectiles
still take priority when choosing a direction.

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
against player runs. The November 21 samples used an earlier average profile that tracked a
single nearby shot. Both profiles can miss off-screen ricochets and other warning types. Compare conditions
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

## Under the Bed boss calibration

The earlier target was about six wins in ten for the average bot in each hero's
boss room; after the owner's November 22 playtest it is superseded - the fights are
designed for readable fun first (see the distinct-move redesign below) and the owner
tunes final difficulty by playtest. Bot samples remain the record of how the average
profile fares. Each hero starts at full health with their starter loadout.
Baby wins only if the Guardian Lamb survives. A lamb rescue counts as a loss even
if Baby subsequently defeats Dust Bunny. Room depth and attack decisions vary
between runs. Use normal difficulty and fresh test preferences. To repeat one
matchup in a dev build:

```sh
java -Dfluffy.play=underbed -Dfluffy.bossRoom=true -Dfluffy.bot=true \
  -Dfluffy.botProfile=average -Dfluffy.hero=sister -Dfluffy.prefs=bed-sister-sample \
  -Dfluffy.botRuns=200 -Dfluffy.botSpeed=480 -Dfluffy.botLog=bed-sister-sample.jsonl \
  -jar target/game-*-jar-with-dependencies.jar
```

Count `run` events with `result == "victory"` and `lambSaves == 0` as wins.
Use a new log path for each sample.
Full-route fights can start with a better gun or fewer hearts. Hard mode also changes
boss pace and projectile speed.

### Distinct-move redesign, November 22 (late)

The owner's playtest of the pattern-pressure build found the clone: the same spiral,
converging bloom ring and eleven-wide aimed fan appeared verbatim in the Yarn Hydra,
Dust Bunny and Lint Regent fights (and the ring and fan in four of six). The bloom
ring was the one attack that killed humans while the average bot strolled out of it,
and the fights felt longer than their 700 HP - which was exact: the length was
volume and residue, not health. Confirmed direction: de-clone all six residents in
the floor enemies' idiom - one readable formation per move with a walking door, aim
committed at release, nothing converging on the hero - drop the 6/10 bot target in
favour of the owner's playtest, and keep HP at 700.

`BedPattern` lost every shared helper (`snip`, `bloomRing`, `aimedFan`); the middle
moves are now per resident (sneeze chain and clinging lintels, parted hem and pillow
fort, clothesline curtain, knitting Vs and dropped loop, marching ranks, collectors'
patrol and the outward-opening seal). Cycles shortened from 33-38 volleys to 20-24,
so fights shorten on their own.

Fresh 200-fight samples per hero, same conditions as above (starter loadout,
normal difficulty, `bossRoom`):

| Boss (hero) | Base HP | Bot wins | Median fight time |
| --- | ---: | ---: | ---: |
| Dust Bunny (Baby) | 700 | 200/200 (100%) | 51.6 s |
| Monster Under the Bed (Boy) | 700 | 200/200 (100%) | 43.7 s |
| Sock Ninja (Sister) | 700 | 88/200 (44.0%) | 48.1 s |
| Yarn Hydra (Granny) | 700 | 200/200 (100%) | 50.4 s |
| Wind-up General (Grandad) | 700 | 200/200 (100%) | 34.9 s |
| Lint Regent (King) | 700 | 199/200 (99.5%) | 62.0 s |

Baby spent the Guardian Lamb in 0 of 200 fights (89 of 200 on the previous build).
The high rates are expected, not a regression: doors and streets are exactly what
the average profile sidesteps, and these samples are now evidence, not a target.
The owner's playtest is the tuning bar; per-boss HP and beat trims stay the
one-line levers if a fight still runs long after playing it.

### Pattern-pressure redesign, November 22 (evening)

*Superseded later the same evening by the distinct-move redesign above.*

The owner's direction: residents stand their ground like the Fluffy King (0/200 for
the average bot with starter gear, as is Grandfluff - see the owner's samples), the
difficulty comes from more bullets and more distinct attacks, bosses never chase, and
700 HP stays the cap. Every resident now plays five distinct moves: a signature, a
Grandfluff-tempo spiral (quick beats, many arms, curving), a converging bloom ring
around the bait (creep, then rush - the red light's trick), a themed finisher with
the King's eleven-wide aimed fan, and a fifth distinct pattern each (sneeze cone,
mattress quake rings, needle web, braid, tolling bell, sceptre sweep). Fronts persist
until they hit a wall; only fast shots must clear before the next attack. Calibration
bots in `bossRoom=true` runs get no boss chest and no flawless heart container - that
loot persisted through retries and dirtied earlier samples (the owner caught it).

The escape tests' dodge margin was recalibrated from 0.17 to 0.10: the old margin
modelled a player sloppier than the average bot the tests exist to represent, and it
capped the density the fights could legally reach.

Current honest state (100-fight samples): Sister 56%, King 95%, the others 100% with
1-3 hearts lost per clear - damage now flows but the rate is still ~2.5x short of the
six-in-ten target. The proven converters (from the ninja, the one calibrated fight,
and the owner's King samples): close-range fast convergence and telegraphed contact
attacks. Next lever: a telegraphed themed lunge in each resident's cycle, or the
owner's playtest verdict on the current feel.

### Calibrated patterns, November 22, 2026

The current fights use shorter, separate projectile fronts, with body volleys of at
most ten bullets. Each boss has its own timing and trajectories. The 700 HP cap and
shared player/bot warnings for remote lanes still apply. The Sock Ninja opens her
signature with a marked dash (pose plus edge chevrons, one heart on contact, no band
drawn on the floor), and several body moves end with an accent-coloured heavy shot
that reacquires the hero at release; those stay slower than the ninja's marked needle.

The average bot had won every smoke fight on the earlier distinct-pattern build. The
recalibration restored pressure with readable levers: the Regent's beats tightened to
.40/.50 s, his crown, seal and necklace fronts sped up, the levy rows ride at 3.0 u/s,
his necklace star reacquires on the last two beats, he rages below 60% health and his
HP rose to the 700 cap; the Hydra's traded knot slowed from 4.45 to 4.15 u/s. A
cornered-rage attempt to close the crown's walking aisle failed the escape test and
was reverted - pressure never comes from removing the walking street.

| Boss (hero) | Base HP | Bot wins | Median fight time |
| --- | ---: | ---: | ---: |
| Dust Bunny (Baby) | 700 | 111/200 (55.5%) | 50.3 s |
| Monster Under the Bed (Boy) | 700 | 120/200 (60.0%) | 42.8 s |
| Sock Ninja (Sister) | 545 | 117/200 (58.5%) | 39.0 s |
| Yarn Hydra (Granny) | 700 | 107/200 (53.5%) | 47.5 s |
| Wind-up General (Grandad) | 700 | 114/200 (57.0%) | 30.6 s |
| Lint Regent (King) | 700 | 122/200 (61.0%) | 60.6 s |

The bot won 691 of 1,200 fights (57.6%) in fresh 200-fight samples per hero. Baby
spent the Guardian Lamb in 89 of 200 fights, all scored as losses; every other hero's
losses were plain deaths. The earlier win-rate tables below describe superseded
patterns.

### Earlier 700 HP cap and warning-aware bot

That build shortened recovery and capped every resident at 700 HP. The bot reads
the same marked lanes shown on screen. Earlier rage adds pressure to the Dust Bunny,
Monster and Wind-up General fights. The General's large spring and bank shells deal
one heart on contact.

| Boss (hero) | Base HP | Bot wins | Median fight time, previous → current |
| --- | ---: | ---: | ---: |
| Dust Bunny (Baby) | 700 | 50/80 (62.5%) | 78.5 → 52.9 s |
| Monster Under the Bed (Boy) | 700 | 50/80 (62.5%) | 67.0 → 44.2 s |
| Sock Ninja (Sister) | 445 | 61/100 (61.0%) | 33.4 → 33.4 s |
| Yarn Hydra (Granny) | 700 | 65/100 (65.0%) | 60.8 → 50.2 s |
| Wind-up General (Grandad) | 700 | 60/100 (60.0%) | 46.8 → 33.7 s |
| Lint Regent (King) | 640 | 58/100 (58.0%) | 58.0 → 59.0 s |

The bot won 344 of 560 fights (61.4%) in the samples at the selected settings.
Baby used the Lamb in 30 of 80 fights, all scored as losses. The previous timings
came from the earlier November 22 sample with a bot that missed the marked lanes.

### Earlier November 22, 2026

The updated average bot won 80 of 80 fights per hero with the previous boss settings.
The retune adds fast shots along three marked lanes ahead of the slow body volleys and
raises each boss's base health. Each row reports the sample at the selected health.

| Boss (hero) | Base health before → after | Wins | Win rate |
| --- | ---: | ---: | ---: |
| Dust Bunny (Baby) | 518 → 1,100 | 76/120 | 63.3% |
| Monster Under the Bed (Boy) | 544 → 1,110 | 75/120 | 62.5% |
| Sock Ninja (Sister) | 312 → 445 | 120/200 | 60.0% |
| Yarn Hydra (Granny) | 492 → 880 | 126/200 | 63.0% |
| Wind-up General (Grandad) | 950 → 1,065 | 72/120 | 60.0% |
| Lint Regent (King) | 415 → 640 | 75/120 | 62.5% |

The bot won 544 of 880 fights (61.8%). Baby used the Lamb in 44 of 120 fights.
These samples were used to select the settings. The rates estimate normal-mode
performance with the bot and starter loadouts.

### November 21, 2026

These results used the earlier average bot. Each final sample used fresh fights
after selecting that boss's settings.

| Boss (hero) | Baseline wins | Final wins | Final win rate |
| --- | ---: | ---: | ---: |
| Dust Bunny (Baby) | 27/30 | 108/200 | 54% |
| Monster Under the Bed (Boy) | 7/30 | 107/200 | 53.5% |
| Sock Ninja (Sister) | 0/30 | 119/200 | 59.5% |
| Yarn Hydra (Granny) | 9/30 | 117/200 | 58.5% |
| Wind-up General (Grandad) | 30/30 | 116/200 | 58% |
| Lint Regent (King) | 4/30 | 108/200 | 54% |

The average bot won 675 of 1,200 fights (56.25%). Baby defeated Dust Bunny in all
200 fights and spent the Lamb in 92 of them. At 200 fights per boss, sampling
uncertainty near the target is roughly seven percentage points in either direction
(95% interval). Human win rates require human playtest data.

## Under the Bed floor calibration

Start the average bot at the entrance with starter gear and ordinary chest loot. Score
one win per run that reaches the boss alive. For Baby, a `lamb_save` before the boss
room counts as a loss, even if Baby later finishes the floor. A rescue or death during
the boss fight comes after the scoring point. A run that times out before arrival
counts as a failed trip.

To sample one hero, use fresh preferences and a fresh log path in a dev build.

```sh
java -Dfluffy.play=underbed -Dfluffy.bot=true -Dfluffy.botProfile=average \
  -Dfluffy.hero=baby -Dfluffy.prefs=bed-floor-baby -Dfluffy.botLog=bed-floor-baby.jsonl \
  -Dfluffy.botRuns=200 -Dfluffy.botSpeed=160 -jar target/game-*-jar-with-dependencies.jar
```

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
