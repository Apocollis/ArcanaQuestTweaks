# RTG module (1.6)

Last updated: 2026-08-20, 12-block component plate (yards between roads/houses).

This is the RTG module: village flatten/placement, then post-terrain structure skip/settle. Locked intent, current pipeline, and why earlier approaches were dropped. Read this before changing village flatten, spawn veto, piece retry, or shrine/house/hut land settle.

## How the parent mods work

### Realistic Terrain Generation (RTG 7.3.3.6)

RTG replaces the Overworld chunk generator. `ChunkGeneratorRTG.func_185932_a` roughly:

1. `getLandscape` — per-column height `noise[]`, river strength, biomes
2. `generateTerrain(primer, noise)` — stone/dirt/water from that height field
3. `villageGenerator.generate()` — vanilla village map gen **after** land exists

RTG does not flatten under villages. Tweaks rewrites `landscape.noise` **between** (1) and (2) so pieces still place in populate, but the ground they sit on is already plated.

### Vanilla `MapGenVillage`

- `func_75047_a` — can this chunk be a village start (well)?
- `func_75049_b` — create `StructureVillagePieces.Start` (well + piece list)
- `func_151539_a` (`MapGenBase`) — generate into a primer/world
- Houses: `StructureVillagePieces.func_176066_d`
- `MapGenStructure.func_175797_c` — `isInsideStructure` / InControl “Village”

Pieces use AABBs. Roads that cross water become wooden docks. Tweaks does not replace piece types; it vetoes starts, retries house/RC origins, and shapes RTG noise.

### Recurrent Complex

`GenericVillageCreationHandler.buildComponent` adds RC buildings as village pieces, same start/facing as vanilla houses. Tweaks retries wet AABBs like vanilla houses. RC worldgen rays for **non-village** structures are the Depths module (`RayMatcher`), not this file.

## Locked intent

Keep **what** villages create (vanilla pieces + Recurrent Complex, plus at most one Astral small shrine). Change **where** they may start and **how RTG land under them is shaped**.

- Do **not** pave oceans or rivers. Vanilla wooden docks on water are OK.
- Houses and RC buildings must not sit in ocean-like or river water. Skip and retry onto land. Do not cancel the whole village because a building would have been wet.
- **Ocean-like** (never raise, never well start): `Type.OCEAN` or `Type.WATER` (not swamp), plus names `ocean`, `kelp` (BOP Kelp Forest `kelp_forest`), `coral`, `reef`, `atoll`, `lagoon`. Pure **BEACH** is not ocean.
- Wells may sit **16 blocks from the coast** (Chebyshev). Do not veto a land/beach well just because ocean is nearby beyond that. Veto if ocean-like or river is **closer than** `villageCoastBuffer` (default 16). `0` = well column only.
- In **swamp-like** biomes, pieces may stay. Plate buildings, mixed roads, **and the yards between them** (12-block component pad). Roads on open water stay docks.
- **Never raise or plate ocean-like or river columns**, even inside a house or road AABB.
- Keep a **flat plate under dry-land roads**, including stretches with no buildings. Omit a road from the plate hull only if **every** sampled column is flooded (true dock). A puddle on a forest path must not drop that path off the plate.
- The plate is the **walkable village footprint**: pieces **and** the land between them, plus a 12-block full pad around each land component (including roads). It is **not** a village-wide rectangle. Empty AABB corners with no nearby piece stay hills.
- Inland plains/forest (playtest “village 2”) is the target look for non-water biomes.
- Structure detection reaching a bit past the village is acceptable; do not chase that unless asked.

## Hard constraints

- Stay version **1.6**.
- **New chunks only.** Flattening writes RTG `landscape.noise` before `generateTerrain`. Existing chunks are not recarved.
- RTG jar in this pack: `RTG-1.12.2-7.3.3.6.jar`. RTG has **no** village terrain queue of its own.
- Production RTG chunk method is `func_185932_a` (`remap = false`). Injects that replace the return need `CallbackInfoReturnable<Chunk>`.
- Mixin targets use SRG names. Do not use DeferredRegister / 1.16-style registries.
- Village mixins live in **required** `mixins.aqtweaks.json` (Forge, RTG, Recurrent Complex assumed present). Astral / Cambion / Mystical hut mixins are **optional** json (`required: false`).
- Two mixins target `ChunkGeneratorRTG`: village flatten **before** `generateTerrain`; Depths Deepslate fill at **TAIL** of `generateTerrain` (Y min..-1, not Y=0). See [depths.md](depths.md). Do not merge them.
- Player/world/block/primer access goes through `util/Reflect.java`. Landscape samples and structure boxes are not raw `World.getChunkProvider()`.

