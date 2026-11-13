# Nether hazards: lava spouts and ash clouds

Last updated: 2026-11-09. Vanilla Nether (`WorldProviderHell`), so no parent jar and the handlers register unconditionally. Tweaks cfg: `config/arcanaquesttweaks/aqtweaks_nether.cfg`. Overview of all dimension hazards: [dimension-hazards.md](dimension-hazards.md). Scheduling and the event packet: [hazards.md](hazards.md) (channels `spout` id 1 and `ash` id 2).

The Nether already has Simple Difficulty heat (`dimensionTemperature.json`); these two hazards are timed events on top of it, on **separate timers** that may overlap.

## Lava spouts (`nether/LavaSpoutHandler`)

* **Event:** a 30 s window (`Spout Window Seconds`) after a calm gap of 3 to 6 minutes (`Spout Interval Min/Max Seconds`). The warning is the **tremor** (`Spout Tremor Seconds`, 6): a rising lava rumble, bubbling lava surfaces near the player and a very small camera shake (`client/nether/SpoutTremor`, reads the hazard event, so a command-started event shakes the same).
* **Eruptions:** at the start of the window each survival player gets 3 to 6 eruptions (`SpoutSchedule.draw`): the first within 3 s, each next 2 to 5 s later, all starting inside the window. Each one, when due, picks a lava **source** with air above within 6 to 24 blocks of the player (`SpoutSitePicker` acceptance plus a loaded-chunk probe, at most `Spout Probes Per Eruption`, not within 10 blocks of another active site, at most `Max Active Spouts`), sends `PacketSpoutTelegraph` (id 10) and erupts 3 s later (client bubbling and smoke, then a burst of flame, lava and smoke).
* **Damage:** every living entity in a column (radius `Spout Column Radius`, height 12) takes `Spout Damage` (`aqtweaks_spout`, armor-bypassing fire damage) and is set on fire, unless it has the `Fire Resistance Effect` (default `minecraft:fire_resistance`) or is immune to fire. The pack's SetBonus gives that effect to Black and Orange Salamander Hide, Fiery, Phoenix and Ashen sets.
* **Thrown lava (`SplashPatch`, `SplashQueue`):** up to `Splashes Per Eruption` patches 5 to 8 blocks from the spout: `FLOWING_LAVA` at `Splash Center Level` (4), level 5 on the four neighbours and level 6 on the next diamond ring (centre level 5 skips the outer ring); vanilla adds a level 7 ring. Never a source block. Site rule: in the radius-4 diamond around the centre there must be no liquid and no air cell without a solid full-cube floor; otherwise the patch is skipped. Every placed cell goes into `SplashQueue` (saved data `aqtweaks_splash`, a position list with expiry ticks). Cells are cleared after `Splash Lifetime Seconds`, when their chunk unloads, and after a restart once their chunk loads, but only if they are still `FLOWING_LAVA` or `LAVA` with `LEVEL > 0`; a source is never touched. `Max Thrown Lava Cells` bounds the total. `Throw Lava` off leaves flame, noise and damage only.
* **Block reads** peek `ChunkProviderServer.loadedChunks` (`LoadedBlocks`, through `AccessorChunkProviderServer`), so no probe loads or generates a chunk or pins one against unloading.

## Ash clouds (`nether/AshCloudManager`)

