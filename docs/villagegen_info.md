# Village generation in this pack

Last updated: 2026-08-21.

How Minecraft 1.12.2 **marks** a village, how it **pastes** buildings, and what RTG, Geographicraft, Recurrent Complex, Charm, and Tweaks each change. Tweaks locked intent and flatten knobs stay in [rtg.md](rtg.md). This file is the pipeline reference.

Jars checked: `RTG-1.12.2-7.3.3.6`, `Geographicraft-1.12.2-0.9.8.5`, `RecurrentComplexVolts-1.12.2-2.0.0.9`, `Charm-1.12.2-1.4.1`, vanilla `MapGenVillage`.

Worldgen applies to **new chunks only**.

## Two passes (vanilla still owns both)

Layout does **not** place blocks. Populate does. RTG only changes **when** those vanilla methods run relative to terrain.

| Pass | When | Vanilla entry | What it does |
| --- | --- | --- | --- |
| **Mark / layout** | During chunk creation | `MapGenVillage.func_186125_a` → `MapGenBase.func_151539_a` | `canSpawn` + `getStructureStart`. Stores a `Start` in `structureMap`. Pieces are AABBs only. Y is still template Y (logs often `minY=64 maxY=151`). |
| **Paste** | `populate` | `MapGenVillage.func_175794_a` (`generateStructure`) | Each piece `addComponentParts`. `getAverageGroundLevel` averages **world** columns, then the box is offset to that Y. |

```
RTG generateChunk
  getLandscape (noise)
  Tweaks flatten noise          ← plate contract lives here
  generateTerrain
  villageGenerator.generate     ← layout (often already done by Tweaks dummy primer)

RTG populate
  villageGenerator.generateStructure
    Charm ASMHooks.addComponentParts   ← this pack
      piece.addComponentParts
        vanilla / RC snap to world height
```

Tweaks injects flatten **between** `getLandscape` and `generateTerrain`, and calls `generate()` early (`layoutVillageGrid` + dummy primer) so AABBs exist before stone is carved.

## Vanilla: who may start, then the piece graph

### Well chunk (`MapGenVillage.func_75047_a`)

This is the only “this chunk is a village well” test.

1. Divide the world into cells of size `distance` (default 32; RTG can pass a settings map).
2. Seed `World.setRandomSeed(cellX, cellZ, 10387312)`.
3. Pick one well chunk in that cell: `cellOrigin + random(0, distance - minTownSeparation)`.
4. Current chunk must be that well chunk.
5. Biome: `BiomeProvider.areBiomesViable(chunkX*16+8, chunkZ*16+8, 0, VILLAGE_SPAWN_BIOMES)`.

Default `VILLAGE_SPAWN_BIOMES` (`field_75055_e`): plains, desert, savanna, taiga. Radius `0` is **one column**, the chunk **center** (`+8,+8`), not the well column (`+2,+2`).

If that passes, `func_75049_b` builds `MapGenVillage.Start` → `StructureVillagePieces.Start` (well) and grows roads/houses. Forge `VillagerRegistry.IVillageCreationHandler` extra pieces join that weighted list (`getVillagePieceWeight` / `buildComponent`).

Houses: `StructureVillagePieces.func_176066_d`. Roads: `func_176069_e`. Vanilla roads over liquid become oak planks unless Tweaks omits the path at layout.

`Start` is stored in `structureMap` keyed by well chunk (`ChunkPos.asLong`). If a key already exists, later `generate()` **does not** call `canSpawn` again. A Start created while the veto was skipped stays locatable.

### Locate and “inside village”

| Caller | Reads |
| --- | --- |
| `/locate Village` | Nearest Start in `structureMap` (`func_180706_b`). AABB **center**, often Y ~100. Not Tweaks’ detection mixin. |
| `/aqvillage` | OP Tweaks command: plate Y+1, ~6 off the well. Prefers unexplored. See [rtg.md](rtg.md). |
| `isInsideStructure("Village", pos)` / InControl | Vanilla child-piece boxes, **or** Tweaks `MixinMapGenVillageInside` (land boxes + `villageBoxXZPad`, well floor through plate + `villageBoxHeight`). |
| Antique Atlas village marker | Structure presence, same family as locate. |

