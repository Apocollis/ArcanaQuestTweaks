# Temperature (Simple Difficulty)

Parent: **Simple Difficulty** (`simpledifficulty` 0.3.9). Optional: **Serene Seasons** (greenhouse glass, read by registry name only).

This module owns how Simple Difficulty **body and world temperature** are calculated in this pack: surface indoor insulation, glass and greenhouse roofs, and boat wetness.

Scope boundaries:

- The **feather** consequence of being hypothermic or hyperthermic (max-stamina cap, slowness, thirst drip, the zero-cap kill) is [stamina.md](stamina.md) under *Simple Difficulty thermia feather drain*.
- Cinder Ward / Astral Warmth clamps and Spelunker's underground pull run later on the capability in `MixinTemperatureCapability`; they are [reskillable.md](reskillable.md).
- Comfort's temperature **penalty** band (`comfort_min` / `comfort_max` 11/14) is [comfort.md](comfort.md).
- Campfire neighbor notification and the crucible heat spoof are [thaumcraft.md](thaumcraft.md).

## Files

- `simpledifficulty/DynamicModifierInsulation.java` — the `ITemperatureDynamicModifier`
- `simpledifficulty/SimpleDifficultyModule.java` — constructor registers it once
- `mixin/simpledifficulty/MixinWorldUtil.java` — boat position lift
- `mixins.aqtweaks.simpledifficulty.json` — `required: false`
- `ArcanaQuestTweaksConfig.SimpleDifficulty` — the seven cfg keys below
- `util/Reflect.java` — `getRidingEntity` (`func_184187_bx`)

## How Simple Difficulty calculates temperature

