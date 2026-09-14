# Spawning module (1.8)

Last updated: 2026-09-14. Cage `MixinMobSpawnerBaseLogic` is in `mixins.aqtweaks.early.json` (late prepare loads `MobSpawnerBaseLogic` too early).

Config: `config/arcanaquesttweaks/aqtweaks_spawning.cfg`. Pack lists: `config/arcanaquest/mob_overworldspawntype.json`, `mob_structurespawns.json`, `mob_spawnparties.json`, `mob_tier.json`, and `mob_spawnrules.cfg` (Tweaks does **not** ship or write these files). Always registered. Vanilla spawner mixins in **required** `mixins.aqtweaks.json`. Compile-hard InControl `1.12-3.10.4` (`after:incontrol`) plus nested McJtyTools `StructureCache`.

## Locked intent

InControl `potentialspawn.json` keeps one biome pool for surface and cave hostiles. Vanilla `WorldEntitySpawner` picks **one** `SpawnListEntry` at `WorldServer.getSpawnListEntryForTypeAt` and does **not** reroll when `spawn.json` denies that pick. Tweaks strips **surface-only** ids from cave picks and **underground-only** ids from surface picks at the **exact pick pos**.

1.12 does not loop `groupcountmin`/`groupcountmax`. After the first ticking spawn of a pack, Tweaks rolls **target ∈ [min, max]** from pack **tier** (`mob_tier.json` + `mob_spawnrules.cfg` `*_group_min`/`*_group_max`), not the vanilla 4–4 row. Per-id cfg overrides still win (e.g. feral goblin 3–5). `spawn.json` stays the safety net. Do **not** scan `potentialspawn.json` for group sizes.

After same-species fill, the first matching **spawn party** in pack JSON may place other species nearby. Cage spawners and TC portal summons never enter this path.

Vanilla hostile cap is `EnumCreatureType.MONSTER` **70**, scaled by loaded chunks (`70 * chunks / 289`). Tweaks uses cfg **Hostile Mob Cap** (default **200**) for that getter inside `findChunksForSpawning` only.

Do **not** put `seesky` / height on `potentialspawn.json`. Do **not** copy layer ids into Tweaks cfg. Do **not** treat ids in **both** JSON arrays as exclusive. Do **not** `@Overwrite` `findChunksForSpawning` or Depths’ `getRandomChunkPosition`. Do **not** hook `performWorldGenSpawning`. Do **not** mixin `World` / `WorldServer` for spawn capture. Do **not** `@Overwrite` `StructureCache.parseStructureData`.

**Out of scope:** dual-pass spawn seeds.

## How parents work

**Vanilla / Forge:** `getSpawnListEntryForTypeAt` → `getPossibleCreatures` → `PotentialSpawns` → `WeightedRandom`. Then 3 packs × 4 XZ tries (`ΔY` = 0). After a spawn, `ForgeEventFactory.getMaxSpawnPackSize` (`getMaxSpawnedInChunk()`, often 1) aborts the chunk. Hostile budget: `getMaxNumberOfCreature()` × eligible chunks / 289.

**Cage spawners:** `MobSpawnerBaseLogic.updateSpawner` decrements delay; at 0 it tries to spawn. `resetTimer()` (200–800) runs only on success or max nearby. A failed `canEntitySpawnSpawner` leaves delay at 0, so the cage retries every tick. Tweaks sets a short fail-recheck delay on that path only (HEAD started-at-zero + RETURN still zero). Decrement 1→0 returns without spawning and must not reset.

**InControl:** **adds** `potentialspawn.json` rows onto the vanilla biome list (creeper 1–2, etc.) and does **not** delete vanilla 4–4. Tweaks then keeps the **last** row per entity class so fill and WeightedRandom use InControl group counts and weights. `spawn.json` deny does not reroll. Mixins player min-distance on the same spawner (`WorldEntitySpawnerMixin`). Tweaks uses `GeneralConfiguration.MIN_PLAYER_*_SPAWN_DISTANCE` for extra members. Per-mob `maxcount` is separate from the global MONSTER cap.

`StructureCache.parseStructureData` reads `MapGenStructureData.getTagCompound()` (already the Features map) and, stock, only stores each start’s origin `ChunkX`/`ChunkZ`. Tweaks RETURN-injects chunk longs for every feature and child `BB` (`[minX,minY,minZ,maxX,maxY,maxZ]`). `isInStructure` is then true across Better Mineshafts (and other multi-chunk structures), so InControl `structure: Mineshaft` rules match the footprint.

**Pack JSON** `mob_overworldspawntype.json`: `{ "surface": [...], "underground": [...] }`.

| Set | Cave pick | Surface pick |
| --- | --- | --- |
| surface − underground | **remove** | keep |
| underground − surface | keep | **remove** |
| intersection | keep | keep |
| in neither | keep | keep |

**Pack JSON** `mob_structurespawns.json`: `{ "Mineshaft": [ "minecraft:witch", … ] }` (structure **name** → entity ids). Missing file → cave exemption off. Names must match InControl / `MapGenStructureData` (vanilla `Mineshaft`, not a YUNG registry id). Cave picks still strip other surface-only ids.

