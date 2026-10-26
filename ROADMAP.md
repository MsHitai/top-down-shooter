# Enter the Fluffy - roadmap

Each floor is a `world/Stage` with its own rooms, furniture, enemies, music and a pool of three bosses
(one is rolled per run, like ETG). The Fluffy King is the final boss: he lives in the Fluffy Kingdom, up in the clouds.

| Floor | Stage | Bosses | Status |
|---|---|---|---|
| 1 | The Village | Haymaker, Harvestman, Hollow Witch | done |
| 2 | The Town | Trash That Can, Road Rager (both designed by the owner's son), Traffic Light | done |
| 3 | The City (at night) | Night Hawk (helicopter), Pigeon King, Evil Train (designed by the owner's son) | done |
| 4 | The Skyscraper: an elevator climb, floor after floor, to the roof | Kaiju (a Dragun-style giant beyond the roof, drawn by the owner's son), Mega Mech, The Messenger of the Fluffy King (his sword's triangle volleys drawn by the owner's son) | done |
| 5 (final) | **The Fluffy Kingdom** (the owner's son's idea): the Fluffy King's home in the clouds - reached by helicopter from the skyscraper's roof | **The Fluffy King** | first version playable - see below |
| secret (the true ending) | **Space** (the owner's idea): the family builds a cardboard rocket at home, one piece per hero who beats the Fluffy King | **Grandfluff** - the Fluffy King's grandfather, three phases | planned - see below |
| secret | **The Deep Sea** (the owner's idea): a hole in the clouds, dive in with the Flippers | **The Fluffy Shark** | done (0.7.0) - see below |
| secret 1 | **The Slums** (a hidden way down from the city): narrow alleys, old acid, trap rooms and the acid gang | one of three: **the Blue Blob**, **the Walking Building** (both the owner's son's ideas) and **the Alley Cat** (the owner's son wanted a cat boss) | done (0.9.0) - see below |
| secret 2 (an ending) | **The Undercity**, under the Slums (through the sewers): the streets the False King rules | **The Skunk King** - the False King | done (0.9.0) - see below |

## Floor 5: The Fluffy Kingdom

The helicopter from the skyscraper's roof flies up into the clouds.

Done (first version): `Stage.CLOUDS`, floor 5 - cloud islands with open sky all round (the floor puffs out
at its edges), pearly gates at the start, a cloud temple, storm clouds, a rainbow's-end treasure room and
the Fluffy King's throne room with a pink carpet; cloud banks, marble columns, golden urns and presents for
furniture; holes in the clouds to fall through; doors plugged by clouds during a fight; its own music
("Silver Lining"); a rainbow to slide home on after the King. Enemies: the **Sword Angel** and the **Bow
Angel** (the owner's ideas) and the **Thundercloud**.

Still to do:

- **Linked by bridges or by hopping across** instead of plain doors.
- ~~Flying birds as moving obstacles~~ - done: during fights in the clouds a bird (sometimes a little
  flock) flies across after a warning arrow at the edge; touching one hurts, they're gone once off the cloud.

## Secret detour: The Slums and the Undercity - done (0.9.0)

Like ETG's Oubliette: a hidden way off the city leads down into the bad part of town, and further down to
the False King's streets - a **detour of two floors that ends the run in a second ending**.

**Up and down** (the owner's idea): the Slums are the Fluffy Kingdom turned upside down. The real Fluffy
King lives *above* everything, in the clouds; the False King hides *below* everything. The clouds are
bright, open and fluffy, the Slums dark, cramped and grimy. **The names**: the Slums (the owner's pick -
"ghetto"/"Fluffetto" were on the table, but that word carries real-world history that doesn't belong in
a kids' game) and, below it, the Undercity.

- **The way in** - a secret to find, not to buy, like ETG's fireplace in the Keep: one dead-end city alley
  has a burning oil drum in front of boarded-up planks. Put it out with water (the Water Pistol, the
  Bubble Blaster, the Snowball Slinger - the city shop always sells one if you have none, the owner's
  call: no getting stuck for lack of a gun), shoot the boards and take the steps down. Baa-llo drops
  rumours about it.
- **The Slums** (counted as floor 4, at night): long narrow alleys (the owner's pick, the opposite of the
  open clouds), the odd courtyard, old acid that never dries, its music "Back Alley Blues". The acid gang
  - the Sludger, the **Acid Sprayer** and the **Barrel Roller** - and, deeper in, **the marked**: critters
  changed by the Skunk King's stink, each with one mismatched green-yellow ear and glowing eyes
  (`enemy/Marked`) - marked rat packs and the **Stink Pigeon**. Two **trap rooms** a map: 20 seconds of
  rising acid or bursting pipes, with a gunner and rat packs (the owner found it too easy without).
  Bosses: **the Blue Blob** (splits in two, then four; "Gutter Groove"), **the Alley Cat** (on a sofa
  carried by goons, then he prowls and pounces) and **the Walking Building** (like ETG's Wall Monger; its
  long street is entered from the side, right beside it - no goons, the owner's calls).
- **The Undercity** (floor 5, the last of the detour): the Slums' boss leaves an open sewer hatch down to
  sewer tunnels and buried streets lit by glowing water, "Down Where the King Sleeps", Baa-llo's shop
  stays. Mostly the marked: the **Sewer Gator** (never hides - the owner found waiting for it dull), the
  **Alley Dog** (howls in rat packs) and the King's court, the **Raccoon** coin-thief, who got this job when the owner
  picked the skunk over the raccoon for the King.
- **The Skunk King**, the False King ("The False King's March"): on a shopping-cart throne - tail spray,
  perfume bottles, paper-plane decrees; at half health his crown falls and he panics, spinning and
  spraying. Beating him is the **normal ending**: the critters' green fades, the streets are quiet - but
  the real King still sulks in the clouds. The clouds stay the true ending, so it doesn't count as
  "kingdom saved".
- **Gas clouds** came with it: hot grey smoke (now also the Evil Train's) and the Skunk King's green stink;
  a roll goes through, a blank blows them away, at most 18 a room.
- Secret: none of its bosses turn up in endless.

Still open:

- **The detour in endless**: the detour's own enemies (all but the Sludger) and its bosses, once found,
  never come up in endless waves yet.
- **More trap rooms**: the lights go out; a garbage compactor whose walls close in.
- **More of the gang and the marked**: a sewer rat king.
- Spare ways in (if the drum ever needs company): a cracked wall only a blast opens; a manhole that
  rattles after three rolls over it; a graffiti arrow pointing at a fake wall. And an odd-looking manhole
  in the Slums as a shortcut or a bonus room.
- Stink clouds that roll along the floor.

## Boss ideas on file

- **The Mailman** (spare, no floor yet): runs laps around the room throwing curving envelopes and parcels that burst;
  enraged, a dog chases him too.

## Secret floor: Space - the true ending (planned)

The owner's idea: the whole family has to beat the Fluffy King before the way to space opens - building
it together, the way ETG has each character kill their own past.

**The rocket at home.** A cardboard rocket grows in the living room (`HubScreen`), one piece for each hero
who has beaten the Fluffy King in a story run with them (the Undercity's normal ending doesn't count). The
first win puts a big cardboard box there with a crayon porthole; then each hero adds theirs:

- Boy: the nose cone (a traffic cone or a party hat)
- Sister: fins cut like throwing stars, painted with her ninja sign
- Granny: a knitted scarf round the top, and a little space helmet for the cat
- Grandad: the engine - a bolted-on vacuum cleaner and his toolbox; it rattles and puffs a bit of smoke

Walking up to it says who has built it so far and how many are still to go. Once it's finished: "WE
SHOULD TRY IT SOMETIME... UP IN THE CLOUDS!" Needs a per-hero record of King wins (prefs), not just the
win count. Grandad only unlocks with the first win, so the order sorts itself out.

**The way there.** Once the rocket is finished, every story run: when the Fluffy King falls, the family's
rocket comes roaring in and lands on the clouds next to the rainbow exit - flown by Grandad, or by Granny
when you're playing as Grandad. Prompt: something encouraging like "WANT TO GO EVEN HIGHER?" / "BLAST OFF?".

**The floor.** The hardest floor yet. Ideas: low gravity (rolls go further, knocked enemies drift), little
planets whose gravity bends every bullet that flies past (yours too - `Bullet.curve`), drifting asteroids
as moving cover that can be shot apart; aliens, laser-grid satellites, comets with burning tails, maybe
space sheep (a wink at Baa-llo). The enemy line-up is a job for the owner's son.

**The boss: Grandfluff**, the Fluffy King's grandfather (the owner's idea), in his castle on the moon. A
good guy gone grumpy - and beating him is the true ending: an end to all the trouble (reruns are still
there, but this is where the story finishes). Three phases:

1. **The wise old king** - stays on his throne and draws constellations: stars appear, then light up into
   lines between them; slow, beautiful spirals; swipes of his walking stick. Elegant, readable patterns.
2. **Young again** - eats a bit of moon cheese / star dust, throws off his robe and gets young and quick
   (like the King's tantrum, but bigger): bounces round the arena in low gravity, belly flops, comets flung
   across the room.
3. **He becomes the moon** - grows so big only his face fits (like the Kaiju), the arena is his face:
   craters spit rings, his eyes sweep beams, and his pull drags you toward his mouth - keep moving.

The ending: the two kings making up, with our Grandad nodding beside them.

## Secret floor: The Deep Sea - done (0.7.0)

Every map of the Fluffy Kingdom hides one hole in the clouds with the sea below; with the Flippers (Baa-llo's
city shop) you dive in. Seabed, reef, kelp and trench rooms, a sunken ship for treasure, coral, rocks,
seaweed and giant clams for cover, sea mines; the Flufferfish, Starfish, Diver, Turtle and Swordfish (the
owner's son's line-up); the **Fluffy Shark** in its lair - fin guns, a stunning tail wave, lunging bites.
A waterspout takes you back up. Endless waves 26-30 flood the arena, wave 30 is the shark - once the player has found the sea in a story run.

Still open:

- Slow, drifting movement under water; bubbles from vents that push bullets up.
- Jellyfish that pulse rings; the shark calling in remoras, or showing only its fin before a lunge.

## Flufful - an unlockable friend (like ETG's Gunther) - done

The owner's all-time favourite ETG gun is Gunther, so the game gets its own take on it: **Flufful**,
a fluffy companion gun that talks while you fight. The name is Fluffy + Gundul - the owner always mixed
up Gunther with Gundul from God of War, so that's its backstory.

- Unlocked for good by beating the Fluffy Shark (in the secret sea, or at endless wave 30 once the sea has been found);
  from then on it can come out of B chests. Endless ammo.
- Talks: quips in a speech bubble about kills, reloads, bosses, the player getting hit, being put away.
- Grows stronger with kills: levels at 20, 60, 120 and 200 kills (damage, fire rate, bigger pom-poms).
