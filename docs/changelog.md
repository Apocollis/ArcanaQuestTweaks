# Changelog (1.10)

Stay on version **1.10** until a plan bumps `ArcanaQuestTweaks.VERSION`.

## 2026-10-28 — Light recording for Chunk Steward staged populate (API v4)

- **`PopulateScopeBridge`** (`rtg/`, registered once from mod init through `ChunkStewardAPI.registerPopulateScope`, loaded only when the runtime API version is 4 or higher): Chunk Steward's staged populate applies RTG's decorations, the rest of the populate and the mod world generators after the populate call returned. The bridge opens `PopulateRelight`'s recording scope for that part (`PopulateRelight.beginResume`, which accepts a chunk that is already marked terrain populated), so those block writes and light checks are recorded and replayed in slices like an unstaged populate. A chunk with a record that is still queued keeps it; `take` is keyed by chunk. Compiled against `ChunkSteward-1.1.jar` (API version 4). Nothing changes without Chunk Steward's `features.enabled`.

## 2026-10-28 — Village plate on a Chunk Steward worker (API v3)

- **Flatten and seal can run on a terrain worker** (`rtg/PipelineContext`, `rtg/VillagePlateJobPreparer`, `rtg/VillagePlateSnapshots`; compiled against `ChunkSteward-1.1.jar`, API version 3). `PipelineContext.active()` now answers `ChunkStewardAPI.isPipelineContext()`, so the guards that were inert are live: a worker generator is never stashed (`MixinChunkGeneratorRTGVillage`, `MixinMapGenVillageWorld`), `registerVillages` does not lay out on a worker, and `MixinMapGenVillageStart` does not write the village registry from a worker. Every Chunk Steward reference sits in a nested class that loads only after the runtime `API_VERSION` (read by reflection, since the constant is inlined by javac) is 3 or higher; on a version 2 jar, or without Chunk Steward, nothing changes.
- **`VillagePlateJobPreparer`** (registered once from mod init): `prepare` runs the same village layout and builds the same snapshot the inline hooks build, one list for flatten (recovered, flatten reach) and one for seal (registry as it is, seal reach), plus the start chunks of every village near the chunk. A chunk with no village gets an empty snapshot. `isStillValid` is read-only and false only when the set of villages near the chunk changed since `prepare`; a failed `prepare` returns an unavailable snapshot, so that worker chunk is discarded and generated inline.
- **Worker passes read the job snapshot**, never the live registry; an empty snapshot writes nothing and a missing or unavailable context skips the pass. On the server thread and on verify the inline path is unchanged (`VillagePlateSnapshots` is that code moved out of the mixin). Landscape reads on a worker use the worker's own generator (pushed for the whole `generateChunk`); with none, `sampleLandscape` returns null instead of falling back to the stash.
- Not covered, and still live reads on a worker: `world.getBiome` of loaded chunks, `VillageShoreMask` and the village-water checks. No version bump.

## 2026-10-28 — Populate column relight reaches RTG chunks

- **RTG-Plus populate is now recorded** (`mixin/rtg/MixinRtgLatePopulate`, late json): RTG's `LatePopulatingChunk.doPopulate` calls `checkLight()`, `IChunkGenerator.populate`, `GameRegistry.generateWorld` and `markDirty()` itself and never reaches `Chunk.populate(IChunkGenerator)`, so the populate relight did not apply to RTG overworld chunks (flight: `relightBlock` in `placeLeavesBlock` did not collapse). The hook begins the same scope before `doPopulate` and ends it on return. Key: `Populate Relight RTG` (default true, needs `Populate Relight`). Checked against RTG-1.12.2-9.0.3.

## 2026-10-27 — Atum sandstorm gameplay expansion: 3-tier scour, baubles goggles, mob physics & scour

- **3-Tier Sand Scour Damage** (`AtumStormGameplayHandler`, `aqtweaks_atum.cfg`): Reworked unsheltered sandstorm abrasion damage (evaluated every 80 ticks / 4 seconds, bypassing standard armor absorption):
  - *Tier 0 (Exposed)*: Missing any armor slot (`HEAD`, `CHEST`, `LEGS`, `FEET`) deals **6.0 damage** (3 hearts).
  - *Tier 1 (Fully Armored)*: All 4 armor slots filled with standard armor deals **4.0 damage** (2 hearts).
  - *Tier 2 (Full Atum / Whitelisted Magic Armor)*: All 4 armor slots filled with Atum-native armor or whitelisted sets (`sandstormResistantArmorWhitelist`) deals **2.0 damage** (1 heart).
- **Protective Headgear & Baubles Goggles** (`BaublesGogglesHelper`): Stinging grit blindness and sand inhalation choking now strictly require Atum-specific headgear (`desert_helmet_*`, `wanderer_helmet`, `mummy_helmet`, `eyes_of_atem`, `halo_of_ra`, `mask_of_nuit`) or goggles. Non-Atum helmets (iron, diamond, etc.) no longer protect eyes/airways. Goggles (such as Thaumcraft Goggles of Revealing) worn in the **Baubles head accessory slot** provide full protection while leaving the primary armor helmet slot free.
- **Sandblasting Armor Durability Wear** (`enableArmorAbrasionWear`, `abrasionWearIntervalSeconds`): Unsheltered non-Atum armor pieces take 1 point of durability wear every 10 seconds. All `atum:*` armor pieces and configured whitelisted magic armors are immune to abrasive sandblasting.
- **Mob Physical Wind Push & Storm Scour** (`handleServerMobEffects`):
  - Unsheltered mobs in Atum sandstorms receive physical wind push, scaled by knockback resistance.
  - *Atum Undead, Constructs, and Fauna*: Mummies, Wraiths, Forsaken, Bonestorms, Pharaohs, Stoneguards, Stonewardens, Efreet, and native animals (Desert Wolves, Camels, Scarabs, Tarantulas, Desert Rabbits) have full immunity to storm scour damage.
  - *Atum Bandits & Brigands* (`EntityBanditBase`): Mortal brigands are treated as wearing full Atum gear, taking **2.0 damage per 4 seconds** when unsheltered.
  - *Non-Native Invading Mobs*: Evaluated under standard 6 / 4 / 2 tier damage rules based on equipped armor, plus slowness.
  - Staggered shelter checks (every 20 ticks per entity ID) ensure zero TPS impact.

