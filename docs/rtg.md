# RTG module (1.6)

Last updated: 2026-08-22, village plate: native RTG surface (mud → loamy grass:2 only), `/aqvillage` on plate ~6 off well. Unknown landscape is not wet.

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

- Do **not** pave oceans or rivers. Do **not** place wooden docks: a path that touches ocean/river, or that is mostly flooded lake, is omitted, not converted to planks.
- Houses, RC buildings, and waystones retry inland if they touch **ocean/river** (biome or RTG `landscape.river`). Low dry land (noise below min well Y) is **raised**, not skipped.
- **Ocean-like** (never raise, never well start unless a dry slot exists): `Type.OCEAN` or `Type.WATER` (not swamp), plus names `ocean`, `kelp` (BOP Kelp Forest `kelp_forest`), `coral`, `reef`, `atoll`, `lagoon`. Pure **BEACH** is not ocean. RTG `landscape.river` above `STRONG_RIVER` is never-raise even if the biome provider says plains.
- Wells may sit **16 blocks from the coast** (Chebyshev). Do not veto a land/beach well just because ocean is nearby beyond that. Veto if ocean-like is **closer than** `villageCoastBuffer`. Nearby **river does not** cancel a dry well. A well **in** ocean/river walks inland within `villageWaterRetryDistance`; open ocean with no dry slot is cancelled.
- In **swamp-like** biomes, pieces may stay. Plate buildings, mixed roads, **and the yards between them** (12-block component pad, overlapping). Open-water paths that are mostly lake are omitted rather than docked.
- **Never raise or plate ocean-like or river columns**, even inside a house or road AABB. No village piece may remain on those biomes.
- A **kept** house/RC/shrine/road on flooded non-ocean land (swamp water, plains lake) gets dirt **up to plate Y for the whole 12-pad**. Pads overlap into one village footprint. Lake **outside** the pads stays water. Dry wells below `villageMinWellHeight` are **kept** and the plate is `max(raw, knob)`.
- Keep a **flat plate under dry-land roads**, including stretches with no buildings. Omit a road from the plate (and from layout) if it touches ocean/river or is mostly lake.
- The plate is the **walkable village footprint**: every surviving piece **and** a 12-block hard pad around it. Overlap is one level. It is **not** a village-wide rectangle. Empty AABB corners with no nearby piece stay hills.
- Inland plains/forest (playtest “village 2”) is the target look for non-water biomes.
- Structure detection uses **plated land boxes** plus `villageBoxXZPad`, Y from the **well shaft floor** through plate plus `villageBoxHeight`. It does not use the unsnapped start AABB or template Y `64..151`.

## Hard constraints

- Stay version **1.6**.
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
3. Flatten `landscape.noise` under land boxes (houses, well, mixed/dry roads). Never write ocean/river. Flooded swamp/lake **inside the hard pad** is raised to plate Y.
4. RTG carves from that noise. Caves and ravines then punch the primer. Tweaks reseals pad columns (not ocean/river) solid up to plate Y before `new Chunk`. Production method `func_185932_a` (`remap = false`).
5. Populate places the same pieces. Wet houses/RC/shrine/waystone/paths retry inland; leftover ocean/river or mostly-lake paths are omitted. If layout missed, paste skips the building when the surface is still liquid.

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

Layout must be cheap. `layoutVillageGrid` runs at **RETURN** of RTG `getNewerNoise` (noise already filled) for the current chunk plus the **vanilla well chunk** of each nearby village cell (spacing from the map gen, UT default 25, radius 8 chunks). The well is `cellOrigin + random(0, spacing - minTown)` with seed `setRandomSeed(cellX, cellZ, 10387312)`, not the cell origin. Generating only origins almost never created the `Start`, so hill-side chunks flattened as raw RTG and buildings stepped. It does **not** call `generate()` on all 289 neighbors. Do not layout at `func_185932_a` HEAD — noise is empty there and unknown-as-wet omitted every road.

## File map

