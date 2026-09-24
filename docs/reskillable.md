# Reskillable module (1.8)

Last updated: 2026-09-21.

Config: `config/arcanaquesttweaks/aqtweaks_reskillable.cfg`. Handler registers only if `reskillable` is loaded (`CommonProxy.init`). Compile-hard CAD Reskillable **1.13.1** API; types live only in `com.apocollis.aqtweaks.reskillable`. Soft `@Mod` `after:reskillable` (not `required-after`).

Stamina Armor Mastery / Mining Efficiency **perk id lookups** stay in [stamina.md](stamina.md) (`Reflect.hasUnlockable`, `aqtweaks_stamina.cfg`). This jar **registers** those traits (same ids as the old pack CrT). Remove pack `stamina_perks.zs` or the duplicate registry will conflict.

Effortless Building placement bonuses need both `reskillable` and `effortlessbuilding`. Optional `mixins.aqtweaks.effortlessbuilding.json` (`required: false`). Parent jar: `effortlessbuilding-1.12.2-2.16`. Soft `after:effortlessbuilding`.

Magic **schools** stay in this module. They borrow existing mixin json when the parent already has one (Bewitchment, Astral, Simple Difficulty) and add `mixins.aqtweaks.botania.json` / `mixins.aqtweaks.embers.json`. There is no Botania module doc.

## Locked intent

Linear drip on every skill level (`bonus = level × k`, assumed cap **32**). Traits/perks stay the spikes. Do **not** mixin CombatRules. Do **not** gate recipes (pack Recipe Stages). Do **not** apply vanilla `REACH_DISTANCE`.

## How the parent mods work

### Reskillable 1.13.1

Skills: `reskillable:attack`, `defense`, `agility`, `building`, `mining`, `gathering`, `farming`, `magic`.

`PlayerDataHandler.get(player).getSkillInfo(skill).getLevel()`. `LevelUpEvent.Post` after a level change. Player data also exists on the client (EB place-reach preview).

### Effortless Building 2.16

`ReachHelper.getMaxReach` is the upgrade ladder (and feeds max-blocks via `ceil(maxReach^1.6)` and `getPlacementReach = maxReach / 4`). Tweaks does **not** mixin `getMaxReach` (that would also change GUI array/mirror reach and inflate blocks nonlinearly).

Tweaks injects **RETURN** on:

- `getPlacementReach` — Floor/Line/Wall/three-click **placement** ray only
- `getMaxBlocksPlacedAtOnce` — survival multi-place cap

Creative still uses EB’s creative limits (no Tweaks add).

## Design plan (hooks)

| Skill | Hook | Formula |
| --- | --- | --- |
| Attack | `generic.attackDamage` op 0 | `level × 0.125` |
| Defense | `generic.armor` op 0 | `level × 0.25` |
| Agility | `generic.movementSpeed` op 0 | `level × 0.0003125` |
| Building | EB getters above | place `floor(level × 0.125)`; blocks `floor(level × 1)` |
| Mining | `PlayerEvent.BreakSpeed` | `speed × (1 + level × 0.01)` |
| Gathering | harvest / shear / fish | `min(1.0, level × 0.00625)` chance of +1 |
| Farming | mature crop `HarvestDropsEvent` | same k as Gathering |
| Magic | `LivingHurtEvent` NORMAL + TC Heal mixin | `± min(level × 0.0125, 0.4)` (heal is + only) |

Mining is **server-authoritative**: `onBreakSpeed` returns early on `player.world.isRemote`, so the client never previews the bonus — the block just finishes at the server's rate.

Attributes restamp: `LevelUpEvent.Post`, `CacheInvalidatedEvent`, login, respawn, clone, dim change, Tweaks cfg change. Logout and world unload only invalidate the level cache. Skip `FakePlayer`. One Tweaks UUID per attribute; `setSaved(false)`. Skip remove/apply when an identical unsaved modifier is already present (re-applying still dirties the attribute and resends `SPacketEntityProperties`).

**Do not** also multiply Attack/Defense in hurt. Magic has no vanilla attribute.

### Gathering vs Farming

Farming: mature `BlockCrops`, nether wart age 3, cocoa age 2, pumpkin, melon. Not saturation.

Gathering: logs / `logWood`, gravel, leaves / `treeLeaves`, flowers, mushrooms, tall grass. Skip silk touch. Skip `ore*` OreDict and vanilla ore blocks. Extra wool after a successful **vanilla** sheep shear (next-tick if `getSheared()` flipped). Farm Animania sheep/goats skip that vanilla wool drip. Extra fish: one more stack entry on `ItemFishedEvent`.

