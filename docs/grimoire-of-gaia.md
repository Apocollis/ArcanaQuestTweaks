# Grimoire of Gaia module (1.8)

Last updated: 2026-08-31.

Config: `config/arcanaquesttweaks/aqtweaks_grimoireofgaia.cfg` (master switch) and `config/arcanaquesttweaks/gaia_mob_damage.json` (per-mob **base** + type multipliers). Soft dependency (`after:grimoireofgaia`). Optional `mixins.aqtweaks.gaia.json` (`required: false`). Parent jar: `GrimoireOfGaia3-1.12.2-1.7.2`.

## Locked intent

Tweaks owns **outgoing base amount** and **damage type**. Gaia still owns AI, models, who swings, held weapons, and Strength-style buffs.

`gaia_mob_damage.json` `mobs[id]` is `ATTACK_DAMAGE` **base** (`setBaseValue`). Orc `4` plus an iron sword / Strength still hits harder than 4 — raise or lower JSON to compensate. Spell = `getAttributeValue()` × `spellMultiplier` (includes gear/buffs); bomb same with `bombMultiplier`.

**One extra HP packet must not run.** Drop Gaia extras (melee instant-damage potion, archer harming tip, bomb thrown 2.0). **Do not** convert pierce into +6. Do **not** strip weapon or potion attack modifiers.

**Types**

| Attack | Packet Tweaks keeps | Type | Attribution |
| --- | --- | --- | --- |
| Melee | Vanilla `"mob"` from `super.attackEntityAsMob` | Physical: armor + Protection. Vanilla shield. | Mob is `trueSource` (untouched) |
| Archer | Vanilla tipped arrow | Projectile physical. Vanilla shield. | Shooter on the arrow |
| Magic / bubble / poison / web / magic-random | Gaia’s only HP packet, recast | `causeIndirectMagicDamage` (cultist-orb): armor/shields skip; Bewitchment Magic Protection yes; vanilla Protection still applies | Projectile immediate, caster `trueSource` |
| Bomb | Gaia’s MAGIC packet, recast | Explosion: armor + Blast Protection. Facing shield **zeroes** the hit (vanilla 1.12). | Bomb immediate, thrower `trueSource` |

Death messages and thorns / “hurt the attacker” read `trueSource`. The dropped MAGIC 6 had none.

Partial blast-shield absorption is **out of scope** (possible later module).

JSON load **once** in preInit. Edit JSON, **restart** (no Tweaks rebuild). Mixins are the hook; JSON is the table.

## How the parent mod works (1.7.2)

**Tier amounts:** `GaiaConfig.ATTRIBUTES.tierNattackDamage` (percent). `EntityAttributes` static init: `ATTACK_DAMAGE_1/2/3 = 4/8/12 × percent/100`. Each mob class sets vanilla `ATTACK_DAMAGE` from one of those three in `applyEntityAttributes`. Tweaks JSON **replaces the attribute base** for listed ids (instance `%` unused for those ids). Swords and Strength still add on top.

**`GaiaConfig.DAMAGE` is on/off, not an amount:** `baseDamage` (melee extra potion), `shieldsBlockPiercing`, `baseDamageArchers` (Hard-mode arrow tip).

**Melee** (`EntityMobHostileBase` / `EntityMobAssistBase.func_70652_k`):

1. `super.attackEntityAsMob` → vanilla `"mob"`, `trueSource` = Gaia mob, physical, armored.
2. If that succeeded and `baseDamage`: `addPotionEffect(INSTANT_DAMAGE, duration 2, amplifier 0)` → harming I = **6** `DamageSource.MAGIC` (unblockable, no attacker). Shield + `shieldsBlockPiercing` can skip (2). Tweaks **drops (2)**; does not recast it.

**Spell fireballs** (`EntityGaiaProjectileMagic`, `MagicRandom`, `Bubble`, `Poison`, `Web`): one HP packet, `attackEntityFrom(DamageSource.MAGIC, ATTACK_DAMAGE_2 / 2)`. Always the **global T2 constant**, not the shooter’s attribute. `MAGIC` is a singleton (`isMagicDamage` + `isUnblockable`, no `trueSource`). Status potions (slowness, poison, …) are not a second HP hit. All five use the same MAGIC source; type is the entity class.

**Bomb** (`EntityGaiaProjectileBomb`): (1) same MAGIC `ATTACK_DAMAGE_2 / 2`; (2) `causeThrownDamage` **2.0**. Neither sets `isExplosion()`.

**Archer** (`gaia.entity.ai.Ranged.rangedAttack`): vanilla `EntityTippedArrow`. On Hard + `baseDamageArchers`, tip INSTANT_DAMAGE → extra MAGIC **6**.

Golem/witch `INSTANT_DAMAGE` in bytecode is potion **immunity** (`func_70687_e`), not extra player pierce.

An **orc** that also casts still has T1 on the entity in Gaia; Gaia bolts ignore it and use T2/2. Tweaks uses the same JSON **base** for that orc’s melee; bolts use live `getAttributeValue()` (base + gear + buffs) × `spellMultiplier`.

## How Tweaks hooks in

No tick tracker. No `LivingHurtEvent` correlation.

