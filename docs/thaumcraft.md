# Thaumcraft module

Config: `config/arcanaquesttweaks/aqtweaks_thaumcraft.cfg`. Registers only if `thaumcraft` is loaded.

## What Tweaks does

Adds pack-side warp **sources and sinks** that Thaumcraft does not have: first visit to a dimension, lingering in configured dimensions / deep underground / Roguelike dungeons, and reducing warp after a successful sleep. Comfort (separate module) also drains temp warp while “homestead” resting.

## How the parent mod works

Thaumcraft 6 stores warp on the player capability `IPlayerWarp`:

- `EnumWarpType.NORMAL` — sticky
- `TEMPORARY` — decays
- `PERMANENT` — research/eldritch, Tweaks rarely touches except Bewitchment ritual wrapper

API (used via reflection so Tweaks compiles if TC is absent at runtime):

- `ThaumcraftCapabilities.getWarp(player)`
- `IPlayerWarp.get / add / reduce(EnumWarpType, int)`
- `IPlayerWarp.sync(EntityPlayerMP)` — must run after server-side changes or the client HUD is stale

Thaumcraft itself awards warp from research, eldritch, flux, etc. Tweaks does not replace that; it only **adds** environmental/sleep rules.

Sounds like `thaumcraft:whispers` are TC’s registry names played through vanilla `SoundEvent`.

## How Tweaks hooks in

`ThaumcraftHelper` lazy-inits those methods. `ThaumcraftModule` uses Forge events:

| Event | Behavior |
| --- | --- |
| `PlayerLoggedInEvent` | Init persisted NBT `VisitedDimensions` to current dim so login is not “first visit” |
| `PlayerChangedDimensionEvent` | If dim not in the list, append. Waits 2s (40 ticks off-thread before scheduling to server) so dimension loading and teleport transitions stabilize, then: add normal/temp warp, sync, sound (`thaumcraft:whispers`), and send chat message |
| `PlayerWakeUpEvent` | If not `wakeImmediately` and it is daytime: reduce normal/temp warp per cfg, sync, send chat message |
| `PlayerTickEvent` every 400 ticks (~20s) | Exposure: finds the shortest interval among configured dimension, Y ≤ threshold ($Y \le 30$), or `ChunkProviderServer.isInsideStructure(..., "RoguelikeDungeon", pos)`. Accrues `WarpExposureProgress` in persisted NBT; at interval awards 1 temp warp and plays whispers. When not exposed, progress decays steadily (at 1.5× the accrual rate) toward 0 rather than wiping instantly |

Dungeon check relies on vanilla structure lookup. Roguelike Dungeons Arcana registers that structure name. Village detection pad ([rtg.md](rtg.md)) is a different `isInsideStructure` path.

## Files

- `thaumcraft/ThaumcraftModule.java`
- `thaumcraft/ThaumcraftHelper.java`

## Do not regress

- Always `syncWarp` after add/reduce on the server.
- Dimension warp is **first visit only** (persisted array).
- Sleep must be a real night sleep (`wakeImmediately` false, daytime).
- Off-thread sleep then `addScheduledTask` — do not call TC API directly from worker thread.
- Exposure decay ensures players who briefly step into an exposed area don't instantly build permanent progress if they leave.
