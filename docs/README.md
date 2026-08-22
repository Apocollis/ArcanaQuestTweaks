# Arcana Quest Tweaks — Architecture

This directory is the design and engineering spec for `aqtweaks` **1.6**. Read the index, then the module file for the system you are changing. Worldgen applies to **new chunks only**.

**Ops (reproduce / ship):** [compatibility-matrix.md](compatibility-matrix.md) · [build-and-release.md](build-and-release.md) · [verification.md](verification.md)

Mod: `aqtweaks`. Minecraft 1.12.2 / CleanroomMC / Forge. Stay on **1.6** unless asked to bump.

`aqtweaks` is a **tweak layer**. Parent mods still own their systems. Tweaks listens to Forge events, calls public APIs (`FeathersHelper`, Thaumcraft warp caps, Bewitchment `Ritual`), or mixins parent methods when events are not enough. Vanilla calls inside `remap = false` mixins go through `Reflect` — see below.

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
| Village gen (pack pipeline) | Vanilla + RTG + Geographicraft + Recurrent Complex + Charm + Tweaks overlay | [villagegen_info.md](villagegen_info.md) |
| Recipes | Forge `CraftingHelper` (Metallurgy / Spartan JSON) | [recipes.md](recipes.md) |
| Compatibility / jars | Compile vs mixin vs runtime vs copy script | [compatibility-matrix.md](compatibility-matrix.md) |
| Build / deploy | `gradlew build` vs `build_gradle.ps1` | [build-and-release.md](build-and-release.md) |
| Release smoke | Boot, optional absences, worldgen, stamina | [verification.md](verification.md) |

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
| `mixins.aqtweaks.charm.json` | false | RTG Charm village paste skip | Skip |
| `mixins.aqtweaks.bewitchment.json` | false | RTG Cambion houses | Skip |
| `mixins.aqtweaks.mysticalworld.json` | false | RTG Mystical huts | Skip |

`mixins.aqtweaks.json` contents (package `com.apocollis.aqtweaks.mixin`):

- Client: `MixinRenderGlobal` (Depths hide sky)
- Common: `MixinChunkProviderServer`, `depthsupdate.MixinDepthsCaveNoiseGenerator`, `cofh.MixinDistributionUniform`, `reccomplex.MixinRayMatcher`, `reccomplex.MixinGenericVillageCreationHandler`, Better Caves / RTG village mixins listed in [depths.md](depths.md) and [rtg.md](rtg.md), `MixinStructureVillagePieces`, `MixinStructureStartVillagePaste`, `MixinMapGenVillageInside/Spawn/Start/World`, `MixinCraftingHelperFindFiles`. Charm paste: optional `mixins.aqtweaks.charm.json`.

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

Cached reflection for entity/world/block/NBT/sound/primer and soft-mod APIs (Elenai weight, Grapple, glider, thirst, Reskillable).

**Use Reflect** for vanilla member access inside **`remap = false` mixin bodies** (those strings are not remapped). Also use it for parent mods loaded only by reflection.

**Direct vanilla in Tweaks’ own classes is allowed.** Event handlers are remapped (`defaultRemapJar = true`). `DepthsFogHandler.entity.world` and `ThaumcraftModule` `getChunkProvider()` are not defects.

Do not add raw MCP names inside `remap = false` mixins.

## Adding an integration

When hooking a new parent (or a new mixin on an existing one):

1. **Classpath:** add the exact jar to `libs/` (and to `build_gradle.ps1` `$deps` if this machine should copy it). Update [compatibility-matrix.md](compatibility-matrix.md).
2. **`@Mod`:** `required-after` only if Tweaks must not load without it. Otherwise `after:` or omit.
3. **Mixin:** new json `required: false` unless the pack always ships the parent **and** missing it should crash. Register the json in `AQTweaksLateMixinLoader`. Mixin targets: SRG in vanilla, parent members as in that jar.
4. **Side:** client-only in the json `client` array or `@SideOnly`. Packets: `SimpleNetworkWrapper` side as today (stamina 0–2 are SERVER).
5. **Absent parent:** `Loader.isModLoaded` or `required: false`. Do not `import` parent types from always-loaded classes if the mod is optional (Bewitchment `Ritual` is compile-hard because the handler only registers when loaded — still keep that class off the bus).
6. **Config:** new `@Config` defaults; instance files **keep old keys**. Document live vs dead knobs in the module doc.
7. **Verify:** add a row to [verification.md](verification.md). Worldgen → new chunks. Mixin vanilla calls → Reflect or remap.

## Workflow (always)

1. Investigate read-only.
2. Write `implementation_plan.md`, also put the plan in chat.
3. Wait for explicit `proceed`.
4. Implement, then `.\build_gradle.ps1` unless told not to rebuild. Portable compile: `.\gradlew.bat build`. Details: [build-and-release.md](build-and-release.md).

Worldgen changes apply to **new chunks only**.
