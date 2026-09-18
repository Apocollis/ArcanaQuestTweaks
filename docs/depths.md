# Depths module (1.8)

Last updated: 2026-09-18.

Config: `config/arcanaquesttweaks/aqtweaks_depths.cfg`. **New chunks only** for carve/fill. Client fog/sky apply immediately.

This is the negative-Y compatibility layer plus AQTweaks’ own -Y cave generator. Village flatten on the same RTG chunk class is [rtg.md](rtg.md). Do not mix the two mixins.

## Locked intent

Depths Update extends the Overworld to **Y = -64**. Several 1.12 gens still assume Y ≥ 0. Tweaks:

- Fills RTG’s sub-zero primer with Deepslate (bedrock at min Y). **Never write Y=0** in that fill.
- Disables Depths’ own -Y cave samples, then carves AQTweaks caves in the primer (upper tunnels, chambers, lower deep, sparse shafts).
- Decorates the lower cavern with Quark speleothem clusters (primer-only; stock Quark never walks below Y 4).
- Opens mouths at Y0 into **+Y Better Caves** without cancelling BC.
- Lets Better Caves carve RTG terracotta/clay/etc.; cancels BC bedrock flatten; fixes surface-altitude for -Y.
- Lets CoFH World and Recurrent Complex rays go down to min Y.
- Client: dark fog and no skybox below Y0 (Overworld only).

**Primer owns -Y.** Chunk writes below 0 are not trusted. The chunk pass only reinforces Y0–4 seam air and seals water-biome Y0.

## Hard constraints

- Stay version **1.8**.
- Mixins live in **required** `mixins.aqtweaks.json`. RTG, Depths Update, Better Caves, CoFH World, and Recurrent Complex are assumed present in this pack.
- `MapGenBetterCaves` subclasses `MapGenCaves`. Cancel vanilla worms only when `this.getClass().getName()` is exactly `net.minecraft.world.gen.MapGenCaves`.
- Production Better Caves carve method is `func_186125_a` (`remap = false` on the mixin class). Seam inject is `@At("RETURN")` — after BC, not instead of BC.
- `RayMatcher.cast` is an `@Overwrite`. RC version bumps can silently break deep structure placement.

## How the parent mods work

### Depths Update (`sayys.depthsupdate`)

Adds negative Y and a `CaveNoiseGenerator` that samples `ICaveGenerator` per column/Y. Vanilla/Better Caves were written for 0–255. Depths’ built-in -Y caves did not match this pack’s Better Caves look.

Two mixins, one target class:

1. `MixinDepthsCaveNoiseGenerator` **Redirects** `ICaveGenerator.sample` so **y &lt; 0 is never sampled**. This has **no config gate** — Depths parent -Y caves stay off whenever the mixin applies.
2. `MixinCaveNoiseGenerator` **Inject HEAD cancellable** on `generate`. If Depths module + Better Depths Caves are on, Tweaks writes the primer and **`ci.cancel()`** so Depths’ generate body does not run. If that flag is off, the inject returns without cancel; Depths generate still runs, but (1) still skips y &lt; 0, so you get **no** Tweaks -Y caves and **no** Depths -Y caves.

### YUNG's Better Caves

`MapGenBetterCaves.func_186125_a` carves **+Y** caves in the primer. Tweaks runs **after RETURN** and punches tunnel mouths at Y0–4 along `UpperTunnelNetwork` (same paths as -Y upper worms). Does not cancel BC.

- `CarverUtils.canReplaceBlock`: BC stops on many RTG blocks; Tweaks allows rock/ground/clay/sand/grass/ice/packed ice/crafted snow, plus name fallbacks (`stone`, `deepslate`, `clay`, `terracotta`, `dirt`, `sand`, `rock`, `granite`, `diorite`, `andesite`, `basalt`, `tuff`, `slate`). Gated by `enableDepthsModule` only (not the BC-negative-Y flag).
- `BetterCavesUtils.getSurfaceAltitudeForColumn`: BC used cave ceilings as “surface”; Tweaks finds the highest solid with only air/water above to 255. Gated by `enableDepthsModule` **and** `enableBetterCavesNegativeY`. Null primer → 64.
- `FlattenBedrock.flattenBedrock`: cancelled so Depths/RTG bedrock at -64 is not flattened back to Y0. Gated by `enableDepthsModule` **and** `adjustBetterCavesBedrock`.

### RTG (`ChunkGeneratorRTG`)

