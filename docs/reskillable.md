# Reskillable module (1.9)

Last updated: 2026-09-21.

Config: `config/arcanaquesttweaks/aqtweaks_reskillable.cfg`. Handler registers only if `reskillable` is loaded (`CommonProxy.init`). Compile-hard CAD Reskillable **1.13.1** API; types live only in `com.apocollis.aqtweaks.reskillable`. Soft `@Mod` `after:reskillable` (not `required-after`).

Stamina Armor Mastery / Mining Efficiency **perk id lookups** stay in [stamina.md](stamina.md) (`PerkAccess.has`, `aqtweaks_stamina.cfg`). This jar **registers** those traits (same ids as the old pack CrT). Remove pack `stamina_perks.zs` or the duplicate registry will conflict.

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
| Building | `generic.reachDistance` op 0 (`UUID_REACH`, not saved) | `min(Vanilla Reach Max, level × Vanilla Reach Per Level)` (0.0625 → +1 at 16, +2 at 32; max 3). Block place / break / use only; attack and mob range unchanged. Works in EB Normal mode, which is plain vanilla placement |
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

Stock TC foci stamp `setMagicDamage()` in the [thaumcraft module](thaumcraft.md) mixin (not a `thrown` prefix). DSS Blade Beam stamps **magic + unblockable** on `EntitySwordBeam` impact (`mixins.aqtweaks.dss.json`). Heal on living uses `heal(float)`: caster outgoing drip, then Blood Pact, then Full Font if stamped, then Benevolent ×2 when the target is another living entity. A self-heal skips Benevolent. No incoming DR on heals. Food/regen/potions are not scaled.

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
| `aqtweaks:weapon_tempo` | attack | 3,2 | 4 | attack 24, trait Melee Efficiency |
| `aqtweaks:ranged_efficiency` | attack | 2,3 | 3 | attack 16, agility 12 |
| `aqtweaks:power_attack` | attack | 2,1 | 2 | attack 12 |
| `aqtweaks:shield_efficiency` | defense | 2,2 | 2 | defense 12 |
| `aqtweaks:respite` | defense | 2,1 | 3 | defense 16, magic 16 |
| `aqtweaks:armor_mastery` | defense | 2,3 | 3 | defense 12, agility 16 |
| `aqtweaks:adrenaline` | agility | 2,1 | 3 | agility 16, defense 12 |
| `aqtweaks:evasion` | agility | 3,2 | 3 | agility 16 |
| `aqtweaks:expert_climber` | agility | 1,2 | 3 | agility 20 |
| `aqtweaks:marathon_runner` | agility | 3,3 | 3 | agility 16 |
| `aqtweaks:cardio_master` | agility | 4,3 | 4 | agility 24, `trait|aqtweaks:marathon_runner` |
| `aqtweaks:mining_efficiency` | mining | 2,3 | 3 | mining 16 |
| `aqtweaks:mining_expert` | mining | 3,3 | 4 | mining 24 |
| `aqtweaks:precision_shot` | attack | 3,3 | 2 | attack 12, agility 16 |
| `aqtweaks:herbalist` | gathering | 2,1 | 3 | gathering 16, magic 12 |
| `aqtweaks:gathering_efficiency` | gathering | 2,3 | 2 | gathering 12 |
| `aqtweaks:herd_abundance` | gathering | 2,2 | 3 | gathering 16, farming 12 |
| `aqtweaks:water_collector` | gathering | 3,1 | 3 | gathering 16 |
| `aqtweaks:heartwood` | gathering | 3,2 | 3 | gathering 16 |
| `aqtweaks:forager` | farming | 1,1 | 1 | farming 4 |
| `aqtweaks:bountiful_harvest` | farming | 2,2 | 3 | farming 16 |
| `aqtweaks:rancher` | farming | 2,1 | 3 | farming 16, gathering 12 |
| `aqtweaks:iron_gut` | farming | 2,3 | 2 | farming 12 |
| `aqtweaks:drafter` | building | 2,1 | 2 | building 12 |
| `aqtweaks:glass_cutter` | building | 2,2 | 2 | building 8 |
| `aqtweaks:sculptor` | building | 2,3 | 3 | building 16 |
| `aqtweaks:reach1` | building | 0,1 | 2 | building 10 (EB tier 1: about 7 blocks in play) |
| `aqtweaks:reach2` | building | 0,2 | 3 | building 20, trait reach1 (EB tier 2: about 15 blocks) |
| `aqtweaks:reach3` | building | 0,3 | 4 | building 28, trait reach2 (EB tier 3: about 30 blocks) |
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
| `aqtweaks:riposte` | defense | 1,1 | 0 (class perk) | not buyable |
| `aqtweaks:steady_draw` | attack | 4,2 | 0 (class perk) | not buyable |
| `aqtweaks:fleetfoot` | agility | 2,3 | 0 (class perk) | not buyable |
| `aqtweaks:vein_rhythm` | mining | 2,2 | 0 (class perk) | not buyable |
| `aqtweaks:studious` | magic | 1,3 | 0 (class perk) | not buyable |
| `aqtweaks:hearthkeeper` | building | 1,1 | 0 (class perk) | not buyable |