## 2026-10-27 — Populate column relight

- **Populate no longer runs a light walk per placed block** (`world/PopulateRelight`, `world/PopulateRelightQueue`, `mixin/lighting/MixinChunkPopulateRelight`; early json): while a new chunk populates on the server thread, `Chunk.relightBlock` and `World.checkLight` are recorded (highest Y per column, checked positions) and the opening full `Chunk.checkLight` only marks the chunk terrain-populated. The records replay later, one `relightBlock` per dirty column, then the checks, then the real `checkLight`, in the Chunk Steward slice, nearest chunk first. The chunk is held unsent until its replay finishes; a chunk near a player (`Populate Relight Immediate Chunks`, default 1) replays at populate return. Reading a height from a pending column replays that column first. Pending work runs on unload, world unload, server stop and after `Populate Relight Max Age Ticks`. Switch `Populate Relight` (`depthsupdate` config). New chunks only. A template paste keeps its own recorder.
- **RTG overhang repair waits for the replay** (`RtgLightFixQueue`): a chunk is not repaired while it or a neighbour has a pending populate record, so the replay cannot undo the repair.
- Begins at the opening `checkLight()` call inside populate, not at the head, so Chunk Steward cancelling populate at the head cannot leave a scope open.

## 2026-10-27 — Atum ruins no longer depend on chunk visit order

- **Ruin check fills the pyramid map first** (`MixinMapGenRuin`, `mixins.aqtweaks.atum.json`): Atum rejects a ruin when a pyramid start is near it, but its check reads the pyramid map up to 4 chunks from the candidate while the pyramid generator only fills 8 chunks around the chunk being generated. A candidate at the edge of that window was judged on whichever pyramids had been stored so far, so two generation orders kept different ruins. On the first `isPyramidInChunk` read of each check the mixin now calls the pyramid generator's own `generate` centred on the check (gated by `AtumConfig.PYRAMID_ENABLED`), so the answer is the same in any order. Saved structure data is unchanged; only new chunks get consistent ruins.

## 2026-10-27 — Atum plate footprint alignment

- **One plate per pyramid** (`AtumPyramidPlate.adopt`): `adopt` used to register the template box (33 by 33, vanilla sizes a template box `0..size`) while `retarget` registers a 32 by 32 footprint, so a rotated pyramid got two plates with different keys and bounds, and which one smoothing used depended on map order. `adopt` now recovers the template origin from the box and rotation and registers the same `footprint(...)` as `retarget`, so it finds the existing plate. A new `[AQ-VERIFY] atum duplicate plate` warning (once) fires if a plate is about to be added that overlaps an existing one. Per-thread plate maps are a separate, later pass.

## 2026-10-27 — Atum plate reach and off-thread well memo

- **Atum plate order** (`AtumPyramidSmoothing.adoptNearby`, `AtumPyramidPlate.reachChunks`): the adopt scan now covers the plate's full reach, `(32 + apron + falloff + corridor + 15) >> 4` chunks (5 with the defaults), instead of a fixed 4. A saved start 5 chunks away is registered before the chunk is smoothed, whatever order chunks are visited in. A late adopt (more than 4 chunks) logs once at debug level as `[AQ-VERIFY] atum late plate adopt`.
- **Village well worker memo** (`VillageWellCache`): `startRejectReason` and `findDryWell` called off the server thread (Chunk Steward pipeline legs) used to skip the memo and rescan up to 4,224 columns each time. Workers now use their own memo maps (`workerRejectMemo`, `workerDryWellMemo`, same TTL, cap and config reset); the server-thread memo is unchanged and never shared with workers, so verdicts do not leak between threads and the pipeline verify stays an independent comparison. `[AQ-PERF]` detail strings carry ` worker` on those calls. `chunkExists` is unchanged.

## 2026-10-27 — Village start loads again

- **Launch crash** (`MixinMapGenVillageWell`): the `@Shadow` on `hasMoreThanTwoComponents` missed the runtime field and `MapGenVillage.Start` failed to load. The sizeable flag is now the start class's own boolean. After expansion it is set when non-road pieces, including the well, are more than two.

## 2026-10-26 — Village paths stay on their line

- **Path continuation** (`VillageLandHelper.pathContinuationCandidates`): a wet path is rebuilt on its own line, back along the road and forward, in the original facing. It is kept only when the new box meets the old one and it is not 1 to 4 blocks beside a road already placed. Pieces that path had added are removed with it. Houses, waystones, and RC village pieces still use the inland search.
- **Cap** (`Village Path Retry Cap`, default 3): after three successful path slides, further wet paths in that village are omitted. `0` omits every wet path.

## 2026-10-27 — Village bridges: dead ends dropped, approach at deck height

- **Dead-end bridges** (`VillageBridgePrune`, called from `MixinMapGenVillageStart` before `VillagePlate.remember`): a bridge road (a road crossing water) whose either end touches no other kept piece (within 2 blocks of the end slice) is dropped, together with the pieces that were reachable from the well only through it. A bridge that runs out into open water fails the far-end test. The village is never vetoed. New `Village Bridge Require Both Ends` (default true). Log: `bridge drop dead-end reason=no-near-piece|no-far-piece`.
- **Approach at deck height** (`VillageBridges.approach`): the road's dry columns within 8 slices of water are brought to the deck Y (`max(round(plate), sea level + 1)`): ground below is filled with dirt and topped with grass path (gravel on sand and the gravel list), ground above (seal wall, cliff lip, up to 6 blocks) is cut through the road width only. Deck and approach share one Y. Wet slices come from landscape and are cached, so every clip agrees.

## 2026-10-26 — RC relocation and village path link

