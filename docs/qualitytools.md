# Quality Tools module (1.8)

Last updated: 2026-09-21. Vanilla loot/durability mixins in `mixins.aqtweaks.early.json`.

Loot stamp, wear/Broken overlays, Dawnstone rune upgrades. Tweaks cfg: `config/arcanaquesttweaks/aqtweaks_qualitytools.cfg`. Mixins: vanilla loot/durability in **`mixins.aqtweaks.early.json`** (jar `MixinConfigs`; pack ships QT). QT + Embers targets in optional `mixins.aqtweaks.qualitytools.json`. Soft `@Mod` `after:qualitytools` (not `required-after`). Embers already `after:`.

## Locked intent

- One live QT `Quality` compound. Tweaks stash `aqtweaks.QualityBase` = last earned **kept** quality (`red` / `yellow` / `green` / `blue` / `gold`). `red` is a real trait, not a wear overlay.
- Pack quality JSON contract (per type): exactly **one** `dark_gray` = Broken; exactly **one** `gray` = wear. Tweaks looks those up by color.
- Wear/Broken never write QualityBase. `red` **does**.
- No QT living-update first stamp. Craft, `/give`, JEI, trades stay untagged until loot/drop/equipment hooks or a rune.
- World loot and drops stamp **before** pickup (`generateQualityTag(stack, false)`). First-gen `dark_gray` starts at **25%** remaining durability; first-gen `gray` at **50%**. Already-worse damage is kept. `red` is not forced to a fraction.
- Wear (default ≤20% remaining): unique `gray` only. **Never** if live is `dark_gray`. Wear never applies `dark_gray`.
- Repair to default ≥75%: strip only live `gray` / `dark_gray`; restore QualityBase (including `red`) or `normal`. Do **not** strip `red`.
- Lethal damage: apply unique `dark_gray`, drop the stack, keep QualityBase, play `ENTITY_ITEM_BREAK` at volume 0.5 / pitch 1.5±0.15 — **except** `charm:salvage`, which Charm already salvages. Tweaks does not cancel that destroy, does not stamp Broken, and does not drop a second copy. Wear still applies. Do not mixin Charm.
- Dawnstone Anvil: **strict** ladder, no skip: `red` → white (`normal`) → `yellow` → `green` → `blue` → `gold`. Common does the first two hammers. Uncommon/Rare/Legendary are yellow→green, green→blue, blue→gold. The **same** rune also rerolls that tier’s color (Common yellow, Uncommon green, Rare blue, Legendary gold). **One rune consumed** per success. Place the **tool/gear first** (slot 0), then the rune (slot 1), same as stock repair. Unwritten Rune (`sccraftingrunes:itemmatbag`) is never matched.
- Do not compile-hard `sccraftingrunes`; cfg defaults are registry names.
- Do not cancel all of `CommonEventHandler.onLivingUpdate` (attribute reapply must stay).
- Do not globally no-op `generateQualityTag(..., false)` (chest/drop stamping uses it).
- Reforging Station: still sync QualityBase after `reforgeTool` if the station exists.

## How the parents work

Quality Tools `1.0.7` (`qualitytools`, `QualityTools-1.0.7_for_1.12.2.jar`) stores `Quality` `{Name, Color, Slots, AttributeModifiers}`. `normal` writes **no** tag. `QualityToolsHelper.generateQualityTag(stack, boolean)`: `false` = first apply (stock living-update lambda); `true` = `TileEntityReforgingStation.reforgeTool`. Apply API is `ConfigLoader.qualityTypes` + `QualityType.itemMatches` / `QualityEntry`.

Embers `1.26.1` `DawnstoneAnvilRecipe.getResult` copies recipe output stacks, not input NBT. `TileEntityDawnstoneAnvil.getResult` **empties both slots first**, then asks the recipe. Leftover runes must be written back onto slot 1 inside `getResult`, not returned as a second output (those eject as item entities). `RecipeRegistry.getDawnstoneAnvilRecipe` returns the first `matches`. Tweaks recipes are inserted at index 0.

Charm Salvage (`charm:salvage`) listens to `PlayerDestroyItemEvent` after destroy. Tweaks’ `attemptDamageItem` mixin runs **before** that. Cancelling destroy would skip Charm.

Vanilla `TileEntityLockableLoot.fillWithLoot` materializes loot on first access (player or hopper).

Upgrade runes are SeriousCreeper Crafting Runes (`sccraftingrunes:itemcommonmat` / `itemuncommonmat` / `itemraremat` / `itemlegendarymat`). Cfg still accepts the old `*_mat` paths as aliases.

## Algorithms

**Stamp (first gen, `false` only)**  
If untagged and `isQualityItem`: `generateQualityTag(stack, false)`. Then pre-damage `dark_gray` to 25% remaining / `gray` to 50% (`setItemDamage`; keep worse). Copy kept live colors to QualityBase. Server: `fillWithLoot` RETURN **only if `lootTable` was set at HEAD**, via `getItems()` (never `getStackInSlot` — that re-enters `fillWithLoot`); `PlayerContainerEvent.Open` (skip player inventory, crafting matrix/result, merchant); `EntityJoinWorld` `EntityItem`; living equipment except players.