* **Event:** every 8 to 15 minutes (`Ash Interval Min/Max Seconds`), lasting 4 to 6 minutes (`Ash Duration Min/Max Seconds`). At its start one cloud spawns near every survival player not already inside one (cap `Max Ash Clouds Per World`). It grows in over 15 s and fades over the last 20 s.
* **Gas, not an ellipsoid:** a cloud has a drifting centre (`Ash Cloud Speed`) and a reach (`Ash Cloud Reach Horizontal/Vertical`), and its exact cell set is a six-neighbour flood fill (`AshCloudField`) of open cells inside the reach, rebuilt about once a second in slices (`Gas Cells Per Tick`, one budget for the whole Nether, split evenly across the clouds that are filling; `FillBudget`) with a node cap (`Gas Max Cells`). Seeds come from the previous set, so the cloud cannot jump a wall when the centre crosses it. `GasPermeability` decides open: air, plants, ladders, fire, partial blocks and open doors, gates and trapdoors (by their `OPEN` property); blocked: liquids, full cubes, closed doors, gates and trapdoors, and the `Gas Blocking Blocks` list (glass, panes, iron bars). An unloaded neighbour is blocked.
* **Effects, once a second:** inside the cloud the dose rises by the gear rate (`Ash Dose Gain Exposed/Armored/Proper`: 1.0 / 0.6 / 0.25) and outside it falls at twice that. Dose 5 gives Slowness I and Wither I, 12 gives Wither II and short Blindness pulses, 20 gives Wither III (`Ash Dose Threshold 1/2/3`). A stronger Wither already on the player is not replaced. Ash lung from the first second: an air debt drains the air bar, then 2 drowning-type damage a second (`aqtweaks_ash`). Gear tier is `weather/GearTierRule`: EXPOSED if an armor slot is empty, PROPER with `Wither Ward Effect` (`rustic:wither_ward`, from the pack's SetBonus on Wither Bone and Black Salamander sets, and from Mead) with four armor pieces, or four pieces on `Ash Proper Gear Whitelist`, else ARMORED. Dose and debt live in memory only (cleared on logout, death and dimension change).
* **Client:** `PacketAshCloud` (id 11) sends centre, velocity, reach and a coarse 4-block occupancy bitmap on spawn, about once a second, on login and dimension change, and once when it ends. `client/nether/AshCloudRenderer` moves the centres by velocity, spawns dark smoke only in occupied cells that are air in the client chunk data (so ash pours through doorways and never appears in sealed rooms), plus falling flakes and a muffled wind loop; the spawn budgets are rates (`Ash Max Particles Per Cloud/Total` divided by the 60 tick lifetime), not live counts. `AshFogHandler` closes the fog while the camera is inside, applied by `MixinEntityRendererFog` after every fog event handler.

## Test command

`/netherhazard <spout|ash|clear|status> [seconds] [skipWarning]` (OP): `spout` and `ash` start an event through the hazard engine (a spout event warns with its tremor unless `skipWarning`); `clear` ends both, removes all clouds and clears all tracked thrown lava; `status` prints both channels, the cloud count with their cell counts, the tracked lava count, active spouts and the caller's dose.

## Files

| Piece | Role |
| --- | --- |
| `nether/NetherHazards.java` | The two channels and the registration |
| `nether/LavaSpoutHandler.java`, `SpoutSchedule.java`, `SpoutSitePicker.java`, `SplashPatch.java`, `SplashQueue.java`, `LoadedBlocks.java` | Spout scheduling, site rules, thrown lava, loaded-chunk reads |
| `nether/AshCloudManager.java`, `AshCloud.java`, `AshCloudField.java`, `GasPermeability.java`, `AshDoseTracker.java` | Ash clouds, flood fill, permeability, dose |
| `nether/ClientSpouts.java`, `ClientAshClouds.java`, `network/PacketSpoutTelegraph.java`, `network/PacketAshCloud.java` | Client copies and packets |
| `client/nether/SpoutEffects.java`, `SpoutTremor.java`, `AshCloudRenderer.java`, `AshFogHandler.java`, `client/hazard/HazardLoopSound.java` | Client effects |
| `weather/GearTierRule.java`, `nether/CommandNetherHazard.java` | Shared tier rule, command |

## Do not regress

* Never read blocks with `World.getBlockState` or `getLoadedChunk` for the probes, the flood fill or the lava sweep: they clear the chunk's unload flag. Use `LoadedBlocks.peek`.
* Thrown lava is only ever `FLOWING_LAVA`, never a source, and only removed when `LEVEL > 0`.
* The flood fill never starts from an open cell that was not in the previous set (it must not jump a wall).
* Ash dose and clouds are memory-only; only the thrown-lava queue is persisted.

## Config key reference

| Key | Default | Note |
| --- | --- | --- |
| Enable Lava Spouts | `true` | Timed eruption events in the Nether: lava lakes throw flame and temporary flowing lava near players. |
| Enable Ash Clouds | `true` | Timed withering ash clouds in the Nether that flow through open space and build a Wither dose. |
| Spout Interval Min Seconds | `180` |  |
| Spout Interval Max Seconds | `360` |  |
| Spout Window Seconds | `30` | Length of one eruption event in seconds. |
| Spout Tremor Seconds | `6` | Warning before an eruption event: rumble, bubbling lava and a faint camera shake. |
| Tremor Camera Shake | `true` | Client: very small camera shake during the tremor. |
| Tremor Max Particles | `60` | Client: cap on lava bubble particles spawned per tick at full tremor. |
| Spout Min Per Player | `3` |  |
| Spout Max Per Player | `6` |  |
| Spout Stagger Min Seconds | `2.0` |  |
| Spout Stagger Max Seconds | `5.0` |  |
| Spout Range | `24` |  |
| Spout Min Distance | `6` |  |
| Spout Probes Per Eruption | `40` |  |
| Max Active Spouts | `8` |  |
| Spout Column Radius | `1.5` |  |
| Spout Damage | `4.0` |  |
| Throw Lava | `true` | Eruptions place temporary flowing lava nearby. Off leaves flame, noise and damage only. |
| Splash Center Level | `4` | Lava level of the splash centre: 4 reaches about 3 spaces, 5 about 2. |
| Splashes Per Eruption | `2` |  |
| Splash Lifetime Seconds | `20` |  |
| Max Thrown Lava Cells | `96` |  |
| Fire Resistance Effect | `minecraft:fire_resistance` | Potion registry name that fully protects against the spout column. |
| Ash Interval Min Seconds | `480` |  |
| Ash Interval Max Seconds | `900` |  |
| Ash Duration Min Seconds | `240` |  |
| Ash Duration Max Seconds | `360` |  |
| Ash Cloud Reach Horizontal | `20.0` |  |
| Ash Cloud Reach Vertical | `16.0` |  |
| Ash Cloud Speed | `1.5` | Drift speed of a cloud centre in blocks per second. |
| Gas Cells Per Tick | `3000` | Flood-fill budget per tick for the whole Nether, shared by all filling clouds |
| Gas Max Cells | `30000` |  |
| Gas Blocking Blocks | `(list)` | Registry names (wildcards supported) that block the gas even though they are not full cubes. |
| Max Ash Clouds Per World | `8` |  |
| Ash Dose Gain Exposed | `1.0` |  |
| Ash Dose Gain Armored | `0.6` |  |
| Ash Dose Gain Proper | `0.25` |  |
| Ash Dose Threshold 1 | `5.0` | Dose for Slowness I and Wither I. |
| Ash Dose Threshold 2 | `12.0` | Dose for Wither II and short Blindness pulses. |
| Ash Dose Threshold 3 | `20.0` | Dose for Wither III (the cap). |
| Wither Ward Effect | `rustic:wither_ward` | Potion registry name that marks proper ash gear (the pack's SetBonus gives it to Wither Bone and Black Salamander sets). |
| Ash Proper Gear Whitelist | `new String[0]` | Item registry names (wildcards supported). Four armor slots from this list count as proper ash gear without the effect. |
| Enable Spout Effects | `true` | Client: spout telegraph, eruption and tremor effects. |
| Enable Ash Particles | `true` | Client: draw the ash cloud as particles. |
| Ash Max Particles Per Cloud | `600` |  |
| Ash Max Particles Total | `2500` |  |
| Ash Render Distance | `64` |  |
| Ash Flake Rate | `8` | Client: falling ash flakes spawned per tick inside a cloud. |
| Enable Ash Fog | `true` | Client: local fog while the camera is inside a cloud. |
| Enable Ash Sound | `true` | Client: muffled wind while inside a cloud. |
| Nether Hazard Debug | `false` | Log spout sites, rejected sites and cloud spawns. |
