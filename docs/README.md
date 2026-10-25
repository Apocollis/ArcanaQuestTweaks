# Arcana Quest Tweaks — Architecture

This directory is the design and engineering spec for `aqtweaks` **1.10**. Read the index, then the module file for the system you are changing. Worldgen applies to **new chunks only**.

**Ops (reproduce / ship):** [compatibility-matrix.md](compatibility-matrix.md) · [build-and-release.md](build-and-release.md) · [verification.md](verification.md) · [changelog.md](changelog.md) · [history/2026-10-code-review-cleanup.md](history/2026-10-code-review-cleanup.md)

Mod: `aqtweaks`. Minecraft 1.12.2 / CleanroomMC / Forge. Stay on **1.10** unless asked to bump.

**Always compile as Java 21.** Gradle toolchain may be JDK 25, but `JavaCompile` must keep `options.release = 21` (class major 65). Do not emit Java 22+ bytecode. Mixin/Fugue will refuse class version 66+. Details: [build-and-release.md](build-and-release.md).

`aqtweaks` is a **tweak layer** for the **Arcana Quest pack**, not a standalone optional-mod product. The pack is expected to ship required parents (DEVBOX + `libs/`). If you need a parent class or method, **compile-hard** (`import` + jar on the classpath). Do not add `Reflect` wrappers just to avoid a compile dependency. If compile-hard gains nothing, keep `Loader.isModLoaded`. Mixin json `required: false` only skips that json at **load** if the jar is missing; it does not forbid compiling against it. Curse relations: [compatibility-matrix.md](compatibility-matrix.md#curseforge-relations-file-page).

Parent mods still own their systems. Tweaks listens to Forge events, calls public APIs (`FeathersHelper`, Thaumcraft warp caps, Bewitchment `Ritual`), or mixins parent methods when events are not enough. Vanilla members are called directly everywhere, including inside `remap = false` mixin bodies — see below.

## Module docs

Each file covers: what Tweaks changes, how the **parent mod** implements the feature, how Tweaks hooks it, algorithms, live config, files/mixins, and do-not-regress rules.

| Module | Parent mod(s) | Doc |
| --- | --- | --- |
| Stamina | Elenai Dodge 2 Extended, Grappling Hook, Dynamic Sword Skills, Embers, Reskillable, Simple Difficulty, Spartan Weaponry | [stamina.md](stamina.md) |
| Temperature | Simple Difficulty + optional Serene Seasons (greenhouse glass by registry name) | [temperature.md](temperature.md) |
| Reskillable | Reskillable + optional Effortless Building, Botania, Bewitchment, Astral, Embers, Simple Difficulty | [reskillable.md](reskillable.md) |
| Grimoire of Gaia | Grimoire of Gaia (`gaia`) | [grimoire-of-gaia.md](grimoire-of-gaia.md) |
| Thaumcraft | Thaumcraft 6 | [thaumcraft.md](thaumcraft.md) |
| Bewitchment | Bewitchment + Thaumcraft | [bewitchment.md](bewitchment.md) |
| Comfort | Vanilla + optional Thaumcraft, Simple Difficulty, Biomes O' Plenty | [comfort.md](comfort.md) |
| Portal | Tweaks-owned (no parent) | [portal.md](portal.md) |
| Twilight Forest | RandomPortals + Twilight Forest | [twilightforest.md](twilightforest.md) |
| Aether | RandomPortals + The Aether (`aether_legacy`) | [aether.md](aether.md) |
| Depths | Depths Update, YUNG's Better Caves, RTG, CoFH World, Recurrent Complex, optional Quark | [depths.md](depths.md) |
| RTG | Realistic Terrain Generation + vanilla `MapGenVillage` + Recurrent Complex + Astral / Bewitchment Cambion / Mystical World huts | [rtg.md](rtg.md) |
| Village gen (pack pipeline) | Vanilla + RTG + Geographicraft + Recurrent Complex + Charm + Tweaks overlay | [villagegen_info.md](villagegen_info.md) |
| Client | Toughness Bar (optional), tooltip lines for non-Metallurgy tools, default world type (RTG) | [client.md](client.md) |
| MineMenu | Vanilla client mouse grab (MineMenu, Reskillable, Hwyla screens) | [minemenu.md](minemenu.md) |
| Spawning | Vanilla `PotentialSpawns` + pack `mob_overworldspawntype.json` (InControl still owns deny) | [spawning.md](spawning.md) |
| Recipes | Forge `CraftingHelper` (Metallurgy / Spartan JSON) | [recipes.md](recipes.md) |
| Advancement | Animania Base (Farm / Extra addon JSON via Base handler) | [advancement.md](advancement.md) |
| Somnia | Somnia Refreshed | [somnia.md](somnia.md) |
| Stats Keeper | Stats Keeper (`stats_keeper`) | [statskeeper.md](statskeeper.md) |
| Game Stages | Game Stages + Recipe Stages + Chisel | [gamestages.md](gamestages.md) |
| Quality Tools | Quality Tools + Embers (Dawnstone) + Crafting Runes (registry names) | [qualitytools.md](qualitytools.md) |
| Better Mineshafts | YUNG’s Better Mineshafts + RTG locate | [bettermineshafts.md](bettermineshafts.md) |
| Simple Tomb | Simple Tomb death drop slot restore, displacement, Baubles, and admin backups | [simpletomb.md](simpletomb.md) |
| Server | Dedicated-server fixes for client-only calls in parents: GeographiCraft spawn salvage | [server.md](server.md) |
| Compatibility / jars | Compile vs mixin vs runtime vs copy script | [compatibility-matrix.md](compatibility-matrix.md) |
| Build / deploy | `gradlew build` vs `build_gradle.ps1` | [build-and-release.md](build-and-release.md) |
| Release smoke | Boot, optional absences, worldgen, stamina | [verification.md](verification.md) |

Astral surface shrines, Bewitchment Cambion houses, and Mystical World thatch huts are **not** separate modules. They are post-terrain structure settle/skip under [rtg.md](rtg.md). Ritual warp is [bewitchment.md](bewitchment.md). Cambion **worldgen** is RTG.

## How modules load

### `@Mod` vs real parents

`ArcanaQuestTweaks` declares:

`required-after:elenaidodge2;required-after:charm;after:incontrol;after:grimoireofgaia;after:thaumcraft;after:bewitchment;after:grapplemod;after:dynamicswordskills;after:baubles;after:embers;after:qualitytools;after:reskillable;after:effortlessbuilding;after:stats_keeper;after:randomportals;after:twilightforest;after:aether_legacy;after:chisel;after:gamestages;after:recipestages;after:simpletomb;after:chunksteward`

That is **not** the full parent list. Soft parents that Tweaks mixins or events against, without `after:` / `required-after:`:

| Parent | Used by | If missing |
| --- | --- | --- |
| RTG, Depths Update, Better Caves, CoFH World, Recurrent Complex | Depths + RTG mixins in **required** `mixins.aqtweaks.json` (plus optional `mixins.aqtweaks.depthsupdate.json` for `HeightManager` cache) | Required json can fail apply; this pack always ships them |
| Astral Sorcery | Optional mixin json + village shrine handler | Mixin config `required: false`; handler not registered |
| Mystical World | Optional mixin json | No hut skip/settle |
| Dungeon Additions | Optional mixin json (`mixins.aqtweaks.da.json`) | No DA village overlap skip |
| Simple Difficulty, Biomes O' Plenty | Comfort potions / hot spring; Water Collector (SD mixins + bus handler); BOP optional mixin skips village water/quicksand lakes | Those benefits no-op; SD/BOP mixin json skipped; campfire neighbor mixin and crucible heat spoof are skipped with that json / when campfire block is absent (Thaumcraft still loads; unlit or missing campfire does not heat a crucible); vanilla water-lake skip still runs |
| Rustic | Iron Gut `FluidBooze.inebriate` | Mixin json skipped; tipsy applies as stock |
| Roguelike Dungeons Arcana | Thaumcraft dungeon warp via `isInsideStructure("RoguelikeDungeon")` | Dungeon exposure never matches |
| Reskillable | Per-level bonuses + stamina perk id lookup | Module not registered; stamina `hasUnlockable` no-ops |
| Effortless Building | Building skill place-reach / max blocks | Mixin json skipped; Building drip unused |
| InControl | Spawning layer filter + pack fill + structure BB cache (compile-hard min-distance and `StructureCache`) | Missing jar fails compile; pack always ships it |
| Animania | Advancement: skip Base world-load reload + Farm/Extra inject. Rancher clocks if Reskillable | Mixin json skipped; Animania advancements load as stock; no Rancher |
| YUNG’s Better Mineshafts | Locate pin + failed-entrance stub + Tweaks rate/spacing/Y | Mixin json skipped; stock BM Y=64 locate |
| Stats Keeper | Elixir of Vitality drink cancel at SK cap | Handler not registered; vanilla + SK consume/refuse as stock |
| RandomPortals + Twilight Forest | TF landing safety + grass pads | Handler not registered; RP mixin json skipped; stock RP 1:1 landings |
| RandomPortals + Aether | Island landing | Handler not registered; mixin still compiles Aether types if RP json applies |
| Chisel + Game Stages + Recipe Stages | Chisel output gated on Recipe Stages | Mixin json skipped; stock Chisel; crafting-table stages unchanged |
| Quality Tools | Loot stamp, wear/Broken, Dawnstone runes | Optional json skipped; vanilla early mixins still apply (pack ships QT); handler not registered |
| Quark | Depths lower-cavern speleothem primer decor | Helper not called; +Y stock Quark unchanged; Deepslate columns/spikes still generate |
| Simple Tomb | Death drop origin slot restore + Baubles + admin backup | Module not registered; mixin json skipped; stock Simple Tomb behavior |
| Chunk Steward | Server tick headroom pacing (pacing/StewardPacing -> ChunkStewardAPI) for DeferredRelight and SomniaOptimizationHandler | StewardPacing.isPresent() false (or LinkageError caught on stale jar); falls back to unpaced fixed budgets |

### Init (`CommonProxy` / `ClientProxy`)

**preInit**

- Register SimpleNetworkWrapper messages 0–2 (stamina climb/grapple), 3 (prospector marks), and 4 (temperature feather cap). See [stamina.md](stamina.md).
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
- If `randomportals` **and** `twilightforest`: `TfPortalLandingHandler` (TF dest biome/landmark/grass landing; [twilightforest.md](twilightforest.md)).
- If `randomportals` **and** `aether_legacy`: `AetherPortalLandingHandler` (Aether island snap; [aether.md](aether.md)).
- If `qualitytools`: `QualityToolsModule` (loot/drop stamp). `postInit` registers Dawnstone rune recipes when `embers` is also loaded. See [qualitytools.md](qualitytools.md).
- If `simpledifficulty`: `SimpleDifficultyModule`. Its constructor also registers `DynamicModifierInsulation` with `TemperatureRegistry` once (surface indoor insulation; see [temperature.md](temperature.md)).
- If `animania`: `AnimaniaModule`.
- If `reskillable`: `RespiteHandler` and `PerkWishlist`.
- If `reskillable` and `dynamicstealth`: `PerkStealth`.
- Game Stages / Chisel: no bus handler (mixins only; [gamestages.md](gamestages.md)).
- If `simpletomb`: `SimpleTombModule`. Server starting: `/aqtomb` (`CommandAqTomb`, [simpletomb.md](simpletomb.md)).

**init (client)**

- `StaminaModuleClient`, `DssSkillsGuiClient` (Skills GUI `isPressed()` when DSS did not already send, plus client `/dssgui`; compile-hard DSS; registered only if `dynamicswordskills`), `BaublesMenuClient` (Baubles `isPressed()` → `PacketOpen(EXPANSION)` when Baubles did not already send; compile-hard BaublesEX; registered only if `baubles`), `DepthsFogHandler`, `ClientModule`, `ProspectorOutline`.
- Entity renderer for `EntityArcaneRift` in **client preInit**. If `grimoireofgaia` and Deep Dwarf enabled: `RenderDeepDwarf`. Item models on `ModelRegistryEvent`.

`postInit` registers `SpawnLayerFilter` (after InControl `PotentialSpawns`), enables structure cave exemption if `incontrol` is loaded. `ArcanaQuestTweaks.serverStarting` (`@Mod.EventHandler` on `FMLServerStartingEvent`) registers `/aqvillage` (`CommandAqVillage`, [rtg.md](rtg.md)) and `/aqcomfort` (`CommandAqComfort`, [comfort.md](comfort.md)).

### MixinBooter: early vs late

Vanilla `World`, `MobSpawnerBaseLogic`, `TileEntityLockableLoot`, `ItemStack`, `BlockCrops`, client `Minecraft`, and `GuiIngameForge` are already loaded when late mixins prepare. Any new mixin on one of these goes in the early json. Portal glowstone light (`MixinWorldRiftLight` on `World.getRawLight`), deferred relight of failed light checks (`MixinWorldCheckLight` on `World.checkLight`, queue in `world/DeferredRelight`), cage fail delay (`MixinMobSpawnerBaseLogic` on `updateSpawner`), Quality loot stamp (`MixinTileEntityLockableLoot`), Quality wear (`MixinItemStackQualityDurability`), Reskillable free break (`MixinItemStackDurability` on `damageItem`) and crop seed invoker (`MixinBlockCropsSeed`), MineMenu mouse re-grab (`MixinMinecraftMouseGrab` on `displayGuiScreen`, **client** array), and the runic shielding health redirect (`thaumcraft.MixinGuiIngameForgeRunicShield` on `GuiIngameForge.renderHealth`, **client** array) are in **`mixins.aqtweaks.early.json`**. Charm village paste (`mixins.aqtweaks.charm.json`, `required: true`) is on the same jar `MixinConfigs` so Charm `ASMHooks` is mixed before Charm’s transformer defines it. MixinBooter 11 reads `MixinConfigs` from the jar manifest (set in `build.gradle`). Do not register these json files from `AQTweaksLateMixinLoader`. Missing either fails load. Charm is `@Mod required-after` (Curse / pack prerequisite).

### MixinBooter late loader

`AQTweaksLateMixinLoader` always returns these configs (MixinBooter / Fugue). Charm paste is **not** here — it is on jar `MixinConfigs` with the early json. Its `shouldMixinConfigQueue(String)` override queues every config except `mixins.aqtweaks.geographicraft.json`, which is queued only when `Loader.isModLoaded("geographicraft")`. That is the only config gated this way; the others rely on `required: false` alone.

| File | `required` | Module | If parent jar missing |
| --- | --- | --- | --- |
| `mixins.aqtweaks.json` | **true** | Depths, RTG villages, Recipes | Load fails |
| `mixins.aqtweaks.vanilla.json` | **true** | Vanilla-only: `AccessorChunk` (Somnia light checks), `AccessorChunkProviderServer` (`chunkGenerator` and `loadedChunks`), `InvokerStructureStart` (protected `updateBoundingBox` for village/mineshaft pads), `MixinEntityAITasks` (swallows an NPE thrown by any AI task `updateTask`, then `resetTask`; logs once per task class via `util/AiTaskReport`), `MixinEntityGhastAIFireballAttack` (no target: stop attacking) | Load fails |
| `mixins.aqtweaks.grapple.json` | false | Stamina | Skip |
| `mixins.aqtweaks.dss.json` | false | Stamina | Skip |
| `mixins.aqtweaks.elenaidodge.json` | false | Stamina feather colors + armor tooltip weight | Skip |
| `mixins.aqtweaks.baubles.json` | false | MineMenu + Thaumcraft ring models | Skip |
| `mixins.aqtweaks.toughnessbar.json` | false | Client HUD | Skip |
| `mixins.aqtweaks.astral.json` | false | RTG shrines + Reskillable Astromancer altar | Skip |
| `mixins.aqtweaks.bewitchment.json` | false | RTG Cambion + circle/menhir/wickerman; Reskillable Witch drain/Hearth/Stitch | Skip |
| `mixins.aqtweaks.mysticalworld.json` | false | RTG Mystical huts | Skip |
| `mixins.aqtweaks.biomesoplenty.json` | false | RTG BOP water/quicksand village skip | Skip |
| `mixins.aqtweaks.da.json` | false | RTG Dungeon Additions village overlap skip | Skip |
| `mixins.aqtweaks.depthsupdate.json` | false | Depths: `MixinHeightManager` dimension-indexed `HeightCache` for `HeightManager.get(int)` and `isExtended(int)` | Skip |
| `mixins.aqtweaks.gaia.json` | false | Grimoire of Gaia drop pierce + recast bolts/bombs + Deathword ranged | Skip |
| `mixins.aqtweaks.effortlessbuilding.json` | false | Reskillable Building EB place reach + max blocks | Skip |
| `mixins.aqtweaks.thaumcraft.json` | false | Thaumcraft focus HP magic flag + Heal scale + runic shielding HUD + Infernal Furnace gated metals | Skip |
| `mixins.aqtweaks.animania.json` | false | Advancement: cancel Animania `onWorldLoad` | Skip |
| `mixins.aqtweaks.somnia.json` | false | Somnia: chunk light fix, 3-tier SMP sleep (Case A/B/C), Case B 2x time, fatigue tuning & chat notifications | Skip |
| `mixins.aqtweaks.incontrol.json` | false | Spawning: `StructureCache.parseStructureData` BB chunk expand | Skip |
| `mixins.aqtweaks.bettermineshafts.json` | false | Better Mineshafts locate pin + entrance stub + Tweaks placement + RTG 9 mineshaft paste hook (`MixinChunkGeneratorRTGMineshaftPaste`) | Skip |
| `mixins.aqtweaks.randomportals.json` | false | TF grass pad + Aether island pad on RP generate | Skip |
| `mixins.aqtweaks.chisel.json` | false | Game Stages: Chisel GUI + `canChisel` | Skip |
| `mixins.aqtweaks.recipestages.json` | false | Game Stages: capture `setRecipeStage` | Skip |
| `mixins.aqtweaks.qualitytools.json` | false | Quality Tools living-update skip, reforge base, Dawnstone mismatch | Skip |
| `mixins.aqtweaks.simpledifficulty.json` | false | Reskillable Water Collector + Cinder/Astral temp clamp + campfire neighbor notify (`MixinBlockCampfire`: `setBlockState` flags `2` become `3` so Inspirations cauldrons see light and extinguish) + boat position lift (`MixinWorldUtil`: `getSidedBlockPos` returns one block up for a player riding a boat over water, so an open boat is not wet; [temperature.md](temperature.md)) | Skip |
| `mixins.aqtweaks.playerrevive.json` | false | Reskillable Fast Revive | Skip |
| `mixins.aqtweaks.rustic.json` | false | Reskillable Iron Gut | Skip |
| `mixins.aqtweaks.botania.json` | false | Reskillable Druid mana thrift + Grove | Skip |
| `mixins.aqtweaks.embers.json` | false | Reskillable Artificer Ember thrift + Foundry Pulse | Skip |
| `mixins.aqtweaks.prospectus.json` | false | Reskillable Prospector pick accuracy + ore outline | Skip |
| `mixins.aqtweaks.sereneseasons.json` | false | `MixinSSCommand`: `/ss setseason <TAB>` lists the sub-season names; completion never returns null (fixes the `list1` NPE, AQ-005) | Skip |
| `mixins.aqtweaks.treechopper.json` | false | Reskillable harvest perks on Tree Chopper trees (`MixinTreeHandler` sets the felling player) | Skip |
| `mixins.aqtweaks.simpletomb.json` | false | Simple Tomb: origin slot mapping + Baubles + non-destructive restore | Skip |
| `mixins.aqtweaks.libraryex.json` | false | `MixinEntityHelper`: synchronize `ENTITY_RESOURCE_LOCATION_CACHE.computeIfAbsent` in `EntityHelper.getEntityLocation` so client/server entity ticks do not throw CME | Skip |
| `mixins.aqtweaks.geographicraft.json` | false | Server: `MixinDimensionManager` redirects client-only `Biome.getBiomeName` in `salvageSpawn` to the registry name ([server.md](server.md)). Queued only if `geographicraft` is loaded | Not queued |

`mixins.aqtweaks.json` contents (package `com.apocollis.aqtweaks.mixin`):

- Client: `MixinRenderGlobal` (Depths hide sky), `MixinEntityRendererMouse` (skip look while a screen is open; [minemenu.md](minemenu.md)), `MixinEntityRendererLight` (Dark Vision lightmap lift on `updateLightmap`; [reskillable.md](reskillable.md))
- Common: `depthsupdate.MixinDepthsCaveNoiseGenerator`, `cofh.MixinDistributionUniform`, `reccomplex.MixinRayMatcher`, `reccomplex.MixinGenericVillageCreationHandler`, Better Caves / RTG village mixins listed in [depths.md](depths.md) and [rtg.md](rtg.md), `MixinStructureVillagePieces`, `MixinStructureStartVillagePaste`, `MixinWorldGenLakes`, `MixinMapGenVillageInside/Spawn/Start/World`, `MixinCraftingHelperFindFiles`, `MixinWorldEntitySpawner`. Charm paste: `mixins.aqtweaks.charm.json` on jar `MixinConfigs`. Portal `MixinWorldRiftLight` and `MixinBlockRiftLight`, cage `MixinMobSpawnerBaseLogic`, Quality `MixinTileEntityLockableLoot` and `MixinItemStackQualityDurability`, Reskillable `MixinItemStackDurability` and `MixinBlockCropsSeed`, and MineMenu `MixinMinecraftMouseGrab` (client) are in `mixins.aqtweaks.early.json`. InControl `MixinStructureCache` is in `mixins.aqtweaks.incontrol.json`. Better Mineshafts mixins (`canSpawn`, locate pin on `MapGenMineshaft` guarded to BM, entrance stub, Start Y) are in `mixins.aqtweaks.bettermineshafts.json`. The Depths Y 0-4 seam pass is `SeamReinforcer`, called from `MixinChunkGeneratorRTG` at RETURN of `generateChunk`. RandomPortals grass pads / Aether island pads: `mixins.aqtweaks.randomportals.json`. Quality Tools parent mixins: `mixins.aqtweaks.qualitytools.json`.

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
| `aqtweaks_twilightforest.cfg` | `TwilightForestModuleConfig` |
| `aqtweaks_aether.cfg` | `AetherModuleConfig` |
| `aqtweaks_statskeeper.cfg` | `StatsKeeperModuleConfig` |
| `aqtweaks_gamestages.cfg` | `GameStagesModuleConfig` |
| `aqtweaks_qualitytools.cfg` | `QualityToolsModuleConfig` |
| `aqtweaks_reskillable.cfg` | `ReskillableModuleConfig` |
| `aqtweaks_spawning.cfg` | `SpawningModuleConfig` |
| `aqtweaks_bettermineshafts.cfg` | `BetterMineshaftsModuleConfig` |
| `aqtweaks_minemenu.cfg` | `MineMenuModuleConfig` |
| `aqtweaks_comfort_settings.json` | `ComfortConfigLoader` (not `@Config`) |
