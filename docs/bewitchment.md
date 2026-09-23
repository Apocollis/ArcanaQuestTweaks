# Bewitchment module (1.8)

Last updated: 2026-09-16.

Config: `config/arcanaquesttweaks/aqtweaks_bewitchment.cfg`. `BewitchmentRegistryHandler` registers on the Forge bus only if `bewitchment` is loaded.

Cambion house paste-at-ground, skip-air, and village-skip live in the RTG module ([rtg.md](rtg.md)), not here. Those mixins are `mixins.aqtweaks.bewitchment.json` (`required: false`). Witch-school drain thrift, Hearth altar gain, Stitch poppet hits, and Cold Iron Mind ritual-warp halving live in the Reskillable module ([reskillable.md](reskillable.md)); they reuse this json and `WarpRitualWrapper.onFinished`. This file covers **ritual warp** and **CraftTweaker Spinning Wheel** recipes.

## Locked intent

When listed Bewitchment rituals **finish**, grant Thaumcraft warp (normal, temporary, optional permanent). Do not change circles, cost, or JEI identity. Halted/failed rituals grant nothing.

## How the parent mod works

Bewitchment registers `com.bewitchment.api.registry.Ritual` on a Forge registry (`RegistryEvent.Register<Ritual>`). Each ritual has:

- Registry name (`bewitchment:conjure_demon`, hellmouths, etc.)
- Circle inputs, sacrifice predicate, outputs, power, duration
- Callbacks: `isValid`, `onStarted`, `onFinished`, `onHalted`, `onUpdate`

The altar runs the **registry value** for that id. Re-registering the **same** `ResourceLocation` replaces the entry. Tweaks does not edit Bewitchment’s class; it wraps the existing ritual object.

Compile-time dependency: `WarpRitualWrapper` **extends** `Ritual`. This is not reflection-safe like `ThaumcraftHelper`. The handler class must not load if Bewitchment is absent (`CommonProxy` already gates registration).

## Design plan (wrap on the same registry event)

`BewitchmentRegistryHandler.registerRituals`:

1. If **either** `bewitchment` or `thaumcraft` is missing → **return**. Wrappers are **not** registered without Thaumcraft. (Older notes that said “wrapper still registers, warp no-ops” are wrong.)
2. For each cfg line `id=normal,temp[,permanent]`, `registry.getValue(id)`.
3. If the original is null (id typo, or **this handler ran before Bewitchment registered**), skip silently.
4. Parse ints (need at least normal and temp). Permanent defaults to 0.
5. `new WarpRitualWrapper(original, …)` with the **same** registry name, then `registry.register(wrapper)`.

### Event order (do not miss this)

Both mods subscribe to `RegistryEvent.Register<Ritual>`. Tweaks **looks up** the original in that same event. `@Mod` has `after:bewitchment`, and Tweaks registers its handler in `init` after Bewitchment has typically registered its listener — but the **event dispatch order** is still “whoever was on the bus first.”

If Tweaks runs first, `getValue` is null and **no wrap happens** (no log). After a Bewitchment version bump, confirm listed ids still exist and that wraps apply (JEI/altar still one ritual; completing it grants warp).

### `WarpRitualWrapper`

Constructor copies from parent:

- `getRegistryName()`, `input`, `sacrificePredicate`, `output`
- `canBePerformedRemotely`, `startingPower`, `runningPower`
- `circles[0]`, `circles[1]`, `circles[2]`, `time`

Then `setRegistryName(parent.getRegistryName())` again.

Delegates `isValid`, `onStarted`, `onHalted`, `onUpdate` unchanged.

`onFinished`: `parent.onFinished(...)` first, then if player non-null and **server world**, add warp types with amount &gt; 0 (helper indices 0/1/2) and `syncWarp` if anything was added.

A new Bewitchment `Ritual` constructor argument that is not copied will desync circles/power. Re-read `Ritual.<init>` on Bewitchment updates.

## Config (`aqtweaks_bewitchment.cfg`)

One list, `ritualWarpList`. Format: `registry_name=normal,temporary[,permanent]`.

