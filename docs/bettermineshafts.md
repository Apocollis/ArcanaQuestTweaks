# Better Mineshafts module (1.8)

Last updated: 2026-09-18. Vanilla inherited `@Shadow` on BM classes: see the note on `remap = false` annotation strings in the README (Cleanroom named supers).

Optional mixin layer if YUNG’s Better Mineshafts is present (`mixins.aqtweaks.bettermineshafts.json`, `required: false`). Tweaks Forge cfg: `aqtweaks_bettermineshafts.cfg`.

## Locked intent

`/locate Mineshaft` and `isInsideStructure("Mineshaft")` should agree with where BM actually registered tunnels — not Y=64 air and not the Start AABB center at ~Y 136.

On **new chunks**, a locate can-spawn hit must get a Start (same Tweaks ocean/beach skip, spacing, and rate; biome **provider** at Y=64 — not the loaded RTG column). Already-generated empty pins are not retro-pasted; locate must skip them.

Do **not** move shafts into Depths −Y. Do **not** edit BM’s `bettermineshafts-1_12_2.cfg` spawn rate as the source of truth while Tweaks Enable is on — Tweaks owns the roll.

Keep hillside/cliff openings. Density is Tweaks rate × spacing, not BM 0.003 on every land chunk.

## How the parent works

Jar: `BetterMineshaftsForge-1.12.2-2.2.1`. `EventMineshaftGen` (`LOW`) replaces `InitMapGenEvent.MINESHAFT` with `MapGenBetterMineshaft`. RTG already stores that instance and calls `generate` / `generateStructure` / `getNearestStructurePos("Mineshaft")`. `getStructureName()` stays `"Mineshaft"` (Mineshaft.dat + locate name). Start NBT id is `bettermineshafts:BetterMineshaft`.

Better Caves 2.0.4 can wrap MINESHAFT only when `originalGen == newGen`; BM runs after that and always `setNewGen`, so BM owns the generator.

Vanilla `MapGenMineshaft.getNearestStructurePos` (`func_180706_b`) is a can-spawn spiral. It does **not** read `structureMap` and always returns `(chunk<<4)+8, 64, …`. BM `canSpawn` used `world.getBiome` (chunk biome once generated) plus `rand` seeded by the caller — locate and generate seeded that rand differently, so a pin could miss a Start.

`VerticalEntrance` initial box is `maxY = 256` so a hillside opening can paste in later chunks. `addComponentParts` returns false when surface height is `< 60` or there is no drop-off (typical RTG plains). Vanilla `StructureStart.generateStructure` then `iterator.remove()`s that piece. Tweaks’ village snapshot iterator still removes on false. Remaining tunnels stay in `structureMap`, but no component contains the Y=64 locate pin.

Start ctor picks tunnel Y from `MineshaftVariantSettings.minY`/`maxY` (0 treated as 17/37). Those settings objects are shared per biome variant — Tweaks must copy before writing Y.

## Design plan

- `BetterMineshaftCanSpawn`: if Tweaks Enable is false, return null (parent canSpawn). Else chunk spacing (`floorMod` both axes); then optional ocean/beach from `BiomeProvider` at `(cx<<4)+8, 64, (cz<<4)+8`; spawn roll on a per-chunk `Random` using Tweaks rate (not BM `Configuration.mineshaftSpawnRate`). Land path still discards one `rand.nextDouble()` so Start layout stays on the parent stream.
- `MixinMapGenBetterMineshaft` HEAD of `func_75047_a`: that canSpawn. `MixinMapGenMineshaftLocate` (target `MapGenMineshaft`, which declares `func_180706_b`; BM does not override it and Mixin only injects into methods the target itself declares) RETURN of `func_180706_b`, guarded `instanceof MapGenBetterMineshaft`: nearest registered Start pin; else vanilla pin if the chunk is not generated-empty; else keep walking the spiral. Pin Y=24 when unexplored. Ranking: the nearest registered Start pin and the nearest predicted can-spawn chunk compete by distance; `findUnexplored` ignores registered Starts; any candidate within 32 blocks (XZ) of the player is skipped, so a repeat `/locate` from inside a mineshaft moves on to the next one.
- `MixinVerticalEntrance` RETURN of `func_74875_a`: if the parent returned false, keep a **small** box around `centerPos` (Y through Y+6, ±2 XZ) and return true. Do not shrink `maxY` while a cliff opening exists. Set the box via Reflect (`setBoundingBox` / `boundingBox` / `field_74887_e`).
- `MixinStructureStartMineshaftBox` (target vanilla `StructureStart`, guarded `instanceof MapGenBetterMineshaft.Start`) RETURN of `func_75068_a`: `StructureAccess.updateStructureStartBoundingBox` after piece removal. It cannot live on BM's `Start`, which declares only constructors. `MixinMapGenBetterMineshaftStart` HEAD ctor `@ModifyVariable` (settings arg, local index 5): `BetterMineshaftStartSettings.withTweaksY` — local copy with Tweaks min/max Y, never mutate shared variant settings.

