# RTG module (1.8)

Last updated: 2026-08-28, `MixinWorldGenLakes` remap true (`generate`) + lake-block field walk.

This is the RTG module: village flatten/placement, then post-terrain structure skip/settle. Locked intent, current pipeline, and why earlier approaches were dropped. Read this before changing village flatten, spawn veto, piece retry, or shrine/house/hut land settle.

Vanilla / RTG / Geographicraft / Recurrent Complex / Charm roles: [villagegen_info.md](villagegen_info.md).

## How the parent mods work

### Realistic Terrain Generation (RTG 7.3.3.6)

RTG replaces the Overworld chunk generator. `ChunkGeneratorRTG.func_185932_a` roughly:

1. `getLandscape` — per-column height `noise[]`, river strength, biomes
2. `generateTerrain(primer, noise)` — stone/dirt/water from that height field
3. caves, then ravines (carve the primer)
4. `villageGenerator.generate()` — vanilla village map gen **after** land exists

RTG does not flatten under villages. Tweaks rewrites `landscape.noise` **between** (1) and (2), then **reseals pad columns after (3)** so caves/ravines cannot leave a well over a chasm.

### Vanilla `MapGenVillage`

- `func_75047_a` — can this chunk be a village start (well)?
- `func_75049_b` — create `StructureVillagePieces.Start` (well + piece list)
- `func_151539_a` (`MapGenBase`) — generate into a primer/world
- Houses: `StructureVillagePieces.func_176066_d`
- `MapGenStructure.func_175797_c` — `isInsideStructure` / InControl “Village”

Pieces use AABBs. Tweaks does not replace piece types; it vetoes starts **and removes them from `structureMap`**, retries house/RC/waystone origins, omits ocean/river paths (and mostly-flooded lakes), and shapes RTG noise. Pack pipeline: [villagegen_info.md](villagegen_info.md).

### Recurrent Complex

`GenericVillageCreationHandler.buildComponent` adds RC buildings as village pieces, same start/facing as vanilla houses. Tweaks retries wet AABBs like vanilla houses. RC worldgen rays for **non-village** structures are the Depths module (`RayMatcher`), not this file.

## Locked intent

Keep **what** villages create (vanilla pieces + Recurrent Complex, plus at most one Astral small shrine). Change **where** they may start and **how RTG land under them is shaped**.

- Do **not** pave oceans or rivers into piers or docks. 1-block ocean/river notches inside the village pad may fill when Village Shore Close Ocean is on. Do **not** place wooden docks: a path that touches ocean/river, or that is mostly flooded lake, is omitted, not converted to planks.
- Houses, RC buildings, and waystones retry inland if they touch **ocean/river** (biome or RTG `landscape.river`). Low dry land (noise below min well Y) is **raised**, not skipped.
- **Ocean-like** (never raise, never well start unless a dry slot exists): `Type.OCEAN` or `Type.WATER` (not swamp), plus names `ocean`, `kelp` (BOP Kelp Forest `kelp_forest`), `coral`, `reef`, `atoll`, `lagoon`. Pure **BEACH** is not ocean. RTG `landscape.river` above `STRONG_RIVER` is never-raise even if the biome provider says plains.
- Wells may sit **16 blocks from the coast** (Chebyshev). Do not veto a land/beach well just because ocean is nearby beyond that. Veto if ocean-like is **closer than** `villageCoastBuffer`. Nearby **river does not** cancel a dry well. A well **in** ocean/river walks inland within `villageWaterRetryDistance`; open ocean with no dry slot is cancelled.
- In **swamp-like** biomes, pieces may stay. Plate buildings, mixed roads, **and the yards between them** (12-block component pad, overlapping). Open-water paths that are mostly lake are omitted rather than docked.
- Never raise or plate ocean-like or river columns, even inside a house or road AABB, **except** 1-block ocean/river notches inside the hard pad when Village Shore Close Ocean is on. No village piece may remain on those biomes.
- A **kept** house/RC/shrine/road on flooded non-ocean land (swamp water, plains lake) gets dirt **up to plate Y for the whole 12-pad**. Pads overlap into one village footprint. Lake **outside** the pads stays water. Dry wells below `villageMinWellHeight` are **kept** and the plate is `max(raw, knob)`.
- Keep a **flat plate under dry-land roads**, including stretches with no buildings. Omit a road from the plate (and from layout) if it touches ocean/river or is mostly lake.
- The plate is the **walkable village footprint**: every surviving piece **and** a 12-block hard pad around it. Overlap is one level. It is **not** a village-wide rectangle. Empty AABB corners with no nearby piece stay hills.
- Inland plains/forest (playtest “village 2”) is the target look for non-water biomes.
- Structure detection uses the **flatten plate** (land-box AABB + component pad + Hermite falloff), Y from the **well shaft floor** through plate plus `villageBoxHeight`. Those volumes are also saved as extra `AQTVillagePlate` children on the Start (`Village.dat`) without replacing house/path/RC boxes.

## Hard constraints

- Stay version **1.8**.
- **New chunks only.** Flattening writes RTG `landscape.noise` before `generateTerrain`. Village-pad columns are resealed after caves/ravines. Existing chunks are not recarved.
- RTG jar in this pack: `RTG-1.12.2-7.3.3.6.jar`. RTG has **no** village terrain queue of its own.
- Production RTG chunk method is `func_185932_a` (`remap = false`). Injects that replace the return need `CallbackInfoReturnable<Chunk>`.
- Mixin targets use SRG names. Do not use DeferredRegister / 1.16-style registries.
- Village mixins live in **required** `mixins.aqtweaks.json` (Forge, RTG, Recurrent Complex assumed present). Astral / Cambion / Mystical hut mixins are **optional** json (`required: false`).
- Two mixins target `ChunkGeneratorRTG`: village flatten **before** `generateTerrain` and pad **seal before `new Chunk`**; Depths Deepslate fill at **TAIL** of `generateTerrain` (Y min..-1, not Y=0). See [depths.md](depths.md). Do not merge them.
- Player/world/block/primer access goes through `util/Reflect.java`. Landscape samples and structure boxes are not raw `World.getChunkProvider()`.

## Design plan (placement vs flatten)

Keep **what** villages create. Change **where** they start and **how RTG land under them is shaped**.

1. Layout village XZ **before** RTG `generateTerrain` (`layoutVillageGrid`).
2. Sample plate Y from the well via live `ChunkGeneratorRTG.getLandscape`.
3. Flatten `landscape.noise` under land boxes (houses, well, mixed/dry roads). Never write ocean/river except 1-block pad notches. Flooded swamp/lake **inside the hard pad** is raised to plate Y.
4. RTG carves from that noise. Caves and ravines then punch the primer. Tweaks reseals shore-mask columns solid up to plate Y before `new Chunk`. Production method `func_185932_a` (`remap = false`).
5. Populate places the same pieces. Wet houses/RC/shrine/waystone/paths retry inland; leftover ocean/river or mostly-lake paths are omitted. If layout missed, paste skips the building when the surface is still liquid. Water lakes whose blob overlaps the 12-pad are skipped (lava lakes are not). After paste in a chunk, Tweaks re-checks light at torch/lamp sources so flag-2 placements actually flood.

