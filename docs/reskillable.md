# Reskillable module (1.8)

Last updated: 2026-09-10.

Config: `config/arcanaquesttweaks/aqtweaks_reskillable.cfg`. Handler registers only if `reskillable` is loaded (`CommonProxy.init`). Compile-hard CAD Reskillable **1.13.1** API; types live only in `com.apocollis.aqtweaks.reskillable`. Soft `@Mod` `after:reskillable` (not `required-after`).

Stamina Armor Mastery / Mining Efficiency **perk id lookups** stay in [stamina.md](stamina.md) (`Reflect.hasUnlockable`, `aqtweaks_stamina.cfg`). This jar **registers** the stamina-tree traits listed below plus Mining Expert. It still does **not** register `aqtweaks:armor_mastery` or `aqtweaks:mining_efficiency` (pack CrT).

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

Gathering: logs / `logWood`, gravel, leaves / `treeLeaves`, flowers, mushrooms, tall grass. Skip silk touch. Skip `ore*` OreDict and vanilla ore blocks. Extra wool after a successful sheep shear (next-tick if `getSheared()` flipped). Extra fish: one more stack entry on `ItemFishedEvent`.

### Magic classify

Outgoing if trueSource is the player; incoming if victim is the player. Same classify:

1. `isMagicDamage()` **or** cfg allow-prefix on `damageType`, **and**
2. type is not `thorns`, **and**
3. immediate source is not `EntityPotion` / `EntityAreaEffectCloud`, **and**
4. source is not the vanilla `DamageSource.MAGIC` singleton, **and**
5. type is not on the deny list (`wither`, `onFire`, `lava`, `hotFloor`).

Gaia bolts Tweaks recast to `causeIndirectMagicDamage` still match. Splash/lingering potion HP does not.

Stock TC foci stamp `setMagicDamage()` in the [thaumcraft module](thaumcraft.md) mixin (not a `thrown` prefix). Heal on living uses `heal(float)`: caster outgoing `× (1 + bonus)` only; no incoming DR on heals. Food/regen/potions are not scaled.

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
| `aqtweaks:melee_efficiency` | attack | 2,2 | 6 | attack 16, agility 12 |
| `aqtweaks:ranged_efficiency` | attack | 2,3 | 6 | attack 16, agility 12 |
| `aqtweaks:shield_efficiency` | defense | 2,2 | 6 | defense 16 |
| `aqtweaks:adrenaline` | agility | 2,1 | 6 | agility 16, defense 12 |
| `aqtweaks:expert_climber` | agility | 1,2 | 6 | agility 20 |
| `aqtweaks:cardio_master` | agility | 3,3 | 6 | agility 20 |
| `aqtweaks:mining_expert` | mining | 3,3 | 6 | mining 24 |

Mining Expert: `PlayerEvent.HarvestCheck` client+server. Pickaxe tool class, block pickaxe or null tool, harvest ≤ floor. Does not change `Item.getHarvestLevel`.

Lang: `reskillable.unlock.aqtweaks.<path>` / `.desc`. Icons: `aqtweaks:textures/unlockables/<path>.png`.

## Files

- `reskillable/ReskillablePerkRegistry.java`, `AqtweaksTrait.java`, `ReskillablePerkLayout.java`
- `reskillable/ReskillableModule.java` — events + Mining Expert HarvestCheck
- `stamina/StaminaPerks.java` — spend reductions + Adrenaline
- `reskillable/ReskillableBonuses.java` — levels, classify, EB add
- `reskillable/EffortlessBuildingHooks.java` — mixin bridge (no Reskillable imports)
- `mixin/effortlessbuilding/MixinReachHelper.java`
- `mixins.aqtweaks.effortlessbuilding.json`

## Do not regress

- Stamina `aqtweaks_stamina.cfg` Reskillable perk section and `Reflect.hasUnlockable`
- No unlockable registration for Armor Mastery / Mining Efficiency (pack CrT)
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
- Mining: faster break; server-authoritative, so the client shows no bonus preview; stamina break cost unchanged (perk still does)
- Farming: extra wheat on mature crop; not on stone or ore
- Gathering: extra log/leaf/flint, extra wool, extra fish; **not** ore; silk touch no extra
- Mining Expert: wood pick + perk drops diamond ore/obsidian; tooltip stars unchanged; fist does not

## Out of scope unless asked

- Extra HP; attack speed; bow damage; general % DR; potion duration
- Vanilla reach; Agility stamina/dodge; farming saturation; gathering extra **ore** (pack Mining perk)
- Recipe/item gating (Recipe Stages / Game Stages / CrT)
- Shipping Universal Tweaks Armor Curve in this jar
