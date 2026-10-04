# Verification (1.8)

Last updated: 2026-10-04.

Manual release / smoke checklist. **No automated tests.** Harness: CurseForge **Arcana Quest DEVBOX**, remapped `ArcanaQuestTweaks-1.8.jar` in `mods/`. Algorithms and full checklists stay in module docs; this is the pack-level pass/fail.

Worldgen applies to **new chunks only**.

## Build artifact

- [ ] `.\gradlew.bat build` or `.\build_gradle.ps1` succeeds (`verifyReleaseJar`: class major 65, `VillagePlate.class`, mixin json + `mixins.aqtweaks.refmap.json`).
- [ ] Instance `mods/` has `ArcanaQuestTweaks-1.8.jar`, **not** `-dev`.
- [ ] Only one Tweaks jar (the deploy script deletes other `ArcanaQuestTweaks-*.jar`).

## Boot

- [ ] Client starts the full pack; Charm is present (`required-after:charm`). No mixin apply crash from `mixins.aqtweaks.json`, `mixins.aqtweaks.early.json`, or `mixins.aqtweaks.charm.json`. Log must not say `MixinWorldRiftLight` / `World was loaded too early`, `MixinMinecraftMouseGrab` / `Minecraft was loaded too early`, `MixinEntityRendererMouse` / `EntityRenderer was loaded too early`, `MixinMobSpawnerBaseLogic` / `MobSpawnerBaseLogic was loaded too early`, `MixinTileEntityLockableLoot` / `TileEntityLockableLoot was loaded too early`, `MixinItemStackQualityDurability` / `MixinItemStackDurability` / `ItemStack was loaded too early`, `MixinBlockCropsSeed` / `BlockCrops was loaded too early`, `MixinASMHooksVillagePaste` / `ASMHooks was loaded too early`, `MixinWorldGenLakes` / `field_150589_a was not located` / `WorldGenLakes in invalid classes`, `empty category` / `StatsKeeperModuleConfig` / `BetterMineshaftsModuleConfig`, BM `setBoundingBox` / `func_75072_c` was not located, `MixinRPOTeleporter` / `field_85192_a was not located` in `RPOTeleporter`, `parseStructureData is not cancellable`, `StackOverflowError` / `ItemTechnomancerScribingTools` / `QualityDurability.afterSetDamage` during recipe init, or world-tick `StackOverflowError` / `QualityStamp.stampInventory` / `fillWithLoot`. Optional `mixins.aqtweaks.gaia.json` must not log `InvalidInjectionException` (vanilla INVOKEs must be MCP + `remap = true`; a miss boots anyway because `required: false`).
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
| Twilight Forest | No `TfPortalLandingHandler` unless RP is also present |
| The Aether | No `AetherPortalLandingHandler` unless RP is also present |
| Quark | Helper not called; +Y stock Quark unchanged; Deepslate columns/spikes still generate |
| Chisel | No Chisel mixins; stock GUI / in-world carving |
| Recipe Stages | No `setRecipeStage` capture; Chisel gate has no CT index (fallback map also missing) |
| Game Stages | `lockedStage` no-ops; Chisel mixins still apply if Chisel is present |
| Quality Tools | No `mixins.aqtweaks.qualitytools.json`; vanilla loot/durability mixins still apply (pack ships QT); no rune recipes |
| Simple Tomb | No `mixins.aqtweaks.simpletomb.json`; stock tomb placement and vanilla giveInventory restore |

### Do not treat as optional

Missing **RTG, Depths Update, Better Caves, CoFH World, Recurrent Complex, or IvToolkit** with the current required mixin json can **fail mixin apply** at boot. This pack always ships them. Elenai Extended and **Charm** are `@Mod` **required-after**. InControl is `@Mod` **after** and compile-hard for pack fill.

## Config

- [ ] After a Java default change, instance cfg still has the **old** value until edited (e.g. RTG Coast Buffer 32 vs 16).
- [ ] Change a live cfg in-game: stamina DSS costs refresh (`DssSkillCosts.invalidate`). Comfort JSON needs a restart (loaded in preInit). Settings vs blocks are separate files; the old combined `aqtweaks_comfort.json` is ignored.

## Worldgen (new chunks)

Seam, village world push/pop and Mineshaft locate were dead code until the 2026-10-04 target fixes; verify them on a **fresh world**.

