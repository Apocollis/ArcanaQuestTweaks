# Better Mineshafts module (1.8)

Last updated: 2026-09-14.

Optional mixin layer if YUNG’s Better Mineshafts is present (`mixins.aqtweaks.bettermineshafts.json`, `required: false`). No Tweaks Forge cfg.

## Locked intent

`/locate Mineshaft` and `isInsideStructure("Mineshaft")` should agree with where BM actually registered tunnels — not Y=64 air and not the Start AABB center at ~Y 136.

Do **not** move shafts into Depths −Y. Do **not** change BM spawn rate or ocean/beach `canSpawn` (biome-provider vs RTG landscape mismatch stays parent behavior).

## How the parent works

Jar: `BetterMineshaftsForge-1.12.2-2.2.1`. `EventMineshaftGen` (`LOW`) replaces `InitMapGenEvent.MINESHAFT` with `MapGenBetterMineshaft`. RTG already stores that instance and calls `generate` / `generateStructure` / `getNearestStructurePos("Mineshaft")`. `getStructureName()` stays `"Mineshaft"` (Mineshaft.dat + locate name). Start NBT id is `bettermineshafts:BetterMineshaft`.

Better Caves 2.0.4 can wrap MINESHAFT only when `originalGen == newGen`; BM runs after that and always `setNewGen`, so BM owns the generator.

Vanilla `MapGenMineshaft.getNearestStructurePos` (`func_180706_b`) is a can-spawn spiral. It does **not** read `structureMap` and always returns `(chunk<<4)+8, 64, …`.

`VerticalEntrance` initial box is `maxY = 256` so a hillside opening can paste in later chunks. `addComponentParts` returns false when surface height is `< 60` or there is no drop-off (typical RTG plains). Vanilla `StructureStart.generateStructure` then `iterator.remove()`s that piece. Tweaks’ village snapshot iterator still removes on false. Remaining tunnels stay in `structureMap`, but no component contains the Y=64 locate pin.

## Design plan

- `MixinVerticalEntrance` RETURN of `func_74875_a`: if the parent returned false, keep a **small** box around `centerPos` (Y through Y+6, ±2 XZ) and return true. Do not shrink `maxY` while a cliff opening exists.
- `MixinMapGenBetterMineshaftStart` RETURN of `func_75068_a`: `updateBoundingBox()` (`func_75072_c`) after piece removal.
- `MixinMapGenBetterMineshaft` RETURN of `func_180706_b`: nearest registered Start pin (entrance `centerPos`, else start-chunk at `minY+4`). If none, retarget the vanilla Y=64 pin to Y=24.

`VerticalEntranceAccess` is only implemented by the entrance mixin. `BetterMineshaftLocate` is only called from the BM mixin json.

## Files

| Piece | Role |
| --- | --- |
| `bettermineshafts/BetterMineshaftLocate.java` | Pin + nearest-Start pick |
| `bettermineshafts/VerticalEntranceAccess.java` | Entrance `centerPos` |
| `mixin/bettermineshafts/MixinMapGenBetterMineshaft.java` | Locate RETURN |
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

1. New Overworld chunks, BM loaded. `/locate Mineshaft` → TP. Spectator: tunnels or shaft near that XZ; `isInsideStructure("Mineshaft")` true at the pin and at tunnel Y; false high in the sky.
2. Flat plains (no cliff opening): locate is tunnel Y at the start chunk, not ~136.
3. Relog: same nearest mineshaft. Optional json skip if BM jar absent (no mixin apply crash).
4. Villages unchanged.
