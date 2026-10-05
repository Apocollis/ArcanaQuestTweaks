# Code review cleanup (October 2026)

Record of the refactor only. Gameplay tweaks made in the same weeks (Reach perks, sleep minimum, portal changes and so on) are in [../changelog.md](../changelog.md) and the module docs. This file exists so that, if something misbehaves after the refactor, there is a short map from symptom to cause.

Scope: a full senior-level review of `aqtweaks` 1.8 (docs against code, parent-mod hooks, performance, coding mistakes, reflection where the pack-first rule says to compile against the parent). Delivered in four batches, each built with `.\build_gradle.ps1` and boot-tested in the DEVBOX instance before the next.

## Why

- Several mixin injectors matched nothing and failed silently (no `defaultRequire`), so features such as the Depths seam pass, village world push/pop and BM mineshaft locate had quietly stopped working.
- A large `util/Reflect.java` and many reflection handles hid wrong SRG names and broke in ways that never reached the log.
- The project rule is pack-first: if a parent class is needed, compile against it. Reflection was used only to avoid a compile dependency.
- A few hot paths allocated per call, and a few maps never pruned.

## Batch 1: dead injectors and wrong handles

| Change | Intent | Where to look if it breaks |
| --- | --- | --- |
| Village `pushVillageWorld` / `popVillageWorld` retargeted to `func_186125_a`, per-thread stack for nested layout calls | The old targets did not exist, so the generator was never set for village wet/flatten tests | `MixinMapGenVillageWorld` (suspect for road-only waterside villages, AQ-011) |
| Seam pass restored from a deleted `MixinChunkProviderServer` to `SeamReinforcer`, called at RETURN of RTG `func_185932_a`, Overworld only | Connect Better Caves with the Depths caverns across bedrock on new chunks only | `depths/SeamReinforcer`, `MixinChunkGeneratorRTG` |
| BM mineshaft locate hook moved to `MapGenMineshaft` with an `instanceof` guard | `/locate Mineshaft` only ever found one | `MixinMapGenMineshaftLocate`, `BetterMineshaftLocate` |
| `StructureStart.updateBoundingBox` through an `@Invoker`; wrong SRG names fixed | Village save and BM box refresh silently did nothing | `mixin/vanilla/InvokerStructureStart` |
| `scripts/check_mixin_targets.py` added | Mixin only searches the target class, never superclasses, so a wrong `method=` is invisible without `defaultRequire`. The script checks every `method=` against MCP/SRG and parent jars | run before every build |
| `injectors.defaultRequire = 1` on the optional jsons | An injector that matches nothing now errors in `cleanmix.log` instead of vanishing | see Batch 4 for the required jsons |

## Batch 2: parent mods compiled against

Grappling Hook, Open Glider, Waystones and Thaumcraft warp now use direct calls behind cached `Loader.isModLoaded` flags (`GrappleHelper`, `OpenGliderHelper`, `WaystoneBridge`, `ThaumcraftHelper`, `StaminaFeathers`, `PerkAccess`, `SimpleDifficultyHelper`). Intent: drop about 200 lines of grapple reflection and make a changed parent signature a compile error instead of a runtime mystery. Where to look: the matching `libs/` jar and `compatibility-matrix.md`.

## Batch 3: `Reflect` removed

`util/Reflect.java` was deleted. Vanilla members are called directly; protected or private ones go through Mixin accessors in `mixin.vanilla` (`AccessorMapGenBase`, `AccessorMapGenStructure`, `AccessorMapGenVillage`, `AccessorStructureComponent`, `AccessorVillageStart`, `AccessorChunkProviderServer`, `InvokerStructureStart`, `AccessorChunk`) wrapped by `rtg/StructureAccess`. Key finding that changed the plan: `remap = false` on a mixin only affects annotation strings. Method bodies are still remapped, so direct vanilla calls inside them are safe, and `@Shadow` fields need `@Shadow(remap = false)` with the SRG name. Reflection that remains on purpose: `StructureAccess.getChunkGenerator` / `StructureVillageOverlap` (unknown third-party wrappers), `MixinASMHooksVillagePaste` (class-load order), `AnimaniaAddons` (class-presence probe), `RecipeStitchPoppet` (one private field; an accessor risks loading `InventoryCrafting` early in a required json).

## Batch 4: performance, robustness, hygiene

- Performance and leaks: server-tick queue instead of a thread per dimension warp; cached exposure grants, spawn ids, cozy blocks and weapon types (each cleared when its config reloads); cheap precondition before perk lookups; quality-wear trace off by default; `PerkDurability` mark expires after one tick and clears on logout; bounded or pruned per-player maps.
- Robustness: Simple Tomb slot map handed over through `TombSlotMapAccess` and stale tags cleared on respawn; AI task and Charm bridge failures log once.
- Hygiene: `injectors.defaultRequire = 1` on the four required jsons; `mcmod.info` version from `build.gradle`; build artifacts removed from the Git index.

## Lessons

- **`defaultRequire` on a required json turns a dead injector into a startup crash.** It happened once: `MixinStructureStartVillagePaste.aqtweaks$skipWetVillagePaste` scanned 0 targets because Charm's ASM rewrites that call into `ASMHooks.addComponentParts`, which `MixinASMHooksVillagePaste` handles. It is now `require = 0`. Mixin reports one failed injector per boot, so another could still surface. First move: read the newest crash report and `cleanmix.log`.
- Static method-name checks do not validate `@At` INVOKE/FIELD targets.
- Mixin classes should not carry static initializers; helper classes (`util/AiTaskReport`) hold the state.
- Reskillable logs "Unlockable not found" at boot for perk-to-perk requirements because it reads saved strings from `config/reskillable.cfg` while constructing each trait. Tweaks restamps the right requirements afterward. Cosmetic, left as is.

## Out of scope on purpose

AQ-011 (intermittent road-only or sparse waterside villages) and AQ-005 (third-party tab-complete NPE) were not pursued: both are intermittent. The prime suspect for AQ-011 is the Batch 1 world push/pop; the A/B test is to remove `MixinMapGenVillageWorld` from `mixins.aqtweaks.json`.

## If something regresses

1. Newest crash report and `logs/cleanmix.log` (mixin failures) in the instance.
2. `python scripts/check_mixin_targets.py`.
3. Compare the symptom to the "Where to look" columns above.
4. The working plan and issues tracker (run logs, per-batch checklists) are archived locally in `.cursor/plans/archive/` and are not published.
