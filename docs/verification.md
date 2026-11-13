# Verification (1.9)

Last updated: 2026-10-16.

Manual release / smoke checklist. **No automated tests.** Harness: CurseForge **Arcana Quest DEVBOX**, remapped `ArcanaQuestTweaks-1.10.jar` in `mods/`. Algorithms and full checklists stay in module docs; this is the pack-level pass/fail.

Worldgen applies to **new chunks only**.

## Build artifact

- [ ] `.\gradlew.bat build` or `.\build_gradle.ps1` succeeds (`verifyReleaseJar`: class major 65, `VillagePlate.class`, mixin json + `mixins.aqtweaks.refmap.json`).
- [ ] Instance `mods/` has `ArcanaQuestTweaks-1.10.jar`, **not** `-dev`.
- [ ] Only one Tweaks jar (the deploy script deletes other `ArcanaQuestTweaks-*.jar`).

## Boot

- [ ] Client starts the full pack; Charm is present (`required-after:charm`). No mixin apply crash from `mixins.aqtweaks.json`, `mixins.aqtweaks.early.json`, or `mixins.aqtweaks.charm.json`. Log must not say `MixinWorldRiftLight` / `World was loaded too early`, `MixinMinecraftMouseGrab` / `Minecraft was loaded too early`, `MixinMobSpawnerBaseLogic` / `MobSpawnerBaseLogic was loaded too early`, `MixinTileEntityLockableLoot` / `TileEntityLockableLoot was loaded too early`, `MixinItemStackQualityDurability` / `MixinItemStackDurability` / `ItemStack was loaded too early`, `MixinBlockCropsSeed` / `BlockCrops was loaded too early`, `MixinASMHooksVillagePaste` / `ASMHooks was loaded too early`, `MixinWorldGenLakes` / `field_150589_a was not located` / `WorldGenLakes in invalid classes`, `empty category` / `StatsKeeperModuleConfig` / `BetterMineshaftsModuleConfig`, BM `setBoundingBox` / `func_75072_c` was not located, `MixinRPOTeleporter` / `field_85192_a was not located` in `RPOTeleporter`, `parseStructureData is not cancellable`, `StackOverflowError` / `ItemTechnomancerScribingTools` / `QualityDurability.afterSetDamage` during recipe init, or world-tick `StackOverflowError` / `QualityStamp.stampInventory` / `fillWithLoot`. Optional `mixins.aqtweaks.gaia.json` must not log `InvalidInjectionException` (vanilla INVOKEs must be MCP + `remap = true`; a miss boots anyway because `required: false`).
- [ ] Mixin log does **not** say Tweaks mixins require class version 69 (Java 21 class files).
- [ ] `python scripts/check_mixin_targets.py` exits 0 (every mixin `method=` is declared on its target class). Optional-parent mixin jsons set `injectors.defaultRequire = 1`, so an injector that matches nothing now skips that mixin with a log error instead of silently doing nothing. Check `cleanmix.log` for any `aqtweaks` injection failure.
- [ ] No `[AQ-REFLECT]` lines (the class is gone). `cleanmix.log` shows APPLY audits for the `Accessor*` / `InvokerStructureStart` interfaces in `mixins.aqtweaks.vanilla.json` (a required json: a bad accessor target would fail boot).
- [ ] Wait through full JEI / ThaumicJEI / **TC6 Aspects 4 JEI** load. Title screen stays up. No `hs_err_pid*.log`.
- [ ] Dedicated server: **not routinely tested** in this repo. If you ship a server, start one with the same mods and confirm it reaches “Done”.

### Optional-mod absences (safe)

These json files are `required: false`. Removing the parent should skip that json, not crash Tweaks:

| Remove | Expect |
| --- | --- |
| Grapple | No motor mixin; grapple stamina no-ops if not loaded |
| Dynamic Sword Skills | No skill feather gate |
| Toughness Bar | No HUD mixin; feathers still on the right; tooltip compat still runs |
| Astral Sorcery | No shrine mixins; no village `AQTSmallShrine` |
| Bewitchment | No Cambion/circle mixins; no ritual wrap (handler not registered) |
| Mystical World | No hut skip/settle |
| Dungeon Additions | No `mixins.aqtweaks.da.json`; DA structures generate as stock |
| Biomes O' Plenty | No BOP lake village skip mixin; vanilla water-lake skip still runs; hot spring comfort no-ops |
| Grimoire of Gaia | No Gaia mixins; pierce and MAGIC bolts/bombs stay parent vanilla |
| Reskillable | No per-level bonuses; stamina perk lookups no-op |
| Effortless Building | No Building place-reach / max-blocks mixin |
| Thaumcraft | No focus mixins; frost stays `thrown` / not Magic; warp handler not registered |
| Animania | No `MixinAddonHandler`; stock world-load advancement reload + Farm/Extra inject |
| Somnia Refreshed | No `MixinSomniaUtil`; stock Somnia light check behavior |
| InControl | No `MixinStructureCache`; stock origin-chunk `isInStructure` (Tweaks still loads) |
| YUNG’s Better Mineshafts | No locate/stub mixins; stock BM Y=64 `/locate Mineshaft` |
| Stats Keeper | No `LifeElixirCapHandler`; stock SK Finish refuse + vanilla consume |
| RandomPortals | No `MixinRPOTeleporter`; stock RP generate (leaves/Y≥70 fallback). Handler also off if TF missing |
| Twilight Forest | No `TfPortalLandingHandler` unless RP is also present. No `CarminiteReactorRitualHandler` unless Thaumcraft, Game Stages, Betweenlands, and ContentTweaker are also present |
| The Aether | No `AetherPortalLandingHandler` unless RP is also present |
| Quark | Helper not called; +Y stock Quark unchanged; Deepslate columns/spikes still generate |
| Chisel | No Chisel mixins; stock GUI / in-world carving |
| Recipe Stages | No `setRecipeStage` capture; Chisel gate has no CT index (fallback map also missing) |
| Game Stages | `lockedStage` no-ops; Chisel mixins still apply if Chisel is present |
| Quality Tools | No `mixins.aqtweaks.qualitytools.json`; vanilla loot/durability mixins still apply (pack ships QT); no rune recipes |
| Simple Tomb | No `mixins.aqtweaks.simpletomb.json`; stock tomb placement and vanilla giveInventory restore |
| Chunk Steward | `StewardPacing.isPresent()` false (or `LinkageError` caught on stale jar); `DeferredRelight` and `SomniaOptimizationHandler` run with stock fixed budgets |

### Do not treat as optional

Missing **RTG, Depths Update, Better Caves, CoFH World, Recurrent Complex, or IvToolkit** with the current required mixin json can **fail mixin apply** at boot. This pack always ships them (`mixins.aqtweaks.depthsupdate.json` is `required: false` with `defaultRequire: 1`, while the core Depths mixins remain in required `mixins.aqtweaks.json`). Elenai Extended and **Charm** are `@Mod` **required-after**. InControl is `@Mod` **after** and compile-hard for pack fill.

## Config

- [ ] After a Java default change, instance cfg still has the **old** value until edited (e.g. RTG Coast Buffer 32 vs 16).
- [ ] Change a live cfg in-game: stamina DSS costs refresh (`DssSkillCosts.invalidate`). Comfort JSON needs a restart (loaded in preInit). Settings vs blocks are separate files; the old combined `aqtweaks_comfort.json` is ignored.

## Worldgen (new chunks)

Seam, village world push/pop and Mineshaft locate were dead code until the 2026-10-04 target fixes; verify them on a **fresh world**.

`Village Flatten Debug` is **off** by default. Turn it on only while diagnosing; it appends every line to instance `logs/villagepatch.log` (not `latest.log`) and stalls chunk gen.

