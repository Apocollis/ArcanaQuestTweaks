# Infinity Blade (1.10)

Config: `config/arcanaquesttweaks/aqtweaks_weapon.cfg` (`InfinityBladeModuleConfig`). Package `com.apocollis.aqtweaks.weapon`. No parent mod is required.

## What it is

`aqtweaks:infinity_blade`, an `ItemSword` (`ItemInfinityBlade`) with the animated 36-frame texture from the old ContentTweaker placeholder. It replaces the ContentTweaker `contenttweaker:infinity_sword`, which never loaded under Cleanroom (ghost item), so its recipe and attribute NBT never worked. No swords existed from the old item, so there is no remap.

Because it is an `ItemSword`, attack cooldown, sweep, shield disable, Dynamic Sword Skills and Quality Tools work as for any sword. Stamina treats any `ItemSword` as a medium weapon (`StaminaModule.classifyWeapon` fall-through), so nothing is added there.

## Config

| Key | Default | Notes |
| --- | --- | --- |
| `Attack Damage` | 18.0 | Total damage (the player's base 1 is included; the modifier is `damage - 1`, never below 0). Range 1 to 10000 |
| `Attack Speed` | 1.6 | Attacks per second (the modifier is `speed - 4`). Range 0.1 to 20 |
| `Durability` | 6000 | Range 1 to 1000000; read live by `getMaxDamage` |
| `Repair Item` | `minecraft:nether_star` | Registry name that repairs it in an anvil; `none` or empty disables anvil repair |
| `Enable Default Recipe` | **false** | Registers a placeholder shaped recipe (8 Nether Stars around a Diamond Sword). Off because the crafting method is undecided; needs a restart. With it off Tweaks registers no recipe, so a CraftTweaker or JSON recipe has no conflict |

Damage and speed are read live and apply the next time the item is equipped or its tooltip is built.

## Rarity

The item reports vanilla Epic. Its tier comes from the [rarity overhaul](rarity.md): add `aqtweaks:infinity_blade=LEGENDARY` to `Custom Item Rarities`. The list is empty by default.

## Files

`weapon/ItemInfinityBlade`, `weapon/WeaponModule` (item and recipe registration), `weapon/InfinityBladeStats` (pure damage, speed and repair-match maths, unit tested), `weapon/WeaponRecipeHandler` (placeholder recipe), `weapon/client/WeaponClientEvents` (inventory model). Assets: `assets/aqtweaks/models/item/infinity_blade.json`, `textures/items/infinity_blade.png` (32 x 1152, 36 frames of 32 x 32) and `.png.mcmeta` (frametime 2). Lang `item.aqtweaks.infinity_blade.name`.

## DEVBOX

The old `scripts/contenttweaker/infinity_sword.zs` and `scripts/crafttweaker/infinity_sword_stats.zs` were deleted. The ContentTweaker model, texture and lang for `infinity_sword` were left in place (unused).

## Do not regress

- Keep stats in config, not hard-coded; keep `Enable Default Recipe` off by default.
- Keep the repair item config-driven (`none` disables it).
- Do not add rarity logic here; the rarity overhaul owns it.
