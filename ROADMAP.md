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
| secret 1 | **The Slums** (a hidden way down from the city): trap rooms and acid gunners | one of three: **the Alley Cat Boss** (the owner's son wanted a cat boss), **The Walking Building** and **the Blue Blob** (both the owner's son's ideas) | idea - see below |
| secret 2 (an ending) | **The Undercity**, under the Slums (through the sewers): the streets the False King rules | **The Skunk King** - the False King | idea - see below |

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

## Secret floor: The Slums - idea

Like ETG's Oubliette (the sewers under the Gungeon, all poison goop): a hidden way off the city leads down
into the bad part of town. Graffiti, burnt-out cars, broken street lamps, puddles of acid everywhere.

**Build order** (agreed with the owner): 1. the floor itself, 2. the way in, 3. its enemies, 4. trap rooms,
5. the bosses one at a time (the Blue Blob first), 6. the Undercity and the Skunk King.

**Step 1 done - the floor** (`Stage.SLUMS`, dev builds: `-Dfluffy.slums=true`): a secret floor counted as
floor 4 (`StoryDirector.SLUMS_FLOOR`, as hard as the skyscraper), at night, no floor number in its title.
Rooms are mostly long narrow alleys (the owner's pick: cramped, the opposite of the open clouds) - 29x8
left-right or 11x20 up-down, junk piled along both walls and a lane down the middle
(`RoomTemplates.alley`) - sometimes a courtyard (19x12) or a small trap room with open hatches
(`StageMap.slumsArena`). Old acid on the floor (the owner said yes): 2-4 pools in every alley and
courtyard that never dry up (`Room.acidPools`, `Puddle.old`). Themes backalley, slumstreet, underpass,
junkyard, hideout (treasure), emptylot (boss), store_slums; furniture dumpsters, burnt-out cars, rusty
oil drums, trash bags and street lamps; boarded-up doors; the exit is a sewer hatch. Music "Back Alley
Blues". Enemies for now: Sludgers (two a room at most), rat packs, robbers, dynamite, shell cops, shotgun
kin. Until its own exist: the city's bosses stand in, and the hatch leads on to the clouds (floor 5).

**Step 2 done - the way in**: every city map picks one combat room with no top door (a roomy dead end if
there is one; `Room.slumsWay`): planks across a doorway in the middle of its top wall (`Prop.Kind.BOARDS`)
and a burning oil drum in front (`FIRE_DRUM`, flames and a wisp of smoke, it glows at night). Neither
breaks while it burns; three hits of water (`Weapon.wet` / `Bullet.wet`: the Water Pistol, the Bubble
Blaster, the Snowball Slinger) put the fire out with a hiss, then the boards can be shot through and the
steps lead down (`GameWorld.enterSlums`: the city is left behind for good, like ETG's Oubliette; its chest
pools carry on). The city shop always sells a water gun in its first gun slot when the player has none,
and Baa-llo drops rumours on coming back in ("I HEAR RUMBLING UNDER THE CITY...").

**The names**: **the Slums** (the owner's pick - "ghetto"/"Fluffetto" were on the table, but that word
carries real-world history that doesn't belong in a kids' game) and, below it, **the Undercity**.

**Up and down** (the owner's idea): the Slums are the Fluffy Kingdom turned upside down. The real Fluffy
King lives *above* everything, in the clouds; the False King hides *below* everything - the further you go
down, the closer you get to him. The road down mirrors the road up: the clouds are bright, open and
fluffy, the Slums dark, cramped and grimy.

The plan (agreed with the owner): a **detour of two floors that ends the run in a second ending**.

- **The way in - a secret to find, not to buy** (unlike the Flippers: no special key item from Baa-llo).
  Like ETG's fireplace in the Keep (put out its fire and a secret door opens to the Oubliette), the city
  hides a secret passage (agreed with the owner):
  - **A burning oil drum** in one city combat room (a dead-end alley), against a boarded-up wall. Put the
    fire out with water - the Water Pistol, the Bubble Blaster (maybe the Snowball Slinger) - and the
    boards behind it can be shot through: steps down into the dark. One per city map, like the sea's hole;
    a wisp of smoke is the only hint.
  - **Never stuck without water** (the owner's call: depending on a gun you happen not to carry is bad
    design, and a special water bottle would be too much like ETG): the city shop always stocks one of the
    water guns, in an ordinary gun slot at an ordinary price - it doesn't stand out, but it's always there
    to buy. (If the player already has one, the slot is a normal roll.)
  - Baa-llo might still drop a rumour ("I heard something rumbling under the city...") - a hint, not a key.
  - Spare ideas: a cracked wall only a blast opens; a manhole that rattles after three rolls over it; a
    graffiti arrow pointing at a fake wall.
- **Slums floor 1**: one boss rolled from three, like a story floor - **the Alley Cat Boss**, **the
  Walking Building** and **the Blue Blob**.
- **Slums floor 2: the Undercity** (deeper down, through the sewers - the owner's son's idea): the
  False King's streets. **The way down** (agreed with the owner): after the Slums' boss, the floor's exit
  is an open sewer hatch (like the other stages' gate, bus stop, helicopter) - the Slums were secret
  enough to find, so the detour simply carries on. (Spare: an odd-looking manhole in the Slums as a
  shortcut or a bonus room.)
  The Undercity could be half sewer tunnels (the way to the King, his court of rats), half streets
  underground. Its boss is **the Skunk King**, and beating him ends
  the run in a **"normal ending"**, like a VN's: the green fades from the marked critters and they're
  just critters again, the streets are quiet - but up in the clouds the real Fluffy King still sulks, a
  last picture that hints there's more. The clouds stay the "true ending",
  so the detour skips the skyscraper and the clouds and has to be about as hard as they are.
- **Not in endless**: the Slums' bosses are secret, so endless boss waves never roll them
  (`Boss.pickAny` must skip them).

- **Trap rooms**: small rooms with pits, rooms that fill with acid, flames from burst pipes, rooms whose
  doors lock with no enemies - just traps to survive for a while.
- **The marked** (agreed with the owner): the Undercity's monsters are ordinary sewer and street critters
  changed by the Skunk King's stink - he made it, and it's what makes them his. Not disgusting, just
  clearly *wrong*: every one has **one mismatched sickly green-yellow ear** and **eyes glowing the same
  colour** (the eyes carry it in the dark; an ear alone is only a few pixels at our size). One shared
  drawing helper puts the mark on all of them, like the angels' wings and halo in `enemy/Angel`. The
  Slums are people-gangsters (Sludgers, the Alley Cat's goons) with **a few marked critters** mixed in
  as a hint of what's below; the Undercity is mostly the marked. Ideas: a marked rat pack (the town's
  Rats gone wrong), a pigeon spitting stink pellets, a sewer gator bursting out of the water, an alley
  dog whose howl calls the others.
- **Acid gunners**: the **Sludger** (done - a hazmat goon lobbing acid globs; also a rare guest in the
  town, the city and the skyscraper) and more of its gang - **step 3 done**: the **Acid Sprayer** (hoses a
  sweeping arc of acid, its span shown on the floor first) and the **Barrel Roller** (bowls rusty drums
  that burst into a pool and a ring of drops; shoot one on its way to pop it early; rubber boots, so his
  own acid doesn't get him). Still an idea: a sewer rat king.
- **Marked critters in the Slums** (step 3 done, deeper rooms only, a hint of the Undercity): marked rat
  packs (quicker, tougher) and the **Stink Pigeon** (flies, spits stink pellets that burst into green gas -
  the first use of the Skunk King's stink). The shared mark is `enemy/Marked`.
- The Acid Boots (done) pay off down here.

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
- **The Skunk King, the False King** (floor 2, the ending; the owner's pick over the raccoon): a fat
  scruffy skunk - his white stripe looks like a cheap royal sash - in a cardboard crown painted gold (the
  paint has run), an old shower curtain for a cape and a plunger for a sceptre, on a throne of trash bags
  and a shopping cart. The real King is fluffy and lives above everything; the fake one stinks and lives
  below. His stink is his own invention - the gas that marks the Undercity's critters. Attacks:
  - **Tail spray**: the tail goes up and shakes (the warning, like the Sniper's lock), then a cone of
    stink that leaves a gas cloud hanging there.
  - **Perfume bottles**: lobbed like the Egg Lobber, they burst into a bullet ring and a small cloud.
  - **Royal decrees**: paper planes folding into bullet patterns.
  - Phase two: his crown falls off, he panics and sprays while spinning round, so clouds slowly fill
    the arena and herd you into the gaps.
  - Keep it fair: only a few clouds at once, and they fade - a room full of fog would hide the bullets.
  - The raccoon isn't lost: one of his court - a sneaky coin-stealer, or a sewer mini-boss.

### Gas clouds (new hazard, for the Skunk King)

Done: `objects/GasCloud` (kinds SMOKE and STINK), `GameWorld.releaseGas`. A soft puff that drifts and
slows down, then thins away. Grey smoke is hot: it sets you alight like flames (the flame warning). The
green stink fills the acid drop icon (the same timer as acid puddles, but the Acid Boots don't help). A dodge roll goes through safely, a blank blows the clouds away, enemies' gas
never hurts enemies (the marked critters are his), and the room holds at most 18 at once. The Evil
Train's smoke is real grey smoke clouds now instead of hard round bullets (the owner's call).

Still to do: the green-yellow STINK for the Skunk King - maybe clouds that roll along the floor.

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