### Magic classify

Outgoing if trueSource is the player; incoming if victim is the player. Same classify:

1. `isMagicDamage()` **or** cfg allow-prefix on `damageType`, **and**
2. type is not `thorns`, **and**
3. immediate source is not `EntityPotion` / `EntityAreaEffectCloud`, **and**
4. source is not the vanilla `DamageSource.MAGIC` singleton, **and**
5. type is not on the deny list (`wither`, `onFire`, `lava`, `hotFloor`).

Gaia bolts Tweaks recast to `causeIndirectMagicDamage` still match. Splash/lingering potion HP does not.

Stock TC foci stamp `setMagicDamage()` in the [thaumcraft module](thaumcraft.md) mixin (not a `thrown` prefix). DSS Blade Beam stamps **magic + unblockable** on `EntitySwordBeam` impact (`mixins.aqtweaks.dss.json`). Heal on living uses `heal(float)`: caster outgoing drip, then Blood Pact, then Full Font if stamped; no incoming DR on heals. Food/regen/potions are not scaled.

Outgoing classified Magic (hurt + Heal-focus + Blade Beam) is `scaleOutgoingMagic`: drip × Blood Pact × Full Font stamp (stamp then clears). Incoming Magic is drip only.

Full Font: aura full means `AuraHandler.getVis ≥ getAuraBase × 0.9` (cfg). `ItemCaster.consumeVis` amount ×1.5 when full+perk **then** Vis Thrift. Success stamps; next classified outgoing ×1.5. Not workbench (`crafting` flag). Blood Pact: unsaved `generic.maxHealth` −8 while unlocked (Stats Keeper extra hearts untouched); outgoing ×2 after drip.

`logClassify` (default **true**) INFO-logs unique `damageType` + `isMagicDamage` + source classes for player-involved hits (cap 48). Default allow prefix **`fireball`** (ghast / blaze / Lich). Instance cfg may still be empty. **Do not** add `thrown`.

## Config

| Name | Default | Live? | Meaning |
| --- | --- | --- | --- |
| Enable Reskillable Bonuses | true | yes (restamp) | Master switch |
| Attack damage / Defense armor / Agility speed per level | 0.125 / 0.25 / 0.0003125 | yes | Attribute add |
| EB place reach / max blocks per level | 0.125 / 1 | yes | Mixin add on EB getters |
| Mining break speed per level | 0.01 | yes | BreakSpeed |
| Gathering / Farming extra chance per level | 0.00625 | yes | Proc chance, capped at `min(1.0, level × k)` |
| Magic hurt per level | 0.0125 | yes | Cap 40% |
| Log damage classify | true | yes | First-pass DEVBOX log |
| Allow type prefixes | `fireball` | yes | Ghast/Lich if `isMagicDamage` is false; foci use the TC mixin |
| Deny types | wither, onFire, lava, hotFloor | yes | Never spell-like |

Instance cfg keeps old keys when Java defaults change.

Layout/enable for Tweaks-owned traits: `aqtweaks_reskillable.cfg` **Perks** (restart). Effect knobs for stamina traits: `aqtweaks_stamina.cfg`. Mining Expert harvest floor: Mining section `expertHarvestFloor` (vanilla diamond = **3**).

Register in **preInit** (`ReskillablePerkRegistry`). CAD `getTraitConfig` still writes `reskillable.cfg`; Tweaks overlays `UnlockableConfig` so Tweaks cfg wins at runtime.

