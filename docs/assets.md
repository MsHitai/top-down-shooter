# Generated art, sound and music

[Documentation index](README.md)

## Asset workflow

Assets live under `src/main/resources/assets/` and load with the `assets/` prefix.
Generate art/audio from scripts instead of hand-editing binary output. Only
`dev.properties` is Maven-filtered; filtering binary assets corrupts them.
Only `drop.mp3` and `die-sound.mp3` are loose in the assets root; other resources use
sprites, tiles, sfx, music, fonts and shaders folders. Window icons are stock libGDX
resources outside that assets root.

The detailed reference below was moved from DEVELOPING.md. Runtime drawing rules are
in [rendering](rendering.md), layouts in [rooms and cover](rooms-and-cover.md).

## Generated formats and rendering constraints

The game renders directly at the window's
resolution, with continuous camera motion. Do not downsample the scene to sprite resolution:
small stars, the King's fluff, rotated sprites and moving bullet outlines lose detail and
flicker as they cross a coarse pixel grid. Sprite assets retain Nearest filtering. Lights, bullets and floor effects keep their original
full-resolution rendering. Assembled enemy bodies retain native-resolution sampling: forcing moving body parts onto a
coarse pixel grid aliases their fine features. The scene and camera are never downsampled.
Smooth shape masks use white RGB even in transparent pixels, so filtering a tinted edge does
not introduce a dark fringe around pale fluff, stars and rings.

Authored 32x40 enemy sheets at 64 pixels per world unit now cover `BulletKin`, `ShotgunKin`,
`VeteranKin`, `BandanaKin`, `PirateKin`, `TankerKin`, `BlueShotgunKin`, `SawnOffKin`, and the
three mancers. Six poses cover four walking frames, idle and braced firing; the white silhouette
row preserves hit flashes. `gfx/KinArt` draws the costumes while the enemy classes keep their
weapon movement, wind-ups, pump actions, recoil, charge orbs and attack tells.

Town and marked rats use authored 40x28 frames in `rat.png` at 64 pixels/unit (`RatArt`),
with four scurrying poses and idle. Large connected fur planes, an inset eye, separate feet and a
bent tail follow the kin's art approach without a post-process texture. Marked rats retain their
1.5x scale and green landmarks; nine fade palettes support the ending, and each kind has its own
white damage-flash row. Collision, speed and squishing rules are unchanged.

The six Under-the-Bed bosses use `boss_under_bed.png`: 128x128 cells, three columns,
two character rows followed by legacy white hit-flash rows and two pupil-layer rows.
The pupils (or button holes) follow the hero, including when the body is mirrored or recoiling;
boss cards and book portraits use a centred gaze. Gameplay keeps these six
bosses in their normal colours when hit so sustained fire never hides the designs. `tools/bed_boss_art.py`
redraws the designs in `output/imagegen/under-the-bed-jokes-v1` as transparent pixel
sprites, including their household props. `tools/boss_art.py` generates this sheet
alongside the shared boss atlas; `BossArt` uses it for gameplay, boss cards and the
Flufficon. To regenerate just the boss assets, run
`python3 -c 'import sys; from pathlib import Path; sys.path.insert(0, "tools"); import boss_art; boss_art.generate(Path("src/main/resources/assets/sprites"))'`.

Under-the-Bed attack geometry, turn timing and fairness checks are documented in
[Under the Bed](under-the-bed.md). Keep those rules independent of art and portrait drawing.

World drawing uses two depth-sorted layers: floor objects/ground enemies, then small flying enemies.
`Enemy.drawsAboveFurniture()` defaults to `flying && !boss`, so hovering shapes, ghosts, angels,
clouds and other regular flyers remain visible over the props they cross. `CheeseWorm` opts out:
its `flying` flag disables navigation collisions for burrowing, not aerial movement. Boss scenery
keeps its existing depth rules. `EnemyDrawOrderTest` checks furniture crossings, ordinary ground
occlusion, ordering between flyers and the burrower's exception.

Projectiles use stable surface lighting. Round shots, drops and bombs have a shaded edge and
small highlight. Squares, diamonds and triangles use a bevel. The sprite sheet from
`tools/projectile_art.py` adds prepainted layers for stars, clouds, feathers, notes, missiles,
wings, rubble, swords, petals and pillows. The same star appears in Grandfluff's constellation
warning and its moving shots. Night Hawk's missiles, the Kaiju's rubble and the trash bags use
render-only shapes. Beam lengths and warning positions still come from the attack logic.
Projectile collisions, movement, lifetime and damage are independent of the artwork.

