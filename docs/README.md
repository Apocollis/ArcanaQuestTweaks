# Arcana Quest Tweaks — Architecture

This directory is the design and engineering spec for `aqtweaks` **1.6**. Read the index, then the module file for the system you are changing. Worldgen applies to **new chunks only**.

Mod: `aqtweaks`. Minecraft 1.12.2 / CleanroomMC / Forge. Stay on **1.6** unless asked to bump.

`aqtweaks` is a **tweak layer**. Parent mods still own their systems. Tweaks listens to Forge events, calls public APIs (`FeathersHelper`, Thaumcraft warp caps, Bewitchment `Ritual`), or mixins parent methods when events are not enough.

Almost all player/world/block access goes through `util/Reflect.java` (Cleanroom / modern Java). Do not replace that with raw MCP getters in new code unless the call site is already a mixin targeting a known class.

## Module docs

Each file covers: what Tweaks changes, how the **parent mod** implements the feature, how Tweaks hooks it, algorithms, live config, files/mixins, and do-not-regress rules.

| Module | Parent mod(s) | Doc |
| --- | --- | --- |
| Stamina | Elenai Dodge 2 Extended, Grappling Hook, Dynamic Sword Skills, Embers, Reskillable, Simple Difficulty, Spartan Weaponry | [stamina.md](stamina.md) |
| Grimoire of Gaia | Grimoire of Gaia (`gaia`) | [grimoire-of-gaia.md](grimoire-of-gaia.md) |
| Thaumcraft | Thaumcraft 6 | [thaumcraft.md](thaumcraft.md) |
| Bewitchment | Bewitchment + Thaumcraft | [bewitchment.md](bewitchment.md) |
| Comfort | Vanilla + optional Thaumcraft, Simple Difficulty, Biomes O' Plenty | [comfort.md](comfort.md) |
| Depths | Depths Update, YUNG's Better Caves, RTG, CoFH World, Recurrent Complex | [depths.md](depths.md) |
| RTG | Realistic Terrain Generation + vanilla `MapGenVillage` + Recurrent Complex + Astral / Bewitchment Cambion / Mystical World huts | [rtg.md](rtg.md) |
| Recipes | Forge `CraftingHelper` (Metallurgy / Spartan JSON) | [recipes.md](recipes.md) |

Astral surface shrines, Bewitchment Cambion houses, and Mystical World thatch huts are **not** separate modules. They are post-terrain structure settle/skip under [rtg.md](rtg.md). Ritual warp is [bewitchment.md](bewitchment.md). Cambion **worldgen** is RTG.

## How modules load

### `@Mod` vs real parents

`ArcanaQuestTweaks` declares:

`required-after:elenaidodge2;after:grimoireofgaia;after:thaumcraft;after:bewitchment;after:grapplemod;after:embers`

That is **not** the full parent list. Soft parents that Tweaks mixins or events against, without `after:` / `required-after:`:

| Parent | Used by | If missing |
| --- | --- | --- |
| RTG, Depths Update, Better Caves, CoFH World, Recurrent Complex | Depths + RTG mixins in **required** `mixins.aqtweaks.json` | Mixin apply can fail; this pack always ships them |
| Astral Sorcery | Optional mixin json + village shrine handler | Mixin config `required: false`; handler not registered |
| Mystical World | Optional mixin json | No hut skip/settle |
| Simple Difficulty, Biomes O' Plenty | Comfort potions / hot spring block | Those benefits no-op |
| Roguelike Dungeons Arcana | Thaumcraft dungeon warp via `isInsideStructure("RoguelikeDungeon")` | Dungeon exposure never matches |

### Init (`CommonProxy` / `ClientProxy`)

**preInit**

- Register SimpleNetworkWrapper messages 0–2 (stamina climb/grapple). See [stamina.md](stamina.md).
- `ComfortConfigLoader.load` from the Forge config directory.

**init (common)**