| Id | Skill | Default cell | Cost | Req |
| --- | --- | --- | --- | --- |
| `aqtweaks:melee_efficiency` | attack | 2,2 | 3 | attack 16, agility 12 |
| `aqtweaks:ranged_efficiency` | attack | 2,3 | 3 | attack 16, agility 12 |
| `aqtweaks:power_attack` | attack | 2,1 | 2 | attack 12 |
| `aqtweaks:shield_efficiency` | defense | 2,2 | 2 | defense 12 |
| `aqtweaks:respite` | defense | 2,1 | 3 | defense 16, magic 16 |
| `aqtweaks:armor_mastery` | defense | 2,3 | 3 | defense 12, agility 16 |
| `aqtweaks:adrenaline` | agility | 2,1 | 3 | agility 16, defense 12 |
| `aqtweaks:evasion` | agility | 3,2 | 3 | agility 16 |
| `aqtweaks:expert_climber` | agility | 1,2 | 3 | agility 20 |
| `aqtweaks:cardio_master` | agility | 3,3 | 3 | agility 20 |
| `aqtweaks:mining_efficiency` | mining | 2,3 | 3 | mining 16 |
| `aqtweaks:mining_expert` | mining | 3,3 | 4 | mining 24 |
| `aqtweaks:precision_shot` | attack | 3,3 | 2 | attack 12, agility 16 |
| `aqtweaks:herbalist` | gathering | 2,1 | 3 | gathering 16, magic 12 |
| `aqtweaks:gathering_efficiency` | gathering | 2,3 | 2 | gathering 12 |
| `aqtweaks:herd_abundance` | gathering | 2,2 | 3 | gathering 16, farming 12 |
| `aqtweaks:water_collector` | gathering | 3,1 | 3 | gathering 16 |
| `aqtweaks:bountiful_harvest` | farming | 2,2 | 3 | farming 16 |
| `aqtweaks:rancher` | farming | 2,1 | 3 | farming 16, gathering 12 |
| `aqtweaks:iron_gut` | farming | 2,3 | 2 | farming 12 |
| `aqtweaks:drafter` | building | 2,1 | 2 | building 12 |
| `aqtweaks:glass_cutter` | building | 2,2 | 2 | building 8 |
| `aqtweaks:sculptor` | building | 2,3 | 3 | building 16 |
| `aqtweaks:transpose` | building | 3,1 | 3 | building 20, magic 20 |
| `aqtweaks:vis_thrift` | magic | 2,1 | 3 | magic 16 |
| `aqtweaks:quiet_mind` | magic | 2,2 | 3 | magic 16, defense 12 |
| `aqtweaks:full_font` | magic | 2,3 | 3 | magic 16 |
| `aqtweaks:blood_pact` | magic | 3,3 | 4 | magic 20 |
| `aqtweaks:druid` | magic | 1,2 | 3 | magic 20, not Witch/Astromancer/Artificer |
| `aqtweaks:mana_veil` | magic | 0,2 | 1 | Druid |
| `aqtweaks:living_edge` | magic | 0,3 | 1 | Druid |
| `aqtweaks:witch` | magic | 3,1 | 3 | magic 20, not the other schools |
| `aqtweaks:cold_iron_mind` | magic | 3,0 | 1 | Witch |
| `aqtweaks:stitch` | magic | 4,0 | 1 | Witch |
| `aqtweaks:astromancer` | magic | 4,1 | 3 | magic 20, not the other schools |
| `aqtweaks:astral_warmth` | magic | 4,2 | 1 | Astromancer |
| `aqtweaks:star_powered` | magic | 4,3 | 1 | Astromancer |
| `aqtweaks:artificer` | magic | 0,0 | 3 | magic 20, not the other schools |
| `aqtweaks:live_spark` | magic | 1,0 | 1 | Artificer |
| `aqtweaks:cinder_ward` | magic | 2,0 | 1 | Artificer |

CAD stock costs are stamped on the same LOWEST pass. Hillwalker is **4**. Drop Guarantee stays **4**. Elenai dodge is **2**. Safe Port is **2** at magic 12, agility 12, defense 12. Golden Osmosis is **2** at magic 8, with mining, gathering, and attack still at 6.

Precision Shot: full-draw `ItemBow` (`ArrowLooseEvent` charge ≥ 20) stamps the arrow; that hit **×2** once. Not melee.

Herbalist: always +1 on listed block namespaces. Bountiful Harvest: always +1 mature food crops; not melon/pumpkin; not Herbalist namespaces.

Gathering Efficiency: −1 forage `BreakEvent` stamina only.

Rancher: tended Animania (`handFed` or `interacted`) within 16 of the perk player extra-tick gestation, dry, growth, drink recovery, wool, hen lay, mating AI delay. No extra milk bucket.

Herd Abundance: always +1 Farm breed wool after a successful Animania sheep/goat shear; always +1 egg on empty-hand nest extract. Not milk, not clocks.

Glass Cutter: harvest of OreDict `blockGlass`/`paneGlass` (and vanilla glass/pane types) drops the block when silk is not already recovering it. Runs before the empty-drop return. No hardness/stamina change.

Iron Gut: cancels Rustic `FluidBooze.inebriate` (tipsy over-drink). Drink benefits remain.

Water Collector: SD world-drink dirty chance 0; canteen fill NORMAL/RAIN → PURIFIED; glass bottle on source water → `SDItems.purifiedWaterBottle`. Compile-hard Simple Difficulty 0.3.9.

