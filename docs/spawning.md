# Spawning module (1.8)

Last updated: 2026-09-08.

Config: `config/arcanaquesttweaks/aqtweaks_spawning.cfg`. Pack lists: `config/arcanaquest/mob_overworldspawntype.json` and `config/arcanaquest/mob_spawnparties.json` (Tweaks does **not** ship or write these files). Always registered. Vanilla spawner mixins in **required** `mixins.aqtweaks.json`. Compile-hard InControl `1.12-3.10.4` (`after:incontrol`).

## Locked intent

InControl `potentialspawn.json` keeps one biome pool for surface and cave hostiles. Vanilla `WorldEntitySpawner` picks **one** `SpawnListEntry` at `WorldServer.getSpawnListEntryForTypeAt` and does **not** reroll when `spawn.json` denies that pick. Tweaks strips **surface-only** ids from cave picks and **underground-only** ids from surface picks at the **exact pick pos**.

1.12 does not loop `groupcountmin`/`groupcountmax`. After the first ticking spawn of a pack, Tweaks rolls **target ∈ [min, max]** and places more of that entry nearby until target (bounded attempts, Y scan). Per-id cfg overrides replace InControl min/max (e.g. feral goblin 3–5). `spawn.json` stays the safety net.

After same-species fill, the first matching **spawn party** in pack JSON may place other species nearby. Cage spawners and TC portal summons never enter this path.

Vanilla hostile cap is `EnumCreatureType.MONSTER` **70**, scaled by loaded chunks (`70 * chunks / 289`). Tweaks uses cfg **Hostile Mob Cap** (default **200**) for that getter inside `findChunksForSpawning` only.

Do **not** put `seesky` / height on `potentialspawn.json`. Do **not** copy layer ids into Tweaks cfg. Do **not** treat ids in **both** JSON arrays as exclusive. Do **not** `@Overwrite` `findChunksForSpawning` or Depths’ `getRandomChunkPosition`. Do **not** hook `performWorldGenSpawning`. Do **not** mixin `World` / `WorldServer` for spawn capture.

**Out of scope:** dual-pass spawn seeds.

## How parents work

**Vanilla / Forge:** `getSpawnListEntryForTypeAt` → `getPossibleCreatures` → `PotentialSpawns` → `WeightedRandom`. Then 3 packs × 4 XZ tries (`ΔY` = 0). After a spawn, `ForgeEventFactory.getMaxSpawnPackSize` (`getMaxSpawnedInChunk()`, often 1) aborts the chunk. Hostile budget: `getMaxNumberOfCreature()` × eligible chunks / 289.

**InControl:** writes `minGroupCount`/`maxGroupCount` on the entry; `spawn.json` deny does not reroll. Mixins player min-distance on the same spawner (`WorldEntitySpawnerMixin`). Tweaks uses `GeneralConfiguration.MIN_PLAYER_*_SPAWN_DISTANCE` for extra members. Per-mob `maxcount` is separate from the global MONSTER cap.

**Pack JSON** `mob_overworldspawntype.json`: `{ "surface": [...], "underground": [...] }`.

| Set | Cave pick | Surface pick |
| --- | --- | --- |
| surface − underground | **remove** | keep |
| underground − surface | keep | **remove** |
| intersection | keep | keep |
| in neither | keep | keep |

**Pack JSON** `mob_spawnparties.json`: `{ "parties": [ { "id", "leaders", "chance", "when", "companions" } ] }`. Missing file → mixed groups off. Omit `when` (or omit a key) → that check is ignored (any dim / biome / time / layer / height). `time`: `night` / `day` / `both` (omit = both). First matching party only. Companions via `EntityList.createEntityByIDFromName`.

## How Tweaks hooks in

**Layer filter:** `SpawnTypeLists.load` in preInit. `SpawnLayerFilter` on `PotentialSpawns` **LOWEST**. Cave pick: `Y < Cave Max Y` **and** sky light ≤ Max Cave Sky Light.