Do not recarve old chunks. Do not veto a whole village because one building was wet. Do not treat a puddle on a path as a water bridge.

## Why we flatten noise, not blocks

Vanilla villages layout **after** terrain exists. RTG order is:

1. `getLandscape` (height noise, rivers)
2. `generateTerrain(primer, noise)`
3. `villageGenerator.generate()` (pieces)

If we wait until populate, the land is already carved. So we:

1. Layout village pieces (XZ) **before** RTG carves the chunk.
2. Sample plate Y from the **well column** in RTG `landscape.noise`.
3. Rewrite `landscape.noise` under the land footprint (12-pad, fill non-ocean water).
4. Let RTG `generateTerrain` build blocks from that noise.
5. After caves/ravines, refill village-pad columns up to plate Y.
6. Populate still places the same pieces.

Layout must be cheap. `layoutVillageGrid` runs **once per chunk** at **RETURN** of RTG `getNewerNoise` (noise already filled), or at flatten if noise never ran, for the current chunk plus the **vanilla well chunk** of each nearby village cell (spacing from the map gen, UT default 25, radius 8 chunks). Nested `getLandscape` during flatten/plate samples increments `SAMPLING` so `getNewerNoise` does not layout again. Flatten writes `landscape.noise` **once** before `generateTerrain` (no second `ModifyArg` pass). The well is `cellOrigin + random(0, spacing - minTown)` with seed `setRandomSeed(cellX, cellZ, 10387312)`, not the cell origin. Generating only origins almost never created the `Start`, so hill-side chunks flattened as raw RTG and buildings stepped. It does **not** call `generate()` on all 289 neighbors. Do not layout at `func_185932_a` HEAD — noise is empty there and unknown-as-wet omitted every road.

Flatten looks up `VillagePlate` records by **land-box overlap** first. Hit → flatten that chunk (no `rememberNearby`, no start-AABB scan). Miss → `ensureStarts` only if Tweaks’ list is empty, then `rememberNearby` on that miss, then `mergeStartAabbHits` only if land boxes still miss. Seal uses land/shrine overlap only (flatten already ran this chunk). They do **not** walk every Start in `structureMap` on wilderness. Mixin well-walk **replaces** the Record for that **well chunk**. `forgetRejectedStarts` caches kept well chunks this session so inland towns are not re-vetted on every chunk. `rememberAll` never overwrites a walked well. A chunk inside the start AABB with no land-box hit is an empty corner or omitted dock — not a hull flatten. Public `isNeverRaiseAt` does **not** read the land-box landscape ThreadLocal (that cache is only for `landBoxesOf` / `wetFraction`). RTG already caches `getLandscape`. `landBoxesOf` still omits a road only when its full AABB is flooded (`isAabbFullyFlooded`).

## File map

| File | Role |
| --- | --- |
| `rtg/VillageLandHelper.java` | Wet tests, swamp/ocean-like/river/beach, well veto, forget-once rejected Starts, coast buffer, per-column AABB wet/mostly-wet, paste floor skip, grid layout, live RTG noise sample, waystone inland slots, column landscape cache, stash `MapGenVillage`/RTG by World and seed+dim |
| `rtg/VillageShoreMask.java` | Hard-pad occupancy: 1-block ocean notch fill, opening (trim jetties), 8-connected rim for brick |
| `rtg/StructureLandSettle.java` | Post-terrain fill + rim. Large Astral under-fill is raw marble; Cambion uses pad 6 + falloff 12 |
| `rtg/VillageDebug.java` | `logs/villagepatch.log` in the instance folder (not `latest.log`) |
| `rtg/StructureVillageOverlap.java` | Village AABB/Y test for post-terrain schematics (not Y=0). BFS unwrap of wrapped providers/generators for `MapGenVillage` / RTG |
| `rtg/StructureLandSettle.java` | Fill under a placed schematic + rim slope; swamp-liquid fill; overwrite plant-like blocks |
| `mixin/bettercaves/MixinChunkGeneratorRTGVillage.java` | Stash gens on RTG construct; layout once per chunk after `getNewerNoise` RETURN + flatten noise once + seal pad after caves/ravines |
| `mixin/MixinMapGenVillageSpawn.java` | Well veto (`func_75047_a`) |
| `mixin/MixinMapGenVillageStart.java` | Remember start after create (`func_75049_b`) |
| `mixin/MixinMapGenVillageWorld.java` | Push/pop `World` around village `generate`; unwrap RTG from wrapped chunk gens |
| `rtg/VillagePieceVillagePlate.java` | Non-placing pad children saved on the Start (`AQTVillagePlate`). Houses/paths/RC stay separate |
| `mixin/MixinMapGenVillageInside.java` | Flatten plate as “inside village”; fallback if `Village.dat` has no pad children yet |
| `rtg/CommandAqVillage.java` | OP `/aqvillage` (level 2): TP on generated ground ~6 off the well; prefers unexplored. Miss logs provider/generator to `latest.log` |
| `mixin/MixinStructureVillagePieces.java` | House skip/retry inland on water; waystone relocates inland as the same piece; wet paths retry inland then omit |
| `rtg/VillageRelight.java` | After village paste in a chunk, `checkLight` at emitting blocks in the clip |
| `mixin/MixinStructureStartVillagePaste.java` | Populate abort on ocean/river floor; stamp `AQTVillagePlate`; relight clip |
| `mixin/charm/MixinASMHooksVillagePaste.java` | Same abort on Charm `ASMHooks.addComponentParts` (optional `mixins.aqtweaks.charm.json`) |
| `mixin/reccomplex/MixinGenericVillageCreationHandler.java` | RC building skip/retry on water |
| `rtg/VillagePieceAstralSmallShrine.java` | Village component that pastes Astral `smallShrine`; AABB from pattern; path overlap OK at layout; paste skips ocean/river **biome** only; liquid-only fill (no dirt collar) |
| `rtg/VillageAstralSmallShrineHandler.java` | Forge village handler, weight 5, limit 1. Inland retry on building collision or ocean/river biome. `CommonProxy` registers only if `astralsorcery` is loaded. Piece id `AQTSmallShrine` |
| `mixin/bewitchment/MixinWorldGenCambionHome.java` | House +1 paste, skip schematic air, village skip, hole-fill 6-pad at plains Y. Optional `mixins.aqtweaks.bewitchment.json` |
| `mixin/bewitchment/MixinWorldGenCambionHomeMedium.java` | Same for medium Cambion house |
| `mixin/bewitchment/MixinWorldGenStonecircle.java` | Village skip + Chebyshev retry. No plate |
| `mixin/bewitchment/MixinWorldGenMenhir.java` | Same for menhir |
| `mixin/bewitchment/MixinWorldGenWickerman.java` | Same for wickerman; relocates spawned entities |
| `mixin/mysticalworld/MixinStructureGenerator.java` | Hut village-skip/retry + land settle; barrow skip/retry (no plate). Optional `mixins.aqtweaks.mysticalworld.json` |
| `mixin/astral/MixinWorldGenAttributeCommon.java` | Skip Astral surface shrines on village overlap. Optional `mixins.aqtweaks.astral.json` |
| `mixin/astral/MixinWorldGenAttributeStructure.java` | Settle land after `generateAsSubmergedStructure`; small shrine/ruin use walkway Y + swamp fill |
| `mixin/MixinWorldGenLakes.java` | Skip vanilla water lakes on village 12-pad. Required `mixins.aqtweaks.json`. Remap **true**, MCP `generate`; lake fluid via `block` / `field_150589_a` (no `@Shadow block` — Unimined maps that MCP name to the wrong SRG). Lava lakes are not skipped |
| `mixin/biomesoplenty/MixinGeneratorLakes.java` | Skip BOP water and quicksand lakes on village overlap. Optional `mixins.aqtweaks.biomesoplenty.json` |
| `ArcanaQuestTweaksConfig.RtgModuleConfig.surface` | `config/arcanaquesttweaks/aqtweaks_rtg.cfg` |
| `mixins.aqtweaks.json` | Required: village spawn/start/world/inside, `MixinWorldGenLakes`, `MixinStructureVillagePieces`, `MixinStructureStartVillagePaste`, `MixinChunkGeneratorRTGVillage`, `MixinGenericVillageCreationHandler` |
| `mixins.aqtweaks.charm.json` | Optional: Charm ASM village paste skip |

