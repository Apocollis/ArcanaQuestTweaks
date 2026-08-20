# Compatibility matrix (1.6)

Last updated: 2026-08-20.

This is the compile / mixin-apply / runtime contract. Module behavior lives in the per-module docs. Do not treat “required vs optional” as one bit.

Gradle compiles **every jar in `libs/`** (`fileTree`). There is **no hash pin** yet. `build_gradle.ps1` copies only a subset from DEVBOX and **skips missing files**. A green build can still be the wrong set. See [build-and-release.md](build-and-release.md).

## Role columns

| Role | Meaning |
| --- | --- |
| `@Mod` | `required-after` / `after` / omitted on `ArcanaQuestTweaks` |
| Mixin apply | Late json `required: true` → missing target **crashes load**. `required: false` → skip that json |
| Compile | Java `import` of parent types (or mixin class target). String `@Mixin(targets = "...")` does **not** need the jar to compile |
| Runtime | `Loader.isModLoaded`, potion/registry lookup, or class-name prefix. Missing → feature no-ops (unless mixin apply already crashed) |
| Copy | Listed in `build_gradle.ps1` `$deps` (copy **if** the file exists in DEVBOX `mods`) |

## Toolchain (not CurseForge jars)

| Piece | Pin in repo today |
| --- | --- |
| Game | Minecraft **1.12.2** |
| Mappings | MCP **stable_39** / 1.12 |
| Loader | Cleanroom **0.5.7-alpha** (`build.gradle` `cleanroom.loader`) |
| MixinBooter / Fugue | Pack: `mixinbooter-11.13.jar`, `Fugue-0.23.7.jar` (not in Tweaks `libs/` copy list) |
| Unimined | Gradle plugin **1.4.17-kappa** |
| Java | Toolchain **25**; deploy script sets `JAVA_HOME` to Zulu 25 |
| Remap | `defaultRemapJar = true` → ship `ArcanaQuestTweaks-1.6.jar`, not `-dev` |

## Parents

Jar names below are from the **Arcana Quest DEVBOX** instance on 2026-08-20 unless noted. Copy-list names that differ are called out.

