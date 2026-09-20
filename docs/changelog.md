# Changelog (1.8)

Stay on version **1.8** until a plan bumps `ArcanaQuestTweaks.VERSION`.

## 2026-09-20 — Quality Tools Module

- Loot/drops stamp Quality Tools tags before pickup; crafted gear stays untagged (living-update first roll skipped). Wear `gray` at ≤20% remaining (not on Broken); break without Salvage drops `dark_gray` and keeps QualityBase. Dawnstone Anvil upgrades with Crafting Runes on a strict color ladder (rune consumed). Pack `tools.json` still needs a `dark_gray` Broken entry.

## 2026-09-20 — Game Stages / Chisel

- **Chisel** crafts (GUI `SlotChiselSelection.craft` and in-world `ItemChisel.canChisel`) refuse Recipe-Staged outputs unless `GameStageHelper.hasStage` is true. Pack allowlist: `apprentice_builder`, `experienced_builder`, `master_builder`. Red action bar `chat.aqtweaks.gamestages.chisel_locked`. Optional mixin json; AutoChisel and Chisels and Bits unchanged.

## 2026-09-20 — release hardening

- **Java target stays 21** (`options.release = 21`, class major 65). Do not `--release 8`. Mixin json is `JAVA_21`. Fugue refuses 66+ only.
- **Village unload:** `VillagePlate` is loaded when village Events register so Forge’s `EventSubscriptionTransformer` does not ClassReader empty bytes during `stopServer`.
- **Charm** is `@Mod required-after` (Curse / pack prerequisite). `mixins.aqtweaks.charm.json` is `required: true` on jar `MixinConfigs` with the early json; compile-hard `ASMHooks`. Isolated boot without Charm is expected to fail.
- Shared `mixins.aqtweaks.refmap.json` is packaged; mixin configs point at it (silences missing per-json refmap warnings).
- **Evasion:** drop unresolvable CAD `trait|` rows such as instance `trait|elenaidodge2:dodge`. Requirement is agility 16.
- Gradle `verifyReleaseJar` (via `check`): remapped jar has `VillagePlate`, mixin json, refmap, and every class major 65.