## Design plan (placement vs flatten)

Keep **what** villages create. Change **where** they start and **how RTG land under them is shaped**.

1. Layout village XZ **before** RTG `generateTerrain` (`layoutVillageGrid`).
2. Sample plate Y from the well via live `ChunkGeneratorRTG.getLandscape`.
3. Flatten `landscape.noise` under land boxes (houses, well, mixed/dry roads). Skip ocean-like, river, and flooded columns. Swamp-like buildings get a rounded raise.
4. RTG carves from that noise. Production method `func_185932_a` (`remap = false`).
5. Populate places the same pieces. Wet houses/RC/shrine/waystone retry inland; docks stay wood.

Do not recarve old chunks. Do not veto a whole village because one building was wet. Do not treat a puddle on a path as a dock.

## Why we flatten noise, not blocks

Vanilla villages layout **after** terrain exists. RTG order is:

1. `getLandscape` (height noise, rivers)
2. `generateTerrain(primer, noise)`
3. `villageGenerator.generate()` (pieces)

If we wait until populate, the land is already carved. So we:

1. Layout village pieces (XZ) **before** RTG carves the chunk.
2. Sample plate Y from the **well column** in RTG `landscape.noise`.
3. Rewrite `landscape.noise` under the land footprint.
4. Let RTG `generateTerrain` build blocks from that noise.
5. Populate still places the same pieces.

Layout must be cheap. `layoutVillageGrid` generates the **current chunk** plus the **vanilla well chunk** of each nearby village cell (spacing from the map gen, UT default 25, radius 8 chunks). The well is `cellOrigin + random(0, spacing - minTown)` with seed `setRandomSeed(cellX, cellZ, 10387312)`, not the cell origin. Generating only origins almost never created the `Start`, so hill-side chunks flattened as raw RTG and buildings stepped. It does **not** call `generate()` on all 289 neighbors.

## File map

| File | Role |
| --- | --- |
| `rtg/VillageLandHelper.java` | Wet tests, swamp/ocean-like/river/beach, well veto, coast buffer, fully flooded roads, grid layout, live RTG noise sample, waystone inland slots |
| `rtg/VillagePlate.java` | In-memory starts, land vs building boxes, plate height cache |
| `rtg/VillageDebug.java` | `logs/villagepatch.log` in the instance folder (not `latest.log`) |
| `rtg/StructureVillageOverlap.java` | Village AABB/Y test for post-terrain schematics (not Y=0) |
| `rtg/StructureLandSettle.java` | Fill under a placed schematic + rim slope; swamp-liquid fill; overwrite plant-like blocks |
| `mixin/bettercaves/MixinChunkGeneratorRTGVillage.java` | Layout-first + flatten noise |
| `mixin/MixinMapGenVillageSpawn.java` | Well veto (`func_75047_a`) |
| `mixin/MixinMapGenVillageStart.java` | Remember start after create (`func_75049_b`) |
| `mixin/MixinMapGenVillageWorld.java` | Push/pop `World` around village `generate` |
| `mixin/MixinMapGenVillageInside.java` | Padded start AABB as “inside village” for InControl / `isInsideStructure` |
| `mixin/MixinStructureVillagePieces.java` | House skip/retry on water; waystone relocates inland as the same piece |
| `mixin/reccomplex/MixinGenericVillageCreationHandler.java` | RC building skip/retry on water |
| `rtg/VillagePieceAstralSmallShrine.java` | Village component that pastes Astral `smallShrine`; fluid notify; skip ocean/river floor |
| `rtg/VillageAstralSmallShrineHandler.java` | Forge village handler, weight 5, limit 1. `CommonProxy` registers only if `astralsorcery` is loaded. Piece id `AQTSmallShrine` |
| `mixin/bewitchment/MixinWorldGenCambionHome.java` | Y+1 paste + village skip (no plate). Optional `mixins.aqtweaks.bewitchment.json` |
| `mixin/bewitchment/MixinWorldGenCambionHomeMedium.java` | Same for medium Cambion house |
| `mixin/mysticalworld/MixinStructureGenerator.java` | Skip/settle Mystical World huts (not barrows). Optional `mixins.aqtweaks.mysticalworld.json` |
| `mixin/astral/MixinWorldGenAttributeCommon.java` | Skip Astral surface shrines on village overlap. Optional `mixins.aqtweaks.astral.json` |
| `mixin/astral/MixinWorldGenAttributeStructure.java` | Settle land after `generateAsSubmergedStructure`; small shrine/ruin use walkway Y + swamp fill |
| `ArcanaQuestTweaksConfig.RtgModuleConfig.surface` | `config/arcanaquesttweaks/aqtweaks_rtg.cfg` |
| `mixins.aqtweaks.json` | Required: village spawn/start/world/inside, `MixinStructureVillagePieces`, `MixinChunkGeneratorRTGVillage`, `MixinGenericVillageCreationHandler` |