| Modid | Jar (DEVBOX) | `@Mod` | Mixin apply | Compile | Runtime | Copy | Doc | Mixins / notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `elenaidodge2` | `ElenaiDodge2Extended-1.12.2-1.1.3.jar` | **required-after** | — | **yes** (`FeathersHelper`, HUD) | hard | **yes** | [stamina.md](stamina.md) | Compile **Extended 1.1.3**, not 1.1.0 |
| `grapplemod` | `grappling_hook_mod-1.12.2-v13.jar` | after | optional `mixins.aqtweaks.grapple.json` | no (string target) | yes | **no** | stamina | `MixinGrappleController` → `grappleController.updatePlayerPos` |
| `embers` | `embers-1.26.1.jar` | after | — | no (reflection) | yes | **no** | stamina | `EmberInventoryUtil` via `EmberMotorHelper` |
| `dynamicswordskills` | `1.12.2-DynamicSwordSkills-6.0.1.jar` (+ `SwordSkillsApi-1.1.0`) | omitted | optional `mixins.aqtweaks.dss.json` | no (string target) | yes | **no** | stamina | `MixinSkillActive` → `SkillActive.trigger`. Still compiles Elenai for spend |
| `openglider` | `OpenGlider-1.12.1-1.1.0.jar` | omitted | — | no (Reflect) | yes | **no** | stamina | Undeploy when empty |
| `reskillable` | `Reskillable-1.12.2-1.13.1.jar` | omitted | — | no (Reflect) | yes | **no** | stamina | Looks up `aqtweaks:armor_mastery` / `mining_efficiency` — **this jar does not register them** |
| `simpledifficulty` | `SimpleDifficulty-1.12.2-0.3.9.jar` | omitted | — | no | yes | **no** | stamina, comfort | Thirst; potion ids `heat_protection` / `cold_protection` / `cold_resist` |
| `grimoireofgaia` | `GrimoireOfGaia3-1.12.2-1.7.2.jar` | after | — | no | yes (`gaia.` prefix) | **no** | [grimoire-of-gaia.md](grimoire-of-gaia.md) | Events only |
| `thaumcraft` | `Thaumcraft-1.12.2-6.1.BETA26.jar` | after | — | no (`ThaumcraftHelper` reflection) | yes | **no** | [thaumcraft.md](thaumcraft.md) | Pack also has Fix / ResearchPatcher; Tweaks talks to TC API only |
| `bewitchment` | `bewitchment-1.12.2-0.0.22.65.jar` | after | optional `mixins.aqtweaks.bewitchment.json` | **yes** (`Ritual`, Cambion worldgen classes) | yes | **yes** | [bewitchment.md](bewitchment.md), [rtg.md](rtg.md) | Ritual wrap needs TC at register time. Cambion: `MixinWorldGenCambionHome` / `Medium` |
| `rtg` | `RTG-1.12.2-7.3.3.6.jar` | omitted | **required** json | **yes** (`ChunkGeneratorRTG`) | pack always | **yes** | [rtg.md](rtg.md), [depths.md](depths.md) | `MixinChunkGeneratorRTG` + `MixinChunkGeneratorRTGVillage` |
| `depthsupdate` | `depthsupdate-1.12.2-1.0.0-a12.jar` | omitted | **required** json | **yes** (`CaveNoiseGenerator` import) | pack always | **no** | depths | `MixinDepthsCaveNoiseGenerator`; `MixinCaveNoiseGenerator` string-targets the same class |
| `bettercaves` | `bettercaves-1.12.2-2.0.4.jar` | omitted | **required** json | **yes** (`FastNoise`, BC classes) | pack always | **yes** | depths | Depths-pass, carver utils, flatten bedrock, surface altitude |
| `cofhworld` | `CoFHWorld-1.12.2-1.4.0.1-universal.jar` | omitted | **required** json | **yes** | pack always | **yes** | depths | `MixinDistributionUniform` `Math.max` redirect |
| `reccomplex` | `RecurrentComplexVolts-1.12.2-2.0.0.9.jar` | omitted | **required** json | **yes** | pack always | **yes** | depths, rtg | `MixinRayMatcher` **@Overwrite**; `MixinGenericVillageCreationHandler`. Needs `IvToolkit-1.3.3-1.12.jar` (copied) |
| `astralsorcery` | `astralsorcery-1.12.2-1.10.27.jar` | omitted | optional `mixins.aqtweaks.astral.json` | **yes** if that json compiles against AS | skip json if absent | **yes** | rtg | Shrine skip/settle; village piece only if loaded |
| `mysticalworld` | `mysticalworld-1.12.2-1.11.0.jar` | omitted | optional `mixins.aqtweaks.mysticalworld.json` | **yes** (`StructureGenerator`) | skip json if absent | **yes** | rtg | Huts only, not barrows. Pack also has `mysticallib` |
| `roguelike` / Arcana | DEVBOX `RoguelikeDungeons-Arcana-2.5.3.jar` | omitted | — | no | `isInsideStructure("RoguelikeDungeon")` | script lists **`RoguelikeDungeons-Arcana-1.12.2-2.5.0.jar`** (name mismatch → copy often skips) | thaumcraft | No Tweaks mixin on that method |
| `biomesoplenty` | `BiomesOPlenty-1.12.2-7.0.1.2445-universal.jar` | omitted | — | no | yes | **no** | comfort, rtg | Hot spring block; kelp/coral biome names |
| Forge | (Cleanroom) | — | **required** json | yes | always | — | [recipes.md](recipes.md) | `MixinCraftingHelperFindFiles`. Metallurgy/Spartan jars are runtime recipe trees, not Tweaks compile deps |
| `waystones` | `Waystones_1.12.2-4.1.0.jar` | omitted | — | no | village piece class name | **no** | rtg | Relocate same gazebo; Tweaks does not mixin Waystones |

Vanilla `MapGenVillage` / `MapGenCaves` / `ChunkProviderServer` / `RenderGlobal` are Forge/vanilla, not extra jars.

## `build_gradle.ps1` copy list vs contract

**Copied if present:** Elenai Extended 1.1.3, Bewitchment, Roguelike **2.5.0 filename**, CoFH World, Better Caves, RC 2.0.0.9, IvToolkit, RTG 7.3.3.6, Astral 1.10.27, Mystical World 1.11.0. Also **deletes** stale `ElenaiDodge2-1.12.2-1.1.0.jar` and `RecurrentComplexVolts-1.12.2-2.0.0.7.jar` from `libs/`.

**Not copied (but needed to compile and/or mixin-apply):** Depths Update **a12**, Thaumcraft, Gaia, Grapple, Embers, DSS, Simple Difficulty, BOP. If they are already in `libs/` from an older copy, Gradle still uses them — unversioned.

**DEVBOX vs copy filename:** Roguelike on disk is `RoguelikeDungeons-Arcana-2.5.3.jar`; the script looks for `...-1.12.2-2.5.0.jar`.

## Upgrade check (every parent bump)

1. Diff mixin targets (SRG names, `@Overwrite` body — especially `RayMatcher.cast`).
2. Re-run [verification.md](verification.md) rows for that module.
3. Worldgen: **new chunks only**.
4. Forge cfg files in the instance **keep old values**; Java default changes do not migrate.

Hash pinning of `libs/` is a follow-up (`proceed and pin`), not this doc pass.