`generateTerrain` only fills 0–255. Below is void. `MixinChunkGeneratorRTG` TAIL-fills minY..-1 with Deepslate and bedrock at minY. **Do not write Y=0** — that sealed Better Caves mouths.

Village flatten is `MixinChunkGeneratorRTGVillage` on the same class; it rewrites noise **before** `generateTerrain`. See [rtg.md](rtg.md).

The two run in the right order because of their **injection points**, not because of any declared priority: the village mixin injects at `generateTerrain` HEAD (plus earlier `func_185932_a` / `getNewerNoise` points), the Deepslate fill at `generateTerrain` TAIL. Neither sets `@Mixin(priority = ...)`, and their adjacency in `mixins.aqtweaks.json` is coincidental. Reordering that list changes nothing; needing it to change something means the injection points are wrong.

### CoFH World

`DistributionUniform.generateFeature` does `Math.max(y, 0)` (or similar `Math.max(II)`). Redirect that call while Depths + CoFH flags are on so the floor is `minWorldY` instead of 0. If the method gains another `Math.max(II)`, the redirect may hit the wrong call.

### Recurrent Complex (IvToolkit rays)

`RayMatcher.cast` stopped at Y=0. `@Overwrite` continues the ray to `minWorldY` when RC negative-Y is enabled. Structures can sit in the deep. Re-read this method on every RC bump.

### Vanilla `ChunkProviderServer.func_185932_a`

After the chunk exists, reinforce Y0–4 seam air on tunnel paths (chunk -Y writes are unreliable; primer owns -Y). Water biomes: seal Y0 with Deepslate so oceans do not open into the deep.

## Design plan (carve pipeline)

Per Overworld chunk, conceptually:

1. RTG `getLandscape` → (village flatten, if any) → `generateTerrain` 0–255 → Depths mixin fills Y min..-1 solid.
2. Depths `CaveNoiseGenerator.generate`: Tweaks cancels parent body and carves -Y in the **primer** (bedrock floor, upper worms, lower cavern, decor).
3. Better Caves carves +Y. Tweaks then opens Y0 mouths on the same worm paths (land only).
4. Vanilla `MapGenCaves` worms cancelled; BC subclass not cancelled.
5. Chunk provide: reinforce seam air Y0–4; water/beach/river/ocean columns get Deepslate at Y0 if air.

Shared path math: `UpperTunnelNetwork` (world seed). Primer, BC companion, and chunk seam must stay on that one network or mouths miss the tunnels.

## Current cave algorithm

Hardcoded in `MixinCaveNoiseGenerator` + `UpperTunnelNetwork`. Noise is Better Caves `FastNoise`. Seed splits: low 16 bits / next 16 bits plus fixed offsets. `init` rebuilds when the world seed changes (`initializedSeed`); a mid-session seed change re-inits both the mixin noise and `UpperTunnelNetwork`.

Vertical bands (defaults, `minWorldY` = -64):

| Band | Y | Role |
| --- | --- | --- |
| Bedrock | minY .. minY+3 | Solid bedrock |
| Lower deep | -60 .. ~-26 | Dual-noise cavern, lava at -55, landmass islands |
| Roof shell | ceilY ~-27..-23 through -23 | Deepslate lid; sparse shafts only |
| Upper worms | -22 .. +4 | BC-style dual-noise tunnels + chambers |
| Seam | -1 .. +4 (3×3 at -1..+2) | Mouths into +Y BC |

### Upper tunnels (`UpperTunnelNetwork`)

Mimic Better Caves 1.12 `CaveCarver`: sample two noises, require both ≥ threshold, then blend upward (F1 into y+1, F2 into y+2) for headroom. Soft-close: threshold rises 30% over the top 5 blocks (`DIG_TOP` 4, `TOP_CUTOFF` 5).

**Memoised.** Every value here is a pure function of world XZ and the seed, so results are cached per thread (`ThreadLocal`, LRU-bounded, cleared when `init` rebuilds noise). Do not make it a shared map — worldgen is not guaranteed to be single-threaded. Three caches:

| Cache | Key | Holds |
| --- | --- | --- |
| `columns` | world XZ | Raw dig bitmask (27 bits, Y `DIG_BOTTOM`..`DIG_TOP`) + blended tunnel flags, from **one** 108-sample pass |
| `digs` | world XZ | Finished `ColumnDigCache` (post chamber connectors) |
| `cells` | chamber cell XZ | Spawn pass, jittered centre, `columnHasWorm(centre)`, half-size, square/round, floor Y |

