# Arcana Quest Tweaks — Architecture

This directory is the design and engineering spec for `aqtweaks` **1.8**. Read the index, then the module file for the system you are changing. Worldgen applies to **new chunks only**.

**Ops (reproduce / ship):** [compatibility-matrix.md](compatibility-matrix.md) · [build-and-release.md](build-and-release.md) · [verification.md](verification.md)

Mod: `aqtweaks`. Minecraft 1.12.2 / CleanroomMC / Forge. Stay on **1.8** unless asked to bump.

**Always compile as Java 21.** Gradle toolchain may be JDK 25, but `JavaCompile` must keep `options.release = 21` (class major 65). Do not emit Java 22+ bytecode. Mixin/Fugue will refuse class version 66+. Details: [build-and-release.md](build-and-release.md).

`aqtweaks` is a **tweak layer** for the **Arcana Quest pack**, not a standalone optional-mod product. The pack is expected to ship required parents (DEVBOX + `libs/`). If you need a parent class or method, **compile-hard** (`import` + jar on the classpath). Do not add `Reflect` wrappers just to avoid a compile dependency. Mixin json `required: false` only skips that json at **load** if the jar is missing; it does not forbid compiling against it.

Parent mods still own their systems. Tweaks listens to Forge events, calls public APIs (`FeathersHelper`, Thaumcraft warp caps, Bewitchment `Ritual`), or mixins parent methods when events are not enough. Vanilla calls inside `remap = false` mixins go through `Reflect` — see below.

## Module docs

Each file covers: what Tweaks changes, how the **parent mod** implements the feature, how Tweaks hooks it, algorithms, live config, files/mixins, and do-not-regress rules.

| Module | Parent mod(s) | Doc |
| --- | --- | --- |
| Stamina | Elenai Dodge 2 Extended, Grappling Hook, Dynamic Sword Skills, Embers, Reskillable, Simple Difficulty, Spartan Weaponry | [stamina.md](stamina.md) |
| Reskillable | Reskillable + optional Effortless Building | [reskillable.md](reskillable.md) |
| Grimoire of Gaia | Grimoire of Gaia (`gaia`) | [grimoire-of-gaia.md](grimoire-of-gaia.md) |
| Thaumcraft | Thaumcraft 6 | [thaumcraft.md](thaumcraft.md) |
| Bewitchment | Bewitchment + Thaumcraft | [bewitchment.md](bewitchment.md) |
| Comfort | Vanilla + optional Thaumcraft, Simple Difficulty, Biomes O' Plenty | [comfort.md](comfort.md) |
| Portal | Tweaks-owned (no parent) | [portal.md](portal.md) |
| Depths | Depths Update, YUNG's Better Caves, RTG, CoFH World, Recurrent Complex | [depths.md](depths.md) |
| RTG | Realistic Terrain Generation + vanilla `MapGenVillage` + Recurrent Complex + Astral / Bewitchment Cambion / Mystical World huts | [rtg.md](rtg.md) |
| Village gen (pack pipeline) | Vanilla + RTG + Geographicraft + Recurrent Complex + Charm + Tweaks overlay | [villagegen_info.md](villagegen_info.md) |
| Client | Toughness Bar (optional), tooltip lines for non-Metallurgy tools | [client.md](client.md) |
| Spawning | Vanilla `PotentialSpawns` + pack `mob_overworldspawntype.json` (InControl still owns deny) | [spawning.md](spawning.md) |
| Recipes | Forge `CraftingHelper` (Metallurgy / Spartan JSON) | [recipes.md](recipes.md) |
| Advancement | Animania Base (Farm / Extra addon JSON via Base handler) | [advancement.md](advancement.md) |
| Somnia | Somnia Refreshed | [somnia.md](somnia.md) |
| Stats Keeper | Stats Keeper (`stats_keeper`) | [statskeeper.md](statskeeper.md) |
| Better Mineshafts | YUNG’s Better Mineshafts + RTG locate | [bettermineshafts.md](bettermineshafts.md) |
| Compatibility / jars | Compile vs mixin vs runtime vs copy script | [compatibility-matrix.md](compatibility-matrix.md) |
| Build / deploy | `gradlew build` vs `build_gradle.ps1` | [build-and-release.md](build-and-release.md) |
| Release smoke | Boot, optional absences, worldgen, stamina | [verification.md](verification.md) |

