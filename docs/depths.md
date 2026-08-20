# Depths module

Config: `config/arcanaquesttweaks/aqtweaks_depths.cfg`. New chunks only for carve/fill.

## What Tweaks does

Depths Update extends the Overworld to **Y = -64**, but several 1.12 gens still assume Y ≥ 0. Tweaks:

- Fills RTG’s sub-zero primer with Deepslate (bedrock at min Y).
- Replaces Depths’ own -Y cave sampler with AQTweaks caves (upper tunnels, chambers, lower deep, sparse shafts).
- Opens mouths at Y0 into **+Y Better Caves** without cancelling BC.
- Lets Better Caves carve RTG terracotta/clay/etc.; shifts/cancels BC bedrock flatten; fixes surface-altitude for -Y.
- Lets CoFH World and Recurrent Complex rays go down to min Y.
- Client: dark fog and no skybox below Y0.

## How the parent mods work

### Depths Update (`sayys.depthsupdate`)

Adds negative Y to the world and a `CaveNoiseGenerator` that samples `ICaveGenerator` per column/Y. Vanilla/Better Caves were written for 0–255. Depths’ built-in -Y caves did not match the pack’s Better Caves look.

Tweaks **Redirects** `ICaveGenerator.sample` so Depths does **not** carve `y < 0`. Then `MixinCaveNoiseGenerator` (same class, bettercaves package) injects AQTweaks primer carve + decor. Two mixins, one target: disable parent -Y, then write ours.

Vanilla `MapGenCaves.recursiveGenerate` is cancelled **only** when `this` is exactly `net.minecraft.world.gen.MapGenCaves`. `MapGenBetterCaves` subclasses it — must not cancel that subclass.

### YUNG's Better Caves

`MapGenBetterCaves` carves **+Y** caves in the primer (`func_186125_a`). Tweaks runs **after RETURN** and punches tunnel mouths at Y0–4 along `UpperTunnelNetwork` (same paths as -Y upper worms). Does not cancel BC.

- `CarverUtils.canReplaceBlock`: BC stops on many RTG blocks; Tweaks allows rock/ground/clay/sand/grass/ice and name fallbacks.
- `BetterCavesUtils.getSurfaceAltitudeForColumn`: BC used cave ceilings as “surface”; Tweaks finds the highest solid with only air/water above to 255.
- `FlattenBedrock.flattenBedrock`: cancelled so Depths/RTG bedrock at -64 is not flattened back to Y0.

### RTG (`ChunkGeneratorRTG`)

`generateTerrain` only fills 0–255. Below is void. Tweaks `MixinChunkGeneratorRTG` TAIL-fills minY..-1 with Deepslate and bedrock at minY. **Do not write Y=0** here — that sealed Better Caves mouths. Village flatten is a **different** mixin on the same class (`MixinChunkGeneratorRTGVillage`); see [rtg.md](rtg.md).

### CoFH World

`DistributionUniform.generateFeature` does `Math.max(y, 0)` (or similar clamp). Redirect `Math.max(II)` while Depths+CoFH flags are on so the floor is `minWorldY` instead of 0.

### Recurrent Complex (IvToolkit rays)

`RayMatcher.cast` stopped at Y=0. `@Overwrite` continues the ray to `minWorldY` when RC negative-Y is enabled. Structures can sit in the deep.

### Vanilla `ChunkProviderServer.func_185932_a`

After the chunk exists, reinforce Y0–4 seam air on tunnel paths (chunk -Y writes are unreliable; primer owns -Y). Water biomes: seal Y0 with Deepslate so oceans do not open into the deep.

## Client parents

Vanilla `RenderGlobal.renderSky` — cancel below Y0 in Overworld. Fog via `EntityViewRenderEvent` (not a parent mod).

## Files (high level)

| File | Parent hook |
| --- | --- |
| `mixin/depthsupdate/MixinDepthsCaveNoiseGenerator.java` | Skip Depths sample y&lt;0 |
| `mixin/bettercaves/MixinCaveNoiseGenerator.java` | AQTweaks -Y carve |
| `mixin/bettercaves/MixinBetterCavesDepthsPass.java` | After BC, Y0 mouths |
| `mixin/bettercaves/MixinChunkGeneratorRTG.java` | RTG sub-zero fill |
| `mixin/bettercaves/MixinCarverUtils.java` | BC replaceable blocks |
| `mixin/bettercaves/MixinBetterCavesUtils.java` | Surface altitude |
| `mixin/bettercaves/MixinFlattenBedrock.java` | Cancel Y0 flatten |
| `mixin/bettercaves/MixinDepthsMapGenCaves.java` | Cancel vanilla worms only |
| `mixin/cofh/MixinDistributionUniform.java` | CoFH min Y |
| `mixin/reccomplex/MixinRayMatcher.java` | RC rays to -64 |
| `mixin/MixinChunkProviderServer.java` | Seam reinforce / water seal |
| `mixin/MixinRenderGlobal.java` | Hide sky |
| `depths/UpperTunnelNetwork.java` | Shared tunnel paths |
| `depths/DepthsFogHandler.java` | Fog |
| `depths/DepthsBiomeUtil.java` | Water biome tests |

## Do not regress

- Primer owns **-Y**. Chunk writes below 0 are not trusted.
- Do not cancel `MapGenBetterCaves`.
- Do not fill Y=0 solid in RTG terrain mixin.
- Water columns: no Y0 mouths into the ocean.
- `enableBetterDepthsCaves` vs `enableBetterCavesNegativeY` are different flags (generation vs BC utility hooks).
