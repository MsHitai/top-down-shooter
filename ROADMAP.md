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
| secret 1 | **The Bad District** (a detour off the city): trap rooms and acid gunners | one of three: **the Alley Cat Boss** (the owner's son wanted a cat boss), **The Walking Building** and **the Blue Blob** (both the owner's son's ideas) | idea - see below |
| secret 2 (an ending) | **The Bad District, deeper down**: the streets the False King rules | **The Raccoon King** - the False King | idea - see below |

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

## Secret floor: The Bad District - idea

Like ETG's Oubliette (the sewers under the Gungeon, all poison goop): a hidden way off the city leads into
the bad part of town. Graffiti, burnt-out cars, broken street lamps, puddles of acid everywhere.

The plan (agreed with the owner): a **detour of two floors that ends the run in a second ending**.

- **The way in**: from the city, a manhole that needs something - a gas mask from Baa-llo, like the
  Flippers for the sea.
- **District floor 1**: one boss rolled from three, like a story floor - **the Alley Cat Boss**, **the
  Walking Building** and **the Blue Blob**.
- **District floor 2**: the False King's streets; its boss is **the Raccoon King**, and beating him ends
  the run in a **"normal ending"**, like a VN's: the streets are quiet, but up in the clouds the real
  Fluffy King still sulks - a last picture that hints there's more. The clouds stay the "true ending",
  so the detour skips the skyscraper and the clouds and has to be about as hard as they are.
- **Not in endless**: the district's bosses are secret, so endless boss waves never roll them
  (`Boss.pickAny` must skip them).

- **Trap rooms**: small rooms with pits, rooms that fill with acid, flames from burst pipes, rooms whose
  doors lock with no enemies - just traps to survive for a while.
- **Acid gunners**: the **Sludger** (done - a hazmat goon lobbing acid globs; for now a rare guest in the
  town, the city and the skyscraper) and more of its gang: a sprayer that hoses a sweeping arc, a barrel
  roller whose drums burst into pools, a sewer rat king.
- The Acid Boots (done) pay off down here; Baa-llo's city shop hints at the way in, like the Flippers.

### Its bosses

- **The Alley Cat Boss** (floor 1; the owner's son wanted a cat boss): a big smug ginger tomcat with a
  torn ear, a fur coat, gold chains and sunglasses, carried in on a sofa by his goons - a gangster king,
  not a fairy-tale one. Ideas: claw-swipe crescents, a hairball that bursts, his goons dropping the sofa
  and joining in; enraged, he leaps off the sofa and pounces.
- **The Walking Building** (floor 1; the owner's son's idea): a skyscraper on legs - windows open to
  fire, rooftop antenna, enemies leaving through the front door.
- **The Blue Blob** (floor 1; the owner's son's idea, after ETG's Blobulord): a big wobbly blue blob. At
  75% of its health it splits into two blobs, at 50% each of those splits again - four blobs. Every blob,
  big or small, has the same attacks (smaller ones just have less health and fire smaller patterns):
  shooting blobby volleys at the player, and a happy bounce - it jumps up joyously and, as it lands, bullets
  fly out in a ring around the room. The fight is won when the last blob pops.
- **The Raccoon King, the False King** (floor 2, the ending): a fat scruffy raccoon - the bandit's mask
  suits an impostor - in a cardboard crown painted gold (the paint has run), an old shower curtain for a
  royal cape and a plunger for a sceptre, on a throne of trash bags and a shopping cart. Ideas: "royal
  decrees" (paper planes folding into bullet patterns), tipping his throne to roll acid barrels, calling
  in his court of Sludgers; phase two: his crown falls off, he panics and goes wild digging through the
  trash for anything to throw.

## Boss ideas on file

- **Pigeon King** (City): a fat pigeon with a bottle-cap crown on a statue; flocks swoop across the room in
  lines, breadcrumbs scatter and turn into bullets; enraged, it leaves the statue and flaps around.
- **The Mailman** (spare, no floor yet): runs laps around the room throwing curving envelopes and parcels that burst;
  enraged, a dog chases him too.

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
