# ENTER THE FLUFFY

A top-down bullet-hell shooter in the spirit of *Enter the Gungeon*, written in Java 21 with libGDX
(LWJGL3 desktop backend) and built with Maven. Keyboard + mouse or an Xbox controller.

### How to run

    mvn clean package
    java -jar target/game-0.10.0-jar-with-dependencies.jar

or run `com.trush.game.launcher.AppLauncher` from the IDE.

### Modes

The title screen's picture shows the road of the story - each place appears out of the mist once you've
been there. **NEW GAME** opens with the story of how it all began (a picture book - skippable), then you're at
home in the family's living room; after that the title screen says **CONTINUE** and takes you straight
home. Walk around the room: the **front door** goes outside (a story run), the **TV** plays endless, the
picture book on the **bookshelf** tells the story again and the **trophy shelf** shows your records.
The **family** stands about the room too - walk up to one to play as them:
- **the boy** - three hearts and the Rusty Sidearm
- **his sister**, a ninja wannabe - rolls further, throws stars, but only two hearts
- **granny** - her cat pounces on enemies and brings her presents after fights; she walks a bit slower
  and carries her old pearl-handled revolver
- **grandad** - brings his old double-barrel shotgun, and every gun he gets is +1, but his roll is shorter. He'll only come along once the Fluffy
  King has been beaten ("maybe another time, kiddo").

**OPTIONS** (on the title screen and in the pause menu) has music and sound volume, fullscreen and the
controls; on the title screen it can also reset all progress.