CAD stock costs are stamped on the same LOWEST pass. Hillwalker is **4**. Drop Guarantee stays **4**. Elenai dodge is **2**. Safe Port is **2** at magic 12, agility 12, defense 12. Golden Osmosis is **2** at magic 8, with mining, gathering, and attack still at 6.

Precision Shot: full-draw `ItemBow` (`ArrowLooseEvent` charge ≥ 20) stamps the arrow; that hit **×2** once. Not melee.

Herbalist: always +1 on listed block namespaces. Bountiful Harvest: always +1 mature food crops; not melon/pumpkin; not Herbalist namespaces.

Mining Efficiency: non-forage blocks bill stamina every `Mining Efficiency Interval Ticks` (20) instead of every 15, 25% less.

Gathering Efficiency: forage blocks (logs, leaves, gravel, mushrooms) bill stamina every `Gathering Efficiency Interval Ticks` (20) instead of every 15, 25% less (hold billing, see [stamina.md](stamina.md)).

Forager: plant blocks (`ForagerBlocks.isPlant`: bushes, plantables, plant or vine material, grown pumpkins and melons, `Forager Free Blocks`) cost no stamina; without it a hardness 0 block costs the flat `Plant Break Cost` (1). Heartwood: the extra-log roll that Lumberjack had (gathering x 0.02 per log). Lumberjack: `LumberjackSpeed` multiplies the log break speed at LOWEST priority on both sides (the client decides when a dig is finished; Tree Chopper sets original speed divided by the log count, so a tree digs in half the time).


Rancher: tended Animania (`handFed` or `interacted`) within 16 of the perk player get one extra clock step per living tick, applied as 20 steps once per second. Gestation stops at 1 so Animania can birth on its own step to 0. Dry stops at -1. Wool and hen lay stop at 0. Age and drink recovery have no Tweaks floor. Mating AI delay is unchanged. No extra milk bucket.

Herd Abundance: always +1 Farm breed wool after a successful Animania sheep/goat shear; always +1 egg on empty-hand nest extract. Not milk, not clocks.

Glass Cutter: harvest of OreDict `blockGlass`/`paneGlass` (and vanilla glass/pane types) drops the block when silk is not already recovering it. Runs before the empty-drop return. No hardness/stamina change.

Iron Gut: cancels Rustic `FluidBooze.inebriate` (tipsy over-drink). Drink benefits remain.

Water Collector: SD world-drink dirty chance 0; canteen fill NORMAL/RAIN → PURIFIED; glass bottle on source water → `SDItems.purifiedWaterBottle`. Compile-hard Simple Difficulty 0.3.9.

`SimpleDifficultyModule` also runs the stamina temperature tick (gold-feather clear, `MaxFeathersEvent` cap, zero-max thermia kill, hypothermia slowness, hyperthermia thirst drip). Water Collector behavior is unchanged. Algorithm: [stamina.md](stamina.md).

Drafter / Sculptor / Transpose: EB `sanitize` snaps locked modes / quick replace.

Reach I / II / III: `MixinReachHelper` injects `getMaxReach` RETURN into `EffortlessBuildingHooks.maxReach`. With `Disable EB Reach Upgrade Items` (default true) the result is the highest owned perk tier (`BuildConfig.reach.maxReachLevel1/2/3`, default 20/50/100), else `maxReachLevel0`; the stored item upgrade level is ignored; creative and `enableReachUpgrades=false` are untouched. The three `effortlessbuilding:reach_upgrade*` items are removed from their creative tab in `CommonProxy.postInit` (so they vanish from creative and JEI), their recipes are skipped (`RecipeJsonSkip`) and `ReskillableModule.onReachItemUse` cancels right-click on the items. Tested in play (2026-10-04): tiers 1 to 3 give roughly 7, 15 and 30 blocks, well under the 20/50/100 config values, because EB's placement ray is further limited in practice; descriptions state the measured ranges. Icons are the EB item textures (`textures/unlockables/reach1-3.png`). `getPlacementReach` and max blocks derive from `getMaxReach`, so they follow the tier.

Vis Thrift: +0.30 on `getTotalVisDiscount`. Quiet Mind: −round(0.35×bound) warp severity after visor.

### Magic schools (mutex)

Four schools. Unlocking one blocks the other three (`not|trait|`). School cost 3, requirement **magic 20**. The school includes parent thrift ×0.70 **and** the identity. Each school has two follow-ups cost 1. CAD resolves `trait|` while the perk is constructed, so `ReskillablePerkRegistry` restamps those holders on the LOWEST registry pass after every school exists. Leftover Magic cell: **1,3**.

### Wishlist perks

