# Dreams, daily runs and boss challenges

[Documentation index](README.md)

The bedroom appears after a win/ending. Bed starts a dream, calendar starts today's
dream, and toy box starts boss rush. All use `Dream` state and `StoryDirector`, carried
between floors in memory through `RunSave.dream`, never written as a resumable disk trip.
Completing a bed/calendar dream opens [Under the Bed](under-the-bed.md).

## Ordinary dreams

A dream has five floors. Looks come from found stages, excluding sea and Under the Bed,
with no repeats when possible and no immediate repeats in fallback selection. Enemy
stages expand with dream depth; they need not match the room look. Secret entrances are
removed and no rocket appears. Boss selection first prefers a beaten boss near this
depth, then relaxes that restriction if needed; it avoids repeats and excludes sea,
Shadow King and Grandfluff. The last boss is Alarm Clock, Nightmare or Big Bad Teddy,
with an unbeaten dream boss preferred. These have dreamland arena/music and do not enter
ordinary endless selection.

Each floor gets one rule, two from floor 4. Boss rush gets one on even floors and none
on odd floors. [Under the Bed](under-the-bed.md#room-modifiers) assigns one rule per
room, with the shop and treasure rooms exempt. The HUD shows active rules.

| Rule | Behavior |
| --- | --- |
| Bouncy | Eligible plain player bullets get two extra wall bounces |
| Gold Rush | Double coins |
| Tough Crowd | Tougher enemy variants |
| Fast Forward | World advances ×1.2 |
| Moon Boots | Roll reach ×1.5 |
| Lights Out | Dedicated darker mask and reduced clear area |
| Glass Cannon | Double damage in both directions |
| Mixed Up | Eligible regular enemies sometimes borrow another enemy's volley |
| Soft Landing | Dodge-roll landings clear bullets within 0.9 units, with a five-second cooldown |
| Quick Draw | The next weapon attack after a dodge roll deals 50% extra damage, with a three-second cooldown after firing |
| Cross Draft | Floor arrows warn of a brief sideways gust that drifts both sides' free-flight bullets |
| Slow Reload | Reloads started under the rule take 30% longer, including with reload-boosting items |
| Small Blanks | The blank ring clears bullets, disperses gas and dazes enemies within 2 units. Applies to carried blanks and Bubble Wand |
| Falling Dust | Every six seconds, debris marks the hero's current spot. It lands 1.2 seconds later and deals half a heart of base damage on contact |

Mixed Up, Cross Draft and Falling Dust apply in regular combat rooms. Each Mixed Up replacement
randomly picks one volley from the Shotgun Kin's fan, the Shapeshifter's square, RoboCart's four-bolt
cross, the Agent's twin shot and the Bloomancer's six-petal ring. The same pattern can roll twice in
a row. A borrowed cross rolls a horizontal and vertical `+` or a diagonal `X`. A borrowed twin shot
fires one pair, and a borrowed petal ring blooms directly from the shooter.
The attack uses the shooter's usual wind-up. Cross Draft adds 1.1 units per second of sideways
drift to free-flight bullets during each 1.5-second gust. Gusts alternate direction, and scripted
projectile paths stay intact. Falling Dust skips trap rooms and clears its warning when the fight ends.

## Story dares

The skull's **Story Dares** screen offers Fast Forward, Glass Cannon, Small Blanks,
Tough Crowd, Lights Out, Slow Reload, Mixed Up, Cross Draft and Falling Dust. Toggle
up to two Dares in the list. The panel on the right previews the focused rule and
shows your selections. With two Dares active, you may choose one positive modifier
for the entire trip: Bouncy Bullets, Gold Rush, Moon Boots, Soft Landing, or Quick Draw.
These use the same effects as the Dream rules above. Choose **Confirm** before taking
a new trip through the front door. To clear the selection, deselect the Dares and confirm.

The game remembers confirmed selections for future trips. The active trip keeps its
own Dares and modifier through detours, saves, and retries. The HUD shows both.
Hard Mode can be toggled independently. [Chest rewards](loot-and-chests.md#story-dare-rewards)
apply while at least one dare is active.

## Today's dream

`todaySeed` uses the local calendar date as YYYYMMDD. Floor generation is seeded from
that date but selection also depends on the account's discovered/unlocked content; it
is not a guarantee that differently progressed players get identical runs. A retry
retains the original seed even past midnight. Daily completion is counted once per seed;
best daily score can be submitted for a finished or unfinished attempt.

## Boss rush

`rushBosses` builds an ordered list: one from each eligible road/underground stage,
a discovered sea boss before King, King or Shadow King, Grandfluff if previously beaten,
then a dream boss. Beaten bosses are preferred, with fallback when the pool is empty.
Each floor has a start and boss arena. Boss strength uses its home level and tier zero.

A shop follows every second boss (floor 2, 4, 6...), excluding sea. Moon uses the cloud
shop. Shops are level-one price, include an ammo stand and guarantee a gun. Bosses pay
twice normal coins and drop ammo if needed: brown normally, green 25% of the time.
Exits are `NEXT_BOSS` and `TOY_BOX`; ordinary dream exits are `DREAM` and `WAKE`.

## Boss rematches

The bedroom's boxing bell opens a scrollable roster of bosses already beaten in the
Flufficon. Move through it with the wheel, Up/Down or W/S, or the D-pad or left stick.
The selected boss's portrait is on the left and its rule is on the right. Each boss
keeps its rolled rule while the menu is open. The possible rules are Fast Forward,
Lights Out, Slow Reload and Small Blanks.

Click the controls on the right or move Right to choose a difficulty. Normal awards
a silver medal and Hard awards gold. Baby can practise without medals. Confirm starts
the rematch. Cancel, Esc or controller B returns to the bedroom.
Rematch difficulty is local to the attempt, and Retry keeps the boss, difficulty and rule.

The hero starts in a treasure room with one free chest before the boss arena. The chest
holds one random gun of its tier and takes no damage from shots. Each attempt, including
Retry, rolls a new chest using the selected boss's best medal.

| Best medal | Highest tier |
| --- | --- |
| None | B |
| Silver | A |
| Gold | S |

Normal and Practice use tier weights of 40, 30, 20, 8 and 2 from D through S. Hard uses
33, 31, 22, 10 and 4. Each roll uses only the weights through the medal's cap. The gun
pool includes the Flufful for rematches even before its permanent unlock.

The Dream rule takes effect in the arena. A clear waits for the entire encounter and
the boss death animation. The Flufficon keeps the best medal per boss. Rematches have
their own results screen and leave campaign saves, discoveries, unlocks and scores intact.

## Records and completion

`finishDream` updates dreams/rush counts and fastest times; `submitDaily` maintains
per-date best score. `dreamFastest` covers both bed and daily completions. Ending art is
`DreamEndingPicture`, with dream achievements rather than kingdom-win accounting.
Under the Bed reuses the carrier but explicitly skips dream completion records.

## Implementation

- [Dream](../src/main/java/com/trush/game/world/Dream.java)
- [BossRematch](../src/main/java/com/trush/game/world/BossRematch.java)
- [RematchMenu](../src/main/java/com/trush/game/screen/RematchMenu.java)
- [StoryDirector](../src/main/java/com/trush/game/world/StoryDirector.java)
- [StageMap](../src/main/java/com/trush/game/world/StageMap.java)
- [ETGFanfic](../src/main/java/com/trush/game/launcher/ETGFanfic.java)
- [DreamEndingPicture](../src/main/java/com/trush/game/screen/DreamEndingPicture.java)
- [AlarmClock](../src/main/java/com/trush/game/enemy/AlarmClock.java)
- [Nightmare](../src/main/java/com/trush/game/enemy/Nightmare.java)
- [BigTeddy](../src/main/java/com/trush/game/enemy/BigTeddy.java)

## Existing checks

- [DreamTest](../src/test/java/com/trush/game/world/DreamTest.java)
- [RushShopTest](../src/test/java/com/trush/game/world/RushShopTest.java)
- [DreamCatcherTest](../src/test/java/com/trush/game/save/DreamCatcherTest.java)