| Check | Expect | Doc |
| --- | --- | --- |
| Village generation end to end (new chunks, `/aqvillage`, coast and inland) | Same layout as before Reflect removal. With village debug on: `stamp village plate` and `seal chunk=...` lines appear, no `NullPointerException`/`ClassCastException` from `StructureAccess`. Reload the world: village Starts keep their padded boxes (the save call now works). With village debug or perf timers on, one `village layout well=` line per new village (`logs/villagepatch.log`, and `[AQ-PERF]` `village.layoutAudit` when timers are on) | [rtg.md](rtg.md) |
| New Atum pyramid | Apron sits on the dune average. The door is about 3 blocks above it (`Pyramid Door Lift`), with the stairs on the sand and the maze below the apron. Hermite slope, no sand wall into the face. The entrance walk is 6 wide and slopes from the door to the sand. Pyramids already pasted keep the ground-level door. Master switch off restores Atum's sink | [client.md](client.md) |
| Atum sandstorm (`/atumweather sandstorm`, outdoors, then in a doorway) | Looking upwind, sand fills about 50 blocks toward the gale. Storm mix is about 85% small flecks. Dust spawns only under open sky, then blows through an open doorway and stops on a wall. Under a roof the fog opens back to the clear range. Bare-headed, the air bar drains and choking damage starts when it is empty; a desert helmet refills it. Missing chest, legs, or boots takes half a heart about every 4 seconds; full armor or shelter stops it | [client.md](client.md) |
| Walk and fly into fresh terrain at 20 TPS with Chunk Steward generating | Chunks appear continuously ahead of the player. No empty band that fills in together after movement stops. Chunk Steward `dbg near` shows few loaded-unpopulated chunks near the player | [somnia.md](somnia.md) |
| Mineshaft Start / locate / entrance stub | Unchanged; no accessor errors | [bettermineshafts.md](bettermineshafts.md) |
| Thaumcraft dimension warp, sleep cleanse, exposure warp | Same awards, sound and chat; the 2-second delayed award still lands | [thaumcraft.md](thaumcraft.md) |
| Boat in water with Simple Difficulty | Temperature sample lifts as before (`MixinWorldUtil` now direct) | [temperature.md](temperature.md) |
| Sleep 6+ in-game hours with Somnia (alone, then with others), wake before morning, and leave the bed early after 6h | Sticky warp drops by `Normal Warp Reduction` each time; a sleep under 6h clears nothing; chat line shows when something cleared | [thaumcraft.md](thaumcraft.md) |
| Sleep 8 hours with temp warp and a Homestead band I / II / III bed | About 2 / 3 / 6 temp warp cleared over 8 h in Case B (none double counted in Case A where player ticks run); no Homestead means none from this credit | [comfort.md](comfort.md) |
| Boot log, bibliocraft | No `Parsing error loading recipe bibliocraft:tapemeasure`; `Skipping recipe JSON with known-missing item bibliocraft:tape` appears once | [recipes.md](recipes.md) |
| New RTG plains or hillside mineshaft | A 3×3 stone-brick collar about five blocks deep with a center hole, and a ladder down to the tunnel; no one-block plank lip on a slope; no shaft under water | [bettermineshafts.md](bettermineshafts.md) |
| Fell a tree with Tree Chopper at gathering 16+ with Lumberjack and Reforester | Extra logs roll per log; saplings roll per leaf (gathering x 0.03), never more than 64 from one tree; breaking a single log or leaf by hand still rolls; without Tree Chopper nothing changes | [reskillable.md](reskillable.md) |
| New RTG plains mineshaft, shaft built | `latest.log` has `[AQ-MINESHAFT] surface shaft hub=... target=... surfaceY=... siblings=N hubInside=...`; the ladder shaft ends inside a mineshaft tunnel or room (no stone-only shaft); when `hubInside=false` the shaft is at the nearest tunnel box, not the hub | [bettermineshafts.md](bettermineshafts.md) |
| Building 32, Normal mode: place and break a block about 2 blocks past normal reach (4.5 + 2) | Works; at Building 0 the same target is out of reach. Melee reach unchanged | [reskillable.md](reskillable.md) |
| Building perks Reach I/II/III, EB non-Normal mode | No perk: reach 5; Reach I (Building 10): 20; Reach II locked until I owned, then 50; III: 100. Upgrade items not craftable, do nothing on right-click; icons show the EB item art | [reskillable.md](reskillable.md) |
| Boot with the Batch 4 jar | No `Unlockable ... not found` Reskillable errors; `cleanmix.log` has no `aqtweaks` injection failure (required jsons now fail loudly); no `quality-wear` lines in `latest.log` unless `Trace Wear` is on | [qualitytools.md](qualitytools.md) |
| Enter the Nether for the first time, then wait 2s | Dimension warp, sound and chat arrive about 2 seconds later, with no new thread | [thaumcraft.md](thaumcraft.md) |
| Die with a Simple Tomb, keep an item soulbound, respawn | Items return to their slots from the tomb; no stray grave tag on kept items | [simpletomb.md](simpletomb.md) |
| Start a break with Stone Cleaver, cancel it, then use the tool on something else | The unrelated damage is not free | [reskillable.md](reskillable.md) |
| Open a rift pair, look away and walk off, then return; go through a cross-dimension pair and come back | Rift cylinder and particles stay visible when looking away; the rift renders on return; `latest.log` has one `[AQ-PORTAL] DS tracker override` line; particles visibly thin out over the 60 s | [portal.md](portal.md) |
| Sleep with Somnia, then wake | Camera follows the mouse immediately; no attack click or Escape needed | [minemenu.md](minemenu.md) |
| Type `/ss setseason ` (with the trailing space) and press Tab | No crash report (`list1` is null); the 12 sub-season names are offered (`early_spring` ... `late_winter`) and narrow as you type, e.g. `mid` leaves the three `mid_` seasons | [compatibility-matrix.md](compatibility-matrix.md) |
| Explore new terrain, including a village and any structure with lanterns, at night | Torches, lamps and lanterns light their surroundings within a few seconds of generation, no manual torch needed; TPS and tick time unchanged while flying | [rtg.md](rtg.md) |
| Fly into an unvisited village at night (client side) | Charm perimeter torches and lanterns light their surroundings within a couple of seconds of the chunk arriving, no torch placed by hand; F3 light near them matches; FPS steady while exploring. With `Client Relight Debug` on, `[AQ-CLIENT-RELIGHT]` lines appear | [client.md](client.md) |
| Glide until feathers hit 0 | The glider retracts at the next billing tick and the player falls; feathers then regenerate. Deploying with 0 feathers is undone at the next tick | [stamina.md](stamina.md) |
| Village plate on a hill with caves below | Seal covers 15 blocks below the plate height and never goes below Y50; plate at Y64 seals Y50-64; caves open below that | [rtg.md](rtg.md) |
| Grappling Hook: plant, hang, swing, Shift+W climb, motor with and without Ember | Same bills as before (hang 1/s, swing 2/s, climb 3/s). Motor off when Ember is empty; unhook when feathers run out. No `NoSuchFieldException` / reflection warnings in the log | [stamina.md](stamina.md) |
| Open Glider with empty feathers | Glider undeploys. Without Open Glider installed, no `NoClassDefFoundError` | [stamina.md](stamina.md) |
| Village on water edge with a Waystones gazebo | Wet gazebo is rebuilt inland (`waystone relocate hit` with village debug on). Without Waystones installed, villages still generate | [rtg.md](rtg.md) |
| Sleep with warp, dimension first visit, exposure warp, ritual warp | Warp changes and syncs exactly as before; a Thaumcraft error logs once (`[AQ-TC]`) and does not disable later warp | [thaumcraft.md](thaumcraft.md) |
| Full Font, Vis Thrift, EB reach/max blocks, forage and wool perks | Unchanged behavior. With Reskillable or Animania Farm removed, no `NoClassDefFoundError` | [reskillable.md](reskillable.md) |
| Fluid-filled crucible over lit and unlit Simple Difficulty campfire | Lit: heats (cap 200); unlit: cools. Without Simple Difficulty, crucibles behave as stock | [thaumcraft.md](thaumcraft.md) |
| New Overworld chunks, fly caves from Y −60 to −1 | Vis crystal clusters sit on cave surfaces (air touching rock), mixed shard types, none inside solid stone. Surface crystals above Y 0 are unchanged. `Enable Deep Vis Crystals` = false leaves the deep band empty. With performance stage timers on, `[AQ-PERF]` includes `thaum.deepCrystals` | [thaumcraft.md](thaumcraft.md) |
| Village on a hill with caves below | Caves/ravines below Y50 stay open under the village; Y50 to plate is solid; wells and houses on solid ground; coastal brick wall reaches Y50 | [rtg.md](rtg.md) |
| `/locate Mineshaft`, TP, `/locate` again | The second locate returns a different mineshaft (not the one you stand in); with several generated, the nearer of registered vs predicted wins | [bettermineshafts.md](bettermineshafts.md) |
| Boot log | No `Mixin apply ... failed` for any `aqtweaks` json; `cleanmix.log` shows `aqtweaks$refreshMineshaftBox` on `StructureStart` | [bettermineshafts.md](bettermineshafts.md) |
| Fresh Overworld, fly new chunks | `[AQ-DEPTHS] Chunk pass: tunnel-path seam reinforce Y0-4 after BC` appears once in `latest.log`. Better Caves mouths at Y 0-4 connect to the Depths tunnels; ocean/river/beach columns keep Deepslate at Y0 (no water draining into the deep) | [depths.md](depths.md) |
| New village chunks with `/aqvillage` | Village still plates; no growth of the generator/world stacks (no repeated `pushGenerator` without pop in `villagepatch.log` with debug on); starts rejected by `rejectCoastalVillageStarts` are forgotten | [rtg.md](rtg.md) |
| `/locate Mineshaft` on a new world | Pin lands on a registered Better Mineshafts Start (Y 24 when unexplored); `cleanmix.log` shows `MixinMapGenMineshaftLocate` injected into `MapGenMineshaft`; vanilla Mineshaft generators in other dimensions unaffected | [bettermineshafts.md](bettermineshafts.md) |
| Flying new chunks (near and far from villages) | Away from villages, chat TPS stays near 20 (no growing hitch as more towns exist). A village still plates. With debug off, `villagepatch.log` stays empty | [rtg.md](rtg.md) |
| `HeightManager` array cache (`Verify HeightManager Cache` = true) | Fly new chunks across Overworld, Nether, End, Twilight Forest, Aether: zero `[AQ-DEPTHS] HeightCache mismatch` warnings in `latest.log`; Spark shows `HeightManager.get` / `isExtended` near 0% | [depths.md](depths.md) |
| Deferred paste relight (`Performance Stage Timers` = true) | Generate a High City at distance and again standing within 4 chunks (priority batch, `paste.queued ...,priority`, no single long tick): `paste.template` ends `,bulk,deferred` and is about 3 s, no multi-second "Can't keep up"; `paste.drain` lines show small `units`/`pending` falling to 0; fly the structure after `pending=0` and compare light with `Defer Template Relight` = false. Fly away before it drains and return: `paste.unloadFlush` appears and no chunk is dark. Stop the server mid-drain and reload: no dark columns | [depths.md](depths.md) |
| Gap recheck pacing + RTG light fix deferral & direct relight (`Performance Stage Timers` = true, `RTG Light Hack Fix` = true (default) for the direct relight, false for the wool baseline) | Generate a High City near and far, and fly over new and saved chunks: no single tick over a few hundred ms after generation (`Can't keep up` quiet); `[AQ-PERF]` shows `light.gap.cols` / `light.gap.skipped` and `rtg.lightFix.calls` (including `fixLightingOnLoad` enqueues), `.repeat` (merged repeats), `.run`, `.drain` small per tick, and `rtg.woolRelight` counting direct `checkLightFor` repairs without `setBlockState` spikes. Walk new terrain, then look at chunk edges, cave mouths and overhangs a minute later and inside the High City: no lasting dark or bright seams, compared with `Pace Chunk Gap Recheck` = false, `Defer RTG Light Fix` = false, and `RTG Light Hack Fix` = false. Fly away before the queue drains and return (`rtg.lightFix.unloadFlush`), stop the server mid-drain and reload: no dark chunks | [depths.md](depths.md), [rtg.md](rtg.md) |
| Non-RTG light fix and `/aqlight` (`Performance Stage Timers` = true) | Walk new Atum terrain (dimension 17) and look under overhangs and canyon walls a few seconds after chunks populate: no black cells next to lit neighbours, without a relog; `[AQ-PERF]` shows `rtg.lightFix.calls` / `.run` tagged `dim=17`. The same walk in the overworld lights a repaired chunk the player already has. `/aqlight fix direct` still does not resend, so a cell that reads right there but stays black is the command's server-only repair; `/aqlight fix wool` still updates the client by itself | [rtg.md](rtg.md) |
| RTG light-fix adaptive drain (`Performance Stage Timers` = true, `RTG Light Hack Fix` left at true) | Walk and turn through new terrain: overhangs and cave mouths ahead settle within a second or two of the chunks populating; chunks behind or far away settle later. `[AQ-PERF]` `rtg.lightFix.latency.t0` stays shorter than `.t2`, and `pending.t0` falls while `pending.t2` can linger. `RTG Light Fix Adaptive Slice` = false returns distance order and the fixed 15 / 30 ms slices | [rtg.md](rtg.md) |
| Dynamic Stealth trims (`Performance Stage Timers` = true) | In a mob-heavy area: `[AQ-PERF]` shows `ds.trackerMemoHit` well above `ds.trackerCanSee` counts; Spark `Sight.canSee` from `DSEntityTrackerEntry` and `World.getChunk` under `ImprovedRayTracing.rayTraceBlocks` drop. Walk around corners and doors with mobs near: entities appear/disappear without noticeable lag (at most 3 ticks); stealth gauge and mob detection behave the same with `Tracker Sight Memo Ticks` = 0 and 3; arcane rifts still render after a dimension change | [dynamicstealth.md](dynamicstealth.md) |
| Bulk Template Relight (`Performance Stage Timers` = true) | Visit a Dungeon Additions High City on new chunks: no multi-second stall; `[AQ-PERF]` shows `paste.template` with `,bulk` and `paste.replay cols=..`; walk the structure interior, edges and chunk borders for dark or bright patches; compare with `Bulk Template Relight` = false if in doubt | [depths.md](depths.md) |
| Biome class cache (`Verify Biome Class Cache` = true) | Fly new chunks across ocean, river, beach, swamp and inland villages: zero `[AQ-VERIFY] biome class cache mismatch` lines; Spark shows `VillageCharmPaste.shouldSkip` well below 3.6 s / 9.7 s | [rtg.md](rtg.md) |
| Village plate snapshot (`Verify Village Snapshot` = true) | Fly new chunks near villages (inland, swamp, river, ocean edge): zero `[AQ-VERIFY] village snapshot mismatch` lines; villages flatten and seal as before | [rtg.md](rtg.md) |
| RC relocation and path link | New chunks near a village with RC loaded: `[AQ-RC] Canceled` followed by `placed` (outside the village) or `Dropped`; no RC structure inside the pad. Roads that ended in grass now continue as a one-wide path to the next road or well | [rtg.md](rtg.md) |
| Village bridges (new chunks, river or beach village) | Every bridge deck meets a path at the same Y on both banks (no dip); a bridge starts on a pad and lands on a pad that has a structure, never open water, a bare bank or a road-only plate; a dropped wet road logs `bridge drop reason=` in `villagepatch.log` | [rtg.md](rtg.md) |
| Populate relight (new overworld chunks) | Spark profile: `relightBlock` inside `doPopulate` / `placeLeavesBlock` and the opening `Chunk.checkLight` collapse; `[AQ-PERF]` `populate.recorded`, `populate.queued`, `populate.replay`, `populate.heightReplay`, `populate.latency`, `populate.drain` show the work moved to the drain. In game: no dark canopy or dark emitters once a chunk has arrived, snow on top of a cold forest canopy, no long-held chunks at the view edge | [depths.md](depths.md) |
| Upper tunnel noise skip (`Verify Upper Tunnel Noise` = true) | Fly new chunks over several seeds: zero `[AQ-VERIFY] UpperTunnelNetwork mismatch` lines; Spark `UpperTunnelNetwork.forColumn` drops from 3.9 s / 5.8 s | [depths.md](depths.md) |
| RTG relight probe (`Performance Stage Timers` = true) | `[AQ-PERF]` lines include `rtg.fixLightingAround` and `rtg.woolRelight=0.0ms/N`; N per populate decides Phase 3.2 | [rtg.md](rtg.md) |
| `VillageWellCache` (`Performance Stage Timers` = true, `Verify Village Well Verdict Memo` = true) | Fly new chunks across coastal and inland villages and run `/aqvillage`: zero `[AQ-RTG] VillageWellCache` mismatch warnings in `latest.log`; `[AQ-PERF]` shows `village.wellReject` and `village.findDryWell` with high `memoHit=` counts; Spark shows `VillageLandHelper.chunkExists` / `RegionFileCache` dramatically reduced | [rtg.md](rtg.md) |
| Coastal village ocean face (new chunks) | Cleaner XZ outline (open 1-block sea inlets filled). Stone brick **only** where the plate cliffs ≥2 (including top); sand/grass on level rims; no open-water pier; inland hill cliffs not bricked | rtg |
| Village interior (new chunks) | No stone brick patches in yards/farms or along chunk lines; no pad across a Land of Lakes / forest watercourse; no bare plate spur without a building | rtg |
| Village path across a river (new chunks) | Stone brick deck at plate Y, cobble wall rails, piers on runs >4; water under it; no oak planks at water level. `villagepatch.log`: `bridge clip=` lines. Ocean paths still omitted | rtg |
| Village small shrine / waystone | Complete shrine on the plate (not half over a pond). No dirt/grass collar. F3 River/ocean columns still skipped | rtg |
| River through forest/shrubland (new chunks) | No well in the channel; no house/RC/plank dock on F3 River; land plate inland | rtg |
| RC church/inn next to river or ocean (new chunks) | Building on the plate inland; water unplated. `villagepatch.log`: `rc aabb wet` then retry hit or omit | rtg |
| Large Astral temple (new chunks) | Raw marble under the pad, not dirt; no floating logs/leaves in or above the AABB | rtg |
| Cambion house on a slope (new chunks) | Pad flush with plains; cobble on the grass; door +1 above cobble; holes under footprint filled; no mesa; village overlap still cancels | rtg |
| Mystical barrow or hut next to a village | Relocates (step 8, up to 32) or skips; barrow not plated | rtg |
| Bewitchment circle / menhir / wickerman next to a village | Relocates or skips; no extra plate | rtg |
| Dungeon Additions structure overlapping a village (new chunks) | Cancelled (no castle/tower/outpost inside the village); still generates away from villages and in Nether/End/Aether | rtg |
| Desert village (new chunks) | One sand plate through yards; no toothed red-sand holes; no BOP quicksand in land boxes | rtg |
| Village pad water pond (new chunks) | No 6–16 block water lake through yards or house foundations. Ponds still generate off the 12-pad. Lava lakes still generate | rtg |
| Inland plains (example `-2897, 97, -2119`) | Flat plate, houses on it, blend to hills | rtg |
| Sea-level forest (`-524, 64, 5893`) | Path, lamps, houses **same Y** | rtg |
| Beach ~16 from water | Village may start; buildings inland; **no** sand piers or plank bridges | rtg |
| Coral reef / kelp / ocean well (`-3452, 63, -2191`) | No village start unless a dry slot exists in retry range (`veto` `ocean_well` / `coast_ocean`) | rtg |
| Approach a new village from unloaded chunks | No `ConcurrentModificationException` in `StructureStart.generateStructure`. Relog not required. Well not sitting on ocean/river | rtg |
| Stamina after G2 (sprint / climb / grapple) | Same costs and cancel behavior as before Comfort remap; no reflection on the tick path | stamina |
| River well | Walks inland (`well-walk`); no plank dock; plate at land Y not riverbed | rtg |
| Dry plains well below Y 64 | Kept; `plateSample … target=64`; not `flooded_well` | rtg |
| Flooded plains well | Raised to min well height if not never-raise; `/locate Village` can find it | [villagegen_info.md](villagegen_info.md) |
| Dry plains village | RC paste Y ≈ plate Y; well chunk `pad>0` | villagegen_info |
| House/RC on a plains lake edge | Omitted, **or** dirt pad for the **12-block** footprint (lake beyond the pad stays water) | rtg |
| `/aqvillage` (OP) | Relog Overworld, run **before** new chunks: feet on ground ~6 off the well (`unexplored` or `known`), not inside a hill, not `No village generator on this world`. Nether still errors (INFO line in `latest.log`). Non-OP denied. | rtg |
| Village `AQTSmallShrine` (new villages) | Uncommon (weight 5). Fly several towns until one appears; then complete marble, no dirt collar, at most one. Missing shrine in some villages is OK. | rtg |
| Pad top | Native RTG surface (sand/grass). `biomesoplenty:mud` → loamy `grass:2` | rtg |
| New inland `/locate Village` | Houses and roads present; `villagepatch.log` `landBoxes` ≫ 1. Already-visited ghost wells stay empty | rtg |
| Water pieces | No waystone/house in a lake; a wet path slides along its own line or is omitted; no oak plank path over leftover open water; **no** roads/houses in F3 River or ocean | rtg |
| Swamp village plate | Well on grass, not over a ravine; roads level with houses; overlapping pads; `seal chunk=` in debug. A flooded path next to the well or a house stays on that line (`mostly_wet` under 0.50, no sideways `path retry hit`) | rtg |
| Wet path on a new meadow or grassland village | `village layout` road hits are `back` only (`lat` and `spiral` are 0). At most 3 path hits. A wet path with no dry column on its line logs `path retry miss` and is absent. No second path four blocks beside another, and no road several dozen blocks from the town | rtg |
| BOP coniferous, brushland, or steppe village (new chunks) | Road columns are gravel where the surface was BOP grass or dirt. Vanilla-grass towns still get grass path. `road-gravel` in `villagepatch.log` when debug is on | rtg |
| Coast village whose original well column is water (new chunks) | Buildings sit around the dry well, not on one side of a shifted town. Log shows `well-before`, not `well-walk chunk=` | rtg |
| RC natural structure on a new village plot | Structure does not paste. `latest.log` has `[AQ-RC] Canceled overlapping natural structure`. RC buildings that are village pieces still generate | rtg |
| Hill village (new chunks) | All pieces on **one** well Y; no chunk-border stone wall; unused AABB corners stay hills | rtg |
| Under a house at plate-4 | `isInsideStructure("Village")` true; below well floor false | rtg |
| Yard between path and house (12-pad / Hermite) | `isInsideStructure("Village")` true after new gen and after relog. Outside pad+falloff false. Plate+31 false if box height is 30 | rtg |
| Night village lamps (new chunks) | Path torch and fence lamp light the plate and nearby house walls; not a 1-block puddle | rtg |
| Below Y0 Overworld | Deepslate fill, AQ caves, Y0 mouths on land, **no** ocean drain | [depths.md](depths.md) |
| -Y caves after a perf change (new chunks) | Same seed, same chunks: tunnels, chambers, pillars, bridges, stalactites and floater cleanup unchanged. Perf work is exact-equivalence, so any visible difference is a bug | depths |
| Spark while flying new terrain | `UpperTunnelNetwork.forColumn`, `columnStrength`, `getSurfaceAltitudeForColumn` and chunk/BC `Reflect` getBlock/getMaterial well down; Tweaks no longer ~half of chunk gen. Same-seed bridges in the same places | depths |
| Fog / sky below Y0 | Dark fog ~32–52; no skybox | depths |
| Deep cave floor/ceiling (new chunks, Quark on) | Tapered Quark stone speleothems mixed with existing Deepslate columns/spikes/bridges. +Y Better Caves still have stock Quark clusters. Quark jar removed or cfg off: no deep speleothems; cave shape unchanged | depths |
| Deep cave pillars (new chunks) | Denser 20-block-cell pillars (~1.44× vs old 24). Same-seed **bridges** match the previous build; pillar sites shift. Old chunks stay sparse | depths |
| New `/locate Mineshaft` | Unexplored pin has tunnels; `isInsideStructure` true at the pin. Old generated-empty pin is skipped. New terrain rarer than every-land 0.003 (Tweaks 0.001 + spacing 4); hill openings still happen | [bettermineshafts.md](bettermineshafts.md) |

