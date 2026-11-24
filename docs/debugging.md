# Dev builds, flags and recording

[Documentation index](README.md)

The flags below are JVM properties, placed before `-jar`. Build with the dev profile:

```sh
mvn clean package -Pdev
java -Dfluffy.prefs=test -Dfluffy.play=story -Dfluffy.floor=3 -Dfluffy.bossRoom=true -jar target/game-*-jar-with-dependencies.jar
```

Normal release builds ignore dev flags. IDE execution without filtered properties counts
as dev. Read new flags through `Dev.flag`, `Dev.number` or `Dev.text`, never directly
through `System.getProperty`. Only `dev.properties` is filtered.

Use `fluffy.prefs=test` or a task-specific suffix to keep experiments separate from real
progress. Bot sampling guidance and result interpretation are in [balancing](balancing.md).

## Shortcut reference

Debug flags only work in a dev build - `mvn clean package -Pdev` (running from the IDE counts as dev too);
a normal build ignores them. Pass them as JVM options, e.g.
`java -Dfluffy.floor=3 -Dfluffy.bossRoom=true -jar target/...`:

- `-Dfluffy.god=true` - hits never cost health
- `-Dfluffy.allGuns=true` - start with every gun
- `-Dfluffy.items=all` or `-Dfluffy.items=fluff_field,grenade` - start with these items
- `-Dfluffy.floor=N` - a new story run starts on floor N (2 = the town, 3 = the city, 4 = the skyscraper,
  5 = the Fluffy Kingdom, 6 = the secret sea)