Drafter / Sculptor / Transpose: EB `sanitize` snaps locked modes / quick replace.

Vis Thrift: +0.30 on `getTotalVisDiscount`. Quiet Mind: −round(0.35×bound) warp severity after visor.

### Magic schools (mutex)

Four schools. Unlocking one blocks the other three (`not|trait|`). School cost 3, requirement **magic 20**. The school includes parent thrift ×0.70 **and** the identity. Each school has two follow-ups cost 1. CAD resolves `trait|` while the perk is constructed, so `ReskillablePerkRegistry` restamps those holders on the LOWEST registry pass after every school exists. Leftover Magic cells: **0,1** and **1,3**.

### Wishlist perks

Drop spikes use `skillLevel × 0.02`, one extra item. The forage and farming drips still roll on their own. Silk Touch skips ore and forage extras. Tomato vines are left to Orchard; Bountiful Harvest and the farming drip skip them.

| Id | Skill | Cost | Req |
| --- | --- | --- | --- |
| `dark_vision` | mining | 2 | mining 8 |
| `spelunker` | mining | 3 | mining 16 |
| `tunnel_sense` | mining | 3 | mining 12 |
| `prospector` | mining | 2 | mining 12 |
| `motherlode` | mining | 3 | mining 16 |
| `lithomancy` | mining | 3 | mining 16, magic 12 |
| `stone_cleaver` | mining | 4 | mining 20 |
| `lumberjack` | gathering | 3 | gathering 16 |
| `reforester` | gathering | 2 | gathering 12 |
| `sifter` | gathering | 2 | gathering 8 |
| `wood_splitter` | gathering | 4 | gathering 20 |
| `orchard` | farming | 2 | farming 12 |
| `sower` | farming | 2 | farming 12 |
| `husbandry` | farming | 3 | farming 16 |
| `hearty_meal` | farming | 3 | farming 16 |
| `seed_harvester` | farming | 2 | farming 12 |
| `finisher` | attack | 3 | attack 16 |
| `aura_breaker` | attack | 3 | attack 12, magic 20 |
| `bleeding_edge` | attack | 2 | attack 12 |
| `pinning_shot` | attack | 2 | attack 12 |
| `opportunistic` | attack | 4 | attack 16, agility 12 |
| `fortify` | defense | 4 | defense 20, Shield Efficiency |
| `unyielding` | defense | 3 | defense 16 |
| `awareness` | defense | 3 | defense 16 |
| `fast_revive` | defense | 2 | defense 8 |
| `taunt` | defense | 3 | defense 16, not Low Profile |
| `low_profile` | defense | 3 | defense 16, not Taunt |
| `soft_step` | agility | 2 | agility 8 |
| `tumble` | agility | 2 | agility 12 |
| `slow_fall` | agility | 3 | agility 12 |

Dark Vision: Night Vision while combined light at the feet is ≤ 7, clear at 9. Spelunker: underground (Thaumcraft under / underDeep Y) halves those two exposure banks and pulls Simple Difficulty body temperature 4 points toward 11 (10 and 11 stay). Tunnel Sense: hostile within 10 blocks plays `block.note.pling` for that player and applies Glowing for 100 ticks; it does not fire again until that ends. Prospector: breaking stone, cobble, or stone brick marks ores in 5 blocks, cooldown 15 seconds. Motherlode: one extra on one ore stack. Lithomancy: one Rare Earth roll, Thaumcraft base × `(1 + mining × 0.0625)`, and Thaumcraft’s own roll is skipped for that break. Stone Cleaver / Wood Splitter: tool harvest level ≥ 3, no durability on stone/cobble/gravel or logs. Reforester: one sapling if the leaf drop has none; shears and silk skip it. Sifter: gravel stays, plus a flint roll; clay adds one clay ball on the same roll. Orchard: melon, pumpkin, and ripe Rustic / Farmer’s Delight / Extra Delight fruit. Sower: 3×3 seeds on farmland. Husbandry: ageable mobs within 16 grow and cool down faster. Hearty Meal: +2 hunger. Seed Harvester: one replant item. Finisher: melee ×1.5 under 25% health. Aura Breaker: Broken Magic Shield III for 5 seconds. Bleeding Edge: `lycanitesmobs:bleed` 16 seconds, melee only. Pinning Shot: physical ranged Slowness II for 40 ticks. Opportunistic: redirect Dynamic Stealth `Sight.canSee` in `entityAttackedPre` when a different living attacker hit the target in the last 60 ticks. Fortify: shield stamina 0, Resistance II after 20 still ticks while blocking. Unyielding: knockback resistance 1 while the shield is up. Awareness: cancel `StealthAttackEvent`. Fast Revive: each perk holder counts as a second helper in `Revival.tick`. Taunt ×2 and Low Profile ×0.75 on the six `THREATGEN_*` stats. Soft Step: cancel farmland trample. Tumble: fall damage ×0.5. Slow Fall: Rustic `PotionFeather` while a solid block is beside the player.

