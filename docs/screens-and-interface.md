# Run screens, camera, HUD and presentation

[Documentation index](README.md)

`GameScreen` owns playing, paused, game-over, stage-clear and victory states. It mediates
input, whether world time advances, music crossfades and menu transitions. `GameWorld`
remains responsible for simulation; directors decide mode completion and encounter text.

## Camera and draw layers

The normal camera follows the player, leans toward the aim and toward a boss, while
keeping the hero visible and respecting room edges. Boss `viewZoom` can widen the view.
Sky/roof/Moon stages allow the camera to see beyond room edges for their backgrounds.
The HUD uses a fixed 7×5 screen camera; prompts, pad reticle and other world overlays
use the world camera. Do not place a world prompt with HUD coordinates.

Boss introductions glide to the boss, pause the world for `BossCard`, then glide back.
On death the camera follows the boss through the final blast, then immediately starts
returning to the hero. Grandad's line starts one second after he's back in view and the
camera has nearly reached its normal follow position. An elevator transition stops the
world and shows closed doors plus a changing storey counter between tower rooms.

## Combat HUD

The HUD includes layered red/gold hearts, shield/helmet state, blanks, coins/keys,
weapon/ammo, passive icons and active item/cooldown. The active box is bottom-left;
backpack swapping and bagged cooldown are modeled by Player. `RELOAD` appears when a
magazine is nearly empty and `EMPTY` when ammunition is gone. Long Shot has a cover-limited
laser sight. Charged Egg Lobber and Grenade share a landing marker; Slingshot shows pull.
Hazard icons fill before fire/acid damage and frost displays buildup near the hero.

If every surviving enemy has been off screen for 1.2 s, edge arrows point toward the
lost enemies. Boss-specific incoming attacks can have their own edge warnings. These
indicators should not change simulation or substitute for correct navigation.

## Maps and overlays

Minimap and big map share the room renderer. The big map pauses and enables visited-room
teleport, but is blocked during a fight. Its input must not accidentally remain held after
other overlays. Loot icons distinguish tier-colored chests, keys, other pickups and exits.
Guns on the pad wheel overlap; the selected card is drawn last and larger.

Director banners show stage/wave/room events; `hudNote` keeps ongoing information such
as trap survival countdowns and dream rules visible. Keeper and weapon dialogue use
speech bubbles. `AchievementBanner` is drawn by the game over any screen.

## Results and endings

Game over and victory offer try/play again, go home or exit. Stage clear can go on or
save an eligible campaign trip. Rocket, dream and rush clears use specialized labels.
`EndingPicture` depicts the kingdom restored; `NormalEndingPicture` depicts the False
King route; `TrueEndingPicture` reconciles the kings on the Moon; `DreamEndingPicture`
wakes the family. Under the Bed has its own return text and hub position. End-state
menus must use the mode's accounting and save rules; see [campaign](campaign.md) and
[saving](saving.md).

## Implementation

- [GameScreen](../src/main/java/com/trush/game/screen/GameScreen.java)
- [BossCard](../src/main/java/com/trush/game/screen/BossCard.java)
- [AchievementBanner](../src/main/java/com/trush/game/screen/AchievementBanner.java)
- [EndingPicture](../src/main/java/com/trush/game/screen/EndingPicture.java)
- [NormalEndingPicture](../src/main/java/com/trush/game/screen/NormalEndingPicture.java)
- [TrueEndingPicture](../src/main/java/com/trush/game/screen/TrueEndingPicture.java)
- [DreamEndingPicture](../src/main/java/com/trush/game/screen/DreamEndingPicture.java)
- [Director](../src/main/java/com/trush/game/world/Director.java)