| Attack | First / only packet | Extra (drop) | Tweaks |
| --- | --- | --- | --- |
| Melee | Vanilla `"mob"` | Instant-damage **6** MAGIC | Redirect `addPotionEffect` in `func_70652_k` (hostile + assist): skip INSTANT_DAMAGE on players. Backup: `PotionApplicableEvent` DENY if `INSTANT_DAMAGE` duration **2** amplifier **0** on a player. No replacement hit. |
| Archer | Vanilla tipped arrow | Instant-damage tip | Redirect `Ranged.rangedAttack` `addEffect`: skip INSTANT_DAMAGE. Do not add +6 to arrow damage. |
| Magic / bubble / poison / web / magic-random | Gaia MAGIC `ATTACK_DAMAGE_2/2` | Status potions stay | Redirect `attackEntityFrom`: if MAGIC and player, amount = shooter `getAttributeValue()` × `spellMultiplier`. Source = `causeIndirectMagicDamage(projectile, shooter)`. |
| Bomb | Gaia MAGIC | Thrown **2.0** | MAGIC → `GaiaDamageSources.Bomb` (`setExplosion()`, bomb + thrower), amount = thrower `getAttributeValue()` × `bombMultiplier`. Skip thrown invoke for players. |

**Cleanroom INVOKE rule:** mixin **class** stays `remap = false` (Gaia methods are SRG: `func_70652_k`, `func_70227_a`, `func_70184_a`, `rangedAttack`). Vanilla **INVOKE** targets must be **MCP + `remap = true`** (`addPotionEffect`, `attackEntityFrom`, `addEffect`). SRG on those INVOKEs fails `InvalidInjectionPointException` and the json is `required: false`, so the game boots **with mixins off**. Same class of bug as `WorldGenLakes`. After deploy, `latest.log` must have **zero** `mixins.aqtweaks.gaia.json` injection failures before tuning JSON.

**Per-mob JSON:** keys `grimoireofgaia:<path>` (e.g. `orc`). Shipped seeds are Gaia **100%** constants (T1 **4**, T2 **8**, T3 **12** by class). Unlisted ids keep Gaia’s attribute. `EntityJoinWorldEvent` (server) `setBaseValue` only — weapons and buffs stack. Missing keys on upgrade are merged from defaults.

Master switch `Disable Piercing Damage` (default **true**): false → Gaia vanilla (no JSON overwrite, mixins call through).

Player-only for melee skip and bolt/bomb retype. Archer tip skip is at shoot time (all victims).

## Config

| Name | Default | Live? | Meaning |
| --- | --- | --- | --- |
| Disable Piercing Damage (`aqtweaks_grimoireofgaia.cfg`) | true | yes (Forge sync) | Drop extras + retype bolts/bombs + apply JSON bases |
| `gaia_mob_damage.json` | T1/T2/T3 seeds, multipliers 1.0 | no (preInit) | Per-id melee **base**; spell/bomb multipliers |

## Files

- `gaia/GaiaPierce.java` — skip potion/tip; retype MAGIC
- `gaia/GaiaDamageSources.java` — `Bomb` only
- `gaia/GaiaDamageConfig.java` — JSON
- `gaia/GaiaDamageHandler.java` — join-world `setBaseValue`; `PotionApplicableEvent` pierce backup
- `mixin/gaia/MixinEntityMobHostileBase.java`, `MixinEntityMobAssistBase.java`, `MixinGaiaMagicProjectile.java`, `MixinEntityGaiaProjectileBomb.java`, `MixinRanged.java`
- `mixins.aqtweaks.gaia.json`

## Do not regress

- Do not change `super.attackEntityAsMob` or arrow `setDamage` (only the extra potion/tip).
- Magic bolts: `causeIndirectMagicDamage` (magic + bypass armor), not the MAGIC singleton.
- Bomb: explosion, not magic-bypass; not `setDamageIsAbsolute`; do not null `getDamageLocation()` (shield must still be able to zero).
- Bolt status potions stay.
- Do not mixin vanilla `EntityTippedArrow` globally.
- Mixin json `required: false`. Vanilla INVOKEs: MCP + `remap = true`.
- JSON is **base** (`setBaseValue`). Do not strip weapon/Strength modifiers.
- Mixin APPLY in the log does not prove pierce is skipped — diamond must not take a flat MAGIC 6.

## Out of scope unless asked

- Partial shield absorption for blasts
- Gaia AI, beacon/cloud potions
- Rewriting archer arrow damage from JSON
- Hot-reload JSON without restart

## Verify

- `latest.log`: no `Mixin apply for mod aqtweaks failed mixins.aqtweaks.gaia.json` / `InvalidInjectionException`.
- Orc JSON `4`: **no** MAGIC 6. Diamond: physical leftover only (not a flat ~3 hearts of magic). Unarmored physical may exceed 4 (sword/buffs).
- Bolt: attributed indirect magic; armor does not help; Magic Protection does; death names caster.
- Bomb: armor + Blast Protection; no extra 2.0; facing shield zeroes as vanilla; death names thrower.
- JSON edit + restart moves orc melee and orc bolts together.