`wormDigCore` and `columnHasWorm` read bits off the cached raw mask — the raw samples and the blended samples come from the same pass because `blendAndDig` only mutates copies made after the mask is built. Chamber cells matter most: each column scans 9 cells but a chunk only touches ~16 distinct ones. The `digs` cache is what stops the primer carve, the BC companion, and the chunk seam from rebuilding the same 256 columns three times.

| System | Noise | XZ / Y scale | Base thr | F1 / F2 | Freq |
| --- | --- | --- | --- | --- | --- |
| Type 1 (worm) | CubicFractal RigidMulti, 1 octave, gain 0.3 | 1.6 / 5.0 | 0.95 | 0.9 / 0.9 | 0.03 |
| Type 2 (open spur) | Simplex | 0.9 / 2.2 | 0.82 | 0.95 / 0.5 | 0.025 |

A column carves if Type 1 **or** Type 2 digs that Y.

**Chambers:** 23-block cells. Spawn noise ≥ 0.08, jittered center, require a worm at the center. Half-size 4–6 (diameter 8–12), square or round. Floor is first worm Y in -20..-8 (clamped). Height 4–8, top capped at -5. Neighbor raw-dig within ±1 Y connects corridors without hollowing the whole room.

**Lower breach (sparse shafts):** only if tunnel/chamber floor sits on or 1 above the lower ceiling (`ceilY`). Shaft is `ceilY-2` .. `tunnelFloorY`. Not a swiss-cheese roof.

**Seam flag:** any tunnel carve in Y -2..+4. BC companion and chunk pass use this, not a second noise.

### Lower cavern (`MixinCaveNoiseGenerator`)

- Dual SimplexFractal, freq 0.009, sampled every 4 Y then lerped (`xz` 0.55, `y` 0.45).
- Open when `lower1 * lower2 < 0.45`, tightened near the ceiling (last 4 blocks) and near lava (below lava+5).
- **Landmass:** Simplex floor `> -0.12` is land. Surface = lava Y (-55) plus 0–2 from a second height noise. Land columns stay solid up to that surface (rooted to bedrock). Do not carve those cells into air.
- **Lava channels:** floor `&lt; -0.25` and not land → fill air at/below lava with lava.
- **Ceiling Y:** Simplex bump around -25, clamped **-27..-23**. Roof loop fills air/lava with Deepslate from `ceilY` through -23 except breach shafts.

Decor (Deepslate), after carve:

- **Columns:** 24-block cells, spawn noise ≥ 0.25, radius ~3.75 wider at ends, strength &gt; 0.18. The 9-cell scan is `pillarMinDist`, resolved **once per column**; `columnStrength(minDist, heightFrac)` then runs per Y. The flare radius is the same for every cell at a given Y and strength falls off with distance, so the nearest centre is always the strongest — nearest distance alone is enough.
- **Floor spikes:** land only, spike noise &gt; 0.52, height 4 or 5 (rarer than stalactites).
- **Stalactites:** spike `&lt; -0.15` and not a lower-breach column, length 5–16 down from ceiling, stop on solid, stay above land surface.
- **Bridges:** 16-block cells, spawn ≥ 0.28, span 16, half-width ~1.2. Both ends land, mid a lava channel. Solid fill under a smooth arch, deck 4–6 above lava. Skip breach shafts.
- **Orphan cleanup:** isolated floaters and short stacks (≤8) that do not touch the ceiling, with air/lava above and below. Protects columns, bridge fill, and short floor spikes.
- **Quark speleothems:** after that pass, if Quark is loaded and `enableQuarkSpeleothems` is on, `QuarkSpeleothemDecor` plants in-chunk clusters (60 attempts, 10 per cluster, 1–3 tapering `EnumSize` blocks). Same walk-to-full-block logic as stock `SpeleothemGenerator`, but Y is the lower cavern (−59..−24) and writes go to the primer. Deepslate hosts `stone_speleothem` (Quark has no deepslate variant). Skip roof-breach shaft columns. Does not replace Deepslate columns/spikes/bridges. Stock +Y Quark gen is unchanged.

Carve loop skips existing air and bedrock. Land fill is applied again after carve so skipped cells still get a floor.

When Better Depths Caves is on, `generate` **cancels** after this write. Parent Depths cave body does not run.

## Seam (Y0 mouths)

Same `UpperTunnelNetwork` as -Y upper worms. Overworld only (`dimension == 0`).

