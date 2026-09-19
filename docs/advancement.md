# Advancement module (1.8)

Last updated: 2026-09-10.

No Tweaks cfg. No bus handler. Optional mixin if Animania Base is present.

## Locked intent

Stop Animania from **reloading every advancement** on each server `WorldEvent.Load`, and do **not** inject Animania **Farm** / **Extra** addon advancement JSON. Pack progression is Triumph / Better Questing.

Do **not** skip vanilla/Forge’s first `AdvancementManager` load. Do **not** cancel other mods’ `WorldEvent.Load` subscribers. Do **not** touch Animania animals, recipes, or addon init.

## How the parent works

Animania Base (`animania`, jar `animania-1.12.2-base-2.0.3.28.jar`) `AddonHandler` is `@EventBusSubscriber`. Farm (`animania-1.12.2-farm-1.0.2.28.jar`) and Extra are **non-mod** jars loaded as addons (`LoadAddon`). FML injects them onto the classpath; they are not Forge containers.

`onWorldLoad(WorldEvent.Load)` (server worlds only, **every dimension**):

1. Reflect the world’s `AdvancementManager` and static `ADVANCEMENT_LIST`.
2. `list.clear()`, reload custom + built-in, then `ForgeHooks.loadAdvancements` (full mod JSON walk — join spam).
3. For each loaded addon except `template`, walk `assets/<addonId>/animania/advancements` via `AddonResourcePack.getAddonPath` and put builders into the map.
4. `list.loadAdvancements` + tree layout.

Forge’s first load (world construct) never sees Farm/Extra JSON. Step 3 is the only inject path. Cancelling the whole method drops the redundant reload **and** Farm/Extra trees.

## Design plan

Optional late mixin `mixins.aqtweaks.animania.json` (`required: false`). `MixinAddonHandler` `@Inject` HEAD `cancellable` on static `onWorldLoad`, `ci.cancel()`. Compile-hard `AddonHandler` from Base.

Farm `animania-1.12.2-farm-1.0.2.28.jar` is compile-hard for the **Rancher** perk (`AnimaniaFarmClocks`). Do **not** mixin Farm/Extra for advancements. Still do not load Farm/Extra advancement JSON (`onWorldLoad` stays cancelled).

Missing Base jar → json skipped; Tweaks still boots.

## Files

| Piece | Role |
| --- | --- |
| `mixin/animania/MixinAddonHandler.java` | Cancel `onWorldLoad` |
| `mixins.aqtweaks.animania.json` | Optional apply |

## Live config

None.

## Do not regress

- Do not mixin Farm/Extra **advancement** classes.
- Farm jar **is** on `libs/` for Rancher animal clocks. Extra stays off unless a later perk needs it.
- Do not put this mixin in **required** `mixins.aqtweaks.json`.
- Do not `@Mod required-after:animania`.
- Recipes skip (`MixinCraftingHelperFindFiles`) stays `/recipes`-gated; it does not swallow advancements.

## Verify

Join: `latest.log` has no `AddonHandler.onWorldLoad` → `ForgeHooks.loadAdvancements` stacks. Mixin json applied. Animania cows/goats/items still exist. Advancement GUI has no Animania Farm/Extra trees from those addon files. Triumph / BQ still load. Remove Animania Base: json skipped.
