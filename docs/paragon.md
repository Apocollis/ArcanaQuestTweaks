# Tartarite Paragon absorption

Metallurgy's Tartarite tools and armor can take a Paragon: four metal blocks around the item copy a Metallurgy metal's properties onto it. Tweaks adds the same for metals from other mods in the pack, so a Tartarite item can absorb Aurorian Steel, Fiery, Octine, Knightmetal, Zanite or Gravitite. Magic-mod metals (Botania, Thaumcraft, Astral, Kami, Crimson) are out by design, as are Netherite (not enabled), Mystical World and Bewitchment.

## Locked intent

* Only the **nine** Metallurgy items take a trait: `metallurgy:tartarite_sword`, `pickaxe`, `axe`, `shovel`, `hoe`, `helmet`, `chestplate`, `leggings`, `boots`. Spartan Weaponry Tartarite weapons are not generated in this pack (`Spartan Weaponry Integration=false` in `config/metallurgy_reforged/general.cfg`) and Metallurgy gives them no Paragon in any case.
* **One Paragon per item.** An item with Metallurgy's `paragon` tag takes none of ours. An item with one of ours can be swapped to another trait (the old trait's data is removed) but not re-applied with the same one.
* **Per-piece and set effects.** A trait's per-item effect (burn on hit, wear scaling) works on that item alone. A perk that is a **full-set** effect in its home mod (Gravitite's high jump) works only when **all four worn armor pieces carry the same trait** (16 metal blocks in total). A mixed set gets only the per-piece parts.
* Properties that belong to a particular item, not to the metal (right-click abilities of boss weapons, Betweenlands corrosion, Embers clockwork), are not absorbable. The material survey is in `.plans/archive/20261108-1335-expand-tartarite-paragon-absorption.md` (sections 10 to 13).

## How Metallurgy's Paragon works (checked in `Metallurgy-4-Reforged-1.4.1.jar`)

* Nine data recipes of type `metallurgy:tartarite_paragon_shaped`, pattern `" M " / "MIM" / " M "`, `I` the Tartarite item and `M` a Metallurgy metal block. The Java side writes the NBT key `paragon`, copies the donor metal's attribute modifiers and, from `TartariteEffect.inventoryTick`, runs the donor's effect.
* It looks the donor up in its own metal map, so it cannot take an outside metal. Tweaks therefore keeps its own key, `arcana_paragon`, and runs its own traits.

## What Tweaks adds

* **Recipe type `aqtweaks:paragon_shaped`** (`ParagonRecipeFactory`, registered in `assets/aqtweaks/recipes/_factories.json`). A recipe file names a trait and an item: `{"type": "aqtweaks:paragon_shaped", "trait": "fiery", "item": "metallurgy:tartarite_sword", "conditions": [...]}`. `ParagonRecipe` extends the shaped ore recipe, so JEI shows the pattern and the result carrying the trait. One file per trait and item that makes sense (36 in `assets/aqtweaks/recipes/`); delete a file to remove that combination. The block ring is an ore-dictionary ingredient (`blockAurorianSteel`, `blockFiery`, `blockOctine`, `blockKnightmetal`, `blockZanite`, `blockEnchantedGravitite`), so chiseled or alternate blocks of the same metal also work.
* **`arcana_paragon`** on the stack holds the trait id. `ParagonRegistry` is the only reader: it looks at an item only if it is one of the nine, and at its tag once.
* **`ParagonEventHandler`** hands block break, mining speed, melee hit, armor damage, right click on a block, hoe use, jump, a 20-tick wearer sync and a post-hit launch queue to the trait of the stack. `ParagonTrait` is the base class; a trait overrides only what it needs.
* **Metallurgy wins a tie.** If both recipes could ever match, `ParagonEventHandler.onCrafted` removes our trait from a result that carries Metallurgy's `paragon`.
* **Tooltip** (`client/paragon/ParagonTooltipHandler`): a header (`Paragon: Fiery`), one line on the effect, the set-perk line on armor, and the trait's own lines (Aurorian Steel level, Zanite wear tier).

## Traits

