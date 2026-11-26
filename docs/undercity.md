# Undercity and the False King ending

[Documentation index](README.md)

The Slums boss exit descends to `Stage.UNDERCITY`, an explicit destination stored in
`RunSave.stage`. Its effective floor is 5, but it is off the main road. Underground
geometry reuses long alleys and tunnels, side-entered boss rooms, junk and glowing sewer-water
pools. A [timed trap room](slums.md#trap-rooms) can appear on 10% of maps. Baa-llo has a shop.
Themes are sewergate, sewer, buriedstreet, cistern, hoard, throneroom and store_undercity.

## Enemies

Marked rats, Stink Pigeons and some acid gang return. Sewer Gator visibly crawls and
bites, spits fans and thrashes a ring after bubbles; it never hides. Alley Dog is a
large marked critter: telegraphed lunges end in tooth fans, and limited howls emit a
sound ring and summon marked rats. Raccoon belongs to the King's court rather than
the marked family: it steals coins and flees, can escape without kill score, and drops
the stolen coins plus four when killed. Its touch hurts only when the hero has no coins.

`Marked` draws the common green ear/eyes and supplies the fading mark for the ending.
Stable dex IDs distinguish marked and ordinary critters in the Flufficon.

## Skunk King

The False King starts on a shopping-cart throne. A warned tail cone emits stink clouds
and pellets; perfume bottles burst into rings and gas; paper decrees unfold into plus/x
lines. Below 50% his crown falls and remains drawn on the floor; he tumbles out into a
wild phase of spiral stink, junk and bottles. His music is `boss_undercity`.

## Ending and continuation

Winning produces `NormalEndingPicture`: the critters' marks fade and the streets are
quiet, while the real King remains above. `addFalseKingBeaten` updates its own counter;
it does not unlock Grandad or build a rocket piece. Baby results go to the separate
baby win counter.

The victory menu's `FIND THE REAL KING!` carries the same loadout and run stats to the
skyscraper as floor 6, then clouds as floor 7. A later King victory counts normally.
Do not derive the destination or win condition from `floor == 5`; use explicit stage
and `endsTheRun`. This continuation also permits a run visiting every campaign stage
for Grand Tour. See [campaign](campaign.md), [saving](saving.md), and [progression](progression.md).

## Implementation

- [StoryDirector](../src/main/java/com/trush/game/world/StoryDirector.java)
- [Stage](../src/main/java/com/trush/game/world/Stage.java)
- [RunSave](../src/main/java/com/trush/game/save/RunSave.java)
- [SewerGator](../src/main/java/com/trush/game/enemy/SewerGator.java)
- [AlleyDog](../src/main/java/com/trush/game/enemy/AlleyDog.java)
- [Raccoon](../src/main/java/com/trush/game/enemy/Raccoon.java)
- [SkunkKing](../src/main/java/com/trush/game/enemy/SkunkKing.java)
- [Marked](../src/main/java/com/trush/game/enemy/Marked.java)
- [NormalEndingPicture](../src/main/java/com/trush/game/screen/NormalEndingPicture.java)
- [GameScreen](../src/main/java/com/trush/game/screen/GameScreen.java)

## Existing checks

- [UndercityTest](../src/test/java/com/trush/game/world/UndercityTest.java)
- [UndercityCrittersTest](../src/test/java/com/trush/game/enemy/UndercityCrittersTest.java)