| File | Role |
| --- | --- |
| `rtg/VillageLandHelper.java` | Wet tests, swamp/ocean-like/river/beach, well veto, forget rejected Starts, coast buffer, per-column AABB wet/mostly-wet, paste floor skip, grid layout, live RTG noise sample, waystone inland slots |
| `rtg/VillagePlate.java` | In-memory starts, land vs building boxes, plate height cache |
| `rtg/VillageDebug.java` | `logs/villagepatch.log` in the instance folder (not `latest.log`) |
| `rtg/StructureVillageOverlap.java` | Village AABB/Y test for post-terrain schematics (not Y=0). Unwraps wrapped `IChunkGenerator` to find `MapGenVillage` / RTG |
| `rtg/StructureLandSettle.java` | Fill under a placed schematic + rim slope; swamp-liquid fill; overwrite plant-like blocks |
| `mixin/bettercaves/MixinChunkGeneratorRTGVillage.java` | Layout after `getNewerNoise` RETURN + flatten noise + seal pad after caves/ravines |
| `mixin/MixinMapGenVillageSpawn.java` | Well veto (`func_75047_a`) |
| `mixin/MixinMapGenVillageStart.java` | Remember start after create (`func_75049_b`) |
| `mixin/MixinMapGenVillageWorld.java` | Push/pop `World` around village `generate`; unwrap RTG from wrapped chunk gens |
| `mixin/MixinMapGenVillageInside.java` | Plated land boxes as “inside village” for InControl / `isInsideStructure` |
| `mixin/MixinStructureVillagePieces.java` | House skip/retry inland on water; waystone relocates inland as the same piece; wet paths retry inland then omit |
| `mixin/MixinStructureStartVillagePaste.java` | Populate abort if Charm did not wrap paste and a non-road floor is still liquid |
| `mixin/charm/MixinASMHooksVillagePaste.java` | Same abort on Charm `ASMHooks.addComponentParts` (optional `mixins.aqtweaks.charm.json`) |
| `mixin/reccomplex/MixinGenericVillageCreationHandler.java` | RC building skip/retry on water |
| `rtg/VillagePieceAstralSmallShrine.java` | Village component that pastes Astral `smallShrine`; fluid notify; skip ocean/river floor |
| `rtg/VillageAstralSmallShrineHandler.java` | Forge village handler, weight 5, limit 1. `CommonProxy` registers only if `astralsorcery` is loaded. Piece id `AQTSmallShrine` |
| `rtg/CommandAqVillage.java` | OP `/aqvillage` (level 2): TP onto the plate ~6 off the well; prefers unexplored |
| `mixin/bewitchment/MixinWorldGenCambionHome.java` | Y+1 paste + village skip (no plate). Optional `mixins.aqtweaks.bewitchment.json` |
| `mixin/bewitchment/MixinWorldGenCambionHomeMedium.java` | Same for medium Cambion house |
| `mixin/mysticalworld/MixinStructureGenerator.java` | Skip/settle Mystical World huts (not barrows). Optional `mixins.aqtweaks.mysticalworld.json` |
| `mixin/astral/MixinWorldGenAttributeCommon.java` | Skip Astral surface shrines on village overlap. Optional `mixins.aqtweaks.astral.json` |
| `mixin/astral/MixinWorldGenAttributeStructure.java` | Settle land after `generateAsSubmergedStructure`; small shrine/ruin use walkway Y + swamp fill |
| `ArcanaQuestTweaksConfig.RtgModuleConfig.surface` | `config/arcanaquesttweaks/aqtweaks_rtg.cfg` |
| `mixins.aqtweaks.json` | Required: village spawn/start/world/inside, `MixinStructureVillagePieces`, `MixinStructureStartVillagePaste`, `MixinChunkGeneratorRTGVillage`, `MixinGenericVillageCreationHandler` |
| `mixins.aqtweaks.charm.json` | Optional: Charm ASM village paste skip |

Related but separate: `MixinChunkGeneratorRTG.java` fills Deepslate below Y=0 for Depths. Do not conflate with village flatten. See [depths.md](depths.md).

## Current flatten algorithm

