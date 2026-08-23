# Verification (1.6)

Last updated: 2026-08-23.

Manual release / smoke checklist. **No automated tests.** Harness: CurseForge **Arcana Quest DEVBOX**, remapped `ArcanaQuestTweaks-1.6.jar` in `mods/`. Algorithms and full checklists stay in module docs; this is the pack-level pass/fail.

Worldgen applies to **new chunks only**.

## Build artifact

- [ ] `.\gradlew.bat build` or `.\build_gradle.ps1` succeeds.
- [ ] Instance `mods/` has `ArcanaQuestTweaks-1.6.jar`, **not** `-dev`.
- [ ] Only one Tweaks jar (the deploy script deletes other `ArcanaQuestTweaks-*.jar`).

## Boot

- [ ] Client starts the full pack; no mixin apply crash from `mixins.aqtweaks.json`.
- [ ] Mixin log does **not** say Tweaks mixins require class version 69 (Java 21 class files).
- [ ] Wait through full JEI / ThaumicJEI / **TC6 Aspects 4 JEI** load. Title screen stays up. No `hs_err_pid*.log`.
- [ ] Dedicated server: **not routinely tested** in this repo. If you ship a server, start one with the same mods and confirm it reaches “Done”.

### Optional-mod absences (safe)

These json files are `required: false`. Removing the parent should skip that json, not crash Tweaks:

| Remove | Expect |
| --- | --- |
| Grapple | No motor mixin; grapple stamina no-ops if not loaded |
| Dynamic Sword Skills | No skill feather gate |
| Astral Sorcery | No shrine mixins; no village `AQTSmallShrine` |
| Bewitchment | No Cambion/circle mixins; no ritual wrap (handler not registered) |
| Mystical World | No hut skip/settle |
| Biomes O' Plenty | No quicksand village skip mixin; hot spring comfort no-ops |

### Do not treat as optional

Missing **RTG, Depths Update, Better Caves, CoFH World, Recurrent Complex, or IvToolkit** with the current required mixin json can **fail mixin apply** at boot. This pack always ships them. Elenai Extended is `@Mod` **required-after**.

## Config

- [ ] After a Java default change, instance cfg still has the **old** value until edited (e.g. RTG Coast Buffer 32 vs 16).
- [ ] Change a live cfg in-game: stamina DSS costs refresh (`DssSkillCosts.invalidate`). Comfort JSON needs a restart (loaded in preInit).

## Worldgen (new chunks)

`Village Flatten Debug` is **off** by default. Turn it on only while diagnosing; it appends every line to instance `logs/villagepatch.log` (not `latest.log`) and stalls chunk gen.

