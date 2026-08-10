# Arcana Quest Tweaks Project Rules

## 1. Code Change Process
- **CRITICAL / DEFAULT RULE**: Never make any code, script, asset, or configuration changes to the codebase without first presenting an implementation plan in `implementation_plan.md` and obtaining explicit user approval (`proceed`). This is a mandatory, default rule for all tasks.

## 2. Source of Truth & Workflow
- **Authoritative repo**: This Git directory (`ArcanaQuestTweaks`). Edit here only.
- **Do not** maintain a parallel editable copy under `ArcanaQuestTweaksDEVBOX` for day-to-day work.
- **Build**: `powershell -ExecutionPolicy Bypass -File .\build_gradle.ps1`
  - Populates `libs/` from the CurseForge instance mods folder
  - Builds a **remapped** jar (`defaultRemapJar = true`)
  - Deploys to workspace `mods/` and `C:\Users\hughe\curseforge\minecraft\Instances\Arcana Quest DEVBOX\mods`
- **Exclusions from git** (see `.gitignore`): `build/`, `.gradle/`, `libs/`, `mods/`, `temp/`, `run/`

## 3. Structure Loot Table Integration Rule
- **Structure-Specific Loot Files**: Always add LootTweaker rules directly into the existing structure-specific `.zs` files under `scripts/loottweaker/` (e.g. `loot-simple_dungeon.zs`, `loot-abandoned_mineshaft.zs`, `loot-woodland_mansion.zs`, `loot-desert_pyramid.zs`, `loot-jungle_temple.zs`, `loot-stronghold_library.zs`, `loot-nether_bridge.zs`, `loot-end_city.zs`). Do NOT create standalone mod-specific loot scripts (such as `grimoire_gaia_loot.zs` or `round3_dungeon_loot.zs`).