Drop spikes use `skillLevel × 0.02`, one extra item. The forage and farming drips still roll on their own. Silk Touch skips ore and forage extras. Tomato vines are left to Orchard; Bountiful Harvest and the farming drip skip them.

| Id | Skill | Cost | Req |
| --- | --- | --- | --- |
| `benevolent` | magic | 3 | magic 16 |
| `dark_vision` | mining | 2 | mining 8 |
| `spelunker` | mining | 3 | mining 16 |
| `tunnel_sense` | mining | 3 | mining 12 |
| `prospector` | mining | 2 | mining 12 |
| `motherlode` | mining | 3 | mining 16 |
| `lithomancy` | mining | 3 | mining 16, magic 12 |
| `stone_cleaver` | mining | 4 | mining 20 |
| `lumberjack` | gathering | 3 | gathering 16 (logs break `Lumberjack Speed Multiplier` (2.0) times faster, both sides, also Tree Chopper trees; the extra-log roll moved to Heartwood on 2026-11-20) |
| `heartwood` | gathering | 3 | gathering 16 (+1 log, gathering x `Heartwood Chance Per Level` (0.02) per log; also on Tree Chopper trees) |
| `reforester` | gathering | 2 | gathering 12 (sapling chance per leaf = gathering x `Reforester Chance Per Level`, default 0.03; `Reforester Guaranteed` restores one per leaf; `Reforester Max Per Tree` default 64) |
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

Dark Vision: client lightmap mixes at 0.4 wherever the perk is on, easing over 5 ticks. A Night Vision potion stays at full strength and skips this mix. Dynamic Stealth `hasNightvision` is true for that player, so the night-vision sight bonus applies. Benevolent: Heal focus on any living target other than the caster is ×2 after the drip, Blood Pact, and Full Font. Spelunker: underground (Thaumcraft under / underDeep Y) halves those two exposure banks and pulls Simple Difficulty body temperature 4 points toward 11 (10 and 11 stay). Tunnel Sense: hostile within 10 blocks plays `block.note.pling` for that player and applies Glowing for 100 ticks; it does not fire again until that ends. Prospector: a Prospectus pick right-click uses Tweaks base chances (wood 10 through steel 75) and, with the perk, adds 25 (cap 100) and outlines the ore blocks that click counted for 7 seconds. No cooldown. Invar keeps the Prospectus accuracy. Motherlode: one extra on one ore stack. Lithomancy: one Rare Earth roll, Thaumcraft base × `(1 + mining × 0.0625)`, and Thaumcraft’s own roll is skipped for that break. Stone Cleaver / Wood Splitter: tool harvest level ≥ 3, no durability on stone/cobble/gravel or logs. Reforester: one sapling if the leaf drop has none; shears and silk skip it. Sifter: gravel stays, plus a flint roll; clay adds one clay ball on the same roll. Orchard: melon, pumpkin, and ripe Rustic / Farmer’s Delight / Extra Delight fruit. Sower: 3×3 seeds on farmland. Husbandry: ageable mobs within 16 grow and cool down faster. Hearty Meal: +2 hunger. Seed Harvester: one replant item. Finisher: melee ×1.5 under 25% health. Aura Breaker: Broken Magic Shield III for 5 seconds. Bleeding Edge: `lycanitesmobs:bleed` 16 seconds, melee only. Pinning Shot: physical ranged Slowness II for 40 ticks. Opportunistic: redirect Dynamic Stealth `Sight.canSee` in `entityAttackedPre` when a different living attacker hit the target in the last 60 ticks. Fortify: shield stamina 0, Resistance II after 20 still ticks while blocking. Unyielding: knockback resistance 1 while the shield is up. Awareness: cancel `StealthAttackEvent`. Fast Revive: each perk holder counts as a second helper in `Revival.tick`. Taunt ×2 and Low Profile ×0.75 on the six `THREATGEN_*` stats. Soft Step: cancel farmland trample. Tumble: fall damage ×0.5. Slow Fall: Rustic `PotionFeather` while a solid block is beside the player.

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

## Perk toggling

Some perks can be switched on and off by the player. Left-click (no Shift) an **unlocked** toggleable perk in Reskillable's skill screen (`GuiSkillInfo`): the perk stops working (a dark tile with "OFF", and the tooltip says Disabled); click again to switch it back on. Shift+Left-click refund and Right-click back are unchanged. The perks stay unlocked, so Reskillable's own `trait|` prerequisites still count them. Refunding a perk resets it to on. There is no command or keybind.