Related but separate: `MixinChunkGeneratorRTG.java` fills Deepslate below Y=0 for Depths. Do not conflate with village flatten. See [depths.md](depths.md).

## Current flatten algorithm

Entry: `MixinChunkGeneratorRTGVillage.aqtweaks$flattenNoise`, once, immediately before `generateTerrain`. RTG passes `landscape.noise` into that call; mutating the cached landscape is the array terrain uses. There is no second `@ModifyArg` pass.

### Footprints

- **Land boxes:** surviving houses, RC (full `.rcst` size), well, and roads that were **not omitted**. Flatten does **not** fall back to the unsnapped start AABB. A road is omitted from layout (and thus from the plate) if **any** column is ocean/river **biome**, or if at least half its columns are lake. RTG river *noise* on desert/mesa land does not drop the path.
- **No village-wide rectangle.** Each column uses Euclidean distance to the **nearest land component AABB** (including paths). Empty corners of the start AABB stay hills.
- **12-block component pad:** `dist ≤ villageComponentPad` (default 12) is **100% plate** at well Y, then `VillageShoreMask` (open jetties, optional 1-block ocean close). Overlapping pads fill grass between roads and houses when pieces are ≤24 blocks apart. Same rule for **flooded non-ocean** columns (swamp water, plains lakes). Inside this pad (and the shrine pad), flatten/seal skip only ocean/river **biome** except 1-block close — dry RTG river noise still plates. Outside the pad, `landscape.river > 0.4` still never-raise (no sand piers).
- **Outer Hermite:** `pad < dist ≤ pad + villageEdgeFalloff`. Smoothstep (`3t²−2t³`) plate → raw RTG. Default falloff **12**. Live cfg may still have **48** until edited — set it to 12 if yards still ramp.
- **Village shrine:** not a land-component pad source. 100% plate inside AABB; extra full-plate radius `smallShrinePad` (3). In town, nearby road/house 12-zones already cover the yard.
- **Building boxes:** land boxes minus roads. Used only for swamp raise ramps **outside** the hard pad. Shrine raise radius 3; others `max(xzPad, waterBank)` (16).

`distanceToBoxXZ` is 0 inside the AABB and Euclidean outside (rounded pad corners).

### Column rules (in order)

1. **Ocean-like or river biome** → never write, except a **1-block** notch inside the hard pad (cardinal-enclosed or 7 of 8 land neighbors) when Village Shore Close Ocean is on. Pure beach is **not** ocean. Ocean-like names (`kelp`, `coral`, `reef`, …) win even if the biome is also tagged BEACH. Inside the hard pad, RTG river **noise** on a land biome is not this skip.
2. **Flooded swamp-like**, `dist ≤ pad` from a land component (or shrine pad 3) → 100% plate, at least Y 64. This is the in-between grass in swamp villages.
3. **Flooded swamp-like**, in the outer Hermite band → blend plate → original water. Water bank can ease that toward skipped ocean/river.
4. **Flooded swamp-like**, outside that, inside building raise radius → swamp-water approach ramp (not in-village yards).
5. **Flooded non-ocean (plains lake, etc.)**, `dist ≤ pad` → 100% plate (build up from the water). Lake **outside** the pad stays water.
6. **Flooded** otherwise → skip. Includes flooded beach so we do not build sand piers into ocean.
7. **Dry**, `dist ≤ pad` from a land component → 100% plate Y, then **shore mask**: opening removes 1-block jetties (raw RTG); interiors (`dist == 0`) stay plated.
8. **Dry**, in the outer Hermite band → blend plate → raw height.
9. **Dry**, in that band, near skipped water → **water bank** over `villageWaterBank` (16).

Dock water (ocean/river roads omitted from land boxes; mostly-lake roads omitted) stays water. Land within pad of a house or mixed road still plates.

After `generateTerrain`, caves and ravines can punch the plate. Before `new Chunk`, **shore-mask** columns are refilled solid up to plate Y (stone near bedrock, dirt). Plate top stays what RTG placed (sand, grass, …). Only `biomesoplenty:mud` is replaced with loamy grass (`biomesoplenty:grass` meta 2). Cave holes at plate Y use the biome `topBlock`. Pad columns on the **8-connected rim** of that mask (`Village Ocean Wall`, default on) overwrite `y = 1 .. plateY-1` with stone brick. Inland height drops are not bricked.

### Plate Y

Sampled from the live `ChunkGeneratorRTG.getLandscape` (the mixin `this`), **not** `World.getChunkProvider()`, which often is not an RTG instance and returned 0/NaN.

- Usable height: not NaN and `> 1`.
- Dry land: `max(sampled well height, villageMinWellHeight)` (default 64). Failed dry samples are still **not** floored to 64 (that made hills into sea-level mesas).
- Never-raise well that walked inland: sample the **dry** well column, not the riverbed.
- Never-raise well (ocean/river) that did not walk: skip (do not cache), except swamp → min well Y.
- Failed well sample: swamp → min well Y (and cache). Otherwise skip that village this chunk and **do not cache**. Do not sample land-box centers (that locked hill houses to a different Y than the well).
- Swamp well below min height: plate at min well height.

Cached per world seed + **well chunk** `(startChunkX, startChunkZ)` in `VillagePlate.HEIGHTS`.

### Recursion guard