Mineshafts may sit under RTG rivers/beaches the provider still calls plains.

## Files

| Piece | Role |
| --- | --- |
| `ArcanaQuestTweaksConfig.BetterMineshaftsModuleConfig` | Nested `general`; `aqtweaks_bettermineshafts.cfg` (no BM imports) |
| `bettermineshafts/BetterMineshaftCanSpawn.java` | Spacing first, then provider biome, then Tweaks rate |
| `bettermineshafts/BetterMineshaftStartSettings.java` | Local Start Y copy |
| `bettermineshafts/BetterMineshaftLocate.java` | Pin + skip generated-empty |
| `bettermineshafts/VerticalEntranceAccess.java` | Entrance `centerPos` |
| `mixin/bettermineshafts/MixinMapGenBetterMineshaft.java` | canSpawn |
| `mixin/bettermineshafts/MixinMapGenMineshaftLocate.java` | locate pin (on `MapGenMineshaft`, BM-guarded) |
| `mixin/bettermineshafts/MixinMapGenBetterMineshaftStart.java` | Tweaks Y (ctor settings) |
| `mixin/bettermineshafts/MixinStructureStartMineshaftBox.java` | Refresh Start AABB after `generateStructure` (on `StructureStart`, BM-guarded) |
| `mixin/bettermineshafts/MixinVerticalEntrance.java` | Failed openings: mark the piece (`aqtweaks$isFallback`) and keep the underground stub; the shaft itself is built by the Start hook |
| `bettermineshafts/MineshaftSurfaceShaft.java` | Ladder shaft + plank collar from the tunnel to the ground. `carveForStart` aims it at the mineshaft: straight down the hub when the hub lies inside a sibling piece box, otherwise at the nearest sibling box (floor + 1). Logs one `[AQ-MINESHAFT]` line per Start |
| `mixins.aqtweaks.bettermineshafts.json` | Optional late mixin config (`required: false`) |

## Live config

`config/arcanaquesttweaks/aqtweaks_bettermineshafts.cfg` (`category = ""`, nested `general`). Instance files keep missing keys at Java defaults. Do not put primitives on the `@Config` root (Forge empty-category crash).

| Category / Key | Default | Effect |
| --- | --- | --- |
| `general` / Enable Better Mineshafts Tweaks | true | False → parent canSpawn and parent Y; locate/stub still run |
| `general` / Mineshaft Spawn Rate | 0.001 | Per eligible chunk (BM 0.003 unused while Enable) |
| `general` / Chunk Spacing | 4 | Both chunk X and Z must be multiples; 1 = no grid |
| `general` / Skip Ocean And Beach | true | Provider ocean/beach skip |
| `general` / Surface Shaft Fallback | true | When BM cannot build its entrance (surface below Y60 / no drop-off, typical on RTG), build a 1x1 ladder shaft from tunnel level to the ground with a raised plank collar. Not built under water or when the tunnel is within 3 blocks of the surface. Off keeps the closed stub. New chunks only |
| `general` / Tunnel Min Y / Tunnel Max Y | 17 / 37 | Start tunnel Y; min clamped to 1 (no Depths −Y) |

Worldgen applies to **new chunks only**. GUI cfg change does not retrofit old terrain.

## Do not regress

- Do not put BM mixins in **required** `mixins.aqtweaks.json`.
- Do not add `@Mod required-after:bettermineshafts`.
- Do not import BM types from always-on bus classes (`ArcanaQuestTweaksConfig`).
- Do not put string/primitive fields on a `@Config(..., category = "")` class (Forge empty-category crash). Nest them under `general`.
- Do not mutate shared `MineshaftVariantSettings`.
- Do not leave a 15×256 air column as Mineshaft for InControl / `isInsideStructure`. The fallback shaft's box is only the 3×3 column from tunnel level to one block above the ground.
- Village `MixinStructureStartVillagePaste` still snapshots/skips wet houses only.

## Verify

0. Flat RTG plains mineshaft (BM entrance refused): a plank collar with a hole is visible at the surface, a ladder runs down to the tunnel, and `isInsideStructure` is true along the shaft but false high in the sky.
1. New Overworld, unexplored `/locate Mineshaft` → TP → tunnels at the pin; `isInsideStructure("Mineshaft")` true at the pin and tunnel Y; false high in the sky.
2. Flat plains (no cliff opening): locate is tunnel Y at the start chunk, not ~136.
3. After visiting a known **old** empty pin, next `/locate` is not that chunk.
4. Relog: Mineshaft.dat Starts still win. Log must not say `setBoundingBox` / `func_75072_c` / `func_75047_a` was not located.
5. New terrain is rarer than the post-provider flood (spacing 4 × 0.001). Hill/cliff openings still appear sometimes.
6. Disable Tweaks Enable: parent density/canSpawn; no mixin apply crash.
7. Villages unchanged.