| Check | Expect | Doc |
| --- | --- | --- |
| Flying new chunks (near and far from villages) | Away from villages, chat TPS stays near 20 (no growing hitch as more towns exist). A village still plates. With debug off, `villagepatch.log` stays empty | [rtg.md](rtg.md) |
| Coastal village ocean face (new chunks) | Cleaner XZ outline (no 1-block jetties; 1-block ocean notches in the pad filled). Stone brick on the whole rim below the plate; sand/grass on top; no open-water pier; inland hill cliffs not bricked | rtg |
| Village small shrine / waystone | Complete shrine on the plate (not half over a pond). No dirt/grass collar. F3 River/ocean columns still skipped | rtg |
| River through forest/shrubland (new chunks) | No well in the channel; no house/RC/plank dock on F3 River; land plate inland | rtg |
| Large Astral temple (new chunks) | Raw marble under the pad, not dirt; no floating logs/leaves in or above the AABB | rtg |
| Cambion house on a slope (new chunks) | Pad flush with plains; cobble on the grass; door +1 above cobble; holes under footprint filled; no mesa; village overlap still cancels | rtg |
| Mystical barrow or hut next to a village | Relocates (step 8, up to 32) or skips; barrow not plated | rtg |
| Bewitchment circle / menhir / wickerman next to a village | Relocates or skips; no extra plate | rtg |
| Desert village (new chunks) | One sand plate through yards; no toothed red-sand holes; no BOP quicksand in land boxes | rtg |
| Inland plains (example `-2897, 97, -2119`) | Flat plate, houses on it, blend to hills | rtg |
| Sea-level forest (`-524, 64, 5893`) | Path, lamps, houses **same Y** | rtg |
| Beach ~16 from water | Village may start; buildings inland; **no** sand piers or plank bridges | rtg |
| Coral reef / kelp / ocean well (`-3452, 63, -2191`) | No village start unless a dry slot exists in retry range (`veto` `ocean_well` / `coast_ocean`) | rtg |
| River well | Walks inland (`well-walk`); no plank dock; plate at land Y not riverbed | rtg |
| Dry plains well below Y 64 | Kept; `plateSample … target=64`; not `flooded_well` | rtg |
| Flooded plains well | Raised to min well height if not never-raise; `/locate Village` can find it | [villagegen_info.md](villagegen_info.md) |
| Dry plains village | RC paste Y ≈ plate Y; well chunk `pad>0` | villagegen_info |
| House/RC on a plains lake edge | Omitted, **or** dirt pad for the **12-block** footprint (lake beyond the pad stays water) | rtg |
| `/aqvillage` (OP) | Relog Overworld, run **before** new chunks: plate Y+1, ~6 off the well (`unexplored` or `known`), not `No village generator on this world`. Nether still errors (INFO line in `latest.log`). Non-OP denied. | rtg |
| Village `AQTSmallShrine` (new villages) | Uncommon (weight 5). Fly several towns until one appears; then complete marble, no dirt collar, at most one. Missing shrine in some villages is OK. | rtg |
| Pad top | Native RTG surface (sand/grass). `biomesoplenty:mud` → loamy `grass:2` | rtg |
| New inland `/locate Village` | Houses and roads present; `villagepatch.log` `landBoxes` ≫ 1. Already-visited ghost wells stay empty | rtg |
| Water pieces | No waystone/house in a lake; wet path retries inland; no oak plank path over leftover open water; **no** roads/houses in F3 River or ocean | rtg |
| Swamp village plate | Well on grass, not over a ravine; roads level with houses; overlapping pads; `seal chunk=` in debug | rtg |
| Hill village (new chunks) | All pieces on **one** well Y; no chunk-border stone wall; unused AABB corners stay hills | rtg |
| Under a house at plate-4 | `isInsideStructure("Village")` true; below well floor false | rtg |
| Below Y0 Overworld | Deepslate fill, AQ caves, Y0 mouths on land, **no** ocean drain | [depths.md](depths.md) |
| Fog / sky below Y0 | Dark fog ~32–52; no skybox | depths |

Log snippets if debug on: `veto chunk=`, `forget chunk=`, `flatten chunk=`, `seal chunk=`, `waystone relocate`, `village piece skip water floor charm`, `astral small shrine village piece`.

## Stamina

Use the full list in [stamina.md](stamina.md) **Verify**. Minimum: jump costs/blocks, melee hit spend, bow draw, climb slide when empty, grapple hang vs climb vs grounded, HUD when dodge locked.

## Other modules (one-line)

| Module | Smoke |
| --- | --- |
| Gaia | Gaia melee second hit reduced by armor; magic protection still applies |
| Thaumcraft | First Nether visit warps after ~2s; sleep at dawn reduces warp; whispers underground on interval |
| Bewitchment | Listed ritual **finish** grants warp; halt does not |
| Comfort | Homestead icon while resting in a scored room; hot spring → cold resist if SD+BOP |
| Recipes | Pack boots without Metallurgy `generated/item/spartanweaponry` recipe spam |

## After mixin / parent bumps

Re-read mixin targets ([compatibility-matrix.md](compatibility-matrix.md) upgrade check). RC version bumps: confirm `RayMatcher.cast` `@Overwrite` still matches.
