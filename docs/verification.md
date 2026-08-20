# Verification (1.6)

Last updated: 2026-08-20.

Manual release / smoke checklist. **No automated tests.** Harness: CurseForge **Arcana Quest DEVBOX**, remapped `ArcanaQuestTweaks-1.6.jar` in `mods/`. Algorithms and full checklists stay in module docs; this is the pack-level pass/fail.

Worldgen applies to **new chunks only**.

## Build artifact

- [ ] `.\gradlew.bat build` or `.\build_gradle.ps1` succeeds.
- [ ] Instance `mods/` has `ArcanaQuestTweaks-1.6.jar`, **not** `-dev`.
- [ ] Only one Tweaks jar (the deploy script deletes other `ArcanaQuestTweaks-*.jar`).

## Boot

- [ ] Client starts the full pack; no mixin apply crash from `mixins.aqtweaks.json`.
- [ ] Dedicated server: **not routinely tested** in this repo. If you ship a server, start one with the same mods and confirm it reaches “Done”.

### Optional-mod absences (safe)

These json files are `required: false`. Removing the parent should skip that json, not crash Tweaks:

| Remove | Expect |
| --- | --- |
| Grapple | No motor mixin; grapple stamina no-ops if not loaded |
| Dynamic Sword Skills | No skill feather gate |
| Astral Sorcery | No shrine mixins; no village `AQTSmallShrine` |
| Bewitchment | No Cambion mixins; no ritual wrap (handler not registered) |
| Mystical World | No hut skip/settle |

### Do not treat as optional

Missing **RTG, Depths Update, Better Caves, CoFH World, Recurrent Complex, or IvToolkit** with the current required mixin json can **fail mixin apply** at boot. This pack always ships them. Elenai Extended is `@Mod` **required-after**.

## Config

- [ ] After a Java default change, instance cfg still has the **old** value until edited (e.g. RTG Coast Buffer 32 vs 16).
- [ ] Change a live cfg in-game: stamina DSS costs refresh (`DssSkillCosts.invalidate`). Comfort JSON needs a restart (loaded in preInit).

## Worldgen (new chunks)

Flatten debug on → instance `logs/villagepatch.log` (not `latest.log`).

| Check | Expect | Doc |
| --- | --- | --- |
| Inland plains (example `-2897, 97, -2119`) | Flat plate, houses on it, blend to hills | [rtg.md](rtg.md) |
| Sea-level forest (`-524, 64, 5893`) | Path, lamps, houses **same Y** | rtg |
| Beach ~16 from water | Village may start; buildings inland; **no** sand piers | rtg |
| Coral reef / kelp / ocean / river well (`-3452, 63, -2191`) | No village start (`veto` … `ocean_well` / `coast_ocean` / …) | rtg |
| Docks | Wood OK; no land mesa under dock AABB | rtg |
| Below Y0 Overworld | Deepslate fill, AQ caves, Y0 mouths on land, **no** ocean drain | [depths.md](depths.md) |
| Fog / sky below Y0 | Dark fog ~32–52; no skybox | depths |

Log snippets if debug on: `veto chunk=`, `flatten chunk=`, `waystone relocate`, `astral shrine skip ocean floor`.

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