- Always: `StaminaModule`, `GrimoireOfGaiaModule`, `ComfortSystemHandler`.
- If `thaumcraft`: `ThaumcraftModule`.
- If `bewitchment`: `BewitchmentRegistryHandler` (ritual wrap still no-ops unless Thaumcraft is also loaded; see [bewitchment.md](bewitchment.md)).
- If `astralsorcery`: `VillageAstralSmallShrineHandler.register()` (structure piece id `AQTSmallShrine`).

**init (client)**

- `StaminaModuleClient`, `DepthsFogHandler`.

`postInit` is empty.

### MixinBooter late loader

`AQTweaksLateMixinLoader` always returns these configs (MixinBooter / Fugue). There is no early mixin json.

| File | `required` | Module | If parent jar missing |
| --- | --- | --- | --- |
| `mixins.aqtweaks.json` | **true** | Depths, RTG villages, Recipes | Load fails |
| `mixins.aqtweaks.grapple.json` | false | Stamina | Skip |
| `mixins.aqtweaks.dss.json` | false | Stamina | Skip |
| `mixins.aqtweaks.astral.json` | false | RTG post-terrain shrines | Skip |
| `mixins.aqtweaks.bewitchment.json` | false | RTG Cambion houses | Skip |
| `mixins.aqtweaks.mysticalworld.json` | false | RTG Mystical huts | Skip |

`mixins.aqtweaks.json` contents (package `com.apocollis.aqtweaks.mixin`):

- Client: `MixinRenderGlobal` (Depths hide sky)
- Common: `MixinChunkProviderServer`, `depthsupdate.MixinDepthsCaveNoiseGenerator`, `cofh.MixinDistributionUniform`, `reccomplex.MixinRayMatcher`, `reccomplex.MixinGenericVillageCreationHandler`, Better Caves / RTG village mixins listed in [depths.md](depths.md) and [rtg.md](rtg.md), `MixinStructureVillagePieces`, `MixinMapGenVillageInside/Spawn/Start/World`, `MixinCraftingHelperFindFiles`

Two mixins target `ChunkGeneratorRTG` in that required json, in this order:

1. `MixinChunkGeneratorRTG` — Depths Deepslate fill at **TAIL** of `generateTerrain` (Y -64..-1, not Y=0).
2. `MixinChunkGeneratorRTGVillage` — layout-first + flatten `landscape.noise` **before** `generateTerrain`.

Safe sequence: flatten noise → RTG carves 0–255 → fill sub-zero solid. Do not conflate the two mixins.

### Config files (`config/arcanaquesttweaks/`)

Forge `@Config` on nested classes in `ArcanaQuestTweaksConfig`. Comfort is JSON, not Forge cfg.

| File | Class |
| --- | --- |
| `aqtweaks_stamina.cfg` | `StaminaModuleConfig` |
| `aqtweaks_grimoireofgaia.cfg` | `GrimoireOfGaiaConfig` |
| `aqtweaks_thaumcraft.cfg` | `ThaumcraftConfig` |
| `aqtweaks_bewitchment.cfg` | `BewitchmentConfig` |
| `aqtweaks_depths.cfg` | `DepthsModuleConfig` |
| `aqtweaks_rtg.cfg` | `RtgModuleConfig` |
| `aqtweaks_comfort.json` | `ComfortConfigLoader` (not `@Config`) |

`ConfigEventHandler` runs `ConfigManager.sync` on any `aqtweaks` cfg change and invalidates DSS skill-cost cache. Existing instance files keep old values when Java defaults change.

### `util/Reflect.java`

Large cached reflection layer for entity/world/block/NBT/sound/primer access. Worldgen mixins and event handlers should keep using it so Cleanroom / Java 25 field/method differences stay in one place. Depths Deepslate/lava/air/bedrock block states are resolved here (`getDeepslateState`, etc.).

## Workflow (always)

1. Investigate read-only.
2. Write `implementation_plan.md`, also put the plan in chat.
3. Wait for explicit `proceed`.
4. Implement, then `.\build_gradle.ps1` (Java 25; copies the remapped jar to workspace `mods` and CurseForge Arcana Quest DEVBOX). Skip rebuild only if told not to.

Worldgen changes apply to **new chunks only**.
