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
| secret | Space | ? | idea - a hidden way in, like ETG's secret floors |
| secret | **The Deep Sea** (the owner's idea): a hole in the clouds, dive in with the Flippers | **The Fluffy Shark** | done (0.7.0) - see below |
| secret boss | (hidden in the City) | **The Walking Building** (the owner's son's idea): a skyscraper that gets up, walks and shoots | idea |

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

## Boss ideas on file

- **Pigeon King** (City): a fat pigeon with a bottle-cap crown on a statue; flocks swoop across the room in
  lines, breadcrumbs scatter and turn into bullets; enraged, it leaves the statue and flaps around.
- **The Mailman** (spare): runs laps around the room throwing curving envelopes and parcels that burst;
  enraged, a dog chases him too.
- **The Walking Building** (secret): a skyscraper on legs - windows open to fire, rooftop antenna, maybe
  people/enemies leaving through the front door.

## Secret floor: The Deep Sea - done (0.7.0)

Every map of the Fluffy Kingdom hides one hole in the clouds with the sea below; with the Flippers (Baa-llo's
city shop) you dive in. Seabed, reef, kelp and trench rooms, a sunken ship for treasure, coral, rocks,
seaweed and giant clams for cover, sea mines; the Flufferfish, Starfish, Diver, Turtle and Swordfish (the
owner's son's line-up); the **Fluffy Shark** in its lair - fin guns, a stunning tail wave, lunging bites.
A waterspout takes you back up. Endless waves 26-30 flood the arena, wave 30 is the shark.

Still open:

- Slow, drifting movement under water; bubbles from vents that push bullets up.
- Jellyfish that pulse rings; the shark calling in remoras, or showing only its fin before a lunge.

## Flufful - an unlockable friend (like ETG's Gunther) - done

The owner's all-time favourite ETG gun is Gunther, so the game gets its own take on it: **Flufful**,
a fluffy companion gun that talks while you fight. The name is Fluffy + Gundul - the owner always mixed
up Gunther with Gundul from God of War, so that's its backstory.

- Unlocked for good by beating the Fluffy Shark (in the secret sea, or at endless wave 30);
  from then on it can come out of B chests. Endless ammo.
- Talks: quips in a speech bubble about kills, reloads, bosses, the player getting hit, being put away.
- Grows stronger with kills: levels at 20, 60, 120 and 200 kills (damage, fire rate, bigger pom-poms).