Existing Tweaks costs were retuned in the same pass. Stock traits are stamped on the LOWEST registry pass.

| School | Thrift | Identity | Follow-ups |
| --- | --- | --- | --- |
| Druid | Botania `ManaItemHandler` spend ×0.70 | Grove: generating `addMana` ×1.15 within 24 of a Druid | Mana Veil: absorb 20% of a hurt packet at 1000 mana/HP (`requestManaExact`). Living Edge: Botania damage ×1.15 |
| Witch | `MagicPower.attemptDrain` ×0.70 | Hearth: `TileEntityWitchesAltar.scan` RETURN, `gain` ×1.15 if a Witch is within 24 | Cold Iron Mind: ritual-finish warp each type `/ 2` in `WarpRitualWrapper`. Stitch: skip the second `damageItem` in `Util.attemptDamagePoppet`; shapeless poppet + `bewitchment:witches_stitching` repair (perk-gated when a player is on the container) |
| Astromancer | altar `getPassiveStarlightRequired` ×0.70 during `ActiveCraftingTask` | `craftingTickTime` ×0.80 (25% faster) | Astral Warmth: SD temp ≥ 9 after `tickUpdate`. Star Powered: open night sky, all outgoing ×1.25 + Regen I + Elenai replenishment |
| Artificer | `EmberInventoryUtil.removeEmber` ×0.70 | Foundry Pulse: stamper / mixer-bottom / melter-bottom extra `update` every 4 ticks if an Artificer is within 24 | Live Spark: `ember` / Embers damage ×1.20 **after** Magic drip. Cinder Ward: SD temp ≤ 15; fire ×0.80, or heal 20% of the fire packet if `fire_resistance` is active |

Do not extra-stamp Botania/Astral as classified Magic. Embers already `setMagicDamage()`. Schools are not a new docs module; mixins sit in the parent json when it already exists.

Power Attack: connecting medium/heavy melee on a **full** regular feather bar; **×1.5** / **×2** and **+2** extra half-feathers (Efficiency does not cut the +2). Light never procs.

Mining Expert: `PlayerEvent.HarvestCheck` client+server. Pickaxe tool class, block pickaxe or null tool, harvest ≤ floor. Does not change `Item.getHarvestLevel`.

Respite: `LivingDamageEvent` LOWEST. Lethal except `outOfWorld`. Regen II + `teastory:defence` 5s. Cooldown 60s (harmful potion, same PNG as the tree icon).

Evasion: `LivingAttackEvent` HIGH, living attacker (PvP included). Spends Elenai dodge cost (`ModConfig.common.feathers.cost` / `airborneCost`). Dodge **sound** only (no roll, no `ServerDodgeEffects.run`). Cooldown 30s potion. Tree requirement is **agility 16** only. Instance `trait|elenaidodge2:dodge` is stripped at load (Elenai has no CAD trait).

Adrenaline cooldown potion uses `unlockables/adrenaline.png` (20s).

Mining Expert: `PlayerEvent.HarvestCheck` client+server. Pickaxe tool class, block pickaxe or null tool, harvest ≤ floor. Does not change `Item.getHarvestLevel`.

Lang: `reskillable.unlock.aqtweaks.<path>` / `.desc`. Icons: `aqtweaks:textures/unlockables/<path>.png` (no leftover slash-square placeholders). Adrenaline / Evasion / Respite cooldown HUD uses the same PNG as the trait.

## Files