| Id | Normal | Temp | Permanent |
| --- | --- | --- | --- |
| `bewitchment:conjure_imp` | 1 | 3 | 0 |
| `bewitchment:conjure_demon` | 2 | 5 | 0 |
| `bewitchment:conjure_baphomet` | 5 | 15 | 2 |
| `bewitchment:conjure_leonard` | 5 | 15 | 2 |
| `bewitchment:lesser_hellmouth` | 2 | 5 | 0 |
| `bewitchment:hellmouth` | 3 | 8 | 0 |
| `bewitchment:greater_hellmouth` | 4 | 10 | 0 |
| `bewitchment:sowing_salt` | 2 | 4 | 0 |
| `bewitchment:drought` | 2 | 4 | 0 |
| `bewitchment:hungry_flames` | 2 | 4 | 0 |
| `bewitchment:conjure_wither` | 3 | 6 | 0 |

Malformed lines (`=` missing, fewer than two warp ints) are skipped. `NumberFormatException` is printed.

## CraftTweaker Spinning Wheel

`CTBewitchmentSpinningWheel` is scanned by CraftTweaker (`@ZenRegister`). `@ModOnly("bewitchment")` skips it if Bewitchment is absent. Tweaks does **not** `import` this class from `CommonProxy` or other always-on bus handlers. Compile-hard CraftTweaker jar: `CraftTweaker2-1.12-4.1.20.715.jar`.

Zen package: `mods.bewitchment.SpinningWheel`. Parent type: `com.bewitchment.api.registry.SpinningWheelRecipe` on Forge registry `bewitchment:spinning_wheel_recipe` (constructor `(ResourceLocation, List<Ingredient>, List<ItemStack>)`, max 4 inputs, max 2 outputs). Tweaks exposes a **single** output.

| Method | Effect |
| --- | --- |
| `addRecipe(String name, IItemStack output, IIngredient[] inputs)` | Queue an `IAction` that registers one recipe. Inputs must be **1–4**. Empty/null name, null/empty output, or a null ingredient logs and skips. |
| `removeRecipe(IItemStack output)` | Remove every recipe whose **first** output matches item + meta + NBT (stack size ignored). |
| `removeRecipe(String name)` | Remove the recipe at that `ResourceLocation`. |

Name with a `:` is a full id (`bewitchment:golden_thread`). Otherwise Tweaks uses `crafttweaker:<name>`. Actions run through `CraftTweakerAPI.apply` against `GameRegistry.findRegistry(SpinningWheelRecipe.class)`. Removes go through `IForgeRegistryModifiable.remove`. Bewitchment did not call `allowModification()` on this registry; if remove throws, Tweaks logs and does not silently swallow.

```zenscript
import mods.bewitchment.SpinningWheel;

SpinningWheel.removeRecipe(<bewitchment:golden_thread>);
SpinningWheel.removeRecipe("bewitchment:golden_thread");
SpinningWheel.addRecipe("pack_thread", <bewitchment:golden_thread>, [<ore:string>, <bewitchment:oak_apple_gall>]);
```

MoreTweaker already covers oven / distillery / cauldron / ritual. It does **not** cover the Spinning Wheel.

## Files

- `thaumcraft/BewitchmentRegistryHandler.java` (package is historical)
- `thaumcraft/WarpRitualWrapper.java`
- `thaumcraft/ThaumcraftHelper.java` — add/sync
- `compat/crafttweaker/CTBewitchmentSpinningWheel.java` — `mods.bewitchment.SpinningWheel`
- Cambion worldgen: `mixin/bewitchment/MixinWorldGenCambionHome.java`, `MixinWorldGenCambionHomeMedium.java` — documented in [rtg.md](rtg.md)

## Do not regress

- Copy constructor args from the parent ritual; a mismatch breaks circles/power.
- Warp only on `onFinished`, not `onStarted` / halt.
- Keep the same registry name so JEI/altar still resolve the ritual.
- Do not register wrappers when Thaumcraft is absent (current code returns early).
- Do not move Cambion paste/skip into this module; it shares village overlap with RTG.
- Do not load `CTBewitchmentSpinningWheel` from always-on bus classes. Input count stays 1–4.

## Out of scope unless asked

- Warp on ritual start
- Wrapping rituals when TC is missing (identity-only wrappers)
- Hedge Witch / Alchemist village pieces
- Other Bewitchment machines (oven, distillery, cauldron, ritual) and multi-output Spinning Wheel recipes
