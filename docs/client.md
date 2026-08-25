# Client module (1.6)

Last updated: 2026-08-24.

Config: `config/arcanaquesttweaks/aqtweaks_client.cfg`. Client-only. No `@Mod` parent. Optional mixin if Toughness Bar is present.

## Locked intent

HUD and tooltip presentation that is not stamina spend. Do **not** change Elenai feather or thirst icons. Do **not** mixin Metallurgy. Skip `metallurgy:` items so Reforged’s own shift stats are not doubled.

## How the parents work

### Toughness Bar 2.4 (`toughnessbar`)

`EventHandlerClient.onRenderArmorToughnessEvent` listens to `FOOD` Post. Stock overlay uses `GuiIngameForge.right_height` and X `width/2 + 82`, then `IINC` **-8** (hunger column, right to left).

Vanilla armor uses `width/2 - 91` and steps **+8**. **Overloaded Armor Bar** cancels vanilla ARMOR and never increments `left_height`.

### Metallurgy 4 Reforged (`metallurgy`)

`ItemUtils.buildStatsTooltip` (shift-gated) adds harvest stars (`U+2B51`), durability `remaining/max`, and efficiency float with `ScaleFormatting` colors. Harvest stars only on pickaxes in Metallurgy; Tweaks still shows harvest for any mining tool that reports a harvest level. Creative tab string is `Metallurgy Tools`.

## Design plan

### Toughness HUD

Optional late mixin `mixins.aqtweaks.toughnessbar.json` (`required: false`), `MixinEventHandlerClient`.

When `Move Toughness Bar To Armor Side` is true:

- GET `right_height` → `left_height + 10` (sit one row above Overloaded Armor Bar).
- PUT `right_height` → `left_height`.
- Constant `82` → `-91` (armor left edge).
- Constant `-8` → `+8` (left to right, matching armor).

When false: stock right-side RTL. Missing jar → json skipped.

### Metallurgy tooltip compat

`ClientModule` on `ItemTooltipEvent`. If the flag is on, the stack is a mining tool (`ItemPickaxe` / `ItemAxe` / `ItemSpade` / `ItemTool`, or tool classes `pickaxe`/`axe`/`shovel`), and the registry domain is not `metallurgy`, append:

1. `§9{Name} Tools` — `minecraft` → Vanilla; else Forge mod display name (fallback modid).
2. `Harvest Level:` colored stars — max `getHarvestLevel` over tool classes; star count `clamp(harvest + 1, 1, 7)`; omit if all levels `< 0`.
3. `Durability:` remaining/max — skip if `maxDamage <= 0`. Color: remaining ratio `< 0.33` red, `< 0.66` yellow, else green.
4. `Efficiency:` `item.getDestroySpeed` on stone / log / dirt for the tool class (base speed, not Efficiency enchant). Color index `ceil(clamp(speed - 6, 0, 6))` into the same seven Metallurgy colors.

Always visible (Metallurgy’s own items stay shift-gated by Metallurgy). Swords and hoes are skipped unless they expose those tool classes.

## Files

| Piece | Role |
| --- | --- |
| `ArcanaQuestTweaksConfig.ClientModuleConfig` | `aqtweaks_client.cfg` |
| `client/ClientModule.java` | Tooltip handler; registered in `ClientProxy` |
| `mixin/toughnessbar/MixinEventHandlerClient.java` | Optional Toughness Bar overlay |

## Live config

| Knob | Default | Notes |
| --- | --- | --- |
| Move Toughness Bar To Armor Side | true | Armor column, LTR. Old `aqtweaks_stamina.cfg` HUD key is dead |
| Metallurgy Tooltip Compat | true | Non-Metallurgy mining tools only |

## Do not regress

- Feathers and thirst stay on the right.
- Toughness must not share the armor **row** (keep `left_height + 10` GET).
- Do not import Metallurgy types. Do not add toughness mixin to the required json.
- Durability line must not appear on unbreakable tools (`maxDamage <= 0`).

## Verify

Armor + toughness: icons left to right, one row above armor; feathers above thirst. Flag off: stock right RTL. Iron pick: Vanilla Tools + stars + durability + efficiency. Axe efficiency is log speed, not 1.0. Metallurgy pick: no second Tweaks harvest line. Remove toughnessbar jar: mixin skipped; tooltips still work.