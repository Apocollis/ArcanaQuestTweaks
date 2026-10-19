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

## Files

| Piece | Role |
| --- | --- |
| `mixin/geographicraft/MixinDimensionManager.java` | Redirect `getBiomeName` in `salvageSpawn` |
| `mixins.aqtweaks.geographicraft.json` | Optional late json |
| `AQTweaksLateMixinLoader.java` | Lists the json; `shouldMixinConfigQueue` gates it on `geographicraft` |
| `zone/rong/mixinbooter/ILateMixinLoader.java` | Compile stub; declares the `shouldMixinConfigQueue(String)` default so the override compiles |

## Do not regress

- Redirect only the call in `salvageSpawn`. There is exactly one `func_185359_l` call in `DimensionManager`, so no ordinal. Re-check on a GeographiCraft bump.
- Keep `remap = false` with the SRG target descriptor (`func_185359_l`). The method is absent on the server, so an MCP name would never be remapped onto it.
- Keep the handler non-static. `salvageSpawn` is an instance method.
- Do not call any other `@SideOnly(CLIENT)` member from the handler (for example `Biome.getBiomeName` itself).
- Do not import GeographiCraft types. Do not put the jar in `libs/`.
- The `ILateMixinLoader` stub is compiled into the Tweaks jar (pre-existing). Keep its default methods matching MixinBooter's (`shouldMixinConfigQueue(String)` returns `true`).