**Pack JSON** `mob_spawnparties.json`: `{ "parties": [ { "id", "leaders", "chance", "when", "companions" } ] }`. Missing file → mixed groups off. Omit `when` (or omit a key) → that check is ignored (any dim / biome / time / layer / height). `time`: `night` / `day` / `both` (omit = both). First matching party only. Companions via `EntityList.createEntityByIDFromName`.

Keys are the `@SerializedName` values on `SpawnParties`:

| Key | Type | Default | Meaning |
| --- | --- | --- | --- |
| `parties` | list | — | Absent array → mixed groups off |
| `parties[].id` | string | `unnamed` | Log label only |
| `parties[].leaders` | list of entity ids | — | Ids whose first ticking spawn triggers the party. Empty → party dropped at load |
| `parties[].chance` | double 0.0–1.0 | 1.0 | Rolled after `when` matches. A failed roll stops the search — no later party runs. Outside 0..1 → party dropped |
| `parties[].when` | object | omitted = always | Rows below |
| `parties[].companions` | list | — | Empty (or all entries invalid) → party dropped at load |
| `when.dimension` | list of ints | any | Dimension id must be in the list |
| `when.layer` | `any` / `surface` / `cave` | `any` | `SpawnLayerFilter.isCavePick` at the leader pos. Unknown string → party dropped |
| `when.time` | `both` / `day` / `night` | `both` | `world.isDaytime()`. Unknown string → party dropped |
| `when.biomes` | list of biome registry ids | any | Leader-pos biome must be in the list |
| `when.minheight` | int | none | Leader `y <` this → no match |
| `when.maxheight` | int | none | Leader `y >` this → no match |
| `companions[].mob` | entity id | — | Must resolve to an `EntityLiving` class; unknown or non-living logged once and skipped |
| `companions[].min` | int | 0 | Negative clamps to 0 |
| `companions[].max` | int | 0 | Negative, or `max < min`, skips that companion. Both keys omitted = 0–0, so nothing spawns |

## How Tweaks hooks in

**Layer filter:** `SpawnTypeLists.load` and `SpawnStructureLists.load` in preInit. `SpawnLayerFilter` on `PotentialSpawns` **LOWEST**, registered in **postInit** (after InControl): strip exclusive layer ids, then **one entry per entity class** (last wins). Cave pick: `Y < Cave Max Y` **and** sky light ≤ Max Cave Sky Light. If InControl is loaded, a cave strip of a **surface-only** id is skipped when `StructureCache` says the pick is in a structure that lists that id. `SpawnLayerFilter` does not import `StructureCache`; `SpawnStructureExemption` is wired from `postInit`.

**Structure cache:** optional `mixins.aqtweaks.incontrol.json` `MixinStructureCache` on `parseStructureData` RETURN. Helper `StructureCacheHooks` adds BB chunks. Do not late-mixin `World` / `WorldServer`.

**Pack fill:** `MixinWorldEntitySpawner` only. ThreadLocal entry via Redirect of `getSpawnListEntryForTypeAt` while `findChunksForSpawning` runs. Redirect `spawnEntity`: after success, `SpawnPackFiller` extras then `SpawnParties`. Redirect `getMaxSpawnPackSize` → **1** when fill applies. Recursion guard while filling (`filling` also blocks companion parties from stacking).

**Hostile cap:** Redirect `EnumCreatureType.getMaxNumberOfCreature()` in the same mixin. MONSTER → cfg (default 200). Other types stay vanilla 10 / 15 / 5. Do **not** call `getMaxNumberOfCreature()` from that redirect (recursion). Cage spawners and TC portals unused.

**Cage delay:** `MixinMobSpawnerBaseLogic` on `updateSpawner` in **`mixins.aqtweaks.early.json`** (jar `MixinConfigs`), not the late Tweaks json — late prepare hits `MobSpawnerBaseLogic` after it is already loaded and crashes boot. HEAD records whether delay started at 0; RETURN on the **server** sets `spawnDelay` to **Cage Fail Recheck Delay** (default 20) if it is still 0 (failed attempt). Does not call `resetTimer()` on fail. Does not reset after vanilla decrements 1→0 without spawning. Success / max nearby stay vanilla 200–800. Master off → delay can stick at 0. Does not make cultists/blazes spawn, and does not run pack fill/parties.

Range: cfg override `modid:path=min-max`, else pack **tier** for that entity id (common 2–4, uncommon 1–2, rare/elite 1, …), else the picked entry. Clamp to Group Size Cap. `1..1` skips filler (still pack-size 1). Mixed groups still run if fill is skipped.

Ids missing from `mob_tier.json` keep the picked entry (vanilla 4–4 possible).

## Live config (`aqtweaks_spawning.cfg`)

