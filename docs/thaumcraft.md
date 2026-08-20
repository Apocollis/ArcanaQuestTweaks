# Thaumcraft module (1.6)

Last updated: 2026-08-20.

Config: `config/arcanaquesttweaks/aqtweaks_thaumcraft.cfg`. Event handler registers only if `thaumcraft` is loaded (`CommonProxy.init`). Warp API is reflection (`ThaumcraftHelper`) so the rest of Tweaks still compiles if TC is absent from the workspace.

Comfort homestead drain is a **different** NBT key and module ([comfort.md](comfort.md)). Bewitchment ritual warp is [bewitchment.md](bewitchment.md). Village `isInsideStructure` padding is [rtg.md](rtg.md) — not this dungeon check.

## Locked intent

Add pack-side warp **sources and sinks** Thaumcraft does not have: first visit to a dimension, lingering in configured dimensions / deep underground / Roguelike dungeons, and reducing warp after a successful night sleep. Do not replace TC research/flux/eldritch warp.

## How the parent mod works

Thaumcraft 6 stores warp on capability `IPlayerWarp`:

| `EnumWarpType` | Tweaks index | Role |
| --- | --- | --- |
| `NORMAL` | 0 | Sticky |
| `TEMPORARY` | 1 | Decays; most Tweaks grants |
| `PERMANENT` | 2 | Research/eldritch; this module does not grant it (ritual wrapper can) |

API (reflection):

- `ThaumcraftCapabilities.getWarp(player)`
- `IPlayerWarp.get / add / reduce(EnumWarpType, int)`
- `IPlayerWarp.sync(EntityPlayerMP)` — **required** after server-side changes or the client HUD is stale

Sounds such as `thaumcraft:whispers` are TC registry names played through vanilla `SoundEvent`.

## Design plan (hooks)

`ThaumcraftModule` uses Forge events only. No TC mixin.

| Event | Behavior |
| --- | --- |
| `PlayerLoggedInEvent` | If persisted `VisitedDimensions` is missing, set it to **current dim** so login is not “first visit” |
| `PlayerChangedDimensionEvent` | If `enableDimensionWarp` and dim not in the list: append, then a **worker thread sleeps 2s**, then `server.addScheduledTask` awards warp |
| `PlayerWakeUpEvent` | If `enableWarpCleansing`, not `wakeImmediately`, world is daytime: reduce configured types, sync, optional chat |
| `PlayerTickEvent` END every **400 ticks** (~20s) | Exposure: shortest matching interval; accrue or decay `WarpExposureProgress` |

### Dimension first visit

Persisted int array `VisitedDimensions`. Amounts: `dimensionNormalWarp` (2) + `dimensionTempWarp` (5). Sync, play `dimensionEntrySound` at `dimensionEntrySoundVolume` (2), send `dimensionChatMessageText`. Skip award/sound/chat if both amounts are 0.

After the 2s delay, abort if the player is dead, world is null, or the player is no longer in `playerEntities` (teleport/logout). **Do not call the TC API on the worker thread.**

### Sleep cleanse

Not a nap: `wakeImmediately` false **and** `world.isDaytime()`. Optional normal reduce 1, temp reduce 2 (`clearNormalWarp` / `clearTempWarp`). Chat only if something was actually cleared and `enableChatMessage`.

### Exposure

Each 400-tick pass is worth **20 seconds** of progress (the tick period).

Sources (take the **minimum** interval in seconds among those that match):

1. `exposureDimensionsConfig` entries `dimId=seconds` (default Nether `-1=300`, End `1=180`).
2. If `enableUndergroundExposure` and player Y **≤** `exposureUndergroundY` (30): `exposureUndergroundInterval` (300).
3. If `enableDungeonExposure` and chunk provider is `ChunkProviderServer` and `isInsideStructure(world, "RoguelikeDungeon", pos)`: `exposureDungeonInterval` (180).

There is **no Tweaks mixin** on `isInsideStructure` for this. Roguelike Dungeons Arcana must register that structure name. Village detection ([rtg.md](rtg.md)) is a different inject on village map gen.

If any source matches (`shortestInterval != MAX_VALUE`):

- `WarpExposureProgress += 20`
- If progress **≥** interval: reset to 0, add **1 temporary** warp, sync, optional whispers (`enableExposureSound`)

If **no** source matches:

- `WarpExposureProgress = max(0, progress - 20)`

Accrual and decay are **1:1** (20 seconds per check either way). Not 1.5×.

`Y` threshold `@Config.RangeInt` allows -1..256 so Depths negative Y still counts as “underground” when ≤ 30.

## Config (`aqtweaks_thaumcraft.cfg`)

| Name | Default | Live? | Meaning |
| --- | --- | --- | --- |
| Enable Sleep Warp Cleansing | true | yes | Master sleep sink |
| Clear Normal Warp | true | yes | Reduce sticky on sleep |
| Normal Warp Reduction | 1 | yes | Per successful sleep |
| Clear Temporary Warp | true | yes | Reduce temp on sleep |
| Temporary Warp Reduction | 2 | yes | Per successful sleep |
| Enable Sleep Chat Message | true | yes | Chat if anything cleared |
| Sleep Chat Message Text | (purple “whispers grow quieter”) | yes | |
| Enable Dimension Entry Warp | true | yes | Master first-visit source |
| Dimension Entry Normal Warp | 2 | yes | |
| Dimension Entry Temporary Warp | 5 | yes | |
| Dimension Chat Message Text | (purple “ancient whispers”) | yes | |
| Dimension Entry Sound | `thaumcraft:whispers` | yes | Empty = no sound |
| Dimension Entry Sound Volume | 2.0 | yes | |
| Enable Warp Exposure | true | yes | Master tick source |
| Exposure Dimensions Config | `-1=300`, `1=180` | yes | `id=seconds` |
| Enable Deep Underground Exposure | true | yes | |
| Underground Y Threshold | 30 | yes | Y ≤ this |
| Underground Exposure Interval | 300 | yes | Seconds / 1 temp warp |
| Enable Dungeon Exposure | true | yes | `RoguelikeDungeon` |
| Dungeon Exposure Interval | 180 | yes | |
| Enable Exposure Sound | true | yes | |
| Exposure Sound Effect | `thaumcraft:whispers` | yes | |
| Exposure Sound Volume | 2.0 | yes | |

## Files

- `thaumcraft/ThaumcraftModule.java`
- `thaumcraft/ThaumcraftHelper.java` — lazy `init()`, type index 0/1/2, `sync` only if `EntityPlayerMP`

## Do not regress

- Always `syncWarp` after add/reduce on the server.
- Dimension warp is **first visit only** (persisted array). Login must seed the current dim.
- Sleep must be a real night sleep (`wakeImmediately` false, daytime).
- Off-thread sleep then `addScheduledTask` — never TC API from the worker thread.
- Exposure decay is **−20 / 20s**, same as accrual, not an instant wipe and not 1.5×.
- Comfort `WarpCleansingProgress` is a different counter.

## Out of scope unless asked

- Permanent warp from dimensions/sleep/exposure
- Mixin into Roguelike structure lookup
- Wiring `villageMinWellHeight` or village detection into dungeon warp