A Start with no pasted buildings is still a Village for locate. Template Y `64..151` plus teleport Y=100 can count as “inside” even when the plate is at 64.

## RTG 7.3.3.6 — host only

`ChunkGeneratorRTG` constructs **vanilla** `new MapGenVillage(settingsMap)`, then `TerrainGen.getModdedMapGen(..., EventType.VILLAGE)`.

- `useVillages` can disable villages.
- Settings map can change **spacing**.
- Layout: after `generateTerrain` (and after caves/ravines), `villageGenerator.func_186125_a`. Tweaks also layouts **before** terrain so flatten can see AABBs, then reseals the pad after cave carve.
- Paste: in `func_185931_b` (`populate`), `func_175794_a`.
- `/locate Village` and `isInsideStructure("Village", …)` delegate to that same `MapGenVillage`.

RTG does **not** replace `MapGenVillage`, does **not** flatten under villages, does **not** change `VILLAGE_SPAWN_BIOMES`, and does **not** add RC pieces. It only owns Overworld chunk gen (height first) and fires `InitMapGenEvent`.

## Geographicraft 0.9.8.5 — biome list only

Package is still `climateControl`. `AbstractWorldGenerator` builds a `VillageBiomes` list: biome ids `0..255` where `ClimateControlRules.hasVillages(id)` is true. If `controlVillageBiomes` is on, it **overwrites** `MapGenVillage.field_75055_e`.

That only changes **which biomes may host a well** (swamp, extra plains variants, etc.). No layout, no paste, no flatten, no RC. `VillageBiomes.contains` is a normal list contains.

`rtgAwareRiverReduction` adjusts river **gen layers** when the world type name is `"RTG"`. Not villages.

## Recurrent Complex Volts 2.0.0.9 — extra pieces, not extra villages

RC does **not** create village starts. It registers `IVillageCreationHandler`s so vanilla `Start` growth can pick `.rcst` buildings.

**Layout** (`GenericVillageCreationHandler.buildComponent`):

- Active structure + `VanillaGeneration` for `generationID`.
- Weight/count from the structure file and `RCConfig.tweakedSpawnRate`.
- Skip if `VanillaGeneration.generatesIn(start.biome)` is false.
- AABB is **`Structure.size()`** at the street origin (full template, not a 1×1 stub).
- Collision: `canVillageGoDeeper` + `StructureComponent.findIntersecting`.

**Paste** (`GenericVillagePiece.func_74875_a`):

1. If `averageGroundLvl < 0`, vanilla `getAverageGroundLevel`.
2. Offset the box to that Y, then add rotated `VanillaGeneration.spawnShift`.
3. `StructureGenerator` pastes from the box **min corner**.

RC does **not** read Tweaks’ plate. If those columns were never written, snap follows water/noise (logged `y0=60` vs plate `64.1`). If they were plated, snap matches (logged `y0=66` vs plate `66.7`).

Villager NBT `profession` 14/17/18 null is RC/Forge registry noise; RC skips the entity and still places blocks. That is not an empty village.

RC selectors (`Village_farm`, `Rc:Village_farm_vanilla_…`) name a **piece** or structure id. That is not vanilla `/locate Village` (well Start).

## Charm 1.4.1 — biomes + paste wrapper

Not required to understand vanilla, but this pack ships it and it changes Tweaks’ paste hooks.

`MoreVillageBiomes.preInit` also does `MapGenVillage.field_75055_e = allBiomes` (taiga, jungle, extras). **Last writer wins** versus Geographicraft.

Charm ASM rewrites `StructureStart.generateStructure` so paste goes through `ASMHooks.addComponentParts`:

1. Post `StructureEventBase.Pre`. If result is `DENY`, skip the piece.
2. Call the real `addComponentParts`.
3. Post `Post` if paste returned true.