Astral surface shrines, Bewitchment Cambion houses, and Mystical World thatch huts are **not** separate modules. They are post-terrain structure settle/skip under [rtg.md](rtg.md). Ritual warp is [bewitchment.md](bewitchment.md). Cambion **worldgen** is RTG.

## How modules load

### `@Mod` vs real parents

`ArcanaQuestTweaks` declares:

`required-after:elenaidodge2;after:incontrol;after:grimoireofgaia;after:thaumcraft;after:bewitchment;after:grapplemod;after:embers;after:reskillable;after:effortlessbuilding;after:stats_keeper`

That is **not** the full parent list. Soft parents that Tweaks mixins or events against, without `after:` / `required-after:`:

| Parent | Used by | If missing |
| --- | --- | --- |
| RTG, Depths Update, Better Caves, CoFH World, Recurrent Complex | Depths + RTG mixins in **required** `mixins.aqtweaks.json` | Mixin apply can fail; this pack always ships them |
| Astral Sorcery | Optional mixin json + village shrine handler | Mixin config `required: false`; handler not registered |
| Mystical World | Optional mixin json | No hut skip/settle |
| Simple Difficulty, Biomes O' Plenty | Comfort potions / hot spring block; BOP optional mixin skips village water/quicksand lakes | Those benefits no-op; BOP lake mixin json skipped; vanilla water-lake skip still runs |
| Roguelike Dungeons Arcana | Thaumcraft dungeon warp via `isInsideStructure("RoguelikeDungeon")` | Dungeon exposure never matches |
| Reskillable | Per-level bonuses + stamina perk id lookup | Module not registered; stamina `hasUnlockable` no-ops |
| Effortless Building | Building skill place-reach / max blocks | Mixin json skipped; Building drip unused |
| InControl | Spawning layer filter + pack fill + structure BB cache (compile-hard min-distance and `StructureCache`) | Missing jar fails compile; pack always ships it |
| Animania | Advancement: skip Base world-load reload + Farm/Extra inject | Mixin json skipped; Animania advancements load as stock |
| YUNG’s Better Mineshafts | Locate pin + failed-entrance stub | Mixin json skipped; stock BM Y=64 locate |
| Stats Keeper | Elixir of Vitality drink cancel at SK cap | Handler not registered; vanilla + SK consume/refuse as stock |

### Init (`CommonProxy` / `ClientProxy`)

**preInit**

- Register SimpleNetworkWrapper messages 0–2 (stamina climb/grapple). See [stamina.md](stamina.md).
- `ArcanaQuestTweaksConfig.normalizePinned()` — forces `DepthsModuleConfig.general.minWorldY` to `DEPTHS_FLOOR_Y` (−64) even if an instance cfg was hand-edited.
- `ComfortConfigLoader.load`, `GaiaDamageConfig.load`, `SpawnTypeLists.load`, `SpawnStructureLists.load`, `SpawnParties.load`, and `SpawnGroupCounts.load` from the Forge config directory.
- `PortalModule.preInit` (`ForgeChunkManager` callback). Item/entity register via `RegistryEvent` (not init).
- `MapGenStructureIO.registerStructureComponent(VillagePieceVillagePlate.class, "AQTVillagePlate")` — **unconditional**. The Astral `AQTSmallShrine` piece is the conditional one (init, only if `astralsorcery`).
- If `reskillable`: `ReskillablePerkRegistry` (Unlockable `RegistryEvent`).

**init (common)**

