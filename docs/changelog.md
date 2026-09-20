# Changelog (1.8)

Stay on version **1.8** until a plan bumps `ArcanaQuestTweaks.VERSION`.

## 2026-09-20 — release hardening

- **Java target stays 21** (`options.release = 21`, class major 65). Do not `--release 8`. Mixin json is `JAVA_21`. Fugue refuses 66+ only.
- **Village unload:** `VillagePlate` is loaded when village Events register so Forge’s `EventSubscriptionTransformer` does not ClassReader empty bytes during `stopServer`.
- **Charm** is `@Mod required-after` (Curse / pack prerequisite). `mixins.aqtweaks.charm.json` is `required: true` on jar `MixinConfigs` with the early json; compile-hard `ASMHooks`. Isolated boot without Charm is expected to fail.
- Shared `mixins.aqtweaks.refmap.json` is packaged; mixin configs point at it (silences missing per-json refmap warnings).
- **Evasion:** drop unresolvable CAD `trait|` rows such as instance `trait|elenaidodge2:dodge`. Requirement is agility 16.
- Gradle `verifyReleaseJar` (via `check`): remapped jar has `VillagePlate`, mixin json, refmap, and every class major 65.
