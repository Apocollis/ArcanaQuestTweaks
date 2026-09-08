# Spawning module (1.8)

Last updated: 2026-09-07.

Config: `config/arcanaquesttweaks/aqtweaks_spawning.cfg`. Pack lists: `config/arcanaquest/mob_overworldspawntype.json` (Tweaks does **not** ship or write this file). Always registered. Vanilla spawner mixins in **required** `mixins.aqtweaks.json`. Compile-hard InControl `1.12-3.10.4` (`after:incontrol`).

## Locked intent

InControl `potentialspawn.json` keeps one biome pool for surface and cave hostiles. Vanilla `WorldEntitySpawner` picks **one** `SpawnListEntry` at `WorldServer.getSpawnListEntryForTypeAt` and does **not** reroll when `spawn.json` denies that pick. Tweaks strips **surface-only** ids from cave picks and **underground-only** ids from surface picks at the **exact pick pos**.

1.12 does not loop `groupcountmin`/`groupcountmax`. After the first ticking spawn of a pack, Tweaks rolls **target ∈ [min, max]** and places more of that entry nearby until target (bounded attempts, Y scan). Per-id cfg overrides replace InControl min/max (e.g. feral goblin 3–5). `spawn.json` stays the safety net.

Do **not** put `seesky` / height on `potentialspawn.json`. Do **not** copy layer ids into Tweaks cfg. Do **not** treat ids in **both** JSON arrays as exclusive. Do **not** `@Overwrite` `findChunksForSpawning` or Depths’ `getRandomChunkPosition`. Do **not** hook `performWorldGenSpawning`.

**Out of scope:** dual-pass spawn seeds.

## How parents work

**Vanilla / Forge:** `getSpawnListEntryForTypeAt` → `getPossibleCreatures` → `PotentialSpawns` → `WeightedRandom`. Then 3 packs × 4 XZ tries (`ΔY` = 0). After a spawn, `ForgeEventFactory.getMaxSpawnPackSize` (`getMaxSpawnedInChunk()`, often 1) aborts the chunk.

**InControl:** writes `minGroupCount`/`maxGroupCount` on the entry; `spawn.json` deny does not reroll. Mixins player min-distance on the same spawner (`WorldEntitySpawnerMixin`). Tweaks uses `GeneralConfiguration.MIN_PLAYER_*_SPAWN_DISTANCE` for extra members.

**Pack JSON** `mob_overworldspawntype.json`: `{ "surface": [...], "underground": [...] }`.

| Set | Cave pick | Surface pick |
| --- | --- | --- |
| surface − underground | **remove** | keep |
| underground − surface | keep | **remove** |
| intersection | keep | keep |
| in neither | keep | keep |

## How Tweaks hooks in

**Layer filter:** `SpawnTypeLists.load` in preInit. `SpawnLayerFilter` on `PotentialSpawns` **LOWEST**. Cave pick: `Y < Cave Max Y` **and** sky light ≤ Max Cave Sky Light.

**Pack fill:** `MixinWorldEntitySpawner` only. ThreadLocal entry via Redirect of `getSpawnListEntryForTypeAt` while `findChunksForSpawning` runs. Redirect `spawnEntity`: after success, roll target and `SpawnPackFiller` extras. Redirect `getMaxSpawnPackSize` → **1** when fill applies. Recursion guard while filling.

Range: cfg override `modid:path=min-max`, else entry min/max, clamp to Group Size Cap. `1..1` skips filler (still pack-size 1).

## Live config (`aqtweaks_spawning.cfg`)

| Knob | Default | Live? |
| --- | --- | --- |
| Enable Spawning Module | true | yes |
| Filter Potential Spawns | true | yes |
| Spawn Type File | `arcanaquest/mob_overworldspawntype.json` | reload on Tweaks cfg change |
| Cave Max Y | 60 | yes |
| Max Cave Sky Light | 0 | yes |
| Overworld Only | true | yes |
| Monster Only | true | yes |
| Fill Pack Size | true | yes |
| Max Extra Attempts | 24 | yes |
| Pack Radius | 8 | yes |
| Y Range | 8 | yes |
| Group Size Cap | 8 | yes |
| Group Size Overrides | `grimoireofgaia:goblin_feral=3-5` | yes |

JSON layer file: edit needs **restart**. Override map rebuilds on Tweaks cfg change.

## Files

| Piece | Role |
| --- | --- |
| `spawning/SpawnTypeLists.java` | Pack JSON exclusive sets |
| `spawning/SpawnLayerFilter.java` | `PotentialSpawns` LOWEST |
| `spawning/SpawnGroupSizes.java` | Overrides + roll target |
| `spawning/SpawnPackContext.java` | ThreadLocal pack entry |
| `spawning/SpawnPackFiller.java` | Extra placements; InControl min-distance |
| `mixin/MixinWorldEntitySpawner.java` | Capture entry, fill pack, pack-size 1. Do **not** mixin `WorldServer` (late prepare: loaded too early, same class of bug as `World.getRawLight`). |
| `ArcanaQuestTweaksConfig.SpawningModuleConfig` | `aqtweaks_spawning.cfg` |

## Do not regress

- Intersection ids on **both** layers. Unknown ids not stripped.
- Forest Y≥60 under leaves stays surface pool.
- Nether/End / non-MONSTER unchanged when those flags are on.
- Rare `1..1` stays singles. Feral goblin default override 3–5 until the cfg line is removed.
- Depths `getRandomChunkPosition` mixin untouched. InControl player-distance mixin still applies to vanilla tries.
- Java 21 `--release`. Stamina packets stay 0–2.

## Verify

Boot: `Loaded spawn types from …`; no mixin fail on `MixinWorldEntitySpawner` / `findChunksForSpawning`. Closed cave: dwarf/cave_spider/krake yes, Dryad/witch/Wildkin no. Night plains: InControl 2..4 husk/zombie packs of 2–4, not stuck at 1, not 5+. `goblin_feral` 3–5 when the first lands. Fill Pack Size off: old singles. Master off or JSON missing: no layer strip.