- `reskillable/ReskillablePerkRegistry.java`, `AqtweaksTrait.java`, `ReskillablePerkLayout.java`
- `reskillable/ReskillableModule.java` — events + Mining Expert HarvestCheck
- `reskillable/RespiteHandler.java` — lethal save
- `potion/PotionPerkCooldown.java` — Adrenaline / Evasion / Respite CD HUD
- `stamina/StaminaPerks.java` — spend reductions, Adrenaline, Evasion, Power Attack
- `reskillable/ReskillableBonuses.java` — levels, classify, EB add
- `reskillable/EffortlessBuildingHooks.java` — mixin bridge (no Reskillable imports)
- `animania/AnimaniaModule.java`, `AnimaniaFarmClocks.java`, `AnimaniaFarmProducts.java`
- `thaumcraft/ThaumcraftPerkHooks.java`
- `simpledifficulty/SimpleDifficultyModule.java`
- `mixin/effortlessbuilding/MixinReachHelper.java`, `MixinModeSettingsManager.java`, `MixinModifierSettingsManager.java`
- `mixin/thaumcraft/MixinCasterManager.java`, `MixinWarpEvents.java`, `MixinItemCaster.java`
- `mixin/dss/MixinEntitySwordBeam.java`
- `mixin/simpledifficulty/MixinThirstUtilInternal.java`, `MixinItemCanteen.java`, `MixinItemCanteenUse.java`, `MixinTemperatureCapability.java`
- `mixin/rustic/MixinFluidBooze.java`
- `mixin/animania/MixinGenericAIMate.java`, `MixinBlockNest.java`
- `reskillable/MagicSchoolPresence.java`, `MagicSchoolEffects.java`, `MagicSchoolBotania.java`, `MagicSchoolFoundry.java`
- `reskillable/RecipeStitchPoppet.java`, `StitchRecipeEvents.java`
- `thaumcraft/WarpRitualWrapper.java` — Cold Iron Mind halves wrapped ritual warp
- `mixin/botania/MixinManaItemHandler.java`, `MixinSubTileGenerating.java` — `mixins.aqtweaks.botania.json`
- `mixin/bewitchment/MixinMagicPower.java`, `MixinTileEntityWitchesAltar.java`, `MixinUtilPoppet.java`
- `mixin/astral/MixinActiveCraftingTask.java`, `MixinAbstractAltarRecipe.java`
- `mixin/embers/MixinEmberInventoryUtil.java`, `MixinTileEntityStamper.java`, `MixinTileEntityMixerBottom.java`, `MixinTileEntityFurnaceBottom.java` — `mixins.aqtweaks.embers.json`
- `mixin/simpledifficulty/MixinTemperatureCapability.java`
- `mixins.aqtweaks.effortlessbuilding.json`

## Do not regress

- Stamina `aqtweaks_stamina.cfg` Reskillable perk section and `Reflect.hasUnlockable`
- Duplicate registry if pack CrT still registers Armor Mastery / Mining Efficiency
- No Reskillable `import` from `StaminaModule` / `Reflect` / config class body
- No vanilla `REACH_DISTANCE`; do not mixin EB `getMaxReach`
- Attack/Defense not extra-multiplied in `LivingHurtEvent` except Power Attack’s tagged multiplier
- Comfort still only cancels rest; Gaia bolt retype still runs before this NORMAL hurt
- Evasion must not call Elenai `ServerDodgeEffects.run` or post `SpendFeatherEvent`
- Comfort still only cancels rest; Gaia bolt retype still runs before this NORMAL hurt
- `--release 21`

## Verify

- Reskillable jar removed: Tweaks boots; stamina perks unchanged; EB mixin json skipped or hooks no-op
- Attack 16 → +2.0 `attackDamage`; 32 → +4.0; login/respawn keep it; sword tooltip/F3
- Defense: armor bar up, max health unchanged
- Agility: move speed up; Elenai dodge unchanged
- Building: EB placement ray +2 / +4 at 16 / 32; max blocks +16 / +32 survival; creative unchanged; melee reach unchanged; EB jar absent → no mixin crash
- Mining: faster break; server-authoritative, so the client shows no bonus preview; stamina break cost unchanged (perk still does)
- Farming: extra wheat on mature crop; not on stone or ore
- Gathering: extra log/leaf/flint, extra wool, extra fish; **not** ore; silk touch no extra
- Mining Expert: wood pick + perk drops diamond ore/obsidian; tooltip stars unchanged; fist does not
- Magic schools: mutex in GUI; Druid flowers +15% mana in 24; Witch drain cheaper; Astromancer altar faster; Artificer stamper extra ticks; Cinder/Astral temp clamps with SD; Stitch repair shapeless

## Out of scope unless asked

- Extra HP; attack speed; bow damage; general % DR; potion duration
- Vanilla reach; Agility stamina/dodge; farming saturation; gathering extra **ore** (pack Mining perk)
- Recipe/item gating (Recipe Stages / Game Stages / CrT)
- Shipping Universal Tweaks Armor Curve in this jar
