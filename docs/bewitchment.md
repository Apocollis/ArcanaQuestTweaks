# Bewitchment module

Config: `config/arcanaquesttweaks/aqtweaks_bewitchment.cfg`. Registers only if `bewitchment` is loaded. Warp apply also needs `thaumcraft`.

## What Tweaks does

When listed Bewitchment rituals **finish**, grant Thaumcraft warp (normal, temporary, optional permanent).

Cambion house Y+1 paste and village-skip live in the RTG module ([rtg.md](rtg.md)), not here. No land plate.

## How the parent mod works

Bewitchment registers `com.bewitchment.api.registry.Ritual` on a Forge registry (`RegistryEvent.Register<Ritual>`). Each ritual has:

- Registry name (`bewitchment:conjure_demon`, hellmouths, etc.)
- Circle inputs, sacrifice predicate, outputs, power, duration
- Callbacks: `isValid`, `onStarted`, `onFinished`, `onHalted`, `onUpdate`

The altar runs the **registry value** for that id. Re-registering the **same** `ResourceLocation` replaces the entry. Tweaks does not edit Bewitchment’s class; it wraps the existing ritual object.

## How Tweaks hooks in

`BewitchmentRegistryHandler.registerRituals`:

1. For each cfg line `id=normal,temp[,permanent]`, look up the original `Ritual`.
2. Construct `WarpRitualWrapper` with the same registry name and copied fields.
3. `registry.register(wrapper)` overwrites the id.

`WarpRitualWrapper` extends `Ritual` and **delegates** every callback to `parent`. After `parent.onFinished(...)`, if the player is server-side, `ThaumcraftHelper.addWarp` + `syncWarp`.

If TC is missing, the wrapper still registers but warp calls no-op inside the helper.

## Files

- `thaumcraft/BewitchmentRegistryHandler.java` (package is historical)
- `thaumcraft/WarpRitualWrapper.java`

## Do not regress

- Copy constructor args from the parent ritual; a mismatch breaks circles/power.
- Warp only on `onFinished`, not `onStarted` / halt.
- Keep the same registry name so JEI/altar still resolve the ritual.