- **Which perks:** `Toggleable Perks` in `aqtweaks_reskillable.cfg` (General). Default: Dark Vision and Tunnel Sense (Mining), Power Attack (Attack), Respite, Low Profile and Taunt (Defense), Full Font (Magic), Sower (Farming). Their descriptions end with "Can be toggled on and off." (static lang text: edit it too if you change the list). Only `aqtweaks:` perks whose effect Tweaks implements through `PerkAccess` work; other ids are ignored. Perks Reskillable implements itself (the `stamp(...)` ones) cannot be toggled.
- **Enforcement:** `PerkAccess.has` (and so `PerkAccess.on` and `StaminaPerks.unlocked`) returns false for a toggleable perk the player switched off. `PerkAccess.unlocked` ignores the toggle (used by the toggle packet and the unlock check).
- **State:** `PerkToggleState` stores the disabled ids in the player's persisted NBT (`AQTDisabledPerks`, survives death, relog and dimension change). The client keeps a mirror of the **local player's** list (`PacketSyncPerkToggles`, cleared on disconnect); other players read as enabled on your client. `Dark Vision` is client-side, which is why the mirror exists.
- **Packets:** 21 `PacketTogglePerk` (server; validated: toggleable, unlocked, not spectator), 22 `PacketSyncPerkToggles` (client; sent on login, respawn, dimension change, after a toggle, after a refund clears the flag).
- **Code:** `reskillable/ToggleablePerks`, `PerkToggleState`, `PerkToggleEvents` (also clears the flag on `LockUnlockableEvent.Post` and re-applies attribute perks after a toggle), `reskillable/client/PerkToggleClient`, `mixin/reskillable/MixinGuiSkillInfo` (`mixins.aqtweaks.reskillable.json`: click hook on SRG `func_73864_a`, icon overlay by redirecting the icon draw in `drawUnlockable`, status line by redirecting `RenderHelper.renderTooltip` in `makeUnlockableTooltip`).
- **Verify:** unlock Tunnel Sense, left-click it in the Mining screen: tile shows OFF, tooltip says Disabled, hostiles no longer ping; relog and die: still off; Shift+Left-click refund and re-buy: on again; a non-toggleable perk ignores the click.

## Starter class perks

Six class-exclusive perks, one per starter class (`.plans/archive/20261103-1255-starter-class-selection-gui.md` sections 12.9 and 13). A class grants its perk with `PlayerSkillInfo.unlock`, which does not check requirements.

**Exclusivity (`ClassPerkSupport`).** `ClassPerkSupport.register(registry, path, layout)` registers an `AqtweaksTrait` with cost 0 and one requirement, `adv|aqtweaks:class_only` (`ReskillableAPI` registers the `adv` prefix; an unknown advancement is never achieved and its tooltip prints an empty name, so the skill screen shows the perk locked and unbuyable). `LockUnlockableEvent.Pre` is cancelled for these perks, so nobody can give one back. A LOWEST registry pass stamps cost 0 and the requirement again in case Reskillable's own config pass rewrote them. `/reskillable resetskill` and `resetall` still clear them (`respec`); the starter module's `/aqclass regrant` restores them. Only the cell, parent skill and `Enable` come from `aqtweaks_reskillable.cfg` Perks; cost and requirements are forced.

| Perk | Class / skill | Effect | Notes |
| --- | --- | --- | --- |
| Riposte | Warrior / Defense | A shield raised at most `Riposte Parry Window Ticks` (10) before a hit parries it: the attacker is Stunned `Riposte Stagger Ticks` (10), the shield hold timer restarts (next stamina charge is a full interval away) and a sound/particle plays. | Vanilla and modded shields through `LivingAttackEvent` (the same frontal test as vanilla `canBlockDamageSource`, melee attackers only for the stagger); Aegis through `AegisBlockEvent` (Aegis is never billed, so only the stagger applies). One parry per 10 ticks. |
| Steady Draw | Archer / Attack | Drawing a bow costs no stamina; the hold cost starts only after `Steady Draw Full Draw Ticks` (20) at the normal (Ranged Efficiency) interval; `Steady Draw Arrow Return Chance` (0.75) of arrows that hit a living entity drop as an arrow item at its feet. | Arrow return needs pickup status ALLOWED (Infinity arrows are creative-only in vanilla) and a vanilla `EntityTippedArrow`; tipped arrows come back as plain arrows. Hooks: `StaminaModule.onItemUseStart` / `onItemUseTick`, `ProjectileImpactEvent.Arrow`. |
| Fleetfoot | Scout / Agility | Sneaking gives back `Fleetfoot Sneak Speed Restore` (0.5) of the vanilla sneak slowdown; sprint speed +`Fleetfoot Light Weapon Sprint Bonus` (10%) with a LIGHT weapon or empty hand (`StaminaModule.getWeaponType`); the first `Fleetfoot Free Sprint Ticks` (60) of a sprint run cost no stamina. | Two movement-speed modifiers (operation 2, not saved), checked every 5 ticks. The free start re-arms after `Fleetfoot Free Sprint Rearm Ticks` (40) of not sprinting, so stop-start sprinting cannot chain it. Cardio Master still lowers the per-spend cost after the free window; Adrenaline and Evasion do not read the sprint modifier. |
| Vein Rhythm | Miner / Mining | Each pickaxe-class break of stone or ore within `Vein Rhythm Window Ticks` (40) of the last adds a stack (max 5), each +4% break speed; the stacks fade after the window. | Server-side break speed (`PlayerEvent.BreakSpeed`) with a per-player counter removed on logout. The client's own mining animation does not know the stacks. |
| Studious | Scholar / Magic | Three Scholar-only theorycrafting cards, delivered by a virtual research aid, plus **Steady Focus**: focus cooldowns `Steady Focus Cooldown Reduction` (0.25) shorter. | Cards in `thaumcraft/studious/`. See Studious below. |
| Hearthkeeper | Homesteader / Building | **Open Hearth**: other players within `Open Hearth Range` (16) of the Homesteader get +`Open Hearth Visitor Bonus` (8) comfort, and the Homesteader gets a diminishing company bonus (6, 4, 3, 2, then 1 each, cap 15). **Home box**: anyone resting in the Homesteader's home gets +`Home Box Score Bonus` (15%) of their cozy score, and **Carry the Warmth** (Rested lasts +50%). | Comfort hooks in `comfort/HearthkeeperHooks.java`. See [comfort.md](comfort.md). |

