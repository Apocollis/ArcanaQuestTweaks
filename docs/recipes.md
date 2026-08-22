# Recipes (Forge CraftingHelper) (1.6)

Last updated: 2026-08-22.

No dedicated cfg. Small loader fix, not a gameplay module. Mixin lives in **required** `mixins.aqtweaks.json` (`MixinCraftingHelperFindFiles`). Forge is always present; this is not optional. Mixin json `compatibilityLevel` is **JAVA_21**.

## Locked intent

Skip broken recipe JSON under paths containing `generated/item/spartanweaponry` so Forge does not fail or spam while loading Metallurgy-generated Spartan Weaponry files. Do not parse those files. Do not mixin extra Forge internals unless a new dead tree appears. Do not inject a lambda into `CraftingHelper`.

## How the parent works

Forge `CraftingHelper.findFiles(ModContainer, base, preprocessor, processor, defaultUnfoundRoot, visitAllFiles)` walks a mod jar/dir (e.g. `assets/.../recipes`) and runs `processor` on each file. Signature (remap false):

`findFiles(Lnet/minecraftforge/fml/common/ModContainer;Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/BiFunction;ZZ)Z`

A single invalid JSON can spam errors or break load order.

## Design plan

`MixinCraftingHelperFindFiles` `@ModifyVariable` on the `BiFunction` processor (`HEAD`, `argsOnly`, ordinal 0). The mixin only `return RecipeJsonSkip.wrapProcessor(processor, base)`.

`RecipeJsonSkip.wrapProcessor`:

If `processor` is null, or `base` is null, or `base` does **not** contain `/recipes`, return the original processor.

Otherwise wrap (lambda lives in Tweaks, not Forge): if `file` is a `Path` and `shouldSkip(path)` → return `Boolean.TRUE` (Forge treats that as handled) **without** calling the real processor. Else `processor.apply(root, file)`.

`RecipeJsonSkip.SKIP_CONTAINS` currently:

- `generated/item/spartanweaponry`

Path matching uses `Path.toString()` with `\` → `/`. First skip logs once per JVM (`AtomicBoolean`) at INFO: `Skipping Metallurgy recipe JSON under {needle}`.

Add more needles in that array later. Keep the `/recipes` gate so unrelated `findFiles` walks are untouched.

## Files

- `mixin/MixinCraftingHelperFindFiles.java` (invoke-static only)
- `recipe/RecipeJsonSkip.java` (`shouldSkip`, `wrapProcessor`)

## Do not regress

- Return `Boolean.TRUE` for skips (handled), not `false` (which can look like failure).
- Do not drop the `/recipes` `base` check.
- Do not put a lambda in the mixin body.
- Do not make this mixin `required: false`; it targets Forge.

## Out of scope unless asked

- Fixing the Metallurgy generator itself
- Skipping by recipe serializer / JSON parse errors instead of path
