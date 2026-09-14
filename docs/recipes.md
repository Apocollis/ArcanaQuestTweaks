# Recipes (Forge CraftingHelper) (1.8)

Last updated: 2026-09-10.

No dedicated cfg. Small loader fix, not a gameplay module. Mixin lives in **required** `mixins.aqtweaks.json` (`MixinCraftingHelperFindFiles`). Forge is always present; this is not optional. Mixin json `compatibilityLevel` is **JAVA_21**.

## Locked intent

Skip broken recipe JSON under paths containing `generated/item/spartanweaponry` and `draugr_ingot_from_block`, as well as recipes referencing any of the 40 known-missing item IDs from pack boot logs (Bibliocraft, Reliquary, Depths Update, DA, AutoOreDictConv, HammerX, Lycanites Mobs, Nether's Delight Legacy) so Forge does not dump 197 `Parsing error loading recipe` stack traces.

Do not blanket-skip unknown items via registry lookups (new unknown items must still dump so pack changes stay visible). Do not mixin extra Forge internals. Do not inject a lambda into `CraftingHelper`.

## How the parent works

Forge `CraftingHelper.findFiles(ModContainer, base, preprocessor, processor, defaultUnfoundRoot, visitAllFiles)` walks a mod jar/dir (e.g. `assets/.../recipes`) and runs `processor` on each file. Signature (remap false):

`findFiles(Lnet/minecraftforge/fml/common/ModContainer;Ljava/lang/String;Ljava/util/function/Function;Ljava/util/function/BiFunction;ZZ)Z`

Forge logs errors inside its own processor (`CraftingHelper.java:726`), so wrapping `processor.apply` in try/catch cannot prevent error spam. Files must be skipped before invoking the processor.

## Design plan

`MixinCraftingHelperFindFiles` `@ModifyVariable` on the `BiFunction` processor (`HEAD`, `argsOnly`, ordinal 0). The mixin only `return RecipeJsonSkip.wrapProcessor(processor, base)`.

`RecipeJsonSkip.wrapProcessor`:

If `processor` is null, or `base` is null, or `base` does **not** contain `/recipes`, return the original processor.

Otherwise wrap (lambda lives in Tweaks, not Forge): if `file` is a `Path` and `shouldSkip(path)` → return `Boolean.TRUE` (Forge treats that as handled) **without** calling the real processor. Else `processor.apply(root, file)`.

`RecipeJsonSkip.shouldSkip`:

1. `SKIP_CONTAINS`:
   - `generated/item/spartanweaponry` (logs once per JVM at INFO: `Skipping Metallurgy recipe JSON under {needle}`)
   - `draugr_ingot_from_block` (skips `da:draugr_ingot_from_block` with unknown type `da:crafting_shaped` without IO)
2. If the file ends with `.json`:
   - Reads UTF-8 content via `Files.readString(file, StandardCharsets.UTF_8)`. IO failure returns `false` (delegated to Forge).
   - Checks for `"<id>"` for any entry in `SKIP_ITEMS` (40 known missing items).
   - If matched, logs once per missing item ID at INFO (`Skipping recipe JSON with known-missing item {}`) via a thread-safe set and returns `true`.

Keep the `/recipes` gate so unrelated `findFiles` walks are untouched.

## Files

- `mixin/MixinCraftingHelperFindFiles.java` (invoke-static only)
- `recipe/RecipeJsonSkip.java` (`shouldSkip`, `wrapProcessor`)

## Do not regress

- Return `Boolean.TRUE` for skips (handled), not `false` (which can look like failure).
- Do not drop the `/recipes` `base` check.
- Do not put a lambda in the mixin body.
- Do not make this mixin `required: false`; it targets Forge.
- Do not skip all unknown items dynamically via registry lookup; keep the explicit allowlist so unexpected broken recipes remain visible.

## Out of scope unless asked

- Fixing the Metallurgy generator itself
- Modifying Forge `CraftingHelper` bytecode directly