**Primer (after BC):** for each land column with `shouldOpenSeam()`, air Y -1..+4. Then 3×3 air at Y -1..+2 around those cores (chunk-local; `tryCarve` drops out-of-chunk neighbors).

**Chunk (after provide):** same core path, air Y 0..+4, 3×3 at Y 0..+2. **Water biomes skip mouths** and if Y0 is air, set Deepslate.

`DepthsBiomeUtil.isWaterBiome`: `Type.WATER`, `OCEAN`, `RIVER`, or **`BEACH`**, or name contains `ocean`, `deep_ocean`, `beach`, `river`, `coral`, `kelp`. This is **stricter than RTG village “ocean-like”** — beaches are sealed so the sea floor does not open into the deep. Do not reuse village beach exceptions here.

## Client

**Fog** (`DepthsFogHandler`): Overworld, eye Y &lt; 0, not standing in water/lava material at the eye. `EventPriority.LOWEST`. Color lerp 85% toward (0.10, 0.10, 0.12). Fog start **32**, end **52**. Flags: `enableDepthsModule` and `deepCaveFog`.

**Sky** (`MixinRenderGlobal.renderSky` HEAD cancel): Overworld, view entity eye Y &lt; 0. Flags: `enableDepthsModule` and `hideSkyBelowZero`.

## Config (`aqtweaks_depths.cfg`)

| Name | Default | Live? | Meaning |
| --- | --- | --- | --- |
| Enable Depths Module | true | yes | Master. Off: fill/carve/fog/sky/CoFH/RC/BC hooks that check it skip. **Exception:** Depths `ICaveGenerator.sample` y&lt;0 is still redirected off. |
| Minimum World Y Elevation | -64 | — | **Pinned.** Bedrock floor and ray/CoFH clamp read the constant; not a tunable knob |
| Better Depths Caves | true | yes | AQTweaks primer carve + BC mouths + chunk seam. Off: those skip; Depths still has no -Y samples |
| Enable Quark Speleothems | true | yes | Lower-cavern primer Quark clusters. Off or Quark missing: skip; cave shape unchanged |
| Deep Cave Fog | true | yes | Client fog below Y0 |
| Hide Skybox Below Y 0 | true | yes | Client skip `renderSky` |
| Enable CoFH World Negative Y | true | yes | `Math.max` floor → minWorldY |
| Enable Better Caves Negative Y | true | yes | Surface-altitude mixin only |
| Adjust Better Caves Bedrock Height | true | yes | Cancel `FlattenBedrock` |
| Enable Recurrent Complex Negative Y | true | yes | `RayMatcher.cast` down to minWorldY |

`enableBetterDepthsCaves` (generation) and `enableBetterCavesNegativeY` (BC util) are different flags. Do not wire them together.

## File map

| File | Parent hook |
| --- | --- |
| `mixin/depthsupdate/MixinDepthsCaveNoiseGenerator.java` | Skip Depths `sample` when y&lt;0 (**no config**) |
| `mixin/bettercaves/MixinCaveNoiseGenerator.java` | AQTweaks -Y primer carve; cancel Depths `generate` when caves on |
| `mixin/bettercaves/MixinBetterCavesDepthsPass.java` | After BC, Y0 mouths |
| `mixin/bettercaves/MixinChunkGeneratorRTG.java` | RTG sub-zero fill, not Y=0 |
| `mixin/bettercaves/MixinCarverUtils.java` | BC replaceable blocks (`enableDepthsModule`) |
| `mixin/bettercaves/MixinBetterCavesUtils.java` | True surface altitude |
| `mixin/bettercaves/MixinFlattenBedrock.java` | Cancel Y0 flatten |
| `mixin/bettercaves/MixinDepthsMapGenCaves.java` | Cancel vanilla `MapGenCaves` only |
| `mixin/cofh/MixinDistributionUniform.java` | CoFH min Y |
| `mixin/reccomplex/MixinRayMatcher.java` | `@Overwrite` rays to -64 |
| `mixin/MixinChunkProviderServer.java` | Seam reinforce / water seal (Y≥0) |
| `mixin/MixinRenderGlobal.java` | Hide sky |
| `depths/UpperTunnelNetwork.java` | Shared tunnel / chamber / seam / shaft paths |
| `depths/PrimerAccess.java` | Direct (remapped) primer/state reads + open-sky surface scan |
| `depths/DepthsFogHandler.java` | Fog |
| `depths/DepthsBiomeUtil.java` | Water/beach/ocean/river/coral/kelp for seam seal |
| `depths/QuarkSpeleothemDecor.java` | Primer Quark speleothems in the lower cavern (`Loader.isModLoaded("quark")`) |
| `ArcanaQuestTweaksConfig.DepthsModuleConfig` | `aqtweaks_depths.cfg` |

