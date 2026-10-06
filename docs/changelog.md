# Changelog (1.8)

Stay on version **1.8** until a plan bumps `ArcanaQuestTweaks.VERSION`.

## 2026-10-04 — Surface indoor insulation and boat wetness

- `DynamicModifierInsulation` pulls biome, time, altitude, snow, and Serene Seasons temperature toward neutral when the sampled position is sheltered, covering the surface houses Simple Difficulty's `Y >= 64` underground cutoff leaves exposed. Heaters, chillers, armor, baubles, held items, and wetness keep full effect indoors.
- Shelter is an opaque **or glass** roof plus, by default, at least 3 of 4 cardinal collision walls. Glass ceilings count because glass does not raise the light heightmap. Serene Seasons greenhouse glass shifts the target from 12 to 14 — more heating in cold, less cooling in hot — matching Serene Seasons' own 7-block, ignore-what's-between scan so warmth and crop fertility agree. Greenhouse glass is read by registry name, so no Serene Seasons jar is added.
- `MixinWorldUtil` lifts the temperature sample one block when a player rides a boat over water, so an open boat in clear weather no longer reads as wet `-6`. Rain, submerged boats, swimming, and other mounts are unchanged. Seven new `aqtweaks_stamina.cfg` keys. New module doc: [temperature.md](temperature.md).

## 2026-10-04 — Somnia wake no longer desyncs the mouse

- Somnia's wake packet closes the sleep screen on the network thread, so the cursor grab did not take effect and the camera stopped following the mouse until a click or Escape. `MixinMinecraftMouseGrab` now re-queues any off-thread `displayGuiScreen` onto the client thread.

## 2026-10-06 — Deferred relight queues only light emitters

- `DeferredRelight.onCheckFailed` ignores positions whose block does not emit light. The lighting check runs for every opaque block placed during generation, so shrines and villages queued thousands of stone blocks; the 8192 cap dropped the torches and lanterns first, and each retry did a full flood-fill. Never reads an unloaded chunk.

## 2026-10-06 — Deferred relight for chunk-generation lighting

- Torches and lanterns placed during chunk generation (villages, ocean monuments, shrines) no longer stay dark. `World.checkLight` silently did nothing when the surrounding chunks were not loaded and nothing retried. New `MixinWorldCheckLight` (early json) queues each failed server-side check in `DeferredRelight`, which retries it once the area is loaded (bounded: 8192 entries, 6000 ticks, 64 per tick per world). Config `Enable Deferred Relight`, default on. See `rtg.md` item 28.

## 2026-10-06 — Tab-complete crash traced to Serene Seasons

- The `"list1" is null` soft crash in `MinecraftServer.getTabCompletions` (AQ-005) comes from `/ss` (`/sereneseasons`), e.g. `/ss setseason <TAB>`: `SSCommand.getTabCompletions` returns `null` for any argument position but the first. New optional `mixins.aqtweaks.sereneseasons.json` / `MixinSSCommand` returns the sub-season names (`early_spring` ... `late_winter`, filtered by what you typed) for `/ss setseason <TAB>` and an empty list elsewhere. Compile-hard against the Serene Seasons jar (`libs/`, `$deps`).

## 2026-10-04 — Remote rift lighting retries until its chunks load

- `RiftLighting` kept a `checkLight` job for one attempt only. `World.checkLight` silently returns false while the surrounding area is not loaded, so the far end of a pair (and a rift seen right after a dimension change) stayed unlit until another light update. Jobs now stay queued until `checkLight` returns true, up to 30 s.

## 2026-10-04 — Portal: fading particles, rift always tracked under Dynamic Stealth

- Rift particles thin out as the rift's time runs down (100% to 15% of the 2 per tick). New synced `LIFESPAN` parameter on `EntityArcaneRift`.
- `MixinDSEntityTrackerEntry` keeps the rift tracked whenever it is in range and the player's chunk is loaded, so DS no longer removes it when you look away, and without the full-bypass render failure on return. The rift is not in the DS Full Bypass list.

## 2026-10-04 — Homestead I Learning is 4 minutes

- Extra Alchemy Learning from Homestead / Comfort tier I lasts 4:00 (4800 ticks), down from 8:00. Tier II and III effects are unchanged.

## 2026-10-04 — Sleep minimum 5 hours

