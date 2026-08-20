# Recipes (Forge CraftingHelper)

No dedicated cfg. Small loader fix, not a gameplay module.

## What Tweaks does

Skip broken recipe JSON under paths containing `generated/item/spartanweaponry` so Forge does not fail recipe load (Metallurgy-generated Spartan files).

## How the parent works

Forge `CraftingHelper.findFiles(ModContainer, base, preprocessor, processor, ...)` walks a mod jar/dir (e.g. `assets/.../recipes`) and runs `processor` on each file. A single invalid JSON can spam errors or break load order.

## How Tweaks hooks in

`MixinCraftingHelperFindFiles` `@ModifyVariable` on the `BiFunction` processor (HEAD, argsOnly). If `base` contains `/recipes`, wrap the processor: `RecipeJsonSkip.shouldSkip(Path)` → return `Boolean.TRUE` (treated as handled) without parsing.

Add more path needles in `RecipeJsonSkip.SKIP_CONTAINS` later; do not mixin extra Forge internals unless needed.

## Files

- `mixin/MixinCraftingHelperFindFiles.java`
- `recipe/RecipeJsonSkip.java`