Effect numbers: `aqtweaks_reskillable.cfg` Starter Combat Perks, Starter Scholar Perks and Starter Home Perks.

### Starter perk keys (`aqtweaks_reskillable.cfg`)

| Category | Key | Default | Use |
| --- | --- | --- | --- |
| Starter Combat Perks | Riposte Parry Window Ticks | 10 | A hit is a parry when the shield was raised at most this long before it |
| | Riposte Stagger Ticks | 10 | Stun on the attacker after a parry (0 disables) |
| | Riposte Free Parry Block | true | A parried block costs no shield stamina |
| | Steady Draw Full Draw Ticks | 20 | Bow draw time where the hold cost starts |
| | Steady Draw Arrow Return Chance | 0.75 | Chance a hit arrow is returned as an item |
| | Fleetfoot Sneak Speed Restore | 0.5 | Share of the vanilla sneak slowdown given back |
| | Fleetfoot Light Weapon Sprint Bonus | 0.10 | Sprint speed bonus with a LIGHT weapon or empty hand |
| | Fleetfoot Free Sprint Ticks / Rearm Ticks | 60 / 40 | Stamina-free start of each sprint; idle time before it re-arms |
| | Vein Rhythm Window Ticks / Max Stacks / Bonus Per Stack | 40 / 5 / 0.04 | Mining stack window, cap and break-speed bonus per stack |
| Starter Scholar Perks | Studious Extra Choice / Rounds | true / 3 | The first rounds of a theory offer 3 cards |
| | Eureka Lead Threshold / Bonus / Inspiration Cost | 30 / 15 / 0 | When Eureka is offered, the progress it adds and its inspiration cost |
| | Cross-Reference Min Categories | 2 | Categories with progress needed before it is offered |
| | Cross-Reference Max Categories | 4 | The most categories it spreads over |
| | Cross-Reference Bonus | 5 | Progress added to each such category |
| | Cross-Reference Inspiration Cost | 1 | Inspiration it costs |
| | Second Opinion Extra Draws | 1 | Extra 3-card rounds added to the theory |
| | Second Opinion Progress | 5 | Progress added to the leading category |
| | Second Opinion Inspiration Cost | 0 | Inspiration it costs |
| | Steady Focus Cooldown Reduction | 0.25 | The Scholar's focus cooldowns are this much shorter |
| Starter Home Perks | Open Hearth Range / Visitor Bonus | 16 / 8 | Blocks and comfort score for other players near a Homesteader |
| | Open Hearth Keeper Curve | 6, 4, 3, 2 | The Homesteader's own diminishing company bonus: first other player, second, and so on |
| | Open Hearth Keeper Tail | 1 | Bonus for each further player after the curve |
| | Open Hearth Keeper Cap | 15 | The most the company bonus can reach (0 or less = no cap) |
| | Home Box Score Bonus | 0.15 | Share of cozy score added for anyone resting in a Homesteader's home |
| | Carry the Warmth Extra | 0.5 | Rested lasts this much longer in that home |

**Studious (Scholar).** `StudiousModule.init` registers one virtual aid (`StudiousAid`, keyed by class name like every Thaumcraft aid; its aid object is a string, so `checkSurroundingAids` can never match it) and three cards (`TheorycraftManager.registerCard`, each `isAidOnly` and each refusing in `initialize` unless the player has the perk). `MixinTileResearchTable` adds the aid key to a Scholar's `startNewTheory` set, which also gives the stock +1 starting inspiration, and queues `Studious Extra Choice Rounds` (3) rounds that offer three cards instead of two. Thaumcraft draws an aid card on about a quarter of picks, once each per theory. Cards: **Eureka** (offered once one category has `Eureka Lead Threshold` (30) progress: +`Eureka Bonus` (15) to the leading category, costs `Eureka Inspiration Cost` (0), a refund of the usual 1); **Cross-Reference** (once at least 2 categories have progress: +5 to each, up to 4, costs 1); **Second Opinion** (free: +5 to the leading category and `Second Opinion Extra Draws` (1) more three-card rounds). Marginalia (book consumption) was dropped. **Steady Focus**: `MixinCasterManager` scales the tick argument of `CasterManager.setCooldown` for a player with the perk, on both sides; Thaumcraft Fix, Thaumic Tweaker and Thaumic Speedup were checked and do not touch that method.

