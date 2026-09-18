# Better Mineshafts module (1.8)

Last updated: 2026-09-17. Vanilla inherited `@Shadow` on BM classes uses Reflect (Cleanroom named supers).

Optional mixin layer if YUNG’s Better Mineshafts is present (`mixins.aqtweaks.bettermineshafts.json`, `required: false`). No Tweaks Forge cfg.

## Locked intent

`/locate Mineshaft` and `isInsideStructure("Mineshaft")` should agree with where BM actually registered tunnels — not Y=64 air and not the Start AABB center at ~Y 136.

On **new chunks**, a locate can-spawn hit must get a Start (same ocean/beach skip and spawn rate, biome **provider** at Y=64 — not the loaded RTG column). Already-generated empty pins are not retro-pasted; locate must skip them.

Do **not** move shafts into Depths −Y. Do **not** change BM spawn rate.

## How the parent works

Jar: `BetterMineshaftsForge-1.12.2-2.2.1`. `EventMineshaftGen` (`LOW`) replaces `InitMapGenEvent.MINESHAFT` with `MapGenBetterMineshaft`. RTG already stores that instance and calls `generate` / `generateStructure` / `getNearestStructurePos("Mineshaft")`. `getStructureName()` stays `"Mineshaft"` (Mineshaft.dat + locate name). Start NBT id is `bettermineshafts:BetterMineshaft`.

Better Caves 2.0.4 can wrap MINESHAFT only when `originalGen == newGen`; BM runs after that and always `setNewGen`, so BM owns the generator.

Vanilla `MapGenMineshaft.getNearestStructurePos` (`func_180706_b`) is a can-spawn spiral. It does **not** read `structureMap` and always returns `(chunk<<4)+8, 64, …`. BM `canSpawn` used `world.getBiome` (chunk biome once generated) plus `rand` seeded by the caller — locate and generate seeded that rand differently, so a pin could miss a Start.

`VerticalEntrance` initial box is `maxY = 256` so a hillside opening can paste in later chunks. `addComponentParts` returns false when surface height is `< 60` or there is no drop-off (typical RTG plains). Vanilla `StructureStart.generateStructure` then `iterator.remove()`s that piece. Tweaks’ village snapshot iterator still removes on false. Remaining tunnels stay in `structureMap`, but no component contains the Y=64 locate pin.

## Design plan

- `BetterMineshaftCanSpawn`: ocean/beach from `BiomeProvider` at `(cx<<4)+8, 64, (cz<<4)+8`; spawn roll on a per-chunk `Random` (not caller `MapGenBase.rand`). Land path still discards one `rand.nextDouble()` so Start layout stays on the parent stream.
- `MixinMapGenBetterMineshaft` HEAD of `func_75047_a`: that canSpawn. RETURN of `func_180706_b`: nearest registered Start pin; else vanilla pin if the chunk is not generated-empty; else keep walking the spiral. Pin Y=24 when unexplored.
- `MixinVerticalEntrance` RETURN of `func_74875_a`: if the parent returned false, keep a **small** box around `centerPos` (Y through Y+6, ±2 XZ) and return true. Do not shrink `maxY` while a cliff opening exists. Set the box via Reflect (`setBoundingBox` / `boundingBox` / `field_74887_e`).
- `MixinMapGenBetterMineshaftStart` RETURN of `func_75068_a`: `Reflect.updateStructureStartBoundingBox` after piece removal.

Mineshafts may sit under RTG rivers/beaches the provider still calls plains.

## Files

| Piece | Role |
| --- | --- |
| `bettermineshafts/BetterMineshaftCanSpawn.java` | Provider biome + rate |
| `bettermineshafts/BetterMineshaftLocate.java` | Pin + skip generated-empty |
| `bettermineshafts/VerticalEntranceAccess.java` | Entrance `centerPos` |
| `mixin/bettermineshafts/MixinMapGenBetterMineshaft.java` | canSpawn + locate |
| `mixin/bettermineshafts/MixinMapGenBetterMineshaftStart.java` | Refresh Start AABB |
| `mixin/bettermineshafts/MixinVerticalEntrance.java` | Stub failed openings |
| `mixins.aqtweaks.bettermineshafts.json` | Optional late mixin config (`required: false`) |

## Live config

None. Always on when the json applies.

## Do not regress

- Do not put BM mixins in **required** `mixins.aqtweaks.json`.
- Do not add `@Mod required-after:bettermineshafts`.
- Do not import BM types from always-on bus classes.
- Do not leave a 15×256 air column as Mineshaft for InControl / `isInsideStructure`.
- Village `MixinStructureStartVillagePaste` still snapshots/skips wet houses only.

## Verify

1. New Overworld, unexplored `/locate Mineshaft` → TP → tunnels at the pin; `isInsideStructure("Mineshaft")` true at the pin and tunnel Y; false high in the sky.
2. Flat plains (no cliff opening): locate is tunnel Y at the start chunk, not ~136.
3. After visiting a known **old** empty pin, next `/locate` is not that chunk.
4. Relog: Mineshaft.dat Starts still win. Log must not say `setBoundingBox` / `func_75072_c` / `func_75047_a` was not located.
5. Villages unchanged.