- **RC relocation** (`RcStructureVillageOverlapHandler`): a cancelled overlapping natural structure is held (id and box). On the next server tick one walk of `Village RC Relocate Walk` blocks (default 64, steps of 8, nearest first) looks for a shifted box outside the village whose chunks are loaded; the structure is generated there once (seed from world seed, id and original box; RC's placer picks Y). No spot, or RC refuses it: dropped, no retry. The relocated suggest never queues another walk. `[AQ-RC]` lines in `latest.log`.
- **Path link** (`VillagePathLinker`, after `VillageRoadStamper` in the paste hook): a road end in the open is joined to the nearest other road or the well within `Village Path Link Gap` (default 16) by a one-wide grass path (gravel on sand and the gravel block list). Never crosses a village piece or water; clip-bounded and a pure function of the kept pieces. 0 turns it off.

## 2026-10-26 — Village plate snapshot (T1, behaviour-preserving)

- `VillagePlateSnapshot`: the flatten (`aqtweaks$flattenNoise`) and seal (`aqtweaks$sealVillagePlate`) passes now read an immutable value copy of the village records near the chunk (xz, well, start chunk, land, building and shrine boxes) plus each plate height, built once per pass by `aqtweaks$snapshot`. Same records, same order, same plate-height resolve, so the output does not change. Groundwork for running these passes in a Chunk Steward worker (plan `20261026-0008-village-plate-snapshot-for-chunksteward-pipeline.md`; T2 needs the Chunk Steward 1.1 jar).
- `PipelineContext.active()` (constant `false` until T2) guards the generator stash and `registerVillages`, so a pipeline worker generator can never become the stashed one. The layout scratch `ChunkPrimer` is now one per thread.
- New `Verify Village Snapshot` (`aqtweaks_rtg.cfg`, Surface, dev-only, default off): compares each snapshot with the live records and a fresh plate-height resolve and logs the first `[AQ-VERIFY] village snapshot mismatch`.

## 2026-10-26 — Village well before layout, BOP gravel roads, swamp paths, RC overlap

- **Well before layout** (`MixinMapGenVillageWell`): a never-raise well column is resolved before the town grows, so roads and houses are laid out on the dry spot. The structure-map key stays the original chunk. The old post-layout shift is gone (`well-before` in `villagepatch.log`).
- **BOP roads** (`VillageRoadStamper`): after paste, a road column whose top is `biomesoplenty:grass` or `biomesoplenty:dirt` with air above becomes gravel. Vanilla grass path, sand-to-gravel, and river bridges are unchanged. List: `Village Road Gravel Blocks`.
- **Swamp and lake paths** (`pathWetFraction`): flooded columns that are not ocean or river, and that sit inside another kept piece's pad, count as dry. Ocean, river, and the bridge rule are unchanged.
- **RC natural structures** (`RcStructureVillageOverlapHandler`): a first-time suggest whose box overlaps a village is cancelled before paste. RC village pieces are not this event. `latest.log`: `[AQ-RC]`.

## 2026-10-26 — Atum sandstorm upwind reach and exposure

- **Upwind band** (`AtumOutdoorDustEmitter`): in a storm, outdoor dust spawns from 50 blocks upwind to 16 blocks downwind, and lives 38 ticks, so looking into the wind is a wall of sand. Clear weather stays ±25 blocks along the wind.
- **Inflow** (`ParticleAtumDustSheet`): sand still spawns only under open sky. Once moving, it can blow through a doorway or window and dies when it hits a solid block.
- **Exposure** (`AtumStormGameplayHandler`): without protective headgear, an unsheltered player loses breath and chokes when the bar is empty. Missing chest, legs, or boots (or armor under 10) takes abrasion every 4 seconds; Atum wanderer or mummy cloth on those slots stops it. A strong storm plays an occasional low howl. Keys: `Enable Sand Inhalation Choking`, `Enable Abrasive Sand Scour`, `Enable Desert Storm Ambience`.

## 2026-10-26 — Light chunk resend, pyramid door lift, denser sandstorm

- **Direct light repair resends the chunk** (`RtgLightFixQueue.finishColumnRepair`): when `RTG Light Hack Fix` is on and a fix entry repaired at least one column, that chunk is sent once to players who already have it. The direct `checkLightFor` writes sky light on the server only; the resend is the same chunk packet a relog sends. Wool mode does not send it. RTG and Atum share the repair, so one send covers both. `/aqlight fix direct` still does not.
- **Atum pyramid door lift** (`AtumPyramidPlate`, `Pyramid Door Lift`, default 3): a new pyramid's apron stays on the dune average, and the template is raised so the door is 3 blocks above that apron. The entrance walk slopes from the door down to the sand. Pyramids already pasted keep their door.
- **Atum sandstorm** (`AtumOutdoorDustEmitter`, `AtumFogHandler`): clear weather and storms both spawn 80% small flecks and 20% moderate sheets, at the same sizes and speeds. Storm attempts are `700 + stormStrength * 1100`. `Max Sandstorm Dust Particles` defaults to 12000. Outdoors at full storm the terrain fog runs from the camera to about `0.12 * far`; under a roof the fog stays at the clear range.

## 2026-10-26 — Village layout counts, Atum pyramid plateau

- **Village layout audit** (`rtg/VillageLayoutAudit`): placement is unchanged. When village debug or the perf timers are on, each new village logs roads and houses placed, how many were retried off water, which slot won (back, lateral, toward the well, spiral), and a 5×5 dry/height sample around the well. Lateral is still the first dry street slot, which is the parallel-road case the counts are there to measure.
- **Atum pyramid plateau** (`atum/AtumPyramidPlate`, `atum/AtumPyramidSmoothing`): on new Atum chunks the pyramid's exterior base (template Y=8) sits on the average height of its rotated 32×32 footprint, clamped to at least Y 55. A 4-block apron and a 16-block Hermite slope blend the dunes, and a 6-wide entrance walk slopes from that plateau out to the sand. The two blocks under the sand cap are limestone (Atum has no sandstone block). Keys in `aqtweaks_atum.cfg`.

## 2026-10-26 — Atum gets RTG's light fix, and `/aqlight`

- **Non-RTG light fix** (`world/NonRtgLightFixTrigger`, `mixin/lighting/MixinChunkPopulateLight`, `RtgLightFixQueue.request`): chunks in `Non-RTG Light Fix Dimensions` (default `17`, Atum) now get RTG's own column light fix, with the same direct repair (`RTG Light Hack Fix`) and the same Chunk Steward paced, tiered, deduped queue. RTG's `fixLightingAround` and `fixLightingOnLoad` only run for `BiomeProviderRTG`, so Atum never got the fix and showed black cells under overhangs. A chunk is requested on load and, when a chunk finishes populating (a hook at the end of `Chunk.populate`), the chunk and its four diagonal neighbours. Keys: `Non-RTG Light Fix Enable`, `Non-RTG Light Fix Dimensions`. Same limits as RTG: Y 255 to 49 only, in-chunk neighbours only, and the direct repair sends no block-change packet.
- **`/aqlight`** (OP): server-side light at the block you look at (sky, block, neighbours, height map, `canSeeSky`, queued, whether RTG's rule would fire); `/aqlight scan [radius]` counts cells RTG's test would repair, plus the cells it misses (below Y 49, only brighter across the chunk edge); `/aqlight fix [direct|wool] [radius] [all]` repairs on the spot. Use it to tell missing light from client-stale light.

## 2026-10-26 — `RTG Light Hack Fix` replaces the wool switch

- The repair switch is now `RTG Light Hack Fix` in `aqtweaks_rtg.cfg`. **True (default) uses the direct `checkLightFor(SKY)` repair.** False restores RTG's wool place-and-remove. The old key `RTG Light Fix Wool` is unread; true on that key meant wool. See [rtg.md](rtg.md).

## 2026-10-26 — RTG light-fix drain follows the player

- `RtgLightFixQueue` drains in three tiers when `RTG Light Fix Adaptive Slice` is on (default). Chunks in the travel or look cone, and chunks inside the immediate radius, run first and may use up to the near slice, capped by Chunk Steward headroom minus `RTG Light Fix Reserve Ms` (4). Near chunks outside the cone use the normal slice. Unseen chunks wait for `RTG Light Fix Unseen Min Headroom Ms` (15) and a longer quiet period (`RTG Light Fix Unseen Settle Ticks`, 80). One visible chunk still runs each tick; below 20 TPS that is the only start. `RTG Light Fix Neighbourhood Trigger` skips the quiet wait once the four diagonal neighbours are terrain-populated. `[AQ-PERF]` adds `rtg.lightFix.latency.t0` / `.t1` / `.t2`, `pending.t0` / `.t1` / `.t2` (the printed milliseconds are tick counts and queue sizes), and `rtg.lightFix.budget`. See [rtg.md](rtg.md).

## 2026-10-25 — RTG `fixLightingOnLoad` deferral and direct `checkLightFor` repair (Steps C & D)

- **Queue `fixLightingOnLoad` (`mixin/rtg/MixinRtgFixLightingDefer`)**: `EventHandlerCommon.fixLightingOnLoad(ChunkEvent.Load)` is now intercepted at `HEAD` and enqueued into `RtgLightFixQueue` when `Defer RTG Light Fix` is enabled. Loaded chunks deduplicate with populate-time requests and drain under the shared tick slice budget, eliminating synchronous 256-column walks on `ChunkEvent.Load`.
- **Direct `checkLightFor` instead of wool place-and-remove (`mixin/rtg/MixinRtgFixLightingRepair`)**: In `EventHandlerCommon.fixLighting(Chunk, int, int)`, redirects `World.setBlockState(wool, 3)` and `World.setBlockToAir` to a single direct `World.checkLightFor(EnumSkyBlock.SKY, pos)` call when `RTG Light Fix Wool` is `false`. **The default is `true` (RTG's original wool behaviour)** since 2026-10-26: the direct call sends no block-change packet, so clients that already hold the chunk may keep stale light; a client update (`markBlockForUpdate`) is planned before the direct relight becomes the default. The direct relight skips the second light flood, block state changes, and neighbor block notifications (`markAndNotifyBlock`). `[AQ-PERF]` `rtg.woolRelight` continues to count column repairs.

## 2026-10-25 — Remove Astral Sorcery small shrine from village generation

- Disabled Astral Sorcery small shrine generation in villages by default (`enableAstralSmallShrineVillagePiece = false` in `ArcanaQuestTweaksConfig` and `aqtweaks_rtg.cfg`).
- `VillageAstralSmallShrineHandler.register()` now conditionally registers `IVillageCreationHandler` only when the config knob is enabled, and `getVillagePieceWeight()` returns `null` when disabled.
- Deserialization registration (`MapGenStructureIO.registerStructureComponent(VillagePieceAstralSmallShrine.class, "AQTSmallShrine")`) remains active to ensure clean backward compatibility for existing saved worlds.
- Wild Astral Sorcery shrines in the natural world remain completely untouched.

## 2026-10-25 — Atum visual overhaul: Linear fog, clear skies & 3D storm particles

- Atum 2 fog and sandstorm overhaul (`client/atum/` & `mixin/atum/`):
  - Injects late mixin `MixinAtumClientEvents` at `HEAD` of `ClientEvents.renderFog` and `ClientEvents.renderSand(float, int...)` to completely disable Atum's yellow EXP screen fog and 2D HUD sandstorm overlay (`sandstormoverlay.png`).
  - `AtumFogHandler` enforces clean `FogMode.LINEAR` across sky (`0.0F` to `farPlane`) and terrain (`0.75 * farPlane` to `farPlane`, or `0.40 * farPlane` during sandstorms), removing the blinding yellow EXP fog. Horizon colors remain untouched, keeping views through doorways and windows bright and clear without indoor darkening.
  - Replaces the 2D HUD storm with true volumetric 3D billboard dust particles (`AtumOutdoorDustEmitter`, `ParticleAtumDustSheet`). Particles render with soft depth blending and warm sandy tint `(0.92, 0.78, 0.44)`. Clear weather maintains a steady desert breeze (32 attempts/tick, 0.22 blocks/tick, up to 300 particles).
  - Wind vector is strictly locked to a single uniform heading across the entire dimension ($\theta = 0.45$ rad). All particles stream in unison in the exact same direction with zero angle jitter or conflicting directions.
  - Suppresses Dynamic Surroundings dust motes in Atum via late mixin `MixinDustJetEffect`, and installs an `IRenderHandler` weather suppressor so DSurround's diagonal rain/dust precipitation is completely disabled in Atum.
  - Spawns ~75% large billowing clouds (3.6–5.2 blocks clear, 5.0–7.5 blocks storm) and ~25% small sand grains/flecks (0.6–1.1 blocks clear, 0.8–1.5 blocks storm), all moving along the unified wind heading.
  - When a sandstorm is active (`/atumweather sandstorm`), wind speed (0.90–1.30 blocks/tick), particle density (160 attempts/tick, >2x increase), and cap (up to 1400 particles) accelerate for a thick, roaring desert gale.
  - Strict `world.canSeeSky(pos)` enforcement on spawn and in-flight guarantees that particles never spawn or blow inside covered rooms, pyramids, or underground caves.
  - Configurable via `aqtweaks_client.cfg` under category `"Atum Fog"`. See [client.md](client.md).

## 2026-10-25 — Deep vis crystals below Y 0

- `DeepCrystalGenerator` seeds Thaumcraft vis crystal clusters on cave surfaces in new Overworld chunks, Y −60 through −1 by default. Thaumcraft's own pass samples `nextInt(max(5, surfaceY - 5))`, so it never tries below Y 0. The deep pass copies that cluster shape, shard roll, attempt count and size budget (`Deep Crystal Density Scale`, default 1.0). `Enable Deep Vis Crystals` off, or Thaumcraft `generateCrystals` off, places nothing. Growth still uses the chunk aura. See [thaumcraft.md](thaumcraft.md).

## 2026-10-25 — 1.10: Deep vis crystals (Thaumcraft)

- `thaumcraft/DeepCrystalGenerator` (an `IWorldGenerator`, registered from `CommonProxy.init` when Thaumcraft is loaded) seeds Thaumcraft vis-crystal clusters on cave surfaces below Y 0. Thaumcraft's own pass picks the crystal Y with `nextInt(max(5, surface - 5))` and so never samples below 0; this mirrors its attempt count, size budget, aspect roll and 3x3x3 cluster rule (air or replaceable, touching rock) with Y in a configurable band. Config in `aqtweaks_thaumcraft.cfg`: `Enable Deep Vis Crystals` (true), `Deep Crystal Min Y` (-60), `Deep Crystal Max Y` (-1), `Deep Crystal Density Scale` (1.0). Spread, vis drain and flux pollution are Thaumcraft's own and have no Y limit. Confirmed in game. `[AQ-PERF]` counter `thaum.deepCrystals`. See [thaumcraft.md](thaumcraft.md).

## 2026-10-25 — 1.10: chunk gap recheck pacing and RTG light fix deferral

- **Chunk gap recheck pacing** (`world/GapRecheckPacer`, `mixin/lighting/MixinChunkGapRecheck`, early json). After the High City paste was fixed (3.4 s, profile `3heUXWJhoX`), the remaining stall was `Chunk.onTick` -> `Chunk.recheckGaps` (18.3 s): vanilla rechecks every flagged skylight column of a chunk in one server call (the client passes `onlyOne = true`). The server now rechecks one column per call and skips the call when the shared tick slice is spent (flags stay set, resume next tick), floor 2 columns per tick, near-player chunks use a larger slice. Config `Pace Chunk Gap Recheck` (true), `Gap Recheck Max Slice Ms` (10), `Gap Recheck Near Slice Ms` (30), `Gap Recheck Near Chunks` (3) in `aqtweaks_depths.cfg`. `[AQ-PERF]`: `light.gap.cols`, `light.gap.skipped`.
- **RTG light fix deferral and dedupe** (`world/RtgLightFixQueue`, `mixin/rtg/MixinRtgFixLightingDefer`). RTG's `fixLighting(World, ChunkPos)` for the four diagonal neighbours of each populated chunk (wool place and remove per column, 91% of its 28.2 s) is queued per chunk with repeat requests merged, and run once after the neighbourhood is quiet (`RTG Light Fix Settle Ticks`, 40), paced by the Chunk Steward slice (`RTG Light Fix Max Slice Ms` 15, near `RTG Light Fix Near Slice Ms` 30, `RTG Light Fix Near Chunks` 6, `RTG Light Fix Immediate Chunks` 2, `RTG Light Fix Max Age Ticks` 6000; switch `Defer RTG Light Fix` in `aqtweaks_rtg.cfg`). Flushed on chunk unload, world unload and server stop. `[AQ-PERF]`: `rtg.lightFix.calls`, `.repeat`, `.immediate`, `.run`, `.drain`, `.unloadFlush`.
- **Shared tick slice** (`pacing/TickSlice`): `DeferredRelight`, `PasteRelightQueue`, `RtgLightFixQueue` and the gap pacing start one clock per server tick and measure against it, so their combined work stays near the largest single limit.

## 2026-10-25 — 1.10: Dynamic Stealth server cost

- New `dynamicstealth/StealthPerf` with three trims, all configurable in the new `aqtweaks_dynamicstealth.cfg` (see [dynamicstealth.md](dynamicstealth.md)): a per-trace chunk cache for `ImprovedRayTracing.rayTraceBlocks` (`MixinImprovedRayTracing`, exact), a 3-tick memo of the entity tracker's `Sight.canSee` per (player, entity) (`MixinDSEntityTrackerEntry`, `@Redirect`; client entity visibility can lag up to 3 ticks; AI sight and the gauge untouched) and a 3-tick memo of `Sight.seenEntities` for the client visibility packet (`MixinDSVisibilityPacket`). No range, threshold or formula is changed. `FantasticLib-1.12.2.070.jar` added to `libs/` and the build script for `ImprovedRayTracing`. `[AQ-PERF]` adds `ds.trackerCanSee`, `ds.trackerMemoHit`, `ds.seenEntities`, `ds.seenMemoHit`.

## 2026-10-24 — 1.10: chunkgen Phase 0 and 3.1: biome cache, tunnel noise skip, relight probe

- `VillageLandHelper`: `isOceanBiome`, `isOceanOrRiverBiome`, `isBeachBiome` and `swampTagged` read a per-`Biome` bitmask cache (`ConcurrentHashMap`) instead of repeating `BiomeDictionary` lookups and `String.toLowerCase` per column. Same answers as before (the uncached bodies are kept as `*Uncached`). New `Verify Biome Class Cache` (`aqtweaks_rtg.cfg`, dev-only, default off) compares every cached answer to the uncached one and logs the first `[AQ-VERIFY]` mismatch. Target: `VillageCharmPaste.shouldSkip` (3.6 s / 9.7 s in the two Oct 23 profiles).
- `UpperTunnelNetwork.columnData`: B-noise (`type1B` / `type2B`) is sampled only from the first Y where the A noise reaches its threshold (no cell below it can dig, so the result is identical). Tunnel dig flags are `int` bitmasks instead of `boolean[27]`, and the four sample arrays are per-thread scratch. New `Verify Upper Tunnel Noise` (`depthsupdate` config, dev-only, default off) recomputes each column the old way and logs the first `[AQ-VERIFY]` mismatch.
- Deferred paste relight, near-player change. The first deferred test generated the High City inside the 4-chunk radius and the "replay at once near a player" rule ran the whole batch in the paste tick (profile: `PasteRelightQueue.enqueue` to `flushBatch` 10.0 s of the 12.65 s paste; the drain for other batches was 3.2 s over about 25 s). Now only columns within `Defer Template Relight Immediate Chunks` (default 1) of a player are relit in the paste tick. A paste touching a chunk within `Defer Template Relight Near Chunks` (4) is queued as a priority batch: it goes to the front and drains with `Defer Template Relight Near Slice Ms` (default 40, capped by `StewardPacing.sliceBudgetMs` headroom; 96 units per tick when Chunk Steward is absent). `paste.queued` detail now shows `immediate=N` and `,priority`.
- Deferred paste relight (`world/PasteRelightQueue`): the Bulk Template Relight replay no longer runs inside the paste tick. The first High City test with bulk mode alone cut `da.paste` from 26.0 s to 16.0 s, of which 13.2 s was the replay. Each paste is now queued as a batch and drained on world-tick END under the `StewardPacing` slice (`sliceBudgetMs`, `remainingMs(40)`, `getLoadFactor`, same pattern as `DeferredRelight`), nearest chunks to a player first, with a floor of 2 units per tick. Phase 1 relights every column, phase 2 runs the recorded checks. Pending work is flushed at once when a player is within `Defer Template Relight Near Chunks` (default 4) of any touched chunk, per chunk on `ChunkEvent.Unload` (fires before `saveChunkData`), per world on world unload, for everything on `FMLServerStoppingEvent`, and for batches older than `Defer Template Relight Max Age Ticks` (6000). New config in `aqtweaks_depths.cfg`: `Defer Template Relight` (true), `Defer Template Relight Max Slice Ms` (15), `Defer Template Relight Near Chunks` (4), `Defer Template Relight Max Age Ticks` (6000). `[AQ-PERF]` adds `paste.queued`, `paste.drain` (ms, units, pending), `paste.latency`, `paste.unloadFlush`, `paste.expired`. Block placement (about 3 s for the High City) still runs in the paste tick.
- Bulk Template Relight (`world/BulkTemplateRelight`, `mixin/lighting/MixinChunkBulkRelight` + `MixinTemplateBulkRelight`, plus a HEAD hook in `MixinWorldCheckLight`; early json). While a `Template.addBlocksToWorld` paste with a bounding volume of at least `Bulk Template Relight Min Volume` (default 4096) runs on the server, `Chunk.relightBlock` (highest Y per column) and `World.checkLight` (each position) are recorded instead of run, then replayed once when the paste returns: relight first, then the checks. Targets the Dungeon Additions High City paste, one 26.0 s call of which 22.6 s was per-block relight. Config `Bulk Template Relight` (default on) in `aqtweaks_depths.cfg`. `[AQ-PERF]` gains `paste.template` (time and size, bulk or plain), `paste.replay` (columns, checks, recorded relights) and `relight.calls` (count of relightBlock calls outside a bulk paste). No verify flag: confirm by visiting a pasted structure.
- New diagnostic `MixinRtgFixLightingProbe` (main json, no behaviour change): with `Performance Stage Timers` on, `[AQ-PERF]` gains `rtg.fixLightingAround` (calls and time) and `rtg.woolRelight` (count of RTG wool-trick relights; its time is 0 by design). Used to size the lighting work before changing RTG's relight path.

## 2026-10-19 — RTG 9.0.3 compatibility: `LatePopulatingChunk` seal target and mineshaft paste hook

- Updated RTG dependency from `RTG-1.12.2-7.3.3.6.jar` to `RTG-1.12.2-9.0.3.jar` in `build_gradle.ps1` (pruning 7.3.3.6 from `libs/`).
- Retargeted `MixinChunkGeneratorRTGVillage.aqtweaks$sealVillagePlateBeforeChunk` from `Chunk.<init>` to `LatePopulatingChunk.<init>` (which `ChunkGeneratorRTG.func_185932_a` now instantiates).
- Added `MixinChunkGeneratorRTGMineshaftPaste` (`mixins.aqtweaks.bettermineshafts.json`, `required: false`) at RETURN of `ChunkGeneratorRTG.generateStructure(World, Random, StructureBoundingBox, StructureStart)`. RTG 9.0.3 pastes mineshafts directly via `StructureComponent.func_74875_a` instead of calling `StructureStart.func_75068_a`, which bypassed `MixinStructureStartMineshaftBox`. The new hook restores Start AABB refreshing and `MineshaftSurfaceShaft.carveForStart` on RTG 9 chunks while keeping `MixinStructureStartMineshaftBox` for non-RTG dimensions. See [rtg.md](rtg.md) and [bettermineshafts.md](bettermineshafts.md).

## 2026-10-19 — Toolchain doc: MixinBooter / Fugue / Cleanroom runtime

- [compatibility-matrix.md](compatibility-matrix.md) Toolchain now lists the shipped `!mixinbooter-11.18.jar` and `+Fugue-0.24.4.jar` (was 11.13 / 0.23.7), the pack runtime Cleanroom 0.6.13-alpha, and that Fugue 0.25+ needs Cleanroom 0.7.3-alpha+ (on 0.6.x it surfaces as a misleading Music Triggers / Impossible Library `NoClassDefFoundError`).

## 2026-10-19 — Server module: GeographiCraft dedicated-server spawn crash

- New Server module ([server.md](server.md)). `MixinDimensionManager` (`mixins.aqtweaks.geographicraft.json`, `required: false`) redirects the client-only `Biome.func_185359_l` (`getBiomeName`) call in GeographiCraft's `DimensionManager.salvageSpawn` to the biome registry name (`"unknown"` if null). Dedicated servers no longer throw `NoSuchMethodError` while placing the first spawn. The name only fed a log line.
- `AQTweaksLateMixinLoader` overrides `shouldMixinConfigQueue(String)` and queues the GeographiCraft json only when `geographicraft` is loaded. The compile stub `zone/rong/mixinbooter/ILateMixinLoader` gains the matching default method.
- `villagegen_info.md` now names the shipped GeographiCraft **1.0** jar (was 0.9.8.5); the biome-list behavior it describes is unchanged in 1.0.

## 2026-10-19 — Default World Type to RTG in World Creation GUI

- In `GuiCreateWorld` (`MixinGuiCreateWorld`), the Create New World screen initializes with `selectedIndex` pointing to RTG (`WorldType.getName() == "RTG"`) instead of vanilla Default (0). Backed by client config key `Default World Type` under category `world creation` in `aqtweaks_client.cfg` (default `"RTG"`). Empty or unregistered names leave vanilla Default. Recreate World continues to copy the source world's type because `recreateFromExistingWorld` executes after constructor return. See [client.md](client.md).

## 2026-10-18 — Infernal Furnace smelts gated metals

- The Infernal Furnace cooks the ores and dusts whose vanilla furnace recipes `furnace_gated_metals.zs` removed: one ingot per cook, with a nugget and Rare Earth roll only when that ore does not already have that bonus. Gold and silver keep Thaumcraft's nuggets. Flag off does not restore the furnace recipes. See [thaumcraft.md](thaumcraft.md).

## 2026-10-18 — Village walls build down to sea level; DA StructureStart shadow fix & LibraryEx CME fix

- In `MixinChunkGeneratorRTGVillage`, village ocean/cliff walls are allowed to generate down to sea level (`y62`, `colFloor = min(sealFloor, 62)`) instead of stopping at `plateY - villagePlateDepth` (which was leaving open gaps and floating unsupported walls above the beach or water on high-elevation cliffs). Sealing of interior village columns is unchanged and still respects `plateY - villagePlateDepth` to preserve underground caves. See [rtg.md](rtg.md).
- `MixinDaStructureStart`: removed `@Shadow(remap = false)` on `StructureStart.field_75075_a` and `field_75074_b` (which missed MCP-named superclass fields under Cleanroom and disabled the mixin on all 14 targets) in favor of casting `(StructureStart) (Object) this` and calling `getComponents()` / `getBoundingBox()`. DA village-overlap skip and `da.init` / `da.paste` stage timers apply again. See [rtg.md](rtg.md).
- New `MixinEntityHelper` (`mixins.aqtweaks.libraryex.json`, `required: false`) redirects `Map.computeIfAbsent` in LibraryEx's `EntityHelper.getEntityLocation` and synchronizes on the cache map so concurrent client/server living-entity ticks (e.g. NetherEx freeze checks) do not throw `ConcurrentModificationException` under Java 25. See [README.md](README.md) and [compatibility-matrix.md](compatibility-matrix.md).

## 2026-10-17 — Deferred relight uses average headroom

- `DeferredRelight` no longer stops at `remainingMs(40)` on world-tick end, which was clamping near torches to 4 checks whenever that tick had already used ~40 ms even at a solid 20 TPS. One slice per server tick is `min(Deferred Relight Max Slice Ms, headroom)` at full TPS (default 8 ms, not raised when headroom is smaller). Below 20 TPS it uses remaining time against 40 ms. Under half load factor the slice is 0. The 4-check near floor still runs. Distant emitters keep a 1-check/20-tick floor when the slice is already used up, and that floor is all they get while optional work is paused.
- `shouldPauseOptionalWork()` is true when the server is behind, or when chunks are generating and average headroom is under 4 ms. At 20 TPS with at least 4 ms free, Somnia's already-lit 20-tick re-check runs while chunks trickle in. `!isLightPopulated()` is still every tick. A missing or stale Chunk Steward jar skips the slice clock. See [rtg.md](rtg.md) and [somnia.md](somnia.md).

## 2026-10-17 — First chunk light pass is not paced

- `SomniaOptimizationHandler` runs `chunk.checkLight()` every tick while `!chunk.isLightPopulated()`, including when Chunk Steward reports busy. `PlayerChunkMap` only sends a chunk after that first pass, so the 20-tick throttle and `shouldPauseOptionalWork()` were holding new chunks invisible until generation stopped. Those two gates stay on already light-populated re-checks. The Somnia sleep branch is unchanged. `DeferredRelight` only retries emitter flood-fills after a chunk can already be sent, so its busy skip stays. See [somnia.md](somnia.md).

## 2026-10-16 — Depths HeightManager cache and village well cache/memo

- **Part A (`HeightManager` cache):** New `MixinHeightManager` (`mixins.aqtweaks.depthsupdate.json`, `required: false`, `defaultRequire: 1`) and `depths/HeightCache` replace `HeightManager.get(int)` and `isExtended(int)` with a dimension-indexed array (`-64..191`, size 256) and hash-map fallback outside that range, invalidated on `HeightManager.initialize` and Depths config reload. Eliminates the 14.37% Server-thread `ConcurrentHashMap.computeIfAbsent` hotspot in Spark. Configurable via `Enable HeightManager Cache` and `Verify HeightManager Cache` in `aqtweaks_depths.cfg`. See [depths.md](depths.md).
- **Part B (`VillageWellCache`):** New `rtg/VillageWellCache` caches `VillageLandHelper.chunkExists` per dimension (peeking `ChunkProviderServer.loadedChunks` via `AccessorChunkProviderServer` without touching `unloadQueued`, checking a permanent positive `LongOpenHashSet` and a short-TTL negative `Long2LongOpenHashMap` default 200 ticks before calling `World.isChunkGeneratedAt`) and memoizes `startRejectReason` / `findDryWell` with a short TTL (default 600 ticks) keyed by `(dimension, seed, configFingerprint)`. A `null` (allow) verdict is only cached/used while no `StructureStart` exists at that well chunk so `forgetRejectedStarts` still vets newly created starts into `VETTED_STARTS`. `StageTimer` records `village.wellReject` and `village.findDryWell` with distinct/repeat/memoHit counters. Configurable via 5 new keys in `aqtweaks_rtg.cfg`. See [rtg.md](rtg.md).

## 2026-10-16 — Chunk Steward headroom pacing & worldgen stage timers

- New `pacing/StewardPacing` runtime bridge to `ChunkStewardAPI` (compile-hard against `ChunkSteward-0.3.jar`, `after:chunksteward` on `@Mod`, inner static `Api` class loaded only when `chunksteward` is present with `LinkageError` fallback).
- `DeferredRelight` paces near-player emitter checks against `StewardPacing.remainingMs(40.0)` with a 4-check/tick floor, and limits distant (>96 blocks) emitters to a 1-check/20-tick floor when `StewardPacing.shouldPauseOptionalWork()` is true so they cannot starve indefinitely.
- `SomniaOptimizationHandler` skips non-priority 20-tick ambient light checks when `StewardPacing.shouldPauseOptionalWork()` is true; priority chunks within 2 chunks of an awake player still drain every tick.
- Step 0 worldgen stage timer (`perf/StageTimer`, gated behind `Performance Stage Timers` in `aqtweaks_rtg.cfg`, default off) logs 5-second summaries (`[AQ-PERF]`) for village layout, flatten, seal, paste sub-stages, and Dungeon Additions `Start` `create` vs `func_75068_a`.
- `/aqvillage` (`CommandAqVillage.nearestUnexplored`) logs a stopwatch breakdown of total ms, cells scanned, `isChunkGeneratedAt`, `canSpawnVillage`, and `resolvedWellForChunk`.

## 2026-10-16 — Dungeon Additions village overlap skip

- When `Skip Structures On Village` is on, Overworld Dungeon Additions structure starts (`WorldGenFrozenCastle$Start`, `WorldGenLichTower$Start`, `WorldGenOutposts$Start`, and the other 11 DA worldgen starts) cancel `generateStructure` if their bounding box overlaps a village pad + Y volume. No retry. Nether, End, and Aether DA structures are unaffected. Optional `mixins.aqtweaks.da.json`. New chunks only. See [rtg.md](rtg.md).

## 2026-10-16 — Mineshaft mouth stone-brick collar

- The fallback surface shaft's one-block oak ring is now a 3×3 stone-brick collar five blocks deep (shorter shafts stop at the tunnel). The center stays the hole and ladder, so a hillside no longer slices the mouth into a plank lip. New chunks only. See [bettermineshafts.md](bettermineshafts.md).

## 2026-10-06 — Version 1.9

- `build.gradle` `version`, `ArcanaQuestTweaks.VERSION` and `mcmod.info` (via `${version}`) are now **1.9**. Curse already holds 1.8, so the file for the upload must be a new version; everything dated 2026-10-04 to 2026-10-06 below ships in 1.9. No rebuild was done for the bump: the next `.\build_gradle.ps1` produces `ArcanaQuestTweaks-1.9.jar`.

## 2026-10-04 — Surface indoor insulation and boat wetness

- `DynamicModifierInsulation` pulls biome, time, altitude, snow, and Serene Seasons temperature toward neutral when the sampled position is sheltered, covering the surface houses Simple Difficulty's `Y >= 64` underground cutoff leaves exposed. Heaters, chillers, armor, baubles, held items, and wetness keep full effect indoors.
- Shelter is an opaque **or glass** roof plus, by default, at least 3 of 4 cardinal collision walls. Glass ceilings count because glass does not raise the light heightmap. Serene Seasons greenhouse glass shifts the target from 12 to 14 — more heating in cold, less cooling in hot — matching Serene Seasons' own 7-block, ignore-what's-between scan so warmth and crop fertility agree. Greenhouse glass is read by registry name, so no Serene Seasons jar is added.
- `MixinWorldUtil` lifts the temperature sample one block when a player rides a boat over water, so an open boat in clear weather no longer reads as wet `-6`. Rain, submerged boats, swimming, and other mounts are unchanged. Seven new `aqtweaks_stamina.cfg` keys. New module doc: [temperature.md](temperature.md).

## 2026-10-04 — Somnia wake no longer desyncs the mouse

- Somnia's wake packet closes the sleep screen on the network thread, so the cursor grab did not take effect and the camera stopped following the mouse until a click or Escape. `MixinMinecraftMouseGrab` now re-queues any off-thread `displayGuiScreen` onto the client thread.

## 2026-10-06 — Server relight queue skips liquids, nearest first

- `DeferredRelight` ignores liquid emitters (underground lava made up most of the 10,000+ queued per minute) and retries nearby emitters (within 96 blocks of a player) first; distant ones use leftover budget. Same rule as the client sweep.

## 2026-10-06 — Client relight sweep skips lava, prioritizes nearby

- First version's queue sat at its 8192 cap with only 0 to 52 checks run per minute: underground lava was flagged stale by the thousand and blocked real torches. The sweep now skips liquid emitters, caps 64 stale cells per section, remembers positions it already checked, drops the oldest when full, and runs checks nearest-first (within 128 blocks).

## 2026-10-06 — Client relight sweep

- New client-only `ClientRelightSweep`: when a chunk arrives, emitters (torches, lanterns, lava) whose stored light, or an open neighbor's, is lower than they should give get a `World.checkLight` on the client thread. Fixes village torches and lanterns (Charm perimeter lights) that stayed dark on the client until a torch was placed nearby. Config `Enable Client Relight Sweep` default on, `Client Relight Debug` default off.

## 2026-10-06 — Deferred relight diagnostics

- New `Deferred Relight Debug` (rtg cfg, default off): `[AQ-RELIGHT]` log line per minute per world with lit-at-placement, queued, retried, expired and overflow counts and sample coordinates. For diagnosing village torches that stay dark in new chunks.

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