- **Story**: floor after floor, each a new random map. Walking into a combat room locks the doors until
  every enemy is dead. Find chests, keys and the treasure rooms (two on every floor but the skyscraper), then beat the floor's boss and take
  the way on it opens (a gate, a bus stop, the tower's doors, a helicopter). Like ETG you can only save
  **between floors** (on the floor-clear screen); going out the front door again picks the run up on the next floor.
  1. **The Village** - cottages, yards, barns, orchards
  2. **The Town** - streets, a market, alleys, a park, open sewer hatches to fall into
  3. **The City** - at night: avenues, a subway, rooftops, a construction site; lamps light the dark
  4. **The Skyscraper** - an elevator climb: offices, server rooms, a cafeteria, floor after floor (each
     bigger and busier than the last) up to the roof, where a giant waits; a helicopter flies you up into the clouds
  5. **The Fluffy Kingdom** - cloud islands in the open sky: pearly gates, a cloud temple, storm clouds,
     holes to fall through, and the Fluffy King on his throne. Beat him to win the run.

  ...and they say there's something far below the clouds.
- **Endless**: one arena, waves forever. Each stage's enemies join in turn, and every 5th wave is a boss
  wave that climbs the stages (wave 5 a village boss, 10 town, 15 city, 20 skyscraper, 25 the Fluffy King -
  and if you last beyond that, you'll see). A boss wave heals you, gives a heart container and an item, and new furniture replaces
  what was shot to pieces. Then Baa-llo sets up his travelling shop in the arena: ring the bell on his
  counter when you're ready for the next wave.

Rooms come in sizes: small one-screen rooms (in the town and city: "trap" rooms full of pits), roomy
combat rooms and big open boss arenas - the camera follows you and leans toward where you aim.

### Controls

| Keyboard + mouse | Xbox controller | Action |
| --- | --- | --- |
| WASD / arrows | left stick | move |
| mouse | right stick | aim |
| hold left button | RB | shoot |
| Space / right button / Left Shift | LB | dodge roll (invincible for most of it; roll over tables, squish rats) |
| E | A | open a chest / take the exit / flip a table |
| R | X | reload |
| Q | R3 (click the right stick) | blank: clears every enemy bullet and dazes enemies for a moment |
| F | RT | use your active item (med kit, candy; hold to throw the grenade further) |
| wheel / Tab / 1-9 | tap Y / hold Y | switch gun / gun wheel |
| hold M | hold LT | map: teleport to any room you've visited (not during a fight) |
| Esc / P | Start | pause |
| N | | toggle music |

### What's in it

- **Guns** - start with the Rusty Sidearm; the rest come out of chests and Baa-llo's shop, from tier D
  up to A: confetti, bubbles, snowballs, popcorn, peas, a slingshot, a hair dryer, a kazoo, boomerangs, bees, pillows, rockets, rainbows, flares, acid, bombs, a biting shark, a
  frying pan and stranger things. Finding a gun you
  already have upgrades it instead ("+1").
- **Chests and keys** - brown, blue, green and red chests (D-A), and very rarely a black one (S) with
  one of the best guns in the game; a chest holds a gun or an item of its
  tier, or else hearts, ammo, blanks and keys. Locked chests cost a key; each floor guarantees one.
- **Coins and Baa-llo's shop** - enemies, cleared rooms, chests and bosses drop coins (like ETG's shell
  casings). Every floor has a shop (a coin on the map) run by **Baa-llo**, a woolly sheep in a red cap
  and a monocle: two guns or items, a heart, a key and a blank or an ammo box, one of them on sale (on
  the first floor there's always a cheap gun). Walk up to a stand to see the price, press E / A to buy. Guns and items cost 25 / 45 / 70 / 100 coins by tier
  (D-A), hearts and blanks 15, ammo 20, keys 30, and 10% more each floor. Don't shoot the shopkeeper:
  three hits and he holds a grudge - prices up by half for the rest of the run.
- **Items** - passive ones that change how you play (faster feet, hotter bullets, a shield, a force field,
  a bike helmet, bullets that freeze, burn, bounce or sniff out enemies, pom-poms circling you, a little cloud friend, a piggy bank
  and more) and one active item at a time, used with F / RT (a med kit, a grenade, a packed lunch, a
  wind-up toy robot, a bubble wand...). The rarest come in black chests. They come from
  chests and the shop, each at most once a run.
- **Hearts** - half hearts drop now and then; **heart containers** (+1 max heart) come from chests, the
  Fluffy Heart, endless boss waves, or beating a boss **without taking a single hit**. Up to 16 hearts:
  past 8 they turn golden and sit on top of the red ones.
- **Fire and acid** - a dynamite leaves the floor burning, some guns set enemies on fire or eat at them
  with acid, and some enemies spit acid that pools on the floor. Walk into flames and you catch fire;
  stand in acid and it bites. A little flame or acid drop beside your head fills up before it hurts. A
  dodge roll passes over both unharmed - and rolling puts the flames out.
- **Ice** - snowflakes don't hurt, but enough of them freeze you in a block of ice: mash the buttons to
  break out. Frozen enemies are brittle and shatter into flying shards, and ice on the floor is slippery
  for everyone.
- **Cover** - flip tables, hide behind pillars, barrels, cars; most furniture can be shot to pieces.
  Cover only protects you in the direction it really covers.
- **Enemies** - village: Bullet Kin, Shotgun Kin, Bulletmancer. Town: Hydrant, Rats, Shell Cop,
  Shapeshifter. City: Biker, Sniper, Robber (dodge-rolls your bullets). Skyscraper: RoboCart, Android,
  Agent, Mountain and Lead Cubes. Fluffy Kingdom: Sword Angel, Bow Angel, Thundercloud (and birds to
  dodge). Everywhere: the Dynamite (shoot it before it reaches you - it's the blast that hurts). Now and then, from the town on, a
  Sludger in a hazmat suit lobbing globs of acid. And, very rarely, a street lamp
  that isn't a lamp at all.
- **Bosses** - one per floor, rolled from the stage's three:
  - Village
    - **the Haymaker** - a scarecrow who's had enough of standing in a field
    - **the Harvestman** - a hulking masked farmhand with a chainsaw, straight out of a horror movie -
      and he comes for you
    - **the Hollow Witch** - the witch from the campfire stories, on her broomstick, with jack-o'-lanterns
      up her sleeve
  - Town
    - **the Trash That Can** - a hopping, furious bin that throws its rubbish at you (designed by the
      owner's son)
    - **the Road Rager** - a monster truck with a cannon on the roof and friends inside (designed by the
      owner's son)
    - **the Traffic Light** - plays "Red Light, Green Light", and you'd better stop on red
  - City
    - **the Night Hawk** - a helicopter with a searchlight that won't let you out of its sight
    - **the Pigeon King** - a fat pigeon with a bottle-cap crown, perched on his statue in the plaza
    - **the Evil Train** - goes round and round its own track, and the wagons hurt too (designed by the
      owner's son)
  - Skyscraper, up on the roof
    - **the Kaiju** - a giant climbing up over the roof's edge, like ETG's Dragun (drawn by the owner's son)
    - **the Mega Mech** - a giant security robot that jumps much higher than anything that heavy should
    - **the Messenger** - the Fluffy King's herald, riding a cloud with a magic sword (its triangle
      volleys drawn by the owner's son)
  - the Fluffy Kingdom
    - **the Fluffy King** - the final boss: he got out of bed on the wrong side, and now the whole kingdom
      is shooting

  When a boss appears the camera goes over to it for its introduction card; when it goes down the camera
  watches it fall, then comes back to you.

The owner's son also came up with the Fluffy Kingdom and the Walking Building - see
[ROADMAP.md](ROADMAP.md) for what's next (spoilers in there too).

<details>
<summary><b>Spoilers: the secret floors</b> (don't open if you'd rather find them yourself)</summary>

- **The Deep Sea** (the owner's idea) - every map of the Fluffy Kingdom hides one hole in the clouds that
  looks down on the sea, with a shark fin in it. Baa-llo's city shop sells the **Flippers** (65 coins;
  every dodge roll sends bubbles after nearby enemies) - wear them and stop on that hole to dive in.
  Without them it's just a fall. Down there: a seabed, coral reefs, kelp forests, a trench, a sunken ship
  full of treasure and sea mines that blow when anyone comes close. A waterspout takes you back up to
  the room you dived from.
- **Sea creatures** (picked by the owner's son): the Flufferfish, the Starfish, the Diver, the Turtle and
  the Swordfish.
- **the Fluffy Shark** - the boss down there: the shark from the Flufful, only huge, deep blue and very
  angry. Beating it unlocks **the Flufful** for good: a talking gun like ETG's Gunther - a fluffy pink
  pistol with a sweet shark on top (drawn by the owner) that chats between fights, never runs out of
  ammo and grows with every kill. From then on it can turn up in tier B chests.
- **Endless**: once you've dived into the sea in a story run, waves 26-30 flood the arena and bring the
  sea creatures; wave 30 is the Fluffy Shark (it unlocks the Flufful too). Until then the shark stays
  out of endless. After that, any boss can turn up.
- **The Slums** - one dead-end alley in every city map has a burning oil drum in front
  of a boarded-up doorway. Put the fire out with water (the Water Pistol, the Bubble Blaster or the
  Snowball Slinger - Baa-llo's city shop always has one if you don't), shoot the boards through and go
  down the steps: the bad part of town, all narrow alleys and old acid, instead of the rest of the city.
  The Sludger's whole gang lives there - the Acid Sprayer hosing arcs of acid, the Barrel Roller bowling
  drums that burst into pools (shoot them on the way!) - and, deeper in, critters with one odd green ear
  and glowing eyes: marked rats, and stink pigeons that spit clouds of stink. Two rooms on every map are
  traps: the doors lock and you survive 20 seconds of rising acid or bursting pipes, with a gunner and rat
  packs chasing you into them.
  Its bosses (one per trip): **the Blue Blob** (the owner's son's idea) - a big happy blob that fires
  gooey volleys and bounces with joy, splashing rings of drops; at three quarters of its health it splits
  in two, at half each of those splits again - pop all four! - and **the Alley Cat** (the owner's son
  wanted a cat boss): a smug ginger tomcat in shades, a fur coat and gold chains, carried in on a sofa by
  his goons, swiping crescents of claws and coughing up hairballs - until the goons drop the sofa and he
  comes after you himself - and **the Walking Building** (the owner's son's idea): a tenement block on
  two stomping legs that fills a long street from wall to wall and walks down it at you, windows firing,
  its front-door mouth spitting junk and spilling acid. Beat it before it squashes you against the end of the street!
- **The Undercity** - the Slums' boss leaves an open sewer hatch: down it, the False
  King's streets - sewer tunnels and old streets buried under the city, lit by glowing green water, full
  of critters with that odd green ear - sewer gators that thrash rings of water, alley dogs that howl
  in rat packs - and the King's raccoons, who snatch your coins and run (shoot them to get them back!).
  Baa-llo keeps a shop down here too. At the end waits **the Skunk King**, the False King: a fat skunk in
  a cardboard crown, a shower-curtain cape and a plunger for a sceptre, riding a shopping cart full of
  trash - stink clouds, perfume bottles and royal decrees, and when his crown falls off he really panics.
  Beating him ends the run with the **normal ending** - the streets are quiet... but the real King still
  sulks up in the clouds. The true ending is still up there.
- **The family's rocket and the Moon** (work in progress) - every one of the family who beats the Fluffy
  King adds a piece to a cardboard rocket in the living room. Once all four have, the rocket lands on the
  clouds after the King - climb in to save the kingdom *and* fly on up: half a minute dodging and
  shooting asteroids, then the Moon - craters, a moon-cheese mine, an old observatory, crystal spires and
  a starry sky with Earth hanging in it. Its folk (the owner's line-up): fluffy moon bunnies with glowing
  red eyes, living asteroids that split in two when you shoot them, astronauts with jetpacks and little
  green aliens that beam about. In his castle waits **Grandfluff**, the Fluffy King's grandfather - a good
  old king gone grumpy: first drawing constellations of bullets with his walking stick, then a bite of
  moon cheese makes him young again, and at the end he grows as big as the Moon itself. Cheer him up
  for the **true ending**.

</details>

### Working on the game

Dev builds with debug shortcuts (god mode, every gun, jumping to any floor or boss), the scripts that
generate the art, sound and music, and how to add room layouts are in [DEVELOPING.md](DEVELOPING.md)
(it spoils the secrets too). Notes for coding agents are in [AGENTS.md](AGENTS.md).
