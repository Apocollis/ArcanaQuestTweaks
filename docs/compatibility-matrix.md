# Compatibility matrix (1.8)

Last updated: 2026-09-20.

This is the compile / mixin-apply / runtime contract. Module behavior lives in the per-module docs. Do not treat “required vs optional” as one bit.

Gradle compiles **every jar in `libs/`** (`fileTree`). Those jars are **gitignored** (`lib/`, `libs/`, `*.jar` except `gradle/wrapper/gradle-wrapper.jar`). There is **no hash pin** yet. `build_gradle.ps1` copies only a subset from DEVBOX and **skips missing files**. A green build can still be the wrong set. See [build-and-release.md](build-and-release.md).

`libs/` must hold **only** jars listed in the Parents table below. An unlisted jar silently joins the compile classpath and lets code import a parent this contract never promised. `build_gradle.ps1` deletes the stale names it knows about on every run; anything else that turns up in `libs/` has to be removed by hand.

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
| Java | Toolchain **25**; `--release 21` class files; deploy script sets `JAVA_HOME` to Zulu 25 |
| Remap | `defaultRemapJar = true` → ship `ArcanaQuestTweaks-1.8.jar`, not `-dev` |

## Parents

Jar names below are from the **Arcana Quest DEVBOX** instance on 2026-08-20 unless noted. Copy-list names that differ are called out.

