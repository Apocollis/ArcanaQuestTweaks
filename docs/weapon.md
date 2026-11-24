# Additions — Infinity Blade & Emptiness Bosses (1.10)

Config: `config/arcanaquesttweaks/aqtweaks_additions.cfg` (`AdditionsModuleConfig`, migrated automatically from legacy `aqtweaks_weapon.cfg`). Package `com.apocollis.aqtweaks.additions`.
- **Infinity Blade** (`aqtweaks:infinity_blade`): no parent mod required.
- **The Nameless Vessel** (`aqtweaks:nameless_vessel`, entity ID `6`, Stage 1) and **The Nameless One** (`aqtweaks:nameless_one`, entity ID `5`, Stage 2): registered when `geckolib3` (`SauriaLib 3`) and `thaumcraft` are loaded.

## Infinity Blade

`aqtweaks:infinity_blade`, an `ItemSword` (`ItemInfinityBlade`) with the animated 36-frame texture from the old ContentTweaker placeholder. It replaces the ContentTweaker `contenttweaker:infinity_sword`, which never loaded under Cleanroom (ghost item), so its recipe and attribute NBT never worked. No swords existed from the old item, so there is no remap.

Because it is an `ItemSword`, attack cooldown, sweep, shield disable, Dynamic Sword Skills and Quality Tools work as for any sword. Stamina treats any `ItemSword` as a medium weapon (`StaminaModule.classifyWeapon` fall-through), so nothing is added there.

### Config (`Infinity Blade` category)

