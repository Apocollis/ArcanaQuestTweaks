# Dynamic Stealth server cost (1.10)

Tweaks trims the server cost of Dynamic Stealth (parent `dynamicstealth`, jar `DynamicStealth-1.12.2.120.jar`, plus `FantasticLib-1.12.2.070.jar` for `ImprovedRayTracing`; both compile-hard in `libs/`). **Nothing here changes a range, a threshold or a formula.** View ranges are not lowered. Rift tracking is handled separately (see [portal.md](portal.md) and `MixinDSEntityTrackerEntry`).

## Where the cost was (296 s capture, 2026-10-23)

`Sight.visualStealthLevel` 18.5 s (6.3%): the entity tracker (`EntityTrackerEdit` to `DSEntityTrackerEntry.func_180233_c` to `Sight.canSee`) 12.4 s, the client visibility packet (`Network.VisibilityPacket` to `Sight.seenEntities`) 5.2 s. Inside the sight check, the ray trace is 14.6 s, 9.4 s of it `World.getBlockState` (4.3 s chunk lookups). AI sight is under 1%. Dynamic Stealth's config has no sight-check frequency setting.

## What Tweaks does

All in `dynamicstealth/StealthPerf.java`; mixins in `mixins.aqtweaks.dss.json` (`required: false`).

| Change | Mixin | Effect |
| --- | --- | --- |
| Ray chunk cache | `MixinImprovedRayTracing` (HEAD/RETURN on `rayTraceBlocks(World, Vec3d, Vec3d, int, boolean, boolean)`, `@Redirect` of its `World.getBlockState` calls) | Within one trace the last chunk is kept and read directly. Equivalent to `World.getBlockState` (outside-build-height check, then the chunk's `getBlockState`). Exact. |
| Tracker sight memo | `MixinDSEntityTrackerEntry` (`@Redirect` of the `Sight.canSee` call in `func_180233_c`, after the existing rift override) | Reuses the answer per (player, entity) for `Tracker Sight Memo Ticks` (default 3) while neither moved more than a block, the dimension and entity are the same. An entity can appear or disappear on the client up to that many ticks late. **Only the tracker call is memoised**; AI sight and the stealth gauge still call the real `Sight.canSee`. |
| Visibility packet memo | `MixinDSVisibilityPacket` (`@Redirect` of `Sight.seenEntities` in the `VisibilityPacket` constructor) | Reuses the per-player seen map for `Visibility Packet Memo Ticks` (default 3). Client gauges/outlines can lag by that many ticks. |

## Config (`aqtweaks_dynamicstealth.cfg`)

| Name | Default | Live? | Meaning |
| --- | --- | --- | --- |
| Ray Chunk Cache | true | yes | Chunk kept across the steps of one trace |
| Tracker Sight Memo Ticks | 3 | yes | 0 = off. 20 max |
| Visibility Packet Memo Ticks | 3 | yes | 0 = off. 20 max |

`[AQ-PERF]` (with `Performance Stage Timers`): `ds.trackerCanSee` (real `canSee` calls and time), `ds.trackerMemoHit` (reused answers), `ds.seenEntities`, `ds.seenMemoHit`.

## Files

| File | Role |
| --- | --- |
| `dynamicstealth/StealthPerf.java` | Memos (weak per-player maps, bounded and pruned every 200 ticks) and the ray chunk cache (thread-local, cleared at the end of each trace) |
| `mixin/dss/MixinImprovedRayTracing.java`, `MixinDSVisibilityPacket.java`, `MixinDSEntityTrackerEntry.java` | Hooks above |

## Do not regress

* The rift override stays first in `func_180233_c`; the memo never applies to rifts.
* Do not memoise `Sight.canSee` for AI callers (`EntitySensesEdit`, `AIWatchClosestEdit`) or the gauge.
* Do not change Dynamic Stealth's ranges, thresholds or multipliers.