**Skip living-update stamp**  
`@Redirect` only the `generateQualityTag` invoke in `CommonEventHandler`’s inventory lambda when the module is enabled.

**Wear / break** (`ItemStack.attemptDamageItem`, server, quality-capable; not creative when a player is present):  
- `isQualityItem` is the JSON whitelist **or** an existing Quality tag (including blank `Quality:{}`). Do not cache while `qualityTypes` is null or empty; never cache false.  
- Lethal + `charm:salvage`: do not cancel, do not Broken, do not Tweaks-drop.  
- Else lethal **with a player**: cancel destroy; durability 1; save QualityBase if empty and live is not gray/Broken; unique `dark_gray`; drop; clear the stack; `ENTITY_ITEM_BREAK`.  
- Else lethal **with a null player** (typical armor `attemptDamageItem`): same stamp + clamp; **leave the piece in the slot** (no drop). Skip the client thread (`FMLCommonHandler` effective side).  
- Missing `dark_gray`: log once, let vanilla destroy.  
- Else remaining/max ≤ `lowDurability`, not Broken, no wear-flag: at most one roll per `wearCheckIntervalTicks` (40) when a player/world is present; null-player armor skips that interval and uses the mixin `Random`. Success p = `(damage/max) × (wearDurabilityRef / max(1, max/2)) × wearChance`, then clamp to `[wearChanceFloor, wearChanceCeiling]` (defaults 0.05–0.50) unless `wearChance` is 0. Then unique `gray`. Missing `gray`: log once.  
- `setItemDamage`: **quality items only** (`isQualityItem`). Leaving the low band clears the wear-flag only. ≥ `highDurability` restores QualityBase if live is gray/Broken. Use the `damage` argument; do not call `getItemDamage()` from the RETURN inject (items such as CR Technomancer Scribing Tools write NBT in `getDamage` and re-enter `setItemDamage`). Re-entry is ThreadLocal-guarded.

**Rune anvil**

| Rune | ID | From (exact kept) | To |
| --- | --- | --- | --- |
| Common | `sccraftingrunes:itemcommonmat` | `red` | strip Quality, clear QualityBase |
| Common | same | `normal` | `yellow` + QualityBase |
| Common | same | `yellow` | reroll `yellow` + QualityBase |
| Uncommon | `sccraftingrunes:itemuncommonmat` | `yellow` | `green` + QualityBase |
| Uncommon | same | `green` | reroll `green` + QualityBase |
| Rare | `sccraftingrunes:itemraremat` | `green` | `blue` + QualityBase |
| Rare | same | `blue` | reroll `blue` + QualityBase |
| Legendary | `sccraftingrunes:itemlegendarymat` | `blue` | `gold` + QualityBase |
| Legendary | same | `gold` | reroll `gold` + QualityBase |

`gray` / `dark_gray` refuse every rune except Common when QualityBase is `red` (gray overlay still counts as red→white). Failed match (quality gear in slot 0 + configured rune in slot 1, `matches` false): action bar `chat.aqtweaks.quality.rune_mismatch`, 40-tick cooldown, rune not consumed. No message for stock repair, empty slots, Unwritten Rune, rune-first placement, or auto-hammers with no nearby player.

JEI: a few example bottoms (iron sword/pick/chestplate). Missing rune item → that recipe does not register.

**Reforge (`true`)**  
Kept live color → QualityBase; `gray` / `dark_gray` / `normal` → clear QualityBase.

## Files

| Piece | Role |
| --- | --- |
| `qualitytools/QualityToolsModule.java` | Bus: container open, entity join; postInit anvil recipes if `embers` |
| `qualitytools/QualityNbt.java` | QualityBase / wear-flag / color apply |
| `qualitytools/QualityStamp.java` | First-gen stamp + pre-damage |
| `qualitytools/QualityDurability.java` | Wear / break / 75% restore; Salvage skip |
| `qualitytools/QualityRuneAnvilRecipe.java` | Dawnstone recipes (Embers import only here + register) |
| `mixin/MixinTileEntityLockableLoot.java` | `fillWithLoot` stamp when loot actually generated. `mixins.aqtweaks.early.json` |
| `mixin/MixinItemStackQualityDurability.java` | `attemptDamageItem` + `setItemDamage`. `mixins.aqtweaks.early.json` |
| `mixin/qualitytools/MixinCommonEventHandler.java` | Skip living-update stamp |
| `mixin/qualitytools/MixinTileEntityReforgingStation.java` | Sync QualityBase after reforge |
| `mixin/qualitytools/MixinTileEntityDawnstoneAnvil.java` | Failed-rune action bar |
| `mixins.aqtweaks.qualitytools.json` | `required: false` |
| `ArcanaQuestTweaksConfig.QualityToolsModuleConfig` | `aqtweaks_qualitytools.cfg` |