Log snippets if debug on: `veto chunk=`, `forget chunk=`, `flatten chunk=`, `seal chunk=`, `waystone relocate`, `village piece skip water floor charm`, `astral small shrine village piece`.

## Stamina

Use the full list in [stamina.md](stamina.md) **Verify**. Minimum: jump costs/blocks, melee hit spend, bow draw, climb slide when empty, grapple hang vs climb vs grounded, HUD when dodge locked. Temperature feather drain (the temperature model itself is [temperature.md](temperature.md)): while hypothermic or hyperthermic, no periodic thermia hearts, gold feathers and the Feathers potion stay off, the feather cap falls about one half-feather every 5 seconds, holds the last half-feather for one ramp, then drops to 0. At that step the death message is that Simple Difficulty source (hyperthermia, not dehydration). After the potion ends the cap returns one half-feather every 10 ticks. Hyperthermia also drops about one thirst point per 10 seconds. Slowness ramps I → III in the cold and clears when hypothermia ends. With armor + toughness + thirst: toughness **left-to-right** one row above **armor** (left); feathers above **thirst** (right), not overlapping. Hover worn armor in the Baubles screen and in the survival inventory: weight icons show, no `ConcurrentModificationException` from `TooltipEventListener.getWeight`. Iron pick tooltip: `Vanilla Tools` + harvest stars + durability + efficiency. Metallurgy pick: no duplicate Tweaks harvest line. See [client.md](client.md).