The family has distinct 40x56 sheets at 64 pixels per world unit: a hooded boy, a slim ninja
sister with a ponytail, crossed rear straps and a plain rear sash band, a rounder granny with a shawl and glasses, a broad grandad
with a waistcoat and moustache, a large-headed baby, and a puffy pocket-size King. Walk, back,
roll and all death frames share the original logical anchors. Faces follow the old 20x28 portraits:
compact eyes looking to one side, stepped head shapes and clean skin planes. Hair, glasses, the
moustache and the pacifier distinguish the family even at gameplay scale. Rear views have their own back seams,
hood/shawl/waistcoat backs, without front buttons or front pockets. The little King's crown overlaps
the upper fluff; his dot eyes and upturned smile sit on a single pink plane. His tucked roll retains a visible
face and folds his feet across his belly so the full rotation reads on a round silhouette.
Standing and rolling share the same face; the roll never paints an extra mouth. `Sprites.HERO_W/H` remain logical
20x28 sizes; use `Sprites.heroW/heroH` for source regions, and `Sprites.w/h` for props. The
player's world dimensions, feet box, belly hitbox and earned skull-crown anchors are unchanged. The hub,
rocket pilot and ending/intro pictures all use the hero-specific scale.

The remaining regular enemy cast uses authored 48x48 sheets (`cast_*.png`, `gfx/CastArt`),
from `tools/enemy_art.py`, called by `gen_sprites.py`. All 58 Flufficon entries now use authored
bodies or core layers. Humanoid costumes have material-specific construction (cap piping, lapels,
mask lenses, suit seams, pockets and metal panel edges). Dandelion has separate ivory petals and
an ochre seed head. Rats keep low connected fur planes and planted feet. No random surface grain
or full-scene pixelation is applied to these sprites.

The first six columns hold four steps, idle and preparation. Alley Dog adds lunge
and howl columns. Row two is an alpha-identical white hit silhouette.
All seven Under the Bed residents use this same format, drawn by `tools/bed_enemy_art.py`
through `enemy_art.py`: clustered dust fibres, enamelled tin and brass, swirled glass,
wood grain and a steel spring, stitched quilt panels, and bevelled, chipped ivory.
Their collision circles remain independent of the larger artwork.
Multipart creatures retain their original animated appendages and combat effects: wing beats,
pincers, shields, rotating shells/craters, veteran marks/medals, emerging worm segments, jumping
bunnies, jet flames and teleport alpha. Body, head, fist and drum layers can be placed independently.
Authored bodies bypass `ActorInk` to keep their own palette and outline. Enemy guns use layered
metal edges, a dark muzzle and a separate grip; the riot shield keeps its directional projection,
transparency and raised/lowered state. Hitboxes, health, timing and bullet patterns are unchanged.

Bosses use painted parts from `tools/boss_art.py` (`boss_parts.png`, read by `gfx/BossArt`).
Each part attaches to the same animated anchor as the original geometry: hinges, weapons, limbs
and faces can still move independently through their attack phases. The lower atlas half repeats
the silhouettes in white for hit flashes. The giant Grandfluff moon uses its own 128-pixel disc
in `boss_moon.png` so crater rims and ledges remain crisp across the arena. `ActorInk` still outlines the assembled bosses; the
painted fleece and crowns of the Fluffy King, Grandfluff, and Shadow King carry their own outlines
so the shading never cuts across their faces. Grandfluff shrinks to zero before the giant moon
starts appearing during the phase transition. The Fluffy King has dot eyes and a small smile on
his throne, animated eyes and brows in battle, and redder fleece in phase two.

The actor layer uses the current GL viewport's full physical pixel dimensions, resizes with the
window, preserves clipping/transform/blend state, and composites only the occupied rectangle.
Body sampling remains at native resolution, including transformed portraits, to keep small moving
features stable. It does not downsample the scene. Translucent material keeps its alpha, white hit flashes and
red attack signals keep their brightness, and the original two kin plus the soft-robed mancers
bypass the extra ink pass. The Night Hawk's
additive searchlight is drawn on the ground before the body pass so it still lights the floor.
Shaders live in `assets/shaders/actor.vert` and `actor.frag`; material masks are generated by
`Draw.materialSurface`, with no random texture noise or time-varying lighting.

The Kaiju also has an anatomy redesign: charcoal reptile scales, heavier upright haunches,
a long muzzle with small amber eyes, a moving lower jaw, short tucked resting forelimbs,
continuous tapered muscles, and large jagged dorsal plates that turn blue during breath charge.
Head hitbox, mouth/beam origin, slam targets, timing, warning zones and damage keep their values.
Raised paws and the Kraken's emerging tentacles receive the same material pass separately from
floor warnings and beams.