**Deviation from the plan.** Thaumcraft marks the picked card selected right after `activate`, so Second Opinion cannot redraw the current set from inside the card; it widens the next draw instead. `ResearchTableData.placedCards` is never incremented by Thaumcraft, so Eureka reads the category totals only.

## Files

- `reskillable/ReskillablePerkRegistry.java`, `AqtweaksTrait.java`, `ReskillablePerkLayout.java`
- `reskillable/ReskillableModule.java` — events + Mining Expert HarvestCheck
- `reskillable/RespiteHandler.java` — lethal save
- `potion/PotionPerkCooldown.java` — Adrenaline / Evasion / Respite CD HUD
- `stamina/StaminaPerks.java` — spend reductions, Adrenaline, Evasion, Power Attack
- `reskillable/ReskillableBonuses.java` — levels, classify, EB add
- `reskillable/EffortlessBuildingHooks.java` — mixin bridge; direct `ReskillableBonuses.addBuildingPlaceReach` / `addBuildingMaxBlocks` behind `isModLoaded("reskillable")`
- `animania/AnimaniaModule.java`, `AnimaniaFarmClocks.java`, `AnimaniaFarmProducts.java`
- `thaumcraft/ThaumcraftPerkHooks.java`
- `thaumcraft/studious/` — `StudiousModule` (registration, perk test), `StudiousAid`, `CardEureka`, `CardCrossReference`, `CardSecondOpinion`, `StudiousMath` (pure), `StudiousHooks` (Steady Focus)
- `mixin/thaumcraft/MixinTileResearchTable.java` (Studious aid and extra choice), `MixinCasterManager.java` (`setCooldown` for Steady Focus)
- `comfort/HearthkeeperHooks.java`, `HearthkeeperMath.java` — Hearthkeeper (see [comfort.md](comfort.md))
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
- `mixin/botania/MixinManaItemHandler.java`, `MixinSubTileGenerating.java`, `MixinLensDamage.java` — `mixins.aqtweaks.botania.json`
- `mixin/explosion/MixinExplosionBurstOwner.java` — early json; credits a Botania mana burst's shooter for Explosive and Storm lens blasts
- **Mana burst damage credit:** Botania's Damage lens hit with `DamageSource.MAGIC`, which has no attacker, so a mana blaster never aggroed villagers, golems or mobs and kills gave the player no credit. `MixinLensDamage` redirects its `attackEntityFrom` to `causeIndirectMagicDamage(burst, shooter)` (same magic, armour-bypassing damage) when the burst has a shooter. The blaster's burst never records one (its constructor uses the world-only `EntityThrowable` constructor, so vanilla `getThrower()` is null), so `MixinEntityManaBurstShooter` implements `botania.BurstShooter` on `EntityManaBurst` and stores the player in the player constructor; both attribution mixins read that. `MixinExplosionBurstOwner` makes `Explosion.getExplosivePlacedBy()` return the shooter when the exploder is an `EntityManaBurst` (matched by class name, no Botania import in the early json), so Explosive and Storm lens blasts are credited too; the shooter still takes their own blast. Terra Sword beams, magic missiles, pixies and the other Botania weapons already credit the player. Spreaders (no shooter) are unchanged
- `mixin/bewitchment/MixinMagicPower.java`, `MixinTileEntityWitchesAltar.java`, `MixinUtilPoppet.java`
- `mixin/astral/MixinActiveCraftingTask.java`, `MixinAbstractAltarRecipe.java`
- `mixin/embers/MixinEmberInventoryUtil.java`, `MixinTileEntityStamper.java`, `MixinTileEntityMixerBottom.java`, `MixinTileEntityFurnaceBottom.java` — `mixins.aqtweaks.embers.json`
- `mixin/simpledifficulty/MixinTemperatureCapability.java`
- `mixins.aqtweaks.effortlessbuilding.json`
- `reskillable/ProspectAccuracy.java`, `ProspectorSample.java` — Prospectus pick chances and the ore boxes from one click
- `mixin/prospectus/MixinItemProspector.java`, `InvokerProspectus.java` — `mixins.aqtweaks.prospectus.json`. Right-click is `func_180614_a`; `getBlockState` invoke is MCP with `remap = true`
- `mixin/dss/MixinEntitySightData.java` — Dark Vision counts as `hasNightvision`
- `reskillable/PerkOpportunistic.java`, `mixin/dss/MixinDynamicStealthAttack.java` — records recent living-source hits; the mixin redirects `Sight.canSee` inside `DynamicStealth.entityAttackedPre` to false when a different attacker hit the victim in the last 60 ticks
- `reskillable/PerkThreat.java` — Taunt ×2 and Low Profile ×0.75 on the six threat-generation stats (decay stays 1)
- `reskillable/OrchardFruit.java` — ripe tree fruit for Orchard (right-click harvest does not fire `HarvestDropsEvent`)
- `reskillable/client/PacketProspectorMarks.java` — ore positions sent to the mining player; the client draws the boxes
- `potion/PerkCooldownEffects.java` — applies the Adrenaline / Evasion / Respite cooldown potions server-side
- `mixin/playerrevive/MixinRevival.java` — Fast Revive: each perk holder counts as a second helper in `Revival.tick` (`mixins.aqtweaks.playerrevive.json`)
- `mixin/thaumcraft/MixinToolEvents.java` — cancels Thaumcraft's Rare Earth roll in `ToolEvents.harvestBlockEvent` when Lithomancy owns it (`PerkDrops.ownsLithomancy`)
- `reskillable/client/DarkVisionLight.java` — lightmap mix
- `reskillable/PerkDurability.java`, `PerkDrops.java` — Stone Cleaver / Wood Splitter, crop/seed drops. The durability mark expires after one world tick and clears on logout.
- `AqtweaksTrait` builds each trait without its `trait|` requirement rows (Reskillable resolves them at construction and logged "Unlockable not found" for forward references); `ReskillablePerkRegistry.stampParentPerkCosts` (LOWEST) restamps every trait whose layout has `trait|` rows once all are registered.
- `mixin/MixinItemStackDurability.java` (`ItemStack.damageItem` HEAD), `mixin/MixinBlockCropsSeed.java` (`BlockCrops.getSeed` invoker) — **`mixins.aqtweaks.early.json`**; FQCN into `reskillable/` helpers