| Knob | Default | Live? |
| --- | --- | --- |
| Enable Spawning Module | true | yes |
| Filter Potential Spawns | true | yes |
| Spawn Type File | `arcanaquest/mob_overworldspawntype.json` | reload on Tweaks cfg change |
| Spawn Structure File | `arcanaquest/mob_structurespawns.json` | reload on Tweaks cfg change |
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
| Spawn Tier File | `arcanaquest/mob_tier.json` | reload on Tweaks cfg change |
| Spawn Rules File | `arcanaquest/mob_spawnrules.cfg` | reload on Tweaks cfg change |
| Enable Mixed Groups | true | yes |
| Spawn Parties File | `arcanaquest/mob_spawnparties.json` | reload on Tweaks cfg change |
| Hostile Mob Cap | 200 | yes |
| Cage Fail Recheck Delay | 20 | yes |

JSON layer / parties / structure files: edit needs **restart** (or a Tweaks cfg save to trigger reload). Override map rebuilds on Tweaks cfg change. Master off → vanilla 70 cap, no filter, no fill, no parties, no cage fail delay.

## Files

| Piece | Role |
| --- | --- |
| `spawning/SpawnTypeLists.java` | Pack JSON exclusive sets |
| `spawning/SpawnStructureLists.java` | Pack JSON structure → ids (no `StructureCache` import) |
| `spawning/SpawnStructureExemption.java` | `StructureCache.isInStructure` (InControl present) |
| `spawning/StructureCacheHooks.java` | Feature/child `BB` → chunk longs |
| `spawning/SpawnLayerFilter.java` | `PotentialSpawns` LOWEST; layer strip; last-per-class (drop vanilla 4–4); structure cave keep |
| `spawning/SpawnGroupCounts.java` | Pack tier id → group min/max |
| `spawning/SpawnGroupSizes.java` | Overrides + tier range + roll target + findChunks cap |
| `spawning/SpawnPackContext.java` | ThreadLocal pack entry |
| `spawning/SpawnPackFiller.java` | Extra placements; InControl min-distance |
| `spawning/SpawnParties.java` | Mixed groups from pack JSON |
| `spawning/CageSpawnerHooks.java` | Cage fail-path delay (HEAD/RETURN) |
| `mixin/MixinWorldEntitySpawner.java` | Capture entry, fill pack, pack-size 1, hostile cap. Do **not** mixin `WorldServer`. |
| `mixin/incontrol/MixinStructureCache.java` | Expand structure BB chunks. `mixins.aqtweaks.incontrol.json` |
| `mixin/MixinMobSpawnerBaseLogic.java` | Cage `updateSpawner` fail delay. Remap true. `mixins.aqtweaks.early.json` |
| `ArcanaQuestTweaksConfig.SpawningModuleConfig` | `aqtweaks_spawning.cfg` |

## Do not regress

- Intersection ids on **both** layers. Unknown ids not stripped. Duplicate vanilla+InControl rows: last (InControl) kept. Surface-only ids stay stripped in ordinary caves; listed structure ids remain on cave picks only inside those structures. Surface-only ids stay stripped in ordinary caves; listed structure ids remain on cave picks only inside those structures.
- Forest Y≥60 under leaves stays surface pool.
- Nether/End / non-MONSTER unchanged when those flags are on (layer filter / pack fill). Hostile **cap** still applies in every dim for MONSTER.
- Rare `1..1` stays singles. Feral goblin default override 3–5 until the cfg line is removed.
- Cage spawner / TC lesser portal: no pack fill, no party, unchanged cap formula (they do not use `findChunksForSpawning`). Failed cage attempts wait **Cage Fail Recheck Delay** (default 20), not 0 and not vanilla 200–800. Success still 200–800.
- Depths `getRandomChunkPosition` mixin untouched. InControl player-distance mixin still applies to vanilla tries.
- Java 21 `--release`. Stamina packets stay 0–2.

## Verify

Boot: `Loaded spawn types from …`; `Loaded structure spawns from …` when that JSON is present; `Loaded N spawn parties from …`; `Loaded pack spawn group sizes for N mobs.` when pack files present. No mixin fail on `MixinWorldEntitySpawner` / `findChunksForSpawning`, `MixinMobSpawnerBaseLogic` / `updateSpawner`, or `MixinStructureCache` / `parseStructureData`. Closed cave: dwarf/cave_spider/krake yes, Dryad/witch/Wildkin no. Better Mineshaft **non-origin** chunk: witch/vindicator/pillager can appear; ordinary closed cave away from the shaft still has no witch/illager. Night plains: creeper packs 1–2 (not 4); enderman 1; zombie/skeleton/spider 2–4 mixed not always 4. `goblin_feral` 3–5 when the first lands. Fill Pack Size off: old singles. Master off or JSON missing: no layer strip (last-per-class still runs if the module is on). Missing parties JSON: mixed groups off, no crash. Missing structure JSON: no cave exemption, no crash; BB cache mixin still runs. Natural Overworld `thaumcraft:cultistcleric`: 2–3 knights + 2–3 CR archers (CheckSpawn / crimsoncult stage can still deny). Portal / cage cleric: no party. Failed cultist/blaze cage: Delay ≈ 20 (cfg), not 0, not 200–800. Legal zombie cage still attempts after delay 1→0; success still 200–800. Hostile cap 200 lets natural MONSTER count exceed the old 70-scaled ceiling; cfg 70 restores vanilla. Animals/water/ambient caps unchanged.