Entry: `MixinChunkGeneratorRTGVillage.aqtweaks$flattenNoise`.

Also runs from `@ModifyArg` on the `float[]` passed to `generateTerrain`, in case the landscape object and the terrain array are not the same instance.

### Footprints

- **Land boxes:** surviving houses, RC (full `.rcst` size), well, and roads that were **not omitted**. Flatten does **not** fall back to the unsnapped start AABB. A road is omitted from layout (and thus from the plate) if **any** column is ocean/river, or if at least half its columns are lake. Flatten still never writes ocean/river **columns**.
- **No village-wide rectangle.** Each column uses Euclidean distance to the **nearest land component AABB** (including paths). Empty corners of the start AABB stay hills.
- **12-block component pad:** `dist ≤ villageComponentPad` (default 12) is **100% plate** at well Y. Overlapping pads fill grass between roads and houses when pieces are ≤24 blocks apart. Same rule for **flooded non-ocean** columns (swamp water, plains lakes).
- **Outer Hermite:** `pad < dist ≤ pad + villageEdgeFalloff`. Smoothstep (`3t²−2t³`) plate → raw RTG. Default falloff **12**. Live cfg may still have **48** until edited — set it to 12 if yards still ramp.
- **Village shrine:** not a land-component pad source. 100% plate inside AABB; extra full-plate radius `smallShrinePad` (3). In town, nearby road/house 12-zones already cover the yard.
- **Building boxes:** land boxes minus roads. Used only for swamp raise ramps **outside** the hard pad. Shrine raise radius 3; others `max(xzPad, waterBank)` (16).

`distanceToBoxXZ` is 0 inside the AABB and Euclidean outside (rounded pad corners).

### Column rules (in order)

1. **Ocean-like or river biome** → never write. Pure beach is **not** ocean. Ocean-like names (`kelp`, `coral`, `reef`, …) win even if the biome is also tagged BEACH.
2. **Flooded swamp-like**, `dist ≤ pad` from a land component (or shrine pad 3) → 100% plate, at least Y 64. This is the in-between grass in swamp villages.
3. **Flooded swamp-like**, in the outer Hermite band → blend plate → original water. Water bank can ease that toward skipped ocean/river.
4. **Flooded swamp-like**, outside that, inside building raise radius → swamp-water approach ramp (not in-village yards).
5. **Flooded non-ocean (plains lake, etc.)**, `dist ≤ pad` → 100% plate (build up from the water). Lake **outside** the pad stays water.
6. **Flooded** otherwise → skip. Includes flooded beach so we do not build sand piers into ocean.
7. **Dry**, `dist ≤ pad` from a land component → 100% plate Y.
8. **Dry**, in the outer Hermite band → blend plate → raw height.
9. **Dry**, in that band, near skipped water → **water bank** over `villageWaterBank` (16).

Dock water (ocean/river roads omitted from land boxes; mostly-lake roads omitted) stays water. Land within pad of a house or mixed road still plates.

After `generateTerrain`, caves and ravines can punch the plate. Before `new Chunk`, pad columns that are **not** ocean/river are refilled solid up to plate Y (stone near bedrock, dirt). Plate top stays what RTG placed (sand, grass, …). Only `biomesoplenty:mud` is replaced with loamy grass (`biomesoplenty:grass` meta 2). Cave holes at plate Y use the biome `topBlock`.

### Plate Y

Sampled from the live `ChunkGeneratorRTG.getLandscape` (the mixin `this`), **not** `World.getChunkProvider()`, which often is not an RTG instance and returned 0/NaN.

- Usable height: not NaN and `> 1`.
- Dry land: `max(sampled well height, villageMinWellHeight)` (default 64). Failed dry samples are still **not** floored to 64 (that made hills into sea-level mesas).
- Never-raise well that walked inland: sample the **dry** well column, not the riverbed.
- Failed sample: try land-box centers that are not never-raise. Still fail: skip flatten for that village this chunk (do not cache 64), except swamp wells fall back to min well height.
- Swamp well below min height: plate at min well height.