## Config key reference (keys not named elsewhere in this file)

Exact `@Config.Name` strings and code defaults, so the doc and the cfg cannot drift apart. The meaning of each group is in the sections above; the note is the first sentence of the in-game comment.

| Key | Default | Section | Note |
| --- | --- | --- | --- |
| Effect Duration Ticks | `100` | ReskillableRespite | Regen II and Tea Story defence duration. |
| Regen Amplifier | `1` | ReskillableRespite | 0 = Regeneration I, 1 = Regeneration II. |
| Defence Potion ID | `teastory:defence` | ReskillableRespite |  |
| Defence Amplifier | `0` | ReskillableRespite |  |
| Damage Per Level | `0.125` | ReskillableAttack | Added to generic.attackDamage (operation 0). |
| Armor Per Level | `0.25` | ReskillableDefense | Added to generic.armor (operation 0). |
| Speed Per Level | `0.0003125` | ReskillableAgility | Added to generic.movementSpeed. |
| EB Place Reach Per Level | `0.125` | ReskillableBuilding | Added to Effortless Building getPlacementReach (its non-Normal build modes). |
| EB Max Blocks Per Level | `1.0` | ReskillableBuilding | Added to Effortless Building getMaxBlocksPlacedAtOnce in survival. |
| Break Speed Per Level | `0.01` | ReskillableMining | BreakSpeed multiply: speed × (1 + level × k). |
| Mining Expert Harvest Floor | `3` | ReskillableMining | Pickaxes with Mining Expert treat harvest as at least this level. |
| Extra Drop Chance Per Level | `0.00625` | ReskillableGathering | Chance of +1 forage / extra wool / extra fish. |
| Herbalist Namespaces | `{bewitchment, thaumcraft, botania, rustic, teastory, astralsorcery, mysticalworld }` | ReskillableGathering | Block registry namespaces that always +1 with Herbalist. |
| Extra Drop Chance Per Level | `0.00625` | ReskillableFarming | Chance of +1 on a mature crop harvest. |
| Rancher Range | `16.0` | ReskillableFarming | Blocks from a Rancher player to extra-tick tended Animania animals. |
| Hurt Multiply Per Level | `0.0125` | ReskillableMagic | Spell-like LivingHurtEvent multiply, capped at 40%. |
| Log Damage Classify | `true` | ReskillableMagic | INFO-log unique damageType / isMagicDamage / source classes for player-involved hits (cap 48). |
| Allow Type Prefixes | `{fireball}` | ReskillableMagic | If isMagicDamage is false, still treat damageType (lowercase prefix) as spell-like. |
| Deny Types | `{wither, onFire, lava, hotFloor}` | ReskillableMagic | Exact damageType strings that never count as spell-like. |
| Vis Thrift Add | `0.30` | ReskillableMagic | Added to CasterManager.getTotalVisDiscount when Vis Thrift is unlocked. |
| Quiet Mind Bound Fraction | `0.35` | ReskillableMagic | Subtract round(fraction × incident bound) from warp severity after visor. |
| Quiet Mind Cancel Chat | `Whispers try to cloud your thoughts, but you push them away...` | ReskillableMagic |  |
| Full Font Aura Ratio | `0.9` | ReskillableMagic | Chunk vis / aura base at or above this counts as full. |
| Full Font Vis Cost | `1.5` | ReskillableMagic | Multiply ItemCaster.consumeVis amount before Vis Thrift when the aura is full. |
| Full Font Output | `1.5` | ReskillableMagic | Outgoing classified Magic / Heal-focus after drip and Blood Pact, then the stamp clears. |
| Blood Pact Max Health | `-8.0` | ReskillableMagic | Unsaved generic.maxHealth add while Blood Pact is unlocked. |
| Blood Pact Outgoing | `2.0` | ReskillableMagic | Outgoing classified Magic multiply after drip, before Full Font. |
| School Thrift | `0.70` | ReskillableMagic | Parent resource spend multiply on the school perk (mana, ritual drain, altar starlight, Ember). |
| Grove Range | `24.0` | ReskillableMagic |  |
| Grove Mana | `1.15` | ReskillableMagic | Generating-flower addMana multiply while a Druid is in range. |
| Mana Veil Absorb | `0.20` | ReskillableMagic |  |
| Mana Veil Per HP | `1000` | ReskillableMagic |  |
| Hearth Altar Gain | `1.15` | ReskillableMagic |  |
| Altar Craft Time | `0.80` | ReskillableMagic | Astromancer craftingTickTime multiply. |
| Star Powered Damage | `1.25` | ReskillableMagic |  |
| Cinder Ward Fire | `0.80` | ReskillableMagic |  |
| Cinder Ward Heal | `0.20` | ReskillableMagic |  |
| Cinder Temp Max | `15` | ReskillableMagic |  |
| Astral Temp Min | `9` | ReskillableMagic |  |
| Foundry Pulse Every | `4` | ReskillableMagic | Extra machine tick when world time modulo this is 0. |