`VillageLandHelper.SAMPLING` counts `getLandscape` samples (`pushSampling` / `popSampling`). `getNewerNoise` must not layout villages while sampling, or flatten ↔ noise recurses. Flatten’s own `getLandscape` is sampled so it cannot re-layout. `pushColumnLandscapeCache` / `popColumnLandscapeCache` reuse landscape per chunk during `landBoxesOf` / `wetFraction` only. Piece skip, flatten columns, and populate abort use live `isNeverRaiseAt` (no ThreadLocal cache).

## Placement rules

### Well veto (`startRejectReason`)

Reject only if the well column (`chunkX * 16 + 2`, `chunkZ * 16 + 2`) is **never-raise** (ocean-like biome, river biome, or RTG `landscape.river` &gt; `STRONG_RIVER`) **and** there is no dry column within `villageWaterRetryDistance`. If a dry slot exists, keep the Start (structureMap key stays the original well chunk) and offset pieces to that XZ (`well-walk` in the log).

A dry land well below `villageMinWellHeight` is **not** rejected; the plate is `max(raw, knob)`.

A dry land/beach well is rejected if **ocean-like** is closer than `villageCoastBuffer` (`coast_ocean`). Nearby river does **not** cancel (`coast_river` is gone). `0` = well column only.

The veto always runs. After layout, `forgetRejectedStarts` removes only true rejects from the map and `VillagePlate`. Walked wells stay. `isRtgLandscapeLake` must not nested-`getLandscape` while already sampling.

If an existing `aqtweaks_rtg.cfg` still has Coast Buffer **32**, Forge keeps that saved value.

### Houses, RC, and village shrine

`isBuildingWet`: ocean/river biome or RTG river always wet (retry inland). Swamp-like and low dry land (noise below min well Y) are not building-wet; that land is raised to the plate. `isFloodedAt` still treats lakes as flooded so mostly-lake paths are omitted.

Retry walks inland (`villageWaterRetryDistance`, default 20): street slots, then toward the well, then a spiral around the well. A path that touches ocean/river or is **at least half** wet retries inland the same way; if every slot still fails, it is omitted (no lake bridge). A forest path with a puddle stays. The Astral small-shrine village piece uses the same wet skip/retry. At populate, Charm `ASMHooks.addComponentParts` (and vanilla `MixinStructureStartVillagePaste` if Charm did not wrap the invoke) skips a non-road building only if **every** clipped column is never-raise. Leftover lakes still paste so a shrine/house that spans chunks is not sliced. Layout omission is the real drop.

**Waystones:** `ComponentVillageWaystone` is not retried as a random house. A wet (never-raise) waystone is rebuilt inland (street, then toward the well, then a spiral around the well, all four facings). Failed wet retries are removed from the start lists. Ocean/river still never get a plate. Waystones’ own `villageChance` can still skip a village; this only keeps a rolled waystone from being deleted.

Building wet tests sample **every column** in the AABB (not stride 2). Wet tests must sample live RTG `landscape` (`VillageLandHelper.pushGenerator` during layout). `World.getChunkProvider()` is often not `ChunkGeneratorRTG`.

### Swamp-like vs ocean-like

`isOceanBiome` order (avoid treating reef as beach, or swamp as WATER-ocean):

1. Name contains `ocean`, `kelp`, `coral`, `reef`, `atoll`, or `lagoon` → ocean-like. Covers BOP **Kelp Forest** (`biomesoplenty:kelp_forest`) even when BOP itself has `canGenerateVillages=false`.
2. Pure **BEACH** (type or name `beach`) → not ocean.
3. `Type.OCEAN` → ocean-like.
4. Swamp-tagged (type or swamp names) → not ocean.
5. `Type.WATER` → ocean-like.

`isSwampLikeForRaise` is swamp names/types **and not** ocean-like/river. Names include swamp, marsh, bog, wetland, bayou, mangrove, fen, moor, peat, muskeg, plus `BiomeDictionary.Type.SWAMP`.

If a pack tags mangrove as OCEAN, it is treated as ocean (never raise). That matches “never pave ocean.”

Flooded for paths = never-raise **or** RTG lake (`noise < villageMinWellHeight`, default 64). Never-raise columns are never filled. Config `villageMinWellHeight` is live.

## Structure detection

`MixinMapGenVillageInside` treats the **flatten plate** as village for `isInsideStructure`: AABB expand of each land box by `villageComponentPad + villageEdgeFalloff` (and shrine pad + falloff). Y is **well shaft floor through plate + villageBoxHeight**. After generation, Tweaks also appends non-placing `AQTVillagePlate` children to the Start so `Village.dat` stores those boxes next to houses/paths; vanilla `isVecInside` then matches after relog. Mixin still covers towns generated before the stamp. Unsnapped well template `64..78` uses `plate - 14`. Template start Y (`minY=64 maxY=151`) is not used as the only test.

Flatten does **not** use `villageBoxXZPad` as extra 100% plate or as detection; flatten uses per-component distance (`villageComponentPad`, default 12). `villageBoxXZPad` is swamp dock-approach only. Live `villageEdgeFalloff` may still be **48** (Forge keeps saved cfg); code default is 12. `written=256 pad=0` is falloff-only blend, not a missing component pad.

## `/aqvillage` (OP)

Permission level **2** (same as `/locate`). Player sender only.

- `/aqvillage` — nearest **unexplored** allowed well in 16 village cells; else nearest known allowed Start.
- `/aqvillage unexplored` — unexplored only; errors if none in range.
- `/aqvillage known` — nearest already-generated allowed well.

Teleport is **on the generated ground** at that column (`world.getHeight`, skip leaves), about 6 blocks off the well. It does **not** use the well-column HEIGHTS cache (that Y can sit inside a hill or below the walkable plate). Search does not layout villages; arriving generates the chunk.

`MapGenVillage` is stashed when `ChunkGeneratorRTG` is constructed (and again on layout), keyed by World and by `seed+dimension`. Unwrap BFS walks wrapped chunk providers/generators. A miss logs one INFO line to `latest.log` (`[aqvillage] missing MapGenVillage …`) then throws. Nether/Aether still have no village generator.

## Config (`aqtweaks_rtg.cfg` → Surface)