- Always: `StaminaModule`, `GaiaDamageHandler` (Gaia JSON bases), `ComfortSystemHandler`, `VillageLandHelper.Events`.
- If `thaumcraft`: `ThaumcraftModule`.
- If `bewitchment`: `BewitchmentRegistryHandler` (ritual wrap still no-ops unless Thaumcraft is also loaded; see [bewitchment.md](bewitchment.md)).
- If `astralsorcery`: `VillageAstralSmallShrineHandler.register()` (structure piece id `AQTSmallShrine`).
- If `reskillable`: `ReskillableModule`.
- If `somnia`: `SomniaSleepHandler.init()`.
- If `stats_keeper`: `LifeElixirCapHandler` (elixir drink cancel at SK max health; level-up sound on a successful drink).

**init (client)**

- `StaminaModuleClient`, `DepthsFogHandler`, `ClientModule`.
- Entity renderer for `EntityArcaneRift` in **client preInit**. If `grimoireofgaia` and Deep Dwarf enabled: `RenderDeepDwarf`. Item models on `ModelRegistryEvent`.

`postInit` registers `SpawnLayerFilter` (after InControl `PotentialSpawns`), enables structure cave exemption if `incontrol` is loaded, and runs `Reflect.auditUnresolved()`. `ArcanaQuestTweaks.serverStarting` (`@Mod.EventHandler` on `FMLServerStartingEvent`) registers `/aqvillage` (`CommandAqVillage`, [rtg.md](rtg.md)) and `/aqcomfort` (`CommandAqComfort`, [comfort.md](comfort.md)).

### MixinBooter: early vs late

Vanilla `World` and `MobSpawnerBaseLogic` are already loaded when late mixins prepare. Portal glowstone light (`MixinWorldRiftLight` on `World.getRawLight`) and cage fail delay (`MixinMobSpawnerBaseLogic` on `updateSpawner`) are in **`mixins.aqtweaks.early.json`** (`required: true`). MixinBooter 11 reads that name from the jar manifest attribute `MixinConfigs` (set in `build.gradle`). Do not register this json from `AQTweaksLateMixinLoader`. Missing it fails load.

### MixinBooter late loader

`AQTweaksLateMixinLoader` always returns these configs (MixinBooter / Fugue). **`mixins.aqtweaks.charm.json` is first** so Charm `ASMHooks` is mixed before `mixins.aqtweaks.json` prepares `StructureStart` (Charm ASM would otherwise define `ASMHooks` too early). Do not put the Charm json on jar `MixinConfigs`.

| File | `required` | Module | If parent jar missing |
| --- | --- | --- | --- |
| `mixins.aqtweaks.charm.json` | false | RTG Charm village paste skip (listed **first**) | Skip |
| `mixins.aqtweaks.json` | **true** | Depths, RTG villages, Recipes | Load fails |
| `mixins.aqtweaks.grapple.json` | false | Stamina | Skip |
| `mixins.aqtweaks.dss.json` | false | Stamina | Skip |
| `mixins.aqtweaks.toughnessbar.json` | false | Client HUD | Skip |
| `mixins.aqtweaks.astral.json` | false | RTG post-terrain shrines | Skip |
| `mixins.aqtweaks.bewitchment.json` | false | RTG Cambion + circle/menhir/wickerman | Skip |
| `mixins.aqtweaks.mysticalworld.json` | false | RTG Mystical huts | Skip |
| `mixins.aqtweaks.biomesoplenty.json` | false | RTG BOP water/quicksand village skip | Skip |
| `mixins.aqtweaks.gaia.json` | false | Grimoire of Gaia drop pierce + recast bolts/bombs | Skip |
| `mixins.aqtweaks.effortlessbuilding.json` | false | Reskillable Building EB place reach + max blocks | Skip |
| `mixins.aqtweaks.thaumcraft.json` | false | Thaumcraft focus HP magic flag + Heal scale | Skip |
| `mixins.aqtweaks.animania.json` | false | Advancement: cancel Animania `onWorldLoad` | Skip |
| `mixins.aqtweaks.somnia.json` | false | Somnia: chunk light fix, 3-tier SMP sleep (Case A/B/C), Case B 2x time, fatigue tuning & chat notifications | Skip |
| `mixins.aqtweaks.incontrol.json` | false | Spawning: `StructureCache.parseStructureData` BB chunk expand | Skip |
| `mixins.aqtweaks.bettermineshafts.json` | false | Better Mineshafts locate pin + entrance stub | Skip |