Tweaks’ `@Redirect` on `StructureComponent.func_74875_a` inside `StructureStart.func_75068_a` often **never runs**. Layout omission (houses/paths/RC) is the reliable drop. Tweaks also injects `ASMHooks.addComponentParts` HEAD (`mixins.aqtweaks.charm.json`) and returns false for leftover ocean/river floors — the same skip Charm would do on Pre `DENY`.

`villageDoorsForBiome` / `BiomeEvent.GetVillageBlockID` only theme wood and doors.

## Tweaks overlay (1.6 plate contract)

| Hook | Role |
| --- | --- |
| `MixinMapGenVillageSpawn` | After vanilla `canSpawn`, veto never-raise wells with no dry slot; ocean coast buffer. Always runs. |
| `forgetRejectedStarts` | Drops vetoed Starts from `structureMap` + `VillagePlate` so `/locate` cannot find them. Walked wells stay. |
| `MixinMapGenVillageStart` | Offset walked wells; `VillagePlate.remember` with actual well XZ. |
| `layoutVillageGrid` | Dummy-primer `generate()` after `getNewerNoise` so AABBs exist before flatten. Stash generators for `/aqvillage`. |
| `MixinChunkGeneratorRTGVillage` | Rewrite `landscape.noise` from **land boxes** + pad 12 + Hermite falloff. Never write ocean/river (including RTG river). Raise dry land to min well Y. Reseal pad after caves/ravines. Mud → loamy grass:2 only. |
| `MixinStructureVillagePieces` | House/waystone skip/retry inland on never-raise; wet paths retry inland then omit leftover ocean/river or mostly-wet docks. |
| `MixinGenericVillageCreationHandler` | Same skip/retry for RC AABBs. |
| `MixinASMHooksVillagePaste` | Charm populate abort on ocean/river floor (`mixins.aqtweaks.charm.json`). |
| `MixinStructureStartVillagePaste` | Same abort if Charm did not wrap the invoke. |
| `MixinMapGenVillageInside` | Detection = land boxes + `villageBoxXZPad`, well floor through plate + `villageBoxHeight`. |

`isLandscapeLake`: a **null** sample (or nested sampling) is **not** wet. Load-time forget must not treat missing landscape as a flooded plains well. Layout must not treat missing landscape as a lake (that omitted every road).

### Config knobs (do not confuse)

| Knob | Actual job |
| --- | --- |
| `villageComponentPad` (12) | Hard plate around **land component** AABBs. Overlap = village footprint. |
| `villageEdgeFalloff` (code 12; live cfg often **48**) | Blend **outside** that pad. `written=256 pad=0` is falloff-only, not a cliff. |
| `villageBoxXZPad` (8) | Detection around land boxes / swamp approach. **Not flatten.** |
| `villageMinWellHeight` (64) | Dry well / lake plate floor. Wired. |
| `villageCoastBuffer` (code 16; live may be 32) | Dry well veto vs nearby **ocean**. Nearby river does not cancel. |

Forge keeps saved cfg values when Java defaults change.

## What each mod does not do

| Mod | Does not |
| --- | --- |
| RTG | Flatten, change spawn biomes, add buildings, snap RC to a plate. |
| Geographicraft | Layout, paste, terrain, piece graph. |
| Recurrent Complex | Choose well chunks, flatten, read Tweaks plate Y. |
| Charm | Flatten, create extra Starts (only extra biomes + paste events). |
| Tweaks | Pave ocean/river; fill lakes around a kept hall; recarve old chunks. |

## Implications for village smoothing

Paste (vanilla house, shrine, RC) always follows **world height**. The plate is a contract:

- **Keep the piece** → its columns must already be dry land at plate Y (ocean/river still never written; then the piece must not exist).
- **Cannot write those columns** → omit the piece at **layout** (retry inland, else delete). Charm paste will still place whatever remains.

Smoothing does not use the detection pad, the unsnapped start AABB, or a paste-time `StructureStart` redirect as the primary omit. Flooded wells must not remain in `structureMap`.