| Trait id | Metal block | Mod | Applies to | Effect and source |
| --- | --- | --- | --- | --- |
| `aurorian_steel` | `blockAurorianSteel` | The Aurorian | all nine | The item levels up with use and gains enchantment levels. Delegates to The Aurorian's own `AurorianSteelHelper.handleAurorianSteelDurability` (compile-hard). Triggers: block break (tools), melee hit (sword, axe), damage taken (armor), hoe use. Needs the item to carry an enchantment, as in The Aurorian. |
| `fiery` | `blockFiery` | Twilight Forest | sword, pickaxe, axe, shovel | A hit sets the target on fire (`Fiery Burn Seconds`, default 15, the Fiery pickaxe's value) with flame particles. Not applied to fire-immune targets. |
| `octine` | `blockOctine` | The Betweenlands | sword, pickaxe, axe, shovel | A hit sets the target on fire for `Octine Burn Seconds` (5) with `Octine Burn Chance` (0.25; The Betweenlands raises it to 0.5 only with a Crimson circle gem, which Tartarite does not carry). |
| `knightmetal` | `blockKnightmetal` | Twilight Forest | sword, pickaxe, axe | Sword and pickaxe: `Knightmetal Bonus Damage` (2) extra against an armored target. Axe: the same against an unarmored one. This is Twilight Forest's own rule (`ItemTFKnightlySword.onDamage`). |
| `zanite` | `blockZanite` | The Aether | sword, pickaxe, axe, shovel, four armor pieces | Grows stronger as it wears down, in five tiers whose thresholds are the Aether's (250-use) steps as shares of the item's durability: 0 at 96 percent left or more, 1 at 80 to 96, 2 at 44 to 80, 3 at 20 to 44, 4 below 20. Tools mine faster (x1, x1, x1.5, x2, x3), the sword adds 0 to 4 damage, each armor piece adds `Zanite Armor Bonus Per Tier` x tier armor points through an attribute modifier refreshed every second. The tool and sword numbers follow Aether's values; the armor numbers are an approximation (the Aether uses a damage-reduction table). |
| `gravitite` | `blockEnchantedGravitite` | The Aether | sword, pickaxe, axe, shovel, four armor pieces | Sword: pushes the target up by `Gravitite Launch Speed` (1) after the hit's knockback. Pickaxe, axe, shovel: a right click on a block the tool is effective on, with air above, lifts it as an Aether floating block (costs `Gravitite Float Durability`, 4). A shovel loses its path-making while it carries the trait. Full set of four: a jump rises at `Gravitite Set Jump Speed` (1; a plain jump is about 0.42). Fall damage is not negated, as in the Aether. |

**Left out on purpose:** Phoenix (Lost Aether Content) has no metal block or ingot (its gear is found loot), Netherite is not enabled, Amedian tools are plain stat tools, Holystone, Valkyrie and Skyroot add marginal or no behavior, Embers Dawnstone's only property is a knockback-immune bauble. Stat-only metals need no trait: Metallurgy's Paragon already copies attribute modifiers, and ours does not duplicate that.

## Files

* `paragon/TartariteKind`, `ParagonRules`, `WearTier` (pure logic, tested), `ParagonTrait`, `ParagonRegistry`, `ParagonRecipe`, `ParagonRecipeFactory`, `ParagonEventHandler`, `ParagonModule`, and the traits `AurorianSteelParagonTrait`, `FieryParagonTrait`, `OctineParagonTrait`, `KnightmetalParagonTrait`, `ZaniteParagonTrait`, `GravititeParagonTrait`.
* `client/paragon/ParagonTooltipHandler`.
* `assets/aqtweaks/recipes/_factories.json` and `paragon_<trait>_<item>.json`; lang keys `aqtweaks.paragon.*`.
* Wiring: `CommonProxy.preInit` (`ParagonModule.preInit`, traits register before recipes load) and `init` (`ParagonModule.init`); `ClientProxy` (tooltip handler when `metallurgy` is loaded). The `@Mod` dependency string gained `after:metallurgy;after:theaurorian;after:thebetweenlands`.
* Tests: `ParagonLogicTest`.
* Compile-hard parent types: only `AurorianSteelHelper` (`theaurorian`) in `AurorianSteelParagonTrait` and `EntityFloatingBlock` (`aether_legacy`) in `GravititeParagonTrait`; both jars are in `libs/`. Every other trait uses vanilla types and the metal's ore-dictionary name, so `thebetweenlands`, `twilightforest` and Metallurgy are not compile dependencies. A trait registers, and its recipes load, only when its mod (and `metallurgy`) is present.

## Live config (`config/arcanaquesttweaks/aqtweaks_paragon.cfg`)

| Category | Key | Default | Meaning |
| --- | --- | --- | --- |
| General | `Enable Paragon Absorption` | true | Master switch for recipes and effects. Items keep their stored trait. |
| General | `Disabled Traits` | empty | Trait ids to switch off (recipe and effect): `aurorian_steel`, `fiery`, `octine`, `knightmetal`, `zanite`, `gravitite`. |
| Traits | `Fiery Burn Seconds` | 15 | Burn time of a Fiery hit. |
| Traits | `Octine Burn Chance` | 0.25 | Chance of an Octine burn. |
| Traits | `Octine Burn Seconds` | 5 | Burn time of an Octine hit. |
| Traits | `Knightmetal Bonus Damage` | 2.0 | Extra damage against the matching target. |
| Traits | `Zanite Armor Bonus Per Tier` | 0.5 | Armor points per tier per piece. |
| Traits | `Gravitite Launch Speed` | 1.0 | Sword launch speed. |
| Traits | `Gravitite Set Jump Speed` | 1.0 | Full-set jump speed. |
| Traits | `Gravitite Float Durability` | 4 | Durability lost per floated block. |

## Do not regress

* Only the nine items. Do not add a `spartan*` or name-based match.
* Do not read an unrelated stack's tag in a hot event; `ParagonRegistry.kindOf` returns at once for any other item.
* Do not copy the donor item's class into the trait (no cast to `ItemZaniteTool` and the like): the Tartarite stack is not an instance of it.
* Do not put parent types in `ParagonModule` or the proxies; only trait classes import them, and they are constructed behind `Loader.isModLoaded`.
* Keep the recipe shape and the `arcana_paragon` key; saved items depend on them.

## Verify

See [verification.md](verification.md) (Tartarite Paragon). Run `python scripts/doc_audit.py`: expect `OK 0 hard findings`.