## Temperature

Full spec: [temperature.md](temperature.md). The hypothermia / hyperthermia **feather** drain is under [Stamina](#stamina) above.

- [ ] **Surface house at night, cold biome, note the season.** Thermometer outside vs inside. Inside moves halfway from the outside value toward canceling biome, time, altitude, and the current season. A heater or campfire then adds its full block bonus on top. Verify the **delta**, not an absolute number.
- [ ] **Desert noon.** Daytime shade (up to 6) is already gone under the roof before the pull. Inside is lower than outside, and a chiller still applies in full.
- [ ] **Glass roof.** A room with a plain glass or stained-glass ceiling insulates toward 12 exactly like an opaque roof, even though `canSeeSky` is true under glass. Glass panes as walls pass on collision alone.
- [ ] **Greenhouse glass.** A greenhouse of `sereneseasons:greenhouse_glass` insulates toward 14 — warmer than a plain house in winter, less cooled in summer, delta about **+1** at the default factor 0.5. Raise the ceiling above `greenhouseGlassMaxHeight` and warmth stops at the same height Serene Seasons stops fertilizing crops. Put an opaque layer between the player and the glass inside the band and it still reads as a greenhouse. At factor 1.0 plus a campfire the room can reach `HOT`.
- [ ] **Serene Seasons absent.** Pull the SS jar from the instance and boot. No crash at `DynamicModifierInsulation` registration, no `NoClassDefFoundError`, greenhouse handling silently off, standard insulation still working.
- [ ] **Enclosure.** Opaque or glass room with one air-gap side: loose mode (strictness 1) insulates, strict mode (2) does not. An open door in that side still counts as a wall. Under an oak or a cliff overhang: loose mode off (rays exhaust unwalled). Ice ceiling: insulates via the opaque branch.
- [ ] **Y 50-63 house.** Sheltered room with the feet position at Y 62 (swamp, or a floor dug one block down). Insulation still applies, scaled by whatever Simple Difficulty's own `(y-50)/14` already took off - **not zero**.
- [ ] **Below Y 50.** Biome, time, and season are already 0 from Simple Difficulty's scale, so the pull is a visible no-op. Spelunker's capability pull is unchanged and does not interact.
- [ ] **Thermometer frame cost.** Hold a thermometer and watch F3 frame time inside an opaque house (hits the `canSeeSky` short-circuit), under a glass roof (pays the upward scan), and outdoors. Repeat with several item-frame thermometers in view. All three stay flat.
- [ ] **Chunk edge.** Stand in a sheltered room within 5 blocks of an unloaded chunk border, and under a glass roof near one. No chunk-load stall and no new chunks generated by standing still.
- [ ] **Single-player, thermometer in hand, long session.** No `ConcurrentModificationException` or hang from the shelter memo. The modifier is one shared instance across the client and integrated-server threads; the two `isRemote`-selected memos are what keep that safe.
- [ ] **Boat.** Clear weather on the ocean: no wet -6. Rain on an open boat: wet applies. Submerged boat (block above is water): wet applies. Boat in a one-high water channel under stone: no lift, position stays in the water. Leave the boat and swim: wet applies. A horse in water stays wet.

## Other modules (one-line)

| Module | Smoke |
| --- | --- |
| Gaia | Melee: one physical hit, names the mob, **no MAGIC 6** (diamond must not take a flat ~3 hearts of magic). Unarmored may exceed JSON if the mob holds a sword. Hard archer: no MAGIC tip. Bolts: armor skip, Magic Protection works, death names shooter. Bomb: armor + Blast Protection, no extra 2.0, facing shield zeroes. Log: no `mixins.aqtweaks.gaia.json` injection failure. JSON `grimoireofgaia:orc` changes orc melee and bolts after restart. Default JSON HP/armor: orc 30/4, dwarf 60/8, feral goblin 15/4, sporeling 15/2. Deep Dwarf: `/summon aqtweaks:deep_dwarf` hostile, purple face, white/gray hair and beard, red eyes, stock gear; JSON attack 10 / HP 60 / armor 8. Deathword: ranged Gaia magic (no melee), piercing like orc bolt, wither +30 ticks stacked, summons/beacon stay. |
| Thaumcraft | First Nether visit warps after ~2s (+5 sticky +5 temp); sleep at dawn reduces **sticky** only (no extra temp); whispers ~every 5 min while exposed (30s silent score ticks). Warp Ward pauses exposure banks. Focus pouch in a BaublesEX slot ≥ 4: cycle focus onto the gauntlet, focus is not deleted, no `NoSuchFieldError` / `ClassCastException` from `fetchFocusFromPouch`. Log: no `mixins.aqtweaks.thaumcraft.json` injection failure. Fire/frost foci `isMagic=true classified=true`; Heal on self scales with Magic; snowball does not. Runic gear (overhaul off): red hearts plus runes, extra rows on empty sockets past 10, armor above those rows; gold hearts only for absorption above the gear cap. No runic gear: golden apple stays gold hearts. Mundane, apprentice, and fancy rings do not paint a second player skin; mundane and fancy amulets still render |
| Bewitchment | Listed ritual **finish** grants warp; halt does not. CraftTweaker `mods.bewitchment.SpinningWheel.addRecipe` / `removeRecipe` updates JEI and the wheel; unnamed ids are `crafttweaker:<name>` |
| Comfort | Rested I in a scored room with hearth, bed, or seat, while healthy. Insight, Vigor and Fortitude match the tier. Rested stays after leaving, a hit, or an attack, and milk does not clear it. Clarity shows only while resting and only with Thaumcraft; warp still adds 2/3/6 per 30s scan and fires at 12. Hurt amount above 0 or an attack blocks re-entry for 30s / 15s. `/aqcomfort` prints the breakdown; `reload` and `families` are OP. Hot spring still applies cold resist. A saved settings file keeps its old 15/40/60 until those keys are edited; a new file uses 24/48/66 |
| Portal | Arcane Tunnel binds on air or block then opens a 60s two-way rift (cross-dim if bound elsewhere); land at dest rift XYZ after ~3s gate; Unstable Arcane Tunnel lands ~4000–6000 same dim; villagers/mobs in the box teleport; sitting pet stays; lead follows. Dark cave mid-cell lights like glowstone. Sit in an open rift: no `updateClouds` CME. Do not DS full-bypass the rift (blanks the cylinder). See [portal.md](portal.md) |
| Client | Toughness LTR above armor; iron pick shows Vanilla Tools stats; Metallurgy pick not duplicated |
| MineMenu | Reskillable, BetterQuesting, and Hwyla config: look frozen while open. Close from a corner: cursor grabbed at center, no yaw snap, next look starts from center. Inventory, Baubles, and Esc the same. MineMenu Baubles entry opens the expanded GUI once; the real Baubles key still once and does not crash. DSS Skills GUI once from MineMenu with the key unbound or assigned; space does not open it while unbound; the real key opens once and does not crash; `/dssgui` opens it. Boot: no `Minecraft was loaded too early` / `EntityRenderer was loaded too early`. See [minemenu.md](minemenu.md), [stamina.md](stamina.md) |
| Recipes | Pack boots without Metallurgy `generated/item/spartanweaponry` recipe spam or `Parsing error loading recipe` stacks for the 40 known-missing items and `draugr_ingot_from_block`; one-shot skip lines logged per missing item ID; new/unexpected missing items still dump |
| Reskillable | Attack 16 → +2 damage; Mining Expert wood pick drops diamond ore; stamina perks on tree; Magic school mutex holds in both directions after the LOWEST restamp; Tunnel Sense glow lasts 5 seconds; Fortify shield stamina is 0. Full list: [reskillable.md](reskillable.md) |
| Spawning | Boot log loads spawn types, structure spawns, parties, and pack group sizes. First ticks: no `parseStructureData is not cancellable`. Closed cave: dwarf/cave_spider/krake yes, Dryad/witch/Wildkin no, zombie/goblin still yes. Mineshaft non-origin chunk: witch/illager/pillager can appear; ordinary cave still no. Night surface: reverse exclusives; zombie still yes. Creeper packs 1–2 not 4; enderman 1; zombie/skeleton/spider 2–4 mixed. Default `goblin_feral=3-5`. Fill Pack Size off: old singles. Natural Overworld cleric: knights + CR archers; cage/portal cleric: no party. Failed cultist/blaze cage Delay ≈ 20 (cfg) not 0 and not 200–800; zombie cage still attempts after 1→0; success still 200–800. Hostile cap default 200. No mixin fail on `MixinWorldEntitySpawner`, `MixinMobSpawnerBaseLogic`, or `MixinStructureCache`. See [spawning.md](spawning.md) |
| Advancement | Join log: no `AddonHandler.onWorldLoad` → `ForgeHooks.loadAdvancements`. Mixin json applied. Animania animals still spawn/register. No Farm/Extra Animania advancement trees. See [advancement.md](advancement.md) |
| Better Mineshafts | `/locate Mineshaft` TPs to tunnels; new chunks use `aqtweaks_bettermineshafts.cfg` rate/spacing; log: no `mixins.aqtweaks.bettermineshafts.json` `setBoundingBox` / `func_75072_c` apply failure. See [bettermineshafts.md](bettermineshafts.md) |
| Stats Keeper | 10 hearts: elixir consumes, +1 heart, drink sound from `aqtweaks_statskeeper.cfg` (default level-up; empty = silent). 20 hearts: drink cancelled, stack remains, red action bar `Your vitality is already at its peak!`, no drink sound. Baubles/buffs above 20 hearts with unused SK additional still drink. See [statskeeper.md](statskeeper.md) |
| Twilight Forest | New Overworld→TF RandomPortals trip into a locked/hazard column lands in a safe biome **on grass** (frame bottom **on** grass, not sunk), not a landmark, not tree canopy. Return through the same sending portal stays linked. Nether RP unchanged. Rift items unchanged. Carminite ritual: incomplete Octine shell falls through to a normal reactor; missing `adamantine` refuses only on a full shell and keeps Salis Mundus; creative spends nothing; breaking one Octine mid-charge fizzles and leaves the rest; the climax does not hurt the player or neighboring blocks; drop is 5–7 `contenttweaker:phlogiston_matrix`. See [twilightforest.md](twilightforest.md) |
| Aether | New Overworld→Aether RandomPortals trip into open sky lands on island grass/dirt/holystone, not a floating Y≥70 frame. Return through the same sending portal stays linked. TF and Nether RP unchanged. See [aether.md](aether.md) |
| Game Stages | No `apprentice_builder`: chiseling a staged output (GUI and in-world) fails, red action bar, input remains. After the stage is granted, that tier chisels; higher builder tiers stay locked. Unstaged variants and crafting-table Recipe Stages unchanged. See [gamestages.md](gamestages.md) |
| Quality Tools | Crafted sword has no Quality tag. Loot chest stamps before pickup (`dark_gray` ~25% remaining, `gray` ~50%). Dawnstone: tool then rune; same-tier reroll changes Name when another same-color exists; wrong rung action bar, rune kept. Break without Salvage drops Broken with one use left; the next break destroys it. Repair 26–51% turns Broken into `gray`; past 51% restores the saved quality. Salvage uses Charm. See [qualitytools.md](qualitytools.md) |
| Simple Tomb | Death preserves main inventory, hotbar, armor, offhand, and BaublesEX slot positions. Retrieving tomb restores items to matching slots. Occupied slots displace existing item non-destructively to first available main inventory slot or player feet if inventory full. Overworld saves rolling backup to `aqtweaks_death_backups.dat`. OP command `/aqtomb list <player>` and `/aqtomb recover <player> [backupIndex] [targetPlayer]` restores backup non-destructively. See [simpletomb.md](simpletomb.md) |

## Edge cases

| Check | Expect | Doc |
| --- | --- | --- |
| Empty-stamina melee against a **mob** | Short-stamina hit on a zombie takes the reduced multiplier once, same as a hit on a player, then the attacker tag clears. Mobs are not exempt | [stamina.md](stamina.md) |
| Two players join on the same tick | Each ends with weight from their **own** armor. Neither is left on an emptied weight array or the other player's value | stamina |
| Cross-dimension rift trip that fails | Entity stays in the origin dimension. No teleport loop, no entity stuck in the AABB re-firing every tick, no ghost copy at the destination | [portal.md](portal.md) |
| Expert Climber with the perk unlocked, low feathers | Server permits the climb and the client does **not** slide. No rubber-band between a client-side `motionY = -0.15` and the server position | stamina, [reskillable.md](reskillable.md) |
| World A → title screen → world B on a different seed | No village plate heights carried over from A. `getRawLight` is back on its no-rift fast path (no lit cells at B's spawn) | [rtg.md](rtg.md), portal |
| Leave world / stop integrated server | No `NoClassDefFoundError: VillagePlate`. Log has no `EventSubscriptionTransformer` AIOOBE on Tweaks classes | rtg |
| Evasion perk | Purchasable at agility 16. No `trait\|elenaidodge2:dodge`. Boot may log that that row was dropped | [reskillable.md](reskillable.md) |
| Thermometer in an item frame indoors | Reads the insulated value, same as the player's own readout. Frame rate does not drop with several frames in view (shelter memo) | [temperature.md](temperature.md) |
| Greenhouse roof taller than Serene Seasons' `greenhouse_glass_max_height` | Warmth and crop fertility stop at the **same** height. Tweaks' `greenhouseGlassMaxHeight` must equal SS's key | [temperature.md](temperature.md) |
| Boat in a one-high water channel under stone | Sample stays in the water (wet applies). It is **not** lifted into the ceiling, and the player is not treated as sheltered | [temperature.md](temperature.md) |
| Serene Seasons removed from the instance | No `NoClassDefFoundError` from `DynamicModifierInsulation`. Greenhouse handling off, standard insulation unaffected | [temperature.md](temperature.md) |
| `minWorldY` | Pinned at **-64**. No cfg knob to change it; bedrock floor, CoFH `Math.max` floor, and `RayMatcher.cast` all read that constant | [depths.md](depths.md) |

## Atum storm warning

| Check | Expect | Doc |
| --- | --- | --- |
| Wait for a natural sandstorm outdoors (short clear interval in `aqtweaks_atum.cfg` helps) | About 30 s before the storm the dust flecks speed up and thicken, then the storm ramps on from that level with no jump; no fog, sound or damage change | [client.md](client.md) |
| Join, or enter Atum from a portal, 10 s before a storm | The warning picks up the remainder at once; the storm starts when the server's does | client |
| `/weather clear` in Atum | The warning never sits at full | client |
| `Enable Storm Warning` false | Behaviour as before; `Warning Resync Seconds` 0 keeps only the join sync | client |

## Twilight Forest Ley-Rupture

| Check | Expect | Doc |
| --- | --- | --- |
| `/leyrupture surge` | 30 s of warning: crickets and ambient animals go silent, a hum rises, in the last 10 s violet and emerald glyphs rise, zigzag and fade after about 2 s over a wide area (none on top of you); then 25 to 30 s of scour every 4 s (about 2 s grace) with a heavy eruption of glyphs, sparks and plumes in violet (70 percent) and emerald (30 percent) across a 48 block radius, plumes at least 8 blocks away, thinning when sheltered; hills do not thin the far field | [twilightforest.md](twilightforest.md) |
| Forest floor in diamond armor / full Steeleaf or Thaumium robes | 4.0 / 1.5 magic damage per hit, hunger burn, violet-emerald edge static | twilightforest |
| Climb a tree; stand on a leaf block; go underground | The effect shrinks steadily and stops about 14 blocks above the ground; a leaf counts the ground below; caves are full effect | twilightforest |
| Inside a Lich Tower, Labyrinth, Knight Stronghold; just outside | Damage stops inside, resumes just outside; hills, hedge maze, naga courtyard are not shelter | twilightforest |
| Use a Thaumcraft gauntlet, Botania rod, Astral wand, Aether staff, Twilight scepter, mount a Bewitchment broom while exposed | 3.0 recoil scaled by exposure, sparks and hiss, once per cooldown; a caster adds capped warp; `/leyrupture item` reports the rule | twilightforest |
| Carry an Embers cartridge and a Botania mana tablet in the open | About 10% charge per 5 s, scour damage taken, temporary warp up to the cap | twilightforest |
| Place a Vis Battery in the open before a surge | Its charge rises about 1 per 5 s during the active phase, with flux added; not under a roof, in a structure or high in the canopy | twilightforest |
| Active phase | A few wraiths or wolves surface 12 to 24 blocks away, glowing, no drops, and dissipate at the end | twilightforest |
| `/leyrupture biome` in the Dark Forest, Fire Swamp, Highlands, a Clearing | Intensity and extra effect differ as configured | twilightforest |

## Nether hazards

| Check | Expect | Doc |
| --- | --- | --- |
| `/netherhazard spout` near lava | A 6 s tremor (rumble, bubbling lava, faint shake), then 3 to 6 eruptions 2 to 5 s apart, each with 3 s of bubbling first; a splash patch (centre level 4, gradient to about 3 spaces, no source) that is gone after 20 s, also after a chunk unload and a restart | [nether.md](nether.md) |
| Fire Resistance or a full Salamander set in a column | No damage, no ignition | nether |
| `/netherhazard ash` | A cloud grows around you, drifts, flows through a doorway and around pillars but not through a sealed room; dose gives Slowness I / Wither I-III; leaving fades it; milk clears it; Wither Ward slows the dose to a quarter | nether |
| `/netherhazard status`, `clear` | Cloud and thrown-lava counts; clear removes clouds and lava | nether |
| Several ash clouds at once (`Max Ash Clouds` 8) | Clouds still grow, fade, veer and drift without popping; the ash tick stays small in spark | nether |
| Overworld, Atum, Aurorian | Unaffected | nether |

## Aether Silver Squalls

| Check | Expect | Doc |
| --- | --- | --- |
| `/aethersquall gust` | A 5 s warning (streaming wisps, rising wind), then a 2 to 5 s gust that pushes you, heading shifting about every 2.5 s; Shift cuts the push to 25%; gliding and creative flight are pushed too | [aether.md](aether.md) |
| `/aethersquall lightning` outdoors in iron or Zanite | A spark ring and column at your feet for about 1.75 s, then a bolt: 6 damage if you stay, none if you leave the 2.5 block ring; no fires | aether |
| Full Valkyrie/Phoenix/Obsidian/Neptune set | Bolt lands on nearby ground; 2 damage if caught; push reduced | aether |
| Under a roof | No push, no strike | aether |
| Open Glider, Aether parachute, grappling hook in a gust | Work as without a gust | aether |

## Moonfrost visibility and shelter

| Check | Expect | Doc |
| --- | --- | --- |
| `/aurorianweather moonfrost`, watch the 30 s warning (`Hazard Debug` on) | Snow and wind rise with no fog; the debug line shows the warning rising; the storm continues with no dip | [aurorian.md](aurorian.md) |
| Full storm | Heavy snow streaking steeply down with the wind in seven visibly different sizes and kinds (specks, flakes, pellets, soft puffs, rare chunky clumps, streaks), pulsing with gusts; a dense veil about 10 blocks away in every direction, including above and behind; no fog anywhere | aurorian |
| Walk under a tree | No change in snow or damage (the canopy is not a roof for particles, shelter or the hazards) | aurorian |
| Enter a stone-roofed room | Effects ease out and damage stops after about 2 s; the snow fades over about a second with no jump; snow shows through openings; no flake is ever visible under a roof or below a thin ceiling; a wall upwind in open sky also stops the hazards | aurorian |

## Portable Vis Battery

| Check | Expect | Doc |
| --- | --- | --- |
| Set a battery to charge 7 (`/setblock ... thaumcraft:vis_battery 7`), break it | The dropped item has damage 7; placing it gives charge 7; middle-click picks a charge-7 stack | [thaumcraft.md](thaumcraft.md) |
| Two stacks of different charge | They do not merge; equal charges do; tooltip `Stored vis: 7/10`, empty shows none | thaumcraft |
| Explosion, Silk Touch | Charge kept | thaumcraft |
| `Portable Vis Battery Charge` false | Break drops an empty battery; a stack that already has damage still places that charge | thaumcraft |
| Crafting recipe, JEI, creative tab | Empty battery crafts and shows; a full one appears with `Creative Charged Batteries` | thaumcraft |

## Hazard timeline

| Check | Expect | Doc |
| --- | --- | --- |
| `/aurorianweather moonfrost` in the Aurorian | The 30 s warning plays (fog closing, wind rising), then the storm ramps in; with `skipWarning` it starts at once | [hazards.md](hazards.md) |
| `/aurorianweather clear` | An active storm ramps down; an upcoming one is cancelled; `status` shows the event | hazards |
| Two clients, one joining 10 s into a warning | Same warning start and storm start; the late joiner picks up the remainder | hazards |
| `/time set` day and night | The event does not move | hazards |
| Server restart mid-gap | The upcoming storm is still there; an event that has passed is not replayed | hazards |

## Aurorian

| Check | Expect | Doc |
| --- | --- | --- |
| Lighting (existing world) | Outdoors F3 sky light 9; `/time set day` and `night` and many `/time add 1000` do not change the screen brightness or the moon (full, overhead); crops and saplings grow in the open; no outdoor hostile spawns on open ground | [aurorian.md](aurorian.md) |
| Lighting off (`Enable Fixed Lighting` false) | Vanilla Aurorian behavior returns, including the time-driven brightness swing | aurorian |
| `/aurorianweather moonfrost` | Wind rises, moon snow falls, storm ramps over 30 s; `status` agrees; `clear` ends it with the fade-out | aurorian |
| Open ground, no armor | Frostbite 6.0 every 4 s, rime builds while standing still, stamina regeneration about 20x slower, wind pushes (sneaking anchors) | aurorian |
| Full iron armor / full Aurorian, Arctic or Yeti set / magic armors / Cold Resistance potion | Damage 4.0 / 2.0 / 2.0 / 2.0, rime slower, regeneration 2x / 1.25x; Atum armor counts as ordinary armor | aurorian |
| Standing still | Slowness I at rime 5, Slowness III and Mining Fatigue I at 10, Slowness V at 15; walking thaws it even under Slowness V | aurorian |
| Roof, cave, upwind wall, lit campfire, Comfort room | Hazards stop and rime melts; regeneration resumes within about a second (client clamp) | aurorian |
| `/time set` during a storm; two clients | The storm does not move; both see the same state | aurorian |
| Relog or death mid-storm | No stuck regeneration delta or rime; no disconnect when the regeneration state changes | aurorian |
| Overworld, Nether, Atum | Unchanged (Atum shelter now calls `StormShelter`; behaviour the same) | aurorian |
| Snow accumulation: stand in open ground in a full storm for a few minutes | Snow layers appear across the whole loaded area (to your view distance), rising gradually, faster mid-storm; none under a roof, a canopy or beside a torch; crops, water and tall grass are left alone; two players close together do not double it | aurorian |
| Smooth snow | Any two orthogonally adjacent snow columns differ by at most 2 layers; dig a hole and it fills first | aurorian |
| Wind shadow | Behind a wall or tree line the ground stays thinner for a stretch downwind; `Snow Wind Shadow` off gives an even rate | aurorian |
| Depth | `Max Snow Layers` 16 stacks a second block on a full one and stops at 16; 8 stops at one block; later storms add nothing to full columns | aurorian |
| Burial | Stand still in a full storm: Slowness II once the snow reaches your chest, the air bar drains once your head is under and you lose health but stop at 1 heart; digging with a hand or shovel frees you; creative and spectator are unaffected; `Enable Snow Burial` off does nothing | aurorian |
| After the storm | The snow stays; a torch beside a layer clears it with no drop; a shovel drops snowballs as vanilla; quit and reload changes nothing | aurorian |

## Void Breach ritual

Needs Twilight Forest, Thaumcraft, Thaumic Augmentation and Game Stages. Creative world first. Design: [twilightforest.md](twilightforest.md) (Void Breach).

1. An incomplete casing, or stone around the reactor, with Salis Mundus in hand does not cancel the click and a ready vanilla reactor arms normally.
2. Six `thebetweenlands:octine_block` around the reactor still runs the phlogiston ritual unchanged.
3. Six Void Metal blocks, no `voidmastery`: purple and gray refusal, Salis Mundus kept, the ritual does not start.
4. `Casing Block` and `Cap Block` changes work (new shell works, old one no longer does); an Octine or air value logs one warning and uses Void Metal.
5. With `voidmastery`, survival: one Salis Mundus spent, a 10-second charge (cave rumble, low portal roar, inward purple motes). Breaking a casing block at tick 100 fizzles with no fracture and the remaining blocks stay.
6. It works away from the Dark Tower (Overworld, and another dimension). It is refused inside The Emptiness and when TA's `DisableEmptinessDimension` is true, with nothing consumed.
7. Climax: the reactor and casing vanish, the player takes no blast damage, a fracture opens at the reactor and one opens on standable ground beside the Eldritch Spire; walking through works both ways.
8. The Fracture Locator points at the new fractures.
9. Eldritch Guardians spawn around the fractures as they do around TA's natural ones.
10. A second ritual near the same spire does not stack a fracture on the first.
11. Forced failure (for example the Emptiness cannot load): the Salis Mundus drops at the reactor and the shell stays.
12. A server restart during the charge forgets the ritual: the shell stays, the Salis Mundus is spent.
| Start a Void Breach or phlogiston charge, quit to title, load another world | No stale ritual, sound or drop in the new world | twilightforest |

## Infinity Blade

Creative world. Design: [weapon.md](weapon.md).

1. Startup log has no ghost-item warning for `infinity_sword` (the old scripts are deleted) and no error for `aqtweaks:infinity_blade`.
2. `/give @p aqtweaks:infinity_blade`: the animated texture cycles in hand and inventory (not the missing-model texture).
3. The tooltip shows 18 attack damage and 1.6 attack speed. Change `Attack Damage` in `aqtweaks_weapon.cfg` and re-equip: the number changes.
4. Durability reads out of 6000; changing `Durability` applies. Anvil repair works with the configured `Repair Item`; `none` disables it.
5. `Enable Default Recipe` false (default): no crafting-grid recipe appears in JEI. True (restart): 8 Nether Stars around a Diamond Sword craft it.
6. Rarity is Epic by default; add `aqtweaks:infinity_blade=LEGENDARY` to `Custom Item Rarities` in `aqtweaks_rarity.cfg` for Legendary gold.
7. Stamina treats it as a medium weapon (swing cost like other swords).

## Rarity overhaul

Creative world with Quality Tools, Metallurgy items and JEI. Design: [rarity.md](rarity.md).

1. Startup log has no `AQTweaks-Rarity` error. If it says it could not add the rarities or recolor, Legendary and Mythic show as Epic or vanilla colors stay; report it.
2. Tooltips: a native Uncommon item is green, Rare dark aqua, Epic light purple. The same colors show in the JEI hover tooltip. The hotbar name popup stays white.
3. An iron sword enchanted with Sharpness only is Uncommon (green); with Mending it is Rare (dark aqua); with a Very Rare enchantment it is Epic. A curse-only item keeps its vanilla tier. An enchanted book keeps its own rating.
4. A genuinely Rare, Epic or Legendary item with any enchantment keeps its tier (for example an Epic item with Sharpness stays Epic).
5. Quality Tools: reforge until a gold quality (Masterwork, Vorpal) appears: Legendary gold. Green or yellow quality: Uncommon. dark_aqua quality: Rare. Red, gray or dark_gray quality: tier unchanged. The `Quality:` line keeps its own color.
6. A Common item with a Very Rare enchantment and a gold quality is Legendary (highest trait wins, no stacking).
7. `Custom Item Rarities`: `minecraft:nether_star=LEGENDARY` gives a gold name; `minecraft:dragon_egg=MYTHIC` is a rainbow name in the tooltip and in the JEI hover; a config reload applies without a restart; the override wins even over an enchanted item.
8. `Show Rarity Subtitle` true adds one line under the name at every tier (and respects `Subtitle Visibility` and `Subtitle Style`).
9. A bad color code, enchantment line, quality line or override line logs one warning each and does not crash.
10. A long Mythic item name still wraps correctly with the rainbow.
11. Crafting Rune names look unchanged. `Enable Rarity Overhaul` false restores vanilla colors after a restart.
12. Spark: no visible cost from `RarityOverrides.resolve` with many enchanted items in a chest view.
| `minecraft:golden_apple:1` and a damaged enchanted tool | Rarity of subtype items unchanged; damaged gear does not change tier | rarity |
| Blocking with a shield with Unyielding (Reskillable) | Knockback resistance holds; no per-tick attribute traffic | reskillable |
| Atum sandstorm with goggles or wildcard armor | Same protection as before; stepping behind a wall stops the push at once and the debuffs within half a second | client |

## Mana-Weave infusion

Needs Thaumcraft and Botania. Creative world, then survival checks. Design: [thaumcraft.md](thaumcraft.md) (Mana-Weave).

1. Thaumonomicon, Infusion tab: the "Mana-Weaving" entry sits left of Infusion Enchantment (it needs Theory of Infusion 2); its second page shows the recipe (the chestplate case).
2. Build 8 pedestals. Put a damaged iron chestplate in the middle and on the pedestals `botania:manasteelchest`, a Mana Tablet at 500,000 mana (`/give` then fill, or a pool), a Rune of Mana, Mana Pearl, Mana Diamond, Salis Mundus, two Thaumium Ingots. The matrix accepts. Swap the tablet for one with less mana, then a creative tablet: it does not accept. Swap the chestplate for a Manasteel chestplate as the central item: it does not accept.
3. Feed essentia; the infusion completes; the chestplate keeps its damage and enchants, gains the tooltip line, and the tablet and pedestal items are gone. Check the instability (9) reads as high and note the effect.
4. A bow or shield (no cousin) needs a Manasteel Block instead of the cousin. A Spartan longbow needs `spartanweaponryarcana:longbow_manasteel`. A hoe needs the block.
5. Wear the infused chestplate (damaged) with a charged Mana Tablet in the inventory: durability rises one point per tick and the tablet drains 140 per point. Empty tablet: no repair, no error.
6. Carry an infused pickaxe in the inventory: it repairs at 120 per point from any slot. An infused chestplate in the inventory (not worn) does not repair.
7. Mine with an infused pickaxe and mana in the inventory: durability does not drop and mana falls 60 per block. With no mana the tool wears normally. Same for armor hits (70 per point).
8. Quality Tools: the wear overlay on an infused item does not desync when mana pays for a hit.
9. The Spartan Manasteel items are expected to self-repair; if one does not, report it (it needs a separate fix).
10. `Mana Repair Cost Multiplier` 0.5 halves the drain; `Enable Mana Repair` false after a restart removes the recipe.
## Tartarite Paragon

Build, deploy, then in a creative world with a Tartarite item of each kind (`metallurgy:tartarite_*`) and the metal blocks (`/give` the block named in [paragon.md](paragon.md)).

| Check | Expect | Doc |
| --- | --- | --- |
| JEI shows the recipes | Searching a Tartarite item lists Metallurgy's Paragon and the aqtweaks ones: four blocks plus the item in the plus pattern, the output tooltip showing the trait | [paragon.md](paragon.md) |
| Craft each trait on a sword | Output keeps damage, enchantments and name; tooltip header `Paragon: <metal>` and one effect line | paragon |
| Item with Metallurgy's Paragon, or a boots/other item not in the recipe list | The aqtweaks recipe does not match | paragon |
| Second craft: same trait / other trait | Same trait: no match. Other trait: the item swaps and the old trait's line disappears | paragon |
| Aurorian Steel (enchanted Tartarite pickaxe, sword, helmet, hoe) | Tooltip shows the level `[n/max]`; mining, hitting, taking damage and tilling raise it; at the maximum an enchantment gains a level | paragon |
| Fiery sword; Octine sword | Fiery: target burns 15 s with flame particles. Octine: about a quarter of hits burn for 5 s. Fire-immune targets unaffected | paragon |
| Knightmetal sword on an armored mob / a bare mob; axe on both | Sword: +2 damage on armored only. Axe: +2 on unarmored only | paragon |
| Zanite pickaxe at full durability, then worn below 20 percent left | Mining speed rises in steps (x3 at the last tier); a Zanite sword hits harder as it wears; four Zanite armor pieces raise the armor bar by half a point per tier per piece and drop it when a piece is removed | paragon |
| Gravitite sword | The target rises about a block after the hit; a player target syncs without rubber banding | paragon |
| Gravitite pickaxe / axe / shovel on a harvestable block with air above | The block lifts as a floating block, 4 durability lost; chests, bedrock and a block under another block do nothing | paragon |
| Gravitite helmet, chestplate, leggings and boots together | A jump rises about 5 blocks; with one piece missing or a different trait among them it is a normal jump | paragon |
| `Disabled Traits` = `fiery` and restart | The Fiery recipes no longer match and an existing Fiery item does nothing | paragon |
| Server and client | No errors at startup from the recipe factory; a pack without a trait's mod loads without its recipes | paragon |

## Doc drift check

Run `python scripts/doc_audit.py` (stdlib only, read-only). Expect `OK 0 hard findings`. It checks: every `docs/*.md` is linked from the README and every README link resolves; every class file named in a doc (written with its package path and .java suffix) exists (history and changelog are skipped); every `Mixin*` class and non-mixin class is named in some doc; every `mixins.aqtweaks*.json` appears in the docs and names no missing class; every `Mixin*` class is registered in a json; every `@Config.Name` appears in a doc (category objects and perk layouts excepted); the README `@Mod` dependency string equals the code. The last section, table row names that are not config keys, is informational (many rows are labels). A new config key, class or mixin json without a doc line fails the check: add the line, do not add an exclusion.

## After mixin / parent bumps

Re-read mixin targets ([compatibility-matrix.md](compatibility-matrix.md) upgrade check). RC version bumps: confirm `RayMatcher.cast` `@Overwrite` still matches.