## Design history (do not regress)

### 1. Fill Y=0 in the RTG terrain mixin

Solid Deepslate at Y=0 capped every Better Caves mouth. **Fix:** fill minY .. −1 (bedrock at `minY`, Deepslate above), never Y=0.

### 2. Cancel `MapGenCaves` for all subclasses

`MapGenBetterCaves` extends it. Cancelling the parent method killed +Y BC. **Fix:** class-name equals vanilla `MapGenCaves` only.

### 3. Chunk writes for -Y caves

Blocks below 0 often failed to stick on the chunk object. **Fix:** primer is authority for -Y; chunk pass only touches Y≥0 seam/seal.

### 4. Cave ceiling as “surface”

BC `getSurfaceAltitudeForColumn` returned underground lids, so caves broke the surface or died early. **Fix:** highest solid with open sky (air/water) to 255.

The first version searched **up** from every candidate to 255 to prove open sky. That nested scan could never fail: descending from 254, the first solid found already had nothing solid above it. The **only** case it changed was a solid block at Y 255, where every candidate failed and the method fell through to the 64 fallback. `PrimerAccess.openSkySurfaceY` is one downward pass and keeps that fallback explicitly (`Y 255 solid → 64`). Do not “simplify” it to plain first-non-air-from-the-top.

### 5. BC `FlattenBedrock` at Y0

Moved Depths bedrock back up. **Fix:** cancel flatten when the bedrock flag is on.

### 6. Water mouths into the ocean

Seam air under oceans drained the sea into the deep. **Fix:** `DepthsBiomeUtil` water **including beach**; Deepslate lid at Y0.

### 7. Depths parent -Y plus Tweaks -Y

Double carve / wrong look. **Fix:** never sample Depths y&lt;0; when Tweaks caves are on, cancel Depths `generate` after our primer write.

### 8. Reflection and repeated noise on the carve path

A Spark profile put Tweaks at ~56% of chunk generation: 34s primer carve, 18s surface altitude, 12s seam pass. None of it was algorithm cost. `UpperTunnelNetwork` re-sampled the same columns for all three passes and re-scanned chamber cells per column; `columnStrength` ran its 9-cell scan per Y; and every primer read went through `Reflect`’s `Method.invoke` because the mixins are `remap = false`.

**Fix:** memoise (above), hoist `pillarMinDist` per column, and move primer reads into `PrimerAccess`. Cave shape is unchanged — all three are exact-equivalence rewrites, not approximations.

## Do not regress

- Primer owns **-Y**. Chunk writes below 0 are not trusted.
- Do not cancel `MapGenBetterCaves`.
- Do not fill Y=0 solid in the RTG terrain mixin.
- The two `ChunkGeneratorRTG` mixins are ordered by injection point (village HEAD, fill TAIL). Never by `mixins.aqtweaks.json` list order or `@Mixin(priority)`.
- Water/beach/river/ocean/coral/kelp columns: no Y0 mouths into the sea.
- Keep `UpperTunnelNetwork` as the single path for primer, BC mouths, and chunk seam.
- `UpperTunnelNetwork` caches are **per thread** and bounded. Never a shared map; never unbounded.
- Performance work here stays exact-equivalence. No coarser noise, fewer octaves, or shifted thresholds.
- Vanilla member access stays out of `remap = false` mixin bodies — use `PrimerAccess` (hot path) or `Reflect`.
- `enableBetterDepthsCaves` vs `enableBetterCavesNegativeY` stay separate.
- `@Overwrite` on `RayMatcher.cast` — re-verify on Recurrent Complex updates.
- Turning off Better Depths Caves does **not** restore Depths’ own -Y caves (sample redirect has no flag).
- Quark speleothems in the deep are **primer-only** and **in-chunk**. Do not mixin `SpeleothemGenerator` to lower its Y walk, and do not `setBlockState` below 0 for this decor.
- Do not replace Deepslate columns, floor spikes, stalactites, or bridges with speleothems.

## Out of scope unless asked

- Recarving old chunks
- Making Depths parent -Y caves available again behind a flag
- Village flatten (that is [rtg.md](rtg.md))
- Version bump