## Do not regress

- Do not put vanilla-target mixins (`ItemStack`, `BlockCrops`, …) in late `mixins.aqtweaks.json` (`loaded too early` at boot)

- Stamina `aqtweaks_stamina.cfg` Reskillable perk section and `PerkAccess.has`
- Duplicate registry if pack CrT still registers Armor Mastery / Mining Efficiency
- No Reskillable `import` from `StaminaModule` / `StaminaFeathers` / config class body
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
- Building: vanilla block reach +1 / +2 at 16 / 32 in Normal mode (server rejects past about 6 to 8 blocks); EB placement ray +2 / +4 at 16 / 32 in EB non-Normal modes; max blocks +16 / +32 survival; creative unchanged; melee reach unchanged; EB jar absent → no mixin crash
- Mining: faster break; server-authoritative, so the client shows no bonus preview; stamina break cost unchanged (perk still does)
- Farming: extra wheat on mature crop; not on stone or ore
- Gathering: extra log/leaf/flint, extra wool, extra fish; **not** ore; silk touch no extra
- Mining Expert: wood pick + perk drops diamond ore/obsidian; tooltip stars unchanged; fist does not
- Magic schools: mutex in GUI; Druid flowers +15% mana in 24; Witch drain cheaper; Astromancer altar faster; Artificer stamper extra ticks; Cinder/Astral temp clamps with SD; Stitch repair shapeless

## Out of scope unless asked

- Recipe/item gating (Recipe Stages / Game Stages / CrT)
- Shipping Universal Tweaks Armor Curve in this jar

Weapon Tempo: `StaminaPerks.tempoFree` (called first in `StaminaModule.onAttackEntity`) reads the live DSS combo through `DssComboTempo` (imports DSS types; only used when `dynamicswordskills` is loaded). The attack about to land is combo hit `numHits + 1`; hits 1 and 2 are never free; hit N is free when `N % Weapon Tempo Free Hit Interval == 0` (3, 6, 9, 12) or, on the other hits, with chance `Weapon Tempo Bonus Chance Per Level` (0.05) x Basic Technique level (`SwordBasic`, max 10), rolled once with the player RNG. A free hit pays nothing, skips Power Attack and cannot take the empty-stamina penalty. No lock-on, no combo (timed out, finished, damage tolerance) means no effect. Expected free share of a full combo at 5% per level: 33% at level 1, 40% at level 4, 49% at level 7, 58% at level 10. Icons `melee_efficiency.png` (vanilla iron sword + feather) and `weapon_tempo.png` (iron sword + five gold ticks) come from `scripts/generate_efficiency_icons.py` and `scripts/icon_sources/`. Attack-tree cells used: (0,1) (1,1) (2,1) (4,1) (2,2) (3,2) (4,2) (1,3) (2,3) (3,3) (4,3).