| Name | Default | Live? | Meaning |
| --- | --- | --- | --- |
| Enable RTG Village Terrain Smoothing | true | yes | Master flatten + layout-first |
| Village Component Pad | 12 | yes | Full plate around each land component, including roads. Overlap fills yards |
| Village Edge Falloff | 12 | yes | Hermite **beyond** the component pad. Live cfg may still be **48** — set to 12 if yards ramp |
| Village Water Bank | 16 | yes | Outer-rim ease toward skipped ocean/river; 0 = old waterline cliffs |
| Village Ocean Wall | true | yes | Stone brick on 8-connected shore-mask rim, below plate top |
| Village Shore Smooth | true | yes | Open 1-block jetties on the coastal plate; interiors stay plated |
| Village Shore Smooth Radius | 1 | yes | Chebyshev opening kernel. `0` = no opening |
| Village Shore Close Ocean | true | yes | Fill 1-block ocean/river notches in the hard pad (enclosed or 7/8 neighbors) |
| Village Plate Slope | 0 | yes | Extra dome from box center; 0 = flat |
| Skip Water Village Pieces | true | yes | House/RC/shrine/waystone/path retry inland; omit leftover ocean/river or mostly-wet paths; paste abort |
| Village Water Retry Distance | 20 | yes | Retry walk |
| Reject Coastal Village Starts | true | yes | Well veto: never-raise with no dry slot; ocean coast buffer. Walk river/ocean wells inland |
| Village Min Well Height | 64 | yes | Dry well / lake plate floor. Live DEVBOX cfg already 64 |
| Village Coast Buffer | 16 | yes | Chebyshev; veto dry well if ocean-like closer than this. Nearby river does not cancel. `0` = well column only |
| Enable Village Bounding Box Detection | true | yes | Flatten 12-pad (yards + kept paths) as Village |
| Village Box XZ Pad | 8 | yes | Flatten swamp dock-approach only. Not detection |
| Village Box Height | 30 | yes | Detection Y above plate. Floor is the well shaft (~11–14 below plate). Live cfg may still be **32** |
| Enable Village Relight | true | yes | After populate paste, re-check light at torches/lamps in that chunk clip |
| Village Flatten Debug | false | yes | `logs/villagepatch.log`. Live DEVBOX must be edited off; old true is kept until changed |
| Skip Structures On Village | true | yes | Cancel AS surface shrines and Cambion houses on village AABB. MW hut/barrow and Bewitchment circle/menhir/wickerman skip that spot and retry nearby. Vanilla water lakes and BOP water/quicksand on the village pad are skipped |
| Enable Structure Land Settle | true | yes | Fill under those structures and ramp the rim |
| Enable Astral Shrine Settle | true | yes | Village-skip + land settle for surface shrines |
| Enable Cambion House Settle | true | yes | Village-skip; house +1 (cobble on grass); skip air; pad at plains Y hole-fill only |
| Enable Astral Small Shrine Village Piece | true | yes | At most one small shrine as a village building |
| Enable Mystical Hut Settle | true | yes | Hut village-skip/retry + land settle. Barrows skip/retry when Skip Structures is on; no barrow plate |
| Structure Fill Depth | 16 | yes | Max blocks filled down under a pad |
| Structure Rim Bank | 16 | yes | Slope from large shrine / hut pad to land |
| Small Shrine Pad | 3 | yes | Buffer around small shrine/ruin settle and village shrine AABB |
| Cambion House Pad | 6 | yes | Full plate around Cambion house AABB |
| Cambion House Falloff | 12 | yes | Hermite beyond Cambion pad, raise-only |

## Post-terrain structures (not village flatten)

Astral surface shrines, Bewitchment Cambion houses, and Mystical World huts/barrows paste **after** RTG terrain. They cannot reuse village noise flatten.

- Overlap a village (real AABB / Y, not chunk origin at Y=0) → Astral/Cambion **do not place**. Huts, barrows, stone circles, menhirs, and wickermen **retry nearby** (Chebyshev step 8, up to 32); miss → skip and do not mark. Vanilla `WorldGenLakes` water and BOP water/quicksand lakes whose ~16×8 blob overlaps the 12-pad are cancelled. Lava lakes still generate.
- **Large ancient/desert shrines:** fill under the footprint with raw `astralsorcery:blockmarble` (`MarbleBlockType.RAW`), rim still biome top/filler, max depth 16, rim 16, never ocean/river. After settle, clear wood/leaves/vines in the template AABB through `maxY + 16`. Treasure caves are not settled.
- Mystical **huts:** biome fill under the footprint (min foundation Y), rim 16, never ocean/river. **Barrows:** skip/retry only, **no plate**.
- **Small shrine and small ruin:** plate Y is the generate **center / walkway**, not min foundation Y. Rim is `smallShrinePad` (3), not the 16-block large-shrine bank. In **swamp-like** biomes, water is filled up to that plate. Ocean/river still never filled. Plant-like blocks (BOP / Rustic / Farmer’s Delight, `BlockBush`, `Material.PLANTS`) are overwritten; leftover tops above the plate are cleared. Logs and leaves are not (small only).
- **Cambion houses:** paste at ground Y **+ 1** so cobble/step sit **on** the grass (`canSpawnHere` still at ground Y). Schematic **air is not placed**. Village overlap still cancels. The 6-pad stays at **plains Y**: fill air/plants/liquid up to ground Y only (no mesa). Hermite 12 raises dips toward that Y, never above. Door is one above cobble. Ocean/river never filled; swamp water in the pad may fill.
- **Village piece:** at most one Astral **small shrine** (not the ruin) via Forge `IVillageCreationHandler` (`AQTSmallShrine`), weight **5**, limit 1 (uncommon). AABB is the unrotated pattern min/max so every marble chunk intersects the piece. Layout allows overlap with the attaching **Path**; inland retry if it hits a building. Wet/omit is ocean/river **biome** only (not RTG river noise). Flatten plates its AABB with at most 3 blocks of extra full plate; nearby road/house 12-zones still own the yard. Liquid blocks get flag 3 + `neighborChanged` so lantern water flows. Ocean/river **biome** columns are not pasted; leftover lake and river-noise sand still paste. After paste, **liquid-only** fill inside the AABB (no `settlePadded` dirt collar). Wild shrines still spawn and keep pad 3; overlap skip prevents a second shrine on the same village.
- Bewitchment **stone circles / menhir / wickerman** use the same Chebyshev retry as MW barrows. No extra plate. Village Hedge Witch/Alchemist pieces are still village buildings.

Mixins: `mixins.aqtweaks.astral.json`, `mixins.aqtweaks.bewitchment.json`, `mixins.aqtweaks.mysticalworld.json`, `mixins.aqtweaks.biomesoplenty.json` (`required: false`). Village flatten/retry mixins are in required `mixins.aqtweaks.json`.

### Structure land settle (`StructureLandSettle`)

Used after Astral surface shrines, Mystical huts, and Cambion houses place. Server-only.

- Under the footprint: biome `fillerBlock` (else dirt), except **large Astral** uses raw marble. Top of the rim (not under-structure) uses `topBlock` (else grass). Stop on non-fillable solid or on liquid unless swamp-fill is on.
- **Never write** `isNeverRaiseAt` (ocean-like / river biome **or** RTG river). Swamp-like liquid may be replaced when `fillSwampLiquid` is true (small shrine/ruin walkway Y, Cambion pad).
- **Fillable:** air, snow layer, tallgrass, flowers, double plant, lily, `Material.PLANTS` / `VINE` / `CACTUS`, `BlockBush` / `BlockReed` / `BlockVine`, `isReplaceable`. **Not fillable:** leaves, wood, rock (except large-temple foliage clear, which is a separate pass).
- After fill, plant-like blocks from plate Y through plate+3 are cleared to air. Logs/leaves stop that plant clear.
- Large temples: `clearFoliage` removes wood/leaves/vines in the template AABB through `maxY + 16`.
- Rim: Euclidean distance to the AABB, smoothstep over `structureRimBank` (16) for huts and large shrines, `smallShrinePad` (3) for small shrine/ruin, or `cambionHouseFalloff` (12) beyond the Cambion hole-fill pad toward **ground Y** (not above plains). Only raises toward plate (will not dig). `settleTemplate` uses origin Y as floor after rotation AABB.