`mixins.aqtweaks.json` contents (package `com.apocollis.aqtweaks.mixin`):

- Client: `MixinRenderGlobal` (Depths hide sky)
- Common: `MixinChunkProviderServer`, `depthsupdate.MixinDepthsCaveNoiseGenerator`, `cofh.MixinDistributionUniform`, `reccomplex.MixinRayMatcher`, `reccomplex.MixinGenericVillageCreationHandler`, Better Caves / RTG village mixins listed in [depths.md](depths.md) and [rtg.md](rtg.md), `MixinStructureVillagePieces`, `MixinStructureStartVillagePaste`, `MixinWorldGenLakes`, `MixinMapGenVillageInside/Spawn/Start/World`, `MixinCraftingHelperFindFiles`, `MixinWorldEntitySpawner`. Charm paste: optional `mixins.aqtweaks.charm.json`. Portal `MixinWorldRiftLight` and cage `MixinMobSpawnerBaseLogic` are in `mixins.aqtweaks.early.json`. InControl `MixinStructureCache` is in `mixins.aqtweaks.incontrol.json`. Better Mineshafts locate mixins are in `mixins.aqtweaks.bettermineshafts.json`.

Two mixins target `ChunkGeneratorRTG` in that required json. Their order comes from injection points, not from this list:

1. `MixinChunkGeneratorRTG` — Depths Deepslate fill at **TAIL** of `generateTerrain` (Y -64..-1, not Y=0).
2. `MixinChunkGeneratorRTGVillage` — layout-first + flatten `landscape.noise` **before** `generateTerrain`.

Safe sequence: flatten noise → RTG carves 0–255 → fill sub-zero solid. Do not conflate the two mixins.

### Config files (`config/arcanaquesttweaks/`)

Forge `@Config` on nested classes in `ArcanaQuestTweaksConfig`. Comfort is JSON, not Forge cfg.

| File | Class |
| --- | --- |
| `aqtweaks_stamina.cfg` | `StaminaModuleConfig` |
| `aqtweaks_client.cfg` | `ClientModuleConfig` |
| `aqtweaks_grimoireofgaia.cfg` | `GrimoireOfGaiaConfig` |
| `gaia_mob_damage.json` | `GaiaDamageConfig` (not `@Config`) |
| `aqtweaks_thaumcraft.cfg` | `ThaumcraftConfig` |
| `aqtweaks_bewitchment.cfg` | `BewitchmentConfig` |
| `aqtweaks_depths.cfg` | `DepthsModuleConfig` |
| `aqtweaks_rtg.cfg` | `RtgModuleConfig` |
| `aqtweaks_portal.cfg` | `PortalModuleConfig` |
| `aqtweaks_statskeeper.cfg` | `StatsKeeperModuleConfig` |
| `aqtweaks_reskillable.cfg` | `ReskillableModuleConfig` |
| `aqtweaks_spawning.cfg` | `SpawningModuleConfig` |
| `aqtweaks_comfort_settings.json` | `ComfortConfigLoader` (not `@Config`) |
| `aqtweaks_comfort_blocks.json` | `ComfortConfigLoader` (not `@Config`) |

`ConfigEventHandler` runs `ConfigManager.sync` on any `aqtweaks` cfg change, then `normalizePinned()`, invalidates DSS skill-cost cache, reloads spawn-type, spawn-party, and spawn-tier JSON plus spawn-rules cfg, rebuilds spawn group overrides, and restamps Reskillable attributes if that mod is loaded. Existing instance files keep old keys when Java defaults change.