Cached per world seed + start AABB in `VillagePlate.HEIGHTS`.

### Recursion guard

`VillageLandHelper.SAMPLING` counts `getLandscape` samples. `getNewerNoise` HEAD must not layout villages while sampling, or flatten ↔ noise recurses.

## Placement rules

### Well veto (`startRejectReason`)

Reject only if the well column (`chunkX * 16 + 2`, `chunkZ * 16 + 2`) is **never-raise** (ocean-like biome, river biome, or RTG `landscape.river` &gt; `STRONG_RIVER`) **and** there is no dry column within `villageWaterRetryDistance`. If a dry slot exists, keep the Start (structureMap key stays the original well chunk) and offset pieces to that XZ (`well-walk` in the log).

A dry land well below `villageMinWellHeight` is **not** rejected; the plate is `max(raw, knob)`.

A dry land/beach well is rejected if **ocean-like** is closer than `villageCoastBuffer` (`coast_ocean`). Nearby river does **not** cancel (`coast_river` is gone). `0` = well column only.

The veto always runs. After layout, `forgetRejectedStarts` removes only true rejects from the map and `VillagePlate`. Walked wells stay. `isRtgLandscapeLake` must not nested-`getLandscape` while already sampling.

If an existing `aqtweaks_rtg.cfg` still has Coast Buffer **32**, Forge keeps that saved value.

### Houses, RC, and village shrine

`isBuildingWet`: ocean/river biome or RTG river always wet (retry inland). Swamp-like and low dry land (noise below min well Y) are not building-wet; that land is raised to the plate. `isFloodedAt` still treats lakes as flooded so mostly-lake paths are omitted.

Retry walks inland (`villageWaterRetryDistance`, default 20): street slots, then toward the well, then a spiral around the well. A path that touches ocean/river or is **at least half** wet retries inland the same way; if every slot still fails, it is omitted (no lake bridge). A forest path with a puddle stays. The Astral small-shrine village piece uses the same wet skip/retry. At populate, Charm `ASMHooks.addComponentParts` (and vanilla `MixinStructureStartVillagePaste` if Charm did not wrap the invoke) skips a non-road building if the surface is still liquid or a never-raise biome (not the well, not swamp). Layout omission is the real drop; paste skip is leftover ocean/river only.

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

`MixinMapGenVillageInside` treats **land component AABBs + xzPad (8)** as village for `isInsideStructure`, only if a plate height was cached this session. Y is **well shaft floor through plate + villageBoxHeight**. Unsnapped well template `64..78` uses `plate - 14` so a hill village is not Village down to Y=64. Template start Y (`minY=64 maxY=151`) and the huge unsnapped start AABB are not used. No plate → miss (vanilla child pieces after snap may still match).

Flatten does **not** use `villageBoxXZPad` as extra 100% plate; flatten uses per-component distance (`villageComponentPad`, default 12). Live `villageEdgeFalloff` may still be **48** (Forge keeps saved cfg); code default is 12. `written=256 pad=0` is falloff-only blend, not a missing component pad.

## `/aqvillage` (OP)

Permission level **2** (same as `/locate`). Player sender only.

- `/aqvillage` — nearest **unexplored** allowed well in 16 village cells; else nearest known allowed Start.
- `/aqvillage unexplored` — unexplored only; errors if none in range.
- `/aqvillage known` — nearest already-generated allowed well.

Teleport is **on the plate** (`round(plate)+1`), about 6 blocks east of the well (other cardinals if that column is unsafe), not the well shaft and not vanilla `/locate` AABB center / Y=100. Search does not layout villages; arriving generates the chunk.

## Config (`aqtweaks_rtg.cfg` → Surface)