`StructureVillageOverlap` tests real AABB/Y against remembered village starts (not chunk origin at Y=0). Treasure shrines are excluded from the Astral skip **and** settle mixins.

## Debug log

`Village Flatten Debug` writes `logs/villagepatch.log` (instance cwd), truncated each launch. Default **off** — appending every line stalls chunk gen.

Useful lines:

- `register chunk=... biome=... landBoxes=... buildings=...` — start remembered
- `layout cell=... origin=... wellChunk=... hit=yes|no` — well chunk laid out for flatten
- `veto chunk=... ocean_well|river_well|coast_ocean ...` — well rejected (open ocean / no dry slot)
- `well-walk chunk=... from=... to=...` — never-raise well kept; pieces offset inland
- `plateSample … target=64` — dry low well raised to min well height (not `flooded_well`)
- `plate Y=... landBoxes=... componentPad=... falloff=...` — per-village flatten shape
- `flatten chunk=... boxes=... dry=... wet=... written=... pad=... raised=...` — writes this chunk
- `house` / `rc` / `astral shrine` retry hit/miss
- `waystone aabb wet` / `waystone relocate hit|miss` — same gazebo moved inland
- `path aabb wet ... retrying inland` / `path retry hit` / `path retry miss ... omitted` — wet path moves inland; leftover ocean/river or lake-bridge is dropped
- `village piece skip water floor` — paste refused because the whole clip is never-raise
- `astral` / `cambion skip village overlap` / `mystical hut|barrow skip|relocate` — post-terrain skip or retry
- `cambion plate at=...` — Cambion pad written
- `astral small shrine village piece at=...` — shrine pasted as a village building
- `seal chunk=... wall=` — pad reseal; `wall` is ocean-rim brick columns

`once(key)` logs a given village/chunk at most once per session.

## Design history (do not regress)

### 1. Raise everything inside the padded hull

Padded land boxes were 100% plate, and **ocean inside the pad was raised too**. Playtest: blocky sand platforms from beach/ocean villages into the sea. Inland plains looked correct.

**Fix:** never write ocean/river, even inside a piece box. Do not treat `inPad` as a license to fill water.

### 2. Floor plate Y to 64

Every `plateSample` was `raw=0.0 target=64.0`. `Reflect.getChunkGenerator` was not `ChunkGeneratorRTG` (wrapper). Failed/zero samples were floored to 64, so hills became sea-level mesas.

**Fix:** sample `getLandscape` on the live generator. Do not floor a failed dry sample to 64.

### 3. Extra 8-block pad as extra flat plate

The XZ pad made a larger mesa whose last land column was still full plate height; the next column was skipped water → 2–5 block dirt/sand cliffs (swamp `79,80,1609`, beach `5673,74,2156`). `waterFactor` also *weakened* falloff next to water, which made the cliff sharper.

**Fix:** 100% plate = unpadded land boxes. Falloff 48 to hills. Water bank 16 pulls the rim down to shore. Swamp pad is a ramp, not a cylinder.

### 4. Whole-road AABB vs per-column skip

Excluding a road that *touches* ocean drops the land half of that path from the plate. Including flooded roads in the hull lets a dock AABB plate a sand strip beside the dock.

**Current compromise:** omit **fully flooded** roads from land boxes (`isAabbFullyFlooded`); skip ocean/flooded **columns** at flatten time. Dry road-only streets still plate. See (7) for the any-column bug.

### 5. Flooded beach counted as dry

`isRtgLandscapeWet` used `World.getChunkProvider()`’s generator, which is often not RTG. Null landscape → not wet. Beach-tagged water accepted shrine and waystone AABBs on the coast.

**Fix:** push the live `ChunkGeneratorRTG` during layout and sample that. Village shrine paste also skips ocean/river liquid as a last resort.

### 6. Wet waystone became a random house

Retry called `func_176066_d` again, which picks another piece. Waystones is limit 1, so the village lost its gazebo.

**Fix:** rebuild `ComponentVillageWaystone` inland (street, well, spiral). Do not pave water.

### 7. Any-flooded road omitted from the plate

`landBoxesOf` dropped a path if **any** sampled column was flooded. At sea-level forest (`-524, 64, 5893`) that was almost every road. Houses still plated to 64; dirt paths and lamps sat one block lower.

**Fix:** omit the road only if **every** sampled column is flooded (true dock). Mixed/dry paths plate with houses. Per-column skip still leaves actual water.

### 8. WATER / coral / kelp not treated as ocean

Well veto used `Type.OCEAN` and a short name list. BOP **Coral Reef** is often `Type.WATER`, and a sand speck can be BEACH, so a well spawned in the reef (`-3452, 63, -2191`). Buildings used the same biome test and sat on water. Kelp Forest is the same class of biome.

**Fix:** ocean-like includes `Type.WATER` (not swamp) and names `kelp` / `coral` / `reef` / `atoll` / `lagoon`. Pure beach is still allowed. Coast buffer 16 vetoes a well only if ocean-like or river is closer than 16; a well 16+ from water may start. Wet buildings retry inland.

### 9. Layout generated cell origins, not wells

`layoutVillageGrid` called `generate()` on `cell * spacing`. Vanilla wells use `setRandomSeed(cellX, cellZ, 10387312)` then offset by `nextInt(distance - minTown)`. Outlying chunks generated before the well chunk had no `Start`, so flatten no-oped. Plains villages on hills stepped (`-4031` plaza vs `-3962` tower, ~7 blocks) and RC buildings kept dirt cliffs at their far face.

**Fix:** generate the seeded well chunk per nearby cell. Keep radius 8; do not scan 289 neighbors.

### 10. Per-piece plate and swamp flooded skip

100% plate was each land AABB; yards followed 48-block falloff toward raw RTG. Flooded swamp columns ignored land boxes and only raised around buildings, so mixed swamp roads pitted.

**Attempted fix:** dry 100% plate as an unpadded land **hull** (union rectangle). Flooded swamp inside a piece AABB plated. Yards that were wet but between a road and a house (inside no AABB) stayed a basin.

### 11. Union hull vs component pad

The hull flattened empty wilderness corners of the start AABB, and still left swamp grass between path and house (shot: path/farm plated, 2–3 block drop in the yard). A Hermite-only 12 from the AABB would not flatten that yard (mid-gap blend ~0.5).

**Fix:** 100% plate for `dist ≤ pad` from the nearest land component **including roads**, dry and flooded non-ocean. Default pad **12**. Hermite only beyond that pad. No village-wide rectangle. Ocean/river roads are omitted. Shrine extra full plate is 3.