Its reach is narrower than it looks. It subscribes to `ConfigChangedEvent.OnConfigChangedEvent`, which Forge fires from the **client in-game config GUI only** — never on a dedicated server, and never from hand-editing a cfg file. The only JSON it reloads is spawn-type, spawn-party, and spawn-tier (plus `mob_spawnrules.cfg`); comfort (`aqtweaks_comfort_settings.json`, `aqtweaks_comfort_blocks.json`) and `gaia_mob_damage.json` are preInit-only and need a **restart**.

`aqtweaks_grimoireofgaia.cfg`, `aqtweaks_thaumcraft.cfg`, and `aqtweaks_bewitchment.cfg` wrap their keys in a `general { }` block because those three `@Config` annotations omit `category = ""`. `aqtweaks_spawning.cfg` and `aqtweaks_statskeeper.cfg` also nest a `General` object (still `category = ""`) so their keys sit in `general { }` too. The remaining six with `category = ""` keep keys at file root. That asymmetry is **intentional** — normalizing the three omit-category files would reset tuned values in existing instance files.

Pack-owned (not Tweaks): `config/arcanaquest/mob_overworldspawntype.json`, `mob_spawnparties.json`, `mob_tier.json`, and `mob_spawnrules.cfg` for the [spawning](spawning.md) module.

### `util/Reflect.java`

Cached reflection for entity/world/block/NBT/sound/primer and soft-mod APIs (Elenai weight, Grapple, glider, thirst, Reskillable perk ids). Per-level Reskillable bonuses compile-hard the API in the [reskillable module](reskillable.md). Warp stays `ThaumcraftHelper` reflection; focus mixins compile-hard TC in [thaumcraft.md](thaumcraft.md).

**Use Reflect** for vanilla member access inside **`remap = false` mixin bodies** (those strings are not remapped). Pack parents: compile-hard their types when you need methods or classes.

**Direct vanilla in Tweaks’ own classes is allowed.** Event handlers are remapped (`defaultRemapJar = true`). `DepthsFogHandler.entity.world` and `ThaumcraftModule` `getChunkProvider()` are not defects.

Do not add raw MCP names inside `remap = false` mixins.

## Adding an integration

When hooking a new parent (or a new mixin on an existing one):

1. **Classpath:** add the exact jar to `libs/` (and to `build_gradle.ps1` `$deps` if this machine should copy it). Update [compatibility-matrix.md](compatibility-matrix.md). **Compile-hard** that parent if you need its classes or methods.
2. **`@Mod`:** `required-after` only if Tweaks must not load without it. Otherwise `after:` or omit. Pack mods can still be compile-hard without `required-after`.
3. **Mixin:** new json `required: false` unless the pack always ships the parent **and** missing it should crash. Register the json in `AQTweaksLateMixinLoader`. Mixin targets: SRG in vanilla, parent members as in that jar. `required: false` is load-time skip, not “string-target only.”
4. **Side:** client-only in the json `client` array or `@SideOnly`. Packets: `SimpleNetworkWrapper` side as today (stamina 0–2 are SERVER).
5. **Absent parent:** `Loader.isModLoaded` or mixin json `required: false` so isolated boot can skip. Do not `import` parent types from **always-loaded** classes (Bewitchment `Ritual` is compile-hard on the handler that only registers when loaded — keep that class off the bus).
6. **Config:** new `@Config` defaults; instance files **keep old keys**. Document live vs dead knobs in the module doc.
7. **Verify:** add a row to [verification.md](verification.md). Worldgen → new chunks. Mixin vanilla calls → Reflect or remap.

## Workflow (always)

1. Investigate read-only.
2. Write `.cursor/plans/YYYYMMDD-HHmm-<kebab-task>.md` (never repo-root `implementation_plan.md`), also put the plan in chat. Delete that file when the chat’s work is finished.
3. Wait for explicit `proceed`.
4. Implement, then `.\build_gradle.ps1` unless told not to rebuild. Portable compile: `.\gradlew.bat build`. That path already sets `--release 21`. Never drop that flag. Details: [build-and-release.md](build-and-release.md).

Worldgen changes apply to **new chunks only**.