| Name | Default | Live? | Meaning |
| --- | --- | --- | --- |
| Enable RTG Village Terrain Smoothing | true | yes | Master flatten + layout-first |
| Village Component Pad | 12 | yes | Full plate around each land component, including roads. Overlap fills yards |
| Village Edge Falloff | 12 | yes | Hermite **beyond** the component pad. Live cfg may still be **48** — set to 12 if yards ramp |
| Village Water Bank | 16 | yes | Outer-rim ease toward skipped ocean/river; 0 = old waterline cliffs |
| Village Plate Slope | 0 | yes | Extra dome from box center; 0 = flat |
| Skip Water Village Pieces | true | yes | House/RC/shrine/waystone/path retry inland; omit leftover ocean/river or mostly-wet paths; paste abort |
| Village Water Retry Distance | 20 | yes | Retry walk |
| Reject Coastal Village Starts | true | yes | Well veto: never-raise with no dry slot; ocean coast buffer. Walk river/ocean wells inland |
| Village Min Well Height | 64 | yes | Dry well / lake plate floor. Live DEVBOX cfg already 64 |
| Village Coast Buffer | 16 | yes | Chebyshev; veto dry well if ocean-like closer than this. Nearby river does not cancel. `0` = well column only |
| Enable Village Bounding Box Detection | true | yes | Plated land boxes as Village |
| Village Box XZ Pad | 8 | yes | Detection pad around **land boxes** + swamp dock-approach radius. Not flatten mesa |
| Village Box Height | 32 | yes | Detection Y above plate. Floor is the well shaft (~11–14 below plate) |
| Village Flatten Debug | true | yes | `logs/villagepatch.log` |
| Skip Structures On Village | true | yes | Cancel AS surface shrines, Cambion houses, MW huts on village AABB |
| Enable Structure Land Settle | true | yes | Fill under those structures and ramp the rim |
| Enable Astral Shrine Settle | true | yes | Village-skip + land settle for surface shrines |
| Enable Cambion House Settle | true | yes | Village-skip for Cambion houses (no land plate; Y+1 only) |
| Enable Astral Small Shrine Village Piece | true | yes | At most one small shrine as a village building |
| Enable Mystical Hut Settle | true | yes | Village-skip + land settle for thatch huts |
| Structure Fill Depth | 16 | yes | Max blocks filled down under a pad |
| Structure Rim Bank | 16 | yes | Slope from large shrine / hut pad to land |
| Small Shrine Pad | 3 | yes | Buffer around small shrine/ruin settle and village shrine AABB |

## Post-terrain structures (not village flatten)

Astral surface shrines, Bewitchment Cambion houses, and Mystical World thatch huts paste **after** RTG terrain. They cannot reuse village noise flatten.

- Overlap a village (real AABB / Y, not chunk origin at Y=0) → **do not place**.
- Ancient / desert shrines and Mystical huts: if placed → fill under the footprint (min foundation Y), biome top/filler, max depth 16, rim slope 16, never fill ocean/river/liquids, do not rewrite structure blocks.
- **Small shrine and small ruin:** plate Y is the generate **center / walkway**, not min foundation Y. Rim is `smallShrinePad` (3), not the 16-block large-shrine bank. In **swamp-like** biomes, water is filled up to that plate. Ocean/river still never filled. Plant-like blocks (BOP / Rustic / Farmer’s Delight, `BlockBush`, `Material.PLANTS`) are overwritten; leftover tops above the plate are cleared. Logs and leaves are not.
- **Cambion houses:** **Y+1** paste only (same as Bewitchment’s unburned wickerman). `canSpawnHere` stays on ground Y or houses never spawn. **No land plate** — settle was a 1-block pit around the house. Village overlap still cancels placement.
- **Village piece:** at most one Astral **small shrine** (not the ruin) via Forge `IVillageCreationHandler` (`AQTSmallShrine`). Flatten plates its AABB with at most 3 blocks of extra full plate; nearby road/house 12-zones still own the yard. Liquid blocks get flag 3 + `neighborChanged` so lantern water flows. If the floor is still ocean/river liquid at paste time, skip that chunk. Wild shrines still spawn; overlap skip prevents a second shrine on the same village.
- Treasure caves, village Hedge Witch/Alchemist pieces, MW barrows, wickerman/menhir/circles are out of this pass.