`Village Flatten Debug` is **off** by default. Turn it on only while diagnosing; it appends every line to instance `logs/villagepatch.log` (not `latest.log`) and stalls chunk gen.

| Check | Expect | Doc |
| --- | --- | --- |
| Village generation end to end (new chunks, `/aqvillage`, coast and inland) | Same layout as before Reflect removal. With village debug on: `stamp village plate` and `seal chunk=...` lines appear, no `NullPointerException`/`ClassCastException` from `StructureAccess`. Reload the world: village Starts keep their padded boxes (the save call now works) | [rtg.md](rtg.md) |
| Mineshaft Start / locate / entrance stub | Unchanged; no accessor errors | [bettermineshafts.md](bettermineshafts.md) |
| Thaumcraft dimension warp, sleep cleanse, exposure warp | Same awards, sound and chat; the 2-second delayed award still lands | [thaumcraft.md](thaumcraft.md) |
| Boat in water with Simple Difficulty | Temperature sample lifts as before (`MixinWorldUtil` now direct) | [temperature.md](temperature.md) |
| Sleep 6+ in-game hours with Somnia (alone, then with others), wake before morning, and leave the bed early after 6h | Sticky warp drops by `Normal Warp Reduction` each time; a sleep under 6h clears nothing; chat line shows when something cleared | [thaumcraft.md](thaumcraft.md) |
| Sleep 8 hours with temp warp and a Homestead band I / II / III bed | About 2 / 3 / 6 temp warp cleared over 8 h in Case B (none double counted in Case A where player ticks run); no Homestead means none from this credit | [comfort.md](comfort.md) |
| Boot log, bibliocraft | No `Parsing error loading recipe bibliocraft:tapemeasure`; `Skipping recipe JSON with known-missing item bibliocraft:tape` appears once | [recipes.md](recipes.md) |
| New RTG plains mineshaft | A plank collar with a hole at the surface and a ladder down to the tunnel; no shaft under water | [bettermineshafts.md](bettermineshafts.md) |
| Fell a tree with Tree Chopper at gathering 16+ with Lumberjack and Reforester | Extra logs roll per log; saplings roll per leaf (gathering x 0.03), never more than 64 from one tree; breaking a single log or leaf by hand still rolls; without Tree Chopper nothing changes | [reskillable.md](reskillable.md) |
| New RTG plains mineshaft, shaft built | `latest.log` has `[AQ-MINESHAFT] surface shaft hub=... target=... surfaceY=... siblings=N hubInside=...`; the ladder shaft ends inside a mineshaft tunnel or room (no stone-only shaft); when `hubInside=false` the shaft is at the nearest tunnel box, not the hub | [bettermineshafts.md](bettermineshafts.md) |
| Building 32, Normal mode: place and break a block about 2 blocks past normal reach (4.5 + 2) | Works; at Building 0 the same target is out of reach. Melee reach unchanged | [reskillable.md](reskillable.md) |
| Building perks Reach I/II/III, EB non-Normal mode | No perk: reach 5; Reach I (Building 10): 20; Reach II locked until I owned, then 50; III: 100. Upgrade items not craftable, do nothing on right-click; icons show the EB item art | [reskillable.md](reskillable.md) |
| Glide until feathers hit 0 | The glider retracts at the next billing tick and the player falls; feathers then regenerate. Deploying with 0 feathers is undone at the next tick | [stamina.md](stamina.md) |
| Village plate on a hill with caves below | Seal covers 15 blocks below the plate height and never goes below Y50; plate at Y64 seals Y50-64; caves open below that | [rtg.md](rtg.md) |
| Grappling Hook: plant, hang, swing, Shift+W climb, motor with and without Ember | Same bills as before (hang 1/s, swing 2/s, climb 3/s). Motor off when Ember is empty; unhook when feathers run out. No `NoSuchFieldException` / reflection warnings in the log | [stamina.md](stamina.md) |
| Open Glider with empty feathers | Glider undeploys. Without Open Glider installed, no `NoClassDefFoundError` | [stamina.md](stamina.md) |
| Village on water edge with a Waystones gazebo | Wet gazebo is rebuilt inland (`waystone relocate hit` with village debug on). Without Waystones installed, villages still generate | [rtg.md](rtg.md) |
| Sleep with warp, dimension first visit, exposure warp, ritual warp | Warp changes and syncs exactly as before; a Thaumcraft error logs once (`[AQ-TC]`) and does not disable later warp | [thaumcraft.md](thaumcraft.md) |
| Full Font, Vis Thrift, EB reach/max blocks, forage and wool perks | Unchanged behavior. With Reskillable or Animania Farm removed, no `NoClassDefFoundError` | [reskillable.md](reskillable.md) |
| Fluid-filled crucible over lit and unlit Simple Difficulty campfire | Lit: heats (cap 200); unlit: cools. Without Simple Difficulty, crucibles behave as stock | [thaumcraft.md](thaumcraft.md) |
| Village on a hill with caves below | Caves/ravines below Y50 stay open under the village; Y50 to plate is solid; wells and houses on solid ground; coastal brick wall reaches Y50 | [rtg.md](rtg.md) |
| `/locate Mineshaft`, TP, `/locate` again | The second locate returns a different mineshaft (not the one you stand in); with several generated, the nearer of registered vs predicted wins | [bettermineshafts.md](bettermineshafts.md) |
| Boot log | No `Mixin apply ... failed` for any `aqtweaks` json; `cleanmix.log` shows `aqtweaks$refreshMineshaftBox` on `StructureStart` | [bettermineshafts.md](bettermineshafts.md) |
| Fresh Overworld, fly new chunks | `[AQ-DEPTHS] Chunk pass: tunnel-path seam reinforce Y0-4 after BC` appears once in `latest.log`. Better Caves mouths at Y 0-4 connect to the Depths tunnels; ocean/river/beach columns keep Deepslate at Y0 (no water draining into the deep) | [depths.md](depths.md) |
| New village chunks with `/aqvillage` | Village still plates; no growth of the generator/world stacks (no repeated `pushGenerator` without pop in `villagepatch.log` with debug on); starts rejected by `rejectCoastalVillageStarts` are forgotten | [rtg.md](rtg.md) |
| `/locate Mineshaft` on a new world | Pin lands on a registered Better Mineshafts Start (Y 24 when unexplored); `cleanmix.log` shows `MixinMapGenMineshaftLocate` injected into `MapGenMineshaft`; vanilla Mineshaft generators in other dimensions unaffected | [bettermineshafts.md](bettermineshafts.md) |
| Flying new chunks (near and far from villages) | Away from villages, chat TPS stays near 20 (no growing hitch as more towns exist). A village still plates. With debug off, `villagepatch.log` stays empty | [rtg.md](rtg.md) |
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
| Water pieces | No waystone/house in a lake; wet path retries inland; no oak plank path over leftover open water; **no** roads/houses in F3 River or ocean | rtg |
| Swamp village plate | Well on grass, not over a ravine; roads level with houses; overlapping pads; `seal chunk=` in debug | rtg |
| Hill village (new chunks) | All pieces on **one** well Y; no chunk-border stone wall; unused AABB corners stay hills | rtg |
| Under a house at plate-4 | `isInsideStructure("Village")` true; below well floor false | rtg |
| Yard between path and house (12-pad / Hermite) | `isInsideStructure("Village")` true after new gen and after relog. Outside pad+falloff false. Plate+31 false if box height is 30 | rtg |
| Night village lamps (new chunks) | Path torch and fence lamp light the plate and nearby house walls; not a 1-block puddle | rtg |
| Below Y0 Overworld | Deepslate fill, AQ caves, Y0 mouths on land, **no** ocean drain | [depths.md](depths.md) |
| -Y caves after a perf change (new chunks) | Same seed, same chunks: tunnels, chambers, pillars, bridges, stalactites and floater cleanup unchanged. Perf work here is exact-equivalence, so any visible difference is a bug | depths |
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
| Comfort | Homestead I in a scored room that also has a hearth, bed, or seat, while healthy: Learning 8:00, no Soot XP boost. Lanterns/structure alone do not start it. Penalties can deny it. Scan every 30s; Homestead potion 45s. Hurt: clear + 30s before re-entry. Attack entity: clear + 15s. II: Learning gone, XP boost I + endurance I + replenishment 4:00. III: XP boost II + endurance II + replenishment 8:00. Temp warp cleanse +2/+3/+6 per scan, fire at 12 (180s / 120s / 60s); counter survives Homestead drop. OP `/aqcomfort` prints the breakdown and can refresh without waiting 30s; non-OP denied. Hot spring → cold resist if SD+BOP |
| Portal | Arcane Tunnel binds on air or block then opens a 60s two-way rift (cross-dim if bound elsewhere); land at dest rift XYZ after ~3s gate; Unstable Arcane Tunnel lands ~4000–6000 same dim; villagers/mobs in the box teleport; sitting pet stays; lead follows. Dark cave mid-cell lights like glowstone. Sit in an open rift: no `updateClouds` CME. Do not DS full-bypass the rift (blanks the cylinder). See [portal.md](portal.md) |
| Client | Toughness LTR above armor; iron pick shows Vanilla Tools stats; Metallurgy pick not duplicated |
| MineMenu | Reskillable, BetterQuesting, and Hwyla config: look frozen while open. Close from a corner: cursor grabbed at center, no yaw snap, next look starts from center. Inventory, Baubles, and Esc the same. MineMenu Baubles entry opens the expanded GUI once; the real Baubles key still once and does not crash. DSS Skills GUI once from MineMenu with the key unbound or assigned; space does not open it while unbound; the real key opens once and does not crash; `/dssgui` opens it. Boot: no `Minecraft was loaded too early` / `EntityRenderer was loaded too early`. See [minemenu.md](minemenu.md), [stamina.md](stamina.md) |
| Recipes | Pack boots without Metallurgy `generated/item/spartanweaponry` recipe spam or `Parsing error loading recipe` stacks for the 40 known-missing items and `draugr_ingot_from_block`; one-shot skip lines logged per missing item ID; new/unexpected missing items still dump |
| Reskillable | Attack 16 → +2 damage; Mining Expert wood pick drops diamond ore; stamina perks on tree; Magic school mutex holds in both directions after the LOWEST restamp; Tunnel Sense glow lasts 5 seconds; Fortify shield stamina is 0. Full list: [reskillable.md](reskillable.md) |
| Spawning | Boot log loads spawn types, structure spawns, parties, and pack group sizes. First ticks: no `parseStructureData is not cancellable`. Closed cave: dwarf/cave_spider/krake yes, Dryad/witch/Wildkin no, zombie/goblin still yes. Mineshaft non-origin chunk: witch/illager/pillager can appear; ordinary cave still no. Night surface: reverse exclusives; zombie still yes. Creeper packs 1–2 not 4; enderman 1; zombie/skeleton/spider 2–4 mixed. Default `goblin_feral=3-5`. Fill Pack Size off: old singles. Natural Overworld cleric: knights + CR archers; cage/portal cleric: no party. Failed cultist/blaze cage Delay ≈ 20 (cfg) not 0 and not 200–800; zombie cage still attempts after 1→0; success still 200–800. Hostile cap default 200. No mixin fail on `MixinWorldEntitySpawner`, `MixinMobSpawnerBaseLogic`, or `MixinStructureCache`. See [spawning.md](spawning.md) |
| Advancement | Join log: no `AddonHandler.onWorldLoad` → `ForgeHooks.loadAdvancements`. Mixin json applied. Animania animals still spawn/register. No Farm/Extra Animania advancement trees. See [advancement.md](advancement.md) |
| Better Mineshafts | `/locate Mineshaft` TPs to tunnels; new chunks use `aqtweaks_bettermineshafts.cfg` rate/spacing; log: no `mixins.aqtweaks.bettermineshafts.json` `setBoundingBox` / `func_75072_c` apply failure. See [bettermineshafts.md](bettermineshafts.md) |
| Stats Keeper | 10 hearts: elixir consumes, +1 heart, drink sound from `aqtweaks_statskeeper.cfg` (default level-up; empty = silent). 20 hearts: drink cancelled, stack remains, red action bar `Your vitality is already at its peak!`, no drink sound. Baubles/buffs above 20 hearts with unused SK additional still drink. See [statskeeper.md](statskeeper.md) |
| Twilight Forest | New Overworld→TF RandomPortals trip into a locked/hazard column lands in a safe biome **on grass** (frame bottom **on** grass, not sunk), not a landmark, not tree canopy. Return through the same sending portal stays linked. Nether RP unchanged. Rift items unchanged. See [twilightforest.md](twilightforest.md) |
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

## After mixin / parent bumps

Re-read mixin targets ([compatibility-matrix.md](compatibility-matrix.md) upgrade check). RC version bumps: confirm `RayMatcher.cast` `@Overwrite` still matches.