- `Sleep Minimum Hours` default is now 5 (was 6). A test sleep that began at about 00:20 and ended at dawn lasted 5.7 hours and was correctly refused at 6. Confirmed working in play; the temporary `[AQ-SLEEP]` diagnostics were removed. An existing `aqtweaks_thaumcraft.cfg` keeps its saved value until edited.

## 2026-10-04 — Cleanup Batch 4 (performance, robustness, hygiene)

- Performance: dimension-warp 2s delay is a server-tick queue (no `new Thread` per dimension); Thaumcraft exposure grants parsed once; `SpawnLayerFilter` caches entity id strings; Comfort cozy scan caches `Block -> CozyConfig` (cleared on comfort config reload); Stamina caches weapon/throwing classification per item (cleared on config change, `Locale.ROOT`); `PerkWishlist` fortify/unyielding/slow fall test the cheap condition before the perk lookup; `QualityDurability.trace` is gated by `Trace Wear` (default off, no NBT reads or formatting when off).
- Leaks: `PerkDurability` marks expire after one tick (a cancelled break no longer makes an unrelated `damageItem` free) and clear on logout; `LAST_NOTIFY_TICK` and `MISMATCH_AT` prune expired entries; `VETTED_STARTS` is bounded.
- Simple Tomb: slot map handed to the tomb through `TombSlotMapAccess` instead of an NBT write/read round trip; grave-slot tags and a stale death context are cleared on respawn and after drops.
- `MixinEntityAITasks` logs the first NPE and any reset failure per task class (`[AQ-AI]`); `MixinASMHooksVillagePaste` logs once if its bridge fails (`[AQ-CHARM]`).
- Reskillable: perk requirement rows that name another trait (`trait|`) are applied after every Tweaks trait is registered. The boot "Unlockable not found" errors remain: they are Reskillable re-reading the saved requirement strings in `config/reskillable.cfg` while it constructs each trait (7 forward references plus `elenaidodge2:dodge`), which Tweaks cannot intercept. They are cosmetic; the restamp sets the correct requirements. Building perk icons: Drafter, Sculptor and Transpose now use the Effortless Building radial-menu icons (wall, cube, replace).
- Build: the four required mixin jsons now set `injectors.defaultRequire = 1`. First boot crashed on `MixinStructureStartVillagePaste.aqtweaks$skipWetVillagePaste` (0 targets: Charm's ASM rewrites that invoke into `ASMHooks.addComponentParts`, which `MixinASMHooksVillagePaste` handles), so that one redirector is now `require = 0`; `mcmod.info` takes its version from `build.gradle` (`verifyReleaseJar` fails if `ArcanaQuestTweaks.VERSION` differs) and lists `charm`; `depthsupdate` is in the `$deps` copy list; build artifacts (`.gradle/`, `build/`, `remap_out*.txt`, `_spark_scan.py`) removed from the Git index (files kept, now ignored).

## 2026-10-04 — Building Reach I / II / III perks

- The EB reach upgrade items are also hidden (creative tab removed in `postInit`, so JEI drops them). Perk descriptions state the measured ranges, about 7 / 15 / 30 blocks.

- New Building perks Reach I, II, III (chained, Building 10 / 20 / 28) set Effortless Building's reach tier to 20 / 50 / 100. The EB reach upgrade items are disabled (no recipes, right-click cancelled, stored upgrade level ignored); `Disable EB Reach Upgrade Items` = false restores them. Icons reuse the EB item art.

## 2026-10-04 — Building skill raises vanilla block reach

- The Building skill now adds to the Forge `generic.reachDistance` attribute (`Vanilla Reach Per Level` 0.0625, `Vanilla Reach Max` 3.0), so block placing, breaking and using reach further in Normal mode. Before, the bonus only touched Effortless Building's non-Normal modes, so it was never visible in plain play. Attack and mob reach are unchanged.

## 2026-10-04 — Tree Chopper harvest perks, mineshaft shaft aimed at the tunnels

- Lumberjack, Reforester and every other harvest perk now work on trees felled by Tree Chopper. Its `DestroyTree` breaks blocks with `World.destroyBlock(pos, true)`, which raised `HarvestDropsEvent` with no harvester, so all perks were skipped. `MixinTreeHandler` (new optional `mixins.aqtweaks.treechopper.json`, Tree Chopper jar added to `libs/`) records the felling player in `HarvestActor`.
- Reforester is now a gathering-scaled chance per leaf (`Reforester Chance Per Level` 0.03) instead of one per leaf, with `Reforester Max Per Tree` = 64 for a felled tree and `Reforester Guaranteed` to restore the old rule. Skill descriptions updated.
- The Better Mineshafts surface shaft is built by a Start-level hook that can see the sibling tunnel pieces: straight down the hub when it lies inside one, otherwise at the nearest piece box. Fixes shafts that ended in solid stone. One `[AQ-MINESHAFT]` log line per Start.

## 2026-10-04 — Sleep warp cleanse by hours slept

- The sleep cleanse no longer requires morning or `wakeImmediately == false`. It requires `Sleep Minimum Hours` (default 6, in-game, counted on the world clock so Somnia fast-forward counts) between going to bed and waking. Previously waking before morning, or leaving the bed after a long sleep, cleared nothing.
- Temporary warp: sleep time that player ticks did not cover (Somnia Case B) is credited at the player's Homestead band rate (2 / 3 / 6 progress per 30s, 12 per temp warp) through the existing Comfort counter. New config `Sleep Minimum Hours`, `Sleep Comfort Temporary Warp Clear`.

## 2026-10-04 — Mineshaft surface shaft, recipe skip, build script

- Better Mineshafts entrances that BM refuses to build (common on RTG terrain) now get a 1x1 ladder shaft with a plank collar from tunnel level to the ground (`MineshaftSurfaceShaft`; config `Surface Shaft Fallback`, default on). Previously only a hidden stub was kept and the mineshaft had no way in.
- `RecipeJsonSkip` also matches unqualified ids (`"tape"`) inside the owning mod's own recipes, which silences the `bibliocraft:tapemeasure` parse error.
- `build_gradle.ps1`: no more false failure when stderr is merged (`GRADLE_OPTS` native-access flag, exit-code check), env-var overrides for the DEVBOX and JDK paths, exact-version jar copy.

## 2026-10-04 — Glider cancel at 0 feathers, plate depth rule

- `OpenGliderHelper.undeploy` also sends Open Glider's `PacketClientGliding(false)`. Before, the server flag cleared but the client kept gliding, so billing stopped and feathers regenerated mid-glide.
- Village plate seal: new `Village Plate Depth` (default 15). The seal runs from `max(Village Plate Min Y, plate Y - Depth)` to the plate Y (Min Y stays 50).

## 2026-10-04 — `Reflect` removed (cleanup batch 3)

- `util/Reflect.java` (3,900 lines, ~190 reflective handles) is deleted. `remap = false` only affects mixin annotation strings; method bodies are remapped by `remapJar`, so vanilla is called directly everywhere. Verified in the built jar: a `remap = false` mixin body references `field_151587_i` / `func_185904_a`.
- Protected/private vanilla members go through new accessors in `mixin.vanilla` (`AccessorMapGenBase`, `AccessorMapGenStructure`, `AccessorMapGenVillage`, `AccessorStructureComponent`, `AccessorVillageStart`, `AccessorChunkProviderServer`) wrapped by `rtg/StructureAccess`. This also turns the village save (`saveMapGenStructureStart`) and box refresh into working code instead of silent no-ops.
- Elenai feathers/weight moved to `stamina/StaminaFeathers`; `PerkAccess.has` is the guarded Reskillable entry; `SimpleDifficultyHelper` gained guarded thirst/temperature getters; Depths block constants are `DepthsBlocks`.
- The village seal loops in `MixinChunkGeneratorRTGVillage` use `PrimerAccess` instead of per-block reflective invokes. `ThaumcraftModule` per-tick checks are plain field reads.
- The startup `[AQ-REFLECT]` audit and its 12 WARN handles are gone with the class.

## 2026-10-04 — Compile-hard conversions (cleanup batch 2)

- Grappling Hook, Open Glider and Waystones jars added to `libs/` and `build_gradle.ps1`. `stamina/GrappleHelper`, `OpenGliderHelper`, `rtg/WaystoneBridge` replace the reflection in `Reflect`, `GrappleClientInput` and `MixinStructureVillagePieces`. `MixinGrappleController` is a class-target mixin with a direct `motor` redirect (no per-tick field lookup).
- `ThaumcraftHelper` calls the Thaumcraft warp capability directly; one failure logs once instead of disabling warp for the session.
- Own-class `Class.forName` bridges (`ReskillableBonuses`, Animania Farm products/clocks) are direct calls behind cached `isModLoaded` flags (`AnimaniaAddons.FARM` for the Farm addon, which has no mod id).
- `MixinTileCrucible` asks `SimpleDifficultyHelper.isBurningCampfire` (typed `BlockCampfire.BURNING` check) instead of matching a registry string.
- `MixinGeneratorLakes`, `MixinWorldGenLakes` use `@Shadow`; `MixinSkillActive` targets `SkillActive.class` and reads `getUnlocalizedName` directly.

## 2026-10-04 — Mineshaft Start mixin, locate ranking, village plate depth

- `MixinMapGenBetterMineshaftStart` was skipped entirely at load (its `func_75068_a` hook targeted a method BM's `Start` does not declare). The box refresh moved to `MixinStructureStartMineshaftBox` on `StructureStart`, BM-guarded; the ctor Y override uses local index 5.
- `/locate Mineshaft` ranks the nearest registered Start against the nearest predicted chunk by distance, ignores registered Starts for `findUnexplored`, and skips candidates within 32 blocks of the player. Previously the first generated Start won every locate.
- The village plate seal now fills from `Village Plate Min Y` (default 50) to the plate height instead of from Y1, so caves exist under villages again. New chunks only.
- `check_mixin_targets.py` resolves nested target classes (`Outer.Inner`).

## 2026-10-04 — Dead mixin targets fixed (cleanup batch 1)

- `MixinMapGenVillageWorld` targeted `MapGenBase.func_151539_a`, a 1.7/1.8 name. It now targets `func_186125_a`, and a per-thread stack replaces the per-instance flag so nested layout `generate` calls pop their own generator.
- The Depths Y 0-4 seam pass (`SeamReinforcer`) now runs from RETURN of `ChunkGeneratorRTG.generateChunk`, once per new chunk. It was on `ChunkProviderServer`, which does not declare `func_185932_a`. `MixinChunkProviderServer` is removed.
- Better Mineshafts locate pin moved to `MixinMapGenMineshaftLocate` on `MapGenMineshaft` (BM guarded), because BM does not declare `func_180706_b`.
- `Reflect`: fixed `isSizeableStructure` (`func_75069_d`), `writeStructureComponentsToNBT` (`func_143021_a`), `ChunkProviderServer.chunkGenerator` (`field_186029_c`), `BlockPos.down`, `DamageSource.isDamageAbsolute`, `EntityArrow.shootingEntity`, `markBlockRangeForRenderUpdate`. Removed the nonexistent `setBoundingBox` handle. `updateBoundingBox` now goes through `InvokerStructureStart` (it is protected).
- `Reflect.auditUnresolved` separates parent-mod (INFO) from vanilla (WARN) handles.
- Optional mixin jsons set `injectors.defaultRequire = 1`. Required jsons (`aqtweaks`, `early`, `vanilla`, `charm`) follow after a clean boot.
- New `scripts/check_mixin_targets.py`. See [build-and-release.md](build-and-release.md).

## 2026-10-02 — Campfire neighbor notification and Thaumcraft crucible heat

- Simple Difficulty campfires notify neighbors on light, age, and extinguish so an Inspirations cauldron boils from the configured odd metas, and a fluid-filled Thaumcraft crucible heats from a burning campfire. Spec: [thaumcraft.md](thaumcraft.md).

## 2026-09-30 — Simple Tomb slot restoration, displacement, Baubles, and admin backups

- Death captures player inventory and BaublesEX slot indices in NBT. On grave retrieval, items return to exact original slots. If an occupied slot cannot accept the grave item, existing items are non-destructively displaced into empty main inventory slots, or dropped safely at the player's feet if inventory is full.
- Death events also record up to 3 rolling death backups per player in Overworld WorldSavedData (`aqtweaks_death_backups.dat`).
- Added `/aqtomb <list|recover>` OP level 2 admin command for recovering inventory if graves are lost or fail placement.
- Cosmetic Armor is preserved on death by Corpse Complex and explicitly excluded from tomb handling. Spec: [simpletomb.md](simpletomb.md).

## 2026-09-29 — Broken one use, then destroy; repair bands

- The first break leaves Broken with one use (`max - 1`). Spending that use destroys the stack. Repair from 26% up to 51% turns Broken into wear `gray`. Past 51%, wear or Broken restores the saved quality. Spec: [qualitytools.md](qualitytools.md).

## 2026-09-28 — Thaumcraft ring null texture bind

- A null bauble texture is skipped before `bindTexture`, so a worn Thaumcraft ring no longer aborts the rest of the bauble pass. Spec: [thaumcraft.md](thaumcraft.md).

## 2026-09-28 — Elenai armor tooltip weight list

- Armor tooltips read the weight string locally, so hovering armor in the Baubles screen no longer throws `ConcurrentModificationException` from Elenai's static list. Spec: [stamina.md](stamina.md).

## 2026-09-28 — Dawnstone rune reroll skips current Name

- Same-tier rune hammers pick equally among other qualities of that color (loot weight unused). Step-up still uses the full next-color pool. Type match prefers the file that lists the live quality Name.

## 2026-09-28 — Thaumcraft rings skip amulet model

- `thaumcraft:baubles` meta 1, 3, and 5 no longer get `ModelAmulet`, so rings do not draw a second player skin. A null amulet texture also skips that body draw. Spec: [thaumcraft.md](thaumcraft.md).

## 2026-09-25 — Thermia cap kill and slow recovery

- Hypothermia and hyperthermia still cannot push the Elenai max below 1. The next penalty step at the config base drains the last half-feather and kills with that Simple Difficulty source. After the potion ends, the cap returns one half-feather every 10 ticks. Spec: [stamina.md](stamina.md).

## 2026-09-25 — Rancher clock throttle and perk lookup

- Rancher still doubles Animania clocks, applied as 20 steps once per second. Gestation stops at 1 so birth still happens. Armor weight, perk checks, gold feathers, and Simple Difficulty thirst use cached or compile-hard calls. Climb jump packets send on change. Spec: [reskillable.md](reskillable.md), [stamina.md](stamina.md).

## 2026-09-24 — Dark Vision flat mix and sight

- Dark Vision holds the lightmap at 0.4 everywhere the perk is on. Dynamic Stealth treats that player as having night vision. Spec: [reskillable.md](reskillable.md).

## 2026-09-24 — Prospecting pick outline, Dark Vision curve

- Prospector outlines ores a Prospectus pick counted, for 7 seconds, and raises that pick's accuracy by 25. Every pick uses the Tweaks base chances. Dark Vision mixes at 0.8 in pitch dark and eases over 5 ticks. Spec: [reskillable.md](reskillable.md).

## 2026-09-23 — Benevolent, Prospector outline, Dark Vision mix

- Benevolent doubles a Heal focus on any other living target. Prospector draws ore boxes for the mining player. Dark Vision mixes the client lightmap up to 0.7 instead of applying Night Vision. Spec: [reskillable.md](reskillable.md).

## 2026-09-23 — Reskillable perk wishlist

- New perks for mining, gathering, farming, attack, defense, and agility, plus the cost pass on the existing trees. Magic schools require Magic 20 and restamp their `not|trait|` locks after every school is registered. Spec: [reskillable.md](reskillable.md).

## 2026-09-23 — TF/Aether portal sits on grass

- RandomPortals vertical dest frames were one block in the surface (bottom row replaced grass). Tweaks now raises them so the bottom row rests on the grass/island. See [twilightforest.md](twilightforest.md) and [aether.md](aether.md).

## 2026-09-23 — GUI close once, DSS unbound key

- Mouse re-grab runs only on the outermost `displayGuiScreen` return, so Reskillable, BetterQuesting, and Hwyla config close with the pointer on the crosshair. DSS ignores key 0, so an unbound skills bind does not open on space. Assigned DSS and Baubles keys no longer call `Keyboard.isKeyDown`. See [minemenu.md](minemenu.md) and [stamina.md](stamina.md).

## 2026-09-23 — GUI mouse recenter and Baubles key

- Closing a screen forces the cursor free, centers it, then grabs again, and skips look for two camera frames so the warp is not yaw. MineMenu’s Baubles entry polls `isPressed()` and sends `PacketOpen(EXPANSION)` when the hardware key is up. See [minemenu.md](minemenu.md).

## 2026-09-23 — DSS skills GUI from MineMenu

- Client tick polls DSS Skills GUI `KeyBinding.isPressed()` and sends `OpenGuiPacket(0)` when the hardware key is not down. Client `/dssgui` sends the same packet. Real key presses stay on DSS `KeyInputEvent`. See [stamina.md](stamina.md).

## 2026-09-23 — MineMenu mouse grab

- Client mixins skip `EntityPlayerSP.turn` while any screen is open, and re-grab plus drain the LWJGL recenter delta when `displayGuiScreen` returns to play. `Minecraft` inject is in the early json client array. Spec: [minemenu.md](minemenu.md).

## 2026-09-21 — Reskillable Magic schools

- Four mutex Magic schools (Druid, Witch, Astromancer, Artificer) plus two follow-ups each. Thrift ×0.70 on the school perk. Spec: [reskillable.md](reskillable.md). Parent mixins stay in existing json where those modules already exist; Botania/Embers get optional json under this module.

## 2026-09-21 — Aether portal island landing

- RandomPortals dest dim 4 snaps onto aether grass/dirt/holystone before search/build (200 then 400). Mixin rejects void pads. Linked return portals unchanged. See [aether.md](aether.md).

## 2026-09-21 — Dawnstone same-tier reroll

- Common/Uncommon/Rare/Legendary also reroll yellow/green/blue/gold when the piece is already that tier. Ladder upgrades unchanged. Duplicate `cfg` string compare no longer gates Uncommon+.

## 2026-09-21 — Dawnstone rune registry names

- Crafting Runes 1.1 ids are `sccraftingrunes:itemcommonmat` (and uncommon/rare/legendary). Tweaks defaults and lookup now use those; old `*_mat` cfg values still resolve. Place the tool first, then the rune.

## 2026-09-20 — Quality Tools wear/break skips

- Do not cache `isQualityItem` before QT types load (crafted tools were stuck `not_quality`). Never cache a false miss; a live Quality tag counts as eligible. Armor `attemptDamageItem` with a null player still stamps wear/Broken in-slot.

## 2026-09-20 — Quality Tools wear chance

- Wear `gray` is no longer a flat 100% on the first eligible 40-tick hit. p = used × (250 / (max/2)), clamped to 0.05–0.50 (`wearChance` 0 still disables). Stone/iron sit on the ceiling; diamond stays below it.

## 2026-09-20 — Quality Tools Module

- Loot/drops stamp Quality Tools tags before pickup; crafted gear stays untagged (living-update first roll skipped). Wear `gray` at ≤20% remaining (not on Broken); break without Salvage drops `dark_gray` and keeps QualityBase. Dawnstone Anvil upgrades with Crafting Runes on a strict color ladder (rune consumed). Pack `tools.json` still needs a `dark_gray` Broken entry.

## 2026-09-20 — Game Stages / Chisel

- **Chisel** crafts (GUI `SlotChiselSelection.craft` and in-world `ItemChisel.canChisel`) refuse Recipe-Staged outputs unless `GameStageHelper.hasStage` is true. Pack allowlist: `apprentice_builder`, `experienced_builder`, `master_builder`. Red action bar `chat.aqtweaks.gamestages.chisel_locked`. Optional mixin json; AutoChisel and Chisels and Bits unchanged.

## 2026-09-20 — release hardening

- **Java target stays 21** (`options.release = 21`, class major 65). Do not `--release 8`. Mixin json is `JAVA_21`. Fugue refuses 66+ only.
- **Village unload:** `VillagePlate` is loaded when village Events register so Forge’s `EventSubscriptionTransformer` does not ClassReader empty bytes during `stopServer`.
- **Charm** is `@Mod required-after` (Curse / pack prerequisite). `mixins.aqtweaks.charm.json` is `required: true` on jar `MixinConfigs` with the early json; compile-hard `ASMHooks`. Isolated boot without Charm is expected to fail.
- Shared `mixins.aqtweaks.refmap.json` is packaged; mixin configs point at it (silences missing per-json refmap warnings).
- **Evasion:** drop unresolvable CAD `trait|` rows such as instance `trait|elenaidodge2:dodge`. Requirement is agility 16.
- Gradle `verifyReleaseJar` (via `check`): remapped jar has `VillagePlate`, mixin json, refmap, and every class major 65.