Related but separate: `MixinChunkGeneratorRTG.java` fills Deepslate below Y=0 for Depths. Do not conflate with village flatten. See [depths.md](depths.md).

## Current flatten algorithm

Entry: `MixinChunkGeneratorRTGVillage.aqtweaks$flattenNoise`.

Also runs from `@ModifyArg` on the `float[]` passed to `generateTerrain`, in case the landscape object and the terrain array are not the same instance.

### Footprints

- **Land boxes:** houses, RC, well, and roads that are **not fully flooded**. A road is omitted only if **every** sampled column is flooded (`isAabbFullyFlooded`). Fully flooded roads (docks) are not pad sources. Flatten still never writes ocean/river **columns**.
- **No village-wide rectangle.** Each column uses Euclidean distance to the **nearest land component AABB** (including paths). Empty corners of the start AABB stay hills.
- **12-block component pad:** `dist ≤ villageComponentPad` (12) is **100% plate** at well Y. Overlapping pads fill grass between roads and houses when pieces are ≤24 blocks apart (the swamp-yard basin). Same rule for **swamp-like flooded** columns.
- **Outer Hermite:** `12 < dist ≤ 12 + villageEdgeFalloff`. Smoothstep (`3t²−2t³`) plate → raw RTG. Default falloff **12**. Live cfg may still have **48** until edited.
- **Village shrine:** not a land-component pad source. 100% plate inside AABB; extra full-plate radius `smallShrinePad` (3). In town, nearby road/house 12-zones already cover the yard.
- **Building boxes:** land boxes minus roads. Used only for swamp **dock approach** ramps **outside** the 12-zone. Shrine raise radius 3; others `max(xzPad, waterBank)` (16).

`distanceToBoxXZ` is 0 inside the AABB and Euclidean outside (rounded pad corners).

### Column rules (in order)

1. **Ocean-like or river biome** → never write. Pure beach is **not** ocean. Ocean-like names (`kelp`, `coral`, `reef`, …) win even if the biome is also tagged BEACH.
2. **Flooded swamp-like**, `dist ≤ 12` from a land component (or shrine pad 3) → 100% plate, at least Y 64. This is the in-between grass in swamp villages.
3. **Flooded swamp-like**, in the outer Hermite band → blend plate → original water. Water bank can ease that toward skipped ocean/river.
4. **Flooded swamp-like**, outside that, inside building raise radius → dock-approach ramp (not in-village yards).
5. **Flooded** otherwise → skip. Includes flooded beach/plains so we do not build sand piers.
6. **Dry**, `dist ≤ 12` from a land component → 100% plate Y.
7. **Dry**, in the outer Hermite band → blend plate → raw height.
8. **Dry**, in that band, near skipped water → **water bank** over `villageWaterBank` (16).

Dock water (fully flooded roads omitted from land boxes) stays water. Land within 12 of a house or mixed road still plates.