Vanilla durability/loot mixins FQCN into `QualityStamp` / `QualityDurability` (compile-hard QT; no parent `import` on the mixin class). Do not import QT from `CommonProxy` or `ArcanaQuestTweaksConfig`. Pack ships Quality Tools.

## Live config (`config/arcanaquesttweaks/aqtweaks_qualitytools.cfg`)

| Category / Key | Type | Default | Description |
| --- | --- | --- | --- |
| `general` / `Enable Quality Tools Module` | bool | true | Master switch. False: living-update stamp is not skipped; Tweaks wear/break/runes/loot stamp off |
| `general` / `Low Durability` | double | 0.20 | Wear at or below this remaining/max |
| `general` / `High Durability` | double | 0.75 | Repair ratio that clears gray/dark_gray |
| `general` / `Wear Chance` | double | 1.0 | Multiplier on used × (ref / (max/2)). 0 disables wear |
| `general` / `Wear Chance Floor` | double | 0.05 | Minimum p when Wear Chance > 0 |
| `general` / `Wear Chance Ceiling` | double | 0.50 | Maximum p when Wear Chance > 0 |
| `general` / `Wear Durability Ref` | int | 250 | Hardness term: ref / (max/2). Vanilla iron tool max |
| `general` / `Wear Check Interval Ticks` | int | 40 | Minimum ticks between wear rolls on one stack |
| `general` / `Common Rune` | string | `sccraftingrunes:itemcommonmat` | red→white, white→yellow, yellow reroll; `common_mat` alias |
| `general` / `Uncommon Rune` | string | `sccraftingrunes:itemuncommonmat` | yellow→green, green reroll; `uncommon_mat` alias |
| `general` / `Rare Rune` | string | `sccraftingrunes:itemraremat` | green→blue, blue reroll; `rare_mat` alias |
| `general` / `Legendary Rune` | string | `sccraftingrunes:itemlegendarymat` | blue→gold, gold reroll; `legendary_mat` alias |

## Pack instance (not this repo)

Quality JSON must keep **one** `dark_gray` and **one** `gray` per type. **`Quailities/tools.json` currently has `gray` (chipped) but no `dark_gray`** — add that or tool breaks cannot stamp Broken. Optional: weight 0 on those two so loot never rolls them (Tweaks still applies them). Shields/fishing need a `blue` entry before Rare/Legendary match. `contenttweaker:reforge_rune` stays the Reforging Station material until that station is removed.

## Do not regress

- Do not `@Mod required-after:qualitytools`.
- Do not cancel all of `onLivingUpdate`.
- Do not mixin Charm; Salvage and Tweaks-Broken are mutually exclusive on the lethal hit.
- Do not consume Unwritten Rune / `itemmatbag` (`mat_bag`).
- Do not skip rungs (Common cannot turn `red` or `yellow` into `green` in one hammer). Same-tier reroll is not a skip.
- Do not stamp `ContainerPlayer` crafting slots or merchant trades.
- Do not put QT or Embers types on `CommonProxy` / `ArcanaQuestTweaksConfig`.
- Do not put `MixinTileEntityLockableLoot` / `MixinItemStackQualityDurability` in late `mixins.aqtweaks.json`.
- Do not cache `isQualityItem` while `qualityTypes` is null or empty; never cache a false miss.
- Do not require a live Quality tag (or a player) before wear/Broken; armor often calls `attemptDamageItem` with a null damager.
- Do not call `getItemDamage()` from `setItemDamage` RETURN. Do not run wear/restore on non-quality items.
- Do not call `getStackInSlot` from `fillWithLoot` RETURN. Nested `fillWithLoot` (loot table already null) must not stamp.

## Verify

1. Craft a sword: no Quality in chest/hotbar.
2. Open a loot chest: `dark_gray` ~25% remaining, `gray` ~50%; pickup does not reroll.
3. Dawnstone: tool first, then the next rune; same rune again rerolls that color. Wrong rune: action bar, rune not consumed. Second Common after red→white → yellow.
4. Wear below 20%: stone/iron ~50% per eligible 40-tick damage hit; diamond pick ~0.29 at 90% used. Crafted untagged sword/shovel still eligible. QualityBase unchanged; repair to 75% restores base. `Wear Chance` 0 never stamps. Armor with a null damager stamps in-slot (Broken stays equipped).
5. Break without Salvage: drop `dark_gray`; QualityBase intact; Salvage-matching sound. Break with Salvage: Charm drop at 0 durability; no Tweaks second copy.
6. Boot without `qualitytools`: Tweaks mixin json for QT classes skipped; vanilla loot/durability mixins still apply (NCDFE if those helpers run — pack ships QT).
7. `.\build_gradle.ps1`: Java 21, QT + Embers jars in `libs/`.
