# Somnia module (1.8)

Last updated: 2026-09-25.

Optional mixin layer if Somnia Refreshed is present (`mixins.aqtweaks.somnia.json`, `required: false`).
Configured via `config/arcanaquesttweaks/aqtweaks_somnia.cfg`.

## Locked intent

1. **Chunk Light Bottleneck Optimization**: Eliminate the server tick performance bottleneck caused by Somnia Refreshed's ASM light checks interacting with Depths Update's expanded chunk height (Y = -64 to 320).
   - Ensure `chunk.checkLight()` is never invoked more than once per tick for any given chunk.
   - When sleep is active, respect `disableMoodSoundAndLightCheck`.
   - When awake, prioritize backlogged chunks (`queuedLightChecks < 4096`) within player proximity ($\le 2$ chunks / ~32 blocks) to drain every tick so village houses and immediate surroundings relight within seconds without requiring a torch.
   - For distant chunks ($> 2$ chunks away) or chunks whose lighting queue has reached idle (`>= 4096`), throttle `checkLight` to once every 20 ticks (1s) round-robin. This avoids server tick hitches and stuttering during high-speed exploration or chunk generation.
2. **3-Tier SMP Sleep System**:
   - **Case A (100% Sleeping)**: Full fast-forward simulation. All world, tile, entity, and player ticks accelerate together.
   - **Case B (50%–99% Sleeping)**: Time-only fast-forward. Vanilla `WorldServer.tick` already ran (`+1` world time, one fatigue recover). Tweaks adds `(caseBTimeMultiplier - 1)` extra time and fatigue so default `2.0` is **double per world tick**, not triple. Awake players and mobs stay at 20 TPS; no extra entity/tile ticks.
   - **Case C (<50% Sleeping)**: Normal daylight cycle (no time acceleration). Sleeping players rest in bed and recover fatigue slowly at 10 points per in-game hour.
3. **Calibrated Fatigue Recovery**:
   - 1 in-game hour = 1,000 ticks.
   - 10 points of fatigue recovered per in-game hour (0.01 per tick) across all sleep states, configurable via `fatigueRecoveredPerHour` (default 10.0).
   - Explicitly overrides and ignores Somnia's parent `fatigueReplenishRate`, `fasterWorldTime`, and `fasterWorldTimeMultiplier`.
4. **Morpheus-Style Notifications**:
   - Bed enter: `<Player> is now sleeping. [<sleeping>/<total> (<pct>%)]`
   - Sleep activation: `[Somnia] 1/1 (100%) of players are sleeping! Fast-forwarding the night...` (live non-spectator count, not the cfg threshold)
   - Premature bed exit: `<Player> has left their bed. [<sleeping>/<total> (<pct>%)]`
   - Morning wakeup: suppressed to prevent spam.

## How the parent works

- Somnia Refreshed (`Somnia-1.0.1.jar`) redirects `Chunk.onTick` to `SomniaUtil.chunkLightCheck`.
- In `ServerTickHandler.tickStart()`, Somnia queries `SomniaState.getState(this)` and runs `doMultipliedTicking()` when `ACTIVE`.
- `SomniaState.getState()` natively required 100% sleeping and included spectators.
- In `doMultipliedTicking()`, Somnia called `doMultipliedServerTicking()` in a multiplied loop, ticking world entities and tiles at super-speed.

## Design plan

- `MixinSomniaUtil` (@Overwrite) delegates to `SomniaOptimizationHandler.chunkLightCheck(chunk)`.\
- `MixinSomniaState` (@Overwrite) delegates to `SomniaSleepHandler.getState(handler)` to filter spectators, evaluate the 50% threshold, route to `ACTIVE` vs `WAITING_PLAYERS`, and broadcast activation.
- `MixinServerTickHandler` (@Inject at HEAD of `doMultipliedTicking`) intercepts multiplied ticking. During Case B, it steps `worldServer.setWorldTime` by `(caseBTimeMultiplier - 1)` (vanilla world tick already applied `+1`), recovers that extra fatigue on sleepers, syncs `SPacketTimeUpdate`, and cancels the full entity/world tick loop. During Case A (100% sleeping), it lets normal multiplied ticking proceed.
- `SomniaSleepHandler` listens to Forge `PlayerTickEvent`, `PlayerWakeUpEvent`, `PlayerLoggedOutEvent`, and `PlayerChangedDimensionEvent` for Morpheus chat notifications and state tracking.
- Missing Somnia jar → mixin config skipped gracefully (`required: false`).

