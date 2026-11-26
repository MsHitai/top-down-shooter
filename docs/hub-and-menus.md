# Home, title screen and options

[Documentation index](README.md)

## Title and first entry

`MainMenuScreen` draws its menu over `TitlePicture`, a moving road through village,
town, night city, skyscraper and the cloud castle. Unreached places appear misty with
question marks; new places sparkle once, with `titleSeen` tracking announcements.
The castle changes from stormy to bright/rainbow after a kingdom win. `titleWins` tracks
new kingdom-save messages; sea discovery adds a fin. The menu sits on the right beneath
the castle. The first New Game plays `IntroScreen`, then home; later Continue means home,
where the front door handles saved trips.

`IntroScreen` is a six-page picture book using game portraits, props and hero art with
typewriter text. Key/click/A turns a page; Esc/B/Start skips. The bookshelf can replay it.

## Hub spots

`HubScreen` lets the selected family member walk around depth-sorted furniture and
interact with a nearby `Spot` using E/A. The room is a fixed home view with its own movement code. Dodge rolling also works
here (Space/right mouse/LB), uses the selected hero's reach and Baby's double spin, and
still stops at walls/furniture. Interaction waits until the roll ends.

| Spot | Result |
| --- | --- |
| Front door | Continue saved trip, start new story, or stay home |
| TV | Endless, leaving a saved trip intact |
| Bookshelf | Replay intro and return home |
| Trophy shelf | Flufficon and records |
| Family members | Choose a playable hero |
| Cardboard rocket | Builder progress and completion feedback |
| Skull, after the first regular Fluffy King win | Toggle hard mode or open the Story dare cards |
| Console, after rocket boarding | Egor's rocket score chase |
| Bedroom bed / calendar / toy box | Dream / daily / boss rush |
| Open under-bed entrance | Short hero-specific trip |
| King's shrinking potion | Play as pocket King |

The bedroom is right of the living room after qualifying progress; a one-time card
announces it and dream stars decorate the room. The King's room is on the left once all
four builders have crowns, with a full-size happy King on a throne and potion table.
His move-in has a one-time announcement. See [progression](progression.md) for prerequisites.

Rocket art stacks the box with Boy's cone, Sister's fins, Granny's scarf/cat helmet and
Grandad's engine. Completed rockets rattle and smoke. Home uses `home.png`/`home.txt`:
baked room art plus free-standing furniture/hero, rather than world prop collision.

## Options and shared menus

`MenuList` handles keyboard, mouse and pad menu navigation. `OptionsMenu` overlays title
or pause with music/sound volume, fullscreen, controls and records. Wear Crowns appears
once earned crowns exist and only hides their art. Reset Progress is title-only, asks
for confirmation, and keeps options while clearing progress. The game owns the shared
font and volume preferences; each track's own volume is multiplied by the music option.

Runs return home through `GameScreen.toMenu`; the name is historical and does not imply
return to the title screen. UI state/camera/HUD details are in [screens and interface](screens-and-interface.md).

## Implementation

- [MainMenuScreen](../src/main/java/com/trush/game/screen/MainMenuScreen.java)
- [TitlePicture](../src/main/java/com/trush/game/screen/TitlePicture.java)
- [IntroScreen](../src/main/java/com/trush/game/screen/IntroScreen.java)
- [HubScreen](../src/main/java/com/trush/game/screen/HubScreen.java)
- [MenuList](../src/main/java/com/trush/game/screen/MenuList.java)
- [OptionsMenu](../src/main/java/com/trush/game/screen/OptionsMenu.java)
- [ETGFanfic](../src/main/java/com/trush/game/launcher/ETGFanfic.java)
