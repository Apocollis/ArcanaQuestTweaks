# Reskillable module (1.8)

Last updated: 2026-09-05.

Config: `config/arcanaquesttweaks/aqtweaks_reskillable.cfg`. Handler registers only if `reskillable` is loaded (`CommonProxy.init`). Compile-hard CAD Reskillable **1.13.1** API; types live only in `com.apocollis.aqtweaks.reskillable`. Soft `@Mod` `after:reskillable` (not `required-after`).

Stamina Armor Mastery / Mining Efficiency **perk id lookups** stay in [stamina.md](stamina.md) (`Reflect.hasUnlockable`, `aqtweaks_stamina.cfg`). This module does **not** register those unlockables.

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
| Gathering | harvest / shear / fish | `level × 0.00625` chance of +1 |
| Farming | mature crop `HarvestDropsEvent` | same k as Gathering |
| Magic | `LivingHurtEvent` NORMAL | `± min(level × 0.0125, 0.4)` |

Attributes restamp: `LevelUpEvent.Post`, login, respawn, clone, dim change, Tweaks cfg change, every 20 server ticks. Skip `FakePlayer`. One Tweaks UUID per attribute; remove then apply; `setSaved(false)`.

**Do not** also multiply Attack/Defense in hurt. Magic has no vanilla attribute.

### Gathering vs Farming

Farming: mature `BlockCrops`, nether wart age 3, cocoa age 2, pumpkin, melon. Not saturation.

Gathering: logs / `logWood`, gravel, leaves / `treeLeaves`, flowers, mushrooms, tall grass. Skip silk touch. Skip `ore*` OreDict and vanilla ore blocks. Extra wool after a successful sheep shear (next-tick if `getSheared()` flipped). Extra fish: one more stack entry on `ItemFishedEvent`.

### Magic classify

Outgoing if trueSource is the player; incoming if victim is the player. Same classify:

1. `isMagicDamage()` **or** cfg allow-prefix on `damageType`, **and**
2. type is not `thorns`, **and**
3. immediate source is not `EntityPotion` / `EntityAreaEffectCloud`, **and**
4. source is not the vanilla `DamageSource.MAGIC` singleton, **and**
5. type is not on the deny list (`wither`, `onFire`, `lava`, `hotFloor`).

Gaia bolts Tweaks recast to `causeIndirectMagicDamage` still match. Splash/lingering potion HP does not.

`logClassify` (default **true**) INFO-logs unique `damageType` + `isMagicDamage` + source classes for player-involved hits (cap 48) so a gauntlet/focus shot can lock allow prefixes without a rebuild.

## Config

| Name | Default | Live? | Meaning |
| --- | --- | --- | --- |
| Enable Reskillable Bonuses | true | yes (restamp) | Master switch |
| Attack damage / Defense armor / Agility speed per level | 0.125 / 0.25 / 0.0003125 | yes | Attribute add |
| EB place reach / max blocks per level | 0.125 / 1 | yes | Mixin add on EB getters |
| Mining break speed per level | 0.01 | yes | BreakSpeed |
| Gathering / Farming extra chance per level | 0.00625 | yes | Proc chance |
| Magic hurt per level | 0.0125 | yes | Cap 40% |
| Log damage classify | true | yes | First-pass DEVBOX log |
| Allow type prefixes | empty | yes | After gauntlet log |
| Deny types | wither, onFire, lava, hotFloor | yes | Never spell-like |

Instance cfg keeps old keys when Java defaults change.

## Files

- `reskillable/ReskillableModule.java` — events
- `reskillable/ReskillableBonuses.java` — levels, classify, EB add
- `reskillable/EffortlessBuildingHooks.java` — mixin bridge (no Reskillable imports)
- `mixin/effortlessbuilding/MixinReachHelper.java`
- `mixins.aqtweaks.effortlessbuilding.json`

## Do not regress

- Stamina `aqtweaks_stamina.cfg` Reskillable perk section and `Reflect.hasUnlockable`
- No unlockable registration
- No Reskillable `import` from `StaminaModule` / `Reflect` / config class body
- No vanilla `REACH_DISTANCE`; do not mixin EB `getMaxReach`
- Attack/Defense not extra-multiplied in `LivingHurtEvent`
- Comfort still only cancels rest; Gaia bolt retype still runs before this NORMAL hurt
- `--release 21`

## Verify

- Reskillable jar removed: Tweaks boots; stamina perks unchanged; EB mixin json skipped or hooks no-op
- Attack 16 → +2.0 `attackDamage`; 32 → +4.0; login/respawn keep it; sword tooltip/F3
- Defense: armor bar up, max health unchanged
- Agility: move speed up; Elenai dodge unchanged
- Building: EB placement ray +2 / +4 at 16 / 32; max blocks +16 / +32 survival; creative unchanged; melee reach unchanged; EB jar absent → no mixin crash
- Mining: faster break; stamina break cost unchanged (perk still does)
- Farming: extra wheat on mature crop; not on stone or ore
- Gathering: extra log/leaf/flint, extra wool, extra fish; **not** ore; silk touch no extra
- Magic: `latest.log` `AQTweaks-Reskillable` line for a gauntlet/focus shot (`type=`, `isMagic=`, `classified=`). At Magic 32, classified outgoing +40%, Gaia bolt taken −40%; harming splash does not scale; sword/thorns do not scale

## Out of scope unless asked

- Perk registration; extra HP; attack speed; bow damage; general % DR; potion duration
- Vanilla reach; Agility stamina/dodge; farming saturation; gathering extra **ore** (pack Mining perk)
- Recipe/item gating (Recipe Stages / Game Stages / CrT)
- Shipping Universal Tweaks Armor Curve in this jar
