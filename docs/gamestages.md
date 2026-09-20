# Game Stages module (1.8)

Last updated: 2026-09-20.

Optional Chisel gate when Recipe Stages has staged the **output** item. Tweaks cfg: `config/arcanaquesttweaks/aqtweaks_gamestages.cfg`. Mixins: `mixins.aqtweaks.chisel.json` and `mixins.aqtweaks.recipestages.json` (`required: false`). Soft `@Mod` `after:chisel;after:gamestages;after:recipestages` (not `required-after`).

## Locked intent

Stop players from chiseling (GUI or in-world) into a block whose item is Recipe-Staged unless `GameStageHelper.hasStage(player, stage)` is true. Notify on the action bar. Pack stages are `apprentice_builder`, `experienced_builder`, and `master_builder` (`scripts/crafttweaker/recipe_stages_building.zs`).

Do **not** empty Chisel variant slots (that would hide the output and skip the message). Do **not** mixin AutoChisel. Do **not** mixin Chisels and Bits. Do **not** change crafting-table Recipe Stages.

## How the parents work

Recipe Stages (`recipestages`, `recipestages-2.0.1.jar`) `Recipes.setRecipeStage(String, IIngredient)` records outputs then wraps matching Forge `IRecipe`s. Public `Recipes.recipes` is `Map<String, List<IRecipe>>`. Many `chisel:*` variants have **no** crafting-table recipe, so they never appear in that map.

Chisel (`chisel`, `Chisel-MC1.12.2-1.0.2.45.jar`) `SlotChiselSelection.craft` calls `IChiselItem.canChisel`. Stock `ItemChisel.canChisel` only checks the chisel stack is non-empty. `ChiselMode` also calls `canChisel` for in-world left-click. Hitech GUI still uses `SlotChiselSelection`. `InventoryChiselSelection.updateItems` only fills variant slots.

Game Stages (`gamestages`, `GameStages-1.12.2-2.0.123.jar`) `GameStageHelper.hasStage(player, stage)`.

## Design plan

Compile-hard Chisel, Game Stages, Recipe Stages, and CraftTweaker `IIngredient` / `CraftTweakerMC`. Keep those imports off `CommonProxy` and `ArcanaQuestTweaksConfig`. No bus handler.

1. `MixinRecipesSetRecipeStage` `@Inject` HEAD on `Recipes.setRecipeStage(String, IIngredient)` → `GameStagesRecipeIndex.add`.
2. `GameStagesRecipeIndex.matchingStages`: captured ingredients that `matches` the output (CT wildcard `:*` works), cached per item id+meta (cleared on each capture). Fallback: `Recipes.recipes` outputs via `OreDictionary.itemMatches` / `ItemStack.areItemsEqual`. Allowlist is applied in `GameStagesChiselHooks`.
3. `GameStagesChiselHooks.lockedStage`: module enable, not FakePlayer, both `gamestages` and `recipestages` loaded, then `GameStageHelper.hasStage`. Nested `Lookup` so missing Game Stages does not load `GameStageHelper` until the check runs.
4. `MixinSlotChiselSelection` HEAD on `craft`: if locked, return `ItemStack.EMPTY`. Notify only when `simulate == false` and server.
5. `MixinItemChisel` RETURN on `canChisel`: if variation output locked, return false and notify (in-world path). 40-tick cooldown per player.

Allowlist default is the three builder stages. Empty allowlist (all entries blank) = every captured / mapped stage.

Missing Chisel → chisel json skipped. Missing Recipe Stages → capture mixin skipped; Chisel mixins no-op the index. Missing Game Stages → `lockedStage` returns null.

## Files

| Piece | Role |
| --- | --- |
| `gamestages/GameStagesRecipeIndex.java` | CT ingredient capture + `Recipes.recipes` fallback |
| `gamestages/GameStagesChiselHooks.java` | Stage check, notify, allowlist |
| `mixin/chisel/MixinSlotChiselSelection.java` | GUI craft deny |
| `mixin/chisel/MixinItemChisel.java` | `canChisel` deny (in-world) |
| `mixin/recipestages/MixinRecipesSetRecipeStage.java` | Capture `setRecipeStage` |
| `ArcanaQuestTweaksConfig.GameStagesModuleConfig` | Nested `general`; `aqtweaks_gamestages.cfg` |
| `assets/aqtweaks/lang/en_us.lang` | `chat.aqtweaks.gamestages.chisel_locked` |

## Live config (`config/arcanaquesttweaks/aqtweaks_gamestages.cfg`)

| Category / Key | Type | Default | Description |
| --- | --- | --- | --- |
| `general` / `Enable Game Stages Module` | boolean | `true` | When false, Chisel is not gated. |
| `general` / `Chisel Stage Allowlist` | string[] | `apprentice_builder`, `experienced_builder`, `master_builder` | Only these Recipe Stages apply to Chisel. Empty = all captured stages. |

In-game Tweaks GUI syncs without a restart. Hand-edit of the cfg file on a dedicated server needs a restart.

## Do not regress

- Do not put Game Stages / Recipe Stages / Chisel types on `CommonProxy` or `ArcanaQuestTweaksConfig`.
- Do not `@Mod required-after` those three.
- Do not hide `InventoryChiselSelection` variant slots.
- Do not mixin AutoChisel or Chisels and Bits.
- Do not notify on the `craft` simulate path.
- Do not spam the action bar (`NOTIFY_COOLDOWN_TICKS` = 40).
- Do not put string/primitive fields on a `@Config(..., category = "")` class. Nest them under `general`.
- Do not treat CraftTweaker scripts in the pack instance as Tweaks source.

## Verify

1. Build succeeds with `options.release = 21` (skipped this change unless asked).
2. No `apprentice_builder`: chiseling a staged output (GUI and in-world) fails, red action bar, input not consumed.
3. `/gamestage add @p apprentice_builder`: that tier’s variants chisel; higher tiers still locked.
4. Unstaged variants still chisel.
5. Crafting-table Recipe Stages unchanged.
6. Strip Chisel or Recipe Stages: Tweaks still loads; matching json skipped.