| Key | Default | Notes |
| --- | --- | --- |
| `Attack Damage` | 18.0 | Total damage (the player's base 1 is included; the modifier is `damage - 1`, never below 0). Range 1 to 10000 |
| `Attack Speed` | 1.6 | Attacks per second (the modifier is `speed - 4`). Range 0.1 to 20 |
| `Durability` | 6000 | Range 1 to 1000000; read live by `getMaxDamage` |
| `Repair Item` | `minecraft:nether_star` | Registry name that repairs it in an anvil; `none` or empty disables anvil repair |
| `Enable Default Recipe` | `false` | Registers a placeholder shaped recipe (8 Nether Stars around a Diamond Sword). Off because the crafting method is undecided; needs a restart. With it off Tweaks registers no recipe, so a CraftTweaker or JSON recipe has no conflict |

Damage and speed are read live and apply the next time the item is equipped or its tooltip is built.

### Rarity

The item reports vanilla Epic. Its tier comes from the [rarity overhaul](rarity.md): add `aqtweaks:infinity_blade=LEGENDARY` to `Custom Item Rarities`. The list is empty by default.

## The Nameless Vessel & The Nameless One

Two-stage final boss encounter for The Emptiness (`geckolib3` + `thaumcraft`):
- **Stage 1 — The Nameless Vessel (`aqtweaks:nameless_vessel`, ID `6`):** `0.85 × 2.45` block floating cultist vessel (`450 HP`, `8` armor, `4` toughness, `0.85` KB resist, `12.0` melee damage, `100.0` single-hit damage cap, purple `BossInfoServer`). On lethal damage (`health - amount <= 1.0`), intercepts death, plays the 30-tick (`1.50 s`) `animation.nameless_vessel.erupt` sequence, and spawns Stage 2 (`EntityNamelessOne`) at its position in `animation.nameless_one.spawn_erupt` without dropping Stage 1 loot.
- **Stage 2 — The Nameless One (`aqtweaks:nameless_one`, ID `5`):** `2.0 × 5.4` block cosmic avatar (`1200 HP`, `12` armor, `8` toughness, `1.0` KB resist, `18.0` melee damage, `150.0` single-hit damage cap, yellow notched-6 `BossInfoServer`). Uses a 90-tick (`4.50 s`) custom `onDeathUpdate` for its 3-beat death sequence (`halo_ring` shatter hidden after tick 31, heart core rupture at tick 32, and smooth alpha dissolve over ticks 66–90).
- Both entities render through SauriaLib 3 (`GeoEntityRenderer` + fullbright `GeoLayerRenderer` glowmasks for the eye, chest glyph, halo arcs, and heart core).

### Nameless config (`aqtweaks_additions.cfg`, categories `Nameless Vessel`, `Nameless One` and the module root)

| Key | Vessel | One | Use |
| --- | --- | --- | --- |
| Enable Nameless Vessel / Enable Nameless One | true | true | Register the entity (restart; needs SauriaLib 3 and Thaumcraft) |
| Max Health | 450 | 1200 | Health |
| Armor | 8 | 12 | Armor |
| Armor Toughness | 4 | 8 | Armor toughness |
| Knockback Resistance | 0.85 | 1.0 | 0 to 1 |
| Melee Attack Damage | 12 | 18 | Base claw swipe damage |
| Movement Speed | 0.35 | 0.40 | Hover glide speed attribute |
| Follow Range | 48 | 64 | Target detection range in blocks |
| Single Hit Damage Cap | 100 | 150 | The most one hit can take (0 turns it off) |
| Eruption Transition Ticks | 30 | | Stage 1 to Stage 2 eruption length (Vessel only) |
| Void Aegis Absorption | | 100 | Absorption granted per Void Aegis activation (the combat AI is not built yet) |
| Stagger Slump Damage Multiplier | | 1.5 | Damage multiplier only during the held slump of the stagger animation (ticks 15 to 65) |
| Spawn Invulnerability Ticks | | 40 | Invulnerable while emerging from the Vessel |
| Death Sequence Ticks | | 90 | Length of the three-beat death before removal |
| Spawn Stage 2 On Vessel Defeat | true (module root) | | Defeating the Vessel triggers the eruption and spawns the One |
| Nameless One Drop Items | empty (module root) | | Guaranteed drops when the One dies: `modid:item[:meta][*count]`, for example `thaumcraft:primordial_pearl*1` |

Both entities have only a basic melee brain (claw swipe or combo when the target is within 4 blocks, `EntityNamelessOne.onLivingUpdate`) and the animation state machine; the planned combat AI (attack selection, casts, the beam, phases, Void Aegis, restore casts, stagger interrupts) is not built; the plan is `.plans/20261116-0245-emptiness-final-boss.md` (section 15 lists its open design findings).

## Files

`additions/AdditionsModule` (item and gated entity registration), `additions/AdditionsConfigMigration` (one-time `aqtweaks_weapon.cfg` migration), `additions/ItemInfinityBlade`, `additions/InfinityBladeStats` (unit tested), `additions/AdditionsRecipeHandler`, `additions/NamelessBossRegistry`, `additions/EntityNamelessVessel`, `additions/EntityNamelessOne`, `additions/client/AdditionsClientEvents`, `additions/client/NamelessBossClient`, `additions/client/NamelessVesselModel`, `additions/client/RenderNamelessVessel`, `additions/client/LayerNamelessVesselGlow`, `additions/client/NamelessOneModel`, `additions/client/RenderNamelessOne`, `additions/client/LayerNamelessOneGlow`. Assets: `assets/aqtweaks/models/item/infinity_blade.json`, `textures/items/infinity_blade.png` and `.png.mcmeta`, `geo/nameless_vessel.geo.json`, `geo/nameless_one.geo.json`, `animations/nameless_vessel.animation.json`, `animations/nameless_one.animation.json`, `textures/entity/nameless_vessel*.png`, `textures/entity/nameless_one*.png`.

## DEVBOX

The old `scripts/contenttweaker/infinity_sword.zs` and `scripts/crafttweaker/infinity_sword_stats.zs` were deleted. The ContentTweaker model, texture and lang for `infinity_sword` were left in place (unused).

## Do not regress

- Keep stats in config, not hard-coded; keep `Enable Default Recipe` off by default.
- Keep the repair item config-driven (`none` disables it).
- Do not add rarity logic here; the rarity overhaul owns it.
- Keep all `software.bernie.geckolib3.*` imports isolated behind `Loader.isModLoaded("geckolib3")` so `Infinity Blade` still registers even if `geckolib3` is absent.

