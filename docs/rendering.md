# Rendering, depth, lighting and camera geometry

[Documentation index](README.md)

The game draws at the window's physical viewport resolution with continuous camera
motion. Do not downsample the scene or force animated body parts onto a coarse sprite
grid: small stars, fluff, outlines and rotated sprites flicker or lose detail. Pixel
sprite textures retain Nearest filtering. Collision dimensions remain independent.

## World passes

`RoomRenderer` bakes floor/walls/doors from theme tiles into a room texture and draws
barricades for locked exits. Tile art includes floor variants, wall faces/caps, door
frame and decorated/window faces. Open cloud/roof caps let the sky show outside the
floor; cloud rims puff outward. Moon uses a space background instead.

World actors use two depth-sorted layers: ground objects/enemies, then small flyers.
`Enemy.drawsAboveFurniture` defaults to `flying && !boss`. Cheese Worm opts out because
its flight flag represents burrowing collision. Boss scenery has separate floor/body/
overlay rules; raised Kraken tentacles and Kaiju paws must stay correctly layered.

Night vignettes and lamp light, underwater tint/shafts/bubbles, cloud sky and Moon
parallax are world presentation. Bullets are drawn clearly over gas/darkness as their
passes require. Dream Lights Out has its own stronger mask and alpha-blended lamps;
ordinary night retains its usual lighting. The Night Hawk's additive searchlight is
on the floor before body rendering.

## Art helpers and material pass

`KinArt`, `RatArt` and `CastArt` provide authored regular-enemy bodies while enemy classes
animate their attack tells and appendages. `BossArt` supplies multipart bosses and
portraits. `ProjectileArt` supplies stable shaded shapes; other geometry uses `Draw`.
Materials and outlines do not change timing, hit regions, warning positions or beam length.

The actor pass matches the physical viewport, resizes with it, preserves clipping,
transform and blend state and composites only its occupied rectangle. Native-resolution
sampling also applies to transformed portraits. Transparent material retains alpha;
white flashes and red warnings keep brightness. Authored bodies can bypass `ActorInk`
to retain their palettes and outlines. Shaders are `assets/shaders/actor.vert` and
`actor.frag`; generated masks use stable surfaces without random/time-varying grain.
Smooth masks keep white RGB in transparent pixels to avoid dark fringes when tinted.

## Scale and resource ownership

Use `Sprites.heroW/heroH` for high-resolution hero source frames, logical anchors for
world positioning, and `Sprites.w/h` for prop world dimensions. Gun atlas rectangles
are source pixels while grips remain logical coordinates. Home regions use their own
scale. Keep a sprite/art redesign from changing foot collision or muzzle reach by accident.
The shared Press Start 2P font belongs to `ETGFanfic` and includes Russian Cyrillic.
Keep integer font positioning disabled in the 7×5 world.

Check moving gameplay and portraits as well as still images after art changes. Generation
and asset formats are documented in [assets](assets.md); camera/HUD ownership in
[screens and interface](screens-and-interface.md).

## Implementation

- [Draw](../src/main/java/com/trush/game/gfx/Draw.java)
- [Sprites](../src/main/java/com/trush/game/gfx/Sprites.java)
- [RoomRenderer](../src/main/java/com/trush/game/gfx/RoomRenderer.java)
- [KinArt](../src/main/java/com/trush/game/gfx/KinArt.java)
- [RatArt](../src/main/java/com/trush/game/gfx/RatArt.java)
- [CastArt](../src/main/java/com/trush/game/gfx/CastArt.java)
- [BossArt](../src/main/java/com/trush/game/gfx/BossArt.java)
- [ProjectileArt](../src/main/java/com/trush/game/gfx/ProjectileArt.java)
- [ActorInk](../src/main/java/com/trush/game/gfx/ActorInk.java)
- [EnemyMaterial](../src/main/java/com/trush/game/gfx/EnemyMaterial.java)
- [GameWorld](../src/main/java/com/trush/game/world/GameWorld.java)

## Existing checks

- [EnemyDrawOrderTest](../src/test/java/com/trush/game/world/EnemyDrawOrderTest.java)
- [EnemyMaterialCoverageTest](../src/test/java/com/trush/game/gfx/EnemyMaterialCoverageTest.java)