| Modid | Jar (DEVBOX) | `@Mod` | Mixin apply | Compile | Runtime | Copy | Doc | Mixins / notes |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `elenaidodge2` | `ElenaiDodge2Extended-1.12.2-1.1.3.jar` | **required-after** | — | **yes** (`FeathersHelper`, HUD) | hard | **yes** | [stamina.md](stamina.md) | Compile **Extended 1.1.3**, not 1.1.0 |
| `toughnessbar` | `toughnessbar-2.4.jar` | omitted | optional `mixins.aqtweaks.toughnessbar.json` | no (string target) | client HUD | **no** | [client.md](client.md) | `MixinEventHandlerClient` → `EventHandlerClient.onRenderArmorToughnessEvent`. Armor column, left-to-right |
| `grapplemod` | `grappling_hook_mod-1.12.2-v13.jar` | after | optional `mixins.aqtweaks.grapple.json` | no (string target) | yes | **no** | stamina | `MixinGrappleController` → `grappleController.updatePlayerPos` |
| `embers` | `embers-1.26.1.jar` | after | optional `mixins.aqtweaks.qualitytools.json` (Dawnstone `onHit`) | **yes** (`DawnstoneAnvilRecipe`, `RecipeRegistry`, `TileEntityDawnstoneAnvil`) | yes | **yes** | stamina, [qualitytools.md](qualitytools.md) | Stamina still uses `EmberInventoryUtil` via `EmberMotorHelper` Reflect. Quality rune recipes compile-hard the anvil API. Mixin json is shared with QT and skipped if that json is not applied |
| `dynamicswordskills` | `1.12.2-DynamicSwordSkills-6.0.1.jar` (+ `SwordSkillsApi-1.1.0`) | omitted | optional `mixins.aqtweaks.dss.json` | **yes** (`EntitySwordBeam`, `SkillActive` still also string-targeted) | yes | **yes** | stamina, [reskillable.md](reskillable.md) | `MixinSkillActive` → `SkillActive.trigger`. `MixinEntitySwordBeam` stamps magic + unblockable on impact. Compiles Elenai for spend |
| `openglider` | `OpenGlider-1.12.1-1.1.0.jar` | omitted | — | no (Reflect) | yes | **no** | stamina | Undeploy when empty |
| `reskillable` | `Reskillable-1.12.2-1.13.1.jar` | after | — | **yes** (`reskillable` package only) | yes | **yes** | [reskillable.md](reskillable.md), stamina | Per-level bonuses compile-hard. Registers Tweaks traits except `armor_mastery` / `mining_efficiency`. Stamina effects use Reflect for perk **ids** |
| `effortlessbuilding` | `effortlessbuilding-1.12.2-2.16.jar` | after | optional `mixins.aqtweaks.effortlessbuilding.json` | **yes** (`ReachHelper` mixin) | yes | **yes** | [reskillable.md](reskillable.md) | `MixinReachHelper` → `getPlacementReach` + `getMaxBlocksPlacedAtOnce` RETURN. Not `getMaxReach` |
| `simpledifficulty` | `SimpleDifficulty-1.12.2-0.3.9.jar` | omitted | optional `mixins.aqtweaks.simpledifficulty.json` | **yes** (`ThirstUtilInternal`, `ItemCanteen`, `SDItems`) | yes | **yes** | stamina, comfort, [reskillable.md](reskillable.md) | Water Collector: dirty-drink skip, canteen purify, glass-bottle fill. Thirst potions still runtime ids for Comfort |
| `grimoireofgaia` | `GrimoireOfGaia3-1.12.2-1.7.2.jar` | after | optional `mixins.aqtweaks.gaia.json` | **yes** (Gaia entity/projectile classes) | yes | **yes** | [grimoire-of-gaia.md](grimoire-of-gaia.md) | Melee/assist skip instant damage; MAGIC bolts → `causeIndirectMagicDamage`; bomb explosion; skip archer tip. Vanilla INVOKEs MCP + `remap = true` |
| `thaumcraft` | `Thaumcraft-1.12.2-6.1.BETA26.jar` | after | optional `mixins.aqtweaks.thaumcraft.json` | **yes** (focus effect classes, `ItemCaster`, `AuraHandler`) | yes | **yes** | [thaumcraft.md](thaumcraft.md) | Warp still `ThaumcraftHelper` reflection. `MixinFocusEffectExecute` stamps `setMagicDamage` on Fire/Frost/Air/Earth/Flux/Curse/Heal `attackEntityFrom`. `MixinFocusEffectHeal` scales `heal`. `MixinItemCaster.consumeVis` Full Font. Vanilla INVOKE MCP + `remap = true`. Pack also has Fix / ResearchPatcher |
| `rustic` | `rustic-1.2.0.jar` | omitted | optional `mixins.aqtweaks.rustic.json` | **yes** (`FluidBooze`) | yes | **yes** | [reskillable.md](reskillable.md) | Iron Gut cancels `inebriate` (tipsy). Not Rustichromia |
| `bewitchment` | `bewitchment-1.12.2-0.0.22.65.jar` | after | optional `mixins.aqtweaks.bewitchment.json` | **yes** (`Ritual`, `SpinningWheelRecipe`, Cambion worldgen classes) | yes | **yes** | [bewitchment.md](bewitchment.md), [rtg.md](rtg.md) | Ritual wrap needs TC at register time. Cambion: `MixinWorldGenCambionHome` / `Medium`. CT Spinning Wheel Zen class compile-hards this jar |
| `crafttweaker` | `CraftTweaker2-1.12-4.1.20.715.jar` | omitted | — | **yes** (`@ZenRegister`, `CraftTweakerMC`, `IAction`) | CT annotation scan + `@ModOnly("bewitchment")` | **yes** | [bewitchment.md](bewitchment.md) | `mods.bewitchment.SpinningWheel`. No Tweaks mixin. Not `required-after`. Do not import the Zen class from always-on bus handlers |
| `rtg` | `RTG-1.12.2-7.3.3.6.jar` | omitted | **required** json | **yes** (`ChunkGeneratorRTG`) | pack always | **yes** | [rtg.md](rtg.md), [depths.md](depths.md) | `MixinChunkGeneratorRTG` + `MixinChunkGeneratorRTGVillage` |
| `depthsupdate` | `depthsupdate-1.12.2-1.0.0-a12.jar` | omitted | **required** json | **yes** (`CaveNoiseGenerator` import) | pack always | **no** | depths | `MixinDepthsCaveNoiseGenerator`; `MixinCaveNoiseGenerator` string-targets the same class |
| `bettercaves` | `bettercaves-1.12.2-2.0.4.jar` | omitted | **required** json | **yes** (`FastNoise`, BC classes) | pack always | **yes** | depths | Depths-pass, carver utils, flatten bedrock, surface altitude |
| `cofhworld` | `CoFHWorld-1.12.2-1.4.0.1-universal.jar` | omitted | **required** json | **yes** | pack always | **yes** | depths | `MixinDistributionUniform` `Math.max` redirect |
| `reccomplex` | `RecurrentComplexVolts-1.12.2-2.0.0.9.jar` | omitted | **required** json | **yes** | pack always | **yes** | depths, rtg | `MixinRayMatcher` **@Overwrite**; `MixinGenericVillageCreationHandler`. Needs `IvToolkit-1.3.3-1.12.jar` (copied) |
| `astralsorcery` | `astralsorcery-1.12.2-1.10.27.jar` | omitted | optional `mixins.aqtweaks.astral.json` | **yes** if that json compiles against AS | skip json if absent | **yes** | rtg | Shrine skip/settle; village piece only if loaded |
| `mysticalworld` | `mysticalworld-1.12.2-1.11.0.jar` | omitted | optional `mixins.aqtweaks.mysticalworld.json` | **yes** (`StructureGenerator`) | skip json if absent | **yes** | rtg | Huts only, not barrows. Pack also has `mysticallib` |
| `roguelike` / Arcana | DEVBOX `RoguelikeDungeons-Arcana-2.5.3.jar` | omitted | — | no | `isInsideStructure("RoguelikeDungeon")` | **no** | thaumcraft | No Tweaks mixin on that method. Compile = no, so the jar must **not** sit in `libs/`; the script deletes `RoguelikeDungeons-Arcana-1.12.2-2.5.0.jar` there. That name never matched the DEVBOX file (`2.5.3`) anyway |
| `biomesoplenty` | `BiomesOPlenty-1.12.2-7.0.1.2445-universal.jar` | omitted | optional `mixins.aqtweaks.biomesoplenty.json` | no (string target) | yes | **no** | comfort, rtg | Hot spring block; kelp/coral biome names; `MixinGeneratorLakes` skips village water/quicksand |
| `charm` | `Charm-1.12.2-1.4.1.jar` | **required-after** | **required** `mixins.aqtweaks.charm.json` (jar `MixinConfigs`) | **yes** (`ASMHooks`) | hard | **yes** | rtg | `MixinASMHooksVillagePaste` → `svenhjol.charm.base.ASMHooks`. Return `true` to skip wet paste without dropping the piece. Curse / pack prerequisite. |
| Forge | (Cleanroom) | — | **required** json | yes | always | — | [recipes.md](recipes.md) | `MixinCraftingHelperFindFiles`. Metallurgy/Spartan jars are runtime recipe trees, not Tweaks compile deps |
| InControl | `incontrol-1.12-3.10.4.jar` | **after** | optional `mixins.aqtweaks.incontrol.json` + vanilla spawner in **required** json | **yes** (`GeneralConfiguration`, nested `StructureCache`) | pack always | **yes** | [spawning.md](spawning.md) | Layer filter `PotentialSpawns` LOWEST then last-per-class. Pack fill: `MixinWorldEntitySpawner` only (not `WorldServer`). `MixinStructureCache` expands feature/child `BB` chunks. Compile-hard `mcjty.tools.cache.StructureCache` via extracted `mcjtytools-1.12-0.0.21.jar` (nested in InControl, not a Curse parent). Do not import `PotentialSpawnRule` (`RuleBase` not in `libs/`). InControl’s own player-distance mixin stays |
| `waystones` | `Waystones_1.12.2-4.1.0.jar` | omitted | — | no | village piece class name | **no** | rtg | Relocate same gazebo; Tweaks does not mixin Waystones |
| `animania` | `animania-1.12.2-base-2.0.3.28.jar` + Farm `animania-1.12.2-farm-1.0.2.28.jar` | omitted | optional `mixins.aqtweaks.animania.json` | **yes** (`AddonHandler`, Farm animal bases for Rancher) | yes | **yes** | [advancement.md](advancement.md), [reskillable.md](reskillable.md) | `MixinAddonHandler` cancels `onWorldLoad`. `MixinGenericAIMate` + `AnimaniaModule` Rancher clocks. Farm is a non-mod addon; Extra not compile |
| `somnia` | `Somnia-1.0.1.jar` | omitted | optional `mixins.aqtweaks.somnia.json` | **yes** (`SomniaUtil`) | yes | **yes** | [somnia.md](somnia.md) | `MixinSomniaUtil` `@Overwrite` `chunkLightCheck`; `SomniaOptimizationHandler` throttles ambient to 20t & fixes duplicate calls |
| `bettermineshafts` | `BetterMineshaftsForge-1.12.2-2.2.1.jar` | omitted | optional `mixins.aqtweaks.bettermineshafts.json` | **yes** (`MapGenBetterMineshaft`, `VerticalEntrance`, `MineshaftVariantSettings`) | skip json if absent | **yes** | [bettermineshafts.md](bettermineshafts.md) | Locate pin; stub failed openings; Tweaks rate/spacing/Y (local settings copy) |
| `stats_keeper` | `StatsKeeper-1.12.2-3.1.13.jar` | after | — | **yes** (`IHealth`, `SKCapabilities`, `SKHealthConfig`) | yes | **yes** | [statskeeper.md](statskeeper.md) | Bus handler cancels `contenttweaker:life_elixir` drink at SK cap. No mixin. ContentTweaker is registry-name only |
| `randomportals` | `randomportals-cleanroom0.1.0.jar` | after | optional `mixins.aqtweaks.randomportals.json` | **yes** (`NetherPortalEvent`, `RPOTeleporter`, `RPOConfig`) | yes | **yes** | [twilightforest.md](twilightforest.md) | `MixinRPOTeleporter` grass pad on `isValidPortalPosition` + `findTopLeft`. Do not shadow `Teleporter.world`. Handler also needs TF |
| `twilightforest` | `twilightforest-1.12.2-3.15.1.jar` | after | — | **yes** (`TFTeleporter`, `TFWorld`) | yes | **yes** | [twilightforest.md](twilightforest.md) | Bus handler only if RP is also loaded. No TF mixin |
| `quark` | `Quark-r1.6-179.jar` | omitted | — | **yes** (`BlockSpeleothem`, `Speleothems`) | yes | **yes** | [depths.md](depths.md) | Primer speleothems in the lower cavern. No Quark mixin. Handler class is only invoked after `Loader.isModLoaded("quark")` |
| `autoreglib` | `AutoRegLib-1.3-32.jar` | omitted | — | **yes** (Quark `BlockMod` super) | Quark runtime | **yes** | [depths.md](depths.md) | Compile-only for Quark `BlockSpeleothem`. Tweaks does not import ARL types |
| `chisel` | `Chisel-MC1.12.2-1.0.2.45.jar` | after | optional `mixins.aqtweaks.chisel.json` | **yes** (`SlotChiselSelection`, `ItemChisel`, `ICarvingVariation`) | skip json if absent | **yes** | [gamestages.md](gamestages.md) | `MixinSlotChiselSelection` HEAD on `craft`; `MixinItemChisel` RETURN on `canChisel`. Not Chisels and Bits |
| `gamestages` | `GameStages-1.12.2-2.0.123.jar` | after | — | **yes** (`GameStageHelper`) | yes | **yes** | [gamestages.md](gamestages.md) | Compile-hard on the Chisel hook nested lookup only. No Game Stages mixin |
| `recipestages` | `recipestages-2.0.1.jar` | after | optional `mixins.aqtweaks.recipestages.json` | **yes** (`Recipes.setRecipeStage`, `Recipes.recipes`) | skip json if absent | **yes** | [gamestages.md](gamestages.md) | Capture CT `IIngredient` pairs. Needs CraftTweaker already on the classpath |
| `qualitytools` | `QualityTools-1.0.7_for_1.12.2.jar` | after | optional `mixins.aqtweaks.qualitytools.json` | **yes** (`QualityToolsHelper`, `QualityType`, `CommonEventHandler`, `TileEntityReforgingStation`) | skip json if absent; vanilla loot/durability mixins no-op | **yes** | [qualitytools.md](qualitytools.md) | Living-update stamp skip; reforge QualityBase. Vanilla `MixinTileEntityLockableLoot` + `MixinItemStackQualityDurability` are in the **required** json and gate on `Loader.isModLoaded`. Crafting Runes are registry-name only — do not put `craftingrunes-1.1.jar` in `libs/` |