- **`getPlayerTargetTemperature`** (`TemperatureUtilInternal`): `pos = WorldUtil.getSidedBlockPos`, then sum `getWorldInfluence + getPlayerInfluence` over `TemperatureRegistry.modifiers`. Then, **per entry** in `dynamicModifiers`, call `applyDynamicWorldInfluence` and then `applyDynamicPlayerInfluence` on that same entry — interleaved per modifier, **not** two chained sweeps. Returns `(int) t`.
- **`getWorldTemperature`**: world influences only, then `applyDynamicWorldInfluence` per dynamic modifier. Thermometers and `calculateClientWorldEntityTemperature` use this.
- **Registered modifiers in this pack**: `Default` (+12), `Altitude`, `Armor`, `Baubles`, `BlocksTiles`, `Dimension`, `HeldItems`, `SereneSeasons`, `Snow`, `Sprint`, `Temporary`, `Time`, `Wet`. (`Baubles` comes from Simple Difficulty's `CompatController`; `HarvestFestival` is in the jar but that mod is not in the pack.)
- **`ModifierBase.getPlayerInfluence` returns 0** and none of `Biome`, `Time`, `Altitude`, `Snow`, `SereneSeasons` override it.
- **`applyUndergroundEffect`** returns the raw value as soon as `Y >= 64`. Below that it scales by `(y - cutoff) / (64 - cutoff)`, and 0 at or below the cutoff, only when `undergroundEffect` is on, the dimension has sky light, and neither `pos` nor `pos.up()` can see sky. Applied by `Biome`, `Time`, `SereneSeasons`; **not** by `Altitude` or `Snow`.
- **`ModifierTime`** applies `timeTemperatureShade` only when the time value is already **positive**, clamped `max(0, v + shade)`. Nights are unaffected by shade.
- **`ModifierSnow`** needs `isRaining() && canSeeSky(pos)`, so it is 0 whenever sheltered.
- **`ModifierAltitude`** = `-1 × abs((64 - y)/64 × AltitudeMultiplier + 1)`. Always negative, exactly `-1` at Y=64, `0` at Y=96 with multiplier 2, `-3` at Y=0. Not roof-gated.
- **`ModifierWet`** order: `IFluidBlock` temperature from `JsonConfig.fluidTemperatures` if present; else `Material.WATER` → `wetValue`; else `isRainingAt(pos)` → `wetValue`; else 0. Vanilla water is `BlockLiquid`, not `IFluidBlock`, so the first branch never fires for it.
- **`TemperatureEnum` bands**: `FREEZING` 0–5, `COLD` 6–10, `NORMAL` 11–14, `HOT` 15–19, `BURNING` 20–25.
- **Sampling rate**: `TemperatureCapability.tickUpdate` every **40 ticks** server-side; `TemperatureGui.onClientTick` once per client tick; **`ItemThermometer`'s `IItemPropertyGetter` once per rendered frame**.

### Pack Simple Difficulty cfg (DEVBOX)

| Key | Value | Why it matters here |
| --- | --- | --- |
| `UndergroundEffect` | true | Enables the sub-64 scale |
| `UndergroundEffectCutoff` | **50** | Y 50–63 still keeps 86–93% of its environment |
| `TimeTemperatureShade` | -6 | Day-only; does nothing at night |
| `WetValue` | -6 | What a boat was wrongly collecting |
| `AltitudeMultiplier` | 2 | Altitude is about -1 at sea level |
| Serene Seasons | on | Midwinter -4, midsummer +4 |

`fluidTemperatures.json` has only `hot_spring_water`, so vanilla water falls through to `Material.WATER`.

## Indoor insulation

`DynamicModifierInsulation` (`getName()` → `"AQTweaksInsulation"`) is registered once from the `SimpleDifficultyModule` **constructor** behind a static guard, calling `TemperatureRegistry.registerDynamicModifier`. Not from `CommonProxy` — that class must not import Simple Difficulty. `TemperatureRegistry` keys by `getName()` into a `LinkedHashMap` that is never cleared, so once is enough.

**Why it exists.** `ModifierBase.applyUndergroundEffect` returns the raw value as soon as `Y >= 64`, so biome, time, and Serene Seasons stay at full strength in a surface house. Shade does not cover nights: `ModifierTime` applies `timeTemperatureShade` (pack value `-6`) only when the time value is already **positive**, clamped `max(0, v + shade)`.

The work happens in `applyDynamicWorldInfluence`, so thermometers, item-frame thermometers, the client world readout, and the player target all agree. `applyDynamicPlayerInfluence` is a pass-through. In `getPlayerTargetTemperature` Simple Difficulty walks `dynamicModifiers` and calls world-then-player **per entry** — interleaved, not two chained sweeps.

**Dampened set** — `Biome`, `Time`, `Altitude`, `Snow`, `SereneSeasons`, each re-read with `getWorldInfluence(world, pos)` so shade and the underground scale are already in the number, and each looked up **by name at call time** so Simple Difficulty's own registration order (including `CompatController` registering `SereneSeasons`) is irrelevant. `ModifierBase.getPlayerInfluence` returns 0 and none of the five override it, so there is no double count on the player path.

- `Altitude` is in the set as a deliberate **elevation** choice, not an exposure fix. It is `-1 × abs((64 − y)/64 × AltitudeMultiplier + 1)`, always negative, exactly `-1` at Y=64 and `0` at Y=96 with the pack's multiplier 2, and it is not roof-gated. Including it gives every surface house a flat ~+0.5 and leaves a Y=96 house relatively *colder* than a Y=64 one.
- `Snow` is always 0 while sheltered (`ModifierSnow` needs `canSeeSky(pos)`). It stays in the sum for symmetry only; it contributes nothing.
- Everything else keeps full effect indoors: `Default`, `BlocksTiles`, `Armor`, `Baubles`, `HeldItems`, `Temporary`, `Wet`, `Sprint`, `Dimension`. Heaters and chillers apply on top of the insulated value.

**No `Y < 64` gate.** The underground scale is already inside every `getWorldInfluence` value this modifier reads *and* inside `currentTemp`, so there is nothing to double-count. The pack sets `UndergroundEffectCutoff=50`, so Y 50–63 keeps 86–93% of its environment — a gate there would strand every lowland, beach, and swamp house with no insulation at all. Below Y=50 the dampened values are already 0 and the pull is a natural no-op. `SpelunkerComfort.pull` is a separate system: it runs later on the capability in `MixinTemperatureCapability` and keys off `ThaumcraftConfig.exposureUndergroundYMin/Max`.

**Roof.** `roofType(world, pos)` is the **only** scan and returns `NONE` / `GLASS` / `OPAQUE`:

1. `!canSeeSky(pos) && !canSeeSky(pos.up())` → `OPAQUE`. One heightmap pair covers every opaque house and every cave, and keeps the block scan off the common path.
2. Otherwise scan `dy = 2 .. indoorInsulationCeilingMaxHeight` (default 16): `Material.GLASS` → `GLASS`; `!isPassable` → `OPAQUE`; unloaded → `NONE`.
3. Exhausted → `NONE`.

`hasRoof` is a one-line wrapper (`roofType(...) != NONE`) and is what the rays call. `checkCeiling` calls `roofType` once, returns early on `NONE`, then applies the greenhouse upgrade below.

The block scan exists because **glass does not raise the light heightmap**. `Block.lightOpacity` is `fullBlock ? 255 : 0` where `fullBlock = getDefaultState().isOpaqueCube()`, and `BlockBreakable.isOpaqueCube()` is false — so vanilla glass, stained glass, glass panes, **and** Serene Seasons greenhouse glass all sit at `lightOpacity == 0` and `canSeeSky` stays true under every one of them. That is the same reason Simple Difficulty's own shade check fails under glass.

Glass is `state.getMaterial() == Material.GLASS`, not `instanceof BlockGlass` and not `== Blocks.GLASS` — either of those misses Chisel's glass variants, and this pack ships Chisel. Ice is `Material.ICE` / `PACKED_ICE` and is **not** glass, so an ice roof falls through to the opaque branch and insulates. Opacity uses `isPassable`, **not** `isOpaqueCube`: `BlockLeaves.isOpaqueCube` tracks the client's fancy/fast graphics setting and is not side-stable.

**Greenhouse glass.** When the roof is not `NONE`, `checkCeiling` scans `dy = 1 .. greenhouseGlassMaxHeight` (default **7**) for `sereneseasons:greenhouse_glass` and **does not stop at intervening blocks**. That is Serene Seasons' `SeasonalCropGrowthHandler.isGreenhouseGlassAboveBlock` verbatim, so player warmth and crop fertility agree in the same build. Keep the key equal to Serene Seasons' `greenhouse_glass_max_height` in `config/sereneseasons/cropfertility.cfg` (7 in this pack).

The block is resolved **by registry name**, lazily, behind `Loader.isModLoaded("sereneseasons")` → `ForgeRegistries.BLOCKS.containsKey` → `getValue`, guarding both `null` and `Blocks.AIR`, inside `try/catch (Throwable)`. Same shape as `VillageLandHelper`'s BOP grass lookup. There is **no** Serene Seasons import, no jar in `libs/`, no `$deps` entry, no `@Mod` dependency, and no compat-matrix parent row: only block identity is needed, so compile-hard gains nothing. A direct `SSBlocks.greenhouse_glass` reference would `NoClassDefFoundError` whenever Simple Difficulty is present and Serene Seasons is absent, because this class is constructed under the `simpledifficulty` gate only. If the block does not resolve, greenhouse handling disables and standard insulation still applies.

**Enclosure.** `indoorInsulationEnclosureStrictness` `0` is roof only (this includes cliff overhangs and any opaque canopy). `1` (default) needs at least 3 of 4 cardinal directions walled, `2` needs all 4. Each ray steps `d = 1 .. indoorInsulationMaxDistance` (default 5):

- `!world.isBlockLoaded(checkPos)` → direction open, stop. A temperature sample must never force a chunk load or generation.
- `getCollisionBoundingBox != NULL_AABB` at foot **or** eye → walled, stop.
- `!hasRoof(checkPos)` → stepped out from under the roof, open, stop.

The wall test is collision, **not** `material.blocksMovement()`: leaves, wood, and iron all block movement at the material. An air gap is the opening; an **open door** keeps a side box so it still counts as a wall, so loose mode does not mean "one open door". There is no glass clause — every glass block returns `FULL_BLOCK_AABB`, and `BlockPane` overrides only `addCollisionBoxToList`, so its thin box is non-NULL too. Collision already marks all glass walled.

That test is coarser than "wall": one bottom slab, carpet, pressure plate, fence post, or chest per side counts as a full wall, and `!isPassable` does the same job up to 16 blocks overhead, so a lone slab on a high platform reads as a ceiling. Accepted — the roof check still gates everything. Under an oak or a cliff overhang the rays never register "open" (everything has a roof); they **exhaust unwalled**, so strictness 1 and 2 still fail. A hall wider than `indoorInsulationMaxDistance` likewise exhausts and does not count as enclosed.

**Pull.** With `targetTemp` = `greenhouseGlassTargetTemperature` under greenhouse glass, else 12:

```
targetOffset = targetTemp - 12
envOffset    = Σ getWorldInfluence(world, pos) over the dampened set
pull         = (envOffset - targetOffset) × indoorInsulationFactor
newTemp      = currentTemp - pull
```

At factor 1.0 this lands exactly on `targetTemp + nonEnvironmentModifiers`; at the default 0.5 it sits halfway between the outdoor value and that target. The greenhouse bonus is therefore `targetOffset × factor` — **+1 at the default factor, not +2** — reaching the full +2 only at factor 1.0. Worked check at 0.5: cold `envOffset = -10` → `pull = -6` → `+6` instead of `+5`; hot `envOffset = +8` → `pull = +3` → `-3` instead of `-4`.

14 is the top of `TemperatureEnum.NORMAL` (11–14) and matches the Comfort module's `comfort_max`. `HOT` starts at 15, so factor 1.0 plus any campfire or `BlocksTiles` heat can tip a greenhouse into `HOT` and hyperthermia. `greenhouseGlassTargetTemperature` allows up to 18.0, which sits inside `HOT` — that is deliberately dangerous.

**Shelter memo.** `isSheltered` and the ceiling type are cached per instance, keyed `pos.toLong() ^ (dimension × 31)`, and the whole map is cleared whenever `world.getTotalWorldTime()` changes. That bound is what keeps it from growing. It exists because `ItemThermometer`'s `IItemPropertyGetter` calls `WorldUtil.calculateClientWorldEntityTemperature` → `getWorldTemperature` → this modifier **once per rendered frame**, per thermometer model — held item, inventory and JEI slots, and every item-frame thermometer in view. Server sampling is only every 40 ticks (`TemperatureCapability.tickUpdate`) and the HUD is once per client tick.

`TemperatureRegistry.dynamicModifiers` is a **single static map**, so there is exactly one modifier instance and the client thread and the integrated server thread both call into it. The modifier therefore holds **two** memos and picks one by `world.isRemote`, which confines each `HashMap` to a single thread. The lazily resolved greenhouse block is `volatile` and assigned **before** its resolved flag for the same reason. Do not collapse the two memos back into one field.

`indoorInsulation*` and `greenhouseGlassTargetTemperature` are read on both sides and are **not** synced. Simple Difficulty syncs its own `ModConfig.server`; aqtweaks cfg does not. A client whose `aqtweaks_stamina.cfg` differs from the server's renders a wrong thermometer, HUD, and greenhouse state. The pack ships identical configs, so this is a known limit rather than a bug.

## Boat wetness

`MixinWorldUtil` (`mixins.aqtweaks.simpledifficulty.json`, `remap = false`) injects at `RETURN` of the **static** `WorldUtil.getSidedBlockPos` with `CallbackInfoReturnable<BlockPos>`.

`getSidedBlockPos` floors `posY + 0.5` on both sides (server `entity.getPosition()`, client `BlockPos(positionVector + (0, 0.5, 0))`). A player riding a boat sits at `boat.posY - 0.45` — `EntityBoat.getMountedYOffset()` is `-0.1`, `EntityPlayer.getYOffset()` is `-0.35` — so that block is the water under the hull. This pack's `fluidTemperatures.json` has no `"water"` entry, so `ModifierWet` falls through the `IFluidBlock` branch (vanilla water is `BlockLiquid`, not `IFluidBlock`) and returns `wetValue` `-6` from `Material.WATER`.

The lift applies only when the entity is an `EntityPlayer`, `Reflect.getRidingEntity` is an `EntityBoat` (subclasses included), and **all** of:

- the returned block is `Material.WATER`, and
- the block above is **not** `Material.WATER` (a submerged boat keeps the original position), and
- the block above is **passable** (`getCollisionBoundingBox == NULL_AABB`) — without this, a boat in a one-high water channel under stone would sample inside the ceiling, which would also read as sheltered to the insulation modifier.

The lift moves **every** sample one block up while riding, not only wetness: altitude, nearby blocks, rain, and the shelter test all run at the returned position. Rain is still `isRainingAt` on that returned position, so an open boat in the rain stays wet. Horses and other mounts are out of scope and stay wet in water; swimming stays wet.

Vanilla access inside this `remap = false` mixin goes through `Reflect`: `getBlockState`, `getMaterial`, `getMaterialWater`, `getCollisionBoundingBox`, `up`, `getRidingEntity` (`func_184187_bx`). Do not put raw MCP names in the mixin body.

## Config (`aqtweaks_stamina.cfg` → `Simple Difficulty Integration`)

These seven keys live in the stamina cfg file because they sit on `StaminaModuleConfig.simpleDifficulty`, which predates this module. Moving them would rename keys and reset tuned values in existing instance files, so they stay put.

| Key | Default | Notes |
| --- | --- | --- |
| Indoor Insulation Enabled | true | Pull biome/time/altitude/snow/season toward neutral when sheltered |
| Indoor Insulation Factor | 0.5 | 1.0 lands exactly on the target; the greenhouse bonus is this × the target offset |
| Indoor Insulation Enclosure Strictness | 1 | 0 roof only, 1 at least 3 of 4 walled, 2 all 4 |
| Indoor Insulation Max Distance | 5 | Horizontal ray length; a wider hall exhausts unwalled |
| Indoor Insulation Ceiling Max Height | 16 | Glass/opaque scan depth; only run when the position can still see sky |
| Greenhouse Glass Target Temperature | 14.0 | Top of `NORMAL`; `HOT` starts at 15, so above 14 is deliberately dangerous |
| Greenhouse Glass Max Height | 7 | Keep equal to Serene Seasons `greenhouse_glass_max_height` |

Read on both sides and **not** synced. See the SMP note under *Indoor insulation*.

## Verify

Full checklist: [verification.md](verification.md) under *Temperature*. Minimum: thermometer delta outside vs inside a surface house at night, a glass roof behaving like an opaque one, a greenhouse reading warmer by about +1 at the default factor, a Y 62 house still insulating, and an open boat on clear ocean water no longer reading wet.

## Design history (do not regress)

### 1. Insulation gated off below Y=64

An early draft skipped the pull whenever `pos.getY() < 64` and Simple Difficulty's `undergroundEffect` was on, to avoid "a second pull". There is no second pull: the underground scale is already inside every `getWorldInfluence` value the modifier reads *and* inside `currentTemp`. With the pack's `UndergroundEffectCutoff=50`, Y 50–63 keeps 86–93% of its environment, so the gate stranded every lowland, beach, and swamp house with no insulation from either side. The gate also keyed off the wrong system — `SpelunkerComfort` uses `ThaumcraftConfig.exposureUndergroundYMin/Max`, not Simple Difficulty's cutoff. **Fix:** no Y gate. Below Y=50 the dampened values are already 0 and the pull is a natural no-op.

### 2. Two roof scans that had to agree

`hasRoof` returned a boolean while `checkCeiling` was told to "determine `GLASS` vs `OPAQUE` from the same scan" — which a boolean cannot carry, so the second caller needed its own near-duplicate scan free to drift. **Fix:** one primitive `roofType` returning the enum; `hasRoof` is a one-line wrapper over it. One scan, one place to change the block tests.

### 3. Greenhouse scan split warmth from crop fertility

A 16-deep greenhouse scan that stopped at the first opaque block disagreed with Serene Seasons' own `isGreenhouseGlassAboveBlock`, which scans `dy = 1 .. greenhouse_glass_max_height` (**7** in this pack) and ignores intervening blocks. A 10-tall greenhouse would have warmed the player while its crops stayed infertile, and glass above an opaque ceiling would have fertilized without warming. **Fix:** the greenhouse scan matches Serene Seasons exactly and has its own `greenhouseGlassMaxHeight` key, kept equal to theirs. The glass/opaque roof scan keeps a separate, larger cap.

### 4. Glass roofs and a dead glass wall clause

`canSeeSky` is true under glass — `lightOpacity` is `fullBlock ? 255 : 0` and `BlockBreakable.isOpaqueCube()` is false — so the plain `canSeeSky` roof check failed under every glass ceiling, the same way Simple Difficulty's shade check does. The first fix also added a glass branch to the *wall* test, which was dead: every glass block returns `FULL_BLOCK_AABB` and `BlockPane` overrides only `addCollisionBoxToList`, so collision already marked all glass walled. **Fix:** an upward block scan handles glass **ceilings**; the wall test stays pure collision with no glass clause. Glass is `Material.GLASS` so Chisel variants match; ice is deliberately not glass.

### 5. `isOpaqueCube` on leaves is graphics-dependent

`BlockLeaves.isOpaqueCube` tracks the client's fancy/fast setting, so a ceiling decided with it can differ client vs server. **Fix:** the scan uses `!isPassable`, which is side-stable and already catches leaves, stairs, and slabs.

### 6. Thermometer readout runs per frame, not per tick

`ItemThermometer`'s `IItemPropertyGetter` calls `calculateClientWorldEntityTemperature` → `getWorldTemperature` → this modifier once per **rendered frame**, per thermometer model. An early draft put the upward block scan ahead of the `canSeeSky` check and let the rays call the full `checkCeiling`, which is up to ~300 `getBlockState` per sample on that path. **Fix:** `canSeeSky` short-circuits first, rays call `hasRoof` only, the greenhouse scan is skipped when there is no roof, and a shelter memo keyed on position + dimension is cleared whenever `getTotalWorldTime()` changes.

### 7. Boat lift into a tunnel ceiling

Lifting the sample whenever the block above was not water pushed it inside the stone of a one-high water channel, which then also read as sheltered. **Fix:** the block above must be non-water **and** passable, or the original position stands.

### 8. Compile-hard `SSBlocks` would crash without Serene Seasons

`DynamicModifierInsulation` is constructed under the `simpledifficulty` gate only, so a direct `SSBlocks.greenhouse_glass` reference would `NoClassDefFoundError` whenever Simple Difficulty is present and Serene Seasons is not. Only block identity is needed, so compile-hard gains nothing. **Fix:** registry-name lookup behind `Loader.isModLoaded("sereneseasons")`, guarding `null` and `Blocks.AIR`, with no jar in `libs/` and no compat-matrix parent row.

### 9. One shared instance, one HashMap

`TemperatureRegistry.dynamicModifiers` is a single static map, so the modifier is one object that both the client thread and the integrated server thread call. The first implementation kept a single `HashMap` memo and set the greenhouse `resolved` flag before storing the block, so single-player could corrupt the map or briefly read a null block through a `true` flag. **Fix:** two memos picked by `world.isRemote` (thread-confined), and `volatile` fields with the value assigned before the flag.

## Do not regress

- Insulation has **no `Y < 64` gate**. The underground scale is already inside the `getWorldInfluence` values and inside `currentTemp`. Do not re-add one, and do not conflate it with `SpelunkerComfort` (different system, different Y range, runs later on the capability).
- `roofType` is the **only** roof scan; `hasRoof` is a wrapper over it. Do not reintroduce a parallel boolean roof check. Rays call `hasRoof`, never `checkCeiling` — the greenhouse scan must stay off the ray path.
- `canSeeSky` short-circuits first in `roofType`. Do not move the upward block scan ahead of it; that scan exists only for the glass case, where `canSeeSky` is true.
- Ceilings use `isPassable`, **not** `isOpaqueCube` (graphics-dependent for leaves). Glass is `state.getMaterial() == Material.GLASS`, not `instanceof BlockGlass` and not `== Blocks.GLASS`, or Chisel's glass stops matching. Ice stays excluded.
- No glass clause in the wall test — collision already covers glass blocks and panes. The wall test stays collision, never `material.blocksMovement()`.
- The greenhouse scan stays `dy = 1 .. greenhouseGlassMaxHeight` (7) and keeps ignoring intervening blocks, matching Serene Seasons. Raising it to the glass/opaque cap or stopping at the first opaque block splits warmth from crop fertility.
- Greenhouse glass stays a **registry-name** lookup behind `Loader.isModLoaded("sereneseasons")`, guarding `null` and `Blocks.AIR`, resolved lazily in a `try/catch`. No Serene Seasons import, no jar in `libs/`, no `$deps` entry, no `@Mod` dependency, no compat-matrix parent row.
- The dampened set is exactly `Biome`, `Time`, `Altitude`, `Snow`, `SereneSeasons`, looked up **by name at call time**. Never add `Default`, `BlocksTiles`, `Armor`, `Baubles`, `HeldItems`, `Temporary`, `Wet`, `Sprint`, or `Dimension` — heaters, chillers, clothing, and wetness keep full effect indoors. Name lookup at call time is what makes Simple Difficulty's registration order irrelevant.
- Rays stop as open on `!world.isBlockLoaded(checkPos)`. A temperature sample must never force a chunk load or generation. The shelter memo clears on `getTotalWorldTime()` change; do not turn it into an unbounded cache.
- Insulation registers once in the `SimpleDifficultyModule` **constructor** behind a static guard, never from `CommonProxy`.
- The modifier is **one shared instance** (single static registry map). Keep the two `isRemote`-selected memos and the `volatile` greenhouse fields with value-before-flag ordering. A single memo map is a single-player data race.
- The boat lift requires the block above to be non-water **and** passable. `Reflect` only inside `MixinWorldUtil`; `DynamicModifierInsulation` is a normal class and may call vanilla directly.