**Pack fill:** `MixinWorldEntitySpawner` only. ThreadLocal entry via Redirect of `getSpawnListEntryForTypeAt` while `findChunksForSpawning` runs. Redirect `spawnEntity`: after success, `SpawnPackFiller` extras then `SpawnParties`. Redirect `getMaxSpawnPackSize` → **1** when fill applies. Recursion guard while filling (`filling` also blocks companion parties from stacking).

**Hostile cap:** Redirect `EnumCreatureType.getMaxNumberOfCreature()` in the same mixin. MONSTER → cfg (default 200). Other types stay vanilla 10 / 15 / 5. Do **not** call `getMaxNumberOfCreature()` from that redirect (recursion). Cage spawners and TC portals unused.

Range: cfg override `modid:path=min-max`, else entry min/max, clamp to Group Size Cap. `1..1` skips filler (still pack-size 1). Mixed groups still run if fill is skipped.

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
| Enable Mixed Groups | true | yes |
| Spawn Parties File | `arcanaquest/mob_spawnparties.json` | reload on Tweaks cfg change |
| Hostile Mob Cap | 200 | yes |

JSON layer / parties files: edit needs **restart** (or a Tweaks cfg save to trigger reload). Override map rebuilds on Tweaks cfg change. Master off → vanilla 70 cap, no filter, no fill, no parties.

## Files

| Piece | Role |
| --- | --- |
| `spawning/SpawnTypeLists.java` | Pack JSON exclusive sets |
| `spawning/SpawnLayerFilter.java` | `PotentialSpawns` LOWEST; cave test + strip for companions |
| `spawning/SpawnGroupSizes.java` | Overrides + roll target + findChunks cap |
| `spawning/SpawnPackContext.java` | ThreadLocal pack entry |
| `spawning/SpawnPackFiller.java` | Extra placements; InControl min-distance |
| `spawning/SpawnParties.java` | Mixed groups from pack JSON |
| `mixin/MixinWorldEntitySpawner.java` | Capture entry, fill pack, pack-size 1, hostile cap. Do **not** mixin `WorldServer`. |
| `ArcanaQuestTweaksConfig.SpawningModuleConfig` | `aqtweaks_spawning.cfg` |

## Do not regress

- Intersection ids on **both** layers. Unknown ids not stripped.
- Forest Y≥60 under leaves stays surface pool.
- Nether/End / non-MONSTER unchanged when those flags are on (layer filter / pack fill). Hostile **cap** still applies in every dim for MONSTER.
- Rare `1..1` stays singles. Feral goblin default override 3–5 until the cfg line is removed.
- Cage spawner / TC lesser portal: no pack fill, no party, unchanged cap formula (they do not use `findChunksForSpawning`).
- Depths `getRandomChunkPosition` mixin untouched. InControl player-distance mixin still applies to vanilla tries.
- Java 21 `--release`. Stamina packets stay 0–2.

## Verify

Boot: `Loaded spawn types from …`; `Loaded N spawn parties from …` when JSON present. No mixin fail on `MixinWorldEntitySpawner` / `findChunksForSpawning`. Closed cave: dwarf/cave_spider/krake yes, Dryad/witch/Wildkin no. Night plains: InControl 2..4 husk/zombie packs of 2–4, not stuck at 1, not 5+. `goblin_feral` 3–5 when the first lands. Fill Pack Size off: old singles. Master off or JSON missing: no layer strip. Missing parties JSON: mixed groups off, no crash. Natural Overworld `thaumcraft:cultistcleric`: 2–3 knights + 2–3 CR archers (CheckSpawn / crimsoncult stage can still deny). Portal / cage cleric: no party. Hostile cap 200 lets natural MONSTER count exceed the old 70-scaled ceiling; cfg 70 restores vanilla. Animals/water/ambient caps unchanged.