### 12. Pieces generated on water (waystone + plank path)

Stride-2 layout samples treated a 5×5 gazebo on a sand speck as dry. Roads were never skipped, so vanilla turned a lake path into a wooden dock. Flatten correctly refused to pave the lake, so there was no land plate under those pieces.

**Fix:** sample every AABB column; relocate/skip wet buildings inland; wet paths retry inland then omit leftover docks; paste-abort if the surface is still liquid.

### 13. River pieces, AABB-only lake fill, caves after terrain

Swamp–river villages still pasted roads/houses on F3 River (paths only omitted at 50% wet). Flooded non-swamp fill was **AABB-only**, so yards and roads over swamp water stayed sunken. RTG caves/ravines run **after** `generateTerrain` and cut the plate (well over a chasm, floating lamps). Load-time `forget flooded_well` treated a **null** landscape as wet and dropped dry plains Starts.

**Fix:** omit any path that touches ocean/river; fill all non-ocean water inside the hard pad; reseal pad columns after caves/ravines; null landscape is **not** wet (unknown). Ocean/river biome tests still omit.

### 14. All villages well-only (`/locate` empty)

`registerVillages` at `func_185932_a` HEAD plus `isRtgLandscapeWet` treating null/nested samples as wet while the generator was on the stack omitted all four well roads on dry plains (`landBoxes=1 buildings=1`). Vanilla then skipped paste (`isSizeableStructure` false). `/aqvillage` could not see `MapGenVillage` on a wrapped chunk generator.

**Fix:** unknown landscape is dry; layout after `getNewerNoise` RETURN; unwrap nested `IChunkGenerator` to find RTG / `MapGenVillage`. New chunks only — already visited ghost wells stay empty.

### 15. River well re-register, over-veto, mud pad, `/aqvillage`

Biome provider at the well said plains while F3 was River (`landscape.river`). `flooded_well` (noise &lt; 64) plus `coast_river` vetoed ~260 cells per session; `/locate` stuck on one leftover Start. The river well then `forget` + `register` again, plate Y = riverbed 53.5, BOP mud as pad top. `/aqvillage` still missed wrapped generators.

**Fix:** never-raise = ocean/river biome **or** RTG river. Dry land below `villageMinWellHeight` (64) is kept and raised. River/ocean wells walk inland or veto only if no dry slot. Pad top is native RTG surface; `biomesoplenty:mud` becomes loamy grass (`grass` meta 2). Stash `MapGenVillage` + RTG per World for `/aqvillage`. Detection Y includes the well shaft under the plate. `/aqvillage` stands on the plate, ~6 off the well.

### 16. `rememberAll` on every non-village chunk

Flatten and seal called `VillagePlate.rememberAll` whenever `overlappingRecords` was empty. That is most chunks. Each miss walked every `structureMap` Start, then `landBoxesOf` scanned every road column and sampled RTG landscape. Cost grew with how many villages the player had already generated. TPS collapsed while flying new terrain.

**Fix:** `ensureStarts` only when Tweaks’ list is empty. Mixin well-walk **replaces** that Record. `rememberAll` / `rememberNearby` are `rememberIfAbsent`. Landscape ThreadLocal is only for `landBoxesOf`. Public `isNeverRaiseAt` is live. Populate abort uses `isNeverRaiseAt`, not biome-only.

### 17. Cambion float, barrow-in-village, dirt temple pads

Cambion Y+1 on steep RTG left air under the cobble. Barrows were not village-skipped. Large Astral settle used biome dirt; trees clipped the dome.

**Fix:** Cambion 6-pad + Hermite 12 after Y+1 paste. Hut/barrow Chebyshev retry. Large temples: raw marble under-fill + foliage clear through `maxY+16`.

### 18. Stepped hill plates / dropped chunk

Flatten returned when `overlappingRecords` was empty even if the chunk sat in the start AABB (`no-land-boxes`). A well-only or pre-walk snapshot (AABB-keyed duplicate Records after well-walk) left house chunks as raw RTG while neighbors plated to well Y — chunk-aligned stone cliffs, a forgotten 16×16 rectangle, pieces at different heights. Plate Y also fell back to a **land-box center**, so the first hillside house could cache a different village Y than the well.

**Fix:** one Record per well chunk; HEIGHTS on that key; well column only (swamp → min well Y); flatten/seal merge AABB-miss villages and rebuild unlocked boxes from the live Start once; still no hull flatten of empty AABB corners.

### 19. `forgetRejectedStarts` / `villageHits` every chunk; sliced shrine; sand ocean cliff

`layoutVillageGrid` re-ran `startRejectReason` on every Start every chunk (`findDryWell` retry 32). Flatten/seal `villageHits` always `rememberNearby` then scanned start AABBs. Flare `xnAtr3x33E`. Village shrine paste aborted a whole chunk on leftover lake. Plate-to-ocean face stayed RTG sand/stone/ore.

**Fix:** forget-once cache; land-box hit returns immediately (flatten still **refreshes unlocked** boxes once); seal does not recover; paste skip only if every clip column is never-raise; shrine AABB from pattern; stone-brick rim on never-raise-adjacent pad columns.

### 20. Desert/mesa toothed plate; Cambion trench; post-terrain collisions

RTG `landscape.river > 0.4` skipped flatten inside village pads and omitted desert paths, so 12-pads did not meet (sand islands, toothed red-sand yards). Cambion settle used `floorY = pasteY`, so the solid top sat one below cobble. Stone circles pasted through farms. BOP quicksand lakes carved village squares. Village shrine `settlePadded` wrote a dirt collar.

**Fix:** inside the component/shrine pad, skip only ocean/river biome; path omit the same; flatten refreshes unlocked Records once. Cambion `floorY = pasteY + 1`. Circle/menhir/wickerman Chebyshev retry. BOP `GeneratorLakes` quicksand skip on village overlap. Village shrine liquid-only fill, no pad.

### 21. `/aqvillage` after relog; village shrine never placed

Stash of `MapGenVillage` only ran during new-chunk layout. Relog in old terrain emptied `WeakHashMap<World>`; unwrap missed wrapped providers. Chat `No village generator on this world`. Village `AQTSmallShrine` `build` died on Path `findIntersecting`, then wet-tested RTG river noise on the large AABB, so a pick often could not place. Weight stayed 5.

**Fix:** stash at RTG construct and by seed+dim; BFS unwrap; INFO log on miss. Shrine layout allows Path overlap, retries inland on a building hit, uses biome ocean/river for wet/paste skip.

### 22. Jagged coastal plate; patchy stone brick

The 12-pad stayed 100% plate up to the ocean/river biome staircase. `Village Ocean Wall` only bricked cardinal never-raise-biome neighbors, so diagonal and beach-water faces kept RTG sand/stone/dirt.