### Plate Y

Sampled from the live `ChunkGeneratorRTG.getLandscape` (the mixin `this`), **not** `World.getChunkProvider()`, which often is not an RTG instance and returned 0/NaN.

- Usable height: not NaN and `> 1`.
- Dry hill: use the sampled well height. **Do not** `max(height, 64)` — that flattened hills to sea level when sampling failed or was low.
- Failed sample: try land-box centers that are not ocean/river and are `>= 64`. Still fail: skip flatten for that village this chunk (do not cache 64), except swamp wells fall back to 64.
- Swamp well below 64: plate at 64.

Cached per world seed + start AABB in `VillagePlate.HEIGHTS`.

### Recursion guard

`VillageLandHelper.SAMPLING` counts `getLandscape` samples. `getNewerNoise` HEAD must not layout villages while sampling, or flatten ↔ noise recurses.

## Placement rules

### Well veto (`startRejectReason`)

Reject if the well column (`chunkX * 16 + 2`, `chunkZ * 16 + 2`) is:

- ocean-like biome (see below)
- river biome (pure beach excluded)
- ocean-like or river **closer than** `villageCoastBuffer` (Chebyshev `max(|dx|,|dz|) < buffer`, sampled every 2 blocks). Distance **at or beyond** the buffer is allowed. Default **16**. `0` = well column only.
- flooded RTG water **and not** swamp-like

Beach wells are allowed if they pass the coast buffer. A beach village can still reach ocean; flatten must skip ocean columns rather than paving them. Buildings that would sit on water retry inland; the village is not cancelled.

If an existing `aqtweaks_rtg.cfg` still has Coast Buffer **32**, Forge keeps that saved value. Set it to **16** (or delete the key) to match the intended default.

### Houses, RC, and village shrine

`isBuildingWet`: ocean-like/river always wet. Swamp-like never wet (stay and get a pad). Else flooded.

Retry walks back/sideways along the street (`villageWaterRetryDistance`, default 20). Roads are not skipped. The Astral small-shrine village piece uses the same wet skip/retry.

**Waystones:** `ComponentVillageWaystone` is not retried as a random house. A wet waystone is rebuilt inland (street, then toward the well, then a spiral around the well, all four facings). Ocean/river still never get a plate. Waystones’ own `villageChance` can still skip a village; this only keeps a rolled waystone from being deleted.

Flooded **beach** is not an ocean biome. Wet tests must sample live RTG `landscape.noise` (`VillageLandHelper.pushGenerator` during layout). `World.getChunkProvider()` is often not `ChunkGeneratorRTG`; a null sample used to count as dry and left shrine/waystone AABBs hanging over coastal water.

### Swamp-like vs ocean-like

`isOceanBiome` order (avoid treating reef as beach, or swamp as WATER-ocean):

1. Name contains `ocean`, `kelp`, `coral`, `reef`, `atoll`, or `lagoon` → ocean-like. Covers BOP **Kelp Forest** (`biomesoplenty:kelp_forest`) even when BOP itself has `canGenerateVillages=false`.
2. Pure **BEACH** (type or name `beach`) → not ocean.
3. `Type.OCEAN` → ocean-like.
4. Swamp-tagged (type or swamp names) → not ocean.
5. `Type.WATER` → ocean-like.

`isSwampLikeForRaise` is swamp names/types **and not** ocean-like/river. Names include swamp, marsh, bog, wetland, bayou, mangrove, fen, moor, peat, muskeg, plus `BiomeDictionary.Type.SWAMP`.

If a pack tags mangrove as OCEAN, it is treated as ocean (never raise). That matches “never pave ocean.”

Flooded = ocean-like/river biome **or** RTG landscape wet. `FLOOD_LEVEL` is hardcoded **64**. Config `villageMinWellHeight` (65) is **not** wired into these checks.

## Structure detection

`MixinMapGenVillageInside` treats the **start AABB + xzPad (8)** as village for `isInsideStructure`. Height uses start min/max Y plus plate and `villageBoxHeight` (32).

This is why detection extends past the built village. User accepted that. Flatten does **not** use that pad as extra 100% plate; flatten uses per-component distance.

