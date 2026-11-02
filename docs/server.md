# Server module (1.9)

Last updated: 2026-10-19.

Dedicated-server fixes for parent mods that call client-only (`@SideOnly(CLIENT)`) vanilla members from common code. No bus handler, no config. One optional mixin json per parent, queued only when that parent is loaded.

## Locked intent

A parent that calls a client-only member on a common path works in single player but throws `NoSuchMethodError` on a dedicated server, because Forge strips `@SideOnly(CLIENT)` members from server classes. Tweaks replaces that one call with a side-safe equivalent. Behavior on the client stays the same apart from what the replacement returns.

Do **not** rewrite the parent's surrounding logic. Do **not** add the parent jar to `libs/` when the mixin is a string target that only touches vanilla types.

## GeographiCraft spawn salvage

### How the parent works

GeographiCraft (`geographicraft`, jar `Geographicraft-1.12.2-1.0.jar`, package `climateControl`) moves the initial world spawn out of forbidden start biomes. `DimensionManager.salvageSpawn(World, ClimateControlSettings)` (private, instance) walks outward from the `WorldInfo` spawn until it finds an allowed biome, then logs `"setting spawn at <x>, <z> in biome <name>"`. The name comes from `Biome.func_185359_l()` (`getBiomeName`), which is `@SideOnly(CLIENT)`. On a dedicated server the first new world crashes at that log line.

The name only feeds the log string. It is not compared, looked up or branched on.

### Hook

`mixin/geographicraft/MixinDimensionManager` (`@Mixin(targets = "climateControl.DimensionManager", remap = false)`) has one `@Redirect` on the `INVOKE` of `Lnet/minecraft/world/biome/Biome;func_185359_l()Ljava/lang/String;` inside `salvageSpawn(Lnet/minecraft/world/World;LclimateControl/api/ClimateControlSettings;)V`. The handler returns `biome.getRegistryName().toString()` (for example `minecraft:ocean`), or `"unknown"` when the registry name is null. The log line on both sides shows the registry id instead of the display name.

### Loading

`mixins.aqtweaks.geographicraft.json` (`required: false`, `defaultRequire: 1`, common array). `AQTweaksLateMixinLoader.shouldMixinConfigQueue` queues it only when `Loader.isModLoaded("geographicraft")`. GeographiCraft is not compile-hard and not in `libs/`.

## MDECore `/mde tpx` cross-dimension teleport

### How the parent works

MDECore (`mdecore`, jar `mdecore-1.12-1.1.jar`, package `com.mattdahepic.mdecore`) provides `/mde tpx` (handled by `TPXLogic`). The dimension-only form (`/mde tpx <dim>`) calls `TeleportHelper.transferPlayerToDimension(player, dimension, playerList)` followed by `sendPlayerToSpawnInCurrentWorld(sender)`.
Instead of standard Forge dimension transfer (`player.changeDimension(dim)`), `mdecore` manually fires an outdated 1.8-era packet sequence: sending `SPacketRespawn`, removing the entity dangerously from the old world, spawning into the new world, skipping `SPacketPlayerAbilities`, inventory synchronization, time/weather updates, and attribute sync (`SPacketEntityProperties`), and prematurely firing `PlayerChangedDimensionEvent`. Mod listeners (e.g. `OpenGlider`, `veining`) receiving this event attempt client sync packets before the client world is initialized, triggering an `FMLProxyPacket` handshake rejection and disconnecting the client.
Furthermore, `sendPlayerToSpawnInCurrentWorld` moves the player to the dimension's default spawn coordinate. In the Nether (spawn `0, 0, 0`), this drops the player into solid bedrock at `Y = 0.5`.

### Hook

`mixin/mdecore/MixinTeleportHelper` (`@Mixin(targets = "com.mattdahepic.mdecore.helpers.TeleportHelper", remap = false)`):
- `@Overwrite` on `transferPlayerToDimension`: delegates safely to `player.changeDimension(dimension)`.
- `@Inject` at `HEAD`, cancellable, on `sendPlayerToSpawnInCurrentWorld`: cancels when `player.dimension == -1` (the Nether) so the player remains safely at the portal generated/located by `changeDimension` instead of displacing to bedrock.

### Loading

`mixins.aqtweaks.mdecore.json` (`required: false`, `defaultRequire: 1`, common array). `AQTweaksLateMixinLoader.shouldMixinConfigQueue` queues it only when `Loader.isModLoaded("mdecore")`. MDECore is not compile-hard and not in `libs/`.

## Files

| Piece | Role |
| --- | --- |
| `mixin/geographicraft/MixinDimensionManager.java` | Redirect `getBiomeName` in `salvageSpawn` |
| `mixins.aqtweaks.geographicraft.json` | Optional late json |
| `mixin/mdecore/MixinTeleportHelper.java` | Replace manual transfer with `changeDimension` and cancel Nether spawn move |
| `mixins.aqtweaks.mdecore.json` | Optional late json |
| `AQTweaksLateMixinLoader.java` | Lists the json; `shouldMixinConfigQueue` gates it on `mdecore` (and `geographicraft`, `atum`, `dsurround`) |
| `zone/rong/mixinbooter/ILateMixinLoader.java` | Compile stub; declares the `shouldMixinConfigQueue(String)` default so the override compiles |

## Do not regress

- Redirect only the call in `salvageSpawn`. There is exactly one `func_185359_l` call in `DimensionManager`, so no ordinal. Re-check on a GeographiCraft bump.
- Keep `remap = false` with the SRG target descriptor (`func_185359_l`). The method is absent on the server, so an MCP name would never be remapped onto it.
- Keep the handler non-static. `salvageSpawn` is an instance method.
- Do not call any other `@SideOnly(CLIENT)` member from the handler (for example `Biome.getBiomeName` itself).
- Do not import GeographiCraft or MDECore types. Do not put either jar in `libs/`.
- In `MixinTeleportHelper`, do not cancel `sendPlayerToSpawnInCurrentWorld` for dimensions other than the Nether (`dimension == -1`); other dimensions expect spawn placement.
- The `ILateMixinLoader` stub is compiled into the Tweaks jar (pre-existing). Keep its default methods matching MixinBooter's (`shouldMixinConfigQueue(String)` returns `true`).
