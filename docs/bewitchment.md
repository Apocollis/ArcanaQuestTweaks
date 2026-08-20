# Bewitchment module (1.6)

Last updated: 2026-08-20.

Config: `config/arcanaquesttweaks/aqtweaks_bewitchment.cfg`. `BewitchmentRegistryHandler` registers on the Forge bus only if `bewitchment` is loaded.

Cambion house Y+1 paste and village-skip live in the RTG module ([rtg.md](rtg.md)), not here. Those mixins are `mixins.aqtweaks.bewitchment.json` (`required: false`). This file is **ritual warp only**.

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

## Files

- `thaumcraft/BewitchmentRegistryHandler.java` (package is historical)
- `thaumcraft/WarpRitualWrapper.java`
- `thaumcraft/ThaumcraftHelper.java` — add/sync
- Cambion worldgen: `mixin/bewitchment/MixinWorldGenCambionHome.java`, `MixinWorldGenCambionHomeMedium.java` — documented in [rtg.md](rtg.md)

## Do not regress

- Copy constructor args from the parent ritual; a mismatch breaks circles/power.
- Warp only on `onFinished`, not `onStarted` / halt.
- Keep the same registry name so JEI/altar still resolve the ritual.
- Do not register wrappers when Thaumcraft is absent (current code returns early).
- Do not move Cambion paste/skip into this module; it shares village overlap with RTG.

## Out of scope unless asked

- Warp on ritual start
- Wrapping rituals when TC is missing (identity-only wrappers)
- Hedge Witch / Alchemist village pieces