## Files

| Piece | Role |
| --- | --- |
| `somnia/SomniaOptimizationHandler.java` | Core lighting check optimization, duplicate tracker, sleep check, and proximity-priority awake drain |
| `somnia/SomniaSleepHandler.java` | 3-tier sleep state logic, Case B time advancement & fatigue recovery, and Morpheus notifications |
| `mixin/somnia/MixinSomniaUtil.java` | `@Overwrite SomniaUtil.chunkLightCheck` |
| `mixin/somnia/MixinSomniaState.java` | `@Overwrite SomniaState.getState` |
| `mixin/somnia/MixinServerTickHandler.java` | `@Inject doMultipliedTicking` to handle Case B time-only advancement |
| `mixin/vanilla/AccessorChunk.java` | Direct accessor for `queuedLightChecks` (`field_76649_t`) on `Chunk` |
| `mixins.aqtweaks.somnia.json` | Optional late mixin config (`required: false`) |
| `mixins.aqtweaks.vanilla.json` | Required late mixin config for vanilla accessors/mixins |

## Live config (`config/arcanaquesttweaks/aqtweaks_somnia.cfg`)

| Category / Key | Type | Default | Description |
| --- | --- | --- | --- |
| `Enable Somnia Module` | boolean | `true` | Master toggle for AQTweaks Somnia enhancements |
| `Sleep Percentage Threshold` | double | `0.50` | Percentage of non-spectator players required to activate fast-forward (0.01 - 1.0) |
| `Case B Partial Sleep Time Multiplier` | double | `2.0` | Total world-time / sleeper-fatigue rate vs one vanilla world tick during Case B (Tweaks extra = multiplier − 1) |
| `Fatigue Recovered Per In-Game Hour` | double | `10.0` | Fatigue points recovered per in-game hour (1,000 ticks) in bed |
| `Enable Sleep Notifications` | boolean | `true` | Morpheus-style chat notifications |

## Do not regress

- Do not put Somnia mixins in **required** `mixins.aqtweaks.json`.
- Do not add `@Mod required-after:somnia`.
- Do not import Somnia classes in `ArcanaQuestTweaksConfig` or always-on event handlers.
- Do not disable Somnia's daytime / anytime sleep capability (`SleepingTimeCheckEvent` remains untouched).
- Do not check `queuedLightChecks > 0` directly for backlogs (4096 is idle). Do not use `queuedLightChecks < 4096` as an uncapped global flush every tick across all loaded chunks.

## Verify

1. Build succeeds with `options.release = 21` via `build_gradle.ps1`.
2. Spark profile confirms `SomniaUtil.chunkLightCheck` stays small while flying awake; new chunks near player relight within a few seconds of arrival.
3. Sleeping with 100% players in bed runs Case A full acceleration.
4. Sleeping with $\ge 50\%$ but $< 100\%$ players in bed runs Case B: world time `+2` and sleeper fatigue `2 × (fatigueRecoveredPerHour / 1000)` per world tick; awake players stay at 20 TPS (no extra entity/tile ticks).
5. Sleeping with $< 50\%$ players in bed runs Case C (time does not speed up, resting in bed clears 10 fatigue per in-game hour).
6. Chat: bed enter `[sleeping/total (pct%)]`; activation uses the same live count (solo `1/1 (100%)`, not cfg `50%`); early bed leave `[sleeping/total (pct%)]`.
