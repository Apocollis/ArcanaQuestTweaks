# Reskillable module (1.8)

Last updated: 2026-09-20.

Config: `config/arcanaquesttweaks/aqtweaks_reskillable.cfg`. Handler registers only if `reskillable` is loaded (`CommonProxy.init`). Compile-hard CAD Reskillable **1.13.1** API; types live only in `com.apocollis.aqtweaks.reskillable`. Soft `@Mod` `after:reskillable` (not `required-after`).

Stamina Armor Mastery / Mining Efficiency **perk id lookups** stay in [stamina.md](stamina.md) (`Reflect.hasUnlockable`, `aqtweaks_stamina.cfg`). This jar **registers** those traits (same ids as the old pack CrT). Remove pack `stamina_perks.zs` or the duplicate registry will conflict.

Effortless Building placement bonuses need both `reskillable` and `effortlessbuilding`. Optional `mixins.aqtweaks.effortlessbuilding.json` (`required: false`). Parent jar: `effortlessbuilding-1.12.2-2.16`. Soft `after:effortlessbuilding`.

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
| `aqtweaks:melee_efficiency` | attack | 2,2 | 4 | attack 16, agility 12 |
| `aqtweaks:ranged_efficiency` | attack | 2,3 | 4 | attack 16, agility 12 |
| `aqtweaks:power_attack` | attack | 2,1 | 3 | attack 12 |
| `aqtweaks:shield_efficiency` | defense | 2,2 | 4 | defense 16 |
| `aqtweaks:respite` | defense | 2,1 | 4 | defense 16, magic 16 |
| `aqtweaks:armor_mastery` | defense | 2,3 | 4 | defense 8, agility 16 |
| `aqtweaks:adrenaline` | agility | 2,1 | 4 | agility 16, defense 12 |
| `aqtweaks:evasion` | agility | 3,2 | 3 | agility 16 |
| `aqtweaks:expert_climber` | agility | 1,2 | 4 | agility 20 |
| `aqtweaks:cardio_master` | agility | 3,3 | 4 | agility 20 |
| `aqtweaks:mining_efficiency` | mining | 2,3 | 6 | mining 20 |
| `aqtweaks:mining_expert` | mining | 3,3 | 4 | mining 24 |
| `aqtweaks:precision_shot` | attack | 3,3 | 3 | attack 12, agility 16 |
| `aqtweaks:herbalist` | gathering | 2,1 | 4 | gathering 16, magic 12 |
| `aqtweaks:gathering_efficiency` | gathering | 2,3 | 4 | gathering 12 |
| `aqtweaks:herd_abundance` | gathering | 2,2 | 4 | gathering 16, farming 12 |
| `aqtweaks:water_collector` | gathering | 3,1 | 4 | gathering 16 |
| `aqtweaks:bountiful_harvest` | farming | 2,2 | 4 | farming 16 |
| `aqtweaks:rancher` | farming | 2,1 | 4 | farming 16, gathering 12 |
| `aqtweaks:iron_gut` | farming | 2,3 | 3 | farming 16 |
| `aqtweaks:drafter` | building | 2,1 | 3 | building 12 |
| `aqtweaks:glass_cutter` | building | 2,2 | 2 | building 12 |
| `aqtweaks:sculptor` | building | 2,3 | 3 | building 20 |
| `aqtweaks:transpose` | building | 3,1 | 3 | building 24, magic 30 |
| `aqtweaks:vis_thrift` | magic | 2,1 | 4 | magic 20 |
| `aqtweaks:quiet_mind` | magic | 2,2 | 4 | magic 16, defense 12 |
| `aqtweaks:full_font` | magic | 2,3 | 4 | magic 16 |
| `aqtweaks:blood_pact` | magic | 3,3 | 4 | magic 20 |

CAD `reskillable:hillwalker` cost is stamped **6** at register LOWEST. CAD `reskillable:drop_guarantee` cost is stamped **4**.

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

Power Attack: connecting medium/heavy melee on a **full** regular feather bar; **×1.5** / **×2** and **+2** extra half-feathers (Efficiency does not cut the +2). Light never procs.

Mining Expert: `PlayerEvent.HarvestCheck` client+server. Pickaxe tool class, block pickaxe or null tool, harvest ≤ floor. Does not change `Item.getHarvestLevel`.

Respite: `LivingDamageEvent` LOWEST. Lethal except `outOfWorld`. Regen II + `teastory:defence` 5s. Cooldown 60s (harmful potion, same PNG as the tree icon).

Evasion: `LivingAttackEvent` HIGH, living attacker (PvP included). Spends Elenai dodge cost (`ModConfig.common.feathers.cost` / `airborneCost`). Dodge **sound** only (no roll, no `ServerDodgeEffects.run`). Cooldown 30s potion. Tree requirement is **agility 16** only. Instance `trait|elenaidodge2:dodge` is stripped at load (Elenai has no CAD trait).

Adrenaline cooldown potion uses `unlockables/adrenaline.png` (20s).

Mining Expert: `PlayerEvent.HarvestCheck` client+server. Pickaxe tool class, block pickaxe or null tool, harvest ≤ floor. Does not change `Item.getHarvestLevel`.

Lang: `reskillable.unlock.aqtweaks.<path>` / `.desc`. Icons: `aqtweaks:textures/unlockables/<path>.png`.

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
- `mixin/simpledifficulty/MixinThirstUtilInternal.java`, `MixinItemCanteen.java`, `MixinItemCanteenUse.java`
- `mixin/rustic/MixinFluidBooze.java`
- `mixin/animania/MixinGenericAIMate.java`, `MixinBlockNest.java`
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

## Out of scope unless asked

- Extra HP; attack speed; bow damage; general % DR; potion duration
- Vanilla reach; Agility stamina/dodge; farming saturation; gathering extra **ore** (pack Mining perk)
- Recipe/item gating (Recipe Stages / Game Stages / CrT)
- Shipping Universal Tweaks Armor Curve in this jar