Mixins: `mixins.aqtweaks.astral.json`, `mixins.aqtweaks.bewitchment.json`, `mixins.aqtweaks.mysticalworld.json` (`required: false`). Village flatten/retry mixins are in required `mixins.aqtweaks.json`.

### Structure land settle (`StructureLandSettle`)

Used after Astral ancient/desert shrines and Mystical huts place (not Cambion). Server-only.

- Under the footprint: biome `fillerBlock` (else dirt) down to `structureFillDepth` (16). Top of the rim (not under-structure) uses `topBlock` (else grass). Stop on non-fillable solid or on liquid unless swamp-fill is on.
- **Never write** `isNeverRaiseBiome` (ocean-like / river). Swamp-like liquid may be replaced when `fillSwampLiquid` is true (small shrine/ruin walkway Y).
- **Fillable:** air, snow layer, tallgrass, flowers, double plant, lily, `Material.PLANTS` / `VINE` / `CACTUS`, `BlockBush` / `BlockReed` / `BlockVine`, `isReplaceable`. **Not fillable:** leaves, wood, rock.
- After fill, plant-like blocks from plate Y through plate+3 are cleared to air. Logs/leaves stop the clear.
- Rim: Euclidean distance to the AABB, smoothstep over `structureRimBank` (16) for huts and large shrines, or `smallShrinePad` (3) for small shrine/ruin. Only raises toward plate (will not dig). `settleTemplate` uses origin Y as floor after rotation AABB.

`StructureVillageOverlap` tests real AABB/Y against remembered village starts (not chunk origin at Y=0). Treasure shrines are excluded from the Astral skip mixin.

## Debug log

`Village Flatten Debug` writes `logs/villagepatch.log` (instance cwd), truncated each launch.

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
- `village piece skip water floor` / `astral shrine skip ocean floor` — paste refused water
- `astral` / `cambion` / `mystical hut skip village overlap` — post-terrain skip
- `astral small shrine village piece at=...` — shrine pasted as a village building

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

## Playtest reference (this line)

- **Wanted:** inland plains village (example `-2897, 97, -2119`) — flat plate, houses on it, blend to hills.
- **Wanted:** sea-level forest (`-524, 64, 5893`) — dirt path, lamps, and houses on the same Y.
- **Wanted:** beach/land well ~16 from water — village starts; buildings retry inland, not on the water.
- **Wanted:** small Astral shrine/ruin — land buffer at most 3 around the marble, not a 16-block mesa.
- **Wanted:** grass between a dirt path and a house at the same Y as the path (swamp/forest yards). L-shaped villages hug pieces; unused AABB corners stay hills.
- **Unwanted (fixed in flatten, verify on new chunks):** beach sand piers into ocean; ocean ledges; swamp/beach vertical plate walls into water; 1-block grass pads under houses with path one lower; village well in coral reef / kelp forest / open ocean; **river well in the water** (walk inland); plains hill villages stepping instead of one pad; dirt cliff at the far end of a tall RC village piece; in-village grass basins between roads and houses; oak plank path sitting in a lake; houses/roads in F3 River; well over a ravine; floating lamps after cave carve; whole pad forced to loamy grass; leftover BOP mud on the pad.
- Swamp villages still keep pieces in swamp water; that water **inside the 12-pad** is filled to plate Y. Open swamp **outside** the pads stays water. Ocean/river columns are never filled.

## Likely next levers

- `Village Water Bank` if ramps are too short/long.
- `Village Edge Falloff`: new default is 12. Existing `aqtweaks_rtg.cfg` with 48 stays 48 until you set 12.
- Houses that sit *on* the waterline still get a flat core (100% plate under the AABB); only the skirt ramps.
- Existing instance cfg may still have Coast Buffer 32 / Water Retry 32 until changed.

## Out of scope unless asked

- Recarving old chunks
- Moving houses already sitting on hills
- Version bump
- Changing vanilla/RC piece sets beyond the small shrine
- Filling ocean/river to make more village land
- Small ruin as a village piece
- Marble fill under Astral temple pads (`astralsorcery:blockmarble`)
- Forcing Waystones `villageChance` so every village has a waystone