- `-Dfluffy.sea=true` - a story run dives straight into the secret sea (with `-Dfluffy.bossRoom=true`:
  into the Fluffy Shark's lair)
- `-Dfluffy.slums=true` - a story run starts down in the Slums (with `-Dfluffy.bossRoom=true`, at its boss)
- `-Dfluffy.undercity=true` - a story run starts down in the Undercity (with `-Dfluffy.bossRoom=true`, at the
  Skunk King)
- `-Dfluffy.enemy=raccoon` - every story fight is just that kind of enemy (names in `StoryDirector.NAMES`:
  kin, veteran, shotgun, sawn_off, riot_cop, pirate, bandana, tanker, ghost, blue_shotgun, rats, sludger, sprayer, roller, marked_rats,
  stink_pigeon, gator, dog, raccoon, spiralmancer, bloomancer, cherub, dandelion...; the Moon's: bunnies,
  asteroid, astronaut, alien, ufo, golem, worm, veteran_bunnies, veteran_asteroid, veteran_astronaut, veteran_alien - try them with `-Dfluffy.moon=true -Dfluffy.fight=true`; the sea's: flufferfish, starfish, diver, turtle, swordfish, crab, octopus - with `-Dfluffy.sea=true -Dfluffy.fight=true`)
- `-Dfluffy.win=true` - a story run opens on the victory screen (with `-Dfluffy.undercity=true`: the normal ending)
- `-Dfluffy.fight=true` - a story run starts in the floor's deepest roomy fight (to try its enemies; e.g. with
  `-Dfluffy.slums=true`)
- `-Dfluffy.slums=true -Dfluffy.trap=acid` (or `pipes`) - start in one of the Slums' trap rooms, with that trap
- `-Dfluffy.floor=3 -Dfluffy.slumsWay=true` - a city run starts in the dead end with the burning drum
  (the way down to the Slums; add `-Dfluffy.allGuns=true` for a water gun)
- `-Dfluffy.play=story` / `-Dfluffy.play=endless` - skip the main menu and start right away; `-Dfluffy.play=home` - straight into the living room
- `-Dfluffy.bossRoom=true` - a new story run starts at the boss
- `-Dfluffy.retryBoss=true` - "try again" goes straight back to the boss
- `-Dfluffy.boss=evil_train` - always fight this boss (names are forgiving, e.g. `night hawk`); the sea's `kraken` / `gorgon` with `-Dfluffy.sea=true -Dfluffy.bossRoom=true`
- `-Dfluffy.keepSave=true` - continuing a saved run doesn't use the save up
- `-Dfluffy.mimic=true` - every town / city combat room hides a mimic lamp
- `-Dfluffy.blackChest=true` - every treasure chest (and a flawless boss's chest) is a black S chest, on any floor
- `-Dfluffy.startWave=N`, `-Dfluffy.layout=N` - endless mode: start wave, arena layout
- `-Dfluffy.shop=true` - a story run starts in Baa-llo's shop; endless opens his stall before wave 1
- `-Dfluffy.coins=N` - start with N coins
- `-Dfluffy.junk=N` - start with N junk and the Junk Knight awake at level N (7 = wings and a bow)
- `-Dfluffy.gun=chomper` - start with that gun in hand (any chest gun or the Flufful, by name: `long_shot`, `party_rocket`...)
- `-Dfluffy.bossHp=N` - a story boss starts at N% of its health, to try its later phases (Grandfluff: 40 = young again; 1 = young with a sliver of gold left, one hit from the Moon)
- `-Dfluffy.moon=true` - a story run starts up on the Moon (with `-Dfluffy.bossRoom=true`, at its boss)
- `-Dfluffy.play=ride` - the rocket ride up to the Moon first, then a story run (add `-Dfluffy.moon=true` to land on the Moon)
- `-Dfluffy.play=console` - straight into the rocket game on the living room's console (add `-Dfluffy.bossRoom=true` for a boss after 3 s - `-Dfluffy.boss=nighthawk` / `messenger` / `witch` / `fluffyking` / `grandfluff` picks which - `-Dfluffy.god=true` to keep your hearts)
- `-Dfluffy.console=true` - the rocket game console stands in the living room, as if the rocket had been up to the Moon (not saved)
- `-Dfluffy.book=N` - with `-Dfluffy.play=home`: the Flufficon opens right away at page N (0 records, 1 achievements, 2 enemies, 3 bosses, 4 guns, 5 items, 6 synergies)
- `-Dfluffy.prefs=test` - plays on a save file of its own (`~/.prefs/enter-the-fluffy-test`), the real save untouched
- `-Dfluffy.hero=baby` - plays as that one of the family (boy, sister, granny, grandad, baby or king; not saved)
- `-Dfluffy.skull=true` - the hard-mode skull is in the living room (as if the rocket were built)
- `-Dfluffy.hard=true` - every story run is in hard mode (not the baby's)
- `-Dfluffy.crowns=all` (or `boy,granny`) - those heroes wear the golden skull crown (not saved); `all`
  also brings the Fluffy King home
- `-Dfluffy.kingRoom=true` - the living room opens in the King's room (once he's moved in)
- `-Dfluffy.bot=true` - the balancing bot plays story runs by itself (dodges by predicting bullets, rolls, blanks when low, uses active items, explores, opens chests, fights the boss) and logs every fight and run as JSON lines; best with `-Dfluffy.play=story -Dfluffy.prefs=bot`. Extras: `-Dfluffy.botSpeed=N` (world steps a frame, default 3), `-Dfluffy.botRuns=N` (quit after N runs), `-Dfluffy.botFloors=N` (each run stops after N floors - with `-Dfluffy.floor`, `-Dfluffy.gun`, `-Dfluffy.items` to try a loadout), `-Dfluffy.botFloorLimit=S` (seconds on a floor before it gives up as stuck), `-Dfluffy.botKeepGun=true` (stays with the gun it starts with until it's empty), `-Dfluffy.botLog=path` (default `bot-log.jsonl`). Select `-Dfluffy.botProfile=average` (default, slower combat decisions and limited bullet tracking) or `-Dfluffy.botProfile=strong` (original full prediction).
- `-Dfluffy.play=dream` / `daily` / `rush` - straight into a dream, today's dream or the boss rush
- `-Dfluffy.bedroom=true` - the kids' bedroom is there without a win, and home opens in it
- `-Dfluffy.play=underbed` - straight to Under the Bed. Pair with `-Dfluffy.hero=baby`
  (or another hero) and `-Dfluffy.bossRoom=true` to try their boss.
  Add `-Dfluffy.fight=true` to start in a combat room, and `-Dfluffy.enemy=dust_mite`,
  `wind_up_beetle`, `lost_marble`, `peg_croc`, `quilt_moth` or `tumble_die` to try one
  resident's attacks. `dust_bunny_kit` tries the Dust Bunny's summoned helper.
- `-Dfluffy.play=home -Dfluffy.bedroom=true -Dfluffy.underBed=true` - try the unlocked
  entrance at the foot of the bed.
- `-Dfluffy.dreamFloor=N` - a dream (or the boss rush) starts on its floor N (`5`: a dream boss; with `-Dfluffy.boss=nightmare` any dream floor gets that boss, `-Dfluffy.bossRoom=true` starts in its arena)
- `-Dfluffy.dreamRule=glass_cannon` - forces a floor-wide Dream rule (`world/Dream.Rule` name: bouncy, gold_rush, tough_crowd, fast_forward, moon_boots, lights_out, glass_cannon, mixed_up, soft_landing, quick_draw, cross_draft). Under the Bed rolls its own room modifiers.
- `-Dfluffy.record=clip.mp4` - films the game into a video (needs `ffmpeg` on the PATH): every frame is drawn as exactly 1/30 s of game time and piped to ffmpeg, so the video is smooth however slowly it's drawn; the window opens at 1120x800 (the view's 7:5), fullscreen is ignored and the main menu's DEV BUILD tag hidden. `-Dfluffy.recordSeconds=N` quits after N seconds of video, `-Dfluffy.recordSkip=N` leaves out the first N. With the bot, `-Dfluffy.botSpeed=2` (its two 1/60 s steps are one frame) and boss cards play out instead of being skipped - e.g. `-Dfluffy.play=story -Dfluffy.bot=true -Dfluffy.botSpeed=2 -Dfluffy.god=true -Dfluffy.floor=3 -Dfluffy.fight=true -Dfluffy.record=city.mp4 -Dfluffy.recordSeconds=30`.
- `-Dfluffy.rocket=N` - the living room's cardboard rocket shows as built by the first N of the family (boy, sister, granny, grandad - the baby never helps; 0-4, not saved) - try it with `-Dfluffy.play=home`

A dev build shows "DEV BUILD" in the main menu.

## Build and regression checks

Run `mvn clean package` for the normal build and full existing JUnit suite. Targeted
checks can use `mvn -Dtest=ClassNameTest test`. Tests mocking libGDX classes need Mockito's
`MockMakers.SUBCLASS` because inline mocking can fail on newer JDKs. Do not introduce
backend extraction hacks; LWJGL3 already handles native extraction.

Recorder needs ffmpeg on PATH. It pipes fixed 1/30 s frames from a 1120×800 window;
`recordSkip` skips initial video and `recordSeconds` sets duration. Bot with recording
uses `botSpeed=2` so two 1/60 s steps match a video frame and boss cards can play out.
For an actual balancing run, avoid god mode and forced loot unless those are the condition
being measured, and label the flags used.

## Implementation

- [Dev](../src/main/java/com/trush/game/launcher/Dev.java)
- [Recorder](../src/main/java/com/trush/game/launcher/Recorder.java)
- [AppLauncher](../src/main/java/com/trush/game/launcher/AppLauncher.java)
- [Bot](../src/main/java/com/trush/game/bot/Bot.java)
- [Gamepad](../src/main/java/com/trush/game/input/Gamepad.java)