All 64 room themes use sparse, shallow-contrast floors (`quiet_floor`); the six village themes
retain their approved palette. Wood, worn slabs, raised panels, carpeting, moon craters, cheese,
sand, clouds and the dream floor each retain characteristic marks. Walls, windows, doors and
hazard colours keep their navigation/combat meaning. The living room and bedroom share calmer
wide floorboards; the King's room has broad low-contrast rose slabs.

Furniture and guns receive connected inset highlights and dark bevels before atlas packing
(`detailed_sprite`). Both atlases use 64 source pixels/unit. Chests have inset front panels,
raised riveted straps and hinges; stone supports have carved flutes and cracks, and furniture has
inset fasteners. Pit/exit/coin symbols and fire pixels only receive nearest-neighbour enlargement.
`props.txt` rectangles are source pixels; `Sprites.prop` returns scaled regions so `Sprites.w/h`
keep the original world dimensions and cover boxes. Home regions still use 32 px/unit.
`guns.txt` rectangles are source pixels, but grip coordinates stay logical 32 px/unit: gun size,
grip position and muzzle reach remain unchanged. Room backgrounds retain their original grid.
Check motion as well as still images when changing the art.

Dream Lights Out uses a separate darkness mask: 18% dark at the hero and 86% at the edge,
with a smaller clear area than normal night. Dream lamps use alpha blending instead of additive
white light, so pale floors and overlapping shop lights no longer wash out. Ordinary night keeps
its previous mask and lighting.

Everything except the font is generated by scripts in `tools/` - tweak and re-run them rather than
editing the files:

- `python3 tools/gen_sprites.py` - family sheets, authored enemies (`bullet_kin.png`, `shotgun_kin.png`, `actor_*.png`, `cast_*.png`, `kin_weapons.png`, `rat.png`; cast layers from `tools/enemy_art.py`), guns, items, furniture, chests and all room tilesets (needs Pillow)
- `python3 tools/gen_sfx.py` - sound effects (needs numpy; ffmpeg for the death cry)
- `python3 tools/music/tracks.py <GeneralUser-GS.sf2> src/main/resources/assets/music [track ...]` - the
  soundtrack: menu, home, village, town, city, tower, sky, sea, slums, undercity, moon, and the boss themes boss_moon (Grandfluff), boss_slums, boss_undercity, boss (the Fluffy King),
  boss_village, boss_town, boss_city, boss_tower, boss_sea and boss_dream.
  Under the Bed uses `under_bed` (Dust Bunny Strut: G minor, 104 bpm; tiptoeing swing - a marimba
  sneaks the hook over a chromatically creeping bass, a bassoon grumbles the monster's part,
  pizzicato critters scurry, kalimba dust and brushes) for the floor and
  `boss_under_bed` (The Floorboards Answer: D minor, 138 bpm; a leaping open-fifth motif
  drives a 3+3+2 stomping string/piano engine, short horn calls, toy xylophone and brass;
  a gentler bowed-string opening and an eerie Eb-Lydian middle section with background choir) for all six boss fights.
  Needs numpy, ffmpeg and FluidSynth (`brew install fluid-synth`).
  Delete the leftover `.wav` and `.mid` files afterwards.

## Sound registration and runtime music

Register generated effects in `audio/Sfx`. The pickup sound uses `drop.mp3`; death audio
is trimmed from `die-sound.mp3` so it aligns with the animation, followed by `body_fall`
when the hero lands. Regenerating SFX rewrites WAVs: commit only actual audio changes.

`Stage.music`, `Stage.bossMusic`, `Boss.music` and the director choose tracks;
`GameScreen` crossfades. Title/home fade in. The options volume multiplies each track's
own volume; `Sfx.volume` controls effects. Bot mode mutes sound. Track generation can
fall back to ffmpeg's Vorbis encoder if libvorbis is unavailable.

## Generator sources

- [gen_sprites.py](../tools/gen_sprites.py)
- [enemy_art.py](../tools/enemy_art.py), [bed_enemy_art.py](../tools/bed_enemy_art.py)
- [boss_art.py](../tools/boss_art.py), [bed_boss_art.py](../tools/bed_boss_art.py)
- [projectile_art.py](../tools/projectile_art.py)
- [gen_sfx.py](../tools/gen_sfx.py)
- [tracks.py](../tools/music/tracks.py), [musiclib.py](../tools/music/musiclib.py)

## Implementation

- [Sprites](../src/main/java/com/trush/game/gfx/Sprites.java)
- [Draw](../src/main/java/com/trush/game/gfx/Draw.java)
- [Sfx](../src/main/java/com/trush/game/audio/Sfx.java)
- [GameScreen](../src/main/java/com/trush/game/screen/GameScreen.java)
- [Stage](../src/main/java/com/trush/game/world/Stage.java)
- [Boss](../src/main/java/com/trush/game/enemy/Boss.java)
