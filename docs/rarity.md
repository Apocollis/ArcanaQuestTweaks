# Rarity overhaul (1.10)

Config: `config/arcanaquesttweaks/aqtweaks_rarity.cfg` (`RarityModuleConfig`). Package `com.apocollis.aqtweaks.rarity`. No parent mod is required; Quality Tools is read through item NBT only.

## What it does

One six-tier ladder across vanilla rarity, enchantments and Quality Tools qualities:

| Tier | Color | Source |
| --- | --- | --- |
| Common | `§f` white | native |
| Uncommon | `§a` green | native; common/uncommon enchantments; green and yellow qualities |
| Rare | `§3` dark aqua | native; rare enchantments; dark_aqua qualities. Matches the Rare Rune's own `§3` |
| Epic | `§d` light purple (vanilla) | native; very rare enchantments |
| Legendary | `§6` gold | gold qualities; config overrides |
| Mythic / Relic | rainbow in tooltips (base color Legendary gold) | config overrides only |

**Red, gray and dark gray belong to Quality Tools' negative qualities**, so no rarity uses them and a negative quality never changes a tier.

## Resolution

For one stack: native `Item.getRarity`, then the enchantment mapping, then the quality mapping, then `Custom Item Rarities`.

- The tier before the override is the **highest** of the base tier, the best non-curse enchantment tier and the quality tier. Traits never stack: a Common item with a Very Rare enchantment (Epic) and a gold quality (Legendary) is Legendary. A trait never lowers a tier.
- **Enchantments.** Each enchantment's own `Enchantment.Rarity` is mapped (`Enchantment Rarity Map`, default COMMON and UNCOMMON to Uncommon, RARE to Rare, VERY_RARE to Epic). Curses are skipped; a curses-only item keeps its native tier. Enchanted books use stored enchantments, not the `ench` tag, and keep their own rating.
- **Base tier.** Vanilla's `Item.getRarity` answers Rare for any enchanted item that does not set its own rarity, which would hide the mapping. When an enchanted stack's native tier is Rare, the unenchanted rating of the item is probed once (cached per item and subtype meta; damage is not a subtype): a Common probe means the Rare came from the bump and the base is Common, so a Sharpness-only sword is Uncommon and a Mending sword is Rare. A genuinely Rare, Epic or Legendary item never drops.
- **Quality Tools.** The roll is the root NBT compound `Quality`; its `Color` string (for example `gold`) is mapped by `Quality Color Map` (default green and yellow to Uncommon, dark_aqua to Rare, gold to Legendary). Colors not listed do nothing.
- **Overrides** are applied last and always win, including a lower tier. Format `modid:item=TIER`, `modid:item:meta=TIER` or `modid:*=TIER`; precedence is meta, then item, then `modid:*`; `RELIC` is accepted for `MYTHIC`. The list is **empty** by default. Examples: `minecraft:nether_star=LEGENDARY`, `aqtweaks:infinity_blade=LEGENDARY`.

## Config keys

| Key | Default | Notes |
| --- | --- | --- |
| `Enable Rarity Overhaul` | true | false leaves every rarity and color alone |
| `Color Uncommon` / `Color Rare` / `Color Epic` / `Color Legendary` | `a` / `3` / `d` / `6` | one of 0-9, a-f, optionally prefixed `§` or `&`; a bad value logs a warning and uses the default. Mythic uses the Legendary color as its base |
| `Mythic Style` | ANIMATED_RAINBOW | or STATIC_RAINBOW; unknown values fall back |
| `Enchantment Rarity Mapping` / `Enchantment Rarity Map` | true / see above | bad lines are skipped with a warning |
| `Quality Rarity Mapping` / `Quality Color Map` | true / see above | bad lines are skipped with a warning |
| `Custom Item Rarities` | empty | see Resolution |
| `Show Rarity Subtitle` | false | a tooltip line under the name with the tier |
| `Subtitle Visibility` | ALL | or ABOVE_COMMON, HIGH_ONLY |
| `Subtitle Style` | ICON | or CLEAN |

## How it works

- `RarityModule.init()` (from `CommonProxy.preInit`, both sides) reads the config into an immutable `State`, adds `LEGENDARY` and `MYTHIC` to `EnumRarity` with Forge's `EnumHelper.addRarity`, and sets the display color of Uncommon, Rare, Epic, Legendary and Mythic by writing the `TextFormatting` field of `EnumRarity` (found by type; the name differs between mapped and runtime builds). If either step fails it logs and keeps vanilla colors, and Legendary and Mythic show as Epic. `RarityModule.reload()` re-reads on a config change.
- Two **early** mixins call `RarityOverrides.resolve` by FQCN (they import no mod type): `MixinItemStackRarity` on `ItemStack.getRarity`, and `MixinItemRarity` on `Item.getForgeRarity` (`remap = false`, a Forge method). Tooltips and JEI color line 0 from `getForgeRarity`, not `ItemStack.getRarity`, so both are needed. `MixinItemRarity` only replaces an `EnumRarity` and leaves custom `IRarity` objects alone.
- `client/RarityTooltipHandler` (an `ItemTooltipEvent` at LOW priority) rebuilds line 0 as a per-character rainbow for a Mythic stack and optionally inserts the subtitle at index 1. JEI's hover tooltip fires the same event.
- Pure, unit-tested classes (no Minecraft types): `RarityTier`, `RarityColors`, `RarityOverrideParser`, `RarityEnchantTier`, `RarityQualityTier`, `RarityRainbow`.

## Notes and limits

- Quality Tools adds a separate `Quality: <name>` tooltip line colored from its own JSON; the rarity color is on the name line. Its quality JSON colors changed from `blue` to `dark_aqua` in the DEVBOX on 2026-11-10 to match Rare.
- The Crafting Runes (`sccraftingrunes:itemcommonmat`, `itemuncommonmat`, `itemraremat`, `itemlegendarymat`) color their own names with lang codes (`§a`, `§3`, `§6`), so they never show the rarity color and need no override.
- The hotbar item-name popup is plain white in vanilla and is not changed. A hook for it is parked in `.plans/parked/20261110-0745-hotbar-rarity-name-hook.md`.
- A mod that overrides `Item.getForgeRarity` itself bypasses the `Item` mixin (check in game).
- The enchantment read happens on every `getRarity` call for an enchanted stack. If Spark shows it, add a per-tag cache.

## Files

`rarity/RarityModule`, `RarityRegistry`, `RarityOverrides`, `RarityTier`, `RarityColors`, `RarityOverrideParser`, `RarityEnchantTier`, `RarityQualityTier`, `RarityRainbow`, `client/RarityTooltipHandler`, `mixin/MixinItemStackRarity`, `mixin/MixinItemRarity` (both in `mixins.aqtweaks.early.json`), `ClientProxy` (tooltip handler), `CommonProxy.preInit` (`RarityModule.init`), `ArcanaQuestTweaksConfig` (`RarityModuleConfig`, reload hook), lang keys `aqtweaks.rarity.tier.*` and `aqtweaks.rarity.icon.*`.

## Do not regress

- The early mixins must not import mod or parent types; keep the FQCN call.
- With `Enable Rarity Overhaul` false, with an empty override list and with both mappings off, `resolve` must return the native rarity unchanged.
- A trait must never lower a tier; only an explicit `Custom Item Rarities` entry may.
- Keep red, gray and dark gray out of the rarity palette.
