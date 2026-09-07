# Spawning module (1.8)

Last updated: 2026-09-07.

Config: `config/arcanaquesttweaks/aqtweaks_spawning.cfg` (knobs only). Pack lists: `config/arcanaquest/mob_overworldspawntype.json` (DEVBOX; Tweaks does **not** ship or write this file). Always registered. No mixin. No InControl compile dep.

## Locked intent

InControl `potentialspawn.json` keeps one biome pool for surface and cave hostiles. Vanilla `WorldEntitySpawner` picks **one** `SpawnListEntry` at `WorldServer.getSpawnListEntryForTypeAt` and does **not** reroll when `spawn.json` denies that pick. Tweaks strips **surface-only** ids from cave picks and **underground-only** ids from surface picks at the **exact pick pos**, so the weighted roll is already a legal layer. `spawn.json` stays the safety net.

Do **not** put `seesky` / height on `potentialspawn.json` (seed-Y deadlock with Depths). Do **not** copy mob id arrays into Tweaks cfg. Do **not** treat ids that appear in **both** JSON arrays as exclusive (zombie, goblin, orc, … stay both-layer).

**Out of scope:** cave-floor snap and dual-pass spawn seeds (Depths solid-rock waste).

## How parents work

**Vanilla / Forge:** `getSpawnListEntryForTypeAt` → chunk generator `getPossibleCreatures` → `ForgeEventFactory.getPotentialSpawns` (`WorldEvent.PotentialSpawns`) → `WeightedRandom`. The event list is a mutable copy. The entry is reused for a few XZ jitters at the **same Y** (`ΔY` is always 0).

**InControl:** `potentialspawn.json` fills that list; `spawn.json` deny on `CheckSpawn` does not pick another entry. Surface deny in this pack is `maxheight: 60` **and** `seesky: false`.

**Pack JSON** `mob_overworldspawntype.json`: `{ "surface": [...], "underground": [...] }` — Gaia, Lycanites, Betweenlands, vanilla, etc.

| Set | Cave pick | Surface pick |
| --- | --- | --- |
| surface − underground | **remove** | keep |
| underground − surface | keep | **remove** |
| intersection | keep | keep |
| in neither | keep | keep |

## How Tweaks hooks in

`SpawnTypeLists.load` in **preInit** (Forge config dir + cfg relative path). Missing or bad JSON: log once, filter no-ops. JSON edit needs **restart** (same as Comfort). Tweaks spawning cfg change reloads the JSON.

`SpawnLayerFilter` on `WorldEvent.PotentialSpawns` **`LOWEST`** (after InControl). Mutate `event.getList()`; do not cancel.

**Cave pick:** `pos.getY() < Cave Max Y` **and** `getLightFor(SKY, pos) <= Max Cave Sky Light`. Do **not** use bare `canSeeSky` (false under trees). Surface pick otherwise. Overworld + `MONSTER` only by default.

Registry id from `EntityList.getKey(entry.entityClass)`. Null key kept.

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

Instance cfg keeps old keys. Path is relative to the Forge config directory.

## Files

| Piece | Role |
| --- | --- |
| `spawning/SpawnTypeLists.java` | Load pack JSON; exclusive sets |
| `spawning/SpawnLayerFilter.java` | `PotentialSpawns` LOWEST |
| `ArcanaQuestTweaksConfig.SpawningModuleConfig` | `aqtweaks_spawning.cfg` |

## Do not regress

- Intersection ids must spawn on **both** layers. Unknown ids must not be stripped.
- Forest Y≥60 under leaves stays **surface** pool (skylight, not `canSeeSky`).
- Nether/End, animals, water, ambient unchanged when Monster/Overworld flags are on.
- No mixin on `WorldServer` / `WorldEntitySpawner`. No Tweaks write to pack InControl JSON.
- Java 21 `--release`. Stamina packets stay 0–2.

## Verify

Boot DEVBOX: log `Loaded spawn types from …`; `aqtweaks_spawning.cfg` has **no** mob arrays. Closed cave Y≪60, skylight 0: dwarf / cave_spider / krake can appear; Dryad / witch / Wildkin do not; zombie/goblin still allowed; denser than pre-filter. Night surface: Dryad / witch / Wildkin can appear; dwarf / cave_spider / beholder do not; zombie still allowed. Master off or JSON deleted: no Tweaks strip.