Vanilla `MapGenVillage` / `MapGenCaves` / `WorldGenLakes` / `ChunkProviderServer` / `RenderGlobal` / `WorldEntitySpawner` / `MobSpawnerBaseLogic` are Forge/vanilla, not extra jars. `MixinWorldGenLakes` skips water (not lava) on the village pad. `MixinWorldEntitySpawner` fills pack sizes (late json). `MixinMobSpawnerBaseLogic` applies cage fail-recheck delay (early json) — do not late-mixin `MobSpawnerBaseLogic`, `World`, or `WorldServer`.

## `build_gradle.ps1` copy list vs contract

**Copied if present:** Elenai Extended 1.1.3, Bewitchment, CraftTweaker **1.12-4.1.20.715**, CoFH World, Better Caves, RC 2.0.0.9, IvToolkit, RTG 7.3.3.6, Astral 1.10.27, Mystical World 1.11.0, Grimoire of Gaia 1.7.2, Reskillable 1.13.1, Effortless Building 2.16, Thaumcraft 6.1 BETA26, InControl **1.12-3.10.4**, Animania Base **2.0.3.28**, Animania Farm **1.0.2.28**, Somnia **1.0.1**, Better Mineshafts **1.12.2-2.2.1**, Stats Keeper **1.12.2-3.1.13**, RandomPortals **cleanroom0.1.0**, Dynamic Sword Skills **6.0.1** + SwordSkillsApi **1.1.0**, Simple Difficulty **0.3.9**, Rustic **1.2.0**, Twilight Forest **1.12.2-3.15.1**, Quark **r1.6-179**, AutoRegLib **1.3-32**, Charm **1.12.2-1.4.1**, Chisel **MC1.12.2-1.0.2.45**, Game Stages **1.12.2-2.0.123**, Recipe Stages **2.0.1**, Quality Tools **1.0.7_for_1.12.2**, Embers **1.26.1**. After InControl copy, the script **extracts** nested `META-INF/libraries/mcjtytools-1.12-0.0.21.jar` to `libs/` for compile-hard `StructureCache`. Also **deletes** from `libs/`: `ElenaiDodge2-1.12.2-1.1.0.jar`, `RecurrentComplexVolts-1.12.2-2.0.0.7.jar`, `RoguelikeDungeons-Arcana-1.12.2-2.5.0.jar`, `BaublesEX-1.12.2-2.3.5.jar`, `WearableBackpacks-RLCraft-1.12.2-3.2.7.jar`.

**Not copied (but needed to compile and/or mixin-apply):** Depths Update **a12**, Grapple, BOP.

**DEVBOX vs copy filename:** Roguelike on disk is `RoguelikeDungeons-Arcana-2.5.3.jar`, while the script's name is `...-1.12.2-2.5.0.jar`. That mismatch used to make the copy silently skip; the same name is now on the **delete** list instead, so a `2.5.3` jar dropped into `libs/` by hand still has to be pulled out by hand.

## Upgrade check (every parent bump)

1. Diff mixin targets (SRG names, `@Overwrite` body — especially `RayMatcher.cast`).
2. Re-run [verification.md](verification.md) rows for that module.
3. Worldgen: **new chunks only**.
4. Forge cfg files in the instance **keep old values**; Java default changes do not migrate.

Hash pinning of `libs/` is a follow-up (`proceed and pin`), not this doc pass.