**Fix:** `VillageShoreMask` opens 1-block jetties (piece AABBs stay plated), fills 1-block ocean/river notches in the pad, and bricks the 8-connected rim of that mask.

### 23. Cambion pad sunk or mesa

Y+1 paste stacked a ground layer above plains. Template air at y=0 punched pits. `settlePadded` dirt-filled the 6-pad as `underStructure`.

**Fix:** paste house at ground Y+1 (cobble on grass); skip schematic air; `fillHolesPadded` only raises air/liquid up to **plains Y** (pad flush, never a mesa).

### 24. Yards not Village

`isInsideStructure` used land AABBs + 8, so grass between a path and a house missed.

**Fix:** detection and `StructureVillageOverlap` use the flatten hard pad (Euclidean 12 to land boxes, shrine 3). `villageBoxHeight` default 30.

### 25. Water lakes carving the plate

Populate `WorldGenLakes` (and BOP water lakes) ran after village paste and punched 6–16 block ponds through yards and house foundations.

**Fix:** skip water lakes when the generate blob overlaps the village 12-pad (`Skip Structures On Village`). Same as BOP quicksand. Lava lakes unchanged.

### 26. `WorldGenLakes` SRG shadow under CleanroomRemapper

`remap = false` plus `@Shadow field_150589_a` failed at boot: Cleanroom 0.6.12 applies mixins to MCP-named vanilla, so the field is `block`. Mixin apply marked `WorldGenLakes` invalid; Lycanites `WorldGenOozeLakes` then crashed as a follow-on. Remap true plus `@Shadow private Block block` is also wrong: Unimined maps the generic MCP name `block` to `field_150556_a`, not `WorldGenLakes.field_150589_a`.

**Fix:** remap true and MCP `generate` (refmap `func_180709_b`). Read the fluid with a field walk (`block`, then `field_150589_a`) so Lycanites subclasses still resolve the field on the vanilla super. BOP `MixinGeneratorLakes` stays `remap = false` (mod class, SRG).

### 27. Village detection in `Village.dat`; `/aqvillage` into ground

Pad/height mixin needed this-session flatten cache, so yards missed after relog. `/aqvillage` used well-column HEIGHTS even when that Y was inside a hill.

**Fix:** append non-placing `AQTVillagePlate` children (pad + Hermite AABB, well floor through plate + box height) after layout; houses/paths/RC keep their own boxes. Mixin fallback samples plate if uncached. `/aqvillage` stands on generated surface at the stand column.

### 28. Village torches not lighting the plate

Populate placed lamps with flag 2; block light did not flood (1-block puddles, dark houses).

**Fix:** after `MapGenVillage.Start` paste for a chunk clip, `checkLight` every emitting block in that AABB. No flatten or detection changes.

## Playtest reference (this line)

- **Wanted:** inland plains village (example `-2897, 97, -2119`) — flat plate, houses on it, blend to hills.
- **Wanted:** sea-level forest (`-524, 64, 5893`) — dirt path, lamps, and houses on the same Y.
- **Wanted:** beach/land well ~16 from water — village starts; buildings retry inland, not on the water.
- **Wanted:** small Astral shrine/ruin — land buffer at most 3 around the marble, not a 16-block mesa.
- **Wanted:** large Astral ancient/desert temple — raw marble under the pad, not dirt; no floating logs/leaves in or above the AABB.
- **Wanted:** new Cambion house on a slope — pad flush with plains; cobble on the grass (house +1); door +1 above cobble; air under the footprint filled.
- **Wanted:** Mystical barrow or hut next to a village — relocates or skips; barrow is not plated.
- **Wanted:** grass between a dirt path and a house is Village (`isInsideStructure`); Hermite skirt is Village; beyond pad+falloff is not. Y well floor through plate + `villageBoxHeight`. Relog still Village (pad boxes in `Village.dat`).
- **Wanted:** new village at night — path torch and fence lamp light the plate and nearby walls (not a 1-block puddle).
- **Wanted:** hill village — well, houses, RC, and path pads at **one** well Y; Hermite only outside the 12-pad; no chunk-aligned stone wall through town.
- **Wanted:** desert/mesa village — one sand plate through yards and paths; no toothed red-sand holes between houses; F3 River biome still unplated.
- **Wanted:** new Cambion house — pad flush with grass; cobble one above that; door one above cobble; no pit; no extra pad layer.
- **Wanted:** Bewitchment stone circle / menhir / wickerman next to a village — relocates or skips; farms intact.
- **Wanted:** wild BOP quicksand still generates; none inside desert village land boxes.
- **Wanted:** small water ponds still generate off the village pad; none through yards or house foundations. Lava lakes still generate.
- **Wanted:** village Astral small shrine **capable of generating** (weight 5, not every village); when it does, complete on the plate, no dirt/grass collar; true ocean/river biome columns still skip.
- **Wanted:** coastal plate — cleaner top-down outline (no 1-block sand jetties; 1-block ocean notches in the pad filled); stone-brick on the whole 8-connected rim below the top; sand/grass on top; inland hill cliffs still dirt/stone.
- **Unwanted (fixed in flatten, verify on new chunks):** beach sand piers into ocean (except 1-block enclosed notches); ocean ledges; swamp/beach vertical plate walls into water; 1-block grass pads under houses with path one lower; village well in coral reef / kelp forest / open ocean; **river well in the water** (walk inland); plains hill villages stepping instead of one pad; **chunk-aligned stone cliff / forgotten 16×16 in town**; dirt cliff at the far end of a tall RC village piece; in-village grass basins between roads and houses; oak plank path sitting in a lake; **small water pond through village foundations**; houses/roads in F3 River; well over a ravine; floating lamps after cave carve; whole pad forced to loamy grass; leftover BOP mud on the pad; **half marble shrine**; raw sand/ore ocean plate face; **toothed sand/red-sand plate in desert/mesa**; **dirt collar around village shrine**; **BOP quicksand in a village square**; **stone circle through a village farm**; **sawtooth coastal plate / mixed dirt-stone-sand ocean face**.
- Swamp villages still keep pieces in swamp water; that water **inside the 12-pad** is filled to plate Y. Open swamp **outside** the pads stays water. Ocean/river columns are never filled except 1-block pad notches.

## Likely next levers

- `Village Shore Smooth Radius` if the coast still looks toothed (try 2) or too inset (0).
- `Village Water Bank` if ramps are too short/long.
- `Village Edge Falloff`: new default is 12. Existing `aqtweaks_rtg.cfg` with 48 stays 48 until you set 12.
- Houses that sit *on* the waterline still get a flat core (100% plate under the AABB); only the skirt ramps.
- Existing instance cfg may still have Coast Buffer 32 / Water Retry 32 until changed.

## Out of scope unless asked

- Recarving old chunks
- Moving houses already sitting on hills
- Version bump
- Changing vanilla/RC piece sets beyond the small shrine
- Filling ocean/river beyond 1-block pad notches (`Village Shore Close Ocean`)
- Small ruin as a village piece
- Forcing Waystones `villageChance` so every village has a waystone