## Config (`aqtweaks_rtg.cfg` → Surface)

| Name | Default | Live? | Meaning |
| --- | --- | --- | --- |
| Enable RTG Village Terrain Smoothing | true | yes | Master flatten + layout-first |
| Village Component Pad | 12 | yes | Full plate around each land component, including roads. Overlap fills yards |
| Village Edge Falloff | 12 | yes | Hermite **beyond** the component pad. Live cfg may still be **48** |
| Village Water Bank | 16 | yes | Outer-rim ease toward skipped ocean/river; 0 = old waterline cliffs |
| Village Plate Slope | 0 | yes | Extra dome from box center; 0 = flat |
| Skip Water Village Pieces | true | yes | House/RC/village shrine retry |
| Village Water Retry Distance | 20 | yes | Retry walk |
| Reject Coastal Village Starts | true | yes | Well veto: ocean-like, river, coast buffer, flooded non-swamp |
| Village Min Well Height | 65 | **no** | Not referenced; flood tests use 64 |
| Village Coast Buffer | 16 | yes | Chebyshev; veto if ocean-like/river closer than this. `0` = well column only |
| Enable Village Bounding Box Detection | true | yes | Padded start as Village |
| Village Box XZ Pad | 8 | yes | Detection pad + swamp dock-approach radius. Not flatten mesa |
| Village Box Height | 32 | yes | Detection Y above plate |
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
- `veto chunk=... ocean_well|river_well|coast_ocean|coast_river|flooded_well ...` — well rejected
- `plate Y=... landBoxes=... componentPad=... falloff=...` — per-village flatten shape
- `flatten chunk=... boxes=... dry=... wet=... written=... pad=... raised=...` — writes this chunk
- `house` / `rc` / `astral shrine` retry hit/miss
- `waystone aabb wet` / `waystone relocate hit|miss` — same gazebo moved inland
- `astral shrine skip ocean floor` — village shrine refused water at paste time
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

**Fix:** 100% plate for `dist ≤ 12` from the nearest land component **including roads**, dry and swamp-like. Hermite only beyond that pad. No village-wide rectangle. Docks omitted from land boxes stay water. Shrine extra full plate is 3.

## Playtest reference (this line)

- **Wanted:** inland plains village (example `-2897, 97, -2119`) — flat plate, houses on it, blend to hills.
- **Wanted:** sea-level forest (`-524, 64, 5893`) — dirt path, lamps, and houses on the same Y.
- **Wanted:** beach/land well ~16 from water — village starts; buildings retry inland, not on the water.
- **Wanted:** small Astral shrine/ruin — land buffer at most 3 around the marble, not a 16-block mesa.
- **Wanted:** grass between a dirt path and a house at the same Y as the path (swamp/forest yards). L-shaped villages hug pieces; unused AABB corners stay hills.
- **Unwanted (fixed in flatten, verify on new chunks):** beach sand piers into ocean; ocean ledges; swamp/beach vertical plate walls into water; 1-block grass pads under houses with path one lower; village well in coral reef / kelp forest / open ocean / river; plains hill villages stepping instead of one pad; dirt cliff at the far end of a tall RC village piece; in-village grass basins between roads and houses.
- Docks against swamp water are OK; the land behind them should ramp, not a 90° dirt wall.

## Likely next levers

- `Village Water Bank` if ramps are too short/long.
- `Village Edge Falloff`: new default is 12. Existing `aqtweaks_rtg.cfg` with 48 stays 48 until you set 12.
- Wire `villageMinWellHeight` or drop it, so config matches `FLOOD_LEVEL`.
- Detection still uses start AABB + pad 8; user said that is fine.
- Houses that sit *on* the waterline still get a flat core (100% plate under the AABB); only the skirt ramps.
- Existing instance cfg may still have Coast Buffer 32 until changed.

## Out of scope unless asked

- Recarving old chunks
- Moving houses already sitting on hills
- Version bump
- Changing vanilla/RC piece sets beyond the small shrine
- Filling ocean/river to make more village land
- Small ruin as a village piece
- Marble fill under Astral temple pads (`astralsorcery:blockmarble`)
- Forcing Waystones `villageChance` so every village has a waystone
